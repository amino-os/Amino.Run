package sapphire.policy.cache;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

import sapphire.common.AppObject;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Created by Vishwajeet on 13/2/18.
 */

public class CacheLeasePolicyTest {
    CacheLeasePolicy.CacheLeaseClientPolicy client;
    CacheLeasePolicy.CacheLeaseClientPolicy clientOne;
    CacheLeasePolicy.CacheLeaseServerPolicy server;

    private CacheLeasePolicyTest so;
    private AppObject appObject;
    private UUID lease;
    public long time = CacheLeasePolicy.DEFAULT_LEASE_PERIOD;

    @Before
    public void setUp() throws Exception{
        this.client = spy(CacheLeasePolicy.CacheLeaseClientPolicy.class);
        so = new CacheLeasePolicyTestStub();
        appObject = mock(AppObject.class);
        this.server = spy(CacheLeasePolicy.CacheLeaseServerPolicy.class);
        this.server.$__initialize(appObject);
        this.client.setServer(this.server);

        this.clientOne = spy(CacheLeasePolicy.CacheLeaseClientPolicy.class);
        this.clientOne.setServer(this.server);
    }

    /**
     * Check if the client has a valid lease,
     * if not, then under onRPC a new lease will be assigned to the client
     * and method will be invoked on the cachedObject.
     */
    @Test
    public void testGetLease() throws Exception{
        assertFalse(this.client.leaseStillValid());

        String methodName = "public Object sapphire.common.ObjectHandler.invoke() throws java.lang.Exception";
        ArrayList<Object> params = new ArrayList<Object>();

        // Get new lease and invoke cached object
        this.client.onRPC(methodName,params);

        // Verify if the client has a valid lease
        assertTrue(this.client.leaseStillValid());

        // Release current lease for client
        this.client.releaseCurrentLease();
        // Verify that the lease got released
        assertFalse(this.client.leaseStillValid());
    }

    // Verify that the code throws a specific exception
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * When the client has a valid lease,
     * a new request for getNewLease by another client(clientOne in this case) should fail
     * unless the client lease expires or client executes releaseCurrentLease.
     */
    @Test
    public void testFailedLease() throws Exception{
        // Assign lease to client
        this.client.getNewLease(time);

        // Cannot assign lease to clientOne because currently client holds it
        thrown.expectMessage(equalTo("Could not get lease."));
        this.clientOne.getNewLease(time);
    }

    /**
     * If a client needs lease for extended period, renew his lease.
     * If the client lease is still valid, renew using the same lease ID.
     * In case current lease expires, throw error if someone else holds the lease.
     * Else assign a new lease if someone else's lease has expired.
     * When trying to release an already expired lease, throw error.
     */
    @Test
    public void testRenewLeaseAndReleaseExpired() throws Exception{
        long time = 100;
        this.client.getNewLease(time);
        lease = this.client.lease;
        verify(this.server).getLease(time);

        // Renew lease for client using the same lease ID
        this.client.getNewLease(time);
        verify(this.server).getLease(lease, time);

        // Expire lease for client
        Thread.sleep(time + CacheLeasePolicy.LEASE_BUFFER); // Accounting for LEASE-BUFFER

        this.clientOne.getNewLease(time);

        // Someone else has a valid lease, so client cannot have it
        thrown.expectMessage(equalTo("Could not get lease."));
        this.client.getNewLease(time);

        // Expire lease for clientOne
        Thread.sleep(time + CacheLeasePolicy.LEASE_BUFFER);

        // Someone else's lease expired, so client can now have a new lease
        this.client.getNewLease(time);

        // Verifying lease expired for clientOne
        assertFalse(this.clientOne.leaseStillValid());

        // Throws error because trying to release expired lease
        thrown.expect(LeaseExpiredException.class);
        this.clientOne.releaseCurrentLease();
    }
}

// Stub because AppObject expects a stub/subclass of the original class.
class CacheLeasePolicyTestStub extends CacheLeasePolicyTest implements Serializable {}
