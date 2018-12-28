package sapphire.app.labelselector;

import java.io.Serializable;
import java.util.*;

public class Requirement implements Serializable {
    public static final String Equal = "=";
    public static final String In = "in";
    public static final String NotIn = "notin";
    public static final String Exists = "exists";

    private String key;
    private String operator;
    private List<String> values;

    public Requirement(String key, String operator, List<String> values) {
        this.key = key;
        this.operator = operator;
        this.values = values;
    }

    private boolean hasValue(String value) {
        return values != null && values.contains(value);
    }

    public String key() {
        return key;
    }

    public Set<String> values() {
        Set<String> valueSet = new HashSet<>();
        valueSet.addAll(values);
        return valueSet;
    }

    public String operator() {
        return operator;
    }

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
                buffer.append(key);
                break;
        }

        if (operator.equals(In) || operator.equals(NotIn)) {
            buffer.append("{");
        }

        buffer.append(String.join(",", values));

        if (operator.equals(In) || operator.equals(NotIn)) {
            buffer.append("}");
        }

        return buffer.toString();
    }

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

        public Requirement create() {
            return new Requirement(key, operator, values);
        }
    }
}
