package sapphire.policy.atleastoncerpc;

import sapphire.policy.SapphirePolicy;

import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AtLeastOnceRPCPolicyTest {
    SapphirePolicy.SapphireClientPolicy clientPolicy;
    SapphirePolicy.SapphireServerPolicy serverPolicy;
    SapphirePolicy.SapphireGroupPolicy groupPolicy;

    @org.junit.Before
    public void setUp() throws Exception {
        this.clientPolicy = new AtLeastOnceRPCPolicy.AtLeastOnceRPCClientPolicy();
        this.serverPolicy = mock(AtLeastOnceRPCPolicy.AtLeastOnceRPCServerPolicy.class);
        this.clientPolicy.setServer(this.serverPolicy);
    }

    @org.junit.Test
    public void atleaseOnceRPC() throws Exception {
        // normal case: call goes well
       when(this.serverPolicy.onRPC("foo", new ArrayList<Object>())).thenReturn("OK");

        Object expected = null;
        try {
            expected = this.clientPolicy.onRPC("foo", new ArrayList<Object>());
        }catch (Exception e){
            fail(String.format("unexpected exception was thrown: %s", e.toString()));
        }

        assertEquals("OK", expected);
    }

    @org.junit.Test
    public void atleaseOnceRPCRetries() throws Exception {
        // transient failure case: first call gets exception; second call goes through
        when(this.serverPolicy.onRPC("foo", new ArrayList<Object>()))
                .thenThrow(new Exception("transient; should've been resolved after 2 retries"))
                .thenThrow(new Exception("transient; should've been resolved after 1 retry"))
                .thenReturn("OK");

        Object expected = null;
        try {
            expected = this.clientPolicy.onRPC("foo", new ArrayList<Object>());
        }catch (Exception e){
            fail(String.format("unexpected exception was thrown: %s", e.toString()));
        }

        assertEquals("OK", expected);
    }

    @org.junit.Test(timeout = 500, expected = TimeoutException.class)
    public void atleaseOnceRPCGivesupAfterTimeout() throws Exception {
        // persistent failure case
        when(this.serverPolicy.onRPC("foo", new ArrayList<Object>())).thenThrow(new Exception("persistent"));

        // to shorten RPC timeout allowance for unit test
        ((AtLeastOnceRPCPolicy.AtLeastOnceRPCClientPolicy)this.clientPolicy).setTimeout(100);

        Object result = null;
        result = this.clientPolicy.onRPC("foo", new ArrayList<Object>());
        fail("exception should have been thrown; should never come at this point");
    }
}