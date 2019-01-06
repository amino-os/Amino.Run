package sapphire.sysSapphireObjects.migrationScheduler;

import sapphire.app.SapphireObject;
import sapphire.sysSapphireObjects.migrationScheduler.policy.ExecutionPolicy;

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
