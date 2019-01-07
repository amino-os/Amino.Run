package sapphire.sysSapphireObjects.metricCollector;

public interface MetricWithSelector {
    Metric getMetric(MetricSelector metricSelector);
}
