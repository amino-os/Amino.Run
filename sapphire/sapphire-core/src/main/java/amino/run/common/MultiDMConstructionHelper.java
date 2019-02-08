package amino.run.common;

import amino.run.app.DMSpec;
import amino.run.app.MicroServiceSpec;
import amino.run.policy.SapphirePolicyContainer;
import amino.run.runtime.SapphireConfiguration;
import java.lang.annotation.Annotation;
import java.util.*;

/** Helper class for construction of multi-DM chain. */
public class MultiDMConstructionHelper {
    /**
     * Creates a policy name chain based on annotations and return list of policy names.
     *
     * @param annotations Annotations that contain chain of policy names.
     * @return List of policy names.
     */
    public static List<String> getPolicyNameChain(Annotation[] annotations) {
        List<String> policyNameChain = new ArrayList<>();
        for (Annotation annotation : annotations) {
            if (annotation instanceof SapphireConfiguration) {
                String[] policyAnnotations = ((SapphireConfiguration) annotation).Policies();
                for (String policyAnnotation : policyAnnotations) {
                    String[] policyNames = policyAnnotation.split(",");
                    for (String policyName : policyNames) {
                        policyNameChain.add(policyName.trim());
                    }
                }
            }
        }

        return policyNameChain;
    }

    /**
     * Creates a policy name chain based on MicroServicerSpec and return list of policy names.
     *
     * @param spec
     * @return list of policy names
     */
    public static List<String> getPolicyNameChain(MicroServiceSpec spec) {
        List<String> policyNameChain = new ArrayList<>();
        for (DMSpec dm : spec.getDmList()) {
            policyNameChain.add(dm.getName());
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
            List<SapphirePolicyContainer> policyContainers, int startIdx) {
        List<String> policyNames = new ArrayList<>();
        for (int m = startIdx; m < policyContainers.size(); m++) {
            policyNames.add(policyContainers.get(m).getPolicyName());
        }

        return policyNames;
    }
}
