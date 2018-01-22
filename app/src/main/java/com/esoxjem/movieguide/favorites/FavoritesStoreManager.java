package com.esoxjem.movieguide.favorites;

import sapphire.app.SapphireObject;
import sapphire.policy.ShiftPolicy;

import static sapphire.runtime.Sapphire.new_;

/**
 * Created by f80049853 on 1/18/2018.
 */

public class FavoritesStoreManager implements SapphireObject {
    private FavoritesStore favoritesStore;
    private FavoritesShare favoritesShare;
    public FavoritesStoreManager ()
    {
        //this.fs = (FavoritesStore) new_(FavoritesStore.class);
    }

    public FavoritesStore getFavoritesStore(){
        return favoritesStore = (FavoritesStore) new_(FavoritesStore.class);
    }

    public FavoritesShare getFavoritesShare(){
        return  favoritesShare = (FavoritesShare) new_(FavoritesShare.class);
    }

}
