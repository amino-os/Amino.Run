package sapphire.policy.transaction;

import org.junit.Before;
import org.junit.Test;
import sapphire.common.AppObject;

import java.io.Serializable;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ExtResourceTransactionManagerTest {
    private UUID transcationId = UUID.randomUUID();
    private TransactionManager businessObject = mock(TransactionManager.class, withSettings().extraInterfaces(Serializable.class));
    private AppObject appObject = new AppObject(businessObject);
    private AppObjectShimServerPolicy appObjectShimServerPolicy = mock(AppObjectShimServerPolicy.class);
    private SandboxProvider sandboxProvider = mock(SandboxProvider.class);
    private TransactionManager intTxManager = mock(TransactionManager.class);
    private ExtResourceTransactionManager txMgr = new ExtResourceTransactionManager(sandboxProvider, intTxManager);

    @Before
    public void setup() throws TransactionExecutionException {
        when(appObjectShimServerPolicy.getAppObject()).thenReturn(appObject);
        when(sandboxProvider.getSandbox(transcationId)).thenReturn(appObjectShimServerPolicy);
        ExtResourceTransactionManager txMgr = new ExtResourceTransactionManager(sandboxProvider, intTxManager);
    }

    @Test
    public void test_vote_yes_if_both_yes() throws Exception {
        when(businessObject.vote(transcationId)).thenReturn(TransactionManager.Vote.YES);
        when(intTxManager.vote(transcationId)).thenReturn(TransactionManager.Vote.YES);
        TransactionManager.Vote vote = txMgr.vote(transcationId);
        assertEquals(TransactionManager.Vote.YES, vote);
    }

    @Test
    public void test_vote_no_if_extResource_fails() throws Exception {
        when(intTxManager.vote(transcationId)).thenReturn(TransactionManager.Vote.YES);
        when(businessObject.vote(transcationId)).thenReturn(TransactionManager.Vote.UNCERTIAN);
        TransactionManager.Vote vote = txMgr.vote(transcationId);
        assertEquals(TransactionManager.Vote.NO, vote);
    }

    @Test
    public void test_tx_primitive_engages_intTxMgr_and_extResource() {
        txMgr.commit(transcationId);
        verify(intTxManager, times(1)).commit(transcationId);
        verify(businessObject, times(1)).commit(transcationId);
    }
}