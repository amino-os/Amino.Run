
package sapphire.compiler;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.TreeSet;

import org.apache.harmony.rmi.common.RMIUtil;
import org.apache.harmony.rmi.compiler.RmicUtil;

/**
 * Generates stub code for a given App Object class.
 *
 * @author aaasz
 *
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
		return ((packageName == null) ? "" //$NON-NLS-1$
				: ("package " + GlobalStubConstants.getAppPackageName(packageName) +";" + EOLN + EOLN)); //$NON-NLS-1$
	}

	@Override
	public String getStubClassDeclaration() {
		StringBuilder buffer = new StringBuilder("");
		buffer.append("public final class " + stubName + " extends " + className + " implements ");
		buffer.append("sapphire.common.AppObjectStub"); //$NON-NLS-1$
		buffer.append(" {" + EOLN + EOLN); //$NON-NLS-1$
		return buffer.toString();
	}

	@Override
	public String getStubFields() {
		StringBuilder buffer = new StringBuilder();
		buffer.append(indenter.indent() + "sapphire.policy.SapphirePolicy.SapphireClientPolicy $__client = null;" + EOLN);
		buffer.append(indenter.indent() + "boolean $__directInvocation = false;" + EOLN);
		return buffer.toString();
	}

	@Override
	public String getStubConstructors() {
		StringBuilder buffer = new StringBuilder();
		StringBuilder paramNames = new StringBuilder();

		Constructor<?>[] constructors = stubClass.getConstructors();

		for(Constructor<?> constructor : constructors)
		{
			buffer.append(indenter.indent() + "public " + stubName + " (");
			Class<?>[] params = constructor.getParameterTypes();

			for (int i = 0; i < params.length; i++) {
				buffer.append(((i > 0) ? ", " : "")
						+ RMIUtil.getCanonicalName(params[i]) + ' ' + RmicUtil.getParameterName(params[i], i + 1));

				paramNames.append(((i > 0) ? ", " : "") + RmicUtil.getParameterName(params[i], i + 1));
			}
			buffer.append(") {" + EOLN);
			buffer.append(indenter.tIncrease() + "super(" + paramNames + ");" + EOLN);

			buffer.append(indenter.indent() + "}" + EOLN + EOLN);
		}
		return buffer.toString();
	}

	@Override
	public String getStubAdditionalMethods() {
		StringBuilder buffer = new StringBuilder("");

		/* The $__initialize function */
		buffer.append(indenter.indent() + "public void $__initialize(sapphire.policy.SapphirePolicy.SapphireClientPolicy client) {" + EOLN);
		buffer.append(indenter.tIncrease() + "$__client = client;" + EOLN);
		buffer.append(indenter.indent() + "}" + EOLN + EOLN);

		/* The $__initialize function for directInvocation */
		buffer.append(indenter.indent() + "public void $__initialize(boolean directInvocation) {" + EOLN);
		buffer.append(indenter.tIncrease() + "$__directInvocation = directInvocation;" + EOLN);
		buffer.append(indenter.indent() + "}" + EOLN + EOLN);

		/* Implement the $__clone() method */
		buffer.append(indenter.indent() + "public Object $__clone() throws CloneNotSupportedException {" + EOLN);
		buffer.append(indenter.tIncrease() + "return super.clone();" + EOLN + indenter.indent() + "}" + EOLN + EOLN);

		return buffer.toString();
	}

	/**
	 * Returns the stub implementation code section source for the methods
	 *
	 * @return  Stub implementation code for the methods.
	 */
	@Override
	public String getMethodContent(MethodStub m) {
		StringBuilder buffer = new StringBuilder("");

		// Construct list of comma separated params & the ArrayList of params
		StringBuilder cListParams = new StringBuilder("(");
		StringBuilder listParams = new StringBuilder("");

		for (int i = 0; i < m.numParams; i++) {
			listParams.append(indenter.tIncrease(2) + "$__params.add(" + m.paramNames[i]+");" + EOLN);
			cListParams.append(((i > 0) ? ", " : "") + ' ' + m.paramNames[i]);
		}
		cListParams.append(")");

		// Check if direct invocation
		buffer.append(indenter.indent() + "java.lang.Object $__result = null;" + EOLN);
		buffer.append(indenter.indent() + "try {" + EOLN);
		buffer.append(indenter.tIncrease() + "if ($__directInvocation)" + EOLN);
		if (!m.retType.getSimpleName().equals("void"))
			buffer.append(indenter.tIncrease(2) + "$__result = super." + m.name + cListParams.toString() + ";" + EOLN);
		else
			buffer.append(indenter.tIncrease(2) + "super." + m.name + cListParams.toString() + ";" + EOLN);
		buffer.append(indenter.tIncrease() + "else {" + EOLN);
		buffer.append(indenter.tIncrease(2) + "java.util.ArrayList<Object> $__params = new java.util.ArrayList<Object>();" + EOLN); //$NON-NLS-1$
		buffer.append(indenter.tIncrease(2) + "String $__method = \"" + m.genericName + "\";" + EOLN); //$NON-NLS-1$
		buffer.append(listParams.toString()); //$NON-NLS-1$
		buffer.append(indenter.tIncrease(2) + "$__result = $__client.onRPC($__method, $__params);" + EOLN);
		buffer.append(indenter.tIncrease() + "}" + EOLN);
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
