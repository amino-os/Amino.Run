package amino.run.sampleSO;

/** Created by Venugopal Reddy K on 6/9/18. */
import amino.run.app.SapphireObject;
import amino.run.runtime.SapphireConfiguration;

@SapphireConfiguration(Policies = "amino.run.policy.DefaultSapphirePolicy")
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
