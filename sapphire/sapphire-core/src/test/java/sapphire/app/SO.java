package sapphire.app;

import sapphire.policy.scalability.LoadBalancedFrontendPolicy;
import sapphire.policy.scalability.ScaleUpFrontendPolicy;

/** Created by Venugopal Reddy K on 6/9/18. */
@ScaleUpFrontendPolicy.ScaleUpFrontendPolicyConfigAnnotation(
        replicationRateInMs = 20,
        loadbalanceConfig =
                @LoadBalancedFrontendPolicy.LoadBalancedFrontendPolicyConfigAnnotation(
                        maxconcurrentReq = 2,
                        replicacount = 2))
@LoadBalancedFrontendPolicy.LoadBalancedFrontendPolicyConfigAnnotation(
        maxconcurrentReq = 2,
        replicacount = 2)
public class SO implements SapphireObject {
    public Integer i = 1;

    public Integer getI() {
        return i;
    }

    public void setI(Integer value) {
        i = value;
    }

    public Integer getIDelayed() {
        try {
            Thread.sleep(15);
        } catch (InterruptedException e) {

        }
        return i;
    }
}
