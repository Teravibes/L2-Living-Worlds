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

import java.net.SocketAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.l2jmobius.commons.network.internal.BufferPool;
import org.l2jmobius.commons.util.ConfigReader;

/**
 * Configures and initializes connection parameters for the network layer.<br>
 * This class handles configuration settings, buffer pools and network properties.
 * @author Mobius
 */
public class ConnectionConfig
{
	public static final int HEADER_SIZE = 2;
	
	private static final Pattern BUFFER_POOL_PROPERTY = Pattern.compile("(BufferPool\\.\\w+?\\.)Size", Pattern.CASE_INSENSITIVE);
	private static final int MINIMUM_POOL_GROUPS = 3;
	
	public ResourcePool resourcePool;
	public SocketAddress address;
	
	public float initBufferPoolFactor;
	public long shutdownWaitTime;
	public int threadPoolSize;
	public boolean useNagle;
	public boolean dropPackets;
	public int dropPacketThreshold;
	public int threadPriority;
	public boolean autoExpandPoolCapacity;
	
	/**
	 * Initializes the connection configuration with the specified socket address.
	 * @param socketAddress the address to which this configuration applies
	 */
	public ConnectionConfig(SocketAddress socketAddress)
	{
		address = socketAddress;
		threadPoolSize = 2;
		
		// Initialize Resource Pool and default buffer settings.
		resourcePool = new ResourcePool();
		resourcePool.addBufferPool(HEADER_SIZE, new BufferPool(100, HEADER_SIZE));
		
		// Read configuration properties.
		final ConfigReader networkConfig = new ConfigReader("config/Network.ini");
		shutdownWaitTime = networkConfig.getInt("ShutdownWaitTime", 5) * 1000L;
		
		// Configure thread pool based on processor count.
		final int processors = Runtime.getRuntime().availableProcessors();
		threadPoolSize = networkConfig.getInt("ThreadPoolSize", threadPoolSize);
		threadPoolSize = threadPoolSize < 1 ? processors * 4 : threadPoolSize;
		
		// Other network and buffer configurations.
		threadPriority = networkConfig.getInt("ThreadPriority", Thread.NORM_PRIORITY);
		autoExpandPoolCapacity = networkConfig.getBoolean("BufferPool.AutoExpandCapacity", true);
		initBufferPoolFactor = networkConfig.getFloat("BufferPool.InitFactor", 0);
		dropPackets = networkConfig.getBoolean("DropPackets", dropPackets);
		dropPacketThreshold = networkConfig.getInt("DropPacketThreshold", 250);
		resourcePool.setBufferSegmentSize(networkConfig.getInt("BufferSegmentSize", resourcePool.getSegmentSize()));
		
		// Set up custom buffer pools from properties.
		networkConfig.getStringPropertyNames().forEach(property ->
		{
			final Matcher matcher = BUFFER_POOL_PROPERTY.matcher(property);
			if (matcher.matches())
			{
				final int size = networkConfig.getInt(property, 10);
				final int bufferSize = networkConfig.getInt(matcher.group(1) + "BufferSize", 1024);
				resourcePool.addBufferPool(bufferSize, new BufferPool(size, bufferSize));
			}
		});
		
		// Add additional buffer pool for segment size.
		resourcePool.addBufferPool(resourcePool.getSegmentSize(), new BufferPool(100, resourcePool.getSegmentSize()));
		
		// Ensure minimum pool groups.
		final int missingPools = MINIMUM_POOL_GROUPS - resourcePool.bufferPoolSize();
		for (int i = 0; i < missingPools; i++)
		{
			final int bufferSize = 256 << i;
			resourcePool.addBufferPool(bufferSize, new BufferPool(10, bufferSize));
		}
		
		// Initialize resource pool buffers.
		resourcePool.initializeBuffers(autoExpandPoolCapacity, initBufferPoolFactor);
	}
}
