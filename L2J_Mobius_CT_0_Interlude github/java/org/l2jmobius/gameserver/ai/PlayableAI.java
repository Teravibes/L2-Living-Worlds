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

import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * This class manages AI of Playable.<br>
 * PlayableAI :
 * <li>SummonAI</li>
 * <li>PlayerAI</li>
 * @author JIV
 */
public abstract class PlayableAI extends CreatureAI
{
	protected PlayableAI(Playable playable)
	{
		super(playable);
	}
	
	@Override
	protected void onIntentionAttack(Creature target)
	{
		if ((target != null) && target.isPlayable())
		{
			final Player player = _actor.asPlayer();
			final Player targetPlayer = target.asPlayer();
			if (targetPlayer.isProtectionBlessingAffected() && ((player.getLevel() - targetPlayer.getLevel()) >= 10) && (player.getKarma() > 0) && !(target.isInsideZone(ZoneId.PVP)))
			{
				// If attacker have karma and have level >= 10 than his target and target have Newbie Protection Buff.
				player.sendPacket(SystemMessageId.THAT_IS_THE_INCORRECT_TARGET);
				clientActionFailed();
				return;
			}
			
			if (player.isProtectionBlessingAffected() && ((targetPlayer.getLevel() - player.getLevel()) >= 10) && (targetPlayer.getKarma() > 0) && !(target.isInsideZone(ZoneId.PVP)))
			{
				// If target have karma and have level >= 10 than his target and actor have Newbie Protection Buff.
				player.sendPacket(SystemMessageId.THAT_IS_THE_INCORRECT_TARGET);
				clientActionFailed();
				return;
			}
			
			if (targetPlayer.isCursedWeaponEquipped() && (player.getLevel() <= 20))
			{
				player.sendPacket(SystemMessageId.THAT_IS_THE_INCORRECT_TARGET);
				clientActionFailed();
				return;
			}
			
			if (player.isCursedWeaponEquipped() && (targetPlayer.getLevel() <= 20))
			{
				player.sendPacket(SystemMessageId.THAT_IS_THE_INCORRECT_TARGET);
				clientActionFailed();
				return;
			}
		}
		
		super.onIntentionAttack(target);
	}
	
	@Override
	protected void onIntentionCast(Skill skill, WorldObject target)
	{
		if ((target != null) && (target.isPlayable()) && skill.hasNegativeEffect())
		{
			final Player player = _actor.asPlayer();
			final Player targetPlayer = target.asPlayer();
			if (targetPlayer.isProtectionBlessingAffected() && ((player.getLevel() - targetPlayer.getLevel()) >= 10) && (player.getKarma() > 0) && !target.isInsideZone(ZoneId.PVP))
			{
				// If attacker have karma and have level >= 10 than his target and target have Newbie Protection Buff.
				player.sendPacket(SystemMessageId.THAT_IS_THE_INCORRECT_TARGET);
				clientActionFailed();
				return;
			}
			
			if (player.isProtectionBlessingAffected() && ((targetPlayer.getLevel() - player.getLevel()) >= 10) && (targetPlayer.getKarma() > 0) && !target.isInsideZone(ZoneId.PVP))
			{
				// If target have karma and have level >= 10 than his target and actor have Newbie Protection Buff.
				player.sendPacket(SystemMessageId.THAT_IS_THE_INCORRECT_TARGET);
				clientActionFailed();
				return;
			}
			
			if (targetPlayer.isCursedWeaponEquipped() && ((player.getLevel() <= 20) || (targetPlayer.getLevel() <= 20)))
			{
				player.sendPacket(SystemMessageId.THAT_IS_THE_INCORRECT_TARGET);
				clientActionFailed();
				return;
			}
		}
		
		super.onIntentionCast(skill, target);
	}
}
