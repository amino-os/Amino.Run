package com.esoxjem.movieguide.favorites;

import java.util.HashMap;
import java.util.Map;

import sapphire.app.SapphireObject;
import sapphire.policy.ShiftPolicy;

/**
 * Created by f80049853 on 1/18/2018.
 */

public class FavoritesShare implements SapphireObject{
    HashMap<String,String> moviemap = new HashMap<>();
    private Map<String, String> moviemapnew = new HashMap<>();
    int cnt=0;
    public FavoritesShare(){}
    public void PrintMsg()
    {
        System.out.println("PrintMsg to test Sapphire. Cnt = " + cnt);
        cnt++;

    }

    public HashMap getMovieMap()
    {
        System.out.println("get Mapt = " + cnt);
        return moviemap;
    }
    public void setMovieMap(String movieid)
    {
        System.out.println("set Mapt = " + cnt);
        moviemap.put(movieid,movieid);
    }

    public boolean isfavorite(String movieid)
    {
        System.out.println("isfavorite Mapt = " + movieid);
        if(moviemap.containsKey(movieid)) return true;
        return false;
    }

    public void unfavorite(String movieid)
    {
        System.out.println("unfavorite Mapt = " + movieid);
        moviemap.remove(movieid);

    }
}