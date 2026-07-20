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
package org.l2jmobius.gameserver.model.actor.holders.player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.model.actor.Player;

/**
 * TODO: System messages:<br>
 * ADD: 3223: The previous name is being registered. Please try again later.<br>
 * DEL 3219: $s1 was successfully deleted from your Contact List.<br>
 * DEL 3217: The name is not currently registered.
 * @author UnAfraid, mrTJO
 */
public class ContactList
{
	private static final Logger LOGGER = Logger.getLogger(ContactList.class.getName());
	
	private final Player _player;
	private final Set<String> _contacts = ConcurrentHashMap.newKeySet();
	
	private static final String QUERY_ADD = "REPLACE INTO character_contacts (charId, contactId) VALUES (?, ?)";
	private static final String QUERY_REMOVE = "DELETE FROM character_contacts WHERE charId = ? and contactId = ?";
	private static final String QUERY_LOAD = "SELECT contactId FROM character_contacts WHERE charId = ?";
	
	public ContactList(Player player)
	{
		_player = player;
		restore();
	}
	
	public void restore()
	{
		_contacts.clear();
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(QUERY_LOAD))
		{
			statement.setInt(1, _player.getObjectId());
			try (ResultSet rset = statement.executeQuery())
			{
				int contactId;
				String contactName;
				while (rset.next())
				{
					contactId = rset.getInt(1);
					contactName = CharInfoTable.getInstance().getNameById(contactId);
					if ((contactName == null) || contactName.equals(_player.getName()) || (contactId == _player.getObjectId()))
					{
						continue;
					}
					
					_contacts.add(contactName);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Error found in " + _player.getName() + "'s ContactsList: " + e.getMessage(), e);
		}
	}
	
	public boolean add(String name)
	{
		final int contactId = CharInfoTable.getInstance().getIdByName(name);
		if (_contacts.contains(name))
		{
			_player.sendMessage("The name already exists on the added list.");
			return false;
		}
		else if (_player.getName().equals(name))
		{
			_player.sendMessage("You cannot add your own name.");
			return false;
		}
		else if (_contacts.size() >= 100)
		{
			_player.sendMessage("The maximum number of names (100) has been reached. You cannot register any more.");
			return false;
		}
		else if (contactId < 1)
		{
			_player.sendMessage("The name " + name + " doesn't exist. Please try another name.");
			return false;
		}
		else
		{
			for (String contactName : _contacts)
			{
				if (contactName.equalsIgnoreCase(name))
				{
					_player.sendMessage("The name already exists on the added list.");
					return false;
				}
			}
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(QUERY_ADD))
		{
			statement.setInt(1, _player.getObjectId());
			statement.setInt(2, contactId);
			statement.execute();
			
			_contacts.add(name);
			
			_player.sendMessage(name + " was successfully added to your Contact List.");
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Error found in " + _player.getName() + "'s ContactsList: " + e.getMessage(), e);
		}
		
		return true;
	}
	
	public void remove(String name)
	{
		final int contactId = CharInfoTable.getInstance().getIdByName(name);
		if (!_contacts.contains(name))
		{
			_player.sendMessage("The name is not currently registered.");
			return;
		}
		else if (contactId < 1)
		{
			// TODO: Message?
			return;
		}
		
		_contacts.remove(name);
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(QUERY_REMOVE))
		{
			statement.setInt(1, _player.getObjectId());
			statement.setInt(2, contactId);
			statement.execute();
			
			_player.sendMessage(name + " was successfully deleted from your Contact List.");
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Error found in " + _player.getName() + "'s ContactsList: " + e.getMessage(), e);
		}
	}
	
	public Set<String> getAllContacts()
	{
		return _contacts;
	}
}
