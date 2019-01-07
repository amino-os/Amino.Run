package sapphire.sysSapphireObjects.metricCollector.metric.wma;

public class Observation {
    private long timeInMillis;
    private float value;

    public Observation(long timeInMillis, float value) {
        this.timeInMillis = timeInMillis;
        this.value = value;
    }

    public long getTimeInMillis() {
        return timeInMillis;
    }

    public void setTimeInMillis(long timeInMillis) {
        this.timeInMillis = timeInMillis;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }
}
