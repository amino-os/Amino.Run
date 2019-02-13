package amino.run.runtime.util;

import amino.run.common.SapphireObjectCreationException;
import amino.run.common.SapphireObjectID;
import amino.run.common.SapphireObjectNotFoundException;
import amino.run.kernel.common.GlobalKernelReferences;
import amino.run.kernel.common.KernelObjectNotCreatedException;
import amino.run.policy.DefaultPolicy;
import amino.run.policy.Policy;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.logging.Logger;

/** Collection of helper methods that are necessary for creating policy instances. */
public class PolicyCreationHelper {
    static Logger logger = java.util.logging.Logger.getLogger(PolicyCreationHelper.class.getName());

    /**
     * Creates group policy and returns it after catching the exceptions properly. This method
     * should be called as a part of creating policy chain from kernel or library.
     *
     * @param policyName
     * @param sapphireObjId
     * @return
     * @throws SapphireObjectCreationException
     */
    public static Policy.GroupPolicy createGroupPolicy(
            String policyName, SapphireObjectID sapphireObjId)
            throws SapphireObjectCreationException {
        try {
            /* Create the Kernel Object for the Group Policy and get the Group Policy Stub from OMS */
            Class<?> sapphireGroupPolicyClass =
                    getPolicyMap(policyName).get("sapphireGroupPolicyClass");

            Policy.GroupPolicy groupPolicyStub =
                    GlobalKernelReferences.nodeServer.oms.createGroupPolicy(
                            sapphireGroupPolicyClass, sapphireObjId);

            return groupPolicyStub;
        } catch (ClassNotFoundException
                | KernelObjectNotCreatedException
                | RemoteException
                | SapphireObjectNotFoundException e) {
            logger.severe("Failed to create a group policy: " + policyName);
            throw new SapphireObjectCreationException(e);
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
                policyMap.put("sapphireServerPolicyClass", c);
                continue;
            }
            if (Policy.ClientPolicy.class.isAssignableFrom(c)) {
                policyMap.put("sapphireClientPolicyClass", c);
                continue;
            }
            if (Policy.GroupPolicy.class.isAssignableFrom(c)) {
                policyMap.put("sapphireGroupPolicyClass", c);
                continue;
            }
        }

        /* If no policies specified use the defaults */
        if (!policyMap.containsKey("sapphireServerPolicyClass"))
            policyMap.put("sapphireServerPolicyClass", DefaultPolicy.DefaultServerPolicy.class);
        if (!policyMap.containsKey("sapphireClientPolicyClass"))
            policyMap.put("sapphireClientPolicyClass", DefaultPolicy.DefaultClientPolicy.class);
        if (!policyMap.containsKey("sapphireGroupPolicyClass"))
            policyMap.put("sapphireGroupPolicyClass", DefaultPolicy.DefaultGroupPolicy.class);

        return policyMap;
    }
}
