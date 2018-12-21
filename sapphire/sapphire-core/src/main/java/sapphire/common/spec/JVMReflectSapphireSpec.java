package sapphire.common.spec;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.harmony.rmi.common.RMIUtil;
import sapphire.app.DMSpec;
import sapphire.app.SapphireObjectSpec;
import sapphire.common.AppObjectStub;
import sapphire.compiler.GlobalStubConstants;

/**
 * Host Sapphire Object Specification.
 *
 * <p>Implementation of SapphireSpec for objects in languages that support java reflections.
 */
class JVMReflectSapphireSpec implements SapphireSpec {

    private Class<?> c;
    private Class<?> stubClass;
    private Map<String, Method> methodCache;
    private List<DMSpec> DmList;

    protected JVMReflectSapphireSpec(SapphireObjectSpec s) {
        if (s.getName() != null
                || s.getSourceFileLocation() != null
                || s.getConstructorName() != null) {
            throw new IllegalArgumentException(
                    "Too much data provided for host language sapphire object specification. Please remove name, soureceFileLocation and constructorName.");
        }

        try {
            c = Class.forName(s.getJavaClassName());

            String appStubClassName =
                    GlobalStubConstants.getAppPackageName(RMIUtil.getPackageName(c))
                            + "."
                            + RMIUtil.getShortName(c)
                            + GlobalStubConstants.STUB_SUFFIX;
            stubClass = Class.forName(appStubClassName);
        } catch (ClassNotFoundException ex) {
            throw new IllegalArgumentException(
                    "Initialized sapphire spec with bad class name " + s.getJavaClassName());
        }
        // build cache
        for (Method m : c.getDeclaredMethods()) {
            methodCache.put(m.getName(), m);
        }
        DmList = new ArrayList<DMSpec>(s.getDmList());
    }

    public Object construct(Object[] params)
            throws NoSuchMethodException, InstantiationException, InvocationTargetException,
                    IllegalAccessException {
        // TODO should probably cache these too
        Class<?>[] types = new Class<?>[params.length];
        for (int i = 0; i < params.length; i++) {
            types[i] = params[i].getClass();
        }

        Constructor<?> cons = c.getConstructor(types);

        return cons.newInstance(params);
    }

    public Object construct()
            throws NoSuchMethodException, InstantiationException, InvocationTargetException,
                    IllegalAccessException {
        // TODO should probably cache these too
        Constructor<?> cons = c.getConstructor();
        return cons.newInstance();
    }

    public Object invoke(Object host, String method, Object[] params)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method m = methodCache.get(method);
        if (m == null) throw new NoSuchMethodException("Method " + method + " not found in " + c);
        return m.invoke(host, params);
    }

    public List<DMSpec> getDmList() {
        return new ArrayList<DMSpec>(DmList);
    }

    public AppObjectStub getStub() {
        AppObjectStub stub;
        try {
            stub = (AppObjectStub) stubClass.newInstance();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        stub.$__initialize(false);
        return stub;
    }

    public boolean isHostType() {
        return true;
    }
}
