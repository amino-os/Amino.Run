package sapphire.policy.servercache;

import java.util.ArrayList;



import sapphire.policy.SapphirePolicy;
import sapphire.policy.cache.CacheLeasePolicy.CacheLeaseServerPolicy;

/* 
 * This class must be applied to an object that extends the List class.
 * 
 * It interposes on each method call, stores everything to disk and caches sublists. (see Facebook's TAO paper)
 */

public class DurableOnDemandCachedList extends SapphirePolicy {

	public static class DurableOnDemandCachedListClientPolicy extends SapphireClientPolicy {
		private DurableOnDemandCachedListServerPolicy server;
		private DurableOnDemandCachedListGroupPolicy group;

		@Override
		public void onCreate(SapphireGroupPolicy group) {
			this.group = (DurableOnDemandCachedListGroupPolicy) group;
		}

		@Override
		public SapphireGroupPolicy getGroup() {
			return group;
		}

		@Override
		public SapphireServerPolicy getServer() {
			return server;
		}

		@Override
		public void setServer(SapphireServerPolicy server) {
			this.server = (DurableOnDemandCachedListServerPolicy) server;
		}

		@Override
		public Object onRPC(String method, ArrayList<Object> params) throws Exception {
			/* Switch on the method we need to execute */



			return null;
		}
	}


	// TODO: think about concurrency
	public static class DurableOnDemandCachedListServerPolicy extends SapphireServerPolicy {
		private DurableOnDemandCachedListGroupPolicy group;
		int listSize;  // cache the size of the list
		int numMisses; // to automatically grow the cache if possible


		@Override
		public void onCreate(SapphireGroupPolicy group) {
			// TODO Auto-generated method stub
			this.group = (DurableOnDemandCachedListGroupPolicy) group;
		}

		@Override
		public SapphireGroupPolicy getGroup() {
			return group;
		}

		@Override
		public void onMembershipChange() {

		}
		
		
		@Override
		public Object onRPC(String method, ArrayList<Object> params) throws Exception {
			return null;
		}
		
	}

	public static class DurableOnDemandCachedListGroupPolicy extends SapphireGroupPolicy {
		CacheLeaseServerPolicy server;

		@Override
		public void addServer(SapphireServerPolicy server) {
			this.server = (CacheLeaseServerPolicy) server;
		}

		@Override
		public void onFailure(SapphireServerPolicy server) {

		}

		@Override
		public SapphireServerPolicy onRefRequest() {
			return server;
		}

		@Override
		public ArrayList<SapphireServerPolicy> getServers() {
			return null;
		}

		@Override
		public void onCreate(SapphireServerPolicy server) {
			addServer(server);
		}
	}
}
