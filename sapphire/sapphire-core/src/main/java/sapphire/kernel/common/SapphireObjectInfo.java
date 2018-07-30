package sapphire.kernel.common;

/** Created by Venugopal Reddy K on 23/7/18. */
public class SapphireObjectInfo {
    byte[] opaqueObject;
    DMConfigInfo dmConfigInfo;

    public SapphireObjectInfo(DMConfigInfo dmConfig, byte[] opaqueObject) {
        this.dmConfigInfo = dmConfig;
        this.opaqueObject = opaqueObject;
    }

    public DMConfigInfo getDmConfigInfo() {
        return dmConfigInfo;
    }

    public byte[] getOpaqueObject() {
        return opaqueObject;
    }
}
