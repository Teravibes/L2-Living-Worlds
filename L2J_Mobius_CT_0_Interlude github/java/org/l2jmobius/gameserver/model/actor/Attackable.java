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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.ai.Action;
import org.l2jmobius.gameserver.ai.AttackableAI;
import org.l2jmobius.gameserver.ai.CreatureAI;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.ai.SiegeGuardAI;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.config.custom.ChampionMonstersConfig;
import org.l2jmobius.gameserver.config.custom.FakePlayersConfig;
import org.l2jmobius.gameserver.config.custom.PremiumSystemConfig;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.managers.CursedWeaponsManager;
import org.l2jmobius.gameserver.managers.EventDropManager;
import org.l2jmobius.gameserver.managers.PcCafePointsManager;
import org.l2jmobius.gameserver.managers.WalkingManager;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.model.actor.enums.creature.Team;
import org.l2jmobius.gameserver.model.actor.enums.npc.DropType;
import org.l2jmobius.gameserver.model.actor.holders.npc.AbsorberInfo;
import org.l2jmobius.gameserver.model.actor.holders.npc.AggroInfo;
import org.l2jmobius.gameserver.model.actor.holders.npc.DamageDoneInfo;
import org.l2jmobius.gameserver.model.actor.instance.GrandBoss;
import org.l2jmobius.gameserver.model.actor.instance.Monster;
import org.l2jmobius.gameserver.model.actor.status.AttackableStatus;
import org.l2jmobius.gameserver.model.actor.tasks.attackable.CommandChannelTimer;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.npc.attackable.OnAttackableAggroRangeEnter;
import org.l2jmobius.gameserver.model.events.holders.actor.npc.attackable.OnAttackableAttack;
import org.l2jmobius.gameserver.model.events.holders.actor.npc.attackable.OnAttackableKill;
import org.l2jmobius.gameserver.model.groups.CommandChannel;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.siege.manor.Seed;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.taskmanagers.DecayTaskManager;

public class Attackable extends Npc
{
	// Raid
	private boolean _isRaid = false;
	private boolean _isRaidMinion = false;
	
	//
	private boolean _champion = false;
	private final Map<Creature, AggroInfo> _aggroList = new ConcurrentHashMap<>();
	private boolean _canReturnToSpawnPoint = true;
	private boolean _seeThroughSilentMove = false;
	
	// Manor
	private boolean _seeded = false;
	private Seed _seed = null;
	private int _seederObjId = 0;
	private final AtomicReference<ItemHolder> _harvestItem = new AtomicReference<>();
	
	// Spoil
	private int _spoilerObjectId;
	private final AtomicReference<Collection<ItemHolder>> _sweepItems = new AtomicReference<>();
	
	// Over-hit
	private boolean _overhit;
	private double _overhitDamage;
	private Creature _overhitAttacker;
	
	// Command channel
	private CommandChannel _firstCommandChannelAttacked = null;
	private CommandChannelTimer _commandChannelTimer = null;
	private long _commandChannelLastAttack = 0;
	
	// Soul crystal
	private boolean _absorbed;
	private final Map<Integer, AbsorberInfo> _absorbersList = new ConcurrentHashMap<>();
	
	// Misc
	private boolean _mustGiveExpSp;
	protected int _onKillDelay = 2500; // L2J uses 5000
	
	/**
	 * Creates an attackable NPC.
	 * @param template the attackable NPC template
	 */
	public Attackable(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.Attackable);
		setInvul(false);
		_mustGiveExpSp = true;
	}
	
	@Override
	public AttackableStatus getStatus()
	{
		return (AttackableStatus) super.getStatus();
	}
	
	@Override
	public void initCharStatus()
	{
		setStatus(new AttackableStatus(this));
	}
	
	@Override
	protected CreatureAI initAI()
	{
		return new AttackableAI(this);
	}
	
	public Map<Creature, AggroInfo> getAggroList()
	{
		return _aggroList;
	}
	
	public boolean canReturnToSpawnPoint()
	{
		return _canReturnToSpawnPoint;
	}
	
	public void setCanReturnToSpawnPoint(boolean value)
	{
		_canReturnToSpawnPoint = value;
	}
	
	public boolean canSeeThroughSilentMove()
	{
		return _seeThroughSilentMove;
	}
	
	public void setSeeThroughSilentMove(boolean value)
	{
		_seeThroughSilentMove = value;
	}
	
	/**
	 * Use the skill if minimum checks are pass.
	 * @param skill the skill
	 */
	public void useMagic(Skill skill)
	{
		if ((skill == null) || isAlikeDead() || skill.isPassive() || isCastingNow() || isSkillDisabled(skill))
		{
			return;
		}
		
		if ((getCurrentMp() < (getStat().getMpConsume(skill) + getStat().getMpInitialConsume(skill))) || (getCurrentHp() <= skill.getHpConsume()))
		{
			return;
		}
		
		if (!skill.isStatic())
		{
			if (skill.isMagic())
			{
				if (isMuted())
				{
					return;
				}
			}
			else if (isPhysicalMuted())
			{
				return;
			}
		}
		
		final WorldObject target = skill.getFirstOfTargetList(this);
		if (target != null)
		{
			getAI().setIntention(Intention.CAST, skill, target);
		}
	}
	
	/**
	 * Reduce the current HP of the Attackable.
	 * @param damage The HP decrease value
	 * @param attacker The Creature who attacks
	 */
	@Override
	public void reduceCurrentHp(double damage, Creature attacker, Skill skill)
	{
		reduceCurrentHp(damage, attacker, true, false, skill);
	}
	
	/**
	 * Reduce the current HP of the Attackable, update its _aggroList and launch the doDie Task if necessary.
	 * @param damage The HP decrease value
	 * @param attacker The Creature who attacks
	 * @param awake The awake state (If True : stop sleeping)
	 * @param isDOT
	 * @param skill
	 */
	@Override
	public void reduceCurrentHp(double damage, Creature attacker, boolean awake, boolean isDOT, Skill skill)
	{
		if (_isRaid && !isMinion() && (attacker != null) && (attacker.getParty() != null) && attacker.getParty().isInCommandChannel() && attacker.getParty().getCommandChannel().meetRaidWarCondition(this))
		{
			if (_firstCommandChannelAttacked == null) // looting right isn't set
			{
				synchronized (this)
				{
					if (_firstCommandChannelAttacked == null)
					{
						_firstCommandChannelAttacked = attacker.getParty().getCommandChannel();
						if (_firstCommandChannelAttacked != null)
						{
							_commandChannelTimer = new CommandChannelTimer(this);
							_commandChannelLastAttack = System.currentTimeMillis();
							ThreadPool.schedule(_commandChannelTimer, 10000); // check for last attack
							_firstCommandChannelAttacked.broadcastPacket(new CreatureSay(null, ChatType.PARTYROOM_ALL, "", "You have looting rights!")); // TODO: retail msg
						}
					}
				}
			}
			else if (attacker.getParty().getCommandChannel().equals(_firstCommandChannelAttacked)) // is in same channel
			{
				_commandChannelLastAttack = System.currentTimeMillis(); // update last attack time
			}
		}
		
		// Add damage and hate to the attacker AggroInfo of the Attackable _aggroList
		if (attacker != null)
		{
			addDamage(attacker, (int) damage, skill);
		}
		
		// If this Attackable is a Monster and it has spawned minions, call its minions to battle
		if (isMonster())
		{
			Monster master = asMonster();
			
			if (master.hasMinions())
			{
				master.getMinionList().onAssist(this, attacker);
			}
			
			master = master.getLeader();
			if ((master != null) && master.hasMinions())
			{
				master.getMinionList().onAssist(this, attacker);
			}
		}
		
		// Reduce the current HP of the Attackable and launch the doDie Task if necessary
		super.reduceCurrentHp(damage, attacker, awake, isDOT, skill);
	}
	
	public synchronized void setMustRewardExpSp(boolean value)
	{
		_mustGiveExpSp = value;
	}
	
	public synchronized boolean getMustRewardExpSP()
	{
		return _mustGiveExpSp && !isFakePlayer();
	}
	
	/**
	 * Kill the Attackable (the corpse disappeared after 7 seconds), distribute rewards (EXP, SP, Drops...) and notify Quest Engine.<br>
	 * Actions:<br>
	 * Distribute Exp and SP rewards to Player (including Summon owner) that hit the Attackable and to their Party members<br>
	 * Notify the Quest Engine of the Attackable death if necessary.<br>
	 * Kill the Npc (the corpse disappeared after 7 seconds)<br>
	 * Caution: This method DOESN'T GIVE rewards to Pet.
	 * @param killer The Creature that has killed the Attackable
	 */
	@Override
	public boolean doDie(Creature killer)
	{
		// Kill the Npc (the corpse disappeared after 7 seconds)
		if (!super.doDie(killer))
		{
			return false;
		}
		
		// Delayed notification.
		if (killer != null)
		{
			final Player player = killer.asPlayer();
			if ((player != null) && EventDispatcher.getInstance().hasListener(EventType.ON_ATTACKABLE_KILL, this))
			{
				EventDispatcher.getInstance().notifyEventAsyncDelayed(new OnAttackableKill(player, this, killer.isSummon()), this, _onKillDelay);
			}
		}
		
		// Notify to minions if there are.
		if (isMonster())
		{
			final Monster mob = asMonster();
			final Monster leader = mob.getLeader();
			if ((leader != null) && leader.hasMinions())
			{
				final int respawnTime = NpcConfig.MINIONS_RESPAWN_TIME.containsKey(getId()) ? NpcConfig.MINIONS_RESPAWN_TIME.get(getId()) * 1000 : -1;
				leader.getMinionList().onMinionDie(mob, respawnTime);
			}
			
			if (mob.hasMinions())
			{
				mob.getMinionList().onMasterDie(false);
			}
		}
		
		return true;
	}
	
	private static class PartyContainer
	{
		public Party party;
		public long damage;
		
		public PartyContainer(Party party, long damage)
		{
			this.party = party;
			this.damage = damage;
		}
	}
	
	/**
	 * Distribute Exp and SP rewards to Player (including Summon owner) that hit the Attackable and to their Party members.<br>
	 * Actions:<br>
	 * Get the Player owner of the Servitor (if necessary) and Party in progress.<br>
	 * Calculate the Experience and SP rewards in function of the level difference.<br>
	 * Add Exp and SP rewards to Player (including Summon penalty) and to Party members in the known area of the last attacker.<br>
	 * Caution : This method DOESN'T GIVE rewards to Pet.
	 * @param lastAttacker The Creature that has killed the Attackable
	 */
	@Override
	protected void calculateRewards(Creature lastAttacker)
	{
		try
		{
			if (_aggroList.isEmpty())
			{
				return;
			}
			
			// NOTE: Concurrent-safe map is used because while iterating to verify all conditions sometimes an entry must be removed.
			final Map<Player, DamageDoneInfo> rewards = new ConcurrentHashMap<>();
			
			Player maxDealer = null;
			long maxDamage = 0;
			long totalDamage = 0;
			
			// While Iterating over This Map Removing Object is Not Allowed
			// Go through the _aggroList of the Attackable
			for (AggroInfo info : _aggroList.values())
			{
				if (info == null)
				{
					continue;
				}
				
				// Get the Creature corresponding to this attacker
				final Player attacker = info.getAttacker().asPlayer();
				if (attacker == null)
				{
					continue;
				}
				
				// Get damages done by this attacker
				final long damage = info.getDamage();
				
				// Prevent unwanted behavior
				if (damage > 1)
				{
					// Check if damage dealer isn't too far from this (killed monster)
					if (calculateDistance3D(attacker) > PlayerConfig.ALT_PARTY_RANGE)
					{
						continue;
					}
					
					totalDamage += damage;
					
					// Calculate real damages (Summoners should get own damage plus summon's damage)
					final DamageDoneInfo reward = rewards.computeIfAbsent(attacker, DamageDoneInfo::new);
					reward.addDamage(damage);
					
					if (reward.getDamage() > maxDamage)
					{
						maxDealer = attacker;
						maxDamage = reward.getDamage();
					}
				}
			}
			
			final List<PartyContainer> damagingParties = new ArrayList<>();
			for (AggroInfo info : _aggroList.values())
			{
				final Creature attacker = info.getAttacker();
				if (attacker == null)
				{
					continue;
				}
				
				long totalMemberDamage = 0;
				final Party party = attacker.getParty();
				if (party == null)
				{
					continue;
				}
				
				Optional<PartyContainer> partyContainerStream = Optional.empty();
				for (int i = 0, damagingPartiesSize = damagingParties.size(); i < damagingPartiesSize; i++)
				{
					final PartyContainer p = damagingParties.get(i);
					if (p.party == party)
					{
						partyContainerStream = Optional.of(p);
						break;
					}
				}
				
				final PartyContainer container = partyContainerStream.orElse(new PartyContainer(party, 0L));
				final List<Player> members = party.getMembers();
				for (Player e : members)
				{
					final AggroInfo memberAggro = _aggroList.get(e);
					if (memberAggro == null)
					{
						continue;
					}
					
					if (memberAggro.getDamage() > 1)
					{
						totalMemberDamage += memberAggro.getDamage();
					}
				}
				
				container.damage = totalMemberDamage;
				
				if (!partyContainerStream.isPresent())
				{
					damagingParties.add(container);
				}
			}
			
			final PartyContainer mostDamageParty;
			damagingParties.sort(Comparator.comparingLong(c -> -c.damage));
			mostDamageParty = !damagingParties.isEmpty() ? damagingParties.get(0) : null;
			
			// Manage Base, Quests and Sweep drops of the Attackable
			if ((mostDamageParty != null) && (mostDamageParty.damage > maxDamage))
			{
				Player leader = mostDamageParty.party.getLeader();
				doItemDrop(leader);
				EventDropManager.getInstance().doEventDrop(leader, this);
			}
			else
			{
				doItemDrop((maxDealer != null) && maxDealer.isOnline() ? maxDealer : lastAttacker);
				EventDropManager.getInstance().doEventDrop(lastAttacker, this);
			}
			
			if (!getMustRewardExpSP())
			{
				return;
			}
			
			if (!rewards.isEmpty())
			{
				for (DamageDoneInfo reward : rewards.values())
				{
					if (reward == null)
					{
						continue;
					}
					
					// Attacker to be rewarded
					final Player attacker = reward.getAttacker();
					
					// Total amount of damage done
					final long damage = reward.getDamage();
					
					// Get party
					final Party attackerParty = attacker.getParty();
					
					// Penalty applied to the attacker's XP
					// If this attacker have servitor, get Exp Penalty applied for the servitor.
					final float penalty = attacker.hasServitor() ? attacker.getSummon().asServitor().getExpMultiplier() : 1;
					
					// If there's NO party in progress
					if (attackerParty == null)
					{
						// Calculate Exp and SP rewards
						if (isInSurroundingRegion(attacker))
						{
							// Calculate the difference of level between this attacker (player or servitor owner) and the Attackable
							// mob = 24, atk = 10, diff = -14 (full xp)
							// mob = 24, atk = 28, diff = 4 (some xp)
							// mob = 24, atk = 50, diff = 26 (no xp)
							final double[] expSp = calculateExpAndSp(attacker.getLevel(), damage, totalDamage);
							double exp = expSp[0];
							double sp = expSp[1];
							
							if (ChampionMonstersConfig.CHAMPION_ENABLE && _champion)
							{
								exp *= ChampionMonstersConfig.CHAMPION_REWARDS_EXP_SP;
								sp *= ChampionMonstersConfig.CHAMPION_REWARDS_EXP_SP;
							}
							
							exp *= penalty;
							
							// Check for an over-hit enabled strike
							final Creature overhitAttacker = _overhitAttacker;
							if (_overhit && (overhitAttacker != null))
							{
								final Player player = overhitAttacker.asPlayer();
								if ((player != null) && (attacker == player))
								{
									attacker.sendPacket(SystemMessageId.OVER_HIT);
									exp += calculateOverhitExp(exp);
								}
							}
							
							// Distribute the Exp and SP between the Player and its Summon
							if (!attacker.isDead())
							{
								long addExp = Math.round(attacker.calcStat(Stat.EXPSP_RATE, exp, null, null));
								int addSp = (int) attacker.calcStat(Stat.EXPSP_RATE, sp, null, null);
								
								// Premium rates
								if (attacker.hasPremiumStatus())
								{
									addExp *= PremiumSystemConfig.PREMIUM_RATE_XP;
									addSp *= PremiumSystemConfig.PREMIUM_RATE_SP;
								}
								
								attacker.addExpAndSp(addExp, addSp, useVitalityRate());
								if ((addExp > 0) && useVitalityRate())
								{
									attacker.updateVitalityPoints(getVitalityPoints(attacker.getLevel(), damage), true, false);
									PcCafePointsManager.getInstance().givePcCafePoint(attacker, exp);
								}
							}
						}
					}
					else
					{
						// share with party members
						long partyDmg = 0;
						double partyMul = 1;
						int partyLvl = 0;
						
						// Get all Creature that can be rewarded in the party
						final List<Player> rewardedMembers = new ArrayList<>();
						
						// Go through all Player in the party
						final List<Player> groupMembers = attackerParty.isInCommandChannel() ? attackerParty.getCommandChannel().getMembers() : attackerParty.getMembers();
						for (Player partyPlayer : groupMembers)
						{
							if ((partyPlayer == null) || partyPlayer.isDead())
							{
								continue;
							}
							
							// Get the RewardInfo of this Player from Attackable rewards
							final DamageDoneInfo reward2 = rewards.get(partyPlayer);
							
							// If the Player is in the Attackable rewards add its damages to party damages
							if (reward2 != null)
							{
								if (calculateDistance3D(partyPlayer) < PlayerConfig.ALT_PARTY_RANGE)
								{
									partyDmg += reward2.getDamage(); // Add Player damages to party damages
									rewardedMembers.add(partyPlayer);
									
									if (partyPlayer.getLevel() > partyLvl)
									{
										if (attackerParty.isInCommandChannel())
										{
											partyLvl = attackerParty.getCommandChannel().getLevel();
										}
										else
										{
											partyLvl = partyPlayer.getLevel();
										}
									}
								}
								
								rewards.remove(partyPlayer); // Remove the Player from the Attackable rewards
							}
							else if (calculateDistance3D(partyPlayer) < PlayerConfig.ALT_PARTY_RANGE)
							{
								rewardedMembers.add(partyPlayer);
								if (partyPlayer.getLevel() > partyLvl)
								{
									if (attackerParty.isInCommandChannel())
									{
										partyLvl = attackerParty.getCommandChannel().getLevel();
									}
									else
									{
										partyLvl = partyPlayer.getLevel();
									}
								}
							}
						}
						
						// If the party didn't killed this Attackable alone
						if (partyDmg < totalDamage)
						{
							partyMul = ((double) partyDmg / totalDamage);
						}
						
						// Calculate Exp and SP rewards
						final double[] expSp = calculateExpAndSp(partyLvl, partyDmg, totalDamage);
						double exp = expSp[0];
						double sp = expSp[1];
						
						if (ChampionMonstersConfig.CHAMPION_ENABLE && _champion)
						{
							exp *= ChampionMonstersConfig.CHAMPION_REWARDS_EXP_SP;
							sp *= ChampionMonstersConfig.CHAMPION_REWARDS_EXP_SP;
						}
						
						exp *= partyMul;
						sp *= partyMul;
						
						// Check for an over-hit enabled strike
						// (When in party, the over-hit exp bonus is given to the whole party and splitted proportionally through the party members)
						final Creature overhitAttacker = _overhitAttacker;
						if (_overhit && (overhitAttacker != null))
						{
							final Player player = overhitAttacker.asPlayer();
							if ((player != null) && (attacker == player))
							{
								attacker.sendPacket(SystemMessageId.OVER_HIT);
								exp += calculateOverhitExp(exp);
							}
						}
						
						// Distribute Experience and SP rewards to Player Party members in the known area of the last attacker
						if (partyDmg > 0)
						{
							attackerParty.distributeXpAndSp(exp, sp, rewardedMembers, partyLvl, partyDmg, this);
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "", e);
		}
	}
	
	@Override
	public void addAttackerToAttackByList(Creature creature)
	{
		if ((creature == null) || (creature == this) || getAttackByList().contains(creature))
		{
			return;
		}
		
		getAttackByList().add(creature);
	}
	
	public Creature getMainDamageDealer()
	{
		if (_aggroList.isEmpty())
		{
			return null;
		}
		
		long damage = 0;
		Creature damageDealer = null;
		for (AggroInfo info : _aggroList.values())
		{
			if ((info != null) && (info.getDamage() > damage) && (calculateDistance3D(info.getAttacker()) < PlayerConfig.ALT_PARTY_RANGE))
			{
				damage = info.getDamage();
				damageDealer = info.getAttacker();
			}
		}
		
		return damageDealer;
	}
	
	/**
	 * Add damage and hate to the attacker AggroInfo of the Attackable _aggroList.
	 * @param attacker The Creature that gave damages to this Attackable
	 * @param damage The number of damages given by the attacker Creature
	 * @param skill
	 */
	public void addDamage(Creature attacker, int damage, Skill skill)
	{
		if (attacker == null)
		{
			return;
		}
		
		// Notify the Attackable AI with ATTACKED
		if (!isDead())
		{
			try
			{
				// If monster is on walk - stop it
				if (isWalker() && !isCoreAIDisabled() && WalkingManager.getInstance().isOnWalk(this))
				{
					WalkingManager.getInstance().stopMoving(this, false, true);
				}
				
				getAI().notifyAction(Action.ATTACKED, attacker);
				
				// Calculate the amount of hate this attackable receives from this attack.
				final long hateValue = ((long) damage * 100) / (getLevel() + 7);
				addDamageHate(attacker, damage, (int) hateValue);
				
				final Player player = attacker.asPlayer();
				if ((player != null) && EventDispatcher.getInstance().hasListener(EventType.ON_ATTACKABLE_ATTACK, this))
				{
					EventDispatcher.getInstance().notifyEventAsync(new OnAttackableAttack(player, this, damage, skill, attacker.isSummon()), this);
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "", e);
			}
		}
	}
	
	/**
	 * Adds damage and hate to the attacker aggression list for this character.
	 * @param attacker The Creature that gave damages to this Attackable
	 * @param damage The number of damages given by the attacker Creature
	 * @param aggroValue The hate (=damage) given by the attacker Creature
	 */
	public void addDamageHate(Creature attacker, long damage, long aggroValue)
	{
		if ((attacker == null) || (attacker == this))
		{
			return;
		}
		
		// Check if fake players should aggro each other.
		if (isFakePlayer() && !FakePlayersConfig.FAKE_PLAYER_AGGRO_FPC && attacker.isFakePlayer())
		{
			return;
		}
		
		// Get the AggroInfo of the attacker Creature from the _aggroList of the Attackable
		final AggroInfo ai = _aggroList.computeIfAbsent(attacker, AggroInfo::new);
		ai.addDamage(damage);
		
		// Traps does not cause aggro
		// making this hack because not possible to determine if damage made by trap
		// so just check for triggered trap here
		long aggro = aggroValue;
		final Player targetPlayer = attacker.asPlayer();
		if ((targetPlayer == null) || (targetPlayer.getTrap() == null) || !targetPlayer.getTrap().isTriggered())
		{
			ai.addHate(aggro);
		}
		
		if ((targetPlayer != null) && (aggro == 0))
		{
			addDamageHate(attacker, 0, 1);
			
			// Set the intention to the Attackable to ACTIVE
			if (getAI().getIntention() == Intention.IDLE)
			{
				getAI().setIntention(Intention.ACTIVE);
			}
			
			// Notify to scripts
			if (EventDispatcher.getInstance().hasListener(EventType.ON_ATTACKABLE_AGGRO_RANGE_ENTER, this))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnAttackableAggroRangeEnter(this, targetPlayer, attacker.isSummon()), this);
			}
		}
		else if ((targetPlayer == null) && (aggro == 0))
		{
			aggro = 1;
			ai.addHate(1);
		}
		
		// Set the intention to the Attackable to ACTIVE
		if ((aggro != 0) && (getAI().getIntention() == Intention.IDLE))
		{
			getAI().setIntention(Intention.ACTIVE);
		}
	}
	
	public void reduceHate(Creature target, long amount)
	{
		if (getAI() instanceof SiegeGuardAI)
		{
			// TODO: this just prevents error until siege guards are handled properly
			stopHating(target);
			setTarget(null);
			getAI().setIntention(Intention.IDLE);
			return;
		}
		
		if (target == null) // whole aggrolist
		{
			final Creature mostHated = getMostHated();
			if (mostHated == null) // makes target passive for a moment more
			{
				((AttackableAI) getAI()).setGlobalAggro(-25);
				return;
			}
			
			for (AggroInfo ai : _aggroList.values())
			{
				ai.addHate(amount);
			}
			
			if (getHating(mostHated) >= 0)
			{
				((AttackableAI) getAI()).setGlobalAggro(-25);
				clearAggroList();
				getAI().setIntention(Intention.ACTIVE);
				if (!isFakePlayer())
				{
					setWalking();
				}
			}
			return;
		}
		
		final AggroInfo ai = _aggroList.get(target);
		if (ai == null)
		{
			return;
		}
		
		ai.addHate(amount);
		if ((ai.getHate() >= 0) && (getMostHated() == null))
		{
			((AttackableAI) getAI()).setGlobalAggro(-25);
			clearAggroList();
			getAI().setIntention(Intention.ACTIVE);
			if (!isFakePlayer())
			{
				setWalking();
			}
		}
	}
	
	/**
	 * Clears _aggroList hate of the Creature without removing from the list.
	 * @param target
	 */
	public void stopHating(Creature target)
	{
		if (target == null)
		{
			return;
		}
		
		final AggroInfo ai = _aggroList.get(target);
		if (ai != null)
		{
			ai.stopHate();
		}
	}
	
	/**
	 * @return the most hated Creature of the Attackable _aggroList.
	 */
	public Creature getMostHated()
	{
		if (_aggroList.isEmpty() || isAlikeDead())
		{
			return null;
		}
		
		Creature mostHated = null;
		long maxHate = 0;
		
		// While Interacting over This Map Removing Object is Not Allowed
		// Go through the aggroList of the Attackable
		for (AggroInfo ai : _aggroList.values())
		{
			if (ai == null)
			{
				continue;
			}
			
			if (ai.checkHate(this) > maxHate)
			{
				mostHated = ai.getAttacker();
				maxHate = ai.getHate();
			}
		}
		
		return mostHated;
	}
	
	/**
	 * @return the 2 most hated Creature of the Attackable _aggroList.
	 */
	public List<Creature> get2MostHated()
	{
		if (_aggroList.isEmpty() || isAlikeDead())
		{
			return null;
		}
		
		Creature mostHated = null;
		Creature secondMostHated = null;
		long maxHate = 0;
		final List<Creature> result = new ArrayList<>();
		
		// While iterating over this map removing objects is not allowed
		// Go through the aggroList of the Attackable
		for (AggroInfo ai : _aggroList.values())
		{
			if (ai == null)
			{
				continue;
			}
			
			if (ai.checkHate(this) > maxHate)
			{
				secondMostHated = mostHated;
				mostHated = ai.getAttacker();
				maxHate = ai.getHate();
			}
		}
		
		result.add(mostHated);
		
		if (getAttackByList().contains(secondMostHated))
		{
			result.add(secondMostHated);
		}
		else
		{
			result.add(null);
		}
		
		return result;
	}
	
	public List<Creature> getHateList()
	{
		if (_aggroList.isEmpty() || isAlikeDead())
		{
			return null;
		}
		
		final List<Creature> result = new ArrayList<>();
		for (AggroInfo ai : _aggroList.values())
		{
			if (ai == null)
			{
				continue;
			}
			
			ai.checkHate(this);
			
			result.add(ai.getAttacker());
		}
		
		return result;
	}
	
	/**
	 * @param target The Creature whose hate level must be returned
	 * @return the hate level of the Attackable against this Creature contained in _aggroList.
	 */
	public long getHating(Creature target)
	{
		if (_aggroList.isEmpty() || (target == null))
		{
			return 0;
		}
		
		final AggroInfo ai = _aggroList.get(target);
		if (ai == null)
		{
			return 0;
		}
		
		if (ai.getAttacker().isPlayer())
		{
			final Player act = ai.getAttacker().asPlayer();
			if (act.isInvisible() || act.isInvul() || act.isSpawnProtected())
			{
				// Remove Object Should Use This Method and Can be Blocked While Interacting
				_aggroList.remove(target);
				return 0;
			}
		}
		
		if (!ai.getAttacker().isSpawned() || ai.getAttacker().isInvisible())
		{
			_aggroList.remove(target);
			return 0;
		}
		
		if (ai.getAttacker().isAlikeDead())
		{
			ai.stopHate();
			return 0;
		}
		
		return ai.getHate();
	}
	
	public void doItemDrop(Creature mainDamageDealer)
	{
		doItemDrop(getTemplate(), mainDamageDealer);
	}
	
	/**
	 * Manage Base, Quests and Special Events drops of Attackable (called by calculateRewards).<br>
	 * Concept:<br>
	 * During a Special Event all Attackable can drop extra Items.<br>
	 * Those extra Items are defined in the table allNpcDateDrops of the EventDroplist.<br>
	 * Each Special Event has a start and end date to stop to drop extra Items automatically.<br>
	 * Actions:<br>
	 * Manage drop of Special Events created by GM for a defined period.<br>
	 * Get all possible drops of this Attackable from NpcTemplate and add it Quest drops.<br>
	 * For each possible drops (base + quests), calculate which one must be dropped (random).<br>
	 * Get each Item quantity dropped (random).<br>
	 * Create this or these Item corresponding to each Item Identifier dropped.<br>
	 * If the autoLoot mode is actif and if the Creature that has killed the Attackable is a Player, Give the item(s) to the Player that has killed the Attackable.<br>
	 * If the autoLoot mode isn't actif or if the Creature that has killed the Attackable is not a Player, add this or these item(s) in the world as a visible object at the position where mob was last.
	 * @param npcTemplate
	 * @param mainDamageDealer
	 */
	public void doItemDrop(NpcTemplate npcTemplate, Creature mainDamageDealer)
	{
		if (mainDamageDealer == null)
		{
			return;
		}
		
		final Player player = mainDamageDealer.asPlayer();
		
		// Don't drop anything if the last attacker or owner isn't Player
		if (player == null)
		{
			// unless its a fake player and they can drop items
			if (mainDamageDealer.isFakePlayer() && FakePlayersConfig.FAKE_PLAYER_CAN_DROP_ITEMS)
			{
				final Collection<ItemHolder> deathItems = npcTemplate.calculateDrops(DropType.DROP, this, mainDamageDealer);
				if (deathItems != null)
				{
					for (ItemHolder drop : deathItems)
					{
						final ItemTemplate item = ItemData.getInstance().getTemplate(drop.getId());
						
						// Check if the autoLoot mode is active
						if (PlayerConfig.AUTO_LOOT_ITEM_IDS.contains(item.getId()) || isFlying() || (!item.hasExImmediateEffect() && ((!_isRaid && PlayerConfig.AUTO_LOOT) || (_isRaid && PlayerConfig.AUTO_LOOT_RAIDS))))
						{
							// do nothing
						}
						else if (PlayerConfig.AUTO_LOOT_HERBS && item.hasExImmediateEffect())
						{
							for (SkillHolder skillHolder : item.getSkills())
							{
								doSimultaneousCast(skillHolder.getSkill());
							}
							
							mainDamageDealer.broadcastInfo(); // ? check if this is necessary
						}
						else
						{
							final Item droppedItem = dropItem(mainDamageDealer, drop); // drop the item on the ground
							if (FakePlayersConfig.FAKE_PLAYER_CAN_PICKUP)
							{
								mainDamageDealer.getFakePlayerDrops().add(droppedItem);
							}
						}
					}
					
					deathItems.clear();
				}
			}
			return;
		}
		
		CursedWeaponsManager.getInstance().checkDrop(this, player);
		
		if (isSpoiled())
		{
			_sweepItems.set(npcTemplate.calculateDrops(DropType.SPOIL, this, player));
		}
		
		final Collection<ItemHolder> deathItems = npcTemplate.calculateDrops(DropType.DROP, this, player);
		if (deathItems != null)
		{
			for (ItemHolder drop : deathItems)
			{
				final ItemTemplate item = ItemData.getInstance().getTemplate(drop.getId());
				
				// Check if the autoLoot mode is active
				if (PlayerConfig.AUTO_LOOT_ITEM_IDS.contains(item.getId()) || isFlying() || (!item.hasExImmediateEffect() && ((!_isRaid && PlayerConfig.AUTO_LOOT) || (_isRaid && PlayerConfig.AUTO_LOOT_RAIDS))) || (item.hasExImmediateEffect() && PlayerConfig.AUTO_LOOT_HERBS))
				{
					player.doAutoLoot(this, drop); // Give the item(s) to the Player that has killed the Attackable
				}
				else
				{
					dropItem(player, drop); // drop the item on the ground
				}
				
				// Broadcast message if RaidBoss was defeated
				if (_isRaid && !_isRaidMinion && (drop.getCount() > 0))
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.S1_DIED_AND_DROPPED_S3_S2);
					sm.addString(getName());
					sm.addItemName(item);
					sm.addInt(drop.getCount());
					broadcastPacket(sm);
				}
			}
			
			deathItems.clear();
		}
	}
	
	/**
	 * @return the active weapon of this Attackable (= null).
	 */
	public Item getActiveWeapon()
	{
		return null;
	}
	
	/**
	 * Verifies if the creature is in the aggro list.
	 * @param creature the creature
	 * @return {@code true} if the creature is in the aggro list, {@code false} otherwise
	 */
	public boolean isInAggroList(Creature creature)
	{
		return (creature != null) && _aggroList.containsKey(creature);
	}
	
	/**
	 * Clear the _aggroList of the Attackable.
	 */
	public void clearAggroList()
	{
		_aggroList.clear();
		
		// clear overhit values
		_overhit = false;
		_overhitDamage = 0;
		_overhitAttacker = null;
	}
	
	/**
	 * @return {@code true} if there is a loot to sweep, {@code false} otherwise.
	 */
	@Override
	public boolean isSweepActive()
	{
		return _sweepItems.get() != null;
	}
	
	/**
	 * @return a copy of dummy items for the spoil loot.
	 */
	public List<ItemTemplate> getSpoilLootItems()
	{
		final Collection<ItemHolder> sweepItems = _sweepItems.get();
		final List<ItemTemplate> lootItems = new LinkedList<>();
		if (sweepItems != null)
		{
			for (ItemHolder item : sweepItems)
			{
				lootItems.add(ItemData.getInstance().getTemplate(item.getId()));
			}
		}
		
		return lootItems;
	}
	
	/**
	 * @return table containing all Item that can be spoiled.
	 */
	public Collection<ItemHolder> takeSweep()
	{
		return _sweepItems.getAndSet(null);
	}
	
	/**
	 * @return table containing all Item that can be harvested.
	 */
	public ItemHolder takeHarvest()
	{
		return _harvestItem.getAndSet(null);
	}
	
	/**
	 * Checks if the corpse is too old.
	 * @param attacker the player to validate
	 * @param remainingTime the time to check
	 * @param sendMessage if {@code true} will send a message of corpse too old
	 * @return {@code true} if the corpse is too old
	 */
	public boolean isOldCorpse(Player attacker, int remainingTime, boolean sendMessage)
	{
		if (!isDead() || (DecayTaskManager.getInstance().getRemainingTime(this) >= remainingTime))
		{
			return false;
		}
		
		if (sendMessage && (attacker != null))
		{
			attacker.sendPacket(SystemMessageId.THE_CORPSE_IS_TOO_OLD_THE_SKILL_CANNOT_BE_USED);
		}
		
		return true;
	}
	
	/**
	 * @param sweeper the player to validate.
	 * @param sendMessage sendMessage if {@code true} will send a message of sweep not allowed.
	 * @return {@code true} if is the spoiler or is in the spoiler party.
	 */
	public boolean checkSpoilOwner(Player sweeper, boolean sendMessage)
	{
		if ((sweeper.getObjectId() == _spoilerObjectId) || sweeper.isInLooterParty(_spoilerObjectId))
		{
			return true;
		}
		
		if (sendMessage)
		{
			sweeper.sendPacket(SystemMessageId.THERE_ARE_NO_PRIORITY_RIGHTS_ON_A_SWEEPER);
		}
		
		return false;
	}
	
	/**
	 * Set the over-hit flag on the Attackable.
	 * @param status The status of the over-hit flag
	 */
	public void overhitEnabled(boolean status)
	{
		_overhit = status;
	}
	
	/**
	 * Set the over-hit values like the attacker who did the strike and the amount of damage done by the skill.
	 * @param attacker The Creature who hit on the Attackable using the over-hit enabled skill
	 * @param damage The amount of damage done by the over-hit enabled skill on the Attackable
	 */
	public void setOverhitValues(Creature attacker, double damage)
	{
		// Calculate the over-hit damage
		// Ex: mob had 10 HP left, over-hit skill did 50 damage total, over-hit damage is 40
		final double overhitDmg = -(getCurrentHp() - damage);
		if (overhitDmg < 0)
		{
			// we didn't killed the mob with the over-hit strike. (it wasn't really an over-hit strike)
			// let's just clear all the over-hit related values
			overhitEnabled(false);
			_overhitDamage = 0;
			_overhitAttacker = null;
			return;
		}
		
		overhitEnabled(true);
		_overhitDamage = overhitDmg;
		_overhitAttacker = attacker;
	}
	
	/**
	 * Return the Creature who hit on the Attackable using an over-hit enabled skill.
	 * @return Creature attacker
	 */
	public Creature getOverhitAttacker()
	{
		return _overhitAttacker;
	}
	
	/**
	 * Return the amount of damage done on the Attackable using an over-hit enabled skill.
	 * @return double damage
	 */
	public double getOverhitDamage()
	{
		return _overhitDamage;
	}
	
	/**
	 * @return True if the Attackable was hit by an over-hit enabled skill.
	 */
	public boolean isOverhit()
	{
		return _overhit;
	}
	
	/**
	 * Activate the absorbed soul condition on the Attackable.
	 */
	public void absorbSoul()
	{
		_absorbed = true;
	}
	
	/**
	 * @return True if the Attackable had his soul absorbed.
	 */
	public boolean isAbsorbed()
	{
		return _absorbed;
	}
	
	/**
	 * Adds an attacker that successfully absorbed the soul of this Attackable into the _absorbersList.
	 * @param attacker
	 */
	public void addAbsorber(Player attacker)
	{
		// If we have no _absorbersList initiated, do it
		final AbsorberInfo ai = _absorbersList.get(attacker.getObjectId());
		
		// If the Creature attacker isn't already in the _absorbersList of this Attackable, add it
		if (ai == null)
		{
			_absorbersList.put(attacker.getObjectId(), new AbsorberInfo(attacker.getObjectId(), getCurrentHp()));
		}
		else
		{
			ai.setAbsorbedHp(getCurrentHp());
		}
		
		// Set this Attackable as absorbed
		absorbSoul();
	}
	
	public void resetAbsorbList()
	{
		_absorbed = false;
		_absorbersList.clear();
	}
	
	public Map<Integer, AbsorberInfo> getAbsorbersList()
	{
		return _absorbersList;
	}
	
	/**
	 * Calculate the Experience and SP to distribute to attacker (Player, Servitor or Party) of the Attackable.
	 * @param charLevel The killer level
	 * @param damage The damages given by the attacker (Player, Servitor or Party)
	 * @param totalDamage The total damage done
	 * @return
	 */
	private double[] calculateExpAndSp(int charLevel, long damage, long totalDamage)
	{
		final int levelDiff = charLevel - getLevel();
		double xp = 0;
		double sp = 0;
		
		if ((levelDiff < RatesConfig.MONSTER_EXP_MAX_LEVEL_DIFFERENCE) && (levelDiff > -RatesConfig.MONSTER_EXP_MAX_LEVEL_DIFFERENCE))
		{
			xp = Math.max(0, (getExpReward(charLevel) * damage) / totalDamage);
			sp = Math.max(0, (getSpReward(charLevel) * damage) / totalDamage);
			
			if ((charLevel > 84) && (levelDiff <= -3))
			{
				double mul;
				switch (levelDiff)
				{
					case -3:
					{
						mul = 0.97;
						break;
					}
					case -4:
					{
						mul = 0.67;
						break;
					}
					case -5:
					{
						mul = 0.42;
						break;
					}
					case -6:
					{
						mul = 0.25;
						break;
					}
					case -7:
					{
						mul = 0.15;
						break;
					}
					case -8:
					{
						mul = 0.09;
						break;
					}
					case -9:
					{
						mul = 0.05;
						break;
					}
					case -10:
					{
						mul = 0.03;
						break;
					}
					default:
					{
						mul = 1;
						break;
					}
				}
				
				xp *= mul;
				sp *= mul;
			}
		}
		
		return new double[]
		{
			xp,
			sp
		};
	}
	
	public double calculateOverhitExp(double exp)
	{
		// Get the percentage based on the total of extra (over-hit) damage done relative to the total (maximum) ammount of HP on the Attackable
		double overhitPercentage = ((_overhitDamage * 100) / getMaxHp());
		
		// Over-hit damage percentages are limited to 25% max
		if (overhitPercentage > 25)
		{
			overhitPercentage = 25;
		}
		
		// Get the overhit exp bonus according to the above over-hit damage percentage
		// (1/1 basis - 13% of over-hit damage, 13% of extra exp is given, and so on...)
		return (overhitPercentage / 100) * exp;
	}
	
	/**
	 * Return True.
	 */
	@Override
	public boolean canBeAttacked()
	{
		return true;
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		
		// Clear mob spoil, seed
		setSpoilerObjectId(0);
		
		// Clear all aggro char from list
		clearAggroList();
		
		// Clear Harvester reward
		_harvestItem.set(null);
		
		// fake players
		if (isFakePlayer())
		{
			getFakePlayerDrops().clear(); // Clear existing fake player drops
			setKarma(0); // reset karma
			setScriptValue(0); // remove pvp flag
			setRunning(); // don't walk
		}
		else
		{
			setWalking();
		}
		
		// Clear mod Seeded stat
		_seeded = false;
		_seed = null;
		_seederObjId = 0;
		
		// Clear overhit value
		overhitEnabled(false);
		
		_sweepItems.set(null);
		resetAbsorbList();
		
		setWalking();
		
		// Check the region where this mob is, do not activate the AI if region is inactive.
		if (hasAI())
		{
			// Set the intention of the Attackable to ACTIVE
			getAI().setIntention(Intention.ACTIVE);
			
			// Check the region where this mob is, do not activate the AI if region is inactive.
			if (!isInActiveRegion())
			{
				getAI().stopAITask();
			}
		}
	}
	
	/**
	 * Checks if it is spoiled.
	 * @return {@code true} if it is spoiled, {@code false} otherwise
	 */
	public boolean isSpoiled()
	{
		return _spoilerObjectId != 0;
	}
	
	/**
	 * Gets the spoiler object ID.
	 * @return the spoiler object ID if it is spoiled, 0 otherwise
	 */
	public int getSpoilerObjectId()
	{
		return _spoilerObjectId;
	}
	
	/**
	 * Sets the spoiler object ID.
	 * @param spoilerObjectId spoilerObjectId the spoiler object ID
	 */
	public void setSpoilerObjectId(int spoilerObjectId)
	{
		_spoilerObjectId = spoilerObjectId;
	}
	
	/**
	 * Sets state of the mob to seeded. Parameters needed to be set before.
	 * @param seeder
	 */
	public void setSeeded(Player seeder)
	{
		if ((_seed == null) || (_seederObjId != seeder.getObjectId()))
		{
			return;
		}
		
		_seeded = true;
		int count = 1;
		for (int skillId : getTemplate().getSkills().keySet())
		{
			switch (skillId)
			{
				case 4303: // Strong type x2
				{
					count *= 2;
					break;
				}
				case 4304: // Strong type x3
				{
					count *= 3;
					break;
				}
				case 4305: // Strong type x4
				{
					count *= 4;
					break;
				}
				case 4306: // Strong type x5
				{
					count *= 5;
					break;
				}
				case 4307: // Strong type x6
				{
					count *= 6;
					break;
				}
				case 4308: // Strong type x7
				{
					count *= 7;
					break;
				}
				case 4309: // Strong type x8
				{
					count *= 8;
					break;
				}
				case 4310: // Strong type x9
				{
					count *= 9;
					break;
				}
			}
		}
		
		// hi-level mobs bonus
		final int diff = getLevel() - _seed.getLevel() - 5;
		if (diff > 0)
		{
			count += diff;
		}
		
		_harvestItem.set(new ItemHolder(_seed.getCropId(), count * RatesConfig.RATE_DROP_MANOR));
	}
	
	/**
	 * Sets the seed parameters, but not the seed state
	 * @param seed - instance {@link Seed} of used seed
	 * @param seeder - player who sows the seed
	 */
	public void setSeeded(Seed seed, Player seeder)
	{
		if (_seeded)
		{
			return;
		}
		
		_seed = seed;
		_seederObjId = seeder.getObjectId();
	}
	
	public int getSeederId()
	{
		return _seederObjId;
	}
	
	public Seed getSeed()
	{
		return _seed;
	}
	
	public boolean isSeeded()
	{
		return _seeded;
	}
	
	/**
	 * Set delay for onKill() call, in ms Default: 5000 ms
	 * @param delay
	 */
	public void setOnKillDelay(int delay)
	{
		_onKillDelay = delay;
	}
	
	public int getOnKillDelay()
	{
		return _onKillDelay;
	}
	
	/**
	 * Check if the server allows Random Animation.
	 */
	// This is located here because Monster and FriendlyMob both extend this class. The other non-pc instances extend either Npc or Monster.
	@Override
	public boolean hasRandomAnimation()
	{
		return ((GeneralConfig.MAX_MONSTER_ANIMATION > 0) && isRandomAnimationEnabled() && !(this instanceof GrandBoss));
	}
	
	public void setCommandChannelTimer(CommandChannelTimer commandChannelTimer)
	{
		_commandChannelTimer = commandChannelTimer;
	}
	
	public CommandChannelTimer getCommandChannelTimer()
	{
		return _commandChannelTimer;
	}
	
	public CommandChannel getFirstCommandChannelAttacked()
	{
		return _firstCommandChannelAttacked;
	}
	
	public void setFirstCommandChannelAttacked(CommandChannel firstCommandChannelAttacked)
	{
		_firstCommandChannelAttacked = firstCommandChannelAttacked;
	}
	
	/**
	 * @return the _commandChannelLastAttack
	 */
	public long getCommandChannelLastAttack()
	{
		return _commandChannelLastAttack;
	}
	
	/**
	 * @param channelLastAttack the _commandChannelLastAttack to set
	 */
	public void setCommandChannelLastAttack(long channelLastAttack)
	{
		_commandChannelLastAttack = channelLastAttack;
	}
	
	public void returnHome()
	{
		clearAggroList();
		
		if (hasAI() && (getSpawn() != null))
		{
			getAI().setIntention(Intention.MOVE_TO, getSpawn().getLocation());
		}
	}
	
	/*
	 * Return vitality points decrease (if positive) or increase (if negative) based on damage. Maximum for damage = maxHp.
	 */
	public float getVitalityPoints(int level, long damage)
	{
		// sanity check
		if (damage <= 0)
		{
			return 0;
		}
		
		final long expReward = getExpReward(level);
		final float divider = (getLevel() > 0) && (expReward > 0) ? (getTemplate().getBaseHpMax() * 9 * getLevel() * getLevel()) / (100 * expReward) : 0;
		if (divider == 0)
		{
			return 0;
		}
		
		// negative value - vitality will be consumed
		return -Math.min(damage, getMaxHp()) / divider;
	}
	
	/*
	 * True if vitality rate for exp and sp should be applied
	 */
	public boolean useVitalityRate()
	{
		return !_champion || ChampionMonstersConfig.CHAMPION_ENABLE_VITALITY;
	}
	
	/** Return True if the Creature is RaidBoss or his minion. */
	@Override
	public boolean isRaid()
	{
		return _isRaid;
	}
	
	/**
	 * Set this Npc as a Raid instance.
	 * @param isRaid
	 */
	public void setIsRaid(boolean isRaid)
	{
		_isRaid = isRaid;
	}
	
	/**
	 * Set this Npc as a Minion instance.
	 * @param value
	 */
	public void setIsRaidMinion(boolean value)
	{
		_isRaid = value;
		_isRaidMinion = value;
	}
	
	@Override
	public boolean isRaidMinion()
	{
		return _isRaidMinion;
	}
	
	@Override
	public boolean isMinion()
	{
		return getLeader() != null;
	}
	
	/**
	 * @return leader of this minion or null.
	 */
	public Attackable getLeader()
	{
		return null;
	}
	
	public void setChampion(boolean champ)
	{
		_champion = champ;
		if (ChampionMonstersConfig.SHOW_CHAMPION_AURA)
		{
			setTeam(champ ? Team.RED : Team.NONE);
		}
	}
	
	@Override
	public boolean isChampion()
	{
		return _champion;
	}
	
	@Override
	public boolean isAttackable()
	{
		return true;
	}
	
	@Override
	public Attackable asAttackable()
	{
		return this;
	}
}
