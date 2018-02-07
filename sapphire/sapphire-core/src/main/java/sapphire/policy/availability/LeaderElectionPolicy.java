package sapphire.policy.availability;

import com.sun.security.ntlm.Server;

import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import sapphire.policy.DefaultSapphirePolicy;
import sapphire.raft.ServerContext;
import sapphire.raft.ServerState;

/**
 * {@code LeaderElectionPolicy} enforces that 1) all operations will be directed to the
 * <em>primary</em> Sapphire object replica, and 2) there will be at most one primary replica at any
 * given time. It is possible that no primary replica exists at some time for limited period in
 * which case the client can retry failed operations.
 *
 * @author terryz
 */

public class LeaderElectionPolicy extends DefaultSapphirePolicy {
    public static class ClientPolicy extends DefaultClientPolicy {
        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            GroupPolicy group = (GroupPolicy)getGroup();
            ServerPolicy server = group.getPrimaryServer();

            Object ret = null;
            try {
                ret = server.onRPC(method, params);
            } catch (Exception e) {
            }
            return ret;
        }
    }

    public static class ServerPolicy extends DefaultServerPolicy {
        private final ServerContext context;
        private final ScheduledExecutorService scheduler =
                Executors.newScheduledThreadPool(1);

        private ServerState.State state;

        public ServerPolicy() {
            this(new ServerContext());
        }

        public ServerPolicy(ServerContext context) {
            this.context = context;
            if (state == ServerState.State.LEADER) {
                // renew lease
                scheduler.schedule((Callable<Boolean>) null, context.getHeartbeatInterval().toMillis(), TimeUnit.MILLISECONDS);
            } else {
                // vote for leader
                scheduler.schedule((Callable<Boolean>) null, context.getElectionTimeout().toMillis(), TimeUnit.MILLISECONDS);
            }
        }

        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            if (state != ServerState.State.LEADER) {
                // TODO: create an exception for leader change
                throw new Exception("Leader changed");
            }

            return super.onRPC(method, params);
        }

        @Override
        protected void finalize() throws Throwable {
            scheduler.shutdown();
            super.finalize();
        }
    }

    public static class GroupPolicy extends DefaultGroupPolicy {
        public ServerPolicy getPrimaryServer() {
            return null;
        }
    }
}
