package sapphire.policy.scalability;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import sapphire.common.Utils;
import sapphire.kernel.common.GlobalKernelReferences;
import sapphire.kernel.common.KernelObjectNotFoundException;
import sapphire.kernel.common.KernelObjectStub;
import sapphire.policy.DefaultSapphirePolicy;
import sapphire.policy.scalability.masterslave.Lock;
import sapphire.policy.scalability.masterslave.MethodInvocationRequest;
import sapphire.policy.scalability.masterslave.MethodInvocationResponse;
import sapphire.policy.scalability.masterslave.ReplicationRequest;
import sapphire.policy.scalability.masterslave.ReplicationResponse;
import sapphire.runtime.annotations.RuntimeSpec;

import static sapphire.policy.scalability.masterslave.MethodInvocationResponse.ReturnCode;
/**
 * Base class for LoadBalancedMasterSlave DM
 * @author terryz
 */
public abstract class LoadBalancedMasterSlaveBase extends DefaultSapphirePolicy {
    /**
     * Base implementation of client side policy
     */
    public abstract static class ClientBase extends DefaultClientPolicy {
        private static Logger logger = Logger.getLogger(ClientBase.class.getName());
        private final AtomicLong SeqGenerator = new AtomicLong();
        private final UUID CLIENT_ID;

        public ClientBase() {
            CLIENT_ID = UUID.randomUUID();
        }

        @Override
        public Object onRPC(String method, ArrayList<Object> params) throws Exception {
            final int MAX_RETRY=5;
            int retryCnt=1, waitInMilliseconds=50;
            GroupBase group = (GroupBase) getGroup();
            MethodInvocationRequest.MethodType type = MethodInvocationRequest.MethodType.MUTABLE;

            do {
                ServerBase server = (ServerBase)getServer();
                if (isImmutable(server.getClass(), method)) {
                    server = group.getRandomServer();
                    type = MethodInvocationRequest.MethodType.IMMUTABLE;
                } else {
                    server = group.getMaster();
                }

                if (server == null) {
                    logger.log(Level.INFO, "failed to get server with {0} MAX_RETRY", retryCnt);
                    Thread.sleep(waitInMilliseconds);
                    waitInMilliseconds <<= 1;
                    continue;
                }

                MethodInvocationRequest request = new MethodInvocationRequest(
                        CLIENT_ID.toString(),
                        SeqGenerator.getAndAdd(1L),
                        method,
                        params,
                        type);

                MethodInvocationResponse response = server.onRPC(request);
                switch (response.getReturnCode()) {
                    case SUCCESS:
                        return response.getResult();
                    case FAILURE:
                        logger.log(Level.WARNING, "failed to execute request {0}: {1}",
                                new Object[]{request, response});
                        throw (Exception)response.getResult();
                    case REDIRECT:
                        Thread.sleep(waitInMilliseconds);
                        waitInMilliseconds <<= 1;
                }
            } while (++retryCnt <= MAX_RETRY);

            throw new Exception(String.format("failed to execute method %s after retries", method));
        }

        private boolean isImmutable(Class clazz, String method) {
            Method[] methods = clazz.getMethods();
            for (Method m : methods) {
                if (m.toGenericString().equals(method)) {
                    return Utils.isImmutableMethod(m);
                }
            }

            return false;
        }
    }

    /**
     * Base implementation of master slave server policy
     */
    public abstract static class ServerBase extends DefaultServerPolicy {
        /**
         * @return the ID of the server
         */
        public String getServerId() {
            return $__getKernelOID().toString();
        }

        /**
         * Starts the server
         */
        public abstract void start();

        /**
         * Handles replication requests from other servers
         *
         * @param request replication request
         * @return replication response
         */
        public abstract ReplicationResponse handleReplication(ReplicationRequest request);

        /**
         * Update the App object with the given object and updates the commit index
         * accordingly.
         *
         * @param object
         * @param largestCommittedIndex
         */
        public abstract void syncObject(Serializable object, long largestCommittedIndex);

        /**
         * Invokes the given request on App object
         *
         * @param request method invocation request
         * @return method invocation response
         */
        public MethodInvocationResponse onRPC(MethodInvocationRequest request) {
            try {
                Object ret = sapphire_getAppObject().invoke(request.getMethodName(), request.getParams());
                return new MethodInvocationResponse(ReturnCode.SUCCESS, ret);
            } catch (Exception e) {
                return new MethodInvocationResponse(ReturnCode.FAILURE, e);
            }
        }
    }

    /**
     * Base implementation of master/slave group policy
     *
     * TODO (Terry): Make Group Policy highly available.
     * At present group policy has only one instance which
     * does not satisfy the high available requirement.
     */
    public abstract static class GroupBase extends DefaultGroupPolicy {
        private Logger logger;
        private Random random = new Random(System.currentTimeMillis());
        private Lock masterLock;

        @Override
        public void onCreate(SapphireServerPolicy server, Annotation[] annotations) {
            logger = Logger.getLogger(this.getClass().getName());
            this.addServer(server);

            RuntimeSpec spec = Utils.getRuntimeSpec(server.getClass());
            try {
                ArrayList<InetSocketAddress> servers = GlobalKernelReferences.nodeServer.oms.getServers();
                // TODO: Remove the following check. Use heap to find the best server location.
                if (servers.size() <= spec.replicas()) {
                    throw new IllegalStateException(String.format("server# (%s) <= replicas# (%s)",
                            servers.size(), spec.replicas()));
                }

                List<InetSocketAddress> unavailable = new ArrayList<InetSocketAddress>();
                unavailable.add(GlobalKernelReferences.nodeServer.oms.lookupKernelObject($__getKernelOID()));
                InetSocketAddress dest = getAvailable(servers, unavailable);

                ServerBase s = (ServerBase) server;
                s.sapphire_pin_to_server(dest);
                ((KernelObjectStub) s).$__updateHostname(dest);
                unavailable.add(dest);
                s.start();
                logger.info("created master on " + dest);

                for (int i=0; i<spec.replicas()-1; i++) {
                    dest = getAvailable(servers, unavailable);
                    ServerBase replica = (ServerBase)s.sapphire_replicate();
                    replica.sapphire_pin_to_server(dest);
                    ((KernelObjectStub) replica).$__updateHostname(dest);
                    removeServer(replica);
                    addServer(replica);
                    replica.start();
                    unavailable.add(dest);
                    logger.info("created slave on " + dest);
                }
            } catch (RemoteException e) {
                throw new RuntimeException("failed to create group: " + e, e);
            } catch (KernelObjectNotFoundException e) {
                throw new RuntimeException("unable to find kernel object: "+e, e);
            } catch (NotBoundException e) {
                throw new Error("rmi operation not bound: "+e, e);
            }
        }

        InetSocketAddress getAvailable(List<InetSocketAddress> servers, List<InetSocketAddress> unavailable) {
            for (InetSocketAddress s : servers) {
                if (! unavailable.contains(s)) {
                    return s;
                }
            }
            return null;
        }

        /**
         * Renew lock
         *
         * @param serverId Id of the server
         * @return <code>true</code> if lock renew succeeds; <code>false</code> otherwise
         */
        public boolean renewLock(String serverId) {
            if (serverId == null || serverId.isEmpty()) {
                throw new IllegalArgumentException("server ID not specified");
            }

            if (masterLock == null) {
                return false;
            }

            return masterLock.renew(serverId);
        }

        /**
         * Obtain lock
         *
         * @param serverId the Id of the server
         * @param masterLeaseTimeoutInMillis
         * @return <code>true</code> if lock is granted; <code>false</code> otherwise
         */
        public boolean obtainLock(String serverId, long masterLeaseTimeoutInMillis) {
            if (masterLock == null) {
                masterLock = new Lock(serverId, masterLeaseTimeoutInMillis);
                return true;
            }

            return masterLock.obtain(serverId);
        }

        /**
         * @return master server, or <code>null</code> if no master available
         */
        public ServerBase getMaster() {
            // TODO: cache master to avoid calling group policy remotely for RPC
            if (masterLock != null) {
                List<ServerBase> servers = getServerPolicies();
                for (ServerBase s : servers) {
                    try {
                        if (s.getServerId() != null && s.getServerId().equals(masterLock.getClientId())) {
                            return s;
                        }
                    } catch (Throwable e) {
                        logger.log(Level.WARNING, "unable to get master from group: " + e.getMessage());
                    }
                }
            }
            return null;
        }

        public ServerBase getSlave() {
            ServerBase master = getMaster();
            List<ServerBase> servers = getServerPolicies();
            for (ServerBase s : servers) {
                if (! s.equals(master)) {
                    return s;
                }
            }
            return null;
        }

        private List<ServerBase> getServerPolicies() {
            ArrayList<SapphireServerPolicy> servers = super.getServers();
            List<ServerBase> result = new ArrayList<ServerBase>();
            for (SapphireServerPolicy s: servers) {
                result.add((ServerBase) s);
            }
            return result;
        }

        public ServerBase getRandomServer() {
            List<ServerBase> servers = getServerPolicies();
            return servers.get(random.nextInt(Integer.MAX_VALUE)%servers.size());
        }
    }
}
