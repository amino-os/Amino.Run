package sapphire.policy.serializability;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

import sapphire.common.AppObject;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;



/**
 * Created by Venugopal Reddy K 00900280 on 2/2/18.
 */

public class OptimisticLockTransactPolicyTest {
    LockingTransactionPolicy.ClientPolicy client1;
    LockingTransactionPolicy.ClientPolicy client2;
    LockingTransactionPolicy.ServerPolicy server;

    private LockingTransactionTest so;
    private AppObject appObject;
    private ArrayList<Object> noParams, oneParam, twoParam, threeParam;
    String startMethodName = "public void sapphire.policy.serializability.LockingTransactionImpl.startTransaction() throws java.lang.Exception",
            commitMethodName = "public void sapphire.policy.serializability.LockingTransactionImpl.commitTransaction() throws java.lang.Exception",
            setMethodName = "public void sapphire.policy.serializability.LockingTransactionTest.setI(int)",
            getMethodName = "public int sapphire.policy.serializability.LockingTransactionTest.getI()";

    @Before
    public void setUp() throws Exception {
        this.client1 = spy(OptimisticLockTransactPolicy.ClientPolicy.class);
        this.client2 = spy(OptimisticLockTransactPolicy.ClientPolicy.class);
        so = new OptimisticLockTransactPolicyTestStub();
        appObject = new AppObject(so);
        this.server = spy(OptimisticLockTransactPolicy.ServerPolicy.class);
        this.server.$__initialize(appObject);
        this.client1.setServer(this.server);
        this.client2.setServer(this.server);
        noParams = new ArrayList<Object>();
        oneParam = new ArrayList<Object>();
        oneParam.add(new Integer(1));
        twoParam = new ArrayList<Object>();
        twoParam.add(new Integer(2));
        threeParam = new ArrayList<Object>();
        threeParam.add(new Integer(3));
    }

    @Test
    public void regularRPC() throws Exception {
        String methodName = "public java.lang.String java.lang.Object.toString()";
        this.client1.onRPC(methodName, noParams);
        verify(this.server).onRPC(methodName, noParams);
        // Check that DM methods were not called
        verify(this.server, never()).getLease((Matchers.anyLong()));
    }

    @Test
    public void startAndCommitTransaction() throws Exception {

        // Update the object to 1 from first client
        this.client1.onRPC(setMethodName, oneParam);
        verify(this.server).onRPC(setMethodName, oneParam);
        assertEquals(so.getI(), 1);

        // Start a transaction with first client
        this.client1.onRPC(startMethodName, noParams);

        // Update the object to 2 from the first client
        this.client1.onRPC(setMethodName, twoParam);
        assertEquals(so.getI(), 2);
        // Check that it was not executed against the server.
        verify(this.server, never()).onRPC(setMethodName, twoParam);
        // Commit the transaction for first client
        this.client1.onRPC(commitMethodName, noParams);
        // Check that it got sync'd to the server.
        verify(this.server).syncObject((UUID) any(), (Serializable)any());

        // Verify that the object has been updated
        this.client1.onRPC(getMethodName, noParams);
        verify(this.server).onRPC(getMethodName, noParams);
        assertEquals(((LockingTransactionTest)appObject.getObject()).getI(), 2);
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void startAndCommitTransactionMultipleClients() throws Exception {

        // Update the object to 1 from first client
        this.client1.onRPC(setMethodName, oneParam);
        verify(this.server).onRPC(setMethodName, oneParam);
        assertEquals(so.getI(), 1);

        // Update the object to 3 from second client
        this.client2.onRPC(setMethodName, threeParam);
        verify(this.server).onRPC(setMethodName, threeParam);
        assertEquals(so.getI(), 3);

        // Start transaction with first client
        this.client1.onRPC(startMethodName, noParams);

        // Start transaction with second client
        this.client2.onRPC(startMethodName, noParams);

        // Update the object to 2 from first client
        this.client1.onRPC(setMethodName, twoParam);
        assertEquals(so.getI(), 2);

        // Check that it was not executed against the server.
        verify(this.server, never()).onRPC(setMethodName, twoParam);

        // Commit the transaction of first client
        this.client1.onRPC(commitMethodName, noParams);
        // Check that it got sync'd to the server.
        verify(this.server).syncObject((UUID) any(), (Serializable)any());

        // Check that it was not executed against the server.
        verify(this.server, never()).onRPC(setMethodName, twoParam);

        // Verify that the object has been updated
        this.client1.onRPC(getMethodName, noParams);
        verify(this.server).onRPC(getMethodName, noParams);
        assertEquals(((LockingTransactionTest)appObject.getObject()).getI(), 2);

        // Commit the transaction of second client
        thrown.expect(Exception.class);
        thrown.expectMessage(containsString(" is invalid"));
        this.client2.onRPC(commitMethodName, noParams);
    }

    @Test
    public void startOneTransactionAndDoNonTransactionRpcMultipleClients() throws Exception {

        // Start transaction with first client
        this.client1.onRPC(startMethodName, noParams);

        // Update the object to 3 from second client
        this.client2.onRPC(setMethodName, threeParam);
        verify(this.server).onRPC(setMethodName, threeParam);
        assertEquals(so.getI(), 3);

        // Update the object to 2 from first client
        this.client1.onRPC(setMethodName, twoParam);
        assertEquals(so.getI(), 2);

        // Commit the transaction of first client
        this.client1.onRPC(commitMethodName, noParams);
        assertEquals(so.getI(), 2);

    }
}

// Stub because AppObject expects a stub/subclass of the original class.
class OptimisticLockTransactPolicyTestStub extends LockingTransactionTest implements Serializable {}

