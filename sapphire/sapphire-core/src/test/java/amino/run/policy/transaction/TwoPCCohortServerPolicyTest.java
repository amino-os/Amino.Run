package amino.run.policy.transaction;

import static amino.run.policy.Upcalls.ServerUpcalls;
import static amino.run.policy.transaction.TwoPCCohortPolicy.TwoPCCohortServerPolicy;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import amino.run.common.AppObject;
import amino.run.common.ReflectionTestUtil;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TwoPCCohortServerPolicyTest {
    private SandboxProvider sandboxProvider = mock(SandboxProvider.class);
    private TransactionManager transactionManager = mock(TransactionManager.class);
    private TwoPCCohortServerPolicy serverPolicy = new TwoPCCohortServerPolicy();

    @Before
    public void Setup() throws NoSuchFieldException, IllegalAccessException {
        ReflectionTestUtil.setField(this.serverPolicy, "sandboxProvider", sandboxProvider);
        ReflectionTestUtil.setField(this.serverPolicy, "transactionManager", transactionManager);
    }

    @After
    public void clean() {
        TransactionContext.leaveTransaction();
    }

    @Test
    public void test_none_transaction_rpc_passes_though() throws Exception {
        AppObject appObject = mock(AppObject.class);
        when(appObject.invoke("foo", null)).thenReturn("bar");
        serverPolicy.$__initialize(appObject);

        Object result = serverPolicy.onRPC("foo", null, "foo", null);

        verifyNoMoreInteractions(sandboxProvider);
        verifyZeroInteractions(transactionManager);
        verify(appObject).invoke("foo", null);
        assertEquals("bar", result);
    }

    @Test
    public void test_transaction_rpc_engages_sandbox() throws Exception {
        UUID transactionId = UUID.randomUUID();
        TransactionWrapper wrapper = new TransactionWrapper(transactionId, "foo", null);

        ServerUpcalls sandbox = mock(ServerUpcalls.class);
        when(this.sandboxProvider.getSandbox(this.serverPolicy, transactionId)).thenReturn(sandbox);

        Object result =
                serverPolicy.onRPC(
                        "tx_rpc", wrapper.getRPCParams(), "tx_rpc", wrapper.getRPCParams());

        verify(transactionManager).join(transactionId);
        verify(sandboxProvider).getSandbox(this.serverPolicy, transactionId);
        verify(sandbox).onRPC("foo", null, "foo", null);
        verify(transactionManager).leave(transactionId);
    }

    @Test
    public void test_transaction_primitive_vote_req() throws Exception {
        UUID transactionId = UUID.randomUUID();
        TransactionWrapper wrapper = new TransactionWrapper(transactionId, "tx_vote_req", null);

        Object result =
                this.serverPolicy.onRPC(
                        "tx_rpc", wrapper.getRPCParams(), "tx_rpc", wrapper.getRPCParams());

        verify(this.transactionManager).vote(transactionId);
    }
}
