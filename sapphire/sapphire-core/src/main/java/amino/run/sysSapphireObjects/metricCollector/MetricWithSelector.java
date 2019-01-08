package amino.run.sysSapphireObjects.metricCollector;

public interface MetricWithSelector {
    Metric getMetric(MetricSelector metricSelector);
}
