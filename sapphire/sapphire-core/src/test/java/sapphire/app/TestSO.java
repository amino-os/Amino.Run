package sapphire.app;

public class TestSO implements SapphireObject {
    public Integer i = 0;

    public void incVal() {
        i++;
    }

    public void decVal() {
        i--;
    }

    public Integer getVal() {
        return i;
    }
}
