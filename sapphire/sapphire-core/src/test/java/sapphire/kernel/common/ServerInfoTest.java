package sapphire.kernel.common;

import static org.junit.Assert.*;

import org.junit.Assert;
import org.junit.Test;

public class ServerInfoTest {

    @Test(expected = IllegalArgumentException.class)
    public void testAddLabelWithNullKey() {
        ServerInfo info = new ServerInfo(null, null);
        info.addLabel(null, "val");
    }

    @Test
    public void testGetLabelWithNull() {
        ServerInfo info = new ServerInfo(null, null);
        Assert.assertNull(info.getLabelValue(null));
    }

    @Test
    public void testRemoveLabelWithNull() {
        ServerInfo info = new ServerInfo(null, null);
        Assert.assertNull(info.removeLabel(null));
    }

    @Test(expected = NullPointerException.class)
    public void addLabelsWithNull() {
        ServerInfo info = new ServerInfo(null, null);
        info.addLabels(null);
    }
}
