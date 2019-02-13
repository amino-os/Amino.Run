package amino.run.app.labelselector;

import java.io.Serializable;
import java.util.*;

/**
 * Labels are list of key-value pair used for objects tagging.
 *
 * <p>Application can use {@link Selector} to filter tagged objects.
 */
public class Labels extends HashMap<String, String> implements Serializable {
    /**
     * Create {@link Selector} with available labels.
     *
     * @return return {@link Selector} object
     */
    public Selector asSelector() throws RequirementInvalidException {
        Selector selector = new Selector();
        Requirement req;

        for (Map.Entry<String, String> label : super.entrySet()) {
            req =
                    new Requirement(
                            label.getKey().trim(),
                            Requirement.Equal,
                            new ArrayList<>(Collections.singletonList(label.getValue())));
            selector.add(req);
        }

        return selector;
    }
}
