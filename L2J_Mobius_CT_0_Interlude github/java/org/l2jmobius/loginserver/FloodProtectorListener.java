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
package org.l2jmobius.loginserver;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.loginserver.config.LoginConfig;

/**
 * Listens for incoming login connections and applies basic IP based flood protection.<br>
 * Maintains per address connection state and delegates accepted sockets to subclasses.
 * <ul>
 * <li>Binds a server socket on a configured address and port.</li>
 * <li>Tracks connection count and timestamps per remote IP.</li>
 * <li>Uses {@link LoginConfig} limits to mitigate abusive connection patterns.</li>
 * </ul>
 * @author BazookaRpm
 */
public abstract class FloodProtectorListener extends Thread
{
	private static final Logger LOGGER = Logger.getLogger(FloodProtectorListener.class.getName());
	
	// Server socket backlog size.
	private static final int SERVER_SOCKET_BACKLOG = 50;
	
	// Flood protection state per remote IP.
	private final Map<String, ForeignConnection> _floodProtection = new ConcurrentHashMap<>();
	
	// Login server listening socket.
	private final ServerSocket _serverSocket;
	
	/**
	 * Creates a new flood protector listener bound to the given IP and port.
	 * @param listenIp The IP address to bind, or "*" to listen on all interfaces.
	 * @param port The TCP port to listen on.
	 * @throws IOException If the socket cannot be created or bound.
	 */
	public FloodProtectorListener(String listenIp, int port) throws IOException
	{
		// If listening IP is "*", listen on all interfaces.
		if ("*".equals(listenIp))
		{
			_serverSocket = new ServerSocket(port);
		}
		else
		{
			// Listen on the specified IP with a fixed backlog size.
			_serverSocket = new ServerSocket(port, SERVER_SOCKET_BACKLOG, InetAddress.getByName(listenIp));
		}
	}
	
	/**
	 * Accepts incoming connections and applies flood protection rules.
	 */
	@Override
	public void run()
	{
		Socket clientSocket = null;
		while (!isInterrupted())
		{
			try
			{
				// Accept incoming connections.
				clientSocket = _serverSocket.accept();
				
				if (LoginConfig.FLOOD_PROTECTION)
				{
					// Check flood protection for this remote address.
					final String clientAddress = clientSocket.getInetAddress().getHostAddress();
					ForeignConnection foreignConnection = _floodProtection.get(clientAddress);
					if (foreignConnection != null)
					{
						foreignConnection.connectionNumber += 1;
						
						if (((foreignConnection.connectionNumber > LoginConfig.FAST_CONNECTION_LIMIT) && ((System.currentTimeMillis() - foreignConnection.lastConnection) < LoginConfig.NORMAL_CONNECTION_TIME)) || ((System.currentTimeMillis() - foreignConnection.lastConnection) < LoginConfig.FAST_CONNECTION_TIME) || (foreignConnection.connectionNumber > LoginConfig.MAX_CONNECTION_PER_IP))
						{
							foreignConnection.lastConnection = System.currentTimeMillis();
							clientSocket.close();
							foreignConnection.connectionNumber -= 1;
							
							if (!foreignConnection.isFlooding)
							{
								LOGGER.warning("Potential flood from address " + clientAddress + ".");
							}
							
							foreignConnection.isFlooding = true;
							continue;
						}
						
						// If the connection was previously considered flooding but now passes the check.
						if (foreignConnection.isFlooding)
						{
							foreignConnection.isFlooding = false;
							LOGGER.info("Address " + clientAddress + " is no longer considered as flooding.");
						}
						
						foreignConnection.lastConnection = System.currentTimeMillis();
					}
					else
					{
						// Initialize flood protection state for the new remote address.
						foreignConnection = new ForeignConnection(System.currentTimeMillis());
						_floodProtection.put(clientAddress, foreignConnection);
					}
				}
				
				// Add client connection for further processing (implementation in subclasses).
				addClient(clientSocket);
			}
			catch (Exception e)
			{
				// Handle exceptions and potential thread interruption.
				if (isInterrupted())
				{
					// Close server socket and break the loop on thread interruption.
					try
					{
						_serverSocket.close();
					}
					catch (IOException io)
					{
						LOGGER.log(Level.INFO, "Error while closing server socket on interruption in " + getClass().getSimpleName() + ".", io);
					}
					break;
				}
				
				LOGGER.log(Level.WARNING, "Unexpected exception in " + getClass().getSimpleName() + " main loop.", e);
			}
		}
	}
	
	/**
	 * Represents flood protection state for a single remote IP address.
	 */
	protected static class ForeignConnection
	{
		public int connectionNumber;
		public long lastConnection;
		public boolean isFlooding = false;
		
		/**
		 * Creates a new foreign connection record with an initial timestamp.
		 * @param time The timestamp of the first connection.
		 */
		public ForeignConnection(long time)
		{
			// Initialize a new foreign connection.
			lastConnection = time;
			connectionNumber = 1;
		}
	}
	
	/**
	 * Adds a client connection for further processing in subclasses.
	 * @param socket The accepted client socket.
	 */
	public abstract void addClient(Socket socket);
	
	/**
	 * Decrements and optionally removes flood protection entry for the given IP.<br>
	 * Should be called when the session or socket associated with the given address is closed.
	 * @param ip The remote IP address.
	 */
	public void removeFloodProtection(String ip)
	{
		// Only proceed if flood protection is enabled.
		if (!LoginConfig.FLOOD_PROTECTION)
		{
			return;
		}
		
		// Decrement connection count or remove if no more connections exist.
		final ForeignConnection foreignConnection = _floodProtection.get(ip);
		if (foreignConnection != null)
		{
			foreignConnection.connectionNumber -= 1;
			if (foreignConnection.connectionNumber == 0)
			{
				_floodProtection.remove(ip);
			}
		}
		else
		{
			LOGGER.warning("Attempted to remove flood protection for unknown IP " + ip + ".");
		}
	}
	
	/**
	 * Closes the listening socket.
	 */
	public void close()
	{
		try
		{
			_serverSocket.close();
		}
		catch (IOException e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": " + e.getMessage());
		}
	}
}
