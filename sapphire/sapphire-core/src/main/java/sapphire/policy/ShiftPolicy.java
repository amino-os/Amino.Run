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
		private static final long serialVersionUID = -6003032571186258361L;
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
		private static final long serialVersionUID = -6002032571186258361L;
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
			logger.info("[shift] onRPC " + this.oid + " method: " + method + ", times been called:" + this.shiftRPCLoad );
			Object obj = super.onRPC(method, params);

			if (this.shiftRPCLoad > 0 && this.shiftRPCLoad % this.LOAD == 0) {
				logger.info("[shift] time to shift off");
				OMSServer oms = GlobalKernelReferences.nodeServer.oms;
				ArrayList<InetSocketAddress> servers = oms.getServers();
				for (InetSocketAddress node : servers) {
					logger.info("[shift] shifting candidate: " + node );
				}

				KernelServerImpl localKernel = GlobalKernelReferences.nodeServer;
				InetSocketAddress localAddress = localKernel.getLocalHost();
				logger.info("[shift] local kernel address: " + localAddress );

				InetSocketAddress shiftWinner = this.getNextShiftTarget(servers, localAddress);
				if (shiftWinner.equals(localAddress)) {
					logger.info("[shift] no target to shift" );
				} else {
					logger.info("[shift] shifting SO " + this.oid + " to " + shiftWinner);
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
		private static final long serialVersionUID = -6001032571186258361L;
	}
}