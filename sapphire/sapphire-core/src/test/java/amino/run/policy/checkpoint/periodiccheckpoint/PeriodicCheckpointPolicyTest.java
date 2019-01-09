package amino.run.policy.checkpoint.periodiccheckpoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import amino.run.common.AppObject;
import java.io.Serializable;
import java.util.ArrayList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/** Created by quinton on 1/16/18. */
public class PeriodicCheckpointPolicyTest {
    PeriodicCheckpointPolicy.ClientPolicy client;
    PeriodicCheckpointPolicy.ServerPolicy server;
    private PeriodicCheckpointerTest so;
    private AppObject appObject;
    private ArrayList<Object> noParams, oneParam, twoParam;

    @Before
    public void setUp() throws Exception {
        this.client = Mockito.spy(PeriodicCheckpointPolicy.ClientPolicy.class);
        this.server = Mockito.spy(PeriodicCheckpointPolicy.ServerPolicy.class);
        so = new PeriodicCheckpointerTestStub();
        appObject = new AppObject(so);
        server.$__initialize(appObject);
        this.client.setServer(this.server);
        noParams = new ArrayList<Object>();
        oneParam = new ArrayList<Object>();
        oneParam.add(new Integer(1));
        twoParam = new ArrayList<Object>();
        twoParam.add(new Integer(2));
    }

    @After
    public void tearDown() {
        this.server.deleteCheckpoint();
    }

    @Test
    public void regularRPCWithAutomaticSaveCheckpoint() throws Exception {
        String methodName = "public java.lang.String java.lang.Object.toString()";
        this.client.onRPC(methodName, noParams);
        // Check that the server got the request.
        verify(this.server).onRPC(methodName, noParams);
        // Check that DM saved a checkpoint.
        verify(this.server, times(1)).saveCheckpoint();
        // And didn't try to restore it
        verify(this.server, never()).restoreCheckpoint();
    }

    @Test
    public void exceptionRPCWithAutomaticRestoreCheckpoint() throws Exception {
        String
                regularMethodName =
                        "public void amino.run.policy.checkpoint.periodiccheckpoint.PeriodicCheckpointPolicyTest$PeriodicCheckpointerTest.setI(int)",
                exceptionMethodName =
                        "public void amino.run.policy.checkpoint.periodiccheckpoint.PeriodicCheckpointPolicyTest$PeriodicCheckpointerTest.failingSetMethod(int)",
                getMethodName =
                        "public int amino.run.policy.checkpoint.periodiccheckpoint.PeriodicCheckpointPolicyTest$PeriodicCheckpointerTest.getI()";

        boolean exceptionWasThrown = false;

        this.client.onRPC(regularMethodName, oneParam);
        verify(this.server).onRPC(regularMethodName, oneParam);
        // Check that DM saved a checkpoint.
        verify(this.server, times(1)).saveCheckpoint();
        // Verify that the object has been updated
        this.client.onRPC(getMethodName, noParams);
        verify(this.server).onRPC(getMethodName, noParams);
        assertEquals(((PeriodicCheckpointerTest) appObject.getObject()).getI(), 1);

        try {
            this.client.onRPC(
                    exceptionMethodName,
                    twoParam); // This sets the value to two, then throws an exception
        } catch (Exception e) {
            exceptionWasThrown = true;
            // Discard the exception - we were expecting it.
        }
        assertTrue(exceptionWasThrown);
        // Check that correct DM method was called.
        verify(this.server, times(1)).restoreCheckpoint();

        // Verify that the object has been restored
        this.client.onRPC(getMethodName, noParams);
        assertEquals(((PeriodicCheckpointerTest) appObject.getObject()).getI(), 1);
    }

    public static class PeriodicCheckpointerTest implements Serializable {
        int i = 0;

        public void setI(int i) {
            this.i = i;
        }

        public int getI() {
            return i;
        }

        /**
         * failingSetMethod changes the state of the object, and then throws an exception. Used for
         * unit testing of rollback to previous checkpoint.
         *
         * @param i
         * @return
         */
        public int failingSetMethod(int i) throws Exception {
            this.i = i;
            throw new Exception("Set i to " + i + " and then threw an exception");
        }
    }
    // Stub because AppObject expects a stub/subclass of the original class.
    public static class PeriodicCheckpointerTestStub
            extends PeriodicCheckpointPolicyTest.PeriodicCheckpointerTest implements Serializable {}
}
