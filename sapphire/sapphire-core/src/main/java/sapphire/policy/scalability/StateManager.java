package sapphire.policy.scalability;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static sapphire.policy.scalability.StateManager.Event.OBTAIN_LOCK_FAILED;
import static sapphire.policy.scalability.StateManager.Event.OBTAIN_LOCK_SUCCEEDED;
import static sapphire.policy.scalability.StateManager.Event.RENEW_LOCK_FAILED;
import static sapphire.policy.scalability.StateManager.Event.RENEW_LOCK_SUCCEEDED;
import static sapphire.policy.scalability.StateManager.LockOp.OBTAIN_LOCK;
import static sapphire.policy.scalability.StateManager.LockOp.RENEW_LOCK;

/**
 * {@code StateManager} uses a state machine to manage server states.
 * <p>
 * Server states are defined in {@link State}. State transitions are defined in
 * {@link StateManager#stateTransition(Event)}. Events that trigger state stateTransition are defined in
 * {@link Event}.
 *
 * @author terryz
 */
public final class StateManager {
    /**
     * Events that trigger state stateTransition
     */
    public enum Event {
        OBTAIN_LOCK_SUCCEEDED, OBTAIN_LOCK_FAILED, RENEW_LOCK_SUCCEEDED, RENEW_LOCK_FAILED
    }

    /**
     * Lock operations
     */
    public enum LockOp {
        OBTAIN_LOCK, RENEW_LOCK
    }

    private final Logger logger = Logger.getLogger(State.Master.class.getName());
    private final Random random = new Random(System.currentTimeMillis());

    private final State SLAVE_STATE;
    private final State MASTER_STATE;
    private ScheduledExecutorService lockingExecutor;

    /**
     * The Id of the client in which this <code>StateManager</code> instance resides.
     * <p>
     * Each DM server has its own state manager. Multiple DM servers may run in a single JVM.
     * Client Id is used to distinguish state managers of different DM servers.
     */
    private final String clientId;

    /**
     * Name of this state manager
     */
    private final String name;

    /**
     * The current state of the state manager
     */
    private State currentState;

    /**
     *
     */
    private final LoadBalancedMasterSlavePolicy.GroupPolicy group;

    /**
     *
     */
    private final Configuration config;

    /**
     * Constructor
     *
     * @param clientId the clientId of the state manager
     * @param group the group policy
     * @param config configuration
     */
    public StateManager(String clientId, LoadBalancedMasterSlavePolicy.GroupPolicy group, Configuration config) {
        this.clientId = clientId;
        this.name = String.format("StateManager_%s_%s", clientId, random.nextInt(Integer.MAX_VALUE));
        this.config = config;
        this.group = group;

        this.SLAVE_STATE = new State.Slave();
        this.MASTER_STATE = new State.Master();

        this.currentState = SLAVE_STATE;
        startLockingExecutor();
    }

    /**
     * Transits from the current state to the next state for the given {@link Event} based on the
     * following state transition diagram.
     * <p>
     *            |
     *            | Starts up
     *            |
     *            | load checkpoint /
     *            | execute pending requests
     *            v
     *         -------
     *        | Slave |
     *         -------
     *          |   ^
     * get lock |   | cannot renew lock
     *          |   |
     *          V   |
     *        --------
     *       | Master |
     *        --------
     */
    public synchronized final void stateTransition(Event event) {
        switch (getCurrentStateName()) {
            case MASTER:
                if (event == Event.RENEW_LOCK_FAILED) {
                    // step down to slave
                    switchTo(SLAVE_STATE);
                } else if (event == Event.RENEW_LOCK_SUCCEEDED) {
                    // stay as master
                    // do nothing
                } else {
                    throw new AssertionError(String.format("State %s cannot handle event %s", getCurrentStateName(), event));
                }
                break;
            case SLAVE:
                if (event == Event.OBTAIN_LOCK_FAILED) {
                    // stay as slave
                    // do nothing
                } else if (event == Event.OBTAIN_LOCK_SUCCEEDED) {
                    // step up to master
                    switchTo(MASTER_STATE);
                } else {
                    throw new AssertionError(String.format("State %s cannot handle event %s", getCurrentStateName(), event));
                }
                break;
        }
    }

    /**
     * Switches from the current state to the next state. Stay in the current state if any
     * error occurs during transition.
     *
     * @param nextState the next state
     */
    private synchronized void switchTo(State nextState) {
        State previousState = currentState;
        try {
            previousState.leave();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "failed to leave state {0}: {1}", new Object[]{previousState, e});
            return;
        }

        currentState = nextState;
        try {
            currentState.enter();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "failed to enter state {0}: {1}", new Object[]{currentState, e});
            currentState = previousState;
        }
    }

    private void startLockingExecutor() {
        if (lockingExecutor == null || lockingExecutor.isShutdown()) {
            lockingExecutor = Executors.newSingleThreadScheduledExecutor();
        }

        lockingExecutor.scheduleWithFixedDelay(
                Util.RunnerWrapper(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            switch (getCurrentState().getName()) {
                                case SLAVE:
                                    stateTransition(obtainLock(OBTAIN_LOCK) ? OBTAIN_LOCK_SUCCEEDED : OBTAIN_LOCK_FAILED);
                                    break;

                                case MASTER:
                                    stateTransition(obtainLock(RENEW_LOCK) ? RENEW_LOCK_SUCCEEDED : RENEW_LOCK_FAILED);
                                    break;
                            }
                        } catch (Throwable e) {
                            logger.log(Level.FINE, "unable to obtain lock: {0}", e);
                        }
                    }
                }), config.getInitDelayLimitInMillis(), config.getMasterLeaseRenewIntervalInMillis(), TimeUnit.MILLISECONDS);
    }


    private void shutdownLockingExecutor() {
        try {
            if (lockingExecutor != null) {
                lockingExecutor.shutdown();
                lockingExecutor.awaitTermination(config.getShutdownGracePeriodInMillis(), TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "replication thread interrupted during await termination: {0}", e);
        }
    }

    private boolean obtainLock(LockOp mode) {
        boolean lockObtained = false;
        try {
            if (mode == RENEW_LOCK) {
                lockObtained = group.renewLock(clientId, Long.valueOf(0));
            } else if (mode == OBTAIN_LOCK) {
                // TODO (Terry): Fix clientIndex
                lockObtained = group.obtainLock(clientId, Long.valueOf(0));
            } else {
                throw new AssertionError("invalid lock mode " + mode);
            }
        } catch (Throwable e) {
            logger.log(Level.FINE, "unable to {0} lock: {1}", new Object[]{mode, e});
        }
        return lockObtained;
    }

    /**
     * Returns the name of the current state
     *
     * @return the name of the current state
     */
    public final State.StateName getCurrentStateName() {
        return currentState.getName();
    }

    public final State getCurrentState() {
        return currentState;
    }

    @Override
    protected void finalize() throws Throwable {
        shutdownLockingExecutor();
        super.finalize();
    }

    @Override
    public String toString() {
        return name;
    }
}
