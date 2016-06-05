package com.tytanapps.game2048.activities;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.tytanapps.game2048.Game;
import com.tytanapps.game2048.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

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
        listOfTiles.removeAllViewsInLayout();

        List<Integer> listOfTileValues = Game.getListOfAllTileValues();

        for(int tile : listOfTileValues) {
            LinearLayout tileIconLayout = new LinearLayout(this);
            tileIconLayout.setOrientation(LinearLayout.HORIZONTAL);

            TextView tileNumberTextView = new TextView(this);

            switch (tile) {
                case Game.X_TILE_VALUE:
                    tileNumberTextView.setText("X Tile");
                    break;
                case Game.CORNER_TILE_VALUE:
                    tileNumberTextView.setText("Corner Tile");
                    break;
                case Game.GHOST_TILE_VALUE:
                    tileNumberTextView.setText("Ghost Tile");
                    break;
                default:
                    tileNumberTextView.setText(""+tile);
                    break;
            }

            final int currentTile = tile;
            Button changeIconButton = new Button(this);
            changeIconButton.setText("Change");
            changeIconButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showPictureDialog(currentTile);
                }
            });

            Button resetImageButton = new Button(this);
            resetImageButton.setText("Reset");
            resetImageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    clearSaveFile(currentTile);
                    Toast.makeText(getApplicationContext(), "Deleted " + currentTile, Toast.LENGTH_SHORT).show();
                    createLinearLayout();
                }
            });

            ImageView tileIcon = new ImageView(this);
            tileIcon.setImageDrawable(getTileIcon(tile));

            tileIconLayout.addView(tileNumberTextView);
            tileIconLayout.addView(tileIcon);
            tileIconLayout.addView(changeIconButton);
            tileIconLayout.addView(resetImageButton);
            listOfTiles.addView(tileIconLayout);
        }
    }

    private void showPictureDialog(int tile) {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        Log. d("a", "Tile"+tile);

        if(tile < 0)
            tile = tile * -1 + 100;
        startActivityForResult(photoPickerIntent, tile);
    }

    private Drawable getTileIcon(int tile) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        File fileCustomTiles = getIconFile(tile);
        Bitmap bitmap = BitmapFactory.decodeFile(fileCustomTiles.getAbsolutePath(), options);

        if (bitmap != null)
            return new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(bitmap, 128, 128, true));
        return getResources().getDrawable(getIconResourceId(tile));
    }

    /**
     * Update the tile's icon to match its value
     * @param tileValue The numerical value of the tile
     */
    private int getIconResourceId(int tileValue) {
        switch(tileValue) {
            case -2:
                return R.drawable.tile_x;
            case -1:
                return R.drawable.tile_corner;
            case 0:
                return R.drawable.tile_blank;
            case 2:
                return R.drawable.tile_2;
            case 4:
                return R.drawable.tile_4;
            case 8:
                return R.drawable.tile_8;
            case 16:
                return R.drawable.tile_16;
            case 32:
                return R.drawable.tile_32;
            case 64:
                return R.drawable.tile_64;
            case 128:
                return R.drawable.tile_128;
            case 256:
                return R.drawable.tile_256;
            case 512:
                return R.drawable.tile_512;
            case 1024:
                return R.drawable.tile_1024;
            case 2048:
                return R.drawable.tile_2048;
            // If I did not create an image for the tile,
            // default to a question mark
            default:
                return R.drawable.tile_question;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        Log.d("a", "ON RESULT");

        try {

            if(requestCode == -1*(Game.X_TILE_VALUE - 100) ||
               requestCode == -1*(Game.CORNER_TILE_VALUE - 100) ||
               requestCode == -1*(Game.GHOST_TILE_VALUE - 100))
                    requestCode = -1*(requestCode - 100);

            Uri selectedImage = imageReturnedIntent.getData();
            InputStream imageStream = getContentResolver().openInputStream(selectedImage);
            Bitmap yourSelectedImage = BitmapFactory.decodeStream(imageStream);

            File fileCustomTiles = getIconFile(requestCode);
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(fileCustomTiles);
                yourSelectedImage.compress(Bitmap.CompressFormat.PNG, 90, out);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (out != null)
                        out.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        catch(IOException e) {
            Log.e(LOG_TAG, "ERROR");
            Log.e(LOG_TAG, e.toString());
        }
    }

    private void clearSaveFile(int tile) {
        // Delete the custom icon for this tile
        File customIconFile = getIconFile(tile);
        customIconFile.delete();
    }

    private File getIconFile(int tile) {
        return new File(getFilesDir(), getString(R.string.file_custom_tile_icons) + tile);
    }

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
