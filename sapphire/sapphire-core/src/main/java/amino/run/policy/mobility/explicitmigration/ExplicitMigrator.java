package amino.run.policy.mobility.explicitmigration;

import java.net.InetSocketAddress;

/**
 * Created by Malepati Bala Siva Sai Akhil on 1/22/18. This interface must be implemented by all
 * SO's that use ExplicitMigrationPolicy. A convenience implementation (ExplicitMigratorImpl) is
 * provided, so that SO's can just extend that.
 */
public interface ExplicitMigrator {
    public void migrateTo(InetSocketAddress destinationAddr) throws MigrationException;
}
