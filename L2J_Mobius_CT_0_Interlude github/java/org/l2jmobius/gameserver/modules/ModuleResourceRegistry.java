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
package org.l2jmobius.gameserver.modules;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The single place the stock data loaders look to find directories contributed by enabled modules. {@link ModuleManager}
 * fills it during discovery, before any data loader runs; a loader that has been taught about modules asks it for the
 * roots of the resource type it owns and scans them alongside the stock datapack folders.
 * <p>
 * The framework's core guarantee lives here too: with no module installed or enabled, the registry is empty, every
 * {@link #getRoots} returns an empty list, and a loader behaves exactly as stock.
 */
public class ModuleResourceRegistry
{
	private final Map<ModuleResourceType, List<File>> _roots = new EnumMap<>(ModuleResourceType.class);

	protected ModuleResourceRegistry()
	{
	}

	/**
	 * Records one directory an enabled module contributes for a resource type. Called only from module discovery.
	 * @param type the resource type the directory holds
	 * @param directory the absolute directory to scan
	 */
	public synchronized void register(ModuleResourceType type, File directory)
	{
		_roots.computeIfAbsent(type, k -> new ArrayList<>()).add(directory);
	}

	/**
	 * @param type a resource type
	 * @return the directories enabled modules contributed for that type, in registration order; never {@code null}
	 */
	public synchronized List<File> getRoots(ModuleResourceType type)
	{
		final List<File> roots = _roots.get(type);
		return (roots == null) ? Collections.emptyList() : new ArrayList<>(roots);
	}

	public static ModuleResourceRegistry getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final ModuleResourceRegistry INSTANCE = new ModuleResourceRegistry();
	}
}
