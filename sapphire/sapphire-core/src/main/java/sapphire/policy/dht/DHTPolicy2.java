package sapphire.policy.dht;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import sapphire.policy.SapphirePolicy;

public class DHTPolicy2 extends SapphirePolicy {

	public static class DHTClientPolicy extends SapphireClientPolicy {

		DHTServerPolicy server = null;
		DHTGroupPolicy group = null;

		@Override
		public void onCreate(SapphireGroupPolicy group) {
			this.group = (DHTGroupPolicy) group;
		}

		@Override
		public void setServer(SapphireServerPolicy server) {
			this.server = (DHTServerPolicy) server;
		}

		@Override
		public SapphireServerPolicy getServer() {
			return server;
		}

		@Override
		public SapphireGroupPolicy getGroup() {
			return group;
		}
	}

	public static class DHTServerPolicy extends SapphireServerPolicy {
		private static Logger logger = Logger.getLogger(SapphireServerPolicy.class.getName());
		private DHTGroupPolicy group = null;

		@Override
		public void onCreate(SapphireGroupPolicy group) {
			this.group = (DHTGroupPolicy) group;
		}

		@Override
		public SapphireGroupPolicy getGroup() {
			return group;
		}

		@Override
		public void onMembershipChange() {}

		@Override
		public Object onRPC(String method, ArrayList<Object> params) throws Exception {
			/* We assume that the first param is the index */
			DHTServerPolicy responsibleNode = group.dhtGetResponsibleNode((String)params.get(0));
			return responsibleNode.forwardedRPC(method, params);
		}

		public Object forwardedRPC(String method, ArrayList<Object> params) throws Exception {
			return super.onRPC(method, params);
		}
	}

	public static class DHTGroupPolicy extends SapphireGroupPolicy {
		private static Logger logger = Logger.getLogger(SapphireGroupPolicy.class.getName());
		private HashMap<String, DHTNode> nodes;
		private int groupSize = 0;

		private class DHTNode implements Comparable<DHTNode>, Serializable {
			public String id;
			public DHTServerPolicy server;

			public DHTNode(String id, DHTServerPolicy server) {
				this.id = id;
				this.server = server;
			}

			@Override
			public boolean equals(Object other) {
				DHTNode o = (DHTNode) other;
				if (o == null)
					return false;
				if (o.id.compareTo(id) == 0)
					return true;
				return false;
			}

			@Override
			/**
			 * @throws NullPointerException if another is null
			 */
			public int compareTo(DHTNode another) {
				return another.id.compareTo(this.id);
			}
		}

		@Override
		public void onCreate(SapphireServerPolicy server) {
			nodes = new HashMap<String, DHTNode>();

			try {
				ArrayList<String> regions = sapphire_getRegions();

				// Add the first DHT node
				groupSize++;
				String id = Integer.toString(groupSize);

				DHTServerPolicy dhtServer = (DHTServerPolicy) server;
				DHTNode newNode = new DHTNode(id, dhtServer);
				nodes.put(id, newNode);

				for (int i = 1; i < regions.size(); i++) {
					DHTServerPolicy replica = (DHTServerPolicy)dhtServer.sapphire_replicate();
					replica.sapphire_pin(regions.get(i));
				}
				dhtServer.sapphire_pin(regions.get(0));
			} catch (RemoteException e) {
				e.printStackTrace();
				throw new Error("Could not create new group policy because the oms is not available.");
			}
		}


		@Override
		public void addServer(SapphireServerPolicy server) {
			String id = Integer.toString(groupSize);
			DHTServerPolicy dhtServer = (DHTServerPolicy) server;
			DHTNode newNode = new DHTNode(id, dhtServer);
			nodes.put(id, newNode);

			groupSize++;
			logger.info("NODES: " + nodes.toString() + " Group size = " + groupSize);
		}

		@Override
		public ArrayList<SapphireServerPolicy> getServers() {
			ArrayList servers  = new ArrayList<SapphireServerPolicy> ();
			for (String id: nodes.keySet()) {
				servers.add(nodes.get(id).server);
			}

			return servers;
		}

		@Override
		public void removeServer(SapphireServerPolicy server) {
		}

		@Override
		public void onFailure(SapphireServerPolicy server) {
			// TODO
		}

		@Override
		public SapphireServerPolicy onRefRequest() {
			// TODO
			return null;
		}

		/**
		 * TODO: key is literally from the input value currently. It should be hashed key.
		 */
		public DHTServerPolicy dhtGetResponsibleNode(String key) {
			if (nodes.containsKey(key)) {
				DHTServerPolicy server = nodes.get(key).server;
				logger.info("Retrieved the server for key " + key);
				return server;
			}

			logger.severe("Could not find server for key " + key);
			return null;
		}
	}
}
