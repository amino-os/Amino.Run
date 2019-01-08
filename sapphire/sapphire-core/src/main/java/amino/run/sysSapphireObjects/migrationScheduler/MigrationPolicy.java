package amino.run.sysSapphireObjects.migrationScheduler;

import java.io.Serializable;

public interface MigrationPolicy extends Serializable {
    public void start() throws Exception;
}
