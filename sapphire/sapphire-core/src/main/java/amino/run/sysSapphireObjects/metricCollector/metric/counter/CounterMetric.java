package amino.run.sysSapphireObjects.metricCollector.metric.counter;

import amino.run.app.labelselector.Labels;
import amino.run.policy.util.ResettableTimer;
import amino.run.sysSapphireObjects.metricCollector.Metric;
import amino.run.sysSapphireObjects.metricCollector.SendMetric;
import java.util.TimerTask;
import java.util.logging.Logger;

/**
 * Counter is a cumulative metric that represents a single monotonically increasing counter whose
 * value can increase or be reset to zero on restart.
 */
public class CounterMetric implements Metric {
    private static Logger logger = Logger.getLogger(CounterMetric.class.getName());

    private String metricName;
    private long count;
    private Labels labels;
    private transient ResettableTimer metricSendTimer;
    private transient long metricUpdateFrequency;
    private transient SendMetric metricAggregator;

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
        return metricName + "<" + labels.toString() + ":" + count + ">";
    }

    private CounterMetric(
            String name, Labels labels, long metricUpdateFrequency, SendMetric sendMetric) {
        this.metricName = name;
        this.labels = labels;
        this.metricUpdateFrequency = metricUpdateFrequency;
        metricAggregator = sendMetric;
    }

    private CounterMetric(String name, Labels labels, long count) {
        this.metricName = name;
        this.labels = labels;
        this.count = count;
    }

    /** resets count to zero */
    public void reset() {
        synchronized (this) {
            count = 0;
        }
    }

    /** @return true if count value is modified */
    public boolean modified() {
        return count != 0;
    }

    /** @return count value */
    public long getCount() {
        return count;
    }

    /** increments count value */
    public void incCount() {
        synchronized (this) {
            count++;
        }
    }

    /** @return labels */
    public Labels getLabels() {
        return labels;
    }

    /** starts a ResettableTimer and resets once metricUpdateFrequency is reached */
    public void start() {
        metricSendTimer =
                new ResettableTimer(
                        new TimerTask() {
                            public void run() {
                                try {
                                    if (modified()) {
                                        metricAggregator.send(CounterMetric.this);
                                    }
                                } catch (Exception e) {
                                    logger.warning(
                                            String.format(
                                                    "%s: Sending metric failed", e.toString()));
                                    return;
                                }
                                // reset the count value and timer after push is done
                                reset();
                                metricSendTimer.reset();
                            }
                        },
                        metricUpdateFrequency);
        metricSendTimer.start();
    }

    /** stops timer */
    public void stop() {
        metricSendTimer.cancel();
    }

    public static CounterMetric.Builder newBuilder() {
        return new CounterMetric.Builder();
    }

    public static class Builder {
        private String metricName;
        private Labels labels;
        private long metricUpdateFrequency;
        private SendMetric sendMetric;
        private long count;

        public CounterMetric.Builder setMetricName(String metricName) {
            this.metricName = metricName;
            return this;
        }

        public CounterMetric.Builder setLabels(Labels labels) {
            this.labels = labels;
            return this;
        }

        public CounterMetric.Builder setFrequency(long metricUpdateFrequency) {
            this.metricUpdateFrequency = metricUpdateFrequency;
            return this;
        }

        public CounterMetric.Builder setSendMetric(SendMetric sendMetric) {
            this.sendMetric = sendMetric;
            return this;
        }

        public CounterMetric.Builder setCount(long count) {
            this.count = count;
            return this;
        }

        public CounterMetric create() {
            CounterMetric counterMetric;
            if (metricUpdateFrequency == 0 && sendMetric == null) {
                counterMetric = new CounterMetric(metricName, labels, count);
            } else {
                counterMetric =
                        new CounterMetric(metricName, labels, metricUpdateFrequency, sendMetric);
                counterMetric.start();
            }
            return counterMetric;
        }
    }
}
