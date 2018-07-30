package sapphire.policy;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
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

        SapphireGroupPolicy getGroup();

        Object onRPC(String method, ArrayList<Object> params) throws Exception;

        void onMembershipChange();
    }

    interface SapphireGroupPolicyUpcalls extends Serializable {
        void onCreate(SapphireServerPolicy server, Annotation[] annotations) throws Exception;

        void addServer(SapphireServerPolicy server) throws Exception;

        void removeServer(SapphireServerPolicy server) throws Exception;

        ArrayList<SapphireServerPolicy> getServers() throws Exception;

        void onFailure(SapphireServerPolicy server) throws Exception;

        SapphireServerPolicy onRefRequest() throws Exception;
    }
}
