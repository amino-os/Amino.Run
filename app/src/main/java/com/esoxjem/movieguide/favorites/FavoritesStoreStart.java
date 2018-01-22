package com.esoxjem.movieguide.favorites;

import sapphire.app.AppEntryPoint;
import sapphire.app.AppObjectNotCreatedException;
import sapphire.common.AppObjectStub;

import static sapphire.runtime.Sapphire.new_;

/**
 * Created by f80049853 on 1/12/2018.
 */


public class FavoritesStoreStart implements AppEntryPoint {

    @Override
    public AppObjectStub start() throws AppObjectNotCreatedException {
        return (AppObjectStub) new_(FavoritesStoreManager.class);
    }
}