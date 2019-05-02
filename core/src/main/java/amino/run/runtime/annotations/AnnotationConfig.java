package amino.run.runtime.annotations;

import amino.run.policy.Upcalls;
import java.util.HashMap;
import java.util.Map;

/**
 * This class can be removed after we completely deprecated Java annotation based policy
 * configurations.
 *
 * @deprecated This class is created to support java annotation based policy configurations.
 */
public class AnnotationConfig implements Upcalls.PolicyConfig {
    private String annotationType;

    private Map<String, String> properties = new HashMap<String, String>();

    public void addConfig(String key, String value) {
        properties.put(key, value);
    }

    public String getConfig(String key) {
        return properties.get(key);
    }

    public String getAnnotationType() {
        return annotationType;
    }

    public void setAnnotationType(String annotationType) {
        this.annotationType = annotationType;
    }
}
