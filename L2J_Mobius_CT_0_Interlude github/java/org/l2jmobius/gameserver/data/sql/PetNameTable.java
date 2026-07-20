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
package org.l2jmobius.gameserver.data.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.data.xml.PetDataTable;

public class PetNameTable
{
	private static final Logger LOGGER = Logger.getLogger(PetNameTable.class.getName());
	
	// SQL
	private static final String CHECK_PET_NAME = "SELECT p.name FROM pets p JOIN items i ON p.item_obj_id = i.object_id WHERE p.name=? AND i.item_id=?";
	
	public boolean doesPetNameExist(String name, int petNpcId)
	{
		boolean result = false;
		final int petItemId = PetDataTable.getInstance().getPetItemsByNpc(petNpcId);
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(CHECK_PET_NAME))
		{
			ps.setString(1, name);
			ps.setInt(2, petItemId);
			
			try (ResultSet rs = ps.executeQuery())
			{
				result = rs.next();
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not check existing pet name: " + e.getMessage(), e);
		}
		
		return result;
	}
	
	public boolean isValidPetName(String name)
	{
		// Only allow alphanumeric names.
		if (!StringUtil.isAlphaNumeric(name))
		{
			return false;
		}
		
		try
		{
			Pattern pattern = Pattern.compile(ServerConfig.PET_NAME_TEMPLATE);
			return pattern.matcher(name).matches();
		}
		catch (PatternSyntaxException e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Invalid PetNameTemplate regex in config: " + ServerConfig.PET_NAME_TEMPLATE);
			return true; // If regex is broken, fallback to allowing all names.
		}
	}
	
	private static class SingletonHolder
	{
		protected static final PetNameTable INSTANCE = new PetNameTable();
	}
	
	public static PetNameTable getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
}
