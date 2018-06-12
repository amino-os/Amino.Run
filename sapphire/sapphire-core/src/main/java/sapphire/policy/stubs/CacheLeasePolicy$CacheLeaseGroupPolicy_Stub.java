/*
 * Stub for class sapphire.policy.cache.CacheLeasePolicy.CacheLeaseGroupPolicy
 * Generated by Sapphire Compiler (sc).
 */
package sapphire.policy.stubs;


public final class CacheLeasePolicy$CacheLeaseGroupPolicy_Stub extends sapphire.policy.cache.CacheLeasePolicy.CacheLeaseGroupPolicy implements sapphire.kernel.common.KernelObjectStub {

    sapphire.kernel.common.KernelOID $__oid = null;
    java.net.InetSocketAddress $__hostname = null;

    public CacheLeasePolicy$CacheLeaseGroupPolicy_Stub(sapphire.kernel.common.KernelOID oid) {
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
        CacheLeasePolicy$CacheLeaseGroupPolicy_Stub other = (CacheLeasePolicy$CacheLeaseGroupPolicy_Stub) obj;
        if (! other.$__oid.equals($__oid))
            return false;
        return true;
    }
    @Override
    public int hashCode() { 
        return $__oid.getID();
    }


    // Implementation of removeServer(SapphirePolicy.SapphireServerPolicy)
    public void removeServer(sapphire.policy.SapphirePolicy.SapphireServerPolicy $param_SapphirePolicy$SapphireServerPolicy_1) {
        java.util.ArrayList<Object> $__params = new java.util.ArrayList<Object>();
        String $__method = "public void sapphire.policy.cache.CacheLeasePolicy$CacheLeaseGroupPolicy.removeServer(sapphire.policy.SapphirePolicy$SapphireServerPolicy)";
        $__params.add($param_SapphirePolicy$SapphireServerPolicy_1);
        java.lang.Object $__result = null;
        try {
            $__result = $__makeKernelRPC($__method, $__params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Implementation of onRefRequest()
    public sapphire.policy.SapphirePolicy.SapphireServerPolicy onRefRequest() {
        java.util.ArrayList<Object> $__params = new java.util.ArrayList<Object>();
        String $__method = "public sapphire.policy.SapphirePolicy$SapphireServerPolicy sapphire.policy.cache.CacheLeasePolicy$CacheLeaseGroupPolicy.onRefRequest()";
        java.lang.Object $__result = null;
        try {
            $__result = $__makeKernelRPC($__method, $__params);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ((sapphire.policy.SapphirePolicy.SapphireServerPolicy) $__result);
    }

    // Implementation of onFailure(SapphirePolicy.SapphireServerPolicy)
    public void onFailure(sapphire.policy.SapphirePolicy.SapphireServerPolicy $param_SapphirePolicy$SapphireServerPolicy_1) {
        java.util.ArrayList<Object> $__params = new java.util.ArrayList<Object>();
        String $__method = "public void sapphire.policy.cache.CacheLeasePolicy$CacheLeaseGroupPolicy.onFailure(sapphire.policy.SapphirePolicy$SapphireServerPolicy)";
        $__params.add($param_SapphirePolicy$SapphireServerPolicy_1);
        java.lang.Object $__result = null;
        try {
            $__result = $__makeKernelRPC($__method, $__params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Implementation of onCreate(SapphirePolicy.SapphireServerPolicy)
    public void onCreate(sapphire.policy.SapphirePolicy.SapphireServerPolicy $param_SapphirePolicy$SapphireServerPolicy_1) {
        java.util.ArrayList<Object> $__params = new java.util.ArrayList<Object>();
        String $__method = "public void sapphire.policy.cache.CacheLeasePolicy$CacheLeaseGroupPolicy.onCreate(sapphire.policy.SapphirePolicy$SapphireServerPolicy)";
        $__params.add($param_SapphirePolicy$SapphireServerPolicy_1);
        java.lang.Object $__result = null;
        try {
            $__result = $__makeKernelRPC($__method, $__params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Implementation of getServers()
    public java.util.ArrayList getServers() {
        java.util.ArrayList<Object> $__params = new java.util.ArrayList<Object>();
        String $__method = "public java.util.ArrayList<sapphire.policy.SapphirePolicy$SapphireServerPolicy> sapphire.policy.cache.CacheLeasePolicy$CacheLeaseGroupPolicy.getServers()";
        java.lang.Object $__result = null;
        try {
            $__result = $__makeKernelRPC($__method, $__params);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ((java.util.ArrayList) $__result);
    }

    // Implementation of addServer(SapphirePolicy.SapphireServerPolicy)
    public void addServer(sapphire.policy.SapphirePolicy.SapphireServerPolicy $param_SapphirePolicy$SapphireServerPolicy_1) {
        java.util.ArrayList<Object> $__params = new java.util.ArrayList<Object>();
        String $__method = "public void sapphire.policy.cache.CacheLeasePolicy$CacheLeaseGroupPolicy.addServer(sapphire.policy.SapphirePolicy$SapphireServerPolicy)";
        $__params.add($param_SapphirePolicy$SapphireServerPolicy_1);
        java.lang.Object $__result = null;
        try {
            $__result = $__makeKernelRPC($__method, $__params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Implementation of $__initialize(String, ArrayList)
    public void $__initialize(java.lang.String $param_String_1, java.util.ArrayList $param_ArrayList_2) {
        java.util.ArrayList<Object> $__params = new java.util.ArrayList<Object>();
        String $__method = "public void sapphire.policy.DefaultSapphirePolicyUpcallImpl$DefaultSapphireGroupPolicyUpcallImpl.$__initialize(java.lang.String,java.util.ArrayList<java.lang.Object>)";
        $__params.add($param_String_1);
        $__params.add($param_ArrayList_2);
        java.lang.Object $__result = null;
        try {
            $__result = $__makeKernelRPC($__method, $__params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
