package sapphire.app;

/**
 * Programming languages supported by Sapphire
 *
 * <p>WARNING: String values of this enum will be used in {@code polyglot.eval} function and
 * therefore they should be consistent with the definitions in GraalVM.
 */
public enum Language {
    java("java"),
    ruby("ruby"),
    python("python"),
    js("js"),
    R("R");

    private final String graalID;

    private Language(String id) {
        graalID = id;
    }

    public String toString() {
        return graalID;
    }

    public boolean isHostLanguage() {
        return this == Language.java;
    }
}
