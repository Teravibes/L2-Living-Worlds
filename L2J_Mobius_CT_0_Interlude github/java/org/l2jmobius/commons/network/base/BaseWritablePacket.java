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
package org.l2jmobius.commons.network.base;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract class for writable packets backed by a byte array, with a maximum raw data size of 65533 bytes.<br>
 * Provides methods to write various types of data to the packet.
 * @author Mobius
 * @since October 29th 2020
 */
public abstract class BaseWritablePacket
{
	private static final Map<Class<?>, Integer> MAXIMUM_PACKET_SIZE = new ConcurrentHashMap<>();
	
	private final int _initialSize = MAXIMUM_PACKET_SIZE.getOrDefault(getClass(), 8);
	
	private byte[] _data;
	private byte[] _sendableBytes;
	private int _position = 2; // Allocate space for size (max length 65535 - size header).
	
	protected BaseWritablePacket()
	{
		_data = new byte[_initialSize];
	}
	
	public void write(byte value)
	{
		// Check current size.
		if (_position < 65535)
		{
			// Check capacity.
			if (_position == _data.length)
			{
				_data = Arrays.copyOf(_data, _data.length * 2); // Double the capacity.
			}
			
			// Set value.
			_data[_position++] = value;
			return;
		}
		
		throw new IndexOutOfBoundsException("Packet data exceeded the raw data size limit of 65533!");
	}
	
	/**
	 * Write <b>boolean</b> to the packet data.<br>
	 * 8bit integer (00) or (01)
	 * @param value
	 */
	public void writeBoolean(boolean value)
	{
		writeByte(value ? 1 : 0);
	}
	
	/**
	 * Write <b>String</b> to the packet data.
	 * @param text
	 */
	public void writeString(String text)
	{
		if (text != null)
		{
			writeBytes(text.getBytes(StandardCharsets.UTF_16LE));
		}
		
		writeShort(0);
	}
	
	/**
	 * Write <b>String</b> with fixed size specified as (short size, char[size]) to the packet data.
	 * @param text
	 */
	public void writeSizedString(String text)
	{
		if (text != null)
		{
			writeShort(text.length());
			writeBytes(text.getBytes(StandardCharsets.UTF_16LE));
		}
		else
		{
			writeShort(0);
		}
	}
	
	/**
	 * Write <b>byte[]</b> to the packet data.<br>
	 * 8bit integer array (00...)
	 * @param array
	 */
	public void writeBytes(byte[] array)
	{
		for (int i = 0; i < array.length; i++)
		{
			write(array[i]);
		}
	}
	
	/**
	 * Write <b>byte</b> to the packet data.<br>
	 * 8bit integer (00)
	 * @param value
	 */
	public void writeByte(int value)
	{
		write((byte) (value & 0xff));
	}
	
	/**
	 * Write <b>boolean</b> to the packet data.<br>
	 * 8bit integer (00) or (01)
	 * @param value
	 */
	public void writeByte(boolean value)
	{
		writeByte(value ? 1 : 0);
	}
	
	/**
	 * Write <b>short</b> to the packet data.<br>
	 * 16bit integer (00 00)
	 * @param value
	 */
	public void writeShort(int value)
	{
		write((byte) (value & 0xff));
		write((byte) ((value >> 8) & 0xff));
	}
	
	/**
	 * Write <b>boolean</b> to the packet data.<br>
	 * 16bit integer (00 00)
	 * @param value
	 */
	public void writeShort(boolean value)
	{
		writeShort(value ? 1 : 0);
	}
	
	/**
	 * Write <b>int</b> to the packet data.<br>
	 * 32bit integer (00 00 00 00)
	 * @param value
	 */
	public void writeInt(int value)
	{
		write((byte) (value & 0xff));
		write((byte) ((value >> 8) & 0xff));
		write((byte) ((value >> 16) & 0xff));
		write((byte) ((value >> 24) & 0xff));
	}
	
	/**
	 * Write <b>boolean</b> to the packet data.<br>
	 * 32bit integer (00 00 00 00)
	 * @param value
	 */
	public void writeInt(boolean value)
	{
		writeInt(value ? 1 : 0);
	}
	
	/**
	 * Write <b>long</b> to the packet data.<br>
	 * 64bit integer (00 00 00 00 00 00 00 00)
	 * @param value
	 */
	public void writeLong(long value)
	{
		write((byte) (value & 0xff));
		write((byte) ((value >> 8) & 0xff));
		write((byte) ((value >> 16) & 0xff));
		write((byte) ((value >> 24) & 0xff));
		write((byte) ((value >> 32) & 0xff));
		write((byte) ((value >> 40) & 0xff));
		write((byte) ((value >> 48) & 0xff));
		write((byte) ((value >> 56) & 0xff));
	}
	
	/**
	 * Write <b>boolean</b> to the packet data.<br>
	 * 64bit integer (00 00 00 00 00 00 00 00)
	 * @param value
	 */
	public void writeLong(boolean value)
	{
		writeLong(value ? 1 : 0);
	}
	
	/**
	 * Write <b>float</b> to the packet data.<br>
	 * 32bit single precision float (00 00 00 00)
	 * @param value
	 */
	public void writeFloat(float value)
	{
		writeInt(Float.floatToRawIntBits(value));
	}
	
	/**
	 * Write <b>double</b> to the packet data.<br>
	 * 64bit double precision float (00 00 00 00 00 00 00 00)
	 * @param value
	 */
	public void writeDouble(double value)
	{
		writeLong(Double.doubleToRawLongBits(value));
	}
	
	/**
	 * Can be overridden to write data after packet has initialized.<br>
	 * Called when getSendableBytes generates data, ensures that the data are processed only once.
	 */
	public void write()
	{
		// Overridden by server implementation.
	}
	
	/**
	 * Returns the byte array containing the packet's data, including the size header.<br>
	 * This method should be called after all data has been written to the packet.
	 * @return Byte array of the sendable packet data.
	 */
	public synchronized byte[] getSendableBytes()
	{
		// Generate sendable byte array.
		if (_sendableBytes == null /* Not processed */)
		{
			// Write packet implementation (only once).
			if (_position == 2)
			{
				write();
				
				// Update maximum packet size if needed.
				if (_position > _initialSize)
				{
					MAXIMUM_PACKET_SIZE.put(getClass(), Math.min(_position, 65535));
				}
			}
			
			// Check if data was written.
			if (_position > 2)
			{
				// Trim array of data.
				_sendableBytes = Arrays.copyOf(_data, _position);
				
				// Add size info at start (unsigned short - max size 65535).
				_sendableBytes[0] = (byte) (_position & 0xff);
				_sendableBytes[1] = (byte) ((_position >> 8) & 0xffff);
			}
		}
		
		// Return the data.
		return _sendableBytes;
	}
	
	/**
	 * Gets the length of the data written to the packet, including the size header.<br>
	 * Note that the data must be written first before calling this method.
	 * @return The length of the data.
	 */
	public int getLength()
	{
		return _position;
	}
}
