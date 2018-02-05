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

/**
 * Created by mbssaiakhil on 23/1/18.
 */

@RunWith(MockitoJUnitRunner.class)
public class ExplicitMigrationPolicyTest {
    ExplicitMigrationPolicy.ClientPolicy client;
    ExplicitMigrationPolicy.ServerPolicy server;
    private sapphire.policy.mobility.explicitmigration.ExplicitMigrationTest so;
    private AppObject appObject;
    private ArrayList<Object> noParams, oneParam;

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
        oneParam = new ArrayList<Object>();
        oneParam.add(new InetSocketAddress("192.168.42.146", 22342));
    }

    @Test
    public void regularRPC() throws Exception {
        String methodName = "public java.lang.String java.lang.Object.toString()";
        InetSocketAddress testDestAddr = new InetSocketAddress("192.168.42.146", 22342);

        this.client.onRPC(methodName, noParams);
        verify(this.server).onRPC(methodName, noParams);

        // Check that DM methods were not called
        verify(this.server, never()).migrateObject(testDestAddr);
    }

    // ToDo: Mock the oms of KernelServerImpl and getServers() of OMSServer or OMSServerImpl

    // Once getServers() of oms is mocked then following test cases should pass
    // Currently added as ignored test cases which need above mentioned mocking

    @Test @Ignore
    public void basicExplicitMigration() throws Exception {
        String explicitMigrateObject = "public void sapphire.policy.mobility.explicitmigration.ExplicitMigrationImpl.migrateObject() throws java.lang.Exception";
        InetSocketAddress testDestAddr = new InetSocketAddress("192.168.42.146", 22342);

        // After mocking following should be the server list returned from getServers() of OMSServerImpl
        ArrayList<InetSocketAddress> mockedServerList = new ArrayList<InetSocketAddress>();
        mockedServerList.add(new InetSocketAddress("192.168.42.145", 22342));
        mockedServerList.add(new InetSocketAddress("192.168.42.146", 22342));

        // After mocking following should be localServerAddress
        InetSocketAddress localServerAddr = new InetSocketAddress("192.168.42.145", 22342);

        ArrayList<InetSocketAddress> someList = new ArrayList<InetSocketAddress>();
        someList.add(testDestAddr);

        this.client.onRPC(explicitMigrateObject, oneParam);
        verify(this.server).onRPC(explicitMigrateObject, oneParam);

        // Check that DM methods were called only once
        verify(this.server, times(1)).migrateObject(testDestAddr);
    }

    @Test @Ignore
    public void selfExplicitMigration() throws Exception {
        String explicitMigrateObject = "public void sapphire.policy.mobility.explicitmigration.ExplicitMigrationImpl.migrateObject() throws java.lang.Exception";
        InetSocketAddress testDestAddr = new InetSocketAddress("192.168.42.146", 22342);

        // After mocking following should be the server list returned from getServers() of OMSServerImpl
        ArrayList<InetSocketAddress> mockedServerList = new ArrayList<InetSocketAddress>();
        mockedServerList.add(new InetSocketAddress("192.168.42.145", 22342));
        mockedServerList.add(new InetSocketAddress("192.168.42.146", 22342));

        // After mocking following should be localServerAddress
        InetSocketAddress localServerAddr = new InetSocketAddress("192.168.42.146", 22342);

        ArrayList<InetSocketAddress> someList = new ArrayList<InetSocketAddress>();
        someList.add(testDestAddr);

        this.client.onRPC(explicitMigrateObject, oneParam);
        verify(this.server).onRPC(explicitMigrateObject, oneParam);

        // Check that DM methods were called only once
        verify(this.server, times(1)).migrateObject(testDestAddr);

        thrown.expect(DestinationSameAsSourceKernelServerException.class);
        thrown.expectMessage("The local and destinations Kernel Server address of migrations are same");
    }

    @Test @Ignore
    public void destinationNotFoundExplicitMigration() throws Exception {
        String explicitMigrateObject = "public void sapphire.policy.mobility.explicitmigration.ExplicitMigrationImpl.migrateObject() throws java.lang.Exception";
        InetSocketAddress testDestAddr = new InetSocketAddress("192.168.42.145", 22342);

        // After mocking following should be the server list returned from getServers() of OMSServerImpl
        ArrayList<InetSocketAddress> mockedServerList = new ArrayList<InetSocketAddress>();
        mockedServerList.add(new InetSocketAddress("192.168.42.145", 22342));
        mockedServerList.add(new InetSocketAddress("192.168.42.146", 22342));
        mockedServerList.add(new InetSocketAddress("192.168.42.147", 22342));

        // After mocking following should be localServerAddress
        InetSocketAddress localServerAddr = new InetSocketAddress("192.168.42.148", 22342);

        ArrayList<InetSocketAddress> someList = new ArrayList<InetSocketAddress>();
        someList.add(testDestAddr);

        this.client.onRPC(explicitMigrateObject, oneParam);
        verify(this.server).onRPC(explicitMigrateObject, oneParam);

        // Check that DM methods were called only once
        verify(this.server, times(1)).migrateObject(testDestAddr);

        thrown.expect(NotFoundDestinationKernelServerException.class);
        thrown.expectMessage("The destinations address passed is not present as one of the Kernel Servers");
    }
}

// Stub because AppObject expects a stub/subclass of the original class.
class ExplicitMigrationTestStub extends ExplicitMigrationTest implements Serializable {}

