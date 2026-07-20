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
package org.l2jmobius.gameserver.model;

import java.util.Objects;

import org.l2jmobius.gameserver.model.interfaces.ILocational;
import org.l2jmobius.gameserver.model.interfaces.IPositionable;

/**
 * Represents a 3D coordinate (x, y, z) with an optional heading and instance id.
 */
public class Location implements IPositionable
{
	protected volatile int _x;
	protected volatile int _y;
	protected volatile int _z;
	protected volatile int _heading;
	protected volatile int _instanceId;
	
	/**
	 * Constructs a Location at a specified x, y and z coordinate, with a default heading and instance id of 0.
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param z the z coordinate
	 */
	public Location(int x, int y, int z)
	{
		_x = x;
		_y = y;
		_z = z;
		_heading = 0;
		_instanceId = 0;
	}
	
	/**
	 * Constructs a Location at a specified x, y, z coordinate and heading, with a default instance id of 0.
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param z the z coordinate
	 * @param heading the heading (direction)
	 */
	public Location(int x, int y, int z, int heading)
	{
		_x = x;
		_y = y;
		_z = z;
		_heading = heading;
		_instanceId = 0;
	}
	
	/**
	 * Constructs a Location based on the position and heading of a WorldObject.
	 * @param obj the WorldObject to derive the location from.
	 */
	public Location(WorldObject obj)
	{
		this(obj.getX(), obj.getY(), obj.getZ(), obj.getHeading(), obj.getInstanceId());
	}
	
	/**
	 * Constructs a Location based on the position, heading and instance id of a WorldObject.
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param z the z coordinate
	 * @param heading the heading (direction)
	 * @param instanceId the instance id
	 */
	public Location(int x, int y, int z, int heading, int instanceId)
	{
		_x = x;
		_y = y;
		_z = z;
		_heading = heading;
		_instanceId = instanceId;
	}
	
	/**
	 * Constructs a Location from a {@link StatSet}, retrieving values for x, y, z, heading and instance id.
	 * @param set a StatSet containing x, y, z and heading properties.
	 */
	public Location(StatSet set)
	{
		_x = set.getInt("x", 0);
		_y = set.getInt("y", 0);
		_z = set.getInt("z", 0);
		_heading = set.getInt("heading", 0);
		_instanceId = set.getInt("instanceId", 0);
	}
	
	/**
	 * Retrieves the x coordinate.
	 * @return the x coordinate
	 */
	@Override
	public int getX()
	{
		return _x;
	}
	
	/**
	 * Retrieves the y coordinate.
	 * @return the y coordinate
	 */
	@Override
	public int getY()
	{
		return _y;
	}
	
	/**
	 * Retrieves the z coordinate.
	 * @return the z coordinate
	 */
	@Override
	public int getZ()
	{
		return _z;
	}
	
	/**
	 * Sets the x, y and z coordinates of this Location.
	 * @param x the new x coordinate
	 * @param y the new y coordinate
	 * @param z the new z coordinate
	 */
	@Override
	public void setXYZ(int x, int y, int z)
	{
		_x = x;
		_y = y;
		_z = z;
	}
	
	/**
	 * Updates the x, y and z coordinates of this Location based on another {@link ILocational}.
	 * @param loc the source ILocational to copy coordinates from.
	 */
	@Override
	public void setXYZ(ILocational loc)
	{
		setXYZ(loc.getX(), loc.getY(), loc.getZ());
	}
	
	/**
	 * Retrieves the heading (direction) of this Location.
	 * @return the heading of this location.
	 */
	@Override
	public int getHeading()
	{
		return _heading;
	}
	
	/**
	 * Sets the heading (direction) for this Location.
	 * @param heading the new the heading for this location.
	 */
	@Override
	public void setHeading(int heading)
	{
		_heading = Math.clamp(heading, 0, 65535);
	}
	
	/**
	 * Get the instance Id.
	 * @return the instance Id
	 */
	@Override
	public int getInstanceId()
	{
		return _instanceId;
	}
	
	/**
	 * Set the instance Id.
	 * @param instanceId the instance Id to set
	 */
	@Override
	public void setInstanceId(int instanceId)
	{
		_instanceId = instanceId;
	}
	
	/**
	 * Provides the current Location instance, as this class implements {@link IPositionable}.
	 * @return the current Location instance
	 */
	@Override
	public IPositionable getLocation()
	{
		return this;
	}
	
	/**
	 * Sets this Location's coordinates and heading to match another Location's.
	 * @param loc the source Location to copy from.
	 */
	@Override
	public void setLocation(Location loc)
	{
		_x = loc.getX();
		_y = loc.getY();
		_z = loc.getZ();
		_heading = loc.getHeading();
		_instanceId = loc.getInstanceId();
	}
	
	/**
	 * Creates a shallow copy of this Location.
	 * @return a new Location instance with the same x, y, z, heading and instance id values.
	 */
	@Override
	public Location clone()
	{
		return new Location(_x, _y, _z, _heading, _instanceId);
	}
	
	/**
	 * Computes a hash code based on this Location's coordinates and heading.
	 * @return the hash code for this Location.
	 */
	@Override
	public int hashCode()
	{
		return (31 * Objects.hash(_x, _y)) + Objects.hash(_z);
	}
	
	/**
	 * Checks equality between this Location and another object.<br>
	 * Two Locations are considered equal if they have the same x, y, z coordinates and heading.
	 * @param obj the object to compare
	 * @return {@code true} if the given object is a Location with the same x, y, z and heading values; otherwise, {@code false}
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof Location)
		{
			final Location loc = (Location) obj;
			return (getX() == loc.getX()) && (getY() == loc.getY()) && (getZ() == loc.getZ()) && (getHeading() == loc.getHeading()) && (getInstanceId() == loc.getInstanceId());
		}
		
		return false;
	}
	
	/**
	 * Provides a string representation of this Location.
	 * @return a formatted string containing the class name, x, y, z coordinates and heading.
	 */
	@Override
	public String toString()
	{
		return "[" + getClass().getSimpleName() + "] X: " + _x + " Y: " + _y + " Z: " + _z + " Heading: " + _heading + " InstanceId: " + _instanceId;
	}
}
