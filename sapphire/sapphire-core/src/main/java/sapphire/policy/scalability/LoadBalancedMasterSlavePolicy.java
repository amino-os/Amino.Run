package sapphire.policy.scalability;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import sapphire.common.Utils;
import sapphire.policy.DefaultSapphirePolicy;
import sapphire.runtime.MethodInvocationRequest;
import sapphire.runtime.MethodInvocationResponse;
import sapphire.runtime.annotations.Runtime;
import sapphire.runtime.exception.AppExecutionException;

import static sapphire.runtime.MethodInvocationResponse.ReturnCode.REDIRECT;

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
 * Failed replica always comes back in {@code RECOVERING} stateMgr. Replica in {@code RECOVERING}
 * stateMgr does not serve read or write requests, but it is able handle replication requests.
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
        private final SequenceGenerator SeqGenerator = SequenceGenerator.newBuilder().startingAt(0).step(1).build();
        private final String CLIENT_ID;

        private boolean isImmutableMethod(String methodName, ArrayList<Object> params) {
            Class<?> clazz = getServer().sapphire_getAppObject().getObject().getClass();
            return Utils.isImmutableMethod(clazz, methodName, params);
        }

        public ClientPolicy() {
            super();
            CLIENT_ID = "Client_" + System.currentTimeMillis();
        }

        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            int cnt=1, retry=5;
            long waitInMilliseconds = 50L;
            GroupPolicy group = (GroupPolicy) getGroup();

            // TODO (Terry): Wrap with a generic exponential backoff retryer
            do {
                SapphireServerPolicy targetServer = group.getMaster();

                if (isImmutableMethod(method, params)) {
                    targetServer = group.getSlave();
                }

                if (targetServer == null) {
                    throw new Exception(String.format("unable to find target server from server list: %s", group.getServers()));
                }

                MethodInvocationRequest request = MethodInvocationRequest.newBuilder()
                        .clientId(CLIENT_ID)
                        .requestId(SeqGenerator.getNextSequence())
                        .methodName(method)
                        .params(params)
                        .build();
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
    @Runtime(replicas=3)
    public static class ServerPolicy extends DefaultServerPolicy {
        private final Logger logger = Logger.getLogger(LoadBalancedMasterSlavePolicy.ServerPolicy.class.getName());

        private final String SERVER_ID;
        private Configuration config;
        private FileLogger requestLogger;
        private CommitExecutor commitExecutor;
        private StateManager stateMgr;

        public ServerPolicy() {
            SERVER_ID = "Server_" + System.currentTimeMillis();
        }

        @Override
        public void onCreate(SapphireGroupPolicy group) {
            super.onCreate(group);
            GroupPolicy groupPolicy = (GroupPolicy)getGroup();

            config = Configuration.newBuilder().build();
            Context context = Context.newBuilder()
                    .group(groupPolicy)
                    .entryLogger(requestLogger)
                    .config(config)
                    .build();

            this.stateMgr = new StateManager(SERVER_ID, context);

            // serverPolicy.$__initialize sets the appObject.
            // It must be called before onCreate.
            // Otherwise, NPE will be thrown from CommitExecutor.getInstance.
            commitExecutor = CommitExecutor.getInstance(appObject, 0L, config);

            try {
                this.requestLogger = new FileLogger(config, commitExecutor, true);
            } catch (Exception e) {
                throw new AssertionError("failed to construct entry logger: {0}", e);
            }

            logger.log(Level.INFO, "LoadBalancedMasterSlavePolicy$ServerPolicy created");
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

            for (LogEntry entry : request.getEntries()) {
                if (entry.getIndex() > previousIndex) {
                    if (! this.requestLogger.indexExists(entry.getIndex())) {
                        try {
                            this.requestLogger.append(entry);
                            this.requestLogger.markReplicated(entry.getIndex());
                            commitExecutor.applyWriteAsync(entry.getRequest(), entry.getIndex());
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
                    if (request.isImmutable()) {
                        return commitExecutor.applyRead(request);
                    }
                    // redirect non-read operations to master
                    return MethodInvocationResponse.newBuilder()
                            .returnCode(REDIRECT)
                            .result(null)
                            .build();

                case MASTER:
                    if (request.isImmutable()) {
                        return commitExecutor.applyRead(request);
                    }

                    LogEntry entry = LogEntry.newBuilder().request(request).build();
                    try {
                        this.requestLogger.append(entry);
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Invocation of request {0} failed: {1}", new Object[]{request, e});
                        return MethodInvocationResponse.newBuilder()
                                .returnCode(MethodInvocationResponse.ReturnCode.FAILURE)
                                .result(e)
                                .build();
                    }

                    return commitExecutor.applyWriteSync(request, entry.getIndex());
            }

            throw new AssertionError("should never reach here");
        }

        @Override
        protected void finalize() throws Throwable {
            if (requestLogger != null) {
                requestLogger.close();
            }

            if (commitExecutor != null) {
                commitExecutor.close();
            }
            super.finalize();
        }

        public String getServerId() {
            return SERVER_ID;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ServerPolicy)) return false;
            ServerPolicy that = (ServerPolicy) o;
            return Objects.equals(SERVER_ID, that.SERVER_ID);
        }

        @Override
        public int hashCode() {
            return Objects.hash(SERVER_ID);
        }
    }

    /**
     * Group policy
     *
     * TODO (Terry): Make Group Policy highly available.
     * At present group policy has only one instance which
     * does not satisfy the high available requirement.
     */
    public static class GroupPolicy extends DefaultGroupPolicy {
        private Lock masterLock;

        /**
         * @return master server, or <code>null</code> if no master available
         */
        public ServerPolicy getMaster() {
            List<ServerPolicy> servers = getServerPolicies();
            if (servers.size() != 2) {
                throw new IllegalStateException("");
            }

            for (ServerPolicy s : servers) {
                if (s.getServerId() == masterLock.getClientId()) {
                    return s;
                }
            }
            return null;
        }

        // TODO (Terry): Group should broadcast membership change events
        // Master state should listen to slave-change events
        public ServerPolicy getSlave() {
            ServerPolicy master = getMaster();
            List<ServerPolicy> servers = getServerPolicies();
            for (ServerPolicy s : servers) {
                if (! s.equals(master)) {
                    return s;
                }
            }
            return null;
        }

        /**
         * Renew lock
         *
         * @param serverId Id of the server
         * @return <code>true</code> if lock renew succeeds; <code>false</code> otherwise
         */
        public boolean renewLock(String serverId) {
            if (serverId == null || serverId.isEmpty()) {
                throw new IllegalArgumentException("server ID not specified");
            }

            if (masterLock == null) {
                return false;
            }

            return masterLock.renew(serverId);
        }

        /**
         * Obtain lock
         *
         * @param serverId the Id of the server
         * @param masterLeaseTimeoutInMillis
         * @return <code>true</code> if lock is granted; <code>false</code> otherwise
         */
        public boolean obtainLock(String serverId, long masterLeaseTimeoutInMillis) {
            if (masterLock == null) {
                masterLock = new Lock(serverId, masterLeaseTimeoutInMillis);
                return true;
            }

            return masterLock.obtain(serverId);
        }

        private List<ServerPolicy> getServerPolicies() {
            ArrayList<SapphireServerPolicy> servers = super.getServers();
            List<ServerPolicy> result = new ArrayList<ServerPolicy>();
            for (SapphireServerPolicy s: servers) {
                result.add((ServerPolicy)s);
            }
            return result;
        }
    }
}
