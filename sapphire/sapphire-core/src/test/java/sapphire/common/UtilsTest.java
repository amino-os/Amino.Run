package sapphire.common;

import org.junit.Assert;
import org.junit.Test;

import java.io.Serializable;
import java.util.ArrayList;

import sapphire.kernel.common.KernelOID;
import sapphire.kernel.common.KernelRPC;
import sapphire.policy.scalability.LogEntry;
import sapphire.runtime.MethodInvocationRequest;

import static org.junit.Assert.*;

/**
 * Created by quinton on 1/23/18.
 */
public class UtilsTest {

    static class TestOuter implements Serializable {
        int o;
        TestInner innerObj;
        TestOuter() {
            innerObj=new TestInner();
        }
    }
    static class TestInner implements Serializable {
        int i;
    }
    @Test
    public void clonesAreDisjoint() throws Exception {
        UtilsTest.TestOuter testObj = new TestOuter();
        testObj.o = 1;
        testObj.innerObj.i=1;
        TestOuter cloneObj = (TestOuter)Utils.ObjectCloner.deepCopy(testObj);
        cloneObj.o = 2;
        cloneObj.innerObj.i = 2;
        assertNotEquals(testObj.o, cloneObj.o);
        assertNotEquals(testObj.innerObj.i, cloneObj.innerObj.i);
    }

    @Test
    public void testLogEntrySerialization() throws Exception {
        ArrayList<Object> params = new ArrayList<Object>();
        params.add("hello");
        MethodInvocationRequest request = MethodInvocationRequest.newBuilder()
                .clientId("clientId")
                .requestId(0L)
                .methodType(MethodInvocationRequest.MethodType.WRITE)
                .methodName("invoke")
                .params(params)
                .build();
        LogEntry expected = LogEntry.newBuilder()
                .term(0)
                .index(0)
                .request(request)
                .build();

        byte[] bytes = Utils.toBytes(expected);
        LogEntry actual = (LogEntry)Utils.toObject(bytes);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testMethodInvocationRequestSerialization() throws Exception {
        ArrayList<Object> params = new ArrayList<Object>();
        params.add("hello");

        MethodInvocationRequest request = MethodInvocationRequest.newBuilder()
                .clientId("clientId")
                .requestId(0L)
                .methodType(MethodInvocationRequest.MethodType.WRITE)
                .methodName("invoke")
                .params(params)
                .build();

        byte[] bytes = Utils.toBytes(request);
        MethodInvocationRequest actual = (MethodInvocationRequest)Utils.toObject(bytes);
        Assert.assertEquals(request, actual);
    }

    @Test
    public void testKernelRPCSerialization() throws Exception {
        KernelOID oid = new KernelOID(10);
        ArrayList<Object> params = new ArrayList<Object>();
        params.add("hello");

        KernelRPC expected = new KernelRPC(oid, "method", params);

        byte[] bytes = Utils.toBytes(expected);
        KernelRPC actual = (KernelRPC)Utils.toObject(bytes);
        Assert.assertEquals(expected, actual);
    }
}