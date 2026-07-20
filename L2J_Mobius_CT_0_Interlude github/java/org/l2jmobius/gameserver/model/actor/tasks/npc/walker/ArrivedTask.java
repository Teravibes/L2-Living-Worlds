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
package org.l2jmobius.gameserver.model.actor.tasks.npc.walker;

import org.l2jmobius.gameserver.managers.WalkingManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.holders.npc.WalkInfo;

/**
 * Walker arrive task.
 * @author GKR
 */
public class ArrivedTask implements Runnable
{
	private final WalkInfo _walk;
	private final Npc _npc;
	
	public ArrivedTask(Npc npc, WalkInfo walk)
	{
		_npc = npc;
		_walk = walk;
	}
	
	@Override
	public void run()
	{
		_walk.setBlocked(false);
		WalkingManager.getInstance().startMoving(_npc, _walk.getRoute().getName());
	}
}
