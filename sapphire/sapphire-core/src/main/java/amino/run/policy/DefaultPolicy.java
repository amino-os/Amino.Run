package amino.run.policy;

import amino.run.app.MicroServiceSpec;
import amino.run.common.NoKernelServerFoundException;
import amino.run.kernel.common.GlobalKernelReferences;
import amino.run.kernel.server.KernelServerImpl;
import amino.run.oms.OMSServer;
import amino.run.runtime.AddEvent;
import java.net.InetSocketAddress;
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

        /**
         * Migrates Sapphire Object to different Server
         *
         * @throws Exception migrateObject migrates the object to the specified Kernel Server
         */
        @AddEvent(event = "Migrate")
        public void migrateObject(InetSocketAddress destinationAddr) throws Exception {
            logger.info(
                    "Performing Explicit Migration of the object to Destination Kernel Server with address as "
                            + destinationAddr);
            OMSServer oms = GlobalKernelReferences.nodeServer.oms;
            ArrayList<InetSocketAddress> servers = new ArrayList<>(oms.getServers(null));

            KernelServerImpl localKernel = GlobalKernelReferences.nodeServer;
            InetSocketAddress localAddress = localKernel.getLocalHost();

            logger.info(
                    "Performing Explicit Migration of object from "
                            + localAddress
                            + " to "
                            + destinationAddr);

            if (!(servers.contains(destinationAddr))) {
                throw new NoKernelServerFoundException(
                        String.format(
                                "The destinations address %s passed is not present as one of the Kernel Servers",
                                destinationAddr));
            }

            if (!localAddress.equals(destinationAddr)) {
                localKernel.moveKernelObjectToServer(this, destinationAddr);
            }

            logger.info(
                    "Successfully performed Explicit Migration of object from "
                            + localAddress
                            + " to "
                            + destinationAddr);
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
