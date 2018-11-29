package sapphire.policy.scalability;

import org.junit.Assert;
import org.junit.Test;
import sapphire.policy.scalability.masterslave.Lock;

/** @author terryz */
public class LockTest {
    private String clientId = "clientId";
    private long lockTimeoutInMillis = 50L;

    @Test
    public void testLockNotExpired() throws Exception {
        long startTime = System.currentTimeMillis();
        Lock lock = new Lock(clientId, lockTimeoutInMillis);
        // sleep for less than lockTimeout
        Thread.sleep(Math.max(0, lockTimeoutInMillis - 20));
        // Note: sleep is not accurate, so we check below against currentTimeMillis to avoid false failures.
        Assert.assertFalse("lock should not expire within timeout",
                System.currentTimeMillis() - startTime < lockTimeoutInMillis && lock.isExpired());
    }

    @Test
    public void testLockExpired() throws Exception {
        Lock lock = new Lock(clientId, lockTimeoutInMillis);
        // sleep for longer than lockTimeout to let lock expire
        Thread.sleep(lockTimeoutInMillis + 20);
        Assert.assertTrue("lock should have expired", lock.isExpired());
    }

    @Test
    public void testLockRenewSucceeded() throws Exception {
        long startTime = System.currentTimeMillis();
        Lock lock = new Lock(clientId, lockTimeoutInMillis);
        // sleep for less than lockTimeout
        Thread.sleep(Math.max(0, lockTimeoutInMillis - 20));
        // Note: sleep is not accurate, so we check below against currentTimeMillis to avoid false failures.
        Assert.assertTrue("renew should succeed within timeout",
                System.currentTimeMillis() - startTime < lockTimeoutInMillis && lock.renew(clientId));
    }

    @Test
    public void testLockRenewFailedWithInvalidClientId() throws Exception {
        Lock lock = new Lock(clientId, lockTimeoutInMillis);
        Assert.assertFalse("renew should fail because client id does not match", lock.renew("invalidclient"));
        Assert.assertEquals("client id of the lock should not change", clientId, lock.getClientId());
    }

    @Test
    public void testLockRenewFailedWithLockTimeout() throws Exception {
        Lock lock = new Lock(clientId, lockTimeoutInMillis);
        // sleep for longer than lockTimeout to let lock expire
        Thread.sleep(lockTimeoutInMillis + 20);
        Assert.assertFalse("renew should fail because lock has expired", lock.renew(clientId));
        Assert.assertEquals("lock client id should not change", clientId, lock.getClientId());
    }

    @Test
    public void testObtainLockSucceeded() throws Exception {
        String newClientId = "newClientId";
        Lock lock = new Lock(clientId, lockTimeoutInMillis);
        // sleep for longer than lockTimeout to let lock expire
        Thread.sleep(lockTimeoutInMillis + 20);
        Assert.assertTrue("obtain lock should succeed", lock.obtain(newClientId));
        Assert.assertEquals("lock client id should match", newClientId, lock.getClientId());
    }

    @Test
    public void testObtainLockSucceededWithSameClientId() throws Exception {
        Lock lock = new Lock(clientId, lockTimeoutInMillis);
        // sleep for longer than lockTimeout to let lock expire
        Thread.sleep(lockTimeoutInMillis + 20);
        Assert.assertTrue("obtain lock should succeed", lock.obtain(clientId));
        Assert.assertEquals("lock client id should match", clientId, lock.getClientId());
    }

    @Test
    public void testObtainLockFailedWithUnexpiredLock() throws Exception {
        String newClientId = "newClientId";
        long startTime = System.currentTimeMillis();
        // sleep for less than lockTimeout
        Lock lock = new Lock(clientId, Math.max(0, lockTimeoutInMillis - 20));
        Assert.assertFalse(
                "obtain lock should fail if lock is not expired", System.currentTimeMillis() - startTime < lockTimeoutInMillis && lock.obtain(newClientId));
        if (System.currentTimeMillis() - startTime < lockTimeoutInMillis) {
            Assert.assertEquals("lock client id should not changed", clientId, lock.getClientId());
        }
    }
}
