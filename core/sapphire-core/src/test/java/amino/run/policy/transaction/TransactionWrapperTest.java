package amino.run.policy.transaction;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import org.junit.Test;

public class TransactionWrapperTest {
    @Test
    public void test_wrap_rpc_in_tx() {
        String method = "foo";
        ArrayList<Object> params = new ArrayList<Object>();
        UUID transactionId = UUID.randomUUID();
        TransactionWrapper wrapper = new TransactionWrapper(transactionId, method, params);

        ArrayList<Object> wrappedMessage = wrapper.getRPCParams();
        assertEquals(wrappedMessage.get(0), transactionId);
        ArrayList<Object> innerRPCMessage = (ArrayList<Object>) wrappedMessage.get(1);
        assertEquals(innerRPCMessage.get(0), method);
        assertEquals(innerRPCMessage.get(1), params);
    }

    @Test
    public void test_extract_rpc_wrapper() {
        UUID id = UUID.randomUUID();
        ArrayList<Object> params = new ArrayList<Object>();

        ArrayList<Object> wrapped =
                new ArrayList<Object>(
                        Arrays.asList(id, new ArrayList<Object>(Arrays.asList("bar", params))));

        TransactionWrapper parser = new TransactionWrapper("tx_rpc", wrapped);

        assertEquals(parser.getTransaction(), id);
        assertEquals(parser.getInnerRPCMethod(), "bar");
        assertEquals(parser.getInnerRPCParams(), params);
    }
}
