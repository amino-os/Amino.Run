package amino.run.sysSapphireObjects.metricCollector.metric.gauge;

import amino.run.app.labelselector.Labels;
import amino.run.app.labelselector.Selector;
import amino.run.sysSapphireObjects.metricCollector.Collector;
import amino.run.sysSapphireObjects.metricCollector.Metric;
import amino.run.sysSapphireObjects.metricCollector.MetricSelector;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class GaugeCollector implements Collector {
    private Labels mandatoryLabels;
    private ConcurrentHashMap<Labels, GaugeMetricAggregator> collector;

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
        GaugeMetricAggregator serverMetric = collector.get(clientMetric.getLabels());

        if (serverMetric == null) {
            serverMetric =
                    new GaugeMetricAggregator(clientMetric.getName(), clientMetric.getLabels());
            serverMetric.merge(clientMetric);
            collector.put(clientMetric.getLabels(), serverMetric);
            return;
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
                        metricList.add(collector.getMetric(metricSelector));
                    }
                });

        return metricList;
    }
}
