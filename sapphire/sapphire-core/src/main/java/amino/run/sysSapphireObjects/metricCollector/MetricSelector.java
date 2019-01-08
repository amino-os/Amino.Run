package amino.run.sysSapphireObjects.metricCollector;

import java.io.Serializable;

public interface MetricSelector extends Serializable {
    String getName();

    Object getMetricSelector();
}
