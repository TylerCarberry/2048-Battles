package com.example.app_2048;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Save
{
	public static final String SAVEGAMELOCATION = "savegame.txt";
	public static final String HIGHSCORELOCATION = "highscore.txt";
	public static final String KEYMAPLOCATION = "keymap.txt";
	
	/**
	 * Saves the current game to a file. Overwrites any previous saves.
	 * @param game The game to save
	 * @throws IOException Save file can not be accessed
	 */
	public static void saveGame(Game game) throws IOException
	{
		File file = new File(SAVEGAMELOCATION);
		
		// Serialize the game
		FileOutputStream fop = new FileOutputStream(file);
		ObjectOutputStream output = new ObjectOutputStream(fop);
		
		// Write the game to the file
		output.writeObject(game);
	
		output.close();
		fop.close();
	}
	
	/**
	 * Loads the saved game from a file
	 * @return The saved game
	 * @throws IOException Save file can not be accessed
	 * @throws ClassNotFoundException Class of a serialized object cannot be found
	 */
	public static Game loadGame() throws IOException, ClassNotFoundException
	{
		
		FileInputStream fi = new FileInputStream(SAVEGAMELOCATION);
		ObjectInputStream input = new ObjectInputStream(fi);
		
		Game game = (Game) input.readObject();
		
		fi.close();
		input.close();
		
		return game;
	}
	
	/**
	 * Clears the save file
	 * @throws IOException Save file can not be accessed
	 */
	public static void clearSave() throws IOException
	{
		File file = new File(SAVEGAMELOCATION);
		
		// Serialize the game
		FileOutputStream fop = new FileOutputStream(file);
		ObjectOutputStream output = new ObjectOutputStream(fop);
		
		// Write an empty String over the game
		output.writeObject("");
	
		output.close();
		fop.close();
	}
	
	/**
	 * Saves the high score. 
	 * Overwrites the previous value even if it is lower
	 * @param score The new high score
	 * @throws FileNotFoundException Save file can not be accessed
	 */
	public static void saveHighScore(int score) throws FileNotFoundException
	{
		File file = new File(HIGHSCORELOCATION);
		
		PrintWriter output = new PrintWriter(file);
		output.println(score);
		
		output.close();
	}
	
	/**
	 * Load the saved high score from a file 
	 * If the file cannot be accessed or is empty returns 0
	 * @return The saved high score
	 */
	public static int loadHighScore()
	{
		File file = new File(HIGHSCORELOCATION);
		int highScore = 0;
		try
		{
			Scanner scan = new Scanner(file);
			highScore = scan.nextInt();
		}
		
		// If the file cannot be accessed or the file is empty
		// assume that it is a new game and the high score is 0
		catch (FileNotFoundException e) { }
		catch (NoSuchElementException e) { }
		
		return highScore;
	}
}
