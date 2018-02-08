package sapphire.policy.mobility.explicitmigration;

import java.net.InetSocketAddress;

/**
 * Created by Malepati Bala Siva Sai Akhil on 6/2/18.
 */

// making the class final, so that this class is non-extendable
public final class ExplicitMigrationPolicyTestConstants {

    // hiding the constructor
    private ExplicitMigrationPolicyTestConstants() {}

    // regularRPC() test case related constants
    public static final InetSocketAddress regularRPC_testDestAddr = new InetSocketAddress("192.168.42.146", 22342);

    // basicExplicitMigration() test case related constants
    public static final InetSocketAddress basicExplicitMigration_localServerAddr = new InetSocketAddress("192.168.42.145", 22342);
    public static final InetSocketAddress basicExplicitMigration_kernelServerAddr1 = new InetSocketAddress("192.168.42.145", 22342);
    public static final InetSocketAddress basicExplicitMigration_kernelServerAddr2 = new InetSocketAddress("192.168.42.146", 22342);
    public static final InetSocketAddress basicExplicitMigration_testDestAddr = new InetSocketAddress("192.168.42.146", 22342);

    // destinationNotFoundExplicitMigration() test case related constants
    public static final InetSocketAddress destinationNotFoundExplicitMigration_localServerAddr = new InetSocketAddress("192.168.42.146", 22342);
    public static final InetSocketAddress destinationNotFoundExplicitMigration_kernelServerAddr1 = new InetSocketAddress("192.168.42.146", 22342);
    public static final InetSocketAddress destinationNotFoundExplicitMigration_kernelServerAddr2 = new InetSocketAddress("192.168.42.147", 22342);
    public static final InetSocketAddress destinationNotFoundExplicitMigration_kernelServerAddr3 = new InetSocketAddress("192.168.42.148", 22342);
    public static final InetSocketAddress destinationNotFoundExplicitMigration_testDestAddr = new InetSocketAddress("192.168.42.145", 22342);

    // retryMigrateObjectRPCFromClient() test case related constants
    public static final InetSocketAddress retryMigrateObjectRPCFromClient_localServerAddr = new InetSocketAddress("192.168.42.145", 22342);
    public static final InetSocketAddress retryMigrateObjectRPCFromClient_kernelServerAddr1 = new InetSocketAddress("192.168.42.145", 22342);
    public static final InetSocketAddress retryMigrateObjectRPCFromClient_kernelServerAddr2 = new InetSocketAddress("192.168.42.146", 22342);
    public static final InetSocketAddress retryMigrateObjectRPCFromClient_testDestAddr = new InetSocketAddress("192.168.42.146", 22342);
}
