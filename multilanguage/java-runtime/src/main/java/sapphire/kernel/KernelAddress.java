package sapphire.kernel;

/**
 * Created by Jithu Thomas on 15/6/18.
 */
public class KernelAddress {

    // Kernel_client - Address of remote Kernel gRPC server.
    // Kernel_server - Address of local Kernel gRPC server.
    static final String kernel_server_host = "localhost";
    static final int kernel_server_port = 8080;

    // Kernel_client - Address of local application gRPC server.
    static final String app_server_host = "localhost";
    static final int app_server_port = 8081;

    // Kernel_server - Address of Sapphire Process gRPC server.
    static final String sapphire_process_host = "localhost";
    static final int sapphire_process_port = 7000;
}
