package amino.run.policy.scalability.masterslave;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** @author terryz */
public interface State {
    /** Valid state names */
    enum StateName {
        SLAVE,
        MASTER
    }

    //
    // Interface operations that every {@State} class must implement
    //

    /**
     * Method to be called when we enter a new state. Suppose we transit from state A to state B,
     * the state manager will first call <code>A.leave()</code>, then call <code>B.enter()</code>.
     */
    void enter();

    /**
     * Method to be called when we leave an old state. Suppose we transit from state A to state B,
     * the state manager will first call <code>A.leave()</code>, then call <code>B.enter()</code>.
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
            if (this == o) return true;

            if (o == null) return false;

            if (getClass() != o.getClass()) return false;

            return getName() == ((State) o).getName();
        }
    }

    /**
     * Represents master state in a master/slave set up.
     *
     * <p>Servers in master state performs the following operations.
     *
     * <ul>
     *   <li>initialize <code>nextIndex</code> for each slave
     *   <li>accept commands from clients and append new entries into append
     *   <li>replicate append entries to slaves
     *   <li>mark append entry committed if it is stored on a majority of servers
     *   <li>renew lock periodically
     *       <ul>
     *         <li>renew succeeded: stay as master
     *         <li>renew failed: step down as slave if fails to renew lock
     *       </ul>
     * </ul>
     */
    final class Master extends AbstractState {
        private static final Logger logger = Logger.getLogger(Master.class.getName());
        private final Committer commitExecutor;
        private Replicator replicator;
        private static Master instance;

        private Master(Context context) {
            super(StateName.MASTER);
            this.commitExecutor = context.getCommitExecutor();
            this.replicator = context.getReplicator();
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
            if (replicator != null) {
                replicator.open();
            }
        }

        @Override
        public void leave() {
            logger.log(Level.FINE, "leave master state");
            closeCommitExecutor();
            closeReplicator();
        }

        @Override
        protected void finalize() throws Throwable {
            closeCommitExecutor();
            closeReplicator();
            super.finalize();
        }

        private void closeReplicator() {
            if (replicator != null) {
                try {
                    replicator.close();
                } catch (IOException e) {
                    logger.log(Level.WARNING, "failed to close replicator", e);
                }
            }
        }

        private void closeCommitExecutor() {
            if (commitExecutor != null) {
                commitExecutor.close();
            }
        }
    }

    /**
     * Represents slave state in a Master Slave set up.
     *
     * <p>Servers in slave state performs the following operations.
     *
     * <ul>
     *   <li>serves read requests
     *   <li>respond to <code>AppendEntries</code> RPC from master
     *   <li>obtains lock from group periodically
     *       <ul>
     *         <li>lock obtained: becomes master
     *         <li>lock not obtained: stay as slave
     *       </ul>
     * </ul>
     */
    final class Slave extends AbstractState {
        private static final Logger logger = Logger.getLogger(Slave.class.getName());
        private final Committer commitExecutor;
        private static Slave instance;

        private Slave(Context context) {
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
