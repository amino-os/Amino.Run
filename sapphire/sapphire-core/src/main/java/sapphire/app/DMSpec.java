package sapphire.app;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.yaml.snakeyaml.Yaml;

public final class DMSpec {
    private String name;
    private Map<String, String> properties;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public void addProperty(String name, String value) {
        if (properties == null) {
            properties = new HashMap<>();
        }
        this.properties.put(name, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DMSpec dmSpec = (DMSpec) o;
        return Objects.equals(name, dmSpec.name) && Objects.equals(properties, dmSpec.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, properties);
    }

    @Override
    public String toString() {
        return new Yaml().dump(this);
    }
}
