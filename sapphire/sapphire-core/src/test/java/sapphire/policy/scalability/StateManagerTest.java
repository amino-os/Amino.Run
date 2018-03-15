package sapphire.policy.scalability;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author terryz
 */
public class StateManagerTest {
    private String clientId;
    private Configuration config;
    private long Thread_Wait_Time;

    @Before
    public void setup() {
        long Master_Lease_Timeout_InMillis = 50;
        long Master_Lease_Renew_Interval_InMillis = 10;
        long Init_Delay_Limit_InMillis = 1;

        this.clientId = "client";
        this.Thread_Wait_Time = Master_Lease_Timeout_InMillis * 12;
        this.config = Configuration.newBuilder()
                .masterLeaseRenewIntervalInMillis(Master_Lease_Renew_Interval_InMillis)
                .masterLeaseTimeoutInMillis(Master_Lease_Timeout_InMillis)
                .initDelayLimitInMillis(Init_Delay_Limit_InMillis).build();
    }

    @Test
    public void verifyToString() throws Exception {
        LoadBalancedMasterSlavePolicy.GroupPolicy group = new LoadBalancedMasterSlavePolicy.GroupPolicy() {
            @Override
            public boolean obtainLock(String serverId, long timeoutInMillis) {
                return false;
            }
            @Override
            public boolean renewLock(String serverId) {
                return false;
            }
        };

        Context context = Context.newBuilder()
                .config(config)
                .entryLogger(null)
                .group(group)
                .build();

        StateManager stateMgr = new StateManager(clientId, context);
        Assert.assertTrue("String value of State Manager should starts with StateManager_client", stateMgr.toString().startsWith("StateManager_client_"));
    }

    @Test
    public void verifyInitialState() throws Exception {
        LoadBalancedMasterSlavePolicy.GroupPolicy group = new LoadBalancedMasterSlavePolicy.GroupPolicy() {
            @Override
            public boolean obtainLock(String serverId, long timeoutInMillis) {
                return false;
            }
            @Override
            public boolean renewLock(String serverId) {
                return false;
            }
        };

        Context context = Context.newBuilder()
                .config(config)
                .entryLogger(null)
                .group(group)
                .build();

        StateManager stateMgr = new StateManager(clientId, context);
        Assert.assertEquals("Initial state should be " + State.StateName.SLAVE, State.StateName.SLAVE, stateMgr.getCurrentStateName());
    }

    @Test
    public void verifySlaveObtainLockFailed() throws Exception {
        LoadBalancedMasterSlavePolicy.GroupPolicy group = new LoadBalancedMasterSlavePolicy.GroupPolicy() {
            @Override
            public boolean obtainLock(String serverId, long timeoutInMillis) {
                return false;
            }
            @Override
            public boolean renewLock(String serverId) {
                return false;
            }
        };

        Context context = Context.newBuilder()
                .config(config)
                .entryLogger(null)
                .group(group)
                .build();

        final StateManager stateMgr = new StateManager(clientId, context);

        // Let state machine run for one second
        Thread.sleep(Thread_Wait_Time);

        // Verify that the end state is still slave because we failed to obtain the lock
        Assert.assertEquals(State.Slave.getInstance(context).getName(), stateMgr.getCurrentStateName());
    }

    @Test
    public void verifyObtainLockSucceededRenewLockSucceeded() throws Exception {
        LoadBalancedMasterSlavePolicy.GroupPolicy group = new LoadBalancedMasterSlavePolicy.GroupPolicy() {
            @Override
            public boolean obtainLock(String serverId, long timeoutInMillis) {
                return true;
            }
            @Override
            public boolean renewLock(String serverId) {
                return true;
            }
        };

        Context context = Context.newBuilder()
                .config(config)
                .entryLogger(null)
                .group(group)
                .build();

        final StateManager stateMgr = new StateManager(clientId, context);

        // Let state machine run for one second
        Thread.sleep(Thread_Wait_Time);

        // Verify that the end state is master because obtain lock and renew lock succeeded
        Assert.assertEquals(State.Master.getInstance(context).getName(), stateMgr.getCurrentStateName());
    }

    @Test
    public void verifyObtainLockSucceededRenewLockFailed() throws Exception {
        LoadBalancedMasterSlavePolicy.GroupPolicy group = new LoadBalancedMasterSlavePolicy.GroupPolicy() {
            int obtainLockCnt = 0;
            @Override
            public boolean obtainLock(String serverId, long timeoutInMillis) {
                // return true for the first invocation, and false for the rest invocations
                return (obtainLockCnt++ == 0);
            }

            @Override
            public boolean renewLock(String serverId) {
                return false;
            }
        };

        Context context = Context.newBuilder()
                .config(config)
                .entryLogger(null)
                .group(group)
                .build();

        final StateManager stateMgr = new StateManager("client", context);

        // Let state machine run for one second
        Thread.sleep(Thread_Wait_Time);

        // Verify that the end state is slave because renew lock failed
        Assert.assertEquals(State.Slave.getInstance(context).getName(), stateMgr.getCurrentStateName());
    }
}