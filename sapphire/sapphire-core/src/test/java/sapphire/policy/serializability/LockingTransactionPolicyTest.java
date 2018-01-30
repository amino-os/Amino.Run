package sapphire.policy.serializability;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

import sapphire.common.AppObject;
import sapphire.common.Utils;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.any;

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
        so = new LockingTransactionTestStub();
        appObject = new AppObject(so);
        this.server = spy(LockingTransactionPolicy.ServerPolicy.class);
        this.server.$__initialize(appObject);
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
        verify(this.server, never()).getLease((Matchers.anyLong()));
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
        // Check that it was not executed against the server.
        verify(this.server, never()).onRPC(setMethodName, twoParam);
        // Commit the transaction
        this.client.onRPC(commitMethodName, noParams);
        // Check that it got sync'd to the server.
        verify(this.server).syncObject((UUID) any(), (Serializable)any());

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
        AppObject clone = (AppObject)Utils.ObjectCloner.deepCopy(appObject);
        // We need to mock the implementation in this case, because in this fake unit test environment,
        // RMI does not occur, so client and server DM's end up referring to the same object (rather than
        // different objects, due to RMI serialization.
        doReturn(clone).when(this.server).sapphire_getAppObject();
        this.client.onRPC(startMethodName, noParams);
        // Update the object again, this time to 2
        this.client.onRPC(setMethodName, twoParam);
        verify(this.server, never()).onRPC(setMethodName, twoParam);
        // Check that the client has the new value.
        assertEquals(2, this.client.onRPC(getMethodName, noParams));
        // Check that the server has the old value.
        assertEquals(1, so.getI());
        // Rollback the transaction
        this.client.onRPC(rollbackMethodName, noParams);

        // Verify that the object has been restored when viewed from the client.
        assertEquals(1, this.client.onRPC(getMethodName, noParams));
        // ... and on the server.
        assertEquals(1, so.getI());

        verify(this.server).onRPC(getMethodName, noParams);
    }
}

// Stub because AppObject expects a stub/subclass of the original class.
class LockingTransactionTestStub extends LockingTransactionTest implements Serializable {}



