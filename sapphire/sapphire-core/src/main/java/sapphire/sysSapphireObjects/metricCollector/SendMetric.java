package sapphire.sysSapphireObjects.metricCollector;

public interface SendMetric {
    void send(Metric metric) throws Exception;
}
