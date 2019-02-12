package amino.run.common.ArgumentParser;

import com.google.common.net.InetAddresses;
import com.google.devtools.common.options.Converter;
import com.google.devtools.common.options.OptionsParsingException;

/** Custom parsing type to handle the IP Address */
public class IPParser implements Converter {
    /**
     * Validates the given input is in ipv4 or ipv6 format
     *
     * @param input
     * @return
     * @throws OptionsParsingException
     */
    @Override
    public String convert(String input) throws OptionsParsingException {
        if (InetAddresses.isInetAddress(input)) {
            return input;
        }
        throw new OptionsParsingException(input + " is not a valid IP");
    }
    /** @return description of Param Type */
    @Override
    public String getTypeDescription() {
        return "a IP address string";
    }
}
