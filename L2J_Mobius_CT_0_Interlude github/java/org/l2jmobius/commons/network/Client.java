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

import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.l2jmobius.commons.network.internal.InternalWritableBuffer;

/**
 * Represents a generic client in a network context.<br>
 * This abstract class provides the foundation for managing connections, sending and receiving data packets, and handling connection states.<br>
 * It requires implementation of encryption, decryption, and connection management methods.
 * @param <T> The type of Connection associated with this client.
 * @author JoeAlisson, Mobius
 */
public abstract class Client<T extends Connection<?>>
{
	private static final ConcurrentLinkedQueue<Client<?>> PENDING_CLIENTS = new ConcurrentLinkedQueue<>();
	
	private final T _connection;
	private final Queue<WritablePacket<? extends Client<T>>> _packetsToWrite = new ConcurrentLinkedQueue<>();
	private final AtomicBoolean _writing = new AtomicBoolean();
	private final AtomicBoolean _disconnecting = new AtomicBoolean();
	private final AtomicBoolean _closing = new AtomicBoolean();
	private final AtomicInteger _estimateQueueSize = new AtomicInteger();
	private final AtomicInteger _dataSentSize = new AtomicInteger();
	private boolean _readingPayload;
	private int _expectedReadSize;
	
	/**
	 * Constructs a new Client using the specified connection.
	 * @param connection The Connection to the client.
	 * @throws IllegalArgumentException if the connection is null or closed.
	 */
	protected Client(T connection)
	{
		if ((connection == null) || !connection.isOpen())
		{
			throw new IllegalArgumentException("The connection is null or closed.");
		}
		
		_connection = connection;
	}
	
	/**
	 * Sends a packet to this client.<br>
	 * If another packet is being sent, the actual packet is queued to be sent after all previous packets.
	 * @param packet The packet to be sent.
	 */
	protected void writePacket(WritablePacket<? extends Client<T>> packet)
	{
		if (!isConnected() || (packet == null) || packetCanBeDropped(packet))
		{
			return;
		}
		
		_estimateQueueSize.incrementAndGet();
		_packetsToWrite.add(packet);
		writeFairPacket();
	}
	
	/**
	 * Determines if a packet can be dropped based on the connection's drop packet settings.
	 * @param packet The packet to check.
	 * @return True if the packet can be dropped, false otherwise.
	 */
	@SuppressWarnings(
	{
		"unchecked",
		"rawtypes"
	})
	private boolean packetCanBeDropped(WritablePacket packet)
	{
		return _connection.dropPackets() && (_estimateQueueSize.get() > _connection.dropPacketThreshold()) && packet.canBeDropped(this);
	}
	
	/**
	 * Sends multiple packets to this client, queuing them if needed.
	 * @param packets The collection of packets to be sent.
	 */
	protected void writePackets(Collection<WritablePacket<? extends Client<T>>> packets)
	{
		if (!isConnected() || (packets == null) || packets.isEmpty())
		{
			return;
		}
		
		_estimateQueueSize.addAndGet(packets.size());
		_packetsToWrite.addAll(packets);
		writeFairPacket();
	}
	
	/**
	 * Attempts to initiate a fair packet write operation, ensuring only one write operation occurs at a time.<br>
	 * This method starts the packet-sending process.
	 */
	private void writeFairPacket()
	{
		if (_writing.compareAndSet(false, true))
		{
			sendFairPacket();
		}
	}
	
	/**
	 * Writes the next packet in the queue.<br>
	 * If no packets are left, releases resources associated with the write operation and disconnects if the client is closing.
	 */
	private void writeNextPacket()
	{
		final WritablePacket<? extends Client<T>> packet = _packetsToWrite.poll();
		if (packet == null)
		{
			releaseWritingResource();
			if (_closing.get())
			{
				disconnect();
			}
		}
		else
		{
			_estimateQueueSize.decrementAndGet();
			write(packet);
		}
	}
	
	/**
	 * Sends a packet fairly among pending clients, ensuring fair access to the network resources.<br>
	 * This method takes the next client in the queue and initiates its packet sending.
	 */
	private void sendFairPacket()
	{
		PENDING_CLIENTS.offer(this);
		
		final Client<?> nextClient = PENDING_CLIENTS.poll();
		if (nextClient != null)
		{
			nextClient.writeNextPacket();
		}
	}
	
	/**
	 * Writes a specified packet to the connection. Encrypts the data, writes headers and manages the buffer.<br>
	 * If the packet cannot be written, it handles resource release and retries.
	 * @param packet The packet to be written.
	 */
	@SuppressWarnings(
	{
		"unchecked",
		"rawtypes"
	})
	private void write(WritablePacket packet)
	{
		boolean written = false;
		InternalWritableBuffer buffer = null;
		try
		{
			buffer = packet.writeData(this);
			
			final int payloadSize = buffer.limit() - ConnectionConfig.HEADER_SIZE;
			if (payloadSize <= 0)
			{
				return;
			}
			
			if (encrypt(buffer, ConnectionConfig.HEADER_SIZE, payloadSize))
			{
				final int bufferLimit = buffer.limit();
				_dataSentSize.set(bufferLimit);
				if (bufferLimit <= ConnectionConfig.HEADER_SIZE)
				{
					return;
				}
				
				packet.writeHeader(buffer, bufferLimit);
				written = _connection.write(buffer.toByteBuffers());
			}
		}
		catch (Exception e)
		{
			// Placeholder for handling/logging Exception if needed.
		}
		finally
		{
			if (!written)
			{
				handleNotWritten(buffer);
			}
		}
	}
	
	/**
	 * Handles scenarios where a packet could not be written successfully.<br>
	 * Releases any associated buffer resources and re-attempts the packet send if the client is still connected.
	 * @param buffer The buffer containing packet data, which may need resource release.
	 */
	private void handleNotWritten(InternalWritableBuffer buffer)
	{
		if (!releaseWritingResource() && (buffer != null))
		{
			buffer.releaseResources();
		}
		
		if (isConnected())
		{
			writeFairPacket();
		}
	}
	
	/**
	 * Begins reading data from the connection.
	 */
	public void read()
	{
		_expectedReadSize = ConnectionConfig.HEADER_SIZE;
		_readingPayload = false;
		_connection.readHeader();
	}
	
	/**
	 * Reads the payload of the specified data size from the connection.
	 * @param dataSize The size of the data to be read.
	 */
	public void readPayload(int dataSize)
	{
		_expectedReadSize = dataSize;
		_readingPayload = true;
		_connection.read(dataSize);
	}
	
	/**
	 * Close the underlying Connection to the client.<br>
	 * All pending packets are cancelled.
	 */
	public void close()
	{
		close(null);
	}
	
	/**
	 * Sends the packet and close the underlying Connection to the client.<br>
	 * All others pending packets are cancelled.
	 * @param packet to be sent before the connection is closed.
	 */
	public void close(WritablePacket<? extends Client<T>> packet)
	{
		if (!isConnected())
		{
			return;
		}
		
		_packetsToWrite.clear();
		if (packet != null)
		{
			_packetsToWrite.add(packet);
		}
		
		_closing.set(true);
		
		writeFairPacket();
	}
	
	/**
	 * Resumes sending data after a specified amount has been successfully sent.
	 * @param result The number of bytes sent.
	 */
	public void resumeSend(int result)
	{
		_dataSentSize.addAndGet(-result);
		_connection.write();
	}
	
	/**
	 * Completes the current writing operation and prepares to send the next packet.
	 */
	public void finishWriting()
	{
		_connection.releaseWritingBuffer();
		sendFairPacket();
	}
	
	private boolean releaseWritingResource()
	{
		final boolean released = _connection.releaseWritingBuffer();
		_writing.set(false);
		return released;
	}
	
	/**
	 * Disconnects the client, releasing resources associated with the connection.
	 */
	public void disconnect()
	{
		if (_disconnecting.compareAndSet(false, true))
		{
			try
			{
				onDisconnection();
			}
			finally
			{
				_packetsToWrite.clear();
				_connection.close();
			}
		}
	}
	
	/**
	 * Retrieves the connection associated with this client.
	 * @return The connection.
	 */
	public T getConnection()
	{
		return _connection;
	}
	
	public int getDataSentSize()
	{
		return _dataSentSize.get();
	}
	
	/**
	 * Retrieves the client's IP address.
	 * @return The client's IP address as a string.
	 */
	public String getHostAddress()
	{
		return _connection == null ? "" : _connection.getRemoteAddress();
	}
	
	/**
	 * Checks if the client is still connected.
	 * @return {@code true} if connected, {@code false} otherwise.
	 */
	public boolean isConnected()
	{
		return _connection.isOpen() && !_closing.get();
	}
	
	/**
	 * Retrieves an estimate of the queue size for packets to be sent.
	 * @return The estimated queue size.
	 */
	public int getEstimateQueueSize()
	{
		return _estimateQueueSize.get();
	}
	
	/**
	 * Retrieves the resource pool associated with the client's connection.
	 * @return The {@link ResourcePool} used by the connection.
	 */
	public ResourcePool getResourcePool()
	{
		return _connection.getResourcePool();
	}
	
	/**
	 * Checks if the client is currently reading payload data.
	 * @return True if reading payload, false otherwise.
	 */
	public boolean isReadingPayload()
	{
		return _readingPayload;
	}
	
	/**
	 * Resumes reading operation with the specified number of bytes read.
	 * @param bytesRead The number of bytes read.
	 */
	public void resumeRead(int bytesRead)
	{
		_expectedReadSize -= bytesRead;
		_connection.read();
	}
	
	/**
	 * Retrieves the total size of data sent in the current session.
	 * @return The size of data sent.
	 */
	public int getExpectedReadSize()
	{
		return _expectedReadSize;
	}
	
	/**
	 * Encrypts the specified data in-place.
	 * @param data The data to be encrypted.
	 * @param offset The initial index of the data to encrypt.
	 * @param size The length of data to encrypt.
	 * @return True if the data was successfully encrypted, false otherwise.
	 */
	public abstract boolean encrypt(Buffer data, int offset, int size);
	
	/**
	 * Decrypts the specified data in-place.
	 * @param data The data to be decrypted.
	 * @param offset The initial index of the data to decrypt.
	 * @param size The length of data to decrypt.
	 * @return True if the data was successfully decrypted, false otherwise.
	 */
	public abstract boolean decrypt(Buffer data, int offset, int size);
	
	/**
	 * Handles the client's disconnection.<br>
	 * This method must save all data and release all resources related to the client.<br>
	 * No more packet can be sent after this method is called.
	 */
	protected abstract void onDisconnection();
	
	/**
	 * Handles the client's connection.<br>
	 * This method should not use blocking operations.<br>
	 * The Packets can be sent only after this method is called.
	 */
	public abstract void onConnected();
}
