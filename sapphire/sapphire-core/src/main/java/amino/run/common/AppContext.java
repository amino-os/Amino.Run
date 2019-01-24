package amino.run.common;

import java.util.ArrayList;

/**
 * App stub context preserved across the DM client policies of chain till the RPC is actually sent
 * out to remote server
 */
public class AppContext {
    /* Previous DM's method name. Current DM's method name is stored before calling the next DM's client onRPC() */
    private String prevMtdName;

    private String appMtdName; // App method name
    private ArrayList<Object> appParams; // App parameters

    // Stack of parameters pushed from Appstub till it reach current DM server stub
    private ArrayList<Object> params;

    public String getPrevMtdName() {
        return prevMtdName;
    }

    public void setPrevMtdName(String mtdName) {
        this.prevMtdName = mtdName;
    }

    public ArrayList<Object> getParams() {
        return params;
    }

    public void setParams(ArrayList<Object> params) {
        this.params = params;
    }

    public String getAppMtdName() {
        return appMtdName;
    }

    public void setAppMtdName(String appMtdName) {
        this.appMtdName = appMtdName;
    }

    public ArrayList<Object> getAppParams() {
        return appParams;
    }

    public void setAppParams(ArrayList<Object> appParams) {
        this.appParams = appParams;
    }
}
