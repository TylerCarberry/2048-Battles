package com.tytanapps.game2048;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class CustomIconActivity extends Activity {

    private final static String LOG_TAG = CustomIconActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_icon);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.custom_icon, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        createLinearLayout();
        super.onResume();
    }

    private void createLinearLayout() {
        LinearLayout listOfTiles = (LinearLayout) findViewById(R.id.tile_icon_linear_layout);


        for(int i=2; i <= 2048; i *= 2) {
            LinearLayout tileIconLayout = new LinearLayout(this);
            tileIconLayout.setOrientation(LinearLayout.HORIZONTAL);

            TextView tileNumberTextView = new TextView(this);
            tileNumberTextView.setText("" + i);

            final int currentTile = i;
            Button changeIconButton = new Button(this);
            changeIconButton.setText("Change");
            changeIconButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showPictureDialog(currentTile);
                }
            });

            tileIconLayout.addView(tileNumberTextView);
            tileIconLayout.addView(changeIconButton);
            listOfTiles.addView(tileIconLayout);

        }
    }

    private void showPictureDialog(int tile) {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, tile);
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        Log.d("A", "ON ACTIVITY RESULT");

        try {
            Uri selectedImage = imageReturnedIntent.getData();
            InputStream imageStream = getContentResolver().openInputStream(selectedImage);
            Bitmap yourSelectedImage = BitmapFactory.decodeStream(imageStream);

            Log.d(LOG_TAG, "Is yourSelected image null? " + (yourSelectedImage == null));

            /*
            Drawable imageDrawable = new BitmapDrawable(yourSelectedImage);


            int width = getResources().getDrawable(R.drawable.tile_2).getBounds().width();
            int height = getResources().getDrawable(R.drawable.tile_2).getBounds().height();

*/

//            Drawable imageDrawable = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(yourSelectedImage, 128, 128, true));


            File fileCustomTiles = new File(getFilesDir(), getString(R.string.file_custom_tile_icons) + requestCode);

            FileOutputStream out = null;
            try {
                out = new FileOutputStream(fileCustomTiles);
                yourSelectedImage.compress(Bitmap.CompressFormat.PNG, 90, out);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = BitmapFactory.decodeFile(fileCustomTiles.getAbsolutePath(), options);

            Log.d("A", "result null? " + (bitmap == null));

            Log.d("A", "DONE ACTIVITY RESULT");
        }

        catch(IOException e) {
            Log.e(LOG_TAG, "ERROR");
            Log.e(LOG_TAG, e.toString());
        }
    }

    /*

    private void saveChosenIcon(Bitmap icon, int tile) {

        Log.d(LOG_TAG, "Enter save chosen icon");

        FileOutputStream out = null;
        File saveFile = new File(getFilesDir(), "savedTileIcon"+tile);
        try {
            out = new FileOutputStream(saveFile);
            icon.compress(Bitmap.CompressFormat.PNG, 90, out);
            Log.d(LOG_TAG, "saved tile");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Log.d(LOG_TAG, "trying to read back icon");

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeFile(saveFile.getAbsolutePath(), options);

        Log.d(LOG_TAG, "Is bitmap null 1:" + (bitmap == null));

        //selected_photo.setImageBitmap(bitmap);
    }

*/


    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_custom_icon, container, false);
            return rootView;
        }
    }
}
