package amino.run.runtime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import amino.run.app.DMSpec;
import amino.run.app.Language;
import amino.run.app.MicroServiceSpec;
import amino.run.common.*;
import amino.run.policy.DefaultPolicy;
import amino.run.policy.PolicyContainer;
import amino.run.policy.dht.DHTPolicy;
import amino.run.sampleSO.SO;
import java.util.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class SapphireMultiPolicyChainTest extends BaseTest {

    @Rule public ExpectedException thrown = ExpectedException.none();

    private MicroServiceSpec spec;

    @Before
    public void setUp() throws Exception {
        this.serversInSameRegion = false;
        spec =
                MicroServiceSpec.newBuilder()
                        .setLang(Language.java)
                        .setJavaClassName("amino.run.sampleSO.SO")
                        .addDMSpec(
                                DMSpec.newBuilder().setName(DefaultPolicy.class.getName()).create())
                        .addDMSpec(DMSpec.newBuilder().setName(DHTPolicy.class.getName()).create())
                        .create();
        super.setUp(2, spec);
    }

    @Test
    public void testNew_() {
        Object temp = Sapphire.new_(SO.class);
        assertNotEquals(null, temp);
    }

    @Test
    public void testCreatePolicyChain() throws Exception {
        String region = "IND";
        AppObjectStub aos = Sapphire.createPolicyChain(spec, region, null);
        assertNotNull(aos);
    }

    @Test
    public void testCreateConnectedPolicyTwoPolicies() throws Exception {
        List<String> policyNameChain = new ArrayList<>();
        List<SapphirePolicyContainer> processedPolicies = new ArrayList<>();

        /* Register for a sapphire object Id from OMS */
        SapphireObjectID soid = spiedOms.registerSapphireObject();

        policyNameChain.add("amino.run.policy.dht.DHTPolicy");
        policyNameChain.add(("amino.run.policy.DefaultPolicy"));
        for (int i = 0; i < 2; i++) {
            Sapphire.createConnectedPolicy(
                    i, null, policyNameChain, processedPolicies, soid, spec, null);
        }
        assertEquals(2, processedPolicies.size());
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Deleting SapphireObject: " + group.getSapphireObjId());
        super.tearDown();
    }
}
