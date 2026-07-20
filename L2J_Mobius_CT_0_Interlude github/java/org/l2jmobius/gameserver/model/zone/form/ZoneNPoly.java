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

import java.awt.Polygon;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.zone.ZoneForm;

/**
 * A not so primitive npoly zone
 * @author durgus, Mobius
 */
public class ZoneNPoly extends ZoneForm
{
	private final Polygon _p;
	private final int _z1;
	private final int _z2;
	private final Location _centerPoint;
	
	/**
	 * @param x
	 * @param y
	 * @param z1
	 * @param z2
	 */
	public ZoneNPoly(int[] x, int[] y, int z1, int z2)
	{
		_p = new Polygon(x, y, x.length);
		
		_z1 = Math.min(z1, z2);
		_z2 = Math.max(z1, z2);
		
		double area = 0;
		double cx = 0;
		double cy = 0;
		for (int i = 0; i < _p.npoints; i++)
		{
			final int nextIndex = (i + 1) % _p.npoints;
			final double crossProduct = (_p.xpoints[i] * _p.ypoints[nextIndex]) - (_p.xpoints[nextIndex] * _p.ypoints[i]);
			area += crossProduct;
			cx += (_p.xpoints[i] + _p.xpoints[nextIndex]) * crossProduct;
			cy += (_p.ypoints[i] + _p.ypoints[nextIndex]) * crossProduct;
		}
		
		area /= 2.0;
		if (area == 0)
		{
			cx = 0;
			cy = 0;
			for (int i = 0; i < _p.npoints; i++)
			{
				cx += _p.xpoints[i];
				cy += _p.ypoints[i];
			}
			
			cx /= _p.npoints;
			cy /= _p.npoints;
		}
		else
		{
			cx /= (6.0 * area);
			cy /= (6.0 * area);
		}
		
		_centerPoint = new Location((int) cx, (int) cy, (_z1 + _z2) / 2);
	}
	
	@Override
	public boolean isInsideZone(int x, int y, int z)
	{
		return _p.contains(x, y) && (z >= _z1) && (z <= _z2);
	}
	
	@Override
	public boolean intersectsRectangle(int ax1, int ax2, int ay1, int ay2)
	{
		return _p.intersects(Math.min(ax1, ax2), Math.min(ay1, ay2), Math.abs(ax2 - ax1), Math.abs(ay2 - ay1));
	}
	
	@Override
	public double getDistanceToZone(int x, int y)
	{
		final int[] xPoints = _p.xpoints;
		final int[] yPoints = _p.ypoints;
		double test;
		double shortestDist = Math.pow(xPoints[0] - x, 2) + Math.pow(yPoints[0] - y, 2);
		
		for (int i = 1; i < _p.npoints; i++)
		{
			test = Math.pow(xPoints[i] - x, 2) + Math.pow(yPoints[i] - y, 2);
			if (test < shortestDist)
			{
				shortestDist = test;
			}
		}
		
		return Math.sqrt(shortestDist);
	}
	
	// getLowZ() / getHighZ() - These two functions were added to cope with the demand of the new fishing algorithms, wich are now able to correctly place the hook in the water, thanks to getHighZ(). getLowZ() was added, considering potential future modifications.
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
		for (int i = 0; i < _p.npoints; i++)
		{
			final int nextIndex = (i + 1) == _p.xpoints.length ? 0 : i + 1;
			final int vx = _p.xpoints[nextIndex] - _p.xpoints[i];
			final int vy = _p.ypoints[nextIndex] - _p.ypoints[i];
			final float length = (float) Math.sqrt((vx * vx) + (vy * vy)) / STEP;
			for (int o = 1; o <= length; o++)
			{
				dropDebugItem(Inventory.ADENA_ID, 1, (int) (_p.xpoints[i] + ((o / length) * vx)), (int) (_p.ypoints[i] + ((o / length) * vy)), z);
			}
		}
	}
	
	@Override
	public Location getRandomPoint()
	{
		final int minX = _p.getBounds().x;
		final int maxX = _p.getBounds().x + _p.getBounds().width;
		final int minY = _p.getBounds().y;
		final int maxY = _p.getBounds().y + _p.getBounds().height;
		
		int x = Rnd.get(minX, maxX);
		int y = Rnd.get(minY, maxY);
		
		int antiBlocker = 0;
		while (!_p.contains(x, y) && (antiBlocker++ < 1000))
		{
			x = Rnd.get(minX, maxX);
			y = Rnd.get(minY, maxY);
		}
		
		return new Location(x, y, GeoEngine.getInstance().getHeight(x, y, (_z1 + _z2) / 2));
	}
	
	public int[] getX()
	{
		return _p.xpoints;
	}
	
	public int[] getY()
	{
		return _p.ypoints;
	}
	
	@Override
	public Location getCenterPoint()
	{
		return _centerPoint;
	}
}
