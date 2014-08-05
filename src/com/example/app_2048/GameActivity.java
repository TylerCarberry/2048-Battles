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
		createGrid();
		updateGame();
		if((!madeFirstMove) && game.getTurns() == 1)
		{
			if(game.getTimeLeft() > 0)
				createCountdownTimer();
			madeFirstMove = true;
		}
		/*
		MediaPlayer mp= new MediaPlayer();
        mp = MediaPlayer.create(this, R.raw.jaracanda);
        
        if(mp == null) {            
            Log.v(LOG_TAG, "Create() on MediaPlayer failed.");       
        } else {
            mp.setOnCompletionListener(new OnCompletionListener() {

            @Override
              public void onCompletion(MediaPlayer mp) {
                  mp.stop();
                  mp.release();
              }
            });
            mp.start();
            Log.d(LOG_TAG, "successfully started");

        }
        */
		
		GridLayout v = (GridLayout) findViewById(R.id.grid_layout);
        
		super.onResume();
	}
	
	private void createGrid() {
		
		GridLayout v = (GridLayout) findViewById(R.id.grid_layout);
		v.setBackgroundColor(color.holo_green_light);
		
		v.setRowCount(game.getGrid().getNumRows());
		v.setColumnCount(game.getGrid().getNumCols());
		
		Button button, checkIfExists;
        Spec specRow, specCol;
        GridLayout.LayoutParams gridLayoutParam;
        String tile;
        
        for(int row = 0; row < v.getRowCount(); row++) {
        	for(int col = 0; col < v.getColumnCount(); col++) {
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
        		
        		if(game.getGrid().get(new Location(row, col)) == 0)
        			button.setVisibility(View.INVISIBLE);
        		
        		v.addView(button,gridLayoutParam);
        	}
        }
        
	}
	
	private void updateGrid() {
		
		GridLayout v = (GridLayout) findViewById(R.id.grid_layout);
		
		Button button;
        int tile;
        String buttonText;
        
        for(int row = 0; row < v.getRowCount(); row++) {
        	for(int col = 0; col < v.getColumnCount(); col++) {
        		button = (Button) findViewById(row * 100 + col);
        		
        		tile = game.getGrid().get(new Location(row, col));
        		
        		if (tile == 0)
        			button.setVisibility(View.INVISIBLE);
        		else
        			button.setVisibility(View.VISIBLE);
        			
        		
        		switch(tile) {
        		
        		case -1:
        			buttonText = "XX";
        			break;
        		case -2:
        			buttonText = "x";
        			break;
        		default:
        			buttonText = String.valueOf(tile);
        		}
        		
        		button.setText(buttonText);
        		
        	}
        }
	}
	
	/**
	 * When a button is pressed to make the game act
	 * @param view The button that was pressed
	 */
	public void act(View view) {
		switch(view.getId()) {
		case R.id.undo_button:
			if(game.getTurns() > 1)
				game.undo();
			break;
		case R.id.shuffle_button:
			game.shuffle();
			break;
		}
		
		updateGame();
	}
	
	public void act(int direction) {
		
		Log.d(LOG_TAG, "Entering act");
		
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
			
			Log.d(LOG_TAG, "Distance: " + distance);
			
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
		
		if(translateAnimations.size() == 0)
			return;
		
		translateAnimations.get(0).addListener(new AnimatorListener(){
			
			@Override
			public void onAnimationEnd(Animator animation) {
				turnNumber++;
				game.addRandomPiece();
				createGrid();
				updateGame();
			}
			
			@Override
			public void onAnimationStart(Animator animation) { }
			@Override
			public void onAnimationCancel(Animator animation) { }
			@Override
			public void onAnimationRepeat(Animator animation) { }
			
		});
		
		
		// Move all of the tiles
		for(ObjectAnimator animation: translateAnimations)
			animation.start();
		
	}
	
	public void addTile() {
		
	}
	
	
	/**
	 * Update the text views
	 */
	public void updateGame() {
		TextView gameTextView = (TextView) findViewById(R.id.game_textview);
		TextView turnTextView = (TextView) findViewById(R.id.turn_textview);
		TextView scoreTextView = (TextView) findViewById(R.id.score_textview);
		TextView undosTextView = (TextView) findViewById(R.id.undos_textview);
		TextView movesTextView = (TextView) findViewById(R.id.moves_textView);
		// TextView timeTextView = (TextView) findViewById(R.id.time_textview);
		
		gameTextView.setText(game.getGrid().toString());
		turnTextView.setText("Turn #" + turnNumber);
		scoreTextView.setText("Score: " + game.getScore());
		
		int undosLeft = game.getUndosRemaining();
		if(undosLeft >= 0)
			undosTextView.setText("Undos left: " + undosLeft);
		else
			undosTextView.setText("");
			
		int movesLeft = game.getMovesRemaining();
		if(movesLeft >= 0)
			movesTextView.setText("Moves left: " + movesLeft);
		else
			movesTextView.setText("");
		
		
		/*
		double timeLeft = game.getTimeLeft();
		if(timeLeft >= 0)
			timeTextView.setText("Time left: " + timeLeft);
		else
			timeTextView.setText("time unlimited");
		*/
		
		updateGrid();
		
		if(game.lost())
			Toast.makeText(getApplicationContext(), "YOU LOSE", Toast.LENGTH_SHORT).show();
	}
	
	public void createCountdownTimer() {
		Log.d(LOG_TAG, "create countdown timer");
		
		TextView timeLeftTextView = (TextView) findViewById(R.id.time_textview);
		
		Timer timeLeftTimer = new Timer(timeLeftTextView, (long) game.getTimeLeft() * 1000);
		
		timeLeftTimer.start();
		
	}
	
	
	
	
	/**
	 * Used to update the time left TextView
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
	 * A placeholder fragment containing a simple view.
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
				
			}
			else {
				Log.d(LOG_TAG, "No intent passed");
			}
			
			
			
			return rootView;
		}
		
		public void testAnimation(View view) {
			// Row 2 column 2 (Start at 0)
			Button translateButton = (Button) getView().findViewById(2 * 100 + 2);
			
			Assert.assertTrue(translateButton != null);
			
			ObjectAnimator translateAnimation =
					ObjectAnimator.ofFloat(translateButton, View.TRANSLATION_X, 200);
			
			setupAnimation(translateButton, translateAnimation);
		}

		private void setupAnimation(View view, final Animator animation) {

			view.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					animation.start();
				}
			});

		}
	}

	@Override 
    public boolean onTouchEvent(MotionEvent event){ 
        this.mDetector.onTouchEvent(event);
        // Be sure to call the superclass implementation
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent event) { 
        return true;
    }

    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2, 
            float velocityX, float velocityY) {
        
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
        return true;
    }

    @Override
    public void onLongPress(MotionEvent event) {}

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
            float distanceY) {
        return true;
    }

    @Override
    public void onShowPress(MotionEvent event) {}

    @Override
    public boolean onSingleTapUp(MotionEvent event) {
    	return true;
    }
}