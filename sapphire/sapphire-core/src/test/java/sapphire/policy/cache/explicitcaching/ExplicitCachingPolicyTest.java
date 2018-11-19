package sapphire.policy.cache.explicitcaching;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

import java.io.Serializable;
import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;
import sapphire.common.AppObject;

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
    public void regularRPC() throws Exception {
        Serializable so =
                mock(Serializable.class, withSettings().extraInterfaces(ExplicitCacher.class));
        AppObject appObject = mock(AppObject.class);
        when(appObject.getObject()).thenReturn(so);
        when(this.server.getCopy()).thenReturn(appObject);

        assertNull(this.client.getCachedCopy());

        /* Invoke a method without explicit pull to cache the app object */
        ArrayList<Object> params = new ArrayList<Object>();
        this.client.onRPC("foo", params);

        /* Verify that app object is not cached. And method is invoked on the remote server */
        assertNull(this.client.getCachedCopy());
        verify(this.server, never()).getCopy();
        verify(this.server, times(1)).onRPC("foo", params);
    }

    @Test
    public void pullAndDoRPC() throws Exception {
        String methodPull = "public void sapphire.appexamples.minnietwitter.app.UserManager.pull()";
        ArrayList<Object> params = new ArrayList<Object>();
        Serializable so =
                mock(Serializable.class, withSettings().extraInterfaces(ExplicitCacher.class));
        AppObject appObject = mock(AppObject.class);
        when(appObject.getObject()).thenReturn(so);
        when(this.server.getCopy()).thenReturn(appObject);

        assertNull(this.client.getCachedCopy());

        /* Explicitly pull the cache of app object */
        this.client.onRPC(methodPull, new ArrayList<Object>());

        /* Invoke a method */
        this.client.onRPC("foo", params);

        /* Verify whether invocation happened on locally cached object(not on the server) */
        verify(appObject, times(1)).invoke("foo", params);
        verify(this.server, never()).onRPC("foo", params);
    }

    @Test
    public void pullMultipleTimes() throws Exception {
        String methodPull = "public void sapphire.appexamples.minnietwitter.app.UserManager.pull()";

        Serializable staleSO =
                mock(Serializable.class, withSettings().extraInterfaces(ExplicitCacher.class));
        Serializable latestSO =
                mock(Serializable.class, withSettings().extraInterfaces(ExplicitCacher.class));

        AppObject staleCopy = mock(AppObject.class);
        when(staleCopy.getObject()).thenReturn(staleSO);
        when(this.server.getCopy()).thenReturn(staleCopy);

        /* Explicitly pull the cache of app object. It should get staleCopy reference */
        this.client.onRPC(methodPull, new ArrayList<Object>());

        AppObject remoteCopy = mock(AppObject.class);
        when(remoteCopy.getObject()).thenReturn(latestSO);
        when(this.server.getCopy()).thenReturn(remoteCopy);

        /* Pull again when cached object is already available */
        this.client.onRPC(methodPull, new ArrayList<Object>());

        /* Verify whether stale copy is discarded and new cache object is pulled */
        verifyZeroInteractions(staleSO);
        AppObject localCopy = this.client.getCachedCopy();
        assertNotEquals(staleCopy, localCopy);
        assertEquals(latestSO, localCopy.getObject());
    }

    @Test
    public void push() throws Exception {
        String methodPull = "public void sapphire.appexamples.minnietwitter.app.UserManager.pull()";
        String methodPush = "public void sapphire.appexamples.minnietwitter.app.UserManager.push()";

        Serializable latestSO =
                mock(Serializable.class, withSettings().extraInterfaces(ExplicitCacher.class));
        AppObject appObject = mock(AppObject.class);
        when(appObject.getObject()).thenReturn(latestSO);
        when(this.server.getCopy()).thenReturn(appObject);

        /* Explicitly pull the cache of app object */
        this.client.onRPC(methodPull, new ArrayList<Object>());

        assertEquals(latestSO, this.client.getCachedCopy().getObject());

        /* Explicit push of cached object */
        this.client.onRPC(methodPush, new ArrayList<Object>());

        /* Verify whether object is sync'd to server */
        verifyZeroInteractions(latestSO);
        verify(this.server, times(1)).syncCopy(latestSO);
    }
}
