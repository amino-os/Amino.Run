package amino.run.runtime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import amino.run.app.DMSpec;
import amino.run.app.Language;
import amino.run.app.MicroServiceSpec;
import amino.run.common.BaseTest;
import amino.run.common.MicroServiceID;
import amino.run.policy.DefaultPolicy;
import amino.run.policy.PolicyContainer;
import amino.run.policy.dht.DHTPolicy;
import amino.run.sampleSO.SO;
import java.util.ArrayList;
import java.util.List;
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
                        .addDMSpec(DMSpec.newBuilder().setName(DHTPolicy.class.getName()).create())
                        .addDMSpec(
                                DMSpec.newBuilder().setName(DefaultPolicy.class.getName()).create())
                        .create();
        super.setUp(2, spec);
    }

    @Test
    public void testNew_() throws Exception {
        Object temp = Sapphire.new_(SO.class);
        assertNotEquals(null, temp);
    }

    @Test
    public void testCreatePolicy() throws Exception {
        List<PolicyContainer> policyNameChain = new ArrayList<PolicyContainer>();
        List<PolicyContainer> processedPolicies = new ArrayList<PolicyContainer>();

        /* Register for a sapphire object Id from OMS */
        MicroServiceID microServiceId = spiedOms.registerSapphireObject();

        policyNameChain.add(new PolicyContainer("amino.run.policy.dht.DHTPolicy", null));
        Sapphire.createPolicy(microServiceId, spec, policyNameChain, processedPolicies, null);
        assertEquals(1, processedPolicies.size());
    }

    @Test
    public void testCreatePolicyTwoPolicies() throws Exception {
        List<PolicyContainer> policyNameChain = new ArrayList<PolicyContainer>();
        List<PolicyContainer> processedPolicies = new ArrayList<PolicyContainer>();

        /* Register for a sapphire object Id from OMS */
        MicroServiceID microServiceId = spiedOms.registerSapphireObject();

        policyNameChain.add(new PolicyContainer("amino.run.policy.dht.DHTPolicy", null));
        policyNameChain.add(new PolicyContainer("amino.run.policy.DefaultPolicy", null));

        Sapphire.createPolicy(microServiceId, spec, policyNameChain, processedPolicies, null);
        assertEquals(2, processedPolicies.size());
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Deleting SapphireObject: " + group.getSapphireObjId());
        super.tearDown();
    }
}
