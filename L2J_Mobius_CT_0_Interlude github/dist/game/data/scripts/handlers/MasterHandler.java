/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.config.custom.AutoPlayConfig;
import org.l2jmobius.gameserver.config.custom.AutoPotionsConfig;
import org.l2jmobius.gameserver.config.custom.BankingConfig;
import org.l2jmobius.gameserver.config.custom.ChatModerationConfig;
import org.l2jmobius.gameserver.config.custom.MultilingualSupportConfig;
import org.l2jmobius.gameserver.config.custom.OfflinePlayConfig;
import org.l2jmobius.gameserver.config.custom.OfflineTradeConfig;
import org.l2jmobius.gameserver.config.custom.OnlineInfoConfig;
import org.l2jmobius.gameserver.config.custom.PasswordChangeConfig;
import org.l2jmobius.gameserver.config.custom.PremiumSystemConfig;
import org.l2jmobius.gameserver.config.custom.WeddingConfig;
import org.l2jmobius.gameserver.handler.ActionClickHandler;
import org.l2jmobius.gameserver.handler.ActionShiftHandler;
import org.l2jmobius.gameserver.handler.AdminCommandHandler;
import org.l2jmobius.gameserver.handler.BypassHandler;
import org.l2jmobius.gameserver.handler.ChatHandler;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.handler.IHandler;
import org.l2jmobius.gameserver.handler.ItemHandler;
import org.l2jmobius.gameserver.handler.PunishmentHandler;
import org.l2jmobius.gameserver.handler.TargetHandler;
import org.l2jmobius.gameserver.handler.UserCommandHandler;
import org.l2jmobius.gameserver.handler.VoicedCommandHandler;

import handlers.actions.click.ArtefactClick;
import handlers.actions.click.DecoyClick;
import handlers.actions.click.DoorClick;
import handlers.actions.click.ItemClick;
import handlers.actions.click.NpcClick;
import handlers.actions.click.PetClick;
import handlers.actions.click.PlayerClick;
import handlers.actions.click.StaticObjectClick;
import handlers.actions.click.SummonClick;
import handlers.actions.click.TrapClick;
import handlers.actions.shiftclick.DoorShiftClick;
import handlers.actions.shiftclick.ItemShiftClick;
import handlers.actions.shiftclick.NpcShiftClick;
import handlers.actions.shiftclick.PlayerShiftClick;
import handlers.actions.shiftclick.StaticObjectShiftClick;
import handlers.actions.shiftclick.SummonShiftClick;
import handlers.bypass.communityboard.ClanBoard;
import handlers.bypass.communityboard.DropSearchBoard;
import handlers.bypass.communityboard.FavoriteBoard;
import handlers.bypass.communityboard.FriendsBoard;
import handlers.bypass.communityboard.HomeBoard;
import handlers.bypass.communityboard.HomepageBoard;
import handlers.bypass.communityboard.MailBoard;
import handlers.bypass.communityboard.MemoBoard;
import handlers.bypass.communityboard.RegionBoard;
import handlers.bypass.npc.Augment;
import handlers.bypass.npc.Buy;
import handlers.bypass.npc.BuyShadowItem;
import handlers.bypass.npc.ChatLink;
import handlers.bypass.npc.ClanWarehouse;
import handlers.bypass.npc.EnchantSkillList;
import handlers.bypass.npc.Festival;
import handlers.bypass.npc.FindPvP;
import handlers.bypass.npc.Freight;
import handlers.bypass.npc.Link;
import handlers.bypass.npc.Loto;
import handlers.bypass.npc.Multisell;
import handlers.bypass.npc.NpcViewMod;
import handlers.bypass.npc.Observation;
import handlers.bypass.npc.PlayerHelp;
import handlers.bypass.npc.PrivateWarehouse;
import handlers.bypass.npc.QuestList;
import handlers.bypass.npc.Rift;
import handlers.bypass.npc.ScriptLink;
import handlers.bypass.npc.Sell;
import handlers.bypass.npc.SkillList;
import handlers.bypass.npc.SupportBlessing;
import handlers.bypass.npc.SupportMagic;
import handlers.bypass.npc.TerritoryStatus;
import handlers.bypass.npc.TutorialClose;
import handlers.bypass.npc.VoiceCommand;
import handlers.bypass.npc.Wear;
import handlers.chat.channels.ChatAlliance;
import handlers.chat.channels.ChatBattlefield;
import handlers.chat.channels.ChatClan;
import handlers.chat.channels.ChatGeneral;
import handlers.chat.channels.ChatHeroVoice;
import handlers.chat.channels.ChatParty;
import handlers.chat.channels.ChatPartyMatchRoom;
import handlers.chat.channels.ChatPartyRoomAll;
import handlers.chat.channels.ChatPartyRoomCommander;
import handlers.chat.channels.ChatPetition;
import handlers.chat.channels.ChatShout;
import handlers.chat.channels.ChatTrade;
import handlers.chat.channels.ChatWhisper;
import handlers.chat.commands.admin.AdminAdmin;
import handlers.chat.commands.admin.AdminAnnouncements;
import handlers.chat.commands.admin.AdminAugment;
import handlers.chat.commands.admin.AdminBuffs;
import handlers.chat.commands.admin.AdminCHSiege;
import handlers.chat.commands.admin.AdminCamera;
import handlers.chat.commands.admin.AdminChangeAccessLevel;
import handlers.chat.commands.admin.AdminClan;
import handlers.chat.commands.admin.AdminCreateItem;
import handlers.chat.commands.admin.AdminCursedWeapons;
import handlers.chat.commands.admin.AdminDebug;
import handlers.chat.commands.admin.AdminDelete;
import handlers.chat.commands.admin.AdminDestroyItems;
import handlers.chat.commands.admin.AdminDisconnect;
import handlers.chat.commands.admin.AdminDoorControl;
import handlers.chat.commands.admin.AdminEditChar;
import handlers.chat.commands.admin.AdminEffects;
import handlers.chat.commands.admin.AdminEnchant;
import handlers.chat.commands.admin.AdminEvents;
import handlers.chat.commands.admin.AdminExpSp;
import handlers.chat.commands.admin.AdminFakePlayers;
import handlers.chat.commands.admin.AdminFence;
import handlers.chat.commands.admin.AdminFightCalculator;
import handlers.chat.commands.admin.AdminGamePoints;
import handlers.chat.commands.admin.AdminGeodata;
import handlers.chat.commands.admin.AdminGm;
import handlers.chat.commands.admin.AdminGmChat;
import handlers.chat.commands.admin.AdminGmSpeed;
import handlers.chat.commands.admin.AdminGoto;
import handlers.chat.commands.admin.AdminGrandBoss;
import handlers.chat.commands.admin.AdminHeal;
import handlers.chat.commands.admin.AdminHelp;
import handlers.chat.commands.admin.AdminHide;
import handlers.chat.commands.admin.AdminHtml;
import handlers.chat.commands.admin.AdminInstance;
import handlers.chat.commands.admin.AdminInstanceZone;
import handlers.chat.commands.admin.AdminInvul;
import handlers.chat.commands.admin.AdminKick;
import handlers.chat.commands.admin.AdminKill;
import handlers.chat.commands.admin.AdminLevel;
import handlers.chat.commands.admin.AdminLogin;
import handlers.chat.commands.admin.AdminMammon;
import handlers.chat.commands.admin.AdminManor;
import handlers.chat.commands.admin.AdminMenu;
import handlers.chat.commands.admin.AdminMessages;
import handlers.chat.commands.admin.AdminOnline;
import handlers.chat.commands.admin.AdminPForge;
import handlers.chat.commands.admin.AdminPathNode;
import handlers.chat.commands.admin.AdminPcCafePoints;
import handlers.chat.commands.admin.AdminPetition;
import handlers.chat.commands.admin.AdminPledge;
import handlers.chat.commands.admin.AdminPremium;
import handlers.chat.commands.admin.AdminPunishment;
import handlers.chat.commands.admin.AdminQuest;
import handlers.chat.commands.admin.AdminRegions;
import handlers.chat.commands.admin.AdminRates;
import handlers.chat.commands.admin.AdminReload;
import handlers.chat.commands.admin.AdminRepairChar;
import handlers.chat.commands.admin.AdminRes;
import handlers.chat.commands.admin.AdminRide;
import handlers.chat.commands.admin.AdminScan;
import handlers.chat.commands.admin.AdminSearch;
import handlers.chat.commands.admin.AdminServerInfo;
import handlers.chat.commands.admin.AdminSevenSigns;
import handlers.chat.commands.admin.AdminShop;
import handlers.chat.commands.admin.AdminShowQuests;
import handlers.chat.commands.admin.AdminShutdown;
import handlers.chat.commands.admin.AdminSiege;
import handlers.chat.commands.admin.AdminSkill;
import handlers.chat.commands.admin.AdminSpawn;
import handlers.chat.commands.admin.AdminSummon;
import handlers.chat.commands.admin.AdminSuperHaste;
import handlers.chat.commands.admin.AdminTarget;
import handlers.chat.commands.admin.AdminTargetSay;
import handlers.chat.commands.admin.AdminTeleport;
import handlers.chat.commands.admin.AdminTest;
import handlers.chat.commands.admin.AdminVitality;
import handlers.chat.commands.admin.AdminFpcRoute;
import handlers.chat.commands.admin.AdminZone;
import handlers.chat.commands.admin.AdminZoneBuild;
import handlers.chat.commands.user.ChannelDelete;
import handlers.chat.commands.user.ChannelInfo;
import handlers.chat.commands.user.ChannelLeave;
import handlers.chat.commands.user.ClanPenalty;
import handlers.chat.commands.user.ClanWarsList;
import handlers.chat.commands.user.Dismount;
import handlers.chat.commands.user.Loc;
import handlers.chat.commands.user.Mount;
import handlers.chat.commands.user.OlympiadStat;
import handlers.chat.commands.user.PartyInfo;
import handlers.chat.commands.user.SiegeStatus;
import handlers.chat.commands.user.Time;
import handlers.chat.commands.user.Unstuck;
import handlers.chat.commands.voiced.AutoPlay;
import handlers.chat.commands.voiced.AutoPotion;
import handlers.chat.commands.voiced.Banking;
import handlers.chat.commands.voiced.ChangePassword;
import handlers.chat.commands.voiced.ChatAdmin;
import handlers.chat.commands.voiced.ExperienceGain;
import handlers.chat.commands.voiced.Lang;
import handlers.chat.commands.voiced.Offline;
import handlers.chat.commands.voiced.OfflinePlay;
import handlers.chat.commands.voiced.Online;
import handlers.chat.commands.voiced.Premium;
import handlers.chat.commands.voiced.Wedding;
import handlers.items.BeastSoulShot;
import handlers.items.BeastSpice;
import handlers.items.BeastSpiritShot;
import handlers.items.BlessedSpiritShot;
import handlers.items.Book;
import handlers.items.Calculator;
import handlers.items.CharmOfCourage;
import handlers.items.Elixir;
import handlers.items.EnchantScrolls;
import handlers.items.ExtractableItems;
import handlers.items.FishShots;
import handlers.items.Harvester;
import handlers.items.ItemSkills;
import handlers.items.ItemSkillsTemplate;
import handlers.items.Maps;
import handlers.items.MercTicket;
import handlers.items.PetFood;
import handlers.items.Recipes;
import handlers.items.RollingDice;
import handlers.items.Seed;
import handlers.items.SevenSignsRecord;
import handlers.items.SoulShots;
import handlers.items.SpecialXMas;
import handlers.items.SpiritShot;
import handlers.items.SummonItems;
import handlers.punishments.BanHandler;
import handlers.punishments.ChatBanHandler;
import handlers.punishments.JailHandler;
import handlers.skill.targets.Area;
import handlers.skill.targets.AreaCorpseMob;
import handlers.skill.targets.AreaFriendly;
import handlers.skill.targets.AreaSummon;
import handlers.skill.targets.Aura;
import handlers.skill.targets.AuraCorpseMob;
import handlers.skill.targets.AuraFriendly;
import handlers.skill.targets.BehindArea;
import handlers.skill.targets.BehindAura;
import handlers.skill.targets.Clan;
import handlers.skill.targets.ClanMember;
import handlers.skill.targets.CommandChannel;
import handlers.skill.targets.CorpseClan;
import handlers.skill.targets.CorpseMob;
import handlers.skill.targets.EnemySummon;
import handlers.skill.targets.FlagPole;
import handlers.skill.targets.FrontArea;
import handlers.skill.targets.FrontAura;
import handlers.skill.targets.Ground;
import handlers.skill.targets.Holy;
import handlers.skill.targets.One;
import handlers.skill.targets.OwnerPet;
import handlers.skill.targets.Party;
import handlers.skill.targets.PartyClan;
import handlers.skill.targets.PartyMember;
import handlers.skill.targets.PartyNotMe;
import handlers.skill.targets.PartyOther;
import handlers.skill.targets.PcBody;
import handlers.skill.targets.Pet;
import handlers.skill.targets.Self;
import handlers.skill.targets.Servitor;
import handlers.skill.targets.Summon;
import handlers.skill.targets.TargetParty;
import handlers.skill.targets.Unlockable;

/**
 * Master handler.
 * @author UnAfraid
 */
public class MasterHandler
{
	private static final Logger LOGGER = Logger.getLogger(MasterHandler.class.getName());
	
	private static final IHandler<?, ?>[] LOAD_INSTANCES =
	{
		ActionClickHandler.getInstance(),
		ActionShiftHandler.getInstance(),
		AdminCommandHandler.getInstance(),
		BypassHandler.getInstance(),
		ChatHandler.getInstance(),
		CommunityBoardHandler.getInstance(),
		ItemHandler.getInstance(),
		PunishmentHandler.getInstance(),
		UserCommandHandler.getInstance(),
		VoicedCommandHandler.getInstance(),
		TargetHandler.getInstance(),
	};
	
	private static final Class<?>[][] HANDLERS =
	{
		{
			// Action Handlers
			ArtefactClick.class,
			DecoyClick.class,
			DoorClick.class,
			ItemClick.class,
			NpcClick.class,
			PlayerClick.class,
			PetClick.class,
			StaticObjectClick.class,
			SummonClick.class,
			TrapClick.class,
		},
		{
			// Action Shift Handlers
			DoorShiftClick.class,
			ItemShiftClick.class,
			NpcShiftClick.class,
			PlayerShiftClick.class,
			StaticObjectShiftClick.class,
			SummonShiftClick.class,
		},
		{
			// Admin Command Handlers
			AdminAdmin.class,
			AdminAnnouncements.class,
			AdminAugment.class,
			AdminBuffs.class,
			AdminCamera.class,
			AdminChangeAccessLevel.class,
			AdminCHSiege.class,
			AdminClan.class,
			AdminCreateItem.class,
			AdminCursedWeapons.class,
			AdminDebug.class,
			AdminDelete.class,
			AdminDestroyItems.class,
			AdminDisconnect.class,
			AdminDoorControl.class,
			AdminEditChar.class,
			AdminEffects.class,
			AdminEnchant.class,
			AdminEvents.class,
			AdminExpSp.class,
			AdminFakePlayers.class,
			AdminFence.class,
			AdminFightCalculator.class,
			AdminGamePoints.class,
			AdminGeodata.class,
			AdminGm.class,
			AdminGmChat.class,
			AdminGmSpeed.class,
			AdminGoto.class,
			AdminGrandBoss.class,
			AdminHeal.class,
			AdminHelp.class,
			AdminHide.class,
			AdminHtml.class,
			AdminInstance.class,
			AdminInstanceZone.class,
			AdminInvul.class,
			AdminKick.class,
			AdminKill.class,
			AdminLevel.class,
			AdminLogin.class,
			AdminMammon.class,
			AdminManor.class,
			AdminMenu.class,
			AdminMessages.class,
			AdminOnline.class,
			AdminPathNode.class,
			AdminPcCafePoints.class,
			AdminPetition.class,
			AdminPForge.class,
			AdminPledge.class,
			AdminPremium.class,
			AdminPunishment.class,
			AdminQuest.class,
			AdminRegions.class,
			AdminRates.class,
			AdminReload.class,
			AdminRepairChar.class,
			AdminRes.class,
			AdminRide.class,
			AdminScan.class,
			AdminSearch.class,
			AdminServerInfo.class,
			AdminSevenSigns.class,
			AdminShop.class,
			AdminShowQuests.class,
			AdminShutdown.class,
			AdminSiege.class,
			AdminSkill.class,
			AdminSpawn.class,
			AdminSummon.class,
			AdminSuperHaste.class,
			AdminTarget.class,
			AdminTargetSay.class,
			AdminTeleport.class,
			AdminTest.class,
			AdminVitality.class,
			AdminZone.class,
			AdminFpcRoute.class,
			AdminZoneBuild.class,
		},
		{
			// Bypass Handlers
			Augment.class,
			Buy.class,
			BuyShadowItem.class,
			ChatLink.class,
			ClanWarehouse.class,
			EnchantSkillList.class,
			Festival.class,
			FindPvP.class,
			Freight.class,
			Link.class,
			Loto.class,
			Multisell.class,
			NpcViewMod.class,
			Observation.class,
			PlayerHelp.class,
			PrivateWarehouse.class,
			QuestList.class,
			Rift.class,
			Sell.class,
			ScriptLink.class,
			SkillList.class,
			SupportBlessing.class,
			SupportMagic.class,
			TerritoryStatus.class,
			TutorialClose.class,
			VoiceCommand.class,
			Wear.class,
		},
		{
			// Chat Handlers
			ChatGeneral.class,
			ChatAlliance.class,
			ChatBattlefield.class,
			ChatClan.class,
			ChatHeroVoice.class,
			ChatParty.class,
			ChatPartyMatchRoom.class,
			ChatPartyRoomAll.class,
			ChatPartyRoomCommander.class,
			ChatPetition.class,
			ChatShout.class,
			ChatWhisper.class,
			ChatTrade.class,
		},
		{
			// Community Board
			ClanBoard.class,
			DropSearchBoard.class,
			FavoriteBoard.class,
			FriendsBoard.class,
			HomeBoard.class,
			HomepageBoard.class,
			MailBoard.class,
			MemoBoard.class,
			RegionBoard.class,
		},
		{
			// Item Handlers
			BeastSoulShot.class,
			BeastSpice.class,
			BeastSpiritShot.class,
			BlessedSpiritShot.class,
			Book.class,
			Calculator.class,
			CharmOfCourage.class,
			Elixir.class,
			EnchantScrolls.class,
			ExtractableItems.class,
			FishShots.class,
			Harvester.class,
			ItemSkills.class,
			ItemSkillsTemplate.class,
			Maps.class,
			MercTicket.class,
			PetFood.class,
			Recipes.class,
			RollingDice.class,
			Seed.class,
			SevenSignsRecord.class,
			SoulShots.class,
			SpecialXMas.class,
			SpiritShot.class,
			SummonItems.class,
		},
		{
			// Punishment Handlers
			BanHandler.class,
			ChatBanHandler.class,
			JailHandler.class,
		},
		{
			// User Command Handlers
			ClanPenalty.class,
			ClanWarsList.class,
			Dismount.class,
			Unstuck.class,
			Loc.class,
			Mount.class,
			PartyInfo.class,
			Time.class,
			OlympiadStat.class,
			ChannelLeave.class,
			ChannelDelete.class,
			ChannelInfo.class,
			SiegeStatus.class,
		},
		{
			// TODO: Add configuration options for this voiced commands.
			// CastleHandler.class,
			// ClanHandler.class,
			ExperienceGain.class,
			WeddingConfig.ALLOW_WEDDING ? Wedding.class : null,
			AutoPlayConfig.ENABLE_AUTO_PLAY ? AutoPlay.class : null,
			BankingConfig.BANKING_SYSTEM_ENABLED ? Banking.class : null,
			ChatModerationConfig.CHAT_ADMIN ? ChatAdmin.class : null,
			MultilingualSupportConfig.MULTILANG_ENABLE && MultilingualSupportConfig.MULTILANG_VOICED_ALLOW ? Lang.class : null,
			PasswordChangeConfig.ALLOW_CHANGE_PASSWORD ? ChangePassword.class : null,
			OfflinePlayConfig.ENABLE_OFFLINE_PLAY_COMMAND ? OfflinePlay.class : null,
			OfflineTradeConfig.ENABLE_OFFLINE_COMMAND && (OfflineTradeConfig.OFFLINE_TRADE_ENABLE || OfflineTradeConfig.OFFLINE_CRAFT_ENABLE) ? Offline.class : null,
			OnlineInfoConfig.ENABLE_ONLINE_COMMAND ? Online.class : null,
			PremiumSystemConfig.PREMIUM_SYSTEM_ENABLED ? Premium.class : null,
			AutoPotionsConfig.AUTO_POTIONS_ENABLED ? AutoPotion.class : null,
		},
		{
			// Target Handlers
			Area.class,
			AreaCorpseMob.class,
			AreaFriendly.class,
			AreaSummon.class,
			Aura.class,
			AuraCorpseMob.class,
			AuraFriendly.class,
			BehindArea.class,
			BehindAura.class,
			Clan.class,
			ClanMember.class,
			CommandChannel.class,
			CorpseClan.class,
			CorpseMob.class,
			EnemySummon.class,
			FlagPole.class,
			FrontArea.class,
			FrontAura.class,
			Ground.class,
			Holy.class,
			One.class,
			OwnerPet.class,
			Party.class,
			PartyClan.class,
			PartyMember.class,
			PartyNotMe.class,
			PartyOther.class,
			PcBody.class,
			Pet.class,
			Self.class,
			Servitor.class,
			Summon.class,
			TargetParty.class,
			Unlockable.class,
		},
	};
	
	public static void main(String[] args)
	{
		LOGGER.log(Level.INFO, "Loading Handlers...");
		
		final Map<IHandler<?, ?>, Method> registerHandlerMethods = new HashMap<>();
		for (IHandler<?, ?> loadInstance : LOAD_INSTANCES)
		{
			registerHandlerMethods.put(loadInstance, null);
			for (Method method : loadInstance.getClass().getMethods())
			{
				if (method.getName().equals("registerHandler") && !method.isBridge())
				{
					registerHandlerMethods.put(loadInstance, method);
				}
			}
		}
		
		registerHandlerMethods.entrySet().stream().filter(e -> e.getValue() == null).forEach(e -> LOGGER.log(Level.WARNING, "Failed loading handlers of: " + e.getKey().getClass().getSimpleName() + " seems registerHandler function does not exist."));
		
		for (Class<?>[] classes : HANDLERS)
		{
			for (Class<?> c : classes)
			{
				if (c == null)
				{
					continue; // Disabled handler
				}
				
				try
				{
					final Object handler = c.getDeclaredConstructor().newInstance();
					for (Entry<IHandler<?, ?>, Method> entry : registerHandlerMethods.entrySet())
					{
						if ((entry.getValue() != null) && entry.getValue().getParameterTypes()[0].isInstance(handler))
						{
							entry.getValue().invoke(entry.getKey(), handler);
						}
					}
				}
				catch (Exception e)
				{
					LOGGER.log(Level.WARNING, "Failed loading handler: " + c.getSimpleName(), e);
				}
			}
		}
		
		for (IHandler<?, ?> loadInstance : LOAD_INSTANCES)
		{
			LOGGER.log(Level.INFO, loadInstance.getClass().getSimpleName() + ": Loaded " + loadInstance.size() + " handlers.");
		}
		
		LOGGER.log(Level.INFO, "Handlers Loaded...");
	}
}
