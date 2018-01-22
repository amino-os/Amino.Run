package sapphire.policy.cache;

import org.junit.Before;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import sapphire.common.AppObject;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class WriteThroughCachePolicyTest {
    private WriteThroughCachePolicy.WriteThroughCacheClientPolicy client;
    private WriteThroughCachePolicy.WriteThroughCacheServerPolicy server;
    private Object object;
    private AppObject appObject;

    @Before
    public void setUp() throws Exception {
        client = spy(WriteThroughCachePolicy.WriteThroughCacheClientPolicy.class);
        server = spy(WriteThroughCachePolicy.WriteThroughCacheServerPolicy.class);
        client.setServer(this.server);
        object = new ArrayList<Object>(Arrays.asList("Hello"));
        appObject = new AppObject(object);
        server.$__initialize(appObject);
    }

    @org.junit.Test
    public void readOnCacheRepeatedly() throws Exception {
        String methodName = "public java.lang.String java.util.AbstractCollection.toString()";

        when(this.client.isMethodMutable(methodName, new ArrayList<Object>())).thenReturn(false);

        for (int i = 0; i < 5; i++) {
            Object actual = this.client.onRPC(methodName, new ArrayList<Object>());
            assertEquals(object.toString(), actual);
        }

        // verify that read operations only called on cached object
        verify(server, never()).onRPC(methodName, new ArrayList<Object>());

        // verify that server.getObject was only invoked once
        verify(server, times(1)).getObject();
    }

    @org.junit.Test
    public void writeThroughCache() throws Exception {
        String methodName = "public boolean java.util.AbstractList.add(E)";
        ArrayList<Object> params = new ArrayList<Object>(Arrays.<Object>asList("World"));
        List<String> expectedResult = Arrays.asList("Hello", "World");

        when(this.client.isMethodMutable(methodName, params)).thenReturn(true);

        Object actual = this.client.onRPC(methodName, params);

        // verify that the return value of the method invocation is correct
        assertEquals(true, actual);

        // verify that server side object has been modified
        assertEquals(expectedResult.toString(), server.sapphire_getAppObject().getObject().toString());

        // read from cache one more time to verify that cached object is correct
        String m = "public java.lang.String java.util.AbstractCollection.toString()";
        when(this.client.isMethodMutable(m, new ArrayList<Object>())).thenReturn(false);
        actual = this.client.onRPC(m, new ArrayList<Object>());
        assertEquals(expectedResult.toString(), actual.toString());
    }
}