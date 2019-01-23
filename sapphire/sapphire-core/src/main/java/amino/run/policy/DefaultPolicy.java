package amino.run.policy;

import amino.run.app.MicroServiceSpec;
import java.rmi.RemoteException;
import java.util.ArrayList;

public class DefaultPolicy extends Policy {

    public static class DefaultServerPolicy extends ServerPolicy {
        @Override
        public GroupPolicy getGroup() {
            return group;
        }

        @Override
        public void onMembershipChange() {}

        @Override
        public void onCreate(GroupPolicy group, MicroServiceSpec spec) {
            super.onCreate(group, spec);
        }

        public void onDestroy() {
            super.onDestroy();
        }
    }

    public static class DefaultClientPolicy extends ClientPolicy {
        private DefaultServerPolicy server;
        private DefaultGroupPolicy group;

        @Override
        public void setServer(ServerPolicy server) {
            this.server = (DefaultServerPolicy) server;
        }

        @Override
        public ServerPolicy getServer() {
            return server;
        }

        @Override
        public GroupPolicy getGroup() {
            return group;
        }

        @Override
        public void onCreate(GroupPolicy group, MicroServiceSpec spec) {
            this.group = (DefaultGroupPolicy) group;
        }
    }

    public static class DefaultGroupPolicy extends GroupPolicy {
        private ArrayList<ServerPolicy> servers = new ArrayList<ServerPolicy>();
        protected String region = "";
        protected MicroServiceSpec spec = null;

        @Override
        /**
         * it is advisable that defer putting this server policy into group policy until the server
         * policy is complete with full policy chain.
         */
        public synchronized void addServer(ServerPolicy server) throws RemoteException {
            if (servers == null) {
                // TODO: Need to change it to proper exception
                throw new RemoteException("Group object deleted");
            }
            servers.add(server);
        }

        @Override
        public synchronized void removeServer(ServerPolicy server) throws RemoteException {
            if (servers != null) {
                servers.remove(server);
            }
        }

        @Override
        public ArrayList<ServerPolicy> getServers() throws RemoteException {
            if (servers == null) {
                return new ArrayList<ServerPolicy>();
            }
            return new ArrayList<ServerPolicy>(servers);
        }

        @Override
        public void onCreate(String region, ServerPolicy server, MicroServiceSpec spec)
                throws RemoteException {
            this.region = region;
            this.spec = spec;
        }

        @Override
        public synchronized void onDestroy() throws RemoteException {
            super.onDestroy();
            servers = null;
        }
    }
}
