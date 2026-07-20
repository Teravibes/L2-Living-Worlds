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

/**
 * Represents a buffer for reading and writing different data types.<br>
 * This interface provides methods to read and write primitive data types<br>
 * like byte, short, and int at specific indices within the buffer.<br>
 * Additionally, methods to get and set the buffer's limit are provided.<br>
 * @author JoeAlisson
 */
public interface Buffer
{
	/**
	 * Reads a byte value from the buffer at the specified index.
	 * @param index The index from where the byte should be read.
	 * @return The byte value at the specified index.
	 */
	byte readByte(int index);
	
	/**
	 * Writes a byte value to the buffer at the specified index.
	 * @param index The index at which the byte value should be written.
	 * @param value The byte value to be written.
	 */
	void writeByte(int index, byte value);
	
	/**
	 * Reads a short value (16-bit integer) from the buffer at the specified index.
	 * @param index The index from where the short should be read.
	 * @return The short value at the specified index.
	 */
	short readShort(int index);
	
	/**
	 * Writes a short value (16-bit integer) to the buffer at the specified index.
	 * @param index The index at which the short value should be written.
	 * @param value The short value to be written.
	 */
	void writeShort(int index, short value);
	
	/**
	 * Reads an int value (32-bit integer) from the buffer at the specified index.
	 * @param index The index from where the int should be read.
	 * @return The int value at the specified index.
	 */
	int readInt(int index);
	
	/**
	 * Writes an int value (32-bit integer) to the buffer at the specified index.
	 * @param index The index at which the int value should be written.
	 * @param value The int value to be written.
	 */
	void writeInt(int index, int value);
	
	/**
	 * Retrieves the current limit of the buffer.
	 * @return The buffer's current limit.
	 */
	int limit();
	
	/**
	 * Sets a new limit for the buffer.
	 * @param newLimit The new limit to be set for the buffer.
	 */
	void limit(int newLimit);
}
