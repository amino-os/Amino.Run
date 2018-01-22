package sapphire.policy.serializability;

import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import sapphire.policy.SapphirePolicy;

/**
 * Serializes all RPCs to Sapphire object with server side locking.
 *
 * <em>Notes:</em>
 *
 * This implementation closely follows the DM definition by maintaining one lock
 * for the whole Sapphire object in which case <i>all operations</i> on this Sapphire
 * object will be serialized. In reality, developers may just want to serialize
 * invocations on one specific operation or a combination of a few operations.
 */
public class SerializableRPCPolicy extends SapphirePolicy {
    public static class SerializableRPCClientPolicy extends SapphireClientPolicy {
        private SerializableRPCPolicy.SerializableRPCServerPolicy server;
        private SerializableRPCPolicy.SerializableRPCGroupPolicy group;

        @Override
        public void onCreate(SapphireGroupPolicy group) {
            this.group = (SerializableRPCPolicy.SerializableRPCGroupPolicy)group;
        }

        @Override
        public void setServer(SapphireServerPolicy server) {
            this.server = (SerializableRPCPolicy.SerializableRPCServerPolicy)server;
        }

        @Override
        public SapphireServerPolicy getServer() {
            return server;
        }

        @Override
        public SapphireGroupPolicy getGroup() {
            return group;
        }
    }

    public static class SerializableRPCServerPolicy extends SapphireServerPolicy {
        private final Lock lock = new ReentrantLock();
        private SerializableRPCPolicy.SerializableRPCGroupPolicy group;

        @Override
        public void onCreate(SapphireGroupPolicy group) {
            this.group = (SerializableRPCPolicy.SerializableRPCGroupPolicy) group;
        }

        @Override
        public SapphireGroupPolicy getGroup() {
            return this.group;
        }

        @Override
        public void onMembershipChange() {
        }

        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            lock.lock();
            try {
                return appObject.invoke(method, params);
            } finally {
                lock.unlock();
            }
        }
    }

    public static class SerializableRPCGroupPolicy extends SapphireGroupPolicy {
        private SerializableRPCPolicy.SerializableRPCServerPolicy server;

        @Override
        public void onCreate(SapphireServerPolicy server) {
            this.server = (SerializableRPCPolicy.SerializableRPCServerPolicy)server;
        }

        @Override
        public void addServer(SapphireServerPolicy server) {
            this.server = (SerializableRPCPolicy.SerializableRPCServerPolicy)server;
        }

        @Override
        public ArrayList<SapphireServerPolicy> getServers() {
            return null;
        }

        @Override
        public void onFailure(SapphireServerPolicy server) {
        }
    }
}
