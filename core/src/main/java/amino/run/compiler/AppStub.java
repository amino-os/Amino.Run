package amino.run.compiler;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.TreeSet;
import org.apache.harmony.rmi.common.RMIUtil;
import org.apache.harmony.rmi.compiler.RmicUtil;

/**
 * Generates stub code for a given App Object class.
 *
 * @author aaasz
 */
public final class AppStub extends Stub {

    public AppStub(Class<?> cls) throws ClassNotFoundException {
        super(cls);
    }

    @Override
    public TreeSet<MethodStub> getMethods() {
        TreeSet<MethodStub> ms = new TreeSet<MethodStub>();

        for (Method m : stubClass.getDeclaredMethods()) {
            // Add public methods to methods vector
            if (Modifier.isPublic(m.getModifiers())) {
                ms.add(new MethodStub((Method) m));
            }
        }
        return ms;
    }

    @Override
    public String getPackageStatement() {
        return ((packageName == null)
                ? "" //$NON-NLS-1$
                : ("package "
                        + GlobalStubConstants.getAppPackageName(packageName)
                        + ";"
                        + EOLN
                        + EOLN)); //$NON-NLS-1$
    }

    @Override
    public String getImportStatement() {
        // Import statement is part of getStubSource in parent Stub class.
        // While PolicyStub needs to import additional classes, AppStub doesn't.
        return "";
    }

    @Override
    public String getStubClassDeclaration() {
        StringBuilder buffer = new StringBuilder("");
        buffer.append("public final class " + stubName + " extends " + className + " implements ");
        buffer.append("amino.run.common.AppObjectStub"); // $NON-NLS-1$
        buffer.append(" {" + EOLN + EOLN); // $NON-NLS-1$
        return buffer.toString();
    }

    @Override
    public String getStubFields() {
        StringBuilder buffer = new StringBuilder();

        buffer.append(
                indenter.indent()
                        + "transient amino.run.common.AppObjectStub.Context $__context = null;"
                        + EOLN);

        buffer.append(
                indenter.indent()
                        + "amino.run.common.MicroServiceID $__microServiceId = null;"
                        + EOLN);
        buffer.append(
                indenter.indent()
                        + "amino.run.policy.Policy.ClientPolicy $__client = null;"
                        + EOLN);
        buffer.append(indenter.indent() + "boolean $__directInvocation = false;" + EOLN);
        return buffer.toString();
    }

    @Override
    public String getStubConstructors() {
        StringBuilder buffer = new StringBuilder();
        StringBuilder paramNames;
        StringBuilder exceptionNames;

        Constructor<?>[] constructors = stubClass.getConstructors();

        for (Constructor<?> constructor : constructors) {
            buffer.append(indenter.indent() + "public " + stubName + " (");
            Class<?>[] params = constructor.getParameterTypes();
            paramNames = new StringBuilder();
            for (int i = 0; i < params.length; i++) {
                buffer.append(
                        ((i > 0) ? ", " : "")
                                + RMIUtil.getCanonicalName(params[i])
                                + ' '
                                + RmicUtil.getParameterName(params[i], i + 1));

                paramNames.append(
                        ((i > 0) ? ", " : "") + RmicUtil.getParameterName(params[i], i + 1));
            }

            buffer.append(")");
            Class<?>[] exceptions = constructor.getExceptionTypes();
            exceptionNames = new StringBuilder();
            if (exceptions.length > 0) {
                buffer.append(" throws ");
                exceptionNames.append(exceptions[0].getName());
                for (int i = 1; i < exceptions.length; i++) {
                    exceptionNames.append(", " + exceptions[i].getName());
                }
            }

            buffer.append(exceptionNames + " {" + EOLN);
            buffer.append(indenter.tIncrease() + "super(" + paramNames + ");" + EOLN);

            buffer.append(indenter.indent() + "}" + EOLN + EOLN);
        }
        return buffer.toString();
    }

    @Override
    public String getStubAdditionalMethods() {
        StringBuilder buffer = new StringBuilder("");

        /* The $__initialize function */
        buffer.append(
                indenter.indent()
                        + "public void $__initialize(amino.run.common.MicroServiceID microServiceId, amino.run.policy.Policy.ClientPolicy client) {"
                        + EOLN);
        buffer.append(indenter.tIncrease() + "$__client = client;" + EOLN);
        buffer.append(indenter.tIncrease() + "$__microServiceId = microServiceId;" + EOLN);
        buffer.append(indenter.indent() + "}" + EOLN + EOLN);

        /* The $__initialize function for directInvocation */
        buffer.append(
                indenter.indent() + "public void $__initialize(boolean directInvocation) {" + EOLN);
        buffer.append(indenter.tIncrease() + "$__directInvocation = directInvocation;" + EOLN);
        buffer.append(indenter.indent() + "}" + EOLN + EOLN);

        buffer.append(
                indenter.indent()
                        + "public amino.run.common.MicroServiceID $__getMicroServiceId() {"
                        + EOLN);
        buffer.append(indenter.tIncrease() + "return $__microServiceId;" + EOLN);
        buffer.append(indenter.indent() + "}" + EOLN + EOLN);

        /* Implement the $__clone() method */
        buffer.append(
                indenter.indent()
                        + "public Object $__clone() throws CloneNotSupportedException {"
                        + EOLN);
        buffer.append(
                indenter.tIncrease()
                        + "return super.clone();"
                        + EOLN
                        + indenter.indent()
                        + "}"
                        + EOLN
                        + EOLN);

        buffer.append(
                indenter.indent()
                        + "private void readObject(java.io.ObjectInputStream inputStream) throws java.lang.ClassNotFoundException, java.io.IOException {"
                        + EOLN);
        buffer.append(indenter.tIncrease() + "inputStream.defaultReadObject();" + EOLN);
        buffer.append(
                indenter.tIncrease()
                        + "$__context = new amino.run.common.AppObjectStub.Context(java.util.UUID.randomUUID(), amino.run.kernel.common.GlobalKernelReferences.nodeServer.getLocalHost());"
                        + EOLN
                        + indenter.indent()
                        + "}"
                        + EOLN
                        + EOLN);
        buffer.append(
                indenter.indent()
                        + "private void writeObject(java.io.ObjectOutputStream outputStream) throws java.io.IOException {"
                        + EOLN);
        buffer.append(
                indenter.tIncrease()
                        + "outputStream.defaultWriteObject();"
                        + EOLN
                        + indenter.indent()
                        + "}");

        return buffer.toString();
    }

    /**
     * Returns the stub implementation code section source for the methods
     *
     * @param m : Method for which stub implementation code is needed.
     * @param isDMMethod : Is not used as the same is not needed in AppStub class.
     * @return Stub implementation code for the methods.
     */
    @Override
    public String getMethodContent(MethodStub m, boolean isDMMethod) {
        StringBuilder buffer = new StringBuilder("");

        buffer.append(indenter.indent() + "java.lang.Object $__result = null;" + EOLN);

        int tabWidth = 1;

        // Construct list of comma separated params & the ArrayList of params
        StringBuilder cListParams = new StringBuilder("(");
        StringBuilder listParams = new StringBuilder("");

        for (int i = 0; i < m.numParams; i++) {
            listParams.append(
                    indenter.tIncrease(tabWidth)
                            + "$__params.add("
                            + m.paramNames[i]
                            + ");"
                            + EOLN);
            cListParams.append(((i > 0) ? ", " : "") + ' ' + m.paramNames[i]);
        }
        cListParams.append(")");

        // Check if direct invocation
        buffer.append(indenter.indent() + "if ($__directInvocation) {" + EOLN);

        buffer.append(indenter.tIncrease(tabWidth) + "try {" + EOLN);
        if (!m.retType.getSimpleName().equals("void"))
            buffer.append(
                    indenter.tIncrease(tabWidth + 1)
                            + "$__result = super."
                            + m.name
                            + cListParams.toString()
                            + ";"
                            + EOLN);
        else
            buffer.append(
                    indenter.tIncrease(tabWidth + 1)
                            + "super."
                            + m.name
                            + cListParams.toString()
                            + ";"
                            + EOLN);

        buffer.append(
                indenter.tIncrease(tabWidth)
                        + "} catch (java.lang.Exception e) {"
                        + EOLN
                        + indenter.tIncrease(tabWidth + 1)
                        + "throw new amino.run.common.AppExceptionWrapper(e);"
                        + EOLN
                        + indenter.tIncrease(tabWidth)
                        + '}'
                        + EOLN);

        buffer.append(indenter.indent() + "} else {" + EOLN); // $NON-NLS-1$

        buffer.append(
                indenter.tIncrease(tabWidth)
                        + "amino.run.common.AppObjectStub.context.set($__context);"
                        + EOLN);
        buffer.append(
                indenter.tIncrease(tabWidth)
                        + "java.util.ArrayList<Object> $__params = new java.util.ArrayList<Object>();"
                        + EOLN); //$NON-NLS-1$
        buffer.append(
                indenter.tIncrease(tabWidth)
                        + "String $__method = \""
                        + m.genericName
                        + "\";"
                        + EOLN); //$NON-NLS-1$
        buffer.append(listParams.toString()); // $NON-NLS-1$
        buffer.append(indenter.tIncrease(tabWidth) + "try {" + EOLN); // $NON-NLS-1$
        buffer.append(
                indenter.tIncrease(tabWidth + 1)
                        + "$__result = $__client.onRPC($__method, $__params);"
                        + EOLN); // $NON-NLS-1$
        buffer.append(
                indenter.tIncrease(tabWidth)
                        + "} catch (amino.run.common.AppExceptionWrapper e) {"
                        + EOLN
                        + indenter.tIncrease(tabWidth + 1)
                        + "Exception ex = e.getException();"
                        + EOLN);
        buffer.append(indenter.tIncrease(tabWidth + 1));
        for (Iterator i = m.catches.iterator(); i.hasNext(); ) {
            Class clz = (Class) i.next();
            buffer.append(
                    "if (ex instanceof "
                            + clz.getName()
                            + ") {"
                            + EOLN
                            + indenter.tIncrease(tabWidth + 2)
                            + "throw ("
                            + clz.getName()
                            + ") ex;"
                            + EOLN);
            if (i.hasNext()) {
                buffer.append(indenter.tIncrease(tabWidth + 1) + "} else ");
            }
        }

        buffer.append(
                indenter.tIncrease(tabWidth + 1)
                        + "} else {"
                        + EOLN //$NON-NLS-1$
                        + indenter.tIncrease(tabWidth + 2)
                        + "throw new java.lang.RuntimeException(ex);"
                        + EOLN
                        + indenter.tIncrease(tabWidth + 1)
                        + '}'
                        + EOLN);

        for (Iterator i = m.catches.iterator(); i.hasNext(); ) {
            Class clz = (Class) i.next();
            buffer.append(
                    indenter.tIncrease(tabWidth)
                            + "} catch (" //$NON-NLS-1$
                            + clz.getName()
                            + " e) {"
                            + EOLN //$NON-NLS-1$
                            + indenter.tIncrease(tabWidth + 1)
                            + "throw e;"
                            + EOLN); //$NON-NLS-1$
        }

        buffer.append(
                indenter.tIncrease(tabWidth)
                        + "} catch (java.lang.Exception e) {"
                        + EOLN //$NON-NLS-1$
                        + indenter.tIncrease(tabWidth + 1)
                        + "throw new java.lang.RuntimeException(e);"
                        + EOLN //$NON-NLS-1$
                        + indenter.tIncrease(tabWidth)
                        + "}"
                        + EOLN); //$NON-NLS-1$
        buffer.append(indenter.indent() + "}" + EOLN); // $NON-NLS-1$

        if (!m.retType.getSimpleName().equals("void")) {
            buffer.append(
                    indenter.indent()
                            + "return " //$NON-NLS-1$
                            + RmicUtil.getReturnObjectString(m.retType, "$__result")
                            + ';'
                            + EOLN);
        }

        return buffer.toString();
    }
}
