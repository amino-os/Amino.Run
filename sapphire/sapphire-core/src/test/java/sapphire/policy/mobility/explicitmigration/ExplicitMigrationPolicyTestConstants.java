package sapphire.policy.mobility.explicitmigration;

import java.net.InetSocketAddress;

/** Created by Malepati Bala Siva Sai Akhil on 6/2/18. */

// making the class final, so that this class is non-extendable
public final class ExplicitMigrationPolicyTestConstants {

    // hiding the constructor
    private ExplicitMigrationPolicyTestConstants() {}

    // unique InetSocketAddresses for use in different tests
    public static final InetSocketAddress localServerAddr =
            new InetSocketAddress("192.168.42.146", 22342);

    public static final InetSocketAddress kernelServerAddr1 =
            new InetSocketAddress("192.168.42.147", 22342);

    public static final InetSocketAddress kernelServerAddr2 =
            new InetSocketAddress("192.168.42.148", 22342);

    public static final InetSocketAddress kernelServerAddr3 =
            new InetSocketAddress("192.168.42.149", 22342);

    public static final InetSocketAddress kernelServerAddr4 =
            new InetSocketAddress("192.168.42.150", 22342);
}
