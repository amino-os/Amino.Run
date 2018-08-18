package sapphire.common;

/** Created by Venugopal Reddy K 00900280 on 16/7/18. */

/** Sapphire object name modification exception */
public class SapphireObjectNameModificationException extends Exception {
    String name;
    SapphireObjectID sapphireObjId;

    public SapphireObjectNameModificationException(SapphireObjectID sapphireObjId, String name) {
        this.sapphireObjId = sapphireObjId;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public SapphireObjectID getSapphireObjId() {
        return sapphireObjId;
    }
}
