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
package org.l2jmobius.gameserver.model.actor.holders.npc;

import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.managers.WalkingManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.npc.OnNpcMoveRouteFinished;

/**
 * Holds info about current walk progress.<br>
 * Maintains the route cursor and runtime state for an NPC walking task.
 * <ul>
 * <li>Supports sequential and random traversal.</li>
 * <li>Dispatches route-finished event on last node reach.</li>
 * <li>Stores a check task reference and debug timestamp.</li>
 * </ul>
 * @author BazookaRpm
 */
public class WalkInfo
{
	// Constants.
	private static final int FLAG_BLOCKED = 0x01;
	private static final int FLAG_SUSPENDED = 0x02;
	private static final int FLAG_STOPPED_BY_ATTACK = 0x04;
	
	private static final int STEP_FORWARD = 1;
	
	private static final int NODE_FIRST = 0;
	private static final int NODE_SECOND = 1;
	
	// Route Data.
	private final String _routeName;
	
	// Task Data.
	private ScheduledFuture<?> _walkCheckTask;
	
	// Runtime State.
	private int _stateFlags;
	private int _cursor;
	private int _step;
	
	// Debug Data.
	private long _lastActionTime;
	
	public WalkInfo(String routeName)
	{
		_routeName = routeName;
		_walkCheckTask = null;
		
		_stateFlags = 0;
		_cursor = NODE_FIRST;
		_step = STEP_FORWARD;
		
		_lastActionTime = 0L;
	}
	
	public boolean isBlocked()
	{
		return (_stateFlags & FLAG_BLOCKED) != 0;
	}
	
	public void setBlocked(boolean value)
	{
		int flags = _stateFlags;
		if (value)
		{
			flags |= FLAG_BLOCKED;
		}
		else
		{
			flags &= ~FLAG_BLOCKED;
		}
		_stateFlags = flags;
	}
	
	public boolean isSuspended()
	{
		return (_stateFlags & FLAG_SUSPENDED) != 0;
	}
	
	public void setSuspended(boolean value)
	{
		int flags = _stateFlags;
		if (value)
		{
			flags |= FLAG_SUSPENDED;
		}
		else
		{
			flags &= ~FLAG_SUSPENDED;
		}
		_stateFlags = flags;
	}
	
	public boolean isStoppedByAttack()
	{
		return (_stateFlags & FLAG_STOPPED_BY_ATTACK) != 0;
	}
	
	public void setStoppedByAttack(boolean value)
	{
		int flags = _stateFlags;
		if (value)
		{
			flags |= FLAG_STOPPED_BY_ATTACK;
		}
		else
		{
			flags &= ~FLAG_STOPPED_BY_ATTACK;
		}
		_stateFlags = flags;
	}
	
	public int getCurrentNodeId()
	{
		return _cursor;
	}
	
	public long getLastAction()
	{
		return _lastActionTime;
	}
	
	public void setLastAction(long value)
	{
		_lastActionTime = value;
	}
	
	public ScheduledFuture<?> getWalkCheckTask()
	{
		return _walkCheckTask;
	}
	
	public void setWalkCheckTask(ScheduledFuture<?> task)
	{
		_walkCheckTask = task;
	}
	
	public WalkRoute getRoute()
	{
		final WalkingManager manager = WalkingManager.getInstance();
		final WalkRoute route = manager.getRoute(_routeName);
		return route;
	}
	
	public WalkNode getCurrentNode()
	{
		final WalkRoute route = getRoute();
		int cursor = _cursor;
		if (cursor < NODE_FIRST)
		{
			cursor = NODE_FIRST;
		}
		
		final int size = route.getNodeList().size();
		final int last = size - 1;
		if (cursor > last)
		{
			cursor = last;
		}
		
		return route.getNodeList().get(cursor);
	}
	
	public synchronized void calculateNextNode(Npc npc)
	{
		final WalkRoute route = getRoute();
		final int nodesCount = route.getNodesCount();
		final byte repeatType = route.getRepeatType();
		if (repeatType == WalkingManager.REPEAT_RANDOM)
		{
			if (nodesCount <= NODE_SECOND)
			{
				_cursor = NODE_FIRST;
				return;
			}
			
			final int base = _cursor;
			int selected = base;
			while (selected == base)
			{
				final int candidate = Rnd.get(nodesCount);
				if (candidate != base)
				{
					selected = candidate;
				}
			}
			
			_cursor = selected;
			return;
		}
		
		int cursor = _cursor;
		cursor += _step;
		_cursor = cursor;
		
		if (_cursor == nodesCount)
		{
			final EventDispatcher dispatcher = EventDispatcher.getInstance();
			if (dispatcher.hasListener(EventType.ON_NPC_MOVE_ROUTE_FINISHED, npc))
			{
				dispatcher.notifyEventAsync(new OnNpcMoveRouteFinished(npc), npc);
			}
			
			if (!route.repeatWalk())
			{
				WalkingManager.getInstance().cancelMoving(npc);
				return;
			}
			
			switch (repeatType)
			{
				case WalkingManager.REPEAT_GO_BACK:
				{
					_step = -STEP_FORWARD;
					_cursor = nodesCount - 2;
					break;
				}
				case WalkingManager.REPEAT_GO_FIRST:
				{
					_step = STEP_FORWARD;
					_cursor = NODE_FIRST;
					break;
				}
				case WalkingManager.REPEAT_TELE_FIRST:
				{
					npc.teleToLocation(npc.getSpawn().getLocation());
					_step = STEP_FORWARD;
					_cursor = NODE_FIRST;
					break;
				}
				default:
				{
					break;
				}
			}
			return;
		}
		
		if (_cursor == WalkingManager.NO_REPEAT)
		{
			_cursor = NODE_SECOND;
			_step = STEP_FORWARD;
		}
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder(220);
		sb.append("WalkInfo [routeName=");
		sb.append(_routeName);
		sb.append(", walkCheckTask=");
		sb.append(_walkCheckTask);
		sb.append(", stateFlags=0x");
		sb.append(Integer.toHexString(_stateFlags));
		sb.append(", cursor=");
		sb.append(_cursor);
		sb.append(", step=");
		sb.append(_step);
		sb.append(", blocked=");
		sb.append(isBlocked());
		sb.append(", suspended=");
		sb.append(isSuspended());
		sb.append(", stoppedByAttack=");
		sb.append(isStoppedByAttack());
		sb.append(", lastActionTime=");
		sb.append(_lastActionTime);
		sb.append(']');
		return sb.toString();
	}
}
