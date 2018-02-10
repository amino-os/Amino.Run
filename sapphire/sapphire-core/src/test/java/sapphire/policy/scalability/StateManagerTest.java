package sapphire.policy.scalability;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author terryz
 */
public class StateManagerTest {

    @Test
    public void verifyToString() throws Exception {
        StateManager stateMgr = new StateManager("test");
        Assert.assertTrue("String value of State Manager should starts with StateManager_test", stateMgr.toString().startsWith("StateManager_test_"));
    }

    @Test
    public void verifyInitialState() throws Exception {
        StateManager stateMgr = new StateManager("test");
        Assert.assertEquals("Initial state should be " + State.StateName.CANDIDATE, State.StateName.CANDIDATE, stateMgr.getCurrentStateName());
    }

    @Test
    public void verifyStateTransition() throws Exception {
        List<Object[]> specs = new ArrayList<Object[]>() {{
            add(new Object[]{new State.Candidate(), StateManager.Event.OBTAIN_LOCK_FAILED, new State.Slave()});
            add(new Object[]{new State.Candidate(), StateManager.Event.OBTAIN_LOCK_SUCCEEDED, new State.Master()});
            add(new Object[]{new State.Slave(), StateManager.Event.OBTAIN_LOCK_FAILED, new State.Slave()});
            add(new Object[]{new State.Slave(), StateManager.Event.OBTAIN_LOCK_SUCCEEDED, new State.Master()});
            add(new Object[]{new State.Master(), StateManager.Event.RENEW_LOCK_FAILED, new State.Slave()});
            add(new Object[]{new State.Master(), StateManager.Event.RENEW_LOCK_SUCCEEDED, new State.Master()});
        }};

        StateManager stateMgr = new StateManager("test");
        for (Object[] spec : specs) {
            State srcState = (State)spec[0];
            StateManager.Event event = (StateManager.Event)spec[1];
            State dstState = (State)spec[2];

            setCurrentState(stateMgr, srcState);
            stateMgr.stateTransition(event);
            Assert.assertEquals(dstState.getName(), stateMgr.getCurrentStateName());
        }

        specs = new ArrayList<Object[]>() {{
            add(new Object[]{new State.Candidate(), StateManager.Event.RENEW_LOCK_FAILED});
            add(new Object[]{new State.Candidate(), StateManager.Event.RENEW_LOCK_SUCCEEDED});
            add(new Object[]{new State.Slave(), StateManager.Event.RENEW_LOCK_FAILED});
            add(new Object[]{new State.Slave(), StateManager.Event.RENEW_LOCK_SUCCEEDED});
            add(new Object[]{new State.Master(), StateManager.Event.OBTAIN_LOCK_FAILED});
            add(new Object[]{new State.Master(), StateManager.Event.OBTAIN_LOCK_SUCCEEDED});
        }};

        for (Object[] spec : specs) {
            State srcState = (State)spec[0];
            StateManager.Event event = (StateManager.Event)spec[1];

            setCurrentState(stateMgr, srcState);
            try {
                stateMgr.stateTransition(event);
            } catch (Throwable e) {
                Assert.assertTrue("Should get assert error", e instanceof AssertionError);
            }
        }
    }

    private void setCurrentState(StateManager stateMgr, State state) throws Exception {
        Field f = stateMgr.getClass().getDeclaredField("currentState");
        f.setAccessible(true);
        f.set(stateMgr, state);
    }
}