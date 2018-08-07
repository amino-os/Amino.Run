package sapphire.oms;

import java.io.Serializable;

/** Created by Venugopal Reddy K on 23/7/18. */
public class SapphireClientInfo implements Serializable {
    private byte[] opaqueObject;
    private int clientId;
    private int sapphireId;

    public SapphireClientInfo(int clientId, int sapphireId, byte[] opaqueObject) {
        this.clientId = clientId;
        this.sapphireId = sapphireId;
        this.opaqueObject = opaqueObject;
    }

    public int getClientId() {
        return clientId;
    }

    public int getSapphireId() {
        return sapphireId;
    }

    public byte[] getOpaqueObject() {
        return opaqueObject;
    }
}
