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

import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.player.Duel;
import org.l2jmobius.gameserver.model.actor.instance.StaticObject;
import org.l2jmobius.gameserver.model.interfaces.ILocational;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.targets.TargetType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class PlayerAI extends PlayableAI
{
	private boolean _thinking; // to prevent recursive thinking
	
	private IntentionCommand _nextIntention = null;
	
	public PlayerAI(Player player)
	{
		super(player);
	}
	
	private void saveNextIntention(Intention intention, Object arg0, Object arg1)
	{
		_nextIntention = new IntentionCommand(intention, arg0, arg1);
	}
	
	@Override
	public IntentionCommand getNextIntention()
	{
		return _nextIntention;
	}
	
	/**
	 * Saves the current Intention for this PlayerAI if necessary and calls changeIntention in AbstractAI.
	 * @param intention The new Intention to set to the AI
	 * @param arg0 The first parameter of the Intention
	 * @param arg1 The second parameter of the Intention
	 */
	@Override
	protected synchronized void changeIntention(Intention intention, Object arg0, Object arg1)
	{
		// Forget next if it's not cast or it's cast and skill is toggle.
		if ((intention != Intention.CAST) || ((arg0 != null) && !((Skill) arg0).isToggle()))
		{
			_nextIntention = null;
			super.changeIntention(intention, arg0, arg1);
			return;
		}
		
		// do nothing if next intention is same as current one.
		if ((intention == _intention) && (arg0 == _intentionArg0) && (arg1 == _intentionArg1))
		{
			super.changeIntention(intention, arg0, arg1);
			return;
		}
		
		// save current intention so it can be used after cast
		saveNextIntention(_intention, _intentionArg0, _intentionArg1);
		super.changeIntention(intention, arg0, arg1);
	}
	
	/**
	 * Launch actions corresponding to the Action ReadyToAct.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Launch actions corresponding to the Action Think</li>
	 * </ul>
	 */
	@Override
	protected void onActionReadyToAct()
	{
		// Launch actions corresponding to the Action Think
		if (_nextIntention != null)
		{
			setIntention(_nextIntention._intention, _nextIntention._arg0, _nextIntention._arg1);
			_nextIntention = null;
		}
		
		super.onActionReadyToAct();
	}
	
	@Override
	protected void onActionForgetObject(WorldObject object)
	{
		if (object.isPlayer())
		{
			getActor().getKnownRelations().remove(object.getObjectId());
		}
		
		super.onActionForgetObject(object);
	}
	
	/**
	 * Launch actions corresponding to the Action Cancel.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Stop an AI Follow Task</li>
	 * <li>Launch actions corresponding to the Action Think</li>
	 * </ul>
	 */
	@Override
	protected void onActionCancel()
	{
		_nextIntention = null;
		super.onActionCancel();
	}
	
	/**
	 * Finalize the casting of a skill. This method overrides CreatureAI method.<br>
	 * <b>What it does:</b><br>
	 * Check if actual intention is set to CAST and, if so, retrieves latest intention before the actual CAST and set it as the current intention for the player.
	 */
	@Override
	protected void onActionFinishCasting()
	{
		if (getIntention() == Intention.CAST)
		{
			// run interrupted or next intention
			if (_nextIntention != null)
			{
				if (_nextIntention._intention != Intention.CAST)
				{
					setIntention(_nextIntention._intention, _nextIntention._arg0, _nextIntention._arg1);
				}
				else
				{
					setIntention(Intention.IDLE);
				}
			}
			else
			{
				// set intention to idle if skill doesn't change intention.
				setIntention(Intention.IDLE);
			}
		}
	}
	
	@Override
	protected void onIntentionRest()
	{
		if (getIntention() == Intention.REST)
		{
			return;
		}
		
		changeIntention(Intention.REST, null, null);
		setTarget(null);
		if (getAttackTarget() != null)
		{
			setAttackTarget(null);
		}
		
		clientStopMoving(null);
	}
	
	@Override
	protected void onIntentionActive()
	{
		setIntention(Intention.IDLE);
	}
	
	/**
	 * Manage the Move To Intention : Stop current Attack and Launch a Move to Location Task.<br>
	 * <br>
	 * <b><u>Actions</u> : </b>
	 * <ul>
	 * <li>Stop the actor auto-attack server side AND client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
	 * <li>Set the Intention of this AI to MOVE_TO</li>
	 * <li>Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet MoveToLocation (broadcast)</li>
	 * </ul>
	 */
	@Override
	protected void onIntentionMoveTo(ILocational loc)
	{
		if (getIntention() == Intention.REST)
		{
			// Cancel action client side by sending Server->Client packet ActionFailed to the Player actor
			clientActionFailed();
			return;
		}
		
		final Player player = _actor.asPlayer();
		if (player.getDuelState() == Duel.DUELSTATE_DEAD)
		{
			clientActionFailed();
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_MOVE_WHILE_FROZEN_PLEASE_WAIT));
			return;
		}
		
		if (_actor.isAllSkillsDisabled() || _actor.isCastingNow() || _actor.isAttackingNow())
		{
			clientActionFailed();
			saveNextIntention(Intention.MOVE_TO, loc, null);
			return;
		}
		
		// Set the Intention of this AbstractAI to MOVE_TO
		changeIntention(Intention.MOVE_TO, loc, null);
		
		// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
		clientStopAutoAttack();
		
		// Abort the attack of the Creature and send Server->Client ActionFailed packet
		_actor.abortAttack();
		
		// Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet MoveToLocation (broadcast)
		moveTo(loc.getX(), loc.getY(), loc.getZ());
	}
	
	@Override
	protected void clientNotifyDead()
	{
		_clientMovingToPawnOffset = 0;
		super.clientNotifyDead();
	}
	
	private void thinkAttack()
	{
		final Creature target = getAttackTarget();
		if (target == null)
		{
			return;
		}
		
		if (checkTargetLostOrDead(target))
		{
			// Notify the target
			setAttackTarget(null);
			return;
		}
		
		if (maybeMoveToPawn(target, _actor.getPhysicalAttackRange()))
		{
			return;
		}
		
		clientStopMoving(null);
		_actor.doAttack(target);
	}
	
	private void thinkCast()
	{
		final Creature target = getCastTarget();
		if ((_skill.getTargetType() == TargetType.GROUND) && _actor.isPlayer())
		{
			if (maybeMoveToPosition(_actor.asPlayer().getCurrentSkillWorldPosition(), _actor.getMagicalAttackRange(_skill)))
			{
				_actor.setCastingNow(false);
				return;
			}
		}
		else
		{
			if (checkTargetLost(target))
			{
				if (_skill.hasNegativeEffect() && (getAttackTarget() != null))
				{
					// Notify the target
					setCastTarget(null);
				}
				
				_actor.setCastingNow(false);
				return;
			}
			
			if ((target != null) && maybeMoveToPawn(target, _actor.getMagicalAttackRange(_skill)))
			{
				_actor.setCastingNow(false);
				return;
			}
		}
		
		if ((_skill.getHitTime() > 50) && !_skill.isSimultaneousCast())
		{
			clientStopMoving(null);
		}
		
		// Check if target has changed.
		final WorldObject currentTarget = _actor.getTarget();
		if ((currentTarget != target) && (currentTarget != null) && (target != null))
		{
			_actor.setTarget(target);
			_actor.doCast(_skill);
			_actor.setTarget(currentTarget);
			return;
		}
		
		_actor.doCast(_skill);
	}
	
	private void thinkPickUp()
	{
		if (_actor.isAllSkillsDisabled() || _actor.isCastingNow())
		{
			return;
		}
		
		final WorldObject target = getTarget();
		if (checkTargetLost(target) || maybeMoveToPawn(target, 36))
		{
			return;
		}
		
		setIntention(Intention.IDLE);
		_actor.asPlayer().doPickupItem(target);
	}
	
	private void thinkInteract()
	{
		if (_actor.isAllSkillsDisabled() || _actor.isCastingNow())
		{
			return;
		}
		
		final WorldObject target = getTarget();
		if (checkTargetLost(target) || maybeMoveToPawn(target, 36))
		{
			return;
		}
		
		if (!(target instanceof StaticObject))
		{
			_actor.asPlayer().doInteract(target.asCreature());
		}
		
		setIntention(Intention.IDLE);
	}
	
	@Override
	public void onActionThink()
	{
		if (_thinking && (getIntention() != Intention.CAST))
		{
			return;
		}
		
		_thinking = true;
		try
		{
			if (getIntention() == Intention.ATTACK)
			{
				thinkAttack();
			}
			else if (getIntention() == Intention.CAST)
			{
				thinkCast();
			}
			else if (getIntention() == Intention.PICK_UP)
			{
				thinkPickUp();
			}
			else if (getIntention() == Intention.INTERACT)
			{
				thinkInteract();
			}
		}
		finally
		{
			_thinking = false;
		}
	}
}
