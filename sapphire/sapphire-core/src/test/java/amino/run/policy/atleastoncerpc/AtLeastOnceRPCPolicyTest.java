package amino.run.policy.atleastoncerpc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import amino.run.common.AppExceptionWrapper;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

public class AtLeastOnceRPCPolicyTest {
    AtLeastOnceRPCPolicy.AtLeastOnceRPCClientPolicy clientPolicy;
    AtLeastOnceRPCPolicy.AtLeastOnceRPCServerPolicy serverPolicy;

    @org.junit.Before
    public void setUp() throws Exception {
        this.clientPolicy = new AtLeastOnceRPCPolicy.AtLeastOnceRPCClientPolicy();
        this.serverPolicy = mock(AtLeastOnceRPCPolicy.AtLeastOnceRPCServerPolicy.class);
        this.clientPolicy.setServer(this.serverPolicy);
    }

    @org.junit.Test
    public void happyCase() throws Exception {
        // normal case: call goes well
        when(this.serverPolicy.onRPC("foo", new ArrayList<Object>())).thenReturn("OK");

        Object expected = null;
        try {
            expected = this.clientPolicy.onRPC("foo", new ArrayList<Object>());
        } catch (Exception e) {
            fail(String.format("unexpected exception was thrown: %s", e.toString()));
        }

        assertEquals("OK", expected);
    }

    @org.junit.Test
    public void retriesSucceed() throws Exception {
        // transient failure case: first, second call gets exception; third call goes through
        when(this.serverPolicy.onRPC("foo", new ArrayList<Object>()))
                .thenThrow(new Exception("transient; should've been resolved after 2 retries"))
                .thenThrow(new Exception("transient; should've been resolved after 1 retry"))
                .thenReturn("OK");

        Object expected = null;
        try {
            expected = this.clientPolicy.onRPC("foo", new ArrayList<Object>());
        } catch (Exception e) {
            fail(String.format("unexpected exception was thrown: %s", e.toString()));
        }

        assertEquals("OK", expected);
    }

    @org.junit.Test(timeout = 500, expected = TimeoutException.class)
    public void givesUpAfterTimeout() throws Exception {
        // persistent failure case
        when(this.serverPolicy.onRPC("foo", new ArrayList<Object>()))
                .thenThrow(new Exception("persistent non-app error"));

        // to shorten RPC timeout allowance for unit test
        this.clientPolicy.setTimeout(100);
        this.clientPolicy.onRPC("foo", new ArrayList<Object>());
        fail("TimeoutException should have been thrown; should never get to this point");
    }

    @org.junit.Test(timeout = 500, expected = AppExceptionWrapper.class)
    public void doesNotRetryApplicationExceptions() throws Exception {
        // persistent failure case
        when(this.serverPolicy.onRPC("foo", new ArrayList<Object>()))
                .thenThrow(new AppExceptionWrapper(new Exception("persistent app error")));

        // to shorten RPC timeout allowance for unit test
        this.clientPolicy.setTimeout(100);
        this.clientPolicy.onRPC("foo", new ArrayList<Object>());
        fail("AppExceptionWrapper should have been thrown; should never get to this point");
    }
}
