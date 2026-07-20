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
package org.l2jmobius.gameserver.config;

import org.l2jmobius.commons.util.ConfigReader;
import org.l2jmobius.gameserver.util.FloodProtectorSettings;

/**
 * This class loads all the flood protector related configurations.
 * @author Mobius
 */
public class FloodProtectorConfig
{
	// File
	private static final String FLOOD_PROTECTOR_CONFIG_FILE = "./config/FloodProtector.ini";
	
	// Constants
	public static FloodProtectorSettings FLOOD_PROTECTOR_USE_ITEM;
	public static FloodProtectorSettings FLOOD_PROTECTOR_ROLL_DICE;
	public static FloodProtectorSettings FLOOD_PROTECTOR_ITEM_PET_SUMMON;
	public static FloodProtectorSettings FLOOD_PROTECTOR_HERO_VOICE;
	public static FloodProtectorSettings FLOOD_PROTECTOR_GLOBAL_CHAT;
	public static FloodProtectorSettings FLOOD_PROTECTOR_SUBCLASS;
	public static FloodProtectorSettings FLOOD_PROTECTOR_DROP_ITEM;
	public static FloodProtectorSettings FLOOD_PROTECTOR_ENCHANT_ITEM;
	public static FloodProtectorSettings FLOOD_PROTECTOR_SERVER_BYPASS;
	public static FloodProtectorSettings FLOOD_PROTECTOR_MULTISELL;
	public static FloodProtectorSettings FLOOD_PROTECTOR_TRANSACTION;
	public static FloodProtectorSettings FLOOD_PROTECTOR_MANUFACTURE;
	public static FloodProtectorSettings FLOOD_PROTECTOR_SENDMAIL;
	public static FloodProtectorSettings FLOOD_PROTECTOR_CHARACTER_SELECT;
	public static FloodProtectorSettings FLOOD_PROTECTOR_ITEM_AUCTION;
	public static FloodProtectorSettings FLOOD_PROTECTOR_PLAYER_ACTION;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(FLOOD_PROTECTOR_CONFIG_FILE);
		FLOOD_PROTECTOR_USE_ITEM = new FloodProtectorSettings("UseItemFloodProtector");
		FLOOD_PROTECTOR_ROLL_DICE = new FloodProtectorSettings("RollDiceFloodProtector");
		FLOOD_PROTECTOR_ITEM_PET_SUMMON = new FloodProtectorSettings("ItemPetSummonFloodProtector");
		FLOOD_PROTECTOR_HERO_VOICE = new FloodProtectorSettings("HeroVoiceFloodProtector");
		FLOOD_PROTECTOR_GLOBAL_CHAT = new FloodProtectorSettings("GlobalChatFloodProtector");
		FLOOD_PROTECTOR_SUBCLASS = new FloodProtectorSettings("SubclassFloodProtector");
		FLOOD_PROTECTOR_DROP_ITEM = new FloodProtectorSettings("DropItemFloodProtector");
		FLOOD_PROTECTOR_ENCHANT_ITEM = new FloodProtectorSettings("EnchantItemFloodProtector");
		FLOOD_PROTECTOR_SERVER_BYPASS = new FloodProtectorSettings("ServerBypassFloodProtector");
		FLOOD_PROTECTOR_MULTISELL = new FloodProtectorSettings("MultiSellFloodProtector");
		FLOOD_PROTECTOR_TRANSACTION = new FloodProtectorSettings("TransactionFloodProtector");
		FLOOD_PROTECTOR_MANUFACTURE = new FloodProtectorSettings("ManufactureFloodProtector");
		FLOOD_PROTECTOR_SENDMAIL = new FloodProtectorSettings("SendMailFloodProtector");
		FLOOD_PROTECTOR_CHARACTER_SELECT = new FloodProtectorSettings("CharacterSelectFloodProtector");
		FLOOD_PROTECTOR_ITEM_AUCTION = new FloodProtectorSettings("ItemAuctionFloodProtector");
		FLOOD_PROTECTOR_PLAYER_ACTION = new FloodProtectorSettings("PlayerActionFloodProtector");
		loadFloodProtectorConfigs(config);
	}
	
	/**
	 * Loads flood protector configurations.
	 * @param configs the ConfigReader parser containing the actual values of the flood protector
	 */
	private static void loadFloodProtectorConfigs(ConfigReader configs)
	{
		loadFloodProtectorConfig(configs, FLOOD_PROTECTOR_USE_ITEM, "UseItem", 4);
		loadFloodProtectorConfig(configs, FLOOD_PROTECTOR_ROLL_DICE, "RollDice", 42);
		loadFloodProtectorConfig(configs, FLOOD_PROTECTOR_ITEM_PET_SUMMON, "ItemPetSummon", 16);
		loadFloodProtectorConfig(configs, FLOOD_PROTECTOR_HERO_VOICE, "HeroVoice", 100);
		loadFloodProtectorConfig(configs, FLOOD_PROTECTOR_GLOBAL_CHAT, "GlobalChat", 5);
		loadFloodProtectorConfig(configs, FLOOD_PROTECTOR_SUBCLASS, "Subclass", 20);
		loadFloodProtectorConfig(configs, FLOOD_PROTECTOR_DROP_ITEM, "DropItem", 10);
		loadFloodProtectorConfig(configs, FLOOD_PROTECTOR_SERVER_BYPASS, "ServerBypass", 5);
		loadFloodProtectorConfig(configs, FLOOD_PROTECTOR_MULTISELL, "MultiSell", 1);
		loadFloodProtectorConfig(configs, FLOOD_PROTECTOR_TRANSACTION, "Transaction", 10);
		loadFloodProtectorConfig(configs, FLOOD_PROTECTOR_MANUFACTURE, "Manufacture", 3);
		loadFloodProtectorConfig(configs, FLOOD_PROTECTOR_SENDMAIL, "SendMail", 100);
		loadFloodProtectorConfig(configs, FLOOD_PROTECTOR_CHARACTER_SELECT, "CharacterSelect", 30);
		loadFloodProtectorConfig(configs, FLOOD_PROTECTOR_ITEM_AUCTION, "ItemAuction", 9);
		loadFloodProtectorConfig(configs, FLOOD_PROTECTOR_PLAYER_ACTION, "PlayerAction", 3);
	}
	
	/**
	 * Loads single flood protector configuration.
	 * @param configReader the ConfigReader parser
	 * @param floodConfig flood protector configuration instance
	 * @param configString flood protector configuration string that determines for which flood protector configuration should be read
	 * @param defaultInterval default flood protector interval
	 */
	private static void loadFloodProtectorConfig(ConfigReader configReader, FloodProtectorSettings floodConfig, String configString, int defaultInterval)
	{
		floodConfig.setProtectionInterval(configReader.getInt("FloodProtector" + configString + "Interval", defaultInterval));
		floodConfig.setLogFlooding(configReader.getBoolean("FloodProtector" + configString + "LogFlooding", false));
		floodConfig.setPunishmentLimit(configReader.getInt("FloodProtector" + configString + "PunishmentLimit", 0));
		floodConfig.setPunishmentType(configReader.getString("FloodProtector" + configString + "PunishmentType", "none"));
		floodConfig.setPunishmentTime(configReader.getInt("FloodProtector" + configString + "PunishmentTime", 0) * 60000);
	}
}
