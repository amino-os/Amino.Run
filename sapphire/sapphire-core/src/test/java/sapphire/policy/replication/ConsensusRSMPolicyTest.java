package sapphire.policy.replication;

import org.junit.Assert;
import org.junit.Test;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import sapphire.common.Utils;

import static org.junit.Assert.*;

/**
 * Created by terryz on 4/9/18.
 */
public class ConsensusRSMPolicyTest {

    @Test
    public void testSerialization() throws Exception {
        ConsensusRSMPolicy.ClientPolicy client = new ConsensusRSMPolicy.ClientPolicy();
        Assert.assertNotNull(Utils.ObjectCloner.deepCopy(client));

        ConsensusRSMPolicy.GroupPolicy group = new ConsensusRSMPolicy.GroupPolicy();
        Assert.assertNotNull(Utils.ObjectCloner.deepCopy(group));

        ConsensusRSMPolicy.ServerPolicy server = new ConsensusRSMPolicy.ServerPolicy();
        // TODO: Fix all serialization issues
        // Unit test will fail after uncommenting the following statement.
        // The current Sapphire core requires that server policies and group policies
        // have to be serializable.
//        server.onCreate(group);
        Assert.assertNotNull(Utils.ObjectCloner.deepCopy(server));
    }
}