package amino.run.policy.transaction;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import amino.run.common.ReflectionTestUtil;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;

public class TLSTransactionManagerTest {
    UUID txId;
    TwoPCLocalParticipants participantsManager;
    TLSTransactionManager txManager = new TLSTransactionManager();
    TransactionValidator txPromiser = mock(TransactionValidator.class);

    @Before
    public void setup() throws NoSuchFieldException, IllegalAccessException {
        this.txId = UUID.randomUUID();
        this.participantsManager = mock(TwoPCLocalParticipants.class);

        ReflectionTestUtil.setField(
                this.txManager, "localParticipantsManager", participantsManager);
        this.txManager.setValidator(txPromiser);
    }

    @Test
    public void test_commit_fanout() throws Exception {
        txManager.commit(txId);
        verify(this.participantsManager).fanOutTransactionPrimitive(txId, "tx_commit");
    }

    @Test
    public void test_vote_yes_on_all_good() throws Exception {
        when(this.participantsManager.allParticipantsVotedYes(any(UUID.class))).thenReturn(true);
        when(this.txPromiser.promises(any(UUID.class))).thenReturn(true);

        TwoPCLocalStatus statusManager = mock(TwoPCLocalStatus.class);
        when(statusManager.getStatus(txId)).thenReturn(TwoPCLocalStatus.LocalStatus.GOOD);
        ReflectionTestUtil.setField(this.txManager, "localStatusManager", statusManager);

        TransactionManager.Vote result = txManager.vote(txId);

        assertEquals(TransactionManager.Vote.YES, result);
    }

    @Test
    public void test_vote_no_on_any_nogood() throws Exception {
        when(this.participantsManager.allParticipantsVotedYes(any(UUID.class))).thenReturn(false);

        TwoPCLocalStatus statusManager = mock(TwoPCLocalStatus.class);
        when(statusManager.getStatus(txId)).thenReturn(TwoPCLocalStatus.LocalStatus.GOOD);
        ReflectionTestUtil.setField(this.txManager, "localStatusManager", statusManager);

        TransactionManager.Vote result = txManager.vote(txId);

        assertEquals(TransactionManager.Vote.NO, result);
    }
}
