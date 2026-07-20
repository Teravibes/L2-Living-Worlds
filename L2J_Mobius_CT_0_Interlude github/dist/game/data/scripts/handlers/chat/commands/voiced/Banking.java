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
package handlers.chat.commands.voiced;

import org.l2jmobius.gameserver.config.custom.BankingConfig;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;

/**
 * This class trades Gold Bars for Adena and vice versa.
 * @author Ahmed
 */
public class Banking implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"bank",
		"withdraw",
		"deposit"
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar, String params)
	{
		if (command.equals("bank"))
		{
			activeChar.sendMessage(".deposit (" + BankingConfig.BANKING_SYSTEM_ADENA + " Adena = " + BankingConfig.BANKING_SYSTEM_GOLDBARS + " Goldbar) / .withdraw (" + BankingConfig.BANKING_SYSTEM_GOLDBARS + " Goldbar = " + BankingConfig.BANKING_SYSTEM_ADENA + " Adena)");
		}
		else if (command.equals("deposit"))
		{
			if (activeChar.getInventory().getInventoryItemCount(57, 0) >= BankingConfig.BANKING_SYSTEM_ADENA)
			{
				if (!activeChar.reduceAdena(ItemProcessType.BUY, BankingConfig.BANKING_SYSTEM_ADENA, activeChar, false))
				{
					return false;
				}
				
				activeChar.getInventory().addItem(ItemProcessType.COMPENSATE, 3470, BankingConfig.BANKING_SYSTEM_GOLDBARS, activeChar, null);
				activeChar.getInventory().updateDatabase();
				activeChar.sendMessage("Thank you, you now have " + BankingConfig.BANKING_SYSTEM_GOLDBARS + " Goldbar(s), and " + BankingConfig.BANKING_SYSTEM_ADENA + " less adena.");
			}
			else
			{
				activeChar.sendMessage("You do not have enough Adena to convert to Goldbar(s), you need " + BankingConfig.BANKING_SYSTEM_ADENA + " Adena.");
			}
		}
		else if (command.equals("withdraw"))
		{
			if (activeChar.getInventory().getInventoryItemCount(3470, 0) >= BankingConfig.BANKING_SYSTEM_GOLDBARS)
			{
				if (!activeChar.destroyItemByItemId(ItemProcessType.SELL, 3470, BankingConfig.BANKING_SYSTEM_GOLDBARS, activeChar, false))
				{
					return false;
				}
				
				activeChar.getInventory().addAdena(ItemProcessType.COMPENSATE, BankingConfig.BANKING_SYSTEM_ADENA, activeChar, null);
				activeChar.getInventory().updateDatabase();
				activeChar.sendMessage("Thank you, you now have " + BankingConfig.BANKING_SYSTEM_ADENA + " Adena, and " + BankingConfig.BANKING_SYSTEM_GOLDBARS + " less Goldbar(s).");
			}
			else
			{
				activeChar.sendMessage("You do not have any Goldbars to turn into " + BankingConfig.BANKING_SYSTEM_ADENA + " Adena.");
			}
		}
		
		return true;
	}
	
	@Override
	public String[] getCommandList()
	{
		return VOICED_COMMANDS;
	}
}
