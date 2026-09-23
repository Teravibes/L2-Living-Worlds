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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.PhantomPlaystyleData;
import org.l2jmobius.gameserver.data.xml.PhantomPlaystyleData.Cond;
import org.l2jmobius.gameserver.data.xml.PhantomPlaystyleData.PlayEntry;
import org.l2jmobius.gameserver.data.xml.PhantomPlaystyleData.Playstyle;
import org.l2jmobius.gameserver.data.xml.PhantomPlaystyleData.Use;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.Monster;
import org.l2jmobius.gameserver.model.skill.AbnormalType;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.targets.TargetType;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * Picks the next skill a recruited phantom should cast, from its class's ordered playstyle
 * ({@link PhantomPlaystyleData}). Stateless except for the caller-owned {@link PlayState}: the party
 * manager holds one per member and calls {@link #pick} from its combat tick; the returned action is cast
 * with the manager's own {@code setTarget}/{@code doCast} guards.
 * <p>
 * The engine only decides the TACTICAL layer (which listed skill fits this moment). Mechanics come from
 * the live {@link Skill}: reuse via {@code isSkillDisabled}, MP via {@code getMpConsume}, cast-time
 * preconditions (Frenzy's HP gate, Backstab's behind check) via {@code checkCondition} - so it never
 * attempts a cast the engine core would reject.
 */
public class PhantomPlaystyleEngine
{
	/** Human pacing: a member fires at most one playstyle cast per this floor (plus jitter), so skills read as decisions, not a dump. */
	private static final int DEFAULT_PACE_MS = 1600;
	private static final int PACE_JITTER_MS = 900;
	/** DURABLE_TARGET: a setup debuff/limit amortizes on a raid or on anything whose MAX HP is at least this - i.e.
	 * durability is the target's total health pool (a proxy for how long it will live), not how much is left now. */
	private static final int DURABLE_TARGET_HP = 4000;
	/** Fallback engagement reach for melee skills that report no cast range, plus slack on all range gates. */
	private static final int MELEE_REACH = 80;
	private static final int RANGE_SLACK = 60;
	/** Default affect radius for MOBS_NEAR / the sleeper guard when an AoE skill reports none. */
	private static final int DEFAULT_AOE_RADIUS = 200;
	/** How many recent targets the once-per-target ledger remembers (LRU); enough to span a boss + its adds. */
	private static final int LEDGER_MAX_TARGETS = 24;
	/** Half-angle (degrees) of the frontal/rear arc used to approximate FRONT_AREA / BEHIND_AREA hit geometry. */
	private static final double ARC_HALF_ANGLE = 90.0;

	/** A chosen cast: the skill and who to point it at (self for PANIC/LIMIT, the focus otherwise). */
	public static class CastAction
	{
		public final Skill skill;
		public final Creature target;
		/** True when this cast consumes a once-per-target slot (OPENER / ONCE_PER_TARGET) - committed to the ledger by {@link #confirmCast} only after launch. */
		final boolean oncePerTarget;
		/** The engagement focus this cast belongs to; the ledger key {@link #confirmCast} writes under. */
		final int focusObjectId;
		/** The skill id recorded in the once-per-target ledger on a confirmed launch. */
		final int ledgerSkillId;

		CastAction(Skill skill, Creature target, boolean oncePerTarget, int focusObjectId, int ledgerSkillId)
		{
			this.skill = skill;
			this.target = target;
			this.oncePerTarget = oncePerTarget;
			this.focusObjectId = focusObjectId;
			this.ledgerSkillId = ledgerSkillId;
		}
	}

	/**
	 * Per-member playstyle runtime state, owned by the party manager's Member record. The playstyle
	 * resolves lazily on first use and re-resolves whenever the data generation changes, so a
	 * {@code //phantom playstyle} reload applies to members already in the field.
	 */
	public static class PlayState
	{
		Playstyle playstyle; // the resolved lineage playstyle (null when the class has none)
		boolean lookedUp;
		long nextCastAt; // pacing gate; PANIC ignores it
		// Once-per-target ledger: skill ids already spent, per target object id. An LRU bounded map (not a single
		// current-target set) so a boss -> add -> boss switch does NOT reopen OPENER/ONCE_PER_TARGET on the boss.
		final Map<Integer, Set<Integer>> castLedger = new LinkedHashMap<>(32, 0.75f, true)
		{
			@Override
			protected boolean removeEldestEntry(Map.Entry<Integer, Set<Integer>> eldest)
			{
				return size() > LEDGER_MAX_TARGETS;
			}
		};
		public List<Integer> parkedIds; // autoSkills parked at recruit time so AutoUse doesn't compete (restored on release)
		public List<Integer> parkedBuffIds; // playstyle-listed ids pulled out of autoBuffs (PANIC/LIMIT self-buffs AutoUse would burn at full HP)
		int parkedGeneration = -1; // data generation the current parking reflects; a reload bump re-parks (see syncParkingIfReloaded)

		int generation = -1; // data generation this resolution came from

		/** Forces re-resolution when the data has been reloaded, so an edit applies without re-recruiting. */
		void refreshIfReloaded()
		{
			final int current = PhantomPlaystyleData.getInstance().getGeneration();
			if (generation != current)
			{
				generation = current;
				lookedUp = false;
			}
		}
	}

	/**
	 * The skills of {@code playstyle} this player can actually act on right now: known (learned) and inside
	 * the entry's level window. This is what makes parking safe - a lineage whose playstyle lists only
	 * skills the member has not reached yet must NOT have its AutoUse emptied, or it would fight with
	 * nothing at all.
	 */
	private static int usableCount(Player npc, Playstyle playstyle)
	{
		if (playstyle == null)
		{
			return 0;
		}
		final int level = npc.getLevel();
		int count = 0;
		for (PlayEntry entry : playstyle.entries)
		{
			// PULL entries are pull-only openers the combat rotation never fires, so they must not, on their own,
			// count as "this playstyle can field something" - otherwise a class listing only a PULL tag would be
			// parked out of AutoUse and then stand silent in a fight (pick() skips PULL).
			if ((entry.use != Use.PULL) && entry.appliesAt(level) && (npc.getKnownSkill(entry.skillId) != null))
			{
				count++;
			}
		}
		return count;
	}

	/**
	 * Walks the member's playstyle in order and returns the first entry whose tactical conditions AND
	 * mechanical gates pass, or {@code null} when nothing fits this tick (the caller falls back to plain
	 * auto-attacking). Never called for supports - their supportTick already plays their class.
	 * @param npc the phantom
	 * @param focus the mob the party wants dead (never null)
	 * @param state the member's playstyle runtime state
	 * @param healerReady a live party healer with MP exists (gates LIMIT entries)
	 * @param underAttack something is actively coming at this member (gates PANIC entries)
	 * @param mpReservePercent below this own-MP percent, spending entries are skipped (PANIC/LIMIT exempt)
	 * @param roleName the member's party role name, used to resolve role-split lineages
	 */
	public static CastAction pick(Player npc, Creature focus, PlayState state, boolean healerReady, boolean underAttack, int mpReservePercent, String roleName)
	{
		final int classId = npc.getPlayerClass().getId();
		state.refreshIfReloaded();
		if (!state.lookedUp)
		{
			state.lookedUp = true;
			state.playstyle = PhantomPlaystyleData.getInstance().getPlaystyle(classId, roleName);
		}
		if (state.playstyle == null)
		{
			return null;
		}
		final List<PlayEntry> entries = state.playstyle.entries;
		if (entries.isEmpty())
		{
			return null;
		}
		final int level = npc.getLevel();

		// The once-per-target ledger for THIS focus (created on first sight, remembered across target switches so
		// returning to a boss after tagging an add does not reopen its OPENER / ONCE_PER_TARGET entries).
		final Set<Integer> spentOnFocus = state.castLedger.computeIfAbsent(focus.getObjectId(), k -> new HashSet<>());

		final long now = System.currentTimeMillis();
		final boolean paced = now < state.nextCastAt;
		final int mpPercent = npc.getCurrentMpPercent();

		for (PlayEntry entry : entries)
		{
			// PULL is a pull-only opener consumed by the party manager's pull hook, never part of the combat
			// rotation - so a manager-owned taunt or a cheap tag nuke listed for pulling is not spammed in a fight.
			if (entry.use == Use.PULL)
			{
				continue;
			}
			// Outside its authored level window (a level 80 archer has outgrown Power Shot).
			if (!entry.appliesAt(level))
			{
				continue;
			}
			// Emergencies outrank the human pacing beat; everything else respects it.
			if (paced && (entry.use != Use.PANIC))
			{
				continue;
			}
			// Below the role's MP reserve only survival spending is allowed.
			if ((mpPercent < mpReservePercent) && !entry.use.self())
			{
				continue;
			}
			final boolean oncePerTarget = (entry.use == Use.OPENER) || entry.conds.contains(Cond.ONCE_PER_TARGET);
			// OPENER is once-per-engagement by contract; ONCE_PER_TARGET says so explicitly. The per-target ledger
			// (above) makes either fire at most once against this focus, even across target switches.
			if (oncePerTarget && spentOnFocus.contains(entry.skillId))
			{
				continue;
			}
			final Skill skill = npc.getKnownSkill(entry.skillId);
			if ((skill == null) || npc.isSkillDisabled(skill) || (npc.getCurrentMp() < skill.getMpConsume()) || !PhantomBuffs.canAffordReagent(npc, skill))
			{
				continue;
			}
			final boolean selfCast = entry.use.self() || (skill.getTargetType() == TargetType.SELF);
			if (!selfCast && !inReach(npc, focus, skill))
			{
				continue; // out of range - positioning/auto-attack closes the gap, retry next tick
			}
			if (!conditionsPass(npc, focus, entry, skill, healerReady, underAttack))
			{
				continue;
			}
			final Creature target = selfCast ? npc : focus;
			// Last word goes to the skill's own cast-time preconditions (behind checks, HP gates, weapon
			// checks) so the engine never queues a cast the core would reject with a failure message.
			if (!skill.checkCondition(npc, target, false))
			{
				continue;
			}
			final int pace = (entry.paceMs > 0) ? entry.paceMs : DEFAULT_PACE_MS;
			state.nextCastAt = now + pace + Rnd.get(PACE_JITTER_MS);
			// The once-per-target ledger is written by confirmCast AFTER the cast actually launches - not here. An
			// opener the core then rejects at doCast (out of range, interrupted, target gone) must stay retryable
			// instead of being permanently marked spent for the life of this target.
			return new CastAction(skill, target, oncePerTarget, focus.getObjectId(), entry.skillId);
		}
		return null;
	}

	/**
	 * Records a once-per-target cast in the member's ledger. Called by the party manager ONLY after {@code doCast}
	 * has actually put the phantom into a casting state - so the OPENER / ONCE_PER_TARGET slot is spent on a real
	 * launch, never on a decision the engine core silently refused. A no-op for ordinary (repeatable) casts.
	 * @param state the member's playstyle runtime state (holds the per-target ledger)
	 * @param action the action returned by {@link #pick} that has now launched
	 */
	public static void confirmCast(PlayState state, CastAction action)
	{
		if ((action == null) || !action.oncePerTarget)
		{
			return;
		}
		state.castLedger.computeIfAbsent(action.focusObjectId, k -> new HashSet<>()).add(action.ledgerSkillId);
	}

	/**
	 * Out-of-combat preparation for a charge class: the charge-builder it should bank BEFORE a pull, so it opens
	 * prepared instead of building reactively (Interlude sonic/force energy persists ~10 minutes, so one pre-charge
	 * carries many pulls). Returns a self-cast builder when the member is currently below its authored target charge
	 * and the skill is ready, else {@code null}. Identified purely from the playstyle data: the entry gated on
	 * {@code CHARGES_BELOW} is by construction "build up to N charges", and its threshold is the target level.
	 * @param npc the phantom
	 * @param state the member's playstyle runtime state
	 * @param roleName the member's party role name, used to resolve role-split lineages
	 */
	public static CastAction pickPrep(Player npc, PlayState state, String roleName)
	{
		final int classId = npc.getPlayerClass().getId();
		state.refreshIfReloaded();
		if (!state.lookedUp)
		{
			state.lookedUp = true;
			state.playstyle = PhantomPlaystyleData.getInstance().getPlaystyle(classId, roleName);
		}
		if (state.playstyle == null)
		{
			return null;
		}
		final int level = npc.getLevel();
		for (PlayEntry entry : state.playstyle.entries)
		{
			if (!entry.appliesAt(level) || !entry.conds.contains(Cond.CHARGES_BELOW))
			{
				continue; // only the charge-builder entry drives pre-charging
			}
			if (npc.getCharges() >= entry.chargesBelow)
			{
				return null; // already at/above the authored target charge - nothing to prepare
			}
			final Skill skill = npc.getKnownSkill(entry.skillId);
			if ((skill == null) || npc.isSkillDisabled(skill) || (npc.getCurrentMp() < skill.getMpConsume()) || !PhantomBuffs.canAffordReagent(npc, skill))
			{
				return null; // builder not castable this moment - retry a later tick
			}
			if (!skill.checkCondition(npc, npc, false))
			{
				return null;
			}
			return new CastAction(skill, npc, false, 0, entry.skillId); // self-cast; not a once-per-target ledger entry
		}
		return null;
	}

	/**
	 * The ranged tag a camp puller should open a pull with, so a nuker shoots the mob instead of running into melee to
	 * body-pull it. Returns the first {@code use="PULL"} entry that is castable on {@code prey} RIGHT NOW - learned,
	 * inside its level window, off cooldown, affordable, and already in cast range - else {@code null} (the puller then
	 * keeps closing and body-pulls as before). PULL entries are read only here; the combat rotation never fires them.
	 * <p>
	 * Range is deliberately part of the gate: the puller starts at camp, far from the prey, so this returns {@code null}
	 * on the outbound run and only fires once the puller has closed to the tag skill's range. Below {@code mpReservePercent}
	 * the puller skips the tag and body-pulls, so a low-MP mage does not spend its fight mana just to pull.
	 * @param npc the puller
	 * @param prey the mob being fetched
	 * @param state the member's playstyle runtime state
	 * @param mpReservePercent below this own-MP percent, tag with a skill is skipped in favour of a body-pull
	 * @param roleName the member's party role name, used to resolve role-split lineages
	 */
	public static CastAction pickPullTag(Player npc, Monster prey, PlayState state, int mpReservePercent, String roleName)
	{
		final int classId = npc.getPlayerClass().getId();
		state.refreshIfReloaded();
		if (!state.lookedUp)
		{
			state.lookedUp = true;
			state.playstyle = PhantomPlaystyleData.getInstance().getPlaystyle(classId, roleName);
		}
		if (state.playstyle == null)
		{
			return null;
		}
		final int level = npc.getLevel();
		final int mpPercent = npc.getCurrentMpPercent();
		for (PlayEntry entry : state.playstyle.entries)
		{
			if ((entry.use != Use.PULL) || !entry.appliesAt(level))
			{
				continue;
			}
			if (mpPercent < mpReservePercent)
			{
				return null; // too low on mana to spend on a pull tag - body-pull and keep the mana for the fight
			}
			final Skill skill = npc.getKnownSkill(entry.skillId);
			if ((skill == null) || npc.isSkillDisabled(skill) || (npc.getCurrentMp() < skill.getMpConsume()) || !PhantomBuffs.canAffordReagent(npc, skill))
			{
				continue;
			}
			if (!inReach(npc, prey, skill))
			{
				return null; // still closing the distance - the caller body-pulls this tick and retries when in range
			}
			if (!skill.checkCondition(npc, prey, false))
			{
				continue;
			}
			return new CastAction(skill, prey, false, 0, entry.skillId);
		}
		return null;
	}

	private static boolean conditionsPass(Player npc, Creature focus, PlayEntry entry, Skill skill, boolean healerReady, boolean underAttack)
	{
		for (Cond cond : entry.conds)
		{
			switch (cond)
			{
				case ALWAYS:
				{
					break;
				}
				case TARGET_HP_BELOW:
				{
					if (focus.getCurrentHpPercent() >= entry.hpBelow)
					{
						return false;
					}
					break;
				}
				case TARGET_HP_ABOVE:
				{
					if (focus.getCurrentHpPercent() <= entry.hpAbove)
					{
						return false;
					}
					break;
				}
				case SELF_HP_BELOW:
				{
					if (npc.getCurrentHpPercent() >= entry.selfHpBelow)
					{
						return false;
					}
					break;
				}
				case MP_ABOVE:
				{
					if (npc.getCurrentMpPercent() <= entry.mpAbove)
					{
						return false;
					}
					break;
				}
				case MOBS_NEAR:
				{
					// PvE-only (an AoE justified by a monster pack). A Player focus has no mob pack, so the condition
					// fails and the AoE entry does not fire in PvP; the PvE path still passes a Monster here.
					if (!(focus instanceof Monster) || (countPack(npc, (Monster) focus, skill) < entry.mobsAtLeast))
					{
						return false;
					}
					break;
				}
				case MOBS_UNSPOILED:
				{
					// PvE-only (AoE-spoil worth gate). Spoil does not apply to a Player, so this never fires in PvP.
					if (!(focus instanceof Monster) || (countUnspoiled((Monster) focus, skill) < entry.mobsAtLeast))
					{
						return false; // not enough unspoiled mobs to make an AoE-spoil worthwhile
					}
					break;
				}
				case REAR:
				{
					if (!npc.isBehind(focus))
					{
						return false;
					}
					break;
				}
				case CHARGES:
				{
					if (npc.getCharges() < entry.chargesAtLeast)
					{
						return false;
					}
					break;
				}
				case CHARGES_BELOW:
				{
					if (npc.getCharges() >= entry.chargesBelow)
					{
						return false;
					}
					break;
				}
				case DEBUFF_MISSING:
				{
					final AbnormalType type = skill.getAbnormalType();
					if ((type != null) && (type != AbnormalType.NONE) && (focus.getEffectList().getBuffInfoByAbnormalType(type) != null))
					{
						return false; // the slot is already held (by us or a partner) - don't re-stack
					}
					break;
				}
				case SELF_ABNORMAL_FREE:
				{
					final AbnormalType selfType = skill.getAbnormalType();
					if ((selfType != null) && (selfType != AbnormalType.NONE) && (npc.getEffectList().getBuffInfoByAbnormalType(selfType) != null))
					{
						return false; // an active self-limit already owns this slot - casting would overwrite it (Guts onto Frenzy)
					}
					break;
				}
				case NOT_SPOILED:
				{
					// Spoil is PvE-only. On a Player focus this condition can never be satisfied usefully, so the
					// spoil skill it guards does not fire in PvP; the PvE path still passes a Monster here.
					if (!(focus instanceof Monster) || ((Monster) focus).isSpoiled())
					{
						return false; // already spoiled (or not a monster) - re-casting is wasted MP; retries next tick
					}
					break;
				}
				case DURABLE_TARGET:
				{
					if (!focus.isRaid() && (focus.getMaxHp() < DURABLE_TARGET_HP))
					{
						return false; // small mob - a setup/limit cast never amortizes (measured on total HP, not what's left)
					}
					break;
				}
				case HEALER_READY:
				{
					if (!healerReady)
					{
						return false;
					}
					break;
				}
				case ONCE_PER_TARGET:
				{
					// Enforced in pick() against the PlayState ledger (needs the caller's state, not just the entry).
					break;
				}
				case UNDER_ATTACK:
				{
					if (!underAttack)
					{
						return false;
					}
					break;
				}
			}
		}
		// The once-per-target ledger check (kept out of the switch so the state stays with the caller's loop).
		return true;
	}

	/** Where an AoE's affect area is anchored: on the caster (AURA family) or on the selected target (AREA family). */
	private static boolean isCasterCentered(Skill skill)
	{
		switch (skill.getTargetType())
		{
			case AURA:
			case AURA_CORPSE_MOB:
			case AURA_FRIENDLY:
			case FRONT_AURA:
			case BEHIND_AURA:
			{
				return true;
			}
			default:
			{
				return false;
			}
		}
	}

	/** The directional restriction of an AoE: only in front of / only behind the caster, or all around (0 = any). */
	private static int arcSign(Skill skill)
	{
		switch (skill.getTargetType())
		{
			case FRONT_AREA:
			case FRONT_AURA:
			{
				return 1; // frontal cone
			}
			case BEHIND_AREA:
			case BEHIND_AURA:
			{
				return -1; // rear cone
			}
			default:
			{
				return 0; // full circle
			}
		}
	}

	/** True if {@code mob} lies within the skill's directional arc relative to the caster's facing (frontal or rear). */
	private static boolean inArc(Player npc, Creature mob, int arcSign)
	{
		if (arcSign == 0)
		{
			return true;
		}
		double delta = Math.abs(npc.calculateDirectionTo(mob) - LocationUtil.convertHeadingToDegree(npc.getHeading()));
		if (delta > 180)
		{
			delta = 360 - delta; // normalize to 0..180 off the caster's heading
		}
		return (arcSign > 0) ? (delta <= ARC_HALF_ANGLE) : (delta >= (180 - ARC_HALF_ANGLE));
	}

	/**
	 * The mobs this AoE skill would actually and usefully hit right now, counting only ENGAGED monsters (in
	 * combat) so idle neutrals wandering into radius never inflate the count and pull the pack. The radius is
	 * measured around the skill's real center - the caster for AURA-family skills, the focus for target-centered
	 * ones - and, for a FRONT/BEHIND skill, each candidate must also fall inside the caster's frontal/rear arc so
	 * a cone is not judged by mobs standing beside or behind the target. BR-005 guard: a SLEEPING mob in the blast
	 * means a partner spent control on it and area damage would break it, so the pack does not count at all.
	 * @return the engaged pack size, or {@code Integer.MIN_VALUE} when a slept mob vetoes the AoE
	 */
	private static int countPack(Player npc, Monster focus, Skill skill)
	{
		final int radius = (skill.getAffectRange() > 0) ? skill.getAffectRange() : DEFAULT_AOE_RADIUS;
		final boolean casterCentered = isCasterCentered(skill);
		final Creature center = casterCentered ? npc : focus;
		final int arc = arcSign(skill);
		// The focus takes the hit when it is inside the affect area (always, for a target-centered blast; only when
		// within reach, for a caster-centered aura) AND inside the skill's directional arc.
		final boolean focusInRange = !casterCentered || (npc.calculateDistance2D(focus) <= radius);
		int count = (focusInRange && inArc(npc, focus, arc)) ? 1 : 0;
		for (Monster mob : World.getInstance().getVisibleObjectsInRange(center, Monster.class, radius))
		{
			if (mob.isDead() || (mob == focus)) // focus already accounted for above
			{
				continue;
			}
			if (mob.isSleeping())
			{
				return Integer.MIN_VALUE; // never AoE into party-owned sleep
			}
			// Only mobs actually in the fight (not neutral passers-by) and inside the skill's real hit geometry.
			if (mob.isInCombat() && inArc(npc, mob, arc))
			{
				count++;
			}
		}
		return count;
	}

	/**
	 * Unspoiled, ENGAGED live monsters inside an AoE-spoil's affect radius around the focus - the worth gate for
	 * Spoil Festival. Like {@link #countPack} it counts only in-combat mobs, so a focus flanked by untouched
	 * neutral monsters can't satisfy the pack size and pull them; the sleeper veto is handled the same way.
	 * @return the unspoiled pack size, or {@code Integer.MIN_VALUE} when a slept mob vetoes the AoE
	 */
	private static int countUnspoiled(Monster focus, Skill skill)
	{
		final int radius = (skill.getAffectRange() > 0) ? skill.getAffectRange() : DEFAULT_AOE_RADIUS;
		int count = focus.isSpoiled() ? 0 : 1; // the focus itself, only if it still needs spoiling
		for (Monster mob : World.getInstance().getVisibleObjectsInRange(focus, Monster.class, radius))
		{
			if (mob.isDead() || (mob == focus))
			{
				continue;
			}
			if (mob.isSleeping())
			{
				return Integer.MIN_VALUE; // never AoE into party-owned sleep
			}
			if (!mob.isSpoiled() && mob.isInCombat()) // unspoiled AND in the fight - not a neutral bystander
			{
				count++;
			}
		}
		return count;
	}

	/** Range gate: melee skills need contact reach, ranged ones their cast range (with slack for drift while both move). */
	private static boolean inReach(Player npc, Creature focus, Skill skill)
	{
		final int reach = (skill.getCastRange() > 0) ? (skill.getCastRange() + RANGE_SLACK) : ((skill.getAffectRange() > 0) ? skill.getAffectRange() : MELEE_REACH + RANGE_SLACK);
		// Collision-aware, so this matches the range the rest of the core actually fights at: CreatureAI.maybeMoveToPawn
		// parks the attacker at reach + BOTH collision radii, and LocationUtil.checkIfInRange adds the same. A raw
		// center-to-center check reads a large raid boss (big collision radius) as out of reach while the phantom is
		// physically beside it auto-attacking, wrongly suppressing melee/short-range playstyle skills on exactly the
		// targets they matter most on.
		return LocationUtil.checkIfInRange(reach, npc, focus, false);
	}

	/**
	 * Parks the skills this player's playstyle owns out of AutoUse at recruit time so the round-robin
	 * dump can't compete with the engine's decisions. Every OFFENSIVE auto-skill is parked when a
	 * playstyle exists (unlisted skills are deliberately unused: curation, not omission). Playstyle-LISTED
	 * ids are additionally pulled out of the auto-BUFF list - PANIC/LIMIT skills (Ultimate Evasion,
	 * Frenzy, Battle Roar...) are continuous self-buffs there, and AutoUse would burn them off cooldown at
	 * full HP on trash (the same bug the tank's Ultimate Defense parking fixed). Unlisted self-buffs keep
	 * their normal AutoUse upkeep. Both lists are stored on {@code state} for restoration at release.
	 */
	public static void parkAutoSkills(Player npc, PlayState state, String roleName)
	{
		// Stamp first so a later //phantom playstyle reload (generation bump) re-parks even a member the current
		// generation could not field a playstyle for (usableCount == 0 below), and so the first combat tick after
		// recruit does not needlessly unpark+repark (syncParkingIfReloaded sees the same generation and no-ops).
		state.parkedGeneration = PhantomPlaystyleData.getInstance().getGeneration();
		final Playstyle playstyle = PhantomPlaystyleData.getInstance().getPlaystyle(npc.getPlayerClass().getId(), roleName);
		// FAIL-SAFE: only take over when the playstyle can actually field something at this member's level.
		// A lineage whose entries are all still unlearned (a level 25 member on a 3rd-class rotation) would
		// otherwise be parked into silence - no AutoUse dump AND no playstyle cast. Legacy behavior is the
		// floor: this system may only ever improve a phantom, never leave it fighting bare-handed.
		if (usableCount(npc, playstyle) == 0)
		{
			return;
		}
		if (!npc.getAutoUseSettings().getAutoSkills().isEmpty())
		{
			state.parkedIds = new ArrayList<>(npc.getAutoUseSettings().getAutoSkills());
			npc.getAutoUseSettings().getAutoSkills().clear();
		}
		for (PlayEntry entry : playstyle.entries)
		{
			if (npc.getAutoUseSettings().getAutoBuffs().remove(Integer.valueOf(entry.skillId)))
			{
				if (state.parkedBuffIds == null)
				{
					state.parkedBuffIds = new ArrayList<>();
				}
				state.parkedBuffIds.add(entry.skillId);
			}
		}
	}

	/**
	 * Returns this member's engine-parked offensive skills and self-buffs to AutoUse (the inverse of
	 * {@link #parkAutoSkills}, minus the manager-owned lists it restores separately). Used both when a member
	 * leaves party service and when a reload changes ownership.
	 */
	public static void unparkAutoSkills(Player npc, PlayState state)
	{
		if (state.parkedIds != null)
		{
			for (Integer id : state.parkedIds)
			{
				if (!npc.getAutoUseSettings().getAutoSkills().contains(id))
				{
					npc.getAutoUseSettings().getAutoSkills().add(id);
				}
			}
			state.parkedIds = null;
		}
		if (state.parkedBuffIds != null)
		{
			for (Integer id : state.parkedBuffIds)
			{
				if (!npc.getAutoUseSettings().getAutoBuffs().contains(id))
				{
					npc.getAutoUseSettings().getAutoBuffs().add(id);
				}
			}
			state.parkedBuffIds = null;
		}
	}

	/**
	 * Reconciles AutoUse ownership after a {@code //phantom playstyle} reload so live members react to an edit
	 * that ADDS or REMOVES a playstyle without re-recruiting. The old parking is undone and re-run for the
	 * freshly resolved playstyle, atomically, before the tick's {@link #pick} resolves the new generation:
	 * <ul>
	 * <li>ADDED a playstyle - nothing was parked before, so the offensive AutoUse is now parked (no double casting).</li>
	 * <li>REMOVED a playstyle - the old parking is restored and re-park is a no-op (usableCount 0), so the member
	 * falls back to its AutoUse instead of being stuck on plain attacks.</li>
	 * <li>UNCHANGED - the same skills are parked again (idempotent).</li>
	 * </ul>
	 */
	public static void syncParkingIfReloaded(Player npc, PlayState state, String roleName)
	{
		if (state.parkedGeneration == PhantomPlaystyleData.getInstance().getGeneration())
		{
			return;
		}
		unparkAutoSkills(npc, state); // hand back whatever the previous generation parked...
		parkAutoSkills(npc, state, roleName); // ...then take ownership for the new one (re-stamps parkedGeneration)
	}
}
