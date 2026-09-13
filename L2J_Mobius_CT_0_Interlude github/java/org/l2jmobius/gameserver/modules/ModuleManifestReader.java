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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.l2jmobius.gameserver.modules.Json.JsonException;

/**
 * Reads and validates a module's {@code module.json} into a {@link ModuleManifest}.
 * <p>
 * Validation is intentionally strict and refuses a module with a clear reason rather than enabling a half-defined one.
 * It checks the required identity fields, verifies the id is a safe directory name, and confirms the manifest declares a
 * supported {@code apiVersion}.
 */
public class ModuleManifestReader
{
	private static final Logger LOGGER = Logger.getLogger(ModuleManifestReader.class.getName());

	/** The modding API version this platform provides. A manifest must declare a value the platform can satisfy. */
	public static final String SUPPORTED_API_VERSION = "1";

	private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");

	/**
	 * Reads and validates a manifest file.
	 * @param manifestFile the path to a {@code module.json}
	 * @return the validated manifest
	 * @throws ModuleException if the file cannot be read, is not well-formed, is missing a required field, has an unsafe
	 *             id, or declares an unsupported api version
	 */
	public ModuleManifest read(Path manifestFile) throws ModuleException
	{
		final String text;
		try
		{
			text = new String(Files.readAllBytes(manifestFile), StandardCharsets.UTF_8);
		}
		catch (IOException e)
		{
			throw new ModuleException("Could not read manifest " + manifestFile + ": " + e.getMessage(), e);
		}

		final Object parsed;
		try
		{
			parsed = Json.parse(text);
		}
		catch (JsonException e)
		{
			throw new ModuleException("Manifest " + manifestFile + " is not valid JSON: " + e.getMessage(), e);
		}

		if (!(parsed instanceof Map))
		{
			throw new ModuleException("Manifest " + manifestFile + " must be a JSON object.");
		}

		@SuppressWarnings("unchecked")
		final Map<String, Object> root = (Map<String, Object>) parsed;

		final String id = requireString(root, "id", manifestFile);
		if (!ID_PATTERN.matcher(id).matches())
		{
			throw new ModuleException("Manifest " + manifestFile + " has an invalid id '" + id + "'. Use lowercase letters, digits, and single hyphens, for example 'beast-taming'.");
		}

		final String apiVersion = requireString(root, "apiVersion", manifestFile);
		if (!SUPPORTED_API_VERSION.equals(apiVersion))
		{
			throw new ModuleException("Module '" + id + "' targets apiVersion '" + apiVersion + "', but this platform provides apiVersion '" + SUPPORTED_API_VERSION + "'.");
		}

		final String name = requireString(root, "name", manifestFile);
		final String version = requireString(root, "version", manifestFile);
		final String entrypoint = requireString(root, "entrypoint", manifestFile);
		final String description = optionalString(root, "description");
		final String author = optionalString(root, "author");
		final int priority = optionalInt(root, "priority", 100, manifestFile);
		final Map<ModuleResourceType, List<String>> resources = readResources(root, id, manifestFile);
		final Map<ModuleResourceType, List<ModuleIdRange>> reserves = readReserves(root, id, manifestFile);
		final List<String> dependencies = readIdList(root, "dependencies", id, manifestFile);
		final List<String> conflicts = readIdList(root, "conflicts", id, manifestFile);
		if (dependencies.contains(id))
		{
			throw new ModuleException("Module '" + id + "' lists itself as a dependency in " + manifestFile + ".");
		}
		if (conflicts.contains(id))
		{
			throw new ModuleException("Module '" + id + "' lists itself as a conflict in " + manifestFile + ".");
		}
		final ModuleDatabaseSpec database = readDatabase(root, manifestFile);

		return new ModuleManifest(id, name, version, apiVersion, entrypoint, description, author, priority, resources, reserves, dependencies, conflicts, database);
	}

	/**
	 * Parses and validates the optional {@code database} block: an object with a module-relative {@code install} script,
	 * an optional {@code remove} script, and the {@code tables} the module owns. Both script paths are checked to be safe
	 * relative paths inside the module directory, since the platform resolves and runs the install script.
	 */
	private ModuleDatabaseSpec readDatabase(Map<String, Object> root, Path manifestFile) throws ModuleException
	{
		final Object block = root.get("database");
		if (block == null)
		{
			return new ModuleDatabaseSpec("", "", java.util.Collections.emptyList());
		}
		if (!(block instanceof Map))
		{
			throw new ModuleException("Manifest " + manifestFile + " field 'database' must be a JSON object.");
		}

		@SuppressWarnings("unchecked")
		final Map<String, Object> db = (Map<String, Object>) block;
		final String install = optionalString(db, "install");
		final String remove = optionalString(db, "remove");
		if (!install.isEmpty())
		{
			validateResourcePath(install, "database.install", manifestFile);
		}
		if (!remove.isEmpty())
		{
			validateResourcePath(remove, "database.remove", manifestFile);
		}

		final List<String> tables = new ArrayList<>();
		final Object tablesValue = db.get("tables");
		if (tablesValue != null)
		{
			if (!(tablesValue instanceof List))
			{
				throw new ModuleException("Manifest " + manifestFile + " field 'database.tables' must be an array of table names.");
			}
			for (Object element : (List<?>) tablesValue)
			{
				if (!(element instanceof String) || ((String) element).trim().isEmpty())
				{
					throw new ModuleException("Manifest " + manifestFile + " field 'database.tables' must list non-empty table names.");
				}
				tables.add(((String) element).trim());
			}
		}

		return new ModuleDatabaseSpec(install, remove, tables);
	}

	/**
	 * Parses and validates the optional {@code reserves} block: a JSON object keyed by resource type whose values are
	 * arrays of two-element {@code [low, high]} id ranges. Only the id-bearing types items, skills, and npcs may reserve;
	 * html and multisell have no id space and are rejected. Each range must be two non-negative integers with
	 * {@code low <= high}.
	 */
	private Map<ModuleResourceType, List<ModuleIdRange>> readReserves(Map<String, Object> root, String id, Path manifestFile) throws ModuleException
	{
		final Map<ModuleResourceType, List<ModuleIdRange>> reserves = new EnumMap<>(ModuleResourceType.class);
		final Object block = root.get("reserves");
		if (block == null)
		{
			return reserves;
		}
		if (!(block instanceof Map))
		{
			throw new ModuleException("Manifest " + manifestFile + " field 'reserves' must be a JSON object.");
		}

		@SuppressWarnings("unchecked")
		final Map<String, Object> reserveMap = (Map<String, Object>) block;
		for (Map.Entry<String, Object> entry : reserveMap.entrySet())
		{
			final ModuleResourceType type = ModuleResourceType.fromKey(entry.getKey());
			if (type == null)
			{
				LOGGER.warning("Module '" + id + "' declares an unknown reserve type '" + entry.getKey() + "' in " + manifestFile + "; skipping it.");
				continue;
			}
			if ((type != ModuleResourceType.ITEMS) && (type != ModuleResourceType.SKILLS) && (type != ModuleResourceType.NPCS))
			{
				throw new ModuleException("Manifest " + manifestFile + " reserve type '" + entry.getKey() + "' has no id space; only items, skills, and npcs may be reserved.");
			}
			if (!(entry.getValue() instanceof List))
			{
				throw new ModuleException("Manifest " + manifestFile + " reserve type '" + entry.getKey() + "' must be an array of [low, high] ranges.");
			}

			final List<ModuleIdRange> ranges = new ArrayList<>();
			for (Object element : (List<?>) entry.getValue())
			{
				ranges.add(parseRange(element, entry.getKey(), manifestFile));
			}
			reserves.put(type, ranges);
		}
		return reserves;
	}

	private ModuleIdRange parseRange(Object element, String typeKey, Path manifestFile) throws ModuleException
	{
		if (!(element instanceof List) || (((List<?>) element).size() != 2))
		{
			throw new ModuleException("Manifest " + manifestFile + " reserve type '" + typeKey + "' must list two-element [low, high] ranges.");
		}
		final List<?> pair = (List<?>) element;
		final int low = asInt(pair.get(0), typeKey, manifestFile);
		final int high = asInt(pair.get(1), typeKey, manifestFile);
		if (low < 0)
		{
			throw new ModuleException("Manifest " + manifestFile + " reserve type '" + typeKey + "' has a negative id " + low + ".");
		}
		if (high < low)
		{
			throw new ModuleException("Manifest " + manifestFile + " reserve type '" + typeKey + "' range [" + low + ", " + high + "] has its high end below its low end.");
		}
		return new ModuleIdRange(low, high);
	}

	private int asInt(Object value, String typeKey, Path manifestFile) throws ModuleException
	{
		if (value instanceof Long)
		{
			return ((Long) value).intValue();
		}
		throw new ModuleException("Manifest " + manifestFile + " reserve type '" + typeKey + "' ranges must be whole numbers.");
	}

	/**
	 * Parses an optional array-of-strings field ({@code dependencies} or {@code conflicts}) into a list of module ids,
	 * validating each entry against the module id format so a typo is caught early rather than silently never matching.
	 */
	private List<String> readIdList(Map<String, Object> root, String field, String id, Path manifestFile) throws ModuleException
	{
		final List<String> ids = new ArrayList<>();
		final Object block = root.get(field);
		if (block == null)
		{
			return ids;
		}
		if (!(block instanceof List))
		{
			throw new ModuleException("Manifest " + manifestFile + " field '" + field + "' must be an array of module ids.");
		}

		for (Object element : (List<?>) block)
		{
			if (!(element instanceof String) || ((String) element).trim().isEmpty())
			{
				throw new ModuleException("Manifest " + manifestFile + " field '" + field + "' must list non-empty module ids.");
			}
			final String entry = ((String) element).trim();
			if (!ID_PATTERN.matcher(entry).matches())
			{
				throw new ModuleException("Manifest " + manifestFile + " field '" + field + "' has an invalid module id '" + entry + "'.");
			}
			ids.add(entry);
		}
		return ids;
	}

	/**
	 * Parses and validates the optional {@code resources} block: a JSON object whose keys are resource types and whose
	 * values are arrays of module-relative directory paths. Every path is checked to be a safe relative path inside the
	 * module directory, since these paths are later resolved and scanned. An unknown resource type is warned about and
	 * skipped rather than silently ignored, per the manifest schema policy.
	 */
	private Map<ModuleResourceType, List<String>> readResources(Map<String, Object> root, String id, Path manifestFile) throws ModuleException
	{
		final Map<ModuleResourceType, List<String>> resources = new EnumMap<>(ModuleResourceType.class);
		final Object block = root.get("resources");
		if (block == null)
		{
			return resources;
		}
		if (!(block instanceof Map))
		{
			throw new ModuleException("Manifest " + manifestFile + " field 'resources' must be a JSON object.");
		}

		@SuppressWarnings("unchecked")
		final Map<String, Object> resourceMap = (Map<String, Object>) block;
		for (Map.Entry<String, Object> entry : resourceMap.entrySet())
		{
			final ModuleResourceType type = ModuleResourceType.fromKey(entry.getKey());
			if (type == null)
			{
				LOGGER.warning("Module '" + id + "' declares an unknown resource type '" + entry.getKey() + "' in " + manifestFile + "; skipping it.");
				continue;
			}

			if (!(entry.getValue() instanceof List))
			{
				throw new ModuleException("Manifest " + manifestFile + " resource type '" + entry.getKey() + "' must be an array of paths.");
			}

			final List<String> paths = new ArrayList<>();
			for (Object element : (List<?>) entry.getValue())
			{
				if (!(element instanceof String) || ((String) element).trim().isEmpty())
				{
					throw new ModuleException("Manifest " + manifestFile + " resource type '" + entry.getKey() + "' must list non-empty string paths.");
				}
				final String path = ((String) element).trim();
				validateResourcePath(path, entry.getKey(), manifestFile);
				paths.add(path);
			}
			resources.put(type, paths);
		}
		return resources;
	}

	/**
	 * Rejects any resource path that is not a safe relative path inside the module directory. Absolute paths, Windows
	 * drive paths, and any path with a {@code ..} segment are refused, so a module can never point a loader outside its
	 * own directory.
	 */
	private void validateResourcePath(String path, String typeKey, Path manifestFile) throws ModuleException
	{
		final String normalized = path.replace('\\', '/');
		if (normalized.startsWith("/") || normalized.matches("^[A-Za-z]:.*"))
		{
			throw new ModuleException("Manifest " + manifestFile + " resource type '" + typeKey + "' path '" + path + "' must be relative to the module directory, not absolute.");
		}
		for (String segment : normalized.split("/"))
		{
			if (segment.equals(".."))
			{
				throw new ModuleException("Manifest " + manifestFile + " resource type '" + typeKey + "' path '" + path + "' must not traverse outside the module directory with '..'.");
			}
		}
	}

	private String requireString(Map<String, Object> root, String key, Path manifestFile) throws ModuleException
	{
		final Object value = root.get(key);
		if (!(value instanceof String) || ((String) value).trim().isEmpty())
		{
			throw new ModuleException("Manifest " + manifestFile + " is missing required string field '" + key + "'.");
		}
		return ((String) value).trim();
	}

	private String optionalString(Map<String, Object> root, String key)
	{
		final Object value = root.get(key);
		return (value instanceof String) ? ((String) value).trim() : "";
	}

	private int optionalInt(Map<String, Object> root, String key, int defaultValue, Path manifestFile) throws ModuleException
	{
		final Object value = root.get(key);
		if (value == null)
		{
			return defaultValue;
		}
		if (value instanceof Long)
		{
			return ((Long) value).intValue();
		}
		if (value instanceof Double)
		{
			return ((Double) value).intValue();
		}
		throw new ModuleException("Manifest " + manifestFile + " field '" + key + "' must be a number.");
	}
}
