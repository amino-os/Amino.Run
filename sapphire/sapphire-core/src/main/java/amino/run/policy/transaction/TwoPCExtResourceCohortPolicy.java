package amino.run.policy.transaction;

/** DM for 2PC participants with external resource */
public class TwoPCExtResourceCohortPolicy extends TwoPCCohortPolicy {
    /** DCAP distributed transaction default client policy */
    public static class TwoPCExtResourceCohortClientPolicy extends TwoPCCohortClientPolicy {}

    /** DCAP distributed transaction default server policy */
    public static class TwoPCExtResourceCohortServerPolicy extends TwoPCCohortServerPolicy {
        public TwoPCExtResourceCohortServerPolicy() {
            super(null);

            TLSTransactionManager internalTransactionManager = new TLSTransactionManager();
            internalTransactionManager.setValidator(
                    new NonconcurrentTransactionValidator(
                            this.getAppObject(), this.sandboxProvider));
            this.setTransactionManager(
                    new ExtResourceTransactionManager(
                            this.sandboxProvider, internalTransactionManager));
        }
    }

    /** DCAP distributed transaction default group policy */
    public static class TwoPCExtResourceCohortGroupPolicy extends TwoPCCohortGroupPolicy {}
}
