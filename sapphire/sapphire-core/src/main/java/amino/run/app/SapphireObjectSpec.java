package amino.run.app;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.yaml.snakeyaml.Yaml;

/**
 * Sapphire Object Specification.
 *
 * <p>Most applications should use Yaml file to specify sapphire objects. Yaml files can be parsed
 * into SapphireObjectSpec with {@link #fromYaml(String)}.
 *
 * <p>Java application has the option to create {@link SapphireObjectSpec} programmatically with
 * {@link Builder} class. <code>
 *      LoadBalancedFrontendPolicy.Config config = new LoadBalancedFrontendPolicy.Config();
 *      config.setMaxConcurrentReq(200);
 *      config.setReplicaCount(30);
 *
 *      DMSpec dm = Utils.toDMSpec(config);
 *      SapphireObjectSpec spec = SapphireObjectSpec.newBuilder()
 *                                      .setName("soname")
 *                                      .setLang(Language.Java)
 *                                      .addDMSpec(dm)
 *                                      .create();
 * </code> Yaml of one Sapphire Object Specification Example: <code>
 * !!amino.run.app.SapphireObjectSpec
 * constructorName: college
 * dmList:
 * - configs:
 *   - !!amino.run.policy.scalability.ScaleUpFrontendPolicy$Config {replicationRateInMs: 100}
 *   - !!amino.run.policy.scalability.LoadBalancedFrontendPolicy$Config {maxConcurrentReq: 200,
 *     replicaCount: 30}
 *   name: amino.run.policy.scalability.ScaleUpFrontendPolicy
 * javaClassName: null
 * lang: js
 * name: com.org.College
 * sourceFileLocation: src/main/js/college.js
 * </code>
 */
public class SapphireObjectSpec implements Serializable {
    /** Programming Language in which the Sapphire object is written */
    private Language lang;

    /** Name of Sapphire object */
    private String name;

    /** Java class name of Sapphire object. Only used when {@link #lang} is Java. */
    private String javaClassName;

    /** Location of Sapphire object source file. Used when {@link #lang} is not Java */
    private String sourceFileLocation;

    /** Name of Sapphire object constructor. Used when {@link #lang} is not Java */
    private String constructorName;

    /** List of Deployment Managers to be applied on Sapphire object */
    private List<DMSpec> dmList = new ArrayList<>();

    private NodeSelectorSpec nodeSelectorSpec;

    public static Builder newBuilder() {
        return new Builder();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSourceFileLocation() {
        return sourceFileLocation;
    }

    public void setSourceFileLocation(String sourceFileLocation) {
        this.sourceFileLocation = sourceFileLocation;
    }

    public String getJavaClassName() {
        return javaClassName;
    }

    public void setJavaClassName(String javaClassName) {
        this.javaClassName = javaClassName;
    }

    public String getConstructorName() {
        return constructorName;
    }

    public void setConstructorName(String constructorName) {
        this.constructorName = constructorName;
    }

    public Language getLang() {
        return lang;
    }

    public void setLang(Language lang) {
        this.lang = lang;
    }

    public List<DMSpec> getDmList() {
        return dmList;
    }

    public void setDmList(List<DMSpec> dmList) {
        this.dmList = dmList;
    }

    public void addDMSpec(DMSpec dmSpec) {
        dmList.add(dmSpec);
    }

    public NodeSelectorSpec getNodeSelectorSpec() {
        return nodeSelectorSpec;
    }

    public void setNodeSelectorSpec(NodeSelectorSpec nodeSelectorSpec) {
        this.nodeSelectorSpec = nodeSelectorSpec;
    }

    public static SapphireObjectSpec fromYaml(String yamlString) {
        Yaml yaml = new Yaml();
        return yaml.loadAs(yamlString, SapphireObjectSpec.class);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SapphireObjectSpec that = (SapphireObjectSpec) o;
        return lang == that.lang
                && Objects.equals(name, that.name)
                && Objects.equals(javaClassName, that.javaClassName)
                && Objects.equals(sourceFileLocation, that.sourceFileLocation)
                && Objects.equals(constructorName, that.constructorName)
                && Objects.equals(dmList, that.dmList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                lang,
                name,
                javaClassName,
                sourceFileLocation,
                constructorName,
                dmList,
                nodeSelectorSpec);
    }

    @Override
    public String toString() {
        Yaml yaml = new Yaml();
        return yaml.dump(this);
    }

    public static class Builder {
        private String name;
        private Language lang;
        private String javaClassName;
        private String sourceFileLocation;
        private String constructorName;
        private List<DMSpec> dmList = new ArrayList<>();
        private NodeSelectorSpec nodeSelectorSpec;

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setLang(Language lang) {
            this.lang = lang;
            return this;
        }

        public Builder setJavaClassName(String javaClassName) {
            this.javaClassName = javaClassName;
            return this;
        }

        public Builder setSourceFileLocation(String sourceFileLocation) {
            this.sourceFileLocation = sourceFileLocation;
            return this;
        }

        public Builder setConstructorName(String constructorName) {
            this.constructorName = constructorName;
            return this;
        }

        public Builder addDMSpec(DMSpec dmSpec) {
            if (dmList == null) {
                dmList = new ArrayList<>();
            }
            dmList.add(dmSpec);

            return this;
        }

        public Builder setNodeSelectorSpec(NodeSelectorSpec nodeSelectorSpec) {
            this.nodeSelectorSpec = nodeSelectorSpec;
            return this;
        }

        public SapphireObjectSpec create() {
            SapphireObjectSpec spec = new SapphireObjectSpec();
            spec.setName(name);
            spec.setLang(lang);
            spec.setJavaClassName(javaClassName);
            spec.setSourceFileLocation(sourceFileLocation);
            spec.setConstructorName(constructorName);
            spec.setDmList(dmList);
            spec.setNodeSelectorSpec(nodeSelectorSpec);
            return spec;
        }
    }
}
