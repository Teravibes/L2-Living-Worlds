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
 * Responsible for handling incoming data and converting it into a readable packet.<br>
 * This functional interface defines a method to process raw data from a buffer and create a corresponding packet instance.
 * @param <T> The type of the client associated with the packet handler.
 * @author JoeAlisson
 */
@FunctionalInterface
public interface PacketHandler<T extends Client<Connection<T>>>
{
	/**
	 * Converts the data in the buffer into a readable packet.<br>
	 * This method should interpret the raw data and construct a packet instance that represents the data for further processing.
	 * @param buffer The buffer containing the data to be converted.
	 * @param client The client who sent the data.
	 * @return A {@link ReadablePacket} corresponding to the data received.
	 */
	ReadablePacket<T> handlePacket(ReadableBuffer buffer, T client);
}
