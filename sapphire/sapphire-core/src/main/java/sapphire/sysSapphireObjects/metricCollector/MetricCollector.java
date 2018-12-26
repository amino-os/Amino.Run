package sapphire.sysSapphireObjects.metricCollector;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import sapphire.app.SapphireObject;

/** A Metric collector where keys are {@code String}s and values are {@code Serializable}s. */
public class MetricCollector implements SapphireObject {
    private ConcurrentHashMap<String, Collector> metricCollectors;

    public MetricCollector() {
        metricCollectors = new ConcurrentHashMap<>();
    }

    public boolean push(Metric metric) throws Exception {
        if (metric == null) return false;
        String metricName = metric.getName();
        System.out.println(String.format("<Server>: pushing %s", metricName));

        Collector collector = metricCollectors.get(metricName);
        if (collector == null) {
            // TODO define new exception
            throw new Exception("metric not registered");
        }

        synchronized (collector) {
            collector.collect(metric);
        }

        return true;
    }

    public ArrayList<Metric> get(MetricSelector selector) throws Exception {
        if (selector == null) {
            // TODO define new exception
            throw new Exception("invalid selector");
        }

        String metricName = selector.getName();
        System.out.println(String.format("<Server>: getting  %s", metricName));

        Collector collector = metricCollectors.get(metricName);
        if (collector == null) {
            // TODO define new exception
            throw new Exception("metric not registered");
        }

        synchronized (collector) {
            return collector.retrieve(selector);
        }
    }

    public Boolean Register(MetricSchema schema) throws Exception {
        if (schema == null) return false;
        String metricName = schema.getName();
        System.out.println(
                String.format("<Server>: registering new metric with key: %s", metricName));

        Collector collector = metricCollectors.get(metricName);
        if (collector != null) {
            // TODO define new exception
            throw new Exception("metric already registered");
        }

        metricCollectors.put(metricName, schema.getCollector());
        return true;
    }
}
