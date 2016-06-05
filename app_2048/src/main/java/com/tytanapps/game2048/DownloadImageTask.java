package com.tytanapps.game2048;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import com.tytanapps.game2048.activities.MultiplayerActivity;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


// Set the icon of a ImageView to the url in its tag
public class DownloadImageTask extends AsyncTask<ImageView, Void, Bitmap> {
    final static String LOG_TAG = MultiplayerActivity.class.getSimpleName();

    ImageView imageView;

    @Override
    protected Bitmap doInBackground(ImageView... imageViews) {
        this.imageView = imageViews[0];
        return downloadImage((String) imageView.getTag());
    }

    @Override
    protected void onPostExecute(Bitmap result) {
        imageView.setImageBitmap(result);
    }

    private Bitmap downloadImage(String stringUrl) {
        try {
            URL url = new URL(stringUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (IOException e) {
            Log.d(LOG_TAG, e.toString());
            return null;
        }
    }
}