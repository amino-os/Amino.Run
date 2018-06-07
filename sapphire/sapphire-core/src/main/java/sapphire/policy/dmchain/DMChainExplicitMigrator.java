package sapphire.policy.dmchain;

import java.net.InetSocketAddress;

/**
 * Created by Malepati Bala Siva Sai Akhil on 1/22/18.
 * This interface must be implemented by all SO's that use ExplicitMigrationPolicy.
 * A convenience implementation (ExplicitMigratorImpl) is provided, so that SO's
 * can just extend that.
 */

public interface DMChainExplicitMigrator {
    public void migrateObject(InetSocketAddress destinationAddr) throws MigrationException;
}
