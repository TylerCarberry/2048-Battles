package com.tytanapps.game2048;
// Tyler Carberry

import java.util.*;
public class Grid implements Cloneable, java.io.Serializable
{
	private static final long serialVersionUID = -5760271435893128475L;
	private int[][] board;
	private boolean hideTileValues = false;
	
	/** Constructor
	 * @param rows The number of rows in the grid
	 * @param columns The number of columns in the grid
	 */
	public Grid (int rows, int columns)
	{
		board = new int[rows][columns];
	}
	
	/**
	 * Creates a grid from a 2d array
	 * @param array The array that the grid is created from
	 */
	public Grid(int[][] array)
	{
		board = array;
	}
	
	/**
	 * @param loc The location to get
	 * @return The value at that location
	 */
	public int get(Location loc)
	{
		return board[loc.getRow()][loc.getCol()];
	}
	
	/**
	 * @param loc The location to change
	 * @param newObject The new value of the location
	 * @return The old value at the location
	 */
	public int set(Location loc, int newObject)
	{
		int temp = get(loc);
		board[loc.getRow()][loc.getCol()] = newObject;
		return temp;
	}
	
	/**
	 * @param g The grid to compare
	 * @return If the 2 grids are equal
	 */
	public boolean equals(Grid g)
	{
		return (Arrays.deepEquals(board, g.getArray()));
	}
	
	/**
	 * Creates a grid based off a 2d array
	 * @param newBoard The new grid
	 */
	public void setArray(int[][] newBoard)
	{
		 board = newBoard;
	}
	
	public int[][] getArray()
	{
		return board;
	}
	
	public int getNumRows()
	{
		return board.length;
	}
	
	public int getNumCols()
	{
		return board[0].length;
	}
	
	public boolean isEmpty(Location loc)
	{
		return (get(loc) == 0);
	}
	
	/** Move the piece in location from into location to, overriding
	 *  what was originally there. Set the old location to 0.
	 * @param from The tile the move
	 * @param to The destination of the tile
	 */
	public void move(Location from, Location to)
	{
		set(to, get(from));
		set(from, 0);
	}
	
	/**
	 * @return A list of the empty locations
	 */
	public List<Location> getEmptyLocations()
	{
		LinkedList<Location> empty = new LinkedList<Location>();
		for(int row = 0; row < getNumRows(); row++)
			for(int col = 0; col < getNumCols(); col++)
				if(board[row][col] == 0)
					empty.add(new Location(row,col));
		
		return empty;
	}
	
	/**
	 * @return A list of filled locations
	 */
	public List<Location> getFilledLocations()
	{
		LinkedList<Location> filled = new LinkedList<Location>();
		for(int row = 0; row < getNumRows(); row++)
			for(int col = 0; col < getNumCols(); col++)
				if(board[row][col] != 0)
					filled.add(new Location(row,col));
			
		return filled;
	}
	
	/**
	 * Credit to chessdork for the code to combine the act methods
	 * https://github.com/chessdork/2048
	 * 
	 * Returns a list of locations in the order they should be traversed
	 * when shifting tiles.  If the direction is UP or LEFT, the list 
	 * begins at (0,0), traverses left-to-right, up-to-down, and terminates 
	 * at (numRows, numCols).  If the direction is DOWN or RIGHT, the list
	 * is reversed.
	 * 
	 */
	public List<Location> getLocationsInTraverseOrder(int direction)
	{
		List<Location> locs = toList();

		if (direction == Location.RIGHT || direction == Location.DOWN)
			Collections.reverse(locs);
		
		return locs;
	}
	
	/**
	 * Converts the grid into a list from left to right, top to bottom
	 * @return The grid as a list
	 */
	public List<Location> toList()
	{
		List<Location> locs = new ArrayList<Location>();
		
		for (int row = 0; row < getNumRows(); row++)
			for (int col = 0; col < getNumCols(); col++)
				locs.add(new Location(row, col));
		
		return locs;
	}
	
		
	/**
	 *  Sets all of the spaces to 0
	 */
	public void clear()
	{
		for(int row = 0; row < board.length; row++)
			for(int col = 0; col < board[0].length; col++)
				board[row][col] = 0;
	}
	
	/**
	 * Clones the array
	 * Used because the array clone method creates an alias for 2d integer arrays
	 */
	public Grid clone()
	{
		int[][] temp = new int[board.length][board[0].length];
		
		for(int row = 0; row < board.length; row++)
			for(int col = 0; col < board[0].length; col++)
				temp[row][col] = board[row][col];
		
		Grid result = new Grid(temp);
		return result;	
	}
	
	
	// Returns if the location is a valid position in the grid
	public boolean isValid(Location loc)
	{
		return (loc.getRow() >= 0 && loc.getRow() < getNumRows() &&
				loc.getCol() >= 0 && loc.getCol() < getNumCols());
	}
	
	/**
	 * @return The string representation of the grid
	 * Even Spacing
	 * | 4  | 16 | 256|1028|
	 */
	public String toString()
	{
		String output = "";
		

		Location loc;
		for(int row = 0; row < getNumRows(); row++)
		{
			for(int col = 0; col < getNumCols(); col++)
			{
				loc = new Location (row, col);
				
				if(hideTileValues)
				{
					if(get(loc) == 0)
						output += "|    ";
					else if(get(loc) == -1)
						output += "|XXXX";
					else
						output += "|  ? ";
					
				}
				
				// In X tile mode the X tile is represented as -2
				else if(get(loc) == -2)
					output += "|  x ";
				// In corner mode the blocks are represented as -1
				else if(get(loc) == -1)
					output += "|XXXX";
				else if(get(loc) == 0)
					output += "|    ";
				else if(get(loc) >= 1000)
					output += "|" + get(loc);
				else if(get(loc) >= 100)
					output += "| " + get(loc);
				else if(get(loc) >= 10)
					output += "| " + get(loc) + " ";
				else
					output += "| " + get(loc) + "  ";
			}
			
			output+= "|\n";
		}
		
		return output;
	}

	public void hideTileValues(boolean enabled)
	{
		hideTileValues = enabled;
	}
}
