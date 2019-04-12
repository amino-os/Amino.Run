package amino.run.policy;

import amino.run.common.MicroServiceID;
import amino.run.common.ReplicaID;
import amino.run.kernel.common.KernelOID;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DefaultPolicyTest {
    private int cnt;
    private Thread[] addThreads;
    private Thread[] delThreads;
    private DefaultPolicy.DefaultServerPolicy[] servers;
    private DefaultPolicy.DefaultGroupPolicy group;

    @Before
    public void setup() throws Exception {
        cnt = 1000;
        group = new DefaultPolicy.DefaultGroupPolicy();
        MicroServiceID serviceId = new MicroServiceID(UUID.randomUUID());
        addThreads = new Thread[cnt];
        delThreads = new Thread[cnt];
        servers = new DefaultPolicy.DefaultServerPolicy[cnt];
        for (int i = 0; i < addThreads.length; i++) {
            servers[i] = new DefaultPolicy.DefaultServerPolicy();
            servers[i].$__setKernelOID(new KernelOID(i));
            servers[i].setReplicaId(new ReplicaID(serviceId, UUID.randomUUID()));
        }

        for (int i = 0; i < addThreads.length; i++) {
            final DefaultPolicy.DefaultServerPolicy s = servers[i];
            addThreads[i] =
                    new Thread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    group.addServer(s);
                                }
                            });
        }

        for (int i = 0; i < delThreads.length; i++) {
            final DefaultPolicy.DefaultServerPolicy s = servers[i];
            delThreads[i] =
                    new Thread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    group.removeServer(s);
                                }
                            });
        }
    }

    @Test
    public void testAddRemoveServer() throws Exception {
        // start addServer threads
        for (int i = 0; i < addThreads.length; i++) {
            addThreads[i].start();
        }
        for (int i = 0; i < addThreads.length; i++) {
            addThreads[i].join();
        }

        // verify server count
        Assert.assertEquals(cnt, group.getServers().size());

        // start delete server threads
        for (int i = 0; i < delThreads.length; i++) {
            delThreads[i].start();
        }

        for (int i = 0; i < delThreads.length; i++) {
            delThreads[i].join();
        }

        // verify server count
        Assert.assertEquals(0, group.getServers().size());
    }
}
