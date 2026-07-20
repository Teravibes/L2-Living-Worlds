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

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.util.ConfigReader;
import org.l2jmobius.gameserver.model.skill.AbnormalVisualEffect;

/**
 * This class loads all the custom offline play related configurations.
 * @author Mobius
 */
public class OfflinePlayConfig
{
	// File
	private static final String OFFLINE_PLAY_CONFIG_FILE = "./config/Custom/OfflinePlay.ini";
	
	// Constants
	public static boolean ENABLE_OFFLINE_PLAY_COMMAND;
	public static boolean RESTORE_AUTO_PLAY_OFFLINERS;
	public static boolean OFFLINE_PLAY_PREMIUM;
	public static boolean OFFLINE_PLAY_LOGOUT_ON_DEATH;
	public static boolean OFFLINE_PLAY_DISCONNECT_SAME_ACCOUNT;
	public static String OFFLINE_PLAY_LOGIN_MESSAGE;
	public static boolean OFFLINE_PLAY_SET_NAME_COLOR;
	public static int OFFLINE_PLAY_NAME_COLOR;
	public static List<AbnormalVisualEffect> OFFLINE_PLAY_ABNORMAL_EFFECTS = new ArrayList<>();
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(OFFLINE_PLAY_CONFIG_FILE);
		ENABLE_OFFLINE_PLAY_COMMAND = config.getBoolean("EnableOfflinePlayCommand", false);
		RESTORE_AUTO_PLAY_OFFLINERS = config.getBoolean("RestoreAutoPlayOffliners", true);
		OFFLINE_PLAY_PREMIUM = config.getBoolean("OfflinePlayPremium", false);
		OFFLINE_PLAY_LOGOUT_ON_DEATH = config.getBoolean("OfflinePlayLogoutOnDeath", true);
		OFFLINE_PLAY_DISCONNECT_SAME_ACCOUNT = config.getBoolean("OfflinePlayDisconnectSameAccount", false);
		OFFLINE_PLAY_LOGIN_MESSAGE = config.getString("OfflinePlayLoginMessage", "");
		OFFLINE_PLAY_SET_NAME_COLOR = config.getBoolean("OfflinePlaySetNameColor", false);
		OFFLINE_PLAY_NAME_COLOR = Integer.decode("0x" + config.getString("OfflinePlayNameColor", "808080"));
		OFFLINE_PLAY_ABNORMAL_EFFECTS.clear();
		final String offlinePlayAbnormalEffects = config.getString("OfflinePlayAbnormalEffect", "").trim();
		if (!offlinePlayAbnormalEffects.isEmpty())
		{
			for (String ave : offlinePlayAbnormalEffects.split(","))
			{
				OFFLINE_PLAY_ABNORMAL_EFFECTS.add(Enum.valueOf(AbnormalVisualEffect.class, ave.trim()));
			}
		}
	}
}
