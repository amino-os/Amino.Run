package sapphire.policy.mobility.explicitmigration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface ExplicitMigrationPolicySpec {
    /** @return maximum time to retry */
    long retryTimeoutInMillis() default 15000;

    /** @return initial retry wait time */
    long minWaitIntervalInMillis() default 100;

    /** @return name of method that triggers object migration */
    String migrateObjecgtMethodName() default ".migrateObject(";
}
