package sapphire.policy.dht;

import static org.mockito.Mockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServer;
import sapphire.oms.OMSServerImpl;
import sapphire.policy.SapphirePolicy;
import sapphire.policy.SapphirePolicyContainer;

public class DHTPolicyTest {
    private OMSServer oms;
    private ServerPolicy[] servers = new ServerPolicy[3];
    private Object[] requests = new Object[] {"r1", "r2", "r3", "r4", "r5"};

    @Before
    public void setup() throws Exception {
        for (int i = 0; i < 3; i++) {
            servers[i] = new ServerPolicy();
        }

        ArrayList<String> regions = new ArrayList<>(Arrays.asList("region-1", "region-2"));
        oms = spy(OMSServerImpl.class);
        KernelServerImpl.oms = oms;

        when(oms.getRegions()).thenReturn(regions);
    }

    @Test
    public void test() throws Exception {
        DHTPolicy.DHTGroupPolicy group = new DHTPolicy.DHTGroupPolicy();
        group.onCreate(servers[0], null);
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
        private SapphirePolicy.SapphireGroupPolicy group;
        List<Object> requests = new ArrayList<>();

        @Override
        public void onCreate(SapphirePolicy.SapphireGroupPolicy group, Annotation[] annotations) {
            this.group = group;
        }

        @Override
        public Object onRPC(String method, ArrayList<Object> params) {
            requests.add(params.get(0));
            return null;
        }

        @Override
        public SapphirePolicy.SapphireServerPolicy sapphire_replicate(List<SapphirePolicyContainer> processedPolicies) {
            return new ServerPolicy();
        }

        @Override
        public void sapphire_pin(String region) {}
    }
}
