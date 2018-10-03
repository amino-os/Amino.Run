package sapphire.common;

public enum Language {
    JAVA,
    RUBY,
    PYTHON,
    JAVASCRIPT,
    R;

    // Note: this string is consistent with GraalVM, can be use in polyglot.eval function call.
    @Override
    public String toString() {
        switch (ordinal()) {
            case 0:
                return "java";
            case 1:
                return "ruby";
            case 2:
                return "python";
            case 3:
                return "js";
            case 4:
                return "R";
            default:
                return "java";
        }
    }
}
