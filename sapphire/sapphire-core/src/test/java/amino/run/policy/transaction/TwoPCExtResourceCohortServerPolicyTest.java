package amino.run.policy.transaction;

import static org.mockito.Mockito.*;

import amino.run.common.AppObject;
import amino.run.common.ReflectionTestUtil;
import java.io.Serializable;
import java.util.UUID;
import org.junit.Test;

public class TwoPCExtResourceCohortServerPolicyTest {
    @Test
    public void test_tx_engages_extResource_on_join() throws Exception {
        UUID transactionId = UUID.randomUUID();

        TwoPCExtResourceCohortPolicy.ServerPolicy serverPolicy =
                new TwoPCExtResourceCohortPolicy.ServerPolicy();
        SandboxProvider sandboxProvider = mock(SandboxProvider.class);
        ReflectionTestUtil.setField(serverPolicy, "sandboxProvider", sandboxProvider);
        TransactionManager transactionManager = mock(TransactionManager.class);
        serverPolicy.setTransactionManager(
                new ExtResourceTransactionManager(sandboxProvider, transactionManager));

        TransactionManager businessObj =
                mock(TransactionManager.class, withSettings().extraInterfaces(Serializable.class));
        AppObject appObject = new AppObject(businessObj);

        AppObjectShimServerPolicy sandboxedServerPolicy = mock(AppObjectShimServerPolicy.class);
        when(sandboxedServerPolicy.getAppObject()).thenReturn(appObject);

        when(sandboxProvider.getSandbox(serverPolicy, transactionId))
                .thenReturn(sandboxedServerPolicy);
        when(sandboxProvider.getSandbox(transactionId)).thenReturn(sandboxedServerPolicy);

        serverPolicy.$__initialize(appObject);

        TransactionWrapper wrapper = new TransactionWrapper(transactionId, "foo", null);

        Object result = serverPolicy.onRPC("tx_rpc", wrapper.getRPCParams());

        verify(businessObj).join(transactionId);
        verify(transactionManager).join(transactionId);
    }
}
