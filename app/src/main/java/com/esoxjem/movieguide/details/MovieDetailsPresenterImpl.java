package com.esoxjem.movieguide.details;

import android.os.AsyncTask;

import com.esoxjem.movieguide.Movie;
import com.esoxjem.movieguide.Review;
import com.esoxjem.movieguide.Video;
import com.esoxjem.movieguide.favorites.FavoritesInteractor;

import com.esoxjem.movieguide.favorites.FavoritesShare;
import com.esoxjem.movieguide.util.RxUtils;

import java.util.HashMap;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * @author arun
 */
class MovieDetailsPresenterImpl implements MovieDetailsPresenter {
    private MovieDetailsView view;
    private MovieDetailsInteractor movieDetailsInteractor;
    private FavoritesInteractor favoritesInteractor;
    private Disposable trailersSubscription;
    private Disposable reviewSubscription;
    private static FavoritesShare favoritesShare;
    private HashMap<String,String> movieid=null;

    MovieDetailsPresenterImpl(MovieDetailsInteractor movieDetailsInteractor, FavoritesInteractor favoritesInteractor) {
        this.movieDetailsInteractor = movieDetailsInteractor;
        this.favoritesInteractor = favoritesInteractor;
        //new getfstore().execute("192.168.10.34", "22346", "10.0.2.15", "22345");
    }

    @Override
    public void setView(MovieDetailsView view) {
        this.view = view;
    }

    @Override
    public void destroy() {
        view = null;
        RxUtils.unsubscribe(trailersSubscription, reviewSubscription);
    }

    @Override
    public void showDetails(Movie movie) {
        if (isViewAttached()) {
            view.showDetails(movie);
        }
    }

    private boolean isViewAttached() {
        return view != null;
    }

    @Override
    public void showTrailers(Movie movie) {
        trailersSubscription = movieDetailsInteractor.getTrailers(movie.getId())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onGetTrailersSuccess, t -> onGetTrailersFailure());
    }

    private void onGetTrailersSuccess(List<Video> videos) {
        if (isViewAttached()) {
            view.showTrailers(videos);
        }
    }

    private void onGetTrailersFailure() {
        // Do nothing
    }

    @Override
    public void showReviews(Movie movie) {
        reviewSubscription = movieDetailsInteractor.getReviews(movie.getId()).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onGetReviewsSuccess, t -> onGetReviewsFailure());
    }

    private void onGetReviewsSuccess(List<Review> reviews) {
        if (isViewAttached()) {
            view.showReviews(reviews);
        }
    }

    private void onGetReviewsFailure() {
        // Do nothing
    }

    @Override
    public void showFavoriteButton(Movie movie) {
        //int cnt=so.isfavorite(movie.getId());
        //System.out.println("PrintMsg to test Sapphire. Cnt = " + cnt);
        new getisfavorites().execute(movie.getId());

        /*
        boolean isFavorite = favoritesInteractor.isFavorite(movie.getId());

        if (isViewAttached()) {
            if (isFavorite) {
                view.showFavorited();
            } else {
                view.showUnFavorited();
            }
        }
        */

    }

    @Override
    public void onFavoriteClick(Movie movie) {
        if (isViewAttached()) {

            new getonFavoriteClick().execute(movie);

/*
            boolean isFavorite = favoritesInteractor.isFavorite(movie.getId());
            if (isFavorite) {
                favoritesInteractor.unFavorite(movie.getId());
                view.showUnFavorited();
            } else {
                favoritesInteractor.setFavorite(movie);
                view.showFavorited();
            }
*/
        }

    }

    private class getisfavorites extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... params) {
            String response = null;
            try {
                boolean isFavorite = favoritesInteractor.isFavorite(params[0]);
                if (isViewAttached()) {
                    if (isFavorite) {
                        view.showFavorited();
                    } else {
                        view.showUnFavorited();
                    }
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return response;
        }
    }
    private class getonFavoriteClick extends AsyncTask<Movie, Void, String> {
        protected String doInBackground(Movie... params) {
            String response = null;
            try {

                boolean isFavorite = favoritesInteractor.isFavorite(params[0].getId());
                if (isFavorite) {
                    favoritesInteractor.unFavorite(params[0].getId());
                    view.showUnFavorited();
                } else {
                    favoritesInteractor.setFavorite(params[0]);
                    view.showFavorited();
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return response;
        }
    }
    private class getunfavorites extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... params) {
            String response = null;
            try {
                boolean isFavorite = favoritesInteractor.isFavorite(params[0]);
                if (isViewAttached()) {
                    if (isFavorite) {
                        view.showFavorited();
                    } else {
                        view.showUnFavorited();
                    }
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return response;
        }
    }
}
