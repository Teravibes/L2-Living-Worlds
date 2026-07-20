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
package org.l2jmobius.gameserver.model.actor.status;

import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.actor.holders.player.Duel;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.util.LocationUtil;

public class SummonStatus extends PlayableStatus
{
	public SummonStatus(Summon activeChar)
	{
		super(activeChar);
	}
	
	@Override
	public void reduceHp(double value, Creature attacker)
	{
		reduceHp(value, attacker, true, false, false);
	}
	
	@Override
	public void reduceHp(double amount, Creature attacker, boolean awake, boolean isDOT, boolean isHPConsumption)
	{
		final Summon summon = getActiveChar();
		if ((attacker == null) || summon.isDead())
		{
			return;
		}
		
		final Player attackerPlayer = attacker.asPlayer();
		if ((attackerPlayer != null) && ((summon.getOwner() == null) || (summon.getOwner().getDuelId() != attackerPlayer.getDuelId())))
		{
			attackerPlayer.setDuelState(Duel.DUELSTATE_INTERRUPTED);
		}
		
		double value = amount;
		final Player caster = summon.getTransferingDamageTo();
		if (summon.getOwner().getParty() != null)
		{
			if ((caster != null) && LocationUtil.checkIfInRange(1000, summon, caster, true) && !caster.isDead() && summon.getParty().getMembers().contains(caster))
			{
				int transferDmg = Math.min((int) caster.getCurrentHp() - 1, ((int) value * (int) summon.getStat().calcStat(Stat.TRANSFER_DAMAGE_TO_PLAYER, 0, null, null)) / 100);
				if (transferDmg > 0)
				{
					int membersInRange = 0;
					for (Player member : caster.getParty().getMembers())
					{
						if (LocationUtil.checkIfInRange(1000, member, caster, false) && (member != caster))
						{
							membersInRange++;
						}
					}
					
					if (attacker.isPlayable() && (caster.getCurrentCp() > 0))
					{
						if (caster.getCurrentCp() > transferDmg)
						{
							caster.getStatus().reduceCp(transferDmg);
						}
						else
						{
							transferDmg = (int) (transferDmg - caster.getCurrentCp());
							caster.getStatus().reduceCp((int) caster.getCurrentCp());
						}
					}
					
					if (membersInRange > 0)
					{
						caster.reduceCurrentHp(transferDmg / membersInRange, attacker, null);
						value -= transferDmg;
					}
				}
			}
		}
		else if ((caster != null) && (caster == summon.getOwner()) && LocationUtil.checkIfInRange(1000, summon, caster, true) && !caster.isDead()) // when no party, transfer only to owner (caster)
		{
			int transferDmg = Math.min((int) caster.getCurrentHp() - 1, ((int) value * (int) summon.getStat().calcStat(Stat.TRANSFER_DAMAGE_TO_PLAYER, 0, null, null)) / 100);
			if (transferDmg > 0)
			{
				if (attacker.isPlayable() && (caster.getCurrentCp() > 0))
				{
					if (caster.getCurrentCp() > transferDmg)
					{
						caster.getStatus().reduceCp(transferDmg);
					}
					else
					{
						transferDmg = (int) (transferDmg - caster.getCurrentCp());
						caster.getStatus().reduceCp((int) caster.getCurrentCp());
					}
				}
				
				caster.reduceCurrentHp(transferDmg, attacker, null);
				value -= transferDmg;
			}
		}
		
		super.reduceHp(value, attacker, awake, isDOT, isHPConsumption);
	}
	
	@Override
	public Summon getActiveChar()
	{
		return super.getActiveChar().asSummon();
	}
}
