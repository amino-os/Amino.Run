package sapphire.policy.scalability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
import static sapphire.runtime.MethodInvocationResponse.ReturnCode.FAILURE;
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
 *     <li>Log entry application: In RAFT, append entries will be committed to underlying state machine
 *     only if the entries have been replicated to majority servers. In this DM, append entries are
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
 *     <li>Master appends the write request to local append file</li>
 *     <li>Master executes the write request</li>
 *     <li>Master replicates write request to slave. Requests will be put in pending append on slave
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
                    targetServer = group.getRandomServer(group.getServerPolicies());
                }

                if (targetServer == null) {
                    throw new Exception(String.format("unable to find target server from server list: %s", group.getServers()));
                }

                MethodInvocationRequest request = MethodInvocationRequest.newBuilder().methodName(method).params(params).build();
                MethodInvocationResponse response = targetServer.onRPC(request);
                switch (response.getReturnCode()) {
                    case SUCCESS:
                        return response.getResult();
                    case FAILURE:
                        // No need to retry application errors
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
        private final ILogger requestLogger;
        private final IReplicator asyncReplicator;

        public ServerPolicy() {
            this.config = Configuration.newBuilder()
                    .masterLeaseRenewIntervalInMillis(Master_Lease_Renew_Interval_InMillis)
                    .masterLeaseTimeoutInMIllis(Master_Lease_Timeout_InMillis)
                    .initDelayLimitInMillis(Init_Delay_Limit_InMillis)
                    .build();

            // TODO (Terry): we should try to avoid doing cast here
            LoadBalancedMasterSlavePolicy.GroupPolicy group = (LoadBalancedMasterSlavePolicy.GroupPolicy)getGroup();
            this.stateMgr = new StateManager(getClientId(), group, this.config);

            try {
                this.requestLogger = new FileLogger(config.getLogFilePath(), config.getSnapshotFilePath(), true);
            } catch (Exception e) {
                throw new AssertionError("failed to construct entry logger: {0}", e);
            }

            List<ServerPolicy> slaves = group.getSlaves();
            this.asyncReplicator = new AsyncReplicator(slaves.get(0), requestLogger);

            logger.log(Level.INFO, "LoadBalancedMasterSlavePolicy$ServerPolicy starts successfully");
        }

        /**
         * Handles replication requests from master. This method will only be invoked on slaves.
         *
         * 1. Handle duplicated entries properly. Do nothing if log entry already exists.
         * 2. Log entries must be appended in order (according to index)
         * 3. Previous entry must exist before appending the current entry
         *
         * @param request replication request
         * @return replication response
         */
        public ReplicationResponse handleReplication(ReplicationRequest request) {

            if (request == null || request.getEntries() == null || request.getEntries().size() == 0) {
                return ReplicationResponse.newBuilder()
                        .returnCode(ReplicationResponse.ReturnCode.SUCCESS)
                        .result(this.requestLogger.getIndexOfLargestReceivedEntry())
                        .build();
            }

            long previousIndex = request.getIndexOfPreviousSyncedEntry();
            if (! this.requestLogger.indexExists(previousIndex)) {
                return ReplicationResponse.newBuilder()
                        .returnCode(ReplicationResponse.ReturnCode.TRACEBACK)
                        .result(this.requestLogger.getIndexOfLargestReceivedEntry())
                        .build();
            }

            for (Entry entry : request.getEntries()) {
                if (entry.getIndex() > previousIndex) {
                    if (! this.requestLogger.indexExists(entry.getIndex())) {
                        try {
                            this.requestLogger.append(entry);
                            // TODO (Terry): add job into invocationExecutor
                        } catch (Exception e) {
                            logger.log(Level.SEVERE, "failed to append log entry {0}: {1}", new Object[]{entry, e});
                            return ReplicationResponse.newBuilder()
                                    .returnCode(ReplicationResponse.ReturnCode.FAILURE)
                                    .result(e)
                                    .build();
                        }
                    }

                    previousIndex = entry.getIndex();
                } else {
                    return ReplicationResponse.newBuilder()
                            .returnCode(ReplicationResponse.ReturnCode.FAILURE)
                            .result(new Exception(String.format("log entries out of order in request %s", request)))
                            .build();
                }
            }

            return ReplicationResponse.newBuilder()
                    .returnCode(ReplicationResponse.ReturnCode.SUCCESS)
                    .build();
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
                    return MethodInvocationResponse.newBuilder()
                            .returnCode(REDIRECT)
                            .result(null)
                            .build();

                case MASTER:
                    if (request.getMethodType() != null && request.getMethodType() == READ) {
                        return invokeMethod(request);
                    }

                    // Log entry replication will be executed asynchronously because
                    // we need to return the client regardless whether or not replication
                    // succeeds.
                    Entry entry = LogEntry.newBuilder().request(request).build();
                    try {
                        this.requestLogger.append(entry);
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Invocation of request {0} failed: {1}", new Object[]{request, e});
                        return MethodInvocationResponse.newBuilder()
                                .returnCode(MethodInvocationResponse.ReturnCode.FAILURE)
                                .result(e)
                                .build();
                    }

                    MethodInvocationResponse resp = invokeMethod(request);
                    requestLogger.markCommitted(entry);

                    return resp;
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
                resp = MethodInvocationResponse.newBuilder().returnCode(SUCCESS).result(ret).build();
            } catch (ExecutionException e) {
                // Method invocation on application object failed.
                // This is caused by application errors, not Sapphire errors
                AppExecutionException ex = new AppExecutionException("method invocation on app object failed", e);
                logger.log(Level.FINE, "failed to process request {0} on {1}: {2}", new Object[]{request, appObject, ex});
                resp = MethodInvocationResponse.newBuilder().returnCode(FAILURE).result(ex).build();
            } catch (InterruptedException e) {
                // TODO (Terry): what should we do here?
                // This is likely caused by Sapphire errors.
                // The problem is: the other replica may not run into this exception. This exception
                // may cause two replicas out of sync.
                AppExecutionException ex = new AppExecutionException("Method invocation on appobject was interrupted", e);
                logger.log(Level.SEVERE, "the process of request {0} on {1} was interrupted: {2}", new Object[]{request, appObject, ex});
                resp = MethodInvocationResponse.newBuilder().returnCode(FAILURE).result(ex).build();
            }

            return resp;
        }

        private String getClientId() {
            return String.valueOf(identityHashCode(this));
        }

        @Override
        protected void finalize() throws Throwable {
            invocationExecutor.shutdown();
            if (asyncReplicator != null) {
                asyncReplicator.close();
            }

            super.finalize();
        }
    }

    /**
     * Group policy
     */
    public static class GroupPolicy extends DefaultGroupPolicy {
        private Lock masterLock;
        private Configuration config;

        public List<ServerPolicy> getServerPolicies() {
            ArrayList<SapphireServerPolicy> servers = super.getServers();
            List<ServerPolicy> result = new ArrayList<ServerPolicy>();
            for (SapphireServerPolicy s: servers) {
                result.add((ServerPolicy)s);
            }
            return result;
        }

        /**
         * @return master server, or <code>null</code> if no master available
         */
        public ServerPolicy getMaster() {
            // TODO (Terry): to be implemented
            return null;
        }

        // TODO (Terry): Group should broadcast membership change events
        // Master state should listen to slave-change events
        /**
         * @return slave servers
         */
        public List<ServerPolicy> getSlaves() {
            // TODO (Terry): to be implemented
            return new ArrayList(Collections.<SapphireServerPolicy>emptyList());
        }

        /**
         * @return a random server from the group or <code>null</code> if no server exists
         */
        public ServerPolicy getRandomServer(List<ServerPolicy> servers) {
            if (servers == null || servers.size() <= 0) {
                return null;
            }

            return servers.get(random.nextInt(Integer.MAX_VALUE) % servers.size());
        }

        /**
         * Renew lock
         *
         * @param clientId Id of the client
         * @param clientIndex largest append index observed on client
         * @return <code>true</code> if lock renew succeeds; <code>false</code> otherwise
         */
        public boolean renewLock(String clientId, long clientIndex) {
            if (clientId == null || clientId.isEmpty()) {
                throw new IllegalArgumentException("clientId not specified");
            }

            if (masterLock == null) {
                return false;
            }

            return masterLock.renew(clientId, clientIndex);
        }

        /**
         * Obtain lock
         *
         * @param clientId the Id of the client
         * @param clientIndex the largest append entry logIndex observed on client
         * @return <code>true</code> if lock is granted; <code>false</code> otherwise
         */
        public boolean obtainLock(String clientId, long clientIndex) {
            if (masterLock == null) {
                masterLock = new Lock(clientId, clientIndex, config.getMasterLeaseTimeoutInMillis());
                return true;
            }

            return masterLock.obtain(clientId, clientIndex);
        }

        public void setConfig(Configuration config) {
            if (config == null) {
                throw new IllegalArgumentException("config is not specified");
            }

            this.config = config;
        }
    }
}
