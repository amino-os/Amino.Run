package amino.run.policy;

import amino.run.kernel.common.KernelObjectStub;
import java.io.Serializable;

/**
 * Contains object reference for the given policyName: group, server, serverStub, client This
 * information is used when kernel or DM tries to create/replicate the DM chain such as linking the
 * current DM with the outer or inner DM (i.e., serverPolicyStub->next client) It is also used to
 * find reference to already created group policy when creating a replica.
 */
public class PolicyContainer implements Serializable {
    public String policyName;
    public Policy.GroupPolicy groupPolicy;
    public Policy.ServerPolicy serverPolicy;
    public KernelObjectStub serverPolicyStub;
    public Policy.ClientPolicy clientPolicy;

    public PolicyContainer(String policyName) {
        this.policyName = policyName;
    }
}
