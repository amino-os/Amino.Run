package sapphire.policy.scalability;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author terryz
 */
public class StateManagerTest {
    private final String clientId = "client";
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
        LoadBalancedMasterSlavePolicy.GroupPolicy group = new LoadBalancedMasterSlavePolicy.GroupPolicy() {
            @Override
            public boolean obtainLock(String clientId, long logIndex) {
                return false;
            }
            @Override
            public boolean renewLock(String client, long logIndex) {
                return false;
            }
        };

        group.setConfig(config);
        final StateManager stateMgr = new StateManager(clientId, group, config);

        // Let state machine run for one second
        Thread.sleep(Thread_Wait_Time);

        // Verify that the end state is still slave because we failed to obtain the lock
        Assert.assertEquals(new State.Slave(stateMgr).getName(), stateMgr.getCurrentStateName());
    }

    @Test
    public void verifyObtainLockSucceededRenewLockSucceeded() throws Exception {
        LoadBalancedMasterSlavePolicy.GroupPolicy group = new LoadBalancedMasterSlavePolicy.GroupPolicy() {
            @Override
            public boolean obtainLock(String clientId, long logIndex) {
                return true;
            }
            @Override
            public boolean renewLock(String client, long logIndex) {
                return true;
            }
        };

        group.setConfig(config);
        final StateManager stateMgr = new StateManager(clientId, group, config);

        // Let state machine run for one second
        Thread.sleep(Thread_Wait_Time);

        // Verify that the end state is master because obtain lock and renew lock succeeded
        Assert.assertEquals(new State.Master(stateMgr).getName(), stateMgr.getCurrentStateName());
    }

    @Test
    public void verifyObtainLockSucceededRenewLockFailed() throws Exception {
        LoadBalancedMasterSlavePolicy.GroupPolicy group = new LoadBalancedMasterSlavePolicy.GroupPolicy() {
            int obtainLockCnt = 0;
            @Override
            public boolean obtainLock(String clientId, long logIndex) {
                // return true for the first invocation, and false for the rest invocatiions
                return (obtainLockCnt++ == 0);
            }

            @Override
            public boolean renewLock(String client, long logIndex) {
                return false;
            }
        };

        group.setConfig(config);
        final StateManager stateMgr = new StateManager("client", group, config);

        // Let state machine run for one second
        Thread.sleep(Thread_Wait_Time);

        // Verify that the end state is slave because renew lock failed
        Assert.assertEquals(new State.Slave(stateMgr).getName(), stateMgr.getCurrentStateName());
    }
}