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
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.communitybbs.BB.Mail;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.handler.IWriteBoardHandler;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.player.BlockList;
import org.l2jmobius.gameserver.model.events.Containers;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.model.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.MailType;
import org.l2jmobius.gameserver.network.serverpackets.ExMailArrived;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.HtmlUtil;

/**
 * Mail board.
 * @author Skache
 */
public class MailBoard implements IWriteBoardHandler
{
	private static final Logger LOGGER = Logger.getLogger(MailBoard.class.getName());
	
	private static final String SELECT_MAILS = "SELECT * FROM bbs_mail ORDER BY id ASC";
	private static final String INSERT_MAIL = "INSERT INTO bbs_mail (id,receiver_id,sender_id,location,recipients,subject,message,sent_date,is_unread) VALUES (?,?,?,?,?,?,?,?,?)";
	private static final String DELETE_MAIL = "DELETE FROM bbs_mail WHERE id=?";
	private static final String UPDATE_MAIL_AS_READ = "UPDATE bbs_mail SET is_unread=0 WHERE id=?";
	private static final String UPDATE_MAIL_LOCATION = "UPDATE bbs_mail SET location=? WHERE id=?";
	
	private final Map<Integer, Set<Mail>> _mails = new ConcurrentHashMap<>();
	private int _lastMailId = 0;
	
	public MailBoard()
	{
		Containers.Global().addListener(new ConsumerEventListener(Containers.Global(), EventType.ON_PLAYER_LOGIN, (OnPlayerLogin event) -> onPlayerLogin(event), this));
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(SELECT_MAILS);
			ResultSet rs = ps.executeQuery())
		{
			while (rs.next())
			{
				int receiverId = rs.getInt("receiver_id");
				Mail mail = new Mail(rs);
				Set<Mail> mails = _mails.computeIfAbsent(receiverId, k -> new TreeSet<>(Comparator.comparing(Mail::getSentDate).reversed()));
				mails.add(mail);
				
				// Calculate last used mail ID
				int mailId = rs.getInt("id");
				if (mailId > _lastMailId)
				{
					_lastMailId = mailId;
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("Couldn't load mails.");
		}
	}
	
	private static final String[] COMMANDS =
	{
		"_bbsmail",
		"_maillist_0_1_0_",
	};
	
	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
	
	@Override
	public boolean onCommand(String command, Player player)
	{
		CommunityBoardHandler.getInstance().addBypass(player, "Mail Command", command);
		
		if (command.equals("_bbsmail") || command.equals("_maillist_0_1_0_"))
		{
			showMailList(player, 1, MailType.INBOX);
		}
		else if (command.startsWith("_bbsmail"))
		{
			final StringTokenizer st = new StringTokenizer(command, ";");
			st.nextToken();
			final String action = st.nextToken().toLowerCase();
			
			try
			{
				switch (action)
				{
					case "inbox":
					case "sentbox":
					case "archive":
					case "temparchive":
						final int page = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 1;
						final String sType = st.hasMoreTokens() ? st.nextToken() : "";
						final String search = st.hasMoreTokens() ? st.nextToken() : "";
						showMailList(player, page, MailType.valueOf(action.toUpperCase()), sType, search);
						break;
					case "write":
						showWriteView(player);
						break;
					case "view":
						final int viewLetterId = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : -1;
						final Mail viewMail = getMail(player, viewLetterId);
						if (viewMail != null)
						{
							showMailView(player, viewMail);
							if (viewMail.isUnread())
							{
								setMailAsRead(player, viewMail.getId());
							}
						}
						else
						{
							showLastForum(player);
						}
						break;
					case "reply":
						final int replyLetterId = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : -1;
						final Mail replyMail = getMail(player, replyLetterId);
						if (replyMail != null)
						{
							showWriteView(player, replyMail);
						}
						else
						{
							showLastForum(player);
						}
						break;
					case "del":
						final int delLetterId = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : -1;
						final Mail delMail = getMail(player, delLetterId);
						if (delMail != null)
						{
							deleteMail(player, delMail.getId());
						}
						
						showLastForum(player);
						break;
					case "store":
						final int storeLetterId = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : -1;
						final Mail storeMail = getMail(player, storeLetterId);
						if (storeMail != null)
						{
							setMailLocation(player, storeMail.getId(), MailType.ARCHIVE);
						}
						
						showMailList(player, 1, MailType.ARCHIVE);
						break;
					default:
						LOGGER.warning("Unknown action: " + action);
						break;
				}
			}
			catch (IllegalArgumentException e)
			{
				LOGGER.warning("Invalid MailType or command format: " + action);
			}
		}
		
		return false;
	}
	
	@Override
	public boolean writeCommunityBoardCommand(Player player, String arg1, String arg2, String arg3, String arg4, String arg5)
	{
		if (arg1.equals("Send"))
		{
			sendMail(arg3, arg4, arg5, player);
			showMailList(player, 1, MailType.SENTBOX);
		}
		else if (arg1.startsWith("Search"))
		{
			final StringTokenizer st = new StringTokenizer(arg1, ";");
			st.nextToken();
			
			showMailList(player, 1, Enum.valueOf(MailType.class, st.nextToken().toUpperCase()), arg4, arg5);
		}
		
		return false;
	}
	
	private synchronized int getNewMailId()
	{
		return ++_lastMailId;
	}
	
	private Set<Mail> getMails(int objectId)
	{
		return _mails.computeIfAbsent(objectId, m -> ConcurrentHashMap.newKeySet());
	}
	
	private Mail getMail(Player player, int mailId)
	{
		return getMails(player.getObjectId()).stream().filter(l -> l.getId() == mailId).findFirst().orElse(null);
	}
	
	public boolean checkIfUnreadMail(Player player)
	{
		return getMails(player.getObjectId()).stream().anyMatch(Mail::isUnread);
	}
	
	private void showMailList(Player player, int page, MailType type)
	{
		showMailList(player, page, type, "", "");
	}
	
	private void showMailList(Player player, int pageValue, MailType type, String sType, String search)
	{
		Set<Mail> mails;
		if (!sType.equals("") && !search.equals(""))
		{
			mails = ConcurrentHashMap.newKeySet();
			
			final boolean byTitle = sType.equalsIgnoreCase("title");
			
			for (Mail mail : getMails(player.getObjectId()))
			{
				if (byTitle && mail._subject.toLowerCase().contains(search.toLowerCase()))
				{
					mails.add(mail);
				}
				else if (!byTitle)
				{
					final String writer = getPlayerName(mail.getSenderId());
					if (writer.toLowerCase().contains(search.toLowerCase()))
					{
						mails.add(mail);
					}
				}
			}
		}
		else
		{
			mails = getMails(player.getObjectId());
		}
		
		final int countMails = getMailCount(player.getObjectId(), type, sType, search);
		final int maxpage = getPagesCount(countMails);
		int page = pageValue;
		if (page > maxpage)
		{
			page = maxpage;
		}
		
		if (page < 1)
		{
			page = 1;
		}
		
		player.setMailPosition(page);
		
		int index = 0;
		int minIndex = 0;
		int maxIndex = 0;
		maxIndex = (page == 1 ? page * 9 : (page * 10) - 1);
		minIndex = maxIndex - 9;
		
		String content = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/mail/mail.html");
		
		int inboxCount = getMailCount(player.getObjectId(), MailType.INBOX, "", "");
		int sentboxCount = getMailCount(player.getObjectId(), MailType.SENTBOX, "", "");
		int archiveCount = getMailCount(player.getObjectId(), MailType.ARCHIVE, "", "");
		int tempArchiveCount = getMailCount(player.getObjectId(), MailType.TEMPARCHIVE, "", "");
		
		String inbox = (type == MailType.INBOX) ? MailType.INBOX.getDescription() + "&nbsp;(" + inboxCount + ")" : MailType.INBOX.getBypass() + "&nbsp;(" + inboxCount + ")";
		String sentbox = (type == MailType.SENTBOX) ? MailType.SENTBOX.getDescription() + "&nbsp;(" + sentboxCount + ")" : MailType.SENTBOX.getBypass() + "&nbsp;(" + sentboxCount + ")";
		String archive = (type == MailType.ARCHIVE) ? MailType.ARCHIVE.getDescription() + "&nbsp;(" + archiveCount + ")" : MailType.ARCHIVE.getBypass() + "&nbsp;(" + archiveCount + ")";
		
		content = content.replace("%inbox%", inbox);
		content = content.replace("%sentbox%", sentbox);
		content = content.replace("%archive%", archive);
		content = content.replace("%temparchive%", Integer.toString(tempArchiveCount));
		content = content.replace("%type%", type.getDescription());
		content = content.replace("%htype%", type.toString().toLowerCase());
		
		final StringBuilder sb = new StringBuilder();
		for (Mail mail : mails)
		{
			if (mail.getMailType().equals(type))
			{
				if (index < minIndex)
				{
					index++;
					continue;
				}
				
				if (index > maxIndex)
				{
					break;
				}
				
				sb.append("<table width=610><tr><td width=5></td><td width=150>");
				sb.append(getPlayerName(mail.getSenderId()));
				sb.append("</td><td width=300><a action=\"bypass _bbsmail;view;");
				sb.append(mail.getId());
				sb.append("\">");
				
				if (mail.isUnread())
				{
					sb.append("<font color=\"LEVEL\">");
				}
				
				sb.append(abbreviate(mail.getSubject(), 30));
				if (mail.isUnread())
				{
					sb.append("</font>");
				}
				
				StringUtil.append(sb, "</a></td><td width=150>", mail.getFormattedSentDate(), "</td><td width=5></td></tr></table><img src=\"L2UI.Squaregray\" width=610 height=1>");
				index++;
			}
		}
		
		content = content.replace("%maillist%", sb.toString());
		
		// CLeanup sb.
		sb.setLength(0);
		
		final String fullSearch = (!sType.equals("") && !search.equals("")) ? ";" + sType + ";" + search : "";
		
		// Previous page button
		sb.append("<td><table><tr><td></td></tr><tr><td><button action=\"bypass _bbsmail;");
		sb.append(type);
		sb.append(";");
		sb.append((page == 1 ? page : page - 1));
		sb.append(fullSearch);
		sb.append("\" back=\"l2ui_ch3.prev1_down\" fore=\"l2ui_ch3.prev1\" width=16 height=16></td></tr></table></td>");
		int i = 0;
		if (maxpage > 21)
		{
			if (page <= 11)
			{
				for (i = 1; i <= (10 + page); i++)
				{
					if (i == page)
					{
						sb.append("<td> ");
						sb.append(i);
						sb.append(" </td>");
					}
					else
					{
						sb.append("<td><a action=\"bypass _bbsmail;");
						sb.append(type);
						sb.append(";");
						sb.append(i);
						sb.append(fullSearch);
						sb.append("\"> ");
						sb.append(i);
						sb.append(" </a></td>");
					}
				}
			}
			else if ((page > 11) && ((maxpage - page) > 10))
			{
				for (i = (page - 10); i <= (page - 1); i++)
				{
					if (i == page)
					{
						continue;
					}
					
					sb.append("<td><a action=\"bypass _bbsmail;");
					sb.append(type);
					sb.append(";");
					sb.append(i);
					sb.append(fullSearch);
					sb.append("\"> ");
					sb.append(i);
					sb.append(" </a></td>");
				}
				
				for (i = page; i <= (page + 10); i++)
				{
					if (i == page)
					{
						sb.append("<td> ");
						sb.append(i);
						sb.append(" </td>");
					}
					else
					{
						sb.append("<td><a action=\"bypass _bbsmail;");
						sb.append(type);
						sb.append(";");
						sb.append(i);
						sb.append(fullSearch);
						sb.append("\"> ");
						sb.append(i);
						sb.append(" </a></td>");
					}
				}
			}
			else if ((maxpage - page) <= 10)
			{
				for (i = (page - 10); i <= maxpage; i++)
				{
					if (i == page)
					{
						sb.append("<td> ");
						sb.append(i);
						sb.append(" </td>");
					}
					else
					{
						sb.append("<td><a action=\"bypass _bbsmail;");
						sb.append(type);
						sb.append(";");
						sb.append(i);
						sb.append(fullSearch);
						sb.append("\"> ");
						sb.append(i);
						sb.append(" </a></td>");
					}
				}
			}
		}
		else
		{
			for (i = 1; i <= maxpage; i++)
			{
				if (i == page)
				{
					sb.append("<td> ");
					sb.append(i);
					sb.append(" </td>");
				}
				else
				{
					sb.append("<td><a action=\"bypass _bbsmail;");
					sb.append(type).append(";");
					sb.append(i);
					sb.append(fullSearch);
					sb.append("\"> ");
					sb.append(i);
					sb.append(" </a></td>");
				}
			}
		}
		
		// Next page button
		sb.append("<td><table><tr><td></td></tr><tr><td><button action=\"bypass _bbsmail;");
		sb.append(type).append(";");
		sb.append((page == maxpage ? page : page + 1));
		sb.append(fullSearch);
		sb.append("\" back=\"l2ui_ch3.next1_down\" fore=\"l2ui_ch3.next1\" width=16 height=16></td></tr></table></td>");
		content = content.replace("%maillistlength%", sb.toString());
		HtmlUtil.sendCBHtml(player, content);
	}
	
	private static String abbreviate(String s, int maxWidth)
	{
		return s.length() > maxWidth ? s.substring(0, maxWidth) : s;
	}
	
	private void showMailView(Player player, Mail mail)
	{
		if (mail == null)
		{
			showMailList(player, 1, MailType.INBOX);
			return;
		}
		
		String content = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/mail/mail-show.html");
		
		String link = mail.getMailType().getBypass() + "&nbsp;&gt;&nbsp;" + mail.getSubject();
		content = content.replace("%maillink%", link);
		
		content = content.replace("%writer%", getPlayerName(mail.getSenderId()));
		content = content.replace("%sentDate%", mail.getFormattedSentDate());
		content = content.replace("%receiver%", mail.getRecipients());
		content = content.replace("%delDate%", "Unknown");
		content = content.replace("%title%", mail.getSubject().replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\"", "&quot;"));
		content = content.replace("%mes%", mail.getMessage().replaceAll("\r\n", "<br>").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\"", "&quot;"));
		content = content.replace("%mailId%", mail.getId() + "");
		
		HtmlUtil.sendCBHtml(player, content);
	}
	
	private static void showWriteView(Player player)
	{
		String content = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/mail/mail-write.html");
		HtmlUtil.sendCBHtml(player, content);
	}
	
	private static void showWriteView(Player player, Mail mail)
	{
		String content = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/mail/mail-reply.html");
		
		String link = mail.getMailType().getBypass() + "&nbsp;&gt;&nbsp;<a action=\"bypass _bbsmail;view;" + mail.getId() + "\">" + mail.getSubject() + "</a>&nbsp;&gt;&nbsp;";
		content = content.replace("%maillink%", link);
		
		content = content.replace("%recipients%", mail.getSenderId() == player.getObjectId() ? mail.getRecipients() : getPlayerName(mail.getSenderId()));
		content = content.replace("%mailId%", mail.getId() + "");
		HtmlUtil.sendCBHtml(player, content);
	}
	
	public void sendMail(String recipients, String subject, String message, Player player)
	{
		// Current time.
		final long currentDate = Calendar.getInstance().getTimeInMillis();
		
		// Get the current time - 1 day under timestamp format.
		final Timestamp ts = new Timestamp(currentDate - 86400000L);
		
		// Check sender mails based on previous timestamp. If more than 10 mails have been found for today, then cancel the use.
		if (getMails(player.getObjectId()).stream().filter(l -> l.getSentDate().after(ts) && (l.getMailType() == MailType.SENTBOX)).count() >= 10)
		{
			player.sendPacket(SystemMessageId.NO_MORE_MESSAGES_MAY_BE_SENT_AT_THIS_TIME_EACH_ACCOUNT_IS_ALLOWED_10_MESSAGES_PER_DAY);
			return;
		}
		
		// Format recipient names. If more than 5 are found, cancel the mail.
		final String[] recipientNames = recipients.trim().split(";");
		if ((recipientNames.length > 5) && !player.isGM())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_LIMITED_TO_FIVE_RECIPIENTS_AT_A_TIME);
			return;
		}
		
		// Edit subject.
		if ((subject == null) || subject.isEmpty())
		{
			subject = "(no subject)";
		}
		
		// Edit message.
		message = message.replaceAll("\n", "<br1>");
		
		// Get the current time under timestamp format.
		final Timestamp time = new Timestamp(currentDate);
		final String formattedTime = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(time);
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(INSERT_MAIL))
		{
			ps.setInt(3, player.getObjectId());
			ps.setString(4, "inbox");
			ps.setString(5, recipients);
			ps.setString(6, subject);
			ps.setString(7, message);
			ps.setTimestamp(8, time);
			ps.setInt(9, 1);
			
			for (String recipientName : recipientNames)
			{
				// Recipient is an invalid player, or is the sender.
				final int recipientId = CharInfoTable.getInstance().getIdByName(recipientName);
				if ((recipientId <= 0) || (recipientId == player.getObjectId()))
				{
					player.sendPacket(SystemMessageId.INVALID_TARGET);
					continue;
				}
				
				final Player recipientPlayer = World.getInstance().getPlayer(recipientId);
				
				if (!player.isGM())
				{
					// Sender is a regular player, while recipient is a GM.
					if (CharInfoTable.getInstance().getAccessLevelById(recipientId) > 0)
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_MESSAGE_TO_S1_DID_NOT_REACH_IT_S_RECIPIENT_YOU_CANNOT_SEND_MAIL_TO_THE_GM_STAFF);
						sm.addString(recipientName);
						player.sendPacket(sm);
						continue;
					}
					
					// Check if the recipient is in block mode or has blocked the sender.
					if (recipientPlayer != null)
					{
						// Check if the recipient is blocking all messages.
						if (recipientPlayer.getMessageRefusal())
						{
							final SystemMessage sm = new SystemMessage(SystemMessageId.S1_IS_BLOCKING_EVERYTHING);
							sm.addString(recipientName);
							player.sendPacket(sm);
							continue;
						}
						
						// Check if the recipient has blocked the sender.
						if (BlockList.isInBlockList(recipientPlayer, player))
						{
							final SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_BLOCKED_YOU_YOU_CANNOT_SEND_MAIL_TO_S1);
							sm.addString(recipientName);
							player.sendPacket(sm);
							continue;
						}
					}
					
					// The recipient box is already full.
					if (isInboxFull(recipientId))
					{
						player.sendPacket(SystemMessageId.THE_MESSAGE_WAS_NOT_SENT);
						if (recipientPlayer != null)
						{
							recipientPlayer.sendPacket(SystemMessageId.YOUR_MAILBOX_IS_FULL_THERE_IS_A_100_MESSAGE_LIMIT);
						}
						
						continue;
					}
				}
				
				final int id = getNewMailId();
				
				ps.setInt(1, id);
				ps.setInt(2, recipientId);
				ps.addBatch();
				
				getMails(recipientId).add(new Mail(id, recipientId, player.getObjectId(), MailType.INBOX, recipients, subject, message, time, formattedTime, true));
				
				if (recipientPlayer != null)
				{
					recipientPlayer.sendPacket(SystemMessageId.YOU_VE_GOT_MAIL);
					recipientPlayer.sendPacket(ExMailArrived.STATIC_PACKET);
				}
			}
			
			// Create a copy into player's sent box, if at least one recipient has been reached.
			final int[] result = ps.executeBatch();
			if (result.length > 0)
			{
				final int id = getNewMailId();
				
				ps.setInt(1, id);
				ps.setInt(2, player.getObjectId());
				ps.setInt(3, player.getObjectId());
				ps.setString(4, "sentbox");
				ps.setString(5, recipients);
				ps.setString(6, subject);
				ps.setString(7, message);
				ps.setTimestamp(8, time);
				ps.setInt(9, 0);
				ps.execute();
				
				getMails(player.getObjectId()).add(new Mail(id, player.getObjectId(), player.getObjectId(), MailType.SENTBOX, recipients, subject, message, time, formattedTime, false));
				
				player.sendPacket(SystemMessageId.YOU_VE_SENT_MAIL);
			}
		}
		catch (Exception e)
		{
			LOGGER.warning(MailBoard.class.getSimpleName() + ": Couldn't send mail for player " + player.getName() + ". Exception: " + e.getMessage());
		}
	}
	
	private int getMailCount(int objectId, MailType location, String type, String search)
	{
		int count = 0;
		if (!type.equals("") && !search.equals(""))
		{
			boolean byTitle = type.equalsIgnoreCase("title");
			for (Mail mail : getMails(objectId))
			{
				if (!mail.getMailType().equals(location))
				{
					continue;
				}
				
				if (byTitle && mail.getSubject().toLowerCase().contains(search.toLowerCase()))
				{
					count++;
				}
				else if (!byTitle)
				{
					String writer = getPlayerName(mail.getSenderId());
					if (writer.toLowerCase().contains(search.toLowerCase()))
					{
						count++;
					}
				}
			}
		}
		else
		{
			for (Mail mail : getMails(objectId))
			{
				if (mail.getMailType().equals(location))
				{
					count++;
				}
			}
		}
		
		return count;
	}
	
	private void deleteMail(Player player, int mailId)
	{
		// Cleanup memory.
		getMails(player.getObjectId()).removeIf(m -> m.getId() == mailId);
		
		// Cleanup database.
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(DELETE_MAIL))
		{
			ps.setInt(1, mailId);
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.warning(MailBoard.class.getSimpleName() + ": Couldn't delete mail #" + mailId + ". Exception: " + e.getMessage());
		}
	}
	
	private void setMailAsRead(Player player, int mailId)
	{
		final Mail mail = getMail(player, mailId);
		if (mail != null)
		{
			mail.setAsRead();
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(UPDATE_MAIL_AS_READ))
		{
			ps.setInt(1, mailId);
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.warning(MailBoard.class.getSimpleName() + ": Couldn't set read status for mail #" + mailId + ". Exception: " + e.getMessage());
		}
	}
	
	private void setMailLocation(Player player, int mailId, MailType location)
	{
		final Mail mail = getMail(player, mailId);
		if (mail != null)
		{
			mail.setMailType(location);
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(UPDATE_MAIL_LOCATION))
		{
			ps.setString(1, location.toString().toLowerCase());
			ps.setInt(2, mailId);
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.warning(MailBoard.class.getSimpleName() + ": Couldn't set mail #" + mailId + " location. Exception: " + e.getMessage());
		}
	}
	
	private boolean isInboxFull(int objectId)
	{
		return getMailCount(objectId, MailType.INBOX, "", "") >= 100;
	}
	
	private void showLastForum(Player player)
	{
		final int page = player.getMailPosition() % 1000;
		final int type = player.getMailPosition() / 1000;
		
		showMailList(player, page, MailType.VALUES[type]);
	}
	
	private static String getPlayerName(int objectId)
	{
		final String name = CharInfoTable.getInstance().getNameById(objectId);
		return (name == null) ? "Unknown" : name;
	}
	
	private static int getPagesCount(int mailCount)
	{
		if (mailCount < 1)
		{
			return 1;
		}
		
		if ((mailCount % 10) == 0)
		{
			return mailCount / 10;
		}
		
		return (mailCount / 10) + 1;
	}
	
	private void onPlayerLogin(OnPlayerLogin event)
	{
		if (GeneralConfig.ENABLE_COMMUNITY_BOARD)
		{
			final Player player = event.getPlayer();
			if (checkIfUnreadMail(player))
			{
				player.sendPacket(SystemMessageId.YOU_VE_GOT_MAIL);
				player.sendPacket(ExMailArrived.STATIC_PACKET);
			}
		}
	}
	
	public static MailBoard getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final MailBoard INSTANCE = new MailBoard();
	}
}
