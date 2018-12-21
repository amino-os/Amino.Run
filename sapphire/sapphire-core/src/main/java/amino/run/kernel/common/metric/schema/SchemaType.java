package amino.run.kernel.common.metric.schema;

/** List of different supported metric schema type */
public enum SchemaType {
    CounterMetric("CounterMetric");

    private String type;

    SchemaType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }
}
