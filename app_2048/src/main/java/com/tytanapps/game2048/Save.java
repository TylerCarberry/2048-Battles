package com.tytanapps.game2048;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class Save
{
	/**
	 * Saves the current game to a file. Overwrites any previous saves.
	 * @param object The object to save
     * @param file The file to save to
	 * @throws IOException Save file can not be accessed
	 */
	public static void save(Serializable object, File file) throws IOException
	{
		// Serialize the object
		FileOutputStream fop = new FileOutputStream(file);
		ObjectOutputStream output = new ObjectOutputStream(fop);
		
		// Write the object to the file
		output.writeObject(object);
	
		output.close();
		fop.close();
	}
	
	/**
	 * Loads the saved game from a file
	 * @return The saved game as a Serializable object
	 * @throws IOException Save file can not be accessed
	 * @throws ClassNotFoundException Class of a serialized object cannot be found
	 */
	public static Serializable load(File file) throws IOException, ClassNotFoundException
	{	
		FileInputStream fi = new FileInputStream(file);
		ObjectInputStream input = new ObjectInputStream(fi);

        // Read the object from the file
		Serializable object = (Serializable) input.readObject();

		fi.close();
		input.close();

		return object;
	}

	/**
	 * Clears the save file
	 * @throws IOException Save file can not be accessed
	 */
	public static void clearFile(File file) throws IOException
    {
		FileOutputStream fop = new FileOutputStream(file);
		ObjectOutputStream output = new ObjectOutputStream(fop);
		
		// Write an empty String over the file
		output.writeObject("");
	
		output.close();
		fop.close();
	}
}
