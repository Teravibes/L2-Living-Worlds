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
package handlers.bypass.communityboard;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.handler.IWriteBoardHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.clan.ClanMember;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.util.HtmlUtil;

/**
 * Clan board.
 * @author Zoey76, Skache
 */
public class ClanBoard implements IWriteBoardHandler
{
	private static final Logger LOGGER = Logger.getLogger(ClanBoard.class.getName());
	private static final String[] COMMANDS =
	{
		"_bbsclan",
		"_bbsclan_list",
		"_bbsclan_clanhome"
	};
	
	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
	
	@Override
	public boolean onCommand(String command, Player player)
	{
		if (command.equals("_bbsclan"))
		{
			CommunityBoardHandler.getInstance().addBypass(player, "Clan", command);
			final Clan clan = player.getClan();
			if ((clan == null) || (clan.getLevel() < 2))
			{
				clanList(player, 1);
			}
			else
			{
				clanHome(player);
			}
		}
		else if (command.startsWith("_bbsclan_clanlist"))
		{
			CommunityBoardHandler.getInstance().addBypass(player, "Clan List", command);
			if (command.equals("_bbsclan_clanlist"))
			{
				clanList(player, 1);
			}
			else if (command.startsWith("_bbsclan_clanlist;"))
			{
				try
				{
					clanList(player, Integer.parseInt(command.split(";")[1]));
				}
				catch (Exception e)
				{
					clanList(player, 1);
					LOGGER.warning(ClanBoard.class.getSimpleName() + ": " + player + " send invalid clan list bypass " + command + "!");
				}
			}
		}
		else if (command.startsWith("_bbsclan_clanhome"))
		{
			CommunityBoardHandler.getInstance().addBypass(player, "Clan Home", command);
			if (command.equals("_bbsclan_clanhome"))
			{
				clanHome(player);
			}
			else if (command.startsWith("_bbsclan_clanhome;"))
			{
				try
				{
					clanHome(player, Integer.parseInt(command.split(";")[1]));
				}
				catch (Exception e)
				{
					clanHome(player);
					LOGGER.warning(ClanBoard.class.getSimpleName() + ": " + player + " send invalid clan home bypass " + command + "!");
				}
			}
		}
		else if (command.startsWith("_bbsclan_clannotice_edit;"))
		{
			CommunityBoardHandler.getInstance().addBypass(player, "Clan Edit", command);
			clanNotice(player, player.getClanId());
		}
		else if (command.startsWith("_bbsclan_clannotice_enable"))
		{
			CommunityBoardHandler.getInstance().addBypass(player, "Clan Notice Enable", command);
			final Clan clan = player.getClan();
			if (clan != null)
			{
				clan.setNoticeEnabled(true);
			}
			
			clanNotice(player, player.getClanId());
		}
		else if (command.startsWith("_bbsclan_clannotice_disable"))
		{
			CommunityBoardHandler.getInstance().addBypass(player, "Clan Notice Disable", command);
			final Clan clan = player.getClan();
			if (clan != null)
			{
				clan.setNoticeEnabled(false);
			}
			
			clanNotice(player, player.getClanId());
		}
		else if (command.startsWith("_bbsclan;mail"))
		{
			CommunityBoardHandler.getInstance().addBypass(player, "Clan Mail", command);
			final Clan clan = player.getClan();
			if (clan != null)
			{
				sendClanMail(player, clan.getId());
			}
		}
		else
		{
			CommunityBoardHandler.separateAndSend("<html><body><br><br><center>Command " + command + " needs development.</center><br><br></body></html>", player);
		}
		
		return true;
	}
	
	private void clanNotice(Player player, int clanId)
	{
		final Clan cl = ClanTable.getInstance().getClan(clanId);
		if (cl != null)
		{
			if (cl.getLevel() < 2)
			{
				player.sendPacket(SystemMessageId.THERE_ARE_NO_COMMUNITIES_IN_MY_CLAN_CLAN_COMMUNITIES_ARE_ALLOWED_FOR_CLANS_WITH_SKILL_LEVELS_OF_2_AND_HIGHER);
				onCommand("_bbsclan_clanlist", player);
			}
			else
			{
				StringBuilder html = new StringBuilder();
				html.append("<html><body><br><table><tr><td FIXWIDTH=15>&nbsp;</td><td width=630 height=30 align=left><a action=\"bypass _bbshome\">Home</a>&nbsp;&gt;<a action=\"bypass _bbsclan_clanlist\">&nbsp;&amp;$809;</a>&nbsp;&gt;<a action=\"bypass _bbsclan_clanhome;");
				html.append(clanId);
				html.append("\">&amp;$802;</a></td></tr></table>");
				html.append("<table border=0 cellspacing=0 cellpadding=0 width=625 bgcolor=000000>");
				html.append("<tr><td height=10></td></tr>");
				html.append("<tr><td fixWIDTH=5></td><td fixWIDTH=630>");
				html.append("<a action=\"bypass _bbsclan_clanhome;");
				html.append(clanId);
				html.append(";announce\">[Clan Announcement]</a>&nbsp;&nbsp;");
				html.append("<a action=\"bypass _bbsclan_clanhome;");
				html.append(clanId);
				html.append(";cbb\">[Clan Bulletin Board]</a>&nbsp;&nbsp;");
				html.append("<a action=\"bypass _bbsclan_clanhome;");
				html.append(clanId);
				html.append(";cbb\">[Clan Management]</a>&nbsp;&nbsp;");
				html.append("<a action=\"bypass _bbsclan;mail\">[Clan Mail]</a>&nbsp;&nbsp;");
				html.append("[Clan Notice]</a>&nbsp;&nbsp;");
				html.append("</td><td fixWIDTH=5></td></tr>");
				html.append("<tr><td height=10></td></tr>");
				html.append("</table>");
				if (player.isClanLeader())
				{
					html.append("<br><br><center><table width=610 border=0 cellspacing=0 cellpadding=0><tr><td fixwidth=610><font color=\"AAAAAA\">The Clan Notice function allows the clan leader to send messages through a pop-up window to clan members at login.</font> </td></tr><tr><td height=20></td></tr>");
					final Clan clan = player.getClan();
					if (clan.isNoticeEnabled())
					{
						html.append("<tr><td fixwidth=610> Clan Notice Function:&nbsp;&nbsp;&$227;&nbsp;&nbsp;&nbsp;/&nbsp;&nbsp;&nbsp;<a action=\"bypass _bbsclan_clannotice_disable\">&$228;</a>");
					}
					else
					{
						html.append("<tr><td fixwidth=610> Clan Notice Function:&nbsp;&nbsp;<a action=\"bypass _bbsclan_clannotice_enable\">&$227;</a>&nbsp;&nbsp;&nbsp;/&nbsp;&nbsp;&nbsp;&$228;");
					}
					
					html.append("</td></tr></table><img src=\"L2UI.Squaregray\" width=\"610\" height=\"1\"><br> <br><table width=610 border=0 cellspacing=2 cellpadding=0><tr><td>Edit Notice: </td></tr><tr><td height=5></td></tr><tr><td><MultiEdit var =\"Content\" width=610 height=100></td></tr></table><br><table width=610 border=0 cellspacing=0 cellpadding=0><tr><td height=5></td></tr><tr><td align=center FIXWIDTH=65><button value=\"&$140;\" action=\"Write Notice Set _ Content Content Content\" back=\"l2ui_ch3.smallbutton2_down\" width=65 height=20 fore=\"l2ui_ch3.smallbutton2\" ></td><td align=center FIXWIDTH=45></td><td align=center FIXWIDTH=500></td></tr></table></center></body></html>");
					HtmlUtil.sendCBHtml(player, html.toString(), clan.getNotice());
				}
				else
				{
					html.append("<img src=\"L2UI.squareblank\" width=\"1\" height=\"10\"><center><table border=0 cellspacing=0 cellpadding=0><tr><td>You are not your clan's leader, and therefore cannot change the clan notice</td></tr></table>");
					final Clan clan = player.getClan();
					if (clan.isNoticeEnabled())
					{
						html.append("<table border=0 cellspacing=0 cellpadding=0><tr><td>The current clan notice:</td></tr><tr><td fixwidth=5></td><td FIXWIDTH=630 align=left>" + clan.getNotice() + "</td><td fixqqwidth=5></td></tr></table>");
					}
					html.append("</center></body></html>");
					CommunityBoardHandler.separateAndSend(html.toString(), player);
				}
			}
		}
	}
	
	private void clanList(Player player, int indexValue)
	{
		int index = indexValue;
		if (index < 1)
		{
			index = 1;
		}
		
		final StringBuilder html = new StringBuilder(2048);
		html.append("<html><body><br><center><table><tr><td FIXWIDTH=15>&nbsp;</td><td width=630 height=30 align=left><a action=\"bypass _bbshome\">Home</a>&nbsp;>&nbsp;&amp;$809;</td></tr></table>");
		html.append("<table border=0 cellspacing=0 cellpadding=0 width=625 bgcolor=000000>");
		html.append("<tr><td height=10></td></tr>");
		html.append("<tr><td fixWIDTH=5></td><td fixWIDTH=630>");
		
		// Show "GO TO MY CLAN" button if player has a clan.
		if (player.getClan() != null)
		{
			html.append("<a action=\"bypass _bbsclan_clanhome;").append(player.getClan().getId());
			html.append("\">[GO TO MY CLAN]</a>&nbsp;&nbsp;");
		}
		else
		{
			html.append("<img src=\"L2UI.SquareBlank\" width=\"0\" height=\"0\">&nbsp;&nbsp;");
		}
		
		html.append("</td><td fixWIDTH=5></td></tr>");
		html.append("<tr><td height=10></td></tr>");
		html.append("</table><br>");
		html.append("<table border=0 cellspacing=0 cellpadding=2 bgcolor=000000 width=625>");
		html.append("<tr><td FIXWIDTH=5></td><td FIXWIDTH=200 align=center>CLAN NAME</td><td FIXWIDTH=200 align=center>CLAN LEADER</td>");
		html.append("<td FIXWIDTH=100 align=center>CLAN LEVEL</td><td FIXWIDTH=100 align=center>CLAN MEMBERS</td><td FIXWIDTH=5></td></tr>");
		html.append("</table><img src=\"L2UI.Squareblank\" width=\"1\" height=\"5\">");
		
		int startIndex = (index - 1) * 10;
		int endIndex = startIndex + 10;
		
		int i = 0;
		int currentIndex = 0;
		for (Clan cl : ClanTable.getInstance().getClans())
		{
			if ((currentIndex >= startIndex) && (currentIndex < endIndex))
			{
				html.append("<img src=\"L2UI.SquareBlank\" width=\"610\" height=\"3\"><table border=0 cellspacing=0 cellpadding=0 width=610><tr> <td FIXWIDTH=5></td><td FIXWIDTH=200 align=center><a action=\"bypass _bbsclan_clanhome;");
				html.append(cl.getId());
				html.append("\">");
				html.append(cl.getName());
				html.append("</a></td><td FIXWIDTH=200 align=center>");
				html.append(cl.getLeaderName());
				html.append("</td><td FIXWIDTH=100 align=center>");
				html.append(cl.getLevel());
				html.append("</td><td FIXWIDTH=100 align=center>");
				html.append(cl.getMembersCount());
				html.append("</td><td FIXWIDTH=5></td></tr><tr><td height=5></td></tr></table><img src=\"L2UI.SquareBlank\" width=\"610\" height=\"3\"><img src=\"L2UI.SquareGray\" width=\"610\" height=\"1\">");
			}
			
			currentIndex++;
			if (currentIndex >= endIndex)
			{
				break;
			}
		}
		
		html.append("<img src=\"L2UI.SquareBlank\" width=\"610\" height=\"2\"><table cellpadding=0 cellspacing=2 border=0><tr>");
		if (index == 1)
		{
			html.append("<td><button action=\"\" back=\"l2ui_ch3.prev1_down\" fore=\"l2ui_ch3.prev1\" width=16 height=16 ></td>");
		}
		else
		{
			html.append("<td><button action=\"bypass _bbsclan_clanlist;");
			html.append(index - 1);
			html.append("\" back=\"l2ui_ch3.prev1_down\" fore=\"l2ui_ch3.prev1\" width=16 height=16 ></td>");
		}
		
		int nbp = (int) Math.ceil((double) ClanTable.getInstance().getClanCount() / 10); // Calculate number of pages
		for (i = 1; i <= nbp; i++)
		{
			if (i == index)
			{
				html.append("<td> ");
				html.append(i);
				html.append(" </td>");
			}
			else
			{
				html.append("<td><a action=\"bypass _bbsclan_clanlist;");
				html.append(i);
				html.append("\"> ");
				html.append(i);
				html.append(" </a></td>");
			}
		}
		
		if (index == nbp)
		{
			html.append("<td><button action=\"\" back=\"l2ui_ch3.next1_down\" fore=\"l2ui_ch3.next1\" width=16 height=16 ></td>");
		}
		else
		{
			html.append("<td><button action=\"bypass _bbsclan_clanlist;");
			html.append(index + 1);
			html.append("\" back=\"l2ui_ch3.next1_down\" fore=\"l2ui_ch3.next1\" width=16 height=16 ></td>");
		}
		html.append("</tr></table><table border=0 cellspacing=0 cellpadding=0><tr><td width=610><img src=\"sek.cbui141\" width=\"610\" height=\"1\"></td></tr></table><table border=0><tr><td><combobox width=75 var=keyword list=\"Clan Name;Clan Ruler\"></td><td><edit var = \"Search\" width=130 height=11 length=\"16\"></td>" + "<td><button value=\"&$420;\" action=\"Write Search -1 0 Search keyword keyword\" back=\"l2ui_ch3.smallbutton2_down\" width=65 height=20 fore=\"l2ui_ch3.smallbutton2\"> </td> </tr></table><br><br></center></body></html>");
		CommunityBoardHandler.separateAndSend(html.toString(), player);
	}
	
	private void clanHome(Player player)
	{
		clanHome(player, player.getClan().getId());
	}
	
	private void clanHome(Player player, int clanId)
	{
		final Clan cl = ClanTable.getInstance().getClan(clanId);
		if (cl != null)
		{
			if (cl.getLevel() < 2)
			{
				player.sendPacket(SystemMessageId.THERE_ARE_NO_COMMUNITIES_IN_MY_CLAN_CLAN_COMMUNITIES_ARE_ALLOWED_FOR_CLANS_WITH_SKILL_LEVELS_OF_2_AND_HIGHER);
				onCommand("_bbsclan_clanlist", player);
			}
			else
			{
				StringBuilder html = new StringBuilder();
				html.append("<html><body><br><table><tr><td FIXWIDTH=15>&nbsp;</td><td width=630 height=30 align=left><a action=\"bypass _bbshome\">Home</a> &nbsp;&gt; <a action=\"bypass _bbsclan_clanlist\">&nbsp;&amp;$809;</a>&nbsp;&gt;");
				html.append("&amp;$802; </a></td></tr></table>");
				
				// Add the background table (without buttons) for everyone.
				html.append("<table border=0 cellspacing=0 cellpadding=0 width=625 bgcolor=000000>");
				html.append("<tr><td height=10></td></tr>");
				html.append("<tr><td fixWIDTH=5></td><td fixwidth=630>");
				
				// Show the buttons only if the player is the clan leader.
				if (player.getObjectId() == cl.getLeaderId())
				{
					html.append("<a action=\"bypass _bbsclan_clanhome;");
					html.append(clanId);
					html.append(";announce\">[Clan Announcement]</a>&nbsp;&nbsp;");
					html.append("<a action=\"bypass _bbsclan_clanhome;");
					html.append(clanId);
					html.append(";cbb\">[Clan Bulletin Board]</a>&nbsp;&nbsp;");
					html.append("<a action=\"bypass _bbsclan_clanhome;");
					html.append(clanId);
					html.append(";cbb\">[Clan Management]</a>&nbsp;&nbsp;");
					html.append("<a action=\"bypass _bbsclan;mail\">[Clan Mail]</a>&nbsp;&nbsp;");
					html.append("<a action=\"bypass _bbsclan_clannotice_edit;");
					html.append(clanId);
					html.append(";cnotice\">[Clan Notice]</a>&nbsp;&nbsp;");
				}
				else
				{
					html.append("<img src=\"L2UI.SquareBlank\" width=\"0\" height=\"0\">&nbsp;&nbsp;");
				}
				
				html.append("</td><td fixWIDTH=5></td></tr>");
				html.append("<tr><td height=10></td></tr></table>");
				html.append("<table border=0 cellspacing=0 cellpadding=0 width=610><tr><td height=10></td></tr>");
				html.append("<tr><td fixWIDTH=5></td><td fixwidth=290 valign=top></td>");
				html.append("<td fixWIDTH=5></td><td fixWIDTH=5 align=center valign=top><img src=\"l2ui.squaregray\" width=2 height=128></td>");
				html.append("<td fixWIDTH=5></td><td fixwidth=295><table border=0 cellspacing=0 cellpadding=0 width=295>");
				html.append("<tr><td fixWIDTH=100 align=left>CLAN NAME</td><td fixWIDTH=195 align=left>");
				html.append(cl.getName());
				html.append("</td></tr>");
				html.append("<tr><td height=7></td></tr>");
				html.append("<tr><td fixWIDTH=100 align=left>CLAN LEVEL</td><td fixWIDTH=195 align=left height=16>");
				html.append(cl.getLevel());
				html.append("</td></tr>");
				html.append("<tr><td height=7></td></tr>");
				html.append("<tr><td fixWIDTH=100 align=left>CLAN MEMBERS</td><td fixWIDTH=195 align=left height=16>");
				html.append(cl.getMembersCount());
				html.append("</td></tr>");
				html.append("<tr><td height=7></td></tr>");
				html.append("<tr><td fixWIDTH=100 align=left>CLAN LEADER</td><td fixWIDTH=195 align=left height=16>");
				html.append(cl.getLeaderName());
				html.append("</td></tr>");
				html.append("<tr><td height=7></td></tr>");
				html.append("<tr><td fixWIDTH=100 align=left>ALLIANCE</td><td fixWIDTH=195 align=left height=16>");
				html.append((cl.getAllyName() != null) ? cl.getAllyName() : "");
				html.append("</td></tr></table></td><td fixWIDTH=5></td></tr>");
				html.append("<tr><td height=10></td></tr></table>");
				html.append("<img src=\"L2UI.squareblank\" width=\"1\" height=\"5\"><img src=\"L2UI.squaregray\" width=\"610\" height=\"1\"><br></center><br> <br></body></html>");
				CommunityBoardHandler.separateAndSend(html.toString(), player);
			}
		}
	}
	
	private void clanSearch(Player player, String searchType, String searchKeyword)
	{
		List<Clan> clans = filterClans(searchType, searchKeyword);
		StringBuilder html = generateSearchHtml(player, clans, searchType, searchKeyword);
		CommunityBoardHandler.separateAndSend(html.toString(), player);
	}
	
	private List<Clan> filterClans(String searchType, String searchKeyword)
	{
		List<Clan> clans = new ArrayList<>();
		
		if (!searchType.isEmpty() && !searchKeyword.isEmpty())
		{
			boolean searchByName = searchType.equalsIgnoreCase("Clan Name");
			boolean searchByLeader = searchType.equalsIgnoreCase("Clan Ruler");
			
			for (Clan clan : ClanTable.getInstance().getClans())
			{
				boolean matches = false;
				
				if (searchByName && clan.getName().toLowerCase().contains(searchKeyword.toLowerCase()))
				{
					matches = true;
				}
				
				if (searchByLeader)
				{
					ClanMember leader = clan.getLeader();
					if ((leader != null) && leader.getName().toLowerCase().contains(searchKeyword.toLowerCase()))
					{
						matches = true;
					}
				}
				
				if (matches)
				{
					clans.add(clan);
				}
			}
		}
		
		return clans;
	}
	
	private StringBuilder generateSearchHtml(Player player, List<Clan> clans, String searchType, String searchKeyword)
	{
		StringBuilder html = new StringBuilder(2048);
		html.append("<html><body><br><br><center><br1><br1><table border=0 cellspacing=0 cellpadding=0><tr><td FIXWIDTH=15>&nbsp;</td><td width=610 height=30 align=left>");
		html.append("<a action=\"bypass _bbshome\">Home</a> &nbsp;&gt; <a action=\"bypass _bbsclan_clanlist\">&nbsp;&amp;$809;</a>");
		if (!searchKeyword.isEmpty())
		{
			html.append("&nbsp;&gt; <a action=\"bypass _bbsclan_clanlist\"> Search </a>");
		}
		html.append("</td></tr></table><table border=0 cellspacing=0 cellpadding=0 width=625 bgcolor=000000><tr><td height=10></td></tr><tr><td fixWIDTH=5></td><td fixWIDTH=630><a action=\"bypass _bbsclan_clanhome;");
		html.append(player.getClan() != null ? player.getClan().getId() : 0);
		html.append("\">[GO TO MY CLAN]</a>&nbsp;&nbsp;</td><td fixWIDTH=5></td></tr><tr><td height=10></td></tr></table><br><table border=0 cellspacing=0 cellpadding=2 bgcolor=000000 width=625><tr><td FIXWIDTH=5></td><td FIXWIDTH=200 align=center>CLAN NAME</td><td FIXWIDTH=200 align=center>CLAN LEADER</td><td FIXWIDTH=100 align=center>CLAN LEVEL</td><td FIXWIDTH=100 align=center>CLAN MEMBERS</td><td FIXWIDTH=5></td></tr></table><img src=\"L2UI.Squareblank\" width=\"1\" height=\"5\">");
		
		if (!clans.isEmpty())
		{
			for (Clan clan : clans)
			{
				html.append("<img src=\"L2UI.SquareBlank\" width=\"610\" height=\"3\"><table border=0 cellspacing=0 cellpadding=0 width=610><tr><td FIXWIDTH=5></td><td FIXWIDTH=200 align=center><a action=\"bypass _bbsclan_clanhome;");
				html.append(clan.getId());
				html.append("\">");
				html.append(clan.getName());
				html.append("</a></td><td FIXWIDTH=200 align=center>");
				html.append(clan.getLeaderName());
				html.append("</td><td FIXWIDTH=100 align=center>");
				html.append(clan.getLevel());
				html.append("</td><td FIXWIDTH=100 align=center>");
				html.append(clan.getMembersCount());
				html.append("</td><td FIXWIDTH=5></td></tr><tr><td height=5></td></tr></table><img src=\"L2UI.SquareBlank\" width=\"610\" height=\"3\"><img src=\"L2UI.SquareGray\" width=\"610\" height=\"1\">");
			}
		}
		else
		{
			html.append("<br><br><font>No ");
			html.append(searchType.equalsIgnoreCase("Clan Name") ? "clans" : "rulers");
			html.append(" found matching the name: </font>");
			html.append(searchKeyword);
			html.append("<br><br>");
		}
		
		html.append("<img src=\"L2UI.SquareBlank\" width=\"610\" height=\"2\"><table cellpadding=0 cellspacing=2 border=0><tr><td><combobox width=75 var=keyword list=\"Clan Name;Clan Ruler\"></td><td><edit var = \"Search\" width=130 height=11 length=\"16\"></td><td><button value=\"Search\" action=\"Write Search -1 0 Search keyword keyword\" back=\"l2ui_ch3.smallbutton2_down\" width=65 height=20 fore=\"l2ui_ch3.smallbutton2\"> </td></tr></table><br><br></center></body></html>");
		
		return html;
	}
	
	public String generateClanMailHtml(Player player, int clanId, String clanName)
	{
		StringBuilder html = new StringBuilder();
		html.append("<html><body><br>");
		html.append("<table width=610>");
		html.append("<tr>");
		html.append("<td width=15>&nbsp;</td>");
		html.append("<td width=595 height=30>");
		html.append("<a action=\"bypass _bbshome\">Home</a>&nbsp;>&nbsp;");
		html.append("<a action=\"bypass _bbsclan_clanlist\">&nbsp;&amp;$809;</a>&nbsp;>&nbsp;");
		html.append("<a action=\"bypass _bbsclan_clanhome;");
		html.append(clanId);
		html.append("\">&$802;</a>");
		html.append("</td>");
		html.append("</tr>");
		html.append("</table>");
		html.append("<img src=\"L2UI.squaregray\" width=610 height=1><br>");
		html.append("<table width=610>");
		html.append("<tr>");
		html.append("<td align=right width=60 height=24>Recipient:</td>");
		html.append("<td width=540 height=24>");
		html.append(clanName);
		html.append(" Clan Members</td>");
		html.append("</tr><tr>");
		html.append("<td align=right height=24>&$413;:</td>");
		html.append("<td><edit var=\"Title\" width=540 height=13 length=128></td>");
		html.append("</tr><tr>");
		html.append("<td valign=top align=right height=24>&$427;:</td>");
		html.append("<td><MultiEdit var=\"Content\" width=540 height=313></td>");
		html.append("</tr>");
		html.append("</table><br>");
		html.append("<img src=\"L2UI.squaregray\" width=610 height=1><br>");
		html.append("<table width=610>");
		html.append("<tr>");
		html.append("<td width=465 align=center></td>");
		html.append("<td width=70 align=center>");
		html.append("<button value=\"&$1078;\" action=\"Write CMail mail " + clanId + " Title Title Content\" back=\"l2ui_ch3.smallbutton2_down\" width=65 height=20 fore=\"l2ui_ch3.smallbutton2\"></td>");
		html.append("<td width=70 align=center>");
		html.append("<button value=\"&$141;\" action=\"bypass _bbsclan_clanlist\" back=\"l2ui_ch3.smallbutton2_down\" width=65 height=20 fore=\"l2ui_ch3.smallbutton2\"></td>");
		html.append("<td width=5></td>");
		html.append("</tr>");
		html.append("</table>");
		html.append("</body></html>");
		return html.toString();
	}
	
	private void sendClanMail(Player player, int clanId)
	{
		final Clan clan = ClanTable.getInstance().getClan(clanId);
		if (clan == null)
		{
			return;
		}
		
		if ((player.getClanId() != clanId) || !player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.ONLY_THE_CLAN_LEADER_IS_ENABLED);
			clanList(player, 1);
			return;
		}
		
		String content = generateClanMailHtml(player, clanId, clan.getName());
		CommunityBoardHandler.separateAndSend(content, player);
	}
	
	@Override
	public boolean writeCommunityBoardCommand(Player player, String arg1, String arg2, String arg3, String arg4, String arg5)
	{
		final Clan clan = ClanTable.getInstance().getClan(player.getClanId());
		
		if ("Set".equalsIgnoreCase(arg1) && "_".equals(arg2))
		{
			if (clan != null)
			{
				clan.setNotice(arg3);
				player.sendPacket(SystemMessageId.YOUR_CLAN_NOTICE_HAS_BEEN_SAVED);
				return true;
			}
		}
		else if (arg1.equalsIgnoreCase("mail"))
		{
			if (Integer.parseInt(arg2) != player.getClanId())
			{
				return true;
			}
			
			if (clan == null)
			{
				return true;
			}
			
			final StringBuilder membersList = new StringBuilder();
			for (ClanMember players : clan.getMembers())
			{
				if (players != null)
				{
					if (membersList.length() > 0)
					{
						membersList.append(";");
					}
					
					membersList.append(players.getName());
				}
			}
			
			MailBoard.getInstance().sendMail(membersList.toString(), arg4, arg5, player);
			clanHome(player, player.getClanId());
		}
		else if (arg1.equals("-1") && arg2.equals("0"))
		{
			String searchKeyword = arg3;
			String searchType = arg4;
			clanSearch(player, searchType, searchKeyword);
			return true;
		}
		
		return false;
	}
}
