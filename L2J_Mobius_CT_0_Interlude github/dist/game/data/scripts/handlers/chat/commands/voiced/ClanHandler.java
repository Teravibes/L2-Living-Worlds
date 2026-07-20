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
package handlers.chat.commands.voiced;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Player;

public class ClanHandler implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"set name",
		"set home",
		"set group"
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar, String params)
	{
		if (!command.equals("set"))
		{
			return false;
		}
		
		final WorldObject target = activeChar.getTarget();
		if ((target == null) || !target.isPlayer())
		{
			return false;
		}
		
		final Player player = target.asPlayer();
		final boolean isSameClan = (activeChar.getClan() != null) && (player.getClan() != null) && (activeChar.getClan().getId() == player.getClan().getId());
		if (!isSameClan)
		{
			return false;
		}
		
		if (params.startsWith("privileges"))
		{
			final String privilegesValue = params.substring(11);
			if (!StringUtil.isNumeric(privilegesValue))
			{
				return false;
			}
			
			final int privilegesMask = Integer.parseInt(privilegesValue);
			final boolean hasEnoughPrivileges = (activeChar.getClanPrivileges().getMask() > privilegesMask) && activeChar.isClanLeader();
			if (!hasEnoughPrivileges)
			{
				return false;
			}
			
			player.getClanPrivileges().setMask(privilegesMask);
			activeChar.sendMessage("Your clan privileges have been set to " + privilegesMask + " by " + activeChar.getName() + ".");
		}
		else if (params.startsWith("title"))
		{
			if (!player.isClanLeader())
			{
				return false;
			}
			
			final String newTitle = params.substring(11);
			player.setTitle(newTitle);
			player.sendMessage("Your title has been set to " + newTitle + " by " + activeChar.getName() + ".");
		}
		
		return true;
	}
	
	@Override
	public String[] getCommandList()
	{
		return VOICED_COMMANDS;
	}
}
