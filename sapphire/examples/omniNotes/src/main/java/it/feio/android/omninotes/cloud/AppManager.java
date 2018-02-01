package it.feio.android.omninotes.cloud;

import it.feio.android.omninotes.db.DbHelper;
import sapphire.app.SapphireObject;

/**
 * Created by howell on 1/16/18.
 */

public class AppManager implements SapphireObject {

    //
    private static AppManager instance_ = null;
    public static void setInstane(AppManager instance) {
        AppManager.instance_ = instance;
    }
    public static AppManager getInstance() {
        return AppManager.instance_;
    }

    // TODO: to make it class level members
    private static DbHelper dbHelper = null;
    public static void setStaticDbHelper(DbHelper dbHelper) {AppManager.dbHelper = dbHelper;}
    public static DbHelper getStaticDbHelper() { return AppManager.dbHelper; }

    private String memo;

    public void setMemo(String content) {
        this.memo = "memo:" + content;
    }

    public String getMemo() {
        return this.memo;
    }

    public DbHelper getDbHelper(){
        return DbHelper.getInstance();
    }
}
