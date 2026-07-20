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
package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.skill.CommonSkill;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.targets.TargetType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;

public class RequestMagicSkillUse extends ClientPacket
{
	private int _magicId;
	private boolean _ctrlPressed;
	private boolean _shiftPressed;
	
	@Override
	protected void readImpl()
	{
		_magicId = readInt(); // Identifier of the used skill
		_ctrlPressed = readInt() != 0; // True if it's a ForceAttack : Ctrl pressed
		_shiftPressed = readByte() != 0; // True if Shift pressed
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (player.isDead())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.isFakeDeath())
		{
			if (_magicId != CommonSkill.FAKE_DEATH.getId())
			{
				player.sendPacket(SystemMessageId.YOU_CANNOT_MOVE_WHILE_SITTING);
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			else if (player.isSkillDisabled(CommonSkill.FAKE_DEATH.getSkill()))
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}
		
		// Get the level of the used skill.
		Skill skill = player.getKnownSkill(_magicId);
		if (skill == null)
		{
			// Player doesn't know this skill, maybe it's the display Id.
			skill = player.getCustomSkill(_magicId);
			if (skill == null)
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}
		
		// If alternate rule Karma punishment is set to true, forbid teleport to player with Karma.
		if (!PlayerConfig.ALT_GAME_KARMA_PLAYER_CAN_TELEPORT && (player.getKarma() > 0) && skill.hasEffectType(EffectType.TELEPORT))
		{
			return;
		}
		
		// Players mounted on pets cannot use any toggle skills.
		if (skill.isToggle() && player.isMounted())
		{
			return;
		}
		
		player.onActionRequest();
		
		// Stop if use self-buff (except if on Boat).
		if ((skill.isContinuous() && !skill.isDebuff() && (skill.getTargetType() == TargetType.SELF)) && !player.isInBoat())
		{
			player.getAI().setIntention(Intention.MOVE_TO, player.getLocation());
		}
		
		player.useMagic(skill, _ctrlPressed, _shiftPressed);
	}
}
