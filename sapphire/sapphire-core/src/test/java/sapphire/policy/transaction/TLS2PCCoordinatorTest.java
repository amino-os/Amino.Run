package sapphire.policy.transaction;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import sapphire.common.ReflectionTestUtil;
import sapphire.policy.serializability.TransactionAlreadyStartedException;

public class TLS2PCCoordinatorTest {
    private TransactionValidator validator = mock(TransactionValidator.class);
    private TLS2PCCoordinator coordinator = new TLS2PCCoordinator(validator);
    private TwoPCLocalParticipants participants = mock(TwoPCLocalParticipants.class);
    private TwoPCParticipants participantManager = mock(TwoPCParticipants.class);
    private UUID transactionId = UUID.randomUUID();

    @Before
    public void setup() throws NoSuchFieldException, IllegalAccessException {
        TransactionContext.leaveTransaction();
        ReflectionTestUtil.setField(this.coordinator, "validator", validator);
        ReflectionTestUtil.setField(this.coordinator, "localParticipantsManager", participants);
    }

    @Test(expected = TransactionAlreadyStartedException.class)
    public void test_coordinator_throws_on_reentrant() throws TransactionAlreadyStartedException {
        TransactionContext.enterTransaction(UUID.randomUUID(), participantManager);

        coordinator.beginTransaction();
    }

    @Test
    public void test_yes_if_promise_went_well() throws Exception {
        when(validator.promises(transactionId)).thenReturn(true);
        when(participants.allParticipantsVotedYes(transactionId)).thenReturn(true);

        TransactionManager.Vote vote = this.coordinator.vote(transactionId);

        assertEquals(TransactionManager.Vote.YES, vote);
    }

    @Test
    public void test_nogo_if_promise_rejected() throws Exception {
        when(validator.promises(transactionId)).thenReturn(false);
        when(participants.allParticipantsVotedYes(transactionId)).thenReturn(true);

        TransactionManager.Vote vote = this.coordinator.vote(transactionId);

        assertEquals(TransactionManager.Vote.NO, vote);
    }
}
