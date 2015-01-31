package com.tytanapps.game2048;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.backup.BackupManager;
import android.app.backup.RestoreObserver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.event.Event;
import com.google.android.gms.games.event.Events;
import com.google.example.games.basegameutils.BaseGameActivity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * The only fragment in the activity. Has the game board and the
 * game info such as score or turn number
 */
public class GameFragment extends Fragment implements GestureDetector.OnGestureListener {

    private final static String LOG_TAG = GameFragment.class.getSimpleName();

    // The time in milliseconds for the animation
    public static final long SHUFFLE_SPEED = 300;

    // The % chance of a bonus or attack each move in arcade mode
    public static final double CHANCE_OF_ARCADE_BONUS = 0.10;

    // The time in milliseconds before a new tile appears in speed mode
    public static final int SPEED_MODE_DELAY = 1000;

    // These values are overridden with the options chosen in the settings
    private static long tileSlideSpeed = 125;
    private static long swipeSensitivity = 75;

    private static boolean boardCreated = false;
    private static Game game;

    private GridLayout gridLayout;

    // Warns about making a making a move that may lose the game
    private boolean XTileAttackActive = false;
    private boolean ghostAttackActive = false;

    // Used to detect swipes and move the board
    private GestureDetectorCompat mDetector;

    // Becomes false when the game is moved and becomes true in onDown
    private boolean listenForSwipe = true;

    private boolean animationInProgress = false;

    // This only becomes true after the lose message is shown
    private boolean gameLost = false;

    // This keeps track of the active animations and
    // stops them in onStop
    private ArrayList<ObjectAnimator> activeAnimations
            = new ArrayList<ObjectAnimator>();

    // Stores info about the game such as high score
    private static GameData gameStats;

    // The distance in pixels between tiles
    private static int verticalTileDistance = 0;
    private static int horizontalTileDistance = 0;

    // Stores custom tile icons
    private Map<Integer, Drawable> customTileIcon = new HashMap<Integer, Drawable>();

    // Stores a cache of the scaled tiles
    private SparseArray<Drawable> tileIcons = new SparseArray<Drawable>();

    private List<Timer> activeTimers = new ArrayList<Timer>();

    private int secondsRemaining = GameModes.SURVIVAL_MODE_TIME;

    private boolean multiplayerActive = false;

    public GameFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // The game has a different layout for single and multiplayer games
        View rootView;
        if(container == null) {
            multiplayerActive = false;

            rootView = inflater.inflate(R.layout.fragment_game, container, false);

            final ImageButton undoButton = (ImageButton) rootView.findViewById(R.id.undo_button);
            undoButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    //showCongratulationsDialog(2048);
                    undo();
                }
            });

            final ImageButton restartButton = (ImageButton) rootView.findViewById(R.id.restart_button);
            restartButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    /*
                    Grid newGrid = game.getGrid();
                    List<Location> tiles = newGrid.toList();
                    for (Location tile : tiles)
                        newGrid.set(tile, newGrid.get(tile) * 2);
                    game.setGrid(newGrid);
                    updateGame();
                    */

                    //restartGame();

                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
                    if(prefs.getBoolean(getString(R.string.preference_prompt_restart), true))
                        promptRestartGame();
                    else
                        restartGame();

                }
            });
        }
        else {
            multiplayerActive = true;
            rootView = inflater.inflate(R.layout.fragment_multiplayer_game, container, false);
        }

        // Start listening for swipes
        rootView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return onTouchEvent(event);
            }
        });
        mDetector = new GestureDetectorCompat(getActivity(),this);

        return rootView;
    }

    @Override
    public void onStart() {
        // If GameActivity is loaded for the first time the grid is created. If user returns to
        // this activity after switching to another activity, the grid is still recreated because
        // there is a chance that android killed this activity in the background
        boardCreated = false;

        gridLayout = (GridLayout) getView().findViewById(R.id.grid_layout);

        // Load the custom tiles and place them in the Map customTileIcon to be accessed later
        //loadCustomTileIcons();

        // Load the saved file containing the game. This also updates the screen.
        load();

        if(game.getSpeedMode()) {
            activateSpeedMode();
        }
        if(game.getSurvivalMode()) {
            activateSurvivalMode();
        }

        // Disable the undo and powerup buttons based on the undos/powerups remaining
        if(! multiplayerActive) {
            setUndoButtonEnabled(game.getUndosRemaining() != 0);
            setPowerupButtonEnabled(game.getPowerupsRemaining() != 0);

            // When the powerup button is pressed it switches its background to one suppressed
            // When it is released its background changes back and the powerup dialog is shown
            ImageButton powerupButton = (ImageButton) getView().findViewById(R.id.powerup_button);
            powerupButton.setOnTouchListener(new View.OnTouchListener() {

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN)
                        v.setBackgroundDrawable(getResources().getDrawable(R.drawable.powerup_button_selected));
                    else if (event.getAction() == MotionEvent.ACTION_UP) {
                        v.setBackgroundDrawable(getResources().getDrawable(R.drawable.powerup_button));
                        showPowerupDialog();
                    }
                    return true;
                }
            });

            if(game.getGameModeId() == GameModes.CUSTOM_MODE_ID) {
                getView().findViewById(R.id.high_score_textview).setVisibility(View.GONE);
                getView().findViewById(R.id.turn_textview).setVisibility(View.VISIBLE);
            }
            else {
                getView().findViewById(R.id.high_score_textview).setVisibility(View.VISIBLE);
                getView().findViewById(R.id.turn_textview).setVisibility(View.GONE);
            }
        }

        // Load the settings for the tile speed and sensitivity
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        swipeSensitivity = Integer.valueOf(prefs.getString(getString(R.string.preference_swipe_sensitivity), "75"));
        tileSlideSpeed = Integer.valueOf(prefs.getString(getString(R.string.preference_slide_speed), "125"));

        // Start the multiplayer aspects of the game if necessary
        if(multiplayerActive) {
            ((MultiplayerActivity) getActivity()).gameHasLoaded();

            // There is a 3 second delay before a multiplayer game starts
            //createCountdown(3 * 1000);

            // If the user chose to hide their identity do not show them their own identity
            // This confirms the change to the user as they cannot see their opponents screen
            if(! prefs.getBoolean(getString(R.string.preference_hide_identity), false)) {
                updatePlayerName();
                updatePlayerPic();
            }
            // Show the opponent's first name and profile picture.
            // If the opponent hid their identity these methods do nothing
            updateOpponentName();
            updateOpponentPic();
        }

        super.onStart();
    }

    @Override
    public void onPause() {

        if (getApiClient().isConnected()) {
            Games.Events.increment(this.getApiClient(), getString(R.string.event_tiles_combined), game.getTilesCombined());
            game.resetTilesCombined();
        }

        // Only save a game that is still in progress and not a multiplayer game
        if(! (game.isGameLost() || gameLost || multiplayerActive))
            save();
        super.onPause();
    }


    @Override
    public void onStop() {
        // Stop all active animations. If this is not done the game will crash
        for(ObjectAnimator animation : activeAnimations)
            animation.end();
        activeAnimations.clear();
        animationInProgress = false;

        for(Timer t : activeTimers)
            t.cancel();
        activeTimers.clear();

        // The achievement long time player is unlocked after 2048 moves are made. The game will
        // still call the api every time afterwards but for now this is not a major issue
        if(gameStats.getTotalMoves() >= 2048 && getApiClient().isConnected())
            Games.Achievements.unlock(getApiClient(), getString(R.string.achievement_long_time_player));

        super.onStop();
    }

    /**
     * Moves all of the tiles
     * The game has an imageview as a tile in each position of the grid The tiles move to the
     * correct position and get reset after each move. This means that after each move there is
     * one imageview at every position even if the space is empty
     *
     * | 16 | 8 | 4 | x |
     * | 4  | 2 | 2 | x | --> Moved left --> The tiles slide into their new position
     * | x  | 4 | x | x |                    The moved tiles get reset to their original position
     * | x  | x | x | x |                    Every tile is updated with its new value
     *
     * @param direction Should use the static variables in Location class
     */
    protected void act(int direction) {
        animationInProgress = true;

        // If the ice attack is active in that direction do not move
        if((game.getAttackDuration() > 0 && game.getIceDirection() == direction)) {
            emphasizeAttack();
            animationInProgress = false;
            return;
        }

        // A game cannot be moved after it is lost
        if(gameLost) {
            animationInProgress = false;
            return;
        }

        // If the genie is active and the player is about to lose a dialog appears warning the user
        if(game.getGenieEnabled() && game.causeGameToLose(direction)) {
            animationInProgress = false;
            warnAboutMove(direction);
            return;
        }

        // Calculate the distance that tiles should move when the game is swiped
        calculateDistances();

        final Game lastGame = game.clone();

        // This is compared to the highest tile after the move in order to unlock achievements
        int highestTile = game.highestPiece();

        // An list of the move animations to play
        ArrayList<ObjectAnimator> translateAnimations = new ArrayList<ObjectAnimator>();

        // Get a list of all tile locations and loop through each tile
        List<Location> tiles = game.getGrid().getLocationsInTraverseOrder(direction);
        for(Location tile : tiles) {
            // Move the piece and determine the number of spaces it moved
            int distance = game.move(tile, direction);

            // Only animate buttons that moved
            if(distance > 0) {
                ImageView movedTile = findTileByLocation(tile);

                if(direction == Location.LEFT || direction == Location.UP)
                    distance *= -1;

                // The tag is changed to a value different than its actual value
                // which causes it to be updated in updateGrid
                movedTile.setTag(-10);

                // Determine the distance to move in pixels
                ObjectAnimator animation;
                if(direction == Location.LEFT || direction == Location.RIGHT) {
                    distance *= horizontalTileDistance;
                    animation = ObjectAnimator.ofFloat(movedTile, View.TRANSLATION_X, distance);
                }
                else {
                    distance *= verticalTileDistance;
                    animation = ObjectAnimator.ofFloat(movedTile, View.TRANSLATION_Y, distance);
                }

                // Time in milliseconds to move the tile
                animation.setDuration(tileSlideSpeed);

                // Add the new animation to the list
                translateAnimations.add(animation);
            }
        }

        if(translateAnimations.isEmpty()) {
            animationInProgress = false;

            if(game.isGameLost())
                lost();

            return;
        }

        translateAnimations.get(0).addListener(new Animator.AnimatorListener(){
            @Override
            // When the animation is over increment the turn number, update the game,
            // and add a new tile
            public void onAnimationEnd(Animator animation) {

                updateGame();
                gameStats.incrementTotalMoves(1);

                if(gameStats.getTotalMoves() == 2048 && getApiClient().isConnected()) {
                    Games.Achievements.unlock(getApiClient(), getString(R.string.achievement_long_time_player));
                }

                activeAnimations.clear();
                animationInProgress = false;

                if(game.getArcadeMode() && Math.random() < CHANCE_OF_ARCADE_BONUS)
                    addRandomBonus();

                game.newTurn();
                updateTextviews();

                if(game.getAttackDuration() == 0) {
                    if(XTileAttackActive)
                        endXAttack();
                    else if(ghostAttackActive)
                        endGhostAttack();
                }

                addTile();

                // Save the game history before each move
                game.saveLastGameToHistory(lastGame);

                if(game.isGameLost())
                    lost();
            }

            @Override
            public void onAnimationStart(Animator animation) { }
            @Override
            public void onAnimationCancel(Animator animation) {
                animationInProgress = false;
            }
            @Override
            public void onAnimationRepeat(Animator animation) { }
        });

        // Move all of the tiles
        for(ObjectAnimator animation: translateAnimations) {
            animation.start();
            activeAnimations.add(animation);
        }

        // If a new highest tile is created this move unlock an achievement
        if(game.highestPiece() > highestTile) {
            if (game.highestPiece() >= 128 && game.getGameModeId() == GameModes.NORMAL_MODE_ID)
                unlockAchievementNewHighestTile(game.highestPiece());
            if(game.highestPiece() == 2048)
                showCongratulationsDialog(game.highestPiece());
        }

        if(multiplayerActive) {
                ((MultiplayerActivity) getActivity()).sendMessage("s" + game.getScore(), false);
        }
    }

    /**
     * Update the entire screen: the grid and game information.
     */
    public void updateGame() {
        updateTextviews();
        updateGrid();
    }

    /**
     * Update the turn number, score, number of undos and powerups left, and the active attack
     */
    protected void updateTextviews() {
        TextView gameModeTextView, turnTextView, scoreTextView, highScoreTextView,
                timeLeftTextView, undosTextView, powerupsTextView, activeAttacksTextView;
        try {
            gameModeTextView = (TextView) getView().findViewById(R.id.game_mode_textview);
            turnTextView = (TextView) getView().findViewById(R.id.turn_textview);
            scoreTextView = (TextView) getView().findViewById(R.id.score_textview);
            highScoreTextView = (TextView) getView().findViewById(R.id.high_score_textview);
            timeLeftTextView = (TextView) getView().findViewById(R.id.time_left_textview);
            undosTextView = (TextView) getView().findViewById(R.id.undos_textview);
            powerupsTextView = (TextView) getView().findViewById(R.id.powerups_textview);
            activeAttacksTextView = (TextView) getView().findViewById(R.id.active_attacks_textview);
        } catch (NullPointerException e) {
            return;
        }

        // Update attacks
        if(! multiplayerActive || ((MultiplayerActivity)(getActivity())).isMultiplayerActive())
            activeAttacksTextView.setText(getAttackString());

        if(! multiplayerActive) {
            if(game.getGameModeId() == GameModes.CUSTOM_MODE_ID)
                // Update the turn number
                turnTextView.setText(getString(R.string.turn) + " #" + game.getTurns());
            else {
                // Update the high score
                int highScore = Math.max(game.getScore(), gameStats.getHighScore(game.getGameModeId()));
                highScoreTextView.setText(String.format(getString(R.string.high_score), highScore));
            }

            // Update the score
            scoreTextView.setText(getString(R.string.score) + ": " + game.getScore());

            timeLeftTextView.setVisibility((game.getSurvivalMode() ? View.VISIBLE : View.INVISIBLE));
            timeLeftTextView.setText(""+getSecondsRemaining());

            gameModeTextView.setText(GameModes.getGameTitleById(game.getGameModeId()));

            // Update the undos left
            int undosLeft = (game.getUseItemInventory()) ? gameStats.getUndoInventory() : game.getUndosRemaining();
            if (undosLeft <= 0) {
                undosTextView.setVisibility(View.INVISIBLE);
                undosTextView.setText("");
                if (undosLeft == 0)
                    setUndoButtonEnabled(false);
            } else {
                undosTextView.setVisibility(View.VISIBLE);
                undosTextView.setText("" + undosLeft);
                setUndoButtonEnabled(true);
            }

            // Update the powerups left
            int powerupsLeft = (game.getUseItemInventory()) ? gameStats.getPowerupInventory() : game.getPowerupsRemaining();
            if (powerupsLeft <= 0) {
                powerupsTextView.setVisibility(View.INVISIBLE);
                powerupsTextView.setText("");
                if (powerupsLeft == 0)
                    setPowerupButtonEnabled(false);
            } else {
                powerupsTextView.setVisibility(View.VISIBLE);
                powerupsTextView.setText("" + powerupsLeft);
                setPowerupButtonEnabled(true);
            }
        }
    }

    /**
     * Create the game board. This places blank tiles at every position in the grid layout
     */
    private void createGrid() {
        // The grid that all tiles are on
        GridLayout gridLayout = (GridLayout) getView().findViewById(R.id.grid_layout);

        // Set the number of rows and columns in the game
        gridLayout.setRowCount(game.getGrid().getNumRows());
        gridLayout.setColumnCount(game.getGrid().getNumCols());

        // The new tile to insert
        ImageView tile;
        // Used to check if the tile already exists
        ImageView existingTile;
        GridLayout.Spec specRow, specCol;
        GridLayout.LayoutParams gridLayoutParam;
        int tileValue;

        List<Location> tileLocations = game.getGrid().toList();
        for(Location tileLoc : tileLocations) {
            specRow = GridLayout.spec(tileLoc.getRow(), 1);
            specCol = GridLayout.spec(tileLoc.getCol(), 1);
            gridLayoutParam = new GridLayout.LayoutParams(specRow, specCol);

            // Add a blank tile to that spot on the grid
            ImageView blankTile = new ImageView(getActivity());
            setIcon(blankTile, 0);
            ((GridLayout) getActivity().findViewById(R.id.grid_layout)).addView(blankTile, gridLayoutParam);

            // Check if that tile already exists
            existingTile = findTileByLocation(tileLoc);

            // Remove the existing tile if there is one
            if(existingTile!=null)
                ((ViewGroup) existingTile.getParent()).removeView(existingTile);

            tile = new ImageView(getActivity());
            tile.setId(getTileIdByLocation(tileLoc));

            // Tiles without a value become invisible
            tileValue = game.getGrid().get(tileLoc);
            tile.setVisibility((tileValue == 0) ? View.INVISIBLE : View.VISIBLE);

            gridLayout.addView(tile, gridLayoutParam);
        }
        boardCreated = true;
    }

    /**
     * Update the game board
     */
    private void updateGrid() {

        if(! boardCreated)
            createGrid();

        GridLayout gridLayout = (GridLayout) getView().findViewById(R.id.grid_layout);
        gridLayout.setRowCount(game.getGrid().getNumRows());
        gridLayout.setColumnCount(game.getGrid().getNumCols());

        ImageView tile;
        GridLayout.Spec specRow, specCol;
        GridLayout.LayoutParams gridLayoutParam;
        int expectedValue, actualValue;

        List<Location> tileLocations = game.getGrid().toList();

        for(Location tileLoc : tileLocations) {
            tile = findTileByLocation(tileLoc);
            expectedValue = game.getGrid().get(tileLoc);

            // A tiles's tag is its value
            try {
                actualValue = Integer.parseInt(tile.getTag().toString());
            }
            catch(Exception e) {
                // Update the tile just in case
                actualValue = -10;
            }

            if(expectedValue != actualValue) {
                Drawable tileBackground = tile.getBackground();

                specRow = GridLayout.spec(tileLoc.getRow(), 1);
                specCol = GridLayout.spec(tileLoc.getCol(), 1);
                gridLayoutParam = new GridLayout.LayoutParams(specRow, specCol);

                // Remove the tile
                ViewGroup layout = (ViewGroup) tile.getParent();
                if (null != layout)
                    layout.removeView(tile);

                // Create a new tile to insert back into the board
                tile = new ImageView(getActivity());
                tile.setId(getTileIdByLocation(tileLoc));
                tile.setTag(expectedValue);

                if(! game.getDestinationLocations().contains(tileLoc))
                    setIcon(tile, expectedValue);
                else
                    tile.setBackgroundDrawable(tileBackground);

                if (expectedValue == 0)
                    tile.setVisibility(View.INVISIBLE);
                else
                    tile.setVisibility(View.VISIBLE);

                // Insert the new tile into the board
                gridLayout.addView(tile, gridLayoutParam);
            }
        }

        for(Location combinedLoc : game.getDestinationLocations())
            animateTileCombine(findTileByLocation(combinedLoc));

        if(game.isGameLost())
            lost();
    }

    private void animateTileCombine(ImageView tile) {
        Location tileLoc = getTileLocationById(tile.getId());
        tile.setVisibility(View.VISIBLE);

        int tileValue = game.getGrid().get(tileLoc);
        tile.setTag(tileValue);

        // Increment the time left on survival mode when tiles 8 or higher combine
        // 8's combine --> +2 seconds
        // 16's --> +3 seconds
        // 32's --> +4 seconds ...
        //incrementTimeLeft((int) (Math.log10(tileValue/4)/Math.log10(2)));


        if(game.getSurvivalMode() && tileValue >= 16)
            incrementTimeLeft(2);

        Drawable[] layers = new Drawable[2];

        // The current icon
        // I used a workaround to fix a bug that was caused when both of the tiles that
        // combine are moving. This will causes issues when I implement zen mode because this
        // code expects two similar tiles to combine
        layers[0] = getTileIconDrawable(tileValue);

        // The new icon
        layers[1] = getTileIconDrawable(tileValue);

        TransitionDrawable transition = new TransitionDrawable(layers);
        tile.setImageDrawable(transition);
        transition.startTransition((int) tileSlideSpeed);

        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(tile, View.SCALE_X, 1.15f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(tile, View.SCALE_Y, 1.15f);

        // The tile increases in size by a factor of 1.1 and shrinks back down. At the same time
        // it is fading to the new value.
        scaleDownX.setDuration(tileSlideSpeed / 2);
        scaleDownY.setDuration(tileSlideSpeed / 2);
        scaleDownX.setRepeatCount(1);
        scaleDownX.setRepeatMode(ObjectAnimator.REVERSE);
        scaleDownY.setRepeatCount(1);
        scaleDownY.setRepeatMode(ObjectAnimator.REVERSE);

        AnimatorSet scaleDown = new AnimatorSet();

        scaleDown.play(scaleDownX).with(scaleDownY);
        scaleDown.start();
        transition.startTransition((int) tileSlideSpeed);

    }

    private void setIcon(ImageView tile, int tileValue) {
        tile.setBackgroundDrawable(getTileIconDrawable(tileValue));
    }

    private Drawable getTileIconDrawable(int tileValue) {
        if(tileValue != 0 && (game.getGhostMode() || game.getActiveAttack() == Game.GHOST_ATTACK
                || ghostAttackActive))
            tileValue = Game.GHOST_TILE_VALUE;

        Drawable cachedDrawable = tileIcons.get(tileValue);
        if(cachedDrawable != null)
            return cachedDrawable;

        int tileSize = calculateTileSize(game.getGrid().getNumRows(), game.getGrid().getNumCols());
        Bitmap tileDrawable = BitmapFactory.decodeResource(getResources(), getTileIconResource(tileValue));
        Bitmap tileBitmap = Bitmap.createScaledBitmap(tileDrawable, tileSize, tileSize, false);
        Drawable resultDrawable = new BitmapDrawable(getResources(), tileBitmap);

        tileIcons.put(tileValue, resultDrawable);

        return resultDrawable;
    }

    /**
     * Calculate the size of a tile on the board.
     * Assume that the screen width is the same distance as
     * from the bottom of the power up button to the top of the ad.
     * @param rows The number of rows in the game
     * @param columns The number of columns in the game
     * @return The dimension of one side of the tile in pixels
     */
    private int calculateTileSize(int rows, int columns) {
        final int TILE_MARGIN = 25;

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        width -= getResources().getDimension(R.dimen.activity_horizontal_margin) * 2;

        // The ad takes up additional space in landscape mode
        height -= getResources().getDimension(R.dimen.activity_vertical_margin) * 2 + 80;

        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (columns > rows)
                return width / columns - TILE_MARGIN;
            return width / rows - TILE_MARGIN;
        }
        else {
            if (columns > rows)
                return height / columns - TILE_MARGIN;
            return height / rows - TILE_MARGIN;
        }
    }

    /**
     * Update the tile's icon to match its value
     * @param tileValue The numerical value of the tile
     */
    private int getTileIconResource(int tileValue) {
        if(tileValue != 0 && (game.getGameModeId() == GameModes.GHOST_MODE_ID || ghostAttackActive))
            return R.drawable.tile_question;
        else {
            switch(tileValue) {
                case Game.GHOST_TILE_VALUE:
                    return R.drawable.tile_question;
                case Game.X_TILE_VALUE:
                    return R.drawable.tile_x;
                case Game.CORNER_TILE_VALUE:
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
                case 4096:
                    return R.drawable.tile_4096;
                // If I did not create an image for the tile, default to a 0 tile
                default:
                    return R.drawable.tile_0;
            }
        }
    }

    private void loadCustomTileIcons() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        for(int tile : Game.getListOfAllTileValues()) {
            File fileCustomTiles = getIconFile(tile);

            if(fileCustomTiles != null) {
                Bitmap bitmap = BitmapFactory.decodeFile(fileCustomTiles.getAbsolutePath(), options);

                if (bitmap != null) {
                    Drawable tileIconDrawable = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(bitmap, 128, 128, true));
                    customTileIcon.put(tile, tileIconDrawable);
                }
            }
        }
    }

    public File getIconFile(int tile) {
        return new File(getActivity().getFilesDir(), getString(R.string.file_custom_tile_icons) + tile);
    }

    /**
     * @return the string telling the user the current attack and duration left
     */
    private String getAttackString() {
        int activeGameAttack = game.getActiveAttack();
        String resultString = "";

        switch(activeGameAttack) {
            case Game.X_ATTACK:
                resultString += String.format(getString(R.string.x_attack_active), game.getAttackDuration());
                break;
            case Game.GHOST_ATTACK:
                resultString += String.format(getString(R.string.ghost_attack_active), game.getAttackDuration());
                break;
            case Game.ICE_ATTACK:
                resultString += String.format(getString(R.string.ice_attack_active),
                        Location.directionToString(game.getIceDirection()),
                        game.getAttackDuration());
                break;
            default:
                return "";
        }
        return resultString;
    }

    private void lost() {
        if(multiplayerActive) {
            Toast.makeText(getActivity(), "You cannot move. Use a powerup to continue", Toast.LENGTH_LONG).show();
            return;
        }

        // Prevent the notification from appearing multiple times
        if(gameLost)
            return;

        gameLost = true;

        // This is the only place where total games played is incremented.
        gameStats.incrementGamesPlayed(1);

        // You cannot act on a game once you lose
        setUndoButtonEnabled(false);
        setPowerupButtonEnabled(false);

        gameStats.updateGameRecords(game.getGameModeId(), game);

        // Save the updated gameStats and delete the current game save file.
        // The user can no longer continue this game.
        save();
        File currentGameFile = new File(getActivity().getFilesDir(), getString(R.string.file_current_game));
        currentGameFile.delete();

        showLoseDialog();

        updateLeaderboards(game.getScore(), game.getGameModeId());
        submitEvents(game);

        if(game.getScore() <= 200 && game.getGameModeId() == GameModes.NORMAL_MODE_ID &&  getApiClient().isConnected()) {
            Games.Achievements.unlock(getApiClient(), getString(R.string.achievement_worst_player_ever));
        }
        if(game.getPowerupsUsed() <= 0 && game.getUndosUsed() <= 0 &&
                game.getGameModeId() == GameModes.PRACTICE_MODE_ID &&  getApiClient().isConnected()) {
            Games.Achievements.unlock(getApiClient(), getString(R.string.achievement_i_dont_want_any_help));
        }

        if(multiplayerActive) {
            ((MultiplayerActivity) getActivity()).sendMessage(getString(R.string.opponent_lost), true);
            sendAnalyticsEvent("Game Fragment", "Game Lost", "Multiplayer");
        }
        else {
            ((GameActivity) getActivity()).displayInterstitial();
            sendAnalyticsEvent("Game Fragment", "Game Lost", "Single Player");
        }
    }

    /** Create the message that is shown to the user after they lose.
     * @param myGame The game that was currently played
     * @param myGameStats The game stats of the game
     * @return The message to display
     */
    private String createLoseMessage(Game myGame, GameData myGameStats) {
        String message = "";
        // Notify if there is a new high score
        if(myGame.getScore() > myGameStats.getHighScore(myGame.getGameModeId())) {
            message += String.format(getString(R.string.new_high_score), myGame.getScore());
        }

        // Notify if there is a new highest tile
        if(myGame.highestPiece() > myGameStats.getHighestTile(myGame.getGameModeId())) {
            if(! message.equals(""))
                message += "\n";
            message += String.format(getString(R.string.new_high_tile), myGame.highestPiece());
        }

        // Only notify if there is a new low score if there are no other records.
        if(myGameStats.getLowestScore(game.getGameModeId()) < 0 ||
                myGame.getScore() < myGameStats.getLowestScore(game.getGameModeId())) {

            if(message.equals(""))
                message += String.format(getString(R.string.new_low_score), myGame.getScore());
        }

        // If there are no records then just show the score
        if(message.equals(""))
            message += String.format(getString(R.string.final_score), myGame.getScore());
        return message;
    }

    private void showCongratulationsDialog(final int tile) {
        animateFlyingTiles(150,15);

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.congratulations));

        LinearLayout linearLayout = new LinearLayout(getActivity());
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        int padding = (int) getResources().getDimension(R.dimen.activity_horizontal_margin);

        // The text instructions
        TextView textView = new TextView(getActivity());

        // TODO: use strings.xml
        textView.setText(tile + " tile reached!");

        textView.setTextSize(22);
        textView.setGravity(Gravity.CENTER_HORIZONTAL);
        textView.setPadding(0, padding, 0, padding);

        ImageView tileImageView = new ImageView(getActivity());
        tileImageView.setPadding(padding, padding, padding, padding);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER;
        tileImageView.setLayoutParams(layoutParams);
        setIcon(tileImageView, tile);


        Button continuePlayingButton = new Button(getActivity());
        continuePlayingButton.setText("Continue Playing");


        Button shareButton = new Button(getActivity());
        shareButton.setText("Brag");
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);

                shareIntent.putExtra(Intent.EXTRA_TEXT, "I just reached a " + tile + " tile in 2048 Battles! Try to beat me! " + MainActivity.APP_URL);
                shareIntent.setType("text/plain");
                startActivity(shareIntent);

                sendAnalyticsEvent("Game Fragment", "Congratulations Dialog", "Share Button");
                if(getApiClient().isConnected())
                    Games.Achievements.unlock(getApiClient(), getString(R.string.achievement_brag_to_your_friends));
            }
        });

        linearLayout.addView(textView);
        linearLayout.addView(tileImageView);
        linearLayout.addView(continuePlayingButton);
        linearLayout.addView(shareButton);

        builder.setView(linearLayout);

        final AlertDialog alertDialog = builder.create();

        continuePlayingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        alertDialog.show();

        sendAnalyticsEvent("Game Fragment", "Congratulations Dialog", "Tile: " + tile);
    }

    public void animateFlyingTiles(final int amount, final int delay) {
        final Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            int times = 0;
            @Override
            public void run() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        animateFlyingTile();
                    }
                });
                times++;
                if(times > amount)
                    timer.cancel();
            }
        }, delay, delay);
    }

    public void animateFlyingTile() {
        final RelativeLayout mainFragment = (RelativeLayout) getView().findViewById(R.id.game_fragment);
        final ImageView tile = new ImageView(getActivity());

        double rand = Math.random();
        if(rand < 0.1)
            tile.setBackgroundResource(R.drawable.tile_2);
        else if(rand < 0.2)
            tile.setBackgroundResource(R.drawable.tile_4);
        else if(rand < 0.3)
            tile.setBackgroundResource(R.drawable.tile_8);
        else if(rand < 0.4)
            tile.setBackgroundResource(R.drawable.tile_16);
        else if(rand < 0.5)
            tile.setBackgroundResource(R.drawable.tile_32);
        else if(rand < 0.6)
            tile.setBackgroundResource(R.drawable.tile_64);
        else if(rand < 0.7)
            tile.setBackgroundResource(R.drawable.tile_256);
        else if(rand < 0.8)
            tile.setBackgroundResource(R.drawable.tile_512);
        else if(rand < 0.9)
            tile.setBackgroundResource(R.drawable.tile_1024);
        else
            tile.setBackgroundResource(R.drawable.tile_2048);

        Display display = getActivity().getWindowManager().getDefaultDisplay();

        int startingX, startingY, endingX, endingY;

        if(Math.random() > 0.5) {
            startingX = (int) (Math.random() * display.getWidth()) - 200;
            startingY = -200;
        }
        else {
            startingX = -200;
            startingY = (int) (Math.random() * display.getHeight()) - 200;
        }

        if(Math.random() > 0.5) {
            endingX = (int) (Math.random() * display.getWidth()) + 200;
            endingY = display.getHeight() + 200;
        }
        else {
            endingX = display.getWidth() + 200;
            endingY = (int) (Math.random() * display.getHeight() + 200);
        }

        if(Math.random() > 0.5) {
            int temp = startingX;
            startingX = endingX;
            endingX = temp;

            temp = startingY;
            startingY = endingY;
            endingY = temp;
        }

        ObjectAnimator animatorX = ObjectAnimator.ofFloat(tile, View.TRANSLATION_X, endingX - startingX);
        ObjectAnimator animatorY = ObjectAnimator.ofFloat(tile, View.TRANSLATION_Y, endingY - startingY);

        animatorX.setDuration(1000);
        animatorY.setDuration(1000);

        animatorX.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mainFragment.removeView(tile);
            }

            @Override
            public void onAnimationStart(Animator animation) {}
            @Override
            public void onAnimationCancel(Animator animation) {}
            @Override
            public void onAnimationRepeat(Animator animation) {}
        });

        RelativeLayout.LayoutParams layoutParams=new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(startingX, startingY, 0, 0);
        tile.setLayoutParams(layoutParams);

        mainFragment.addView(tile);

        animatorX.start();
        animatorY.start();
    }

    /**
     * Updates the leaderboards with the new score
     * @param score The final score of the game
     * @param gameModeId The game mode using the GameModes class
     */
    private void updateLeaderboards(int score, int gameModeId) {
        if(getApiClient().isConnected()){
            String leaderboard = null;
            switch(gameModeId) {
                case GameModes.NORMAL_MODE_ID:
                    leaderboard = getString(R.string.leaderboard_classic_mode);
                    break;
                case GameModes.ARCADE_MODE_ID:
                    leaderboard = getString(R.string.leaderboard_arcade_mode);
                    break;
                case GameModes.X_MODE_ID:
                    leaderboard = getString(R.string.leaderboard_xmode);
                    break;
                case GameModes.CORNER_MODE_ID:
                    leaderboard = getString(R.string.leaderboard_corner_mode);
                    break;
                case GameModes.RUSH_MODE_ID:
                    leaderboard = getString(R.string.leaderboard_rush_mode);
                    break;
            }

            if(leaderboard != null)
                Games.Leaderboards.submitScore(getApiClient(),
                        leaderboard,
                        score);
        }
    }

    /**
     * Update the events on google play games with the game information
     * @param myGame The game to get the data from
     */
    private void submitEvents(Game myGame)
    {
        if(getApiClient().isConnected()) {
            String playedGameId = getString(R.string.event_games_lost);
            String totalMovesId = getString(R.string.event_moves);
            String totalScoreId = getString(R.string.event_score);
            String tilesCombinedId = getString(R.string.event_tiles_combined);

            // Increment the event counters
            Games.Events.increment(this.getApiClient(), playedGameId, 1);
            Games.Events.increment(this.getApiClient(), totalMovesId, myGame.getTurns());
            Games.Events.increment(this.getApiClient(), totalScoreId, myGame.getScore());
            Games.Events.increment(this.getApiClient(), tilesCombinedId, game.getTilesCombined());
        }
    }

    public void callback() {
        // EventCallback is a subclass of ResultCallback; use this to handle the
        // query results
        EventCallback ec = new EventCallback();

        // Load all events tracked for your game
        com.google.android.gms.common.api.PendingResult<Events.LoadEventsResult>
                pr = Games.Events.load(this.getApiClient(), true);
        pr.setResultCallback(ec);
    }

    class EventCallback implements ResultCallback
    {
        // Handle the results from the events load call
        public void onResult(com.google.android.gms.common.api.Result result) {
            Events.LoadEventsResult r = (Events.LoadEventsResult)result;
            com.google.android.gms.games.event.EventBuffer eb = r.getEvents();

            for (int i=0; i < eb.getCount(); i++) {
                Event e = eb.get(i);
                Toast.makeText(getActivity(), "" + e.getValue(), Toast.LENGTH_SHORT).show();
            }
            eb.close();
        }
    }

    protected void showPowerupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        if((!game.isGameLost()) || multiplayerActive) {
            if (game.getPowerupsRemaining() == 0) {
                builder.setTitle("No More Powerups").setMessage("There are no powerups remaining");
            }
            else {
                builder.setTitle(getString(R.string.prompt_choose_powerup)).setItems(R.array.powerups, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Games.Events.increment(getApiClient(), getString(R.string.event_powerups_used), 1);
                        // The 'which' argument contains the index position
                        // of the selected item
                        switch (which) {
                            case 0:
                                shuffleGame();
                                game.decrementPowerupsRemaining();
                                if(game.getUseItemInventory())
                                    gameStats.decrementPowerupInventory();
                                break;
                            case 1:
                                // The number of powerups is decremented in removeTile
                                // after a tile has been selected
                                removeTile();
                                break;
                            case 2:
                                removeLowTiles();
                                game.decrementPowerupsRemaining();
                                if(game.getUseItemInventory())
                                    gameStats.decrementPowerupInventory();
                                break;
                            case 3:
                                game.setGenieEnabled(true);
                                game.decrementPowerupsRemaining();
                                if(game.getUseItemInventory())
                                    gameStats.decrementPowerupInventory();
                                updateTextviews();
                                sendAnalyticsEvent("Game Fragment", "Item Used", "Genie");
                                break;
                        }
                        setPowerupButtonEnabled(game.getPowerupsRemaining() != 0);
                    }
                });
            }
            builder.create().show();
        }
    }

    private void showLoseDialog() {
        // Create a new lose dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.you_lost));
        // Two buttons appear, try again and cancel
        builder.setPositiveButton(R.string.try_again, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                restartGame();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });

        // Create the message to show the player
        String message = createLoseMessage(game, gameStats);
        builder.setMessage(message);
        AlertDialog dialog = builder.create();

        // You must click on one of the buttons in order to dismiss the dialog
        dialog.setCanceledOnTouchOutside(false);

        // Show the dialog
        dialog.show();
    }

    /**
     * @deprecated This method is no longer used because I switched to ImageViews instead of
     * buttons. I may need this method later for zen mode.
     * @param tile The number representation of the tile
     * @return The string representation of the tile
     */
    private String convertToTileText(int tile) {
        switch (tile) {
            case 0:
                return "";
            case -1:
                return "XX";
            case -2:
                return "x";
            default:
                return "" + tile;
        }
    }

    /**
     * Add a new tile to the board.
     * It fades in while expanding from half its size.
     */
    private void addTile() {
        // Add a new tile to the game object
        Location loc = game.addRandomPiece();

        // Find the tile to make appear
        ImageView newTile = findTileByLocation(loc);

        // Immediately set the alpha of the tile to 0 and shrink to half size
        ObjectAnimator.ofFloat(newTile, View.ALPHA, 0).setDuration(0).start();

        // Update the new tile's tag and icon
        int tileValue = game.getGrid().get(loc);
        newTile.setTag(tileValue);
        setIcon(newTile, tileValue);

        ObjectAnimator.ofFloat(newTile, View.SCALE_X, .50f).setDuration(0).start();
        ObjectAnimator.ofFloat(newTile, View.SCALE_Y, .50f).setDuration(0).start();

        // Make the tile visible. It still cannot be seen because the alpha is 0
        newTile.setVisibility(View.VISIBLE);

        // Fade the tile in
        ObjectAnimator alphaAnimation = ObjectAnimator.ofFloat(newTile, View.ALPHA, 1)
                .setDuration(tileSlideSpeed);

        // Keep track of the active animations in case the activity is stopped
        alphaAnimation.addListener(new Animator.AnimatorListener(){
            @Override
            public void onAnimationEnd(Animator animation) {
                activeAnimations.clear();
            }
            @Override
            public void onAnimationStart(Animator animation) { }
            @Override
            public void onAnimationCancel(Animator animation) {
                activeAnimations.clear();
            }
            @Override
            public void onAnimationRepeat(Animator animation) { }
        });

        ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(newTile, View.SCALE_X, 1.00f).setDuration(tileSlideSpeed);
        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(newTile, View.SCALE_Y, 1.00f).setDuration(tileSlideSpeed);

        activeAnimations.add(alphaAnimation);
        activeAnimations.add(scaleUpX);
        activeAnimations.add(scaleUpY);

        AnimatorSet addTile = new AnimatorSet();
        addTile.play(alphaAnimation).with(scaleUpX).with(scaleUpY);

        addTile.start();
        alphaAnimation.start();
    }

    /**
     * Remove a tile from the board when it is tapped
     */
    private void removeTile() {
        animationInProgress = true;

        for(int row = 0; row < game.getGrid().getNumRows(); row++) {
            for(int col = 0; col < game.getGrid().getNumCols(); col++) {
                ImageView tile = (ImageView) getView().findViewById(row * 100 + col);

                if(tile.getVisibility() == View.VISIBLE &&
                        (game.getGrid().get(new Location(row, col)) > 0)) {

                    // Start shaking if the value is > 0
                    Animation shake = AnimationUtils.loadAnimation(getActivity(), R.anim.shake);
                    tile.startAnimation(shake);

                    tile.setOnClickListener(new View.OnClickListener(){

                        @Override
                        public void onClick(View view) {
                            clearTileListeners();
                            // Create and start an animation of the tile fading away
                            (ObjectAnimator.ofFloat(view, View.ALPHA, 0)
                                    .setDuration(tileSlideSpeed)).start();
                            game.removeTile(new Location(view.getId() / 100, view.getId() % 100));
                            game.decrementPowerupsRemaining();
                            if(game.getUseItemInventory())
                                gameStats.decrementPowerupInventory();
                            setPowerupButtonEnabled(game.getPowerupsRemaining() != 0);
                            updateTextviews();

                            sendAnalyticsEvent("Game Fragment", "Item Used", "Remove Tile");
                        }
                    });
                }
            }
        }

        View gameActivity = getView();
        gameActivity.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                clearTileListeners();
                setPowerupButtonEnabled(true);
                return true;
            }
        });
    }

    private void clearTileListeners() {
        animationInProgress = false;
        getView().setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return onTouchEvent(event);
            }
        });

        for(int row = 0; row < game.getGrid().getNumRows(); row++) {
            for(int col = 0; col < game.getGrid().getNumCols(); col++) {
                ImageView tile = (ImageView) getView().findViewById(getTileIdByLocation(new Location(row, col)));
                tile.setOnClickListener(null);
                tile.clearAnimation();
            }
        }
    }

    /**
     * Calculate the distances that tiles should move when the game is swiped
     */
    private void calculateDistances() {
        GridLayout grid = (GridLayout) getView().findViewById(R.id.grid_layout);
        verticalTileDistance = grid.getHeight() / game.getGrid().getNumRows();
        horizontalTileDistance = grid.getWidth() / game.getGrid().getNumCols();
    }

    /**
     * Give a random bonus or attack to the player
     * 50% chance of an attack: ice attack, ghost attack, XTile attack, shuffle attack
     * 50% chance of bonus: +1 undo, +1 powerup
     */
    private void addRandomBonus() {
        String item;

        double rand = Math.random();
        if(rand < .5 && game.getAttackDuration() <= 0) {
            if(rand < .125)
                ice();
            else
            if(rand < .25)
                ghostAttack();
            else
            if(rand < .375)
                XTileAttack();
            else {
                shuffleGame();
                Toast.makeText(getActivity(), getString(R.string.random_shuffle), Toast.LENGTH_SHORT).show();
            }

            updateTextviews();
        }
        else {
            if(rand < 0.75) {
                game.incrementUndosRemaining();
                setUndoButtonEnabled(true);
                item = getString(R.string.undo);
            }
            else {
                game.incrementPowerupsRemaining();
                setPowerupButtonEnabled(true);
                item = getString(R.string.powerup);
            }

            Toast.makeText(getActivity(), "Bonus! +1 " + item, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Remove all 2's and 4's from the game with a fade out animation
     */
    private void removeLowTiles() {

        animationInProgress = true;

        // Save the game history before each move
        game.saveGameInHistory();

        // The grid where all of the tiles are in
        Grid gameBoard = game.getGrid();

        // Get a list of all tiles
        List<Location> tiles = game.getGrid().getFilledLocations();

        // An list of the fade animations to play
        ArrayList<ObjectAnimator> alphaAnimations = new ArrayList<ObjectAnimator>();

        // Loop through each tile
        for(Location tile : tiles) {
            if(gameBoard.get(tile) == 2 || gameBoard.get(tile) == 4) {

                ImageView toRemove = findTileByLocation(tile);

                // Set the tag to the new value
                toRemove.setTag(0);

                // Create a new animation of the tile fading away and
                // add it to the list
                alphaAnimations.add(ObjectAnimator.ofFloat(toRemove, View.ALPHA, 0)
                        .setDuration(tileSlideSpeed));
            }
        }

        if(alphaAnimations.isEmpty()) {
            animationInProgress = false;
            return;
        }

        // Assume that all animations finish at the same time
        alphaAnimations.get(0).addListener(new Animator.AnimatorListener(){

            @Override
            public void onAnimationEnd(Animator animation) {
                game.removeLowTiles();
                game.newTurn();
                gameStats.incrementTotalMoves(1);
                if(gameStats.getTotalMoves() == 2048 && getApiClient().isConnected()) {
                    Games.Achievements.unlock(getApiClient(), getString(R.string.achievement_long_time_player));
                }
                updateGame();
                activeAnimations.clear();
                animationInProgress = false;

                sendAnalyticsEvent("Game Fragment", "Item Used", "Remove Low Tiles");
            }

            @Override
            public void onAnimationStart(Animator animation) { }
            @Override
            public void onAnimationCancel(Animator animation) {
                activeAnimations.clear();
                animationInProgress = false;
            }
            @Override
            public void onAnimationRepeat(Animator animation) { }
        });

        // Remove all of the tiles
        for(ObjectAnimator animation: alphaAnimations) {
            activeAnimations.add(animation);
            animation.start();
        }
    }

    /**
     * Shuffles the game board and animates the grid
     * The grid layout spins 360�, the tiles are shuffled, then it spins
     * back in the opposite direction
     */
    protected void shuffleGame() {
        // Save the game history before each move
        game.saveGameInHistory();

        GridLayout gridLayout = (GridLayout) getView().findViewById(R.id.grid_layout);

        // Causes conflicts when the shuffle button is double tapped
        if(animationInProgress)
            return;

        ObjectAnimator rotateAnimation =
                ObjectAnimator.ofFloat(gridLayout, View.ROTATION, 360);
        rotateAnimation.setRepeatCount(1);
        rotateAnimation.setRepeatMode(ValueAnimator.REVERSE);

        // 300 ms should be fast enough to not notice the tiles changing
        rotateAnimation.setDuration(SHUFFLE_SPEED);

        rotateAnimation.addListener(new Animator.AnimatorListener(){
            @Override
            public void onAnimationStart(Animator animation) {
                animationInProgress = true;
            }
            @Override
            public void onAnimationEnd(Animator animation) {
                activeAnimations.clear();
                animationInProgress = false;
            }
            @Override
            public void onAnimationCancel(Animator animation) {
                activeAnimations.clear();
                animationInProgress = false;
            }
            @Override
            public void onAnimationRepeat(Animator animation) {
                game.shuffle();
                gameStats.incrementShufflesUsed(1);
                gameStats.incrementTotalMoves(1);
                if(gameStats.getTotalMoves() == 2048 && getApiClient().isConnected()) {
                    Games.Achievements.unlock(getApiClient(), getString(R.string.achievement_long_time_player));
                }
                updateGame();
            }
        });

        activeAnimations.add(rotateAnimation);
        rotateAnimation.start();

        sendAnalyticsEvent("Game Fragment", "Item Used", "Shuffle");
    }

    /**
     * Freezes the game (can not move in a direction for a random amount of turns)
     */
    protected void ice() {
        // This attack cannot be stacked
        if(game.getAttackDuration() <= 0)
            game.ice();
    }

    /**
     * Temporarily adds an X tile to the game for a limited amount of time
     */
    protected void XTileAttack() {
        // This attack cannot be stacked
        if(game.getAttackDuration() <= 0) {
            game.XTileAttack();
            XTileAttackActive = true;
            updateGrid();
        }
    }

    private void endXAttack() {
        XTileAttackActive = false;
        Location XTileLoc = game.endXTileAttack();

        if(XTileLoc != null) {
            ImageView tile = findTileByLocation(XTileLoc);

            // Create and start an animation of the tile fading away
            ObjectAnimator fade = ObjectAnimator.ofFloat(tile, View.ALPHA, 0)
                    .setDuration(tileSlideSpeed);
            fade.start();
        }
    }

    protected void ghostAttack() {
        game.ghostAttack();

        List<Location> tileLocs = game.getGrid().getFilledLocations();
        int tileValue;
        ImageView tile;

        for(Location loc : tileLocs) {
            tileValue = game.getGrid().get(loc);
            tile = findTileByLocation(loc);

            Drawable[] layers = new Drawable[2];
            // The current icon
            layers[0] = getTileIconDrawable(tileValue);
            // No icon found, default to question mark
            layers[1] = getTileIconDrawable(-10);
            TransitionDrawable transition = new TransitionDrawable(layers);
            tile.setImageDrawable(transition);
            transition.startTransition((int) tileSlideSpeed);
        }

        ghostAttackActive = true;
    }
    private void endGhostAttack() {
        ghostAttackActive = false;

        List<Location> tileLocs = game.getGrid().getFilledLocations();
        int tileValue;
        ImageView tile;

        for(Location loc : tileLocs) {
            tileValue = game.getGrid().get(loc);
            tile = findTileByLocation(loc);

            Drawable[] layers = new Drawable[2];

            // The ghost icon
            layers[0] = getTileIconDrawable(-10);
            // The tile icon
            layers[1] = getTileIconDrawable(tileValue);

            TransitionDrawable transition = new TransitionDrawable(layers);
            tile.setImageDrawable(transition);
            transition.startTransition((int) tileSlideSpeed);
        }
    }

    private void activateSpeedMode() {
        final Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(game.getGrid().getEmptyLocations().isEmpty()) {
                    timer.cancel();

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            lost();
                            activeTimers.remove(timer);
                        }
                    });
                }
                else {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            addTile();
                        }
                    });
                }}
        }, SPEED_MODE_DELAY, SPEED_MODE_DELAY);
        activeTimers.add(timer);
    }

    private void activateSurvivalMode() {
        if(game.getTimeLeft() > 0)
            secondsRemaining = (int) game.getTimeLeft();
        else
            secondsRemaining = GameModes.SURVIVAL_MODE_TIME;

        final Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                if(game.isGameLost())
                    timer.cancel();
                else
                    if(getSecondsRemaining() == 0) {
                        timer.cancel();
                        getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            lost();
                            activeTimers.remove(timer);
                        }
                    });
                    }
                else {
                    decrementTimeLeft(1);
                }

            }
        }, 1000, 1000);
        activeTimers.add(timer);

        updateTextviews();
    }

    public int getSecondsRemaining() {
        return secondsRemaining;
    }

    public int decrementTimeLeft(int seconds) {
        secondsRemaining -= seconds;
        game.setTimeLimit(secondsRemaining);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateTextviews();
            }
        });

        return secondsRemaining;
    }

    public int incrementTimeLeft(int seconds) {
        secondsRemaining += seconds;
        if(secondsRemaining > 30)
            secondsRemaining = 30;

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateTextviews();
            }
        });
        return secondsRemaining;
    }

    /**
     * Warn the user about moving in that direction
     */
    private void warnAboutMove(final int direction) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.are_you_sure));
        builder.setMessage("Moving " + Location.directionToString(direction) + " might cause you to lose");

        // Two buttons appear, yes and no
        builder.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
            // If the user clicked yes consume the powerup and move
            public void onClick(DialogInterface dialog, int id) {
                game.setGenieEnabled(false);
                game.act(direction);
                updateGame();
                if(game.getGameModeId() == GameModes.PRACTICE_MODE_ID)
                    game.setGenieEnabled(true);
            }
        });
        builder.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
            // If the user clicked no consume the powerup and don't move
            public void onClick(DialogInterface dialog, int id) {
                if(game.getGameModeId() != GameModes.PRACTICE_MODE_ID)
                    game.setGenieEnabled(false);
            }
        });

        AlertDialog dialog = builder.create();

        // You must click on one of the buttons in order to dismiss the dialog
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    /**
     * Undo the game. Currently does not have any animations because it
     * would be difficult to track every tile separately
     */
    protected void undo() {
        final ImageButton undoButton = (ImageButton) getView().findViewById(R.id.undo_button);

        if(undoButton == null) {
            game.undo();
            gameStats.incrementTotalMoves(1);
            gameStats.incrementUndosUsed(1);
            if(game.getUseItemInventory())
                gameStats.decrementUndoInventory();
            updateGame();

            Games.Events.increment(this.getApiClient(), getString(R.string.event_undos_used), 1);
            sendAnalyticsEvent("Game Fragment", "Item Used", "Undo");
            return;
        }

        if(game.getUndosRemaining() == 0)
            setUndoButtonEnabled(false);
        else {
            if(game.getTurns() > 1) {
                // Reset the rotation to the default orientation
                undoButton.setRotation(0);

                ObjectAnimator spinAnimation = ObjectAnimator.ofFloat(undoButton, View.ROTATION, -360);

                if(game.getUndosRemaining() == 1) {
                    spinAnimation.addListener(new Animator.AnimatorListener() {

                        @Override
                        public void onAnimationCancel(Animator animation) {
                            undoButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.undo_button_gray));
                        }
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            undoButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.undo_button_gray));
                        }
                        @Override
                        public void onAnimationRepeat(Animator animation) {}
                        @Override
                        public void onAnimationStart(Animator animation) {}
                    });
                }

                spinAnimation.start();

                game.undo();
                gameStats.incrementTotalMoves(1);
                gameStats.incrementUndosUsed(1);
                if(game.getUseItemInventory())
                    gameStats.decrementUndoInventory();
                updateGame();

                Games.Events.increment(this.getApiClient(), getString(R.string.event_undos_used), 1);
            }
        }
    }

    // If an ice attack is active and the user tries to move anyway screen will flash
    private void emphasizeAttack() {
        TextView activeAttack = (TextView) getView().findViewById(R.id.active_attacks_textview);

        if(activeAttack.getAnimation() == null) {
            ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(activeAttack, View.SCALE_X, 1.1f);
            ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(activeAttack, View.SCALE_Y, 1.1f);

            // The tile increases in size by a factor of 1.1 and shrinks back down. At the same time
            // it is fading to the new value.
            scaleUpX.setDuration(500);
            scaleUpY.setDuration(500);
            scaleUpX.setRepeatCount(1);
            scaleUpX.setRepeatMode(ObjectAnimator.REVERSE);
            scaleUpY.setRepeatCount(1);
            scaleUpY.setRepeatMode(ObjectAnimator.REVERSE);

            AnimatorSet scaleDown = new AnimatorSet();

            scaleDown.play(scaleUpX).with(scaleUpY);
            scaleDown.start();
        }
    }

    // TODO
    private void promptRestartGame() {
        //FrameLayout frameLayout = new FrameLayout(getActivity());
        CheckBox checkbox = new CheckBox(getActivity());
        checkbox.setText(getString(R.string.dont_ask_again));
        //checkbox.setGravity(Gravity.RIGHT);

        checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
                prefs.edit().putBoolean(getString(R.string.preference_prompt_restart), !isChecked).apply();

            }
        });

        //frameLayout.addView(checkbox);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle(getString(R.string.ask_restart_game));
        alertDialogBuilder.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                restartGame();
            }
        });
        alertDialogBuilder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        alertDialogBuilder.setView(checkbox);

        alertDialogBuilder.create().show();

    }

    /**
     * Restart the game.
     */
    private void restartGame() {
        // Spin the restart button 360 degrees counterclockwise
        ImageButton restartButton = (ImageButton) getView().findViewById(R.id.restart_button);
        restartButton.setRotation(0);
        ObjectAnimator.ofFloat(restartButton, View.ROTATION, -360).start();

        if(animationInProgress)
            clearTileListeners();

        for(Timer t : activeTimers)
                t.cancel();
        activeTimers.clear();

        // Save any new records
        gameStats.updateGameRecords(game.getGameModeId(), game);

        // Create a new game
        game = game.getOriginalGame();
        game.finishedCreatingGame();

        // Activate speed or survival mode if necessary
        if(game.getSpeedMode())
            activateSpeedMode();
        if(game.getSurvivalMode())
            activateSurvivalMode();

        // Disable the undo button and powerup button if necessary
        setUndoButtonEnabled(game.getUndosRemaining() != 0);
        setPowerupButtonEnabled(game.getPowerupsRemaining() != 0);

        gameLost = false;
        updateGame();
    }

    /**
     * Save the game and game stats to a file
     */
    private void save() {

        File currentGameFile = new File(getActivity().getFilesDir(), getString(R.string.file_current_game));
        File gameStatsFile = new File(getActivity().getFilesDir(), getString(R.string.file_game_stats));

        File currentGameScreenshotFile = new File(getActivity().getFilesDir(), "CURRENT_GAME_SCREENSHOT");

        try {
            Save.save(game, currentGameFile);
            Save.save(gameStats, gameStatsFile);

            View currentGame = getView().findViewById(R.id.grid_layout);
            if(currentGame != null) {
                Bitmap currentGameBitmap = loadBitmapFromView(currentGame, currentGame.getWidth(), currentGame.getHeight());
                Save.saveBitmap(currentGameBitmap, currentGameScreenshotFile);
            }
            //Save.save(gameStats, gameStatsFile);
        } catch (IOException e) {
            e.printStackTrace();
            // Notify the user of the error through a toast
            Toast.makeText(getActivity(), getString(R.string.error_can_not_save), Toast.LENGTH_SHORT).show();
        }
        requestBackup();
    }

    /**
     * Load the game from a file and update the game
     */
    private void load() {

        File currentGameFile = new File(getActivity().getFilesDir(), getString(R.string.file_current_game));
        File gameStatsFile = new File(getActivity().getFilesDir(), getString(R.string.file_game_stats));

        try {
            game = (Game) Save.load(currentGameFile);
            gameStats = (GameData) Save.load(gameStatsFile);

            if(game != null) {
                if(game.getUseItemInventory()) {
                    game.setUndoLimit(gameStats.getUndoInventory());
                    game.setPowerupLimit(gameStats.getPowerupInventory());
                }

                if(! (getActivity() instanceof MultiplayerActivity))
                    updateGame();
            }

        } catch (ClassNotFoundException e) {
            Log.e(LOG_TAG, "Class not found exception in load");
            game = new Game();
            gameStats = new GameData();
        } catch (IOException e) {
            game = new Game();
            gameStats = new GameData();
        }
    }

    /**
     * Updates player_imageview with the active player's google profile picture
     */
    protected void updatePlayerPic() {
        ImageView playerPicture = (ImageView) getView().findViewById(R.id.player_imageview);
        ((MultiplayerActivity) getActivity()).setImageViewBackground(playerPicture, ((MultiplayerActivity) getActivity()).getPlayer().getImage().getUrl());

    }

    /**
     * Updates opponent_imageview with the opponents google profile picture
     */
    protected void updateOpponentPic() {
        ImageView opponentPicture = (ImageView) getView().findViewById(R.id.opponent_imageview);
        ((MultiplayerActivity) getActivity()).setImageViewBackground(opponentPicture, ((MultiplayerActivity) getActivity()).getOpponentPicUrl());
    }

    /**
     * Updates player_name with the active player's first name
     */
    protected void updatePlayerName() {
        ((TextView) getView().findViewById(R.id.player_name)).setText(((MultiplayerActivity) getActivity()).getPlayerName());
    }

    /**
     * Updates opponent_name with the opponent's first name
     * If the opponent chose to hide their identity the textview does not change
     */
    protected void updateOpponentName() {
        String opponentName = ((MultiplayerActivity) getActivity()).getOpponentName();
        if(opponentName != null)
            ((TextView) getView().findViewById(R.id.opponent_name)).setText(opponentName);

    }

    protected void clearMultiplayerInventory() {
        ViewGroup bonusesLinearLayout = (ViewGroup) getView().findViewById(R.id.bonuses_linear_layout);
        bonusesLinearLayout.removeAllViews();
    }

    /**
     * Creates the coundown before the multiplayer game starts
     * @param countdownMilliseconds The number of milliseconds before a multiplayer game starts
     */
    private void setCountdown(double countdownMilliseconds) {
        TextView countdownTextView = ((TextView) getView().findViewById(R.id.countdown_textview));
        countdownTextView.setText("" + ((int) countdownMilliseconds / 1000));
        countdownTextView.setVisibility(View.VISIBLE);
    }

    /**
     * The countdown before the game statrs
     * @param milliseconds
     */
    protected void createCountdown(final double milliseconds) {
        game.setGrid(new Grid(4,4));
        updateGame();

        final Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            int times = 0;

            @Override
            public void run() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setCountdown(milliseconds - times++ * 1000);
                    }
                });
                if (times >= milliseconds / 1000) {
                    timer.cancel();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            endCountdown();
                        }
                    });
                }

            }
        }, 0, 1000);
    }

    /**
     * Stop the countdown before the multiplayer game starts and start the game
     */
    private void endCountdown() {
        getView().findViewById(R.id.countdown_textview).setVisibility(View.GONE);
        getView().findViewById(R.id.active_attacks_textview).setVisibility(View.VISIBLE);


        // Create the game timer
        ((MultiplayerActivity) getActivity()).createMultiplayerTimer(MultiplayerActivity.MULTIPLAYER_GAME_LENGTH);

        game = GameModes.practiceMode();
        updateGame();
    }

    private static Bitmap loadBitmapFromView(View v, int width, int height) {
        if(width > 0 && height > 0) {
            Bitmap b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);
            v.layout(0, 0, v.getLayoutParams().width, v.getLayoutParams().height);
            v.draw(c);
            return b;
        }
        return null;
    }

    protected void requestBackup() {
        BackupManager bm = new BackupManager(getActivity());
        bm.dataChanged();
    }

    public void requestRestore() {
        Log.d(LOG_TAG, "request restore");

        BackupManager bm = new BackupManager(getActivity());
        bm.requestRestore(
                new RestoreObserver() {
                    @Override
                    public void restoreStarting(int numPackages) {
                        Log.d(LOG_TAG, "Restore from cloud starting.");
                        Log.d(LOG_TAG, "" + gameStats.getTotalMoves());

                        super.restoreStarting(numPackages);
                    }

                    @Override
                    public void onUpdate(int nowBeingRestored, String currentPackage) {
                        Log.d(LOG_TAG, "Restoring " + currentPackage);
                        super.onUpdate(nowBeingRestored, currentPackage);
                    }

                    @Override
                    public void restoreFinished(int error) {
                        Log.d(LOG_TAG, "Restore from cloud finished.");

                        super.restoreFinished(error);
                        Log.d(LOG_TAG, "" + gameStats.getTotalMoves());

                        Log.d(LOG_TAG, "calling load");
                        load();

                    }
                });
    }

    /**
     * Unlock an achievement when a new highest tile is reached
     * @param tile The new highest tile
     */
    private void unlockAchievementNewHighestTile(int tile) {
        if(getApiClient().isConnected()) {
            switch(tile) {
                case 128:
                    Games.Achievements.unlock(getApiClient(), getString(R.string.achievement_128_tile));
                    break;
                case 256:
                    Games.Achievements.unlock(getApiClient(), getString(R.string.achievement_256_tile));
                    break;
                case 512:
                    Games.Achievements.unlock(getApiClient(), getString(R.string.achievement_512_tile));
                    break;
                case 1024:
                    Games.Achievements.unlock(getApiClient(), getString(R.string.achievement_1024_tile));
                    break;
                case 2048:
                    Games.Achievements.unlock(getApiClient(), getString(R.string.achievement_2048_tile));
                    break;
            }
        }
    }

    private void setUndoButtonEnabled(boolean enabled) {
        // Disable the undo button if there are no undos remaining
        ImageButton undoButton = (ImageButton) getView().findViewById(R.id.undo_button);
        if(undoButton != null) {
            undoButton.setEnabled(game.getUndosRemaining() != 0);
            undoButton.setBackgroundDrawable(getResources().getDrawable(
                    (enabled) ? R.drawable.undo_button : R.drawable.undo_button_gray));
        }
    }

    private void setPowerupButtonEnabled(boolean enabled) {
        ImageButton powerupButton = (ImageButton) getView().findViewById(R.id.powerup_button);
        if(powerupButton != null) {
            powerupButton.setEnabled(enabled);
            powerupButton.setBackgroundDrawable(getResources().getDrawable(
                    (enabled) ? R.drawable.powerup_button : R.drawable.powerup_button_disabled));
        }
    }

    public Game getGame() {
        return game;
    }

    public GameData getGameStats() {
        return gameStats;
    }

    public Game setGame(Game newGame) {
        Game oldGame = game;
        game = newGame;
        return oldGame;
    }


    private List<ImageView> getListOfAllTiles() {
        List<ImageView> tiles = new ArrayList<ImageView>();
        List<Location> locs = game.getGrid().toList();

        for(Location loc : locs)
            tiles.add(findTileByLocation(loc));

        return tiles;
    }

    /**
     * @param loc The location of the tile to find
     * @return The ImageView of the tile
     */
    private ImageView findTileByLocation(Location loc) {
        // It is more efficient to find a view using its parent than the entire activity
        if(gridLayout != null)
            return (ImageView) gridLayout.findViewById(getTileIdByLocation(loc));
        return (ImageView) getView().findViewById(getTileIdByLocation(loc));
    }

    /**
     * This method supports grids up to a size of 100x100 before collisions occur
     * @param loc The location of the tile to find the id for
     * @return The id of the tile
     */
    private int getTileIdByLocation(Location loc) {
        return loc.getRow() * 100 + loc.getCol();
    }

    private Location getTileLocationById(int id) {
        return new Location(id / 100 , id % 100);
    }


    /**
     * Precondition: The activity that this fragment is in extends from BaseGameActivity
     * @return the api client of the activity that it is in.
     */
    protected GoogleApiClient getApiClient() {
        if(getActivity().getClass().getSuperclass() == BaseGameActivity.class)
            return ((BaseGameActivity) getActivity()).getApiClient();

        Log.w(LOG_TAG, "GameFragment is not a member of an activity that extends BaseGameActivity");
        return null;
    }

    private void sendAnalyticsEvent(String categoryId, String actionId, String labelId) {
        // Get tracker.
        Tracker t = ((MainApplication)getActivity().getApplication()).getTracker(MainApplication.TrackerName.APP_TRACKER);
        // Build and send an Event.
        t.send(new HitBuilders.EventBuilder()
                .setCategory(categoryId)
                .setAction(actionId)
                .setLabel(labelId).build());
    }

    public boolean onTouchEvent(MotionEvent event) {
        this.mDetector.onTouchEvent(event);
        return true;
    }

    @Override
    public boolean onDown(MotionEvent event) {
        listenForSwipe = true;
        return true;
    }

    /**
     * When the screen is swiped, move the board
     */
    @Override
    public boolean onScroll(MotionEvent initialEvent, MotionEvent currentEvent,
                            float distanceX, float distanceY) {

        if(listenForSwipe && !animationInProgress) {
            float totalDistanceX = Math.abs(initialEvent.getX() - currentEvent.getX());
            float totalDistanceY = Math.abs(initialEvent.getY() - currentEvent.getY());

            if(totalDistanceX > totalDistanceY) {
                if(totalDistanceX > swipeSensitivity) {
                    if (initialEvent.getX() > currentEvent.getX())
                        act(Location.LEFT);
                    else
                        act(Location.RIGHT);
                    listenForSwipe = false;
                }
            }
            else if(totalDistanceY > swipeSensitivity) {
                if(initialEvent.getY() > currentEvent.getY())
                    act(Location.UP);
                else
                    act(Location.DOWN);
                listenForSwipe = false;
            }
        }
        return true;
    }

    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2,
                           float velocityX, float velocityY) {
        return true;
    }
    @Override
    public void onShowPress(MotionEvent event) {}
    @Override
    public void onLongPress(MotionEvent event) {}
    @Override
    public boolean onSingleTapUp(MotionEvent event) { return true; }
}