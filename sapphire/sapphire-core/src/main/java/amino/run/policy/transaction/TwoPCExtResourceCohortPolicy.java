package amino.run.policy.transaction;

/** DM for 2PC participants with external resource */
public class TwoPCExtResourceCohortPolicy extends TwoPCCohortPolicy {
    /** DCAP distributed transaction default client policy */
    public static class ClientPolicy extends TwoPCCohortPolicy.ClientPolicy {}

    /** DCAP distributed transaction default server policy */
    public static class ServerPolicy extends TwoPCCohortPolicy.ServerPolicy {
        public ServerPolicy() {
            super(null);

            TLSTransactionManager internalTransactionManager = new TLSTransactionManager();
            internalTransactionManager.setValidator(
                    new NonconcurrentTransactionValidator(
                            this.sapphire_getAppObject(), this.sandboxProvider));
            this.setTransactionManager(
                    new ExtResourceTransactionManager(
                            this.sandboxProvider, internalTransactionManager));
        }
    }

    /** DCAP distributed transaction default group policy */
    public static class GroupPolicy extends TwoPCCohortPolicy.GroupPolicy {}
}
