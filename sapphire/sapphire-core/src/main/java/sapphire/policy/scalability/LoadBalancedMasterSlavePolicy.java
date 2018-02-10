package sapphire.policy.scalability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import sapphire.common.AppObject;
import sapphire.policy.DefaultSapphirePolicy;
import sapphire.runtime.MethodInvocationRequest;
import sapphire.runtime.MethodInvocationResponse;

import static sapphire.runtime.MethodInvocationRequest.MethodType.READ;
import static sapphire.runtime.MethodInvocationRequest.MethodType.WRITE;
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
 * <ol>
 *      <li>State Transition Thread:</li>
 *      <li>Object Method Invocation Thread:</li>
 *      <li>Replication Thread:</li>
 * </ol>
 *
 * <p>
 * <em>Limitations:</em>
 * <ol>
 *     <li>Eventual Consistency: The replication from master to slave is asynchronous, therefore
 *     read on slave replica may return stale data</li>
 *     <li>Single Failure Tolerance: This DM can only tolerate one failure at a time. For
 *     example, if master fails, slave will be promoted to master without data loss. In this case,
 *     the master failure has been handled properly. Now assuming the failed replica comes back as a
 *     slave, and starts to sync up with the master. If master fails before sync finishes, user
 *     will experience data loss.</li>
 * </ol>
 *
 * @author terryz
 */

public class LoadBalancedMasterSlavePolicy extends DefaultSapphirePolicy {

    /**
     * Client side policy
     */
    public static class ClientPolicy extends DefaultClientPolicy {
        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            GroupPolicy group = (GroupPolicy)getGroup();
            SapphireServerPolicy master = group.getMaster().get(0);

            Object ret = null;
            try {
                // TODO (Terry): Distinguish read and write
                // Since we do not have a reliable way to distinguish read operation and write
                // operation for the time being, we let master handle all operations.
                //
                // A byproduct of this approach is that users will not read stale data.
                // We may consider distinguish read operation and write operation in the future.

                // TODO (Terry): Need to expand RMI to take MethodInvocationRequest
                ret = master.onRPC(method, params);
            } catch (Exception e) {
                // handle exceptions
            }
            return ret;
        }
    }

    /**
     * Server side policy
     */
    public static class ServerPolicy extends DefaultServerPolicy {
        private static Logger logger = Logger.getLogger(LoadBalancedMasterSlavePolicy.ServerPolicy.class.getName());

        private final ScheduledExecutorService replicationExecutor = Executors.newSingleThreadScheduledExecutor();
        private final ExecutorService mInvocationExecutor = Executors.newSingleThreadExecutor();
        private final ExecutorService sTransitionExecutor = Executors.newSingleThreadExecutor();

        private final StateManager stateMgr;
        private final ILogger requstLogger;
        private final IReplicator requestReplicator;

        public ServerPolicy() {
            this.stateMgr = new StateManager(String.valueOf(System.identityHashCode(this)));
            this.requstLogger = new MemoryLogger();
            this.requestReplicator = new AsyncReplicator();
            // TODO (Terry): start async replication thread
        }

        /**
         *
         * @param request
         * @return
         */
        public MethodInvocationResponse onRPC(MethodInvocationRequest request) {
            GroupPolicy group = (GroupPolicy)getGroup();

            switch (this.stateMgr.getCurrentStateName()) {
                case CANDIDATE:
                    if (request.getMethodType() == READ) {
                        ArrayList<SapphireServerPolicy> servers = group.getServers();
                        return new MethodInvocationResponse.Builder(REDIRECT, servers).build();
                    } else if (request.getMethodType() == WRITE) {
                        // redirect to master
                        ArrayList<SapphireServerPolicy> master = group.getMaster();
                        return new MethodInvocationResponse.Builder(REDIRECT, master).build();
                    }
                    break;

                case SLAVE:
                    if (request.getMethodType() == READ) {
                        return invokeMethod(request);
                    } else if (request.getMethodType() == WRITE) {
                        // redirect to master
                        ArrayList<SapphireServerPolicy> master = group.getMaster();
                        return new MethodInvocationResponse.Builder(REDIRECT, master).build();
                    }
                    break;

                case MASTER:
                    if (request.getMethodType() == READ) {
                        return invokeMethod(request);
                    } else if (request.getMethodType() == WRITE) {
                        logRequest(request);
                        return invokeMethod(request);
                    }
                    break;
            }

            throw new AssertionError("should never reach here");
        }

        @Override
        protected void finalize() throws Throwable {
            replicationExecutor.shutdown();
            mInvocationExecutor.shutdown();
            sTransitionExecutor.shutdown();
            super.finalize();
        }

        /**
         * Invokes method on dedicated {@link #mInvocationExecutor} thread
         *
         * @param request
         * @return {@link MethodInvocationResponse}
         */
        private MethodInvocationResponse invokeMethod(final MethodInvocationRequest request) {
            final AppObject targetObj = appObject;
            Future<Object> f = mInvocationExecutor.submit(new Callable<Object>() {
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
                logger.log(Level.FINE, "failed to process request {0}: {1}", new Object[]{request, e});
                resp = new MethodInvocationResponse.Builder(FAIL, e).build();
            } catch (InterruptedException e) {
                // TODO (Terry): what should we do here?
                logger.log(Level.FINE, "failed to process request {0}: {1}", new Object[]{request, e});
                resp = new MethodInvocationResponse.Builder(FAIL, e).build();
            }

            return resp;
        }

        private void logRequest(MethodInvocationRequest request) {
            LogEntry entry = new LogEntry();
            this.requstLogger.log(entry);
        }
    }

    /**
     * Group policy
     */
    public static class GroupPolicy extends DefaultGroupPolicy {

        public ArrayList<SapphireServerPolicy> getMaster() {
            return new ArrayList(Collections.<SapphireServerPolicy>emptyList());
        }

        public ArrayList<SapphireServerPolicy> getSlaves() {
            return new ArrayList(Collections.<SapphireServerPolicy>emptyList());
        }

    }
}
