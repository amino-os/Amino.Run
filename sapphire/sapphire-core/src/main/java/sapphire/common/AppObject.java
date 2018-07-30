package sapphire.common;

import java.util.ArrayList;
import sapphire.kernel.common.GlobalKernelReferences;

public class AppObject extends ObjectHandler {
    private String runtime;
    private SapphireReplicaID sapphireReplicaId;

    @Override
    protected Class<?> getClass(Object obj) {
        return obj.getClass().getSuperclass();
    }

    public AppObject(Object obj) {
        super(obj);
    }

    public AppObject(Object obj, String runtime, SapphireReplicaID sapphireReplicaId) {
        super(obj);
        this.runtime = runtime;
        this.sapphireReplicaId = sapphireReplicaId;
    }

    public String getRuntime() {
        return runtime;
    }

    public SapphireReplicaID getSapphireReplicaId() {
        return sapphireReplicaId;
    }

    public Object invoke(String method, ArrayList<Object> params) throws Exception {
        if (null != getRuntime()) {
            if (getRuntime().equalsIgnoreCase("java")) {
                return GlobalKernelReferences.nodeServer
                        .getJavaGrpcClient()
                        .rpcInvoke(getSapphireReplicaId(), method, params);
            } else if (getRuntime().equalsIgnoreCase("go")) {
                // TODO: Need to call go runtime
                return null;
            } else {
                return null;
            }
        } else {
            /* local invocation */
            return super.invoke(method, params);
        }
    }
}
