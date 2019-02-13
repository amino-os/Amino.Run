package amino.run.policy.transaction;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import amino.run.common.AppObject;
import amino.run.common.ReflectionTestUtil;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

public class TwoPCCoordinatorServerPolicyTest {
    private TwoPCCoordinatorPolicy.ServerPolicy coordinatorServerPolicy =
            new TwoPCCoordinatorPolicy.ServerPolicy();
    private TwoPCCoordinator coordinator = mock(TwoPCCoordinator.class);
    private SandboxProvider sandboxProvider = mock(SandboxProvider.class);
    private AppObjectShimServerPolicy sandbox = mock(AppObjectShimServerPolicy.class);

    @Before
    public void Setup() throws Exception {
        when(sandboxProvider.getSandbox(eq(coordinatorServerPolicy), any(UUID.class)))
                .thenReturn(sandbox);
        ReflectionTestUtil.setField(coordinatorServerPolicy, "coordinator", coordinator);
        ReflectionTestUtil.setField(coordinatorServerPolicy, "sandboxProvider", sandboxProvider);
    }

    @Test
    public void test_commit_observes_2pc_protocols() throws Exception {
        when(coordinator.vote(any(UUID.class))).thenReturn(TransactionManager.Vote.YES);

        coordinatorServerPolicy.onRPC("foo", null);

        InOrder inOrder = inOrder(coordinator);
        inOrder.verify(coordinator).beginTransaction();
        inOrder.verify(coordinator).vote(any(UUID.class));
        inOrder.verify(coordinator).commit(any(UUID.class));
    }

    @Test
    public void test_abort_observes_2pc_protocols() throws Exception {
        when(coordinator.vote(any(UUID.class))).thenReturn(TransactionManager.Vote.NO);

        try {
            coordinatorServerPolicy.onRPC("foo", null);
        } catch (TransactionAbortException e) {
            // do nothing on expected exception
        }

        InOrder inOrder = inOrder(coordinator);
        inOrder.verify(coordinator).beginTransaction();
        inOrder.verify(coordinator).vote(any(UUID.class));
        inOrder.verify(coordinator).abort(any(UUID.class));
    }

    @Test
    public void test_works_on_sandbox_inside_tx() throws Exception {
        AppObject appObject = mock(AppObject.class);
        when(appObject.invoke("foo", null)).thenReturn("bar");
        coordinatorServerPolicy.$__initialize(appObject);

        try {
            this.coordinatorServerPolicy.onRPC("foo", null);
        } catch (Exception e) {
        }

        verifyZeroInteractions(appObject);
        verify(this.sandbox).onRPC("foo", null);
    }

    @Test
    public void test_noupdate_on_tx_aborted() throws Exception {
        when(coordinator.vote(any(UUID.class))).thenReturn(TransactionManager.Vote.NO);

        AppObject originalAppObject = new AppObject("bar");
        this.coordinatorServerPolicy.$__initialize(originalAppObject);

        try {
            coordinatorServerPolicy.onRPC("foo", null);
        } catch (TransactionAbortException e) {
            // as expected exception
            assertSame(originalAppObject, this.coordinatorServerPolicy.sapphire_getAppObject());
            return;
        }

        fail("should not reach here");
    }

    @Test
    public void test_content_changed_on_tx_commit() throws Exception {
        when(coordinator.vote(any(UUID.class))).thenReturn(TransactionManager.Vote.YES);

        AppObject originalAppObject = new AppObject("bar");
        this.coordinatorServerPolicy.$__initialize(originalAppObject);

        coordinatorServerPolicy.onRPC("foo", null);

        assertNotSame(originalAppObject, this.coordinatorServerPolicy.sapphire_getAppObject());
    }
}
