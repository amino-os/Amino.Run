package com.esoxjem.movieguide.listing;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;

import com.esoxjem.movieguide.Constants;
import com.esoxjem.movieguide.Movie;
import com.esoxjem.movieguide.R;
import com.esoxjem.movieguide.details.MovieDetailsActivity;
import com.esoxjem.movieguide.details.MovieDetailsFragment;
import com.esoxjem.movieguide.favorites.FavoritesStore;
import com.esoxjem.movieguide.favorites.FavoritesStoreManager;
import com.esoxjem.movieguide.favorites.FavoritesShare;

import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import sapphire.kernel.server.KernelServer;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServer;

public class MoviesListingActivity extends AppCompatActivity implements MoviesListingFragment.Callback
{
    public static final String DETAILS_FRAGMENT = "DetailsFragment";
    private boolean twoPaneMode;
    public static FavoritesStoreManager favoritesStoreManager;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        //new GenerateWorld().execute("192.168.10.34", "22346", "10.0.2.15", "22345");
        //new getfstore().execute("192.168.10.34", "22346", "10.0.2.15", "22345");
        setContentView(R.layout.activity_main);
        setToolbar();

        if (findViewById(R.id.movie_details_container) != null)
        {
            twoPaneMode = true;

            if (savedInstanceState == null)
            {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.movie_details_container, new MovieDetailsFragment())
                        .commit();
            }
        } else
        {
            twoPaneMode = false;
        }
    }

    private void setToolbar()
    {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null)
        {
            getSupportActionBar().setTitle(R.string.movie_guide);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public void onMoviesLoaded(Movie movie)
    {
        if(twoPaneMode)
        {
            loadMovieFragment(movie);
        } else
        {
            // Do not load in single pane view
        }
    }

    @Override
    public void onMovieClicked(Movie movie)
    {
        if (twoPaneMode)
        {
            loadMovieFragment(movie);
        } else
        {
            startMovieActivity(movie);
        }
    }

    private void startMovieActivity(Movie movie)
    {
        Intent intent = new Intent(this, MovieDetailsActivity.class);
        Bundle extras = new Bundle();
        extras.putParcelable(Constants.MOVIE, movie);
        intent.putExtras(extras);
        startActivity(intent);
    }

    private void loadMovieFragment(Movie movie)
    {
        MovieDetailsFragment movieDetailsFragment = MovieDetailsFragment.getInstance(movie);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.movie_details_container, movieDetailsFragment, DETAILS_FRAGMENT)
                .commit();
    }

    /*
    private class getfstore extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... params) {
            String response = null;
            Registry registry;
            try {

                String[] args = new String[]{ "192.168.10.34", "22346", "10.0.2.15", "22345" };
                //String[] args = new String[]{ "127.0.0.1", "58230", "10.0.2.15", "22345" };
                registry = LocateRegistry.getRegistry(args[0], Integer.parseInt(args[1]));
                OMSServer server = (OMSServer) registry.lookup("SapphireOMS");
                System.out.println(server);
                KernelServer nodeServer = new KernelServerImpl(new InetSocketAddress(args[2], Integer.parseInt(args[3])), new InetSocketAddress(args[0], Integer.parseInt(args[1])));
                //KernelServer nodeServer = new KernelServerImpl(new InetSocketAddress(args[2], Integer.parseInt(args[3])), server);

                favoritesStoreManager = (FavoritesStoreManager) server.getAppEntryPoint();
                //favoritesStore=favoritesStoreManager.getFavoritesStore();
                Simple so=favoritesStoreManager.getsimple();
                int cnt=so.getMap();
                //favoritesStore = (FavoritesStore) server.start();


            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return response;
        }

    }
    */

}
