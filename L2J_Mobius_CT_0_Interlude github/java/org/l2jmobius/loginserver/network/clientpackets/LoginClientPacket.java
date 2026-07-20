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
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.loginserver.network.clientpackets;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.network.ReadablePacket;
import org.l2jmobius.loginserver.network.LoginClient;

/**
 * Base implementation for all incoming login client packets.<br>
 * Provides a common, exception safe wrapper around the packet body parsing.
 * <ul>
 * <li>Delegates packet parsing to subclasses via {@link #readImpl()}.</li>
 * <li>Catches and logs unexpected exceptions during packet read.</li>
 * <li>Associates each packet with a {@link LoginClient} instance.</li>
 * </ul>
 * @author BazookaRpm
 */
public abstract class LoginClientPacket extends ReadablePacket<LoginClient>
{
	// Logger.
	private static final Logger LOGGER = Logger.getLogger(LoginClientPacket.class.getName());
	
	/**
	 * Executes the packet read logic with centralized exception handling.<br>
	 * Subclasses implement their parsing in {@link #readImpl()}.
	 * @return {@code true} if the packet was read successfully, {@code false} otherwise.
	 */
	@Override
	protected boolean read()
	{
		try
		{
			return readImpl();
		}
		catch (Exception e)
		{
			if (LOGGER.isLoggable(Level.SEVERE))
			{
				final LoginClient client = getClient();
				final String clientDescription = client != null ? client.toString() : "unknown client";
				LOGGER.log(Level.SEVERE, "Error while reading login packet " + getClass().getSimpleName() + " from " + clientDescription + ".", e);
			}
			
			return false;
		}
	}
	
	/**
	 * Reads and parses the body of a specific login client packet type.
	 * @return {@code true} if the packet body was read successfully, {@code false} otherwise.
	 */
	protected abstract boolean readImpl();
}
