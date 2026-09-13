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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The startup cross-module validation, kept as pure logic with no engine dependencies so it can be reasoned about and
 * tested on its own. It decides which enabled modules must be refused because they collide with each other, before any
 * module resource enters a live registry or any module code runs (framework specification section 6.1).
 * <p>
 * It covers the checks that only need the modules themselves: reserved id-range overlap between modules, declared
 * conflicts, required dependencies, and dependency cycles. The separate check of a module's reserved ranges against ids
 * the base game already owns needs the stock datapack indexed first and is handled elsewhere.
 * <p>
 * Only enabled modules take part. A disabled module contributes nothing, so it is neither refused here nor counted as a
 * satisfied dependency. Refusals are computed in a fixed order so the outcome never depends on iteration order: reserved
 * range overlaps first, then declared conflicts, then unmet dependencies to a fixpoint, then dependency cycles.
 */
public final class ModuleValidator
{
	private ModuleValidator()
	{
	}

	/**
	 * A single module reduced to just what cross-module validation needs.
	 *
	 * @param id the module id
	 * @param enabled whether its switch is on
	 * @param dependencies ids of modules this one requires
	 * @param conflicts ids of modules this one refuses to run alongside
	 * @param reserves the id ranges it claims, per resource type
	 */
	public record Candidate(String id, boolean enabled, List<String> dependencies, List<String> conflicts, Map<ModuleResourceType, List<ModuleIdRange>> reserves)
	{
	}

	/**
	 * Validates the discovered modules against each other.
	 * @param candidates every discovered module, enabled or not
	 * @return an insertion-ordered map from the id of each refused module to the reason, empty when nothing is refused
	 */
	public static Map<String, String> validate(List<Candidate> candidates)
	{
		final Map<String, Candidate> byId = new LinkedHashMap<>();
		for (Candidate candidate : candidates)
		{
			byId.put(candidate.id(), candidate);
		}

		final Map<String, String> refused = new LinkedHashMap<>();

		// 1. Reserved id-range overlap between two enabled modules.
		final List<Candidate> enabled = new ArrayList<>();
		for (Candidate candidate : candidates)
		{
			if (candidate.enabled())
			{
				enabled.add(candidate);
			}
		}
		for (int i = 0; i < enabled.size(); i++)
		{
			for (int j = i + 1; j < enabled.size(); j++)
			{
				final Candidate a = enabled.get(i);
				final Candidate b = enabled.get(j);
				final String overlap = firstOverlap(a, b);
				if (overlap != null)
				{
					refused.putIfAbsent(a.id(), "reserved id range overlaps module '" + b.id() + "' on " + overlap);
					refused.putIfAbsent(b.id(), "reserved id range overlaps module '" + a.id() + "' on " + overlap);
				}
			}
		}

		// 2. Declared conflicts: an enabled module that names an enabled, still-standing module it cannot run with.
		for (Candidate candidate : enabled)
		{
			if (refused.containsKey(candidate.id()))
			{
				continue;
			}
			for (String conflictId : candidate.conflicts())
			{
				final Candidate other = byId.get(conflictId);
				if ((other != null) && other.enabled() && !refused.containsKey(conflictId))
				{
					refused.put(candidate.id(), "declares a conflict with enabled module '" + conflictId + "'");
					break;
				}
			}
		}

		// 3. Required dependencies, to a fixpoint so a refusal cascades to whatever depended on the refused module.
		boolean changed = true;
		while (changed)
		{
			changed = false;
			for (Candidate candidate : enabled)
			{
				if (refused.containsKey(candidate.id()))
				{
					continue;
				}
				for (String dependencyId : candidate.dependencies())
				{
					final Candidate dependency = byId.get(dependencyId);
					final String reason = dependencyProblem(dependencyId, dependency, refused);
					if (reason != null)
					{
						refused.put(candidate.id(), reason);
						changed = true;
						break;
					}
				}
			}
		}

		// 4. Dependency cycles among the modules still standing. Membership is computed against a stable snapshot of who
		// was already refused, so that refusing one member does not hide the rest of its own cycle.
		final Set<String> standingBeforeCycles = new HashSet<>(refused.keySet());
		for (Candidate candidate : enabled)
		{
			if (!standingBeforeCycles.contains(candidate.id()) && inCycle(candidate.id(), byId, standingBeforeCycles))
			{
				refused.put(candidate.id(), "is part of a dependency cycle");
			}
		}

		return refused;
	}

	private static String dependencyProblem(String dependencyId, Candidate dependency, Map<String, String> refused)
	{
		if (dependency == null)
		{
			return "requires module '" + dependencyId + "', which is not installed";
		}
		if (!dependency.enabled())
		{
			return "requires module '" + dependencyId + "', which is installed but disabled";
		}
		if (refused.containsKey(dependencyId))
		{
			return "requires module '" + dependencyId + "', which was refused";
		}
		return null;
	}

	private static String firstOverlap(Candidate a, Candidate b)
	{
		for (ModuleResourceType type : a.reserves().keySet())
		{
			final List<ModuleIdRange> bRanges = b.reserves().get(type);
			if (bRanges == null)
			{
				continue;
			}
			for (ModuleIdRange aRange : a.reserves().get(type))
			{
				for (ModuleIdRange bRange : bRanges)
				{
					if (aRange.overlaps(bRange))
					{
						return type.getKey() + " " + aRange + " and " + bRange;
					}
				}
			}
		}
		return null;
	}

	/**
	 * @return whether the given module can reach itself by following required-dependency edges, walking only enabled,
	 *         still-standing modules; a self-dependency counts as a cycle
	 */
	private static boolean inCycle(String startId, Map<String, Candidate> byId, Set<String> refused)
	{
		final Set<String> visited = new HashSet<>();
		final List<String> stack = new ArrayList<>();
		stack.add(startId);
		boolean first = true;
		while (!stack.isEmpty())
		{
			final String current = stack.remove(stack.size() - 1);
			if (!first && current.equals(startId))
			{
				return true;
			}
			first = false;
			if (!visited.add(current))
			{
				continue;
			}

			final Candidate candidate = byId.get(current);
			if ((candidate == null) || !candidate.enabled() || (refused.contains(current) && !current.equals(startId)))
			{
				continue;
			}
			for (String dependencyId : candidate.dependencies())
			{
				stack.add(dependencyId);
			}
		}
		return false;
	}
}
