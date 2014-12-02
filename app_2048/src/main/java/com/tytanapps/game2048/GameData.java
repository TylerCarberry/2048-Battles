//***********************************************************************
// Tyler Carberry
// GameData.java
// Stores information about the app that persists through each game
//***********************************************************************

package com.tytanapps.game2048;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameData implements java.io.Serializable {
	
	private static final long serialVersionUID = -4114433717246666155L;
	
	private int totalGamesPlayed;
	private int totalUndosUsed;
	private int totalShufflesUsed;
	private int totalMoves;

    private int powerupInventory;
    private int undoInventory;

    // Stores the highest and lowest scores of each mode
    private Map<Integer, Integer> highScores;
    private Map<Integer, Integer> lowScores;

    // Stores the game with the highest and lowest scores in each mode
    private Map<Integer, Game> bestGames;
    private Map<Integer, Game> worstGames;

    // Stores the highest tile of each mode
    private Map<Integer, Integer> highTiles;

    // Stores custom tile icons
    private Map<Integer, List<Byte>> customTileIcon = new HashMap<Integer, List<Byte>>();

    public GameData() {
        totalGamesPlayed = 0;
        totalUndosUsed = 0;
        totalShufflesUsed = 0;
        totalMoves = 0;
        powerupInventory = 0;
        undoInventory = 0;

        highScores = new HashMap<Integer, Integer>();
        lowScores = new HashMap<Integer, Integer>();

        bestGames = new HashMap<Integer, Game>();
        worstGames = new HashMap<Integer, Game>();

        highTiles = new HashMap<Integer, Integer>();
    }

    /**
     * Updates the high score, highest tile, and best game based on the current game
     * @param gameMode Should be a static variable in the game modes class
     * @param game The game to compare against the records
     */
    public void updateGameRecords(int gameMode, Game game) {

        // Update the high score for that mode
        if(!highScores.containsKey(gameMode) || game.getScore() > highScores.get(gameMode)) {
            highScores.put(gameMode, game.getScore());
            bestGames.put(gameMode, game);
        }
        // Update the lowest score for that mode
        else if(!lowScores.containsKey(gameMode) || game.getScore() < lowScores.get(gameMode)) {
            lowScores.put(gameMode, game.getScore());
            worstGames.put(gameMode, game);
        }

        // Update the highest tile for that mode
        if(!highTiles.containsKey(gameMode) || game.highestPiece() > highTiles.get(gameMode)) {
            highTiles.put(gameMode, game.highestPiece());
        }
    }

    public int getHighScore(int gameMode) {
        if(highScores.containsKey(gameMode))
            return highScores.get(gameMode);
        return 0;
    }
    public int getHighestTile(int gameMode) {
        if(highTiles.containsKey(gameMode))
            return highTiles.get(gameMode);
        return 0;
    }
    public int getLowestScore(int gameMode) {
        if(lowScores.containsKey(gameMode))
            return lowScores.get(gameMode);
        return 0;
    }

    public Bitmap getCustomTileIconBitmap(int tileValue) {
        if(customTileIcon.containsKey(tileValue)) {
            List<Byte> imageByteList = customTileIcon.get(tileValue);

            byte[] imageByteArray = new byte[imageByteList.size()];
            for(int i = 0; i < imageByteList.size(); i++)
                imageByteArray[i] = imageByteList.get(i).byteValue();

            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inPreferredConfig = Bitmap.Config.ARGB_8888;

            return BitmapFactory.decodeByteArray(imageByteArray, 0, imageByteArray.length, opt);
        }
        return null;
    }
    public void setCustomTileIcon(int tileValue, List iconByteArray) {
        customTileIcon.put(tileValue, iconByteArray);
    }

    public Game getWorstGame(int gameMode) {
        return worstGames.get(gameMode);
    }
    public Game getBestGame(int gameMode) {
        return bestGames.get(gameMode);
    }

    public void incrementGamesPlayed(int amount) {
        totalGamesPlayed += amount;
    }
    public void incrementUndosUsed(int amount) {
        totalUndosUsed += amount;
    }
    public void incrementShufflesUsed(int amount) {
        totalShufflesUsed += amount;
    }
    public void incrementTotalMoves(int amount) {
        totalMoves += amount;
    }

    public int getTotalGamesPlayed () {
        return totalGamesPlayed;
    }
    public int getTotalUndosUsed () {
        return totalUndosUsed;
    }
    public int getTotalShufflesUsed () {
        return totalShufflesUsed;
    }
    public int getTotalMoves () {
        return totalMoves;
    }

    /**
     * Increment the number of powerups by 1
     * @return The new number of powerups
     */
    public int incrementPowerupInventory() {
        return incrementPowerupInventory(1);
    }

    /**
     * Increment the number of powerups.
     * @param amount The amount to increment the powerups by
     * @return The new number of powerups
     */
    public int incrementPowerupInventory(int amount) {
        return powerupInventory += amount;
    }

    /**
     * Decrement the number of powerups by 1
     * If the number of powerups is < 0 the amount stays at 0.
     * @return The new number of powerups
     */
    public int decrementPowerupInventory() {
        if(powerupInventory > 0)
            powerupInventory--;
        return powerupInventory;
    }

    /**
     * @return The number of powerups in the players inventory
     */
    public int getPowerupInventory() {
        return powerupInventory;
    }

    /**
     * Increment the number of undos by 1
     * @return The new number of undos
     */
    public int incrementUndoInventory() {
        return incrementUndoInventory(1);
    }

    /**
     * Increment the number of undos.
     * @param amount The amount to increment the undos by
     * @return The new number of undos
     */
    public int incrementUndoInventory(int amount) {
        return undoInventory += amount;
    }

    /**
     * Decrement the number of undos by 1
     * If the number of undos is < 0 the amount stays at 0.
     * @return The new number of undos
     */
    public int decrementUndoInventory() {
        if(undoInventory > 0)
            undoInventory--;
        return undoInventory;
    }

    /**
     * @return The number of undos in the players inventory
     */
    public int getUndoInventory() {
        return undoInventory;
    }

}
