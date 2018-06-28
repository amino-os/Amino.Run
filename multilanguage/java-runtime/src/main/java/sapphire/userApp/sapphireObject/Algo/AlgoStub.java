/**
 * Created by Jithu Thomas on 25/6/18.
 */

package sapphire.userApp.sapphireObject.Algo;

import algo_proto.AlgoProto;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;


public class AlgoStub {

    public byte[] Sort_Wrap(byte[] reqData) {
        System.out.println("Inside Sort_Wrap()");

        AlgoProto.SortRequest request;
        try {
            request = AlgoProto.SortRequest.parseFrom(reqData);
        } catch (InvalidProtocolBufferException e) {
            System.out.println("Sort_Wrap() paramater deserialization failed: " + e.getMessage());
            return null;
        }

        int arr[] = request.getArrList().stream().mapToInt(i->i).toArray();
        int res[] = Algo.Sort(arr);
        List<Integer> resp = Arrays.asList(ArrayUtils.toObject(res));

        AlgoProto.SortReply response;
        response = AlgoProto.SortReply
                .newBuilder()
                .addAllArr(resp).build();
        return response.toByteArray();
    }

    public byte[] Search_Wrap(byte[] reqData) {
        System.out.println("Inside Search_Wrap()");

        AlgoProto.SearchRequest request;
        try {
            request = AlgoProto.SearchRequest.parseFrom(reqData);
        } catch (InvalidProtocolBufferException e) {
            System.out.println("Search_Wrap() parameter deserialization failed: " + e.getMessage());
            return null;
        }

        AlgoProto.SearchReply response;
        response = AlgoProto.SearchReply
                .newBuilder()
                .setRet(Algo.Search((Integer[])request.getNumList().toArray(), request.getKey())).build();
        return response.toByteArray();
    }

    public byte[] Fibbonaci_Wrap(byte[] reqData) {
        System.out.println("Inside Fibbonaci_Wrap()");

        AlgoProto.FibbonaciRequest request;
        try {
            request = AlgoProto.FibbonaciRequest.parseFrom(reqData);
        } catch (InvalidProtocolBufferException e) {
            System.out.println("Fibbonaci_Wrap() parameter deserialization failed: " + e.getMessage());
            return null;
        }

        AlgoProto.FibbonaciReply response;
        response = AlgoProto.FibbonaciReply
                .newBuilder()
                .setRet(Algo.Fibbonaci(request.getNum())).build();
        return response.toByteArray();
    }
}
