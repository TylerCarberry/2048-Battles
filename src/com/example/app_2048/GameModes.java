package com.example.app_2048;

public class GameModes
{
	public static final int LOAD_GAME_ID = 0;
	public static final int NORMAL_MODE_ID = 1;
	public static final int PRACTICE_MODE_ID = 2;
	public static final int PRO_MODE_ID = 3;
	public static final int RUSH_MODE_ID = 4;
	public static final int SURVIVAL_MODE_ID = 5;
	public static final int X_MODE_ID = 6;
	public static final int CORNER_MODE_ID = 7;
	public static final int SPEED_MODE_ID = 8;
	public static final int ZEN_MODE_ID = 9;
	public static final int CRAZY_MODE_ID = 10;
	
	
	public static Game newGameFromId(int id)
	{
		switch (id) {
		case NORMAL_MODE_ID:
			return normalMode();
		case PRACTICE_MODE_ID:
			return practiceMode();
		case PRO_MODE_ID:
			return proMode();
		case RUSH_MODE_ID:
			return rushMode();
		case SURVIVAL_MODE_ID:
			return survivalMode();
		case X_MODE_ID:
			return XMode();
		case CORNER_MODE_ID:
			return cornerMode();
		case SPEED_MODE_ID:
			return speedMode();
		case ZEN_MODE_ID:
			return zenMode();
		case CRAZY_MODE_ID:
			return crazyMode();
		default:
			return normalMode();
		}
	}
	
	
	// Practice Mode
	// Unlimited everything
	public static Game practiceMode()
	{
		Game game = new Game();
		game.setMoveLimit(-1);
		game.setUndoLimit(-1);
		game.setTimeLimit(-1);

		return game;
	}

	// Normal Mode
	// Unlimited moves and time
	// 10 undos
	public static Game normalMode()
	{
		Game game = new Game();
		game.setMoveLimit(-1);
		game.setUndoLimit(10);
		game.setTimeLimit(-1);

		return game;
	}

	// Pro Mode
	// Unlimited moves and time
	// No undos
	public static Game proMode()
	{
		Game game = new Game();
		game.setMoveLimit(-1);
		game.setUndoLimit(0);
		game.setTimeLimit(-1);

		return game;
	}

	// Rush Mode
	// Higher value tiles spawn
	public static Game rushMode()
	{
		Game game = new Game();
		game.dynamicTileSpawning(true);

		return game;
	}


	// Survival Mode
	// Unlimited moves and undos
	// Only 30 seconds to play. The time increases when tiles >= 8 combine
	public static Game survivalMode()
	{
		Game game = new Game();
		game.setMoveLimit(-1);
		game.setUndoLimit(-1);
		game.setTimeLimit(30);
		game.survivalMode();

		return game;
	}

	// XMode
	// Unlimited moves and time
	// 10 undos
	// Places an X on the board that can move but not combine 
	public static Game XMode()
	{
		Game game = new Game();
		game.setMoveLimit(-1);
		game.setUndoLimit(10);
		game.setTimeLimit(-1);
		game.XMode();

		return game;
	}

	// Corner Mode
	// Unlimited moves and time
	// 10 undos
	// Places immovable pieces in the corners of the board
	public static Game cornerMode()
	{
		Game game = new Game();
		game.setMoveLimit(-1);
		game.setUndoLimit(10);
		game.setTimeLimit(-1);
		game.cornerMode();

		return game;
	}

	// Speed Mode
	// Unlimited moves and time
	// 10 undos
	// Tiles appear every every 2 seconds even if no move was made
	public static Game speedMode()
	{
		Game game = new Game();
		game.setMoveLimit(-1);
		game.setUndoLimit(-1);
		game.setTimeLimit(-1);
		game.speedMode(true);

		return game;
	}

	// Zen Mode
	// Unlimited moves, undos and time
	// Every piece can combine
	public static Game zenMode()
	{
		Game game = new Game();
		game.setMoveLimit(-1);
		game.setUndoLimit(-1);
		game.setTimeLimit(-1);
		game.zenMode(true);

		return game;
	}

	// Crazy Mode
	// Unlimited moves and undos
	// A 5x5 game with every other mode enabled (except zen)
	public static Game crazyMode()
	{
		Game game = new Game(5,5);
		game.setMoveLimit(-1);
		game.setUndoLimit(-1);
		game.setTimeLimit(30);
		game.survivalMode();
		game.cornerMode();
		game.XMode();
		game.dynamicTileSpawning(true);
		game.speedMode(true);

		return game;
	}

}
