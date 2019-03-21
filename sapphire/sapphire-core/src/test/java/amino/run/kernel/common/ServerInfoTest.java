package amino.run.kernel.common;

import org.junit.Test;

public class ServerInfoTest {
    private static String LABEL_PREFIX = "label_";
    private static String NON_EXISTENT_LABEL = "non_existent_label";
    private int numOfLabels = 5;

    @Test(expected = NullPointerException.class)
    public void addLabelsWithNull() {
        ServerInfo server = new ServerInfo(null);
        server.addLabels(null);
    }
    // TODO add Server info test for node selection
}
