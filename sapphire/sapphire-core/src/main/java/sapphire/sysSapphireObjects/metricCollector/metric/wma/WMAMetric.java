package sapphire.sysSapphireObjects.metricCollector.metric.wma;

import java.util.ArrayList;
import java.util.TimerTask;
import java.util.logging.Logger;
import sapphire.app.labelselector.Labels;
import sapphire.policy.util.ResettableTimer;
import sapphire.sysSapphireObjects.metricCollector.Metric;
import sapphire.sysSapphireObjects.metricCollector.SendMetric;

/**
 * Weighted moving average (wma) gives weighted average of the last n values, where the weighting
 * decreases with each previous value.
 */
public class WMAMetric implements Metric {
    private static Logger logger = Logger.getLogger(WMAMetric.class.getName());
    private String metricName;
    private Labels labels;
    private int bucketSize;
    private ArrayList<Observation> list;
    private float wmaNew;
    private ArrayList<Float> wmaValues;
    private float total = 0;
    private float numerator = 0;
    private long metricUpdateFrequency;
    private transient ResettableTimer metricSendTimer;
    private SendMetric metricAggregator;

    private WMAMetric(
            String metricName,
            Labels labels,
            int bucketSize,
            long metricUpdateFrequency,
            SendMetric sendMetric) {
        this.metricName = metricName;
        this.labels = labels;
        this.bucketSize = bucketSize;
        this.metricUpdateFrequency = metricUpdateFrequency;
        metricAggregator = sendMetric;
        this.list = new ArrayList<>();
        this.wmaValues = new ArrayList<>();
    }

    private WMAMetric(String metricName, Labels labels, ArrayList<Float> wmaValues) {
        this.metricName = metricName;
        this.labels = labels;
        this.wmaValues = wmaValues;
    }

    @Override
    public String getName() {
        return metricName;
    }

    @Override
    public Object getMetric() {
        return this;
    }

    /** @return true if wmaValues is modified */
    public boolean modified() {
        return wmaValues.size() != 0;
    }

    /** resets wmaValues */
    public void reset() {
        synchronized (this) {
            wmaValues.clear();
        }
    }

    /**
     * Calculates weighted moving average whenever a new value is added to the bucket
     *
     * @param list List of observedValues
     * @param oldvalue Previous value
     * @return
     */
    public float calculateWMA(ArrayList<Observation> list, float oldvalue) {
        int size = (list.size() < bucketSize) ? list.size() : bucketSize;
        float totalNew = total + (list.get(size - 1).getValue()) - oldvalue;
        float numeratorNew = numerator + (size * list.get(size - 1).getValue()) - total;
        int denominator = (size * (size + 1)) / 2;
        wmaNew = numeratorNew / denominator;
        total = totalNew;
        numerator = numeratorNew;
        return wmaNew;
    }

    /** starts a ResettableTimer and resets once metricUpdateFrequency is reached */
    public void start() {
        metricSendTimer =
                new ResettableTimer(
                        new TimerTask() {
                            public void run() {
                                try {
                                    if (modified()) {
                                        metricAggregator.send(WMAMetric.this);
                                    }
                                } catch (Exception e) {
                                    logger.warning(
                                            String.format(
                                                    "%s: Sending metric failed", e.toString()));
                                    return;
                                }
                                // reset the wmaValues and timer after push is done
                                reset();
                                metricSendTimer.reset();
                            }
                        },
                        metricUpdateFrequency);
        metricSendTimer.start();
    }

    /** stops the timer */
    public void stop() {
        metricSendTimer.cancel();
    }

    /**
     * Sets the value and calculate wma for the value
     *
     * @param time time when the value is added in a particular bucket
     * @param value Value to be added
     */
    public void setValue(long time, float value) {
        synchronized (this) {
            float oldvalue = (list.isEmpty()) ? 0 : list.get(list.size() - 1).getValue();
            Observation observation = new Observation(time, value);
            if (list.size() == bucketSize) {
                oldvalue = list.get(0).getValue();
                list.remove(0);
            }
            list.add(observation);
            wmaNew = calculateWMA(list, oldvalue);
            wmaValues.add(wmaNew);
        }
    }

    /** @return labels */
    public Labels getLabels() {
        return labels;
    }

    /** @return wmaValues */
    public ArrayList<Float> getValue() {
        return wmaValues;
    }

    public static WMAMetric.Builder newBuilder() {
        return new WMAMetric.Builder();
    }

    public static class Builder {
        private int bucketSize;
        private int batchSize;
        private String metricName;
        private Labels labels;
        private long metricUpdateFrequency;
        private SendMetric sendMetric;
        private ArrayList<Float> wmaValues;

        public WMAMetric.Builder setBucketSize(int bucketSize) {
            this.bucketSize = bucketSize;
            return this;
        }

        public WMAMetric.Builder setMetricName(String metricName) {
            this.metricName = metricName;
            return this;
        }

        public WMAMetric.Builder setLabels(Labels labels) {
            this.labels = labels;
            return this;
        }

        public WMAMetric.Builder setvalues(ArrayList<Float> wmaValues) {
            this.wmaValues = wmaValues;
            return this;
        }

        public WMAMetric.Builder setFrequency(long metricUpdateFrequency) {
            this.metricUpdateFrequency = metricUpdateFrequency;
            return this;
        }

        public WMAMetric.Builder setSendMetric(SendMetric sendMetric) {
            this.sendMetric = sendMetric;
            return this;
        }

        public WMAMetric create() {
            WMAMetric wmaMetric;
            if (metricUpdateFrequency == 0 && sendMetric == null) {
                wmaMetric = new WMAMetric(metricName, labels, wmaValues);
            } else {
                wmaMetric =
                        new WMAMetric(
                                metricName, labels, bucketSize, metricUpdateFrequency, sendMetric);
                wmaMetric.start();
            }
            return wmaMetric;
        }
    }
}
