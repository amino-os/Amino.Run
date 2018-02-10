package sapphire.policy.scalability;

import java.util.Random;

import static sapphire.policy.scalability.State.StateName.SLAVE;

/**
 * {@code StateManager} uses a state machine to manage server states.
 *
 * Server states are defined in {@link State}. State transitions are defined in
 * {@link StateManager#stateTransition(Event)}. Events that trigger state stateTransition are defined in
 * {@link Event}.
 *
 * @author terryz
 */
public final class StateManager {
    private final Random random = new Random(System.currentTimeMillis());
    private final State CANDIDATE_STATE = new State.Candidate();
    private final State SLAVE_STATE = new State.Slave();
    private final State MASTER_STATE = new State.Master();

    /**
     * The name of the {@StateManager} instance.
     *
     * Each server has its own state manager. Multiple servers may run in a single JVM. Giving
     * each state manager a name to make troubleshooting easier.
     */
    private final String name;

    /**
     * The current state of the state manager
     */
    private State currentState;

    /**
     * Constructor
     *
     * @param name the name of the state manager
     */
    public StateManager(String name) {
        this.name = String.format("StateManager_%s_%s", name, random.nextInt(Integer.MAX_VALUE));
        this.currentState = CANDIDATE_STATE;
    }

    /**
     * Events that trigger state stateTransition
     */
    public enum Event {
        OBTAIN_LOCK_SUCCEEDED, OBTAIN_LOCK_FAILED, RENEW_LOCK_SUCCEEDED, RENEW_LOCK_FAILED
    }

    /**
     * Transits from the current state to the next state for the given {@link Event} based on the
     * following state transition diagram.
     *
     *                  |
     *                  | Starts up
     *                  |
     *                  | load checkpoint /
     *                  | execute pending requests
     *                  v
     *              -----------   unable to get lock    -------
     *             | Candidate | --------------------> | Slave |
     *              -----------                         -------
     *                  |                                |   ^
     *                  |                       get lock |   | cannot renew lock
     *                  | get lock                       |   |
     *                  |                                V   |
     *                  |                               --------
     *                  +----------------------------> | Master |
     *                                                  --------
     */
    public synchronized final void stateTransition(Event event) {
        switch(getCurrentStateName()) {
            case CANDIDATE:
                if (event == Event.OBTAIN_LOCK_FAILED) {
                    // becomes a slave
                    switchTo(SLAVE_STATE);
                } else if (event == Event.OBTAIN_LOCK_SUCCEEDED) {
                    // becomes a master
                    switchTo(MASTER_STATE);
                } else {
                    throw new AssertionError(String.format("State %s cannot handle event %s", getCurrentStateName(), event));
                }
                break;
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
     * Switches from the current state to the destination state.
     *
     * @param dest the destination state
     */
    private synchronized void switchTo(State dest) {

        currentState.leave();

        currentState = dest;

        currentState.enter();
    }

    /**
     * Returns the name of the current state
     * @return the name of the current state
     */
    public final State.StateName getCurrentStateName() {
        return currentState.getName();
    }

    @Override
    public String toString() {
        return name;
    }
}
