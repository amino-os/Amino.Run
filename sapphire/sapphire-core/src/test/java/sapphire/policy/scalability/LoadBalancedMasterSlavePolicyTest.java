package sapphire.policy.scalability;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;

/**
 * @author terryz
 */
public class LoadBalancedMasterSlavePolicyTest {
    private String clientId = "clientId";
    private long logIndex = 1000L;
    private LoadBalancedMasterSlavePolicy.GroupPolicy group = new LoadBalancedMasterSlavePolicy.GroupPolicy();

    @Test
    public void testRenewLockSucceeded() throws Exception {
        long lockTimeoutInMillis = 100L;
        Lock masterLock = new Lock(clientId, lockTimeoutInMillis);
        setMasterLock(group, masterLock);
        Assert.assertTrue("renew should succeed", group.renewLock(clientId));
    }

    @Test
    public void testRenewLockFailedWithExpriedLock() throws Exception {
        long lockTimeoutInMillis = 10L;
        Lock masterLock = new Lock(clientId, lockTimeoutInMillis);
        setMasterLock(group, masterLock);

        // sleep for 100 milliseconds (longer than logTimeoutInMillis) to let lock expire
        Thread.sleep(100L);
        boolean result = group.renewLock(clientId);
        Assert.assertFalse("renew should fail because lock has expired", group.renewLock(clientId));
    }

    @Test
    public void testObtainLockSucceeded() throws Exception {
        long lockTimeoutInMillis = 10L;
        Lock masterLock = new Lock(clientId, lockTimeoutInMillis);
        setMasterLock(group, masterLock);

        // sleep for 100 milliseconds to let lock expire
        Thread.sleep(100L);
        Assert.assertTrue("obtain lock should succeed", group.obtainLock(clientId));
    }

    @Test
    public void testObtainLockFailedWithUnexpiredLock() throws Exception {
        long lockTimeoutInMillis = 100L;
        Lock masterLock = new Lock(clientId, lockTimeoutInMillis);
        setMasterLock(group, masterLock);

        Assert.assertFalse("obtain lock should fail", group.obtainLock("AnotherClient"));
    }

    private void setMasterLock(LoadBalancedMasterSlavePolicy.GroupPolicy group, Lock masterLock) throws Exception {
        Class<?> clazz = group.getClass();
        Field[] fields = clazz.getDeclaredFields();
        Field f = group.getClass().getDeclaredField("masterLock");
        f.setAccessible(true);
        f.set(group, masterLock);
    }
}