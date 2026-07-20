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
package org.l2jmobius.gameserver.taskmanagers;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.custom.AutoPlayConfig;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.managers.PhantomManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.actor.instance.Monster;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.item.Weapon;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * @author Mobius
 */
public class AutoPlayTaskManager
{
	private static final Set<Set<Player>> POOLS = ConcurrentHashMap.newKeySet();
	private static final Map<Player, Integer> IDLE_COUNT = new ConcurrentHashMap<>();
	private static final int POOL_SIZE = 200;
	private static final int TASK_DELAY = 700;
	private static final Integer AUTO_ATTACK_ACTION = 2;
	
	protected AutoPlayTaskManager()
	{
	}
	
	private class AutoPlay implements Runnable
	{
		private final Set<Player> _players;
		
		public AutoPlay(Set<Player> players)
		{
			_players = players;
		}
		
		@Override
		public void run()
		{
			if (_players.isEmpty())
			{
				return;
			}
			
			PLAY: for (Player player : _players)
			{
				if (!player.isOnline() || (player.isInOfflineMode() && !player.isOfflinePlay()) || !AutoPlayConfig.ENABLE_AUTO_PLAY)
				{
					stopAutoPlay(player);
					continue PLAY;
				}
				
				if (player.isSitting() || player.isCastingNow() || (player.getQueuedSkill() != null))
				{
					continue PLAY;
				}
				
				// Next target mode.
				final int targetMode = player.getAutoPlaySettings().getNextTargetMode();
				
				// Skip thinking.
				final WorldObject target = player.getTarget();
				if ((target != null) && target.isCreature())
				{
					final Creature creature = target.asCreature();
					if (creature.isAlikeDead() || !isTargetModeValid(targetMode, player, creature))
					{
						// Logic for Spoil (254) skill.
						if (creature.isMonster() && creature.isDead() && player.getAutoUseSettings().getAutoSkills().contains(254))
						{
							final Skill sweeper = player.getKnownSkill(42);
							if (sweeper != null)
							{
								final Monster monster = target.asMonster();
								if (monster.checkSpoilOwner(player, false))
								{
									// Move to target.
									if (player.calculateDistance2D(target) > 40)
									{
										if (!player.isMoving())
										{
											player.getAI().setIntention(Intention.MOVE_TO, target);
										}
										
										continue PLAY;
									}
									
									// Sweep target.
									player.doCast(sweeper);
									continue PLAY;
								}
							}
						}
						
						// Clear target.
						player.setTarget(null);
					}
					else if ((creature.getTarget() == player) || (creature.getTarget() == null))
					{
						// GeoEngine can see target check.
						if (!GeoEngine.getInstance().canSeeTarget(player, creature))
						{
							player.setTarget(null);
							continue PLAY;
						}
						
						// Logic adjustment for summons not attacking.
						final Summon summon = player.getSummon();
						if ((summon != null) && summon.hasAI() && !summon.isMoving() && !summon.isDisabled() && (summon.getAI().getIntention() != Intention.ATTACK) && (summon.getAI().getIntention() != Intention.CAST) && creature.isAutoAttackable(player) && GeoEngine.getInstance().canSeeTarget(player, creature))
						{
							summon.getAI().setIntention(Intention.ATTACK, creature);
						}
						
						// We take granted that mage classes do not auto hit.
						if (isMageCaster(player))
						{
							continue PLAY;
						}
						
						// Check if actually attacking.
						if (player.hasAI() && !player.isAttackingNow() && !player.isCastingNow() && !player.isMoving() && !player.isDisabled())
						{
							if (player.getAI().getIntention() != Intention.ATTACK)
							{
								if (creature.isAutoAttackable(player))
								{
									// GeoEngine can see target check.
									if (!GeoEngine.getInstance().canSeeTarget(player, creature))
									{
										player.setTarget(null);
										continue PLAY;
									}
									
									player.getAI().setIntention(Intention.ATTACK, creature);
								}
							}
							else if (creature.hasAI() && !creature.getAI().isAutoAttacking())
							{
								final Weapon weapon = player.getActiveWeaponItem();
								if (weapon != null)
								{
									final int idleCount = IDLE_COUNT.getOrDefault(player, 0);
									if (idleCount > 10)
									{
										final boolean ranged = weapon.getItemType().isRanged();
										final double angle = LocationUtil.calculateHeadingFrom(player, creature);
										final double radian = Math.toRadians(angle);
										final double course = Math.toRadians(180);
										final double distance = (ranged ? player.getCollisionRadius() : player.getCollisionRadius() + creature.getTemplate().getCollisionRadius()) * 2;
										final int x1 = (int) (Math.cos(Math.PI + radian + course) * distance);
										final int y1 = (int) (Math.sin(Math.PI + radian + course) * distance);
										final Location location;
										if (ranged)
										{
											location = new Location(player.getX() + x1, player.getY() + y1, player.getZ());
										}
										else
										{
											location = new Location(creature.getX() + x1, creature.getY() + y1, player.getZ());
										}
										
										player.getAI().setIntention(Intention.MOVE_TO, location);
										IDLE_COUNT.remove(player);
									}
									else
									{
										IDLE_COUNT.put(player, idleCount + 1);
									}
								}
							}
						}
						
						continue PLAY;
					}
				}
				
				// Reset idle count.
				IDLE_COUNT.remove(player);
				
				// Pickup.
				if (player.getAutoPlaySettings().doPickup() && player.isInventoryUnder90(false))
				{
					PICKUP: for (Item droppedItem : World.getInstance().getVisibleObjectsInRange(player, Item.class, 200))
					{
						// Check if item is reachable.
						if ((droppedItem == null) //
							|| (!droppedItem.isSpawned()) //
							|| AutoPlayConfig.IGNORED_AUTO_PICK_ITEMS.contains(droppedItem.getId()) //
							|| !GeoEngine.getInstance().canMoveToTarget(player.getX(), player.getY(), player.getZ(), droppedItem.getX(), droppedItem.getY(), droppedItem.getZ(), player.getInstanceId()))
						{
							continue PICKUP;
						}
						
						// Move to item.
						if (player.calculateDistance2D(droppedItem) > 20)
						{
							if (!player.isMoving())
							{
								player.getAI().setIntention(Intention.MOVE_TO, droppedItem);
							}
							
							continue PLAY;
						}
						
						// Try to pick it up.
						if (!droppedItem.isProtected() || (droppedItem.getOwnerId() == player.getObjectId()))
						{
							player.doPickupItem(droppedItem);
							continue PLAY; // Avoid pickup being skipped.
						}
					}
				}
				
				// Find target.
				// Recruited fake-party members never use the stock auto-assist branch: their assist mode is driven by
				// PhantomPartyManager (which stops the scanner entirely), so a recruit running AutoPlay is always in
				// "attack freely" mode and must scan for its own targets. Without this exclusion the branch pinned a
				// partied free-hunting recruit to the leader's target (or FOLLOW when the leader had none), so it
				// neither hunted nor fought back when attacked.
				Creature creature = null;
				final Party party = player.getParty();
				final Player leader = party == null ? null : party.getLeader();
				if (AutoPlayConfig.ENABLE_AUTO_ASSIST && (party != null) && (leader != null) && (leader != player) && !leader.isDead() && !PhantomManager.getInstance().isRecruit(player))
				{
					if (leader.calculateDistance3D(player) < (PlayerConfig.ALT_PARTY_RANGE * 2 /* 2? */))
					{
						final WorldObject leaderTarget = leader.getTarget();
						if ((leaderTarget != null) && (leaderTarget.isAttackable() || (leaderTarget.isPlayable() && !party.containsPlayer(leaderTarget.asPlayer()))))
						{
							creature = leaderTarget.asCreature();
						}
						else if ((player.getAI().getIntention() != Intention.FOLLOW) && !player.isDisabled())
						{
							player.getAI().setIntention(Intention.FOLLOW, leader);
						}
					}
				}
				else
				{
					double closestDistance = Double.MAX_VALUE;
					TARGET: for (Creature nearby : World.getInstance().getVisibleObjectsInRange(player, Creature.class, player.getAutoPlaySettings().isShortRange() && (targetMode != 2 /* Characters */) ? AutoPlayConfig.AUTO_PLAY_SHORT_RANGE : AutoPlayConfig.AUTO_PLAY_LONG_RANGE))
					{
						// Skip unavailable creatures.
						if ((nearby == null) || nearby.isAlikeDead())
						{
							continue TARGET;
						}
						
						// Check creature target.
						if (player.getAutoPlaySettings().isRespectfulHunting() && !nearby.isPlayable() && (nearby.getTarget() != null) && (nearby.getTarget() != player) && !(player.hasSummon() && (player.getSummon().getObjectId() == nearby.getTarget().getObjectId())))
						{
							continue TARGET;
						}
						
						// Check next target mode.
						if (!isTargetModeValid(targetMode, player, nearby))
						{
							continue TARGET;
						}
						
						// Check if creature is reachable.
						if ((Math.abs(player.getZ() - nearby.getZ()) < 800) && GeoEngine.getInstance().canSeeTarget(player, nearby) && GeoEngine.getInstance().canMoveToTarget(player.getX(), player.getY(), player.getZ(), nearby.getX(), nearby.getY(), nearby.getZ(), player.getInstanceId()))
						{
							final double creatureDistance = player.calculateDistance2D(nearby);
							if (creatureDistance < closestDistance)
							{
								creature = nearby;
								closestDistance = creatureDistance;
							}
						}
					}
				}
				
				// New target was assigned.
				if (creature != null)
				{
					player.setTarget(creature);
					
					// We take granted that mage classes do not auto hit.
					if (isMageCaster(player))
					{
						continue PLAY;
					}
					
					player.getAI().setIntention(Intention.ATTACK, creature);
				}
			}
		}
		
		private boolean isMageCaster(Player player)
		{
			return !player.getAutoUseSettings().getAutoActions().contains(AUTO_ATTACK_ACTION);
		}
		
		private boolean isTargetModeValid(int mode, Player player, Creature creature)
		{
			if (!creature.isTargetable() || (creature.isNpc() && (creature.isInvul() || !creature.asNpc().isShowName())))
			{
				return false;
			}
			
			switch (mode)
			{
				case 1: // Monster
				{
					return creature.isMonster() && !creature.isRaid() && creature.isAutoAttackable(player);
				}
				case 2: // Characters
				{
					return creature.isPlayable() && creature.isAutoAttackable(player);
				}
				case 3: // NPC
				{
					return creature.isNpc() && !creature.isMonster() && !creature.isInsideZone(ZoneId.PEACE);
				}
				default: // Any Target
				{
					return (creature.isNpc() && !creature.isInsideZone(ZoneId.PEACE)) || (creature.isPlayable() && creature.isAutoAttackable(player));
				}
			}
		}
	}
	
	public synchronized void startAutoPlay(Player player)
	{
		for (Set<Player> pool : POOLS)
		{
			if (pool.contains(player))
			{
				return;
			}
		}
		
		player.setAutoPlaying(true);
		
		for (Set<Player> pool : POOLS)
		{
			if (pool.size() < POOL_SIZE)
			{
				player.onActionRequest();
				pool.add(player);
				return;
			}
		}
		
		final Set<Player> pool = ConcurrentHashMap.newKeySet(POOL_SIZE);
		player.onActionRequest();
		pool.add(player);
		ThreadPool.schedulePriorityTaskAtFixedRate(new AutoPlay(pool), TASK_DELAY, TASK_DELAY);
		POOLS.add(pool);
	}
	
	public void stopAutoPlay(Player player)
	{
		for (Set<Player> pool : POOLS)
		{
			if (pool.remove(player))
			{
				player.setAutoPlaying(false);
				
				// Pets must follow their owner.
				if (player.hasServitor() || player.hasPet())
				{
					player.getSummon().followOwner();
				}
				
				IDLE_COUNT.remove(player);
				return;
			}
		}
	}
	
	public static AutoPlayTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final AutoPlayTaskManager INSTANCE = new AutoPlayTaskManager();
	}
}
