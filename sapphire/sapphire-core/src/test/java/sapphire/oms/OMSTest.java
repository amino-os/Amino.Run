package sapphire.oms;

import static org.junit.Assert.assertEquals;
import static sapphire.common.SapphireUtils.deleteSapphireObject;
import static sapphire.common.SapphireUtils.getOmsSapphireInstance;
import static sapphire.common.UtilsTest.extractFieldValueOnInstance;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import sapphire.app.Language;
import sapphire.app.SapphireObject;
import sapphire.app.SapphireObjectSpec;
import sapphire.common.AppObject;
import sapphire.common.BaseTest;
import sapphire.common.IgnoreAfter;
import sapphire.common.SapphireObjectID;
import sapphire.common.SapphireUtils;
import sapphire.kernel.common.GlobalKernelReferences;
import sapphire.kernel.common.KernelOID;
import sapphire.kernel.common.KernelObjectFactory;
import sapphire.kernel.common.KernelObjectStub;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.policy.DefaultSapphirePolicy;
import sapphire.policy.SapphirePolicy;
import sapphire.runtime.EventHandler;
import sapphire.runtime.Sapphire;
import sapphire.sampleSO.SO;
import sapphire.sampleSO.stubs.SO_Stub;

/** OMS API test cases */

/** Created by Venugopal Reddy K 00900280 on 16/4/18. */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
    KernelServerImpl.class,
    Sapphire.class,
    KernelObjectFactory.class,
    LocateRegistry.class,
    SapphireUtils.class
})
public class OMSTest extends BaseTest {
    @Rule public ExpectedException thrown = ExpectedException.none();
    // To make the current test name available inside test methods
    @Rule public TestName methodName = new TestName();

    // To access the methods in SapphireObjectManager through Reflection
    OMSServerImpl omsImpl;
    SapphireObjectManager fieldValue;

    public static class DefaultSO extends SO implements SapphireObject {}

    public static class Group_Stub extends DefaultSapphirePolicy.DefaultGroupPolicy
            implements KernelObjectStub {
        sapphire.kernel.common.KernelOID $__oid = null;
        java.net.InetSocketAddress $__hostname = null;
        AppObject appObject = null;
        SapphirePolicy.SapphireClientPolicy $__nextClientPolicy = null;

        public Group_Stub(sapphire.kernel.common.KernelOID oid) {
            this.$__oid = oid;
        }

        public sapphire.kernel.common.KernelOID $__getKernelOID() {
            return this.$__oid;
        }

        public java.net.InetSocketAddress $__getHostname() {
            return this.$__hostname;
        }

        public void $__updateHostname(java.net.InetSocketAddress hostname) {
            this.$__hostname = hostname;
        }

        public void $__setNextClientPolicy(SapphirePolicy.SapphireClientPolicy clientPolicy) {
            $__nextClientPolicy = clientPolicy;
        }

        public AppObject $__getAppObject() {
            return this.appObject;
        }
    }

    public static class Server_Stub extends DefaultSapphirePolicy.DefaultServerPolicy
            implements KernelObjectStub {
        KernelOID $__oid = null;
        InetSocketAddress $__hostname = null;
        AppObject appObject = null;
        SapphirePolicy.SapphireClientPolicy $__nextClientPolicy = null;

        public Server_Stub(KernelOID oid) {
            this.oid = oid;
            this.$__oid = oid;
        }

        public KernelOID $__getKernelOID() {
            return $__oid;
        }

        public InetSocketAddress $__getHostname() {
            return $__hostname;
        }

        public void $__updateHostname(InetSocketAddress hostname) {
            this.$__hostname = hostname;
        }

        public void $__setNextClientPolicy(SapphirePolicy.SapphireClientPolicy clientPolicy) {
            $__nextClientPolicy = clientPolicy;
        }

        public AppObject $__getAppObject() {
            return this.appObject;
        }
    }

    @Before
    public void setUp() throws Exception {
        SapphireObjectSpec spec =
                SapphireObjectSpec.newBuilder()
                        .setLang(Language.java)
                        .setJavaClassName("sapphire.sampleSO.SO")
                        .create();
        super.setUp(
                spec,
                new HashMap<String, Class>() {
                    {
                        put("DefaultSapphirePolicy", Group_Stub.class);
                    }
                },
                new HashMap<String, Class>() {
                    {
                        put("DefaultSapphirePolicy", Server_Stub.class);
                    }
                });
        SapphireObjectID sapphireObjId = sapphireObjServer.createSapphireObject(spec.toString());

        soStub = (SO_Stub) sapphireObjServer.acquireSapphireObjectStub(sapphireObjId);
        client =
                (DefaultSapphirePolicy.DefaultClientPolicy)
                        extractFieldValueOnInstance(soStub, "$__client");
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
                1, getOmsSapphireInstance(spiedOms, group.getSapphireObjId()).getReferenceCount());

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
                2, getOmsSapphireInstance(spiedOms, group.getSapphireObjId()).getReferenceCount());

        /* setI on the client side stub */
        appObjstub.setI(new Integer(100));

        /* Verify if value is set on the SO */
        assertEquals(new Integer(100), so.getI());

        /* Reference count must become 1(decrement by 1) upon detach */
        sapphireObjServer.detachFromSapphireObject("MySapphireObject");
        assertEquals(
                1, getOmsSapphireInstance(spiedOms, group.getSapphireObjId()).getReferenceCount());
    }

    @Test
    public void getServerTest() throws Exception {
        List<InetSocketAddress> servers = spiedOms.getServers(null);

        /* Reference count must become 3 (Since three kernel server are added )  */
        assertEquals(new Integer(3), new Integer(servers.size()));
    }

    @Test
    public void mainTest() throws Exception {
        // this gives NumberFormatException
        OMSServerImpl.main(new String[] {localIP, "port", "--servicePort=10010"});
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
            deleteSapphireObject(spiedOms, group.getSapphireObjId());
        }
    }
}
