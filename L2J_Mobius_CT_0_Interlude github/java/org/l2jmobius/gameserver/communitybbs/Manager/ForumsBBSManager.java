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
package org.l2jmobius.gameserver.communitybbs.Manager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.communitybbs.BB.Forum;
import org.l2jmobius.gameserver.model.actor.Player;

public class ForumsBBSManager extends BaseBBSManager
{
	private static final Logger LOGGER = Logger.getLogger(ForumsBBSManager.class.getName());
	private final Collection<Forum> _table;
	private int _lastid = 1;
	
	/**
	 * Instantiates a new forums bbs manager.
	 */
	protected ForumsBBSManager()
	{
		_table = ConcurrentHashMap.newKeySet();
		try (Connection con = DatabaseFactory.getConnection();
			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("SELECT forum_id FROM forums WHERE forum_type = 0"))
		{
			while (rs.next())
			{
				addForum(new Forum(rs.getInt("forum_id"), null));
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Data error on Forum (root): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Inits the root.
	 */
	public void initRoot()
	{
		_table.forEach(Forum::vload);
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _table.size() + " forums. Last forum id used: " + _lastid);
	}
	
	/**
	 * Adds the forum.
	 * @param ff the forum
	 */
	public void addForum(Forum ff)
	{
		if (ff == null)
		{
			return;
		}
		
		_table.add(ff);
		
		if (ff.getID() > _lastid)
		{
			_lastid = ff.getID();
		}
	}
	
	@Override
	public void parsecmd(String command, Player player)
	{
	}
	
	/**
	 * Gets the forum by name.
	 * @param name the forum name
	 * @return the forum by name
	 */
	public Forum getForumByName(String name)
	{
		for (Forum forum : _table)
		{
			if (forum.getName().equals(name))
			{
				return forum;
			}
		}
		
		return null;
	}
	
	/**
	 * Creates the new forum.
	 * @param name the forum name
	 * @param parent the parent forum
	 * @param type the forum type
	 * @param perm the perm
	 * @param oid the oid
	 * @return the new forum
	 */
	public Forum createNewForum(String name, Forum parent, int type, int perm, int oid)
	{
		final Forum forum = new Forum(name, parent, type, perm, oid);
		forum.insertIntoDb();
		return forum;
	}
	
	/**
	 * Gets the a new Id.
	 * @return the a new Id
	 */
	public int getANewID()
	{
		return ++_lastid;
	}
	
	/**
	 * Gets the forum by Id.
	 * @param idf the the forum Id
	 * @return the forum by Id
	 */
	public Forum getForumByID(int idf)
	{
		for (Forum f : _table)
		{
			if (f.getID() == idf)
			{
				return f;
			}
		}
		
		return null;
	}
	
	@Override
	public void parsewrite(String ar1, String ar2, String ar3, String ar4, String ar5, Player player)
	{
	}
	
	/**
	 * Gets the single instance of ForumsBBSManager.
	 * @return single instance of ForumsBBSManager
	 */
	public static ForumsBBSManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ForumsBBSManager INSTANCE = new ForumsBBSManager();
	}
}
