package sapphire.compiler;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.TreeSet;

import org.apache.harmony.rmi.compiler.RmicUtil;

public class PolicyStub extends Stub {
	
    public PolicyStub(Class<?> cls) throws ClassNotFoundException {
        super(cls);
    }
 
	@Override
	public TreeSet<MethodStub> getMethods() {
        TreeSet<MethodStub> ms = new TreeSet<MethodStub>();
        
        Class<?> ancestorClass = stubClass;
    	while ((!ancestorClass.getSimpleName().equals("SapphireServerPolicyLibrary")) &&
    			(!ancestorClass.getSimpleName().equals("SapphireGroupPolicyLibrary"))) {
    		
    		for (Method m : ancestorClass.getDeclaredMethods()) {
                // Add public methods to methods vector
        		if (Modifier.isPublic(m.getModifiers())) {
        			ms.add(new MethodStub((Method) m));
        		}
            }
    		
    		ancestorClass = ancestorClass.getSuperclass();
    	}
		
		return ms;
	}

	@Override
	public String getPackageStatement() {
		return ((packageName == null) ? "" //$NON-NLS-1$
                : ("package " + GlobalStubConstants.getPolicyPackageName() + ';' + EOLN + EOLN)); //$NON-NLS-1$
	}

	@Override
	public String getStubClassDeclaration() {
		StringBuilder buffer = new StringBuilder("");
		buffer.append("public final class " + stubName + " extends " + className + " implements ");
		buffer.append("sapphire.kernel.common.KernelObjectStub"); //$NON-NLS-1$
		buffer.append(" {" + EOLN + EOLN); //$NON-NLS-1$
		return buffer.toString();
	}

	@Override
	public String getStubFields() {
		StringBuilder buffer = new StringBuilder();
		buffer.append(indenter.indent() + "sapphire.kernel.common.KernelOID $__oid = null;" + EOLN);
		buffer.append(indenter.indent() + "java.net.InetSocketAddress $__hostname = null;" + EOLN);
		return buffer.toString();
	}

	@Override
	public String getStubConstructors() {
		StringBuilder buffer = new StringBuilder();
		buffer.append(indenter.indent() + "public " + stubName + "(sapphire.kernel.common.KernelOID oid) {" + EOLN);
		buffer.append(indenter.tIncrease() + "this.$__oid = oid;" + EOLN + indenter.indent() + "}" + EOLN);
		return buffer.toString();
	}

	@Override
	public String getStubAdditionalMethods() {
		StringBuilder buffer = new StringBuilder();
		
		/* Implementation for getKernelOID */
		buffer.append(indenter.indent() + "public sapphire.kernel.common.KernelOID $__getKernelOID() {" + EOLN);
		buffer.append(indenter.tIncrease() + "return this.$__oid;" + EOLN + indenter.indent() + "}" + EOLN + EOLN);

		/* Implementation for getHostname */
		buffer.append(indenter.indent() + "public java.net.InetSocketAddress $__getHostname() {" + EOLN);
		buffer.append(indenter.tIncrease() + "return this.$__hostname;" + EOLN + indenter.indent() + "}" + EOLN + EOLN);

		/* Implementation for updateHostname */
		buffer.append(indenter.indent() + "public void $__updateHostname(java.net.InetSocketAddress hostname) {" + EOLN);
		buffer.append(indenter.tIncrease() + "this.$__hostname = hostname;" + EOLN + indenter.indent() + "}" + EOLN + EOLN);
		
		/* Implementation for makeRPC */
		buffer.append(indenter.indent() + "public Object $__makeKernelRPC(java.lang.String method, java.util.ArrayList<Object> params) throws java.rmi.RemoteException, java.lang.Exception {" + EOLN);
		buffer.append(indenter.tIncrease() + "sapphire.kernel.common.KernelRPC rpc = new sapphire.kernel.common.KernelRPC($__oid, method, params);" + EOLN);
		buffer.append(indenter.tIncrease() + "try {" + EOLN);
		buffer.append(indenter.tIncrease(2) + "return sapphire.kernel.common.GlobalKernelReferences.nodeServer.getKernelClient().makeKernelRPC(this, rpc);" + EOLN);
		buffer.append(indenter.tIncrease() + "} catch (sapphire.kernel.common.KernelObjectNotFoundException e) {" + EOLN);
		buffer.append(indenter.tIncrease(2) + "throw new java.rmi.RemoteException();" + EOLN);
		buffer.append(indenter.tIncrease() + "}" + EOLN);
		buffer.append(indenter.indent() + "}" + EOLN + EOLN);
		
		/* Override $__initialize */
		/*
		buffer.append(indenter.indent() + "public void $__initialize(java.lang.String $param_String_1, java.util.ArrayList $param_ArrayList_2) { " + EOLN);
		buffer.append(indenter.tIncrease() + "java.util.ArrayList<Object> $__params = new java.util.ArrayList<Object>();" + EOLN);
		buffer.append(indenter.tIncrease() + "String $__method = \"$__initialize\";" + EOLN);
		buffer.append(indenter.tIncrease() + "$__params.add($param_String_1);" + EOLN);
		buffer.append(indenter.tIncrease() + "$__params.add($param_ArrayList_2);" + EOLN);
		buffer.append(indenter.tIncrease() + "try {" + EOLN);
		buffer.append(indenter.tIncrease(2) + "$__makeKernelRPC($__method, $__params);" + EOLN);
		buffer.append(indenter.tIncrease() + "} catch (Exception e) {" +EOLN);
		buffer.append(indenter.tIncrease(2) + "e.printStackTrace();" + EOLN);
		buffer.append(indenter.tIncrease() + "}" +EOLN);
		buffer.append(indenter.indent() + "}" + EOLN + EOLN);
		*/
		/* Override the other $__initialize */
		/*
		buffer.append(indenter.indent() + "public void $__initialize(sapphire.common.AppObject $param_AppObject_1) { " + EOLN);
		buffer.append(indenter.tIncrease() + "java.util.ArrayList<Object> $__params = new java.util.ArrayList<Object>();" + EOLN);
		buffer.append(indenter.tIncrease() + "String $__method = \"$__initialize\";" + EOLN);
		buffer.append(indenter.tIncrease() + "$__params.add($param_AppObject_1);" + EOLN);
		buffer.append(indenter.tIncrease() + "try {" + EOLN);
		buffer.append(indenter.tIncrease(2) + "$__makeKernelRPC($__method, $__params);" + EOLN);
		buffer.append(indenter.tIncrease() + "} catch (Exception e) {" +EOLN);
		buffer.append(indenter.tIncrease(2) + "e.printStackTrace();" + EOLN);
		buffer.append(indenter.tIncrease() + "}" +EOLN);
		buffer.append(indenter.indent() + "}" + EOLN + EOLN);
		*/
		
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
     * @return  Stub implementation code for this method.
     */
	@Override
    public String getMethodContent(MethodStub m) {
        StringBuilder buffer = new StringBuilder("");

        // Construct list of parameters and String holding the method name 
        // to call KernelObjectStub.makeRPC
        buffer.append(indenter.indent() + "java.util.ArrayList<Object> $__params = new java.util.ArrayList<Object>();" + EOLN); //$NON-NLS-1$
        buffer.append(indenter.indent() + "String $__method = \"" + m.genericName + "\";" + EOLN); //$NON-NLS-1$
        
        if (m.numParams > 0) {
            // Write invocation parameters.
        	// TODO: primitive types ??
            for (int i = 0; i < m.numParams; i++) {
                buffer.append(indenter.indent() + "$__params.add(" + m.paramNames[i]+");" + EOLN);
            }
        }

        // Write return statement.
        buffer.append(indenter.indent() + "java.lang.Object $__result = null;" + EOLN);
        buffer.append(indenter.indent() + "try {" + EOLN);
        buffer.append(indenter.tIncrease() + "$__result = $__makeKernelRPC($__method, $__params);" + EOLN); //$NON-NLS-1$ //$NON-NLS-2$
        buffer.append(indenter.indent() + "} catch (Exception e) {" + EOLN); //$NON-NLS-1$ //$NON-NLS-2$
        buffer.append(indenter.tIncrease() + "e.printStackTrace();" + EOLN); //$NON-NLS-1$
        buffer.append(indenter.indent() + "}" + EOLN); //$NON-NLS-1$
        if (!m.retType.getSimpleName().equals("void")) {
        	buffer.append(indenter.indent() + "return " //$NON-NLS-1$
        			+ RmicUtil.getReturnObjectString(m.retType, "$__result")
                    + ';' + EOLN);
        }
        return buffer.toString();
    }
}
