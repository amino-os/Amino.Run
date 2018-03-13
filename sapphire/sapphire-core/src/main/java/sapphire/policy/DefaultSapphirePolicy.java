package sapphire.policy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
		// TODO (Terry): Upgrade JDK to 1.8 and replace Hashset with ConcurrentHashSet
		Set<SapphireServerPolicy> servers = new HashSet<SapphireServerPolicy>();

		@Override
		public void addServer(SapphireServerPolicy server) {
			// TODO (Terry): Verifies equals method is implemented properly in SapphireServerPolicy
			servers.add(server);
		}

		@Override
		public void onFailure(SapphireServerPolicy server) {}

		@Override
		public SapphireServerPolicy onRefRequest() {
			return null;
		}

		@Override
		public ArrayList<SapphireServerPolicy> getServers() {
			// TODO (Terry): We should return a defensive copy of server clones!
			return new ArrayList<SapphireServerPolicy>(servers);
		}

		@Override
		public void onCreate(SapphireServerPolicy server) {
			// TODO Auto-generated method stub

		}
	}
}
