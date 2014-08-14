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

import com.tytanapps.game2048.R;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.GridLayout.Spec;
import android.widget.ImageView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Build;
import android.preference.PreferenceManager;

public class GameActivity extends Activity implements OnGestureListener {
	
	// The time in milliseconds for the animation
	public static final long SHUFFLE_SPEED = 300;
	public static final long NEW_TILE_SPEED = 300;
	
	private static boolean boardCreated = false;
	private static Game game;
	final static String LOG_TAG = GameActivity.class.getSimpleName();
	
	// Used to detect swipes and move the board
	private GestureDetectorCompat mDetector; 
	
	boolean animationInProgress = false;
	boolean gameLost = false;
	
	// TODO: This will keep track of the active animations and stop
	// them in onStop
	private ArrayList<ObjectAnimator> activeAnimations
		= new ArrayList<ObjectAnimator>();
	
	// Stores info about the game such as high score
	private static Statistics gameStats;
	
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
        
        boardCreated = false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.game, menu);
		return true;
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
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
				}
			});
			builder.create().show();

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
		
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onResume() {
		// Load the saved file containing the game. This also updates the screen.
		load();
		
		// Disable the undo button if there are no undos remaining
		Button undoButton = ((Button) findViewById(R.id.undo_button));
		undoButton.setEnabled(game.getUndosRemaining() != 0);
		
		super.onResume();
	}
	
	@Override
	protected void onStop() {
		// Only save a game that is still in progress
		if(! game.lost())
			save();
		
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
				// On my device each column is 145 pixels apart and each row is 110
				// TODO: Change this to support different screen sizes
				
				ObjectAnimator animation;
				if(direction == Location.LEFT || direction == Location.RIGHT) {
					distance *= 145;
					animation = ObjectAnimator.ofFloat(movedTile, View.TRANSLATION_X, distance);
				}
				else {
					distance *= 110;
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
			
			// Unnecessary code?
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
		for(ObjectAnimator animation: translateAnimations)
			animation.start();
	}
	
	/**
	 * Update the game information. 
	 * Turn, Score, Undos Left, and Moves Left
	 */
	public void updateGame() {
		
		//Log.d(LOG_TAG, "Entering update game");
		
		TextView turnTextView = (TextView) findViewById(R.id.turn_textview);
		TextView scoreTextView = (TextView) findViewById(R.id.score_textview);
		TextView undosTextView = (TextView) findViewById(R.id.undos_textview);
		TextView movesTextView = (TextView) findViewById(R.id.moves_textView);
		
		// Update the turn number
		turnTextView.setText(getString(R.string.turn) + " #" + game.getTurns());
		
		// Update the score
		scoreTextView.setText(getString(R.string.score) + ": " + game.getScore());
		
		// Update the undos 
		int undosLeft = game.getUndosRemaining();
		if(undosLeft >= 0)
			undosTextView.setText(getString(R.string.undo_remaining) + ": " + undosLeft);
		else
			undosTextView.setText("");
		
		// Update moves left
		int movesLeft = game.getMovesRemaining();
		if(movesLeft >= 0)
			movesTextView.setText(getString(R.string.move_remaining) + " :" + movesLeft);
		else
			movesTextView.setText("");
		
		// Update the game board
		updateGrid();
	}
	
	/**
	 * Create the game board 
	 * Precondition: the grid does not already exist
	 */
	private void createGrid() {
		
		// The grid that all tiles are on
		GridLayout gridLayout = (GridLayout) findViewById(R.id.grid_layout);
		
		// Set the number of rows and columns in the game
		gridLayout.setRowCount(game.getGrid().getNumRows());
		gridLayout.setColumnCount(game.getGrid().getNumCols());

		ImageView tile;
		Spec specRow, specCol;
		GridLayout.LayoutParams gridLayoutParam;
		int tileValue;

		// Create an ImageView for every tile on the board
		for(int row = 0; row < gridLayout.getRowCount(); row++) {
			for(int col = 0; col < gridLayout.getColumnCount(); col++) {
				specRow = GridLayout.spec(row, 1); 
				specCol = GridLayout.spec(col, 1);
				gridLayoutParam = new GridLayout.LayoutParams(specRow, specCol);
				
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
	 * Update the game board 
	 */
	private void updateGrid() {
		
		if(! boardCreated)
			createGrid();
		
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
		
		String message = "";
		
		Button undoButton = (Button) findViewById(R.id.undo_button);
		undoButton.setEnabled(false);
		
		// Notify if there is a new high score
		if(game.getScore() > gameStats.highScore) {
			gameStats.highScore = game.getScore();
			gameStats.bestGame = game;
			message += "New High Score! " + game.getScore();
		}
		
		// Notify if there is a new highest tile
		if(game.highestPiece() > gameStats.highestTile) {
			gameStats.highestTile = game.highestPiece();
			
			if(! message.equals(""))
				message += "\n"; 
			message += "New Highest Tile! " + game.highestPiece();
		}
		
		// Only notify if there is a new low score if there are no other records.
		if(gameStats.lowScore < 0 ||
				game.getScore() < gameStats.lowScore) {
			gameStats.lowScore = game.getScore();
			gameStats.worstGame = game;
			
			if(message.equals(""))
				message += "New Lowest Score! " + game.getScore();
		}
		
		// If there are no records then just show the score
		if(message.equals(""))
			message += "Final Score: " + game.getScore();
		
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
	}
	
	/**
	 * This method is no longer used because I switched
	 * to ImageViews instead of buttons
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
		ObjectAnimator.ofFloat(newTile, View.ALPHA, 1).setDuration(NEW_TILE_SPEED).start();
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
				animationInProgress = false;
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
		
		// Remove all of the tiles
		for(ObjectAnimator animation: alphaAnimations)
			animation.start();
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
				animationInProgress = false;
			}
			@Override
			public void onAnimationCancel(Animator animation) {
				Log.d(LOG_TAG, "Shuffle animation cancelled");
			}
			@Override
			public void onAnimationRepeat(Animator animation) {
				game.shuffle();
				gameStats.totalShufflesUsed += 1;
				gameStats.totalMoves += 1;
				updateGame();
			}
		});
		
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
	
	/*
	public void createCountdownTimer() {
		Log.d(LOG_TAG, "create countdown timer");

		TextView timeLeftTextView = (TextView) findViewById(R.id.time_textview);
		Timer timeLeftTimer = new Timer(timeLeftTextView, (long) game.getTimeLeft() * 1000);
		timeLeftTimer.start();
	}
	
	/**
	 * Used to update the time left TextView
	 * ***** Is currently not used in the game *****
	 */
	
	/*
	public class Timer extends CountDownTimer
	{
		private TextView timeLeftTextView;
		public final static int COUNT_DOWN_INTERVAL = 1000;
		
		public Timer(TextView textview, long millisInFuture) 
		{
			super(millisInFuture, COUNT_DOWN_INTERVAL);
			timeLeftTextView = textview;
			
			Log.d(LOG_TAG, "constructor");
		}

		public void onFinish()
		{
			Log.d(LOG_TAG, "finish");
			timeLeftTextView.setText("Time Up!");
		}

		public void onTick(long millisUntilFinished) 
		{
			Log.d(LOG_TAG, "tick");
			timeLeftTextView.setText("Time Left : " + millisUntilFinished/1000);
		}
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
	        
	        final Button iceButton = (Button) rootView.findViewById(R.id.ice);  
	        iceButton.setOnClickListener(new View.OnClickListener() {
	            public void onClick(View v) {
	            	((GameActivity)getActivity()).ice();
	            }
	        });
			
			return rootView;
		}
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
}