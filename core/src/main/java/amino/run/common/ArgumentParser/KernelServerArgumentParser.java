package amino.run.common.ArgumentParser;

import com.google.devtools.common.options.Option;
import java.util.HashMap;
import java.util.Map;

/** To Parse the Arguments required for Kernel Server process */
public class KernelServerArgumentParser extends OMSArgumentParser {
    @Option(
            name = "kernel-server-ip",
            help = "kernel server ip",
            defaultValue = "127.0.0.1",
            converter = Converters.IPConverter.class,
            category = "startup")
    public String kernelServerIP;

    @Option(
            name = "kernel-server-port",
            help = "kernel server port",
            defaultValue = "22345",
            category = "startup",
            converter = Converters.PortConverter.class)
    public Integer kernelServerPort;

    @Option(
            name = "labels",
            abbrev = 'l',
            help = "Label for kernel server ",
            defaultValue = "region=default-region",
            converter = Converters.LabelConverter.class,
            category = "startup")
    public Map<String, String> labels = new HashMap<String, String>();

    @Option(
            name = "metric-server",
            help = "metric server",
            defaultValue = "logger",
            category = "startup")
    public String metricServer;

    @Option(
            name = "metric-server-ip-port",
            help = "metric server ip port",
            defaultValue = "127.0.0.1:9091",
            category = "startup")
    public String metricServerIpAndPort;
}
