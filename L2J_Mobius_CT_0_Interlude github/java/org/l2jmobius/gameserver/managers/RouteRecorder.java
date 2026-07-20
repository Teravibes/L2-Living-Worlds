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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.data.xml.RouteData;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Player;

/**
 * Tracks in-progress GM route recording sessions. Called by ValidatePosition on every
 * position update; the admin command handler starts/stops sessions via this class.
 */
public class RouteRecorder
{
	private static final int MIN_POINT_DISTANCE = 150;

	private static class Session
	{
		final String name;
		final List<Location> points = new ArrayList<>();
		Location last;

		Session(String name, Location start)
		{
			this.name = name;
			this.last = start;
			points.add(start);
		}
	}

	private static final Map<Integer, Session> SESSIONS = new ConcurrentHashMap<>();

	/**
	 * Starts a recording session for the given player.
	 * @return error message, or {@code null} on success
	 */
	public static String startRecording(Player player, String routeName)
	{
		if (SESSIONS.containsKey(player.getObjectId()))
		{
			return "Already recording '" + SESSIONS.get(player.getObjectId()).name + "'. Use //stop_route first.";
		}
		final Location start = new Location(player.getX(), player.getY(), player.getZ());
		SESSIONS.put(player.getObjectId(), new Session(routeName, start));
		return null;
	}

	/**
	 * Stops recording, saves the route, and returns a status message.
	 */
	public static String stopRecording(Player player)
	{
		final Session session = SESSIONS.remove(player.getObjectId());
		if (session == null)
		{
			return "Not recording. Start with //record_route <name>.";
		}
		final Location cur = new Location(player.getX(), player.getY(), player.getZ());
		if (distSq(cur, session.last) > ((long) MIN_POINT_DISTANCE * MIN_POINT_DISTANCE))
		{
			session.points.add(cur);
		}
		if (session.points.size() < 2)
		{
			return "Route '" + session.name + "' has only " + session.points.size() + " point(s) — not saved. Walk further before stopping.";
		}
		RouteData.getInstance().saveRoute(session.name, session.points);
		return "Route '" + session.name + "' saved with " + session.points.size() + " waypoints.";
	}

	public static boolean isRecording(Player player)
	{
		return SESSIONS.containsKey(player.getObjectId());
	}

	public static String currentRouteName(Player player)
	{
		final Session s = SESSIONS.get(player.getObjectId());
		return s == null ? null : s.name;
	}

	/** Called from ValidatePosition on every position packet while a session is active. */
	public static void onPlayerMoved(Player player)
	{
		final Session session = SESSIONS.get(player.getObjectId());
		if (session == null)
		{
			return;
		}
		final Location cur = new Location(player.getX(), player.getY(), player.getZ());
		if (distSq(cur, session.last) >= ((long) MIN_POINT_DISTANCE * MIN_POINT_DISTANCE))
		{
			session.points.add(cur);
			session.last = cur;
			if ((session.points.size() % 10) == 0)
			{
				player.sendMessage("[Route recording] " + session.points.size() + " waypoints so far.");
			}
		}
	}

	private static long distSq(Location a, Location b)
	{
		final long dx = a.getX() - b.getX();
		final long dy = a.getY() - b.getY();
		return (dx * dx) + (dy * dy);
	}
}
