package sapphire.policy.checkpoint.durableserializablerpc;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import sapphire.common.AppObject;
import sapphire.policy.checkpoint.durableserializable.DurableSerializableRPCPolicy;
import sapphire.policy.checkpoint.periodiccheckpoint.PeriodicCheckpointPolicyTest.PeriodicCheckpointerTest;
import sapphire.policy.serializability.SerializableRPCPolicyTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by quinton on 1/16/18.
 * Also runs all of the SerializableRPC tests from the base class.
 * Durability tests are run here.
 */

public class DurableSerializableRPCPolicyTest extends SerializableRPCPolicyTest {
    DurableSerializableRPCPolicy.ClientPolicy checkpointClient;
    DurableSerializableRPCPolicy.ServerPolicy checkpointServer;
    private PeriodicCheckpointerTest so;
    private AppObject checkpointAppObject;
    private ArrayList<Object> noParams, oneParam, twoParam;


    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.checkpointClient = spy(DurableSerializableRPCPolicy.ClientPolicy.class);
        this.checkpointServer = spy(DurableSerializableRPCPolicy.ServerPolicy.class);
        so = new sapphire.policy.checkpoint.periodiccheckpoint.PeriodicCheckpointPolicyTest.PeriodicCheckpointerTestStub();
        checkpointAppObject = new AppObject(so);
        checkpointServer.$__initialize(checkpointAppObject);
        this.checkpointClient.setServer(this.checkpointServer);
        noParams = new ArrayList<Object>();
        oneParam = new ArrayList<Object>();
        oneParam.add(new Integer(1));
        twoParam = new ArrayList<Object>();
        twoParam.add(new Integer(2));
    }

    @Test
    public void regularRPCWithAutomaticSaveCheckpoint() throws Exception {
        String methodName = "public java.lang.String java.lang.Object.toString()";
        this.checkpointClient.onRPC(methodName, noParams);
        // Check that the server got the request.
        verify(this.checkpointServer).onRPC(methodName, noParams);
       // Check that DM saved a checkpoint.
        verify(this.checkpointServer, times(1)).saveCheckpoint();
        // And didn't try to restore it
        verify(this.checkpointServer, never()).restoreCheckpoint();
    }

    @Test
    public void exceptionRPCWithAutomaticRestoreCheckpoint() throws Exception {
        String regularMethodName = "public void sapphire.policy.checkpoint.periodiccheckpoint.PeriodicCheckpointPolicyTest$PeriodicCheckpointerTest.setI(int)",
            exceptionMethodName = "public void sapphire.policy.checkpoint.periodiccheckpoint.PeriodicCheckpointPolicyTest$PeriodicCheckpointerTest.failingSetMethod(int)",
            getMethodName = "public int sapphire.policy.checkpoint.periodiccheckpoint.PeriodicCheckpointPolicyTest$PeriodicCheckpointerTest.getI()";

        boolean exceptionWasThrown = false;

        this.checkpointClient.onRPC(regularMethodName, oneParam);
        verify(this.checkpointServer).onRPC(regularMethodName, oneParam);
        // Check that DM saved a checkpoint.
        verify(this.checkpointServer, times(1)).saveCheckpoint();
        // Verify that the object has been updated
        this.checkpointClient.onRPC(getMethodName, noParams);
        verify(this.checkpointServer).onRPC(getMethodName, noParams);
        assertEquals(((PeriodicCheckpointerTest)checkpointAppObject.getObject()).getI(), 1);

        try {
            this.checkpointClient.onRPC(exceptionMethodName, twoParam); // This sets the value to two, then throws an exception
        }
        catch (Exception e) {
            exceptionWasThrown = true;
            // Discard the exception - we were expecting it.
        }
        assertTrue(exceptionWasThrown);
        // Check that correct DM method was called.
        verify(this.checkpointServer, times(1)).restoreCheckpoint();

        // Verify that the object has been restored
        this.checkpointClient.onRPC(getMethodName, noParams);
        assertEquals(((PeriodicCheckpointerTest)checkpointAppObject.getObject()).getI(), 1);
    }
}


