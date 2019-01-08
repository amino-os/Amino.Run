package amino.run.sysSapphireObjects.migrationScheduler;

import amino.run.app.SapphireObject;
import amino.run.sysSapphireObjects.migrationScheduler.policy.ExecutionPolicy;

public class MigrationScheduler implements SapphireObject {
    private MigrationPolicy policy;

    public MigrationScheduler(String policyConstant, String policySpec) {
        switch (policyConstant) {
            case MigrationSchedulerPolicies.ExecutionPolicy:
                policy = new ExecutionPolicy(policySpec);
                break;
        }
    }

    public void start() throws Exception {
        policy.start();
    }
}
