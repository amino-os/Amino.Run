package sapphire.policy.primitive;

import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import sapphire.common.AppObject;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author terryz
 */
public class ImmutablePolicyTest {
    private ImmutablePolicy.ClientPolicy client;
    private ImmutablePolicy.ServerPolicy server;
    private AppObject appObject;

    @Before
    public void setUp() throws Exception {
        client = spy(ImmutablePolicy.ClientPolicy.class);
        server = spy(ImmutablePolicy.ServerPolicy.class);
        client.setServer(this.server);
        Object object = new ImmutablePolicyTest.SO_Stub();
        appObject = new AppObject(object);
        server.$__initialize(appObject);
    }

    @Test
    public void execOnImmutableObjectRepeatedly() throws Exception {
        final String methodName = "public int sapphire.policy.primitive.ImmutablePolicyTest$SO.getValue() throws java.lang.Exception";
        final int len = 5;
        final List<FutureTask<Object>> taskList = new ArrayList<FutureTask<Object>>();

        for (int i=0; i<len; i++) {
            FutureTask<Object> task = new FutureTask<Object>(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    return client.onRPC(methodName, new ArrayList<Object>());
                }
            });
            taskList.add(task);
        }

        // Run tasks in parallel
        ExecutorService executor = Executors.newFixedThreadPool(taskList.size());
        for (FutureTask<Object> t: taskList) {
            executor.execute(t);
        }

        for (int i=0; i<taskList.size(); i++) {
            Object actual = taskList.get(i).get();
            // verify return result is correct
            assertEquals(SO.DEFAULT, actual);
        }

        // verify that onRPC is not invoked on server
        verify(server, never()).onRPC(methodName, new ArrayList<Object>());

        // verify that server.getObject was only invoked once
        verify(server, times(1)).sapphire_getAppObject();
    }

    public static class SO implements Serializable {
        public static final int DEFAULT = 100;

        public int getValue() throws Exception {
            return DEFAULT;
        }
    }

    public static class SO_Stub extends ImmutablePolicyTest.SO {
    }

}