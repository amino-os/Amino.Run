package it.feio.android.omninotes.storage;

import android.content.Context;

// import com.squareup.haha.perflib.Instance;

import it.feio.android.omninotes.utils.StorageHelper;

/**
 * Created by howell on 1/16/18.
 */

public class StorageManager {
    private static StorageManager instance_ = null;

    public static StorageManager Instance() {
        if (StorageManager.instance_ == null) {
            // to change to new_ and AppEntry-creating
            StorageManager.instance_ = new StorageManager();
        }

        return StorageManager.instance_;
    }
    public boolean delete(Context mContext, String name) {
        return StorageHelper.delete(mContext, name);
    }
}
