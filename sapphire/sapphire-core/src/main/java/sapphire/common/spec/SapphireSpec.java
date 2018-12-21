package sapphire.common.spec;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.yaml.snakeyaml.Yaml;
import sapphire.app.DMSpec;
import sapphire.app.SapphireObjectSpec;
import sapphire.common.AppObjectStub;

/**
 * Sapphire Object Specification.
 *
 * <p>Most applications should use Yaml file to create instances of SapphireSpec via {@link
 * #fromYaml(InputStream)}.
 *
 * <p>Java application has the option to create {@link SapphireObjectSpec} programmatically with
 * {@link SpecBuilder} class, then call {@link #fromBuilder(SapphireObjectSpec)}. </code> Yaml
 * Example: <code>
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

    /**
     * Obtain a builder object to create a new SapphireSpec via {@link
     * fromBuilder(SapphireObjectSpec)}
     */
    public static SapphireObjectSpec newBuilder() {
        return new SapphireObjectSpec();
    }

    /**
     * Create a SapphireSpec from details specified in Yaml String. Throws IOException if Yaml
     * includes references to source files or DMs that do not exist.
     */
    public static SapphireSpec fromYaml(String yamlString) throws IOException {
        Yaml yaml = new Yaml();
        return fromBuilder(yaml.load(yamlString));
    }

    /**
     * Create a SapphireSpec from details specified in Yaml read from stream.
     *
     * @throws IOException if Yaml includes references to source files or DMs that do not exist, or
     *     if yamlSource throws exception.
     */
    public static SapphireSpec fromYAML(InputStream yamlSource) throws IOException {
        Yaml yaml = new Yaml();
        return fromBuilder(yaml.load(yamlSource));
    }

    /**
     * Create a SapphireSpec from details specified in builder class.
     *
     * @throws IOException if builder references source files or DMs that do not exist.
     */
    public static SapphireSpec fromBuilder(SapphireObjectSpec s) throws IOException {
        if (s.getLang().supportJavaReflect()) {
            return new JVMReflectSapphireSpec(s);
        } else if (s.getLang().supportGraalVMReflect()) {
            return new GraalVMSapphireSpec(s);
        } else {
            throw new IllegalArgumentException("language " + s.getLang() + " not supported");
        }
    }

    /**
     * Obtain a new instance of the described Sapphire Object. params are flatened and passed to the
     * constructor of the described object. e.g. {1, 2} as object(1, 2).
     *
     * @throws NoSuchMethodException if param types do not match any known constructor
     * @throws InstantiationException if instantiation failed because the desribed object cannot be
     *     instantiated.
     * @throws InvocationTargetException if the constructor threw an exception. This exception wraps
     *     the internal exception.
     * @throws IllegalAccessException if the specified constructor is private.
     */
    public Object construct(Object[] params)
            throws NoSuchMethodException, InstantiationException, InvocationTargetException,
                    IllegalAccessException;

    /**
     * Obtain a new instance of the described Sapphire Object. Calls the no argument constructor.
     *
     * @throws NoSuchMethodException if there does not exist a no argument constructor.
     * @throws InstantiationException if instantiation failed because the desribed object cannot be
     *     instantiated.
     * @throws InvocationTargetException if the constructor threw an exception. This exception wraps
     *     the internal exception.
     * @throws IllegalAccessException if the no argument constructor is private.
     */
    public Object construct()
            throws NoSuchMethodException, InstantiationException, InvocationTargetException,
                    IllegalAccessException;

    /**
     * Invoke the method {@link method} on {@link host} passing in parameters {@link params}.
     *
     * @throws NoSuchMethodException if method of that name and parameter type does not exist
     * @throws InvocationTargetException if the underlying method threw an exception. This exception
     *     wraps the internal exception.
     * @throws IllegalAccessException if the method specified is private.
     */
    public Object invoke(Object host, String method, Object[] params)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException;

    /**
     * Get the list of DMs modifying this type of Sapphire Objects. List is in order of outermost to
     * innermost. Modifying this list does not change the behaviour of the SapphireSpec.
     */
    public List<DMSpec> getDmList();

    /**
     * Get a client stub for accessing an object of specified type. The stub is not hooked up to any
     * DMs.
     */
    public AppObjectStub getStub();
}
