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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.l2jmobius.commons.util.ConfigReader;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.scripting.ScriptEngine;
import org.l2jmobius.gameserver.scripting.engine.ScriptClassLoader;

/**
 * The single authority for installed modules: it scans the modules root, reads and validates each manifest, loads each
 * module's own configuration, and then compiles and enables the modules whose switch is on.
 * <p>
 * The work is split into two phases so it can slot into the server boot sequence. {@link #discover()} runs after the
 * configuration is loaded and validates every candidate. {@link #enableModules()} runs after the stock scripts have
 * executed, so module code can rely on the normal handler singletons already existing, and it is what actually compiles
 * scripts and invokes each entry point.
 * <p>
 * The framework's core guarantee holds here: a module whose switch is off, or whose directory is absent, contributes
 * nothing, and a candidate that fails validation is refused with a clear reason rather than half-enabled.
 */
public class ModuleManager
{
	private static final Logger LOGGER = Logger.getLogger(ModuleManager.class.getName());
	private static final String LOG_PREFIX = "[Modules] ";

	private final ModuleManifestReader _manifestReader = new ModuleManifestReader();
	private final List<DiscoveredModule> _modules = new ArrayList<>();
	private final Map<String, ModuleHandles> _handlesByModule = new LinkedHashMap<>();
	private final Set<String> _refusedIds = new HashSet<>();

	private int _refused;

	protected ModuleManager()
	{
		discover();
	}

	/**
	 * Scans the modules root and validates every candidate module, without running any module code. Validation failures
	 * refuse the offending module and continue with the others.
	 */
	private void discover()
	{
		ModulesConfig.load();
		if (!ModulesConfig.isEnabled())
		{
			LOGGER.info(LOG_PREFIX + "Module framework disabled by configuration.");
			return;
		}

		final File root = ModulesConfig.getModulesRoot();
		LOGGER.info(LOG_PREFIX + "Scanning " + root.getPath());
		if (!root.isDirectory())
		{
			LOGGER.info(LOG_PREFIX + "No modules root present, nothing to load.");
			return;
		}

		final File[] entries = root.listFiles(File::isDirectory);
		if (entries == null)
		{
			LOGGER.warning(LOG_PREFIX + "Could not read modules root " + root.getPath());
			return;
		}

		for (File dir : entries)
		{
			final Path manifestFile = dir.toPath().resolve("module.json");
			if (!Files.isRegularFile(manifestFile))
			{
				LOGGER.warning(LOG_PREFIX + "Skipping '" + dir.getName() + "': no module.json.");
				continue;
			}

			try
			{
				final ModuleManifest manifest = _manifestReader.read(manifestFile);
				if (!manifest.getId().equals(dir.getName()))
				{
					throw new ModuleException("Module id '" + manifest.getId() + "' does not match its directory name '" + dir.getName() + "'.");
				}

				final ConfigReader configReader = new ConfigReader(dir.toPath().resolve("config").resolve("module.ini").toString());
				final boolean enabled = configReader.getBoolean("Enabled", false);
				_modules.add(new DiscoveredModule(manifest, dir.toPath(), new ModuleConfig(configReader), enabled));

				LOGGER.info(LOG_PREFIX + "Found " + manifest.getName() + " " + manifest.getVersion() + " (api " + manifest.getApiVersion() + "), enabled=" + enabled);
			}
			catch (ModuleException e)
			{
				_refused++;
				LOGGER.warning(LOG_PREFIX + "Refused '" + dir.getName() + "': " + e.getMessage());
			}
		}

		// Deterministic order: higher priority first, then ascending id.
		_modules.sort(Comparator.comparingInt((DiscoveredModule m) -> m.manifest.getPriority()).reversed().thenComparing(m -> m.manifest.getId()));

		// Cross-module validation (reserved id overlaps, conflicts, dependencies, cycles) runs before any module
		// resource is registered, so a colliding or unsatisfiable module never contributes data or code. The separate
		// check against ids the base game already owns needs the stock datapack indexed first and is not done here yet.
		validateAcrossModules();

		// Only an enabled module that survived validation contributes its data to the loaders. This runs during
		// discovery, before the data loaders, so a module's own resource roots are in place when they scan.
		for (DiscoveredModule module : _modules)
		{
			if (module.enabled && !_refusedIds.contains(module.manifest.getId()))
			{
				registerResources(module.manifest, module.directory);
			}
		}
	}

	/**
	 * Runs the pure {@link ModuleValidator} over the discovered modules and records every refusal it reports, so a
	 * refused module is skipped by both resource registration and {@link #enableModules()}.
	 */
	private void validateAcrossModules()
	{
		final List<ModuleValidator.Candidate> candidates = new ArrayList<>();
		for (DiscoveredModule module : _modules)
		{
			final ModuleManifest manifest = module.manifest;
			candidates.add(new ModuleValidator.Candidate(manifest.getId(), module.enabled, manifest.getDependencies(), manifest.getConflicts(), manifest.getReserves()));
		}

		for (Map.Entry<String, String> refusal : ModuleValidator.validate(candidates).entrySet())
		{
			if (_refusedIds.add(refusal.getKey()))
			{
				_refused++;
				LOGGER.warning(LOG_PREFIX + "Refused '" + refusal.getKey() + "': " + refusal.getValue() + ".");
			}
		}
	}

	/**
	 * Resolves an enabled module's declared resource directories against its own directory and records each existing one
	 * in the {@link ModuleResourceRegistry}, so the loaders that read the registry scan the module's data alongside the
	 * stock datapack. Paths were already validated as safe and relative by the manifest reader; a declared directory that
	 * does not exist on disk is warned about and skipped rather than failing the module.
	 */
	private void registerResources(ModuleManifest manifest, Path moduleDirectory)
	{
		for (ModuleResourceType type : ModuleResourceType.values())
		{
			for (String relativePath : manifest.getResourcePaths(type))
			{
				final File directory = moduleDirectory.resolve(relativePath).toFile();
				if (!directory.isDirectory())
				{
					LOGGER.warning(LOG_PREFIX + manifest.getName() + ": declared " + type.getKey() + " resource '" + relativePath + "' is not a directory, skipping it.");
					continue;
				}

				ModuleResourceRegistry.getInstance().register(type, directory);
				LOGGER.info(LOG_PREFIX + manifest.getName() + ": registered " + type.getKey() + " resource root " + relativePath);
			}
		}
	}

	/**
	 * Compiles and enables every discovered module whose switch is on, in the deterministic order set at discovery. Each
	 * entry point is instantiated and handed a {@link ModuleContext}. A failure enabling one module refuses it and
	 * continues with the others.
	 */
	public void enableModules()
	{
		if (!ModulesConfig.isEnabled())
		{
			return;
		}

		// Base-game id-range check. Runs here, after the data loaders have populated, refusing a module whose reserved
		// range hits an id the stock or custom datapack already owns before that module's code runs.
		validateReservesAgainstBaseGame();

		int enabledCount = 0;
		for (DiscoveredModule module : _modules)
		{
			if (!module.enabled || _refusedIds.contains(module.manifest.getId()))
			{
				continue;
			}

			try
			{
				enable(module);
				enabledCount++;
			}
			catch (Exception e)
			{
				_refused++;
				LOGGER.log(Level.WARNING, LOG_PREFIX + "Refused '" + module.manifest.getId() + "': " + e.getMessage(), e);
			}
		}

		LOGGER.info(LOG_PREFIX + enabledCount + " module(s) enabled, " + _refused + " refused.");
	}

	/**
	 * Checks every surviving enabled module's reserved id ranges against the ids the base game already owns, for each
	 * id-bearing resource type (items, skills, npcs), and refuses a module that claims a range overlapping a stock or
	 * custom id. Because this runs after the loaders have merged module data, a colliding module's data has already
	 * loaded for this boot: refusing it stops its code from running and logs a clear reason so the operator fixes the
	 * range, and the next start is clean.
	 */
	private void validateReservesAgainstBaseGame()
	{
		checkReservesAgainstBaseGame(ModuleResourceType.ITEMS, ItemData.getInstance().getBaseGameItemIds());
		checkReservesAgainstBaseGame(ModuleResourceType.SKILLS, SkillData.getInstance().getBaseGameSkillIds());
		checkReservesAgainstBaseGame(ModuleResourceType.NPCS, NpcData.getInstance().getBaseGameNpcIds());
	}

	private void checkReservesAgainstBaseGame(ModuleResourceType type, Set<Integer> baseIds)
	{
		for (DiscoveredModule module : _modules)
		{
			final String id = module.manifest.getId();
			if (!module.enabled || _refusedIds.contains(id))
			{
				continue;
			}

			for (ModuleIdRange range : module.manifest.getReserves(type))
			{
				final Integer collision = firstIdInRange(baseIds, range);
				if (collision != null)
				{
					if (_refusedIds.add(id))
					{
						_refused++;
						LOGGER.warning(LOG_PREFIX + "Refused '" + id + "': reserved " + type.getKey() + " range " + range + " overlaps base-game " + type.getKey() + " id " + collision + ".");
					}
					break;
				}
			}
		}
	}

	private static Integer firstIdInRange(Set<Integer> ids, ModuleIdRange range)
	{
		for (int id : ids)
		{
			if (range.contains(id))
			{
				return id;
			}
		}
		return null;
	}

	private void enable(DiscoveredModule module) throws Exception
	{
		final ModuleManifest manifest = module.manifest;
		final List<Path> scripts = collectScripts(module.directory.resolve("scripts"));
		if (scripts.isEmpty())
		{
			throw new ModuleException("Module '" + manifest.getId() + "' has no scripts under its scripts directory.");
		}

		LOGGER.info(LOG_PREFIX + manifest.getName() + ": compiling " + scripts.size() + " script(s).");
		final ScriptClassLoader classLoader = ScriptEngine.getInstance().compileModuleScripts(scripts);

		final Class<?> entryClass;
		try
		{
			entryClass = classLoader.loadClass(manifest.getEntrypoint());
		}
		catch (ClassNotFoundException e)
		{
			throw new ModuleException("Entry point '" + manifest.getEntrypoint() + "' was not found among the compiled scripts.", e);
		}

		if (!GameModule.class.isAssignableFrom(entryClass))
		{
			throw new ModuleException("Entry point '" + manifest.getEntrypoint() + "' does not implement GameModule.");
		}

		// Run the module's install script before its code, so its tables exist when onEnable runs. This never drops
		// tables; removal of stored data is a separate, explicit, opt-in action.
		final ModuleDatabaseSpec database = manifest.getDatabase();
		if (database.hasInstall())
		{
			LOGGER.info(LOG_PREFIX + manifest.getName() + ": running database install " + database.install());
			ModuleDatabase.runScript(manifest.getId(), module.directory.resolve(database.install()));
		}

		final GameModule instance = (GameModule) entryClass.getDeclaredConstructor().newInstance();

		final ModuleHandles handles = new ModuleHandles(manifest.getId());
		final ModuleContext context = new DefaultModuleContext(manifest.getId(), module.config, new ModuleHandlers(handles), new ModuleEvents(handles));
		instance.onEnable(context);

		_handlesByModule.put(manifest.getId(), handles);
		LOGGER.info(LOG_PREFIX + manifest.getName() + ": enabled, " + handles.size() + " registration(s).");
	}

	private List<Path> collectScripts(Path scriptsDir) throws Exception
	{
		if (!Files.isDirectory(scriptsDir))
		{
			return Collections.emptyList();
		}

		try (Stream<Path> stream = Files.walk(scriptsDir))
		{
			return stream.filter(Files::isRegularFile).filter(p -> p.getFileName().toString().endsWith(".java")).sorted().collect(Collectors.toList());
		}
	}

	/**
	 * @return the registration record for an enabled module, or {@code null} if that module is not enabled
	 */
	public ModuleHandles getHandles(String moduleId)
	{
		return _handlesByModule.get(moduleId);
	}

	public static ModuleManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final ModuleManager INSTANCE = new ModuleManager();
	}

	private static class DiscoveredModule
	{
		final ModuleManifest manifest;
		final Path directory;
		final ModuleConfig config;
		final boolean enabled;

		DiscoveredModule(ModuleManifest manifest, Path directory, ModuleConfig config, boolean enabled)
		{
			this.manifest = manifest;
			this.directory = directory;
			this.config = config;
			this.enabled = enabled;
		}
	}
}
