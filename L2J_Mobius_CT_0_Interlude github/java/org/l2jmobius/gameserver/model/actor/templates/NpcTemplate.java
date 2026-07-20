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
package org.l2jmobius.gameserver.model.actor.templates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.config.custom.ChampionMonstersConfig;
import org.l2jmobius.gameserver.config.custom.NpcStatMultipliersConfig;
import org.l2jmobius.gameserver.config.custom.PremiumSystemConfig;
import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.actor.enums.npc.AISkillScope;
import org.l2jmobius.gameserver.model.actor.enums.npc.AIType;
import org.l2jmobius.gameserver.model.actor.enums.npc.DropType;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.actor.enums.player.Sex;
import org.l2jmobius.gameserver.model.actor.holders.npc.DropGroupHolder;
import org.l2jmobius.gameserver.model.actor.holders.npc.DropHolder;
import org.l2jmobius.gameserver.model.actor.holders.npc.FakePlayerHolder;
import org.l2jmobius.gameserver.model.actor.stat.PlayerStat;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.util.MathUtil;

/**
 * @author NosBit, Nobius
 */
public class NpcTemplate extends CreatureTemplate
{
	private static final Logger LOGGER = Logger.getLogger(NpcTemplate.class.getName());
	
	private int _id;
	private int _displayId;
	private byte _level;
	private String _type;
	private String _name;
	private boolean _usingServerSideName;
	private String _title;
	private boolean _usingServerSideTitle;
	private StatSet _parameters;
	private Sex _sex;
	private int _chestId;
	private int _rhandId;
	private int _lhandId;
	private int _weaponEnchant;
	private double _exp;
	private double _sp;
	private boolean _unique;
	private boolean _attackable;
	private boolean _targetable;
	private boolean _talkable;
	private boolean _isQuestMonster;
	private boolean _undying;
	private boolean _showName;
	private boolean _randomWalk;
	private boolean _randomAnimation;
	private boolean _flying;
	private boolean _fakePlayer;
	private FakePlayerHolder _fakePlayerInfo;
	private boolean _canMove;
	private boolean _noSleepMode;
	private boolean _passableDoor;
	private boolean _hasSummoner;
	private boolean _canBeSown;
	private int _corpseTime;
	private AIType _aiType;
	private int _aggroRange;
	private int _clanHelpRange;
	private boolean _isChaos;
	private boolean _isAggressive;
	private int _soulShot;
	private int _spiritShot;
	private int _soulShotChance;
	private int _spiritShotChance;
	private int _minSkillChance;
	private int _maxSkillChance;
	private int _baseAttackAngle;
	private Map<Integer, Skill> _skills;
	private Map<AISkillScope, List<Skill>> _aiSkillLists;
	private Set<Integer> _clans;
	private Set<Integer> _ignoreClanNpcIds;
	private List<DropGroupHolder> _dropGroups;
	private List<DropHolder> _dropListDeath;
	private List<DropHolder> _dropListSpoil;
	private double _collisionRadiusGrown;
	private double _collisionHeightGrown;
	
	private final List<PlayerClass> _teachInfo = new ArrayList<>();
	
	/**
	 * Constructor of Creature.
	 * @param set The StatSet object to transfer data to the method
	 */
	public NpcTemplate(StatSet set)
	{
		super(set);
	}
	
	@Override
	public void set(StatSet set)
	{
		super.set(set);
		_id = set.getInt("id");
		_displayId = set.getInt("displayId", _id);
		_level = set.getByte("level", (byte) 70);
		_type = set.getString("type", "Folk");
		_name = set.getString("name", "");
		_usingServerSideName = set.getBoolean("usingServerSideName", false);
		_title = set.getString("title", "");
		_usingServerSideTitle = set.getBoolean("usingServerSideTitle", false);
		setRace(set.getEnum("race", Race.class, Race.NONE));
		_sex = set.getEnum("sex", Sex.class, Sex.ETC);
		_chestId = set.getInt("chestId", 0);
		_rhandId = set.getInt("rhandId", 0);
		_lhandId = set.getInt("lhandId", 0);
		_weaponEnchant = set.getInt("weaponEnchant", 0);
		_exp = set.getDouble("exp", 0);
		_sp = set.getDouble("sp", 0);
		_unique = set.getBoolean("unique", false);
		_attackable = set.getBoolean("attackable", true);
		_targetable = set.getBoolean("targetable", true);
		_talkable = set.getBoolean("talkable", true);
		_isQuestMonster = _title.contains("Quest");
		_undying = set.getBoolean("undying", !_type.equals("Monster") && !_type.equals("RaidBoss") && !_type.equals("GrandBoss"));
		_showName = set.getBoolean("showName", true);
		_randomWalk = set.getBoolean("randomWalk", !_type.equals("Guard"));
		_randomAnimation = set.getBoolean("randomAnimation", true);
		_flying = set.getBoolean("flying", false);
		_fakePlayer = set.getBoolean("fakePlayer", false);
		if (_fakePlayer)
		{
			_fakePlayerInfo = new FakePlayerHolder(set);
			
			// Check if a character with the same name already exists.
			if (CharInfoTable.getInstance().getIdByName(_name) > 0)
			{
				LOGGER.info(getClass().getSimpleName() + ": Fake player id [" + _id + "] conflict. A real player with name [" + _name + "] already exists.");
			}
		}
		
		_canMove = (set.getDouble("baseWalkSpd", 1d) <= 0.1) || set.getBoolean("canMove", true);
		_noSleepMode = set.getBoolean("noSleepMode", false);
		_passableDoor = set.getBoolean("passableDoor", false);
		_hasSummoner = set.getBoolean("hasSummoner", false);
		_canBeSown = set.getBoolean("canBeSown", false);
		_corpseTime = set.getInt("corpseTime", NpcConfig.DEFAULT_CORPSE_TIME);
		_aiType = set.getEnum("aiType", AIType.class, AIType.FIGHTER);
		
		// L2J aggro range tempfix
		final int aggroValue = set.getInt("aggroRange", 0);
		if (_type.equalsIgnoreCase("Monster") && (aggroValue > NpcConfig.MAX_AGGRO_RANGE))
		{
			_aggroRange = NpcConfig.MAX_AGGRO_RANGE;
		}
		else
		{
			_aggroRange = aggroValue;
		}
		
		_clanHelpRange = set.getInt("clanHelpRange", 0);
		_isChaos = set.getBoolean("isChaos", false);
		_isAggressive = set.getBoolean("isAggressive", true);
		_soulShot = set.getInt("soulShot", 0);
		_spiritShot = set.getInt("spiritShot", 0);
		_soulShotChance = set.getInt("soulShotChance", 0);
		_spiritShotChance = set.getInt("spiritShotChance", 0);
		_minSkillChance = set.getInt("minSkillChance", 7);
		_maxSkillChance = set.getInt("maxSkillChance", 15);
		_baseAttackAngle = set.getInt("width", 120);
		_collisionRadiusGrown = set.getDouble("collisionRadiusGrown", 0);
		_collisionHeightGrown = set.getDouble("collisionHeightGrown", 0);
		if (NpcStatMultipliersConfig.ENABLE_NPC_STAT_MULTIPLIERS) // Custom NPC Stat Multipliers
		{
			switch (_type)
			{
				case "Monster":
				{
					_baseHpMax *= NpcStatMultipliersConfig.MONSTER_HP_MULTIPLIER;
					_baseMpMax *= NpcStatMultipliersConfig.MONSTER_MP_MULTIPLIER;
					_basePAtk *= NpcStatMultipliersConfig.MONSTER_PATK_MULTIPLIER;
					_baseMAtk *= NpcStatMultipliersConfig.MONSTER_MATK_MULTIPLIER;
					_basePDef *= NpcStatMultipliersConfig.MONSTER_PDEF_MULTIPLIER;
					_baseMDef *= NpcStatMultipliersConfig.MONSTER_MDEF_MULTIPLIER;
					_aggroRange *= NpcStatMultipliersConfig.MONSTER_AGRRO_RANGE_MULTIPLIER;
					_clanHelpRange *= NpcStatMultipliersConfig.MONSTER_CLAN_HELP_RANGE_MULTIPLIER;
					break;
				}
				case "RaidBoss":
				case "GrandBoss":
				{
					_baseHpMax *= NpcStatMultipliersConfig.RAIDBOSS_HP_MULTIPLIER;
					_baseMpMax *= NpcStatMultipliersConfig.RAIDBOSS_MP_MULTIPLIER;
					_basePAtk *= NpcStatMultipliersConfig.RAIDBOSS_PATK_MULTIPLIER;
					_baseMAtk *= NpcStatMultipliersConfig.RAIDBOSS_MATK_MULTIPLIER;
					_basePDef *= NpcStatMultipliersConfig.RAIDBOSS_PDEF_MULTIPLIER;
					_baseMDef *= NpcStatMultipliersConfig.RAIDBOSS_MDEF_MULTIPLIER;
					_aggroRange *= NpcStatMultipliersConfig.RAIDBOSS_AGRRO_RANGE_MULTIPLIER;
					_clanHelpRange *= NpcStatMultipliersConfig.RAIDBOSS_CLAN_HELP_RANGE_MULTIPLIER;
					break;
				}
				case "Guard":
				{
					_baseHpMax *= NpcStatMultipliersConfig.GUARD_HP_MULTIPLIER;
					_baseMpMax *= NpcStatMultipliersConfig.GUARD_MP_MULTIPLIER;
					_basePAtk *= NpcStatMultipliersConfig.GUARD_PATK_MULTIPLIER;
					_baseMAtk *= NpcStatMultipliersConfig.GUARD_MATK_MULTIPLIER;
					_basePDef *= NpcStatMultipliersConfig.GUARD_PDEF_MULTIPLIER;
					_baseMDef *= NpcStatMultipliersConfig.GUARD_MDEF_MULTIPLIER;
					_aggroRange *= NpcStatMultipliersConfig.GUARD_AGRRO_RANGE_MULTIPLIER;
					_clanHelpRange *= NpcStatMultipliersConfig.GUARD_CLAN_HELP_RANGE_MULTIPLIER;
					break;
				}
				case "Defender":
				{
					_baseHpMax *= NpcStatMultipliersConfig.DEFENDER_HP_MULTIPLIER;
					_baseMpMax *= NpcStatMultipliersConfig.DEFENDER_MP_MULTIPLIER;
					_basePAtk *= NpcStatMultipliersConfig.DEFENDER_PATK_MULTIPLIER;
					_baseMAtk *= NpcStatMultipliersConfig.DEFENDER_MATK_MULTIPLIER;
					_basePDef *= NpcStatMultipliersConfig.DEFENDER_PDEF_MULTIPLIER;
					_baseMDef *= NpcStatMultipliersConfig.DEFENDER_MDEF_MULTIPLIER;
					_aggroRange *= NpcStatMultipliersConfig.DEFENDER_AGRRO_RANGE_MULTIPLIER;
					_clanHelpRange *= NpcStatMultipliersConfig.DEFENDER_CLAN_HELP_RANGE_MULTIPLIER;
					break;
				}
			}
		}
	}
	
	public int getId()
	{
		return _id;
	}
	
	public int getDisplayId()
	{
		return _displayId;
	}
	
	public byte getLevel()
	{
		return _level;
	}
	
	public String getType()
	{
		return _type;
	}
	
	public boolean isType(String type)
	{
		return _type.equalsIgnoreCase(type);
	}
	
	public String getName()
	{
		return _name;
	}
	
	public boolean isUsingServerSideName()
	{
		return _usingServerSideName;
	}
	
	public String getTitle()
	{
		return _title;
	}
	
	public boolean isUsingServerSideTitle()
	{
		return _usingServerSideTitle;
	}
	
	public StatSet getParameters()
	{
		return _parameters;
	}
	
	public void setParameters(StatSet set)
	{
		_parameters = set;
	}
	
	public Sex getSex()
	{
		return _sex;
	}
	
	public int getChestId()
	{
		return _chestId;
	}
	
	public int getRHandId()
	{
		return _rhandId;
	}
	
	public int getLHandId()
	{
		return _lhandId;
	}
	
	public int getWeaponEnchant()
	{
		return _weaponEnchant;
	}
	
	public double getExp()
	{
		return _exp;
	}
	
	public double getSP()
	{
		return _sp;
	}
	
	public boolean isUnique()
	{
		return _unique;
	}
	
	public boolean isAttackable()
	{
		return _attackable;
	}
	
	public boolean isTargetable()
	{
		return _targetable;
	}
	
	public boolean isTalkable()
	{
		return _talkable;
	}
	
	public boolean isQuestMonster()
	{
		return _isQuestMonster;
	}
	
	public boolean isUndying()
	{
		return _undying;
	}
	
	public boolean isShowName()
	{
		return _showName;
	}
	
	public boolean isRandomWalkEnabled()
	{
		return _randomWalk;
	}
	
	public boolean isRandomAnimationEnabled()
	{
		return _randomAnimation;
	}
	
	public boolean isFlying()
	{
		return _flying;
	}
	
	public boolean isFakePlayer()
	{
		return _fakePlayer;
	}
	
	public FakePlayerHolder getFakePlayerInfo()
	{
		return _fakePlayerInfo;
	}
	
	public boolean canMove()
	{
		return _canMove;
	}
	
	public boolean isNoSleepMode()
	{
		return _noSleepMode;
	}
	
	public boolean isPassableDoor()
	{
		return _passableDoor;
	}
	
	public boolean hasSummoner()
	{
		return _hasSummoner;
	}
	
	public boolean canBeSown()
	{
		return _canBeSown;
	}
	
	public int getCorpseTime()
	{
		return _corpseTime;
	}
	
	public AIType getAIType()
	{
		return _aiType;
	}
	
	public int getAggroRange()
	{
		return _aggroRange;
	}
	
	public int getClanHelpRange()
	{
		return _clanHelpRange;
	}
	
	public boolean isChaos()
	{
		return _isChaos;
	}
	
	public boolean isAggressive()
	{
		return _isAggressive;
	}
	
	public int getSoulShot()
	{
		return _soulShot;
	}
	
	public int getSpiritShot()
	{
		return _spiritShot;
	}
	
	public int getSoulShotChance()
	{
		return _soulShotChance;
	}
	
	public int getSpiritShotChance()
	{
		return _spiritShotChance;
	}
	
	public int getMinSkillChance()
	{
		return _minSkillChance;
	}
	
	public int getMaxSkillChance()
	{
		return _maxSkillChance;
	}
	
	public int getBaseAttackAngle()
	{
		return _baseAttackAngle;
	}
	
	@Override
	public Map<Integer, Skill> getSkills()
	{
		return _skills;
	}
	
	public void setSkills(Map<Integer, Skill> skills)
	{
		_skills = skills != null ? Collections.unmodifiableMap(skills) : Collections.emptyMap();
	}
	
	public List<Skill> getAISkills(AISkillScope aiSkillScope)
	{
		return _aiSkillLists.getOrDefault(aiSkillScope, Collections.emptyList());
	}
	
	public void setAISkillLists(Map<AISkillScope, List<Skill>> aiSkillLists)
	{
		_aiSkillLists = aiSkillLists != null ? Collections.unmodifiableMap(aiSkillLists) : Collections.emptyMap();
	}
	
	public Set<Integer> getClans()
	{
		return _clans;
	}
	
	/**
	 * @param clans A sorted array of clan ids
	 */
	public void setClans(Set<Integer> clans)
	{
		_clans = clans != null ? Collections.unmodifiableSet(clans) : null;
	}
	
	/**
	 * @param clanName clan name to check if it belongs to this NPC template clans.
	 * @param clanNames clan names to check if they belong to this NPC template clans.
	 * @return {@code true} if at least one of the clan names belong to this NPC template clans, {@code false} otherwise.
	 */
	public boolean isClan(String clanName, String... clanNames)
	{
		// Using local variable for the sake of reloading since it can be turned to null.
		final Set<Integer> clans = _clans;
		if (clans == null)
		{
			return false;
		}
		
		int clanId = NpcData.getInstance().getGenericClanId();
		if (clans.contains(clanId))
		{
			return true;
		}
		
		clanId = NpcData.getInstance().getClanId(clanName);
		if (clans.contains(clanId))
		{
			return true;
		}
		
		for (String name : clanNames)
		{
			clanId = NpcData.getInstance().getClanId(name);
			if (clans.contains(clanId))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * @param clans A set of clan names to check if they belong to this NPC template clans.
	 * @return {@code true} if at least one of the clan names belong to this NPC template clans, {@code false} otherwise.
	 */
	public boolean isClan(Set<Integer> clans)
	{
		// Using local variable for the sake of reloading since it can be turned to null.
		final Set<Integer> clanSet = _clans;
		if ((clanSet == null) || (clans == null))
		{
			return false;
		}
		
		final int clanId = NpcData.getInstance().getGenericClanId();
		if (clanSet.contains(clanId))
		{
			return true;
		}
		
		for (Integer id : clans)
		{
			if (clanSet.contains(id))
			{
				return true;
			}
		}
		
		return false;
	}
	
	public Set<Integer> getIgnoreClanNpcIds()
	{
		return _ignoreClanNpcIds;
	}
	
	public boolean hasIgnoreClanNpcIds()
	{
		return _ignoreClanNpcIds != null;
	}
	
	/**
	 * @param ignoreClanNpcIds the ignore clan npc ids
	 */
	public void setIgnoreClanNpcIds(Set<Integer> ignoreClanNpcIds)
	{
		_ignoreClanNpcIds = ignoreClanNpcIds != null ? Collections.unmodifiableSet(ignoreClanNpcIds) : null;
	}
	
	public void removeDropGroups()
	{
		_dropGroups = null;
	}
	
	public void removeDrops()
	{
		_dropListDeath = null;
		_dropListSpoil = null;
	}
	
	public void setDropGroups(List<DropGroupHolder> groups)
	{
		_dropGroups = groups;
	}
	
	public void addDrop(DropHolder dropHolder)
	{
		if (_dropListDeath == null)
		{
			_dropListDeath = new ArrayList<>(1);
		}
		
		_dropListDeath.add(dropHolder);
	}
	
	public void addSpoil(DropHolder dropHolder)
	{
		if (_dropListSpoil == null)
		{
			_dropListSpoil = new ArrayList<>(1);
		}
		
		_dropListSpoil.add(dropHolder);
	}
	
	public List<DropGroupHolder> getDropGroups()
	{
		return _dropGroups;
	}
	
	public List<DropHolder> getDropList()
	{
		return _dropListDeath;
	}
	
	public List<DropHolder> getSpoilList()
	{
		return _dropListSpoil;
	}
	
	public List<ItemHolder> calculateDrops(DropType dropType, Creature victim, Creature killer)
	{
		if (dropType == DropType.DROP)
		{
			// calculate group drops
			List<ItemHolder> groupDrops = null;
			if (_dropGroups != null)
			{
				groupDrops = calculateGroupDrops(victim, killer);
				
				if ((groupDrops != null) && victim.isMonster() && victim.asMonster().isSeeded())
				{
					groupDrops.removeIf(i -> (i.getId() != 57 /* Adena */) && (i.getId() != 6361 /* Green Seal Stone */) && (i.getId() != 6362 /* Red Seal Stone */) && (i.getId() != 6360 /* Blue Seal Stone */));
				}
			}
			
			// calculate ungrouped drops
			List<ItemHolder> ungroupedDrops = null;
			if (_dropListDeath != null)
			{
				ungroupedDrops = calculateUngroupedDrops(dropType, victim, killer);
				
				if ((ungroupedDrops != null) && victim.isMonster() && victim.asMonster().isSeeded())
				{
					ungroupedDrops.removeIf(i -> (i.getId() != 57 /* Adena */) && (i.getId() != 6361 /* Green Seal Stone */) && (i.getId() != 6362 /* Red Seal Stone */) && (i.getId() != 6360 /* Blue Seal Stone */));
				}
			}
			
			// return results
			if ((groupDrops != null) && (ungroupedDrops != null))
			{
				groupDrops.addAll(ungroupedDrops);
				ungroupedDrops.clear();
				return groupDrops;
			}
			
			if (groupDrops != null)
			{
				return groupDrops;
			}
			
			if (ungroupedDrops != null)
			{
				return ungroupedDrops;
			}
		}
		else if ((dropType == DropType.SPOIL) && (_dropListSpoil != null))
		{
			return calculateUngroupedDrops(dropType, victim, killer);
		}
		
		// no drops
		return null;
	}
	
	private List<ItemHolder> calculateGroupDrops(Creature victim, Creature killer)
	{
		// level difference calculations
		final int levelDifference = victim.getLevel() - killer.getLevel();
		final double levelGapChanceToDropAdena = MathUtil.scaleToRange(levelDifference, -RatesConfig.DROP_ADENA_MAX_LEVEL_DIFFERENCE, -RatesConfig.DROP_ADENA_MIN_LEVEL_DIFFERENCE, RatesConfig.DROP_ADENA_MIN_LEVEL_GAP_CHANCE, 100d);
		final double levelGapChanceToDrop = MathUtil.scaleToRange(levelDifference, -RatesConfig.DROP_ITEM_MAX_LEVEL_DIFFERENCE, -RatesConfig.DROP_ITEM_MIN_LEVEL_DIFFERENCE, RatesConfig.DROP_ITEM_MIN_LEVEL_GAP_CHANCE, 100d);
		
		List<ItemHolder> calculatedDrops = null;
		int dropOccurrenceCounter = victim.isRaid() ? RatesConfig.DROP_MAX_OCCURRENCES_RAIDBOSS : RatesConfig.DROP_MAX_OCCURRENCES_NORMAL;
		if (dropOccurrenceCounter > 0)
		{
			final Player player = killer.asPlayer();
			List<ItemHolder> randomDrops = null;
			ItemHolder cachedItem = null;
			double totalChance; // total group chance is 100
			for (DropGroupHolder group : _dropGroups)
			{
				totalChance = 0;
				GROUP_DROP: for (DropHolder dropItem : group.getDropList())
				{
					final int itemId = dropItem.getItemId();
					final ItemTemplate item = ItemData.getInstance().getTemplate(itemId);
					final boolean champion = victim.isChampion();
					
					// chance
					double rateChance = 1;
					if (RatesConfig.RATE_DROP_CHANCE_BY_ID.get(itemId) != null)
					{
						rateChance *= RatesConfig.RATE_DROP_CHANCE_BY_ID.get(itemId);
						if (champion && (itemId == Inventory.ADENA_ID))
						{
							rateChance *= ChampionMonstersConfig.CHAMPION_ADENAS_REWARDS_CHANCE;
						}
						
						if ((itemId == Inventory.ADENA_ID) && (rateChance > 100))
						{
							rateChance = 100;
						}
					}
					else if (item.hasExImmediateEffect())
					{
						rateChance *= RatesConfig.RATE_HERB_DROP_CHANCE_MULTIPLIER;
					}
					else if (victim.isRaid())
					{
						rateChance *= RatesConfig.RATE_RAID_DROP_CHANCE_MULTIPLIER;
					}
					else
					{
						rateChance *= RatesConfig.RATE_DEATH_DROP_CHANCE_MULTIPLIER * (champion ? ChampionMonstersConfig.CHAMPION_REWARDS_CHANCE : 1);
					}
					
					// premium chance
					if (player != null)
					{
						if (PremiumSystemConfig.PREMIUM_SYSTEM_ENABLED && player.hasPremiumStatus())
						{
							if (PremiumSystemConfig.PREMIUM_RATE_DROP_CHANCE_BY_ID.get(itemId) != null)
							{
								rateChance *= PremiumSystemConfig.PREMIUM_RATE_DROP_CHANCE_BY_ID.get(itemId);
							}
							else if (item.hasExImmediateEffect())
							{
								// TODO: Premium herb chance? :)
							}
							else if (victim.isRaid())
							{
								// TODO: Premium raid chance? :)
							}
							else
							{
								rateChance *= PremiumSystemConfig.PREMIUM_RATE_DROP_CHANCE;
							}
						}
						
						// bonus drop rate effect
						rateChance *= player.getStat().getBonusDropRateMultiplier();
					}
					
					// only use total chance on x1, custom rates break this logic because total chance is more than 100%
					if (rateChance == 1)
					{
						totalChance += dropItem.getChance();
					}
					else
					{
						totalChance = dropItem.getChance();
					}
					
					final double groupItemChance = totalChance * (group.getChance() / 100) * rateChance;
					
					// check if maximum drop occurrences have been reached
					// items that have 100% drop chance without server rate multipliers drop normally
					if ((dropOccurrenceCounter == 0) && (groupItemChance < 100) && (randomDrops != null) && (calculatedDrops != null))
					{
						if ((rateChance == 1) && !randomDrops.isEmpty()) // custom rates break this logic because total chance is more than 100%
						{
							// remove highest chance item (temporarily if no other item replaces it)
							cachedItem = randomDrops.remove(0);
							calculatedDrops.remove(cachedItem);
						}
						
						dropOccurrenceCounter = 1;
					}
					
					// check level gap that may prevent to drop item
					if ((Rnd.nextDouble() * 100) > (dropItem.getItemId() == Inventory.ADENA_ID ? levelGapChanceToDropAdena : levelGapChanceToDrop))
					{
						continue GROUP_DROP;
					}
					
					// calculate chances
					final ItemHolder drop = calculateGroupDrop(group, dropItem, victim, killer, groupItemChance);
					if (drop == null)
					{
						continue GROUP_DROP;
					}
					
					// create lists
					if (randomDrops == null)
					{
						randomDrops = new ArrayList<>(dropOccurrenceCounter);
					}
					
					if (calculatedDrops == null)
					{
						calculatedDrops = new ArrayList<>(dropOccurrenceCounter);
					}
					
					// finally
					final Float itemChance = RatesConfig.RATE_DROP_CHANCE_BY_ID.get(dropItem.getItemId());
					if (itemChance != null)
					{
						if ((groupItemChance * itemChance) < 100)
						{
							dropOccurrenceCounter--;
							if (rateChance == 1) // custom rates break this logic because total chance is more than 100%
							{
								randomDrops.add(drop);
							}
						}
					}
					else if (groupItemChance < 100)
					{
						dropOccurrenceCounter--;
						if (rateChance == 1) // custom rates break this logic because total chance is more than 100%
						{
							randomDrops.add(drop);
						}
					}
					
					calculatedDrops.add(drop);
					
					// no more drops from this group, only use on x1, custom rates break this logic because total chance is more than 100%
					if (rateChance == 1)
					{
						break GROUP_DROP;
					}
				}
			}
			
			// add temporarily removed item when not replaced
			if ((dropOccurrenceCounter > 0) && (cachedItem != null) && (calculatedDrops != null))
			{
				calculatedDrops.add(cachedItem);
			}
			
			// clear random drops
			if (randomDrops != null)
			{
				randomDrops.clear();
				randomDrops = null;
			}
			
			// champion extra drop
			if (victim.isChampion() && (Rnd.get(100) < (victim.getLevel() < killer.getLevel() ? ChampionMonstersConfig.CHAMPION_REWARD_LOWER_LEVEL_ITEM_CHANCE : ChampionMonstersConfig.CHAMPION_REWARD_HIGHER_LEVEL_ITEM_CHANCE)))
			{
				// create list
				if (calculatedDrops == null)
				{
					calculatedDrops = new ArrayList<>();
				}
				
				if (!calculatedDrops.containsAll(ChampionMonstersConfig.CHAMPION_REWARD_ITEMS))
				{
					calculatedDrops.addAll(ChampionMonstersConfig.CHAMPION_REWARD_ITEMS);
				}
			}
		}
		
		return calculatedDrops;
	}
	
	private List<ItemHolder> calculateUngroupedDrops(DropType dropType, Creature victim, Creature killer)
	{
		final List<DropHolder> dropList = dropType == DropType.SPOIL ? _dropListSpoil : _dropListDeath;
		
		// level difference calculations
		final int levelDifference = victim.getLevel() - killer.getLevel();
		final double levelGapChanceToDropAdena = MathUtil.scaleToRange(levelDifference, -RatesConfig.DROP_ADENA_MAX_LEVEL_DIFFERENCE, -RatesConfig.DROP_ADENA_MIN_LEVEL_DIFFERENCE, RatesConfig.DROP_ADENA_MIN_LEVEL_GAP_CHANCE, 100d);
		final double levelGapChanceToDrop = MathUtil.scaleToRange(levelDifference, -RatesConfig.DROP_ITEM_MAX_LEVEL_DIFFERENCE, -RatesConfig.DROP_ITEM_MIN_LEVEL_DIFFERENCE, RatesConfig.DROP_ITEM_MIN_LEVEL_GAP_CHANCE, 100d);
		
		int dropOccurrenceCounter = victim.isRaid() ? RatesConfig.DROP_MAX_OCCURRENCES_RAIDBOSS : RatesConfig.DROP_MAX_OCCURRENCES_NORMAL;
		List<ItemHolder> calculatedDrops = null;
		List<ItemHolder> randomDrops = null;
		ItemHolder cachedItem = null;
		if (dropOccurrenceCounter > 0)
		{
			for (DropHolder dropItem : dropList)
			{
				// check if maximum drop occurrences have been reached
				// items that have 100% drop chance without server rate multipliers drop normally
				if ((dropOccurrenceCounter == 0) && (dropItem.getChance() < 100) && (randomDrops != null) && (calculatedDrops != null))
				{
					// remove highest chance item (temporarily if no other item replaces it)
					cachedItem = randomDrops.remove(0);
					calculatedDrops.remove(cachedItem);
					dropOccurrenceCounter = 1;
				}
				
				// check level gap that may prevent to drop item
				if ((Rnd.nextDouble() * 100) > (dropItem.getItemId() == Inventory.ADENA_ID ? levelGapChanceToDropAdena : levelGapChanceToDrop))
				{
					continue;
				}
				
				// calculate chances
				final ItemHolder drop = calculateUngroupedDrop(dropItem, victim, killer);
				if (drop == null)
				{
					continue;
				}
				
				// create lists
				if (randomDrops == null)
				{
					randomDrops = new ArrayList<>(dropOccurrenceCounter);
				}
				
				if (calculatedDrops == null)
				{
					calculatedDrops = new ArrayList<>(dropOccurrenceCounter);
				}
				
				// finally
				final Float itemChance = RatesConfig.RATE_DROP_CHANCE_BY_ID.get(dropItem.getItemId());
				if (itemChance != null)
				{
					if ((dropItem.getChance() * itemChance) < 100)
					{
						dropOccurrenceCounter--;
						randomDrops.add(drop);
					}
				}
				else if (dropItem.getChance() < 100)
				{
					dropOccurrenceCounter--;
					randomDrops.add(drop);
				}
				
				calculatedDrops.add(drop);
			}
		}
		
		// add temporarily removed item when not replaced
		if ((dropOccurrenceCounter > 0) && (cachedItem != null) && (calculatedDrops != null))
		{
			calculatedDrops.add(cachedItem);
		}
		
		// clear random drops
		if (randomDrops != null)
		{
			randomDrops.clear();
			randomDrops = null;
		}
		
		// champion extra drop
		if (victim.isChampion() && (Rnd.get(100) < (victim.getLevel() < killer.getLevel() ? ChampionMonstersConfig.CHAMPION_REWARD_LOWER_LEVEL_ITEM_CHANCE : ChampionMonstersConfig.CHAMPION_REWARD_HIGHER_LEVEL_ITEM_CHANCE)))
		{
			// create list
			if (calculatedDrops == null)
			{
				calculatedDrops = new ArrayList<>();
			}
			
			if (!calculatedDrops.containsAll(ChampionMonstersConfig.CHAMPION_REWARD_ITEMS))
			{
				calculatedDrops.addAll(ChampionMonstersConfig.CHAMPION_REWARD_ITEMS);
			}
		}
		
		return calculatedDrops;
	}
	
	/**
	 * @param group
	 * @param dropItem
	 * @param victim
	 * @param killer
	 * @param chance
	 * @return ItemHolder
	 */
	private ItemHolder calculateGroupDrop(DropGroupHolder group, DropHolder dropItem, Creature victim, Creature killer, double chance)
	{
		final int itemId = dropItem.getItemId();
		final ItemTemplate item = ItemData.getInstance().getTemplate(itemId);
		final boolean champion = victim.isChampion();
		
		// calculate if item will drop
		if ((Rnd.nextDouble() * 100) < chance)
		{
			// amount is calculated after chance returned success
			double rateAmount = 1;
			if (RatesConfig.RATE_DROP_AMOUNT_BY_ID.get(itemId) != null)
			{
				rateAmount *= RatesConfig.RATE_DROP_AMOUNT_BY_ID.get(itemId);
				if (champion && (itemId == Inventory.ADENA_ID))
				{
					rateAmount *= ChampionMonstersConfig.CHAMPION_ADENAS_REWARDS_AMOUNT;
				}
			}
			else if (item.hasExImmediateEffect())
			{
				rateAmount *= RatesConfig.RATE_HERB_DROP_AMOUNT_MULTIPLIER;
			}
			else if (victim.isRaid())
			{
				rateAmount *= RatesConfig.RATE_RAID_DROP_AMOUNT_MULTIPLIER;
			}
			else
			{
				rateAmount *= RatesConfig.RATE_DEATH_DROP_AMOUNT_MULTIPLIER * (champion ? ChampionMonstersConfig.CHAMPION_REWARDS_AMOUNT : 1);
			}
			
			// premium amount
			final Player player = killer.asPlayer();
			if (player != null)
			{
				if (PremiumSystemConfig.PREMIUM_SYSTEM_ENABLED && player.hasPremiumStatus())
				{
					if (PremiumSystemConfig.PREMIUM_RATE_DROP_AMOUNT_BY_ID.get(itemId) != null)
					{
						rateAmount *= PremiumSystemConfig.PREMIUM_RATE_DROP_AMOUNT_BY_ID.get(itemId);
					}
					else if (item.hasExImmediateEffect())
					{
						// TODO: Premium herb amount? :)
					}
					else if (victim.isRaid())
					{
						// TODO: Premium raid amount? :)
					}
					else
					{
						rateAmount *= PremiumSystemConfig.PREMIUM_RATE_DROP_AMOUNT;
					}
				}
				
				// bonus drop amount effect
				final PlayerStat stat = player.getStat();
				rateAmount *= stat.getBonusDropAmountMultiplier();
				if (itemId == Inventory.ADENA_ID)
				{
					rateAmount *= stat.getBonusDropAdenaMultiplier();
				}
			}
			
			// finally
			return new ItemHolder(itemId, (int) (Rnd.get(dropItem.getMin(), dropItem.getMax()) * rateAmount));
		}
		
		return null;
	}
	
	/**
	 * @param dropItem
	 * @param victim
	 * @param killer
	 * @return ItemHolder
	 */
	private ItemHolder calculateUngroupedDrop(DropHolder dropItem, Creature victim, Creature killer)
	{
		switch (dropItem.getDropType())
		{
			case DROP:
			{
				final int itemId = dropItem.getItemId();
				final ItemTemplate item = ItemData.getInstance().getTemplate(itemId);
				final boolean champion = victim.isChampion();
				
				// chance
				double rateChance = 1;
				if (RatesConfig.RATE_DROP_CHANCE_BY_ID.get(itemId) != null)
				{
					rateChance *= RatesConfig.RATE_DROP_CHANCE_BY_ID.get(itemId);
					if (champion && (itemId == Inventory.ADENA_ID))
					{
						rateChance *= ChampionMonstersConfig.CHAMPION_ADENAS_REWARDS_CHANCE;
					}
					
					if ((itemId == Inventory.ADENA_ID) && (rateChance > 100))
					{
						rateChance = 100;
					}
				}
				else if (item.hasExImmediateEffect())
				{
					rateChance *= RatesConfig.RATE_HERB_DROP_CHANCE_MULTIPLIER;
				}
				else if (victim.isRaid())
				{
					rateChance *= RatesConfig.RATE_RAID_DROP_CHANCE_MULTIPLIER;
				}
				else
				{
					rateChance *= RatesConfig.RATE_DEATH_DROP_CHANCE_MULTIPLIER * (champion ? ChampionMonstersConfig.CHAMPION_REWARDS_CHANCE : 1);
				}
				
				// premium chance
				final Player player = killer.asPlayer();
				if (player != null)
				{
					if (PremiumSystemConfig.PREMIUM_SYSTEM_ENABLED && player.hasPremiumStatus())
					{
						if (PremiumSystemConfig.PREMIUM_RATE_DROP_CHANCE_BY_ID.get(itemId) != null)
						{
							rateChance *= PremiumSystemConfig.PREMIUM_RATE_DROP_CHANCE_BY_ID.get(itemId);
						}
						else if (item.hasExImmediateEffect())
						{
							// TODO: Premium herb chance? :)
						}
						else if (victim.isRaid())
						{
							// TODO: Premium raid chance? :)
						}
						else
						{
							rateChance *= PremiumSystemConfig.PREMIUM_RATE_DROP_CHANCE;
						}
					}
					
					// bonus drop rate effect
					rateChance *= player.getStat().getBonusDropRateMultiplier();
				}
				
				// calculate if item will drop
				if ((Rnd.nextDouble() * 100) < (dropItem.getChance() * rateChance))
				{
					// amount is calculated after chance returned success
					double rateAmount = 1;
					if (RatesConfig.RATE_DROP_AMOUNT_BY_ID.get(itemId) != null)
					{
						rateAmount *= RatesConfig.RATE_DROP_AMOUNT_BY_ID.get(itemId);
						if (champion && (itemId == Inventory.ADENA_ID))
						{
							rateAmount *= ChampionMonstersConfig.CHAMPION_ADENAS_REWARDS_AMOUNT;
						}
					}
					else if (item.hasExImmediateEffect())
					{
						rateAmount *= RatesConfig.RATE_HERB_DROP_AMOUNT_MULTIPLIER;
					}
					else if (victim.isRaid())
					{
						rateAmount *= RatesConfig.RATE_RAID_DROP_AMOUNT_MULTIPLIER;
					}
					else
					{
						rateAmount *= RatesConfig.RATE_DEATH_DROP_AMOUNT_MULTIPLIER * (champion ? ChampionMonstersConfig.CHAMPION_REWARDS_AMOUNT : 1);
					}
					
					// premium amount
					if (player != null)
					{
						if (PremiumSystemConfig.PREMIUM_SYSTEM_ENABLED && player.hasPremiumStatus())
						{
							if (PremiumSystemConfig.PREMIUM_RATE_DROP_AMOUNT_BY_ID.get(itemId) != null)
							{
								rateAmount *= PremiumSystemConfig.PREMIUM_RATE_DROP_AMOUNT_BY_ID.get(itemId);
							}
							else if (item.hasExImmediateEffect())
							{
								// TODO: Premium herb amount? :)
							}
							else if (victim.isRaid())
							{
								// TODO: Premium raid amount? :)
							}
							else
							{
								rateAmount *= PremiumSystemConfig.PREMIUM_RATE_DROP_AMOUNT;
							}
						}
						
						// bonus drop amount effect
						final PlayerStat stat = player.getStat();
						rateAmount *= stat.getBonusDropAmountMultiplier();
						if (itemId == Inventory.ADENA_ID)
						{
							rateAmount *= stat.getBonusDropAdenaMultiplier();
						}
					}
					
					// finally
					return new ItemHolder(itemId, (int) (Rnd.get(dropItem.getMin(), dropItem.getMax()) * rateAmount));
				}
				break;
			}
			case SPOIL:
			{
				// chance
				double rateChance = RatesConfig.RATE_SPOIL_DROP_CHANCE_MULTIPLIER;
				
				// premium chance
				final Player player = killer.asPlayer();
				if (player != null)
				{
					if (PremiumSystemConfig.PREMIUM_SYSTEM_ENABLED && player.hasPremiumStatus())
					{
						rateChance *= PremiumSystemConfig.PREMIUM_RATE_SPOIL_CHANCE;
					}
					
					// bonus spoil rate effect
					rateChance *= player.getStat().getBonusSpoilRateMultiplier();
				}
				
				// calculate if item will be rewarded
				if ((Rnd.nextDouble() * 100) < (dropItem.getChance() * rateChance))
				{
					// amount is calculated after chance returned success
					double rateAmount = RatesConfig.RATE_SPOIL_DROP_AMOUNT_MULTIPLIER;
					
					// premium amount
					if (PremiumSystemConfig.PREMIUM_SYSTEM_ENABLED && (player != null) && player.hasPremiumStatus())
					{
						rateAmount *= PremiumSystemConfig.PREMIUM_RATE_SPOIL_AMOUNT;
					}
					
					// finally
					return new ItemHolder(dropItem.getItemId(), (int) (Rnd.get(dropItem.getMin(), dropItem.getMax()) * rateAmount));
				}
				break;
			}
		}
		
		return null;
	}
	
	public double getCollisionRadiusGrown()
	{
		return _collisionRadiusGrown;
	}
	
	public double getCollisionHeightGrown()
	{
		return _collisionHeightGrown;
	}
	
	public static boolean isAssignableTo(Class<?> subValue, Class<?> clazz)
	{
		// If clazz represents an interface
		if (clazz.isInterface())
		{
			// check if obj implements the clazz interface
			for (Class<?> interface1 : subValue.getInterfaces())
			{
				if (clazz.getName().equals(interface1.getName()))
				{
					return true;
				}
			}
		}
		else
		{
			Class<?> sub = subValue;
			do
			{
				if (sub.getName().equals(clazz.getName()))
				{
					return true;
				}
				
				sub = sub.getSuperclass();
			}
			while (sub != null);
		}
		
		return false;
	}
	
	/**
	 * Checks if obj can be assigned to the Class represented by clazz.<br>
	 * This is true if, and only if, obj is the same class represented by clazz, or a subclass of it or obj implements the interface represented by clazz.
	 * @param obj
	 * @param clazz
	 * @return {@code true} if the object can be assigned to the class, {@code false} otherwise
	 */
	public static boolean isAssignableTo(Object obj, Class<?> clazz)
	{
		return isAssignableTo(obj.getClass(), clazz);
	}
	
	public boolean canTeach(PlayerClass playerClass)
	{
		// If the player is on a third class, fetch the class teacher information for its parent class.
		if (playerClass.level() == 3)
		{
			return _teachInfo.contains(playerClass.getParent());
		}
		
		return _teachInfo.contains(playerClass);
	}
	
	public List<PlayerClass> getTeachInfo()
	{
		return _teachInfo;
	}
	
	public void addTeachInfo(List<PlayerClass> teachInfo)
	{
		_teachInfo.addAll(teachInfo);
	}
}
