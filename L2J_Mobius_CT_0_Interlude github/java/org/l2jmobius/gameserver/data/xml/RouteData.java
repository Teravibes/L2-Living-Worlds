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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

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
	public void saveRoute(String name, List<Location> points)
	{
		_routes.put(name, Collections.unmodifiableList(new ArrayList<>(points)));
		final File file = new File("data/routes/" + sanitize(name) + ".xml");
		try (PrintWriter pw = new PrintWriter(file, "UTF-8"))
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
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not save route '" + name + "': " + e.getMessage(), e);
		}
	}

	private static String sanitize(String name)
	{
		return name.replaceAll("[^a-zA-Z0-9_\\-]", "_");
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
