package com.example.app_2048;
/** 
 * @author Tyler Carberry
 * Stack
 * Stores the history of the game board and score
 */

import java.util.LinkedList;

import android.util.Log;
public class Stack implements Cloneable, java.io.Serializable
{
	private static final long serialVersionUID = -7493874171801469542L;
	final static String LOG_TAG = Stack.class.getSimpleName();
	
	LinkedList<Grid> stackBoard;
	LinkedList<Integer> stackScore;
	
	/**
	 * Constructor
	 * A double stack that stores both the board and score
	 */
	public Stack()
	{
		stackBoard = new LinkedList<Grid>();
		stackScore = new LinkedList<Integer>();
	}
	
	/** 
	 * Both objects are pushed at the same time to avoid
	 * the score and board getting out of sync
	 * @param board The next board
	 * @param score The next game score
	 */
	public void push(Grid board, int score)
	{
		stackScore.add(score);
		stackBoard.add(board);
	}
	
	/**
	 * @return and remove the most recent score
	 */
	public int popScore()
	{
		return stackScore.removeLast();
	}
	
	/**
	 * @return and remove the most recent game board
	 */
	public Grid popBoard()
	{
		return stackBoard.removeLast();
	}
	
	/** 
	 * @return The most recent score without removing it
	 */
	public int frontScore()
	{
		return stackScore.getLast();
	}
	
	/** 
	 * @return The most recent score without removing it
	 */
	public Grid frontBoard()
	{
		return stackBoard.getLast();
	}
	
	/**
	 * @return true if either the board stack or score stack is empty
	 */
	public boolean isEmpty() {
		return stackBoard.isEmpty() || stackScore.isEmpty();
	}
	
	
	/**
	 * @return A clone of the stack and all of its elements
	 */
	public Stack clone()
	{
		Stack result = new Stack();
		
		for(int i = stackBoard.size() - 1; i >= 0; i--)
			result.push(stackBoard.get(i), stackScore.get(i));
		
		return result;
	}
	
	/**
	 * Should only be used for debugging purposes
	 */
	public String toString() {
		String result = "";
		
		for(int i = 0; i < stackBoard.size(); i++)
			result += stackBoard.get(i).toString() + stackScore.get(i) + "\n";
		
		return result;
	}
}
