package sapphire.compiler;

public class GlobalStubConstants {
    public static final String POLICY_STUB_PACKAGE = "sapphire.policy.stubs";
    public static final String APPOBJECT_PACKAGE = "sapphire.common.AppObject";
    public static final String STUB_SUFFIX = "_Stub";
    public static final String STUB_PACKAGE_PART = "stubs";
    
    public static String getAppPackageName(String classPackageName) {
    	return classPackageName + ".stubs";
    }
    
    public static String getAppObjectPackageName() {
    	return APPOBJECT_PACKAGE;
    }

    public static String getPolicyPackageName() {
    	return POLICY_STUB_PACKAGE;
    }
}
