package sapphire.common;

import sapphire.app.SapphireObjectSpec;

public class SapphireObjectIdentifier {
    private final String DefaultSapphireName = "";

    private SapphireObjectID sapphireObjectID;
    // TODO: Sapphire Object name can be removed as it part of SapphireObjectSpec
    private String sapphireObjectName;
    private SapphireObjectSpec sapphireObjectSpec;

    public SapphireObjectIdentifier(SapphireObjectID soID) {
        sapphireObjectID = soID;
        sapphireObjectName = DefaultSapphireName;
    }

    public void setSapphireObjectName(String sapphireObjectName) {
        this.sapphireObjectName = sapphireObjectName;
    }

    public void setSapphireObjectSpec(SapphireObjectSpec soSpec) {
        this.sapphireObjectSpec = soSpec;
    }

    public SapphireObjectID getSapphireObjectID() {
        return sapphireObjectID;
    }

    public String getSapphireObjectName() {
        return sapphireObjectName;
    }

    public SapphireObjectSpec getSapphireObjectSpec() {
        return sapphireObjectSpec;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SapphireObjectIdentifier that = (SapphireObjectIdentifier) o;

        return that.getSapphireObjectID().equals(sapphireObjectID);
    }

    @Override
    public int hashCode() {
        return sapphireObjectID.hashCode();
    }

    @Override
    public String toString() {
        String ret =
                "SapphireObjectIdentifier("
                        + sapphireObjectID.getID()
                        + ","
                        + sapphireObjectName
                        + ","
                        + sapphireObjectSpec.toString()
                        + ")";
        return ret;
    }
}
