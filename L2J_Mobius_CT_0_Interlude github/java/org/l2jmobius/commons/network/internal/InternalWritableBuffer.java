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
package org.l2jmobius.commons.network.internal;

import java.nio.ByteBuffer;

import org.l2jmobius.commons.network.ResourcePool;
import org.l2jmobius.commons.network.WritableBuffer;

/**
 * An abstract base class for internal writable buffers.<br>
 * This class defines the common interface for various implementations of writable buffers within the network package.
 * @author JoeAlisson, Mobius
 */
public abstract class InternalWritableBuffer extends WritableBuffer
{
	/**
	 * Gets the current buffer position.
	 * @return The current position within the buffer.
	 */
	public abstract int position();
	
	/**
	 * Sets the buffer position to a new value.
	 * @param pos The new position within the buffer.
	 */
	public abstract void position(int pos);
	
	/**
	 * Marks the end of the buffer's content.
	 */
	public abstract void mark();
	
	/**
	 * Converts the writable buffer into an array of ByteBuffers.
	 * @return An array of ByteBuffers containing the contents of the writable buffer.
	 */
	public abstract ByteBuffer[] toByteBuffers();
	
	/**
	 * Releases the resources used by the buffer.
	 */
	public abstract void releaseResources();
	
	/**
	 * Create a new Dynamic Buffer that increases as needed based on ArrayPacketBuffer
	 * @param buffer the base buffer
	 * @param resourcePool the resource pool used to get new buffers when needed
	 * @param packetClass the Class<?> of the writable packet
	 * @return a new Dynamic buffer
	 */
	public static InternalWritableBuffer dynamicOf(ArrayPacketBuffer buffer, ResourcePool resourcePool, Class<?> packetClass)
	{
		final DynamicPacketBuffer copy = new DynamicPacketBuffer(buffer.toByteBuffer(), resourcePool, packetClass);
		copy.limit(buffer.limit());
		return copy;
	}
	
	/**
	 * Create a new Dynamic Buffer that increases as needed
	 * @param resourcePool the resource pool used to get new buffers
	 * @param packetClass the Class<?> of the writable packet
	 * @return a new Dynamic Buffer
	 */
	public static InternalWritableBuffer dynamicOf(ResourcePool resourcePool, Class<?> packetClass)
	{
		return new DynamicPacketBuffer(resourcePool, packetClass);
	}
	
	/**
	 * Create a new buffer backed by array
	 * @param resourcePool the resource pool used to get new buffers
	 * @param packetClass the Class<?> of the writable packet
	 * @return a Buffer backed by array
	 */
	public static InternalWritableBuffer arrayBacked(ResourcePool resourcePool, Class<?> packetClass)
	{
		return new ArrayPacketBuffer(resourcePool, packetClass);
	}
}
