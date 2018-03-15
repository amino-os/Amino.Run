package sapphire.policy.scalability;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import sapphire.common.AppObject;

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
        LoadBalancedMasterSlavePolicy.GroupPolicy group = new LoadBalancedMasterSlavePolicy.GroupPolicy();
        List<LoadBalancedMasterSlavePolicy.ServerPolicy> servers = createServers(group, 2);
        group.obtainLock(servers.get(0).getServerId(), 10000L);
        Assert.assertEquals(servers.get(0), group.getMaster());
        Assert.assertEquals(servers.get(1), group.getSlave());
    }

    private List<LoadBalancedMasterSlavePolicy.ServerPolicy> createServers(LoadBalancedMasterSlavePolicy.GroupPolicy group, int cnt) {
        Configuration config = Configuration.newBuilder().build();
        File logFile = new File(config.getLogFilePath());
        if (logFile.exists()) {
            logFile.delete();
        }

        File snapshotFile = new File(config.getSnapshotFilePath());
        if (snapshotFile.exists()) {
            snapshotFile.delete();
        }

        List<LoadBalancedMasterSlavePolicy.ServerPolicy> servers = new ArrayList<LoadBalancedMasterSlavePolicy.ServerPolicy>();
        for (int i=0; i<cnt; i++) {
            LoadBalancedMasterSlavePolicy.ServerPolicy server = new LoadBalancedMasterSlavePolicy.ServerPolicy();
            AppObject appObj = new AppObject(new Date());
            server.$__initialize(appObj);
            server.onCreate(group);
            group.addServer(server);
            servers.add(server);
        }
        return servers;
    }

    private void setMasterLock(LoadBalancedMasterSlavePolicy.GroupPolicy group, Lock masterLock) throws Exception {
        Class<?> clazz = group.getClass();
        Field[] fields = clazz.getDeclaredFields();
        Field f = group.getClass().getDeclaredField("masterLock");
        f.setAccessible(true);
        f.set(group, masterLock);
    }


}