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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.skill.AbnormalType;
import org.l2jmobius.gameserver.model.skill.BuffInfo;
import org.l2jmobius.gameserver.model.skill.Skill;

/**
 * Shared buff plan for support phantoms (personal buddies + recruited buffers): decides which buffs a target
 * actually wants, by the target's archetype (caster vs fighter) and its role in the group, so a buffer stops
 * wasting physical buffs on casters (and vice-versa) and only fully buffs the player it serves.
 * <ul>
 * <li><b>LEADER</b> (the player being served): the full kit minus the wrong-archetype buffs (no Haste/Might/
 * Focus for a caster; no Acumen/Empower/Wild Magic for a fighter).</li>
 * <li><b>MEMBER</b> (other party members): the bare essentials - Wind Walk plus Haste (melee) or Acumen/
 * Berserker (caster).</li>
 * <li><b>SELF</b> (the buffer itself): just movement and casting speed - Wind Walk and Acumen.</li>
 * </ul>
 * Buff ids cover the Interlude Prophet / Elder / Warcryer sets plus their scroll/song/dance equivalents, so the
 * same plan works whatever class is doing the buffing.
 */
public final class PhantomBuffs
{
	/** Who a buff target is, relative to the buffer. */
	public enum Tier
	{
		LEADER,
		MEMBER,
		SELF
	}

	// The buffs the support keeps up AUTOMATICALLY - a curated core set, on purpose. A high-level Prophet/Elder
	// knows far more buffs than a target's 20 buff slots (MaxBuffAmount) can hold: every resistance, both the base
	// and "greater" variant of a buff, War/Earth Chant, and so on. Auto-maintaining all of them overflowed the slot
	// cap, and because the engine evicts the OLDEST buff on each new cast, the buffer rotated through the whole kit
	// forever (the endless re-buff loop a low-level buffer never hit, because its short kit fit). So the buffer keeps
	// only this fixed core that fits comfortably; the situational / consumable "greater" buffs and the resistances
	// are cast ONLY when the player asks for them by name (see requestedBuff / "<buff> on <name>"). Each buff's class
	// variants are listed so one plan covers every buffer class: Prophet / Elder (1xxx + 43xx/44xx variants) AND the
	// Orc buffers, which are REAL classes when recruited - the Warcryer/Doomcryer "Chant of ..." set and the Overlord/
	// Dominator "Pa'agrio" set (added at the end of AUTO_MELEE/AUTO_CASTER; without them an Orc buffer buffed nothing).
	private static final Set<Integer> AUTO_COMMON = Set.of(1204, 4342, 4391, // Wind Walk
		1282); // Pa'agrian Haste (Overlord's run-speed buff - its Wind Walk equivalent)
	private static final Set<Integer> AUTO_MELEE = Set.of( //
		1086, 4357, 4402, // Haste
		1068, 4345, 4393, // Might (base; Greater Might is on request only - it shares a slot with Greater Shield)
		1077, 4359, 4404, // Focus
		1242, 4360, 4405, // Death Whisper
		1240, 4358, 4403, // Guidance
		1268, 4354, 4399, // Vampiric Rage
		1087, 4406, // Agility
		1040, // Shield (base; Greater Shield is on request only) - PD_UP slot
		1243, // Bless Shield (shield block rate, SHIELD_PROB_UP - its own slot, distinct from Shield's PD_UP)
		1044, // Regeneration (HP regen, HP_REGEN_UP - only lands if the buffer is an Elder/cleric line that knows it)
		1259, 4350, // Resist Shock (anti-stun, RESIST_SHOCK - own slot, no reagent)
		1035, // Mental Shield
		1036, // Magic Barrier
		1045, // Blessed Body
		1048, // Blessed Soul
		1062, 4352, 4397, // Berserker Spirit
		// Warcryer / Doomcryer chants. A recruited Warcryer is a REAL Warcryer (class 52, a selectable 2nd class),
		// not a Prophet, so its buff kit is the "Chant of ..." family - a wholly different id set the buffer loop
		// otherwise never recognised (it said "rebuffing" and cast nothing). These are the physical/melee chants:
		1007, // Chant of Battle (P.Atk)
		1251, // Chant of Fury (atk. speed)
		1253, // Chant of Rage (crit damage)
		1308, // Chant of Predator (crit rate)
		1309, // Chant of Eagle (accuracy)
		1310, // Chant of Vampire (vampiric attack)
		1390, // War Chant (P.Atk / P.Def)
		// Warcryer chants that help everyone (also listed under AUTO_CASTER):
		1006, // Chant of Fire (M.Def)
		1009, // Chant of Shielding (P.Def)
		1391, // Earth Chant (P.Def)
		1252, // Chant of Evasion (evasion)
		1284, // Chant of Revenge (damage reflect)
		// Overlord "Pa'agrio" chants (Overlord = class 51, also a real class recruited as a buffer). Physical set:
		1003, // Pa'agrian Gift (P.Atk)
		1249, // The Vision of Pa'agrio (accuracy)
		1250, // Under the Protection of Pa'agrio (shield block rate)
		1261, // The Rage of Pa'agrio (all-round power buff)
		// Overlord chants that help everyone (also listed under AUTO_CASTER):
		1005, // Blessings of Pa'agrio (P.Def)
		1008, // The Glory of Pa'agrio (M.Def)
		1260); // The Tact of Pa'agrio (evasion)
	private static final Set<Integer> AUTO_CASTER = Set.of( //
		1085, 4355, 4400, // Acumen
		1059, 4356, 4401, // Empower (base; Greater Empower is on request only)
		1303, 5164, // Wild Magic
		1078, 4351, // Concentration
		1040, // Shield (casters take P.Def for survivability too - this was missing, so mages got no Shield)
		1047, // Mana Regeneration (MP_REGEN_UP - standard caster sustain; only lands if the buffer knows it)
		1259, 4350, // Resist Shock (anti-stun, RESIST_SHOCK - a stunned caster is a dead caster; own slot, no reagent)
		1035, // Mental Shield
		1036, // Magic Barrier
		1045, // Blessed Body
		1048, // Blessed Soul
		1062, // Berserker Spirit
		// Warcryer / Doomcryer chants a caster wants (see the melee list for why these are needed):
		1002, // Flame Chant (casting speed)
		1006, // Chant of Fire (M.Def)
		1009, // Chant of Shielding (P.Def survivability)
		1391, // Earth Chant (P.Def)
		1252, // Chant of Evasion (evasion)
		1284, // Chant of Revenge (damage reflect)
		// Overlord "Pa'agrio" chants a caster wants:
		1004, // The Wisdom of Pa'agrio (casting speed)
		1261, // The Rage of Pa'agrio (all-round power buff)
		1005, // Blessings of Pa'agrio (P.Def survivability)
		1008, // The Glory of Pa'agrio (M.Def)
		1260); // The Tact of Pa'agrio (evasion)

	// Berserker Spirit (Prophet 1062 + its Elder/Orc variants). A power buff that also CUTS P.Def and M.Def, so it
	// helps damage dealers but hurts a tank - withheld from tanks by tankAvoids() at buff-selection and at spawn.
	private static final Set<Integer> BERSERKER_SPIRIT = Set.of(1062, 4352, 4397);

	// Pre-buff kits applied to a recruited member the moment it spawns, so it arrives already fully buffed for its
	// level (no need to re-buff a fresh party from scratch). Prophet/Elder primary ids; an unknown id is skipped.
	private static final int[] PREBUFF_COMMON =
	{
		1204 // Wind Walk
	};
	// The pre-buff kits mirror the AUTO_* maintained kits (Prophet/Elder primary ids), so a member spawns with the
	// SAME buffs the party's buffer keeps up. That is what makes ongoing upkeep of recruited members cheap: the
	// buffer finds the slots already filled and casts nothing until a 20-minute buff actually lapses, instead of
	// paying the whole kit right before a pull. Berserker Spirit is applied separately (PREBUFF_BERSERKER) so it can
	// be withheld from tanks.
	private static final int[] PREBUFF_MELEE =
	{
		1068, // Might
		1086, // Haste
		1077, // Focus
		1242, // Death Whisper
		1240, // Guidance
		1268, // Vampiric Rage
		1087, // Agility
		1040, // Shield
		1243, // Bless Shield (shield block rate)
		1044, // Regeneration (HP regen)
		1259, // Resist Shock (anti-stun)
		1035, // Mental Shield
		1036, // Magic Barrier
		1045, // Blessed Body
		1048 // Blessed Soul
	};
	private static final int[] PREBUFF_CASTER =
	{
		1085, // Acumen
		1059, // Empower (Greater Empower is maintained by the buffer if known; pre-buff uses base Empower)
		1303, // Wild Magic
		1078, // Concentration
		1397, // Clarity
		1040, // Shield (P.Def survivability for casters too)
		1047, // Mana Regeneration
		1259, // Resist Shock (a stunned caster is a dead caster)
		1035, // Mental Shield
		1036, // Magic Barrier
		1045, // Blessed Body
		1048, // Blessed Soul
		1062 // Berserker Spirit (a nuker wants the power; casters are never tanks)
	};
	// Berserker Spirit for MELEE - applied only to non-tanks (it lowers P.Def / M.Def; see tankAvoids).
	private static final int[] PREBUFF_BERSERKER =
	{
		1062 // Berserker Spirit
	};

	// Buff names/aliases a player might ask for by name ("give me might", "ww pls"). Maps to a canonical word
	// matched against the skills the buffer actually knows (so "might" finds "Greater Might" too).
	private static final Map<String, String> BUFF_ALIASES = new HashMap<>();
	static
	{
		BUFF_ALIASES.put("wind walk", "wind walk");
		BUFF_ALIASES.put("ww", "wind walk");
		BUFF_ALIASES.put("haste", "haste");
		BUFF_ALIASES.put("acumen", "acumen");
		BUFF_ALIASES.put("might", "might");
		BUFF_ALIASES.put("greater might", "greater might");
		BUFF_ALIASES.put("gmight", "greater might");
		BUFF_ALIASES.put("shield", "shield");
		BUFF_ALIASES.put("bless shield", "bless shield");
		BUFF_ALIASES.put("blessed shield", "bless shield");
		BUFF_ALIASES.put("greater shield", "greater shield");
		BUFF_ALIASES.put("gshield", "greater shield");
		BUFF_ALIASES.put("focus", "focus");
		BUFF_ALIASES.put("death whisper", "death whisper");
		BUFF_ALIASES.put("dw", "death whisper");
		BUFF_ALIASES.put("guidance", "guidance");
		BUFF_ALIASES.put("empower", "empower");
		BUFF_ALIASES.put("greater empower", "greater empower");
		BUFF_ALIASES.put("war chant", "war chant");
		BUFF_ALIASES.put("earth chant", "earth chant");
		// Specific chants map to their exact skill name so a named chant resolves to that chant only. Without these
		// a bare "chant" match would grab the first chant the buffer happened to know (e.g. Chant of Revenge). A
		// chant the buffer has not learned now returns "i don't have it" instead of silently swapping in another.
		BUFF_ALIASES.put("flame chant", "flame chant");
		BUFF_ALIASES.put("chant of victory", "chant of victory");
		BUFF_ALIASES.put("chant of battle", "chant of battle");
		BUFF_ALIASES.put("chant of fury", "chant of fury");
		BUFF_ALIASES.put("chant of rage", "chant of rage");
		BUFF_ALIASES.put("chant of predator", "chant of predator");
		BUFF_ALIASES.put("chant of eagle", "chant of eagle");
		BUFF_ALIASES.put("chant of vampire", "chant of vampire");
		BUFF_ALIASES.put("chant of fire", "chant of fire");
		BUFF_ALIASES.put("chant of shielding", "chant of shielding");
		BUFF_ALIASES.put("chant of evasion", "chant of evasion");
		BUFF_ALIASES.put("chant of revenge", "chant of revenge");
		BUFF_ALIASES.put("chant of life", "chant of life");
		BUFF_ALIASES.put("chant of spirit", "chant of spirit");
		BUFF_ALIASES.put("holy resist", "holy resist");
		BUFF_ALIASES.put("holy resistance", "holy resist");
		BUFF_ALIASES.put("unholy resist", "unholy resist");
		BUFF_ALIASES.put("unholy resistance", "unholy resist");
		BUFF_ALIASES.put("resist shock", "resist shock");
		BUFF_ALIASES.put("berserker", "berserker");
		BUFF_ALIASES.put("zerk", "berserker");
		BUFF_ALIASES.put("vampiric rage", "vampiric rage");
		BUFF_ALIASES.put("vamp", "vampiric rage");
		BUFF_ALIASES.put("concentration", "concentration");
		BUFF_ALIASES.put("wild magic", "wild magic");
		BUFF_ALIASES.put("magic barrier", "magic barrier");
		BUFF_ALIASES.put("clarity", "clarity");
		BUFF_ALIASES.put("agility", "agility");
		BUFF_ALIASES.put("regeneration", "regeneration");
		BUFF_ALIASES.put("regen", "regeneration");
		BUFF_ALIASES.put("mental shield", "mental shield");
		BUFF_ALIASES.put("bless the body", "bless the body");
		BUFF_ALIASES.put("bless the soul", "bless the soul");
		BUFF_ALIASES.put("prophecy", "prophecy");
		BUFF_ALIASES.put("vampiric", "vampiric rage");
	}

	private PhantomBuffs()
	{
	}

	/**
	 * @return the canonical name of a buff the message asks for by name (e.g. "give me might" -> "might"), or
	 *         {@code null} if the line names no known buff. Longest alias wins ("magic barrier" over "shield").
	 */
	public static String requestedBuff(String message)
	{
		final String m = " " + message.toLowerCase().replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim() + " ";
		String best = null;
		for (String alias : BUFF_ALIASES.keySet())
		{
			if (m.contains(" " + alias + " ") && ((best == null) || (alias.length() > best.length())))
			{
				best = alias;
			}
		}
		return (best == null) ? null : BUFF_ALIASES.get(best);
	}

	/** @return a known buff whose name contains {@code canonicalName} (so "might" finds Greater Might), else null. */
	public static Skill findKnown(List<Skill> known, String canonicalName)
	{
		for (Skill skill : known)
		{
			if (skill.getName().toLowerCase().contains(canonicalName))
			{
				return skill;
			}
		}
		return null;
	}

	/** @return {@code true} if a class is a magic user (caster), so it wants caster buffs, not physical ones. */
	public static boolean isCaster(Player player)
	{
		return player.getPlayerClass().isMage();
	}

	/**
	 * @return {@code true} if {@code caster} can pay this buff's item reagent (Spirit Ore for Greater Might /
	 *         Greater Shield / Clarity, etc.), or it needs none. Support phantoms are stocked with reagents at spawn
	 *         (see PhantomManager), but if the stock ever runs dry this lets the buff loop SKIP the unaffordable buff
	 *         instead of re-issuing a cast the engine will reject every tick (which froze the buffer on one buff).
	 */
	public static boolean canAffordReagent(Player caster, Skill buff)
	{
		final int reagentId = buff.getItemConsumeId();
		if (reagentId <= 0)
		{
			return true;
		}
		return caster.getInventory().getInventoryItemCount(reagentId, -1) >= buff.getItemConsumeCount();
	}

	/**
	 * Whether {@code skillId} is part of the curated core kit auto-maintained on a target of the given archetype.
	 * EVERY tier - self, member and leader alike - gets the same full class-appropriate kit now (a fighter the melee
	 * core, a caster the magic core, both Wind Walk); the split is purely caster-vs-fighter, keyed on
	 * {@code targetIsCaster}. Only this curated core is auto-maintained (so it fits the 20 buff-slot cap and never
	 * rotates); situational / consumable buffs (Greater Might/Shield, War/Earth Chant, Clarity, Greater Empower, extra
	 * resistances, ...) are left out here and cast on request only.
	 * @param skillId the buff the buffer knows
	 * @param targetIsCaster whether the target is a magic user (chooses the caster vs melee core)
	 * @param tier retained for the call sites' intent (self / member / leader); it no longer changes the kit - every
	 *            tier receives the full archetype core. Role-specific exceptions (e.g. Berserker off a tank) layer on
	 *            top via {@link #tankAvoids}, not here.
	 * @return {@code true} if this buff should be auto-maintained on that target
	 */
	public static boolean wanted(int skillId, boolean targetIsCaster, Tier tier)
	{
		return AUTO_COMMON.contains(skillId) || (targetIsCaster ? AUTO_CASTER.contains(skillId) : AUTO_MELEE.contains(skillId));
	}

	/**
	 * @return {@code true} if this buff should be withheld from a TANK target. Berserker Spirit is a power buff that
	 *         also lowers P.Def / M.Def, so it belongs on damage dealers, not the party's tank. Kept separate from
	 *         {@link #wanted} so the archetype whitelist stays about class (caster vs fighter) and role exceptions
	 *         layer on top.
	 */
	public static boolean tankAvoids(int skillId)
	{
		return BERSERKER_SPIRIT.contains(skillId);
	}

	/**
	 * Decides whether {@code buff} should be (re)cast on {@code target} right now, respecting the engine's abnormal
	 * stacking so a support never loops on a buff it cannot land. The presence test keys on the buff's abnormal
	 * SLOT, not its skill id (different buffer classes fill one slot with different skills), and it compares abnormal
	 * LEVELS so a stronger effect already in the slot is left alone instead of being re-cast forever - e.g. a P.Atk
	 * herb (Herb of Power, abnormalLevel 3) blocks a level-25 buffer's Might (abnormalLevel 2), so we skip Might
	 * until the herb fades rather than hammering a cast the engine keeps rejecting (the tester's "buffer casts the
	 * same buff over and over" bug). Rules:
	 * <ul>
	 * <li>slot empty -&gt; cast;</li>
	 * <li>slot held by a WEAKER effect (lower abnormal level) -&gt; cast, to upgrade it;</li>
	 * <li>slot held by a STRONGER effect (higher abnormal level) -&gt; skip, our cast would be rejected;</li>
	 * <li>slot held by an equal-level effect -&gt; recast only when it is about to expire ({@code <= refreshSeconds}).</li>
	 * </ul>
	 * A slotless buff ({@code abnormalType} NONE) stacks independently, so it keys on its own skill id as before.
	 * @param target the buff target
	 * @param buff the buff the support is considering
	 * @param refreshSeconds recast an equal-or-weaker slot when it has this many seconds or fewer left
	 * @return {@code true} if the buff should be cast now
	 */
	public static boolean needsBuff(Player target, Skill buff, int refreshSeconds)
	{
		final AbnormalType slot = buff.getAbnormalType();
		if ((slot == null) || slot.isNone())
		{
			// Slotless buff: independent stack, key on the skill's own id.
			final BuffInfo info = target.getEffectList().getBuffInfoBySkillId(buff.getId());
			return (info == null) || (info.getTime() <= refreshSeconds);
		}
		final BuffInfo info = target.getEffectList().getBuffInfoByAbnormalType(slot);
		if (info == null)
		{
			return true; // slot free
		}
		final int have = info.getSkill().getAbnormalLevel();
		final int want = buff.getAbnormalLevel();
		if (have > want)
		{
			return false; // a stronger effect holds the slot - our cast would be rejected; never loop on it
		}
		if (have < want)
		{
			return true; // upgrade the weaker effect in the slot
		}
		return info.getTime() <= refreshSeconds; // same strength: only refresh when it is about to drop
	}

	/**
	 * Applies the full archetype-appropriate buff kit to {@code target} directly (used to pre-buff a recruited
	 * member the moment it spawns, so a fresh party arrives already buffed). Each buff is applied at its max level;
	 * unknown ids are skipped. The buffs are real effects with normal durations - the party's buffer keeps them up
	 * afterwards.
	 */
	public static void applyFullBuffs(Player target, boolean isTank)
	{
		applyBuffs(target, PREBUFF_COMMON);
		if (isCaster(target))
		{
			applyBuffs(target, PREBUFF_CASTER);
		}
		else
		{
			applyBuffs(target, PREBUFF_MELEE);
			if (!isTank)
			{
				applyBuffs(target, PREBUFF_BERSERKER); // damage dealers get Berserker; a tank does not (it cuts defenses)
			}
		}
	}

	private static void applyBuffs(Player target, int[] ids)
	{
		for (int id : ids)
		{
			final int max = SkillData.getInstance().getMaxLevel(id);
			if (max <= 0)
			{
				continue;
			}
			final Skill skill = SkillData.getInstance().getSkill(id, max);
			if (skill != null)
			{
				skill.applyEffects(target, target);
			}
		}
	}

	// ===== Cross-caster buff reservations =====
	// Several independent bot buff sources can target the same player: the recruited-party supports
	// (PhantomPartyManager) and the personal support buddy (PhantomBuddyManager). A cast is not instant, so if two
	// of them both decide to cast the SAME buff inside the cast-time window, each sees "target not buffed yet" and
	// both cast, landing the buff twice (a double icon, and for some buffs a doubled stat). The shared, race-safe
	// PhantomBuffReservations registry makes the sources coordinate: a caster claims (target, skill) just before
	// casting; anyone else who finds it claimed by a different caster skips that buff. Claims auto-expire.
	private static final PhantomBuffReservations BUFF_RESERVATIONS = new PhantomBuffReservations();

	/**
	 * Attempts to claim a buff cast so no other bot double-casts the same buff on the same target. Call this
	 * immediately before {@code doCast}, once every other gate (range / MP / stand-up) has passed.
	 * @param targetObjectId the buff target's object id
	 * @param skillId the buff skill id
	 * @param casterObjectId the casting phantom's object id (a caster never blocks its own re-claim)
	 * @param holdMillis how long the claim is held (use {@link #buffHoldMillis(Skill)})
	 * @return {@code true} if this caster may cast now (claim taken/refreshed); {@code false} if a different bot is
	 *         already landing this exact buff on this target and the caller should skip it
	 */
	public static boolean reserveBuff(int targetObjectId, int skillId, int casterObjectId, int holdMillis)
	{
		return BUFF_RESERVATIONS.reserve(PhantomBuffReservations.key(targetObjectId, skillId), System.currentTimeMillis(), casterObjectId, holdMillis);
	}

	/** How long to hold a buff reservation: the skill's cast time plus a margin for the effect to actually land. */
	public static int buffHoldMillis(Skill buff)
	{
		return Math.max(2500, buff.getHitTime() + 1200);
	}

	// ===== Cross-caster resurrection claims =====
	// The same coordination problem as buffs, for Resurrection. A party can hold several rezzers at once - the
	// recruited-party healers/buffers (PhantomPartyManager) AND the personal support buddy (PhantomBuddyManager) -
	// and a res cast is not instant, so without coordination they would all fire on the same corpse (the tester's
	// "everyone spam-rezzes together"). A rezzer claims a corpse just before casting; anyone else that finds it
	// claimed by a DIFFERENT rezzer spreads onto another corpse (or waits a tick). Claims carry an expiry, and once
	// a cast lands the corpse's own isReviveRequested() flag becomes the universal skip, so nothing needs releasing.
	// A dedicated registry (not the buff one) keeps res claims cleanly separate from buff reservations.
	private static final PhantomBuffReservations RES_CLAIMS = new PhantomBuffReservations();
	private static final int RESURRECTION_ID = 1016;

	/**
	 * Read-only: is {@code corpse} already claimed for a res by a rezzer other than {@code askerObjectId}? Lets a
	 * rezzer pick a different corpse in the same tick instead of piling onto one another is already raising.
	 * @param corpseObjectId the fallen member's object id
	 * @param askerObjectId the rezzer asking (its own live claim does not count)
	 * @param now the current time in millis
	 * @return {@code true} if a different rezzer is already claiming this corpse
	 */
	public static boolean isResClaimed(int corpseObjectId, int askerObjectId, long now)
	{
		return RES_CLAIMS.isHeldByOther(PhantomBuffReservations.key(corpseObjectId, RESURRECTION_ID), now, askerObjectId);
	}

	/**
	 * Attempts to claim {@code corpse} for a res cast so no other bot double-casts Resurrection on it. Call this
	 * immediately before {@code doCast}, once every other gate (range / MP / stand-up) has passed.
	 * @param corpseObjectId the fallen member's object id
	 * @param casterObjectId the casting rezzer's object id (a rezzer never blocks its own re-claim)
	 * @param holdMillis how long the claim is held (long enough for the cast/revive-request to land)
	 * @return {@code true} if this rezzer may cast now; {@code false} if a different bot is already raising this corpse
	 */
	public static boolean claimRes(int corpseObjectId, int casterObjectId, int holdMillis)
	{
		return RES_CLAIMS.reserve(PhantomBuffReservations.key(corpseObjectId, RESURRECTION_ID), System.currentTimeMillis(), casterObjectId, holdMillis);
	}
}
