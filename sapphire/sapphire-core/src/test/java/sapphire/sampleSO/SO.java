package sapphire.sampleSO;

/** Created by Venugopal Reddy K on 6/9/18. */
import sapphire.app.SapphireObject;
import sapphire.runtime.SapphireConfiguration;

@SapphireConfiguration(Policies = "sapphire.policy.DefaultSapphirePolicy")
public class SO implements SapphireObject {
    public Integer i = 1;

    public Integer getI() {
        return i;
    }

    public void setI(Integer value) {
        i = value;
    }

    public void incI() {
        i++;
    }

    public void decI() {
        i--;
    }

    public Integer getIDelayed() {
        try {
            Thread.sleep(15);
        } catch (InterruptedException e) {

        }
        return i;
    }
}
