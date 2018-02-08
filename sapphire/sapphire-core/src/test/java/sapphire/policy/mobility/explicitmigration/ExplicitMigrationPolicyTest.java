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
import sapphire.kernel.common.KernelObjectMigratingException;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServerImpl;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
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
    }

    @Test
    public void regularRPC() throws Exception {
        String methodName = "public java.lang.String java.lang.Object.toString()";

        this.client.onRPC(methodName, noParams);
        verify(this.server).onRPC(methodName, noParams);

        // Check that DM methods were not called
        verify(this.server, never()).migrateObject(ExplicitMigrationPolicyTestConstants.regularRPC_testDestAddr);
    }

    @Test
    public void retryRegularRPCFromClient() throws Exception {
        String methodName = "public java.lang.String java.lang.Object.toString()";

        // Mocking the Server Policy such that, would always throw KernelObjectMigratingException onRPC()
        // In order to test the scenario the exponential backoff retry in this case
        ExplicitMigrationPolicy.ServerPolicy mockServerPolicy = mock(ExplicitMigrationPolicy.ServerPolicy.class);
        when(mockServerPolicy.onRPC(methodName, noParams)).thenThrow(new KernelObjectMigratingException());

        this.client.setServer(mockServerPolicy);
        try {
            this.client.onRPC(methodName, noParams);
        } catch (Exception e) {
            // Caught the KernelObjectMigratingException as a user
        }

        // Check that onRPC() of server is called 4 times, as per the current values which
        // decide the number of exponential backoffs
        verify(mockServerPolicy, times(4)).onRPC(methodName, noParams);

        // Check that DM method i.e migrateObject(...) was not called
        verify(mockServerPolicy, never()).migrateObject((InetSocketAddress)any());
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

    @Test @Ignore
    public void retryMigrateObjectRPCFromClient() throws Exception {
        String explicitMigrateObject = "public void sapphire.policy.mobility.explicitmigration.ExplicitMigrationImpl.migrateObject() throws java.lang.Exception";
        oneParam.add(ExplicitMigrationPolicyTestConstants.retryMigrateObjectRPCFromClient_testDestAddr);

        // After mocking following should be the server list returned from getServers() of OMSServerImpl
        ArrayList<InetSocketAddress> mockedServerList = new ArrayList<InetSocketAddress>();
        mockedServerList.add(ExplicitMigrationPolicyTestConstants.retryMigrateObjectRPCFromClient_kernelServerAddr1);
        mockedServerList.add(ExplicitMigrationPolicyTestConstants.retryMigrateObjectRPCFromClient_kernelServerAddr2);
        mockedServerList.add(ExplicitMigrationPolicyTestConstants.retryMigrateObjectRPCFromClient_kernelServerAddr2);

        // After mocking ExplicitMigrationPolicyTestConstants.destinationNotFoundExplicitMigration_localServerAddr should be localServerAddress

        ArrayList<Object> testDestAddrList = new ArrayList<Object>();
        testDestAddrList.add(ExplicitMigrationPolicyTestConstants.destinationNotFoundExplicitMigration_testDestAddr);

        try {
            this.client.onRPC(explicitMigrateObject, oneParam);
        } catch (Exception e) {
            // Caught the KernelObjectMigratingException as a user
        }

        // Check that onRPC() of server is called 4 times, as per the current values which
        // decide the number of exponential backoffs
        verify(this.server, times(4)).onRPC(explicitMigrateObject, oneParam);

        // Check that DM method migrateObject(...) was called all the 4 times, the onRPC() was called
        verify(this.server, times(4)).migrateObject(ExplicitMigrationPolicyTestConstants.retryMigrateObjectRPCFromClient_testDestAddr);
    }
}

// Stub because AppObject expects a stub/subclass of the original class
class ExplicitMigrationTestStub extends ExplicitMigrationPolicyTest.ExplicitMigrationTest implements Serializable {}

