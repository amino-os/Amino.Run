package sapphire.policy.checkpoint.explicitcheckpoint;

import java.io.Serializable;

/**
 * Created by quinton on 1/16/18.
 */

public class ExplicitCheckpointerTest extends ExplicitCheckpointerImpl {
    int i = 0;
    public void setI(int i) {
        this.i = i;
    }
    public int getI() {
        return i;
    }
}
