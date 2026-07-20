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

import org.l2jmobius.gameserver.data.xml.MapRegionData;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.model.zone.type.TownZone;

public class TownManager
{
	public static int getTownCastle(int townId)
	{
		switch (townId)
		{
			case 912:
			{
				return 1;
			}
			case 916:
			{
				return 2;
			}
			case 918:
			{
				return 3;
			}
			case 922:
			{
				return 4;
			}
			case 924:
			{
				return 5;
			}
			case 926:
			{
				return 6;
			}
			case 1538:
			{
				return 7;
			}
			case 1537:
			{
				return 8;
			}
			case 1714:
			{
				return 9;
			}
			default:
			{
				return 0;
			}
		}
	}
	
	public static boolean townHasCastleInSiege(int townId)
	{
		final int castleIndex = getTownCastle(townId);
		if (castleIndex > 0)
		{
			final Castle castle = CastleManager.getInstance().getCastles().get(CastleManager.getInstance().getCastleIndex(castleIndex));
			if (castle != null)
			{
				return castle.getSiege().isInProgress();
			}
		}
		
		return false;
	}
	
	public static boolean townHasCastleInSiege(int x, int y)
	{
		return townHasCastleInSiege(MapRegionData.getInstance().getMapRegionLocId(x, y));
	}
	
	public static TownZone getTown(int townId)
	{
		for (TownZone temp : ZoneManager.getInstance().getAllZones(TownZone.class))
		{
			if (temp.getTownId() == townId)
			{
				return temp;
			}
		}
		
		return null;
	}
	
	/**
	 * Returns the town at that position (if any)
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public static TownZone getTown(int x, int y, int z)
	{
		for (ZoneType temp : ZoneManager.getInstance().getZones(x, y, z))
		{
			if (temp instanceof TownZone)
			{
				return (TownZone) temp;
			}
		}
		
		return null;
	}
}
