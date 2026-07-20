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
package org.l2jmobius.gameserver.ai;

import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.WorldRegion;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.interfaces.ILocational;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.AutoAttackStart;
import org.l2jmobius.gameserver.network.serverpackets.AutoAttackStop;
import org.l2jmobius.gameserver.network.serverpackets.Die;
import org.l2jmobius.gameserver.network.serverpackets.MoveToLocation;
import org.l2jmobius.gameserver.network.serverpackets.MoveToPawn;
import org.l2jmobius.gameserver.network.serverpackets.StopMove;
import org.l2jmobius.gameserver.taskmanagers.AttackStanceTaskManager;
import org.l2jmobius.gameserver.taskmanagers.CreatureFollowTaskManager;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;

/**
 * Mother class of all objects AI in the world.<br>
 * AbastractAI:<br>
 * <li>CreatureAI</li>
 */
public abstract class AbstractAI
{
	/** The creature that this AI manages */
	protected final Creature _actor;
	
	/** Current long-term intention */
	protected Intention _intention = Intention.IDLE;
	/** Current long-term intention parameter */
	protected Object _intentionArg0 = null;
	/** Current long-term intention parameter */
	protected Object _intentionArg1 = null;
	
	/** Flags about client's state, in order to know which messages to send */
	protected volatile boolean _clientAutoAttacking;
	/** Flags about client's state, in order to know which messages to send */
	protected int _clientMovingToPawnOffset;
	
	/** Different targets this AI maintains */
	private WorldObject _target;
	private Creature _castTarget;
	protected Creature _attackTarget;
	protected Creature _followTarget;
	
	/** The skill we are currently casting by INTENTION_CAST */
	protected Skill _skill;
	
	/** Different internal state flags */
	private int _moveToPawnTimeout;
	
	private NextAction _nextAction;
	
	/**
	 * @return the _nextAction
	 */
	public NextAction getNextAction()
	{
		return _nextAction;
	}
	
	/**
	 * @param nextAction the next action to set.
	 */
	public void setNextAction(NextAction nextAction)
	{
		_nextAction = nextAction;
	}
	
	protected AbstractAI(Creature creature)
	{
		_actor = creature;
	}
	
	/**
	 * @return the Creature managed by this Accessor AI.
	 */
	public Creature getActor()
	{
		return _actor;
	}
	
	/**
	 * @return the current Intention.
	 */
	public Intention getIntention()
	{
		return _intention;
	}
	
	/**
	 * Set the Intention of this AbstractAI.<br>
	 * <font color=#FF0000><b><u>Caution</u>: This method is USED by AI classes</b></font><b><u><br>
	 * Overridden in</u>:</b><br>
	 * <b>AttackableAI</b> : Create an AI Task executed every 1s (if necessary)<br>
	 * <b>PlayerAI</b> : Stores the current AI intention parameters to later restore it if necessary.
	 * @param intention The new Intention to set to the AI
	 * @param arg0 The first parameter of the Intention
	 * @param arg1 The second parameter of the Intention
	 */
	synchronized void changeIntention(Intention intention, Object arg0, Object arg1)
	{
		_intention = intention;
		_intentionArg0 = arg0;
		_intentionArg1 = arg1;
	}
	
	/**
	 * Launch the CreatureAI onIntention method corresponding to the new Intention.<br>
	 * <font color=#FF0000><b><u>Caution</u>: Stop the FOLLOW mode if necessary</b></font>
	 * @param intention The new Intention to set to the AI
	 */
	public void setIntention(Intention intention)
	{
		setIntention(intention, null, null);
	}
	
	/**
	 * Launch the CreatureAI onIntention method corresponding to the new Intention.<br>
	 * <font color=#FF0000><b><u>Caution</u>: Stop the FOLLOW mode if necessary</b></font>
	 * @param intention The new Intention to set to the AI
	 * @param arg0 The first parameter of the Intention (optional target)
	 */
	public void setIntention(Intention intention, Object arg0)
	{
		setIntention(intention, arg0, null);
	}
	
	/**
	 * Launch the CreatureAI onIntention method corresponding to the new Intention.<br>
	 * <font color=#FF0000><b><u>Caution</u>: Stop the FOLLOW mode if necessary</b></font>
	 * @param intention The new Intention to set to the AI
	 * @param arg0 The first parameter of the Intention (optional target)
	 * @param arg1 The second parameter of the Intention (optional target)
	 */
	public void setIntention(Intention intention, Object arg0, Object arg1)
	{
		// Stop the follow mode if necessary
		if ((intention != Intention.FOLLOW) && (intention != Intention.ATTACK))
		{
			stopFollow();
		}
		
		// Launch the onIntention method of the CreatureAI corresponding to the new Intention
		switch (intention)
		{
			case IDLE:
			{
				onIntentionIdle();
				break;
			}
			case ACTIVE:
			{
				onIntentionActive();
				break;
			}
			case REST:
			{
				onIntentionRest();
				break;
			}
			case ATTACK:
			{
				onIntentionAttack((Creature) arg0);
				break;
			}
			case CAST:
			{
				onIntentionCast((Skill) arg0, (WorldObject) arg1);
				break;
			}
			case MOVE_TO:
			{
				onIntentionMoveTo((ILocational) arg0);
				break;
			}
			case FOLLOW:
			{
				onIntentionFollow((Creature) arg0);
				break;
			}
			case PICK_UP:
			{
				onIntentionPickUp((WorldObject) arg0);
				break;
			}
			case INTERACT:
			{
				onIntentionInteract((WorldObject) arg0);
				break;
			}
		}
		
		// If do move or follow intention drop next action.
		final NextAction nextAction = _nextAction;
		if ((nextAction != null) && nextAction.isRemovedBy(intention))
		{
			_nextAction = null;
		}
	}
	
	/**
	 * Launch the CreatureAI onAction method corresponding to the Action.<br>
	 * <font color=#FF0000><b><u>Caution</u>: The current general intention won't be change (ex : If the character attack and is stunned, he will attack again after the stunned period)</b></font>
	 * @param action The action whose the AI must be notified
	 */
	public void notifyAction(Action action)
	{
		notifyAction(action, null, null);
	}
	
	/**
	 * Launch the CreatureAI onAction method corresponding to the Action.<br>
	 * <font color=#FF0000><b><u>Caution</u>: The current general intention won't be change (ex : If the character attack and is stunned, he will attack again after the stunned periode)</b></font>
	 * @param action The action whose the AI must be notified
	 * @param arg0 The first parameter of the Action (optional target)
	 */
	public void notifyAction(Action action, Object arg0)
	{
		notifyAction(action, arg0, null);
	}
	
	/**
	 * Launch the CreatureAI onAction method corresponding to the Action.<br>
	 * <font color=#FF0000><b><u>Caution</u>: The current general intention won't be change (ex : If the character attack and is stunned, he will attack again after the stunned periode)</b></font>
	 * @param action The action whose the AI must be notified
	 * @param args The arguments of the Action
	 */
	public void notifyAction(Action action, Object... args)
	{
		if ((!_actor.isSpawned() && !_actor.isTeleporting()) || !_actor.hasAI())
		{
			return;
		}
		
		switch (action)
		{
			case THINK:
			{
				onActionThink();
				break;
			}
			case ATTACKED:
			{
				onActionAttacked((Creature) args[0]);
				break;
			}
			case AGGRESSION:
			{
				onActionAggression((Creature) args[0], ((Number) args[1]).intValue());
				break;
			}
			case STUNNED:
			{
				onActionStunned((Creature) args[0]);
				break;
			}
			case PARALYZED:
			{
				onActionParalyzed((Creature) args[0]);
				break;
			}
			case SLEEPING:
			{
				onActionSleeping((Creature) args[0]);
				break;
			}
			case ROOTED:
			{
				onActionRooted((Creature) args[0]);
				break;
			}
			case CONFUSED:
			{
				onActionConfused((Creature) args[0]);
				break;
			}
			case MUTED:
			{
				onActionMuted((Creature) args[0]);
				break;
			}
			case EVADED:
			{
				onActionEvaded((Creature) args[0]);
				break;
			}
			case READY_TO_ACT:
			{
				if (!_actor.isCastingNow() && !_actor.isCastingSimultaneouslyNow())
				{
					onActionReadyToAct();
				}
				break;
			}
			case USER_CMD:
			{
				onActionUserCmd(args[0], args[1]);
				break;
			}
			case ARRIVED:
			{
				// happens e.g. from stopmove but we don't process it if we're casting
				if (!_actor.isCastingNow() && !_actor.isCastingSimultaneouslyNow())
				{
					onActionArrived();
				}
				break;
			}
			case ARRIVED_REVALIDATE:
			{
				// this is disregarded if the char is not moving any more
				if (_actor.isMoving())
				{
					onActionArrivedRevalidate();
				}
				break;
			}
			case ARRIVED_BLOCKED:
			{
				onActionArrivedBlocked((Location) args[0]);
				break;
			}
			case FORGET_OBJECT:
			{
				final WorldObject worldObject = (WorldObject) args[0];
				_actor.removeSeenCreature(worldObject);
				onActionForgetObject(worldObject);
				break;
			}
			case CANCEL:
			{
				onActionCancel();
				break;
			}
			case DEATH:
			{
				onActionDeath();
				break;
			}
			case FAKE_DEATH:
			{
				onActionFakeDeath();
				break;
			}
			case FINISH_CASTING:
			{
				onActionFinishCasting();
				break;
			}
			case AFRAID:
			{
				onActionAfraid((Creature) args[0], (Boolean) args[1]);
				break;
			}
		}
		
		// Do next action.
		final NextAction nextAction = _nextAction;
		if ((nextAction != null) && nextAction.isTriggeredBy(action))
		{
			nextAction.doAction();
		}
	}
	
	protected abstract void onIntentionIdle();
	
	protected abstract void onIntentionActive();
	
	protected abstract void onIntentionRest();
	
	protected abstract void onIntentionAttack(Creature target);
	
	protected abstract void onIntentionCast(Skill skill, WorldObject target);
	
	protected abstract void onIntentionMoveTo(ILocational destination);
	
	protected abstract void onIntentionFollow(Creature target);
	
	protected abstract void onIntentionPickUp(WorldObject item);
	
	protected abstract void onIntentionInteract(WorldObject object);
	
	protected abstract void onActionThink();
	
	protected abstract void onActionAttacked(Creature attacker);
	
	protected abstract void onActionAggression(Creature target, int aggro);
	
	protected abstract void onActionStunned(Creature attacker);
	
	protected abstract void onActionParalyzed(Creature attacker);
	
	protected abstract void onActionSleeping(Creature attacker);
	
	protected abstract void onActionRooted(Creature attacker);
	
	protected abstract void onActionConfused(Creature attacker);
	
	protected abstract void onActionMuted(Creature attacker);
	
	protected abstract void onActionEvaded(Creature attacker);
	
	protected abstract void onActionReadyToAct();
	
	protected abstract void onActionUserCmd(Object arg0, Object arg1);
	
	protected abstract void onActionArrived();
	
	protected abstract void onActionArrivedRevalidate();
	
	protected abstract void onActionArrivedBlocked(Location location);
	
	protected abstract void onActionForgetObject(WorldObject object);
	
	protected abstract void onActionCancel();
	
	protected abstract void onActionDeath();
	
	protected abstract void onActionFakeDeath();
	
	protected abstract void onActionFinishCasting();
	
	protected abstract void onActionAfraid(Creature effector, boolean start);
	
	/**
	 * Cancel action client side by sending Server->Client packet ActionFailed to the Player actor.<br>
	 * <font color=#FF0000><b><u>Caution</u>: Low level function, used by AI subclasses</b></font>
	 */
	protected void clientActionFailed()
	{
		if (_actor.isPlayer())
		{
			_actor.sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
	
	/**
	 * Move the actor to Pawn server side AND client side by sending Server->Client packet MoveToPawn <i>(broadcast)</i>.<br>
	 * <font color=#FF0000><b><u>Caution</u>: Low level function, used by AI subclasses</b></font>
	 * @param pawn
	 * @param offsetValue
	 */
	public void moveToPawn(WorldObject pawn, int offsetValue)
	{
		// Check if actor can move.
		if (!_actor.isMovementDisabled() && !_actor.isAttackingNow() && !_actor.isCastingNow())
		{
			int offset = offsetValue;
			if (offset < 10)
			{
				offset = 10;
			}
			
			// Prevent possible extra calls to this function (there is none?), also don't send movetopawn packets too often.
			final int gameTime = GameTimeTaskManager.getInstance().getGameTicks();
			if (_actor.isMoving() && (_target == pawn))
			{
				if (_clientMovingToPawnOffset == offset)
				{
					if (gameTime < _moveToPawnTimeout)
					{
						return;
					}
				}
				// Minimum time to calculate new route is 2 seconds.
				else if (_actor.isOnGeodataPath() && (gameTime < (_moveToPawnTimeout + 10)))
				{
					return;
				}
			}
			
			// Set AI movement data
			_clientMovingToPawnOffset = offset;
			_target = pawn;
			_moveToPawnTimeout = gameTime;
			_moveToPawnTimeout += 1000 / GameTimeTaskManager.MILLIS_IN_TICK;
			
			if (pawn == null)
			{
				return;
			}
			
			// Calculate movement data for a move to location action and add the actor to movingObjects of GameTimeTaskManager
			_actor.moveToLocation(pawn.getX(), pawn.getY(), pawn.getZ(), offset);
			
			// May result to make monsters stop moving.
			// if (!_actor.isMoving())
			// {
			// clientActionFailed();
			// return;
			// }
			
			// Send a Server->Client packet MoveToPawn/MoveToLocation to the actor and all Player in its _knownPlayers
			if (pawn.isCreature())
			{
				if (_actor.isOnGeodataPath())
				{
					_actor.broadcastMoveToLocation();
					_clientMovingToPawnOffset = 0;
				}
				else
				{
					final WorldRegion region = _actor.getWorldRegion();
					if ((region != null) && region.isActive())
					{
						_actor.broadcastPacket(new MoveToPawn(_actor, pawn, offset));
					}
				}
			}
			else
			{
				_actor.broadcastMoveToLocation();
			}
		}
		else
		{
			clientActionFailed();
		}
	}
	
	public void moveTo(ILocational loc)
	{
		moveTo(loc.getX(), loc.getY(), loc.getZ());
	}
	
	/**
	 * Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet MoveToLocation <i>(broadcast)</i>.<br>
	 * <font color=#FF0000><b><u>Caution</u>: Low level function, used by AI subclasses</b></font>
	 * @param x
	 * @param y
	 * @param z
	 */
	protected void moveTo(int x, int y, int z)
	{
		// Check if actor can move
		if (!_actor.isMovementDisabled())
		{
			// Set AI movement data
			_clientMovingToPawnOffset = 0;
			
			// Calculate movement data for a move to location action and add the actor to movingObjects of GameTimeTaskManager
			_actor.moveToLocation(x, y, z, 0);
			
			// Send a Server->Client packet MoveToLocation to the actor and all Player in its _knownPlayers
			_actor.broadcastMoveToLocation();
		}
		else
		{
			clientActionFailed();
		}
	}
	
	/**
	 * Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation <i>(broadcast)</i>.<br>
	 * <font color=#FF0000><b><u>Caution</u>: Low level function, used by AI subclasses</b></font>
	 * @param loc
	 */
	public void clientStopMoving(Location loc)
	{
		// Stop movement of the Creature
		if (_actor.isMoving())
		{
			_actor.stopMove(loc);
		}
		
		_clientMovingToPawnOffset = 0;
	}
	
	/**
	 * Client has already arrived to target, no need to force StopMove packet.
	 */
	protected void clientStoppedMoving()
	{
		if (_clientMovingToPawnOffset > 0) // movetoPawn needs to be stopped
		{
			_clientMovingToPawnOffset = 0;
			_actor.broadcastPacket(new StopMove(_actor));
		}
	}
	
	public boolean isAutoAttacking()
	{
		return _clientAutoAttacking;
	}
	
	public void setAutoAttacking(boolean isAutoAttacking)
	{
		if (_actor.isSummon())
		{
			final Summon summon = _actor.asSummon();
			if (summon.getOwner() != null)
			{
				summon.getOwner().getAI().setAutoAttacking(isAutoAttacking);
			}
			return;
		}
		
		_clientAutoAttacking = isAutoAttacking;
	}
	
	/**
	 * Start the actor Auto Attack client side by sending Server->Client packet AutoAttackStart <i>(broadcast)</i>.<br>
	 * <font color=#FF0000><b><u>Caution</u>: Low level function, used by AI subclasses</b></font>
	 */
	public void clientStartAutoAttack()
	{
		// Non attackable NPCs should not get in combat.
		if (_actor.isNpc() && (!_actor.isAttackable() || _actor.isCoreAIDisabled()))
		{
			return;
		}
		
		if (_actor.isSummon())
		{
			final Summon summon = _actor.asSummon();
			if (summon.getOwner() != null)
			{
				summon.getOwner().getAI().clientStartAutoAttack();
			}
			return;
		}
		
		if (!_clientAutoAttacking)
		{
			if (_actor.isPlayer())
			{
				final Player player = _actor.asPlayer();
				if (player.hasSummon())
				{
					final Summon summon = player.getSummon();
					summon.broadcastPacket(new AutoAttackStart(summon.getObjectId()));
				}
			}
			
			// Send a Server->Client packet AutoAttackStart to the actor and all Player in its _knownPlayers
			_actor.broadcastPacket(new AutoAttackStart(_actor.getObjectId()));
			setAutoAttacking(true);
		}
		
		AttackStanceTaskManager.getInstance().addAttackStanceTask(_actor);
	}
	
	/**
	 * Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop <i>(broadcast)</i>.<br>
	 * <font color=#FF0000><b><u>Caution</u>: Low level function, used by AI subclasses</b></font>
	 */
	public void clientStopAutoAttack()
	{
		if (_actor.isSummon())
		{
			final Summon summon = _actor.asSummon();
			if (summon.getOwner() != null)
			{
				summon.getOwner().getAI().clientStopAutoAttack();
			}
			return;
		}
		
		if (_actor.isPlayer())
		{
			if (!AttackStanceTaskManager.getInstance().hasAttackStanceTask(_actor) && isAutoAttacking())
			{
				AttackStanceTaskManager.getInstance().addAttackStanceTask(_actor);
			}
		}
		else if (_clientAutoAttacking)
		{
			_actor.broadcastPacket(new AutoAttackStop(_actor.getObjectId()));
			setAutoAttacking(false);
		}
	}
	
	public int getClientMovingToPawnOffset()
	{
		return _clientMovingToPawnOffset;
	}
	
	/**
	 * Kill the actor client side by sending Server->Client packet AutoAttackStop, StopMove/StopRotation, Die <i>(broadcast)</i>.<br>
	 * <font color=#FF0000><b><u>Caution</u>: Low level function, used by AI subclasses</b></font>
	 */
	protected void clientNotifyDead()
	{
		// Send a Server->Client packet Die to the actor and all Player in its _knownPlayers
		_actor.broadcastPacket(new Die(_actor));
		
		// Init AI
		_intention = Intention.IDLE;
		_target = null;
		_castTarget = null;
		_attackTarget = null;
		
		// Cancel the follow task if necessary
		stopFollow();
	}
	
	/**
	 * Update the state of this actor client side by sending Server->Client packet MoveToPawn/MoveToLocation and AutoAttackStart to the Player player.<br>
	 * <font color=#FF0000><b><u>Caution</u>: Low level function, used by AI subclasses</b></font>
	 * @param player The PlayerIstance to notify with state of this Creature
	 */
	public void describeStateToPlayer(Player player)
	{
		if (_actor.isVisibleFor(player) && _actor.isMoving())
		{
			if ((_clientMovingToPawnOffset != 0) && isFollowing())
			{
				// Send a Server->Client packet MoveToPawn to the actor and all Player in its _knownPlayers
				player.sendPacket(new MoveToPawn(_actor, _followTarget, _clientMovingToPawnOffset));
			}
			else
			{
				// Send a Server->Client packet MoveToLocation to the actor and all Player in its _knownPlayers
				player.sendPacket(new MoveToLocation(_actor));
			}
		}
	}
	
	public boolean isFollowing()
	{
		return (_followTarget != null) && _followTarget.isCreature() && ((_intention == Intention.FOLLOW) || CreatureFollowTaskManager.getInstance().isFollowing(_actor));
	}
	
	/**
	 * Create and Launch an AI Follow Task to execute every 1s.
	 * @param target The Creature to follow
	 */
	public void startFollow(Creature target)
	{
		startFollow(target, -1);
	}
	
	/**
	 * Create and Launch an AI Follow Task to execute every 0.5s, following at specified range.
	 * @param target The Creature to follow
	 * @param range
	 */
	public void startFollow(Creature target, int range)
	{
		stopFollow();
		_followTarget = target;
		if (range == -1)
		{
			CreatureFollowTaskManager.getInstance().addNormalFollow(_actor, range);
		}
		else
		{
			CreatureFollowTaskManager.getInstance().addAttackFollow(_actor, range);
		}
	}
	
	/**
	 * Stop an AI Follow Task.
	 */
	public void stopFollow()
	{
		CreatureFollowTaskManager.getInstance().remove(_actor);
		_followTarget = null;
	}
	
	public Creature getFollowTarget()
	{
		return _followTarget;
	}
	
	protected WorldObject getTarget()
	{
		return _target;
	}
	
	protected void setTarget(WorldObject target)
	{
		_target = target;
	}
	
	protected void setCastTarget(Creature target)
	{
		_castTarget = target;
	}
	
	public Creature getCastTarget()
	{
		return _castTarget;
	}
	
	protected void setAttackTarget(Creature target)
	{
		_attackTarget = target;
	}
	
	public Creature getAttackTarget()
	{
		return _attackTarget;
	}
	
	/**
	 * Stop all Ai tasks and futures.
	 */
	public void stopAITask()
	{
		stopFollow();
	}
	
	@Override
	public String toString()
	{
		return "Actor: " + _actor;
	}
}
