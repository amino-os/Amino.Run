package sapphire.policy.scalability;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import sapphire.common.AppObject;
import sapphire.kernel.common.KernelOID;
import sapphire.policy.scalability.masterslave.MethodInvocationRequest;
import sapphire.policy.scalability.masterslave.MethodInvocationResponse;
import sapphire.runtime.annotations.Immutable;

/** @author terryz */
public class LoadBalancedMasterSlaveSyncPolicyIntegTest {
    private LoadBalancedMasterSlaveSyncPolicy.ClientPolicy client;
    private LoadBalancedMasterSlaveSyncPolicy.GroupPolicy group;
    private LoadBalancedMasterSlaveSyncPolicy.ServerPolicy server1;
    private LoadBalancedMasterSlaveSyncPolicy.ServerPolicy server2;
    private Object object1;
    private AppObject appObject1;
    private String data = "defaultData";
    private Object object2;
    private AppObject appObject2;

    @Before
    public void setup() throws Exception {
        object1 = new Data_Stub(data);
        appObject1 = new AppObject(object1);
        object2 = new Data_Stub(data);
        appObject2 = new AppObject(object2);

        server1 = new ServerMock();
        server1.$__setKernelOID(new KernelOID(1));
        server1.$__initialize(appObject1);
        server2 = new ServerMock();
        server2.$__setKernelOID(new KernelOID(2));
        server2.$__initialize(appObject2);

        client = new ClientMock();

        group = new GroupMock();
        group.addServer(server1);
        group.addServer(server2);

        client.setServer(server1);
        client.onCreate(group, new HashMap<>());
        server1.onCreate(group, new HashMap<>());
        server2.onCreate(group, new HashMap<>());
        server1.start();
        server2.start();
    }

    @Test
    public void testImmutableOperation() throws Exception {
        Method m = object1.getClass().getMethod("getData", new Class[] {});
        String methodName = m.toGenericString();
        ArrayList<Object> params = new ArrayList<Object>();

        Object ret = client.onRPC(methodName, params);
        Assert.assertEquals(data, ret);
    }

    @Test
    public void testMutableOperation() throws Exception {
        final String newData = "newData";
        Class[] paramTypes = new Class[1];
        paramTypes[0] = String.class;
        Method m = object1.getClass().getMethod("setData", paramTypes);

        String methodName = m.toGenericString();
        ArrayList<Object> params = new ArrayList<Object>();
        params.add(newData);

        Object ret = client.onRPC(methodName, params);
        Assert.assertNull(ret);

        // verify app object1 in server1 has been updated
        Data d1 = (Data) server1.sapphire_getAppObject().getObject();
        Assert.assertEquals(newData, d1.getData());

        // verify app object1 in server2 has been udpated (via replication)
        Data d2 = (Data) server2.sapphire_getAppObject().getObject();
        Assert.assertEquals(newData, d2.getData());
    }

    @Test
    public void testRepeatedRequests() throws Exception {
        String CLIENT_ID = "client0";
        long requestId = 100;
        int cnt = 10;

        Method m = object1.getClass().getMethod("incrementCnt", new Class[0]);
        String methodName = m.toGenericString();
        ArrayList<Object> params = new ArrayList<Object>();

        for (int i = 0; i < cnt; i++) {
            invokeWithRetry(CLIENT_ID, requestId, methodName, params, client);
        }

        // verify app object1 in server1 has been updated
        Data d1 = (Data) server1.sapphire_getAppObject().getObject();
        Assert.assertTrue(1 == d1.getCnt());

        // verify app object1 in server2 has been udpated (via replication)
        Data d2 = (Data) server2.sapphire_getAppObject().getObject();
        Assert.assertTrue(1 == d2.getCnt());
    }

    private MethodInvocationResponse invokeWithRetry(
            String CLIENT_ID,
            long requestId,
            String methodName,
            ArrayList<Object> params,
            LoadBalancedMasterSlaveSyncPolicy.ClientPolicy client) {
        int retry = 0;
        long waitInMilliseconds = 50L;

        do {
            try {
                MethodInvocationRequest request =
                        new MethodInvocationRequest(
                                CLIENT_ID,
                                requestId,
                                methodName,
                                params,
                                MethodInvocationRequest.MethodType.MUTABLE);

                LoadBalancedMasterSlaveBase.GroupBase group =
                        (LoadBalancedMasterSlaveBase.GroupBase) client.getGroup();
                LoadBalancedMasterSlaveBase.ServerBase server =
                        (LoadBalancedMasterSlaveBase.ServerBase) group.getMaster();

                return server.onRPC(request);
            } catch (Exception e) {
                try {
                    Thread.sleep(waitInMilliseconds);
                } catch (InterruptedException ex) {
                    // ignore
                }
                waitInMilliseconds <<= 1;
            }
        } while (retry++ < 5);

        return null;
    }

    private static class ClientMock extends LoadBalancedMasterSlaveSyncPolicy.ClientPolicy {}

    private static class ServerMock extends LoadBalancedMasterSlaveSyncPolicy.ServerPolicy {}

    private static class GroupMock extends LoadBalancedMasterSlaveSyncPolicy.GroupPolicy {}

    public static class Data implements Serializable {
        private String data = "";
        private Integer cnt = 0;

        public Data(String data) {
            this.data = data;
        }

        public void setData(String data) {
            this.data = data;
        }

        @Immutable
        public String getData() {
            return this.data;
        }

        public void incrementCnt() {
            this.cnt++;
        }

        private Integer getCnt() {
            return this.cnt;
        }
    }

    public static class Data_Stub extends Data {
        public Data_Stub(String str) {
            super(str);
        }
    }
}
