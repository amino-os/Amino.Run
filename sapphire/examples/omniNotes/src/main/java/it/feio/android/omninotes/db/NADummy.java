package it.feio.android.omninotes.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by howell on 1/18/18.
 */

public abstract class NADummy extends SQLiteOpenHelper {
    public NADummy() {
        super(null, "", null, 1);
    }

    public NADummy(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }
}