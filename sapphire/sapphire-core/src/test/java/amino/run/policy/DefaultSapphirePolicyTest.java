package amino.run.policy;

import amino.run.kernel.common.KernelOID;
import java.rmi.RemoteException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DefaultSapphirePolicyTest {
    private int cnt;
    private Thread[] addThreads;
    private Thread[] delThreads;
    private DefaultSapphirePolicy.DefaultServerPolicy[] servers;
    private DefaultSapphirePolicy.DefaultGroupPolicy group;

    @Before
    public void setup() throws Exception {
        cnt = 1000;
        group = new DefaultSapphirePolicy.DefaultGroupPolicy();
        addThreads = new Thread[cnt];
        delThreads = new Thread[cnt];
        servers = new DefaultSapphirePolicy.DefaultServerPolicy[cnt];
        for (int i = 0; i < addThreads.length; i++) {
            servers[i] = new DefaultSapphirePolicy.DefaultServerPolicy();
            servers[i].$__setKernelOID(new KernelOID(i));
        }

        for (int i = 0; i < addThreads.length; i++) {
            final DefaultSapphirePolicy.DefaultServerPolicy s = servers[i];
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
            final DefaultSapphirePolicy.DefaultServerPolicy s = servers[i];
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
