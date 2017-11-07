package sapphire.policy;

import java.io.Serializable;
import java.util.ArrayList;

import sapphire.policy.SapphirePolicy.SapphireGroupPolicy;
import sapphire.policy.SapphirePolicy.SapphireServerPolicy;

public interface SapphireCachingPolicyUpcalls extends Serializable {
	
	public interface  SapphireCachingClientUpcalls extends Serializable {
		public void onCreate(SapphireGroupPolicy group);
		public void setServer(SapphireServerPolicy server);
		public SapphireServerPolicy getServer();
		public SapphireGroupPolicy getGroup();
		public Object onRPC(String method, ArrayList<Object> params) throws Exception;
	}
	
	public interface SapphireCachingServerUpcalls extends Serializable {
		public void onCreate(SapphireGroupPolicy group);
		public SapphireGroupPolicy getGroup();
		public Object onRPC(String method, ArrayList<Object> params) throws Exception;
	}
	

}
