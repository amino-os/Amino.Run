package amino.run.policy;

import amino.run.common.MicroServiceNotFoundException;
import amino.run.common.MicroServiceReplicaNotFoundException;
import amino.run.policy.transaction.IllegalComponentException;
import amino.run.policy.transaction.TransactionContext;
import amino.run.policy.transaction.TwoPCClient;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

public abstract class DefaultUpcallImpl extends Library {

    public abstract static class ClientPolicy extends ClientPolicyLibrary {
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            // only transaction-capable SO is allowed in DCAP transaction -- change of the original
            // behavior
            if (!(this instanceof TwoPCClient) && this.hasTransaction()) {
                throw new IllegalComponentException(method);
            }

            /* The default behavior is to just perform the RPC to the Policy Server */
            Object ret = null;

            try {
                ret = getServer().onRPC(method, params);
            } catch (RemoteException e) {
                // TODO: Quinton: This looks like a bug.  RemoteExceptions are silently swallowed
                // and null is returned.
                setServer(getGroup().onRefRequest());
            }
            return ret;
        }

        /**
         * Set the remote server policy in cache.
         *
         * @param server Remote server policy
         */
        protected abstract void setServer(Policy.ServerPolicy server);

        /**
         * Get the cached reference to remote server policy object.
         *
         * @return Remote server policy
         * @throws RemoteException
         */
        protected abstract Policy.ServerPolicy getServer() throws RemoteException;

        /**
         * Get the cached reference to remote group policy object.
         *
         * @return Remote group policy
         */
        protected abstract Policy.GroupPolicy getGroup();

        protected UUID getCurrentTransaction() {
            return TransactionContext.getCurrentTransaction();
        }

        protected boolean hasTransaction() {
            return this.getCurrentTransaction() != null;
        }
    }

    public abstract static class ServerPolicy extends ServerPolicyLibrary {
        /**
         * Get the local cached list of all replicas for the microservice.
         *
         * <p>Note: This list gets populated iff the respective DM's group policy support
         * onMembershipChange notifications.
         *
         * @return List of server policies
         * @throws RemoteException
         */
        protected abstract ArrayList<Policy.ServerPolicy> getServers() throws RemoteException;

        /**
         * Membership change notification handler invoked from respective DM's group policy upon
         * addition/removal of replica. It is used to refresh the locally cached list of replicas(if
         * replica maintains any). And replica can initialize its data structures maintained w.r.t
         * to other replicas of the microservice
         *
         * @param servers
         * @throws RemoteException
         */
        public abstract void onMembershipChange(ArrayList<Policy.ServerPolicy> servers)
                throws RemoteException;

        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            return appObject.invoke(method, params);
        }

        /* This function is here just to generate the stub for this function in all server policies */
        @Override
        public Policy.ServerPolicy replicate(String region) throws RemoteException {
            return super.replicate(region);
        }

        /* This function is here just to generate the stub for this function in all server policies */
        @Override
        public void pin_to_server(InetSocketAddress server)
                throws RemoteException, MicroServiceNotFoundException,
                        MicroServiceReplicaNotFoundException {
            super.pin_to_server(server);
        }

        @Override
        public void terminate() throws RemoteException {
            super.terminate();
        }
    }

    public abstract static class GroupPolicy extends GroupPolicyLibrary {

        /*
         * INTERNAL FUNCTIONS (Used by MicroService runtime)
         */
        public void $__initialize(String appObjectClassName, ArrayList<Object> params) {
            this.appObjectClassName = appObjectClassName;
            this.params = params;
        }

        @Override
        public Policy.ServerPolicy onRefRequest() throws RemoteException {
            ArrayList<Policy.ServerPolicy> servers = getServers();
            return servers.get(new Random().nextInt(servers.size()));
        }
    }
}
