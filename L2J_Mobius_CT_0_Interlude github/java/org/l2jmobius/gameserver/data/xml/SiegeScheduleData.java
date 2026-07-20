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
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.siege.SiegeScheduleDate;

/**
 * @author UnAfraid
 */
public class SiegeScheduleData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(SiegeScheduleData.class.getName());
	
	private final Map<Integer, SiegeScheduleDate> _scheduleData = new HashMap<>();
	
	protected SiegeScheduleData()
	{
		load();
	}
	
	@Override
	public synchronized void load()
	{
		parseDatapackFile("config/SiegeSchedule.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _scheduleData.size() + " siege schedulers.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node cd = n.getFirstChild(); cd != null; cd = cd.getNextSibling())
				{
					switch (cd.getNodeName())
					{
						case "schedule":
						{
							final StatSet set = new StatSet();
							final NamedNodeMap attrs = cd.getAttributes();
							for (int i = 0; i < attrs.getLength(); i++)
							{
								final Node node = attrs.item(i);
								final String key = node.getNodeName();
								String val = node.getNodeValue();
								if ("day".equals(key) && !StringUtil.isNumeric(val))
								{
									val = Integer.toString(getValueForField(val));
								}
								
								set.set(key, val);
							}
							
							_scheduleData.put(set.getInt("castleId"), new SiegeScheduleDate(set));
							break;
						}
					}
				}
			}
		}
	}
	
	/**
	 * Retrieves the integer value associated with a specific field name in the {@code Calendar} class. This method uses reflection to access the value of the specified field from {@code Calendar}.
	 * @param field the name of the field in the {@code Calendar} class to retrieve
	 * @return the integer value of the specified {@code Calendar} field, or {@code -1} if the field cannot be accessed or does not exist
	 */
	private int getValueForField(String field)
	{
		try
		{
			return Calendar.class.getField(field).getInt(Calendar.class.getName());
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Could not get value for field " + field + ". " + e.getMessage());
			return -1;
		}
	}
	
	/**
	 * Retrieves the scheduled siege date associated with a specific castle ID.
	 * @param castleId the ID of the castle for which to retrieve the siege schedule date
	 * @return the {@code SiegeScheduleDate} for the specified castle ID, or {@code null} if no schedule data is available for this castle ID
	 */
	public SiegeScheduleDate getScheduleDateForCastleId(int castleId)
	{
		return _scheduleData.get(castleId);
	}
	
	public static SiegeScheduleData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final SiegeScheduleData INSTANCE = new SiegeScheduleData();
	}
}
