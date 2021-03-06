package amino.run.compiler;

import amino.run.policy.Upcalls;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.TreeSet;
import org.apache.harmony.rmi.compiler.RmicUtil;

public class PolicyStub extends Stub {

    public PolicyStub(Class<?> cls) {
        super(cls);
    }

    @Override
    public TreeSet<MethodStub> getMethods() {
        TreeSet<MethodStub> ms = new TreeSet<MethodStub>();

        Class<?> ancestorClass = stubClass;
        while ((!ancestorClass.getSimpleName().equals("ServerPolicyLibrary"))
                && (!ancestorClass.getSimpleName().equals("GroupPolicyLibrary"))) {

            for (Method m : ancestorClass.getDeclaredMethods()) {
                // Add public methods to methods vector
                if (Modifier.isPublic(m.getModifiers())) {
                    ms.add(new MethodStub(m));
                }
            }

            ancestorClass = ancestorClass.getSuperclass();
        }

        return ms;
    }

    @Override
    public String getPackageStatement() {
        return ((packageName == null)
                ? "" //$NON-NLS-1$
                : ("package "
                        + GlobalStubConstants.getPolicyPackageName()
                        + ';'
                        + EOLN
                        + EOLN)); //$NON-NLS-1$
    }

    @Override
    public String getImportStatement() {
        return "import "
                + GlobalStubConstants.POLICY_CLASS
                + ";"
                + EOLN //$NON-NLS-1$
                + "import "
                + GlobalStubConstants.APPOBJECT_CLASS
                + ";"
                + EOLN
                + EOLN; //$NON-NLS-1$
    }

    @Override
    public String getStubClassDeclaration() {
        StringBuilder buffer = new StringBuilder("");
        buffer.append("public final class " + stubName + " extends " + className + " implements ");
        buffer.append("amino.run.kernel.common.KernelObjectStub"); // $NON-NLS-1$
        buffer.append(" {" + EOLN + EOLN); // $NON-NLS-1$
        return buffer.toString();
    }

    @Override
    public String getStubFields() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(
                indenter.indent() + "amino.run.kernel.common.KernelOID $__oid = null;" + EOLN);
        buffer.append(indenter.indent() + "java.net.InetSocketAddress $__hostname = null;" + EOLN);
        buffer.append(indenter.indent() + "Policy.ClientPolicy $__nextClientPolicy = null;" + EOLN);
        return buffer.toString();
    }

    @Override
    public String getStubConstructors() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(
                indenter.indent()
                        + "public "
                        + stubName
                        + "(amino.run.kernel.common.KernelOID oid) {"
                        + EOLN);
        buffer.append(
                indenter.tIncrease()
                        + "this.$__oid = oid;"
                        + EOLN
                        + indenter.indent()
                        + "}"
                        + EOLN);
        return buffer.toString();
    }

    @Override
    public String getStubAdditionalMethods() {
        boolean isGroupPolicy = Upcalls.GroupUpcalls.class.isAssignableFrom(stubClass);
        StringBuilder buffer = new StringBuilder();

        /* Implementation for getKernelOID */
        buffer.append(
                indenter.indent()
                        + "public amino.run.kernel.common.KernelOID $__getKernelOID() {"
                        + EOLN);
        buffer.append(
                indenter.tIncrease()
                        + "return this.$__oid;"
                        + EOLN
                        + indenter.indent()
                        + "}"
                        + EOLN
                        + EOLN);

        /* Implementation for getHostname */
        buffer.append(
                indenter.indent() + "public java.net.InetSocketAddress $__getHostname() {" + EOLN);
        buffer.append(
                indenter.tIncrease()
                        + "return this.$__hostname;"
                        + EOLN
                        + indenter.indent()
                        + "}"
                        + EOLN
                        + EOLN);

        /* Implementation for updateHostname */
        buffer.append(
                indenter.indent()
                        + "public void $__updateHostname(java.net.InetSocketAddress hostname) {"
                        + EOLN);
        buffer.append(
                indenter.tIncrease()
                        + "this.$__hostname = hostname;"
                        + EOLN
                        + indenter.indent()
                        + "}"
                        + EOLN
                        + EOLN);

        /* Implementation for setNextClientPolicy
         * This is not needed for group policy but it is retained as group policy implements
         * the same KernelObjectStub that requires setNextClientPolicy */
        // TODO: Remove setNextClientPolicy for group policy if above restriction is eliminated.
        buffer.append(
                indenter.indent()
                        + "public void $__setNextClientPolicy(Policy.ClientPolicy clientPolicy) {"
                        + EOLN);
        buffer.append(
                indenter.tIncrease()
                        + "this.$__nextClientPolicy = clientPolicy;"
                        + EOLN
                        + indenter.indent()
                        + "}"
                        + EOLN
                        + EOLN);

        if (!isGroupPolicy) {
            /* Implementation for makeRPC */
            addServerPolicyContent(buffer);
        }

        /* Implementation for makeKernelDMRPC */
        buffer.append(
                indenter.indent()
                        + "public Object $__makeKernelDMRPC(java.lang.String method, java.util.ArrayList<Object> params) throws java.rmi.RemoteException, java.lang.Exception {"
                        + EOLN);

        buffer.append(
                indenter.tIncrease()
                        + "amino.run.common.AppObjectStub.Context context = amino.run.common.AppObjectStub.context.get();"
                        + EOLN);
        buffer.append(
                indenter.tIncrease()
                        + "amino.run.kernel.common.KernelRPC rpc = new amino.run.kernel.common.KernelRPC(context, $__oid, method, params);"
                        + EOLN);
        buffer.append(indenter.tIncrease() + "try {" + EOLN);
        buffer.append(
                indenter.tIncrease(2)
                        + "return amino.run.kernel.common.GlobalKernelReferences.nodeServer.getKernelClient().makeKernelRPC(this, rpc);"
                        + EOLN);
        buffer.append(
                indenter.tIncrease()
                        + "} catch (amino.run.kernel.common.KernelObjectNotFoundException e) {"
                        + EOLN);
        buffer.append(indenter.tIncrease(2) + "throw new java.rmi.RemoteException();" + EOLN);
        buffer.append(indenter.tIncrease() + "}" + EOLN);
        buffer.append(indenter.indent() + "}" + EOLN + EOLN);

        /* Override equals  */
        buffer.append(indenter.indent() + "@Override" + EOLN);
        buffer.append(indenter.indent() + "public boolean equals(Object obj) { " + EOLN);
        buffer.append(indenter.tIncrease() + stubName + " other = (" + stubName + ") obj;" + EOLN);
        buffer.append(indenter.tIncrease() + "if (! other.$__oid.equals($__oid))" + EOLN);
        buffer.append(indenter.tIncrease(2) + "return false;" + EOLN);
        buffer.append(indenter.tIncrease() + "return true;" + EOLN);
        buffer.append(indenter.indent() + "}" + EOLN);

        /* Override hashCode */
        buffer.append(indenter.indent() + "@Override" + EOLN);
        buffer.append(indenter.indent() + "public int hashCode() { " + EOLN);
        buffer.append(indenter.tIncrease() + "return $__oid.getID();" + EOLN);
        buffer.append(indenter.indent() + "}" + EOLN);

        return buffer.toString();
    }

    /**
     * Returns the stub implementation code section source for this method
     *
     * @return Stub implementation code for this method.
     */
    @Override
    public String getMethodContent(MethodStub m, boolean isDMMethod) {
        StringBuilder buffer = new StringBuilder("");

        // Construct list of parameters and String holding the method name
        // to call KernelObjectStub.makeRPC
        buffer.append(
                indenter.indent()
                        + "java.util.ArrayList<Object> $__params = new java.util.ArrayList<Object>();"
                        + EOLN); //$NON-NLS-1$
        buffer.append(
                indenter.indent()
                        + "String $__method = \""
                        + m.genericName
                        + "\";"
                        + EOLN); //$NON-NLS-1$

        if (m.numParams > 0) {
            // Write invocation parameters.
            // TODO: primitive types ??
            for (int i = 0; i < m.numParams; i++) {
                buffer.append(indenter.indent() + "$__params.add(" + m.paramNames[i] + ");" + EOLN);
            }
        }

        // Write return statement.
        buffer.append(indenter.indent() + "java.lang.Object $__result = null;" + EOLN);

        /* If method do not throw generic exception. Catch all the exceptions in the stub and rethrow
        them based on exceptions method is allowed to throw. Runtime exceptions are thrown to app
        as is. And rest of the exceptions are wrapped into runtime exceptions and thrown to app */
        if (!m.exceptions.contains(Exception.class)) {
            /* Append try catch block for this case */
            buffer.append(indenter.indent() + "try {" + EOLN);
            buffer.append(
                    indenter.tIncrease()
                            + getMethodRPCContent(isDMMethod)
                            + EOLN); //$NON-NLS-1$ //$NON-NLS-2$

        } else {
            buffer.append(
                    indenter.indent()
                            + getMethodRPCContent(isDMMethod)
                            + EOLN); //$NON-NLS-1$ //$NON-NLS-2$
        }

        if (!m.exceptions.contains(Exception.class)) {
            for (Iterator i = m.catches.iterator(); i.hasNext(); ) {
                Class clz = (Class) i.next();
                buffer.append(
                        indenter.indent()
                                + "} catch (" //$NON-NLS-1$
                                + clz.getName()
                                + " e) {"
                                + EOLN //$NON-NLS-1$
                                + indenter.tIncrease()
                                + "throw e;"
                                + EOLN); //$NON-NLS-1$
            }

            buffer.append(
                    indenter.indent()
                            + "} catch (java.lang.Exception e) {"
                            + EOLN //$NON-NLS-1$
                            + indenter.tIncrease()
                            + "throw new java.lang.RuntimeException(e);" //$NON-NLS-1$
                            + EOLN //$NON-NLS-1$
                            + indenter.indent()
                            + '}'
                            + EOLN);
        }

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

    /**
     * Returns the KernelRPC stub implementation code based on whether the method is a DM method or
     * Application method.
     *
     * @param isDMMethod
     * @return Stub code for RPC call based on method type.
     */
    public String getMethodRPCContent(boolean isDMMethod) {
        if (isDMMethod == true) {
            return "$__result = $__makeKernelDMRPC($__method, $__params);";
        } else {
            return "$__result = $__makeKernelRPC($__method, $__params);";
        }
    }

    private void addServerPolicyContent(StringBuilder buffer) {
        /* Implementation for makeRPC */
        buffer.append(
                indenter.indent()
                        + "public Object $__makeKernelRPC(java.lang.String method, java.util.ArrayList<Object> params) throws java.rmi.RemoteException, java.lang.Exception {"
                        + EOLN);

        buffer.append(indenter.tIncrease() + "if ($__nextClientPolicy != null) {" + EOLN);
        buffer.append(
                indenter.tIncrease(2) + "return $__nextClientPolicy.onRPC(method, params);" + EOLN);
        buffer.append(indenter.tIncrease() + "}" + EOLN + EOLN);

        buffer.append(
                indenter.tIncrease()
                        + "amino.run.common.AppObjectStub.Context context = amino.run.common.AppObjectStub.context.get();"
                        + EOLN);
        buffer.append(
                indenter.tIncrease()
                        + "amino.run.kernel.common.KernelRPC rpc = new amino.run.kernel.common.KernelRPC(context, $__oid, method, params);"
                        + EOLN);
        buffer.append(indenter.tIncrease() + "try {" + EOLN);
        buffer.append(
                indenter.tIncrease(2)
                        + "return amino.run.kernel.common.GlobalKernelReferences.nodeServer.getKernelClient().makeKernelRPC(this, rpc);"
                        + EOLN);
        buffer.append(
                indenter.tIncrease()
                        + "} catch (amino.run.kernel.common.KernelObjectNotFoundException e) {"
                        + EOLN);
        buffer.append(indenter.tIncrease(2) + "throw new java.rmi.RemoteException();" + EOLN);
        buffer.append(indenter.tIncrease() + "}" + EOLN);
        buffer.append(indenter.indent() + "}" + EOLN + EOLN);
    }
}
