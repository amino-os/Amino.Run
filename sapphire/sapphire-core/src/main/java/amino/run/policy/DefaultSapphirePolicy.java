package amino.run.policy;

import amino.run.app.SapphireObjectSpec;
import java.rmi.RemoteException;
import java.util.ArrayList;

public class DefaultSapphirePolicy extends SapphirePolicy {

    public static class DefaultServerPolicy extends SapphireServerPolicy {
        @Override
        public SapphireGroupPolicy getGroup() {
            return group;
        }

        @Override
        public void onMembershipChange() {}

        @Override
        public void onCreate(SapphireGroupPolicy group, SapphireObjectSpec spec) {
            super.onCreate(group, spec);
        }

        public void onDestroy() {
            super.onDestroy();
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
        public void onCreate(SapphireGroupPolicy group, SapphireObjectSpec spec) {
            this.group = (DefaultGroupPolicy) group;
        }
    }

    public static class DefaultGroupPolicy extends SapphireGroupPolicy {
        private ArrayList<SapphireServerPolicy> servers = new ArrayList<SapphireServerPolicy>();
        protected String region = "";
        protected SapphireObjectSpec spec = null;

        @Override
        /**
         * it is advisable that defer putting this server policy into group policy until the server
         * policy is complete with full policy chain.
         */
        public synchronized void addServer(SapphireServerPolicy server) throws RemoteException {
            if (servers == null) {
                // TODO: Need to change it to proper exception
                throw new RemoteException("Group object deleted");
            }
            servers.add(server);
        }

        @Override
        public synchronized void removeServer(SapphireServerPolicy server) throws RemoteException {
            if (servers != null) {
                servers.remove(server);
            }
        }

        @Override
        public void onFailure(SapphireServerPolicy server) throws RemoteException {}

        @Override
        public ArrayList<SapphireServerPolicy> getServers() throws RemoteException {
            if (servers == null) {
                return new ArrayList<SapphireServerPolicy>();
            }
            return new ArrayList<SapphireServerPolicy>(servers);
        }

        @Override
        public void onCreate(String region, SapphireServerPolicy server, SapphireObjectSpec spec)
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
