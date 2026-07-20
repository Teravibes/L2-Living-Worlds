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

import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.l2jmobius.commons.time.TimeUtil;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.data.xml.AdminData;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.olympiad.Hero;
import org.l2jmobius.gameserver.model.olympiad.Olympiad;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * This class handles following admin commands: - admin|admin1/admin2/admin3/admin4/admin5 = slots for the 5 starting admin menus - gmliston/gmlistoff = includes/excludes active character from /gmlist results - silence = toggles private messages acceptance mode - diet = toggles weight penalty mode -
 * tradeoff = toggles trade acceptance mode - reload = reloads specified component from multisell|skill|npc|htm|item - set/set_menu/set_mod = alters specified server setting - saveolymp = saves olympiad state manually - manualhero = cycles olympiad and calculate new heroes.
 * @version $Revision: 1.3.2.1.2.4 $ $Date: 2007/07/28 10:06:06 $
 */
public class AdminAdmin implements IAdminCommandHandler
{
	private static final Logger LOGGER = Logger.getLogger(AdminAdmin.class.getName());
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_admin",
		"admin_admin1",
		"admin_admin2",
		"admin_admin3",
		"admin_admin4",
		"admin_admin5",
		"admin_admin6",
		"admin_admin7",
		"admin_gmliston",
		"admin_gmlistoff",
		"admin_silence",
		"admin_diet",
		"admin_tradeoff",
		"admin_set",
		"admin_set_mod",
		"admin_checkolympiad",
		"admin_saveolymp",
		"admin_sethero",
		"admin_givehero",
		"admin_endolympiad",
		"admin_setconfig",
		"admin_config_server",
		"admin_gmon"
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if (command.startsWith("admin_admin"))
		{
			showMainPage(activeChar, command);
		}
		else if (command.equals("admin_config_server"))
		{
			showConfigPage(activeChar);
		}
		else if (command.startsWith("admin_gmliston"))
		{
			AdminData.getInstance().addGm(activeChar, false);
			activeChar.sendSysMessage("Registered into GM list.");
			AdminHtml.showAdminHtml(activeChar, "gm_menu.htm");
		}
		else if (command.startsWith("admin_gmlistoff"))
		{
			AdminData.getInstance().addGm(activeChar, true);
			activeChar.sendSysMessage("Removed from GM list.");
			AdminHtml.showAdminHtml(activeChar, "gm_menu.htm");
		}
		else if (command.startsWith("admin_silence"))
		{
			if (activeChar.isSilenceMode()) // already in message refusal mode
			{
				activeChar.setSilenceMode(false);
				activeChar.sendPacket(SystemMessageId.MESSAGE_ACCEPTANCE_MODE);
			}
			else
			{
				activeChar.setSilenceMode(true);
				activeChar.sendPacket(SystemMessageId.MESSAGE_REFUSAL_MODE);
			}
			
			AdminHtml.showAdminHtml(activeChar, "gm_menu.htm");
		}
		else if (command.startsWith("admin_checkolympiad"))
		{
			final long currentTimeMillis = System.currentTimeMillis();
			final long olympiadEndMillis = Olympiad.getInstance().getMillisToOlympiadEnd();
			final long weeklyChangeMillis = Olympiad.getInstance().getMillisToWeekChange();
			
			final String currentServerTime = TimeUtil.getDateTimeString(currentTimeMillis);
			final String olympiadEndTime = TimeUtil.getDateTimeString(currentTimeMillis + olympiadEndMillis);
			final String weeklyChangeTime = TimeUtil.getDateTimeString(currentTimeMillis + weeklyChangeMillis);
			
			final String olympiadTimeLeft = TimeUtil.formatDuration(olympiadEndMillis).replaceAll(",? \\d+ seconds?", "").replaceAll(",? \\d+ milliseconds?", "");
			final String weeklyChangeTimeLeft = TimeUtil.formatDuration(weeklyChangeMillis).replaceAll(",? \\d+ seconds?", "").replaceAll(",? \\d+ milliseconds?", "");
			
			final NpcHtmlMessage html = new NpcHtmlMessage();
			final StringBuilder sb = new StringBuilder();
			
			sb.append("<html><title>Olympiad Period</title><body>");
			sb.append("<center><table width=280>");
			sb.append("<tr><td width=60><button value=\"Main\" action=\"bypass admin_admin\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
			sb.append("<td width=150><center>Olympiad Info</center></td>");
			sb.append("<td width=60><button value=\"Back\" action=\"bypass admin_admin2\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
			sb.append("</table></center><br>");
			
			// Current Server Time.
			sb.append("<center><table width=280 bgcolor=\"000000\">");
			sb.append("<tr><td width=120>Current Time:</td><td width=140><font color=\"LEVEL\">");
			sb.append(currentServerTime);
			sb.append("</font></td></tr></table></center><br><br>");
			
			// Olympiad End Time.
			sb.append("<center><table width=280 bgcolor=\"000000\">");
			sb.append("<tr><td width=120>Olympiad Period Ends:</td><td width=120><font color=\"LEVEL\">");
			sb.append(olympiadEndTime);
			sb.append("</font></td></tr>");
			sb.append("<tr><td width=120>Time Left:</td><td width=120><font color=\"LEVEL\">");
			sb.append(olympiadTimeLeft);
			sb.append("</font></td></tr></table></center><br>");
			
			// Weekly Change.
			sb.append("<center><table width=280 bgcolor=\"000000\">");
			sb.append("<tr><td width=120>Next Weekly Change:</td><td width=120><font color=\"LEVEL\">");
			sb.append(weeklyChangeTime);
			sb.append("</font></td></tr>");
			sb.append("<tr><td width=120>Time Left:</td><td width=120><font color=\"LEVEL\">");
			sb.append(weeklyChangeTimeLeft);
			sb.append("</font></td></tr></table></center><br>");
			
			sb.append("</body></html>");
			
			html.setHtml(sb.toString());
			activeChar.sendPacket(html);
		}
		else if (command.startsWith("admin_saveolymp"))
		{
			Olympiad.getInstance().saveOlympiadStatus();
			activeChar.sendSysMessage("olympiad system saved.");
		}
		else if (command.startsWith("admin_endolympiad"))
		{
			try
			{
				Olympiad.getInstance().manualSelectHeroes();
			}
			catch (Exception e)
			{
				LOGGER.warning("An error occured while ending olympiad: " + e);
			}
			
			activeChar.sendSysMessage("Heroes formed.");
		}
		else if (command.startsWith("admin_sethero"))
		{
			if (activeChar.getTarget() == null)
			{
				activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
				return false;
			}
			
			final Player target = activeChar.getTarget().isPlayer() ? activeChar.getTarget().asPlayer() : activeChar;
			target.setHero(!target.isHero());
			target.broadcastUserInfo();
		}
		else if (command.startsWith("admin_givehero"))
		{
			if (activeChar.getTarget() == null)
			{
				activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
				return false;
			}
			
			final Player target = activeChar.getTarget().isPlayer() ? activeChar.getTarget().asPlayer() : activeChar;
			if (Hero.getInstance().isHero(target.getObjectId()))
			{
				activeChar.sendSysMessage("This player has already claimed the hero status.");
				return false;
			}
			
			if (!Hero.getInstance().isUnclaimedHero(target.getObjectId()))
			{
				activeChar.sendSysMessage("This player cannot claim the hero status.");
				return false;
			}
			
			Hero.getInstance().claimHero(target);
		}
		else if (command.startsWith("admin_diet"))
		{
			try
			{
				final StringTokenizer st = new StringTokenizer(command);
				st.nextToken();
				if (st.nextToken().equalsIgnoreCase("on"))
				{
					activeChar.setDietMode(true);
					activeChar.sendSysMessage("Diet mode on");
				}
				else if (st.nextToken().equalsIgnoreCase("off"))
				{
					activeChar.setDietMode(false);
					activeChar.sendSysMessage("Diet mode off");
				}
			}
			catch (Exception ex)
			{
				if (activeChar.getDietMode())
				{
					activeChar.setDietMode(false);
					activeChar.sendSysMessage("Diet mode off");
				}
				else
				{
					activeChar.setDietMode(true);
					activeChar.sendSysMessage("Diet mode on");
				}
			}
			finally
			{
				activeChar.refreshOverloaded();
			}
			
			AdminHtml.showAdminHtml(activeChar, "gm_menu.htm");
		}
		else if (command.startsWith("admin_tradeoff"))
		{
			try
			{
				final String mode = command.substring(15);
				if (mode.equalsIgnoreCase("on"))
				{
					activeChar.setTradeRefusal(true);
					activeChar.sendSysMessage("Trade refusal enabled");
				}
				else if (mode.equalsIgnoreCase("off"))
				{
					activeChar.setTradeRefusal(false);
					activeChar.sendSysMessage("Trade refusal disabled");
				}
			}
			catch (Exception ex)
			{
				if (activeChar.getTradeRefusal())
				{
					activeChar.setTradeRefusal(false);
					activeChar.sendSysMessage("Trade refusal disabled");
				}
				else
				{
					activeChar.setTradeRefusal(true);
					activeChar.sendSysMessage("Trade refusal enabled");
				}
			}
			
			AdminHtml.showAdminHtml(activeChar, "gm_menu.htm");
		}
		else if (command.startsWith("admin_setconfig"))
		{
			final StringTokenizer st = new StringTokenizer(command);
			st.nextToken();
			try
			{
				final String pName = st.nextToken();
				final String pValue = st.nextToken();
				if (Float.valueOf(pValue) == null)
				{
					activeChar.sendSysMessage("Invalid parameter!");
					return false;
				}
				
				switch (pName)
				{
					case "RateXp":
					{
						RatesConfig.RATE_XP = Float.parseFloat(pValue);
						break;
					}
					case "RateSp":
					{
						RatesConfig.RATE_SP = Float.parseFloat(pValue);
						break;
					}
					case "RateDropSpoil":
					{
						RatesConfig.RATE_SPOIL_DROP_CHANCE_MULTIPLIER = Float.parseFloat(pValue);
						break;
					}
				}
				
				activeChar.sendSysMessage("Config parameter " + pName + " set to " + pValue);
			}
			catch (Exception e)
			{
				activeChar.sendSysMessage("Usage: //setconfig <parameter> <value>");
			}
			finally
			{
				showConfigPage(activeChar);
			}
		}
		else if (command.startsWith("admin_set"))
		{
			// nothing
		}
		else if (command.startsWith("admin_gmon"))
		{
			// nothing
		}
		
		return true;
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private void showMainPage(Player activeChar, String command)
	{
		int mode = 0;
		String filename = null;
		try
		{
			mode = Integer.parseInt(command.substring(11));
		}
		catch (Exception e)
		{
			// Not important.
		}
		
		switch (mode)
		{
			case 1:
			{
				filename = "main";
				break;
			}
			case 2:
			{
				filename = "game";
				break;
			}
			case 3:
			{
				filename = "effects";
				break;
			}
			case 4:
			{
				filename = "server";
				break;
			}
			case 5:
			{
				filename = "mods";
				break;
			}
			case 6:
			{
				filename = "char";
				break;
			}
			case 7:
			{
				filename = "gm";
				break;
			}
			default:
			{
				filename = "main";
				break;
			}
		}
		
		AdminHtml.showAdminHtml(activeChar, filename + "_menu.htm");
	}
	
	private void showConfigPage(Player activeChar)
	{
		final NpcHtmlMessage adminReply = new NpcHtmlMessage();
		final StringBuilder replyMSG = new StringBuilder("<html><title>L2J :: Config</title><body>");
		replyMSG.append("<center><table width=270><tr><td width=60><button value=\"Main\" action=\"bypass admin_admin\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td width=150><center>Config Server Panel</center></td><td width=60><button value=\"Back\" action=\"bypass admin_admin4\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr></table></center><br>");
		replyMSG.append("<center><table width=260><tr><td width=140></td><td width=40></td><td width=40></td></tr>");
		replyMSG.append("<tr><td><font color=\"00AA00\">Drop:</font></td><td></td><td></td></tr>");
		replyMSG.append("<tr><td><font color=\"LEVEL\">Rate EXP</font> = " + RatesConfig.RATE_XP + "</td><td><edit var=\"param1\" width=40 height=12></td><td><button value=\"Set\" action=\"bypass -h admin_setconfig RateXp $param1\" width=30 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
		replyMSG.append("<tr><td><font color=\"LEVEL\">Rate SP</font> = " + RatesConfig.RATE_SP + "</td><td><edit var=\"param2\" width=40 height=12></td><td><button value=\"Set\" action=\"bypass -h admin_setconfig RateSp $param2\" width=30 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
		replyMSG.append("<tr><td><font color=\"LEVEL\">Rate Drop Spoil</font> = " + RatesConfig.RATE_SPOIL_DROP_CHANCE_MULTIPLIER + "</td><td><edit var=\"param4\" width=40 height=12></td><td><button value=\"Set\" action=\"bypass -h admin_setconfig RateDropSpoil $param4\" width=30 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
		replyMSG.append("</table></body></html>");
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}
}
