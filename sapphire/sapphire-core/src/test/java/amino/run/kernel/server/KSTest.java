package amino.run.kernel.server;

import static amino.run.common.UtilsTest.extractFieldValueOnInstance;
import static junit.framework.TestCase.assertEquals;

import amino.run.app.Language;
import amino.run.app.MicroServiceSpec;
import amino.run.common.BaseTest;
import amino.run.kernel.common.*;
import amino.run.oms.KernelServerManager;
import amino.run.oms.OMSServer;
import amino.run.policy.util.ResettableTimer;
import amino.run.sampleSO.SO;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.*;
import org.junit.*;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

/** Created by Vishwajeet on 11/9/18. */
@RunWith(PowerMockRunner.class)
public class KSTest extends BaseTest {
    SO so;
    KernelServerImpl ks;
    KernelObjectManager kom;
    ResettableTimer value;
    KernelServerManager kernelServerManager;
    ResettableTimer heartbeatTimer;
    @Rule public ExpectedException thrown = ExpectedException.none();
    // Added to allow SystemExit in order to prevent termination of code
    @Rule public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Before
    public void setUp() throws Exception {
        MicroServiceSpec spec =
                MicroServiceSpec.newBuilder()
                        .setLang(Language.java)
                        .setJavaClassName("amino.run.sampleSO.SO")
                        .create();
        super.setUp(1, spec);
        so = ((SO) (server1.sapphire_getAppObject().getObject()));

        ks = (KernelServerImpl) spiedKs1;
        kernelServerManager =
                (KernelServerManager)
                        extractFieldValueOnInstance(KernelServerImpl.oms, "serverManager");
        kom = (KernelObjectManager) extractFieldValueOnInstance(ks, "objectManager");
        heartbeatTimer = (ResettableTimer) extractFieldValueOnInstance(ks, "ksHeartbeatSendTimer");
        heartbeatTimer.reset();
    }

    @Test
    public void testMakeKernelRPC() throws Exception {
        String method = "public java.lang.Integer amino.run.sampleSO.SO.getI()";
        ArrayList<Object> params = new ArrayList<Object>();
        /* Make kernel rpc with a method name not present in server policy so that make rpc fails at invocation time(but
        succeeds at object lookup).
        Note: We have passed app method name instead of server's onRPC method to fail the rpc invocation. */
        KernelRPC rpc = new KernelRPC(server1.$__getKernelOID(), method, params);
        thrown.expect(KernelRPCException.class);
        spiedKs1.makeKernelRPC(rpc);
    }

    @Test
    public void getKernelObjectTest() throws Exception {
        // Get the existing kernel object
        ks.getKernelObject(client.getServer().$__getKernelOID());
    }

    @Test(expected = KernelObjectNotFoundException.class)
    public void getNonExistentKernelObjectTest() throws Exception {
        // Get the non-existent kernel object. It must throw kernel object not found exception.
        ks.getKernelObject(new KernelOID(0));
    }

    @Test
    public void testMain() throws Exception {
        // for testing negative scenario with only 3 arguments
        KernelServerImpl.main(
                new String[] {LOOP_BACK_IP_ADDR, Integer.toString(kernelPort1), LOOP_BACK_IP_ADDR});
    }

    /**
     * Testing kernelobject unhealthy and healthy states
     *
     * @throws Exception
     */
    @Test
    public void testHealth() throws Exception {
        List<KernelOID> kIdsBeforeFailure = Arrays.asList(kom.getAllKernelObjectOids());
        // Testing unhealthy state
        so.setStatus(false);
        Thread.sleep(
                KernelServerImpl.KS_HEARTBEAT_PERIOD // heartbeattimer in KernelServer
                        + OMSServer.KS_HEARTBEAT_TIMEOUT // healthCheckTimer in serverPolicy
                        + OMSServer.KS_HEARTBEAT_TIMEOUT); // healthCheckTimer in groupPolicy
        List<KernelOID> kIdsAfterFailure = Arrays.asList(kom.getAllKernelObjectOids());
        Assert.assertNotEquals(kIdsBeforeFailure, kIdsAfterFailure);
        // Testing healthy state
        so.setStatus(true);
        Thread.sleep(
                KernelServerImpl.KS_HEARTBEAT_PERIOD // heartbeattimer in KernelServer
                        + OMSServer.KS_HEARTBEAT_TIMEOUT // healthCheckTimer in serverPolicy
                        + OMSServer.KS_HEARTBEAT_TIMEOUT); // healthCheckTimer in groupPolicy
        List<KernelOID> kIdsAfterSuccess = Arrays.asList(kom.getAllKernelObjectOids());
        Assert.assertEquals(kIdsAfterFailure, kIdsAfterSuccess);
        for (KernelOID id : kIdsAfterSuccess) {
            Assert.assertEquals(true, kom.lookupObject(id).isStatus());
        }
    }

    /**
     * Testing kernelserver failure
     *
     * @throws Exception
     */
    // Limitation: This testcase doesn't test the scenario in which one kernelserver and one or more
    // replicas
    // are present and making kernelserver down will make the replicas removed from group policy and
    // add replica cannot be done
    @Test
    public void testKernelServerFail() throws Exception {
        // stopping kernel rpc
        Method method = KernelServerImpl.class.getDeclaredMethod("checkKernelServerStatus", null);
        method.setAccessible(true);
        method.invoke(ks);
        method.invoke(ks);
        // starting the timer in kernelservermanager so that the kernelserver which is not sending
        // its heartbeat will be removed from the list
        Map<InetSocketAddress, ResettableTimer> heartbeatTimers =
                (Map<InetSocketAddress, ResettableTimer>)
                        extractFieldValueOnInstance(kernelServerManager, "ksHeartBeatTimers");
        ResettableTimer ksHeartBeatTimer = heartbeatTimers.get(ks.getLocalHost());
        ksHeartBeatTimer.reset();
        Thread.sleep(
                OMSServer.KS_HEARTBEAT_TIMEOUT // waiting for timeout and stopheartbeat to happen
                        + KernelServerImpl.KS_HEARTBEAT_PERIOD // heartbeat timer in kernelserver
                        + (OMSServer.KS_HEARTBEAT_TIMEOUT
                                * 2)); // healthchecktimer in serverpolicy and group policy
    }

    @Test
    public void getAllKernelObjectOidsTest() throws Exception {
        // As KernelObjectManager.addObject is called once during
        // create, length returned should be 1.
        assertEquals(new Integer(1), new Integer(kom.getAllKernelObjectOids().length));
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }
}
