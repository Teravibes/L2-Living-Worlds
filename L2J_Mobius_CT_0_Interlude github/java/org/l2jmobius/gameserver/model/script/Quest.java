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
package org.l2jmobius.gameserver.model.script;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.commons.util.TraceUtil;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.config.custom.ChampionMonstersConfig;
import org.l2jmobius.gameserver.config.custom.PremiumSystemConfig;
import org.l2jmobius.gameserver.config.custom.RandomSpawnsConfig;
import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.managers.PcCafePointsManager;
import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.actor.enums.creature.TrapAction;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.actor.holders.npc.MinionList;
import org.l2jmobius.gameserver.model.actor.instance.Door;
import org.l2jmobius.gameserver.model.actor.instance.Monster;
import org.l2jmobius.gameserver.model.actor.instance.Trap;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.events.Containers;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.ListenersContainer;
import org.l2jmobius.gameserver.model.events.annotations.Id;
import org.l2jmobius.gameserver.model.events.annotations.Ids;
import org.l2jmobius.gameserver.model.events.annotations.NpcLevelRange;
import org.l2jmobius.gameserver.model.events.annotations.NpcLevelRanges;
import org.l2jmobius.gameserver.model.events.annotations.Priority;
import org.l2jmobius.gameserver.model.events.annotations.Range;
import org.l2jmobius.gameserver.model.events.annotations.Ranges;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.annotations.RegisterType;
import org.l2jmobius.gameserver.model.events.holders.IBaseEvent;
import org.l2jmobius.gameserver.model.events.holders.actor.creature.OnCreatureDeath;
import org.l2jmobius.gameserver.model.events.holders.actor.creature.OnCreatureSee;
import org.l2jmobius.gameserver.model.events.holders.actor.creature.OnCreatureZoneEnter;
import org.l2jmobius.gameserver.model.events.holders.actor.creature.OnCreatureZoneExit;
import org.l2jmobius.gameserver.model.events.holders.actor.npc.OnNpcCanBeSeen;
import org.l2jmobius.gameserver.model.events.holders.actor.npc.OnNpcEventReceived;
import org.l2jmobius.gameserver.model.events.holders.actor.npc.OnNpcFirstTalk;
import org.l2jmobius.gameserver.model.events.holders.actor.npc.OnNpcMoveFinished;
import org.l2jmobius.gameserver.model.events.holders.actor.npc.OnNpcMoveNodeArrived;
import org.l2jmobius.gameserver.model.events.holders.actor.npc.OnNpcMoveRouteFinished;
import org.l2jmobius.gameserver.model.events.holders.actor.npc.OnNpcSkillFinished;
import org.l2jmobius.gameserver.model.events.holders.actor.npc.OnNpcSkillSee;
import org.l2jmobius.gameserver.model.events.holders.actor.npc.OnNpcSpawn;
import org.l2jmobius.gameserver.model.events.holders.actor.npc.OnNpcTeleport;
import org.l2jmobius.gameserver.model.events.holders.actor.npc.attackable.OnAttackableAggroRangeEnter;
import org.l2jmobius.gameserver.model.events.holders.actor.npc.attackable.OnAttackableAttack;
import org.l2jmobius.gameserver.model.events.holders.actor.npc.attackable.OnAttackableFactionCall;
import org.l2jmobius.gameserver.model.events.holders.actor.npc.attackable.OnAttackableHate;
import org.l2jmobius.gameserver.model.events.holders.actor.npc.attackable.OnAttackableKill;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogout;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerProfessionCancel;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerProfessionChange;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerSkillLearn;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerSummonSpawn;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerSummonTalk;
import org.l2jmobius.gameserver.model.events.holders.actor.trap.OnTrapAction;
import org.l2jmobius.gameserver.model.events.holders.item.OnItemBypassEvent;
import org.l2jmobius.gameserver.model.events.holders.item.OnItemTalk;
import org.l2jmobius.gameserver.model.events.holders.olympiad.OnOlympiadMatchResult;
import org.l2jmobius.gameserver.model.events.holders.sieges.castle.OnCastleSiegeFinish;
import org.l2jmobius.gameserver.model.events.holders.sieges.castle.OnCastleSiegeOwnerChange;
import org.l2jmobius.gameserver.model.events.holders.sieges.castle.OnCastleSiegeStart;
import org.l2jmobius.gameserver.model.events.listeners.AbstractEventListener;
import org.l2jmobius.gameserver.model.events.listeners.AnnotationEventListener;
import org.l2jmobius.gameserver.model.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.model.events.listeners.DummyEventListener;
import org.l2jmobius.gameserver.model.events.listeners.FunctionEventListener;
import org.l2jmobius.gameserver.model.events.listeners.RunnableEventListener;
import org.l2jmobius.gameserver.model.events.returns.AbstractEventReturn;
import org.l2jmobius.gameserver.model.events.returns.TerminateReturn;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.interfaces.IPositionable;
import org.l2jmobius.gameserver.model.item.EtcItem;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.itemcontainer.PetInventory;
import org.l2jmobius.gameserver.model.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.model.itemcontainer.PlayerWarehouse;
import org.l2jmobius.gameserver.model.olympiad.Olympiad;
import org.l2jmobius.gameserver.model.script.timers.IEventTimerCancel;
import org.l2jmobius.gameserver.model.script.timers.IEventTimerEvent;
import org.l2jmobius.gameserver.model.script.timers.TimerExecutor;
import org.l2jmobius.gameserver.model.script.timers.TimerHolder;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.enums.AcquireSkillType;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.model.spawns.Spawn;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SpecialCamera;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.scripting.ScriptEngine;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * Quest main class.
 * @author Luis Arias, Mobius
 */
public class Quest implements IEventTimerEvent<String>, IEventTimerCancel<String>
{
	public static final Logger LOGGER = Logger.getLogger(Quest.class.getName());
	
	private static final String DEFAULT_NO_QUEST_MSG = "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>";
	private static final String DEFAULT_ALREADY_COMPLETED_MSG = "<html><body>This quest has already been completed.</body></html>";
	private static final String QUEST_DELETE_FROM_CHAR_QUERY = "DELETE FROM character_quests WHERE charId=? AND name=?";
	private static final String QUEST_DELETE_FROM_CHAR_QUERY_NON_REPEATABLE_QUERY = "DELETE FROM character_quests WHERE charId=? AND name=? AND var!=?";
	
	// Dimensional Diamond Rewards by Class for 2nd class transfer quest (35)
	protected static final Map<Integer, Integer> DF_REWARD_35 = new HashMap<>();
	static
	{
		DF_REWARD_35.put(1, 61);
		DF_REWARD_35.put(4, 45);
		DF_REWARD_35.put(7, 128);
		DF_REWARD_35.put(11, 168);
		DF_REWARD_35.put(15, 49);
		DF_REWARD_35.put(19, 61);
		DF_REWARD_35.put(22, 128);
		DF_REWARD_35.put(26, 168);
		DF_REWARD_35.put(29, 49);
		DF_REWARD_35.put(32, 61);
		DF_REWARD_35.put(35, 128);
		DF_REWARD_35.put(39, 168);
		DF_REWARD_35.put(42, 49);
		DF_REWARD_35.put(45, 61);
		DF_REWARD_35.put(47, 61);
		DF_REWARD_35.put(50, 49);
		DF_REWARD_35.put(54, 85);
		DF_REWARD_35.put(56, 85);
	}
	
	// Dimensional Diamond Rewards by Race for 2nd class transfer quest (37)
	protected static final Map<Integer, Integer> DF_REWARD_37 = new HashMap<>();
	static
	{
		DF_REWARD_37.put(0, 96);
		DF_REWARD_37.put(1, 102);
		DF_REWARD_37.put(2, 98);
		DF_REWARD_37.put(3, 109);
		DF_REWARD_37.put(4, 50);
	}
	
	// Dimensional Diamond Rewards by Class for 2nd class transfer quest (39)
	protected static final Map<Integer, Integer> DF_REWARD_39 = new HashMap<>();
	static
	{
		DF_REWARD_39.put(1, 72);
		DF_REWARD_39.put(4, 104);
		DF_REWARD_39.put(7, 96);
		DF_REWARD_39.put(11, 122);
		DF_REWARD_39.put(15, 60);
		DF_REWARD_39.put(19, 72);
		DF_REWARD_39.put(22, 96);
		DF_REWARD_39.put(26, 122);
		DF_REWARD_39.put(29, 45);
		DF_REWARD_39.put(32, 104);
		DF_REWARD_39.put(35, 96);
		DF_REWARD_39.put(39, 122);
		DF_REWARD_39.put(42, 60);
		DF_REWARD_39.put(45, 64);
		DF_REWARD_39.put(47, 72);
		DF_REWARD_39.put(50, 92);
		DF_REWARD_39.put(54, 82);
		DF_REWARD_39.put(56, 23);
	}
	
	private final Map<ListenerRegisterType, Set<Integer>> _registeredIds = new ConcurrentHashMap<>();
	private final Queue<AbstractEventListener> _listeners = new PriorityBlockingQueue<>();
	private final Map<Predicate<Player>, String> _startCondition = new LinkedHashMap<>(1);
	private final Map<String, List<QuestTimer>> _questTimers = new HashMap<>();
	private TimerExecutor<String> _timerExecutor;
	
	private final int _questId;
	private final String _description;
	private final Path _scriptFile;
	private boolean _isCustom = false;
	public int[] _questItemIds = null;
	
	/**
	 * The Quest object constructor.<br>
	 * Constructing a quest also calls the {@code init_LoadGlobalData} convenience method.
	 * @param questId ID of the quest
	 * @param description client description of the quest
	 */
	public Quest(int questId, String description)
	{
		_scriptFile = Path.of(ScriptEngine.getInstance().getCurrentLoadingScript().toUri());
		initializeAnnotationListeners();
		
		_questId = questId;
		_description = description;
		if (questId > 0)
		{
			ScriptManager.getInstance().addQuest(this);
		}
		else
		{
			ScriptManager.getInstance().addScript(this);
		}
		
		onLoad();
	}
	
	@Override
	public void onTimerEvent(TimerHolder<String> holder)
	{
		onTimerEvent(holder.getEvent(), holder.getParams(), holder.getNpc(), holder.getPlayer());
	}
	
	@Override
	public void onTimerCancel(TimerHolder<String> holder)
	{
		onTimerCancel(holder.getEvent(), holder.getParams(), holder.getNpc(), holder.getPlayer());
	}
	
	public void onTimerEvent(String event, StatSet params, Npc npc, Player player)
	{
		LOGGER.warning("[" + getClass().getSimpleName() + "]: Timer event arrived at non overriden onTimerEvent method event: " + event + " npc: " + npc + " player: " + player);
	}
	
	public void onTimerCancel(String event, StatSet params, Npc npc, Player player)
	{
	}
	
	/**
	 * @return the {@link TimerExecutor} object that manages timers
	 */
	public TimerExecutor<String> getTimers()
	{
		if (_timerExecutor == null)
		{
			synchronized (this)
			{
				if (_timerExecutor == null)
				{
					_timerExecutor = new TimerExecutor<>(this, this);
				}
			}
		}
		
		return _timerExecutor;
	}
	
	public boolean hasTimers()
	{
		return _timerExecutor != null;
	}
	
	/**
	 * Gets the quest ID.
	 * @return the quest ID
	 */
	public int getId()
	{
		return _questId;
	}
	
	/**
	 * @return the name of the quest
	 */
	public String getName()
	{
		return getClass().getSimpleName();
	}
	
	/**
	 * @return the client description of the quest
	 */
	public String getDescription()
	{
		return _description;
	}
	
	/**
	 * @return Returns the scriptFile.
	 */
	public Path getScriptFile()
	{
		return _scriptFile;
	}
	
	/**
	 * @return the path of the quest script
	 */
	public String getPath()
	{
		final String path = getClass().getName().replace('.', '/');
		return path.substring(0, path.lastIndexOf('/' + getClass().getSimpleName()));
	}
	
	/**
	 * Verifies if this is a custom quest.
	 * @return {@code true} if the quest script is a custom quest, {@code false} otherwise.
	 */
	public boolean isCustomQuest()
	{
		return _isCustom;
	}
	
	/**
	 * If a quest is set as custom, it will display it's name in the NPC Quest List.<br>
	 * Retail quests are unhardcoded to display the name using a client string.
	 * @param value if {@code true} the quest script will be set as custom quest.
	 */
	public void setCustom(boolean value)
	{
		_isCustom = value;
	}
	
	/**
	 * @return the NpcStringId of the current quest, used in Quest link bypass
	 */
	public int getNpcStringId()
	{
		return _questId > 10000 ? _questId - 5000 : _questId;
	}
	
	public void reload()
	{
		unload();
		
		try
		{
			ScriptEngine.getInstance().executeScript(_scriptFile);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Failed to reload script!", e);
		}
	}
	
	public void unload()
	{
		unload(true);
	}
	
	public void unload(boolean removeFromList)
	{
		onSave();
		
		// Cancel all pending timers before reloading.
		// If timers ought to be restarted, the quest can take care of it with its code (example: save global data indicating what timer must be restarted).
		for (List<QuestTimer> timers : _questTimers.values())
		{
			for (QuestTimer timer : timers)
			{
				timer.cancel();
			}
			
			timers.clear();
		}
		
		_questTimers.clear();
		
		if (removeFromList)
		{
			ScriptManager.getInstance().removeScript(this);
		}
		
		_listeners.forEach(AbstractEventListener::unregisterMe);
		_listeners.clear();
		if (_timerExecutor != null)
		{
			_timerExecutor.cancelAllTimers();
		}
	}
	
	/**
	 * This method is, by default, called by the constructor of all scripts.<br>
	 * Children of this class can implement this function in order to define what variables to load and what structures to save them in.<br>
	 * By default, nothing is loaded.
	 */
	protected void onLoad()
	{
	}
	
	/**
	 * The function onSave is, by default, called at shutdown, for all quests, by the ScriptManager.<br>
	 * Children of this class can implement this function in order to convert their structures<br>
	 * into <var, value> tuples and make calls to save them to the database, if needed.<br>
	 * By default, nothing is saved.
	 */
	public void onSave()
	{
	}
	
	/**
	 * Add a new quest state of this quest to the database.
	 * @param player the owner of the newly created quest state
	 * @return the newly created {@link QuestState} object
	 */
	public QuestState newQuestState(Player player)
	{
		return new QuestState(this, player, State.CREATED);
	}
	
	/**
	 * Get the specified player's {@link QuestState} object for this quest.<br>
	 * If the player does not have it and initIfNode is {@code true},<br>
	 * create a new QuestState object and return it, otherwise return {@code null}.
	 * @param player the player whose QuestState to get
	 * @param initIfNone if true and the player does not have a QuestState for this quest,<br>
	 *            create a new QuestState
	 * @return the QuestState object for this quest or null if it doesn't exist
	 */
	public QuestState getQuestState(Player player, boolean initIfNone)
	{
		final QuestState qs = player.getQuestState(getName());
		if ((qs != null) || !initIfNone)
		{
			return qs;
		}
		
		return newQuestState(player);
	}
	
	/**
	 * Add a timer to the quest (if it doesn't exist already) and start it.
	 * @param name the name of the timer (also passed back as "event" in {@link #onEvent(String, Npc, Player)})
	 * @param time time in ms for when to fire the timer
	 * @param npc the NPC associated with this timer (can be null)
	 * @param player the player associated with this timer (can be null)
	 * @see #startQuestTimer(String, long, Npc, Player, boolean)
	 */
	public void startQuestTimer(String name, long time, Npc npc, Player player)
	{
		startQuestTimer(name, time, npc, player, false);
	}
	
	/**
	 * Gets the quest timers.
	 * @return the quest timers
	 */
	public Map<String, List<QuestTimer>> getQuestTimers()
	{
		return _questTimers;
	}
	
	/**
	 * Add a timer to the quest (if it doesn't exist already) and start it.
	 * @param name the name of the timer (also passed back as "event" in {@link #onEvent(String, Npc, Player)})
	 * @param time time in ms for when to fire the timer
	 * @param npc the NPC associated with this timer (can be null)
	 * @param player the player associated with this timer (can be null)
	 * @param repeating indicates whether the timer is repeatable or one-time.<br>
	 *            If {@code true}, the task is repeated every {@code time} milliseconds until explicitly stopped.
	 */
	public void startQuestTimer(String name, long time, Npc npc, Player player, boolean repeating)
	{
		if (name == null)
		{
			return;
		}
		
		synchronized (_questTimers)
		{
			if (!_questTimers.containsKey(name))
			{
				_questTimers.put(name, new CopyOnWriteArrayList<>());
			}
			
			// If there exists a timer with this name, allow the timer only if the [npc, player] set is unique nulls act as wildcards.
			if (getQuestTimer(name, npc, player) == null)
			{
				_questTimers.get(name).add(new QuestTimer(this, name, time, npc, player, repeating));
			}
		}
	}
	
	/**
	 * Get a quest timer that matches the provided name and parameters.
	 * @param name the name of the quest timer to get
	 * @param npc the NPC associated with the quest timer to get
	 * @param player the player associated with the quest timer to get
	 * @return the quest timer that matches the specified parameters or {@code null} if nothing was found
	 */
	public QuestTimer getQuestTimer(String name, Npc npc, Player player)
	{
		if (name == null)
		{
			return null;
		}
		
		final List<QuestTimer> timers = _questTimers.get(name);
		if ((timers == null) || timers.isEmpty())
		{
			return null;
		}
		
		for (QuestTimer timer : timers)
		{
			if ((timer != null) && timer.equals(this, name, npc, player))
			{
				return timer;
			}
		}
		
		return null;
	}
	
	/**
	 * Cancel all quest timers with the specified name.
	 * @param name the name of the quest timers to cancel
	 */
	public void cancelQuestTimers(String name)
	{
		if (name == null)
		{
			return;
		}
		
		final List<QuestTimer> timers = _questTimers.get(name);
		if ((timers == null) || timers.isEmpty())
		{
			return;
		}
		
		for (QuestTimer timer : timers)
		{
			if (timer != null)
			{
				timer.cancel();
			}
		}
		
		timers.clear();
	}
	
	/**
	 * Cancel the quest timer that matches the specified name and parameters.
	 * @param name the name of the quest timer to cancel
	 * @param npc the NPC associated with the quest timer to cancel
	 * @param player the player associated with the quest timer to cancel
	 */
	public void cancelQuestTimer(String name, Npc npc, Player player)
	{
		if (name == null)
		{
			return;
		}
		
		final List<QuestTimer> timers = _questTimers.get(name);
		if ((timers == null) || timers.isEmpty())
		{
			return;
		}
		
		for (QuestTimer timer : timers)
		{
			if ((timer != null) && timer.equals(this, name, npc, player))
			{
				timer.cancel();
			}
		}
	}
	
	/**
	 * Remove a quest timer from the list of all timers.<br>
	 * Note: does not stop the timer itself!
	 * @param timer the {@link QuestState} object to remove
	 */
	public void removeQuestTimer(QuestTimer timer)
	{
		if (timer == null)
		{
			return;
		}
		
		final List<QuestTimer> timers = _questTimers.get(timer.toString());
		if (timers != null)
		{
			timers.remove(timer);
		}
	}
	
	// These are methods to call within the core to call the quest events.
	
	/**
	 * @param npc the teleport NPC
	 */
	public void notifyTeleport(Npc npc)
	{
		try
		{
			onTeleport(npc);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception on onTeleport() in notifyTeleport(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * @param event
	 * @param npc
	 * @param player
	 */
	public void notifyEvent(String event, Npc npc, Player player)
	{
		String res = null;
		try
		{
			res = onEvent(event, npc, player);
		}
		catch (Exception e)
		{
			showError(player, e);
			return;
		}
		
		showResult(player, res, npc);
	}
	
	/**
	 * @param npc
	 * @param player
	 */
	public void notifyTalk(Npc npc, Player player)
	{
		String res = null;
		try
		{
			final String startConditionHtml = getStartConditionHtml(player);
			if (!player.hasQuestState(getName()) && (startConditionHtml != null))
			{
				res = startConditionHtml;
			}
			else
			{
				res = onTalk(npc, player);
			}
		}
		catch (Exception e)
		{
			showError(player, e);
			return;
		}
		
		player.setLastQuestNpcObject(npc.getObjectId());
		showResult(player, res, npc);
	}
	
	/**
	 * Override the default NPC dialogs when a quest defines this for the given NPC.<br>
	 * Note: If the default html for this npc needs to be shown, onFirstTalk should call npc.showChatWindow(player) and then return null.
	 * @param npc the NPC whose dialogs to override
	 * @param player the player talking to the NPC
	 */
	public void notifyFirstTalk(Npc npc, Player player)
	{
		String res = null;
		try
		{
			res = onFirstTalk(npc, player);
		}
		catch (Exception e)
		{
			showError(player, e);
			return;
		}
		
		showResult(player, res, npc);
	}
	
	/**
	 * @param item
	 * @param player
	 */
	public void notifyItemTalk(Item item, Player player)
	{
		String res = null;
		try
		{
			res = onItemTalk(item, player);
		}
		catch (Exception e)
		{
			showError(player, e);
			return;
		}
		
		showResult(player, res);
	}
	
	/**
	 * @param item
	 * @param player
	 * @return
	 */
	public String onItemTalk(Item item, Player player)
	{
		return null;
	}
	
	/**
	 * @param eventName - name of event
	 * @param sender - NPC, who sent event
	 * @param receiver - NPC, who received event
	 * @param reference - WorldObject to pass, if needed
	 */
	public void notifyEventReceived(String eventName, Npc sender, Npc receiver, WorldObject reference)
	{
		try
		{
			onEventReceived(eventName, sender, receiver, reference);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception on onEventReceived() in notifyEventReceived(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * @param npc
	 */
	public void notifyMoveFinished(Npc npc)
	{
		try
		{
			onMoveFinished(npc);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception on onMoveFinished() in notifyMoveFinished(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * @param npc
	 */
	public void notifyNodeArrived(Npc npc)
	{
		try
		{
			onNodeArrived(npc);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception on onNodeArrived() in notifyNodeArrived(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * @param npc
	 */
	public void notifyRouteFinished(Npc npc)
	{
		try
		{
			onRouteFinished(npc);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception on onRouteFinished() in notifyRouteFinished(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * @param npc
	 * @param player
	 * @return {@code true} if player can see this npc, {@code false} otherwise.
	 */
	public boolean notifyOnCanSeeMe(Npc npc, Player player)
	{
		try
		{
			return onCanSeeMe(npc, player);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception on onCanSeeMe() in notifyOnCanSeeMe(): " + e.getMessage(), e);
		}
		
		return false;
	}
	
	// These are methods that java calls to invoke scripts.
	
	/**
	 * This function is called in place of {@link #onAttack(Npc, Player, int, boolean, Skill)} if the former is not implemented.<br>
	 * If a script contains both onAttack(..) implementations, then this method will never be called unless the script's {@link #onAttack(Npc, Player, int, boolean, Skill)} explicitly calls this method.
	 * @param npc this parameter contains a reference to the exact instance of the NPC that got attacked the NPC.
	 * @param attacker this parameter contains a reference to the exact instance of the player who attacked.
	 * @param damage this parameter represents the total damage that this attack has inflicted to the NPC.
	 * @param isSummon this parameter if it's {@code false} it denotes that the attacker was indeed the player, else it specifies that the damage was actually dealt by the player's pet.
	 */
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
	}
	
	/**
	 * This function is called whenever a player attacks an NPC that is registered for the quest.<br>
	 * If is not overridden by a subclass, then default to the returned value of the simpler (and older) {@link #onAttack(Npc, Player, int, boolean)} override.
	 * @param npc this parameter contains a reference to the exact instance of the NPC that got attacked.
	 * @param attacker this parameter contains a reference to the exact instance of the player who attacked the NPC.
	 * @param damage this parameter represents the total damage that this attack has inflicted to the NPC.
	 * @param isSummon this parameter if it's {@code false} it denotes that the attacker was indeed the player, else it specifies that the damage was actually dealt by the player's summon
	 * @param skill parameter is the skill that player used to attack NPC.
	 */
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		onAttack(npc, attacker, damage, isSummon);
	}
	
	/**
	 * This function is called whenever an <b>exact instance</b> of a character who was previously registered for this event dies.<br>
	 * The registration for {@link #onDeath(Creature, Creature, QuestState)} events <b>is not</b> done via the quest itself, but it is instead handled by the QuestState of a particular player.
	 * @param killer this parameter contains a reference to the exact instance of the NPC that <b>killed</b> the character.
	 * @param victim this parameter contains a reference to the exact instance of the character that got killed.
	 * @param qs this parameter contains a reference to the QuestState of whomever was interested (waiting) for this kill.
	 */
	public void onDeath(Creature killer, Creature victim, QuestState qs)
	{
		onEvent("", (killer instanceof Npc) ? killer.asNpc() : null, qs.getPlayer());
	}
	
	/**
	 * This function is called whenever a player clicks on a link in a quest dialog and whenever a timer fires.<br>
	 * If the player has a quest state, use it as parameter in the next call, otherwise return null.
	 * @param event this parameter contains a string identifier for the event.<br>
	 *            Generally, this string is passed directly via the link.<br>
	 *            For example:<br>
	 *            <code>
	 *            &lt;a action="bypass -h Script 626_ADarkTwilight 31517-01.htm"&gt;hello&lt;/a&gt;
	 *            </code><br>
	 *            The above link sets the event variable to "31517-01.htm" for the quest 626_ADarkTwilight.<br>
	 *            In the case of timers, this will be the name of the timer.<br>
	 *            This parameter serves as a sort of identifier.
	 * @param npc this parameter contains a reference to the instance of NPC associated with this event.<br>
	 *            This may be the NPC registered in a timer, or the NPC with whom a player is speaking, etc.<br>
	 *            This parameter may be {@code null} in certain circumstances.
	 * @param player this parameter contains a reference to the player participating in this function.<br>
	 *            It may be the player speaking to the NPC, or the player who caused a timer to start (and owns that timer).<br>
	 *            This parameter may be {@code null} in certain circumstances.
	 * @return the text returned by the event (may be {@code null}, a filename or just text)
	 */
	public String onEvent(String event, Npc npc, Player player)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a player kills a NPC that is registered for the quest.
	 * @param npc this parameter contains a reference to the exact instance of the NPC that got killed.
	 * @param killer this parameter contains a reference to the exact instance of the player who killed the NPC.
	 * @param isSummon this parameter if it's {@code false} it denotes that the attacker was indeed the player, else it specifies that the killer was the player's pet.
	 */
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
	}
	
	/**
	 * This function is called whenever a player clicks to the "Script" link of an NPC that is registered for the quest.
	 * @param npc this parameter contains a reference to the exact instance of the NPC that the player is talking with.
	 * @param talker this parameter contains a reference to the exact instance of the player who is talking to the NPC.
	 * @return the text returned by the event (may be {@code null}, a filename or just text)
	 */
	public String onTalk(Npc npc, Player talker)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a player talks to an NPC that is registered for the quest.<br>
	 * That is, it is triggered from the very first click on the NPC, not via another dialog.<br>
	 * <b>Note 1:</b><br>
	 * Each NPC can be registered to at most one quest for triggering this function.<br>
	 * In other words, the same one NPC cannot respond to an "onFirstTalk" request from two different quests.<br>
	 * Attempting to register an NPC in two different quests for this function will result in one of the two registration being ignored.<br>
	 * <b>Note 2:</b><br>
	 * Since a Quest link isn't clicked in order to reach this, a quest state can be invalid within this function.<br>
	 * The coder of the script may need to create a new quest state (if necessary).<br>
	 * <b>Note 3:</b><br>
	 * The returned value of onFirstTalk replaces the default HTML that would have otherwise been loaded from a sub-folder of DatapackRoot/game/data/html/.<br>
	 * If you wish to show the default HTML, within onFirstTalk do npc.showChatWindow(player) and then return ""
	 * @param npc this parameter contains a reference to the exact instance of the NPC that the player is talking with.
	 * @param player this parameter contains a reference to the exact instance of the player who is talking to the NPC.
	 * @return the text returned by the event (may be {@code null}, a filename or just text)
	 */
	public String onFirstTalk(Npc npc, Player player)
	{
		return null;
	}
	
	/**
	 * @param item
	 * @param player
	 * @param event
	 */
	public void onItemEvent(Item item, Player player, String event)
	{
	}
	
	/**
	 * This function is called whenever a player request a skill list.<br>
	 * @param npc this parameter contains a reference to the exact instance of the NPC that the player requested the skill list.
	 * @param player this parameter contains a reference to the exact instance of the player who requested the skill list.
	 */
	public void onAcquireSkillList(Npc npc, Player player)
	{
	}
	
	/**
	 * This function is called whenever a player request a skill info.
	 * @param npc this parameter contains a reference to the exact instance of the NPC that the player requested the skill info.
	 * @param player this parameter contains a reference to the exact instance of the player who requested the skill info.
	 * @param skill this parameter contains a reference to the skill that the player requested its info.
	 */
	public void onAcquireSkillInfo(Npc npc, Player player, Skill skill)
	{
	}
	
	/**
	 * This function is called whenever a player acquire a skill.<br>
	 * @param npc this parameter contains a reference to the exact instance of the NPC that the player requested the skill.
	 * @param player this parameter contains a reference to the exact instance of the player who requested the skill.
	 * @param skill this parameter contains a reference to the skill that the player requested.
	 * @param type the skill learn type
	 */
	public void onAcquireSkill(Npc npc, Player player, Skill skill, AcquireSkillType type)
	{
	}
	
	/**
	 * This function is called whenever a player casts a skill near a registered NPC (1000 distance).<br>
	 * <b>Note:</b><br>
	 * If a skill does damage, both onSkillSee(..) and onAttack(..) will be triggered for the damaged NPC!<br>
	 * However, only onSkillSee(..) will be triggered if the skill does no damage,<br>
	 * or if it damages an NPC who has no onAttack(..) registration while near another NPC who has an onSkillSee registration.<br>
	 * @param npc the NPC that saw the skill
	 * @param caster the player who cast the skill
	 * @param skill the actual skill that was used
	 * @param targets a collection of all objects (can be any type of object, including mobs and players) that were affected by the skill
	 * @param isSummon if {@code true}, the skill was actually cast by the player's summon, not the player himself
	 */
	public void onSkillSee(Npc npc, Player caster, Skill skill, List<WorldObject> targets, boolean isSummon)
	{
	}
	
	/**
	 * This function is called whenever an NPC finishes casting a skill.
	 * @param npc the NPC that casted the skill.
	 * @param player the player who is the target of the skill. Can be {@code null}.
	 * @param skill the actual skill that was used by the NPC.
	 */
	public void onSpellFinished(Npc npc, Player player, Skill skill)
	{
	}
	
	/**
	 * This function is called whenever a trap action is performed.
	 * @param trap this parameter contains a reference to the exact instance of the trap that was activated.
	 * @param trigger this parameter contains a reference to the exact instance of the character that triggered the action.
	 * @param action this parameter contains a reference to the action that was triggered.
	 */
	public void onTrapAction(Trap trap, Creature trigger, TrapAction action)
	{
	}
	
	/**
	 * This function is called whenever an NPC spawns or re-spawns and passes a reference to the newly (re)spawned NPC.<br>
	 * Currently the only function that has no reference to a player.<br>
	 * It is useful for initializations, starting quest timers, displaying chat (NpcSay), and more.
	 * @param npc this parameter contains a reference to the exact instance of the NPC who just (re)spawned.
	 */
	public void onSpawn(Npc npc)
	{
	}
	
	/**
	 * This function is called whenever an NPC is teleport.
	 * @param npc this parameter contains a reference to the exact instance of the NPC who just teleport.
	 */
	protected void onTeleport(Npc npc)
	{
	}
	
	/**
	 * This function is called whenever an NPC is called by another NPC in the same faction.
	 * @param npc this parameter contains a reference to the exact instance of the NPC who is being asked for help.
	 * @param caller this parameter contains a reference to the exact instance of the NPC who is asking for help.
	 * @param attacker this parameter contains a reference to the exact instance of the player who attacked.
	 * @param isSummon this parameter if it's {@code false} it denotes that the attacker was indeed the player, else it specifies that the attacker was the player's summon.
	 */
	public void onFactionCall(Npc npc, Npc caller, Player attacker, boolean isSummon)
	{
	}
	
	/**
	 * This function is called whenever a player enters an NPC aggression range.
	 * @param npc this parameter contains a reference to the exact instance of the NPC whose aggression range is being transgressed.
	 * @param player this parameter contains a reference to the exact instance of the player who is entering the NPC's aggression range.
	 * @param isSummon this parameter if it's {@code false} it denotes that the character that entered the aggression range was indeed the player, else it specifies that the character was the player's summon.
	 */
	public void onAggroRangeEnter(Npc npc, Player player, boolean isSummon)
	{
	}
	
	/**
	 * This function is called whenever an NPC "sees" a creature.
	 * @param npc the NPC who sees the creature
	 * @param creature the creature seen by the NPC
	 */
	public void onCreatureSee(Npc npc, Creature creature)
	{
	}
	
	/**
	 * This function is called whenever a player enters the game.
	 * @param player this parameter contains a reference to the exact instance of the player who is entering to the world.
	 */
	public void onEnterWorld(Player player)
	{
	}
	
	/**
	 * This function is called whenever a character enters a registered zone.
	 * @param creature this parameter contains a reference to the exact instance of the character who is entering the zone.
	 * @param zone this parameter contains a reference to the zone.
	 */
	public void onEnterZone(Creature creature, ZoneType zone)
	{
	}
	
	/**
	 * This function is called whenever a character exits a registered zone.
	 * @param creature this parameter contains a reference to the exact instance of the character who is exiting the zone.
	 * @param zone this parameter contains a reference to the zone.
	 */
	public void onExitZone(Creature creature, ZoneType zone)
	{
	}
	
	/**
	 * @param eventName - name of event
	 * @param sender - NPC, who sent event
	 * @param receiver - NPC, who received event
	 * @param reference - WorldObject to pass, if needed
	 * @return
	 */
	public String onEventReceived(String eventName, Npc sender, Npc receiver, WorldObject reference)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a NPC finishes moving
	 * @param npc registered NPC
	 */
	public void onMoveFinished(Npc npc)
	{
	}
	
	/**
	 * This function is called whenever a walker NPC (controlled by WalkingManager) arrive a walking node
	 * @param npc registered NPC
	 */
	public void onNodeArrived(Npc npc)
	{
	}
	
	/**
	 * This function is called whenever a walker NPC (controlled by WalkingManager) arrive to last node
	 * @param npc registered NPC
	 */
	public void onRouteFinished(Npc npc)
	{
	}
	
	/**
	 * @param mob
	 * @param player
	 * @param isSummon
	 * @return {@code true} if npc can hate the playable, {@code false} otherwise.
	 */
	public boolean onNpcHate(Attackable mob, Player player, boolean isSummon)
	{
		return true;
	}
	
	/**
	 * @param summon
	 */
	public void onSummonSpawn(Summon summon)
	{
	}
	
	/**
	 * @param summon
	 */
	public void onSummonTalk(Summon summon)
	{
	}
	
	/**
	 * @param npc
	 * @param player
	 * @return {@code true} if player can see this npc, {@code false} otherwise.
	 */
	public boolean onCanSeeMe(Npc npc, Player player)
	{
		return false;
	}
	
	/**
	 * Show an error message to the specified player.
	 * @param player the player to whom to send the error (must be a GM)
	 * @param t the {@link Throwable} to get the message/stacktrace from
	 * @return {@code false}
	 */
	public boolean showError(Player player, Throwable t)
	{
		LOGGER.log(Level.WARNING, getScriptFile().toAbsolutePath().toString(), t);
		if (t.getMessage() == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": " + t.getMessage());
		}
		
		if ((player != null) && player.getAccessLevel().isGm())
		{
			final String res = "<html><body><title>Script error</title>" + TraceUtil.getStackTrace(t) + "</body></html>";
			return showResult(player, res);
		}
		
		return false;
	}
	
	/**
	 * @param player the player to whom to show the result
	 * @param res the message to show to the player
	 * @return {@code false} if the message was sent, {@code true} otherwise
	 * @see #showResult(Player, String, Npc)
	 */
	public boolean showResult(Player player, String res)
	{
		return showResult(player, res, null);
	}
	
	/**
	 * Show a message to the specified player.<br>
	 * <u><i>Concept:</i></u><br>
	 * Three cases are managed according to the value of the {@code res} parameter:<br>
	 * <ul>
	 * <li><u>{@code res} ends with ".htm" or ".html":</u> the contents of the specified HTML file are shown in a dialog window</li>
	 * <li><u>{@code res} starts with "&lt;html&gt;":</u> the contents of the parameter are shown in a dialog window</li>
	 * <li><u>all other cases :</u> the text contained in the parameter is shown in chat</li>
	 * </ul>
	 * @param player the player to whom to show the result
	 * @param npc npc to show the result for
	 * @param res the message to show to the player
	 * @return {@code false} if the message was sent, {@code true} otherwise
	 */
	public boolean showResult(Player player, String res, Npc npc)
	{
		if ((res == null) || res.isEmpty() || (player == null))
		{
			return true;
		}
		
		if (res.endsWith(".htm") || res.endsWith(".html"))
		{
			showHtmlFile(player, res, npc);
		}
		else if (res.startsWith("<html"))
		{
			final NpcHtmlMessage npcReply = new NpcHtmlMessage(npc != null ? npc.getObjectId() : 0, res);
			npcReply.replace("%playername%", player.getName());
			player.sendPacket(npcReply);
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
		else
		{
			player.sendMessage(res);
		}
		
		return false;
	}
	
	/**
	 * Loads all quest states and variables for the specified player.
	 * @param player the player who is entering the world
	 */
	public static void playerEnter(Player player)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement invalidQuestData = con.prepareStatement("DELETE FROM character_quests WHERE charId = ? AND name = ?");
			PreparedStatement invalidQuestDataVar = con.prepareStatement("DELETE FROM character_quests WHERE charId = ? AND name = ? AND var = ?");
			PreparedStatement ps1 = con.prepareStatement("SELECT name, value FROM character_quests WHERE charId = ? AND var = ?"))
		{
			// Get list of quests owned by the player from database
			ps1.setInt(1, player.getObjectId());
			ps1.setString(2, "<state>");
			try (ResultSet rs = ps1.executeQuery())
			{
				while (rs.next())
				{
					// Get the ID of the quest and its state
					final String questId = rs.getString("name");
					final String statename = rs.getString("value");
					
					// Search quest associated with the ID
					final Quest q = ScriptManager.getInstance().getScript(questId);
					if (q == null)
					{
						LOGGER.finer("Unknown quest " + questId + " for " + player);
						if (GeneralConfig.AUTODELETE_INVALID_QUEST_DATA)
						{
							invalidQuestData.setInt(1, player.getObjectId());
							invalidQuestData.setString(2, questId);
							invalidQuestData.executeUpdate();
						}
						continue;
					}
					
					// Create a new QuestState for the player that will be added to the player's list of quests
					new QuestState(q, player, State.getStateId(statename));
				}
			}
			
			// Get list of quests owned by the player from the DB in order to add variables used in the quest.
			try (PreparedStatement ps2 = con.prepareStatement("SELECT name, var, value FROM character_quests WHERE charId = ? AND var <> ?"))
			{
				ps2.setInt(1, player.getObjectId());
				ps2.setString(2, "<state>");
				try (ResultSet rs = ps2.executeQuery())
				{
					while (rs.next())
					{
						final String questId = rs.getString("name");
						final String var = rs.getString("var");
						final String value = rs.getString("value");
						
						// Get the QuestState saved in the loop before.
						final Quest quest = ScriptManager.getInstance().getScript(questId);
						if (quest != null)
						{
							final QuestState qs = quest.getQuestState(player, true);
							if (qs == null)
							{
								LOGGER.finer("Lost variable " + var + " in quest " + questId + " for " + player);
								
								if (GeneralConfig.AUTODELETE_INVALID_QUEST_DATA)
								{
									invalidQuestDataVar.setInt(1, player.getObjectId());
									invalidQuestDataVar.setString(2, questId);
									invalidQuestDataVar.setString(3, var);
									invalidQuestDataVar.executeUpdate();
								}
								continue;
							}
							
							// Add parameter to the quest.
							qs.setInternal(var, value);
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "could not insert char quest:", e);
		}
		
		// Events? Remove this?
		for (String name : ScriptManager.getInstance().getScripts().keySet())
		{
			player.processScriptEvent(name, "enter");
		}
	}
	
	/**
	 * Insert in the database the quest for the player.
	 * @param qs the {@link QuestState} object whose variable to insert
	 * @param var the name of the variable
	 * @param value the value of the variable
	 */
	public static void createQuestVarInDb(QuestState qs, String var, String value)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO character_quests (charId,name,var,value) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE value=?"))
		{
			statement.setInt(1, qs.getPlayer().getObjectId());
			statement.setString(2, qs.getQuestName());
			statement.setString(3, var);
			statement.setString(4, value);
			statement.setString(5, value);
			statement.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "could not insert char quest:", e);
		}
	}
	
	/**
	 * Update the value of the variable "var" for the specified quest in database
	 * @param qs the {@link QuestState} object whose variable to update
	 * @param var the name of the variable
	 * @param value the value of the variable
	 */
	public static void updateQuestVarInDb(QuestState qs, String var, String value)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE character_quests SET value=? WHERE charId=? AND name=? AND var = ?"))
		{
			statement.setString(1, value);
			statement.setInt(2, qs.getPlayer().getObjectId());
			statement.setString(3, qs.getQuestName());
			statement.setString(4, var);
			statement.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "could not update char quest:", e);
		}
	}
	
	/**
	 * Delete a variable of player's quest from the database.
	 * @param qs the {@link QuestState} object whose variable to delete
	 * @param var the name of the variable to delete
	 */
	public static void deleteQuestVarInDb(QuestState qs, String var)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM character_quests WHERE charId=? AND name=? AND var=?"))
		{
			statement.setInt(1, qs.getPlayer().getObjectId());
			statement.setString(2, qs.getQuestName());
			statement.setString(3, var);
			statement.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Unable to delete char quest!", e);
		}
	}
	
	/**
	 * Delete from the database all variables and states of the specified quest state.
	 * @param qs the {@link QuestState} object whose variables to delete
	 * @param repeatable if {@code false}, the state variable will be preserved, otherwise it will be deleted as well
	 */
	public static void deleteQuestInDb(QuestState qs, boolean repeatable)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(repeatable ? QUEST_DELETE_FROM_CHAR_QUERY : QUEST_DELETE_FROM_CHAR_QUERY_NON_REPEATABLE_QUERY))
		{
			ps.setInt(1, qs.getPlayer().getObjectId());
			ps.setString(2, qs.getQuestName());
			if (!repeatable)
			{
				ps.setString(3, "<state>");
			}
			
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "could not delete char quest:", e);
		}
	}
	
	/**
	 * Create a database record for the specified quest state.
	 * @param qs the {@link QuestState} object whose data to write in the database
	 */
	public static void createQuestInDb(QuestState qs)
	{
		createQuestVarInDb(qs, "<state>", State.getStateName(qs.getState()));
	}
	
	/**
	 * Update a quest state record of the specified quest state in database.
	 * @param qs the {@link QuestState} object whose data to update in the database
	 */
	public static void updateQuestInDb(QuestState qs)
	{
		updateQuestVarInDb(qs, "<state>", State.getStateName(qs.getState()));
	}
	
	/**
	 * @param player the player whose language settings to use in finding the html of the right language
	 * @return the default html for when no quest is available: "You are either not on a quest that involves this NPC.."
	 */
	public static String getNoQuestMsg(Player player)
	{
		final String result = HtmCache.getInstance().getHtm(player, "data/html/noquest.htm");
		return (result != null) && (result.length() > 0) ? result : DEFAULT_NO_QUEST_MSG;
	}
	
	/**
	 * @param player the player whose language settings to use in finding the html of the right language
	 * @return the default html for when no quest is already completed: "This quest has already been completed."
	 */
	public static String getAlreadyCompletedMsg(Player player)
	{
		final String result = HtmCache.getInstance().getHtm(player, "data/html/alreadycompleted.htm");
		return (result != null) && (result.length() > 0) ? result : DEFAULT_ALREADY_COMPLETED_MSG;
	}
	
	/**
	 * Add the quest to the NPC's startQuest
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addStartNpc(int... npcIds)
	{
		setNpcQuestStartId(npcIds);
	}
	
	/**
	 * Add the quest to the NPC's startQuest
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addStartNpc(Collection<Integer> npcIds)
	{
		setNpcQuestStartId(npcIds);
	}
	
	/**
	 * Add the quest to the NPC's first-talk (default action dialog).
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addFirstTalkId(int... npcIds)
	{
		setNpcFirstTalkId(event -> notifyFirstTalk(event.getNpc(), event.getPlayer()), npcIds);
	}
	
	/**
	 * Add the quest to the NPC's first-talk (default action dialog).
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addFirstTalkId(Collection<Integer> npcIds)
	{
		setNpcFirstTalkId(event -> notifyFirstTalk(event.getNpc(), event.getPlayer()), npcIds);
	}
	
	/**
	 * Add the NPC to the AcquireSkill dialog.
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addAcquireSkillId(int... npcIds)
	{
		setPlayerSkillLearnId(event -> onAcquireSkill(event.getTrainer(), event.getPlayer(), event.getSkill(), event.getAcquireType()), npcIds);
	}
	
	/**
	 * Add the NPC to the AcquireSkill dialog.
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addAcquireSkillId(Collection<Integer> npcIds)
	{
		setPlayerSkillLearnId(event -> onAcquireSkill(event.getTrainer(), event.getPlayer(), event.getSkill(), event.getAcquireType()), npcIds);
	}
	
	/**
	 * Add the Item to the notify when player speaks with it.
	 * @param itemIds the IDs of the Item to register
	 */
	public void addItemBypassEventId(int... itemIds)
	{
		setItemBypassEvenId(event -> onItemEvent(event.getItem(), event.getPlayer(), event.getEvent()), itemIds);
	}
	
	/**
	 * Add the Item to the notify when player speaks with it.
	 * @param itemIds the IDs of the Item to register
	 */
	public void addItemBypassEventId(Collection<Integer> itemIds)
	{
		setItemBypassEvenId(event -> onItemEvent(event.getItem(), event.getPlayer(), event.getEvent()), itemIds);
	}
	
	/**
	 * Add the Item to the notify when player speaks with it.
	 * @param itemIds the IDs of the Item to register
	 */
	public void addItemTalkId(int... itemIds)
	{
		setItemTalkId(event -> notifyItemTalk(event.getItem(), event.getPlayer()), itemIds);
	}
	
	/**
	 * Add the Item to the notify when player speaks with it.
	 * @param itemIds the IDs of the Item to register
	 */
	public void addItemTalkId(Collection<Integer> itemIds)
	{
		setItemTalkId(event -> notifyItemTalk(event.getItem(), event.getPlayer()), itemIds);
	}
	
	/**
	 * Add this quest to the list of quests that the passed mob will respond to for attack events.
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addAttackId(int... npcIds)
	{
		setAttackableAttackId(attack -> onAttack(attack.getTarget(), attack.getAttacker(), attack.getDamage(), attack.isSummon(), attack.getSkill()), npcIds);
	}
	
	/**
	 * Add this quest to the list of quests that the passed mob will respond to for attack events.
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addAttackId(Collection<Integer> npcIds)
	{
		setAttackableAttackId(attack -> onAttack(attack.getTarget(), attack.getAttacker(), attack.getDamage(), attack.isSummon(), attack.getSkill()), npcIds);
	}
	
	/**
	 * Add this quest to the list of quests that the passed mob will respond to for kill events.
	 * @param npcIds
	 */
	public void addKillId(int... npcIds)
	{
		setAttackableKillId(kill -> onKill(kill.getTarget(), kill.getAttacker(), kill.isSummon()), npcIds);
	}
	
	/**
	 * Add this quest event to the collection of NPC IDs that will respond to for on kill events.
	 * @param npcIds the collection of NPC IDs
	 */
	public void addKillId(Collection<Integer> npcIds)
	{
		setAttackableKillId(kill -> onKill(kill.getTarget(), kill.getAttacker(), kill.isSummon()), npcIds);
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for Talk Events.
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addTalkId(int... npcIds)
	{
		setNpcTalkId(npcIds);
	}
	
	public void addTalkId(Collection<Integer> npcIds)
	{
		setNpcTalkId(npcIds);
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for Teleport Events.
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addTeleportId(int... npcIds)
	{
		setNpcTeleportId(event -> notifyTeleport(event.getNpc()), npcIds);
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for Teleport Events.
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addTeleportId(Collection<Integer> npcIds)
	{
		setNpcTeleportId(event -> notifyTeleport(event.getNpc()), npcIds);
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for spawn events.
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addSpawnId(int... npcIds)
	{
		setNpcSpawnId(event -> onSpawn(event.getNpc()), npcIds);
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for spawn events.
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addSpawnId(Collection<Integer> npcIds)
	{
		setNpcSpawnId(event -> onSpawn(event.getNpc()), npcIds);
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for skill see events.
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addSkillSeeId(int... npcIds)
	{
		setNpcSkillSeeId(event -> onSkillSee(event.getTarget(), event.getCaster(), event.getSkill(), event.getTargets(), event.isSummon()), npcIds);
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for skill see events.
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addSkillSeeId(Collection<Integer> npcIds)
	{
		setNpcSkillSeeId(event -> onSkillSee(event.getTarget(), event.getCaster(), event.getSkill(), event.getTargets(), event.isSummon()), npcIds);
	}
	
	/**
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addSpellFinishedId(int... npcIds)
	{
		setNpcSkillFinishedId(event -> onSpellFinished(event.getCaster(), event.getTarget(), event.getSkill()), npcIds);
	}
	
	/**
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addSpellFinishedId(Collection<Integer> npcIds)
	{
		setNpcSkillFinishedId(event -> onSpellFinished(event.getCaster(), event.getTarget(), event.getSkill()), npcIds);
	}
	
	/**
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addTrapActionId(int... npcIds)
	{
		setTrapActionId(event -> onTrapAction(event.getTrap(), event.getTrigger(), event.getAction()), npcIds);
	}
	
	/**
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addTrapActionId(Collection<Integer> npcIds)
	{
		setTrapActionId(event -> onTrapAction(event.getTrap(), event.getTrigger(), event.getAction()), npcIds);
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for faction call events.
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addFactionCallId(int... npcIds)
	{
		setAttackableFactionIdId(event -> onFactionCall(event.getNpc(), event.getCaller(), event.getAttacker(), event.isSummon()), npcIds);
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for faction call events.
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addFactionCallId(Collection<Integer> npcIds)
	{
		setAttackableFactionIdId(event -> onFactionCall(event.getNpc(), event.getCaller(), event.getAttacker(), event.isSummon()), npcIds);
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for character see events.
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addAggroRangeEnterId(int... npcIds)
	{
		setAttackableAggroRangeEnterId(event -> onAggroRangeEnter(event.getNpc(), event.getPlayer(), event.isSummon()), npcIds);
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for character see events.
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addAggroRangeEnterId(Collection<Integer> npcIds)
	{
		setAttackableAggroRangeEnterId(event -> onAggroRangeEnter(event.getNpc(), event.getPlayer(), event.isSummon()), npcIds);
	}
	
	/**
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addCreatureSeeId(int... npcIds)
	{
		setCreatureSeeId(event -> onCreatureSee(event.getCreature().asNpc(), event.getSeen()), npcIds);
	}
	
	/**
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addCreatureSeeId(Collection<Integer> npcIds)
	{
		setCreatureSeeId(event -> onCreatureSee(event.getCreature().asNpc(), event.getSeen()), npcIds);
	}
	
	/**
	 * Register onEnterZone trigger for zone
	 * @param zoneId the ID of the zone to register
	 */
	public void addEnterZoneId(int zoneId)
	{
		setCreatureZoneEnterId(event -> onEnterZone(event.getCreature(), event.getZone()), zoneId);
	}
	
	/**
	 * Register onEnterZone trigger for zones
	 * @param zoneIds the IDs of the zones to register
	 */
	public void addEnterZoneId(int... zoneIds)
	{
		setCreatureZoneEnterId(event -> onEnterZone(event.getCreature(), event.getZone()), zoneIds);
	}
	
	/**
	 * Register onEnterZone trigger for zones
	 * @param zoneIds the IDs of the zones to register
	 */
	public void addEnterZoneId(Collection<Integer> zoneIds)
	{
		setCreatureZoneEnterId(event -> onEnterZone(event.getCreature(), event.getZone()), zoneIds);
	}
	
	/**
	 * Register onExitZone trigger for zone
	 * @param zoneId the ID of the zone to register
	 */
	public void addExitZoneId(int zoneId)
	{
		setCreatureZoneExitId(event -> onExitZone(event.getCreature(), event.getZone()), zoneId);
	}
	
	/**
	 * Register onExitZone trigger for zones
	 * @param zoneIds the IDs of the zones to register
	 */
	public void addExitZoneId(int... zoneIds)
	{
		setCreatureZoneExitId(event -> onExitZone(event.getCreature(), event.getZone()), zoneIds);
	}
	
	/**
	 * Register onExitZone trigger for zones
	 * @param zoneIds the IDs of the zones to register
	 */
	public void addExitZoneId(Collection<Integer> zoneIds)
	{
		setCreatureZoneExitId(event -> onExitZone(event.getCreature(), event.getZone()), zoneIds);
	}
	
	/**
	 * Register onEventReceived trigger for NPC
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addEventReceivedId(int... npcIds)
	{
		setNpcEventReceivedId(event -> notifyEventReceived(event.getEventName(), event.getSender(), event.getReceiver(), event.getReference()), npcIds);
	}
	
	/**
	 * Register onEventReceived trigger for NPC
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addEventReceivedId(Collection<Integer> npcIds)
	{
		setNpcEventReceivedId(event -> notifyEventReceived(event.getEventName(), event.getSender(), event.getReceiver(), event.getReference()), npcIds);
	}
	
	/**
	 * Register onMoveFinished trigger for NPC
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addMoveFinishedId(int... npcIds)
	{
		setNpcMoveFinishedId(event -> notifyMoveFinished(event.getNpc()), npcIds);
	}
	
	/**
	 * Register onMoveFinished trigger for NPC
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addMoveFinishedId(Collection<Integer> npcIds)
	{
		setNpcMoveFinishedId(event -> notifyMoveFinished(event.getNpc()), npcIds);
	}
	
	/**
	 * Register onNodeArrived trigger for NPC
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addNodeArrivedId(int... npcIds)
	{
		setNpcMoveNodeArrivedId(event -> notifyNodeArrived(event.getNpc()), npcIds);
	}
	
	/**
	 * Register onNodeArrived trigger for NPC
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addNodeArrivedId(Collection<Integer> npcIds)
	{
		setNpcMoveNodeArrivedId(event -> notifyNodeArrived(event.getNpc()), npcIds);
	}
	
	/**
	 * Register onRouteFinished trigger for NPC
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addRouteFinishedId(int... npcIds)
	{
		setNpcMoveRouteFinishedId(event -> notifyRouteFinished(event.getNpc()), npcIds);
	}
	
	/**
	 * Register onRouteFinished trigger for NPC
	 * @param npcIds the IDs of the NPCs to register
	 */
	public void addRouteFinishedId(Collection<Integer> npcIds)
	{
		setNpcMoveRouteFinishedId(event -> notifyRouteFinished(event.getNpc()), npcIds);
	}
	
	/**
	 * Register onNpcHate trigger for NPC
	 * @param npcIds
	 */
	public void addNpcHateId(int... npcIds)
	{
		addNpcHateId(event -> new TerminateReturn(!onNpcHate(event.getNpc(), event.getPlayer(), event.isSummon()), false, false), npcIds);
	}
	
	/**
	 * Register onNpcHate trigger for NPC
	 * @param npcIds
	 */
	public void addNpcHateId(Collection<Integer> npcIds)
	{
		addNpcHateId(event -> new TerminateReturn(!onNpcHate(event.getNpc(), event.getPlayer(), event.isSummon()), false, false), npcIds);
	}
	
	/**
	 * Register onSummonSpawn trigger when summon is spawned.
	 * @param npcIds
	 */
	public void addSummonSpawnId(int... npcIds)
	{
		setPlayerSummonSpawnId(event -> onSummonSpawn(event.getSummon()), npcIds);
	}
	
	/**
	 * Register onSummonSpawn trigger when summon is spawned.
	 * @param npcIds
	 */
	public void addSummonSpawnId(Collection<Integer> npcIds)
	{
		setPlayerSummonSpawnId(event -> onSummonSpawn(event.getSummon()), npcIds);
	}
	
	/**
	 * Register onSummonTalk trigger when master talked to summon.
	 * @param npcIds
	 */
	public void addSummonTalkId(int... npcIds)
	{
		setPlayerSummonTalkId(event -> onSummonTalk(event.getSummon()), npcIds);
	}
	
	/**
	 * Register onSummonTalk trigger when summon is spawned.
	 * @param npcIds
	 */
	public void addSummonTalkId(Collection<Integer> npcIds)
	{
		setPlayerSummonTalkId(event -> onSummonTalk(event.getSummon()), npcIds);
	}
	
	/**
	 * Registers onCanSeeMe trigger whenever an npc info must be sent to player.
	 * @param npcIds
	 */
	public void addCanSeeMeId(int... npcIds)
	{
		addNpcHateId(event -> new TerminateReturn(!notifyOnCanSeeMe(event.getNpc(), event.getPlayer()), false, false), npcIds);
	}
	
	/**
	 * Registers onCanSeeMe trigger whenever an npc info must be sent to player.
	 * @param npcIds
	 */
	public void addCanSeeMeId(Collection<Integer> npcIds)
	{
		addNpcHateId(event -> new TerminateReturn(!notifyOnCanSeeMe(event.getNpc(), event.getPlayer()), false, false), npcIds);
	}
	
	/**
	 * Use this method to get a random party member from a player's party.<br>
	 * Useful when distributing rewards after killing an NPC.
	 * @param player this parameter represents the player whom the party will taken.
	 * @return {@code null} if {@code player} is {@code null}, {@code player} itself if the player does not have a party, and a random party member in all other cases
	 */
	public static Player getRandomPartyMember(Player player)
	{
		if (player == null)
		{
			return null;
		}
		
		final Party party = player.getParty();
		if (party == null)
		{
			return player;
		}
		
		final List<Player> members = party.getMembers();
		if (members.isEmpty())
		{
			return player;
		}
		
		Player member = members.get(Rnd.get(members.size()));
		while (player.getInstanceId() != member.getInstanceId())
		{
			member = members.get(Rnd.get(members.size()));
		}
		
		return member;
	}
	
	/**
	 * Get a random party member with required cond value.
	 * @param player the instance of a player whose party is to be searched
	 * @param cond the value of the "cond" variable that must be matched
	 * @return a random party member that matches the specified condition, or {@code null} if no match was found
	 */
	public Player getRandomPartyMember(Player player, int cond)
	{
		return getRandomPartyMember(player, "cond", String.valueOf(cond));
	}
	
	/**
	 * Auxiliary function for party quests.<br>
	 * Note: This function is only here because of how commonly it may be used by quest developers.<br>
	 * For any variations on this function, the quest script can always handle things on its own.
	 * @param player the instance of a player whose party is to be searched
	 * @param var the quest variable to look for in party members. If {@code null}, it simply unconditionally returns a random party member
	 * @param value the value of the specified quest variable the random party member must have
	 * @return a random party member that matches the specified conditions or {@code null} if no match was found.<br>
	 *         If the {@code var} parameter is {@code null}, a random party member is selected without any conditions.<br>
	 *         The party member must be within a range of 1500 ingame units of the target of the reference player, or, if no target exists, within the same range of the player itself
	 */
	public Player getRandomPartyMember(Player player, String var, String value)
	{
		// if no valid player instance is passed, there is nothing to check...
		if (player == null)
		{
			return null;
		}
		
		// for null var condition, return any random party member.
		if (var == null)
		{
			return getRandomPartyMember(player);
		}
		
		// normal cases...if the player is not in a party, check the player's state
		QuestState temp = null;
		final Party party = player.getParty();
		
		// if this player is not in a party, just check if this player instance matches the conditions itself
		if ((party == null) || party.getMembers().isEmpty())
		{
			temp = player.getQuestState(getName());
			if ((temp != null) && temp.isSet(var) && temp.get(var).equalsIgnoreCase(value))
			{
				return player; // match
			}
			
			return null; // no match
		}
		
		// if the player is in a party, gather a list of all matching party members (possibly including this player)
		final List<Player> candidates = new ArrayList<>();
		
		// get the target for enforcing distance limitations.
		WorldObject target = player.getTarget();
		if (target == null)
		{
			target = player;
		}
		
		for (Player partyMember : party.getMembers())
		{
			if (partyMember == null)
			{
				continue;
			}
			
			temp = partyMember.getQuestState(getName());
			if ((temp != null) && (temp.get(var) != null) && (temp.get(var)).equalsIgnoreCase(value) && partyMember.isInsideRadius3D(target, PlayerConfig.ALT_PARTY_RANGE) && (player.getInstanceId() == partyMember.getInstanceId()))
			{
				candidates.add(partyMember);
			}
		}
		
		// if there was no match, return null...
		if (candidates.isEmpty())
		{
			return null;
		}
		
		// if a match was found from the party, return one of them at random.
		return candidates.get(Rnd.get(candidates.size()));
	}
	
	/**
	 * Auxiliary function for party quests.<br>
	 * Note: This function is only here because of how commonly it may be used by quest developers.<br>
	 * For any variations on this function, the quest script can always handle things on its own.
	 * @param player the player whose random party member is to be selected
	 * @param state the quest state required of the random party member
	 * @return {@code null} if nothing was selected or a random party member that has the specified quest state
	 */
	public Player getRandomPartyMemberState(Player player, byte state)
	{
		// if no valid player instance is passed, there is nothing to check...
		if (player == null)
		{
			return null;
		}
		
		// normal cases...if the player is not in a party check the player's state
		QuestState temp = null;
		final Party party = player.getParty();
		
		// if this player is not in a party, just check if this player instance matches the conditions itself
		if ((party == null) || party.getMembers().isEmpty())
		{
			temp = player.getQuestState(getName());
			if ((temp != null) && (temp.getState() == state))
			{
				return player; // match
			}
			
			return null; // no match
		}
		
		// if the player is in a party, gather a list of all matching party members (possibly
		// including this player)
		final List<Player> candidates = new ArrayList<>();
		
		// get the target for enforcing distance limitations.
		WorldObject target = player.getTarget();
		if (target == null)
		{
			target = player;
		}
		
		for (Player partyMember : party.getMembers())
		{
			if (partyMember == null)
			{
				continue;
			}
			
			temp = partyMember.getQuestState(getName());
			if ((temp != null) && (temp.getState() == state) && partyMember.isInsideRadius3D(target, PlayerConfig.ALT_PARTY_RANGE) && (player.getInstanceId() == partyMember.getInstanceId()))
			{
				candidates.add(partyMember);
			}
		}
		
		// if there was no match, return null...
		if (candidates.isEmpty())
		{
			return null;
		}
		
		// if a match was found from the party, return one of them at random.
		return candidates.get(Rnd.get(candidates.size()));
	}
	
	/**
	 * Get a random party member from the specified player's party.<br>
	 * If the player is not in a party, only the player himself is checked.<br>
	 * The lucky member is chosen by standard loot roll rules -<br>
	 * each member rolls a random number, the one with the highest roll wins.
	 * @param player the player whose party to check
	 * @param npc the NPC used for distance and other checks (if {@link #checkPartyMember(Player, Npc)} is overriden)
	 * @return the random party member or {@code null}
	 */
	public Player getRandomPartyMember(Player player, Npc npc)
	{
		if ((player == null) || !checkDistanceToTarget(player, npc))
		{
			return null;
		}
		
		final Party party = player.getParty();
		Player luckyPlayer = null;
		if (party == null)
		{
			if (checkPartyMember(player, npc))
			{
				luckyPlayer = player;
			}
		}
		else
		{
			int highestRoll = 0;
			for (Player member : party.getMembers())
			{
				final int rnd = getRandom(1000);
				if ((rnd > highestRoll) && checkPartyMember(member, npc))
				{
					highestRoll = rnd;
					luckyPlayer = member;
				}
			}
		}
		
		return (luckyPlayer != null) && checkDistanceToTarget(luckyPlayer, npc) ? luckyPlayer : null;
	}
	
	/**
	 * This method is called for every party member in {@link #getRandomPartyMember(Player, Npc)}.<br>
	 * It is intended to be overriden by the specific quest implementations.
	 * @param player the player to check
	 * @param npc the NPC that was passed to {@link #getRandomPartyMember(Player, Npc)}
	 * @return {@code true} if this party member passes the check, {@code false} otherwise
	 */
	public boolean checkPartyMember(Player player, Npc npc)
	{
		return true;
	}
	
	/**
	 * Get a random party member from the player's party who has this quest at the specified quest progress.<br>
	 * If the player is not in a party, only the player himself is checked.
	 * @param player the player whose random party member state to get
	 * @param condition the quest progress step the random member should be at (-1 = check only if quest is started)
	 * @param playerChance how many times more chance does the player get compared to other party members (3 - 3x more chance).<br>
	 *            On retail servers, the killer usually gets 2-3x more chance than other party members
	 * @param target the NPC to use for the distance check (can be null)
	 * @return the {@link QuestState} object of the random party member or {@code null} if none matched the condition
	 */
	public QuestState getRandomPartyMemberState(Player player, int condition, int playerChance, Npc target)
	{
		if ((player == null) || (playerChance < 1))
		{
			return null;
		}
		
		QuestState qs = player.getQuestState(getName());
		if (!player.isInParty())
		{
			return !checkPartyMemberConditions(qs, condition, target) || !checkDistanceToTarget(player, target) ? null : qs;
		}
		
		final List<QuestState> candidates = new ArrayList<>();
		if (checkPartyMemberConditions(qs, condition, target) && (playerChance > 0))
		{
			for (int i = 0; i < playerChance; i++)
			{
				candidates.add(qs);
			}
		}
		
		for (Player member : player.getParty().getMembers())
		{
			if (member == player)
			{
				continue;
			}
			
			qs = member.getQuestState(getName());
			if (checkPartyMemberConditions(qs, condition, target))
			{
				candidates.add(qs);
			}
		}
		
		if (candidates.isEmpty())
		{
			return null;
		}
		
		qs = candidates.get(getRandom(candidates.size()));
		return !checkDistanceToTarget(qs.getPlayer(), target) ? null : qs;
	}
	
	private boolean checkPartyMemberConditions(QuestState qs, int condition, Npc npc)
	{
		return (qs != null) && ((condition == -1) ? qs.isStarted() : qs.isCond(condition)) && checkPartyMember(qs, npc);
	}
	
	private static boolean checkDistanceToTarget(Player player, Npc target)
	{
		return (target == null) || LocationUtil.checkIfInRange(PlayerConfig.ALT_PARTY_RANGE, player, target, true);
	}
	
	/**
	 * This method is called for every party member in {@link #getRandomPartyMemberState(Player, int, int, Npc)} if/after all the standard checks are passed.<br>
	 * It is intended to be overriden by the specific quest implementations.<br>
	 * It can be used in cases when there are more checks performed than simply a quest condition check,<br>
	 * for example, if an item is required in the player's inventory.
	 * @param qs the {@link QuestState} object of the party member
	 * @param npc the NPC that was passed as the last parameter to {@link #getRandomPartyMemberState(Player, int, int, Npc)}
	 * @return {@code true} if this party member passes the check, {@code false} otherwise
	 */
	public boolean checkPartyMember(QuestState qs, Npc npc)
	{
		return true;
	}
	
	/**
	 * Send an HTML file to the specified player.
	 * @param player the player to send the HTML to
	 * @param filename the name of the HTML file to show
	 * @return the contents of the HTML file that was sent to the player
	 * @see #showHtmlFile(Player, String, Npc)
	 */
	public String showHtmlFile(Player player, String filename)
	{
		return showHtmlFile(player, filename, null);
	}
	
	/**
	 * Send an HTML file to the specified player.
	 * @param player the player to send the HTML file to
	 * @param filename the name of the HTML file to show
	 * @param npc the NPC that is showing the HTML file
	 * @return the contents of the HTML file that was sent to the player
	 * @see #showHtmlFile(Player, String, Npc)
	 */
	public String showHtmlFile(Player player, String filename, Npc npc)
	{
		// final boolean questwindow = !filename.endsWith(".html");
		
		// Create handler to file linked to the quest
		String content = getHtm(player, filename);
		
		// Send message to client if message not empty
		if (content != null)
		{
			if (npc != null)
			{
				content = content.replace("%objectId%", String.valueOf(npc.getObjectId()));
			}
			
			// if (questwindow && (_questId > 0) && (_questId < 20000) && (_questId != 999))
			// {
			// final NpcQuestHtmlMessage npcReply = new NpcQuestHtmlMessage(npc != null ? npc.getObjectId() : 0, _questId);
			// npcReply.setHtml(content);
			// npcReply.replace("%playername%", player.getName());
			// player.sendPacket(npcReply);
			// }
			// else
			// {
			final NpcHtmlMessage npcReply = new NpcHtmlMessage(npc != null ? npc.getObjectId() : 0, content);
			npcReply.replace("%playername%", player.getName());
			player.sendPacket(npcReply);
			
			// }
			
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
		
		return content;
	}
	
	/**
	 * @param player for language prefix.
	 * @param fileName the html file to be get.
	 * @return the HTML file contents
	 */
	public String getHtm(Player player, String fileName)
	{
		final HtmCache hc = HtmCache.getInstance();
		String content = hc.getHtm(player, fileName.startsWith("data/") ? fileName : "data/scripts/" + getPath() + "/" + fileName);
		if (content == null)
		{
			content = hc.getHtm(player, "data/scripts/" + getPath() + "/" + fileName);
			if (content == null)
			{
				content = hc.getHtm(player, "data/scripts/quests/" + getName() + "/" + fileName);
			}
		}
		
		return content;
	}
	
	/**
	 * @return the registered quest items IDs.
	 */
	public int[] getRegisteredItemIds()
	{
		return _questItemIds;
	}
	
	/**
	 * Registers all items that have to be destroyed in case player abort the quest or finish it.
	 * @param items
	 */
	public void registerQuestItems(int... items)
	{
		for (int id : items)
		{
			if ((id != 0) && (ItemData.getInstance().getTemplate(id) == null))
			{
				LOGGER.severe(super.getClass().getSimpleName() + ": Found registerQuestItems for non existing item: " + id + "!");
			}
		}
		
		_questItemIds = items;
	}
	
	/**
	 * Remove all quest items associated with this quest from the specified player's inventory.
	 * @param player the player whose quest items to remove
	 */
	public void removeRegisteredQuestItems(Player player)
	{
		takeItems(player, -1, _questItemIds);
	}
	
	public void setOnEnterWorld(boolean value)
	{
		if (value)
		{
			setPlayerLoginId(event -> onEnterWorld(event.getPlayer()));
		}
		else
		{
			for (AbstractEventListener listener : getListeners())
			{
				if (listener.getType() == EventType.ON_PLAYER_LOGIN)
				{
					listener.unregisterMe();
				}
			}
		}
	}
	
	/**
	 * Gets the start conditions.
	 * @return the start conditions
	 */
	private Map<Predicate<Player>, String> getStartConditions()
	{
		return _startCondition;
	}
	
	/**
	 * Verifies if the player meets all the start conditions.
	 * @param player the player
	 * @return {@code true} if all conditions are met
	 */
	public boolean canStartQuest(Player player)
	{
		for (Predicate<Player> cond : _startCondition.keySet())
		{
			if (!cond.test(player))
			{
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Gets the HTML for the first starting condition not met.
	 * @param player the player
	 * @return the HTML
	 */
	public String getStartConditionHtml(Player player)
	{
		for (Entry<Predicate<Player>, String> startRequirement : _startCondition.entrySet())
		{
			if (!startRequirement.getKey().test(player))
			{
				return startRequirement.getValue();
			}
		}
		
		return null;
	}
	
	/**
	 * Adds a predicate to the start conditions.
	 * @param questStartRequirement the predicate condition
	 * @param html the HTML to display if that condition is not met
	 */
	public void addCondStart(Predicate<Player> questStartRequirement, String html)
	{
		getStartConditions().put(questStartRequirement, html);
	}
	
	/**
	 * Adds a minimum/maximum level start condition to the quest.
	 * @param minLevel the minimum player's level to start the quest
	 * @param maxLevel the maximum player's level to start the quest
	 * @param html the HTML to display if the condition is not met
	 */
	public void addCondLevel(int minLevel, int maxLevel, String html)
	{
		getStartConditions().put(p -> (p.getLevel() >= minLevel) && (p.getLevel() <= maxLevel), html);
	}
	
	/**
	 * Adds a minimum level start condition to the quest.
	 * @param minLevel the minimum player's level to start the quest
	 * @param html the HTML to display if the condition is not met
	 */
	public void addCondMinLevel(int minLevel, String html)
	{
		getStartConditions().put(p -> p.getLevel() >= minLevel, html);
	}
	
	/**
	 * Adds a minimum/maximum level start condition to the quest.
	 * @param maxLevel the maximum player's level to start the quest
	 * @param html the HTML to display if the condition is not met
	 */
	public void addCondMaxLevel(int maxLevel, String html)
	{
		getStartConditions().put(p -> p.getLevel() <= maxLevel, html);
	}
	
	/**
	 * Adds a race start condition to the quest.
	 * @param race the race
	 * @param html the HTML to display if the condition is not met
	 */
	public void addCondRace(Race race, String html)
	{
		getStartConditions().put(p -> p.getRace() == race, html);
	}
	
	/**
	 * Adds a not-race start condition to the quest.
	 * @param race the race
	 * @param html the HTML to display if the condition is not met
	 */
	public void addCondNotRace(Race race, String html)
	{
		getStartConditions().put(p -> p.getRace() != race, html);
	}
	
	/**
	 * Adds a quest completed start condition to the quest.
	 * @param name the quest name
	 * @param html the HTML to display if the condition is not met
	 */
	public void addCondCompletedQuest(String name, String html)
	{
		getStartConditions().put(p -> p.hasQuestState(name) && p.getQuestState(name).isCompleted(), html);
	}
	
	/**
	 * Adds a class ID start condition to the quest.
	 * @param playerClass the class identifier as a {@link PlayerClass} enum value
	 * @param html the HTML to display if the condition is not met
	 */
	public void addCondClassId(PlayerClass playerClass, String html)
	{
		getStartConditions().put(p -> p.getPlayerClass() == playerClass, html);
	}
	
	/**
	 * Adds a not-class ID start condition to the quest.
	 * @param playerClass the class identifier as a {@link PlayerClass} enum value
	 * @param html the HTML to display if the condition is not met
	 */
	public void addCondNotClassId(PlayerClass playerClass, String html)
	{
		getStartConditions().put(p -> p.getPlayerClass() != playerClass, html);
	}
	
	/**
	 * Adds a subclass active start condition to the quest.
	 * @param html the HTML to display if the condition is not met
	 */
	public void addCondIsSubClassActive(String html)
	{
		getStartConditions().put(p -> p.isSubClassActive(), html);
	}
	
	/**
	 * Adds a not-subclass active start condition to the quest.
	 * @param html the HTML to display if the condition is not met
	 */
	public void addCondIsNotSubClassActive(String html)
	{
		getStartConditions().put(p -> !p.isSubClassActive(), html);
	}
	
	/**
	 * Adds a category start condition to the quest.
	 * @param categoryType the category type
	 * @param html the HTML to display if the condition is not met
	 */
	public void addCondInCategory(CategoryType categoryType, String html)
	{
		getStartConditions().put(p -> p.isInCategory(categoryType), html);
	}
	
	private void initializeAnnotationListeners()
	{
		final List<Integer> ids = new ArrayList<>();
		for (Method method : getClass().getMethods())
		{
			if (method.isAnnotationPresent(RegisterEvent.class) && method.isAnnotationPresent(RegisterType.class))
			{
				final RegisterEvent listener = method.getAnnotation(RegisterEvent.class);
				final RegisterType regType = method.getAnnotation(RegisterType.class);
				final ListenerRegisterType type = regType.value();
				final EventType eventType = listener.value();
				if (method.getParameterCount() != 1)
				{
					LOGGER.warning(getClass().getSimpleName() + ": Non properly defined annotation listener on method: " + method.getName() + " expected parameter count is 1 but found: " + method.getParameterCount());
					continue;
				}
				else if (!eventType.isEventClass(method.getParameterTypes()[0]))
				{
					LOGGER.warning(getClass().getSimpleName() + ": Non properly defined annotation listener on method: " + method.getName() + " expected parameter to be type of: " + eventType.getEventClass().getSimpleName() + " but found: " + method.getParameterTypes()[0].getSimpleName());
					continue;
				}
				else if (!eventType.isReturnClass(method.getReturnType()))
				{
					LOGGER.warning(getClass().getSimpleName() + ": Non properly defined annotation listener on method: " + method.getName() + " expected return type to be one of: " + Arrays.toString(eventType.getReturnClasses()) + " but found: " + method.getReturnType().getSimpleName());
					continue;
				}
				
				int priority = 0;
				
				// Clear the list
				ids.clear();
				
				// Scan for possible Id filters
				for (Annotation annotation : method.getAnnotations())
				{
					if (annotation instanceof Id)
					{
						final Id npc = (Id) annotation;
						for (int id : npc.value())
						{
							ids.add(id);
						}
					}
					else if (annotation instanceof Ids)
					{
						final Ids npcs = (Ids) annotation;
						for (Id npc : npcs.value())
						{
							for (int id : npc.value())
							{
								ids.add(id);
							}
						}
					}
					else if (annotation instanceof Range)
					{
						final Range range = (Range) annotation;
						if (range.from() > range.to())
						{
							LOGGER.warning(getClass().getSimpleName() + ": Wrong " + annotation.getClass().getSimpleName() + " from is higher then to!");
							continue;
						}
						
						for (int id = range.from(); id <= range.to(); id++)
						{
							ids.add(id);
						}
					}
					else if (annotation instanceof Ranges)
					{
						final Ranges ranges = (Ranges) annotation;
						for (Range range : ranges.value())
						{
							if (range.from() > range.to())
							{
								LOGGER.warning(getClass().getSimpleName() + ": Wrong " + annotation.getClass().getSimpleName() + " from is higher then to!");
								continue;
							}
							
							for (int id = range.from(); id <= range.to(); id++)
							{
								ids.add(id);
							}
						}
					}
					else if (annotation instanceof NpcLevelRange)
					{
						final NpcLevelRange range = (NpcLevelRange) annotation;
						if (range.from() > range.to())
						{
							LOGGER.warning(getClass().getSimpleName() + ": Wrong " + annotation.getClass().getSimpleName() + " from is higher then to!");
							continue;
						}
						else if (type != ListenerRegisterType.NPC)
						{
							LOGGER.warning(getClass().getSimpleName() + ": ListenerRegisterType " + type + " for " + annotation.getClass().getSimpleName() + " NPC is expected!");
							continue;
						}
						
						for (int level = range.from(); level <= range.to(); level++)
						{
							final List<NpcTemplate> templates = NpcData.getInstance().getAllOfLevel(level);
							templates.forEach(template -> ids.add(template.getId()));
						}
					}
					else if (annotation instanceof NpcLevelRanges)
					{
						final NpcLevelRanges ranges = (NpcLevelRanges) annotation;
						for (NpcLevelRange range : ranges.value())
						{
							if (range.from() > range.to())
							{
								LOGGER.warning(getClass().getSimpleName() + ": Wrong " + annotation.getClass().getSimpleName() + " from is higher then to!");
								continue;
							}
							else if (type != ListenerRegisterType.NPC)
							{
								LOGGER.warning(getClass().getSimpleName() + ": ListenerRegisterType " + type + " for " + annotation.getClass().getSimpleName() + " NPC is expected!");
								continue;
							}
							
							for (int level = range.from(); level <= range.to(); level++)
							{
								final List<NpcTemplate> templates = NpcData.getInstance().getAllOfLevel(level);
								templates.forEach(template -> ids.add(template.getId()));
							}
						}
					}
					else if (annotation instanceof Priority)
					{
						final Priority p = (Priority) annotation;
						priority = p.value();
					}
				}
				
				if (!ids.isEmpty())
				{
					_registeredIds.computeIfAbsent(type, _ -> ConcurrentHashMap.newKeySet()).addAll(ids);
				}
				
				registerAnnotation(method, eventType, type, priority, ids);
			}
		}
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Provides callback operation when Attackable dies from a player.
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setAttackableKillId(Consumer<OnAttackableKill> callback, int... npcIds)
	{
		for (int id : npcIds)
		{
			if (NpcData.getInstance().getTemplate(id) == null)
			{
				LOGGER.severe(super.getClass().getSimpleName() + ": Found addKillId for non existing NPC: " + id + "!");
			}
		}
		
		return registerConsumer(callback, EventType.ON_ATTACKABLE_KILL, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Provides callback operation when Attackable dies from a player.
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setAttackableKillId(Consumer<OnAttackableKill> callback, Collection<Integer> npcIds)
	{
		for (int id : npcIds)
		{
			if (NpcData.getInstance().getTemplate(id) == null)
			{
				LOGGER.severe(super.getClass().getSimpleName() + ": Found addKillId for non existing NPC: " + id + "!");
			}
		}
		
		return registerConsumer(callback, EventType.ON_ATTACKABLE_KILL, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Provides instant callback operation when Attackable dies from a player with return type.
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> addCreatureKillId(Function<OnCreatureDeath, ? extends AbstractEventReturn> callback, int... npcIds)
	{
		return registerFunction(callback, EventType.ON_CREATURE_DEATH, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Provides instant callback operation when Attackable dies from a player.
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setCreatureKillId(Consumer<OnCreatureDeath> callback, int... npcIds)
	{
		return registerConsumer(callback, EventType.ON_CREATURE_DEATH, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Provides instant callback operation when {@link Attackable} dies from a {@link Player}.
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setCreatureKillId(Consumer<OnCreatureDeath> callback, Collection<Integer> npcIds)
	{
		return registerConsumer(callback, EventType.ON_CREATURE_DEATH, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Provides instant callback operation when {@link Player} talk to {@link Npc} for first time.
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setNpcFirstTalkId(Consumer<OnNpcFirstTalk> callback, int... npcIds)
	{
		for (int id : npcIds)
		{
			if (NpcData.getInstance().getTemplate(id) == null)
			{
				LOGGER.severe(super.getClass().getSimpleName() + ": Found addFirstTalkId for non existing NPC: " + id + "!");
			}
		}
		
		return registerConsumer(callback, EventType.ON_NPC_FIRST_TALK, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Provides instant callback operation when {@link Player} talk to {@link Npc} for first time.
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setNpcFirstTalkId(Consumer<OnNpcFirstTalk> callback, Collection<Integer> npcIds)
	{
		for (int id : npcIds)
		{
			if (NpcData.getInstance().getTemplate(id) == null)
			{
				LOGGER.severe(super.getClass().getSimpleName() + ": Found addFirstTalkId for non existing NPC: " + id + "!");
			}
		}
		
		return registerConsumer(callback, EventType.ON_NPC_FIRST_TALK, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Provides instant callback operation when {@link Player} talk to {@link Npc}.
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setNpcTalkId(Collection<Integer> npcIds)
	{
		for (int id : npcIds)
		{
			if (NpcData.getInstance().getTemplate(id) == null)
			{
				LOGGER.severe(super.getClass().getSimpleName() + ": Found addTalkId for non existing NPC: " + id + "!");
			}
		}
		
		return registerDummy(EventType.ON_NPC_TALK, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Provides instant callback operation when {@link Player} talk to {@link Npc}.
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setNpcTalkId(int... npcIds)
	{
		for (int id : npcIds)
		{
			if (NpcData.getInstance().getTemplate(id) == null)
			{
				LOGGER.severe(super.getClass().getSimpleName() + ": Found addTalkId for non existing NPC: " + id + "!");
			}
		}
		
		return registerDummy(EventType.ON_NPC_TALK, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Provides instant callback operation when teleport {@link Npc}.
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setNpcTeleportId(Consumer<OnNpcTeleport> callback, Collection<Integer> npcIds)
	{
		return registerConsumer(callback, EventType.ON_NPC_TELEPORT, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Provides instant callback operation when teleport {@link Npc}.
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setNpcTeleportId(Consumer<OnNpcTeleport> callback, int... npcIds)
	{
		return registerConsumer(callback, EventType.ON_NPC_TELEPORT, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Provides instant callback operation when {@link Player} talk to {@link Npc} and must receive quest state.
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setNpcQuestStartId(int... npcIds)
	{
		for (int id : npcIds)
		{
			if (NpcData.getInstance().getTemplate(id) == null)
			{
				LOGGER.severe(super.getClass().getSimpleName() + ": Found addStartNpc for non existing NPC: " + id + "!");
			}
		}
		
		return registerDummy(EventType.ON_NPC_QUEST_START, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Provides instant callback operation when {@link Player} talk to {@link Npc} and must receive quest state.
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setNpcQuestStartId(Collection<Integer> npcIds)
	{
		for (int id : npcIds)
		{
			if (NpcData.getInstance().getTemplate(id) == null)
			{
				LOGGER.severe(super.getClass().getSimpleName() + ": Found addStartNpc for non existing NPC: " + id + "!");
			}
		}
		
		return registerDummy(EventType.ON_NPC_QUEST_START, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Provides instant callback operation when Npc sees skill from a player.
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setNpcSkillSeeId(Consumer<OnNpcSkillSee> callback, int... npcIds)
	{
		for (int id : npcIds)
		{
			if (NpcData.getInstance().getTemplate(id) == null)
			{
				LOGGER.severe(super.getClass().getSimpleName() + ": Found addSkillSeeId for non existing NPC: " + id + "!");
			}
		}
		
		return registerConsumer(callback, EventType.ON_NPC_SKILL_SEE, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Provides instant callback operation when Npc sees skill from a player.
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setNpcSkillSeeId(Consumer<OnNpcSkillSee> callback, Collection<Integer> npcIds)
	{
		for (int id : npcIds)
		{
			if (NpcData.getInstance().getTemplate(id) == null)
			{
				LOGGER.severe(super.getClass().getSimpleName() + ": Found addSkillSeeId for non existing NPC: " + id + "!");
			}
		}
		
		return registerConsumer(callback, EventType.ON_NPC_SKILL_SEE, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Provides instant callback operation when Npc casts skill on a player.
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setNpcSkillFinishedId(Consumer<OnNpcSkillFinished> callback, int... npcIds)
	{
		for (int id : npcIds)
		{
			if (NpcData.getInstance().getTemplate(id) == null)
			{
				LOGGER.severe(super.getClass().getSimpleName() + ": Found addSpellFinishedId for non existing NPC: " + id + "!");
			}
		}
		
		return registerConsumer(callback, EventType.ON_NPC_SKILL_FINISHED, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Provides instant callback operation when Npc casts skill on a player.
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setNpcSkillFinishedId(Consumer<OnNpcSkillFinished> callback, Collection<Integer> npcIds)
	{
		for (int id : npcIds)
		{
			if (NpcData.getInstance().getTemplate(id) == null)
			{
				LOGGER.severe(super.getClass().getSimpleName() + ": Found addSpellFinishedId for non existing NPC: " + id + "!");
			}
		}
		
		return registerConsumer(callback, EventType.ON_NPC_SKILL_FINISHED, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Provides instant callback operation when Npc is spawned.
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setNpcSpawnId(Consumer<OnNpcSpawn> callback, int... npcIds)
	{
		for (int id : npcIds)
		{
			if (NpcData.getInstance().getTemplate(id) == null)
			{
				LOGGER.severe(super.getClass().getSimpleName() + ": Found addSpawnId for non existing NPC: " + id + "!");
			}
		}
		
		return registerConsumer(callback, EventType.ON_NPC_SPAWN, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Provides instant callback operation when Npc is spawned.
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setNpcSpawnId(Consumer<OnNpcSpawn> callback, Collection<Integer> npcIds)
	{
		for (int id : npcIds)
		{
			if (NpcData.getInstance().getTemplate(id) == null)
			{
				LOGGER.severe(super.getClass().getSimpleName() + ": Found addSpawnId for non existing NPC: " + id + "!");
			}
		}
		
		return registerConsumer(callback, EventType.ON_NPC_SPAWN, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Provides instant callback operation when {@link Npc} receives event from another {@link Npc}
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setNpcEventReceivedId(Consumer<OnNpcEventReceived> callback, int... npcIds)
	{
		return registerConsumer(callback, EventType.ON_NPC_EVENT_RECEIVED, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Provides instant callback operation when {@link Npc} receives event from another {@link Npc}
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setNpcEventReceivedId(Consumer<OnNpcEventReceived> callback, Collection<Integer> npcIds)
	{
		return registerConsumer(callback, EventType.ON_NPC_EVENT_RECEIVED, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Provides instant callback operation when {@link Npc} finishes to move.
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setNpcMoveFinishedId(Consumer<OnNpcMoveFinished> callback, int... npcIds)
	{
		return registerConsumer(callback, EventType.ON_NPC_MOVE_FINISHED, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Provides instant callback operation when {@link Npc} finishes to move.
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setNpcMoveFinishedId(Consumer<OnNpcMoveFinished> callback, Collection<Integer> npcIds)
	{
		return registerConsumer(callback, EventType.ON_NPC_MOVE_FINISHED, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Provides instant callback operation when {@link Npc} arrive to node of its route
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setNpcMoveNodeArrivedId(Consumer<OnNpcMoveNodeArrived> callback, int... npcIds)
	{
		return registerConsumer(callback, EventType.ON_NPC_MOVE_NODE_ARRIVED, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Provides instant callback operation when {@link Npc} arrive to node of its route
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setNpcMoveNodeArrivedId(Consumer<OnNpcMoveNodeArrived> callback, Collection<Integer> npcIds)
	{
		return registerConsumer(callback, EventType.ON_NPC_MOVE_NODE_ARRIVED, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Provides instant callback operation when {@link Npc} finishes to move on its route.
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setNpcMoveRouteFinishedId(Consumer<OnNpcMoveRouteFinished> callback, int... npcIds)
	{
		return registerConsumer(callback, EventType.ON_NPC_MOVE_ROUTE_FINISHED, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Provides instant callback operation when {@link Npc} finishes to move on its route.
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setNpcMoveRouteFinishedId(Consumer<OnNpcMoveRouteFinished> callback, Collection<Integer> npcIds)
	{
		return registerConsumer(callback, EventType.ON_NPC_MOVE_ROUTE_FINISHED, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Provides instant callback operation when {@link Npc} is about to hate and start attacking a creature.
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setNpcHateId(Consumer<OnAttackableHate> callback, int... npcIds)
	{
		return registerConsumer(callback, EventType.ON_NPC_HATE, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Provides instant callback operation when {@link Npc} is about to hate and start attacking a creature.
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setNpcHateId(Consumer<OnAttackableHate> callback, Collection<Integer> npcIds)
	{
		return registerConsumer(callback, EventType.ON_NPC_HATE, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Provides instant callback operation when {@link Npc} is about to hate and start attacking a creature.
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> addNpcHateId(Function<OnAttackableHate, TerminateReturn> callback, int... npcIds)
	{
		return registerFunction(callback, EventType.ON_NPC_HATE, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Provides instant callback operation when {@link Npc} is about to hate and start attacking a creature.
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> addNpcHateId(Function<OnAttackableHate, TerminateReturn> callback, Collection<Integer> npcIds)
	{
		return registerFunction(callback, EventType.ON_NPC_HATE, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Provides instant callback operation when {@link Npc} is about to hate and start attacking a creature.
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setNpcCanBeSeenId(Consumer<OnNpcCanBeSeen> callback, int... npcIds)
	{
		return registerConsumer(callback, EventType.ON_NPC_CAN_BE_SEEN, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Provides instant callback operation when {@link Npc} is about to hate and start attacking a creature.
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setNpcCanBeSeenId(Consumer<OnNpcCanBeSeen> callback, Collection<Integer> npcIds)
	{
		return registerConsumer(callback, EventType.ON_NPC_CAN_BE_SEEN, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Provides instant callback operation when {@link Npc} is about to hate and start attacking a creature.
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setNpcCanBeSeenId(Function<OnNpcCanBeSeen, TerminateReturn> callback, int... npcIds)
	{
		return registerFunction(callback, EventType.ON_NPC_CAN_BE_SEEN, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Provides instant callback operation when {@link Npc} is about to hate and start attacking a creature.
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setNpcCanBeSeenId(Function<OnNpcCanBeSeen, TerminateReturn> callback, Collection<Integer> npcIds)
	{
		return registerFunction(callback, EventType.ON_NPC_CAN_BE_SEEN, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Provides instant callback operation when {@link Creature} sees another creature.
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setCreatureSeeId(Consumer<OnCreatureSee> callback, int... npcIds)
	{
		for (int id : npcIds)
		{
			Npc.addCreatureSeeId(id);
		}
		
		return registerConsumer(callback, EventType.ON_CREATURE_SEE, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Provides instant callback operation when {@link Creature} sees another creature.
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setCreatureSeeId(Consumer<OnCreatureSee> callback, Collection<Integer> npcIds)
	{
		for (int id : npcIds)
		{
			Npc.addCreatureSeeId(id);
		}
		
		return registerConsumer(callback, EventType.ON_CREATURE_SEE, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Provides instant callback operation when Attackable is under attack to other clan mates.
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setAttackableFactionIdId(Consumer<OnAttackableFactionCall> callback, int... npcIds)
	{
		return registerConsumer(callback, EventType.ON_ATTACKABLE_FACTION_CALL, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Provides instant callback operation when Attackable is under attack to other clan mates.
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setAttackableFactionIdId(Consumer<OnAttackableFactionCall> callback, Collection<Integer> npcIds)
	{
		return registerConsumer(callback, EventType.ON_ATTACKABLE_FACTION_CALL, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Provides instant callback operation when Attackable is attacked from a player.
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setAttackableAttackId(Consumer<OnAttackableAttack> callback, int... npcIds)
	{
		for (int id : npcIds)
		{
			if (NpcData.getInstance().getTemplate(id) == null)
			{
				LOGGER.severe(super.getClass().getSimpleName() + ": Found addAttackId for non existing NPC: " + id + "!");
			}
		}
		
		return registerConsumer(callback, EventType.ON_ATTACKABLE_ATTACK, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Provides instant callback operation when Attackable is attacked from a player.
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setAttackableAttackId(Consumer<OnAttackableAttack> callback, Collection<Integer> npcIds)
	{
		for (int id : npcIds)
		{
			if (NpcData.getInstance().getTemplate(id) == null)
			{
				LOGGER.severe(super.getClass().getSimpleName() + ": Found addAttackId for non existing NPC: " + id + "!");
			}
		}
		
		return registerConsumer(callback, EventType.ON_ATTACKABLE_ATTACK, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Provides instant callback operation when {@link Player} enters in {@link Attackable}'s aggressive range.
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setAttackableAggroRangeEnterId(Consumer<OnAttackableAggroRangeEnter> callback, int... npcIds)
	{
		return registerConsumer(callback, EventType.ON_ATTACKABLE_AGGRO_RANGE_ENTER, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Provides instant callback operation when {@link Player} enters in {@link Attackable}'s aggressive range.
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setAttackableAggroRangeEnterId(Consumer<OnAttackableAggroRangeEnter> callback, Collection<Integer> npcIds)
	{
		return registerConsumer(callback, EventType.ON_ATTACKABLE_AGGRO_RANGE_ENTER, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Provides instant callback operation when {@link Player} learn's a {@link Skill}.
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setPlayerSkillLearnId(Consumer<OnPlayerSkillLearn> callback, int... npcIds)
	{
		return registerConsumer(callback, EventType.ON_PLAYER_SKILL_LEARN, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Provides instant callback operation when {@link Player} learn's a {@link Skill}.
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setPlayerSkillLearnId(Consumer<OnPlayerSkillLearn> callback, Collection<Integer> npcIds)
	{
		return registerConsumer(callback, EventType.ON_PLAYER_SKILL_LEARN, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Provides instant callback operation when {@link Player} summons a servitor or a pet
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setPlayerSummonSpawnId(Consumer<OnPlayerSummonSpawn> callback, int... npcIds)
	{
		return registerConsumer(callback, EventType.ON_PLAYER_SUMMON_SPAWN, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Provides instant callback operation when {@link Player} summons a servitor or a pet
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setPlayerSummonSpawnId(Consumer<OnPlayerSummonSpawn> callback, Collection<Integer> npcIds)
	{
		return registerConsumer(callback, EventType.ON_PLAYER_SUMMON_SPAWN, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Provides instant callback operation when {@link Player} talk with a servitor or a pet
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setPlayerSummonTalkId(Consumer<OnPlayerSummonTalk> callback, int... npcIds)
	{
		return registerConsumer(callback, EventType.ON_PLAYER_SUMMON_TALK, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Provides instant callback operation when {@link Player} talk with a servitor or a pet
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setPlayerSummonTalkId(Consumer<OnPlayerSummonSpawn> callback, Collection<Integer> npcIds)
	{
		return registerConsumer(callback, EventType.ON_PLAYER_SUMMON_TALK, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Provides instant callback operation when {@link Player} summons a servitor or a pet
	 * @param callback
	 * @return
	 */
	protected final List<AbstractEventListener> setPlayerLoginId(Consumer<OnPlayerLogin> callback)
	{
		return registerConsumer(callback, EventType.ON_PLAYER_LOGIN, ListenerRegisterType.GLOBAL);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Provides instant callback operation when {@link Player} summons a servitor or a pet
	 * @param callback
	 * @return
	 */
	protected final List<AbstractEventListener> setPlayerLogoutId(Consumer<OnPlayerLogout> callback)
	{
		return registerConsumer(callback, EventType.ON_PLAYER_LOGOUT, ListenerRegisterType.GLOBAL);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Provides instant callback operation when {@link org.l2jmobius.gameserver.model.actor.Creature} Enters on a {@link ZoneType}.
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setCreatureZoneEnterId(Consumer<OnCreatureZoneEnter> callback, int... npcIds)
	{
		return registerConsumer(callback, EventType.ON_CREATURE_ZONE_ENTER, ListenerRegisterType.ZONE, npcIds);
	}
	
	/**
	 * Provides instant callback operation when {@link org.l2jmobius.gameserver.model.actor.Creature} Enters on a {@link ZoneType}.
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setCreatureZoneEnterId(Consumer<OnCreatureZoneEnter> callback, Collection<Integer> npcIds)
	{
		return registerConsumer(callback, EventType.ON_CREATURE_ZONE_ENTER, ListenerRegisterType.ZONE, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Provides instant callback operation when {@link org.l2jmobius.gameserver.model.actor.Creature} Exits on a {@link ZoneType}.
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setCreatureZoneExitId(Consumer<OnCreatureZoneExit> callback, int... npcIds)
	{
		return registerConsumer(callback, EventType.ON_CREATURE_ZONE_EXIT, ListenerRegisterType.ZONE, npcIds);
	}
	
	/**
	 * Provides instant callback operation when {@link org.l2jmobius.gameserver.model.actor.Creature} Exits on a {@link ZoneType}.
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setCreatureZoneExitId(Consumer<OnCreatureZoneExit> callback, Collection<Integer> npcIds)
	{
		return registerConsumer(callback, EventType.ON_CREATURE_ZONE_EXIT, ListenerRegisterType.ZONE, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Provides instant callback operation when {@link org.l2jmobius.gameserver.model.actor.instance.Trap} acts.
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setTrapActionId(Consumer<OnTrapAction> callback, int... npcIds)
	{
		return registerConsumer(callback, EventType.ON_TRAP_ACTION, ListenerRegisterType.NPC, npcIds);
	}
	
	/**
	 * Provides instant callback operation when {@link org.l2jmobius.gameserver.model.actor.instance.Trap} acts.
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setTrapActionId(Consumer<OnTrapAction> callback, Collection<Integer> npcIds)
	{
		return registerConsumer(callback, EventType.ON_TRAP_ACTION, ListenerRegisterType.NPC, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Provides instant callback operation when {@link ItemTemplate} receives an event from {@link Player}.
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setItemBypassEvenId(Consumer<OnItemBypassEvent> callback, int... npcIds)
	{
		return registerConsumer(callback, EventType.ON_ITEM_BYPASS_EVENT, ListenerRegisterType.ITEM, npcIds);
	}
	
	/**
	 * Provides instant callback operation when {@link ItemTemplate} receives an event from {@link Player}.
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setItemBypassEvenId(Consumer<OnItemBypassEvent> callback, Collection<Integer> npcIds)
	{
		return registerConsumer(callback, EventType.ON_ITEM_BYPASS_EVENT, ListenerRegisterType.ITEM, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Provides instant callback operation when {@link Player} talk to {@link ItemTemplate}.
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setItemTalkId(Consumer<OnItemTalk> callback, int... npcIds)
	{
		return registerConsumer(callback, EventType.ON_ITEM_TALK, ListenerRegisterType.ITEM, npcIds);
	}
	
	/**
	 * Provides instant callback operation when {@link Player} talk to {@link ItemTemplate}.
	 * @param callback
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> setItemTalkId(Consumer<OnItemTalk> callback, Collection<Integer> npcIds)
	{
		return registerConsumer(callback, EventType.ON_ITEM_TALK, ListenerRegisterType.ITEM, npcIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Provides instant callback operation when Olympiad match finishes.
	 * @param callback
	 * @return
	 */
	protected final List<AbstractEventListener> setOlympiadMatchResult(Consumer<OnOlympiadMatchResult> callback)
	{
		return registerConsumer(callback, EventType.ON_OLYMPIAD_MATCH_RESULT, ListenerRegisterType.OLYMPIAD);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Provides instant callback operation when castle siege begins
	 * @param callback
	 * @param castleIds
	 * @return
	 */
	protected final List<AbstractEventListener> setCastleSiegeStartId(Consumer<OnCastleSiegeStart> callback, int... castleIds)
	{
		return registerConsumer(callback, EventType.ON_CASTLE_SIEGE_START, ListenerRegisterType.CASTLE, castleIds);
	}
	
	/**
	 * Provides instant callback operation when castle siege begins
	 * @param callback
	 * @param castleIds
	 * @return
	 */
	protected final List<AbstractEventListener> setCastleSiegeStartId(Consumer<OnCastleSiegeStart> callback, Collection<Integer> castleIds)
	{
		return registerConsumer(callback, EventType.ON_CASTLE_SIEGE_START, ListenerRegisterType.CASTLE, castleIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Provides instant callback operation when Castle owner has changed during a siege
	 * @param callback
	 * @param castleIds
	 * @return
	 */
	protected final List<AbstractEventListener> setCastleSiegeOwnerChangeId(Consumer<OnCastleSiegeOwnerChange> callback, int... castleIds)
	{
		return registerConsumer(callback, EventType.ON_CASTLE_SIEGE_OWNER_CHANGE, ListenerRegisterType.CASTLE, castleIds);
	}
	
	/**
	 * Provides instant callback operation when Castle owner has changed during a siege
	 * @param callback
	 * @param castleIds
	 * @return
	 */
	protected final List<AbstractEventListener> setCastleSiegeOwnerChangeId(Consumer<OnCastleSiegeOwnerChange> callback, Collection<Integer> castleIds)
	{
		return registerConsumer(callback, EventType.ON_CASTLE_SIEGE_OWNER_CHANGE, ListenerRegisterType.CASTLE, castleIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Provides instant callback operation when castle siege ends
	 * @param callback
	 * @param castleIds
	 * @return
	 */
	protected final List<AbstractEventListener> setCastleSiegeFinishId(Consumer<OnCastleSiegeFinish> callback, int... castleIds)
	{
		return registerConsumer(callback, EventType.ON_CASTLE_SIEGE_FINISH, ListenerRegisterType.CASTLE, castleIds);
	}
	
	/**
	 * Provides instant callback operation when castle siege ends
	 * @param callback
	 * @param castleIds
	 * @return
	 */
	protected final List<AbstractEventListener> setCastleSiegeFinishId(Consumer<OnCastleSiegeFinish> callback, Collection<Integer> castleIds)
	{
		return registerConsumer(callback, EventType.ON_CASTLE_SIEGE_FINISH, ListenerRegisterType.CASTLE, castleIds);
	}
	
	// ---------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Provides instant callback operation when player's profession has change
	 * @param callback
	 * @return
	 */
	protected final List<AbstractEventListener> setPlayerProfessionChangeId(Consumer<OnPlayerProfessionChange> callback)
	{
		return registerConsumer(callback, EventType.ON_PLAYER_PROFESSION_CHANGE, ListenerRegisterType.GLOBAL);
	}
	
	/**
	 * Provides instant callback operation when player's cancel profession
	 * @param callback
	 * @return
	 */
	protected final List<AbstractEventListener> setPlayerProfessionCancelId(Consumer<OnPlayerProfessionCancel> callback)
	{
		return registerConsumer(callback, EventType.ON_PLAYER_PROFESSION_CANCEL, ListenerRegisterType.GLOBAL);
	}
	
	// --------------------------------------------------------------------------------------------------
	// --------------------------------Default listener register methods---------------------------------
	// --------------------------------------------------------------------------------------------------
	
	/**
	 * Method that registers Function type of listeners (Listeners that need parameters but doesn't return objects)
	 * @param callback
	 * @param type
	 * @param registerType
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> registerConsumer(Consumer<? extends IBaseEvent> callback, EventType type, ListenerRegisterType registerType, int... npcIds)
	{
		return registerListener(container -> new ConsumerEventListener(container, type, callback, this), registerType, npcIds);
	}
	
	/**
	 * Method that registers Function type of listeners (Listeners that need parameters but doesn't return objects)
	 * @param callback
	 * @param type
	 * @param registerType
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> registerConsumer(Consumer<? extends IBaseEvent> callback, EventType type, ListenerRegisterType registerType, Collection<Integer> npcIds)
	{
		return registerListener(container -> new ConsumerEventListener(container, type, callback, this), registerType, npcIds);
	}
	
	/**
	 * Method that registers Function type of listeners (Listeners that need parameters and return objects)
	 * @param callback
	 * @param type
	 * @param registerType
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> registerFunction(Function<? extends IBaseEvent, ? extends AbstractEventReturn> callback, EventType type, ListenerRegisterType registerType, int... npcIds)
	{
		return registerListener(container -> new FunctionEventListener(container, type, callback, this), registerType, npcIds);
	}
	
	/**
	 * Method that registers Function type of listeners (Listeners that need parameters and return objects)
	 * @param callback
	 * @param type
	 * @param registerType
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> registerFunction(Function<? extends IBaseEvent, ? extends AbstractEventReturn> callback, EventType type, ListenerRegisterType registerType, Collection<Integer> npcIds)
	{
		return registerListener(container -> new FunctionEventListener(container, type, callback, this), registerType, npcIds);
	}
	
	/**
	 * Method that registers runnable type of listeners (Listeners that doesn't needs parameters or return objects)
	 * @param callback
	 * @param type
	 * @param registerType
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> registerRunnable(Runnable callback, EventType type, ListenerRegisterType registerType, int... npcIds)
	{
		return registerListener(container -> new RunnableEventListener(container, type, callback, this), registerType, npcIds);
	}
	
	/**
	 * Method that registers runnable type of listeners (Listeners that doesn't needs parameters or return objects)
	 * @param callback
	 * @param type
	 * @param registerType
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> registerRunnable(Runnable callback, EventType type, ListenerRegisterType registerType, Collection<Integer> npcIds)
	{
		return registerListener(container -> new RunnableEventListener(container, type, callback, this), registerType, npcIds);
	}
	
	/**
	 * Method that registers runnable type of listeners (Listeners that doesn't needs parameters or return objects)
	 * @param callback
	 * @param type
	 * @param registerType
	 * @param priority
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> registerAnnotation(Method callback, EventType type, ListenerRegisterType registerType, int priority, int... npcIds)
	{
		return registerListener(container -> new AnnotationEventListener(container, type, callback, this, priority), registerType, npcIds);
	}
	
	/**
	 * Method that registers runnable type of listeners (Listeners that doesn't needs parameters or return objects)
	 * @param callback
	 * @param type
	 * @param registerType
	 * @param priority
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> registerAnnotation(Method callback, EventType type, ListenerRegisterType registerType, int priority, Collection<Integer> npcIds)
	{
		return registerListener(container -> new AnnotationEventListener(container, type, callback, this, priority), registerType, npcIds);
	}
	
	/**
	 * Method that registers dummy type of listeners (Listeners doesn't gets notification but just used to check if their type present or not)
	 * @param type
	 * @param registerType
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> registerDummy(EventType type, ListenerRegisterType registerType, int... npcIds)
	{
		return registerListener(container -> new DummyEventListener(container, type, this), registerType, npcIds);
	}
	
	/**
	 * Method that registers dummy type of listeners (Listeners doesn't gets notification but just used to check if their type present or not)
	 * @param type
	 * @param registerType
	 * @param npcIds
	 * @return
	 */
	protected final List<AbstractEventListener> registerDummy(EventType type, ListenerRegisterType registerType, Collection<Integer> npcIds)
	{
		return registerListener(container -> new DummyEventListener(container, type, this), registerType, npcIds);
	}
	
	// --------------------------------------------------------------------------------------------------
	// --------------------------------------Register methods--------------------------------------------
	// --------------------------------------------------------------------------------------------------
	
	/**
	 * Generic listener register method
	 * @param action
	 * @param registerType
	 * @param ids
	 * @return
	 */
	protected final List<AbstractEventListener> registerListener(Function<ListenersContainer, AbstractEventListener> action, ListenerRegisterType registerType, int... ids)
	{
		final List<AbstractEventListener> listeners = new ArrayList<>(ids.length > 0 ? ids.length : 1);
		if (ids.length > 0)
		{
			for (int id : ids)
			{
				switch (registerType)
				{
					case NPC:
					{
						final NpcTemplate template = NpcData.getInstance().getTemplate(id);
						if (template != null)
						{
							listeners.add(template.addListener(action.apply(template)));
						}
						break;
					}
					case ZONE:
					{
						final ZoneType template = ZoneManager.getInstance().getZoneById(id);
						if (template != null)
						{
							listeners.add(template.addListener(action.apply(template)));
						}
						break;
					}
					case ITEM:
					{
						final ItemTemplate template = ItemData.getInstance().getTemplate(id);
						if (template != null)
						{
							listeners.add(template.addListener(action.apply(template)));
						}
						break;
					}
					case CASTLE:
					{
						final Castle template = CastleManager.getInstance().getCastleById(id);
						if (template != null)
						{
							listeners.add(template.addListener(action.apply(template)));
						}
						break;
					}
					default:
					{
						LOGGER.warning(getClass().getSimpleName() + ": Unhandled register type: " + registerType);
					}
				}
				
				_registeredIds.computeIfAbsent(registerType, _ -> ConcurrentHashMap.newKeySet()).add(id);
			}
		}
		else
		{
			switch (registerType)
			{
				case OLYMPIAD:
				{
					final Olympiad template = Olympiad.getInstance();
					listeners.add(template.addListener(action.apply(template)));
					break;
				}
				case GLOBAL: // Global Listener
				{
					final ListenersContainer template = Containers.Global();
					listeners.add(template.addListener(action.apply(template)));
					break;
				}
				case GLOBAL_NPCS: // Global Npcs Listener
				{
					final ListenersContainer template = Containers.Npcs();
					listeners.add(template.addListener(action.apply(template)));
					break;
				}
				case GLOBAL_MONSTERS: // Global Monsters Listener
				{
					final ListenersContainer template = Containers.Monsters();
					listeners.add(template.addListener(action.apply(template)));
					break;
				}
				case GLOBAL_PLAYERS: // Global Players Listener
				{
					final ListenersContainer template = Containers.Players();
					listeners.add(template.addListener(action.apply(template)));
					break;
				}
			}
		}
		
		_listeners.addAll(listeners);
		return listeners;
	}
	
	/**
	 * Generic listener register method
	 * @param action
	 * @param registerType
	 * @param ids
	 * @return
	 */
	protected final List<AbstractEventListener> registerListener(Function<ListenersContainer, AbstractEventListener> action, ListenerRegisterType registerType, Collection<Integer> ids)
	{
		final List<AbstractEventListener> listeners = new ArrayList<>(!ids.isEmpty() ? ids.size() : 1);
		if (!ids.isEmpty())
		{
			for (int id : ids)
			{
				switch (registerType)
				{
					case NPC:
					{
						final NpcTemplate template = NpcData.getInstance().getTemplate(id);
						if (template != null)
						{
							listeners.add(template.addListener(action.apply(template)));
						}
						break;
					}
					case ZONE:
					{
						final ZoneType template = ZoneManager.getInstance().getZoneById(id);
						if (template != null)
						{
							listeners.add(template.addListener(action.apply(template)));
						}
						break;
					}
					case ITEM:
					{
						final ItemTemplate template = ItemData.getInstance().getTemplate(id);
						if (template != null)
						{
							listeners.add(template.addListener(action.apply(template)));
						}
						break;
					}
					case CASTLE:
					{
						final Castle template = CastleManager.getInstance().getCastleById(id);
						if (template != null)
						{
							listeners.add(template.addListener(action.apply(template)));
						}
						break;
					}
					default:
					{
						LOGGER.warning(getClass().getSimpleName() + ": Unhandled register type: " + registerType);
					}
				}
			}
			
			_registeredIds.computeIfAbsent(registerType, _ -> ConcurrentHashMap.newKeySet()).addAll(ids);
		}
		else
		{
			switch (registerType)
			{
				case OLYMPIAD:
				{
					final Olympiad template = Olympiad.getInstance();
					listeners.add(template.addListener(action.apply(template)));
					break;
				}
				case GLOBAL: // Global Listener
				{
					final ListenersContainer template = Containers.Global();
					listeners.add(template.addListener(action.apply(template)));
					break;
				}
				case GLOBAL_NPCS: // Global Npcs Listener
				{
					final ListenersContainer template = Containers.Npcs();
					listeners.add(template.addListener(action.apply(template)));
					break;
				}
				case GLOBAL_MONSTERS: // Global Monsters Listener
				{
					final ListenersContainer template = Containers.Monsters();
					listeners.add(template.addListener(action.apply(template)));
					break;
				}
				case GLOBAL_PLAYERS: // Global Players Listener
				{
					final ListenersContainer template = Containers.Players();
					listeners.add(template.addListener(action.apply(template)));
					break;
				}
			}
		}
		
		_listeners.addAll(listeners);
		return listeners;
	}
	
	public Set<Integer> getRegisteredIds(ListenerRegisterType type)
	{
		return _registeredIds.getOrDefault(type, Collections.emptySet());
	}
	
	public Queue<AbstractEventListener> getListeners()
	{
		return _listeners;
	}
	
	/**
	 * -------------------------------------------------------------------------------------------------------
	 */
	
	/**
	 * Show an on screen message to the player.
	 * @param player the player to display the message to
	 * @param text the message to display
	 * @param time the duration of the message in milliseconds
	 */
	public static void showOnScreenMsg(Player player, String text, int time)
	{
		player.sendPacket(new ExShowScreenMessage(text, time));
	}
	
	/**
	 * Show an on screen message to the player.
	 * @param player the player to display the message to
	 * @param text the message to display
	 * @param position the position on the screen
	 * @param time the duration of the message in milliseconds
	 */
	public static void showOnScreenMsg(Player player, String text, int position, int time)
	{
		player.sendPacket(new ExShowScreenMessage(text, position, time));
	}
	
	/**
	 * Show an on screen message to the player.
	 * @param player the player to display the message to
	 * @param systemMsg the system message to display
	 * @param position the position of the message on the screen
	 * @param time the duration of the message in milliseconds
	 * @param params values of parameters to replace in the system message (like S1, C1 etc.)
	 */
	public static void showOnScreenMsg(Player player, SystemMessageId systemMsg, int position, int time, String... params)
	{
		player.sendPacket(new ExShowScreenMessage(systemMsg, position, time, params));
	}
	
	/**
	 * Add a temporary spawn of the specified NPC.
	 * @param npcId the ID of the NPC to spawn
	 * @param pos the object containing the spawn location coordinates
	 * @return the {@link Npc} object of the newly spawned NPC or {@code null} if the NPC doesn't exist
	 * @see #addSpawn(int, IPositionable, boolean, long, boolean, int)
	 * @see #addSpawn(int, int, int, int, int, boolean, long, boolean, int)
	 */
	public static Npc addSpawn(int npcId, IPositionable pos)
	{
		return addSpawn(npcId, pos.getX(), pos.getY(), pos.getZ(), pos.getHeading(), false, 0, false, 0);
	}
	
	/**
	 * Add a temporary spawn of the specified NPC.
	 * @param npcId the ID of the NPC to spawn
	 * @param pos the object containing the spawn location coordinates
	 * @param instanceId the ID of the instance to spawn the NPC in (0 - the open world)
	 * @return the {@link Npc} object of the newly spawned NPC or {@code null} if the NPC doesn't exist
	 * @see #addSpawn(int, IPositionable, boolean, long, boolean, int)
	 * @see #addSpawn(int, int, int, int, int, boolean, long, boolean, int)
	 */
	public Npc addSpawn(int npcId, Location pos, int instanceId)
	{
		return addSpawn(npcId, pos, false, 0, false, instanceId);
	}
	
	/**
	 * Add a temporary spawn of the specified NPC.
	 * @param summoner the NPC that requires this spawn
	 * @param npcId the ID of the NPC to spawn
	 * @param pos the object containing the spawn location coordinates
	 * @param randomOffset if {@code true}, adds +/- 50~100 to X/Y coordinates of the spawn location
	 * @param despawnDelay time in milliseconds till the NPC is despawned (0 - only despawned on server shutdown)
	 * @return the {@link Npc} object of the newly spawned NPC, {@code null} if the NPC doesn't exist
	 */
	public static Npc addSpawn(Npc summoner, int npcId, IPositionable pos, boolean randomOffset, long despawnDelay)
	{
		return addSpawn(summoner, npcId, pos.getX(), pos.getY(), pos.getZ(), pos.getHeading(), randomOffset, despawnDelay, false, 0);
	}
	
	/**
	 * Add a temporary spawn of the specified NPC.
	 * @param npcId the ID of the NPC to spawn
	 * @param pos the object containing the spawn location coordinates
	 * @param isSummonSpawn if {@code true}, displays a summon animation on NPC spawn
	 * @return the {@link Npc} object of the newly spawned NPC or {@code null} if the NPC doesn't exist
	 * @see #addSpawn(int, IPositionable, boolean, long, boolean, int)
	 * @see #addSpawn(int, int, int, int, int, boolean, long, boolean, int)
	 */
	public static Npc addSpawn(int npcId, IPositionable pos, boolean isSummonSpawn)
	{
		return addSpawn(npcId, pos.getX(), pos.getY(), pos.getZ(), pos.getHeading(), false, 0, isSummonSpawn, 0);
	}
	
	/**
	 * Add a temporary spawn of the specified NPC.
	 * @param npcId the ID of the NPC to spawn
	 * @param pos the object containing the spawn location coordinates
	 * @param randomOffset if {@code true}, adds +/- 50~100 to X/Y coordinates of the spawn location
	 * @param despawnDelay time in milliseconds till the NPC is despawned (0 - only despawned on server shutdown)
	 * @return the {@link Npc} object of the newly spawned NPC or {@code null} if the NPC doesn't exist
	 * @see #addSpawn(int, IPositionable, boolean, long, boolean, int)
	 * @see #addSpawn(int, int, int, int, int, boolean, long, boolean, int)
	 */
	public static Npc addSpawn(int npcId, IPositionable pos, boolean randomOffset, long despawnDelay)
	{
		return addSpawn(npcId, pos.getX(), pos.getY(), pos.getZ(), pos.getHeading(), randomOffset, despawnDelay, false, 0);
	}
	
	/**
	 * Add a temporary spawn of the specified NPC.
	 * @param npcId the ID of the NPC to spawn
	 * @param pos the object containing the spawn location coordinates
	 * @param randomOffset if {@code true}, adds +/- 50~100 to X/Y coordinates of the spawn location
	 * @param despawnDelay time in milliseconds till the NPC is despawned (0 - only despawned on server shutdown)
	 * @param isSummonSpawn if {@code true}, displays a summon animation on NPC spawn
	 * @return the {@link Npc} object of the newly spawned NPC or {@code null} if the NPC doesn't exist
	 * @see #addSpawn(int, IPositionable, boolean, long, boolean, int)
	 * @see #addSpawn(int, int, int, int, int, boolean, long, boolean, int)
	 */
	public static Npc addSpawn(int npcId, IPositionable pos, boolean randomOffset, long despawnDelay, boolean isSummonSpawn)
	{
		return addSpawn(npcId, pos.getX(), pos.getY(), pos.getZ(), pos.getHeading(), randomOffset, despawnDelay, isSummonSpawn, 0);
	}
	
	/**
	 * Add a temporary spawn of the specified NPC.
	 * @param npcId the ID of the NPC to spawn
	 * @param pos the object containing the spawn location coordinates
	 * @param randomOffset if {@code true}, adds +/- 50~100 to X/Y coordinates of the spawn location
	 * @param despawnDelay time in milliseconds till the NPC is despawned (0 - only despawned on server shutdown)
	 * @param isSummonSpawn if {@code true}, displays a summon animation on NPC spawn
	 * @param instanceId the ID of the instance to spawn the NPC in (0 - the open world)
	 * @return the {@link Npc} object of the newly spawned NPC or {@code null} if the NPC doesn't exist
	 * @see #addSpawn(int, IPositionable)
	 * @see #addSpawn(int, IPositionable, boolean)
	 * @see #addSpawn(int, IPositionable, boolean, long)
	 * @see #addSpawn(int, IPositionable, boolean, long, boolean)
	 * @see #addSpawn(int, int, int, int, int, boolean, long, boolean, int)
	 */
	public static Npc addSpawn(int npcId, IPositionable pos, boolean randomOffset, long despawnDelay, boolean isSummonSpawn, int instanceId)
	{
		return addSpawn(npcId, pos.getX(), pos.getY(), pos.getZ(), pos.getHeading(), randomOffset, despawnDelay, isSummonSpawn, instanceId);
	}
	
	/**
	 * Add a temporary spawn of the specified NPC.
	 * @param npcId the ID of the NPC to spawn
	 * @param x the X coordinate of the spawn location
	 * @param y the Y coordinate of the spawn location
	 * @param z the Z coordinate (height) of the spawn location
	 * @param heading the heading of the NPC
	 * @param randomOffset if {@code true}, adds +/- 50~100 to X/Y coordinates of the spawn location
	 * @param despawnDelay time in milliseconds till the NPC is despawned (0 - only despawned on server shutdown)
	 * @return the {@link Npc} object of the newly spawned NPC or {@code null} if the NPC doesn't exist
	 * @see #addSpawn(int, IPositionable, boolean, long, boolean, int)
	 * @see #addSpawn(int, int, int, int, int, boolean, long, boolean, int)
	 */
	public static Npc addSpawn(int npcId, int x, int y, int z, int heading, boolean randomOffset, long despawnDelay)
	{
		return addSpawn(npcId, x, y, z, heading, randomOffset, despawnDelay, false, 0);
	}
	
	/**
	 * Add a temporary spawn of the specified NPC.
	 * @param npcId the ID of the NPC to spawn
	 * @param x the X coordinate of the spawn location
	 * @param y the Y coordinate of the spawn location
	 * @param z the Z coordinate (height) of the spawn location
	 * @param heading the heading of the NPC
	 * @return the {@link Npc} object of the newly spawned NPC or {@code null} if the NPC doesn't exist
	 * @see #addSpawn(int, IPositionable, boolean, long, boolean, int)
	 * @see #addSpawn(int, int, int, int, int, boolean, long, boolean, int)
	 */
	public static Npc addSpawn(int npcId, int x, int y, int z, int heading)
	{
		return addSpawn(npcId, x, y, z, heading, false, 0, false, 0);
	}
	
	/**
	 * Add a temporary spawn of the specified NPC.
	 * @param npcId the ID of the NPC to spawn
	 * @param x the X coordinate of the spawn location
	 * @param y the Y coordinate of the spawn location
	 * @param z the Z coordinate (height) of the spawn location
	 * @param heading the heading of the NPC
	 * @param randomOffset if {@code true}, adds +/- 50~100 to X/Y coordinates of the spawn location
	 * @param despawnDelay time in milliseconds till the NPC is despawned (0 - only despawned on server shutdown)
	 * @param isSummonSpawn if {@code true}, displays a summon animation on NPC spawn
	 * @return the {@link Npc} object of the newly spawned NPC or {@code null} if the NPC doesn't exist
	 * @see #addSpawn(int, IPositionable, boolean, long, boolean, int)
	 * @see #addSpawn(int, int, int, int, int, boolean, long, boolean, int)
	 */
	public static Npc addSpawn(int npcId, int x, int y, int z, int heading, boolean randomOffset, long despawnDelay, boolean isSummonSpawn)
	{
		return addSpawn(npcId, x, y, z, heading, randomOffset, despawnDelay, isSummonSpawn, 0);
	}
	
	/**
	 * Add a temporary spawn of the specified NPC.
	 * @param npcId the ID of the NPC to spawn
	 * @param x the X coordinate of the spawn location
	 * @param y the Y coordinate of the spawn location
	 * @param z the Z coordinate (height) of the spawn location
	 * @param heading the heading of the NPC
	 * @param randomOffset if {@code true}, adds +/- 50~100 to X/Y coordinates of the spawn location
	 * @param despawnDelay time in milliseconds till the NPC is despawned (0 - only despawned on server shutdown)
	 * @param isSummonSpawn if {@code true}, displays a summon animation on NPC spawn
	 * @param instanceId the ID of the instance to spawn the NPC in (0 - the open world)
	 * @return the {@link Npc} object of the newly spawned NPC or {@code null} if the NPC doesn't exist
	 * @see #addSpawn(int, IPositionable, boolean, long, boolean, int)
	 * @see #addSpawn(int, int, int, int, int, boolean, long)
	 * @see #addSpawn(int, int, int, int, int, boolean, long, boolean)
	 */
	public static Npc addSpawn(int npcId, int x, int y, int z, int heading, boolean randomOffset, long despawnDelay, boolean isSummonSpawn, int instanceId)
	{
		return addSpawn(null, npcId, x, y, z, heading, randomOffset, despawnDelay, isSummonSpawn, instanceId);
	}
	
	/**
	 * Add a temporary spawn of the specified NPC.
	 * @param summoner the NPC that requires this spawn
	 * @param npcId the ID of the NPC to spawn
	 * @param xValue the X coordinate of the spawn location
	 * @param yValue the Y coordinate of the spawn location
	 * @param zValue the Z coordinate (height) of the spawn location
	 * @param heading the heading of the NPC
	 * @param randomOffset if {@code true}, adds +/- 50~100 to X/Y coordinates of the spawn location
	 * @param despawnDelay time in milliseconds till the NPC is despawned (0 - only despawned on server shutdown)
	 * @param isSummonSpawn if {@code true}, displays a summon animation on NPC spawn
	 * @param instanceId the ID of the instance to spawn the NPC in (0 - the open world)
	 * @return the {@link Npc} object of the newly spawned NPC or {@code null} if the NPC doesn't exist
	 * @see #addSpawn(int, IPositionable, boolean, long, boolean, int)
	 * @see #addSpawn(int, int, int, int, int, boolean, long)
	 * @see #addSpawn(int, int, int, int, int, boolean, long, boolean)
	 */
	public static Npc addSpawn(Npc summoner, int npcId, int xValue, int yValue, int zValue, int heading, boolean randomOffset, long despawnDelay, boolean isSummonSpawn, int instanceId)
	{
		try
		{
			if ((xValue == 0) && (yValue == 0))
			{
				LOGGER.severe("addSpawn(): invalid spawn coordinates for NPC #" + npcId + "!");
				return null;
			}
			
			int x = xValue;
			int y = yValue;
			if (randomOffset)
			{
				int offset = Rnd.get(50, 100);
				if (Rnd.nextBoolean())
				{
					offset *= -1;
				}
				
				x += offset;
				offset = Rnd.get(50, 100);
				if (Rnd.nextBoolean())
				{
					offset *= -1;
				}
				
				y += offset;
			}
			
			final Spawn spawn = new Spawn(npcId);
			spawn.setInstanceId(instanceId);
			spawn.setHeading(heading);
			spawn.setXYZ(x, y, zValue);
			spawn.stopRespawn();
			
			final Npc npc = spawn.doSpawn(isSummonSpawn);
			if (despawnDelay > 0)
			{
				npc.scheduleDespawn(despawnDelay);
			}
			
			if (summoner != null)
			{
				summoner.addSummonedNpc(npc);
			}
			
			// Retain monster original position if ENABLE_RANDOM_MONSTER_SPAWNS is enabled.
			if (RandomSpawnsConfig.ENABLE_RANDOM_MONSTER_SPAWNS && !randomOffset && npc.isMonster())
			{
				spawn.setXYZ(x, y, zValue);
				npc.setXYZ(x, y, zValue);
				if (heading > -1)
				{
					npc.setHeading(heading);
				}
			}
			
			// Fixes invisible NPCs spawned by script.
			npc.broadcastInfo();
			
			return npc;
		}
		catch (Exception e)
		{
			LOGGER.warning("Could not spawn NPC #" + npcId + "; error: " + e.getMessage());
		}
		
		return null;
	}
	
	/**
	 * @param trapId
	 * @param x
	 * @param y
	 * @param z
	 * @param heading
	 * @param instanceId
	 * @return
	 */
	public Trap addTrap(int trapId, int x, int y, int z, int heading, int instanceId)
	{
		final NpcTemplate npcTemplate = NpcData.getInstance().getTemplate(trapId);
		final Trap trap = new Trap(npcTemplate, instanceId, -1);
		trap.setCurrentHp(trap.getMaxHp());
		trap.setCurrentMp(trap.getMaxMp());
		trap.setInvul(true);
		trap.setHeading(heading);
		trap.spawnMe(x, y, z);
		return trap;
	}
	
	/**
	 * @param master
	 * @param minionId
	 * @return
	 */
	public Npc addMinion(Monster master, int minionId)
	{
		return MinionList.spawnMinion(master, minionId);
	}
	
	/**
	 * Get the amount of an item in player's inventory.
	 * @param player the player whose inventory to check
	 * @param itemId the ID of the item whose amount to get
	 * @return the amount of the specified item in player's inventory
	 */
	public static int getQuestItemsCount(Player player, int itemId)
	{
		return player.getInventory().getInventoryItemCount(itemId, -1);
	}
	
	/**
	 * Get the total amount of all specified items in player's inventory.
	 * @param player the player whose inventory to check
	 * @param itemIds a list of IDs of items whose amount to get
	 * @return the summary amount of all listed items in player's inventory
	 */
	public int getQuestItemsCount(Player player, int... itemIds)
	{
		int count = 0;
		for (Item item : player.getInventory().getItems())
		{
			if (item == null)
			{
				continue;
			}
			
			for (int itemId : itemIds)
			{
				if (item.getId() == itemId)
				{
					if ((count + item.getCount()) > Integer.MAX_VALUE)
					{
						return Integer.MAX_VALUE;
					}
					
					count += item.getCount();
				}
			}
		}
		
		return count;
	}
	
	/**
	 * Check if the player has the specified item in his inventory.
	 * @param player the player whose inventory to check for the specified item
	 * @param item the {@link ItemHolder} object containing the ID and count of the item to check
	 * @return {@code true} if the player has the required count of the item
	 */
	protected static boolean hasItem(Player player, ItemHolder item)
	{
		return hasItem(player, item, true);
	}
	
	/**
	 * Check if the player has the required count of the specified item in his inventory.
	 * @param player the player whose inventory to check for the specified item
	 * @param item the {@link ItemHolder} object containing the ID and count of the item to check
	 * @param checkCount if {@code true}, check if each item is at least of the count specified in the ItemHolder,<br>
	 *            otherwise check only if the player has the item at all
	 * @return {@code true} if the player has the item
	 */
	protected static boolean hasItem(Player player, ItemHolder item, boolean checkCount)
	{
		if (item == null)
		{
			return false;
		}
		
		if (checkCount)
		{
			return getQuestItemsCount(player, item.getId()) >= item.getCount();
		}
		
		return hasQuestItems(player, item.getId());
	}
	
	/**
	 * Check if the player has all the specified items in his inventory and, if necessary, if their count is also as required.
	 * @param player the player whose inventory to check for the specified item
	 * @param checkCount if {@code true}, check if each item is at least of the count specified in the ItemHolder,<br>
	 *            otherwise check only if the player has the item at all
	 * @param itemList a list of {@link ItemHolder} objects containing the IDs of the items to check
	 * @return {@code true} if the player has all the items from the list
	 */
	protected static boolean hasAllItems(Player player, boolean checkCount, ItemHolder... itemList)
	{
		if ((itemList == null) || (itemList.length == 0))
		{
			return false;
		}
		
		for (ItemHolder item : itemList)
		{
			if (!hasItem(player, item, checkCount))
			{
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Check for an item in player's inventory.
	 * @param player the player whose inventory to check for quest items
	 * @param itemId the ID of the item to check for
	 * @return {@code true} if the item exists in player's inventory, {@code false} otherwise
	 */
	public static boolean hasQuestItems(Player player, int itemId)
	{
		return player.getInventory().getItemByItemId(itemId) != null;
	}
	
	/**
	 * Check for multiple items in player's inventory.
	 * @param player the player whose inventory to check for quest items
	 * @param itemIds a list of item IDs to check for
	 * @return {@code true} if all items exist in player's inventory, {@code false} otherwise
	 */
	public static boolean hasQuestItems(Player player, int... itemIds)
	{
		if ((itemIds == null) || (itemIds.length == 0))
		{
			return false;
		}
		
		final PlayerInventory inv = player.getInventory();
		for (int itemId : itemIds)
		{
			if (inv.getItemByItemId(itemId) == null)
			{
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Check for multiple items in player's inventory.
	 * @param player the player whose inventory to check for quest items
	 * @param itemIds a list of item IDs to check for
	 * @return {@code true} if at least one items exist in player's inventory, {@code false} otherwise
	 */
	public boolean hasAtLeastOneQuestItem(Player player, int... itemIds)
	{
		final PlayerInventory inv = player.getInventory();
		for (int itemId : itemIds)
		{
			if (inv.getItemByItemId(itemId) != null)
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Extensive player ownership check for single or multiple items.<br>
	 * Checks inventory, warehouse and summon.
	 * @param player the player to check for quest items
	 * @param itemIds a list of item IDs to check for
	 * @return {@code true} if player owns at least one items, {@code false} otherwise.
	 */
	public boolean ownsAtLeastOneItem(Player player, int... itemIds)
	{
		// Inventory.
		final PlayerInventory inventory = player.getInventory();
		for (int itemId : itemIds)
		{
			if (inventory.getItemByItemId(itemId) != null)
			{
				return true;
			}
		}
		
		// Warehouse.
		final PlayerWarehouse warehouse = player.getWarehouse();
		for (int itemId : itemIds)
		{
			if (warehouse.getItemByItemId(itemId) != null)
			{
				return true;
			}
		}
		
		// Summon.
		if (player.hasSummon())
		{
			final PetInventory petInventory = player.getSummon().getInventory();
			if (petInventory != null)
			{
				for (int itemId : itemIds)
				{
					if (petInventory.getItemByItemId(itemId) != null)
					{
						return true;
					}
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Get the enchantment level of an item in player's inventory.
	 * @param player the player whose item to check
	 * @param itemId the ID of the item whose enchantment level to get
	 * @return the enchantment level of the item or 0 if the item was not found
	 */
	public static int getEnchantLevel(Player player, int itemId)
	{
		final Item enchantedItem = player.getInventory().getItemByItemId(itemId);
		if (enchantedItem == null)
		{
			return 0;
		}
		
		return enchantedItem.getEnchantLevel();
	}
	
	/**
	 * Give Adena to the player.
	 * @param player the player to whom to give the Adena
	 * @param count the amount of Adena to give
	 * @param applyRates if {@code true} quest rates will be applied to the amount
	 */
	public static void giveAdena(Player player, int count, boolean applyRates)
	{
		if (applyRates)
		{
			rewardItems(player, Inventory.ADENA_ID, count);
		}
		else
		{
			giveItems(player, Inventory.ADENA_ID, count);
		}
	}
	
	/**
	 * Give a reward to player using multipliers.
	 * @param player the player to whom to give the item
	 * @param holder
	 */
	public static void rewardItems(Player player, ItemHolder holder)
	{
		rewardItems(player, holder.getId(), holder.getCount());
	}
	
	/**
	 * Give a reward to player using multipliers.
	 * @param player the player to whom to give the item
	 * @param itemId the ID of the item to give
	 * @param countValue the amount of items to give
	 */
	public static void rewardItems(Player player, int itemId, int countValue)
	{
		if (countValue <= 0)
		{
			return;
		}
		
		final ItemTemplate item = ItemData.getInstance().getTemplate(itemId);
		if (item == null)
		{
			return;
		}
		
		int count = countValue;
		try
		{
			if (itemId == Inventory.ADENA_ID)
			{
				count *= RatesConfig.RATE_QUEST_REWARD_ADENA;
			}
			else if (RatesConfig.RATE_QUEST_REWARD_USE_MULTIPLIERS)
			{
				if (item instanceof EtcItem)
				{
					switch (((EtcItem) item).getItemType())
					{
						case POTION:
						{
							count *= RatesConfig.RATE_QUEST_REWARD_POTION;
							break;
						}
						case SCRL_ENCHANT_WP:
						case SCRL_ENCHANT_AM:
						case SCROLL:
						{
							count *= RatesConfig.RATE_QUEST_REWARD_SCROLL;
							break;
						}
						case RECIPE:
						{
							count *= RatesConfig.RATE_QUEST_REWARD_RECIPE;
							break;
						}
						case MATERIAL:
						{
							count *= RatesConfig.RATE_QUEST_REWARD_MATERIAL;
							break;
						}
						default:
						{
							count *= RatesConfig.RATE_QUEST_REWARD;
						}
					}
				}
			}
			else
			{
				count *= RatesConfig.RATE_QUEST_REWARD;
			}
		}
		catch (Exception e)
		{
			count = Integer.MAX_VALUE;
		}
		
		// Add items to player's inventory
		final Item itemInstance = player.getInventory().addItem(ItemProcessType.QUEST, itemId, count, player, player.getTarget());
		if (itemInstance == null)
		{
			return;
		}
		
		sendItemGetMessage(player, itemInstance, count);
	}
	
	/**
	 * Send the system message and the status update packets to the player.
	 * @param player the player that has got the item
	 * @param item the item obtain by the player
	 * @param count the item count
	 */
	private static void sendItemGetMessage(Player player, Item item, int count)
	{
		// If item for reward is gold, send message of gold reward to client
		if (item.getId() == Inventory.ADENA_ID)
		{
			final SystemMessage smsg = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S1_ADENA);
			smsg.addInt(count);
			player.sendPacket(smsg);
		}
		// Otherwise, send message of object reward to client
		else
		{
			if (count > 1)
			{
				final SystemMessage smsg = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S2_S1_S);
				smsg.addItemName(item);
				smsg.addInt(count);
				player.sendPacket(smsg);
			}
			else
			{
				final SystemMessage smsg = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S1);
				smsg.addItemName(item);
				player.sendPacket(smsg);
			}
		}
		
		// send packets
		final StatusUpdate su = new StatusUpdate(player);
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);
	}
	
	/**
	 * Give item/reward to the player
	 * @param player
	 * @param itemId
	 * @param count
	 */
	public static void giveItems(Player player, int itemId, int count)
	{
		giveItems(player, itemId, count, 0);
	}
	
	/**
	 * Give item/reward to the player
	 * @param player
	 * @param holder
	 */
	protected static void giveItems(Player player, ItemHolder holder)
	{
		giveItems(player, holder.getId(), holder.getCount());
	}
	
	/**
	 * @param player
	 * @param itemId
	 * @param count
	 * @param enchantlevel
	 */
	public static void giveItems(Player player, int itemId, int count, int enchantlevel)
	{
		if (count <= 0)
		{
			return;
		}
		
		final ItemTemplate template = ItemData.getInstance().getTemplate(itemId);
		if (template == null)
		{
			return;
		}
		
		// Apply quest item drop amount multiplier configuration.
		int finalCount = count;
		if (template.isQuestItem())
		{
			finalCount = (int) (count * RatesConfig.QUEST_ITEM_DROP_AMOUNT_MULTIPLIER);
		}
		
		// Add items to player's inventory.
		final Item item = player.getInventory().addItem(ItemProcessType.QUEST, itemId, finalCount, player, player.getTarget());
		if (item == null)
		{
			return;
		}
		
		// set enchant level for item if that item is not adena
		if ((enchantlevel > 0) && (itemId != Inventory.ADENA_ID))
		{
			item.setEnchantLevel(enchantlevel);
		}
		
		sendItemGetMessage(player, item, finalCount);
	}
	
	/**
	 * Give the specified player a set amount of items if he is lucky enough.<br>
	 * Not recommended to use this for non-stacking items.
	 * @param player the player to give the item(s) to
	 * @param itemId the ID of the item to give
	 * @param amountToGive the amount of items to give
	 * @param limit the maximum amount of items the player can have. Won't give more if this limit is reached. 0 - no limit.
	 * @param dropChance the drop chance as a decimal digit from 0 to 1
	 * @param playSound if true, plays ItemSound.quest_itemget when items are given and ItemSound.quest_middle when the limit is reached
	 * @return {@code true} if limit > 0 and the limit was reached or if limit <= 0 and items were given; {@code false} in all other cases
	 */
	public static boolean giveItemRandomly(Player player, int itemId, int amountToGive, int limit, double dropChance, boolean playSound)
	{
		return giveItemRandomly(player, null, itemId, amountToGive, amountToGive, limit, dropChance, playSound);
	}
	
	/**
	 * Give the specified player a set amount of items if he is lucky enough.<br>
	 * Not recommended to use this for non-stacking items.
	 * @param player the player to give the item(s) to
	 * @param npc the NPC that "dropped" the item (can be null)
	 * @param itemId the ID of the item to give
	 * @param amountToGive the amount of items to give
	 * @param limit the maximum amount of items the player can have. Won't give more if this limit is reached. 0 - no limit.
	 * @param dropChance the drop chance as a decimal digit from 0 to 1
	 * @param playSound if true, plays ItemSound.quest_itemget when items are given and ItemSound.quest_middle when the limit is reached
	 * @return {@code true} if limit > 0 and the limit was reached or if limit <= 0 and items were given; {@code false} in all other cases
	 */
	public static boolean giveItemRandomly(Player player, Npc npc, int itemId, int amountToGive, int limit, double dropChance, boolean playSound)
	{
		return giveItemRandomly(player, npc, itemId, amountToGive, amountToGive, limit, dropChance, playSound);
	}
	
	/**
	 * Give the specified player a random amount of items if he is lucky enough.<br>
	 * Not recommended to use this for non-stacking items.
	 * @param player the player to give the item(s) to
	 * @param npc the NPC that "dropped" the item (can be null)
	 * @param itemId the ID of the item to give
	 * @param minAmount the minimum amount of items to give
	 * @param maxAmount the maximum amount of items to give (will give a random amount between min/maxAmount multiplied by quest rates)
	 * @param limit the maximum amount of items the player can have. Won't give more if this limit is reached. 0 - no limit.
	 * @param dropChance the drop chance as a decimal digit from 0 to 1
	 * @param playSound if true, plays ItemSound.quest_itemget when items are given and ItemSound.quest_middle when the limit is reached
	 * @return {@code true} if limit > 0 and the limit was reached or if limit <= 0 and items were given; {@code false} in all other cases
	 */
	public static boolean giveItemRandomly(Player player, Npc npc, int itemId, int minAmount, int maxAmount, int limit, double dropChance, boolean playSound)
	{
		final int currentCount = getQuestItemsCount(player, itemId);
		if ((limit > 0) && (currentCount >= limit))
		{
			return true;
		}
		
		int minAmountWithBonus = (int) (minAmount * RatesConfig.QUEST_ITEM_DROP_AMOUNT_MULTIPLIER);
		int maxAmountWithBonus = (int) (maxAmount * RatesConfig.QUEST_ITEM_DROP_AMOUNT_MULTIPLIER);
		double dropChanceWithBonus = dropChance;
		if ((npc != null) && ChampionMonstersConfig.CHAMPION_ENABLE && npc.isChampion())
		{
			if ((itemId == Inventory.ADENA_ID) || (itemId == Inventory.ANCIENT_ADENA_ID))
			{
				dropChanceWithBonus *= ChampionMonstersConfig.CHAMPION_ADENAS_REWARDS_CHANCE;
				minAmountWithBonus *= ChampionMonstersConfig.CHAMPION_ADENAS_REWARDS_AMOUNT;
				maxAmountWithBonus *= ChampionMonstersConfig.CHAMPION_ADENAS_REWARDS_AMOUNT;
			}
			else
			{
				dropChanceWithBonus *= ChampionMonstersConfig.CHAMPION_REWARDS_CHANCE;
				minAmountWithBonus *= ChampionMonstersConfig.CHAMPION_REWARDS_AMOUNT;
				maxAmountWithBonus *= ChampionMonstersConfig.CHAMPION_REWARDS_AMOUNT;
			}
		}
		
		int amountToGive = (minAmountWithBonus == maxAmountWithBonus) ? minAmountWithBonus : Rnd.get(minAmountWithBonus, maxAmountWithBonus);
		final double random = Rnd.nextDouble();
		
		// Inventory slot check (almost useless for non-stacking items)
		if ((dropChanceWithBonus >= random) && (amountToGive > 0) && player.getInventory().validateCapacityByItemId(itemId))
		{
			if ((limit > 0) && ((currentCount + amountToGive) > limit))
			{
				amountToGive = limit - currentCount;
			}
			
			// Give the item to player
			if (player.addItem(ItemProcessType.QUEST, itemId, amountToGive, npc, true) != null)
			{
				// limit reached (if there is no limit, this block doesn't execute)
				if ((currentCount + amountToGive) == limit)
				{
					if (playSound)
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
					}
					
					return true;
				}
				
				if (playSound)
				{
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				
				// if there is no limit, return true every time an item is given
				if (limit <= 0)
				{
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Take an amount of a specified item from player's inventory.
	 * @param player the player whose item to take
	 * @param itemId the ID of the item to take
	 * @param amount the amount to take
	 * @return {@code true} if any items were taken, {@code false} otherwise
	 */
	public static boolean takeItems(Player player, int itemId, int amount)
	{
		final Collection<Item> items = player.getInventory().getAllItemsByItemId(itemId);
		if (amount < 0)
		{
			items.forEach(i -> takeItem(player, i, i.getCount()));
		}
		else
		{
			int currentCount = 0;
			for (Item i : items)
			{
				int toDelete = i.getCount();
				if ((currentCount + toDelete) > amount)
				{
					toDelete = amount - currentCount;
				}
				
				if (toDelete > 0)
				{
					takeItem(player, i, toDelete);
				}
				
				currentCount += toDelete;
			}
		}
		
		return true;
	}
	
	private static boolean takeItem(Player player, Item item, int toDelete)
	{
		if (item.isEquipped())
		{
			final InventoryUpdate iu = new InventoryUpdate();
			for (Item itm : player.getInventory().unEquipItemInBodySlotAndRecord(item.getTemplate().getBodyPart()))
			{
				iu.addModifiedItem(itm);
			}
			
			player.sendInventoryUpdate(iu);
			player.broadcastUserInfo();
		}
		
		return player.destroyItemByItemId(ItemProcessType.QUEST, item.getId(), toDelete, player, true);
	}
	
	/**
	 * Take a set amount of a specified item from player's inventory.
	 * @param player the player whose item to take
	 * @param holder the {@link ItemHolder} object containing the ID and count of the item to take
	 * @return {@code true} if the item was taken, {@code false} otherwise
	 */
	protected static boolean takeItem(Player player, ItemHolder holder)
	{
		return (holder != null) && takeItems(player, holder.getId(), holder.getCount());
	}
	
	/**
	 * Take a set amount of all specified items from player's inventory.
	 * @param player the player whose items to take
	 * @param itemList the list of {@link ItemHolder} objects containing the IDs and counts of the items to take
	 * @return {@code true} if all items were taken, {@code false} otherwise
	 */
	protected static boolean takeAllItems(Player player, ItemHolder... itemList)
	{
		if ((itemList == null) || (itemList.length == 0))
		{
			return false;
		}
		
		// first check if the player has all items to avoid taking half the items from the list
		if (!hasAllItems(player, true, itemList))
		{
			return false;
		}
		
		for (ItemHolder item : itemList)
		{
			// this should never be false, but just in case
			if (!takeItem(player, item))
			{
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Take an amount of all specified items from player's inventory.
	 * @param player the player whose items to take
	 * @param amount the amount to take of each item
	 * @param itemIds a list or an array of IDs of the items to take
	 * @return {@code true} if all items were taken, {@code false} otherwise
	 */
	public static boolean takeItems(Player player, int amount, int... itemIds)
	{
		boolean check = true;
		if (itemIds != null)
		{
			for (int item : itemIds)
			{
				check &= takeItems(player, item, amount);
			}
		}
		
		return check;
	}
	
	/**
	 * Send a packet in order to play a sound to the player.
	 * @param player the player whom to send the packet
	 * @param sound the name of the sound to play
	 */
	public static void playSound(Player player, String sound)
	{
		player.sendPacket(QuestSound.getSound(sound));
	}
	
	/**
	 * Send a packet in order to play a sound to the player.
	 * @param player the player whom to send the packet
	 * @param sound the {@link QuestSound} object of the sound to play
	 */
	public static void playSound(Player player, QuestSound sound)
	{
		player.sendPacket(sound.getPacket());
	}
	
	/**
	 * Add EXP and SP as quest reward.
	 * @param player the player whom to reward with the EXP/SP
	 * @param exp the amount of EXP to give to the player
	 * @param sp the amount of SP to give to the player
	 */
	public static void addExpAndSp(Player player, long exp, int sp)
	{
		long addExp = exp;
		int addSp = sp;
		
		// Premium rates
		if (player.hasPremiumStatus())
		{
			addExp *= PremiumSystemConfig.PREMIUM_RATE_QUEST_XP;
			addSp *= PremiumSystemConfig.PREMIUM_RATE_QUEST_SP;
		}
		
		player.addExpAndSp((long) player.calcStat(Stat.EXPSP_RATE, addExp * RatesConfig.RATE_QUEST_REWARD_XP, null, null), (int) player.calcStat(Stat.EXPSP_RATE, addSp * RatesConfig.RATE_QUEST_REWARD_SP, null, null));
		PcCafePointsManager.getInstance().givePcCafePoint(player, (long) (addExp * RatesConfig.RATE_QUEST_REWARD_XP));
	}
	
	/**
	 * Get a random integer from 0 (inclusive) to {@code max} (exclusive).<br>
	 * Use this method instead of importing {@link org.l2jmobius.commons.util.Rnd} utility.
	 * @param max the maximum value for randomization
	 * @return a random integer number from 0 to {@code max - 1}
	 */
	public static int getRandom(int max)
	{
		return Rnd.get(max);
	}
	
	/**
	 * Get a random integer from {@code min} (inclusive) to {@code max} (inclusive).<br>
	 * Use this method instead of importing {@link org.l2jmobius.commons.util.Rnd} utility.
	 * @param min the minimum value for randomization
	 * @param max the maximum value for randomization
	 * @return a random integer number from {@code min} to {@code max}
	 */
	public static int getRandom(int min, int max)
	{
		return Rnd.get(min, max);
	}
	
	/**
	 * Get a random long from 0 (inclusive) to {@code max} (exclusive).<br>
	 * Use this method instead of importing {@link org.l2jmobius.commons.util.Rnd} utility.
	 * @param max the maximum value for randomization
	 * @return a random long number from 0 to {@code max - 1}
	 */
	public static long getRandom(long max)
	{
		return Rnd.get(max);
	}
	
	/**
	 * Get a random long from {@code min} (inclusive) to {@code max} (inclusive).<br>
	 * Use this method instead of importing {@link org.l2jmobius.commons.util.Rnd} utility.
	 * @param min the minimum value for randomization
	 * @param max the maximum value for randomization
	 * @return a random long number from {@code min} to {@code max}
	 */
	public static long getRandom(long min, long max)
	{
		return Rnd.get(min, max);
	}
	
	/**
	 * Get a random boolean.<br>
	 * Use this method instead of importing {@link org.l2jmobius.commons.util.Rnd} utility.
	 * @return {@code true} or {@code false} randomly
	 */
	public static boolean getRandomBoolean()
	{
		return Rnd.nextBoolean();
	}
	
	/**
	 * Get a random entry.
	 * @param <T>
	 * @param array of values.
	 * @return one value from array.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getRandomEntry(T... array)
	{
		if (array.length == 0)
		{
			return null;
		}
		
		return array[getRandom(array.length)];
	}
	
	/**
	 * Get a random entry.
	 * @param <T>
	 * @param list of values.
	 * @return one value from list.
	 */
	public static <T> T getRandomEntry(List<T> list)
	{
		if (list.isEmpty())
		{
			return null;
		}
		
		return list.get(getRandom(list.size()));
	}
	
	/**
	 * Get a random entry.
	 * @param array of Integers.
	 * @return one Integer from array.
	 */
	public static int getRandomEntry(int... array)
	{
		return array[getRandom(array.length)];
	}
	
	/**
	 * Get the ID of the item equipped in the specified inventory slot of the player.
	 * @param player the player whose inventory to check
	 * @param slot the location in the player's inventory to check
	 * @return the ID of the item equipped in the specified inventory slot or 0 if the slot is empty or item is {@code null}.
	 */
	public static int getItemEquipped(Player player, int slot)
	{
		return player.getInventory().getPaperdollItemId(slot);
	}
	
	/**
	 * @return the number of ticks from the {@link org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager}.
	 */
	public static int getGameTicks()
	{
		return GameTimeTaskManager.getInstance().getGameTicks();
	}
	
	/**
	 * Execute a procedure for each player depending on the parameters.
	 * @param player the player on which the procedure will be executed
	 * @param npc the related NPC
	 * @param isSummon {@code true} if the event that called this method was originated by the player's summon, {@code false} otherwise
	 * @param includeParty if {@code true}, #actionForEachPlayer(Player, Npc, boolean) will be called with the player's party members
	 * @param includeCommandChannel if {@code true}, {@link #actionForEachPlayer(Player, Npc, boolean)} will be called with the player's command channel members
	 * @see #actionForEachPlayer(Player, Npc, boolean)
	 */
	public void executeForEachPlayer(Player player, Npc npc, boolean isSummon, boolean includeParty, boolean includeCommandChannel)
	{
		if ((includeParty || includeCommandChannel) && player.isInParty())
		{
			final Party party = player.getParty();
			if (includeCommandChannel && party.isInCommandChannel())
			{
				party.getCommandChannel().forEachMember(member ->
				{
					actionForEachPlayer(member, npc, isSummon);
					return true;
				});
			}
			else if (includeParty)
			{
				party.forEachMember(member ->
				{
					actionForEachPlayer(member, npc, isSummon);
					return true;
				});
			}
		}
		else
		{
			actionForEachPlayer(player, npc, isSummon);
		}
	}
	
	/**
	 * Overridable method called from {@link #executeForEachPlayer(Player, Npc, boolean, boolean, boolean)}
	 * @param player the player on which the action will be run
	 * @param npc the NPC related to this action
	 * @param isSummon {@code true} if the event that called this method was originated by the player's summon
	 */
	public void actionForEachPlayer(Player player, Npc npc, boolean isSummon)
	{
		// To be overridden in quest scripts.
	}
	
	/**
	 * Open a door if it is present on the instance and it is not open.
	 * @param doorId the ID of the door to open
	 * @param instanceId the ID of the instance the door is in (0 if the door is not not inside an instance)
	 */
	public void openDoor(int doorId, int instanceId)
	{
		final Door door = getDoor(doorId, instanceId);
		if (door == null)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": called openDoor(" + doorId + ", " + instanceId + "); but door was not found!", new NullPointerException());
		}
		else if (!door.isOpen())
		{
			door.openMe();
		}
	}
	
	/**
	 * Close a door if it is present in a specified the instance and it is open.
	 * @param doorId the ID of the door to close
	 * @param instanceId the ID of the instance the door is in (0 if the door is not not inside an instance)
	 */
	public void closeDoor(int doorId, int instanceId)
	{
		final Door door = getDoor(doorId, instanceId);
		if (door == null)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": called closeDoor(" + doorId + ", " + instanceId + "); but door was not found!", new NullPointerException());
		}
		else if (door.isOpen())
		{
			door.closeMe();
		}
	}
	
	/**
	 * Retrieve a door from an instance or the real world.
	 * @param doorId the ID of the door to get
	 * @param instanceId the ID of the instance the door is in (0 if the door is not not inside an instance)
	 * @return the found door or {@code null} if no door with that ID and instance ID was found
	 */
	public Door getDoor(int doorId, int instanceId)
	{
		final Door door;
		if (instanceId == 0)
		{
			door = DoorData.getInstance().getDoor(doorId);
		}
		else
		{
			final Instance instance = InstanceManager.getInstance().getInstance(instanceId);
			if (instance != null)
			{
				door = instance.getDoor(doorId);
			}
			else
			{
				door = DoorData.getInstance().getDoor(doorId);
			}
		}
		
		return door;
	}
	
	/**
	 * Teleport a player into/out of an instance.
	 * @param player the player to teleport
	 * @param loc the {@link Location} object containing the destination coordinates
	 * @param instanceId the ID of the instance to teleport the player to (0 to teleport out of an instance)
	 */
	public void teleportPlayer(Player player, Location loc, int instanceId)
	{
		teleportPlayer(player, loc, instanceId, true);
	}
	
	/**
	 * Teleport a player into/out of an instance.
	 * @param player the player to teleport
	 * @param loc the {@link Location} object containing the destination coordinates
	 * @param instanceId the ID of the instance to teleport the player to (0 to teleport out of an instance)
	 * @param allowRandomOffset if {@code true}, will randomize the teleport coordinates by +/-Config.MAX_OFFSET_ON_TELEPORT
	 */
	public void teleportPlayer(Player player, Location loc, int instanceId, boolean allowRandomOffset)
	{
		player.teleToLocation(loc, instanceId, allowRandomOffset ? PlayerConfig.MAX_OFFSET_ON_TELEPORT : 0);
	}
	
	/**
	 * Monster is running and attacking the character.
	 * @param npc the NPC that performs the attack
	 * @param target the target of the attack
	 */
	protected void addAttackDesire(Npc npc, Creature target)
	{
		addAttackDesire(npc, target, 999);
	}
	
	/**
	 * Monster is running and attacking the target.
	 * @param npc the NPC that performs the attack
	 * @param target the target of the attack
	 * @param desire the desire to perform the attack
	 */
	protected void addAttackDesire(Npc npc, Creature target, int desire)
	{
		if (npc.isAttackable())
		{
			npc.asAttackable().addDamageHate(target, 0, desire);
		}
		
		npc.setRunning();
		npc.getAI().setIntention(Intention.ATTACK, target);
	}
	
	/**
	 * Adds desire to move to the given NPC.
	 * @param npc the NPC
	 * @param loc the location
	 * @param desire the desire
	 */
	protected void addMoveToDesire(Npc npc, Location loc, int desire)
	{
		npc.getAI().setIntention(Intention.MOVE_TO, loc);
	}
	
	/**
	 * Instantly cast a skill upon the given target.
	 * @param npc the caster NPC
	 * @param target the target of the cast
	 * @param skill the skill to cast
	 */
	protected void castSkill(Npc npc, Playable target, SkillHolder skill)
	{
		npc.setTarget(target);
		npc.doCast(skill.getSkill());
	}
	
	/**
	 * Instantly cast a skill upon the given target.
	 * @param npc the caster NPC
	 * @param target the target of the cast
	 * @param skill the skill to cast
	 */
	protected void castSkill(Npc npc, Playable target, Skill skill)
	{
		npc.setTarget(target);
		npc.doCast(skill);
	}
	
	/**
	 * Adds the desire to cast a skill to the given NPC.
	 * @param npc the NPC whom cast the skill
	 * @param target the skill target
	 * @param skill the skill to cast
	 * @param desire the desire to cast the skill
	 */
	protected void addSkillCastDesire(Npc npc, Creature target, SkillHolder skill, int desire)
	{
		addSkillCastDesire(npc, target, skill.getSkill(), desire);
	}
	
	/**
	 * Adds the desire to cast a skill to the given NPC.
	 * @param npc the NPC whom cast the skill
	 * @param target the skill target
	 * @param skill the skill to cast
	 * @param desire the desire to cast the skill
	 */
	protected void addSkillCastDesire(Npc npc, Creature target, Skill skill, int desire)
	{
		if (npc.isAttackable())
		{
			npc.asAttackable().addDamageHate(target, 0, desire);
		}
		
		npc.setTarget(target);
		npc.getAI().setIntention(Intention.CAST, skill, target);
	}
	
	/**
	 * Sends the special camera packet to the player.
	 * @param player the player
	 * @param creature the watched creature
	 * @param force
	 * @param angle1
	 * @param angle2
	 * @param time
	 * @param range
	 * @param duration
	 * @param relYaw
	 * @param relPitch
	 * @param isWide
	 * @param relAngle
	 */
	public static void specialCamera(Player player, Creature creature, int force, int angle1, int angle2, int time, int range, int duration, int relYaw, int relPitch, int isWide, int relAngle)
	{
		player.sendPacket(new SpecialCamera(creature, force, angle1, angle2, time, range, duration, relYaw, relPitch, isWide, relAngle));
	}
	
	/**
	 * Sends the special camera packet to the player.
	 * @param player
	 * @param creature
	 * @param force
	 * @param angle1
	 * @param angle2
	 * @param time
	 * @param duration
	 * @param relYaw
	 * @param relPitch
	 * @param isWide
	 * @param relAngle
	 */
	public static void specialCameraEx(Player player, Creature creature, int force, int angle1, int angle2, int time, int duration, int relYaw, int relPitch, int isWide, int relAngle)
	{
		player.sendPacket(new SpecialCamera(creature, player, force, angle1, angle2, time, duration, relYaw, relPitch, isWide, relAngle));
	}
	
	/**
	 * Sends the special camera packet to the player.
	 * @param player
	 * @param creature
	 * @param force
	 * @param angle1
	 * @param angle2
	 * @param time
	 * @param range
	 * @param duration
	 * @param relYaw
	 * @param relPitch
	 * @param isWide
	 * @param relAngle
	 * @param unk
	 */
	public static void specialCamera3(Player player, Creature creature, int force, int angle1, int angle2, int time, int range, int duration, int relYaw, int relPitch, int isWide, int relAngle, int unk)
	{
		player.sendPacket(new SpecialCamera(creature, force, angle1, angle2, time, range, duration, relYaw, relPitch, isWide, relAngle, unk));
	}
	
	/**
	 * @param player
	 * @param x
	 * @param y
	 * @param z
	 */
	public static void addRadar(Player player, int x, int y, int z)
	{
		player.getRadar().addMarker(x, y, z);
	}
	
	/**
	 * @param player
	 * @param x
	 * @param y
	 * @param z
	 */
	public void removeRadar(Player player, int x, int y, int z)
	{
		player.getRadar().removeMarker(x, y, z);
	}
	
	/**
	 * @param player
	 */
	public void clearRadar(Player player)
	{
		player.getRadar().removeAllMarkers();
	}
	
	public QuestState getClanLeaderQuestState(Player player, Npc npc)
	{
		// If player is the leader, retrieves directly the qS and bypass others checks
		if (player.isClanLeader() && player.isInsideRadius3D(npc, PlayerConfig.ALT_PARTY_RANGE))
		{
			return player.getQuestState(getName());
		}
		
		// Verify if the player got a clan
		final Clan clan = player.getClan();
		if (clan == null)
		{
			return null;
		}
		
		// Verify if the leader is online
		final Player leader = clan.getLeader().getPlayer();
		if (leader == null)
		{
			return null;
		}
		
		if (leader.isDead())
		{
			return null;
		}
		
		// Verify if the player is on the radius of the leader. If true, send leader's quest state.
		if (leader.isInsideRadius3D(npc, PlayerConfig.ALT_PARTY_RANGE))
		{
			return leader.getQuestState(getName());
		}
		
		return null;
	}
	
	private void setQuestToOfflineMembers(List<Integer> objectsId)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			final PreparedStatement stm = con.prepareStatement("INSERT INTO character_quests (charId,name,var,value) VALUES (?,?,?,?)");
			for (Integer charId : objectsId)
			{
				stm.setInt(1, charId.intValue());
				stm.setString(2, getName());
				stm.setString(3, "<state>");
				stm.setString(4, "1");
				stm.executeUpdate();
			}
			
			stm.close();
			con.close();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Error in updating character_quest table from Quest.java on method setQuestToOfflineMembers");
			LOGGER.info(e.toString());
		}
	}
	
	private void deleteQuestToOfflineMembers(int clanId)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			final PreparedStatement stm = con.prepareStatement("DELETE FROM character_quests WHERE name = ? and charId IN (SELECT charId FROM characters WHERE clanid = ? AND online = 0)");
			stm.setString(1, getName());
			stm.setInt(2, clanId);
			stm.executeUpdate();
			
			stm.close();
			con.close();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Error in deleting infos from character_quest table from Quest.java on method deleteQuestToOfflineMembers");
			LOGGER.info(e.toString());
		}
	}
	
	/**
	 * Sets the current quest to clan offline's members
	 * @param player the current player (should be clan leader)
	 */
	public void setQuestToClanMembers(Player player)
	{
		if (player.isClanLeader())
		{
			// Setting it for online members...
			for (Player onlineMember : player.getClan().getOnlineMembers(0))
			{
				if (!onlineMember.isClanLeader())
				{
					onlineMember.setQuestState(player.getQuestState(getName()));
				}
			}
			
			// Setting it for offline members...
			setQuestToOfflineMembers(player.getClan().getOfflineMembersIds());
		}
	}
	
	/**
	 * Finish the current quest to a clan's members
	 * @param player clan's leader
	 */
	public void finishQuestToClan(Player player)
	{
		if (player.isClanLeader())
		{
			// Deleting it for online members...
			for (Player onlineMember : player.getClan().getOnlineMembers(0))
			{
				if (!onlineMember.isClanLeader())
				{
					onlineMember.delQuestState(getName());
				}
			}
			
			// Deleting it for offline members...
			deleteQuestToOfflineMembers(player.getClanId());
		}
	}
}
