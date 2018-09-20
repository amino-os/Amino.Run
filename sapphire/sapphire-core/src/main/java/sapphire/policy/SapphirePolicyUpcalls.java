package sapphire.policy;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import sapphire.policy.SapphirePolicy.SapphireGroupPolicy;
import sapphire.policy.SapphirePolicy.SapphireServerPolicy;

public interface SapphirePolicyUpcalls {
    interface SapphireClientPolicyUpcalls extends Serializable {
        void onCreate(SapphireGroupPolicy group, Annotation[] annotations);

        void setServer(SapphireServerPolicy server);

        SapphireServerPolicy getServer();

        SapphireGroupPolicy getGroup();

        Object onRPC(String method, ArrayList<Object> params) throws Exception;
    }

    interface SapphireServerPolicyUpcalls extends Serializable {
        void onCreate(SapphireGroupPolicy group, Annotation[] annotations);

        void initialize();

        void onDestroy();

        SapphireGroupPolicy getGroup();

        Object onRPC(String method, ArrayList<Object> params) throws Exception;

        void onMembershipChange();

        List<SapphirePolicyContainer> getProcessedPolicies();

        void setNextDMs(List<SapphirePolicyContainer> DMChain);

        void setProcessedPolicies(List<SapphirePolicyContainer> setProcessedDMs);
    }

    interface SapphireGroupPolicyUpcalls extends Serializable {
        void onCreate(SapphireServerPolicy server, Annotation[] annotations) throws RemoteException;

        void addServer(SapphireServerPolicy server) throws RemoteException;

        void onDestroy() throws RemoteException;

        void removeServer(SapphireServerPolicy server) throws RemoteException;

        ArrayList<SapphireServerPolicy> getServers() throws RemoteException;

        void onFailure(SapphireServerPolicy server) throws RemoteException;

        SapphireServerPolicy onRefRequest() throws RemoteException;
    }
}
