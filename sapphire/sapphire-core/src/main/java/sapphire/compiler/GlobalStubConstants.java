package sapphire.compiler;

public class GlobalStubConstants {
    public static final String POLICY_STUB_PACKAGE = "sapphire.policy.stubs";
    public static final String APPOBJECT_PACKAGE = "sapphire.common.AppObject";
    public static final String POLICY_PACKAGE = "sapphire.policy.SapphirePolicy";
    public static final String STUB_SUFFIX = "_Stub";
    public static final String STUB_PACKAGE_PART = "stubs";
    public static final String APPSTUB_POLICY_CLIENT_FIELD_NAME = "$__client";
    public static final String POLICY_ONDESTROY_MTD_NAME_FORMAT =
            "public void %s.onDestroy() throws java.rmi.RemoteException";

    public static String getAppPackageName(String classPackageName) {
        return classPackageName + ".stubs";
    }

    public static String getImportAppObjectPackageName() {
        return APPOBJECT_PACKAGE;
    }

    public static String getImportPolicyPackageName() {
        return POLICY_PACKAGE;
    }

    public static String getPolicyPackageName() {
        return POLICY_STUB_PACKAGE;
    }
}
