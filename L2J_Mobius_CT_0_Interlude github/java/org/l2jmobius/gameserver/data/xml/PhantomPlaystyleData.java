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
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.StatSet;

/**
 * Per-class combat playstyles for recruited phantom party members, loaded from
 * {@code data/PhantomPlaystyles.xml}. A playstyle is an ORDERED list of skill entries (document order =
 * priority); each entry names a skill id, a use category, and the conditions under which a phantom of
 * that class should cast it. The playstyle engine walks the list each combat tick and casts the first
 * entry whose conditions pass - everything the class knows but the playstyle does not list is
 * deliberately NOT used (curation is the point: a Bladedancer spamming Poison Blade Dance reads as a
 * bot; one that saves Medusa for a crowded pull reads as a player).
 * <p>
 * Hard skill mechanics (cast range, MP, reuse, item consume, cast-time HP gates like Frenzy's) are NOT
 * duplicated here - they come from the live {@code Skill} object at cast time. The XML only encodes the
 * tactical layer the skill data cannot express: when a human of that class would bother.
 */
public class PhantomPlaystyleData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(PhantomPlaystyleData.class.getName());

	/** What kind of moment an entry is for; drives the engine's target choice and MP-reserve handling. */
	public enum Use
	{
		OPENER, // first hit of a fight (positional blow, alpha debuff); falls back to ROTATION order after
		ROTATION, // bread-and-butter damage, cast whenever conditions allow
		AOE, // area damage; gated on a real pack and never into party-slept mobs
		DEBUFF, // target weakening worth the cast only on durable targets
		CONTROL, // stun/slow/root style interruption
		PANIC, // self-defense at low HP while under attack (targets self)
		LIMIT, // low-HP limit buffs (Frenzy/Zealot family): healer-gated, skill data enforces its own HP gate (targets self)
		PULL, // ranged tag the camp puller opens a pull with (a cheap single-target nuke); read ONLY by the pull
		// hook, never by the combat rotation - so a nuker that would otherwise body-pull instead shoots the mob.
		STANCE; // a combat toggle (Vicious Stance) turned on in a fight while it is off, and dropped before resting (targets self)

		public boolean self()
		{
			return (this == PANIC) || (this == LIMIT);
		}
	}

	/** The closed condition vocabulary an entry's {@code when} attribute may use (comma = AND). */
	public enum Cond
	{
		ALWAYS, // no tactical gate (mechanics gates still apply)
		TARGET_HP_BELOW, // target under hpBelow% (execute-style skills)
		TARGET_HP_ABOVE, // target over hpAbove% (don't open long setups on a dying mob)
		SELF_HP_BELOW, // own HP under selfHpBelow%
		MP_ABOVE, // own MP over mpAbove% (spend only when comfortable)
		MOBS_NEAR, // at least mobsAtLeast live monsters inside the skill's affect range around the target
		MOBS_UNSPOILED, // at least mobsAtLeast UNSPOILED live monsters in range (Spoil Festival: don't AoE-spoil an already-spoiled pack)
		REAR, // standing behind the target (positional blows)
		CHARGES, // at least chargesAtLeast force/sonic charges banked (spenders)
		CHARGES_BELOW, // fewer than chargesBelow charges banked (builders)
		DEBUFF_MISSING, // the skill's abnormal slot is free on the target (don't re-stack a held debuff)
		SELF_ABNORMAL_FREE, // the skill's abnormal slot is free on the CASTER (don't overwrite an active self-limit, e.g. Guts replacing Frenzy in the shared PINCH slot)
		NOT_SPOILED, // the target has no spoiler yet (Spoil retries on a resisted attempt and never re-casts an already-spoiled mob)
		DURABLE_TARGET, // target is a raid or meaty enough that a setup cast amortizes
		HEALER_READY, // a live party healer with MP is present (gate for LIMIT self-endangering)
		ONCE_PER_TARGET, // cast at most once per target (openers, per-fight debuffs)
		UNDER_ATTACK, // something is actively coming at this member (PANIC gate)
		PVP, // the target is a player (dagger PvP openers and target drops)
		FOCUS_ON_ME; // the target has this member selected (Trick/Switch make it drop us)
	}

	/** One ordered playstyle line: cast {@code skillId} when all {@code conds} hold. Immutable after load. */
	public static class PlayEntry
	{
		public final int skillId;
		public final Use use;
		public final EnumSet<Cond> conds;
		public final int hpBelow; // TARGET_HP_BELOW threshold (percent)
		public final int hpAbove; // TARGET_HP_ABOVE threshold (percent)
		public final int selfHpBelow; // SELF_HP_BELOW threshold (percent)
		public final int mpAbove; // MP_ABOVE threshold (percent)
		public final int mobsAtLeast; // MOBS_NEAR minimum pack size
		public final int chargesAtLeast; // CHARGES minimum banked
		public final int chargesBelow; // CHARGES_BELOW bank cap for builders
		public final int paceMs; // per-entry pacing floor override (0 = engine default)
		public final int minLevel; // entry inactive below this character level
		public final int maxLevel; // ...and above this one. "Not learned yet" needs no bound (unknown skills are
		// skipped anyway); maxLevel is what expresses an OUTGROWN skill - a level 80 archer
		// still knows Power Shot, so only an explicit cap retires it.

		PlayEntry(int skillId, Use use, EnumSet<Cond> conds, StatSet set)
		{
			this.skillId = skillId;
			this.use = use;
			this.conds = conds;
			hpBelow = set.getInt("hpBelow", 100);
			hpAbove = set.getInt("hpAbove", 0);
			selfHpBelow = set.getInt("selfHpBelow", 100);
			mpAbove = set.getInt("mpAbove", 0);
			mobsAtLeast = set.getInt("mobsAtLeast", 3);
			chargesAtLeast = set.getInt("chargesAtLeast", 2);
			chargesBelow = set.getInt("chargesBelow", 8);
			paceMs = set.getInt("paceMs", 0);
			minLevel = set.getInt("minLevel", 1);
			maxLevel = set.getInt("maxLevel", 100);
		}

		/** @return {@code true} if this entry applies at {@code level} (the XML's minLevel/maxLevel window). */
		public boolean appliesAt(int level)
		{
			return (level >= minLevel) && (level <= maxLevel);
		}
	}

	/**
	 * One class lineage's ordered playstyle. {@code role} is optional and disambiguates lineages whose
	 * classes are SHARED between two archetypes - a level 20-39 Human archer and dagger are both a plain
	 * Rogue, so class id alone cannot tell them apart; the recruited member's party role can.
	 */
	public static class Playstyle
	{
		public final String name;
		public final String role; // null = applies to any role
		public final List<PlayEntry> entries;

		Playstyle(String name, String role, List<PlayEntry> entries)
		{
			this.name = name;
			this.role = role;
			this.entries = entries;
		}
	}

	/** Swapped wholesale on (re)load so game threads never observe a half-built map. */
	private volatile Map<Integer, List<Playstyle>> _byClassId = new HashMap<>();
	private Map<Integer, List<Playstyle>> _building;
	private final List<String> _warnings = new ArrayList<>();
	/** Bumped on every (re)load so cached per-member resolutions know to look again - including the
	 * "this class had no playstyle" answer, which an object comparison alone could never invalidate. */
	private volatile int _generation;

	protected PhantomPlaystyleData()
	{
		load();
	}

	@Override
	public synchronized void load()
	{
		_warnings.clear();
		_building = new HashMap<>();
		parseDatapackFile("data/PhantomPlaystyles.xml");
		_byClassId = _building;
		_building = null;
		_generation++;
		LOGGER.info(getClass().getSimpleName() + ": Loaded playstyles for " + _byClassId.size() + " class id(s).");
	}

	/** Re-reads the file live ({@code //phantom playstyle}); members re-resolve on their next tick. */
	public String reload()
	{
		_warnings.clear();
		load();
		final String result = "Reloaded PhantomPlaystyles.xml: playstyles for " + _byClassId.size() + " class id(s). Members pick them up on their next combat tick (no re-recruit needed).";
		return _warnings.isEmpty() ? result : (result + " " + _warnings.size() + " problem(s) found - see //phantom playstyle check.");
	}

	/**
	 * Problems found while parsing (unknown condition, bad id, duplicate class id...). Kept so an admin
	 * authoring playstyles in game can see them with {@code //phantom playstyle check} instead of having
	 * to read the server log.
	 */
	public List<String> getWarnings()
	{
		return _warnings;
	}

	/** Current data generation; changes on every (re)load. */
	public int getGeneration()
	{
		return _generation;
	}

	/** Records a parse problem for both the log and the in-game report. */
	private void warn(String message)
	{
		_warnings.add(message);
		LOGGER.warning(getClass().getSimpleName() + ": " + message);
	}

	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, "playstyle", playstyleNode ->
		{
			final StatSet attrs = new StatSet(parseAttributes(playstyleNode));
			final String name = attrs.getString("name", "unnamed");
			final String classIds = attrs.getString("classIds", "");
			final String roleAttr = attrs.getString("role", "").trim();
			final String role = roleAttr.isEmpty() ? null : roleAttr.toUpperCase();
			if (classIds.isEmpty())
			{
				warn("Playstyle '" + name + "' has no classIds - skipped.");
				return;
			}

			final List<PlayEntry> entries = new ArrayList<>();
			forEach(playstyleNode, "skill", skillNode ->
			{
				final StatSet set = new StatSet(parseAttributes(skillNode));
				final int id = set.getInt("id", 0);
				if (id <= 0)
				{
					warn("Playstyle '" + name + "' has a <skill> without an id - skipped.");
					return;
				}
				final Use use;
				try
				{
					use = Use.valueOf(set.getString("use", "ROTATION").trim().toUpperCase());
				}
				catch (IllegalArgumentException e)
				{
					warn("Playstyle '" + name + "' skill " + id + " has an unknown use '" + set.getString("use", "") + "' - skipped.");
					return;
				}
				final EnumSet<Cond> conds = EnumSet.noneOf(Cond.class);
				for (String token : set.getString("when", "ALWAYS").split(","))
				{
					final String trimmed = token.trim().toUpperCase();
					if (trimmed.isEmpty())
					{
						continue;
					}
					try
					{
						conds.add(Cond.valueOf(trimmed));
					}
					catch (IllegalArgumentException e)
					{
						warn("Playstyle '" + name + "' skill " + id + " has an unknown condition '" + trimmed + "' - the entry is skipped so a typo can't silently relax a gate.");
						return;
					}
				}
				entries.add(new PlayEntry(id, use, conds, set));
			});

			final Playstyle playstyle = new Playstyle(name, role, Collections.unmodifiableList(entries));
			for (String idToken : classIds.split(","))
			{
				try
				{
					final int classId = Integer.parseInt(idToken.trim());
					final List<Playstyle> forClass = _building.computeIfAbsent(classId, k -> new ArrayList<>());
					boolean duplicate = false;
					for (Playstyle existing : forClass)
					{
						// Two playstyles may share a class id ONLY when their roles differ (Rogue = archer or
						// dagger). Same class id AND same role is an authoring mistake.
						if ((existing.role == null) ? (role == null) : existing.role.equals(role))
						{
							duplicate = true;
							break;
						}
					}
					if (duplicate)
					{
						warn("Class id " + classId + " (role " + (role == null ? "any" : role) + ") is claimed twice - keeping the first, ignoring '" + name + "'.");
					}
					else
					{
						forClass.add(playstyle);
					}
				}
				catch (NumberFormatException e)
				{
					warn("Playstyle '" + name + "' has a non-numeric class id '" + idToken + "' - ignored.");
				}
			}
		}));
	}

	/**
	 * Resolves the playstyle a member should follow. A role-specific entry wins over a role-agnostic one,
	 * so a shared 1st class (Rogue, Elven Scout, Assassin) can carry both an archer and a dagger playstyle.
	 * @param classId the phantom's exact {@code PlayerClass} id
	 * @param roleName the member's party role name (e.g. {@code "ARCHER"}), or {@code null}
	 * @return the matching playstyle, or {@code null} when none is defined (the member then keeps the
	 *         legacy AutoUse behavior)
	 */
	public Playstyle getPlaystyle(int classId, String roleName)
	{
		final List<Playstyle> forClass = _byClassId.get(classId);
		if (forClass == null)
		{
			return null;
		}
		Playstyle generic = null;
		for (Playstyle playstyle : forClass)
		{
			if (playstyle.role == null)
			{
				if (generic == null)
				{
					generic = playstyle;
				}
			}
			else if (playstyle.role.equals(roleName))
			{
				return playstyle;
			}
		}
		return generic;
	}

	public static PhantomPlaystyleData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final PhantomPlaystyleData INSTANCE = new PhantomPlaystyleData();
	}
}
