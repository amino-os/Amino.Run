package sapphire.kernel.common;

/** Created by Venugopal Reddy K on 23/7/18. */
public class DMConfigInfo {
    String clientPolicy;
    String serverPolicy;
    String groupPolicy;

    public DMConfigInfo(String client, String server, String group) {
        clientPolicy = client;
        serverPolicy = server;
        groupPolicy = group;
    }

    public String getClientPolicy() {
        return clientPolicy;
    }

    public String getServerPolicy() {
        return serverPolicy;
    }

    public String getGroupPolicy() {
        return groupPolicy;
    }
}
