package com.esoxjem.movieguide.favorites;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.esoxjem.movieguide.Movie;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import sapphire.app.SapphireObject;
import sapphire.policy.ShiftPolicy;

/**
 * @author arun
 */
@Singleton
public class FavoritesStore implements SapphireObject
{

    private static final String PREF_NAME = "FavoritesStore";
    //private SharedPreferences pref;
    //HashMap<String, String> moviemap = new HashMap<>();
    private Map<String, String> moviemap = new HashMap<>();
    private String username;
    private int cnt=0;
    @Inject
    //public FavoritesStore(Context context)
    public FavoritesStore()
    {
        //pref = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        //this.username=username;
    }

    //public void setFavorite(Movie movie)
    public void setFavorite(String movieid,String moviejson)
    {
        //SharedPreferences.Editor editor = pref.edit();


        //Moshi moshi = new Moshi.Builder().build();
        //JsonAdapter<Movie> jsonAdapter = moshi.adapter(Movie.class);
        //String movieJson = jsonAdapter.toJson(movie);
        //editor.putString(movie.getId(), movieJson);
        //editor.apply();

        moviemap.put(movieid,moviejson);

        /*
        Moshi moshi = new Moshi.Builder().build();
        JsonAdapter<Movie> jsonAdapter = moshi.adapter(Movie.class);
        String movieJson = jsonAdapter.toJson(movie);
        //editor.putString(movie.getId(), movieJson);
        //editor.apply();

        moviemap.put(movie.getId(),movieJson);
        */
    }

    public boolean isFavorite(String id)
    {
        //String movieJson = pref.getString(id, null);
        String movieJson ="";

        movieJson= moviemap.get(id);
        if(movieJson == null  || movieJson.length()==0)
            return false;
        return true;

        /*

        if (!TextUtils.isEmpty(movieJson))
        {
            return true;
        } else
        {
            return false;
        }
        */
    }



/*
    public List<Movie> getFavorites(HashMap movieidmap) throws IOException
    {
        //Map<String, ?> allEntries = pref.getAll();
        ArrayList<Movie> movies = new ArrayList<>(24);
        Moshi moshi = new Moshi.Builder().build();

        for (Map.Entry<String, String> entry : moviemap.entrySet())
        //for (Map.Entry<String, ?> entry : allEntries.entrySet())
        {
            if(movieidmap.containsKey(entry.getKey())) {
                //String movieJson = pref.getString(entry.getKey(), null);
                String movieJson = moviemap.get(entry.getKey());
                if (!TextUtils.isEmpty(movieJson)) {
                    JsonAdapter<Movie> jsonAdapter = moshi.adapter(Movie.class);

                    Movie movie = jsonAdapter.fromJson(movieJson);
                    movies.add(movie);
                } else {
                    // Do nothing;
                }
            }
        }
        */
        //public List<Movie> getFavorites() throws IOException
        public List<String> getFavorites() throws IOException
        {
            //Map<String, ?> allEntries = pref.getAll();

            ArrayList<String> movies = new ArrayList<>(24);
            //Moshi moshi = new Moshi.Builder().build();

            for (Map.Entry<String, String> entry : moviemap.entrySet())
            //for (Map.Entry<String, ?> entry : allEntries.entrySet())
            {
                                 //String movieJson = pref.getString(entry.getKey(), null);
                    String movieJson = moviemap.get(entry.getKey());
                    if (!TextUtils.isEmpty(movieJson)) {
                        //JsonAdapter<Movie> jsonAdapter = moshi.adapter(Movie.class);

                        //Movie movie = jsonAdapter.fromJson(movieJson);
                        movies.add(movieJson);
                    } else {
                        // Do nothing;
                    }
             }
        return movies;

            /*

            ArrayList<Movie> movies = new ArrayList<>(24);
            Moshi moshi = new Moshi.Builder().build();

            for (Map.Entry<String, String> entry : moviemap.entrySet())
            //for (Map.Entry<String, ?> entry : allEntries.entrySet())
            {
                //String movieJson = pref.getString(entry.getKey(), null);
                String movieJson = moviemap.get(entry.getKey());
                if (!TextUtils.isEmpty(movieJson)) {
                    JsonAdapter<Movie> jsonAdapter = moshi.adapter(Movie.class);

                    Movie movie = jsonAdapter.fromJson(movieJson);
                    movies.add(movie);
                } else {
                    // Do nothing;
                }
            }
            return movies;
            */
    }

    public void unfavorite(String id)
    {
        //SharedPreferences.Editor editor = pref.edit();
        //editor.remove(id);
        //editor.apply();
        moviemap.remove(id);
    }
}
