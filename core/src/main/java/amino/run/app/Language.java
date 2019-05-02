package amino.run.app;

/**
 * Programming languages supported by MicroService
 *
 * <p>WARNING: String values of this enum will be used in {@code polyglot.eval} function and
 * therefore they should be consistent with the definitions in GraalVM.
 */
public enum Language {
    java,
    ruby,
    python,
    js,
    R
}
