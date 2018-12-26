package sapphire.sysSapphireObjects.metricCollector;

import java.io.Serializable;

public interface Metric extends Serializable {
    String getName();

    Object getMetric();
}
