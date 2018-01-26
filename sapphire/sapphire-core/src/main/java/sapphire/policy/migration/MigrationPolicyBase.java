package sapphire.policy.migration;

import java.net.InetSocketAddress;
import java.util.ArrayList;

import sapphire.policy.DefaultSapphirePolicy;

/**
 * Created by mbssaiakhil on 1/22/18.
 *
 * Base class for migration policies.
 * Put common stuff in here that all migration policies can inherit/reuse.
 **/
public abstract class MigrationPolicyBase extends DefaultSapphirePolicy {
    public static abstract class ClientPolicy extends DefaultClientPolicy {}

    public static abstract class ServerPolicy extends DefaultServerPolicy {

        // getOtherKernelServers() returns all the kernel servers from a list other than current
        public ArrayList<InetSocketAddress> getOtherKernelServers(ArrayList<InetSocketAddress> allKernelServers, InetSocketAddress currKernelServer) {
            allKernelServers.remove(currKernelServer);
            return allKernelServers;
        }
    }

    public static abstract class GroupPolicy extends DefaultGroupPolicy {}
}
