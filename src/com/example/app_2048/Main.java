//package com.example.app_2048;
///**
// * Tyler Carberry
// * The driver for the game 2048
// * This game is based off the original 2048 made by Gabriele Cirulli
// * at https://github.com/gabrielecirulli/2048
// */
//
//import java.io.FileNotFoundException;
//import java.io.IOException;
//import java.util.HashMap;
//import java.util.InputMismatchException;
//import java.util.Scanner;
//
////***************************************************************************
//// NOT USED IN APP, ONLY FOR REFERENCE
////***************************************************************************
//public class Main
//{
//	// All methods can use the scanner without declaring it each time
//	private static Scanner scan = new Scanner (System.in);
//	
//	// Enter the character that the user entered
//	// Get the direction back that they want to move
//	// Currently only works for up,right,down,left
//	private static HashMap<Character,Integer> keyMap = new HashMap<Character,Integer>();
//
//	public static void main(String[] args)
//	{	
//		loadKeyMap();
//		
//		int input = -1;
//		do
//		{
//			// Intro to game
//			System.out.println("|| --------------------------------------- ||");
//			System.out.println("||  Welcome to 2048 ||  By Tyler Carberry  ||");
//			System.out.println("|| --------------------------------------- ||");
//			System.out.println("||               How To Play               ||");
//			System.out.println("||  Combine similar numbers to reach 2048  ||");
//			System.out.println("|| --------------------------------------- ||");
//			System.out.println("||                Controls                 ||");
//			System.out.println("||       Left, Right, Up, Down, Quit       ||");
//			System.out.println("|| --------------------------------------- ||");
//			System.out.println("||    Type the first letter of your move   ||");
//			System.out.println("||        and press enter (l,r,u,d)        ||");
//			System.out.println("|| **** YOU CANNOT USE THE ARROW KEYS **** ||");
//			System.out.println("|| --------------------------------------- ||");
//			System.out.println("||                Power Ups                ||");
//			System.out.println("||         Shuffle, Undo, Autoplay,        ||");
//			System.out.println("||            Remove Low Tiles             ||");
//			System.out.println("||    Type the full word and press enter   ||");
//			System.out.println("|| --------------------------------------- ||");
//			System.out.println();
//			System.out.println("1. Practice Mode");
//			System.out.println("\tUnlimited Everything");
//			System.out.println("2. Normal Mode");
//			System.out.println("\t10 Undos");
//			System.out.println("3. Pro Mode");
//			System.out.println("\tNo Undos");
//			System.out.println("4. Rush Mode");
//			System.out.println("\tHigher value tiles will spawn");
//			System.out.println("5. Survival Mode");
//			System.out.println("\t30 seconds. Combine tiles for more time");
//			System.out.println("6. XMode");
//			System.out.println("\tMovable X tile that can't be combined");
//			System.out.println("7. Corner Mode");
//			System.out.println("\tImmovable Blocks In Corners");
//			System.out.println("8. Speed Mode");
//			System.out.println("\tTiles appear every 2 seconds");
//			System.out.println("9. Zen Mode");
//			System.out.println("\tAll ties can combine. Impossible to lose");
//			System.out.println("10. Crazy Mode");
//			System.out.println("\t5x5 grid, with every mode enabled");
//			System.out.println("11. Custom Game");
//			System.out.println("12. Autoplay");
//			System.out.println("13. Settings");
//
//			input = getIntegerInput(1, 13, "Incorrect input. Enter 1 through 12 with no punctuation");
//
//			switch(input)
//			{
//				case 1: practiceMode();
//						break;
//				case 2: normalMode();
//						break;
//				case 3: proMode();
//						break;
//				case 4: rushMode();
//						break;
//				case 5: survivalMode();
//						break;
//				case 6: XMode();
//						break;
//				case 7: cornerMode();
//						break;
//				case 8: speedMode();
//						break;
//				case 9: zenMode();
//						break;
//				case 10: crazyMode();
//						break;
//				case 11: customManualPlay();
//						break;
//				case 12: autoplayMode(new Game());
//						break;
//				case 13: customizeKeyMap();
//					break;
//			}
//
//			System.out.println("Play again? y/n");
//		}
//		while(scan.next().charAt(0) == 'y');
//	}
//
//	//---------------------------------------------------------
//	// Manual Play
//	//---------------------------------------------------------
//	public static void manualPlay(Game game)
//	{	
//		String direction = "";
//		
//		// Used to prevent printing Congratulations every move after it is won
//		boolean winningGame = false;
//		
//		System.out.println(game);
//
//		while(!(game.lost() || direction.contains("auto")))
//		{
//			direction = scan.next();
//			direction = direction.toLowerCase();
//
//			// Autoplay is in a separate code block so that the game is not
//			// printed again after it is finished
//			if(direction.contains("auto"))
//				autoplayMode(game);
//			else
//			{
//				// Remove Low Tiles
//				if(direction.contains("remove"))
//					game.removeLowTiles();
//				
//				// Hide Tile Values
//				else if(direction.contains("hide"))
//					game.hideTileValues(5);
//
//				// Undo
//				else if(direction.equals("undo"))
//					game.undo();
//
//				// Shuffle
//				else if(direction.equals("shuffle"))
//					game.shuffle();
//
//				// Save Game
//				else if(direction.equals("save"))
//					try
//					{
//						System.out.println("Saving the game...");
//						Save.saveGame(game);
//					}
//					catch (IOException e) 
//					{
//						System.out.println("Error: Saved game can not be accessed");
//						e.printStackTrace();
//					}
//				
//				// Load Game
//				else if(direction.equals("load"))
//					try
//					{
//						game = Save.loadGame();
//					}
//					catch(IOException e)
//					{
//						System.out.println("Error: Save file cannot be accessed");
//						e.printStackTrace();
//					}
//					catch (ClassNotFoundException e)
//					{
//						System.out.println("Error: Cannot read save file");
//						e.printStackTrace();
//					}
//					
//
//				// Clear Save
//				else if(direction.equals("clear"))
//					try
//					{
//						System.out.println("Deleting the save...");
//						Save.clearSave();
//					}
//					catch (IOException e)
//					{
//						System.out.println("Error: Save file can not be accessed");
//						e.printStackTrace();
//					}
//				
//				// Quit
//				else if(direction.charAt(0) == 'q')
//					game.quit();
//				
//				// If the game is moving up,right,down,or left
//				else if(keyMap.containsKey(direction.charAt(0)))
//				{
//					game.act(keyMap.get(direction.charAt(0)));
//				}
//				
//				
//				/*
//				// W or U to move up
//				else if(direction.charAt(0) == 'u' || direction.charAt(0) == 'w')
//					game.act(Location.UP);
//				
//				// R or D to move right
//				else if(direction.charAt(0) == 'r') // || direction.charAt(0) == 'd')
//					game.act(Location.RIGHT);
//
//				// D or S to move down
//				else if(direction.charAt(0) == 'd' || direction.charAt(0) == 's')
//					game.act(Location.DOWN);
//
//				// L or A to move left
//				else if(direction.charAt(0) == 'l' || direction.charAt(0) == 'a')
//					game.act(Location.LEFT);
//				*/
//
//				else
//				{
//					System.out.println("Invalid Command");
//					System.out.println("Controls: Left, Right, Up, Down, Quit, Undo, Shuffle");	
//				}
//
//				System.out.println(game);
//				
//				// If the game is won
//				if(game.won() && winningGame == false)
//				{
//					System.out.println("Congratulations! Game Won!");
//					System.out.println("Continuing in free play mode.");
//					winningGame = true;
//				}
//			}
//		}
//
//		// When the game is over
//		
//		// Save the score if it is higher than the previous high score
//		try
//		{
//			if(game.getScore() > Save.loadHighScore())
//			{
//				System.out.println("Congratulations! New High Score: " + game.getScore());
//				Save.saveHighScore(game.getScore());
//			}
//		}
//		catch (FileNotFoundException e)
//		{
//			System.out.println("Error: High score save file cannot be accessed");
//			e.printStackTrace();
//		}
//		
//		// If no move has been made don't print the time played
//		if(game.timePlayed() != 0)
//		{
//			// Calculate the time played
//			int milliseconds = (int) (game.timePlayed() * 1000);
//			int seconds = (int) (milliseconds / 1000) % 60 ;
//			int minutes = (int) (milliseconds / (1000*60));
//			milliseconds -= seconds * 60;
//
//			// Round the time to the nearest millisecond
//			// Prevent precision errors (0.89999999999 seconds)
//			String milli = "" + milliseconds;
//			milli = "." + milli.substring(0,3);
//
//			System.out.println("Time Played: " + minutes + " minutes " + (seconds + milli) + " seconds");
//		}
//	}
//	
//	
//
//	// Practice Mode
//	// Unlimited everything
//	public static void practiceMode()
//	{
//		Game game = new Game();
//		game.setMoveLimit(-1);
//		game.setUndoLimit(-1);
//		game.setTimeLimit(-1);
//		
//		manualPlay(game);
//	}
//
//	// Normal Mode
//	// Unlimited moves and time
//	// 10 undos
//	public static void normalMode()
//	{
//		Game game = new Game();
//		game.setMoveLimit(-1);
//		game.setUndoLimit(10);
//		game.setTimeLimit(-1);
//
//		manualPlay(game);
//	}
//
//	// Pro Mode
//	// Unlimited moves and time
//	// No undos
//	public static void proMode()
//	{
//		Game game = new Game();
//		game.setMoveLimit(-1);
//		game.setUndoLimit(0);
//		game.setTimeLimit(-1);
//
//		manualPlay(game);
//	}
//
//	// Rush Mode
//	// Higher value tiles spawn
//	public static void rushMode()
//	{
//		Game game = new Game();
//		game.dynamicTileSpawning(true);
//
//		manualPlay(game);
//	}
//
//
//	// Survival Mode
//	// Unlimited moves and undos
//	// Only 30 seconds to play. The time increases when tiles >= 8 combine
//	public static void survivalMode()
//	{
//		Game game = new Game();
//		game.setMoveLimit(-1);
//		game.setUndoLimit(-1);
//		game.setTimeLimit(30);
//		game.survivalMode();
//
//		manualPlay(game);
//	}
//
//	// XMode
//	// Unlimited moves and time
//	// 10 undos
//	// Places an X on the board that can move but not combine 
//	public static void XMode()
//	{
//		Game game = new Game();
//		game.setMoveLimit(-1);
//		game.setUndoLimit(10);
//		game.setTimeLimit(-1);
//		game.XMode();
//
//		manualPlay(game);
//	}
//
//	// Corner Mode
//	// Unlimited moves and time
//	// 10 undos
//	// Places immovable pieces in the corners of the board
//	public static void cornerMode()
//	{
//		Game game = new Game();
//		game.setMoveLimit(-1);
//		game.setUndoLimit(10);
//		game.setTimeLimit(-1);
//		game.cornerMode();
//
//		manualPlay(game);
//	}
//	
//	// Speed Mode
//	// Unlimited moves and time
//	// 10 undos
//	// Tiles appear every every 2 seconds even if no move was made
//	public static void speedMode()
//	{
//		Game game = new Game();
//		game.setMoveLimit(-1);
//		game.setUndoLimit(-1);
//		game.setTimeLimit(-1);
//		game.speedMode(true);
//		
//		manualPlay(game);
//	}
//
//	// Zen Mode
//	// Unlimited moves, undos and time
//	// Every piece can combine
//	public static void zenMode()
//	{
//		Game game = new Game();
//		game.setMoveLimit(-1);
//		game.setUndoLimit(-1);
//		game.setTimeLimit(-1);
//		game.zenMode(true);
//
//		manualPlay(game);
//	}
//	
//	// Crazy Mode
//	// Unlimited moves and undos
//	// A 5x5 game with every other mode enabled (except zen)
//	public static void crazyMode()
//	{
//		Game game = new Game(5,5);
//		game.setMoveLimit(-1);
//		game.setUndoLimit(-1);
//		game.setTimeLimit(30);
//		game.survivalMode();
//		game.cornerMode();
//		game.XMode();
//		game.dynamicTileSpawning(true);
//		game.speedMode(true);
//
//		manualPlay(game);
//	}
//	
//	//--------------------------------------------------------------------
//	// Customize Manual Play
//	// Can change the board size, Move Limit, Undo Limit, Time Limit,
//	// Activate Corner Mode, XMode, and Survival Mode
//	//--------------------------------------------------------------------
//	public static void customManualPlay()
//	{
//		int limit;
//		
//		System.out.println("Enter the number of rows. Recommended: 4");
//		int rows = 	getIntegerInput(1, 100, "Error: Enter a valid number of rows");
//		
//		System.out.println("Enter the number of columns. Recommended: 4");
//		int cols = 	getIntegerInput(1, 100, "Error: Enter a valid number of columns");
//		
//		Game game = new Game(rows,cols);
//		
//		// I don't know what this does but without it the program skips the next scan
//		scan.nextLine();
//		
//		System.out.println("Enter Move Limit (press enter for unlimited)");
//		limit = getLimitInput();
//		game.setMoveLimit(limit);
//		
//		System.out.println("Enter Undo Limit (press enter for unlimited)");
//		limit = getLimitInput();
//		game.setUndoLimit(limit);
//		
//		
//		System.out.println("Enter Time Limit In Seconds (press enter for unlimited)");
//		limit = getLimitInput();
//		game.setTimeLimit(limit);
//		
//		if(limit > 0)
//		{
//			// A game can only be in survival mode if it has a time limit
//			System.out.println("Survival Mode? (Combine tiles to increase time limit)");
//			System.out.println("Press enter for no, anything else for yes");
//			if(! scan.nextLine().equals(""))
//				game.survivalMode();
//		}
//		
//		System.out.println("Corner Mode? (Immovable X's in corner)");
//		System.out.println("Press enter for no, anything else for yes");
//		if(! scan.nextLine().equals(""))
//			game.cornerMode();
//		
//		System.out.println("X Mode? (A movable X that cannot be combined)");
//		System.out.println("Press enter for no, anything else for yes");
//		if(! scan.nextLine().equals(""))
//			game.XMode();
//		
//		System.out.println("Rush Mode? (Higher value tiles can appear)");
//		System.out.println("Press enter for no, anything else for yes");
//		if(! scan.nextLine().equals(""))
//			game.dynamicTileSpawning(true);
//		
//		System.out.println("Speed Mode? (Tiles appear every 2 seconds)");
//		System.out.println("Press enter for no, anything else for yes");
//		if(! scan.nextLine().equals(""))
//			game.speedMode(true);
//		
//		manualPlay(game);
//	}
//	
//	public static void autoplayMode(Game game)
//	{
//		System.out.println("Recursive, Circle, Corner, or Random? 1/2/3/4");	
//		int input = getIntegerInput(1, 4, "Incorrect input. Enter 1, 2, 3 or 4 with no punctuation");
//
//		switch(input)
//		{
//			case 1: recursiveHelper(game);
//					break;
//			case 2: Autoplay.circlePlay(game);
//					break;
//			case 3: Autoplay.cornerPlay(game);
//					break;
//			case 4: Autoplay.randomPlay(game);
//					break;
//			
//			default:
//			{	
//				System.out.println("Developer Mode Enabled");
//				System.out.println("Testing the recursive play");
//				for(int i = 0; i < 100; i++)
//				{
//					Autoplay.recursivePlay(game.clone(), game.clone(), 2048, true);
//					System.out.println(Autoplay.getAutoMoveCount());
//					Autoplay.setAutoMoveCount(0);
//				}
//			}
//		}
//	}
//	
//	//---------------------------------------------------------
//	// Calls the recursive play method
//	//---------------------------------------------------------
//	public static void recursiveHelper(Game game)
//	{
//		// Turn speed mode off because it interferes with autoplay
//		game.speedMode(false);
//		
//		System.out.println("Play until which tile is reached?");
//		System.out.println("(Values above 2048 are not recommended)");
//		int tile = getIntegerInput(0, 17000, "Error: Enter the target tile");
//		
//		Autoplay.recursivePlay(game, game, tile, true);
//		System.out.println("**** GAME WON ****");
//		System.out.println("Total Number of Moves: " + Autoplay.getAutoMoveCount());
//	}
//	
//	/**
//	 * Loads the saved HashMap that stores the controls
//	 * Assigns it to the global variable keyMap
//	 */
//	private static void loadKeyMap()
//	{
////		try
////		{
////			keyMap = Save.loadKeyMap();
////		} 
////		catch (IOException e)
////		{
////			System.err.println("Can not access file");
////		}
////		catch (ClassNotFoundException e)
////		{
////			System.err.println("Can not read file");
////		}
////		finally
////		{
////			if(keyMap == null)
////			{
////				// Create the default key mapping
////				keyMap = new HashMap<Character, Integer>();
////				
////				keyMap.put('u', Location.UP);
////				keyMap.put('r', Location.RIGHT);
////				keyMap.put('d', Location.DOWN);
////				keyMap.put('l', Location.LEFT);
////			}
////		}
//	}
//	
//	private static void customizeKeyMap()
//	{
//		char input;
//		
//		System.out.println("Customizing Input");
//		System.out.println("Type the key that you want to use");
//		
//		System.out.print("UP ");
//		input = scan.next().charAt(0);
//		keyMap.put(input, Location.UP);
//		
//		System.out.print("RIGHT ");
//		input = scan.next().charAt(0);
//		keyMap.put(input, Location.RIGHT);
//		
//		System.out.print("DOWN ");
//		input = scan.next().charAt(0);
//		keyMap.put(input, Location.DOWN);
//		
//		System.out.print("LEFT ");
//		input = scan.next().charAt(0);
//		keyMap.put(input, Location.LEFT);
//		
////		try
////		{
////			Save.saveKeyMap(keyMap);
////		} 
////		catch (IOException e)
////		{
////			System.err.println("Error: Save file cannot be accessed");
////			e.printStackTrace();
////		}
//	}
//	
//	/**
//	 * Used to get input when choosing a number
//	 * @param minValue The lowest value that the input can have
//	 * @param maxValue The highest value that the input can have
//	 * @param errorMessage Printed when invalid input entered
//	 * @return A valid integer input
//	 */
//	public static int getIntegerInput(int minValue, int maxValue, String errorMessage)
//	{
//		int input = 0;
//		boolean isInteger = false;
//		boolean isValid = false;
//		
//		// Error trapping
//		do
//		{
//			try
//			{
//				input = scan.nextInt();
//				isInteger = true;
//			}
//			catch(InputMismatchException e)
//			{
//				// Clears the invalid input
//				scan.next();
//				isInteger = false;
//			}
//			
//			if(!isInteger || (input < minValue || input > maxValue))
//			{
//				isValid = false;
//				System.out.println(errorMessage);
//			}
//			else
//				isValid = true;
//
//		} while(!isValid);
//	
//		return input;	
//	}
//	
//	/**
//	 * Used to get input for the time limit, undo limit, and time limit
//	 * Valid input is an integer 0 or greater or an empty string
//	 * @return The valid limit. (An integer -1 or greater)
//	 */
//	public static int getLimitInput()
//	{
//		String limit;
//		
//		while(true)
//		{
//			limit = scan.nextLine();
//			
//			if(limit.equals(""))
//				return -1;
//			else
//			{
//				try
//				{
//					int moveLimit = Integer.parseInt(limit);
//					
//					if(moveLimit >= 0)
//						return moveLimit;
//					else
//						System.out.println("Error: Enter a value 0 or greater");
//				
//				}
//				catch (Exception e)
//				{
//					System.out.println("Error: Enter a numerical value 0 or greater");
//					System.out.println("Press enter for unlimited");
//				}
//				
//			}
//		}
//	}
//	
//}
