package sapphire.policy.serializability;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

import sapphire.common.AppObject;
import sapphire.common.Utils;

import org.junit.Rule;
import org.junit.rules.ExpectedException;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;



/**
 * Created by Venugopal Reddy K 00900280 on 2/2/18.
 */

public class OptConcurrentTransactPolicyTest {
    OptConcurrentTransactPolicy.ClientPolicy client1;
    OptConcurrentTransactPolicy.ClientPolicy client2;
    OptConcurrentTransactPolicy.ServerPolicy server;

    private OptConcurrentTransactionTest so;
    private AppObject appObject;
    private ArrayList<Object> noParams, oneParam, twoParam, threeParam;
    String startMethodName = "public void sapphire.policy.serializability.OptConcurrentTransactionImpl.startTransaction() throws java.lang.Exception",
            commitMethodName = "public void sapphire.policy.serializability.OptConcurrentTransactionImpl.commitTransaction() throws java.lang.Exception",
            rollbackMethodName = "public void sapphire.policy.serializability.OptConcurrentTransactionImpl.rollbackTransaction() throws java.lang.Exception",
            setMethodName = "public void sapphire.policy.serializability.OptConcurrentTransactionTest.setI(int)",
            getMethodName = "public int sapphire.policy.serializability.OptConcurrentTransactionTest.getI()";

    @Before
    public void setUp() throws Exception {
        this.client1 = spy(OptConcurrentTransactPolicy.ClientPolicy.class);
        this.client2 = spy(OptConcurrentTransactPolicy.ClientPolicy.class);
        so = new OptConcurrentTransactPolicyTestStub();
        appObject = new AppObject(so);
        this.server = spy(OptConcurrentTransactPolicy.ServerPolicy.class);
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
        verify(this.server, never()).getTransaction();
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
        assertEquals(((OptConcurrentTransactionTest)appObject.getObject()).getI(), 2);
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
        assertEquals(so.getI(), 2);

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
        //thrown.expect(Exception.class);
        //thrown.expectMessage(containsString(" failed"));
        this.client1.onRPC(commitMethodName, noParams);
        assertEquals(so.getI(), 2);

    }

    @Test
    public void startTransactionAndRollback() throws Exception {

        // Update the object to 1
        this.client1.onRPC(setMethodName, oneParam);
        verify(this.server).onRPC(setMethodName, oneParam);
        assertEquals(1, so.getI());

        // Start a transaction
        AppObject clone = (AppObject)Utils.ObjectCloner.deepCopy(appObject);
        // We need to mock the implementation in this case, because in this fake unit test environment,
        // RMI does not occur, so client and server DM's end up referring to the same object (rather than
        // different objects, due to RMI serialization.
        doReturn(clone).when(this.server).sapphire_getAppObject();
        this.client1.onRPC(startMethodName, noParams);
        // Update the object again, this time to 2
        this.client1.onRPC(setMethodName, twoParam);
        verify(this.server, never()).onRPC(setMethodName, twoParam);
        // Check that the client has the new value.
        assertEquals(2, this.client1.onRPC(getMethodName, noParams));
        verify(this.server, never()).onRPC(getMethodName, noParams);

        // Check that the server has the old value.
        assertEquals(1, so.getI());
        // Rollback the transaction
        this.client1.onRPC(rollbackMethodName, noParams);

        // Verify that the value is retrieved from server through rpc call
        assertEquals(1, this.client1.onRPC(getMethodName, noParams));
        verify(this.server).onRPC(getMethodName, noParams);
    }
}

// Stub because AppObject expects a stub/subclass of the original class.
class OptConcurrentTransactPolicyTestStub extends OptConcurrentTransactionTest implements Serializable {}

