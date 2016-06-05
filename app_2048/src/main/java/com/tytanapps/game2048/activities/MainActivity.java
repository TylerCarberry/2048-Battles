package com.tytanapps.game2048.activities;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.quest.Quest;
import com.google.android.gms.games.quest.QuestUpdateListener;
import com.google.android.gms.games.quest.Quests;
import com.google.android.gms.games.request.GameRequest;
import com.google.android.gms.games.request.Requests;
import com.google.example.games.basegameutils.BaseGameActivity;
import com.tytanapps.game2048.DownloadImageTask;
import com.tytanapps.game2048.Game;
import com.tytanapps.game2048.GameData;
import com.tytanapps.game2048.GameModes;
import com.tytanapps.game2048.MainApplication;
import com.tytanapps.game2048.R;
import com.tytanapps.game2048.Save;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;


public class MainActivity extends BaseGameActivity implements QuestUpdateListener {

    private final static String LOG_TAG = MainActivity.class.getSimpleName();
    private static final String analyticsCategory = "Main Activity";

    public static final String GITHUB_URL = "https://github.com/TylerCarberry/2048-for-Android";
    public static final String APP_URL = "https://play.google.com/store/apps/details?id=com.tytanapps.game2048";

    private final static int SEND_REQUEST_CODE = 1001;
    private final static int SEND_GIFT_CODE = 1002;
    private final static int SHOW_INBOX = 1003;
    private final static int QUEST_CODE = 1004;

    // The tiles that fly across the background travel for between 3 and 6 seconds
    public final static int FLYING_TILE_SPEED = 6000;

    // Stores a cache of the scaled tiles the fly across the background
    private SparseArray<Drawable> tileIcons = new SparseArray<Drawable>();

    private static GameData gameData;

    private static boolean activityIsVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public void onStart() {
        activityIsVisible = true;

        // Give the user a bonus if a day has past since they last played
        addWelcomeBackBonus();

        // Used to debug only
        if(false) {
            try {
                incrementPowerupInventory(5);
                incrementUndoInventory(5);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        readGameData();
        updateInventoryTextView();

        animateFlyingTiles(-1, 300);

        super.onStart();
    }

    @Override
    public void onResume() {
        checkIfQuestActive();
        checkPendingPlayGifts();
        super.onResume();
    }

    @Override
    protected void onStop() {
        activityIsVisible = false;
        super.onStop();
    }

    /**
     * Switches to the game activity
     */
    private void startGameActivity() {
        startActivity(new Intent(this, GameActivity.class));
    }

    /**
     * Adds a power up or undo to the user's inventory if a day has passed since they last played
     * If the user changed the date to cheat they must wait 3 days
     */
    public void addWelcomeBackBonus() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // The number of days since the epoch
        long lastDatePlayed = prefs.getLong(getString(R.string.shared_preference_last_date_played), -1);
        long currentDate = TimeUnit.MILLISECONDS.toDays(Calendar.getInstance().getTimeInMillis());

        if (currentDate > lastDatePlayed && lastDatePlayed != -1) {
            displayWelcomeBackBonus();
        }
        else
            // The time was changed
            if (currentDate < lastDatePlayed) {
                Toast.makeText(this, getString(R.string.user_changed_date), Toast.LENGTH_LONG).show();

                // The user must wait another 3 days
                currentDate = lastDatePlayed + 3;
            }

        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(getString(R.string.shared_preference_last_date_played), currentDate);
        editor.apply();
    }

    private void displayWelcomeBackBonus() {
        // Create a new dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.welcome_back));

        try {
            if (Math.random() < 0.5) {
                incrementPowerupInventory(1);
                builder.setMessage(getString(R.string.daily_bonus_powerup));
            }
            else {
                incrementUndoInventory(1);
                builder.setMessage(getString(R.string.daily_bonus_undo));
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, getString(R.string.error_claim_bonus), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, getString(R.string.error_claim_bonus), Toast.LENGTH_LONG).show();
        }

        // Show the message to the player
        builder.create().show();
    }

    public void setQuestButtonEmphasis(boolean enabled) {
        ImageButton questsButton = (ImageButton) findViewById(R.id.quests_button);
        questsButton.setImageResource((enabled) ? R.drawable.games_quests_green : R.drawable.games_quests);
    }

    public void updateInventoryTextView() {
        if(gameData == null)
            readGameData();

        TextView undoTextView = (TextView) findViewById(R.id.undo_inventory);
        undoTextView.setText("" + gameData.getUndoInventory());

        TextView powerupTextView = (TextView) findViewById(R.id.powerup_inventory);
        powerupTextView.setText("" + gameData.getPowerupInventory());
    }

    public void animateFlyingTiles(final int amount, final int delay) {
        final Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            int times = 0;

            @Override
            public void run() {
                if (activityIsVisible && (times < amount || amount < 0)) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            animateFlyingTile();
                        }
                    });
                    times++;
                } else
                    timer.cancel();

            }
        }, delay, delay);
    }

    public void animateFlyingTile() {
        final RelativeLayout mainFragment = (RelativeLayout) findViewById(R.id.main_fragment_background);
        if(mainFragment == null)
            return;

        int randomFlyingSpeed = (int) (Math.random() * FLYING_TILE_SPEED/2 + FLYING_TILE_SPEED/2);

        final ImageView tile = new ImageView(this);

        // Random power of 2 from 2 to 1024
        int tileValue = (int) Math.pow(2, ((int) (10 * Math.random())) + 1);

        int tileSize = getResources().getDimensionPixelSize(R.dimen.main_activity_tile_size);
        tile.setImageDrawable(getTileIconDrawable(tileValue, tileSize));

        Display display = getWindowManager().getDefaultDisplay();

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

        float[] rotateAmount = {(float) (2 * (Math.random() - 0.5) * 360), (float) (2 * (Math.random() - 0.5) * 360)};
        ObjectAnimator rotateAnimation = ObjectAnimator.ofFloat(tile, View.ROTATION, rotateAmount);

        animatorX.setDuration(randomFlyingSpeed);
        animatorY.setDuration(randomFlyingSpeed);
        rotateAnimation.setDuration(randomFlyingSpeed);

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

        tile.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    int tileSize = getResources().getDimensionPixelSize(R.dimen.main_activity_tile_size);
                    Bitmap tileDrawable = BitmapFactory.decodeResource(getResources(), R.drawable.tile_2048);
                    Bitmap tileBitmap = Bitmap.createScaledBitmap(tileDrawable, tileSize, tileSize, false);
                    tile.setImageBitmap(tileBitmap);

                    if (getApiClient().isConnected()) {
                        Games.Events.increment(getApiClient(), getString(R.string.event_tap_on_flying_tile), 1);
                        Games.Achievements.increment(getApiClient(), getString(R.string.achievement_tile_tapper), 1);
                        Games.Achievements.increment(getApiClient(), getString(R.string.achievement_tile_tapper_insane), 1);
                    }
                }

                tile.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        return false;
                    }
                });

                return true;
            }
        });

        RelativeLayout.LayoutParams layoutParams=new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(startingX, startingY, 0, 0);
        tile.setLayoutParams(layoutParams);

        mainFragment.addView(tile);

        float[] scaleSize = {(float) (Math.random()/2+.5), (float) (Math.random()/2+.5)};
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(tile, View.SCALE_X, scaleSize);
        scaleX.setDuration(randomFlyingSpeed);

        ObjectAnimator scaleY = ObjectAnimator.ofFloat(tile, View.SCALE_Y, scaleSize);
        scaleY.setDuration(randomFlyingSpeed);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(animatorX).with(animatorY).with(rotateAnimation).with(scaleX).with(scaleY);
        animatorSet.start();
    }

    private Drawable getTileIconDrawable(int tileValue, int tileSize) {
        Drawable cachedDrawable = tileIcons.get(tileValue);
        if(cachedDrawable != null)
            return cachedDrawable;

        Bitmap tileDrawable = BitmapFactory.decodeResource(getResources(), getTileIconResource(tileValue));
        Bitmap tileBitmap = Bitmap.createScaledBitmap(tileDrawable, tileSize, tileSize, false);
        Drawable resultDrawable = new BitmapDrawable(getResources(), tileBitmap);

        tileIcons.put(tileValue, resultDrawable);

        return resultDrawable;
    }

    /**
     * Update the tile's icon to match its value
     * @param tileValue The numerical value of the tile
     */
    private int getTileIconResource(int tileValue) {
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
            case 8192:
                return R.drawable.tile_8192;
            case 16384:
                return R.drawable.tile_16384;
            // If the tile is so high that I did not create an image, default to infinity
            default:
                return R.drawable.tile_infinity;
        }
    }

    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.single_player_imagebutton:
                showSinglePlayerDialog();
                break;
            case R.id.multiplayer_imagebutton:
                if(getApiClient().isConnected()) {
                    Intent multiplayerIntent = new Intent(getBaseContext(), MultiplayerActivity.class);
                    multiplayerIntent.putExtra("startMultiplayer", true);
                    startActivity(multiplayerIntent);
                }
                else {
                    signIn();
                }
                break;
            case R.id.help_button:
                showHelpDialog();
                break;
            case R.id.settings_button:
                Intent showSettings = new Intent(this, SettingsActivity.class);
                startActivity(showSettings);
                break;
            case R.id.logo_imageview:
                if(view.getRotation() % 360 == 0) {
                    view.setRotation(0);
                    Animator animator = ObjectAnimator.ofFloat(view, View.ROTATION, 360);
                    animator.setDuration(1500);
                    animator.start();
                }
                break;
            case R.id.reset_game_button:
                resetGame();
                break;
            case R.id.sign_out_button:
                showSignOutDialog();
                break;
            case R.id.share_button:
                createShareIntent();
                break;
            case R.id.achievements_button:
                sendAnalyticsEvent(analyticsCategory, "Google Play Games", "Achievements Button Press");
                if(getApiClient().isConnected())
                    startActivityForResult(Games.Achievements.getAchievementsIntent(getApiClient()), 1);
                else
                    signIn();
                break;
            case R.id.quests_button:
                sendAnalyticsEvent(analyticsCategory, "Google Play Games", "Quests Button Press");
                if(getApiClient().isConnected())
                    showQuests();
                else
                    signIn();
                break;
            case R.id.leaderboards_button:
                sendAnalyticsEvent(analyticsCategory, "Google Play Games", "Leaderboards Button Press");
                if(getApiClient().isConnected())
                    startActivityForResult(Games.Leaderboards.getAllLeaderboardsIntent(getApiClient()), 0);
                else
                    signIn();
                break;
            case R.id.gifts_button:
                sendAnalyticsEvent(analyticsCategory, "Google Play Games", "Gifts Button Press");
                if(getApiClient().isConnected())
                    sendGift();
                else
                    signIn();
                break;
            case R.id.inbox_button:
                sendAnalyticsEvent(analyticsCategory, "Google Play Games", "Inbox Button Press");
                if(getApiClient().isConnected())
                    startActivityForResult(Games.Requests.getInboxIntent(getApiClient()), SHOW_INBOX);
                else
                    signIn();
                break;
        }
    }

    private void createShareIntent() {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);

        shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_intent_message) + " " + APP_URL);
        shareIntent.setType("text/plain");

        if(getApiClient().isConnected()) {
            Games.Achievements.unlock(getApiClient(), getString(R.string.achievement_brag_to_your_friends));
            Games.Events.increment(getApiClient(), getString(R.string.event_shares), 1);
        }

        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_title)));
    }

    private void openGithubLink() {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(GITHUB_URL));
        startActivity(i);

        sendAnalyticsEvent(analyticsCategory, "Help Dialog", "Github");
    }



    /**
     * Shows either the continue game or new game dialog depending if there is a saved game.
     * Choosing not to continue the game shows the new game dialog.
     */
    private void showSinglePlayerDialog() {
        AlertDialog continueGameDialog = getContinueGameDialog();
        if(continueGameDialog != null) {
            continueGameDialog.show();
        }
        else {
            getNewSinglePlayerGameDialog().show();
        }
    }

    /**
     * @return the dialog allowing the user to start a new preset or custom game
     */
    private Dialog getNewSinglePlayerGameDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.new_game);
        dialog.setTitle(getString(R.string.start_new_game));

        RelativeLayout gridLayout = (RelativeLayout) dialog.findViewById(R.id.new_game_layout);
        createNewGameListeners(gridLayout, dialog);

        return dialog;
    }

    /**
     * This method is needed instead of looping through the children because the new game
     * ViewGroup contains a LinearLayout and GridLayout each of which has the buttons
     * @param viewGroup The viewGroup that contains the new game buttons
     * @return A list containing all new game buttons
     */
    private List<Button> getListOfNewGameButtons(ViewGroup viewGroup) {
        List<Button> buttons = new ArrayList<Button>();

        for(int i = 0; i < viewGroup.getChildCount(); i++) {
            View view = viewGroup.getChildAt(i);
            if(view instanceof Button) {
                buttons.add((Button)view);
            }
            else if(view instanceof ViewGroup)
                buttons.addAll(getListOfNewGameButtons((ViewGroup) view));
        }

        return buttons;
    }

    /**
     * Add listeners to all of the new game buttons
     * @param viewGroup The root ViewGroup containing the buttons
     * @param dialog The dialog that the buttons are shown in. This allows the dialog to close
     *               when starting a new game.
     */
    private void createNewGameListeners(ViewGroup viewGroup, Dialog dialog) {
        for(Button button : getListOfNewGameButtons(viewGroup)) {
            button.setOnClickListener(getOnClickListener(button, dialog));
        }
    }

    /**
     * Set the text of all buttons back to the mode name
     * @param viewGroup The root ViewGroup containing the new game buttons
     */
    private void clearGameDescriptions(ViewGroup viewGroup) {
        for(Button button : getListOfNewGameButtons(viewGroup)) {
            button.setText(GameModes.getGameTitleById(getGameModeIdFromButton(button)));

            // If the button has a tag of 1 the description is being shown.
            // If 0, the mode name.
            button.setTag(0);

            button.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.new_game_text_size));
        }
    }

    private View.OnClickListener getOnClickListener(Button newGameButton, final Dialog dialog) {
        final int gameModeId = getGameModeIdFromButton(newGameButton);

        // Custom Game
        if(newGameButton.getId() == R.id.custom_button) {
            return new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Button button = (Button) view;
                    if(view.getTag() != null && view.getTag().toString().equals("1")) {
                        startActivity(new Intent(getApplicationContext(), CustomGameActivity.class));
                        dialog.dismiss();
                    }
                    else {
                        ViewGroup viewGroup = (ViewGroup) button.getParent();
                        if(viewGroup.getChildCount() <= 3)
                            viewGroup = (ViewGroup) viewGroup.getParent();
                        clearGameDescriptions(viewGroup);

                        int width = view.getWidth();
                        int height = button.getHeight();
                        button.setTextSize(getResources().getDimension(R.dimen.new_game_text_size_small));
                        button.setText(GameModes.getGameDescById(gameModeId));
                        button.setTag(1);
                        button.setHeight(height);
                        button.setWidth(width);
                    }
                }
            };
        }

        // Preset game mode
        return new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Button button = (Button) view;
                    if(view.getTag() != null && view.getTag().toString().equals("1")) {
                        Game game = GameModes.newGameFromId(gameModeId);
                        game.setGameModeId(gameModeId);

                        File currentGameFile = new File(getFilesDir(), getString(R.string.file_current_game));
                        try {
                            Save.save(game, currentGameFile);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        // Switch to the game activity
                        startGameActivity();
                        dialog.dismiss();
                    }
                    else {
                        ViewGroup viewGroup = (ViewGroup) button.getParent();
                        if(viewGroup.getChildCount() <= 3)
                            viewGroup = (ViewGroup) viewGroup.getParent();
                        clearGameDescriptions(viewGroup);

                        int width = view.getWidth();
                        int height = button.getHeight();
                        button.setTextSize(getResources().getDimension(R.dimen.new_game_text_size_small));
                        button.setText(GameModes.getGameDescById(gameModeId));
                        button.setTag(1);
                        button.setHeight(height);
                        button.setWidth(width);
                    }
                }
            };
    }

    private int getGameModeIdFromButton(Button newGameButton) {
        int gameMode;
        switch(newGameButton.getId()) {
            case R.id.classic_button:
                gameMode = GameModes.NORMAL_MODE_ID;
                break;
            case R.id.practice_button:
                gameMode = GameModes.PRACTICE_MODE_ID;
                break;
            case R.id.arcade_button:
                gameMode = GameModes.ARCADE_MODE_ID;
                break;
            case R.id.xmode_button:
                gameMode = GameModes.X_MODE_ID;
                break;
            case R.id.corner_button:
                gameMode = GameModes.CORNER_MODE_ID;
                break;
            case R.id.survival_button:
                gameMode = GameModes.SURVIVAL_MODE_ID;
                break;
            case R.id.rush_button:
                gameMode = GameModes.RUSH_MODE_ID;
                break;
            case R.id.ghost_button:
                gameMode = GameModes.GHOST_MODE_ID;
                break;
            case R.id.speed_button:
                gameMode = GameModes.SPEED_MODE_ID;
                break;
            case R.id.crazy_button:
                gameMode = GameModes.CRAZY_MODE_ID;
                break;
            case R.id.custom_button:
                gameMode = GameModes.CUSTOM_MODE_ID;
                break;
            default:
                gameMode = GameModes.NORMAL_MODE_ID;
        }
        return gameMode;
    }

    private View getCustomGameView() {
        Button customGameButton = new Button(this);
        customGameButton.setBackgroundResource(R.drawable.tile_game_mode);
        customGameButton.setText(getString(R.string.mode_custom));
        customGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), CustomGameActivity.class));
            }
        });
        return customGameButton;
    }

    /**
     * Adds the continue game listview to the screen
     * Contains the game mode and screenshot of the game
     */
    private AlertDialog getContinueGameDialog() {
        File savedGameFile = new File(getFilesDir(), getString(R.string.file_current_game));
        Game savedGame;
        try {
            savedGame = (Game) Save.load(savedGameFile);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        if(savedGame == null)
            return null;

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle(getString(R.string.title_continue_game));

        dialogBuilder.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                sendAnalyticsEvent(analyticsCategory, "Game", "Continue");
                startGameActivity();
            }
        });

        dialogBuilder.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                getNewSinglePlayerGameDialog().show();
            }
        });

        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setBackgroundColor(getResources().getColor(R.color.SecondaryBackground));

        Bitmap savedGameBitmap = getSavedGameBitmap();
        if(savedGameBitmap != null) {
            ImageView savedGameImageView = new ImageView(this);
            savedGameImageView.setImageBitmap(savedGameBitmap);
            dialogLayout.addView(savedGameImageView);
        }

        dialogBuilder.setView(dialogLayout);


        return dialogBuilder.create();
    }

    private Bitmap getSavedGameBitmap() {
        File savedGameBitmapFile = new File(getFilesDir(), getString(R.string.file_screenshot));
        return Save.loadBitmap(savedGameBitmapFile);
    }

    /**
     * Display all quests
     */
    protected void showQuests() {
        // In the developer tutorial they use Quests.SELECT_ALL_QUESTS but that is not valid for me.
        // That may require an update but for now selecting all possibilities works the same way
        int[] questParams = new int[8];
        questParams[0] = Games.Quests.SELECT_ACCEPTED;
        questParams[1] = Games.Quests.SELECT_OPEN;
        questParams[2] = Games.Quests.SELECT_ENDING_SOON;
        questParams[3] = Games.Quests.SELECT_UPCOMING;
        questParams[4] = Games.Quests.SELECT_COMPLETED;
        questParams[5] = Games.Quests.SELECT_COMPLETED_UNCLAIMED;
        questParams[6] = Games.Quests.SELECT_FAILED;
        questParams[7] = Games.Quests.SELECT_EXPIRED;

        Intent questsIntent = Games.Quests.getQuestsIntent(getApiClient(), questParams);
        startActivityForResult(questsIntent, QUEST_CODE);
    }

    protected void showHelpDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setTitle(getString(R.string.how_to_play));
        dialog.setContentView(R.layout.how_to_play_dialog);

        // VideoView with game animation how to play
        VideoView videoview = (VideoView) dialog.findViewById(R.id.how_to_play_videoview);

        Uri uri = Uri.parse("android.resource://"+getPackageName()+"/"+R.raw.how_to_play_2048);
        videoview.setVideoURI(uri);
        videoview.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setLooping(true);
            }
        });
        videoview.setZOrderOnTop(true);
        videoview.start();

        View.OnClickListener openGithubListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGithubLink();
            }
        };
        dialog.findViewById(R.id.open_source).setOnClickListener(openGithubListener);
        dialog.findViewById(R.id.creator_name).setOnClickListener(openGithubListener);

        dialog.show();

        sendAnalyticsEvent(analyticsCategory, "Button Press", "Help");
    }

    private void sendAnalyticsEvent(String categoryId, String actionId, String labelId) {
        // Get tracker.
        Tracker t = ((MainApplication)getApplication()).getTracker(MainApplication.TrackerName.APP_TRACKER);
        // Build and send an Event.
        t.send(new HitBuilders.EventBuilder()
                .setCategory(categoryId)
                .setAction(actionId)
                .setLabel(labelId).build());
    }

    /**
     * Show a dialog allowing the user to switch to CustomIconActivity
     * Not currently used
     */
    protected void showThemesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.themes));

        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        //Button defaultTheme = new Button(this);
        //defaultTheme.setText("Default");

        Button customTheme = new Button(this);
        customTheme.setText(getString(R.string.theme_custom));
        customTheme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), CustomIconActivity.class));
            }
        });

        //linearLayout.addView(defaultTheme);
        linearLayout.addView(customTheme);

        builder.setView(linearLayout);
        builder.create().show();
    }

    protected void sendGift() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.prompt_choose_gift)).setItems(R.array.gifts, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent;
                // The 'which' argument contains the index position
                // of the selected item
                switch (which) {
                    // Powerup
                    case 0:
                        intent = Games.Requests.getSendIntent(getApiClient(), GameRequest.TYPE_GIFT,
                                "p".getBytes(), Requests.REQUEST_DEFAULT_LIFETIME_DAYS, BitmapFactory.decodeResource(getResources(),
                                        R.drawable.powerup_button), getString(R.string.powerup));
                        startActivityForResult(intent, SEND_GIFT_CODE);
                        break;
                    case 1:
                        intent = Games.Requests.getSendIntent(getApiClient(), GameRequest.TYPE_GIFT,
                                "u".getBytes(), Requests.REQUEST_DEFAULT_LIFETIME_DAYS, BitmapFactory.decodeResource(getResources(),
                                        R.drawable.undo_button), getString(R.string.undo));
                        startActivityForResult(intent, SEND_GIFT_CODE);
                        break;
                }
            }
        });
        builder.create().show();
    }

    protected void checkIfQuestActive() {
        PendingResult<Quests.LoadQuestsResult> s = Games.Quests.load(getApiClient(),
                new int[]{Games.Quests.SELECT_OPEN, Quests.SELECT_ACCEPTED, Quests.SELECT_COMPLETED_UNCLAIMED},
                Quests.SORT_ORDER_ENDING_SOON_FIRST, false);

        s.setResultCallback(new ResultCallback<Quests.LoadQuestsResult>() {
            @Override
            public void onResult(Quests.LoadQuestsResult loadQuestsResult) {
                setQuestButtonEmphasis(loadQuestsResult.getQuests().getCount() > 0);
            }
        });
    }

    protected void checkPendingPlayGifts() {
        PendingResult<Requests.LoadRequestsResult> pendingGifts = Games.Requests.loadRequests(getApiClient(), Requests.REQUEST_DIRECTION_INBOUND,
                GameRequest.TYPE_GIFT, Requests.SORT_ORDER_EXPIRING_SOON_FIRST);
        pendingGifts.setResultCallback(new ResultCallback<Requests.LoadRequestsResult>() {
            @Override
            public void onResult(Requests.LoadRequestsResult loadRequestsResult) {
                final ImageButton inboxButton = (ImageButton) findViewById(R.id.inbox_button);

                if (loadRequestsResult.getRequests(GameRequest.TYPE_GIFT).getCount() > 0) {
                    if(inboxButton.getVisibility() == View.VISIBLE)
                        return;

                    inboxButton.setVisibility(View.VISIBLE);

                    inboxButton.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View view, MotionEvent event) {
                            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                                inboxButton.setImageResource(R.drawable.inbox_button_pressed);
                            }
                            else if (event.getAction() == MotionEvent.ACTION_UP) {
                                inboxButton.setImageResource(R.drawable.inbox_button);

                                if(event.getX() > 0 && event.getX() < view.getWidth() &&
                                        event.getY() > 0 && event.getY() < view.getHeight())
                                    onClick(view);
                            }
                            return true;
                        }
                    });

                    ObjectAnimator scaleX = ObjectAnimator.ofFloat(inboxButton, View.SCALE_X, 0.90f);
                    ObjectAnimator scaleY = ObjectAnimator.ofFloat(inboxButton, View.SCALE_Y, 0.90f);

                    scaleX.setDuration(1000);
                    scaleY.setDuration(1000);
                    scaleX.setRepeatCount(ObjectAnimator.INFINITE);
                    scaleX.setRepeatMode(ObjectAnimator.REVERSE);
                    scaleY.setRepeatCount(ObjectAnimator.INFINITE);
                    scaleY.setRepeatMode(ObjectAnimator.REVERSE);

                    AnimatorSet scaleDown = new AnimatorSet();
                    scaleDown.play(scaleX).with(scaleY);
                    scaleDown.start();
                }
                else
                    inboxButton.setVisibility(View.GONE);
            }
        });
    }

    private void readGameData() {
        File gameDataFile = new File(getFilesDir(), getString(R.string.file_game_stats));
        gameData = new GameData();
        try {
            gameData = (GameData) Save.load(gameDataFile);
        } catch (IOException e) {

            try {
                Save.save(new GameData(), gameDataFile);
            } catch (IOException e1) {
                Toast.makeText(this, getString(R.string.error_can_not_save), Toast.LENGTH_LONG).show();
            }

            //e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void saveGameData() {
        File gameDataFile = new File(getFilesDir(), getString(R.string.file_game_stats));
        try {
            Save.save(gameData, gameDataFile);
        } catch (IOException e) {
            Toast.makeText(this, getString(R.string.error_can_not_save), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    @Override
    public void onQuestCompleted(Quest quest) {

        // Claim the quest reward.
        Games.Quests.claim(this.getApiClient(), quest.getQuestId(),
                quest.getCurrentMilestone().getMilestoneId());

        // Process the RewardData to provision a specific reward.
        try {
            // The reward will be in the form [integer][character]
            // The integer is the number of items gained
            // The character is either u or p for undos or powerups
            String rewardRaw = new
                    String(quest.getCurrentMilestone().getCompletionRewardData(), "UTF-8");

            int rewardAmount = Character.getNumericValue(rewardRaw.charAt(0));

            String message = "";
            if(rewardRaw.charAt(1) == 'p') {
                message =  String.format(getString(R.string.quest_reward_powerups), rewardAmount);
                incrementPowerupInventory(rewardAmount);
            }
            else {
                if(rewardRaw.charAt(1) == 'u') {
                    message =  String.format(getString(R.string.quest_reward_undos), rewardAmount);
                    incrementUndoInventory(rewardAmount);
                }
            }

            final Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.quest_completed);
            dialog.setTitle(getString(R.string.quest_completed));

            ((TextView)dialog.findViewById(R.id.quest_name_textview)).setText(quest.getName());
            ((TextView)dialog.findViewById(R.id.quest_reward_textview)).setText(message);

            ImageView questBanner = (ImageView) dialog.findViewById(R.id.quest_banner);
            questBanner.setTag(quest.getBannerImageUrl());

            DownloadImageTask downloadImageTask =  new DownloadImageTask(){
                @Override
                protected void onPostExecute(Bitmap result) {
                    super.onPostExecute(result);
                    dialog.show();
                    checkIfQuestActive();
                }
            };
            downloadImageTask.execute(questBanner);

        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error_claim_bonus), Toast.LENGTH_LONG).show();
            Log.w(LOG_TAG, e.toString());
        }
    }

    public void signIn() {
        beginUserInitiatedSignIn();
    }

    public void showSignOutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.google_play_games));

        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setGravity(Gravity.CENTER_HORIZONTAL);

        int padding = (int) getResources().getDimension(R.dimen.activity_horizontal_margin);

        // The text instructions
        TextView textView = new TextView(this);
        textView.setText(getString(R.string.signed_in_google_play_games));
        textView.setTextSize(22);
        textView.setGravity(Gravity.CENTER_HORIZONTAL);
        textView.setPadding(padding, padding, padding, padding);

        final Button signoutButton = new Button(this);
        signoutButton.setText(getString(R.string.sign_out));
        signoutButton.setPadding(padding, padding, padding, padding);
        signoutButton.setBackgroundResource(R.drawable.tile_button_background);
        signoutButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.sign_out_text_size));


        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        signoutButton.setLayoutParams(layoutParams);

        linearLayout.addView(textView);
        linearLayout.addView(signoutButton);

        builder.setView(linearLayout);
        final AlertDialog dialog = builder.create();

        signoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signOut();
                dialog.dismiss();
            }
        });

        dialog.show();

        sendAnalyticsEvent(analyticsCategory, "Google Play Games", "Sign Out");
    }

    @Override
    public void signOut() {
        super.signOut();

        // The sign in button is not a normal button, so keep it as a default view
        View signInButton = findViewById(R.id.sign_in_button);

        // If the user has switched views before the sign in completed the buttons are null
        if(signInButton != null)
            signInButton.setVisibility(View.VISIBLE);

        View signOutButton = findViewById(R.id.sign_out_button);
        if(signOutButton != null)
            signOutButton.setVisibility(View.GONE);
    }

    @Override
    public void onSignInFailed() {
        // The sign in button is not a normal button, so keep it as a default view
        View signInButton = findViewById(R.id.sign_in_button);

        // If the user has switched views before the sign in completed the buttons are null
        if(signInButton != null)
            signInButton.setVisibility(View.VISIBLE);

        View signOutButton = findViewById(R.id.sign_out_button);
        if(signOutButton != null)
            signOutButton.setVisibility(View.GONE);
    }

    @Override
    public void onSignInSucceeded() {
        // The sign in button is not a normal button, so keep it as a default view
        View signInButton = findViewById(R.id.sign_in_button);

        // If the user has switched views before the sign in completed the buttons are null
        if(signInButton != null)
            signInButton.setVisibility(View.GONE);


        View signOutButton = findViewById(R.id.sign_out_button);
        if(signOutButton != null)
            signOutButton.setVisibility(View.VISIBLE);

        // Start the Quest listener.
        Games.Quests.registerQuestUpdateListener(this.getApiClient(), this);
    }

    private void handleInboxResult(ArrayList<GameRequest> gameRequests) {
        for(GameRequest request : gameRequests) {

            if(getApiClient().isConnected())
                Games.Requests.acceptRequest(getApiClient(), request.getRequestId());
            else
                break;

            String senderName = request.getSender().getDisplayName();
            String message;

            if(new String(request.getData()).equals("p")) {
                message = String.format(getString(R.string.powerup_gift_received), senderName);
                try {
                    incrementPowerupInventory(1);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
            else {

                message = String.format(getString(R.string.undo_gift_received), senderName);
                try {
                    incrementUndoInventory(1);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }

    private void incrementPowerupInventory(int amount) throws IOException, ClassNotFoundException {
        if(gameData == null)
            readGameData();
        gameData.incrementPowerupInventory(amount);
        saveGameData();
        updateInventoryTextView();
    }

    private void incrementUndoInventory(int amount) throws IOException, ClassNotFoundException {
        if(gameData == null)
            readGameData();
        gameData.incrementUndoInventory(amount);
        saveGameData();
        updateInventoryTextView();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case SEND_REQUEST_CODE:
                if (resultCode == GamesActivityResultCodes.RESULT_SEND_REQUEST_FAILED) {
                    Toast.makeText(this, R.string.error_send_request, Toast.LENGTH_LONG).show();
                }
                break;
            case SEND_GIFT_CODE:
                if (resultCode == GamesActivityResultCodes.RESULT_SEND_REQUEST_FAILED) {
                    Toast.makeText(this, getString(R.string.error_send_gift), Toast.LENGTH_LONG).show();
                }
                break;
            case SHOW_INBOX:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    handleInboxResult(Games.Requests
                            .getGameRequestsFromInboxResponse(data));
                } else {
                    // handle failure to process inbox result
                    if(resultCode != Activity.RESULT_CANCELED)
                        Toast.makeText(this, getString(R.string.error_claim_gift), Toast.LENGTH_LONG).show();
                }
                break;
            case QUEST_CODE:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    Quest quest = data.getParcelableExtra(Quests.EXTRA_QUEST);

                    if(quest.getState() == Quest.STATE_COMPLETED)
                        onQuestCompleted(quest);
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // This snippet hides the system bars.
    private void hideSystemUI() {
        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    // This snippet shows the system bars. It does this by removing all the flags
// except for the ones that make the content appear under the system bars.
    private void showSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    /**
     * Delete the current game file and overall game statistics file
     */
    private void resetGame() {
        File currentGameFile = new File(getFilesDir(), getString(R.string.file_current_game));
        currentGameFile.delete();

        gameData = new GameData();
        gameData.incrementUndoInventory(5);
        gameData.incrementPowerupInventory(3);

        saveGameData();
    }


}
