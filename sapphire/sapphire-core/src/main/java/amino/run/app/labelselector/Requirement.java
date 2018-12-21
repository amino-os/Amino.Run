package amino.run.app.labelselector;

import java.io.Serializable;
import java.util.*;

/**
 * Requirement is condition used in selector.
 *
 * <p>Each requirement can represent one of different condition such as equal/in/notin.
 *
 * <p>Application should use {@link Requirement.Builder} for creating Requirement. <code>
 *    Requirement req = Requirement.newBuilder().key("key1")
 *                 .equal()
 *                 .value("value1")
 *                 .create();
 *    </code>
 */
public class Requirement implements Serializable {
    public static final String Equal = "=";
    public static final String In = "in";
    public static final String NotIn = "notin";
    public static final String Exists = "exists";

    private String key;
    private String operator;
    private List<String> values;

    private Requirement(String key, String operator, List<String> values) {
        this.key = key;
        this.operator = operator;
        this.values = values;
    }

    private boolean hasValue(String value) {
        return values != null && values.contains(value);
    }

    /**
     * Get requirement key
     *
     * @return key
     */
    public String key() {
        return key;
    }

    /**
     * Get list of conditions's values
     *
     * @return return list of values
     */
    public Set<String> values() {
        Set<String> valueSet = new HashSet<>();
        valueSet.addAll(values);
        return valueSet;
    }

    /**
     * Get operation in condition
     *
     * @return return operation
     */
    public String operator() {
        return operator;
    }

    /**
     * Test label against condition in Requirement
     *
     * @param labels label getting tested
     * @return return true if label satisfy condition
     */
    public boolean matches(Labels labels) {
        switch (operator) {
            case Equal:
                if (!labels.has(key)) {
                    return false;
                }
                return hasValue(labels.get(key));
            case In:
                if (!labels.has(key)) {
                    return false;
                }
                return hasValue(labels.get(key));
            case NotIn:
                if (!labels.has(key)) {
                    return true;
                }
                return !hasValue(labels.get(key));
            case Exists:
                return labels.has(key);
        }

        return false;
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        // insert key
        buffer.append(key);

        // insert operator
        switch (operator) {
            case Equal:
                buffer.append("=");
                break;
            case In:
                buffer.append(" in ");
                break;
            case NotIn:
                buffer.append(" notin ");
                break;
            case Exists:
                buffer.append(" exists");
                break;
        }

        if (operator.equals(In) || operator.equals(NotIn)) {
            buffer.append("{");
        }

        if (values != null) {
            buffer.append(String.join(",", values));
        }

        if (operator.equals(In) || operator.equals(NotIn)) {
            buffer.append("}");
        }

        return buffer.toString();
    }

    /**
     * Create {@link Requirement.Builder} instance
     *
     * @return
     */
    public static Requirement.Builder newBuilder() {
        return new Requirement.Builder();
    }

    public static class Builder {
        private String key;
        private String operator;
        private List<String> values;

        public Requirement.Builder key(String key) {
            this.key = key;
            return this;
        }

        public Requirement.Builder value(String label) {
            this.values = new ArrayList<>(Arrays.asList(label));
            return this;
        }

        public Requirement.Builder values(String... label) {
            this.values = new ArrayList<>(Arrays.asList(label));
            return this;
        }

        public Requirement.Builder equal() {
            this.operator = Equal;
            return this;
        }

        public Requirement.Builder in() {
            this.operator = In;
            return this;
        }

        public Requirement.Builder notIn() {
            this.operator = NotIn;
            return this;
        }

        public Requirement.Builder exists() {
            this.operator = Exists;
            return this;
        }

        // TODO: add operator specific checks
        // Equal operator should no have any values more than one
        // Exists should not have any values
        public Requirement create() {
            return new Requirement(key, operator, values);
        }
    }
}
