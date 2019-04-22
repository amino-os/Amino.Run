package amino.run.kernel.common.metric.clients;

import amino.run.kernel.common.metric.Metric;
import amino.run.kernel.common.metric.MetricClient;
import amino.run.kernel.common.metric.schema.Schema;
import amino.run.kernel.common.metric.schema.SchemaType;
import amino.run.kernel.common.metric.type.Counter;
import amino.run.kernel.common.metric.type.Gauge;
import amino.run.kernel.common.metric.type.Summary;
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.CounterMetricFamily;
import io.prometheus.client.GaugeMetricFamily;
import io.prometheus.client.SummaryMetricFamily;
import io.prometheus.client.exporter.PushGateway;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PrometheusPushGateWay implements MetricClient {

    private String pushGatewayIpAndPort;

    public PrometheusPushGateWay(String pushGatewayIpAndPort) {
        this.pushGatewayIpAndPort = pushGatewayIpAndPort;
    }

    @Override
    public void send(HashMap<String, String> labels, ArrayList<Metric> metrics) throws Exception {

        CollectorRegistry registry = new CollectorRegistry();
        PushMetricCollector pushMetricCollector =
                new PushMetricCollector(labels, metrics).register(registry);

        PushGateway pushGateway = new PushGateway(pushGatewayIpAndPort);
        pushGateway.push(registry, "amino-job", labels);
    }

    @Override
    public boolean register(Schema schema) throws Exception {
        return false;
    }

    @Override
    public boolean unregister(Schema schema) throws Exception {
        return false;
    }

    public class PushMetricCollector extends Collector {
        ArrayList<String> keys = new ArrayList<String>();
        ArrayList<String> values = new ArrayList<String>();;
        private ArrayList<Metric> metrics;

        @Override
        public List<MetricFamilySamples> collect() {
            List<MetricFamilySamples> mfs = new ArrayList<MetricFamilySamples>();
            for (Metric metric : metrics) {
                Schema schema = metric.getSchema();
                if (schema.getType().equals(SchemaType.Summary.toString())) {
                    SummaryMetricFamily summary =
                            new SummaryMetricFamily(schema.getName(), "", keys);
                    if (metric instanceof Summary) {
                        Summary summaryMetric = (Summary) metric;
                        summary.addMetric(
                                values,
                                summaryMetric.getObservationCount(),
                                summaryMetric.getObservationSum());
                    }
                    mfs.add(summary);
                }

                if (schema.getType().equals(SchemaType.Counter.toString())) {
                    CounterMetricFamily counter =
                            new CounterMetricFamily(schema.getName(), "", keys);
                    if (metric instanceof Counter) {
                        Counter counterMetric = (Counter) metric;
                        counter.addMetric(values, counterMetric.getCount());
                    }
                    mfs.add(counter);
                }

                if (schema.getType().equals(SchemaType.Gauge.toString())) {
                    GaugeMetricFamily gauge = new GaugeMetricFamily(schema.getName(), "", keys);
                    if (metric instanceof Gauge) {
                        Gauge gaugeMetric = (Gauge) metric;
                        gauge.addMetric(values, gaugeMetric.getValue());
                    }
                    mfs.add(gauge);
                }
            }
            return mfs;
        }

        public PushMetricCollector(HashMap<String, String> labels, ArrayList<Metric> metrics) {
            for (Map.Entry<String, String> label : labels.entrySet()) {
                this.keys.add(label.getKey());
                this.values.add(label.getValue());
            }
            this.metrics = metrics;
        }
    }
}
