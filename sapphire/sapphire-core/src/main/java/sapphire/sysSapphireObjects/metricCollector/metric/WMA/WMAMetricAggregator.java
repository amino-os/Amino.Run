package sapphire.sysSapphireObjects.metricCollector.metric.WMA;

import java.util.ArrayList;
import java.util.logging.Logger;
import sapphire.app.labelselector.Labels;
import sapphire.sysSapphireObjects.metricCollector.Metric;

public class WMAMetricAggregator implements Metric {
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
    public String getName() {
        return metricName;
    }

    @Override
    public Object getMetric() {
        return this;
    }

    public void merge(WMAMetric metric) {
        logger.info("Received metric : " + metric.toString());
        synchronized (this) {
            wmaValues = metric.getValue();
        }
        logger.info("Collected metric : " + this.toString());
    }
}
