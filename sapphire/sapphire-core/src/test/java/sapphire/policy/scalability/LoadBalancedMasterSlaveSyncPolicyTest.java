package sapphire.policy.scalability;

import org.junit.Assert;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import sapphire.common.AppObject;
import sapphire.kernel.common.KernelOID;
import sapphire.policy.scalability.masterslave.Lock;

/**
 * @author terryz
 */
public class LoadBalancedMasterSlaveSyncPolicyTest {
    private String clientId = "clientId";
    private LoadBalancedMasterSlaveSyncPolicy.GroupPolicy group = new LoadBalancedMasterSlaveSyncPolicy.GroupPolicy();

    @Test
    public void testRenewLockSucceeded() throws Exception {
        long lockTimeoutInMillis = 100L;
        Lock masterLock = new Lock(clientId, lockTimeoutInMillis);
        setMasterLock(group, masterLock);
        Assert.assertTrue("renew should succeed", group.renewLock(clientId));
    }

    @Test
    public void testRenewLockFailedWithExpiredLock() throws Exception {
        long lockTimeoutInMillis = 10L;
        Lock masterLock = new Lock(clientId, lockTimeoutInMillis);
        setMasterLock(group, masterLock);

        // sleep for 100 milliseconds (longer than logTimeoutInMillis) to let lock expire
        Thread.sleep(100L);
        Assert.assertFalse("renew should fail because lock has expired", group.renewLock(clientId));
    }

    @Test
    public void testObtainLockSucceeded() throws Exception {
        long lockTimeoutInMillis = 10L;
        Lock masterLock = new Lock(clientId, lockTimeoutInMillis);
        setMasterLock(group, masterLock);

        // sleep for 100 milliseconds to let lock expire
        Thread.sleep(100L);
        Assert.assertTrue("obtain lock should succeed", group.obtainLock(clientId, lockTimeoutInMillis));
    }

    @Test
    public void testObtainLockFailedWithUnexpiredLock() throws Exception {
        long lockTimeoutInMillis = 100L;
        Lock masterLock = new Lock(clientId, lockTimeoutInMillis);
        setMasterLock(group, masterLock);

        Assert.assertFalse("obtain lock should fail", group.obtainLock("AnotherClient", lockTimeoutInMillis));
    }

    @Test
    public void testGetMasterSlave() throws Exception {
        LoadBalancedMasterSlaveSyncPolicy.GroupPolicy group = new LoadBalancedMasterSlaveSyncPolicy.GroupPolicy();
        List<LoadBalancedMasterSlaveSyncPolicy.ServerPolicy> servers = createServers(group, 2);
        group.obtainLock(servers.get(0).getServerId(), 10000L);
        Assert.assertEquals(servers.get(0), group.getMaster());
        Assert.assertEquals(servers.get(1), group.getSlave());
    }

    private List<LoadBalancedMasterSlaveSyncPolicy.ServerPolicy> createServers(LoadBalancedMasterSlaveSyncPolicy.GroupPolicy group, int cnt) {
        List<LoadBalancedMasterSlaveSyncPolicy.ServerPolicy> servers = new ArrayList<LoadBalancedMasterSlaveSyncPolicy.ServerPolicy>();
        for (int i=0; i<cnt; i++) {
            LoadBalancedMasterSlaveSyncPolicy.ServerPolicy server = new LoadBalancedMasterSlaveSyncPolicy.ServerPolicy();
            AppObject appObj = new AppObject(new Date());
            server.$__setKernelOID(new KernelOID(i));
            server.$__initialize(appObj);
            server.onCreate(group, new Annotation[]{});
            group.addServer(server);
            servers.add(server);
        }
        return servers;
    }

    private void setMasterLock(LoadBalancedMasterSlaveSyncPolicy.GroupPolicy group, Lock masterLock) throws Exception {
        Class<?> clazz = group.getClass().getSuperclass();
        Field f = clazz.getDeclaredField("masterLock");
        f.setAccessible(true);
        f.set(group, masterLock);
    }
}