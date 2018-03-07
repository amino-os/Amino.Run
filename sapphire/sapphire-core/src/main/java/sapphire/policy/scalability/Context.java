package sapphire.policy.scalability;

/**
 * @author terryz
 */
public class Context {
    private final LoadBalancedMasterSlavePolicy.GroupPolicy group;

    private final FileLogger entryLogger;

    private final Configuration config;

    private final CommitExecutor commitExecutor;

    private Context(Builder builder) {
        this.group = builder.group;
        this.entryLogger = builder.entryLogger;
        this.config = builder.config;
        this.commitExecutor = builder.commitExecutor;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public LoadBalancedMasterSlavePolicy.GroupPolicy getGroup() {
        return this.group;
    }

    public FileLogger getEntryLogger() {
        return this.entryLogger;
    }

    public Configuration getConfig() {
        return this.config;
    }

    public CommitExecutor getCommitExecutor() {return this.commitExecutor;}

    public static class Builder {
        private LoadBalancedMasterSlavePolicy.GroupPolicy group;
        private FileLogger entryLogger;
        private Configuration config;
        private CommitExecutor commitExecutor;

        public Builder group(LoadBalancedMasterSlavePolicy.GroupPolicy group) {
            this.group = group;
            return this;
        }

        public Builder entryLogger(FileLogger entryLogger) {
            this.entryLogger = entryLogger;
            return this;
        }

        public Builder config(Configuration config) {
            this.config = config;
            return this;
        }

        public Builder commitExecutor(CommitExecutor executor) {
            this.commitExecutor = executor;
            return this;
        }

        public Context build() {
            if (group == null) {
                throw new NullPointerException("group not specified");
            }
            return new Context(this);
        }
    }
}
