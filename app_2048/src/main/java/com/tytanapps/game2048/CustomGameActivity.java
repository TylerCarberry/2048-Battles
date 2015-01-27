package com.tytanapps.game2048;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class CustomGameActivity extends Activity {

    private static final int MAX_GRID_SIZE = 15;

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
        switch(view.getId()) {
            case R.id.create_game_button:
                createGame();
                break;
        }
    }

    private void createGame() {
        int width = ((NumberPicker)findViewById(R.id.width_number_picker)).getValue();
        int height = ((NumberPicker)findViewById(R.id.height_number_picker)).getValue();

        List<Game.Mode> gameModes = getSelectedGameModes();
        if(! isCustomGameValid(width, height, gameModes))
            return;

        Game game = new Game(width, height);
        game.setGameModeId(GameModes.CUSTOM_MODE_ID);

        if(gameModes.contains(Game.Mode.XMODE))
            game.enableXMode();
        if(gameModes.contains(Game.Mode.CORNER))
            game.enableCornerMode();
        if(gameModes.contains(Game.Mode.SURVIVAL))
            game.enableSurvivalMode();
        game.setSpeedMode(gameModes.contains(Game.Mode.SPEED));
        game.setDynamicTileSpawning(gameModes.contains(Game.Mode.RUSH));
        game.setGhostMode(gameModes.contains(Game.Mode.GHOST));
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

    private List<Game.Mode> getSelectedGameModes() {
        boolean xmode = ((CheckBox)findViewById(R.id.xmode_checkbox)).isChecked();
        boolean cornerMode = ((CheckBox)findViewById(R.id.corner_mode_checkbox)).isChecked();
        boolean speedMode = ((CheckBox)findViewById(R.id.speed_mode_checkbox)).isChecked();
        boolean surivalMode = ((CheckBox)findViewById(R.id.survival_mode_checkbox)).isChecked();
        boolean rushMode = ((CheckBox)findViewById(R.id.rush_mode_checkbox)).isChecked();
        boolean ghostMode = ((CheckBox)findViewById(R.id.ghost_mode_checkbox)).isChecked();

        List<Game.Mode> gameModes = new ArrayList<Game.Mode>();

        if(xmode)
            gameModes.add(Game.Mode.XMODE);
        if(cornerMode)
            gameModes.add(Game.Mode.CORNER);
        if(speedMode)
            gameModes.add(Game.Mode.SPEED);
        if(surivalMode)
            gameModes.add(Game.Mode.SURVIVAL);
        if(rushMode)
            gameModes.add(Game.Mode.RUSH);
        if(ghostMode)
            gameModes.add(Game.Mode.GHOST);

        return gameModes;
    }

    private void updateGamePreview() {
        int width = ((NumberPicker)findViewById(R.id.width_number_picker)).getValue();
        int height = ((NumberPicker)findViewById(R.id.height_number_picker)).getValue();

        List<Game.Mode> gameModes = getSelectedGameModes();
        View gamePreview =  generateGamePreview(height, width, gameModes);

        FrameLayout gamePreviewFrame = (FrameLayout) findViewById(R.id.game_preview_game_layout);
        gamePreviewFrame.removeAllViews();
        gamePreviewFrame.addView(gamePreview);

    }

    private View generateGamePreview(int width, int height, List<Game.Mode> modes) {
        GridLayout gridLayout = new GridLayout(this);
        gridLayout.setColumnCount(width);
        gridLayout.setRowCount(height);

        final int SPACING = 20;


        FrameLayout gamePreviewFrame = (FrameLayout) findViewById(R.id.game_preview_game_layout);
        //Point size = new Point();
        //gamePreviewFrame.getSize(size);
        int previewWidth = gamePreviewFrame.getMeasuredWidth();
        int previewHeight = gamePreviewFrame.getMeasuredHeight();

        int tileHeight = previewHeight / height - SPACING;
        int tileWidth = previewWidth / width - SPACING;

        int tileLength = (tileWidth < tileHeight) ? tileWidth : tileHeight;

        Bitmap blankTileBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.tile_blank);
        blankTileBitmap = (Bitmap.createScaledBitmap(blankTileBitmap, tileLength, tileLength, false));

        for(int x = 0; x < width; x++) {
            for(int y = 0; y < height; y++) {
                ImageView tileBackground = new ImageView(this);
                tileBackground.setImageBitmap(blankTileBitmap);
                tileBackground.setPadding(SPACING, SPACING, 0, 0);
                gridLayout.addView(tileBackground);

            }
        }

        int cornerTileResource = (modes.contains(Game.Mode.GHOST)) ? R.drawable.tile_question : R.drawable.tile_corner;
        int xTileResource = (modes.contains(Game.Mode.GHOST)) ? R.drawable.tile_question : R.drawable.tile_x;

        Bitmap xTileBitmap = BitmapFactory.decodeResource(getResources(), xTileResource);
        xTileBitmap = (Bitmap.createScaledBitmap(xTileBitmap, tileLength, tileLength, false));

        Bitmap cornerTileBitmap = BitmapFactory.decodeResource(getResources(), cornerTileResource);
        cornerTileBitmap = (Bitmap.createScaledBitmap(cornerTileBitmap, tileLength, tileLength, false));



        if(modes.contains(Game.Mode.XMODE)) {
            ImageView XTile = new ImageView(this);
            XTile.setImageBitmap(xTileBitmap);
            XTile.setPadding(SPACING, SPACING, 0, 0);
            GridLayout.Spec specRow, specCol;

            if (height >= 2 && width >= 2) {
                specCol = GridLayout.spec(1, 1);
                specRow = GridLayout.spec(1, 1);
            }
            else {
                if (modes.contains(Game.Mode.CORNER) && (height >= 3 || width >= 3)) {
                    if (width > 2) {
                        specCol = GridLayout.spec(1, 1);
                        specRow = GridLayout.spec(0, 1);
                    } else {
                        specCol = GridLayout.spec(0, 1);
                        specRow = GridLayout.spec(1, 1);
                    }
                }
                else {
                    specCol = GridLayout.spec(0, 1);
                    specRow = GridLayout.spec(0, 1);
                }
            }
            GridLayout.LayoutParams gridLayoutParam = new GridLayout.LayoutParams(specRow, specCol);
            gridLayout.addView(XTile, gridLayoutParam);
        }

        if(modes.contains(Game.Mode.CORNER)) {
            GridLayout.Spec specRow = GridLayout.spec(0, 1);
            GridLayout.Spec specCol = GridLayout.spec(0, 1);
            GridLayout.LayoutParams gridLayoutParam = new GridLayout.LayoutParams(specRow, specCol);

            // Add a blank tile to that spot on the grid
            ImageView cornerTile = new ImageView(this);
            cornerTile.setImageBitmap(cornerTileBitmap);
            cornerTile.setPadding(SPACING, SPACING, 0, 0);
            gridLayout.addView(cornerTile, gridLayoutParam);

            cornerTile = new ImageView(this);
            cornerTile.setImageBitmap(cornerTileBitmap);
            cornerTile.setPadding(SPACING, SPACING, 0, 0);
            specRow = GridLayout.spec(height - 1, 1);
            specCol = GridLayout.spec(0, 1);
            gridLayoutParam = new GridLayout.LayoutParams(specRow, specCol);
            gridLayout.addView(cornerTile, gridLayoutParam);

            cornerTile = new ImageView(this);
            cornerTile.setImageBitmap(cornerTileBitmap);
            cornerTile.setPadding(SPACING, SPACING, 0, 0);
            specRow = GridLayout.spec(height - 1, 1);
            specCol = GridLayout.spec(width - 1, 1);
            gridLayoutParam = new GridLayout.LayoutParams(specRow, specCol);
            gridLayout.addView(cornerTile, gridLayoutParam);

            cornerTile = new ImageView(this);
            cornerTile.setImageBitmap(cornerTileBitmap);
            cornerTile.setPadding(SPACING, SPACING, 0, 0);
            specRow = GridLayout.spec(0, 1);
            specCol = GridLayout.spec(width - 1, 1);
            gridLayoutParam = new GridLayout.LayoutParams(specRow, specCol);
            gridLayout.addView(cornerTile, gridLayoutParam);
        }

        gridLayout.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        return gridLayout;
    }

    private boolean isCustomGameValid(int width, int height, List<Game.Mode> modes) {

        int availableTiles = width * height;

        if(modes.contains(Game.Mode.XMODE))
            availableTiles--;

        if(modes.contains(Game.Mode.CORNER)) {
            if(width >= 2 && height >= 2)
                availableTiles -= 4;
            else
                availableTiles -= 2;
        }


        if(availableTiles < 2) {
            Toast.makeText(this, getString(R.string.error_grid_small), Toast.LENGTH_LONG).show();
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

            String[] values=new String[MAX_GRID_SIZE];
            for(int i = 0; i < values.length; i++){
                values[i] = ""+(i+1);
            }

            widthNumberPicker.setMaxValue(MAX_GRID_SIZE);
            widthNumberPicker.setMinValue(1);
            widthNumberPicker.setDisplayedValues(values);
            widthNumberPicker.setValue(4);

            heightNumberPicker.setMaxValue(MAX_GRID_SIZE);
            heightNumberPicker.setMinValue(1);
            heightNumberPicker.setDisplayedValues(values);
            heightNumberPicker.setValue(4);


            widthNumberPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                @Override
                public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                    ((CustomGameActivity)getActivity()).updateGamePreview();
                }
            });

            heightNumberPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                @Override
                public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                    ((CustomGameActivity) getActivity()).updateGamePreview();
                }
            });

            CheckBox xModeCheckbox = (CheckBox) rootView.findViewById(R.id.xmode_checkbox);
            CheckBox cornerCheckbox = (CheckBox) rootView.findViewById(R.id.corner_mode_checkbox);
            CheckBox arcadeCheckbox = (CheckBox) rootView.findViewById(R.id.arcade_mode_checkbox);
            CheckBox speedCheckbox = (CheckBox) rootView.findViewById(R.id.speed_mode_checkbox);
            CheckBox survivalCheckbox = (CheckBox) rootView.findViewById(R.id.survival_mode_checkbox);
            CheckBox rushCheckbox = (CheckBox) rootView.findViewById(R.id.rush_mode_checkbox);
            CheckBox ghostCheckbox = (CheckBox) rootView.findViewById(R.id.ghost_mode_checkbox);


            CompoundButton.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    ((CustomGameActivity)getActivity()).updateGamePreview();
                }
            };

            xModeCheckbox.setOnCheckedChangeListener(onCheckedChangeListener);
            cornerCheckbox.setOnCheckedChangeListener(onCheckedChangeListener);
            arcadeCheckbox.setOnCheckedChangeListener(onCheckedChangeListener);
            speedCheckbox.setOnCheckedChangeListener(onCheckedChangeListener);
            survivalCheckbox.setOnCheckedChangeListener(onCheckedChangeListener);
            rushCheckbox.setOnCheckedChangeListener(onCheckedChangeListener);
            ghostCheckbox.setOnCheckedChangeListener(onCheckedChangeListener);



            return rootView;
        }

        @Override
        public void onStart() {
            super.onStart();
            //((CustomGameActivity)getActivity()).updateGamePreview();
        }
    }
}
