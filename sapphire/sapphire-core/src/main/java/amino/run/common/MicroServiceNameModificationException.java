package amino.run.common;

/** Created by Venugopal Reddy K 00900280 on 16/7/18. */

/** Sapphire object name modification exception */
public class MicroServiceNameModificationException extends Exception {
    String name;
    MicroServiceID MicroServiceId;

    public MicroServiceNameModificationException(MicroServiceID MicroServiceId, String name) {
        this.MicroServiceId = MicroServiceId;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public MicroServiceID getSapphireObjId() {
        return MicroServiceId;
    }
}
