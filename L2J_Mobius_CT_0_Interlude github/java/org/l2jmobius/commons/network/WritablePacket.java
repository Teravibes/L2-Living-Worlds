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

import org.l2jmobius.commons.network.internal.ArrayPacketBuffer;
import org.l2jmobius.commons.network.internal.InternalWritableBuffer;

/**
 * Abstract class representing a packet that can be sent to clients.<br>
 * All data sent must include a header with 2 bytes and an optional payload.<br>
 * The first two bytes are a 16-bit integer indicating the size of the packet.
 * @param <T> The type of Client associated with this packet.
 * @author JoeAlisson, Mobius
 */
public abstract class WritablePacket<T extends Client<Connection<T>>>
{
	private volatile boolean _broadcast;
	private ArrayPacketBuffer _broadcastCacheBuffer;
	
	protected WritablePacket()
	{
	}
	
	/**
	 * Writes the packet data to the buffer for the specified client.
	 * <p>
	 * If the packet is marked as broadcast, it attempts to use a cached buffer to reduce redundant data processing.<br>
	 * Otherwise, it directly writes the data to a new buffer.
	 * </p>
	 * @param client The client for whom the packet data is being written.
	 * @return An {@link InternalWritableBuffer} containing the packet data.
	 * @throws Exception If an error occurs during the data writing process.
	 */
	public InternalWritableBuffer writeData(T client) throws Exception
	{
		if (_broadcast)
		{
			return writeDataWithCache(client);
		}
		
		return writeDataToBuffer(client);
	}
	
	/**
	 * Writes the packet data to the buffer for the specified client.<br>
	 * If the packet is marked as broadcast, it will attempt to use a cached buffer.
	 * @param client The client to whom the packet data is being written.
	 * @return An {@link InternalWritableBuffer} containing the packet data.
	 * @throws Exception If an error occurs during writing.
	 */
	private synchronized InternalWritableBuffer writeDataWithCache(T client) throws Exception
	{
		if (_broadcastCacheBuffer != null)
		{
			return InternalWritableBuffer.dynamicOf(_broadcastCacheBuffer, client.getResourcePool(), getClass());
		}
		
		InternalWritableBuffer buffer = writeDataToBuffer(client);
		if (buffer instanceof ArrayPacketBuffer)
		{
			_broadcastCacheBuffer = (ArrayPacketBuffer) buffer;
			buffer = InternalWritableBuffer.dynamicOf(_broadcastCacheBuffer, client.getResourcePool(), getClass());
		}
		
		return buffer;
	}
	
	/**
	 * Writes packet data to a new buffer for the specified client.<br>
	 * This method initializes the buffer's position and releases resources if writing fails.
	 * @param client The client to whom the packet data is being written.
	 * @return An {@link InternalWritableBuffer} containing the packet data.
	 * @throws Exception If an error occurs during writing.
	 */
	private InternalWritableBuffer writeDataToBuffer(T client) throws Exception
	{
		final InternalWritableBuffer buffer = choosePacketBuffer(client);
		buffer.position(ConnectionConfig.HEADER_SIZE);
		if (write(client, buffer))
		{
			buffer.mark();
			return buffer;
		}
		
		buffer.releaseResources();
		throw new Exception();
	}
	
	/**
	 * Chooses an appropriate buffer based on whether the packet is marked as broadcast.
	 * @param client The client for whom the buffer is being chosen.
	 * @return An {@link InternalWritableBuffer} suitable for the packet type.
	 */
	private InternalWritableBuffer choosePacketBuffer(T client)
	{
		if (_broadcast)
		{
			return InternalWritableBuffer.arrayBacked(client.getResourcePool(), getClass());
		}
		
		return InternalWritableBuffer.dynamicOf(client.getResourcePool(), getClass());
	}
	
	/**
	 * Writes the header to the specified buffer.
	 * @param buffer The buffer to which the header is written.
	 * @param header The header value to write.
	 */
	public void writeHeader(InternalWritableBuffer buffer, int header)
	{
		buffer.writeShort(0, (short) header);
	}
	
	/**
	 * Mark this packet as broadcast. A broadcast packet is sent to more than one client.<br>
	 * Caution: This method should be called before {@link Client#writePacket(WritablePacket)}.<br>
	 * A broadcast packet will create a Buffer cache where the data is written once and only the copy is sent to the client.
	 * @implNote Each copy will be encrypted to each client.
	 */
	public void sendInBroadcast()
	{
		_broadcast = true;
	}
	
	/**
	 * If this method returns true, the packet will be considered disposable.
	 * @param client client to send data to
	 * @return if the packet is disposable or not.
	 */
	public boolean canBeDropped(T client)
	{
		return false;
	}
	
	/**
	 * Writes the data to the buffer for the specified client.<br>
	 * This abstract method should be implemented in subclasses to define the specific packet writing logic.
	 * @param client The client to whom the packet is being sent.
	 * @param buffer The writable buffer where the packet data is written.
	 * @return true if the packet was successfully written, false otherwise.
	 */
	protected abstract boolean write(T client, WritableBuffer buffer);
	
	/**
	 * Converts this packet to a string representation.
	 * @return The simple name of the packet's class.
	 */
	@Override
	public String toString()
	{
		return getClass().getSimpleName();
	}
}
