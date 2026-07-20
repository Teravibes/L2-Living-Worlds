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
package org.l2jmobius.gameserver.model.script;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.sql.AnnouncementsTable;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.managers.EventDropManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.npc.EventDropHolder;
import org.l2jmobius.gameserver.model.announce.EventAnnouncement;
import org.l2jmobius.gameserver.model.events.Containers;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.OnServerStart;
import org.l2jmobius.gameserver.model.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.spawns.Spawn;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * Parent class for long time events.<br>
 * Maintains config reading, spawn of NPCs, adding of event's drop.
 * @author GKR, Mobius
 */
public class LongTimeEvent extends Script
{
	protected String _eventName;
	protected Date _startDate = null;
	protected Date _endDate = null;
	protected Date _dropStartDate = null;
	protected Date _dropEndDate = null;
	protected boolean _initialized = false;
	protected boolean _active = false;
	
	// Messages
	protected String _onEnterMsg = "";
	protected String _endMsg = "";
	protected int _enterAnnounceId = -1;
	
	// NPCs to spawn and their spawn points
	protected final List<NpcSpawn> _spawnList = new ArrayList<>();
	
	// Drop data for event
	protected final List<EventDropHolder> _dropList = new ArrayList<>();
	
	// Items to destroy when event ends
	protected final List<Integer> _destroyItemsOnEnd = new ArrayList<>();
	
	protected class NpcSpawn
	{
		protected final int npcId;
		protected final Location loc;
		protected final int respawnTime;
		
		protected NpcSpawn(int spawnNpcId, Location spawnLoc, int spawnRespawnTime)
		{
			npcId = spawnNpcId;
			loc = spawnLoc;
			respawnTime = spawnRespawnTime;
		}
	}
	
	public LongTimeEvent()
	{
		loadConfig();
		
		if ((_startDate != null) && (_endDate != null))
		{
			final Date now = new Date();
			if (isWithinRange(now))
			{
				startEvent();
				LOGGER.info("Event " + _eventName + " active till " + _endDate);
			}
			else if (_startDate.after(now))
			{
				final long delay = _startDate.getTime() - System.currentTimeMillis();
				ThreadPool.schedule(new ScheduleStart(), delay);
				LOGGER.info("Event " + _eventName + " will be started at " + _startDate);
			}
			else
			{
				// Destroy items that must exist only on event period.
				destroyItemsOnEnd();
				LOGGER.info("Event " + _eventName + " has passed... Ignored ");
			}
		}
		
		_initialized = true;
	}
	
	/**
	 * Checks if the given date is within the event period.
	 * @param date the date to check
	 * @return {@code true} if the date is within range
	 */
	private boolean isWithinRange(Date date)
	{
		return (date.equals(_startDate) || date.after(_startDate)) && (date.equals(_endDate) || date.before(_endDate));
	}
	
	private boolean isWithinDropPeriod(Date date)
	{
		return (date.equals(_dropStartDate) || date.after(_dropStartDate)) && (date.equals(_dropEndDate) || date.before(_dropEndDate));
	}
	
	/**
	 * Checks if the event period is valid (start before end).
	 * @return {@code true} if the period is valid
	 */
	private boolean isValidPeriod()
	{
		return (_startDate != null) && (_endDate != null) && _startDate.before(_endDate);
	}
	
	/**
	 * Parses a date range string into start and end dates.
	 * @param dateRange the date range string
	 * @param format the date format to use
	 * @return
	 */
	private Date[] parseDateRange(String dateRange, SimpleDateFormat format)
	{
		final String[] dates = dateRange.split("-");
		if (dates.length == 2)
		{
			try
			{
				final Date start = format.parse(dates[0].trim());
				final Date end = format.parse(dates[1].trim());
				return new Date[]
				{
					start,
					end
				};
			}
			catch (Exception e)
			{
				LOGGER.warning("Invalid Date Format: " + e.getMessage());
			}
		}
		
		return null;
	}
	
	/**
	 * Load event configuration file
	 */
	private void loadConfig()
	{
		new IXmlReader()
		{
			@Override
			public void load()
			{
				parseDatapackFile("data/scripts/events/" + getName() + "/config.xml");
			}
			
			@Override
			public void parseDocument(Document document, File file)
			{
				if (!document.getDocumentElement().getNodeName().equalsIgnoreCase("event"))
				{
					throw new NullPointerException("WARNING!!! " + getName() + " event: bad config file!");
				}
				
				_eventName = document.getDocumentElement().getAttributes().getNamedItem("name").getNodeValue();
				final String currentYear = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
				final String period = document.getDocumentElement().getAttributes().getNamedItem("active").getNodeValue();
				String dropPeriod = document.getDocumentElement().getAttributes().getNamedItem("dropPeriod") != null ? document.getDocumentElement().getAttributes().getNamedItem("dropPeriod").getNodeValue() : null;
				
				// If no drop period is defined, use the active period as the drop period
				if ((dropPeriod == null) || dropPeriod.isEmpty())
				{
					dropPeriod = period; // Use the same period as the active period
				}
				
				final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MM yyyy", Locale.US);
				
				// Handle active period
				if (period.length() == 21)
				{
					final Date[] range = parseDateRange(period, dateFormat);
					if (range != null)
					{
						_startDate = range[0];
						_endDate = range[1];
					}
				}
				else if (period.length() == 11)
				{
					final String[] parts = period.split("-");
					final String start = parts[0] + " " + currentYear;
					final String end = parts[1] + " " + currentYear;
					final Date[] range = parseDateRange(start + "-" + end, dateFormat);
					if (range != null)
					{
						_startDate = range[0];
						_endDate = range[1];
					}
				}
				
				// Handle drop period (may be different from event period)
				if (dropPeriod.length() == 21)
				{
					final Date[] dropRange = parseDateRange(dropPeriod, dateFormat);
					if (dropRange != null)
					{
						_dropStartDate = dropRange[0];
						_dropEndDate = dropRange[1];
					}
				}
				else if (dropPeriod.length() == 11)
				{
					final String[] parts = dropPeriod.split("-");
					final String start = parts[0] + " " + currentYear;
					final String end = parts[1] + " " + currentYear;
					final Date[] dropRange = parseDateRange(start + "-" + end, dateFormat);
					if (dropRange != null)
					{
						_dropStartDate = dropRange[0];
						_dropEndDate = dropRange[1];
					}
				}
				
				// Check that drop period is inside active period
				if (((_dropStartDate != null) && _dropStartDate.before(_startDate)) || ((_dropEndDate != null) && _dropEndDate.after(_endDate)))
				{
					throw new NullPointerException("WARNING!!! " + getName() + " event: drop period must be within the active period");
				}
				
				if (!isValidPeriod())
				{
					throw new NullPointerException("WARNING!!! " + getName() + " event: illegal event period");
				}
				
				final Date today = new Date();
				
				if (_startDate.after(today) || isWithinRange(today))
				{
					for (Node n = document.getDocumentElement().getFirstChild(); n != null; n = n.getNextSibling())
					{
						// Loading droplist
						if (n.getNodeName().equalsIgnoreCase("droplist"))
						{
							for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
							{
								if (d.getNodeName().equalsIgnoreCase("add"))
								{
									try
									{
										final int itemId = Integer.parseInt(d.getAttributes().getNamedItem("item").getNodeValue());
										final int minCount = Integer.parseInt(d.getAttributes().getNamedItem("min").getNodeValue());
										final int maxCount = Integer.parseInt(d.getAttributes().getNamedItem("max").getNodeValue());
										final String chance = d.getAttributes().getNamedItem("chance").getNodeValue();
										final double finalChance = !chance.isEmpty() && chance.endsWith("%") ? Double.parseDouble(chance.substring(0, chance.length() - 1)) : 0;
										final Node minLevelNode = d.getAttributes().getNamedItem("minLevel");
										final int minLevel = minLevelNode == null ? 1 : Integer.parseInt(minLevelNode.getNodeValue());
										final Node maxLevelNode = d.getAttributes().getNamedItem("maxLevel");
										final int maxLevel = maxLevelNode == null ? Integer.MAX_VALUE : Integer.parseInt(maxLevelNode.getNodeValue());
										final Node monsterIdsNode = d.getAttributes().getNamedItem("monsterIds");
										final Set<Integer> monsterIds = new HashSet<>();
										if (monsterIdsNode != null)
										{
											for (String id : monsterIdsNode.getNodeValue().split(","))
											{
												monsterIds.add(Integer.parseInt(id));
											}
										}
										
										if (ItemData.getInstance().getTemplate(itemId) == null)
										{
											LOGGER.warning(getName() + " event: " + itemId + " is wrong item id, item was not added in droplist");
											continue;
										}
										
										if (minCount > maxCount)
										{
											LOGGER.warning(getName() + " event: item " + itemId + " - min greater than max, item was not added in droplist");
											continue;
										}
										
										if ((finalChance < 0) || (finalChance > 100))
										{
											LOGGER.warning(getName() + " event: item " + itemId + " - incorrect drop chance, item was not added in droplist");
											continue;
										}
										
										_dropList.add(new EventDropHolder(itemId, minCount, maxCount, finalChance, minLevel, maxLevel, monsterIds));
									}
									catch (NumberFormatException nfe)
									{
										LOGGER.warning("Wrong number format in config.xml droplist block for " + getName() + " event");
									}
								}
							}
						}
						else if (n.getNodeName().equalsIgnoreCase("spawnlist"))
						{
							// Loading spawnlist
							for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
							{
								if (d.getNodeName().equalsIgnoreCase("add"))
								{
									try
									{
										final int npcId = Integer.parseInt(d.getAttributes().getNamedItem("npc").getNodeValue());
										final int xPos = Integer.parseInt(d.getAttributes().getNamedItem("x").getNodeValue());
										final int yPos = Integer.parseInt(d.getAttributes().getNamedItem("y").getNodeValue());
										final int zPos = Integer.parseInt(d.getAttributes().getNamedItem("z").getNodeValue());
										final Node headingNode = d.getAttributes().getNamedItem("heading");
										final String headingValue = headingNode == null ? null : headingNode.getNodeValue();
										final int heading = headingValue != null ? Integer.parseInt(headingValue) : 0;
										final Node respawnTimeNode = d.getAttributes().getNamedItem("respawnTime");
										final String respawnTimeValue = respawnTimeNode == null ? null : respawnTimeNode.getNodeValue();
										final int respawnTime = respawnTimeValue != null ? Integer.parseInt(respawnTimeValue) : 0;
										
										if (NpcData.getInstance().getTemplate(npcId) == null)
										{
											LOGGER.warning(getName() + " event: " + npcId + " is wrong NPC id, NPC was not added in spawnlist");
											continue;
										}
										
										_spawnList.add(new NpcSpawn(npcId, new Location(xPos, yPos, zPos, heading), respawnTime * 1000));
									}
									catch (NumberFormatException nfe)
									{
										LOGGER.warning("Wrong number format in config.xml spawnlist block for " + getName() + " event");
									}
								}
							}
						}
						else if (n.getNodeName().equalsIgnoreCase("messages"))
						{
							// Loading Messages
							for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
							{
								if (d.getNodeName().equalsIgnoreCase("add"))
								{
									final String msgType = d.getAttributes().getNamedItem("type").getNodeValue();
									final String msgText = d.getAttributes().getNamedItem("text").getNodeValue();
									if ((msgType != null) && (msgText != null))
									{
										if (msgType.equalsIgnoreCase("onEnd"))
										{
											_endMsg = msgText;
										}
										else if (msgType.equalsIgnoreCase("onEnter"))
										{
											_onEnterMsg = msgText;
										}
									}
								}
							}
						}
					}
				}
				
				// Load destroy item list at all times.
				for (Node n = document.getDocumentElement().getFirstChild(); n != null; n = n.getNextSibling())
				{
					if (n.getNodeName().equalsIgnoreCase("destroyItemsOnEnd"))
					{
						for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
						{
							if (d.getNodeName().equalsIgnoreCase("item"))
							{
								try
								{
									final int itemId = Integer.parseInt(d.getAttributes().getNamedItem("id").getNodeValue());
									if (ItemData.getInstance().getTemplate(itemId) == null)
									{
										LOGGER.warning(getName() + " event: Item " + itemId + " does not exist.");
										continue;
									}
									
									_destroyItemsOnEnd.add(itemId);
								}
								catch (NumberFormatException nfe)
								{
									LOGGER.warning("Wrong number format in config.xml destroyItemsOnEnd block for " + getName() + " event");
								}
							}
						}
					}
				}
			}
		}.load();
	}
	
	protected class ScheduleStart implements Runnable
	{
		@Override
		public void run()
		{
			startEvent();
		}
	}
	
	protected void startEvent()
	{
		// Set Active.
		_active = true;
		
		// Add event drops if within the drop period.
		if ((_dropStartDate != null) && (_dropEndDate != null) && isWithinDropPeriod(new Date()))
		{
			EventDropManager.getInstance().addDrops(this, _dropList);
		}
		
		if (!_spawnList.isEmpty())
		{
			if (_initialized)
			{
				// Add spawns on event start.
				spawnNpcs();
			}
			else // Add spawns on server start.
			{
				Containers.Global().addListener(new ConsumerEventListener(Containers.Global(), EventType.ON_SERVER_START, _spawnNpcs, this));
			}
		}
		
		// Event enter announcement.
		if (!_onEnterMsg.isEmpty())
		{
			// Send message on begin.
			Broadcast.toAllOnlinePlayers(_onEnterMsg);
			
			// Add announce for entering players.
			final EventAnnouncement announce = new EventAnnouncement(_startDate, _endDate, _onEnterMsg);
			AnnouncementsTable.getInstance().addAnnouncement(announce);
			_enterAnnounceId = announce.getId();
		}
		
		// Schedule event end.
		final Long millisToEventEnd = _endDate.getTime() - System.currentTimeMillis();
		ThreadPool.schedule(new ScheduleEnd(), millisToEventEnd);
	}
	
	/**
	 * Event spawns must initialize after server loads scripts.
	 */
	private final Consumer<OnServerStart> _spawnNpcs = _ ->
	{
		spawnNpcs();
		Containers.Global().removeListenerIf(EventType.ON_SERVER_START, listener -> listener.getOwner() == this);
	};
	
	protected void spawnNpcs()
	{
		final Long millisToEventEnd = _endDate.getTime() - System.currentTimeMillis();
		for (NpcSpawn npcSpawn : _spawnList)
		{
			final Npc npc = addSpawn(npcSpawn.npcId, npcSpawn.loc.getX(), npcSpawn.loc.getY(), npcSpawn.loc.getZ(), npcSpawn.loc.getHeading(), false, millisToEventEnd, false);
			if (npcSpawn.respawnTime > 0)
			{
				final Spawn spawn = npc.getSpawn();
				spawn.setRespawnDelay(npcSpawn.respawnTime);
				spawn.startRespawn();
				ThreadPool.schedule(spawn::stopRespawn, millisToEventEnd - npcSpawn.respawnTime);
			}
		}
	}
	
	protected class ScheduleEnd implements Runnable
	{
		@Override
		public void run()
		{
			stopEvent();
		}
	}
	
	protected void stopEvent()
	{
		// Set Active.
		_active = false;
		
		// Stop event drops.
		EventDropManager.getInstance().removeDrops(this);
		
		// Destroy items that must exist only on event period.
		destroyItemsOnEnd();
		
		// Send message on end.
		if (!_endMsg.isEmpty())
		{
			Broadcast.toAllOnlinePlayers(_endMsg);
		}
		
		// Remove announce for entering players.
		if (_enterAnnounceId != -1)
		{
			AnnouncementsTable.getInstance().deleteAnnouncement(_enterAnnounceId);
		}
	}
	
	protected void destroyItemsOnEnd()
	{
		if (!_destroyItemsOnEnd.isEmpty())
		{
			for (int itemId : _destroyItemsOnEnd)
			{
				// Remove item from online players.
				for (Player player : World.getInstance().getPlayers())
				{
					if (player != null)
					{
						player.destroyItemByItemId(ItemProcessType.DESTROY, itemId, -1, player, true);
					}
				}
				
				// Update database.
				try (Connection con = DatabaseFactory.getConnection();
					PreparedStatement statement = con.prepareStatement("DELETE FROM items WHERE item_id=?"))
				{
					statement.setInt(1, itemId);
					statement.execute();
				}
				catch (SQLException e)
				{
					LOGGER.warning(e.toString());
				}
			}
		}
	}
	
	/**
	 * @return the event start date
	 */
	public Date getStartDate()
	{
		return _startDate;
	}
	
	/**
	 * @return the event end date
	 */
	public Date getEndDate()
	{
		return _endDate;
	}
	
	/**
	 * @return {@code true} if now is event period
	 */
	public boolean isEventPeriod()
	{
		return _active;
	}
}
