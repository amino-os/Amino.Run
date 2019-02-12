package amino.run.common.ArgumentParser;

import com.google.devtools.common.options.Converter;
import com.google.devtools.common.options.OptionsParsingException;
import java.util.HashMap;
import java.util.Map;

/** Custom parsing type to handle the map data */
public class LabelParser implements Converter {
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
        String[] labelArr = input.split(",", -1);
        String mapKey;
        String mapValue;
        int idx;
        // TODO : After the label parameters are finalized the exception need to be handled
        for (String arg : labelArr) {
            // Splitting off the key from the value
            idx = arg.indexOf('=');
            if (idx > 0) {
                mapKey = arg.substring(0, idx);
                mapValue = arg.substring(idx + 1);
                if (mapValue.length() == 0) mapValue = "";
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
