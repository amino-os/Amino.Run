package sapphire.policy.scalability;

import static sapphire.policy.scalability.masterslave.MethodInvocationResponse.ReturnCode.REDIRECT;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import sapphire.app.SapphireObjectSpec;
import sapphire.policy.scalability.masterslave.*;

/**
 * A master slave DM that replicates requests from master to slave synchronously in best effort. It
 * stops replication if the slave is down or is not reachable. When slave is back online, master
 * will sync up slave's object state and enable replications.
 *
 * <p>TODO: Make Group Policy highly available. Unlink PAXOS or RAFT, Master Slave DM is not able to
 * decide the leader by itself when run in two nodes. It relies on some arbitrator to decide the
 * leader. In this implementation, it is the Group policy who decides the leader. The Group policy
 * itself has only one instance - it is not highly available. This should be addressed after we
 * integrates Sapphire with Diamond and Tapir. TODO: Support multiple slaves and enable
 * semi-synchronous replication
 */
public class LoadBalancedMasterSlaveSyncPolicy extends LoadBalancedMasterSlaveBase {

    /** Client side policy */
    public static class ClientPolicy extends ClientBase {}

    /** Server side policy */
    public static class ServerPolicy extends ServerBase {
        private transient Logger logger;
        private transient Committer commitExecutor;
        private transient StateManager stateMgr;
        private transient Processor processor;

        @Override
        public void onCreate(SapphireGroupPolicy group, SapphireObjectSpec spec) {
            super.onCreate(group, spec);
        }

        @Override
        public void start() {
            logger = Logger.getLogger(ServerPolicy.class.getName());
            GroupPolicy groupPolicy = (GroupPolicy) getGroup();

            Configuration config = new Configuration();
            RequestReplicator replicator = new RequestReplicator(config, groupPolicy);
            replicator.open();

            commitExecutor = new Committer(appObject, 0L, config);
            commitExecutor.open();
            processor = new Processor(config, groupPolicy, commitExecutor, replicator);
            processor.open();

            Context context = new Context(groupPolicy, config, commitExecutor, replicator);

            this.stateMgr = new StateManager(getServerId(), context);
            logger.log(Level.INFO, "LoadBalancedMasterSlavePolicy$ServerPolicy created");
        }

        @Override
        public void syncObject(Serializable object, long largestCommittedIndex) {
            commitExecutor.updateObject(object, largestCommittedIndex);
        }

        /**
         * Handles replication requests from master. This method will only be invoked on slaves.
         *
         * <p>1. Log entries must be appended in order (according to index) 2. Previous entry must
         * exist before appending the current entry
         *
         * <p>Since we have only one master and master uses one thread to do replications, there is
         * no need to synchronize on this method.
         *
         * @param request replication request
         * @return replication response
         */
        @Override
        public ReplicationResponse handleReplication(ReplicationRequest request) {
            if (request == null
                    || request.getEntries() == null
                    || request.getEntries().size() == 0) {
                return new ReplicationResponse(ReplicationResponse.ReturnCode.SUCCESS, null);
            }

            if (request.getIndexOfLargestCommittedEntry()
                    > commitExecutor.getIndexOfLargestCommittedEntry()) {
                // Last committed index on master does not exist on Slave.
                // Slave is out of date
                return new ReplicationResponse(
                        ReplicationResponse.ReturnCode.TRACEBACK,
                        commitExecutor.getIndexOfLargestCommittedEntry());
            }

            for (LogEntry entry : request.getEntries()) {
                // TODO (Terry): apply write async
                commitExecutor.applyWriteSync(entry.getRequest(), entry.getIndex());
            }

            return new ReplicationResponse(ReplicationResponse.ReturnCode.SUCCESS, null);
        }

        /**
         * invoke the given request on App object
         *
         * @param request method invocation request
         * @return method invocation response
         */
        public MethodInvocationResponse onRPC(MethodInvocationRequest request) {
            if (request.isImmutable()) {
                return commitExecutor.applyRead(request);
            }

            switch (this.stateMgr.getCurrentState().getName()) {
                case SLAVE:
                    return new MethodInvocationResponse(REDIRECT, null);

                case MASTER:
                    return processor.process(request);
            }

            throw new AssertionError("should never reach here");
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            try {
                finalize();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }

        @Override
        protected void finalize() throws Throwable {
            if (commitExecutor != null) {
                commitExecutor.close();
                commitExecutor = null;
            }

            if (processor != null) {
                processor.close();
                processor = null;
            }

            if (stateMgr != null) {
                stateMgr.halt();
                stateMgr = null;
            }
            super.finalize();
        }
    }

    /** Group policy */
    public static class GroupPolicy extends LoadBalancedMasterSlaveBase.GroupBase {}
}
