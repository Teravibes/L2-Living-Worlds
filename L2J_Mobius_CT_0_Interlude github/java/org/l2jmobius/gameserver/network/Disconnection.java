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
package org.l2jmobius.gameserver.network;

import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.managers.AntiFeedManager;
import org.l2jmobius.gameserver.managers.PhantomManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.taskmanagers.AttackStanceTaskManager;

/**
 * Handles the disconnection process for a player or client.
 * @author NB4L1, Mobius
 */
public class Disconnection
{
	private static final Logger LOGGER = Logger.getLogger(Disconnection.class.getName());
	private static final Logger LOGGER_ACCOUNTING = Logger.getLogger("accounting");
	
	private final GameClient _client;
	private final Player _player;
	
	/**
	 * Constructs a new Disconnection instance for the given client and player.<br>
	 * Resolves the client and player references and performs initial cleanup tasks.
	 * @param client the GameClient to disconnect (can be null if player is provided).
	 * @param player the Player to disconnect (can be null if client is provided).
	 */
	private Disconnection(GameClient client, Player player)
	{
		// Resolve client and player.
		_client = client != null ? client : player != null ? player.getClient() : null;
		_player = player != null ? player : client != null ? client.getPlayer() : null;
		
		// Stop player tasks.
		if (_player != null)
		{
			_player.stopAllTasks();
		}
		
		// Anti Feed.
		AntiFeedManager.getInstance().onDisconnect(_client);
		
		if (_client != null)
		{
			_client.setPlayer(null);
		}
		
		if (_player != null)
		{
			_player.setClient(null);
		}
	}
	
	/**
	 * Creates a Disconnection instance for the given GameClient.
	 * @param client the GameClient to disconnect.
	 * @return a new Disconnection instance.
	 */
	public static Disconnection of(GameClient client)
	{
		return new Disconnection(client, null);
	}
	
	/**
	 * Creates a Disconnection instance for the given Player.
	 * @param player the Player to disconnect.
	 * @return a new Disconnection instance.
	 */
	public static Disconnection of(Player player)
	{
		return new Disconnection(null, player);
	}
	
	/**
	 * Creates a Disconnection instance for the given GameClient and Player.
	 * @param client the GameClient to disconnect.
	 * @param player the Player to disconnect.
	 * @return a new Disconnection instance.
	 */
	public static Disconnection of(GameClient client, Player player)
	{
		return new Disconnection(client, player);
	}
	
	/**
	 * Stores the player's data and deletes it if the player is online.<br>
	 * If an error occurs during the process, a warning is logged.
	 */
	public void storeAndDelete()
	{
		try
		{
			if (_player != null)
			{
				_player.storeMe();
				
				if (_player.isOnline())
				{
					// Living World: despawn any "regular" phantoms that were login-spawned to accompany this player.
					PhantomManager.getInstance().onOwnerLogout(_player);

					_player.deleteMe();

					LOGGER_ACCOUNTING.info("Logged out, " + _player);
				}
			}
			else if (_client != null)
			{
				LOGGER_ACCOUNTING.info("Logged out, " + _client);
			}
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Problem with storeAndDelete: " + e.getMessage());
		}
	}
	
	/**
	 * Stores and deletes the player's data, then closes the connection with the given packet.
	 * @param packet the ServerPacket to send before closing the connection (can be null).
	 */
	public void storeAndDeleteWith(ServerPacket packet)
	{
		storeAndDelete();
		
		if (_client != null)
		{
			_client.close(packet);
		}
	}
	
	/**
	 * Handles the disconnection process based on the player's state.<br>
	 * If the player can logout immediately, the player's data is stored and deleted.<br>
	 * Otherwise, the process is scheduled after a delay.
	 */
	public void onDisconnection()
	{
		if (_player == null)
		{
			return;
		}
		
		if (_player.canLogout())
		{
			storeAndDelete();
		}
		else
		{
			ThreadPool.schedule(() ->
			{
				if (_player.isOnline())
				{
					storeAndDelete();
				}
			}, AttackStanceTaskManager.COMBAT_TIME);
		}
	}
}
