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
package org.l2jmobius.gameserver.model.announce;

import java.util.Date;

import org.l2jmobius.gameserver.managers.IdManager;

/**
 * @author UnAfraid, Mobius
 */
public class EventAnnouncement implements IAnnouncement
{
	private final int _id;
	private Date _startDate = null;
	private Date _endDate = null;
	private String _content;
	
	public EventAnnouncement(Date startDate, Date endDate, String content)
	{
		_id = IdManager.getInstance().getNextId();
		_startDate = startDate;
		_endDate = endDate;
		_content = content;
	}
	
	@Override
	public int getId()
	{
		return _id;
	}
	
	@Override
	public AnnouncementType getType()
	{
		return AnnouncementType.EVENT;
	}
	
	@Override
	public void setType(AnnouncementType type)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean isValid()
	{
		final Date now = new Date();
		return (_startDate != null) && (_endDate != null) && !now.before(_startDate) && !now.after(_endDate);
	}
	
	@Override
	public String getContent()
	{
		return _content;
	}
	
	@Override
	public void setContent(String content)
	{
		_content = content;
	}
	
	@Override
	public String getAuthor()
	{
		return "N/A";
	}
	
	@Override
	public void setAuthor(String author)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean deleteMe()
	{
		IdManager.getInstance().releaseId(_id);
		return true;
	}
	
	@Override
	public boolean storeMe()
	{
		return true;
	}
	
	@Override
	public boolean updateMe()
	{
		throw new UnsupportedOperationException();
	}
}
