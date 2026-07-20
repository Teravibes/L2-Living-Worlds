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
package org.l2jmobius.gameserver.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.clan.ClanMember;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.sevensigns.SevenSigns;
import org.l2jmobius.gameserver.model.siege.Castle;

public class CastleManager
{
	private static final Logger LOGGER = Logger.getLogger(CastleManager.class.getName());
	
	private static final List<Castle> _castles = new CopyOnWriteArrayList<>();
	
	private static final Map<Integer, Long> _castleSiegeDate = new ConcurrentHashMap<>();
	
	private static final int[] _castleCirclets =
	{
		0,
		6838,
		6835,
		6839,
		6837,
		6840,
		6834,
		6836,
		8182,
		8183
	};
	
	public int findNearestCastleIndex(WorldObject obj)
	{
		return findNearestCastleIndex(obj, Long.MAX_VALUE);
	}
	
	public int findNearestCastleIndex(WorldObject obj, long maxDistanceValue)
	{
		int index = getCastleIndex(obj);
		if (index < 0)
		{
			Castle castle;
			double distance;
			long maxDistance = maxDistanceValue;
			for (int i = 0; i < _castles.size(); i++)
			{
				castle = _castles.get(i);
				if (castle == null)
				{
					continue;
				}
				
				distance = castle.getDistance(obj);
				if (maxDistance > distance)
				{
					maxDistance = (long) distance;
					index = i;
				}
			}
		}
		
		return index;
	}
	
	public Castle getCastleById(int castleId)
	{
		for (Castle temp : _castles)
		{
			if (temp.getResidenceId() == castleId)
			{
				return temp;
			}
		}
		
		return null;
	}
	
	public Castle getCastleByOwner(Clan clan)
	{
		if (clan == null)
		{
			return null;
		}
		
		for (Castle temp : _castles)
		{
			if (temp.getOwnerId() == clan.getId())
			{
				return temp;
			}
		}
		
		return null;
	}
	
	public Castle getCastle(String name)
	{
		for (Castle temp : _castles)
		{
			if (temp.getName().equalsIgnoreCase(name.trim()))
			{
				return temp;
			}
		}
		
		return null;
	}
	
	public Castle getCastle(int x, int y, int z)
	{
		for (Castle temp : _castles)
		{
			if (temp.checkIfInZone(x, y, z))
			{
				return temp;
			}
		}
		
		return null;
	}
	
	public Castle getCastle(WorldObject activeObject)
	{
		return getCastle(activeObject.getX(), activeObject.getY(), activeObject.getZ());
	}
	
	public int getCastleIndex(int castleId)
	{
		Castle castle;
		for (int i = 0; i < _castles.size(); i++)
		{
			castle = _castles.get(i);
			if ((castle != null) && (castle.getResidenceId() == castleId))
			{
				return i;
			}
		}
		
		return -1;
	}
	
	public int getCastleIndex(WorldObject activeObject)
	{
		return getCastleIndex(activeObject.getX(), activeObject.getY(), activeObject.getZ());
	}
	
	public int getCastleIndex(int x, int y, int z)
	{
		Castle castle;
		for (int i = 0; i < _castles.size(); i++)
		{
			castle = _castles.get(i);
			if ((castle != null) && castle.checkIfInZone(x, y, z))
			{
				return i;
			}
		}
		
		return -1;
	}
	
	public List<Castle> getCastles()
	{
		return _castles;
	}
	
	public boolean hasOwnedCastle()
	{
		boolean hasOwnedCastle = false;
		for (Castle castle : _castles)
		{
			if (castle.getOwnerId() > 0)
			{
				hasOwnedCastle = true;
				break;
			}
		}
		
		return hasOwnedCastle;
	}
	
	public void validateTaxes(int sealStrifeOwner)
	{
		int maxTax;
		switch (sealStrifeOwner)
		{
			case SevenSigns.CABAL_DUSK:
			{
				maxTax = 5;
				break;
			}
			case SevenSigns.CABAL_DAWN:
			{
				maxTax = 25;
				break;
			}
			default: // no owner
			{
				maxTax = 15;
				break;
			}
		}
		
		for (Castle castle : _castles)
		{
			if (castle.getTaxPercent() > maxTax)
			{
				castle.setTaxPercent(maxTax);
			}
		}
	}
	
	public int getCirclet()
	{
		return getCircletByCastleId(1);
	}
	
	public int getCircletByCastleId(int castleId)
	{
		if ((castleId > 0) && (castleId < 10))
		{
			return _castleCirclets[castleId];
		}
		
		return 0;
	}
	
	// remove this castle's circlets from the clan
	public void removeCirclet(Clan clan, int castleId)
	{
		for (ClanMember member : clan.getMembers())
		{
			removeCirclet(member, castleId);
		}
	}
	
	public void removeCirclet(ClanMember member, int castleId)
	{
		if (member == null)
		{
			return;
		}
		
		final Player player = member.getPlayer();
		final int circletId = getCircletByCastleId(castleId);
		if (circletId != 0)
		{
			// online-player circlet removal
			if (player != null)
			{
				try
				{
					final Item circlet = player.getInventory().getItemByItemId(circletId);
					if (circlet != null)
					{
						if (circlet.isEquipped())
						{
							player.getInventory().unEquipItemInSlot(circlet.getLocationSlot());
						}
						
						player.destroyItemByItemId(ItemProcessType.DESTROY, circletId, 1, player, true);
					}
					return;
				}
				catch (NullPointerException e)
				{
					// continue removing offline
				}
			}
			// else offline-player circlet removal
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement("DELETE FROM items WHERE owner_id = ? and item_id = ?"))
			{
				ps.setInt(1, member.getObjectId());
				ps.setInt(2, circletId);
				ps.execute();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "Failed to remove castle circlets offline for player " + member.getName() + ": " + e.getMessage(), e);
			}
		}
	}
	
	public void loadInstances()
	{
		try (Connection con = DatabaseFactory.getConnection();
			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("SELECT id FROM castle ORDER BY id"))
		{
			while (rs.next())
			{
				_castles.add(new Castle(rs.getInt("id")));
			}
			
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _castles.size() + " castles");
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception: loadCastleData(): " + e.getMessage(), e);
		}
	}
	
	public void activateInstances()
	{
		for (Castle castle : _castles)
		{
			castle.activateInstance();
		}
	}
	
	public void registerSiegeDate(int castleId, long siegeDate)
	{
		_castleSiegeDate.put(castleId, siegeDate);
	}
	
	public int getSiegeDates(long siegeDate)
	{
		int count = 0;
		for (long date : _castleSiegeDate.values())
		{
			if (Math.abs(date - siegeDate) < 1000)
			{
				count++;
			}
		}
		
		return count;
	}
	
	public static CastleManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final CastleManager INSTANCE = new CastleManager();
	}
}
