package sapphire.policy;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Map;
import sapphire.policy.SapphirePolicy.SapphireGroupPolicy;
import sapphire.policy.SapphirePolicy.SapphireServerPolicy;

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
         * @param configMap the map that contains sapphire policy configurations. The key is the
         *     class name of the configuration and the value is a {@link SapphirePolicyConfig}
         *     instance.
         */
        void onCreate(SapphireGroupPolicy group, Map<String, SapphirePolicyConfig> configMap);

        void setServer(SapphireServerPolicy server);

        SapphireServerPolicy getServer();

        SapphireGroupPolicy getGroup();

        Object onRPC(String method, ArrayList<Object> params) throws Exception;
    }

    interface SapphireServerPolicyUpcalls extends Serializable {

        // TODO (merged):
        // onCreate is called during creation of Sapphire policy.
        // void onCreate(SapphireGroupPolicy group, Annotation[] annotations);
        /**
         * Initialize server policy.
         *
         * @param group the group policy that manages this server policy
         * @param configMap the map that contains sapphire policy configurations. The key is the
         *     class name of the configuration and the value is a
         */
        void onCreate(SapphireGroupPolicy group, Map<String, SapphirePolicyConfig> configMap);

        // initialize is called after migration.
        void initialize();

        void onDestroy();

        SapphireGroupPolicy getGroup();

        Object onRPC(String method, ArrayList<Object> params) throws Exception;

        void onMembershipChange();
    }

    interface SapphireGroupPolicyUpcalls extends Serializable {
        /**
         * Initialize g porouplicy.
         *
         * @param region
         * @param server the server policy that is managed by the group policy
         * @param configMap the map that contains sapphire policy configurations. The key is the
         *     class name of the configuration, and the value is a {@link SapphirePolicyConfig}
         *     instance.
         */
        void onCreate(
                String region,
                SapphireServerPolicy server,
                Map<String, SapphirePolicyConfig> configMap)
                throws RemoteException;

        void addServer(SapphireServerPolicy server) throws RemoteException;

        void onDestroy() throws RemoteException;

        void removeServer(SapphireServerPolicy server) throws RemoteException;

        ArrayList<SapphireServerPolicy> getServers() throws RemoteException;

        void onFailure(SapphireServerPolicy server) throws RemoteException;

        SapphireServerPolicy onRefRequest() throws RemoteException;

        void onMigrate(SapphireServerPolicy server) throws RemoteException;
    }
}
