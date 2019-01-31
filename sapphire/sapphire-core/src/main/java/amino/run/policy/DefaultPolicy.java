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
import java.util.*;

public class DefaultPolicy extends Policy {
    protected static int replicaCount = 3;

    public static class DefaultServerPolicy extends ServerPolicy {
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
        public void onCreate(GroupPolicy group, MicroServiceSpec spec) {
            this.group = (DefaultGroupPolicy) group;
        }
    }

    public static class DefaultGroupPolicy extends GroupPolicy {
        private ArrayList<ServerPolicy> servers = new ArrayList<ServerPolicy>();
        protected String region = "";
        protected MicroServiceSpec spec = null;
        protected int HEALTH_STATUS_MAX_SKIP_TICKS = 3;
        protected long HEALTH_STATUS_REPORT_INTERVAL = OMSServer.KS_HEARTBEAT_TIMEOUT;
        protected transient volatile int healthStatusTick = 1;
        protected transient ResettableTimer healthCheckTimer;

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
        public void onCreate(String region, ServerPolicy server, MicroServiceSpec spec)
                throws RemoteException {
            this.region = region;
            this.spec = spec;
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
        @AddEvent(event = "NOTIFY")
        public void onNotification(NotificationObject notificationObject) throws RemoteException {
            super.onNotification(notificationObject);
            if (!(notificationObject instanceof SapphireStatusObject)) {
                /* Unsupported notification received */
                return;
            }

            try {
                SapphireStatusObject statusObject = (SapphireStatusObject) notificationObject;
                KernelObjectStub server = (KernelObjectStub) getServer(statusObject.getServerId());
                ServerPolicy serverPolicy = getServer(statusObject.getServerId());
                if (statusObject.isStatus()) {
                    /* Mark the server as healthy */
                    server.$__setLastSeenTick(healthStatusTick);
                    return;
                }
                removeReplica(serverPolicy);
                addReplicas(region);
            } catch (KernelObjectNotFoundException e) {
                e.toString();
            } catch (MicroServiceNotFoundException e) {
                e.toString();
            } catch (MicroServiceReplicaNotFoundException e) {
                e.toString();
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

        /**
         * Check the status of appobject from each replica and add healthy ones in the list
         *
         * @return List of healthy replicas
         * @throws RemoteException
         */
        private ServerPolicy getHealthyReplicas() throws Exception {
            ArrayList<ServerPolicy> servers = getServers();
            for (ServerPolicy server : servers) {
                KernelObjectStub serverStub = (KernelObjectStub) server;
                // return healthy server
                if ((healthStatusTick - serverStub.$__getLastSeenTick())
                        <= HEALTH_STATUS_MAX_SKIP_TICKS) {
                    return server;
                }
            }
            throw new Exception("Healthy replica not available");
        }

        private void addReplicas(String region)
                throws RemoteException, MicroServiceNotFoundException,
                MicroServiceReplicaNotFoundException {
            ArrayList<ServerPolicy> servers = getServers();

            /* Get the list of available servers in region */
            List<InetSocketAddress> fullKernelList;
            fullKernelList = sapphire_getAddressList(null, region);
            if (fullKernelList.isEmpty()) {
                // Kernel Servers not available in the region
                return;
            }

            int remainingReplicaCount;
            remainingReplicaCount = replicaCount - servers.size();
            List<InetSocketAddress> ksToBeUsed = new ArrayList<>(fullKernelList);
            Set<InetSocketAddress> ksList = new HashSet<>();
            // add the kernelservers in which replica is already present in the list
            for (ServerPolicy s : servers) {
                ksList.add(s.sapphire_locate_kernel_object(s.$__getKernelOID()));
            }

            // remove the kernelserver in which replica is already present in order to make the
            // replicas distributed across kernel servers
            for (InetSocketAddress i : ksList) {
                ksToBeUsed.remove(i);
            }
            while (remainingReplicaCount > 0) {
                if (!ksToBeUsed.isEmpty()) {
                    InetSocketAddress currentKS = ksToBeUsed.get(0);
                    ServerPolicy replicaSource = null;
                    //adding new replica
                    try {
                        replicaSource = getHealthyReplicas();
                    } catch (Exception e){
                        logger.warning("Healthy replica not available");
                        return;
                    }
                    addReplica(replicaSource, currentKS, region, false);
                    ksToBeUsed.remove(currentKS);
                    remainingReplicaCount--;
                } else {
                    ksToBeUsed = fullKernelList;
                }
            }
        }
    }
}
