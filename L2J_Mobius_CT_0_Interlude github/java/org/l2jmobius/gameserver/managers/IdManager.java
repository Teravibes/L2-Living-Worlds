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
package org.l2jmobius.gameserver.managers;

import java.util.BitSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.config.IdManagerConfig;
import org.l2jmobius.gameserver.util.PrimeCapacityAllocator;

/**
 * Manages unique IDs for objects.
 * @author Mobius
 */
public class IdManager
{
	private static final Logger LOGGER = Logger.getLogger(IdManager.class.getName());
	
	private static final int TOTAL_ID_COUNT = IdManagerConfig.LAST_OBJECT_ID - IdManagerConfig.FIRST_OBJECT_ID;
	
	private BitSet _freeIds;
	private int _freeIdCount;
	private int _nextFreeId;
	private final Lock _lock = new ReentrantLock();
	
	public IdManager()
	{
		// Cleanup database.
		DatabaseIdManager.cleanDatabase();
		DatabaseIdManager.cleanCharacterStatus();
		DatabaseIdManager.cleanTimestamps();
		
		// Initialize BitSet with initial capacity or next prime.
		try
		{
			_freeIds = new BitSet(PrimeCapacityAllocator.nextCapacity(IdManagerConfig.INITIAL_CAPACITY));
			_freeIds.clear();
			_freeIdCount = TOTAL_ID_COUNT;
			
			// Register used ids.
			for (int usedObjectId : DatabaseIdManager.getUsedIds())
			{
				final int objectId = usedObjectId - IdManagerConfig.FIRST_OBJECT_ID;
				if (objectId < 0)
				{
					continue;
				}
				
				_freeIds.set(objectId);
				_freeIdCount--;
			}
			
			_nextFreeId = _freeIds.nextClearBit(0);
		}
		catch (Exception e)
		{
			LOGGER.severe("IdManager: Could not be initialized properly: " + e.getMessage());
		}
		
		LOGGER.info("IdManager: " + _freeIds.size() + " ids available.");
	}
	
	/**
	 * Increases the capacity of the BitSet to accommodate more IDs, based on the current utilization and threshold.<br>
	 * Uses the next prime number for optimal BitSet size, up to the maximum allowed ID range.
	 */
	private void increaseBitSetCapacity()
	{
		final int currentSize = _freeIds.size();
		final int newSize = Math.min(PrimeCapacityAllocator.nextCapacity((int) (currentSize * IdManagerConfig.RESIZE_MULTIPLIER)), TOTAL_ID_COUNT);
		
		// Only resize if the new size is larger than the current size.
		if (newSize > currentSize)
		{
			final BitSet newBitSet = new BitSet(newSize);
			newBitSet.or(_freeIds);
			_freeIds = newBitSet;
			LOGGER.info("IdManager: Increased BitSet capacity to " + newSize);
		}
	}
	
	/**
	 * Allocates and returns the next available unique ID.<br>
	 * If the BitSet reaches the defined utilization threshold, it is resized to maintain available capacity.
	 * @return the next unique ID
	 * @throws NullPointerException if there are no valid IDs remaining in the configured range
	 */
	public int getNextId()
	{
		_lock.lock();
		try
		{
			final int newId = _nextFreeId;
			_freeIds.set(newId);
			_freeIdCount--;
			
			// Check utilization and increase capacity if needed.
			final double utilization = (double) (TOTAL_ID_COUNT - _freeIdCount) / _freeIds.size();
			if (utilization >= IdManagerConfig.RESIZE_THRESHOLD)
			{
				increaseBitSetCapacity();
				_nextFreeId = _freeIds.nextClearBit(0); // Reset to the first available ID in the resized BitSet.
			}
			
			int nextFree = _freeIds.nextClearBit(newId);
			if (nextFree < 0)
			{
				nextFree = _freeIds.nextClearBit(0);
			}
			
			if (nextFree < 0)
			{
				if (_freeIds.size() < TOTAL_ID_COUNT)
				{
					increaseBitSetCapacity();
					_nextFreeId = _freeIds.nextClearBit(0); // Reset again if resizing occurred.
				}
				else
				{
					throw new NullPointerException("IdManager: Ran out of valid ids.");
				}
			}
			
			_nextFreeId = nextFree;
			
			return newId + IdManagerConfig.FIRST_OBJECT_ID;
		}
		finally
		{
			_lock.unlock();
		}
	}
	
	/**
	 * Releases a previously allocated ID, making it available for future allocations.
	 * @param objectId the ID to release
	 */
	public void releaseId(int objectId)
	{
		_lock.lock();
		try
		{
			if ((objectId - IdManagerConfig.FIRST_OBJECT_ID) > -1)
			{
				_freeIds.clear(objectId - IdManagerConfig.FIRST_OBJECT_ID);
				_freeIdCount++;
			}
			else
			{
				LOGGER.warning("IdManager: Release objectID " + objectId + " failed (< " + IdManagerConfig.FIRST_OBJECT_ID + ")");
			}
		}
		finally
		{
			_lock.unlock();
		}
	}
	
	/**
	 * Retrieves the current count of available IDs within the range.
	 * @return the number of available IDs
	 */
	public int getAvailableIdCount()
	{
		_lock.lock();
		try
		{
			return _freeIdCount;
		}
		finally
		{
			_lock.unlock();
		}
	}
	
	public static IdManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final IdManager INSTANCE = new IdManager();
	}
}
