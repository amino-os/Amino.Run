package sapphire.kernel.common;

import java.io.Serializable;
import java.net.InetSocketAddress;

/**
 * Created by SrinivasChilveri on 3/30/18. ServerInfo Includes the host and region etc which can be
 * used to register with the oms
 *
 * @author SrinivasChilveri
 */
public class ServerInfo implements Serializable {
    public static int ROLE_KERNEL_SERVER = 0;
    public static int ROLE_KERNEL_CLIENT = 1;
    private InetSocketAddress host;
    private String region;
    private int role;

    public ServerInfo(InetSocketAddress addr, String reg, int role) {
        this.host = addr;
        this.region = reg;
        this.role = role;
    }

    public InetSocketAddress getHost() {
        return host;
    }

    public String getRegion() {
        return region;
    }

    public int getRole() {
        return role;
    }
}
