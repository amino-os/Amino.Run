package sapphire.sysSapphireObjects.metricCollector.metric.gauge;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import sapphire.app.labelselector.Labels;
import sapphire.app.labelselector.Selector;
import sapphire.sysSapphireObjects.metricCollector.Collector;
import sapphire.sysSapphireObjects.metricCollector.Metric;
import sapphire.sysSapphireObjects.metricCollector.MetricSelector;

public class GaugeCollector implements Collector {
    private Labels mandatoryLabels;
    private ConcurrentHashMap<Labels, GaugeMetric> collector;

    public GaugeCollector(Labels labels) {
        this.mandatoryLabels = labels;
        this.collector = new ConcurrentHashMap<>();
    }

    @Override
    public void collect(Metric metric) throws Exception {
        if (!(metric instanceof GaugeMetric)) {
            throw new Exception("invalid collector");
        }
        GaugeMetric clientMetric = (GaugeMetric) metric.getMetric();
        GaugeMetric serverMetric = collector.get(clientMetric.getLabels());

        if (serverMetric == null) {
            collector.put(clientMetric.getLabels(), clientMetric);
        }
        serverMetric.merge(clientMetric);
    }

    @Override
    public ArrayList<Metric> retrieve(MetricSelector metricSelector) throws Exception {
        ArrayList<Metric> metricList = new ArrayList<>();
        Object oSelector = metricSelector.getMetricSelector();
        if (!(oSelector instanceof GaugeMetricSelector)) {
            throw new Exception("invalid selector");
        }

        GaugeMetricSelector gMetricSelector = (GaugeMetricSelector) oSelector;
        Selector selector = gMetricSelector.getSelector();

        collector.forEach(
                (labels, collector) -> {
                    if (selector.matches(labels)) {
                        metricList.add(collector);
                    }
                });

        return metricList;
    }
}
