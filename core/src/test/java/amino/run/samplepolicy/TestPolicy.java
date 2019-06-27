package amino.run.samplepolicy;

import amino.run.policy.DefaultPolicy;

public class TestPolicy extends DefaultPolicy {
    public static class ServerPolicy extends DefaultServerPolicy {
        /**
         * Method declared to check static method generation in Server policy stub.
         *
         * @return true
         */
        public static Boolean staticMethod() {
            return true;
        }
    }

    public static class GroupPolicy extends DefaultGroupPolicy {
        /**
         * Method declared to check static method generation in Group policy stub methods.
         *
         * @return true
         */
        public static Boolean staticMethod() {
            return true;
        }
    }
}
