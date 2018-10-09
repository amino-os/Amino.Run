package sapphire.policy;

import java.lang.annotation.Annotation;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import sapphire.common.SapphireObjectNotFoundException;
import sapphire.common.SapphireObjectReplicaNotFoundException;
import sapphire.policy.transaction.IllegalComponentException;
import sapphire.policy.transaction.TransactionContext;
import sapphire.policy.transaction.TwoPCClient;

/**
 * Class that describes how Sapphire Policies look like. Each policy should extend this class. Each
 * Sapphire Policy contains a Server Policy, a Client Policy and a Group Policy. The Policies
 * contain a set of internal functions (used by the sapphire runtime system), a set of upcall
 * functions that are called when an event happened and a set of functions that implement the
 * sapphire API for policies.
 */
public abstract class SapphirePolicy extends SapphirePolicyLibrary {

    public abstract static class SapphireClientPolicy extends SapphireClientPolicyLibrary {
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

    public abstract static class SapphireServerPolicy extends SapphireServerPolicyLibrary {
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            if (nextServerKernelObject == null) {
                /* The default behavior is to just invoke the method on the Sapphire Object this Server Policy Object manages */
                return appObject.invoke(method, params);
            } else {
                return nextServerKernelObject.invoke(method, params);
            }
        }

        public SapphireServerPolicy sapphire_replicate(
                List<SapphirePolicyContainer> processedPolicies) throws RemoteException {
            return super.sapphire_replicate(processedPolicies);
        }

        /* This function is added here just to generate the stub for this function in all Policies server policy */
        public void sapphire_pin(String region)
                throws RemoteException, SapphireObjectNotFoundException,
                        SapphireObjectReplicaNotFoundException {
            super.sapphire_pin(region);
        }
        /* This function is added here just to generate the stub for this function in all Policies server policy */
        public void sapphire_pin_to_server(InetSocketAddress server)
                throws RemoteException, SapphireObjectNotFoundException,
                        SapphireObjectReplicaNotFoundException {
            super.sapphire_pin_to_server(server);
        }

        /* This function is added here just to generate the stub for this function in all Policies server policy */
        public void sapphire_pin_to_server(
                SapphireServerPolicy sapphireServerPolicy, InetSocketAddress server)
                throws RemoteException, SapphireObjectNotFoundException,
                        SapphireObjectReplicaNotFoundException {
            super.sapphire_pin_to_server(sapphireServerPolicy, server);
        }

        public void sapphire_remove_replica() throws RemoteException {
            super.sapphire_remove_replica();
        }

        public String sapphire_getRegion() {
            return super.sapphire_getRegion();
        }

        public void onDestroy() {}
    }

    public abstract static class SapphireGroupPolicy extends SapphireGroupPolicyLibrary {

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
