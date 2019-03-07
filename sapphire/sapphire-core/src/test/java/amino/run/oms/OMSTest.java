package amino.run.oms;

import static org.junit.Assert.assertEquals;

import amino.run.app.Language;
import amino.run.app.MicroServiceSpec;
import amino.run.common.BaseTest;
import amino.run.common.MicroServiceID;
import amino.run.common.ReplicaID;
import amino.run.common.TestUtils;
import amino.run.kernel.common.KernelOID;
import amino.run.kernel.server.KernelServerImpl;
import amino.run.runtime.EventHandler;
import amino.run.sampleSO.SO;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

/** OMS API test cases */

/** Created by Venugopal Reddy K 00900280 on 16/4/18. */
@RunWith(PowerMockRunner.class)
public class OMSTest extends BaseTest {
    SO so;
    @Rule public ExpectedException thrown;

    // To access the methods in MicroServiceManager through Reflection
    OMSServerImpl omsImpl;
    MicroServiceManager fieldValue;

    public OMSTest() {
        thrown = ExpectedException.none();
    }

    @Before
    public void setUp() throws Exception {
        MicroServiceSpec spec =
                MicroServiceSpec.newBuilder()
                        .setLang(Language.java)
                        .setJavaClassName("amino.run.sampleSO.SO")
                        .create();
        super.setUp(1, spec);
        so = ((SO) (server1.getAppObject().getObject()));
        omsImpl = (OMSServerImpl) spiedOms;

        // In order to access the methods in MicroServiceManager, reflection is used to get the
        // MicroServiceManager instance from OMSServerImpl
        Field field = OMSServerImpl.class.getDeclaredField("objectManager");

        // To access the private field
        field.setAccessible(true);
        fieldValue = (MicroServiceManager) field.get(omsImpl);
    }

    @Test
    public void acquireSapphireObjectStubSuccessTest() throws Exception {
        MicroServiceID sid = group.getSapphireObjId();
        SO appObjstub = (SO) registry.acquireStub(sid);
        assertEquals(
                1,
                TestUtils.getOmsSapphireInstance(spiedOms, group.getSapphireObjId())
                        .getReferenceCount());

        /* setI on the client side stub */
        appObjstub.setI(new Integer(10));

        /* Verify if value is set on the SO */
        assertEquals(new Integer(10), so.getI());
    }

    @Test
    public void attachAndDetactSapphireObjectSuccessTest() throws Exception {
        registry.setName(group.getSapphireObjId(), "MySapphireObject");

        SO appObjstub = (SO) registry.attachTo("MySapphireObject");

        /* Reference count must become 2. Once user created it and other attached to it */
        assertEquals(
                2,
                TestUtils.getOmsSapphireInstance(spiedOms, group.getSapphireObjId())
                        .getReferenceCount());

        /* setI on the client side stub */
        appObjstub.setI(new Integer(100));

        /* Verify if value is set on the SO */
        assertEquals(new Integer(100), so.getI());

        /* Reference count must become 1(decrement by 1) upon detach */
        registry.detachFrom("MySapphireObject");
        assertEquals(
                1,
                TestUtils.getOmsSapphireInstance(spiedOms, group.getSapphireObjId())
                        .getReferenceCount());
    }

    @Test
    public void getServerTest() throws Exception {
        List<InetSocketAddress> servers = spiedOms.getServers(null);

        /* Reference count must become 1 (Since 1 kernel server is added )  */
        assertEquals(new Integer(kernelServerCount), new Integer(servers.size()));
    }

    @Test
    public void mainTest() throws Exception {
        // this gives NumberFormatException
        OMSServerImpl.main(new String[] {LOOP_BACK_IP_ADDR, "port", "--servicePort=10010"});
    }

    @Test
    public void getAllKernalObjectTest() throws Exception {
        ArrayList<KernelOID> arr = omsImpl.getAllKernelObjects();
        /* MicroService Object Created with the default DM  and number Kernel object should be two */
        assertEquals(new Integer(2), new Integer(arr.size()));
    }

    @Test
    public void sapphireInstanceAndReplicaDispatcherTest() throws Exception {
        /* Setup had created a sapphire object with default SO. Hence SO count is 1 */
        assertEquals(new Integer(1), new Integer(omsImpl.getAllMicroServices().size()));

        /* register a new sapphire object, set the handler, get the handler back, verify if it is same as what we have
        set, add a replica to it, set replica handler, get the handler and verify if it same as what is set and
        then unregister the replica and the sapphire object */
        MicroServiceID microServiceId = omsImpl.registerMicroService();

        /* Count becomes 2 after registering new sapphire object */
        assertEquals(new Integer(2), new Integer(omsImpl.getAllMicroServices().size()));

        EventHandler groupHandler =
                new EventHandler(
                        ((KernelServerImpl) spiedksOnOms).getLocalHost(),
                        new ArrayList() {
                            {
                                add(group);
                            }
                        });
        fieldValue.setInstanceDispatcher(microServiceId, groupHandler);
        assertEquals(groupHandler, fieldValue.getInstanceDispatcher(microServiceId));

        /* Register a replica to this SO, check if it is added, set handler, get it back and verify if it is same */
        ReplicaID replicaId = omsImpl.registerReplica(microServiceId);
        assertEquals(new Integer(1), new Integer(omsImpl.getReplicasById(microServiceId).length));

        EventHandler replicaHandler =
                new EventHandler(
                        ((KernelServerImpl) spiedKs1).getLocalHost(),
                        new ArrayList() {
                            {
                                add(server1);
                            }
                        });
        fieldValue.setReplicaDispatcher(replicaId, replicaHandler);
        assertEquals(replicaHandler, fieldValue.getReplicaDispatcher(replicaId));

        /* unregister the replica and check it is removed from sapphire object */
        omsImpl.unRegisterReplica(replicaId);
        assertEquals(new Integer(0), new Integer(omsImpl.getReplicasById(microServiceId).length));

        /* unregister the sapphire object */
        omsImpl.unRegisterMicroService(microServiceId);

        /* Count becomes 1 after unregistering the sapphire object */
        assertEquals(new Integer(1), new Integer(omsImpl.getAllMicroServices().size()));
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }
}
