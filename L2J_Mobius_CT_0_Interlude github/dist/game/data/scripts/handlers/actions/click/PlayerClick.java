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
package handlers.actions.click;

import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.handler.IActionClickHandler;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;

public class PlayerClick implements IActionClickHandler
{
	private static final int CURSED_WEAPON_VICTIM_MIN_LEVEL = 21;
	
	/**
	 * Manage actions when a player click on this Player.<br>
	 * <br>
	 * <b><u>Actions on first click on the Player (Select it)</u>:</b><br>
	 * <li>Set the target of the player</li>
	 * <li>Send a Server->Client packet MyTargetSelected to the player (display the select window)</li><br>
	 * <br>
	 * <b><u>Actions on second click on the Player (Follow it/Attack it/Intercat with it)</u>:</b><br>
	 * <li>Send a Server->Client packet MyTargetSelected to the player (display the select window)</li>
	 * <li>If target Player has a Private Store, notify the player AI with INTERACT</li>
	 * <li>If target Player is autoAttackable, notify the player AI with ATTACK</li>
	 * <li>If target Player is NOT autoAttackable, notify the player AI with FOLLOW</li><br>
	 * <br>
	 * <b><u>Example of use</u>:</b><br>
	 * <li>Client packet : Action, AttackRequest</li><br>
	 * @param player The player that start an action on target Player
	 */
	@Override
	public boolean onAction(Player player, WorldObject target, boolean interact)
	{
		// Check if the Player is confused
		if (player.isOutOfControl())
		{
			return false;
		}
		
		// Aggression target lock effect
		if (player.isLockedTarget() && (player.getLockedTarget() != target))
		{
			player.sendPacket(SystemMessageId.FAILED_TO_CHANGE_ATTACK_TARGET);
			return false;
		}
		
		// Check if the player already target this Player
		if (player.getTarget() != target)
		{
			// Set the target of the player
			player.setTarget(target);
		}
		else if (interact)
		{
			// Check if this Player has a Private Store
			final Player targetPlayer = target.asPlayer();
			if (targetPlayer.isInStoreMode())
			{
				player.getAI().setIntention(Intention.INTERACT, target);
			}
			else
			{
				// Check if this Player is autoAttackable
				if (target.isAutoAttackable(player))
				{
					// Player with level < 21 can't attack a cursed weapon holder
					// And a cursed weapon holder can't attack players with level < 21
					if ((targetPlayer.isCursedWeaponEquipped() && (player.getLevel() < CURSED_WEAPON_VICTIM_MIN_LEVEL)) //
						|| (player.isCursedWeaponEquipped() && (targetPlayer.getLevel() < CURSED_WEAPON_VICTIM_MIN_LEVEL)))
					{
						player.sendPacket(ActionFailed.STATIC_PACKET);
					}
					else
					{
						player.getAI().setIntention(Intention.ATTACK, target);
						player.onActionRequest();
					}
				}
				else
				{
					// This Action Failed packet avoids player getting stuck when clicking three or more times
					player.sendPacket(ActionFailed.STATIC_PACKET);
					
					// if (GeoEngine.getInstance().canMoveToTarget(player, target))
					// {
					player.getAI().setIntention(Intention.FOLLOW, target);
					// }
				}
			}
		}
		
		return true;
	}
	
	@Override
	public InstanceType getInstanceType()
	{
		return InstanceType.Player;
	}
}
