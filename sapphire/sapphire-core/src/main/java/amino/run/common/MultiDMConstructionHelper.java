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
     * @return list of policy names
     */
    public static List<String> getPolicyNameChain(MicroServiceSpec spec) {
        List<String> policyNameChain = new ArrayList<String>();
        List<DMSpec> dmList = spec.getDmList();
        for (int i = 0; i < dmList.size(); i++) {
            policyNameChain.add(dmList.get(i).getName());
        }

        return policyNameChain;
    }

    /**
     * Gets the policy names from the policyContainers.
     *
     * @param policyContainers
     * @return list of policy names
     */
    public static List<String> getPolicyNames(List<PolicyContainer> policyContainers) {
        List<String> policyNames = new ArrayList<String>();
        for (int m = 0; m < policyContainers.size(); m++) {
            policyNames.add(policyContainers.get(m).policyName);
        }

        return policyNames;
    }
}
