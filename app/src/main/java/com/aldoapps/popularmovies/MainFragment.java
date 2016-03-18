package com.aldoapps.popularmovies;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.aldoapps.popularmovies.data.FlagPreference;
import com.aldoapps.popularmovies.data.MovieProvider;
import com.aldoapps.popularmovies.model.discover.DiscoverResponse;
import com.aldoapps.popularmovies.model.discover.Movie;
import com.aldoapps.popularmovies.util.MovieConst;
import com.paginate.Paginate;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainFragment extends Fragment {

    @Bind(R.id.grid_view) GridView mGridView;
    @Bind(R.id.toolbar) Toolbar mToolbar;
    @Bind(android.R.id.empty) TextView mEmpty;

    private MoviePosterAdapter mAdapter;
    private List<Movie> mMovieList = new ArrayList<>();

    private int mCurrentPage = 2;
    private boolean mIsFinished = false;
    private Paginate mPaginate;
    private int mTotalPages = 1;

    public MainFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mAdapter = new MoviePosterAdapter(getActivity(), mMovieList);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.menu_main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_settings:
                showSortByDialogue();
                break;
            case R.id.action_favorite:
                showFavoriteList();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showFavoriteList() {
        mToolbar.setTitle(getString(R.string.app_name) + " (Favorite) ");

        if(mPaginate != null){
            mPaginate.unbind();
        }

        FlagPreference.setToFavorite(getContext());
        MovieProvider movieProvider = new MovieProvider(getContext());
        mMovieList.clear();
        mMovieList.addAll(movieProvider.getAllMovie());
        mAdapter.setIsFavorite(true);
        mAdapter.notifyDataSetChanged();
        movieProvider.close();
    }

    private void showSortByDialogue() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.sort_by);
        builder.setItems(R.array.sort_by_array, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mAdapter.setIsFavorite(false);

                switch (which) {
                    case 0:
                        FlagPreference.setToPopularity(getContext());
                        executeMovieTask();
                        break;
                    case 1:
                        FlagPreference.setToHighestRated(getContext());
                        executeMovieTask();
                        break;
                }
            }
        });
        builder.show();
    }

    @Override
    public void onResume() {
        super.onResume();

        loadMovieAccordingToPreference();
    }

    public void loadMovieAccordingToPreference(){
        switch (FlagPreference.getFlag(getContext())){
            case FlagPreference.SORT_BY_FAVORITE:
                mToolbar.setTitle(getString(R.string.app_name) + " (Favorite) ");
                showFavoriteList();
                break;
            case FlagPreference.SORT_BY_HIGHEST_RATED:
                mToolbar.setTitle(getString(R.string.app_name) + " (Hi Rate) ");
                executeMovieTask();
                break;
            case FlagPreference.SORT_BY_POPULARITY:
                mToolbar.setTitle(getString(R.string.app_name) + " (Popular) ");
                executeMovieTask();
                break;
        }

    }

    private void executeMovieTask() {
        if(isNetworkAvailable()){
            mMovieList.clear();
            mAdapter.notifyDataSetChanged();
            if(mPaginate != null){
                mPaginate.unbind();
                mPaginate.setHasMoreDataToLoad(false);
            }

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(MovieConst.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            TmdbApi tmdbApi = retrofit.create(TmdbApi.class);

            Call<DiscoverResponse> call = null;

            switch (FlagPreference.getFlag(getContext())){
                case MovieConst.SORT_BY_HIGHEST_RATED_DESC:
                    mToolbar.setTitle(getString(R.string.app_name) + " (Hi Rate) ");
                    call = tmdbApi.discoverMovies(MovieConst.SORT_BY_HIGHEST_RATED_DESC,
                            getResources().getString(R.string.API_KEY),
                            MovieConst.VOTE_AVERAGE_VALUE,
                            MovieConst.VOTE_COUNT_VALUE);
                    break;
                case MovieConst.SORT_BY_POPULARITY_DESC:
                    mToolbar.setTitle(getString(R.string.app_name) + " (Popular) ");
                    call = tmdbApi.discoverMovies(MovieConst.SORT_BY_POPULARITY_DESC,
                            getResources().getString(R.string.API_KEY)
                            );
                    break;
            }

            if (call != null) {
                call.enqueue(new Callback<DiscoverResponse>() {
                    @Override
                    public void onResponse(Call<DiscoverResponse> call, Response<DiscoverResponse> response) {
                        mMovieList.addAll(response.body().getMovies());
                        mAdapter.notifyDataSetChanged();

                        mIsFinished = true;
                        mCurrentPage = 2;
                        mTotalPages = response.body().getTotalPages();

                        if(mTotalPages > 1){
                            paginateNextPage();
                        }
                    }

                    @Override
                    public void onFailure(Call<DiscoverResponse> call, Throwable t) {
                        mCurrentPage = 2;
                        mIsFinished = true;
                        Toast.makeText(getActivity(),
                                "Failed to fetch data ", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }else{
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.no_internet_message)
                    .setTitle(R.string.no_internet_title)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivityForResult(new Intent(Settings.ACTION_SETTINGS), 0);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                        }
                    });
            builder.show();
        }
    }

    private void paginateNextPage() {
        Paginate.Callbacks paginateCallbacks = new Paginate.Callbacks() {
            @Override
            public void onLoadMore() {
                executeMovieTaskNextPage();
            }

            @Override
            public boolean isLoading() {
                if(mCurrentPage == mTotalPages){
                    mPaginate.setHasMoreDataToLoad(false);
                    return false;
                }

                return !mIsFinished;
            }

            @Override
            public boolean hasLoadedAllItems() {
                if(mCurrentPage == mTotalPages) {
                    mPaginate.setHasMoreDataToLoad(false);
                    return true;
                }

                return false;
            }
        };

        mPaginate = Paginate.with(mGridView, paginateCallbacks)
                .setLoadingTriggerThreshold(2)
                .addLoadingListItem(true)
                .build();
    }

    private void executeMovieTaskNextPage() {
        if(isNetworkAvailable()){
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(MovieConst.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            TmdbApi tmdbApi = retrofit.create(TmdbApi.class);

            Call<DiscoverResponse> call = null;

            switch (FlagPreference.getFlag(getContext())){
                case MovieConst.SORT_BY_HIGHEST_RATED_DESC:
                    mToolbar.setTitle(getString(R.string.app_name) + " (Hi Rate) ");
                    call = tmdbApi.discoverMoviesPage(MovieConst.SORT_BY_FAVORITE_DESC,
                            getResources().getString(R.string.API_KEY),
                            MovieConst.VOTE_AVERAGE_VALUE,
                            MovieConst.VOTE_COUNT_VALUE,
                            mCurrentPage
                            );
                    break;
                case MovieConst.SORT_BY_POPULARITY_DESC:
                    mToolbar.setTitle(getString(R.string.app_name) + " (Popular) ");
                    call = tmdbApi.discoverMoviesPage(MovieConst.SORT_BY_POPULARITY_DESC,
                            getResources().getString(R.string.API_KEY),
                            mCurrentPage
                            );
                    break;
            }

            // set flag is finished
            mIsFinished = false;

            if (call != null) {
                call.enqueue(new Callback<DiscoverResponse>() {
                    @Override
                    public void onResponse(Call<DiscoverResponse> call, Response<DiscoverResponse> response) {
                        mMovieList.addAll(response.body().getMovies());
                        mAdapter.notifyDataSetChanged();
                        mIsFinished = true;
                        ++mCurrentPage;
                    }

                    @Override
                    public void onFailure(Call<DiscoverResponse> call, Throwable t) {
                        mIsFinished = true;
                        ++mCurrentPage;
                        Toast.makeText(getActivity(),
                                "Failed to fetch data ", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }else{
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.no_internet_message)
                    .setTitle(R.string.no_internet_title)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivityForResult(new Intent(Settings.ACTION_SETTINGS), 0);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                        }
                    });
            builder.show();
        }
    }

    private boolean isNetworkAvailable(){
        ConnectivityManager connectivityManager = (ConnectivityManager)
                getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        ButterKnife.bind(this, view);

        mToolbar.setTitle(getString(R.string.app_name));
        ((MainActivity) getActivity()).setSupportActionBar(mToolbar);


        mGridView.setAdapter(mAdapter);
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getActivity(), DetailActivity.class);
                intent.putExtra(MovieConst.KEY, mMovieList.get(position).getId());
                startActivity(intent);
            }
        });
        mGridView.setEmptyView(mEmpty);
        return view;
    }
}
