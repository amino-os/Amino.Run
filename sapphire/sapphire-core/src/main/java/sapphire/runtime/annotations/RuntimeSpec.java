package sapphire.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for specifying Sapphire object runtime specifications.
 *
 * @author terryz
 */
@Target(ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface RuntimeSpec {
    /** @return number of desired replicas */
    int replicas() default 1;

    /** @return labels used to select hosts to run replicas */
    String[] hostLabels() default {};
}
