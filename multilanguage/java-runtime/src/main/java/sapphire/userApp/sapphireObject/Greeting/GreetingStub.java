/**
 * Created by Jithu Thomas on 21/6/18.
 */

package sapphire.userApp.sapphireObject.Greeting;

import com.google.protobuf.InvalidProtocolBufferException;
import greeting_proto.GreetingProto;

public class GreetingStub {

    public byte[] HelloWorld_Wrap(byte[] reqData) {
        System.out.println("Inside HelloWorld_Wrap()");

        GreetingProto.HelloWorldRequest request;
        try {
            request = GreetingProto.HelloWorldRequest.parseFrom(reqData);
        } catch (InvalidProtocolBufferException e){
            System.out.println("HelloWorld_Wrap() parameter deserialization failed: " + e.getMessage());
            return null;
        }

        GreetingProto.HelloWorldReply response;
        response = GreetingProto.HelloWorldReply
                .newBuilder()
                .setRetName(Greeting.HelloWorld(request.getName(), request.getNum())).build();

        return response.toByteArray();
    }

    public byte[] Fibbonaci_Wrap(byte[] reqData) {
        System.out.println("Inside Fibbonaci_Wrap()");

        GreetingProto.FibbonaciRequest request;
        try {
            request = GreetingProto.FibbonaciRequest.parseFrom(reqData);
        } catch (InvalidProtocolBufferException e) {
            System.out.println("Fibbonaci_Wrap() parameter deserialization failed: " + e.getMessage());
            return null;
        }

        GreetingProto.FibbonaciReply response;
        response = GreetingProto.FibbonaciReply
                .newBuilder()
                .setRet(Greeting.Fibbonaci(request.getNum())).build();
        return response.toByteArray();
    }
}
