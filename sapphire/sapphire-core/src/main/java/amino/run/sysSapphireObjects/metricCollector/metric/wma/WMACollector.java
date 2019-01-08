package amino.run.sysSapphireObjects.metricCollector.metric.wma;

import amino.run.app.labelselector.Labels;
import amino.run.app.labelselector.Selector;
import amino.run.sysSapphireObjects.metricCollector.Collector;
import amino.run.sysSapphireObjects.metricCollector.Metric;
import amino.run.sysSapphireObjects.metricCollector.MetricSelector;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class WMACollector implements Collector {
    private Labels mandatoryLabels;
    private ConcurrentHashMap<Labels, WMAMetricAggregator> collector;
    private int bucketSize;

    public WMACollector(Labels labels, int bucketSize) {
        this.mandatoryLabels = labels;
        this.bucketSize = bucketSize;
        this.collector = new ConcurrentHashMap<>();
    }

    @Override
    public void collect(Metric metric) throws Exception {
        if (!(metric instanceof WMAMetric)) {
            throw new Exception("invalid collector");
        }
        WMAMetric clientMetric = (WMAMetric) metric.getMetric();
        WMAMetricAggregator serverMetric = collector.get(clientMetric.getLabels());

        if (serverMetric == null) {
            serverMetric =
                    new WMAMetricAggregator(
                            clientMetric.getName(), clientMetric.getLabels(), bucketSize);
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
        if (!(oSelector instanceof WMAMetricSelector)) {
            throw new Exception("invalid selector");
        }

        WMAMetricSelector gMetricSelector = (WMAMetricSelector) oSelector;
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
