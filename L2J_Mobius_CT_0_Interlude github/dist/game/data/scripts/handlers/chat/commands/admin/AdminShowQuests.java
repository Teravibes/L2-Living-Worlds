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
package handlers.chat.commands.admin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowQuestMark;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.QuestList;

/**
 * TODO: Rework and cleanup.
 * @author Korvin, Zoey76
 */
public class AdminShowQuests implements IAdminCommandHandler
{
	private static final Logger LOGGER = Logger.getLogger(AdminShowQuests.class.getName());
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_charquestmenu",
		"admin_setcharquest"
	};
	
	private static final String[] _states =
	{
		"CREATED",
		"STARTED",
		"COMPLETED"
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		final String[] cmdParams = command.split(" ");
		Player target = null;
		WorldObject targetObject = null;
		final String[] val = new String[4];
		val[0] = null;
		if (cmdParams.length > 1)
		{
			target = World.getInstance().getPlayer(cmdParams[1]);
			if (cmdParams.length > 2)
			{
				if (cmdParams[2].equals("0"))
				{
					val[0] = "var";
					val[1] = "Start";
				}
				
				if (cmdParams[2].equals("1"))
				{
					val[0] = "var";
					val[1] = "Started";
				}
				
				if (cmdParams[2].equals("2"))
				{
					val[0] = "var";
					val[1] = "Completed";
				}
				
				if (cmdParams[2].equals("3"))
				{
					val[0] = "full";
				}
				
				if (cmdParams[2].contains("_"))
				{
					val[0] = "name";
					val[1] = cmdParams[2];
				}
				
				if ((cmdParams.length > 3) && cmdParams[3].equals("custom"))
				{
					val[0] = "custom";
					val[1] = cmdParams[2];
				}
			}
		}
		else
		{
			targetObject = activeChar.getTarget();
			if ((targetObject != null) && targetObject.isPlayer())
			{
				target = targetObject.asPlayer();
			}
		}
		
		if (target == null)
		{
			activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
			return false;
		}
		
		if (command.startsWith("admin_charquestmenu"))
		{
			if (val[0] != null)
			{
				showQuestMenu(target, activeChar, val);
			}
			else
			{
				showFirstQuestMenu(target, activeChar);
			}
		}
		else if (command.startsWith("admin_setcharquest"))
		{
			if (cmdParams.length >= 5)
			{
				val[0] = cmdParams[2];
				val[1] = cmdParams[3];
				val[2] = cmdParams[4];
				if (cmdParams.length == 6)
				{
					val[3] = cmdParams[5];
				}
				
				setQuestVar(target, activeChar, val);
			}
			else
			{
				return false;
			}
		}
		
		return true;
	}
	
	private void showFirstQuestMenu(Player target, Player actor)
	{
		final StringBuilder replyMSG = new StringBuilder("<html><body><table width=270><tr><td width=45><button value=\"Main\" action=\"bypass admin_admin\" width=45 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td width=180><center>Player: " + target.getName() + "</center></td><td width=45><button value=\"Back\" action=\"bypass admin_admin6\" width=45 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr></table>");
		final NpcHtmlMessage adminReply = new NpcHtmlMessage();
		final int ID = target.getObjectId();
		replyMSG.append("Quest Menu for <font color=\"LEVEL\">" + target.getName() + "</font> (ID:" + ID + ")<br><center>");
		replyMSG.append("<table width=250><tr><td><button value=\"CREATED\" action=\"bypass -h admin_charquestmenu " + target.getName() + " 0\" width=95 height=21 back=\"bigbutton_over\" fore=\"bigbutton\"></td></tr>");
		replyMSG.append("<tr><td><button value=\"STARTED\" action=\"bypass -h admin_charquestmenu " + target.getName() + " 1\" width=95 height=21 back=\"bigbutton_over\" fore=\"bigbutton\"></td></tr>");
		replyMSG.append("<tr><td><button value=\"COMPLETED\" action=\"bypass -h admin_charquestmenu " + target.getName() + " 2\" width=95 height=21 back=\"bigbutton_over\" fore=\"bigbutton\"></td></tr>");
		replyMSG.append("<tr><td><br><button value=\"All\" action=\"bypass -h admin_charquestmenu " + target.getName() + " 3\" width=95 height=21 back=\"bigbutton_over\" fore=\"bigbutton\"></td></tr>");
		replyMSG.append("<tr><td><br><br>Manual Edit by Quest number:<br></td></tr>");
		replyMSG.append("<tr><td><edit var=\"qn\" width=65 height=12><br><button value=\"Edit\" action=\"bypass -h admin_charquestmenu " + target.getName() + " $qn custom\" width=65 height=21 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\"></td></tr>");
		replyMSG.append("</table></center></body></html>");
		adminReply.setHtml(replyMSG.toString());
		actor.sendPacket(adminReply);
	}
	
	private void showQuestMenu(Player target, Player actor, String[] val)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			ResultSet rs;
			PreparedStatement req;
			final int ID = target.getObjectId();
			final StringBuilder replyMSG = new StringBuilder("<html><body>");
			final NpcHtmlMessage adminReply = new NpcHtmlMessage();
			
			switch (val[0])
			{
				case "full":
				{
					replyMSG.append("<table width=250><tr><td>Full Quest List for <font color=\"LEVEL\">" + target.getName() + "</font> (ID:" + ID + ")</td></tr>");
					req = con.prepareStatement("SELECT DISTINCT name FROM character_quests WHERE charId='" + ID + "' AND var='<state>' ORDER by name");
					req.execute();
					rs = req.getResultSet();
					while (rs.next())
					{
						replyMSG.append("<tr><td><a action=\"bypass -h admin_charquestmenu " + target.getName() + " " + rs.getString(1) + "\">" + rs.getString(1) + "</a></td></tr>");
					}
					replyMSG.append("</table></body></html>");
					rs.close();
					req.close();
					break;
				}
				case "name":
				{
					final QuestState qs = target.getQuestState(val[1]);
					final String state = (qs != null) ? _states[qs.getState()] : "CREATED";
					replyMSG.append("Character: <font color=\"LEVEL\">" + target.getName() + "</font><br>Quest: <font color=\"LEVEL\">" + val[1] + "</font><br>State: <font color=\"LEVEL\">" + state + "</font><br><br>");
					replyMSG.append("<center><table width=250><tr><td width=70>Var</td><td width=40>Value</td><td>New Value</td><td>&nbsp;</td></tr>");
					req = con.prepareStatement("SELECT var,value FROM character_quests WHERE charId='" + ID + "' and name='" + val[1] + "'");
					req.execute();
					rs = req.getResultSet();
					while (rs.next())
					{
						final String var_name = rs.getString(1);
						if (var_name.equals("<state>"))
						{
							continue;
						}
						
						replyMSG.append("<tr><td>" + var_name + "</td><td>" + rs.getString(2) + "</td><td><edit var=\"var" + var_name + "\" width=80 height=15></td><td><button value=\"Set\" action=\"bypass -h admin_setcharquest " + target.getName() + " " + val[1] + " " + var_name + " $var" + var_name + "\" width=30 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td><button value=\"Del\" action=\"bypass -h admin_setcharquest " + target.getName() + " " + val[1] + " " + var_name + " delete\" width=30 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
					}
					
					rs.close();
					req.close();
					replyMSG.append("</table><br><br><table width=250><tr><td>Repeatable quest:</td><td>Unrepeatable quest:</td></tr>");
					replyMSG.append("<tr><td><button value=\"Quest Complete\" action=\"bypass -h admin_setcharquest " + target.getName() + " " + val[1] + " state COMPLETED 1\" width=95 height=21 back=\"bigbutton_over\" fore=\"bigbutton\"></td>");
					replyMSG.append("<td><button value=\"Quest Complete\" action=\"bypass -h admin_setcharquest " + target.getName() + " " + val[1] + " state COMPLETED 0\" width=95 height=21 back=\"bigbutton_over\" fore=\"bigbutton\"></td></tr>");
					replyMSG.append("</table><br><br><font color=\"ff0000\">Delete Quest from DB:</font><br><button value=\"Quest Delete\" action=\"bypass -h admin_setcharquest " + target.getName() + " " + val[1] + " state DELETE\" width=95 height=21 back=\"bigbutton_over\" fore=\"bigbutton\">");
					replyMSG.append("</center></body></html>");
					break;
				}
				case "var":
				{
					replyMSG.append("Character: <font color=\"LEVEL\">" + target.getName() + "</font><br>Quests with state: <font color=\"LEVEL\">" + val[1] + "</font><br>");
					replyMSG.append("<table width=250>");
					req = con.prepareStatement("SELECT DISTINCT name FROM character_quests WHERE charId='" + ID + "' and var='<state>' and value='" + val[1] + "'");
					req.execute();
					rs = req.getResultSet();
					while (rs.next())
					{
						replyMSG.append("<tr><td><a action=\"bypass -h admin_charquestmenu " + target.getName() + " " + rs.getString(1) + "\">" + rs.getString(1) + "</a></td></tr>");
					}
					
					rs.close();
					req.close();
					replyMSG.append("</table></body></html>");
					break;
				}
				case "custom":
				{
					boolean exqdb = true;
					boolean exqch = true;
					final int qnumber = Integer.parseInt(val[1]);
					String state = null;
					String qname = null;
					QuestState qs = null;
					
					final Quest quest = ScriptManager.getInstance().getQuest(qnumber);
					if (quest != null)
					{
						qname = quest.getName();
						qs = target.getQuestState(qname);
					}
					else
					{
						exqdb = false;
					}
					
					if (qs != null)
					{
						state = _states[qs.getState()];
					}
					else
					{
						exqch = false;
						state = "N/A";
					}
					
					if (exqdb)
					{
						if (exqch)
						{
							replyMSG.append("Character: <font color=\"LEVEL\">" + target.getName() + "</font><br>Quest: <font color=\"LEVEL\">" + qname + "</font><br>State: <font color=\"LEVEL\">" + state + "</font><br><br>");
							replyMSG.append("<center><table width=250><tr><td width=70>Var</td><td width=40>Value</td><td>New Value</td><td>&nbsp;</td></tr>");
							req = con.prepareStatement("SELECT var,value FROM character_quests WHERE charId='" + ID + "' and name='" + qname + "'");
							req.execute();
							rs = req.getResultSet();
							while (rs.next())
							{
								final String var_name = rs.getString(1);
								if (var_name.equals("<state>"))
								{
									continue;
								}
								
								replyMSG.append("<tr><td>" + var_name + "</td><td>" + rs.getString(2) + "</td><td><edit var=\"var" + var_name + "\" width=80 height=15></td><td><button value=\"Set\" action=\"bypass -h admin_setcharquest " + target.getName() + " " + qname + " " + var_name + " $var" + var_name + "\" width=30 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td><button value=\"Del\" action=\"bypass -h admin_setcharquest " + target.getName() + " " + qname + " " + var_name + " delete\" width=30 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
							}
							replyMSG.append("</table><br><br><table width=250><tr><td>Repeatable quest:</td><td>Unrepeatable quest:</td></tr>");
							replyMSG.append("<tr><td><button value=\"Quest Complete\" action=\"bypass -h admin_setcharquest " + target.getName() + " " + qname + " state COMPLETED 1\" width=95 height=21 back=\"bigbutton_over\" fore=\"bigbutton\"></td>");
							replyMSG.append("<td><button value=\"Quest Complete\" action=\"bypass -h admin_setcharquest " + target.getName() + " " + qname + " state COMPLETED 0\" width=95 height=21 back=\"bigbutton_over\" fore=\"bigbutton\"></td></tr>");
							replyMSG.append("</table><br><br><font color=\"ff0000\">Delete Quest from DB:</font><br><button value=\"Quest Delete\" action=\"bypass -h admin_setcharquest " + target.getName() + " " + qname + " state DELETE\" width=95 height=21 back=\"bigbutton_over\" fore=\"bigbutton\">");
							replyMSG.append("</center></body></html>");
						}
						else
						{
							replyMSG.append("Character: <font color=\"LEVEL\">" + target.getName() + "</font><br>Quest: <font color=\"LEVEL\">" + qname + "</font><br>State: <font color=\"LEVEL\">" + state + "</font><br><br>");
							replyMSG.append("<center>Start this Quest for player:<br>");
							replyMSG.append("<button value=\"Create Quest\" action=\"bypass -h admin_setcharquest " + target.getName() + " " + qnumber + " state CREATE\" width=95 height=21 back=\"bigbutton_over\" fore=\"bigbutton\"><br><br>");
							replyMSG.append("<font color=\"ee0000\">Only for Unrepeateble quests:</font><br>");
							replyMSG.append("<button value=\"Create & Complete\" action=\"bypass -h admin_setcharquest " + target.getName() + " " + qnumber + " state CC\" width=95 height=21 back=\"bigbutton_over\" fore=\"bigbutton\"><br><br>");
							replyMSG.append("</center></body></html>");
						}
					}
					else
					{
						replyMSG.append("<center><font color=\"ee0000\">Quest with number </font><font color=\"LEVEL\">" + qnumber + "</font><font color=\"ee0000\"> doesn't exist!</font></center></body></html>");
					}
					break;
				}
			}
			
			adminReply.setHtml(replyMSG.toString());
			actor.sendPacket(adminReply);
		}
		catch (Exception e)
		{
			actor.sendMessage("There was an error.");
			LOGGER.warning(AdminShowQuests.class.getSimpleName() + ": " + e.getMessage());
		}
	}
	
	private void setQuestVar(Player target, Player actor, String[] val)
	{
		QuestState qs = target.getQuestState(val[0]);
		final String[] outval = new String[3];
		if (val[1].equals("state"))
		{
			switch (val[2])
			{
				case "COMPLETED":
				{
					qs.exitQuest(val[3].equals("1"));
					break;
				}
				case "DELETE":
				{
					Quest.deleteQuestInDb(qs, true);
					qs.exitQuest(true);
					target.sendPacket(new QuestList(target));
					target.sendPacket(new ExShowQuestMark(qs.getQuest().getId()));
					break;
				}
				case "CREATE":
				{
					qs = ScriptManager.getInstance().getQuest(Integer.parseInt(val[0])).newQuestState(target);
					qs.setState(State.STARTED);
					qs.setCond(1);
					target.sendPacket(new QuestList(target));
					target.sendPacket(new ExShowQuestMark(qs.getQuest().getId()));
					val[0] = qs.getQuest().getName();
					break;
				}
				case "CC":
				{
					qs = ScriptManager.getInstance().getQuest(Integer.parseInt(val[0])).newQuestState(target);
					qs.exitQuest(false);
					target.sendPacket(new QuestList(target));
					target.sendPacket(new ExShowQuestMark(qs.getQuest().getId()));
					val[0] = qs.getQuest().getName();
					break;
				}
			}
		}
		else
		{
			if (val[2].equals("delete"))
			{
				qs.unset(val[1]);
			}
			else
			{
				qs.set(val[1], val[2]);
			}
			
			target.sendPacket(new QuestList(target));
			target.sendPacket(new ExShowQuestMark(qs.getQuest().getId()));
		}
		
		actor.sendMessage("");
		outval[0] = "name";
		outval[1] = val[0];
		showQuestMenu(target, actor, outval);
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
