package sapphire.sysSapphireObjects.metricCollector.metric.wma;

import java.util.ArrayList;
import java.util.logging.Logger;
import sapphire.app.labelselector.Labels;
import sapphire.sysSapphireObjects.metricCollector.Metric;
import sapphire.sysSapphireObjects.metricCollector.MetricSelector;
import sapphire.sysSapphireObjects.metricCollector.MetricWithSelector;

public class WMAMetricAggregator implements MetricWithSelector {
    private static Logger logger = Logger.getLogger(WMAMetricAggregator.class.getName());
    private String metricName;
    private ArrayList<Float> wmaValues;
    private Labels labels;

    public WMAMetricAggregator(String metricName, Labels labels) {
        this.metricName = metricName;
        this.labels = labels;
        this.wmaValues = new ArrayList<>();
    }

    @Override
    public Metric getMetric(MetricSelector metricSelector) {
        WMAMetric wmaMetric =
                WMAMetric.newBuilder()
                        .setMetricName(metricName)
                        .setLabels(labels)
                        .setvalues(wmaValues)
                        .create();
        return wmaMetric;
    }

    public void merge(WMAMetric metric) {
        logger.info("Received metric : " + metric.toString());
        synchronized (this) {
            wmaValues = metric.getValue();
        }
        logger.info("Collected metric : " + this.toString());
    }
}
