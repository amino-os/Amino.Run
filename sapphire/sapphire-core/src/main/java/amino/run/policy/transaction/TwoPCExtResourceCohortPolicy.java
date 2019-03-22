package amino.run.policy.transaction;

/** DM for 2PC participants with external resource */
public class TwoPCExtResourceCohortPolicy extends TwoPCCohortPolicy {
    /** DCAP distributed transaction default client policy */
    public static class TwoPCExtResourceCohortClientPolicy extends TwoPCCohortClientPolicy {}

    /** DCAP distributed transaction default server policy */
    public static class TwoPCExtResourceCohortServerPolicy extends TwoPCCohortServerPolicy {

        @Override
        public void onCreate(GroupPolicy group) {
            super.onCreate(group);
            this.transactionManager =
                    new ExtResourceTransactionManager(
                            this.sandboxProvider, this.transactionManager);
        }
    }

    /** DCAP distributed transaction default group policy */
    public static class TwoPCExtResourceCohortGroupPolicy extends TwoPCCohortGroupPolicy {}
}
