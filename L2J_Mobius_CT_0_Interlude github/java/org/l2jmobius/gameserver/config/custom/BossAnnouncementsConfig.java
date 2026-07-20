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

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.commons.util.ConfigReader;

/**
 * This class loads all the custom boss announcement related configurations.
 * @author Mobius
 */
public class BossAnnouncementsConfig
{
	// File
	private static final String BOSS_ANNOUNCEMENTS_CONFIG_FILE = "./config/Custom/BossAnnouncements.ini";
	
	// Constants
	public static boolean RAIDBOSS_SPAWN_ANNOUNCEMENTS;
	public static boolean RAIDBOSS_DEFEAT_ANNOUNCEMENTS;
	public static boolean RAIDBOSS_INSTANCE_ANNOUNCEMENTS;
	public static boolean GRANDBOSS_SPAWN_ANNOUNCEMENTS;
	public static boolean GRANDBOSS_DEFEAT_ANNOUNCEMENTS;
	public static boolean GRANDBOSS_INSTANCE_ANNOUNCEMENTS;
	public static Set<Integer> RAIDBOSSES_EXCLUDED_FROM_SPAWN_ANNOUNCEMENTS = new HashSet<>();
	public static Set<Integer> RAIDBOSSES_EXCLUDED_FROM_DEFEAT_ANNOUNCEMENTS = new HashSet<>();
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(BOSS_ANNOUNCEMENTS_CONFIG_FILE);
		RAIDBOSS_SPAWN_ANNOUNCEMENTS = config.getBoolean("RaidBossSpawnAnnouncements", false);
		RAIDBOSS_DEFEAT_ANNOUNCEMENTS = config.getBoolean("RaidBossDefeatAnnouncements", false);
		RAIDBOSS_INSTANCE_ANNOUNCEMENTS = config.getBoolean("RaidBossInstanceAnnouncements", false);
		GRANDBOSS_SPAWN_ANNOUNCEMENTS = config.getBoolean("GrandBossSpawnAnnouncements", false);
		GRANDBOSS_DEFEAT_ANNOUNCEMENTS = config.getBoolean("GrandBossDefeatAnnouncements", false);
		GRANDBOSS_INSTANCE_ANNOUNCEMENTS = config.getBoolean("GrandBossInstanceAnnouncements", false);
		
		RAIDBOSSES_EXCLUDED_FROM_SPAWN_ANNOUNCEMENTS.clear();
		for (String raidbossId : config.getString("RaidbossExcludedFromSpawnAnnouncements", "").split(","))
		{
			if (!raidbossId.isEmpty())
			{
				RAIDBOSSES_EXCLUDED_FROM_SPAWN_ANNOUNCEMENTS.add(Integer.parseInt(raidbossId.trim()));
			}
		}
		
		RAIDBOSSES_EXCLUDED_FROM_DEFEAT_ANNOUNCEMENTS.clear();
		for (String raidbossId : config.getString("RaidbossExcludedFromDefeatAnnouncements", "").split(","))
		{
			if (!raidbossId.isEmpty())
			{
				RAIDBOSSES_EXCLUDED_FROM_DEFEAT_ANNOUNCEMENTS.add(Integer.parseInt(raidbossId.trim()));
			}
		}
	}
}
