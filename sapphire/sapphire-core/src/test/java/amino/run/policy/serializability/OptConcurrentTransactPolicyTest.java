package amino.run.policy.serializability;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import amino.run.common.AppObject;
import amino.run.common.Utils;
import java.io.Serializable;
import java.util.ArrayList;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/** Created by Venugopal Reddy K 00900280 on 2/2/18. */
public class OptConcurrentTransactPolicyTest {
    OptConcurrentTransactPolicy.ClientPolicy client1;
    OptConcurrentTransactPolicy.ClientPolicy client2;
    OptConcurrentTransactPolicy.ServerPolicy server;

    private OptConcurrentTransactionTest_Stub so;
    private AppObject appObject;
    private ArrayList<Object> noParams, oneParam, twoParam, threeParam;
    String
            startMethodName =
                    "public void amino.run.policy.serializability.OptConcurrentTransactPolicyTest$OptConcurrentTransactionTest.startTransaction() throws amino.run.policy.serializability.TransactionAlreadyStartedException,amino.run.policy.serializability.TransactionException",
            commitMethodName =
                    "public void amino.run.policy.serializability.OptConcurrentTransactPolicyTest$OptConcurrentTransactionTest.commitTransaction() throws amino.run.policy.serializability.NoTransactionStartedException,amino.run.policy.serializability.TransactionException",
            rollbackMethodName =
                    "public void amino.run.policy.serializability.OptConcurrentTransactPolicyTest$OptConcurrentTransactionTest.rollbackTransaction() throws amino.run.policy.serializability.NoTransactionStartedException,amino.run.policy.serializability.TransactionException",
            setMethodName =
                    "public void amino.run.policy.serializability.OptConcurrentTransactPolicyTest$OptConcurrentTransactionTest.setI(int)",
            getMethodName =
                    "public int amino.run.policy.serializability.OptConcurrentTransactPolicyTest$OptConcurrentTransactionTest.getI()";

    /* APP SO class */
    public static class OptConcurrentTransactionTest extends TransactionImpl {
        int i = 0;

        public void setI(int i) {
            this.i = i;
        }

        public int getI() {
            return i;
        }
    }

    /* APP SO Stub class */
    public static class OptConcurrentTransactionTest_Stub
            extends OptConcurrentTransactPolicyTest.OptConcurrentTransactionTest {}

    @Before
    public void setUp() throws Exception {
        this.client1 = spy(OptConcurrentTransactPolicy.ClientPolicy.class);
        this.client2 = spy(OptConcurrentTransactPolicy.ClientPolicy.class);
        so = new OptConcurrentTransactionTest_Stub();
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
        verify(this.server, never()).sapphire_getAppObject();
    }

    @Test
    public void startAndCommitTransaction() throws Exception {

        // Update the object to 1 from first client
        this.client1.onRPC(setMethodName, oneParam);
        verify(this.server).onRPC(setMethodName, oneParam);
        assertEquals(so.getI(), 1);

        // In UT env, client and server are referring to the same app object(rather on different
        // object).
        // We need to mock the get app object method to avoid that.
        // Client operates on the cloned copy of appObject. And server operates on appObject
        AppObject clonedAppObject = (AppObject) Utils.ObjectCloner.deepCopy(appObject);
        doReturn(clonedAppObject).when(this.server).sapphire_getAppObject();

        OptConcurrentTransactionTest clonedSo =
                (OptConcurrentTransactionTest) clonedAppObject.getObject();

        // Start a transaction with first client
        this.client1.onRPC(startMethodName, noParams);

        // Update the object to 2 from the first client
        this.client1.onRPC(setMethodName, twoParam);
        assertEquals(clonedSo.getI(), 2);

        // Check that it was not executed against the server.
        verify(this.server, never()).onRPC(setMethodName, twoParam);

        // Server need to get the actual appObject(not the clone). Set back to real method call.
        when(this.server.sapphire_getAppObject()).thenCallRealMethod();

        // Commit the transaction for first client
        this.client1.onRPC(commitMethodName, noParams);

        // Check that it got sync'd to the server.
        verify(this.server).syncObject((byte[]) any(), (Serializable) any());

        // Verify that the object has been updated
        this.client1.onRPC(getMethodName, noParams);
        verify(this.server).onRPC(getMethodName, noParams);
        assertEquals(((OptConcurrentTransactionTest) appObject.getObject()).getI(), 2);
    }

    @Test
    public void startAndCommitTransactionWithOutModifyingObject() throws Exception {
        // Update the object to 1 from first client
        this.client1.onRPC(setMethodName, oneParam);
        verify(this.server).onRPC(setMethodName, oneParam);
        assertEquals(so.getI(), 1);

        // In UT env, client and server are referring to the same app object(rather on different
        // object).
        // We need to mock the get app object method to avoid that.
        // Client operates on the cloned copy of appObject. And server operates on appObject
        AppObject clonedAppObject = (AppObject) Utils.ObjectCloner.deepCopy(appObject);
        doReturn(clonedAppObject).when(this.server).sapphire_getAppObject();

        OptConcurrentTransactionTest clonedSo =
                (OptConcurrentTransactionTest) clonedAppObject.getObject();

        // Start a transaction with first client
        this.client1.onRPC(startMethodName, noParams);

        // Commit the transaction for first client without changing the object state
        this.client1.onRPC(commitMethodName, noParams);
        // Check that object is not sync'd to the server.
        verify(this.server, never()).syncObject((byte[]) any(), (Serializable) any());
    }

    @Rule public ExpectedException thrown = ExpectedException.none();

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

        // In UT env, client and server are referring to the same app object(rather on different
        // object).
        // We need to mock the get app object method to avoid that.
        // Client operates on the cloned copy of appObject. And server operates on appObject
        AppObject clonedAppObject1 = (AppObject) Utils.ObjectCloner.deepCopy(appObject);
        doReturn(clonedAppObject1).when(this.server).sapphire_getAppObject();

        OptConcurrentTransactionTest clonedSo1 =
                (OptConcurrentTransactionTest) clonedAppObject1.getObject();
        // Start transaction with first client
        this.client1.onRPC(startMethodName, noParams);

        // In UT env, client and server are referring to the same app object(rather on different
        // object).
        // We need to mock the get app object method to avoid that.
        // Client operates on the cloned copy of appObject. And server operates on appObject
        AppObject clonedAppObject2 = (AppObject) Utils.ObjectCloner.deepCopy(appObject);
        doReturn(clonedAppObject2).when(this.server).sapphire_getAppObject();

        OptConcurrentTransactionTest clonedSo2 =
                (OptConcurrentTransactionTest) clonedAppObject2.getObject();

        // Start transaction with second client
        this.client2.onRPC(startMethodName, noParams);

        // Update the object to 2 from first client
        this.client1.onRPC(setMethodName, twoParam);
        assertEquals(clonedSo1.getI(), 2);

        // Check that it was not executed against the server.
        verify(this.server, never()).onRPC(setMethodName, twoParam);

        // Server need to get the actual appObject(not the clone). Set back to real method call.
        when(this.server.sapphire_getAppObject()).thenCallRealMethod();

        // Commit the transaction of first client
        this.client1.onRPC(commitMethodName, noParams);

        // Check that it got sync'd to the server.
        verify(this.server).syncObject((byte[]) any(), (Serializable) any());

        // Check that it was not executed against the server.
        verify(this.server, never()).onRPC(setMethodName, twoParam);

        // Verify that the actual object has been updated(non transactional rpc to server)
        this.client1.onRPC(getMethodName, noParams);
        verify(this.server).onRPC(getMethodName, noParams);
        assertEquals(((OptConcurrentTransactionTest) (appObject.getObject())).getI(), 2);

        // Update the object to 1 from second client
        this.client2.onRPC(setMethodName, oneParam);
        assertEquals(clonedSo2.getI(), 1);

        /* Commit the transaction of second client. It should fail as the first client has
        already committed the transaction */
        thrown.expect(TransactionException.class);
        thrown.expectMessage(containsString(" is invalid"));
        this.client2.onRPC(commitMethodName, noParams);
    }

    @Test
    public void startOneTransactionAndDoNonTransactionRpcMultipleClients() throws Exception {

        // In UT env, client and server are referring to the same app object(rather on different
        // object).
        // We need to mock the get app object method to avoid that.
        // Client operates on the cloned copy of appObject. And server operates on appObject
        AppObject clonedAppObject = (AppObject) Utils.ObjectCloner.deepCopy(appObject);
        doReturn(clonedAppObject).when(this.server).sapphire_getAppObject();

        OptConcurrentTransactionTest clonedSo =
                (OptConcurrentTransactionTest) clonedAppObject.getObject();

        // Start transaction with first client
        this.client1.onRPC(startMethodName, noParams);

        // Update the object to 3 from second client
        this.client2.onRPC(setMethodName, threeParam);
        verify(this.server).onRPC(setMethodName, threeParam);
        assertEquals(so.getI(), 3);

        // Update the object to 2 from first client
        this.client1.onRPC(setMethodName, twoParam);
        assertEquals(clonedSo.getI(), 2);

        // Commit the transaction of first client
        thrown.expect(TransactionException.class);
        thrown.expectMessage(containsString(" is invalid"));
        this.client1.onRPC(commitMethodName, noParams);
    }

    @Test
    public void startTransactionAndRollback() throws Exception {

        // Update the object to 1
        this.client1.onRPC(setMethodName, oneParam);
        verify(this.server).onRPC(setMethodName, oneParam);
        assertEquals(1, so.getI());

        // Start a transaction
        AppObject clonedAppObject = (AppObject) Utils.ObjectCloner.deepCopy(appObject);

        // We need to mock the implementation in this case, because in this fake unit test
        // environment,
        // RMI does not occur, so client and server DM's end up referring to the same object (rather
        // than
        // different objects, due to RMI serialization.
        doReturn(clonedAppObject).when(this.server).sapphire_getAppObject();

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

    @Test
    public void startTransactWhenTransactIsInProgress() throws Exception {

        // Start a transaction
        AppObject clonedAppObject = (AppObject) Utils.ObjectCloner.deepCopy(appObject);

        // We need to mock the implementation in this case, because in this fake unit test
        // environment,
        // RMI does not occur, so client and server DM's end up referring to the same object (rather
        // than
        // different objects, due to RMI serialization.
        doReturn(clonedAppObject).when(this.server).sapphire_getAppObject();

        this.client1.onRPC(startMethodName, noParams);

        thrown.expect(TransactionAlreadyStartedException.class);
        thrown.expectMessage(containsString("Transaction already started on Sapphire object."));
        this.client1.onRPC(startMethodName, noParams);
    }

    @Test
    public void commitTransactWhenTransactNotStarted() throws Exception {

        thrown.expect(NoTransactionStartedException.class);
        thrown.expectMessage(containsString("No transaction to commit."));
        this.client1.onRPC(commitMethodName, noParams);
    }

    @Test
    public void rollbackTransactWhenTransactNotStarted() throws Exception {

        thrown.expect(NoTransactionStartedException.class);
        thrown.expectMessage(containsString("No transaction to rollback."));
        this.client1.onRPC(rollbackMethodName, noParams);
    }
}
