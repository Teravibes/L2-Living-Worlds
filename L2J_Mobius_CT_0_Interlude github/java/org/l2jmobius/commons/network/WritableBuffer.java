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

import java.nio.charset.StandardCharsets;

/**
 * Represents a buffer capable of writing various data types.<br>
 * This abstract class provides methods to write data in different formats to the buffer, including primitives and strings.
 * @author JoeAlisson
 */
public abstract class WritableBuffer implements Buffer
{
	/**
	 * Write <b>char</b> to the buffer.<br>
	 * 16 bit char
	 * @param value the char to be put on data.
	 */
	public abstract void writeChar(char value);
	
	/**
	 * Write a <b>byte</b> to the buffer.<br>
	 * 8bit integer (00) If the underlying data array can't hold a new byte its size is increased 20%
	 * @param value to be written.
	 */
	public abstract void writeByte(byte value);
	
	/**
	 * Write a int to the buffer, the int is casted to a byte.
	 * @param value to be written.
	 */
	public void writeByte(int value)
	{
		writeByte((byte) value);
	}
	
	/**
	 * Writes a boolean value to the buffer as a byte.<br>
	 * Writes 1 if true, otherwise 0. 8-bit integer (00)
	 * @param value The boolean value to be written.
	 */
	public void writeByte(boolean value)
	{
		writeByte((byte) (value ? 1 : 0));
	}
	
	/**
	 * Writes a byte array to the buffer.<br>
	 * 8-bit integer array (00 ...)
	 * @param value The byte array to be written.
	 */
	public abstract void writeBytes(byte... value);
	
	/**
	 * Writes a short value to the buffer.<br>
	 * 16-bit integer (00 00)
	 * @param value The short value to be written.
	 */
	public abstract void writeShort(short value);
	
	/**
	 * Write <b>short</b> to the buffer.<br>
	 * 16bit integer (00 00)
	 * @param value to be written.
	 */
	public void writeShort(int value)
	{
		writeShort((short) value);
	}
	
	/**
	 * Writes a boolean value to the buffer as a short.<br>
	 * Writes 1 if true, otherwise 0. 16-bit integer (00 00)
	 * @param value The boolean value to be written.
	 */
	public void writeShort(boolean value)
	{
		writeShort((short) (value ? 1 : 0));
	}
	
	/**
	 * Write <b>int</b> to the buffer.<br>
	 * 32bit integer (00 00 00 00)
	 * @param value to be written.
	 */
	public abstract void writeInt(int value);
	
	/**
	 * Write <b>boolean</b> to the buffer.<br>
	 * If the value is true so write a <b>byte</b> with value 1, otherwise 0 32bit integer (00 00 00 00)
	 * @param value to be written.
	 */
	public void writeInt(boolean value)
	{
		writeInt(value ? 1 : 0);
	}
	
	/**
	 * Write <b>float</b> to the buffer.<br>
	 * 32bit float point number (00 00 00 00)
	 * @param value to be written.
	 */
	public abstract void writeFloat(float value);
	
	/**
	 * Write <b>long</b> to the buffer.<br>
	 * 64bit integer (00 00 00 00 00 00 00 00)
	 * @param value to be written.
	 */
	public abstract void writeLong(long value);
	
	/**
	 * Write <b>double</b> to the buffer.<br>
	 * 64bit double precision float (00 00 00 00 00 00 00 00)
	 * @param value to be written.
	 */
	public abstract void writeDouble(double value);
	
	/**
	 * Write a <b>String</b> to the buffer with a null termination (\000). Each character is a 16bit char
	 * @param text to be written.
	 */
	public void writeString(CharSequence text)
	{
		if (text == null)
		{
			writeChar('\000');
			return;
		}
		
		writeStringWithCharset(text);
		writeChar('\000');
	}
	
	private void writeStringWithCharset(CharSequence text)
	{
		writeBytes(text.toString().getBytes(StandardCharsets.UTF_16LE));
	}
	
	/**
	 * Writes a String to the buffer preceded by a short 16-bit indicating the string's length, without a null termination.<br>
	 * Each character is a 16-bit char.
	 * @param text The text to be written.
	 */
	public void writeSizedString(CharSequence text)
	{
		if ((text != null) && (text.length() > 0))
		{
			writeShort(text.length());
			writeStringWithCharset(text);
		}
		else
		{
			writeShort(0);
		}
	}
}
