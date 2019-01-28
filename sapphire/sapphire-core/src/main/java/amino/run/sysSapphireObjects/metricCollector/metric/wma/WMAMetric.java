package amino.run.sysSapphireObjects.metricCollector.metric.wma;

import amino.run.app.labelselector.Labels;
import amino.run.policy.util.ResettableTimer;
import amino.run.sysSapphireObjects.metricCollector.Metric;
import amino.run.sysSapphireObjects.metricCollector.SendMetric;
import java.util.ArrayList;
import java.util.TimerTask;
import java.util.logging.Logger;

/**
 * Weighted moving average (wma) gives weighted average of the last n values, where the weighting
 * decreases with each previous value.
 */
public class WMAMetric implements Metric {
    private static Logger logger = Logger.getLogger(WMAMetric.class.getName());
    private String metricName;
    private Labels labels;
    private transient int bucketSize;
    private ArrayList<Observation> observations;
    private float wmaValue;
    private transient long metricUpdateFrequency;
    private transient ResettableTimer metricSendTimer;
    private transient SendMetric metricAggregator;

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
        this.observations = new ArrayList<>();
        this.wmaValue = 0;
    }

    private WMAMetric(String metricName, Labels labels, float wmaValues) {
        this.metricName = metricName;
        this.labels = labels;
        this.wmaValue = wmaValues;
    }

    @Override
    public String getName() {
        return metricName;
    }

    @Override
    public Metric getMetric() {
        return this;
    }

    @Override
    public String toString() {
        return metricName + "<" + labels.toString() + ":" + wmaValue + ">";
    }

    /** @return true if observations is modified */
    public boolean modified() {
        return observations.size() != 0;
    }

    /** resets observations */
    public void reset() {
        synchronized (this) {
            observations.clear();
            wmaValue = 0f;
        }
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
                                // reset the observations and timer after push is done
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
            Observation observation = new Observation(time, value);
            observations.add(observation);
            if (observations.size() == bucketSize) {
                try {
                    metricAggregator.send(this);
                } catch (Exception e) {
                    logger.warning(String.format("%s: Sending metric failed", e.toString()));
                    return;
                }
                reset();
                metricSendTimer.reset();
            }
        }
    }

    /** @return labels */
    public Labels getLabels() {
        return labels;
    }

    /** @return observations */
    public ArrayList<Observation> getObservations() {
        return observations;
    }

    public float getValue() {
        return wmaValue;
    }

    public static WMAMetric.Builder newBuilder() {
        return new WMAMetric.Builder();
    }

    public static class Builder {
        private int bucketSize;
        private String metricName;
        private Labels labels;
        private long metricUpdateFrequency;
        private SendMetric sendMetric;
        private float wmaValues;

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

        public WMAMetric.Builder setValue(float wmaValues) {
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
