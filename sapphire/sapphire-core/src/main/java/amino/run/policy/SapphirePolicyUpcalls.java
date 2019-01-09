package amino.run.policy;

import amino.run.app.SapphireObjectSpec;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface SapphirePolicyUpcalls {

    /**
     * Interface for sapphire policy configuration.
     *
     * <p>Each sapphire policy can optionally define a Config class to allow programmers to pass
     * configurations to the sapphire policy. All Config classes should implement this interface.
     */
    interface SapphirePolicyConfig extends Serializable {}

    /** Interface for client policy */
    interface SapphireClientPolicyUpcalls extends Serializable {
        /**
         * Initialize client policy.
         *
         * @param group the group policy
         * @param spec sapphire object spec
         */
        void onCreate(SapphirePolicy.SapphireGroupPolicy group, SapphireObjectSpec spec);

        void setServer(SapphirePolicy.SapphireServerPolicy server);

        SapphirePolicy.SapphireServerPolicy getServer();

        SapphirePolicy.SapphireGroupPolicy getGroup();

        Object onRPC(String method, ArrayList<Object> params) throws Exception;
    }

    interface SapphireServerPolicyUpcalls extends Serializable {

        /**
         * Initialize server policy.
         *
         * @param group the group policy that manages this server policy
         * @param spec sapphire object spec
         */
        void onCreate(SapphirePolicy.SapphireGroupPolicy group, SapphireObjectSpec spec);

        void onDestroy();

        SapphirePolicy.SapphireGroupPolicy getGroup();

        Object onRPC(String method, ArrayList<Object> params) throws Exception;

        void onMembershipChange();
    }

    interface SapphireGroupPolicyUpcalls extends Serializable {
        /**
         * Initialize group policy.
         *
         * @param region
         * @param server the server policy that is managed by the group policy
         * @param spec sapphire object spec
         */
        void onCreate(
                String region, SapphirePolicy.SapphireServerPolicy server, SapphireObjectSpec spec)
                throws RemoteException;

        void addServer(SapphirePolicy.SapphireServerPolicy server) throws RemoteException;

        void onDestroy() throws RemoteException;

        void removeServer(SapphirePolicy.SapphireServerPolicy server) throws RemoteException;

        ArrayList<SapphirePolicy.SapphireServerPolicy> getServers() throws RemoteException;

        void onFailure(SapphirePolicy.SapphireServerPolicy server) throws RemoteException;

        SapphirePolicy.SapphireServerPolicy onRefRequest() throws RemoteException;
    }
}
