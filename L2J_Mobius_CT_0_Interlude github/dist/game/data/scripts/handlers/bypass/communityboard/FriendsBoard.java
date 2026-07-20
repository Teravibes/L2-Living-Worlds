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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.handler.IParseBoardHandler;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.player.BlockList;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.FriendList;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Friends board.
 * @author Skache
 */
public class FriendsBoard implements IParseBoardHandler
{
	private static final String FRIENDLIST_DELETE_BUTTON = "<br>\n<table><tr><td width=10></td><td>Are you sure you want to delete all friends from your Friends List?</td><td width=20></td><td><button value=\"OK\" action=\"bypass _friend;delall\" back=\"l2ui_ch3.smallbutton2_down\" width=65 height=20 fore=\"l2ui_ch3.smallbutton2\"></td><td><button value=\"Cancel\" action=\"bypass _friendlist\" back=\"l2ui_ch3.smallbutton2_down\" width=65 height=20 fore=\"l2ui_ch3.smallbutton2\"></td></tr></table>";
	private static final String BLOCKLIST_DELETE_BUTTON = "<br>\n<table><tr><td width=10></td><td>Are you sure you want to delete all players from your Block List?</td><td width=20></td><td><button value=\"OK\" action=\"bypass _block;delall\" back=\"l2ui_ch3.smallbutton2_down\" width=65 height=20 fore=\"l2ui_ch3.smallbutton2\"></td><td><button value=\"Cancel\" action=\"bypass _friendblocklist\" back=\"l2ui_ch3.smallbutton2_down\" width=65 height=20 fore=\"l2ui_ch3.smallbutton2\"></td></tr></table>";
	
	private static final String[] COMMANDS =
	{
		"_friendlist",
		"_friendblocklist",
		"_friend",
		"_block"
	};
	
	@Override
	public boolean onCommand(String command, Player player)
	{
		if (command.equals("_friendlist") || command.equals("_friendlist_0_"))
		{
			CommunityBoardHandler.getInstance().addBypass(player, "Friends List", command);
			showFriendsList(player, false);
		}
		else if (command.equals("_friendblocklist"))
		{
			CommunityBoardHandler.getInstance().addBypass(player, "Ignore list", command);
			showBlockList(player, false);
		}
		else if (command.startsWith("_friend"))
		{
			final StringTokenizer st = new StringTokenizer(command, ";");
			st.nextToken();
			final String action = st.nextToken();
			if (action.equals("select"))
			{
				player.selectFriend((st.hasMoreTokens()) ? Integer.parseInt(st.nextToken()) : 0);
				showFriendsList(player, false);
			}
			else if (action.equals("deselect"))
			{
				player.deselectFriend((st.hasMoreTokens()) ? Integer.parseInt(st.nextToken()) : 0);
				showFriendsList(player, false);
			}
			else if (action.equals("delall"))
			{
				try (Connection con = DatabaseFactory.getConnection())
				{
					final PreparedStatement statement = con.prepareStatement("DELETE FROM character_friends WHERE char_id = ? OR friend_id = ?");
					statement.setInt(1, player.getObjectId());
					statement.setInt(2, player.getObjectId());
					statement.execute();
					statement.close();
				}
				catch (Exception e)
				{
					LOG.warning("could not delete friends objectid: " + e);
				}
				
				for (int friendId : player.getFriendList())
				{
					final Player friend = World.getInstance().getPlayer(friendId);
					if (friend != null)
					{
						friend.getFriendList().remove(Integer.valueOf(friend.getObjectId()));
						friend.getSelectedFriendList().remove(Integer.valueOf(friend.getObjectId()));
						
						friend.sendPacket(new FriendList(friend));
					}
				}
				
				player.getFriendList().clear();
				player.getSelectedFriendList().clear();
				showFriendsList(player, false);
				player.sendMessage("You have cleared your friend list.");
				player.sendPacket(new FriendList(player));
			}
			else if (action.equals("delconfirm"))
			{
				showFriendsList(player, true);
			}
			else if (action.equals("del"))
			{
				try (Connection con = DatabaseFactory.getConnection())
				{
					for (int friendId : player.getSelectedFriendList())
					{
						final PreparedStatement statement = con.prepareStatement("DELETE FROM character_friends WHERE (charId = ? AND friendId = ?) OR (charId = ? AND friendId = ?)");
						statement.setInt(1, player.getObjectId());
						statement.setInt(2, friendId);
						statement.setInt(3, friendId);
						statement.setInt(4, player.getObjectId());
						statement.execute();
						statement.close();
						
						final String name = CharInfoTable.getInstance().getNameById(friendId);
						final Player friend = World.getInstance().getPlayer(friendId);
						if (friend != null)
						{
							friend.getFriendList().remove(Integer.valueOf(friend.getObjectId()));
							friend.sendPacket(new FriendList(friend));
						}
						
						final SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_BEEN_DELETED_FROM_YOUR_FRIENDS_LIST);
						sm.addString(name);
						player.sendPacket(sm);
						player.getFriendList().remove(Integer.valueOf(friendId));
					}
				}
				catch (Exception e)
				{
					LOG.warning("could not delete friend objectid: " + e);
				}
				
				player.getSelectedFriendList().clear();
				showFriendsList(player, false);
				player.sendPacket(new FriendList(player));
			}
			else if (action.equals("mail"))
			{
				if (!player.getSelectedFriendList().isEmpty())
				{
					showMailWrite(player);
				}
				else
				{
					player.sendMessage("Please select character.");
				}
			}
		}
		else if (command.startsWith("_block"))
		{
			final StringTokenizer st = new StringTokenizer(command, ";");
			st.nextToken();
			final String action = st.nextToken();
			if (action.equals("select"))
			{
				player.selectBlock((st.hasMoreTokens()) ? Integer.parseInt(st.nextToken()) : 0);
				showBlockList(player, false);
			}
			else if (action.equals("deselect"))
			{
				player.deselectBlock((st.hasMoreTokens()) ? Integer.parseInt(st.nextToken()) : 0);
				showBlockList(player, false);
			}
			else if (action.equals("delall"))
			{
				final List<Integer> list = new ArrayList<>();
				list.addAll(player.getBlockList().getBlockList());
				
				for (Integer blockId : list)
				{
					BlockList.removeFromBlockList(player, blockId);
				}
				
				player.getSelectedBlocksList().clear();
				showBlockList(player, false);
			}
			else if (action.equals("delconfirm"))
			{
				showBlockList(player, true);
			}
			else if (action.equals("del"))
			{
				for (Integer blockId : player.getSelectedBlocksList())
				{
					BlockList.removeFromBlockList(player, blockId);
				}
				
				player.getSelectedBlocksList().clear();
				showBlockList(player, false);
			}
		}
		
		return true;
	}
	
	private void showFriendsList(Player player, boolean delMsg)
	{
		String html = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/friends/friends_list.html");
		if (html == null)
		{
			return;
		}
		
		final List<Integer> list = new ArrayList<>(player.getFriendList());
		final List<Integer> slist = new ArrayList<>(player.getSelectedFriendList());
		
		final StringBuilder friendListHtml = new StringBuilder();
		
		for (Integer id : list)
		{
			if (slist.contains(id))
			{
				continue;
			}
			
			final String friendName = CharInfoTable.getInstance().getNameById(id);
			if (friendName == null)
			{
				continue;
			}
			
			final Player friend = World.getInstance().getPlayer(id);
			friendListHtml.append("<a action=\"bypass _friend;select;");
			friendListHtml.append(id);
			friendListHtml.append("\">[Select]</a>&nbsp;");
			friendListHtml.append(friendName);
			friendListHtml.append(" ");
			friendListHtml.append(((friend != null) && friend.isOnline()) ? "(on)" : "(off)");
			friendListHtml.append("<br1>");
		}
		
		html = html.replace("%friendslist%", friendListHtml.toString());
		
		friendListHtml.setLength(0);
		
		for (Integer id : slist)
		{
			final String friendName = CharInfoTable.getInstance().getNameById(id);
			if (friendName == null)
			{
				continue;
			}
			
			final Player friend = World.getInstance().getPlayer(id);
			friendListHtml.append("<a action=\"bypass _friend;deselect;");
			friendListHtml.append(id);
			friendListHtml.append("\">[Deselect]</a>&nbsp;");
			friendListHtml.append(friendName);
			friendListHtml.append(" ");
			friendListHtml.append(((friend != null) && friend.isOnline()) ? "(on)" : "(off)");
			friendListHtml.append("<br1>");
		}
		
		html = html.replace("%selectedFriendsList%", friendListHtml.toString());
		html = html.replace("%deleteMSG%", delMsg ? FRIENDLIST_DELETE_BUTTON : "");
		CommunityBoardHandler.separateAndSend(html, player);
	}
	
	private void showBlockList(Player player, boolean delMsg)
	{
		String html = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/friends/friends_block_list.html");
		if (html == null)
		{
			return;
		}
		
		final List<Integer> list = new ArrayList<>(player.getBlockList().getBlockList());
		final List<Integer> slist = new ArrayList<>(player.getSelectedBlocksList());
		
		final StringBuilder blockListHtml = new StringBuilder();
		
		for (Integer id : list)
		{
			if (slist.contains(id))
			{
				continue;
			}
			
			final String blockName = CharInfoTable.getInstance().getNameById(id);
			if (blockName == null)
			{
				continue;
			}
			
			final Player block = World.getInstance().getPlayer(id);
			blockListHtml.append("<a action=\"bypass _block;select;");
			blockListHtml.append(id).append("\">[Select]</a>&nbsp;");
			blockListHtml.append(blockName).append(" ");
			blockListHtml.append(((block != null) && block.isOnline()) ? "(on)" : "(off)");
			blockListHtml.append("<br1>");
		}
		
		html = html.replace("%blocklist%", blockListHtml.toString());
		
		blockListHtml.setLength(0);
		
		for (Integer id : slist)
		{
			final String blockName = CharInfoTable.getInstance().getNameById(id);
			if (blockName == null)
			{
				continue;
			}
			
			final Player block = World.getInstance().getPlayer(id);
			blockListHtml.append("<a action=\"bypass _block;deselect;");
			blockListHtml.append(id).append("\">[Deselect]</a>&nbsp;");
			blockListHtml.append(blockName).append(" ");
			blockListHtml.append(((block != null) && block.isOnline()) ? "(on)" : "(off)");
			blockListHtml.append("<br1>");
		}
		
		html = html.replace("%selectedBlocksList%", blockListHtml.toString());
		html = html.replace("%deleteMSG%", delMsg ? BLOCKLIST_DELETE_BUTTON : "");
		CommunityBoardHandler.separateAndSend(html, player);
	}
	
	public static void showMailWrite(Player player)
	{
		String html = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/friends/friend-mail.html");
		if (html == null)
		{
			return;
		}
		
		final StringBuilder sb = new StringBuilder();
		for (int id : player.getSelectedFriendList())
		{
			final String friendName = CharInfoTable.getInstance().getNameById(id);
			if (friendName == null)
			{
				continue;
			}
			
			if (sb.length() > 0)
			{
				sb.append(";");
			}
			
			sb.append(friendName);
		}
		
		html = html.replace("%list%", sb.toString());
		CommunityBoardHandler.separateAndSend(html, player);
	}
	
	public boolean writeCommunityBoardCommand(Player player, String arg1, String arg2, String arg3, String arg4, String arg5)
	{
		if (arg1.equals("mail"))
		{
			MailBoard.getInstance().sendMail(arg3, arg4, arg5, player);
			showFriendsList(player, false);
		}
		
		return false;
	}
	
	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
}
