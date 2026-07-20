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
package org.l2jmobius.gameserver.util;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.interfaces.ILocational;

/**
 * Utility class providing a set of methods for spatial calculations between locations and objects.
 * @author Mobius
 */
public class LocationUtil
{
	/**
	 * Calculates the angle between two locations, with the first location as the origin.
	 * @param origin the origin location
	 * @param target the target location
	 * @return the angle in degrees from the origin to the target location, relative to the horizontal line
	 */
	public static double calculateAngleFrom(ILocational origin, ILocational target)
	{
		return calculateAngleFrom(origin.getX(), origin.getY(), target.getX(), target.getY());
	}
	
	/**
	 * Calculates the angle between two sets of coordinates, with the first coordinate pair as the origin.
	 * @param originX the x-coordinate of the origin
	 * @param originY the y-coordinate of the origin
	 * @param targetX the x-coordinate of the target location
	 * @param targetY the y-coordinate of the target location
	 * @return the angle in degrees from the origin to the target location, relative to the horizontal line
	 */
	public static double calculateAngleFrom(int originX, int originY, int targetX, int targetY)
	{
		double angle = Math.toDegrees(Math.atan2(targetY - originY, targetX - originX));
		if (angle < 0)
		{
			angle += 360;
		}
		
		return angle;
	}
	
	/**
	 * Calculates the heading angle in client format from one location to another.
	 * @param origin the origin location
	 * @param target the target location
	 * @return the heading in client format from the origin to the target location
	 */
	public static int calculateHeadingFrom(ILocational origin, ILocational target)
	{
		return calculateHeadingFrom(origin.getX(), origin.getY(), target.getX(), target.getY());
	}
	
	/**
	 * Calculates the heading angle in client format between two sets of coordinates.
	 * @param originX the x-coordinate of the origin
	 * @param originY the y-coordinate of the origin
	 * @param targetX the x-coordinate of the target location
	 * @param targetY the y-coordinate of the target location
	 * @return the heading in client format from the origin to the target location
	 */
	public static int calculateHeadingFrom(int originX, int originY, int targetX, int targetY)
	{
		double angle = Math.toDegrees(Math.atan2(targetY - originY, targetX - originX));
		if (angle < 0)
		{
			angle += 360;
		}
		
		return (int) (angle * 182.044444444);
	}
	
	/**
	 * Calculates the heading angle in client format based on a specified direction vector.
	 * @param deltaX the x-component of the direction vector
	 * @param deltaY the y-component of the direction vector
	 * @return the heading in client format based on the specified direction vector
	 */
	public static int calculateHeadingFrom(double deltaX, double deltaY)
	{
		double angle = Math.toDegrees(Math.atan2(deltaY, deltaX));
		if (angle < 0)
		{
			angle += 360;
		}
		
		return (int) (angle * 182.044444444);
	}
	
	/**
	 * Converts a heading value from client format to degrees.
	 * @param clientHeading the client-specific heading value
	 * @return the corresponding angle in degrees
	 */
	public static double convertHeadingToDegree(int clientHeading)
	{
		return clientHeading / 182.044444444;
	}
	
	/**
	 * Calculates the distance between two sets of coordinates, with options to include the Z-axis and return the squared distance.
	 * @param x1 the x-coordinate of the first point
	 * @param y1 the y-coordinate of the first point
	 * @param z1 the z-coordinate of the first point
	 * @param x2 the x-coordinate of the second point
	 * @param y2 the y-coordinate of the second point
	 * @param z2 the z-coordinate of the second point
	 * @param includeZ if {@code true}, includes the Z-axis in the distance calculation
	 * @param squared if {@code true}, returns the squared distance; otherwise, returns the actual distance
	 * @return the distance between the two points, squared if specified
	 */
	public static double calculateDistance(double x1, double y1, double z1, double x2, double y2, double z2, boolean includeZ, boolean squared)
	{
		final double distance = Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2) + (includeZ ? Math.pow(z1 - z2, 2) : 0);
		return squared ? distance : Math.sqrt(distance);
	}
	
	/**
	 * Calculates the distance between two locations, with options to include the Z-axis and return the squared distance.
	 * @param loc1 the first location
	 * @param loc2 the second location
	 * @param includeZ if {@code true}, includes the Z-axis in the distance calculation
	 * @param squared if {@code true}, returns the squared distance; otherwise, returns the actual distance
	 * @return the distance between the two locations, squared if specified
	 */
	public static double calculateDistance(ILocational loc1, ILocational loc2, boolean includeZ, boolean squared)
	{
		return calculateDistance(loc1.getX(), loc1.getY(), loc1.getZ(), loc2.getX(), loc2.getY(), loc2.getZ(), includeZ, squared);
	}
	
	/**
	 * Checks if two objects are within a specified range of each other, with options to include the Z-axis. Takes into account each object's collision radius if they are creatures.
	 * @param range the maximum allowable distance between the two objects
	 * @param obj1 the first object
	 * @param obj2 the second object
	 * @param includeZ if {@code true}, includes the Z-axis in the range check
	 * @return {@code true} if the two objects are within the specified range, {@code false} otherwise
	 */
	public static boolean checkIfInRange(int range, WorldObject obj1, WorldObject obj2, boolean includeZ)
	{
		if ((obj1 == null) || (obj2 == null) || (obj1.getInstanceId() != obj2.getInstanceId()))
		{
			return false;
		}
		
		int combinedRadius = 0;
		if (obj1.isCreature())
		{
			combinedRadius += obj1.asCreature().getTemplate().getCollisionRadius();
		}
		
		if (obj2.isCreature())
		{
			combinedRadius += obj2.asCreature().getTemplate().getCollisionRadius();
		}
		
		return calculateDistance(obj1, obj2, includeZ, false) <= (range + combinedRadius);
	}
	
	/**
	 * Checks if a target object, identified by its object ID, is within a specified radius of a given object.
	 * @param obj the reference object
	 * @param targetObjId the object ID of the target
	 * @param radius the maximum allowable distance between the objects
	 * @return {@code true} if the target object is within the specified radius of the reference object, {@code false} otherwise
	 */
	public static boolean isInsideRangeOfObjectId(WorldObject obj, int targetObjId, int radius)
	{
		final WorldObject target = World.getInstance().findObject(targetObjId);
		return (target != null) && (obj.calculateDistance3D(target) <= radius);
	}
	
	/**
	 * Creates a random location around the specified center location.
	 * @param center the center location
	 * @param minRadius the minimum allowable distance from the center to pick a point
	 * @param maxRadius the maximum allowable distance from the center to pick a point
	 * @return a random location between minRange and maxRange of the center location
	 */
	public static Location getRandomLocation(ILocational center, int minRadius, int maxRadius)
	{
		final int randomX = Rnd.get(minRadius, maxRadius);
		final int randomY = Rnd.get(minRadius, maxRadius);
		final double angle = Math.toRadians(Rnd.get(360));
		final int newX = (int) (center.getX() + (randomX * Math.cos(angle)));
		final int newY = (int) (center.getY() + (randomY * Math.sin(angle)));
		return new Location(newX, newY, center.getZ());
	}
}
