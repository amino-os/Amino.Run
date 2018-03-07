package sapphire.policy.scalability;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author terryz
 */
public interface State {
    /**
     * Valid state names
     */
    enum StateName {
        SLAVE, MASTER
    }

    //
    // Interface operations that every {@State} class must implement
    //

    /**
     * Method to be called when we enter a new state.
     * Suppose we transit from state A to state B, the state manager will first call
     * <code>A.leave()</code>, then call <code>B.enter()</code>.
     */
    void enter();

    /**
     * Method to be called when we leave an old state.
     * Suppose we transit from state A to state B, the state manager will first call
     * <code>A.leave()</code>, then call <code>B.enter()</code>.
     */
    void leave();

    /**
     * Returns the name of the state
     *
     * @return state name
     */
    StateName getName();

    //
    // State implementations
    //

    abstract class AbstractState implements State {
        private final StateName name;

        AbstractState(StateName name) {
            this.name = name;
        }

        @Override
        public final StateName getName() {
            return name;
        }

        @Override
        public String toString() {
            return name.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;

            if (o == null)
                return false;

            if (getClass() != o.getClass())
                return false;

            return getName() == ((State) o).getName();
        }
    }

    /**
     * <ul>
     *      <li>initialize <code>nextIndex</code> for each slave</li>
     *      <li>accept commands from clients and append new entries into append</li>
     *      <li>replicate append entries to slaves</li>
     *      <li>mark append entry committed if it is stored on a majority of servers</li>
     *      <li>renew lock periodically</li>
     *      <ul>
     *          <li>renew succeeded: stay as master</li>
     *          <li>renew failed: step down as slave if fails to renew lock</li>
     *      </ul>
     * </ul>
     */
    final class Master extends AbstractState {
        private static final Logger logger = Logger.getLogger(Master.class.getName());
        private final LoadBalancedMasterSlavePolicy.GroupPolicy group;
        private final ILogger<LogEntry> entryLogger;
        private final Configuration config;
        private final CommitExecutor commitExecutor;
        private AsyncReplicator replicator;
        private static Master instance;

        private Master(Context context) {
            super(StateName.MASTER);
            this.group = context.getGroup();
            this.entryLogger = context.getEntryLogger();
            this.config = context.getConfig();
            this.commitExecutor = context.getCommitExecutor();
        }

        public static synchronized Master getInstance(Context context) {
            if (instance == null) {
                instance = new Master(context);
            }
            return instance;
        }

        @Override
        public void enter() {
            logger.log(Level.FINE, "enter master state");
            if (commitExecutor != null) {
                commitExecutor.open();
            }
            replicator = new AsyncReplicator(group, entryLogger, config);
        }

        @Override
        public void leave() {
            logger.log(Level.FINE, "leave master state");
            closeCommitExecutor();
            shutdownReplicator();
        }

        @Override
        protected void finalize() throws Throwable {
            closeCommitExecutor();
            shutdownReplicator();
            super.finalize();
        }

        private void shutdownReplicator() {
            if (replicator != null) {
                replicator.close();
            }
        }

        private void closeCommitExecutor() {
            if (commitExecutor != null) {
                commitExecutor.close();
            }
        }
    }

    /**
     * <ul>
     *      <li>serves read requests</li>
     *      <li>respond to <code>AppendEntries</code> RPC from master</li>
     *      <li>maintains <code>lastAppliedIndex</code> and <code>lastCommittedIndex</code></li>
     *      <li>whenever <code>lastCommittedIndex > lastAppliedIndex</code>,
     *      increments <code>lastAppliedIndex</code> and applies append[lastAppliedIndex]</li>
     *      <li>obtains lock from group periodically</li>
     *      <ul>
     *          <li>lock obtained: becomes master</li>
     *          <li>lock not obtained: stay as slave</li>
     *      </ul>
     * </ul>
     */
    final class Slave extends AbstractState {
        private final CommitExecutor commitExecutor;
        private static Slave instance;

        public Slave(Context context) {
            super(StateName.SLAVE);
            this.commitExecutor = context.getCommitExecutor();
        }

        public static synchronized Slave getInstance(Context context) {
            if (instance == null) {
                instance = new Slave(context);
            }
            return instance;
        }

        @Override
        public void enter() {
            if (commitExecutor != null) {
                commitExecutor.open();
            }
        }

        @Override
        public void leave() {
            closeCommitExecutor();
        }

        @Override
        protected void finalize() throws Throwable {
            closeCommitExecutor();
            super.finalize();
        }

        private void closeCommitExecutor() {
            if (commitExecutor != null) {
                commitExecutor.close();
            }
        }
    }
}

