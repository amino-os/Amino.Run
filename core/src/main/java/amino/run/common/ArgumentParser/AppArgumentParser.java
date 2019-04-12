package amino.run.common.ArgumentParser;

import com.google.devtools.common.options.Option;

/** To Parse the Arguments required for App */
public class AppArgumentParser extends KernelServerArgumentParser {

    @Option(
            name = "app-args",
            help = "Additional parameters required for APP",
            defaultValue = "",
            category = "startup")
    public String appArgs;
}
