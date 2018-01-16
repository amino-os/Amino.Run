package sapphire.policy.checkpoint.explicitcheckpoint;

import java.io.Serializable;

/**
 * Created by quinton on 1/16/18.
 */

public class ExplicitCheckpointerTest extends ExplicitCheckpointerImpl implements Serializable {
    int i = 0;
    String s = "";

    public void setI(int i) {
        this.i = i;
    }

    public void setS(String s) {
        this.s = s;
    }

    public int getI() {
        return i;
    }

    public String getS() {
        return s;
    }
}
