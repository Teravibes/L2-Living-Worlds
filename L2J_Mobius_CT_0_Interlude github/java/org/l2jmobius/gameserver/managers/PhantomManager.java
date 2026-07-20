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

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.logging.Logger;

import org.w3c.dom.Document;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.config.custom.AutoPlayConfig;
import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.PlayerTemplateData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.appearance.PlayerAppearance;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.actor.holders.player.AutoPlaySettingsHolder;
import org.l2jmobius.gameserver.model.actor.holders.player.AutoUseSettingsHolder;
import org.l2jmobius.gameserver.model.actor.holders.player.ClassType;
import org.l2jmobius.gameserver.model.actor.instance.Monster;
import org.l2jmobius.gameserver.model.actor.templates.PlayerTemplate;
import org.l2jmobius.gameserver.model.item.Armor;
import org.l2jmobius.gameserver.model.item.EtcItem;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.Weapon;
import org.l2jmobius.gameserver.model.item.enums.BodyPart;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.item.type.ArmorType;
import org.l2jmobius.gameserver.model.item.type.CrystalType;
import org.l2jmobius.gameserver.model.item.type.EtcItemType;
import org.l2jmobius.gameserver.model.item.type.WeaponType;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.targets.TargetType;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.L2Friend;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.taskmanagers.AutoPlayTaskManager;
import org.l2jmobius.gameserver.taskmanagers.AutoUseTaskManager;

/**
 * Real-Player phantom system.
 * <p>
 * A phantom is a genuine {@link Player} database object with no client attached - the same
 * clientless-Player pattern Mobius ships for offline traders / offline-play
 * ({@code OfflineTraderTable}, {@code OfflinePlayTable}). That gives it real skills, inventory, stats,
 * leveling and PvP eligibility an NPC cannot have.
 * <p>
 * <b>Combat is delegated to the engine's native auto-hunt</b> ({@link AutoPlayTaskManager} +
 * {@link AutoUseTaskManager}) - the exact systems {@code OfflinePlayTable.restoreOfflinePlayers}
 * uses to make a clientless player farm. They handle target selection, movement (geodata
 * pathfinding), attacking, skills, buffs, soulshots and potions. This manager owns the <b>macro</b>
 * layer: data-driven deployment from {@code data/PhantomPopulations.xml}, gearing, supervision and
 * death-respawn.
 */
public class PhantomManager implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(PhantomManager.class.getName());

	private static final String ACCOUNT_NAME = "phantom";
	// Regulars persist across restarts (stable charId) so a future friend tier can reference them by id; they
	// live under this distinct account so the boot sweep (which only targets ACCOUNT_NAME) never touches them
	// and despawn() knows to keep their row instead of deleting it.
	private static final String ACCOUNT_NAME_REGULAR = "phantom_regular";
	// Short grace after a player's EnterWorld before we login-spawn their befriended regulars, so login itself
	// finishes first and the spawn work runs off the login (packet) thread.
	private static final long FRIEND_SPAWN_DELAY = 3000;
	// Supervisor cadence: cleanup gone phantoms and revive/respawn dead ones. Combat runs on AutoPlay's
	// own 700ms loop, so this can be lazy.
	private static final long SUPERVISE_INTERVAL = 5000;
	// How often phantoms drop a monster another phantom or a real player is already fighting, so they
	// spread over the mobs instead of ganging one. Fast tick - this should feel responsive.
	private static final long DECONFLICT_INTERVAL = 1000;
	// How long a dead phantom stays down before it is replaced (population) or revived (ad-hoc).
	private static final long RESPAWN_DELAY = 15000;
	// On-demand activation: a population's phantoms exist only while a real player is near its anchor, and
	// despawn a short while after the last one leaves. Keeps memory/DB free for empty zones and rolls a
	// fresh crowd on every visit. ACTIVATION_MARGIN is added to the group radius to decide "near"; the
	// grace delay avoids spawn/despawn thrash when a player skirts the edge; the stagger spreads the spawn
	// cost (DB insert + level + gear per phantom) over a few ticks so entering a zone does not hitch.
	private static final int ACTIVATION_MARGIN = 2000;
	private static final long DEACTIVATE_DELAY = 30000;
	private static final long SPAWN_STAGGER = 250;
	// AutoPlay "next target" modes (mirrors the client toggle): 1 = monsters only.
	private static final int TARGET_MODE_MONSTER = 1;
	// AutoPlay auto-action id for the basic melee attack. Without it, AutoPlay treats the character as a
	// mage caster that never auto-hits (see AutoPlayTaskManager.isMageCaster).
	private static final int AUTO_ATTACK_ACTION = 2;
	// How many soulshots to hand a freshly geared phantom (no runtime restock yet).
	private static final int SHOT_COUNT = 5000;
	// Arrows handed to an archer so its bow can actually fire (a bow with no ammunition does nothing). Big stack
	// so a farm session doesn't run dry; the engine auto-equips them into the left hand on the first shot.
	private static final int ARROW_COUNT = 20000;
	// Recruited party members gear up for real content (unlike the cheap, intentionally-patchy ambient loadout):
	// a chance the member is an enchanted player, and if so a modest uniform enchant on its weapon + armor.
	private static final int PARTY_ENCHANT_CHANCE = 65; // percent of recruited members that come enchanted
	private static final int PARTY_ENCHANT_MIN = 3;
	private static final int PARTY_ENCHANT_MAX = 6;
	// Healing potions for in-combat HP sustain while farming. Generous stack since phantoms fight a lot;
	// refreshed on every (re)spawn. Native auto-potion drinks one when HP falls below the percent.
	private static final int HP_POTION_ID = 1539; // Greater Healing Potion
	private static final int HP_POTION_COUNT = 20000;
	private static final int HP_POTION_PERCENT = 60;
	// Buff reagents: a few support buffs consume an item per cast (the prophet's Greater Might / Greater Shield
	// and the caster's Clarity all eat Spirit Ore, id 3031). A clientless buffer that has none silently fails the
	// cast - the engine rejects it in checkDoCastConditions - and, since the buff never lands, re-tries it every
	// tick forever (the "high-level buffer stuck buffing in a loop"; a low-level one doesn't know those buffs, so
	// it never hit it). We hand every support phantom a large stack of whatever reagents its known buffs require,
	// so the greater buffs actually land and upkeep completes. Weight is a non-issue: diet mode zeroes it.
	private static final int BUFF_REAGENT_COUNT = 20000;
	// Heal skill ids a buddy may already know (Heal, Battle Heal, Greater Heal, Greater Battle Heal). If it
	// knows none (Prophet/Warcryer), it is granted Heal (1011) so every buddy can top its owner up.
	private static final int[] HEAL_SKILL_IDS =
	{
		1011,
		1015,
		1217,
		1218
	};
	// Minimum spacing between phantoms at spawn, so a group does not stack on one tile.
	private static final int MIN_SEPARATION = 250;
	// First-occupation FIGHTER class ids per race (verified in FakePlayerAppearanceFactory), weighted by
	// repetition so phantoms vary in race/body type. All are melee, so the sword + soulshot path is shared.
	private static final int[] FIGHTER_CLASS_POOL =
	{
		0, 0, 0, // Human Fighter
		18, 18, // Elven Fighter
		31, 31, // Dark Fighter
		44, // Orc Fighter
		53 // Dwarven Fighter
	};
	// First-occupation MYSTIC (DD) base classes; orcs/dwarves excluded (no nuker line).
	private static final int[] MAGE_CLASS_POOL =
	{
		10, 10, // Human Mage
		25, // Elven Mystic
		38 // Dark Mystic
	};
	// How many (cheapest) candidate items to keep per slot/grade, so phantoms vary their look without
	// pulling in rare/expensive drops.
	private static final int CANDIDATES_PER_SLOT = 6;
	// Safety ceiling on total live phantom Player objects (each is far heavier than an NPC fake player).
	private static final int MAX_PHANTOMS = 200;
	// Proximity dormancy: a phantom only runs the (costly) auto-hunt while a real, client-connected player
	// is near. Hysteresis (wake closer than sleep) stops it flapping at the boundary. Ideal for solo play:
	// only the handful of phantoms around you actually compute.
	private static final int WAKE_RANGE = 3500;
	private static final int SLEEP_RANGE = 4500;
	// Resting: when safe (no mob near, not in combat) and low on HP/MP, a phantom sits to regen, then
	// stands when recovered or threatened. This is what sustains mages (MP) and wounded fighters (HP).
	// HP uses a moderate band; MP (a caster's lifeblood) sits later but recovers all the way to full so a
	// nuker gets a proper refill instead of standing back up half-charged.
	private static final int REST_SIT_PERCENT = 35;
	private static final int REST_STAND_PERCENT = 85;
	private static final int REST_MP_SIT_PERCENT = 30; // sit once MP drops to ~this
	private static final int REST_MP_STAND_PERCENT = 100; // and stay seated until MP is fully restored
	private static final int REST_DANGER_RANGE = 700;
	// Share of phantoms that roll a (DD) mage instead of a fighter.
	private static final int MAGE_CHANCE = 30;
	// Default chance a spawn uses one of a population's fixed "regular" identities (name/appearance) instead
	// of a fresh random one, giving that zone a few recognizable recurring faces. Only applies when the
	// population actually defines <regular> entries; overridable per-population via the regularChance attribute.
	private static final int REGULAR_CHANCE_DEFAULT = 25;
	// Safety ceiling on auto-generated regulars per population (regularCount), so a stray config can't flood one.
	private static final int MAX_AUTO_REGULARS = 30;
	// Mage combat: casters don't move under AutoPlay, so a faster tick positions them. They hold at
	// CAST_RANGE to nuke; when MP drops below CAST_MP_PERCENT they melee the target until MP recovers
	// (a pure caster is otherwise passive with no MP). TOLERANCE stops constant micro-repositioning.
	private static final long MAGE_TICK_INTERVAL = 1000;
	private static final int MAGE_CAST_RANGE = 650;
	private static final int MAGE_RANGE_TOLERANCE = 150;
	private static final int MAGE_CAST_MP_PERCENT = 20;
	// Idle roaming: the native AutoPlay never moves a phantom past its search radius, so once no monster is
	// in range it stands still forever. When an awake phantom has nothing to fight, the supervisor walks it
	// to a fresh spot inside its home area so the next AutoPlay scan finds new mobs - this is what keeps a
	// spread-out zone active instead of phantoms freezing wherever the local mobs ran out.
	private static final int ROAM_RADIUS = 1200;
	private static final int ROAM_MIN_DISTANCE = 400;
	private static final int ROAM_ATTEMPTS = 10;
	private static final int IDLE_TARGET_CLEAR_TICKS = 3;
	// Post-kill breather: after a phantom kills its mob it stands a beat before pulling the next one, instead
	// of machine-gunning straight through the spawn. (Lost targets without a kill re-acquire immediately.)
	private static final long POST_KILL_DELAY_MIN = 1200;
	private static final long POST_KILL_DELAY_MAX = 3200;
	// Initial dispersal: when a population activates (a real player approaches), phantoms first fan out toward
	// the perimeter of their area for this long before hunting, so they spread instead of all piling onto the
	// nearest mobs the instant they spawn. DISPERSE_MIN_RADIUS_PCT is the closest-to-edge they aim for.
	private static final long DISPERSE_DURATION = 4500;
	private static final double DISPERSE_MIN_RADIUS_PCT = 0.55;
	// Caster looting: after a kill a mage walks to the corpse so the auto-pickup (which it can't reach from
	// cast range) grabs the drop; the walk ends on arrival or after the cap.
	private static final int LOOT_WALK_RANGE = 180;
	private static final long LOOT_WALK_MAX = 6000;

	// Body slots a phantom is geared in. R_HAND = sword; the rest are LIGHT/HEAVY armor pieces.
	private static final BodyPart[] GEAR_SLOTS =
	{
		BodyPart.R_HAND,
		BodyPart.CHEST,
		BodyPart.LEGS,
		BodyPart.GLOVES,
		BodyPart.FEET,
		BodyPart.HEAD
	};
	// The cheapest few tradeable items per grade for each slot, resolved once from the datapack, so each
	// phantom can roll a varied but level-appropriate look (data-driven - no hard-coded item ids). Two
	// loadouts: fighters (sword + light/heavy armor) and mages (magic weapon + robe).
	private static final Map<BodyPart, EnumMap<CrystalType, List<ItemTemplate>>> FIGHTER_GEAR = new EnumMap<>(BodyPart.class);
	private static final Map<BodyPart, EnumMap<CrystalType, List<ItemTemplate>>> MAGE_GEAR = new EnumMap<>(BodyPart.class);
	// Caster armor item ids that have NO Orc-Mystic model in the client and so render invisibly on an Orc
	// (Warcryer) - chest/legs vanish. There is no server-side attribute to detect this (identical items render
	// differently), so it is a tested deny-list: an Orc caster is geared from everything EXCEPT these. Add ids
	// here as the debug gear log turns up more offenders. Confirmed so far: 445 Paradia Tunic (chest),
	// 474 Stockings of Mana (legs).
	private static final Set<Integer> ORC_CASTER_ARMOR_SKIP = Set.of(445, 474);
	private static volatile boolean _gearBuilt = false;

	/**
	 * A buddy is a support-class phantom that idles in a town (no auto-hunt) until a real player whispers it,
	 * parties it, and uses it as a personal buffer/healer. Each role fixes the exact support class to spawn.
	 * All three cast (mage gear loadout); Prophet/Warcryer get a basic Heal granted since their Interlude class
	 * has none, so every buddy can top its owner up.
	 */
	enum BuddyRole
	{
		NONE(0),
		ELDER(30), // Elven Elder - mage buffs, heals, recharge
		PROPHET(17), // Prophet - fighter buffs (Heal granted)
		WARCRYER(52); // Warcryer - Orc buffs (Heal granted)

		final int classId;

		BuddyRole(int classId)
		{
			this.classId = classId;
		}

		boolean isBuddy()
		{
			return this != NONE;
		}

		static BuddyRole fromString(String value)
		{
			if ((value == null) || value.isEmpty())
			{
				return NONE;
			}
			switch (value.trim().toUpperCase())
			{
				case "BUDDY_ELDER":
				case "ELDER":
				{
					return ELDER;
				}
				case "BUDDY_PROPHET":
				case "PROPHET":
				{
					return PROPHET;
				}
				case "BUDDY_WARCRYER":
				case "WARCRYER":
				{
					return WARCRYER;
				}
				default:
				{
					return NONE;
				}
			}
		}
	}

	/**
	 * A recruited combat party member. Unlike a {@link BuddyRole} buddy (which idles in a town until partied),
	 * a party member is spawned on demand off-screen when a real player shouts an LFM/LFP, walks to the player,
	 * and is then invited into the party. {@link PhantomPartyManager} owns its behaviour from spawn.
	 * <ul>
	 * <li>{@code mage} - caster gear loadout (magic weapon + robe + spiritshots).</li>
	 * <li>{@code supportAs} - non-NONE roles reuse the proven {@link #outfitBuddy} support outfit (no auto-hunt;
	 * the manager casts heals/buffs by hand). Combat roles use {@link #outfitCombat} (a fixed class + auto-skills).</li>
	 * </ul>
	 * Combat class ids are standard Interlude occupations resolved per level tier; if a template is missing the
	 * spawner falls back to a plain fighter/mage, so a bad id degrades instead of crashing.
	 */
	public enum PartyRole
	{
		TANK(false, BuddyRole.NONE),
		WARRIOR(false, BuddyRole.NONE),
		ARCHER(false, BuddyRole.NONE),
		DAGGER(false, BuddyRole.NONE),
		SINGER(false, BuddyRole.NONE), // Swordsinger: melee that keeps the party's songs running (2-min recast loop)
		DANCER(false, BuddyRole.NONE), // Bladedancer: dual-wield melee that keeps the dances running
		NUKER(true, BuddyRole.NONE),
		HEALER(true, BuddyRole.ELDER), // Elven Elder kit (heals + recharge); granted Resurrection on spawn
		BUFFER(true, BuddyRole.PROPHET); // Prophet fighter-buff kit

		final boolean mage;
		final BuddyRole supportAs;

		PartyRole(boolean mage, BuddyRole supportAs)
		{
			this.mage = mage;
			this.supportAs = supportAs;
		}

		/** @return {@code true} for HEALER/BUFFER: outfit via the support path, behaviour driven by hand. */
		public boolean isSupport()
		{
			return supportAs.isBuddy();
		}

		/**
		 * Resolves the loose tokens a player shouts ("healer", "dd", "box", "tank", "ee") to a role.
		 * @return the matched role, or {@code null} if the word names no role
		 */
		public static PartyRole fromToken(String token)
		{
			switch (token)
			{
				case "tank":
				case "tanker":
				case "knight":
				case "pally":
				case "paladin":
				{
					return TANK;
				}
				case "dd":
				case "dps":
				case "damage":
				case "dealer":
				{
					return randomDps(); // "dd" = any damage dealer, so a party comes out varied
				}
				case "fighter":
				case "warrior":
				case "melee":
				case "glad":
				case "gladiator":
				case "wl":
				case "warlord":
				{
					return WARRIOR;
				}
				case "archer":
				case "bow":
				case "hawkeye":
				case "ranger":
				{
					return ARCHER;
				}
				case "dagger":
				case "rogue":
				case "th":
				case "dwarf":
				case "assassin":
				{
					return DAGGER;
				}
				case "nuker":
				case "mage":
				case "nuke":
				case "sorc":
				case "sorcerer":
				case "wizard":
				case "ss":
				{
					return NUKER;
				}
				case "healer":
				case "heal":
				case "ee":
				case "elder":
				case "bishop":
				case "se":
				case "shillien":
				case "cleric":
				{
					return HEALER;
				}
				case "buffer":
				case "buff":
				case "pp":
				case "prophet":
				case "wc":
				case "warcryer":
				{
					return BUFFER;
				}
				case "sws":
				case "singer":
				case "swordsinger":
				{
					return SINGER;
				}
				case "bd":
				case "dancer":
				case "bladedancer":
				{
					return DANCER;
				}
				default:
				{
					return null;
				}
			}
		}

		/** A random damage-dealer role, so a bare "dd"/"dps" request yields a varied party. */
		private static PartyRole randomDps()
		{
			final PartyRole[] dps =
			{
				WARRIOR,
				ARCHER,
				DAGGER,
				NUKER
			};
			return dps[Rnd.get(dps.length)];
		}
	}

	/** A requested recruit: the behaviour role plus an optional exact class id (0 = the role's default class). */
	public static class Recruit
	{
		public final PartyRole role;
		public final int classId;

		public Recruit(PartyRole role, int classId)
		{
			this.role = role;
			this.classId = classId;
		}
	}

	/**
	 * @return {@code true} if a class is a specific 2nd-or-higher occupation worth requesting by name (e.g.
	 *         Shillien Elder, Gladiator) - base and 1st classes are excluded so generic words ("fighter",
	 *         "knight", "mage") fall through to the level-appropriate role tokens instead.
	 */
	public static boolean isSelectableClass(PlayerClass playerClass)
	{
		int depth = 0;
		PlayerClass parent = playerClass.getParent();
		while (parent != null)
		{
			depth++;
			parent = parent.getParent();
		}
		return depth >= 2;
	}

	/** Maps a specific class to the behaviour role used for its gear/weapon/support handling. */
	public static PartyRole roleForClass(PlayerClass playerClass)
	{
		final String name = playerClass.name().toLowerCase();
		if (nameHas(name, "paladin", "dark_avenger", "darkavenger", "temple_knight", "templeknight", "shillien_knight", "shillienknight", "hell_knight", "hellknight", "phoenix_knight", "phoenixknight", "evas_templar", "eva_templar", "shillien_templar", "knight"))
		{
			return PartyRole.TANK;
		}
		if (nameHas(name, "cleric", "bishop", "oracle", "elder", "cardinal", "saint"))
		{
			return PartyRole.HEALER;
		}
		if (nameHas(name, "singer", "muse")) // Swordsinger / Sword Muse
		{
			return PartyRole.SINGER;
		}
		if (nameHas(name, "dancer")) // Bladedancer / Spectral Dancer
		{
			return PartyRole.DANCER;
		}
		if (nameHas(name, "prophet", "warcryer", "doomcryer", "overlord", "dominator", "shaman", "hierophant"))
		{
			return PartyRole.BUFFER;
		}
		if (playerClass.isMage())
		{
			return PartyRole.NUKER;
		}
		if (nameHas(name, "ranger", "hawkeye", "sentinel", "sagittarius", "scout", "archer"))
		{
			return PartyRole.ARCHER;
		}
		if (nameHas(name, "hunter", "walker", "adventurer", "rider", "assassin", "rogue", "scavenger", "seeker"))
		{
			return PartyRole.DAGGER;
		}
		return PartyRole.WARRIOR;
	}

	private static boolean nameHas(String name, String... keys)
	{
		for (String key : keys)
		{
			if (name.contains(key))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * A fixed "regular" identity for a population: a recurring, recognizable face rather than a fresh random
	 * one each spawn. Name + appearance are stable, so the LLM brain (which hashes the name into a persistent
	 * voice) gives the same personality every time and the player recognizes the character across sessions.
	 * {@code classId <= 0} means "roll a class as usual" (only name/appearance are pinned).
	 */
	private static class Regular
	{
		String name;
		boolean female;
		byte face;
		byte hairColor;
		byte hairStyle;
		int classId; // 0 = use the population's normal class roll; buddies always keep their role class
	}

	/** A deployment group: where/how many phantoms spawn, their level range, and whether to respawn. */
	private static class Population
	{
		String name = "unnamed";
		Location center;
		int radius = 800;
		int count;
		int minLevel = 1;
		int maxLevel = 1;
		boolean respawn = true;
		BuddyRole role = BuddyRole.NONE; // NONE = ordinary field hunter; otherwise an idle support buddy
		final List<Location> polygon = new ArrayList<>(); // optional area; spawn inside it instead of a circle
		final List<Regular> regulars = new ArrayList<>(); // fixed identities that recur in this area (authored + auto)
		int regularCount; // how many stable regulars to auto-generate for this area (0 = none / authored only)
		int regularChance = REGULAR_CHANCE_DEFAULT; // % chance a spawn uses a regular (only if regulars exist)
		boolean active; // phantoms currently spawned (a real player is in range)
		long emptySince; // when the last observer left this area (0 while a player is near)
	}

	private static class PhantomData
	{
		final Player player;
		final Location home;
		final Population population; // null for ad-hoc (admin) spawns
		final boolean mage; // pure caster: needs the mage combat tick to position/kite it
		final BuddyRole role; // NONE for hunters; otherwise this is an idle support buddy (see PhantomBuddyManager)
		long deadSince; // 0 while alive
		boolean dormant; // auto-hunt paused because no real player is near
		boolean resting; // sitting to regenerate HP/MP
		int idleTargetTicks; // stuck with a target but no movement/attack/cast
		boolean dispersing; // initial fan-out before hunting begins
		long disperseUntil; // when dispersal ends and the auto-hunt starts
		long huntPauseUntil; // post-kill breather: paused, standing, until this time (0 = hunting)
		int claimedOid; // object id of the monster this phantom currently owns (0 = none)
		int lastMobX; // last known position of the engaged mob, so a caster can walk to the loot after a kill
		int lastMobY;
		volatile boolean buddyEngaged; // buddy is partied/claimed by a player: skip the proximity despawn (grace)
		volatile boolean recruited; // recruited combat party member: PhantomPartyManager owns it; skip ALL hunter logic
		int friendOwnerId; // objectId of the real player this regular was login-spawned to accompany (0 = not a friend spawn)

		PhantomData(Player player, Location home, Population population, boolean mage, BuddyRole role)
		{
			this.player = player;
			this.home = home;
			this.population = population;
			this.mage = mage;
			this.role = role;
		}
	}

	private final ConcurrentHashMap<Integer, PhantomData> _phantoms = new ConcurrentHashMap<>();
	// CopyOnWriteArrayList: //phantom reload rewrites this from the admin's packet thread while the
	// supervisor iterates it every tick - a plain ArrayList would risk a ConcurrentModificationException.
	// Writes are rare (boot + reload) and the list is tiny, so copy-on-write is the cheap safe choice.
	private final List<Population> _populations = new CopyOnWriteArrayList<>();
	private boolean _supervising = false;
	// Live phantoms promoted to regular THIS session (befriended while spawned under the ephemeral 'phantom'
	// account). Player._accountName is final, so the in-memory instance keeps the old account until it is next
	// reloaded from DB (where the promotion is already written) - this set bridges that gap for isRegular().
	private final Set<Integer> _promoted = ConcurrentHashMap.newKeySet();
	// Cached friend-regular charIds per online real player (ownerObjectId -> regular charIds), so the supervisor
	// can keep them spawned without re-querying the DB every tick. Loaded at login, extended on befriend,
	// dropped at logout.
	private final ConcurrentHashMap<Integer, Set<Integer>> _friendRegularsByOwner = new ConcurrentHashMap<>();
	// Last time the supervisor ran the friend-regular ensure pass (throttled; it self-heals, no need every tick).
	private long _lastFriendEnsure = 0;
	// <friend> creation orders authored in the fpc-editor (PhantomPopulations.xml): materialized once when the
	// owner is online (create + befriend + spawn), then inert - an entry whose name already exists is skipped,
	// so the file can stay in place and never duplicates or resurrects a friend the player later deleted.
	private final List<CraftedFriend> _craftedFriends = new ArrayList<>();

	/** A `<friend>` node from PhantomPopulations.xml: a friend to craft for a player, authored in the fpc-editor. */
	private static class CraftedFriend
	{
		String owner; // the real player's character name
		String name; // the friend's character name (also the "already created" marker)
		String classSpec = ""; // fighter/mage/elder/prophet/warcryer or a class id; empty = random fighter
		int level; // 0 = match the owner's level at creation
		int sex = -1; // 0 male, 1 female, -1 random
		int face = -1; // 0-2, -1 random
		int hairStyle = -1; // 0-2, -1 random
		int hairColor = -1; // 0-3, -1 random
	}

	protected PhantomManager()
	{
		load();
	}

	@Override
	public void load()
	{
		// Phantoms/buddies/party members are real `characters` rows (account_name='phantom') that are only
		// deleted when despawn() runs deliberately (zone-empty timeout, //phantom clear, reload). An unclean
		// shutdown (the usual "just kill the PC" case) never runs despawn(), so every such session leaves its
		// active phantom rows orphaned forever - they bloat the DB, become permanent CharInfoTable RAM residents
		// (the whole characters table is loaded at boot), and make name generation collide more. Sweep them here,
		// once at boot, before anything spawns. load() runs exactly once per JVM (lazy singleton, not touched by
		// //reloadfakeplayers), so this can never delete a live phantom's row.
		sweepOrphanedPhantoms();

		_populations.clear();
		synchronized (_craftedFriends)
		{
			_craftedFriends.clear();
		}
		parseDatapackFile("data/PhantomPopulations.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _populations.size() + " phantom population(s), " + _craftedFriends.size() + " crafted-friend order(s).");
		if (!_populations.isEmpty() || !_craftedFriends.isEmpty())
		{
			// Populations are spawned on demand (when a real player approaches), not at boot - the supervisor
			// drives activation/deactivation, so nothing is created until someone is near. Crafted-friend
			// orders also materialize from the supervisor (when their owner is online).
			startSupervising();
		}
	}

	/**
	 * Re-reads {@code PhantomPopulations.xml} live ({@code //phantom reload}): despawns everything (persistent
	 * regulars keep their rows and are respawned by the friend ensure pass), then re-parses populations and
	 * crafted-friend orders. Unlike {@link #load()} this does NOT run the boot orphan sweeps - phantoms are
	 * live, and the sweeps are only safe before anything has spawned.
	 * @return a short result message for the invoking tooling
	 */
	public String reloadPopulations()
	{
		final int removed = clear();
		_populations.clear();
		synchronized (_craftedFriends)
		{
			_craftedFriends.clear();
		}
		parseDatapackFile("data/PhantomPopulations.xml");
		if (!_populations.isEmpty() || !_craftedFriends.isEmpty())
		{
			startSupervising();
		}
		LOGGER.info(getClass().getSimpleName() + ": Reloaded PhantomPopulations.xml: " + _populations.size() + " population(s), " + _craftedFriends.size() + " crafted-friend order(s); " + removed + " phantom(s) despawned.");
		return "Reloaded: " + _populations.size() + " population(s), " + _craftedFriends.size() + " friend order(s). " + removed + " phantom(s) despawned; zones redeploy on approach, friends rejoin in ~15s. Note: recruited parties/buddies do NOT respawn - re-recruit/re-summon them.";
	}

	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, "population", populationNode ->
		{
			final StatSet set = new StatSet(parseAttributes(populationNode));
			final Population population = new Population();
			population.name = set.getString("name", "unnamed");
			population.center = new Location(set.getInt("x"), set.getInt("y"), set.getInt("z"));
			population.radius = set.getInt("radius", 800);
			population.count = set.getInt("count", 0);
			population.minLevel = set.getInt("minLevel", 1);
			population.maxLevel = set.getInt("maxLevel", population.minLevel);
			population.respawn = set.getBoolean("respawn", true);
			population.role = BuddyRole.fromString(set.getString("role", ""));
			population.regularChance = set.getInt("regularChance", REGULAR_CHANCE_DEFAULT);
			population.regularCount = set.getInt("regularCount", 0);
			forEach(populationNode, "point", pointNode ->
			{
				final StatSet p = new StatSet(parseAttributes(pointNode));
				population.polygon.add(new Location(p.getInt("x"), p.getInt("y"), p.getInt("z", population.center.getZ())));
			});
			forEach(populationNode, "regular", regularNode ->
			{
				final StatSet r = new StatSet(parseAttributes(regularNode));
				final Regular regular = new Regular();
				regular.name = r.getString("name", "").trim();
				if (regular.name.isEmpty())
				{
					LOGGER.warning(getClass().getSimpleName() + ": Skipping a <regular> with no name in population '" + population.name + "'.");
					return;
				}
				regular.female = r.getBoolean("female", false);
				regular.face = (byte) r.getInt("face", 0);
				regular.hairColor = (byte) r.getInt("hairColor", 0);
				regular.hairStyle = (byte) r.getInt("hairStyle", 0);
				regular.classId = r.getInt("classId", 0);
				population.regulars.add(regular);
			});
			generateAutoRegulars(population);
			_populations.add(population);
		}));

		// <friend> creation orders (authored in the fpc-editor's Phantoms > Friends panel): craft a persistent
		// regular to the given spec for the given player, once, the next time that player is online.
		forEach(document, "list", listNode -> forEach(listNode, "friend", friendNode ->
		{
			final StatSet set = new StatSet(parseAttributes(friendNode));
			final CraftedFriend friend = new CraftedFriend();
			friend.owner = set.getString("owner", "").trim();
			friend.name = set.getString("name", "").trim();
			if (friend.owner.isEmpty() || friend.name.isEmpty())
			{
				LOGGER.warning(getClass().getSimpleName() + ": Skipping a <friend> without both owner and name.");
				return;
			}
			friend.classSpec = set.getString("class", "");
			friend.level = set.getInt("level", 0);
			final String sex = set.getString("sex", "").trim().toLowerCase();
			friend.sex = sex.startsWith("f") ? 1 : sex.startsWith("m") ? 0 : -1;
			friend.face = set.getInt("face", -1);
			friend.hairStyle = set.getInt("hairStyle", -1);
			friend.hairColor = set.getInt("hairColor", -1);
			synchronized (_craftedFriends)
			{
				_craftedFriends.add(friend);
			}
			// Logged per order so a typo'd owner (which would otherwise wait forever, silently) is visible.
			LOGGER.info(getClass().getSimpleName() + ": <friend> order queued: '" + friend.name + "' for owner '" + friend.owner + "' (materializes when that character is online).");
		}));
	}

	/**
	 * Activates populations a real player has approached and despawns those everyone has left (after a grace
	 * delay). Called every supervisor tick so phantoms exist only around the player(s).
	 */
	private void updatePopulations(List<Player> observers, long now)
	{
		for (Population population : _populations)
		{
			if (population.count <= 0)
			{
				continue;
			}
			if (isAnyoneNear(population, observers))
			{
				population.emptySince = 0;
				if (!population.active)
				{
					activate(population);
				}
			}
			else if (population.active)
			{
				if (population.emptySince == 0)
				{
					population.emptySince = now;
				}
				else if ((now - population.emptySince) >= DEACTIVATE_DELAY)
				{
					deactivate(population);
				}
			}
		}
	}

	/** @return {@code true} if any real player is within {@link #ACTIVATION_MARGIN} of a population's area. */
	private static boolean isAnyoneNear(Population population, List<Player> observers)
	{
		final long range = (long) population.radius + ACTIVATION_MARGIN;
		final long rangeSq = range * range;
		for (Player observer : observers)
		{
			final long dx = observer.getX() - population.center.getX();
			final long dy = observer.getY() - population.center.getY();
			if (((dx * dx) + (dy * dy)) <= rangeSq)
			{
				return true;
			}
		}
		return false;
	}

	/** Spawns a population's phantoms (staggered to spread the cost) when a player first enters its area. */
	private void activate(Population population)
	{
		population.active = true;
		population.emptySince = 0;
		if (!AutoPlayConfig.ENABLE_AUTO_PLAY)
		{
			LOGGER.warning(getClass().getSimpleName() + ": EnableAutoPlay is false in config/Custom/AutoPlay.ini - phantoms in '" + population.name + "' will not hunt.");
		}
		// A buddy that stayed alive on grace (followed its owner away and came back) is already counted: only
		// top the group back up to its count, so we don't stack a second buddy on the lingering one.
		int alive = 0;
		for (PhantomData data : _phantoms.values())
		{
			if (data.population == population)
			{
				alive++;
			}
		}
		final int toSpawn = population.count - alive;
		if (toSpawn <= 0)
		{
			return;
		}
		LOGGER.info(getClass().getSimpleName() + ": Activating population '" + population.name + "' (" + toSpawn + " phantoms).");
		for (int i = 0; i < toSpawn; i++)
		{
			ThreadPool.schedule(() ->
			{
				// A quick in-and-out could have deactivated it before this staggered spawn fires.
				if (population.active)
				{
					deployOne(population);
				}
			}, i * SPAWN_STAGGER);
		}
	}

	/** Despawns all of a population's phantoms (deleting their DB rows) once everyone has left its area. */
	private void deactivate(Population population)
	{
		population.active = false;
		population.emptySince = 0;
		int removed = 0;
		for (PhantomData data : new ArrayList<>(_phantoms.values()))
		{
			if (data.population == population)
			{
				// A buddy that is partied/claimed by a player gets a grace period: it stays alive even though the
				// town it idled in is now empty (the player may have teleported it out to a hunting zone). The
				// PhantomBuddyManager despawns it when the party disbands, the owner logs off, or grace runs out,
				// and activate()'s top-up counts it so re-entering the town won't stack a second buddy on it. We
				// leave the population deactivated regardless, so we don't re-run this every tick (which spammed
				// the log with "despawned 0").
				if (data.role.isBuddy() && data.buddyEngaged)
				{
					continue;
				}
				despawn(data);
				removed++;
			}
		}
		LOGGER.info(getClass().getSimpleName() + ": Deactivated population '" + population.name + "' (despawned " + removed + ").");
	}

	/** Spawns one phantom into a population: rolls a scattered location and a level in the group's range. */
	private boolean deployOne(Population population)
	{
		final Location location = rollLocation(population);
		if (location == null)
		{
			return false;
		}
		// Rnd.get(origin, bound) is inclusive on both ends.
		final int level = Rnd.get(population.minLevel, population.maxLevel);
		return createAndSpawn(location, level, population) != null;
	}

	/** Picks a geodata-valid, ground-snapped spawn point inside a population's circle or polygon. */
	private Location rollLocation(Population population)
	{
		Location fallback = null;
		for (int attempt = 0; attempt < 15; attempt++)
		{
			final int x;
			final int y;
			if (population.polygon.size() >= 3)
			{
				final Location point = randomPointInPolygon(population);
				x = point.getX();
				y = point.getY();
			}
			else
			{
				// sqrt() spreads points evenly across the disc area instead of bunching them near the center.
				final double angle = Rnd.nextDouble() * 2 * Math.PI;
				final int distance = (int) (population.radius * Math.sqrt(Rnd.nextDouble()));
				x = population.center.getX() + (int) (Math.cos(angle) * distance);
				y = population.center.getY() + (int) (Math.sin(angle) * distance);
			}
			final Location valid = GeoEngine.getInstance().getValidLocation(population.center, new Location(x, y, population.center.getZ()));
			final int groundZ = GeoEngine.getInstance().getHeight(valid.getX(), valid.getY(), valid.getZ());
			final Location candidate = new Location(valid.getX(), valid.getY(), groundZ);
			fallback = candidate;
			if (isClearOfOtherPhantoms(candidate))
			{
				return candidate;
			}
		}
		return fallback; // gave up finding a clear spot; place anyway
	}

	/** @return {@code true} if no live phantom is within {@link #MIN_SEPARATION} of the spot. */
	private boolean isClearOfOtherPhantoms(Location loc)
	{
		for (PhantomData data : _phantoms.values())
		{
			final Player other = data.player;
			if ((other == null) || other.isDead())
			{
				continue;
			}
			final double dx = other.getX() - loc.getX();
			final double dy = other.getY() - loc.getY();
			if (((dx * dx) + (dy * dy)) < (MIN_SEPARATION * MIN_SEPARATION))
			{
				return false;
			}
		}
		return true;
	}

	private static Location randomPointInPolygon(Population population)
	{
		int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
		for (Location v : population.polygon)
		{
			minX = Math.min(minX, v.getX());
			maxX = Math.max(maxX, v.getX());
			minY = Math.min(minY, v.getY());
			maxY = Math.max(maxY, v.getY());
		}
		for (int i = 0; i < 30; i++)
		{
			final int rx = Rnd.get(minX, maxX);
			final int ry = Rnd.get(minY, maxY);
			if (isInPolygon(rx, ry, population.polygon))
			{
				return new Location(rx, ry, population.center.getZ());
			}
		}
		return population.center;
	}

	private static boolean isInPolygon(int px, int py, List<Location> poly)
	{
		boolean inside = false;
		for (int i = 0, j = poly.size() - 1; i < poly.size(); j = i++)
		{
			final int xi = poly.get(i).getX(), yi = poly.get(i).getY();
			final int xj = poly.get(j).getX(), yj = poly.get(j).getY();
			if (((yi > py) != (yj > py)) && (px < ((double) (xj - xi) * (py - yi) / (yj - yi)) + xi))
			{
				inside = !inside;
			}
		}
		return inside;
	}

	/**
	 * Auto-generates {@code population.regularCount} stable regular identities and appends them to the
	 * population's roster (alongside any hand-authored ones). Each slot is built from a seed derived from the
	 * population name + slot index, so the same slot yields the same name/appearance/class on every restart -
	 * no manual authoring needed. Reuses the shared name pools and mage/fighter class pools so auto-regulars
	 * blend in with the rest of the crowd. Buddies keep their role class at spawn; the generated classId is
	 * simply ignored there.
	 * @param population the group to populate (no-op if regularCount <= 0)
	 */
	private void generateAutoRegulars(Population population)
	{
		final int wanted = Math.min(population.regularCount, MAX_AUTO_REGULARS);
		for (int i = 0; i < wanted; i++)
		{
			// Deterministic per (population, slot): a stable seed so the identity is identical across restarts.
			final Random rng = new Random((((long) population.name.hashCode()) << 20) ^ ((i + 1) * 0x9E3779B97F4A7C15L));
			final Regular regular = new Regular();
			regular.name = FakePlayerAppearanceFactory.generateName(rng);
			final boolean mage = rng.nextInt(100) < MAGE_CHANCE;
			final int[] pool = mage ? MAGE_CLASS_POOL : FIGHTER_CLASS_POOL;
			regular.classId = pool[rng.nextInt(pool.length)];
			// Orc (44) / Dwarf (53) bodies skew male, like the appearance factory and the random spawn path.
			regular.female = ((regular.classId == 44) || (regular.classId == 53)) ? (rng.nextInt(100) < 30) : rng.nextBoolean();
			regular.face = (byte) rng.nextInt(3); // 0-2
			regular.hairColor = (byte) rng.nextInt(4); // 0-3
			regular.hairStyle = (byte) rng.nextInt(3); // 0-2
			population.regulars.add(regular);
		}
	}

	/**
	 * Rolls whether this spawn should use one of the population's fixed regular identities, and if so picks one
	 * that isn't already standing in the world (so the same regular never appears twice at once).
	 * @param population the owning group (null for ad-hoc/admin/recruit spawns, which never use regulars)
	 * @return a free regular to spawn as, or {@code null} to spawn a fresh random identity
	 */
	private Regular pickRegular(Population population)
	{
		if ((population == null) || population.regulars.isEmpty() || (Rnd.get(100) >= population.regularChance))
		{
			return null;
		}

		// Exclude regulars already spawned (a phantom carrying that exact name). Player.create would also reject a
		// duplicate name, but filtering first lets us pick another free regular instead of failing the spawn.
		final List<Regular> free = new ArrayList<>(population.regulars);
		for (PhantomData data : _phantoms.values())
		{
			final String live = data.player.getName();
			free.removeIf(regular -> regular.name.equalsIgnoreCase(live));
		}
		return free.isEmpty() ? null : free.get(Rnd.get(free.size()));
	}

	/**
	 * Looks up the charId of an already-persisted regular by name, scoped to the {@link #ACCOUNT_NAME_REGULAR}
	 * account so a real player who happens to share the name is never matched.
	 * @return the stored charId, or 0 if this regular has no persisted row yet
	 */
	private int findRegularCharId(String name)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT charId FROM characters WHERE char_name=? AND account_name=?"))
		{
			ps.setString(1, name);
			ps.setString(2, ACCOUNT_NAME_REGULAR);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					return rs.getInt("charId");
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Failed to look up persisted regular '" + name + "': " + e.getMessage());
		}
		return 0;
	}

	/**
	 * @return {@code true} if the given player is a persistent "regular" phantom - either loaded from the
	 *         {@link #ACCOUNT_NAME_REGULAR} account, or promoted to it this session by being befriended
	 *         ({@link #_promoted}; the final in-memory account name lags the DB until the next reload). Real
	 *         players and ordinary ephemeral phantoms return {@code false}.
	 */
	public boolean isRegular(Player player)
	{
		return (player != null) && (ACCOUNT_NAME_REGULAR.equals(player.getAccountName()) || _promoted.contains(player.getObjectId()));
	}

	/** @return {@code true} if the given player is any live phantom this manager owns (regular or ephemeral). */
	public boolean isPhantom(Player player)
	{
		return (player != null) && _phantoms.containsKey(player.getObjectId());
	}

	/**
	 * Completes a friendship between a real player and ANY live phantom server-side, promoting the phantom to a
	 * persistent regular in the process. A phantom is clientless, so it can never answer the
	 * {@code FriendAddRequest} dialog the stock invite flow sends - instead, when a player friend-invites one we
	 * accept immediately: promote it to the {@link #ACCOUNT_NAME_REGULAR} account (so its row, name, look and
	 * class become permanent - "you liked this one, it's a character now"), persist the pair in
	 * {@code character_friends} both directions (mirroring {@code RequestAnswerFriendInvite}) and update both
	 * in-memory lists, so it shows in the player's friends window right away. No XML authoring is needed;
	 * befriending IS what makes a phantom a regular. Buddies work too: their persisted support class maps back
	 * to a {@link BuddyRole} at friend-spawn time, so a befriended buffer comes back as a proper idle buddy
	 * (whisperable for buffs/party), not a hunter.
	 * @param player the inviting real player
	 * @param phantom the target phantom (assumed {@link #isPhantom(Player)})
	 */
	public void befriendPhantom(Player player, Player phantom)
	{
		if ((player == null) || (phantom == null))
		{
			return;
		}
		if (player.getFriendList().contains(phantom.getObjectId()))
		{
			player.sendPacket(SystemMessageId.THIS_PLAYER_IS_ALREADY_REGISTERED_IN_YOUR_FRIENDS_LIST);
			return;
		}

		// Promote an ephemeral phantom to a persistent regular: flip its DB row to the regular account so the
		// boot sweep skips it, despawn keeps it, and the friend login-spawn can find it. The in-memory account
		// can't change (final), so _promoted covers the live instance until it is next reloaded from DB. The
		// UPDATE is durable: Player.storeMe() never writes account_name, so nothing can revert it.
		if (!isRegular(phantom))
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement("UPDATE characters SET account_name=? WHERE charId=?"))
			{
				ps.setString(1, ACCOUNT_NAME_REGULAR);
				ps.setInt(2, phantom.getObjectId());
				ps.executeUpdate();
			}
			catch (Exception e)
			{
				LOGGER.warning(getClass().getSimpleName() + ": Failed to promote phantom " + phantom.getName() + " to regular: " + e.getMessage());
				return; // without the promotion the friendship row would dangle once the phantom's row is deleted
			}
			_promoted.add(phantom.getObjectId());
			LOGGER.info(getClass().getSimpleName() + ": Promoted phantom '" + phantom.getName() + "' (charId=" + phantom.getObjectId() + ") to a persistent regular (befriended by " + player.getName() + ").");
		}

		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("INSERT INTO character_friends (charId, friendId) VALUES (?, ?), (?, ?)"))
		{
			ps.setInt(1, player.getObjectId());
			ps.setInt(2, phantom.getObjectId());
			ps.setInt(3, phantom.getObjectId());
			ps.setInt(4, player.getObjectId());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Failed to persist friendship between " + player.getName() + " and regular " + phantom.getName() + ": " + e.getMessage());
			return;
		}
		player.getFriendList().add(phantom.getObjectId());
		phantom.getFriendList().add(player.getObjectId());
		// Track it for the supervisor's ensure pass, so from this moment it is kept online with its owner.
		_friendRegularsByOwner.computeIfAbsent(player.getObjectId(), k -> ConcurrentHashMap.newKeySet()).add(phantom.getObjectId());
		final SystemMessage msg = new SystemMessage(SystemMessageId.S1_HAS_BEEN_ADDED_TO_YOUR_FRIENDS_LIST);
		msg.addString(phantom.getName());
		player.sendPacket(msg);
		player.sendPacket(new L2Friend(phantom, 1)); // show it online in the friends window right away
	}

	/**
	 * "Always online" friend behaviour (Phase 3b): shortly after a player logs in, spawn each of their
	 * befriended regulars at its own stored location and announce it online, so a friend shows up in the
	 * friends list even when the player is nowhere near that regular's home zone. Also primes the
	 * {@link #_friendRegularsByOwner} cache the supervisor uses to KEEP them online (respawn after death,
	 * zone deactivation, etc.) for as long as the owner is. Runs off the login thread.
	 * @param owner the player who just entered the world
	 */
	public void onOwnerLogin(Player owner)
	{
		if ((owner == null) || owner.getFriendList().isEmpty())
		{
			return;
		}
		final int ownerId = owner.getObjectId();
		ThreadPool.schedule(() ->
		{
			final Player online = World.getInstance().getPlayer(ownerId);
			if (online == null)
			{
				return; // logged back out before the delay elapsed
			}
			final Set<Integer> ids = ConcurrentHashMap.newKeySet();
			ids.addAll(findFriendRegulars(ownerId));
			_friendRegularsByOwner.put(ownerId, ids);
			ensureFriendRegulars(ownerId, online);
		}, FRIEND_SPAWN_DELAY);
	}

	/**
	 * Spawns any of this owner's friend-regulars that are not currently live, announcing each to the owner.
	 * Called at login and periodically from the supervisor, so a friend that died, was despawned with a
	 * deactivating population, or failed to spawn earlier comes back on its own while the owner is online.
	 */
	private void ensureFriendRegulars(int ownerId, Player owner)
	{
		final Set<Integer> ids = _friendRegularsByOwner.get(ownerId);
		if ((ids == null) || ids.isEmpty())
		{
			return;
		}
		for (int regularId : ids)
		{
			// The player may have friend-deleted it since the cache was primed (stock RequestFriendDel updates
			// the owner's in-memory list) - prune instead of resurrecting an ex-friend.
			if (!owner.getFriendList().contains(regularId))
			{
				ids.remove(regularId);
				continue;
			}
			if (_phantoms.containsKey(regularId))
			{
				continue; // already live (population spawn, a prior ensure, or promoted while spawned)
			}
			final Player regular = spawnFriendRegular(regularId, ownerId);
			if (regular != null)
			{
				owner.sendPacket(new L2Friend(regular, 1)); // flip it online in the friends window
			}
		}
	}

	/**
	 * Despawns the friend-regulars that were login-spawned for a player who is logging out, so they do not
	 * linger with nobody around. A regular that another still-online player is also friends with is handed over
	 * to that player rather than despawned.
	 * @param owner the player logging out
	 */
	public void onOwnerLogout(Player owner)
	{
		if (owner == null)
		{
			return;
		}
		final int ownerId = owner.getObjectId();
		_friendRegularsByOwner.remove(ownerId); // stop the supervisor keeping them online
		for (PhantomData data : new ArrayList<>(_phantoms.values()))
		{
			if (data.friendOwnerId != ownerId)
			{
				continue;
			}
			final int newOwner = otherOnlineFriendOf(data.player.getObjectId(), ownerId);
			if (newOwner != 0)
			{
				data.friendOwnerId = newOwner; // still wanted by another online friend - keep it, hand it over
			}
			else
			{
				despawn(data);
			}
		}
	}

	/**
	 * Spawns a specific persisted regular by charId at its own stored location because a friend (its owner) is
	 * now online. No-op if it is already live (a population may have spawned it, or a prior login), the phantom
	 * cap is reached, or the row cannot be loaded.
	 * @param charId the regular's stable charId
	 * @param ownerId the real player it accompanies (tagged on the phantom for logout despawn)
	 * @return the spawned phantom, or {@code null} if nothing was spawned
	 */
	private Player spawnFriendRegular(int charId, int ownerId)
	{
		if (_phantoms.containsKey(charId) || (_phantoms.size() >= MAX_PHANTOMS))
		{
			return null;
		}
		Player phantom = null;
		try
		{
			phantom = Player.load(charId);
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Failed to load friend-regular " + charId + ": " + e.getMessage());
		}
		if (phantom == null)
		{
			return null;
		}
		final int groundZ = GeoEngine.getInstance().getHeight(phantom.getX(), phantom.getY(), phantom.getZ());
		final Location spawnLocation = new Location(phantom.getX(), phantom.getY(), groundZ);
		// A befriended buddy's support class maps back to its BuddyRole, so it comes back as a proper idle
		// buddy (buddy gear/reagents, registered with PhantomBuddyManager, whisperable for buffs/party)
		// instead of a sword-swinging hunter. The buddy class ids (17/30/52) are never in the hunter pools,
		// so an ordinary promoted hunter can't be misread as one. For everything else the caster flag comes
		// from the class itself (isMage), not the DD pool - so a promoted support-class recruit (Bishop etc.)
		// re-gears as a robe caster rather than a melee fighter.
		final BuddyRole role = buddyRoleForClass(phantom.getPlayerClass().getId());
		final boolean mage = role.isBuddy() || phantom.getPlayerClass().isMage();
		return finishSpawn(phantom, spawnLocation, phantom.getLevel(), mage, role, null, true, ownerId);
	}

	/** @return the {@link BuddyRole} whose support class the given classId is, or {@link BuddyRole#NONE}. */
	private static BuddyRole buddyRoleForClass(int classId)
	{
		for (BuddyRole role : BuddyRole.values())
		{
			if (role.isBuddy() && (role.classId == classId))
			{
				return role;
			}
		}
		return BuddyRole.NONE;
	}

	/**
	 * Creates a brand-new persistent regular to the player's own spec - name, class, level, sex, looks - spawns
	 * it next to them, and befriends it on the spot ("recreate an old friend to play together"). It is created
	 * straight onto the {@link #ACCOUNT_NAME_REGULAR} account, so it is permanent from birth and flows into the
	 * whole friend tier (always-online, friend chat, stable brain persona keyed to the chosen name).
	 * @param owner the real player crafting the friend (also its friend + spawn anchor)
	 * @param name the character name (1-16 letters/digits, must be free)
	 * @param classSpec {@code fighter}/{@code mage}/{@code elder}/{@code prophet}/{@code warcryer}, a raw class
	 *            id, or empty for a random fighter
	 * @param level target level, or 0 to match the owner's level (clamped 1-80)
	 * @param sex 0 = male, 1 = female, -1 = random
	 * @param face 0-2 or -1 = random
	 * @param hairStyle 0-2 or -1 = random
	 * @param hairColor 0-3 or -1 = random
	 * @return a human-readable result message for the invoking tooling
	 */
	public String craftFriend(Player owner, String name, String classSpec, int level, int sex, int face, int hairStyle, int hairColor)
	{
		if ((owner == null) || (name == null) || name.isEmpty())
		{
			return "No name given.";
		}
		if (!name.matches("[A-Za-z0-9]{1,16}"))
		{
			return "Invalid name '" + name + "' (1-16 letters/digits).";
		}
		if (CharInfoTable.getInstance().doesCharNameExist(name))
		{
			return "The name '" + name + "' is already taken.";
		}
		if (_phantoms.size() >= MAX_PHANTOMS)
		{
			return "Phantom cap reached (" + MAX_PHANTOMS + ").";
		}

		// Resolve the class: an archetype keyword, a buddy role, or a raw class id.
		final int classId;
		final String spec = (classSpec == null) ? "" : classSpec.trim().toLowerCase();
		switch (spec)
		{
			case "":
			case "fighter":
			{
				classId = FIGHTER_CLASS_POOL[Rnd.get(FIGHTER_CLASS_POOL.length)];
				break;
			}
			case "mage":
			{
				classId = MAGE_CLASS_POOL[Rnd.get(MAGE_CLASS_POOL.length)];
				break;
			}
			case "elder":
			case "healer":
			{
				classId = BuddyRole.ELDER.classId;
				break;
			}
			case "prophet":
			case "buffer":
			{
				classId = BuddyRole.PROPHET.classId;
				break;
			}
			case "warcryer":
			{
				classId = BuddyRole.WARCRYER.classId;
				break;
			}
			default:
			{
				int parsed;
				try
				{
					parsed = Integer.parseInt(spec);
				}
				catch (NumberFormatException e)
				{
					return "Unknown class '" + classSpec + "' (use fighter/mage/elder/prophet/warcryer or a class id).";
				}
				classId = parsed;
				break;
			}
		}
		final PlayerClass playerClass = PlayerClass.getPlayerClass(classId);
		final PlayerTemplate template = (playerClass == null) ? null : PlayerTemplateData.getInstance().getTemplate(playerClass);
		if (template == null)
		{
			return "No player template for class id " + classId + ".";
		}

		final boolean female = (sex == 1) || ((sex < 0) && Rnd.nextBoolean());
		final PlayerAppearance appearance = new PlayerAppearance( //
			(byte) ((face >= 0) ? Math.min(face, 2) : Rnd.get(0, 2)), //
			(byte) ((hairColor >= 0) ? Math.min(hairColor, 3) : Rnd.get(0, 3)), //
			(byte) ((hairStyle >= 0) ? Math.min(hairStyle, 2) : Rnd.get(0, 2)), female);

		// Persistent from birth: created straight onto the regular account (no promotion step needed).
		final Player phantom = Player.create(template, ACCOUNT_NAME_REGULAR, name, appearance);
		if (phantom == null)
		{
			return "Creation failed (duplicate name / db error?).";
		}

		final int targetLevel = Math.max(1, Math.min(80, (level > 0) ? level : owner.getLevel()));
		final int groundZ = GeoEngine.getInstance().getHeight(owner.getX() + 50, owner.getY() + 50, owner.getZ());
		final Location spawnLocation = new Location(owner.getX() + 50, owner.getY() + 50, groundZ);
		final BuddyRole role = buddyRoleForClass(classId);
		final boolean mage = role.isBuddy() || playerClass.isMage();
		try
		{
			if (finishSpawn(phantom, spawnLocation, targetLevel, mage, role, null, false, owner.getObjectId()) == null)
			{
				return "Spawn failed - check the gameserver log.";
			}
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Failed to craft friend '" + name + "': " + e.getMessage());
			GameClient.deleteCharByObjId(phantom.getObjectId()); // don't leave a half-made row behind
			return "Spawn failed: " + e.getMessage();
		}
		befriendPhantom(owner, phantom);
		return "Created your friend '" + name + "' (" + playerClass + ", level " + targetLevel + (role.isBuddy() ? (", " + role + " buddy") : "") + ") - added to your friends list.";
	}

	/**
	 * Attempts every pending editor-authored {@code <friend>} order belonging to this (online, real) player:
	 * an order whose name already exists is inert (created on an earlier pass / an earlier boot - never
	 * duplicated, never re-befriended after a friend-delete), anything else is crafted via
	 * {@link #craftFriend}. Each order is attempted at most once per load - success or failure it is removed,
	 * so a bad entry (invalid name/class) logs once instead of spamming every pass; a reload re-arms it.
	 * The one transient exception is the phantom cap: a cap-blocked order is kept and retried later.
	 */
	private void materializeCraftedFriends(Player owner)
	{
		if ((owner == null) || isPhantom(owner))
		{
			return;
		}
		synchronized (_craftedFriends)
		{
			for (Iterator<CraftedFriend> it = _craftedFriends.iterator(); it.hasNext();)
			{
				final CraftedFriend friend = it.next();
				if (!friend.owner.equalsIgnoreCase(owner.getName()))
				{
					continue;
				}
				if (_phantoms.size() >= MAX_PHANTOMS)
				{
					return; // transient (phantom cap): keep the order and retry on a later pass
				}
				it.remove();
				if (CharInfoTable.getInstance().doesCharNameExist(friend.name))
				{
					// Already created on an earlier boot (or the name is simply taken) - the order is permanently
					// inert. Logged so "nothing happened" is diagnosable from the gameserver log.
					LOGGER.info(getClass().getSimpleName() + ": <friend> order '" + friend.name + "' for " + owner.getName() + ": name already exists - order is inert (created earlier, or the name is taken).");
					continue;
				}
				final String result = craftFriend(owner, friend.name, friend.classSpec, friend.level, friend.sex, friend.face, friend.hairStyle, friend.hairColor);
				LOGGER.info(getClass().getSimpleName() + ": <friend> order '" + friend.name + "' for " + owner.getName() + ": " + result);
			}
		}
	}

	/** @return the charIds of the given player's befriended regulars (friends on the {@code phantom_regular} account). */
	private List<Integer> findFriendRegulars(int ownerId)
	{
		final List<Integer> ids = new ArrayList<>();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT c.charId FROM characters c JOIN character_friends f ON c.charId=f.friendId WHERE f.charId=? AND c.account_name=?"))
		{
			ps.setInt(1, ownerId);
			ps.setString(2, ACCOUNT_NAME_REGULAR);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					ids.add(rs.getInt("charId"));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Failed to list friend-regulars for " + ownerId + ": " + e.getMessage());
		}
		return ids;
	}

	/** @return the objectId of an online real player (other than {@code excludeOwnerId}) friends with this regular, or 0. */
	private int otherOnlineFriendOf(int regularId, int excludeOwnerId)
	{
		for (Player p : World.getInstance().getPlayers())
		{
			if ((p.getObjectId() != excludeOwnerId) && !isRegular(p) && p.getFriendList().contains(regularId))
			{
				return p.getObjectId();
			}
		}
		return 0;
	}

	/** @return {@code true} if the class id belongs to the DD-mage pool (so the caster gear/combat tick applies). */
	private static boolean isMageClass(int classId)
	{
		for (int mageClass : MAGE_CLASS_POOL)
		{
			if (mageClass == classId)
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Creates a brand-new clientless fighter phantom, levels/skills/gears it, spawns it, and hands it to
	 * the native auto-hunt so it seeks and fights monsters on its own.
	 * @param location where to spawn (also its home)
	 * @param level the level to bring the phantom to
	 * @param population the owning group (null for ad-hoc admin spawns)
	 * @return the spawned phantom, or {@code null} on failure
	 */
	private Player createAndSpawn(Location location, int level, Population population)
	{
		if (_phantoms.size() >= MAX_PHANTOMS)
		{
			return null; // safety ceiling reached
		}
		// Snap the spawn point to the geodata ground. The population deploy path already snaps via
		// rollLocation, but the ad-hoc/admin path keeps the caller's raw Z (e.g. the admin's feet Z) at an
		// offset (x,y) whose real ground sits higher/lower, so the phantom ends up embedded in or floating
		// over the terrain and its pathfinding raycasts from a bad Z. Snapping here makes every path safe;
		// it is idempotent for the already-snapped deploy locations.
		final int groundZ = GeoEngine.getInstance().getHeight(location.getX(), location.getY(), location.getZ());
		final Location spawnLocation = new Location(location.getX(), location.getY(), groundZ);
		final BuddyRole role = (population != null) ? population.role : BuddyRole.NONE;
		try
		{
			// Maybe use one of this population's fixed "regular" identities so the zone has recurring, recognizable
			// faces (stable name -> stable brain voice) instead of an all-random crowd. Null = spawn randomly.
			final Regular regular = pickRegular(population);

			// Buddies are a fixed support class (Elder/Prophet/Warcryer) and always cast, so they take the mage
			// gear loadout. Ordinary phantoms roll a random race/body type: mostly melee fighters, a share DD
			// mages. A regular may pin its own class; otherwise roll. Either way fall back to Human Fighter if a
			// template is missing.
			final boolean mage;
			final int classId;
			if (role.isBuddy())
			{
				mage = true;
				classId = role.classId;
			}
			else if ((regular != null) && (regular.classId > 0))
			{
				classId = regular.classId;
				mage = isMageClass(classId);
			}
			else
			{
				mage = Rnd.get(100) < MAGE_CHANCE;
				final int[] pool = mage ? MAGE_CLASS_POOL : FIGHTER_CLASS_POOL;
				classId = pool[Rnd.get(pool.length)];
			}
			PlayerClass playerClass = PlayerClass.getPlayerClass(classId);
			PlayerTemplate template = (playerClass == null) ? null : PlayerTemplateData.getInstance().getTemplate(playerClass);
			if (template == null)
			{
				playerClass = PlayerClass.FIGHTER;
				template = PlayerTemplateData.getInstance().getTemplate(playerClass);
			}
			if (template == null)
			{
				LOGGER.warning(getClass().getSimpleName() + ": No FIGHTER template available.");
				return null;
			}

			// A regular pins its own name/appearance; otherwise roll. Orc (Warcryer) and dwarf bodies skew male,
			// like the NPC appearance factory.
			final boolean female = (regular != null) ? regular.female //
				: (((classId == 44) || (classId == 53) || (role == BuddyRole.WARCRYER)) ? (Rnd.get(100) < 30) : Rnd.nextBoolean());
			final PlayerAppearance appearance = (regular != null) //
				? new PlayerAppearance(regular.face, regular.hairColor, regular.hairStyle, female) //
				: new PlayerAppearance((byte) Rnd.get(0, 2), (byte) Rnd.get(0, 3), (byte) Rnd.get(0, 2), female);

			// A regular gets a PERSISTENT row under ACCOUNT_NAME_REGULAR so its charId is stable across reboots
			// (the future friend tier references it by id). First spawn creates that row; every spawn after
			// loads the same row back instead of creating a new character - keeping its charId and its persisted
			// level/class/skills (only the consumable loadout is refreshed below, see the gearing block). Random
			// phantoms (regular == null) are unchanged: ephemeral, ACCOUNT_NAME.
			final int existingId = (regular != null) ? findRegularCharId(regular.name) : 0;
			final boolean loadedRegular = (regular != null) && (existingId > 0);
			final Player phantom;
			if (loadedRegular)
			{
				phantom = Player.load(existingId);
				if (phantom == null)
				{
					LOGGER.warning(getClass().getSimpleName() + ": Player.load(" + existingId + ") returned null for persisted regular '" + regular.name + "'.");
					return null;
				}
			}
			else
			{
				phantom = Player.create(template, (regular != null) ? ACCOUNT_NAME_REGULAR : ACCOUNT_NAME, (regular != null) ? regular.name : nextName(), appearance);
				if (phantom == null)
				{
					LOGGER.warning(getClass().getSimpleName() + ": Player.create returned null (duplicate name / db error?).");
					return null;
				}
			}

			// A loaded regular's class/appearance came back from its DB row, so the pre-load roll above is moot -
			// derive the caster flag from the ACTUAL restored class (the auto-hunt combat tick and gear pick both
			// key off it, and a mismatched flag would gear/fight it as the wrong archetype). isMage() rather than
			// the DD-pool check, so a support-class char (possible via promotion or an authored classId) re-gears
			// as a robe caster, not a melee fighter.
			final boolean effectiveMage = loadedRegular ? phantom.getPlayerClass().isMage() : mage;
			return finishSpawn(phantom, spawnLocation, level, effectiveMage, role, population, loadedRegular, 0);
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Failed to spawn phantom: " + e.getMessage());
			return null;
		}
	}

	/**
	 * Finishes materializing an already created-or-loaded phantom {@link Player}: gears it, drops it into the
	 * world at {@code spawnLocation}, registers it, and hands it to the buddy manager or the auto-hunt. Shared
	 * by the population/admin path ({@link #createAndSpawn}) and the friend login-spawn path
	 * ({@link #spawnFriendRegular}).
	 * @param loadedRegular {@code true} if {@code phantom} came from {@link Player#load} (skip re-leveling, just
	 *            refresh the consumable loadout on a clean inventory)
	 * @param friendOwnerId objectId of the real player this phantom was spawned to accompany (0 = none), used to
	 *            despawn it again when that owner logs out
	 * @return the phantom, now live
	 */
	private Player finishSpawn(Player phantom, Location spawnLocation, int level, boolean mage, BuddyRole role, Population population, boolean loadedRegular, int friendOwnerId)
	{
		// Ignore the weight penalty. Phantoms carry a large stack of healing potions (+ soulshots), which
		// easily exceeds a non-Dwarf's max load - at >=100% load the engine applies Weight Penalty (skill
		// 4270) level 4, whose runSpd multiplier is 0, so the phantom is fully immobilized. Dwarves have a
		// far higher carry capacity, which is why only they moved before this. Diet mode zeroes the penalty
		// regardless of load (set before items are added so the inventory weight never pins them).
		phantom.setDietMode(true);

		// Level, skill and gear the phantom BEFORE it enters the world, so the very first CharInfo nearby
		// players receive already shows its full set. (A post-spawn equip update can be throttled or coalesced
		// by broadcastCharInfo, leaving gear invisible even though it was equipped.) A loaded regular already
		// has its level/class/skills persisted, so the exp/class/skill steps below are guarded no-ops for it -
		// but its consumables were spent hunting and its auto-use/soulshot registrations are runtime-only, so
		// it still needs re-gearing. Wipe its stale inventory first so the restock doesn't stack duplicates.
		phantom.setOnlineStatus(true, false);
		if (loadedRegular)
		{
			phantom.getInventory().destroyAllItems(ItemProcessType.DESTROY, phantom, null);
		}
		if (role.isBuddy())
		{
			outfitBuddy(phantom, level, role);
		}
		else if (friendOwnerId != 0)
		{
			// A friend parties with its owner and pulls real weight in content: full best-in-grade kit
			// (the recruited-member loadout), not the deliberately cheap ambient look.
			outfitFriend(phantom, level, mage);
		}
		else
		{
			outfit(phantom, level, mage);
		}
		phantom.refreshOverloaded();
		enterWorld(phantom, spawnLocation);

		final PhantomData data = new PhantomData(phantom, spawnLocation, population, mage, role);
		data.friendOwnerId = friendOwnerId;
		_phantoms.put(phantom.getObjectId(), data);
		if (role.isBuddy())
		{
			// Buddies don't hunt - they stand where placed (and do nothing, not even self-buff) until a real
			// player whispers/parties them. PhantomBuddyManager owns everything from there.
			PhantomBuddyManager.getInstance().onBuddySpawned(data.player, role.name());
		}
		else
		{
			// Fan out first; the auto-hunt starts when dispersal ends (see supervise), so a freshly spawned
			// group spreads over its area instead of all converging on the nearest mobs at once.
			beginDisperse(phantom, data);
		}
		startSupervising();
		final String kind = role.isBuddy() ? (role + " buddy") //
			: (friendOwnerId != 0) ? "friend-regular" //
				: ACCOUNT_NAME_REGULAR.equals(phantom.getAccountName()) ? "regular" : "phantom";
		LOGGER.info(getClass().getSimpleName() + ": Spawned " + kind + " '" + phantom.getName() + "' (objId=" + phantom.getObjectId() + ", level " + level + ").");
		return phantom;
	}

	/**
	 * Spawns a single ad-hoc phantom (admin testing path). Population-driven deployment uses
	 * {@link #deployOne(Population)} instead.
	 * @param location where to spawn
	 * @param level the level to bring it to
	 * @return the spawned phantom, or {@code null} on failure
	 */
	public Player spawnPhantom(Location location, int level)
	{
		if (!AutoPlayConfig.ENABLE_AUTO_PLAY)
		{
			LOGGER.warning(getClass().getSimpleName() + ": EnableAutoPlay is false in config/Custom/AutoPlay.ini - phantoms will spawn but not hunt.");
		}
		return createAndSpawn(location, level, null);
	}

	/**
	 * Drops an already-leveled, already-geared clientless player into the world exactly the way
	 * offline-play characters are restored - no GameClient is ever attached. {@code setOfflinePlay(true)}
	 * is required so the AutoPlay loop does not treat the missing client as a plain offline-shop and stop
	 * the task.
	 */
	private void enterWorld(Player phantom, Location location)
	{
		phantom.setCurrentHp(phantom.getMaxHp());
		phantom.setCurrentMp(phantom.getMaxMp());
		phantom.setCurrentCp(phantom.getMaxCp());
		phantom.spawnMe(location.getX(), location.getY(), location.getZ());
		phantom.setOfflinePlay(true);
		phantom.setOnlineStatus(true, true);
		phantom.setRunning();
		phantom.broadcastUserInfo();
	}

	/**
	 * Brings a phantom to the requested level (granting the exact experience for it), advances it to the
	 * class its level warrants, learns that class's full skill tree up to its level, gears it, tops up its
	 * bars, and registers its skills with the auto-use system.
	 * @param phantom the phantom to outfit
	 * @param level the target level
	 */
	private void outfit(Player phantom, int level, boolean mage)
	{
		if (level > 1)
		{
			final long currentExp = phantom.getExp();
			final long targetExp = ExperienceData.getInstance().getExpForLevel(level);
			if (targetExp > currentExp)
			{
				phantom.addExpAndSp(targetExp - currentExp, 0);
			}
		}
		// Advance through the class transfers a real character of this level would have done (fighters down
		// a melee branch, mages down a DD/nuker branch), then learn everything that class can learn by now -
		// so a level-40 phantom is a proper 2nd-class character with a real kit. Learning is looped so
		// chained skills resolve.
		transferClass(phantom, level, mage);
		learnAllSkills(phantom);
		gear(phantom, level, mage);
		phantom.setCurrentHpMp(phantom.getMaxHp(), phantom.getMaxMp());
		phantom.setCurrentCp(phantom.getMaxCp());
		registerAutoSkills(phantom);
	}

	/**
	 * Outfits a befriended/crafted friend-regular: same leveling/class/skill pipeline as {@link #outfit}, but
	 * geared through {@link #gearParty} - best-in-grade role weapon, full armor set (no random gaps), all five
	 * jewelry slots, a shield for a tank class, an enchant chance - and it arrives fully buffed. A friend is
	 * meant to party with its owner and do real content, not blend in as a cheaply-dressed ambient extra.
	 */
	private void outfitFriend(Player phantom, int level, boolean mage)
	{
		if (level > 1)
		{
			final long currentExp = phantom.getExp();
			final long targetExp = ExperienceData.getInstance().getExpForLevel(level);
			if (targetExp > currentExp)
			{
				phantom.addExpAndSp(targetExp - currentExp, 0);
			}
		}
		transferClass(phantom, level, mage);
		learnAllSkills(phantom);
		buildGear();
		gearParty(phantom, level, mage, roleForClass(phantom.getPlayerClass()));
		PhantomBuffs.applyFullBuffs(phantom);
		phantom.setCurrentHpMp(phantom.getMaxHp(), phantom.getMaxMp());
		phantom.setCurrentCp(phantom.getMaxCp());
		registerAutoSkills(phantom);
	}

	/**
	 * Outfits a support buddy: brings it to {@code level}, sets it directly to its fixed support class (no
	 * random transfer), learns that class's full skill tree, gives it the mage gear loadout (magic weapon +
	 * robe + spiritshots, so its casts hit faster), tops up its bars and guarantees it can heal. Unlike
	 * {@link #outfit}, it does NOT register auto-hunt skills - a buddy never hunts; the PhantomBuddyManager
	 * casts its buffs/heals by hand on the player it is partied with.
	 * @param phantom the buddy
	 * @param level the target level (should be 40+ so a 2nd-class buff kit is available)
	 * @param role which support class to become
	 */
	private void outfitBuddy(Player phantom, int level, BuddyRole role)
	{
		if (level > 1)
		{
			final long currentExp = phantom.getExp();
			final long targetExp = ExperienceData.getInstance().getExpForLevel(level);
			if (targetExp > currentExp)
			{
				phantom.addExpAndSp(targetExp - currentExp, 0);
			}
		}
		// Become the exact support class straight away (the base Mystic/Cleric template was used to create it).
		if (phantom.getPlayerClass().getId() != role.classId)
		{
			phantom.setPlayerClass(role.classId);
			phantom.setBaseClass(role.classId);
		}
		learnAllSkills(phantom);
		grantHeal(phantom, level);
		giveBuffReagents(phantom); // Spirit Ore etc. so consumable buffs (Greater Might/Shield, Clarity) actually land
		gear(phantom, level, true); // cast loadout
		phantom.setCurrentHpMp(phantom.getMaxHp(), phantom.getMaxMp());
		phantom.setCurrentCp(phantom.getMaxCp());
		logBuddyGear(phantom, role);
	}

	/**
	 * TEMP DEBUG: logs every piece a buddy has equipped (slot item name / id / bodypart), to diagnose the Orc
	 * Warcryer bare-torso render. Remove once the gearing is sorted.
	 */
	private void logBuddyGear(Player phantom, BuddyRole role)
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("BUDDY GEAR [").append(role).append(' ').append(phantom.getName()).append(", race=").append(phantom.getRace()).append(", class=").append(phantom.getPlayerClass()).append(", lvl ").append(phantom.getLevel()).append("] equipped:");
		boolean any = false;
		for (Item item : phantom.getInventory().getPaperdollItems())
		{
			any = true;
			sb.append(" {").append(item.getTemplate().getName()).append(" id=").append(item.getId()).append(" bodypart=").append(item.getTemplate().getBodyPart()).append("}");
		}
		if (!any)
		{
			sb.append(" (nothing)");
		}
		LOGGER.info(getClass().getSimpleName() + ": " + sb);
	}

	/**
	 * Ensures a buddy can heal. The Elven Elder learns heals naturally; Prophet and Warcryer do not have one
	 * in Interlude, so grant a basic Heal (id 1011) scaled to level - the user wants every buddy able to top
	 * its owner up (to roughly half health), and this keeps that data-driven rather than per-class hard-coding.
	 */
	private void grantHeal(Player phantom, int level)
	{
		for (int healId : HEAL_SKILL_IDS)
		{
			if (phantom.getKnownSkill(healId) != null)
			{
				return; // already has a heal
			}
		}
		// Heal (1011) has 18 levels; pick one in step with the buddy's level, capped at the table maximum.
		final int healLevel = Math.max(1, Math.min(18, level / 4));
		final Skill heal = SkillData.getInstance().getSkill(1011, healLevel);
		if (heal != null)
		{
			phantom.addSkill(heal, true);
		}
	}

	/**
	 * Stocks a support phantom with the item reagents its buffs consume (Spirit Ore for the prophet's Greater
	 * Might / Greater Shield and the caster's Clarity, etc.), scanned data-driven from the buffs it actually knows
	 * so it adapts to whatever class/level kit it ended up with. Without the reagent the engine rejects the cast and
	 * the buffer loops on the missing buff forever - so this both fixes that loop and keeps the greater buffs usable.
	 * Must run AFTER the skill tree is learned. A big stack so a long session never runs dry; diet mode keeps the
	 * weight off.
	 * @param phantom the support phantom (buddy or recruited buffer/healer)
	 */
	private void giveBuffReagents(Player phantom)
	{
		final Set<Integer> reagents = new HashSet<>();
		for (Skill skill : phantom.getAllSkills())
		{
			if ((skill == null) || skill.isPassive() || (skill.getItemConsumeId() <= 0) || (skill.getItemConsumeCount() <= 0))
			{
				continue;
			}
			// Only beneficial buffs - never stock reagents for offensive/summon/debuff skills.
			if (skill.isContinuous() && (skill.getEffectPoint() >= 0) && !skill.isDebuff())
			{
				reagents.add(skill.getItemConsumeId());
			}
		}
		for (int reagentId : reagents)
		{
			phantom.getInventory().addItem(ItemProcessType.REWARD, reagentId, BUFF_REAGENT_COUNT, phantom, null);
		}
		if (!reagents.isEmpty())
		{
			LOGGER.info(getClass().getSimpleName() + ": Stocked " + phantom.getName() + " with buff reagents " + reagents + " (x" + BUFF_REAGENT_COUNT + " each).");
		}
	}

	/**
	 * Advances a phantom from its base class to the occupation its level warrants by walking the class
	 * tree, choosing a random in-archetype branch at each transfer: 1st at 20+, 2nd at 40+, 3rd at 76+.
	 * @param phantom the phantom (created as a base fighter or mage)
	 * @param level its level
	 * @param mage {@code true} to follow the DD/nuker line, {@code false} for the melee line
	 */
	private void transferClass(Player phantom, int level, boolean mage)
	{
		final int targetTier = (level < 20) ? 0 : (level < 40) ? 1 : (level < 76) ? 2 : 3;
		if (targetTier == 0)
		{
			return; // base class is correct below level 20
		}
		// Idempotent: a reloaded regular is already transferred to its warranted tier. transferClass walks
		// forward from the CURRENT class, so re-running it on such a char would over-advance it (e.g. bump a
		// tier-2 fighter to a tier-3 class). Only transfer a char still short of its target tier (a fresh one).
		if (phantom.getPlayerClass().level() >= targetTier)
		{
			return;
		}
		PlayerClass current = phantom.getPlayerClass();
		for (int step = 0; step < targetTier; step++)
		{
			final PlayerClass next = randomChild(current, mage);
			if (next == null)
			{
				break; // no further transfer available
			}
			current = next;
		}
		if (current.getId() != phantom.getPlayerClass().getId())
		{
			phantom.setPlayerClass(current.getId());
			phantom.setBaseClass(current.getId());
		}
	}

	/**
	 * A random class transfer from the given class within the wanted archetype: melee fighters, or DD
	 * mages (mystic but not priest/summoner). Returns {@code null} if there are none.
	 */
	private static PlayerClass randomChild(PlayerClass parent, boolean mage)
	{
		final List<PlayerClass> options = new ArrayList<>();
		for (PlayerClass child : parent.getNextClasses())
		{
			final boolean wanted = mage ? (child.isOfType(ClassType.MYSTIC) && !child.isSummoner()) : !child.isMage();
			if (wanted)
			{
				options.add(child);
			}
		}
		return options.isEmpty() ? null : options.get(Rnd.get(options.size()));
	}

	/** Learns every skill the phantom's class can learn at its level, looped so chained skills resolve. */
	private void learnAllSkills(Player phantom)
	{
		int guard = 0;
		while ((phantom.giveAvailableSkills(false, true, true) > 0) && (guard++ < 10))
		{
			// keep learning until nothing new becomes available
		}
	}

	/**
	 * Equips a grade-appropriate set and hands over matching shots (auto-enabled): fighters get a sword +
	 * light/heavy armor + soulshots; mages get a magic weapon + robe + spiritshots. The weapon is what
	 * makes their attack skills / nukes usable; the armor keeps them alive long enough to hunt.
	 * @param phantom the phantom to gear
	 * @param level its level (decides the grade tier)
	 * @param mage {@code true} for a caster loadout
	 */
	private void gear(Player phantom, int level, boolean mage)
	{
		buildGear();
		final Map<BodyPart, EnumMap<CrystalType, List<ItemTemplate>>> set = mage ? MAGE_GEAR : FIGHTER_GEAR;
		final CrystalType desired = gradeForLevel(level);

		// Weapon first, so we know the grade actually equipped (shots must match it exactly).
		final ItemTemplate weapon = pickForSlot(set, BodyPart.R_HAND, desired);
		if (weapon == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": No " + (mage ? "magic weapon" : "sword") + " found in the datapack - phantom stays unarmed.");
		}
		else
		{
			equip(phantom, weapon);
			final int shotId = mage ? spiritshotIdFor(weapon.getCrystalType()) : soulshotIdFor(weapon.getCrystalType());
			phantom.getInventory().addItem(ItemProcessType.REWARD, shotId, SHOT_COUNT, phantom, null);
			phantom.addAutoSoulShot(shotId); // registers both soulshots and spiritshots for auto-use
		}

		// Healing potions: in-combat HP sustain (sitting can't help mid-fight). Native auto-potion drinks
		// one when HP drops below the threshold; the big stack lasts a long farm session.
		phantom.getInventory().addItem(ItemProcessType.REWARD, HP_POTION_ID, HP_POTION_COUNT, phantom, null);
		phantom.getAutoUseSettings().setAutoPotionItem(HP_POTION_ID);
		phantom.getAutoPlaySettings().setAutoPotionPercent(HP_POTION_PERCENT);

		// Armor pieces: a varied at-or-below-grade piece per slot. Completeness varies (chest almost
		// always, helmet/gloves often skipped) so a group is not a row of identical fully-armored clones.
		// Orc casters (Warcryer) skip the armor ids that have no orc model and would render invisibly.
		final Set<Integer> skip = (mage && (phantom.getRace() == Race.ORC)) ? ORC_CASTER_ARMOR_SKIP : null;
		for (BodyPart slot : GEAR_SLOTS)
		{
			if (slot == BodyPart.R_HAND)
			{
				continue;
			}
			if ((slot == BodyPart.HEAD) && (Rnd.get(100) >= 55))
			{
				continue;
			}
			if ((slot == BodyPart.GLOVES) && (Rnd.get(100) >= 60))
			{
				continue;
			}
			if ((slot == BodyPart.FEET) && (Rnd.get(100) >= 85))
			{
				continue;
			}
			if ((slot == BodyPart.LEGS) && (Rnd.get(100) >= 92))
			{
				continue;
			}
			final ItemTemplate piece = pickForSlot(set, slot, desired, skip);
			if (piece != null)
			{
				equip(phantom, piece);
			}
		}
		// No broadcast here: gear() runs before the phantom enters the world, so the whole set is in place
		// for the spawn CharInfo (enterWorld broadcasts afterwards).
	}

	/**
	 * Gears a <b>recruited party member</b> properly for real content, unlike the deliberately cheap and patchy
	 * ambient {@link #gear} loadout. The difference matters for raids: the ambient path picks the cheapest items,
	 * randomly skips helmet/gloves/boots, and equips no jewelry or shield - leaving a "tank" badly under-armored.
	 * Here a party member gets:
	 * <ul>
	 * <li>a <b>best-in-grade</b> weapon for its role (sword / bow / dagger / magic staff) + matching shots (+ arrows
	 * for an archer);</li>
	 * <li>a <b>full</b> armor set - every slot, no random gaps - best in grade (a TANK prefers HEAVY);</li>
	 * <li>all five <b>jewelry</b> slots (necklace + two earrings + two rings) for the P./M.Def the ambient bots lack;</li>
	 * <li>a <b>shield</b> for the TANK;</li>
	 * <li>a chance the whole weapon+armor kit is <b>enchanted</b> (a modest uniform level), so some read as geared
	 * players rather than fresh dingbats.</li>
	 * </ul>
	 */
	private void gearParty(Player phantom, int level, boolean mage, PartyRole role)
	{
		final CrystalType grade = gradeForLevel(level);
		// A chance this member is an enchanted player; if so, a modest uniform enchant on weapon + armor (jewelry is
		// not enchantable in Interlude, so it stays +0).
		final int enchant = (Rnd.get(100) < PARTY_ENCHANT_CHANCE) ? Rnd.get(PARTY_ENCHANT_MIN, PARTY_ENCHANT_MAX + 1) : 0;

		// Weapon (best in grade for the role) + matching shots (+ arrows for a bow).
		final ItemTemplate weapon = partyWeapon(role, mage, grade);
		if (weapon != null)
		{
			equip(phantom, weapon, enchant);
			final int shotId = mage ? spiritshotIdFor(weapon.getCrystalType()) : soulshotIdFor(weapon.getCrystalType());
			phantom.getInventory().addItem(ItemProcessType.REWARD, shotId, SHOT_COUNT, phantom, null);
			phantom.addAutoSoulShot(shotId);
			if ((weapon instanceof Weapon) && (((Weapon) weapon).getItemType() == WeaponType.BOW))
			{
				final ItemTemplate arrow = findArrow(weapon.getCrystalType());
				if (arrow != null)
				{
					phantom.getInventory().addItem(ItemProcessType.REWARD, arrow.getId(), ARROW_COUNT, phantom, null);
				}
			}
		}

		// In-combat HP sustain (same as ambient): auto-potion drinks one below the threshold.
		phantom.getInventory().addItem(ItemProcessType.REWARD, HP_POTION_ID, HP_POTION_COUNT, phantom, null);
		phantom.getAutoUseSettings().setAutoPotionItem(HP_POTION_ID);
		phantom.getAutoPlaySettings().setAutoPotionPercent(HP_POTION_PERCENT);

		// Full armor set: every slot, best in grade, no random gaps. Orc casters still skip the ids that have no
		// orc model and would render invisibly.
		final Set<Integer> skip = (mage && (phantom.getRace() == Race.ORC)) ? ORC_CASTER_ARMOR_SKIP : null;
		final boolean preferHeavy = (role == PartyRole.TANK); // a tank wants the highest mitigation it can wear
		for (BodyPart slot : GEAR_SLOTS)
		{
			if (slot == BodyPart.R_HAND)
			{
				continue;
			}
			final ItemTemplate piece = bestArmor(slot, grade, mage, skip, preferHeavy);
			if (piece != null)
			{
				equip(phantom, piece, enchant);
			}
		}

		// Shield for the tank (a big chunk of a knight's mitigation + block) - and the singer, a knight-line class
		// that fights sword-and-board while it keeps the songs running.
		if ((role == PartyRole.TANK) || (role == PartyRole.SINGER))
		{
			final ItemTemplate shield = bestEquip(grade, item -> (item instanceof Armor) && (((Armor) item).getItemType() == ArmorType.SHIELD));
			if (shield != null)
			{
				equip(phantom, shield, enchant);
			}
		}

		// Jewelry: necklace + two earrings + two rings (matched by body part; the engine fills both ears/fingers).
		equipJewelry(phantom, BodyPart.NECK, grade, 1);
		equipJewelry(phantom, BodyPart.LR_EAR, grade, 2);
		equipJewelry(phantom, BodyPart.LR_FINGER, grade, 2);
	}

	/**
	 * One-time debug dump of a recruited member's actual equipped loadout (with enchant levels) and, for a tank,
	 * whether it actually knows its taunt skills at this level - so a raid trace can rule the gear/skills in or out.
	 */
	private void logLoadout(Player phantom, PartyRole role, int level)
	{
		final StringBuilder sb = new StringBuilder();
		for (Item item : phantom.getInventory().getItems())
		{
			if (item.isEquipped())
			{
				sb.append(item.getTemplate().getName());
				if (item.getEnchantLevel() > 0)
				{
					sb.append(" +").append(item.getEnchantLevel());
				}
				sb.append(", ");
			}
		}
		String extra = "";
		if (role == PartyRole.TANK)
		{
			extra = " | taunts known: Aggression=" + (phantom.getKnownSkill(28) != null) + " AuraOfHate=" + (phantom.getKnownSkill(18) != null);
		}
		LOGGER.info("PARTY-RAID GEAR " + role + " '" + phantom.getName() + "' lvl" + level + " grade=" + gradeForLevel(level) + extra + " | equipped: " + sb);
	}

	/** Best-in-grade weapon for a party role: bow/dagger for the ranged/rogue roles, magic staff for casters, else a sword. */
	private static ItemTemplate partyWeapon(PartyRole role, boolean mage, CrystalType grade)
	{
		if (mage)
		{
			return bestEquip(grade, item -> item.isMagicWeapon() && (item.getBodyPart() == BodyPart.LR_HAND));
		}
		if (role == PartyRole.ARCHER)
		{
			final ItemTemplate bow = bestEquip(grade, item -> (item instanceof Weapon) && (((Weapon) item).getItemType() == WeaponType.BOW));
			if (bow != null)
			{
				return bow;
			}
		}
		else if (role == PartyRole.DAGGER)
		{
			final ItemTemplate dagger = bestEquip(grade, item -> (item instanceof Weapon) && (((Weapon) item).getItemType() == WeaponType.DAGGER));
			if (dagger != null)
			{
				return dagger;
			}
		}
		else if (role == PartyRole.DANCER)
		{
			// Dances hard-require equipped dual swords (<using kind="DUAL"/> in the skill data) - a dancer holding
			// anything else cannot dance at all. Duals exist from D grade up; bestEquip steps down a grade if needed.
			final ItemTemplate dual = bestEquip(grade, item -> (item instanceof Weapon) && (((Weapon) item).getItemType() == WeaponType.DUAL));
			if (dual != null)
			{
				return dual;
			}
		}
		// Sword fallback (and the default melee weapon). A TANK or SINGER needs a ONE-handed sword so its shield fits
		// the left hand; a two-handed sword would otherwise be unequipped when the shield goes on.
		final boolean oneHandOnly = (role == PartyRole.TANK) || (role == PartyRole.SINGER);
		return bestEquip(grade, item -> (item instanceof Weapon) && (((Weapon) item).getItemType() == WeaponType.SWORD) && (!oneHandOnly || (item.getBodyPart() == BodyPart.R_HAND)));
	}

	/** Best-in-grade armor piece for a slot: a tank prefers HEAVY (falls back to any), casters take MAGIC, others LIGHT/HEAVY. */
	private static ItemTemplate bestArmor(BodyPart slot, CrystalType grade, boolean mage, Set<Integer> skip, boolean preferHeavy)
	{
		if (preferHeavy)
		{
			final ItemTemplate heavy = bestArmorOfTypes(slot, grade, skip, EnumSet.of(ArmorType.HEAVY));
			if (heavy != null)
			{
				return heavy;
			}
		}
		return bestArmorOfTypes(slot, grade, skip, mage ? EnumSet.of(ArmorType.MAGIC) : EnumSet.of(ArmorType.LIGHT, ArmorType.HEAVY));
	}

	private static ItemTemplate bestArmorOfTypes(BodyPart slot, CrystalType grade, Set<Integer> skip, Set<ArmorType> allowed)
	{
		return bestEquip(grade, item ->
		{
			if (!(item instanceof Armor) || (item.getBodyPart() != slot))
			{
				return false;
			}
			if ((skip != null) && skip.contains(item.getId()))
			{
				return false;
			}
			return allowed.contains(((Armor) item).getItemType());
		});
	}

	/** Equips {@code count} copies of the best-in-grade jewelry for a body part (necklace/earrings/rings). */
	private void equipJewelry(Player phantom, BodyPart part, CrystalType grade, int count)
	{
		final ItemTemplate jewel = bestEquip(grade, item -> (item instanceof Armor) && (item.getBodyPart() == part));
		if (jewel == null)
		{
			return;
		}
		for (int i = 0; i < count; i++)
		{
			equip(phantom, jewel); // jewelry is not enchantable in Interlude - +0
		}
	}

	/**
	 * The best (most valuable, a good proxy for "best stats") tradeable equipable item of {@code grade} that matches
	 * {@code filter}, stepping down a grade if none exists at the desired one. Scans the item table directly (not the
	 * trimmed-to-cheapest ambient gear map) since party members want the strongest in grade, not the budget option.
	 */
	private static ItemTemplate bestEquip(CrystalType desired, Predicate<ItemTemplate> filter)
	{
		CrystalType grade = desired;
		while (grade != null)
		{
			ItemTemplate best = null;
			for (ItemTemplate item : ItemData.getInstance().getAllItems())
			{
				if ((item == null) || !item.isEquipable() || !item.isTradeable() || (item.getReferencePrice() <= 0) || (item.getCrystalType() != grade))
				{
					continue;
				}
				if (filter.test(item) && ((best == null) || (item.getReferencePrice() > best.getReferencePrice())))
				{
					best = item;
				}
			}
			if (best != null)
			{
				return best;
			}
			grade = (grade.ordinal() > 0) ? CrystalType.values()[grade.ordinal() - 1] : null;
		}
		return null;
	}

	/** Adds a single template to the phantom's inventory and equips it. */
	private void equip(Player phantom, ItemTemplate template)
	{
		equip(phantom, template, 0);
	}

	/** Adds a single template, enchants it (when {@code enchant > 0}), and equips it. */
	private Item equip(Player phantom, ItemTemplate template, int enchant)
	{
		final Item item = phantom.getInventory().addItem(ItemProcessType.REWARD, template.getId(), 1, phantom, null);
		if (item != null)
		{
			if (enchant > 0)
			{
				item.setEnchantLevel(enchant); // set before equipping so the enchant stat bonuses are applied on equip
			}
			phantom.getInventory().equipItem(item);
		}
		return item;
	}

	/** A random candidate item for a slot at the desired grade, stepping down if a grade has none. */
	private static ItemTemplate pickForSlot(Map<BodyPart, EnumMap<CrystalType, List<ItemTemplate>>> set, BodyPart slot, CrystalType desired)
	{
		return pickForSlot(set, slot, desired, null);
	}

	/**
	 * A random candidate item for a slot at the desired grade, stepping down if a grade has none. Item ids in
	 * {@code exclude} are skipped (used to keep non-orc-rendering armor off Orc casters); if a whole grade is
	 * excluded it falls through to a lower grade.
	 */
	private static ItemTemplate pickForSlot(Map<BodyPart, EnumMap<CrystalType, List<ItemTemplate>>> set, BodyPart slot, CrystalType desired, Set<Integer> exclude)
	{
		final EnumMap<CrystalType, List<ItemTemplate>> byGrade = set.get(slot);
		if (byGrade == null)
		{
			return null;
		}
		for (int ordinal = desired.ordinal(); ordinal >= 0; ordinal--)
		{
			final List<ItemTemplate> list = byGrade.get(CrystalType.values()[ordinal]);
			if ((list == null) || list.isEmpty())
			{
				continue;
			}
			if ((exclude == null) || exclude.isEmpty())
			{
				return list.get(Rnd.get(list.size()));
			}
			final List<ItemTemplate> allowed = new ArrayList<>(list.size());
			for (ItemTemplate candidate : list)
			{
				if (!exclude.contains(candidate.getId()))
				{
					allowed.add(candidate);
				}
			}
			if (!allowed.isEmpty())
			{
				return allowed.get(Rnd.get(allowed.size()));
			}
		}
		return null;
	}

	/** Highest weapon grade a phantom of this level may wield (aligned with when Expertise is learned). */
	private static CrystalType gradeForLevel(int level)
	{
		if (level < 20)
		{
			return CrystalType.NONE;
		}
		if (level < 40)
		{
			return CrystalType.D;
		}
		if (level < 52)
		{
			return CrystalType.C;
		}
		if (level < 61)
		{
			return CrystalType.B;
		}
		if (level < 76)
		{
			return CrystalType.A;
		}
		return CrystalType.S;
	}

	/** Soulshot item id matching a weapon grade. */
	private static int soulshotIdFor(CrystalType grade)
	{
		switch (grade)
		{
			case D:
			{
				return 1463;
			}
			case C:
			{
				return 1464;
			}
			case B:
			{
				return 1465;
			}
			case A:
			{
				return 1466;
			}
			case S:
			{
				return 1467;
			}
			default:
			{
				return 1835; // No Grade
			}
		}
	}

	/** Spiritshot item id matching a weapon grade. */
	private static int spiritshotIdFor(CrystalType grade)
	{
		switch (grade)
		{
			case D:
			{
				return 2510;
			}
			case C:
			{
				return 2511;
			}
			case B:
			{
				return 2512;
			}
			case A:
			{
				return 2513;
			}
			case S:
			{
				return 2514;
			}
			default:
			{
				return 2509; // No Grade
			}
		}
	}

	/**
	 * Resolves the cheapest few tradeable items per grade for each gear slot from the datapack, once, into
	 * two loadouts: fighters (sword + LIGHT/HEAVY armor) and mages (magic weapon + MAGIC robe). FULL_ARMOR
	 * is skipped so it never conflicts with the separate legs piece.
	 */
	private static void buildGear()
	{
		if (_gearBuilt)
		{
			return;
		}
		synchronized (FIGHTER_GEAR)
		{
			if (_gearBuilt)
			{
				return;
			}
			initGearMap(FIGHTER_GEAR);
			initGearMap(MAGE_GEAR);
			for (ItemTemplate item : ItemData.getInstance().getAllItems())
			{
				if ((item == null) || !item.isEquipable() || !item.isTradeable() || (item.getReferencePrice() <= 0))
				{
					continue;
				}

				if (item instanceof Weapon)
				{
					// Caster pool = two-handed magic STAVES only (lrhand). Physical blades flagged
					// is_magic_weapon (e.g. Hell Knife dagger) otherwise leak in, and a caster holding a melee
					// blade renders with no chest/legs even when the armor itself is fine (confirmed: Elemental
					// Tunic renders with a staff, vanishes with a Hell Knife). Staves always render with robes.
					if (item.isMagicWeapon() && (item.getBodyPart() == BodyPart.LR_HAND))
					{
						MAGE_GEAR.get(BodyPart.R_HAND).get(item.getCrystalType()).add(item);
					}
					else if (((Weapon) item).getItemType() == WeaponType.SWORD)
					{
						FIGHTER_GEAR.get(BodyPart.R_HAND).get(item.getCrystalType()).add(item);
					}
				}
				else if (item instanceof Armor)
				{
					final BodyPart part = item.getBodyPart();
					if ((part != BodyPart.CHEST) && (part != BodyPart.LEGS) && (part != BodyPart.GLOVES) && (part != BodyPart.FEET) && (part != BodyPart.HEAD))
					{
						continue; // skip FULL_ARMOR (conflicts with separate legs) and non-armor slots
					}
					final ArmorType type = ((Armor) item).getItemType();
					if ((type == ArmorType.LIGHT) || (type == ArmorType.HEAVY))
					{
						FIGHTER_GEAR.get(part).get(item.getCrystalType()).add(item);
					}
					else if (type == ArmorType.MAGIC)
					{
						MAGE_GEAR.get(part).get(item.getCrystalType()).add(item);
					}
				}
			}
			trimGear(FIGHTER_GEAR);
			trimGear(MAGE_GEAR);
			_gearBuilt = true;
		}
	}

	private static void initGearMap(Map<BodyPart, EnumMap<CrystalType, List<ItemTemplate>>> set)
	{
		for (BodyPart slot : GEAR_SLOTS)
		{
			final EnumMap<CrystalType, List<ItemTemplate>> byGrade = new EnumMap<>(CrystalType.class);
			for (CrystalType grade : CrystalType.values())
			{
				byGrade.put(grade, new ArrayList<>());
			}
			set.put(slot, byGrade);
		}
	}

	/** Keeps only the cheapest few per slot/grade, so phantoms roll basic gear, not rare drops. */
	private static void trimGear(Map<BodyPart, EnumMap<CrystalType, List<ItemTemplate>>> set)
	{
		for (EnumMap<CrystalType, List<ItemTemplate>> byGrade : set.values())
		{
			for (List<ItemTemplate> list : byGrade.values())
			{
				list.sort(Comparator.comparingInt(ItemTemplate::getReferencePrice));
				if (list.size() > CANDIDATES_PER_SLOT)
				{
					list.subList(CANDIDATES_PER_SLOT, list.size()).clear();
				}
			}
		}
	}

	/**
	 * Registers the phantom's own learned skills with the native auto-use system: self-buffs go to the
	 * auto-buff list (recast when they drop), offensive actives go to the auto-skill list (cast on the
	 * current target in combat). Classification is by skill metadata, so it is Interlude-data-driven and
	 * needs no hard-coded skill ids.
	 */
	private void registerAutoSkills(Player phantom)
	{
		final AutoUseSettingsHolder autoUse = phantom.getAutoUseSettings();
		for (Skill skill : phantom.getAllSkills())
		{
			if (skill.isPassive() || skill.isToggle())
			{
				continue;
			}
			// Self-buffs: lasting, beneficial, cast on self.
			if (skill.isContinuous() && !skill.isDebuff() && (skill.getEffectPoint() >= 0) && (skill.getTargetType() == TargetType.SELF))
			{
				autoUse.getAutoBuffs().add(skill.getId());
			}
			// Offensive actives: instant (non-continuous) harmful skills used on the target.
			else if (skill.isActive() && !skill.isContinuous() && (skill.getEffectPoint() < 0))
			{
				autoUse.getAutoSkills().add(skill.getId());
			}
		}
	}

	/**
	 * Configures the auto-hunt preferences and starts the native AutoPlay + AutoUse tasks. Soulshots
	 * would be registered here too (via {@code addAutoSoulShot}) once phantoms are geared with a weapon.
	 */
	private void enableAutoHunt(Player phantom, boolean mage)
	{
		final AutoPlaySettingsHolder settings = phantom.getAutoPlaySettings();
		settings.setNextTargetMode(TARGET_MODE_MONSTER);
		// Long-range search so they actively seek mobs across the zone (short range left them standing idle
		// unless a mob aggroed them - AutoPlay doesn't roam beyond its search radius). Clumping is instead
		// held down by spaced spawns + respectful hunting (below).
		settings.setShortRange(false);
		settings.setRespectfulHunting(true); // skip mobs already being fought, so phantoms don't converge on one
		settings.setPickup(true);

		// Fighters auto-attack (action id 2). Mages are pure casters: no auto-attack, so AutoPlay only picks
		// the target and AutoUse casts their nukes - the mage combat tick keeps them at casting range and
		// kites them out when they run low on MP (it does the moving AutoPlay won't do for a caster).
		// Idempotent: this runs again on every re-activation and post-kill resume, so guard against stacking
		// duplicate auto-attack actions on the same phantom.
		if (!mage && !phantom.getAutoUseSettings().getAutoActions().contains(AUTO_ATTACK_ACTION))
		{
			phantom.getAutoUseSettings().getAutoActions().add(AUTO_ATTACK_ACTION);
		}

		AutoPlayTaskManager.getInstance().startAutoPlay(phantom);
		AutoUseTaskManager.getInstance().startAutoUseTask(phantom);
	}

	/** Despawns and forgets every phantom (also deletes their DB rows). */
	public int clear()
	{
		int removed = 0;
		for (PhantomData data : new ArrayList<>(_phantoms.values()))
		{
			despawn(data);
			removed++;
		}
		_phantoms.clear();
		for (Population population : _populations)
		{
			population.active = false;
			population.emptySince = 0;
		}
		return removed;
	}

	/**
	 * Stops the auto-hunt, removes the phantom from the world, forgets it, and - for an ordinary (ephemeral)
	 * phantom - deletes its character row. The row must go for those because phantoms are spawned/despawned on
	 * demand - without deleting it every zone visit would leak an orphan {@code phantom}-account character.
	 * <p>
	 * A persisted regular ({@code account_name='phantom_regular'}) is the opposite: its row is the whole point
	 * (stable charId across reboots for the future friend tier), so it is stored and kept instead - only its
	 * live world/AutoPlay state is torn down.
	 */
	private void despawn(PhantomData data)
	{
		final int objectId = data.player.getObjectId();
		// isRegular (not a raw account check) so a phantom promoted THIS session - whose final in-memory
		// account still reads 'phantom' - is recognized and its row kept too.
		final boolean persistent = isRegular(data.player);
		try
		{
			AutoPlayTaskManager.getInstance().stopAutoPlay(data.player);
			AutoUseTaskManager.getInstance().stopAutoUseTask(data.player);
			if (persistent)
			{
				data.player.storeMe();
			}
			data.player.deleteMe();
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Failed to despawn phantom " + objectId + ": " + e.getMessage());
		}
		_phantoms.remove(objectId);
		_promoted.remove(objectId); // the DB row now carries the regular account; the live-instance bridge is done
		if (persistent)
		{
			return; // keep the row - this regular respawns into the same character next time
		}
		try
		{
			GameClient.deleteCharByObjId(objectId);
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Failed to delete phantom row " + objectId + ": " + e.getMessage());
		}
	}

	/**
	 * Deletes every leftover {@code account_name='phantom'} character row (and its cascade of item/skill/quest/
	 * etc. rows, via {@link GameClient#deleteCharByObjId(int)}) that a previous unclean shutdown left behind.
	 * Runs once at boot from {@link #load()}, before any phantom is spawned, so it only ever removes orphans -
	 * never a live phantom. Same net effect as despawn(), but for whatever survived a hard kill.
	 */
	private void sweepOrphanedPhantoms()
	{
		final List<Integer> orphanIds = new ArrayList<>();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT charId FROM characters WHERE account_name=?"))
		{
			ps.setString(1, ACCOUNT_NAME);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					orphanIds.add(rs.getInt("charId"));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Failed to scan for orphaned phantom rows: " + e.getMessage());
			return;
		}

		for (int objectId : orphanIds)
		{
			try
			{
				GameClient.deleteCharByObjId(objectId);
			}
			catch (Exception e)
			{
				LOGGER.warning(getClass().getSimpleName() + ": Failed to delete orphaned phantom row " + objectId + ": " + e.getMessage());
			}
		}

		if (!orphanIds.isEmpty())
		{
			LOGGER.info(getClass().getSimpleName() + ": Swept " + orphanIds.size() + " orphaned phantom character row(s) left by a previous unclean shutdown.");
		}

		// Second pass: regulars nobody is friends with anymore. A phantom is promoted to the regular account
		// the moment it is befriended; if the player later friend-deletes it (stock RequestFriendDel removes
		// both character_friends directions), its row would otherwise linger forever. XML/auto regulars are
		// recreated deterministically from their seed on next spawn, so sweeping an unbefriended row loses
		// nothing but a stale charId nobody references.
		final List<Integer> friendless = new ArrayList<>();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT charId FROM characters WHERE account_name=? AND NOT EXISTS (SELECT 1 FROM character_friends WHERE friendId = characters.charId)"))
		{
			ps.setString(1, ACCOUNT_NAME_REGULAR);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					friendless.add(rs.getInt("charId"));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Failed to scan for friendless regular rows: " + e.getMessage());
			return;
		}
		for (int objectId : friendless)
		{
			try
			{
				GameClient.deleteCharByObjId(objectId);
			}
			catch (Exception e)
			{
				LOGGER.warning(getClass().getSimpleName() + ": Failed to delete friendless regular row " + objectId + ": " + e.getMessage());
			}
		}
		if (!friendless.isEmpty())
		{
			LOGGER.info(getClass().getSimpleName() + ": Swept " + friendless.size() + " regular row(s) no longer referenced by any friendship.");
		}
	}

	public int getCount()
	{
		return _phantoms.size();
	}

	// ===== Buddy support API (called by PhantomBuddyManager) =====

	/**
	 * Marks/unmarks a buddy as engaged (partied or claimed by a player). An engaged buddy is exempt from the
	 * proximity despawn so it can follow its owner out of the town it spawned in (see {@link #deactivate}).
	 * @return {@code true} if the player is a live buddy phantom and the flag was set
	 */
	public boolean setBuddyEngaged(Player buddy, boolean engaged)
	{
		final PhantomData data = (buddy == null) ? null : _phantoms.get(buddy.getObjectId());
		if ((data == null) || !data.role.isBuddy())
		{
			return false;
		}
		data.buddyEngaged = engaged;
		return true;
	}

	/** @return {@code true} if the player is a live buddy phantom managed by this manager. */
	public boolean isBuddy(Player player)
	{
		final PhantomData data = (player == null) ? null : _phantoms.get(player.getObjectId());
		return (data != null) && data.role.isBuddy();
	}

	/** Despawns a single buddy (used by PhantomBuddyManager when a party ends / owner logs off / grace runs out). */
	public void despawnBuddy(Player buddy)
	{
		final PhantomData data = (buddy == null) ? null : _phantoms.get(buddy.getObjectId());
		if ((data != null) && data.role.isBuddy())
		{
			despawn(data);
		}
	}

	// ===== Recruited party-member API (called by PhantomPartyManager / RequestJoinParty) =====

	/**
	 * Spawns a single recruited combat party member of a given role at a location, brought to {@code level}.
	 * Combat roles get a fixed class (per level tier), gear, learned skills and the native AutoUse task (so they
	 * cast/shot/pot on whatever target the party manager assigns); support roles reuse the buddy outfit and are
	 * cast by hand. The member does NOT auto-hunt and is exempt from the proximity despawn - PhantomPartyManager
	 * owns it from here (walk-in, party-join, follow, assist, disband).
	 * @return the spawned member, or {@code null} on failure (caller should fall back gracefully)
	 */
	public Player spawnPartyMember(Location location, int level, PartyRole role, int overrideClassId)
	{
		if (_phantoms.size() >= MAX_PHANTOMS)
		{
			return null;
		}
		final int groundZ = GeoEngine.getInstance().getHeight(location.getX(), location.getY(), location.getZ());
		final Location spawnLocation = new Location(location.getX(), location.getY(), groundZ);
		try
		{
			final boolean mage = role.mage;
			// A specific requested class (e.g. Shillien Elder) overrides the role's default occupation.
			final int classId = (overrideClassId > 0) ? overrideClassId : (role.isSupport() ? role.supportAs.classId : Math.max(0, roleClassId(role, level)));
			PlayerClass playerClass = PlayerClass.getPlayerClass(classId);
			PlayerTemplate template = (playerClass == null) ? null : PlayerTemplateData.getInstance().getTemplate(playerClass);
			if (template == null) // bad/missing class id: degrade to a plain fighter/mage base
			{
				playerClass = mage ? PlayerClass.getPlayerClass(10) : PlayerClass.FIGHTER;
				template = (playerClass == null) ? null : PlayerTemplateData.getInstance().getTemplate(playerClass);
			}
			if (template == null)
			{
				playerClass = PlayerClass.FIGHTER;
				template = PlayerTemplateData.getInstance().getTemplate(playerClass);
			}
			if (template == null)
			{
				LOGGER.warning(getClass().getSimpleName() + ": No template available for party member role " + role + ".");
				return null;
			}

			final boolean female = Rnd.nextBoolean();
			final PlayerAppearance appearance = new PlayerAppearance((byte) Rnd.get(0, 2), (byte) Rnd.get(0, 3), (byte) Rnd.get(0, 2), female);
			final Player phantom = Player.create(template, ACCOUNT_NAME, nextName(), appearance);
			if (phantom == null)
			{
				LOGGER.warning(getClass().getSimpleName() + ": Player.create returned null for party member (duplicate name / db error?).");
				return null;
			}
			phantom.setDietMode(true); // ignore the weight of the potion/shot stack (see createAndSpawn)
			phantom.setOnlineStatus(true, false);

			if (role.isSupport())
			{
				// The phantom was already created from the right support class template (default or override), so
				// outfitSupport just levels/learns/gears it (no class transfer needed).
				outfitSupport(phantom, level, role);
				if (role == PartyRole.HEALER)
				{
					grantRes(phantom, level); // every healer can raise a fallen party member
				}
			}
			else
			{
				outfitCombat(phantom, level, role);
			}
			phantom.refreshOverloaded();
			enterWorld(phantom, spawnLocation);

			// role=NONE + recruited: skips every hunter path; population=null so the proximity deactivate never
			// touches it. PhantomPartyManager drives it from onMemberSpawned onward.
			final PhantomData data = new PhantomData(phantom, spawnLocation, null, mage, BuddyRole.NONE);
			data.recruited = true;
			_phantoms.put(phantom.getObjectId(), data);

			// Combat roles fire skills/shots/potions through the native AutoUse task on the party manager's
			// assigned target. No AutoPlay here: the manager picks the target (assist), not the engine's scanner.
			if (!role.isSupport())
			{
				if (!mage && !phantom.getAutoUseSettings().getAutoActions().contains(AUTO_ATTACK_ACTION))
				{
					phantom.getAutoUseSettings().getAutoActions().add(AUTO_ATTACK_ACTION);
				}
				// AutoUse only fires offensive skills while the player "is auto-playing" (see AutoUseTaskManager).
				// We set the flag without starting the AutoPlay target-scanner: in assist mode PhantomPartyManager
				// picks the target (the leader's) and AutoUse casts the role's skills + shots on it. Free mode
				// starts the real scanner via setRecruitHunting.
				phantom.setAutoPlaying(true);
				AutoUseTaskManager.getInstance().startAutoUseTask(phantom);
			}
			startSupervising();
			LOGGER.info(getClass().getSimpleName() + ": Spawned " + role + " party member '" + phantom.getName() + "' (objId=" + phantom.getObjectId() + ", level " + level + ").");
			if (PhantomPartyManager.DEBUG)
			{
				logLoadout(phantom, role, level);
			}
			return phantom;
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Failed to spawn party member role " + role + ": " + e.getMessage());
			return null;
		}
	}

	/** @return {@code true} if the player is a live recruited party member managed by PhantomPartyManager. */
	public boolean isRecruit(Player player)
	{
		final PhantomData data = (player == null) ? null : _phantoms.get(player.getObjectId());
		return (data != null) && data.recruited;
	}

	/**
	 * Toggles a recruited member's "free hunt" mode. ON starts the native AutoPlay scanner (the member grabs
	 * its own nearby mobs); OFF stops it so the party manager's assist (focus the leader's target) takes over.
	 */
	public void setRecruitHunting(Player member, boolean hunting)
	{
		final PhantomData data = (member == null) ? null : _phantoms.get(member.getObjectId());
		if ((data == null) || !data.recruited)
		{
			return;
		}
		if (hunting)
		{
			final AutoPlaySettingsHolder settings = member.getAutoPlaySettings();
			settings.setNextTargetMode(TARGET_MODE_MONSTER);
			settings.setShortRange(false);
			settings.setRespectfulHunting(true);
			settings.setPickup(false); // recruited party members never loot - drops are left for the real player
			AutoPlayTaskManager.getInstance().startAutoPlay(member); // also sets isAutoPlaying(true)
		}
		else
		{
			// Back to assist: stop the scanner but KEEP the auto-playing flag so AutoUse still casts the member's
			// skills on the target PhantomPartyManager assigns (stopAutoPlay clears the flag, so re-set it).
			AutoPlayTaskManager.getInstance().stopAutoPlay(member);
			member.setAutoPlaying(true);
		}
	}

	/**
	 * Hands a live befriended regular to {@link PhantomPartyManager} so a party invite can adopt it: stops its
	 * self-directed hunt and mirrors the {@link #spawnPartyMember} runtime state (assist-mode AutoUse for combat
	 * roles, hand-driven casting for supports; {@code recruited} so the supervisor skips every hunter path).
	 * Level and gear are untouched - a friend already spawns with the full party kit. When the party ends the
	 * normal recruit release stores its row and the friend ensure pass respawns it idle within ~15s.
	 * @return the party role derived from its class, or {@code null} if it is not a live phantom
	 */
	public PartyRole adoptFriendForParty(Player friend)
	{
		final PhantomData data = (friend == null) ? null : _phantoms.get(friend.getObjectId());
		if (data == null)
		{
			return null;
		}
		final PartyRole role = roleForClass(friend.getPlayerClass());
		if (data.recruited)
		{
			return role; // already adopted (re-invite within the same party session)
		}
		AutoPlayTaskManager.getInstance().stopAutoPlay(friend);
		if (friend.isSitting())
		{
			friend.standUp();
		}
		data.dormant = false;
		data.resting = false;
		data.dispersing = false;
		if (role.isSupport())
		{
			// Supports are cast by hand from the party tick (heal/buff/res), exactly like recruited healers.
			AutoUseTaskManager.getInstance().stopAutoUseTask(friend);
		}
		else
		{
			if (!data.mage && !friend.getAutoUseSettings().getAutoActions().contains(AUTO_ATTACK_ACTION))
			{
				friend.getAutoUseSettings().getAutoActions().add(AUTO_ATTACK_ACTION);
			}
			// Assist mode: the party manager picks the target; AutoUse casts skills/shots on it (no scanner).
			friend.setAutoPlaying(true);
			AutoUseTaskManager.getInstance().startAutoUseTask(friend);
		}
		data.recruited = true;
		LOGGER.info(getClass().getSimpleName() + ": Friend-regular '" + friend.getName() + "' adopted into a party as " + role + ".");
		return role;
	}

	/** Despawns a recruited member (party disbanded / owner gone / grace elapsed / member dead). */
	public void despawnRecruit(Player member)
	{
		final PhantomData data = (member == null) ? null : _phantoms.get(member.getObjectId());
		if ((data != null) && data.recruited)
		{
			AutoPlayTaskManager.getInstance().stopAutoPlay(member);
			AutoUseTaskManager.getInstance().stopAutoUseTask(member);
			despawn(data);
		}
	}

	/**
	 * Outfits a combat party member: brings it to {@code level}, learns its (already-correct, template-set)
	 * class's full skill tree, gears it, swaps in a bow/dagger for the ranged/rogue roles, and registers its
	 * offensive skills with AutoUse. Unlike {@link #outfit} there is no random class transfer - the member was
	 * created directly from its role's class template.
	 */
	/**
	 * Outfits a recruited support member (healer/buffer) already created from its support class template: brings
	 * it to level, learns the full tree, guarantees a heal, and gives it the caster loadout. Like
	 * {@link #outfitBuddy} but with no class transfer (the class is already correct) - works for both the default
	 * support class and an explicitly requested one (e.g. Shillien Elder).
	 */
	private void outfitSupport(Player phantom, int level, PartyRole role)
	{
		if (level > 1)
		{
			final long currentExp = phantom.getExp();
			final long targetExp = ExperienceData.getInstance().getExpForLevel(level);
			if (targetExp > currentExp)
			{
				phantom.addExpAndSp(targetExp - currentExp, 0);
			}
		}
		learnAllSkills(phantom);
		grantHeal(phantom, level);
		giveBuffReagents(phantom); // Spirit Ore etc. so consumable buffs (Greater Might/Shield, Clarity) actually land
		gearParty(phantom, level, true, role); // full caster loadout + jewelry + enchant chance, so supports survive content
		PhantomBuffs.applyFullBuffs(phantom); // a support arrives self-buffed (Acumen/Empower etc.) too
		phantom.setCurrentHpMp(phantom.getMaxHp(), phantom.getMaxMp());
		phantom.setCurrentCp(phantom.getMaxCp());
	}

	private void outfitCombat(Player phantom, int level, PartyRole role)
	{
		if (level > 1)
		{
			final long currentExp = phantom.getExp();
			final long targetExp = ExperienceData.getInstance().getExpForLevel(level);
			if (targetExp > currentExp)
			{
				phantom.addExpAndSp(targetExp - currentExp, 0);
			}
		}
		learnAllSkills(phantom);
		// Recruited members gear up for real content: best-in-grade role weapon + full armor + jewelry + shield
		// (tank) + a chance of enchant. gearParty picks the role's weapon itself (bow/dagger/staff/sword), so the
		// old separate sword-then-swap step is gone.
		gearParty(phantom, level, role.mage, role);
		PhantomBuffs.applyFullBuffs(phantom); // arrive already buffed for its archetype, so a fresh party isn't unbuffed
		phantom.setCurrentHpMp(phantom.getMaxHp(), phantom.getMaxMp());
		phantom.setCurrentCp(phantom.getMaxCp());
		registerAutoSkills(phantom);
	}

	/** Standard arrow matching a bow's grade (steps down a grade if none exists at the desired one), or null. */
	private static ItemTemplate findArrow(CrystalType desired)
	{
		CrystalType grade = desired;
		while (grade != null)
		{
			for (ItemTemplate item : ItemData.getInstance().getAllItems())
			{
				if ((item instanceof EtcItem) && (((EtcItem) item).getItemType() == EtcItemType.ARROW) && (item.getCrystalType() == grade))
				{
					return item;
				}
			}
			grade = (grade.ordinal() > 0) ? CrystalType.values()[grade.ordinal() - 1] : null;
		}
		return null;
	}

	/** Standard Interlude occupation id for a combat role at its level tier (base / 1st / 2nd class). */
	private static int roleClassId(PartyRole role, int level)
	{
		final int tier = (level < 20) ? 0 : (level < 40) ? 1 : 2;
		switch (role)
		{
			case TANK:
			{
				return new int[]
				{
					0,
					4,
					5
				}[tier]; // Human Fighter / Knight / Paladin
			}
			case WARRIOR:
			{
				return new int[]
				{
					0,
					1,
					2
				}[tier]; // Human Fighter / Warrior / Gladiator
			}
			case ARCHER:
			{
				return new int[]
				{
					0,
					7,
					9
				}[tier]; // Human Fighter / Rogue / Hawkeye
			}
			case DAGGER:
			{
				return new int[]
				{
					0,
					7,
					8
				}[tier]; // Human Fighter / Rogue / Treasure Hunter
			}
			case SINGER:
			{
				return new int[]
				{
					18,
					19,
					21
				}[tier]; // Elven Fighter / Elven Knight / Swordsinger
			}
			case DANCER:
			{
				return new int[]
				{
					31,
					32,
					34
				}[tier]; // Dark Fighter / Palus Knight / Bladedancer
			}
			case NUKER:
			{
				return new int[]
				{
					10,
					11,
					12
				}[tier]; // Human Mage / Wizard / Sorcerer
			}
			default:
			{
				return -1;
			}
		}
	}

	/** Grants Resurrection (1016) to a healer so it can raise dead party members, scaled to its level. */
	private void grantRes(Player phantom, int level)
	{
		if (phantom.getKnownSkill(1016) != null)
		{
			return;
		}
		final int resLevel = Math.max(1, Math.min(9, level / 8));
		final Skill res = SkillData.getInstance().getSkill(1016, resLevel);
		if (res != null)
		{
			phantom.addSkill(res, true);
		}
	}

	private synchronized void startSupervising()
	{
		if (_supervising)
		{
			return;
		}
		_supervising = true;
		ThreadPool.scheduleAtFixedRate(this::supervise, SUPERVISE_INTERVAL, SUPERVISE_INTERVAL);
		ThreadPool.scheduleAtFixedRate(this::mageCombat, MAGE_TICK_INTERVAL, MAGE_TICK_INTERVAL);
		ThreadPool.scheduleAtFixedRate(this::assignTargets, DECONFLICT_INTERVAL, DECONFLICT_INTERVAL);
		LOGGER.info(getClass().getSimpleName() + ": Phantom supervisor started.");
	}

	/**
	 * Keeps awake mage phantoms at casting range of their target and kites them out when low on MP, since
	 * the native AutoPlay never moves a pure caster. AutoUse does the actual nuking once they're in range.
	 */
	private void mageCombat()
	{
		for (PhantomData data : _phantoms.values())
		{
			if (!data.mage || data.role.isBuddy() || data.recruited || data.dormant || data.resting || data.dispersing || (data.huntPauseUntil > 0))
			{
				continue; // buddies/recruits never auto-hunt; otherwise skip if fanning out, on a breather, resting or asleep
			}
			final Player mage = data.player;
			try
			{
				if (mage.isDead() || mage.isCastingNow() || mage.isMovementDisabled())
				{
					continue; // don't interrupt a cast or fight a stun
				}
				// Out of MP: a caster must NOT melee. Disengage and rest to regen; if a mob is on it, back away
				// first and rest once clear. Stop the hunt so AutoPlay doesn't re-target it while it recovers.
				if (mage.getCurrentMpPercent() < MAGE_CAST_MP_PERCENT)
				{
					// Stop the hunt so AutoPlay doesn't re-target it while it recovers.
					if (mage.isAutoPlaying())
					{
						AutoPlayTaskManager.getInstance().stopAutoPlay(mage);
						AutoUseTaskManager.getInstance().stopAutoUseTask(mage);
					}
					mage.setTarget(null);
					if (mage.isInCombat() || isMonsterNear(mage))
					{
						retreatMage(mage); // kite away from the nearest mob; rest once safe
					}
					else
					{
						startRest(mage);
						data.resting = true;
					}
					continue;
				}
				// MP back above the floor: if the OOM handling above had stopped the hunt (recovered while
				// backing off, without ever sitting), resume it now.
				if (!mage.isAutoPlaying())
				{
					enableAutoHunt(mage, true);
				}
				final WorldObject target = mage.getTarget();
				if (!(target instanceof Monster) || ((Monster) target).isDead())
				{
					continue; // AutoPlay will pick a target; nothing to position around yet
				}
				positionMage(mage, (Monster) target);
			}
			catch (Exception e)
			{
				LOGGER.warning(getClass().getSimpleName() + ": Mage combat error for " + mage.getName() + ": " + e.getMessage());
			}
		}
	}

	/** Holds a mage at casting range so AutoUse can nuke. (Out-of-MP handling lives in {@link #mageCombat}.) */
	private void positionMage(Player mage, Monster target)
	{
		double dx = mage.getX() - target.getX();
		double dy = mage.getY() - target.getY();
		double distance = Math.hypot(dx, dy);
		if (Math.abs(distance - MAGE_CAST_RANGE) <= MAGE_RANGE_TOLERANCE)
		{
			return; // already in position - stand still so AutoUse can cast
		}
		if (distance < 1)
		{
			distance = 1;
		}
		// A point MAGE_CAST_RANGE units from the target, on the line toward the mage: walks in when too far,
		// backs off when too close.
		final int standX = target.getX() + (int) ((dx / distance) * MAGE_CAST_RANGE);
		final int standY = target.getY() + (int) ((dy / distance) * MAGE_CAST_RANGE);
		final Location destination = GeoEngine.getInstance().getValidLocation(mage, new Location(standX, standY, mage.getZ()));
		mage.setRunning();
		mage.getAI().setIntention(Intention.MOVE_TO, destination);
	}

	/** Kites a mage one short step directly away from the nearest mob so it can break off and rest when OOM. */
	private void retreatMage(Player mage)
	{
		Monster nearest = null;
		double best = Double.MAX_VALUE;
		for (Monster monster : World.getInstance().getVisibleObjectsInRange(mage, Monster.class, REST_DANGER_RANGE))
		{
			if (monster.isDead())
			{
				continue;
			}
			final double distance = mage.calculateDistance2D(monster);
			if (distance < best)
			{
				best = distance;
				nearest = monster;
			}
		}
		if (nearest == null)
		{
			return;
		}
		double dx = mage.getX() - nearest.getX();
		double dy = mage.getY() - nearest.getY();
		double length = Math.hypot(dx, dy);
		if (length < 1)
		{
			dx = 1;
			dy = 0;
			length = 1;
		}
		final int x = mage.getX() + (int) ((dx / length) * 300);
		final int y = mage.getY() + (int) ((dy / length) * 300);
		final Location destination = GeoEngine.getInstance().getValidLocation(mage, new Location(x, y, mage.getZ()));
		mage.setRunning();
		mage.getAI().setIntention(Intention.MOVE_TO, destination);
	}

	/**
	 * Authoritative target assignment (runs every {@link #DECONFLICT_INTERVAL}). Two passes:
	 * <ol>
	 * <li>Honor each phantom's current valid target, resolving collisions so the closest phantom keeps a mob
	 * and the rest are bumped; detect kills (claimed mob now dead) and start a short post-kill breather.</li>
	 * <li>Give every phantom that has no mob (and is past its breather) the nearest <i>unclaimed</i>, free
	 * monster, claiming it. This is what stops phantoms ganging up: a freed mob is handed to ONE phantom
	 * rather than left for the native AutoPlay scan to re-pick simultaneously.</li>
	 * </ol>
	 * Also stands a resting phantom up immediately if a threat appears, rather than waiting for the slow tick.
	 */
	private void assignTargets()
	{
		final long now = System.currentTimeMillis();
		final Map<Integer, Player> owner = new HashMap<>();
		final List<PhantomData> needsTarget = new ArrayList<>();

		// Pass 1: keep valid targets, resolve collisions, detect kills, manage breathers and rest-on-threat.
		for (PhantomData data : _phantoms.values())
		{
			final Player phantom = data.player;
			try
			{
				if (data.role.isBuddy() || data.recruited || data.dormant || data.dispersing || phantom.isDead())
				{
					continue; // buddies and recruited party members are not part of the hunt/deconflict
				}

				// Resting phantom that just came under threat: stand now, don't wait for the 5s supervise tick.
				if (data.resting)
				{
					if (phantom.isInCombat() || isMonsterNear(phantom))
					{
						endRest(phantom);
						data.resting = false;
						needsTarget.add(data);
					}
					continue;
				}

				// Post-kill breather: a fighter stands still; a mage is walking to its drop. Resume the hunt when
				// the timer elapses, or as soon as a looting mage reaches the corpse (then AutoPlay's pickup,
				// which it prioritises over re-targeting, grabs the drop).
				if (data.huntPauseUntil > 0)
				{
					final boolean reachedLoot = data.mage && (Math.hypot(phantom.getX() - data.lastMobX, phantom.getY() - data.lastMobY) <= LOOT_WALK_RANGE);
					if (reachedLoot || (now >= data.huntPauseUntil))
					{
						data.huntPauseUntil = 0;
						enableAutoHunt(phantom, data.mage);
						needsTarget.add(data);
					}
					continue;
				}

				final WorldObject target = phantom.getTarget();
				final boolean liveMonster = (target instanceof Monster) && !((Monster) target).isDead();

				// Did we lose the mob we owned? If it died, take a breather; if merely lost, re-acquire at once.
				if ((data.claimedOid != 0) && (!liveMonster || (((Monster) target).getObjectId() != data.claimedOid)))
				{
					final WorldObject claimed = World.getInstance().findObject(data.claimedOid);
					final boolean killed = (claimed == null) || ((claimed instanceof Monster) && ((Monster) claimed).isDead());
					data.claimedOid = 0;
					if (killed)
					{
						beginHuntPause(phantom, data, now);
						continue;
					}
				}

				if (!liveMonster)
				{
					needsTarget.add(data);
					continue;
				}
				final Monster monster = (Monster) target;
				// Always defer to a real player fighting this monster.
				if (isContestedByPlayer(monster))
				{
					yieldTarget(phantom);
					data.claimedOid = 0;
					needsTarget.add(data);
					continue;
				}
				// Remember where the mob is, so a caster can walk to its drop after the kill (loot is at the
				// corpse, well outside a mage's cast-range pickup radius).
				data.lastMobX = monster.getX();
				data.lastMobY = monster.getY();
				final int id = monster.getObjectId();
				final Player cur = owner.get(id);
				if (cur == null)
				{
					owner.put(id, phantom);
					data.claimedOid = id;
				}
				else if (phantom.calculateDistance2D(monster) < cur.calculateDistance2D(monster))
				{
					// This phantom is closer: it keeps the mob, the previous owner is bumped to re-acquire.
					yieldTarget(cur);
					final PhantomData curData = _phantoms.get(cur.getObjectId());
					if (curData != null)
					{
						curData.claimedOid = 0;
						needsTarget.add(curData);
					}
					owner.put(id, phantom);
					data.claimedOid = id;
				}
				else
				{
					yieldTarget(phantom);
					data.claimedOid = 0;
					needsTarget.add(data);
				}
			}
			catch (Exception e)
			{
				LOGGER.warning(getClass().getSimpleName() + ": Assign(pass1) error for " + phantom.getName() + ": " + e.getMessage());
			}
		}

		// Pass 2: hand each idle phantom the nearest free, unclaimed monster so they spread over the spawn.
		for (PhantomData data : needsTarget)
		{
			final Player phantom = data.player;
			try
			{
				if (data.dormant || data.dispersing || data.resting || (data.huntPauseUntil > 0) || phantom.isDead())
				{
					continue;
				}
				final Monster mob = nearestFreeMonster(phantom, owner);
				if (mob != null)
				{
					owner.put(mob.getObjectId(), phantom);
					data.claimedOid = mob.getObjectId();
					phantom.setTarget(mob);
					// Fighters engage now; mages just take the target - the mage tick positions and AutoUse nukes.
					if (!data.mage)
					{
						phantom.getAI().setIntention(Intention.ATTACK, mob);
					}
				}
			}
			catch (Exception e)
			{
				LOGGER.warning(getClass().getSimpleName() + ": Assign(pass2) error for " + phantom.getName() + ": " + e.getMessage());
			}
		}
	}

	/** @return the nearest live, auto-attackable monster not already claimed this tick (nor fought by a player). */
	private Monster nearestFreeMonster(Player phantom, Map<Integer, Player> claimed)
	{
		Monster best = null;
		double bestDistance = Double.MAX_VALUE;
		for (Monster monster : World.getInstance().getVisibleObjectsInRange(phantom, Monster.class, AutoPlayConfig.AUTO_PLAY_LONG_RANGE))
		{
			if (monster.isDead() || monster.isAlikeDead() || claimed.containsKey(monster.getObjectId()) || isContestedByPlayer(monster))
			{
				continue;
			}
			if (!monster.isAutoAttackable(phantom) || !GeoEngine.getInstance().canSeeTarget(phantom, monster))
			{
				continue;
			}
			final double distance = phantom.calculateDistance2D(monster);
			if (distance < bestDistance)
			{
				bestDistance = distance;
				best = monster;
			}
		}
		return best;
	}

	/**
	 * Starts a post-kill breather: stops the auto-hunt so the phantom does not instantly chain the next pull.
	 * A fighter stands still for a short random pause (its loot is already underfoot). A mage instead walks to
	 * the corpse it just dropped from cast range, so it ends the pause standing on the loot and the auto-pickup
	 * collects it when the hunt resumes.
	 */
	private void beginHuntPause(Player phantom, PhantomData data, long now)
	{
		if (phantom.isAutoPlaying())
		{
			AutoPlayTaskManager.getInstance().stopAutoPlay(phantom);
			AutoUseTaskManager.getInstance().stopAutoUseTask(phantom);
		}
		phantom.setTarget(null);
		if (data.mage && ((data.lastMobX != 0) || (data.lastMobY != 0)))
		{
			data.huntPauseUntil = now + LOOT_WALK_MAX; // ended early on arrival (see the breather-resume check)
			final Location loot = GeoEngine.getInstance().getValidLocation(phantom, new Location(data.lastMobX, data.lastMobY, phantom.getZ()));
			phantom.setRunning();
			phantom.getAI().setIntention(Intention.MOVE_TO, loot);
		}
		else
		{
			data.huntPauseUntil = now + Rnd.get((int) POST_KILL_DELAY_MIN, (int) POST_KILL_DELAY_MAX);
			phantom.getAI().setIntention(Intention.IDLE);
		}
	}

	/** Begins the initial fan-out: marks the phantom dispersing and sends it toward its area's perimeter. */
	private void beginDisperse(Player phantom, PhantomData data)
	{
		data.dispersing = true;
		data.disperseUntil = System.currentTimeMillis() + DISPERSE_DURATION;
		data.resting = false;
		data.huntPauseUntil = 0;
		data.claimedOid = 0;
		if (phantom.isSitting())
		{
			phantom.standUp();
		}
		disperseMove(phantom, data);
	}

	/** Pushes a phantom outward from its group's centre toward the perimeter (with jitter) so the group fans out. */
	private void disperseMove(Player phantom, PhantomData data)
	{
		final Location center = (data.population != null) ? data.population.center : data.home;
		final int radius = (data.population != null) ? Math.max(ROAM_MIN_DISTANCE + 100, data.population.radius) : ROAM_RADIUS;
		final double dx = phantom.getX() - center.getX();
		final double dy = phantom.getY() - center.getY();
		final double outward = (Math.hypot(dx, dy) < 1) ? (Rnd.nextDouble() * 2 * Math.PI) : Math.atan2(dy, dx);
		Location fallback = null;
		for (int attempt = 0; attempt < ROAM_ATTEMPTS; attempt++)
		{
			// +-45 degrees of jitter around the outward heading so a clustered group spreads in a fan, not a line.
			final double angle = outward + ((Rnd.nextDouble() - 0.5) * (Math.PI / 2));
			final int distance = (int) (radius * (DISPERSE_MIN_RADIUS_PCT + (Rnd.nextDouble() * (1.0 - DISPERSE_MIN_RADIUS_PCT))));
			final int x = center.getX() + (int) (Math.cos(angle) * distance);
			final int y = center.getY() + (int) (Math.sin(angle) * distance);
			final Location destination = GeoEngine.getInstance().getValidLocation(phantom, new Location(x, y, center.getZ()));
			if (fallback == null)
			{
				fallback = destination;
			}
			if (GeoEngine.getInstance().canMoveToTarget(phantom.getX(), phantom.getY(), phantom.getZ(), destination.getX(), destination.getY(), destination.getZ(), phantom.getInstanceId()))
			{
				phantom.setRunning();
				phantom.getAI().setIntention(Intention.MOVE_TO, destination);
				return;
			}
		}
		if (fallback != null)
		{
			phantom.setRunning();
			phantom.getAI().setIntention(Intention.MOVE_TO, fallback);
		}
	}

	/** @return {@code true} if a real, client-connected player is fighting this monster (phantoms must defer). */
	private static boolean isContestedByPlayer(Monster monster)
	{
		// Phantoms/offline players have no client; only a genuine player counts as "the player is hitting it".
		final WorldObject monsterTarget = monster.getTarget();
		if ((monsterTarget instanceof Player) && !((Player) monsterTarget).isInOfflineMode())
		{
			return true;
		}
		// getTarget() alone misses it once a phantom has locked the mob (the mob then targets the phantom), so
		// also honour the aggro list: if any real player has put damage/hate on it, the phantom yields.
		for (Creature attacker : monster.getAggroList().keySet())
		{
			if (attacker.isPlayer() && !attacker.asPlayer().isInOfflineMode())
			{
				return true;
			}
		}
		return false;
	}

	/** Drops a phantom's current target and stops its attack so AutoPlay re-selects a free monster. */
	private void yieldTarget(Player phantom)
	{
		if (phantom.getAI().getIntention() == Intention.ATTACK)
		{
			phantom.getAI().setIntention(Intention.IDLE);
		}
		phantom.setTarget(null);
	}

	private void supervise()
	{
		final long now = System.currentTimeMillis();
		final List<Player> observers = onlineObservers();
		// Spawn populations a player has approached; despawn ones everyone has left (after a grace delay).
		updatePopulations(observers, now);

		// Keep befriended regulars online with their owners (self-healing: respawns one that died, was
		// despawned with a deactivating population, or failed an earlier spawn). Throttled - it's a cheap
		// in-memory check, but there is no need to run it every tick.
		if ((now - _lastFriendEnsure) >= 15000)
		{
			_lastFriendEnsure = now;
			for (Map.Entry<Integer, Set<Integer>> entry : _friendRegularsByOwner.entrySet())
			{
				final Player owner = World.getInstance().getPlayer(entry.getKey());
				if (owner != null)
				{
					ensureFriendRegulars(entry.getKey(), owner);
				}
			}
			// Materialize any editor-authored <friend> orders whose owner is online.
			if (!_craftedFriends.isEmpty())
			{
				for (Player observer : observers)
				{
					materializeCraftedFriends(observer);
				}
			}
		}
		for (PhantomData data : new ArrayList<>(_phantoms.values()))
		{
			final Player phantom = data.player;
			try
			{
				// Forget phantoms that left the world for good.
				if (World.getInstance().findObject(phantom.getObjectId()) == null)
				{
					_phantoms.remove(phantom.getObjectId());
					continue;
				}

				// Recruited combat party members are driven entirely by PhantomPartyManager (follow / assist /
				// support / sustain). They never hunt, rest, roam or proximity-despawn on their own.
				if (data.recruited)
				{
					PhantomPartyManager.getInstance().supervise(phantom);
					continue;
				}

				// Buddies do not hunt/rest/roam: the PhantomBuddyManager owns their behaviour (idle self-buff,
				// and - once partied - buffing/healing/following the owner). Skip all the hunter logic below.
				if (data.role.isBuddy())
				{
					PhantomBuddyManager.getInstance().supervise(phantom);
					continue;
				}

				if (phantom.isDead())
				{
					if (data.deadSince == 0)
					{
						data.deadSince = now;
						// Population phantoms: despawn the corpse now and replace it with a fresh identity
						// shortly after, so the zone stays populated and gear/name rotate on each death.
						if ((data.population != null) && data.population.respawn)
						{
							final Population population = data.population;
							despawn(data);
							// Only replace it if the zone is still active when the delay elapses (a player could
							// have left and the population deactivated in the meantime).
							ThreadPool.schedule(() ->
							{
								if (population.active)
								{
									deployOne(population);
								}
							}, RESPAWN_DELAY);
						}
						else if (data.population != null)
						{
							// Population explicitly configured respawn="false": the death is permanent - despawn
							// and do not replace or revive it (previously fell through to the ad-hoc revive branch
							// below and stood back up anyway, ignoring respawn="false").
							despawn(data);
						}
					}
					else if ((data.population == null) && ((now - data.deadSince) >= RESPAWN_DELAY))
					{
						// Ad-hoc (admin test) phantom only: revive it in place so the test count stays stable.
						revive(data);
					}
					continue;
				}

				// Alive again / still alive: clear the death stamp.
				data.deadSince = 0;

				// Proximity dormancy: only compute the auto-hunt while a real player is near (hysteresis).
				final double nearest = nearestObserverDistance(phantom, observers);
				if (data.dormant)
				{
					if (nearest <= WAKE_RANGE)
					{
						data.dormant = false;
						data.resting = false;
						data.huntPauseUntil = 0;
						// Re-spread on re-approach too, then hunt - same reason as the initial spawn.
						beginDisperse(phantom, data);
					}
					continue; // stay parked while no one is near
				}
				if (nearest > SLEEP_RANGE)
				{
					sleep(phantom);
					data.dormant = true;
					data.resting = false;
					continue;
				}

				// Initial dispersal: fan out, then switch the auto-hunt on once the timer elapses.
				if (data.dispersing)
				{
					if (now >= data.disperseUntil)
					{
						data.dispersing = false;
						enableAutoHunt(phantom, data.mage);
					}
					continue; // don't hunt/rest/roam while still spreading out
				}

				// Post-kill breather is managed by assignTargets (1s): leave it standing, don't roam it here.
				if (data.huntPauseUntil > 0)
				{
					continue;
				}

				// Resting: when safe and low on HP/MP, sit to regen; stand when recovered or threatened.
				final boolean danger = phantom.isInCombat() || hasLiveMonsterTarget(phantom) || isMonsterNear(phantom);
				final boolean active = phantom.isMoving() || phantom.isCastingNow() || phantom.isAttackingNow();
				if (data.resting)
				{
					if (danger || ((phantom.getCurrentHpPercent() >= REST_STAND_PERCENT) && (phantom.getCurrentMpPercent() >= REST_MP_STAND_PERCENT)))
					{
						endRest(phantom);
						data.resting = false;
					}
				}
				else if (!danger && ((phantom.getCurrentHpPercent() < REST_SIT_PERCENT) || (phantom.getCurrentMpPercent() < REST_MP_SIT_PERCENT)))
				{
					startRest(phantom);
					data.resting = true;
				}
				else if (active)
				{
					data.idleTargetTicks = 0;
				}
				else if (hasLiveMonsterTarget(phantom))
				{
					data.idleTargetTicks++;
					if (data.idleTargetTicks >= IDLE_TARGET_CLEAR_TICKS)
					{
						phantom.setTarget(null);
						data.idleTargetTicks = 0;
						if (data.mage && (phantom.getCurrentMpPercent() < MAGE_CAST_MP_PERCENT) && !phantom.isInCombat() && !isMonsterNear(phantom))
						{
							startRest(phantom);
							data.resting = true;
						}
						else
						{
							roam(phantom, data);
						}
					}
				}
				// Roam when idle: nothing to fight, not already walking and not resting. Relocating it lets the
				// next AutoPlay scan pick up monsters it could not previously reach, so it stops standing still.
				else if (!data.resting && !danger && (phantom.getTarget() == null))
				{
					data.idleTargetTicks = 0;
					roam(phantom, data);
				}
			}
			catch (Exception e)
			{
				LOGGER.warning(getClass().getSimpleName() + ": Supervise error for " + phantom.getName() + ": " + e.getMessage());
			}
		}
	}

	/** @return {@code true} if the phantom currently has a living monster target (engaged - don't rest). */
	private static boolean hasLiveMonsterTarget(Player phantom)
	{
		final WorldObject target = phantom.getTarget();
		return (target instanceof Monster) && !((Monster) target).isDead();
	}

	/**
	 * Walks an idle phantom to a random reachable spot inside its home area, so the next native AutoPlay
	 * scan can find monsters it could not previously reach. Without this a phantom that runs out of nearby
	 * mobs just stands still, because AutoPlay never roams beyond its own search radius.
	 */
	private void roam(Player phantom, PhantomData data)
	{
		final int radius = (data.population != null) ? Math.max(ROAM_MIN_DISTANCE + 100, data.population.radius) : ROAM_RADIUS;
		Location fallback = null;
		for (int attempt = 0; attempt < ROAM_ATTEMPTS; attempt++)
		{
			final double angle = Rnd.nextDouble() * 2 * Math.PI;
			final int distance = Rnd.get(ROAM_MIN_DISTANCE, radius);
			final int x = data.home.getX() + (int) (Math.cos(angle) * distance);
			final int y = data.home.getY() + (int) (Math.sin(angle) * distance);
			final Location destination = GeoEngine.getInstance().getValidLocation(phantom, new Location(x, y, data.home.getZ()));
			if (fallback == null)
			{
				fallback = destination;
			}
			if (GeoEngine.getInstance().canMoveToTarget(phantom.getX(), phantom.getY(), phantom.getZ(), destination.getX(), destination.getY(), destination.getZ(), phantom.getInstanceId()))
			{
				phantom.setRunning();
				phantom.getAI().setIntention(Intention.MOVE_TO, destination);
				return;
			}
		}
		if (fallback != null)
		{
			phantom.setRunning();
			phantom.getAI().setIntention(Intention.MOVE_TO, fallback);
		}
	}

	/** @return {@code true} if a live monster is close enough that the phantom should not sit to rest. */
	private static boolean isMonsterNear(Player phantom)
	{
		for (Monster monster : World.getInstance().getVisibleObjectsInRange(phantom, Monster.class, REST_DANGER_RANGE))
		{
			if (!monster.isDead())
			{
				return true;
			}
		}
		return false;
	}

	/** Sits the phantom down to regenerate, pausing the auto-hunt while it rests. */
	private void startRest(Player phantom)
	{
		// Stop both unconditionally: the OOM-mage path may have already stopped AutoPlay on its own, and a
		// sitting phantom must not keep an AutoUse task trying to act.
		if (phantom.isAutoPlaying())
		{
			AutoPlayTaskManager.getInstance().stopAutoPlay(phantom);
		}
		AutoUseTaskManager.getInstance().stopAutoUseTask(phantom);
		if (!phantom.isSitting())
		{
			// sitDown(false): a phantom mage's lingering cast flag would otherwise hit the "Cannot sit while
			// casting" guard in sitDown(true) and never rest.
			phantom.abortCast();
			phantom.sitDown(false);
		}
	}

	/** Stands the phantom back up and resumes the auto-hunt after resting. */
	private void endRest(Player phantom)
	{
		if (phantom.isSitting())
		{
			phantom.standUp();
		}
		wake(phantom);
	}

	/** Genuine, client-connected players (excludes phantoms and offline shops, which have no client). */
	private static List<Player> onlineObservers()
	{
		final List<Player> observers = new ArrayList<>();
		for (Player player : World.getInstance().getPlayers())
		{
			if (!player.isInOfflineMode() && !player.isDead())
			{
				observers.add(player);
			}
		}
		return observers;
	}

	/** Distance to the closest observer, or {@code MAX_VALUE} if there are none. */
	private static double nearestObserverDistance(Player phantom, List<Player> observers)
	{
		double best = Double.MAX_VALUE;
		for (Player observer : observers)
		{
			final double distance = phantom.calculateDistance2D(observer);
			if (distance < best)
			{
				best = distance;
			}
		}
		return best;
	}

	/** Resumes the native auto-hunt for a phantom a real player has approached. */
	private void wake(Player phantom)
	{
		if (phantom.isSitting())
		{
			phantom.standUp();
		}
		phantom.setRunning();
		if (!phantom.isAutoPlaying())
		{
			AutoPlayTaskManager.getInstance().startAutoPlay(phantom);
			AutoUseTaskManager.getInstance().startAutoUseTask(phantom);
		}
	}

	/** Pauses the auto-hunt and freezes a phantom that no real player is near (saves the per-tick cost). */
	private void sleep(Player phantom)
	{
		if (phantom.isAutoPlaying())
		{
			AutoPlayTaskManager.getInstance().stopAutoPlay(phantom);
			AutoUseTaskManager.getInstance().stopAutoUseTask(phantom);
		}
		phantom.getAI().setIntention(Intention.IDLE); // stop any in-progress movement so it parks
	}

	/** Brings a dead phantom back to full health at its home spot and resumes the auto-hunt. */
	private void revive(PhantomData data)
	{
		final Player phantom = data.player;
		phantom.doRevive();
		phantom.setCurrentHp(phantom.getMaxHp());
		phantom.setCurrentMp(phantom.getMaxMp());
		phantom.setCurrentCp(phantom.getMaxCp());
		phantom.teleToLocation(data.home);
		phantom.setRunning();
		data.deadSince = 0;
		// AutoPlay keeps the player pooled across death; only restart if it somehow dropped out.
		if (!phantom.isAutoPlaying())
		{
			AutoPlayTaskManager.getInstance().startAutoPlay(phantom);
			AutoUseTaskManager.getInstance().startAutoUseTask(phantom);
		}
	}

	/** A unique, pronounceable character name (reuses the NPC name generator), checked against the DB. */
	private String nextName()
	{
		for (int attempt = 0; attempt < 50; attempt++)
		{
			final String name = FakePlayerAppearanceFactory.generateName();
			if (!CharInfoTable.getInstance().doesCharNameExist(name))
			{
				return name;
			}
		}
		// Extremely unlikely fallback.
		String name;
		do
		{
			name = "Phantom" + Rnd.get(100000);
		}
		while (CharInfoTable.getInstance().doesCharNameExist(name));
		return name;
	}

	public static PhantomManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final PhantomManager INSTANCE = new PhantomManager();
	}
}
