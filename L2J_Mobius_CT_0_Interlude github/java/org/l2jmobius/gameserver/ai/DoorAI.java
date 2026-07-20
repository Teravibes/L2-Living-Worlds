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
package org.l2jmobius.gameserver.ai;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.instance.Defender;
import org.l2jmobius.gameserver.model.actor.instance.Door;
import org.l2jmobius.gameserver.model.interfaces.ILocational;
import org.l2jmobius.gameserver.model.skill.Skill;

/**
 * @author mkizub
 */
public class DoorAI extends CreatureAI
{
	public DoorAI(Door door)
	{
		super(door);
	}
	
	@Override
	protected void onIntentionIdle()
	{
	}
	
	@Override
	protected void onIntentionActive()
	{
	}
	
	@Override
	protected void onIntentionRest()
	{
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
	protected void onIntentionMoveTo(ILocational destination)
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
	public void onActionThink()
	{
	}
	
	@Override
	protected void onActionAttacked(Creature attacker)
	{
		ThreadPool.execute(new onEventAttackedDoorTask(_actor.asDoor(), attacker));
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
	protected void onActionReadyToAct()
	{
	}
	
	@Override
	protected void onActionUserCmd(Object arg0, Object arg1)
	{
	}
	
	@Override
	protected void onActionArrived()
	{
	}
	
	@Override
	protected void onActionArrivedRevalidate()
	{
	}
	
	@Override
	protected void onActionArrivedBlocked(Location blockedAtLoc)
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
	
	private class onEventAttackedDoorTask implements Runnable
	{
		private final Door _door;
		private final Creature _attacker;
		
		public onEventAttackedDoorTask(Door door, Creature attacker)
		{
			_door = door;
			_attacker = attacker;
		}
		
		@Override
		public void run()
		{
			World.getInstance().forEachVisibleObject(_door, Defender.class, guard ->
			{
				if (_actor.isInsideRadius3D(guard, guard.getTemplate().getClanHelpRange()))
				{
					guard.getAI().notifyAction(Action.AGGRESSION, _attacker, 15);
				}
			});
		}
	}
}
