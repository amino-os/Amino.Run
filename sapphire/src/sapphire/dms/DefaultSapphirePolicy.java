package sapphire.policy;

import java.util.ArrayList;

public class DefaultSapphirePolicy extends SapphirePolicy {
	
	public static class DefaultServerPolicy extends SapphireServerPolicy {
		private DefaultGroupPolicy group;
		
		@Override
		public SapphireGroupPolicy getGroup() {
			return group;
		}

		@Override
		public void onMembershipChange() {}

		@Override
		public void onCreate(SapphireGroupPolicy group) {
			// TODO Auto-generated method stub
			this.group = (DefaultGroupPolicy) group;
		}
	}
	
	public static class DefaultClientPolicy extends SapphireClientPolicy {
		private DefaultServerPolicy server;
		private DefaultGroupPolicy group;
		
		@Override
		public void setServer(SapphireServerPolicy server) {
			this.server = (DefaultServerPolicy) server;
		}

		@Override
		public SapphireServerPolicy getServer() {
			return server;
		}

		@Override
		public SapphireGroupPolicy getGroup() {
			return group;
		}

		@Override
		public void onCreate(SapphireGroupPolicy group) {
			// TODO Auto-generated method stub
			this.group = (DefaultGroupPolicy) group;
		}
	}
	
	public static class DefaultGroupPolicy extends SapphireGroupPolicy {

		@Override
		public void addServer(SapphireServerPolicy server) {}

		@Override
		public void onFailure(SapphireServerPolicy server) {}

		@Override
		public SapphireServerPolicy onRefRequest() {
			return null;
		}

		@Override
		public ArrayList<SapphireServerPolicy> getServers() {
			return null;
		}

		@Override
		public void onCreate(SapphireServerPolicy server) {
			// TODO Auto-generated method stub
			
		}
		
	}
}
