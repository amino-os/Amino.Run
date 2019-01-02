package sapphire.sysSapphireObjects.metricCollector.metric.histogram;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import sapphire.app.labelselector.Labels;
import sapphire.app.labelselector.Selector;
import sapphire.sysSapphireObjects.metricCollector.Collector;
import sapphire.sysSapphireObjects.metricCollector.Metric;
import sapphire.sysSapphireObjects.metricCollector.MetricSelector;

public class HistogramCollector implements Collector {
    private Labels mandatoryLabels;
    private ConcurrentHashMap<Labels, HistogramServerMetric> collector;

    public HistogramCollector(Labels labels) {
        this.mandatoryLabels = labels;
        this.collector = new ConcurrentHashMap<>();
    }

    @Override
    public void collect(Metric metric) throws Exception {
        if (!(metric instanceof HistogramClientMetric)) {
            throw new Exception("invalid collector");
        }
        HistogramClientMetric clientMetric = (HistogramClientMetric) metric.getMetric();
        HistogramServerMetric serverMetric = collector.get(clientMetric.getLabels());

        if (serverMetric == null) {
            serverMetric =
                    new HistogramServerMetric(clientMetric.getName(), clientMetric.getLabels());
            collector.put(clientMetric.getLabels(), serverMetric);
        }
        serverMetric.merge(clientMetric);
    }

    @Override
    public ArrayList<Metric> retrieve(MetricSelector metricSelector) throws Exception {
        ArrayList<Metric> metricList = new ArrayList<>();
        Object oSelector = metricSelector.getMetricSelector();
        if (!(oSelector instanceof HistogramMetricSelector)) {
            throw new Exception("invalid selector");
        }

        HistogramMetricSelector gMetricSelector = (HistogramMetricSelector) oSelector;
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
