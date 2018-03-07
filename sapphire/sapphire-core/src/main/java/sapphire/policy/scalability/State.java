package sapphire.policy.scalability;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
        private AsyncReplicator replicator;

        public Master(LoadBalancedMasterSlavePolicy.GroupPolicy group, ILogger<LogEntry> entryLogger, Configuration config) {
            super(StateName.MASTER);
            this.group = group;
            this.entryLogger = entryLogger;
            this.config = config;
        }

        @Override
        public void enter() {
            replicator = new AsyncReplicator(group, entryLogger, config);
        }

        @Override
        public void leave() {
            shutdownReplicator();
        }

        @Override
        protected void finalize() throws Throwable {
            shutdownReplicator();
            super.finalize();
        }

        private void shutdownReplicator() {
            if (replicator != null) {
                replicator.close();
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
        public Slave() {
            super(StateName.SLAVE);
        }

        @Override
        public void enter() {
        }

        @Override
        public void leave() {
        }
    }
}

