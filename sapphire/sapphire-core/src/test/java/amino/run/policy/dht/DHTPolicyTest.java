package amino.run.policy.dht;

import static org.powermock.api.mockito.PowerMockito.when;

import amino.run.app.NodeSelectorSpec;
import amino.run.app.SapphireObjectSpec;
import amino.run.kernel.common.KernelOID;
import amino.run.kernel.common.ServerInfo;
import amino.run.kernel.server.KernelServerImpl;
import amino.run.oms.OMSServer;
import amino.run.oms.OMSServerImpl;
import amino.run.policy.SapphirePolicy;
import amino.run.policy.SapphirePolicyContainer;
import java.net.InetSocketAddress;
import java.util.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DHTPolicyTest {
    private OMSServer oms;
    private ServerPolicy[] servers = new ServerPolicy[3];
    private InetSocketAddress[] addresses =
            new InetSocketAddress[] {
                new InetSocketAddress("127.0.0.1", 30001),
                new InetSocketAddress("127.0.0.1", 30002),
                new InetSocketAddress("127.0.0.1", 30003)
            };
    private Object[] requests = new Object[] {"r1", "r2", "r3", "r4", "r5"};

    @Before
    public void setup() throws Exception {
        for (int i = 0; i < 3; i++) {
            servers[i] = new ServerPolicy();
        }

        ArrayList<String> regions =
                new ArrayList<>(Arrays.asList("region-1", "region-2", "region-3"));
        oms = Mockito.spy(OMSServerImpl.class);
        KernelServerImpl.oms = oms;
        for (int i = 0; i < regions.size(); i++) {
            String Labelstr =
                    KernelServerImpl.LABEL_OPT + KernelServerImpl.REGION_KEY + "=" + regions.get(i);
            ServerInfo srvinfo = KernelServerImpl.createServerInfo(addresses[i], Labelstr);
            oms.registerKernelServer(srvinfo);
            NodeSelectorSpec nodeSelector = new NodeSelectorSpec();
            nodeSelector.addAndLabel(regions.get(i));
            List<InetSocketAddress> addressList = new ArrayList<>(Arrays.asList(addresses[i]));
            when(oms.getServers(nodeSelector)).thenReturn(addressList);
        }
    }

    @Test
    public void test() throws Exception {
        DHTPolicy.DHTGroupPolicy group = new DHTPolicy.DHTGroupPolicy();
        servers[0].onCreate(group, null);
        servers[1].onCreate(group, null);
        servers[2].onCreate(group, null);

        SapphireObjectSpec spec = new SapphireObjectSpec();
        group.onCreate("region-1", servers[0], spec);
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

        @Override
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
                List<SapphirePolicyContainer> processedPolicies, String region) {
            return new ServerPolicy();
        }

        @Override
        public void sapphire_pin_to_server(
                SapphirePolicy.SapphireServerPolicy sapphireServerPolicy,
                InetSocketAddress server) {}
    }
}
