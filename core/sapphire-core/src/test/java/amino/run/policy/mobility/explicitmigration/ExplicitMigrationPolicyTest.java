package amino.run.policy.mobility.explicitmigration;

import static org.junit.Assert.*;

import amino.run.app.DMSpec;
import amino.run.app.Language;
import amino.run.app.MicroServiceSpec;
import amino.run.common.BaseTest;
import amino.run.kernel.common.KernelObjectStub;
import amino.run.sampleSO.SO;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

/** Created by Malepati Bala Siva Sai Akhil on 23/1/18. */
@RunWith(PowerMockRunner.class)
public class ExplicitMigrationPolicyTest extends BaseTest {
    String appIncIMethod = "public void amino.run.sampleSO.SO.incI()";
    String appGetIMethod = "public java.lang.Integer amino.run.sampleSO.SO.getI()";
    String migrateMethod =
            "public void amino.run.sampleSO.SO.migrateTo(java.net.InetSocketAddress)";

    @Before
    public void setup() throws Exception {
        MicroServiceSpec spec =
                MicroServiceSpec.newBuilder()
                        .setLang(Language.java)
                        .setJavaClassName("amino.run.sampleSO.SO")
                        .addDMSpec(
                                DMSpec.newBuilder()
                                        .setName(ExplicitMigrationPolicy.class.getName())
                                        .create())
                        .create();
        super.setUp(2, spec);
    }

    @Test
    public void regularRPC() throws Exception {
        client.onRPC(appIncIMethod, new ArrayList());
        assertEquals(new Integer(1), ((SO) server1.getAppObject().getObject()).getI());
    }

    @Test
    public void migrateService() throws Exception {
        /* Increment the I value and verify it */
        client.onRPC(appIncIMethod, new ArrayList());

        /* Verification of value of I directly on SO instance on server policy */
        assertEquals(new Integer(1), ((SO) server1.getAppObject().getObject()).getI());

        /* Verification of value of I via RPC invocation from client policy */
        assertEquals(new Integer(1), client.onRPC(appGetIMethod, new ArrayList()));

        /* Get the host on which SO instance(and server policy object) is created */
        InetSocketAddress curHost = ((KernelObjectStub) client.getServer()).$__getHostname();

        InetSocketAddress ks1Host = new InetSocketAddress(LOOP_BACK_IP_ADDR, kernelPort1);
        InetSocketAddress ks2Host = new InetSocketAddress(LOOP_BACK_IP_ADDR, kernelPort2);

        ArrayList migrateParam = new ArrayList();
        if (curHost.equals(ks1Host)) {
            /* Migrate server policy object to ks2Host */
            migrateParam.add(ks2Host);
        } else {
            /* Migrate server policy object to ks1Host */
            migrateParam.add(ks2Host);
        }

        /* Invoke migration */
        client.onRPC(migrateMethod, migrateParam);

        /* Increment the I value again after migration and verify it */
        client.onRPC(appIncIMethod, new ArrayList());
        assertEquals(new Integer(2), client.onRPC(appGetIMethod, new ArrayList()));
    }

    @Test(expected = MigrationException.class)
    public void migrateToNonExistentHost() throws Exception {
        /* Note: Kernel Server 1 and 2 are started during setup. So, kernel server 3 is nonexistent */
        InetSocketAddress nonExistentHost = new InetSocketAddress(LOOP_BACK_IP_ADDR, kernelPort3);
        ArrayList migrateParam = new ArrayList();
        migrateParam.add(nonExistentHost);

        /* Invoke migration */
        client.onRPC(migrateMethod, migrateParam);
    }
}
