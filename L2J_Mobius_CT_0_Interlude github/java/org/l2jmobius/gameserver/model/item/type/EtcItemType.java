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
package org.l2jmobius.gameserver.model.item.type;

/**
 * EtcItem Type enumerated.
 */
public enum EtcItemType implements ItemType
{
	NONE,
	ARROW,
	POTION,
	SCRL_ENCHANT_WP,
	SCRL_ENCHANT_AM,
	SCROLL,
	RECIPE,
	MATERIAL,
	PET_COLLAR,
	CASTLE_GUARD,
	LOTTO,
	RACE_TICKET,
	DYE,
	SEED,
	CROP,
	MATURECROP,
	HARVEST,
	SEED2,
	TICKET_OF_LORD,
	LURE,
	BLESS_SCRL_ENCHANT_WP,
	BLESS_SCRL_ENCHANT_AM,
	COUPON,
	ELIXIR,
	SCRL_ENCHANT_ATTR,
	SCRL_INC_ENCHANT_PROP_WP,
	SCRL_INC_ENCHANT_PROP_AM,
	ANCIENT_CRYSTAL_ENCHANT_WP,
	ANCIENT_CRYSTAL_ENCHANT_AM,
	RUNE_SELECT,
	RUNE,
	
	// L2J CUSTOM, BACKWARD COMPATIBILITY
	SHOT;
	
	/**
	 * @return the ID of the item after applying the mask.
	 */
	@Override
	public int mask()
	{
		return 0;
	}
}
