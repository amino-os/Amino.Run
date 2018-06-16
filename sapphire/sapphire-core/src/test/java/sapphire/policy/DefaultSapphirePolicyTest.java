package sapphire.policy;

import static sapphire.policy.DefaultSapphirePolicy.DefaultGroupPolicy;
import static sapphire.policy.DefaultSapphirePolicy.DefaultServerPolicy;

import java.rmi.RemoteException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import sapphire.kernel.common.KernelOID;

public class DefaultSapphirePolicyTest {
    private int cnt;
    private Thread[] addThreads;
    private Thread[] delThreads;
    private DefaultServerPolicy[] servers;
    private DefaultGroupPolicy group;

    @Before
    public void setup() throws Exception {
        cnt = 1000;
        group = new DefaultGroupPolicy();
        addThreads = new Thread[cnt];
        delThreads = new Thread[cnt];
        servers = new DefaultServerPolicy[cnt];
        for (int i = 0; i < addThreads.length; i++) {
            servers[i] = new DefaultServerPolicy();
            servers[i].$__setKernelOID(new KernelOID(i));
        }

        for (int i = 0; i < addThreads.length; i++) {
            final DefaultServerPolicy s = servers[i];
            addThreads[i] =
                    new Thread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        group.addServer(s);
                                    } catch (RemoteException e) {
                                    }
                                }
                            });
        }

        for (int i = 0; i < delThreads.length; i++) {
            final DefaultServerPolicy s = servers[i];
            delThreads[i] =
                    new Thread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        group.removeServer(s);
                                    } catch (RemoteException e) {
                                    }
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
