package amino.run.kernel.common;

import amino.run.graal.io.*;
import com.google.common.base.Objects;
import java.io.Serializable;
import java.util.*;
import org.graalvm.polyglot.*;

/**
 * MicroService Kernel RPC Includes the object being called, the method and the parameters
 *
 * @author iyzhang
 */
public class KernelRPC implements Serializable {
    private KernelOID oid;
    private String method;
    private ArrayList<Object> params;

    public KernelRPC(KernelOID oid, String method, ArrayList<Object> params) throws Exception {
        this.oid = oid;
        this.method = method;
        this.params = params;
    }

    public KernelOID getOID() {
        return oid;
    }

    public String getMethod() {
        return method;
    }

    public ArrayList<Object> getParams() {
        return params;
    }

    @Override
    public String toString() {
        String ret = method;
        if (params.size() > 0) {
            ret += "(";
            Iterator<Object> it = params.iterator();
            for (Object obj = it.next(); it.hasNext(); obj = it.next()) {
                if (it.hasNext()) {
                    ret = ret + obj.toString() + ",";
                } else {
                    ret = ret + obj.toString() + ")";
                }
            }
        }
        return ret;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KernelRPC kernelRPC = (KernelRPC) o;
        return Objects.equal(oid, kernelRPC.oid)
                && Objects.equal(method, kernelRPC.method)
                && Objects.equal(params, kernelRPC.params);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(oid, method, params);
    }
}
