package amino.run.common;

/** Created by Venugopal Reddy K 00900280 on 16/7/18. */

/** MicroService object name modification exception */
public class MicroServiceNameModificationException extends Exception {
    String name;
    MicroServiceID microServiceId;

    public MicroServiceNameModificationException(MicroServiceID microServiceId, String name) {
        this.microServiceId = microServiceId;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public MicroServiceID getMicroServiceId() {
        return microServiceId;
    }
}
