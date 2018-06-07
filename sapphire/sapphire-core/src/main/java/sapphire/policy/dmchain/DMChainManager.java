package sapphire.policy.dmchain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import sapphire.policy.SapphirePolicy;

public class DMChainManager implements Serializable {

	private List<SapphirePolicy.SapphireGroupPolicy> groups = new ArrayList<SapphirePolicy.SapphireGroupPolicy>();
	private List<SapphirePolicy.SapphireServerPolicy> servers = new ArrayList<SapphirePolicy.SapphireServerPolicy>();
	private List<SapphirePolicy.SapphireClientPolicy> clients = new ArrayList<SapphirePolicy.SapphireClientPolicy>();

	private int groupIdx = 1, serverIdx = 1, clientIdx = 0;

	public SapphirePolicy.SapphireGroupPolicy getNextGroup() {
		if (groups.size() <= groupIdx) {
			return null;
		}

		return groups.get(groupIdx++);
	}

	public SapphirePolicy.SapphireServerPolicy getNextServer() {
		--serverIdx;

		if (serverIdx < 0) {
			return null;
		}

		return servers.get(serverIdx);
	}

	public SapphirePolicy.SapphireClientPolicy getNextClient() {
		++clientIdx;

		if (clients.size() <= clientIdx) {
			return null;
		}

		return clients.get(clientIdx);
	}

	public SapphirePolicy.SapphireGroupPolicy getGroup(int i) {
		if (groups.size() <= i) {
			return null;
		}
		return groups.get(i);
	}

	public SapphirePolicy.SapphireServerPolicy getServer(int i) {
		if (servers.size() <= i) {
			return null;
		}

		return servers.get(i);
	}

	public SapphirePolicy.SapphireClientPolicy getClient(int i) {
		if (clients.size() <= i) {
			return null;
		}

		return clients.get(i);
	}

	public void setGroups(List<SapphirePolicy.SapphireGroupPolicy> groups) {
		this.groups = groups;
	}

	public void setServers(List<SapphirePolicy.SapphireServerPolicy> servers) {
		this.servers = servers;
		// Point to the last server.
		serverIdx = servers.size() - 1;
	}

	public void setClients(List<SapphirePolicy.SapphireClientPolicy> clients) {
		this.clients = clients;
	}
}