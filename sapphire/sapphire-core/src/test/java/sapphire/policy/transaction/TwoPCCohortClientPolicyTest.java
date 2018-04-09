package sapphire.policy.transaction;

import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.UUID;

import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static sapphire.policy.transaction.TwoPCCohortPolicy.*;

import static org.junit.Assert.assertEquals;

public class TwoPCCohortClientPolicyTest {
    @After
    public void cleanContext(){
        TransactionContext.leaveTransaction();
    }

    @Test
    public void test_no_participant_if_no_txn() throws Exception {
        final TwoPCParticipants participants = mock(TwoPCParticipants.class);
        TwoPCCohortServerPolicy serverPolicy = mock(TwoPCCohortServerPolicy.class);

        TwoPCCohortClientPolicy clientPolicy = new TwoPCCohortClientPolicy();
        clientPolicy.setServer(serverPolicy);
        clientPolicy.setParticipantManagerProvider( new TwoPCCohortClientPolicy.ParticipantManagerProvider() {
            @Override
            public TwoPCParticipants Get() {
                return participants;
            }
        });

        clientPolicy.onRPC("foo", null);

        verifyZeroInteractions(participants);
        verify(serverPolicy, times(1)).onRPC("foo", null);
    }

    @Test
    public void test_register_if_in_tx() throws Exception {
        final TwoPCParticipants participants = mock(TwoPCParticipants.class);
        TwoPCCohortServerPolicy serverPolicy = mock(TwoPCCohortServerPolicy.class);

        TwoPCCohortClientPolicy clientPolicy = new TwoPCCohortClientPolicy();
        clientPolicy.setServer(serverPolicy);
        clientPolicy.setParticipantManagerProvider(new TwoPCCohortClientPolicy.ParticipantManagerProvider() {
            @Override
            public TwoPCParticipants Get() {
                return participants;
            }
        });

        UUID txnId = UUID.randomUUID();
        TransactionContext.enterTransaction(txnId);

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
        DefaultServerPolicy server = mock(DefaultServerPolicy.class);
        DefaultClientPolicy other = new DefaultClientPolicy();
        other.setServer(server);
        UUID txnId = UUID.randomUUID();
        TransactionContext.enterTransaction(txnId);

        other.onRPC("foo", null);
    }
}
