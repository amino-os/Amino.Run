package amino.run.policy;

import amino.run.common.*;
import amino.run.kernel.common.KernelObjectNotFoundException;
import amino.run.kernel.common.KernelObjectStub;
import amino.run.policy.util.ResettableTimer;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

public class DefaultSapphirePolicy extends Policy {

    public static class DefaultServerPolicy extends ServerPolicy {
        protected int HEALTH_STATUS_QUERY_INTERVAL = 5000;
        protected transient ResettableTimer healthCheckTimer;
        private DefaultGroupPolicy group;
        private String statusMethodName;

        @Override
        public GroupPolicy getGroup() {
            return group;
        }

        @Override
        public void onMembershipChange() {}

        @Override
        public void onCreate(GroupPolicy group, SapphireObjectSpec spec) {
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
                AbstractSapphireObject class.*/
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
        public void onCreate(GroupPolicy group, SapphireObjectSpec spec) {
            this.group = (DefaultGroupPolicy) group;
        }
    }

    public static class DefaultGroupPolicy extends GroupPolicy {
        private ArrayList<ServerPolicy> servers = new ArrayList<ServerPolicy>();
        protected String region = "";
        protected SapphireObjectSpec spec = null;
        protected int replicaCount = 1;
        protected int HEALTH_STATUS_MAX_SKIP_TICKS = 3;
        protected int HEALTH_STATUS_REPORT_INTERVAL = 5000;
        protected transient volatile int healthStatusTick = 1;
        protected transient ResettableTimer healthCheckTimer;

        @Override
        /**
         * it is advisable that defer putting this server policy into group policy until the server
         * policy is complete with full policy chain.
         */
        public synchronized void addServer(ServerPolicy server) throws RemoteException {
            if (servers == null) {
                // TODO: Need to change it to proper exception
                throw new RemoteException("Group object deleted");
            }
            servers.add(server);
        }

        @Override
        public synchronized void removeServer(ServerPolicy server) throws RemoteException {
            if (servers != null) {
                servers.remove(server);
            }
        }

        @Override
        public ArrayList<ServerPolicy> getServers() throws RemoteException {
            if (servers == null) {
                return new ArrayList<ServerPolicy>();
            }
            return new ArrayList<ServerPolicy>(servers);
        }

        @Override
        public void onCreate(String region, ServerPolicy server, SapphireObjectSpec spec)
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
            servers = null;
        }

        @Override
        public void onNotification(NotificationObject notificationObject) throws RemoteException {
            super.onNotification(notificationObject);
            if (!(notificationObject instanceof SapphireStatusObject)) {
                /* Unsupported notification received */
                return;
            }

            try {
                SapphireStatusObject statusObject = (SapphireStatusObject) notificationObject;
                if (statusObject.isStatus()) {
                    /* Mark the server as healthy */
                    KernelObjectStub server =
                            (KernelObjectStub) getServer(statusObject.getServerId());
                    server.$__setLastSeenTick(healthStatusTick);
                }
            } catch (KernelObjectNotFoundException e) {
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
                        /* Did not receive notification from the server policy for max times. Purge it */
                        removeReplica(server);
                    }
                }

                servers = getServers();
                if (!servers.isEmpty() && servers.size() < replicaCount) {
                    /* Add new replicas in same region if number of servers are less than replica
                    count */
                    addReplicas(servers.get(0).sapphire_getRegion());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void addReplicas(String region)
                throws RemoteException, MicroServiceNotFoundException,
                MicroServiceReplicaNotFoundException {
            ArrayList<ServerPolicy> servers = getServers();

            /* Get the list of available servers in region */
            List<InetSocketAddress> fullKernelList;
            fullKernelList = sapphire_getAddressList(null, region);
            if (null == fullKernelList) {
                // Kernel Servers not available in the region
                return;
            }

            /* Get the list of servers on which replicas already exist */
            ArrayList<InetSocketAddress> sappObjReplicatedKernelList =
                    new ArrayList<InetSocketAddress>();
            for (ServerPolicy tmp : servers) {
                sappObjReplicatedKernelList.add(((KernelObjectStub) tmp).$__getHostname());
            }

            /* Remove the servers which already have replicas of this sapphire object */
            fullKernelList.removeAll(sappObjReplicatedKernelList);

            if (!fullKernelList.isEmpty()) {
                int availableKernelCount = fullKernelList.size();
                int serverPolicyCount = servers.size();
                for (int loop = replicaCount - serverPolicyCount;
                        availableKernelCount > 0 && loop > 0;
                        loop--, availableKernelCount--) {
                    /* create a replica on the first server in the list */
                    addReplica(
                            servers.get(0),
                            fullKernelList.get(availableKernelCount - 1),
                            region,
                            false);
                }
                return;
            }

            // Cannot create replica of the sapphire object. All the kernel servers have its replica
            return;
        }
    }
}
