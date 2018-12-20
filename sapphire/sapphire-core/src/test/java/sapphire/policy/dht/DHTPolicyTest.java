package sapphire.policy.dht;

import static org.mockito.Mockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import sapphire.app.*;
import sapphire.common.LabelUtils;
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
        oms = spy(OMSServerImpl.class);
        KernelServerImpl.oms = oms;
        for (int i = 0; i < regions.size(); i++) {
            String Labelstr = LabelUtils.LABEL_OPT + LabelUtils.REGION_KEY + "=" + regions.get(i);
            ServerInfo srvinfo = KernelServerImpl.createServerInfo(addresses[i], Labelstr);
            oms.registerKernelServer(srvinfo);
            NodeSelectorSpec nodeSelector = new NodeSelectorSpec();
            NodeSelectorTerm term = new NodeSelectorTerm();
            KsAffinity affinity = new KsAffinity();
            String[] vals = {regions.get(i)};
            List<String> valsList = Arrays.asList(vals);

            NodeSelectorRequirement requirement =
                    new NodeSelectorRequirement(LabelUtils.REGION_KEY, LabelUtils.Equals, valsList);
            List<NodeSelectorRequirement> requirementList = Arrays.asList(requirement);

            term.setMatchExpressions(requirementList);
            List<NodeSelectorTerm> terms = Arrays.asList(term);
            affinity.setRequireExpressions(terms);
            nodeSelector.setNodeAffinity(affinity);
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
