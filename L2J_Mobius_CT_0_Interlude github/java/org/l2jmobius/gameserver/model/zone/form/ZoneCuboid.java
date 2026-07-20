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
package org.l2jmobius.gameserver.model.zone.form;

import java.awt.Rectangle;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.zone.ZoneForm;

/**
 * A primitive rectangular zone
 * @author durgus, Mobius
 */
public class ZoneCuboid extends ZoneForm
{
	private final int _z1;
	private final int _z2;
	private final Rectangle _r;
	private final Location _centerPoint;
	
	public ZoneCuboid(int x1, int x2, int y1, int y2, int z1, int z2)
	{
		final int _x1 = Math.min(x1, x2);
		final int _x2 = Math.max(x1, x2);
		final int _y1 = Math.min(y1, y2);
		final int _y2 = Math.max(y1, y2);
		
		_r = new Rectangle(_x1, _y1, _x2 - _x1, _y2 - _y1);
		
		_z1 = Math.min(z1, z2);
		_z2 = Math.max(z1, z2);
		
		_centerPoint = new Location((_x1 + _x2) / 2, (_y1 + _y2) / 2, (_z1 + _z2) / 2);
	}
	
	@Override
	public boolean isInsideZone(int x, int y, int z)
	{
		return _r.contains(x, y) && (z >= _z1) && (z <= _z2);
	}
	
	@Override
	public boolean intersectsRectangle(int ax1, int ax2, int ay1, int ay2)
	{
		return _r.intersects(Math.min(ax1, ax2), Math.min(ay1, ay2), Math.abs(ax2 - ax1), Math.abs(ay2 - ay1));
	}
	
	@Override
	public double getDistanceToZone(int x, int y)
	{
		final int _x1 = _r.x;
		final int _x2 = _r.x + _r.width;
		final int _y1 = _r.y;
		final int _y2 = _r.y + _r.height;
		double test = Math.pow(_x1 - x, 2) + Math.pow(_y2 - y, 2);
		double shortestDist = Math.pow(_x1 - x, 2) + Math.pow(_y1 - y, 2);
		
		if (test < shortestDist)
		{
			shortestDist = test;
		}
		
		test = Math.pow(_x2 - x, 2) + Math.pow(_y1 - y, 2);
		if (test < shortestDist)
		{
			shortestDist = test;
		}
		
		test = Math.pow(_x2 - x, 2) + Math.pow(_y2 - y, 2);
		if (test < shortestDist)
		{
			shortestDist = test;
		}
		
		return Math.sqrt(shortestDist);
	}
	
	/*
	 * getLowZ() / getHighZ() - These two functions were added to cope with the demand of the new fishing algorithms, which are now able to correctly place the hook in the water, thanks to getHighZ(). getLowZ() was added, considering potential future modifications.
	 */
	@Override
	public int getLowZ()
	{
		return _z1;
	}
	
	@Override
	public int getHighZ()
	{
		return _z2;
	}
	
	@Override
	public void visualizeZone(int z)
	{
		final int _x1 = _r.x;
		final int _x2 = _r.x + _r.width;
		final int _y1 = _r.y;
		final int _y2 = _r.y + _r.height;
		
		// x1->x2
		for (int x = _x1; x < _x2; x += STEP)
		{
			dropDebugItem(Inventory.ADENA_ID, 1, x, _y1, z);
			dropDebugItem(Inventory.ADENA_ID, 1, x, _y2, z);
		}
		
		// y1->y2
		for (int y = _y1; y < _y2; y += STEP)
		{
			dropDebugItem(Inventory.ADENA_ID, 1, _x1, y, z);
			dropDebugItem(Inventory.ADENA_ID, 1, _x2, y, z);
		}
	}
	
	@Override
	public Location getRandomPoint()
	{
		final int x = Rnd.get(_r.x, _r.x + _r.width);
		final int y = Rnd.get(_r.y, _r.y + _r.height);
		
		return new Location(x, y, GeoEngine.getInstance().getHeight(x, y, (_z1 + _z2) / 2));
	}
	
	@Override
	public Location getCenterPoint()
	{
		return _centerPoint;
	}
}
