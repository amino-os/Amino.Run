package amino.run.compiler;

public class GlobalStubConstants {
    public static final String POLICY_STUB_PACKAGE = "amino.run.policy.stubs";
    public static final String APPOBJECT_CLASS = "amino.run.common.AppObject";
    public static final String POLICY_CLASS = "amino.run.policy.Policy";
    public static final String STUB_SUFFIX = "_Stub";
    public static final String STUB_PACKAGE_PART = "stubs";
    public static final String APPSTUB_POLICY_CLIENT_FIELD_NAME = "$__client";
    public static final String EVENT_GENERATOR_PACKAGE_NAME = "amino.run.runtime";
    public static final String EVENT_CLASS = "amino.run.runtime.Event";

    public static String getAppPackageName(String classPackageName) {
        return classPackageName + ".stubs";
    }

    // TODO (2018-10-1, Sungwook): Consider removing this as the constant variable is already
    // directly accessible.
    public static String getPolicyPackageName() {
        return POLICY_STUB_PACKAGE;
    }
}
