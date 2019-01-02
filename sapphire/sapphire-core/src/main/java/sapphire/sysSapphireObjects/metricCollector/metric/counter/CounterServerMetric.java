package sapphire.sysSapphireObjects.metricCollector.metric.counter;

import java.util.TimerTask;
import java.util.logging.Logger;
import sapphire.app.labelselector.Labels;
import sapphire.policy.metric.MetricAggregator;
import sapphire.policy.util.ResettableTimer;
import sapphire.sysSapphireObjects.metricCollector.Metric;

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
    private long metricUpdateFrequency;
    private MetricAggregator metricAggregator;

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
        return metricName + "<" + labels.toString() + ":" + count + ">";
    }

    public CounterMetric(String name, Labels labels, long metricUpdateFrequency) {
        this.metricName = name;
        this.labels = labels;
        this.metricUpdateFrequency = metricUpdateFrequency;
        metricAggregator = new MetricAggregator();
    }

    public CounterMetric(String name, int count) {
        this.metricName = name;
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
    public void startTimer() {
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
                                resetTimer();
                            }
                        },
                        metricUpdateFrequency);
        metricSendTimer.start();
    }

    /** stops timer */
    public void stopTimer() {
        metricSendTimer.cancel();
    }

    /** resets timer */
    public void resetTimer() {
        metricSendTimer.reset();
    }

    public static CounterMetric.Builder newBuilder() {
        return new CounterMetric.Builder();
    }

    public static class Builder {
        private String metricName;
        private Labels labels;
        private long metricUpdateFrequency;

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

        public CounterMetric create() {
            CounterMetric counterMetric =
                    new CounterMetric(metricName, labels, metricUpdateFrequency);
            counterMetric.startTimer();
            return counterMetric;
        }
    }
}
