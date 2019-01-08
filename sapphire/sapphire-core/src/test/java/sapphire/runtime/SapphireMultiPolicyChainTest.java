package sapphire.runtime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;
import sapphire.app.DMSpec;
import sapphire.app.Language;
import sapphire.app.SapphireObjectSpec;
import sapphire.common.*;
import sapphire.policy.*;
import sapphire.policy.dht.DHTPolicy;
import sapphire.sampleSO.SO;

@RunWith(PowerMockRunner.class)
public class SapphireMultiPolicyChainTest extends BaseTest {

    @Rule public ExpectedException thrown = ExpectedException.none();

    private SapphireObjectSpec spec;

    @Before
    public void setUp() throws Exception {
        this.serversInSameRegion = false;
        spec =
                SapphireObjectSpec.newBuilder()
                        .setLang(Language.java)
                        .setJavaClassName("sapphire.sampleSO.SO")
                        .addDMSpec(DMSpec.newBuilder().setName(DHTPolicy.class.getName()).create())
                        .addDMSpec(
                                DMSpec.newBuilder()
                                        .setName(DefaultSapphirePolicy.class.getName())
                                        .create())
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
        List<SapphirePolicyContainer> policyNameChain = new ArrayList<SapphirePolicyContainer>();
        List<SapphirePolicyContainer> processedPolicies = new ArrayList<SapphirePolicyContainer>();

        /* Register for a sapphire object Id from OMS */
        SapphireObjectID sapphireObjId = spiedOms.registerSapphireObject();

        policyNameChain.add(new SapphirePolicyContainer("sapphire.policy.dht.DHTPolicy", null));
        Sapphire.createPolicy(sapphireObjId, spec, policyNameChain, processedPolicies, "", null);
        assertEquals(1, processedPolicies.size());
    }

    @Test
    public void testCreatePolicyTwoPolicies() throws Exception {
        List<SapphirePolicyContainer> policyNameChain = new ArrayList<SapphirePolicyContainer>();
        List<SapphirePolicyContainer> processedPolicies = new ArrayList<SapphirePolicyContainer>();

        /* Register for a sapphire object Id from OMS */
        SapphireObjectID sapphireObjId = spiedOms.registerSapphireObject();

        policyNameChain.add(new SapphirePolicyContainer("sapphire.policy.dht.DHTPolicy", null));
        policyNameChain.add(
                new SapphirePolicyContainer("sapphire.policy.DefaultSapphirePolicy", null));

        Sapphire.createPolicy(sapphireObjId, spec, policyNameChain, processedPolicies, "", null);
        assertEquals(2, processedPolicies.size());
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Deleting SapphireObject: " + group.getSapphireObjId());
        super.tearDown();
    }
}
