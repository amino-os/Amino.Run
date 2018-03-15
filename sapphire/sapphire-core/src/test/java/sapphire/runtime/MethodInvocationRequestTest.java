package sapphire.runtime;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;

/**
 * @author terryz
 */
public class MethodInvocationRequestTest {
    @Test
    public void test() throws Exception {
        String clientId = "clientId";
        long requestId = 100;
        String methodName = "method";
        MethodInvocationRequest.MethodType methodType = MethodInvocationRequest.MethodType.IMMUTABLE;
        ArrayList<Object> params = new ArrayList<Object>();
        params.add(new Date());
        MethodInvocationRequest request = MethodInvocationRequest
                .newBuilder()
                .clientId(clientId)
                .requestId(requestId)
                .methodName(methodName)
                .methodType(methodType)
                .params(params)
                .build();

        Assert.assertEquals(clientId, request.getClientId());
        Assert.assertEquals(requestId, request.getRequestId());
        Assert.assertEquals(methodName, request.getMethodName());
        Assert.assertEquals(methodType, request.getMethodType());
        Assert.assertEquals(params, request.getParams());
    }
}