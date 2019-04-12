package amino.run.kernel.common.metric.schema;

/**
 * List of supported metric schema type
 *
 * @author AmitRoushan
 */
public enum SchemaType {
    Counter("Counter"),
    Summary("Summary");

    private String type;

    SchemaType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }
}
