package com.example.app_2048;

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
		if (id == R.id.action_settings) {
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

		default:
			Log.d(LOG_TAG, "Unexpected button pressed");
			return;
		}
		
		Intent startGame = new Intent(this, com.example.app_2048.GameActivity.class);
		startGame.putExtra(GAME_LOCATION, gameId);
		
		startActivity(startGame);
	}
}
