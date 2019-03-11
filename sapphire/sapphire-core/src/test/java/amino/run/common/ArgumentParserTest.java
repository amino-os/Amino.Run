package amino.run.common;

import static junit.framework.TestCase.assertEquals;

import amino.run.common.ArgumentParser.AppArgumentParser;
import amino.run.common.ArgumentParser.KernelServerArgumentParser;
import amino.run.common.ArgumentParser.OMSArgumentParser;
import amino.run.kernel.server.KernelServerImpl;
import amino.run.oms.OMSServerImpl;
import com.google.devtools.common.options.OptionsParser;
import com.google.devtools.common.options.OptionsParsingException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ArgumentParserTest {
    @Rule public ExpectedException thrown = ExpectedException.none();

    /* Verifying the OMSArgumentParser */
    @Test
    public void testOmsArgumentParser() throws Exception {
        OptionsParser parser = OptionsParser.newOptionsParser(OMSArgumentParser.class);
        parser.parse(
                OMSServerImpl.OMS_IP_OPT,
                "127.0.0.1",
                OMSServerImpl.OMS_PORT_OPT,
                "30000",
                OMSServerImpl.SERVICE_PORT,
                "20000");
        OMSArgumentParser omsArgs = parser.getOptions(OMSArgumentParser.class);
        assertEquals(omsArgs.omsIP, "127.0.0.1");
        assertEquals(omsArgs.omsPort, new Integer(30000));
        assertEquals(omsArgs.servicePort, new Integer(20000));
    }

    /* To check the wrong option usage */
    @Test
    public void testWrongOptionUsage() throws Exception {
        OptionsParser parser = OptionsParser.newOptionsParser(OMSArgumentParser.class);
        thrown.expect(OptionsParsingException.class);
        parser.parse("--oms-IP", "127.0.0.1", OMSServerImpl.OMS_PORT_OPT, "30000");
    }

    /* Validating IP format */
    @Test
    public void testWrongIPUsage() throws Exception {
        OptionsParser parser = OptionsParser.newOptionsParser(OMSArgumentParser.class);
        thrown.expect(OptionsParsingException.class);
        parser.parse(OMSServerImpl.OMS_IP_OPT, "127.0.0", OMSServerImpl.OMS_PORT_OPT, "30000");
    }

    /* Validating Port format */
    @Test
    public void testWrongPortUsage() throws Exception {
        OptionsParser parser = OptionsParser.newOptionsParser(OMSArgumentParser.class);
        thrown.expect(OptionsParsingException.class);
        parser.parse(OMSServerImpl.OMS_IP_OPT, "127.0.0.1", OMSServerImpl.OMS_PORT_OPT, "port");
    }

    /* Verifying Minimum and Maximum value of port */
    @Test
    public void testPortRange() throws Exception {
        OptionsParser parser = OptionsParser.newOptionsParser(OMSArgumentParser.class);
        thrown.expect(OptionsParsingException.class);
        parser.parse(OMSServerImpl.OMS_IP_OPT, "127.0.0.1", OMSServerImpl.OMS_PORT_OPT, "-1");
        thrown.expect(OptionsParsingException.class);
        parser.parse(OMSServerImpl.OMS_IP_OPT, "127.0.0.1", OMSServerImpl.OMS_PORT_OPT, "90000");
    }

    /* Verifying the Kernel Server Argument parser */
    @Test
    public void testKernelServerArgumentParser() throws Exception {
        OptionsParser parser = OptionsParser.newOptionsParser(KernelServerArgumentParser.class);
        parser.parse(
                KernelServerImpl.KERNEL_SERVER_IP_OPT,
                "2607:f0d0:1002:0051:0000:0000:0000:0004",
                KernelServerImpl.KERNEL_SERVER_PORT_OPT,
                "20000",
                OMSServerImpl.OMS_IP_OPT,
                "2607:f0d0:1002:0051:0000:0000:0000:0004",
                OMSServerImpl.OMS_PORT_OPT,
                "30000",
                OMSServerImpl.SERVICE_PORT,
                "50000",
                KernelServerImpl.LABEL_OPT,
                "region=r1,environment=dev");
        KernelServerArgumentParser ksArgs = parser.getOptions(KernelServerArgumentParser.class);
        /* Verifying IPV6 Format */
        assertEquals(ksArgs.kernelServerIP, "2607:f0d0:1002:0051:0000:0000:0000:0004");
        assertEquals(ksArgs.kernelServerPort, new Integer(20000));
        assertEquals(ksArgs.omsIP, "2607:f0d0:1002:0051:0000:0000:0000:0004");
        assertEquals(ksArgs.omsPort, new Integer(30000));
        assertEquals(ksArgs.servicePort, new Integer(50000));
        assertEquals(ksArgs.labels.get(KernelServerImpl.REGION_KEY), "r1");
        assertEquals(ksArgs.labels.get("environment"), "dev");
    }

    /* Validating label Format */
    @Test
    public void testWrongLabelUsage() throws Exception {
        OptionsParser parser = OptionsParser.newOptionsParser(KernelServerArgumentParser.class);
        thrown.expect(OptionsParsingException.class);
        parser.parse(KernelServerImpl.LABEL_OPT, "=r1,environment=dev");
    }

    /* Verifying the default values */
    @Test
    public void testDefaultValues() throws Exception {
        OptionsParser parser = OptionsParser.newOptionsParser(KernelServerArgumentParser.class);
        parser.parse(
                KernelServerImpl.KERNEL_SERVER_IP_OPT,
                "127.0.0.1",
                KernelServerImpl.KERNEL_SERVER_PORT_OPT,
                "20000",
                OMSServerImpl.OMS_IP_OPT,
                "127.0.0.1",
                OMSServerImpl.OMS_PORT_OPT,
                "30000");
        KernelServerArgumentParser ksArgs = parser.getOptions(KernelServerArgumentParser.class);
        assertEquals(
                ksArgs.labels.get(KernelServerImpl.REGION_KEY), KernelServerImpl.DEFAULT_REGION);
        assertEquals(ksArgs.servicePort, new Integer(0));
    }

    /* Verifying different label usage */
    @Test
    public void testLabelUsage() throws Exception {
        OptionsParser parser = OptionsParser.newOptionsParser(KernelServerArgumentParser.class);
        parser.parse(KernelServerImpl.LABEL_OPT, "");
        KernelServerArgumentParser ksArgs = parser.getOptions(KernelServerArgumentParser.class);
        assertEquals(ksArgs.labels.isEmpty(), true);
        parser.parse(KernelServerImpl.LABEL_OPT, "region=,environment=dev");
        ksArgs = parser.getOptions(KernelServerArgumentParser.class);
        assertEquals(ksArgs.labels.get(KernelServerImpl.REGION_KEY), "");
        assertEquals(ksArgs.labels.get("environment"), "dev");
    }

    /* Verifying the App Argument Parser */
    @Test
    public void testAppArgumentParser() throws Exception {
        OptionsParser parser = OptionsParser.newOptionsParser(AppArgumentParser.class);
        parser.parse("--app-args", "Test");
        AppArgumentParser appArgs = parser.getOptions(AppArgumentParser.class);
        assertEquals(appArgs.appArgs, "Test");
    }
}
