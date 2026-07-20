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

import java.text.SimpleDateFormat;

import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.config.custom.PremiumSystemConfig;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.managers.PremiumManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class Premium implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"premium"
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar, String target)
	{
		if (command.startsWith("premium") && PremiumSystemConfig.PREMIUM_SYSTEM_ENABLED)
		{
			final SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm");
			final long endDate = PremiumManager.getInstance().getPremiumExpiration(activeChar.getAccountName());
			final NpcHtmlMessage msg = new NpcHtmlMessage(5);
			final StringBuilder html = new StringBuilder();
			if (endDate == 0)
			{
				html.append("<html><body><title>Account Details</title><center>");
				html.append("<table>");
				html.append("<tr><td><center>Account Status: <font color=\"LEVEL\">Normal<br></font></td></tr>");
				html.append("<tr><td>Rate XP: <font color=\"LEVEL\"> x" + RatesConfig.RATE_XP + "<br1></font></td></tr>");
				html.append("<tr><td>Rate SP: <font color=\"LEVEL\"> x" + RatesConfig.RATE_SP + "<br1></font></td></tr>");
				html.append("<tr><td>Drop Chance: <font color=\"LEVEL\"> x" + RatesConfig.RATE_DEATH_DROP_CHANCE_MULTIPLIER + "<br1></font></td></tr><br>");
				html.append("<tr><td>Drop Amount: <font color=\"LEVEL\"> x" + RatesConfig.RATE_DEATH_DROP_AMOUNT_MULTIPLIER + "<br1></font></td></tr><br>");
				html.append("<tr><td>Spoil Chance: <font color=\"LEVEL\"> x" + RatesConfig.RATE_SPOIL_DROP_CHANCE_MULTIPLIER + "<br1></font></td></tr><br>");
				html.append("<tr><td>Spoil Amount: <font color=\"LEVEL\"> x" + RatesConfig.RATE_SPOIL_DROP_AMOUNT_MULTIPLIER + "<br><br></font></td></tr><br>");
				html.append("<tr><td><center>Premium Info & Rules<br></td></tr>");
				html.append("<tr><td>Rate XP: <font color=\"LEVEL\"> x" + (RatesConfig.RATE_XP * PremiumSystemConfig.PREMIUM_RATE_XP) + "<br1></font></td></tr>");
				html.append("<tr><td>Rate SP: <font color=\"LEVEL\"> x" + (RatesConfig.RATE_SP * PremiumSystemConfig.PREMIUM_RATE_SP) + "<br1></font></td></tr>");
				html.append("<tr><td>Drop Chance: <font color=\"LEVEL\"> x" + (RatesConfig.RATE_DEATH_DROP_CHANCE_MULTIPLIER * PremiumSystemConfig.PREMIUM_RATE_DROP_CHANCE) + "<br1></font></td></tr>");
				html.append("<tr><td>Drop Amount: <font color=\"LEVEL\"> x" + (RatesConfig.RATE_DEATH_DROP_AMOUNT_MULTIPLIER * PremiumSystemConfig.PREMIUM_RATE_DROP_AMOUNT) + "<br1></font></td></tr>");
				html.append("<tr><td>Spoil Chance: <font color=\"LEVEL\"> x" + (RatesConfig.RATE_SPOIL_DROP_CHANCE_MULTIPLIER * PremiumSystemConfig.PREMIUM_RATE_SPOIL_CHANCE) + "<br1></font></td></tr>");
				html.append("<tr><td>Spoil Amount: <font color=\"LEVEL\"> x" + (RatesConfig.RATE_SPOIL_DROP_AMOUNT_MULTIPLIER * PremiumSystemConfig.PREMIUM_RATE_SPOIL_AMOUNT) + "<br1></font></td></tr>");
				html.append("<tr><td> <font color=\"70FFCA\">1. Premium benefits CAN NOT BE TRANSFERED.<br1></font></td></tr>");
				html.append("<tr><td> <font color=\"70FFCA\">2. Premium does not effect party members.<br1></font></td></tr>");
				html.append("<tr><td> <font color=\"70FFCA\">3. Premium benefits effect ALL characters in same account.</font></td></tr>");
			}
			else
			{
				html.append("<html><body><title>Premium Account Details</title><center>");
				html.append("<table>");
				html.append("<tr><td><center>Account Status: <font color=\"LEVEL\">Premium<br></font></td></tr>");
				html.append("<tr><td>Rate XP: <font color=\"LEVEL\">x" + (RatesConfig.RATE_XP * PremiumSystemConfig.PREMIUM_RATE_XP) + " <br1></font></td></tr>");
				html.append("<tr><td>Rate SP: <font color=\"LEVEL\">x" + (RatesConfig.RATE_SP * PremiumSystemConfig.PREMIUM_RATE_SP) + "  <br1></font></td></tr>");
				html.append("<tr><td>Drop Chance: <font color=\"LEVEL\">x" + (RatesConfig.RATE_DEATH_DROP_CHANCE_MULTIPLIER * PremiumSystemConfig.PREMIUM_RATE_DROP_CHANCE) + " <br1></font></td></tr>");
				html.append("<tr><td>Drop Amount: <font color=\"LEVEL\">x" + (RatesConfig.RATE_DEATH_DROP_AMOUNT_MULTIPLIER * PremiumSystemConfig.PREMIUM_RATE_DROP_AMOUNT) + " <br1></font></td></tr>");
				html.append("<tr><td>Spoil Chance: <font color=\"LEVEL\">x" + (RatesConfig.RATE_SPOIL_DROP_CHANCE_MULTIPLIER * PremiumSystemConfig.PREMIUM_RATE_SPOIL_CHANCE) + " <br1></font></td></tr>");
				html.append("<tr><td>Spoil Amount: <font color=\"LEVEL\">x" + (RatesConfig.RATE_SPOIL_DROP_AMOUNT_MULTIPLIER * PremiumSystemConfig.PREMIUM_RATE_SPOIL_AMOUNT) + " <br1></font></td></tr>");
				html.append("<tr><td>Expires: <font color=\"00A5FF\">" + format.format(endDate) + "</font></td></tr>");
				html.append("<tr><td>Current Date: <font color=\"70FFCA\">" + format.format(System.currentTimeMillis()) + "<br><br></font></td></tr>");
				html.append("<tr><td><center>Premium Info & Rules<br></center></td></tr>");
				html.append("<tr><td><font color=\"70FFCA\">1. Premium accounts CAN NOT BE TRANSFERED.<br1></font></td></tr>");
				html.append("<tr><td><font color=\"70FFCA\">2. Premium does not effect party members.<br1></font></td></tr>");
				html.append("<tr><td><font color=\"70FFCA\">3. Premium account effects ALL characters in same account.<br><br><br></font></td></tr>");
				html.append("<tr><td><center>Thank you for supporting our server.</td></tr>");
			}
			html.append("</table>");
			html.append("</center></body></html>");
			msg.setHtml(html.toString());
			activeChar.sendPacket(msg);
		}
		else
		{
			return false;
		}
		
		return true;
	}
	
	@Override
	public String[] getCommandList()
	{
		return VOICED_COMMANDS;
	}
}
