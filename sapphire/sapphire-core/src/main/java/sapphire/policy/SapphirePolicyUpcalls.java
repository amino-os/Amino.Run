package sapphire.policy;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Map;
import sapphire.app.DMSpec;
import sapphire.policy.SapphirePolicy.SapphireGroupPolicy;
import sapphire.policy.SapphirePolicy.SapphireServerPolicy;

public interface SapphirePolicyUpcalls {
    /**
     * Each sapphire policy can optionally define a Config class. This config class has to implement
     * {@link SapphirePolicyConfig} interface. This config class allows programmers to pass some
     * configurations into the sapphire policy.
     */
    interface SapphirePolicyConfig extends Serializable {
        /** @return {@link DMSpec} */
        DMSpec toDMSpec();

        /**
         * Parse the given {@link DMSpec} and returns a new {@link SapphirePolicyConfig} instance.
         * @param spec {@link DMSpec}
         * @return a new {@link SapphirePolicyConfig} instance
         */
        SapphirePolicyConfig fromDMSpec(DMSpec spec);
    }

    interface SapphireClientPolicyUpcalls extends Serializable {
        // TODO(multi-lang): replace annotations with Map<String, DMSpec>
        void onCreate(SapphireGroupPolicy group, Annotation[] annotations);

        void setServer(SapphireServerPolicy server);

        SapphireServerPolicy getServer();

        SapphireGroupPolicy getGroup();

        Object onRPC(String method, ArrayList<Object> params) throws Exception;
    }

    interface SapphireServerPolicyUpcalls extends Serializable {
        /**
         * Initialize server policy.
         *
         * @param group the group policy that manages this server policy
         * @param dmSpecMap the map that contains all DM specification. The key is the DM name. The
         *     value is the DM specification.
         */
        void onCreate(SapphireGroupPolicy group, Map<String, DMSpec> dmSpecMap);

        void onDestroy();

        SapphireGroupPolicy getGroup();

        Object onRPC(String method, ArrayList<Object> params) throws Exception;

        void onMembershipChange();
    }

    interface SapphireGroupPolicyUpcalls extends Serializable {
        // TODO(multi-lang): replace annotations with Map<String, DMSpec>
        void onCreate(SapphireServerPolicy server, Annotation[] annotations) throws RemoteException;

        void addServer(SapphireServerPolicy server) throws RemoteException;

        void onDestroy() throws RemoteException;

        void removeServer(SapphireServerPolicy server) throws RemoteException;

        ArrayList<SapphireServerPolicy> getServers() throws RemoteException;

        void onFailure(SapphireServerPolicy server) throws RemoteException;

        SapphireServerPolicy onRefRequest() throws RemoteException;
    }
}
