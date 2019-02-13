package amino.run.policy.transaction;

import static amino.run.policy.transaction.TwoPCCohortPolicy.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class TwoPCCohortClientPolicyTest {
    @Before
    @After
    public void cleanContext() {
        TransactionContext.leaveTransaction();
    }

    @Test
    public void test_regular_passthrough_if_no_txn() throws Exception {
        TwoPCCohortPolicy.ServerPolicy serverPolicy = mock(TwoPCCohortPolicy.ServerPolicy.class);

        TwoPCCohortPolicy.ClientPolicy clientPolicy = new TwoPCCohortPolicy.ClientPolicy();
        clientPolicy.setServer(serverPolicy);

        clientPolicy.onRPC("foo", null);

        verify(serverPolicy, times(1)).onRPC("foo", null);
    }

    @Test
    public void test_register_if_in_tx() throws Exception {
        final TwoPCParticipants participants = mock(TwoPCParticipants.class);
        TwoPCCohortPolicy.ServerPolicy serverPolicy = mock(TwoPCCohortPolicy.ServerPolicy.class);

        TwoPCCohortPolicy.ClientPolicy clientPolicy = new TwoPCCohortPolicy.ClientPolicy();
        clientPolicy.setServer(serverPolicy);

        UUID txnId = UUID.randomUUID();
        TransactionContext.enterTransaction(txnId, participants);

        clientPolicy.onRPC("foo", null);

        verify(participants, times(1)).register(clientPolicy);
        ArgumentCaptor<ArrayList<Object>> argCaptor = new ArgumentCaptor<ArrayList<Object>>();
        verify(serverPolicy, times(1)).onRPC(eq("tx_rpc"), argCaptor.capture());
        ArrayList<Object> args = argCaptor.getValue();
        TransactionWrapper rpcTransaction = new TransactionWrapper("tx_rpc", args);
        assertEquals(rpcTransaction.getTransaction(), txnId);
        assertEquals(rpcTransaction.getInnerRPCMethod(), "foo");
    }

    @Test(expected = IllegalComponentException.class)
    public void test_disallow_uncapable_SO_in_txn() throws Exception {
        final TwoPCParticipants participants = mock(TwoPCParticipants.class);
        DefaultServerPolicy server = mock(DefaultServerPolicy.class);
        DefaultClientPolicy other = new DefaultClientPolicy();
        other.setServer(server);
        UUID txnId = UUID.randomUUID();
        TransactionContext.enterTransaction(txnId, participants);

        other.onRPC("foo", null);
    }
}
