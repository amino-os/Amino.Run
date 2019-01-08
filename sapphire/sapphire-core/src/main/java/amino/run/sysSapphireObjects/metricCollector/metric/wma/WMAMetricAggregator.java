package amino.run.sysSapphireObjects.metricCollector.metric.wma;

import amino.run.app.labelselector.Labels;
import amino.run.sysSapphireObjects.metricCollector.Metric;
import amino.run.sysSapphireObjects.metricCollector.MetricSelector;
import amino.run.sysSapphireObjects.metricCollector.MetricWithSelector;
import java.util.ArrayList;
import java.util.logging.Logger;

public class WMAMetricAggregator implements MetricWithSelector {
    private static Logger logger = Logger.getLogger(WMAMetricAggregator.class.getName());
    private String metricName;
    private ArrayList<Float> wmaValues;
    private Labels labels;
    private ArrayList<Observation> list;
    private ArrayList<Float> observedValues;
    private int bucketSize;
    private float wmaNew;
    private float total = 0;
    private float numerator = 0;

    public WMAMetricAggregator(String metricName, Labels labels, int bucketSize) {
        this.metricName = metricName;
        this.labels = labels;
        this.observedValues = new ArrayList<>();
        this.wmaValues = new ArrayList<>();
        this.bucketSize = bucketSize;
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

    @Override
    public String toString() {
        return metricName + "<" + labels.toString() + ":" + wmaValues + ">";
    }

    /**
     * Add value one by one from the received metric and merge it with the aggregator
     *
     * @param metric
     */
    public void merge(WMAMetric metric) {
        synchronized (this) {
            logger.info("Received metric : " + metric.toString());
            list = metric.getValue();
            for (Observation i : list) {
                float oldValue =
                        (observedValues.isEmpty())
                                ? 0
                                : observedValues.get(observedValues.size() - 1);
                if (observedValues.size() == bucketSize) {
                    oldValue = observedValues.get(0);
                    observedValues.remove(0);
                }
                float currentValue = i.getValue();
                observedValues.add(currentValue);
                wmaNew = calculateWMA(observedValues, oldValue);
                wmaValues.add(wmaNew);
            }
            logger.info("Collected metric : " + this.toString());
        }
    }

    /**
     * Calculates WMA whenever a value is added to the bucket
     *
     * @param observedValues
     * @param oldValue
     * @return calculated WMA
     */
    public float calculateWMA(ArrayList<Float> observedValues, float oldValue) {
        int size = (observedValues.size() < bucketSize) ? observedValues.size() : bucketSize;
        float totalNew = total + (observedValues.get(size - 1)) - oldValue;
        float numeratorNew = numerator + (size * observedValues.get(size - 1)) - total;
        int denominator = (size * (size + 1)) / 2;
        wmaNew = numeratorNew / denominator;
        total = totalNew;
        numerator = numeratorNew;
        return wmaNew;
    }
}
