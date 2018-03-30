package sapphire.kernel.common;
import java.io.Serializable;
import java.net.InetSocketAddress;


/**
 * Created by SrinivasChilveri on 3/30/18.
 * ServerInfo
 * Includes the host and region etc which can be used to register with the oms
 * @author SrinivasChilveri
 *
 */

public class ServerInfo implements Serializable {
    private InetSocketAddress host;
    private String region;


    public ServerInfo(InetSocketAddress addr, String reg) {
        this.host = addr;
        this.region = reg;
    }

    public InetSocketAddress getHost() {
        return host;
    }

    public String getRegion() {
        return region;
    }

}