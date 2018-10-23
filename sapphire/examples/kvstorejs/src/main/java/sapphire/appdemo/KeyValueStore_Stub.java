package sapphire.appdemo;

import org.graalvm.polyglot.Value;
import jdk.nashorn.internal.runtime.regexp.joni.exception.SyntaxException;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.List;

import sapphire.app.SapphireObjectSpec;
import sapphire.common.SapphireObjectID;
import sapphire.graal.io.SerializeValue;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServer;
public final class KeyValueStore_Stub extends sapphire.common.GraalObject implements sapphire.common.AppObjectStub {

    sapphire.policy.SapphirePolicy.SapphireClientPolicy $__client = null;
    boolean $__directInvocation = false;

    public static KeyValueStore_Stub getStub(String specYamlFile, String omsIP, String omsPort) throws Exception {
        String spec = getSpec(specYamlFile);
        Registry registry = LocateRegistry.getRegistry(omsIP, Integer.parseInt(omsPort));
        new KernelServerImpl(new InetSocketAddress("127.0.0.2", 0), new InetSocketAddress(omsIP, Integer.parseInt(omsPort)));
        OMSServer oms = (OMSServer) registry.lookup("SapphireOMS");

        SapphireObjectID oid = oms.createSapphireObject(spec);
        KeyValueStore_Stub store = (KeyValueStore_Stub)oms.acquireSapphireObjectStub(oid);
        store.$__initializeGraal(SapphireObjectSpec.fromYaml(spec));

        return store;
    }

    private static String getSpec(String specYamlFile) throws Exception {
        ClassLoader classLoader = new KeyValueStoreClient().getClass().getClassLoader();
        File file = new File(classLoader.getResource(specYamlFile).getFile());
        List<String> lines = Files.readAllLines(file.toPath());
        return String.join("\n", lines);
    }

    public KeyValueStore_Stub () {super();}

    public void $__initialize(sapphire.policy.SapphirePolicy.SapphireClientPolicy client) {
        $__client = client;
    }

    public void $__initialize(boolean directInvocation) {
        $__directInvocation = directInvocation;
    }

    public Object $__clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public void $__initializeGraal(sapphire.app.SapphireObjectSpec spec, java.lang.Object[] params){
        try {
            super.$__initializeGraal(spec, params);
        } catch (java.lang.Exception e) {
            throw new java.lang.RuntimeException(e);
        }

    }

    public java.lang.Object set(Object... args) {
        java.lang.Object $__result = null;
        if ($__directInvocation) {
            try {
                java.util.ArrayList<Object> objs = new java.util.ArrayList<>();
                objs.addAll(Arrays.asList(args));
                $__result = super.invoke("set", objs);
            } catch (java.lang.Exception e) {
                throw new sapphire.common.AppExceptionWrapper(e);
            }
        } else {
            java.util.ArrayList<Object> $__params = new java.util.ArrayList<Object>();
            String $__method = "public java.lang.Object sapphire.appdemo.KeyValueStore_Stub.set(java.lang.Object...)";
            $__params.addAll(Arrays.asList(args));
            try {
                $__result = $__client.onRPC($__method, $__params);
                if ($__result instanceof SerializeValue) {
                    $__result = deserializedSerializeValue((SerializeValue)$__result);
                    $__result = ((org.graalvm.polyglot.Value)$__result).as(java.lang.Object.class);
                    System.out.println($__result);
                }
            } catch (sapphire.common.AppExceptionWrapper e) {
                Exception ex = e.getException();
                if (ex instanceof java.lang.RuntimeException) {
                    throw (java.lang.RuntimeException) ex;
                } else {
                    throw new java.lang.RuntimeException(ex);
                }
            } catch (java.lang.Exception e) {
                throw new java.lang.RuntimeException(e);
            }
        }

        System.out.println($__result);
        return ($__result);
    }

    public java.lang.Object get(Object... args) {
        java.lang.Object $__result = null;
        if ($__directInvocation) {
            try {
                java.util.ArrayList<Object> objs = new java.util.ArrayList<>();
                objs.addAll(Arrays.asList(args));
                $__result = super.invoke("get", objs);
            } catch (java.lang.Exception e) {
                throw new sapphire.common.AppExceptionWrapper(e);
            }
        } else {
            java.util.ArrayList<Object> $__params = new java.util.ArrayList<Object>();
            String $__method = "public java.lang.Object sapphire.appdemo.KeyValueStore_Stub.get(java.lang.Object...)";
            $__params.addAll(Arrays.asList(args));
            try {
                $__result = $__client.onRPC($__method, $__params);
                if ($__result instanceof SerializeValue) {
                    $__result = deserializedSerializeValue((SerializeValue)$__result);
                    $__result = ((org.graalvm.polyglot.Value)$__result).as(java.lang.Object.class);
                    System.out.println($__result);
                }
            } catch (sapphire.common.AppExceptionWrapper e) {
                Exception ex = e.getException();
                if (ex instanceof java.lang.RuntimeException) {
                    throw (java.lang.RuntimeException) ex;
                } else {
                    throw new java.lang.RuntimeException(ex);
                }
            } catch (java.lang.Exception e) {
                throw new java.lang.RuntimeException(e);
            }
        }

        System.out.println($__result);
        return ($__result);
    }

    public java.lang.Object contains(Object... args) {
        java.lang.Object $__result = null;
        if ($__directInvocation) {
            try {
                java.util.ArrayList<Object> objs = new java.util.ArrayList<>();
                objs.addAll(Arrays.asList(args));
                $__result = super.invoke("contains", objs);
            } catch (java.lang.Exception e) {
                throw new sapphire.common.AppExceptionWrapper(e);
            }
        } else {
            java.util.ArrayList<Object> $__params = new java.util.ArrayList<Object>();
            String $__method = "public java.lang.Object sapphire.appdemo.KeyValueStore_Stub.contains(java.lang.Object...)";
            $__params.addAll(Arrays.asList(args));
            try {
                $__result = $__client.onRPC($__method, $__params);
                if ($__result instanceof SerializeValue) {
                    $__result = deserializedSerializeValue((SerializeValue)$__result);
                    $__result = ((org.graalvm.polyglot.Value)$__result).as(java.lang.Object.class);
                    System.out.println($__result);
                }
            } catch (sapphire.common.AppExceptionWrapper e) {
                Exception ex = e.getException();
                if (ex instanceof java.lang.RuntimeException) {
                    throw (java.lang.RuntimeException) ex;
                } else {
                    throw new java.lang.RuntimeException(ex);
                }
            } catch (java.lang.Exception e) {
                throw new java.lang.RuntimeException(e);
            }
        }

        System.out.println($__result);
        return ($__result);
    }

 }
