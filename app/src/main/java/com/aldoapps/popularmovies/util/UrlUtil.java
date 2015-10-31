package com.aldoapps.popularmovies.util;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;

/**
 * Created by user on 11/10/2015.
 */
public class UrlUtil {
    /**
     * Generate image url by combining various url
     * @param posterPath
     * @return String image url
     */
    public static String generatePosterUrl(String posterPath){
        // example final image URL
        // http://image.tmdb.org/t/p/w185/tbhdm8UJAb4ViCTsulYFL3lxMCd.jpg
        return MovieConst.IMAGE_BASE_URL + MovieConst.POSTER_SIZE_PARAM + posterPath;
    }

    /**
     * Creating intent for opening video link from tmdbapi
     * @param id
     * @return Intent. If user installed youtube it will open youtube app
     * if not it will open browser
     */
    public static Intent watchYoutubeVideo(String id){
        Intent intent = null;
        try{
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + id));
        }catch (ActivityNotFoundException ex){
            intent=new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://www.youtube.com/watch?v="+id));
        }finally {
            return intent;
        }
    }
}
