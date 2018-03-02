package sapphire.policy.scalability;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;

import sapphire.kernel.common.KernelOID;
import sapphire.kernel.common.KernelRPC;
import sapphire.runtime.MethodInvocationRequest;

/**
 * @author terryz
 */
public class UtilTest {

    @Test
    public void testLogEntrySerialization() throws Exception {
        ArrayList<Object> params = new ArrayList<Object>();
        params.add("hello");
        MethodInvocationRequest request = MethodInvocationRequest.newBuilder()
                .methodType(MethodInvocationRequest.MethodType.WRITE)
                .methodName("invoke")
                .params(params)
                .build();
        LogEntry expected = LogEntry.newBuilder()
                .term(0)
                .index(0)
                .request(request)
                .build();

        byte[] bytes = Util.toBytes(expected);
        LogEntry actual = (LogEntry)Util.toObject(bytes);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testMethodInvocationRequestSerialization() throws Exception {
        ArrayList<Object> params = new ArrayList<Object>();
        params.add("hello");

        MethodInvocationRequest request = MethodInvocationRequest.newBuilder()
                .methodType(MethodInvocationRequest.MethodType.WRITE)
                .methodName("invoke")
                .params(params)
                .build();

        byte[] bytes = Util.toBytes(request);
        MethodInvocationRequest actual = (MethodInvocationRequest)Util.toObject(bytes);
        Assert.assertEquals(request, actual);
    }

    @Test
    public void testKernelRPCSerialization() throws Exception {
        KernelOID oid = new KernelOID(10);
        ArrayList<Object> params = new ArrayList<Object>();
        params.add("hello");

        KernelRPC expected = new KernelRPC(oid, "method", params);

        byte[] bytes = Util.toBytes(expected);
        KernelRPC actual = (KernelRPC)Util.toObject(bytes);
        Assert.assertEquals(expected, actual);
    }
}