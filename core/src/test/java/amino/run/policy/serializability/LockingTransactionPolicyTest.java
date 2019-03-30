package amino.run.policy.serializability;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import amino.run.common.AppObject;
import amino.run.common.Utils;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Matchers;
import org.mockito.Mockito;

/** Created by quinton on 1/16/18. */
public class LockingTransactionPolicyTest {
    LockingTransactionPolicy.ClientPolicy client;
    LockingTransactionPolicy.ServerPolicy server;

    private LockingTransactionTest so;
    private AppObject appObject;
    private ArrayList<Object> noParams, oneParam, twoParam;
    String
            startMethodName =
                    "public void amino.run.policy.serializability.LockingTransactionImpl.startTransaction() throws amino.run.policy.serializability.TransactionAlreadyStartedException,amino.run.policy.serializability.TransactionException",
            commitMethodName =
                    "public void amino.run.policy.serializability.LockingTransactionImpl.commitTransaction() throws amino.run.policy.serializability.NoTransactionStartedException,amino.run.policy.serializability.TransactionException",
            rollbackMethodName =
                    "public void amino.run.policy.serializability.LockingTransactionImpl.rollbackTransaction() throws amino.run.policy.serializability.NoTransactionStartedException,amino.run.policy.serializability.TransactionException",
            setMethodName =
                    "public void amino.run.policy.serializability.LockingTransactionTest.setI(int)",
            getMethodName =
                    "public int amino.run.policy.serializability.LockingTransactionTest.getI()";

    @Rule public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        this.client = Mockito.spy(LockingTransactionPolicy.ClientPolicy.class);
        so = new LockingTransactionTestStub();
        appObject = new AppObject(so);
        this.server = Mockito.spy(LockingTransactionPolicy.ServerPolicy.class);
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
        verify(this.server).syncObject((UUID) any(), (Serializable) any());

        // Verify that the object has been updated
        this.client.onRPC(getMethodName, noParams);
        verify(this.server).onRPC(getMethodName, noParams);
        assertEquals(((LockingTransactionTest) appObject.getObject()).getI(), 2);
    }

    @Test
    public void startAndRollbackTransaction() throws Exception {
        // Update the object to 1
        this.client.onRPC(setMethodName, oneParam);
        verify(this.server).onRPC(setMethodName, oneParam);
        assertEquals(1, so.getI());

        // Start a transaction
        AppObject clone = (AppObject) Utils.ObjectCloner.deepCopy(appObject);
        // We need to mock the implementation in this case, because in this fake unit test
        // environment,
        // RMI does not occur, so client and server DM's end up referring to the same object (rather
        // than
        // different objects, due to RMI serialization.
        doReturn(clone).when(this.server).getAppObject();
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

    @Test
    public void startTransactionWithMultipleClients() throws Exception {
        // Start transaction with first client
        this.client.onRPC(startMethodName, noParams);

        // Update the object to 2 from first client
        this.client.onRPC(setMethodName, twoParam);
        assertEquals(2, this.client.onRPC(getMethodName, noParams));

        /* Create another client */
        LockingTransactionPolicy.ClientPolicy client2 =
                Mockito.spy(LockingTransactionPolicy.ClientPolicy.class);
        client2.setServer(this.server);

        // Start a transaction from new client
        thrown.expect(TransactionException.class);
        thrown.expectMessage(containsString("Failed to start a transaction."));
        client2.onRPC(startMethodName, noParams);
    }

    @Test
    public void startTransactWithSingleClientWhenTransactIsInProgress() throws Exception {
        this.client.onRPC(startMethodName, noParams);

        thrown.expect(TransactionAlreadyStartedException.class);
        thrown.expectMessage(containsString("Transaction already started on MicroService object."));
        this.client.onRPC(startMethodName, noParams);
    }

    @Test
    public void commitTransactWhenTransactNotStarted() throws Exception {
        thrown.expect(NoTransactionStartedException.class);
        thrown.expectMessage(containsString("No transaction to commit."));
        this.client.onRPC(commitMethodName, noParams);
    }

    @Test
    public void rollbackTransactWhenTransactNotStarted() throws Exception {
        thrown.expect(NoTransactionStartedException.class);
        thrown.expectMessage(containsString("No transaction to rollback."));
        this.client.onRPC(rollbackMethodName, noParams);
    }
}

// Stub because AppObject expects a stub/subclass of the original class.
class LockingTransactionTestStub extends LockingTransactionTest implements Serializable {}
