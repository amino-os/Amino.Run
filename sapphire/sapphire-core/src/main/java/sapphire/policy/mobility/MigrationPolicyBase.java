package sapphire.policy.mobility;

import java.net.InetSocketAddress;
import java.util.ArrayList;

import sapphire.policy.DefaultSapphirePolicy;

/**
 * Created by mbssaiakhil on 1/22/18.
 *
 * Base class for mobility policies.
 * Put common stuff in here that all mobility policies can inherit/reuse.
 **/
public abstract class MigrationPolicyBase extends DefaultSapphirePolicy {
    public static abstract class ClientPolicy extends DefaultClientPolicy {}

    public static abstract class ServerPolicy extends DefaultServerPolicy {

        // getAllOtherKernelServers() returns all the kernel servers from a list other than current
        public ArrayList<InetSocketAddress> getAllOtherKernelServers(ArrayList<InetSocketAddress> allKernelServers, InetSocketAddress currKernelServer) {
            allKernelServers.remove(currKernelServer);
            return allKernelServers;
        }
    }

    public static abstract class GroupPolicy extends DefaultGroupPolicy {}
}
