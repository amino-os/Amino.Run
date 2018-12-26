package sapphire.sysSapphireObjects.metricCollector;

import java.util.ArrayList;

public interface Collector {
    void collect(Metric metric) throws Exception;

    ArrayList<Metric> retrieve(MetricSelector selector) throws Exception;
}
