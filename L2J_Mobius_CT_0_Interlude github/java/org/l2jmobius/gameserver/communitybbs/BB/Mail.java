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
package org.l2jmobius.gameserver.communitybbs.BB;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import org.l2jmobius.gameserver.network.enums.MailType;

public class Mail
{
	private final int _id;
	private final int _receiverId;
	private final int _senderId;
	
	private final String _recipients;
	public final String _subject;
	private final String _message;
	
	private final Timestamp _sentDate;
	
	private final String _formattedSentDate;
	
	private MailType _mailType;
	
	private boolean _isUnread;
	
	public Mail(ResultSet rs) throws SQLException
	{
		_id = rs.getInt("id");
		_receiverId = rs.getInt("receiver_id");
		_senderId = rs.getInt("sender_id");
		_mailType = Enum.valueOf(MailType.class, rs.getString("location").toUpperCase());
		_recipients = rs.getString("recipients");
		_subject = rs.getString("subject");
		_message = rs.getString("message");
		_sentDate = rs.getTimestamp("sent_date");
		_formattedSentDate = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(_sentDate);
		_isUnread = rs.getInt("is_unread") != 0;
	}
	
	public Mail(int id, int receiverId, int senderId, MailType location, String recipients, String subject, String message, Timestamp sentDate, String formattedSentDate, boolean isUnread)
	{
		_id = id;
		_receiverId = receiverId;
		_senderId = senderId;
		_mailType = location;
		_recipients = recipients;
		_subject = subject;
		_message = message;
		_sentDate = sentDate;
		_formattedSentDate = formattedSentDate;
		_isUnread = isUnread;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public int getReceiverId()
	{
		return _receiverId;
	}
	
	public int getSenderId()
	{
		return _senderId;
	}
	
	public MailType getMailType()
	{
		return _mailType;
	}
	
	public void setMailType(MailType mailType)
	{
		_mailType = mailType;
	}
	
	public String getRecipients()
	{
		return _recipients;
	}
	
	public String getSubject()
	{
		return _subject;
	}
	
	public String getMessage()
	{
		return _message;
	}
	
	public Timestamp getSentDate()
	{
		return _sentDate;
	}
	
	public String getFormattedSentDate()
	{
		return _formattedSentDate;
	}
	
	public boolean isUnread()
	{
		return _isUnread;
	}
	
	public void setAsRead()
	{
		_isUnread = false;
	}
}
