package amino.run.oms;

import static org.junit.Assert.assertEquals;

import amino.run.app.Language;
import amino.run.app.SapphireObjectSpec;
import amino.run.common.BaseTest;
import amino.run.common.IgnoreAfter;
import amino.run.common.SapphireObjectID;
import amino.run.common.SapphireUtils;
import amino.run.kernel.common.GlobalKernelReferences;
import amino.run.kernel.common.KernelOID;
import amino.run.runtime.EventHandler;
import amino.run.sampleSO.SO;
import amino.run.sampleSO.stubs.SO_Stub;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

/** OMS API test cases */

/** Created by Venugopal Reddy K 00900280 on 16/4/18. */
@RunWith(PowerMockRunner.class)
public class OMSTest extends BaseTest {
    SO so;
    @Rule public ExpectedException thrown;

    // To make the current test name available inside test methods
    @Rule public TestName methodName;

    // To access the methods in SapphireObjectManager through Reflection
    OMSServerImpl omsImpl;
    SapphireObjectManager fieldValue;

    public OMSTest() {
        thrown = ExpectedException.none();
        methodName = new TestName();
    }

    @Before
    public void setUp() throws Exception {
        SapphireObjectSpec spec =
                SapphireObjectSpec.newBuilder()
                        .setLang(Language.java)
                        .setJavaClassName("amino.run.sampleSO.SO")
                        .create();
        super.setUp(1, spec);
        so = ((SO) (server1.sapphire_getAppObject().getObject()));
        omsImpl = (OMSServerImpl) spiedOms;

        // In order to access the methods in SapphireObjectManager, reflection is used to get the
        // SapphireObjectManager instance from OMSServerImpl
        Field field = OMSServerImpl.class.getDeclaredField("objectManager");

        // To access the private field
        field.setAccessible(true);
        fieldValue = (SapphireObjectManager) field.get(omsImpl);
    }

    @Test
    public void acquireSapphireObjectStubSuccessTest() throws Exception {
        SapphireObjectID sid = group.getSapphireObjId();
        SO_Stub appObjstub = (SO_Stub) sapphireObjServer.acquireSapphireObjectStub(sid);
        assertEquals(
                1,
                SapphireUtils.getOmsSapphireInstance(spiedOms, group.getSapphireObjId())
                        .getReferenceCount());

        /* setI on the client side stub */
        appObjstub.setI(new Integer(10));

        /* Verify if value is set on the SO */
        assertEquals(new Integer(10), so.getI());
    }

    @Test
    public void attachAndDetactSapphireObjectSuccessTest() throws Exception {
        sapphireObjServer.setSapphireObjectName(group.getSapphireObjId(), "MySapphireObject");

        SO_Stub appObjstub = (SO_Stub) sapphireObjServer.attachToSapphireObject("MySapphireObject");

        /* Reference count must become 2. Once user created it and other attached to it */
        assertEquals(
                2,
                SapphireUtils.getOmsSapphireInstance(spiedOms, group.getSapphireObjId())
                        .getReferenceCount());

        /* setI on the client side stub */
        appObjstub.setI(new Integer(100));

        /* Verify if value is set on the SO */
        assertEquals(new Integer(100), so.getI());

        /* Reference count must become 1(decrement by 1) upon detach */
        sapphireObjServer.detachFromSapphireObject("MySapphireObject");
        assertEquals(
                1,
                SapphireUtils.getOmsSapphireInstance(spiedOms, group.getSapphireObjId())
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
        /* Sapphire Object Created with the default DM  and number Kernel object should be two */
        assertEquals(new Integer(2), new Integer(arr.size()));
    }

    @Test
    @IgnoreAfter // Added to ignore @After for this particular testcase
    public void getAllAndUnRegisterSapphireObjectTest() throws Exception {
        // As only one sapphire object is created during setUp(), after unregister, number of
        // sapphireobjects should be zero
        omsImpl.unRegisterSapphireObject(group.getSapphireObjId());
        ArrayList<SapphireObjectID> afterUnregister = omsImpl.getAllSapphireObjects();
        assertEquals(new Integer(0), new Integer(afterUnregister.size()));
    }

    @Test
    public void getAndUnRegisterSapphireReplicaTest() throws Exception {
        // As only one sapphire replica is created during setUp(), after unregister, number of
        // sapphireReplicas should be zero
        omsImpl.unRegisterSapphireReplica(server1.getReplicaId());
        EventHandler[] arr = omsImpl.getSapphireReplicasById(group.getSapphireObjId());
        assertEquals(new Integer(0), new Integer(arr.length));
    }

    @Test
    public void setAndGetInstanceDispatcherTest() throws Exception {
        EventHandler eventHandler =
                new EventHandler(
                        GlobalKernelReferences.nodeServer.getLocalHost(),
                        new ArrayList() {
                            {
                                add(server1);
                            }
                        });
        fieldValue.setInstanceDispatcher(group.getSapphireObjId(), eventHandler);
        assertEquals(eventHandler, fieldValue.getInstanceDispatcher(group.getSapphireObjId()));
    }

    @Test
    public void setAndGetReplicaDispatcherTest() throws Exception {
        EventHandler eventHandler =
                new EventHandler(
                        GlobalKernelReferences.nodeServer.getLocalHost(),
                        new ArrayList() {
                            {
                                add(server1);
                            }
                        });
        fieldValue.setReplicaDispatcher(server1.getReplicaId(), eventHandler);
        assertEquals(eventHandler, fieldValue.getReplicaDispatcher(server1.getReplicaId()));
    }

    @After
    public void tearDown() throws Exception {
        GlobalKernelReferences.nodeServer.oms = spiedOms;
        Method method = getClass().getMethod(methodName.getMethodName());
        // because sapphire object will be deleted in getAllAndUnRegisterSapphireObjectTest.So no
        // need to delete it again
        if (method.isAnnotationPresent(IgnoreAfter.class)) {
            System.out.println("sapphire object already deleted");
        } else {
            super.tearDown();
        }
    }
}
