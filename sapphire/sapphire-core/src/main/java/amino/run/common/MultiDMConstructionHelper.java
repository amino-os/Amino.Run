package amino.run.common;

import amino.run.app.DMSpec;
import amino.run.app.MicroServiceSpec;
import amino.run.policy.PolicyContainer;
import java.util.*;

/** Helper class for construction of multi-DM chain. */
public class MultiDMConstructionHelper {
    /**
     * Creates a policy name chain based on MicroServicerSpec and return list of policy names.
     *
     * @param spec
     * @param idx starting index of spec where DM lists should be created from
     * @return list of policy names
     */
    public static List<String> getPolicyNameChain(MicroServiceSpec spec, int idx) {
        List<String> policyNameChain = new ArrayList<>();
        List<DMSpec> dmList = spec.getDmList();
        for (int i = idx; i < dmList.size(); i++) {
            policyNameChain.add(dmList.get(i).getName());
        }

        return policyNameChain;
    }

    /**
     * Gets the policy names from the policyContainers.
     *
     * @param policyContainers
     * @param startIdx
     * @return list of policy names
     */
    public static List<String> getPolicyNames(
            List<PolicyContainer> policyContainers, int startIdx) {
        List<String> policyNames = new ArrayList<>();
        for (int m = startIdx; m < policyContainers.size(); m++) {
            policyNames.add(policyContainers.get(m).getPolicyName());
        }

        return policyNames;
    }
}
