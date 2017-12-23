package sapphire.policy;

import java.util.ArrayList;
import java.util.logging.Logger;
import java.net.InetSocketAddress;

// sapphire rt
import sapphire.kernel.common.GlobalKernelReferences;
import sapphire.oms.OMSServer;
import sapphire.kernel.server.KernelServerImpl;

public class ShiftPolicy extends SapphirePolicy {

	public static class ShiftClientPolicy extends DefaultSapphirePolicy.DefaultClientPolicy {
		SapphireServerPolicy server = null;
		SapphireGroupPolicy group = null;

		@Override
		public void onCreate(SapphireGroupPolicy group) {
			this.group = group;
		}

		@Override
		public void setServer(SapphireServerPolicy server) {
			this.server = server;
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

	public static class ShiftServerPolicy extends DefaultSapphirePolicy.DefaultServerPolicy {
		private static Logger logger = Logger.getLogger(SapphireServerPolicy.class.getName());

		private static int LOAD = 5;
		private static int shiftRPCLoad = 0;

		private SapphireGroupPolicy group = null;

		@Override
		public void onCreate(SapphireGroupPolicy group) {
			this.group = group;
		}

		@Override
		public SapphireGroupPolicy getGroup() {
			return group;
		}

		@Override
		public void onMembershipChange() { super.onMembershipChange(); }

		@Override
		public Object onRPC(String method, ArrayList<Object> params) throws Exception {
			this.shiftRPCLoad ++;
			Object obj = super.onRPC(method, params);

			if (this.shiftRPCLoad > 0 && this.shiftRPCLoad % this.LOAD == 0) {
				System.out.println("[ShiftPolicy] Limit reached at " + this.LOAD + ". Shift policy triggered.");
				OMSServer oms = GlobalKernelReferences.nodeServer.oms;
				ArrayList<InetSocketAddress> servers = oms.getServers();

				KernelServerImpl localKernel = GlobalKernelReferences.nodeServer;
				InetSocketAddress localAddress = localKernel.getLocalHost();
				InetSocketAddress shiftWinner = this.getNextShiftTarget(servers, localAddress);

				if (shiftWinner.equals(localAddress)) {
					System.out.println("[ShiftPolicy] There are no targets to migrate Sapphire object to." );
				} else {
					System.out.println("[ShiftPolicy] Shifting Sapphire object " + this.oid + " to " + shiftWinner);
					localKernel.moveKernelObjectToServer(shiftWinner, this.oid);
				}
			}

			return obj;
		}

		private static InetSocketAddress getNextShiftTarget(ArrayList<InetSocketAddress> candidates, InetSocketAddress curr)
		{
			InetSocketAddress chosen =  candidates.get(0);

			Boolean foundCurr = false;
			for (InetSocketAddress node : candidates) {
				if (node.equals(curr)) {
					foundCurr = true;
					continue;
				}

				if (foundCurr){
					chosen = node;
					break;
				}
			}

			return chosen;
		}
	}

	public static class ShiftGroupPolicy extends DefaultSapphirePolicy.DefaultGroupPolicy {
	}
}