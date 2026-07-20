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
package org.l2jmobius.loginserver;

import java.io.IOException;
import java.net.Socket;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.loginserver.config.LoginConfig;

/**
 * Listens for incoming game server connections on the configured login port.<br>
 * Delegates each accepted connection to a dedicated handler thread and tracks active handlers.
 * <ul>
 * <li>Uses flood protection to mitigate abusive connection attempts.</li>
 * <li>Binds to host and port configured in {@link LoginConfig}.</li>
 * <li>Creates one {@link GameServerThread} per accepted socket.</li>
 * <li>Tracks active game server threads in a concurrent collection for monitoring.</li>
 * </ul>
 * @author BazookaRpm
 */
public class GameServerListener extends FloodProtectorListener
{
	private static final Logger LOGGER = Logger.getLogger(GameServerListener.class.getName());
	
	// Active game server handler threads.
	private static final Collection<GameServerThread> _gameServerThreads = ConcurrentHashMap.newKeySet();
	
	/**
	 * Constructs a new game server listener bound to the configured host and port.<br>
	 * Uses flood protection to limit abusive connection attempts.
	 * @throws IOException If binding the server socket fails.
	 */
	public GameServerListener() throws IOException
	{
		super(LoginConfig.GAME_SERVER_LOGIN_HOST, LoginConfig.GAME_SERVER_LOGIN_PORT);
		setName(getClass().getSimpleName() + "-" + LoginConfig.GAME_SERVER_LOGIN_HOST + ":" + LoginConfig.GAME_SERVER_LOGIN_PORT);
		
		// if (LOGGER.isLoggable(Level.INFO))
		// {
		// LOGGER.info(getClass().getSimpleName() + ": Listening for game servers on " + LoginConfig.GAME_SERVER_LOGIN_HOST + ":" + LoginConfig.GAME_SERVER_LOGIN_PORT + ".");
		// }
	}
	
	/**
	 * Registers a newly accepted game server socket and starts its handler thread.<br>
	 * Applies basic validation and error handling to avoid leaving broken connections.
	 * @param socket The accepted game server socket.
	 */
	@Override
	public void addClient(Socket socket)
	{
		if ((socket == null) || socket.isClosed())
		{
			LOGGER.warning(getClass().getSimpleName() + ": Attempted to add game server with null or closed socket.");
			return;
		}
		
		try
		{
			final GameServerThread gameServerThread = new GameServerThread(socket); // The constructor itself already does start().
			_gameServerThreads.add(gameServerThread);
			
			if (LOGGER.isLoggable(Level.FINE))
			{
				final String address = (socket.getInetAddress() != null) ? socket.getInetAddress().getHostAddress() : "unknown";
				LOGGER.fine(getClass().getSimpleName() + ": Registered new game server connection from " + address + ". Active game servers: " + _gameServerThreads.size() + ".");
			}
		}
		catch (Exception e)
		{
			// Ensure that we do not keep a broken connection or leak resources if handler creation fails.
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Failed to register game server connection from " + socket.getRemoteSocketAddress() + ".", e);
			
			try
			{
				socket.close();
			}
			catch (IOException ioe)
			{
				LOGGER.log(Level.FINE, getClass().getSimpleName() + ": Failed to close socket after registration failure.", ioe);
			}
		}
	}
	
	/**
	 * Removes a game server thread from the active collection.<br>
	 * Intended to be called when a game server handler terminates.
	 * @param gameServerThread The game server thread to remove.
	 */
	public void removeGameServer(GameServerThread gameServerThread)
	{
		if (gameServerThread == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Attempted to remove null game server thread.");
			return;
		}
		
		final boolean removed = _gameServerThreads.remove(gameServerThread);
		
		if (LOGGER.isLoggable(Level.FINE))
		{
			LOGGER.fine(getClass().getSimpleName() + ": Removed game server thread " + gameServerThread + " (removed=" + removed + "). Active game servers: " + _gameServerThreads.size() + ".");
		}
	}
	
	/**
	 * Returns the number of active game server connections.
	 * @return The current game server count.
	 */
	public static int getGameServerCount()
	{
		return _gameServerThreads.size();
	}
	
	/**
	 * Returns the active game server threads collection for monitoring purposes.<br>
	 * The returned collection is concurrent and weakly consistent.
	 * @return The active game server threads collection.
	 */
	public static Collection<GameServerThread> getGameServers()
	{
		return _gameServerThreads;
	}
}
