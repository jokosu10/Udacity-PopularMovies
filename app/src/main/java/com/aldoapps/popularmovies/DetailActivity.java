package com.aldoapps.popularmovies;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by user on 03/09/2015.
 */
public class DetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_detail);

        if(getIntent() != null){
            Movie movie = getIntent().getParcelableExtra(Movie.KEY);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, DetailFragment.newInstance(movie), DetailFragment.TAG)
                    .commit();
        }
    }
}
