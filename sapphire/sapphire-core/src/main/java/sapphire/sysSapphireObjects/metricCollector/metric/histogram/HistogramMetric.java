package sapphire.sysSapphireObjects.metricCollector.metric.histogram;

import java.util.LinkedHashMap;
import java.util.TimerTask;
import java.util.logging.Logger;
import sapphire.app.labelselector.Labels;
import sapphire.policy.util.ResettableTimer;
import sapphire.sysSapphireObjects.metricCollector.Metric;
import sapphire.sysSapphireObjects.metricCollector.SendMetric;

/** Histogram samples observations and maintains them in configurable buckets */
public class HistogramMetric implements Metric {
    private static Logger logger = Logger.getLogger(HistogramMetric.class.getName());

    private String metricName;
    private Labels labels;
    private int bucketSize;
    private LinkedHashMap<Long, Object> observedValues;
    private transient ResettableTimer metricSendTimer;
    private transient SendMetric metricAggregator;
    private transient long metricUpdateFrequency;

    private HistogramMetric(
            String metricName,
            Labels labels,
            int bucketSize,
            long metricUpdateFrequency,
            SendMetric sendMetric) {
        this.metricName = metricName;
        this.labels = labels;
        this.bucketSize = bucketSize;
        this.observedValues = new LinkedHashMap<>();
        this.metricUpdateFrequency = metricUpdateFrequency;
        metricAggregator = sendMetric;
    }

    private HistogramMetric(
            String metricName, Labels labels, LinkedHashMap<Long, Object> observedValues) {
        this.metricName = metricName;
        this.labels = labels;
        this.observedValues = observedValues;
    }

    @Override
    public String getName() {
        return metricName;
    }

    @Override
    public Metric getMetric() {
        return this;
    }

    /** @return true if some values are added in observedValues */
    public boolean modified() {
        return observedValues.size() != 0;
    }

    /** starts a ResettableTimer and resets once metricUpdateFrequency is reached */
    public void start() {
        metricSendTimer =
                new ResettableTimer(
                        new TimerTask() {
                            public void run() {
                                try {
                                    if (modified()) {
                                        metricAggregator.send(HistogramMetric.this);
                                    }
                                } catch (Exception e) {
                                    logger.warning(
                                            String.format(
                                                    "%s: Sending metric failed", e.toString()));
                                    return;
                                }
                                // reset the observedValues and timer after push is done
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

    /** reset the observedValues */
    public void reset() {
        synchronized (this) {
            observedValues.clear();
        }
    }

    /**
     * Sets the value in observedValues
     *
     * @param time time when the value is added in a particular bucket
     * @param value value to be added
     */
    public void setValue(long time, Object value) {
        synchronized (this) {
            observedValues.put(time, value);
        }
    }

    /** @return labels */
    public Labels getLabels() {
        return labels;
    }

    /** @return observedValues */
    public LinkedHashMap<Long, Object> getValue() {
        return observedValues;
    }

    public static HistogramMetric.Builder newBuilder() {
        return new HistogramMetric.Builder();
    }

    public static class Builder {
        private int bucketSize;
        private String metricName;
        private Labels labels;
        private long metricUpdateFrequency;
        private SendMetric sendMetric;
        private LinkedHashMap<Long, Object> observedValues;

        public HistogramMetric.Builder setBucketSize(int bucketSize) {
            this.bucketSize = bucketSize;
            return this;
        }

        public HistogramMetric.Builder setMetricName(String metricName) {
            this.metricName = metricName;
            return this;
        }

        public HistogramMetric.Builder setLabels(Labels labels) {
            this.labels = labels;
            return this;
        }

        public HistogramMetric.Builder setFrequency(long metricUpdateFrequency) {
            this.metricUpdateFrequency = metricUpdateFrequency;
            return this;
        }

        public HistogramMetric.Builder setFrequency(SendMetric sendMetric) {
            this.sendMetric = sendMetric;
            return this;
        }

        public HistogramMetric.Builder setvalues(LinkedHashMap<Long, Object> observedValues) {
            this.observedValues = observedValues;
            return this;
        }

        public HistogramMetric create() {
            HistogramMetric histogramMetric;
            if (metricUpdateFrequency == 0 && sendMetric == null) {
                histogramMetric = new HistogramMetric(metricName, labels, observedValues);
            } else {
                histogramMetric =
                        new HistogramMetric(
                                metricName, labels, bucketSize, metricUpdateFrequency, sendMetric);
                histogramMetric.start();
            }
            return histogramMetric;
        }
    }
}
