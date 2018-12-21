package sapphire.app;

/** Programming languages supported by Sapphire. */
public enum Language {
    java("java", true),
    ruby("ruby", true),
    python("python", false),
    js("js", true),
    R("R", false);

    private final String graalID;
    private final boolean isSupported;

    private Language(String id, boolean support) {
        graalID = id;
        isSupported = support;
    }

    /** Language supports JVM Reflection. */
    public boolean supportJavaReflect() {
        return this == Language.java;
    }

    /**
     * Language supports GraalVM Reflection. Objects from this language can be cast to Value and
     * fields and methods can be queried there.
     */
    public boolean supportGraalVMReflect() {
        // TODO this isn't futureproof
        return this != Language.java;
    }

    /** Language ID for interop with GraalVM. */
    public String graalID() {
        return graalID;
    }

    /** Language is supported by the Sapphire Runtime. */
    public boolean isSupported() {
        return isSupported;
    }
}
