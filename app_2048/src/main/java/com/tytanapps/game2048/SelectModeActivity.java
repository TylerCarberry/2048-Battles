package com.tytanapps.game2048;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.quest.Quest;
import com.google.android.gms.games.quest.QuestUpdateListener;
import com.google.android.gms.games.request.GameRequest;
import com.google.android.gms.games.request.Requests;
import com.google.example.games.basegameutils.BaseGameActivity;
import com.tytanapps.game2048.MainApplication.TrackerName;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class SelectModeActivity extends BaseGameActivity implements View.OnClickListener, QuestUpdateListener
{
	private final static String LOG_TAG = SelectModeActivity.class.getSimpleName();

    private final static int SEND_REQUEST_CODE = 1001;
    private final static int SEND_GIFT_CODE = 1002;
    private final static int SHOW_INBOX = 1003;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_select_mode);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // The number of days since the epoch
        long lastDatePlayed = prefs.getLong("lastDatePlayed", -1);
        long currentDate = TimeUnit.MILLISECONDS.toDays(Calendar.getInstance().getTimeInMillis());

        //Toast.makeText(this, "last date played " +lastDatePlayed, Toast.LENGTH_LONG).show();
        //Toast.makeText(this, "current date " +currentDate, Toast.LENGTH_LONG).show();

        if (currentDate > lastDatePlayed) {
            addWelcomeBackBonus();
        }
        else
            // The time was changed
            if (currentDate < lastDatePlayed) {
                Toast.makeText(this, "You changed the date", Toast.LENGTH_LONG).show();

                // The user must wait another 3 days
                currentDate = lastDatePlayed + 3;
            }

        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("lastDatePlayed", currentDate);
        editor.commit();

        getApiClient().registerConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
            @Override
            public void onConnected(Bundle bundle) {
                ArrayList <GameRequest> gameRequests
                        = Games.Requests.getGameRequestsFromBundle(bundle);
                handleInboxResult(gameRequests);
            }

            @Override
            public void onConnectionSuspended(int i) {

            }
        });

	    // Get a Tracker (should auto-report)
	    ((MainApplication) getApplication()).getTracker(MainApplication.TrackerName.APP_TRACKER);
	    // Get tracker.
	    Tracker t = ((MainApplication) getApplication()).getTracker(
	    		TrackerName.APP_TRACKER);
	    // Set screen name.
	    t.setScreenName("Main Activity");
	    // Send a screen view.
	    t.send(new HitBuilders.AppViewBuilder().build());
	}
	
	@Override
	protected void onStart() {
		// Get an Analytics tracker to report app starts & uncaught exceptions etc.
		GoogleAnalytics.getInstance(this).reportActivityStart(this);

        ((LinearLayout) findViewById(R.id.modeLinearLayout)).removeAllViewsInLayout();

        if(isSavedGame())
            addSavedGameView();

        addInventoryView();
        //addMultiplayerGameView();
        createListView();
		super.onStart();
	}
	
	@Override
	protected void onStop() {
		// Stop the analytics tracking
		GoogleAnalytics.getInstance(this).reportActivityStop(this);
		super.onStop();
	}
	
	@Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if(hasFocus) {
            HorizontalScrollView scrollView = (HorizontalScrollView) findViewById(R.id.modeScrollView);
            if(scrollView.getScrollX() == 0) {
                int width = (int) getResources().getDimension(R.dimen.game_mode_item_width);
                scrollView.smoothScrollTo(width / 4, 0);
            }
        }
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
    /**
     *  Action bar item clicks are handled here
     */
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();

		if (id == R.id.action_stats) {
			Intent showInfo = new Intent(this, StatsActivity.class);
			startActivity(showInfo);
			return true;
		}

        if (id == R.id.action_set_icons) {
            startActivity(new Intent(this, CustomIconActivity.class));
            return true;
        }

        if (id == R.id.action_reset_game) {
            resetGame();
            return true;
        }

        if (id == R.id.action_about) {
            Toast.makeText(this, "Made By Tyler Carberry", Toast.LENGTH_SHORT).show();
            return true;
        }

        if (id == R.id.action_settings) {
            Intent showSettings = new Intent(this, SettingsActivity.class);
            startActivity(showSettings);
            return true;
        }
		
		return super.onOptionsItemSelected(item);
	}

    /**
     * Adds the continue game listview to the screen
     * Contains the game mode and screenshot of the game
     */
    private void addSavedGameView() {

        File savedGameFile = new File(getFilesDir(), getString(R.string.file_current_game));
        Game savedGame;
        try {
            savedGame = (Game) Save.load(savedGameFile);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return;
        }

        if(savedGame == null || savedGame.getGameModeId() == GameModes.MULTIPLAYER_MODE_ID)
            return;

        int width = (int) getResources().getDimension(R.dimen.game_mode_item_width);

        LinearLayout listOfModes = (LinearLayout) findViewById(R.id.modeLinearLayout);

        File savedGameBitmapFile = new File(getFilesDir(), "CURRENT_GAME_SCREENSHOT");
        Bitmap savedGameBitmap = Save.loadBitmap(savedGameBitmapFile);

        // The layout the contains all info for that mode
        LinearLayout modeDetailLayout = new LinearLayout(this);
        modeDetailLayout.setPadding((int) getResources().getDimension(R.dimen.activity_horizontal_margin),
                0, (int) getResources().getDimension(R.dimen.activity_horizontal_margin), 0);

        modeDetailLayout.setOrientation(LinearLayout.VERTICAL);

        modeDetailLayout.setLayoutParams(new LinearLayout.LayoutParams(
                        width / 2, LayoutParams.WRAP_CONTENT)
        );
        modeDetailLayout.setGravity(Gravity.CENTER_HORIZONTAL);

        // The mode name
        TextView modeName = new TextView(this);
        modeName.setText(getString(GameModes.getGameTitleById((savedGame.getGameModeId()))));
        modeName.setTextSize(20);
        modeName.setTypeface(null, Typeface.BOLD);
        modeName.setLayoutParams(new LinearLayout.LayoutParams(
                        LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        );
        modeName.setPadding(0, (int) getResources().getDimension(R.dimen.activity_vertical_margin), 0, 0);
        modeName.setGravity(Gravity.CENTER_HORIZONTAL);


        ImageView currentGameImageView = new ImageView(this);
        currentGameImageView.setImageBitmap(savedGameBitmap);

        // The button used to start the game
        Button startGameButton = new Button(this);
        startGameButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.continue_game_button));
        startGameButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT));

        startGameButton.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    v.setBackgroundDrawable(getResources().getDrawable(R.drawable.continue_game_button_pressed));
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    v.setBackgroundDrawable(getResources().getDrawable(R.drawable.continue_game_button));
                    startGameActivity();
                }
                return true;
            }
        });

        // Add each item of the mode to the layout
        modeDetailLayout.addView(startGameButton);
        modeDetailLayout.addView(modeName);
        modeDetailLayout.addView(currentGameImageView);

        modeDetailLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP)
                    startGameActivity();
                return true;
            }
        });

        // Add the mode to the list
        listOfModes.addView(modeDetailLayout);
    }

    /*
    private void addMultiplayerGameView() {
        int width = (int) getResources().getDimension(R.dimen.game_mode_item_width);

        LinearLayout listOfModes = (LinearLayout) findViewById(R.id.modeLinearLayout);

        // The layout the contains all info for that mode
        LinearLayout modeDetailLayout = new LinearLayout(this);
        modeDetailLayout.setPadding((int) getResources().getDimension(R.dimen.activity_horizontal_margin),
                0, (int) getResources().getDimension(R.dimen.activity_horizontal_margin), 0);

        modeDetailLayout.setOrientation(LinearLayout.VERTICAL);
        modeDetailLayout.setLayoutParams(new LinearLayout.LayoutParams(
                        width / 2, LayoutParams.WRAP_CONTENT)
        );
        modeDetailLayout.setGravity(Gravity.CENTER_HORIZONTAL);


        ImageButton multiplayerButton = new ImageButton(this);
        multiplayerButton.setLayoutParams(new LinearLayout.LayoutParams(
                        LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        );
        multiplayerButton.setPadding(0, (int) getResources().getDimension(R.dimen.activity_vertical_margin), 0, (int) getResources().getDimension(R.dimen.activity_vertical_margin));
        multiplayerButton.setBackgroundResource(R.drawable.multiplayer_icon);
        multiplayerButton.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    Intent multiplayerIntent = new Intent(getBaseContext(), MultiplayerActivity.class);
                    multiplayerIntent.putExtra("startMultiplayer", true);
                    startActivity(multiplayerIntent);
                    return true;
                }
                return true;
            }
        });

        // Add each item of the mode to the layout
        modeDetailLayout.addView(multiplayerButton);


        modeDetailLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    Intent multiplayerIntent = new Intent(getBaseContext(), MultiplayerActivity.class);
                    multiplayerIntent.putExtra("startMultiplayer", true);
                    startActivity(multiplayerIntent);
                }
                return true;
            }
        });

        // Add the mode to the list
        listOfModes.addView(modeDetailLayout);
    }
    */

    private void addInventoryView() {
        File gameDataFile = new File(getFilesDir(), getString(R.string.file_game_stats));
        GameData gameData = new GameData();
        try {
            gameData = (GameData) Save.load(gameDataFile);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        int width = (int) getResources().getDimension(R.dimen.game_mode_item_width);

        LinearLayout listOfModes = (LinearLayout) findViewById(R.id.modeLinearLayout);

        // The layout the contains all info for that mode
        LinearLayout modeDetailLayout = new LinearLayout(this);
        modeDetailLayout.setPadding((int) getResources().getDimension(R.dimen.activity_horizontal_margin),
                0, (int) getResources().getDimension(R.dimen.activity_horizontal_margin), 0);

        modeDetailLayout.setOrientation(LinearLayout.VERTICAL);
        modeDetailLayout.setLayoutParams(new LinearLayout.LayoutParams(
                        width / 2, LayoutParams.WRAP_CONTENT));
        modeDetailLayout.setGravity(Gravity.CENTER_HORIZONTAL);

        // The mode name
        TextView modeName = new TextView(this);
        modeName.setText("Inventory");
        modeName.setTextSize(25);
        modeName.setTypeface(null, Typeface.BOLD);
        modeName.setLayoutParams(new LinearLayout.LayoutParams(
                        LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        modeName.setPadding(0, (int) getResources().getDimension(R.dimen.activity_vertical_margin), 0, 0);
        modeName.setGravity(Gravity.CENTER_HORIZONTAL);

        // Powerups
        TextView powerupTextView = new TextView(this);
        powerupTextView.setText("Powerups: " + gameData.getPowerupInventory());
        powerupTextView.setTextSize(20);
        powerupTextView.setLayoutParams(new LinearLayout.LayoutParams(
                        LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        powerupTextView.setPadding(0, (int) getResources().getDimension(R.dimen.activity_vertical_margin), 0, 0);
        powerupTextView.setGravity(Gravity.CENTER_HORIZONTAL);

        // Undos
        TextView undoTextView = new TextView(this);
        undoTextView.setText("Undos: " + gameData.getUndoInventory());
        undoTextView.setTextSize(20);
        undoTextView.setLayoutParams(new LinearLayout.LayoutParams(
                        LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        );
        undoTextView.setPadding(0, (int) getResources().getDimension(R.dimen.activity_vertical_margin),
                0, (int) getResources().getDimension(R.dimen.activity_vertical_margin));
        undoTextView.setGravity(Gravity.CENTER_HORIZONTAL);

        ImageButton sendGiftButton = new ImageButton(this);
        sendGiftButton.setBackgroundResource(R.drawable.games_gifts_green);
        sendGiftButton.setScaleType(ImageView.ScaleType.FIT_XY);

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.prompt_choose_powerup)).setItems(R.array.gifts, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent ;
                // The 'which' argument contains the index position
                // of the selected item
                switch (which) {
                    // Powerup
                    case 0:
                        intent = Games.Requests.getSendIntent(getApiClient(), GameRequest.TYPE_GIFT,
                                "p".getBytes(), Requests.REQUEST_DEFAULT_LIFETIME_DAYS, BitmapFactory.decodeResource(getResources(),
                                        R.drawable.powerup_button), "Powerup Desc");
                        startActivityForResult(intent, SEND_GIFT_CODE);
                        break;
                    case 1:
                        intent = Games.Requests.getSendIntent(getApiClient(), GameRequest.TYPE_GIFT,
                                "u".getBytes(), Requests.REQUEST_DEFAULT_LIFETIME_DAYS, BitmapFactory.decodeResource(getResources(),
                                        R.drawable.undo_button), "Undo Desc");
                        startActivityForResult(intent, SEND_GIFT_CODE);
                        break;
                }
            }
        });

        sendGiftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                builder.create().show();
            }
        });

        Button showInboxButton = new Button(this);
        showInboxButton.setText("Inbox");
        showInboxButton.setLayoutParams(new LinearLayout.LayoutParams(
                        LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        );
        showInboxButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(Games.Requests.getInboxIntent(getApiClient()), SHOW_INBOX);
            }
        });

        // Add each item of the mode to the layout
        modeDetailLayout.addView(modeName);
        modeDetailLayout.addView(powerupTextView);
        modeDetailLayout.addView(undoTextView);
        modeDetailLayout.addView(showInboxButton);
        modeDetailLayout.addView(sendGiftButton);

        modeDetailLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    Intent multiplayerIntent = new Intent(getBaseContext(), MultiplayerActivity.class);
                    multiplayerIntent.putExtra("startMultiplayer", true);
                    startActivity(multiplayerIntent);
                }
                return true;
            }
        });

        // Add the mode to the list
        listOfModes.addView(modeDetailLayout);
    }

    private void createListView() {

        int width = (int) getResources().getDimension(R.dimen.game_mode_item_width);

        LinearLayout listOfModes = (LinearLayout) findViewById(R.id.modeLinearLayout);

        GameData gameStats = new GameData();
        try {
            File file = new File(getFilesDir(), getString(R.string.file_game_stats));
            FileInputStream fi = new FileInputStream(file);
            ObjectInputStream input = new ObjectInputStream(fi);

            gameStats = (GameData) input.readObject();

            fi.close();
            input.close();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (StreamCorruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        Space marginStart = new Space(this);
        marginStart.setLayoutParams(new LinearLayout.LayoutParams(
                        (int) getResources().getDimension(R.dimen.activity_horizontal_margin), LayoutParams.MATCH_PARENT)
        );

        // Loop through every game mode and add it to the list
        for(int id : GameModes.getListOfGameModesIds()) {

            // The layout the contains all info for that mode
            LinearLayout modeDetailLayout = new LinearLayout(this);
            modeDetailLayout.setOrientation(LinearLayout.VERTICAL);

            modeDetailLayout.setLayoutParams(new LinearLayout.LayoutParams(
                            width / 2, LayoutParams.WRAP_CONTENT)
            );
            modeDetailLayout.setGravity(Gravity.CENTER_HORIZONTAL);
            modeDetailLayout.setPadding(0, 0, (int) getResources().getDimension(R.dimen.activity_horizontal_margin), 0);


            // The mode name
            TextView modeName = new TextView(this);
            modeName.setText(getString(GameModes.getGameTitleById(id)));
            modeName.setTextSize(25);
            modeName.setTypeface(null, Typeface.BOLD);
            modeName.setLayoutParams(new LinearLayout.LayoutParams(
                            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            );

            // The mode description
            TextView modeDesc = new TextView(this);
            modeDesc.setText(getString(GameModes.getGameDescById(id)));
            modeDesc.setTextSize(20);
            modeDesc.setLayoutParams(new LinearLayout.LayoutParams(
                            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            );
            modeDesc.setGravity(Gravity.CENTER_HORIZONTAL);

            // High score of that mode
            TextView highScoreTextView = new TextView(this);
            highScoreTextView.setText(String.format(getString(R.string.high_score), gameStats.getHighScore(id)));
            highScoreTextView.setLayoutParams(new LinearLayout.LayoutParams(
                            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            );

            // There is a 50 dp margin between the description and high score
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)highScoreTextView.getLayoutParams();
            params.setMargins(0, 50, 0, 0);
            highScoreTextView.setLayoutParams(params);

            // Highest tile of that mode
            TextView highTileTextView = new TextView(this);
            highTileTextView.setText(String.format(getString(R.string.highest_tile), gameStats.getHighestTile(id)));
            highTileTextView.setLayoutParams(new LinearLayout.LayoutParams(
                            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            );

            // The button used to start the game
            final int gameId = id;
            Button startGameButton = new Button(this);
            startGameButton.setText(getString(R.string.start_game));
            startGameButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT));

            startGameButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Game game = GameModes.newGameFromId(gameId);
                    game.setGameModeId(gameId);

                    File currentGameFile = new File(getFilesDir(), getString(R.string.file_current_game));
                    try {
                        Save.save(game, currentGameFile);
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }

                    // Switch to the game activity
                    startGameActivity();
                }
            });

            // Add each item of the mode to the layout
            modeDetailLayout.addView(modeName);
            modeDetailLayout.addView(modeDesc);
            modeDetailLayout.addView(highScoreTextView);
            modeDetailLayout.addView(highTileTextView);
            modeDetailLayout.addView(startGameButton);

            // Add the mode to the list
            //listOfModes.addView(marginStart);
            listOfModes.addView(modeDetailLayout);
            //listOfModes.addView(marginEnd);
        }
    }

	/**
	 * Display all quests
	 */
	protected void showQuests() {
		// In the developer tutorial they use Quests.SELECT_ALL_QUESTS
		// but that is not valid for me. That may require an update
		// but for now selecting all possibilities works the same way
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
	    
	    // 0 is an arbitrary integer
	    startActivityForResult(questsIntent, 0);
	}

    private void addWelcomeBackBonus() {
        // Create a new dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Welcome Back");

        // Add either 1 or 2 bonus items
        int bonusAmount = (int) (Math.random() * 2 + 1);

        try {
            if (Math.random() < 0.5) {
                //Toast.makeText(this, "1", Toast.LENGTH_SHORT).show();
                incrementPowerupInventory(bonusAmount);
                builder.setMessage("You Gained " + bonusAmount + " Powerup!\n" +
                        "Come back tomorrow for more.");
            }
            else {
                //Toast.makeText(this, "2", Toast.LENGTH_SHORT).show();
                incrementUndoInventory(bonusAmount);
                builder.setMessage("You Gained " + bonusAmount + " Undo!\n" +
                        "Come back tomorrow for more.");
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, "Unable to add random bonus", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Unable to access save file to add random bonus", Toast.LENGTH_LONG).show();
        }

        // Show the message to the player
        builder.create().show();
    }

    /**
     * Called when either the achievements, leaderboards, or quests buttons are pressed
     * @param view The button that was pressed
     */
	public void playGames(View view) {
		if(getApiClient().isConnected()) {
			switch (view.getId()) {
			case R.id.achievements_button:
				startActivityForResult(Games.Achievements.getAchievementsIntent(getApiClient()), 1);
				break;
			case R.id.quests_button:
				showQuests();
				break;
			case R.id.leaderboards_button:
				startActivityForResult(Games.Leaderboards.getAllLeaderboardsIntent(getApiClient()), 0);
				break;
			}
		}
		else {
			Toast.makeText(this, getString(R.string.not_signed_in_error), Toast.LENGTH_SHORT).show();
		}
	}
	
	/**
	 * Switches to the game activity
	 */
	private void startGameActivity() {
		startActivity(new Intent(this, GameActivity.class));
	}

    /**
     * @return Whether there is a saved game
     */
    private boolean isSavedGame() {
        FileInputStream fi;
        File file = new File(getFilesDir(), getString(R.string.file_current_game));

        try {
            fi = new FileInputStream(file);
            ObjectInputStream input = new ObjectInputStream(fi);

            // The value of game is not used but if it is able to be read
            // without any exceptions than it exists.
            Game game = (Game) input.readObject();

            fi.close();
            input.close();

            return true;
        }
        // If an exception is caught then the game does not exist
        catch (FileNotFoundException e) {}
        catch (StreamCorruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return false;
    }

    private void incrementPowerupInventory(int amount) throws IOException, ClassNotFoundException {
        File gameDataFile = new File(getFilesDir(), getString(R.string.file_game_stats));
        GameData gameData = (GameData) Save.load(gameDataFile);
        gameData.incrementPowerupInventory(amount);
        Save.save(gameData, gameDataFile);

        //Toast.makeText(this, "Your powerups: " + gameData.getPowerupInventory(), Toast.LENGTH_LONG).show();
        //Toast.makeText(this, "Your undos: " + gameData.getUndoInventory(), Toast.LENGTH_LONG).show();
    }

    private void incrementUndoInventory(int amount) throws IOException, ClassNotFoundException {
        File gameDataFile = new File(getFilesDir(), getString(R.string.file_game_stats));
        GameData gameData = (GameData) Save.load(gameDataFile);
        gameData.incrementUndoInventory(amount);
        Save.save(gameData, gameDataFile);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case SEND_REQUEST_CODE:
                if (resultCode == GamesActivityResultCodes.RESULT_SEND_REQUEST_FAILED) {
                    Toast.makeText(this, "FAILED TO SEND REQUEST!", Toast.LENGTH_LONG).show();
                }
                break;
            case SEND_GIFT_CODE:
                if (resultCode == GamesActivityResultCodes.RESULT_SEND_REQUEST_FAILED) {
                    Toast.makeText(this, "FAILED TO SEND GIFT!", Toast.LENGTH_LONG).show();
                }
                break;
            case SHOW_INBOX:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    handleInboxResult(Games.Requests
                            .getGameRequestsFromInboxResponse(data));
                } else {
                    // handle failure to process inbox result
                    if(resultCode != Activity.RESULT_CANCELED)
                        Toast.makeText(this, "Unable to claim reward", Toast.LENGTH_LONG).show();
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void handleInboxResult(ArrayList<GameRequest> gameRequests) {
        for(GameRequest request : gameRequests) {
            String message = request.getSender().getDisplayName() + " sent you ";
            if(new String(request.getData()).equals("p")) {
                message += "a powerup";
                try {
                    incrementPowerupInventory(1);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
            else {
                message += "an undo";
                try {
                    incrementUndoInventory(1);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }

            Games.Requests.acceptRequest(getApiClient(), request.getRequestId());

            Toast.makeText(this, message, Toast.LENGTH_LONG).show();

        }

    }


    /**
	 *  Sign in has failed. Show the user the sign-in button.
	 */
    @Override
	public void onSignInFailed() {
	    
		// The sign in button is not a normal button, so keep it as a default view
		View signInButton = findViewById(R.id.sign_in_button);
	    
		// If the user has switched views before the sign in failed then the buttons
		// are null and this will cause an error
		if(signInButton != null)
	    	signInButton.setVisibility(View.VISIBLE);
	    Button signOutButton = (Button) findViewById(R.id.sign_out_button);
	    if(signOutButton != null)
	    	signOutButton.setVisibility(View.GONE);
	    
	    Log.d(LOG_TAG, "login failed");
	}

	@Override
	public void onSignInSucceeded() {
        // The sign in button is not a normal button, so keep it as a default view
        View signInButton = findViewById(R.id.sign_in_button);

        // If the user has switched views before the sign in failed then the buttons
        // are null and this will cause an error
        if(signInButton != null)
            signInButton.setVisibility(View.GONE);
        Button signOutButton = (Button) findViewById(R.id.sign_out_button);
        if(signOutButton != null)
            signOutButton.setVisibility(View.VISIBLE);

	    // Start the Quest listener.
	    Games.Quests.registerQuestUpdateListener(this.getApiClient(), this);
	}

	@Override
	public void onClick(View view) {
	    if (view.getId() == R.id.sign_in_button) {
	        // start the asynchronous sign in flow
	        beginUserInitiatedSignIn();
	    }
	    else if (view.getId() == R.id.sign_out_button) {
	    	// sign out.
	        signOut();

	        // show sign-in button, hide the sign-out button
	        findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
	        findViewById(R.id.sign_out_button).setVisibility(View.GONE);
	    }
	}
	
	@Override
	public void onQuestCompleted(Quest quest) {

        Toast.makeText(this, "Quest Completed", Toast.LENGTH_SHORT).show();
		Log.d(LOG_TAG, "in onQuestCompleted");
		
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
            String reward = "";

            if(rewardRaw.charAt(1) == 'p') {
                reward = "Powerup";
                incrementPowerupInventory(rewardAmount);
            }

            else {
                if(rewardRaw.charAt(1) == 'u') {
                    reward = "Undo";
                    incrementUndoInventory(rewardAmount);
                }
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
	        builder.setTitle("Quest Completed");
	        builder.setMessage("You gained " + rewardAmount + " " + reward);
	        builder.create().show();
	        
	    } catch (Exception e) {
            Toast.makeText(this, "Unable to claim quest reward", Toast.LENGTH_LONG).show();
	    	Log.w(LOG_TAG, e.toString());
	    }
	}

    /**
     * Delete the current game file and overall game statistics file
     */
    private void resetGame() {
        File currentGameFile = new File(getFilesDir(), getString(R.string.file_current_game));
        currentGameFile.delete();

        File currentStatsFile = new File(getFilesDir(), getString(R.string.file_game_stats));
        currentStatsFile.delete();
    }

    /**
     * Contains all of the game views
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.fragment_select_mode, container, false);

            View.OnTouchListener gamesOnClickListener = new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        view.setBackgroundColor(getResources().getColor(R.color.PaleTurquoise));
                    }
                    else if (event.getAction() == MotionEvent.ACTION_UP) {
                        view.setBackgroundColor(getResources().getColor(R.color.LightBlue));
                        ((SelectModeActivity)getActivity()).playGames(view);
                    }
                    return true;
                }
            };

            ImageButton achievementsButton = (ImageButton) rootView.findViewById(R.id.achievements_button);
            ImageButton leaderboardsButton = (ImageButton) rootView.findViewById(R.id.leaderboards_button);
            ImageButton questsButton = (ImageButton) rootView.findViewById(R.id.quests_button);

            achievementsButton.setOnTouchListener(gamesOnClickListener);
            leaderboardsButton.setOnTouchListener(gamesOnClickListener);
            questsButton.setOnTouchListener(gamesOnClickListener);

            return rootView;
        }
    }

    /**
     * This class makes the ad request and loads the ad.
     */
    public static class AdFragment extends Fragment {

        private AdView mAdView;

        public AdFragment() {
        }

        @Override
        public void onActivityCreated(Bundle bundle) {
            super.onActivityCreated(bundle);

            // Gets the ad view defined in layout/ad_fragment.xml with ad unit ID set in
            // values/strings.xml.
            mAdView = (AdView) getView().findViewById(R.id.adView);

            // Create an ad request
            AdRequest adRequest = new AdRequest.Builder()
                    // Test ads are shown on my device and the emulator
                    .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                    .addTestDevice(getString(R.string.test_device_id))
                    .build();

            // Start loading the ad in the background.
            mAdView.loadAd(adRequest);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_ad, container, false);
        }

        /** Called when leaving the activity */
        @Override
        public void onPause() {
            if (mAdView != null) {
                mAdView.pause();
            }
            super.onPause();
        }

        /** Called when returning to the activity */
        @Override
        public void onResume() {
            super.onResume();
            if (mAdView != null) {
                mAdView.resume();
            }
        }

        /** Called before the activity is destroyed */
        @Override
        public void onDestroy() {
            if (mAdView != null) {
                mAdView.destroy();
            }
            super.onDestroy();
        }
    }
}
