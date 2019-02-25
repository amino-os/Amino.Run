package amino.run.policy;

import amino.run.app.MicroServiceSpec;
import amino.run.common.*;
import amino.run.kernel.common.KernelObjectNotFoundException;
import amino.run.kernel.common.KernelObjectStub;
import amino.run.oms.OMSServer;
import amino.run.policy.util.ResettableTimer;
import amino.run.runtime.AddEvent;

import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultPolicy extends Policy {
    protected static final int REPLICA_COUNT = 3;

    public static class DefaultServerPolicy extends ServerPolicy {
        // This interval is equal to thrice the kernelserver heartbeat period which is equal to
        // hearbeat timeout period
        protected long HEALTH_STATUS_QUERY_INTERVAL = OMSServer.KS_HEARTBEAT_TIMEOUT;
        protected transient ResettableTimer healthCheckTimer;
        private String statusMethodName;
		
        @Override
        public GroupPolicy getGroup() {
            return group;
        }

        @Override
        public void onMembershipChange() {}

        @Override
        public void onCreate(GroupPolicy group, MicroServiceSpec spec) {
            super.onCreate(group, spec);
            AppObject appObject = sapphire_getAppObject();
            try {
                statusMethodName =
                        appObject
                                .getClass(appObject.getObject())
                                .getMethod("getStatus")
                                .toGenericString();
            } catch (NoSuchMethodException e) {
                /* get health status method is not found in SO class. It means, SO did not extend
                StatusReporter class.*/
                logger.warning("Microservice should extend StatusReporter to enable health check");
                return;
            }

            if (healthCheckTimer == null) {
                healthCheckTimer =
                        new ResettableTimer(
                                new TimerTask() {
                                    public void run() {
                                        boolean status;
                                        ArrayList<Object> params = new ArrayList<>();
                                        try {
                                            /* Query health status and update to local kernel server */
                                            status = (boolean) onRPC(statusMethodName, params);
                                            sapphire_update_status(status);
                                            healthCheckTimer.reset();
                                        } catch (KernelObjectNotFoundException e) {
                                            throw new Error("kernel object does not exist");
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                },
                                HEALTH_STATUS_QUERY_INTERVAL);
                healthCheckTimer.start();
            }
        }

        public void onDestroy() {
            super.onDestroy();
            if (healthCheckTimer != null) {
                healthCheckTimer.cancel();
                healthCheckTimer = null;
            }
        }
    }

    public static class DefaultClientPolicy extends ClientPolicy {
        private DefaultServerPolicy server;
        private DefaultGroupPolicy group;

        @Override
        public void setServer(ServerPolicy server) {
            this.server = (DefaultServerPolicy) server;
        }

        @Override
        public ServerPolicy getServer() {
            return server;
        }

        @Override
        public GroupPolicy getGroup() {
            return group;
        }

        @Override
        public void onCreate(GroupPolicy group, MicroServiceSpec spec) {
            this.group = (DefaultGroupPolicy) group;
        }
    }

    public static class DefaultGroupPolicy extends GroupPolicy {
        private ConcurrentHashMap<ReplicaID, ServerPolicy> servers =
                new ConcurrentHashMap<ReplicaID, ServerPolicy>();
        protected String region = "";
        protected MicroServiceSpec spec = null;
        // allowing maxminum of 3 ticks to receive notification from server polciy
        protected int HEALTH_STATUS_MAX_SKIP_TICKS = 3;
        // This interval is equal to thrice the kernelserver heartbeat period which is equal to
        // hearbeat timeout period
        protected long HEALTH_STATUS_REPORT_INTERVAL = OMSServer.KS_HEARTBEAT_TIMEOUT;
        protected transient volatile int healthStatusTick = 1;
        protected transient ResettableTimer healthCheckTimer;

        protected void addServer(ServerPolicy server) {
            servers.put(server.getReplicaId(), server);
        }

        protected void removeServer(ServerPolicy server) {
            servers.remove(server.getReplicaId());
        }

        @Override
        public ArrayList<ServerPolicy> getServers() throws RemoteException {
            return new ArrayList<ServerPolicy>(servers.values());
        }

        @Override
        public void onCreate(String region, ServerPolicy server, MicroServiceSpec spec)
                throws RemoteException {
            this.region = region;
            this.spec = spec;
            addServer(server);
            if (healthCheckTimer == null) {
                healthCheckTimer =
                        new ResettableTimer(
                                new TimerTask() {
                                    public void run() {
                                        healthCheckTimerExpired();
                                        healthCheckTimer.reset();
                                    }
                                },
                                HEALTH_STATUS_REPORT_INTERVAL);
                healthCheckTimer.start();
            }
        }

        @Override
        public synchronized void onDestroy() throws RemoteException {
            super.onDestroy();
            if (healthCheckTimer != null) {
                healthCheckTimer.cancel();
                healthCheckTimer = null;
            }
            servers.clear();
        }

        /** Below methods can be used by all the DMs extending this default DM. */

        /**
         * Gets the server policy having the given replica Id from this group policy's servers map
         *
         * @param serverId Server policy replica Id
         * @return Server policy
         */
        protected ServerPolicy getServer(ReplicaID serverId) {
            return servers.get(serverId);
        }

        /**
         * This method is used to replicate a server policy at the given source considering itself
         * as reference copy and pin it to kernel server with specified host. And adds it to its
         * local server list.
         *
         * @param replicaSource Server policy on which a new replica is created considering itself
         *     as reference copy
         * @param dest Host address on which replicated copy need to pin
         * @param region Region in which replicated server has to be pinned. It is passed down to
         *     all downstream DMs till leaf. And downstream DM pinning the chain ensures to pin on
         *     kernel server belonging to the region
         * @return New replica of server policy
         * @throws RemoteException
         * @throws MicroServiceNotFoundException
         * @throws MicroServiceReplicaNotFoundException
         */
        protected ServerPolicy replicate(
                ServerPolicy replicaSource, InetSocketAddress dest, String region)
                throws RemoteException, MicroServiceNotFoundException,
                        MicroServiceReplicaNotFoundException {
            ServerPolicy replica =
                    replicaSource.sapphire_replicate(replicaSource.getProcessedPolicies(), region);
            if (replicaSource.isLastPolicy()) {
                pin(replica, dest);
            }

            addServer(replica);
            return replica;
        }

        /**
         * Pin the server policy to kernel server with specified host
         *
         * @param server
         * @param host
         * @throws MicroServiceReplicaNotFoundException
         * @throws RemoteException
         * @throws MicroServiceNotFoundException
         */
        protected void pin(ServerPolicy server, InetSocketAddress host)
                throws MicroServiceReplicaNotFoundException, RemoteException,
                        MicroServiceNotFoundException {
            server.sapphire_pin_to_server(host);
            ((KernelObjectStub) server).$__updateHostname(host);
        }

        /**
         * Destroys the given server policy from the kernel server where it resides. And removes it
         * from its local server list
         *
         * @param server
         * @throws RemoteException
         */
        protected void terminate(ServerPolicy server) throws RemoteException {
            server.sapphire_terminate();
            removeServer(server);
        }

        /**
         * MicroServiceStatus is received from oms. If status is healthy, LastSeenTick value is set,
         * otherwise that replica will be removed and a new replica will be added
         *
         * @param notificationObject
         * @throws RemoteException
         */
        @Override
        @AddEvent(event = "NOTIFY")
        public void onNotification(NotificationObject notificationObject) throws RemoteException {
            super.onNotification(notificationObject);
            if (!(notificationObject instanceof MicroServiceStatus)) {
                /* Unsupported notification received */
                return;
            }

            try {
                MicroServiceStatus microServiceStatus = (MicroServiceStatus) notificationObject;
                KernelObjectStub server =
                        (KernelObjectStub) getServer(microServiceStatus.getReplicaID());
                ServerPolicy serverPolicy = getServer(microServiceStatus.getReplicaID());
                if (microServiceStatus.isStatus()) {
                    /* Mark the server as healthy */
                    server.$__setLastSeenTick(healthStatusTick);
                    return;
                }
                terminate(serverPolicy);
                addReplicas(region);
            } catch (KernelObjectNotFoundException e) {
                throw new Error("this kernel object does not exist");
            } catch (MicroServiceNotFoundException e) {
                throw new Error("Failed to find Microservice inorder to add replicas");
            } catch (MicroServiceReplicaNotFoundException e) {
                throw new Error("Failed to find Microservice Replica");
            }
        }

        protected void healthCheckTimerExpired() {
            try {
                healthStatusTick++;
                ArrayList<ServerPolicy> servers = getServers();
                for (ServerPolicy server : servers) {
                    KernelObjectStub serverStub = (KernelObjectStub) server;
                    if ((healthStatusTick - serverStub.$__getLastSeenTick())
                            > HEALTH_STATUS_MAX_SKIP_TICKS) {
                        /* Did not receive notification from the server policy for max times which means it is unhealthy. So replica is removed from group policy */
                        removeServer(server);
                        // TODO:region maybe removed later
                        addReplicas(region);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
