package sapphire.compiler;

public class GlobalStubConstants {
    public static final String POLICY_STUB_PACKAGE = "sapphire.policy.stubs";
    public static final String STUB_SUFFIX = "_Stub";
    public static final String STUB_PACKAGE_PART = "stubs";
    public static final String APPSTUB_POLICY_CLIENT_FIELD_NAME = "$__client";

    public static String getAppPackageName(String classPackageName) {
        return classPackageName + ".stubs";
    }

    public static String getPolicyPackageName() {
        return POLICY_STUB_PACKAGE;
    }
}
