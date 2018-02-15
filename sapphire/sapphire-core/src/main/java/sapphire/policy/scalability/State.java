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
        private final Random random = new Random(System.currentTimeMillis());

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
        private final Random random = new Random(System.currentTimeMillis());
        private final int INIT_DELAY_IN_MILLIS = random.nextInt(500);
        private final long PERIOD_IN_MILLIS = 500;

        private ScheduledExecutorService replicationExecutor;

        public Master() {
            super(StateName.MASTER);
        }

        @Override
        public void enter() {
            startReplicationThread();
        }

        @Override
        public void leave() {
            shutdownReplicationThread();
        }

        private void startReplicationThread() {
            if (replicationExecutor == null || replicationExecutor.isShutdown()) {
                replicationExecutor = Executors.newSingleThreadScheduledExecutor();
            }

            replicationExecutor.scheduleAtFixedRate(
                    // wrap runnable with try-catch block
                    Util.RunnerWrapper(new Runnable() {
                        @Override
                        public void run() {
                            // TODO (Terry): doing replication
                            try {
                                Thread.sleep(100);
                            } catch(Exception ex) {

                            }
                        }
                    }), INIT_DELAY_IN_MILLIS, PERIOD_IN_MILLIS, TimeUnit.MILLISECONDS);
        }

        private void shutdownReplicationThread() {
            try {
                if (replicationExecutor != null) {
                    replicationExecutor.shutdown();
                    replicationExecutor.awaitTermination(PERIOD_IN_MILLIS, TimeUnit.MILLISECONDS);
                }
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, "replication thread interrupted during await termination: {0}", e);
            }
        }

        @Override
        protected void finalize() throws Throwable {
            shutdownReplicationThread();
            super.finalize();
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

