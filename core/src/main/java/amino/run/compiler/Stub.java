package amino.run.compiler;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import org.apache.harmony.rmi.common.ClassList;
import org.apache.harmony.rmi.common.RMIUtil;
import org.apache.harmony.rmi.compiler.Indenter;
import org.apache.harmony.rmi.compiler.RMICompilerException;
import org.apache.harmony.rmi.compiler.RmicConstants;
import org.apache.harmony.rmi.compiler.RmicUtil;

public abstract class Stub implements RmicConstants {

    /** Class for which I'm generating this stub */
    Class<?> stubClass;

    /** Indenter to write source files */
    Indenter indenter;

    /** Name of the class to generate stub for */
    protected final String className;

    /** Package of the class to generate stub for */
    protected final String packageName;

    /** Stub class name */
    protected final String stubName;

    /** DM methods that need to go through the DM chain instead of a direct call */
    protected Set<String> dmChainMethods = new HashSet<String>(Arrays.asList("onRPC"));

    /** List of remote methods for the class */
    private final TreeSet<MethodStub> methods;

    /**
     * Creates <code>Stub</code> instance for specified type (app or kernel) and class.
     *
     * @param cls Class to load.
     */
    Stub(Class<?> cls) {
        stubClass = cls;
        className = RMIUtil.getCanonicalName(cls);
        packageName = RMIUtil.getPackageName(cls);
        String shortClassName = RMIUtil.getShortName(cls);
        stubName = shortClassName + stubSuffix;
        methods = getMethods();
    }

    /**
     * Returns stub source code for the loaded class.
     *
     * @return String containing the stub source code for loaded class.
     */
    public String getStubSource() {
        indenter = new Indenter();

        return (getStubHeader()
                + getPackageStatement()
                + EOLN //$NON-NLS-1$
                + getImportStatement()
                + EOLN
                + getStubClassDeclaration()
                + indenter.hIncrease()
                + getStubFields()
                + EOLN
                + getStubConstructors()
                + EOLN
                + getStubAdditionalMethods()
                + EOLN
                + getMethodImplementations()
                + indenter.decrease()
                + '}'
                + EOLN
                + indenter.assertEmpty());
    }

    /**
     * Returns stub source code header
     *
     * @return Stub header.
     */
    private String getStubHeader() {
        return ("/*"
                + EOLN //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                + " * Stub for class "
                + className
                + EOLN //$NON-NLS-1$
                + " * Generated by MicroService Compiler (sc)."
                + EOLN //$NON-NLS-1$
                + " */"
                + EOLN); //$NON-NLS-1$
    }

    /**
     * Returns remote methods implementation.
     *
     * @return Stub method implementations code.
     */
    private String getMethodImplementations() {
        StringBuilder buffer = new StringBuilder();
        for (Iterator<MethodStub> i = methods.iterator(); i.hasNext(); ) {
            MethodStub m = i.next();
            if (this.dmChainMethods.contains(m.name)) {
                buffer.append(EOLN + m.getStubImpl(false));
            } else {
                buffer.append(EOLN + m.getStubImpl(true));
            }
        }
        return buffer.toString();
    }

    public abstract TreeSet<MethodStub> getMethods();

    public abstract String getPackageStatement();

    public abstract String getImportStatement();

    public abstract String getStubClassDeclaration();

    public abstract String getStubFields();

    public abstract String getStubConstructors();

    public abstract String getStubAdditionalMethods();

    public abstract String getMethodContent(MethodStub m, boolean isDMMethod);

    /** Generates Stub code for a particular method. */
    public final class MethodStub implements Comparable<MethodStub> {

        /** The method name (via {@link Method#getName()}) */
        final String name;

        /** The method parameters (via {@link Method#getParameterTypes()}) */
        final Class<?>[] parameters;

        /** The method parameters class names */
        final String[] paramClassNames;

        /** The method parameters names */
        final String[] paramNames;

        /** Number of parameters for this method */
        final int numParams;

        /** The method return type (via {@link Method#getReturnType()}) */
        final Class<?> retType;

        /** The method return type name */
        final String retTypeName;

        /** Exceptions that this method throws */
        final Vector<Class<?>> exceptions;

        /** Exceptions that must be caught in method stub */
        final ClassList catches;

        /**
         * The method short signature (via {@link RMIUtil#getShortMethodSignature(Method)
         * getShortMethodSignature()})
         */
        private final String shortSign;

        /** The declaring class of this method */
        private final Class<?> declaringClass;

        /** The generic name of the method */
        final String genericName;

        public String getGenericName() {
            return genericName;
        }

        public String getName() {
            return name;
        }

        public String[] getParamClassNames() {
            return paramClassNames;
        }

        public String getRetTypeName() {
            return retTypeName;
        }

        public Class<?> getDeclaringClass() {
            return declaringClass;
        }

        /**
         * Creates method stub instance.
         *
         * @param method Method to process.
         * @throws RMICompilerException If some error occurs.
         */
        MethodStub(Method method) {
            this.name = method.getName();
            this.genericName = method.toGenericString();
            this.parameters = method.getParameterTypes();
            this.numParams = parameters.length;
            this.retType = method.getReturnType();
            this.retTypeName = RMIUtil.getCanonicalName(retType);
            this.shortSign = RMIUtil.getShortMethodSignature(method);
            this.declaringClass = method.getDeclaringClass();

            // Create parameter names array & string.
            paramClassNames = new String[numParams];
            paramNames = new String[numParams];
            for (int i = 0; i < numParams; i++) {
                Class<?> parameter = parameters[i];
                paramClassNames[i] = RMIUtil.getCanonicalName(parameter);
                paramNames[i] = RmicUtil.getParameterName(parameter, i + 1);
            }

            // Create list of exceptions declared thrown.
            Class<?>[] exceptionsArray = method.getExceptionTypes();
            exceptions = new Vector<Class<?>>(exceptionsArray.length);
            exceptions.addAll(Arrays.asList(exceptionsArray));

            // Create list of exceptions to be caught.
            catches = new ClassList(false);

            // Add declared thrown exceptions.
            catches.addAll(exceptions);

            /* Add the runtime exception at the end such that all the more specific exceptions of
            runtime are added before it */
            catches.add(RuntimeException.class);
        }

        /**
         * Returns stub implementation for this method
         *
         * @return Stub implementation for this method.
         */
        String getStubImpl(boolean isDMMethod) {
            return (getStubImplHeader()
                    + indenter.hIncrease()
                    + getMethodContent(this, isDMMethod)
                    + indenter.decrease()
                    + '}'
                    + EOLN);
        }

        /**
         * Returns header for the stub implementation for this method
         *
         * @return Stub implementation header for this method.
         */
        private String getStubImplHeader() {
            StringBuilder buffer =
                    new StringBuilder(
                            indenter.indent()
                                    + "// Implementation of "
                                    + shortSign
                                    + EOLN //$NON-NLS-1$
                                    + indenter.indent()
                                    + "public "
                                    + retTypeName //$NON-NLS-1$
                                    + ' '
                                    + name
                                    + '(');

            // Write method parameters.
            for (int i = 0; i < numParams; i++) {
                buffer.append(
                        ((i > 0) ? ", " : "") // $NON-NLS-1$ //$NON-NLS-2$
                                + paramClassNames[i]
                                + ' '
                                + paramNames[i]);
            }

            buffer.append(')');

            if (!exceptions.isEmpty()) {
                buffer.append(EOLN + indenter.tIncrease(2) + "throws "); // $NON-NLS-1$
            }

            // Write exceptions declared thrown.
            for (Iterator<Class<?>> i = exceptions.iterator(); i.hasNext(); ) {
                buffer.append(
                        ((Class<?>) i.next()).getName()
                                + (i.hasNext() ? ", " : "")); // $NON-NLS-1$ //$NON-NLS-2$
            }
            buffer.append(" {" + EOLN);
            return buffer.toString();
        }

        @Override
        public boolean equals(Object object) {
            MethodStub ms = (MethodStub) object;

            if (!ms.getName().equals(name)) return false;

            if (!(declaringClass.isAssignableFrom(ms.getDeclaringClass()))
                    && (!(ms.getDeclaringClass().isAssignableFrom(declaringClass)))) return false;

            if (!ms.getRetTypeName().equals(retTypeName)) return false;

            if (ms.getParamClassNames().length != paramClassNames.length) return false;

            int i = 0;
            while (i < paramClassNames.length) {
                if (!ms.getParamClassNames()[i].equals(paramClassNames[i])) {
                    return false;
                }
                i++;
            }
            return true;
        }

        @Override
        public int compareTo(MethodStub other) {
            if (other.equals(this)) return 0;

            int comp = other.getName().compareTo(name);

            if (comp == 0) return other.getGenericName().compareTo(genericName);

            return comp;
        }
    }
}
