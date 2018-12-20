package sapphire.app;

import java.io.Serializable;
import java.util.Objects;
import java.util.logging.Logger;
import org.yaml.snakeyaml.Yaml;
import sapphire.common.LabelUtils;

/**
 * Specification for node selection. Users use {@code NodeSelectorSpec} to specify on which nodes
 * (i.e. kernel servers) to run their sapphire object.
 *
 * <p>At present we only support specifying {@code NodeSelectorSpec} at sapphire object level. We
 * will consider support specifying {@code NodeSelectorSpec} at DM level in the future if necessary.
 *
 * <p>{@code NodeSelectorSpec} contains a {@code ksAffinity} KsAffinity is considered as selector
 * used to select nodes.
 *
 * <p>By default {@code ksAffinity} KsAffinity is empty which means no selector will be applied in
 * which case all nodes will be returned.
 */
public class NodeSelectorSpec implements Serializable {
    private KsAffinity ksAffinity = null;
    private static Logger logger = Logger.getLogger(NodeSelectorSpec.class.getName());

    public KsAffinity getNodeAffinity() {
        return ksAffinity;
    }

    public void setNodeAffinity(KsAffinity ksAffinity) throws IllegalArgumentException {
        if (!LabelUtils.validateNodeAffinity(ksAffinity)) {
            logger.severe("null or empty nodeAffinity or invalid nodeAffinity are not allowed");
            throw new IllegalArgumentException("Invalid input Argument not allowed " + ksAffinity);
        }
        this.ksAffinity = ksAffinity;
    }

    @Override
    public String toString() {
        Yaml yaml = new Yaml();
        return yaml.dump(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeSelectorSpec that = (NodeSelectorSpec) o;
        return Objects.equals(this.ksAffinity, that.ksAffinity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ksAffinity);
    }
}
