package com.tytanapps.game2048;
/**
 * @author Tyler Carberry
 * 2048
 * The main code of the game 2048
 */

import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Game implements java.io.Serializable
{
	private static final long serialVersionUID = 3356339029021499348L;
	
	private final static String LOG_TAG = Game.class.getSimpleName();

    // The chance of a 2 appearing
    public static final double CHANCE_OF_2 = .90;

    public static final int GHOST_TILE_VALUE = -3;
    public static final int X_TILE_VALUE = -2;
	public static final int CORNER_TILE_VALUE = -1;
	
	public static final int ICE_ATTACK = 1;
	public static final int X_ATTACK = 2;
	public static final int GHOST_ATTACK = 3;

    public static enum Mode {XMODE, CORNER, ARCADE, SURVIVAL, SPEED, RUSH, GHOST}
	
	// The main board the game is played on
	private Grid board;
	
	// Stores the previous boards and scores
	private Stack history;

    private Game originalGame;
	
	private int score = 0;
	private int turnNumber = 1;
	
	// If the game was quit
	private boolean quitGame = false;
	
	// Used to start the time limit on the first move
	// instead of when the game is created
	private boolean newGame = true;
	
	// The time the game was started
	private Date d1;
	
	// Limited number of moves or undos, -1 = unlimited
	private int movesRemaining = -1;
	private int undosRemaining = -1;
	private int powerupsRemaining = -1;

    private int undosUsed = 0;
    private int powerupsUsed = 0;

    // Used to increment the event total tiles combined
    private int tilesCombined = 0;

    // The time limit in seconds before the game automatically quits
	// The timer starts immediately after the first move
	private double timeLeft = -1;

    private Set<Mode> activeModes = new LinkedHashSet<>();
	
	// If true, any tile less than the max tile can spawn
	// Ex. If the highest piece is 32 then a 2,4,8, or 16 can appear
	// All possible tiles have an equal chance of appearing
	private boolean dynamicTileSpawning = false;

    // When two tiles combine their location is stored. New tiles will not move to
	// that location. This prevents tiles from double combining. 
	// (When 4|4|8|0 is shifted left it will form two 8's instead of a 16)
	private ArrayList<Location> destinationLocations = new ArrayList<Location>();

    private int activeAttack = 0;
	private int attackDuration = -1;
    private int iceDirection = -1;
	
	private int gameModeId;

    private boolean useItemInventory = false;
    private boolean genieEnabled = false;

    // This must be updated manually by the driver
    private int opponentScore = -1;
	
	/**
	 * Creates a default game with the size 4x4
	 */
	public Game()
	{
		this(4, 4);
	}
	
	/**
     * Call finishedCreatingGame() before starting the game
	 * @param rows The number of rows in the game
	 * @param cols The number of columns in the game
	 */
	public Game(int rows, int cols) {
		// The main board the game is played on
		board = new Grid(rows,cols);
		
		// Keeps track of the turn number
		turnNumber = 1;
		
		// Store the move history
		history = new Stack();
	}

    /**
     * Needs to be called after the game is created
     */
    public void finishedCreatingGame() {
        originalGame = clone();

        // Adds 2 pieces to the board
        addRandomPiece();
        addRandomPiece();
    }
	
	/**
	 * Moves the entire board in the given direction
	 * @param direction Called using a final variable in the location class
	*/
	public void act(int direction) {
		// Don't move if the game is already lost or quit
		if(isGameLost())
			return;
		
		// If this is the game's first move, keep track of
		// the starting time and activate the time limit
		if(newGame)
			madeFirstMove();
		
		// Used to determine if any pieces moved
		Grid lastBoard = board.clone();
				
		// If moving up or left start at location 0,0 and move right and down
		// If moving right or down start at the bottom right and move left and up
		List<Location> locations = board.getLocationsInTraverseOrder(direction);
		
		// Move each piece in the direction
		for(Location loc : locations)
			move(loc, direction);
		
		// If no pieces moved then it was not a valid move
		if(! board.equals(lastBoard))
		{
			turnNumber++;
			addRandomPiece();
			history.push(lastBoard, score);
			movesRemaining--;
		}
	}
	
	/** 
	 * Move a single piece all of the way in a given direction
	 * Will combine with a piece of the same value
	 * @param from The location of he piece to move
	 * @param direction Called using a final variable in the location class
	 * @return The number of tiles moved
	 */
	public int move(Location from, int direction) {
		int distance = 0;
		
		// Do not move X spaces or 0 spaces
		if(board.get(from) != CORNER_TILE_VALUE && board.get(from) != 0)
		{	
			Location to = from.getAdjacent(direction);
			while(board.isValid(to))
			{
				// If the new position is empty, move
				if(board.isEmpty(to))
				{
					distance++;
					
					board.move(from, to);
					from = to.clone();
					to = to.getAdjacent(direction);
				}
				
				// If the new position has a piece
				else
				{
					// If they have the same value or if zenMode is enabled, combine
					if(!destinationLocations.contains(to) && (board.get(from) == board.get(to)))
					{
						distance++;
						add(from, to);
					}
					
					return distance;
				}
			}
		}
		return distance;
	}
	
	
	/**
	 * Adds piece "from" into piece "to", 4 4 -> 0 8
	 * Precondition: from and to are valid locations with equal values
	 * @param from The piece to move
	 * @param to The destination of the piece
	*/
	private void add(Location from, Location to) {
		if(activeModes.contains(Mode.SURVIVAL) && board.get(from) >= 8)
			timeLeft += board.get(from) / 4;
		
		score += board.get(to) + board.get(from);
		board.set(to, board.get(to) + board.get(from));
		board.set(from, 0);
		
		// If two pieces combined into a tile another piece cannot
		// move there that turn
		destinationLocations.add(to);

        tilesCombined++;
	}
	
	public void newTurn() {
		turnNumber++;
		//history.push(lastBoard, score);

		if(movesRemaining > 0)
			movesRemaining--;
		
		// Clear the tiles to not combine into
		destinationLocations.clear();
		
		if(attackDuration > 0)
			attackDuration--;
		
		if(attackDuration == 0) {
			activeAttack = 0;
			iceDirection = -1;
		}
	}
	
	/** 
	 * Stores the starting game time and activates the time limit after
	 * the first move instead of when the game is created
	 */
	private void madeFirstMove() {
		d1 = new Date();
		newGame = false;
	}
	
	/**
	 * Undo the game 1 turn 
	 * Uses a stack to store previous moves
	 */
	public void undo() {
		if(turnNumber > 1 && undosRemaining != 0)
		{
			// Undo the score, board, and turn #
			score = history.popScore();
			board = history.popBoard();
			turnNumber--;
            undosUsed++;

			// Use up one of the undos allowed
			if(undosRemaining > 0)
				undosRemaining--;
		}
	}
	
	/**
	 * Shuffle the board
	 */
	public void shuffle() {
		// If this is the game's first move, keep track of
		// the starting time and activate the time limit
		if(newGame)
			madeFirstMove();
		
		// Adds every piece > 0 to a linked list
		LinkedList<Integer> pieces = new LinkedList<Integer>();
		int num;
		
		for(int row = 0; row < board.getNumRows(); row++)
			for(int col = 0; col < board.getNumCols(); col++)
			{
				num = board.get(new Location(row, col));
				if(num > 0)
				{
					pieces.add(num);
					
					// Remove the piece from the board
					// This is used instead of board.clear() to prevent
					// the X's from disappearing in corner mode
					board.set(new Location(row,col), 0);
				}
			}
		
		List<Location> empty;
		
		// Adds every piece to a random empty location
		for(int piece : pieces)
		{
			empty = board.getEmptyLocations();
			board.set(empty.get((int) (Math.random() * empty.size())), piece);
		}
		
		turnNumber++;
	}
	
	public void saveGameInHistory() {
		history.push(board.clone(), score);
	}

    public void saveLastGameToHistory(Game game) {
        history.push(game.getGrid().clone(), game.getScore());
    }
	
	
	/**
	 * Remove all 2's and 4's from the board
	 */
	public void removeLowTiles() {
		List<Location> filledTiles = board.getFilledLocations();
		
		// Remove all low tiles (2 and 4) from the board
		for(Location tile : filledTiles) {
			int tileValue = board.get(tile);
				if(tileValue <= 4 && tileValue > 0)
					board.set(tile, 0);
		}
		
		// Don't count the immovable tiles in corner mode
		// towards the number of tiles on the board
		int moveableTiles = 0;
		filledTiles = board.getFilledLocations();
		for(Location tile : filledTiles) {
			int tileValue = board.get(tile);
				if(tileValue != -1)
					moveableTiles++;
		}
		
		// There are always at least 2 pieces on the board
		while(moveableTiles < 2) {
			addRandomPiece();
			moveableTiles++;
		}
	}
	
	/**
	 * Remove the piece from the given location
	 * @param loc The location to remove
	 */
	public void removeTile(Location loc)
	{
		board.set(loc, 0);
	}
	
	/**
	 * Stop the game automatically after a time limit
	 * @param seconds The time limit in seconds
	 */
	public void setTimeLimit(double seconds) {
		if(seconds > 0)
			timeLeft = seconds;
	}
	
	/**
	 * @return the time left in the game
	 */
	public double getTimeLeft()
	{
		return timeLeft;
	}
	
	/**
	 * Places immovable X's in the corners of the board
	 * This will bump existing pieces in the corners of the board
	 * to random free locations
	 */
	public void enableCornerMode() {
		int previousValue;
		
		previousValue = board.set(new Location(0,0), CORNER_TILE_VALUE);
		if(previousValue != 0)
			addRandomPiece(previousValue);
			
		previousValue = board.set(new Location(0,board.getNumCols() - 1), CORNER_TILE_VALUE);
		if(previousValue != 0)
			addRandomPiece(previousValue);
		
		previousValue = board.set(new Location(board.getNumRows() - 1,0), CORNER_TILE_VALUE);
		if(previousValue != 0)
			addRandomPiece(previousValue);
		
		previousValue = board.set(new Location(board.getNumRows() - 1 ,board.getNumCols() - 1), CORNER_TILE_VALUE);
		if(previousValue != 0)
			addRandomPiece(previousValue);
	}
	
	/**
	 * Places an X on the board that can move but not combine 
	 */
	public void enableXMode() {
		List<Location> empty = board.getEmptyLocations();

		if(empty.isEmpty())
			System.err.println("Can not start XMode. The board is filled");
		else
		{
			int randomLoc = (int) (Math.random() * empty.size());
			board.set(empty.get(randomLoc), X_TILE_VALUE);
		}
	}

	
	/**
	 *  The game increases the time limit when tiles >= 8 combine
	 */
	public void enableSurvivalMode() {
        activeModes.add(Mode.SURVIVAL);
		
		// If no time limit is in effect, set it to 15 seconds
		if(timeLeft <= 0)
			timeLeft = GameModes.SURVIVAL_MODE_TIME;
	}
	
	public void setArcadeMode(boolean enabled) {
		if(enabled)
            activeModes.add(Mode.ARCADE);
        else
            activeModes.remove(Mode.ARCADE);
	}

    /*
	public void setZenMode(boolean enabled) {
		zenMode = enabled;
		setDynamicTileSpawning(enabled);
	}
	*/

    public void setGhostMode(boolean enabled) {
        if(enabled)
            activeModes.add(Mode.GHOST);
        else
            activeModes.remove(Mode.GHOST);
    }

    public boolean getGhostMode() {
        return activeModes.contains(Mode.GHOST);
    }
	
	/**
	 * Higher value tiles appear
	 */
	public void setDynamicTileSpawning(boolean enabled)
	{
		dynamicTileSpawning = enabled;
	}
	
	/**
	 * Add a piece automatically every 2 seconds even if no move was made
	 * @param enabled Turn speed mode on or off
	 */
	public void setSpeedMode(boolean enabled) {
        if(enabled)
            activeModes.add(Mode.SPEED);
        else
            activeModes.remove(Mode.SPEED);
	}
	
	public boolean getSurvivalMode() {
		return activeModes.contains(Mode.SURVIVAL);
	}
	
	public boolean getSpeedMode() {
        return activeModes.contains(Mode.SPEED);
	}

    /*
	public boolean getZenMode() {
		return activeModes.contains(Mode.ZEN);
	}
	*/
	
	public boolean getArcadeMode() {
		return activeModes.contains(Mode.ARCADE);
	}
	
	/**
	 * All tiles appear as ?
	 * @param SECONDS The time to hide the values. -1 for unlimited
	 */
	public void hideTileValues(final int SECONDS)
	{
		board.hideTileValues(true);
		
		if(SECONDS >= 0)
		{
			// Create a new thread to show the tiles
			final Thread T = new Thread() {
				public void run()
				{
					try
					{
						// Pause the thread for x milliseconds
						// The game continues to run
						Thread.sleep((long) (SECONDS * 1000.0));
					}
					catch (Exception e)
					{
						System.out.println(e.getMessage());
						e.printStackTrace();
					}
					
					// Unhide the tiles after the time limit
					board.hideTileValues(false);
					printGame();
				}
			}; // end thread

			T.start();
		}
	}
	
	/**
	 * Check if moving in the given direction might cause the game to lose
	 * @param direction The direction to check. A static int from Location class
	 * @return If the game will be lost moving in that direction
	 */
	public boolean causeGameToLose(int direction) {
		if(board.getEmptyLocations().size() > 1)
			return false;
		
		Game nextGame;
		List<Integer> possibleTiles = getPossibleTilesToAdd();
		
		// Check for every tile that might appear
		for(int tile : possibleTiles) {

			nextGame = clone();
			
			List<Location> locations = board.getLocationsInTraverseOrder(direction);
			// Move each piece in the direction
			for(Location loc : locations)
				nextGame.move(loc, direction);

			nextGame.addRandomPiece(tile);
			
			// There is a chance that the game might lose
			if(nextGame.isGameLost())
				return true;
		}
		// The game can move there without losing
		return false;
	}

	/**
	 * You can not move in a random direction for a random amount of time
     * (currently between 3 and 10 moves)
     *
     * If an ice attack is currently active additional time is added to the attack
     * If another attack is active it is cleared
	 */
	public void ice() {
        if(activeAttack == ICE_ATTACK)
            attackDuration += (int) (Math.random() * 7 + 3);
        else {
            attackDuration = (int) (Math.random() * 7 + 3);
            activeAttack = ICE_ATTACK;

            double randomDirection = Math.random();
            if (randomDirection < .5)
                if (randomDirection < .25)
                    iceDirection = Location.UP;
                else
                    iceDirection = Location.DOWN;
            else if (randomDirection < .75)
                iceDirection = Location.LEFT;
            else
                iceDirection = Location.RIGHT;
        }
	}
	
	public int getIceDirection() {
		return iceDirection;
	}
	
	/**
	 * Temporarily add an XTile to the board
	 * @return The location where it was added
	 */
	public Location XTileAttack() {
        if(activeAttack == X_ATTACK) {
            attackDuration += (int) (Math.random() * 6 + 5);
            for(Location loc : board.getFilledLocations())
                if(board.get(loc) == X_TILE_VALUE)
                    return loc;
            return null;
        }
        else {
            attackDuration = (int) (Math.random() * 6 + 5);
            activeAttack = X_ATTACK;
            return addRandomPiece(X_TILE_VALUE);
        }
	}
	
	public Location endXTileAttack() {
		Location XTile = board.find(X_TILE_VALUE);
		
		if(XTile != null)
			board.set(XTile, 0);
		
		return XTile;
	}
	
	/**
	 * Enable ghost attack for between 5 and 10 turns
	 */
	public void ghostAttack() {
        if(activeAttack == GHOST_ATTACK)
            attackDuration += (int) (Math.random() * 6 + 5);
        else {
            attackDuration = (int) (Math.random() * 6 + 5);
            activeAttack = GHOST_ATTACK;
        }
	}
	
	public int getAttackDuration() {
		return attackDuration;
	}
	
	/**
	 *  Limit the number of undos
	 * -1 = unlimited
	 * @param limit The new limit of undos
	 * This overrides the previous limit, does not add to it
	 */
	public void setUndoLimit(int limit)
	{
		undosRemaining = limit;
	}
	
	/**
	 * @return The number of undos left
	 * -1 = unlimited
	 */
	public int getUndosRemaining() {
		return undosRemaining;
	}
	
	public void incrementUndosRemaining() {
		undosRemaining++;
	}
	
	/**
	 * Limit the number of moves
	 * -1 = unlimited
	 * @param limit The new limit of moves
	 * This overrides the previous limit, does not add to it
	 */
	public void setMoveLimit(int limit)
	{
		movesRemaining = limit;
	}
	
	/**
	 * @return The number of moves left
	 * -1 = unlimited
	 */
	public int getMovesRemaining()
	{
		return movesRemaining;
	}
	
	/**
	 *  Limit the number of powerups
	 * -1 = unlimited
	 * @param limit The new limit of powerups
	 * This overrides the previous limit, does not add to it
	 */
	public void setPowerupLimit(int limit)
	{
		powerupsRemaining = limit;
	}
	
	/**
	 * @return The number of powerups left
	 * -1 = unlimited
	 */
	public int getPowerupsRemaining()
	{
		return powerupsRemaining;
	}
	
	/**
	 * Decrement the number of powerups remaining 
	 * @return The new amount remaining.
	 */
	public int decrementPowerupsRemaining() {
		powerupsUsed++;
        if(powerupsRemaining > 0)
			powerupsRemaining--;
		return powerupsRemaining;
	}
	
	/**
	 * Increase the amount of powerups remaining by 1
	 */
	public void incrementPowerupsRemaining() {
		powerupsRemaining++;
	}

    public int getUndosUsed() {
        return undosUsed;
    }

    public int getPowerupsUsed() {
        return powerupsUsed;
    }

    public int getTilesCombined() {
        return tilesCombined;
    }

    public void resetTilesCombined() {
        tilesCombined = 0;
    }

	/**
	 * 
	 * @return A list of the possible tiles to be added to the board
	 */
	private List<Integer> getPossibleTilesToAdd() {
		
		// All powers of 2 less that the highest tile
		ArrayList<Integer> possibleTiles = new ArrayList<Integer>();
		possibleTiles.add(2);
		possibleTiles.add(4);

		// See addRandomPiece() for description of dynamicTileSpawning
		if(dynamicTileSpawning) {
			// The highest tile on the board
			int highest = highestPiece();

			// Add each possible value to possibleTiles
			for(int t = 8; t < highest; t *= 2)
				possibleTiles.add(t);
		}
		return possibleTiles;
	}
	
	/**
	 * Randomly adds a new piece to an empty space
	 * 90% add 2, 10% add 4
	 * CHANCE_OF_2 is a final variable declared at the top
	 * 
	 * If dynamicTileSpawing is true, any tile less than the max tile can spawn
	 * Ex. If the highest piece is 32 then a 2,4,8, or 16 can appear
	 * All possible tiles have an equal chance of appearing
	 */
	public Location addRandomPiece() {
        //if(gameModeId == GameModes.CRAZY_MODE_ID)
        //    return addTitleModePiece();

        if(dynamicTileSpawning) {
            List<Integer> possibleTiles = getPossibleTilesToAdd();
            int random = (int) (Math.random() * possibleTiles.size());
            return addRandomPiece(possibleTiles.get(random));
        }
        else
            return addRandomPiece((Math.random() < CHANCE_OF_2) ? 2 : 4);
	}
	
	/**
	 * Adds a specified tile to the board in a random location
	 * @param tile The number tile to add
	 */
	private Location addRandomPiece(int tile) {
		// A list of the empty spaces on the board
		List<Location> empty = board.getEmptyLocations();

		// If there are no empty pieces on the board don't do anything
		if(! empty.isEmpty())
		{
			int randomLoc = (int) (Math.random() * empty.size());
			Location loc = empty.get(randomLoc);
			board.set(loc, tile);
			return loc;
		}
		return null;
	}
	
	/**
	 * @return Whether or not the game is won
	 * A game is won if there is a 2048 tile or greater
	 */
	public boolean hasUserWon()
	{
		return hasUserWon(2048);
	}
	
	/**
	 * @param winningTile The target tile
	 * @return If a tile is >= winningTile
	 */
	public boolean hasUserWon(int winningTile) {
		Location loc;
		for(int col = 0; col < board.getNumCols(); col++)
		{
			for(int row = 0; row < board.getNumRows(); row++)
			{
				loc = new Location(row, col);
				if(board.get(loc) >= winningTile)
					return true;
			}
		}
		return false;
	}
	
	/**
	 * @return If the game is lost
	 */
	public boolean isGameLost() {
		// If the game is quit then the game is lost
		if(quitGame || movesRemaining == 0)
			return true;
		
		
		if(attackDuration > 0)
			return ! (canMove(Location.UP) || canMove(Location.DOWN) ||
					canMove(Location.LEFT) || canMove(Location.RIGHT)); 	

		// If the board is not filled then the game is not lost
		if(!board.getEmptyLocations().isEmpty())
			return false;
		
		int current = -5;
		int next;
		
		// Check if two of the same number are next to each
		// other in a row.
		for(int row = 0; row < board.getNumRows(); row++)
		{
			for(int col = 0; col < board.getNumCols(); col++)
			{
				next = current;
				current = board.get(new Location(row,col));
				
				if(current == next)
					return false;
			}
			current = -5;
		}
		
		// Check if two of the same number are next to each
		// other in a column.
		for(int col = 0; col < board.getNumCols(); col++)
		{
			for(int row = 0; row < board.getNumRows(); row++)
			{
				next = current;
				current = board.get(new Location(row,col));
				
				if(current == next)
					return false;
			}
			current = -5;
		}
		return true;
	}
	
	/**
	 * @return the number of seconds the game was played for
	 */
	public double timePlayed() {
		// If no move has been made yet
		if(d1 == null)
			return 0;
		
		return (new Date().getTime() - d1.getTime()) / 1000.0;
	}
	
	/**
	 *  Quit the game
	 */
	public void quit() {
        quitGame = true;
    }

    public Game getOriginalGame() {
        return originalGame;
    }

	/**
	 * @return The highest piece on the board
	 */
	public int highestPiece() {
		int highest = 0;
		for(int col = 0; col < board.getNumCols(); col++)
			for(int row = 0; row < board.getNumRows(); row++)
			{
				if(board.get(new Location(row, col)) > highest)
					highest = board.get(new Location(row, col));
			}
		
		return highest;
	}
	
	/**
	 * @param otherGame The other game to check
	 * @return If the games are equal
	 * Games are equal if they have the same board and score, 
	 * even if their history is different.
	 */
	public boolean equals(Game otherGame) {
		return board.equals(otherGame.getGrid()) && score == otherGame.getScore();
	}
	
	/**
	 * Used to avoid creating aliases 
	 * @return A clone of the game
	 */
	public Game clone()
	{
        Game clonedGame = new Game();
        clonedGame.setGrid(getGrid().clone());
        clonedGame.turnNumber = turnNumber;
        clonedGame.score = score;
        clonedGame.history = history.clone();

        clonedGame.movesRemaining = movesRemaining;
        clonedGame.undosRemaining = undosRemaining;
        clonedGame.timeLeft = timeLeft;
        clonedGame.d1 = d1;
        clonedGame.quitGame = quitGame;
        clonedGame.newGame = newGame;

        clonedGame.activeModes = new LinkedHashSet<Mode>(activeModes);

        clonedGame.activeAttack = activeAttack;
        clonedGame.attackDuration = attackDuration;
        clonedGame.iceDirection = iceDirection;
        clonedGame.dynamicTileSpawning = dynamicTileSpawning;

        clonedGame.undosUsed = undosUsed;
        clonedGame.powerupsUsed = powerupsUsed;
        clonedGame.gameModeId = gameModeId;
        clonedGame.useItemInventory = useItemInventory;
        clonedGame.genieEnabled = genieEnabled;

        return clonedGame;
	}

	/**
	 * @param direction Called using the final variables in the location class
	 * @return If the game can move in the given direction
	 */
	public boolean canMove(int direction) {
		if(attackDuration > 0 && iceDirection == direction)
			return false;
		
		Game nextMove = clone();
		
		// Infinite recursion is caused if the game has 
		// an ice attack active. The equals method only checks
		// the grid and score so this will not affect the result
		nextMove.attackDuration = -1;
		
		nextMove.act(direction);
		
		return !(nextMove.equals(this));
	}

    private Location addTitleModePiece() {
        Log.d("a", "Entering addTitleModePiece");

        Location loc = GameModes.getTitleModeLocation(turnNumber);
        board.set(loc, GameModes.getTitleValue(turnNumber));
        return loc;
    }
	
	/**
	 * @return The score of the game
	 */
	public int getScore()
	{
		return score;
	}
	
	public void setScore(int newScore)
	{
		score = newScore;
	}
	
	/**
	 * @return The current turn number of the game
	 */
	public int getTurns()
	{
		return turnNumber;
	}
	
	/**
	 * @return The grid of the game
	 */
	public Grid getGrid()
	{
		return board;
	}
	
	public void setGrid(Grid newBoard) {
		board = newBoard;
	}

    public boolean getGenieEnabled() {
        return genieEnabled;
    }

    public void setGenieEnabled(boolean isGenieEnabled) {
        genieEnabled = isGenieEnabled;
    }

    /**
	 * Changing this value does not affect the game. It is only for reference.
	 * Use the methods such as enableCornerMode() or enableXMode() in addition
	 * @param mode The game mode. Should use the GameModes class
	 */
	public void setGameModeId(int mode) {
		gameModeId = mode;
	}
	
	public int getGameModeId() {
		return gameModeId;
	}

    public int setOpponentScore(int newOpponentScore) {
        int oldOpponentScore = opponentScore;
        opponentScore = newOpponentScore;
        return oldOpponentScore;
    }

    public int getOpponentScore() {
        return opponentScore;
    }

    public void setUseItemInventory(boolean isEnabled) {
        useItemInventory = isEnabled;
    }

    public boolean getUseItemInventory() {
        return useItemInventory;
    }
	
	public int getActiveAttack() {
		return activeAttack;
	}

    public ArrayList<Location> getDestinationLocations() {
        return destinationLocations;
    }


    public static List<Integer> getListOfAllTileValues() {
        ArrayList<Integer> listOfTiles = new ArrayList<Integer>();

        for(int tile = 2; tile <= 2048; tile *= 2)
            listOfTiles.add(tile);

        listOfTiles.add(Game.CORNER_TILE_VALUE);
        listOfTiles.add(Game.X_TILE_VALUE);
        listOfTiles.add(Game.GHOST_TILE_VALUE);

        return listOfTiles;
    }

	/**
	 * Only used in the hideTileValues and speedMode methods to print the game
	 */
	private void printGame()
	{
		System.out.println(toString());
	}
	
	/** @return a string of the game in the form:
	---------------------------------------------
	||  Turn #8  Score: 20  Moves Left: 3
	---------------------------------------------
	| 8  |    | 2  |    |
	| 4  |    |    |    |
	| 2  |    |    | 2  |
	|    |    |    |    |		*/
	public String toString() {
		String output = "---------------------------------------------\n";
		output += "||  Turn #" + turnNumber + "  Score: " + score + "\n";
		output += "||  Moves Left:";
		
		if(movesRemaining >= 0)
			output += movesRemaining;
		else
			output += "�";
		
		output += " Undos Left:";
		
		if(undosRemaining >= 0)
			output += undosRemaining;
		else
			output += "�";
		
		output += " Time Left:";
		
		if(timeLeft >= 0)
			output += timeLeft;
		else
			output += "�";
		
		
		output += "\n---------------------------------------------\n";
		output += board.toString();
		
		return output;
	}
}