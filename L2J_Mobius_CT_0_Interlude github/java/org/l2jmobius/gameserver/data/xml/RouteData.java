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
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.w3c.dom.Document;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.StatSet;

/**
 * Loads named movement routes from {@code data/routes/*.xml}. Each file defines one route with an
 * ordered list of waypoints recorded in-game via {@code //record_route}. Populations in
 * {@code FakePlayerBehavior.xml} reference a route by name instead of embedding inline points.
 */
public class RouteData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(RouteData.class.getName());

	private final Map<String, List<Location>> _routes = new ConcurrentHashMap<>();

	protected RouteData()
	{
		load();
	}

	@Override
	public void load()
	{
		_routes.clear();
		final File folder = new File("data/routes");
		if (!folder.exists() || !folder.isDirectory())
		{
			return;
		}
		final File[] files = folder.listFiles((dir, name) -> name.endsWith(".xml"));
		if (files == null)
		{
			return;
		}
		for (File f : files)
		{
			parseFile(f);
		}
		if (!_routes.isEmpty())
		{
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _routes.size() + " routes.");
		}
	}

	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "route", routeNode ->
		{
			final StatSet set = new StatSet(parseAttributes(routeNode));
			final String name = set.getString("name", file.getName().replace(".xml", ""));
			final List<Location> points = new ArrayList<>();
			forEach(routeNode, "point", pointNode ->
			{
				final StatSet p = new StatSet(parseAttributes(pointNode));
				points.add(new Location(p.getInt("x"), p.getInt("y"), p.getInt("z")));
			});
			if (!points.isEmpty())
			{
				_routes.put(name, Collections.unmodifiableList(points));
			}
		});
	}

	/**
	 * Returns the waypoints for the named route, or {@code null} if not found.
	 */
	public List<Location> getRoute(String name)
	{
		return _routes.get(name);
	}

	public Collection<String> getRouteNames()
	{
		return Collections.unmodifiableCollection(_routes.keySet());
	}

	/**
	 * Saves a route to {@code data/routes/<name>.xml} and registers it in memory.
	 */
	// FPC-011: the one canonical route-name form. Restricting names to these characters means the name equals its own
	// filename (no two names collide onto one file, no metacharacter can corrupt the XML attribute), so the map key,
	// the on-disk file, and the reloadable attribute all agree.
	private static final Pattern VALID_ROUTE_NAME = Pattern.compile("[A-Za-z0-9_-]{1,64}");

	/** @return {@code true} when {@code name} is a valid canonical route name (letters, digits, underscore, hyphen). */
	public static boolean isValidRouteName(String name)
	{
		return (name != null) && VALID_ROUTE_NAME.matcher(name).matches();
	}

	/**
	 * Persist a route and register it in memory. FPC-011: this used to register the route before touching disk, never
	 * created {@code data/routes}, and swallowed write failures, so a GM got a "saved" message for a route that was
	 * gone after restart. Now it validates the name, creates the directory, writes to a temp file and atomically moves
	 * it into place, and registers the route in memory ONLY after the file is durably written - and it returns whether
	 * it actually succeeded so the caller can report the truth.
	 * @param name the route name (must be a valid canonical name)
	 * @param points the recorded waypoints
	 * @return {@code true} if the route was written to disk and registered; {@code false} on any failure
	 */
	public boolean saveRoute(String name, List<Location> points)
	{
		if (!isValidRouteName(name))
		{
			LOGGER.warning(getClass().getSimpleName() + ": Refusing to save route with invalid name '" + name + "' (allowed: letters, digits, '_', '-').");
			return false;
		}
		final File folder = new File("data/routes");
		if (!folder.isDirectory() && !folder.mkdirs())
		{
			LOGGER.warning(getClass().getSimpleName() + ": Could not create route directory '" + folder.getPath() + "'.");
			return false;
		}
		final File file = new File(folder, name + ".xml");
		final File tmp = new File(folder, name + ".xml.tmp");
		try (PrintWriter pw = new PrintWriter(tmp, "UTF-8"))
		{
			pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			pw.println("<route name=\"" + name + "\">");
			for (Location loc : points)
			{
				pw.println("\t<point x=\"" + loc.getX() + "\" y=\"" + loc.getY() + "\" z=\"" + loc.getZ() + "\"/>");
			}
			pw.println("</route>");
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not write route '" + name + "': " + e.getMessage(), e);
			tmp.delete();
			return false;
		}
		try
		{
			Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		}
		catch (Exception atomic)
		{
			// ATOMIC_MOVE is not supported on every filesystem; fall back to a plain replace.
			try
			{
				Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
			catch (Exception plain)
			{
				LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not move route '" + name + "' into place: " + plain.getMessage(), plain);
				tmp.delete();
				return false;
			}
		}
		// Register only now that the complete file is in place, so an in-memory route always has a durable backing file.
		_routes.put(name, Collections.unmodifiableList(new ArrayList<>(points)));
		return true;
	}

	public static RouteData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final RouteData INSTANCE = new RouteData();
	}
}
