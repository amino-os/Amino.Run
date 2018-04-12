package sapphire.policy.transaction;

import org.junit.Before;
import org.junit.Test;
import sapphire.policy.SapphirePolicy.SapphireClientPolicy;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class TLSTransactionManagerTest {
    UUID txId;
    SapphireClientPolicy part1, part2;
    TwoPCLocalParticipants participantsManager;
    TLSTransactionManager txManager = new TLSTransactionManager();

    @Before
    public void setup() {
        this.txId = UUID.randomUUID();
        this.part1 = mock(TwoPCCohortPolicy.TwoPCCohortClientPolicy.class);
        this.part2 = mock(TwoPCCohortPolicy.TwoPCCohortClientPolicy.class);
        this.participantsManager = mock(TwoPCLocalParticipants.class);
        when(participantsManager.getParticipants(this.txId)).thenReturn(Arrays.asList(this.part1, this.part2));

        this.txManager.setLocalParticipantsManager(participantsManager);
    }

    @Test
    public void test_commit_fanout() throws Exception {
        txManager.commit(txId);

        verify(part1).onRPC(eq("tx_rpc"), any());
        verify(part2).onRPC(eq("tx_rpc"), any());
    }

    @Test
    public void test_vote_yes_on_all_good() throws Exception {
        when(part1.onRPC(eq("tx_rpc"), any())).thenReturn(TransactionManager.Vote.YES);
        when(part2.onRPC(eq("tx_rpc"), any())).thenReturn(TransactionManager.Vote.YES);

        TwoPCLocalStatus statusManager = mock(TwoPCLocalStatus.class);
        when(statusManager.getStatus(txId)).thenReturn(TwoPCLocalStatus.LocalStatus.GOOD);

        this.txManager.setLocalStatusManager(statusManager);

        TransactionManager.Vote result = txManager.vote(txId);

        assertEquals(TransactionManager.Vote.YES, result);
    }

    @Test
    public void test_vote_no_on_any_nogood() throws Exception {
        when(part1.onRPC(eq("tx_rpc"), any())).thenReturn(TransactionManager.Vote.YES);
        when(part2.onRPC(eq("tx_rpc"), any())).thenReturn(TransactionManager.Vote.UNCERTIAN);

        TwoPCLocalStatus statusManager = mock(TwoPCLocalStatus.class);
        when(statusManager.getStatus(txId)).thenReturn(TwoPCLocalStatus.LocalStatus.GOOD);

        this.txManager.setLocalStatusManager(statusManager);

        TransactionManager.Vote result = txManager.vote(txId);

        assertEquals(TransactionManager.Vote.NO, result);
    }
}
