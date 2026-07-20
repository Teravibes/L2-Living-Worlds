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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.config.custom.FakePlayersConfig;
import org.l2jmobius.gameserver.config.custom.PrivateStoreRangeConfig;
import org.l2jmobius.gameserver.config.custom.PvpAnnounceConfig;
import org.l2jmobius.gameserver.config.custom.PvpRewardItemConfig;
import org.l2jmobius.gameserver.data.xml.DynamicExpRateData;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.data.xml.NpcPersonalAIData;
import org.l2jmobius.gameserver.handler.BypassHandler;
import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.managers.CHSiegeManager;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.ItemManager;
import org.l2jmobius.gameserver.managers.RaidBossSpawnManager;
import org.l2jmobius.gameserver.managers.TownManager;
import org.l2jmobius.gameserver.managers.WalkingManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.actor.enums.creature.Team;
import org.l2jmobius.gameserver.model.actor.enums.npc.AISkillScope;
import org.l2jmobius.gameserver.model.actor.enums.npc.AIType;
import org.l2jmobius.gameserver.model.actor.enums.player.PrivateStoreType;
import org.l2jmobius.gameserver.model.actor.holders.npc.FakePlayerAppearance;
import org.l2jmobius.gameserver.model.actor.holders.npc.FakePlayerHolder;
import org.l2jmobius.gameserver.model.actor.instance.ClanHallManager;
import org.l2jmobius.gameserver.model.actor.instance.Doorman;
import org.l2jmobius.gameserver.model.actor.instance.Fisherman;
import org.l2jmobius.gameserver.model.actor.instance.Merchant;
import org.l2jmobius.gameserver.model.actor.instance.Teleporter;
import org.l2jmobius.gameserver.model.actor.instance.Trainer;
import org.l2jmobius.gameserver.model.actor.instance.Warehouse;
import org.l2jmobius.gameserver.model.actor.stat.NpcStat;
import org.l2jmobius.gameserver.model.actor.status.NpcStatus;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.npc.OnNpcCanBeSeen;
import org.l2jmobius.gameserver.model.events.holders.actor.npc.OnNpcEventReceived;
import org.l2jmobius.gameserver.model.events.holders.actor.npc.OnNpcSkillFinished;
import org.l2jmobius.gameserver.model.events.holders.actor.npc.OnNpcSpawn;
import org.l2jmobius.gameserver.model.events.holders.actor.npc.OnNpcTeleport;
import org.l2jmobius.gameserver.model.events.returns.TerminateReturn;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.Weapon;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.enums.ShotType;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.olympiad.Olympiad;
import org.l2jmobius.gameserver.model.script.QuestTimer;
import org.l2jmobius.gameserver.model.script.timers.TimerHolder;
import org.l2jmobius.gameserver.model.sevensigns.SevenSigns;
import org.l2jmobius.gameserver.model.sevensigns.SevenSignsFestival;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.model.siege.clanhalls.SiegableHall;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.spawns.Spawn;
import org.l2jmobius.gameserver.model.stats.Formulas;
import org.l2jmobius.gameserver.model.variables.NpcVariables;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.model.zone.type.TownZone;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.AbstractNpcInfo;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.FakePlayerInfo;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;
import org.l2jmobius.gameserver.network.serverpackets.PrivateStoreMsgBuy;
import org.l2jmobius.gameserver.network.serverpackets.PrivateStoreMsgSell;
import org.l2jmobius.gameserver.network.serverpackets.RecipeShopMsg;
import org.l2jmobius.gameserver.network.serverpackets.ServerObjectInfo;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.taskmanagers.DecayTaskManager;
import org.l2jmobius.gameserver.taskmanagers.ItemsAutoDestroyTaskManager;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * This class represents a Non-Player-Creature in the world.<br>
 * It can be a monster or a friendly creature.<br>
 * It uses a template to fetch some static values.
 */
public class Npc extends Creature
{
	/** The interaction distance of the Npc(is used as offset in MovetoLocation method) */
	public static final int INTERACTION_DISTANCE = 250;
	/** Maximum distance where the drop may appear given this NPC position. */
	public static final int RANDOM_ITEM_DROP_LIMIT = 70;
	/** Ids of NPCs that see creatures through the OnCreatureSee event. */
	private static final Set<Integer> CREATURE_SEE_IDS = ConcurrentHashMap.newKeySet();
	/** The Spawn object that manage this Npc */
	private Spawn _spawn;
	/** The flag to specify if this Npc is busy */
	private boolean _isBusy = false;
	/** The busy message for this Npc */
	private String _busyMessage = "";
	/** True if endDecayTask has already been called */
	private volatile boolean _isDecayed = false;
	/** The castle index in the array of Castle this Npc belongs to */
	private int _castleIndex = -2;
	private boolean _isInTown = false;
	/** True if this Npc is autoattackable **/
	private boolean _isAutoAttackable = false;
	/** Time of last social packet broadcast */
	private long _lastSocialBroadcast = 0;
	/** Minimum interval between social packets */
	private static final int MINIMUM_SOCIAL_INTERVAL = 6000;
	/** Support for random animation switching */
	private boolean _isRandomAnimationEnabled = true;
	private boolean _isRandomWalkingEnabled = true;
	private boolean _isWalker = false;
	private boolean _isTalkable = getTemplate().isTalkable();
	private final boolean _isQuestMonster = getTemplate().isQuestMonster();
	private final boolean _isFakePlayer = getTemplate().isFakePlayer();
	private FakePlayerAppearance _fakePlayerAppearance; // optional per-instance look for procedurally generated fake players

	private int _currentLHandId; // normally this shouldn't change from the template, but there exist exceptions
	private int _currentRHandId; // normally this shouldn't change from the template, but there exist exceptions
	private int _currentEnchant; // normally this shouldn't change from the template, but there exist exceptions
	private double _currentCollisionHeight; // used for npc grow effect skills
	private double _currentCollisionRadius; // used for npc grow effect skills
	
	private int _soulshotamount = 0;
	private int _spiritshotamount = 0;
	
	private int _shotsMask = 0;
	private int _killingBlowWeaponId;
	
	private volatile int _scriptValue = 0;
	
	/** Map of summoned NPCs by this NPC. */
	private Map<Integer, Npc> _summonedNpcs = null;
	
	private final List<QuestTimer> _questTimers = new ArrayList<>();
	private final List<TimerHolder<?>> _timerHolders = new ArrayList<>();
	
	/**
	 * Creates a NPC.
	 * @param template the NPC template
	 */
	public Npc(NpcTemplate template)
	{
		// Call the Creature constructor to set the _template of the Creature, copy skills from template to object
		// and link _calculators to NPC_STD_CALCULATOR
		super(template);
		setInstanceType(InstanceType.Npc);
		initCharStatusUpdateValues();
		
		// initialize the "current" equipment
		_currentLHandId = getTemplate().getLHandId();
		_currentRHandId = getTemplate().getRHandId();
		_currentEnchant = NpcConfig.ENABLE_RANDOM_ENCHANT_EFFECT ? Rnd.get(4, 21) : getTemplate().getWeaponEnchant();
		
		// initialize the "current" collisions
		_currentCollisionHeight = getTemplate().getFCollisionHeight();
		_currentCollisionRadius = getTemplate().getFCollisionRadius();
		setFlying(template.isFlying());
	}
	
	/**
	 * Creates a NPC.
	 * @param npcId the NPC ID
	 */
	public Npc(int npcId)
	{
		this(NpcData.getInstance().getTemplate(npcId));
	}
	
	public int getSoulShotChance()
	{
		return getTemplate().getSoulShotChance();
	}
	
	public int getSpiritShotChance()
	{
		return getTemplate().getSpiritShotChance();
	}
	
	public int getMinSkillChance()
	{
		return getTemplate().getMinSkillChance();
	}
	
	public int getMaxSkillChance()
	{
		return getTemplate().getMaxSkillChance();
	}
	
	/**
	 * Verifies if the NPC can cast a skill given the minimum and maximum skill chances.
	 * @return {@code true} if the NPC has chances of casting a skill
	 */
	public boolean hasSkillChance()
	{
		return Rnd.get(100) < Rnd.get(getMinSkillChance(), getMaxSkillChance());
	}
	
	public boolean canMove()
	{
		return getTemplate().canMove();
	}
	
	public boolean isChaos()
	{
		return getTemplate().isChaos();
	}
	
	public List<Skill> getLongRangeSkills()
	{
		return getTemplate().getAISkills(AISkillScope.LONG_RANGE);
	}
	
	public List<Skill> getShortRangeSkills()
	{
		return getTemplate().getAISkills(AISkillScope.SHORT_RANGE);
	}
	
	/**
	 * Send a packet SocialAction to all Player in the _KnownPlayers of the Npc and create a new RandomAnimation Task.
	 * @param animationId
	 */
	public void onRandomAnimation(int animationId)
	{
		// Send a packet SocialAction to all Player in the _KnownPlayers of the Npc
		final long now = System.currentTimeMillis();
		if ((now - _lastSocialBroadcast) > MINIMUM_SOCIAL_INTERVAL)
		{
			_lastSocialBroadcast = now;
			broadcastSocialAction(animationId);
		}
	}
	
	/**
	 * @return true if the server allows Random Animation.
	 */
	public boolean hasRandomAnimation()
	{
		return ((GeneralConfig.MAX_NPC_ANIMATION > 0) && isRandomAnimationEnabled() && (getAiType() != AIType.CORPSE));
	}
	
	/**
	 * Switches random Animation state into val.
	 * @param value needed state of random animation
	 */
	public void setRandomAnimationEnabled(boolean value)
	{
		_isRandomAnimationEnabled = value;
	}
	
	/**
	 * @return {@code true}, if random animation is enabled, {@code false} otherwise.
	 */
	public boolean isRandomAnimationEnabled()
	{
		return !_isFakePlayer && _isRandomAnimationEnabled;
	}
	
	public void setRandomWalking(boolean enabled)
	{
		_isRandomWalkingEnabled = enabled;
	}
	
	public boolean isRandomWalkingEnabled()
	{
		return _isRandomWalkingEnabled;
	}
	
	@Override
	public NpcStat getStat()
	{
		return (NpcStat) super.getStat();
	}
	
	@Override
	public void initCharStat()
	{
		setStat(new NpcStat(this));
	}
	
	@Override
	public NpcStatus getStatus()
	{
		return (NpcStatus) super.getStatus();
	}
	
	@Override
	public void initCharStatus()
	{
		setStatus(new NpcStatus(this));
	}
	
	/** Return the NpcTemplate of the Npc. */
	@Override
	public NpcTemplate getTemplate()
	{
		return (NpcTemplate) super.getTemplate();
	}
	
	/**
	 * Gets the NPC ID.
	 * @return the NPC ID
	 */
	@Override
	public int getId()
	{
		return getTemplate().getId();
	}
	
	@Override
	public boolean canBeAttacked()
	{
		return NpcConfig.ALT_ATTACKABLE_NPCS;
	}
	
	/**
	 * Return the Level of this Npc contained in the NpcTemplate.
	 */
	@Override
	public int getLevel()
	{
		return getTemplate().getLevel();
	}
	
	/**
	 * @return True if the Npc is aggressive (ex : Monster in function of aggroRange).
	 */
	public boolean isAggressive()
	{
		return false;
	}
	
	/**
	 * @return the Aggro Range of this Npc either contained in the NpcTemplate, or overriden by spawnlist AI value.
	 */
	public int getAggroRange()
	{
		return hasAIValue("aggroRange") ? getAIValue("aggroRange") : getTemplate().getAggroRange();
	}
	
	public boolean isInMyClan(Npc npc)
	{
		return getTemplate().isClan(npc.getTemplate().getClans());
	}
	
	/**
	 * Return True if this Npc is undead in function of the NpcTemplate.
	 */
	@Override
	public boolean isUndead()
	{
		return getTemplate().getRace() == Race.UNDEAD;
	}
	
	/**
	 * Send a packet NpcInfo with state of abnormal effect to all Player in the _KnownPlayers of the Npc.
	 */
	@Override
	public void updateAbnormalEffect()
	{
		World.getInstance().forEachVisibleObject(this, Player.class, player ->
		{
			if (!isVisibleFor(player))
			{
				return;
			}
			
			if (_isFakePlayer)
			{
				player.sendPacket(new FakePlayerInfo(this));
				// Vendors also need their store title sign so the "Sell"/"Buy" tag shows above them.
				final FakePlayerAppearance look = _fakePlayerAppearance;
				if ((look != null) && (look.getPrivateStoreType() != 0))
				{
					final int storeType = look.getPrivateStoreType();
					if (storeType == PrivateStoreType.BUY.getId())
					{
						player.sendPacket(new PrivateStoreMsgBuy(getObjectId(), look.getStoreMessage()));
					}
					else if (storeType == PrivateStoreType.MANUFACTURE.getId())
					{
						player.sendPacket(new RecipeShopMsg(getObjectId(), look.getStoreMessage()));
					}
					else
					{
						player.sendPacket(new PrivateStoreMsgSell(getObjectId(), look.getStoreMessage()));
					}
				}
			}
			else if (getRunSpeed() == 0)
			{
				player.sendPacket(new ServerObjectInfo(this, player));
			}
			else if (isSpawned())
			{
				player.sendPacket(new AbstractNpcInfo.NpcInfo(this, player));
			}
		});
	}
	
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		return _isAutoAttackable;
	}
	
	public void setAutoAttackable(boolean flag)
	{
		_isAutoAttackable = flag;
	}
	
	/**
	 * @return the Identifier of the item in the left hand of this Npc contained in the NpcTemplate.
	 */
	public int getLeftHandItem()
	{
		return _currentLHandId;
	}
	
	/**
	 * @return the Identifier of the item in the right hand of this Npc contained in the NpcTemplate.
	 */
	public int getRightHandItem()
	{
		return _currentRHandId;
	}
	
	public int getEnchantEffect()
	{
		return _currentEnchant;
	}
	
	/**
	 * @return the busy status of this Npc.
	 */
	public boolean isBusy()
	{
		return _isBusy;
	}
	
	/**
	 * @param isBusy the busy status of this Npc
	 */
	public void setBusy(boolean isBusy)
	{
		_isBusy = isBusy;
	}
	
	/**
	 * @return the busy message of this Npc.
	 */
	public String getBusyMessage()
	{
		return _busyMessage;
	}
	
	/**
	 * @param message the busy message of this Npc.
	 */
	public void setBusyMessage(String message)
	{
		_busyMessage = message;
	}
	
	/**
	 * @return true if this Npc instance can be warehouse manager.
	 */
	public boolean isWarehouse()
	{
		return false;
	}
	
	public boolean canTarget(Player player)
	{
		if (player.isOutOfControl())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		if (player.isLockedTarget() && (player.getLockedTarget() != this))
		{
			player.sendPacket(SystemMessageId.FAILED_TO_CHANGE_ATTACK_TARGET);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		return true;
	}
	
	public boolean canInteract(Player player)
	{
		if (player.isCastingNow() || player.isCastingSimultaneouslyNow())
		{
			return false;
		}
		else if (player.isDead() || player.isFakeDeath())
		{
			return false;
		}
		else if (player.isSitting())
		{
			return false;
		}
		else if (player.isInStoreMode())
		{
			return false;
		}
		else if (!isInsideRadius3D(player, INTERACTION_DISTANCE))
		{
			return false;
		}
		else if (player.getInstanceId() != getInstanceId())
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * @return the Castle this Npc belongs to.
	 */
	public Castle getCastle()
	{
		// Get castle this NPC belongs to (excluding Attackable)
		if (_castleIndex < 0)
		{
			final TownZone town = TownManager.getTown(getX(), getY(), getZ());
			if (town != null)
			{
				_castleIndex = CastleManager.getInstance().getCastleIndex(town.getTaxById());
			}
			
			if (_castleIndex < 0)
			{
				_castleIndex = CastleManager.getInstance().findNearestCastleIndex(this);
			}
			else
			{
				_isInTown = true; // Npc was spawned in town
			}
		}
		
		if (_castleIndex < 0)
		{
			return null;
		}
		
		return CastleManager.getInstance().getCastles().get(_castleIndex);
	}
	
	/**
	 * Verify if the given player is this NPC's lord.
	 * @param player the player to check
	 * @return {@code true} if the player is clan leader and owner of a castle of fort that this NPC belongs to, {@code false} otherwise
	 */
	public boolean isMyLord(Player player)
	{
		if (player.isClanLeader())
		{
			final int castleId = getCastle() != null ? getCastle().getResidenceId() : -1;
			return player.getClan().getCastleId() == castleId;
		}
		
		return false;
	}
	
	public SiegableHall getConquerableHall()
	{
		return CHSiegeManager.getInstance().getNearbyClanHall(getX(), getY(), 10000);
	}
	
	/**
	 * Return closest castle in defined distance
	 * @param maxDistance long
	 * @return Castle
	 */
	public Castle getCastle(long maxDistance)
	{
		final int index = CastleManager.getInstance().findNearestCastleIndex(this, maxDistance);
		if (index < 0)
		{
			return null;
		}
		
		return CastleManager.getInstance().getCastles().get(index);
	}
	
	public boolean isInTown()
	{
		if (_castleIndex < 0)
		{
			getCastle();
		}
		
		return _isInTown;
	}
	
	/**
	 * Open a quest or chat window on client with the text of the Npc in function of the command.<br>
	 * <br>
	 * <b><u>Example of use</u>:</b>
	 * <ul>
	 * <li>Client packet : RequestBypassToServer</li>
	 * </ul>
	 * @param player
	 * @param command The command string received from client
	 */
	public void onBypassFeedback(Player player, String command)
	{
		if (canInteract(player))
		{
			if (_isBusy && (_busyMessage.length() > 0))
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player, "data/html/npcbusy.htm");
				html.replace("%busymessage%", _busyMessage);
				html.replace("%npcname%", getName());
				html.replace("%playername%", player.getName());
				player.sendPacket(html);
			}
			else
			{
				final IBypassHandler handler = BypassHandler.getInstance().getHandler(command);
				if (handler != null)
				{
					handler.onCommand(command, player, this);
				}
				else
				{
					LOGGER.info(getClass().getSimpleName() + ": Unknown NPC bypass: \"" + command + "\" NpcId: " + getId());
				}
			}
		}
	}
	
	/**
	 * Return null (regular NPCs don't have weapons instances).
	 */
	@Override
	public Item getActiveWeaponInstance()
	{
		return null;
	}
	
	/**
	 * Return the weapon item equipped in the right hand of the Npc or null.
	 */
	@Override
	public Weapon getActiveWeaponItem()
	{
		// Get the weapon identifier equipped in the right hand of the Npc
		final int weaponId = getTemplate().getRHandId();
		if (weaponId < 1)
		{
			return null;
		}
		
		// Get the weapon item equipped in the right hand of the Npc
		final ItemTemplate item = ItemData.getInstance().getTemplate(getTemplate().getRHandId());
		if (!(item instanceof Weapon))
		{
			return null;
		}
		
		return (Weapon) item;
	}
	
	/**
	 * Return null (regular NPCs don't have weapons instances).
	 */
	@Override
	public Item getSecondaryWeaponInstance()
	{
		return null;
	}
	
	/**
	 * Return the weapon item equipped in the left hand of the Npc or null.
	 */
	@Override
	public Weapon getSecondaryWeaponItem()
	{
		// Get the weapon identifier equipped in the right hand of the Npc
		final int weaponId = getTemplate().getLHandId();
		if (weaponId < 1)
		{
			return null;
		}
		
		// Get the weapon item equipped in the right hand of the Npc
		final ItemTemplate item = ItemData.getInstance().getTemplate(getTemplate().getLHandId());
		if (!(item instanceof Weapon))
		{
			return null;
		}
		
		return (Weapon) item;
	}
	
	/**
	 * Send a Server->Client packet NpcHtmlMessage to the Player in order to display the message of the Npc.
	 * @param player The Player who talks with the Npc
	 * @param content The text of the NpcMessage
	 */
	public void insertObjectIdAndShowChatWindow(Player player, String content)
	{
		// Send a Server->Client packet NpcHtmlMessage to the Player in order to display the message of the Npc
		player.sendPacket(new NpcHtmlMessage(getObjectId(), content.replaceAll("%objectId%", String.valueOf(getObjectId()))));
	}
	
	/**
	 * <b><U Format of the pathfile</u>:</b>
	 * <ul>
	 * <li>if the file exists on the server (page number = 0) : <b>data/html/default/12006.htm</b> (npcId-page number)</li>
	 * <li>if the file exists on the server (page number > 0) : <b>data/html/default/12006-1.htm</b> (npcId-page number)</li>
	 * <li>if the file doesn't exist on the server : <b>data/html/npcdefault.htm</b> (message : "I have nothing to say to you")</li>
	 * </ul>
	 * @param npcId The Identifier of the Npc whose text must be display
	 * @param value The number of the page to display
	 * @return the pathfile of the selected HTML file in function of the npcId and of the page number.
	 */
	public String getHtmlPath(int npcId, int value)
	{
		String pom = "";
		if (value == 0)
		{
			pom = Integer.toString(npcId);
		}
		else
		{
			pom = npcId + "-" + value;
		}
		
		final String temp = "data/html/default/" + pom + ".htm";
		if (GeneralConfig.HTM_CACHE)
		{
			// If not running lazy cache the file must be in the cache or it does not exist
			if (HtmCache.getInstance().contains(temp))
			{
				return temp;
			}
		}
		else
		{
			final File file = new File(ServerConfig.DATAPACK_ROOT, temp);
			if (file.isFile())
			{
				final String lowerCaseName = file.getName().toLowerCase();
				if (lowerCaseName.endsWith(".htm") || lowerCaseName.endsWith(".html"))
				{
					return temp;
				}
			}
		}
		
		// If the file is not found, the standard message "I have nothing to say to you" is returned
		return "data/html/npcdefault.htm";
	}
	
	public void showChatWindow(Player player)
	{
		showChatWindow(player, 0);
	}
	
	/**
	 * Returns true if html exists
	 * @param player
	 * @param type
	 * @return boolean
	 */
	private boolean showPkDenyChatWindow(Player player, String type)
	{
		final String html = HtmCache.getInstance().getHtm(player, "data/html/" + type + "/" + getId() + "-pk.htm");
		if (html != null)
		{
			insertObjectIdAndShowChatWindow(player, html);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return true;
		}
		
		return false;
	}
	
	/**
	 * Open a chat window on client with the text of the Npc.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Get the text of the selected HTML file in function of the npcId and of the page number</li>
	 * <li>Send a Server->Client NpcHtmlMessage containing the text of the Npc to the Player</li>
	 * <li>Send a Server->Client ActionFailed to the Player in order to avoid that the client wait another packet</li>
	 * </ul>
	 * @param player The Player that talk with the Npc
	 * @param value The number of the page of the Npc to display
	 */
	public void showChatWindow(Player player, int value)
	{
		if (!_isTalkable)
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.isCursedWeaponEquipped() && (!(player.getTarget() instanceof ClanHallManager) || !(player.getTarget() instanceof Doorman)))
		{
			player.setTarget(player);
			return;
		}
		
		if (player.getKarma() > 0)
		{
			if (!PlayerConfig.ALT_GAME_KARMA_PLAYER_CAN_SHOP && (this instanceof Merchant))
			{
				if (showPkDenyChatWindow(player, "merchant"))
				{
					return;
				}
			}
			else if (!PlayerConfig.ALT_GAME_KARMA_PLAYER_CAN_USE_GK && (this instanceof Teleporter))
			{
				if (showPkDenyChatWindow(player, "teleporter"))
				{
					return;
				}
			}
			else if (!PlayerConfig.ALT_GAME_KARMA_PLAYER_CAN_USE_WAREHOUSE && (this instanceof Warehouse))
			{
				if (showPkDenyChatWindow(player, "warehouse"))
				{
					return;
				}
			}
			else if (!PlayerConfig.ALT_GAME_KARMA_PLAYER_CAN_SHOP && (this instanceof Fisherman))
			{
				if (showPkDenyChatWindow(player, "fisherman"))
				{
					return;
				}
			}
		}
		
		if (getTemplate().isType("Auctioneer") && (value == 0))
		{
			return;
		}
		
		final int npcId = getTemplate().getId();
		
		/* For use with Seven Signs implementation */
		String filename = SevenSigns.SEVEN_SIGNS_HTML_PATH;
		final int sealAvariceOwner = SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_AVARICE);
		final int sealGnosisOwner = SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_GNOSIS);
		final int playerCabal = SevenSigns.getInstance().getPlayerCabal(player.getObjectId());
		final int compWinner = SevenSigns.getInstance().getCabalHighestScore();
		
		switch (npcId)
		{
			case 31127: //
			case 31128: //
			case 31129: // Dawn Festival Guides
			case 31130: //
			case 31131: //
			{
				filename += "festival/dawn_guide.htm";
				break;
			}
			case 31137: //
			case 31138: //
			case 31139: // Dusk Festival Guides
			case 31140: //
			case 31141: //
			{
				filename += "festival/dusk_guide.htm";
				break;
			}
			case 31092: // Black Marketeer of Mammon
			{
				filename += "blkmrkt_1.htm";
				break;
			}
			case 31113: // Merchant of Mammon
			{
				if (FeatureConfig.ALT_STRICT_SEVENSIGNS)
				{
					switch (compWinner)
					{
						case SevenSigns.CABAL_DAWN:
						{
							if ((playerCabal != compWinner) || (playerCabal != sealAvariceOwner))
							{
								player.sendPacket(SystemMessageId.ONLY_A_LORD_OF_DAWN_MAY_USE_THIS);
								player.sendPacket(ActionFailed.STATIC_PACKET);
								return;
							}
							break;
						}
						case SevenSigns.CABAL_DUSK:
						{
							if ((playerCabal != compWinner) || (playerCabal != sealAvariceOwner))
							{
								player.sendPacket(SystemMessageId.ONLY_A_REVOLUTIONARY_OF_DUSK_MAY_USE_THIS);
								player.sendPacket(ActionFailed.STATIC_PACKET);
								return;
							}
							break;
						}
						default:
						{
							player.sendPacket(SystemMessageId.THIS_IS_A_QUEST_EVENT_PERIOD);
							return;
						}
					}
				}
				
				filename += "mammmerch_1.htm";
				break;
			}
			case 31126: // Blacksmith of Mammon
			{
				if (FeatureConfig.ALT_STRICT_SEVENSIGNS)
				{
					switch (compWinner)
					{
						case SevenSigns.CABAL_DAWN:
						{
							if ((playerCabal != compWinner) || (playerCabal != sealGnosisOwner))
							{
								player.sendPacket(SystemMessageId.ONLY_A_LORD_OF_DAWN_MAY_USE_THIS);
								player.sendPacket(ActionFailed.STATIC_PACKET);
								return;
							}
							break;
						}
						case SevenSigns.CABAL_DUSK:
						{
							if ((playerCabal != compWinner) || (playerCabal != sealGnosisOwner))
							{
								player.sendPacket(SystemMessageId.ONLY_A_REVOLUTIONARY_OF_DUSK_MAY_USE_THIS);
								player.sendPacket(ActionFailed.STATIC_PACKET);
								return;
							}
							break;
						}
						default:
						{
							player.sendPacket(SystemMessageId.THIS_IS_A_QUEST_EVENT_PERIOD);
							return;
						}
					}
				}
				
				filename += "mammblack_1.htm";
				break;
			}
			case 31132:
			case 31133:
			case 31134:
			case 31135:
			case 31136: // Festival Witches
			case 31142:
			case 31143:
			case 31144:
			case 31145:
			case 31146:
			{
				filename += "festival/festival_witch.htm";
				break;
			}
			case 31688:
			{
				if (player.isNoble())
				{
					filename = Olympiad.OLYMPIAD_HTML_PATH + "noble_main.htm";
				}
				else
				{
					filename = (getHtmlPath(npcId, value));
				}
				break;
			}
			case 31690:
			case 31769:
			case 31770:
			case 31771:
			case 31772:
			{
				if (player.isHero() || player.isNoble())
				{
					filename = Olympiad.OLYMPIAD_HTML_PATH + "hero_main.htm";
				}
				else
				{
					filename = (getHtmlPath(npcId, value));
				}
				break;
			}
			case 30298: // Blacksmith Pinter
			{
				if (player.isAcademyMember())
				{
					filename = (getHtmlPath(npcId, 1));
				}
				else
				{
					filename = (getHtmlPath(npcId, value));
				}
				break;
			}
			default:
			{
				if ((npcId >= 31865) && (npcId <= 31918))
				{
					if (value == 0)
					{
						filename += "rift/GuardianOfBorder.htm";
					}
					else
					{
						filename += "rift/GuardianOfBorder-" + value + ".htm";
					}
					break;
				}
				
				if (((npcId >= 31093) && (npcId <= 31094)) || ((npcId >= 31172) && (npcId <= 31201)) || ((npcId >= 31239) && (npcId <= 31254)))
				{
					return;
				}
				
				// Get the text of the selected HTML file in function of the npcId and of the page number
				filename = (getHtmlPath(npcId, value));
				break;
			}
		}
		
		// Send a Server->Client NpcHtmlMessage containing the text of the Npc to the Player
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player, filename);
		html.replace("%npcname%", String.valueOf(getName()));
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%festivalMins%", SevenSignsFestival.getInstance().getTimeToNextFestivalStr());
		player.sendPacket(html);
		
		// Send a Server->Client ActionFailed to the Player in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	/**
	 * Open a chat window on client with the text specified by the given file name and path, relative to the datapack root.
	 * @param player The Player that talk with the Npc
	 * @param filename The filename that contains the text to send
	 */
	public void showChatWindow(Player player, String filename)
	{
		// Send a Server->Client NpcHtmlMessage containing the text of the Npc to the Player
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player, filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
		
		// Send a Server->Client ActionFailed to the Player in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	/**
	 * @param level
	 * @return the Exp Reward of this Npc (modified by RATE_XP, or from DynamicExpRates.xml if enabled).
	 */
	public long getExpReward(int level)
	{
		if (DynamicExpRateData.getInstance().isEnabled())
		{
			return (long) (getTemplate().getExp() * DynamicExpRateData.getInstance().getDynamicExpRate(level));
		}
		
		return (long) (getTemplate().getExp() * RatesConfig.RATE_XP);
	}
	
	/**
	 * @param level
	 * @return the SP Reward of this Npc (modified by RATE_SP, or from DynamicExpRates.xml if enabled).
	 */
	public int getSpReward(int level)
	{
		if (DynamicExpRateData.getInstance().isEnabled())
		{
			return (int) (getTemplate().getSP() * DynamicExpRateData.getInstance().getDynamicSpRate(level));
		}
		
		return (int) (getTemplate().getSP() * RatesConfig.RATE_SP);
	}
	
	/**
	 * Kill the Npc (the corpse disappeared after 7 seconds).<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Create a DecayTask to remove the corpse of the Npc after 7 seconds</li>
	 * <li>Set target to null and cancel Attack or Cast</li>
	 * <li>Stop movement</li>
	 * <li>Stop HP/MP/CP Regeneration task</li>
	 * <li>Stop all active skills effects in progress on the Creature</li>
	 * <li>Send the Server->Client packet StatusUpdate with current HP and MP to all other Player to inform</li>
	 * <li>Notify Creature AI</li>
	 * </ul>
	 * @param killer The Creature who killed it
	 */
	@Override
	public boolean doDie(Creature killer)
	{
		if (!super.doDie(killer))
		{
			return false;
		}
		
		// normally this wouldn't really be needed, but for those few exceptions,
		// we do need to reset the weapons back to the initial template weapon.
		_currentLHandId = getTemplate().getLHandId();
		_currentRHandId = getTemplate().getRHandId();
		_currentCollisionHeight = getTemplate().getFCollisionHeight();
		_currentCollisionRadius = getTemplate().getFCollisionRadius();
		
		final Weapon weapon = (killer != null) ? killer.getActiveWeaponItem() : null;
		_killingBlowWeaponId = (weapon != null) ? weapon.getId() : 0;
		if (_isFakePlayer && (killer != null) && killer.isPlayable())
		{
			final Player player = killer.asPlayer();
			if (isScriptValue(0) && (getKarma() < 0))
			{
				if (FakePlayersConfig.FAKE_PLAYER_KILL_KARMA)
				{
					player.setKarma(player.getKarma() + Formulas.calculateKarmaGain(player.getPkKills(), killer.isSummon()));
					player.setPkKills(player.getPkKills() + 1);
					player.broadcastUserInfo();
					player.checkItemRestriction();
					
					// pk item rewards
					if (PvpRewardItemConfig.REWARD_PK_ITEM)
					{
						if (!(PvpRewardItemConfig.DISABLE_REWARDS_IN_INSTANCES && (getInstanceId() != 0)) && //
							!(PvpRewardItemConfig.DISABLE_REWARDS_IN_PVP_ZONES && isInsideZone(ZoneId.PVP)))
						{
							player.addItem(ItemProcessType.REWARD, PvpRewardItemConfig.REWARD_PK_ITEM_ID, PvpRewardItemConfig.REWARD_PK_ITEM_AMOUNT, this, PvpRewardItemConfig.REWARD_PK_ITEM_MESSAGE);
						}
					}
					
					// announce pk
					if (PvpAnnounceConfig.ANNOUNCE_PK_PVP && !player.isGM())
					{
						final String msg = PvpAnnounceConfig.ANNOUNCE_PK_MSG.replace("$killer", player.getName()).replace("$target", getName());
						if (PvpAnnounceConfig.ANNOUNCE_PK_PVP_NORMAL_MESSAGE)
						{
							final SystemMessage sm = new SystemMessage(SystemMessageId.S1_3);
							sm.addString(msg);
							Broadcast.toAllOnlinePlayers(sm);
						}
						else
						{
							Broadcast.toAllOnlinePlayers(msg, false);
						}
					}
				}
			}
			else if (FakePlayersConfig.FAKE_PLAYER_KILL_PVP)
			{
				player.setPvpKills(player.getPvpKills() + 1);
				player.broadcastUserInfo();
				
				// pvp item rewards
				if (PvpRewardItemConfig.REWARD_PVP_ITEM)
				{
					if (!(PvpRewardItemConfig.DISABLE_REWARDS_IN_INSTANCES && (getInstanceId() != 0)) && //
						!(PvpRewardItemConfig.DISABLE_REWARDS_IN_PVP_ZONES && isInsideZone(ZoneId.PVP)))
					{
						player.addItem(ItemProcessType.REWARD, PvpRewardItemConfig.REWARD_PVP_ITEM_ID, PvpRewardItemConfig.REWARD_PVP_ITEM_AMOUNT, this, PvpRewardItemConfig.REWARD_PVP_ITEM_MESSAGE);
					}
				}
				
				// announce pvp
				if (PvpAnnounceConfig.ANNOUNCE_PK_PVP && !player.isGM())
				{
					final String msg = PvpAnnounceConfig.ANNOUNCE_PVP_MSG.replace("$killer", player.getName()).replace("$target", getName());
					if (PvpAnnounceConfig.ANNOUNCE_PK_PVP_NORMAL_MESSAGE)
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.S1_3);
						sm.addString(msg);
						Broadcast.toAllOnlinePlayers(sm);
					}
					else
					{
						Broadcast.toAllOnlinePlayers(msg, false);
					}
				}
			}
		}
		
		DecayTaskManager.getInstance().add(this);
		return true;
	}
	
	/**
	 * Set the spawn of the Npc.
	 * @param spawn The Spawn that manage the Npc
	 */
	public void setSpawn(Spawn spawn)
	{
		_spawn = spawn;
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		
		// Recharge shots
		_soulshotamount = getTemplate().getSoulShot();
		_spiritshotamount = getTemplate().getSpiritShot();
		_killingBlowWeaponId = 0;
		_isRandomAnimationEnabled = getTemplate().isRandomAnimationEnabled();
		_isRandomWalkingEnabled = !WalkingManager.getInstance().isTargeted(this) && getTemplate().isRandomWalkEnabled();
		
		if (isTeleporting())
		{
			if (EventDispatcher.getInstance().hasListener(EventType.ON_NPC_TELEPORT, this))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnNpcTeleport(this), this);
			}
		}
		else if (EventDispatcher.getInstance().hasListener(EventType.ON_NPC_SPAWN, this))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnNpcSpawn(this), this);
		}
		
		if (!isTeleporting())
		{
			WalkingManager.getInstance().onSpawn(this);
		}
		
		if (CREATURE_SEE_IDS.contains(getId()))
		{
			initSeenCreatures();
		}
	}
	
	public static void addCreatureSeeId(int id)
	{
		CREATURE_SEE_IDS.add(id);
	}
	
	/**
	 * Remove the Npc from the world and update its spawn object (for a complete removal use the deleteMe method).<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Remove the Npc from the world when the decay task is launched</li>
	 * <li>Decrease its spawn counter</li>
	 * <li>Manage Siege task (killFlag, killCT)</li>
	 * </ul>
	 * <font color=#FF0000><b><u>Caution</u>: This method DOESN'T REMOVE the object from _allObjects of World </b></font><br>
	 * <font color=#FF0000><b><u>Caution</u>: This method DOESN'T SEND Server->Client packets to players</b></font>
	 */
	@Override
	public void onDecay()
	{
		if (_isDecayed)
		{
			return;
		}
		
		setDecayed(true);
		
		// Remove the Npc from the world when the decay task is launched
		super.onDecay();
		
		// Decrease its spawn counter
		if ((_spawn != null) && !RaidBossSpawnManager.getInstance().isDefined(getId()))
		{
			_spawn.decreaseCount(this);
		}
		
		// Notify Walking Manager
		WalkingManager.getInstance().onDeath(this);
		
		// Removes itself from the summoned list.
		final Creature summoner = getSummoner();
		if ((summoner != null) && summoner.isNpc())
		{
			summoner.asNpc().removeSummonedNpc(getObjectId());
		}
		
		// Stop all timers
		stopQuestTimers();
		stopTimerHolders();
		
		// Clear script value
		_scriptValue = 0;
	}
	
	/**
	 * Remove PROPERLY the Npc from the world.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Remove the Npc from the world and update its spawn object</li>
	 * <li>Remove all WorldObject from _knownObjects and _knownPlayer of the Npc then cancel Attack or Cast and notify AI</li>
	 * <li>Remove WorldObject object from _allObjects of World</li>
	 * </ul>
	 * <font color=#FF0000><b><u>Caution</u>: This method DOESN'T SEND Server->Client packets to players</b></font><br>
	 * UnAfraid: TODO: Add Listener here
	 */
	@Override
	public boolean deleteMe()
	{
		try
		{
			onDecay();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Failed decayMe().", e);
		}
		
		if (isChannelized())
		{
			getSkillChannelized().abortChannelization();
		}
		
		ZoneManager.getInstance().getRegion(this).removeFromZones(this);
		
		return super.deleteMe();
	}
	
	/**
	 * @return the Spawn object that manage this Npc.
	 */
	public Spawn getSpawn()
	{
		return _spawn;
	}
	
	public boolean isDecayed()
	{
		return _isDecayed;
	}
	
	public void setDecayed(boolean decayed)
	{
		_isDecayed = decayed;
	}
	
	public void endDecayTask()
	{
		if (!_isDecayed)
		{
			DecayTaskManager.getInstance().cancel(this);
			onDecay();
		}
	}
	
	// Two functions to change the appearance of the equipped weapons on the NPC
	// This is only useful for a few NPCs and is most likely going to be called from AI
	public void setLHandId(int newWeaponId)
	{
		_currentLHandId = newWeaponId;
		updateAbnormalEffect();
	}
	
	public void setRHandId(int newWeaponId)
	{
		_currentRHandId = newWeaponId;
		updateAbnormalEffect();
	}
	
	public void setLRHandId(int newLWeaponId, int newRWeaponId)
	{
		_currentRHandId = newRWeaponId;
		_currentLHandId = newLWeaponId;
		updateAbnormalEffect();
	}
	
	public void setEnchant(int newEnchantValue)
	{
		_currentEnchant = newEnchantValue;
		updateAbnormalEffect();
	}
	
	public boolean isShowName()
	{
		return getTemplate().isShowName();
	}
	
	@Override
	public boolean isTargetable()
	{
		return getTemplate().isTargetable();
	}
	
	public void setCollisionHeight(double height)
	{
		_currentCollisionHeight = height;
	}
	
	public void setCollisionRadius(double radius)
	{
		_currentCollisionRadius = radius;
	}
	
	public double getCollisionHeight()
	{
		return _currentCollisionHeight;
	}
	
	public double getCollisionRadius()
	{
		return _currentCollisionRadius;
	}
	
	@Override
	public void sendInfo(Player player)
	{
		if (isVisibleFor(player))
		{
			if (_isFakePlayer)
			{
				player.sendPacket(new FakePlayerInfo(this));
				
				// Private store message support.
				final FakePlayerHolder fakePlayerInfo = getTemplate().getFakePlayerInfo();
				final int storeType = (_fakePlayerAppearance != null) && (_fakePlayerAppearance.getPrivateStoreType() != 0) ? _fakePlayerAppearance.getPrivateStoreType() : fakePlayerInfo.getPrivateStoreType();
				if (storeType > 0)
				{
					final String message = (_fakePlayerAppearance != null) && (_fakePlayerAppearance.getPrivateStoreType() != 0) ? _fakePlayerAppearance.getStoreMessage() : fakePlayerInfo.getPrivateStoreMessage();
					if ((message != null) && !message.isEmpty())
					{
						switch (storeType)
						{
							case 1: // Sell
							{
								player.sendPacket(new PrivateStoreMsgSell(getObjectId(), message));
								break;
							}
							case 3: // Buy
							{
								player.sendPacket(new PrivateStoreMsgBuy(getObjectId(), message));
								break;
							}
							case 5: // Manufacture
							{
								player.sendPacket(new RecipeShopMsg(getObjectId(), message));
								break;
							}
							case 8: // Package Sell
							{
								// player.sendPacket(new ExPrivateStoreSetWholeMsg(getObjectId(), message));
								player.sendPacket(new PrivateStoreMsgSell(getObjectId(), message));
								break;
							}
						}
					}
				}
			}
			else if (getRunSpeed() == 0)
			{
				player.sendPacket(new ServerObjectInfo(this, player));
			}
			else
			{
				player.sendPacket(new AbstractNpcInfo.NpcInfo(this, player));
			}
		}
	}
	
	public void showNoTeachHtml(Player player)
	{
		final int npcId = getId();
		String html = "";
		if (this instanceof Warehouse)
		{
			html = HtmCache.getInstance().getHtm(player, "data/html/warehouse/" + npcId + "-noteach.htm");
		}
		else if (this instanceof Trainer)
		{
			html = HtmCache.getInstance().getHtm(player, "data/html/trainer/" + npcId + "-noteach.htm");
			
			// Trainer Healer?
			if (html == null)
			{
				html = HtmCache.getInstance().getHtm(player, "data/scripts/ai/others/HealerTrainer/" + npcId + "-noteach.html");
			}
		}
		
		final NpcHtmlMessage noTeachMsg = new NpcHtmlMessage(getObjectId());
		if (html == null)
		{
			LOGGER.warning("Npc " + npcId + " missing noTeach html!");
			noTeachMsg.setHtml("<html><body>I cannot teach you any skills.<br>You must find your current class teachers.</body></html>");
		}
		else
		{
			noTeachMsg.setHtml(html);
			noTeachMsg.replace("%objectId%", String.valueOf(getObjectId()));
		}
		
		player.sendPacket(noTeachMsg);
	}
	
	public Npc scheduleDespawn(long delay)
	{
		ThreadPool.schedule(() ->
		{
			if (!_isDecayed)
			{
				deleteMe();
			}
		}, delay);
		return this;
	}
	
	@Override
	protected void notifyQuestEventSkillFinished(Skill skill, WorldObject target)
	{
		if ((target != null) && target.isPlayable() && EventDispatcher.getInstance().hasListener(EventType.ON_NPC_SKILL_FINISHED, this))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnNpcSkillFinished(this, target.asPlayer(), skill), this);
		}
	}
	
	@Override
	public boolean isMovementDisabled()
	{
		return super.isMovementDisabled() || !canMove() || (getAiType() == AIType.CORPSE);
	}
	
	public AIType getAiType()
	{
		return getTemplate().getAIType();
	}
	
	public int getColorEffect()
	{
		return 0;
	}
	
	@Override
	public boolean isNpc()
	{
		return true;
	}
	
	@Override
	public Npc asNpc()
	{
		return this;
	}
	
	public void setTeam(Team team, boolean broadcast)
	{
		super.setTeam(team);
		if (broadcast)
		{
			broadcastInfo();
		}
	}
	
	@Override
	public void setTeam(Team team)
	{
		super.setTeam(team);
		broadcastInfo();
	}
	
	@Override
	public boolean isWalker()
	{
		return _isWalker;
	}
	
	public void setWalker()
	{
		_isWalker = true;
	}
	
	@Override
	public boolean isChargedShot(ShotType type)
	{
		return (_shotsMask & type.getMask()) == type.getMask();
	}
	
	@Override
	public void setChargedShot(ShotType type, boolean charged)
	{
		if (charged)
		{
			_shotsMask |= type.getMask();
		}
		else
		{
			_shotsMask &= ~type.getMask();
		}
	}
	
	@Override
	public void rechargeShots(boolean physical, boolean magic)
	{
		if (_isFakePlayer && FakePlayersConfig.FAKE_PLAYER_USE_SHOTS)
		{
			if (physical)
			{
				broadcastPacket(new MagicSkillUse(this, this, 2154, 1, 0, 0));
				setChargedShot(ShotType.SOULSHOTS, true);
			}
			
			if (magic)
			{
				broadcastPacket(new MagicSkillUse(this, this, 2159, 1, 0, 0));
				setChargedShot(ShotType.SPIRITSHOTS, true);
			}
		}
		else if ((_soulshotamount > 0) || (_spiritshotamount > 0))
		{
			if (physical)
			{
				if ((_soulshotamount == 0) || (Rnd.get(100) > getSoulShotChance()))
				{
					return;
				}
				
				_soulshotamount--;
				broadcastPacket(new MagicSkillUse(this, this, 2154, 1, 0, 0));
				setChargedShot(ShotType.SOULSHOTS, true);
			}
			
			if (magic)
			{
				if ((_spiritshotamount == 0) || (Rnd.get(100) > getSpiritShotChance()))
				{
					return;
				}
				
				_spiritshotamount--;
				broadcastPacket(new MagicSkillUse(this, this, 2159, 1, 0, 0));
				setChargedShot(ShotType.SPIRITSHOTS, true);
			}
		}
	}
	
	/**
	 * Receive the stored int value for this {@link Npc} instance.
	 * @return stored script value
	 */
	public int getScriptValue()
	{
		return _scriptValue;
	}
	
	/**
	 * Sets the script value related with this {@link Npc} instance.
	 * @param value value to store
	 */
	public void setScriptValue(int value)
	{
		_scriptValue = value;
	}
	
	/**
	 * @param value value to store
	 * @return {@code true} if stored script value equals given value, {@code false} otherwise
	 */
	public boolean isScriptValue(int value)
	{
		return _scriptValue == value;
	}
	
	/**
	 * @param paramName the parameter name to check
	 * @return given AI parameter value
	 */
	public int getAIValue(String paramName)
	{
		return hasAIValue(paramName) ? NpcPersonalAIData.getInstance().getAIValue(getSpawn().getName(), paramName) : -1;
	}
	
	/**
	 * @param paramName the parameter name to check
	 * @return {@code true} if given parameter is set for NPC, {@code false} otherwise
	 */
	public boolean hasAIValue(String paramName)
	{
		return (getSpawn() != null) && (getSpawn().getName() != null) && NpcPersonalAIData.getInstance().hasAIValue(getSpawn().getName(), paramName);
	}
	
	/**
	 * @param npc NPC to check
	 * @return {@code true} if both given NPC and this NPC is in the same spawn group, {@code false} otherwise
	 */
	public boolean isInMySpawnGroup(Npc npc)
	{
		return ((getSpawn() != null) && (npc.getSpawn() != null) && (getSpawn().getName() != null) && (getSpawn().getName().equals(npc.getSpawn().getName())));
	}
	
	/**
	 * @return {@code true} if NPC currently located in own spawn point, {@code false} otherwise
	 */
	public boolean staysInSpawnLoc()
	{
		return ((_spawn != null) && (_spawn.getX() == getX()) && (_spawn.getY() == getY()));
	}
	
	/**
	 * @return {@code true} if {@link NpcVariables} instance is attached to current player's scripts, {@code false} otherwise.
	 */
	public boolean hasVariables()
	{
		return getScript(NpcVariables.class) != null;
	}
	
	/**
	 * @return {@link NpcVariables} instance containing parameters regarding NPC.
	 */
	public NpcVariables getVariables()
	{
		final NpcVariables vars = getScript(NpcVariables.class);
		return vars != null ? vars : addScript(new NpcVariables());
	}
	
	/**
	 * Send an "event" to all NPCs within given radius
	 * @param eventName - name of event
	 * @param radius - radius to send event
	 * @param reference - WorldObject to pass, if needed
	 */
	public void broadcastEvent(String eventName, int radius, WorldObject reference)
	{
		World.getInstance().forEachVisibleObjectInRange(this, Npc.class, radius, obj ->
		{
			if (obj.hasListener(EventType.ON_NPC_EVENT_RECEIVED))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnNpcEventReceived(eventName, this, obj, reference), obj);
			}
		});
	}
	
	/**
	 * Sends an event to a given object.
	 * @param eventName the event name
	 * @param receiver the receiver
	 * @param reference the reference
	 */
	public void sendScriptEvent(String eventName, WorldObject receiver, WorldObject reference)
	{
		if (EventDispatcher.getInstance().hasListener(EventType.ON_NPC_EVENT_RECEIVED, receiver))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnNpcEventReceived(eventName, this, receiver.asNpc(), reference), receiver);
		}
	}
	
	/**
	 * Gets point in range between radiusMin and radiusMax from this NPC
	 * @param radiusMin miminal range from NPC (not closer than)
	 * @param radiusMax maximal range from NPC (not further than)
	 * @return Location in given range from this NPC
	 */
	public Location getPointInRange(int radiusMin, int radiusMax)
	{
		if ((radiusMax == 0) || (radiusMax < radiusMin))
		{
			return new Location(getX(), getY(), getZ());
		}
		
		final int radius = Rnd.get(radiusMin, radiusMax);
		final double angle = Rnd.nextDouble() * 2 * Math.PI;
		return new Location((int) (getX() + (radius * Math.cos(angle))), (int) (getY() + (radius * Math.sin(angle))), getZ());
	}
	
	/**
	 * Drops an item.
	 * @param creature the last attacker or main damage dealer
	 * @param itemId the item ID
	 * @param itemCount the item count
	 * @return the dropped item
	 */
	public Item dropItem(Creature creature, int itemId, int itemCount)
	{
		Item item = null;
		for (int i = 0; i < itemCount; i++)
		{
			if (ItemData.getInstance().getTemplate(itemId) == null)
			{
				LOGGER.log(Level.SEVERE, "Item doesn't exist so cannot be dropped. Item ID: " + itemId + " Quest: " + getName());
				return null;
			}
			
			item = ItemManager.createItem(ItemProcessType.LOOT, itemId, itemCount, creature, this);
			if (item == null)
			{
				return null;
			}
			
			if (creature != null)
			{
				item.getDropProtection().protect(creature);
			}
			
			// Randomize drop position.
			final int newX = (getX() + Rnd.get((RANDOM_ITEM_DROP_LIMIT * 2) + 1)) - RANDOM_ITEM_DROP_LIMIT;
			final int newY = (getY() + Rnd.get((RANDOM_ITEM_DROP_LIMIT * 2) + 1)) - RANDOM_ITEM_DROP_LIMIT;
			final int newZ = getZ() + 20;
			
			item.dropMe(this, newX, newY, newZ);
			
			// Add drop to auto destroy item task.
			if (!GeneralConfig.LIST_PROTECTED_ITEMS.contains(itemId) && (((GeneralConfig.AUTODESTROY_ITEM_AFTER > 0) && !item.getTemplate().hasExImmediateEffect()) || ((GeneralConfig.HERB_AUTO_DESTROY_TIME > 0) && item.getTemplate().hasExImmediateEffect())))
			{
				ItemsAutoDestroyTaskManager.getInstance().addItem(item);
			}
			
			item.setProtected(false);
			
			// If stackable, end loop as entire count is included in 1 instance of item.
			if (item.isStackable() || !GeneralConfig.MULTIPLE_ITEM_DROP)
			{
				break;
			}
		}
		
		return item;
	}
	
	/**
	 * Method overload for {@link Attackable#dropItem(Creature, int, int)}
	 * @param creature the last attacker or main damage dealer
	 * @param item the item holder
	 * @return the dropped item
	 */
	public Item dropItem(Creature creature, ItemHolder item)
	{
		return dropItem(creature, item.getId(), item.getCount());
	}
	
	@Override
	public String getName()
	{
		return _fakePlayerAppearance != null ? _fakePlayerAppearance.getName() : getTemplate().getName();
	}

	/**
	 * @return the per-instance fake player appearance, or {@code null} if this NPC uses its template's look
	 */
	public FakePlayerAppearance getFakePlayerAppearance()
	{
		return _fakePlayerAppearance;
	}

	/**
	 * Attaches a procedurally generated appearance to this fake player. The {@code FakePlayerInfo}
	 * packet will then render the NPC using this appearance instead of its template.
	 * @param appearance the appearance to use
	 */
	public void setFakePlayerAppearance(FakePlayerAppearance appearance)
	{
		_fakePlayerAppearance = appearance;
	}
	
	@Override
	public boolean isVisibleFor(Player player)
	{
		if (hasListener(EventType.ON_NPC_CAN_BE_SEEN))
		{
			final TerminateReturn term = EventDispatcher.getInstance().notifyEvent(new OnNpcCanBeSeen(this, player), this, TerminateReturn.class);
			if (term != null)
			{
				return term.terminate();
			}
		}
		
		return super.isVisibleFor(player);
	}
	
	/**
	 * Sets if the players can talk with this npc or not
	 * @param value {@code true} if the players can talk, {@code false} otherwise
	 */
	public void setTalkable(boolean value)
	{
		_isTalkable = value;
	}
	
	/**
	 * Checks if the players can talk to this npc.
	 * @return {@code true} if the players can talk, {@code false} otherwise.
	 */
	public boolean isTalkable()
	{
		return _isTalkable;
	}
	
	/**
	 * Checks if the NPC is a Quest Monster.
	 * @return {@code true} if the NPC is a Quest Monster, {@code false} otherwise.
	 */
	public boolean isQuestMonster()
	{
		return _isQuestMonster;
	}
	
	/**
	 * Sets the weapon id with which this npc was killed.
	 * @param weaponId
	 */
	public void setKillingBlowWeapon(int weaponId)
	{
		_killingBlowWeaponId = weaponId;
	}
	
	/**
	 * @return the id of the weapon with which player killed this npc.
	 */
	public int getKillingBlowWeapon()
	{
		return _killingBlowWeaponId;
	}
	
	@Override
	public boolean isFakePlayer()
	{
		return _isFakePlayer;
	}
	
	/**
	 * Adds a summoned NPC.
	 * @param npc the summoned NPC
	 */
	public void addSummonedNpc(Npc npc)
	{
		if (_summonedNpcs == null)
		{
			synchronized (this)
			{
				if (_summonedNpcs == null)
				{
					_summonedNpcs = new ConcurrentHashMap<>();
				}
			}
		}
		
		_summonedNpcs.put(npc.getObjectId(), npc);
		
		npc.setSummoner(this);
	}
	
	/**
	 * Removes a summoned NPC by object ID.
	 * @param objectId the summoned NPC object ID
	 */
	public void removeSummonedNpc(int objectId)
	{
		if (_summonedNpcs != null)
		{
			_summonedNpcs.remove(objectId);
		}
	}
	
	/**
	 * Gets the summoned NPCs.
	 * @return the summoned NPCs
	 */
	public Collection<Npc> getSummonedNpcs()
	{
		return _summonedNpcs != null ? _summonedNpcs.values() : Collections.<Npc> emptyList();
	}
	
	/**
	 * Gets the summoned NPC by object ID.
	 * @param objectId the summoned NPC object ID
	 * @return the summoned NPC
	 */
	public Npc getSummonedNpc(int objectId)
	{
		if (_summonedNpcs != null)
		{
			return _summonedNpcs.get(objectId);
		}
		
		return null;
	}
	
	/**
	 * Gets the summoned NPC count.
	 * @return the summoned NPC count
	 */
	public int getSummonedNpcCount()
	{
		return _summonedNpcs != null ? _summonedNpcs.size() : 0;
	}
	
	/**
	 * Resets the summoned NPCs list.
	 */
	public void resetSummonedNpcs()
	{
		if (_summonedNpcs != null)
		{
			_summonedNpcs.clear();
		}
	}
	
	/**
	 * Broadcasts NpcSay packet to all known players.
	 * @param chatType the chat type
	 * @param text the text
	 */
	public void broadcastSay(ChatType chatType, String text)
	{
		Broadcast.toKnownPlayers(this, new NpcSay(this, chatType, text));
	}
	
	/**
	 * Broadcasts NpcSay packet to all known players with custom string in specific radius.
	 * @param chatType the chat type
	 * @param text the text
	 * @param radius the radius
	 */
	public void broadcastSay(ChatType chatType, String text, int radius)
	{
		Broadcast.toKnownPlayersInRadius(this, new NpcSay(this, chatType, text), radius);
	}
	
	@Override
	public int getMinShopDistance()
	{
		return PrivateStoreRangeConfig.SHOP_MIN_RANGE_FROM_NPC;
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
	
	public void stopQuestTimers()
	{
		synchronized (_questTimers)
		{
			for (QuestTimer timer : _questTimers)
			{
				timer.cancelTask();
			}
			
			_questTimers.clear();
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
	
	public void stopTimerHolders()
	{
		synchronized (_timerHolders)
		{
			for (TimerHolder<?> timer : _timerHolders)
			{
				timer.cancelTask();
			}
			
			_timerHolders.clear();
		}
	}
	
	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder();
		sb.append(getClass().getSimpleName());
		sb.append(":");
		sb.append(getName());
		sb.append("(");
		sb.append(getId());
		sb.append(")[");
		sb.append(getObjectId());
		sb.append("]");
		return sb.toString();
	}
}
