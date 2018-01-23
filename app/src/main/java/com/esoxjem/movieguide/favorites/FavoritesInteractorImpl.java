package com.esoxjem.movieguide.favorites;

import android.os.AsyncTask;
import android.text.TextUtils;

import com.esoxjem.movieguide.Movie;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
//import com.esoxjem.movieguide.details.MovieDetailsPresenterImpl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sapphire.kernel.server.KernelServer;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServer;

import static com.esoxjem.movieguide.listing.MoviesListingActivity.favoritesStoreManager;

/**
 * @author arun
 */
class FavoritesInteractorImpl implements FavoritesInteractor
{
    private static FavoritesStore favoritesStore;
    private static FavoritesShare favoritesShare;
    private HashMap<String,String> movieidmap=null;
    //private Map<String, String> moviemap

    //FavoritesInteractorImpl(FavoritesStore store)
    FavoritesInteractorImpl()
    {
        //favoritesStore = store;
        new getfstore().execute("192.168.10.34", "22346", "10.0.2.15", "22345");
    }

    @Override
    public void setFavorite(Movie movie)
    {
        //new setFavorites().execute(movie);
        //favoritesShare.setMovieMap(movie.getId());
        //favoritesStore.setFavorite(movie);

        Moshi moshi = new Moshi.Builder().build();
        JsonAdapter<Movie> jsonAdapter = moshi.adapter(Movie.class);
        String movieJson = jsonAdapter.toJson(movie);
        favoritesStore.setFavorite(movie.getId(),movieJson);

    }

    @Override
    public boolean isFavorite(String id)
    {
        //get other favorites from the remote

        //String result =  new getisFavorites().execute(id).get();

        //return result.booleanValue();
        //return favoritesShare.isfavorite(id);
        return favoritesStore.isFavorite(id);
    }

    @Override
    public List<Movie> getFavorites()
    {
        //get other favorites from the remote
         //return (List<Movie>) new getFavorites().execute();
        /*
        try
        {
            movieidmap=favoritesShare.getMovieMap();
            return favoritesStore.getFavorites(movieidmap);
        } catch (IOException ignored)
        {
            return new ArrayList<>(0);
        }
        */


        /*
        try
        {
            //movieidmap=favoritesShare.getMovieMap();
            return favoritesStore.getFavorites();
        } catch (IOException ignored)
        {
            return new ArrayList<>(0);
        }
        */

        try
        {
            ArrayList<Movie> movies = new ArrayList<>(24);
            Moshi moshi = new Moshi.Builder().build();
            ArrayList<String> moviesjsons = (ArrayList<String>) favoritesStore.getFavorites();
            for (String moviejson: moviesjsons)
            //for (Map.Entry<String, ?> entry : allEntries.entrySet())
            {
                //String movieJson = pref.getString(entry.getKey(), null);
                //String movieJson = moviemap.get(entry.getKey());
                if (!TextUtils.isEmpty(moviejson)) {
                    JsonAdapter<Movie> jsonAdapter = moshi.adapter(Movie.class);

                    Movie movie = jsonAdapter.fromJson(moviejson);
                    movies.add(movie);
                } else {
                    // Do nothing;
                }
            }
        } catch (IOException ignored)
        {
            return new ArrayList<>(0);
        }
        return new ArrayList<>(0);

    }

    @Override
    public void unFavorite(String id)
    {
        //Set myselft favorites and shared with others
        //new getUnFavorites().execute(id);
        favoritesShare.unfavorite(id);

    }

    private class getfstore extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... params) {
            String response = null;
            Registry registry;
            try {

                String[] args = new String[]{ "192.168.10.34", "22346", "10.0.2.15", "22345" };
                registry = LocateRegistry.getRegistry(args[0], Integer.parseInt(args[1]));
                OMSServer server = (OMSServer) registry.lookup("SapphireOMS");
                System.out.println(server);
                KernelServer nodeServer = new KernelServerImpl(new InetSocketAddress(args[2], Integer.parseInt(args[3])), new InetSocketAddress(args[0], Integer.parseInt(args[1])));
                favoritesStoreManager = (FavoritesStoreManager) server.getAppEntryPoint();
                favoritesShare=favoritesStoreManager.getFavoritesShare();
                favoritesStore=favoritesStoreManager.getFavoritesStore("movieguide");
                //favoritesStore.getFavorites();

                List<String> movieidmaplist = favoritesStore.getFavorites();
                //List<Movie> movieidmaplist = favoritesStore.getFavorites();
                boolean isteststore = favoritesStore.isFavorite("");

                movieidmap=favoritesShare.getMovieMap();
                boolean istest =favoritesShare.isfavorite("");
                favoritesShare.PrintMsg();
                //favoritesStore =favoritesStoreManager.getFavoritesStore();

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return response;
        }
    }
    /*
    private class getUnFavorites extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... params) {
            String response = null;
            try {
                favoritesShare.unfavorite(params[0]);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return response;
        }
    }
    private class getFavorites extends AsyncTask<String, Void, List<Movie>> {
        protected List<Movie> doInBackground(String... params) {
            String response = null;
            try
            {
                //movieidmap=favoritesShare.getMovieMap();
                return favoritesStore.getFavorites();
            } catch (IOException ignored)
            {
                return new ArrayList<>(0);
            }
            //return new ArrayList<>(0);
        }
    }
    private class setFavorites extends AsyncTask<Movie, Void, String> {
        protected String doInBackground(Movie... params) {
            String response = null;
            try {
                favoritesStore.setFavorite(params[0]);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return response;
        }
    }
    private class getisFavorites extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... params) {
            //boolean response = null;
            try {
                    return favoritesShare.isfavorite(params[0])?"1":"0";
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                 return "0";
            }
        }
    }
    */
}
