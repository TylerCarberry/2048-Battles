package com.tytanapps.game2048;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.NumberPicker;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;


public class CustomGameActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_game);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_custom_game, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onClick(View view) {

        Log.d("a", "Entering onclick");

        switch(view.getId()) {
            case R.id.create_game_button:
                createGame();
        }
    }

    private void createGame() {

        Log.d("a", "Entering creategame");

        int width = ((NumberPicker)findViewById(R.id.width_number_picker)).getValue();
        int height = ((NumberPicker)findViewById(R.id.height_number_picker)).getValue();
        boolean xmode = ((CheckBox)findViewById(R.id.xmode_checkbox)).isChecked();
        boolean cornerMode = ((CheckBox)findViewById(R.id.corner_mode_checkbox)).isChecked();
        boolean speedMode = ((CheckBox)findViewById(R.id.speed_mode_checkbox)).isChecked();
        boolean surivalMode = ((CheckBox)findViewById(R.id.survival_mode_checkbox)).isChecked();
        boolean rushMode = ((CheckBox)findViewById(R.id.rush_mode_checkbox)).isChecked();
        boolean ghostMode = ((CheckBox)findViewById(R.id.ghost_mode_checkbox)).isChecked();


        if(! isCustomGameValid(width, height, xmode, cornerMode, speedMode, surivalMode, rushMode))
            return;

        Game game = new Game(width, height);

        if(xmode)
            game.enableXMode();
        if(cornerMode)
            game.enableCornerMode();
        if(surivalMode)
            game.enableSurvivalMode();
        game.setSpeedMode(speedMode);
        game.setDynamicTileSpawning(rushMode);
        game.setGhostMode(ghostMode);
        game.finishedCreatingGame();

        File currentGameFile = new File(getFilesDir(), getString(R.string.file_current_game));
        try {
            Save.save(game, currentGameFile);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        // Switch to the game activity
        startActivity(new Intent(this, GameActivity.class));


    }

    private boolean isCustomGameValid(int width, int height, boolean xmode, boolean cornerMode,
                                      boolean speedMode, boolean surivalMode, boolean rushMode) {

        if(width * height < 1) {
            Toast.makeText(this, getString(R.string.error_grid_small), Toast.LENGTH_LONG).show();
            return false;
        }

        // If corner mode is enabled the width and height must be >2 and cannot both be 2
        if(cornerMode && ((width == 2 && height == 2) || (width < 2 || height < 2))) {
            Toast.makeText(this, "Corner Mode will not fit on that grid size", Toast.LENGTH_LONG).show();
            return false;
        }

        if(xmode && width * height <= 2) {
            Toast.makeText(this, "XMode cannot fit on that grid size", Toast.LENGTH_LONG).show();
            return false;
        }

        if(xmode && cornerMode && width * height <= 7) {
            Toast.makeText(this, "XMode and Corner Mode cannot fit on that grid size", Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
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
            View rootView = inflater.inflate(R.layout.fragment_custom_game, container, false);

            NumberPicker widthNumberPicker = (NumberPicker) rootView.findViewById(R.id.width_number_picker);
            NumberPicker heightNumberPicker = (NumberPicker) rootView.findViewById(R.id.height_number_picker);

            String[] values=new String[5];
            for(int i = 0; i < values.length; i++){
                values[i] = ""+(i+1);
            }

            for(String s : values)
                Log.d("a", s);

            widthNumberPicker.setMaxValue(5);
            widthNumberPicker.setMinValue(1);
            widthNumberPicker.setDisplayedValues(values);

            heightNumberPicker.setMaxValue(5);
            heightNumberPicker.setMinValue(1);
            heightNumberPicker.setDisplayedValues(values);


            return rootView;
        }
    }
}
