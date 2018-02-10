package sapphire.policy.scalability;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author terryz
 */
public class LoadBalancedMasterSlavePolicyTest {

    @Test
    public void test() throws Exception {
        StateManager stateMgr1 = new StateManager("1");
        System.out.println(String.format("%s: %s", stateMgr1, System.identityHashCode(stateMgr1)));
        StateManager stateMgr2 = new StateManager("1");
        System.out.println(String.format("%s: %s", stateMgr2, System.identityHashCode(stateMgr2)));
    }
}