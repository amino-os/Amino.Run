package sapphire.policy.scalability;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author terryz
 */
public class LockTest {
    private String clientId = "clientId";
    private long logIndex = 0L;
    private long lockTimeoutInMillis = 100L;

    @Test
    public void testLockNotExpired() throws Exception {
        Lock lock = new Lock(clientId, logIndex, lockTimeoutInMillis);
        Thread.sleep(50L);
        Assert.assertFalse("lock should not be expired", lock.isExpired());
    }

    @Test
    public void testLockExpired() throws Exception {
        Lock lock = new Lock(clientId, logIndex, lockTimeoutInMillis);
        Thread.sleep(150L);
        Assert.assertTrue("lock should have expired", lock.isExpired());
    }

    @Test
    public void testLockRenewSucceeded() throws Exception {
        Lock lock = new Lock(clientId, logIndex, lockTimeoutInMillis);
        Assert.assertTrue("renew should succeed", lock.renew(clientId, logIndex));
    }

    @Test
    public void testLockRenewFailedWithInvalidClientId() throws Exception {
        Lock lock = new Lock(clientId, logIndex, lockTimeoutInMillis);
        Assert.assertFalse("renew should fail because client id not match", lock.renew("invalidclient", logIndex));
        Assert.assertEquals("lock client id should not change", clientId, lock.getClientId());
        Assert.assertEquals("lock log index should not change", logIndex, lock.getLogIndex());

    }

    @Test
    public void testLockRenewFailedWithInvalidLogIndex() throws Exception {
        Lock lock = new Lock(clientId, logIndex, lockTimeoutInMillis);
        Assert.assertFalse("renew should fail because client id not match", lock.renew(clientId, logIndex-1));
        Assert.assertEquals("lock client id should not change", clientId, lock.getClientId());
        Assert.assertEquals("lock log index should not change", logIndex, lock.getLogIndex());
    }

    @Test
    public void testLockRenewFailedWithLockTimeout() throws Exception {
        long lockTimeoutInMillis = 10;
        Lock lock = new Lock(clientId, logIndex, lockTimeoutInMillis);
        // sleep for 50 milliseconds to let lock expire
        Thread.sleep(50);
        Assert.assertFalse("renew should fail because lock has expired", lock.renew(clientId, logIndex));
        Assert.assertEquals("lock client id should not change", clientId, lock.getClientId());
        Assert.assertEquals("lock log index should not change", logIndex, lock.getLogIndex());
    }

    @Test
    public void testObtainLockSucceeded() throws Exception {
        String newClientId = "newClientId";
        long lockTimeoutInMillis = 10;
        Lock lock = new Lock(clientId, logIndex, lockTimeoutInMillis);
        // sleep for 50 milliseconds to let lock expire
        Thread.sleep(50);
        Assert.assertTrue("obtain lock should succeed", lock.obtain(newClientId, logIndex+1));
        Assert.assertEquals("lock client id should match", newClientId, lock.getClientId());
        Assert.assertEquals("lock log index should match", logIndex+1, lock.getLogIndex());
    }

    @Test
    public void testObtainLockFailedWithInvalidLogIndex() throws Exception {
        String newClientId = "newClientId";
        long lockTimeoutInMillis = 10;
        Lock lock = new Lock(clientId, logIndex, lockTimeoutInMillis);
        // sleep for 50 milliseconds to let lock expire
        Thread.sleep(50);
        Assert.assertFalse("obtain lock should fail because log entry is too old", lock.obtain(newClientId, logIndex-1));
        Assert.assertEquals("lock client id should not changed", clientId, lock.getClientId());
        Assert.assertEquals("lock log index should not changed", logIndex, lock.getLogIndex());
    }

    @Test
    public void testObtainLockFailedWithUnexpiredLock() throws Exception {
        String newClientId = "newClientId";
        long lockTimeoutInMillis = 50;
        Lock lock = new Lock(clientId, logIndex, lockTimeoutInMillis);
        // sleep for 50 milliseconds to let lock expire
        Thread.sleep(10);
        Assert.assertFalse("obtain lock should fail because lock is not expired", lock.obtain(newClientId, logIndex));
        Assert.assertEquals("lock client id should not changed", clientId, lock.getClientId());
        Assert.assertEquals("lock log index should not changed", logIndex, lock.getLogIndex());
    }

}