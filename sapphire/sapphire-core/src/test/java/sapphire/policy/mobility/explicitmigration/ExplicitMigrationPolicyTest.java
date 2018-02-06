package sapphire.policy.mobility.explicitmigration;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.ArrayList;

import sapphire.common.AppObject;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServerImpl;
import sun.awt.X11.XModifierKeymap;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import sapphire.policy.mobility.explicitmigration.ExplicitMigrationPolicyTestConstants;

/**
 * Created by Malepati Bala Siva Sai Akhil on 23/1/18.
 */

@RunWith(MockitoJUnitRunner.class)
public class ExplicitMigrationPolicyTest {

    public static class ExplicitMigrationTest extends ExplicitMigrationImpl {}

    ExplicitMigrationPolicy.ClientPolicy client;
    ExplicitMigrationPolicy.ServerPolicy server;
    private ExplicitMigrationTest so;
    private AppObject appObject;
    private ArrayList<Object> noParams;

    @Rule
    public ExpectedException thrown = ExpectedException.none();


    @Before
    public void setUp() throws Exception {
        this.client = spy(ExplicitMigrationPolicy.ClientPolicy.class);
        this.server = spy(ExplicitMigrationPolicy.ServerPolicy.class);
        so = new ExplicitMigrationTestStub();
        appObject = new AppObject(so);
        server.$__initialize(appObject);
        this.client.setServer(this.server);
        noParams = new ArrayList<Object>();
    }

    @Test
    public void regularRPC() throws Exception {
        String methodName = "public java.lang.String java.lang.Object.toString()";

        this.client.onRPC(methodName, noParams);
        verify(this.server).onRPC(methodName, noParams);

        // Check that DM methods were not called
        verify(this.server, never()).migrateObject(ExplicitMigrationPolicyTestConstants.regularRPC_testDestAddr);
    }

    // ToDo: Mock the oms of KernelServerImpl and getServers() of OMSServer or OMSServerImpl

    // Once getServers() of oms is mocked then following test cases should pass
    // Currently added as ignored test cases which need above mentioned mocking

    @Test @Ignore
    public void basicExplicitMigration() throws Exception {
        String explicitMigrateObject = "public void sapphire.policy.mobility.explicitmigration.ExplicitMigrationImpl.migrateObject() throws java.lang.Exception";

        // After mocking following should be the server list returned from getServers() of OMSServerImpl
        ArrayList<InetSocketAddress> mockedServerList = new ArrayList<InetSocketAddress>();
        mockedServerList.add(ExplicitMigrationPolicyTestConstants.basicExplicitMigration_kernelServerAddr1);
        mockedServerList.add(ExplicitMigrationPolicyTestConstants.basicExplicitMigration_kernelServerAddr2);

        // After mocking ExplicitMigrationPolicyTestConstants.basicExplicitMigration_localServerAddr should be localServerAddress

        ArrayList<Object> testDestAddrList = new ArrayList<Object>();
        testDestAddrList.add(ExplicitMigrationPolicyTestConstants.basicExplicitMigration_testDestAddr);

        this.client.onRPC(explicitMigrateObject, testDestAddrList);
        verify(this.server).onRPC(explicitMigrateObject, testDestAddrList);

        // Check that DM methods were called only once
        verify(this.server, times(1)).migrateObject(ExplicitMigrationPolicyTestConstants.basicExplicitMigration_testDestAddr);
    }

    @Test @Ignore
    public void selfExplicitMigration() throws Exception {
        String explicitMigrateObject = "public void sapphire.policy.mobility.explicitmigration.ExplicitMigrationImpl.migrateObject() throws java.lang.Exception";

        // After mocking following should be the server list returned from getServers() of OMSServerImpl
        ArrayList<InetSocketAddress> mockedServerList = new ArrayList<InetSocketAddress>();
        mockedServerList.add(ExplicitMigrationPolicyTestConstants.selfExplicitMigration_kernelServerAddr1);
        mockedServerList.add(ExplicitMigrationPolicyTestConstants.selfExplicitMigration_kernelServerAddr2);

        // After mocking ExplicitMigrationPolicyTestConstants.selfExplicitMigration_localServerAddr should be localServerAddress

        ArrayList<Object> testDestAddrList = new ArrayList<Object>();
        testDestAddrList.add(ExplicitMigrationPolicyTestConstants.selfExplicitMigration_testDestAddr);

        this.client.onRPC(explicitMigrateObject, testDestAddrList);
        verify(this.server).onRPC(explicitMigrateObject, testDestAddrList);

        // Check that DM methods were called only once
        verify(this.server, times(1)).migrateObject(ExplicitMigrationPolicyTestConstants.selfExplicitMigration_testDestAddr);

        thrown.expect(DestinationSameAsSourceKernelServerException.class);
        thrown.expectMessage("The local and destinations Kernel Server address of migrations are same");
    }

    @Test @Ignore
    public void destinationNotFoundExplicitMigration() throws Exception {
        String explicitMigrateObject = "public void sapphire.policy.mobility.explicitmigration.ExplicitMigrationImpl.migrateObject() throws java.lang.Exception";

        // After mocking following should be the server list returned from getServers() of OMSServerImpl
        ArrayList<InetSocketAddress> mockedServerList = new ArrayList<InetSocketAddress>();
        mockedServerList.add(ExplicitMigrationPolicyTestConstants.destinationNotFoundExplicitMigration_kernelServerAddr1);
        mockedServerList.add(ExplicitMigrationPolicyTestConstants.destinationNotFoundExplicitMigration_kernelServerAddr2);
        mockedServerList.add(ExplicitMigrationPolicyTestConstants.destinationNotFoundExplicitMigration_kernelServerAddr3);

        // After mocking ExplicitMigrationPolicyTestConstants.destinationNotFoundExplicitMigration_localServerAddr should be localServerAddress

        ArrayList<Object> testDestAddrList = new ArrayList<Object>();
        testDestAddrList.add(ExplicitMigrationPolicyTestConstants.destinationNotFoundExplicitMigration_testDestAddr);

        this.client.onRPC(explicitMigrateObject, testDestAddrList);
        verify(this.server).onRPC(explicitMigrateObject, testDestAddrList);

        // Check that DM methods were called only once
        verify(this.server, times(1)).migrateObject(ExplicitMigrationPolicyTestConstants.destinationNotFoundExplicitMigration_testDestAddr);

        thrown.expect(NotFoundDestinationKernelServerException.class);
        thrown.expectMessage("The destinations address passed is not present as one of the Kernel Servers");
    }
}

// Stub because AppObject expects a stub/subclass of the original class.
class ExplicitMigrationTestStub extends ExplicitMigrationPolicyTest.ExplicitMigrationTest implements Serializable {}

