package amino.run.policy.serializability;

import static org.junit.Assert.*;

import amino.run.common.AppObject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import org.junit.Before;

/** @author terryz */
public class SerializableRPCPolicyTest {
    private SerializableRPCPolicy.ClientPolicy client;
    private SerializableRPCPolicy.ServerPolicy server;

    @Before
    public void setUp() throws Exception {
        client = new SerializableRPCPolicy.ClientPolicy();
        server = new SerializableRPCPolicy.ServerPolicy();
        client.setServer(server);
        Object object = new Counter_Stub();
        AppObject appObject = new AppObject(object);
        server.$__initialize(appObject);
    }

    @org.junit.Test
    public void testSerializableRPCsOnServer() throws Exception {
        final String methodName =
                "public int amino.run.policy.serializability.SerializableRPCPolicyTest$Counter.addOne() throws java.lang.Exception";
        final int len = 5;
        List<FutureTask<Object>> taskList = new ArrayList<FutureTask<Object>>();

        for (int i = 0; i < len; i++) {
            FutureTask<Object> task =
                    new FutureTask<Object>(
                            new Callable<Object>() {
                                @Override
                                public Object call() throws Exception {
                                    // invoke onRPC method on server
                                    return server.onRPC(methodName, new ArrayList<Object>());
                                }
                            });
            taskList.add(task);
        }

        verifyTasksInParallel(taskList);
    }

    @org.junit.Test
    public void testSerializableRPCsOnClient() throws Exception {
        final String methodName =
                "public int amino.run.policy.serializability.SerializableRPCPolicyTest$Counter.addOne() throws java.lang.Exception";
        final int len = 5;
        List<FutureTask<Object>> taskList = new ArrayList<FutureTask<Object>>();

        for (int i = 0; i < len; i++) {
            FutureTask<Object> task =
                    new FutureTask<Object>(
                            new Callable<Object>() {
                                @Override
                                public Object call() throws Exception {
                                    // invoke onRPC method on client
                                    return client.onRPC(methodName, new ArrayList<Object>());
                                }
                            });
            taskList.add(task);
        }

        verifyTasksInParallel(taskList);
    }

    private void verifyTasksInParallel(List<FutureTask<Object>> taskList) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(taskList.size());

        // Run tasks in parallel
        // Each task will increment counter by one
        for (FutureTask<Object> t : taskList) {
            executor.execute(t);
        }

        int max = 0;
        for (int i = 0; i < taskList.size(); i++) {
            Object ret = taskList.get(i).get();
            max = Math.max(max, (Integer) ret);
        }

        // Since 1) each task increment counter by one
        // and 2) task are executed in sequence, the final
        // value of the counter should equal to the number
        // of tasks.
        assertEquals(taskList.size(), max);
    }

    public static class Counter implements Serializable {
        private int value = 0;

        public int addOne() throws Exception {
            value++;
            return value;
        }
    }

    public static class Counter_Stub extends Counter {}
}
