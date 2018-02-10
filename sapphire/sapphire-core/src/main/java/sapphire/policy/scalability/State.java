package sapphire.policy.scalability;

/**
 * @author terryz
 */
public interface State {
    /**
     * Valid state names
     */
    enum StateName {
        CANDIDATE, SLAVE, MASTER
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

    final class Candidate extends AbstractState {
        public Candidate() {
            super(StateName.CANDIDATE);
        }

        @Override
        public void enter() {

        }

        @Override
        public void leave() {

        }
    }

    final class Master extends AbstractState {
        public Master() {
            super(StateName.MASTER);
        }

        @Override
        public void enter() {

        }

        @Override
        public void leave() {

        }

    }

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

