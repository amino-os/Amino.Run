package amino.run.sysSapphireObjects.migrationScheduler;

import amino.run.app.SapphireObject;
import amino.run.sysSapphireObjects.migrationScheduler.policy.ExecutionPolicy;
import java.util.logging.Logger;

public class MigrationScheduler implements SapphireObject {
    private static Logger logger = Logger.getLogger(MigrationScheduler.class.getName());
    private MigrationPolicy policy;

    public MigrationScheduler(String policyConstant, String policySpec) {
        switch (policyConstant) {
            case MigrationSchedulerPolicies.ExecutionPolicy:
                policy = new ExecutionPolicy(policySpec);
                break;
        }
        logger.info("MigrationScheduler deployment success !!");
    }

    public void start() throws Exception {
        policy.start();
        logger.info("MigrationScheduler start success !!");
    }
}
