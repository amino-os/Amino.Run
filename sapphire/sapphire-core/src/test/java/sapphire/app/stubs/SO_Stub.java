package sapphire.app.stubs;

import sapphire.app.SO;
import sapphire.common.AppObject;
import sapphire.policy.SapphirePolicy;

/** Created by Venugopal Reddy K on 6/9/18. */
public class SO_Stub extends SO implements sapphire.common.AppObjectStub {

    sapphire.policy.SapphirePolicy.SapphireClientPolicy $__client = null;
    boolean $__directInvocation = false;
    AppObject $__appObject = null;

    public SO_Stub() {
        super();
    }

    @Override
    public void $__initialize(SapphirePolicy.SapphireClientPolicy client) {
        $__client = client;
    }

    @Override
    public void $__initialize(boolean directInvocation) {
        $__directInvocation = directInvocation;
    }

    @Override
    public Object $__clone() throws CloneNotSupportedException {
        return super.clone();
    }

    // Implementation of getI()
    public Integer getI() {
        java.lang.Object $__result = null;
        if ($__directInvocation) {
            try {
                $__result = super.getI();
            } catch (java.lang.Exception e) {
                throw new sapphire.common.AppExceptionWrapper(e);
            }
        } else {
            java.util.ArrayList<Object> $__params = new java.util.ArrayList<Object>();
            String $__method = "public java.lang.Integer sapphire.app.SO.getI()";
            try {
                $__result = $__client.onRPC($__method, $__params);
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
        return ((Integer) $__result);
    }

    // Implementation of setI(Integer)
    public void setI(Integer $param_String_1) {
        java.lang.Object $__result = null;
        if ($__directInvocation) {
            try {
                super.setI($param_String_1);
            } catch (java.lang.Exception e) {
                throw new sapphire.common.AppExceptionWrapper(e);
            }
        } else {
            java.util.ArrayList<Object> $__params = new java.util.ArrayList<Object>();
            String $__method = "public void sapphire.app.SO.setI(java.lang.Integer)";
            $__params.add($param_String_1);
            try {
                $__result = $__client.onRPC($__method, $__params);
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
        return;
    }

    // Implementation of getIDelayed()
    public Integer getIDelayed() {
        java.lang.Object $__result = null;
        if ($__directInvocation) {
            try {
                $__result = super.getIDelayed();
            } catch (java.lang.Exception e) {
                throw new sapphire.common.AppExceptionWrapper(e);
            }
        } else {
            java.util.ArrayList<Object> $__params = new java.util.ArrayList<Object>();
            String $__method = "public java.lang.Integer sapphire.app.SO.getIDelayed()";
            try {
                $__result = $__client.onRPC($__method, $__params);
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
        return ((Integer) $__result);
    }
}
