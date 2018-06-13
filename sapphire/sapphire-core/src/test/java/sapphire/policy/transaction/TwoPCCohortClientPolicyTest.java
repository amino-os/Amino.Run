package sapphire.policy.transaction;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import sapphire.common.ReflectionTestUtil;

import java.util.ArrayList;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.junit.Assert.assertEquals;
import static sapphire.policy.transaction.TwoPCCohortPolicy.*;

public class TwoPCCohortClientPolicyTest {
    @Before
    @After
    public void cleanContext() {
        TransactionContext.leaveTransaction();
    }

    @Test
    public void test_regular_passthrough_if_no_txn() throws Exception {
        TwoPCCohortServerPolicy serverPolicy = mock(TwoPCCohortServerPolicy.class);

        TwoPCCohortClientPolicy clientPolicy = new TwoPCCohortClientPolicy();
        clientPolicy.setServer(serverPolicy);

        clientPolicy.onRPC("foo", null);

        verify(serverPolicy, times(1)).onRPC("foo", null);
    }

    @Test
    public void test_register_if_in_tx() throws Exception {
        final TwoPCParticipants participants = mock(TwoPCParticipants.class);
        TwoPCCohortServerPolicy serverPolicy = mock(TwoPCCohortServerPolicy.class);

        TwoPCCohortClientPolicy clientPolicy = new TwoPCCohortClientPolicy();
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
