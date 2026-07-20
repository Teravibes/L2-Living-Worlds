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
package org.l2jmobius.commons.network.internal;

import java.nio.ByteBuffer;

import org.l2jmobius.commons.network.ReadableBuffer;

/**
 * A wrapper around {@link ByteBuffer} that implements {@link ReadableBuffer},<br>
 * providing methods to read different types of data from the underlying byte buffer.
 * @author JoeAlisson
 */
public class SinglePacketBuffer implements ReadableBuffer
{
	private final ByteBuffer _buffer;
	
	public SinglePacketBuffer(ByteBuffer buffer)
	{
		_buffer = buffer;
	}
	
	@Override
	public char readChar()
	{
		return _buffer.getChar();
	}
	
	@Override
	public byte readByte()
	{
		return _buffer.get();
	}
	
	@Override
	public byte readByte(int index)
	{
		return _buffer.get(index);
	}
	
	@Override
	public byte[] readBytes(int length)
	{
		final byte[] result = new byte[length];
		_buffer.get(result);
		return result;
	}
	
	@Override
	public void readBytes(byte[] dst)
	{
		_buffer.get(dst);
	}
	
	@Override
	public void readBytes(byte[] dst, int offset, int length)
	{
		_buffer.get(dst, offset, length);
	}
	
	@Override
	public short readShort()
	{
		return _buffer.getShort();
	}
	
	@Override
	public short readShort(int index)
	{
		return _buffer.getShort(index);
	}
	
	@Override
	public int readInt()
	{
		return _buffer.getInt();
	}
	
	@Override
	public int readInt(int index)
	{
		return _buffer.getInt(index);
	}
	
	@Override
	public float readFloat()
	{
		return _buffer.getFloat();
	}
	
	@Override
	public long readLong()
	{
		return _buffer.getLong();
	}
	
	@Override
	public double readDouble()
	{
		return _buffer.getDouble();
	}
	
	@Override
	public void writeByte(int index, byte value)
	{
		_buffer.put(index, value);
	}
	
	@Override
	public void writeShort(int index, short value)
	{
		_buffer.putShort(index, value);
	}
	
	@Override
	public void writeInt(int index, int value)
	{
		_buffer.putInt(index, value);
	}
	
	@Override
	public int limit()
	{
		return _buffer.limit();
	}
	
	@Override
	public void limit(int newLimit)
	{
		_buffer.limit(newLimit);
	}
	
	@Override
	public int remaining()
	{
		return _buffer.remaining();
	}
}
