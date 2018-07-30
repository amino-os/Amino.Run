package sapphire.appexamples.minnietwitter.glue;

/**
 * Created by s00432254 on 1/3/2018.
 */

public class Configuration {

    /**
     * TODO (Sungwook, 1/3/2018): Remove hardcoded IP addresses.
     * The first address: OMS host grpc server
     * The second address: kernel client host grpc server
     * The third address: kernel client host rmi
     */
    public static String [] hostAddress = { "192.168.42.7", "20005", "192.168.42.7", "20003", "192.168.42.7", "10003" };
}
