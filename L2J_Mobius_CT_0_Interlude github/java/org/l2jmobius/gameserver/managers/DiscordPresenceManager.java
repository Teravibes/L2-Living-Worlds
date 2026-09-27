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
package org.l2jmobius.gameserver.managers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.xml.ClassListData;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.player.ClassInfoHolder;

/**
 * Publishes the real player's character for the launcher's Discord Rich Presence.
 * <p>
 * Every few seconds this writes {@code presence.json} in the game server's working folder (the pack's {@code game\}
 * folder) with the name, level and class of the first real player online, or {@code "online": false} when nobody is.
 * Phantoms, offline traders and clientless players are skipped. The launcher reads the file and talks to the local
 * Discord client; the server never contacts Discord itself. {@code updated} lets the launcher ignore a file left
 * behind by a server that stopped.
 */
public class DiscordPresenceManager
{
	private static final Logger LOGGER = Logger.getLogger(DiscordPresenceManager.class.getName());
	
	private static final Path FILE = Path.of("presence.json");
	private static final Path TEMP = Path.of("presence.json.tmp");
	private static final long INTERVAL_MS = 5000;
	
	private boolean _warned;
	
	protected DiscordPresenceManager()
	{
		ThreadPool.scheduleAtFixedRate(this::write, INTERVAL_MS, INTERVAL_MS);
	}
	
	private void write()
	{
		final Player player = findRealPlayer();
		final String body;
		if (player == null)
		{
			body = "\"online\":false";
		}
		else
		{
			final ClassInfoHolder info = ClassListData.getInstance().getClass(player.getPlayerClass());
			final String className = (info != null) && (info.getClassName() != null) ? info.getClassName() : "";
			final long since = System.currentTimeMillis() - player.getOnlineTimeMillis();
			body = "\"online\":true,\"name\":\"" + escape(player.getName()) + "\",\"level\":" + player.getLevel() + ",\"className\":\"" + escape(className) + "\",\"since\":" + since;
		}
		
		// Rewrite on every tick so "updated" stays fresh; the launcher treats an old file as a stopped server.
		final String json = "{" + body + ",\"updated\":" + System.currentTimeMillis() + "}";
		try
		{
			Files.writeString(TEMP, json, StandardCharsets.UTF_8);
			Files.move(TEMP, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			_warned = false;
		}
		catch (IOException e)
		{
			if (!_warned)
			{
				LOGGER.log(Level.WARNING, "DiscordPresenceManager: could not write " + FILE.toAbsolutePath() + ": " + e.getMessage());
				_warned = true;
			}
		}
	}
	
	private static Player findRealPlayer()
	{
		final PhantomManager phantoms = PhantomManager.getInstance();
		for (Player player : World.getInstance().getPlayers())
		{
			if ((player != null) && player.isOnline() && !player.isInOfflineMode() && (player.getClient() != null) && !phantoms.isPhantom(player))
			{
				return player;
			}
		}
		return null;
	}
	
	private static String escape(String value)
	{
		if (value == null)
		{
			return "";
		}
		final StringBuilder sb = new StringBuilder(value.length());
		for (char c : value.toCharArray())
		{
			if ((c == '"') || (c == '\\'))
			{
				sb.append('\\').append(c);
			}
			else if (c >= ' ')
			{
				sb.append(c);
			}
		}
		return sb.toString();
	}
	
	public static DiscordPresenceManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final DiscordPresenceManager INSTANCE = new DiscordPresenceManager();
	}
}
