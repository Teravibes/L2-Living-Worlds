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
package org.l2jmobius.commons.network;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.l2jmobius.commons.network.internal.BufferPool;

/**
 * Manages pools of ByteBuffers for efficient resource allocation and reuse.<br>
 * This class maintains a collection of buffer pools, each optimized for different buffer sizes, to reduce the overhead of buffer allocation.
 * @author JoeAlisson, Mobius
 */
public class ResourcePool
{
	// private static final Logger LOGGER = Logger.getLogger(ResourcePool.class.getName());
	
	private final NavigableMap<Integer, BufferPool> _bufferPools = new TreeMap<>();
	private boolean _autoExpandCapacity = true;
	private boolean _initBufferPools = false;
	private float _initBufferPoolFactor = 0;
	private int _bufferSegmentSize = 64;
	
	public ResourcePool()
	{
	}
	
	/**
	 * Retrieves a ByteBuffer sized to match the configured header size.
	 * @return a ByteBuffer with the header size.
	 */
	public ByteBuffer getHeaderBuffer()
	{
		return getSizedBuffer(ConnectionConfig.HEADER_SIZE);
	}
	
	/**
	 * Retrieves a ByteBuffer of at least the specified size.
	 * @param size the minimum size required for the buffer.
	 * @return a ByteBuffer with a capacity matching or exceeding the requested size.
	 */
	public ByteBuffer getBuffer(int size)
	{
		return getSizedBuffer(determineBufferSize(size));
	}
	
	/**
	 * Recycles an existing ByteBuffer and retrieves a new one of the specified size.
	 * @param buffer the buffer to recycle.
	 * @param newSize the size of the new buffer.
	 * @return a ByteBuffer of the specified new size.
	 */
	public ByteBuffer recycleAndGetNew(ByteBuffer buffer, int newSize)
	{
		final int bufferSize = determineBufferSize(newSize);
		if (buffer != null)
		{
			if (buffer.clear().limit() == bufferSize)
			{
				return buffer.limit(newSize);
			}
			
			recycleBuffer(buffer);
		}
		
		return getSizedBuffer(bufferSize).limit(newSize);
	}
	
	/**
	 * Retrieves a ByteBuffer of the specified size from the pool, expanding capacity if necessary.
	 * @param size the size of the buffer to retrieve.
	 * @return a ByteBuffer with at least the specified size.
	 */
	private ByteBuffer getSizedBuffer(int size)
	{
		ByteBuffer buffer = null;
		BufferPool pool = null;
		
		final Entry<Integer, BufferPool> entry = _bufferPools.ceilingEntry(size);
		if (entry != null)
		{
			pool = entry.getValue();
			if (pool != null)
			{
				if (_autoExpandCapacity)
				{
					if (_initBufferPools)
					{
						if (pool.isEmpty())
						{
							// LOGGER.info("ResourcePool: Buffer pool for size " + size + " is empty. Expanding capacity by a factor of " + _initBufferPoolFactor + ".");
							pool.expandCapacity(_initBufferPoolFactor, pool.getMaxSize());
						}
					}
					else if (pool.isFull())
					{
						// LOGGER.info("ResourcePool: Buffer pool for size " + entry.getKey() + " is full. Doubling the capacity from " + pool.getMaxSize() + " to " + (pool.getMaxSize() * 2) + ".");
						pool.expandCapacity(_initBufferPoolFactor, pool.getMaxSize());
					}
				}
				
				buffer = pool.get();
			}
		}
		
		if (buffer == null)
		{
			if (pool == null)
			{
				// LOGGER.warning("ResourcePool: There is no buffer pool handling buffer size " + size + ". Creating a new pool.");
				pool = new BufferPool(10, size);
				if (_initBufferPools)
				{
					pool.initialize(_initBufferPoolFactor);
				}
				
				_bufferPools.put(size, pool);
			}
			
			buffer = ByteBuffer.allocateDirect(size).order(ByteOrder.LITTLE_ENDIAN);
		}
		
		return buffer;
	}
	
	/**
	 * Determines the appropriate buffer size for a given request.
	 * @param size the requested buffer size.
	 * @return the size of the closest matching buffer pool.
	 */
	private int determineBufferSize(int size)
	{
		final Entry<Integer, BufferPool> entry = _bufferPools.ceilingEntry(size);
		if (entry != null)
		{
			return entry.getKey();
		}
		
		// LOGGER.warning("ResourcePool: There is no buffer pool handling buffer size " + size + ". Creating a new pool.");
		final BufferPool pool = new BufferPool(10, size);
		if (_initBufferPools)
		{
			pool.initialize(_initBufferPoolFactor);
		}
		
		_bufferPools.put(size, pool);
		
		return size;
	}
	
	/**
	 * Recycles a buffer by returning it to the appropriate pool.
	 * @param buffer the ByteBuffer to recycle.
	 */
	public void recycleBuffer(ByteBuffer buffer)
	{
		if (buffer != null)
		{
			final BufferPool pool = _bufferPools.get(buffer.capacity());
			if ((pool == null) || !pool.recycle(buffer))
			{
				// LOGGER.warning("ResourcePool: Buffer was not recycled " + buffer + " in pool " + pool);
			}
		}
	}
	
	/**
	 * Retrieves the buffer segment size.
	 * @return the buffer segment size.
	 */
	public int getSegmentSize()
	{
		return _bufferSegmentSize;
	}
	
	/**
	 * Adds a new buffer pool to the resource pool.
	 * @param bufferSize the size of buffers managed by this pool.
	 * @param bufferPool the BufferPool to add.
	 */
	public void addBufferPool(int bufferSize, BufferPool bufferPool)
	{
		_bufferPools.putIfAbsent(bufferSize, bufferPool);
	}
	
	/**
	 * Returns the number of buffer pools in the resource pool.
	 * @return the number of buffer pools.
	 */
	public int bufferPoolSize()
	{
		return _bufferPools.size();
	}
	
	/**
	 * Initializes buffer pools with the specified parameters.
	 * @param autoExpandCapacity whether buffer pools should automatically expand capacity.
	 * @param initBufferPoolFactor the factor by which buffer pools expand.
	 */
	public void initializeBuffers(boolean autoExpandCapacity, float initBufferPoolFactor)
	{
		_autoExpandCapacity = autoExpandCapacity;
		_initBufferPoolFactor = initBufferPoolFactor;
		_initBufferPools = initBufferPoolFactor > 0;
		if (_initBufferPools)
		{
			_bufferPools.values().forEach(pool -> pool.initialize(initBufferPoolFactor));
		}
	}
	
	/**
	 * Sets the buffer segment size.
	 * @param size the new segment size.
	 */
	public void setBufferSegmentSize(int size)
	{
		_bufferSegmentSize = size;
	}
	
	/**
	 * Provides statistics of the buffer pools in this resource pool.
	 * @return a string containing statistics of the buffer pools.
	 */
	public String stats()
	{
		final StringBuilder sb = new StringBuilder();
		for (BufferPool pool : _bufferPools.values())
		{
			sb.append(pool.toString());
			sb.append(System.lineSeparator());
		}
		
		return sb.toString();
	}
}
