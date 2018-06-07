package sapphire.policy;

import java.io.Serializable;
import java.util.ArrayList;

import sapphire.policy.SapphirePolicy.SapphireGroupPolicy;
import sapphire.policy.SapphirePolicy.SapphireServerPolicy;
import sapphire.policy.dmchain.DMChainManager;

public interface SapphirePolicyUpcalls {
	interface  SapphireClientPolicyUpcalls extends Serializable {
		void onCreate(SapphireGroupPolicy group);
//		void setDMChainManager(DMChainManager dmChainManager);
		void setServer(SapphireServerPolicy server);
		SapphireServerPolicy getServer();
		SapphireGroupPolicy getGroup();
		Object onRPC(String method, ArrayList<Object> params) throws Exception;
	}
	
	interface SapphireServerPolicyUpcalls extends Serializable {
		void onCreate(SapphireGroupPolicy group);
//		void setDMChainManager(DMChainManager dmChainManager);
		SapphireGroupPolicy getGroup();
		Object onRPC(String method, ArrayList<Object> params) throws Exception;
		void onMembershipChange();
	}
	
	interface SapphireGroupPolicyUpcalls extends Serializable {
		void onCreate(SapphireServerPolicy server);
//		void setDMChainManager(DMChainManager dmChainManager);
		void addServer(SapphireServerPolicy server);
		void removeServer(SapphireServerPolicy server);
		ArrayList<SapphireServerPolicy> getServers();
		void onFailure(SapphireServerPolicy server);
		SapphireServerPolicy onRefRequest();
	}
}
