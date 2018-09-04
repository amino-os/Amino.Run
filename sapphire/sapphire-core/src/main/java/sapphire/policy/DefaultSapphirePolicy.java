package sapphire.policy;

import java.lang.annotation.Annotation;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import sapphire.common.AppObject;
import sapphire.common.NotificationObject;
import sapphire.common.SapphireObjectNotFoundException;
import sapphire.common.SapphireObjectReplicaNotFoundException;
import sapphire.common.SapphireStatusObject;
import sapphire.kernel.common.KernelObjectNotFoundException;
import sapphire.kernel.common.KernelObjectStub;
import sapphire.policy.util.ResettableTimer;

public class DefaultSapphirePolicy extends SapphirePolicy {

    public static class DefaultServerPolicy extends SapphireServerPolicy {
        protected int HEALTH_STATUS_QUERY_INTERVAL = 5000;
        protected transient ResettableTimer healthCheckTimer;
        private DefaultGroupPolicy group;
        private String statusMethodName;

        @Override
        public SapphireGroupPolicy getGroup() {
            return group;
        }

        @Override
        public void onMembershipChange() {}

        @Override
        public void onCreate(SapphireGroupPolicy group, Annotation[] annotations) {
            this.group = (DefaultGroupPolicy) group;
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

    public static class DefaultClientPolicy extends SapphireClientPolicy {
        private DefaultServerPolicy server;
        private DefaultGroupPolicy group;

        @Override
        public void setServer(SapphireServerPolicy server) {
            this.server = (DefaultServerPolicy) server;
        }

        @Override
        public SapphireServerPolicy getServer() {
            return server;
        }

        @Override
        public SapphireGroupPolicy getGroup() {
            return group;
        }

        @Override
        public void onCreate(SapphireGroupPolicy group, Annotation[] annotations) {
            this.group = (DefaultGroupPolicy) group;
        }
    }

    public static class DefaultGroupPolicy extends SapphireGroupPolicy {
        protected int replicaCount = 1;
        protected int HEALTH_STATUS_MAX_SKIP_TICKS = 3;
        protected int HEALTH_STATUS_REPORT_INTERVAL = 5000;
        protected transient volatile int healthStatusTick = 1;
        protected transient ResettableTimer healthCheckTimer;
        private Set<SapphireServerPolicy> servers =
                Collections.newSetFromMap(new ConcurrentHashMap<SapphireServerPolicy, Boolean>());

        @Override
        public synchronized void addServer(SapphireServerPolicy server) throws RemoteException {
            if (servers == null) {
                // TODO: Need to change it to proper exception
                throw new RemoteException("Group object deleted");
            }
            servers.add(server);
        }

        @Override
        public void removeServer(SapphireServerPolicy server) throws RemoteException {
            if (servers != null) {
                servers.remove(server);
            }
        }

        @Override
        public void onFailure(SapphireServerPolicy server) throws RemoteException {}

        @Override
        public SapphireServerPolicy onRefRequest() throws RemoteException {
            return null;
        }

        @Override
        public ArrayList<SapphireServerPolicy> getServers() throws RemoteException {
            if (servers == null) {
                return new ArrayList<SapphireServerPolicy>();
            }
            return new ArrayList<SapphireServerPolicy>(servers);
        }

        @Override
        public void onCreate(SapphireServerPolicy server, Annotation[] annotations)
                throws RemoteException {
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
                ArrayList<SapphireServerPolicy> servers = getServers();
                for (SapphireServerPolicy server : servers) {
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
                throws RemoteException, SapphireObjectNotFoundException,
                        SapphireObjectReplicaNotFoundException {
            ArrayList<SapphireServerPolicy> servers = getServers();

            /* Get the list of available servers in region */
            ArrayList<InetSocketAddress> fullKernelList;
            fullKernelList = sapphire_getServersInRegion(region);
            if (null == fullKernelList) {
                // Kernel Servers not available in the region
                return;
            }

            /* Get the list of servers on which replicas already exist */
            ArrayList<InetSocketAddress> sappObjReplicatedKernelList =
                    new ArrayList<InetSocketAddress>();
            for (SapphireServerPolicy tmp : servers) {
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
                    addReplica(servers.get(0), fullKernelList.get(availableKernelCount - 1));
                }
                return;
            }

            // Cannot create replica of the sapphire object. All the kernel servers have its replica
            return;
        }
    }
}
