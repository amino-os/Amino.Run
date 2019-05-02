package amino.run.common;

import static junit.framework.TestCase.assertEquals;

import amino.run.common.ArgumentParser.AppArgumentParser;
import amino.run.common.ArgumentParser.KernelServerArgumentParser;
import amino.run.common.ArgumentParser.OMSArgumentParser;
import amino.run.kernel.server.KernelServerImpl;
import amino.run.oms.OMSServerImpl;
import com.google.devtools.common.options.OptionsParser;
import com.google.devtools.common.options.OptionsParsingException;
import org.junit.Test;

public class ArgumentParserTest {

    /* To check the wrong option usage */
    @Test(expected = OptionsParsingException.class)
    public void testWrongOptionUsage() throws Exception {
        OptionsParser parser = OptionsParser.newOptionsParser(OMSArgumentParser.class);
        /* Passing --oms-Ip instead --oms-ip */
        parser.parse("--oms-IP", "127.0.0.1", OMSServerImpl.OMS_PORT_OPT, "30000");
    }

    /* Validating IP format */
    @Test(expected = OptionsParsingException.class)
    public void testWrongIPUsage() throws Exception {
        OptionsParser parser = OptionsParser.newOptionsParser(OMSArgumentParser.class);
        parser.parse(OMSServerImpl.OMS_IP_OPT, "127.0.0", OMSServerImpl.OMS_PORT_OPT, "30000");
    }

    /* Verifying Minimum value of port */
    @Test(expected = OptionsParsingException.class)
    public void testPortLowerRange() throws Exception {
        OptionsParser parser = OptionsParser.newOptionsParser(OMSArgumentParser.class);
        parser.parse(OMSServerImpl.OMS_IP_OPT, "127.0.0.1", OMSServerImpl.OMS_PORT_OPT, "-1");
    }

    /* Verifying Maximum value of port */
    @Test(expected = OptionsParsingException.class)
    public void testPortUpperRange() throws Exception {
        OptionsParser parser = OptionsParser.newOptionsParser(OMSArgumentParser.class);
        parser.parse(OMSServerImpl.OMS_IP_OPT, "127.0.0.1", OMSServerImpl.OMS_PORT_OPT, "90000");
    }

    /* Verifying different label usage */
    @Test
    public void testLabelUsage() throws Exception {
        OptionsParser parser = OptionsParser.newOptionsParser(KernelServerArgumentParser.class);
        /* Blank label passed and validating the empty map */
        parser.parse(KernelServerImpl.LABEL_OPT, "");
        KernelServerArgumentParser ksArgs = parser.getOptions(KernelServerArgumentParser.class);
        assertEquals(ksArgs.labels.isEmpty(), true);

        /* Verifying the blank value for a key */
        parser.parse(KernelServerImpl.LABEL_OPT, "region=,environment=dev");
        ksArgs = parser.getOptions(KernelServerArgumentParser.class);
        assertEquals(ksArgs.labels.get(KernelServerImpl.REGION_KEY), "");
        assertEquals(ksArgs.labels.get("environment"), "dev");
    }

    /* Validating label Format  */
    @Test(expected = OptionsParsingException.class)
    public void testWrongLabelUsage() throws Exception {
        OptionsParser parser = OptionsParser.newOptionsParser(KernelServerArgumentParser.class);
        // Passing the empty key
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

    /* Verifying the App Argument Parser */
    @Test
    public void testAppArgumentParser() throws Exception {
        OptionsParser parser = OptionsParser.newOptionsParser(AppArgumentParser.class);
        parser.parse("--app-args", "Test");
        AppArgumentParser appArgs = parser.getOptions(AppArgumentParser.class);
        assertEquals(appArgs.appArgs, "Test");
    }
}
