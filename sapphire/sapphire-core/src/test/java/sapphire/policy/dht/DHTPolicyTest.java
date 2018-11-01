package sapphire.policy.dht;

import static org.mockito.Mockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

import java.net.InetSocketAddress;
import java.util.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import sapphire.app.SapphireObjectSpec;
import sapphire.kernel.common.KernelOID;
import sapphire.kernel.common.ServerInfo;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServer;
import sapphire.oms.OMSServerImpl;
import sapphire.policy.SapphirePolicy;
import sapphire.policy.SapphirePolicyContainer;

public class DHTPolicyTest {
    private OMSServer oms;
    private ServerPolicy[] servers = new ServerPolicy[3];
    private InetSocketAddress address1 = new InetSocketAddress("127.0.0.1", 30001);
    private InetSocketAddress address2 = new InetSocketAddress("127.0.0.1", 30002);
    private InetSocketAddress address3 = new InetSocketAddress("127.0.0.1", 30003);
    private Object[] requests = new Object[] {"r1", "r2", "r3", "r4", "r5"};

    @Before
    public void setup() throws Exception {
        for (int i = 0; i < 3; i++) {
            servers[i] = new ServerPolicy();
        }

        ArrayList<String> regions =
                new ArrayList<>(Arrays.asList("region-1", "region-2", "region-3"));
        oms = spy(OMSServerImpl.class);
        KernelServerImpl.oms = oms;
        oms.registerKernelServer(new ServerInfo(address1, "region-1"));
        oms.registerKernelServer(new ServerInfo(address2, "region-2"));
        oms.registerKernelServer(new ServerInfo(address2, "region-3"));

        when(oms.getRegions()).thenReturn(regions);
        when(oms.getServerInRegion("region-1")).thenReturn(address1);
        when(oms.getServerInRegion("region-2")).thenReturn(address2);
        when(oms.getServerInRegion("region-3")).thenReturn(address3);
    }

    @Test
    public void test() throws Exception {
        DHTPolicy.DHTGroupPolicy group = new DHTPolicy.DHTGroupPolicy();
        servers[0].onCreate(group, null);
        servers[1].onCreate(group, null);
        servers[2].onCreate(group, null);
        group.onCreate("region-1", servers[0], null);
        group.addServer(servers[1]);
        group.addServer(servers[2]);

        DHTPolicy.DHTClientPolicy client = new DHTPolicy.DHTClientPolicy();
        client.onCreate(group, null);
        for (Object r : requests) {
            ArrayList<Object> params = new ArrayList<>(Arrays.asList(new Object[] {r}));
            client.onRPC("method", params);
        }

        // Verify that all requests have been processed
        int cnt = 0;
        for (ServerPolicy s : servers) {
            cnt += s.requests.size();
        }
        Assert.assertEquals(requests.length, cnt);
    }

    private static class ServerPolicy extends DHTPolicy.DHTServerPolicy {
        List<Object> requests = new ArrayList<>();

        @Override
        public KernelOID $__getKernelOID() {
            int oid = new Random().nextInt();
            return new KernelOID(oid);
        }

        // TODO (merge):
        // @Override
        // public void onCreate(SapphirePolicy.SapphireGroupPolicy group, Annotation[] annotations)
        // {
        public void onCreate(SapphirePolicy.SapphireGroupPolicy group, SapphireObjectSpec spec) {
            super.onCreate(group, spec);
        }

        @Override
        public Object onRPC(String method, ArrayList<Object> params) {
            requests.add(params.get(0));
            return null;
        }

        @Override
        public SapphirePolicy.SapphireServerPolicy sapphire_replicate(
                List<SapphirePolicyContainer> processedPolicies,
                String region,
                SapphireObjectSpec spec) {
            return new ServerPolicy();
        }

        @Override
        public void sapphire_pin(SapphirePolicy.SapphireServerPolicy serverPolicy, String region) {}

        @Override
        public void sapphire_pin_to_server(
                SapphirePolicy.SapphireServerPolicy sapphireServerPolicy,
                InetSocketAddress server) {}
    }
}
