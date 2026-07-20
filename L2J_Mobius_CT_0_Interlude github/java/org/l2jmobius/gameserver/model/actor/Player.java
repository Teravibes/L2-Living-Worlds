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
package org.l2jmobius.gameserver.model.actor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.LoginServerThread;
import org.l2jmobius.gameserver.ai.CreatureAI;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.ai.PlayerAI;
import org.l2jmobius.gameserver.ai.SummonAI;
import org.l2jmobius.gameserver.cache.RelationCache;
import org.l2jmobius.gameserver.communitybbs.BB.Forum;
import org.l2jmobius.gameserver.communitybbs.Manager.ForumsBBSManager;
import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.PvpConfig;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.config.custom.AutoPlayConfig;
import org.l2jmobius.gameserver.config.custom.DualboxCheckConfig;
import org.l2jmobius.gameserver.config.custom.FactionSystemConfig;
import org.l2jmobius.gameserver.config.custom.FakePlayersConfig;
import org.l2jmobius.gameserver.config.custom.FreeMountsConfig;
import org.l2jmobius.gameserver.config.custom.MultilingualSupportConfig;
import org.l2jmobius.gameserver.config.custom.OfflinePlayConfig;
import org.l2jmobius.gameserver.config.custom.OfflineTradeConfig;
import org.l2jmobius.gameserver.config.custom.PremiumSystemConfig;
import org.l2jmobius.gameserver.config.custom.PrivateStoreRangeConfig;
import org.l2jmobius.gameserver.config.custom.PvpAnnounceConfig;
import org.l2jmobius.gameserver.config.custom.PvpRewardItemConfig;
import org.l2jmobius.gameserver.config.custom.PvpTitleColorConfig;
import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.data.holders.AccessLevel;
import org.l2jmobius.gameserver.data.holders.PetLevelData;
import org.l2jmobius.gameserver.data.holders.SellBuffHolder;
import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.data.sql.CharSummonTable;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.sql.OfflinePlayTable;
import org.l2jmobius.gameserver.data.sql.OfflineTraderTable;
import org.l2jmobius.gameserver.data.xml.AdminData;
import org.l2jmobius.gameserver.data.xml.CategoryData;
import org.l2jmobius.gameserver.data.xml.ClassListData;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.data.xml.ExperienceLossData;
import org.l2jmobius.gameserver.data.xml.FishData;
import org.l2jmobius.gameserver.data.xml.HennaData;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.data.xml.PetDataTable;
import org.l2jmobius.gameserver.data.xml.PlayerTemplateData;
import org.l2jmobius.gameserver.data.xml.RecipeData;
import org.l2jmobius.gameserver.data.xml.SendMessageLocalisationData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.handler.ItemHandler;
import org.l2jmobius.gameserver.managers.AntiFeedManager;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.CoupleManager;
import org.l2jmobius.gameserver.managers.CursedWeaponsManager;
import org.l2jmobius.gameserver.managers.DimensionalRiftManager;
import org.l2jmobius.gameserver.managers.DuelManager;
import org.l2jmobius.gameserver.managers.GrandBossManager;
import org.l2jmobius.gameserver.managers.IdManager;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.managers.ItemManager;
import org.l2jmobius.gameserver.managers.ItemsOnGroundManager;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.managers.RecipeManager;
import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.managers.SiegeManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.appearance.PlayerAppearance;
import org.l2jmobius.gameserver.model.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.actor.enums.creature.Team;
import org.l2jmobius.gameserver.model.actor.enums.player.IllegalActionPunishmentType;
import org.l2jmobius.gameserver.model.actor.enums.player.MountType;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerAction;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.actor.enums.player.PrivateStoreType;
import org.l2jmobius.gameserver.model.actor.enums.player.Sex;
import org.l2jmobius.gameserver.model.actor.enums.player.ShortcutType;
import org.l2jmobius.gameserver.model.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.model.actor.holders.creature.TimeStamp;
import org.l2jmobius.gameserver.model.actor.holders.player.AutoPlaySettingsHolder;
import org.l2jmobius.gameserver.model.actor.holders.player.AutoUseSettingsHolder;
import org.l2jmobius.gameserver.model.actor.holders.player.BlockList;
import org.l2jmobius.gameserver.model.actor.holders.player.ContactList;
import org.l2jmobius.gameserver.model.actor.holders.player.Duel;
import org.l2jmobius.gameserver.model.actor.holders.player.Macro;
import org.l2jmobius.gameserver.model.actor.holders.player.MacroList;
import org.l2jmobius.gameserver.model.actor.holders.player.Radar;
import org.l2jmobius.gameserver.model.actor.holders.player.Shortcut;
import org.l2jmobius.gameserver.model.actor.holders.player.Shortcuts;
import org.l2jmobius.gameserver.model.actor.holders.player.SubClassHolder;
import org.l2jmobius.gameserver.model.actor.instance.Boat;
import org.l2jmobius.gameserver.model.actor.instance.Cubic;
import org.l2jmobius.gameserver.model.actor.instance.Decoy;
import org.l2jmobius.gameserver.model.actor.instance.Defender;
import org.l2jmobius.gameserver.model.actor.instance.Door;
import org.l2jmobius.gameserver.model.actor.instance.EventMonster;
import org.l2jmobius.gameserver.model.actor.instance.FriendlyMob;
import org.l2jmobius.gameserver.model.actor.instance.Guard;
import org.l2jmobius.gameserver.model.actor.instance.TamedBeast;
import org.l2jmobius.gameserver.model.actor.instance.Trap;
import org.l2jmobius.gameserver.model.actor.stat.PlayerStat;
import org.l2jmobius.gameserver.model.actor.status.PlayerStatus;
import org.l2jmobius.gameserver.model.actor.tasks.player.DismountTask;
import org.l2jmobius.gameserver.model.actor.tasks.player.FameTask;
import org.l2jmobius.gameserver.model.actor.tasks.player.InventoryEnableTask;
import org.l2jmobius.gameserver.model.actor.tasks.player.LookingForFishTask;
import org.l2jmobius.gameserver.model.actor.tasks.player.PetFeedTask;
import org.l2jmobius.gameserver.model.actor.tasks.player.RentPetTask;
import org.l2jmobius.gameserver.model.actor.tasks.player.ResetChargesTask;
import org.l2jmobius.gameserver.model.actor.tasks.player.SitDownTask;
import org.l2jmobius.gameserver.model.actor.tasks.player.StandUpTask;
import org.l2jmobius.gameserver.model.actor.tasks.player.TeleportWatchdogTask;
import org.l2jmobius.gameserver.model.actor.tasks.player.VitalityTask;
import org.l2jmobius.gameserver.model.actor.tasks.player.WarnUserTakeBreakTask;
import org.l2jmobius.gameserver.model.actor.tasks.player.WaterTask;
import org.l2jmobius.gameserver.model.actor.templates.PlayerTemplate;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.clan.ClanAccess;
import org.l2jmobius.gameserver.model.clan.ClanMember;
import org.l2jmobius.gameserver.model.clan.ClanPrivileges;
import org.l2jmobius.gameserver.model.effects.EffectFlag;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.playable.OnPlayableExpChanged;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerFameChanged;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerHennaRemove;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerItemEquip;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerKarmaChanged;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogout;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerPKChanged;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerProfessionCancel;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerProfessionChange;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerPvPChanged;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerPvPKill;
import org.l2jmobius.gameserver.model.events.listeners.FunctionEventListener;
import org.l2jmobius.gameserver.model.events.returns.TerminateReturn;
import org.l2jmobius.gameserver.model.fishing.Fish;
import org.l2jmobius.gameserver.model.fishing.Fishing;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.groups.PartyDistributionType;
import org.l2jmobius.gameserver.model.groups.PartyMessageType;
import org.l2jmobius.gameserver.model.groups.matching.PartyMatchRoom;
import org.l2jmobius.gameserver.model.groups.matching.PartyMatchRoomList;
import org.l2jmobius.gameserver.model.groups.matching.PartyMatchWaitingList;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.interfaces.ILocational;
import org.l2jmobius.gameserver.model.item.Armor;
import org.l2jmobius.gameserver.model.item.EtcItem;
import org.l2jmobius.gameserver.model.item.Henna;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.Weapon;
import org.l2jmobius.gameserver.model.item.enums.BodyPart;
import org.l2jmobius.gameserver.model.item.enums.ItemLocation;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.enums.ShotType;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.item.recipe.ManufactureItem;
import org.l2jmobius.gameserver.model.item.recipe.RecipeList;
import org.l2jmobius.gameserver.model.item.type.ActionType;
import org.l2jmobius.gameserver.model.item.type.ArmorType;
import org.l2jmobius.gameserver.model.item.type.EtcItemType;
import org.l2jmobius.gameserver.model.item.type.WeaponType;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.itemcontainer.ItemContainer;
import org.l2jmobius.gameserver.model.itemcontainer.PetInventory;
import org.l2jmobius.gameserver.model.itemcontainer.PlayerFreight;
import org.l2jmobius.gameserver.model.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.model.itemcontainer.PlayerRefund;
import org.l2jmobius.gameserver.model.itemcontainer.PlayerWarehouse;
import org.l2jmobius.gameserver.model.multisell.PreparedListContainer;
import org.l2jmobius.gameserver.model.olympiad.Hero;
import org.l2jmobius.gameserver.model.olympiad.Olympiad;
import org.l2jmobius.gameserver.model.punishment.PunishmentAffect;
import org.l2jmobius.gameserver.model.punishment.PunishmentType;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.QuestTimer;
import org.l2jmobius.gameserver.model.script.timers.TimerHolder;
import org.l2jmobius.gameserver.model.sevensigns.SevenSigns;
import org.l2jmobius.gameserver.model.sevensigns.SevenSignsFestival;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.model.siege.Siege;
import org.l2jmobius.gameserver.model.skill.AbnormalType;
import org.l2jmobius.gameserver.model.skill.BuffInfo;
import org.l2jmobius.gameserver.model.skill.CommonSkill;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.enums.Element;
import org.l2jmobius.gameserver.model.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.model.skill.holders.SkillLearn;
import org.l2jmobius.gameserver.model.skill.holders.SkillUseHolder;
import org.l2jmobius.gameserver.model.skill.targets.TargetType;
import org.l2jmobius.gameserver.model.stats.Formulas;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.model.variables.AccountVariables;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.model.zone.ZoneRegion;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.model.zone.type.BossZone;
import org.l2jmobius.gameserver.model.zone.type.WaterZone;
import org.l2jmobius.gameserver.network.Disconnection;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.enums.HtmlActionScope;
import org.l2jmobius.gameserver.network.holders.RequestPartner;
import org.l2jmobius.gameserver.network.holders.TradeList;
import org.l2jmobius.gameserver.network.serverpackets.AbstractHtmlPacket;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ChangeWaitType;
import org.l2jmobius.gameserver.network.serverpackets.CharInfo;
import org.l2jmobius.gameserver.network.serverpackets.ConfirmDlg;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.EtcStatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.ExAutoSoulShot;
import org.l2jmobius.gameserver.network.serverpackets.ExDuelUpdateUserInfo;
import org.l2jmobius.gameserver.network.serverpackets.ExFishingEnd;
import org.l2jmobius.gameserver.network.serverpackets.ExFishingStart;
import org.l2jmobius.gameserver.network.serverpackets.ExOlympiadMode;
import org.l2jmobius.gameserver.network.serverpackets.ExOlympiadUserInfo;
import org.l2jmobius.gameserver.network.serverpackets.ExSetCompassZoneCode;
import org.l2jmobius.gameserver.network.serverpackets.ExStorageMaxCount;
import org.l2jmobius.gameserver.network.serverpackets.ExUseSharedGroupItem;
import org.l2jmobius.gameserver.network.serverpackets.FriendStatusPacket;
import org.l2jmobius.gameserver.network.serverpackets.GetOnVehicle;
import org.l2jmobius.gameserver.network.serverpackets.HennaInfo;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.ItemList;
import org.l2jmobius.gameserver.network.serverpackets.LeaveWorld;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.MyTargetSelected;
import org.l2jmobius.gameserver.network.serverpackets.NicknameChanged;
import org.l2jmobius.gameserver.network.serverpackets.ObservationEnter;
import org.l2jmobius.gameserver.network.serverpackets.ObservationExit;
import org.l2jmobius.gameserver.network.serverpackets.PartySmallWindowUpdate;
import org.l2jmobius.gameserver.network.serverpackets.PetInventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;
import org.l2jmobius.gameserver.network.serverpackets.PledgeShowMemberListDelete;
import org.l2jmobius.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import org.l2jmobius.gameserver.network.serverpackets.PrivateStoreListBuy;
import org.l2jmobius.gameserver.network.serverpackets.PrivateStoreListSell;
import org.l2jmobius.gameserver.network.serverpackets.PrivateStoreManageListBuy;
import org.l2jmobius.gameserver.network.serverpackets.PrivateStoreManageListSell;
import org.l2jmobius.gameserver.network.serverpackets.PrivateStoreMsgBuy;
import org.l2jmobius.gameserver.network.serverpackets.PrivateStoreMsgSell;
import org.l2jmobius.gameserver.network.serverpackets.RecipeShopMsg;
import org.l2jmobius.gameserver.network.serverpackets.RecipeShopSellList;
import org.l2jmobius.gameserver.network.serverpackets.RelationChanged;
import org.l2jmobius.gameserver.network.serverpackets.Ride;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.network.serverpackets.SetupGauge;
import org.l2jmobius.gameserver.network.serverpackets.ShortcutInit;
import org.l2jmobius.gameserver.network.serverpackets.SkillCoolTime;
import org.l2jmobius.gameserver.network.serverpackets.SkillList;
import org.l2jmobius.gameserver.network.serverpackets.Snoop;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.StopMove;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.TargetSelected;
import org.l2jmobius.gameserver.network.serverpackets.TargetUnselected;
import org.l2jmobius.gameserver.network.serverpackets.TradeDone;
import org.l2jmobius.gameserver.network.serverpackets.TradeOtherDone;
import org.l2jmobius.gameserver.network.serverpackets.TradeStart;
import org.l2jmobius.gameserver.network.serverpackets.UserInfo;
import org.l2jmobius.gameserver.network.serverpackets.ValidateLocation;
import org.l2jmobius.gameserver.taskmanagers.AttackStanceTaskManager;
import org.l2jmobius.gameserver.taskmanagers.AutoPlayTaskManager;
import org.l2jmobius.gameserver.taskmanagers.AutoUseTaskManager;
import org.l2jmobius.gameserver.taskmanagers.DecayTaskManager;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;
import org.l2jmobius.gameserver.taskmanagers.ItemsAutoDestroyTaskManager;
import org.l2jmobius.gameserver.taskmanagers.PlayerAutoSaveTaskManager;
import org.l2jmobius.gameserver.taskmanagers.PvpFlagTaskManager;
import org.l2jmobius.gameserver.util.Broadcast;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * This class represents all player characters in the world.<br>
 * There is always a client-thread connected to this (except if a player-store is activated upon logout).
 */
public class Player extends Playable
{
	// Character Skill SQL String Definitions:
	private static final String RESTORE_SKILLS_FOR_CHAR = "SELECT skill_id,skill_level FROM character_skills WHERE charId=? AND class_index=?";
	private static final String UPDATE_CHARACTER_SKILL_LEVEL = "UPDATE character_skills SET skill_level=? WHERE skill_id=? AND charId=? AND class_index=?";
	private static final String ADD_NEW_SKILLS = "REPLACE INTO character_skills (charId,skill_id,skill_level,class_index) VALUES (?,?,?,?)";
	private static final String DELETE_SKILL_FROM_CHAR = "DELETE FROM character_skills WHERE skill_id=? AND charId=? AND class_index=?";
	private static final String DELETE_CHAR_SKILLS = "DELETE FROM character_skills WHERE charId=? AND class_index=?";
	
	// Character Skill Save SQL String Definitions:
	private static final String ADD_SKILL_SAVE = "REPLACE INTO character_skills_save (charId,skill_id,skill_level,remaining_time,reuse_delay,systime,restore_type,class_index,buff_index) VALUES (?,?,?,?,?,?,?,?,?)";
	private static final String RESTORE_SKILL_SAVE = "SELECT skill_id,skill_level,remaining_time, reuse_delay, systime, restore_type FROM character_skills_save WHERE charId=? AND class_index=? ORDER BY buff_index ASC";
	private static final String DELETE_SKILL_SAVE = "DELETE FROM character_skills_save WHERE charId=? AND class_index=?";
	
	// Character Item Reuse Time String Definition:
	private static final String ADD_ITEM_REUSE_SAVE = "INSERT INTO character_item_reuse_save (charId,itemId,itemObjId,reuseDelay,systime) VALUES (?,?,?,?,?)";
	private static final String RESTORE_ITEM_REUSE_SAVE = "SELECT charId,itemId,itemObjId,reuseDelay,systime FROM character_item_reuse_save WHERE charId=?";
	private static final String DELETE_ITEM_REUSE_SAVE = "DELETE FROM character_item_reuse_save WHERE charId=?";
	
	// Character Character SQL String Definitions:
	private static final String INSERT_CHARACTER = "INSERT INTO characters (account_name,charId,char_name,level,maxHp,curHp,maxCp,curCp,maxMp,curMp,face,hairStyle,hairColor,sex,exp,sp,karma,fame,pvpkills,pkkills,clanid,race,classid,deletetime,cancraft,title,title_color,accesslevel,online,isin7sdungeon,clan_privs,wantspeace,base_class,newbie,nobless,power_grade,createDate,lastAccess,last_recom_date) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
	private static final String UPDATE_CHARACTER = "UPDATE characters SET level=?,maxHp=?,curHp=?,maxCp=?,curCp=?,maxMp=?,curMp=?,face=?,hairStyle=?,hairColor=?,sex=?,heading=?,x=?,y=?,z=?,exp=?,expBeforeDeath=?,sp=?,karma=?,fame=?,pvpkills=?,pkkills=?,clanid=?,race=?,classid=?,deletetime=?,title=?,title_color=?,accesslevel=?,online=?,isin7sdungeon=?,clan_privs=?,wantspeace=?,base_class=?,onlinetime=?,newbie=?,nobless=?,power_grade=?,subpledge=?,lvl_joined_academy=?,apprentice=?,sponsor=?,clan_join_expiry_time=?,clan_create_expiry_time=?,char_name=?,death_penalty_level=?,bookmarkslot=?,vitality_points=?,language=?,faction=?,pccafe_points=?,last_recom_date=?,rec_have=?,rec_left=? WHERE charId=?";
	private static final String RESTORE_CHARACTER = "SELECT * FROM characters WHERE charId=?";
	
	// Character Subclass SQL String Definitions:
	private static final String RESTORE_CHAR_SUBCLASSES = "SELECT class_id,exp,sp,level,class_index FROM character_subclasses WHERE charId=? ORDER BY class_index ASC";
	private static final String ADD_CHAR_SUBCLASS = "INSERT INTO character_subclasses (charId,class_id,exp,sp,level,class_index) VALUES (?,?,?,?,?,?)";
	private static final String UPDATE_CHAR_SUBCLASS = "UPDATE character_subclasses SET exp=?,sp=?,level=?,class_id=? WHERE charId=? AND class_index =?";
	private static final String DELETE_CHAR_SUBCLASS = "DELETE FROM character_subclasses WHERE charId=? AND class_index=?";
	
	// Character Henna SQL String Definitions:
	private static final String RESTORE_CHAR_HENNAS = "SELECT slot,symbol_id FROM character_hennas WHERE charId=? AND class_index=?";
	private static final String ADD_CHAR_HENNA = "INSERT INTO character_hennas (charId,symbol_id,slot,class_index) VALUES (?,?,?,?)";
	private static final String DELETE_CHAR_HENNA = "DELETE FROM character_hennas WHERE charId=? AND slot=? AND class_index=?";
	
	// Character Shortcut SQL String Definitions:
	private static final String DELETE_CHAR_SHORTCUTS = "DELETE FROM character_shortcuts WHERE charId=? AND class_index=?";
	
	// Character Recommendation SQL String Definitions:
	private static final String RESTORE_CHAR_RECOMS = "SELECT charId,target_id FROM character_recommends WHERE charId=?";
	private static final String ADD_CHAR_RECOM = "INSERT INTO character_recommends (charId,target_id) VALUES (?,?)";
	private static final String DELETE_CHAR_RECOMS = "DELETE FROM character_recommends WHERE charId=?";
	
	// Character Recipe List Save
	private static final String DELETE_CHAR_RECIPE_SHOP = "DELETE FROM character_recipeshoplist WHERE charId=?";
	private static final String INSERT_CHAR_RECIPE_SHOP = "REPLACE INTO character_recipeshoplist (`charId`, `recipeId`, `price`, `index`) VALUES (?, ?, ?, ?)";
	private static final String RESTORE_CHAR_RECIPE_SHOP = "SELECT * FROM character_recipeshoplist WHERE charId=? ORDER BY `index`";
	
	public static final String NEWBIE_KEY = "NEWBIE";
	
	public static final int ID_NONE = -1;
	
	public static final int REQUEST_TIMEOUT = 15;
	
	private int _pcCafePoints = 0;
	
	private GameClient _client;
	private String _ip = "N/A";
	
	private final String _accountName;
	private long _deleteTimer;
	private Calendar _createDate = Calendar.getInstance();
	
	private String _lang = null;
	private String _htmlPrefix = "";
	
	private volatile boolean _isOnline = false;
	private boolean _offlinePlay = false;
	private boolean _enteredWorld = false;
	private long _onlineTime;
	private long _onlineBeginTime;
	private long _lastAccess;
	private long _uptime;
	
	private final InventoryUpdate _inventoryUpdate = new InventoryUpdate();
	private ScheduledFuture<?> _inventoryUpdateTask;
	private ScheduledFuture<?> _itemListTask;
	private ScheduledFuture<?> _skillListTask;
	private ScheduledFuture<?> _updateAndBroadcastStatusTask;
	private ScheduledFuture<?> _broadcastCharInfoTask;
	private ScheduledFuture<?> _broadcastStatusUpdateTask;
	
	private boolean _subclassLock = false;
	protected int _baseClass;
	protected int _activeClass;
	protected int _classIndex = 0;
	
	/** data for mounted pets */
	private int _controlItemId;
	private PetLevelData _leveldata;
	private int _curFeed;
	protected Future<?> _mountFeedTask;
	private ScheduledFuture<?> _dismountTask;
	private boolean _petItems = false;
	
	/** The list of sub-classes this character has. */
	private final Map<Integer, SubClassHolder> _subClasses = new ConcurrentHashMap<>();
	
	private final PlayerAppearance _appearance;
	
	/** The Experience of the Player before the last Death Penalty */
	private long _expBeforeDeath;
	
	/** The number of player killed during a PvP (the player killed was PvP Flagged) */
	private int _pvpKills;
	
	/** The PK counter of the Player (= Number of non PvP Flagged player killed) */
	private int _pkKills;
	
	/** The PvP Flag state of the Player (0=White, 1=Purple) */
	private byte _pvpFlag;
	
	/** The Fame of this Player */
	private int _fame;
	private ScheduledFuture<?> _fameTask;
	
	/** Vitality recovery task */
	private ScheduledFuture<?> _vitalityTask;
	
	private ScheduledFuture<?> _teleportWatchdog;
	
	/** The Siege state of the Player */
	private byte _siegeState = 0;
	
	/** The id of castle/fort which the Player is registered for siege */
	private int _siegeSide = 0;
	
	private int _curWeightPenalty = 0;
	
	private int _lastCompassZone; // the last compass zone update send to the client
	
	private boolean _isIn7sDungeon = false;
	
	private final ContactList _contactList = new ContactList(this);
	
	// Friend list.
	private final Collection<Integer> _friendList = ConcurrentHashMap.newKeySet();
	
	// Related to Community Board.
	private final List<Integer> _selectedFriendList = new ArrayList<>();
	private final List<Integer> _selectedBlocksList = new ArrayList<>();
	private int _mailPosition;
	
	private boolean _canFeed;
	private boolean _isInSiege;
	private boolean _isInHideoutSiege = false;
	
	/** Olympiad */
	private boolean _inOlympiadMode = false;
	private boolean _olympiadStart = false;
	private int _olympiadGameId = -1;
	private int _olympiadSide = -1;
	/** Olympiad buff count. */
	private int _olyBuffsCount = 0;
	
	/** Duel */
	private boolean _isInDuel = false;
	private boolean _startingDuel = false;
	private int _duelState = Duel.DUELSTATE_NODUEL;
	private int _duelId = 0;
	private SystemMessageId _noDuelReason = SystemMessageId.THERE_IS_NO_OPPONENT_TO_RECEIVE_YOUR_CHALLENGE_FOR_A_DUEL;
	
	/** Boat and AirShip */
	private Vehicle _vehicle = null;
	private Location _inVehiclePosition;
	
	public ScheduledFuture<?> _taskForFish;
	private MountType _mountType = MountType.NONE;
	private int _mountNpcId;
	private int _mountLevel;
	/** Store object used to summon the strider you are mounting **/
	private int _mountObjectID = 0;
	
	public int _telemode = 0;
	
	private boolean _inCrystallize;
	private boolean _isCrafting;
	
	private long _offlineShopStart = 0;
	
	/** The table containing all RecipeList of the Player */
	private final Map<Integer, RecipeList> _dwarvenRecipeBook = new ConcurrentHashMap<>();
	private final Map<Integer, RecipeList> _commonRecipeBook = new ConcurrentHashMap<>();
	
	/** True if the Player is sitting */
	private boolean _waitTypeSitting;
	private boolean _sittingInProgress;
	
	/** Location before entering Observer Mode */
	private final Location _lastLoc = new Location(0, 0, 0);
	private boolean _observerMode = false;
	
	/** Stored from last ValidatePosition **/
	private final Location _lastServerPosition = new Location(0, 0, 0);
	
	private final AtomicBoolean _blinkActive = new AtomicBoolean();
	
	private final Map<Element, AtomicInteger> _elementSeeds = new HashMap<>(4);
	{
		_elementSeeds.put(Element.FIRE, new AtomicInteger());
		_elementSeeds.put(Element.WATER, new AtomicInteger());
		_elementSeeds.put(Element.WIND, new AtomicInteger());
		_elementSeeds.put(Element.EARTH, new AtomicInteger());
	}
	
	/** The number of recommendation obtained by the Player */
	private int _recomHave; // how much I was recommended by others
	/** The number of recommendation that the Player can give */
	private int _recomLeft; // how many recommendations I can give to others
	/** Date when recommendation points were updated last time */
	private long _lastRecomUpdate;
	/** List with the recommendations this player gave */
	private final Collection<Integer> _recomChars = ConcurrentHashMap.newKeySet();
	
	private final PlayerInventory _inventory = new PlayerInventory(this);
	private final PlayerFreight _freight = new PlayerFreight(this);
	private final PlayerWarehouse _warehouse = new PlayerWarehouse(this);
	private PlayerRefund _refund;
	
	private PrivateStoreType _privateStoreType = PrivateStoreType.NONE;
	
	private TradeList _activeTradeList;
	private ItemContainer _activeWarehouse;
	private Map<Integer, ManufactureItem> _manufactureItems;
	private String _storeName = "";
	private TradeList _sellList;
	private TradeList _buyList;
	
	// Multisell
	private PreparedListContainer _currentMultiSell = null;
	
	private boolean _newbie = false;
	private boolean _noble = false;
	private boolean _hero = false;
	
	/** Premium System */
	private boolean _premiumStatus = false;
	
	/** Faction System */
	private boolean _isGood = false;
	private boolean _isEvil = false;
	
	/** The Npc corresponding to the last Folk which one the player talked. */
	private Npc _lastFolkNpc = null;
	
	/** Last NPC Id talked on a quest */
	private int _questNpcObject = 0;
	
	/** The table containing all Quests began by the Player */
	private final Map<String, QuestState> _quests = new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);
	
	/** The list containing all shortcuts of this player. */
	private final Shortcuts _shortcuts = new Shortcuts(this);
	
	/** The list containing all macros of this player. */
	private final MacroList _macros = new MacroList(this);
	
	private final Set<Player> _snoopListener = ConcurrentHashMap.newKeySet(1);
	private final Set<Player> _snoopedPlayer = ConcurrentHashMap.newKeySet(1);
	
	/** Hennas */
	private final Henna[] _henna = new Henna[3];
	private int _hennaSTR;
	private int _hennaINT;
	private int _hennaDEX;
	private int _hennaMEN;
	private int _hennaWIT;
	private int _hennaCON;
	
	/** The Summon of the Player */
	private Summon _summon = null;
	/** The Decoy of the Player */
	private Decoy _decoy = null;
	/** The Trap of the Player */
	private Trap _trap = null;
	/** The Agathion of the Player */
	private int _agathionId = 0;
	
	// apparently, a Player CAN have both a summon AND a tamed beast at the same time!!
	// after Freya players can control more than one tamed beast
	private Collection<TamedBeast> _tamedBeast = null;
	
	private boolean _minimapAllowed = false;
	
	// client radar
	// TODO: This needs to be better integrated and saved/loaded
	private final Radar _radar;
	
	private ScheduledFuture<?> _taskWarnUserTakeBreak;
	
	// Party matching
	// private int _partymatching = 0;
	private int _partyroom = 0;
	// private int _partywait = 0;
	
	// Clan related attributes
	/** The Clan Identifier of the Player */
	private int _clanId;
	
	/** The Clan object of the Player */
	private Clan _clan;
	
	/** Apprentice and Sponsor IDs */
	private int _apprentice = 0;
	private int _sponsor = 0;
	
	private long _clanJoinExpiryTime;
	private long _clanCreateExpiryTime;
	
	private int _powerGrade = 0;
	private ClanPrivileges _clanPrivileges = new ClanPrivileges();
	
	/** Player's pledge class (knight, Baron, etc.) */
	private int _pledgeClass = 0;
	private int _pledgeType = 0;
	
	/** Level at which the player joined the clan as an academy member */
	private int _lvlJoinedAcademy = 0;
	
	private int _wantsPeace = 0;
	
	// Death Penalty Buff Level
	private int _deathPenaltyBuffLevel = 0;
	
	// charges
	private final AtomicInteger _charges = new AtomicInteger();
	private ScheduledFuture<?> _chargeTask = null;
	
	// WorldPosition used by TARGET_SIGNET_GROUND
	private Location _currentSkillWorldPosition;
	
	private AccessLevel _accessLevel;
	
	private boolean _messageRefusal = false; // message refusal mode
	
	private boolean _silenceMode = false; // silence mode
	private List<Integer> _silenceModeExcluded; // silence mode
	private boolean _dietMode = false; // ignore weight penalty
	private boolean _tradeRefusal = false; // Trade refusal
	private boolean _exchangeRefusal = false; // Exchange refusal
	
	private Party _party;
	PartyDistributionType _partyDistributionType;
	
	// this is needed to find the inviting player for Party response
	// there can only be one active party request at once
	private Player _activeRequester;
	private long _requestExpireTime = 0;
	private final RequestPartner _request = new RequestPartner(this);
	private Item _arrowItem;
	
	// Used for protection after teleport
	private long _spawnProtectEndTime = 0;
	private long _teleportProtectEndTime = 0;
	
	// protects a char from aggro mobs when getting up from fake death
	private long _recentFakeDeathEndTime = 0;
	private boolean _isFakeDeath;
	
	/** The fists Weapon of the Player (used when no weapon is equipped) */
	private Weapon _fistsWeaponItem;
	
	private final Map<Integer, String> _chars = new LinkedHashMap<>();
	
	// private byte _updateKnownCounter = 0;
	
	private int _expertiseArmorPenalty = 0;
	private int _expertiseWeaponPenalty = 0;
	private int _expertisePenaltyBonus = 0;
	
	private boolean _isEnchanting = false;
	private int _activeEnchantItemId = ID_NONE;
	private long _activeEnchantTimestamp = 0;
	
	protected boolean _inventoryDisable = false;
	/** Player's cubics. */
	private final Map<Integer, Cubic> _cubics = new ConcurrentSkipListMap<>(); // TODO(Zoey76): This should be sorted in insert order.
	/** Active shots. */
	protected Set<Integer> _activeSoulShots = ConcurrentHashMap.newKeySet(1);
	
	public ReentrantLock soulShotLock = new ReentrantLock();
	
	/** Event parameters */
	private boolean _isRegisteredOnEvent = false;
	private boolean _isOnSoloEvent = false;
	private boolean _isOnEvent = false;
	
	/** new loto ticket **/
	private final int[] _loto = new int[5];
	/** new race ticket **/
	private final int[] _raceTickets = new int[2];
	
	private final BlockList _blockList = new BlockList(this);
	
	private Fish _fish;
	private Item _lure;
	private Fishing _fishCombat;
	private boolean _fishing = false;
	private int _fishX = 0;
	private int _fishY = 0;
	private int _fishZ = 0;
	
	private ScheduledFuture<?> _taskRentPet;
	private ScheduledFuture<?> _taskWater;
	
	/** Last Html Npcs, 0 = last html was not bound to an npc */
	private final int[] _htmlActionOriginObjectIds = new int[HtmlActionScope.values().length];
	/**
	 * Origin of the last incoming html action request.<br>
	 * This can be used for htmls continuing the conversation with an npc.
	 */
	private int _lastHtmlActionOriginObjId;
	
	/** Bypass validations */
	@SuppressWarnings("unchecked")
	private final LinkedList<String>[] _htmlActionCaches = new LinkedList[HtmlActionScope.values().length];
	
	private Forum _forumMail;
	private Forum _forumMemo;
	
	/** Current skill in use. Note that Creature has _lastSkillCast, but this has the button presses */
	private SkillUseHolder _currentSkill;
	private SkillUseHolder _currentPetSkill;
	
	/** Skills queued because a skill is already in progress */
	private SkillUseHolder _queuedSkill;
	
	private int _cursedWeaponEquippedId = 0;
	
	private boolean _canRevive = true;
	private int _reviveRequested = 0;
	private double _revivePower = 0;
	private boolean _revivePet = false;
	
	private double _cpUpdateIncCheck = .0;
	private double _cpUpdateDecCheck = .0;
	private double _cpUpdateInterval = .0;
	private double _mpUpdateIncCheck = .0;
	private double _mpUpdateDecCheck = .0;
	private double _mpUpdateInterval = .0;
	
	private double _originalCp = .0;
	private double _originalHp = .0;
	private double _originalMp = .0;
	
	/** Char Coords from Client */
	private int _clientX;
	private int _clientY;
	private int _clientZ;
	private int _clientHeading;
	
	// during fall validations will be disabled for 1000 ms.
	private static final int FALLING_VALIDATION_DELAY = 1000;
	private volatile long _fallingTimestamp = 0;
	private volatile int _fallingDamage = 0;
	private Future<?> _fallingDamageTask = null;
	
	private int _multiSocialTarget = 0;
	private int _multiSociaAction = 0;
	
	private String _adminConfirmCmd = null;
	
	private volatile long _lastItemAuctionInfoRequest = 0;
	
	private long _pvpFlagLasts;
	
	private long _notMoveUntil = 0;
	
	/** Map containing all custom skills of this player. */
	private Map<Integer, Skill> _customSkills = null;
	
	private volatile int _actionMask;
	
	private Map<Stat, Double> _servitorShare;
	
	/**
	 * Creates a player.
	 * @param objectId the object ID
	 * @param template the player template
	 * @param accountName the account name
	 * @param app the player appearance
	 */
	private Player(int objectId, PlayerTemplate template, String accountName, PlayerAppearance app)
	{
		super(objectId, template);
		setInstanceType(InstanceType.Player);
		initCharStatusUpdateValues();
		initPcStatusUpdateValues();
		
		for (int i = 0; i < _htmlActionCaches.length; ++i)
		{
			_htmlActionCaches[i] = new LinkedList<>();
		}
		
		_accountName = accountName;
		app.setOwner(this);
		_appearance = app;
		
		// Create an AI
		getAI();
		
		// Create a Radar object
		_radar = new Radar(this);
		startVitalityTask();
	}
	
	/**
	 * Creates a player.
	 * @param template the player template
	 * @param accountName the account name
	 * @param app the player appearance
	 */
	private Player(PlayerTemplate template, String accountName, PlayerAppearance app)
	{
		this(IdManager.getInstance().getNextId(), template, accountName, app);
	}
	
	public void setPvpFlagLasts(long time)
	{
		_pvpFlagLasts = time;
	}
	
	public long getPvpFlagLasts()
	{
		return _pvpFlagLasts;
	}
	
	public void startPvPFlag()
	{
		updatePvPFlag(1);
		PvpFlagTaskManager.getInstance().add(this);
	}
	
	public void stopPvpRegTask()
	{
		PvpFlagTaskManager.getInstance().remove(this);
	}
	
	public void stopPvPFlag()
	{
		stopPvpRegTask();
		updatePvPFlag(0);
	}
	
	// L2JMOD Wedding
	private boolean _married = false;
	private int _partnerId = 0;
	private int _coupleId = 0;
	private boolean _engagerequest = false;
	private int _engageid = 0;
	private boolean _marryrequest = false;
	private boolean _marryaccepted = false;
	
	// Item Mall
	private static final String GAME_POINTS_VAR = "PRIME_POINTS"; // Keep compatibility with later clients.
	
	// Save responder name for log it
	private String _lastPetitionGmName = null;
	
	private boolean _hasCharmOfCourage = false;
	
	private final AutoPlaySettingsHolder _autoPlaySettings = new AutoPlaySettingsHolder();
	private final AutoUseSettingsHolder _autoUseSettings = new AutoUseSettingsHolder();
	private final AtomicBoolean _autoPlaying = new AtomicBoolean();
	
	private final List<QuestTimer> _questTimers = new ArrayList<>();
	private final List<TimerHolder<?>> _timerHolders = new ArrayList<>();
	
	// Selling buffs system
	private boolean _isSellingBuffs = false;
	private List<SellBuffHolder> _sellingBuffs = null;
	
	public boolean isSellingBuffs()
	{
		return _isSellingBuffs;
	}
	
	public void setSellingBuffs(boolean value)
	{
		_isSellingBuffs = value;
	}
	
	public List<SellBuffHolder> getSellingBuffs()
	{
		if (_sellingBuffs == null)
		{
			_sellingBuffs = new ArrayList<>();
		}
		
		return _sellingBuffs;
	}
	
	/**
	 * Create a new Player and add it in the characters table of the database.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Create a new Player with an account name</li>
	 * <li>Set the name, the Hair Style, the Hair Color and the Face type of the Player</li>
	 * <li>Add the player in the characters table of the database</li>
	 * </ul>
	 * @param template The PlayerTemplate to apply to the Player
	 * @param accountName The name of the Player
	 * @param name The name of the Player
	 * @param app the player's appearance
	 * @return The Player added to the database or null
	 */
	public static Player create(PlayerTemplate template, String accountName, String name, PlayerAppearance app)
	{
		// Create a new Player with an account name
		final Player player = new Player(template, accountName, app);
		
		// Set the name of the Player
		player.setName(name);
		
		// Set Character's create time
		player.setCreateDate(Calendar.getInstance());
		
		// Set the base class ID to that of the actual class ID.
		player.setBaseClass(player.getPlayerClass());
		
		// Set the player as a newbie if there are no other characters associated with the current account.
		player.setNewbie(PlayerConfig.ALT_GAME_NEW_CHAR_ALWAYS_IS_NEWBIE || (CharInfoTable.getInstance().accountCharNumber(accountName) == 0));
		
		// Add the player in the characters table of the database
		return player.createDb() ? player : null;
	}
	
	public String getAccountName()
	{
		return _client == null ? _accountName : _client.getAccountName();
	}
	
	public String getAccountNamePlayer()
	{
		return _accountName;
	}
	
	public Map<Integer, String> getAccountChars()
	{
		return _chars;
	}
	
	public int getRelation(Player target)
	{
		int result = 0;
		
		// karma and pvp may not be required
		if (getPvpFlag() != 0)
		{
			result |= RelationChanged.RELATION_PVP_FLAG;
		}
		
		if (getKarma() > 0)
		{
			result |= RelationChanged.RELATION_HAS_KARMA;
		}
		
		if (isClanLeader())
		{
			result |= RelationChanged.RELATION_LEADER;
		}
		
		if (getSiegeState() != 0)
		{
			result |= RelationChanged.RELATION_INSIEGE;
			if (getSiegeState() != target.getSiegeState())
			{
				result |= RelationChanged.RELATION_ENEMY;
			}
			else
			{
				result |= RelationChanged.RELATION_ALLY;
			}
			
			if (getSiegeState() == 1)
			{
				result |= RelationChanged.RELATION_ATTACKER;
			}
		}
		
		if ((getClan() != null) && (target.getClan() != null) && (target.getPledgeType() != Clan.SUBUNIT_ACADEMY) && (getPledgeType() != Clan.SUBUNIT_ACADEMY) && target.getClan().isAtWarWith(getClan().getId()))
		{
			result |= RelationChanged.RELATION_1SIDED_WAR;
			if (getClan().isAtWarWith(target.getClan().getId()))
			{
				result |= RelationChanged.RELATION_MUTUAL_WAR;
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieve a Player from the characters table of the database and add it in _allObjects of the L2world (call restore method).<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Retrieve the Player from the characters table of the database</li>
	 * <li>Add the Player object in _allObjects</li>
	 * <li>Set the x,y,z position of the Player and make it invisible</li>
	 * <li>Update the overloaded status of the Player</li>
	 * </ul>
	 * @param objectId Identifier of the object to initialized
	 * @return The Player loaded from the database
	 */
	public static Player load(int objectId)
	{
		return restore(objectId);
	}
	
	private void initPcStatusUpdateValues()
	{
		_cpUpdateInterval = getMaxCp() / 352.0;
		_cpUpdateIncCheck = getMaxCp();
		_cpUpdateDecCheck = getMaxCp() - _cpUpdateInterval;
		_mpUpdateInterval = getMaxMp() / 352.0;
		_mpUpdateIncCheck = getMaxMp();
		_mpUpdateDecCheck = getMaxMp() - _mpUpdateInterval;
	}
	
	@Override
	public PlayerStat getStat()
	{
		return (PlayerStat) super.getStat();
	}
	
	@Override
	public void initCharStat()
	{
		setStat(new PlayerStat(this));
	}
	
	@Override
	public PlayerStatus getStatus()
	{
		return (PlayerStatus) super.getStatus();
	}
	
	@Override
	public void initCharStatus()
	{
		setStatus(new PlayerStatus(this));
	}
	
	public PlayerAppearance getAppearance()
	{
		return _appearance;
	}
	
	/**
	 * @return the base PlayerTemplate link to the Player.
	 */
	public PlayerTemplate getBaseTemplate()
	{
		return PlayerTemplateData.getInstance().getTemplate(_baseClass);
	}
	
	/**
	 * @return the PlayerTemplate link to the Player.
	 */
	@Override
	public PlayerTemplate getTemplate()
	{
		return (PlayerTemplate) super.getTemplate();
	}
	
	/**
	 * @param newclass
	 */
	public void setTemplate(PlayerClass newclass)
	{
		super.setTemplate(PlayerTemplateData.getInstance().getTemplate(newclass));
	}
	
	@Override
	protected CreatureAI initAI()
	{
		return new PlayerAI(this);
	}
	
	/** Return the Level of the Player. */
	@Override
	public int getLevel()
	{
		return getStat().getLevel();
	}
	
	/**
	 * Return the _newbie state of the Player.
	 * @return true, if is newbie.
	 */
	public boolean isNewbie()
	{
		return PlayerConfig.ALT_GAME_NEW_CHAR_ALWAYS_IS_NEWBIE || _newbie;
	}
	
	/**
	 * Set the _newbie state of the Player.
	 * @param isNewbie The Identifier of the _newbie state.
	 */
	public void setNewbie(boolean isNewbie)
	{
		_newbie = isNewbie;
	}
	
	public void setBaseClass(int baseClass)
	{
		_baseClass = baseClass;
	}
	
	public void setBaseClass(PlayerClass playerClass)
	{
		_baseClass = playerClass.getId();
	}
	
	public boolean isInStoreMode()
	{
		return _privateStoreType != PrivateStoreType.NONE;
	}
	
	public boolean isCrafting()
	{
		return _isCrafting;
	}
	
	public void setCrafting(boolean isCrafting)
	{
		_isCrafting = isCrafting;
	}
	
	/**
	 * @return a table containing all Common RecipeList of the Player.
	 */
	public Collection<RecipeList> getCommonRecipeBook()
	{
		return _commonRecipeBook.values();
	}
	
	/**
	 * @return a table containing all Dwarf RecipeList of the Player.
	 */
	public Collection<RecipeList> getDwarvenRecipeBook()
	{
		return _dwarvenRecipeBook.values();
	}
	
	/**
	 * Add a new RecipList to the table _commonrecipebook containing all RecipeList of the Player
	 * @param recipe The RecipeList to add to the _recipebook
	 * @param saveToDb
	 */
	public void registerCommonRecipeList(RecipeList recipe, boolean saveToDb)
	{
		_commonRecipeBook.put(recipe.getId(), recipe);
		
		if (saveToDb)
		{
			insertNewRecipeData(recipe.getId(), false);
		}
	}
	
	/**
	 * Add a new RecipList to the table _recipebook containing all RecipeList of the Player
	 * @param recipe The RecipeList to add to the _recipebook
	 * @param saveToDb
	 */
	public void registerDwarvenRecipeList(RecipeList recipe, boolean saveToDb)
	{
		_dwarvenRecipeBook.put(recipe.getId(), recipe);
		
		if (saveToDb)
		{
			insertNewRecipeData(recipe.getId(), true);
		}
	}
	
	/**
	 * @param recipeId The Identifier of the RecipeList to check in the player's recipe books
	 * @return {@code true}if player has the recipe on Common or Dwarven Recipe book else returns {@code false}
	 */
	public boolean hasRecipeList(int recipeId)
	{
		return _dwarvenRecipeBook.containsKey(recipeId) || _commonRecipeBook.containsKey(recipeId);
	}
	
	/**
	 * Tries to remove a RecipList from the table _DwarvenRecipeBook or from table _CommonRecipeBook, those table contain all RecipeList of the Player
	 * @param recipeId The Identifier of the RecipeList to remove from the _recipebook
	 */
	public void unregisterRecipeList(int recipeId)
	{
		if (_dwarvenRecipeBook.remove(recipeId) != null)
		{
			deleteRecipeData(recipeId, true);
		}
		else if (_commonRecipeBook.remove(recipeId) != null)
		{
			deleteRecipeData(recipeId, false);
		}
		else
		{
			LOGGER.warning("Attempted to remove unknown RecipeList: " + recipeId);
		}
		
		for (Shortcut sc : _shortcuts.getAllShortcuts())
		{
			if ((sc != null) && (sc.getId() == recipeId) && (sc.getType() == ShortcutType.RECIPE))
			{
				deleteShortcut(sc.getSlot(), sc.getPage());
			}
		}
	}
	
	private void insertNewRecipeData(int recipeId, boolean isDwarf)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("INSERT INTO character_recipebook (charId, id, classIndex, type) values(?,?,?,?)"))
		{
			ps.setInt(1, getObjectId());
			ps.setInt(2, recipeId);
			ps.setInt(3, isDwarf ? _classIndex : 0);
			ps.setInt(4, isDwarf ? 1 : 0);
			ps.execute();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "SQL exception while inserting recipe: " + recipeId + " from character " + getObjectId(), e);
		}
	}
	
	private void deleteRecipeData(int recipeId, boolean isDwarf)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM character_recipebook WHERE charId=? AND id=? AND classIndex=?"))
		{
			ps.setInt(1, getObjectId());
			ps.setInt(2, recipeId);
			ps.setInt(3, isDwarf ? _classIndex : 0);
			ps.execute();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "SQL exception while deleting recipe: " + recipeId + " from character " + getObjectId(), e);
		}
	}
	
	/**
	 * @return the Id for the last talked quest NPC.
	 */
	public int getLastQuestNpcObject()
	{
		return _questNpcObject;
	}
	
	public void setLastQuestNpcObject(int npcId)
	{
		_questNpcObject = npcId;
	}
	
	/**
	 * @param quest The name of the quest
	 * @return the QuestState object corresponding to the quest name.
	 */
	public QuestState getQuestState(String quest)
	{
		return _quests.get(quest);
	}
	
	/**
	 * Add a QuestState to the table _quest containing all quests began by the Player.
	 * @param qs The QuestState to add to _quest
	 */
	public void setQuestState(QuestState qs)
	{
		_quests.put(qs.getQuestName(), qs);
	}
	
	/**
	 * Verify if the player has the quest state.
	 * @param quest the quest state to check
	 * @return {@code true} if the player has the quest state, {@code false} otherwise
	 */
	public boolean hasQuestState(String quest)
	{
		return _quests.containsKey(quest);
	}
	
	/**
	 * Remove a QuestState from the table _quest containing all quests began by the Player.
	 * @param quest The name of the quest
	 */
	public void delQuestState(String quest)
	{
		_quests.remove(quest);
	}
	
	/**
	 * Gets all the active quests.
	 * @return a list of active quests
	 */
	public List<Quest> getAllActiveQuests()
	{
		final List<Quest> quests = new LinkedList<>();
		for (QuestState qs : _quests.values())
		{
			if ((qs == null) || (qs.getQuest() == null) || !qs.isStarted())
			{
				continue;
			}
			
			// Ignore other scripts.
			final int questId = qs.getQuest().getId();
			if ((questId > 19999) || (questId < 1))
			{
				continue;
			}
			
			quests.add(qs.getQuest());
		}
		
		return quests;
	}
	
	public void processScriptEvent(String scriptName, String event)
	{
		final Quest quest = ScriptManager.getInstance().getScript(scriptName);
		if ((quest == null) || (event == null) || event.isEmpty())
		{
			return;
		}
		
		final Npc target = _lastFolkNpc;
		if ((target != null) && isInsideRadius2D(target, Npc.INTERACTION_DISTANCE))
		{
			quest.notifyEvent(event, target, this);
		}
		else if (_questNpcObject > 0)
		{
			final WorldObject object = World.getInstance().findObject(getLastQuestNpcObject());
			if ((object != null) && object.isNpc() && isInsideRadius2D(object, Npc.INTERACTION_DISTANCE))
			{
				final Npc npc = object.asNpc();
				quest.notifyEvent(event, npc, this);
			}
		}
	}
	
	/** List of all QuestState instance that needs to be notified of this Player's or its pet's death */
	private final Collection<QuestState> _notifyQuestOfDeathList = ConcurrentHashMap.newKeySet();
	
	/**
	 * Add QuestState instance that is to be notified of Player's death.
	 * @param qs The QuestState that subscribe to this event
	 */
	public void addNotifyQuestOfDeath(QuestState qs)
	{
		if (qs == null)
		{
			return;
		}
		
		if (!_notifyQuestOfDeathList.contains(qs))
		{
			_notifyQuestOfDeathList.add(qs);
		}
	}
	
	/**
	 * Remove QuestState instance that is to be notified of Player's death.
	 * @param qs The QuestState that subscribe to this event
	 */
	public void removeNotifyQuestOfDeath(QuestState qs)
	{
		if (qs == null)
		{
			return;
		}
		
		_notifyQuestOfDeathList.remove(qs);
	}
	
	/**
	 * @return a list of QuestStates which registered for notify of death of this Player.
	 */
	public Collection<QuestState> getNotifyQuestOfDeath()
	{
		return _notifyQuestOfDeathList;
	}
	
	public boolean isNotifyQuestOfDeathEmpty()
	{
		return _notifyQuestOfDeathList.isEmpty();
	}
	
	/**
	 * @return a collection containing all Shortcut of the Player.
	 */
	public Collection<Shortcut> getAllShortcuts()
	{
		return _shortcuts.getAllShortcuts();
	}
	
	/**
	 * @param slot The slot in which the shortcuts is equipped
	 * @param page The page of shortcuts containing the slot
	 * @return the Shortcut of the Player corresponding to the position (page-slot).
	 */
	public Shortcut getShortcut(int slot, int page)
	{
		return _shortcuts.getShortcut(slot, page);
	}
	
	/**
	 * Add a L2shortcut to the Player _shortcuts
	 * @param shortcut
	 */
	public void registerShortcut(Shortcut shortcut)
	{
		_shortcuts.registerShortcut(shortcut);
	}
	
	/**
	 * Updates the shortcut bars with the new skill.
	 * @param skillId the skill Id to search and update.
	 * @param skillLevel the skill level to update.
	 */
	public void updateShortcuts(int skillId, int skillLevel)
	{
		_shortcuts.updateShortcuts(skillId, skillLevel);
	}
	
	/**
	 * Delete the Shortcut corresponding to the position (page-slot) from the Player _shortcuts.
	 * @param slot
	 * @param page
	 */
	public void deleteShortcut(int slot, int page)
	{
		_shortcuts.deleteShortcut(slot, page);
	}
	
	/**
	 * @param macro the macro to add to this Player.
	 */
	public void registerMacro(Macro macro)
	{
		_macros.registerMacro(macro);
	}
	
	/**
	 * @param id the macro Id to delete.
	 */
	public void deleteMacro(int id)
	{
		_macros.deleteMacro(id);
	}
	
	/**
	 * @return all Macro of the Player.
	 */
	public MacroList getMacros()
	{
		return _macros;
	}
	
	/**
	 * Set the siege state of the Player.
	 * @param siegeState 1 = attacker, 2 = defender, 0 = not involved
	 */
	public void setSiegeState(byte siegeState)
	{
		_siegeState = siegeState;
	}
	
	/**
	 * Get the siege state of the Player.
	 * @return 1 = attacker, 2 = defender, 0 = not involved
	 */
	@Override
	public byte getSiegeState()
	{
		return _siegeState;
	}
	
	/**
	 * Set the siege Side of the Player.
	 * @param value
	 */
	public void setSiegeSide(int value)
	{
		_siegeSide = value;
	}
	
	public boolean isRegisteredOnThisSiegeField(int value)
	{
		return (_siegeSide == value) || ((_siegeSide >= 81) && (_siegeSide <= 89));
	}
	
	@Override
	public int getSiegeSide()
	{
		return _siegeSide;
	}
	
	public boolean isSiegeFriend(WorldObject target)
	{
		// If i'm natural or not in siege zone, not friends.
		if ((_siegeState == 0) || !isInsideZone(ZoneId.SIEGE))
		{
			return false;
		}
		
		final Castle castle = CastleManager.getInstance().getCastleById(_siegeSide);
		if (castle == null)
		{
			return false;
		}
		
		// If target isn't a player, is self.
		final Player targetPlayer = target.asPlayer();
		if ((targetPlayer == null) || (targetPlayer == this))
		{
			return false;
		}
		
		// If target isn't on same siege or not on same state, not friends.
		if ((targetPlayer.getSiegeSide() != _siegeSide) || (_siegeState != targetPlayer.getSiegeState()))
		{
			return false;
		}
		
		if (_siegeState == 1)
		{
			// Check first castle mid victory.
			if (!castle.isFirstMidVictory() && (_siegeState == targetPlayer.getSiegeState()))
			{
				return true;
			}
			
			// Attackers are considered friends only if castle has no owner.
			return castle.getOwner() == null;
		}
		
		// Both are defenders, friends.
		return true;
	}
	
	/**
	 * Set the PvP Flag of the Player.
	 * @param pvpFlag
	 */
	public void setPvpFlag(int pvpFlag)
	{
		_pvpFlag = (byte) pvpFlag;
	}
	
	@Override
	public byte getPvpFlag()
	{
		return _pvpFlag;
	}
	
	@Override
	public void updatePvPFlag(int value)
	{
		if (_pvpFlag == value)
		{
			return;
		}
		
		setPvpFlag(value);
		
		updateUserInfo();
		
		// If this player has a pet update the pets pvp flag as well
		if (hasSummon())
		{
			sendPacket(new RelationChanged(_summon, getRelation(this), false));
		}
		
		World.getInstance().forEachVisibleObject(this, Player.class, target ->
		{
			target.sendPacket(new RelationChanged(this, getRelation(target), isAutoAttackable(target)));
			if (hasSummon())
			{
				target.sendPacket(new RelationChanged(_summon, getRelation(target), isAutoAttackable(target)));
			}
		});
	}
	
	@Override
	public void revalidateZone(boolean force)
	{
		// Cannot validate if not in a world region (happens during teleport)
		if (getWorldRegion() == null)
		{
			return;
		}
		
		// This function is called too often from movement code.
		if (!force && (calculateDistance3D(_lastZoneValidateLocation) < 100))
		{
			return;
		}
		
		_lastZoneValidateLocation.setXYZ(this);
		
		ZoneManager.getInstance().getRegion(this).revalidateZones(this);
		
		if (GeneralConfig.ALLOW_WATER)
		{
			checkWaterState();
		}
		
		if (isInsideZone(ZoneId.ALTERED))
		{
			if (_lastCompassZone == ExSetCompassZoneCode.ALTEREDZONE)
			{
				return;
			}
			
			_lastCompassZone = ExSetCompassZoneCode.ALTEREDZONE;
			sendPacket(new ExSetCompassZoneCode(ExSetCompassZoneCode.ALTEREDZONE));
		}
		else if (isInsideZone(ZoneId.SIEGE))
		{
			if (_lastCompassZone == ExSetCompassZoneCode.SIEGEWARZONE2)
			{
				return;
			}
			
			_lastCompassZone = ExSetCompassZoneCode.SIEGEWARZONE2;
			sendPacket(new ExSetCompassZoneCode(ExSetCompassZoneCode.SIEGEWARZONE2));
		}
		else if (isInsideZone(ZoneId.PVP))
		{
			if (_lastCompassZone == ExSetCompassZoneCode.PVPZONE)
			{
				return;
			}
			
			_lastCompassZone = ExSetCompassZoneCode.PVPZONE;
			sendPacket(new ExSetCompassZoneCode(ExSetCompassZoneCode.PVPZONE));
		}
		else if (_isIn7sDungeon)
		{
			if (_lastCompassZone == ExSetCompassZoneCode.SEVENSIGNSZONE)
			{
				return;
			}
			
			_lastCompassZone = ExSetCompassZoneCode.SEVENSIGNSZONE;
			sendPacket(new ExSetCompassZoneCode(ExSetCompassZoneCode.SEVENSIGNSZONE));
		}
		else if (isInsideZone(ZoneId.PEACE))
		{
			if (_lastCompassZone == ExSetCompassZoneCode.PEACEZONE)
			{
				return;
			}
			
			_lastCompassZone = ExSetCompassZoneCode.PEACEZONE;
			sendPacket(new ExSetCompassZoneCode(ExSetCompassZoneCode.PEACEZONE));
		}
		else if (isInsideZone(ZoneId.NO_PVP))
		{
			if (_lastCompassZone == ExSetCompassZoneCode.PEACEZONE)
			{
				return;
			}
			
			_lastCompassZone = ExSetCompassZoneCode.PEACEZONE;
			sendPacket(new ExSetCompassZoneCode(ExSetCompassZoneCode.PEACEZONE));
		}
		else
		{
			if (_lastCompassZone == ExSetCompassZoneCode.GENERALZONE)
			{
				return;
			}
			
			if (_lastCompassZone == ExSetCompassZoneCode.SIEGEWARZONE2)
			{
				updatePvPStatus();
			}
			
			_lastCompassZone = ExSetCompassZoneCode.GENERALZONE;
			sendPacket(new ExSetCompassZoneCode(ExSetCompassZoneCode.GENERALZONE));
		}
	}
	
	/**
	 * @return True if the Player can Craft Dwarven Recipes.
	 */
	public boolean hasDwarvenCraft()
	{
		return getSkillLevel(CommonSkill.CREATE_DWARVEN.getId()) >= 1;
	}
	
	public int getDwarvenCraft()
	{
		return getSkillLevel(CommonSkill.CREATE_DWARVEN.getId());
	}
	
	/**
	 * @return True if the Player can Craft Dwarven Recipes.
	 */
	public boolean hasCommonCraft()
	{
		return getSkillLevel(CommonSkill.CREATE_COMMON.getId()) >= 1;
	}
	
	public int getCommonCraft()
	{
		return getSkillLevel(CommonSkill.CREATE_COMMON.getId());
	}
	
	/**
	 * @return the PK counter of the Player.
	 */
	public int getPkKills()
	{
		return _pkKills;
	}
	
	/**
	 * Set the PK counter of the Player.
	 * @param pkKills
	 */
	public void setPkKills(int pkKills)
	{
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_PK_CHANGED, this))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerPKChanged(this, _pkKills, pkKills), this);
		}
		
		_pkKills = pkKills;
	}
	
	/**
	 * @return the _deleteTimer of the Player.
	 */
	public long getDeleteTimer()
	{
		return _deleteTimer;
	}
	
	/**
	 * Set the _deleteTimer of the Player.
	 * @param deleteTimer
	 */
	public void setDeleteTimer(long deleteTimer)
	{
		_deleteTimer = deleteTimer;
	}
	
	/**
	 * @return the date of last update of recomPoints.
	 */
	public long getLastRecomUpdate()
	{
		return _lastRecomUpdate;
	}
	
	public void setLastRecomUpdate(long date)
	{
		_lastRecomUpdate = date;
	}
	
	/**
	 * @return the number of recommendation obtained by the Player.
	 */
	public int getRecomHave()
	{
		return _recomHave;
	}
	
	/**
	 * Increment the number of recommendation obtained by the Player (Max : 255).
	 */
	protected void incRecomHave()
	{
		if (_recomHave < 255)
		{
			_recomHave++;
		}
	}
	
	/**
	 * Set the number of recommendation obtained by the Player (Max : 255).
	 * @param value
	 */
	public void setRecomHave(int value)
	{
		_recomHave = Math.min(Math.max(value, 0), 255);
	}
	
	/**
	 * @return the number of recommendation that the Player can give.
	 */
	public int getRecomLeft()
	{
		return _recomLeft;
	}
	
	/**
	 * Increment the number of recommendation that the Player can give.
	 */
	protected void decRecomLeft()
	{
		if (_recomLeft > 0)
		{
			_recomLeft--;
		}
	}
	
	public void giveRecom(Player target)
	{
		target.incRecomHave();
		decRecomLeft();
		_recomChars.add(target.getObjectId());
		if (!PlayerConfig.ALT_RECOMMEND)
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement(ADD_CHAR_RECOM))
			{
				ps.setInt(1, getObjectId());
				ps.setInt(2, target.getObjectId());
				ps.execute();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "Failed updating character recommendations for player: " + getObjectId(), e);
			}
		}
	}
	
	public boolean canRecom(Player target)
	{
		return !_recomChars.contains(target.getObjectId());
	}
	
	/**
	 * Retrieve from the database all Recommendation data of this player, add to _recomChars and calculate stats of the player.
	 */
	private void restoreRecom()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(RESTORE_CHAR_RECOMS))
		{
			ps.setInt(1, getObjectId());
			final ResultSet rset = ps.executeQuery();
			while (rset.next())
			{
				_recomChars.add(rset.getInt("target_id"));
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not restore Recommendations for player: " + getObjectId(), e);
		}
	}
	
	private void checkRecom(int recsHave, int recsLeft)
	{
		final Calendar check = Calendar.getInstance();
		check.setTimeInMillis(_lastRecomUpdate);
		check.add(Calendar.DAY_OF_MONTH, 1);
		
		final Calendar min = Calendar.getInstance();
		_recomHave = recsHave;
		_recomLeft = recsLeft;
		if ((getStat().getLevel() < 10) || check.after(min))
		{
			return;
		}
		
		restartRecom();
	}
	
	public void restartRecom()
	{
		_recomChars.clear();
		
		if (!PlayerConfig.ALT_RECOMMEND)
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement(DELETE_CHAR_RECOMS))
			{
				ps.setInt(1, getObjectId());
				ps.execute();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "Failed cleaning character recommendations for player: " + getObjectId(), e);
			}
		}
		
		if (getStat().getLevel() < 20)
		{
			_recomLeft = 3;
			_recomHave--;
		}
		else if (getStat().getLevel() < 40)
		{
			_recomLeft = 6;
			_recomHave -= 2;
		}
		else
		{
			_recomLeft = 9;
			_recomHave -= 3;
		}
		
		if (_recomHave < 0)
		{
			_recomHave = 0;
		}
		
		// If we have to update last update time, but it's now before 13, we should set it to yesterday
		final Calendar update = Calendar.getInstance();
		if (update.get(Calendar.HOUR_OF_DAY) < 13)
		{
			update.add(Calendar.DAY_OF_MONTH, -1);
		}
		
		update.set(Calendar.HOUR_OF_DAY, 13);
		_lastRecomUpdate = update.getTimeInMillis();
	}
	
	/**
	 * Set the exp of the Player before a death
	 * @param exp
	 */
	public void setExpBeforeDeath(long exp)
	{
		_expBeforeDeath = exp;
	}
	
	public long getExpBeforeDeath()
	{
		return _expBeforeDeath;
	}
	
	/**
	 * Set the Karma of the Player and send a Server->Client packet StatusUpdate (broadcast).
	 * @param value
	 */
	@Override
	public void setKarma(int value)
	{
		// Notify to scripts.
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_KARMA_CHANGED, this))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerKarmaChanged(this, getKarma(), value), this);
		}
		
		int karma = value;
		if (karma < 0)
		{
			karma = 0;
		}
		
		if ((getKarma() == 0) && (karma > 0))
		{
			World.getInstance().forEachVisibleObject(this, Guard.class, object ->
			{
				if (object.getAI().getIntention() == Intention.IDLE)
				{
					object.getAI().setIntention(Intention.ACTIVE, null);
				}
			});
		}
		else if ((getKarma() > 0) && (karma == 0))
		{
			// Send a Server->Client StatusUpdate packet with Karma and PvP Flag to the Player and all Player to inform (broadcast)
			setKarmaFlag();
		}
		
		super.setKarma(karma);
		broadcastKarma();
	}
	
	public int getExpertiseArmorPenalty()
	{
		return _expertiseArmorPenalty;
	}
	
	public int getExpertiseWeaponPenalty()
	{
		return _expertiseWeaponPenalty;
	}
	
	public int getExpertisePenaltyBonus()
	{
		return _expertisePenaltyBonus;
	}
	
	public void setExpertisePenaltyBonus(int bonus)
	{
		_expertisePenaltyBonus = bonus;
	}
	
	public int getWeightPenalty()
	{
		return _dietMode ? 0 : _curWeightPenalty;
	}
	
	/**
	 * Update the overloaded status of the Player.
	 */
	public void refreshOverloaded()
	{
		final int maxLoad = getMaxLoad();
		if (maxLoad > 0)
		{
			final long weightproc = ((getCurrentLoad() - getBonusWeightPenalty()) * 1000) / getMaxLoad();
			int newWeightPenalty;
			if ((weightproc < 500) || _dietMode)
			{
				newWeightPenalty = 0;
			}
			else if (weightproc < 666)
			{
				newWeightPenalty = 1;
			}
			else if (weightproc < 800)
			{
				newWeightPenalty = 2;
			}
			else if (weightproc < 1000)
			{
				newWeightPenalty = 3;
			}
			else
			{
				newWeightPenalty = 4;
			}
			
			if (_curWeightPenalty != newWeightPenalty)
			{
				_curWeightPenalty = newWeightPenalty;
				if ((newWeightPenalty > 0) && !_dietMode)
				{
					addSkill(SkillData.getInstance().getSkill(4270, newWeightPenalty));
					setOverloaded(getCurrentLoad() > maxLoad);
				}
				else
				{
					removeSkill(getKnownSkill(4270), false, true);
					setOverloaded(false);
				}
				
				broadcastUserInfo();
				sendPacket(new EtcStatusUpdate(this));
			}
		}
	}
	
	public void refreshExpertisePenalty()
	{
		if (!PlayerConfig.EXPERTISE_PENALTY)
		{
			return;
		}
		
		final int expertiseLevel = getExpertiseLevel();
		int armorPenalty = 0;
		int weaponPenalty = 0;
		int crystaltype;
		for (Item item : _inventory.getItems())
		{
			if ((item != null) && item.isEquipped() && (item.getItemType() != EtcItemType.ARROW))
			{
				crystaltype = item.getTemplate().getCrystalType().getLevel();
				if (crystaltype > expertiseLevel)
				{
					if (item.isWeapon() && (crystaltype > weaponPenalty))
					{
						weaponPenalty = crystaltype;
					}
					else if (crystaltype > armorPenalty)
					{
						armorPenalty = crystaltype;
					}
				}
			}
		}
		
		// calc weapon penalty
		weaponPenalty = weaponPenalty - expertiseLevel - _expertisePenaltyBonus;
		weaponPenalty = Math.min(Math.max(weaponPenalty, 0), 4);
		
		// calc armor penalty
		armorPenalty = armorPenalty - expertiseLevel - _expertisePenaltyBonus;
		armorPenalty = Math.min(Math.max(armorPenalty, 0), 4);
		
		if ((_expertiseWeaponPenalty != weaponPenalty) || (_expertiseArmorPenalty != armorPenalty))
		{
			_expertiseWeaponPenalty = weaponPenalty;
			_expertiseArmorPenalty = armorPenalty;
			if ((_expertiseWeaponPenalty > 0) || (_expertiseArmorPenalty > 0))
			{
				addSkill(SkillData.getInstance().getSkill(4267, 1), false);
			}
			else
			{
				removeSkill(getKnownSkill(CommonSkill.GRADE_PENALTY.getId()), false, true);
			}
			
			sendPacket(new EtcStatusUpdate(this));
		}
	}
	
	public void useEquippableItem(Item item, boolean abortAttack)
	{
		// Check if the item is null.
		if (item == null)
		{
			return;
		}
		
		// Check if the item is owned by this player.
		if (item.getOwnerId() != getObjectId())
		{
			return;
		}
		
		// Check if the item is in the inventory.
		final ItemLocation itemLocation = item.getItemLocation();
		if ((itemLocation != ItemLocation.INVENTORY) && (itemLocation != ItemLocation.PAPERDOLL))
		{
			return;
		}
		
		// Equip or unEquip
		Collection<Item> items = null;
		final boolean isEquiped = item.isEquipped();
		final int oldInvLimit = getInventoryLimit();
		SystemMessage sm = null;
		if (isEquiped)
		{
			if (item.isEnchanted())
			{
				sm = new SystemMessage(SystemMessageId.THE_EQUIPMENT_S1_S2_HAS_BEEN_REMOVED);
				sm.addInt(item.getEnchantLevel());
			}
			else
			{
				sm = new SystemMessage(SystemMessageId.S1_HAS_BEEN_DISARMED);
			}
			
			sm.addItemName(item);
			sendPacket(sm);
			
			final BodyPart bodyPart = BodyPart.fromItem(item);
			items = _inventory.unEquipItemInBodySlotAndRecord(bodyPart);
		}
		else
		{
			items = _inventory.equipItemAndRecord(item);
			if (item.isEquipped())
			{
				final BodyPart bodyPart = item.getTemplate().getBodyPart();
				if (item.isEnchanted())
				{
					sm = new SystemMessage(SystemMessageId.EQUIPPED_S1_S2);
					sm.addInt(item.getEnchantLevel());
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.YOU_HAVE_EQUIPPED_YOUR_S1);
				}
				
				sm.addItemName(item);
				sendPacket(sm);
				
				// Consume mana - will start a task if required; returns if item is not a shadow item
				item.decreaseMana(false);
				
				if ((bodyPart == BodyPart.R_HAND) || (bodyPart == BodyPart.LR_HAND))
				{
					rechargeShots(true, true);
				}
				
				// Notify to scripts
				if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_ITEM_EQUIP, item.getTemplate()))
				{
					EventDispatcher.getInstance().notifyEventAsync(new OnPlayerItemEquip(this, item), item.getTemplate());
				}
			}
			else
			{
				sendPacket(SystemMessageId.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM);
			}
		}
		
		refreshExpertisePenalty();
		broadcastUserInfo();
		
		final InventoryUpdate iu = new InventoryUpdate();
		iu.addItems(items);
		sendInventoryUpdate(iu);
		
		if (abortAttack)
		{
			abortAttack();
		}
		
		if (getInventoryLimit() != oldInvLimit)
		{
			sendPacket(new ExStorageMaxCount(this));
		}
	}
	
	/**
	 * @return the the PvP Kills of the Player (Number of player killed during a PvP).
	 */
	public int getPvpKills()
	{
		return _pvpKills;
	}
	
	/**
	 * Set the the PvP Kills of the Player (Number of player killed during a PvP).
	 * @param pvpKills
	 */
	public void setPvpKills(int pvpKills)
	{
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_PVP_CHANGED, this))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerPvPChanged(this, _pvpKills, pvpKills), this);
		}
		
		_pvpKills = pvpKills;
	}
	
	/**
	 * @return the Fame of this Player
	 */
	public int getFame()
	{
		return _fame;
	}
	
	/**
	 * Set the Fame of this PlayerInstane
	 * @param fame
	 */
	public void setFame(int fame)
	{
		int newFame = fame;
		if (fame > PlayerConfig.MAX_PERSONAL_FAME_POINTS)
		{
			newFame = PlayerConfig.MAX_PERSONAL_FAME_POINTS;
		}
		else if (fame < 0)
		{
			newFame = 0;
		}
		
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_FAME_CHANGED, this))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerFameChanged(this, _fame, newFame), this);
		}
		
		_fame = newFame;
	}
	
	/**
	 * @return the PlayerClass of the Player contained in PlayerTemplate.
	 */
	public PlayerClass getPlayerClass()
	{
		return getTemplate().getPlayerClass();
	}
	
	/**
	 * Set the template of the Player.
	 * @param id The Identifier of the PlayerTemplate to set to the Player
	 */
	public void setPlayerClass(int id)
	{
		if (_subclassLock)
		{
			return;
		}
		
		_subclassLock = true;
		
		try
		{
			if ((_lvlJoinedAcademy != 0) && (_clan != null) && CategoryData.getInstance().isInCategory(CategoryType.THIRD_CLASS_GROUP, id))
			{
				if (_lvlJoinedAcademy <= 16)
				{
					_clan.addReputationScore(FeatureConfig.JOIN_ACADEMY_MAX_REP_SCORE);
				}
				else if (_lvlJoinedAcademy >= 39)
				{
					_clan.addReputationScore(FeatureConfig.JOIN_ACADEMY_MIN_REP_SCORE);
				}
				else
				{
					_clan.addReputationScore(FeatureConfig.JOIN_ACADEMY_MAX_REP_SCORE - ((_lvlJoinedAcademy - 16) * 20));
				}
				
				setLvlJoinedAcademy(0);
				
				// oust pledge member from the academy, cuz he has finished his 2nd class transfer
				final SystemMessage msg = new SystemMessage(SystemMessageId.CLAN_MEMBER_S1_HAS_BEEN_EXPELLED);
				msg.addPcName(this);
				_clan.broadcastToOnlineMembers(msg);
				_clan.broadcastToOnlineMembers(new PledgeShowMemberListDelete(getName()));
				_clan.removeClanMember(getObjectId(), 0);
				sendPacket(SystemMessageId.CONGRATULATIONS_YOU_WILL_NOW_GRADUATE_FROM_THE_CLAN_ACADEMY_AND_LEAVE_YOUR_CURRENT_CLAN_AS_A_GRADUATE_OF_THE_ACADEMY_YOU_CAN_IMMEDIATELY_JOIN_A_CLAN_AS_A_REGULAR_MEMBER_WITHOUT_BEING_SUBJECT_TO_ANY_PENALTIES);
				
				// receive graduation gift
				_inventory.addItem(ItemProcessType.REWARD, 8181, 1, this, null); // give academy circlet
			}
			
			if (isSubClassActive())
			{
				getSubClasses().get(_classIndex).setPlayerClass(id);
			}
			
			setTarget(this);
			broadcastSkillPacket(new MagicSkillUse(this, 5103, 1, 0, 0), this);
			setClassTemplate(id);
			if (getPlayerClass().level() == 3)
			{
				sendPacket(SystemMessageId.CONGRATULATIONS_YOU_VE_COMPLETED_THE_THIRD_CLASS_TRANSFER_QUEST);
			}
			else
			{
				sendPacket(SystemMessageId.CONGRATULATIONS_YOU_VE_COMPLETED_A_CLASS_TRANSFER);
			}
			
			// Remove class permitted hennas.
			for (int slot = 1; slot < 4; slot++)
			{
				final Henna henna = getHenna(slot);
				if ((henna != null) && !henna.isAllowedClass(getPlayerClass()))
				{
					removeHenna(slot);
				}
			}
			
			// Update class icon in party and clan
			if (isInParty())
			{
				_party.broadcastPacket(new PartySmallWindowUpdate(this));
			}
			
			if (_clan != null)
			{
				_clan.broadcastToOnlineMembers(new PledgeShowMemberListUpdate(this));
			}
			
			// Add AutoGet skills and normal skills and/or learnByFS depending on configurations.
			rewardSkills();
			
			if (!isGM() && PlayerConfig.DECREASE_SKILL_LEVEL)
			{
				checkPlayerSkills();
			}
		}
		finally
		{
			_subclassLock = false;
			
			ThreadPool.schedule(() ->
			{
				getInventory().applyItemSkills();
				sendSkillList();
			}, 100);
		}
	}
	
	public boolean isChangingClass()
	{
		return _subclassLock;
	}
	
	/**
	 * Used for AltGameSkillLearn to set a custom skill learning class Id.
	 */
	private PlayerClass _learningClass = getPlayerClass();
	
	/**
	 * @return the custom skill learning class Id.
	 */
	public PlayerClass getLearningClass()
	{
		return _learningClass;
	}
	
	/**
	 * @param learningClass the custom skill learning class Id to set.
	 */
	public void setLearningClass(PlayerClass learningClass)
	{
		_learningClass = learningClass;
	}
	
	/**
	 * @return the Experience of the Player.
	 */
	public long getExp()
	{
		return getStat().getExp();
	}
	
	public void setActiveEnchantItemId(int objectId)
	{
		// If we don't have a Enchant Item, we are not enchanting.
		if (objectId == ID_NONE)
		{
			setActiveEnchantTimestamp(0);
			setEnchanting(false);
		}
		
		_activeEnchantItemId = objectId;
	}
	
	public int getActiveEnchantItemId()
	{
		return _activeEnchantItemId;
	}
	
	public long getActiveEnchantTimestamp()
	{
		return _activeEnchantTimestamp;
	}
	
	public void setActiveEnchantTimestamp(long value)
	{
		_activeEnchantTimestamp = value;
	}
	
	public void setEnchanting(boolean value)
	{
		_isEnchanting = value;
	}
	
	public boolean isEnchanting()
	{
		return _isEnchanting;
	}
	
	/**
	 * Set the fists weapon of the Player (used when no weapon is equiped).
	 * @param weaponItem The fists Weapon to set to the Player
	 */
	public void setFistsWeaponItem(Weapon weaponItem)
	{
		_fistsWeaponItem = weaponItem;
	}
	
	/**
	 * @return the fists weapon of the Player (used when no weapon is equipped).
	 */
	public Weapon getFistsWeaponItem()
	{
		return _fistsWeaponItem;
	}
	
	/**
	 * Returns the appropriate fist weapon template for a given class ID.<br>
	 * Fist weapons are used as default weapons when a player has no weapon equipped.
	 * @param classId the class identifier for this Player
	 * @return the Weapon template for the fist weapon corresponding to the class ID.
	 */
	public Weapon findFistsWeaponItem(int classId)
	{
		final Weapon weaponItem;
		
		if ((classId >= 0x00) && (classId <= 0x09))
		{
			weaponItem = (Weapon) ItemData.getInstance().getTemplate(246 /* Human Fighter Fist */);
		}
		else if ((classId >= 0x0a) && (classId <= 0x11))
		{
			weaponItem = (Weapon) ItemData.getInstance().getTemplate(251 /* Human Mystic Fist */);
		}
		else if ((classId >= 0x12) && (classId <= 0x18))
		{
			weaponItem = (Weapon) ItemData.getInstance().getTemplate(244 /* Elven Fighter Fist */);
		}
		else if ((classId >= 0x19) && (classId <= 0x1e))
		{
			weaponItem = (Weapon) ItemData.getInstance().getTemplate(249 /* Elven Mystic Fist */);
		}
		else if ((classId >= 0x1f) && (classId <= 0x25))
		{
			weaponItem = (Weapon) ItemData.getInstance().getTemplate(245 /* Dark Fighter Fist */);
		}
		else if ((classId >= 0x26) && (classId <= 0x2b))
		{
			weaponItem = (Weapon) ItemData.getInstance().getTemplate(250 /* Dark Mystic Fist */);
		}
		else if ((classId >= 0x2c) && (classId <= 0x30))
		{
			weaponItem = (Weapon) ItemData.getInstance().getTemplate(248 /* Orc Fighter Fist */);
		}
		else if ((classId >= 0x31) && (classId <= 0x34))
		{
			weaponItem = (Weapon) ItemData.getInstance().getTemplate(252 /* Orc Shaman Fist */);
		}
		else if ((classId >= 0x35) && (classId <= 0x39))
		{
			weaponItem = (Weapon) ItemData.getInstance().getTemplate(247 /* Dwarven Fighter Fist */);
		}
		else // Default fist item to avoid NPEs.
		{
			weaponItem = (Weapon) ItemData.getInstance().getTemplate(246 /* Human Fighter Fist */);
		}
		
		return weaponItem;
	}
	
	/**
	 * This method reward all AutoGet skills and Normal skills if Auto-Learn configuration is true.
	 */
	public void rewardSkills()
	{
		// Give all normal skills if activated Auto-Learn is activated, included AutoGet skills.
		if (PlayerConfig.AUTO_LEARN_SKILLS)
		{
			giveAvailableSkills(PlayerConfig.AUTO_LEARN_FS_SKILLS, true, PlayerConfig.AUTO_LEARN_SKILLS_WITHOUT_ITEMS);
		}
		else
		{
			giveAvailableAutoGetSkills();
		}
		
		if (PlayerConfig.DECREASE_SKILL_LEVEL && !isGM())
		{
			checkPlayerSkills();
		}
		
		if (FreeMountsConfig.ENABLE_FREE_STRIDER)
		{
			final Skill skill = SkillData.getInstance().getSkill(49991, 1);
			if (skill != null)
			{
				addSkill(skill, false);
			}
		}
		
		if (FreeMountsConfig.ENABLE_FREE_WYVERN)
		{
			final Skill skill = SkillData.getInstance().getSkill(49992, 1);
			if (skill != null)
			{
				addSkill(skill, false);
			}
		}
		
		checkItemRestriction();
		sendSkillList();
	}
	
	/**
	 * Re-give all skills which aren't saved to database, like Noble, Hero, Clan Skills.
	 */
	public void regiveTemporarySkills()
	{
		// Do not call this on enterworld or char load
		
		// Add noble skills if noble
		if (_noble)
		{
			setNoble(true);
		}
		
		// Add Hero skills if hero
		if (_hero)
		{
			setHero(true);
		}
		
		// Add clan skills
		if (_clan != null)
		{
			_clan.addSkillEffects(this);
			
			if ((_clan.getLevel() >= SiegeManager.getInstance().getSiegeClanMinLevel()) && isClanLeader())
			{
				SiegeManager.getInstance().addSiegeSkills(this);
			}
			
			if (_clan.getCastleId() > 0)
			{
				final Castle castle = CastleManager.getInstance().getCastleByOwner(_clan);
				if (castle != null)
				{
					castle.giveResidentialSkills(this);
				}
			}
		}
		
		// Reload passive skills from armors / jewels / weapons
		getInventory().reloadEquippedItems();
		
		// Add Death Penalty Buff Level
		restoreDeathPenaltyBuffLevel();
	}
	
	/**
	 * Give all available skills to the player.
	 * @param includeByFs if {@code true} forgotten scroll skills present in the skill tree will be added
	 * @param includeAutoGet if {@code true} auto-get skills present in the skill tree will be added
	 * @param includeRequiredItems if {@code true} skills that have required items will be added
	 * @return the amount of new skills earned
	 */
	public int giveAvailableSkills(boolean includeByFs, boolean includeAutoGet, boolean includeRequiredItems)
	{
		int skillCounter = 0;
		
		// Get available skills.
		final Collection<Skill> skills = SkillTreeData.getInstance().getAllAvailableSkills(this, getPlayerClass(), includeByFs, includeAutoGet, includeRequiredItems);
		final List<Skill> skillsForStore = new ArrayList<>();
		for (Skill skill : skills)
		{
			if (getKnownSkill(skill.getId()) == skill)
			{
				continue;
			}
			
			if (getSkillLevel(skill.getId()) == 0)
			{
				skillCounter++;
			}
			
			// Fix when learning toggle skills.
			if (skill.isToggle() && isAffectedBySkill(skill.getId()))
			{
				stopSkillEffects(SkillFinishType.REMOVED, skill.getId());
			}
			
			addSkill(skill, false);
			skillsForStore.add(skill);
			
			if (PlayerConfig.AUTO_LEARN_SKILLS)
			{
				updateShortcuts(skill.getId(), skill.getLevel());
			}
		}
		
		storeSkills(skillsForStore, -1);
		
		if (PlayerConfig.AUTO_LEARN_SKILLS && (skillCounter > 0))
		{
			sendPacket(new ShortcutInit(this));
			sendMessage("You have learned " + skillCounter + " new skills.");
		}
		
		return skillCounter;
	}
	
	/**
	 * Give all available auto-get skills to the player.
	 */
	public void giveAvailableAutoGetSkills()
	{
		// Get available skills.
		final List<SkillLearn> autoGetSkills = SkillTreeData.getInstance().getAvailableAutoGetSkills(this);
		final SkillData st = SkillData.getInstance();
		Skill skill;
		for (SkillLearn s : autoGetSkills)
		{
			skill = st.getSkill(s.getSkillId(), s.getSkillLevel());
			if (skill != null)
			{
				addSkill(skill, true);
			}
			else
			{
				LOGGER.warning("Skipping null auto-get skill for " + this);
			}
		}
	}
	
	/**
	 * Set the Experience value of the Player.
	 * @param exp
	 */
	public void setExp(long exp)
	{
		getStat().setExp(Math.max(0, exp));
	}
	
	/**
	 * @return the Race object of the Player.
	 */
	@Override
	public Race getRace()
	{
		if (!isSubClassActive())
		{
			return getTemplate().getRace();
		}
		
		return PlayerTemplateData.getInstance().getTemplate(_baseClass).getRace();
	}
	
	public Radar getRadar()
	{
		return _radar;
	}
	
	/* Return true if Hellbound minimap allowed */
	public boolean isMinimapAllowed()
	{
		return _minimapAllowed;
	}
	
	/* Enable or disable minimap on Hellbound */
	public void setMinimapAllowed(boolean value)
	{
		_minimapAllowed = value;
	}
	
	/**
	 * @return the SP amount of the Player.
	 */
	public long getSp()
	{
		return getStat().getSp();
	}
	
	/**
	 * Set the SP amount of the Player.
	 * @param sp
	 */
	public void setSp(long sp)
	{
		super.getStat().setSp(Math.max(0, sp));
	}
	
	/**
	 * @param castleId
	 * @return true if this Player is a clan leader in ownership of the passed castle
	 */
	public boolean isCastleLord(int castleId)
	{
		// player has clan and is the clan leader, check the castle info
		if ((_clan != null) && (_clan.getLeader().getPlayer() == this))
		{
			// if the clan has a castle and it is actually the queried castle, return true
			final Castle castle = CastleManager.getInstance().getCastleByOwner(_clan);
			if ((castle != null) && (castle == CastleManager.getInstance().getCastleById(castleId)))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * @return the Clan Identifier of the Player.
	 */
	@Override
	public int getClanId()
	{
		return _clanId;
	}
	
	/**
	 * @return the Clan Crest Identifier of the Player or 0.
	 */
	public int getClanCrestId()
	{
		return _clan != null ? _clan.getCrestId() : 0;
	}
	
	/**
	 * @return The Clan CrestLarge Identifier or 0
	 */
	public int getClanCrestLargeId()
	{
		if ((_clan != null) && ((_clan.getCastleId() != 0) || (_clan.getHideoutId() != 0)))
		{
			return _clan.getCrestLargeId();
		}
		
		return 0;
	}
	
	public long getClanJoinExpiryTime()
	{
		return _clanJoinExpiryTime;
	}
	
	public void setClanJoinExpiryTime(long time)
	{
		_clanJoinExpiryTime = time;
	}
	
	public long getClanCreateExpiryTime()
	{
		return _clanCreateExpiryTime;
	}
	
	public void setClanCreateExpiryTime(long time)
	{
		_clanCreateExpiryTime = time;
	}
	
	public void setOnlineTime(long time)
	{
		_onlineTime = time;
		_onlineBeginTime = System.currentTimeMillis();
	}
	
	public int getOnlineTimeMillis()
	{
		return (int) (System.currentTimeMillis() - _onlineBeginTime);
	}
	
	/**
	 * Return the PcInventory Inventory of the Player contained in _inventory.
	 */
	@Override
	public PlayerInventory getInventory()
	{
		return _inventory;
	}
	
	/**
	 * Delete a Shortcut of the Player _shortcuts.
	 * @param objectId
	 */
	public void removeItemFromShortcut(int objectId)
	{
		_shortcuts.deleteShortcutByObjectId(objectId);
	}
	
	/**
	 * @return True if the Player is sitting.
	 */
	public boolean isSitting()
	{
		return _waitTypeSitting;
	}
	
	/**
	 * Set _waitTypeSitting to given value.
	 * @param value
	 */
	public void setSitting(boolean value)
	{
		_waitTypeSitting = value;
	}
	
	/**
	 * @return {@code true} while a sit-down / stand-up animation is still in progress (blocks a new sit/stand).
	 */
	public boolean isSittingProgress()
	{
		return _sittingInProgress;
	}

	/**
	 * Set _sittingInProgress to given value.
	 * @param value
	 */
	public void setSittingProgress(boolean value)
	{
		_sittingInProgress = value;
	}
	
	/**
	 * Sit down the Player, set the AI Intention to REST and send a Server->Client ChangeWaitType packet (broadcast)
	 */
	public void sitDown()
	{
		sitDown(true);
	}
	
	public void sitDown(boolean checkCast)
	{
		if (_sittingInProgress)
		{
			return;
		}
		
		if (checkCast && isCastingNow())
		{
			sendMessage("Cannot sit while casting");
			return;
		}
		
		if (_waitTypeSitting || isAttackDisabled() || isOutOfControl() || isImmobilized())
		{
			return;
		}
		
		breakAttack();
		setSitting(true);
		setSittingProgress(true);
		getAI().setIntention(Intention.REST);
		broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_SITTING));
		
		// Schedule a sit down task to wait for the animation to finish.
		ThreadPool.schedule(new SitDownTask(this), 2500);
	}
	
	/**
	 * Stand up the Player, set the AI Intention to IDLE and send a Server->Client ChangeWaitType packet (broadcast)
	 */
	public void standUp()
	{
		if (_sittingInProgress)
		{
			return;
		}
		
		if (_waitTypeSitting && !isInStoreMode() && !isAlikeDead())
		{
			setSittingProgress(true);
			if (getEffectList().isAffected(EffectFlag.RELAXING))
			{
				stopEffects(EffectType.RELAXING);
			}
			
			broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_STANDING));
			
			// Schedule a stand up task to wait for the animation to finish.
			ThreadPool.schedule(new StandUpTask(this), 2500);
		}
	}
	
	/**
	 * @return the PlayerWarehouse object of the Player.
	 */
	public PlayerWarehouse getWarehouse()
	{
		return _warehouse;
	}
	
	/**
	 * @return the PlayerFreight object of the Player.
	 */
	public PlayerFreight getFreight()
	{
		return _freight;
	}
	
	/**
	 * @return true if refund list is not empty
	 */
	public boolean hasRefund()
	{
		return (_refund != null) && (_refund.getSize() > 0) && GeneralConfig.ALLOW_REFUND;
	}
	
	/**
	 * @return refund object or create new if not exist
	 */
	public PlayerRefund getRefund()
	{
		if (_refund == null)
		{
			_refund = new PlayerRefund(this);
		}
		
		return _refund;
	}
	
	/**
	 * Clear refund
	 */
	public void clearRefund()
	{
		if (_refund != null)
		{
			_refund.deleteMe();
		}
		
		_refund = null;
	}
	
	/**
	 * @return the Adena amount of the Player.
	 */
	public int getAdena()
	{
		return _inventory.getAdena();
	}
	
	/**
	 * @return the Ancient Adena amount of the Player.
	 */
	public int getAncientAdena()
	{
		return _inventory.getAncientAdena();
	}
	
	/**
	 * Add adena to Inventory of the Player and send a Server->Client InventoryUpdate packet to the Player.
	 * @param process : ItemProcessType identifier of process triggering this action
	 * @param count : int Quantity of adena to be added
	 * @param reference : WorldObject Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 */
	public void addAdena(ItemProcessType process, int count, WorldObject reference, boolean sendMessage)
	{
		final int currentAdena = _inventory.getAdena();
		final int limitRemaining = PlayerConfig.MAX_ADENA - currentAdena;
		final int amountToAdd = count;
		
		if (count > limitRemaining)
		{
			count = limitRemaining;
		}
		
		if (sendMessage)
		{
			if ((count == 0) && (amountToAdd > 0))
			{
				// Failed adding adena.
				final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_EARN_S1_ADENA);
				sm.addInt(amountToAdd);
				sendPacket(sm);
			}
			else
			{
				// Inform player how much adena they received.
				final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S1_ADENA);
				sm.addInt(count);
				sendPacket(sm);
			}
		}
		
		if (count > 0)
		{
			_inventory.addAdena(process, count, this, reference);
			
			// Send update packet.
			if (count == getAdena())
			{
				sendItemList(false);
			}
			else
			{
				final InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(_inventory.getAdenaInstance());
				sendInventoryUpdate(iu);
			}
		}
		
		// If the player has reached the adena limit, inform them.
		if ((_inventory.getAdena() == PlayerConfig.MAX_ADENA) && sendMessage)
		{
			sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_YOUR_OUT_OF_POCKET_ADENA_LIMIT);
		}
	}
	
	/**
	 * Reduce adena in Inventory of the Player and send a Server->Client InventoryUpdate packet to the Player.
	 * @param process : ItemProcessType identifier of process triggering this action
	 * @param count : long Quantity of adena to be reduced
	 * @param reference : WorldObject Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successful
	 */
	public boolean reduceAdena(ItemProcessType process, int count, WorldObject reference, boolean sendMessage)
	{
		if (count > _inventory.getAdena())
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
			}
			
			return false;
		}
		
		if (count > 0)
		{
			final Item adenaItem = _inventory.getAdenaInstance();
			if (!_inventory.reduceAdena(process, count, this, reference))
			{
				return false;
			}
			
			// Send update packet
			final InventoryUpdate iu = new InventoryUpdate();
			iu.addItem(adenaItem);
			sendInventoryUpdate(iu);
			
			if (sendMessage)
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.S1_ADENA_DISAPPEARED);
				sm.addInt(count);
				sendPacket(sm);
			}
		}
		
		return true;
	}
	
	/**
	 * Add ancient adena to Inventory of the Player and send a Server->Client InventoryUpdate packet to the Player.
	 * @param process : ItemProcessType identifier of process triggering this action
	 * @param count : int Quantity of ancient adena to be added
	 * @param reference : WorldObject Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 */
	public void addAncientAdena(ItemProcessType process, int count, WorldObject reference, boolean sendMessage)
	{
		if (sendMessage)
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S2_S1_S);
			sm.addItemName(Inventory.ANCIENT_ADENA_ID);
			sm.addInt(count);
			sendPacket(sm);
		}
		
		if (count <= 0)
		{
			return;
		}
		
		_inventory.addAncientAdena(process, count, this, reference);
		
		final InventoryUpdate iu = new InventoryUpdate();
		iu.addItem(_inventory.getAncientAdenaInstance());
		sendInventoryUpdate(iu);
	}
	
	/**
	 * Reduce ancient adena in Inventory of the Player and send a Server->Client InventoryUpdate packet to the Player.
	 * @param process : ItemProcessType identifier of process triggering this action
	 * @param count : long Quantity of ancient adena to be reduced
	 * @param reference : WorldObject Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successful
	 */
	public boolean reduceAncientAdena(ItemProcessType process, int count, WorldObject reference, boolean sendMessage)
	{
		if (count > _inventory.getAncientAdena())
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
			}
			
			return false;
		}
		
		if (count > 0)
		{
			final Item ancientAdenaItem = _inventory.getAncientAdenaInstance();
			if (!_inventory.reduceAncientAdena(process, count, this, reference))
			{
				return false;
			}
			
			final InventoryUpdate iu = new InventoryUpdate();
			iu.addItem(ancientAdenaItem);
			sendInventoryUpdate(iu);
			
			if (sendMessage)
			{
				final SystemMessage sm;
				if (count > 1)
				{
					sm = new SystemMessage(SystemMessageId.S2_S1_HAS_DISAPPEARED);
					sm.addItemName(Inventory.ANCIENT_ADENA_ID);
					sm.addInt(count);
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S1_HAS_DISAPPEARED);
					sm.addItemName(Inventory.ANCIENT_ADENA_ID);
				}
				
				sendPacket(sm);
			}
		}
		
		return true;
	}
	
	/**
	 * Adds item to inventory and send a Server->Client InventoryUpdate packet to the Player.
	 * @param process : ItemProcessType identifier of process triggering this action
	 * @param item : Item to be added
	 * @param reference : WorldObject Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 */
	public void addItem(ItemProcessType process, Item item, WorldObject reference, boolean sendMessage)
	{
		if (item.getCount() > 0)
		{
			// Sends message to client if requested
			if (sendMessage)
			{
				final SystemMessage sm;
				if (item.getCount() > 1)
				{
					sm = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_S2_S1);
					sm.addItemName(item);
					sm.addInt(item.getCount());
				}
				else if (item.isEnchanted())
				{
					sm = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_A_S1_S2);
					sm.addInt(item.getEnchantLevel());
					sm.addItemName(item);
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_S1);
					sm.addItemName(item);
				}
				
				sendPacket(sm);
			}
			
			// Add the item to inventory
			final Item newitem = _inventory.addItem(process, item, this, reference);
			
			// Send inventory update packet
			final InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addItem(newitem);
			sendInventoryUpdate(playerIU);
			
			// Update current load as well
			final StatusUpdate su = new StatusUpdate(this);
			su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
			sendPacket(su);
			
			// If over capacity, drop the item
			if (!isGM() && !_inventory.validateCapacity(0, item.isQuestItem()) && newitem.isDropable() && (!newitem.isStackable() || (newitem.getLastChange() != Item.MODIFIED)))
			{
				dropItem(ItemProcessType.DROP, newitem, null, true, true);
			}
			else if (CursedWeaponsManager.getInstance().isCursed(newitem.getId()))
			{
				CursedWeaponsManager.getInstance().activate(this, newitem);
			}
		}
	}
	
	/**
	 * Adds item to Inventory and send a Server->Client InventoryUpdate packet to the Player.
	 * @param process : ItemProcessType identifier of process triggering this action
	 * @param itemId : int Item Identifier of the item to be added
	 * @param count : long Quantity of items to be added
	 * @param reference : WorldObject Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return
	 */
	public Item addItem(ItemProcessType process, int itemId, int count, WorldObject reference, boolean sendMessage)
	{
		return addItem(process, itemId, count, -1, reference, sendMessage);
	}
	
	/**
	 * Adds item to Inventory and send a Server->Client InventoryUpdate packet to the Player.
	 * @param process : ItemProcessType identifier of process triggering this action
	 * @param itemId : int Item Identifier of the item to be added
	 * @param count : int Quantity of items to be added
	 * @param enchantLevel : int EnchantLevel of the item to be added
	 * @param reference : WorldObject Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return
	 */
	public Item addItem(ItemProcessType process, int itemId, int count, int enchantLevel, WorldObject reference, boolean sendMessage)
	{
		if (count > 0)
		{
			final ItemTemplate item = ItemData.getInstance().getTemplate(itemId);
			if (item == null)
			{
				LOGGER.severe("Item doesn't exist so cannot be added. Item ID: " + itemId);
				return null;
			}
			
			// Sends message to client if requested
			if (sendMessage && ((!isCastingNow() && item.hasExImmediateEffect()) || !item.hasExImmediateEffect()))
			{
				if (count > 1)
				{
					if ((process == ItemProcessType.SWEEP) || (process == ItemProcessType.QUEST))
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S2_S1_S);
						sm.addItemName(itemId);
						sm.addInt(count);
						sendPacket(sm);
					}
					else
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_S2_S1);
						sm.addItemName(itemId);
						sm.addInt(count);
						sendPacket(sm);
					}
				}
				else if ((process == ItemProcessType.SWEEP) || (process == ItemProcessType.QUEST))
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S1);
					sm.addItemName(itemId);
					sendPacket(sm);
				}
				else
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_S1);
					sm.addItemName(itemId);
					sendPacket(sm);
				}
			}
			
			// Auto-use herbs.
			if (item.hasExImmediateEffect() && item.isEtcItem())
			{
				for (SkillHolder skillHolder : item.getSkills())
				{
					doSimultaneousCast(skillHolder.getSkill());
				}
				
				broadcastInfo();
			}
			else
			{
				// Add the item to inventory
				final Item createdItem = _inventory.addItem(process, itemId, count, this, reference);
				if (enchantLevel > -1)
				{
					createdItem.setEnchantLevel(enchantLevel);
				}
				
				// If over capacity, drop the item
				if (!isGM() && !_inventory.validateCapacity(0, item.isQuestItem()) && createdItem.isDropable() && (!createdItem.isStackable() || (createdItem.getLastChange() != Item.MODIFIED)))
				{
					dropItem(ItemProcessType.DROP, createdItem, null, true);
				}
				else if (CursedWeaponsManager.getInstance().isCursed(createdItem.getId()))
				{
					CursedWeaponsManager.getInstance().activate(this, createdItem);
				}
				
				return createdItem;
			}
		}
		
		return null;
	}
	
	/**
	 * @param process the ItemProcessType identifier of process triggering this action
	 * @param item the item holder
	 * @param reference the reference object
	 * @param sendMessage if {@code true} a system message will be sent
	 */
	public void addItem(ItemProcessType process, ItemHolder item, WorldObject reference, boolean sendMessage)
	{
		addItem(process, item.getId(), item.getCount(), reference, sendMessage);
	}
	
	/**
	 * Destroy item from inventory and send a Server->Client InventoryUpdate packet to the Player.
	 * @param process : ItemProcessType identifier of process triggering this action
	 * @param item : Item to be destroyed
	 * @param reference : WorldObject Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successful
	 */
	public boolean destroyItem(ItemProcessType process, Item item, WorldObject reference, boolean sendMessage)
	{
		return destroyItem(process, item, item.getCount(), reference, sendMessage);
	}
	
	/**
	 * Destroy item from inventory and send a Server->Client InventoryUpdate packet to the Player.
	 * @param process : ItemProcessType identifier of process triggering this action
	 * @param item : Item to be destroyed
	 * @param count
	 * @param reference : WorldObject Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successful
	 */
	public boolean destroyItem(ItemProcessType process, Item item, int count, WorldObject reference, boolean sendMessage)
	{
		final Item destoyedItem = _inventory.destroyItem(process, item, count, this, reference);
		if (destoyedItem == null)
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
			}
			
			return false;
		}
		
		// Send inventory update packet
		final InventoryUpdate playerIU = new InventoryUpdate();
		playerIU.addItem(destoyedItem);
		sendInventoryUpdate(playerIU);
		
		// Update current load as well
		final StatusUpdate su = new StatusUpdate(this);
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);
		
		// Sends message to client if requested
		if (sendMessage)
		{
			final SystemMessage sm;
			if (count > 1)
			{
				sm = new SystemMessage(SystemMessageId.S2_S1_HAS_DISAPPEARED);
				sm.addItemName(destoyedItem);
				sm.addInt(count);
			}
			else
			{
				sm = new SystemMessage(SystemMessageId.S1_HAS_DISAPPEARED);
				sm.addItemName(destoyedItem);
			}
			
			sendPacket(sm);
		}
		
		return true;
	}
	
	/**
	 * Destroys item from inventory and send a Server->Client InventoryUpdate packet to the Player.
	 * @param process : ItemProcessType identifier of process triggering this action (if null item will not be logged)
	 * @param objectId : int Item Instance identifier of the item to be destroyed
	 * @param count : int Quantity of items to be destroyed
	 * @param reference : WorldObject Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successful
	 */
	@Override
	public boolean destroyItem(ItemProcessType process, int objectId, int count, WorldObject reference, boolean sendMessage)
	{
		final Item item = _inventory.getItemByObjectId(objectId);
		if ((item == null) || (item.getCount() < count))
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
			}
			
			return false;
		}
		
		return destroyItem(process, item, count, reference, sendMessage);
	}
	
	/**
	 * Destroy item from inventory by using its <b>itemId</b> and send a Server->Client InventoryUpdate packet to the Player.
	 * @param process : ItemProcessType identifier of process triggering this action
	 * @param itemId : int Item identifier of the item to be destroyed
	 * @param count : int Quantity of items to be destroyed
	 * @param reference : WorldObject Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successful
	 */
	@Override
	public boolean destroyItemByItemId(ItemProcessType process, int itemId, int count, WorldObject reference, boolean sendMessage)
	{
		if (itemId == Inventory.ADENA_ID)
		{
			return reduceAdena(process, count, reference, sendMessage);
		}
		
		final Item item = _inventory.getItemByItemId(itemId);
		if (item == null)
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
			}
			
			return false;
		}
		
		final int itemCount = item.isStackable() ? item.getCount() : _inventory.getInventoryItemCount(itemId, -1);
		final int removeCount = count < 0 ? itemCount : count;
		if ((removeCount <= 0) || (itemCount < removeCount) || (_inventory.destroyItemByItemId(process, itemId, removeCount, this, reference) == null))
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
			}
			
			return false;
		}
		
		// Send inventory update packet
		final InventoryUpdate playerIU = new InventoryUpdate();
		playerIU.addItem(item);
		sendInventoryUpdate(playerIU);
		
		// Update current load as well
		final StatusUpdate su = new StatusUpdate(this);
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);
		
		// Sends message to client if requested
		if (sendMessage)
		{
			final SystemMessage sm;
			if (removeCount > 1)
			{
				sm = new SystemMessage(SystemMessageId.S2_S1_HAS_DISAPPEARED);
				sm.addItemName(itemId);
				sm.addInt(removeCount);
			}
			else
			{
				sm = new SystemMessage(SystemMessageId.S1_HAS_DISAPPEARED);
				sm.addItemName(itemId);
			}
			
			sendPacket(sm);
		}
		
		return true;
	}
	
	/**
	 * Transfers item to another ItemContainer and send a Server->Client InventoryUpdate packet to the Player.
	 * @param process : ItemProcessType identifier of process triggering this action
	 * @param objectId : int Item Identifier of the item to be transfered
	 * @param count : long Quantity of items to be transfered
	 * @param target
	 * @param reference : WorldObject Object referencing current action like NPC selling item or previous item in transformation
	 * @return Item corresponding to the new item or the updated item in inventory
	 */
	public Item transferItem(ItemProcessType process, int objectId, int count, Inventory target, WorldObject reference)
	{
		final Item oldItem = checkItemManipulation(objectId, count, "transfer");
		if (oldItem == null)
		{
			return null;
		}
		
		final Item newItem = _inventory.transferItem(process, objectId, count, target, this, reference);
		if (newItem == null)
		{
			return null;
		}
		
		// Send inventory update packet
		final InventoryUpdate playerIU = new InventoryUpdate();
		if ((oldItem.getCount() > 0) && (oldItem != newItem))
		{
			playerIU.addModifiedItem(oldItem);
		}
		else
		{
			playerIU.addRemovedItem(oldItem);
		}
		
		sendInventoryUpdate(playerIU);
		
		// Update current load as well
		StatusUpdate playerSU = new StatusUpdate(this);
		playerSU.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(playerSU);
		
		// Send target update packet
		if (target instanceof PlayerInventory)
		{
			final Player targetPlayer = ((PlayerInventory) target).getOwner();
			final InventoryUpdate targetIU = new InventoryUpdate();
			if (newItem.getCount() > count)
			{
				targetIU.addModifiedItem(newItem);
			}
			else
			{
				targetIU.addNewItem(newItem);
			}
			
			targetPlayer.sendInventoryUpdate(targetIU);
			
			// Update current load as well
			playerSU = new StatusUpdate(targetPlayer);
			playerSU.addAttribute(StatusUpdate.CUR_LOAD, targetPlayer.getCurrentLoad());
			targetPlayer.sendPacket(playerSU);
		}
		else if (target instanceof PetInventory)
		{
			final PetInventoryUpdate petIU = new PetInventoryUpdate();
			if (newItem.getCount() > count)
			{
				petIU.addModifiedItem(newItem);
			}
			else
			{
				petIU.addNewItem(newItem);
			}
			
			((PetInventory) target).getOwner().sendPacket(petIU);
		}
		
		return newItem;
	}
	
	/**
	 * Use instead of calling {@link #addItem(ItemProcessType, Item, WorldObject, boolean)} and {@link #destroyItemByItemId(ItemProcessType, int, int, WorldObject, boolean)}<br>
	 * This method validates slots and weight limit, for stackable and non-stackable items.
	 * @param process a ItemProcessType representing the process that is exchanging this items
	 * @param reference the (probably NPC) reference, could be null
	 * @param coinId the item Id of the item given on the exchange
	 * @param cost the amount of items given on the exchange
	 * @param rewardId the item received on the exchange
	 * @param count the amount of items received on the exchange
	 * @param sendMessage if {@code true} it will send messages to the acting player
	 * @return {@code true} if the player successfully exchanged the items, {@code false} otherwise
	 */
	public boolean exchangeItemsById(ItemProcessType process, WorldObject reference, int coinId, int cost, int rewardId, int count, boolean sendMessage)
	{
		if (!_inventory.validateCapacityByItemId(rewardId, count))
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.YOUR_INVENTORY_IS_FULL);
			}
			
			return false;
		}
		
		if (!_inventory.validateWeightByItemId(rewardId, count))
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_THE_WEIGHT_LIMIT);
			}
			
			return false;
		}
		
		if (destroyItemByItemId(process, coinId, cost, reference, sendMessage))
		{
			addItem(process, rewardId, count, reference, sendMessage);
			return true;
		}
		
		return false;
	}
	
	/**
	 * Drop item from inventory and send a Server->Client InventoryUpdate packet to the Player.
	 * @param process ItemProcessType identifier of process triggering this action
	 * @param item Item to be dropped
	 * @param reference WorldObject Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage boolean Specifies whether to send message to Client about this action
	 * @param protectItem whether or not dropped item must be protected temporary against other players
	 * @return boolean informing if the action was successful
	 */
	public boolean dropItem(ItemProcessType process, Item item, WorldObject reference, boolean sendMessage, boolean protectItem)
	{
		final Item droppedItem = _inventory.dropItem(process, item, this, reference);
		if (droppedItem == null)
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
			}
			
			return false;
		}
		
		droppedItem.dropMe(this, (getX() + Rnd.get(50)) - 25, (getY() + Rnd.get(50)) - 25, getZ() + 20);
		if ((GeneralConfig.AUTODESTROY_ITEM_AFTER > 0) && GeneralConfig.DESTROY_DROPPED_PLAYER_ITEM && !GeneralConfig.LIST_PROTECTED_ITEMS.contains(droppedItem.getId()) && ((droppedItem.isEquipable() && GeneralConfig.DESTROY_EQUIPABLE_PLAYER_ITEM) || !droppedItem.isEquipable()))
		{
			ItemsAutoDestroyTaskManager.getInstance().addItem(droppedItem);
		}
		
		// protection against auto destroy dropped item
		if (GeneralConfig.DESTROY_DROPPED_PLAYER_ITEM)
		{
			droppedItem.setProtected(droppedItem.isEquipable() && (!droppedItem.isEquipable() || !GeneralConfig.DESTROY_EQUIPABLE_PLAYER_ITEM));
		}
		else
		{
			droppedItem.setProtected(true);
		}
		
		// retail drop protection
		if (protectItem)
		{
			droppedItem.getDropProtection().protect(this);
		}
		
		// Send inventory update packet
		final InventoryUpdate playerIU = new InventoryUpdate();
		playerIU.addItem(droppedItem);
		sendInventoryUpdate(playerIU);
		
		// Update current load as well
		final StatusUpdate su = new StatusUpdate(this);
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);
		
		// Sends message to client if requested
		if (sendMessage)
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_DROPPED_S1);
			sm.addItemName(droppedItem);
			sendPacket(sm);
		}
		
		return true;
	}
	
	public boolean dropItem(ItemProcessType process, Item item, WorldObject reference, boolean sendMessage)
	{
		return dropItem(process, item, reference, sendMessage, false);
	}
	
	/**
	 * Drop item from inventory by using its <b>objectID</b> and send a Server->Client InventoryUpdate packet to the Player.
	 * @param process : ItemProcessType identifier of process triggering this action
	 * @param objectId : int Item Instance identifier of the item to be dropped
	 * @param count : long Quantity of items to be dropped
	 * @param x : int coordinate for drop X
	 * @param y : int coordinate for drop Y
	 * @param z : int coordinate for drop Z
	 * @param reference : WorldObject Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @param protectItem
	 * @return Item corresponding to the new item or the updated item in inventory
	 */
	public Item dropItem(ItemProcessType process, int objectId, int count, int x, int y, int z, WorldObject reference, boolean sendMessage, boolean protectItem)
	{
		final Item invitem = _inventory.getItemByObjectId(objectId);
		final Item item = _inventory.dropItem(process, objectId, count, this, reference);
		if (item == null)
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
			}
			
			return null;
		}
		
		item.dropMe(this, x, y, z);
		if ((GeneralConfig.AUTODESTROY_ITEM_AFTER > 0) && GeneralConfig.DESTROY_DROPPED_PLAYER_ITEM && !GeneralConfig.LIST_PROTECTED_ITEMS.contains(item.getId()) && ((item.isEquipable() && GeneralConfig.DESTROY_EQUIPABLE_PLAYER_ITEM) || !item.isEquipable()))
		{
			ItemsAutoDestroyTaskManager.getInstance().addItem(item);
		}
		
		if (GeneralConfig.DESTROY_DROPPED_PLAYER_ITEM)
		{
			item.setProtected(!(!item.isEquipable() || (item.isEquipable() && GeneralConfig.DESTROY_EQUIPABLE_PLAYER_ITEM)));
		}
		else
		{
			item.setProtected(true);
		}
		
		// retail drop protection
		if (protectItem)
		{
			item.getDropProtection().protect(this);
		}
		
		// Send inventory update packet
		final InventoryUpdate playerIU = new InventoryUpdate();
		playerIU.addItem(invitem);
		sendInventoryUpdate(playerIU);
		
		// Update current load as well
		final StatusUpdate su = new StatusUpdate(this);
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);
		
		// Sends message to client if requested
		if (sendMessage)
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_DROPPED_S1);
			sm.addItemName(item);
			sendPacket(sm);
		}
		
		return item;
	}
	
	public Item checkItemManipulation(int objectId, int count, String action)
	{
		// TODO: if we remove objects that are not visible from the World, we'll have to remove this check
		if (World.getInstance().findObject(objectId) == null)
		{
			LOGGER.finest(getObjectId() + ": player tried to " + action + " item not available in World");
			return null;
		}
		
		final Item item = _inventory.getItemByObjectId(objectId);
		if ((item == null) || (item.getOwnerId() != getObjectId()))
		{
			LOGGER.finest(getObjectId() + ": player tried to " + action + " item he is not owner of");
			return null;
		}
		
		if ((count < 0) || ((count > 1) && !item.isStackable()))
		{
			LOGGER.finest(getObjectId() + ": player tried to " + action + " item with invalid count: " + count);
			return null;
		}
		
		if (count > item.getCount())
		{
			LOGGER.finest(getObjectId() + ": player tried to " + action + " more items than he owns");
			return null;
		}
		
		// Pet is summoned and not the item that summoned the pet AND not the buggle from strider you're mounting
		if ((hasSummon() && (_summon.getControlObjectId() == objectId)) || (_mountObjectID == objectId))
		{
			return null;
		}
		
		if (_activeEnchantItemId == objectId)
		{
			return null;
		}
		
		// We cannot put a Weapon with Augmention in WH while casting (Possible Exploit)
		if (item.isAugmented() && (isCastingNow() || isCastingSimultaneouslyNow()))
		{
			return null;
		}
		
		return item;
	}
	
	public boolean isSpawnProtected()
	{
		return (_spawnProtectEndTime != 0) && (_spawnProtectEndTime > System.currentTimeMillis());
	}
	
	public boolean isTeleportProtected()
	{
		return (_teleportProtectEndTime != 0) && (_teleportProtectEndTime > System.currentTimeMillis());
	}
	
	public void setSpawnProtection(boolean protect)
	{
		_spawnProtectEndTime = protect ? System.currentTimeMillis() + (PlayerConfig.PLAYER_SPAWN_PROTECTION * 1000) : 0;
	}
	
	public void setTeleportProtection(boolean protect)
	{
		_teleportProtectEndTime = protect ? System.currentTimeMillis() + (PlayerConfig.PLAYER_TELEPORT_PROTECTION * 1000) : 0;
	}
	
	/**
	 * Set protection from aggro mobs when getting up from fake death, according settings.
	 * @param protect
	 */
	public void setRecentFakeDeath(boolean protect)
	{
		_recentFakeDeathEndTime = protect ? GameTimeTaskManager.getInstance().getGameTicks() + (PlayerConfig.PLAYER_FAKEDEATH_UP_PROTECTION * GameTimeTaskManager.TICKS_PER_SECOND) : 0;
	}
	
	public boolean isRecentFakeDeath()
	{
		return _recentFakeDeathEndTime > GameTimeTaskManager.getInstance().getGameTicks();
	}
	
	public boolean isFakeDeath()
	{
		return _isFakeDeath;
	}
	
	public void setFakeDeath(boolean value)
	{
		_isFakeDeath = value;
	}
	
	@Override
	public boolean isAlikeDead()
	{
		return super.isAlikeDead() || _isFakeDeath;
	}
	
	/**
	 * @return the client owner of this char.
	 */
	public GameClient getClient()
	{
		return _client;
	}
	
	public void setClient(GameClient client)
	{
		_client = client;
		if ((_client != null) && (_client.getIp() != null))
		{
			_ip = _client.getIp();
		}
	}
	
	public String getIPAddress()
	{
		return _ip;
	}
	
	public Location getCurrentSkillWorldPosition()
	{
		return _currentSkillWorldPosition;
	}
	
	public void setCurrentSkillWorldPosition(Location worldPosition)
	{
		_currentSkillWorldPosition = worldPosition;
	}
	
	@Override
	public void enableSkill(Skill skill)
	{
		super.enableSkill(skill);
		removeTimeStamp(skill);
	}
	
	@Override
	public boolean checkDoCastConditions(Skill skill)
	{
		if (!super.checkDoCastConditions(skill) || _observerMode)
		{
			return false;
		}
		
		if (_inOlympiadMode && skill.isBlockedInOlympiad())
		{
			sendPacket(SystemMessageId.YOU_CANNOT_USE_THAT_SKILL_IN_A_GRAND_OLYMPIAD_GAMES_MATCH);
			return false;
		}
		
		// Check if the spell using charges.
		if (_charges.get() < skill.getChargeConsumeCount())
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS);
			sm.addSkillName(skill);
			sendPacket(sm);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Returns true if cp update should be done, false if not.
	 * @return boolean
	 */
	private boolean needCpUpdate()
	{
		final double currentCp = getCurrentCp();
		if ((currentCp <= 1.0) || (getMaxCp() < MAX_HP_BAR_PX))
		{
			return true;
		}
		
		if ((currentCp <= _cpUpdateDecCheck) || (currentCp >= _cpUpdateIncCheck))
		{
			if (currentCp == getMaxCp())
			{
				_cpUpdateIncCheck = currentCp + 1;
				_cpUpdateDecCheck = currentCp - _cpUpdateInterval;
			}
			else
			{
				final double doubleMulti = currentCp / _cpUpdateInterval;
				int intMulti = (int) doubleMulti;
				_cpUpdateDecCheck = _cpUpdateInterval * (doubleMulti < intMulti ? intMulti - 1 : intMulti);
				_cpUpdateIncCheck = _cpUpdateDecCheck + _cpUpdateInterval;
			}
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Returns true if mp update should be done, false if not.
	 * @return boolean
	 */
	private boolean needMpUpdate()
	{
		final double currentMp = getCurrentMp();
		if ((currentMp <= 1.0) || (getMaxMp() < MAX_HP_BAR_PX))
		{
			return true;
		}
		
		if ((currentMp <= _mpUpdateDecCheck) || (currentMp >= _mpUpdateIncCheck))
		{
			if (currentMp == getMaxMp())
			{
				_mpUpdateIncCheck = currentMp + 1;
				_mpUpdateDecCheck = currentMp - _mpUpdateInterval;
			}
			else
			{
				final double doubleMulti = currentMp / _mpUpdateInterval;
				int intMulti = (int) doubleMulti;
				_mpUpdateDecCheck = _mpUpdateInterval * (doubleMulti < intMulti ? intMulti - 1 : intMulti);
				_mpUpdateIncCheck = _mpUpdateDecCheck + _mpUpdateInterval;
			}
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Send packet StatusUpdate with current HP,MP and CP to the Player and only current HP, MP and Level to all other Player of the Party. <b><u>Actions</u>:</b>
	 * <li>Send the Server->Client packet StatusUpdate with current HP, MP and CP to this Player</li>
	 * <li>Send the Server->Client packet PartySmallWindowUpdate with current HP, MP and Level to all other Player of the Party</li> <font color=#FF0000><b><u>Caution</u>: This method DOESN'T SEND current HP and MP to all Player of the _statusListener</b></font>
	 */
	@Override
	public void broadcastStatusUpdate()
	{
		if (_broadcastStatusUpdateTask == null)
		{
			_broadcastStatusUpdateTask = ThreadPool.schedule(() ->
			{
				final StatusUpdate su = new StatusUpdate(this);
				su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
				su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
				su.addAttribute(StatusUpdate.MAX_MP, getMaxMp());
				su.addAttribute(StatusUpdate.CUR_MP, (int) getCurrentMp());
				su.addAttribute(StatusUpdate.MAX_CP, getMaxCp());
				su.addAttribute(StatusUpdate.CUR_CP, (int) getCurrentCp());
				sendPacket(su);
				
				final boolean needCpUpdate = needCpUpdate();
				final boolean needHpUpdate = needHpUpdate();
				final Party party = getParty();
				
				// Check if a party is in progress and party window update is useful.
				if ((party != null) && (needCpUpdate || needHpUpdate || needMpUpdate()))
				{
					party.broadcastToPartyMembers(this, new PartySmallWindowUpdate(this));
				}
				
				if (_inOlympiadMode && _olympiadStart && (needCpUpdate || needHpUpdate))
				{
					Collection<Player> players = World.getInstance().getVisibleObjects(this, Player.class);
					if ((players != null) && !players.isEmpty())
					{
						final ExOlympiadUserInfo olyInfo = new ExOlympiadUserInfo(this, 1);
						for (Player player : players)
						{
							if ((player != null) && player.isInOlympiadMode() && (player.getOlympiadGameId() == _olympiadGameId))
							{
								player.sendPacket(olyInfo);
							}
						}
					}
					
					players = Olympiad.getInstance().getSpectators(_olympiadGameId);
					if ((players != null) && !players.isEmpty())
					{
						final ExOlympiadUserInfo olyInfo = new ExOlympiadUserInfo(this, getOlympiadSide());
						for (Player spectator : players)
						{
							if (spectator == null)
							{
								continue;
							}
							
							spectator.sendPacket(olyInfo);
						}
					}
				}
				
				// In duel MP updated only with CP or HP.
				if (_isInDuel && (needCpUpdate || needHpUpdate))
				{
					DuelManager.getInstance().broadcastToOppositTeam(this, new ExDuelUpdateUserInfo(this));
				}
				
				_broadcastStatusUpdateTask = null;
			}, 50);
		}
	}
	
	/**
	 * Send a Server->Client packet UserInfo to this Player and CharInfo to all known players.<br>
	 * <font color=#FF0000><b><u>Caution</u>: DON'T SEND UserInfo packet to other players instead of CharInfo packet.<br>
	 * UserInfo packet contains PRIVATE DATA as MaxHP, STR, DEX...</b></font>
	 */
	public void broadcastUserInfo()
	{
		// Send user info to the current player.
		updateUserInfo();
		
		// Broadcast char info to known players.
		broadcastCharInfo();
	}
	
	public void updateUserInfo()
	{
		sendPacket(new UserInfo(this));
		// sendPacket(new ExBrExtraUserInfo(this));
	}
	
	public void broadcastCharInfo()
	{
		// Client is disconnected.
		if (isOnlineInt() == 0)
		{
			return;
		}
		
		if (_broadcastCharInfoTask == null)
		{
			_broadcastCharInfoTask = ThreadPool.schedule(() ->
			{
				final CharInfo charInfo = new CharInfo(this, false);
				charInfo.sendInBroadcast();
				World.getInstance().forEachVisibleObject(this, Player.class, player ->
				{
					if (isVisibleFor(player))
					{
						if (isInvisible() && isGM())
						{
							player.sendPacket(new CharInfo(this, true));
						}
						else
						{
							player.sendPacket(charInfo);
						}
						
						// player.sendPacket(new ExBrExtraUserInfo(this));
						
						// Update relation.
						final int relation = getRelation(player);
						final boolean isAutoAttackable = isAutoAttackable(player);
						final RelationCache oldrelation = getKnownRelations().get(player.getObjectId());
						if ((oldrelation == null) || (oldrelation.getRelation() != relation) || (oldrelation.isAutoAttackable() != isAutoAttackable))
						{
							player.sendPacket(new RelationChanged(this, relation, isAutoAttackable));
							if (hasSummon())
							{
								player.sendPacket(new RelationChanged(_summon, relation, isAutoAttackable));
							}
							
							getKnownRelations().put(player.getObjectId(), new RelationCache(relation, isAutoAttackable));
						}
					}
				});
				_broadcastCharInfoTask = null;
			}, 100);
		}
	}
	
	public void broadcastTitleInfo()
	{
		// Send a Server->Client packet UserInfo to this Player.
		updateUserInfo();
		
		// Send a Server->Client packet TitleUpdate to all known players.
		broadcastPacket(new NicknameChanged(this));
	}
	
	public void broadcastMessage(String text)
	{
		World.getInstance().forEachVisibleObject(this, Player.class, player ->
		{
			if (!isVisibleFor(player))
			{
				return;
			}
			
			player.sendMessage(text);
		});
	}
	
	@Override
	public void broadcastPacket(ServerPacket packet)
	{
		packet.sendInBroadcast();
		
		final boolean isCharInfo = packet instanceof CharInfo;
		if (!isCharInfo)
		{
			sendPacket(packet);
		}
		
		World.getInstance().forEachVisibleObject(this, Player.class, player ->
		{
			if (!isVisibleFor(player))
			{
				return;
			}
			
			player.sendPacket(packet);
			
			if (isCharInfo)
			{
				final int relation = getRelation(player);
				final boolean isAutoAttackable = isAutoAttackable(player);
				final RelationCache cache = getKnownRelations().get(player.getObjectId());
				if ((cache == null) || (cache.getRelation() != relation) || (cache.isAutoAttackable() != isAutoAttackable))
				{
					player.sendPacket(new RelationChanged(this, relation, isAutoAttackable));
					if (hasSummon())
					{
						player.sendPacket(new RelationChanged(_summon, relation, isAutoAttackable));
					}
					
					getKnownRelations().put(player.getObjectId(), new RelationCache(relation, isAutoAttackable));
				}
			}
		});
	}
	
	/**
	 * @return the Alliance Identifier of the Player.
	 */
	@Override
	public int getAllyId()
	{
		return _clan == null ? 0 : _clan.getAllyId();
	}
	
	public int getAllyCrestId()
	{
		return getAllyId() == 0 ? 0 : _clan.getAllyCrestId();
	}
	
	@Override
	public void sendPacket(ServerPacket packet)
	{
		if (_client != null)
		{
			_client.sendPacket(packet);
		}
	}
	
	/**
	 * Send SystemMessage packet.
	 * @param id SystemMessageId
	 */
	@Override
	public void sendPacket(SystemMessageId id)
	{
		sendPacket(new SystemMessage(id));
	}
	
	/**
	 * Manage Interact Task with another Player. <b><u>Actions</u>:</b>
	 * <li>If the private store is a STORE_PRIVATE_SELL, send a Server->Client PrivateBuyListSell packet to the Player</li>
	 * <li>If the private store is a STORE_PRIVATE_BUY, send a Server->Client PrivateBuyListBuy packet to the Player</li>
	 * <li>If the private store is a STORE_PRIVATE_MANUFACTURE, send a Server->Client RecipeShopSellList packet to the Player</li><br>
	 * @param target The Creature targeted
	 */
	public void doInteract(Creature target)
	{
		if (target == null)
		{
			return;
		}
		
		if (target.isPlayer())
		{
			final Player temp = target.asPlayer();
			sendPacket(ActionFailed.STATIC_PACKET);
			
			if ((temp.getPrivateStoreType() == PrivateStoreType.SELL) || (temp.getPrivateStoreType() == PrivateStoreType.PACKAGE_SELL))
			{
				sendPacket(new PrivateStoreListSell(this, temp));
			}
			else if (temp.getPrivateStoreType() == PrivateStoreType.BUY)
			{
				sendPacket(new PrivateStoreListBuy(this, temp));
			}
			else if (temp.getPrivateStoreType() == PrivateStoreType.MANUFACTURE)
			{
				sendPacket(new RecipeShopSellList(this, temp));
			}
		}
		else // _interactTarget=null should never happen but one never knows ^^;
		{
			target.onAction(this);
		}
	}
	
	/**
	 * Manages AutoLoot Task.<br>
	 * <ul>
	 * <li>Send a system message to the player.</li>
	 * <li>Add the item to the player's inventory.</li>
	 * <li>Send a Server->Client packet InventoryUpdate to this player with NewItem (use a new slot) or ModifiedItem (increase amount).</li>
	 * <li>Send a Server->Client packet StatusUpdate to this player with current weight.</li>
	 * </ul>
	 * <font color=#FF0000><b><u>Caution</u>: If a party is in progress, distribute the items between the party members!</b></font>
	 * @param target the NPC dropping the item
	 * @param itemId the item ID
	 * @param itemCount the item count
	 */
	public void doAutoLoot(Attackable target, int itemId, int itemCount)
	{
		if (isInParty() && !ItemData.getInstance().getTemplate(itemId).hasExImmediateEffect())
		{
			_party.distributeItem(this, itemId, itemCount, false, target);
		}
		else if (itemId == Inventory.ADENA_ID)
		{
			addAdena(ItemProcessType.LOOT, itemCount, target, true);
		}
		else
		{
			addItem(ItemProcessType.LOOT, itemId, itemCount, target, true);
		}
	}
	
	/**
	 * Method overload for {@link Player#doAutoLoot(Attackable, int, int)}
	 * @param target the NPC dropping the item
	 * @param item the item holder
	 */
	public void doAutoLoot(Attackable target, ItemHolder item)
	{
		doAutoLoot(target, item.getId(), item.getCount());
	}
	
	/**
	 * Manage Pickup Task. <b><u>Actions</u>:</b>
	 * <li>Send a Server->Client packet StopMove to this Player</li>
	 * <li>Remove the Item from the world and send server->client GetItem packets</li>
	 * <li>Send a System Message to the Player : YOU_PICKED_UP_S1_ADENA or YOU_PICKED_UP_S1_S2</li>
	 * <li>Add the Item to the Player inventory</li>
	 * <li>Send a Server->Client packet InventoryUpdate to this Player with NewItem (use a new slot) or ModifiedItem (increase amount)</li>
	 * <li>Send a Server->Client packet StatusUpdate to this Player with current weight</li> <font color=#FF0000><b><u>Caution</u>: If a Party is in progress, distribute Items between party members</b></font>
	 * @param object The Item to pick up
	 */
	@Override
	public void doPickupItem(WorldObject object)
	{
		if (isAlikeDead() || _isFakeDeath)
		{
			return;
		}
		
		if (getActiveTradeList() != null)
		{
			sendPacket(SystemMessageId.YOU_CANNOT_PICK_UP_OR_USE_ITEMS_WHILE_TRADING);
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Set the AI Intention to IDLE
		getAI().setIntention(Intention.IDLE);
		
		// Check if the WorldObject to pick up is a Item
		if (!object.isItem())
		{
			// do not try to pickup anything that is not an item :)
			LOGGER.warning(this + " trying to pickup wrong target." + getTarget());
			return;
		}
		
		final Item target = (Item) object;
		sendPacket(new StopMove(this));
		SystemMessage smsg = null;
		synchronized (target)
		{
			// Check if the target to pick up is visible
			if (!target.isSpawned())
			{
				// Send a Server->Client packet ActionFailed to this Player
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			if (!target.getDropProtection().tryPickUp(this))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				smsg = new SystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_PICK_UP_S1);
				smsg.addItemName(target);
				sendPacket(smsg);
				return;
			}
			
			if (((isInParty() && (_party.getDistributionType() == PartyDistributionType.FINDERS_KEEPERS)) || !isInParty()) && !_inventory.validateCapacity(target))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.YOUR_INVENTORY_IS_FULL);
				return;
			}
			
			if (isInvisible() && !isGM())
			{
				return;
			}
			
			if ((target.getOwnerId() != 0) && (target.getOwnerId() != getObjectId()) && !isInLooterParty(target.getOwnerId()))
			{
				if (target.getId() == Inventory.ADENA_ID)
				{
					smsg = new SystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_PICK_UP_S1_ADENA);
					smsg.addInt(target.getCount());
				}
				else if (target.getCount() > 1)
				{
					smsg = new SystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_PICK_UP_S2_S1_S);
					smsg.addItemName(target);
					smsg.addInt(target.getCount());
				}
				else
				{
					smsg = new SystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_PICK_UP_S1);
					smsg.addItemName(target);
				}
				
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(smsg);
				return;
			}
			
			if ((target.getItemLootShedule() != null) && ((target.getOwnerId() == getObjectId()) || isInLooterParty(target.getOwnerId())))
			{
				target.resetOwnerTimer();
			}
			
			// Remove the Item from the world and send server->client GetItem packets
			target.pickupMe(this);
			if (GeneralConfig.SAVE_DROPPED_ITEM)
			{
				ItemsOnGroundManager.getInstance().removeObject(target);
			}
		}
		
		// Auto use herbs - pick up
		if (target.getTemplate().hasExImmediateEffect())
		{
			final IItemHandler handler = ItemHandler.getInstance().getHandler(target.getEtcItem());
			if (handler != null)
			{
				handler.onItemUse(this, target, false);
			}
			else
			{
				LOGGER.warning("No item handler registered for item ID: " + target.getId() + ".");
			}
			
			ItemManager.destroyItem(null, target, this, null);
		}
		// Cursed Weapons are not distributed
		else if (CursedWeaponsManager.getInstance().isCursed(target.getId()))
		{
			addItem(ItemProcessType.PICKUP, target, null, true);
		}
		else
		{
			// if item is instance of ArmorType or WeaponType broadcast an "Attention" system message
			if ((target.getItemType() instanceof ArmorType) || (target.getItemType() instanceof WeaponType))
			{
				if (target.isEnchanted())
				{
					smsg = new SystemMessage(SystemMessageId.ATTENTION_S1_PICKED_UP_S2_S3);
					smsg.addPcName(this);
					smsg.addInt(target.getEnchantLevel());
				}
				else
				{
					smsg = new SystemMessage(SystemMessageId.ATTENTION_S1_PICKED_UP_S2);
					smsg.addPcName(this);
				}
				
				smsg.addItemName(target.getId());
				broadcastPacket(smsg);
			}
			
			// Check if a Party is in progress
			if (isInParty())
			{
				_party.distributeItem(this, target);
			}
			else if ((target.getId() == Inventory.ADENA_ID) && (_inventory.getAdenaInstance() != null))
			{
				addAdena(ItemProcessType.PICKUP, target.getCount(), null, true);
				ItemManager.destroyItem(ItemProcessType.PICKUP, target, this, null);
			}
			else
			{
				addItem(ItemProcessType.PICKUP, target, null, true);
				
				// Auto-Equip arrows/bolts if player has a bow/crossbow and player picks up arrows/bolts.
				final Item weapon = _inventory.getPaperdollItem(Inventory.PAPERDOLL_RHAND);
				if (weapon != null)
				{
					final EtcItem etcItem = target.getEtcItem();
					if (etcItem != null)
					{
						final EtcItemType itemType = etcItem.getItemType();
						if ((weapon.getItemType() == WeaponType.BOW) && (itemType == EtcItemType.ARROW))
						{
							checkAndEquipArrows();
						}
					}
				}
			}
		}
	}
	
	@Override
	public void doAttack(Creature target)
	{
		super.doAttack(target);
		setRecentFakeDeath(false);
		if (target.isFakePlayer() && !FakePlayersConfig.FAKE_PLAYER_AUTO_ATTACKABLE)
		{
			updatePvPStatus();
		}
	}
	
	@Override
	public void doCast(Skill skill)
	{
		if ((_currentSkill != null) && !checkUseMagicConditions(skill, _currentSkill.isCtrlPressed(), _currentSkill.isShiftPressed()))
		{
			setCastingNow(false);
			setCastingSimultaneouslyNow(false);
			return;
		}
		
		super.doCast(skill);
		setRecentFakeDeath(false);
	}
	
	public boolean canOpenPrivateStore()
	{
		if ((PrivateStoreRangeConfig.SHOP_MIN_RANGE_FROM_NPC > 0) || (PrivateStoreRangeConfig.SHOP_MIN_RANGE_FROM_PLAYER > 0))
		{
			for (Creature creature : World.getInstance().getVisibleObjects(this, Creature.class))
			{
				if ((creature.getMinShopDistance() > 0) && LocationUtil.checkIfInRange(creature.getMinShopDistance(), this, creature, true))
				{
					sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_OPEN_A_PRIVATE_STORE_HERE));
					return false;
				}
			}
		}
		
		return !isAlikeDead() && !_inOlympiadMode && !isMounted() && !isInsideZone(ZoneId.NO_STORE) && !isCastingNow();
	}
	
	@Override
	public int getMinShopDistance()
	{
		return _waitTypeSitting ? PrivateStoreRangeConfig.SHOP_MIN_RANGE_FROM_PLAYER : 0;
	}
	
	public void tryOpenPrivateBuyStore()
	{
		// Player shouldn't be able to set stores if he/she is alike dead (dead or fake death)
		if (canOpenPrivateStore())
		{
			if ((_privateStoreType == PrivateStoreType.BUY) || (_privateStoreType == PrivateStoreType.BUY_MANAGE))
			{
				setPrivateStoreType(PrivateStoreType.NONE);
			}
			
			if (_privateStoreType == PrivateStoreType.NONE)
			{
				if (_waitTypeSitting)
				{
					standUp();
				}
				
				setPrivateStoreType(PrivateStoreType.BUY_MANAGE);
				sendPacket(new PrivateStoreManageListBuy(this));
			}
		}
		else
		{
			if (isInsideZone(ZoneId.NO_STORE))
			{
				sendPacket(SystemMessageId.YOU_CANNOT_OPEN_A_PRIVATE_STORE_HERE);
			}
			
			sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
	
	public void tryOpenPrivateSellStore(boolean isPackageSale)
	{
		// Player shouldn't be able to set stores if he/she is alike dead (dead or fake death)
		if (canOpenPrivateStore())
		{
			if ((_privateStoreType == PrivateStoreType.SELL) || (_privateStoreType == PrivateStoreType.SELL_MANAGE) || (_privateStoreType == PrivateStoreType.PACKAGE_SELL))
			{
				setPrivateStoreType(PrivateStoreType.NONE);
			}
			
			if (_privateStoreType == PrivateStoreType.NONE)
			{
				if (_waitTypeSitting)
				{
					standUp();
				}
				
				setPrivateStoreType(PrivateStoreType.SELL_MANAGE);
				sendPacket(new PrivateStoreManageListSell(this, isPackageSale));
			}
		}
		else
		{
			if ((_privateStoreType != PrivateStoreType.NONE) && !isAlikeDead())
			{
				setPrivateStoreType(PrivateStoreType.NONE);
				if (_waitTypeSitting)
				{
					standUp();
				}
				return;
			}
			
			if (isInsideZone(ZoneId.NO_STORE))
			{
				sendPacket(SystemMessageId.YOU_CANNOT_OPEN_A_PRIVATE_STORE_HERE);
			}
			
			sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
	
	public PreparedListContainer getMultiSell()
	{
		return _currentMultiSell;
	}
	
	public void setMultiSell(PreparedListContainer list)
	{
		_currentMultiSell = list;
	}
	
	/**
	 * Set a target. <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Remove the Player from the _statusListener of the old target if it was a Creature</li>
	 * <li>Add the Player to the _statusListener of the new target if it's a Creature</li>
	 * <li>Target the new WorldObject (add the target to the Player _target, _knownObject and Player to _KnownObject of the WorldObject)</li>
	 * </ul>
	 * @param worldObject The WorldObject to target
	 */
	@Override
	public void setTarget(WorldObject worldObject)
	{
		WorldObject newTarget = worldObject;
		if (newTarget != null)
		{
			final boolean isInParty = newTarget.isPlayer() && isInParty() && _party.containsPlayer(newTarget.asPlayer());
			
			// Prevents /target exploiting
			if (!isInParty && (Math.abs(newTarget.getZ() - getZ()) > 3000))
			{
				newTarget = null;
			}
			
			// Check if the new target is visible
			if ((newTarget != null) && !isInParty && !newTarget.isSpawned())
			{
				newTarget = null;
			}
			
			// vehicles cannot be targeted
			if (!isGM() && (newTarget instanceof Vehicle))
			{
				newTarget = null;
			}
		}
		
		// Get the current target
		final WorldObject oldTarget = getTarget();
		if (oldTarget != null)
		{
			if (oldTarget.equals(newTarget)) // no target change?
			{
				// Validate location of the target.
				if ((newTarget != null) && (newTarget.getObjectId() != getObjectId()))
				{
					sendPacket(new ValidateLocation(newTarget));
				}
				return;
			}
			
			// Remove the target from the status listener.
			oldTarget.removeStatusListener(this);
		}
		
		if ((newTarget != null) && newTarget.isCreature())
		{
			final Creature target = newTarget.asCreature();
			
			// Validate location of the new target.
			if (newTarget.getObjectId() != getObjectId())
			{
				sendPacket(new ValidateLocation(target));
			}
			
			// Show the client his new target.
			sendPacket(new MyTargetSelected(this, target));
			
			// Register target to listen for hp changes.
			target.addStatusListener(this);
			
			// Send max/current hp.
			final StatusUpdate su = new StatusUpdate(target);
			su.addAttribute(StatusUpdate.MAX_HP, target.getMaxHp());
			su.addAttribute(StatusUpdate.CUR_HP, (int) target.getCurrentHp());
			sendPacket(su);
			
			// To others the new target, and not yourself!
			Broadcast.toKnownPlayers(this, new TargetSelected(getObjectId(), newTarget.getObjectId(), getX(), getY(), getZ()));
		}
		
		// Target was removed?
		if ((newTarget == null) && (getTarget() != null))
		{
			broadcastPacket(new TargetUnselected(this));
		}
		
		// Target the new WorldObject (add the target to the Player _target, _knownObject and Player to _KnownObject of the WorldObject)
		super.setTarget(newTarget);
	}
	
	/**
	 * Return the active weapon instance (always equipped in the right hand).
	 */
	@Override
	public Item getActiveWeaponInstance()
	{
		return _inventory.getPaperdollItem(Inventory.PAPERDOLL_RHAND);
	}
	
	/**
	 * Return the active weapon item (always equipped in the right hand).
	 */
	@Override
	public Weapon getActiveWeaponItem()
	{
		final Item weapon = getActiveWeaponInstance();
		return weapon == null ? _fistsWeaponItem : (Weapon) weapon.getTemplate();
	}
	
	public Item getChestArmorInstance()
	{
		return _inventory.getPaperdollItem(Inventory.PAPERDOLL_CHEST);
	}
	
	public Item getLegsArmorInstance()
	{
		return _inventory.getPaperdollItem(Inventory.PAPERDOLL_LEGS);
	}
	
	public Armor getActiveChestArmorItem()
	{
		final Item armor = getChestArmorInstance();
		return armor == null ? null : (Armor) armor.getTemplate();
	}
	
	public Armor getActiveLegsArmorItem()
	{
		final Item legs = getLegsArmorInstance();
		return legs == null ? null : (Armor) legs.getTemplate();
	}
	
	public boolean isWearingHeavyArmor()
	{
		final Item legs = getLegsArmorInstance();
		final Item armor = getChestArmorInstance();
		if ((armor != null) && (legs != null) && (legs.getItemType() == ArmorType.HEAVY) && (armor.getItemType() == ArmorType.HEAVY))
		{
			return true;
		}
		
		if ((armor != null) && (getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST).getTemplate().getBodyPart() == BodyPart.FULL_ARMOR) && (armor.getItemType() == ArmorType.HEAVY))
		{
			return true;
		}
		
		return false;
	}
	
	public boolean isWearingLightArmor()
	{
		final Item legs = getLegsArmorInstance();
		final Item armor = getChestArmorInstance();
		if ((armor != null) && (legs != null) && (legs.getItemType() == ArmorType.LIGHT) && (armor.getItemType() == ArmorType.LIGHT))
		{
			return true;
		}
		
		if ((armor != null) && (_inventory.getPaperdollItem(Inventory.PAPERDOLL_CHEST).getTemplate().getBodyPart() == BodyPart.FULL_ARMOR) && (armor.getItemType() == ArmorType.LIGHT))
		{
			return true;
		}
		
		return false;
	}
	
	public boolean isWearingMagicArmor()
	{
		final Item legs = getLegsArmorInstance();
		final Item armor = getChestArmorInstance();
		if ((armor != null) && (legs != null) && (legs.getItemType() == ArmorType.MAGIC) && (armor.getItemType() == ArmorType.MAGIC))
		{
			return true;
		}
		
		if ((armor != null) && (_inventory.getPaperdollItem(Inventory.PAPERDOLL_CHEST).getTemplate().getBodyPart() == BodyPart.FULL_ARMOR) && (armor.getItemType() == ArmorType.MAGIC))
		{
			return true;
		}
		
		return false;
	}
	
	public boolean isWearingFormalWear()
	{
		final Item formalWear = getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
		return (formalWear != null) && (formalWear.getTemplate().getBodyPart() == BodyPart.ALLDRESS);
	}
	
	public boolean isMarried()
	{
		return _married;
	}
	
	public void setMarried(boolean value)
	{
		_married = value;
	}
	
	public boolean isEngageRequest()
	{
		return _engagerequest;
	}
	
	public void setEngageRequest(boolean state, int playerid)
	{
		_engagerequest = state;
		_engageid = playerid;
	}
	
	public void setMarryRequest(boolean value)
	{
		_marryrequest = value;
	}
	
	public boolean isMarryRequest()
	{
		return _marryrequest;
	}
	
	public void setMarryAccepted(boolean value)
	{
		_marryaccepted = value;
	}
	
	public boolean isMarryAccepted()
	{
		return _marryaccepted;
	}
	
	public int getEngageId()
	{
		return _engageid;
	}
	
	public int getPartnerId()
	{
		return _partnerId;
	}
	
	public void setPartnerId(int partnerid)
	{
		_partnerId = partnerid;
	}
	
	public int getCoupleId()
	{
		return _coupleId;
	}
	
	public void setCoupleId(int coupleId)
	{
		_coupleId = coupleId;
	}
	
	public void engageAnswer(int answer)
	{
		if (!_engagerequest || (_engageid == 0))
		{
			return;
		}
		
		final Player ptarget = World.getInstance().getPlayer(_engageid);
		setEngageRequest(false, 0);
		if (ptarget != null)
		{
			if (answer == 1)
			{
				CoupleManager.getInstance().createCouple(ptarget, Player.this);
				ptarget.sendMessage("Request to Engage has been >ACCEPTED<");
			}
			else
			{
				ptarget.sendMessage("Request to Engage has been >DENIED<!");
			}
		}
	}
	
	/**
	 * Return the secondary weapon instance (always equipped in the left hand).
	 */
	@Override
	public Item getSecondaryWeaponInstance()
	{
		return _inventory.getPaperdollItem(Inventory.PAPERDOLL_LHAND);
	}
	
	/**
	 * Return the secondary Item item (always equipped in the left hand).<br>
	 * Arrows, Shield..
	 */
	@Override
	public ItemTemplate getSecondaryWeaponItem()
	{
		final Item item = _inventory.getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		return item != null ? item.getTemplate() : null;
	}
	
	/**
	 * Kill the Creature, Apply Death Penalty, Manage gain/loss Karma and Item Drop. <b><u>Actions</u>:</b>
	 * <li>Reduce the Experience of the Player in function of the calculated Death Penalty</li>
	 * <li>If necessary, unsummon the Pet of the killed Player</li>
	 * <li>Manage Karma gain for attacker and Karam loss for the killed Player</li>
	 * <li>If the killed Player has Karma, manage Drop Item</li>
	 * <li>Kill the Player</li><br>
	 * @param killer
	 */
	@Override
	public boolean doDie(Creature killer)
	{
		// Kill the Player
		if (!super.doDie(killer))
		{
			return false;
		}
		
		if (isMounted())
		{
			stopFeed();
		}
		
		// synchronized (this)
		// {
		if (_isFakeDeath)
		{
			stopFakeDeath(true);
		}
		// }
		
		if (killer != null)
		{
			final Player pk = killer.asPlayer();
			final boolean fpcKill = killer.isFakePlayer();
			if ((pk != null) || fpcKill)
			{
				if (pk != null)
				{
					if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_PVP_KILL, this))
					{
						EventDispatcher.getInstance().notifyEventAsync(new OnPlayerPvPKill(pk, this), this);
					}
					
					// pvp/pk item rewards
					if (!(PvpRewardItemConfig.DISABLE_REWARDS_IN_INSTANCES && (getInstanceId() != 0)) && //
						!(PvpRewardItemConfig.DISABLE_REWARDS_IN_PVP_ZONES && isInsideZone(ZoneId.PVP)))
					{
						// pvp
						if (PvpRewardItemConfig.REWARD_PVP_ITEM && (_pvpFlag != 0))
						{
							pk.addItem(ItemProcessType.REWARD, PvpRewardItemConfig.REWARD_PVP_ITEM_ID, PvpRewardItemConfig.REWARD_PVP_ITEM_AMOUNT, this, PvpRewardItemConfig.REWARD_PVP_ITEM_MESSAGE);
						}
						
						// pk
						if (PvpRewardItemConfig.REWARD_PK_ITEM && (_pvpFlag == 0))
						{
							pk.addItem(ItemProcessType.REWARD, PvpRewardItemConfig.REWARD_PK_ITEM_ID, PvpRewardItemConfig.REWARD_PK_ITEM_AMOUNT, this, PvpRewardItemConfig.REWARD_PK_ITEM_MESSAGE);
						}
					}
				}
				
				// announce pvp/pk
				if (PvpAnnounceConfig.ANNOUNCE_PK_PVP && (((pk != null) && !pk.isGM()) || fpcKill))
				{
					String msg = "";
					if (_pvpFlag == 0)
					{
						msg = PvpAnnounceConfig.ANNOUNCE_PK_MSG.replace("$killer", pk != null ? pk.getName() : killer.getName()).replace("$target", getName());
						if (PvpAnnounceConfig.ANNOUNCE_PK_PVP_NORMAL_MESSAGE)
						{
							final SystemMessage sm = new SystemMessage(SystemMessageId.S1);
							sm.addString(msg);
							Broadcast.toAllOnlinePlayers(sm);
						}
						else
						{
							Broadcast.toAllOnlinePlayers(msg, false);
						}
					}
					else if (_pvpFlag != 0)
					{
						msg = PvpAnnounceConfig.ANNOUNCE_PVP_MSG.replace("$killer", killer.getName()).replace("$target", getName());
						if (PvpAnnounceConfig.ANNOUNCE_PK_PVP_NORMAL_MESSAGE)
						{
							final SystemMessage sm = new SystemMessage(SystemMessageId.S1);
							sm.addString(msg);
							Broadcast.toAllOnlinePlayers(sm);
						}
						else
						{
							Broadcast.toAllOnlinePlayers(msg, false);
						}
					}
				}
				
				if (fpcKill && FakePlayersConfig.FAKE_PLAYER_KILL_KARMA && (_pvpFlag == 0) && (getKarma() <= 0))
				{
					killer.setKarma(killer.getKarma() + 150);
					killer.broadcastInfo();
				}
			}
			
			broadcastStatusUpdate();
			
			// Clear resurrect xp calculation
			setExpBeforeDeath(0);
			
			// Issues drop of Cursed Weapon.
			if (isCursedWeaponEquipped())
			{
				CursedWeaponsManager.getInstance().drop(_cursedWeaponEquippedId, killer);
			}
			else
			{
				final boolean insidePvpZone = isInsideZone(ZoneId.PVP);
				final boolean insideSiegeZone = isInsideZone(ZoneId.SIEGE);
				if ((pk == null) || !pk.isCursedWeaponEquipped())
				{
					onDieDropItem(killer); // Check if any item should be dropped
					if (!insidePvpZone && !insideSiegeZone)
					{
						if ((pk != null) && (pk.getClan() != null) && (getClan() != null) && !isAcademyMember() && !(pk.isAcademyMember()))
						{
							if ((_clan.isAtWarWith(pk.getClanId()) && pk.getClan().isAtWarWith(_clan.getId())) || (isInSiege() && pk.isInSiege()))
							{
								if (AntiFeedManager.getInstance().check(killer, this))
								{
									// when your reputation score is 0 or below, the other clan cannot acquire any reputation points
									if (_clan.getReputationScore() > 0)
									{
										pk.getClan().addReputationScore(FeatureConfig.REPUTATION_SCORE_PER_KILL);
									}
									
									// when the opposing sides reputation score is 0 or below, your clans reputation score does not decrease
									if (pk.getClan().getReputationScore() > 0)
									{
										_clan.takeReputationScore(FeatureConfig.REPUTATION_SCORE_PER_KILL);
									}
								}
							}
						}
					}
					
					// Should not penalize player when lucky, in a non siege PvP zone or is in an event.
					if (PlayerConfig.PLAYER_DELEVEL && !isLucky() && (insideSiegeZone || !insidePvpZone) && !isOnEvent())
					{
						calculateDeathExpPenalty(killer, isAtWarWith(pk));
					}
				}
			}
		}
		
		// Unsummon Cubics
		if (!_cubics.isEmpty())
		{
			for (Cubic cubic : _cubics.values())
			{
				cubic.stopAction();
				cubic.cancelDisappear();
			}
			
			_cubics.clear();
		}
		
		if (isChannelized())
		{
			getSkillChannelized().abortChannelization();
		}
		
		if (isInParty() && _party.isInDimensionalRift())
		{
			_party.getDimensionalRift().getDeadMemberList().add(this);
		}
		
		if (_agathionId != 0)
		{
			setAgathionId(0);
		}
		
		// calculate death penalty buff
		calculateDeathPenaltyBuffLevel(killer);
		
		if (hasSummon())
		{
			if (_summon.isBetrayed())
			{
				sendPacket(SystemMessageId.YOUR_PET_SERVITOR_IS_UNRESPONSIVE_AND_WILL_NOT_OBEY_ANY_ORDERS);
			}
			
			_summon.cancelAction();
		}
		
		stopRentPet();
		stopWaterTask();
		
		AntiFeedManager.getInstance().setLastDeathTime(getObjectId());
		
		// FIXME: Karma reduction tempfix.
		if (getKarma() > 0) // && (killer instanceof GuardInstance))
		{
			setKarma(getKarma() < 200 ? 0 : (int) (getKarma() - (getKarma() / 4)));
		}
		
		if (PlayerConfig.DISCONNECT_AFTER_DEATH)
		{
			DecayTaskManager.getInstance().add(this);
		}
		
		return true;
	}
	
	private void onDieDropItem(Creature killer)
	{
		if (isOnEvent() || (killer == null))
		{
			return;
		}
		
		final Player pk = killer.asPlayer();
		if ((getKarma() <= 0) && (pk != null) && (pk.getClan() != null) && (getClan() != null) && (pk.getClan().isAtWarWith(_clanId)
		
		// || _clan.isAtWarWith(killer.asPlayer().getClanId())
		))
		{
			return;
		}
		
		if ((!isInsideZone(ZoneId.PVP) || (pk == null)) && (!isGM() || PvpConfig.KARMA_DROP_GM))
		{
			boolean isKarmaDrop = false;
			final boolean isKillerNpc = killer instanceof Npc;
			final int pkLimit = PvpConfig.KARMA_PK_LIMIT;
			int dropEquip = 0;
			int dropEquipWeapon = 0;
			int dropItem = 0;
			int dropLimit = 0;
			int dropPercent = 0;
			if ((getKarma() > 0) && (_pkKills >= pkLimit))
			{
				isKarmaDrop = true;
				dropPercent = RatesConfig.KARMA_RATE_DROP;
				dropEquip = RatesConfig.KARMA_RATE_DROP_EQUIP;
				dropEquipWeapon = RatesConfig.KARMA_RATE_DROP_EQUIP_WEAPON;
				dropItem = RatesConfig.KARMA_RATE_DROP_ITEM;
				dropLimit = RatesConfig.KARMA_DROP_LIMIT;
			}
			else if (isKillerNpc && (getLevel() > 4) && !isFestivalParticipant())
			{
				dropPercent = RatesConfig.PLAYER_RATE_DROP;
				dropEquip = RatesConfig.PLAYER_RATE_DROP_EQUIP;
				dropEquipWeapon = RatesConfig.PLAYER_RATE_DROP_EQUIP_WEAPON;
				dropItem = RatesConfig.PLAYER_RATE_DROP_ITEM;
				dropLimit = RatesConfig.PLAYER_DROP_LIMIT;
			}
			
			if ((dropPercent > 0) && (Rnd.get(100) < dropPercent))
			{
				int dropCount = 0;
				int itemDropPercent = 0;
				for (Item itemDrop : _inventory.getItems())
				{
					// Don't drop
					if (itemDrop.isShadowItem() || // do not drop Shadow Items
						itemDrop.isTimeLimitedItem() || // do not drop Time Limited Items
						!itemDrop.isDropable() || (itemDrop.getId() == Inventory.ADENA_ID) || // Adena
						(itemDrop.getTemplate().getType2() == ItemTemplate.TYPE2_QUEST) || // Quest Items
						(hasSummon() && (_summon.getControlObjectId() == itemDrop.getId())) || // Control Item of active pet
						(Arrays.binarySearch(PvpConfig.KARMA_LIST_NONDROPPABLE_ITEMS, itemDrop.getId()) >= 0) || // Item listed in the non droppable item list
						(Arrays.binarySearch(PvpConfig.KARMA_LIST_NONDROPPABLE_PET_ITEMS, itemDrop.getId()) >= 0 // Item listed in the non droppable pet item list
						))
					{
						continue;
					}
					
					if (itemDrop.isEquipped())
					{
						// Set proper chance according to Item type of equipped Item
						itemDropPercent = itemDrop.getTemplate().getType2() == ItemTemplate.TYPE2_WEAPON ? dropEquipWeapon : dropEquip;
					}
					else
					{
						itemDropPercent = dropItem; // Item in inventory
					}
					
					// NOTE: Each time an item is dropped, the chance of another item being dropped gets lesser (dropCount * 2)
					if (Rnd.get(100) < itemDropPercent)
					{
						if (itemDrop.isEquipped())
						{
							_inventory.unEquipItemInSlot(itemDrop.getLocationSlot());
						}
						
						dropItem(ItemProcessType.DEATH, itemDrop, killer, true);
						sendItemList(false);
						
						if (isKarmaDrop)
						{
							LOGGER.warning(getName() + " has karma and dropped id = " + itemDrop.getId() + ", count = " + itemDrop.getCount());
						}
						else
						{
							LOGGER.warning(getName() + " dropped id = " + itemDrop.getId() + ", count = " + itemDrop.getCount());
						}
						
						if (++dropCount >= dropLimit)
						{
							break;
						}
					}
				}
			}
		}
	}
	
	public void onKillUpdatePvPKarma(Creature target)
	{
		if ((target == null) || !target.isPlayable())
		{
			return;
		}
		
		// Avoid nulls && check if player != killedPlayer
		final Player killedPlayer = target.asPlayer();
		if ((killedPlayer == null) || (killedPlayer == this))
		{
			return;
		}
		
		// Cursed weapons progress
		if (isCursedWeaponEquipped() && target.isPlayer())
		{
			CursedWeaponsManager.getInstance().increaseKills(_cursedWeaponEquippedId);
			return;
		}
		
		// Olympiad support
		if (isInOlympiadMode() || killedPlayer.isInOlympiadMode())
		{
			return;
		}
		
		// Duel support
		if (isInDuel() && killedPlayer.isInDuel())
		{
			return;
		}
		
		// If both players are in SIEGE zone just increase siege kills/deaths.
		if (target.isPlayer() && isInsideZone(ZoneId.SIEGE) && killedPlayer.isInsideZone(ZoneId.SIEGE))
		{
			if (!isSiegeFriend(killedPlayer))
			{
				final Clan targetClan = killedPlayer.getClan();
				if ((_clan != null) && (targetClan != null))
				{
					_clan.addSiegeKill();
					targetClan.addSiegeDeath();
				}
			}
			return;
		}
		
		// Do nothing when in PVP zone.
		if (isInsideZone(ZoneId.PVP) || target.isInsideZone(ZoneId.PVP))
		{
			return;
		}
		
		// Check if it's pvp
		if (checkIfPvP(target) && (killedPlayer.getPvpFlag() != 0))
		{
			increasePvpKills(target);
		}
		else
		{
			// Target player doesn't have pvp flag set
			// check about wars
			if ((killedPlayer.getClan() != null) && (getClan() != null) && getClan().isAtWarWith(killedPlayer.getClanId()) && killedPlayer.getClan().isAtWarWith(getClanId()) && (killedPlayer.getPledgeType() != Clan.SUBUNIT_ACADEMY) && (getPledgeType() != Clan.SUBUNIT_ACADEMY))
			{
				// 'Both way war' -> 'PvP Kill'
				increasePvpKills(target);
				return;
			}
			
			// 'No war' or 'One way war' -> 'Normal PK'
			if (killedPlayer.getKarma() > 0) // Target player has karma
			{
				if (PvpConfig.KARMA_AWARD_PK_KILL)
				{
					increasePvpKills(target);
				}
			}
			else if (killedPlayer.getPvpFlag() == 0) // Target player doesn't have karma
			{
				if (FactionSystemConfig.FACTION_SYSTEM_ENABLED)
				{
					if ((_isGood && killedPlayer.isGood()) || (_isEvil && killedPlayer.isEvil()))
					{
						increasePkKillsAndKarma(target);
					}
				}
				else
				{
					increasePkKillsAndKarma(target);
				}
				
				checkItemRestriction(); // Unequip adventurer items
			}
		}
	}
	
	/**
	 * Increase the pvp kills count and send the info to the player
	 * @param target
	 */
	public void increasePvpKills(Creature target)
	{
		if (target.isPlayer() && AntiFeedManager.getInstance().check(this, target))
		{
			setPvpKills(_pvpKills + 1);
			updatePvpTitleAndColor(true);
			
			// Send a Server->Client UserInfo packet to attacker with its Karma and PK Counter
			updateUserInfo();
		}
	}
	
	/**
	 * Increase pk count, karma and send the info to the player
	 * @param target
	 */
	public void increasePkKillsAndKarma(Creature target)
	{
		// Only playables can increase karma/pk
		if ((target == null) || !target.isPlayable())
		{
			return;
		}
		
		// Calculate new karma. (calculate karma before incrase pk count!)
		setKarma(getKarma() + Formulas.calculateKarmaGain(_pkKills, target.isSummon()));
		
		// PK Points are increased only if you kill a player.
		if (target.isPlayer())
		{
			setPkKills(_pkKills + 1);
		}
		
		// Update player's UI.
		updateUserInfo();
	}
	
	public void updatePvpTitleAndColor(boolean broadcastInfo)
	{
		if (PvpTitleColorConfig.PVP_COLOR_SYSTEM_ENABLED && !FactionSystemConfig.FACTION_SYSTEM_ENABLED) // Faction system uses title colors.
		{
			if ((_pvpKills >= PvpTitleColorConfig.PVP_AMOUNT1) && (_pvpKills < PvpTitleColorConfig.PVP_AMOUNT2))
			{
				setTitle("\u00AE " + PvpTitleColorConfig.TITLE_FOR_PVP_AMOUNT1 + " \u00AE");
				_appearance.setTitleColor(PvpTitleColorConfig.NAME_COLOR_FOR_PVP_AMOUNT1);
			}
			else if ((_pvpKills >= PvpTitleColorConfig.PVP_AMOUNT2) && (_pvpKills < PvpTitleColorConfig.PVP_AMOUNT3))
			{
				setTitle("\u00AE " + PvpTitleColorConfig.TITLE_FOR_PVP_AMOUNT2 + " \u00AE");
				_appearance.setTitleColor(PvpTitleColorConfig.NAME_COLOR_FOR_PVP_AMOUNT2);
			}
			else if ((_pvpKills >= PvpTitleColorConfig.PVP_AMOUNT3) && (_pvpKills < PvpTitleColorConfig.PVP_AMOUNT4))
			{
				setTitle("\u00AE " + PvpTitleColorConfig.TITLE_FOR_PVP_AMOUNT3 + " \u00AE");
				_appearance.setTitleColor(PvpTitleColorConfig.NAME_COLOR_FOR_PVP_AMOUNT3);
			}
			else if ((_pvpKills >= PvpTitleColorConfig.PVP_AMOUNT4) && (_pvpKills < PvpTitleColorConfig.PVP_AMOUNT5))
			{
				setTitle("\u00AE " + PvpTitleColorConfig.TITLE_FOR_PVP_AMOUNT4 + " \u00AE");
				_appearance.setTitleColor(PvpTitleColorConfig.NAME_COLOR_FOR_PVP_AMOUNT4);
			}
			else if (_pvpKills >= PvpTitleColorConfig.PVP_AMOUNT5)
			{
				setTitle("\u00AE " + PvpTitleColorConfig.TITLE_FOR_PVP_AMOUNT5 + " \u00AE");
				_appearance.setTitleColor(PvpTitleColorConfig.NAME_COLOR_FOR_PVP_AMOUNT5);
			}
			
			if (broadcastInfo)
			{
				broadcastTitleInfo();
			}
		}
	}
	
	public void updatePvPStatus()
	{
		if (isInsideZone(ZoneId.PVP))
		{
			return;
		}
		
		setPvpFlagLasts(System.currentTimeMillis() + PvpConfig.PVP_NORMAL_TIME);
		if (_pvpFlag == 0)
		{
			startPvPFlag();
		}
	}
	
	public void updatePvPStatus(Creature target)
	{
		final Player targetPlayer = target.asPlayer();
		if (targetPlayer == null)
		{
			return;
		}
		
		if (this == targetPlayer)
		{
			return;
		}
		
		if (FactionSystemConfig.FACTION_SYSTEM_ENABLED && target.isPlayer() && ((isGood() && targetPlayer.isEvil()) || (isEvil() && targetPlayer.isGood())))
		{
			return;
		}
		
		if (_isInDuel && (targetPlayer.getDuelId() == getDuelId()))
		{
			return;
		}
		
		if ((!isInsideZone(ZoneId.PVP) || !target.isInsideZone(ZoneId.PVP)) && (targetPlayer.getKarma() == 0))
		{
			if (checkIfPvP(targetPlayer))
			{
				setPvpFlagLasts(System.currentTimeMillis() + PvpConfig.PVP_PVP_TIME);
			}
			else
			{
				setPvpFlagLasts(System.currentTimeMillis() + PvpConfig.PVP_NORMAL_TIME);
			}
			
			if (_pvpFlag == 0)
			{
				startPvPFlag();
			}
		}
	}
	
	/**
	 * @return {@code true} if player has Lucky effect and is level 9 or less
	 */
	public boolean isLucky()
	{
		return (getLevel() <= 9) && isAffectedBySkill(CommonSkill.LUCKY.getId());
	}
	
	/**
	 * Restore the specified % of experience this Player has lost and sends a Server->Client StatusUpdate packet.
	 * @param restorePercent
	 */
	public void restoreExp(double restorePercent)
	{
		if (_expBeforeDeath > 0)
		{
			// Restore the specified % of lost experience.
			getStat().addExp(Math.round(((_expBeforeDeath - getExp()) * restorePercent) / 100));
			setExpBeforeDeath(0);
		}
	}
	
	/**
	 * Reduce the Experience (and level if necessary) of the Player in function of the calculated Death Penalty.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <li>Calculate the Experience loss</li>
	 * <li>Set the value of _expBeforeDeath</li>
	 * <li>Set the new Experience value of the Player and Decrease its level if necessary</li>
	 * <li>Send a Server->Client StatusUpdate packet with its new Experience</li><br>
	 * @param killer
	 * @param atWar
	 */
	public void calculateDeathExpPenalty(Creature killer, boolean atWar)
	{
		final int level = getLevel();
		double percentLost = ExperienceLossData.getInstance().getPercentLost(getLevel());
		
		// Calculate reduce exp lost stats.
		if (killer != null)
		{
			if (killer.isRaid())
			{
				percentLost *= calcStat(Stat.REDUCE_EXP_LOST_BY_RAID, 1);
			}
			else if (killer.isMonster())
			{
				percentLost *= calcStat(Stat.REDUCE_EXP_LOST_BY_MOB, 1);
			}
			else if (killer.isPlayable())
			{
				percentLost *= calcStat(Stat.REDUCE_EXP_LOST_BY_PVP, 1);
			}
		}
		
		// Calculate karma loss configuration.
		if (getKarma() > 0)
		{
			percentLost *= RatesConfig.RATE_KARMA_EXP_LOST;
		}
		
		// Get the total exp for the current player level.
		long currentLevelExp = 0;
		if (!isOnEvent())
		{
			final int maxLevel = ExperienceData.getInstance().getMaxLevel();
			if (level < maxLevel)
			{
				currentLevelExp = getStat().getExpForLevel(level + 1) - getStat().getExpForLevel(level);
			}
			else
			{
				currentLevelExp = getStat().getExpForLevel(maxLevel) - getStat().getExpForLevel(maxLevel - 1);
			}
		}
		
		// Calculate the experience loss.
		long lostExp = 0;
		if (currentLevelExp > 0)
		{
			lostExp = Math.round((currentLevelExp * percentLost) / 100);
			
			// Capping loss to 10% of current level experience.
			lostExp = Math.min(lostExp, currentLevelExp / 10);
			
			// Reduce experience lost for festival and war kills.
			if (isFestivalParticipant() || atWar)
			{
				lostExp /= 4;
			}
		}
		
		setExpBeforeDeath(getExp());
		getStat().removeExp(lostExp);
	}
	
	public boolean isPartyWaiting()
	{
		return PartyMatchWaitingList.getInstance().getPlayers().contains(this);
	}
	
	public void setPartyRoom(int id)
	{
		_partyroom = id;
	}
	
	public int getPartyRoom()
	{
		return _partyroom;
	}
	
	public boolean isInPartyMatchRoom()
	{
		return _partyroom > 0;
	}
	
	/**
	 * Stop the HP/MP/CP Regeneration task. <b><u>Actions</u>:</b>
	 * <li>Set the RegenActive flag to False</li>
	 * <li>Stop the HP/MP/CP Regeneration task</li>
	 */
	public void stopAllTimers()
	{
		stopHpMpRegeneration();
		stopWarnUserTakeBreak();
		stopWaterTask();
		stopFeed();
		storePetFood(_mountNpcId);
		stopRentPet();
		stopPvpRegTask();
		stopChargeTask();
		stopFameTask();
		stopVitalityTask();
	}
	
	/**
	 * @return {@code true} if the character has a summon, {@code false} otherwise
	 */
	public boolean hasSummon()
	{
		return _summon != null;
	}
	
	/**
	 * @return {@code true} if the character has a pet, {@code false} otherwise
	 */
	public boolean hasPet()
	{
		return (_summon != null) && _summon.isPet();
	}
	
	/**
	 * @return {@code true} if the character has a servitor, {@code false} otherwise
	 */
	public boolean hasServitor()
	{
		return (_summon != null) && _summon.isServitor();
	}
	
	/**
	 * @return the Summon of the Player or null.
	 */
	public Summon getSummon()
	{
		return _summon;
	}
	
	/**
	 * @return the Decoy of the Player or null.
	 */
	public Decoy getDecoy()
	{
		return _decoy;
	}
	
	/**
	 * @return the Trap of the Player or null.
	 */
	public Trap getTrap()
	{
		return _trap;
	}
	
	/**
	 * Set the Summon of the Player.
	 * @param summon
	 */
	public void setPet(Summon summon)
	{
		_summon = summon;
	}
	
	/**
	 * Set the Decoy of the Player.
	 * @param decoy
	 */
	public void setDecoy(Decoy decoy)
	{
		_decoy = decoy;
	}
	
	/**
	 * Set the Trap of this Player
	 * @param trap
	 */
	public void setTrap(Trap trap)
	{
		_trap = trap;
	}
	
	/**
	 * @return the Summon of the Player or null.
	 */
	public Collection<TamedBeast> getTrainedBeasts()
	{
		return _tamedBeast;
	}
	
	/**
	 * Set the Summon of the Player.
	 * @param tamedBeast
	 */
	public void addTrainedBeast(TamedBeast tamedBeast)
	{
		if (_tamedBeast == null)
		{
			_tamedBeast = ConcurrentHashMap.newKeySet();
		}
		
		_tamedBeast.add(tamedBeast);
	}
	
	/**
	 * @return the Player requester of a transaction (ex : FriendInvite, JoinAlly, JoinParty...).
	 */
	public RequestPartner getRequest()
	{
		return _request;
	}
	
	/**
	 * Set the Player requester of a transaction (ex : FriendInvite, JoinAlly, JoinParty...).
	 * @param requester
	 */
	public void setActiveRequester(Player requester)
	{
		_activeRequester = requester;
	}
	
	/**
	 * @return the Player requester of a transaction (ex : FriendInvite, JoinAlly, JoinParty...).
	 */
	public Player getActiveRequester()
	{
		final Player requester = _activeRequester;
		if ((requester != null) && requester.isRequestExpired() && (_activeTradeList == null))
		{
			_activeRequester = null;
		}
		
		return _activeRequester;
	}
	
	/**
	 * @return True if a transaction is in progress.
	 */
	public boolean isProcessingRequest()
	{
		return (getActiveRequester() != null) || (_requestExpireTime > GameTimeTaskManager.getInstance().getGameTicks());
	}
	
	/**
	 * @return True if a transaction is in progress.
	 */
	public boolean isProcessingTransaction()
	{
		return (getActiveRequester() != null) || (_activeTradeList != null) || (_requestExpireTime > GameTimeTaskManager.getInstance().getGameTicks());
	}
	
	/**
	 * Select the Warehouse to be used in next activity.
	 * @param partner
	 */
	public void onTransactionRequest(Player partner)
	{
		_requestExpireTime = GameTimeTaskManager.getInstance().getGameTicks() + (REQUEST_TIMEOUT * GameTimeTaskManager.TICKS_PER_SECOND);
		partner.setActiveRequester(this);
	}
	
	/**
	 * Return true if last request is expired.
	 * @return
	 */
	public boolean isRequestExpired()
	{
		return _requestExpireTime <= GameTimeTaskManager.getInstance().getGameTicks();
	}
	
	/**
	 * Select the Warehouse to be used in next activity.
	 */
	public void onTransactionResponse()
	{
		_requestExpireTime = 0;
	}
	
	/**
	 * Select the Warehouse to be used in next activity.
	 * @param warehouse
	 */
	public void setActiveWarehouse(ItemContainer warehouse)
	{
		_activeWarehouse = warehouse;
	}
	
	/**
	 * @return active Warehouse.
	 */
	public ItemContainer getActiveWarehouse()
	{
		return _activeWarehouse;
	}
	
	/**
	 * Select the TradeList to be used in next activity.
	 * @param tradeList
	 */
	public void setActiveTradeList(TradeList tradeList)
	{
		_activeTradeList = tradeList;
	}
	
	/**
	 * @return active TradeList.
	 */
	public TradeList getActiveTradeList()
	{
		return _activeTradeList;
	}
	
	public void onTradeStart(Player partner)
	{
		_activeTradeList = new TradeList(this);
		_activeTradeList.setPartner(partner);
		
		final SystemMessage msg = new SystemMessage(SystemMessageId.YOU_BEGIN_TRADING_WITH_S1);
		msg.addPcName(partner);
		sendPacket(msg);
		sendPacket(new TradeStart(this));
	}
	
	public void onTradeConfirm(Player partner)
	{
		final SystemMessage msg = new SystemMessage(SystemMessageId.S1_HAS_CONFIRMED_THE_TRADE);
		msg.addPcName(partner);
		sendPacket(msg);
		sendPacket(TradeOtherDone.STATIC_PACKET);
	}
	
	public void onTradeCancel(Player partner)
	{
		if (_activeTradeList == null)
		{
			return;
		}
		
		_activeTradeList.lock();
		_activeTradeList = null;
		sendPacket(new TradeDone(0));
		final SystemMessage msg = new SystemMessage(SystemMessageId.S1_HAS_CANCELED_THE_TRADE);
		msg.addPcName(partner);
		sendPacket(msg);
	}
	
	public void onTradeFinish(boolean successfull)
	{
		_activeTradeList = null;
		sendPacket(new TradeDone(1));
		if (successfull)
		{
			sendPacket(SystemMessageId.YOUR_TRADE_IS_SUCCESSFUL);
		}
	}
	
	public void startTrade(Player partner)
	{
		onTradeStart(partner);
		partner.onTradeStart(this);
	}
	
	public void cancelActiveTrade()
	{
		if (_activeTradeList == null)
		{
			return;
		}
		
		final Player partner = _activeTradeList.getPartner();
		if (partner != null)
		{
			partner.onTradeCancel(this);
		}
		
		onTradeCancel(this);
	}
	
	public boolean hasManufactureShop()
	{
		return (_manufactureItems != null) && !_manufactureItems.isEmpty();
	}
	
	/**
	 * Get the manufacture items map of this player.
	 * @return the the manufacture items map
	 */
	public Map<Integer, ManufactureItem> getManufactureItems()
	{
		if (_manufactureItems == null)
		{
			synchronized (this)
			{
				_manufactureItems = Collections.synchronizedMap(new LinkedHashMap<>());
			}
		}
		
		return _manufactureItems;
	}
	
	/**
	 * Get the store name, if any.
	 * @return the store name
	 */
	public String getStoreName()
	{
		return _storeName;
	}
	
	/**
	 * Set the store name.
	 * @param name the store name to set
	 */
	public void setStoreName(String name)
	{
		_storeName = name == null ? "" : name;
	}
	
	/**
	 * @return the _buyList object of the Player.
	 */
	public TradeList getSellList()
	{
		if (_sellList == null)
		{
			_sellList = new TradeList(this);
		}
		
		return _sellList;
	}
	
	/**
	 * @return the _buyList object of the Player.
	 */
	public TradeList getBuyList()
	{
		if (_buyList == null)
		{
			_buyList = new TradeList(this);
		}
		
		return _buyList;
	}
	
	/**
	 * Set the Private Store type of the Player. <b><u>Values</u>:</b>
	 * <li>0 : STORE_PRIVATE_NONE</li>
	 * <li>1 : STORE_PRIVATE_SELL</li>
	 * <li>2 : sellmanage</li>
	 * <li>3 : STORE_PRIVATE_BUY</li>
	 * <li>4 : buymanage</li>
	 * <li>5 : STORE_PRIVATE_MANUFACTURE</li><br>
	 * @param privateStoreType
	 */
	public void setPrivateStoreType(PrivateStoreType privateStoreType)
	{
		_privateStoreType = privateStoreType;
		if (OfflineTradeConfig.OFFLINE_DISCONNECT_FINISHED && (privateStoreType == PrivateStoreType.NONE) && ((_client == null) || _client.isDetached()))
		{
			OfflineTraderTable.getInstance().removeTrader(getObjectId());
			Disconnection.of(this).storeAndDelete();
		}
	}
	
	/**
	 * <b><u>Values</u>:</b>
	 * <li>0 : STORE_PRIVATE_NONE</li>
	 * <li>1 : STORE_PRIVATE_SELL</li>
	 * <li>2 : sellmanage</li>
	 * <li>3 : STORE_PRIVATE_BUY</li>
	 * <li>4 : buymanage</li>
	 * <li>5 : STORE_PRIVATE_MANUFACTURE</li>
	 * @return the Private Store type of the Player.
	 */
	public PrivateStoreType getPrivateStoreType()
	{
		return _privateStoreType;
	}
	
	/**
	 * Set the _clan object, _clanId, _clanLeader Flag and title of the Player.
	 * @param clan
	 */
	public void setClan(Clan clan)
	{
		_clan = clan;
		if (clan == null)
		{
			setTitle("");
			_clanId = 0;
			_clanPrivileges = new ClanPrivileges();
			_pledgeType = 0;
			_powerGrade = 0;
			_lvlJoinedAcademy = 0;
			_apprentice = 0;
			_sponsor = 0;
			_activeWarehouse = null;
			return;
		}
		
		if (!clan.isMember(getObjectId()))
		{
			// char has been kicked from clan
			setClan(null);
			return;
		}
		
		_clanId = clan.getId();
	}
	
	/**
	 * @return the _clan object of the Player.
	 */
	@Override
	public Clan getClan()
	{
		return _clan;
	}
	
	/**
	 * @return True if the Player is the leader of its clan.
	 */
	public boolean isClanLeader()
	{
		return (_clan != null) && (getObjectId() == _clan.getLeaderId());
	}
	
	/**
	 * Reduce the number of arrows owned by the Player and send it Server->Client Packet InventoryUpdate or ItemList (to unequip if the last arrow was consummed).
	 */
	@Override
	protected void reduceArrowCount()
	{
		final Item arrows = _inventory.getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		if (arrows == null)
		{
			_inventory.unEquipItemInSlot(Inventory.PAPERDOLL_LHAND);
			_arrowItem = null;
			sendItemList(false);
			return;
		}
		
		// Adjust item quantity
		if (arrows.getCount() > 1)
		{
			synchronized (arrows)
			{
				arrows.changeCount(null, -1, this, null);
				arrows.setLastChange(Item.MODIFIED);
				_inventory.refreshWeight();
			}
		}
		else
		{
			// Destroy entire item and save to database
			_inventory.destroyItem(ItemProcessType.NONE, arrows, this, null);
			_inventory.unEquipItemInSlot(Inventory.PAPERDOLL_LHAND);
			_arrowItem = null;
			sendItemList(false);
			return;
		}
		
		final InventoryUpdate iu = new InventoryUpdate();
		iu.addModifiedItem(arrows);
		sendInventoryUpdate(iu);
	}
	
	/**
	 * Equip arrows needed in left hand and send a Server->Client packet ItemList to the Player then return True.
	 */
	@Override
	protected boolean checkAndEquipArrows()
	{
		// Check if nothing is equipped in left hand
		if (_inventory.getPaperdollItem(Inventory.PAPERDOLL_LHAND) == null)
		{
			// Get the Item of the arrows needed for this bow
			_arrowItem = _inventory.findArrowForBow(getActiveWeaponItem());
			if (_arrowItem != null)
			{
				// Equip arrows needed in left hand
				_inventory.setPaperdollItem(Inventory.PAPERDOLL_LHAND, _arrowItem);
				
				// Send a Server->Client packet ItemList to this Player to update left hand equipement
				sendItemList(false);
			}
		}
		else
		{
			// Get the Item of arrows equipped in left hand
			_arrowItem = _inventory.getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		}
		
		return _arrowItem != null;
	}
	
	/**
	 * Disarm the player's weapon.
	 * @return {@code true} if the player was disarmed or doesn't have a weapon to disarm, {@code false} otherwise.
	 */
	public boolean disarmWeapons()
	{
		// If there is no weapon to disarm then return true.
		final Item wpn = _inventory.getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		if (wpn == null)
		{
			return true;
		}
		
		// Don't allow disarming a cursed weapon
		if (isCursedWeaponEquipped())
		{
			return false;
		}
		
		// Don't allow disarming if the weapon is force equip.
		if (wpn.getWeaponItem().isForceEquip())
		{
			return false;
		}
		
		final List<Item> unequipped = _inventory.unEquipItemInBodySlotAndRecord(wpn.getTemplate().getBodyPart());
		final InventoryUpdate iu = new InventoryUpdate();
		for (Item itm : unequipped)
		{
			iu.addModifiedItem(itm);
		}
		
		sendInventoryUpdate(iu);
		
		abortAttack();
		broadcastUserInfo();
		
		// This can be 0 if the user pressed the right mousebutton twice very fast.
		if (!unequipped.isEmpty())
		{
			final SystemMessage sm;
			final Item unequippedItem = unequipped.get(0);
			if (unequippedItem.isEnchanted())
			{
				sm = new SystemMessage(SystemMessageId.THE_EQUIPMENT_S1_S2_HAS_BEEN_REMOVED);
				sm.addInt(unequippedItem.getEnchantLevel());
			}
			else
			{
				sm = new SystemMessage(SystemMessageId.S1_HAS_BEEN_DISARMED);
			}
			
			sm.addItemName(unequippedItem);
			sendPacket(sm);
		}
		
		return true;
	}
	
	/**
	 * Disarm the player's shield.
	 * @return {@code true}.
	 */
	public boolean disarmShield()
	{
		final Item sld = _inventory.getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		if (sld != null)
		{
			final List<Item> unequipped = _inventory.unEquipItemInBodySlotAndRecord(sld.getTemplate().getBodyPart());
			final InventoryUpdate iu = new InventoryUpdate();
			for (Item itm : unequipped)
			{
				iu.addModifiedItem(itm);
			}
			
			sendInventoryUpdate(iu);
			
			abortAttack();
			broadcastUserInfo();
			
			// this can be 0 if the user pressed the right mousebutton twice very fast
			if (!unequipped.isEmpty())
			{
				SystemMessage sm = null;
				final Item unequippedItem = unequipped.get(0);
				if (unequippedItem.isEnchanted())
				{
					sm = new SystemMessage(SystemMessageId.THE_EQUIPMENT_S1_S2_HAS_BEEN_REMOVED);
					sm.addInt(unequippedItem.getEnchantLevel());
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S1_HAS_BEEN_DISARMED);
				}
				
				sm.addItemName(unequippedItem);
				sendPacket(sm);
			}
		}
		
		return true;
	}
	
	public boolean mount(Summon pet)
	{
		// If mounts (excluding wyvern) are disallowed during siege, check if player is in a siege zone.
		if (!FeatureConfig.ALLOW_MOUNTS_DURING_SIEGE && isInsideZone(ZoneId.SIEGE))
		{
			final MountType type = MountType.findByNpcId(pet.getId());
			
			// Only allow mounting if the pet is a Strider and the player is the castle lord of the current siege castle.
			final boolean isAllowed = (type == MountType.STRIDER) && (CastleManager.getInstance().getCastle(this) != null) && isClanLeader() && (getClanId() == CastleManager.getInstance().getCastle(this).getOwnerId());
			
			if (!isAllowed)
			{
				sendMessage("You cannot mount during a siege.");
				return false;
			}
		}
		
		if (!disarmWeapons() || !disarmShield())
		{
			return false;
		}
		
		getEffectList().stopAllToggles();
		setMount(pet.getId(), pet.getLevel());
		setMountObjectID(pet.getControlObjectId());
		startFeed(pet.getId());
		broadcastPacket(new Ride(this));
		
		// Notify self and others about speed change
		broadcastUserInfo();
		
		pet.unSummon(this);
		return true;
	}
	
	public boolean mount(int npcId, int controlItemObjId, boolean useFood)
	{
		if (!disarmWeapons() || !disarmShield())
		{
			return false;
		}
		
		getEffectList().stopAllToggles();
		setMount(npcId, getLevel());
		setMountObjectID(controlItemObjId);
		broadcastPacket(new Ride(this));
		
		// Notify self and others about speed change
		broadcastUserInfo();
		if (useFood)
		{
			startFeed(npcId);
		}
		
		return true;
	}
	
	public boolean mountPlayer(Summon pet)
	{
		if ((pet != null) && pet.isMountable() && !isMounted() && !isBetrayed())
		{
			if (isDead())
			{
				// A strider cannot be ridden when dead
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.A_STRIDER_CANNOT_BE_RIDDEN_WHEN_DEAD);
				return false;
			}
			else if (pet.isDead())
			{
				// A dead strider cannot be ridden.
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.A_DEAD_STRIDER_CANNOT_BE_RIDDEN);
				return false;
			}
			else if (pet.isInCombat() || pet.isRooted())
			{
				// A strider in battle cannot be ridden
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.A_STRIDER_IN_BATTLE_CANNOT_BE_RIDDEN);
				return false;
			}
			else if (isInCombat())
			{
				// A strider cannot be ridden while in battle
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.A_STRIDER_CANNOT_BE_RIDDEN_WHILE_IN_BATTLE);
				return false;
			}
			else if (_waitTypeSitting)
			{
				// A strider can be ridden only when standing
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.A_STRIDER_CAN_BE_RIDDEN_ONLY_WHEN_STANDING);
				return false;
			}
			else if (_fishing)
			{
				// You can't mount, dismount, break and drop items while fishing
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.YOU_CANNOT_DO_THAT_WHILE_FISHING_2);
				return false;
			}
			else if (isCursedWeaponEquipped())
			{
				// no message needed, player while transformed doesn't have mount action
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			else if (pet.isHungry())
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.A_HUNGRY_STRIDER_CANNOT_BE_MOUNTED_OR_DISMOUNTED);
				return false;
			}
			else if (!LocationUtil.checkIfInRange(200, this, pet, true))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.YOU_ARE_TOO_FAR_AWAY_FROM_THE_STRIDER_TO_MOUNT_IT);
				return false;
			}
			else if (!pet.isDead() && !isMounted())
			{
				mount(pet);
				sendSkillList(); // Refresh skill list after mounting.
			}
		}
		else if (isRentedPet())
		{
			stopRentPet();
		}
		else if (isMounted())
		{
			if ((_mountType == MountType.WYVERN) && isInsideZone(ZoneId.NO_LANDING) && !isGM())
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.YOU_ARE_NOT_ALLOWED_TO_DISMOUNT_AT_THIS_LOCATION);
				return false;
			}
			else if (isHungry())
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.A_HUNGRY_STRIDER_CANNOT_BE_MOUNTED_OR_DISMOUNTED);
				return false;
			}
			else
			{
				dismount();
			}
		}
		
		return true;
	}
	
	public boolean dismount()
	{
		if (ZoneManager.getInstance().getZone(getX(), getY(), getZ() - 300, WaterZone.class) == null)
		{
			if (!isGM() && !isInWater() && (getZ() > 10000))
			{
				sendPacket(SystemMessageId.YOU_ARE_NOT_ALLOWED_TO_DISMOUNT_AT_THIS_LOCATION);
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			
			// Skip elevation check only in NoLandingZone.
			if (!isInsideZone(ZoneId.NO_LANDING) && ((GeoEngine.getInstance().getHeight(getX(), getY(), getZ()) + 300) < getZ()))
			{
				sendPacket(SystemMessageId.YOU_CANNOT_DISMOUNT_FROM_THIS_ELEVATION);
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}
		else
		{
			ThreadPool.schedule(() ->
			{
				if (isInWater())
				{
					broadcastUserInfo();
				}
			}, 1500);
		}
		
		final boolean wasFlying = isFlying();
		sendPacket(new SetupGauge(getObjectId(), 3, 0, 0));
		final int petId = _mountNpcId;
		setMount(0, 0);
		stopFeed();
		if (wasFlying)
		{
			removeSkill(CommonSkill.WYVERN_BREATH.getSkill());
		}
		
		sendSkillList(); // Refresh skill list after dismounting.
		broadcastPacket(new Ride(this));
		setMountObjectID(0);
		storePetFood(petId);
		
		// Notify self and others about speed change
		broadcastUserInfo();
		
		return true;
	}
	
	public void setUptime(long time)
	{
		_uptime = time;
	}
	
	public long getUptime()
	{
		return System.currentTimeMillis() - _uptime;
	}
	
	/**
	 * Return True if the Player is invulnerable.
	 */
	@Override
	public boolean isInvul()
	{
		return super.isInvul() || isTeleportProtected();
	}
	
	/**
	 * Return True if the Player has a Party in progress.
	 */
	@Override
	public boolean isInParty()
	{
		return _party != null;
	}
	
	/**
	 * Set the _party object of the Player (without joining it).
	 * @param party
	 */
	public void setParty(Party party)
	{
		_party = party;
	}
	
	/**
	 * Set the _party object of the Player AND join it.
	 * @param party
	 */
	public void joinParty(Party party)
	{
		if (party != null)
		{
			// First set the party otherwise this wouldn't be considered
			// as in a party into the Creature.updateEffectIcons() call.
			_party = party;
			party.addPartyMember(this);
		}
	}
	
	/**
	 * Manage the Leave Party task of the Player.
	 */
	public void leaveParty()
	{
		if (isInParty())
		{
			_party.removePartyMember(this, PartyMessageType.DISCONNECTED);
			_party = null;
		}
	}
	
	/**
	 * Return the _party object of the Player.
	 */
	@Override
	public Party getParty()
	{
		return _party;
	}
	
	public void setPartyDistributionType(PartyDistributionType pdt)
	{
		_partyDistributionType = pdt;
	}
	
	public PartyDistributionType getPartyDistributionType()
	{
		return _partyDistributionType;
	}
	
	/**
	 * Return True if the Player is a GM.
	 */
	@Override
	public boolean isGM()
	{
		return getAccessLevel().isGm();
	}
	
	/**
	 * Set the _accessLevel of the Player.
	 * @param level
	 */
	public void setAccessLevel(int level)
	{
		_accessLevel = AdminData.getInstance().getAccessLevel(level);
		_appearance.setNameColor(_accessLevel.getNameColor());
		_appearance.setTitleColor(_accessLevel.getTitleColor());
		broadcastUserInfo();
		
		CharInfoTable.getInstance().addName(this);
		
		if (!AdminData.getInstance().hasAccessLevel(level))
		{
			LOGGER.warning("Tried to set unregistered access level " + level + " for " + this + ". Setting access level without privileges!");
		}
		else if (level > 0)
		{
			LOGGER.warning(_accessLevel.getName() + " access level set for character " + getName() + "! Just a warning to be careful ;)");
		}
	}
	
	public void setAccountAccesslevel(int level)
	{
		LoginServerThread.getInstance().sendAccessLevel(getAccountName(), level);
	}
	
	/**
	 * @return the _accessLevel of the Player.
	 */
	@Override
	public AccessLevel getAccessLevel()
	{
		if (GeneralConfig.EVERYBODY_HAS_ADMIN_RIGHTS)
		{
			return AdminData.getInstance().getMasterAccessLevel();
		}
		
		if (_accessLevel == null)
		{
			setAccessLevel(0);
		}
		
		return _accessLevel;
	}
	
	/**
	 * Update Stats of the Player client side by sending Server->Client packet UserInfo/StatusUpdate to this Player and CharInfo/StatusUpdate to all known players (broadcast).
	 */
	public void updateAndBroadcastStatus()
	{
		if (_updateAndBroadcastStatusTask == null)
		{
			_updateAndBroadcastStatusTask = ThreadPool.schedule(() ->
			{
				refreshOverloaded();
				refreshExpertisePenalty();
				
				// Send a Server->Client packet UserInfo to this Player and CharInfo to all known players (broadcast)
				broadcastUserInfo();
				
				_updateAndBroadcastStatusTask = null;
			}, 50);
		}
	}
	
	/**
	 * Send a Server->Client StatusUpdate packet with Karma and PvP Flag to the Player and all Player to inform (broadcast).
	 */
	public void setKarmaFlag()
	{
		updateUserInfo();
		World.getInstance().forEachVisibleObject(this, Player.class, player ->
		{
			player.sendPacket(new RelationChanged(this, getRelation(player), isAutoAttackable(player)));
			if (hasSummon())
			{
				player.sendPacket(new RelationChanged(_summon, getRelation(player), isAutoAttackable(player)));
			}
		});
	}
	
	/**
	 * Send a Server->Client StatusUpdate packet with Karma to the Player and all Player to inform (broadcast).
	 */
	public void broadcastKarma()
	{
		final StatusUpdate su = new StatusUpdate(this);
		su.addAttribute(StatusUpdate.KARMA, getKarma());
		sendPacket(su);
		
		World.getInstance().forEachVisibleObject(this, Player.class, player ->
		{
			player.sendPacket(new RelationChanged(this, getRelation(player), isAutoAttackable(player)));
			if (hasSummon())
			{
				player.sendPacket(new RelationChanged(_summon, getRelation(player), isAutoAttackable(player)));
			}
		});
	}
	
	/**
	 * Set the online Flag to True or False and update the characters table of the database with online status and lastAccess (called when login and logout).
	 * @param isOnline
	 * @param updateInDb
	 */
	public void setOnlineStatus(boolean isOnline, boolean updateInDb)
	{
		if (_isOnline != isOnline)
		{
			_isOnline = isOnline;
		}
		
		// Update the characters table of the database with online status and lastAccess (called when login and logout)
		if (updateInDb)
		{
			updateOnlineStatus();
		}
	}
	
	public void setIn7sDungeon(boolean isIn7sDungeon)
	{
		_isIn7sDungeon = isIn7sDungeon;
	}
	
	/**
	 * Update the characters table of the database with online status and lastAccess of this Player (called when login and logout).
	 */
	public void updateOnlineStatus()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE characters SET online=?, lastAccess=? WHERE charId=?"))
		{
			ps.setInt(1, isOnlineInt());
			ps.setLong(2, System.currentTimeMillis());
			ps.setInt(3, getObjectId());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Failed updating character online status.", e);
		}
	}
	
	/**
	 * Create a new player in the characters table of the database.
	 * @return
	 */
	private boolean createDb()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(INSERT_CHARACTER))
		{
			ps.setString(1, _accountName);
			ps.setInt(2, getObjectId());
			ps.setString(3, getName());
			ps.setInt(4, getLevel());
			ps.setInt(5, getMaxHp());
			ps.setDouble(6, getCurrentHp());
			ps.setInt(7, getMaxCp());
			ps.setDouble(8, getCurrentCp());
			ps.setInt(9, getMaxMp());
			ps.setDouble(10, getCurrentMp());
			ps.setInt(11, _appearance.getFace());
			ps.setInt(12, _appearance.getHairStyle());
			ps.setInt(13, _appearance.getHairColor());
			ps.setInt(14, _appearance.isFemale() ? 1 : 0);
			ps.setLong(15, getExp());
			ps.setLong(16, getSp());
			ps.setInt(17, getKarma());
			ps.setInt(18, _fame);
			ps.setInt(19, _pvpKills);
			ps.setInt(20, _pkKills);
			ps.setInt(21, _clanId);
			ps.setInt(22, getRace().ordinal());
			ps.setInt(23, getPlayerClass().getId());
			ps.setLong(24, _deleteTimer);
			ps.setInt(25, hasDwarvenCraft() ? 1 : 0);
			ps.setString(26, getTitle());
			ps.setInt(27, _appearance.getTitleColor());
			ps.setInt(28, getAccessLevel().getLevel());
			ps.setInt(29, isOnlineInt());
			ps.setInt(30, _isIn7sDungeon ? 1 : 0);
			ps.setInt(31, _clanPrivileges.getMask());
			ps.setInt(32, _wantsPeace);
			ps.setInt(33, _baseClass);
			ps.setInt(34, _newbie ? 1 : 0);
			ps.setInt(35, _noble ? 1 : 0);
			ps.setLong(36, 0);
			ps.setTimestamp(37, new Timestamp(_createDate.getTimeInMillis()));
			ps.setLong(38, System.currentTimeMillis()); // lastAccess
			ps.setLong(39, System.currentTimeMillis()); // last_recom_date
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not insert char data: " + e.getMessage(), e);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Retrieve a Player from the characters table of the database and add it in _allObjects of the L2world. <b><u>Actions</u>:</b>
	 * <li>Retrieve the Player from the characters table of the database</li>
	 * <li>Add the Player object in _allObjects</li>
	 * <li>Set the x,y,z position of the Player and make it invisible</li>
	 * <li>Update the overloaded status of the Player</li><br>
	 * @param objectId Identifier of the object to initialized
	 * @return The Player loaded from the database
	 */
	private static Player restore(int objectId)
	{
		Player player = null;
		double currentCp = 0;
		double currentHp = 0;
		double currentMp = 0;
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(RESTORE_CHARACTER))
		{
			// Retrieve the Player from the characters table of the database
			ps.setInt(1, objectId);
			try (ResultSet rset = ps.executeQuery())
			{
				if (rset.next())
				{
					final int activeClassId = rset.getInt("classid");
					final boolean female = rset.getInt("sex") != Sex.MALE.ordinal();
					final PlayerTemplate template = PlayerTemplateData.getInstance().getTemplate(activeClassId);
					final PlayerAppearance app = new PlayerAppearance(rset.getByte("face"), rset.getByte("hairColor"), rset.getByte("hairStyle"), female);
					player = new Player(objectId, template, rset.getString("account_name"), app);
					player.setName(rset.getString("char_name"));
					player._lastAccess = rset.getLong("lastAccess");
					final PlayerStat stat = player.getStat();
					stat.setExp(rset.getLong("exp"));
					player.setExpBeforeDeath(rset.getLong("expBeforeDeath"));
					stat.setLevel(rset.getByte("level"));
					stat.setSp(rset.getLong("sp"));
					
					player.setWantsPeace(rset.getInt("wantspeace"));
					
					player.setHeading(rset.getInt("heading"));
					
					player.setKarma(rset.getInt("karma"));
					player.setFame(rset.getInt("fame"));
					player.setPvpKills(rset.getInt("pvpkills"));
					player.setPkKills(rset.getInt("pkkills"));
					player.setOnlineTime(rset.getLong("onlinetime"));
					player.setNewbie(rset.getInt("newbie") == 1);
					player.setNoble(rset.getInt("nobless") == 1);
					
					final int factionId = rset.getInt("faction");
					if (factionId == 1)
					{
						player.setGood();
					}
					
					if (factionId == 2)
					{
						player.setEvil();
					}
					
					player.setClanJoinExpiryTime(rset.getLong("clan_join_expiry_time"));
					if (player.getClanJoinExpiryTime() < System.currentTimeMillis())
					{
						player.setClanJoinExpiryTime(0);
					}
					
					player.setClanCreateExpiryTime(rset.getLong("clan_create_expiry_time"));
					if (player.getClanCreateExpiryTime() < System.currentTimeMillis())
					{
						player.setClanCreateExpiryTime(0);
					}
					
					player.setPcCafePoints(rset.getInt("pccafe_points"));
					player.setPowerGrade(rset.getInt("power_grade"));
					player.setPledgeType(rset.getInt("subpledge"));
					// player.setApprentice(rs.getInt("apprentice"));
					player.setDeleteTimer(rset.getLong("deletetime"));
					player.setTitle(rset.getString("title"));
					player.setAccessLevel(rset.getInt("accesslevel"));
					final int titleColor = rset.getInt("title_color");
					if (titleColor != PlayerAppearance.DEFAULT_TITLE_COLOR)
					{
						player.getAppearance().setTitleColor(titleColor);
					}
					
					player.setFistsWeaponItem(player.findFistsWeaponItem(activeClassId));
					player.setUptime(System.currentTimeMillis());
					
					currentHp = rset.getDouble("curHp");
					currentCp = rset.getDouble("curCp");
					currentMp = rset.getDouble("curMp");
					
					// Check recommendations
					player.setLastRecomUpdate(rset.getLong("last_recom_date"));
					player.checkRecom(rset.getInt("rec_have"), rset.getInt("rec_left"));
					player._classIndex = 0;
					try
					{
						player.setBaseClass(rset.getInt("base_class"));
					}
					catch (Exception e)
					{
						// TODO: Should this be logged?
						player.setBaseClass(activeClassId);
					}
					
					// Restore Subclass Data (cannot be done earlier in function)
					if (restoreSubClassData(player) && (activeClassId != player.getBaseClass()))
					{
						for (SubClassHolder subClass : player.getSubClasses().values())
						{
							if (subClass.getId() == activeClassId)
							{
								player._classIndex = subClass.getClassIndex();
							}
						}
					}
					
					if ((player.getClassIndex() == 0) && (activeClassId != player.getBaseClass()))
					{
						// Subclass in use but doesn't exist in DB -
						// a possible restart-while-modifysubclass cheat has been attempted.
						// Switching to use base class
						player.setPlayerClass(player.getBaseClass());
						LOGGER.warning(player + " reverted to base class. Possibly has tried a relogin exploit while subclassing.");
					}
					else
					{
						player._activeClass = activeClassId;
					}
					
					player.setApprentice(rset.getInt("apprentice"));
					player.setSponsor(rset.getInt("sponsor"));
					player.setLvlJoinedAcademy(rset.getInt("lvl_joined_academy"));
					player.setIn7sDungeon(rset.getInt("isin7sdungeon") == 1);
					CursedWeaponsManager.getInstance().checkPlayer(player);
					
					player.setDeathPenaltyBuffLevel(rset.getInt("death_penalty_level"));
					
					player.setVitalityPoints(rset.getInt("vitality_points"), true);
					
					// Set the x,y,z position of the Player and make it invisible
					final int x = rset.getInt("x");
					final int y = rset.getInt("y");
					final int z = GeoEngine.getInstance().getHeight(x, y, rset.getInt("z"));
					player.setXYZInvisible(x, y, z);
					player.setLastServerPosition(x, y, z);
					
					// character creation Time
					player.getCreateDate().setTimeInMillis(rset.getTimestamp("createDate").getTime());
					
					// Language
					player.setLang(rset.getString("language"));
					
					// Set Hero status if it applies
					player.setHero(Hero.getInstance().isHero(objectId));
					
					final int clanId = rset.getInt("clanid");
					Clan clan = null;
					if (clanId > 0)
					{
						clan = ClanTable.getInstance().getClan(clanId);
						player.setClan(clan);
						if ((clan != null) && clan.isMember(objectId))
						{
							if (clan.getLeaderId() != player.getObjectId())
							{
								if (player.getPowerGrade() == 0)
								{
									player.setPowerGrade(5);
								}
								
								player.setClanPrivileges(clan.getRankPrivs(player.getPowerGrade()));
							}
							else
							{
								player.getClanPrivileges().enableAll();
								player.setPowerGrade(1);
							}
							
							player.setPledgeClass(ClanMember.calculatePledgeClass(player));
						}
					}
					
					if (clan == null)
					{
						if (player.isNoble())
						{
							player.setPledgeClass(5);
						}
						
						if (player.isHero())
						{
							player.setPledgeClass(8);
						}
						
						player.getClanPrivileges().disableAll();
					}
					
					// Retrieve the name and ID of the other characters assigned to this account.
					try (PreparedStatement stmt = con.prepareStatement("SELECT charId, char_name FROM characters WHERE account_name=? AND charId<>?"))
					{
						stmt.setString(1, player._accountName);
						stmt.setInt(2, objectId);
						try (ResultSet chars = stmt.executeQuery())
						{
							while (chars.next())
							{
								player._chars.put(chars.getInt("charId"), chars.getString("char_name"));
							}
						}
					}
				}
			}
			
			if (player == null)
			{
				return null;
			}
			
			// Retrieve from the database all items of this Player and add them to _inventory
			player.getInventory().restore();
			player.getWarehouse().restore();
			player.getFreight().restore();
			
			// Retrieve from the database all secondary data of this Player
			// Note that Clan, Noblesse and Hero skills are given separately and not here.
			// Retrieve from the database all skills of this Player and add them to _skills
			player.restoreCharData();
			
			// Reward auto-get skills and all available skills if auto-learn skills is true.
			player.rewardSkills();
			
			player.restoreItemReuse();
			
			// Restore current Cp, HP and MP values
			player.setCurrentCp(currentCp);
			player.setCurrentHp(currentHp);
			player.setCurrentMp(currentMp);
			
			player.setOriginalCpHpMp(currentCp, currentHp, currentMp);
			if (currentHp < 0.5)
			{
				player.setDead(true);
				player.stopHpMpRegeneration();
			}
			
			// Restore pet if exists in the world
			player.setPet(World.getInstance().getPet(player.getObjectId()));
			if (player.hasSummon())
			{
				player.getSummon().setOwner(player);
			}
			
			// Update the overloaded status of the Player
			player.refreshOverloaded();
			
			// Update the expertise status of the Player
			player.refreshExpertisePenalty();
			
			player.restoreFriendList();
			
			player.setOnlineStatus(true, false);
			
			PlayerAutoSaveTaskManager.getInstance().add(player);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Failed loading character.", e);
		}
		
		return player;
	}
	
	public int getMailPosition()
	{
		return _mailPosition;
	}
	
	public void setMailPosition(int mailPosition)
	{
		_mailPosition = mailPosition;
	}
	
	public int getMailForumPosition()
	{
		final Forum mailForum = getForumMail();
		if (mailForum == null)
		{
			return 0;
		}
		
		return mailForum.getTopicSize();
	}
	
	public Forum getForumMail()
	{
		if (_forumMail == null)
		{
			setForumMail(ForumsBBSManager.getInstance().getForumByName("MailRoot").getChildByName(getName()));
			
			if (_forumMail == null)
			{
				ForumsBBSManager.getInstance().createNewForum(getName(), ForumsBBSManager.getInstance().getForumByName("MailRoot"), Forum.MAIL, Forum.OWNERONLY, getObjectId());
				setForumMail(ForumsBBSManager.getInstance().getForumByName("MailRoot").getChildByName(getName()));
			}
		}
		
		return _forumMail;
	}
	
	public void setForumMail(Forum forum)
	{
		_forumMail = forum;
	}
	
	public Forum getMemo()
	{
		if (_forumMemo == null)
		{
			setMemo(ForumsBBSManager.getInstance().getForumByName("MemoRoot").getChildByName(_accountName));
			
			if (_forumMemo == null)
			{
				ForumsBBSManager.getInstance().createNewForum(_accountName, ForumsBBSManager.getInstance().getForumByName("MemoRoot"), Forum.MEMO, Forum.OWNERONLY, getObjectId());
				setMemo(ForumsBBSManager.getInstance().getForumByName("MemoRoot").getChildByName(_accountName));
			}
		}
		
		return _forumMemo;
	}
	
	public void setMemo(Forum forum)
	{
		_forumMemo = forum;
	}
	
	/**
	 * Restores sub-class data for the Player, used to check the current class index for the character.
	 * @param player
	 * @return
	 */
	private static boolean restoreSubClassData(Player player)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(RESTORE_CHAR_SUBCLASSES))
		{
			ps.setInt(1, player.getObjectId());
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					final SubClassHolder subClass = new SubClassHolder();
					subClass.setPlayerClass(rs.getInt("class_id"));
					subClass.setLevel(rs.getByte("level"));
					subClass.setExp(rs.getLong("exp"));
					subClass.setSp(rs.getLong("sp"));
					subClass.setClassIndex(rs.getInt("class_index"));
					
					// Enforce the correct indexing of _subClasses against their class indexes.
					player.getSubClasses().put(subClass.getClassIndex(), subClass);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not restore classes for " + player.getName() + ": " + e.getMessage(), e);
		}
		
		return true;
	}
	
	/**
	 * Restores:
	 * <ul>
	 * <li>Skills</li>
	 * <li>Macros</li>
	 * <li>Short-cuts</li>
	 * <li>Henna</li>
	 * <li>Teleport Bookmark</li>
	 * <li>Recipe Book</li>
	 * <li>Recipe Shop List (If configuration enabled)</li>
	 * <li>Premium Item List</li>
	 * <li>Pet Inventory Items</li>
	 * </ul>
	 */
	private void restoreCharData()
	{
		// Retrieve from the database all skills of this Player and add them to _skills.
		restoreSkills();
		
		// Retrieve from the database all macroses of this Player and add them to _macros.
		_macros.restoreMe();
		
		// Retrieve from the database all shortcuts of this Player and add them to _shortcuts.
		_shortcuts.restoreMe();
		
		// Retrieve from the database all henna of this Player and add them to _henna.
		restoreHenna();
		
		// Retrieve from the database all recom data of this Player and add to _recomChars.
		if (!PlayerConfig.ALT_RECOMMEND)
		{
			restoreRecom();
		}
		
		// Retrieve from the database the recipe book of this Player.
		restoreRecipeBook(true);
		
		// Restore Recipe Shop list.
		if (PlayerConfig.STORE_RECIPE_SHOPLIST)
		{
			restoreRecipeShopList();
		}
		
		// Restore items in pet inventory.
		restorePetInventoryItems();
	}
	
	/**
	 * Restore recipe book data for this Player.
	 * @param loadCommon
	 */
	private void restoreRecipeBook(boolean loadCommon)
	{
		final String sql = loadCommon ? "SELECT id, type, classIndex FROM character_recipebook WHERE charId=?" : "SELECT id FROM character_recipebook WHERE charId=? AND classIndex=? AND type = 1";
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(sql))
		{
			ps.setInt(1, getObjectId());
			if (!loadCommon)
			{
				ps.setInt(2, _classIndex);
			}
			
			try (ResultSet rs = ps.executeQuery())
			{
				_dwarvenRecipeBook.clear();
				
				RecipeList recipe;
				final RecipeData rd = RecipeData.getInstance();
				while (rs.next())
				{
					recipe = rd.getRecipeList(rs.getInt("id"));
					if (loadCommon)
					{
						if (rs.getInt(2) == 1)
						{
							if (rs.getInt(3) == _classIndex)
							{
								registerDwarvenRecipeList(recipe, false);
							}
						}
						else
						{
							registerCommonRecipeList(recipe, false);
						}
					}
					else
					{
						registerDwarvenRecipeList(recipe, false);
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not restore recipe book data:" + e.getMessage(), e);
		}
	}
	
	/**
	 * Update Player stats in the characters table of the database.
	 * @param storeActiveEffects
	 */
	public synchronized void store(boolean storeActiveEffects)
	{
		storeCharBase();
		storeCharSub();
		storeEffect(storeActiveEffects);
		storeItemReuseDelay();
		if (PlayerConfig.STORE_RECIPE_SHOPLIST)
		{
			storeRecipeShopList();
		}
		
		SevenSigns.getInstance().saveSevenSignsData(getObjectId());
		
		final PlayerVariables vars = getScript(PlayerVariables.class);
		if (vars != null)
		{
			vars.storeMe();
		}
		
		final AccountVariables aVars = getScript(AccountVariables.class);
		if (aVars != null)
		{
			aVars.storeMe();
		}
		
		getInventory().updateDatabase();
		getWarehouse().updateDatabase();
		getFreight().updateDatabase();
	}
	
	@Override
	public void storeMe()
	{
		store(true);
	}
	
	private void storeCharBase()
	{
		// Get the exp, level, and sp of base class to store in base table
		final long exp = getStat().getBaseExp();
		final int level = getStat().getBaseLevel();
		final long sp = getStat().getBaseSp();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(UPDATE_CHARACTER))
		{
			ps.setInt(1, level);
			ps.setInt(2, getMaxHp());
			ps.setDouble(3, getCurrentHp());
			ps.setInt(4, getMaxCp());
			ps.setDouble(5, getCurrentCp());
			ps.setInt(6, getMaxMp());
			ps.setDouble(7, getCurrentMp());
			ps.setInt(8, _appearance.getFace());
			ps.setInt(9, _appearance.getHairStyle());
			ps.setInt(10, _appearance.getHairColor());
			ps.setInt(11, _appearance.isFemale() ? 1 : 0);
			ps.setInt(12, getHeading());
			ps.setInt(13, _observerMode ? _lastLoc.getX() : getX());
			ps.setInt(14, _observerMode ? _lastLoc.getY() : getY());
			ps.setInt(15, _observerMode ? _lastLoc.getZ() : getZ());
			ps.setLong(16, exp);
			ps.setLong(17, _expBeforeDeath);
			ps.setLong(18, sp);
			ps.setInt(19, getKarma());
			ps.setInt(20, _fame);
			ps.setInt(21, _pvpKills);
			ps.setInt(22, _pkKills);
			ps.setInt(23, _clanId);
			ps.setInt(24, getRace().ordinal());
			ps.setInt(25, getPlayerClass().getId());
			ps.setLong(26, _deleteTimer);
			ps.setString(27, getTitle());
			ps.setInt(28, _appearance.getTitleColor());
			ps.setInt(29, getAccessLevel().getLevel());
			ps.setInt(30, isOnlineInt());
			ps.setInt(31, _isIn7sDungeon ? 1 : 0);
			ps.setInt(32, _clanPrivileges.getMask());
			ps.setInt(33, _wantsPeace);
			ps.setInt(34, _baseClass);
			long totalOnlineTime = _onlineTime;
			if (_onlineBeginTime > 0)
			{
				totalOnlineTime += (System.currentTimeMillis() - _onlineBeginTime) / 1000;
			}
			
			ps.setLong(35, _offlineShopStart > 0 ? _onlineTime : totalOnlineTime);
			ps.setInt(36, _newbie ? 1 : 0);
			ps.setInt(37, _noble ? 1 : 0);
			ps.setInt(38, _powerGrade);
			ps.setInt(39, _pledgeType);
			ps.setInt(40, _lvlJoinedAcademy);
			ps.setLong(41, _apprentice);
			ps.setLong(42, _sponsor);
			ps.setLong(43, _clanJoinExpiryTime);
			ps.setLong(44, _clanCreateExpiryTime);
			ps.setString(45, getName());
			ps.setLong(46, _deathPenaltyBuffLevel);
			ps.setInt(47, 0); // _bookmarkSlot
			ps.setInt(48, getVitalityPoints());
			ps.setString(49, _lang);
			int factionId = 0;
			if (_isGood)
			{
				factionId = 1;
			}
			
			if (_isEvil)
			{
				factionId = 2;
			}
			
			ps.setInt(50, factionId);
			ps.setInt(51, _pcCafePoints);
			ps.setLong(52, getLastRecomUpdate());
			ps.setInt(53, getRecomHave());
			ps.setInt(54, getRecomLeft());
			ps.setInt(55, getObjectId());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not store char base data: " + this + " - " + e.getMessage(), e);
		}
	}
	
	private void storeCharSub()
	{
		if (getTotalSubClasses() <= 0)
		{
			return;
		}
		
		// TODO(Zoey76): Refactor this to use batch.
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(UPDATE_CHAR_SUBCLASS))
		{
			for (SubClassHolder subClass : getSubClasses().values())
			{
				ps.setLong(1, subClass.getExp());
				ps.setLong(2, subClass.getSp());
				ps.setInt(3, subClass.getLevel());
				ps.setInt(4, subClass.getId());
				ps.setInt(5, getObjectId());
				ps.setInt(6, subClass.getClassIndex());
				ps.addBatch();
			}
			
			ps.executeBatch();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not store sub class data for " + getName() + ": " + e.getMessage(), e);
		}
	}
	
	@Override
	public void storeEffect(boolean storeEffects)
	{
		if (!PlayerConfig.STORE_SKILL_COOLTIME)
		{
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection())
		{
			// Delete all current stored effects for char to avoid dupe
			try (PreparedStatement delete = con.prepareStatement(DELETE_SKILL_SAVE))
			{
				delete.setInt(1, getObjectId());
				delete.setInt(2, _classIndex);
				delete.execute();
			}
			
			int buffIndex = 0;
			final List<Integer> storedSkills = new ArrayList<>();
			final long currentTime = System.currentTimeMillis();
			
			// Store all effect data along with calulated remaining
			// reuse delays for matching skills. 'restore_type'= 0.
			try (PreparedStatement statement = con.prepareStatement(ADD_SKILL_SAVE))
			{
				if (storeEffects)
				{
					for (BuffInfo info : getEffectList().getEffects())
					{
						if (info == null)
						{
							continue;
						}
						
						final Skill skill = info.getSkill();
						
						// Do not save heals.
						if (skill.getAbnormalType() == AbnormalType.LIFE_FORCE_OTHERS)
						{
							continue;
						}
						
						// Toggles are skipped, unless they are necessary to be always on.
						if (skill.isToggle() && !PlayerConfig.ALT_STORE_TOGGLES)
						{
							continue;
						}
						
						// Dances and songs are not kept in retail.
						if (skill.isDance() && !PlayerConfig.ALT_STORE_DANCES)
						{
							continue;
						}
						
						if (storedSkills.contains(skill.getReuseHashCode()))
						{
							continue;
						}
						
						storedSkills.add(skill.getReuseHashCode());
						
						statement.setInt(1, getObjectId());
						statement.setInt(2, skill.getId());
						statement.setInt(3, skill.getLevel());
						statement.setInt(4, info.getTime());
						
						final TimeStamp t = getSkillReuseTimeStamp(skill.getReuseHashCode());
						statement.setLong(5, (t != null) && (currentTime < t.getStamp()) ? t.getReuse() : 0);
						statement.setLong(6, (t != null) && (currentTime < t.getStamp()) ? t.getStamp() : 0);
						statement.setInt(7, 0); // Store type 0, active buffs/debuffs.
						statement.setInt(8, _classIndex);
						statement.setInt(9, ++buffIndex);
						statement.addBatch();
					}
				}
				
				// Skills under reuse.
				for (Entry<Integer, TimeStamp> ts : getSkillReuseTimeStamps().entrySet())
				{
					final int hash = ts.getKey();
					if (storedSkills.contains(hash))
					{
						continue;
					}
					
					final TimeStamp t = ts.getValue();
					if ((t != null) && (currentTime < t.getStamp()))
					{
						storedSkills.add(hash);
						
						statement.setInt(1, getObjectId());
						statement.setInt(2, t.getSkillId());
						statement.setInt(3, t.getSkillLevel());
						statement.setInt(4, -1);
						statement.setLong(5, t.getReuse());
						statement.setLong(6, t.getStamp());
						statement.setInt(7, 1); // Restore type 1, skill reuse.
						statement.setInt(8, _classIndex);
						statement.setInt(9, ++buffIndex);
						statement.addBatch();
					}
				}
				
				statement.executeBatch();
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not store char effect data: ", e);
		}
	}
	
	private void storeItemReuseDelay()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps1 = con.prepareStatement(DELETE_ITEM_REUSE_SAVE);
			PreparedStatement ps2 = con.prepareStatement(ADD_ITEM_REUSE_SAVE))
		{
			ps1.setInt(1, getObjectId());
			ps1.execute();
			
			final long currentTime = System.currentTimeMillis();
			for (TimeStamp ts : getItemReuseTimeStamps().values())
			{
				if ((ts != null) && (currentTime < ts.getStamp()))
				{
					ps2.setInt(1, getObjectId());
					ps2.setInt(2, ts.getItemId());
					ps2.setInt(3, ts.getItemObjectId());
					ps2.setLong(4, ts.getReuse());
					ps2.setLong(5, ts.getStamp());
					ps2.addBatch();
				}
			}
			
			ps2.executeBatch();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not store char item reuse data: ", e);
		}
	}
	
	/**
	 * @return True if the Player is online.
	 */
	public boolean isOnline()
	{
		return _isOnline;
	}
	
	public int isOnlineInt()
	{
		if (_isOnline && (_client != null))
		{
			return _client.isDetached() ? 2 : 1;
		}
		
		return 0;
	}
	
	public void startOfflinePlay()
	{
		if (hasPremiumStatus() && (DualboxCheckConfig.DUALBOX_CHECK_MAX_OFFLINEPLAY_PREMIUM_PER_IP > 0) && !AntiFeedManager.getInstance().tryAddPlayer(AntiFeedManager.OFFLINE_PLAY, this, DualboxCheckConfig.DUALBOX_CHECK_MAX_OFFLINEPLAY_PREMIUM_PER_IP))
		{
			String limit = String.valueOf(AntiFeedManager.getInstance().getLimit(this, DualboxCheckConfig.DUALBOX_CHECK_MAX_OFFLINEPLAY_PER_IP));
			sendMessage("Only " + limit + " offline players allowed per IP.");
			return;
		}
		else if ((DualboxCheckConfig.DUALBOX_CHECK_MAX_OFFLINEPLAY_PER_IP > 0) && !AntiFeedManager.getInstance().tryAddPlayer(AntiFeedManager.OFFLINE_PLAY, this, DualboxCheckConfig.DUALBOX_CHECK_MAX_OFFLINEPLAY_PER_IP))
		{
			String limit = String.valueOf(AntiFeedManager.getInstance().getLimit(this, DualboxCheckConfig.DUALBOX_CHECK_MAX_OFFLINEPLAY_PER_IP));
			sendMessage("Only " + limit + " offline players allowed per IP.");
			return;
		}
		
		AntiFeedManager.getInstance().removePlayer(AntiFeedManager.GAME_ID, this);
		
		sendPacket(LeaveWorld.STATIC_PACKET);
		
		if (OfflinePlayConfig.OFFLINE_PLAY_SET_NAME_COLOR)
		{
			getAppearance().setNameColor(OfflinePlayConfig.OFFLINE_PLAY_NAME_COLOR);
		}
		
		if (!OfflinePlayConfig.OFFLINE_PLAY_ABNORMAL_EFFECTS.isEmpty())
		{
			startAbnormalVisualEffect(true, OfflinePlayConfig.OFFLINE_PLAY_ABNORMAL_EFFECTS.get(Rnd.get(OfflinePlayConfig.OFFLINE_PLAY_ABNORMAL_EFFECTS.size())));
		}
		
		broadcastUserInfo();
		
		_offlinePlay = true;
		_client.setDetached(true);
		
		if (OfflinePlayConfig.RESTORE_AUTO_PLAY_OFFLINERS)
		{
			OfflinePlayTable.getInstance().storeOfflinePlay(this);
		}
	}
	
	public boolean isOfflinePlay()
	{
		return _offlinePlay;
	}
	
	public void setOfflinePlay(boolean value)
	{
		_offlinePlay = value;
	}
	
	public void setEnteredWorld()
	{
		_enteredWorld = true;
	}
	
	public boolean hasEnteredWorld()
	{
		return _enteredWorld;
	}
	
	/**
	 * Verifies if the player is in offline mode.<br>
	 * The offline mode may happen for different reasons:<br>
	 * Abnormally: Player gets abruptly disconnected from server.<br>
	 * Normally: The player gets into offline shop mode, only available by enabling the offline shop mod.
	 * @return {@code true} if the player is in offline mode, {@code false} otherwise
	 */
	public boolean isInOfflineMode()
	{
		return (_client == null) || _client.isDetached();
	}
	
	public boolean isIn7sDungeon()
	{
		return _isIn7sDungeon;
	}
	
	@Override
	public Skill addSkill(Skill newSkill)
	{
		addCustomSkill(newSkill);
		return super.addSkill(newSkill);
	}
	
	/**
	 * Add a skill to the Player _skills and its Func objects to the calculator set of the Player and save update in the character_skills table of the database. <b><u>Concept</u>:</b> All skills own by a Player are identified in <b>_skills</b> <b><u> Actions</u>:</b>
	 * <li>Replace oldSkill by newSkill or Add the newSkill</li>
	 * <li>If an old skill has been replaced, remove all its Func objects of Creature calculator set</li>
	 * <li>Add Func objects of newSkill to the calculator set of the Creature</li><br>
	 * @param newSkill The Skill to add to the Creature
	 * @param store
	 * @return The Skill replaced or null if just added a new Skill
	 */
	public Skill addSkill(Skill newSkill, boolean store)
	{
		// Add a skill to the Player _skills and its Func objects to the calculator set of the Player
		final Skill oldSkill = addSkill(newSkill);
		
		// Add or update a Player skill in the character_skills table of the database
		if (store)
		{
			storeSkill(newSkill, oldSkill, -1);
		}
		
		return oldSkill;
	}
	
	@Override
	public Skill removeSkill(Skill skill, boolean store)
	{
		removeCustomSkill(skill);
		return store ? removeSkill(skill) : super.removeSkill(skill, true);
	}
	
	public Skill removeSkill(Skill skill, boolean store, boolean cancelEffect)
	{
		removeCustomSkill(skill);
		return store ? removeSkill(skill) : super.removeSkill(skill, cancelEffect);
	}
	
	/**
	 * Remove a skill from the Creature and its Func objects from calculator set of the Creature and save update in the character_skills table of the database. <b><u>Concept</u>:</b> All skills own by a Creature are identified in <b>_skills</b> <b><u> Actions</u>:</b>
	 * <li>Remove the skill from the Creature _skills</li>
	 * <li>Remove all its Func objects from the Creature calculator set</li> <b><u> Overridden in</u>:</b>
	 * <li>Player : Save update in the character_skills table of the database</li><br>
	 * @param skill The Skill to remove from the Creature
	 * @return The Skill removed
	 */
	public Skill removeSkill(Skill skill)
	{
		removeCustomSkill(skill);
		
		// Remove a skill from the Creature and its Func objects from calculator set of the Creature
		final Skill oldSkill = super.removeSkill(skill, true);
		if (oldSkill != null)
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement(DELETE_SKILL_FROM_CHAR))
			{
				// Remove or update a Player skill from the character_skills table of the database
				ps.setInt(1, oldSkill.getId());
				ps.setInt(2, getObjectId());
				ps.setInt(3, _classIndex);
				ps.execute();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "Error could not delete skill: " + e.getMessage(), e);
			}
		}
		
		if (isCursedWeaponEquipped())
		{
			return oldSkill;
		}
		
		if (skill != null)
		{
			for (Shortcut sc : _shortcuts.getAllShortcuts())
			{
				if ((sc != null) && (sc.getId() == skill.getId()) && (sc.getType() == ShortcutType.SKILL) && ((skill.getId() < 3080) || (skill.getId() > 3259)))
				{
					deleteShortcut(sc.getSlot(), sc.getPage());
				}
			}
		}
		
		return oldSkill;
	}
	
	/**
	 * Add or update a Player skill in the character_skills table of the database.<br>
	 * If newClassIndex > -1, the skill will be stored with that class index, not the current one.
	 * @param newSkill
	 * @param oldSkill
	 * @param newClassIndex
	 */
	private void storeSkill(Skill newSkill, Skill oldSkill, int newClassIndex)
	{
		final int classIndex = (newClassIndex > -1) ? newClassIndex : _classIndex;
		try (Connection con = DatabaseFactory.getConnection())
		{
			if ((oldSkill != null) && (newSkill != null))
			{
				try (PreparedStatement ps = con.prepareStatement(UPDATE_CHARACTER_SKILL_LEVEL))
				{
					ps.setInt(1, newSkill.getLevel());
					ps.setInt(2, oldSkill.getId());
					ps.setInt(3, getObjectId());
					ps.setInt(4, classIndex);
					ps.execute();
				}
			}
			else if (newSkill != null)
			{
				try (PreparedStatement ps = con.prepareStatement(ADD_NEW_SKILLS))
				{
					ps.setInt(1, getObjectId());
					ps.setInt(2, newSkill.getId());
					ps.setInt(3, newSkill.getLevel());
					ps.setInt(4, classIndex);
					ps.execute();
				}
			}
			// else
			// {
			// LOGGER.warning("Could not store new skill, it's null!");
			// }
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Error could not store char skills: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Adds or updates player's skills in the database.
	 * @param newSkills the list of skills to store
	 * @param newClassIndex if newClassIndex > -1, the skills will be stored for that class index, not the current one
	 */
	private void storeSkills(List<Skill> newSkills, int newClassIndex)
	{
		if (newSkills.isEmpty())
		{
			return;
		}
		
		final int classIndex = (newClassIndex > -1) ? newClassIndex : _classIndex;
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(ADD_NEW_SKILLS))
		{
			for (Skill addSkill : newSkills)
			{
				ps.setInt(1, getObjectId());
				ps.setInt(2, addSkill.getId());
				ps.setInt(3, addSkill.getLevel());
				ps.setInt(4, classIndex);
				ps.addBatch();
			}
			
			ps.executeBatch();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "Error could not store char skills: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Retrieve from the database all skills of this Player and add them to _skills.
	 */
	private void restoreSkills()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(RESTORE_SKILLS_FOR_CHAR))
		{
			// Retrieve all skills of this Player from the database
			ps.setInt(1, getObjectId());
			ps.setInt(2, _classIndex);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					final int id = rs.getInt("skill_id");
					final int level = rs.getInt("skill_level");
					
					// Create a Skill object for each record
					final Skill skill = SkillData.getInstance().getSkill(id, level);
					if (skill == null)
					{
						LOGGER.warning("Skipped null skill Id: " + id + " Level: " + level + " while restoring player skills for playerObjId: " + getObjectId());
						continue;
					}
					
					// Add the Skill object to the Creature _skills and its Func objects to the calculator set of the Creature
					addSkill(skill);
					
					if (GeneralConfig.SKILL_CHECK_ENABLE && (!isGM() || GeneralConfig.SKILL_CHECK_GM) && !SkillTreeData.getInstance().isSkillAllowed(this, skill))
					{
						PunishmentManager.handleIllegalPlayerAction(this, "Player " + getName() + " has invalid skill " + skill.getName() + " (" + skill.getId() + "/" + skill.getLevel() + "), class:" + ClassListData.getInstance().getClass(getPlayerClass()).getClassName(), IllegalActionPunishmentType.BROADCAST);
						if (GeneralConfig.SKILL_CHECK_REMOVE)
						{
							removeSkill(skill);
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not restore character " + this + " skills: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Retrieve from the database all skill effects of this Player and add them to the player.
	 */
	@Override
	public void restoreEffects()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(RESTORE_SKILL_SAVE))
		{
			ps.setInt(1, getObjectId());
			ps.setInt(2, _classIndex);
			try (ResultSet rs = ps.executeQuery())
			{
				final long currentTime = System.currentTimeMillis();
				while (rs.next())
				{
					final int remainingTime = rs.getInt("remaining_time");
					final long reuseDelay = rs.getLong("reuse_delay");
					final long systime = rs.getLong("systime");
					final int restoreType = rs.getInt("restore_type");
					final Skill skill = SkillData.getInstance().getSkill(rs.getInt("skill_id"), rs.getInt("skill_level"));
					if (skill == null)
					{
						continue;
					}
					
					final long time = systime - currentTime;
					if (time > 10)
					{
						disableSkill(skill, time);
						addTimeStamp(skill, reuseDelay, systime);
					}
					
					// Restore Type 1 The remaning skills lost effect upon logout but were still under a high reuse delay.
					if (restoreType > 0)
					{
						continue;
					}
					
					// Restore Type 0 These skill were still in effect on the character upon logout.
					// Some of which were self casted and might still have had a long reuse delay which also is restored.
					skill.applyEffects(this, this, false, remainingTime);
				}
			}
			
			// Remove previously restored skills
			try (PreparedStatement delete = con.prepareStatement(DELETE_SKILL_SAVE))
			{
				delete.setInt(1, getObjectId());
				delete.setInt(2, _classIndex);
				delete.executeUpdate();
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not restore " + this + " active effect data: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Retrieve from the database all Item Reuse Time of this Player and add them to the player.
	 */
	private void restoreItemReuse()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(RESTORE_ITEM_REUSE_SAVE);
			PreparedStatement delete = con.prepareStatement(DELETE_ITEM_REUSE_SAVE);)
		{
			ps.setInt(1, getObjectId());
			try (ResultSet rs = ps.executeQuery())
			{
				int itemId;
				long reuseDelay;
				long systime;
				boolean isInInventory;
				long remainingTime;
				final long currentTime = System.currentTimeMillis();
				while (rs.next())
				{
					itemId = rs.getInt("itemId");
					reuseDelay = rs.getLong("reuseDelay");
					systime = rs.getLong("systime");
					isInInventory = true;
					
					// Using item Id
					Item item = _inventory.getItemByItemId(itemId);
					if (item == null)
					{
						item = getWarehouse().getItemByItemId(itemId);
						isInInventory = false;
					}
					
					if ((item != null) && (item.getId() == itemId) && (item.getReuseDelay() > 0))
					{
						remainingTime = systime - currentTime;
						if (remainingTime > 10)
						{
							addTimeStampItem(item, reuseDelay, systime);
							if (isInInventory && item.isEtcItem())
							{
								final int group = item.getSharedReuseGroup();
								if (group > 0)
								{
									sendPacket(new ExUseSharedGroupItem(itemId, group, (int) remainingTime, (int) reuseDelay));
								}
							}
						}
					}
				}
			}
			
			// Delete item reuse.
			delete.setInt(1, getObjectId());
			delete.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not restore " + this + " Item Reuse data: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Retrieve from the database all Henna of this Player, add them to _henna and calculate stats of the Player.
	 */
	private void restoreHenna()
	{
		for (int i = 0; i < 3; i++)
		{
			_henna[i] = null;
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(RESTORE_CHAR_HENNAS))
		{
			ps.setInt(1, getObjectId());
			ps.setInt(2, _classIndex);
			try (ResultSet rs = ps.executeQuery())
			{
				int slot;
				int symbolId;
				while (rs.next())
				{
					slot = rs.getInt("slot");
					if ((slot < 1) || (slot > 3))
					{
						continue;
					}
					
					symbolId = rs.getInt("symbol_id");
					if (symbolId == 0)
					{
						continue;
					}
					
					_henna[slot - 1] = HennaData.getInstance().getHenna(symbolId);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Failed restoing character " + this + " hennas.", e);
		}
		
		// Calculate henna modifiers of this player.
		recalcHennaStats();
	}
	
	/**
	 * @return the number of Henna empty slot of the Player.
	 */
	public int getHennaEmptySlots()
	{
		int totalSlots = 0;
		if (getPlayerClass().level() == 1)
		{
			totalSlots = 2;
		}
		else if (getPlayerClass().level() > 1)
		{
			totalSlots = 3;
		}
		
		for (int i = 0; i < 3; i++)
		{
			if (_henna[i] != null)
			{
				totalSlots--;
			}
		}
		
		if (totalSlots <= 0)
		{
			return 0;
		}
		
		return totalSlots;
	}
	
	/**
	 * Remove a Henna of the Player, save update in the character_hennas table of the database and send Server->Client HennaInfo/UserInfo packet to this Player.
	 * @param slot
	 * @return
	 */
	public boolean removeHenna(int slot)
	{
		if ((slot < 1) || (slot > 3))
		{
			return false;
		}
		
		final Henna henna = _henna[slot - 1];
		if (henna == null)
		{
			return false;
		}
		
		_henna[slot - 1] = null;
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(DELETE_CHAR_HENNA))
		{
			statement.setInt(1, getObjectId());
			statement.setInt(2, slot);
			statement.setInt(3, _classIndex);
			statement.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Failed removing character henna.", e);
		}
		
		// Calculate Henna modifiers of this Player
		recalcHennaStats();
		
		// Send Server->Client HennaInfo packet to this Player
		sendPacket(new HennaInfo(this));
		
		// Send Server->Client UserInfo packet to this Player
		updateUserInfo();
		
		// Add the recovered dyes to the player's inventory and notify them.
		_inventory.addItem(ItemProcessType.RESTORE, henna.getDyeItemId(), henna.getCancelCount(), this, null);
		reduceAdena(ItemProcessType.FEE, henna.getCancelFee(), this, false);
		
		final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S2_S1_S);
		sm.addItemName(henna.getDyeItemId());
		sm.addInt(henna.getCancelCount());
		sendPacket(sm);
		sendPacket(SystemMessageId.THE_SYMBOL_HAS_BEEN_DELETED);
		
		// Notify to scripts
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_HENNA_REMOVE, this))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerHennaRemove(this, henna), this);
		}
		
		return true;
	}
	
	/**
	 * Add a Henna to the Player, save update in the character_hennas table of the database and send Server->Client HennaInfo/UserInfo packet to this Player.
	 * @param henna the henna to add to the player.
	 * @return {@code true} if the henna is added to the player, {@code false} otherwise.
	 */
	public boolean addHenna(Henna henna)
	{
		for (int i = 0; i < 3; i++)
		{
			if (_henna[i] == null)
			{
				_henna[i] = henna;
				
				// Calculate Henna modifiers of this Player
				recalcHennaStats();
				
				try (Connection con = DatabaseFactory.getConnection();
					PreparedStatement ps = con.prepareStatement(ADD_CHAR_HENNA))
				{
					ps.setInt(1, getObjectId());
					ps.setInt(2, henna.getDyeId());
					ps.setInt(3, i + 1);
					ps.setInt(4, _classIndex);
					ps.execute();
				}
				catch (Exception e)
				{
					LOGGER.log(Level.SEVERE, "Failed saving character henna.", e);
				}
				
				// Send Server->Client HennaInfo packet to this Player
				sendPacket(new HennaInfo(this));
				
				// Send Server->Client UserInfo packet to this Player
				updateUserInfo();
				
				// Notify to scripts
				if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_HENNA_REMOVE, this))
				{
					EventDispatcher.getInstance().notifyEventAsync(new OnPlayerHennaRemove(this, henna), this);
				}
				
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Calculate Henna modifiers of this Player.
	 */
	private void recalcHennaStats()
	{
		_hennaINT = 0;
		_hennaSTR = 0;
		_hennaCON = 0;
		_hennaMEN = 0;
		_hennaWIT = 0;
		_hennaDEX = 0;
		for (Henna h : _henna)
		{
			if (h == null)
			{
				continue;
			}
			
			_hennaINT += ((_hennaINT + h.getStatINT()) > 5) ? 5 - _hennaINT : h.getStatINT();
			_hennaSTR += ((_hennaSTR + h.getStatSTR()) > 5) ? 5 - _hennaSTR : h.getStatSTR();
			_hennaMEN += ((_hennaMEN + h.getStatMEN()) > 5) ? 5 - _hennaMEN : h.getStatMEN();
			_hennaCON += ((_hennaCON + h.getStatCON()) > 5) ? 5 - _hennaCON : h.getStatCON();
			_hennaWIT += ((_hennaWIT + h.getStatWIT()) > 5) ? 5 - _hennaWIT : h.getStatWIT();
			_hennaDEX += ((_hennaDEX + h.getStatDEX()) > 5) ? 5 - _hennaDEX : h.getStatDEX();
		}
	}
	
	/**
	 * @param slot the character inventory henna slot.
	 * @return the Henna of this Player corresponding to the selected slot.
	 */
	public Henna getHenna(int slot)
	{
		return (slot < 1) || (slot > 3) ? null : _henna[slot - 1];
	}
	
	/**
	 * @return {@code true} if player has at least 1 henna symbol, {@code false} otherwise.
	 */
	public boolean hasHennas()
	{
		for (Henna henna : _henna)
		{
			if (henna != null)
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * @return the henna holder for this player.
	 */
	public Henna[] getHennaList()
	{
		return _henna;
	}
	
	/**
	 * @return the INT Henna modifier of this Player.
	 */
	public int getHennaStatINT()
	{
		return _hennaINT;
	}
	
	/**
	 * @return the STR Henna modifier of this Player.
	 */
	public int getHennaStatSTR()
	{
		return _hennaSTR;
	}
	
	/**
	 * @return the CON Henna modifier of this Player.
	 */
	public int getHennaStatCON()
	{
		return _hennaCON;
	}
	
	/**
	 * @return the MEN Henna modifier of this Player.
	 */
	public int getHennaStatMEN()
	{
		return _hennaMEN;
	}
	
	/**
	 * @return the WIT Henna modifier of this Player.
	 */
	public int getHennaStatWIT()
	{
		return _hennaWIT;
	}
	
	/**
	 * @return the DEX Henna modifier of this Player.
	 */
	public int getHennaStatDEX()
	{
		return _hennaDEX;
	}
	
	public void autoSave()
	{
		storeMe();
		
		if (GeneralConfig.UPDATE_ITEMS_ON_CHAR_STORE)
		{
			getInventory().updateDatabase();
			getWarehouse().updateDatabase();
			getFreight().updateDatabase();
		}
	}
	
	public boolean canLogout()
	{
		if (_subclassLock)
		{
			LOGGER.warning("Player " + getName() + " tried to restart/logout during class change.");
			return false;
		}
		
		if (_activeEnchantItemId != ID_NONE)
		{
			return false;
		}
		
		if (AttackStanceTaskManager.getInstance().hasAttackStanceTask(this) && !(isGM() && GeneralConfig.GM_RESTART_FIGHTING))
		{
			sendPacket(SystemMessageId.YOU_CANNOT_EXIT_WHILE_IN_COMBAT);
			return false;
		}
		
		if (isRegisteredOnEvent())
		{
			sendMessage("A superior power doesn't allow you to leave.");
			return false;
		}
		
		// Prevent player from logging out if they are a festival participant
		// and it is in progress, otherwise notify party members that the player
		// is not longer a participant.
		if (isFestivalParticipant())
		{
			if (SevenSignsFestival.getInstance().isFestivalInitialized())
			{
				sendMessage("You cannot log out while you are a participant in a Festival.");
				return false;
			}
			
			if (isInParty())
			{
				_party.broadcastPacket(new SystemMessage(getName() + " has been removed from the upcoming Festival."));
			}
		}
		
		return true;
	}
	
	/**
	 * Return True if the Player is autoAttackable.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Check if the attacker isn't the Player Pet</li>
	 * <li>Check if the attacker is Monster</li>
	 * <li>If the attacker is a Player, check if it is not in the same party</li>
	 * <li>Check if the Player has Karma</li>
	 * <li>If the attacker is a Player, check if it is not in the same siege clan (Attacker, Defender)</li>
	 * </ul>
	 */
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		if (attacker == null)
		{
			return false;
		}
		
		// Invisible GM players should not be attackable.
		if (isInvisible() && isGM())
		{
			return false;
		}
		
		// Check if the attacker isn't the Player Pet
		if ((attacker == this) || (attacker == _summon))
		{
			return false;
		}
		
		// Friendly mobs do not attack players
		if (attacker instanceof FriendlyMob)
		{
			return false;
		}
		
		// Check if the attacker is a Monster
		if (attacker.isMonster())
		{
			return true;
		}
		
		// is AutoAttackable if both players are in the same duel and the duel is still going on
		final Player attackerPlayer = attacker.asPlayer();
		if (attacker.isPlayable() && (_duelState == Duel.DUELSTATE_DUELLING) && (getDuelId() == attackerPlayer.getDuelId()))
		{
			return true;
		}
		
		// Check if the attacker is not in the same party. NOTE: Party checks goes before oly checks in order to prevent patry member autoattack at oly.
		if (isInParty() && _party.getMembers().contains(attacker))
		{
			return false;
		}
		
		// Check if the attacker is in olympia and olympia start
		if (attacker.isPlayer() && attackerPlayer.isInOlympiadMode())
		{
			return _inOlympiadMode && _olympiadStart && (attacker.asPlayer().getOlympiadGameId() == getOlympiadGameId());
		}
		
		// Check if the attacker is in an event
		if (isOnEvent())
		{
			return isOnSoloEvent() || (getTeam() != attacker.getTeam());
		}
		
		// Check if the attacker is a Playable
		if (attacker.isPlayable())
		{
			if (isInsideZone(ZoneId.PEACE) || isInsideZone(ZoneId.NO_PVP))
			{
				return false;
			}
			
			// Get Player
			final Clan clan = getClan();
			final Clan attackerClan = attackerPlayer.getClan();
			if ((clan != null) && (attackerClan != null))
			{
				if (clan != attackerClan)
				{
					final Siege siege = SiegeManager.getInstance().getSiege(getX(), getY(), getZ());
					if (siege != null)
					{
						// Check if a siege is in progress and if attacker and the Player aren't in the Defender clan.
						if (siege.checkIsDefender(attackerClan) && siege.checkIsDefender(clan))
						{
							return false;
						}
						
						// Check if a siege is in progress and if attacker and the Player aren't in the Attacker clan.
						if (siege.checkIsAttacker(attackerClan) && siege.checkIsAttacker(clan))
						{
							// If first mid victory is achieved, attackers can attack attackers.
							final Castle castle = CastleManager.getInstance().getCastleById(_siegeSide);
							return (castle != null) && castle.isFirstMidVictory();
						}
					}
				}
				
				// Check if clan is at war
				if ((getWantsPeace() == 0) && (attackerPlayer.getWantsPeace() == 0) && !isAcademyMember() && clan.isAtWarWith(attackerPlayer.getClanId()) && attackerClan.isAtWarWith(getClanId()))
				{
					return true;
				}
			}
			
			// Check if the Player is in an arena, but NOT siege zone. NOTE: This check comes before clan/ally checks, but after party checks.
			// This is done because in arenas, clan/ally members can autoattack if they are not in party.
			if ((isInsideZone(ZoneId.PVP) && attackerPlayer.isInsideZone(ZoneId.PVP)) && !(isInsideZone(ZoneId.SIEGE) && attackerPlayer.isInsideZone(ZoneId.SIEGE)))
			{
				return true;
			}
			
			// Check if the attacker is not in the same clan
			if ((clan != null) && clan.isMember(attacker.getObjectId()))
			{
				return false;
			}
			
			// Check if the attacker is not in the same ally
			if (attacker.isPlayer() && (getAllyId() != 0) && (getAllyId() == attackerPlayer.getAllyId()))
			{
				return false;
			}
			
			// Now check again if the Player is in pvp zone, but this time at siege PvP zone, applying clan/ally checks
			if (isInsideZone(ZoneId.PVP) && attackerPlayer.isInsideZone(ZoneId.PVP) && isInsideZone(ZoneId.SIEGE) && attackerPlayer.isInsideZone(ZoneId.SIEGE))
			{
				return true;
			}
			
			if (FactionSystemConfig.FACTION_SYSTEM_ENABLED && ((isGood() && attackerPlayer.isEvil()) || (isEvil() && attackerPlayer.isGood())))
			{
				return true;
			}
		}
		
		if ((attacker instanceof Defender) && (_clan != null))
		{
			final Siege siege = SiegeManager.getInstance().getSiege(this);
			return (siege != null) && siege.checkIsAttacker(_clan);
		}
		
		if (attacker instanceof Guard)
		{
			if (FactionSystemConfig.FACTION_SYSTEM_ENABLED && FactionSystemConfig.FACTION_GUARDS_ENABLED && ((_isGood && attacker.asNpc().getTemplate().isClan(FactionSystemConfig.FACTION_EVIL_TEAM_NAME)) || (_isEvil && attacker.asNpc().getTemplate().isClan(FactionSystemConfig.FACTION_GOOD_TEAM_NAME))))
			{
				return true;
			}
			
			return (getKarma() > 0); // Guards attack only PK players.
		}
		
		// Check if the Player has Karma
		if ((getKarma() > 0) || (_pvpFlag > 0))
		{
			return true;
		}
		
		return false;
	}
	
	/**
	 * Check if the active Skill can be casted.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Check if the skill isn't toggle and is offensive</li>
	 * <li>Check if the target is in the skill cast range</li>
	 * <li>Check if the skill is Spoil type and if the target isn't already spoiled</li>
	 * <li>Check if the caster owns enought consummed Item, enough HP and MP to cast the skill</li>
	 * <li>Check if the caster isn't sitting</li>
	 * <li>Check if all skills are enabled and this skill is enabled</li>
	 * <li>Check if the caster own the weapon needed</li>
	 * <li>Check if the skill is active</li>
	 * <li>Check if all casting conditions are completed</li>
	 * <li>Notify the AI with CAST and target</li>
	 * </ul>
	 * @param skill The Skill to use
	 * @param forceUse used to force ATTACK on players
	 * @param dontMove used to prevent movement, if not in range
	 */
	@Override
	public boolean useMagic(Skill skill, boolean forceUse, boolean dontMove)
	{
		// Check if the skill is active
		if (skill.isPassive())
		{
			// just ignore the passive skill request. why does the client send it anyway ??
			// Send a Server->Client packet ActionFailed to the Player
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// ************************************* Check Casting in Progress *******************************************
		
		// If a skill is currently being used, queue this one if this is not the same
		if (isCastingNow())
		{
			// Check if new skill different from current skill in progress
			if ((_currentSkill != null) && (skill.getId() == _currentSkill.getSkillId()))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			else if (isSkillDisabled(skill))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			
			// Create a new SkillUseHolder object and queue it in the player _queuedSkill
			setQueuedSkill(skill, forceUse, dontMove);
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		setCastingNow(true);
		
		// Create a new SkillUseHolder object and set the player _currentSkill
		// This is used mainly to save & queue the button presses, since Creature has
		// _lastSkillCast which could otherwise replace it
		setCurrentSkill(skill, forceUse, dontMove);
		if (_queuedSkill != null)
		{
			setQueuedSkill(null, false, false);
		}
		
		if (!checkUseMagicConditions(skill, forceUse, dontMove))
		{
			setCastingNow(false);
			return false;
		}
		
		// Check if the target is correct and Notify the AI with CAST and target
		WorldObject target = null;
		switch (skill.getTargetType())
		{
			case AURA: // AURA, SELF should be cast even if no target has been found
			case FRONT_AURA:
			case BEHIND_AURA:
			case GROUND:
			case SELF:
			case AURA_CORPSE_MOB:
			case COMMAND_CHANNEL:
			case AURA_FRIENDLY:
			{
				target = this;
				break;
			}
			default:
			{
				// Get the first target of the list
				target = skill.getFirstOfTargetList(this);
				break;
			}
		}
		
		// Notify the AI with CAST and target
		getAI().setIntention(Intention.CAST, skill, target);
		return true;
	}
	
	private boolean checkUseMagicConditions(Skill skill, boolean forceUse, boolean dontMove)
	{
		// ************************************* Check Player State *******************************************
		
		// Abnormal effects(ex : Stun, Sleep...) are checked in Creature useMagic()
		if (isOutOfControl() || isParalyzed() || isStunned() || isSleeping())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Check if the player is dead.
		if (isDead())
		{
			// Send a Server->Client packet ActionFailed to the Player.
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Block skill usage based on Formal Wear, Cursed Weapon, or Mount.
		final int skillId = skill.getId();
		if (isWearingFormalWear()) // Check if player is wearing formal wear.
		{
			sendPacket(SystemMessageId.WHILE_DRESSED_IN_FORMAL_WEAR_YOU_CAN_T_USE_ITEMS_THAT_REQUIRE_ALL_SKILLS_AND_CASTING_OPERATIONS);
			return false;
		}
		else if (isCursedWeaponEquipped())
		{
			if ((skillId != CommonSkill.VOID_BURST.getId()) && (skillId != CommonSkill.VOID_FLOW.getId()))
			{
				return false;
			}
		}
		else if (isMounted())
		{
			switch (getMountType())
			{
				case STRIDER:
				{
					// Allow magic skills and Strider Siege Assault.
					if (!skill.isMagic() && (skillId != CommonSkill.STRIDER_SIEGE_ASSAULT.getId()))
					{
						return false;
					}
					break;
				}
				case WYVERN:
				{
					// Allow only Wyvern Breath and Wyvern Aegis.
					if ((skillId != CommonSkill.WYVERN_BREATH.getId()) && (skillId != CommonSkill.WYVERN_AEGIS.getId()))
					{
						return false;
					}
					break;
				}
			}
		}
		
		if (_fishing && !skill.hasEffectType(EffectType.FISHING, EffectType.FISHING_START))
		{
			// Only fishing skills are available
			sendPacket(SystemMessageId.ONLY_FISHING_SKILLS_MAY_BE_USED_AT_THIS_TIME);
			return false;
		}
		
		if (_observerMode)
		{
			sendPacket(SystemMessageId.OBSERVERS_CANNOT_PARTICIPATE);
			abortCast();
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Check if the caster is sitting
		if (_waitTypeSitting)
		{
			// Send a System Message to the caster
			sendPacket(SystemMessageId.YOU_CANNOT_MOVE_WHILE_SITTING);
			
			// Send a Server->Client packet ActionFailed to the Player
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Check if the skill type is toggle.
		if (skill.isToggle() && isAffectedBySkill(skill.getId()))
		{
			stopSkillEffects(SkillFinishType.REMOVED, skill.getId());
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Check if the player uses "Fake Death" skill
		// Note: do not check this before TOGGLE reset
		if (_isFakeDeath)
		{
			// Send a Server->Client packet ActionFailed to the Player
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// ************************************* Check Target *******************************************
		// Create and set a WorldObject containing the target of the skill
		WorldObject target = null;
		final TargetType sklTargetType = skill.getTargetType();
		if ((sklTargetType == TargetType.GROUND) && (_currentSkillWorldPosition == null))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		switch (sklTargetType)
		{
			// Target the player if skill type is AURA, PARTY, CLAN or SELF
			case AURA:
			case FRONT_AURA:
			case BEHIND_AURA:
			case PARTY:
			case CLAN:
			case PARTY_CLAN:
			case GROUND:
			case SELF:
			case AREA_SUMMON:
			case AURA_CORPSE_MOB:
			case COMMAND_CHANNEL:
			case AURA_FRIENDLY:
			{
				target = this;
				break;
			}
			case PET:
			case SERVITOR:
			case SUMMON:
			{
				target = _summon;
				break;
			}
			default:
			{
				target = getTarget();
				break;
			}
		}
		
		// Check the validity of the target
		if (target == null)
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Skills can be used on walls and doors only during siege.
		if (target.isDoor())
		{
			// Check if the door belongs to a castle and if the siege is in progress.
			final Door door = target.asDoor();
			if (door.isInsideZone(ZoneId.SIEGE))
			{
				final Castle castle = door.getCastle();
				if ((castle != null) && (castle.getResidenceId() > 0) && !castle.getSiege().isInProgress())
				{
					sendPacket(SystemMessageId.INVALID_TARGET);
					return false;
				}
			}
			else if (skill.getTargetType() != TargetType.UNLOCKABLE)
			{
				sendPacket(SystemMessageId.INVALID_TARGET);
				return false;
			}
		}
		
		// Are the target and the player in the same duel?
		if (_isInDuel && target.isPlayable())
		{
			// Get Player
			final Player cha = target.asPlayer();
			if (cha.getDuelId() != getDuelId())
			{
				sendMessage("You cannot do this while duelling.");
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}
		
		// ************************************* Check skill availability *******************************************
		
		// Check if this skill is enabled (ex : reuse time)
		if (isSkillDisabled(skill))
		{
			if (hasSkillReuse(skill.getReuseHashCode()))
			{
				final int remainingTime = (int) (getSkillRemainingReuseTime(skill.getReuseHashCode()) / 1000);
				final int hours = remainingTime / 3600;
				final int minutes = (remainingTime % 3600) / 60;
				final int seconds = remainingTime % 60;
				if (hours > 0)
				{
					sendMessage("There are " + hours + " hour(s), " + minutes + " minute(s), and " + seconds + " second(s) remaining in " + skill.getName() + "'s re-use time.");
				}
				else if (minutes > 0)
				{
					sendMessage("There are " + minutes + " minute(s), " + seconds + " second(s) remaining in " + skill.getName() + "'s re-use time.");
				}
				else
				{
					sendMessage("There are " + seconds + " second(s) remaining in " + skill.getName() + "'s re-use time.");
				}
			}
			else
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.S1_IS_NOT_AVAILABLE_AT_THIS_TIME_BEING_PREPARED_FOR_REUSE);
				sm.addSkillName(skill);
				sendPacket(sm);
			}
			
			return false;
		}
		
		// ************************************* Check casting conditions *******************************************
		
		// Check if all casting conditions are completed
		if (!skill.checkCondition(this, target, false))
		{
			// Send a Server->Client packet ActionFailed to the Player
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// ************************************* Check Skill Type *******************************************
		
		// Check if this is bad magic skill
		if (skill.hasNegativeEffect())
		{
			if (isInsidePeaceZone(this, target) && !getAccessLevel().allowPeaceAttack())
			{
				// If Creature or target is in a peace zone, send a system message TARGET_IN_PEACEZONE a Server->Client packet ActionFailed
				sendPacket(SystemMessageId.YOU_MAY_NOT_ATTACK_THIS_TARGET_IN_A_PEACEFUL_ZONE);
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			
			if (_inOlympiadMode && !_olympiadStart)
			{
				// if Player is in Olympia and the match isn't already start, send a Server->Client packet ActionFailed
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			
			if (isSiegeFriend(target))
			{
				sendMessage("Force attack is impossible against a temporary allied member during a siege.");
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			
			if (!target.canBeAttacked() && !getAccessLevel().allowPeaceAttack() && !target.isDoor())
			{
				// If target is not attackable, send a Server->Client packet ActionFailed
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			
			// Check for Event Mob's
			if ((target instanceof EventMonster) && ((EventMonster) target).eventSkillAttackBlocked())
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			
			// Check if a Forced ATTACK is in progress on non-attackable target
			if (!target.isAutoAttackable(this) && !forceUse)
			{
				switch (sklTargetType)
				{
					case AURA:
					case FRONT_AURA:
					case BEHIND_AURA:
					case AURA_CORPSE_MOB:
					case CLAN:
					case PARTY:
					case SELF:
					case GROUND:
					case AREA_SUMMON:
					case UNLOCKABLE:
					case AURA_FRIENDLY:
					{
						break;
					}
					default: // Send a Server->Client packet ActionFailed to the Player
					{
						sendPacket(ActionFailed.STATIC_PACKET);
						return false;
					}
				}
			}
			
			// Check if the target is in the skill cast range
			if (dontMove)
			{
				// Calculate the distance between the Player and the target
				if (sklTargetType == TargetType.GROUND)
				{
					if (!isInsideRadius2D(_currentSkillWorldPosition.getX(), _currentSkillWorldPosition.getY(), _currentSkillWorldPosition.getZ(), skill.getCastRange() + getTemplate().getCollisionRadius()))
					{
						// Send a System Message to the caster
						sendPacket(SystemMessageId.YOUR_TARGET_IS_OUT_OF_RANGE);
						
						// Send a Server->Client packet ActionFailed to the Player
						sendPacket(ActionFailed.STATIC_PACKET);
						return false;
					}
				}
				else if ((skill.getCastRange() > 0) && !isInsideRadius2D(target, skill.getCastRange() + getTemplate().getCollisionRadius()))
				{
					// Send a System Message to the caster
					sendPacket(SystemMessageId.YOUR_TARGET_IS_OUT_OF_RANGE);
					
					// Send a Server->Client packet ActionFailed to the Player
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
			}
		}
		
		// Check if the skill is a good magic, target is a monster and if force attack is set, if not then we don't want to cast.
		if ((skill.getEffectPoint() > 0) && target.isMonster() && !forceUse)
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Check if this is a Pvp skill and target isn't a non-flagged/non-karma player
		switch (sklTargetType)
		{
			case PARTY:
			case CLAN: // For such skills, checkPvpSkill() is called from Skill.getTargetList()
			case PARTY_CLAN: // For such skills, checkPvpSkill() is called from Skill.getTargetList()
			case AURA:
			case FRONT_AURA:
			case BEHIND_AURA:
			case AREA_SUMMON:
			case GROUND:
			case SELF:
			{
				break;
			}
			default:
			{
				// Verify that player can attack a player or summon
				if (target.isPlayable() && !getAccessLevel().allowPeaceAttack() && !checkPvpSkill(target, skill))
				{
					// Send a System Message to the player
					sendPacket(SystemMessageId.THAT_IS_THE_INCORRECT_TARGET);
					
					// Send a Server->Client packet ActionFailed to the player
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
			}
		}
		
		// GeoData Los Check here
		if (skill.getCastRange() > 0)
		{
			if (sklTargetType == TargetType.GROUND)
			{
				if (!GeoEngine.getInstance().canSeeTarget(this, _currentSkillWorldPosition))
				{
					sendPacket(SystemMessageId.CANNOT_SEE_TARGET);
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
			}
			else if (!GeoEngine.getInstance().canSeeTarget(this, target))
			{
				sendPacket(SystemMessageId.CANNOT_SEE_TARGET);
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}
		
		// finally, after passing all conditions
		return true;
	}
	
	public boolean isInLooterParty(int looterId)
	{
		final Player looter = World.getInstance().getPlayer(looterId);
		
		// if Player is in a CommandChannel
		if (isInParty() && _party.isInCommandChannel() && (looter != null))
		{
			return _party.getCommandChannel().getMembers().contains(looter);
		}
		
		return isInParty() && (looter != null) && _party.getMembers().contains(looter);
	}
	
	/**
	 * Check if the requested casting is a Pc->Pc skill cast and if it's a valid pvp condition
	 * @param target WorldObject instance containing the target
	 * @param skill Skill instance with the skill being casted
	 * @return {@code false} if the skill is a pvpSkill and target is not a valid pvp target, {@code true} otherwise.
	 */
	public boolean checkPvpSkill(WorldObject target, Skill skill)
	{
		if ((skill == null) || (target == null))
		{
			return false;
		}
		
		if (!target.isPlayable())
		{
			return true;
		}
		
		if (skill.isDebuff() || skill.hasEffectType(EffectType.STEAL_ABNORMAL) || skill.hasNegativeEffect())
		{
			final Player targetPlayer = target.asPlayer();
			if ((targetPlayer == null) || (this == target))
			{
				return false;
			}
			
			// Duel
			if (isInDuel() && targetPlayer.isInDuel() && (getDuelId() == targetPlayer.getDuelId()))
			{
				return true;
			}
			
			// Olympiad
			if (isInOlympiadMode() && targetPlayer.isInOlympiadMode() && (getOlympiadGameId() == targetPlayer.getOlympiadGameId()))
			{
				return true;
			}
			
			final boolean isCtrlPressed = (_currentSkill != null) && _currentSkill.isCtrlPressed();
			
			// Peace Zone
			if (target.isInsideZone(ZoneId.PEACE))
			{
				return false;
			}
			
			// PvP Skills
			if (skill.isPvPOnly() && ((targetPlayer.getPvpFlag() == 0) && (targetPlayer.getKarma() == 0)) && !isInsideZone(ZoneId.PVP) && !isInsideZone(ZoneId.SIEGE))
			{
				return false;
			}
			
			// Siege
			if (isSiegeFriend(targetPlayer))
			{
				sendMessage("Force attack is impossible against a temporary allied member during a siege.");
				return false;
			}
			
			// Party
			if ((isInParty() && targetPlayer.isInParty()) //
				&& ((getParty().getLeader() == targetPlayer.getParty().getLeader()) || ((_party.getCommandChannel() != null) && _party.getCommandChannel().containsPlayer(targetPlayer))))
			{
				return (skill.getEffectRange() > 0) && isCtrlPressed && (getTarget() == target) && skill.isDamage();
			}
			
			// You can debuff anyone except party members while in an arena...
			if (isInsideZone(ZoneId.PVP) && targetPlayer.isInsideZone(ZoneId.PVP))
			{
				return true;
			}
			
			final Clan tClan = targetPlayer.getClan();
			if ((_clan != null) && (tClan != null))
			{
				if (_clan.isAtWarWith(tClan.getId()) && tClan.isAtWarWith(_clan.getId()))
				{
					// Always return true at war
					return true;
				}
				else if ((getClanId() == targetPlayer.getClanId()) || ((getAllyId() > 0) && (getAllyId() == targetPlayer.getAllyId())))
				{
					// Check if skill can do dmg
					return (skill.getEffectRange() > 0) && isCtrlPressed && (getTarget() == target) && skill.isDamage();
				}
			}
			
			// On retail, it is impossible to debuff a "peaceful" player.
			if ((targetPlayer.getPvpFlag() == 0) && (targetPlayer.getKarma() == 0))
			{
				// Check if skill can do dmg
				return (skill.getEffectRange() > 0) && isCtrlPressed && (getTarget() == target) && skill.isDamage();
			}
			
			return (targetPlayer.getPvpFlag() > 0) || (targetPlayer.getKarma() > 0);
		}
		
		return true;
	}
	
	/**
	 * @return True if the Player is a Mage.
	 */
	public boolean isMageClass()
	{
		return getPlayerClass().isMage();
	}
	
	public boolean isMounted()
	{
		return _mountType != MountType.NONE;
	}
	
	public boolean checkLandingState()
	{
		// Check if char is in a no landing zone
		if (isInsideZone(ZoneId.NO_LANDING))
		{
			return true;
		}
		else
		// if this is a castle that is currently being sieged, and the rider is NOT a castle owner
		// he cannot land.
		// castle owner is the leader of the clan that owns the castle where the pc is
		if (isInsideZone(ZoneId.SIEGE) && ((getClan() == null) || (CastleManager.getInstance().getCastle(this) != CastleManager.getInstance().getCastleByOwner(getClan())) || (this != getClan().getLeader().getPlayer())))
		{
			return true;
		}
		
		return false;
	}
	
	// returns false if the change of mount type fails.
	public void setMount(int npcId, int npcLevel)
	{
		final MountType type = MountType.findByNpcId(npcId);
		switch (type)
		{
			case NONE: // None
			{
				if (isFlying())
				{
					removeSkill(CommonSkill.WYVERN_BREATH.getSkill().getId(), false);
					setFlying(false);
					sendSkillList();
				}
				break;
			}
			case WYVERN: // Wyvern
			{
				setFlying(true);
				addSkill(CommonSkill.WYVERN_BREATH.getSkill(), false);
				sendSkillList();
				break;
			}
		}
		
		_mountType = type;
		_mountNpcId = npcId;
		_mountLevel = npcLevel;
	}
	
	/**
	 * @return the type of Pet mounted (0 : none, 1 : Strider, 2 : Wyvern, 3: Wolf).
	 */
	public MountType getMountType()
	{
		return _mountType;
	}
	
	@Override
	public void stopAllEffects()
	{
		super.stopAllEffects();
		updateAndBroadcastStatus();
	}
	
	@Override
	public void stopAllEffectsExceptThoseThatLastThroughDeath()
	{
		super.stopAllEffectsExceptThoseThatLastThroughDeath();
		updateAndBroadcastStatus();
	}
	
	public void stopAllEffectsNotStayOnSubclassChange()
	{
		getEffectList().stopAllEffectsNotStayOnSubclassChange();
		updateAndBroadcastStatus();
	}
	
	public void stopCubics()
	{
		if (_cubics.isEmpty())
		{
			return;
		}
		
		for (Cubic cubic : _cubics.values())
		{
			cubic.stopAction();
			cubic.cancelDisappear();
		}
		
		_cubics.clear();
		broadcastUserInfo();
	}
	
	public void stopCubicsByOthers()
	{
		if (_cubics.isEmpty())
		{
			return;
		}
		
		boolean broadcast = false;
		for (Cubic cubic : _cubics.values())
		{
			if (cubic.givenByOther())
			{
				cubic.stopAction();
				cubic.cancelDisappear();
				_cubics.remove(cubic.getId());
				broadcast = true;
			}
		}
		
		if (broadcast)
		{
			broadcastUserInfo();
		}
	}
	
	/**
	 * Send a Server->Client packet UserInfo to this Player and CharInfo to all known players.
	 */
	@Override
	public void updateAbnormalEffect()
	{
		broadcastUserInfo();
	}
	
	/**
	 * Disable the Inventory and create a new task to enable it after 1.5s.
	 * @param value
	 */
	public void setInventoryBlockingStatus(boolean value)
	{
		_inventoryDisable = value;
		if (value)
		{
			ThreadPool.schedule(new InventoryEnableTask(this), 1500);
		}
	}
	
	/**
	 * @return True if the Inventory is disabled.
	 */
	public boolean isInventoryDisabled()
	{
		return _inventoryDisable;
	}
	
	/**
	 * Add a cubic to this player.
	 * @param cubicId the cubic ID
	 * @param level
	 * @param cubicPower
	 * @param cubicDelay
	 * @param cubicSkillChance
	 * @param cubicMaxCount
	 * @param cubicDuration
	 * @param givenByOther
	 * @return the old cubic for this cubic ID if any, otherwise {@code null}
	 */
	public Cubic addCubic(int cubicId, int level, double cubicPower, int cubicDelay, int cubicSkillChance, int cubicMaxCount, int cubicDuration, boolean givenByOther)
	{
		return _cubics.put(cubicId, new Cubic(this, cubicId, level, (int) cubicPower, cubicDelay, cubicSkillChance, cubicMaxCount, cubicDuration, givenByOther));
	}
	
	/**
	 * Get the player's cubics.
	 * @return the cubics
	 */
	public Map<Integer, Cubic> getCubics()
	{
		return _cubics;
	}
	
	/**
	 * Get the player cubic by cubic ID, if any.
	 * @param cubicId the cubic ID
	 * @return the cubic with the given cubic ID, {@code null} otherwise
	 */
	public Cubic getCubicById(int cubicId)
	{
		return _cubics.get(cubicId);
	}
	
	/**
	 * @return the modifier corresponding to the Enchant Effect of the Active Weapon (Min : 127).
	 */
	public int getEnchantEffect()
	{
		final Item wpn = getActiveWeaponInstance();
		return wpn == null ? 0 : Math.min(127, wpn.getEnchantLevel());
	}
	
	/**
	 * Set the _lastFolkNpc of the Player corresponding to the last Folk wich one the player talked.
	 * @param folkNpc
	 */
	public void setLastFolkNPC(Npc folkNpc)
	{
		_lastFolkNpc = folkNpc;
	}
	
	/**
	 * @return the _lastFolkNpc of the Player corresponding to the last Folk wich one the player talked.
	 */
	public Npc getLastFolkNPC()
	{
		return _lastFolkNpc;
	}
	
	/**
	 * @return True if Player is a participant in the Festival of Darkness.
	 */
	public boolean isFestivalParticipant()
	{
		return SevenSignsFestival.getInstance().isParticipant(this);
	}
	
	public void addAutoSoulShot(int itemId)
	{
		_activeSoulShots.add(itemId);
	}
	
	public boolean removeAutoSoulShot(int itemId)
	{
		return _activeSoulShots.remove(itemId);
	}
	
	public Set<Integer> getAutoSoulShot()
	{
		return _activeSoulShots;
	}
	
	@Override
	public void rechargeShots(boolean physical, boolean magic)
	{
		Item item;
		IItemHandler handler;
		if ((_activeSoulShots == null) || _activeSoulShots.isEmpty())
		{
			return;
		}
		
		for (int itemId : _activeSoulShots)
		{
			item = _inventory.getItemByItemId(itemId);
			if (item != null)
			{
				if (magic && (item.getTemplate().getDefaultAction() == ActionType.SPIRITSHOT))
				{
					handler = ItemHandler.getInstance().getHandler(item.getEtcItem());
					if (handler != null)
					{
						handler.onItemUse(this, item, false);
					}
				}
				
				if (physical && (item.getTemplate().getDefaultAction() == ActionType.SOULSHOT))
				{
					handler = ItemHandler.getInstance().getHandler(item.getEtcItem());
					if (handler != null)
					{
						handler.onItemUse(this, item, false);
					}
				}
			}
			else
			{
				removeAutoSoulShot(itemId);
			}
		}
	}
	
	/**
	 * Cancel autoshot for all shots matching crystaltype {@link ItemTemplate#getCrystalType()}.
	 * @param crystalType int type to disable
	 */
	public void disableAutoShotByCrystalType(int crystalType)
	{
		for (int itemId : _activeSoulShots)
		{
			if (ItemData.getInstance().getTemplate(itemId).getCrystalType().getLevel() == crystalType)
			{
				disableAutoShot(itemId);
			}
		}
	}
	
	/**
	 * Cancel autoshot use for shot itemId
	 * @param itemId int id to disable
	 * @return true if canceled.
	 */
	public boolean disableAutoShot(int itemId)
	{
		if (_activeSoulShots.contains(itemId))
		{
			removeAutoSoulShot(itemId);
			sendPacket(new ExAutoSoulShot(itemId, 0));
			
			final SystemMessage sm = new SystemMessage(SystemMessageId.THE_AUTOMATIC_USE_OF_S1_HAS_BEEN_DEACTIVATED);
			sm.addItemName(itemId);
			sendPacket(sm);
			return true;
		}
		
		return false;
	}
	
	/**
	 * Cancel all autoshots for player
	 */
	public void disableAutoShotsAll()
	{
		for (int itemId : _activeSoulShots)
		{
			sendPacket(new ExAutoSoulShot(itemId, 0));
			final SystemMessage sm = new SystemMessage(SystemMessageId.THE_AUTOMATIC_USE_OF_S1_HAS_BEEN_DEACTIVATED);
			sm.addItemName(itemId);
			sendPacket(sm);
		}
		
		_activeSoulShots.clear();
	}
	
	public ClanPrivileges getClanPrivileges()
	{
		return _clanPrivileges;
	}
	
	public void setClanPrivileges(ClanPrivileges clanPrivileges)
	{
		_clanPrivileges = clanPrivileges.clone();
	}
	
	public boolean hasAccess(ClanAccess access)
	{
		return _clanPrivileges.hasMinimumPrivileges(access);
	}
	
	// baron etc
	public void setPledgeClass(int id)
	{
		_pledgeClass = id;
		checkItemRestriction();
	}
	
	public int getPledgeClass()
	{
		return _pledgeClass;
	}
	
	public void setPledgeType(int typeId)
	{
		_pledgeType = typeId;
	}
	
	@Override
	public int getPledgeType()
	{
		return _pledgeType;
	}
	
	public int getApprentice()
	{
		return _apprentice;
	}
	
	public void setApprentice(int apprenticeId)
	{
		_apprentice = apprenticeId;
	}
	
	public int getSponsor()
	{
		return _sponsor;
	}
	
	public void setSponsor(int sponsorId)
	{
		_sponsor = sponsorId;
	}
	
	@Override
	public void sendMessage(String message)
	{
		sendPacket(new SystemMessage(SendMessageLocalisationData.getLocalisation(this, message)));
	}
	
	/**
	 * Sends a system message to the player.
	 * <p>
	 * If the GM startup builder hide configuration is enabled, the message will be sent using a localized say packet. Otherwise, the message will be sent using the standard sendMessage method.
	 * </p>
	 * @param message the message to send to the player.
	 */
	public void sendSysMessage(String message)
	{
		if (GeneralConfig.GM_STARTUP_BUILDER_HIDE)
		{
			sendPacket(new CreatureSay(null, ChatType.GENERAL, "SYS", SendMessageLocalisationData.getLocalisation(this, message)));
		}
		else
		{
			sendMessage(message);
		}
	}
	
	/**
	 * Toggles the hiding state for GM characters.
	 * <p>
	 * Only applicable for GM characters. This method sets the player's invisibility, invulnerability and silence mode based on the provided hiding state. It also updates the player's abnormal visual effects and broadcasts user info.
	 * </p>
	 * @param hide {@code true} to enable hiding (invisibility, silence, invulnerability), {@code false} to disable hiding.
	 * @return {@code true} if the hiding state was changed, {@code false} if the state was already set or the player is not a GM.
	 */
	public boolean setHiding(boolean hide)
	{
		if (!isGM())
		{
			return false;
		}
		
		if (hasEnteredWorld())
		{
			if (isInvisible() && hide)
			{
				// Already hiding.
				return false;
			}
			
			if (!isInvisible() && !hide)
			{
				// Already visible.
				return false;
			}
		}
		
		setSilenceMode(hide);
		setInvul(hide);
		setInvisible(hide);
		
		if (hide && hasEnteredWorld())
		{
			World.getInstance().forEachVisibleObject(this, Player.class, player ->
			{
				if (player.getTarget() == this)
				{
					player.setTarget(null);
				}
			});
		}
		
		broadcastUserInfo();
		return true;
	}
	
	public void enterObserverMode(Location loc)
	{
		setLastLocation();
		
		// Remove Hide.
		getEffectList().stopSkillEffects(SkillFinishType.REMOVED, AbnormalType.HIDE);
		_observerMode = true;
		stopCubics();
		setTarget(null);
		setParalyzed(true);
		startParalyze();
		setInvul(true);
		setInvisible(true);
		sendPacket(new ObservationEnter(loc));
		teleToLocation(loc, false);
		broadcastUserInfo();
	}
	
	public void setLastLocation()
	{
		_lastLoc.setXYZ(getX(), getY(), getZ());
	}
	
	public void unsetLastLocation()
	{
		_lastLoc.setXYZ(0, 0, 0);
	}
	
	public void enterOlympiadObserverMode(Location loc, int id, boolean storeCoords)
	{
		if (hasSummon())
		{
			_summon.unSummon(this);
		}
		
		// Remove Hide.
		getEffectList().stopSkillEffects(SkillFinishType.REMOVED, AbnormalType.HIDE);
		if (!_cubics.isEmpty())
		{
			for (Cubic cubic : _cubics.values())
			{
				cubic.stopAction();
				cubic.cancelDisappear();
			}
			
			_cubics.clear();
		}
		
		if (_party != null)
		{
			_party.removePartyMember(this, PartyMessageType.EXPELLED);
		}
		
		_olympiadGameId = id;
		if (_waitTypeSitting)
		{
			standUp();
		}
		
		if (storeCoords)
		{
			setLastLocation();
		}
		
		_observerMode = true;
		setTarget(null);
		setInvul(true);
		setInvisible(true);
		teleToLocation(loc, 0);
		sendPacket(new ExOlympiadMode(3));
		broadcastUserInfo();
	}
	
	public void leaveObserverMode()
	{
		setTarget(null);
		
		teleToLocation(_lastLoc, false);
		unsetLastLocation();
		sendPacket(new ObservationExit(getLocation()));
		setParalyzed(false);
		if (!isGM())
		{
			setInvisible(false);
			setInvul(false);
		}
		
		if (hasAI())
		{
			getAI().setIntention(Intention.IDLE);
		}
		
		stopPvPFlag();
		setFalling(); // prevent receive falling damage
		_observerMode = false;
		broadcastUserInfo();
	}
	
	public void leaveOlympiadObserverMode()
	{
		if (_olympiadGameId == -1)
		{
			return;
		}
		
		Olympiad.removeSpectator(_olympiadGameId, this);
		_olympiadGameId = -1;
		_observerMode = false;
		setTarget(null);
		sendPacket(new ExOlympiadMode(0));
		setInstanceId(0);
		teleToLocation(_lastLoc, true);
		if (!isGM())
		{
			setInvisible(false);
			setInvul(false);
		}
		
		if (hasAI())
		{
			getAI().setIntention(Intention.IDLE);
		}
		
		unsetLastLocation();
		broadcastUserInfo();
	}
	
	public void setOlympiadSide(int i)
	{
		_olympiadSide = i;
	}
	
	public int getOlympiadSide()
	{
		return _olympiadSide;
	}
	
	public void setOlympiadGameId(int id)
	{
		_olympiadGameId = id;
	}
	
	public int getOlympiadGameId()
	{
		return _olympiadGameId;
	}
	
	/**
	 * Gets the player's olympiad buff count.
	 * @return the olympiad's buff count
	 */
	public int getOlympiadBuffCount()
	{
		return _olyBuffsCount;
	}
	
	/**
	 * Sets the player's olympiad buff count.
	 * @param buffs the olympiad's buff count
	 */
	public void setOlympiadBuffCount(int buffs)
	{
		_olyBuffsCount = buffs;
	}
	
	public Location getLastLocation()
	{
		return _lastLoc;
	}
	
	public boolean inObserverMode()
	{
		return _observerMode;
	}
	
	public int getTeleMode()
	{
		return _telemode;
	}
	
	public void setTeleMode(int mode)
	{
		_telemode = mode;
	}
	
	public void setLoto(int i, int value)
	{
		_loto[i] = value;
	}
	
	public int getLoto(int i)
	{
		return _loto[i];
	}
	
	public void setRaceTicket(int i, int value)
	{
		_raceTickets[i] = value;
	}
	
	public int getRaceTicket(int i)
	{
		return _raceTickets[i];
	}
	
	public boolean getMessageRefusal()
	{
		return _messageRefusal;
	}
	
	public void setMessageRefusal(boolean mode)
	{
		_messageRefusal = mode;
		sendPacket(new EtcStatusUpdate(this));
	}
	
	public void setDietMode(boolean mode)
	{
		_dietMode = mode;
	}
	
	public boolean getDietMode()
	{
		return _dietMode;
	}
	
	public void setTradeRefusal(boolean mode)
	{
		_tradeRefusal = mode;
	}
	
	public boolean getTradeRefusal()
	{
		return _tradeRefusal;
	}
	
	public void setExchangeRefusal(boolean mode)
	{
		_exchangeRefusal = mode;
	}
	
	public boolean getExchangeRefusal()
	{
		return _exchangeRefusal;
	}
	
	public BlockList getBlockList()
	{
		return _blockList;
	}
	
	public void setHero(boolean hero)
	{
		if (hero && (_baseClass == _activeClass))
		{
			for (Skill skill : SkillTreeData.getInstance().getHeroSkillTree().values())
			{
				addSkill(skill, false); // Don't persist hero skills into database
			}
		}
		else
		{
			for (Skill skill : SkillTreeData.getInstance().getHeroSkillTree().values())
			{
				removeSkill(skill, false, true); // Just remove skills from non-hero players
			}
		}
		
		_hero = hero;
		sendSkillList();
	}
	
	public void setInOlympiadMode(boolean value)
	{
		_inOlympiadMode = value;
	}
	
	public void setOlympiadStart(boolean value)
	{
		_olympiadStart = value;
	}
	
	public boolean isOlympiadStart()
	{
		return _olympiadStart;
	}
	
	public boolean isHero()
	{
		return _hero;
	}
	
	public boolean isInOlympiadMode()
	{
		return _inOlympiadMode;
	}
	
	@Override
	public boolean isInDuel()
	{
		return _isInDuel;
	}
	
	public void setStartingDuel()
	{
		_startingDuel = true;
	}
	
	@Override
	public int getDuelId()
	{
		return _duelId;
	}
	
	public void setDuelState(int mode)
	{
		_duelState = mode;
	}
	
	public int getDuelState()
	{
		return _duelState;
	}
	
	/**
	 * Sets up the duel state using a non 0 duelId.
	 * @param duelId 0=not in a duel
	 */
	public void setInDuel(int duelId)
	{
		if (duelId > 0)
		{
			_isInDuel = true;
			_duelState = Duel.DUELSTATE_DUELLING;
			_duelId = duelId;
		}
		else
		{
			if (_duelState == Duel.DUELSTATE_DEAD)
			{
				enableAllSkills();
				getStatus().startHpMpRegeneration();
			}
			
			_isInDuel = false;
			_duelState = Duel.DUELSTATE_NODUEL;
			_duelId = 0;
		}
		
		_startingDuel = false;
	}
	
	/**
	 * This returns a SystemMessage stating why the player is not available for duelling.
	 * @return S1_CANNOT_DUEL... message
	 */
	public SystemMessage getNoDuelReason()
	{
		final SystemMessage sm = new SystemMessage(_noDuelReason);
		sm.addPcName(this);
		_noDuelReason = SystemMessageId.THERE_IS_NO_OPPONENT_TO_RECEIVE_YOUR_CHALLENGE_FOR_A_DUEL;
		return sm;
	}
	
	/**
	 * Checks if this player might join / start a duel.<br>
	 * To get the reason use getNoDuelReason() after calling this function.
	 * @return true if the player might join/start a duel.
	 */
	public boolean canDuel()
	{
		if (isInCombat() || isJailed())
		{
			_noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_CURRENTLY_ENGAGED_IN_BATTLE;
			return false;
		}
		
		if (isDead() || isAlikeDead() || ((getCurrentHp() < (getMaxHp() / 2)) || (getCurrentMp() < (getMaxMp() / 2))))
		{
			_noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_S_HP_OR_MP_IS_BELOW_50_PERCENT;
			return false;
		}
		
		if (_isInDuel || _startingDuel)
		{
			_noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_ALREADY_ENGAGED_IN_A_DUEL;
			return false;
		}
		
		if (_inOlympiadMode)
		{
			_noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_PARTICIPATING_IN_THE_OLYMPIAD;
			return false;
		}
		
		if (isCursedWeaponEquipped())
		{
			_noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_IN_A_CHAOTIC_STATE;
			return false;
		}
		
		if (_privateStoreType != PrivateStoreType.NONE)
		{
			_noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_CURRENTLY_ENGAGED_IN_A_PRIVATE_STORE_OR_MANUFACTURE;
			return false;
		}
		
		if (isMounted() || isInBoat())
		{
			_noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_CURRENTLY_RIDING_A_BOAT_WYVERN_OR_STRIDER;
			return false;
		}
		
		if (_fishing)
		{
			_noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_CURRENTLY_FISHING;
			return false;
		}
		
		if (isInsideZone(ZoneId.PVP) || isInsideZone(ZoneId.PEACE) || isInsideZone(ZoneId.SIEGE) || isInsideZone(ZoneId.NO_PVP))
		{
			_noDuelReason = SystemMessageId.S1_CANNOT_MAKE_A_CHALLENGE_TO_A_DUEL_BECAUSE_S1_IS_CURRENTLY_IN_A_DUEL_PROHIBITED_AREA_PEACEFUL_ZONE_SEVEN_SIGNS_ZONE_NEAR_WATER_RESTART_PROHIBITED_AREA;
			return false;
		}
		
		return true;
	}
	
	public boolean isNoble()
	{
		return _noble;
	}
	
	public void setNoble(boolean value)
	{
		final Collection<Skill> nobleSkillTree = SkillTreeData.getInstance().getNobleSkillTree().values();
		if (value)
		{
			for (Skill skill : nobleSkillTree)
			{
				addSkill(skill, false);
			}
		}
		else
		{
			for (Skill skill : nobleSkillTree)
			{
				removeSkill(skill, false, true);
			}
		}
		
		_noble = value;
		sendSkillList();
	}
	
	public void setLvlJoinedAcademy(int lvl)
	{
		_lvlJoinedAcademy = lvl;
	}
	
	public int getLvlJoinedAcademy()
	{
		return _lvlJoinedAcademy;
	}
	
	@Override
	public boolean isAcademyMember()
	{
		return _lvlJoinedAcademy > 0;
	}
	
	@Override
	public void setTeam(Team team)
	{
		super.setTeam(team);
		broadcastUserInfo();
		if (hasSummon())
		{
			_summon.broadcastStatusUpdate();
		}
	}
	
	public void setWantsPeace(int wantsPeace)
	{
		_wantsPeace = wantsPeace;
	}
	
	public int getWantsPeace()
	{
		return _wantsPeace;
	}
	
	public boolean isFishing()
	{
		return _fishing;
	}
	
	public void setFishing(boolean fishing)
	{
		_fishing = fishing;
	}
	
	public synchronized void sendSkillList()
	{
		if (_skillListTask == null)
		{
			_skillListTask = ThreadPool.schedule(() ->
			{
				final SkillList skillList = new SkillList();
				for (Skill skill : getAllSkills())
				{
					if (skill == null)
					{
						continue;
					}
					
					final int skillId = skill.getId();
					boolean isDisabled = false;
					
					// Disable clan skills if clan reputation is negative.
					if ((_clan != null) && skill.isClanSkill() && (_clan.getReputationScore() < 0))
					{
						isDisabled = true;
					}
					
					if (!skill.isPassive()) // Skip passive skills.
					{
						if (isWearingFormalWear())
						{
							isDisabled = true;
						}
						else if (isCursedWeaponEquipped())
						{
							if ((skillId != CommonSkill.VOID_BURST.getId()) && (skillId != CommonSkill.VOID_FLOW.getId()))
							{
								isDisabled = true;
							}
						}
						else if (isMounted())
						{
							switch (getMountType())
							{
								case STRIDER:
								{
									// Allow magic skills and Strider Siege Assault.
									if (!skill.isMagic() && (skillId != CommonSkill.STRIDER_SIEGE_ASSAULT.getId()))
									{
										isDisabled = true;
									}
									break;
								}
								case WYVERN:
								{
									// Allow only Wyvern Breath and Wyvern Aegis.
									if ((skillId != CommonSkill.WYVERN_BREATH.getId()) && (skillId != CommonSkill.WYVERN_AEGIS.getId()))
									{
										isDisabled = true;
									}
									break;
								}
							}
						}
					}
					
					skillList.addSkill(skill.getDisplayId(), skill.getDisplayLevel(), skill.isPassive(), isDisabled);
				}
				
				sendPacket(skillList);
				_skillListTask = null;
			}, 300);
		}
	}
	
	/**
	 * 1. Add the specified class ID as a subclass (up to the maximum number of <b>three</b>) for this character.<br>
	 * 2. This method no longer changes the active _classIndex of the player. This is only done by the calling of setActiveClass() method as that should be the only way to do so.
	 * @param classId
	 * @param classIndex
	 * @return boolean subclassAdded
	 */
	public boolean addSubClass(int classId, int classIndex)
	{
		if (_subclassLock)
		{
			return false;
		}
		
		_subclassLock = true;
		
		try
		{
			if ((getTotalSubClasses() == PlayerConfig.MAX_SUBCLASS) || (classIndex == 0))
			{
				return false;
			}
			
			if (getSubClasses().containsKey(classIndex))
			{
				return false;
			}
			
			// Note: Never change _classIndex in any method other than setActiveClass().
			
			final SubClassHolder newClass = new SubClassHolder();
			newClass.setPlayerClass(classId);
			newClass.setClassIndex(classIndex);
			
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement(ADD_CHAR_SUBCLASS))
			{
				// Store the basic info about this new sub-class.
				ps.setInt(1, getObjectId());
				ps.setInt(2, newClass.getId());
				ps.setLong(3, newClass.getExp());
				ps.setLong(4, newClass.getSp());
				ps.setInt(5, newClass.getLevel());
				ps.setInt(6, newClass.getClassIndex()); // <-- Added
				ps.execute();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "WARNING: Could not add character sub class for " + getName() + ": " + e.getMessage(), e);
				return false;
			}
			
			// Commit after database INSERT incase exception is thrown.
			getSubClasses().put(newClass.getClassIndex(), newClass);
			
			final PlayerClass subTemplate = PlayerClass.getPlayerClass(classId);
			final Map<Integer, SkillLearn> skillTree = SkillTreeData.getInstance().getCompleteClassSkillTree(subTemplate);
			final Map<Integer, Skill> prevSkillList = new HashMap<>();
			for (SkillLearn skillInfo : skillTree.values())
			{
				if (skillInfo.getGetLevel() <= 40)
				{
					final Skill prevSkill = prevSkillList.get(skillInfo.getSkillId());
					final Skill newSkill = SkillData.getInstance().getSkill(skillInfo.getSkillId(), skillInfo.getSkillLevel());
					if ((prevSkill != null) && (prevSkill.getLevel() > newSkill.getLevel()))
					{
						continue;
					}
					
					prevSkillList.put(newSkill.getId(), newSkill);
					storeSkill(newSkill, prevSkill, classIndex);
				}
			}
			
			return true;
		}
		finally
		{
			_subclassLock = false;
		}
	}
	
	/**
	 * 1. Completely erase all existance of the subClass linked to the classIndex.<br>
	 * 2. Send over the newClassId to addSubClass() to create a new instance on this classIndex.<br>
	 * 3. Upon Exception, revert the player to their BaseClass to avoid further problems.
	 * @param classIndex the class index to delete
	 * @param newClassId the new class Id
	 * @return {@code true} if the sub-class was modified, {@code false} otherwise
	 */
	public boolean modifySubClass(int classIndex, int newClassId)
	{
		final SubClassHolder subClass = getSubClasses().get(classIndex);
		if (subClass == null)
		{
			return false;
		}
		
		// Notify to scripts before class is removed.
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_PROFESSION_CANCEL, this))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerProfessionCancel(this, subClass.getId()), this);
		}
		
		getSubClasses().remove(classIndex);
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement deleteHennas = con.prepareStatement(DELETE_CHAR_HENNA);
			PreparedStatement deleteShortcuts = con.prepareStatement(DELETE_CHAR_SHORTCUTS);
			PreparedStatement deleteSkillReuse = con.prepareStatement(DELETE_SKILL_SAVE);
			PreparedStatement deleteSkills = con.prepareStatement(DELETE_CHAR_SKILLS);
			PreparedStatement deleteSubclass = con.prepareStatement(DELETE_CHAR_SUBCLASS))
		{
			// Remove class permitted hennas.
			for (int slot = 1; slot < 4; slot++)
			{
				final Henna henna = getHenna(slot);
				if (henna != null)
				{
					deleteHennas.setInt(1, getObjectId());
					deleteHennas.setInt(2, slot);
					deleteHennas.setInt(3, classIndex);
					deleteHennas.execute();
				}
			}
			
			// Remove all shortcuts info stored for this sub-class.
			deleteShortcuts.setInt(1, getObjectId());
			deleteShortcuts.setInt(2, classIndex);
			deleteShortcuts.execute();
			
			// Remove all effects info stored for this sub-class.
			deleteSkillReuse.setInt(1, getObjectId());
			deleteSkillReuse.setInt(2, classIndex);
			deleteSkillReuse.execute();
			
			// Remove all skill info stored for this sub-class.
			deleteSkills.setInt(1, getObjectId());
			deleteSkills.setInt(2, classIndex);
			deleteSkills.execute();
			
			// Remove all basic info stored about this sub-class.
			deleteSubclass.setInt(1, getObjectId());
			deleteSubclass.setInt(2, classIndex);
			deleteSubclass.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not modify sub class for " + getName() + " to class index " + classIndex + ": " + e.getMessage(), e);
			return false;
		}
		
		return addSubClass(newClassId, classIndex);
	}
	
	public boolean isSubClassActive()
	{
		return _classIndex > 0;
	}
	
	public Map<Integer, SubClassHolder> getSubClasses()
	{
		return _subClasses;
	}
	
	public int getTotalSubClasses()
	{
		return getSubClasses().size();
	}
	
	public int getBaseClass()
	{
		return _baseClass;
	}
	
	public int getActiveClass()
	{
		return _activeClass;
	}
	
	public int getClassIndex()
	{
		return _classIndex;
	}
	
	private void setClassTemplate(int classId)
	{
		_activeClass = classId;
		
		final PlayerTemplate pcTemplate = PlayerTemplateData.getInstance().getTemplate(classId);
		if (pcTemplate == null)
		{
			LOGGER.severe("Missing template for classId: " + classId);
			throw new Error();
		}
		
		// Set the template of the Player
		setTemplate(pcTemplate);
		
		// Notify to scripts
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_PROFESSION_CHANGE, this))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerProfessionChange(this, pcTemplate, isSubClassActive()), this);
		}
	}
	
	/**
	 * Changes the character's class based on the given class index.<br>
	 * An index of zero specifies the character's original (base) class, while indexes 1-3 specifies the character's sub-classes respectively.<br>
	 * <font color="00FF00"/>WARNING: Use only on subclase change</font>
	 * @param classIndex
	 */
	public void setActiveClass(int classIndex)
	{
		if (_subclassLock)
		{
			return;
		}
		
		_subclassLock = true;
		
		try
		{
			// Remove active item skills before saving char to database
			// because next time when choosing this class, weared items can
			// be different
			for (Item item : _inventory.getAugmentedItems())
			{
				if ((item != null) && item.isEquipped())
				{
					item.getAugmentation().removeBonus(this);
				}
			}
			
			// abort any kind of cast.
			abortCast();
			
			if (isChannelized())
			{
				getSkillChannelized().abortChannelization();
			}
			
			// 1. Call store() before modifying _classIndex to avoid skill effects rollover.
			// 2. Register the correct _classId against applied 'classIndex'.
			store(PlayerConfig.SUBCLASS_STORE_SKILL_COOLTIME);
			
			resetTimeStamps();
			
			// clear charges
			_charges.set(0);
			stopChargeTask();
			
			if (hasServitor())
			{
				_summon.unSummon(this);
			}
			
			if (classIndex == 0)
			{
				setClassTemplate(_baseClass);
			}
			else
			{
				try
				{
					setClassTemplate(getSubClasses().get(classIndex).getId());
				}
				catch (Exception e)
				{
					LOGGER.log(Level.WARNING, "Could not switch " + getName() + "'s sub class to class index " + classIndex + ": " + e.getMessage(), e);
					return;
				}
			}
			
			_classIndex = classIndex;
			setLearningClass(getPlayerClass());
			
			if (isInParty())
			{
				_party.recalculatePartyLevel();
			}
			
			// Update the character's change in class status.
			// 1. Remove any active cubics from the player.
			// 2. Renovate the characters table in the database with the new class info, storing also buff/effect data.
			// 3. Remove all existing skills.
			// 4. Restore all the learned skills for the current class from the database.
			// 5. Restore effect/buff data for the new class.
			// 6. Restore henna data for the class, applying the new stat modifiers while removing existing ones.
			// 7. Reset HP/MP/CP stats and send Server->Client character status packet to reflect changes.
			// 8. Restore shortcut data related to this class.
			// 9. Resend a class change animation effect to broadcast to all nearby players.
			_autoUseSettings.getAutoSkills().clear();
			_autoUseSettings.getAutoBuffs().clear();
			for (Skill oldSkill : getAllSkills())
			{
				removeSkill(oldSkill, false, true);
			}
			
			stopAllEffectsExceptThoseThatLastThroughDeath();
			stopAllEffectsNotStayOnSubclassChange();
			getEffectList().stopAllToggles();
			stopCubics();
			restoreRecipeBook(false);
			
			// Restore any Death Penalty Buff
			restoreDeathPenaltyBuffLevel();
			
			restoreSkills();
			rewardSkills();
			regiveTemporarySkills();
			getInventory().applyItemSkills();
			
			// Prevents some issues when changing between subclases that shares skills
			resetDisabledSkills();
			
			restoreEffects();
			
			sendPacket(new EtcStatusUpdate(this));
			
			// if player has quest 422: Repent Your Sins, remove it
			final QuestState st = getQuestState("Q00422_RepentYourSins");
			if (st != null)
			{
				st.exitQuest(true);
			}
			
			for (int i = 0; i < 3; i++)
			{
				_henna[i] = null;
			}
			
			restoreHenna();
			sendPacket(new HennaInfo(this));
			if (getCurrentHp() > getMaxHp())
			{
				setCurrentHp(getMaxHp());
			}
			
			if (getCurrentMp() > getMaxMp())
			{
				setCurrentMp(getMaxMp());
			}
			
			if (getCurrentCp() > getMaxCp())
			{
				setCurrentCp(getMaxCp());
			}
			
			refreshOverloaded();
			refreshExpertisePenalty();
			broadcastUserInfo();
			
			// Clear resurrect xp calculation
			setExpBeforeDeath(0);
			
			_shortcuts.restoreMe();
			sendPacket(new ShortcutInit(this));
			broadcastPacket(new SocialAction(getObjectId(), SocialAction.LEVEL_UP));
			sendPacket(new SkillCoolTime(this));
			sendPacket(new ExStorageMaxCount(this));
		}
		finally
		{
			_subclassLock = false;
		}
	}
	
	public void stopWarnUserTakeBreak()
	{
		if (_taskWarnUserTakeBreak == null)
		{
			return;
		}
		
		_taskWarnUserTakeBreak.cancel(true);
		_taskWarnUserTakeBreak = null;
	}
	
	public void startWarnUserTakeBreak()
	{
		if (_taskWarnUserTakeBreak == null)
		{
			_taskWarnUserTakeBreak = ThreadPool.scheduleAtFixedRate(new WarnUserTakeBreakTask(this), 7200000, 7200000);
		}
	}
	
	public void stopRentPet()
	{
		if (_taskRentPet != null)
		{
			// if the rent of a wyvern expires while over a flying zone, tp to down before unmounting
			if (checkLandingState() && (_mountType == MountType.WYVERN))
			{
				teleToLocation(TeleportWhereType.TOWN);
			}
			
			if (dismount()) // this should always be true now, since we teleported already
			{
				_taskRentPet.cancel(true);
				_taskRentPet = null;
			}
		}
	}
	
	public void startRentPet(int seconds)
	{
		if (_taskRentPet == null)
		{
			_taskRentPet = ThreadPool.scheduleAtFixedRate(new RentPetTask(this), seconds * 1000, seconds * 1000);
		}
	}
	
	public boolean isRentedPet()
	{
		return _taskRentPet != null;
	}
	
	public void stopWaterTask()
	{
		if (_taskWater == null)
		{
			return;
		}
		
		_taskWater.cancel(false);
		_taskWater = null;
		sendPacket(new SetupGauge(getObjectId(), 2, 0));
	}
	
	public void startWaterTask()
	{
		if (isDead() || (_taskWater != null))
		{
			return;
		}
		
		// Base time in water is 60 seconds (60000 milliseconds).
		final int baseTimeInWater = 60000;
		
		// Calculate the total stat as a percentage of the base time.
		final double breathPercentage = calcStat(Stat.BREATH, getBaseTemplate().getBaseBreath()) / 100d;
		
		// Apply the percentage to the base time.
		final int timeInWater = (int) (baseTimeInWater * breathPercentage);
		
		// Send the setup gauge packet with the calculated time.
		sendPacket(new SetupGauge(getObjectId(), 2, timeInWater));
		
		// Schedule the water task.
		_taskWater = ThreadPool.scheduleAtFixedRate(new WaterTask(this), timeInWater, 1000);
	}
	
	public boolean isInWater()
	{
		return _taskWater != null;
	}
	
	public void checkWaterState()
	{
		if (isInsideZone(ZoneId.WATER))
		{
			startWaterTask();
		}
		else
		{
			stopWaterTask();
		}
	}
	
	public void onPlayerEnter()
	{
		startWarnUserTakeBreak();
		
		if (SevenSigns.getInstance().isSealValidationPeriod() || SevenSigns.getInstance().isCompResultsPeriod())
		{
			if (!isGM() && isIn7sDungeon() && (SevenSigns.getInstance().getPlayerCabal(getObjectId()) != SevenSigns.getInstance().getCabalHighestScore()))
			{
				teleToLocation(TeleportWhereType.TOWN);
				setIn7sDungeon(false);
				sendMessage("You have been teleported to the nearest town due to the beginning of the Seal Validation period.");
			}
		}
		else
		{
			if (!isGM() && isIn7sDungeon() && (SevenSigns.getInstance().getPlayerCabal(getObjectId()) == SevenSigns.CABAL_NULL))
			{
				teleToLocation(TeleportWhereType.TOWN);
				setIn7sDungeon(false);
				sendMessage("You have been teleported to the nearest town because you have not signed for any cabal.");
			}
		}
		
		if (isGM() && !GeneralConfig.GM_STARTUP_BUILDER_HIDE)
		{
			// Bleah, see L2J custom below.
			if (isInvul())
			{
				sendMessage("Entering world in Invulnerable mode.");
			}
			
			if (isInvisible())
			{
				sendMessage("Entering world in Invisible mode.");
			}
			
			if (_silenceMode)
			{
				sendMessage("Entering world in Silence mode.");
			}
		}
		
		// Buff and status icons
		if (PlayerConfig.STORE_SKILL_COOLTIME)
		{
			restoreEffects();
		}
		
		revalidateZone(true);
		
		notifyFriends();
		if (!isGM() && PlayerConfig.DECREASE_SKILL_LEVEL)
		{
			checkPlayerSkills();
		}
		
		try
		{
			for (ZoneType zone : ZoneManager.getInstance().getZones(this))
			{
				zone.onPlayerLoginInside(this);
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "", e);
		}
		
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_LOGIN, this))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerLogin(this), this);
		}
		
		// TODO : Need to fix that hack!
		if (!isDead())
		{
			// Run on a separate thread to give time to above events to be notified.
			ThreadPool.schedule(() ->
			{
				setCurrentHp(_originalHp);
				setCurrentMp(_originalMp);
				setCurrentCp(_originalCp);
			}, 300);
		}
	}
	
	/**
	 * Fully restores the player's HP, MP, and CP to their maximum values.
	 */
	@Override
	public void fullRestore()
	{
		setCurrentHp(getMaxHp());
		setCurrentMp(getMaxMp());
		setCurrentCp(getMaxCp());
	}
	
	public long getLastAccess()
	{
		return _lastAccess;
	}
	
	@Override
	public void doRevive()
	{
		super.doRevive();
		
		// Stop decay task.
		DecayTaskManager.getInstance().cancel(this);
		
		updateEffectIcons();
		sendPacket(new EtcStatusUpdate(this));
		_revivePet = false;
		_reviveRequested = 0;
		_revivePower = 0;
		
		// Teleport summon to player.
		if (isInsideZone(ZoneId.PEACE) && (_summon != null) && !_summon.isInsideZone(ZoneId.SIEGE))
		{
			_summon.teleToLocation(getLocation(), true);
		}
		
		if (isMounted())
		{
			startFeed(_mountNpcId);
		}
		
		if (isInParty() && _party.isInDimensionalRift() && !DimensionalRiftManager.getInstance().checkIfInPeaceZone(getX(), getY(), getZ()))
		{
			_party.getDimensionalRift().memberRessurected(this);
		}
		
		if (getInstanceId() > 0)
		{
			final Instance instance = InstanceManager.getInstance().getInstance(getInstanceId());
			if (instance != null)
			{
				instance.cancelEjectDeadPlayer(this);
			}
		}
	}
	
	@Override
	public void setName(String value)
	{
		super.setName(value);
		CharInfoTable.getInstance().addName(this);
	}
	
	@Override
	public void doRevive(double revivePower)
	{
		doRevive();
		restoreExp(revivePower);
	}
	
	public void reviveRequest(Player reviver, boolean pet, int power)
	{
		if (isResurrectionBlocked())
		{
			return;
		}
		
		if (_reviveRequested == 1)
		{
			if (_revivePet == pet)
			{
				reviver.sendPacket(SystemMessageId.RESURRECTION_HAS_ALREADY_BEEN_PROPOSED); // Resurrection is already been proposed.
			}
			else if (pet)
			{
				reviver.sendPacket(SystemMessageId.A_PET_CANNOT_BE_RESURRECTED_WHILE_IT_S_OWNER_IS_IN_THE_PROCESS_OF_RESURRECTING); // A pet cannot be resurrected while it's owner is in the process of resurrecting.
			}
			else
			{
				reviver.sendPacket(SystemMessageId.WHILE_A_PET_IS_ATTEMPTING_TO_RESURRECT_IT_CANNOT_HELP_IN_RESURRECTING_ITS_MASTER); // While a pet is attempting to resurrect, it cannot help in resurrecting its master.
			}
			return;
		}
		
		if ((pet && hasPet() && _summon.isDead()) || (!pet && isDead()))
		{
			_reviveRequested = 1;
			_revivePower = Formulas.calculateSkillResurrectRestorePercent(power, reviver);
			_revivePet = pet;
			
			final ConfirmDlg dlg = new ConfirmDlg(SystemMessageId.S1_IS_MAKING_AN_ATTEMPT_AT_RESURRECTION_DO_YOU_WANT_TO_CONTINUE_WITH_THIS_RESURRECTION.getId());
			dlg.getSystemMessage().addPcName(reviver);
			sendPacket(dlg);
		}
	}
	
	public void reviveAnswer(int answer)
	{
		if ((_reviveRequested != 1) || (!isDead() && !_revivePet) || (_revivePet && hasPet() && !_summon.isDead()))
		{
			return;
		}
		
		if (answer == 1)
		{
			if (!_revivePet)
			{
				if (_revivePower != 0)
				{
					doRevive(_revivePower);
				}
				else
				{
					doRevive();
				}
			}
			else if (hasPet())
			{
				if (_revivePower != 0)
				{
					_summon.doRevive(_revivePower);
				}
				else
				{
					_summon.doRevive();
				}
			}
		}
		
		_revivePet = false;
		_reviveRequested = 0;
		_revivePower = 0;
	}
	
	public boolean isReviveRequested()
	{
		return _reviveRequested == 1;
	}
	
	public boolean isRevivingPet()
	{
		return _revivePet;
	}
	
	public void removeReviving()
	{
		_reviveRequested = 0;
		_revivePower = 0;
	}
	
	public void onActionRequest()
	{
		if (isSpawnProtected())
		{
			setSpawnProtection(false);
			if (!isInsideZone(ZoneId.PEACE))
			{
				sendMessage("You are no longer protected from aggressive monsters.");
			}
			
			if (PlayerConfig.RESTORE_SERVITOR_ON_RECONNECT && !hasSummon() && CharSummonTable.getInstance().getServitors().containsKey(getObjectId()))
			{
				CharSummonTable.getInstance().restoreServitor(this);
			}
			
			if (PlayerConfig.RESTORE_PET_ON_RECONNECT && !hasSummon() && CharSummonTable.getInstance().getPets().containsKey(getObjectId()))
			{
				CharSummonTable.getInstance().restorePet(this);
			}
		}
		
		if (isTeleportProtected())
		{
			setTeleportProtection(false);
			if (!isInsideZone(ZoneId.PEACE))
			{
				sendMessage("Teleport spawn protection ended.");
			}
		}
	}
	
	/**
	 * Expertise of the Player (None=0, D=1, C=2, B=3, A=4, S=5)
	 * @return int Expertise skill level.
	 */
	public int getExpertiseLevel()
	{
		return getSkillLevel(239);
	}
	
	@Override
	public void teleToLocation(ILocational loc, boolean allowRandomOffset)
	{
		if ((_vehicle != null) && !_vehicle.isTeleporting())
		{
			setVehicle(null);
		}
		
		super.teleToLocation(loc, allowRandomOffset);
	}
	
	@Override
	public synchronized void onTeleported()
	{
		super.onTeleported();
		
		setLastServerPosition(getX(), getY(), getZ());
		
		// Force a revalidation
		revalidateZone(true);
		
		checkItemRestriction();
		
		if ((PlayerConfig.PLAYER_TELEPORT_PROTECTION > 0) && !_inOlympiadMode)
		{
			setTeleportProtection(true);
		}
		
		// Trained beast is lost after teleport
		if (_tamedBeast != null)
		{
			for (TamedBeast tamedBeast : _tamedBeast)
			{
				tamedBeast.deleteMe();
			}
			
			_tamedBeast.clear();
		}
		
		// Modify the position of the pet if necessary
		if (_summon != null)
		{
			_summon.setFollowStatus(false);
			_summon.teleToLocation(getLocation(), false);
			((SummonAI) _summon.getAI()).setStartFollowController(true);
			_summon.setFollowStatus(true);
			_summon.updateAndBroadcastStatus(0);
		}
		
		// Stop auto play.
		if (AutoPlayConfig.ENABLE_AUTO_PLAY)
		{
			AutoPlayTaskManager.getInstance().stopAutoPlay(this);
			AutoUseTaskManager.getInstance().stopAutoUseTask(this);
		}
		
		// Send info to nearby players.
		broadcastInfo();
	}
	
	@Override
	public void setTeleporting(boolean teleport)
	{
		setTeleporting(teleport, true);
	}
	
	public void setTeleporting(boolean teleport, boolean useWatchDog)
	{
		super.setTeleporting(teleport);
		if (!useWatchDog)
		{
			return;
		}
		
		if (teleport)
		{
			if ((_teleportWatchdog == null) && (PlayerConfig.TELEPORT_WATCHDOG_TIMEOUT > 0))
			{
				synchronized (this)
				{
					_teleportWatchdog = ThreadPool.schedule(new TeleportWatchdogTask(this), PlayerConfig.TELEPORT_WATCHDOG_TIMEOUT * 1000);
				}
			}
		}
		else if (_teleportWatchdog != null)
		{
			_teleportWatchdog.cancel(false);
			_teleportWatchdog = null;
		}
	}
	
	public void setLastServerPosition(int x, int y, int z)
	{
		_lastServerPosition.setXYZ(x, y, z);
	}
	
	public Location getLastServerPosition()
	{
		return _lastServerPosition;
	}
	
	public void setBlinkActive(boolean value)
	{
		_blinkActive.set(value);
	}
	
	public boolean isBlinkActive()
	{
		return _blinkActive.get();
	}
	
	public void addElementSeed(Element element)
	{
		_elementSeeds.get(element).incrementAndGet();
		ThreadPool.schedule(() -> removeElementSeed(element), 5000);
	}
	
	private void removeElementSeed(Element element)
	{
		_elementSeeds.get(element).decrementAndGet();
	}
	
	public int getElementSeedCount(Element element)
	{
		return _elementSeeds.get(element).get();
	}
	
	@Override
	public synchronized void addExpAndSp(double addToExp, double addToSp)
	{
		getStat().addExpAndSp(addToExp, addToSp, false);
	}
	
	public synchronized void addExpAndSp(double addToExp, double addToSp, boolean useVitality)
	{
		getStat().addExpAndSp(addToExp, addToSp, useVitality);
	}
	
	public void removeExpAndSp(long removeExp, long removeSp)
	{
		getStat().removeExpAndSp(removeExp, removeSp, true);
	}
	
	public void removeExpAndSp(long removeExp, long removeSp, boolean sendMessage)
	{
		getStat().removeExpAndSp(removeExp, removeSp, sendMessage);
	}
	
	@Override
	public void reduceCurrentHp(double value, Creature attacker, boolean awake, boolean isDOT, Skill skill)
	{
		if (skill != null)
		{
			getStatus().reduceHp(value, attacker, awake, isDOT, skill.isToggle(), skill.getDmgDirectlyToHP());
		}
		else
		{
			getStatus().reduceHp(value, attacker, awake, isDOT, false, false);
		}
		
		// notify the tamed beast of attacks
		if (_tamedBeast != null)
		{
			for (TamedBeast tamedBeast : _tamedBeast)
			{
				tamedBeast.onOwnerGotAttacked(attacker);
			}
		}
	}
	
	public void broadcastSnoop(ChatType type, String name, String text, CreatureSay cs)
	{
		if (_snoopListener.isEmpty())
		{
			return;
		}
		
		final Snoop sn = new Snoop(getObjectId(), getName(), type, name, text);
		for (Player pci : _snoopListener)
		{
			if (pci != null)
			{
				pci.sendPacket(cs);
				pci.sendPacket(sn);
			}
		}
	}
	
	public void addSnooper(Player pci)
	{
		_snoopListener.add(pci);
	}
	
	public void removeSnooper(Player pci)
	{
		_snoopListener.remove(pci);
	}
	
	public void addSnooped(Player pci)
	{
		_snoopedPlayer.add(pci);
	}
	
	public void removeSnooped(Player pci)
	{
		_snoopedPlayer.remove(pci);
	}
	
	public void addHtmlAction(HtmlActionScope scope, String action)
	{
		_htmlActionCaches[scope.ordinal()].add(action);
	}
	
	public void clearHtmlActions(HtmlActionScope scope)
	{
		_htmlActionCaches[scope.ordinal()].clear();
	}
	
	public void setHtmlActionOriginObjectId(HtmlActionScope scope, int npcObjId)
	{
		if (npcObjId < 0)
		{
			throw new IllegalArgumentException();
		}
		
		_htmlActionOriginObjectIds[scope.ordinal()] = npcObjId;
	}
	
	public int getLastHtmlActionOriginId()
	{
		return _lastHtmlActionOriginObjId;
	}
	
	private boolean validateHtmlAction(Iterable<String> actionIter, String action)
	{
		for (String cachedAction : actionIter)
		{
			if (cachedAction.charAt(cachedAction.length() - 1) == AbstractHtmlPacket.VAR_PARAM_START_CHAR)
			{
				if (action.startsWith(cachedAction.substring(0, cachedAction.length() - 1).trim()))
				{
					return true;
				}
			}
			else if (cachedAction.equals(action))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Check if the HTML action was sent in a HTML packet.<br>
	 * If the HTML action was not sent for whatever reason, -1 is returned.<br>
	 * Otherwise, the NPC object ID or 0 is returned.<br>
	 * 0 means the HTML action was not bound to an NPC<br>
	 * and no range checks need to be made.
	 * @param action the HTML action to check
	 * @return NPC object ID, 0 or -1
	 */
	public int validateHtmlAction(String action)
	{
		for (int i = 0; i < _htmlActionCaches.length; ++i)
		{
			if (validateHtmlAction(_htmlActionCaches[i], action))
			{
				_lastHtmlActionOriginObjId = _htmlActionOriginObjectIds[i];
				return _lastHtmlActionOriginObjId;
			}
		}
		
		return -1;
	}
	
	/**
	 * Performs following tests:
	 * <ul>
	 * <li>Inventory contains item</li>
	 * <li>Item owner id == owner id</li>
	 * <li>It isn't pet control item while mounting pet or pet summoned</li>
	 * <li>It isn't active enchant item</li>
	 * <li>It isn't cursed weapon/item</li>
	 * <li>It isn't wear item</li>
	 * </ul>
	 * @param objectId item object id
	 * @param itemProcessType the item process type
	 * @return
	 */
	public boolean validateItemManipulation(int objectId, ItemProcessType itemProcessType)
	{
		final Item item = _inventory.getItemByObjectId(objectId);
		if ((item == null) || (item.getOwnerId() != getObjectId()))
		{
			LOGGER.finest(getObjectId() + ": player tried to " + itemProcessType + " item he is not owner of.");
			return false;
		}
		
		// Pet is summoned and not the item that summoned the pet AND not the buggle from strider you're mounting
		if ((hasSummon() && (_summon.getControlObjectId() == objectId)) || (_mountObjectID == objectId))
		{
			return false;
		}
		
		if (_activeEnchantItemId == objectId)
		{
			return false;
		}
		
		if (CursedWeaponsManager.getInstance().isCursed(item.getId()))
		{
			// can not trade a cursed weapon
			return false;
		}
		
		return true;
	}
	
	/**
	 * @return Returns the inBoat.
	 */
	public boolean isInBoat()
	{
		return (_vehicle != null) && _vehicle.isBoat();
	}
	
	/**
	 * @return
	 */
	public Boat getBoat()
	{
		return (Boat) _vehicle;
	}
	
	public Vehicle getVehicle()
	{
		return _vehicle;
	}
	
	public void setVehicle(Vehicle v)
	{
		if ((v == null) && (_vehicle != null))
		{
			_vehicle.removePassenger(this);
		}
		
		_vehicle = v;
	}
	
	public boolean isInVehicle()
	{
		return _vehicle != null;
	}
	
	public void setInCrystallize(boolean inCrystallize)
	{
		_inCrystallize = inCrystallize;
	}
	
	public boolean isInCrystallize()
	{
		return _inCrystallize;
	}
	
	/**
	 * @return
	 */
	public Location getInVehiclePosition()
	{
		return _inVehiclePosition;
	}
	
	public void setInVehiclePosition(Location pt)
	{
		_inVehiclePosition = pt;
	}
	
	/**
	 * Manage the delete task of a Player (Leave Party, Unsummon pet, Save its inventory in the database, Remove it from the world...).<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>If the Player is in observer mode, set its position to its position before entering in observer mode</li>
	 * <li>Set the online Flag to True or False and update the characters table of the database with online status and lastAccess</li>
	 * <li>Stop the HP/MP/CP Regeneration task</li>
	 * <li>Cancel Crafting, Attack or Cast</li>
	 * <li>Remove the Player from the world</li>
	 * <li>Stop Party and Unsummon Pet</li>
	 * <li>Update database with items in its inventory and remove them from the world</li>
	 * <li>Remove all WorldObject from _knownObjects and _knownPlayer of the Creature then cancel Attak or Cast and notify AI</li>
	 * <li>Close the connection with the client</li>
	 * </ul>
	 * <br>
	 * Remember this method is not to be used to half-ass disconnect players! This method is dedicated only to erase the player from the world.<br>
	 * If you intend to disconnect a player please use {@link Disconnection}
	 */
	@Override
	public boolean deleteMe()
	{
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_LOGOUT, this))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerLogout(this), this);
		}
		
		try
		{
			for (ZoneType zone : ZoneManager.getInstance().getZones(this))
			{
				zone.onPlayerLogoutInside(this);
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		// Set the online Flag to True or False and update the characters table of the database with online status and lastAccess (called when login and logout)
		try
		{
			if (!_isOnline)
			{
				LOGGER.log(Level.SEVERE, "deleteMe() called on offline character " + this, new RuntimeException());
			}
			
			setOnlineStatus(false, true);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		try
		{
			_isOnline = false;
			_offlinePlay = false;
			abortAttack();
			abortCast();
			stopMove(null);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		try
		{
			PartyMatchWaitingList.getInstance().removePlayer(this);
			if (_partyroom != 0)
			{
				final PartyMatchRoom room = PartyMatchRoomList.getInstance().getRoom(_partyroom);
				if (room != null)
				{
					room.deleteMember(this);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		try
		{
			if (isFlying())
			{
				removeSkill(CommonSkill.WYVERN_BREATH.getSkill());
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		// Make sure player variables are stored.
		getVariables().saveNow();
		
		// Make sure account variables are stored.
		getAccountVariables().saveNow();
		
		// Stop the HP/MP/CP Regeneration task (scheduled tasks)
		try
		{
			stopAllTimers();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		try
		{
			setTeleporting(false);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		// Stop crafting, if in progress
		try
		{
			RecipeManager.getInstance().requestMakeItemAbort(this);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		// Cancel Attak or Cast
		try
		{
			setTarget(null);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		if (isChannelized())
		{
			getSkillChannelized().abortChannelization();
		}
		
		// Stop all toggles.
		getEffectList().stopAllToggles();
		
		// Remove from world regions zones.
		final ZoneRegion region = ZoneManager.getInstance().getRegion(this);
		if (region != null)
		{
			region.removeFromZones(this);
		}
		
		// If a Party is in progress, leave it (and festival party)
		if (isInParty())
		{
			try
			{
				leaveParty();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "deleteMe()", e);
			}
		}
		
		stopCubics();
		
		// Remove the Player from the world
		try
		{
			decayMe();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		if (Olympiad.getInstance().isRegistered(this) || (getOlympiadGameId() != -1))
		{
			Olympiad.getInstance().removeDisconnectedCompetitor(this);
		}
		
		// If the Player has Pet, unsummon it
		if (hasSummon())
		{
			try
			{
				_summon.setRestoreSummon(true);
				
				_summon.unSummon(this);
				
				// Dead pet wasn't unsummoned, broadcast npcinfo changes (pet will be without owner name - means owner offline)
				if (hasSummon())
				{
					_summon.broadcastNpcInfo(0);
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "deleteMe()", e);
			} // returns pet to control item
		}
		
		if (_clan != null)
		{
			// set the status for pledge member list to OFFLINE
			try
			{
				final ClanMember clanMember = _clan.getClanMember(getObjectId());
				if (clanMember != null)
				{
					clanMember.setPlayer(null);
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "deleteMe()", e);
			}
		}
		
		if (getActiveRequester() != null)
		{
			// deals with sudden exit in the middle of transaction
			setActiveRequester(null);
			cancelActiveTrade();
		}
		
		// If the Player is a GM, remove it from the GM List
		if (isGM())
		{
			try
			{
				AdminData.getInstance().deleteGm(this);
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "deleteMe()", e);
			}
		}
		
		try
		{
			// Check if the Player is in observer mode to set its position to its position
			// before entering in observer mode
			if (_observerMode)
			{
				setLocationInvisible(_lastLoc);
			}
			
			if (_vehicle != null)
			{
				_vehicle.oustPlayer(this);
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		// remove player from instance and set spawn location if any
		try
		{
			final int instanceId = getInstanceId();
			if ((instanceId != 0) && !GeneralConfig.RESTORE_PLAYER_INSTANCE)
			{
				final Instance inst = InstanceManager.getInstance().getInstance(instanceId);
				if (inst != null)
				{
					inst.removePlayer(getObjectId());
					final Location loc = inst.getExitLoc();
					if (loc != null)
					{
						final int x = loc.getX() + Rnd.get(-30, 30);
						final int y = loc.getY() + Rnd.get(-30, 30);
						setXYZInvisible(x, y, loc.getZ());
						if (hasSummon()) // dead pet
						{
							_summon.teleToLocation(loc, true);
							_summon.setInstanceId(0);
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		// Update database with items in its inventory and remove them from the world
		try
		{
			_inventory.deleteMe();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		// Update database with items in its warehouse and remove them from the world
		try
		{
			getWarehouse().deleteMe();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		try
		{
			_freight.deleteMe();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		try
		{
			clearRefund();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		if (isCursedWeaponEquipped())
		{
			try
			{
				CursedWeaponsManager.getInstance().getCursedWeapon(_cursedWeaponEquippedId).setPlayer(null);
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "deleteMe()", e);
			}
		}
		
		if (_clanId > 0)
		{
			_clan.broadcastToOtherOnlineMembers(new PledgeShowMemberListUpdate(this), this);
			// ClanTable.getInstance().getClan(getClanId()).broadcastToOnlineMembers(new PledgeShowMemberListAdd(this));
		}
		
		for (Player player : _snoopedPlayer)
		{
			player.removeSnooper(this);
		}
		
		for (Player player : _snoopListener)
		{
			player.removeSnooped(this);
		}
		
		try
		{
			notifyFriends();
			_blockList.playerLogout();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception on deleteMe() notifyFriends: " + e.getMessage(), e);
		}
		
		PlayerAutoSaveTaskManager.getInstance().remove(this);
		
		return super.deleteMe();
	}
	
	// startFishing() was stripped of any pre-fishing related checks, namely the fishing zone check.
	// Also worthy of note is the fact the code to find the hook landing position was also striped.
	// The stripped code was moved into fishing.java.
	// In my opinion it makes more sense for it to be there since all other skill related checks were also there.
	// Last but not least, moving the zone check there, fixed a bug where baits would always be consumed no matter if fishing actualy took place.
	// startFishing() now takes up 3 arguments, wich are acurately described as being the hook landing coordinates.
	public void startFishing(int x, int y, int z)
	{
		stopMove(null);
		setImmobilized(true);
		_fishing = true;
		_fishX = x;
		_fishY = y;
		_fishZ = z;
		
		// broadcastUserInfo();
		// Starts fishing
		final int lvl = getRandomFishLvl();
		final int grade = getRandomFishGrade();
		final int group = getRandomFishGroup(grade);
		final List<Fish> fish = FishData.getInstance().getFish(lvl, group, grade);
		if ((fish == null) || fish.isEmpty())
		{
			sendMessage("Error - Fish are not defined");
			endFishing(false);
			return;
		}
		
		// Use a copy constructor else the fish data may be over-written below
		_fish = fish.get(Rnd.get(fish.size())).clone();
		fish.clear();
		sendPacket(SystemMessageId.YOU_CAST_YOUR_LINE_AND_START_TO_FISH);
		if (!GameTimeTaskManager.getInstance().isNight() && _lure.isNightLure())
		{
			_fish.setFishGroup(-1);
		}
		
		// sendMessage("Hook x,y: " + _x + "," + _y + " - Water Z, Player Z:" + _z + ", " + getZ()); //debug line, uncoment to show coordinates used in fishing.
		broadcastPacket(new ExFishingStart(this, _fish.getFishGroup(), x, y, z, _lure.isNightLure()));
		sendPacket(new PlaySound(1, "SF_P_01", 0, 0, 0, 0, 0));
		startLookingForFishTask();
	}
	
	public void stopLookingForFishTask()
	{
		if (_taskForFish == null)
		{
			return;
		}
		
		_taskForFish.cancel(false);
		_taskForFish = null;
	}
	
	public void startLookingForFishTask()
	{
		if (!isDead() && (_taskForFish == null))
		{
			int checkDelay = 0;
			boolean isNoob = false;
			boolean isUpperGrade = false;
			if (_lure != null)
			{
				final int lureid = _lure.getId();
				isNoob = _fish.getFishGrade() == 0;
				isUpperGrade = _fish.getFishGrade() == 2;
				if ((lureid == 6519) || (lureid == 6522) || (lureid == 6525) || (lureid == 8505) || (lureid == 8508) || (lureid == 8511))
				{
					checkDelay = _fish.getGutsCheckTime() * 133;
				}
				else if ((lureid == 6520) || (lureid == 6523) || (lureid == 6526) || ((lureid >= 8505) && (lureid <= 8513)) || ((lureid >= 7610) && (lureid <= 7613)) || ((lureid >= 7807) && (lureid <= 7809)) || ((lureid >= 8484) && (lureid <= 8486)))
				{
					checkDelay = _fish.getGutsCheckTime() * 100;
				}
				else if ((lureid == 6521) || (lureid == 6524) || (lureid == 6527) || (lureid == 8507) || (lureid == 8510) || (lureid == 8513))
				{
					checkDelay = _fish.getGutsCheckTime() * 66;
				}
			}
			
			_taskForFish = ThreadPool.scheduleAtFixedRate(new LookingForFishTask(this, _fish.getStartCombatTime(), _fish.getFishGuts(), _fish.getFishGroup(), isNoob, isUpperGrade), 10000, checkDelay);
		}
	}
	
	private int getRandomFishGrade()
	{
		switch (_lure.getId())
		{
			case 7807: // green for beginners
			case 7808: // purple for beginners
			case 7809: // yellow for beginners
			case 8486: // prize-winning for beginners
			{
				return 0;
			}
			case 8485: // prize-winning luminous
			case 8506: // green luminous
			case 8509: // purple luminous
			case 8512: // yellow luminous
			{
				return 2;
			}
			default:
			{
				return 1;
			}
		}
	}
	
	private int getRandomFishGroup(int group)
	{
		final int check = Rnd.get(100);
		int type = 1;
		switch (group)
		{
			case 0: // fish for novices
			{
				switch (_lure.getId())
				{
					case 7807: // green lure, preferred by fast-moving (nimble) fish (type 5)
					{
						if (check <= 54)
						{
							type = 5;
						}
						else if (check <= 77)
						{
							type = 4;
						}
						else
						{
							type = 6;
						}
						break;
					}
					case 7808: // purple lure, preferred by fat fish (type 4)
					{
						if (check <= 54)
						{
							type = 4;
						}
						else if (check <= 77)
						{
							type = 6;
						}
						else
						{
							type = 5;
						}
						break;
					}
					case 7809: // yellow lure, preferred by ugly fish (type 6)
					{
						if (check <= 54)
						{
							type = 6;
						}
						else if (check <= 77)
						{
							type = 5;
						}
						else
						{
							type = 4;
						}
						break;
					}
					case 8486: // prize-winning fishing lure for beginners
					{
						if (check <= 33)
						{
							type = 4;
						}
						else if (check <= 66)
						{
							type = 5;
						}
						else
						{
							type = 6;
						}
						break;
					}
				}
				break;
			}
			case 1: // normal fish
			{
				switch (_lure.getId())
				{
					case 7610:
					case 7611:
					case 7612:
					case 7613:
					{
						type = 3;
						break;
					}
					case 6519: // all theese lures (green) are prefered by fast-moving (nimble) fish (type 1)
					case 8505:
					case 6520:
					case 6521:
					case 8507:
					{
						if (check <= 54)
						{
							type = 1;
						}
						else if (check <= 74)
						{
							type = 0;
						}
						else if (check <= 94)
						{
							type = 2;
						}
						else
						{
							type = 3;
						}
						break;
					}
					case 6522: // all theese lures (purple) are prefered by fat fish (type 0)
					case 8508:
					case 6523:
					case 6524:
					case 8510:
					{
						if (check <= 54)
						{
							type = 0;
						}
						else if (check <= 74)
						{
							type = 1;
						}
						else if (check <= 94)
						{
							type = 2;
						}
						else
						{
							type = 3;
						}
						break;
					}
					case 6525: // all theese lures (yellow) are prefered by ugly fish (type 2)
					case 8511:
					case 6526:
					case 6527:
					case 8513:
					{
						if (check <= 55)
						{
							type = 2;
						}
						else if (check <= 74)
						{
							type = 1;
						}
						else if (check <= 94)
						{
							type = 0;
						}
						else
						{
							type = 3;
						}
						break;
					}
					case 8484: // prize-winning fishing lure
					{
						if (check <= 33)
						{
							type = 0;
						}
						else if (check <= 66)
						{
							type = 1;
						}
						else
						{
							type = 2;
						}
						break;
					}
				}
				break;
			}
			case 2: // upper grade fish, luminous lure
			{
				switch (_lure.getId())
				{
					case 8506: // green lure, preferred by fast-moving (nimble) fish (type 8)
					{
						if (check <= 54)
						{
							type = 8;
						}
						else if (check <= 77)
						{
							type = 7;
						}
						else
						{
							type = 9;
						}
						break;
					}
					case 8509: // purple lure, preferred by fat fish (type 7)
					{
						if (check <= 54)
						{
							type = 7;
						}
						else if (check <= 77)
						{
							type = 9;
						}
						else
						{
							type = 8;
						}
						break;
					}
					case 8512: // yellow lure, preferred by ugly fish (type 9)
					{
						if (check <= 54)
						{
							type = 9;
						}
						else if (check <= 77)
						{
							type = 8;
						}
						else
						{
							type = 7;
						}
						break;
					}
					case 8485: // prize-winning fishing lure
					{
						if (check <= 33)
						{
							type = 7;
						}
						else if (check <= 66)
						{
							type = 8;
						}
						else
						{
							type = 9;
						}
						break;
					}
				}
			}
		}
		
		return type;
	}
	
	private int getRandomFishLvl()
	{
		int skillLevel = getSkillLevel(1315 /* Fishing Expertise */);
		final BuffInfo info = getEffectList().getBuffInfoBySkillId(2274 /* Fisherman's Potion */);
		if (info != null)
		{
			skillLevel = (int) info.getSkill().getPower();
		}
		
		if (skillLevel <= 0)
		{
			return 1;
		}
		
		int randomlvl;
		final int check = Rnd.get(100);
		if (check <= 50)
		{
			randomlvl = skillLevel;
		}
		else if (check <= 85)
		{
			randomlvl = skillLevel - 1;
			if (randomlvl <= 0)
			{
				randomlvl = 1;
			}
		}
		else
		{
			randomlvl = skillLevel + 1;
			if (randomlvl > 27)
			{
				randomlvl = 27;
			}
		}
		
		return randomlvl;
	}
	
	public void startFishCombat(boolean isNoob, boolean isUpperGrade)
	{
		_fishCombat = new Fishing(this, _fish, isNoob, isUpperGrade, _lure.getId());
	}
	
	public void endFishing(boolean win)
	{
		_fishing = false;
		_fishX = 0;
		_fishY = 0;
		_fishZ = 0;
		
		// broadcastUserInfo();
		if (_fishCombat == null)
		{
			sendPacket(SystemMessageId.BAITS_HAVE_BEEN_LOST_BECAUSE_THE_FISH_GOT_AWAY);
		}
		
		_fishCombat = null;
		_lure = null;
		
		// Ends fishing
		broadcastPacket(new ExFishingEnd(win, this));
		sendPacket(SystemMessageId.YOU_REEL_YOUR_LINE_IN_AND_STOP_FISHING);
		setImmobilized(false);
		stopLookingForFishTask();
	}
	
	public Fishing getFishCombat()
	{
		return _fishCombat;
	}
	
	public int getFishX()
	{
		return _fishX;
	}
	
	public int getFishY()
	{
		return _fishY;
	}
	
	public int getFishZ()
	{
		return _fishZ;
	}
	
	public void setLure(Item lure)
	{
		_lure = lure;
	}
	
	public Item getLure()
	{
		return _lure;
	}
	
	public int getInventoryLimit()
	{
		int ivlim;
		if (isGM())
		{
			ivlim = PlayerConfig.INVENTORY_MAXIMUM_GM;
		}
		else
		{
			ivlim = getRace() == Race.DWARF ? PlayerConfig.INVENTORY_MAXIMUM_DWARF : PlayerConfig.INVENTORY_MAXIMUM_NO_DWARF;
		}
		
		return ivlim += (int) getStat().calcStat(Stat.INV_LIM, 0, null, null);
	}
	
	public int getWareHouseLimit()
	{
		return (getRace() == Race.DWARF ? PlayerConfig.WAREHOUSE_SLOTS_DWARF : PlayerConfig.WAREHOUSE_SLOTS_NO_DWARF) + (int) getStat().calcStat(Stat.WH_LIM, 0, null, null);
	}
	
	public int getPrivateSellStoreLimit()
	{
		return (getRace() == Race.DWARF ? PlayerConfig.MAX_PVTSTORESELL_SLOTS_DWARF : PlayerConfig.MAX_PVTSTORESELL_SLOTS_OTHER) + (int) getStat().calcStat(Stat.P_SELL_LIM, 0, null, null);
	}
	
	public int getPrivateBuyStoreLimit()
	{
		return (getRace() == Race.DWARF ? PlayerConfig.MAX_PVTSTOREBUY_SLOTS_DWARF : PlayerConfig.MAX_PVTSTOREBUY_SLOTS_OTHER) + (int) getStat().calcStat(Stat.P_BUY_LIM, 0, null, null);
	}
	
	public int getDwarfRecipeLimit()
	{
		return PlayerConfig.DWARF_RECIPE_LIMIT + (int) getStat().calcStat(Stat.REC_D_LIM, 0, null, null);
	}
	
	public int getCommonRecipeLimit()
	{
		return PlayerConfig.COMMON_RECIPE_LIMIT + (int) getStat().calcStat(Stat.REC_C_LIM, 0, null, null);
	}
	
	/**
	 * @return Returns the mountNpcId.
	 */
	public int getMountNpcId()
	{
		return _mountNpcId;
	}
	
	/**
	 * @return Returns the mountLevel.
	 */
	public int getMountLevel()
	{
		return _mountLevel;
	}
	
	public void setMountObjectID(int newID)
	{
		_mountObjectID = newID;
	}
	
	public int getMountObjectID()
	{
		return _mountObjectID;
	}
	
	/**
	 * @return the current skill in use or return null.
	 */
	public SkillUseHolder getCurrentSkill()
	{
		return _currentSkill;
	}
	
	/**
	 * Create a new SkillUseHolder object and set the player _currentSkill.
	 * @param currentSkill
	 * @param ctrlPressed
	 * @param shiftPressed
	 */
	public void setCurrentSkill(Skill currentSkill, boolean ctrlPressed, boolean shiftPressed)
	{
		if (currentSkill == null)
		{
			_currentSkill = null;
			return;
		}
		
		_currentSkill = new SkillUseHolder(currentSkill, ctrlPressed, shiftPressed);
	}
	
	/**
	 * @return the current pet skill in use or return null.
	 */
	public SkillUseHolder getCurrentPetSkill()
	{
		return _currentPetSkill;
	}
	
	/**
	 * Create a new SkillUseHolder object and set the player _currentPetSkill.
	 * @param currentSkill
	 * @param ctrlPressed
	 * @param shiftPressed
	 */
	public void setCurrentPetSkill(Skill currentSkill, boolean ctrlPressed, boolean shiftPressed)
	{
		if (currentSkill == null)
		{
			_currentPetSkill = null;
			return;
		}
		
		_currentPetSkill = new SkillUseHolder(currentSkill, ctrlPressed, shiftPressed);
	}
	
	public SkillUseHolder getQueuedSkill()
	{
		return _queuedSkill;
	}
	
	/**
	 * Create a new SkillUseHolder object and queue it in the player _queuedSkill.
	 * @param queuedSkill
	 * @param ctrlPressed
	 * @param shiftPressed
	 */
	public void setQueuedSkill(Skill queuedSkill, boolean ctrlPressed, boolean shiftPressed)
	{
		if (queuedSkill == null)
		{
			_queuedSkill = null;
			return;
		}
		
		_queuedSkill = new SkillUseHolder(queuedSkill, ctrlPressed, shiftPressed);
	}
	
	/**
	 * @return {@code true} if player is jailed, {@code false} otherwise.
	 */
	public boolean isJailed()
	{
		return PunishmentManager.getInstance().hasPunishment(getObjectId(), PunishmentAffect.CHARACTER, PunishmentType.JAIL) //
			|| PunishmentManager.getInstance().hasPunishment(getAccountName(), PunishmentAffect.ACCOUNT, PunishmentType.JAIL) //
			|| PunishmentManager.getInstance().hasPunishment(getIPAddress(), PunishmentAffect.IP, PunishmentType.JAIL) //
			|| ((_client != null) && (_client.getHardwareInfo() != null) && PunishmentManager.getInstance().hasPunishment(_client.getHardwareInfo().getMacAddress(), PunishmentAffect.HWID, PunishmentType.JAIL));
	}
	
	/**
	 * @return {@code true} if player is chat banned, {@code false} otherwise.
	 */
	public boolean isChatBanned()
	{
		return PunishmentManager.getInstance().hasPunishment(getObjectId(), PunishmentAffect.CHARACTER, PunishmentType.CHAT_BAN) //
			|| PunishmentManager.getInstance().hasPunishment(getAccountName(), PunishmentAffect.ACCOUNT, PunishmentType.CHAT_BAN) //
			|| PunishmentManager.getInstance().hasPunishment(getIPAddress(), PunishmentAffect.IP, PunishmentType.CHAT_BAN) //
			|| ((_client != null) && (_client.getHardwareInfo() != null) && PunishmentManager.getInstance().hasPunishment(_client.getHardwareInfo().getMacAddress(), PunishmentAffect.HWID, PunishmentType.CHAT_BAN));
	}
	
	public void startFameTask(long delay, int fameFixRate)
	{
		if (!PlayerConfig.FAME_SYSTEM_ENABLED)
		{
			return;
		}
		
		if ((getLevel() < 40) || (getPlayerClass().level() < 2))
		{
			return;
		}
		
		if (_fameTask == null)
		{
			_fameTask = ThreadPool.scheduleAtFixedRate(new FameTask(this, fameFixRate), delay, delay);
		}
	}
	
	public void stopFameTask()
	{
		if (_fameTask == null)
		{
			return;
		}
		
		_fameTask.cancel(false);
		_fameTask = null;
	}
	
	public void startVitalityTask()
	{
		if (PlayerConfig.ENABLE_VITALITY && (_vitalityTask == null))
		{
			_vitalityTask = ThreadPool.scheduleAtFixedRate(new VitalityTask(this), 1000, 60000);
		}
	}
	
	public void stopVitalityTask()
	{
		if (_vitalityTask != null)
		{
			_vitalityTask.cancel(false);
			_vitalityTask = null;
		}
	}
	
	public int getPowerGrade()
	{
		return _powerGrade;
	}
	
	public void setPowerGrade(int power)
	{
		_powerGrade = power;
	}
	
	public boolean isCursedWeaponEquipped()
	{
		return _cursedWeaponEquippedId != 0;
	}
	
	public void setCursedWeaponEquippedId(int value)
	{
		_cursedWeaponEquippedId = value;
	}
	
	public int getCursedWeaponEquippedId()
	{
		return _cursedWeaponEquippedId;
	}
	
	public int getDeathPenaltyBuffLevel()
	{
		return _deathPenaltyBuffLevel;
	}
	
	public void setDeathPenaltyBuffLevel(int level)
	{
		_deathPenaltyBuffLevel = level;
	}
	
	public void calculateDeathPenaltyBuffLevel(Creature killer)
	{
		if (killer == null)
		{
			LOGGER.warning(this + " called calculateDeathPenaltyBuffLevel with killer null!");
			return;
		}
		
		if (isResurrectSpecialAffected() || isLucky() || isOnEvent() || isInsideZone(ZoneId.PVP) || isInsideZone(ZoneId.SIEGE) || isGM())
		{
			return;
		}
		
		double percent = 1.0;
		if (killer.isRaid())
		{
			percent *= calcStat(Stat.REDUCE_DEATH_PENALTY_BY_RAID, 1);
		}
		else if (killer.isMonster())
		{
			percent *= calcStat(Stat.REDUCE_DEATH_PENALTY_BY_MOB, 1);
		}
		else if (killer.isPlayable())
		{
			percent *= calcStat(Stat.REDUCE_DEATH_PENALTY_BY_PVP, 1);
		}
		
		if ((Rnd.get(1, 100) <= ((PlayerConfig.DEATH_PENALTY_CHANCE) * percent)) && (!killer.isPlayable() || (getKarma() > 0)))
		{
			increaseDeathPenaltyBuffLevel();
		}
	}
	
	public void increaseDeathPenaltyBuffLevel()
	{
		if (_deathPenaltyBuffLevel >= 15)
		{
			return;
		}
		
		if (_deathPenaltyBuffLevel != 0)
		{
			final Skill skill = SkillData.getInstance().getSkill(5076, getDeathPenaltyBuffLevel());
			if (skill != null)
			{
				removeSkill(skill, true);
			}
		}
		
		_deathPenaltyBuffLevel++;
		addSkill(SkillData.getInstance().getSkill(5076, getDeathPenaltyBuffLevel()), false);
		sendPacket(new EtcStatusUpdate(this));
		final SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_DEATH_PENALTY_IS_NOW_LEVEL_S1);
		sm.addInt(_deathPenaltyBuffLevel);
		sendPacket(sm);
	}
	
	public void reduceDeathPenaltyBuffLevel()
	{
		if (_deathPenaltyBuffLevel <= 0)
		{
			return;
		}
		
		final Skill skill = SkillData.getInstance().getSkill(5076, getDeathPenaltyBuffLevel());
		if (skill != null)
		{
			removeSkill(skill, true);
		}
		
		_deathPenaltyBuffLevel--;
		
		if (_deathPenaltyBuffLevel > 0)
		{
			addSkill(SkillData.getInstance().getSkill(5076, getDeathPenaltyBuffLevel()), false);
			sendPacket(new EtcStatusUpdate(this));
			final SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_DEATH_PENALTY_IS_NOW_LEVEL_S1);
			sm.addInt(_deathPenaltyBuffLevel);
			sendPacket(sm);
		}
		else
		{
			sendPacket(new EtcStatusUpdate(this));
			sendPacket(SystemMessageId.YOUR_DEATH_PENALTY_HAS_BEEN_LIFTED);
		}
	}
	
	public void restoreDeathPenaltyBuffLevel()
	{
		if (_deathPenaltyBuffLevel > 0)
		{
			addSkill(SkillData.getInstance().getSkill(5076, getDeathPenaltyBuffLevel()), false);
		}
	}
	
	@Override
	public void sendDamageMessage(Creature target, int damage, boolean mcrit, boolean pcrit, boolean miss)
	{
		// Check if hit is missed
		if (miss)
		{
			sendPacket(SystemMessageId.YOU_HAVE_MISSED);
			return;
		}
		
		// Check if hit is critical
		if (pcrit)
		{
			sendPacket(SystemMessageId.CRITICAL_HIT);
		}
		
		if (mcrit)
		{
			sendPacket(SystemMessageId.MAGIC_CRITICAL_HIT);
		}
		
		if (isInOlympiadMode() && target.isPlayer() && target.asPlayer().isInOlympiadMode() && (target.asPlayer().getOlympiadGameId() == getOlympiadGameId()))
		{
			Olympiad.getInstance().notifyCompetitorDamage(this, damage, getOlympiadGameId());
		}
		
		if (this != target)
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HIT_FOR_S1_DAMAGE);
			sm.addInt(damage);
			sendPacket(sm);
		}
	}
	
	/**
	 * @param npcId
	 */
	public void setAgathionId(int npcId)
	{
		_agathionId = npcId;
	}
	
	/**
	 * @return
	 */
	public int getAgathionId()
	{
		return _agathionId;
	}
	
	public int getVitalityPoints()
	{
		return getStat().getVitalityPoints();
	}
	
	/**
	 * @return Vitality Level
	 */
	public int getVitalityLevel()
	{
		return getStat().getVitalityLevel();
	}
	
	public void setVitalityPoints(int points, boolean quiet)
	{
		getStat().setVitalityPoints(points, quiet);
	}
	
	public void updateVitalityPoints(float points, boolean useRates, boolean quiet)
	{
		getStat().updateVitalityPoints(points, useRates, quiet);
	}
	
	public void checkItemRestriction()
	{
		for (int i = 0; i < Inventory.PAPERDOLL_TOTALSLOTS; i++)
		{
			final Item equippedItem = _inventory.getPaperdollItem(i);
			if ((equippedItem != null) && !equippedItem.getTemplate().checkCondition(this, this, false))
			{
				_inventory.unEquipItemInSlot(i);
				
				final InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(equippedItem);
				sendInventoryUpdate(iu);
				
				SystemMessage sm = null;
				if (equippedItem.getTemplate().getBodyPart() == BodyPart.BACK)
				{
					sendMessage("Your cloak has been unequipped because your armor set is no longer complete.");
					return;
				}
				
				if (equippedItem.isEnchanted())
				{
					sm = new SystemMessage(SystemMessageId.THE_EQUIPMENT_S1_S2_HAS_BEEN_REMOVED);
					sm.addInt(equippedItem.getEnchantLevel());
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S1_HAS_BEEN_DISARMED);
				}
				
				sm.addItemName(equippedItem);
				sendPacket(sm);
			}
		}
	}
	
	protected void startFeed(int npcId)
	{
		_canFeed = npcId > 0;
		if (!isMounted())
		{
			return;
		}
		
		if (hasSummon())
		{
			setCurrentFeed(_summon.asPet().getCurrentFed());
			_controlItemId = _summon.getControlObjectId();
			sendPacket(new SetupGauge(getObjectId(), 3, (_curFeed * 10000) / getFeedConsume(), (getMaxFeed() * 10000) / getFeedConsume()));
			if (!isDead())
			{
				_mountFeedTask = ThreadPool.scheduleAtFixedRate(new PetFeedTask(this), 10000, 10000);
			}
		}
		else if (_canFeed)
		{
			setCurrentFeed(getMaxFeed());
			sendPacket(new SetupGauge(getObjectId(), 3, (_curFeed * 10000) / getFeedConsume(), (getMaxFeed() * 10000) / getFeedConsume()));
			if (!isDead())
			{
				_mountFeedTask = ThreadPool.scheduleAtFixedRate(new PetFeedTask(this), 10000, 10000);
			}
		}
	}
	
	public void stopFeed()
	{
		if (_mountFeedTask == null)
		{
			return;
		}
		
		_mountFeedTask.cancel(false);
		_mountFeedTask = null;
	}
	
	private final PetLevelData getPetLevelData(int npcId)
	{
		if (_leveldata == null)
		{
			_leveldata = PetDataTable.getInstance().getPetData(npcId).getPetLevelData(getMountLevel());
		}
		
		return _leveldata;
	}
	
	public int getCurrentFeed()
	{
		return _curFeed;
	}
	
	public int getFeedConsume()
	{
		// if pet is attacking
		if (isAttackingNow())
		{
			return getPetLevelData(_mountNpcId).getPetFeedBattle();
		}
		
		return getPetLevelData(_mountNpcId).getPetFeedNormal();
	}
	
	public void setCurrentFeed(int num)
	{
		final boolean lastHungryState = isHungry();
		_curFeed = num > getMaxFeed() ? getMaxFeed() : num;
		sendPacket(new SetupGauge(getObjectId(), 3, (_curFeed * 10000) / getFeedConsume(), (getMaxFeed() * 10000) / getFeedConsume()));
		
		// broadcast move speed change when strider becomes hungry / full
		if (lastHungryState != isHungry())
		{
			broadcastUserInfo();
		}
	}
	
	private int getMaxFeed()
	{
		return getPetLevelData(_mountNpcId).getPetMaxFeed();
	}
	
	public boolean isHungry()
	{
		return _canFeed && (getCurrentFeed() < ((PetDataTable.getInstance().getPetData(getMountNpcId()).getHungryLimit() / 100f) * getPetLevelData(getMountNpcId()).getPetMaxFeed()));
	}
	
	public void enteredNoLanding(int delay)
	{
		_dismountTask = ThreadPool.schedule(new DismountTask(this), delay * 1000);
	}
	
	public void exitedNoLanding()
	{
		if (_dismountTask == null)
		{
			return;
		}
		
		_dismountTask.cancel(true);
		_dismountTask = null;
	}
	
	public void storePetFood(int petId)
	{
		if ((_controlItemId == 0) || (petId == 0))
		{
			return;
		}
		
		final String req = "UPDATE pets SET fed=? WHERE item_obj_id = ?";
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(req))
		{
			ps.setInt(1, _curFeed);
			ps.setInt(2, _controlItemId);
			ps.executeUpdate();
			_controlItemId = 0;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Failed to store Pet [NpcId: " + petId + "] data", e);
		}
	}
	
	public void setInSiege(boolean value)
	{
		_isInSiege = value;
	}
	
	public boolean isInSiege()
	{
		return _isInSiege;
	}
	
	/**
	 * @param isInHideoutSiege sets the value of {@link #_isInHideoutSiege}.
	 */
	public void setInHideoutSiege(boolean isInHideoutSiege)
	{
		_isInHideoutSiege = isInHideoutSiege;
	}
	
	/**
	 * @return the value of {@link #_isInHideoutSiege}, {@code true} if the player is participing on a Hideout Siege, otherwise {@code false}.
	 */
	public boolean isInHideoutSiege()
	{
		return _isInHideoutSiege;
	}
	
	/**
	 * Returns the Number of Charges this Player got.
	 * @return
	 */
	public int getCharges()
	{
		return _charges.get();
	}
	
	public void increaseCharges(int count, int max)
	{
		if (_charges.get() >= max)
		{
			sendPacket(SystemMessageId.YOUR_FORCE_HAS_REACHED_MAXIMUM_CAPACITY);
			return;
		}
		
		// Charge clear task should be reset every time a charge is increased.
		restartChargeTask();
		
		if (_charges.addAndGet(count) >= max)
		{
			_charges.set(max);
			sendPacket(SystemMessageId.YOUR_FORCE_HAS_REACHED_MAXIMUM_CAPACITY);
		}
		else
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_FORCE_HAS_INCREASED_TO_S1_LEVEL);
			sm.addInt(_charges.get());
			sendPacket(sm);
		}
		
		sendPacket(new EtcStatusUpdate(this));
	}
	
	public boolean decreaseCharges(int count)
	{
		if (_charges.get() < count)
		{
			return false;
		}
		
		// Charge clear task should be reset every time a charge is decreased and stopped when charges become 0.
		if (_charges.addAndGet(-count) == 0)
		{
			stopChargeTask();
		}
		else
		{
			restartChargeTask();
		}
		
		sendPacket(new EtcStatusUpdate(this));
		return true;
	}
	
	public void clearCharges()
	{
		_charges.set(0);
		sendPacket(new EtcStatusUpdate(this));
	}
	
	/**
	 * Starts/Restarts the ChargeTask to Clear Charges after 10 Mins.
	 */
	private void restartChargeTask()
	{
		if (_chargeTask != null)
		{
			synchronized (this)
			{
				_chargeTask.cancel(false);
			}
		}
		
		_chargeTask = ThreadPool.schedule(new ResetChargesTask(this), 600000);
	}
	
	/**
	 * Stops the Charges Clearing Task.
	 */
	public void stopChargeTask()
	{
		if (_chargeTask == null)
		{
			return;
		}
		
		_chargeTask.cancel(false);
		_chargeTask = null;
	}
	
	@Override
	public void sendInfo(Player player)
	{
		if (isInBoat())
		{
			setXYZ(getBoat().getLocation());
			player.sendPacket(new CharInfo(this, isInvisible() && player.isGM()));
			// player.sendPacket(new ExBrExtraUserInfo(this));
			player.sendPacket(new GetOnVehicle(getObjectId(), getBoat().getObjectId(), _inVehiclePosition));
		}
		else
		{
			player.sendPacket(new CharInfo(this, isInvisible() && player.isGM()));
			// player.sendPacket(new ExBrExtraUserInfo(this));
		}
		
		final int relation = getRelation(player);
		final boolean isAutoAttackable = isAutoAttackable(player);
		final RelationCache cache = getKnownRelations().get(player.getObjectId());
		if ((cache == null) || (cache.getRelation() != relation) || (cache.isAutoAttackable() != isAutoAttackable))
		{
			player.sendPacket(new RelationChanged(this, relation, isAutoAttackable));
			if (hasSummon())
			{
				player.sendPacket(new RelationChanged(_summon, relation, isAutoAttackable));
			}
			
			getKnownRelations().put(player.getObjectId(), new RelationCache(relation, isAutoAttackable));
		}
		
		switch (_privateStoreType)
		{
			case SELL:
			{
				player.sendPacket(new PrivateStoreMsgSell(this));
				break;
			}
			case PACKAGE_SELL:
			{
				// player.sendPacket(new ExPrivateStoreSetWholeMsg(this));
				player.sendPacket(new PrivateStoreMsgSell(this));
				break;
			}
			case BUY:
			{
				player.sendPacket(new PrivateStoreMsgBuy(this));
				break;
			}
			case MANUFACTURE:
			{
				player.sendPacket(new RecipeShopMsg(this));
				break;
			}
		}
	}
	
	public boolean isAllowedToEnchantSkills()
	{
		if (_subclassLock)
		{
			return false;
		}
		
		if (AttackStanceTaskManager.getInstance().hasAttackStanceTask(this))
		{
			return false;
		}
		
		if (isCastingNow() || isCastingSimultaneouslyNow())
		{
			return false;
		}
		
		if (isInBoat())
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * Set the _createDate of the Player.
	 * @param createDate
	 */
	public void setCreateDate(Calendar createDate)
	{
		_createDate = createDate;
	}
	
	/**
	 * @return the _createDate of the Player.
	 */
	public Calendar getCreateDate()
	{
		return _createDate;
	}
	
	public Collection<Integer> getFriendList()
	{
		return _friendList;
	}
	
	public void selectFriend(Integer friendId)
	{
		if (!_selectedFriendList.contains(friendId))
		{
			_selectedFriendList.add(friendId);
		}
	}
	
	public void deselectFriend(Integer friendId)
	{
		if (_selectedFriendList.contains(friendId))
		{
			_selectedFriendList.remove(friendId);
		}
	}
	
	public List<Integer> getSelectedFriendList()
	{
		return _selectedFriendList;
	}
	
	public void restoreFriendList()
	{
		_friendList.clear();
		
		final String sqlQuery = "SELECT friendId FROM character_friends WHERE charId=? AND relation=0";
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(sqlQuery))
		{
			ps.setInt(1, getObjectId());
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					final int friendId = rs.getInt("friendId");
					if (friendId == getObjectId())
					{
						continue;
					}
					
					_friendList.add(friendId);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Error found in " + getName() + "'s FriendList: " + e.getMessage(), e);
		}
	}
	
	private void notifyFriends()
	{
		final FriendStatusPacket pkt = new FriendStatusPacket(getObjectId());
		for (int id : _friendList)
		{
			final Player friend = World.getInstance().getPlayer(id);
			if (friend != null)
			{
				friend.sendPacket(pkt);
			}
		}
	}
	
	public void selectBlock(Integer friendId)
	{
		if (!_selectedBlocksList.contains(friendId))
		{
			_selectedBlocksList.add(friendId);
		}
	}
	
	public void deselectBlock(Integer friendId)
	{
		if (_selectedBlocksList.contains(friendId))
		{
			_selectedBlocksList.remove(friendId);
		}
	}
	
	public List<Integer> getSelectedBlocksList()
	{
		return _selectedBlocksList;
	}
	
	/**
	 * Verify if this player is in silence mode.
	 * @return the {@code true} if this player is in silence mode, {@code false} otherwise
	 */
	public boolean isSilenceMode()
	{
		return _silenceMode;
	}
	
	/**
	 * While at silenceMode, checks if this player blocks PMs for this user
	 * @param playerObjId the player object Id
	 * @return {@code true} if the given Id is not excluded and this player is in silence mode, {@code false} otherwise
	 */
	public boolean isSilenceMode(int playerObjId)
	{
		if (PlayerConfig.SILENCE_MODE_EXCLUDE && _silenceMode && (_silenceModeExcluded != null))
		{
			return !_silenceModeExcluded.contains(playerObjId);
		}
		
		return _silenceMode;
	}
	
	/**
	 * Set the silence mode.
	 * @param mode the value
	 */
	public void setSilenceMode(boolean mode)
	{
		_silenceMode = mode;
		if (_silenceModeExcluded != null)
		{
			_silenceModeExcluded.clear(); // Clear the excluded list on each setSilenceMode
		}
		
		sendPacket(new EtcStatusUpdate(this));
	}
	
	/**
	 * Add a player to the "excluded silence mode" list.
	 * @param playerObjId the player's object Id
	 */
	public void addSilenceModeExcluded(int playerObjId)
	{
		if (_silenceModeExcluded == null)
		{
			_silenceModeExcluded = new ArrayList<>(1);
		}
		
		_silenceModeExcluded.add(playerObjId);
	}
	
	private void storeRecipeShopList()
	{
		if (hasManufactureShop())
		{
			try (Connection con = DatabaseFactory.getConnection())
			{
				try (PreparedStatement st = con.prepareStatement(DELETE_CHAR_RECIPE_SHOP))
				{
					st.setInt(1, getObjectId());
					st.execute();
				}
				
				try (PreparedStatement st = con.prepareStatement(INSERT_CHAR_RECIPE_SHOP))
				{
					final AtomicInteger slot = new AtomicInteger(1);
					for (ManufactureItem item : _manufactureItems.values())
					{
						st.setInt(1, getObjectId());
						st.setInt(2, item.getRecipeId());
						st.setLong(3, item.getCost());
						st.setInt(4, slot.getAndIncrement());
						st.addBatch();
					}
					
					st.executeBatch();
					// No need to call con.commit() as HikariCP autocommit is true.
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "Could not store recipe shop for playerId " + getObjectId() + ": ", e);
			}
		}
	}
	
	private void restoreRecipeShopList()
	{
		if (_manufactureItems != null)
		{
			_manufactureItems.clear();
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(RESTORE_CHAR_RECIPE_SHOP))
		{
			ps.setInt(1, getObjectId());
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					getManufactureItems().put(rs.getInt("recipeId"), new ManufactureItem(rs.getInt("recipeId"), rs.getInt("price")));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not restore recipe shop list data for playerId: " + getObjectId(), e);
		}
	}
	
	public float getCollisionRadius()
	{
		if (isMounted() && (_mountNpcId > 0))
		{
			return NpcData.getInstance().getTemplate(getMountNpcId()).getFCollisionRadius();
		}
		
		return _appearance.isFemale() ? getBaseTemplate().getFCollisionRadiusFemale() : getBaseTemplate().getFCollisionRadius();
	}
	
	public float getCollisionHeight()
	{
		if (isMounted() && (_mountNpcId > 0))
		{
			return NpcData.getInstance().getTemplate(getMountNpcId()).getFCollisionHeight();
		}
		
		return _appearance.isFemale() ? getBaseTemplate().getFCollisionHeightFemale() : getBaseTemplate().getFCollisionHeight();
	}
	
	public int getClientX()
	{
		return _clientX;
	}
	
	public int getClientY()
	{
		return _clientY;
	}
	
	public int getClientZ()
	{
		return _clientZ;
	}
	
	public int getClientHeading()
	{
		return _clientHeading;
	}
	
	public void setClientX(int value)
	{
		_clientX = value;
	}
	
	public void setClientY(int value)
	{
		_clientY = value;
	}
	
	public void setClientZ(int value)
	{
		_clientZ = value;
	}
	
	public void setClientHeading(int value)
	{
		_clientHeading = value;
	}
	
	/**
	 * @param z
	 * @return true if character falling now on the start of fall return false for correct coord sync!
	 */
	public boolean isFalling(int z)
	{
		if (isDead() || isFlying() || isInsideZone(ZoneId.WATER))
		{
			return false;
		}
		
		if ((_fallingTimestamp != 0) && (System.currentTimeMillis() < _fallingTimestamp))
		{
			return true;
		}
		
		final int deltaZ = getZ() - z;
		if (deltaZ <= getBaseTemplate().getSafeFallHeight())
		{
			_fallingTimestamp = 0;
			return false;
		}
		
		// If there is no geodata loaded for the place we are, client Z correction might cause falling damage.
		if (!GeoEngine.getInstance().hasGeo(getX(), getY()))
		{
			_fallingTimestamp = 0;
			return false;
		}
		
		if (_fallingDamage == 0)
		{
			_fallingDamage = (int) Formulas.calcFallDam(this, deltaZ);
		}
		
		if (_fallingDamageTask != null)
		{
			_fallingDamageTask.cancel(true);
		}
		
		_fallingDamageTask = ThreadPool.schedule(() ->
		{
			if ((_fallingDamage > 0) && !isInvul())
			{
				reduceCurrentHp(Math.min(_fallingDamage, getCurrentHp() - 1), null, false, true, null);
				final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_RECEIVED_S1_DAMAGE_FROM_TAKING_A_HIGH_FALL);
				sm.addInt(_fallingDamage);
				sendPacket(sm);
			}
			
			_fallingDamage = 0;
			_fallingDamageTask = null;
		}, 1500);
		
		// Prevent falling under ground.
		sendPacket(new ValidateLocation(this));
		setFalling();
		
		return false;
	}
	
	/**
	 * Set falling timestamp
	 */
	public void setFalling()
	{
		_fallingTimestamp = System.currentTimeMillis() + FALLING_VALIDATION_DELAY;
	}
	
	/**
	 * Update last item auction request timestamp to current
	 */
	public void updateLastItemAuctionRequest()
	{
		_lastItemAuctionInfoRequest = System.currentTimeMillis();
	}
	
	/**
	 * @return true if receiving item auction requests<br>
	 *         (last request was in 2 seconds before)
	 */
	public boolean isItemAuctionPolling()
	{
		return (System.currentTimeMillis() - _lastItemAuctionInfoRequest) < 2000;
	}
	
	public String getHtmlPrefix()
	{
		return MultilingualSupportConfig.MULTILANG_ENABLE ? _htmlPrefix : "";
	}
	
	public String getLang()
	{
		return _lang;
	}
	
	public boolean setLang(String lang)
	{
		boolean result = false;
		if (MultilingualSupportConfig.MULTILANG_ENABLE)
		{
			if (MultilingualSupportConfig.MULTILANG_ALLOWED.contains(lang))
			{
				_lang = lang;
				result = true;
			}
			else
			{
				_lang = MultilingualSupportConfig.MULTILANG_DEFAULT;
			}
			
			_htmlPrefix = _lang.equals("en") ? "" : "data/lang/" + _lang + "/";
		}
		else
		{
			_lang = null;
			_htmlPrefix = "";
		}
		
		return result;
	}
	
	public long getOfflineStartTime()
	{
		return _offlineShopStart;
	}
	
	public void setOfflineStartTime(long time)
	{
		_offlineShopStart = time;
	}
	
	public int getPcCafePoints()
	{
		return _pcCafePoints;
	}
	
	public void setPcCafePoints(int count)
	{
		_pcCafePoints = count < PremiumSystemConfig.PC_CAFE_MAX_POINTS ? count : PremiumSystemConfig.PC_CAFE_MAX_POINTS;
	}
	
	/**
	 * Remove player from BossZones (used on char logout/exit)
	 */
	public void removeFromBossZone()
	{
		try
		{
			for (BossZone zone : GrandBossManager.getInstance().getZones().values())
			{
				zone.removePlayer(this);
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception on removeFromBossZone(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Check all player skills for skill level. If player level is lower than skill learn level - 9, skill level is decreased to next possible level.
	 */
	public void checkPlayerSkills()
	{
		for (Entry<Integer, Skill> e : getSkills().entrySet())
		{
			final SkillLearn learn = SkillTreeData.getInstance().getClassSkill(e.getKey(), e.getValue().getLevel() % 100, getPlayerClass());
			if (learn != null)
			{
				final int levelDiff = e.getKey() == CommonSkill.EXPERTISE.getId() ? 0 : 9;
				if (getLevel() < (learn.getGetLevel() - levelDiff))
				{
					deacreaseSkillLevel(e.getValue(), levelDiff);
				}
			}
		}
	}
	
	private void deacreaseSkillLevel(Skill skill, int levelDiff)
	{
		int nextLevel = -1;
		final Map<Integer, SkillLearn> skillTree = SkillTreeData.getInstance().getCompleteClassSkillTree(getPlayerClass());
		for (SkillLearn sl : skillTree.values())
		{
			if ((sl.getSkillId() == skill.getId()) && (nextLevel < sl.getSkillLevel()) && (getLevel() >= (sl.getGetLevel() - levelDiff)))
			{
				nextLevel = sl.getSkillLevel(); // next possible skill level
			}
		}
		
		if (nextLevel == -1)
		{
			LOGGER.info("Removing skill " + skill + " from " + this);
			removeSkill(skill, true); // there is no lower skill
		}
		else
		{
			LOGGER.info("Decreasing skill " + skill + " to " + nextLevel + " for " + this);
			addSkill(SkillData.getInstance().getSkill(skill.getId(), nextLevel), true); // replace with lower one
		}
	}
	
	public boolean canMakeSocialAction()
	{
		return (_privateStoreType == PrivateStoreType.NONE) && (getActiveRequester() == null) && !isAlikeDead() && !isAllSkillsDisabled() && !isCastingNow() && !isCastingSimultaneouslyNow() && (getAI().getIntention() == Intention.IDLE);
	}
	
	public void setMultiSocialAction(int id, int targetId)
	{
		_multiSociaAction = id;
		_multiSocialTarget = targetId;
	}
	
	public int getMultiSociaAction()
	{
		return _multiSociaAction;
	}
	
	public int getMultiSocialTarget()
	{
		return _multiSocialTarget;
	}
	
	public boolean canAttackCreature(Creature creature)
	{
		if (creature == null)
		{
			return false;
		}
		
		if (creature.isAttackable())
		{
			return true;
		}
		
		if (creature.isPlayable())
		{
			if (creature.isInsideZone(ZoneId.PVP) && !creature.isInsideZone(ZoneId.SIEGE))
			{
				return true;
			}
			
			Player target;
			if (creature.isSummon())
			{
				target = creature.asSummon().getOwner();
			}
			else
			{
				target = creature.asPlayer();
			}
			
			if (isInDuel() && target.isInDuel() && (target.getDuelId() == getDuelId()))
			{
				return true;
			}
			else if (isInParty() && target.isInParty())
			{
				if (getParty() == target.getParty())
				{
					return false;
				}
				
				if (((getParty().getCommandChannel() != null) || (target.getParty().getCommandChannel() != null)) && (getParty().getCommandChannel() == target.getParty().getCommandChannel()))
				{
					return false;
				}
			}
			else if ((getClan() != null) && (target.getClan() != null))
			{
				if (getClanId() == target.getClanId())
				{
					return false;
				}
				
				if (((getAllyId() > 0) || (target.getAllyId() > 0)) && (getAllyId() == target.getAllyId()))
				{
					return false;
				}
				
				if (getClan().isAtWarWith(target.getClan().getId()) && target.getClan().isAtWarWith(getClan().getId()))
				{
					return true;
				}
			}
			else if ((getClan() == null) || (target.getClan() == null))
			{
				if ((target.getPvpFlag() == 0) && (target.getKarma() == 0))
				{
					return false;
				}
			}
		}
		
		return true;
	}
	
	/**
	 * Test if player inventory is under 90% capacity
	 * @param includeQuestInv check also quest inventory
	 * @return
	 */
	public boolean isInventoryUnder90(boolean includeQuestInv)
	{
		return (includeQuestInv ? _inventory.getSize() : _inventory.getNonQuestSize()) <= (getInventoryLimit() * 0.9);
	}
	
	/**
	 * Test if player inventory is under 80% capacity
	 * @param includeQuestInv check also quest inventory
	 * @return
	 */
	public boolean isInventoryUnder80(boolean includeQuestInv)
	{
		return (includeQuestInv ? _inventory.getSize() : _inventory.getNonQuestSize()) <= (getInventoryLimit() * 0.8);
	}
	
	public boolean havePetInvItems()
	{
		return _petItems;
	}
	
	public void setPetInvItems(boolean haveit)
	{
		_petItems = haveit;
	}
	
	/**
	 * Restore Pet's inventory items from database.
	 */
	private void restorePetInventoryItems()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT object_id FROM `items` WHERE `owner_id`=? AND (`loc`='PET' OR `loc`='PET_EQUIP') LIMIT 1;"))
		{
			ps.setInt(1, getObjectId());
			try (ResultSet rs = ps.executeQuery())
			{
				setPetInvItems(rs.next() && (rs.getInt("object_id") > 0));
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not check Items in Pet Inventory for playerId: " + getObjectId(), e);
		}
	}
	
	public String getAdminConfirmCmd()
	{
		return _adminConfirmCmd;
	}
	
	public void setAdminConfirmCmd(String adminConfirmCmd)
	{
		_adminConfirmCmd = adminConfirmCmd;
	}
	
	public void setPremiumStatus(boolean premiumStatus)
	{
		_premiumStatus = premiumStatus;
	}
	
	public boolean hasPremiumStatus()
	{
		return PremiumSystemConfig.PREMIUM_SYSTEM_ENABLED && _premiumStatus;
	}
	
	public void setLastPetitionGmName(String gmName)
	{
		_lastPetitionGmName = gmName;
	}
	
	public String getLastPetitionGmName()
	{
		return _lastPetitionGmName;
	}
	
	public ContactList getContactList()
	{
		return _contactList;
	}
	
	public long getNotMoveUntil()
	{
		return _notMoveUntil;
	}
	
	public void updateNotMoveUntil()
	{
		_notMoveUntil = System.currentTimeMillis() + PlayerConfig.PLAYER_MOVEMENT_BLOCK_TIME;
	}
	
	@Override
	public boolean isPlayer()
	{
		return true;
	}
	
	@Override
	public Player asPlayer()
	{
		return this;
	}
	
	@Override
	public boolean isChargedShot(ShotType type)
	{
		final Item weapon = getActiveWeaponInstance();
		return (weapon != null) && weapon.isChargedShot(type);
	}
	
	@Override
	public void setChargedShot(ShotType type, boolean charged)
	{
		final Item weapon = getActiveWeaponInstance();
		if (weapon != null)
		{
			weapon.setChargedShot(type, charged);
		}
	}
	
	/**
	 * @param skillId the display skill Id
	 * @return the custom skill
	 */
	public Skill getCustomSkill(int skillId)
	{
		return (_customSkills != null) ? _customSkills.get(skillId) : null;
	}
	
	/**
	 * Add a skill level to the custom skills map.
	 * @param skill the skill to add
	 */
	private void addCustomSkill(Skill skill)
	{
		if ((skill == null) || (skill.getDisplayId() == skill.getId()))
		{
			return;
		}
		
		if (_customSkills == null)
		{
			_customSkills = new ConcurrentHashMap<>();
		}
		
		_customSkills.put(skill.getDisplayId(), skill);
	}
	
	/**
	 * Remove a skill level from the custom skill map.
	 * @param skill the skill to remove
	 */
	private void removeCustomSkill(Skill skill)
	{
		if ((skill != null) && (_customSkills != null) && (skill.getDisplayId() != skill.getId()))
		{
			_customSkills.remove(skill.getDisplayId());
		}
	}
	
	/**
	 * @return {@code true} if current player can revive and shows 'To Village' button upon death, {@code false} otherwise.
	 */
	@Override
	public boolean canRevive()
	{
		return _canRevive;
	}
	
	/**
	 * This method can prevent from displaying 'To Village' button upon death.
	 * @param value
	 */
	@Override
	public void setCanRevive(boolean value)
	{
		_canRevive = value;
	}
	
	public boolean isRegisteredOnEvent()
	{
		return _isRegisteredOnEvent || _isOnEvent;
	}
	
	public void setRegisteredOnEvent(boolean value)
	{
		_isRegisteredOnEvent = value;
	}
	
	@Override
	public boolean isOnEvent()
	{
		return _isOnEvent;
	}
	
	public void setOnEvent(boolean value)
	{
		_isOnEvent = value;
	}
	
	public boolean isOnSoloEvent()
	{
		return _isOnSoloEvent;
	}
	
	public void setOnSoloEvent(boolean value)
	{
		_isOnSoloEvent = value;
	}
	
	public void setOriginalCpHpMp(double cp, double hp, double mp)
	{
		_originalCp = cp;
		_originalHp = hp;
		_originalMp = mp;
	}
	
	/**
	 * @return {@code true} if {@link PlayerVariables} instance is attached to current player's scripts, {@code false} otherwise.
	 */
	public boolean hasVariables()
	{
		return getScript(PlayerVariables.class) != null;
	}
	
	/**
	 * @return {@link PlayerVariables} instance containing parameters regarding player.
	 */
	public PlayerVariables getVariables()
	{
		final PlayerVariables vars = getScript(PlayerVariables.class);
		return vars != null ? vars : addScript(new PlayerVariables(getObjectId()));
	}
	
	/**
	 * @return {@code true} if {@link AccountVariables} instance is attached to current player's scripts, {@code false} otherwise.
	 */
	public boolean hasAccountVariables()
	{
		return getScript(AccountVariables.class) != null;
	}
	
	/**
	 * @return {@link AccountVariables} instance containing parameters regarding player.
	 */
	public AccountVariables getAccountVariables()
	{
		final AccountVariables vars = getScript(AccountVariables.class);
		return vars != null ? vars : addScript(new AccountVariables(getAccountName()));
	}
	
	@Override
	public int getId()
	{
		return getPlayerClass().getId();
	}
	
	public boolean isPartyBanned()
	{
		return PunishmentManager.getInstance().hasPunishment(getObjectId(), PunishmentAffect.CHARACTER, PunishmentType.PARTY_BAN);
	}
	
	/**
	 * @param act
	 * @return {@code true} if action was added successfully, {@code false} otherwise.
	 */
	public boolean addAction(PlayerAction act)
	{
		if (!hasAction(act))
		{
			_actionMask |= act.getMask();
			return true;
		}
		
		return false;
	}
	
	/**
	 * @param act
	 * @return {@code true} if action was removed successfully, {@code false} otherwise.
	 */
	public boolean removeAction(PlayerAction act)
	{
		if (hasAction(act))
		{
			_actionMask &= ~act.getMask();
			return true;
		}
		
		return false;
	}
	
	/**
	 * @param act
	 * @return {@code true} if action is present, {@code false} otherwise.
	 */
	public boolean hasAction(PlayerAction act)
	{
		return (_actionMask & act.getMask()) == act.getMask();
	}
	
	/**
	 * Set true/false if character got Charm of Courage
	 * @param value true/false
	 */
	public void setCharmOfCourage(boolean value)
	{
		_hasCharmOfCourage = value;
	}
	
	/**
	 * @return {@code true} if effect is present, {@code false} otherwise.
	 */
	public boolean hasCharmOfCourage()
	{
		return _hasCharmOfCourage;
	}
	
	public boolean isGood()
	{
		return _isGood;
	}
	
	public boolean isEvil()
	{
		return _isEvil;
	}
	
	public void setGood()
	{
		_isGood = true;
		_isEvil = false;
	}
	
	public void setEvil()
	{
		_isGood = false;
		_isEvil = true;
	}
	
	/**
	 * @param target the target
	 * @return {@code true} if this player got war with the target, {@code false} otherwise.
	 */
	public boolean isAtWarWith(Creature target)
	{
		if (target == null)
		{
			return false;
		}
		
		if ((_clan != null) && !isAcademyMember() && (target.getClan() != null) && !target.isAcademyMember())
		{
			return _clan.isAtWarWith(target.getClan());
		}
		
		return false;
	}
	
	/**
	 * @param target the target
	 * @return {@code true} if this player in same party with the target, {@code false} otherwise.
	 */
	public boolean isInPartyWith(Creature target)
	{
		return isInParty() && target.isInParty() && (getParty().getLeaderObjectId() == target.getParty().getLeaderObjectId());
	}
	
	/**
	 * @param target the target
	 * @return {@code true} if this player in same command channel with the target, {@code false} otherwise.
	 */
	public boolean isInCommandChannelWith(Creature target)
	{
		return isInParty() && target.isInParty() && getParty().isInCommandChannel() && target.getParty().isInCommandChannel() && (getParty().getCommandChannel().getLeaderObjectId() == target.getParty().getCommandChannel().getLeaderObjectId());
	}
	
	/**
	 * @param target the target
	 * @return {@code true} if this player in same clan with the target, {@code false} otherwise.
	 */
	public boolean isInClanWith(Creature target)
	{
		return (getClanId() != 0) && (target.getClanId() != 0) && (getClanId() == target.getClanId());
	}
	
	/**
	 * @param target the target
	 * @return {@code true} if this player in same ally with the target, {@code false} otherwise.
	 */
	public boolean isInAllyWith(Creature target)
	{
		return (getAllyId() != 0) && (target.getAllyId() != 0) && (getAllyId() == target.getAllyId());
	}
	
	/**
	 * @param target the target
	 * @return {@code true} if this player at duel with the target, {@code false} otherwise.
	 */
	public boolean isInDuelWith(Creature target)
	{
		return isInDuel() && target.isInDuel() && (getDuelId() == target.getDuelId());
	}
	
	/**
	 * @param target the target
	 * @return {@code true} if this player is on same siege side with the target, {@code false} otherwise.
	 */
	public boolean isOnSameSiegeSideWith(Creature target)
	{
		return (getSiegeState() > 0) && isInsideZone(ZoneId.SIEGE) && (getSiegeState() == target.getSiegeState()) && (getSiegeSide() == target.getSiegeSide());
	}
	
	/**
	 * @return the game shop points of the player.
	 */
	public long getGamePoints()
	{
		return getAccountVariables().getInt(GAME_POINTS_VAR, 0);
	}
	
	/**
	 * Sets game shop points for current player.
	 * @param points
	 */
	public void setGamePoints(long points)
	{
		// Immediate store upon change
		final AccountVariables vars = getAccountVariables();
		vars.set(GAME_POINTS_VAR, Math.max(points, 0));
		vars.storeMe();
	}
	
	private TerminateReturn onExperienceReceived()
	{
		if (isDead())
		{
			return new TerminateReturn(false, false, false);
		}
		
		return new TerminateReturn(true, true, true);
	}
	
	public void disableExpGain()
	{
		addListener(new FunctionEventListener(this, EventType.ON_PLAYABLE_EXP_CHANGED, (OnPlayableExpChanged _) -> onExperienceReceived(), this));
	}
	
	public void enableExpGain()
	{
		removeListenerIf(EventType.ON_PLAYABLE_EXP_CHANGED, listener -> listener.getOwner() == this);
	}
	
	public void sendInventoryUpdate(InventoryUpdate iu)
	{
		if (_inventoryUpdateTask != null)
		{
			_inventoryUpdateTask.cancel(false);
		}
		
		_inventoryUpdate.putAll(iu.getItemEntries());
		
		_inventoryUpdateTask = ThreadPool.schedule(() ->
		{
			sendPacket(_inventoryUpdate);
		}, 100);
	}
	
	public void sendItemList(boolean open)
	{
		if (_itemListTask != null)
		{
			_itemListTask.cancel(false);
		}
		
		_itemListTask = ThreadPool.schedule(() ->
		{
			sendPacket(new ItemList(this, open));
		}, 250);
	}
	
	/**
	 * Precautionary method to end all tasks upon disconnection.
	 * @TODO: Rework stopAllTimers() method.
	 */
	public void stopAllTasks()
	{
		if ((_mountFeedTask != null) && !_mountFeedTask.isDone() && !_mountFeedTask.isCancelled())
		{
			_mountFeedTask.cancel(false);
			_mountFeedTask = null;
		}
		
		if ((_dismountTask != null) && !_dismountTask.isDone() && !_dismountTask.isCancelled())
		{
			_dismountTask.cancel(false);
			_dismountTask = null;
		}
		
		if ((_fameTask != null) && !_fameTask.isDone() && !_fameTask.isCancelled())
		{
			_fameTask.cancel(false);
			_fameTask = null;
		}
		
		if ((_vitalityTask != null) && !_vitalityTask.isDone() && !_vitalityTask.isCancelled())
		{
			_vitalityTask.cancel(false);
			_vitalityTask = null;
		}
		
		if ((_teleportWatchdog != null) && !_teleportWatchdog.isDone() && !_teleportWatchdog.isCancelled())
		{
			_teleportWatchdog.cancel(false);
			_teleportWatchdog = null;
		}
		
		if ((_taskForFish != null) && !_taskForFish.isDone() && !_taskForFish.isCancelled())
		{
			_taskForFish.cancel(false);
			_taskForFish = null;
		}
		
		if ((_chargeTask != null) && !_chargeTask.isDone() && !_chargeTask.isCancelled())
		{
			_chargeTask.cancel(false);
			_chargeTask = null;
		}
		
		if ((_taskRentPet != null) && !_taskRentPet.isDone() && !_taskRentPet.isCancelled())
		{
			_taskRentPet.cancel(false);
			_taskRentPet = null;
		}
		
		if ((_taskWater != null) && !_taskWater.isDone() && !_taskWater.isCancelled())
		{
			_taskWater.cancel(false);
			_taskWater = null;
		}
		
		if ((_fallingDamageTask != null) && !_fallingDamageTask.isDone() && !_fallingDamageTask.isCancelled())
		{
			_fallingDamageTask.cancel(false);
			_fallingDamageTask = null;
		}
		
		if ((_taskWarnUserTakeBreak != null) && !_taskWarnUserTakeBreak.isDone() && !_taskWarnUserTakeBreak.isCancelled())
		{
			_taskWarnUserTakeBreak.cancel(false);
			_taskWarnUserTakeBreak = null;
		}
		
		synchronized (_questTimers)
		{
			for (QuestTimer timer : _questTimers)
			{
				timer.cancelTask();
			}
			
			_questTimers.clear();
		}
		
		synchronized (_timerHolders)
		{
			for (TimerHolder<?> timer : _timerHolders)
			{
				timer.cancelTask();
			}
			
			_timerHolders.clear();
		}
	}
	
	public void addQuestTimer(QuestTimer questTimer)
	{
		synchronized (_questTimers)
		{
			_questTimers.add(questTimer);
		}
	}
	
	public void removeQuestTimer(QuestTimer questTimer)
	{
		synchronized (_questTimers)
		{
			_questTimers.remove(questTimer);
		}
	}
	
	public void addTimerHolder(TimerHolder<?> timer)
	{
		synchronized (_timerHolders)
		{
			_timerHolders.add(timer);
		}
	}
	
	public void removeTimerHolder(TimerHolder<?> timer)
	{
		synchronized (_timerHolders)
		{
			_timerHolders.remove(timer);
		}
	}
	
	public void setServitorShare(Map<Stat, Double> map)
	{
		_servitorShare = map;
	}
	
	public double getServitorShareBonus(Stat stat)
	{
		final Map<Stat, Double> stats = _servitorShare;
		if (stats == null)
		{
			return 1.0d;
		}
		
		final Double val = stats.get(stat);
		if (val == null)
		{
			return 1.0d;
		}
		
		return val;
	}
	
	public AutoPlaySettingsHolder getAutoPlaySettings()
	{
		return _autoPlaySettings;
	}
	
	public AutoUseSettingsHolder getAutoUseSettings()
	{
		return _autoUseSettings;
	}
	
	public void setAutoPlaying(boolean value)
	{
		_autoPlaying.set(value);
		
		if (!value && _offlinePlay && OfflinePlayConfig.RESTORE_AUTO_PLAY_OFFLINERS)
		{
			OfflinePlayTable.getInstance().removeOfflinePlay(this);
		}
	}
	
	public boolean isAutoPlaying()
	{
		return _autoPlaying.get();
	}
}
