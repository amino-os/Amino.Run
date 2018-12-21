package amino.run.app;

import java.io.Serializable;
import java.util.*;

/**
 * Requirement is condition used in selector.
 *
 * <p>Each requirement can represent one of different condition such as equal/in/notin.
 *
 * <p>Application can create multiple requirements and add them in selectors. <code>
 *    Requirement req = new Requirement(
 *                         "key1", Requirement.Equal, new ArrayList<>(Arrays.asList("value1")));
 *    </code>
 */
public class Requirement implements Serializable {
    private String key;
    private Operator operator;
    private List<String> values;

    public Requirement(String key, Operator operator, List<String> values)
            throws IllegalArgumentException {
        validateRequirement(key, operator, values);
        this.key = key;
        this.operator = operator;
        this.values = values;
    }

    // constructor defined for JavaBeans in yaml parsing
    private Requirement() {}

    // setter and getter method defined for JavaBeans yaml parsing
    public void setKey(String key) {
        this.key = key;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }

    public String getKey() {
        return key;
    }

    public Operator getOperator() {
        return operator;
    }

    public List<String> getValues() {
        return values;
    }

    private void validateRequirement(String key, Operator operator, List<String> values)
            throws IllegalArgumentException {
        switch (operator) {
            case Equal:
                if (values == null || values.isEmpty() || values.size() > 1) {
                    throw new IllegalArgumentException("invalid value for <Equal> operation");
                }
                break;
            case Exists:
                if (values != null && !values.isEmpty()) {
                    throw new IllegalArgumentException("values provided for <Exists> operation");
                }
                break;
        }
    }

    private boolean hasValue(String value) {
        return values != null && values.contains(value);
    }

    /**
     * Test label against condition in Requirement
     *
     * @param labels label getting tested
     * @return return true if label satisfy condition
     */
    public boolean matches(Map<String, String> labels) {
        switch (operator) {
            case Equal:
            case In:
                if (!labels.containsKey(key)) {
                    return false;
                }
                return hasValue(labels.get(key));
            case NotIn:
                if (!labels.containsKey(key)) {
                    return true;
                }
                return !hasValue(labels.get(key));
            case Exists:
                return labels.containsKey(key);
        }

        return false;
    }

    @Override
    public String toString() {
        String req =
                key
                        + " " // insert key
                        + operator; // insert operator
        if (operator.equals(Operator.Exists)) {
            return req;
        }
        return req + " " + Arrays.toString(values.toArray()); // insert values
    }
}
