package com.example.app_2048;

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
import android.content.Intent;
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

public class GameActivity extends Activity implements OnGestureListener {
	
	private static Game game;
	final static String LOG_TAG = GameActivity.class.getSimpleName();
	private GestureDetectorCompat mDetector; 
	private boolean madeFirstMove = false;
	private int turnNumber = 1;
	private static int undosLeft;
	Stack history;
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
        
        history = new Stack();
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
		if (id == R.id.action_settings) {
			return true;
		}
		if (id == R.id.grid_layout) {
			game.removeLowTiles();
			updateGame();
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onResume() {
		
		updateGame();
		
		/*
		if((!madeFirstMove) && game.getTurns() == 1)
		{
			if(game.getTimeLeft() > 0)
				createCountdownTimer();
			madeFirstMove = true;
		}
		*/
		
		super.onResume();
	}
	
	
	
	/**
	 * When a button is pressed to make the game act
	 * @param view The button that was pressed
	 */
	public void act(View view) {
		switch(view.getId()) {
		case R.id.undo_button:
			if((!history.isEmpty()) && undosLeft != 0)
				undo();
			break;
		case R.id.shuffle_button:
			
			// Save the game history before each move
			history.push(game.getGrid().clone(), game.getScore());
			
			shuffleGame();
			
			break;
		}
		
		updateGame();
	}
	
	/**
	 * Moves all of the tiles
	 * @param direction Should use the static variables in Location class
	 */
	public void act(int direction) {
		
		animationInProgress = true;
		
		// Save the game history before each move
		history.push(game.getGrid().clone(), game.getScore());
		
		// Get a list of all tiles
		List<Location> tiles = game.getGrid().getLocationsInTraverseOrder(direction);
		
		// An list of the move animations to play
		ArrayList<ObjectAnimator> translateAnimations = new ArrayList<ObjectAnimator>();
		
		// The grid that the tiles are on
		GridLayout gridLayout = (GridLayout) findViewById(R.id.grid_layout);
		
		// Loop through each tile
		for(Location tile : tiles) {
			// Determine the number of spaces to move
			int distance = game.move(tile, direction);
			
			// Only animate buttons that moved
			if(distance > 0) {
				
				if(direction == Location.LEFT || direction == Location.UP)
					distance *= -1;
				
				Button movedButton = (Button) findViewById(tile.getRow() * 100 + tile.getCol());
				
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
				animation.setDuration(300);
				
				// Add the new animation to the list
				translateAnimations.add(animation);
			}
		}
		
		if(translateAnimations.size() == 0) {
			animationInProgress = false;
			return;
		}
		
		translateAnimations.get(0).addListener(new AnimatorListener(){
			
			@Override
			public void onAnimationEnd(Animator animation) {
				turnNumber++;
				game.addRandomPiece();
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
		for(ObjectAnimator animation: translateAnimations)
			animation.start();
		
	}
	
	private void undo() {
		if(undosLeft != 0) {
			game.setGrid(history.popBoard());
			game.setScore(history.popScore());
			turnNumber--;

			if(undosLeft > 0)
				undosLeft--;

			updateGame();
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
		// TextView timeTextView = (TextView) findViewById(R.id.time_textview);
		
		// Update the turn number
		turnTextView.setText("Turn #" + turnNumber);
		
		// Update the score
		scoreTextView.setText("Score: " + game.getScore());
		
		// Update the undos left
		if(undosLeft >= 0)
			undosTextView.setText("Undos left: " + undosLeft);
		else
			undosTextView.setText("");
		
		// Update moves left
		int movesLeft = game.getMovesRemaining();
		if(movesLeft >= 0)
			movesTextView.setText("Moves left: " + movesLeft);
		else
			movesTextView.setText("");
		
		// Update the game board
		updateGrid();
		
		if(game.lost())
			Toast.makeText(getApplicationContext(), "YOU LOSE", Toast.LENGTH_SHORT).show();
	}

	/**
	 * Update the game board
	 */
	private void updateGrid() {
		
		GridLayout gridLayout = (GridLayout) findViewById(R.id.grid_layout);
		
		gridLayout.setRowCount(game.getGrid().getNumRows());
		gridLayout.setColumnCount(game.getGrid().getNumCols());

		Button button, checkIfExists;
		Spec specRow, specCol;
		GridLayout.LayoutParams gridLayoutParam;
		int tile;

		for(int row = 0; row < gridLayout.getRowCount(); row++) {
			for(int col = 0; col < gridLayout.getColumnCount(); col++) {
				specRow = GridLayout.spec(row, 1); 
				specCol = GridLayout.spec(col, 1);
				gridLayoutParam = new GridLayout.LayoutParams(specRow, specCol);
				
				checkIfExists = (Button) findViewById(row * 100 + col);

				// Remove the tile already there if there is one
				if(checkIfExists != null)
				{
					ViewGroup layout = (ViewGroup) checkIfExists.getParent();
					if(null!=layout)
						layout.removeView(checkIfExists);
				}

				button = new Button(this);
				button.setId(row * 100 + col);
				
				// Doesn't work?
				// button.setWidth(100);
				// button.setHeight(100);
				
				button.setTextSize(30);

				tile = game.getGrid().get(new Location(row, col));

				if(tile == 0)
					button.setVisibility(View.INVISIBLE);
				else {
					switch (tile) {
					case -1:
						button.setText("XX");
						break;
					case -2:
						button.setText("x");
						break;
					default:
						button.setText("" + tile);
					}
					button.setVisibility(View.VISIBLE);
				}

				gridLayout.addView(button,gridLayoutParam);

				/*
				button = (Button) findViewById(row * 100 + col);
				button.setWidth(100);
				button.setHeight(100);
				
				Log.d(LOG_TAG, "Height: " + button.getHeight());
				Log.d(LOG_TAG, "Width: " + button.getWidth());
				*/
				
			}
		}
	}

	/**
	 * Shuffles the game board and animates the grid
	 * The grid layout spins 360¡, the tiles are shuffled, then it spins
	 * back in the opposite direction
	 */
	private void shuffleGame() {
		GridLayout gridLayout = (GridLayout) findViewById(R.id.grid_layout);
		
		// Causes conflicts when the shuffle button is double tapped
		if(animationInProgress)
			return;
		
		ObjectAnimator rotateAnimation =
				ObjectAnimator.ofFloat(gridLayout, View.ROTATION, 360);
		rotateAnimation.setRepeatCount(1);
		rotateAnimation.setRepeatMode(ValueAnimator.REVERSE);
		
		// 300 ms should be fast enough to not notice the tiles changing
		rotateAnimation.setDuration(300);
		
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
				updateGrid();
			}
		});
		
		rotateAnimation.start();
		turnNumber++;
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
			
			Intent intent = getActivity().getIntent();
			
			if(intent != null) {
				int gameMode = intent.getIntExtra(MainActivity.GAME_LOCATION, 1);
				
				// Converts the id of the game mode selected to a game
				game = GameModes.newGameFromId(gameMode);
				
				undosLeft = game.getUndosRemaining();
			}
			else
				Log.d(LOG_TAG, "No intent passed");
			
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