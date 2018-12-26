package sapphire.sysSapphireObjects.metricCollector.metric.counter;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import sapphire.app.labelselector.Labels;
import sapphire.app.labelselector.Selector;
import sapphire.sysSapphireObjects.metricCollector.Collector;
import sapphire.sysSapphireObjects.metricCollector.Metric;
import sapphire.sysSapphireObjects.metricCollector.MetricSelector;

public class CounterCollector implements Collector {
    private Labels mandatoryLabels;
    private ConcurrentHashMap<Labels, CounterMetric> collector;

    public CounterCollector(Labels labels) {
        this.mandatoryLabels = labels;
        this.collector = new ConcurrentHashMap<>();
    }

    @Override
    public void collect(Metric metric) throws Exception {
        if (!(metric instanceof CounterMetric)) {
            throw new Exception("invalid collector");
        }

        CounterMetric clientMetric = (CounterMetric) metric.getMetric();

        // TODO check for mandatory labels

        CounterMetric serverMetric = collector.get(clientMetric.getLabels());
        if (serverMetric == null) {
            collector.put(clientMetric.getLabels(), clientMetric);
        }

        serverMetric.merge(clientMetric);
    }

    @Override
    public ArrayList<Metric> retrieve(MetricSelector metricSelector) throws Exception {
        ArrayList<Metric> metricList = new ArrayList<>();
        Object oSelector = metricSelector.getMetricSelector();
        if (!(oSelector instanceof CounterMetricSelector)) {
            throw new Exception("invalid selector");
        }

        CounterMetricSelector cMetricSelector = (CounterMetricSelector) oSelector;
        Selector selector = cMetricSelector.getSelector();

        collector.forEach(
                (labels, collector) -> {
                    if (selector.matches(labels)) {
                        metricList.add(collector);
                    }
                });

        return metricList;
    }
}
