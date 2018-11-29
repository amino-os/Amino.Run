package sapphire.app;

import java.io.Serializable;
import java.util.*;
import java.util.logging.Logger;
import org.yaml.snakeyaml.Yaml;
import sapphire.common.Utils;

public class NodeSelectorRequirement implements Serializable {

    public String key = null;
    public String operator = null;
    public List<String> values = null;

    private static Logger logger = Logger.getLogger(NodeSelectorRequirement.class.getName());

    public NodeSelectorRequirement() {}

    public NodeSelectorRequirement(String key, String Operator, List<String> vals) {
        if (!Utils.validateNodeSelectRequirement(key, Operator, vals)) {
            logger.warning("validateNodeSelectRequirement failed");
            return;
        }
        this.key = key;
        this.operator = Operator;
        this.values = vals;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        if (!Utils.validateLabelKey(key)) {
            logger.warning("setKey failed because it is null or empty or validation failed" + key);
            return;
        }
        this.key = key;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        if (!Utils.validateOperator(operator)) {
            logger.warning("validateOperator failed");
            return;
        }
        this.operator = operator;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        if (values == null || values.isEmpty()) {
            logger.warning("setValues failed because it is null or empty" + values);
            return;
        }
        for (int i = 0; i < values.size(); i++) {
            if (!Utils.validateLabelValue(values.get(i))) {
                logger.warning("setValues failed because validation failed" + values);
                return;
            }
        }
        this.values = values;
    }

    public void addValuesItem(String valuesItem) {
        if (!Utils.validateLabelValue(valuesItem)) {
            logger.warning("addValuesItem failed because it is null or empty" + values);
            return;
        }
        if (this.values == null) {
            this.values = new ArrayList<String>();
        }
        this.values.add(valuesItem);
        return;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NodeSelectorRequirement nodeSelectorRequirement = (NodeSelectorRequirement) o;
        return Objects.equals(this.key, nodeSelectorRequirement.key)
                && Objects.equals(this.operator, nodeSelectorRequirement.operator)
                && Objects.equals(this.values, nodeSelectorRequirement.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, operator, values);
    }

    @Override
    public String toString() {
        Yaml yaml = new Yaml();
        return yaml.dump(this);
    }
}
