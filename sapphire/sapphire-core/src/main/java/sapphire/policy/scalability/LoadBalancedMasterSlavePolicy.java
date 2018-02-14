package sapphire.policy.scalability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import sapphire.common.AppObject;
import sapphire.policy.DefaultSapphirePolicy;
import sapphire.runtime.MethodInvocationRequest;
import sapphire.runtime.MethodInvocationResponse;
import sapphire.runtime.exception.AppExecutionException;

import static java.lang.System.*;
import static sapphire.runtime.MethodInvocationRequest.MethodType.READ;
import static sapphire.runtime.MethodInvocationResponse.ReturnCode.FAIL;
import static sapphire.runtime.MethodInvocationResponse.ReturnCode.REDIRECT;
import static sapphire.runtime.MethodInvocationResponse.ReturnCode.SUCCESS;

/**
 * {@code LoadBalancedMasterSlavePolicy} directs write requests to <em>master</em> replica and
 * load balance read requests between all replicas. The master replica will replicate write requests
 * to <em>slave</em> replica asynchronously. If master replica dies, slave will be promoted to be
 * master. If slave replica dies, replication will be suspended. Replication will be resumed when
 * slave becomes alive again.
 *
 * <p>
 * The DM is not intended to implement RAFT protocol. If you need RAFT, you should use consensus
 * DMs. Below are some differences between this DM and RAFT protocol:
 * <ul>
 *     <li>Cluster size: RAFT requires at least three servers. This DM requires only two servers.</li>
 *     <li>Leader election: RAFT has its own leader election protocol. This DM does not use
 *     RAFT's leader election protocol to elect master. It assumes that group policy has
 *     access to a highly available strongly consistent storage (e.g. a storage service provided by
 *     RAFT based consensus DMs), and uses this storage to facilitate leader election.
 *     </li>
 *     <li>Log entry application: In RAFT, log entries will be committed to underlying state machine
 *     only if the entries have been replicated to majority servers. In this DM, log entries are
 *     always committed to underlying state machine regardless the replication succeeds or not.
 *     </li>
 * </ul>
 *
 * <p>
 * <em>Leader Election:</em>
 * <p>
 * Replicas compete for an exclusive lease lock on group policy. Replica who wins the lock becomes
 * the master. Master replica should renew lock lease periodically to maintain its master role.
 * In the future, we may put the lock in a distributed high available storage (e.g. etcd), rather
 * than in group policy, to improve availability.
 *
 * <p>
 * <em>Write Operation Processing:</em>
 * <p>
 * <ol>
 *     <li>Client sends write request to master replica. Each request is identified by
 *     <{@code clientId}, {@code requestId}></li>
 *     <li>Master appends the write request to local log file</li>
 *     <li>Master executes the write request</li>
 *     <li>Master replicates write request to slave. Requests will be put in pending log on slave
 *     then be executed asynchronously.</li>
 *     <li>Master return result to client</li>
 * </ol>
 *
 * <p>
 * <em>Master Failure:</em>
 * <p>
 * When master fails, its lock in group policy will expire. The slave replica will grab the lock
 * and become master. Slave replica will become master if, and only if, it grabs the lock in group
 * policy and there is no <em>pending</em> write requests.
 *
 * <p>
 * <em>Slave Failure:</em>
 * When slave fails, request replication from master to slave will fail. Master will retry
 * indefinitely until slave come back online.
 *
 * <p>
 * <em>Failure Recovery:</em>
 * Failed replica always comes back in {@code RECOVING} stateMgr. Replica in {@code RECOVING} stateMgr
 * does not serve read or write requests, but it is able handle replication requests.
 *
 * <p>
 * <em>Thread Model:</em>
 * <ul>
 *      <li>Locking Thread: a thread dedicated for locking and state transition</li>
 *      <li>Object Method Invocation Thread: a thread dedicated to method invocation</li>
 *      <li>Replication Thread: a thread dedicated to replication</li>
 * </ul>
 *
 * <p>
 * <em>Limitations:</em>
 * <ul>
 *     <li>Eventual Consistency: The replication from master to slave is asynchronous, therefore
 *     read on slave replica may return stale data</li>
 *     <li>Single Failure Tolerance: This DM can only tolerate one failure at a time. For
 *     example, if master fails, slave will be promoted to master without data loss. In this case,
 *     the master failure has been handled properly. Now assuming the failed replica comes back as a
 *     slave, and starts to sync up with the master. If master fails before sync finishes, user
 *     will experience data loss.</li>
 * </ul>
 *
 * @author terryz
 */

public class LoadBalancedMasterSlavePolicy extends DefaultSapphirePolicy {
    private final static Random random = new Random(System.currentTimeMillis());
    /**
     * Client side policy
     */
    public static class ClientPolicy extends DefaultClientPolicy {
        private static Logger logger = Logger.getLogger(LoadBalancedMasterSlavePolicy.ClientPolicy.class.getName());

        private boolean isReadMethod(String methodName, ArrayList<Object> params) {
            // TODO (Terry): 1) to be implemented, 2) move to a Util class
            return false;
        }

        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            int cnt=1, retry=5;
            long waitInMilliseconds = 50L;
            GroupPolicy group = (GroupPolicy) getGroup();

            // TODO (Terry): Wrap with a generic exponential backoff retryer
            do {
                SapphireServerPolicy targetServer = group.getMaster();

                if (isReadMethod(method, params)) {
                    targetServer = group.getRandomServer(group.getServers());
                }

                if (targetServer == null) {
                    throw new Exception(String.format("unable to find target server from server list: %s", group.getServers()));
                }

                MethodInvocationRequest request = new MethodInvocationRequest.Builder(method).params(params).build();
                MethodInvocationResponse response = targetServer.onRPC(request);
                switch (response.getReturnCode()) {
                    case SUCCESS:
                        return response.getResult();
                    case FAIL:
                        // This is application error. No need to retry.
                        logger.log(Level.INFO, "failed to execute request {0}: {1}", new Object[]{request, response});
                        throw (AppExecutionException) response.getResult();
                    case REDIRECT:
                        Thread.sleep(waitInMilliseconds);
                        waitInMilliseconds <<= 1;
                }
            } while (++cnt <= retry);

            throw new Exception(String.format("failed to execute method %s after retries", method));
        }
    }

    /**
     * Server side policy
     */
    public static class ServerPolicy extends DefaultServerPolicy {
        private final long Master_Lease_Timeout_InMillis = 500;
        private final long Master_Lease_Renew_Interval_InMillis = 100;
        private final long Init_Delay_Limit_InMillis = 100;

        private final Logger logger = Logger.getLogger(LoadBalancedMasterSlavePolicy.ServerPolicy.class.getName());
        private final ExecutorService invocationExecutor = Executors.newSingleThreadExecutor();

        private final Configuration config;
        private final StateManager stateMgr;
        private final ILogger requstLogger;
        private final IReplicator requestReplicator;

        public ServerPolicy() {
            this.config = Configuration.newBuilder()
                    .masterLeaseRenewIntervalInMillis(Master_Lease_Renew_Interval_InMillis)
                    .masterLeaseTimeoutInMIllis(Master_Lease_Timeout_InMillis)
                    .initDelayLimitInMillis(Init_Delay_Limit_InMillis).build();


            // TODO (Terry): we should not do the cast here
            LoadBalancedMasterSlavePolicy.GroupPolicy group = (LoadBalancedMasterSlavePolicy.GroupPolicy)getGroup();
            this.stateMgr = new StateManager(getClientId(), group, this.config);
            this.requstLogger = new FileLogger();
            this.requestReplicator = new AsyncReplicator();
        }

        /**
         *
         * @param request method invocation request
         * @return method invocation response
         */
        @Override
        public MethodInvocationResponse onRPC(MethodInvocationRequest request) {
            switch (this.stateMgr.getCurrentStateName()) {
                case SLAVE:
                    if (request.getMethodType() != null && request.getMethodType() == READ) {
                        return invokeMethod(request);
                    }
                    // redirect non-read operations to master
                    return new MethodInvocationResponse.Builder(REDIRECT, null).build();

                case MASTER:
                    if (request.getMethodType() != null && request.getMethodType() == READ) {
                        return invokeMethod(request);
                    }

                    logRequest(request);
                    replicateRequest(request);
                    return invokeMethod(request);
            }

            throw new AssertionError("should never reach here");
        }

        /**
         * Invokes method on dedicated {@link #invocationExecutor} thread
         *
         * @param request
         * @return {@link MethodInvocationResponse}
         */
        private MethodInvocationResponse invokeMethod(final MethodInvocationRequest request) {
            final AppObject targetObj = appObject;
            Future<Object> f = invocationExecutor.submit(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    return targetObj.invoke(request.getMethodName(), request.getParams());
                }
            });

            MethodInvocationResponse resp;
            try {
                Object ret = f.get();
                resp = new MethodInvocationResponse.Builder(SUCCESS, ret).build();
            } catch (ExecutionException e) {
                // Method invocation on application object failed.
                // This is caused by application errors, not Sapphire errors
                AppExecutionException ex = new AppExecutionException("method invocation on app object failed", e);
                logger.log(Level.FINE, "failed to process request {0} on {1}: {2}", new Object[]{request, appObject, ex});
                resp = new MethodInvocationResponse.Builder(FAIL, ex).build();
            } catch (InterruptedException e) {
                // TODO (Terry): what should we do here?
                // This is likely caused by Sapphire errors.
                // The problem is: the other replica may not run into this exception. This exception
                // may cause two replicas out of sync.
                AppExecutionException ex = new AppExecutionException("Method invocation on appobject was interrupted", e);
                logger.log(Level.WARNING, "the process of request {0} on {1} was interrupted: {2}", new Object[]{request, appObject, ex});
                resp = new MethodInvocationResponse.Builder(FAIL, ex).build();
            }

            return resp;
        }

        private void logRequest(MethodInvocationRequest request) {
            try {
                this.requstLogger.log(request);
            } catch (Exception e) {
                throw new AssertionError(String.format("failed to log request %s: %s", request, e));
            }
        }

        private void replicateRequest(MethodInvocationRequest request) {
            this.requestReplicator.replicate(null);
        }

        private String getClientId() {
            return String.valueOf(identityHashCode(this));
        }

        @Override
        protected void finalize() throws Throwable {
            invocationExecutor.shutdown();

            super.finalize();
        }
    }

    /**
     * Group policy
     */
    public static class GroupPolicy extends DefaultGroupPolicy {
        /**
         * @return master server, or <code>null</code> if no master available
         */
        public SapphireServerPolicy getMaster() {
            // TODO (Terry): to be implemented
            return null;
        }

        /**
         * @return slave servers
         */
        public ArrayList<SapphireServerPolicy> getSlaves() {
            // TODO (Terry): to be implemented
            return new ArrayList(Collections.<SapphireServerPolicy>emptyList());
        }

        /**
         * @return a random server from the group or <code>null</code> if no server exists
         */
        public SapphireServerPolicy getRandomServer(ArrayList<SapphireServerPolicy> servers) {
            if (servers == null || servers.size() <= 0) {
                return null;
            }

            return servers.get(random.nextInt(Integer.MAX_VALUE) % servers.size());
        }

        /**
         * Lock is a tuple of (clientId, logIndex, lastUpdatedTimestamp) in which clientId the Id of
         * the client who owns the lock, logIndex is the largest log index reported by the client,
         * and lastUpdatedTimestamp is the timestamp when the lock was updated.
         *
         * Lock will be renewed iff 1) the clientId equals the clientId in the lock, and 2) the
         * current lock has not expired which means the lastUpdatedTimestamp of the lock is still
         * within the threshold
         *
         * @param clientId Id of the client
         * @return <code>true</code> if lock renew succeeds; <code>false</code> otherwise
         */
        public boolean renewLock(String clientId) {
            // TODO (Terry): to be implemented
            return false;
        }

        /**
         * Lock is a tuple of (clientId, logIndex, lastUpdatedTimestamp) in which clientId the Id of
         * the client who owns the lock, logIndex is the largest log index reported by the client,
         * and lastUpdatedTimestamp is the timestamp when the lock was updated.
         *
         * The lock will be granted iff 1) the lock has expired which means the
         * <code>lastUpdatedTimestamp</code> of the current lock has passed the threshold, and 2)
         * the <code>clientIndex</code>clientIndex is greater or equal to the <code>logIndex</code>
         * in the lock.
         *
         * @param clientId the Id of the client
         * @param clientIndex the largest log entry index observed on client
         * @return <code>true</code> if lock is granted; <code>false</code> otherwise
         */
        public boolean obtainLock(String clientId, String clientIndex) {
            // TODO (Terry): to be implemented
            return false;
        }
    }
}
