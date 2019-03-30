package amino.run.policy.scalability.masterslave;

import static amino.run.policy.DefaultPolicy.DefaultGroupPolicy;

/** @author terryz */
public class Context {
    private final DefaultGroupPolicy group;

    private final Configuration config;

    private final Committer commitExecutor;

    private final Replicator replicator;

    public Context(
            DefaultGroupPolicy group,
            Configuration config,
            Committer commitExecutor,
            Replicator replicator) {
        this.group = group;
        this.config = config;
        this.commitExecutor = commitExecutor;
        this.replicator = replicator;
    }

    public DefaultGroupPolicy getGroup() {
        return this.group;
    }

    public Configuration getConfig() {
        return this.config;
    }

    public Committer getCommitExecutor() {
        return this.commitExecutor;
    }

    public Replicator getReplicator() {
        return this.replicator;
    }
}
