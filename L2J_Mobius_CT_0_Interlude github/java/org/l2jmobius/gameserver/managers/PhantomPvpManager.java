/*
 * This file is part of the L2J Mobius project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.managers;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.custom.FakePlayersConfig;

/**
 * The phantom PvP decision layer.
 *
 * <p>
 * Player-versus-phantom (and phantom-versus-phantom) combat is not new game mechanics: a field phantom is a real
 * {@link org.l2jmobius.gameserver.model.actor.Player}, so the stock PvP flag, karma / PK, and duel systems already
 * apply to it. What this class owns is the <b>decision</b>: whether a phantom should engage, and whether it should
 * stand and fight or break off and flee. The wiring that reads a phantom's state each tick, sets its target, and
 * moves it lives in {@link PhantomManager} (which owns the per-phantom state bag); the pure, side-effect-free
 * decision functions live here so they can be unit tested without a live server.
 * </p>
 *
 * <p>
 * Everything is gated behind {@link FakePlayersConfig#PHANTOM_PVP_ENABLED}. With the master switch off, the gate
 * methods all return {@code false} and no PvP behavior runs. See {@code docs/PVP_SYSTEM_DESIGN.md}.
 * </p>
 *
 * <p>
 * Phase 0 delivers the config gating and personality rolls; Phase 1 adds the self-defense stand-or-flee decision.
 * Later phases (flag / PK reaction, duels, party defense, open-world ganking) reuse the same gates and the same
 * stand-or-flee routine.
 * </p>
 */
public class PhantomPvpManager
{
	/** Bravery is rolled on this 0-100 scale; 50 is the neutral midpoint that leaves the flee threshold unshifted. */
	public static final int BRAVERY_MIN = 0;
	public static final int BRAVERY_MAX = 100;

	/** A level gap this large against the phantom is hopeless: it flees regardless of its current HP or bravery. */
	public static final int HOPELESS_LEVEL_GAP = 10;

	/** Bravery shifts the flee threshold by at most this many HP points either way (timid flees sooner, brave later). */
	public static final int BRAVERY_SWING = 15;

	/** Each level the enemy is above the phantom raises the flee threshold (flee sooner) by this many HP points. */
	public static final int LEVEL_GAP_HP_WEIGHT = 3;

	/** Clamp on the level-gap contribution to the flee threshold, so a huge gap does not push it past the bravery term. */
	public static final int LEVEL_GAP_HP_MAX = 30;

	/** Each net nearby ally (phantom's side minus enemy's side) shifts the flee threshold by this many HP points. */
	public static final int ALLY_ADVANTAGE_HP_WEIGHT = 4;

	/** Clamp on the ally-count contribution, so being badly outnumbered still cannot swing the threshold without limit. */
	public static final int ALLY_ADVANTAGE_HP_MAX = 20;

	/** An out-of-mana caster/healer cannot fight effectively, so it flees this many HP points sooner. */
	public static final int CASTER_LOW_MP_PENALTY = 15;

	/** Hard floor / ceiling for the effective flee threshold, so it is always a sane HP percentage. */
	public static final int FLEE_HP_FLOOR = 5;
	public static final int FLEE_HP_CEIL = 90;

	protected PhantomPvpManager()
	{
	}

	// ---------------------------------------------------------------------
	// Config gating. All false when the master switch is off.
	// ---------------------------------------------------------------------

	/** @return {@code true} if any phantom PvP behavior may run at all (the master switch). */
	public static boolean pvpEnabled()
	{
		return FakePlayersConfig.PHANTOM_PVP_ENABLED;
	}

	/** @return {@code true} if a phantom should fight back (or flee) when it is attacked. */
	public static boolean selfDefenseEnabled()
	{
		return FakePlayersConfig.PHANTOM_PVP_ENABLED && FakePlayersConfig.PHANTOM_PVP_SELF_DEFENSE;
	}

	/** @return {@code true} if a phantom may engage an already-flagged or red combatant (Phase 2). */
	public static boolean reactToFlaggedEnabled()
	{
		return FakePlayersConfig.PHANTOM_PVP_ENABLED && FakePlayersConfig.PHANTOM_PVP_REACT_TO_FLAGGED;
	}

	/** @return {@code true} if a phantom takes part in formal duels (Phase 3). */
	public static boolean duelsEnabled()
	{
		return FakePlayersConfig.PHANTOM_PVP_ENABLED && FakePlayersConfig.PHANTOM_PVP_DUELS;
	}

	/** @return {@code true} if allies defend their party against a hostile (Phase 2). */
	public static boolean partyDefenseEnabled()
	{
		return FakePlayersConfig.PHANTOM_PVP_ENABLED && FakePlayersConfig.PHANTOM_PVP_PARTY_DEFENSE;
	}

	/** @return {@code true} if defense extends to clan and alliance members, not only the party (Phase 2b). */
	public static boolean clanDefenseEnabled()
	{
		return partyDefenseEnabled() && FakePlayersConfig.PHANTOM_PVP_CLAN_DEFENSE;
	}

	/** @return {@code true} if phantoms may fight each other, not only the player. */
	public static boolean betweenPhantomsEnabled()
	{
		return FakePlayersConfig.PHANTOM_PVP_ENABLED && FakePlayersConfig.PHANTOM_PVP_BETWEEN_PHANTOMS;
	}

	/**
	 * Whether a phantom may fight an opponent of the given kind. A real player is always allowed; a phantom opponent
	 * (phantom-versus-phantom) is allowed only when {@code betweenPhantomsAllowed}. Pure, so the participant rule is
	 * unit tested without a world; the world-facing {@code validPvpOpponent} in {@link PhantomManager} calls this.
	 * @param targetIsPhantom whether the opponent is itself a phantom (Player-based bot)
	 * @param betweenPhantomsAllowed the {@link #betweenPhantomsEnabled()} value
	 * @return {@code true} if this opponent kind is allowed
	 */
	public static boolean opponentKindAllowed(boolean targetIsPhantom, boolean betweenPhantomsAllowed)
	{
		return !targetIsPhantom || betweenPhantomsAllowed;
	}

	/** @return {@code true} if a phantom may start PvP on an unprovoked target in the open field (Phase 4). */
	public static boolean openWorldGankEnabled()
	{
		return FakePlayersConfig.PHANTOM_PVP_ENABLED && FakePlayersConfig.PHANTOM_PVP_OPEN_WORLD_GANK;
	}

	// ---------------------------------------------------------------------
	// Personality, rolled once per phantom at construction.
	// ---------------------------------------------------------------------

	/**
	 * Rolls whether this phantom is an aggressor. An aggressor is a candidate to INITIATE PvP (react to a flag / PK,
	 * or gank); a non-aggressor only ever defends itself or its party. The share of aggressors is
	 * {@link FakePlayersConfig#PHANTOM_PVP_AGGRESSOR_PERCENT}.
	 * @return {@code true} if this phantom is an aggressor
	 */
	public static boolean rollAggressor()
	{
		return Rnd.get(100) < FakePlayersConfig.PHANTOM_PVP_AGGRESSOR_PERCENT;
	}

	/**
	 * Rolls a bravery value on the {@link #BRAVERY_MIN}..{@link #BRAVERY_MAX} scale. Higher bravery lets a phantom
	 * tolerate being more outmatched before it flees.
	 * @return the rolled bravery
	 */
	public static int rollBravery()
	{
		return Rnd.get(BRAVERY_MIN, BRAVERY_MAX);
	}

	/**
	 * Rolls whether an eligible aggressor actually engages a flagged/red target it noticed this consideration. Keeps
	 * PK-reaction occasional rather than automatic; the share is {@link FakePlayersConfig#PHANTOM_PVP_REACT_CHANCE_PERCENT}.
	 * @return {@code true} to engage this time
	 */
	public static boolean rollReactEngage()
	{
		return Rnd.get(100) < FakePlayersConfig.PHANTOM_PVP_REACT_CHANCE_PERCENT;
	}

	/**
	 * Whether a phantom may INITIATE PvP on a target given their levels. It never initiates on a target more than
	 * {@code maxLevelsAbovePlayer} below itself (no level 70 stomping a level 25), nor on one hopelessly above it
	 * ({@link #HOPELESS_LEVEL_GAP}, since it would just flee). Self-defense is unaffected; this only gates initiation.
	 * @param selfLevel the phantom's level
	 * @param targetLevel the target's level
	 * @param maxLevelsAbovePlayer how many levels over the target the phantom may be and still initiate
	 * @return {@code true} if initiation is allowed by level
	 */
	public static boolean mayInitiateByLevel(int selfLevel, int targetLevel, int maxLevelsAbovePlayer)
	{
		if ((selfLevel - targetLevel) > maxLevelsAbovePlayer)
		{
			return false; // too far above the target: an unfair stomp
		}
		if ((targetLevel - selfLevel) >= HOPELESS_LEVEL_GAP)
		{
			return false; // hopelessly outmatched: it would only flee, so do not start
		}
		return true;
	}

	// ---------------------------------------------------------------------
	// Stand-or-flee. Pure functions, no world access, so they unit test cleanly.
	// ---------------------------------------------------------------------

	/**
	 * The HP percentage at or below which a phantom flees, after its bravery and the level gap shift the configured
	 * base. A timid phantom (low bravery) flees sooner; a brave one (high bravery) later. Facing a higher-level enemy
	 * raises the threshold (flee sooner); out-leveling the enemy lowers it. The result is always a sane percentage
	 * between {@link #FLEE_HP_FLOOR} and {@link #FLEE_HP_CEIL}.
	 * @param baseFleeHpPercent the configured base flee threshold ({@link FakePlayersConfig#PHANTOM_PVP_FLEE_HP_PERCENT})
	 * @param bravery this phantom's bravery, {@link #BRAVERY_MIN}..{@link #BRAVERY_MAX}
	 * @param selfLevel the phantom's level
	 * @param enemyLevel the opponent's level
	 * @return the effective flee HP percentage
	 */
	public static int effectiveFleeHpPercent(int baseFleeHpPercent, int bravery, int selfLevel, int enemyLevel)
	{
		return effectiveFleeHpPercent(baseFleeHpPercent, bravery, selfLevel, enemyLevel, 0, false);
	}

	/**
	 * As {@link #effectiveFleeHpPercent(int, int, int, int)}, plus two Phase 2 situational inputs. Outnumbering the
	 * enemy lets a phantom stand longer (lower threshold); being outnumbered makes it flee sooner. An out-of-mana
	 * caster/healer flees sooner because it cannot fight effectively. All terms are clamped and the result stays a
	 * sane percentage between {@link #FLEE_HP_FLOOR} and {@link #FLEE_HP_CEIL}.
	 * @param baseFleeHpPercent the configured base flee threshold
	 * @param bravery this phantom's bravery, {@link #BRAVERY_MIN}..{@link #BRAVERY_MAX}
	 * @param selfLevel the phantom's level
	 * @param enemyLevel the opponent's level
	 * @param allyAdvantage nearby allies on the phantom's side minus nearby allies on the enemy's side (may be negative)
	 * @param casterLowMp {@code true} if this phantom is a caster/healer currently low on MP
	 * @return the effective flee HP percentage
	 */
	public static int effectiveFleeHpPercent(int baseFleeHpPercent, int bravery, int selfLevel, int enemyLevel, int allyAdvantage, boolean casterLowMp)
	{
		int threshold = baseFleeHpPercent;
		// Bravery term: neutral bravery (50) is 0; timid (0) adds +BRAVERY_SWING (flee sooner), brave (100) subtracts it.
		final int clampedBravery = Math.max(BRAVERY_MIN, Math.min(BRAVERY_MAX, bravery));
		threshold += Math.round(((BRAVERY_MAX / 2f) - clampedBravery) * (BRAVERY_SWING / (BRAVERY_MAX / 2f)));
		// Level term: enemy above the phantom flees sooner; phantom above the enemy stands longer.
		final int levelGap = enemyLevel - selfLevel; // positive means the enemy is higher level
		threshold += Math.max(-LEVEL_GAP_HP_MAX, Math.min(LEVEL_GAP_HP_MAX, levelGap * LEVEL_GAP_HP_WEIGHT));
		// Ally term: a positive advantage (phantom outnumbers) lowers the threshold (stand longer); negative raises it.
		final int allyTerm = Math.max(-ALLY_ADVANTAGE_HP_MAX, Math.min(ALLY_ADVANTAGE_HP_MAX, allyAdvantage * ALLY_ADVANTAGE_HP_WEIGHT));
		threshold -= allyTerm;
		// Out-of-mana caster: flee sooner.
		if (casterLowMp)
		{
			threshold += CASTER_LOW_MP_PENALTY;
		}
		return Math.max(FLEE_HP_FLOOR, Math.min(FLEE_HP_CEIL, threshold));
	}

	/**
	 * Whether a phantom should break off and flee rather than keep fighting. It flees when hopelessly out-leveled
	 * (regardless of HP), or when its current HP has fallen to or below its effective flee threshold.
	 * @param selfHpPercent the phantom's current HP percentage (0-100)
	 * @param baseFleeHpPercent the configured base flee threshold
	 * @param bravery this phantom's bravery
	 * @param selfLevel the phantom's level
	 * @param enemyLevel the opponent's level
	 * @return {@code true} to flee, {@code false} to stand and fight
	 */
	public static boolean shouldFlee(int selfHpPercent, int baseFleeHpPercent, int bravery, int selfLevel, int enemyLevel)
	{
		return shouldFlee(selfHpPercent, baseFleeHpPercent, bravery, selfLevel, enemyLevel, 0, false);
	}

	/**
	 * As {@link #shouldFlee(int, int, int, int, int)}, with the Phase 2 ally-count and caster-MP inputs threaded into
	 * the effective threshold. A hopeless level gap still forces a flee regardless of the situational terms.
	 * @param selfHpPercent the phantom's current HP percentage (0-100)
	 * @param baseFleeHpPercent the configured base flee threshold
	 * @param bravery this phantom's bravery
	 * @param selfLevel the phantom's level
	 * @param enemyLevel the opponent's level
	 * @param allyAdvantage nearby allies on the phantom's side minus nearby allies on the enemy's side
	 * @param casterLowMp {@code true} if this phantom is a caster/healer currently low on MP
	 * @return {@code true} to flee, {@code false} to stand and fight
	 */
	public static boolean shouldFlee(int selfHpPercent, int baseFleeHpPercent, int bravery, int selfLevel, int enemyLevel, int allyAdvantage, boolean casterLowMp)
	{
		if ((enemyLevel - selfLevel) >= HOPELESS_LEVEL_GAP)
		{
			return true;
		}
		return selfHpPercent <= effectiveFleeHpPercent(baseFleeHpPercent, bravery, selfLevel, enemyLevel, allyAdvantage, casterLowMp);
	}
}
