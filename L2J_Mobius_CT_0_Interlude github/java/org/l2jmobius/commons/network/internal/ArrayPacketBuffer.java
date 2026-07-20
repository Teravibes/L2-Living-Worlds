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
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.commons.network.ReadableBuffer;
import org.l2jmobius.commons.network.ResourcePool;

/**
 * An implementation of a packet buffer backed by a byte array.<br>
 * This class extends {@link InternalWritableBuffer} and implements {@link ReadableBuffer}, providing methods for writing to and reading from a byte array.
 * @author JoeAlisson, Mobius
 */
public class ArrayPacketBuffer extends InternalWritableBuffer implements ReadableBuffer
{
	private static final Map<Class<?>, Integer> MAXIMUM_PACKET_SIZE = new ConcurrentHashMap<>();
	
	private final ResourcePool _resourcePool;
	private final Class<?> _packetClass;
	private final int _initialSize;
	
	private byte[] _data;
	private int _index;
	private int _limit;
	
	/**
	 * Create a ArrayPacketBuffer
	 * @param resourcePool the resource pool used to get ByteBuffers
	 * @param packetClass the Class<?> of the writable packet
	 */
	public ArrayPacketBuffer(ResourcePool resourcePool, Class<?> packetClass)
	{
		_resourcePool = resourcePool;
		_packetClass = packetClass;
		_initialSize = MAXIMUM_PACKET_SIZE.getOrDefault(packetClass, resourcePool.getSegmentSize());
		_data = new byte[_initialSize];
	}
	
	private void ensureSize(int size)
	{
		if (_data.length < size)
		{
			_data = Arrays.copyOf(_data, (int) ((_data.length + size) * 1.2));
			_limit = _data.length;
		}
	}
	
	@Override
	public void writeChar(char value)
	{
		writeShort((short) value);
	}
	
	@Override
	public void writeByte(byte value)
	{
		writeByte(_index++, value);
	}
	
	@Override
	public void writeByte(int index, byte value)
	{
		ensureSize(index + 1);
		_data[index] = value;
	}
	
	@Override
	public void writeBytes(byte... bytes)
	{
		if (bytes == null)
		{
			return;
		}
		
		ensureSize(_index + bytes.length);
		System.arraycopy(bytes, 0, _data, _index, bytes.length);
		_index += bytes.length;
	}
	
	@Override
	public void writeShort(short value)
	{
		writeShort(_index, value);
		_index += 2;
	}
	
	@Override
	public void writeShort(int index, short value)
	{
		ensureSize(index + 2);
		_data[index++] = (byte) value;
		_data[index] = (byte) (value >>> 8);
	}
	
	@Override
	public void writeInt(int value)
	{
		writeInt(_index, value);
		_index += 4;
	}
	
	@Override
	public void writeInt(int index, int value)
	{
		ensureSize(index + 4);
		_data[index++] = (byte) value;
		_data[index++] = (byte) (value >>> 8);
		_data[index++] = (byte) (value >>> 16);
		_data[index] = (byte) (value >>> 24);
	}
	
	@Override
	public void writeFloat(float value)
	{
		writeInt(Float.floatToRawIntBits(value));
	}
	
	@Override
	public void writeLong(long value)
	{
		ensureSize(_index + 8);
		_data[_index++] = (byte) value;
		_data[_index++] = (byte) (value >>> 8);
		_data[_index++] = (byte) (value >>> 16);
		_data[_index++] = (byte) (value >>> 24);
		_data[_index++] = (byte) (value >>> 32);
		_data[_index++] = (byte) (value >>> 40);
		_data[_index++] = (byte) (value >>> 48);
		_data[_index++] = (byte) (value >>> 56);
	}
	
	@Override
	public void writeDouble(double value)
	{
		writeLong(Double.doubleToRawLongBits(value));
	}
	
	@Override
	public int position()
	{
		return _index;
	}
	
	@Override
	public void position(int pos)
	{
		_index = pos;
	}
	
	@Override
	public char readChar()
	{
		return (char) readShort();
	}
	
	@Override
	public byte readByte()
	{
		return _data[_index++];
	}
	
	@Override
	public byte readByte(int index)
	{
		return _data[index];
	}
	
	private int readUnsigned(int index)
	{
		return Byte.toUnsignedInt(_data[index]);
	}
	
	@Override
	public short readShort()
	{
		return (short) (readUnsigned(_index++) | (readUnsigned(_index++) << 8));
	}
	
	@Override
	public short readShort(int index)
	{
		return (short) (readUnsigned(index++) | (readUnsigned(index) << 8));
	}
	
	@Override
	public int readInt()
	{
		return readUnsigned(_index++) | (readUnsigned(_index++) << 8) | (readUnsigned(_index++) << 16) | (readUnsigned(_index++) << 24);
	}
	
	@Override
	public float readFloat()
	{
		return Float.intBitsToFloat(readInt());
	}
	
	@Override
	public long readLong()
	{
		return Byte.toUnsignedLong(_data[_index++]) | (Byte.toUnsignedLong(_data[_index++]) << 8) | (Byte.toUnsignedLong(_data[_index++]) << 16) | (Byte.toUnsignedLong(_data[_index++]) << 24) | (Byte.toUnsignedLong(_data[_index++]) << 32) | (Byte.toUnsignedLong(_data[_index++]) << 40) | (Byte.toUnsignedLong(_data[_index++]) << 48) | (Byte.toUnsignedLong(_data[_index++]) << 56);
	}
	
	@Override
	public double readDouble()
	{
		return Double.longBitsToDouble(readLong());
	}
	
	@Override
	public byte[] readBytes(int length)
	{
		final byte[] result = new byte[length];
		readBytes(result, 0, length);
		return result;
	}
	
	@Override
	public void readBytes(byte[] dst)
	{
		readBytes(dst, 0, dst.length);
	}
	
	@Override
	public void readBytes(byte[] dst, int offset, int length)
	{
		System.arraycopy(_data, _index, dst, offset, length);
		_index += length;
	}
	
	@Override
	public int readInt(int index)
	{
		return readUnsigned(index++) | (readUnsigned(index++) << 8) | (readUnsigned(index++) << 16) | (readUnsigned(index) << 24);
	}
	
	@Override
	public int limit()
	{
		return _limit;
	}
	
	@Override
	public void limit(int newLimit)
	{
		ensureSize(newLimit);
		_limit = newLimit;
	}
	
	@Override
	public void mark()
	{
		_limit = _index;
	}
	
	@Override
	public ByteBuffer[] toByteBuffers()
	{
		return new ByteBuffer[]
		{
			toByteBuffer()
		};
	}
	
	public ByteBuffer toByteBuffer()
	{
		// Update maximum packet size if needed.
		if (_limit > _initialSize)
		{
			MAXIMUM_PACKET_SIZE.put(_packetClass, Math.min(_limit, 65535));
		}
		
		final ByteBuffer buffer = _resourcePool.getBuffer(_limit);
		buffer.put(_data, 0, _limit);
		return buffer.flip();
	}
	
	@Override
	public void releaseResources()
	{
		_index = 0;
		_limit = _data.length;
	}
	
	@Override
	public int remaining()
	{
		return _limit - _index;
	}
}
