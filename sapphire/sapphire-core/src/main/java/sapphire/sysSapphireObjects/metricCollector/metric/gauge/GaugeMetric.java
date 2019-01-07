package sapphire.sysSapphireObjects.metricCollector.metric.gauge;

import java.util.TimerTask;
import java.util.logging.Logger;
import sapphire.app.labelselector.Labels;
import sapphire.policy.util.ResettableTimer;
import sapphire.sysSapphireObjects.metricCollector.Metric;
import sapphire.sysSapphireObjects.metricCollector.SendMetric;

/**
 * Gauge is a metric that represents a single numerical value that can arbitrarily go up and down.
 */
public class GaugeMetric implements Metric {
    private static Logger logger = Logger.getLogger(GaugeMetric.class.getName());

    private String metricName;
    private float value;
    private Labels labels;
    private transient ResettableTimer metricSendTimer;
    private SendMetric metricAggregator;
    private long metricUpdateFrequency;

    private GaugeMetric(
            String metricName, Labels labels, long metricUpdateFrequency, SendMetric sendMetric) {
        this.metricName = metricName;
        this.labels = labels;
        this.metricUpdateFrequency = metricUpdateFrequency;
        metricAggregator = sendMetric;
    }

    @Override
    public String getName() {
        return metricName;
    }

    @Override
    public Object getMetric() {
        return this;
    }

    @Override
    public String toString() {
        return metricName + "<" + labels.toString() + ":" + value + ">";
    }

    /** @return metric value */
    public float getValue() {
        return value;
    }

    /**
     * Sets the value which will be the average of the previous value
     *
     * @param setVal Value that is to be set and which goes up and down
     */
    public void setValue(float setVal) {
        synchronized (this) {
            value = (value + setVal) / 2;
        }
    }

    /** resets the value */
    public void reset() {
        synchronized (this) {
            value = 0f;
        }
    }

    /** @return labels */
    public Labels getLabels() {
        return labels;
    }

    /** @return true if the value is modified */
    public boolean modified() {
        return value != 0;
    }

    /** starts a ResettableTimer and resets once metricUpdateFrequency is reached */
    public void startTimer() {
        metricSendTimer =
                new ResettableTimer(
                        new TimerTask() {
                            public void run() {
                                try {
                                    if (modified()) {
                                        metricAggregator.send(GaugeMetric.this);
                                    }
                                } catch (Exception e) {
                                    logger.warning(
                                            String.format(
                                                    "%s: Sending metric failed", e.toString()));
                                    return;
                                }
                                // reset the value and timer after push is done
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

    public static GaugeMetric.Builder newBuilder() {
        return new GaugeMetric.Builder();
    }

    public static class Builder {
        private String metricName;
        private Labels labels;
        private long metricUpdateFrequency;
        private SendMetric sendMetric;

        public GaugeMetric.Builder setMetricName(String metricName) {
            this.metricName = metricName;
            return this;
        }

        public GaugeMetric.Builder setLabels(Labels labels) {
            this.labels = labels;
            return this;
        }

        public GaugeMetric.Builder setFrequency(long metricUpdateFrequency) {
            this.metricUpdateFrequency = metricUpdateFrequency;
            return this;
        }

        public GaugeMetric.Builder setSendMetric(SendMetric sendMetric) {
            this.sendMetric = sendMetric;
            return this;
        }

        public GaugeMetric create() {
            GaugeMetric gaugeMetric =
                    new GaugeMetric(metricName, labels, metricUpdateFrequency, sendMetric);
            gaugeMetric.startTimer();
            return gaugeMetric;
        }
    }
}
