package sapphire.sysSapphireObjects.metricCollector.metric.histogram;

import java.util.LinkedHashMap;
import java.util.TimerTask;
import java.util.logging.Logger;
import sapphire.app.labelselector.Labels;
import sapphire.policy.metric.MetricAggregator;
import sapphire.policy.util.ResettableTimer;
import sapphire.sysSapphireObjects.metricCollector.Metric;

/** Histogram samples observations and maintains them in configurable buckets */
public class HistogramMetric implements Metric {
    private static Logger logger = Logger.getLogger(HistogramMetric.class.getName());

    private String metricName;
    private Labels labels;
    private int bucketSize;
    private LinkedHashMap<Long, Object> observedValues;
    private transient ResettableTimer metricSendTimer;
    private MetricAggregator metricAggregator;
    private long metricUpdateFrequency;

    public HistogramMetric(
            String metricName, Labels labels, int bucketSize, long metricUpdateFrequency) {
        this.metricName = metricName;
        this.labels = labels;
        this.bucketSize = bucketSize;
        this.observedValues = new LinkedHashMap<>();
        this.metricUpdateFrequency = metricUpdateFrequency;
        metricAggregator = new MetricAggregator();
    }

    @Override
    public String getName() {
        return metricName;
    }

    @Override
    public Object getMetric() {
        return this;
    }

    /** @return true if some values are added in observedValues */
    public boolean modified() {
        return observedValues.size() != 0;
    }

    /** starts a ResettableTimer and resets once metricUpdateFrequency is reached */
    public void startTimer() {
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
                                resetTimer();
                            }
                        },
                        metricUpdateFrequency);
        metricSendTimer.start();
    }

    /** stops the timer */
    public void stopTimer() {
        metricSendTimer.cancel();
    }

    /** resets the timer */
    public void resetTimer() {
        metricSendTimer.reset();
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

    public static class Builder {
        private int bucketSize;
        private String metricName;
        private Labels labels;
        private long metricUpdateFrequency;

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

        public HistogramMetric create() {
            HistogramMetric histogramMetric =
                    new HistogramMetric(
                            metricName, labels, bucketSize, metricUpdateFrequency);
            histogramMetric.startTimer();
            return histogramMetric;
        }
    }
}
