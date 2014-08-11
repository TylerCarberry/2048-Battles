package com.example.app_2048;

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

import junit.framework.Assert;
import android.R.color;
import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
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
	private GestureDetectorCompat mDetector; 
	// private boolean madeFirstMove = false;
	boolean animationInProgress = false;
	
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
		
		if (id == R.id.action_remove_low) {
			removeLowTiles();
			updateGame();
			return true;
		}
		if (id == R.id.action_how_to_play) {
			Intent showInfo = new Intent(this, com.example.app_2048.InfoActivity.class);
			startActivity(showInfo);
			return true;
		}
		if(id == R.id.action_settings) {
			Intent showSettings = new Intent(this, com.example.app_2048.SettingsActivity.class);
			startActivity(showSettings);
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onResume() {
		loadGame();
		updateGame();
		super.onResume();
	}
	
	@Override
	protected void onStop() {
		saveGame();
		super.onStop();
	}
	
	/**
	 * When a button is pressed to make the game act
	 * @param view The button that was pressed
	 */
	public void act(View view) {
		switch(view.getId()) {
		case R.id.undo_button:
			if(game.getMovesRemaining() != 0)
				undo();
			break;
		case R.id.shuffle_button:
			shuffleGame();
			break;
		case R.id.restart:
			game = new Game();
			updateGame();
			break;
		}
	}
	
	/**
	 * Moves all of the tiles
	 * @param direction Should use the static variables in Location class
	 */
	public void act(int direction) {
		
		animationInProgress = true;
		
		if(! game.canMove(direction)) {
			animationInProgress = false;
			return;
		}

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		int speed = Integer.valueOf(prefs.getString("speed", "300"));
		
		// Save the game history before each move
		game.saveGameInHistory();
		
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
				
				Button movedButton = (Button) findViewById(tile.getRow() * 100 + tile.getCol());
				movedButton.setTag("Moved");
				
				// Determine the distance to move in pixels
				// On my device each column is 145 pixels apart and each row is 110
				// TODO: Change this to support different screen sizes
				
				ObjectAnimator animation;
				if(direction == Location.LEFT || direction == Location.RIGHT) {
					distance *= 145;
					animation = ObjectAnimator.ofFloat(movedButton, View.TRANSLATION_X, distance);
				}
				else {
					distance *= 110;
					animation = ObjectAnimator.ofFloat(movedButton, View.TRANSLATION_Y, distance);
				}
				
				// Time in milliseconds to move the tile
				animation.setDuration(speed);
				
				// Add the new animation to the list
				translateAnimations.add(animation);
			}
		}
		
		translateAnimations.get(0).addListener(new AnimatorListener(){
			
			@Override
			public void onAnimationEnd(Animator animation) {
				
				game.newTurn();
				updateGame();
				addTile();
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
		
		// Move all of the tiles
		for(ObjectAnimator animation: translateAnimations)
			animation.start();
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
		// TextView timeTextView = (TextView) findViewById(R.id.time_textview);
		
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
		
		if(game.lost())
			Toast.makeText(getApplicationContext(), getString(R.string.you_lose), Toast.LENGTH_SHORT).show();
	}
	
	/**
	 * Create the game board 
	 * Precondition: the grid does not already exist
	 */
	private void createGrid() {
		
		GridLayout gridLayout = (GridLayout) findViewById(R.id.grid_layout);
		
		gridLayout.setRowCount(game.getGrid().getNumRows());
		gridLayout.setColumnCount(game.getGrid().getNumCols());

		Button button;
		Spec specRow, specCol;
		GridLayout.LayoutParams gridLayoutParam;
		int tile;

		for(int row = 0; row < gridLayout.getRowCount(); row++) {
			for(int col = 0; col < gridLayout.getColumnCount(); col++) {
				specRow = GridLayout.spec(row, 1); 
				specCol = GridLayout.spec(col, 1);
				gridLayoutParam = new GridLayout.LayoutParams(specRow, specCol);
				
				button = new Button(this);
				button.setId(row * 100 + col);
				
				button.setTextSize(30);

				tile = game.getGrid().get(new Location(row, col));

				if(tile == 0)
					button.setVisibility(View.INVISIBLE);
				else 
					button.setVisibility(View.VISIBLE);
				
				gridLayout.addView(button,gridLayoutParam);
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

		Button button;
		Spec specRow, specCol;
		GridLayout.LayoutParams gridLayoutParam;
		int tile;
		String expectedValue, actualValue;

		for(int row = 0; row < gridLayout.getRowCount(); row++) {
			for(int col = 0; col < gridLayout.getColumnCount(); col++) {
				specRow = GridLayout.spec(row, 1); 
				specCol = GridLayout.spec(col, 1);
				gridLayoutParam = new GridLayout.LayoutParams(specRow, specCol);

				button = (Button) findViewById(row * 100 + col);
				tile = game.getGrid().get(new Location(row,col));
				
				expectedValue = convertToTileText(tile);
				actualValue = button.getText().toString();
				
				// A tile is given a tag when it changes position
				if(button.getTag() != null ||
						// If the value of the button does not match the game
						(! expectedValue.equals(actualValue))) {
					
					ViewGroup layout = (ViewGroup) button.getParent();
					if(null!=layout)
						layout.removeView(button);

					button = new Button(this);
					button.setId(row * 100 + col);
					button.setTextSize(30);

					tile = game.getGrid().get(new Location(row, col));

					if(tile == 0)
						button.setVisibility(View.INVISIBLE);
					else {
						button.setText(convertToTileText(tile));
						button.setVisibility(View.VISIBLE);
					}
					
					button.setTag(null);

					gridLayout.addView(button,gridLayoutParam);
				}
			}
		}
	}
	
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

	private void addTile() {
		
		Location loc = game.addRandomPiece();
		int row = loc.getRow();
		int col = loc.getCol();
		
		Button newTile = (Button) findViewById(row * 100 + col);
		
		int tile = game.getGrid().get(new Location(row, col));
		newTile.setText(convertToTileText(tile));
		
		// Immediately make the button invisible
		ObjectAnimator.ofFloat(newTile, View.ALPHA, 0).setDuration(0).start();
		
		// Make the button visible. It still cannot be seen
		newTile.setVisibility(View.VISIBLE);
		
		// Fade the button in
		ObjectAnimator.ofFloat(newTile, View.ALPHA, 1).setDuration(NEW_TILE_SPEED).start();
	}
	
	private void removeLowTiles() {
		animationInProgress = true;
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		
		// Save the game history before each move
		game.saveGameInHistory();
		
		Grid gameBoard = game.getGrid();
		
		// Get a list of all tiles
		List<Location> tiles = game.getGrid().getFilledLocations();
		
		// An list of the fade animations to play
		ArrayList<ObjectAnimator> alphaAnimations = new ArrayList<ObjectAnimator>();
		
		// An list of the buttons to remove
		ArrayList<Button> toRemoveList = new ArrayList<Button>();
				
		
		
		// Loop through each tile
		for(Location tile : tiles) {
			if(gameBoard.get(tile) == 2 || gameBoard.get(tile) == 4) {
				
				Button toRemove = (Button) findViewById(tile.getRow() * 100 + tile.getCol());
				toRemove.setTag("remove low tiles");
				toRemoveList.add(toRemove);
				
				
				// Determine the distance to move in pixels
				// On my device each column is 145 pixels apart and each row is 110
				// TODO: Change this to support different screen sizes
				
				alphaAnimations.add(ObjectAnimator.ofFloat(toRemove, View.ALPHA, 0)
						.setDuration(NEW_TILE_SPEED));
				
			}
		}
		
		alphaAnimations.get(0).addListener(new AnimatorListener(){
			
			@Override
			public void onAnimationEnd(Animator animation) {
				
				game.removeLowTiles();
				game.newTurn();
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
		
		// Move all of the tiles
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
		//history.push(game.getGrid().clone(), game.getScore());
		
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
				updateGame();
			}
		});
		
		rotateAnimation.start();
	}
	
	private void undo() {
		if(game.getUndosRemaining() != 0) {
			//game.setGrid(history.popBoard());
			//game.setScore(history.popScore());
			
			game.undo();
			
			updateGame();
		}
	}
	
	
	private void saveGame() {
		
		File file = new File(getFilesDir(), getString(R.string.file_current_game));

		try {
			Save.saveGame(game, file);
		} catch (IOException e) {
			e.printStackTrace();
			Toast.makeText(getApplicationContext(), "Error: Save file not found", Toast.LENGTH_SHORT).show();
		}
		
	}
	
	private void loadGame() {
		
		File file = new File(getFilesDir(), getString(R.string.file_current_game));
		try {
			game = Save.loadGame(file);
		} catch (ClassNotFoundException e) {
			game = new Game();
		} catch (IOException e) {
			game = new Game();
		}
		
		updateGame();
	}
	
	
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
			
			return rootView;
		}
	}
	
	@Override 
    public boolean onTouchEvent(MotionEvent event){ 
        this.mDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }
	
     // When the screen is swiped
    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2, 
            float velocityX, float velocityY) {
        
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