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
package org.l2jmobius.gameserver.config.custom;

import org.l2jmobius.commons.util.ConfigReader;

/**
 * This class loads all the custom fake player related configurations.
 * @author Mobius
 */
public class FakePlayersConfig
{
	// File
	private static final String FAKE_PLAYERS_CONFIG_FILE = "./config/Custom/FakePlayers.ini";
	
	// Constants
	public static boolean FAKE_PLAYERS_ENABLED;
	public static boolean FAKE_PLAYER_CHAT;
	public static boolean FAKE_PLAYER_BEHAVIOR;
	public static int FAKE_PLAYER_DEPLOY_COUNT;
	public static int FAKE_PLAYER_BASE_NPC_ID;
	public static boolean FAKE_PLAYER_USE_SHOTS;
	public static boolean FAKE_PLAYER_KILL_PVP;
	public static boolean FAKE_PLAYER_KILL_KARMA;
	public static boolean FAKE_PLAYER_AUTO_ATTACKABLE;
	public static boolean FAKE_PLAYER_AGGRO_MONSTERS;
	public static boolean FAKE_PLAYER_AGGRO_PLAYERS;
	public static boolean FAKE_PLAYER_AGGRO_FPC;
	public static boolean FAKE_PLAYER_CAN_DROP_ITEMS;
	public static boolean FAKE_PLAYER_CAN_PICKUP;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(FAKE_PLAYERS_CONFIG_FILE);
		FAKE_PLAYERS_ENABLED = config.getBoolean("EnableFakePlayers", false);
		FAKE_PLAYER_CHAT = config.getBoolean("FakePlayerChat", false);
		FAKE_PLAYER_BEHAVIOR = config.getBoolean("FakePlayerBehavior", false);
		FAKE_PLAYER_DEPLOY_COUNT = config.getInt("FakePlayerDeployCount", 0);
		FAKE_PLAYER_BASE_NPC_ID = config.getInt("FakePlayerBaseNpcId", 0);
		FAKE_PLAYER_USE_SHOTS = config.getBoolean("FakePlayerUseShots", false);
		FAKE_PLAYER_KILL_PVP = config.getBoolean("FakePlayerKillsRewardPvP", false);
		FAKE_PLAYER_KILL_KARMA = config.getBoolean("FakePlayerUnflaggedKillsKarma", false);
		FAKE_PLAYER_AUTO_ATTACKABLE = config.getBoolean("FakePlayerAutoAttackable", false);
		FAKE_PLAYER_AGGRO_MONSTERS = config.getBoolean("FakePlayerAggroMonsters", false);
		FAKE_PLAYER_AGGRO_PLAYERS = config.getBoolean("FakePlayerAggroPlayers", false);
		FAKE_PLAYER_AGGRO_FPC = config.getBoolean("FakePlayerAggroFPC", false);
		FAKE_PLAYER_CAN_DROP_ITEMS = config.getBoolean("FakePlayerCanDropItems", false);
		FAKE_PLAYER_CAN_PICKUP = config.getBoolean("FakePlayerCanPickup", false);
	}
}
