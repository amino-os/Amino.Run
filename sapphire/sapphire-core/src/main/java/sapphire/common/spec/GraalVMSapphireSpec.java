package sapphire.common.spec;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import sapphire.app.DMSpec;
import sapphire.app.Language;
import sapphire.app.SapphireObjectSpec;
import sapphire.common.AppObjectStub;
import sapphire.graal.io.GraalContext;

/**
 * Host Sapphire Object Specification.
 *
 * <p>Implementation of SapphireSpec for objects from languages managed by GraalVM
 */
class GraalVMSapphireSpec implements SapphireSpec {

    private Language lang;
    private String name;
    private String sourceFileLocation;
    private String constructorName;
    private transient Value metaValue;
    private String javaClassName;
    private List<DMSpec> DmList;

    protected GraalVMSapphireSpec(SapphireObjectSpec s) throws IOException {
        lang = s.getLang();
        name = s.getName();
        sourceFileLocation = s.getSourceFileLocation();

        // Evaluate class definition
        Source source = Source.newBuilder(lang.toString(), new File(sourceFileLocation)).build();
        GraalContext.getContext().eval(source);

        constructorName = s.getConstructorName();
        findConstructor();

        javaClassName = s.getJavaClassName();
        DmList = s.getDmList();
    }

    private void findConstructor() {
        Context c;
        try {
            c = GraalContext.getContext();
        } catch (Exception ex) {
            throw new RuntimeException("Couldn't find graal context");
            // TODO we should handle this better
        }
        // TODO we should allow names deeper in namespace
        metaValue = c.getBindings(lang.toString()).getMember(name);
    }

    public Object construct(Object[] params) throws InvocationTargetException {
        if (metaValue == null) findConstructor();
        try {
            return metaValue.newInstance(params);
        } catch (Exception ex) {
            // TODO should break out the graal errors to the appropriate exception types (applicable
            // elsewhere in file)
            throw new InvocationTargetException(ex);
        }
    }

    public Object construct() throws InvocationTargetException {
        return construct(new Object[0]);
    }

    public Object invoke(Object host, String method, Object[] params)
            throws NoSuchMethodException, InvocationTargetException {
        Value v = (Value) host;
        Value m = v.getMember(method);
        if (m == null || !m.canExecute())
            throw new NoSuchMethodException("Method " + method + " not found in " + name);
        try {
            return m.execute(params);
        } catch (Exception ex) {
            throw new InvocationTargetException(ex);
        }
    }

    public List<DMSpec> getDmList() {
        return new ArrayList<DMSpec>(DmList);
    }

    public AppObjectStub getStub() {
        // TODO: we should fix the relationship between the class and it's stub
        // as we are generating it (or build it into the java spec, and stub generator)
        try {
            Class<?> stubClass = Class.forName(javaClassName);
            // TODO: Currently all polyglot application stub should have default
            return (AppObjectStub) stubClass.newInstance();
        } catch (Exception ex) {
            // TODO we can do better error reporting about failing to make stub
            throw new RuntimeException(ex);
        }
    }

    public boolean isHostType() {
        return false;
    }
}
