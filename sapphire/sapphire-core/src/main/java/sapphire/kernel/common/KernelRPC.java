package sapphire.kernel.common;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.*;
import org.graalvm.polyglot.*;
import sapphire.app.Language;
import sapphire.graal.io.*;

/**
 * Sapphire Kernel RPC Includes the object being called, the method and the parameters
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
        if (params.size() > 0 && params.get(0) instanceof Language) {
            this.params.add(params.get(0));
            for (int i = 1; i < params.size(); ++i) {
                Object p = params.get(i);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                sapphire.graal.io.Serializer serializer =
                        new Serializer(out, (Language) params.get(0));
                serializer.serialize((Value) p);
                this.params.add(out.toByteArray());
            }
        } else {
            this.params = params;
        }
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
        return Objects.equals(oid, kernelRPC.oid)
                && Objects.equals(method, kernelRPC.method)
                && Objects.equals(params, kernelRPC.params);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oid, method, params);
    }
}
