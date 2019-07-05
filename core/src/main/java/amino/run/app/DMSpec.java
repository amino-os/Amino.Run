package amino.run.app;

import static amino.run.policy.Upcalls.PolicyConfig;

import amino.run.policy.Upcalls;
import com.google.common.base.Objects;
import java.io.Serializable;
import java.util.*;
import org.yaml.snakeyaml.Yaml;

/**
 * Deployment Manager Specification. Also known as MicroService Policy Specification.
 *
 * <p>Each DM specification contains a policy name and an optional list of policy configurations.
 */
public final class DMSpec implements Serializable {
    private String name;
    private List<PolicyConfig> configs = new ArrayList<PolicyConfig>();

    /**
     * Returns a builder class for DMSpec.
     *
     * @return a builder class for DMSpec.
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Returns policy name of this DM
     *
     * @return the policy name
     */
    public String getName() {
        return name;
    }

    /**
     * A list of policy configurations.
     *
     * <p>One amino run policy usually has only one configuration. But in some cases, one amino run
     * policy may have multiple configurations. For example, @{@link
     * amino.run.policy.scalability.ScaleUpFrontendPolicy} is a sub class of {@link
     * amino.run.policy.scalability.LoadBalancedFrontendPolicy} policy. Each of them has their own
     * configurations. To properly config a scale up front end policy, we have to specify two sets
     * of configurations, one for scale up front end policy, and the other for load balanced front
     * end policy.
     */
    public List<PolicyConfig> getConfigs() {
        return configs;
    }

    /**
     * Returns a policy configuration map where the key is the name of the configuration and the
     * value is a {@link PolicyConfig} instance.
     *
     * @return a policy configuration map that contains all configurations of this DM.
     */
    private Map<String, PolicyConfig> getConfigMap() {
        Map<String, PolicyConfig> map = new HashMap<String, PolicyConfig>();
        for (PolicyConfig c : configs) {
            map.put(c.getClass().getSimpleName(), c);
        }
        return map;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setConfigs(List<PolicyConfig> configs) {
        this.configs = configs;
    }

    public static DMSpec fromYaml(String yamlStr) {
        return new Yaml().loadAs(yamlStr, DMSpec.class);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DMSpec dmSpec = (DMSpec) o;
        return Objects.equal(name, dmSpec.name) && Objects.equal(configs, dmSpec.configs);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, configs);
    }

    @Override
    public String toString() {
        Yaml yaml = new Yaml();
        return yaml.dump(this);
    }

    public static class Builder {
        private String name;
        private List<PolicyConfig> configs = new ArrayList<PolicyConfig>();

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder addConfig(Upcalls.PolicyConfig config) {
            this.configs.add(config);
            return this;
        }

        public DMSpec create() {
            DMSpec spec = new DMSpec();
            spec.setName(name);
            spec.setConfigs(configs);
            return spec;
        }
    }
}
