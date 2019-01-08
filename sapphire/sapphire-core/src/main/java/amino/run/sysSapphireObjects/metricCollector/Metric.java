package amino.run.sysSapphireObjects.metricCollector;

import java.io.Serializable;

public interface Metric extends Serializable {
    String getName();

    Metric getMetric();
}
