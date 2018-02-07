package sapphire.policy.scalability;

/**
 * @author terryz
 */
public class ServerState {
    /**
     *                 |
     *                 | Starts up
     *                 V
     *              --------
     *             |  Init  |
     *              --------
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
    public static enum State {
        INIT, CANDIDATE, SLAVE, MASTER
    }

    private State currentState = State.INIT;

    public State getCurrentState() {
        return currentState;
    }

    public void setCurrentState(State currentState) {
        this.currentState = currentState;
    }

    public void transition() {

    }
}
