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
public class GridLineIterator3D
{
	// Current position of the iterator.
	private int _currentX; // Current X position of the iterator.
	private int _currentY; // Current Y position of the iterator.
	private int _currentZ; // Current Z position of the iterator.
	
	// Target endpoint of the line.
	private final int _targetX; // Target X endpoint of the line.
	private final int _targetY; // Target Y endpoint of the line.
	private final int _targetZ; // Target Z endpoint of the line.
	
	// Absolute distances along X, Y, and Z axes.
	private final int _deltaX; // Absolute difference in X coordinates.
	private final int _deltaY; // Absolute difference in Y coordinates.
	private final int _deltaZ; // Absolute difference in Z coordinates.
	
	// Step directions for movement along the X, Y, and Z axes.
	private final int _stepX; // Step direction for X (-1 or 1).
	private final int _stepY; // Step direction for Y (-1 or 1).
	private final int _stepZ; // Step direction for Z (-1 or 1).
	
	// Tracks the accumulated error terms for adjusting the minor axes.
	private int _accumulatedErrorXY; // Accumulated error for the Y axis relative to X.
	private int _accumulatedErrorXZ; // Accumulated error for the Z axis relative to X.
	
	// Indicates whether the iteration has started.
	private boolean _hasStarted;
	
	/**
	 * Initializes the iterator to step through points along a 3D line from (startX, startY, startZ) to (endX, endY, endZ).
	 * @param startX the X-coordinate of the start point
	 * @param startY the Y-coordinate of the start point
	 * @param startZ the Z-coordinate of the start point
	 * @param endX the X-coordinate of the end point
	 * @param endY the Y-coordinate of the end point
	 * @param endZ the Z-coordinate of the end point
	 */
	public GridLineIterator3D(int startX, int startY, int startZ, int endX, int endY, int endZ)
	{
		// Initialize current position to the starting point.
		_currentX = startX;
		_currentY = startY;
		_currentZ = startZ;
		
		// Initialize target position.
		_targetX = endX;
		_targetY = endY;
		_targetZ = endZ;
		
		// Calculate absolute differences in X, Y, and Z coordinates.
		_deltaX = Math.abs(endX - startX);
		_deltaY = Math.abs(endY - startY);
		_deltaZ = Math.abs(endZ - startZ);
		
		// Determine step directions for X, Y, and Z based on the sign of the difference.
		_stepX = startX < endX ? 1 : -1;
		_stepY = startY < endY ? 1 : -1;
		_stepZ = startZ < endZ ? 1 : -1;
		
		// Initialize accumulated error terms based on the dominant axis.
		// The dominant axis is the one with the largest delta, as it determines the primary iteration direction.
		if ((_deltaX >= _deltaY) && (_deltaX >= _deltaZ))
		{
			_accumulatedErrorXY = _accumulatedErrorXZ = _deltaX / 2;
		}
		else if ((_deltaY >= _deltaX) && (_deltaY >= _deltaZ))
		{
			_accumulatedErrorXY = _accumulatedErrorXZ = _deltaY / 2;
		}
		else
		{
			_accumulatedErrorXY = _accumulatedErrorXZ = _deltaZ / 2;
		}
		
		// Set the initial iteration state to indicate the first point has not yet been processed.
		_hasStarted = false;
	}
	
	/**
	 * Advances the iterator to the next point on the line.
	 * <p>
	 * This method implements Bresenham's line-drawing algorithm,<br>
	 * which uses accumulated error terms to decide when to step along the minor axes.
	 * </p>
	 * @return {@code true} if the iterator successfully moved to the next point;<br>
	 *         {@code false} if the end of the line has been reached.
	 */
	public boolean next()
	{
		if (!_hasStarted)
		{
			// On the first call, mark the iteration as started but do not move.
			_hasStarted = true;
			return true;
		}
		
		// Stop the iteration if the current position equals the target.
		if ((_currentX == _targetX) && (_currentY == _targetY) && (_currentZ == _targetZ))
		{
			return false;
		}
		
		// Move along the dominant axis and adjust minor axes as needed.
		if ((_deltaX >= _deltaY) && (_deltaX >= _deltaZ))
		{
			// Dominant axis X.
			_currentX += _stepX;
			
			// Adjust Y based on accumulated error.
			_accumulatedErrorXY += _deltaY;
			if (_accumulatedErrorXY >= _deltaX)
			{
				_currentY += _stepY;
				_accumulatedErrorXY -= _deltaX;
			}
			
			// Adjust Z based on accumulated error.
			_accumulatedErrorXZ += _deltaZ;
			if (_accumulatedErrorXZ >= _deltaX)
			{
				_currentZ += _stepZ;
				_accumulatedErrorXZ -= _deltaX;
			}
		}
		else if ((_deltaY >= _deltaX) && (_deltaY >= _deltaZ))
		{
			// Dominant axis Y.
			_currentY += _stepY;
			
			// Adjust X based on accumulated error.
			_accumulatedErrorXY += _deltaX;
			if (_accumulatedErrorXY >= _deltaY)
			{
				_currentX += _stepX;
				_accumulatedErrorXY -= _deltaY;
			}
			
			// Adjust Z based on accumulated error.
			_accumulatedErrorXZ += _deltaZ;
			if (_accumulatedErrorXZ >= _deltaY)
			{
				_currentZ += _stepZ;
				_accumulatedErrorXZ -= _deltaY;
			}
		}
		else
		{
			// Dominant axis Z.
			_currentZ += _stepZ;
			
			// Adjust X based on accumulated error.
			_accumulatedErrorXY += _deltaX;
			if (_accumulatedErrorXY >= _deltaZ)
			{
				_currentX += _stepX;
				_accumulatedErrorXY -= _deltaZ;
			}
			
			// Adjust Y based on accumulated error.
			_accumulatedErrorXZ += _deltaY;
			if (_accumulatedErrorXZ >= _deltaZ)
			{
				_currentY += _stepY;
				_accumulatedErrorXZ -= _deltaZ;
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
	
	/**
	 * Returns the current Z-coordinate of the iterator's position.
	 * @return the current Z-coordinate
	 */
	public int z()
	{
		return _currentZ;
	}
	
	@Override
	public String toString()
	{
		return "[" + _currentX + ", " + _currentY + ", " + _currentZ + "]";
	}
}
