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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.Weapon;
import org.l2jmobius.gameserver.model.item.enums.BodyPart;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.item.type.WeaponType;

/**
 * Weapons for the melee occupations whose skills span more than one weapon family.
 * <ul>
 * <li>A field phantom rolls one of its class's main weapons and never carries a polearm spare.</li>
 * <li>An Olympiad noble always carries the class's fixed Olympiad weapon.</li>
 * <li>A party member (a recruit or an invited friend) rolls a main weapon and also carries a spare, which the leader
 * can have it switch to in party chat ("switch to polearm", "use your blunt", "switch back", "weapons?").</li>
 * </ul>
 * The playstyle engine already skips any entry whose weapon condition fails, so a playstyle can list the skills of
 * both weapons and each one fires only while its weapon is in hand. Spare weapons weigh nothing that matters: every
 * phantom runs in diet mode.
 * <p>
 * {@code research/audit_class_skills.py} reads {@link #SETS} and {@link WeaponKind} from this file, and
 * {@code research/validate_playstyles.py} keeps a copy pinned to it by a test, so keep one set per line.
 */
public final class PhantomWeaponSets
{
	/** Where a phantom is geared: it decides which weapon it rolls and whether it carries a spare. */
	public enum GearContext
	{
		SOLO,
		PARTY,
		OLYMPIAD
	}

	/** A physical weapon family, with the hand it must fill when that matters (two-handed sword versus one-handed). */
	public enum WeaponKind
	{
		DUAL_SWORDS("dual swords", WeaponType.DUAL, null),
		POLEARM("polearm", WeaponType.POLE, null),
		TWO_HANDED_SWORD("two-handed sword", WeaponType.SWORD, BodyPart.LR_HAND),
		TWO_HANDED_BLUNT("two-handed blunt", WeaponType.BLUNT, BodyPart.LR_HAND),
		BLUNT("blunt", WeaponType.BLUNT, BodyPart.R_HAND);

		private final String _label;
		private final WeaponType _type;
		private final BodyPart _hand;

		WeaponKind(String label, WeaponType type, BodyPart hand)
		{
			_label = label;
			_type = type;
			_hand = hand;
		}

		public String label()
		{
			return _label;
		}

		public boolean matches(ItemTemplate item)
		{
			return (item instanceof Weapon) && !item.isMagicWeapon() && (((Weapon) item).getItemType() == _type) && ((_hand == null) || (item.getBodyPart() == _hand));
		}
	}

	/**
	 * One occupation line, matched by a class name key or, when it has none, by race.
	 * @param names class name keys (lower case), empty for a race match
	 * @param race the race matched when {@code names} is empty
	 * @param main the main weapons, one rolled per phantom (field and party)
	 * @param olympiad the fixed weapon of an Olympiad noble
	 * @param spare the second weapon a party member carries
	 */
	private record WeaponSet(List<String> names, Race race, List<WeaponKind> main, WeaponKind olympiad, WeaponKind spare)
	{
		boolean appliesTo(PlayerClass playerClass)
		{
			if (names.isEmpty())
			{
				return playerClass.getRace() == race;
			}
			final String name = playerClass.name().toLowerCase();
			for (String key : names)
			{
				if (name.contains(key))
				{
					return true;
				}
			}
			return false;
		}
	}

	// Checked in order. Only these lines have a real second weapon; every other class keeps its single role weapon.
	private static final List<WeaponSet> SETS = List.of( //
		new WeaponSet(List.of("gladiator", "duelist"), null, List.of(WeaponKind.DUAL_SWORDS), WeaponKind.DUAL_SWORDS, WeaponKind.BLUNT), //
		new WeaponSet(List.of("warlord", "dreadnought"), null, List.of(WeaponKind.POLEARM), WeaponKind.POLEARM, WeaponKind.BLUNT), //
		new WeaponSet(List.of("raider", "destroyer", "titan"), null, List.of(WeaponKind.TWO_HANDED_SWORD, WeaponKind.TWO_HANDED_BLUNT), WeaponKind.TWO_HANDED_BLUNT, WeaponKind.POLEARM), //
		new WeaponSet(List.of(), Race.DWARF, List.of(WeaponKind.BLUNT), WeaponKind.BLUNT, WeaponKind.POLEARM));

	/** The Dwarven Fighter base class matches the dwarf race line but gets no set: a recruit's weapon there follows its requested role. */
	private static final Set<String> BASE_CLASSES = Set.of("dwarven_fighter");

	private static final long EQUIP_RETRY_MS = 250;
	private static final int EQUIP_MAX_TRIES = 40;

	private PhantomWeaponSets()
	{
	}

	private static WeaponSet find(PlayerClass playerClass)
	{
		if ((playerClass == null) || BASE_CLASSES.contains(playerClass.name().toLowerCase()))
		{
			return null;
		}
		for (WeaponSet set : SETS)
		{
			if (set.appliesTo(playerClass))
			{
				return set;
			}
		}
		return null;
	}

	/**
	 * @return the main weapon this class carries in {@code context}, rolled among its main weapons outside the
	 *         Olympiad, or {@code null} when the class has no weapon set (its role weapon applies)
	 */
	public static WeaponKind mainKind(PlayerClass playerClass, GearContext context)
	{
		final WeaponSet set = find(playerClass);
		if (set == null)
		{
			return null;
		}
		if (context == GearContext.OLYMPIAD)
		{
			return set.olympiad;
		}
		return set.main.get(Rnd.get(set.main.size()));
	}

	/** @return the spare weapon a party member of this class carries, or {@code null} (no set, or not a party context) */
	public static WeaponKind spareKind(PlayerClass playerClass, GearContext context)
	{
		final WeaponSet set = find(playerClass);
		return ((set == null) || (context != GearContext.PARTY)) ? null : set.spare;
	}

	/**
	 * Party chat weapon orders for one member. The member answers only when it carries more than one weapon, or when
	 * it was addressed by name.
	 * @param npc the party member
	 * @param text the leader's line, lower case
	 * @param addressed whether the member was named (a refusal is said only then, so a party-wide order stays quiet)
	 * @return the reply to say, {@code ""} to act without a reply, or {@code null} when the line is not a weapon order
	 *         for this member
	 */
	public static String order(Player npc, String text, boolean addressed)
	{
		final List<String> words = Arrays.asList(text.replaceAll("[^a-z ]", " ").trim().split("\\s+"));
		final List<Item> carried = carriedWeapons(npc);
		if (listRequest(text, words))
		{
			if (carried.size() > 1)
			{
				return describe(carried);
			}
			return (addressed && (carried.size() == 1)) ? "just my " + familyLabel(carried.get(0)) : null;
		}
		final boolean swap = hasAny(words, "switch", "swap", "change");
		if (!swap && !hasAny(words, "use", "equip", "grab", "wield"))
		{
			return null;
		}
		// "switch back" (a short line, so "switch back to assist" stays an assist order) or "use your main weapon".
		final boolean back = (swap && hasAny(words, "back") && ((words.size() <= 3) || hasAny(words, "weapon"))) || (hasAny(words, "main", "usual", "normal") && hasAny(words, "weapon"));
		final WeaponType wanted = requestedType(words);
		if ((wanted == null) && !back)
		{
			return null;
		}
		if (carried.size() < 2)
		{
			return (addressed && (wanted != null)) ? "i only have my " + (carried.isEmpty() ? "hands" : familyLabel(carried.get(0))) : null;
		}
		final Item target = back ? mainWeapon(npc, carried) : findCarried(carried, wanted);
		if (target == null)
		{
			return addressed ? "i don't carry a " + typeLabel(wanted) : null;
		}
		if (target.isEquipped())
		{
			return "already using my " + familyLabel(target);
		}
		equip(npc, target, 0);
		return "switching to my " + familyLabel(target);
	}

	private static boolean listRequest(String text, List<String> words)
	{
		return text.startsWith("weapons") || ((hasAny(words, "weapons") || hasAny(words, "weapon")) && hasAny(words, "what", "which", "carry", "have", "got"));
	}

	private static WeaponType requestedType(List<String> words)
	{
		if (hasAny(words, "polearm", "pole", "polearms", "spear", "lance", "halberd", "glaive"))
		{
			return WeaponType.POLE;
		}
		if (hasAny(words, "dual", "duals", "dualsword", "dualswords", "daisho"))
		{
			return WeaponType.DUAL;
		}
		if (hasAny(words, "blunt", "hammer", "mace", "club", "blunts"))
		{
			return WeaponType.BLUNT;
		}
		if (hasAny(words, "sword", "swords", "greatsword", "bigsword"))
		{
			return WeaponType.SWORD;
		}
		return null;
	}

	private static boolean hasAny(List<String> words, String... keys)
	{
		for (String key : keys)
		{
			if (words.contains(key))
			{
				return true;
			}
		}
		return false;
	}

	/** Physical weapons in the bag, equipped one first. */
	private static List<Item> carriedWeapons(Player npc)
	{
		final List<Item> result = new ArrayList<>();
		for (Item item : npc.getInventory().getItems())
		{
			if ((item.getTemplate() instanceof Weapon) && !item.getTemplate().isMagicWeapon() && (((Weapon) item.getTemplate()).getItemType() != WeaponType.FISHINGROD))
			{
				if (item.isEquipped())
				{
					result.add(0, item);
				}
				else
				{
					result.add(item);
				}
			}
		}
		return result;
	}

	private static Item findCarried(List<Item> carried, WeaponType type)
	{
		for (Item item : carried)
		{
			if (((Weapon) item.getTemplate()).getItemType() == type)
			{
				return item;
			}
		}
		return null;
	}

	/** The weapon the class set calls main; without a set, the first carried weapon that is not in hand. */
	private static Item mainWeapon(Player npc, List<Item> carried)
	{
		final WeaponSet set = find(npc.getPlayerClass());
		for (Item item : carried)
		{
			if (set == null)
			{
				if (!item.isEquipped())
				{
					return item;
				}
				continue;
			}
			for (WeaponKind kind : set.main)
			{
				if (kind.matches(item.getTemplate()))
				{
					return item;
				}
			}
		}
		return null;
	}

	private static String describe(List<Item> carried)
	{
		final StringBuilder sb = new StringBuilder();
		for (Item item : carried)
		{
			if (sb.length() > 0)
			{
				sb.append(", ");
			}
			sb.append(familyLabel(item));
			if (item.isEquipped())
			{
				sb.append(" (in hand)");
			}
		}
		return sb.toString();
	}

	private static String familyLabel(Item item)
	{
		for (WeaponKind kind : WeaponKind.values())
		{
			if (kind.matches(item.getTemplate()))
			{
				return kind.label();
			}
		}
		return typeLabel(((Weapon) item.getTemplate()).getItemType());
	}

	private static String typeLabel(WeaponType type)
	{
		if (type == null)
		{
			return "weapon";
		}
		switch (type)
		{
			case POLE:
				return "polearm";
			case DUAL:
				return "dual swords";
			default:
				return type.name().toLowerCase();
		}
	}

	/**
	 * Equips {@code weapon} the way a player's own equip request would: never mid-cast (retried shortly after), and
	 * after the swing in progress. Back on a one-handed weapon, a shield in the bag goes back on too.
	 */
	private static void equip(Player npc, Item weapon, int tries)
	{
		if (npc.isDead() || (weapon.getOwnerId() != npc.getObjectId()) || weapon.isEquipped() || npc.isMounted() || npc.isDisarmed())
		{
			return;
		}
		if (npc.isCastingNow() || npc.isCastingSimultaneouslyNow())
		{
			if (tries < EQUIP_MAX_TRIES)
			{
				ThreadPool.schedule(() -> equip(npc, weapon, tries + 1), EQUIP_RETRY_MS);
			}
			return;
		}
		final long swingLeftMs = TimeUnit.NANOSECONDS.toMillis(npc.getAttackEndTime() - System.nanoTime());
		if ((swingLeftMs > 0) && (tries < EQUIP_MAX_TRIES))
		{
			ThreadPool.schedule(() -> equip(npc, weapon, tries + 1), swingLeftMs);
			return;
		}
		npc.useEquippableItem(weapon, false);
		if (weapon.isEquipped() && (weapon.getTemplate().getBodyPart() == BodyPart.R_HAND))
		{
			for (Item item : npc.getInventory().getItems())
			{
				if (!item.isEquipped() && item.isArmor() && (item.getTemplate().getBodyPart() == BodyPart.L_HAND))
				{
					npc.useEquippableItem(item, false);
					break;
				}
			}
		}
	}
}
