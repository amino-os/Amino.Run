package sapphire.policy.migration.explicitmigration;

/**
 * Created by mbssaiakhil on 1/22/18.
 * This interface must be implemented by all SO's that use ExplicitMigrationPolicy.
 * A convenience implementation (ExplicitMigrationImpl) is provided, so that SO's
 * can just extend that.
 */

public interface ExplicitMigration {
    public void migrateObject() throws Exception;
}
