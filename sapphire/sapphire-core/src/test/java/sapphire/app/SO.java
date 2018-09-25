package sapphire.app;

/** Created by Venugopal Reddy K on 6/9/18. */
import sapphire.runtime.SapphireConfiguration;

@SapphireConfiguration(Policies = "sapphire.policy.DefaultSapphirePolicy")
public class SO {
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
