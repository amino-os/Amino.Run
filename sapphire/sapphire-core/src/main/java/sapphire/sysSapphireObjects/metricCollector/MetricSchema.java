package sapphire.sysSapphireObjects.metricCollector;

import java.io.Serializable;

public interface MetricSchema extends Serializable {
    String getName();

    Collector getCollector();
}
