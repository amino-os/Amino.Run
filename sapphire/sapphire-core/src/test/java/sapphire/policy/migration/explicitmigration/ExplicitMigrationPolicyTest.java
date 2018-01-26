package sapphire.policy.migration.explicitmigration;

import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;
import java.util.ArrayList;

import sapphire.common.AppObject;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Created by mbssaiakhil on 23/1/18.
 */

public class ExplicitMigrationPolicyTest {
    ExplicitMigrationPolicy.ClientPolicy client;
    ExplicitMigrationPolicy.ServerPolicy server;
    private ExplicitMigrationTest so;
    private AppObject appObject;
    private ArrayList<Object> noParams;


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
        verify(this.server, never()).migrateObject();
    }

    // ToDo: Mock the getServers() of OMS
    // After mocking aAdd more test cases, where only 1 Kernel Server or
    // multiple Kernel Servers are registered to the OMS
}

// Stub because AppObject expects a stub/subclass of the original class.
class ExplicitMigrationTestStub extends ExplicitMigrationTest implements Serializable {}

