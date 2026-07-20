/*
 * Copyright © 2019-2021 Async-mmocore
 *
 * This file is part of the Async-mmocore project.
 *
 * Async-mmocore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Async-mmocore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.commons.network;

import java.nio.ByteBuffer;

import org.l2jmobius.commons.network.internal.SinglePacketBuffer;

/**
 * Represents a buffer that can be read in various formats.<br>
 * This interface extends {@link Buffer} and provides methods to read data as different primitives from the buffer.
 * @author JoeAlisson
 */
public interface ReadableBuffer extends Buffer
{
	/**
	 * Reads a char value from the buffer.<br>
	 * 16-bit integer (00 00)
	 * @return The char value read from the buffer.
	 */
	char readChar();
	
	/**
	 * Reads a raw byte from the buffer.
	 * @return The byte read from the buffer.
	 */
	byte readByte();
	
	/**
	 * Reads and returns an array of bytes of the specified length from the internal buffer.
	 * @param length The given length of bytes to be read.
	 * @return A byte array containing the read bytes.
	 */
	byte[] readBytes(int length);
	
	/**
	 * Reads bytes into the specified byte array.
	 * @param dst The byte array to fill with data.
	 */
	void readBytes(byte[] dst);
	
	/**
	 * Reads bytes into the specified byte array, starting at the given offset and reading up to the specified length.
	 * @param dst The byte array to fill with data.
	 * @param offset The starting offset in the array.
	 * @param length The number of bytes to read.
	 */
	void readBytes(byte[] dst, int offset, int length);
	
	/**
	 * Reads a short value from the buffer.<br>
	 * 16-bit integer (00 00)
	 * @return The short value read from the buffer.
	 */
	short readShort();
	
	/**
	 * Reads an int value from the buffer.<br>
	 * 32-bit integer (00 00 00 00)
	 * @return The int value read from the buffer.
	 */
	int readInt();
	
	/**
	 * Reads a long value from the buffer.<br>
	 * 64-bit integer (00 00 00 00 00 00 00 00)
	 * @return The long value read from the buffer.
	 */
	long readLong();
	
	/**
	 * Reads a float value from the buffer.<br>
	 * 32-bit float (00 00 00 00)
	 * @return The float value read from the buffer.
	 */
	float readFloat();
	
	/**
	 * Reads a double value from the buffer.<br>
	 * 64-bit float (00 00 00 00 00 00 00 00)
	 * @return The double value read from the buffer.
	 */
	double readDouble();
	
	/**
	 * Returns the number of remaining bytes available for reading.
	 * @return The number of remaining bytes.
	 */
	int remaining();
	
	/**
	 * Creates a new {@link ReadableBuffer} based on the given {@link ByteBuffer}.
	 * @param buffer The underlying ByteBuffer.
	 * @return A new instance of ReadableBuffer.
	 */
	static ReadableBuffer of(ByteBuffer buffer)
	{
		return new SinglePacketBuffer(buffer);
	}
}
