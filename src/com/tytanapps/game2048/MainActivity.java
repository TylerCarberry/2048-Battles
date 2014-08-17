package com.tytanapps.game2048;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;

import com.google.android.gms.games.Games;
import com.google.example.games.basegameutils.BaseGameActivity;
import com.tytanapps.game2048.R;
import com.tytanapps.game2048.R.id;
import com.tytanapps.game2048.R.layout;
import com.tytanapps.game2048.R.menu;
import com.tytanapps.game2048.R.string;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.os.Build;

public class MainActivity extends BaseGameActivity implements View.OnClickListener
{
	final static String LOG_TAG = MainActivity.class.getSimpleName();
	
	// Stores the mode that is currently selected
	private static int gameId = GameModes.NORMAL_MODE_ID;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
			.add(R.id.container, new PlaceholderFragment()).commit();
		}
		
		//findViewById(R.id.sign_in_button).setOnClickListener(this);
	    //findViewById(R.id.sign_out_button).setOnClickListener(this);
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

		// When the start game button is pressed
		Button startGame = (Button) findViewById(R.id.start_game_button);
		startGame.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// Instead of passing the game to GameActivity through an intent,
				// it is saved to a file. This should allow greater flexibility in
				// the game that is passed and should allow custom mode creation.
				if(gameId != GameModes.LOAD_GAME_ID) {
					Game game = GameModes.newGameFromId(gameId);
					game.setGameModeId(gameId);
					File currentGameFile = new File(getFilesDir(), getString(R.string.file_current_game));
					try {
						Save.save(game, currentGameFile);
					}
					catch (IOException e) {
						e.printStackTrace();
					} 
				}
				// Switch to the game activity
				startGameActivity();
			}
		});
		
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

	/**
	 * Currently the only fragment in the activity.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			
			return rootView;
		}
	}

	/**
	 *  When a button is pressed to change the game mode update the
	 *  title, description, and gameId
	 * @param view The button that was pressed
	 */
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
		case R.id.pro_button:
			gameId = GameModes.PRO_MODE_ID;
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
	
	/**
	 * Switches to the game activity
	 */
	public void startGameActivity() {
		startActivity(new Intent(this, GameActivity.class));
	}

	@Override
	public void onSignInFailed() {
	    // Sign in has failed. So show the user the sign-in button.
	    findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
	    findViewById(R.id.sign_out_button).setVisibility(View.GONE);
	    
	    Log.d(LOG_TAG, "login failed");
	}

	@Override
	public void onSignInSucceeded() {
	    // show sign-out button, hide the sign-in button
	    findViewById(R.id.sign_in_button).setVisibility(View.GONE);
	    findViewById(R.id.sign_out_button).setVisibility(View.VISIBLE);

	    // (your code here: update UI, enable functionality that depends on sign in, etc)
	}

	@Override
	public void onClick(View view) {
	    if (view.getId() == R.id.sign_in_button) {
	        // start the asynchronous sign in flow
	        beginUserInitiatedSignIn();
	    }
	    else if (view.getId() == R.id.sign_out_button) {
	        
	    	Log.d(LOG_TAG, "clicked sign out");
	    	
	    	// sign out.
	        signOut();

	        // show sign-in button, hide the sign-out button
	        findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
	        findViewById(R.id.sign_out_button).setVisibility(View.GONE);
	    }
	}
}
