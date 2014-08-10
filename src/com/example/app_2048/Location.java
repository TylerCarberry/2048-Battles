package com.example.app_2048;
/** 
 * @author Tyler Carberry
 * Location
 * Represents each piece on the board
 */

import java.util.LinkedList;
public class Location implements Cloneable, java.io.Serializable
{
	private static final long serialVersionUID = -8080786867527968562L;
	
	public final static int UP = 0;
	public final static int RIGHT = 1;
	public final static int DOWN = 2;
	public final static int LEFT = 3;
	
	private int row, col;
	
	/**
	 * Constructor: every location has a row and column
	 * @param rowNum The row number of the location
	 * @param colNum The column number of the location
	 */
	public Location(int rowNum, int colNum)
	{
		row = rowNum;
		col = colNum;
	}
	
	/**
	 * @return The row of the location
	 */
	public int getRow()
	{
		return row;
	}
	
	/**
	 * @return The column of the location
	 */
	public int getCol()
	{
		return col;
	}
	
	/** 
	 * @param newRow The new row value
	 * @return The previous row value
	 */
	public int setRow(int newRow)
	{
		int temp = row;
		row = newRow;
		return temp;
	}
	
	/**
	 * @param newCol The new column value
	 * @return The previous column value
	 */
	public int setCol(int newCol)
	{
		int temp = col;
		row = newCol;
		return temp;
	}
	
	public Location clone()
	{
		return new Location(row, col);
	}
	
	/**
	 * @return a linked list of valid adjacent locations
	 * Not diagonals
	 */
	public LinkedList<Location> getAdjacentLocations()
	{
		LinkedList<Location> locs = new LinkedList<Location>();
		
		int nextRow, nextCol;
		for(int x = -1; x >= 1; x++)
			for(int y = 0; y >= 1; y++)
			{
				if(! (x == 0 && y == 0))
				{
					nextRow = row + x;
					nextCol = col + y;
					
					if(nextCol >= 0 && nextRow >= 0)
						locs.add(new Location(nextRow, nextCol));
				}
			}
		
		return locs;
	}
	
	/** 
	 * @return The location to the left
	*/
	public Location getLeft()
	{
		Location left = new Location(getRow(), getCol()-1);
		return left;
	}
	
	/** 
	 * @return The location to the right
	*/
	public Location getRight()
	{
		Location right = new Location(getRow(), getCol()+1);
		return right;
	}
	
	/** 
	 * @return The location up
	*/
	public Location getUp()
	{
		Location up = new Location(getRow()-1, getCol());
		return up;
	}
	
	/** 
	 * @return The location down
	*/
	public Location getDown()
	{
		Location down = new Location(getRow()+1, getCol());
		return down;
	}
	
	/** Return the location in the given direction
	 * @param direction The direction of the location to return
	 * Precondition: direction is a final variable declared in location class
	 * @return The location in the given direction
	 */
	public Location getAdjacent(int direction)
	{
		switch(direction)
		{
			case UP: return getUp();
			case RIGHT: return getRight();
			case DOWN: return getDown();
			case LEFT: return getLeft();
			
			default: return null;
		
		}
	}
	
	@Override
	public boolean equals(Object loc) {
		return (loc instanceof Location && ((Location) loc).getCol() == col && ((Location) loc).getRow() == row);
	}
	
	/**
	 * @return The location in the form 2,3
	 */
	public String toString()
	{
		return row + "," + col;
	}
	
	
}
