package sapphire.policy;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import sapphire.kernel.common.KernelOID;
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
		void onCreate(SapphireGroupPolicy group);
		SapphireGroupPolicy getGroup();
		Object onRPC(String method, ArrayList<Object> params) throws Exception;
		void onMembershipChange();
		List<SapphirePolicyContainer> getProcessedDMs();
		void setThisDM(String thisDM);
		void setNextDMs(List<String> DMChain);
		void setProcessedDMs(List<SapphirePolicyContainer> setProcessedDMs);
	}
	
	interface SapphireGroupPolicyUpcalls extends Serializable {
		void onCreate(SapphireServerPolicy server);
		void addServer(SapphireServerPolicy server);
		void removeServer(SapphireServerPolicy server);
		ArrayList<SapphireServerPolicy> getServers();
		void onFailure(SapphireServerPolicy server);
		SapphireServerPolicy onRefRequest();
	}
}
