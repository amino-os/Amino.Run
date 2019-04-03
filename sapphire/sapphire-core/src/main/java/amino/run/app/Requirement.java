package amino.run.app;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Requirement is condition used in selector.
 *
 * <p>Each requirement can represent one of different condition such as equal/in/notin.
 *
 * <p>Application can create multiple requirements and add them in selectors. <code>
 *    Requirement req = new Requirement(
 *                         "key1", Operator.Equal, new ArrayList<>(Arrays.asList("value1")));
 *    </code>
 */
public class Requirement implements Serializable {
    private String key;
    private Operator operator;
    private List<String> values;

    public Requirement(String key, Operator operator, List<String> values)
            throws IllegalArgumentException {
        this.key = key;
        this.operator = operator;
        this.values = values;
        validateRequirement();
    }

    // constructor defined for JavaBeans in yaml parsing
    private Requirement() {}

    // setter method defined for JavaBeans yaml parsing
    public void setKey(String key) {
        this.key = key;
    }

    // setter method defined for JavaBeans yaml parsing
    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    // setter method defined for JavaBeans yaml parsing
    public void setValues(List<String> values) {
        this.values = values;
    }

    // getter method defined for JavaBeans yaml parsing
    public String getKey() {
        return key;
    }

    // getter method defined for JavaBeans yaml parsing
    public Operator getOperator() {
        return operator;
    }

    // getter method defined for JavaBeans yaml parsing
    public List<String> getValues() {
        return values;
    }

    /**
     * Validate requirement against different supported operations
     *
     * @throws IllegalArgumentException
     */
    public void validateRequirement() throws IllegalArgumentException {
        if (key.isEmpty()) {
            throw new IllegalArgumentException("requirement key empty");
        }

        switch (operator) {
            case Equal:
                if (values == null || values.isEmpty() || values.size() > 1) {
                    throw new IllegalArgumentException(
                            String.format("invalid value for <%s> operation", operator));
                }
                break;
            case In:
            case NotIn:
                if (values == null) {
                    throw new IllegalArgumentException(
                            String.format("invalid value for <%s> operation", operator));
                }
                break;
            case Exists:
                if (values != null && !values.isEmpty()) {
                    throw new IllegalArgumentException(
                            String.format("invalid value for <%s> operation", operator));
                }
                break;
            default:
                throw new IllegalArgumentException(String.format("invalid operator %s", operator));
        }
    }

    /**
     * Test label against condition in Requirement
     *
     * @param labels label getting tested
     * @return return true if label satisfy condition
     */
    public boolean matches(Map<String, String> labels) {
        String labelValue = labels.get(key);
        if (labelValue == null) {
            return false;
        }

        switch (operator) {
            case Equal:
                // Label matching logic for Equal and IN operator is same as both are matching for
                // entity maintained in value set. For Equal operator
                // values has only one entity but for In operator it has set of entity.
            case In:
                return values.contains(labelValue);
            case NotIn:
                return !values.contains(labelValue);
            case Exists:
                return true;
        }

        return false;
    }

    @Override
    public String toString() {
        return key
                + " " // insert key
                + operator // insert operator
                + " "
                + Arrays.toString(values.toArray()); // insert values
    }
}
