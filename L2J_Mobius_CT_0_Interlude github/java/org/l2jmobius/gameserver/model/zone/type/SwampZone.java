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
package org.l2jmobius.gameserver.model.zone.type;

import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.network.serverpackets.OnEventTrigger;

/**
 * another type of zone where your speed is changed
 * @author Kerberos, Mobius
 */
public class SwampZone extends ZoneType
{
	private double _move_bonus;
	private int _castleId;
	private Castle _castle;
	private int _eventId;
	
	public SwampZone(int id)
	{
		super(id);
		
		// Setup default speed reduce (in %)
		_move_bonus = 0.5;
		
		// no castle by default
		_castleId = 0;
		_castle = null;
		
		// no event by default
		_eventId = 0;
	}
	
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("move_bonus"))
		{
			_move_bonus = Double.parseDouble(value);
		}
		else if (name.equals("castleId"))
		{
			_castleId = Integer.parseInt(value);
		}
		else if (name.equals("eventId"))
		{
			_eventId = Integer.parseInt(value);
		}
		else
		{
			super.setParameter(name, value);
		}
	}
	
	private Castle getCastle()
	{
		if ((_castleId > 0) && (_castle == null))
		{
			_castle = CastleManager.getInstance().getCastleById(_castleId);
		}
		
		return _castle;
	}
	
	@Override
	protected void onEnter(Creature creature)
	{
		if (getCastle() != null)
		{
			// castle zones active only during siege
			if (!getCastle().getSiege().isInProgress())
			{
				return;
			}
			
			// defenders not affected
			final Player player = creature.asPlayer();
			if ((player != null) && player.isInSiege() && (player.getSiegeState() == 2))
			{
				return;
			}
		}
		
		creature.setInsideZone(ZoneId.SWAMP, true);
		if (creature.isPlayer())
		{
			if (_eventId > 0)
			{
				creature.sendPacket(new OnEventTrigger(_eventId, true));
			}
			
			creature.asPlayer().broadcastUserInfo();
		}
	}
	
	@Override
	protected void onExit(Creature creature)
	{
		// don't broadcast info if not needed
		if (creature.isInsideZone(ZoneId.SWAMP))
		{
			creature.setInsideZone(ZoneId.SWAMP, false);
			if (creature.isPlayer())
			{
				if (_eventId > 0)
				{
					creature.sendPacket(new OnEventTrigger(_eventId, false));
				}
				
				if (!creature.isTeleporting())
				{
					creature.asPlayer().broadcastUserInfo();
				}
			}
		}
	}
	
	public double getMoveBonus()
	{
		return _move_bonus;
	}
}
