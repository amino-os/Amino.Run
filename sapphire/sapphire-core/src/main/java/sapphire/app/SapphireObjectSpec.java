package sapphire.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.yaml.snakeyaml.Yaml;

public class SapphireObjectSpec {
    public enum Language {
        JS,
        Ruby,
        Java
    };

    private Language lang;
    private String name;
    private String javaClassName;

    private String sourceFileLocation;
    private String constructorName;
    private List<DMSpec> dms;

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

    public List<DMSpec> getDms() {
        return dms;
    }

    public void setDms(List<DMSpec> dms) {
        this.dms = dms;
    }

    public void addDM(DMSpec dmSpec) {
        if (dms == null) {
            dms = new ArrayList<DMSpec>();
        }

        dms.add(dmSpec);
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
                && Objects.equals(dms, that.dms);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lang, name, javaClassName, sourceFileLocation, constructorName, dms);
    }

    @Override
    public String toString() {
        return new Yaml().dump(this);
    }
}
