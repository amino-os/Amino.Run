package sapphire.policy.explicitcaching;

import org.junit.Before;
import org.junit.Test;
import sapphire.common.AppObject;

import java.io.Serializable;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

public class ExplicitCachingPolicyTest {
    ExplicitCachingPolicy.ExplicitCachingClientPolicy client;
    ExplicitCachingPolicy.ExplicitCachingServerPolicy server;

    @Before
    public void setUp() throws Exception {
        this.client = new ExplicitCachingPolicy.ExplicitCachingClientPolicy();
        this.server = mock(ExplicitCachingPolicy.ExplicitCachingServerPolicy.class);
        this.client.setServer(this.server);
    }

    @Test
    public void firstRPCPulls() throws Exception {
        Serializable so = mock(Serializable.class, withSettings().extraInterfaces(ExplicitCacher.class));
        AppObject appObject = mock(AppObject.class);
        when(appObject.getObject()).thenReturn(so);
        when(this.server.getCopy()).thenReturn(appObject);

        assertNull(this.client.getCachedCopy());

        ArrayList<Object> params = new ArrayList<Object>();
        this.client.onRPC("foo", params);

        assertEquals(so, this.client.getCachedCopy().getObject());
        verify(this.server, times(1)).getCopy();
    }

    @Test
    public void regularRPC() throws Exception {
        ArrayList<Object> params = new ArrayList<Object>();
        Serializable so = mock(Serializable.class, withSettings().extraInterfaces(ExplicitCacher.class));
        AppObject localCopy = mock(AppObject.class);
        when(localCopy.getObject()).thenReturn(so);
        this.client.setCopy(localCopy);

        this.client.onRPC("foo", params);

        verify(localCopy, times(1)).invoke("foo", params);
        verifyZeroInteractions(this.server);
    }

    @Test
    public void pull() throws Exception{
        String methodPull = "public void sapphire.appexamples.minnietwitter.app.UserManager.pull()";

        Serializable latestSO = mock(Serializable.class, withSettings().extraInterfaces(ExplicitCacher.class));
        AppObject remoteCopy = mock(AppObject.class);
        when(remoteCopy.getObject()).thenReturn(latestSO);
        when(this.server.getCopy()).thenReturn(remoteCopy);

        AppObject staleLocalCopy = mock(AppObject.class);
        this.client.setCopy(staleLocalCopy);

        this.client.onRPC(methodPull, new ArrayList<Object>());

        verifyZeroInteractions(staleLocalCopy);
        AppObject localCopy = this.client.getCachedCopy();
        assertEquals(latestSO, localCopy.getObject());
    }

    @Test
    public void push() throws Exception{
        String methodPush = "public void sapphire.appexamples.minnietwitter.app.UserManager.push()";

        Serializable latestSO = mock(Serializable.class);
        AppObject localCopy = mock(AppObject.class);
        when(localCopy.getObject()).thenReturn(latestSO);
        this.client.setCopy(localCopy);

        this.client.onRPC(methodPush, new ArrayList<Object>());

        verifyZeroInteractions(latestSO);
        verify(this.server, times(1)).syncCopy(latestSO);
        assertEquals(latestSO, this.client.getCachedCopy().getObject());
    }
}