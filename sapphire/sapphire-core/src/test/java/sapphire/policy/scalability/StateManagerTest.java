package sapphire.policy.scalability;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author terryz
 */
public class StateManagerTest {
    private final long Master_Lease_Timeout_InMillis = 50;
    private final long Master_Lease_Renew_Interval_InMillis = 10;
    private final long Init_Delay_Limit_InMillis = 1;

    private final long Thread_Wait_Time = Master_Lease_Timeout_InMillis * 10;

    private Configuration config;

    @Before
    public void setup() {
        this.config = Configuration.newBuilder()
                .masterLeaseRenewIntervalInMillis(Master_Lease_Renew_Interval_InMillis)
                .masterLeaseTimeoutInMIllis(Master_Lease_Timeout_InMillis)
                .initDelayLimitInMillis(Init_Delay_Limit_InMillis).build();
    }

    @Test
    public void verifyToString() throws Exception {
        StateManager stateMgr = new StateManager("client", null, config);
        Assert.assertTrue("String value of State Manager should starts with StateManager_client", stateMgr.toString().startsWith("StateManager_client_"));
    }

    @Test
    public void verifyInitialState() throws Exception {
        StateManager stateMgr = new StateManager("client", null, config);
        Assert.assertEquals("Initial state should be " + State.StateName.SLAVE, State.StateName.SLAVE, stateMgr.getCurrentStateName());
    }

    @Test
    public void verifySlaveObtainLockFailed() throws Exception {
        LoadBalancedMasterSlavePolicy.GroupPolicy group = spy(LoadBalancedMasterSlavePolicy.GroupPolicy.class);
        when(group.obtainLock(anyString(), anyString())).thenReturn(false); // stay as slave

        final StateManager stateMgr = new StateManager("client", group, config);

        // Let state machine run for one second
        Thread.sleep(Thread_Wait_Time);

        // Verify that the end state is still slave because we failed to get the lock
        Assert.assertEquals(new State.Slave(stateMgr).getName(), stateMgr.getCurrentStateName());
    }

    @Test
    public void verifyStateObtainLockSucceeded() throws Exception {
        LoadBalancedMasterSlavePolicy.GroupPolicy group = spy(LoadBalancedMasterSlavePolicy.GroupPolicy.class);
        when(group.obtainLock(anyString(), anyString())).thenReturn(true); // promoted to master
        when(group.renewLock(anyString())).thenReturn(true);               // stay as master

        final StateManager stateMgr = new StateManager("client", group, config);

        // Let state machine run for one second
        Thread.sleep(Thread_Wait_Time);

        // Verify that the end state is master because obtain lock and renew lock succeeded
        Assert.assertEquals(new State.Master(stateMgr).getName(), stateMgr.getCurrentStateName());
    }

    @Test
    public void verifyStateObtainLockSucceededRenewLockFailed() throws Exception {
        LoadBalancedMasterSlavePolicy.GroupPolicy group = spy(LoadBalancedMasterSlavePolicy.GroupPolicy.class);

        when(group.obtainLock(anyString(), anyString()))
                // first obtain lock succeeds which will promote the server to master
                .thenReturn(true)
                // future obtain lock fails which will let the server stay in slave
                .thenReturn(false);

        // renew lock fails
        when(group.renewLock(anyString())).thenReturn(false);              // demoted to slave

        final StateManager stateMgr = new StateManager("client", group, config);

        // Let state machine run for one second
        Thread.sleep(Thread_Wait_Time);

        // Verify that the end state is slave because renew lock succeeded
        Assert.assertEquals(new State.Slave(stateMgr).getName(), stateMgr.getCurrentStateName());
    }
}