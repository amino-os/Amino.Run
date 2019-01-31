package amino.run.sampleSO;

/** Created by Venugopal Reddy K on 6/9/18. */
import amino.run.app.AbstractSapphireObject;
import amino.run.runtime.SapphireConfiguration;

@SapphireConfiguration(Policies = "amino.run.policy.DefaultPolicy")
public class SO extends AbstractSapphireObject {
    boolean status = true;
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

    public void incI(Integer value) {
        i += value;
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

    @Override
    public boolean getStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }
}
