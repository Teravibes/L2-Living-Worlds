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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.communitybbs.TopicConstructorType;
import org.l2jmobius.gameserver.communitybbs.Manager.ForumsBBSManager;
import org.l2jmobius.gameserver.communitybbs.Manager.TopicBBSManager;

public class Forum
{
	private static final Logger LOGGER = Logger.getLogger(Forum.class.getName());
	
	// type
	public static final int ROOT = 0;
	public static final int NORMAL = 1;
	public static final int CLAN = 2;
	public static final int MEMO = 3;
	public static final int MAIL = 4;
	
	// perm
	public static final int INVISIBLE = 0;
	public static final int ALL = 1;
	public static final int CLANMEMBERONLY = 2;
	public static final int OWNERONLY = 3;
	
	private final Collection<Forum> _children;
	private final Map<Integer, Topic> _topic = new ConcurrentHashMap<>();
	private final int _forumId;
	private String _forumName;
	private int _forumType;
	private int _forumPost;
	private int _forumPerm;
	private final Forum _fParent;
	private int _ownerID;
	private boolean _loaded = false;
	
	/**
	 * Creates new instance of Forum. When you create new forum, use {@link org.l2jmobius.gameserver.communitybbs.Manager.ForumsBBSManager#addForum(org.l2jmobius.gameserver.communitybbs.BB.Forum)} to add forum to the forums manager.
	 * @param forumId
	 * @param fParent
	 */
	public Forum(int forumId, Forum fParent)
	{
		_forumId = forumId;
		_fParent = fParent;
		_children = ConcurrentHashMap.newKeySet();
	}
	
	/**
	 * @param name
	 * @param parent
	 * @param type
	 * @param perm
	 * @param ownerId
	 */
	public Forum(String name, Forum parent, int type, int perm, int ownerId)
	{
		_forumName = name;
		_forumId = ForumsBBSManager.getInstance().getANewID();
		_forumType = type;
		_forumPost = 0;
		_forumPerm = perm;
		_fParent = parent;
		_ownerID = ownerId;
		_children = ConcurrentHashMap.newKeySet();
		parent._children.add(this);
		ForumsBBSManager.getInstance().addForum(this);
		_loaded = true;
	}
	
	private void load()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM forums WHERE forum_id=?"))
		{
			ps.setInt(1, _forumId);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					_forumName = rs.getString("forum_name");
					_forumPost = rs.getInt("forum_post");
					_forumType = rs.getInt("forum_type");
					_forumPerm = rs.getInt("forum_perm");
					_ownerID = rs.getInt("forum_owner_id");
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Data error on Forum " + _forumId + " : " + e.getMessage(), e);
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM topic WHERE topic_forum_id=? ORDER BY topic_id DESC"))
		{
			ps.setInt(1, _forumId);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					final Topic t = new Topic(TopicConstructorType.RESTORE, rs.getInt("topic_id"), rs.getInt("topic_forum_id"), rs.getString("topic_name"), rs.getLong("topic_date"), rs.getString("topic_ownername"), rs.getInt("topic_ownerid"), rs.getInt("topic_type"), rs.getInt("topic_reply"));
					_topic.put(t.getID(), t);
					if (t.getID() > TopicBBSManager.getInstance().getMaxID(this))
					{
						TopicBBSManager.getInstance().setMaxID(t.getID(), this);
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Data error on Forum " + _forumId + " : " + e.getMessage(), e);
		}
	}
	
	private void getChildren()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT forum_id FROM forums WHERE forum_parent=?"))
		{
			ps.setInt(1, _forumId);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					final Forum f = new Forum(rs.getInt("forum_id"), this);
					_children.add(f);
					ForumsBBSManager.getInstance().addForum(f);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Data error on Forum (children): " + e.getMessage(), e);
		}
	}
	
	public int getTopicSize()
	{
		vload();
		return _topic.size();
	}
	
	public Topic getTopic(int j)
	{
		vload();
		return _topic.get(j);
	}
	
	public void addTopic(Topic t)
	{
		vload();
		_topic.put(t.getID(), t);
	}
	
	/**
	 * @return the forum Id
	 */
	public int getID()
	{
		return _forumId;
	}
	
	public String getName()
	{
		vload();
		return _forumName;
	}
	
	public int getType()
	{
		vload();
		return _forumType;
	}
	
	/**
	 * @param name the forum name
	 * @return the forum for the given name
	 */
	public Forum getChildByName(String name)
	{
		vload();
		for (Forum forum : _children)
		{
			if (forum.getName().equals(name))
			{
				return forum;
			}
		}
		
		return null;
	}
	
	/**
	 * @param id
	 */
	public void rmTopicByID(int id)
	{
		_topic.remove(id);
	}
	
	public void insertIntoDb()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("INSERT INTO forums (forum_id,forum_name,forum_parent,forum_post,forum_type,forum_perm,forum_owner_id) VALUES (?,?,?,?,?,?,?)"))
		{
			ps.setInt(1, _forumId);
			ps.setString(2, _forumName);
			ps.setInt(3, _fParent.getID());
			ps.setInt(4, _forumPost);
			ps.setInt(5, _forumType);
			ps.setInt(6, _forumPerm);
			ps.setInt(7, _ownerID);
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Error while saving new Forum to db " + e.getMessage(), e);
		}
	}
	
	public void vload()
	{
		if (!_loaded)
		{
			load();
			getChildren();
			_loaded = true;
		}
	}
}
