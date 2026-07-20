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
package handlers.chat.commands.admin;

import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.taskmanagers.DecayTaskManager;

/**
 * This class handles following admin commands: - res = resurrects target Creature
 * @version $Revision: 1.2.4.5 $ $Date: 2005/04/11 10:06:06 $
 */
public class AdminRes implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_res",
		"admin_res_monster"
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if (command.startsWith("admin_res "))
		{
			handleRes(activeChar, command.split(" ")[1]);
		}
		else if (command.equals("admin_res"))
		{
			handleRes(activeChar);
		}
		else if (command.startsWith("admin_res_monster "))
		{
			handleNonPlayerRes(activeChar, command.split(" ")[1]);
		}
		else if (command.equals("admin_res_monster"))
		{
			handleNonPlayerRes(activeChar);
		}
		
		return true;
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private void handleRes(Player activeChar)
	{
		handleRes(activeChar, null);
	}
	
	private void handleRes(Player activeChar, String resParam)
	{
		WorldObject obj = activeChar.getTarget();
		if (resParam != null)
		{
			// Check if a player name was specified as a param.
			final Player plyr = World.getInstance().getPlayer(resParam);
			if (plyr != null)
			{
				obj = plyr;
			}
			else
			{
				// Otherwise, check if the param was a radius.
				try
				{
					final int radius = Integer.parseInt(resParam);
					World.getInstance().forEachVisibleObjectInRange(activeChar, Player.class, radius, this::doResurrect);
					activeChar.sendSysMessage("Resurrected all players within a " + radius + " unit radius.");
					return;
				}
				catch (NumberFormatException e)
				{
					activeChar.sendSysMessage("Enter a valid player name or radius.");
					return;
				}
			}
		}
		
		if (obj == null)
		{
			obj = activeChar;
		}
		
		doResurrect(obj.asCreature());
	}
	
	private void handleNonPlayerRes(Player activeChar)
	{
		handleNonPlayerRes(activeChar, "");
	}
	
	private void handleNonPlayerRes(Player activeChar, String radiusStr)
	{
		final WorldObject obj = activeChar.getTarget();
		
		try
		{
			int radius = 0;
			if (!radiusStr.isEmpty())
			{
				radius = Integer.parseInt(radiusStr);
				World.getInstance().forEachVisibleObjectInRange(activeChar, Creature.class, radius, knownChar ->
				{
					if (!knownChar.isPlayer())
					{
						doResurrect(knownChar);
					}
				});
				
				activeChar.sendSysMessage("Resurrected all non-players within a " + radius + " unit radius.");
			}
		}
		catch (NumberFormatException e)
		{
			activeChar.sendSysMessage("Enter a valid radius.");
			return;
		}
		
		if ((obj == null) || obj.isPlayer())
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_ITEM);
			return;
		}
		
		doResurrect(obj.asCreature());
	}
	
	private void doResurrect(Creature targetChar)
	{
		if (!targetChar.isDead())
		{
			return;
		}
		
		// If the target is a player, then restore the XP lost on death.
		if (targetChar.isPlayer())
		{
			targetChar.asPlayer().restoreExp(100.0);
		}
		else
		{
			DecayTaskManager.getInstance().cancel(targetChar);
		}
		
		targetChar.doRevive();
	}
}
