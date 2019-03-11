package amino.run.common.ArgumentParser;

import com.google.devtools.common.options.Converters;

/** Custom parsing type to handle the Ports */
public class PortParser extends Converters.RangeConverter {
    private static int MIN_VALUE = 0;
    private static int MAX_VALUE = 65535;

    /** Initializing the RangeConverter Class constructor with min and max port values */
    public PortParser() {
        super(MIN_VALUE, MAX_VALUE);
    }
}
