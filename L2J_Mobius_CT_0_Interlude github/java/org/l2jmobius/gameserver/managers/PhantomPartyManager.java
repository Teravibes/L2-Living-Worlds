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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.ai.Action;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.managers.PhantomManager.PartyRole;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.player.AutoUseSettingsHolder;
import org.l2jmobius.gameserver.model.actor.instance.Monster;
import org.l2jmobius.gameserver.model.events.Containers;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.player.inventory.OnPlayerItemAdd;
import org.l2jmobius.gameserver.model.events.holders.actor.player.inventory.OnPlayerItemTransfer;
import org.l2jmobius.gameserver.model.events.holders.actor.player.trade.OnPlayerTradeCancel;
import org.l2jmobius.gameserver.model.events.holders.actor.player.trade.OnPlayerTradeFinish;
import org.l2jmobius.gameserver.model.events.holders.actor.player.trade.OnPlayerTradeStart;
import org.l2jmobius.gameserver.model.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.groups.PartyDistributionType;
import org.l2jmobius.gameserver.model.groups.PartyMessageType;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.handler.ItemHandler;
import org.l2jmobius.gameserver.model.item.Weapon;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.item.type.WeaponType;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.skill.AbnormalType;
import org.l2jmobius.gameserver.model.skill.BuffInfo;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.targets.TargetType;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;

/**
 * Recruited combat-party brain.
 * <p>
 * When a real player shouts an LFM/LFP, {@link FakePlayerChatManager} parses the wanted roles and calls
 * {@link #recruitFromShout}. For each open slot this manager spawns a level-matched combat phantom of that role
 * (via {@link PhantomManager#spawnPartyMember}) a little way off, out of sight, then walks it to the player and
 * has it ask for an invite - so it reads as a real player who saw the shout and jogged over, not an NPC popping
 * into existence. The invite is auto-accepted server-side ({@code RequestJoinParty} -> {@link #onInvited}).
 * <p>
 * Once partied, a member follows the leader and (default) <b>assists</b> the leader's target - the party focus-
 * fires together; an "attack freely" order flips it to hunt nearby mobs on its own. Healers heal the hurt party
 * member and raise the dead; buffers keep the whole party buffed. Everything is rule-based and works with the
 * LLM brain offline; free-form orders fall through to the brain for a natural reply only when it is online.
 */
public class PhantomPartyManager
{
	private static final Logger LOGGER = Logger.getLogger(PhantomPartyManager.class.getName());

	/**
	 * Raid combat trace. When on, the manager logs (to the gameserver log) a periodic snapshot of every party with a
	 * raid boss engaged - boss HP and who it's hating, plus each member's role/HP/MP/action - and event lines for
	 * taunts, heals, res and deaths. Raid-only, so it's silent during normal farming. Toggle live with
	 * {@code //phantom debug on|off} (or the {@code //debug_on} / {@code //debug_off} shortcuts). Defaults off;
	 * turn it on for a raid session when a trace is needed, so a normal live server does not carry the log cost.
	 */
	public static volatile boolean DEBUG = false;
	private static final long RAID_LOG_INTERVAL = 1500; // throttle for the periodic snapshot
	private long _lastRaidLog;

	private static final long TICK_INTERVAL = 1000;
	// A clientless phantom's cast flag can stick set after a skill has actually finished. Past this window (longer than
	// any melee playstyle skill's cast/hit time) the flag is treated as stale so the member's auto-attack resumes
	// instead of standing idle "mid-cast" - the freeze a low-MP dagger showed when its only skill was gated out.
	private static final long CAST_FLAG_STALE_MS = 1500;
	private static final int MAX_PER_REQUEST = 8; // a full party (minus the leader) from one shout, slots permitting
	private static final int APPROACH_MIN = 700; // how far out a recruit spawns before walking in (close, so a raid party assembles fast)
	private static final int APPROACH_MAX = 1300;
	// Outside a peace zone (open field, dungeon) the long walk-in is fragile: a random far anchor can land in a wall,
	// off a ledge, or in another room, and a recruit jogging hundreds of units through mob packs aggros them or gets
	// stuck. There a recruit instead spawns right beside the leader (this offset band), on a geo-validated reachable
	// tile, and reports in on the spot. The charming walk-in is kept only in peace zones (towns), where it is safe and
	// other players can see it.
	private static final int ADJACENT_MIN = 60;
	private static final int ADJACENT_MAX = 130;
	private static final long ARRIVE_RANGE = 220; // close enough to the leader to say "here, inv me"
	private static final long RECRUIT_TIMEOUT = 150000; // give up + despawn if never invited within this window
	private static final long SPAWN_STAGGER = 500; // small gap so arrivals don't pop on one tile, but a full comp still assembles in a few seconds
	private static final int FOLLOW_RANGE = 250;
	// Formation follow: each member keeps a STABLE personal slot (own compass direction + distance) around the
	// leader and moves to that slot, instead of the engine FOLLOW intention aimed at the leader itself - which
	// walked every member to within 50-100 of the same point on the same 1s heartbeat, so they stacked on one tile
	// and lurched in perfect sync. The slot band tops out so a settled member still counts as "arrived" for the
	// invite check (max slot 110 + tolerance 60 <= ARRIVE_RANGE 220).
	private static final int FORMATION_MIN_DIST = 50;
	private static final int FORMATION_MAX_DIST = 110;
	private static final int FORMATION_TOLERANCE = 60; // settled when this close to the slot - don't micro-adjust
	private static final int FORMATION_REISSUE_DELTA = 50; // re-path only when the slot moved this far (leader ran on)
	private static final int MOVE_JITTER_MAX = 700; // per-member idle->move delay so the party doesn't lurch as one
	private static final int CATCHUP_TP_RANGE = 2000; // fallen this far behind the follow target: teleport to catch up
	private static final int LEASH_RANGE = 1400; // free-hunting member is pulled back if it strays this far
	private static final long TRAVEL_GRACE = 180000; // after a "go to X" order, wait at the spot this long for the leader
	private static final int REGROUP_RANGE = 1500; // ...resuming normal follow once the leader arrives within this
	private static final int SUPPORT_RANGE = 900; // heal/buff/res only when the target is this close
	private static final int ASSIST_MAX_RANGE = 2200; // don't assist a mob the leader targeted across the map
	private static final int DANGER_RANGE = 700;
	private static final int OWNER_HEAL_PERCENT = 60;
	private static final int SELF_HEAL_PERCENT = 45;
	private static final int RAID_HEAL_PERCENT = 80; // under a raid, heal party members pre-emptively at this HP% (boss spikes outrun reactive 60% healing)
	private static final int RAID_TANK_HEAL_PERCENT = 90; // ...and keep the tank topped this high, since it soaks the boss
	private static final int CRITICAL_HEAL_PERCENT = 50; // a member this low is an emergency - heal it before topping the tank
	private static final int BUFF_REFRESH_SECONDS = 20;
	private static final int CASTER_CAST_RANGE = 650; // a nuker walks IN to within this of the assist target so it nukes from range, never melees
	private static final int CASTER_RANGE_TOLERANCE = 150; // ...and stands and casts anywhere inside CAST_RANGE + this, never backing off as the mob closes
	private static final int CASTER_MIN_MP = 20; // below this percent a nuker stops casting and rests instead of meleeing
	private static final int CASTER_SPREAD_STEP = 110; // lateral spacing between casters so several don't stack and eat one AoE
	private static final int RAID_BACKLINE_RANGE = 720; // raid ranged/support lane: far enough to avoid melee AoE, close enough for heals
	private static final int RAID_BACKLINE_TOLERANCE = 140;
	private static final int RAID_SPREAD_STEP = 140;
	private static final int LOS_STEP = 150; // when a backline stand point can't see the boss, pull it this much closer and retry
	private static final int LOS_STEP_MIN = 200; // closest a ranged member is pulled in to regain line of sight (still outside melee)
	// Archers must hold INSIDE bow reach (the generic 720 backline is past a bow's ~500 range, so they could never fire
	// from it - the big ranged DPS bug). Hold range is derived from the bow's actual reach; the spread is tighter than
	// the caster lane so the outer lanes (radial + lateral) still land within reach.
	private static final int ARCHER_RANGE_MARGIN = 80; // hold this far inside the bow's reach so it actually shoots
	private static final int ARCHER_BACKLINE_TOLERANCE = 110;
	private static final int ARCHER_SPREAD_STEP = 70;
	private static final int MP_REST_SIT = 30; // a caster sits to recover when it drops to ~this and is safe
	private static final int MP_REST_STAND = 100; // and stays seated until MP is fully restored
	private static final long STAND_SUPPRESS = 30000; // a "stand" order keeps it on its feet this long before auto-rest resumes
	private static final long OFFLINE_GRACE = 120000;
	private static final long BRB_GRACE = 360000;
	private static final long CORPSE_GRACE = 60000; // keep a fallen member as a raisable corpse this long before despawning it
	private static final int RES_CLAIM_MS = 4500; // one rezzer claims a corpse long enough for the cast/request to land
	private static final long POST_RES_HOLD_MS = 6500; // after a tank battle-res, hold DPS while it is healed and regains hate
	private static final int POST_RES_TANK_READY_PERCENT = 85;
	private static final int RES_SKILL_ID = 1016; // Resurrection (granted to healers on spawn)
	private static final int RES_SCROLL_ID = 737; // Scroll of Resurrection: the item a recruit carries as a fallback rez
	private static final int RES_SCROLL_CAST_RANGE = 400; // skill 2014's cast range - a scroll-rezzer closes to this first
	private static final int RES_SCROLL_CLAIM_MS = 16000; // hold the corpse claim across the scroll's long (~15s) cast so no second scroll is burned on it
	private static final int AGGRESSION_ID = 28; // single-target taunt (knight tree) - "provokes a target to attack"
	private static final int AURA_OF_HATE_ID = 18; // AoE taunt (knight tree) - "provokes nearby enemies to attack"
	private static final int THREAT_SCAN_RANGE = 1000; // how far a tank looks for a mob loose on a squishy party member
	private static final long TAUNT_REFRESH_MS = 6000; // while already top of aggro, only re-taunt this often (its auto-attacks hold hate; spamming taunt just drains MP)
	private static final long TAUNT_MIN_INTERVAL = 1500; // floor between ANY two taunts, so the tank auto-attacks in between instead of taunt-spamming and standing idle (the post-res behaviour)
	private static final int RECHARGE_ID = 1013; // Recharge - an Elder/Shillien Elder refills a party caster's MP
	private static final int RECHARGE_MP_PERCENT = 45; // recharge a mana-user below this MP%
	private static final int RECHARGE_RAID_MP_PERCENT = 65; // raid support starts battery work before healers are already dry
	private static final long CAST_WATCHDOG_FLOOR_MS = 6000; // never abort a cast sooner than this (covers every fast cast)
	private static final long CAST_WATCHDOG_MARGIN_MS = 2000; // slack over a slow cast's own duration before it counts as wedged
	// Raid tactics: kill-order, AoE step-out and debuff cleanse (all raid-gated, so normal farming is unaffected).
	private static final int AOE_MIN_RADIUS = 150; // treat a raid cast as "AoE" if its affect radius is at least this (also catches splash on single-target skills)
	private static final int AOE_STEP_MARGIN = 250; // ...and step this far PAST the blast radius when getting clear
	private static final int PURIFY_ID = 1018; // dispels poison / bleed / paralyze / petrify
	private static final int CURE_POISON_ID = 1012;
	private static final int CURE_BLEEDING_ID = 61;
	// If a raid's tank corpse expires unraised (or the party simply has none), mayAttackRaid held every non-tank
	// member forever with no visible way out but the player guessing "all attack". Fail open after this long instead.
	private static final long NO_TANK_FAILOPEN_MS = 20000;
	// Execute rule: a raid minion in its last sliver may be finished by ANYONE - killing it removes a raid mob
	// from the fight, which outweighs any hate-discipline argument at that HP. (Second Ruell attempt: a Wind add
	// sat at 0-1% HP for two minutes, still swinging, while the gated DPS line watched it.)
	private static final int MINION_EXECUTE_PERCENT = 10;

	// Class competence (party plan Bucket 2): songs/dances, archer discipline, tank panic button, dagger
	// positioning, DPS aggro-easing, nuker crowd control.
	// SWS/BD keep their FULL learned song/dance kit running (the whole point of bringing one - and legitimate to
	// maintain mid-raid, unlike 20-minute buffs). Unlike the 20-minute buffs (which the buffer tops up a little
	// before they lapse), a music class re-sings ONLY once its own copy of the effect has fully expired - it lets
	// the cycle die and then rebuilds it, so it is not paying to refresh a song that still has value. The rotation
	// is no longer pinned to a fixed 3: songs and dances share the target's 12-slot music pool (MaxDanceAmount), so
	// resolveSongs caps a member to its SHARE of that pool (see songRotationCap) - a SwS and a BD in the same party
	// split it instead of evicting each other's music in an endless re-cast loop.
	private static final int MUSIC_POOL_SLOTS = 12; // MaxDanceAmount: songs + dances share one 12-slot pool per target
	// All Interlude songs are party buffs, so the singer auto-maintains every one it has learned (best-value first;
	// resolveSongs filters to what it actually knows). A specific song can also be asked for by name (requestedSong).
	private static final int[] SINGER_SONGS =
	{
		269, // Song of Hunter (crit rate)
		264, // Song of Earth (p.def)
		304, // Song of Vitality (max HP)
		267, // Song of Warding (m.def)
		268, // Song of Wind (speed)
		305, // Song of Vengeance (damage reflect)
		270, // Song of Invocation (max MP)
		265, // Song of Life (HP regen)
		306, // Song of Flame Guard (fire resist)
		308, // Song of Storm Guard (wind resist)
		266, // Song of Water (water resist)
		349, // Song of Renewal (shorter re-use/cast penalties)
		363, // Song of Meditation (MP regen)
		364 // Song of Champion (P.Atk / Atk. speed)
	};
	// Bladedancer dances, melee-comp order. Party-beneficial dances only: the self-only Siren's Dance (365) and
	// Dance of Shadows (366, silent-move), and the offensive Dance of Medusa (367, enemy petrify), are left OUT of
	// the auto rotation - they are still castable on an explicit by-name request (requestedSong).
	private static final int[] DANCER_DANCES =
	{
		275, // Dance of Fury (atk. speed)
		271, // Dance of the Warrior (p.atk)
		274, // Dance of Fire (crit damage)
		310, // Dance of the Vampire (HP drain on hit)
		276, // Dance of Concentration (cast speed)
		273, // Dance of the Mystic (m.atk)
		272, // Dance of Inspiration (bow power)
		277, // Dance of Light
		311, // Dance of Protection
		307, // Dance of Aqua Guard (water resist)
		309 // Dance of Earth Guard (earth resist)
	};
	private static final int[] DANCER_DANCES_MAGE = // mage-heavy comp: feed the casters first
	{
		273, // Dance of the Mystic (m.atk)
		276, // Dance of Concentration (cast speed)
		275, // Dance of Fury (atk. speed)
		271, // Dance of the Warrior (p.atk)
		274, // Dance of Fire (crit damage)
		310, // Dance of the Vampire (HP drain on hit)
		272, // Dance of Inspiration (bow power)
		277, // Dance of Light
		311, // Dance of Protection
		307, // Dance of Aqua Guard (water resist)
		309 // Dance of Earth Guard (earth resist)
	};
	// TANK panic button: Ultimate Defense, hand-cast at low HP while still being hit - instead of the AutoUse
	// auto-buff loop burning it the moment it's off cooldown (it's a SELF-target buff, so registerAutoSkills
	// puts it in the auto-buff list; recruits get it parked out of there and cast by the manager).
	private static final int UD_SKILL_ID = 110; // Ultimate Defense (knight tree)
	private static final int UD_HP_PERCENT = 30;
	// Under multi-raid burst the 1s tick can't catch 30% in time (the Ruell tank went 44% -> 5% between two
	// ticks); with two or more raid mobs on the tank the panic button fires this early instead.
	private static final int UD_HP_BURST_PERCENT = 50;
	// ARCHER skill discipline: below the park mark it stops firing skills and plinks with plain soulshotted
	// auto-shots (which still cost a small per-shot bow MP - see manageArcherMp), resuming skills once MP recovers
	// - instead of dumping every skill off cooldown
	// straight into an empty bar. Plus a short human beat before swinging onto the leader's NEW target.
	private static final int ARCHER_SKILL_PARK_MP = 30;
	private static final int ARCHER_SKILL_RESUME_MP = 55;
	private static final int BOW_REST_RESUME_SHOTS = 4; // resume firing once this many bow shots' worth of MP is back
	private static final long ASSIST_SWITCH_MIN = 400;
	private static final long ASSIST_SWITCH_MAX = 1200;
	// DPS aggro-easing (raid only): a DPS that rips the boss off the tank holds fire briefly so the taunt can
	// land, rather than dragging the boss around the room by its face. Re-arm floor stops a hold-resume stutter.
	private static final long AGGRO_EASE_MS = 4000;
	private static final long AGGRO_EASE_REARM_MS = 8000;
	// DAGGER rear positioning: slide behind a target that is busy with someone else (backstab arc), face-fight
	// only when the mob turns on the dagger itself.
	private static final int REAR_ATTACK_DISTANCE = 45;
	private static final int REAR_TOLERANCE = 60;
	private static final long REAR_MOVE_GRACE = 1200; // let a rear reposition walk before re-evaluating
	// NUKER crowd control: a loose add beating on a squishy gets slept/rooted before the nuker resumes its DPS.
	private static final int SLEEP_ID = 1069; // Sleep (human/elf/DE nuker 2nd classes)
	private static final int DRYAD_ROOT_ID = 1201; // Dryad Root (fallback where known)
	private static final int STUNNING_SHOT_ID = 101; // archer peel: stun what closed on us (learned from 36)
	private static final int HAMSTRING_SHOT_ID = 354; // ...or slow it, so we can walk away (2nd class up)
	private static final int CC_SCAN_RANGE = 900;
	// ===== Ranged self-preservation (ARCHER / NUKER peel) =====
	// A ranged DD that a mob has latched onto does NOT abandon the party's kill target - focus fire is how a party
	// kills anything. It breaks contact instead: control the attacker, back off to its own range, keep shooting the
	// assist target. Only when that is clearly failing does survival outrank damage and it turns on the attacker.
	private static final int PEEL_SELF_HP_PERCENT = 55; // below this, stop kiting and kill what's chewing on us
	private static final int PEEL_EXECUTE_PERCENT = 15; // ...or just finish the attacker if it's nearly dead anyway
	private static final int PEEL_CONTROL_COOLDOWN_MS = 9000; // floor between peel control casts, so it isn't a stun-lock
	private static final int PEEL_STEP_BACK = 250; // how far to disengage past the attacker's reach when kiting
	private static final int PEEL_MOVE_GRACE = 1500; // let a kite step finish before issuing another
	private static final int KITE_STEP_MAX = 250; // a peel step opens the gap by at most this from the CURRENT spot (re-evaluated next tick), so it never snaps onto a distant 650/900 ring
	private static final int KITE_MAX_FROM_PARTY = 1100; // ...and never kites past this from the leader - staying inside the group's cleared bubble instead of backing into fresh spawns
	private static final int PROTECTOR_REACHED_RANGE = 150; // a support that is this close to its protector has arrived
	// The disable a SUPPORT peels with, best first. Sleep is the cleanest (the mob stops entirely), Dryad Root and
	// Trance are the elf/human alternatives, Dreaming Spirit is the Orc-shaman-line sleep.
	private static final int[] SUPPORT_CONTROL_PRIORITY =
	{
		SLEEP_ID,
		1394, // Trance (Eva's Saint / Cardinal line)
		1097, // Dreaming Spirit (Warcryer / Overlord line)
		DRYAD_ROOT_ID
	};
	private static final long CC_COOLDOWN_MS = 15000; // floor between two CC casts by the same nuker

	// Camp-and-pull (party plan Bucket 3): "camp here" plants a fixed anchor at the leader's feet; the party holds
	// there, rests between pulls, and fights only what is brought in. "<name> pull [N]" names the puller, which
	// fetches up to N free mobs back to the camp. Lets the player park a self-running kill camp (and still swing in
	// if they want). Any follow/assist/move order breaks camp.
	private static final int CAMP_ENGAGE_RANGE = 700; // a mob this close to the anchor is "at the camp" - fought here
	private static final int PULL_SEARCH_RANGE = 1400; // how far the puller ranges from the anchor to find a mob
	private static final int PULL_GRAB_RANGE = 250; // a multi-pull only chains an extra mob this close to the puller
	private static final long PULL_TIMEOUT = 12000; // abort a pull run that can't deliver in this long (mob stuck/unreachable)
	private static final long PULL_INTERVAL = 4000; // rest beat between one pull dying/arriving and the next fetch
	private static final int PULL_RETREAT_HP = 40; // a puller this hurt recovers at camp before heading out again
	private static final int MAX_PULL_SIZE = 3; // "pull 3" cap - a puller drags at most this many at once

	// "Human touches": level spread, order latency, event barks (proactive party chat), emotes and small talk.
	// A recruit matching the leader spawns at the leader's level +/- this (a real pickup group is never 8 identical
	// levels). An explicit "lfm buffer lvl 57" request stays exact. The level spread is why the raid-curse guard
	// (see RAID_CURSE_LEVEL_GAP / mayAttackRaid) must check EACH member's level against the boss, not the leader's.
	private static final int RECRUIT_LEVEL_SPREAD = 3;
	// Raid-curse guard: the boss curses (paralyses + heavy DoT) any attacker whose level exceeds the boss's by MORE
	// than this. The core check is per attacker (Creature: getLevel() > raid.getLevel() + 8), so a level-spread party
	// can carry one member high enough to be cursed while the leader is fine - that member must never touch the boss.
	private static final int RAID_CURSE_LEVEL_GAP = 8;
	// A visible order (follow / stay / stand / tp) lands after a human-ish beat instead of the same instant it was
	// typed. State-flag orders (assist/free, pull, brb...) already take effect on the next 1s tick, so they aren't delayed.
	private static final long ORDER_DELAY_MIN = 400;
	private static final long ORDER_DELAY_MAX = 1100;
	// Bark throttles: a member speaks up on its own at most this often, and the whole party keeps a floor between
	// ANY two barks - eight bots must never turn party chat into a firehose. Life-cycle replies (answering the
	// player) are not barks and bypass these.
	private static final long BARK_MEMBER_COOLDOWN = 45000;
	private static final long BARK_PARTY_COOLDOWN = 10000;
	private static final int LOW_HP_BARK_PERCENT = 30; // "getting low here" - once per drop below this...
	private static final int LOW_HP_BARK_CLEAR = 60; // ...re-armed only after recovering above this
	private static final long OVERWHELMED_BARK_COOLDOWN = 30000; // tank "can't hold them all" at most this often
	private static final long SMALLTALK_MIN_MS = 7 * 60000; // downtime-only spontaneous party chat, one random member,
	private static final long SMALLTALK_MAX_MS = 18 * 60000; // same cadence the personal buddies use
	// Player social-action ids (see RequestActionUse): the visible emotes a clientless member can broadcast.
	private static final int SOCIAL_GREETING = 2;
	private static final int SOCIAL_VICTORY = 3;
	private static final int SOCIAL_BOW = 7;
	private static final int SOCIAL_APPLAUD = 11;
	private static final int SOCIAL_DANCE = 12;

	// LLM brain bridge (optional): a free-form whisper/party-chat line that isn't a recognised command is sent
	// here for a natural reply that may carry an action tag, executed and stripped before the member speaks.
	private static final HttpClient BRAIN_HTTP = HttpClient.newHttpClient();
	private static final String BRAIN_URL = "http://127.0.0.1:5000/chat";
	// Outlast a slow local Ollama model (~30s); the old 20s cap silently dropped every reply. See the same
	// constant in FakePlayerChatManager.
	private static final int BRAIN_TIMEOUT_SECONDS = 45;
	// Closing bracket is tolerant ([\]\)]{1,2}) because local Ollama models sometimes emit a malformed close
	// (e.g. "[[TP:roa)"); a strict "\]\]" would neither act on the tag nor strip it, leaking it into player chat.
	private static final Pattern TAG_ASSIST = Pattern.compile("\\[\\[\\s*ASSIST\\s*[\\]\\)]{1,2}", Pattern.CASE_INSENSITIVE);
	private static final Pattern TAG_FREE = Pattern.compile("\\[\\[\\s*FREE\\s*[\\]\\)]{1,2}", Pattern.CASE_INSENSITIVE);
	private static final Pattern TAG_FOLLOW = Pattern.compile("\\[\\[\\s*(FOLLOW|GATHER)\\s*[\\]\\)]{1,2}", Pattern.CASE_INSENSITIVE);
	private static final Pattern TAG_STAY = Pattern.compile("\\[\\[\\s*(STAY|HOLD)\\s*[\\]\\)]{1,2}", Pattern.CASE_INSENSITIVE);
	private static final Pattern TAG_DISBAND = Pattern.compile("\\[\\[\\s*DISBAND\\s*[\\]\\)]{1,2}", Pattern.CASE_INSENSITIVE);
	private static final Pattern TAG_TP = Pattern.compile("\\[\\[\\s*TP\\s*:\\s*([^\\]]+?)\\s*[\\]\\)]{1,2}", Pattern.CASE_INSENSITIVE);
	private static final Pattern TAG_GRACE = Pattern.compile("\\[\\[\\s*GRACE\\s*:\\s*(\\d+)\\s*[\\]\\)]{1,2}", Pattern.CASE_INSENSITIVE);
	private static final Pattern ANY_TAG = Pattern.compile("\\[\\[[^\\]]*[\\]\\)]{1,2}");

	// FAST heals (2s cast) - the reactive workhorse for emergencies and combat spikes, where cast SPEED beats raw
	// power (a slow heal can land after the target is already dead). Greater Battle Heal is exactly what defines a
	// Bishop as a healer - fast spam healing - so it's the primary here, with base Battle Heal behind it. The slow
	// (5s) big heals are deliberately kept OFF this path.
	private static final int[] FAST_HEAL_PRIORITY =
	{
		1218, // Greater Battle Heal (2s cast) - the primary reactive heal
		1015 // Battle Heal (2s cast)
	};

	// STRONG heals for a big but non-critical gap, where there's time to cast (5s). Greater Battle Heal still leads
	// (a Bishop's fast casting makes it the default even here); Major Heal follows as the MP-smart option - it heals
	// as much as Greater Battle Heal for less than half the MP (~48 vs ~108) at the cost of cast time + 1 Spirit Ore,
	// so it doubles as the graceful fallback when the healer is low on MP; Greater Heal (5s, +HP-regen HoT) is last.
	private static final int[] STRONG_HEAL_PRIORITY =
	{
		1218, // Greater Battle Heal
		1401, // Major Heal (Bishop/Cardinal 56+, +1 Spirit Ore) - big heal, very MP-cheap; also the low-MP fallback
		1217 // Greater Heal (+HP-regen HoT)
	};

	// Party/group heals a healer may know, strongest first. Cast only when SEVERAL members are hurt at once - these
	// are slow (7s cast) with a long reuse (25s for 1027/1219), so they're a pre-emptive AoE-damage answer, not a
	// reactive one; near-death members are covered by the fast emergency heal first. Major Group Heal eats Spirit Ore
	// (the reagent guard steps down to a free group heal when the stock runs dry).
	private static final int[] GROUP_HEAL_PRIORITY =
	{
		1402, // Major Group Heal (Bishop 58+, +4 Spirit Ore)
		1219, // Greater Group Heal (Bishop 40+)
		1027 // Group Heal (Cleric)
	};
	private static final int GROUP_HEAL_HP_PERCENT = 75; // a member below this HP% counts as "hurt" for a group heal
	private static final int GROUP_HEAL_MIN_TARGETS = 3; // only worth a slow group heal when at least this many are hurt at once

	// Cheap heals for small top-offs, weakest first - so a 15% tank top-off doesn't burn a Greater Battle Heal.
	private static final int[] LIGHT_HEAL_PRIORITY =
	{
		1011, // Heal
		1015 // Battle Heal
	};
	private static final int BIG_HEAL_DEFICIT = 35; // missing HP% at/above which the strong heal is worth its MP
    /** AOE radius for phantoms to check for mobs with their respective spoiledByObjectId */
    private static final int DEFAULT_AOE_SWEEP_RADIUS = 600;
    private static final int SPOIL_SKILL_ID = 254;
    private static final int SWEEPER_SKILL_ID = 42;

	/** Per-member state. */
	private static class Member
	{
		final Player npc;
		final PartyRole role;
		Player owner; // the recruiter; the party bond forms when the invite is accepted
		boolean partied;
		long deadSince; // 0 while alive; set when first seen dead so the corpse persists for a battle-res window
		boolean pullOrdered; // TANK only: the leader ordered the tank to initiate a raid pull ("tank attack")
		long pullSince; // when that order was given - release the rest of the party shortly after even if aggro reads flaky
		boolean assist = true; // assist the leader's target (default) vs. free-hunt
		boolean following = true;
		boolean reminded; // already whispered "here, inv me" while waiting
		boolean rezOnArrival; // summoned to a dead solo player: self-invite on arrival (a corpse can't answer /invite) so the rez lands at once
		long pendingSince; // spawn time; despawn if never invited within RECRUIT_TIMEOUT
		long graceUntil;
		List<Skill> buffs; // lazy
		List<Skill> fastHealKit; // lazy (known 2s-cast heals, best first); emergencyHeal() picks the strongest affordable
		boolean fastHealLookedUp;
		List<Skill> strongHealKit; // lazy (known big heals for non-critical gaps, best first)
		boolean strongHealLookedUp;
		List<Skill> groupHealKit; // lazy (all known party/group heals, strongest first)
		boolean groupHealLookedUp;
		Skill lightHeal; // lazy (cheapest known heal, for small top-offs)
		boolean lightHealLookedUp;
		Skill res; // lazy (any class that naturally learned Resurrection, not just the HEALER role)
		boolean resLookedUp; // whether the res skill has been resolved from the known list yet
		Skill aggression; // lazy (TANK): single-target taunt
		Skill auraOfHate; // lazy (TANK): AoE taunt
		boolean tauntLookedUp; // whether the two taunt skills have been resolved from the known list yet
		long lastTauntAt; // TANK: when it last taunted, so it doesn't re-taunt every tick and drain its own MP
		Skill recharge; // lazy (support): Recharge, to refill party casters' MP
		boolean rechargeLookedUp;
		Skill cleanse; // lazy (support): a debuff strip (Purify / Cure Poison / Cure Bleeding)
		Set<AbnormalType> cleansable; // which debuffs that cleanse can actually remove, so it isn't cast in vain
		boolean cleanseLookedUp;
		int lastX; // follow-stuck watchdog
		int lastY;
		int stuckTicks;
		long castSince; // abort a wedged cast instead of freezing the member
		long travelUntil; // sent ahead to a destination; wait there (don't follow-yank back) until the leader arrives
		boolean rebuffing; // "rebuff" order: recast the full kit regardless of time left
		int rebuffIdx; // which buff in the list is next for the current target
		List<Player> rebuffQueue; // targets still owed a full kit (leader only, a named member, or the whole party)
		boolean healNow; // "heal me" order: heal the leader once even at full HP
		Skill pendingBuff; // "give me X" / "X on <name>" order: a specific buff to cast next tick
		Player pendingBuffTarget; // who that on-demand buff goes on (the leader, or a named party member)
		long noSitUntil; // "stand" order: don't auto-sit for MP until this time (so it doesn't pop straight back down)
		long recoveryUntil; // after a battle-res, especially the tank, pause DPS until it is stable again
		long lastBarkAt; // proactive-chat throttle: when this member last spoke up on its own
		boolean lowHpBarked; // said "getting low" for the current HP dip; re-armed on recovery
		boolean oomBarked; // said "oom, sec" for the current MP rest; re-armed on standing
		final double formAngle; // formation slot: this member's own compass direction from the follow target...
		final int formDist; // ...and its own distance, so members spread instead of stacking on the leader's tile
		final int moveJitter; // personal idle->move delay (ms) so members don't all start moving the same instant
		long moveDelayUntil; // the pending start-up stagger; 0 when none
		int lastDestX; // last formation destination actually issued, to avoid re-pathing to the same spot
		int lastDestY;
		List<Skill> songs; // lazy (SINGER/DANCER): the full learned song/dance kit this member keeps running (capped to its music-pool share)
		boolean songsLookedUp;
		boolean songsRequested; // explicit "sing"/"dance" order: run the rotation now even out of combat
		Skill pendingSong; // explicit "<song/dance> by name" order (SINGER/DANCER): cast that exact one next tick, even if it's outside the auto rotation
		SpoilBehavior spoilBehavior;
		Skill spoil;
		Skill sweeper;
		boolean scavengerKitLookedUp;
		boolean fieldSweepingInProcess;
		Monster nextMonsterCorpseToSweep;
		List<Monster> targetsToSweep;
		Map<Integer, Integer> lootingSessionItems; // Map of Item.getObjectId() to Item.getCount()
		TradingState tradingState = TradingState.NOT_TRADING;
		Skill survival; // lazy (TANK): Ultimate Defense, hand-cast at low HP (parked out of the auto-buff loop)
		boolean survivalLookedUp;
		Skill cc; // lazy (NUKER): Sleep / Dryad Root for a loose add
		boolean ccLookedUp;
		long lastCcAt; // NUKER: floor between CC casts
		List<Integer> parkedAutoSkills; // ARCHER: auto-skills parked while MP-saving (plain shots only)
		boolean bowResting; // ARCHER: holding fire to recover MP because a bow shot's MP cost can't be met (hysteresis)
		int lastAssistTargetId; // ARCHER: the assist target it last committed to...
		long assistSwitchGate; // ...and the human beat before swinging onto a new one
		long easeUntil; // DPS: holding fire after ripping raid aggro off the tank
		long lastEaseAt; // ...and when it last did, so it doesn't stutter hold-resume-hold
		int positionTicks; // consecutive raid-backline reposition ticks; capped so positioning can never block firing
		long rearMoveAt; // DAGGER: when it last started sliding behind the target
		int rearTargetId; // ...which target that was, so the give-up counter resets on a new fight
		int rearTries; // blocked rear attempts on the current target; past a few, fight from the front
		long castStuckSince; // engageFocus: when this member first looked idle-but-still-"casting" at the attack re-issue, so a lingering clientless cast flag can be cleared once it outlives any real cast (0 = not watching)
		final PhantomPlaystyleEngine.PlayState play = new PhantomPlaystyleEngine.PlayState(); // per-class combat playstyle runtime (entries resolve lazily)
		long lastPeelControlAt; // ARCHER/NUKER: when it last stunned/slowed something that was on it
		long peelMoveAt; // ...and when it last stepped away from it, so kite moves aren't re-issued every tick
		Skill peelControl; // lazy: the control skill it peels with (Stunning Shot / Sleep / Root ...)
		boolean peelControlLookedUp;
		String raidGateReason; // DEBUG: the reason code the raid engage gate (mayAttackRaid) last recorded for this member
		String raidGateLogged; // DEBUG: the reason last written to the log, so a steady state is logged once, not every tick

		Member(Player npc, PartyRole role)
		{
			this.npc = npc;
			this.role = role;
			formAngle = Rnd.nextDouble() * 2 * Math.PI;
			formDist = Rnd.get(FORMATION_MIN_DIST, FORMATION_MAX_DIST);
			moveJitter = Rnd.get(MOVE_JITTER_MAX);
		}

		boolean isSupport()
		{
			return role.isSupport();
		}

        public void addItemsToLootingSession(Item item)
		{
            if (lootingSessionItems == null)
			{
				lootingSessionItems = new ConcurrentHashMap<>();
			}
			lootingSessionItems.put(item.getObjectId(), item.getCount());
        }

        public void removeItemsFromLootingSession(Item item)
		{
            if (lootingSessionItems == null || lootingSessionItems.isEmpty() || !lootingSessionItems.containsKey(item.getObjectId()))
			{
				return;
			}
			if (item.getCount() == 0)
			{
				lootingSessionItems.remove(item.getObjectId());
			}
			else
			{
				lootingSessionItems.put(item.getObjectId(), item.getCount());
			}
        }
	}

	private enum SpoilBehavior {
		SPOIL_ON_ASSIST;
	}

	/**
	 * Per-party (per owner) social state driving the proactive "human touches": the party-wide bark throttle, the
	 * leader level/death transitions ("gz!" / "omg"), the engaged-raid handle for the victory celebration, and the
	 * downtime small-talk timer. Created lazily on the first tick a party exists; dropped when its members are gone.
	 */
	private static class PartyMood
	{
		long lastBarkAt; // party-wide floor between any two barks
		int leaderLevel; // last seen leader level, to catch a level-up
		boolean leaderDead; // last seen leader life state, to react once per death
		Monster engagedRaid; // the raid this party is fighting; checked for isDead() = victory (cleared when the fight ends)
		boolean wipeCalled; // said "this is lost, pull out" for the current raid fight; re-armed when it ends
		long nextSmallTalkAt;
		long overwhelmedBarkAt; // tank "can't hold them all" throttle
	}

	/**
	 * Per-party (per owner) camp-and-pull state. Present in {@link #_camps} only while the party is camping; the
	 * anchor is the leader's position when "camp" was ordered. One member (the {@code pullerId}) fetches mobs back.
	 */
	private static class Camp
	{
		final Location anchor; // fixed camp spot (the leader's position when camp was called)
		int pullSize = 1; // "pull N" - how many mobs the puller drags at once
		int pullerId; // objectId of the designated puller (0 = passive camp: hold and fight only what wanders in)
		Monster pulling; // the mob currently being fetched/dragged home (null between runs)
		boolean hauling; // true while running the load home, so the MOVE_TO isn't re-issued every tick (path stutter)
		boolean pullTagged; // the current run's ranged opener (tank taunt / nuker tag) has fired - at most once per pull
		long pullStartedAt; // when the current fetch began, for the stuck/timeout guard
		long nextPullAt; // don't launch the next fetch until this time (the rest beat between pulls)

		Camp(Location anchor)
		{
			this.anchor = anchor;
		}
	}

	private enum TradingState
	{
		NOT_TRADING,
		IS_TRADING,
		FINISHED,
		CANCELLED;
	}

	private final ConcurrentHashMap<Integer, Member> _members = new ConcurrentHashMap<>();
	private CopyOnWriteArrayList<Member> _pendingTrades = new CopyOnWriteArrayList<>();
	private Member _currentTrading;
	private final ConcurrentHashMap<Integer, PartyMood> _moods = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Integer, Camp> _camps = new ConcurrentHashMap<>();
	// Raid pull control: "all attack" force-releases an owner's party, but normal tank release is per raid mob/add.
	// The old whole-fight release let archers swap from a tank-owned boss to an untanked Bat/Inferior and pull hate.
	private final Set<Integer> _released = ConcurrentHashMap.newKeySet();
	private final Set<Long> _releasedRaidTargets = ConcurrentHashMap.newKeySet();
	// ownerId -> first tick a raid check found no living tank. Cleared once a tank exists again or the raid
	// gate resets (clearRaidRelease); see NO_TANK_FAILOPEN_MS.
	private final ConcurrentHashMap<Integer, Long> _noTankSince = new ConcurrentHashMap<>();
	// DEBUG only: ownerIds whose current raid engagement has already printed its one-time "ENGAGE START" banner, so a
	// fresh pull banners once instead of every snapshot. Pruned when the party is no longer engaged with a raid.
	private final Set<Integer> _raidTraceBannered = ConcurrentHashMap.newKeySet();
	// Targets a support has already committed a heal to THIS tick. Cleared each tick. Stops two healers (processed in
	// the same tick pass) both landing a big heal on the same target before either is flagged "casting" - the in-game
	// double/triple overheal that burned the healers' MP twice as fast and ended raids in a mass-OOM wipe.
	private final Set<Integer> _healedThisTick = ConcurrentHashMap.newKeySet();
	private boolean _ticking = false;

	private final ConsumerEventListener addItemEventListener =
			new ConsumerEventListener(
					Containers.Players(),
					EventType.ON_PLAYER_ITEM_ADD,
					(OnPlayerItemAdd event) -> onPlayerItemAdd(event),
					this
			);

	private final ConsumerEventListener transferItemEventListener =
			new ConsumerEventListener(
					Containers.Players(),
					EventType.ON_PLAYER_ITEM_TRANSFER,
					(OnPlayerItemTransfer event) -> onPlayerItemTransfer(event),
					this
			);

	private final ConsumerEventListener onPlayerTradeStartListener =
			new ConsumerEventListener(
					Containers.Players(),
					EventType.ON_PLAYER_TRADE_START,
					(OnPlayerTradeStart event) -> onPlayerTradeStart(event),
					this
			);

	private final ConsumerEventListener onPlayerTradeFinishListener =
			new ConsumerEventListener(
					Containers.Players(),
					EventType.ON_PLAYER_TRADE_FINISH,
					(OnPlayerTradeFinish event) -> onPlayerTradeFinish(event),
					this
			);

	private final ConsumerEventListener onPlayerTradeCancelListener =
			new ConsumerEventListener(
					Containers.Players(),
					EventType.ON_PLAYER_TRADE_CANCEL,
					(OnPlayerTradeCancel event) -> onPlayerTradeCancel(event),
					this
			);

	protected PhantomPartyManager()
	{
		Containers.Players().addListener(addItemEventListener);
		Containers.Players().addListener(transferItemEventListener);
		Containers.Players().addListener(onPlayerTradeStartListener);
		Containers.Players().addListener(onPlayerTradeFinishListener);
		Containers.Players().addListener(onPlayerTradeCancelListener);
	}

	// ===== Recruitment =====

	/**
	 * Spawns and walks in one level-matched member per wanted role, up to the party's free slots. Staggered so a
	 * multi-role request trickles in like separate people answering the shout.
	 */
	public void recruitFromShout(Player leader, List<PhantomManager.Recruit> recruits, int level)
	{
		if ((leader == null) || (recruits == null) || recruits.isEmpty())
		{
			return;
		}
		int slots = freeSlots(leader);
		if (slots <= 0)
		{
			leader.sendPacket(new CreatureSay(leader, ChatType.WHISPER, "Party", "your party is full"));
			return;
		}
		// Optional requested level (e.g. "lfm buffer lvl 57"), clamped and honoured exactly; otherwise each member
		// rolls its own level near the recruiter's (a real pickup group is never eight identical levels).
		final boolean explicitLevel = level > 0;
		final int requestedLevel = explicitLevel ? Math.max(1, Math.min(80, level)) : leader.getLevel();
		// A solo player who shouts for support while lying dead can't complete the normal "invite me" handshake (a
		// corpse has nobody to target and /invite). Capture that here so the arriving recruit self-invites and rezzes
		// on the spot. Only the summon-time state counts; a normal live call keeps today's flow.
		final boolean summonedWhileDead = leader.isDead();
		int spawned = 0;
		for (PhantomManager.Recruit recruit : recruits)
		{
			if ((spawned >= slots) || (spawned >= MAX_PER_REQUEST))
			{
				break;
			}
			final PhantomManager.Recruit wanted = recruit;
			final int order = spawned;
			final int memberLevel = explicitLevel ? requestedLevel : spreadLevel(requestedLevel);
			ThreadPool.schedule(() -> spawnAndApproach(leader, wanted, memberLevel, summonedWhileDead), 400L + (order * SPAWN_STAGGER) + Rnd.get(300));
			spawned++;
		}
	}

	/** Open party slots a recruiter can still fill, minus members already on the way to them. */
	private int freeSlots(Player leader)
	{
		final int cap = leader.isInParty() ? (9 - leader.getParty().getMemberCount()) : 8;
		int incoming = 0;
		for (Member m : _members.values())
		{
			if ((m.owner == leader) && !m.partied)
			{
				incoming++;
			}
		}
		return cap - incoming;
	}

	private void onPlayerItemAdd(OnPlayerItemAdd event)
	{
		Member npc = _members.get(event.getPlayer().getObjectId());
		if (npc != null) {
			npc.addItemsToLootingSession(event.getItem());
		}
	}

	private void onPlayerItemTransfer(OnPlayerItemTransfer event)
	{
		Member npc = _members.get(event.getPlayer().getObjectId());
		if (npc != null) {
			npc.removeItemsFromLootingSession(event.getItem());
		}
	}

	private void onPlayerTradeStart(OnPlayerTradeStart event)
	{
		Member npc = _members.get(event.getSender().getObjectId());
		if (npc != null) {
			npc.tradingState = TradingState.IS_TRADING;
		}
	}

	private void onPlayerTradeFinish(OnPlayerTradeFinish event)
	{
		Member npc = _members.get(event.getSender().getObjectId());
		if (npc != null) {
			npc.tradingState = TradingState.FINISHED;
		}
	}

	private void onPlayerTradeCancel(OnPlayerTradeCancel event)
	{
		Member npc = _members.get(event.getReceiver().getObjectId());
		if (npc != null) {
			npc.tradingState = TradingState.CANCELLED;
		}
	}

	/**
	 * The spot a recruit spawns at. In a peace zone (town) it is a far anchor so the recruit walks in for effect;
	 * anywhere else it is a short, geo-validated offset right beside the leader, so it never spawns in a wall or a
	 * neighbouring room and never has to jog through mob packs to reach the party. {@code getValidLocation} clamps the
	 * offset back onto the leader's own reachable mesh, so the landing tile is always on the same side of any wall.
	 */
	private Location recruitSpawnAnchor(Player leader)
	{
		final double angle = Rnd.nextDouble() * 2 * Math.PI;
		if (leader.isInsideZone(ZoneId.PEACE))
		{
			final int distance = Rnd.get(APPROACH_MIN, APPROACH_MAX);
			return new Location(leader.getX() + (int) (Math.cos(angle) * distance), leader.getY() + (int) (Math.sin(angle) * distance), leader.getZ());
		}
		final int distance = Rnd.get(ADJACENT_MIN, ADJACENT_MAX);
		final Location beside = new Location(leader.getX() + (int) (Math.cos(angle) * distance), leader.getY() + (int) (Math.sin(angle) * distance), leader.getZ());
		return GeoEngine.getInstance().getValidLocation(leader, beside);
	}

	/** Spawns a member near the leader (walk-in distance in town, right beside them in the field) and starts it moving to its slot. */
	private void spawnAndApproach(Player leader, PhantomManager.Recruit recruit, int level, boolean rezOnArrival)
	{
		if ((leader == null) || !leader.isOnline())
		{
			return;
		}
		final Location anchor = recruitSpawnAnchor(leader);
		final Player npc = PhantomManager.getInstance().spawnPartyMember(anchor, level, recruit.role, recruit.classId, recruit.race);
		if (npc == null)
		{
			return;
		}
		final Member member = new Member(npc, recruit.role);
		member.owner = leader;
		member.rezOnArrival = rezOnArrival;
		member.pendingSince = System.currentTimeMillis();
		parkPanicButtons(member);
		PhantomPlaystyleEngine.parkAutoSkills(npc, member.play, recruit.role.name()); // the playstyle engine owns offensive casting + listed panic/limit self-buffs
		_members.put(npc.getObjectId(), member);
		startTicking();
		npc.setRunning();
		npc.getAI().setIntention(Intention.MOVE_TO, formationSpot(member, leader)); // head for its own slot, not the leader's tile
		// Answer the shout so it feels like a person reacting to the LFM, then it jogs over. The brain phrases it
		// naturally when up; otherwise the canned omw line keeps it from going silent.
		speakEvent(member, true, //
			"You just saw " + leader.getName() + " shout that they want a " + recruit.role.name().toLowerCase() + " around level " + level //
				+ " for a hunting party. Answer the shout out loud in one line: you're up for it, tell them to invite you.", //
			omwLine(recruit.role));
	}

	private static String omwLine(PartyRole role)
	{
		switch (role)
		{
			case HEALER:
			{
				return Rnd.nextBoolean() ? "i can heal, omw" : "healer here, coming";
			}
			case BUFFER:
			{
				return Rnd.nextBoolean() ? "can buff, otw" : "i'll buff, omw";
			}
			case TANK:
			{
				return Rnd.nextBoolean() ? "tank here, omw" : "i can tank, coming";
			}
			case NUKER:
			{
				return Rnd.nextBoolean() ? "nuker, omw" : "mage here, coming";
			}
			case SINGER:
			{
				return Rnd.nextBoolean() ? "sws here, omw" : "got songs, coming";
			}
			case DANCER:
			{
				return Rnd.nextBoolean() ? "bd here, omw" : "got dances, coming";
			}
			default:
			{
				return Rnd.nextBoolean() ? "omw" : "coming, inv me";
			}
		}
	}

	// ===== Invite (called by RequestJoinParty) =====

	/** @return {@code true} if the player is a recruited member waiting on / serving an invite from this manager. */
	public boolean isRecruit(Player player)
	{
		return (player != null) && _members.containsKey(player.getObjectId());
	}

	/**
	 * Whether {@code member} should be dropped as a recipient of a party song/dance. A party's music classes
	 * otherwise clog each other's 12-slot music pool (a SwS's songs and a BD's dances share it), so each keeps
	 * evicting the other's music and re-casting it - a steady mana drain. Excluding the music phantoms from each
	 * other's (and their own kind's) songs/dances keeps their pool clear, so with the wait-for-expiry upkeep they
	 * re-sing only when a cycle actually lapses. Scoped to managed phantom SINGER/DANCER members only, so real
	 * players are never dropped from a party buff, and only to dance/song skills. The caster still lands its own
	 * music on itself (the target handler adds the caster separately), so its upkeep proxy is unaffected. Called by
	 * the {@code PARTY} target handler.
	 * @param skill the skill being resolved
	 * @param member a candidate party recipient
	 * @return {@code true} to skip this recipient for a song/dance
	 */
	public boolean isExcludedMusicRecipient(Skill skill, Player member)
	{
		if ((skill == null) || (member == null) || !skill.isDance())
		{
			return false;
		}
		final Member state = _members.get(member.getObjectId());
		return (state != null) && ((state.role == PartyRole.SINGER) || (state.role == PartyRole.DANCER));
	}

	/**
	 * @param member a recruited phantom
	 * @return the human that recruited {@code member} and is currently partied with it, or {@code null} if the
	 *         phantom isn't a recruit or hasn't been bound into an owner's party yet. Used to re-attribute a
	 *         phantom's quest kill credit to its owner, so partied AI don't have to be denied the last hit.
	 */
	public Player getRecruitOwner(Player member)
	{
		final Member state = (member != null) ? _members.get(member.getObjectId()) : null;
		return ((state != null) && state.partied) ? state.owner : null;
	}

	/**
	 * Binds a recruited member into the inviting player's party (a clientless phantom can't answer the invite
	 * dialog, so accept it server-side). Called by {@code RequestJoinParty}.
	 * @return {@code true} if the member joined
	 */
	public boolean onInvited(Player owner, Player member)
	{
		final Member state = _members.get(member.getObjectId());
		if ((state == null) || (owner == null) || member.isDead() || member.isInParty())
		{
			return false;
		}
		try
		{
			if (!owner.isInParty())
			{
				final PartyDistributionType type = (owner.getPartyDistributionType() != null) ? owner.getPartyDistributionType() : PartyDistributionType.FINDERS_KEEPERS;
				owner.setParty(new Party(owner, type));
			}
			else if (owner.getParty().getMemberCount() >= 9)
			{
				return false;
			}
			member.joinParty(owner.getParty());
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Failed to party recruit " + member.getName() + ": " + e.getMessage());
			return false;
		}
		state.owner = owner;
		state.partied = true;
		state.following = true;
		state.assist = true;
		state.graceUntil = 0;
		ensureFollow(state);
		speakEvent(state, false, //
			owner.getName() + " just invited you and you're now in their party as the " + state.role.name().toLowerCase() //
				+ ". React in one line: you're ready to hunt" //
				+ (state.isSupport() ? ", you'll keep everyone healed and buffed." : "; mention they can tell you to 'attack freely' to let you hunt on your own."), //
			"in, " + (state.isSupport() ? "i'll keep us up" : "i'll assist you - say 'attack freely' to let me hunt"));
		emote(state, SOCIAL_GREETING, 600 + Rnd.get(900)); // a little hello wave on joining
		startTicking();
		return true;
	}

	/**
	 * Adopts a live befriended regular into the inviter's party with the full recruited-member AI (follow,
	 * assist, heals/buffs/res for supports, raid logic, chat commands). Called by {@code RequestJoinParty}
	 * when a player party-invites one of their phantom friends. When the party ends the normal release path
	 * stores the friend's row and PhantomManager's ensure pass respawns it idle next to its owner.
	 * @return {@code true} if the friend joined
	 */
	public boolean onInvitedFriend(Player owner, Player friend)
	{
		if ((owner == null) || (friend == null) || friend.isDead() || friend.isInParty())
		{
			return false;
		}
		if (!_members.containsKey(friend.getObjectId()))
		{
			final PartyRole role = PhantomManager.getInstance().adoptFriendForParty(friend);
			if (role == null)
			{
				return false;
			}
			final Member member = new Member(friend, role);
			member.owner = owner;
			member.pendingSince = System.currentTimeMillis();
			parkPanicButtons(member);
			PhantomPlaystyleEngine.parkAutoSkills(friend, member.play, role.name()); // restored by restoreAutoSkills when the friend is released
			_members.put(friend.getObjectId(), member);
		}
		return onInvited(owner, friend);
	}

	// ===== Commands (whisper + party chat, called from chat handlers) =====

	/** Handles a whisper to a recruited member; a deterministic command, else a natural brain reply. */
	public String handleWhisper(Player owner, Player member, String message)
	{
		final Member state = _members.get(member.getObjectId());
		if ((state == null) || (owner == null) || (message == null))
		{
			return null;
		}
		if (!tryCommand(state, owner, message, true)) // a whisper is always addressed to this one member
		{
			askBrainAsync(state, owner, message); // free-form -> natural reply (brain), canned nudge if offline
		}
		return null;
	}

	/**
	 * A party-chat line drives the whole party: deterministic commands ("assist", "follow", "go to X") apply to
	 * every recruited member at once. Free-form chatter gets a reply from just ONE member, so a full party doesn't
	 * all answer the same line at once.
	 */
	public void handlePartyChat(Player speaker, String message)
	{
		if ((speaker == null) || (message == null) || !speaker.isInParty())
		{
			return;
		}
		final String lower = message.toLowerCase();
		final List<Member> mine = new ArrayList<>();
		final List<Member> named = new ArrayList<>();
		for (Player member : speaker.getParty().getMembers())
		{
			final Member state = _members.get(member.getObjectId());
			if ((state == null) || (state.owner != speaker))
			{
				continue;
			}
			mine.add(state);
			if (lower.contains(state.npc.getName().toLowerCase()))
			{
				named.add(state);
			}
		}
		if (mine.isEmpty())
		{
			return;
		}
		// Address one member by name and only that member reacts; otherwise the line drives the whole party.
		final boolean addressed = !named.isEmpty();
		final List<Member> targets = addressed ? named : mine;
		boolean matched = false;
		if (tryPartyCommand(speaker, message))
		{
			matched = true;
		}
		if (!matched)
		{
			for (Member state : targets)
			{
				if (tryCommand(state, speaker, message, addressed))
				{
					matched = true;
				}
			}
		}
		if (matched)
		{
			return;
		}
		// A request for a class-EXCLUSIVE ability (songs -> SINGER, dances -> DANCER) must only draw a reaction from
		// the class that can actually do it: a buffer should never pipe up "i don't sing". So restrict the free-form
		// reply pool to that class, and if the party has nobody of it, stay silent (nobody reacts) rather than have a
		// wrong-class member answer. (When a capable member is present the deterministic song/dance path above usually
		// handled it already; this catches phrasings that miss that trigger.)
		final PartyRole onlyRole = classExclusiveRequestRole(lower);
		if (onlyRole != null)
		{
			final List<Member> capable = new ArrayList<>();
			for (Member state : (named.isEmpty() ? mine : named))
			{
				if (state.role == onlyRole)
				{
					capable.add(state);
				}
			}
			if (capable.isEmpty())
			{
				return; // nobody in the party can do it - no wrong-class member reacts
			}
			if (named.isEmpty())
			{
				askBrainAsync(capable.get(Rnd.get(capable.size())), speaker, message);
			}
			else
			{
				for (Member state : capable)
				{
					askBrainAsync(state, speaker, message);
				}
			}
			return;
		}
		// Free-form chatter: named members each answer; if nobody was named, one member replies so a full party
		// doesn't all talk over each other.
		if (!named.isEmpty())
		{
			for (Member state : named)
			{
				askBrainAsync(state, speaker, message);
			}
		}
		else
		{
			askBrainAsync(mine.get(Rnd.get(mine.size())), speaker, message);
		}
	}

	/**
	 * @return the role that ALONE should react to a class-exclusive ability request in {@code lower} (SINGER for a
	 *         song request, DANCER for a dance request), or {@code null} if the line asks for nothing class-exclusive.
	 *         Uses the same conservative tokens as the deterministic song/dance triggers so ordinary chat ("using",
	 *         "losing") isn't mistaken for a request.
	 */
	private static PartyRole classExclusiveRequestRole(String lower)
	{
		if (containsAny(lower, "song", "sing pls", "sing please"))
		{
			return PartyRole.SINGER;
		}
		if (containsAny(lower, "dance"))
		{
			return PartyRole.DANCER;
		}
		return null;
	}

	/**
	 * Deterministic parser of commands to the entire party
	 *
	 * @return {@code true} if the line matched a known command (and was acted on); {@code false} for free-form
	 *         text the caller should route the call to each party member
	 */
	private boolean tryPartyCommand(Player owner, String message)
	{
		if (!owner.isInParty())
		{
			return false;
		}

		final String text = message.toLowerCase().trim();

		if (containsAny(text, "party return loot", "party hand over loot"))
		{
			_pendingTrades = new CopyOnWriteArrayList<>();
			_members.values().stream()
					.filter(member -> member.owner == owner && member.lootingSessionItems != null && !member.lootingSessionItems.isEmpty())
					.collect(Collectors.toCollection(() -> _pendingTrades));
			if (_pendingTrades.isEmpty()) {
				return false;
			}
			// first trade can and should be started eagerly
			_currentTrading = _pendingTrades.getFirst();
			if (_currentTrading.tradingState == TradingState.NOT_TRADING)
			{
				PhantomExchangeManager.newInstance().runTrade(
						_currentTrading.npc,
						owner,
						_currentTrading.lootingSessionItems
				);
			}
			return true;
		}

		return false;
	}

	/**
	 * Deterministic group-command parser shared by whisper and party chat (works with the brain offline).
	 * @param addressed {@code true} if this member was specifically addressed (whispered to, or named in the party
	 *            line) - required for the "make you the puller" order so a bare party-wide "pull" doesn't hit everyone
	 * @return {@code true} if the line matched a known command (and was acted on); {@code false} for free-form
	 *         text the caller should route to the brain
	 */
	private boolean tryCommand(Member state, Player owner, String message, boolean addressed)
	{
		final String text = message.toLowerCase().trim();

		// A specific buff by name ("give me might", "ww pls") - checked first so "give me"/"gimme" aren't eaten by
		// the grace ("brb") matcher. Grant it if the buffer knows it, else say it doesn't have it.
		if (state.isSupport())
		{
			final String requested = PhantomBuffs.requestedBuff(text);
			if (requested != null)
			{
				final Skill known = PhantomBuffs.findKnown(buffs(state), requested);
				if (known != null)
				{
					// "<buff> on me" / "<buff> on <member>": a named party member is the target, else the leader.
					final Player named = findPartyMemberByName(state, text);
					final Player target = (named != null) ? named : state.owner;
					state.pendingBuff = known;
					state.pendingBuffTarget = target;
					deliver(state, "sure, " + known.getName().toLowerCase() + ((target == state.owner) ? "" : " on " + target.getName()));
				}
				else
				{
					deliver(state, "i don't have " + requested);
				}
				return true;
			}
		}

		// On-demand SPECIFIC song/dance by name ("sing song of wind", "dance of fire pls", "give me fury"): cast that
		// exact one next tick, even if it's outside the auto rotation (e.g. Dance of Medusa). Checked BEFORE the
		// generic "sing"/"dance" trigger below so a named request casts that one song, not the whole rotation. It
		// resolves against the member's own learned skills, so a singer only answers song names and a dancer dances.
		if ((state.role == PartyRole.SINGER) || (state.role == PartyRole.DANCER))
		{
			final Skill namedSong = requestedSong(state.npc, text);
			if (namedSong != null)
			{
				state.pendingSong = namedSong;
				deliver(state, ((state.role == PartyRole.SINGER) ? "singing " : "dancing ") + namedSong.getName().toLowerCase());
				return true;
			}
		}

		// On-demand songs/dances: out of combat a SINGER/DANCER only performs when asked (in a fight it runs the
		// rotation on its own). "songs pls" reaches the singer, "dance" the dancer, "songs and dances" both.
		// ("song" not "sing" for the singer - a bare "sing" substring also matches "using"/"losing" in casual chat.)
		if (((state.role == PartyRole.SINGER) && containsAny(text, "song", "sing pls", "sing please")) //
			|| ((state.role == PartyRole.DANCER) && containsAny(text, "dance")))
		{
			state.songsRequested = true;
			deliver(state, (state.role == PartyRole.SINGER) ? "singing" : "dancing");
			return true;
		}

		if (((state.role == PartyRole.BOUNTY_HUNTER) && containsAny(text, "spoil on assist")))
        {
			state.spoilBehavior = SpoilBehavior.SPOIL_ON_ASSIST;
			stopCamp(owner);
			setFree(state, false);
			state.following = true;
			deliver(state, "got you boss, spoiling your targets!");
			return true;
		}

		// Raid pull control. Against a raid the party HOLDS until the tank initiates (see combatTick); these orders
		// drive that. Only the tank acknowledges out loud so a full party doesn't chatter over each other.
		// "tank attack" - order the tank to pull the boss; the rest follow once it has aggro.
		if (containsAny(text, "tank attack", "tank pull", "tank go", "tank engage", "tank initiate", "tank in", "pull it", "pull the boss", "pull boss", "initiate"))
		{
			if (state.role == PartyRole.TANK)
			{
				state.pullOrdered = true;
				state.pullSince = System.currentTimeMillis();
				deliver(state, "pulling - hold dps till i have aggro");
				dbg("ORDER 'tank attack' -> TANK '" + state.npc.getName() + "' pullOrdered (party of '" + owner.getName() + "')");
			}
			return true;
		}
		// "all attack" - everyone engages the current raid right now (skip the tank-initiate).
		if (containsAny(text, "all attack", "everyone attack", "all in", "open fire", "engage all", "attack the raid", "everyone in", "burn it"))
		{
			_released.add(owner.getObjectId());
			_releasedRaidTargets.removeIf(key -> raidOwnerId(key) == owner.getObjectId());
			dbg("ORDER 'all attack' -> party of '" + owner.getName() + "' released (all members may engage the raid)");
			if (state.role == PartyRole.TANK)
			{
				state.pullOrdered = true;
				state.pullSince = System.currentTimeMillis();
				deliver(state, "all in!");
			}
			return true;
		}
		// "hold fire" - re-engage the hold (stop feeding the raid, wait for the tank).
		if (containsAny(text, "hold fire", "hold dps", "wait for tank", "fall back", "stop dps", "back off"))
		{
			clearRaidRelease(owner);
			state.pullOrdered = false;
			if (state.role == PartyRole.TANK)
			{
				deliver(state, "holding");
			}
			return true;
		}

		// ===== Camp-and-pull =====
		// Break camp / stop pulling first - these phrases contain "camp"/"pull", so they must match before the
		// generic "camp"/"pull" starters below.
		if (containsAny(text, "break camp", "leave camp", "stop camp", "decamp", "no more camp", "stop camping", "move out"))
		{
			if (stopCamp(owner))
			{
				deliver(state, "breaking camp");
			}
			return true;
		}
		if (containsAny(text, "stop pull", "stop pulling", "no more pull", "hold pull", "quit pull", "stop fetching"))
		{
			final Camp camp = _camps.get(owner.getObjectId());
			if (camp != null)
			{
				camp.pullerId = 0; // stay camped, just stop fetching (fight only what wanders in)
			}
			deliver(state, "ok, no more pulls");
			return true;
		}
		// "<name> pull [N]" - make this member the puller. Only when this member was specifically addressed (whisper or
		// named in the party line), so a bare party-wide "pull" doesn't turn all eight into pullers at once.
		if (addressed && containsWord(text, "pull") && !containsAny(text, "dont pull", "don't pull", "pull it", "pull the boss", "pull boss"))
		{
			startCampPuller(state, parsePullSize(text));
			return true;
		}
		// "camp here" / "make camp" - plant a fixed camp at the leader's spot; the party holds and fights only what's
		// brought in. Matched on explicit phrases (not the bare word "camp"), so "go to abandoned camp" still travels.
		if (text.equals("camp") || containsAny(text, "camp here", "camp up", "set up camp", "make camp", "lets camp", "let's camp", "set up here", "hold this spot", "set camp"))
		{
			if (startCamp(owner))
			{
				deliver(state, "camping here");
			}
			return true;
		}

		// Free-hunt vs assist toggle.
		if (containsAny(text, "attack freely", "free hunt", "go wild", "ffa", "hunt freely", "do your own", "attack anything"))
		{
			stopCamp(owner); // a movement/targeting order breaks camp
			setFree(state, true);
			deliver(state, "k, hunting on my own");
			return true;
		}
		if (containsAny(text, "assist", "focus", "help me", "on my target", "kill my target", "attack my"))
		{
			stopCamp(owner);
			setFree(state, false);
			state.following = true;
			deliver(state, "k, assisting you");
			return true;
		}

		// Stand up on demand (interrupts an MP rest): "stand", "stand up", "get up", "on your feet".
		if (containsAny(text, "stand up", "stand", "get up", "on your feet", "feet"))
		{
			state.noSitUntil = System.currentTimeMillis() + STAND_SUPPRESS; // don't pop straight back down
			afterHumanDelay(state, () ->
			{
				if (state.npc.isSitting())
				{
					state.npc.standUp();
				}
			});
			deliver(state, "up");
			return true;
		}

		// Follow / hold.
		if (containsAny(text, "follow", "come with", "on me", "regroup", "gather", "stack", "stick with"))
		{
			stopCamp(owner); // "follow" breaks camp - the party moves with the leader again
			state.following = true;
			setFree(state, false); // "follow" also leaves free-hunt - without this the member said "coming" but kept attacking
			afterHumanDelay(state, () ->
			{
				final Player npc = state.npc;
				if (npc.isAttackingNow() || npc.isInCombat())
				{
					npc.abortAttack();
					npc.setTarget(null); // drop the mob too, or AutoUse keeps casting at it mid-follow
				}
				ensureFollow(state);
			});
			deliver(state, "coming");
			return true;
		}
		if (containsAny(text, "stay", "wait here", "hold", "stop", "halt"))
		{
			stopCamp(owner); // "stop"/"hold" ends camp too (stop everything, incl. pulling)
			state.following = false;
			setFree(state, false);
			afterHumanDelay(state, () -> state.npc.getAI().setIntention(Intention.IDLE));
			deliver(state, "holding here");
			return true;
		}

		// Grace.
		if (containsAny(text, "brb", "be right back", "give me", "gimme", "afk", "one sec", "1 sec", "moment"))
		{
			state.graceUntil = System.currentTimeMillis() + BRB_GRACE;
			deliver(state, "np");
			return true;
		}

		// Disband / dismiss.
		if (containsAny(text, "disband", "leave party", "leave the party", "dismiss", "you can go", "thanks bye", "thx bye", "bye", "gl hf"))
		{
			deliver(state, "gl hf o/");
			emote(state, SOCIAL_BOW, 400); // a parting bow before leaving
			ThreadPool.schedule(() -> release(state, true), 1800);
			return true;
		}

		// Status.
		if (containsAny(text, "status", "hp?", "mp?", "you ok", "u ok"))
		{
			deliver(state, "hp " + state.npc.getCurrentHpPercent() + "% / mp " + state.npc.getCurrentMpPercent() + "%");
			return true;
		}

		if (containsAny(text, "return loot", "hand over loot", "hand over loot"))
		{
			Map<Integer, Integer> spoils = state.lootingSessionItems;
			if (spoils == null || spoils.isEmpty()) {
				deliver(state, "nothing to hand over yet, how about a few more mobs then?");
				return true;
			}
			if (addressed && state.tradingState == TradingState.NOT_TRADING)
			{
				_currentTrading = state;
				deliver(state, "here's your loot mate, have fun with it!");
				PhantomExchangeManager.newInstance().runTrade(
						state.npc,
						owner,
						spoils
				);
			}
			return true;
		}

		// Support on-demand orders actually do the thing now (not just acknowledge).
		if (state.isSupport())
		{
			// "buff all / everyone / the party": fully (re)buff every living party member, one buff per tick.
			if (containsAny(text, "buff all", "buff everyone", "buff every", "buff the party", "buff party", "buff us all", "buff whole party", "full buff all", "fully buff"))
			{
				startRebuff(state, partyTargets(state));
				deliver(state, "buffing everyone");
				return true;
			}
			// "buff <name>": fully buff one named party member (e.g. the tank by name).
			final Player named = findPartyMemberByName(state, text);
			if ((named != null) && text.contains("buff"))
			{
				startRebuff(state, new ArrayList<>(List.of(named)));
				deliver(state, "buffing " + named.getName());
				return true;
			}
			// Group phrasings ("rebuff", "buff us") recast the WHOLE party's kits, not just the leader's - a real
			// buffer asked to "rebuff" tops up everyone. "buff me" stays leader-only (see below).
			if (!text.contains("buff me") && containsAny(text, "rebuff", "buff us", "rebuf"))
			{
				startRebuff(state, partyTargets(state));
				deliver(state, "rebuffing everyone");
				return true;
			}
			if (containsAny(text, "buff me", "buff", "rebuf"))
			{
				startRebuff(state, new ArrayList<>(List.of(state.owner))); // recast the leader's whole kit over the next ticks
				deliver(state, "rebuffing");
				return true;
			}
			if (containsAny(text, "heal me", "heal us", "heal", "hp"))
			{
				state.healNow = true; // heal the leader once even if not hurt
				deliver(state, "healing you");
				return true;
			}
			if (containsAny(text, "res", "resurrect", "ress", "revive", "rez"))
			{
				// The res loop raises a fallen member automatically - but only a member that actually knows
				// Resurrection can, so don't promise a rez a pure buffer/DPS can't deliver.
				deliver(state, (res(state) != null) ? "rezzing" : "i can't res, sorry");
				return true;
			}
		}

		// "go to / tp <place>": teleport this member to a named gatekeeper spot (reuses the buddy destination data,
		// so the whole party can regroup at the same place the leader's gatekeeper drops them).
		final Map.Entry<String, Location> dest = PhantomBuddyManager.getInstance().findDestination(text);
		if ((dest != null) && isTravelOrder(text))
		{
			stopCamp(owner); // travelling somewhere ends the camp
			afterHumanDelay(state, () -> teleportMember(state, dest.getValue()));
			deliver(state, "omw to " + dest.getKey());
			return true;
		}

		return false; // not a known command -> caller routes free-form text to the brain
	}

	/** {@code true} when the line is an explicit order to travel now, not just mentioning a place. */
	private static boolean isTravelOrder(String text)
	{
		return containsAny(text, "tp", "teleport", "port to", "port us", "go to", "lets go", "let's go", "take us", "head to", "lets head", "move to", "warp", "lets tp", "go there");
	}

	private void setFree(Member state, boolean free)
	{
		state.assist = !free;
		List<Skill> autoUseSkills = provideAutoUsedSkills(state);
		PhantomManager.getInstance().setRecruitHunting(state.npc, free, autoUseSkills);
		if (!free)
		{
			ensureFollow(state);
		}
	}

	private List<Skill> provideAutoUsedSkills(Member state)
	{
		return switch (state.role)
		{
			case BOUNTY_HUNTER -> List.of(state.sweeper, state.spoil);
			default -> List.of();
		};
	}

	/**
	 * Sends a free-form order to the LLM brain (PARTY mode) off the network thread for a natural reply that may
	 * carry an action tag (assist / free / follow / stay / tp / disband / grace), executed then stripped. Falls
	 * back to a canned nudge if the brain is offline, so the member always answers.
	 */
	private void askBrainAsync(Member state, Player owner, String message)
	{
		final int memberId = state.npc.getObjectId();
		final int ownerId = owner.getObjectId();
		ThreadPool.execute(() ->
		{
			final Member s = _members.get(memberId);
			final Player o = (Player) World.getInstance().findObject(ownerId);
			if ((s == null) || (o == null) || s.npc.isDead())
			{
				return;
			}
			String reply = callBrain(s.npc, o.getName(), s.role, isPartiedWith(s), message);
			if ((reply == null) || reply.isEmpty())
			{
				reply = "say: assist, attack freely, follow, hold, a place to tp, brb or bye";
			}
			else
			{
				reply = applyTags(reply, s, o, message);
			}
			if (reply.isEmpty())
			{
				return;
			}
			final Player npc = s.npc;
			if (npc.isInParty())
			{
				npc.getParty().broadcastCreatureSay(new CreatureSay(npc, ChatType.PARTY, npc.getName(), reply), npc);
			}
			else if (o.isOnline())
			{
				o.sendPacket(new CreatureSay(npc, ChatType.WHISPER, npc.getName(), reply));
			}
		});
	}

	/** Calls the brain bridge in PARTY mode; returns the raw reply (possibly with tags), or null if offline. */
	private String callBrain(Player npc, String ownerName, PartyRole role, boolean partied, String message)
	{
		return callBrain(npc, ownerName, role, partied, "PARTY", message);
	}

	/** Calls the brain bridge in {@code mode} (PARTY orders, or PARTYEVENT lifecycle chatter); null if offline. */
	private String callBrain(Player npc, String ownerName, PartyRole role, boolean partied, String mode, String message)
	{
		try
		{
			// Send the member's real level/class so it answers "what lvl are you?" truthfully (an Interlude
			// character caps at 80) and stays consistent with the party it was recruited into, instead of guessing.
			final var playerClass = npc.getPlayerClass();
			final String botClass = playerClass == null ? "" : titleCaseEnum(playerClass.name());
			final HttpRequest request = HttpRequest.newBuilder() //
				.uri(URI.create(BRAIN_URL)) //
				.timeout(Duration.ofSeconds(BRAIN_TIMEOUT_SECONDS)) //
				.header("X-FPC", npc.getName()) //
				.header("X-Mode", mode) //
				.header("X-Player", ownerName) //
				.header("X-Role", role.name().toLowerCase()) //
				.header("X-Partied", Boolean.toString(partied)) //
				.header("X-Bot-Level", Integer.toString(npc.getLevel())) //
				.header("X-Bot-Class", botClass) //
				.header("Content-Type", "text/plain; charset=utf-8") //
				.POST(HttpRequest.BodyPublishers.ofString(message)) //
				.build();
			final HttpResponse<String> response = BRAIN_HTTP.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() == 200)
			{
				final String reply = response.body().trim();
				if (!reply.isEmpty())
				{
					return reply;
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.fine(getClass().getSimpleName() + ": Brain bridge unreachable: " + e.getMessage());
		}
		return null;
	}

	/** "ELVEN_KNIGHT" -&gt; "Elven Knight"; blank stays blank. */
	private static String titleCaseEnum(String enumName)
	{
		if ((enumName == null) || enumName.isEmpty())
		{
			return "";
		}
		final StringBuilder sb = new StringBuilder();
		for (String word : enumName.toLowerCase().split("_"))
		{
			if (word.isEmpty())
			{
				continue;
			}
			if (sb.length() > 0)
			{
				sb.append(' ');
			}
			sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
		}
		return sb.toString();
	}

	/** Executes any action tag in the brain's reply and returns the cleaned, speakable text. */
	private String applyTags(String reply, Member state, Player owner, String playerMessage)
	{
		final boolean partied = isPartiedWith(state);
		if (partied && TAG_ASSIST.matcher(reply).find())
		{
			setFree(state, false);
			state.following = true;
		}
		if (partied && TAG_FREE.matcher(reply).find())
		{
			setFree(state, true);
		}
		if (partied && TAG_FOLLOW.matcher(reply).find())
		{
			state.following = true;
			ensureFollow(state);
		}
		if (partied && TAG_STAY.matcher(reply).find())
		{
			state.following = false;
			setFree(state, false);
			state.npc.getAI().setIntention(Intention.IDLE);
		}
		final Matcher grace = TAG_GRACE.matcher(reply);
		if (grace.find())
		{
			state.graceUntil = System.currentTimeMillis() + (Math.min(30, Integer.parseInt(grace.group(1))) * 60000L);
		}
		final Matcher tp = TAG_TP.matcher(reply);
		if (partied && tp.find())
		{
			final Map.Entry<String, Location> dest = PhantomBuddyManager.getInstance().findDestination(tp.group(1));
			if (dest != null)
			{
				teleportMember(state, dest.getValue());
			}
		}
		if (partied && TAG_DISBAND.matcher(reply).find())
		{
			ThreadPool.schedule(() -> release(state, true), 1000);
		}
		return ANY_TAG.matcher(reply).replaceAll("").trim();
	}

	/**
	 * Teleports a recruited member and finalizes it for a clientless player ({@code onTeleported} re-spawns +
	 * broadcasts; without it the member shows on radar but renders for nobody), then re-grabs follow on arrival.
	 */
	private void teleportMember(Member state, Location destination)
	{
		final Player npc = state.npc;
		npc.abortCast();
		npc.teleToLocation(destination);
		npc.onTeleported();
		npc.broadcastUserInfo();
		// Commit to the destination: wait here for the leader rather than letting the follow watchdog immediately
		// teleport us back to where the leader still is (that was the "everyone tped to the player's spot" bug).
		state.travelUntil = System.currentTimeMillis() + TRAVEL_GRACE;
	}

	/**
	 * This member's personal formation slot around {@code target}: a stable per-member compass direction and
	 * distance, geo-validated. Following the SLOT instead of the target keeps a travelling party spread out like
	 * people, instead of every member converging on the leader's exact tile via the shared engine follow.
	 * <p>
	 * The slot is validated from the TARGET's side (leader -> slot), never from the member's: validating from the
	 * member clips a slot behind a wall to the member's side of it, so the member "arrived" at the wall and stood
	 * there (the stuck-at-the-gate bug). Leader-side validation only keeps the slot out of solid geometry around
	 * the leader; the member's own walk to it goes through real pathfinding (MOVE_TO), which routes around walls.
	 */
	private Location formationSpot(Member state, Creature target)
	{
		final int x = target.getX() + (int) (Math.cos(state.formAngle) * state.formDist);
		final int y = target.getY() + (int) (Math.sin(state.formAngle) * state.formDist);
		return GeoEngine.getInstance().getValidLocation(target, new Location(x, y, target.getZ()));
	}

	/**
	 * Keeps a member moving to its formation slot around {@code target}, re-kicking a stalled walk and teleporting
	 * it to catch up if it gets stuck or falls a long way behind - so a recruit never says "coming" while standing
	 * still. Members start moving after their personal jitter (not all on the same tick) and hold once settled in
	 * their slot, so a travelling party reads as individuals, not a stack.
	 */
	private void driveFollow(Member state, Player target)
	{
		final Player npc = state.npc;
		if (npc.isSitting())
		{
			return;
		}
		final Location spot = formationSpot(state, target);
		// Fallen far behind (the leader teleported or outran it): catch up by teleport, checked FIRST and
		// unconditionally - it used to sit below the "settled" check, which a wall-clipped slot could satisfy
		// while the leader was a zone away, so the teleport never fired.
		if (npc.calculateDistance2D(target) > CATCHUP_TP_RANGE)
		{
			npc.abortCast();
			npc.teleToLocation(new Location(spot.getX() + Rnd.get(-40, 40), spot.getY() + Rnd.get(-40, 40), spot.getZ()));
			npc.onTeleported();
			npc.broadcastUserInfo();
			state.stuckTicks = 0;
			state.moveDelayUntil = 0;
			state.lastX = npc.getX();
			state.lastY = npc.getY();
			return;
		}
		// Settled means near the slot AND actually near the follow target - distance to the slot alone is not
		// enough, because geometry can put the reachable end of a path short of the true slot.
		if ((npc.calculateDistance2D(spot) <= FORMATION_TOLERANCE) && (npc.calculateDistance2D(target) <= (FORMATION_MAX_DIST + FORMATION_TOLERANCE + 130)))
		{
			// Settled in the slot - hold, and re-arm the start-up stagger for the next time the leader moves off.
			state.stuckTicks = 0;
			state.moveDelayUntil = 0;
			state.lastX = npc.getX();
			state.lastY = npc.getY();
			return;
		}
		// Starting from a standstill: wait out this member's personal beat first, so the party doesn't lurch into
		// motion as one. Already-moving members just keep tracking the slot.
		if (!npc.isMoving())
		{
			final long now = System.currentTimeMillis();
			if (state.moveDelayUntil == 0)
			{
				state.moveDelayUntil = now + state.moveJitter;
				if (state.moveJitter > 0)
				{
					return;
				}
			}
			else if (now < state.moveDelayUntil)
			{
				return;
			}
		}
		state.moveDelayUntil = 0;
		if ((Math.abs(npc.getX() - state.lastX) + Math.abs(npc.getY() - state.lastY)) < 30)
		{
			state.stuckTicks++;
		}
		else
		{
			state.stuckTicks = 0;
		}
		if (state.stuckTicks >= 4) // blocked walk (bad path, clipped slot, crowd): pop through to the slot
		{
			npc.abortCast();
			npc.teleToLocation(new Location(spot.getX() + Rnd.get(-40, 40), spot.getY() + Rnd.get(-40, 40), spot.getZ()));
			npc.onTeleported();
			npc.broadcastUserInfo();
			state.stuckTicks = 0;
		}
		// Re-path only when idle or the slot has drifted (the leader ran on) - not every tick, which caused the
		// whole party to re-path on one shared heartbeat.
		else if (!npc.isMoving() || ((Math.abs(spot.getX() - state.lastDestX) + Math.abs(spot.getY() - state.lastDestY)) > FORMATION_REISSUE_DELTA))
		{
			npc.setRunning();
			npc.getAI().setIntention(Intention.MOVE_TO, spot);
			state.lastDestX = spot.getX();
			state.lastDestY = spot.getY();
		}
		state.lastX = npc.getX();
		state.lastY = npc.getY();
	}

	/** Speaks a member reply after a short human-like pause, on party chat if partied else a whisper. */
	private void deliver(Member state, String text)
	{
		if ((text == null) || text.isEmpty())
		{
			return;
		}
		final long delay = 800 + Rnd.get(1200);
		ThreadPool.schedule(() ->
		{
			final Player npc = state.npc;
			if (npc.isDead())
			{
				return;
			}
			if (npc.isInParty())
			{
				npc.getParty().broadcastCreatureSay(new CreatureSay(npc, ChatType.PARTY, npc.getName(), text), npc);
			}
			else if ((state.owner != null) && state.owner.isOnline())
			{
				state.owner.sendPacket(new CreatureSay(npc, ChatType.WHISPER, npc.getName(), text));
			}
		}, delay);
	}

	private void shout(Player npc, String text)
	{
		final CreatureSay cs = new CreatureSay(npc, ChatType.SHOUT, npc.getName(), text);
		for (Player player : World.getInstance().getPlayers())
		{
			player.sendPacket(cs);
		}
	}

	/**
	 * Speaks a recruit lifecycle line (answering the LFM, arriving, joining) in the member's own voice: asks the
	 * brain (PARTYEVENT mode) for a natural line off the game thread, and falls back to the canned {@code fallback}
	 * when the brain is offline - so the recruit sounds like a person when the brain is up but never goes silent
	 * when it isn't. Delivered on global shout when {@code asShout}, otherwise via {@link #deliver} (party chat if
	 * partied, else a whisper to the owner).
	 * @param state the recruited member
	 * @param asShout {@code true} to answer on the global shout channel; {@code false} to whisper/party-chat
	 * @param situation a short description of what just happened, for the brain to react to
	 * @param fallback the canned line to use when the brain is offline
	 */
	private void speakEvent(Member state, boolean asShout, String situation, String fallback)
	{
		final int memberId = state.npc.getObjectId();
		final String ownerName = (state.owner != null) ? state.owner.getName() : "";
		final PartyRole role = state.role;
		final boolean partied = isPartiedWith(state);
		ThreadPool.execute(() ->
		{
			// Re-resolve under the manager's view so we never speak for a member that despawned/released mid-call.
			final Member current = _members.get(memberId);
			if ((current == null) || current.npc.isDead())
			{
				return;
			}
			String line = callBrain(current.npc, ownerName, role, partied, "PARTYEVENT", situation);
			if (line != null)
			{
				line = ANY_TAG.matcher(line).replaceAll("").trim(); // lifecycle chatter is speech only - drop any stray tag
			}
			if ((line == null) || line.isEmpty())
			{
				line = fallback;
			}
			if (asShout)
			{
				shout(current.npc, line);
			}
			else
			{
				deliver(current, line);
			}
		});
	}

	// ===== Slow housekeeping tick (called by PhantomManager supervisor ~5s) =====

	public void supervise(Player member)
	{
		final Member state = _members.get(member.getObjectId());
		if ((state != null) && member.isDead())
		{
			handleDead(state, System.currentTimeMillis());
		}
	}

	/**
	 * A fallen member is kept as a corpse so a healer can battle-res it, instead of despawning the instant it dies
	 * (the old behaviour that made the party melt permanently on a long fight). Each call: auto-accepts a pending
	 * resurrection (a clientless phantom can't click the confirm dialog, so we answer "yes" for it), starts the
	 * corpse window on first death, and only releases once that window elapses or the party/owner is gone.
	 */
	private void handleDead(Member state, long now)
	{
		final Player npc = state.npc;
		// A healer's Resurrection lands as a revive *request* (ConfirmDlg) the corpse must accept - do it server-side.
		if (npc.isReviveRequested())
		{
			npc.reviveAnswer(1); // it stands on the next tick, where deadSince is cleared and it rejoins
			return;
		}
		if (state.deadSince == 0)
		{
			state.deadSince = now; // open the res window
			if (state.role == PartyRole.TANK)
			{
				relockBoss(state.owner); // the BOSS is no longer safe for DPS; already-open adds stay fightable
			}
			// The fallen member can't talk - another member reacts (a healer says it's coming with the res).
			if (Rnd.get(100) < 60)
			{
				final Member mourner = pickPartiedMember(state.owner, m -> (m != state) && !m.npc.isDead());
				if (mourner != null)
				{
					if (res(mourner) != null)
					{
						bark(mourner, npc.getName() + " of your party just died in combat. You can resurrect - say one very short line that you'll res them.", "got you " + npc.getName() + ", rezzing");
					}
					else
					{
						bark(mourner, npc.getName() + " of your party just died in combat. React in one very short line.", Rnd.nextBoolean() ? "rip " + npc.getName() : "F");
					}
				}
			}
			if (DEBUG)
			{
				final Monster boss = engagedRaid(state);
				dbg("DEATH " + roleLabel(npc) + " '" + npc.getName() + "' died" + ((boss != null) ? (" | boss '" + boss.getName() + "' was hating " + describe(boss.getMostHated())) : ""));
			}
			return;
		}
		// Window elapsed, or there's no longer a party/owner to be raised by: let the corpse go.
		if (((now - state.deadSince) >= CORPSE_GRACE) || !isPartiedWith(state) || (state.owner == null) || !state.owner.isOnline())
		{
			release(state, false);
		}
	}

	// ===== Fast behaviour tick =====

	private synchronized void startTicking()
	{
		if (_ticking)
		{
			return;
		}
		_ticking = true;
		ThreadPool.scheduleAtFixedRate(this::tick, TICK_INTERVAL, TICK_INTERVAL);
	}

	private void tick()
	{
		final long now = System.currentTimeMillis();
		_healedThisTick.clear(); // fresh per-tick heal claims, so two healers don't stack on one target this tick
		if (DEBUG && ((now - _lastRaidLog) >= RAID_LOG_INTERVAL))
		{
			_lastRaidLog = now;
			try
			{
				logRaidSnapshot();
			}
			catch (Exception e)
			{
				LOGGER.warning(getClass().getSimpleName() + ": Raid snapshot log error: " + e.getMessage());
			}
		}
		for (Member state : _members.values())
		{
			final Player npc = state.npc;
			try
			{
				if ((npc == null) || (World.getInstance().findObject(npc.getObjectId()) == null))
				{
					_members.remove(npc == null ? -1 : npc.getObjectId());
					continue;
				}
				if (npc.isDead())
				{
					handleDead(state, now); // keep the corpse for a res window instead of despawning on the spot
					continue;
				}
				if (state.deadSince != 0)
				{
					// Stood back up since last tick (a healer raised it): clear the corpse timer and rejoin the fight.
					// The shared res claim carries its own expiry, so there is nothing to release here.
					state.deadSince = 0;
					state.recoveryUntil = now + POST_RES_HOLD_MS;
					if (state.role == PartyRole.TANK)
					{
						state.pullSince = now;
						relockBoss(state.owner); // hold BOSS DPS while the tank is healed; open adds stay fightable
					}
					ensureFollow(state);
					bark(state, "You just got resurrected by a party healer mid-hunt and are back on your feet. Say one very short thanks.", "ty");
				}
				if (!state.partied)
				{
					pendingTick(state, now);
					continue;
				}
				serve(state, now);
			}
			catch (Exception e)
			{
				LOGGER.warning(getClass().getSimpleName() + ": Party tick error for " + (npc == null ? "?" : npc.getName()) + ": " + e.getMessage());
			}
		}
		try
		{
			partyMoodTick(now); // per-party social pass: gz on level-up, leader-down reaction, raid victory, small talk
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Party mood tick error: " + e.getMessage());
		}
	}

	// ===== Raid combat trace (toggle with //phantom debug on|off; raid-only so it's silent during farming) =====

	private void dbg(String line)
	{
		if (DEBUG)
		{
			LOGGER.info("PARTY-RAID " + line);
		}
	}

	/**
	 * Diagnostic: logs every buff a support phantom actually casts, so we can see exactly what a buffer is doing (and
	 * spot a re-cast loop, e.g. two different skills fighting over the same abnormal slot). Toggle with the existing
	 * {@code //phantom debug on|off}. {@code via} says which path issued it (upkeep / rebuff / on-demand).
	 */
	private void dbgBuff(Player npc, Player target, Skill buff, String via)
	{
		if (DEBUG && (buff != null))
		{
			final BuffInfo existing = (target == null) ? null : target.getEffectList().getBuffInfoBySkillId(buff.getId());
			final BuffInfo slot = ((target == null) || (buff.getAbnormalType() == null)) ? null : target.getEffectList().getBuffInfoByAbnormalType(buff.getAbnormalType());
			LOGGER.info("PARTY-BUFF " + npc.getName() + " -> " + (target == null ? "?" : target.getName()) //
				+ " : " + buff.getName() + " (id " + buff.getId() + " lvl " + buff.getLevel() + ", abnormal " + buff.getAbnormalType() + ") [" + via + "]" //
				+ " | this-buff " + (existing == null ? "absent" : existing.getTime() + "s left") //
				+ " | abnormal-slot held by " + ((slot == null) ? "nothing" : (slot.getSkill().getName() + " id " + slot.getSkill().getId())));
		}
	}

	/**
	 * One snapshot per engaged party: the boss (HP + who it's hating) and every member's role/HP/MP/action, now with
	 * the engage-gate reason so a member reported as {@code idle} also says WHY (e.g. {@code HOLD/TANK_NOT_ORDERED} or
	 * {@code HOLD/NO_TANK_WAIT(6/20s)}). The first snapshot of a fresh engagement also prints a one-time ENGAGE START
	 * banner dumping the initial roster + pull state, so "phantoms standing around" has a full trace from the pull.
	 */
	private void logRaidSnapshot()
	{
		final Set<Integer> seenParties = new HashSet<>();
		final Set<Integer> engagedNow = new HashSet<>();
		for (Member state : _members.values())
		{
			final Player owner = state.owner;
			if (!state.partied || (owner == null) || !owner.isOnline() || !owner.isInParty())
			{
				continue;
			}
			if (!seenParties.add(owner.getObjectId()))
			{
				continue; // already logged this party this pass
			}
			final List<Monster> raids = engagedRaids(state);
			if (raids.isEmpty())
			{
				continue;
			}
			engagedNow.add(owner.getObjectId());
			final StringBuilder bosses = new StringBuilder();
			for (Monster boss : raids)
			{
				if (bosses.length() > 0)
				{
					bosses.append(" | ");
				}
				bosses.append("'").append(boss.getName()).append("' lvl=").append(boss.getLevel()).append(" hp=").append(boss.getCurrentHpPercent()).append("% hating=").append(describe(boss.getMostHated()));
			}
			// One-time ENGAGE START banner for a fresh pull: the roster, whether a tank is present/pulling, and the
			// party-wide release flags - the state that decides whether anyone is allowed to swing.
			if (_raidTraceBannered.add(owner.getObjectId()))
			{
				final Member tank = findTankState(state);
				final Player humanTank = humanRaidTank(state);
				dbg("##### ENGAGE START party of '" + owner.getName() + "' vs " + bosses + " #####");
				dbg("  pull-state: released=" + _released.contains(owner.getObjectId()) //
					+ " phantomTank=" + ((tank == null) ? "none" : ("'" + tank.npc.getName() + "' pullOrdered=" + tank.pullOrdered + " dead=" + tank.npc.isDead())) //
					+ " humanTank=" + ((humanTank == null) ? "none" : ("'" + humanTank.getName() + "'")));
				for (Player member : owner.getParty().getMembers())
				{
					dbg("  roster: " + roleLabel(member) + " '" + member.getName() + "' lvl=" + member.getLevel());
				}
			}
			dbg("=== raid " + bosses + " ===");
			for (Player member : owner.getParty().getMembers())
			{
				final Member ms = _members.get(member.getObjectId());
				final String reason = (ms == null) ? "" : (" gate=" + (ms.raidGateReason == null ? "?" : ms.raidGateReason));
				dbg("  " + roleLabel(member) + " '" + member.getName() + "' hp=" + member.getCurrentHpPercent() + "% mp=" + member.getCurrentMpPercent() + "% " + action(member) + reason);
			}
		}
		// Prune banner flags for parties no longer engaged, so the next pull re-banners from scratch.
		_raidTraceBannered.retainAll(engagedNow);
	}

	/** A live raid boss in combat near this member, or {@code null} - the boss to report on / the trigger for the trace. */
	private Monster engagedRaid(Member state)
	{
		final List<Monster> raids = engagedRaids(state);
		return raids.isEmpty() ? null : raids.get(0);
	}

	/**
	 * Radial hold distance for an archer fighting a raid: just inside its bow's actual reach (so it can fire), still
	 * outside melee-AoE, and never past the normal backline. Derived from the equipped bow so it adapts to grade.
	 */
	private int archerHoldRange(Member state)
	{
		final int reach = state.npc.getPhysicalAttackRange();
		return Math.max(350, Math.min(RAID_BACKLINE_RANGE, reach - ARCHER_RANGE_MARGIN));
	}

	/** The engaged main raid boss (a raid that is NOT a minion/add), or {@code null} - the target the tank should hold. */
	private Monster engagedBoss(Member state)
	{
		for (Monster raid : engagedRaids(state))
		{
			if (!raid.isRaidMinion())
			{
				return raid;
			}
		}
		return null;
	}

	/** All live raid mobs/adds in combat near this member. */
	private List<Monster> engagedRaids(Member state)
	{
		final List<Monster> raids = new ArrayList<>();
		for (Monster mob : World.getInstance().getVisibleObjectsInRange(state.npc, Monster.class, ASSIST_MAX_RANGE))
		{
			if (!mob.isDead() && mob.isRaid() && mob.isInCombat())
			{
				raids.add(mob);
			}
		}
		return raids;
	}

	/** Role tag for a party member: its recruited role, or PLAYER for a real human (the leader). */
	private String roleLabel(Player member)
	{
		final Member m = _members.get(member.getObjectId());
		return (m != null) ? m.role.name() : "PLAYER";
	}

	/** Short label for who a creature is (name + role), used for the boss's most-hated target. */
	private String describe(Creature who)
	{
		if (who == null)
		{
			return "none";
		}
		return who.isPlayer() ? (roleLabel((Player) who) + " '" + who.getName() + "'") : who.getName();
	}

	private static String action(Player npc)
	{
		if (npc.isDead())
		{
			return "DEAD";
		}
		if (npc.isSitting())
		{
			return "sitting";
		}
		if (npc.isCastingNow())
		{
			return "casting";
		}
		if (npc.isAttackingNow())
		{
			return "attacking";
		}
		return "idle";
	}

	/** A recruit on its way over: keep walking to the leader, ask for the invite on arrival, give up on timeout. */
	private void pendingTick(Member state, long now)
	{
		final Player npc = state.npc;
		final Player owner = state.owner;
		if ((owner == null) || !owner.isOnline() || ((now - state.pendingSince) > RECRUIT_TIMEOUT))
		{
			if ((owner != null) && owner.isOnline() && !state.reminded)
			{
				deliver(state, "guess not, gl");
			}
			release(state, false);
			return;
		}
		if (npc.calculateDistance2D(owner) <= ARRIVE_RANGE)
		{
			// Summoned to a dead solo player: skip the "invite me" handshake a corpse can't complete and bind into the
			// party server-side (self-invite), so the rez can land at once. Gated to a still-dead summoner - if they
			// got back up on their own before we arrived, fall through to the normal invite handshake below.
			if (state.rezOnArrival && owner.isDead())
			{
				state.rezOnArrival = false;
				if (onInvited(owner, npc)) // sets partied; the next serve() tick rezzes the dead owner (skill or scroll)
				{
					return;
				}
			}
			if (!state.reminded)
			{
				state.reminded = true;
				if (owner.isOnline())
				{
					speakEvent(state, false, //
						"You are standing right next to " + owner.getName() + ", who is looking for a hunting party. Tell them in one line to invite you to the party.", //
						"here, inv me");
					emote(state, SOCIAL_GREETING, 300 + Rnd.get(700)); // wave hello once beside the leader
				}
			}
		}
		else if (state.following)
		{
			driveFollow(state, owner); // keep closing the distance; teleport in if pathing stalls
		}
	}

	/** One service step for a partied member. @return {@code false} if released. */
	private boolean serve(Member state, long now)
	{
		final Player npc = state.npc;
		final Player owner = state.owner;

		if (!isPartiedWith(state))
		{
			release(state, false);
			return false;
		}

		// Owner offline: hold on a grace window (extended by "brb"), then let go.
		if (!owner.isOnline() || (World.getInstance().findObject(owner.getObjectId()) == null))
		{
			if (state.graceUntil == 0)
			{
				state.graceUntil = now + OFFLINE_GRACE;
			}
			if (now >= state.graceUntil)
			{
				release(state, false);
				return false;
			}
			return true;
		}
		if ((state.graceUntil != 0) && (state.graceUntil <= now))
		{
			state.graceUntil = 0;
		}

		// Cast watchdog: a clientless caster can wedge with its casting flag stuck; abort an over-long cast so the
		// member recovers instead of freezing (stops buffing/healing AND following). The limit tracks the LIVE skill's
		// own expected duration (hit + cool time is the pre-haste ceiling; casting speed only shortens it) plus a
		// margin, floored at 6s - so a legitimate slow cast (base Resurrection 6s, Group/Greater Group Heal 7s) runs
		// to completion instead of being aborted at a flat 6s, while a truly stuck flag (which never clears) still trips.
		if (npc.isCastingNow())
		{
			if (state.castSince == 0)
			{
				state.castSince = now;
			}
			else
			{
				final Skill casting = npc.getLastSkillCast();
				final long limit = (casting == null) ? CAST_WATCHDOG_FLOOR_MS : Math.max(CAST_WATCHDOG_FLOOR_MS, casting.getHitTime() + casting.getCoolTime() + CAST_WATCHDOG_MARGIN_MS);
				if ((now - state.castSince) > limit)
				{
					npc.abortCast();
					state.castSince = 0;
				}
			}
			return true;
		}
		state.castSince = 0;

		// Sent ahead to a destination by a "go to X" order: wait there for the leader instead of being pulled
		// back by the follow watchdog. Resume normal behaviour once the leader arrives near us (or grace elapses).
		if (state.travelUntil > now)
		{
			if (npc.calculateDistance2D(owner) > REGROUP_RANGE)
			{
				return true; // hold at the destination
			}
			state.travelUntil = 0;
		}

		// Proactive state call: a member that gets low says so, once per dip (re-armed after recovering) - a real
		// player announces trouble instead of silently dying.
		final int hpPercent = npc.getCurrentHpPercent();
		if (!state.lowHpBarked && (hpPercent < LOW_HP_BARK_PERCENT) && npc.isInCombat())
		{
			state.lowHpBarked = true;
			bark(state, "Your HP just dropped to " + hpPercent + "% mid-fight. Call it out to your party in one very short line.", "getting low here");
		}
		else if (state.lowHpBarked && (hpPercent >= LOW_HP_BARK_CLEAR))
		{
			state.lowHpBarked = false;
		}

		// Emergency scroll rez, ahead of the support/combat split so a pure-DPS member reaches it too: only fires when
		// the party has no living member that can cast Resurrection itself (an all-DPS group, or a group whose only
		// healer is also down). A healer-led party never gets here - partyHasLiveSkillRezzer short-circuits - and the
		// shared claim keeps a single phantom on each corpse, so scrolls are never spammed.
		if (tryScrollRez(state, now))
		{
			return true;
		}

		if (_currentTrading == state) {
			switch (state.tradingState) {
				case IS_TRADING -> {
					return true;
				}
				case FINISHED -> {
					state.lootingSessionItems = null;
					deliver(state, "thank you for your time boss o7");
					state.tradingState = TradingState.NOT_TRADING;
					if (_pendingTrades.isEmpty()) {
						_currentTrading = null;
					} else {
						_pendingTrades.removeFirst();
						_currentTrading = _pendingTrades.isEmpty() ? null : _pendingTrades.getFirst();
					}
					if (_currentTrading != null) {
						if (_currentTrading.tradingState == TradingState.NOT_TRADING)
						{
							PhantomExchangeManager.newInstance().runTrade(
									_currentTrading.npc,
									owner,
									_currentTrading.lootingSessionItems
							);
						}
					}
					return true;
				}
				case CANCELLED -> {
					deliver(state, "not now, got you, back to the grind");
					state.tradingState = TradingState.NOT_TRADING;
					return true;
				}
			}
		}

		if (state.isSupport())
		{
			supportTick(state, now);
		}
		else
		{
			combatTick(state);
		}
		return true;
	}

	/**
	 * True if {@code party} still has a living member that can cast Resurrection (skill {@link #RES_SKILL_ID}) itself -
	 * the natural rezzer, human or phantom. A scroll-carrier only steps in when this is {@code false}, so the scroll
	 * stays a genuine last resort and the healer keeps doing the rezzing whenever one is up.
	 */
	private boolean partyHasLiveSkillRezzer(Party party)
	{
		if (party == null)
		{
			return false;
		}
		for (Player member : party.getMembers())
		{
			if (!member.isDead() && (member.getKnownSkill(RES_SKILL_ID) != null))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Fallback rez with a Scroll of Resurrection (item {@link #RES_SCROLL_ID} -> skill 2014), for a party that has no
	 * living natural rezzer. Every recruit carries a small stack (see {@code PhantomManager} gear-up). Prefers a real
	 * Resurrection whenever the party still has one alive; only an all-DPS group, or one whose healer is also down,
	 * ever burns a scroll. Reuses {@link #findResTarget} and the shared {@link PhantomBuffs} claim so exactly one
	 * phantom raises a given corpse. The claim is held across the scroll's long cast so a second scroll is never spent
	 * on the same body.
	 * @return {@code true} if the phantom committed this tick to closing on, or scroll-rezzing, a corpse
	 */
	private boolean tryScrollRez(Member state, long now)
	{
		final Player npc = state.npc;
		final Player owner = state.owner;
		if ((owner == null) || npc.isDead() || npc.isCastingNow())
		{
			return false;
		}
		final Party party = owner.getParty();
		if ((party == null) || partyHasLiveSkillRezzer(party))
		{
			return false; // a natural rezzer is up (or will be, once raised) - leave the scrolls in the bag
		}
		final Item scroll = npc.getInventory().getItemByItemId(RES_SCROLL_ID);
		if (scroll == null)
		{
			return false; // out of scrolls
		}
		final Player corpse = findResTarget(state, now); // a fallen human first, then a bot member; honours the claim
		if (corpse == null)
		{
			return false;
		}
		if (npc.calculateDistance2D(corpse) > RES_SCROLL_CAST_RANGE)
		{
			// Close to scroll range first (skill 2014 reaches 400); the cast lands on a later tick once in range.
			if (!npc.isMoving())
			{
				npc.setRunning();
				npc.getAI().setIntention(Intention.MOVE_TO, new Location(corpse.getX(), corpse.getY(), corpse.getZ()));
			}
			return true;
		}
		if (!readyToCast(npc))
		{
			return true; // getting up first; the scroll cast lands on the next tick
		}
		if (!PhantomBuffs.claimRes(corpse.getObjectId(), npc.getObjectId(), RES_SCROLL_CLAIM_MS))
		{
			return false; // a different rezzer already has this corpse
		}
		final IItemHandler handler = ItemHandler.getInstance().getHandler(scroll.getEtcItem());
		if (handler == null)
		{
			return false;
		}
		dbg("SCROLL-RES " + npc.getName() + " rezzing " + roleLabel(corpse) + " '" + corpse.getName() + "'");
		npc.setTarget(corpse); // skill 2014 targets PC_BODY, and the item handler checks the skill condition against the current target
		handler.onItemUse(npc, scroll, false);
		return true;
	}

	// ===== Combat roles: assist the leader's target, or free-hunt =====

	private void combatTick(Member state)
	{
		final Player npc = state.npc;
		final Player owner = state.owner;

		// SWS/BD: keeping the songs/dances running beats swinging - it's the whole point of the class, and the
		// 2-minute recast loop is legitimate to maintain mid-fight (unlike 20-minute buffs). Runs in both assist
		// and free-hunt modes.
		if (maintainSongs(state))
		{
			return;
		}
		// TANK panic button: pop Ultimate Defense when critically low and still being hit.
		if ((state.role == PartyRole.TANK) && maybeSurvival(state))
		{
			return;
		}
		// ARCHER skill discipline: park the skill bar when MP runs low (plain soulshotted auto-shots keep firing
		// for free), restore it once recovered.
		if (state.role == PartyRole.ARCHER)
		{
			manageArcherSkills(state);
			if (manageArcherMp(state))
			{
				return; // out of MP for even a bow shot - hold fire and let it regenerate
			}
		}

		if (state.role == PartyRole.BOUNTY_HUNTER && state.fieldSweepingInProcess)
		{
			runSweepOnSelectedCorpses(state);
			return;
		}

		// Charge classes bank their sonic/force energy while settled and out of combat, so a pull OPENS prepared
		// instead of building reactively - an experienced player taps up charges before engaging, and Interlude
		// charge energy persists ~10 minutes so one pre-charge carries many pulls. Only when standing near the
		// leader and not already fighting; mid-fight charge upkeep is the playstyle rotation's job. Whether the
		// class even has a builder is decided by the engine (non-charge classes fall straight through).
		if (!npc.isInCombat() && (npc.calculateDistance2D(owner) <= SUPPORT_RANGE) && prepCharges(state))
		{
			return;
		}

		// Camp-and-pull overrides both assist and free-hunt: the party holds at a fixed anchor and fights only what
		// the puller brings in. The songs/survival/archer upkeep above still applies (a camp fighter still sings,
		// pops UD, etc.), so the camp branch sits below it.
		final Camp camp = _camps.get(owner.getObjectId());
		if (camp != null)
		{
			campTick(state, camp);
			return;
		}

		if (state.assist)
		{
			final WorldObject t = owner.getTarget();
			// Assist only real, legal mob targets. A forbidden target the owner clicked - a fake-player shopkeeper
			// (attacking it flags the phantom for PvP, the "seller got nuked in town" bug), a treasure box, or a quest
			// monster - is never assisted, exactly as it is never picked while free-hunting.
			final boolean haveTarget = (t instanceof Monster) && !((Monster) t).isDead() && !PhantomManager.isPhantomForbiddenTarget((Monster) t) && (owner.calculateDistance2D(t) <= ASSIST_MAX_RANGE);

			// Raid pull control: against a RAID target the party HOLDS until the tank initiates. The tank engages only
			// when ordered ("tank attack"); once it has the boss's threat the rest of the party is released to assist
			// (and can then freely switch between the boss and its adds). Normal mobs are unaffected. When no raid is
			// targeted AND none is engaged near the party, the fight is over - re-arm the hold for the next pull.
			final boolean raidTarget = haveTarget && ((Monster) t).isRaid();
			if (!raidTarget)
			{
				// Re-arm the hold once the fight is over, but only pay for the raid scan if we're actually in a pull
				// state (so normal farming, the common case, never scans here).
				if ((state.pullOrdered || hasRaidRelease(owner)) && (engagedRaid(state) == null))
				{
					state.pullOrdered = false;
					clearRaidRelease(owner);
				}
			}
			Monster gatedFallback = null;
			if (raidTarget && !mayAttackRaid(state, (Monster) t))
			{
				// The leader's raid target is still gated - but don't just idle if there is something this member
				// may legally fight instead (an execute-range add, a minion on the leader or on this member, a
				// released target). Standing down while the raid chewed the party was most of the second wipe.
				gatedFallback = (state.role != PartyRole.TANK) ? raidFallbackTarget(state) : null;
				if (gatedFallback == null)
				{
					holdForPull(state); // wait near the leader, don't feed the raid yet
					return;
				}
			}

			// Raid tactics for a DPS member. (a) Kill order: while the boss has live minions/adds up, clear the adds
			// first (only adds the tank already holds/are released, so this never pulls an untanked add). (b) Ranged
			// step-out: an archer/nuker breaks out of a telegraphed boss AoE before it lands, then resumes fire.
			Monster focus = (gatedFallback != null) ? gatedFallback : (haveTarget ? (Monster) t : null);
			if (raidTarget && isDps(state.role))
			{
				if (isRanged(state.role) && avoidAoe(state))
				{
					return; // get clear of the AoE before resuming fire
				}
				final Monster add = raidKillOrderTarget(state, (Monster) t);
				if (add != null)
				{
					focus = add;
				}
			}

			// The TANK holds and hits the raid BOSS itself, not whatever add the player happens to be shooting - so it
			// melees the boss (sustaining the hate that keeps the boss off the squishies) while the DPS burn the adds.
			// Only once it's allowed to engage the raid (after the "tank attack" order), so the pre-pull hold still works.
			if (state.role == PartyRole.TANK)
			{
				final Monster boss = engagedBoss(state);
				if ((boss != null) && mayAttackRaid(state, boss))
				{
					focus = boss;
				}
			}

			// Leader dead or targetless while a raid fight is still running: fight what we legally can (the
			// fallback scan) instead of standing in the fire doing nothing - the party must not go passive the
			// moment the human dies. Gated on the mood's engaged-raid handle so normal farming never pays the scan.
			if ((focus == null) && (state.role != PartyRole.TANK))
			{
				final PartyMood mood = mood(owner);
				if ((mood != null) && (mood.engagedRaid != null))
				{
					focus = raidFallbackTarget(state);
				}
			}

			// Self-preservation for the squishy ranged roles: something has latched onto us and no tank is holding
			// it. Usually it controls/steps away and we carry on shooting the party's target; only when that is
			// losing does the attacker become the focus. Gated on actually HAVING a party target - kiting exists to
			// protect focus fire, so with nothing to assist there is nothing to protect and the block below simply
			// engages the attacker instead.
			if ((focus != null) && peels(state))
			{
				final Monster onMe = attackerOnMe(state, focus);
				if (onMe != null)
				{
					if (shouldEscalatePeel(state, onMe))
					{
						focus = onMe;
					}
					else if (peelDefend(state, onMe))
					{
						return; // disengaging this tick - engaging now would cancel the move
					}
				}
			}

			// Nothing to assist - the leader's target just died, or they have none. The "don't break focus fire"
			// rule that keeps a member on the party's target has nothing to protect here, so with no kill target
			// EVERY role, melee included, simply kills whatever is on it. Without this a member walks back to its
			// formation slot between pulls while a mob chews on it, which reads as completely broken.
			if (focus == null)
			{
				focus = attackerOnMe(state, null, false);
			}

			// Engage the chosen focus - the shared fight logic (raid aggro-easing, nuker CC, caster range-hold, tank
			// threat, dagger rear, archer positioning, auto-attack upkeep), reused by camp mode. Returns false only
			// when a mage just ran dry, so we fall through to the rest path instead of meleeing on an empty bar.
			if ((focus != null) && engageFocus(state, focus))
			{
				return;
			}

			// if the battle is over, then it's probably a good time to look for spoiled mobs and spoil them
			if (focus == null) {
				if ((state.role == PartyRole.BOUNTY_HUNTER) && checkForSweepableMobs(state))
				{
					return;
				}
			}
			// Nothing to assist (or a nuker that just ran dry): a caster low on MP sits to recover when safe;
			// otherwise stick with the leader.
			if (restForMp(state))
			{
				return;
			}
			if (state.following)
			{
				driveFollow(state, owner);
			}
			return;
		}

		// Free-hunt mode: AutoPlay drives target selection.
		// Ranged self-preservation runs FIRST and regardless of what we're already shooting: a member that is busy
		// killing something else while a second mob eats it is exactly the "he ignored the thing hitting him" case.
		// Normally this controls/kites and returns null (carry on with AutoPlay's pick); a returned target means
		// kiting is losing and it should fight that instead.
		if (peels(state))
		{
			// In free hunt the "focus" is simply whatever AutoPlay currently has us on.
			final Monster onMe = attackerOnMe(state, (npc.getTarget() instanceof Monster) ? (Monster) npc.getTarget() : null);
			if (onMe != null)
			{
				if (shouldEscalatePeel(state, onMe))
				{
					standIfSitting(npc);
					npc.setTarget(onMe);
					if (!state.role.mage)
					{
						npc.setRunning();
						npc.getAI().setIntention(Intention.ATTACK, onMe);
					}
					tryPlaystyle(state, onMe);
					return;
				}
				if (peelDefend(state, onMe))
				{
					return;
				}
			}
		}
		// Belt-and-braces retaliation for the roles the peel logic doesn't cover (melee): if a mob is beating on this
		// member and it still has no target, engage the attacker directly - the scanner can be slow or shy
		// (respectful-hunt edge cases), and a member standing there being hit without answering reads as broken.
		if ((npc.getTarget() == null) && !npc.isAttackingNow() && !npc.isCastingNow())
		{
			for (Monster mob : World.getInstance().getVisibleObjectsInRange(npc, Monster.class, DANGER_RANGE))
			{
				if (!mob.isDead() && (mob.getTarget() == npc))
				{
					standIfSitting(npc);
					npc.setTarget(mob);
					if (!state.role.mage)
					{
						npc.setRunning();
						npc.getAI().setIntention(Intention.ATTACK, mob); // a mage just holds the target - AutoUse nukes it
					}
					break;
				}
			}
		}
		// With this member's offensive AutoUse list parked (the playstyle engine owns it), free-hunt still gets
		// its skills: play the class on whatever AutoPlay/retaliation is currently targeting.
		if (npc.getTarget() instanceof Monster)
		{
			final Monster freeTarget = (Monster) npc.getTarget();
			if (!freeTarget.isDead())
			{
				tryPlaystyle(state, freeTarget);
			}
		}
		// Leash the member back if it wanders off; driveFollow walks it in and teleports if it is very far or stuck.
		if (npc.calculateDistance2D(owner) > LEASH_RANGE)
		{
			PhantomManager.getInstance().setRecruitHunting(npc, false);
			driveFollow(state, owner);
			ThreadPool.schedule(() ->
			{
				if (!npc.isDead() && !state.assist)
				{
					PhantomManager.getInstance().setRecruitHunting(npc, true); // resume hunting once back near the party
				}
			}, 4000);
		}
	}

	private boolean checkForSweepableMobs(Member state)
	{
		final Player npc = state.npc;

		if (!state.scavengerKitLookedUp) {
			state.spoil = npc.getKnownSkill(SPOIL_SKILL_ID);
			state.sweeper = npc.getKnownSkill(SWEEPER_SKILL_ID);
			state.scavengerKitLookedUp = true;
		}

		// Without the Sweeper skill there is nothing to cast on a spoiled corpse; never enter
		// sweeping mode, or runSweepOnSelectedCorpses would dereference a null sweeper.
		if (state.sweeper == null) {
			return false;
		}

		if (!state.fieldSweepingInProcess) {
			List<Monster> sweepableEntities =
					World.getInstance().getVisibleObjectsInRange(npc, Monster.class, DEFAULT_AOE_SWEEP_RADIUS)
							.stream()
							.filter((monster) -> monster.isDead() && monster.getSpoilerObjectId() == npc.getObjectId())
							.collect(Collectors.toCollection(ArrayList::new));

			state.fieldSweepingInProcess = !sweepableEntities.isEmpty();
			if (state.fieldSweepingInProcess) {
				state.targetsToSweep = sweepableEntities;
			}
			return state.fieldSweepingInProcess;
		}

		return false;
	}

	private void runSweepOnSelectedCorpses(Member state)
	{
		Player npc = state.npc;
		Optional<Monster> closestMonsterCorpseOptional = state.targetsToSweep
                .stream()
				.min((monster1, monster2) -> {
                    double distanceToPlayer1 = monster1.calculateDistance2D(npc);
                    double distanceToPlayer2 = monster2.calculateDistance2D(npc);

                    return Double.compare(distanceToPlayer1, distanceToPlayer2);
                });

		 if (closestMonsterCorpseOptional.isPresent())
		 {
			 Monster closestMonsterCorpse = closestMonsterCorpseOptional.get();
			 if (state.nextMonsterCorpseToSweep == closestMonsterCorpse)
			 {
				 npc.setTarget(closestMonsterCorpse);
				 npc.setRunning();
				 npc.getAI().setIntention(Intention.MOVE_TO, closestMonsterCorpse.getLocation());

				 if (npc.calculateDistance2D(closestMonsterCorpse) <= state.sweeper.getCastRange())
				 {
					 npc.getAI().setIntention(Intention.IDLE);
					 npc.doCast(state.sweeper);
					 state.targetsToSweep.remove(closestMonsterCorpse);
					 state.nextMonsterCorpseToSweep = null;
				 }
			 } else
			 {
				 state.nextMonsterCorpseToSweep = closestMonsterCorpse;
			 }
		} else
		{
			state.fieldSweepingInProcess = false;
			state.nextMonsterCorpseToSweep = null;
		}
	}

	/**
	 * Drives a member to fight one chosen {@code focus} mob: the shared engage logic used by both assist mode and
	 * camp mode. Handles raid aggro-easing (a no-op on trash), nuker CC + cast-range hold, the archer target-switch
	 * beat and raid backline, tank threat, dagger rear positioning, and keeping a live auto-attack on the focus.
	 * @return {@code true} if the member is now acting on the focus (caller should return this tick); {@code false}
	 *         only when a mage just ran dry of MP (its target was dropped) so the caller falls through to resting.
	 */
	private boolean engageFocus(Member state, Monster focus)
	{
		final Player npc = state.npc;
		// Never fight inside a peace zone. Mobs don't live in one, so a focus here is a town target (e.g. a fake-player
		// shopkeeper the owner clicked, or a mob chased to the border); attacking it is both illegal for the player and
		// the "phantoms nuked a seller in Gludio" bug. Stand down - dropping the target lets the caller rest/hold.
		if (npc.isInsideZone(ZoneId.PEACE) || focus.isInsideZone(ZoneId.PEACE))
		{
			npc.setTarget(null);
			return false;
		}
		// A DPS that ripped raid aggro off the tank holds fire for a beat so the taunt can land (raid-only; no-op on trash).
		if (isDps(state.role) && easeAggro(state, focus))
		{
			return true;
		}
		if (state.role.mage)
		{
			// A nuker casts from range - it must NEVER be given a physical ATTACK intention (that walked a caster into
			// melee to auto-hit even on a full MP bar). Hold at cast range; AutoUse fires the nukes. Out of MP, drop the
			// target and return false so the caller sits it down to recharge instead of meleeing.
			if (npc.getCurrentMpPercent() >= CASTER_MIN_MP)
			{
				if (maybeCrowdControl(state, focus)) // a loose add on a squishy gets slept/rooted first
				{
					return true;
				}
				standIfSitting(npc);
				npc.setTarget(focus);
				positionCaster(state, focus);
				tryPlaystyle(state, focus); // with AutoUse parked, the engine fires the class's nukes/debuffs
				return true;
			}
			npc.setTarget(null);
			return false;
		}
		// An archer takes a short human beat before swinging onto a NEW target instead of snapping the same tick.
		if ((state.role == PartyRole.ARCHER) && assistSwitchPending(state, focus))
		{
			return true;
		}
		standIfSitting(npc);
		if ((state.role == PartyRole.ARCHER) && focus.isRaid())
		{
			npc.setTarget(focus);
			// Hold inside bow reach (not the 720 backline a bow can't shoot from), tight spread so outer lanes reach too.
			if (positionRaidBackline(state, focus, archerHoldRange(state), ARCHER_BACKLINE_TOLERANCE, ARCHER_SPREAD_STEP))
			{
				return true;
			}
		}
		// A tank actively holds threat; a dagger slides to the rear. If either acted this tick, skip the attack re-issue.
		if ((state.role == PartyRole.TANK) && maintainThreat(state, focus))
		{
			return true;
		}
		if ((state.role == PartyRole.DAGGER) && positionRear(state, focus))
		{
			return true;
		}
		// Class playstyle: the first listed skill whose moment has come (opener from the rear, a banked-charge
		// spender, a pack AoE, a healer-gated limit). Firing one skips the attack re-issue this tick; the swing
		// resumes next tick.
		if (tryPlaystyle(state, focus))
		{
			state.castStuckSince = 0L; // a fresh cast launched this tick - nothing stale for the watchdog below to clear
			return true;
		}
		// Keep a live auto-attack on the focus so a clientless melee/archer keeps plinking with soulshots between skills
		// instead of dropping to IDLE. THE CORE PROBLEM: after a playstyle skill the cast interrupts the melee loop, and
		// the AI is left INTENDING attack on this same focus but with no swing scheduled. Re-issuing setIntention(ATTACK,
		// focus) then does nothing, because CreatureAI.onIntentionAttack treats "already ATTACK on this target" as a
		// no-op (it just sends ActionFailed) instead of relaunching - so the member stands idle taking hits until some
		// other event disturbs its intention (why a "follow" order temporarily un-sticks it, and why this is not tied to
		// MP: it happens after every cast). isAttackingNow() stays true for the whole attack interval (attackEndTime =
		// now + timeBetweenAttacks), so it is false ONLY when the loop is genuinely stopped - exactly this wedge. In that
		// wedged case we poke THINK to relaunch the swing (doAttack self-guards via isAttackDisabled, so it can never
		// swing early); any other case re-engages/retargets normally through setIntention.
		final long now = System.currentTimeMillis();
		boolean casting = npc.isCastingNow();
		if (casting)
		{
			// Safety net for the separate clientless quirk where a cast flag lingers set after the skill finished (the
			// same one restForMp clears with abortCast before sitting). If it outlives any real melee cast, treat it as
			// stuck, clear it, and attack this tick so a stuck flag can never suppress the swing either.
			if (state.castStuckSince == 0L)
			{
				state.castStuckSince = now;
			}
			else if ((now - state.castStuckSince) >= CAST_FLAG_STALE_MS)
			{
				npc.abortCast();
				state.castStuckSince = 0L;
				casting = false;
			}
		}
		else
		{
			state.castStuckSince = 0L;
		}
		if (!casting && (!npc.isAttackingNow() || (npc.getTarget() != focus)))
		{
			npc.setTarget(focus);
			npc.setRunning();
			if ((npc.getAI().getIntention() == Intention.ATTACK) && (npc.getAI().getAttackTarget() == focus))
			{
				// The wedge: setIntention(ATTACK, focus) would no-op here. Poke THINK so onActionThink -> thinkAttack ->
				// doAttack relaunches the interrupted swing.
				npc.getAI().notifyAction(Action.THINK);
				if (DEBUG)
				{
					dbg("ATTACK-RESUME '" + npc.getName() + "' (" + state.role + ") re-launched auto-attack on '" + focus.getName() + "' (" + npc.getCurrentHpPercent() + "% HP / " + npc.getCurrentMpPercent() + "% MP)");
				}
			}
			else
			{
				npc.getAI().setIntention(Intention.ATTACK, focus); // fresh engage or retarget - onIntentionAttack relaunches on its own
			}
		}
		return true;
	}

	// ===== Camp-and-pull (Bucket 3): hold a fixed camp; a named puller drags mobs back to be killed there =====

	/**
	 * One camp-mode tick for a combat member. The party holds at the anchor and fights only what is at the camp; the
	 * designated puller runs out to fetch the next mob when the camp is clear and drags it home.
	 */
	private void campTick(Member state, Camp camp)
	{
		final Player npc = state.npc;
		final long now = System.currentTimeMillis();
		final boolean isPuller = (npc.getObjectId() == camp.pullerId);

		// Retire a finished pull run - delivered to camp, the mob died, or the fetch timed out (mob stuck/unreachable) -
		// and start the rest beat before the next one.
		if (isPuller && (camp.pulling != null) //
			&& (camp.pulling.isDead() //
				|| ((npc.calculateDistance2D(camp.anchor) <= CAMP_ENGAGE_RANGE) && (camp.pulling.calculateDistance2D(camp.anchor) <= CAMP_ENGAGE_RANGE)) //
				|| ((now - camp.pullStartedAt) > PULL_TIMEOUT)))
		{
			camp.pulling = null;
			camp.hauling = false;
			camp.pullTagged = false;
			camp.nextPullAt = now + PULL_INTERVAL;
		}

		final Monster focus = campFocus(state, camp);
		if (focus != null)
		{
			// Something is at the camp - everyone (the puller too, between runs) burns it down here.
			if (engageFocus(state, focus))
			{
				return;
			}
			// a mage that ran dry falls through to rest at camp
		}
		else if (isPuller && (camp.pulling != null))
		{
			haulBack(state, camp); // actively dragging one home
			return;
		}
		else if (isPuller && (now >= camp.nextPullAt))
		{
			runPull(state, camp, now); // camp clear and off the rest beat: go fetch the next mob
			return;
		}
		// Idle camp fighter, a resting puller, or a dry mage: recover MP when safe, then hold at the anchor.
		if (restForMp(state))
		{
			return;
		}
		holdAtCamp(state, camp.anchor);
	}

	/** The nearest live, non-raid mob within {@link #CAMP_ENGAGE_RANGE} of the camp anchor - what the camp fights. */
	private Monster campFocus(Member state, Camp camp)
	{
		final Player npc = state.npc;
		Monster best = null;
		double bestD = Double.MAX_VALUE;
		// Scan around the member a little wider than the engage radius (it stands up to a formation slot off the anchor)
		// then filter to mobs actually at the camp, so the party never wanders off to a mob the puller didn't bring in.
		for (Monster mob : World.getInstance().getVisibleObjectsInRange(npc, Monster.class, CAMP_ENGAGE_RANGE + (2 * FORMATION_MAX_DIST)))
		{
			if (mob.isDead() || mob.isRaid() || PhantomManager.isPhantomForbiddenTarget(mob))
			{
				continue;
			}
			final double d = mob.calculateDistance2D(camp.anchor);
			if ((d <= CAMP_ENGAGE_RANGE) && (d < bestD))
			{
				bestD = d;
				best = mob;
			}
		}
		return best;
	}

	/** The puller heads out to tag the nearest free mob and start dragging it home (or recovers first if hurt). */
	private void runPull(Member state, Camp camp, long now)
	{
		final Player npc = state.npc;
		if (npc.getCurrentHpPercent() < PULL_RETREAT_HP) // too hurt to pull - recover at camp first
		{
			if (restForMp(state))
			{
				return;
			}
			holdAtCamp(state, camp.anchor);
			return;
		}
		final Monster prey = findPullTarget(state, camp);
		if (prey == null)
		{
			camp.nextPullAt = now + PULL_INTERVAL; // nothing free nearby - wait a beat and rescan
			if (restForMp(state))
			{
				return;
			}
			holdAtCamp(state, camp.anchor);
			return;
		}
		camp.pulling = prey;
		camp.hauling = false;
		camp.pullTagged = false;
		camp.pullStartedAt = now;
		standIfSitting(npc);
		npc.setTarget(prey);
		npc.setRunning();
		npc.getAI().setIntention(Intention.ATTACK, prey); // run out and tag it (a skill opener lands once in range, see haulBack)
		bark(state, "You're the camp's puller and you're running out to grab " + prey.getName() + " to bring back to the party. Say it in one very short line.", "pulling, get ready");
	}

	/** Drag the current pull home: grab another free mob if a multi-pull has room, else leg it back to the anchor. */
	private void haulBack(Member state, Camp camp)
	{
		final Player npc = state.npc;
		final Monster prey = camp.pulling;
		if (prey == null)
		{
			return;
		}
		// A pull-tag skill (tank taunt / nuker tag) is still going out - let it finish before any branch below
		// re-issues a move or attack that would interrupt it. Physical body-pulling never trips this (auto-attack
		// is not a cast), so it only holds for the ranged opener.
		if (npc.isCastingNow())
		{
			return;
		}
		// Multi-pull: chain another free mob if we still have room and one is right beside us.
		if (mobsHating(npc) < camp.pullSize)
		{
			final Monster extra = findPullTarget(state, camp);
			if ((extra != null) && (npc.calculateDistance2D(extra) <= PULL_GRAB_RANGE))
			{
				camp.hauling = false;
				standIfSitting(npc);
				npc.setTarget(extra);
				npc.setRunning();
				npc.getAI().setIntention(Intention.ATTACK, extra);
				return;
			}
		}
		// Have a load on us (or nothing else close): run back to camp, dragging whatever aggroed. Issue the MOVE_TO
		// once (not every tick - that re-paths and stutters the run) and only re-nudge it if the run stalls.
		// camp.pullTagged: once a ranged opener has been thrown, head home IMMEDIATELY even if the tag's hate has not
		// registered yet this tick - otherwise the body-pull branch below would run a caster INTO melee in that gap
		// (the "stopped at range, then got dragged onto the mob mid/after cast and died" bug).
		if ((mobsHating(npc) > 0) || (prey.getMostHated() == npc) || camp.pullTagged)
		{
			if (!camp.hauling || !npc.isMoving())
			{
				npc.setRunning();
				npc.getAI().setIntention(Intention.MOVE_TO, new Location(camp.anchor.getX(), camp.anchor.getY(), camp.anchor.getZ()));
				camp.hauling = true;
			}
			return;
		}
		// Nothing has latched on yet. A ranged puller opens with a skill once it has closed to cast range - a TANK
		// taunts, a nuker fires its use="PULL" tag - so it does not run into melee to body-pull. A puller still out of
		// range, low on MP, or with no pull skill falls through and body-pulls exactly as before.
		if (tryPullTag(state, camp, prey))
		{
			return;
		}
		camp.hauling = false;
		standIfSitting(npc);
		npc.setTarget(prey);
		npc.setRunning();
		npc.getAI().setIntention(Intention.ATTACK, prey);
	}

	/**
	 * The puller's ranged pull opener, fired at most once per pull run and only once the puller is in cast range: a
	 * TANK grabs the mob with the manager-owned taunt (Aggression - single-target, long range, hate without needing
	 * to land damage); any other class fires the cheap single-target nuke its playstyle marks {@code use="PULL"}. This
	 * is what makes a nuker shoot the mob instead of running into melee, and a tank pull with hate instead of its face.
	 * Returns {@code false} - so the caller body-pulls as before - when the puller has no pull skill, is still closing,
	 * is low on MP, or the opener is on cooldown / refused by the core.
	 * @return {@code true} if a skill cast launched this tick (caller skips the body-pull attack re-issue)
	 */
	private boolean tryPullTag(Member state, Camp camp, Monster prey)
	{
		if (camp.pullTagged) // already opened this run - the body-pull carries it home from here
		{
			return false;
		}
		final Player npc = state.npc;
		if (npc.isCastingNow()) // a tag launched on an earlier tick is still going out - let it finish
		{
			return true;
		}
		// TANK: pull with Aggression, which is manager-owned and so never listed in the playstyle. Resolve it lazily
		// (the tank may not have taunted yet this session) exactly as maintainThreat does.
		if (state.role == PartyRole.TANK)
		{
			resolveTaunts(state);
			if (castable(npc, state.aggression) && (npc.calculateDistance2D(prey) <= state.aggression.getCastRange()))
			{
				standIfSitting(npc);
				// Drop the run-out ATTACK pursuit BEFORE casting: it seeks melee range, and left active it drags the
				// puller onto the mob during the cast. Stand where we are, tag, then haulBack runs the mob home.
				npc.getAI().setIntention(Intention.IDLE);
				npc.setTarget(prey);
				npc.doCast(state.aggression);
				if (npc.isCastingNow() || npc.isCastingSimultaneouslyNow())
				{
					camp.pullTagged = true;
					return true;
				}
			}
			return false; // out of range / on cooldown / core refused - keep closing and body-pull, retry next tick
		}
		// Everyone else: the class's use="PULL" tag (a cheap single-target nuke), if it has one and is in range with
		// mana to spare. Melee and archers list none, so they body-pull as before (an archer's bow auto-shot already
		// tags from range).
		final PhantomPlaystyleEngine.CastAction action = PhantomPlaystyleEngine.pickPullTag(npc, prey, state.play, mpReserve(state.role), state.role.name());
		if (action == null)
		{
			return false;
		}
		standIfSitting(npc);
		// Drop the run-out ATTACK pursuit BEFORE casting (see the tank branch): for a caster it seeks melee range, so
		// left active it drags her onto the mob mid-cast where she takes hits. Stand and tag from range; haulBack then
		// runs the tagged mob back to camp.
		npc.getAI().setIntention(Intention.IDLE);
		npc.setTarget(action.target);
		npc.doCast(action.skill);
		if (npc.isCastingNow() || npc.isCastingSimultaneouslyNow())
		{
			camp.pullTagged = true;
			return true;
		}
		return false; // core refused the cast (range/interrupt/target gone) - body-pull this tick, retry the tag next
	}

	/** Nearest live, non-raid mob within pull range of the anchor that isn't already on the puller or the party. */
	private Monster findPullTarget(Member state, Camp camp)
	{
		final Player npc = state.npc;
		final Player owner = state.owner;
		Monster best = null;
		double bestD = Double.MAX_VALUE;
		for (Monster mob : World.getInstance().getVisibleObjectsInRange(npc, Monster.class, PULL_SEARCH_RANGE))
		{
			if (mob.isDead() || mob.isRaid() || (mob.getMostHated() == npc) || PhantomManager.isPhantomForbiddenTarget(mob))
			{
				continue;
			}
			if ((mob.calculateDistance2D(camp.anchor) > PULL_SEARCH_RANGE) || isPartyCreature(owner, mob.getMostHated()))
			{
				continue; // too far from camp, or already engaged with the party - leave it
			}
			final double d = npc.calculateDistance2D(mob);
			if (d < bestD)
			{
				bestD = d;
				best = mob;
			}
		}
		return best;
	}

	/** How many live mobs are currently hating this member (the size of the load it's dragging). */
	private int mobsHating(Player npc)
	{
		int n = 0;
		for (Monster mob : World.getInstance().getVisibleObjectsInRange(npc, Monster.class, PULL_SEARCH_RANGE))
		{
			if (!mob.isDead() && (mob.getMostHated() == npc))
			{
				n++;
			}
		}
		return n;
	}

	/** Holds a member in its formation slot around a fixed camp anchor: idle when settled, walk in when off, teleport if very far. */
	private void holdAtCamp(Member state, Location anchor)
	{
		final Player npc = state.npc;
		if (npc.isSitting())
		{
			return;
		}
		final int sx = anchor.getX() + (int) (Math.cos(state.formAngle) * state.formDist);
		final int sy = anchor.getY() + (int) (Math.sin(state.formAngle) * state.formDist);
		final Location slot = GeoEngine.getInstance().getValidLocation(npc, new Location(sx, sy, anchor.getZ()));
		if (npc.calculateDistance2D(slot) <= FORMATION_TOLERANCE)
		{
			state.stuckTicks = 0;
			if (npc.isMoving())
			{
				npc.getAI().setIntention(Intention.IDLE); // settled - stop shuffling
			}
			return;
		}
		// Far from the camp (walked out on a pull, or the anchor was planted a way off): teleport in if really far.
		if (npc.calculateDistance2D(anchor) > CATCHUP_TP_RANGE)
		{
			npc.abortCast();
			npc.teleToLocation(new Location(slot.getX() + Rnd.get(-40, 40), slot.getY() + Rnd.get(-40, 40), slot.getZ()));
			npc.onTeleported();
			npc.broadcastUserInfo();
			state.stuckTicks = 0;
			return;
		}
		if (!npc.isMoving())
		{
			npc.setRunning();
			npc.getAI().setIntention(Intention.MOVE_TO, slot);
		}
	}

	/** {@code true} if {@code who} is the leader or one of its recruited party members (so a mob on it is already engaged). */
	private static boolean isPartyCreature(Player owner, Creature who)
	{
		if ((who == null) || !who.isPlayer() || (owner == null) || !owner.isInParty())
		{
			return false;
		}
		for (Player member : owner.getParty().getMembers())
		{
			if (member == who)
			{
				return true;
			}
		}
		return false;
	}

	/** Plants a camp at the leader's current spot and drops every member out of free-hunt into it. @return {@code true} if a new camp was created (so only one member announces it). */
	private boolean startCamp(Player owner)
	{
		if (owner == null)
		{
			return false;
		}
		final int id = owner.getObjectId();
		if (_camps.containsKey(id))
		{
			return false; // already camping - idempotent, so a party-wide "camp" doesn't get eight confirmations
		}
		_camps.put(id, new Camp(new Location(owner.getX(), owner.getY(), owner.getZ())));
		for (Member m : _members.values())
		{
			if ((m.owner == owner) && m.partied)
			{
				setFree(m, false); // out of free-hunt; campTick drives movement/targeting from here
				m.following = true;
			}
		}
		return true;
	}

	/** Makes the addressed member the camp's puller (planting a camp first if there isn't one), sized by "pull N". */
	private void startCampPuller(Member state, int size)
	{
		final Player owner = state.owner;
		// A support runs through supportTick, which has no fetch logic - it would just stand at camp and never pull.
		// Refuse instead of silently doing nothing, so the player picks a damage dealer.
		if (state.isSupport())
		{
			deliver(state, "i'm support, better to have a dd pull");
			return;
		}
		startCamp(owner); // "<name> pull" with no camp yet plants one at the leader's feet so the order just works
		final Camp camp = _camps.get(owner.getObjectId());
		if (camp == null)
		{
			return;
		}
		camp.pullerId = state.npc.getObjectId();
		camp.pullSize = Math.max(1, Math.min(MAX_PULL_SIZE, size));
		camp.nextPullAt = 0; // go now
		deliver(state, (camp.pullSize > 1) ? ("on it, pulling " + camp.pullSize + " at a time") : "on it, i'll pull");
	}

	/** Ends the owner's camp. @return {@code true} if a camp was actually cleared. */
	private boolean stopCamp(Player owner)
	{
		return (owner != null) && (_camps.remove(owner.getObjectId()) != null);
	}

	/** Extracts a requested pull size ("pull 3") from an order, clamped to [1, {@value #MAX_PULL_SIZE}]; default 1. */
	private static int parsePullSize(String text)
	{
		for (int n = MAX_PULL_SIZE; n >= 2; n--)
		{
			if (text.contains(Integer.toString(n)))
			{
				return n;
			}
		}
		return 1;
	}

	/** {@code true} if {@code word} appears in {@code text} as a whole word or word-prefix ("pull" matches "pulling"). */
	private static boolean containsWord(String text, String word)
	{
		return Pattern.compile("\\b" + Pattern.quote(word)).matcher(text).find();
	}

	/**
	 * Raid pull gate. Against a RAID target: the TANK attacks only once ordered ("tank attack"). Every raid mob/add
	 * is released separately only after that tank is most-hated for that exact target. This is deliberately stricter
	 * than a whole-fight release: Zaken-style fights can have a boss, Inferior, and Bats active together, and ranged
	 * DPS should not unload on an untanked add just because the tank briefly owned the first target.
	 */
	/**
	 * Records the raid-gate verdict and its reason code on the member (DEBUG bookkeeping only) and returns the verdict
	 * unchanged. Every {@link #mayAttackRaid} exit funnels through here, so the periodic snapshot can report exactly
	 * WHY a member is holding fire instead of only that it is idle. Has no effect on behaviour: the side effects that
	 * already ran before the return are untouched, and when {@link #DEBUG} is off this is a plain pass-through.
	 */
	private boolean gate(Member state, boolean allowed, String reason)
	{
		if (DEBUG)
		{
			state.raidGateReason = (allowed ? "GO/" : "HOLD/") + reason;
		}
		return allowed;
	}

	private boolean mayAttackRaid(Member state, Monster raid)
	{
		final Player owner = state.owner;
		final int ownerId = owner.getObjectId();
		// Raid-curse guard (checked first, ahead of even an "all attack" order - the curse is a hard game mechanic, not
		// a risk the player can waive). A member more than RAID_CURSE_LEVEL_GAP levels above this raid mob would only
		// paralyse itself the instant it hit, so it never engages. Mirrors the core curse condition exactly (per
		// attacker, respects the DisableRaidCurse config and the mob's own giveRaidCurse flag; minions delegate to
		// their master). Uses THIS member's level, since level spread can put one recruit over the line alone.
		if (!NpcConfig.RAID_DISABLE_CURSE && raid.giveRaidCurse() && (state.npc.getLevel() > (raid.getLevel() + RAID_CURSE_LEVEL_GAP)))
		{
			return gate(state, false, "RAID_CURSE_GAP(lvl " + state.npc.getLevel() + " vs boss " + raid.getLevel() + ")");
		}
		if (_released.contains(ownerId))
		{
			return gate(state, true, "ALL_ATTACK_ORDER"); // explicit "all attack" order - the player accepted the risk
		}
		if (state.role == PartyRole.TANK)
		{
			if (!state.pullOrdered)
			{
				return gate(state, false, "TANK_NOT_ORDERED"); // tank waits for the "tank attack" order before pulling
			}
			if (raid.getMostHated() == state.npc)
			{
				_releasedRaidTargets.add(raidKey(ownerId, raid.getObjectId()));
			}
			return gate(state, true, "TANK_PULLING");
		}

		// Execute rule: a raid minion in its last sliver is finished by anyone.
		if (raid.isRaidMinion() && (raid.getCurrentHpPercent() <= MINION_EXECUTE_PERCENT))
		{
			return gate(state, true, "MINION_EXECUTE");
		}
		// Defend the leader: a minion whose most-hated is the PLAYER is attackable by everyone - the party helps
		// the human kill the thing that is chasing them (second Ruell attempt: the party watched, gated, while the
		// leader soloed a Wind add from 19% down and eventually died to it).
		if (raid.isRaidMinion() && (raid.getMostHated() == owner))
		{
			return gate(state, true, "DEFEND_LEADER");
		}
		final Member tank = findTankState(state);
		// Raid self-defense: this raid mob is ON this member (most-hated) - standing still sheds no hate in L2,
		// so holding fire while it beats on you is pure loss. Always allowed against a minion; against the BOSS
		// only when there is no living tank to reclaim it (with a tank up, the short aggro-ease + taunt hand it
		// back, which beats out-damaging the taunt).
		if ((raid.getMostHated() == state.npc) && (raid.isRaidMinion() || (tank == null) || tank.npc.isDead()))
		{
			return gate(state, true, "SELF_DEFENSE");
		}
		if ((tank == null) || tank.npc.isDead())
		{
			// No recruited phantom tank, but the human leader may be personally tanking. When that human tank actually
			// holds this raid mob's hate, release DPS now (and mark the target released for the pull, mirroring the
			// phantom-tank owns-target path below) instead of making the party wait out the no-tank fail-open timeout.
			// Gated on the human being most-hated so a tank-class leader who is NOT holding aggro still falls through to
			// the timeout rather than freezing DPS behind a tank that is not doing its job.
			final Player humanTank = humanRaidTank(state);
			if ((humanTank != null) && (raid.getMostHated() == humanTank))
			{
				_releasedRaidTargets.add(raidKey(ownerId, raid.getObjectId()));
				_noTankSince.remove(ownerId);
				return gate(state, true, "HUMAN_TANK_AGGRO");
			}
			final long noTankNow = System.currentTimeMillis();
			final Long since = _noTankSince.putIfAbsent(ownerId, noTankNow);
			if ((since != null) && ((noTankNow - since) >= NO_TANK_FAILOPEN_MS))
			{
				// No living tank for a while now - the corpse grace window expired unraised, or the party never
				// had one. Fail open instead of holding every non-tank member forever with no visible way out.
				if (_released.add(ownerId))
				{
					deliver(state, "no tank up, going in - say 'hold' to stop");
				}
				return gate(state, true, "NO_TANK_FAILOPEN");
			}
			final long waited = (since == null) ? 0 : (noTankNow - since);
			return gate(state, false, "NO_TANK_WAIT(" + (waited / 1000) + "/" + (NO_TANK_FAILOPEN_MS / 1000) + "s)");
		}
		_noTankSince.remove(ownerId);
		final long now = System.currentTimeMillis();
		if ((tank.recoveryUntil > now) && (tank.npc.getCurrentHpPercent() < POST_RES_TANK_READY_PERCENT))
		{
			return gate(state, false, "TANK_RECOVERING"); // tank just stood up; let support heal it and let taunts land before DPS resumes
		}
		if (tank.npc.getCurrentHpPercent() >= POST_RES_TANK_READY_PERCENT)
		{
			tank.recoveryUntil = 0;
		}

		final long key = raidKey(ownerId, raid.getObjectId());
		final Creature hated = raid.getMostHated();
		if (hated == tank.npc)
		{
			_releasedRaidTargets.add(key);
			return gate(state, true, "TANK_HAS_AGGRO");
		}
		// An ADD (raid minion), once opened, stays free for EVERY dps regardless of who currently holds it. The old
		// rule re-locked the add the moment a party member was most-hated on it - but a melee DD out-aggros the tank
		// on an add almost instantly, which (a) locked every OTHER DD out of that add (they all target the same
		// lowest-HP add, so only one ever fought and the rest stood idle) and (b) bounced the holder in and out as the
		// tank re-taunted (the "run to the add, run back, repeat" behaviour). The main BOSS keeps the strict rule so
		// dps still hold off it while it is loose on a squishy and the tank reclaims it.
		if (_releasedRaidTargets.contains(key) && (raid.isRaidMinion() || (hated == null) || !isPartyMember(state, hated)))
		{
			return gate(state, true, "TARGET_RELEASED");
		}
		// non-tank waits for the tank to own this exact raid mob/add - name who currently holds it, the usual culprit
		return gate(state, false, "BOSS_LOCKED(hate=" + describe(hated) + ")");
	}

	private boolean hasRaidRelease(Player owner)
	{
		if (owner == null)
		{
			return false;
		}
		final int ownerId = owner.getObjectId();
		if (_released.contains(ownerId))
		{
			return true;
		}
		for (Long key : _releasedRaidTargets)
		{
			if (raidOwnerId(key) == ownerId)
			{
				return true;
			}
		}
		return false;
	}

	private void clearRaidRelease(Player owner)
	{
		if (owner == null)
		{
			return;
		}
		final int ownerId = owner.getObjectId();
		_released.remove(ownerId);
		_releasedRaidTargets.removeIf(key -> raidOwnerId(key) == ownerId);
		_noTankSince.remove(ownerId); // fresh countdown for the next pull, not whatever was left over from the last one
	}

	/**
	 * Tank-death re-lock: pull the BOSS back behind the gate but keep already-released minions attackable. The old
	 * full {@link #clearRaidRelease} here starved the party to death in the Ruell wipe - every tank death (it died
	 * three times) re-locked every add, so DPS stood idle waiting for a tank that lived seconds at a time, and the
	 * fight produced almost no damage while the adds farmed the party. The adds are already ON the party; locking
	 * DPS out of them protects nothing.
	 */
	private void relockBoss(Player owner)
	{
		if (owner == null)
		{
			return;
		}
		final int ownerId = owner.getObjectId();
		_released.remove(ownerId);
		_releasedRaidTargets.removeIf(key ->
		{
			if (raidOwnerId(key) != ownerId)
			{
				return false;
			}
			final WorldObject obj = World.getInstance().findObject((int) (key & 0xffffffffL));
			return !(obj instanceof Monster) || !((Monster) obj).isRaidMinion(); // re-lock the boss (and anything gone); keep live adds open
		});
		_noTankSince.remove(ownerId);
	}

	private static long raidKey(int ownerId, int raidObjectId)
	{
		return (((long) ownerId) << 32) ^ (raidObjectId & 0xffffffffL);
	}

	private static int raidOwnerId(long key)
	{
		return (int) (key >> 32);
	}

	/** A member waiting on the tank's pull: stop attacking and stay near the leader (no target, so AutoUse stays quiet). */
	private void holdForPull(Member state)
	{
		final Player npc = state.npc;
		standIfSitting(npc);
		npc.setTarget(null);
		final WorldObject target = state.owner.getTarget();
		final Monster raid = ((target instanceof Monster) && ((Monster) target).isRaid()) ? (Monster) target : engagedRaid(state);
		// Only RANGED roles wait in the spread backline; a melee DD holding for the pull stays near the leader instead
		// of running 720 out to a ranged lane and back (it has nothing to do at range anyway).
		if (isRanged(state.role) && (raid != null) && positionRaidBackline(state, raid, RAID_BACKLINE_RANGE, RAID_BACKLINE_TOLERANCE))
		{
			return;
		}
		if (npc.calculateDistance2D(state.owner) > FOLLOW_RANGE)
		{
			if (state.following)
			{
				driveFollow(state, state.owner);
			}
		}
		else if (npc.isInCombat() || npc.isAttackingNow())
		{
			npc.abortAttack();
			npc.getAI().setIntention(Intention.IDLE);
		}
	}

	/**
	 * Holds a nuker within casting range of the assist target so AutoUse can nuke it, instead of letting a physical
	 * ATTACK intention drag it into melee. Walks IN (spread) when out of range; once inside range it stands and
	 * casts and does <b>not</b> back off as the mob closes - a caster that retreats every time its own kill target
	 * steps toward it kites indefinitely and wanders into fresh mobs. Peeling a <i>different</i> mob that latched
	 * onto it is the bounded {@link #kiteAwayFrom}. Mirrors the free-roam mage positioning in {@link PhantomManager}.
	 */
	private void positionCaster(Member state, Monster target)
	{
		if (target.isRaid() && positionRaidBackline(state, target, CASTER_CAST_RANGE, CASTER_RANGE_TOLERANCE))
		{
			return;
		}
		final Player npc = state.npc;
		if (npc.isCastingNow() || npc.isMovementDisabled())
		{
			return; // don't interrupt a cast or fight a stun
		}
		double dx = npc.getX() - target.getX();
		double dy = npc.getY() - target.getY();
		double distance = Math.hypot(dx, dy);
		if (distance <= (CASTER_CAST_RANGE + CASTER_RANGE_TOLERANCE))
		{
			return; // within casting range - stand and cast; never back off just because it closed in
		}
		if (distance < 1)
		{
			distance = 1;
		}
		// Too far to cast: close the gap. AoE spread offsets each caster sideways by a fixed per-member amount so
		// several don't stack on one tile and all eat the same boss AoE. The offset is perpendicular to the target
		// line and stable per member (objId), so it fans them out around the boss without jittering.
		final double perp = Math.atan2(dy, dx) + (Math.PI / 2);
		final int lateral = (((npc.getObjectId() % 5) - 2) * CASTER_SPREAD_STEP); // -2..+2 lanes
		final int standX = target.getX() + (int) ((dx / distance) * CASTER_CAST_RANGE) + (int) (Math.cos(perp) * lateral);
		final int standY = target.getY() + (int) ((dy / distance) * CASTER_CAST_RANGE) + (int) (Math.sin(perp) * lateral);
		final Location destination = GeoEngine.getInstance().getValidLocation(npc, new Location(standX, standY, npc.getZ()));
		npc.setRunning();
		npc.getAI().setIntention(Intention.MOVE_TO, destination);
	}

	/**
	 * Holds ranged DPS/support in a spread raid backline instead of FOLLOWing the leader/tank into melee. The lane is
	 * based on the owner-side of the raid so the party stands in a familiar player-side arc, with stable per-member
	 * lateral offsets to reduce shared AoE hits and cast interruption from body-stacking.
	 * @return {@code true} if a move was issued and the caller should skip attacking/casting this tick
	 */
	private boolean positionRaidBackline(Member state, Monster raid, int desiredRange, int tolerance)
	{
		return positionRaidBackline(state, raid, desiredRange, tolerance, RAID_SPREAD_STEP);
	}

	private boolean positionRaidBackline(Member state, Monster raid, int desiredRange, int tolerance, int spreadStep)
	{
		final Player npc = state.npc;
		if (npc.isCastingNow() || npc.isMovementDisabled() || npc.isSitting())
		{
			return false;
		}
		// Already at a workable RADIAL distance with line of sight: fight from here. The old settled check
		// measured distance to the exact lane point - whose reference is the LEADER, so while the leader kited
		// around, the point never held still and an archer could spend the whole fight "repositioning" without
		// firing one shot (third Ruell attempt: the archer was idle at full HP/MP for the entire fight). The lane
		// is a nice-to-have; being in range and shooting is the job.
		if ((npc.calculateDistance2D(raid) <= (desiredRange + tolerance)) && GeoEngine.getInstance().canSeeTarget(npc, raid))
		{
			state.positionTicks = 0;
			return false;
		}
		// Stall cap: if we've been chasing a stand point for several ticks (geo-clipped lane, moving reference),
		// give up and fight from wherever we are rather than repositioning forever.
		if (state.positionTicks >= 4)
		{
			state.positionTicks = 0;
			return false;
		}
		Creature reference = state.owner;
		if ((reference == null) || (raid.calculateDistance2D(reference) < 120))
		{
			reference = npc;
		}
		double dx = reference.getX() - raid.getX();
		double dy = reference.getY() - raid.getY();
		double distance = Math.hypot(dx, dy);
		if (distance < 1)
		{
			dx = npc.getX() - raid.getX();
			dy = npc.getY() - raid.getY();
			distance = Math.max(1, Math.hypot(dx, dy));
		}
		final double nx = dx / distance;
		final double ny = dy / distance;
		final double perpX = -ny;
		final double perpY = nx;
		final int lane = ((npc.getObjectId() % 7) - 3) * spreadStep; // -3..+3 stable lanes
		// LOS-validated: a far backline lane in a cramped raid room clips behind a wall/pillar, and the core silently
		// rejects an offensive cast with no line of sight - so a caster/archer parked there never fires and reads as
		// idle. Pull the stand point inward until it can actually see the boss instead of committing to a blind lane.
		final Location destination = losStandPoint(npc, raid, nx, ny, perpX, perpY, desiredRange, lane);
		if (npc.calculateDistance2D(destination) <= tolerance)
		{
			state.positionTicks = 0;
			return false;
		}
		state.positionTicks++;
		npc.setRunning();
		npc.getAI().setIntention(Intention.MOVE_TO, destination);
		return true;
	}

	/**
	 * A ranged stand point at {@code desiredRange} from {@code target} on the {@code (nx,ny)} bearing with a lateral
	 * {@code lane} offset, but pulled inward toward the target until it has line of sight. A cramped raid room clips a
	 * far, fanned-out backline point behind a wall or the boss's own bulk, and the core silently rejects an offensive
	 * cast with no LOS, so a member parked there never lands a spell. Steps in along the bearing first (keeping the
	 * anti-stack fan when it can), then straight in without the fan, and finally falls back to a close point on the
	 * target line - being in melee AoE and casting beats standing far back doing nothing.
	 */
	private Location losStandPoint(Player npc, Creature target, double nx, double ny, double perpX, double perpY, int desiredRange, int lane)
	{
		final GeoEngine geo = GeoEngine.getInstance();
		final int instanceId = npc.getInstanceId();
		final int tx = target.getX();
		final int ty = target.getY();
		final int tz = target.getZ();
		for (int range = desiredRange; range >= LOS_STEP_MIN; range -= LOS_STEP)
		{
			final Location candidate = geo.getValidLocation(npc, new Location(tx + (int) (nx * range) + (int) (perpX * lane), ty + (int) (ny * range) + (int) (perpY * lane), npc.getZ()));
			if (geo.canSeeTarget(candidate.getX(), candidate.getY(), candidate.getZ(), tx, ty, tz, instanceId))
			{
				return candidate;
			}
		}
		// The fanned lane never cleared (a tight room); drop the lateral offset and step straight in on the target line.
		for (int range = desiredRange; range >= LOS_STEP_MIN; range -= LOS_STEP)
		{
			final Location candidate = geo.getValidLocation(npc, new Location(tx + (int) (nx * range), ty + (int) (ny * range), npc.getZ()));
			if (geo.canSeeTarget(candidate.getX(), candidate.getY(), candidate.getZ(), tx, ty, tz, instanceId))
			{
				return candidate;
			}
		}
		// Last resort: hug the target line at the minimum range - almost always has LOS, and a close cast beats none.
		return geo.getValidLocation(npc, new Location(tx + (int) (nx * LOS_STEP_MIN), ty + (int) (ny * LOS_STEP_MIN), npc.getZ()));
	}

	/**
	 * Active threat management for a TANK. It is NOT gated behind a raid - a tank holding aggro is useful in any
	 * group fight - but it behaves differently by situation so it doesn't waste taunt cooldowns on trash:
	 * <ul>
	 * <li><b>Raid boss</b> ({@code isRaid()}): keep threat pinned. Re-taunt the boss whenever the taunt is off
	 * cooldown, because a boss makes so much hate the tank must spam taunt to stay on top.</li>
	 * <li><b>Loose mob</b>: any mob whose most-hated is a non-tank party member (it slipped onto the healer/nuker/
	 * leader) is yanked back, in any fight - the nearest such mob is targeted.</li>
	 * <li><b>Plain trash, nothing loose</b>: do nothing special - ordinary melee hate from assisting is enough.</li>
	 * </ul>
	 * The taunt chosen is Aggression (single-target, longer range) when the victim is in cast range, otherwise Aura
	 * of Hate (a self-centred AoE) when the victim is inside its affect radius - so the tank uses whichever can
	 * actually land rather than casting into the void.
	 * @return {@code true} if a taunt was cast (or is in progress) this tick, so the caller skips re-issuing ATTACK
	 */
	private boolean maintainThreat(Member state, Monster assistTarget)
	{
		final Player npc = state.npc;
		if (npc.isCastingNow())
		{
			return true; // already taunting; let it land before swinging again
		}
		resolveTaunts(state);
		if ((state.aggression == null) && (state.auraOfHate == null))
		{
			return false; // too low-level to have a taunt yet - just melee (graceful degrade)
		}
		// Floor between taunts. Without it, a tank that has lost aggro (e.g. just after a battle-res) finds every mob
		// "loose" every tick and taunt-spams forever - never auto-attacking, so it does no melee damage, builds no
		// melee hate and stands idle between casts (the "tank didn't resume hitting the boss after res" bug). Throttling
		// taunts makes it spend the in-between ticks auto-attacking, which both does damage and sustains the hate that
		// keeps the mobs on it. The longer most-hated refresh below still applies on top.
		if ((System.currentTimeMillis() - state.lastTauntAt) < TAUNT_MIN_INTERVAL)
		{
			return false;
		}
		// Pick the mob to pin: a mob loose on a squishy first (nearest one), else the raid boss we're tanking.
		final List<Monster> loose = findLooseMobs(state);
		// Several mobs loose at once: the tank calls it (throttled) so the party hears the fight going sideways.
		if (loose.size() >= 3)
		{
			final PartyMood mood = mood(state.owner);
			final long barkNow = System.currentTimeMillis();
			if ((mood != null) && ((barkNow - mood.overwhelmedBarkAt) >= OVERWHELMED_BARK_COOLDOWN))
			{
				mood.overwhelmedBarkAt = barkNow;
				bark(state, "You're the tank and " + loose.size() + " mobs just slipped off you onto your party. Call it out in one very short line.", "can't hold them all, watch aggro");
			}
		}
		final Monster victim;
		if (!loose.isEmpty())
		{
			victim = nearest(npc, loose); // a mob slipped onto a squishy - always grab it back now
		}
		else if (assistTarget.isRaid())
		{
			// Already top of the boss's aggro? DON'T re-taunt every tick - the tank's own auto-attacks keep it on
			// top, so spamming Aggression/Aura of Hate every second just drained the tank's whole MP bar (then it
			// couldn't taunt when a DPS spike finally pulled aggro, and the party wiped). Re-taunt only if aggro is
			// NOT on us, or a slow refresh interval has elapsed as insurance.
			if ((assistTarget.getMostHated() == npc) && ((System.currentTimeMillis() - state.lastTauntAt) < TAUNT_REFRESH_MS))
			{
				return false; // threat is held - keep swinging and save the MP
			}
			victim = assistTarget;
		}
		else
		{
			return false; // plain trash and nothing slipped - ordinary melee hate is enough, save the taunt
		}
		// Prefer Aggression (single-target, precise, 400-800 range) when the victim is in range; otherwise fall back
		// to Aura of Hate (a self-centred AoE) when the victim is inside its affect radius (tank stands in the pack).
		if (castable(npc, state.aggression) && (npc.calculateDistance2D(victim) <= state.aggression.getCastRange()))
		{
			if (DEBUG && victim.isRaid())
			{
				dbg("TAUNT " + npc.getName() + " casts Aggression on '" + victim.getName() + "' (was hating " + describe(victim.getMostHated()) + ")");
			}
			state.lastTauntAt = System.currentTimeMillis();
			npc.setTarget(victim);
			npc.setRunning();
			npc.doCast(state.aggression);
			return true;
		}
		if (castable(npc, state.auraOfHate) && (npc.calculateDistance2D(victim) <= state.auraOfHate.getAffectRange()))
		{
			if (DEBUG && victim.isRaid())
			{
				dbg("TAUNT " + npc.getName() + " casts Aura of Hate near '" + victim.getName() + "' (was hating " + describe(victim.getMostHated()) + ")");
			}
			state.lastTauntAt = System.currentTimeMillis();
			npc.setTarget(victim); // AURA taunt centres its effect on the caster; a valid hostile target satisfies the cast
			npc.doCast(state.auraOfHate);
			return true;
		}
		return false; // taunt on cooldown / out of MP / victim out of reach - keep swinging, retry next tick
	}

	/** {@code true} if the member knows the skill and can cast it right now (not on cooldown, enough MP). */
	private static boolean castable(Player npc, Skill skill)
	{
		return (skill != null) && !npc.isSkillDisabled(skill) && (npc.getCurrentMp() >= skill.getMpConsume());
	}

	/** Lazily resolves the tank's two taunt skills from its known list (it may know one, both, or neither). */
	private void resolveTaunts(Member state)
	{
		if (state.tauntLookedUp)
		{
			return;
		}
		state.tauntLookedUp = true;
		state.aggression = state.npc.getKnownSkill(AGGRESSION_ID);
		state.auraOfHate = state.npc.getKnownSkill(AURA_OF_HATE_ID);
	}

	// ===== Class competence (Bucket 2): songs/dances, panic buttons, discipline, positioning, CC =====

	/**
	 * SWS/BD song/dance upkeep. First honours any explicit by-name request ({@link Member#pendingSong}); otherwise
	 * resolves the member's rotation once (the role's priority list filtered to what it actually knows, capped at its
	 * music-pool share by {@link #songRotationCap}); then each tick the first song that has fully lapsed on the member
	 * itself is recast on the party (it waits for the cycle to die rather than refreshing early, to save mana). Below
	 * 2nd class (no songs known yet) this costs nothing after the first lookup.
	 * @return {@code true} if a song is mid-cast or was just fired - the caller skips fighting this tick
	 */
	private boolean maintainSongs(Member state)
	{
		if ((state.role != PartyRole.SINGER) && (state.role != PartyRole.DANCER))
		{
			return false;
		}
		final Player npc = state.npc;
		if (npc.isCastingNow())
		{
			return true; // a song is mid-cast - don't clobber it with an attack intention
		}
		// On-demand SPECIFIC song/dance ("dance of fire pls"): cast it now, in or out of combat, even if it's outside
		// the auto rotation. Handled before the fight-time gate so a named request lands while resting too.
		if (state.pendingSong != null)
		{
			final Skill song = state.pendingSong;
			if (castable(npc, song))
			{
				if (!readyToCast(npc))
				{
					return true; // stand up first; cast next tick (pendingSong kept so the order isn't lost)
				}
				state.pendingSong = null;
				npc.setTarget(npc);
				npc.doCast(song);
				return true;
			}
			state.pendingSong = null; // can't cast it right now (out of MP / disabled / dancer without duals) - drop it
		}
		if (!state.songsLookedUp)
		{
			state.songsLookedUp = true;
			state.songs = resolveSongs(state);
		}
		if ((state.songs == null) || state.songs.isEmpty())
		{
			return false;
		}
		// Songs/dances are FIGHT-time only: 2-minute effects kept up outside combat just drain the singer's MP
		// for nothing (the Ruell run's SWS walked to the boss singing and arrived at 0% MP, then never sang in
		// the fight). Out of combat the rotation runs only on an explicit "sing"/"dance" order from the player.
		if (!state.songsRequested && !npc.isInCombat() && !((state.owner != null) && state.owner.isInCombat()) && !raidEngaged(state))
		{
			return false;
		}
		for (Skill song : state.songs)
		{
			final BuffInfo info = npc.getEffectList().getBuffInfoBySkillId(song.getId());
			// Wait for the song/dance to fully lapse before rebuilding it (info == null), rather than topping it up
			// a few seconds early like the long buffs do - letting the cycle die first is what keeps a music class's
			// mana drain low over a long fight.
			if ((info == null) && castable(npc, song))
			{
				if (!readyToCast(npc))
				{
					return true; // getting up first; cast on the next tick
				}
				npc.setTarget(npc);
				npc.doCast(song);
				return true;
			}
		}
		state.songsRequested = false; // rotation is fresh - an on-demand request is fulfilled
		return false;
	}

	/**
	 * The songs/dances this member will keep running: its role's priority list (dancers feed the casters first in
	 * a mage-heavy comp), filtered to what it actually knows, capped at its share of the music pool (see
	 * {@link #songRotationCap}). A dancer holding anything but dual swords gets no rotation at all - every dance
	 * hard-requires equipped duals ({@code <using kind="DUAL"/>}), so trying would just wedge it in a rejected-cast
	 * loop instead of fighting.
	 */
	private List<Skill> resolveSongs(Member state)
	{
		final Player npc = state.npc;
		final int[] priority;
		if (state.role == PartyRole.DANCER)
		{
			final Weapon weapon = npc.getActiveWeaponItem();
			if ((weapon == null) || (weapon.getItemType() != WeaponType.DUAL))
			{
				return null;
			}
			priority = (partyCasterCount(state) >= 2) ? DANCER_DANCES_MAGE : DANCER_DANCES;
		}
		else
		{
			priority = SINGER_SONGS;
		}
		final int cap = songRotationCap(state);
		final List<Skill> songs = new ArrayList<>();
		for (int id : priority)
		{
			final Skill known = npc.getKnownSkill(id);
			if (known != null)
			{
				songs.add(known);
				if (songs.size() >= cap)
				{
					break;
				}
			}
		}
		return songs;
	}

	/**
	 * How many songs/dances this member keeps up automatically: its share of the target's
	 * {@value #MUSIC_POOL_SLOTS}-slot music pool (MaxDanceAmount). Songs and dances share that one pool and evict
	 * oldest-first when it overflows, so when the party fields more than one music class (a SwS AND a BD) the pool
	 * is split between them - otherwise their full rotations would keep evicting each other on shared targets in an
	 * endless re-cast loop. Solo music class: the whole pool (still bounded by what the member has actually learned,
	 * which resolveSongs applies). Members are all recruited before the first fight, so this count is stable by the
	 * time the rotation is resolved.
	 */
	private int songRotationCap(Member state)
	{
		int musicClasses = 0;
		for (Member m : _members.values())
		{
			if ((m.owner == state.owner) && ((m.role == PartyRole.SINGER) || (m.role == PartyRole.DANCER)))
			{
				musicClasses++;
			}
		}
		return Math.max(1, MUSIC_POOL_SLOTS / Math.max(1, musicClasses));
	}

	/**
	 * A specific song/dance the message asks for by name, resolved against what this member actually knows (so a
	 * singer only matches songs and a dancer only dances). Matches the full skill name ("dance of fire") or its
	 * distinctive word ("fire", "fury", "vampire", "hunter"), so "sing song of wind", "dance of the vampire" and
	 * "give me fury" all resolve. Longest match wins. Returns {@code null} if the line names no song/dance it knows.
	 */
	private static Skill requestedSong(Player npc, String text)
	{
		final String message = normalizeWords(text);
		Skill best = null;
		int bestLength = 0;
		for (Skill skill : npc.getAllSkills())
		{
			if ((skill == null) || !skill.isDance()) // songs and dances both report isDance() == true
			{
				continue;
			}
			final String name = normalizeWords(skill.getName()); // e.g. " dance of fire "
			if (message.contains(name) && (name.length() > bestLength))
			{
				best = skill;
				bestLength = name.length();
				continue;
			}
			// Distinctive tail: drop a leading "song of (the) " / "dance of (the) " so "fire"/"vampire"/"hunter" match.
			final String tail = normalizeWords(skill.getName().toLowerCase().replaceFirst("^(song|dance) of (the )?", ""));
			if ((tail.trim().length() >= 4) && message.contains(tail) && (tail.length() > bestLength))
			{
				best = skill;
				bestLength = tail.length();
			}
		}
		return best;
	}

	/** Lowercases, strips punctuation and collapses whitespace, wrapped in single spaces for whole-word contains-checks. */
	private static String normalizeWords(String text)
	{
		return " " + text.toLowerCase().replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim() + " ";
	}

	/** How caster-heavy this party is: a mage leader plus its nuker members (drives the dancer's rotation choice). */
	private int partyCasterCount(Member state)
	{
		int count = ((state.owner != null) && state.owner.isMageClass()) ? 1 : 0;
		for (Member m : _members.values())
		{
			if ((m.owner == state.owner) && m.role.mage && !m.isSupport())
			{
				count++;
			}
		}
		return count;
	}

	/**
	 * Parks skills the manager casts by hand out of the AutoUse lists at recruit time. Ultimate Defense is a
	 * SELF-target continuous buff, so {@code registerAutoSkills} queued it as an auto-buff - and AutoUse re-cast
	 * it the moment it was off cooldown, burning the tank's panic button on trash at full HP. The party manager
	 * pops it itself at low HP instead (see {@link #maybeSurvival}).
	 */
	private static void parkPanicButtons(Member member)
	{
		if (member.role == PartyRole.TANK)
		{
			member.npc.getAutoUseSettings().getAutoBuffs().remove(Integer.valueOf(UD_SKILL_ID));
		}
	}

	/** Returns hand-managed skills to the AutoUse lists when a member leaves party service (a released friend resumes its ambient life). */
	private static void restoreAutoSkills(Member state)
	{
		final Player npc = state.npc;
		// Ultimate Defense is intentionally NOT restored to the auto-buff list. It is a panic button that roots the
		// caster and sits on a 30-minute reuse, so auto-maintaining it froze an ambient phantom out of combat -
		// registerAutoSkills now excludes it (and every emergency self-buff) from the ambient loadout as well, so a
		// released friend resumes ambient life without it. In party it stays hand-cast at low HP by maybeSurvival.
		if (state.parkedAutoSkills != null)
		{
			for (Integer id : state.parkedAutoSkills)
			{
				if (!npc.getAutoUseSettings().getAutoSkills().contains(id))
				{
					npc.getAutoUseSettings().getAutoSkills().add(id);
				}
			}
			state.parkedAutoSkills = null;
		}
		if (state.play.parkedIds != null)
		{
			for (Integer id : state.play.parkedIds)
			{
				if (!npc.getAutoUseSettings().getAutoSkills().contains(id))
				{
					npc.getAutoUseSettings().getAutoSkills().add(id);
				}
			}
			state.play.parkedIds = null;
		}
		if (state.play.parkedBuffIds != null)
		{
			for (Integer id : state.play.parkedBuffIds)
			{
				if (!npc.getAutoUseSettings().getAutoBuffs().contains(id))
				{
					npc.getAutoUseSettings().getAutoBuffs().add(id);
				}
			}
			state.play.parkedBuffIds = null;
		}
	}

	/**
	 * TANK survival: pop Ultimate Defense when critically low and still being hit - a real tank hits its panic
	 * button instead of flatlining with it off cooldown. The engine tracks the (15-min) reuse; {@code castable}
	 * gates re-attempts.
	 * @return {@code true} if the cast fired - the caller skips re-issuing an attack over it this tick
	 */
	private boolean maybeSurvival(Member state)
	{
		final Player npc = state.npc;
		if (!state.survivalLookedUp)
		{
			state.survivalLookedUp = true;
			state.survival = npc.getKnownSkill(UD_SKILL_ID);
		}
		if ((state.survival == null) || npc.isCastingNow() || (npc.getCurrentHpPercent() > UD_HP_BURST_PERCENT) || !castable(npc, state.survival) || !underAttack(npc))
		{
			return false;
		}
		// Between the normal (30%) and burst (50%) marks, pop early only under real burst - two or more raid mobs
		// on this tank chew through 20% HP faster than the 1s tick can see (Ruell: 44% -> 5% between two ticks).
		if (npc.getCurrentHpPercent() > UD_HP_PERCENT)
		{
			int raidsOnMe = 0;
			for (Monster raid : engagedRaids(state))
			{
				if (raid.getMostHated() == npc)
				{
					raidsOnMe++;
				}
			}
			if (raidsOnMe < 2)
			{
				return false;
			}
		}
		if (DEBUG)
		{
			dbg("SURVIVAL TANK '" + npc.getName() + "' pops Ultimate Defense at " + npc.getCurrentHpPercent() + "% HP");
		}
		npc.setTarget(npc);
		npc.doCast(state.survival);
		bark(state, "You're tanking, you just dropped to " + npc.getCurrentHpPercent() + "% HP and hit Ultimate Defense. Call it out in one very short line.", "popping ud");
		return true;
	}

	/**
	 * ARCHER skill discipline: below the park mark its offensive skills are pulled out of AutoUse (stashed on the
	 * member) so it plinks with plain soulshotted auto-shots - which cost only a small per-shot bow MP, far less
	 * than dumping every skill off cooldown - instead of emptying the bar; restored once MP recovers. Both marks
	 * are hysteresis so it doesn't flap. (True MP starvation, where even a bow shot can't be paid for, is handled
	 * separately by {@link #manageArcherMp}.)
	 */
	private static void manageArcherSkills(Member state)
	{
		final Player npc = state.npc;
		final int mp = npc.getCurrentMpPercent();
		if ((state.parkedAutoSkills == null) && (mp < ARCHER_SKILL_PARK_MP) && !npc.getAutoUseSettings().getAutoSkills().isEmpty())
		{
			state.parkedAutoSkills = new ArrayList<>(npc.getAutoUseSettings().getAutoSkills());
			npc.getAutoUseSettings().getAutoSkills().clear();
		}
		else if ((state.parkedAutoSkills != null) && (mp >= ARCHER_SKILL_RESUME_MP))
		{
			for (Integer id : state.parkedAutoSkills)
			{
				if (!npc.getAutoUseSettings().getAutoSkills().contains(id))
				{
					npc.getAutoUseSettings().getAutoSkills().add(id);
				}
			}
			state.parkedAutoSkills = null;
		}
	}

	/**
	 * ARCHER MP starvation fallback, independent of Recharge. A bow shot consumes MP and simply FAILS when the
	 * archer can't pay it ({@code Creature.doAttack}), so at that point it stops attacking and holds fire to let
	 * MP regenerate instead of spamming failed shots at a live target; it resumes once a few shots' worth is back
	 * (hysteresis). A support's Recharge, when present, refills faster and ends the rest sooner.
	 * @return {@code true} while holding fire - the caller skips acting this tick
	 */
	private static boolean manageArcherMp(Member state)
	{
		final Player npc = state.npc;
		final Weapon bow = npc.getActiveWeaponItem();
		final int shotCost = (bow == null) ? 0 : bow.getMpConsume();
		if (shotCost <= 0) // not a bow, or a bow with no MP cost - nothing to starve on
		{
			state.bowResting = false;
			return false;
		}
		final int mp = (int) npc.getCurrentMp();
		if (state.bowResting)
		{
			if (mp >= (shotCost * BOW_REST_RESUME_SHOTS)) // enough for a short burst - resume firing
			{
				state.bowResting = false;
				return false;
			}
			return true; // still recovering
		}
		if (mp < shotCost) // can't afford even one shot: stop so MP regenerates rather than failing attacks
		{
			state.bowResting = true;
			npc.getAI().setIntention(Intention.IDLE);
			return true;
		}
		return false;
	}

	/**
	 * ARCHER human beat on target switches: when the leader's assist target CHANGES, the archer waits a short
	 * randomized moment before swinging onto the new one, instead of snapping the same game tick. The current
	 * target being re-confirmed costs nothing.
	 * @return {@code true} while the beat is still running - the caller skips acting this tick
	 */
	private static boolean assistSwitchPending(Member state, Monster focus)
	{
		if (focus.getObjectId() == state.lastAssistTargetId)
		{
			return false;
		}
		final long now = System.currentTimeMillis();
		if (state.assistSwitchGate == 0)
		{
			state.assistSwitchGate = now + Rnd.get((int) ASSIST_SWITCH_MIN, (int) ASSIST_SWITCH_MAX);
			return true;
		}
		if (now < state.assistSwitchGate)
		{
			return true;
		}
		state.lastAssistTargetId = focus.getObjectId();
		state.assistSwitchGate = 0;
		return false;
	}

	/**
	 * DPS aggro-easing, raid-only: a DPS whose damage just ripped the boss off the tank stops generating hate for
	 * a few seconds so the taunt can land - instead of dragging the boss around the room by its face. Not applied
	 * to trash (the tank deliberately leaves melee DDs their own adds, so easing there would just stutter their
	 * DPS forever), and re-armed on a floor so a boss that stays glued to a member doesn't hold it hostage.
	 * @return {@code true} while holding fire - the caller skips attacking/casting this tick
	 */
	private boolean easeAggro(Member state, Monster focus)
	{
		final long now = System.currentTimeMillis();
		if (now < state.easeUntil)
		{
			return true; // still easing off
		}
		// Only the main BOSS is eased back to the tank. A minion that turned on this member is fought, not
		// dodged - standing still sheds no hate (Ruell: an archer eased off a Wind add and idled while it kept
		// swinging at it); the tank's loose-mob taunt still rescues squishies.
		if (!focus.isRaid() || focus.isRaidMinion() || focus.isDead() || (focus.getMostHated() != state.npc))
		{
			return false;
		}
		final Member tank = findTankState(state);
		if ((tank == null) || tank.npc.isDead())
		{
			return false; // no tank to hand it back to - keep fighting
		}
		if ((now - state.lastEaseAt) < AGGRO_EASE_REARM_MS)
		{
			return false; // just eased and the tank still hasn't taken it back - fighting beats standing there
		}
		state.easeUntil = now + AGGRO_EASE_MS;
		state.lastEaseAt = now;
		if (DEBUG)
		{
			dbg("EASE " + state.role + " '" + state.npc.getName() + "' ripped '" + focus.getName() + "' off the tank - holding fire");
		}
		final Player npc = state.npc;
		npc.abortAttack();
		npc.setTarget(null); // a mage's AutoUse stops nuking when it has no target
		npc.getAI().setIntention(Intention.IDLE);
		bark(state, "You just pulled the raid boss's aggro off your tank, so you're easing off for a few seconds to hand it back. Say it in one very short line.", "easing off, take it back");
		return true;
	}

	/**
	 * DAGGER rear positioning: slide to the target's back arc while it's busy with someone else (backstab-style
	 * crits land from behind; face-tanking is how daggers die), face-fight only when the mob turns on the dagger
	 * itself. Gives up after a few blocked attempts (mob backed into a wall) rather than circling forever.
	 * @return {@code true} while repositioning - the caller skips re-issuing the attack this tick
	 */
	private boolean positionRear(Member state, Monster target)
	{
		final Player npc = state.npc;
		if ((target.getMostHated() == npc) || npc.isCastingNow() || npc.isMovementDisabled())
		{
			return false; // it's facing us anyway (or we can't move) - just fight
		}
		if (target.getObjectId() != state.rearTargetId)
		{
			state.rearTargetId = target.getObjectId();
			state.rearTries = 0;
		}
		if (state.rearTries >= 3)
		{
			return false; // rear spot unreachable (wall?) - fight from the front rather than orbit forever
		}
		final long now = System.currentTimeMillis();
		if ((now - state.rearMoveAt) < REAR_MOVE_GRACE)
		{
			return true; // still sliding around it - let the move finish
		}
		// The mob faces its current victim; its back is directly opposite its heading.
		final double facing = (target.getHeading() * 2 * Math.PI) / 65536.0;
		final Location rear = new Location(target.getX() - (int) (Math.cos(facing) * REAR_ATTACK_DISTANCE), target.getY() - (int) (Math.sin(facing) * REAR_ATTACK_DISTANCE), target.getZ());
		if (npc.calculateDistance2D(rear) <= REAR_TOLERANCE)
		{
			state.rearTries = 0;
			return false; // already behind it - stab away
		}
		state.rearMoveAt = now;
		state.rearTries++;
		npc.setRunning();
		npc.getAI().setIntention(Intention.MOVE_TO, GeoEngine.getInstance().getValidLocation(npc, rear));
		return true;
	}

	/**
	 * NUKER crowd control: a loose add beating on a squishy party member (healer/caster/the leader) gets slept or
	 * rooted before the nuker resumes its normal DPS - CC is the nuker's party job. Never the kill target, never
	 * a raid (immune), never an add already controlled, and throttled so it doesn't chain-CC instead of nuking.
	 * The cast retargets the nuker briefly; the next tick's assist pass re-targets the focus.
	 * @return {@code true} if a CC cast fired - the caller skips positioning/nuking this tick
	 */
	private boolean maybeCrowdControl(Member state, Monster focus)
	{
		final Player npc = state.npc;
		final long now = System.currentTimeMillis();
		if ((now - state.lastCcAt) < CC_COOLDOWN_MS)
		{
			return false;
		}
		if (!state.ccLookedUp)
		{
			state.ccLookedUp = true;
			state.cc = npc.getKnownSkill(SLEEP_ID);
			if (state.cc == null)
			{
				state.cc = npc.getKnownSkill(DRYAD_ROOT_ID);
			}
		}
		if ((state.cc == null) || !castable(npc, state.cc))
		{
			return false;
		}
		for (Monster add : World.getInstance().getVisibleObjectsInRange(npc, Monster.class, CC_SCAN_RANGE))
		{
			if ((add == focus) || add.isDead() || add.isRaid() || add.isSleeping() || add.isRooted())
			{
				continue; // never the kill target or a raid; skip already-controlled adds
			}
			final Creature hated = add.getMostHated();
			if ((hated == null) || !needsRescueFrom(state, hated))
			{
				continue; // not loose on a squishy - the tank or its own DD handles it
			}
			if (npc.calculateDistance2D(add) > state.cc.getCastRange())
			{
				continue;
			}
			state.lastCcAt = now;
			if (DEBUG)
			{
				dbg("CC NUKER '" + npc.getName() + "' casts " + state.cc.getName() + " on loose add '" + add.getName() + "' (was on " + describe(hated) + ")");
			}
			standIfSitting(npc);
			npc.setTarget(add);
			npc.doCast(state.cc);
			return true;
		}
		return false;
	}

	// ===== Ranged self-preservation: peel, kite, escalate =====

	/**
	 * The live monster currently chewing on this member, or {@code null}. Only counts a mob that has actually
	 * settled its hate on us - a mob merely standing nearby, or one the tank is holding, is not our problem.
	 */
	private Monster attackerOnMe(Member state, Monster focus)
	{
		return attackerOnMe(state, focus, true);
	}

	/** The party's current kill target - the leader's live monster target - so a support won't CC the mob being killed. */
	private Monster leaderFocus(Member state)
	{
		final Player owner = state.owner;
		if (owner == null)
		{
			return null;
		}
		final WorldObject t = owner.getTarget();
		return ((t instanceof Monster) && !((Monster) t).isDead()) ? (Monster) t : null;
	}

	/**
	 * As {@link #attackerOnMe(Member, Monster)}, but {@code deferToTank} can be turned off. Deferring is right
	 * while the party is killing something (the tank taunts the add, everyone else keeps focus-firing), and wrong
	 * when there is no kill target at all - then everybody piling onto the loose mob is exactly what should happen.
	 */
	private Monster attackerOnMe(Member state, Monster focus, boolean deferToTank)
	{
		final Player npc = state.npc;
		final Member tank = deferToTank ? findTankState(state) : null;
		for (Monster mob : World.getInstance().getVisibleObjectsInRange(npc, Monster.class, DANGER_RANGE))
		{
			if (mob.isDead() || (mob.getMostHated() != npc))
			{
				continue;
			}
			// A RAID on us is not a peel problem: it can't be slept/rooted, it outruns any kite, and turning on it
			// is the opposite of what we want. easeAggro already handles it - hold fire so the tank can taunt back.
			if (mob.isRaid())
			{
				continue;
			}
			// The mob we are already killing is not a problem to solve - we are handling it by shooting it. This
			// also stops a nuker sleeping its own kill target, which would throw away the party's damage.
			if (mob == focus)
			{
				continue;
			}
			// A live tank close enough to taunt it will pull it off us (maintainThreat scans THREAT_SCAN_RANGE for
			// exactly this) - give it the moment rather than both of us reacting.
			if ((tank != null) && (tank.npc.calculateDistance2D(mob) <= THREAT_SCAN_RANGE))
			{
				continue;
			}
			return mob;
		}
		return null;
	}

	/** The best control skill this member can peel with, resolved once (archers stun/slow, casters sleep/root). */
	private Skill peelControl(Member state)
	{
		if (!state.peelControlLookedUp)
		{
			state.peelControlLookedUp = true;
			final Player npc = state.npc;
			final int[] candidates = (state.role == PartyRole.ARCHER) //
				? new int[]
				{
					STUNNING_SHOT_ID,
					HAMSTRING_SHOT_ID
				} //
				: new int[]
				{
					SLEEP_ID,
					DRYAD_ROOT_ID
				};
			for (int id : candidates)
			{
				final Skill known = npc.getKnownSkill(id);
				if (known != null)
				{
					state.peelControl = known;
					break;
				}
			}
		}
		return state.peelControl;
	}

	/**
	 * {@code true} if this member is one of the squishy ranged roles that should look after itself. Melee are built
	 * to eat hits, and peeling them off a kill target is bad play, so they are deliberately excluded.
	 */
	private static boolean peels(Member state)
	{
		return (state.role == PartyRole.ARCHER) || (state.role == PartyRole.NUKER);
	}

	/**
	 * {@code true} when kiting is no longer the right answer and the member should just kill what is on it: it is
	 * losing the exchange (own HP under {@link #PEEL_SELF_HP_PERCENT}), or the attacker is nearly dead anyway
	 * ({@link #PEEL_EXECUTE_PERCENT}) so finishing it is quicker than fleeing it. Survival outranks focus fire.
	 */
	private boolean shouldEscalatePeel(Member state, Monster attacker)
	{
		final boolean escalate = (state.npc.getCurrentHpPercent() < PEEL_SELF_HP_PERCENT) || (attacker.getCurrentHpPercent() <= PEEL_EXECUTE_PERCENT);
		if (escalate && DEBUG)
		{
			dbg("PEEL " + roleLabel(state.npc) + " '" + state.npc.getName() + "' turns on '" + attacker.getName() + "' (self " + state.npc.getCurrentHpPercent() + "% HP, it " + attacker.getCurrentHpPercent() + "%)");
		}
		return escalate;
	}

	/**
	 * Breaks contact with the mob on us WITHOUT giving up the party's kill target - a real ranged player controls
	 * and steps away rather than dropping focus fire every time something touches them. Control first (stun/slow
	 * for an archer, sleep/root for a caster, on a cooldown so it is a disengage and not a stun-lock), otherwise a
	 * step back out of its reach.
	 * @return {@code true} if it acted this tick - the caller must NOT then engage the assist target, because
	 *         re-issuing an ATTACK intention would cancel the disengage move it just ordered
	 */
	private boolean peelDefend(Member state, Monster attacker)
	{
		final Player npc = state.npc;
		final long now = System.currentTimeMillis();
		final Skill control = peelControl(state);
		if ((control != null) && ((now - state.lastPeelControlAt) >= PEEL_CONTROL_COOLDOWN_MS) && castable(npc, control) //
			&& !attacker.isSleeping() && !attacker.isRooted() && (npc.calculateDistance2D(attacker) <= control.getCastRange()))
		{
			if (!readyToCast(npc))
			{
				return true; // standing up first; retry next tick
			}
			state.lastPeelControlAt = now;
			if (DEBUG)
			{
				dbg("PEEL " + roleLabel(npc) + " '" + npc.getName() + "' casts " + control.getName() + " on '" + attacker.getName() + "' to break contact");
			}
			npc.setTarget(attacker);
			npc.doCast(control);
			return true;
		}
		return kiteAwayFrom(state, attacker);
	}

	/**
	 * Steps a ranged member directly away from {@code threat}, throttled so it doesn't re-path every tick.
	 * @return {@code true} if a disengage move was issued (or one is still running) - the caller must skip
	 *         engaging, since an ATTACK intention would cancel it
	 */
	private boolean kiteAwayFrom(Member state, Monster threat)
	{
		final Player npc = state.npc;
		final long now = System.currentTimeMillis();
		if (npc.isCastingNow() || npc.isMovementDisabled())
		{
			return false; // let the cast finish / can't move anyway - keep fighting normally
		}
		if ((now - state.peelMoveAt) < PEEL_MOVE_GRACE)
		{
			return true; // a step is already running - don't stomp it with an attack order
		}
		double dx = npc.getX() - threat.getX();
		double dy = npc.getY() - threat.getY();
		double distance = Math.hypot(dx, dy);
		final int desired = (state.role == PartyRole.ARCHER) ? archerHoldRange(state) : CASTER_CAST_RANGE;
		if (distance >= desired)
		{
			return false; // already out of its face - nothing to do but keep firing at the party's target
		}
		if (distance < 1)
		{
			// Standing on top of us: pick any direction rather than divide by zero.
			dx = 1;
			dy = 0;
			distance = 1;
		}
		// One SHORT step directly away, capped so it never snaps onto a distant ring: open the gap by at most
		// KITE_STEP_MAX per tick and re-evaluate next tick. A player backs off a step at a time, it does not
		// teleport to a fixed radius. Never overshoot past the range it actually wants (desired + a small margin).
		final int step = (int) Math.min(KITE_STEP_MAX, (desired - distance) + PEEL_STEP_BACK);
		final int standX = npc.getX() + (int) ((dx / distance) * step);
		final int standY = npc.getY() + (int) ((dy / distance) * step);
		// Never kite out of the party's cleared bubble - backing into unexplored ground is exactly how a kite pulls
		// fresh mobs. If this step would take it too far from the leader, don't move: hold here and keep working the
		// assist target (and once its own HP drops, shouldEscalatePeel turns it on the attacker instead of fleeing).
		final Player owner = state.owner;
		if ((owner != null) && (Math.hypot((double) standX - owner.getX(), (double) standY - owner.getY()) > KITE_MAX_FROM_PARTY))
		{
			return false;
		}
		state.peelMoveAt = now;
		if (DEBUG)
		{
			dbg("PEEL " + roleLabel(npc) + " '" + npc.getName() + "' steps back from '" + threat.getName() + "' (was " + (int) distance + " away, capped step " + step + ")");
		}
		npc.setRunning();
		npc.getAI().setIntention(Intention.MOVE_TO, GeoEngine.getInstance().getValidLocation(npc, new Location(standX, standY, npc.getZ())));
		return true;
	}

	/**
	 * Self-preservation for a SUPPORT (healer/buffer). The support's answer to being attacked is the opposite of
	 * the ranged DD's: it never fights back (its damage is irrelevant and hitting things makes more hate), and it
	 * retreats <b>towards</b> its protector rather than away - running off alone would drag it out of heal range,
	 * whereas dragging the mob to the tank/melee is exactly how a human support survives without a tank holding it.
	 * <ol>
	 * <li><b>Disable</b> it - sleep/root/trance, whatever this class actually knows.</li>
	 * <li><b>Fall back</b> to the tank (or the nearest melee, or the leader), so someone who can take the hit does.</li>
	 * </ol>
	 * @return {@code true} if it acted this tick - the caller returns, since re-issuing routine upkeep would cancel
	 *         the retreat move
	 */
	private boolean supportDefend(Member state)
	{
		final Player npc = state.npc;
		// Resolve the party's current kill target (the leader's monster target) and pass it in, so the "don't control
		// the mob we're killing" exclusion in attackerOnMe actually applies - otherwise a healer that pulled hate off
		// the focus could Sleep/Root it and throw away the party's focus damage (or wake a sleeper it just controlled).
		final Monster attacker = attackerOnMe(state, leaderFocus(state));
		if (attacker == null)
		{
			return false;
		}
		// (1) Shut it down if this class knows how. Never the party's kill target (that would throw away the
		// party's damage) - attackerOnMe already excludes a focus, and a slept mob is skipped below.
		final long now = System.currentTimeMillis();
		final Skill control = supportControl(state);
		if ((control != null) && ((now - state.lastPeelControlAt) >= PEEL_CONTROL_COOLDOWN_MS) && castable(npc, control) //
			&& !attacker.isSleeping() && !attacker.isRooted() && (npc.calculateDistance2D(attacker) <= control.getCastRange()) && PhantomBuffs.canAffordReagent(npc, control))
		{
			if (!readyToCast(npc))
			{
				return true; // getting up first
			}
			state.lastPeelControlAt = now;
			if (DEBUG)
			{
				dbg("PEEL " + roleLabel(npc) + " '" + npc.getName() + "' casts " + control.getName() + " on '" + attacker.getName() + "' that is attacking it");
			}
			npc.setTarget(attacker);
			npc.doCast(control);
			return true;
		}
		// (2) Retreat to whoever can take it off us. Not away into open ground - a support that runs off is a
		// support out of range, and the mob follows it anyway.
		return fallBackToProtector(state, attacker);
	}

	/** The disable a support peels with, resolved once: Sleep, Dryad Root, Trance or the Orc Dreaming Spirit. */
	private Skill supportControl(Member state)
	{
		if (!state.peelControlLookedUp)
		{
			state.peelControlLookedUp = true;
			for (int id : SUPPORT_CONTROL_PRIORITY)
			{
				final Skill known = state.npc.getKnownSkill(id);
				if (known != null)
				{
					state.peelControl = known;
					break;
				}
			}
		}
		return state.peelControl;
	}

	/**
	 * Walks a support back to the party member best able to take the mob off it - the tank first, then any melee,
	 * then the leader. Throttled like the ranged kite so it doesn't re-path every tick.
	 * @return {@code true} if a retreat move was issued or is still running
	 */
	private boolean fallBackToProtector(Member state, Monster threat)
	{
		final Player npc = state.npc;
		final long now = System.currentTimeMillis();
		if (npc.isCastingNow() || npc.isMovementDisabled())
		{
			return false;
		}
		if ((now - state.peelMoveAt) < PEEL_MOVE_GRACE)
		{
			return true; // already moving - don't stomp it
		}
		Player protector = null;
		int bestDistance = Integer.MAX_VALUE;
		for (Member m : _members.values())
		{
			if ((m.owner != state.owner) || (m.npc == npc) || m.npc.isDead() || m.isSupport())
			{
				continue;
			}
			final int distance = (int) npc.calculateDistance2D(m.npc);
			// A tank is always the right answer; otherwise take the closest body that fights in melee.
			if (m.role == PartyRole.TANK)
			{
				protector = m.npc;
				break;
			}
			if (((m.role == PartyRole.WARRIOR) || (m.role == PartyRole.DAGGER) || (m.role == PartyRole.MONK) || (m.role == PartyRole.BOUNTY_HUNTER)) && (distance < bestDistance))
			{
				bestDistance = distance;
				protector = m.npc;
			}
		}
		if ((protector == null) && (state.owner != null) && !state.owner.isDead())
		{
			protector = state.owner; // no melee in the party - the human player is the only body available
		}
		if ((protector == null) || (npc.calculateDistance2D(protector) <= PROTECTOR_REACHED_RANGE))
		{
			return false; // nobody to hide behind, or already there - carry on supporting and hope the heal outpaces it
		}
		state.peelMoveAt = now;
		if (DEBUG)
		{
			dbg("PEEL " + roleLabel(npc) + " '" + npc.getName() + "' falls back to " + describe(protector) + " with '" + threat.getName() + "' on it");
		}
		npc.setRunning();
		npc.getAI().setIntention(Intention.MOVE_TO, GeoEngine.getInstance().getValidLocation(npc, new Location(protector.getX(), protector.getY(), protector.getZ())));
		return true;
	}

	// ===== Class playstyle (data-driven per-class combat, PhantomPlaystyles.xml) =====

	/**
	 * MP floor below which a member stops spending on playstyle skills (PANIC/LIMIT are exempt - a panic
	 * button is exactly what a nearly-dry member should still be able to press).
	 * <p>
	 * What a member does BELOW its floor differs by role, and the floors are picked to match: a melee or
	 * archer keeps auto-attacking (much cheaper than skills, so it still contributes and its MP regenerates back
	 * over the floor on its own - a bow's small per-shot MP is handled at the true-starvation end by
	 * {@link #manageArcherMp}), while a caster stops entirely and sits to recharge. So a caster's floor is
	 * pinned to {@link #CASTER_MIN_MP} - the same mark at which {@code engageFocus} drops its target and
	 * hands it to {@code restForMp} - otherwise it would spend the gap between the two standing at cast
	 * range doing nothing at all.
	 */
	private static int mpReserve(PartyRole role)
	{
		if (role == PartyRole.ARCHER)
		{
			return ARCHER_SKILL_PARK_MP; // 30%: back to plain soulshotted bow fire (cheap per-shot MP; manageArcherMp guards true starvation)
		}
		return role.mage ? CASTER_MIN_MP : 10; // a caster casts right down to the mark where it sits to rest
	}

	/**
	 * Whether SOMEONE in this member's party could actually rescue it from a self-endangering LIMIT - the gate for
	 * those skills. Scans the whole roster, not just recruited HEALER members: the human leader counts (on an offline
	 * server they are often a healer class standing in range), and so does any secondary healer (a support with a
	 * usable heal). "Ready" still means the candidate is alive, within {@link #SUPPORT_RANGE}, has meaningful MP, can
	 * act, and holds a usable targeted heal - the same range / MP / disabled-state bar as before, now roster-wide.
	 */
	private boolean healerReady(Member state)
	{
		// The human leader first - the cheapest check and the one the old role-only scan missed entirely.
		if (isReadyHealer(state.npc, state.owner))
		{
			return true;
		}
		for (Member m : _members.values())
		{
			if ((m.owner != state.owner) || !m.partied || (m.npc == state.npc))
			{
				continue; // other party's member, not yet partied, or this member itself (it can't heal its own limit)
			}
			if (isReadyHealer(state.npc, m.npc))
			{
				return true;
			}
		}
		return false;
	}

	/** A candidate that could answer a LIMIT endangering {@code endangered}: alive, close, MP left, able to act, and holding a usable party heal. */
	private boolean isReadyHealer(Player endangered, Player candidate)
	{
		if ((candidate == null) || candidate.isDead() || (candidate.getCurrentMpPercent() <= 25))
		{
			return false;
		}
		if (endangered.calculateDistance2D(candidate) > SUPPORT_RANGE)
		{
			return false;
		}
		// Can't act (stunned/rooted-out/paralyzed/all skills disabled) - can't answer a limit in time.
		if (candidate.isAllSkillsDisabled() || candidate.isStunned() || candidate.isSleeping() || candidate.isParalyzed())
		{
			return false;
		}
		return hasUsablePartyHeal(candidate);
	}

	/** True if {@code p} knows a learned heal it could throw on a partner right now - targeted (not self-only), affordable, and off cooldown. */
	private boolean hasUsablePartyHeal(Player p)
	{
		for (Skill skill : p.getAllSkills())
		{
			if ((skill == null) || !skill.hasEffectType(EffectType.HEAL, EffectType.CPHEAL))
			{
				continue;
			}
			final TargetType tt = skill.getTargetType();
			if ((tt == TargetType.SELF) || (tt == TargetType.NONE))
			{
				continue; // a self-only heal can't rescue the endangered member
			}
			if (p.isSkillDisabled(skill) || (p.getCurrentMp() < skill.getMpConsume()))
			{
				continue;
			}
			return true;
		}
		return false;
	}

	/**
	 * One playstyle decision for this member against {@code focus}: asks the engine for the first listed skill
	 * whose moment has come and hand-casts it with the usual guards. A member whose class has no playstyle (or
	 * whose moment hasn't come) returns {@code false} and fights on with plain attacks.
	 * @return {@code true} if a cast fired - the caller skips the attack re-issue this tick
	 */
	private boolean tryPlaystyle(Member state, Monster focus)
	{
		final Player npc = state.npc;
		// A sitting member is resting on purpose - never stand it up just to skill (the engage paths already
		// stood up members that are actually fighting), and a mid-cast member finishes what it started.
		if (npc.isCastingNow() || npc.isSitting())
		{
			return false;
		}
		// A //phantom playstyle reload may have added or removed this member's playstyle since it was recruited;
		// reconcile AutoUse ownership before deciding, so legacy auto-skills don't compete (nor leave it bare).
		PhantomPlaystyleEngine.syncParkingIfReloaded(npc, state.play, state.role.name());
		final PhantomPlaystyleEngine.CastAction action = PhantomPlaystyleEngine.pick(npc, focus, state.play, healerReady(state), underAttack(npc), mpReserve(state.role), state.role.name());
		if (action == null)
		{
			return false;
		}
		if (DEBUG)
		{
			dbg("PLAYSTYLE '" + npc.getName() + "' (" + npc.getPlayerClass() + ") casts " + action.skill.getName() + (action.target == npc ? " on self" : " on '" + focus.getName() + "'") + " at " + npc.getCurrentHpPercent() + "% HP / " + npc.getCurrentMpPercent() + "% MP");
		}
		npc.setTarget(action.target);
		npc.doCast(action.skill);
		// Commit a once-per-target opener to the ledger ONLY now that the cast has actually launched. A doCast the
		// core rejected (out of range, interrupted, target gone) leaves the ledger clean, so the opener retries next
		// tick instead of being silently burned for the life of this target.
		if (npc.isCastingNow() || npc.isCastingSimultaneouslyNow())
		{
			PhantomPlaystyleEngine.confirmCast(state.play, action);
		}
		return true;
	}

	/**
	 * Out-of-combat charge banking for a charge class (Gladiator / Tyrant lines): if the member is standing ready
	 * and below its authored target charge, self-cast the class's charge-builder so the next pull opens prepared.
	 * A no-op for classes with no builder, for a member that is resting or repositioning, or once it is already
	 * topped up (the builder stops firing, and Interlude charges then persist ~10 minutes across pulls).
	 * @return {@code true} if a builder cast launched this tick (caller skips the rest of the tick)
	 */
	private boolean prepCharges(Member state)
	{
		final Player npc = state.npc;
		if (npc.isSitting() || npc.isMoving())
		{
			return false; // resting to recover, or running to catch up - don't interrupt just to charge
		}
		final PhantomPlaystyleEngine.CastAction action = PhantomPlaystyleEngine.pickPrep(npc, state.play, state.role.name());
		if (action == null)
		{
			return false;
		}
		npc.setTarget(action.target);
		npc.doCast(action.skill);
		return npc.isCastingNow() || npc.isCastingSimultaneouslyNow();
	}

	/** Living mobs near the tank whose most-hated is a squishy party member (aggro slipped onto a healer/caster/leader). */
	private List<Monster> findLooseMobs(Member state)
	{
		final Player npc = state.npc;
		final List<Monster> loose = new ArrayList<>();
		for (Monster mob : World.getInstance().getVisibleObjectsInRange(npc, Monster.class, THREAT_SCAN_RANGE))
		{
			if (mob.isDead())
			{
				continue;
			}
			final Creature hated = mob.getMostHated();
			if ((hated != null) && (hated != npc) && needsRescueFrom(state, hated))
			{
				loose.add(mob);
			}
		}
		return loose;
	}

	/**
	 * {@code true} if a mob hating {@code who} should be taunted off it. The leader and the squishy bots (healers,
	 * buffers, nukers) need rescuing; a melee bruiser (WARRIOR/DAGGER) holds its own add - taunting it away would just
	 * steal the add the DD is killing and make the tank thrash between targets instead of holding the boss.
	 */
	private boolean needsRescueFrom(Member state, Creature who)
	{
		if (!isPartyMember(state, who))
		{
			return false;
		}
		final Member m = _members.get(((Player) who).getObjectId());
		if (m == null)
		{
			return true; // the real human leader - always protect the player
		}
		return (m.role != PartyRole.WARRIOR) && (m.role != PartyRole.DAGGER) && (m.role != PartyRole.MONK) && (m.role != PartyRole.TANK);
	}

	/** {@code true} if {@code who} is a member of this tank's party (so a mob on it is a threat to protect against). */
	private boolean isPartyMember(Member state, Creature who)
	{
		if (!who.isPlayer() || (state.owner == null) || !state.owner.isInParty())
		{
			return false;
		}
		for (Player member : state.owner.getParty().getMembers())
		{
			if (member == who)
			{
				return true;
			}
		}
		return false;
	}

	/** The nearest of a list of mobs to a point of origin. */
	private static Monster nearest(Player origin, List<Monster> mobs)
	{
		Monster best = null;
		double bestDist = Double.MAX_VALUE;
		for (Monster mob : mobs)
		{
			final double dist = origin.calculateDistance2D(mob);
			if (dist < bestDist)
			{
				bestDist = dist;
				best = mob;
			}
		}
		return best;
	}

	// ===== Support roles: heal the hurt, raise the dead, keep the party buffed =====

	private void supportTick(Member state, long now)
	{
		final Player npc = state.npc;
		final Player owner = state.owner;

		// During a raid a healer must keep the TANK in range - the tank fights the boss in melee while the leader
		// (often a caster) hangs back, so anchoring on the leader left the tank OUT of heal range and it died
		// un-healed (the wipe in the trace). Follow/range-gate on the tank in a raid, otherwise on the leader.
		final Monster raidBoss = engagedRaid(state);
		final boolean raid = raidBoss != null;
		final Player anchor = healAnchor(state, raid);
		// In camp mode (never during a raid - you don't camp-pull a boss) a support holds at the camp and heals the
		// fighters there, instead of chasing the leader if they wander off. Its heal/buff/res targeting is unchanged
		// (it scans party members in range), so the tank/DPS killing at the camp stay covered.
		final Camp camp = raid ? null : _camps.get(owner.getObjectId());

		// 0) If the anchor is out of support range, catching up beats trying to cast at nothing. This also keeps a
		// buffer from getting starved on an out-of-range cast and "stopping" - it always moves into range first.
		if (camp != null)
		{
			if (npc.calculateDistance2D(camp.anchor) > SUPPORT_RANGE)
			{
				holdAtCamp(state, camp.anchor); // drifted from camp - get back before casting
				return;
			}
			// in range of the camp: fall through to normal heal/buff/res on whoever's fighting here
		}
		else if (npc.calculateDistance2D(anchor) > SUPPORT_RANGE)
		{
			if (state.following)
			{
				if (!raid || (raidBoss == null) || !positionRaidBackline(state, raidBoss, RAID_BACKLINE_RANGE, RAID_BACKLINE_TOLERANCE))
				{
					driveFollow(state, anchor);
				}
			}
			return;
		}

		// On-demand specific buff ("give me X" / "greater might on <name>"): cast it on the requested target (the
		// leader, or a named party member), honoured even if that target's archetype would normally skip it.
		if (state.pendingBuff != null)
		{
			final Skill buff = state.pendingBuff;
			final Player target = (state.pendingBuffTarget != null) ? state.pendingBuffTarget : owner;
			final boolean targetOk = (target != null) && !target.isDead() && (npc.calculateDistance2D(target) <= SUPPORT_RANGE);
			if (targetOk && !npc.isSkillDisabled(buff) && (npc.getCurrentMp() >= buff.getMpConsume()))
			{
				if (!readyToCast(npc))
				{
					return; // getting up first; cast on the next tick (pendingBuff kept so the order isn't lost)
				}
				state.pendingBuff = null;
				state.pendingBuffTarget = null;
				if (!PhantomBuffs.reserveBuff(target.getObjectId(), buff.getId(), npc.getObjectId(), PhantomBuffs.buffHoldMillis(buff)))
				{
					return; // another support (or the buddy) is already landing this exact buff on the target
				}
				dbgBuff(npc, target, buff, "on-demand");
				npc.setTarget(target);
				npc.doCast(buff);
				return;
			}
			state.pendingBuff = null; // can't satisfy it right now (dead / oom / disabled / out of range) - drop it
			state.pendingBuffTarget = null;
		}

		// On-demand "heal me": one heal on the leader even at full HP.
		if (state.healNow)
		{
			Skill onDemandHeal = emergencyHeal(state);
			if ((onDemandHeal == null) || npc.isSkillDisabled(onDemandHeal) || (npc.getCurrentMp() < onDemandHeal.getMpConsume()))
			{
				onDemandHeal = anyAffordableHeal(state); // low on MP - use whatever heal we can still cast
			}
			if ((onDemandHeal != null) && !owner.isDead() && !npc.isSkillDisabled(onDemandHeal) && (npc.getCurrentMp() >= onDemandHeal.getMpConsume()))
			{
				if (!readyToCast(npc))
				{
					return; // getting up first; heal on the next tick (healNow kept so the order isn't lost)
				}
				state.healNow = false;
				npc.setTarget(owner);
				npc.doCast(onDemandHeal);
				_healedThisTick.add(owner.getObjectId());
				return;
			}
			state.healNow = false; // can't satisfy it right now
		}

		// 1) Emergency heal first. A human healer does not start a res while the live tank is at 35%. A FAST heal is
		// preferred - a critically low member can die during a slow (5s) cast, so speed wins over a bigger heal - but
		// if no fast heal is affordable the healer uses whatever heal it can still pay for: a slow or cheap heal beats
		// letting the member die (the low-MP "healing stops completely" gap).
		final Player urgent = mostHurtBelow(state, CRITICAL_HEAL_PERCENT);
		if (urgent != null)
		{
			Skill emg = emergencyHeal(state);
			if ((emg == null) || npc.isSkillDisabled(emg) || (npc.getCurrentMp() < emg.getMpConsume()))
			{
				emg = anyAffordableHeal(state);
			}
			if ((emg != null) && !npc.isSkillDisabled(emg) && (npc.getCurrentMp() >= emg.getMpConsume()))
			{
				if (!readyToCast(npc))
				{
					return;
				}
				if (DEBUG && raid)
				{
					dbg("HEAL " + npc.getName() + " -> " + roleLabel(urgent) + " '" + urgent.getName() + "' (hp " + urgent.getCurrentHpPercent() + "%) with " + emg.getName());
				}
				npc.setTarget(urgent);
				npc.doCast(emg);
				_healedThisTick.add(urgent.getObjectId());
				return;
			}
		}

		// 1b) Cleanse: strip a removable debuff (poison / bleed / paralyze / petrify) off a party member, the tank
		// first - a paralyzed or held tank loses the boss and the party wipes. Only fires when the support knows a
		// cleanse that can actually remove the debuff carried, so it isn't wasted. Sits above res so a held tank is
		// freed before it dies and needs raising.
		final Skill cleanse = cleanse(state);
		if ((cleanse != null) && castable(npc, cleanse))
		{
			final Player cursed = pickCleanseTarget(state);
			if (cursed != null)
			{
				if (!readyToCast(npc))
				{
					return; // getting up first; cleanse on the next tick
				}
				if (DEBUG && raid)
				{
					dbg("CLEANSE " + npc.getName() + " -> " + roleLabel(cursed) + " '" + cursed.getName() + "' with " + cleanse.getName());
				}
				npc.setTarget(cursed);
				npc.doCast(cleanse);
				return;
			}
		}

		// 2) Raise the fallen: a dead real player first, then a recruited bot member kept as a corpse (battle-res).
		final Skill res = res(state);
		if ((res != null) && !npc.isSkillDisabled(res) && (npc.getCurrentMp() >= res.getMpConsume()))
		{
			final Player corpse = findResTarget(state, now);
			if (corpse != null)
			{
				if (!readyToCast(npc))
				{
					return; // getting up first; res on the next tick
				}
				if (!PhantomBuffs.claimRes(corpse.getObjectId(), npc.getObjectId(), RES_CLAIM_MS))
				{
					return; // a different rezzer (party healer/buffer or the personal buddy) is already raising it
				}
				dbg("RES " + npc.getName() + " rezzing " + roleLabel(corpse) + " '" + corpse.getName() + "'");
				npc.setTarget(corpse);
				npc.doCast(res);
				return;
			}
		}

		// 2b) Step out of a telegraphed raid AoE - but only now that nobody is dying (emergency heal), debuffed (cleanse)
		// or awaiting a res. A dead support heals nothing, yet abandoning a critical heal to dodge is worse, so the
		// life-saving actions above always win and the support only repositions when the party is otherwise stable.
		if (raid && avoidAoe(state))
		{
			return;
		}

		// 2c) Save yourself. A support that dies takes the party with it, and healing generates hate, so a healer
		// pulls mobs onto itself just by doing its job. Placed here deliberately: a dying ally (emergency heal), a
		// disabling debuff and a res all still outrank it, but the routine upkeep below does not.
		if (supportDefend(state))
		{
			return;
		}

		// 3) Recharge: an Elder/Shillien Elder refills a party caster's MP (healers first, then the tank, then other
		// casters). During raids this runs before non-critical top-offs so the Bishop/Prophet don't hit empty before
		// the next spike. Emergency heals above still win.
		final Skill rech = recharge(state);
		if ((rech != null) && castable(npc, rech))
		{
			final Player drained = pickRechargeTarget(state, raid);
			if (drained != null)
			{
				if (!readyToCast(npc))
				{
					return; // getting up first; recharge on the next tick
				}
				if (DEBUG && raid)
				{
					dbg("RECHARGE " + npc.getName() + " -> " + roleLabel(drained) + " '" + drained.getName() + "' (mp " + drained.getCurrentMpPercent() + "%)");
				}
				npc.setTarget(drained);
				npc.doCast(rech);
				return;
			}
		}

		// 3b) Group heal: when several party members in range are hurt at once, a single party heal (Group Heal /
		// Greater Group Heal / Major Group Heal) tops them all far more cheaply than pecking them one at a time - the
		// Bishop/Cardinal's signature party healing, which the support never did before. Near-death members are already
		// covered by the emergency single heal above; this fires only at GROUP_HEAL_MIN_TARGETS+ hurt so it isn't wasted
		// on one dip. A PARTY-target heal resolves the whole party from the caster, so it targets itself.
		final Skill groupHeal = groupHeal(state);
		if ((groupHeal != null) && (countHurtBelow(state, GROUP_HEAL_HP_PERCENT) >= GROUP_HEAL_MIN_TARGETS))
		{
			if (!readyToCast(npc))
			{
				return; // getting up first; group heal on the next tick
			}
			if (DEBUG && raid)
			{
				dbg("GROUPHEAL " + npc.getName() + " '" + groupHeal.getName() + "' (" + countHurtBelow(state, GROUP_HEAL_HP_PERCENT) + " hurt)");
			}
			npc.setTarget(npc);
			npc.doCast(groupHeal);
			return;
		}

		// 4) Heal. Under a raid this is pre-emptive and tank-first (a boss spike outruns reactive 60%-only healing):
		// a critically low member is an emergency, otherwise the tank is kept topped, then the most-hurt member.
		// Outside a raid it's the old behaviour - the most-hurt member below the normal threshold, then self.
		Player worst = pickHealTarget(state, raid);
		if ((worst == null) && (npc.getCurrentHpPercent() < SELF_HEAL_PERCENT))
		{
			worst = npc;
		}
		if (worst != null)
		{
			// Use a cheap heal for a small top-off and save the big (Greater) heal for a real gap. Keeping the tank
			// topped to 90% with Greater Battle Heal every tick was the other half of the MP drain - a 15% top-off
			// does not need the most expensive heal in the book. If that ideal heal isn't affordable right now, drop
			// to any heal we can still pay for - normal healing must not stop just because the big/fast heal is out.
			Skill h = chooseHeal(state, worst);
			if ((h == null) || npc.isSkillDisabled(h) || (npc.getCurrentMp() < h.getMpConsume()))
			{
				h = anyAffordableHeal(state);
			}
			if ((h != null) && !npc.isSkillDisabled(h) && (npc.getCurrentMp() >= h.getMpConsume()))
			{
				if (!readyToCast(npc))
				{
					return; // getting up first; heal on the next tick
				}
				if (DEBUG && raid)
				{
					dbg("HEAL " + npc.getName() + " -> " + roleLabel(worst) + " '" + worst.getName() + "' (hp " + worst.getCurrentHpPercent() + "%) with " + h.getName());
				}
				npc.setTarget(worst);
				npc.doCast(h);
				_healedThisTick.add(worst.getObjectId());
				return;
			}
		}

		// 5) On-demand "(re)buff <who>": recast a full kit on each queued target, one buff per tick, regardless of
		// time left. Sits below res/heal so a long "buff all" can't get someone killed while it grinds through buffs.
		if (state.rebuffing && forceRebuff(state))
		{
			return;
		}

		// 6) Keep the whole party (and self) buffed; one buff per tick so it works through its list. NOT during a raid:
		// members arrive pre-buffed and a real party buffs BEFORE the pull, not during it. Mid-boss the maintenance
		// re-cast just chases buffs that the boss's hits keep interrupting (it reads as a support stuck "re-buffing one
		// person"), and it steals casts the support should be spending on heals/recharge. So while a raid boss is
		// engaged the support skips buff upkeep entirely and stays on healing/battery duty; upkeep resumes after.
		if (!raid && maintainPartyBuffs(state))
		{
			return;
		}

		// 7) MP sustain: sit to recover when safe, stand on threat / recovery.
		if (restForMp(state))
		{
			return;
		}

		// 8) Default: hold at the camp if camping, else stay near the anchor (the tank in a raid, else the leader).
		if (camp != null)
		{
			holdAtCamp(state, camp.anchor);
		}
		else if (state.following)
		{
			if (!raid || (raidBoss == null) || !positionRaidBackline(state, raidBoss, RAID_BACKLINE_RANGE, RAID_BACKLINE_TOLERANCE))
			{
				driveFollow(state, anchor);
			}
		}
	}

	/** Who a support should keep itself in range of: support sticks to the live tank during a raid (so it can heal it), else the leader. */
	private Player healAnchor(Member state, boolean raid)
	{
		if (raid && state.isSupport())
		{
			final Player tank = findTank(state);
			if ((tank != null) && !tank.isDead())
			{
				return tank;
			}
		}
		return state.owner;
	}

	/**
	 * A fallen party member within range this healer should raise: a dead real player takes priority (a human is
	 * waiting to get back up), then a dead recruited bot member kept as a corpse. Skips itself and any corpse whose
	 * revive is already pending (it's about to stand), so multiple healers don't double-cast on the same target.
	 */
	private Player findResTarget(Member state, long now)
	{
		final Player npc = state.npc;
		final Party party = state.owner.getParty();
		if (party == null)
		{
			return null;
		}
		Player botCorpse = null;
		for (Player member : party.getMembers())
		{
			if ((member == npc) || !member.isDead() || member.isReviveRequested() || PhantomBuffs.isResClaimed(member.getObjectId(), npc.getObjectId(), now) || (npc.calculateDistance2D(member) > SUPPORT_RANGE))
			{
				continue;
			}
			if (member.getClient() != null)
			{
				return member; // a fallen human - top priority
			}
			if ((botCorpse == null) && _members.containsKey(member.getObjectId()))
			{
				botCorpse = member; // a fallen recruited member - raise once no human needs it
			}
		}
		return botCorpse;
	}

	/**
	 * Chooses who this healer should heal this tick. Under a raid it is pre-emptive and tank-first: an emergency
	 * (any member below {@link #CRITICAL_HEAL_PERCENT}) is treated as the most-hurt member, otherwise the tank is
	 * kept topped to {@link #RAID_TANK_HEAL_PERCENT} (it eats the boss), then the most-hurt member below
	 * {@link #RAID_HEAL_PERCENT}. Outside a raid it's just the most-hurt member below {@link #OWNER_HEAL_PERCENT}.
	 */
	private Player pickHealTarget(Member state, boolean raid)
	{
		if (!raid)
		{
			return mostHurtBelow(state, OWNER_HEAL_PERCENT);
		}
		// Emergency first: whoever is critically low, even if it's not the tank, gets the heal now.
		final Player critical = mostHurtBelow(state, CRITICAL_HEAL_PERCENT);
		if (critical != null)
		{
			return critical;
		}
		// Then keep the tank topped so the next boss spike doesn't drop it from comfortable to dead - unless another
		// healer is already healing it (coordination: don't all stack on the tank and waste heals on overheal).
		final Player tank = findTank(state);
		if ((tank != null) && !tank.isDead() && (state.npc.calculateDistance2D(tank) <= SUPPORT_RANGE) && (tank.getCurrentHpPercent() < RAID_TANK_HEAL_PERCENT) //
			&& !((tank.getCurrentHpPercent() >= CRITICAL_HEAL_PERCENT) && beingHealedByAnother(state, tank)))
		{
			return tank;
		}
		// Then the most-hurt of everyone else, pre-emptively (higher threshold than normal farming).
		return mostHurtBelow(state, RAID_HEAL_PERCENT);
	}

	/**
	 * The most-hurt living party member in support range whose HP% is under {@code threshold}, or {@code null}.
	 * A member already being healed by another healer is skipped (so multiple healers spread out instead of all
	 * piling onto one target) UNLESS it is critically low, where stacking heals to save it is the right call.
	 */
	private Player mostHurtBelow(Member state, int threshold)
	{
		final Player npc = state.npc;
		final Party party = state.owner.getParty();
		if (party == null)
		{
			return (!state.owner.isDead() && (state.owner.getCurrentHpPercent() < threshold)) ? state.owner : null;
		}
		Player worst = null;
		int worstPct = threshold;
		for (Player member : party.getMembers())
		{
			if (member.isDead() || (npc.calculateDistance2D(member) > SUPPORT_RANGE) || (member.getCurrentHpPercent() >= worstPct))
			{
				continue;
			}
			if ((member.getCurrentHpPercent() >= CRITICAL_HEAL_PERCENT) && beingHealedByAnother(state, member))
			{
				continue; // another healer has this one covered - look for someone else
			}
			worst = member;
			worstPct = member.getCurrentHpPercent();
		}
		return worst;
	}

	/**
	 * {@code true} if {@code target} is already covered by another support this tick - either one committed a heal to
	 * it earlier in this same tick pass ({@link #_healedThisTick}, which catches same-tick simultaneity the casting
	 * check misses), or another healer is mid-cast on it. Lets the others spread to the next-most-hurt instead of
	 * stacking heals and wasting MP on overheal.
	 */
	private boolean beingHealedByAnother(Member state, Player target)
	{
		if (_healedThisTick.contains(target.getObjectId()))
		{
			return true;
		}
		for (Member m : _members.values())
		{
			if ((m != state) && m.isSupport() && (m.owner == state.owner) && !m.npc.isDead() && m.npc.isCastingNow() && (m.npc.getTarget() == target))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * {@code true} if another support in the party is right now casting the same buff on the same target. With a
	 * buffer (Prophet) and one or more healers (Elder) all maintaining their own kits, the kits overlap on the common
	 * buffs (Wind Walk etc.); without this guard two supports both see it "missing" on the same tick and both cast it,
	 * wasting MP and reading as several supports piling buffs on one person. The complementary buffs (a Prophet's
	 * fighter kit, an Elder's caster kit) are unaffected - only a genuine same-buff/same-target collision is skipped.
	 */
	private boolean beingBuffedByAnother(Member state, Player target, Skill buff)
	{
		for (Member m : _members.values())
		{
			if ((m != state) && m.isSupport() && (m.owner == state.owner) && !m.npc.isDead() && m.npc.isCastingNow() //
				&& (m.npc.getTarget() == target) && (m.npc.getLastSkillCast() != null) && (m.npc.getLastSkillCast().getId() == buff.getId()))
			{
				return true;
			}
		}
		return false;
	}

	/** The Recharge skill this support knows (Elder/Shillien Elder have it; Bishops don't), or {@code null}. */
	private Skill recharge(Member state)
	{
		if (!state.rechargeLookedUp)
		{
			state.rechargeLookedUp = true;
			state.recharge = state.npc.getKnownSkill(RECHARGE_ID);
		}
		return state.recharge;
	}

	/**
	 * The party mana-user this support should Recharge: a HEALER first (so it can keep healing), then the TANK (so it
	 * can keep taunting), then another caster (BUFFER/NUKER) - whichever is most starved below {@link #RECHARGE_MP_PERCENT}
	 * and in range. Melee are skipped (MP isn't their bottleneck), but a bow ARCHER that has bottomed out IS topped
	 * up: bow shots consume MP and simply fail at empty, so a starved archer would otherwise stall on a live target.
	 */
	private Player pickRechargeTarget(Member state, boolean raid)
	{
		final Player npc = state.npc;
		final int threshold = raid ? RECHARGE_RAID_MP_PERCENT : RECHARGE_MP_PERCENT;
		Player best = null;
		int bestScore = Integer.MAX_VALUE;
		for (Member m : _members.values())
		{
			if ((m.owner != state.owner) || (m.npc == npc) || m.npc.isDead() || (npc.calculateDistance2D(m.npc) > SUPPORT_RANGE))
			{
				continue;
			}
			final int mp = m.npc.getCurrentMpPercent();
			if (mp >= threshold)
			{
				continue;
			}
			if (m.npc.getKnownSkill(RECHARGE_ID) != null)
			{
				continue; // Interlude Recharge cannot be used on a class that has Recharge
			}
			final int prio = rechargePriority(m.role, mp);
			if (prio < 0)
			{
				continue; // plain melee (and a not-yet-starved archer) don't need recharge
			}
			final int score = (prio * 1000) + mp; // higher priority first, then the most drained
			if (score < bestScore)
			{
				bestScore = score;
				best = m.npc;
			}
		}
		// The human leader was never a Recharge candidate (this loop only walks recruited members). On an offline
		// server the human is the party's most important member, so a mana-using leader must get battery too - and by
		// the SAME role rules as a recruited member, so it also covers a human Sword Singer / Dancer (song/dance MP)
		// and a genuinely bow-starved human archer, not just mage classes. Recharge can't target a class that itself
		// knows Recharge (Elder / SE), so those are excluded.
		final Player owner = state.owner;
		if ((owner != npc) && !owner.isDead() && (npc.calculateDistance2D(owner) <= SUPPORT_RANGE) //
			&& (owner.getCurrentMpPercent() < threshold) && (owner.getKnownSkill(RECHARGE_ID) == null))
		{
			final int mp = owner.getCurrentMpPercent();
			final int prio = rechargePriority(PhantomManager.roleForClass(owner.getPlayerClass()), mp);
			if (prio >= 0)
			{
				final int score = (prio * 1000) + mp;
				if (score < bestScore)
				{
					bestScore = score;
					best = owner;
				}
			}
		}
		return best;
	}

	/**
	 * Recharge priority for a party role given its current MP% (lower number = topped up first), or {@code -1} if
	 * this role/state does not need battery at all. Healers first (they keep everyone alive), then the tank, then the
	 * other mana users - caster DPS, singers and dancers (their songs/dances ARE the party's damage and they know no
	 * Recharge), and support buffers - and finally a genuinely bow-starved archer (its MP funds plain shots, not the
	 * party's spells, so it is last). Plain melee and a not-yet-starved archer return -1. Shared by the recruited-member
	 * scan and the human-leader check so both are judged by the same rules.
	 */
	private static int rechargePriority(PartyRole role, int mp)
	{
		if (role == PartyRole.HEALER)
		{
			return 0;
		}
		if (role == PartyRole.TANK)
		{
			return 1;
		}
		if (role.isSupport() || (role == PartyRole.NUKER) || (role == PartyRole.SINGER) || (role == PartyRole.DANCER))
		{
			return 2;
		}
		if ((role == PartyRole.ARCHER) && (mp < ARCHER_SKILL_PARK_MP))
		{
			return 3;
		}
		return -1;
	}

	/** The recruited TANK in this healer's party (the one that should be kept topped under a raid), or {@code null}. */
	private Player findTank(Member state)
	{
		final Member tank = findTankState(state);
		return (tank == null) ? null : tank.npc;
	}

	private Member findTankState(Member state)
	{
		for (Member m : _members.values())
		{
			if ((m.role == PartyRole.TANK) && (m.owner == state.owner) && !m.npc.isDead())
			{
				return m;
			}
		}
		return null;
	}

	/**
	 * The human party leader when they are personally acting as the raid tank: a living tank-class player. Returned only
	 * for the human, never a phantom - the recruited tank keeps its own {@link Member} path (recovery, reclaim, taunt
	 * upkeep) which a human does not have. Used to release DPS immediately once the human genuinely holds the boss,
	 * instead of leaning on the no-tank fail-open timeout. It deliberately does NOT stand in for the phantom tank in the
	 * squishy self-defense / reclaim logic: a human is not guaranteed to re-taunt, so those paths stay fail-open.
	 */
	private Player humanRaidTank(Member state)
	{
		final Player owner = state.owner;
		if ((owner == null) || owner.isDead())
		{
			return null;
		}
		return (PhantomManager.roleForClass(owner.getPlayerClass()) == PartyRole.TANK) ? owner : null;
	}

	/** {@code true} if a live raid boss is in combat near the party - drives pre-emptive healing and no MP-sitting. */
	private boolean raidEngaged(Member state)
	{
		return engagedRaid(state) != null;
	}

	// ===== Raid tactics: kill order, AoE step-out, debuff cleanse =====

	/** A damage role (not the tank, not a support) - the roles that focus-fire and apply the raid kill order. */
	private static boolean isDps(PartyRole role)
	{
		return (role != PartyRole.TANK) && !role.isSupport();
	}

	/** A ranged damage role (archer or nuker) - the roles that can step out of a boss AoE without giving up their dps. */
	private static boolean isRanged(PartyRole role)
	{
		return (role == PartyRole.ARCHER) || role.mage;
	}

	/**
	 * Raid kill order for a DPS member: when the boss has live minions/adds up, clear the adds before the boss. Returns
	 * the add this member should switch to (the lowest-HP one, to finish kills fastest), or {@code null} to stay on the
	 * boss. Only adds the member may legally attack (tank-held or released, per {@link #mayAttackRaid}) are considered,
	 * so this never makes DPS pull an untanked add - it just re-prioritises among targets already in play.
	 */
	private Monster raidKillOrderTarget(Member state, Monster bossTarget)
	{
		Monster best = null;
		int bestHp = Integer.MAX_VALUE;
		for (Monster raid : engagedRaids(state))
		{
			if (raid.isDead() || !raid.isRaidMinion() || (raid == bossTarget) || !mayAttackRaid(state, raid))
			{
				continue;
			}
			final int hp = (int) raid.getCurrentHp();
			if (hp < bestHp)
			{
				bestHp = hp;
				best = raid;
			}
		}
		return best;
	}

	/**
	 * The raid mob this member may legally fight when its normal assist focus is missing or gated: the lowest-HP
	 * engaged raid mob that passes {@link #mayAttackRaid} (execute-rule slivers, minions on the leader or on this
	 * member, and released targets all qualify through it), or {@code null} to keep holding. This is what keeps
	 * the party fighting when the raid is beating on THEM while the leader is dead, targetless, or aiming at the
	 * one target that is still gated - the second Ruell attempt had the whole DPS line idle for minutes in
	 * exactly those states.
	 */
	private Monster raidFallbackTarget(Member state)
	{
		Monster best = null;
		int bestHp = Integer.MAX_VALUE;
		for (Monster raid : engagedRaids(state))
		{
			if (raid.isDead() || !mayAttackRaid(state, raid))
			{
				continue;
			}
			final int hp = (int) raid.getCurrentHp();
			if (hp < bestHp)
			{
				bestHp = hp;
				best = raid;
			}
		}
		return best;
	}

	/** A raid mob near the party currently mid-cast of a telegraphed AoE/teleport, or {@code null}. */
	private Monster dangerousCaster(Member state)
	{
		for (Monster raid : engagedRaids(state))
		{
			if (raid.isCastingNow())
			{
				final Skill skill = raid.getLastSkillCast();
				if ((skill != null) && isDangerousAoe(skill))
				{
					return raid;
				}
			}
		}
		return null;
	}

	/**
	 * {@code true} if a skill is a telegraphed area effect worth dodging. It must (a) hit an area - an AoE target type
	 * or a splash radius - AND (b) actually deal damage. The damage gate matters: a boss's area <b>Root/Hold/snare</b>
	 * (e.g. Zaken's Chief Mate "Hold", abnormal ROOT_MAGICALLY) is an AoE with no damage; stepping out of it is both
	 * pointless (standing in a root doesn't hurt) and impossible (a rooted member can't move), and a boss that spams it
	 * was making the whole backline abandon healing/fire every tick to "dodge" an un-dodgeable snare. Only real AoE
	 * <i>damage</i> is worth breaking position for.
	 */
	private static boolean isDangerousAoe(Skill skill)
	{
		return (skill.isAOE() || (skill.getAffectRange() >= AOE_MIN_RADIUS)) //
			&& skill.hasEffectType(EffectType.MAGICAL_ATTACK, EffectType.PHYSICAL_ATTACK, EffectType.HP_DRAIN, EffectType.DEATH_LINK);
	}

	/**
	 * If a raid mob is mid-cast of a telegraphed AoE and this member is standing inside the blast, move it out past the
	 * radius (to a spread backline) and skip this tick's action. A member already clear of the radius keeps fighting.
	 * @return {@code true} if the member is stepping out (the caller should return)
	 */
	private boolean avoidAoe(Member state)
	{
		final Monster src = dangerousCaster(state);
		if (src == null)
		{
			return false;
		}
		final Skill danger = src.getLastSkillCast();
		if (danger == null)
		{
			return false;
		}
		final int radius = Math.max(danger.getAffectRange(), AOE_MIN_RADIUS) + AOE_STEP_MARGIN;
		if (state.npc.calculateDistance2D(src) >= radius)
		{
			return false; // already clear of the blast - no need to give up the dps/heal
		}
		standIfSitting(state.npc);
		if (DEBUG)
		{
			dbg("AOE " + state.npc.getName() + " (" + state.role + ") steps out of '" + danger.getName() + "' cast by '" + src.getName() + "'");
		}
		positionRaidBackline(state, src, radius, RAID_BACKLINE_TOLERANCE);
		return true;
	}

	/** The cleanse this support knows (Purify, else Cure Poison, else Cure Bleeding), or {@code null}; resolved once. */
	private Skill cleanse(Member state)
	{
		if (!state.cleanseLookedUp)
		{
			state.cleanseLookedUp = true;
			Skill known = state.npc.getKnownSkill(PURIFY_ID);
			if (known != null)
			{
				state.cleanse = known;
				state.cleansable = EnumSet.of(AbnormalType.POISON, AbnormalType.BLEEDING, AbnormalType.PARALYZE, AbnormalType.TURN_STONE);
			}
			else if ((known = state.npc.getKnownSkill(CURE_POISON_ID)) != null)
			{
				state.cleanse = known;
				state.cleansable = EnumSet.of(AbnormalType.POISON);
			}
			else if ((known = state.npc.getKnownSkill(CURE_BLEEDING_ID)) != null)
			{
				state.cleanse = known;
				state.cleansable = EnumSet.of(AbnormalType.BLEEDING);
			}
		}
		return state.cleanse;
	}

	/**
	 * A party member in range carrying a debuff this support's cleanse can actually remove (the tank first, since a
	 * paralyzed/held tank drops the boss), or {@code null}. Matching the carried debuff against the cleanse's removable
	 * set keeps it from casting on an uncurable debuff.
	 */
	private Player pickCleanseTarget(Member state)
	{
		final Set<AbnormalType> removable = state.cleansable;
		if (removable == null)
		{
			return null;
		}
		if ((state.owner == null) || !state.owner.isInParty())
		{
			return (inSupportRange(state, state.owner) && carriesRemovable(state.owner, removable)) ? state.owner : null;
		}
		Player found = null;
		for (Player member : state.owner.getParty().getMembers())
		{
			if (member.isDead() || !inSupportRange(state, member) || !carriesRemovable(member, removable))
			{
				continue;
			}
			if (isTankPlayer(state, member))
			{
				return member; // a debuffed tank is the priority
			}
			if (found == null)
			{
				found = member;
			}
		}
		return found;
	}

	private boolean inSupportRange(Member state, Player who)
	{
		return (who != null) && (state.npc.calculateDistance2D(who) <= SUPPORT_RANGE);
	}

	/** {@code true} if {@code who} carries any debuff whose abnormal type is in this cleanse's removable set. */
	private static boolean carriesRemovable(Player who, Set<AbnormalType> removable)
	{
		if (who == null)
		{
			return false;
		}
		for (BuffInfo info : who.getEffectList().getDebuffs())
		{
			if (removable.contains(info.getSkill().getAbnormalType()))
			{
				return true;
			}
		}
		return false;
	}

	/** {@code true} if {@code who} is this party's recruited tank. */
	private boolean isTankPlayer(Member state, Player who)
	{
		final Member tank = findTankState(state);
		return (tank != null) && (tank.npc == who);
	}

	private boolean maintainPartyBuffs(Member state)
	{
		if (buffs(state).isEmpty())
		{
			return false;
		}
		final Player owner = state.owner;
		// Upkeep order, one buff per tick: SELF first (so e.g. Acumen lands and the buffer casts the rest faster),
		// then any OTHER support classes (get them ready so they help buff too), then the human leader, then everyone
		// else (recruited DPS/tank and any human members). Recruited members ARE maintained now - they spawn with the
		// full kit (PhantomBuffs.applyFullBuffs mirrors the maintained kit), so upkeep finds their slots filled and
		// casts nothing until a 20-minute buff actually lapses. That keeps "buff everyone" cheap: no full re-cast
		// before a pull (the old MP-drain), just gap-filling as buffs expire. needsBuff/reserve still gate each cast.
		if (castFirstMissing(state, state.npc, PhantomBuffs.Tier.SELF))
		{
			return true;
		}
		// Other support members before the rest, so the party's buffers come up first and can share the work.
		for (Player member : partyPlayers(owner))
		{
			if (member == state.npc)
			{
				continue;
			}
			final Member m = _members.get(member.getObjectId());
			if ((m != null) && m.isSupport() && castFirstMissing(state, member, PhantomBuffs.Tier.MEMBER))
			{
				return true;
			}
		}
		if (castFirstMissing(state, owner, PhantomBuffs.Tier.LEADER))
		{
			return true;
		}
		for (Player member : partyPlayers(owner))
		{
			if ((member == state.npc) || (member == owner))
			{
				continue;
			}
			final Member m = _members.get(member.getObjectId());
			if ((m != null) && m.isSupport())
			{
				continue; // other supports handled in the pass above
			}
			if (castFirstMissing(state, member, PhantomBuffs.Tier.MEMBER))
			{
				return true;
			}
		}
		return false;
	}

	/** The players the support keeps buffed: the leader's party if grouped, otherwise just the leader (solo). */
	private static List<Player> partyPlayers(Player owner)
	{
		return owner.isInParty() ? owner.getParty().getMembers() : List.of(owner);
	}

	/** True if this buff target is a TANK (recruited role or, for the human leader, its class role) - used to withhold Berserker Spirit. */
	private static boolean isTankTarget(Player target)
	{
		return PhantomManager.roleForClass(target.getPlayerClass()) == PartyRole.TANK;
	}

	/** Begins a forced (re)buff: every {@code targets} entry is owed a full archetype kit, served one cast per tick. */
	private void startRebuff(Member state, List<Player> targets)
	{
		state.rebuffQueue = targets;
		state.rebuffIdx = 0;
		state.rebuffing = !targets.isEmpty();
	}

	/**
	 * The "buff all" / "rebuff" target queue, ordered to match the automatic upkeep priority: the acting support
	 * itself first, then any OTHER support members, then the human leader, then everyone else. A plain copy of
	 * {@code getMembers()} would follow the party-list order and could buff a DD before the party's own healers/
	 * buffers - this guarantees the intended self -> support -> user -> members order for the explicit order too.
	 */
	private List<Player> partyTargets(Member state)
	{
		final Player owner = state.owner;
		final Player self = state.npc;
		final List<Player> source = owner.isInParty() ? owner.getParty().getMembers() : List.of(owner);
		final Set<Player> ordered = new LinkedHashSet<>(); // preserves insertion order and de-duplicates
		ordered.add(self); // self first, so its own kit is up and it buffs the rest at full speed
		for (Player p : source) // then the other support classes, so the party's buffers come online early
		{
			final Member m = _members.get(p.getObjectId());
			if ((m != null) && m.isSupport())
			{
				ordered.add(p);
			}
		}
		if (owner != self)
		{
			ordered.add(owner); // the human leader
		}
		ordered.addAll(source); // everyone else, in party order (already-queued targets are skipped by the set)
		return new ArrayList<>(ordered);
	}

	/**
	 * The party member whose name appears in the order text ("buff Trevor"), or {@code null} if none is named. The
	 * acting member's OWN name is skipped, so "Ulondras buff Rhael" (which names both the healer being addressed and
	 * the target) resolves to the target Rhael, not the healer itself.
	 */
	private static Player findPartyMemberByName(Member state, String text)
	{
		final Player owner = state.owner;
		if (!owner.isInParty())
		{
			return null;
		}
		for (Player member : owner.getParty().getMembers())
		{
			if (member == state.npc)
			{
				continue; // don't match the addressed member's own name - we want the target it should buff
			}
			final String name = member.getName().toLowerCase();
			if (!name.isEmpty() && text.contains(name))
			{
				return member;
			}
		}
		return null;
	}

	/**
	 * "(re)buff" order: recast a full archetype kit on each queued target one buff per tick, ignoring how long the
	 * current one has left. Walks an index across the buff list per target; when a target's kit is done (or it is
	 * dead / out of range) it advances to the next, clearing the flag once the queue empties.
	 * @return {@code true} while still rebuffing (a cast was issued or the member is standing up to cast)
	 */
	private boolean forceRebuff(Member state)
	{
		final Player npc = state.npc;
		final List<Skill> all = buffs(state);
		while ((state.rebuffQueue != null) && !state.rebuffQueue.isEmpty())
		{
			final Player target = state.rebuffQueue.get(0);
			if ((target == null) || target.isDead() || (npc.calculateDistance2D(target) > SUPPORT_RANGE))
			{
				state.rebuffQueue.remove(0); // can't reach/buff this one now - skip to the next target
				state.rebuffIdx = 0;
				continue;
			}
			final boolean caster = PhantomBuffs.isCaster(target);
			final boolean tank = isTankTarget(target);
			while (state.rebuffIdx < all.size())
			{
				final Skill buff = all.get(state.rebuffIdx);
				// PERMANENT skip: not part of this target's kit (wrong archetype / Berserker on a tank), or out of a
				// reagent that won't refill on its own. Move past it so the rest of the kit still gets cast.
				if (!PhantomBuffs.wanted(buff.getId(), caster, PhantomBuffs.Tier.LEADER) || (tank && PhantomBuffs.tankAvoids(buff.getId())) || !PhantomBuffs.canAffordReagent(npc, buff))
				{
					state.rebuffIdx++;
					continue;
				}
				// TEMPORARY: on cooldown or not enough MP right now. A real buffer waits and completes the requested
				// kit rather than dropping the buff - hold on this same buff (index NOT advanced) and retry next tick.
				if (npc.isSkillDisabled(buff) || (npc.getCurrentMp() < buff.getMpConsume()))
				{
					return true;
				}
				if (!readyToCast(npc))
				{
					return true; // getting up first; cast this same buff next tick (index NOT advanced, so it isn't skipped)
				}
				if (!PhantomBuffs.reserveBuff(target.getObjectId(), buff.getId(), npc.getObjectId(), PhantomBuffs.buffHoldMillis(buff)))
				{
					state.rebuffIdx++; // another support (or the buddy) is already (re)casting this exact buff - skip so it isn't doubled
					continue;
				}
				state.rebuffIdx++;
				dbgBuff(npc, target, buff, "rebuff");
				npc.setTarget(target);
				npc.doCast(buff);
				return true;
			}
			state.rebuffQueue.remove(0); // finished this target's full kit - on to the next
			state.rebuffIdx = 0;
		}
		state.rebuffing = false;
		state.rebuffQueue = null;
		return false;
	}

	/** Casts the first buff this target is missing/about-to-lose that its archetype + tier actually wants. */
	private boolean castFirstMissing(Member state, Player target, PhantomBuffs.Tier tier)
	{
		final Player npc = state.npc;
		if (target.isDead() || (npc.calculateDistance2D(target) > SUPPORT_RANGE))
		{
			return false;
		}
		final boolean caster = PhantomBuffs.isCaster(target);
		final boolean tank = isTankTarget(target);
		for (Skill buff : buffs(state))
		{
			if (!PhantomBuffs.wanted(buff.getId(), caster, tier) || (tank && PhantomBuffs.tankAvoids(buff.getId())) || (npc.getCurrentMp() < buff.getMpConsume()) || !PhantomBuffs.canAffordReagent(npc, buff))
			{
				continue; // wrong archetype, role-inappropriate (Berserker on a tank), out of MP, or out of reagent
			}
			// Presence keys on the abnormal SLOT + LEVEL, not the skill id - see PhantomBuffs.needsBuff. Different
			// buffer classes fill one slot with different skills (PD_UP is Shield 1040 / Chant of Shielding 1009 /
			// Blessings of Pa'agrio 1005), so keying on id looped a mixed-buffer party forever; and a STRONGER effect
			// already in the slot (a P.Atk herb over a low-level Might) would make an id/any-occupant check re-cast a
			// buff the engine keeps rejecting. needsBuff skips an equal-or-stronger slot, upgrades a weaker one, and
			// refreshes near expiry - so no loop, whatever is holding the slot.
			if (PhantomBuffs.needsBuff(target, buff, BUFF_REFRESH_SECONDS))
			{
				if (beingBuffedByAnother(state, target, buff))
				{
					continue; // another support is already landing this exact buff on this target - don't double-cast it
				}
				if (!readyToCast(npc))
				{
					return true; // getting up first; cast on the next tick
				}
				if (!PhantomBuffs.reserveBuff(target.getObjectId(), buff.getId(), npc.getObjectId(), PhantomBuffs.buffHoldMillis(buff)))
				{
					continue; // another bot buffer (party support or the personal buddy) is already landing this exact buff
				}
				dbgBuff(npc, target, buff, "upkeep");
				npc.setTarget(target);
				npc.doCast(buff);
				return true;
			}
		}
		return false;
	}

	/**
	 * MP sustain for casters (support roles + nuker): sit to recharge WHILE the party fights, standing instantly
	 * if a monster comes at the member itself or the leader runs off. Using the leader's combat state here would
	 * mean a caster farming with you is "in danger" almost always and never actually sits.
	 * @return {@code true} if the member is resting (caller should not also follow)
	 */
	private boolean restForMp(Member state)
	{
		final Player npc = state.npc;
		final Player owner = state.owner;
		// ARCHER belongs in this list even though it is not a caster: in Interlude a BOW ATTACK ITSELF COSTS MP
		// (Creature.doAttack charges the weapon's mp_consume per shot, 1 at no-grade/D up to 10 at S), and at
		// zero MP the attack is REFUSED outright - "not enough MP", retried once a second - so a dry archer just
		// stands there shooting nothing. MP is an archer's ammunition, not a spell budget, so it must be allowed
		// to sit and recover like a caster. This is only ever reached with no target (between pulls), so it
		// cannot sit down mid-fight.
		if (!state.isSupport() && (state.role != PartyRole.NUKER) && (state.role != PartyRole.SINGER) && (state.role != PartyRole.DANCER) && (state.role != PartyRole.ARCHER))
		{
			return false; // true melee roles don't sit for MP - their auto-attack is free
		}
		final int mp = npc.getCurrentMpPercent();
		// "threat" must mean "something is actually attacking ME" - not just "a monster exists nearby". While the
		// party farms there is always a live mob within DANGER_RANGE, so the old isMonsterNear() check left a
		// caster permanently "in danger" and it never sat - it just drained to empty (see the gameserver log:
		// threat=true every tick from 45% down to 1%). A real support sits to recharge between casts and only
		// pops up when a mob comes at it directly, which is exactly what underAttack() detects.
		// A raid counts as "threat" even though the tank is holding aggro (so the healer itself isn't being hit):
		// a support must NEVER sit to meditate mid-boss - a sitting healer heals nothing and the party wipes. This
		// is the normal-farm "sit between pulls" behaviour staying intact, but switched off while a raid is engaged.
		final boolean threat = underAttack(npc) || raidEngaged(state);
		final boolean ownerFar = npc.calculateDistance2D(owner) > SUPPORT_RANGE;
		if (npc.isSitting())
		{
			if ((mp >= MP_REST_STAND) || threat || ownerFar)
			{
				LOGGER.info("===== PARTY-MP [stand] " + npc.getName() + " ===== standing (recovered=" + (mp >= MP_REST_STAND) + " underAttack=" + threat + " ownerFar=" + ownerFar + " mp=" + mp + "%)");
				npc.standUp();
				state.oomBarked = false; // re-arm the "oom, sec" call for the next rest
				return false;
			}
			return true;
		}
		// Sit to recover MP when it drops to the low mark, it's safe, the leader is close, and a recent "stand"
		// order isn't still holding it on its feet.
		if ((mp < MP_REST_SIT) && !threat && !ownerFar && (System.currentTimeMillis() >= state.noSitUntil))
		{
			// sitDown(false) bypasses the "Cannot sit while casting" guard (a clientless caster's cast flag can
			// linger and otherwise blocks the sit forever - it warns but never actually sits).
			npc.abortCast();
			npc.getAI().setIntention(Intention.IDLE);
			npc.sitDown(false);
			if (npc.isSitting())
			{
				LOGGER.info("===== PARTY-MP [sit] " + npc.getName() + " ===== sitting to recover MP (mp=" + mp + "%)");
				// A support announces its mana break - the party's healing/buffs pausing silently reads as a bug.
				if (state.isSupport() && !state.oomBarked)
				{
					state.oomBarked = true;
					bark(state, "You just ran out of mana and sat down to recover before you can heal or buff again. Tell your party in one very short line.", "oom, sec");
				}
			}
			else
			{
				LOGGER.info("===== PARTY-MP [sit-FAILED] " + npc.getName() + " ===== sitDown(false) rejected at mp=" + mp + "%:"
					+ " sitInProgress=" + npc.isSittingProgress()
					+ " castingNow=" + npc.isCastingNow()
					+ " attackingNow=" + npc.isAttackingNow()
					+ " attackDisabled=" + npc.isAttackDisabled()
					+ " immobilized=" + npc.isImmobilized()
					+ " outOfControl=" + npc.isOutOfControl()
					+ " paralyzed=" + npc.isParalyzed());
			}
			return true;
		}
		return false;
	}

	// ===== Human touches: barks (proactive party chat), emotes, party mood =====

	// Canned downtime small talk, used only when the brain is offline.
	private static final String[] SMALL_TALK =
	{
		"decent spot this",
		"xp is not bad here",
		"could do this all day",
		"going through shots fast today",
		"quiet out here",
		"we're doing alright"
	};

	/** A recruit matching the leader spawns near, not at, the leader's level - clamped to Interlude's 1..80. */
	private static int spreadLevel(int level)
	{
		return Math.max(1, Math.min(80, level + Rnd.get(-RECRUIT_LEVEL_SPREAD, RECRUIT_LEVEL_SPREAD)));
	}

	/**
	 * Runs a visible order (follow / stay / stand / tp) after a short human-ish beat, so members don't all snap into
	 * motion the same instant a line is typed. Skipped if the member despawned or died while the beat elapsed.
	 */
	private void afterHumanDelay(Member state, Runnable action)
	{
		ThreadPool.schedule(() ->
		{
			if (_members.containsKey(state.npc.getObjectId()) && !state.npc.isDead())
			{
				action.run();
			}
		}, ORDER_DELAY_MIN + Rnd.get((int) (ORDER_DELAY_MAX - ORDER_DELAY_MIN)));
	}

	/**
	 * A member speaks up on its own (state call, reaction, small talk) - throttled per member AND per party so
	 * eight bots can never turn party chat into a firehose. Phrased by the brain when it is up (PARTYEVENT mode),
	 * the canned {@code fallback} otherwise.
	 * @return {@code true} if the line was actually spoken (not eaten by a throttle)
	 */
	private boolean bark(Member state, String situation, String fallback)
	{
		final long now = System.currentTimeMillis();
		final PartyMood mood = mood(state.owner);
		if (((now - state.lastBarkAt) < BARK_MEMBER_COOLDOWN) || ((mood != null) && ((now - mood.lastBarkAt) < BARK_PARTY_COOLDOWN)))
		{
			return false;
		}
		state.lastBarkAt = now;
		if (mood != null)
		{
			mood.lastBarkAt = now;
		}
		speakEvent(state, false, situation, fallback);
		return true;
	}

	/**
	 * An unthrottled bark for the rare celebration moments (leader level-up, raid kill) where two or three members
	 * reacting in quick succession is exactly the point. Still records the throttle stamps so ordinary barks back off.
	 */
	private void cheer(Member state, String situation, String fallback)
	{
		final long now = System.currentTimeMillis();
		state.lastBarkAt = now;
		final PartyMood mood = mood(state.owner);
		if (mood != null)
		{
			mood.lastBarkAt = now;
		}
		speakEvent(state, false, situation, fallback);
	}

	/** Broadcasts a social action (wave / victory / bow / applaud / dance) from a member after {@code delay} ms. */
	private void emote(Member state, int actionId, long delay)
	{
		final int memberId = state.npc.getObjectId();
		ThreadPool.schedule(() ->
		{
			final Member current = _members.get(memberId);
			if ((current == null) || current.npc.isDead() || current.npc.isSitting())
			{
				return;
			}
			current.npc.broadcastPacket(new SocialAction(current.npc.getObjectId(), actionId));
		}, delay);
	}

	/** This owner's party mood, or {@code null} if the mood tick hasn't seen the party yet. */
	private PartyMood mood(Player owner)
	{
		return (owner == null) ? null : _moods.get(owner.getObjectId());
	}

	/** A random living partied member of {@code owner}'s party matching {@code filter}, or {@code null}. */
	private Member pickPartiedMember(Player owner, Predicate<Member> filter)
	{
		final List<Member> matches = new ArrayList<>();
		for (Member m : _members.values())
		{
			if (m.partied && (m.owner == owner) && filter.test(m))
			{
				matches.add(m);
			}
		}
		return matches.isEmpty() ? null : matches.get(Rnd.get(matches.size()));
	}

	/**
	 * Per-party social pass, run once per tick: congratulate a leader level-up, react to the leader going down,
	 * celebrate a raid kill (barks + victory emotes), and drop a line of downtime small talk on a slow cadence.
	 * All event detection is by state transition (level/death/raid-liveness deltas), so nothing here needs hooks
	 * in stock core code.
	 */
	private void partyMoodTick(long now)
	{
		// Gather each owner's partied members once.
		final Map<Integer, List<Member>> parties = new HashMap<>();
		final Map<Integer, Player> owners = new HashMap<>();
		for (Member m : _members.values())
		{
			if (m.partied && (m.owner != null) && m.owner.isOnline() && isPartiedWith(m))
			{
				parties.computeIfAbsent(m.owner.getObjectId(), k -> new ArrayList<>()).add(m);
				owners.putIfAbsent(m.owner.getObjectId(), m.owner);
			}
		}
		_moods.keySet().retainAll(parties.keySet()); // forget parties that no longer exist
		_camps.keySet().retainAll(parties.keySet()); // ...and their camps
		for (Map.Entry<Integer, List<Member>> entry : parties.entrySet())
		{
			final Player owner = owners.get(entry.getKey());
			final List<Member> members = entry.getValue();
			final PartyMood mood = _moods.computeIfAbsent(entry.getKey(), k ->
			{
				final PartyMood created = new PartyMood();
				created.leaderLevel = owner.getLevel();
				created.leaderDead = owner.isDead();
				created.nextSmallTalkAt = now + SMALLTALK_MIN_MS + Rnd.get((int) (SMALLTALK_MAX_MS - SMALLTALK_MIN_MS));
				return created;
			});

			// Leader levelled up: one or two members say gz, one applauds.
			final int level = owner.getLevel();
			if (level > mood.leaderLevel)
			{
				mood.leaderLevel = level;
				congratulate(owner, members, level);
			}
			else if (level < mood.leaderLevel)
			{
				mood.leaderLevel = level; // deleveled (death) - update silently, nobody congratulates that
			}

			// Leader went down: one member reacts once - a res-capable member if there is one ("rezzing you").
			if (owner.isDead() && !mood.leaderDead)
			{
				mood.leaderDead = true;
				final Member rezzer = pickFrom(members, m -> !m.npc.isDead() && (res(m) != null));
				final Member reactor = (rezzer != null) ? rezzer : pickFrom(members, m -> !m.npc.isDead());
				if (reactor != null)
				{
					if (reactor == rezzer)
					{
						cheer(reactor, "The party leader " + owner.getName() + " just died in combat. You can resurrect - tell them in one very short line you'll res them.", "omg - got you, rezzing");
					}
					else
					{
						cheer(reactor, "The party leader " + owner.getName() + " just died in combat. React in one very short line.", "omg");
					}
				}
			}
			else if (!owner.isDead())
			{
				mood.leaderDead = false;
			}

			// Raid watch: remember the raid the party is fighting; when it dies, celebrate. If the fight just ends
			// (reset / party fled / boss despawned), forget it quietly.
			if (mood.engagedRaid == null)
			{
				mood.engagedRaid = engagedRaid(members.get(0));
			}
			else if (mood.engagedRaid.isDead())
			{
				final Monster fallen = mood.engagedRaid;
				mood.engagedRaid = null;
				celebrate(members, fallen);
			}
			else if (!mood.engagedRaid.isInCombat() || (World.getInstance().findObject(mood.engagedRaid.getObjectId()) == null))
			{
				mood.engagedRaid = null;
			}

			// Wipe call: raid still up, tank down, bodies piling - a real party calls the wipe instead of feeding
			// corpses in silence. One call per fight; the actual retreat stays the player's decision (a "tp <town>"
			// order pulls the bots out). Auto-retreat is Bucket 4 material.
			if (mood.engagedRaid == null)
			{
				mood.wipeCalled = false;
			}
			else if (!mood.wipeCalled)
			{
				int down = owner.isDead() ? 1 : 0;
				for (Member m : members)
				{
					if (m.npc.isDead())
					{
						down++;
					}
				}
				final boolean tankDown = pickFrom(members, m -> (m.role == PartyRole.TANK) && !m.npc.isDead()) == null;
				if (tankDown && (down >= 3))
				{
					mood.wipeCalled = true;
					final Member caller = pickFrom(members, m -> !m.npc.isDead());
					if (caller != null)
					{
						cheer(caller, "Your raid attempt is collapsing - the tank is dead and " + down + " of the party are down. Call the wipe in one very short line: tell everyone to pull out.", "this is lost, pull out!");
					}
				}
			}

			// Downtime small talk: one random member, slow cadence, only when nothing is happening. The timer
			// always re-arms, so a busy party just skips the beat rather than queueing chatter for later.
			if (now >= mood.nextSmallTalkAt)
			{
				mood.nextSmallTalkAt = now + SMALLTALK_MIN_MS + Rnd.get((int) (SMALLTALK_MAX_MS - SMALLTALK_MIN_MS));
				if ((mood.engagedRaid == null) && !owner.isInCombat() && !owner.isDead())
				{
					final Member talker = pickFrom(members, m -> !m.npc.isDead() && !m.npc.isInCombat());
					if (talker != null)
					{
						bark(talker, "You're hunting with " + owner.getName() + "'s party and there's a quiet moment between pulls. Say one short casual line of party small talk (the spot, the xp, your class, nothing much). Don't ask a question, don't say goodbye.", //
							SMALL_TALK[Rnd.get(SMALL_TALK.length)]);
					}
				}
			}
		}
	}

	/** One or two members congratulate the leader's level-up; the first also applauds. */
	private void congratulate(Player owner, List<Member> members, int level)
	{
		final List<Member> pool = new ArrayList<>(members);
		final int want = 1 + Rnd.get(2);
		int said = 0;
		while (!pool.isEmpty() && (said < want))
		{
			final Member m = pool.remove(Rnd.get(pool.size()));
			if (m.npc.isDead())
			{
				continue;
			}
			cheer(m, "The party leader " + owner.getName() + " just hit level " + level + ". Congratulate them in one very short line.", Rnd.nextBoolean() ? "gz!" : "grats");
			if (said == 0)
			{
				emote(m, SOCIAL_APPLAUD, 600 + Rnd.get(900));
			}
			said++;
		}
	}

	/** The whole party celebrates a raid kill: everyone emotes (staggered), one or two members say gg. */
	private void celebrate(List<Member> members, Monster boss)
	{
		dbg("VICTORY raid '" + boss.getName() + "' is dead - party celebrates");
		final int[] socials =
		{
			SOCIAL_VICTORY,
			SOCIAL_APPLAUD,
			SOCIAL_DANCE
		};
		int talkers = 0;
		for (Member m : members)
		{
			if (m.npc.isDead())
			{
				continue;
			}
			emote(m, socials[Rnd.get(socials.length)], 800 + Rnd.get(3000));
			if ((talkers < 2) && (Rnd.get(100) < 60))
			{
				talkers++;
				cheer(m, "Your party just killed the raid boss " + boss.getName() + ". Celebrate in one very short line.", Rnd.nextBoolean() ? "gg!" : "nice, gj all");
			}
		}
	}

	/** A random member of {@code members} matching {@code filter}, or {@code null}. */
	private static Member pickFrom(List<Member> members, Predicate<Member> filter)
	{
		final List<Member> matches = new ArrayList<>();
		for (Member m : members)
		{
			if (filter.test(m))
			{
				matches.add(m);
			}
		}
		return matches.isEmpty() ? null : matches.get(Rnd.get(matches.size()));
	}

	// ===== Helpers =====

	private List<Skill> buffs(Member state)
	{
		if (state.buffs == null)
		{
			final List<Skill> list = new ArrayList<>();
			for (Skill skill : state.npc.getAllSkills())
			{
				if (skill.isPassive() || skill.isToggle() || !skill.isActive() || skill.isDebuff())
				{
					continue;
				}
				// Skip anything that heals - a continuous group heal/HoT (e.g. the Elder's) otherwise leaks into the
				// buff list and gets recast on cooldown "for no reason", looking like a constant group-heal spam.
				if (skill.isContinuous() && (skill.getEffectPoint() >= 0) && !isHealId(skill.getId()) && !skill.hasEffectType(EffectType.HEAL, EffectType.CPHEAL, EffectType.MANAHEAL_PERCENT))
				{
					list.add(skill);
				}
			}
			state.buffs = list;
		}
		return state.buffs;
	}

	private List<Skill> resolveKit(Member state, int[] ids)
	{
		final List<Skill> list = new ArrayList<>();
		for (int id : ids)
		{
			final Skill known = state.npc.getKnownSkill(id);
			if (known != null)
			{
				list.add(known);
			}
		}
		return list;
	}

	/** Known fast (2s-cast) heals, best first (resolved once). */
	private List<Skill> fastHealKit(Member state)
	{
		if (!state.fastHealLookedUp)
		{
			state.fastHealLookedUp = true;
			state.fastHealKit = resolveKit(state, FAST_HEAL_PRIORITY);
		}
		return state.fastHealKit;
	}

	/** Known big heals for non-critical gaps, best first (resolved once). */
	private List<Skill> strongHealKit(Member state)
	{
		if (!state.strongHealLookedUp)
		{
			state.strongHealLookedUp = true;
			state.strongHealKit = resolveKit(state, STRONG_HEAL_PRIORITY);
		}
		return state.strongHealKit;
	}

	/** The first heal in {@code kit} the support can actually cast right now - not disabled, MP covered, reagent in stock. */
	private Skill firstCastable(Member state, List<Skill> kit)
	{
		final Player npc = state.npc;
		for (Skill s : kit)
		{
			if (!npc.isSkillDisabled(s) && (npc.getCurrentMp() >= s.getMpConsume()) && PhantomBuffs.canAffordReagent(npc, s))
			{
				return s;
			}
		}
		return null;
	}

	/**
	 * The best FAST heal the support can cast right now (Greater Battle Heal, then Battle Heal) - the reactive
	 * workhorse for emergencies and combat spikes. Speed over power: never a slow (5s) heal here.
	 */
	private Skill emergencyHeal(Member state)
	{
		return firstCastable(state, fastHealKit(state));
	}

	/**
	 * Any heal the support can actually cast this moment, cheapest first - the low-MP fallback so healing never stops
	 * while some heal remains affordable. A nearly-OOM healer that can no longer pay for Greater/Battle Heal still
	 * casts plain Heal (or a strong heal if that is somehow all it has). Tries: cheap Heal, then a fast heal, then a
	 * strong heal.
	 */
	private Skill anyAffordableHeal(Member state)
	{
		final Player npc = state.npc;
		final Skill light = lightHeal(state); // Heal (1011) then Battle Heal - the cheapest per cast
		if ((light != null) && !npc.isSkillDisabled(light) && (npc.getCurrentMp() >= light.getMpConsume()) && PhantomBuffs.canAffordReagent(npc, light))
		{
			return light;
		}
		final Skill fast = firstCastable(state, fastHealKit(state));
		if (fast != null)
		{
			return fast;
		}
		return firstCastable(state, strongHealKit(state));
	}

	/**
	 * The best STRONG heal for a big, non-critical gap where there's time to cast: Greater Battle Heal, then Major
	 * Heal (MP-cheap; also the low-MP fallback), then Greater Heal. Falls back to a fast heal if it knows no big one.
	 */
	private Skill strongHeal(Member state)
	{
		final Skill s = firstCastable(state, strongHealKit(state));
		return (s != null) ? s : emergencyHeal(state);
	}

	/** The cheapest heal this support knows (for small top-offs); falls back to a fast heal if it knows no cheap one. */
	private Skill lightHeal(Member state)
	{
		if (!state.lightHealLookedUp)
		{
			state.lightHealLookedUp = true;
			for (int id : LIGHT_HEAL_PRIORITY)
			{
				final Skill known = state.npc.getKnownSkill(id);
				if (known != null)
				{
					state.lightHeal = known;
					break;
				}
			}
			if (state.lightHeal == null)
			{
				final List<Skill> kit = fastHealKit(state); // no cheap heal known - fall back to a fast heal it has
				state.lightHeal = kit.isEmpty() ? null : kit.get(kit.size() - 1);
			}
		}
		return state.lightHeal;
	}

	/** All party/group heals this support knows, strongest first (resolved once). */
	private List<Skill> groupHealKit(Member state)
	{
		if (!state.groupHealLookedUp)
		{
			state.groupHealLookedUp = true;
			final List<Skill> list = new ArrayList<>();
			for (int id : GROUP_HEAL_PRIORITY)
			{
				final Skill known = state.npc.getKnownSkill(id);
				if (known != null)
				{
					list.add(known);
				}
			}
			state.groupHealKit = list;
		}
		return state.groupHealKit;
	}

	/** The strongest party/group heal the support can afford (MP + reagent) and cast right now, or {@code null}. */
	private Skill groupHeal(Member state)
	{
		final Player npc = state.npc;
		for (Skill s : groupHealKit(state))
		{
			if (!npc.isSkillDisabled(s) && (npc.getCurrentMp() >= s.getMpConsume()) && PhantomBuffs.canAffordReagent(npc, s))
			{
				return s;
			}
		}
		return null;
	}

	/** How many living party members in support range are hurt below {@code threshold} (drives the group-heal decision). */
	private int countHurtBelow(Member state, int threshold)
	{
		final Player npc = state.npc;
		final Party party = state.owner.getParty();
		if (party == null)
		{
			return (!state.owner.isDead() && (state.owner.getCurrentHpPercent() < threshold)) ? 1 : 0;
		}
		int hurt = 0;
		for (Player member : party.getMembers())
		{
			if (!member.isDead() && (npc.calculateDistance2D(member) <= SUPPORT_RANGE) && (member.getCurrentHpPercent() < threshold))
			{
				hurt++;
			}
		}
		return hurt;
	}

	/**
	 * The heal to use on {@code target}: a FAST heal for a critically low target (speed saves it), the strong
	 * MP-efficient heal for a big but non-critical gap (there's time), else the cheap heal for a small top-off.
	 * Saves the expensive heal's MP for when it actually matters.
	 */
	private Skill chooseHeal(Member state, Player target)
	{
		if (target.getCurrentHpPercent() <= CRITICAL_HEAL_PERCENT)
		{
			return emergencyHeal(state); // dying - fast heal, not a slow big one
		}
		if ((100 - target.getCurrentHpPercent()) >= BIG_HEAL_DEFICIT)
		{
			return strongHeal(state); // big non-critical gap - the MP-smart strong heal
		}
		final Skill light = lightHeal(state);
		return (light != null) ? light : emergencyHeal(state);
	}

	private Skill res(Member state)
	{
		// Rez capability is data-driven: any member whose class naturally learned Resurrection (1016) can raise the
		// fallen - not only the HEALER role. In this datapack that is the Cleric/Oracle line (HEALER) AND the Prophet
		// line (BUFFER, which inherits Cleric's Resurrection); both run the support tick, so both now rez. A class
		// that never learned it (a pure mage nuker, an Orc buffer) simply resolves to null and never enters the loop.
		if (!state.resLookedUp)
		{
			state.res = state.npc.getKnownSkill(RES_SKILL_ID);
			state.resLookedUp = true;
		}
		return state.res;
	}

	private static boolean isHealId(int id)
	{
		for (int[] kit : new int[][]
		{
			FAST_HEAL_PRIORITY,
			STRONG_HEAL_PRIORITY,
			GROUP_HEAL_PRIORITY,
			LIGHT_HEAL_PRIORITY
		})
		{
			for (int healId : kit)
			{
				if (healId == id)
				{
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * One-shot nudge back onto the leader: start moving to this member's formation slot now (no start-up jitter -
	 * this is called on events like joining, a res, or an explicit "follow" order, where reacting promptly is
	 * right); the per-tick {@link #driveFollow} keeps it tracking from there.
	 */
	private void ensureFollow(Member state)
	{
		final Player npc = state.npc;
		if (!state.following || (state.owner == null) || npc.isDead())
		{
			return;
		}
		final Location spot = formationSpot(state, state.owner);
		if (npc.calculateDistance2D(spot) <= FORMATION_TOLERANCE)
		{
			return; // already in position
		}
		npc.setRunning();
		npc.getAI().setIntention(Intention.MOVE_TO, spot);
		state.lastDestX = spot.getX();
		state.lastDestY = spot.getY();
	}

	private static void standIfSitting(Player npc)
	{
		if (npc.isSitting())
		{
			npc.standUp();
		}
	}

	/**
	 * Gate a cast behind standing up. standUp() is a ~2.5s animation and a resting member is paralyzed while
	 * seated, so a spell fired in the same tick would silently fail (checkUseMagicConditions rejects paralysis).
	 * @return {@code true} if the member is already standing and may cast now; {@code false} if it just began
	 *         getting up - the caller must return and retry next tick (without consuming any one-shot order) so the
	 *         action lands once it is on its feet.
	 */
	private static boolean readyToCast(Player npc)
	{
		if (npc.isSitting())
		{
			npc.standUp();
			return false;
		}
		return true;
	}

	private boolean isPartiedWith(Member state)
	{
		final Player owner = state.owner;
		return (owner != null) && state.npc.isInParty() && owner.isInParty() && (state.npc.getParty() == owner.getParty());
	}

	private static boolean isMonsterNear(Player npc)
	{
		for (Monster monster : World.getInstance().getVisibleObjectsInRange(npc, Monster.class, DANGER_RANGE))
		{
			if (!monster.isDead())
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * @return {@code true} if a live monster is actually engaged on {@code npc} (targeting it), i.e. something is
	 *         coming at the support itself. Unlike {@link #isMonsterNear(Player)} (any mob in the area), this stays
	 *         {@code false} while the party simply farms nearby, so a caster can sit to recover MP between casts and
	 *         only stands when a mob turns on it.
	 */
	private static boolean underAttack(Player npc)
	{
		for (Monster monster : World.getInstance().getVisibleObjectsInRange(npc, Monster.class, DANGER_RANGE))
		{
			if (!monster.isDead() && (monster.getTarget() == npc))
			{
				return true;
			}
		}
		return false;
	}

	/** Releases a member from its party and despawns it. */
	private void release(Member state, boolean graceful)
	{
		final Player npc = state.npc;
		restoreAutoSkills(state); // a released friend resumes ambient life with its full AutoUse kit back
		// If this member was its camp's puller, clear the role so no one keeps a dangling pull run pointed at it.
		final Camp camp = (state.owner == null) ? null : _camps.get(state.owner.getObjectId());
		if ((camp != null) && (camp.pullerId == npc.getObjectId()))
		{
			camp.pullerId = 0;
			camp.pulling = null;
		}
		_members.remove(npc.getObjectId());
		try
		{
			if (npc.isInParty())
			{
				npc.getParty().removePartyMember(npc, PartyMessageType.LEFT);
			}
		}
		catch (Exception e)
		{
			// best effort
		}
		PhantomManager.getInstance().despawnRecruit(npc);
	}

	private static boolean containsAny(String text, String... needles)
	{
		for (String needle : needles)
		{
			if (text.contains(needle))
			{
				return true;
			}
		}
		return false;
	}

	public static PhantomPartyManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final PhantomPartyManager INSTANCE = new PhantomPartyManager();
	}
}
