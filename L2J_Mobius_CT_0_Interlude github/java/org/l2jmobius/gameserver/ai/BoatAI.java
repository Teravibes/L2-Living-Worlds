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
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.Boat;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.VehicleDeparture;
import org.l2jmobius.gameserver.network.serverpackets.VehicleInfo;
import org.l2jmobius.gameserver.network.serverpackets.VehicleStarted;

/**
 * @author DS, Mobius
 */
public class BoatAI extends CreatureAI
{
	public BoatAI(Boat boat)
	{
		super(boat);
	}
	
	@Override
	protected void moveTo(int x, int y, int z)
	{
		if (_actor.isMovementDisabled())
		{
			return;
		}
		
		if (!_actor.isMoving())
		{
			_actor.broadcastPacket(new VehicleStarted(getActor(), 1));
		}
		
		_actor.moveToLocation(x, y, z, 0);
		_actor.broadcastPacket(new VehicleDeparture(getActor()));
	}
	
	@Override
	public void clientStopMoving(Location loc)
	{
		if (_actor.isMoving())
		{
			_actor.stopMove(loc);
			_actor.broadcastPacket(new VehicleStarted(getActor(), 0));
			_actor.broadcastPacket(new VehicleInfo(getActor()));
			return;
		}
		
		if (loc != null)
		{
			_actor.broadcastPacket(new VehicleStarted(getActor(), 0));
			_actor.broadcastPacket(new VehicleInfo(getActor()));
		}
	}
	
	@Override
	public void describeStateToPlayer(Player player)
	{
		if (!_actor.isMoving())
		{
			return;
		}
		
		player.sendPacket(new VehicleDeparture(getActor()));
	}
	
	@Override
	protected void onIntentionAttack(Creature target)
	{
	}
	
	@Override
	protected void onIntentionCast(Skill skill, WorldObject target)
	{
	}
	
	@Override
	protected void onIntentionFollow(Creature target)
	{
	}
	
	@Override
	protected void onIntentionPickUp(WorldObject item)
	{
	}
	
	@Override
	protected void onIntentionInteract(WorldObject object)
	{
	}
	
	@Override
	protected void onActionAttacked(Creature attacker)
	{
	}
	
	@Override
	protected void onActionAggression(Creature target, int aggro)
	{
	}
	
	@Override
	protected void onActionStunned(Creature attacker)
	{
	}
	
	@Override
	protected void onActionSleeping(Creature attacker)
	{
	}
	
	@Override
	protected void onActionRooted(Creature attacker)
	{
	}
	
	@Override
	protected void onActionForgetObject(WorldObject object)
	{
	}
	
	@Override
	protected void onActionCancel()
	{
	}
	
	@Override
	protected void onActionDeath()
	{
	}
	
	@Override
	protected void onActionFakeDeath()
	{
	}
	
	@Override
	protected void onActionFinishCasting()
	{
	}
	
	@Override
	protected void clientActionFailed()
	{
	}
	
	@Override
	public void moveToPawn(WorldObject pawn, int offset)
	{
	}
	
	@Override
	protected void clientStoppedMoving()
	{
	}
	
	@Override
	public Boat getActor()
	{
		return (Boat) _actor;
	}
}
