package sapphire.policy;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;

import sapphire.policy.SapphirePolicy.SapphireGroupPolicy;
import sapphire.policy.SapphirePolicy.SapphireServerPolicy;

public interface SapphirePolicyUpcalls {
	interface  SapphireClientPolicyUpcalls extends Serializable {
		void onCreate(SapphireGroupPolicy group);
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
		void onCreate(SapphireServerPolicy server, Annotation[] annotations);
		void addServer(SapphireServerPolicy server);
		void removeServer(SapphireServerPolicy server);
		ArrayList<SapphireServerPolicy> getServers();
		void onFailure(SapphireServerPolicy server);
		SapphireServerPolicy onRefRequest();
	}
}
