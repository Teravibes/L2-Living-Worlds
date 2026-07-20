/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.gameserver.geoengine.util;

/**
 * An iterator that steps through points on a straight line using Bresenham's line-drawing algorithm.
 * <p>
 * Bresenham's algorithm calculates which grid points should be part of a straight line between two points.
 * </p>
 * @author Mobius
 */
public class GridLineIterator2D
{
	// Current position of the iterator.
	private int _currentX; // Current X position of the iterator.
	private int _currentY; // Current Y position of the iterator.
	
	// Target endpoint of the line.
	private final int _targetX; // Target X endpoint of the line.
	private final int _targetY; // Target Y endpoint of the line.
	
	// Step directions for movement along the X and Y axes.
	private final int _stepX; // Step direction for X (-1, 0 or 1).
	private final int _stepY; // Step direction for Y (-1, 0 or 1).
	
	// Absolute distances along X and Y axes.
	private final int _deltaX; // Absolute difference in X coordinates.
	private final int _deltaY; // Absolute difference in Y coordinates.
	
	// Indicates whether the line is steep (Y changes faster than X).
	private final boolean _isSteep;
	
	// Tracks the accumulated error term for adjusting the minor axis.
	private int _accumulatedError;
	
	// Indicates whether the iteration has started.
	private boolean _hasStarted;
	
	/**
	 * Initializes the iterator to step through points along a line from (startX, startY) to (endX, endY).
	 * @param startX the X-coordinate of the start point
	 * @param startY the Y-coordinate of the start point
	 * @param endX the X-coordinate of the end point
	 * @param endY the Y-coordinate of the end point
	 */
	public GridLineIterator2D(int startX, int startY, int endX, int endY)
	{
		// Initialize current position.
		_currentX = startX;
		_currentY = startY;
		
		// Initialize target position.
		_targetX = endX;
		_targetY = endY;
		
		// Calculate absolute differences in X and Y coordinates.
		_deltaX = Math.abs(endX - startX);
		_deltaY = Math.abs(endY - startY);
		
		// Determine step directions for X and Y.
		_stepX = Integer.compare(endX, startX); // -1, 0 or 1.
		_stepY = Integer.compare(endY, startY); // -1, 0 or 1.
		
		// Determine if the line is steep (Y changes more than X).
		_isSteep = _deltaY > _deltaX;
		
		// Initialize the accumulated error term.
		// Start with half the dominant axis to balance initial adjustment.
		_accumulatedError = (_isSteep ? _deltaY : _deltaX) / 2;
		
		// Set the initial iteration state.
		_hasStarted = false;
	}
	
	/**
	 * Advances the iterator to the next point on the line.
	 * <p>
	 * This method implements Bresenham's line-drawing algorithm,<br>
	 * using an accumulated error term to determine when to step along the minor axis.
	 * </p>
	 * @return {@code true} if the iterator successfully moved to the next point or<br>
	 *         {@code false} if the end of the line has been reached.
	 */
	public boolean next()
	{
		if (!_hasStarted)
		{
			_hasStarted = true; // Start the iteration on the first call.
			return true;
		}
		
		// If the current position equals the target, stop iteration.
		if ((_currentX == _targetX) && (_currentY == _targetY))
		{
			return false;
		}
		
		// Move along the line.
		if (_isSteep)
		{
			// For steep lines, Y is the dominant axis.
			_currentY += _stepY;
			_accumulatedError -= _deltaX;
			if (_accumulatedError < 0)
			{
				// Step along X when the accumulated error exceeds the threshold.
				_currentX += _stepX;
				_accumulatedError += _deltaY;
			}
		}
		else
		{
			// For shallow lines, X is the dominant axis.
			_currentX += _stepX;
			_accumulatedError -= _deltaY;
			if (_accumulatedError < 0)
			{
				// Step along Y when the accumulated error exceeds the threshold.
				_currentY += _stepY;
				_accumulatedError += _deltaX;
			}
		}
		
		return true; // Continue iteration.
	}
	
	/**
	 * Returns the current X-coordinate of the iterator's position.
	 * @return the current X-coordinate
	 */
	public int x()
	{
		return _currentX;
	}
	
	/**
	 * Returns the current Y-coordinate of the iterator's position.
	 * @return the current Y-coordinate
	 */
	public int y()
	{
		return _currentY;
	}
	
	@Override
	public String toString()
	{
		return "[" + _currentX + ", " + _currentY + "]";
	}
}
