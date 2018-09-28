package sapphire.policy;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import sapphire.app.DMSpec;

public class DefaultSapphirePolicy extends SapphirePolicy {

    public static class DefaultServerPolicy extends SapphireServerPolicy {
        @Override
        public SapphireGroupPolicy getGroup() {
            return group;
        }

        @Override
        public void onMembershipChange() {}

        @Override
        public void onCreate(SapphireGroupPolicy group, Map<String, DMSpec> dmSpecMap) {
            super.onCreate(group, dmSpecMap);
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
        public void onCreate(SapphireGroupPolicy group, Map<String, DMSpec> dmSpecMap) {
            this.group = (DefaultGroupPolicy) group;
        }
    }

    public static class DefaultGroupPolicy extends SapphireGroupPolicy {
        private Set<SapphireServerPolicy> servers =
                Collections.newSetFromMap(new ConcurrentHashMap<SapphireServerPolicy, Boolean>());

        @Override
        public synchronized void addServer(SapphireServerPolicy server) throws RemoteException {
            if (servers == null) {
                // TODO: Need to change it to proper exception
                throw new RemoteException("Group object deleted");
            }
            servers.add(server);
        }

        @Override
        public void removeServer(SapphireServerPolicy server) throws RemoteException {
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
        public void onCreate(SapphireServerPolicy server, Map<String, DMSpec> dmSpecMap)
                throws RemoteException {
            addServer(server);
        }

        @Override
        public synchronized void onDestroy() throws RemoteException {
            super.onDestroy();
            servers = null;
        }
    }
}
