package amino.run.policy;

import amino.run.common.SapphireObjectNotFoundException;
import amino.run.common.SapphireObjectReplicaNotFoundException;
import amino.run.policy.transaction.IllegalComponentException;
import amino.run.policy.transaction.TransactionContext;
import amino.run.policy.transaction.TwoPCClient;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class DefaultUpcallImpl extends Library {

    public abstract static class ClientPolicy extends SapphireClientPolicyLibrary {
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

        protected UUID getCurrentTransaction() {
            return TransactionContext.getCurrentTransaction();
        }

        protected boolean hasTransaction() {
            return this.getCurrentTransaction() != null;
        }
    }

    public abstract static class ServerPolicy extends SapphireServerPolicyLibrary {
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            return appObject.invoke(method, params);
        }

        @Deprecated
        public Policy.ServerPolicy sapphire_replicate(
                List<SapphirePolicyContainer> processedPolicies) throws RemoteException {
            return super.sapphire_replicate(processedPolicies, "");
        }

        @Override
        public Policy.ServerPolicy sapphire_replicate(
                List<SapphirePolicyContainer> processedPolicies, String region)
                throws RemoteException {
            return super.sapphire_replicate(processedPolicies, region);
        }

        /* This function is added here just to generate the stub for this function in all Policies server policy */
        @Override
        public void sapphire_pin_to_server(
                Policy.ServerPolicy serverPolicy, InetSocketAddress server)
                throws RemoteException, SapphireObjectNotFoundException,
                        SapphireObjectReplicaNotFoundException {
            super.sapphire_pin_to_server(serverPolicy, server);
        }

        @Override
        public void sapphire_remove_replica() throws RemoteException {
            super.sapphire_remove_replica(this.getProcessedPolicies());
        }

        public String sapphire_getRegion() {
            return super.sapphire_getRegion();
        }

        @Override
        public void onDestroy() {}
    }

    public abstract static class GroupPolicy extends SapphireGroupPolicyLibrary {

        /*
         * INTERNAL FUNCTIONS (Used by Sapphire runtime)
         */
        public void $__initialize(String appObjectClassName, ArrayList<Object> params) {
            this.appObjectClassName = appObjectClassName;
            this.params = params;
        }

        @Override
        public Policy.ServerPolicy onRefRequest() throws RemoteException {
            ArrayList<Policy.ServerPolicy> servers = getServers();
            // By default just return the primary/first replica.  All DM's have at least one.
            return servers.get(0);
        }

        @Override
        public void onDestroy() throws RemoteException {
            super.onDestroy();
        }
    }
}
