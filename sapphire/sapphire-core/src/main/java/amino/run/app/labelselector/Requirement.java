package amino.run.app.labelselector;

import java.io.Serializable;
import java.util.*;

/**
 * Requirement is condition used in selector.
 *
 * <p>Each requirement can represent one of different condition such as equal/in/notin.
 *
 * <p>Application can create multiple requirements and add them in {@link Selector}. <code>
 *    Requirement req = new Requirement(
 *                         "key1", Requirement.Equal, new ArrayList<>(Arrays.asList("value1")));
 *        Selector selector = new Selector().add(req);
 *    </code>
 */
public class Requirement implements Serializable {
    private String key;
    private Operator operator;
    private List<String> values;

    public Requirement(String key, Operator operator, List<String> values)
            throws RequirementInvalidException {
        switch (operator) {
            case Equal:
                if (values == null || values.isEmpty() || values.size() > 1) {
                    throw new RequirementInvalidException("invalid value for <Equal> operation");
                }
                break;
            case Exists:
                if (values != null && !values.isEmpty()) {
                    throw new RequirementInvalidException("values provided for <Exists> operation");
                }
                break;
        }
        this.key = key;
        this.operator = operator;
        this.values = values;
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
    public boolean matches(Labels labels) {
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
