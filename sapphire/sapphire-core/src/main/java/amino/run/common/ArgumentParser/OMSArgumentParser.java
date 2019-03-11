package amino.run.common.ArgumentParser;

import com.google.devtools.common.options.Option;
import com.google.devtools.common.options.OptionsBase;

/* To Parse the Arguments required for OMS process */
public class OMSArgumentParser extends OptionsBase {
    @Option(
            name = "oms-ip",
            help = "oms server ip",
            defaultValue = "127.0.0.1",
            converter = IPParser.class,
            category = "startup")
    public String omsIP;
    // TODO: Implementing the  Range converter type for all ports fields
    @Option(
            name = "oms-port",
            help = "oms server port",
            defaultValue = "22222",
            category = "startup",
            converter = PortParser.class)
    public Integer omsPort;

    @Option(
            name = "service-port",
            help = "service port ",
            defaultValue = "0",
            category = "startup",
            converter = PortParser.class)
    public Integer servicePort;
}
