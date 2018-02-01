package it.feio.android.omninotes.cloud;

import it.feio.android.omninotes.db.DbHelper;
import sapphire.app.AppEntryPoint;
import sapphire.app.AppObjectNotCreatedException;
import sapphire.common.AppObjectStub;
import sapphire.runtime.Sapphire;

/**
 * Created by howell on 1/15/18.
 */

public class OmniNotesApp implements AppEntryPoint {
    @Override
    public AppObjectStub start() throws AppObjectNotCreatedException {
        return (AppObjectStub) Sapphire.new_(AppManager.class);
    }
}
