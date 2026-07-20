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
package org.l2jmobius.gameserver.model.item.enums;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;

/**
 * @author Mobius
 */
public enum BodyPart
{
	// Items with no body part.
	NONE(0x0000, Inventory.PAPERDOLL_TOTALSLOTS, "none"),
	
	// Basic body parts.
	UNDERWEAR(0x0001, Inventory.PAPERDOLL_UNDER, "underwear", "shirt"),
	R_EAR(0x0002, Inventory.PAPERDOLL_REAR, "rear", "rbracelet"),
	L_EAR(0x0004, Inventory.PAPERDOLL_LEAR, "lear"),
	LR_EAR(0x0006, -1, "rear;lear"), // Special case, no direct paperdoll slot.
	NECK(0x0008, Inventory.PAPERDOLL_NECK, "neck"),
	R_FINGER(0x0010, Inventory.PAPERDOLL_RFINGER, "rfinger"),
	L_FINGER(0x0020, Inventory.PAPERDOLL_LFINGER, "lfinger"),
	LR_FINGER(0x0030, -1, "rfinger;lfinger"), // Special case, no direct paperdoll slot.
	HEAD(0x0040, Inventory.PAPERDOLL_HEAD, "head"),
	R_HAND(0x0080, Inventory.PAPERDOLL_RHAND, "rhand"),
	L_HAND(0x0100, Inventory.PAPERDOLL_LHAND, "lhand"),
	GLOVES(0x0200, Inventory.PAPERDOLL_GLOVES, "gloves"),
	CHEST(0x0400, Inventory.PAPERDOLL_CHEST, "chest"),
	LEGS(0x0800, Inventory.PAPERDOLL_LEGS, "legs"),
	FEET(0x1000, Inventory.PAPERDOLL_FEET, "feet"),
	BACK(0x2000, Inventory.PAPERDOLL_CLOAK, "back"),
	LR_HAND(0x4000, -1, "lrhand"), // Maps to RHAND slot.
	FULL_ARMOR(0x8000, -1, "fullarmor", "onepiece"), // Maps to CHEST slot.
	
	// Extended body parts.
	HAIR(0x010000, Inventory.PAPERDOLL_HAIR, "hair"),
	ALLDRESS(0x020000, -1, "alldress"), // Maps to CHEST slot.
	HAIR2(0x040000, Inventory.PAPERDOLL_HAIR2, "hair2"),
	HAIRALL(0x080000, -1, "hairall", "dhair"), // Maps to HAIR slot.
	
	// Special slots for pets.
	WOLF(-100, -1, "wolf"),
	HATCHLING(-101, -1, "hatchling"),
	STRIDER(-102, -1, "strider"),
	BABYPET(-103, -1, "babypet"),
	GREATWOLF(-104, -1, "greatwolf");
	
	private final int _mask;
	private final int _paperdollSlot;
	private final String[] _names;
	
	private static final Map<Integer, BodyPart> BODY_PARTS_BY_PAPERDOLL_SLOT = new HashMap<>();
	private static final Map<String, BodyPart> BODY_PARTS_BY_NAME = new HashMap<>();
	static
	{
		for (BodyPart bodyPart : values())
		{
			// Only map paperdoll slots that are valid.
			if ((bodyPart._paperdollSlot >= 0) && (bodyPart._paperdollSlot < Inventory.PAPERDOLL_TOTALSLOTS))
			{
				BODY_PARTS_BY_PAPERDOLL_SLOT.put(bodyPart._paperdollSlot, bodyPart);
			}
			
			// Map all bodypart names.
			for (String name : bodyPart._names)
			{
				BODY_PARTS_BY_NAME.put(name, bodyPart);
			}
		}
	}
	
	/**
	 * Constructor for the body part enumeration.
	 * @param mask bit flag for the body part
	 * @param paperdollSlot the paperdoll slot index, -1 for special cases with no direct mapping
	 * @param names the body part names
	 */
	private BodyPart(int mask, int paperdollSlot, String... names)
	{
		_mask = mask;
		_paperdollSlot = paperdollSlot;
		_names = names;
	}
	
	/**
	 * Gets the body part's bit flag value.
	 * @return the body part mask
	 */
	public int getMask()
	{
		return _mask;
	}
	
	/**
	 * Gets the paperdoll slot index for this body part.
	 * @return the paperdoll slot index, or -1 if this body part doesn't map directly to a slot
	 */
	public int getPaperdollSlot()
	{
		return _paperdollSlot;
	}
	
	/**
	 * Gets the paperdoll index from a body part.<br>
	 * @param bodyPart the body part enum
	 * @return the paperdoll index, or -1 if not found
	 */
	public static int getPaperdollIndex(BodyPart bodyPart)
	{
		switch (bodyPart) // Handle special cases.
		{
			case LR_HAND:
			{
				return Inventory.PAPERDOLL_RHAND;
			}
			case FULL_ARMOR:
			case ALLDRESS:
			{
				return Inventory.PAPERDOLL_CHEST;
			}
			case HAIRALL:
			{
				return Inventory.PAPERDOLL_HAIR;
			}
			default:
			{
				return bodyPart.getPaperdollSlot();
			}
		}
	}
	
	/**
	 * Gets a body part by the paperdoll slot index.
	 * @param slot the paperdoll slot index
	 * @return the body part corresponding to the paperdoll slot, or {@code null} if not found
	 */
	public static BodyPart fromPaperdollSlot(int slot)
	{
		return BODY_PARTS_BY_PAPERDOLL_SLOT.get(slot);
	}
	
	/**
	 * Gets a body part for an equipped item.
	 * @param item the equipped item
	 * @return the body part corresponding to the equipped item, or {@code null} if not found
	 */
	public static BodyPart fromItem(Item item)
	{
		if (item == null)
		{
			return NONE;
		}
		
		final int slot = item.getLocationSlot();
		switch (slot)
		{
			case Inventory.PAPERDOLL_CHEST:
			{
				return item.getTemplate().getBodyPart(); // Special case for chest slots.
			}
			default:
			{
				return BODY_PARTS_BY_PAPERDOLL_SLOT.get(slot);
			}
		}
	}
	
	/**
	 * Gets a body part by the body part name.
	 * @param name the body part name
	 * @return the body part corresponding to the body part name, or {@code null} if not found
	 */
	public static BodyPart fromName(String name)
	{
		return BODY_PARTS_BY_NAME.get(name);
	}
}
