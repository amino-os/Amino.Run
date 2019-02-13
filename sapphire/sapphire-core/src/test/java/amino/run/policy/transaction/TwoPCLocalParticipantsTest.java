package amino.run.policy.transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import amino.run.policy.Policy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class TwoPCLocalParticipantsTest {
    UUID id = UUID.randomUUID();
    Policy.ClientPolicy part1 = mock(TwoPCCohortPolicy.ClientPolicy.class);
    Policy.ClientPolicy part2 = mock(TwoPCCohortPolicy.ClientPolicy.class);
    TwoPCLocalParticipants participants = new TwoPCLocalParticipants();

    @Before
    public void setup() {
        participants.addParticipants(id, Arrays.asList(part1, part2));
    }

    @Test
    public void test_fanout_to_all() throws Exception {
        participants.fanOutTransactionPrimitive(id, "tx_commit");

        verify(part1).onRPC(eq("tx_rpc"), any(ArrayList.class));
        ArgumentCaptor<ArrayList> argumentCaptor = ArgumentCaptor.forClass(ArrayList.class);
        verify(part2).onRPC(eq("tx_rpc"), argumentCaptor.capture());
        ArrayList<Object> params = argumentCaptor.getValue();
        TransactionWrapper tx = new TransactionWrapper("tx_rpc", params);
        assertEquals("tx_commit", tx.getInnerRPCMethod());
    }

    @Test
    public void test_on_all_yes_voted() throws Exception {
        when(part1.onRPC(eq("tx_rpc"), any(ArrayList.class)))
                .thenReturn(TransactionManager.Vote.YES);
        when(part2.onRPC(eq("tx_rpc"), any(ArrayList.class)))
                .thenReturn(TransactionManager.Vote.YES);

        Boolean isAllYes = participants.allParticipantsVotedYes(id);

        assertTrue(isAllYes);
    }

    @Test
    public void test_on_any_not_yes_voted() throws Exception {
        when(part1.onRPC(eq("tx_rpc"), any(ArrayList.class)))
                .thenReturn(TransactionManager.Vote.YES);
        when(part2.onRPC(eq("tx_rpc"), any(ArrayList.class)))
                .thenReturn(TransactionManager.Vote.UNCERTIAN);

        Boolean isAllYes = participants.allParticipantsVotedYes(id);

        assertFalse(isAllYes);
    }
}
