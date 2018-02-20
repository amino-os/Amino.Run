package sapphire.policy.scalability;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Date;

import sapphire.runtime.MethodInvocationRequest;

/**
 * @author terryz
 */
public class FileLoggerTest {

    @Test
    public void testObjectSize() throws Exception {
        ArrayList<Object> params = new ArrayList<Object>();
        params.add(new Date(System.currentTimeMillis()));
        params.add(new Date(System.currentTimeMillis()));
        params.add(new Date(System.currentTimeMillis()));
        params.add(new Date(System.currentTimeMillis()));
        params.add(new Date(System.currentTimeMillis()));

        MethodInvocationRequest request = MethodInvocationRequest.newBuilder().methodName("method").params(params).methodType(MethodInvocationRequest.MethodType.READ).build();
        Object o = LogEntry.newBuilder().index(0).request(request).term(0).build();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(os);
        oos.writeObject(o);
        oos.close();
        byte[] content = os.toByteArray();
        System.out.println("size: " + content.length);

        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(content));
        Object o2 = ois.readObject();
        System.out.println(o2);
    }
}