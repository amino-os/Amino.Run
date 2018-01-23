package sapphire.policy.serializability;

import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;
import java.util.ArrayList;

import sapphire.common.AppObject;
import sapphire.policy.checkpoint.explicitcheckpoint.ExplicitCheckpointPolicy;
import sapphire.policy.checkpoint.explicitcheckpoint.ExplicitCheckpointerTest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by quinton on 1/16/18.
 */

public class LockingTransactionPolicyTest {
    LockingTransactionPolicy.ClientPolicy client;
    LockingTransactionPolicy.ServerPolicy server;
    private LockingTransactionTest so;
    private AppObject appObject;
    private ArrayList<Object> noParams, oneParam, twoParam;


    @Before
    public void setUp() throws Exception {
        this.client = spy(LockingTransactionPolicy.ClientPolicy.class);
        this.server = spy(LockingTransactionPolicy.ServerPolicy.class);
        so = new LockingTransactionTestStub();
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
        verify(this.server, never()).getLease();
    }

    @Test
    public void startAndCommitTransaction() throws Exception {
        String startMethodName = "public void sapphire.policy.serializability.LockingTransactionImpl.startTransaction() throws java.lang.Exception",
                commitMethodName = "public void sapphire.policy.serializability.LockingTransactionImpl.commitTransaction() throws java.lang.Exception",
                setMethodName = "public void sapphire.policy.serializability.LockingTransactionTest.setI(int)",
                getMethodName = "public int sapphire.policy.serializability.LockingTransactionTest.getI()";

        // Update the object to 1
        this.client.onRPC(setMethodName, oneParam);
        verify(this.server).onRPC(setMethodName, oneParam);
        assertEquals(so.getI(), 1);

        // Start a transaction
        this.client.onRPC(startMethodName, noParams);
        // Update the object again, this time to 2
        this.client.onRPC(setMethodName, twoParam);
        assertEquals(so.getI(), 2);
        // Commit the transaction
        this.client.onRPC(commitMethodName, noParams);

        // Verify that the object has been updated
        this.client.onRPC(getMethodName, noParams);
        verify(this.server).onRPC(getMethodName, noParams);
        assertEquals(((LockingTransactionTest)appObject.getObject()).getI(), 2);
    }
    @Test
    public void startAndRollbackTransaction() throws Exception {
        String startMethodName = "public void sapphire.policy.serializability.LockingTransactionImpl.startTransaction() throws java.lang.Exception",
                rollbackMethodName = "public void sapphire.policy.serializability.LockingTransactionImpl.rollbackTransaction() throws java.lang.Exception",
                setMethodName = "public void sapphire.policy.serializability.LockingTransactionTest.setI(int)",
                getMethodName = "public int sapphire.policy.serializability.LockingTransactionTest.getI()";

        // Update the object to 1
        this.client.onRPC(setMethodName, oneParam);
        verify(this.server).onRPC(setMethodName, oneParam);
        assertEquals(1, so.getI());

        // Start a transaction
        this.client.onRPC(startMethodName, noParams);
        // Update the object again, this time to 2
        this.client.onRPC(setMethodName, twoParam);
        verify(this.server, never()).onRPC(setMethodName, twoParam);
        assertEquals(2, so.getI());
        // Rollback the transaction
        this.client.onRPC(rollbackMethodName, noParams);

        // Verify that the object has been restored
        assertEquals(1, this.client.onRPC(getMethodName, noParams));
        verify(this.server).onRPC(getMethodName, noParams);
        // assertEquals(1,((LockingTransactionTest)appObject.getObject()).getI());
    }
}

// Stub because AppObject expects a stub/subclass of the original class.
class LockingTransactionTestStub extends LockingTransactionTest {}



