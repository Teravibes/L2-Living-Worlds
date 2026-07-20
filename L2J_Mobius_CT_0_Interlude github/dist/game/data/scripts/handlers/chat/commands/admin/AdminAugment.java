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
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.options.Augmentation;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * @author Mobius
 */
public class AdminAugment implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_augment"
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		final String[] splitCommand = command.trim().split(" ");
		if (splitCommand.length < 3)
		{
			activeChar.sendSysMessage("Usage: //augment <item id> <option id>");
			return false;
		}
		
		final WorldObject target = activeChar.getTarget() != null ? activeChar.getTarget() : activeChar;
		if (!target.isPlayer())
		{
			activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
			return false;
		}
		
		final int itemId;
		try
		{
			itemId = Integer.parseInt(splitCommand[1]);
		}
		catch (Exception e)
		{
			activeChar.sendSysMessage("No item with id " + splitCommand[1]);
			return false;
		}
		
		final Player player = target.asPlayer();
		final Item item = player.getInventory().getPaperdollItemByItemId(itemId);
		if (item == null)
		{
			activeChar.sendSysMessage("No equipped item with id " + splitCommand[1]);
			return false;
		}
		
		final int optionId;
		try
		{
			optionId = Integer.parseInt(splitCommand[2]);
		}
		catch (Exception e)
		{
			activeChar.sendSysMessage("No option with id " + splitCommand[2]);
			return false;
		}
		
		final Augmentation newAugmentation = new Augmentation(optionId);
		if (item.isAugmented())
		{
			item.removeAugmentation();
		}
		
		item.setAugmentation(newAugmentation);
		player.sendItemList(false);
		return true;
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
