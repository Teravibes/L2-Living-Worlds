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

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A parsed and validated {@code module.json}.
 * <p>
 * It reads the flat identity fields used to discover, order, and enable a module; the {@code resources} block that lists
 * the module's own data directories per resource type; and the cross-module governance fields {@code reserves},
 * {@code dependencies}, and {@code conflicts} that drive startup validation. The database and hook declarations remain
 * part of the schema but are not consumed yet; they are added in later milestones as the loaders and hooks that use them
 * are built.
 */
public class ModuleManifest
{
	private final String _id;
	private final String _name;
	private final String _version;
	private final String _apiVersion;
	private final String _entrypoint;
	private final String _description;
	private final String _author;
	private final int _priority;
	private final Map<ModuleResourceType, List<String>> _resources;
	private final Map<ModuleResourceType, List<ModuleIdRange>> _reserves;
	private final List<String> _dependencies;
	private final List<String> _conflicts;
	private final ModuleDatabaseSpec _database;

	public ModuleManifest(String id, String name, String version, String apiVersion, String entrypoint, String description, String author, int priority, Map<ModuleResourceType, List<String>> resources, Map<ModuleResourceType, List<ModuleIdRange>> reserves, List<String> dependencies, List<String> conflicts, ModuleDatabaseSpec database)
	{
		_id = id;
		_name = name;
		_version = version;
		_apiVersion = apiVersion;
		_entrypoint = entrypoint;
		_description = description;
		_author = author;
		_priority = priority;
		_resources = resources;
		_reserves = reserves;
		_dependencies = dependencies;
		_conflicts = conflicts;
		_database = database;
	}

	public String getId()
	{
		return _id;
	}

	public String getName()
	{
		return _name;
	}

	public String getVersion()
	{
		return _version;
	}

	public String getApiVersion()
	{
		return _apiVersion;
	}

	public String getEntrypoint()
	{
		return _entrypoint;
	}

	public String getDescription()
	{
		return _description;
	}

	public String getAuthor()
	{
		return _author;
	}

	public int getPriority()
	{
		return _priority;
	}

	/**
	 * @param type a resource type
	 * @return the module-relative directories the manifest declared for that type, or an empty list if none
	 */
	public List<String> getResourcePaths(ModuleResourceType type)
	{
		final List<String> paths = _resources.get(type);
		return (paths == null) ? Collections.emptyList() : paths;
	}

	/**
	 * @param type a resource type
	 * @return the id ranges this module reserves for that type, or an empty list if none
	 */
	public List<ModuleIdRange> getReserves(ModuleResourceType type)
	{
		final List<ModuleIdRange> ranges = _reserves.get(type);
		return (ranges == null) ? Collections.emptyList() : ranges;
	}

	/**
	 * @return the reserved id ranges of every type, keyed by type; never {@code null}
	 */
	public Map<ModuleResourceType, List<ModuleIdRange>> getReserves()
	{
		return _reserves;
	}

	/**
	 * @return the ids of modules this one requires; never {@code null}
	 */
	public List<String> getDependencies()
	{
		return _dependencies;
	}

	/**
	 * @return the ids of modules this one refuses to run alongside; never {@code null}
	 */
	public List<String> getConflicts()
	{
		return _conflicts;
	}

	/**
	 * @return this module's database block; empty install and remove when the module declares none
	 */
	public ModuleDatabaseSpec getDatabase()
	{
		return _database;
	}
}
