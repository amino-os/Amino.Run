package sapphire.policy;

import java.lang.annotation.Annotation;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.UUID;
import sapphire.common.AppObject;
import sapphire.common.SapphireObjectNotFoundException;
import sapphire.common.SapphireObjectReplicaNotFoundException;
import sapphire.policy.SapphirePolicy.SapphireServerPolicy;
import sapphire.policy.transaction.IllegalComponentException;
import sapphire.policy.transaction.TransactionContext;
import sapphire.policy.transaction.TwoPCClient;

public abstract class DefaultSapphirePolicyUpcallImpl extends SapphirePolicyLibrary {

    public abstract static class DefaultSapphireClientPolicyUpcallImpl
            extends SapphireClientPolicyLibrary {
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

    public abstract static class DefaultSapphireServerPolicyUpcallImpl
            extends SapphireServerPolicyLibrary {
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            /* The default behavior is to just invoke the method on the Sapphire Object this Server Policy Object manages */
            return appObject.invoke(method, params);
        }
        /* This function is added here just to generate the stub for this function in all DMs server policy */
        public SapphireServerPolicy sapphire_replicate() throws RemoteException {
            return super.sapphire_replicate();
        }
        /* This function is added here just to generate the stub for this function in all DMs server policy */
        public void sapphire_pin(String region)
                throws RemoteException, SapphireObjectNotFoundException,
                        SapphireObjectReplicaNotFoundException {
            super.sapphire_pin(region);
        }
        /* This function is added here just to generate the stub for this function in all DMs server policy */
        public void sapphire_pin_to_server(InetSocketAddress server)
                throws RemoteException, SapphireObjectNotFoundException,
                        SapphireObjectReplicaNotFoundException {
            super.sapphire_pin_to_server(server);
        }

        public void sapphire_remove_replica() throws RemoteException {
            super.sapphire_remove_replica();
        }

        public AppObject sapphire_getRemoteAppObject() throws RemoteException {
            return super.sapphire_getAppObject();
        }

        public String sapphire_getRegion() {
            return super.sapphire_getRegion();
        }

        public void onDestroy() {}
    }

    public abstract static class DefaultSapphireGroupPolicyUpcallImpl
            extends SapphireGroupPolicyLibrary {

        /*
         * INTERNAL FUNCTIONS (Used by Sapphire runtime)
         */
        public void $__initialize(String appObjectClassName, ArrayList<Object> params) {
            this.appObjectClassName = appObjectClassName;
            this.params = params;
        }

        public Annotation[] getAppConfigAnnotation() {
            return super.getAppConfigAnnotation();
        }

        public SapphireServerPolicy onRefRequest() throws RemoteException {
            ArrayList<SapphireServerPolicy> servers = getServers();
            return servers.get(0);
        }

        public void onDestroy() throws RemoteException {
            super.onDestroy();
        }
    }
}
