package sapphire.policy.checkpoint.explicitcheckpoint;

import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

import sapphire.common.AppObject;
import sapphire.policy.checkpoint.explicitcheckpoint.ExplicitCheckpointer;
import sapphire.policy.checkpoint.explicitcheckpoint.ExplicitCheckpointPolicy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;

import static org.mockito.Mockito.withSettings;

/**
 * Created by quinton on 1/16/18.
 */

public class ExplicitCheckpointPolicyTest {
    ExplicitCheckpointPolicy.ClientPolicy client;
    ExplicitCheckpointPolicy.ServerPolicy server;
    private ExplicitCheckpointerTest so;
    private AppObject appObject;
    private ArrayList<Object> noParams, oneParam, twoParam;


    @Before
    public void setUp() throws Exception {
        this.client = spy(ExplicitCheckpointPolicy.ClientPolicy.class);
        this.server = spy(ExplicitCheckpointPolicy.ServerPolicy.class);
        so = new ExplicitCheckpointerTestStub();
        appObject = new AppObject(so);
        server.$__initialize(appObject);
        this.client.setServer(this.server);
        noParams = new ArrayList<Object>();
        oneParam = new ArrayList<Object>();
        oneParam.add(new Integer(1));
        twoParam = new ArrayList<Object>();
        twoParam.add(new Integer(2));
    }

    @Test
    public void regularRPC() throws Exception {
        String methodName = "public java.lang.String java.lang.Object.toString()";
        this.client.onRPC(methodName, noParams);
        verify(this.server).onRPC(methodName, noParams);
       // Check that DM methods were not called
        verify(this.server, never()).saveCheckpoint();
        verify(this.server, never()).restoreCheckpoint();
    }

    @Test
    public void basicSaveCheckpoint() throws Exception {
        String methodName = "public void sapphire.policy.checkpoint.explicitcheckpoint.ExplicitCheckpointerImpl.saveCheckpoint() throws java.lang.Exception";
        this.client.onRPC(methodName, noParams);
        verify(this.server).onRPC(methodName, noParams);
        // Check that correct DM method was called.
        verify(this.server, times(1)).saveCheckpoint();
        verify(this.server, never()).restoreCheckpoint();
    }

    @Test
    public void saveAndRestoreCheckpoint() throws Exception {
        String saveMethodName = "public void sapphire.policy.checkpoint.explicitcheckpoint.ExplicitCheckpointerImpl.saveCheckpoint() throws java.lang.Exception",
                restoreMethodName = "public void sapphire.policy.checkpoint.explicitcheckpoint.ExplicitCheckpointerImpl.restoreCheckpoint() throws java.lang.Exception",
                setMethodName = "public void sapphire.policy.checkpoint.explicitcheckpoint.ExplicitCheckpointerTest.setI(int)",
                getMethodName = "public int sapphire.policy.checkpoint.explicitcheckpoint.ExplicitCheckpointerTest.getI()";

        // Update the object to 1
        this.client.onRPC(setMethodName, oneParam);
        verify(this.server).onRPC(setMethodName, oneParam);
        assertEquals(so.getI(), 1);

        // Save a checkpoint
        this.client.onRPC(saveMethodName, noParams);
        verify(this.server).onRPC(saveMethodName, noParams);
        verify(this.server, never()).restoreCheckpoint();
        verify(this.server, times(1)).saveCheckpoint();

        // Update the object again, this time to 2
        this.client.onRPC(setMethodName, twoParam);
        verify(this.server).onRPC(setMethodName, twoParam);
        assertEquals(so.getI(), 2);

        // Restore the previous checkpoint
        this.client.onRPC(restoreMethodName, noParams);
        verify(this.server).onRPC(restoreMethodName, noParams);

        // Verify that the object has been restored
        this.client.onRPC(getMethodName, noParams);
        verify(this.server).onRPC(getMethodName, noParams);
        assertEquals(((ExplicitCheckpointerTest)appObject.getObject()).getI(), 1);
    }
}


// Stub because AppObject expects a stub/subclass of the original class.
class ExplicitCheckpointerTestStub extends ExplicitCheckpointerTest implements Serializable {}
