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
package org.l2jmobius.gameserver.data.xml;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.config.custom.FakePlayersConfig;

/**
 * @author Mobius
 */
public class FakePlayerData
{
	private static final Logger LOGGER = Logger.getLogger(FakePlayerData.class.getName());
	
	private final Map<String, String> _fakePlayerNames = new ConcurrentHashMap<>();
	private final Map<String, Integer> _fakePlayerIds = new ConcurrentHashMap<>();
	private final Set<String> _talkableFakePlayerNames = ConcurrentHashMap.newKeySet();
	
	protected FakePlayerData()
	{
	}
	
	public void report()
	{
		if (FakePlayersConfig.FAKE_PLAYERS_ENABLED)
		{
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _fakePlayerIds.size() + " templates.");
		}
		else
		{
			LOGGER.info(getClass().getSimpleName() + ": Disabled.");
		}
	}
	
	/**
	 * Adds a mapping between a fake player name and its corresponding NPC ID.
	 * @param name the name of the fake player
	 * @param npcId the NPC ID to associate with the fake player
	 */
	public void addFakePlayerId(String name, int npcId)
	{
		_fakePlayerIds.put(name, npcId);
	}
	
	/**
	 * Retrieves the NPC ID associated with the given fake player name.
	 * @param name the name of the fake player
	 * @return the NPC ID corresponding to the given name, or {@code null} if no match is found
	 */
	public int getNpcIdByName(String name)
	{
		return _fakePlayerIds.get(name);
	}

	/**
	 * Retrieves all known fake player NPC template ids.
	 * @return a collection of every registered fake player npc id
	 */
	public java.util.Collection<Integer> getFakePlayerIds()
	{
		return _fakePlayerIds.values();
	}
	
	/**
	 * Adds a mapping between a lowercase version of a fake player name and its properly formatted name.
	 * @param lowercaseName the lowercase version of the fake player name
	 * @param name the properly formatted name of the fake player
	 */
	public void addFakePlayerName(String lowercaseName, String name)
	{
		_fakePlayerNames.put(lowercaseName, name);
	}
	
	/**
	 * Retrieves the properly formatted name of a fake player, given a case-insensitive name lookup.
	 * @param name the name of the fake player (case-insensitive)
	 * @return the correctly formatted name of the fake player, or {@code null} if no match is found
	 */
	public String getProperName(String name)
	{
		return _fakePlayerNames.get(name.toLowerCase());
	}
	
	/**
	 * Adds a fake player name to the set of talkable fake player names.
	 * @param lowercaseName the lowercase version of the fake player name
	 */
	public void addTalkableFakePlayerName(String lowercaseName)
	{
		_talkableFakePlayerNames.add(lowercaseName);
	}
	
	/**
	 * Checks if a fake player with the given name is marked as talkable.
	 * @param name the name of the fake player (case-insensitive)
	 * @return {@code true} if the fake player with the given name is talkable, {@code false} otherwise
	 */
	public boolean isTalkable(String name)
	{
		return _talkableFakePlayerNames.contains(name.toLowerCase());
	}
	
	public static FakePlayerData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final FakePlayerData INSTANCE = new FakePlayerData();
	}
}
