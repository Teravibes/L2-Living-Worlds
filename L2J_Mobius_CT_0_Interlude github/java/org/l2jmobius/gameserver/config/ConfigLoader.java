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

import org.l2jmobius.gameserver.config.custom.AllowedPlayerRacesConfig;
import org.l2jmobius.gameserver.config.custom.AutoPlayConfig;
import org.l2jmobius.gameserver.config.custom.AutoPotionsConfig;
import org.l2jmobius.gameserver.config.custom.BankingConfig;
import org.l2jmobius.gameserver.config.custom.BossAnnouncementsConfig;
import org.l2jmobius.gameserver.config.custom.CancelReturnConfig;
import org.l2jmobius.gameserver.config.custom.CaptchaConfig;
import org.l2jmobius.gameserver.config.custom.ChampionMonstersConfig;
import org.l2jmobius.gameserver.config.custom.ChatModerationConfig;
import org.l2jmobius.gameserver.config.custom.ClassBalanceConfig;
import org.l2jmobius.gameserver.config.custom.CommunityBoardConfig;
import org.l2jmobius.gameserver.config.custom.CustomMailManagerConfig;
import org.l2jmobius.gameserver.config.custom.DelevelManagerConfig;
import org.l2jmobius.gameserver.config.custom.DualboxCheckConfig;
import org.l2jmobius.gameserver.config.custom.FactionSystemConfig;
import org.l2jmobius.gameserver.config.custom.FakePlayersConfig;
import org.l2jmobius.gameserver.config.custom.FindPvpConfig;
import org.l2jmobius.gameserver.config.custom.FreeMountsConfig;
import org.l2jmobius.gameserver.config.custom.MerchantZeroSellPriceConfig;
import org.l2jmobius.gameserver.config.custom.MultilingualSupportConfig;
import org.l2jmobius.gameserver.config.custom.NoblessMasterConfig;
import org.l2jmobius.gameserver.config.custom.NpcStatMultipliersConfig;
import org.l2jmobius.gameserver.config.custom.OfflinePlayConfig;
import org.l2jmobius.gameserver.config.custom.OfflineTradeConfig;
import org.l2jmobius.gameserver.config.custom.OnlineInfoConfig;
import org.l2jmobius.gameserver.config.custom.PasswordChangeConfig;
import org.l2jmobius.gameserver.config.custom.PremiumSystemConfig;
import org.l2jmobius.gameserver.config.custom.PrivateStoreRangeConfig;
import org.l2jmobius.gameserver.config.custom.PvpAnnounceConfig;
import org.l2jmobius.gameserver.config.custom.PvpRewardItemConfig;
import org.l2jmobius.gameserver.config.custom.PvpTitleColorConfig;
import org.l2jmobius.gameserver.config.custom.RandomSpawnsConfig;
import org.l2jmobius.gameserver.config.custom.SchemeBufferConfig;
import org.l2jmobius.gameserver.config.custom.ScreenWelcomeMessageConfig;
import org.l2jmobius.gameserver.config.custom.SellBuffsConfig;
import org.l2jmobius.gameserver.config.custom.ServerTimeConfig;
import org.l2jmobius.gameserver.config.custom.StartingLocationConfig;
import org.l2jmobius.gameserver.config.custom.StartingTitleConfig;
import org.l2jmobius.gameserver.config.custom.TransmogConfig;
import org.l2jmobius.gameserver.config.custom.WalkerBotProtectionConfig;
import org.l2jmobius.gameserver.config.custom.WarehouseSortingConfig;
import org.l2jmobius.gameserver.config.custom.WeddingConfig;

/**
 * Central configuration loader for initializing all server configuration components.<br>
 * This class serves as the entry point for loading all server configuration settings from various configuration files.<br>
 * The configurations are typically located in the config directory within the server root folder.
 * @author Mobius
 */
public class ConfigLoader
{
	public static void init()
	{
		ServerConfig.load();
		
		// Main configurations.
		ConquerableHallSiegeConfig.load();
		DevelopmentConfig.load();
		FeatureConfig.load();
		FloodProtectorConfig.load();
		GeneralConfig.load();
		GeoEngineConfig.load();
		GrandBossConfig.load();
		IdManagerConfig.load();
		NpcConfig.load();
		OlympiadConfig.load();
		PlayerConfig.load();
		PvpConfig.load();
		RatesConfig.load();
		
		// Custom configurations.
		AllowedPlayerRacesConfig.load();
		AutoPlayConfig.load();
		AutoPotionsConfig.load();
		BankingConfig.load();
		BossAnnouncementsConfig.load();
		CancelReturnConfig.load();
		CaptchaConfig.load();
		ChampionMonstersConfig.load();
		ChatModerationConfig.load();
		ClassBalanceConfig.load();
		CommunityBoardConfig.load();
		CustomMailManagerConfig.load();
		DelevelManagerConfig.load();
		DualboxCheckConfig.load();
		FactionSystemConfig.load();
		FakePlayersConfig.load();
		FindPvpConfig.load();
		FreeMountsConfig.load();
		MerchantZeroSellPriceConfig.load();
		MultilingualSupportConfig.load();
		NoblessMasterConfig.load();
		NpcStatMultipliersConfig.load();
		OfflinePlayConfig.load();
		OfflineTradeConfig.load();
		OnlineInfoConfig.load();
		PasswordChangeConfig.load();
		PremiumSystemConfig.load();
		PrivateStoreRangeConfig.load();
		PvpAnnounceConfig.load();
		PvpRewardItemConfig.load();
		PvpTitleColorConfig.load();
		RandomSpawnsConfig.load();
		SchemeBufferConfig.load();
		ScreenWelcomeMessageConfig.load();
		SellBuffsConfig.load();
		ServerTimeConfig.load();
		StartingLocationConfig.load();
		StartingTitleConfig.load();
		TransmogConfig.load();
		WalkerBotProtectionConfig.load();
		WarehouseSortingConfig.load();
		WeddingConfig.load();
	}
}
