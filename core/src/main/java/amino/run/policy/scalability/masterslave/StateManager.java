package amino.run.policy.scalability.masterslave;

import static amino.run.policy.scalability.masterslave.StateManager.Event.OBTAIN_LOCK_FAILED;
import static amino.run.policy.scalability.masterslave.StateManager.Event.OBTAIN_LOCK_SUCCEEDED;
import static amino.run.policy.scalability.masterslave.StateManager.Event.RENEW_LOCK_FAILED;
import static amino.run.policy.scalability.masterslave.StateManager.Event.RENEW_LOCK_SUCCEEDED;
import static amino.run.policy.scalability.masterslave.StateManager.LockOp.OBTAIN_LOCK;
import static amino.run.policy.scalability.masterslave.StateManager.LockOp.RENEW_LOCK;

import amino.run.common.Utils;
import amino.run.policy.scalability.LoadBalancedMasterSlaveBase;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@code StateManager} uses a state machine to manage server states.
 *
 * <p>Server states are defined in {@link State}. State transitions are defined in {@link
 * StateManager#stateTransition(Event)}. Events that trigger state stateTransition are defined in
 * {@link Event}.
 *
 * @author terryz
 */
public final class StateManager {
    /** Events that trigger state stateTransition */
    public enum Event {
        OBTAIN_LOCK_SUCCEEDED,
        OBTAIN_LOCK_FAILED,
        RENEW_LOCK_SUCCEEDED,
        RENEW_LOCK_FAILED
    }

    /** Lock operations */
    public enum LockOp {
        OBTAIN_LOCK,
        RENEW_LOCK
    }

    private static final Logger logger = Logger.getLogger(State.Master.class.getName());

    private final State SLAVE_STATE;
    private final State MASTER_STATE;
    private ScheduledExecutorService lockingExecutor;

    /**
     * The Id of the server which this <code>StateManager</code> manages.
     *
     * <p>Each server policy has its own state manager. In other words, each replica (or server) has
     * its own state manager.
     */
    private final String serverId;

    /** Name of this state manager */
    private final String name;

    /** The current state of the state manager */
    private State currentState;

    private final LoadBalancedMasterSlaveBase.GroupPolicy group;

    private final Configuration config;

    public StateManager(String serverId, Context context) {
        this.serverId = serverId;
        this.name = String.format("StateManager_%s", serverId);
        this.config = context.getConfig();
        this.group = (LoadBalancedMasterSlaveBase.GroupPolicy) context.getGroup();

        this.SLAVE_STATE = State.Slave.getInstance(context);
        this.MASTER_STATE = State.Master.getInstance(context);

        this.currentState = SLAVE_STATE;
        startLockingExecutor();
    }

    /**
     * Transits from the current state to the next state for the given {@link Event} based on the
     * following state transition diagram.
     *
     * <p>| | Starts up | | load checkpoint / | execute pending requests v ------- | Slave | -------
     * | ^ get lock | | cannot renew lock | | V | -------- | Master | --------
     */
    public final synchronized void stateTransition(Event event) {
        switch (getCurrentState().getName()) {
            case MASTER:
                if (event == Event.RENEW_LOCK_FAILED) {
                    // step down to slave
                    switchTo(SLAVE_STATE);
                } else if (event == Event.RENEW_LOCK_SUCCEEDED) {
                    // stay as master
                    // do nothing
                } else {
                    throw new AssertionError(
                            String.format(
                                    "State %s cannot handle event %s",
                                    getCurrentState().getName(), event));
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
                    throw new AssertionError(
                            String.format(
                                    "State %s cannot handle event %s",
                                    getCurrentState().getName(), event));
                }
                break;
        }
    }

    /**
     * Switches from the current state to the next state. Stay in the current state if any error
     * occurs during transition.
     *
     * @param nextState the next state
     */
    private synchronized void switchTo(State nextState) {
        State previousState = currentState;
        try {
            previousState.leave();
        } catch (Exception e) {
            logger.log(
                    Level.SEVERE,
                    "failed to leave state {0}: {1}",
                    new Object[] {previousState, e});
            return;
        }

        try {
            nextState.enter();
        } catch (Exception e) {
            logger.log(
                    Level.SEVERE, "failed to enter state {0}: {1}", new Object[] {currentState, e});
            currentState = previousState;
        }

        currentState = nextState;
    }

    public final synchronized State getCurrentState() {
        return currentState;
    }

    private void startLockingExecutor() {
        if (lockingExecutor == null || lockingExecutor.isShutdown()) {
            lockingExecutor = Executors.newSingleThreadScheduledExecutor();
        }

        lockingExecutor.scheduleWithFixedDelay(
                Utils.RunnerWrapper(
                        new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    switch (getCurrentState().getName()) {
                                        case SLAVE:
                                            stateTransition(
                                                    obtainLock(OBTAIN_LOCK)
                                                            ? OBTAIN_LOCK_SUCCEEDED
                                                            : OBTAIN_LOCK_FAILED);
                                            break;

                                        case MASTER:
                                            stateTransition(
                                                    obtainLock(RENEW_LOCK)
                                                            ? RENEW_LOCK_SUCCEEDED
                                                            : RENEW_LOCK_FAILED);
                                            break;
                                    }
                                } catch (Throwable e) {
                                    logger.log(Level.FINE, "unable to obtain lock: {0}", e);
                                }
                            }
                        }),
                config.getInitDelayLimitInMillis(),
                config.getMasterLeaseRenewIntervalInMillis(),
                TimeUnit.MILLISECONDS);
    }

    private void shutdownLockingExecutor() {
        try {
            if (lockingExecutor != null) {
                lockingExecutor.shutdown();
                lockingExecutor.awaitTermination(
                        config.getShutdownGracePeriodInMillis(), TimeUnit.MILLISECONDS);
                lockingExecutor = null;
            }
        } catch (InterruptedException e) {
            logger.log(
                    Level.WARNING,
                    "replication thread interrupted during await termination: {0}",
                    e);
        }
    }

    private boolean obtainLock(LockOp mode) {
        boolean lockObtained = false;
        try {
            if (mode == RENEW_LOCK) {
                lockObtained = group.renewLock(serverId);
            } else if (mode == OBTAIN_LOCK) {
                lockObtained = group.obtainLock(serverId, config.getMasterLeaseTimeoutInMillis());
            } else {
                throw new AssertionError("invalid lock mode " + mode);
            }
        } catch (Throwable e) {
            logger.log(Level.FINE, "unable to {0} lock: {1}", new Object[] {mode, e});
        }
        return lockObtained;
    }

    public void halt() {
        shutdownLockingExecutor();
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
