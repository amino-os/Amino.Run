package sapphire.kernel.common;

import static org.junit.Assert.*;

import org.junit.Assert;
import org.junit.Test;

public class ServerInfoTest {

    @Test(expected = NullPointerException.class)
    public void addLabelsWithNull() {
        ServerInfo info = new ServerInfo(null, null);
        info.addLabels(null);
    }
}
