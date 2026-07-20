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

import java.util.Random;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.actor.holders.npc.FakePlayerAppearance;

/**
 * Procedurally generates {@link FakePlayerAppearance}s: unique pronounceable names plus a random but
 * coherent race / gender / class / look. Gear slots are intentionally left empty for now (wiring in
 * verified Interlude item display ids is a separate, data-driven step) but the appearance object
 * already supports them via {@code setArmor}/{@code setWeapon}.
 */
public class FakePlayerAppearanceFactory
{
	// First-occupation (root) class ids per race for Interlude.
	private static final int[] HUMAN_CLASSES =
	{
		0, // Human Fighter
		10 // Human Mystic (Mage)
	};
	private static final int[] ELF_CLASSES =
	{
		18, // Elven Fighter
		25 // Elven Mystic
	};
	private static final int[] DARK_ELF_CLASSES =
	{
		31, // Dark Fighter
		38 // Dark Mystic
	};
	private static final int[] ORC_CLASSES =
	{
		44, // Orc Fighter
		49 // Orc Mystic
	};
	private static final int[] DWARF_CLASSES =
	{
		53 // Dwarven Fighter
	};

	// ===== Curated gear sets (verified item ids from the datapack), tiered by level. =====
	// Weapons.
	private static final int[] NG_FIGHTER_WEAPONS =
	{
		1, 2, 5, 12, 68, 159 // Short/Long Sword, Mace, Knife, Falchion, Bonebreaker
	};
	private static final int[] NG_MAGE_WEAPONS =
	{
		6, 8, 747 // Apprentice's Wand, Willow Staff, Wand of Adept
	};
	private static final int[] D_FIGHTER_WEAPONS =
	{
		129, 125, 127, 268, 72 // Sword of Revolution, Spinebone, Crimson, Bellion Cestus, Stormbringer
	};
	private static final int[] D_MAGE_WEAPONS =
	{
		189, 200, 195 // Staff of Life, Sage's Staff, Cursed Staff
	};
	private static final int[] C_FIGHTER_WEAPONS =
	{
		171, 75, 84 // Deadman's Glory, Caliburs, Homunkulus's Sword
	};
	private static final int[] C_MAGE_WEAPONS =
	{
		2373, 200 // Eldritch Staff, Sage's Staff
	};

	// Armor: {chest options} plus single legs/head/gloves/feet per tier.
	private static final int[] NG_CHEST =
	{
		22, 1101, 352 // Leather Shirt, Tunic of Devotion, Brigandine Tunic
	};
	private static final int NG_LEGS = 29; // Leather Pants
	private static final int NG_HEAD = 43; // Wooden Helmet
	private static final int NG_GLOVES = 50; // Leather Gloves
	private static final int NG_FEET = 37; // Leather Shoes

	private static final int[] D_CHEST =
	{
		400, 356 // Theca Leather Armor, Full Plate Armor
	};
	private static final int[] D_LEGS =
	{
		417, 380 // Manticore Skin Gaiters, Plate Gaiters
	};
	private static final int D_HEAD = 2411; // Brigandine Helmet
	private static final int D_FEET = 2425; // Brigandine Boots

	private static final int[] C_CHEST =
	{
		437 // Mithril Tunic
	};
	private static final int C_LEGS = 59; // Mithril Gaiters
	private static final int C_HEAD = 2412; // Plate Helmet
	private static final int C_FEET = 2428; // Plate Boots

	// Races eligible for generated bots, weighted by repetition (humans most common, dwarves least).
	private static final Race[] RACE_POOL =
	{
		Race.HUMAN, Race.HUMAN, Race.HUMAN,
		Race.ELF, Race.ELF,
		Race.DARK_ELF, Race.DARK_ELF,
		Race.ORC,
		Race.DWARF
	};

	// Believable private-store titles shown above seated vendors. The store-type icon already marks a shop as
	// buying or selling, so these carry no "WTS"/"WTB" tag - the buy signs just phrase the demand ("buying ...").
	private static final String[] SELL_TITLES =
	{
		"items pm", "D grade cheap", "C grade", "soulshots", "spiritshots",
		"mats cheap", "adena items", "cheap stuff inside", "armor parts", "recipes",
		"keymats", "clearance sale", "top D", "weapon", "jewels"
	};
	private static final String[] BUY_TITLES =
	{
		"buying adena", "buying D grade", "buying mats", "buying soulshots", "buying recipes",
		"buying keymats", "buying C grade", "buying enchant scrolls", "buying proofs", "buying gemstones"
	};
	private static final String[] CRAFT_TITLES =
	{
		"crafting D grade", "crafting C grade", "armor craft", "weapon craft", "free craft pm mats",
		"craft service", "making shots", "craft cheap", "mass production"
	};

	private FakePlayerAppearanceFactory()
	{
	}

	/**
	 * Generates a fresh appearance with a random level in the given (inclusive) range.
	 * @param minLevel lowest level to roll
	 * @param maxLevel highest level to roll
	 * @return a populated {@link FakePlayerAppearance}
	 */
	public static FakePlayerAppearance generate(int minLevel, int maxLevel)
	{
		return generate(minLevel, maxLevel, null);
	}

	/**
	 * Generates a fresh appearance, optionally biased toward a dominant race (e.g. a Dwarven village).
	 * @param minLevel lowest level to roll
	 * @param maxLevel highest level to roll
	 * @param dominantRace if not {@code null}, most bots will be this race
	 * @return a populated {@link FakePlayerAppearance}
	 */
	public static FakePlayerAppearance generate(int minLevel, int maxLevel, Race dominantRace)
	{
		return generate(minLevel, maxLevel, dominantRace, false);
	}

	/**
	 * Generates a fresh appearance, optionally biased toward (or locked to) a given race.
	 * @param minLevel lowest level to roll
	 * @param maxLevel highest level to roll
	 * @param dominantRace if not {@code null}, bots will be this race (biased, or guaranteed if forced)
	 * @param forceRace {@code true} to make every bot {@code dominantRace} (e.g. dwarf-only crafters)
	 * @return a populated {@link FakePlayerAppearance}
	 */
	public static FakePlayerAppearance generate(int minLevel, int maxLevel, Race dominantRace, boolean forceRace)
	{
		final Race race = (dominantRace != null) && (forceRace || (Rnd.get(100) < 82)) ? dominantRace : RACE_POOL[Rnd.get(RACE_POOL.length)];
		final boolean female = (race != Race.ORC) && (race != Race.DWARF) ? Rnd.nextBoolean() : Rnd.get(100) < 30; // orcs/dwarves skew male
		final int classId = classForRace(race)[Rnd.get(classForRace(race).length)];
		final PlayerClass playerClass = PlayerClass.getPlayerClass(classId);

		final FakePlayerAppearance look = new FakePlayerAppearance();
		look.setName(generateName());
		look.setRace(race);
		look.setFemale(female);
		if (playerClass != null)
		{
			look.setPlayerClass(playerClass);
		}
		look.setLevel(Rnd.get(minLevel, maxLevel));
		look.setHairStyle(Rnd.get(0, 3));
		look.setHairColor(Rnd.get(0, 3));
		look.setFace(Rnd.get(0, 2));
		equip(look, playerClass != null && playerClass.isMage());
		return look;
	}

	/**
	 * Dresses a generated bot in a coherent gear set for its level tier, branched by fighter/mage.
	 * Some slots are intentionally left empty at random so the crowd is not uniformly fully geared.
	 * @param look the appearance to equip
	 * @param mage {@code true} for a caster loadout (staff/wand), {@code false} for melee
	 */
	private static void equip(FakePlayerAppearance look, boolean mage)
	{
		// The tier a bot's level would allow.
		final int level = look.getLevel();
		final int levelTier = level >= 40 ? 2 : level >= 20 ? 1 : 0;

		// "Wealth" spreads gear grades within a same-level crowd so a cluster is not all in identical
		// top gear: most people are poor/average and only a minority are fully kitted for their level.
		final int wealth = Rnd.get(100);
		final int tier;
		if (wealth < 40)
		{
			tier = 0; // poor: plain no-grade regardless of level
		}
		else if (wealth < 85)
		{
			tier = Math.max(0, levelTier - Rnd.get(0, 1)); // average: their tier, often a grade lower
		}
		else
		{
			tier = levelTier; // well-off: full tier for their level
		}

		// Weapon: most carry one, some go unarmed; a few show a small enchant.
		if (Rnd.get(100) < 88)
		{
			final int[] weapons = mage ? (tier == 2 ? C_MAGE_WEAPONS : tier == 1 ? D_MAGE_WEAPONS : NG_MAGE_WEAPONS) : (tier == 2 ? C_FIGHTER_WEAPONS : tier == 1 ? D_FIGHTER_WEAPONS : NG_FIGHTER_WEAPONS);
			look.setWeapon(weapons[Rnd.get(weapons.length)], 0);
			if ((tier > 0) && (Rnd.get(100) < 12))
			{
				look.setWeaponEnchantLevel(Rnd.get(1, 4));
			}
		}

		// Armor pieces for the rolled tier; chest is picked here, the rest are tier constants.
		final int chest;
		final int legs;
		final int head;
		final int gloves;
		final int feet;
		switch (tier)
		{
			case 2:
			{
				chest = C_CHEST[Rnd.get(C_CHEST.length)];
				legs = C_LEGS;
				head = C_HEAD;
				feet = C_FEET;
				gloves = NG_GLOVES;
				break;
			}
			case 1:
			{
				chest = D_CHEST[Rnd.get(D_CHEST.length)];
				legs = D_LEGS[Rnd.get(D_LEGS.length)];
				head = D_HEAD;
				feet = D_FEET;
				gloves = NG_GLOVES;
				break;
			}
			default:
			{
				chest = NG_CHEST[Rnd.get(NG_CHEST.length)];
				legs = NG_LEGS;
				head = NG_HEAD;
				feet = NG_FEET;
				gloves = NG_GLOVES;
				break;
			}
		}

		// Completeness varies a lot: most wear a chest, fewer wear legs, helmets/gloves are uncommon
		// (so we don't get a cluster of identical fully-armored clones).
		look.setArmor(Rnd.get(100) < 40 ? head : 0, Rnd.get(100) < 88 ? chest : 0, Rnd.get(100) < 70 ? legs : 0, Rnd.get(100) < 38 ? gloves : 0, Rnd.get(100) < 75 ? feet : 0, 0);
	}

	/**
	 * @param kind store kind: BUY, CRAFT/MANUFACTURE, or anything else (sell)
	 * @return a random believable store title for that kind
	 */
	public static String storeTitle(String kind)
	{
		if ("BUY".equalsIgnoreCase(kind))
		{
			return BUY_TITLES[Rnd.get(BUY_TITLES.length)];
		}
		if ("CRAFT".equalsIgnoreCase(kind) || "MANUFACTURE".equalsIgnoreCase(kind))
		{
			return CRAFT_TITLES[Rnd.get(CRAFT_TITLES.length)];
		}
		return SELL_TITLES[Rnd.get(SELL_TITLES.length)];
	}

	private static int[] classForRace(Race race)
	{
		switch (race)
		{
			case ELF:
			{
				return ELF_CLASSES;
			}
			case DARK_ELF:
			{
				return DARK_ELF_CLASSES;
			}
			case ORC:
			{
				return ORC_CLASSES;
			}
			case DWARF:
			{
				return DWARF_CLASSES;
			}
			default:
			{
				return HUMAN_CLASSES;
			}
		}
	}

	/**
	 * @return a unique pronounceable name (max 16 chars, as the client expects)
	 */
	public static String generateName()
	{
		return FakePlayerNameFactory.generateName();
	}

	/**
	 * Deterministic name variant built from a caller-supplied seeded RNG, so the same seed always yields the
	 * same name. Used for stable "regular" identities that must be identical across restarts (unlike the
	 * random {@link #generateName()}).
	 * @param rng a seeded random source (same seed -&gt; same name)
	 * @return a syllable-built name (from the same pools as the random generator), capped at 16 chars
	 */
	public static String generateName(Random rng)
	{
		return FakePlayerNameFactory.generateName(rng);
	}
}
