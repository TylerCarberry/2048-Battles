package com.tytanapps.game2048;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.event.Event;
import com.google.android.gms.games.event.Events;
import com.google.android.gms.games.quest.Quests;
import com.google.example.games.basegameutils.BaseGameActivity;
import com.tytanapps.game2048.R;
import com.tytanapps.game2048.MainApplication.TrackerName;
import com.tytanapps.game2048.R.array;
import com.tytanapps.game2048.R.drawable;
import com.tytanapps.game2048.R.id;
import com.tytanapps.game2048.R.layout;
import com.tytanapps.game2048.R.menu;
import com.tytanapps.game2048.R.string;

import junit.framework.Assert;
import android.R.color;
import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.backup.BackupManager;
import android.app.backup.RestoreObserver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.inputmethod.InputMethodSession.EventCallback;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.GridLayout.Spec;
import android.widget.ImageView;
import android.widget.ShareActionProvider;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Build;
import android.preference.PreferenceManager;

public class GameActivity extends BaseGameActivity implements OnGestureListener {
	
	// The time in milliseconds for the animation
	public static final long SHUFFLE_SPEED = 300;
	public static final long NEW_TILE_SPEED = 300;
	
	private static boolean boardCreated = false;
	private static Game game;
	final static String LOG_TAG = GameActivity.class.getSimpleName();
	
	// Used to detect swipes and move the board
	private GestureDetectorCompat mDetector; 
	
	String appUrl = "https://play.google.com/store/apps/details?id=com.tytanapps.game2048";
	
	boolean animationInProgress = false;
	boolean gameLost = false;
	
	// This keeps track of the active animations and
	// stops them in onStop
	private ArrayList<ObjectAnimator> activeAnimations
		= new ArrayList<ObjectAnimator>();
	
	// Stores info about the game such as high score
	private static Statistics gameStats;
	
	// The distance in pixels between tiles
	private static int verticalTileDistance = 0;
	private static int horizontalTileDistance = 0;
	
	ShareActionProvider mShareActionProvider;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_game);
		
		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.container, new GameFragment()).commit();
		}
		
		// Instantiate the gesture detector with the
        // application context and an implementation of
        // GestureDetector.OnGestureListener
		mDetector = new GestureDetectorCompat(this,this);

		//Get a Tracker (should auto-report)
		((MainApplication) getApplication()).getTracker(MainApplication.TrackerName.APP_TRACKER);

		// Get tracker.
		Tracker t = ((MainApplication) getApplication()).getTracker(TrackerName.APP_TRACKER);

		// Set screen name.
		// Where path is a String representing the screen name.
		t.setScreenName("Game Activity");

		// Send a screen view.
		t.send(new HitBuilders.AppViewBuilder().build());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.game, menu);
		
		// Locate MenuItem with ShareActionProvider
	    MenuItem item = menu.findItem(R.id.menu_item_share);

	    // Fetch and store ShareActionProvider
	    mShareActionProvider = (ShareActionProvider) item.getActionProvider();
	    
		createShareIntent();
	    
		return true;
	}
	
	// Call to update the share intent
	private void createShareIntent() {
		Intent shareIntent = new Intent();
		shareIntent.setAction(Intent.ACTION_SEND);
		shareIntent.putExtra(Intent.EXTRA_TEXT,
				"I am playing 2048. My high score is " + gameStats.highScore
				+ ". Try to beat me! " + appUrl);
		shareIntent.setType("text/plain");
		if (mShareActionProvider != null) {
	        mShareActionProvider.setShareIntent(shareIntent);
	    }
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		// When the powerups menu button is pressed show a dialog to choose between
		// shuffle game and remove low tiles
		if (id == R.id.action_powerups) {
			showPowerupDialog();
			return true;
		}
		
		// When the how to play menu item is pressed switch to InfoActivity
		if (id == R.id.action_how_to_play) {
			Intent showInfo = new Intent(this, com.tytanapps.game2048.InfoActivity.class);
			startActivity(showInfo);
			return true;
		}
		// When the settings menu item is pressed switch to SettingsActivity
		if(id == R.id.action_settings) {
			Intent showSettings = new Intent(this, com.tytanapps.game2048.SettingsActivity.class);
			startActivity(showSettings);
			return true;
		}
		// When the achievements pressed
		if(id == R.id.action_achievements) {
			startActivityForResult(Games.Achievements.getAchievementsIntent(getApiClient()), 1);
			//showQuests();
			return true;
		}
		if(id == R.id.action_leaderboards){
			startActivityForResult(Games.Leaderboards.getAllLeaderboardsIntent(getApiClient()), 2);
		}
		
		/*
		// When the achievements pressed
		if(id == R.id.action_quests) {
			showQuests();
			return true;
		}
		*/

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onStart() {
		
		Log.d(LOG_TAG, "on start");
		
		// If GameActivity is loaded for the first time the grid is created. If user returns to
		// this activity after switching to another activity, the grid is still recreated because
		// there is a chance that android killed this activity in the background
		boardCreated = false;
		
		// Load the saved file containing the game. This also updates the screen.
		load();
		
		// Disable the undo button if there are no undos remaining
		Button undoButton = ((Button) findViewById(R.id.undo_button));
		undoButton.setEnabled(game.getUndosRemaining() != 0);
		
		GoogleAnalytics.getInstance(this).reportActivityStart(this);
		
		super.onStart();
	}
	
	@Override
	protected void onStop() {
		
		Log.d(LOG_TAG, "on stop");
		
		// Only save a game that is still in progress
		if(! game.lost())
			save();
		
		for(ObjectAnimator animation : activeAnimations)
			animation.end();
		
		animationInProgress = false;
		
		GoogleAnalytics.getInstance(this).reportActivityStop(this);

		
		super.onStop();
	}
	
	/**
	 * Moves all of the tiles
	 * @param direction Should use the static variables in Location class
	 */
	public void act(int direction) {
		// If the ice attack is active in that direction do not move
		if(game.getIceDuration() > 0 && game.getIceDirection() == direction)
			return;
		
		animationInProgress = true;
		
		if(game.getArcadeMode() && Math.random() < 0.1)
			addRandomBonus();
		
		calculateDistances();
		int highestTile = game.highestPiece();
		
		// Load the speed to move the tiles from the settings
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
				
		int speed = Integer.valueOf(prefs.getString("speed", "200"));
		
		// Save the game history before each move
		game.saveGameInHistory();
		
		GridLayout gridLayout = (GridLayout) findViewById(R.id.grid_layout);
		
		// Get a list of all tiles
		List<Location> tiles = game.getGrid().getLocationsInTraverseOrder(direction);
		
		// An list of the move animations to play
		ArrayList<ObjectAnimator> translateAnimations = new ArrayList<ObjectAnimator>();
		
		// Loop through each tile
		for(Location tile : tiles) {
			// Determine the number of spaces to move
			int distance = game.move(tile, direction);
			
			// Only animate buttons that moved
			if(distance > 0) {
				
				if(direction == Location.LEFT || direction == Location.UP)
					distance *= -1;
				
				ImageView movedTile = (ImageView) gridLayout.findViewById(tile.getRow() * 100 + tile.getCol());
				
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
				animation.setDuration(speed);
				
				// Add the new animation to the list
				translateAnimations.add(animation);
			}
		}
		
		if(translateAnimations.isEmpty()) {
			animationInProgress = false;
			
			if(game.lost())
				lost();
				
			return;
		}
		
		translateAnimations.get(0).addListener(new AnimatorListener(){
			@Override
			// When the animation is over increment the turn number, update the game, 
			// and add a new tile
			public void onAnimationEnd(Animator animation) {
				game.newTurn();
				updateGame();
				addTile();
				gameStats.totalMoves += 1;
				activeAnimations.clear();
				animationInProgress = false;
				
				if(game.getIceDuration() > 0)
				Toast.makeText(getApplicationContext(),
					"FROZEN!	Cannot move " +
					Location.directionToString(game.getIceDirection()) +
					" for " + game.getIceDuration() + " turns",
					Toast.LENGTH_SHORT).show();
			}
			
			@Override
			public void onAnimationStart(Animator animation) { }
			@Override
			public void onAnimationCancel(Animator animation) {
				Log.d(LOG_TAG, "Animation cancelled");
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
		
		if(game.highestPiece() > highestTile && game.getGameModeId() == GameModes.NORMAL_MODE_ID)
			if(game.highestPiece() >= 128)
				unlockAchievementNewHighestTile(game.highestPiece());
	}
	
	private void addRandomBonus() {
		double rand = Math.random();
		String item = null;
		Log.d(LOG_TAG, ""+rand);
		if(rand < 0.5) 
			ice();
		else {
			if(rand < 0.75) {
				game.incrementUndosRemaining();
				item = "Undo";
			}
			else {
				game.incrementPowerupsRemaining();
				item = "Powerup";
			}
		
		Toast.makeText(getApplicationContext(),	"Bonus! +1 " + item,
				Toast.LENGTH_SHORT).show();
		}
	}
	
	/**
	 * Update the game information. 
	 * Turn, Score, Undos Left, and Moves Left
	 */
	public void updateGame() {
		
		TextView turnTextView = (TextView) findViewById(R.id.turn_textview);
		TextView scoreTextView = (TextView) findViewById(R.id.score_textview);
		TextView undosTextView = (TextView) findViewById(R.id.undos_textview);
		TextView movesTextView = (TextView) findViewById(R.id.moves_textView);
		TextView powerupsTextView = (TextView) findViewById(R.id.powerups_textview);
		
		// Update the turn number
		turnTextView.setText(getString(R.string.turn) + " #" + game.getTurns());
		
		// Update the score
		scoreTextView.setText(getString(R.string.score) + ": " + game.getScore());
		
		// Update the undos left
		int undosLeft = game.getUndosRemaining();
		if(undosLeft >= 0)
			undosTextView.setText(getString(R.string.undo_remaining) + ": " + undosLeft);
		else
			undosTextView.setVisibility(View.INVISIBLE);
		
		// Update moves left
		int movesLeft = game.getMovesRemaining();
		if(movesLeft >= 0)
			movesTextView.setText(getString(R.string.move_remaining) + ": " + movesLeft);
		else
			movesTextView.setVisibility(View.INVISIBLE);
		
		// Update powerups left
		int powerupsLeft = game.getPowerupsRemaining();
		if(powerupsLeft >= 0)
			powerupsTextView.setText(getString(R.string.powerups_remaining) + ": " + powerupsLeft);
		else
			powerupsTextView.setVisibility(View.INVISIBLE);

		// Update the game board
		updateGrid();
	}
	
	/**
	 * Create the game board
	 */
	private void createGrid() {
		
		// The grid that all tiles are on
		GridLayout gridLayout = (GridLayout) findViewById(R.id.grid_layout);
		
		// Set the number of rows and columns in the game
		gridLayout.setRowCount(game.getGrid().getNumRows());
		gridLayout.setColumnCount(game.getGrid().getNumCols());

		// The new tile to insert
		ImageView tile;
		// Used to check if the tile already exists
		ImageView existingTile;
		Spec specRow, specCol;
		GridLayout.LayoutParams gridLayoutParam;
		int tileValue;

		// Create an ImageView for every tile on the board
		for(int row = 0; row < gridLayout.getRowCount(); row++) {
			for(int col = 0; col < gridLayout.getColumnCount(); col++) {
				specRow = GridLayout.spec(row, 1); 
				specCol = GridLayout.spec(col, 1);
				gridLayoutParam = new GridLayout.LayoutParams(specRow, specCol);
				
				// Check if that tile already exists
				existingTile = (ImageView) findViewById(row * 100 + col);

				// Remove the existing tile if there is one
				if(existingTile!=null) {
					((ViewGroup) existingTile.getParent()).removeView(existingTile);
				}

				tile = new ImageView(this);
				tile.setId(row * 100 + col);
				
				tileValue = game.getGrid().get(new Location(row, col));
				if(tileValue == 0)
					tile.setVisibility(View.INVISIBLE);
				else 
					tile.setVisibility(View.VISIBLE);
				
				gridLayout.addView(tile,gridLayoutParam);
			}
		}
		boardCreated = true;
	}
	
	/**
	 * Calculate the distances that tiles should move when the game is swiped
	 */
	private void calculateDistances() {
		GridLayout grid = (GridLayout) findViewById(R.id.grid_layout);
		
		verticalTileDistance = grid.getHeight() / game.getGrid().getNumRows();
		horizontalTileDistance = grid.getWidth() / game.getGrid().getNumCols();
	}

	/**
	 * Update the game board 
	 */
	private void updateGrid() {
		
		if(! boardCreated) {
			createGrid();
		}
		
		GridLayout gridLayout = (GridLayout) findViewById(R.id.grid_layout);
		gridLayout.setRowCount(game.getGrid().getNumRows());
		gridLayout.setColumnCount(game.getGrid().getNumCols());
		
		ImageView tile;
		Spec specRow, specCol;
		GridLayout.LayoutParams gridLayoutParam;
		int tileValue;
		int expectedValue, actualValue;

		for(int row = 0; row < gridLayout.getRowCount(); row++) {
			for(int col = 0; col < gridLayout.getColumnCount(); col++) {

				tile = (ImageView) gridLayout.findViewById(row * 100 + col);

				expectedValue = game.getGrid().get(new Location(row,col));
				
				// A tiles's tag is its value
				try {
					actualValue = Integer.parseInt(tile.getTag().toString());
				}
				catch(Exception e) {
					// Update the tile just in case
					actualValue = -10;
				}

				if(expectedValue != actualValue) {
					
					specRow = GridLayout.spec(row, 1); 
					specCol = GridLayout.spec(col, 1);
					gridLayoutParam = new GridLayout.LayoutParams(specRow, specCol);

					// Remove the tile
					ViewGroup layout = (ViewGroup) tile.getParent();
					if(null!=layout) {
						layout.removeView(tile);
					}

					// Create a new tile to insert back into the board
					tile = new ImageView(this);
					tile.setId(row * 100 + col);
					
					tileValue = game.getGrid().get(new Location(row, col));
					tile.setTag(tileValue);
					setIcon(tile, tileValue);

					if(tileValue == 0) 
						tile.setVisibility(View.INVISIBLE);
					else
						tile.setVisibility(View.VISIBLE);

					// Insert the new tile into the board
					gridLayout.addView(tile,gridLayoutParam);
				}
			}
			
			if(game.lost())
				lost();
		}
	}

	/**
	 * Update the tile's icon to match its value
	 * @param tile The ImageView to change
	 * @param tileValue The numerical value of the tile
	 */
	private void setIcon(ImageView tile, int tileValue) {

		if(game.getGameModeId() == GameModes.GHOST_MODE_ID)
			tile.setBackgroundResource(R.drawable.tile_question);
		else {
			switch(tileValue) {
			case -2:
				tile.setBackgroundResource(R.drawable.tile_x);
				break;
			case -1:
				tile.setBackgroundResource(R.drawable.tile_corner);
				break;
			case 0:
				tile.setBackgroundResource(R.drawable.tile_blank);
				break;
			case 2:
				tile.setBackgroundResource(R.drawable.tile_2);
				break;
			case 4:
				tile.setBackgroundResource(R.drawable.tile_4);
				break;
			case 8:
				tile.setBackgroundResource(R.drawable.tile_8);
				break;
			case 16:
				tile.setBackgroundResource(R.drawable.tile_16);
				break;
			case 32:
				tile.setBackgroundResource(R.drawable.tile_32);
				break;
			case 64:
				tile.setBackgroundResource(R.drawable.tile_64);
				break;
			case 128:
				tile.setBackgroundResource(R.drawable.tile_128);
				break;
			case 256:
				tile.setBackgroundResource(R.drawable.tile_256);
				break;
			case 512:
				tile.setBackgroundResource(R.drawable.tile_512);
				break;
			case 1024:
				tile.setBackgroundResource(R.drawable.tile_1024);
				break;
			case 2048:
				tile.setBackgroundResource(R.drawable.tile_2048);
				break;
			// If I did not create an image for the tile,
			// default to a question mark
			default:
				tile.setBackgroundResource(R.drawable.tile_question);
			}
		}
	}
	
	private void lost() {
		
		// Prevent the notification from appearing multiple times
		if(gameLost)
			return;
		
		gameLost = true;
		
		// This is the only place where total games played is incremented.
		gameStats.totalGamesPlayed++;
		
		// Create a new lose dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("You Lost");
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
		
		// Update the leaderboards
		if(getApiClient().isConnected()){
            Games.Leaderboards.submitScore(getApiClient(), 
                    getString(R.string.leaderboard_classic_mode), 
                    game.getScore());
            
            Games.Leaderboards.submitScore(getApiClient(), 
                    getString(R.string.leaderboard_lowest_score), 
                    game.getScore());
        }
		
		// You cannot undo a game once you lose
		Button undoButton = (Button) findViewById(R.id.undo_button);
		undoButton.setEnabled(false);
		
		// Create the message to show the player
		String message = "";
		message = createLoseMessage(game, gameStats);
		builder.setMessage(message);
		AlertDialog dialog = builder.create();
		
		// You must click on one of the buttons in order to dismiss the dialog
		dialog.setCanceledOnTouchOutside(false);

		// Show the dialog
		dialog.show();
		
		save();
		
		// Delete the current save file. The user can no longer continue this game.
		File currentGameFile = new File(getFilesDir(), getString(R.string.file_current_game));
		currentGameFile.delete();
		
		submitEvent();
	}
	
	public void showQuests()
	{
		int[] foo = new int[1];
		foo[0] = Games.Quests.SELECT_ACCEPTED;
	    Intent questsIntent = Games.Quests.getQuestsIntent(getApiClient(), foo);
	    startActivityForResult(questsIntent, 0);
	}
	
	
	public void submitEvent()
	{
	    // eventId is taken from the developer console
	    String myEventId = getString(R.string.event_lose_game);

	    // increment the event counter
	    Games.Events.increment(this.getApiClient(), myEventId, 1);
	    
	    Log.d(LOG_TAG, "incremented event");
	    
	    //callback();
	}
	
	/*
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

	        for (int i=0; i < eb.getCount(); i++)
	        {
	        	Toast.makeText(getApplicationContext(),
	    				eb,
	    				Toast.LENGTH_SHORT).show();
	        }
	        eb.close();
	    }
	}
	*/
	
	
	/** Create the message that is shown to the user after they lose.
	 * 
	 * @param myGame The game that was currently played
	 * @param myGameStats The game stats of the game
	 * @return The message to display
	 */
	private String createLoseMessage(Game myGame, Statistics myGameStats) {
		String message = "";
		// Notify if there is a new high score
		if(myGame.getScore() > myGameStats.highScore) {
			myGameStats.highScore = myGame.getScore();
			myGameStats.bestGame = myGame;
			message += "New High Score! " + myGame.getScore();
		}

		// Notify if there is a new highest tile
		if(myGame.highestPiece() > myGameStats.highestTile) {
			myGameStats.highestTile = myGame.highestPiece();

			if(! message.equals(""))
				message += "\n"; 
			message += "New Highest Tile! " + myGame.highestPiece();
		}

		// Only notify if there is a new low score if there are no other records.
		if(myGameStats.lowScore < 0 ||
				myGame.getScore() < myGameStats.lowScore) {
			myGameStats.lowScore = myGame.getScore();
			myGameStats.worstGame = myGame;

			if(message.equals(""))
				message += "New Lowest Score! " + myGame.getScore();
		}

		// If there are no records then just show the score
		if(message.equals(""))
			message += "Final Score: " + myGame.getScore();
		return message;
	}
	
	private void showPowerupDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		if(game.lost()) {
			builder.setTitle("Cannot use powerup")
			.setMessage("You cannot use powerups after you lose");
		}
		else
			if(game.getPowerupsRemaining() != 0) {
				builder.setTitle("Choose powerup")
				.setItems(R.array.powerups, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						// The 'which' argument contains the index position
						// of the selected item
						switch(which) {
						case 0:
							shuffleGame();
							break;
						case 1:
							removeLowTiles();
						}
						game.decrementPowerupsRemaining();
					}
				});
			}
			else {
				builder.setTitle("No More Powerups")
				.setMessage("There are no powerups remaining");
			}
		builder.create().show();
	}

	/**
	 * This method is no longer used because I switched
	 * to ImageViews instead of buttons. I may need this method 
	 * later for zen mode.
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
	 * Add a new tile to the board
	 */
	private void addTile() {
		
		// Add a new tile to the game object
		Location loc = game.addRandomPiece();
		
		// Find the tile to make appear
		ImageView newTile = (ImageView) findViewById(loc.getRow() * 100 + loc.getCol());
		
		// Immediately set the alpha of the tile to 0
		ObjectAnimator.ofFloat(newTile, View.ALPHA, 0).setDuration(0).start();
		
		// Update the new tile's tag and icon
		int tileValue = game.getGrid().get(loc);
		newTile.setTag(tileValue);
		setIcon(newTile, tileValue);
		
		// Make the tile visible. It still cannot be seen because the alpha is 0
		newTile.setVisibility(View.VISIBLE);
		
		// Fade the tile in
		
		ObjectAnimator alphaAnimation = ObjectAnimator.ofFloat(newTile, View.ALPHA, 1)
				.setDuration(NEW_TILE_SPEED);
		
		// Assume that all animations finish at the same time
		alphaAnimation.addListener(new AnimatorListener(){

			@Override
			public void onAnimationEnd(Animator animation) {
				activeAnimations.clear();
			}

			@Override
			public void onAnimationStart(Animator animation) { }
			@Override
			public void onAnimationCancel(Animator animation) {
				Log.d(LOG_TAG, "Animation cancelled");
				activeAnimations.clear();
			}
			@Override
			public void onAnimationRepeat(Animator animation) { }
		});
		
		activeAnimations.add(alphaAnimation);
		alphaAnimation.start();
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
		
		GridLayout gridLayout = (GridLayout) findViewById(R.id.grid_layout);
		
		// Loop through each tile
		for(Location tile : tiles) {
			if(gameBoard.get(tile) == 2 || gameBoard.get(tile) == 4) {
				
				ImageView toRemove = (ImageView) gridLayout.findViewById(tile.getRow() * 100 + tile.getCol());
				
				// Set the tag to the new value
				toRemove.setTag(0);
				
				// Create a new animation of the tile fading away and
				// add it to the list
				alphaAnimations.add(ObjectAnimator.ofFloat(toRemove, View.ALPHA, 0)
						.setDuration(NEW_TILE_SPEED));
			}
		}
		
		if(alphaAnimations.isEmpty()) {
			animationInProgress = false;
			return;
			
		}
		
		// Assume that all animations finish at the same time
		alphaAnimations.get(0).addListener(new AnimatorListener(){
			
			@Override
			public void onAnimationEnd(Animator animation) {
				game.removeLowTiles();
				game.newTurn();
				gameStats.totalMoves += 1;
				updateGame();
				activeAnimations.clear();
				animationInProgress = false;
			}
			
			@Override
			public void onAnimationStart(Animator animation) { }
			@Override
			public void onAnimationCancel(Animator animation) {
				Log.d(LOG_TAG, "Animation cancelled");
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
	 * The grid layout spins 360¡, the tiles are shuffled, then it spins
	 * back in the opposite direction
	 */
	private void shuffleGame() {
		
		// Save the game history before each move
		game.saveGameInHistory();
		
		GridLayout gridLayout = (GridLayout) findViewById(R.id.grid_layout);
		
		// Causes conflicts when the shuffle button is double tapped
		if(animationInProgress)
			return;
		
		ObjectAnimator rotateAnimation =
				ObjectAnimator.ofFloat(gridLayout, View.ROTATION, 360);
		rotateAnimation.setRepeatCount(1);
		rotateAnimation.setRepeatMode(ValueAnimator.REVERSE);
		
		// 300 ms should be fast enough to not notice the tiles changing
		rotateAnimation.setDuration(SHUFFLE_SPEED);
		
		rotateAnimation.addListener(new AnimatorListener(){
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
				Log.d(LOG_TAG, "Shuffle animation cancelled");
				activeAnimations.clear();
				animationInProgress = false;
			}
			@Override
			public void onAnimationRepeat(Animator animation) {
				game.shuffle();
				gameStats.totalShufflesUsed += 1;
				gameStats.totalMoves += 1;
				updateGame();
			}
		});
		
		activeAnimations.add(rotateAnimation);
		rotateAnimation.start();
	}
	
	/**
	 * Freezes the game (can not move in a direction for a random amount of turns)
	 */
	public void ice() {
		
		// This attack cannot be stacked
		if(game.getIceDuration() <= 0)
			game.ice();
		
		// Create a new toast to diplay the attack
		Toast.makeText(getApplicationContext(),
				"FROZEN!	Cannot move " +
				Location.directionToString(game.getIceDirection()) +
				" for " + game.getIceDuration() + " turns",
				Toast.LENGTH_SHORT).show();
	}
	
	/**
	 * Undo the game. Currently does not have any animations because it
	 * would be difficult to track every tile separately
	 */
	public void undo() {
		if(game.getUndosRemaining() == 0) {
			((Button) findViewById(R.id.undo_button)).setEnabled(false);
		}
		else
		{
			game.undo();
			gameStats.totalMoves += 1;
			gameStats.totalUndosUsed += 1;
			updateGame();
		}
	}
	
	/**
	 * Restart the game.
	 */
	public void restartGame() {
		// Save any new records
		if(game.highestPiece() > gameStats.highestTile)
			gameStats.highestTile = game.highestPiece();
		
		if(game.getScore() > gameStats.highScore) {
			gameStats.highScore = game.getScore();
			gameStats.bestGame = game;
		}
		
		game = GameModes.newGameFromId(game.getGameModeId());

		// Set the undo button to be enabled or disabled
		Button undoButton = (Button) findViewById(R.id.undo_button);
		undoButton.setEnabled(game.getUndosRemaining() != 0);
		
		gameLost = false;
		updateGame();
	}

	/**
	 * Save the game and game stats to a file
	 */
	private void save() {
		
		File currentGameFile = new File(getFilesDir(), getString(R.string.file_current_game));
		File gameStatsFile = new File(getFilesDir(), getString(R.string.file_game_stats));

		try {
			Save.save(game, currentGameFile);
			Save.save(gameStats, gameStatsFile);
		} catch (IOException e) {
			e.printStackTrace();
			// Notify the user of the error through a toast
			Toast.makeText(getApplicationContext(), "Error: Save file not found", Toast.LENGTH_SHORT).show();
		}
		
		requestBackup();
	}
	
	public void requestBackup() {

		SharedPreferences settings = getSharedPreferences("hi", 0);
		
		BackupManager bm = new BackupManager(this);
		bm.dataChanged();
	}
	
	public void requestRestore()
	{
		BackupManager bm = new BackupManager(this);
		bm.requestRestore(
				new RestoreObserver() {
					@Override
					public void restoreStarting(int numPackages) {
						Log.d(LOG_TAG, "Restore from cloud starting.");
						Log.d(LOG_TAG, ""+gameStats.totalMoves);
						
						super.restoreStarting(numPackages);
					}
					
					@Override
					public void onUpdate(int nowBeingRestored, String currentPackage) {
						Log.d(LOG_TAG, "Restoring "+currentPackage);
						super.onUpdate(nowBeingRestored, currentPackage);
					}
					
					@Override
					public void restoreFinished(int error) {
						Log.d(LOG_TAG, "Restore from cloud finished.");
						
						super.restoreFinished(error);
						Log.d(LOG_TAG, ""+gameStats.totalMoves);
						
					}
				});
	}

	/**
	 * Load the game from a file and update the game
	 */
	private void load() {
		
		File currentGameFile = new File(getFilesDir(), getString(R.string.file_current_game));
		File gameStatsFile = new File(getFilesDir(), getString(R.string.file_game_stats));

		try {
			game = (Game) Save.load(currentGameFile);
			gameStats = (Statistics) Save.load(gameStatsFile);
		} catch (ClassNotFoundException e) {
			Log.w(LOG_TAG, "Class not found exception in load");
			game = new Game();
			gameStats = new Statistics();
		} catch (IOException e) {
			Log.w(LOG_TAG, "IO Exception in load");
			game = new Game();
			gameStats = new Statistics();
		}
		
		updateGame();
	}
	
	/**
	 * Unlock an achievement when a new highest tile is reached 
	 * @param tile The new highest tile
	 */
	private void unlockAchievementNewHighestTile(int tile) {
		
		Log.d(LOG_TAG, "unlocking achievement " + tile + " tile");
		
		if(getApiClient().isConnected()) {
			Log.d(LOG_TAG, "successfully unlocked achievement 128 tile");
			switch(tile) {
			case 128:
				Games.Achievements.unlock(getApiClient(), getString(R.string.tile_128_achievement));
				break;
			case 256:
				Games.Achievements.unlock(getApiClient(), getString(R.string.tile_256_achievement));
				break;
			case 512:
				Games.Achievements.unlock(getApiClient(), getString(R.string.tile_512_achievement));
				break;
			case 1024:
				Games.Achievements.unlock(getApiClient(), getString(R.string.tile_1024_achievement));
				break;
			case 2048:
				Games.Achievements.unlock(getApiClient(), getString(R.string.tile_2048_achievement));
				break;
				
			}
		}
	}
	
	/**
	 * Shows the active quests
	 */
	/*
	public void showQuests()
	{
		// EventCallback is a subclass of ResultCallback; use this to handle the
		// query results
		EventCallback ec = new EventCallback();

		// Load all events tracked for your game
		com.google.android.gms.common.api.PendingResult<Events.LoadEventsResult>
		        pr = Games.Events.load(this.getApiClient(), true);
		pr.setResultCallback(ec);
		
		Intent questsIntent = Games.Quests.getQuestsIntent(this.getApiClient(),
	            Quests.SELECT_ALL_QUESTS);
	    startActivityForResult(questsIntent, 0);
	}
	*/

	/**
	 * The only fragment in the activity. Has the game board and the
	 * game info such as score or turn number
	 */
	public static class GameFragment extends Fragment {

		public GameFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			
			View rootView = inflater.inflate(R.layout.fragment_game, container,
					false);
			
			final Button undoButton = (Button) rootView.findViewById(R.id.undo_button);  
	        undoButton.setOnClickListener(new View.OnClickListener() {
	            public void onClick(View v) {
	            	((GameActivity)getActivity()).undo();
	            }
	        });
			
	        final Button restartButton = (Button) rootView.findViewById(R.id.restart_button);  
	        restartButton.setOnClickListener(new View.OnClickListener() {
	            public void onClick(View v) {
	            	((GameActivity)getActivity()).restartGame();
	            }
	        });
	        
	        /*
	        // Get tracker.
	        Tracker t = ((MainApplication) getActivity().getApplication()).getTracker();

	        // Set screen name.
	        // Where path is a String representing the screen name.
	        t.setScreenName("GameActivity");

	        // Send a screen view.
	        t.send(new HitBuilders.AppViewBuilder().build());

	        t.send(new HitBuilders.ScreenViewBuilder().build());
	        */
	        
	        return rootView;
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch(keyCode){
		case KeyEvent.KEYCODE_DPAD_UP:
			act(Location.UP);
			return true; 
		case KeyEvent.KEYCODE_DPAD_LEFT:
			act(Location.LEFT);
			return true; 
		case KeyEvent.KEYCODE_DPAD_RIGHT:
			act(Location.RIGHT);
			return true; 
		case KeyEvent.KEYCODE_DPAD_DOWN:
			act(Location.DOWN);
			return true; 
		}
		return super.onKeyDown(keyCode, event);
	}
	
	@Override 
    public boolean onTouchEvent(MotionEvent event){ 
        this.mDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }
	
    /**
     * When the screen is swiped move the board
     */
    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2, 
            float velocityX, float velocityY) {
        
    	// If there is currently an animation in progress ignore the swipe
    	if(!animationInProgress) {
    		// Horizontal swipe
    		if(Math.abs(velocityX) > Math.abs(velocityY))
    			if(velocityX > 0)
    				act(Location.RIGHT);
    			else
    				act(Location.LEFT);
    		// Vertical
    		else
    			if(velocityY > 0)
    				act(Location.DOWN);
    			else
    				act(Location.UP);
    	}
    	return true;
    }
    
    @Override
    public boolean onDown(MotionEvent event) { return true; }
    @Override
    public void onLongPress(MotionEvent event) {}
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
            float distanceY) { return true; }
    @Override
    public void onShowPress(MotionEvent event) {}
    @Override
    public boolean onSingleTapUp(MotionEvent event) { return true; }

    // Google+ sign in
	@Override
	public void onSignInFailed() {}
	@Override
	public void onSignInSucceeded() {}
}