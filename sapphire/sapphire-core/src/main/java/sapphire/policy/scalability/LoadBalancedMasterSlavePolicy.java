package sapphire.policy.scalability;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

import sapphire.policy.DefaultSapphirePolicy;
import sapphire.runtime.MethodInvocationRequest;

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
 * Failed replica always comes back in {@code RECOVING} state. Replica in {@code RECOVING} state
 * does not serve read or write requests, but it is able handle replication requests.
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
    private static Logger logger = Logger.getLogger(LoadBalancedMasterSlavePolicy.class.getName());

    public static class ClientPolicy extends DefaultClientPolicy {
        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            GroupPolicy group = (GroupPolicy)getGroup();
            ServerPolicy master = group.getMasterServer();

            Object ret = null;
            try {
                // TODO (Terry): Distinguish read and write
                // Since we do not have a reliable way to distinguish read operation and write
                // operation for the time being, we let master handle all operations.
                //
                // A byproduct of this approach is that users will not read stale data.
                // We may consider distinguish read operation and write operation in the future.
                ret = master.onRPC(method, params);
            } catch (Exception e) {
                // handle exceptions
            }
            return ret;
        }
    }

    public static class ServerPolicy extends DefaultServerPolicy {
        private final ScheduledExecutorService scheduler =
                Executors.newScheduledThreadPool(1);

        private final ServerState state;
        private final ILogger requstLogger;
        private final IReplicator requestReplicator;

        public ServerPolicy() {
            this.state = new ServerState();
            this.requstLogger = new FileLogger();
            this.requestReplicator = new AsyncReplicator();
        }

        @Override
        public Object onRPC(MethodInvocationRequest request) throws Exception {
            Object ret = null;
            switch (this.state.getCurrentState()) {
                case INIT:
                case CANDIDATE:
                case SLAVE:
                    throw new Exception("");
                case MASTER:
                    LogEntry entry = new LogEntry();
                    // append log
                    this.requstLogger.log(entry);
                    // execute operation
                    ret = super.onRPC(request);
                    // replicate request
                    ReplicateResponse resp = this.requestReplicator.replicate(new ReplicateRequest());
            }
            // return result
            return ret;
        }

        @Override
        protected void finalize() throws Throwable {
            scheduler.shutdown();
            super.finalize();
        }
    }

    public static class GroupPolicy extends DefaultGroupPolicy {
        public ServerPolicy getMasterServer() {
            return null;
        }
    }
}
