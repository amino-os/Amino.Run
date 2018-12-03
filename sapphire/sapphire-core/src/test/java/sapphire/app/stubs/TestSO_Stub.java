package sapphire.app.stubs;

public final class TestSO_Stub extends sapphire.app.TestSO
        implements sapphire.common.AppObjectStub {

    sapphire.policy.SapphirePolicy.SapphireClientPolicy $__client = null;
    boolean $__directInvocation = false;

    public TestSO_Stub() {
        super();
    }

    public void $__initialize(sapphire.policy.SapphirePolicy.SapphireClientPolicy client) {
        $__client = client;
    }

    public void $__initialize(boolean directInvocation) {
        $__directInvocation = directInvocation;
    }

    public Object $__clone() throws CloneNotSupportedException {
        return super.clone();
    }

    // Implementation of incVal()
    public void incVal() {
        Object $__result = null;
        if ($__directInvocation) {
            try {
                super.incVal();
            } catch (Exception e) {
                throw new sapphire.common.AppExceptionWrapper(e);
            }
        } else {
            java.util.ArrayList<Object> $__params = new java.util.ArrayList<Object>();
            String $__method = "public void sapphire.app.TestSO.incVal()";
            try {
                $__result = $__client.onRPC($__method, $__params);
            } catch (sapphire.common.AppExceptionWrapper e) {
                Exception ex = e.getException();
                if (ex instanceof RuntimeException) {
                    throw (RuntimeException) ex;
                } else {
                    throw new RuntimeException(ex);
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    // Implementation of getVal()
    public Integer getVal() {
        Object $__result = null;
        if ($__directInvocation) {
            try {
                $__result = super.getVal();
            } catch (Exception e) {
                throw new sapphire.common.AppExceptionWrapper(e);
            }
        } else {
            java.util.ArrayList<Object> $__params = new java.util.ArrayList<Object>();
            String $__method = "public java.lang.Integer sapphire.app.TestSO.getVal()";
            try {
                $__result = $__client.onRPC($__method, $__params);
            } catch (sapphire.common.AppExceptionWrapper e) {
                Exception ex = e.getException();
                if (ex instanceof RuntimeException) {
                    throw (RuntimeException) ex;
                } else {
                    throw new RuntimeException(ex);
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return ((Integer) $__result);
    }

    // Implementation of decVal()
    public void decVal() {
        Object $__result = null;
        if ($__directInvocation) {
            try {
                super.decVal();
            } catch (Exception e) {
                throw new sapphire.common.AppExceptionWrapper(e);
            }
        } else {
            java.util.ArrayList<Object> $__params = new java.util.ArrayList<Object>();
            String $__method = "public void sapphire.app.TestSO.decVal()";
            try {
                $__result = $__client.onRPC($__method, $__params);
            } catch (sapphire.common.AppExceptionWrapper e) {
                Exception ex = e.getException();
                if (ex instanceof RuntimeException) {
                    throw (RuntimeException) ex;
                } else {
                    throw new RuntimeException(ex);
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
