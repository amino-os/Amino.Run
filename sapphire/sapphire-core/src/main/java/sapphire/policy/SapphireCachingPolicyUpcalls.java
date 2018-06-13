package sapphire.policy;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;

import sapphire.policy.SapphirePolicy.SapphireGroupPolicy;
import sapphire.policy.SapphirePolicy.SapphireServerPolicy;

// TODO: Quinton: This is not referenced anywhere and shoulds be deleted/refactored.  It's cut 'n paste from elsewhere in the code and smells bad.
public interface SapphireCachingPolicyUpcalls extends Serializable {
	
	public interface  SapphireCachingClientUpcalls extends Serializable {
		public void onCreate(SapphireGroupPolicy group);
		public void setServer(SapphireServerPolicy server);
		public SapphireServerPolicy getServer();
		public SapphireGroupPolicy getGroup();
		public Object onRPC(String method, ArrayList<Object> params) throws Exception;
	}
	
	public interface SapphireCachingServerUpcalls extends Serializable {
		public void onCreate(SapphireGroupPolicy group, Annotation[] annotations);
		public SapphireGroupPolicy getGroup();
		public Object onRPC(String method, ArrayList<Object> params) throws Exception;
	}
	

}
