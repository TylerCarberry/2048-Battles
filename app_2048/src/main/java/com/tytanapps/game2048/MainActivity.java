package com.tytanapps.game2048;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.quest.Quest;
import com.google.android.gms.games.quest.QuestUpdateListener;
import com.google.example.games.basegameutils.BaseGameActivity;
import com.tytanapps.game2048.MainApplication.TrackerName;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;

public class MainActivity extends BaseGameActivity implements View.OnClickListener, QuestUpdateListener
{
	private final static String LOG_TAG = MainActivity.class.getSimpleName();

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
			.add(R.id.container, new PlaceholderFragment()).commit();
		}

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
	/**
	 * Determine if a saved game exists and either enable or
	 * disable of the continue game button accordingly
	 */
	protected void onResume() {

		FileInputStream fi;
		File file = new File(getFilesDir(), getString(R.string.file_current_game));

		Button continueGame = (Button) findViewById(R.id.continue_game_button);	
		continueGame.setEnabled(false);

		try {
			fi = new FileInputStream(file);
			ObjectInputStream input = new ObjectInputStream(fi);

			// The value of game is not used but if it is able to be read
			// without any exceptions than it exists.
			Game game = (Game) input.readObject();

			fi.close();
			input.close();

			continueGame.setEnabled(true);
		}
		// If an exception is caught then the game does not exist
		// and the continue game button remains disabled
		catch (FileNotFoundException e) {}
		catch (StreamCorruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		// When the continue game button is pressed switch to the game activity
		// without saving over the saved file
		continueGame.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
                startGameActivity();
			}
		});

		super.onResume();
	}

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if(hasFocus) {
            HorizontalScrollView scrollView = (HorizontalScrollView) findViewById(R.id.modeScrollView);
            if(scrollView.getScrollX() == 0) {
                Display display = getWindowManager().getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);
                int width = size.x;
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
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		if (id == R.id.action_how_to_play) {
			Intent showInfo = new Intent(this, com.tytanapps.game2048.InfoActivity.class);
			startActivity(showInfo);
			return true;
		}

		if (id == R.id.action_stats) {
			Intent showInfo = new Intent(this, com.tytanapps.game2048.StatsActivity.class);
			startActivity(showInfo);
			return true;
		}

		if (id == R.id.action_settings) {
			Intent showSettings = new Intent(this, com.tytanapps.game2048.SettingsActivity.class);
			startActivity(showSettings);
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}


    private void createListView() {

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;

        LinearLayout listOfModes = (LinearLayout) findViewById(R.id.modeLinearLayout);
        listOfModes.removeAllViewsInLayout();

        Statistics gameStats = new Statistics();
        try {
            File file = new File(getFilesDir(), getString(R.string.file_game_stats));
            FileInputStream fi = new FileInputStream(file);
            ObjectInputStream input = new ObjectInputStream(fi);

            gameStats = (Statistics) input.readObject();

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

        // Currently there are 13 game modes, TODO: change this to not be hardcoded in
        for(int i = 1; i < 13; i++) {

            // The layout the contains all info for that mode
            LinearLayout modeDetailLayout = new LinearLayout(this);
            modeDetailLayout.setOrientation(LinearLayout.VERTICAL);

            modeDetailLayout.setLayoutParams(new LinearLayout.LayoutParams(
                            width / 2, LayoutParams.WRAP_CONTENT)
            );
            modeDetailLayout.setGravity(Gravity.CENTER_HORIZONTAL);

            // The mode name
            TextView modeName = new TextView(this);
            modeName.setText(getString(GameModes.getGameTitleById(i)));
            modeName.setTextSize(25);
            modeName.setTypeface(null, Typeface.BOLD);
            modeName.setLayoutParams(new LinearLayout.LayoutParams(
                            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            );

            // The mode description
            TextView modeDesc = new TextView(this);
            modeDesc.setText(getString(GameModes.getGameDescById(i)));
            modeDesc.setTextSize(20);
            modeDesc.setLayoutParams(new LinearLayout.LayoutParams(
                            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            );
            modeDesc.setGravity(Gravity.CENTER_HORIZONTAL);

            // High score of that mode
            TextView highScoreTextView = new TextView(this);
            highScoreTextView.setText(String.format(getString(R.string.high_score), gameStats.getHighScore(i)));
            highScoreTextView.setLayoutParams(new LinearLayout.LayoutParams(
                            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            );

            // There is a 50 dp margin between the description and high score
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)highScoreTextView.getLayoutParams();
            params.setMargins(0, 50, 0, 0);
            highScoreTextView.setLayoutParams(params);

            // Highest tile of that mode
            TextView highTileTextView = new TextView(this);
            highTileTextView.setText(String.format(getString(R.string.highest_tile), gameStats.getHighestTile(i)));
            highTileTextView.setLayoutParams(new LinearLayout.LayoutParams(
                            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            );

            // The button used to start the game
            final int gameId = i;
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
            listOfModes.addView(modeDetailLayout);
        }
    }



	/**
	 * Display all quests
	 */
	protected void showQuests()
	{
		// In the developer tutorial they use Quests.SELECT_ALL_QUESTS
		// but that is not valid for me. That may require an update
		// but for now selecting all possibilities works the same way
		int[] questParams = new int[8];
		questParams[0] = Games.Quests.SELECT_ACCEPTED;
		questParams[1] = Games.Quests.SELECT_COMPLETED;
		questParams[2] = Games.Quests.SELECT_COMPLETED_UNCLAIMED;
		questParams[3] = Games.Quests.SELECT_ENDING_SOON;
		questParams[4] = Games.Quests.SELECT_EXPIRED;
		questParams[5] = Games.Quests.SELECT_FAILED;
		questParams[6] = Games.Quests.SELECT_OPEN;
		questParams[7] = Games.Quests.SELECT_UPCOMING;
		
		Intent questsIntent = Games.Quests.getQuestsIntent(getApiClient(), questParams);
	    
	    // 0 is an arbitrary integer
	    startActivityForResult(questsIntent, 0);
	}
	
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
	
	/*
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		Log.d(LOG_TAG, "entering on activity result");
		
		if(resultCode == RESULT_OK) {
			
			Log.d(LOG_TAG, "entering if");
			
			Quest quest = data.getParcelableExtra("EXTRA_QUEST");
			
			Log.d(LOG_TAG, quest.toString());
			
			if(quest.getState() == Games.Quests.SELECT_COMPLETED_UNCLAIMED)
				onQuestCompleted(quest);
		}
	}
	*/

	/**
	 * Currently the only fragment in the activity.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			final View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);


            View.OnTouchListener gamesOnClickListener = new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        view.setBackgroundColor(getResources().getColor(R.color.Yellow));
                    }
                    else if (event.getAction() == MotionEvent.ACTION_UP) {
                        view.setBackgroundColor(getResources().getColor(R.color.white));
                        ((MainActivity)getActivity()).playGames(view);
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
	 *  When a button is pressed to change the game mode update the
	 *  title, description, and gameId
	 * @param view The button that was pressed
	 */
    /*
	public void createGame(View view) {
		
		TextView gameTitle = (TextView) findViewById(R.id.game_mode_textview);
		TextView gameDesc = (TextView) findViewById(R.id.game_desc_textview);

		switch (view.getId()) {
		case R.id.normal_button:
			gameId = GameModes.NORMAL_MODE_ID;
			break;
		case R.id.practice_button:
			gameId = GameModes.PRACTICE_MODE_ID;
			break;
		case R.id.arcade_button:
			gameId = GameModes.ARCADE_MODE_ID;
			break;
		case R.id.x_button:
			gameId = GameModes.X_MODE_ID;
			break;
		case R.id.corner_button:
			gameId = GameModes.CORNER_MODE_ID;
			break;
		case R.id.rush_button:
			gameId = GameModes.RUSH_MODE_ID;
			break;
		case R.id.survival_button:
			gameId = GameModes.SURVIVAL_MODE_ID;
			break;
		case R.id.zen_button:
			gameId = GameModes.ZEN_MODE_ID;
			break;
		case R.id.ghost_button:
			gameId = GameModes.GHOST_MODE_ID;
			break;
		case R.id.crazy_button:
			gameId = GameModes.CRAZY_MODE_ID;
			break;
		case R.id.custom_button:
			gameId = GameModes.CUSTOM_MODE_ID;
			break;
		default:
			Log.d(LOG_TAG, "Unexpected button pressed");
			// Default to normal mode
			gameId = GameModes.NORMAL_MODE_ID;
			return;
		}

		// Update the game title and description
		gameTitle.setText(getString(GameModes.getGameTitleById(gameId)));
		gameDesc.setText(getString(GameModes.getGameDescById(gameId)));
	}
	*/
	
	/**
	 * Switches to the game activity
	 */
	private void startGameActivity() {
		startActivity(new Intent(this, GameActivity.class));
	}

	@Override
	/**
	 *  Sign in has failed. Show the user the sign-in button.
	 */
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

		Log.d(LOG_TAG, "in onQuestCompleted");
		
	    // Claim the quest reward.
	    Games.Quests.claim(this.getApiClient(), quest.getQuestId(),
	            quest.getCurrentMilestone().getMilestoneId());

	    // Process the RewardData to provision a specific reward.
	    try {
	        String reward = new
	                String(quest.getCurrentMilestone().getCompletionRewardData(),
	                "UTF-8");

	        AlertDialog.Builder builder = new AlertDialog.Builder(this);
	        builder.setTitle("Quest Completed");
	        builder.setMessage("You gained " + reward);
	        builder.create().show();
	        
	    } catch (Exception e) {
	    	Log.w(LOG_TAG, e.toString());
	    }
	}
	
}
