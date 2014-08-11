package com.example.app_2048;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;

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
import android.view.ViewGroup;
import android.widget.Button;
import android.os.Build;

public class MainActivity extends Activity
{
	final static String LOG_TAG = MainActivity.class.getSimpleName();
	
	// Used in the intent to pass the game mode id to GameActivity
	public final static String GAME_LOCATION = "GAME";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
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
		catch (FileNotFoundException e) {}
		catch (StreamCorruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
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
			Intent showInfo = new Intent(this, com.example.app_2048.InfoActivity.class);
			startActivity(showInfo);
			return true;
		}
		
		if (id == R.id.action_settings) {
			Intent showSettings = new Intent(this, com.example.app_2048.SettingsActivity.class);
			startActivity(showSettings);
			return true;
		}
		return super.onOptionsItemSelected(item);
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
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			return rootView;
		}
	}
	
	public void createGame(View view) {
		int gameId = GameModes.NORMAL_MODE_ID;
		
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
		case R.id.crazy_button:
			gameId = GameModes.CRAZY_MODE_ID;
			break;
		case R.id.continue_game_button:
			gameId = GameModes.LOAD_GAME_ID;
			break;
			
		default:
			Log.d(LOG_TAG, "Unexpected button pressed");
			return;
		}
		
		// Instead of passing the game to GameActivity through an
		// intent, it is saved to a file. This should allow greater
		// flexibility in the game that is passed and should allow
		// custom mode creation.
		if(gameId != GameModes.LOAD_GAME_ID) {
			Game game = GameModes.newGameFromId(gameId);
			File file = new File(getFilesDir(), getString(R.string.file_current_game));
			try {
				Save.saveGame(game, file);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		startActivity(new Intent(this, GameActivity.class));
	}
	
	/**
	 * Save the game to a file. It will later be read by GameActivity
	 * @param game The game to save
	 */
	private void saveGame(Game game) {
		
		File file = new File(getFilesDir(), "FILENAME");

		// Serialize the game
		FileOutputStream fop;
		try {
			fop = new FileOutputStream(file);
			ObjectOutputStream output = new ObjectOutputStream(fop);

			// Write the game to the file
			output.writeObject(game);

			output.close();
			fop.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
