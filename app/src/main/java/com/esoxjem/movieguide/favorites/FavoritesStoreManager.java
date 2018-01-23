package com.esoxjem.movieguide.favorites;

import java.util.Hashtable;
import java.util.Map;

import sapphire.app.SapphireObject;
import sapphire.policy.ShiftPolicy;
import sapphire.policy.interfaces.dht.DHTKey;

import static sapphire.runtime.Sapphire.new_;

/**
 * Created by f80049853 on 1/18/2018.
 */

public class FavoritesStoreManager implements SapphireObject {
    //private FavoritesStore favoritesStore;
    private FavoritesShare favoritesShare;
    Map<String, FavoritesStore> favoritesStores = new Hashtable<String, FavoritesStore>();

    public FavoritesStoreManager ()
    {
        //this.fs = (FavoritesStore) new_(FavoritesStore.class);
    }

    public FavoritesStore getFavoritesStore(String favoritesStoreName){
        FavoritesStore favoritesStore = favoritesStores.get(favoritesStoreName);
        if(favoritesStore == null)
        {
            favoritesStore = (FavoritesStore) new_(FavoritesStore.class);
            favoritesStores.put(favoritesStoreName,favoritesStore);
        }
        return favoritesStore;
    }

    public FavoritesShare getFavoritesShare(){
        return  favoritesShare = (FavoritesShare) new_(FavoritesShare.class);
    }

}
