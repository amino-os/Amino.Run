package sapphire.demo.stubs;

public final class JSKeyValueStore_Stub extends sapphire.common.GraalObject implements sapphire.common.AppObjectStub {

    sapphire.policy.SapphirePolicy.SapphireClientPolicy $__client = null;
    boolean $__directInvocation = false;

    public JSKeyValueStore_Stub () {super();}

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

    public java.lang.Object get(Object... args) {
        java.lang.Object $__result = null;
        if ($__directInvocation) {
            try {
                java.util.ArrayList<Object> objs = new java.util.ArrayList<>();
                objs.addAll(java.util.Arrays.asList(args));
                $__result = super.invoke("get", objs);
            } catch (java.lang.Exception e) {
                throw new sapphire.common.AppExceptionWrapper(e);
            }
        } else {
            java.util.ArrayList<Object> $__params = new java.util.ArrayList<Object>();
            String $__method = "public java.lang.Object sapphire.demo.stubs.JSKeyValueStore_Stub.get(java.lang.Object...)";
            $__params.addAll(java.util.Arrays.asList(args));
            try {
                $__result = $__client.onRPC($__method, $__params);
                if ($__result instanceof sapphire.graal.io.SerializeValue) {
                    $__result = deserializedSerializeValue((sapphire.graal.io.SerializeValue)$__result);
                    $__result = ((org.graalvm.polyglot.Value)$__result).as(java.lang.Object.class);
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

        return ($__result);
    }

    public java.lang.Object set(Object... args) {
        java.lang.Object $__result = null;
        if ($__directInvocation) {
            try {
                java.util.ArrayList<Object> objs = new java.util.ArrayList<>();
                objs.addAll(java.util.Arrays.asList(args));
                $__result = super.invoke("set", objs);
            } catch (java.lang.Exception e) {
                throw new sapphire.common.AppExceptionWrapper(e);
            }
        } else {
            java.util.ArrayList<Object> $__params = new java.util.ArrayList<Object>();
            String $__method = "public java.lang.Object sapphire.demo.stubs.JSKeyValueStore_Stub.set(java.lang.Object...)";
            $__params.addAll(java.util.Arrays.asList(args));
            try {
                $__result = $__client.onRPC($__method, $__params);
                if ($__result instanceof sapphire.graal.io.SerializeValue) {
                    $__result = deserializedSerializeValue((sapphire.graal.io.SerializeValue)$__result);
                    $__result = ((org.graalvm.polyglot.Value)$__result).as(java.lang.Object.class);
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

        return $__result;
    }

    public java.lang.Object contains(Object... args) {
        java.lang.Object $__result = null;
        if ($__directInvocation) {
            try {
                java.util.ArrayList<Object> objs = new java.util.ArrayList<>();
                objs.addAll(java.util.Arrays.asList(args));
                $__result = super.invoke("contains", objs);
            } catch (java.lang.Exception e) {
                throw new sapphire.common.AppExceptionWrapper(e);
            }
        } else {
            java.util.ArrayList<Object> $__params = new java.util.ArrayList<Object>();
            String $__method = "public java.lang.Object sapphire.demo.stubs.JSKeyValueStore_Stub.contains(java.lang.Object...)";
            $__params.addAll(java.util.Arrays.asList(args));
            try {
                $__result = $__client.onRPC($__method, $__params);
                if ($__result instanceof sapphire.graal.io.SerializeValue) {
                    $__result = deserializedSerializeValue((sapphire.graal.io.SerializeValue)$__result);
                    $__result = ((org.graalvm.polyglot.Value)$__result).as(java.lang.Object.class);
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

        return $__result;
    }
}
