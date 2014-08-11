package com.example.app_2048;

public class Statistics implements java.io.Serializable {
	
	private static final long serialVersionUID = -4114433717246666155L;
	
	public int totalGamesPlayed;
	public int totalUndosUsed;
	public int totalShufflesUsed;
	public int totalMoves;
	
	public int highScore;
	public int highestTile;
	public Game bestGame;
	
	public int lowScore = -1;
	public Game worstGame;
	

}
