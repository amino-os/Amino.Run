package amino.run.app;

/** Class maintains list of Operation supported for {@link Requirement} creation */
public enum Operator {
    Equal("="),
    In("in"),
    NotIn("notin"),
    Exists("exists");

    private String operator;

    Operator(String operator) {
        this.operator = operator;
    }

    @Override
    public String toString() {
        return operator;
    }
}
