/*
 * Stub for class sapphire.policy.checkpoint.explicitcheckpoint.ExplicitCheckpointPolicy.ServerPolicy
 * Generated by Sapphire Compiler (sc).
 */
package sapphire.policy.stubs;


public final class ExplicitCheckpointPolicy$ServerPolicy_Stub extends sapphire.policy.checkpoint.explicitcheckpoint.ExplicitCheckpointPolicy.ServerPolicy implements sapphire.kernel.common.KernelObjectStub {

    sapphire.kernel.common.KernelOID $__oid = null;
    java.net.InetSocketAddress $__hostname = null;

    public ExplicitCheckpointPolicy$ServerPolicy_Stub(sapphire.kernel.common.KernelOID oid) {
        this.$__oid = oid;
    }

    public sapphire.kernel.common.KernelOID $__getKernelOID() {
        return this.$__oid;
    }

    public java.net.InetSocketAddress $__getHostname() {
        return this.$__hostname;
    }

    public void $__updateHostname(java.net.InetSocketAddress hostname) {
        this.$__hostname = hostname;
    }

    public Object $__makeKernelRPC(java.lang.String method, java.util.ArrayList<Object> params) throws java.rmi.RemoteException, java.lang.Exception {
        sapphire.kernel.common.KernelRPC rpc = new sapphire.kernel.common.KernelRPC($__oid, method, params);
        try {
            return sapphire.kernel.common.GlobalKernelReferences.nodeServer.getKernelClient().makeKernelRPC(this, rpc);
        } catch (sapphire.kernel.common.KernelObjectNotFoundException e) {
            throw new java.rmi.RemoteException();
        }
    }

    @Override
    public boolean equals(Object obj) { 
        ExplicitCheckpointPolicy$ServerPolicy_Stub other = (ExplicitCheckpointPolicy$ServerPolicy_Stub) obj;
        if (! other.$__oid.equals($__oid))
            return false;
        return true;
    }
    @Override
    public int hashCode() { 
        return $__oid.getID();
    }


    // Implementation of saveCheckpoint()
    public void saveCheckpoint()
            throws java.lang.Exception {
        java.util.ArrayList<Object> $__params = new java.util.ArrayList<Object>();
        String $__method = "public synchronized void sapphire.policy.checkpoint.CheckpointPolicyBase$ServerPolicy.saveCheckpoint() throws java.lang.Exception";
        java.lang.Object $__result = null;
        try {
            $__result = $__makeKernelRPC($__method, $__params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Implementation of sapphire_replicate()
    public sapphire.policy.SapphirePolicy.SapphireServerPolicy sapphire_replicate() {
        java.util.ArrayList<Object> $__params = new java.util.ArrayList<Object>();
        String $__method = "public sapphire.policy.SapphirePolicy$SapphireServerPolicy sapphire.policy.DefaultSapphirePolicyUpcallImpl$DefaultSapphireServerPolicyUpcallImpl.sapphire_replicate()";
        java.lang.Object $__result = null;
        try {
            $__result = $__makeKernelRPC($__method, $__params);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ((sapphire.policy.SapphirePolicy.SapphireServerPolicy) $__result);
    }

    // Implementation of sapphire_pin_to_server(InetSocketAddress)
    public void sapphire_pin_to_server(java.net.InetSocketAddress $param_InetSocketAddress_1)
            throws java.rmi.RemoteException {
        java.util.ArrayList<Object> $__params = new java.util.ArrayList<Object>();
        String $__method = "public void sapphire.policy.DefaultSapphirePolicyUpcallImpl$DefaultSapphireServerPolicyUpcallImpl.sapphire_pin_to_server(java.net.InetSocketAddress) throws java.rmi.RemoteException";
        $__params.add($param_InetSocketAddress_1);
        java.lang.Object $__result = null;
        try {
            $__result = $__makeKernelRPC($__method, $__params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Implementation of sapphire_pin(String)
    public void sapphire_pin(java.lang.String $param_String_1)
            throws java.rmi.RemoteException {
        java.util.ArrayList<Object> $__params = new java.util.ArrayList<Object>();
        String $__method = "public void sapphire.policy.DefaultSapphirePolicyUpcallImpl$DefaultSapphireServerPolicyUpcallImpl.sapphire_pin(java.lang.String) throws java.rmi.RemoteException";
        $__params.add($param_String_1);
        java.lang.Object $__result = null;
        try {
            $__result = $__makeKernelRPC($__method, $__params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Implementation of restoreCheckpoint()
    public void restoreCheckpoint()
            throws java.lang.Exception {
        java.util.ArrayList<Object> $__params = new java.util.ArrayList<Object>();
        String $__method = "public synchronized void sapphire.policy.checkpoint.CheckpointPolicyBase$ServerPolicy.restoreCheckpoint() throws java.lang.Exception";
        java.lang.Object $__result = null;
        try {
            $__result = $__makeKernelRPC($__method, $__params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Implementation of onRPC(String, ArrayList)
    public java.lang.Object onRPC(java.lang.String $param_String_1, java.util.ArrayList $param_ArrayList_2)
            throws java.lang.Exception {
        java.util.ArrayList<Object> $__params = new java.util.ArrayList<Object>();
        String $__method = "public java.lang.Object sapphire.policy.checkpoint.explicitcheckpoint.ExplicitCheckpointPolicy$ServerPolicy.onRPC(java.lang.String,java.util.ArrayList<java.lang.Object>) throws java.lang.Exception";
        $__params.add($param_String_1);
        $__params.add($param_ArrayList_2);
        java.lang.Object $__result = null;
        try {
            $__result = $__makeKernelRPC($__method, $__params);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return $__result;
    }

    // Implementation of onMembershipChange()
    public void onMembershipChange() {
        java.util.ArrayList<Object> $__params = new java.util.ArrayList<Object>();
        String $__method = "public void sapphire.policy.DefaultSapphirePolicy$DefaultServerPolicy.onMembershipChange()";
        java.lang.Object $__result = null;
        try {
            $__result = $__makeKernelRPC($__method, $__params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Implementation of onCreate(SapphirePolicy.SapphireGroupPolicy)
    public void onCreate(sapphire.policy.SapphirePolicy.SapphireGroupPolicy $param_SapphirePolicy$SapphireGroupPolicy_1) {
        java.util.ArrayList<Object> $__params = new java.util.ArrayList<Object>();
        String $__method = "public void sapphire.policy.DefaultSapphirePolicy$DefaultServerPolicy.onCreate(sapphire.policy.SapphirePolicy$SapphireGroupPolicy)";
        $__params.add($param_SapphirePolicy$SapphireGroupPolicy_1);
        java.lang.Object $__result = null;
        try {
            $__result = $__makeKernelRPC($__method, $__params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Implementation of getGroup()
    public sapphire.policy.SapphirePolicy.SapphireGroupPolicy getGroup() {
        java.util.ArrayList<Object> $__params = new java.util.ArrayList<Object>();
        String $__method = "public sapphire.policy.SapphirePolicy$SapphireGroupPolicy sapphire.policy.DefaultSapphirePolicy$DefaultServerPolicy.getGroup()";
        java.lang.Object $__result = null;
        try {
            $__result = $__makeKernelRPC($__method, $__params);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ((sapphire.policy.SapphirePolicy.SapphireGroupPolicy) $__result);
    }

    // Implementation of deleteCheckpoint()
    public boolean deleteCheckpoint() {
        java.util.ArrayList<Object> $__params = new java.util.ArrayList<Object>();
        String $__method = "public synchronized boolean sapphire.policy.checkpoint.CheckpointPolicyBase$ServerPolicy.deleteCheckpoint()";
        java.lang.Object $__result = null;
        try {
            $__result = $__makeKernelRPC($__method, $__params);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ((java.lang.Boolean) $__result).booleanValue();
    }
}
