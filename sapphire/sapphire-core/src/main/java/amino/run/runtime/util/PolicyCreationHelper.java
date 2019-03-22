package amino.run.runtime.util;

import amino.run.app.MicroServiceSpec;
import amino.run.common.MicroServiceCreationException;
import amino.run.common.MicroServiceID;
import amino.run.common.MicroServiceNotFoundException;
import amino.run.common.MicroServiceReplicaNotFoundException;
import amino.run.kernel.common.GlobalKernelReferences;
import amino.run.kernel.common.KernelObjectNotCreatedException;
import amino.run.kernel.common.KernelObjectStub;
import amino.run.kernel.server.KernelServerImpl;
import amino.run.policy.DefaultPolicy;
import amino.run.policy.Policy;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.logging.Logger;

/** Collection of helper methods that are necessary for creating policy instances. */
public class PolicyCreationHelper {
    public static final String GroupPolicyClass = "GroupPolicyClass";
    public static final String ServerPolicyClass = "ServerPolicyClass";
    public static final String ClientPolicyClass = "ClientPolicyClass";

    static Logger logger = java.util.logging.Logger.getLogger(PolicyCreationHelper.class.getName());

    /**
     * Creates group policy and returns it after catching the exceptions properly. This method
     * should be called as a part of creating policy chain from kernel or library.
     *
     * @param policyName
     * @param microServiceId
     * @param spec
     * @return
     * @throws amino.run.common.MicroServiceCreationException
     */
    public static Policy.GroupPolicy createGroupPolicy(
            String policyName, MicroServiceID microServiceId, MicroServiceSpec spec)
            throws MicroServiceCreationException {
        try {
            /* Create the Kernel Object for the Group Policy and get the Group Policy Stub from OMS */
            Class<?> groupPolicyClass = getPolicyMap(policyName).get(GroupPolicyClass);

            Policy.GroupPolicy groupPolicyStub =
                    GlobalKernelReferences.nodeServer.oms.createGroupPolicy(
                            groupPolicyClass, microServiceId, spec);

            return groupPolicyStub;
        } catch (ClassNotFoundException e) {
            logger.severe("Failed to create a group policy: " + policyName);
            throw new MicroServiceCreationException(e);
        } catch (KernelObjectNotCreatedException e) {
            logger.severe("Failed to create a group policy: " + policyName);
            throw new MicroServiceCreationException(e);
        } catch (MicroServiceNotFoundException e) {
            logger.severe("Failed to create a group policy: " + policyName);
            throw new MicroServiceCreationException(e);
        } catch (RemoteException e) {
            logger.severe("Failed to create a group policy: " + policyName);
            throw new MicroServiceCreationException(e);
        }
    }

    /**
     * Constructs a policy map for each client, server and group policy based on input policy name.
     *
     * @param policyName
     * @return hash map for (policy name, actual policy class name)
     * @throws Exception
     */
    public static HashMap<String, Class<?>> getPolicyMap(String policyName)
            throws ClassNotFoundException {
        HashMap<String, Class<?>> policyMap = new HashMap<String, Class<?>>();
        Class<?> policy = Class.forName(policyName);

        /* Extract the policy component classes (server, client and group) */
        Class<?>[] policyClasses = policy.getDeclaredClasses();

        /* TODO (Sungwook, 2018-10-2) Collapse into a smaller code for statements below
        E.g. policyClass in (Server, Client, Group) {..}
        */
        for (Class<?> c : policyClasses) {
            if (Policy.ServerPolicy.class.isAssignableFrom(c)) {
                policyMap.put(ServerPolicyClass, c);
                continue;
            }
            if (Policy.ClientPolicy.class.isAssignableFrom(c)) {
                policyMap.put(ClientPolicyClass, c);
                continue;
            }
            if (Policy.GroupPolicy.class.isAssignableFrom(c)) {
                policyMap.put(GroupPolicyClass, c);
                continue;
            }
        }

        /* If no policies specified use the defaults */
        if (!policyMap.containsKey(ServerPolicyClass))
            policyMap.put(ServerPolicyClass, DefaultPolicy.DefaultServerPolicy.class);
        if (!policyMap.containsKey(ClientPolicyClass))
            policyMap.put(ClientPolicyClass, DefaultPolicy.DefaultClientPolicy.class);
        if (!policyMap.containsKey(GroupPolicyClass))
            policyMap.put(GroupPolicyClass, DefaultPolicy.DefaultGroupPolicy.class);

        return policyMap;
    }

    /**
     * Last DM should always try to pin the original Microservice based on the host address assigned
     * on the stub if it was not pinned by any DMs.
     *
     * @param serverPolicy server policy which should be the innermost.
     * @throws MicroServiceCreationException
     */
    public static void pinOriginalMicroservice(Policy.ServerPolicy serverPolicy)
            throws MicroServiceCreationException {
        InetSocketAddress address = null;

        try {
            address = ((KernelObjectStub) serverPolicy).$__getHostname();

            KernelServerImpl ks = GlobalKernelReferences.nodeServer;
            if (address != null && !address.equals(ks.getLocalHost())) {
                serverPolicy.pin_to_server(address);
            }
        } catch (RemoteException e) {
            logger.severe(
                    String.format(
                            "Failed to pin original Microservice to %s due to Remote Exception to %s",
                            address, serverPolicy));
            throw new MicroServiceCreationException(e);
        } catch (MicroServiceNotFoundException e) {
            logger.severe("Failed to pin original Microservice to " + address);
            throw new MicroServiceCreationException(e);
        } catch (MicroServiceReplicaNotFoundException e) {
            logger.severe(
                    String.format(
                            "Failed to pin original Microservice to %s because replica was not found.",
                            address));
            throw new MicroServiceCreationException(e);
        }
    }
}
