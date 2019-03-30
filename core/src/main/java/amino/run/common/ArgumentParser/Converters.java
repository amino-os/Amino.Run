package amino.run.common.ArgumentParser;

import com.google.common.net.InetAddresses;
import com.google.devtools.common.options.Converter;
import com.google.devtools.common.options.OptionsParsingException;
import java.util.HashMap;
import java.util.Map;

public final class Converters {

    /** Custom Converter type to handle the IP Address */
    public static class IPConverter implements Converter {
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

    /** Custom Converter type to handle the map data */
    public static class LabelConverter implements Converter {
        private static String OPT_SEPARATOR = "=";
        private static String LABEL_SEPARATOR = ",";

        /**
         * Converts passed string data to map
         *
         * @param input
         * @return
         * @throws OptionsParsingException
         */
        @Override
        public Map<String, String> convert(String input) throws OptionsParsingException {
            Map<String, String> labelMap = new HashMap<String, String>();
            if (input.equals("")) {
                return labelMap;
            }
            String[] labelArr = input.split(LABEL_SEPARATOR, -1);
            String mapKey;
            String mapValue;
            int idx;
            // TODO : After the label parameters are finalized the exception need to be handled
            for (String arg : labelArr) {
                // Splitting off the key from the value
                idx = arg.indexOf(OPT_SEPARATOR);
                if (idx > 0) {
                    mapKey = arg.substring(0, idx);
                    mapValue = arg.substring(idx + 1);
                    labelMap.put(mapKey, mapValue);
                } else {
                    throw new OptionsParsingException(
                            "Variable definitions must be in the form of a "
                                    + "'name=value' assignment");
                }
            }
            return labelMap;
        }

        /** @return description of Param Type */
        @Override
        public String getTypeDescription() {
            // TODO: 1. Filling all the available in get type description
            return "Label for kernel server";
        }
    }

    /** Custom Converter type to handle the Port data */
    public static class PortConverter
            extends com.google.devtools.common.options.Converters.RangeConverter {
        private static int MIN_VALUE = 0;
        private static int MAX_VALUE = 65535;

        /** Initializing the RangeConverter Class constructor with min and max port values */
        public PortConverter() {
            super(MIN_VALUE, MAX_VALUE);
        }
    }
}
