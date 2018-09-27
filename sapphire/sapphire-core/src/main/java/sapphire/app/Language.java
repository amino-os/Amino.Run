package sapphire.app;

/**
 * Programming languages supported by Sapphire
 *
 * <p>WARNING: String values of this enum will be used in {@code polyglot.eval} function and
 * therefore they should be consistent with the definitions in GraalVM.
 */
public enum Language {
    java,
    ruby,
    python,
    js,
    R,
    // this used during serialization, serialization does not have to specify language, if
    // it's not specified we need to explicitly give during deserialization.
    // the goal is to make client call unaware of language, server can figure out language
    // with OID.
    unspecified
}
