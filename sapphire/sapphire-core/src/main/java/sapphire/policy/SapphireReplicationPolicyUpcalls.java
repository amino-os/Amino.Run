package sapphire.policy;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import sapphire.policy.SapphirePolicy.SapphireGroupPolicy;
import sapphire.policy.SapphirePolicy.SapphireServerPolicy;

// TODO: Quinton: This seems to be a strict subset (cut 'n paste) of SapphirePolicyUpcalls.  It
// smells like we need to refactor.
public interface SapphireReplicationPolicyUpcalls extends Serializable {
    public interface SapphireReplicationServerUpcalls extends Serializable {
        public void onCreate(SapphireGroupPolicy group, Annotation[] annotations);

        public SapphireGroupPolicy getGroup();

        public Object onRPC(String method, ArrayList<Object> params) throws Exception;

        public void onMembershipChange();
    }

    public interface SapphireReplicationGroupUpcalls extends Serializable {
        public void onCreate(SapphireServerPolicy server, Annotation[] annotations);

        public void addServer(SapphireServerPolicy server);

        public ArrayList<SapphireServerPolicy> getServers();

        public void onFailure(SapphireServerPolicy server);

        public SapphireServerPolicy onRefRequest();
    }
}
