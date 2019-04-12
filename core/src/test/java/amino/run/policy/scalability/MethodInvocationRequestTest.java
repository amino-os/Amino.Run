package amino.run.policy.scalability;

import amino.run.policy.scalability.masterslave.MethodInvocationRequest;
import java.util.ArrayList;
import java.util.Date;
import org.junit.Assert;
import org.junit.Test;

public class MethodInvocationRequestTest {
    @Test
    public void test() throws Exception {
        String clientId = "clientId";
        long requestId = 100;
        String methodName = "method";
        MethodInvocationRequest.MethodType methodType =
                MethodInvocationRequest.MethodType.IMMUTABLE;
        ArrayList<Object> params = new ArrayList<Object>();
        params.add(new Date());
        MethodInvocationRequest request =
                new MethodInvocationRequest(clientId, requestId, methodName, params, methodType);

        Assert.assertEquals(clientId, request.getClientId());
        Assert.assertEquals(requestId, request.getRequestId());
        Assert.assertEquals(methodName, request.getMethodName());
        Assert.assertEquals(methodType, request.getMethodType());
        Assert.assertEquals(params, request.getParams());
    }
}
