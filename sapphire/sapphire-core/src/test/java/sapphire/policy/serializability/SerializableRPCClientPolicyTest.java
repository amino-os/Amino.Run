package sapphire.policy.serializability;

import org.junit.Before;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import sapphire.common.AppObject;

import static org.junit.Assert.*;

public class SerializableRPCClientPolicyTest {
    private SerializableRPCPolicy.SerializableRPCClientPolicy client;
    private SerializableRPCPolicy.SerializableRPCServerPolicy server;

    @Before
    public void setUp() throws Exception {
        client = new SerializableRPCPolicy.SerializableRPCClientPolicy();
        server = new SerializableRPCPolicy.SerializableRPCServerPolicy();
        client.setServer(server);
        Object object = new Counter_Stub();
        AppObject appObject = new AppObject(object);
        server.$__initialize(appObject);
    }

    @org.junit.Test
    public void testSerializableRPCsOnServer() throws Exception {
        final String methodName = "public int sapphire.policy.serializability.SerializableRPCClientPolicyTest$Counter.addOne() throws java.lang.Exception";
        final int len = 5;
        List<FutureTask<Object>> taskList = new ArrayList<FutureTask<Object>>();

        for (int i=0; i< len; i++) {
            FutureTask<Object> task = new FutureTask<Object>(new Callable<Object>() {
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
        final String methodName = "public int sapphire.policy.serializability.SerializableRPCClientPolicyTest$Counter.addOne() throws java.lang.Exception";
        final int len = 5;
        List<FutureTask<Object>> taskList = new ArrayList<FutureTask<Object>>();

        for (int i=0; i< len; i++) {
            FutureTask<Object> task = new FutureTask<Object>(new Callable<Object>() {
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
        for (FutureTask<Object> t: taskList) {
            executor.execute(t);
        }

        int max = 0;
        for (int i=0; i<taskList.size(); i++) {
            Object ret = taskList.get(i).get();
            max = Math.max(max, (Integer)ret);
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
            Thread.sleep(500L);
            value++;
            return value;
        }
    }

    public static class Counter_Stub extends Counter {
    }
}