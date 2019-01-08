package amino.run.common;

/** Class used for reporting system sapphire object status information. */
public class SystemSapphireObjectStatus implements java.io.Serializable {
    private String sapphireObjectName;
    private boolean deployed;

    /**
     * Construct system sapphire object status instance
     *
     * @param sapphireObjectName sapphire object name
     * @param deployed status
     */
    public SystemSapphireObjectStatus(String sapphireObjectName, boolean deployed) {
        this.deployed = deployed;
        this.sapphireObjectName = sapphireObjectName;
    }

    /**
     * Getter for sapphire object name
     *
     * @return sapphire object name
     */
    public String getSapphireObjectName() {
        return sapphireObjectName;
    }

    /**
     * Getter for sapphire object deployment status
     *
     * @return deployment status
     */
    public boolean isDeployed() {
        return deployed;
    }
}
