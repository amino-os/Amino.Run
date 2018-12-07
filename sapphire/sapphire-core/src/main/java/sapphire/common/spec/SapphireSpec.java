package sapphire.common.spec;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.yaml.snakeyaml.Yaml;
import sapphire.common.AppObjectStub;
import sapphire.app.DMSpec;
import sapphire.app.SapphireObjectSpec;
import sapphire.app.Language;

/**
 * Sapphire Object Specification.
 *
 * <p>Most applications should use Yaml file to specify sapphire object specs used to create
 * SapphireSpec Yaml files can be parsed into SapphireObjectSpec with {@link #fromYaml(String)}.
 *
 * <p>Java application has the option to create {@link SapphireObjectSpec} programmatically with
 * {@link SpecBuilder} class. <code>
 *
 * </code> Yaml of one Sapphire Object Specification Example: <code>
 * !!sapphire.app.SapphireObjectSpec
 * constructorName: college
 * dmList:
 * - configs:
 *   - !!sapphire.policy.scalability.ScaleUpFrontendPolicy$Config {replicationRateInMs: 100}
 *   - !!sapphire.policy.scalability.LoadBalancedFrontendPolicy$Config {maxConcurrentReq: 200,
 *     replicaCount: 30}
 *   name: sapphire.policy.scalability.ScaleUpFrontendPolicy
 * javaClassName: null
 * lang: js
 * name: com.org.College
 * sourceFileLocation: src/main/js/college.js
 * </code>
 */
public interface SapphireSpec extends Serializable {

    public static SapphireObjectSpec newBuilder() {
        return new SapphireObjectSpec();
    }

    public static SapphireSpec fromYaml(String yamlString) throws IOException {
        Yaml yaml = new Yaml();
        return fromBuilder(yaml.load(yamlString));
    }

    public static SapphireSpec fromYAML(InputStream yamlSource) throws IOException {
        Yaml yaml = new Yaml();
        return fromBuilder(yaml.load(yamlSource));
    }

    public static SapphireSpec fromBuilder(SapphireObjectSpec s) throws IOException {
        if (s.getLang().isHostLanguage()) {
            return new HostSapphireSpec(s);
        } else {
            return new GuestSapphireSpec(s);
        }
    }

    public Object construct(Object[] params)
            throws NoSuchMethodException, InstantiationException, InvocationTargetException,
                    IllegalAccessException;

    public Object construct()
            throws NoSuchMethodException, InstantiationException, InvocationTargetException,
                    IllegalAccessException;

    public Object invoke(Object host, String method, Object[] params)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException;

    public List<DMSpec> getDmList();

    public AppObjectStub getStub();

    public boolean isHostType();
}
