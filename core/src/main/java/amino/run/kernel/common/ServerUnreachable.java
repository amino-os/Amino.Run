package amino.run.kernel.common;

import amino.run.common.Notification;

/** ServerUnreachable notification to notify kernel server health to group policy(on OMS) */
public class ServerUnreachable implements Notification {
    public ServerInfo serverInfo;

    public ServerUnreachable(ServerInfo serverInfo) {
        this.serverInfo = serverInfo;
    }
}
