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
package org.l2jmobius.gameserver.model.actor.holders.npc;

import java.util.Collections;
import java.util.List;

import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;

/**
 * Per-instance appearance for a procedurally generated fake player.
 * <p>
 * Normally a fake player's looks come entirely from its NPC template
 * ({@link FakePlayerHolder}), meaning one template equals one fixed-looking
 * character. When an {@code Npc} carries a {@code FakePlayerAppearance}, the
 * {@code FakePlayerInfo} packet reads from this object instead, so many bots can
 * share a single base template yet each render as a unique player (own name,
 * race, gender, class, gear, hair and colors).
 */
public class FakePlayerAppearance
{
	private String _name;
	private String _title = "";
	private Race _race = Race.HUMAN;
	private boolean _female = false;
	private PlayerClass _playerClass = PlayerClass.getPlayerClass(0); // Human Fighter
	private int _level = 1;

	private int _hairStyle = 0;
	private int _hairColor = 0;
	private int _face = 0;
	private int _nameColor = 0xFFFFFF;
	private int _titleColor = 0xECF9A2;

	private int _equipHead = 0;
	private int _equipRHand = 0;
	private int _equipLHand = 0;
	private int _equipGloves = 0;
	private int _equipChest = 0;
	private int _equipLegs = 0;
	private int _equipFeet = 0;
	private int _equipCloak = 0;
	private int _equipHair = 0;
	private int _equipHair2 = 0;
	private int _weaponEnchantLevel = 0;

	private int _privateStoreType = 0; // 0 = none; otherwise a PrivateStoreType id (1 sell, 3 buy...)
	private String _storeMessage = "";
	private boolean _sitting = false;
	private List<FakePlayerStoreItem> _storeItems = Collections.emptyList();
	private List<FakePlayerCraftItem> _craftItems = Collections.emptyList();

	public String getName()
	{
		return _name;
	}

	public FakePlayerAppearance setName(String name)
	{
		_name = name;
		return this;
	}

	public String getTitle()
	{
		return _title;
	}

	public FakePlayerAppearance setTitle(String title)
	{
		_title = title;
		return this;
	}

	public Race getRace()
	{
		return _race;
	}

	public FakePlayerAppearance setRace(Race race)
	{
		_race = race;
		return this;
	}

	public boolean isFemale()
	{
		return _female;
	}

	public FakePlayerAppearance setFemale(boolean female)
	{
		_female = female;
		return this;
	}

	public PlayerClass getPlayerClass()
	{
		return _playerClass;
	}

	public FakePlayerAppearance setPlayerClass(PlayerClass playerClass)
	{
		_playerClass = playerClass;
		return this;
	}

	public int getLevel()
	{
		return _level;
	}

	public FakePlayerAppearance setLevel(int level)
	{
		_level = level;
		return this;
	}

	public int getHairStyle()
	{
		return _hairStyle;
	}

	public FakePlayerAppearance setHairStyle(int hairStyle)
	{
		_hairStyle = hairStyle;
		return this;
	}

	public int getHairColor()
	{
		return _hairColor;
	}

	public FakePlayerAppearance setHairColor(int hairColor)
	{
		_hairColor = hairColor;
		return this;
	}

	public int getFace()
	{
		return _face;
	}

	public FakePlayerAppearance setFace(int face)
	{
		_face = face;
		return this;
	}

	public int getNameColor()
	{
		return _nameColor;
	}

	public FakePlayerAppearance setNameColor(int nameColor)
	{
		_nameColor = nameColor;
		return this;
	}

	public int getTitleColor()
	{
		return _titleColor;
	}

	public FakePlayerAppearance setTitleColor(int titleColor)
	{
		_titleColor = titleColor;
		return this;
	}

	public int getEquipHead()
	{
		return _equipHead;
	}

	public int getEquipRHand()
	{
		return _equipRHand;
	}

	public int getEquipLHand()
	{
		return _equipLHand;
	}

	public int getEquipGloves()
	{
		return _equipGloves;
	}

	public int getEquipChest()
	{
		return _equipChest;
	}

	public int getEquipLegs()
	{
		return _equipLegs;
	}

	public int getEquipFeet()
	{
		return _equipFeet;
	}

	public int getEquipCloak()
	{
		return _equipCloak;
	}

	public int getEquipHair()
	{
		return _equipHair;
	}

	public int getEquipHair2()
	{
		return _equipHair2;
	}

	public int getWeaponEnchantLevel()
	{
		return _weaponEnchantLevel;
	}

	public FakePlayerAppearance setWeapon(int rHand, int lHand)
	{
		_equipRHand = rHand;
		_equipLHand = lHand;
		return this;
	}

	public FakePlayerAppearance setArmor(int head, int chest, int legs, int gloves, int feet, int cloak)
	{
		_equipHead = head;
		_equipChest = chest;
		_equipLegs = legs;
		_equipGloves = gloves;
		_equipFeet = feet;
		_equipCloak = cloak;
		return this;
	}

	public FakePlayerAppearance setWeaponEnchantLevel(int level)
	{
		_weaponEnchantLevel = level;
		return this;
	}

	public int getPrivateStoreType()
	{
		return _privateStoreType;
	}

	public String getStoreMessage()
	{
		return _storeMessage;
	}

	public boolean isSitting()
	{
		return _sitting;
	}

	/**
	 * Turns this bot into a seated private-store vendor.
	 * @param storeType a {@code PrivateStoreType} id (1 = sell, 3 = buy, ...)
	 * @param message the store title shown above the vendor
	 * @return this
	 */
	public FakePlayerAppearance setStore(int storeType, String message)
	{
		_privateStoreType = storeType;
		_storeMessage = message == null ? "" : message;
		_sitting = storeType != 0;
		return this;
	}

	/**
	 * @return the live stock backing this vendor's store (offered items for SELL, wanted items for BUY)
	 */
	public List<FakePlayerStoreItem> getStoreItems()
	{
		return _storeItems;
	}

	public FakePlayerAppearance setStoreItems(List<FakePlayerStoreItem> items)
	{
		_storeItems = items == null ? Collections.emptyList() : items;
		return this;
	}

	/**
	 * @return the recipes this crafter offers in its manufacture store
	 */
	public List<FakePlayerCraftItem> getCraftItems()
	{
		return _craftItems;
	}

	public FakePlayerAppearance setCraftItems(List<FakePlayerCraftItem> items)
	{
		_craftItems = items == null ? Collections.emptyList() : items;
		return this;
	}
}
