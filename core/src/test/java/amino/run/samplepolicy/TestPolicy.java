package amino.run.samplepolicy;

import amino.run.policy.DefaultPolicy;

public class TestPolicy extends DefaultPolicy {
    public static class ServerPolicy extends DefaultServerPolicy {
        public static String getPolicyName() {
            return "amino.run.samplepolicy.TestPolicy.ServerPolicy";
        }
    }

    public static class GroupPolicy extends DefaultGroupPolicy {
        public static String getPolicyName() {
            return "amino.run.samplepolicy.TestPolicy.GroupPolicy";
        }
    }
}
