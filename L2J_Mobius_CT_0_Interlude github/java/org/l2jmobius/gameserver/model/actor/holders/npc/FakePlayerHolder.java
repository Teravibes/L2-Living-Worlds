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

import org.l2jmobius.gameserver.data.xml.FakePlayerData;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;

/**
 * @author Mobius
 */
public class FakePlayerHolder
{
	private final PlayerClass _playerClass;
	private final int _hair;
	private final int _hairColor;
	private final int _face;
	private final int _nameColor;
	private final int _titleColor;
	private final int _equipHead;
	private final int _equipRHand;
	private final int _equipLHand;
	private final int _equipGloves;
	private final int _equipChest;
	private final int _equipLegs;
	private final int _equipFeet;
	private final int _equipCloak;
	private final int _equipHair;
	private final int _equipHair2;
	private final int _agathionId;
	private final int _weaponEnchantLevel;
	private final int _armorEnchantLevel;
	private final boolean _fishing;
	private final int _baitLocationX;
	private final int _baitLocationY;
	private final int _baitLocationZ;
	private final int _recommends;
	private final int _nobleLevel;
	private final boolean _hero;
	private final int _clanId;
	private final int _pledgeStatus;
	private final boolean _isSitting;
	private final int _privateStoreType;
	private final String _privateStoreMessage;
	private final boolean _talkable;
	
	public FakePlayerHolder(StatSet set)
	{
		_playerClass = PlayerClass.getPlayerClass(set.getInt("classId", 1));
		_hair = set.getInt("hair", 1);
		_hairColor = set.getInt("hairColor", 1);
		_face = set.getInt("face", 1);
		_nameColor = set.getInt("nameColor", 0xFFFFFF);
		_titleColor = set.getInt("titleColor", 0xECF9A2);
		_equipHead = set.getInt("equipHead", 0);
		_equipRHand = set.getInt("equipRHand", 0); // Or dual hand.
		_equipLHand = set.getInt("equipLHand", 0);
		_equipGloves = set.getInt("equipGloves", 0);
		_equipChest = set.getInt("equipChest", 0);
		_equipLegs = set.getInt("equipLegs", 0);
		_equipFeet = set.getInt("equipFeet", 0);
		_equipCloak = set.getInt("equipCloak", 0);
		_equipHair = set.getInt("equipHair", 0);
		_equipHair2 = set.getInt("equipHair2", 0);
		_agathionId = set.getInt("agathionId", 0);
		_weaponEnchantLevel = set.getInt("weaponEnchantLevel", 0);
		_armorEnchantLevel = set.getInt("armorEnchantLevel", 0);
		_fishing = set.getBoolean("fishing", false);
		_baitLocationX = set.getInt("baitLocationX", 0);
		_baitLocationY = set.getInt("baitLocationY", 0);
		_baitLocationZ = set.getInt("baitLocationZ", 0);
		_recommends = set.getInt("recommends", 0);
		_nobleLevel = set.getInt("nobleLevel", 0);
		_hero = set.getBoolean("hero", false);
		_clanId = set.getInt("clanId", 0);
		_pledgeStatus = set.getInt("pledgeStatus", 0);
		_isSitting = set.getBoolean("sitting", false);
		_privateStoreType = set.getInt("privateStoreType", 0);
		_privateStoreMessage = set.getString("privateStoreMessage", "");
		_talkable = set.getBoolean("fakePlayerTalkable", true);
		
		// Populate FakePlayerData mappings.
		final String name = set.getString("name", "");
		FakePlayerData.getInstance().addFakePlayerId(name, set.getInt("id", 0)); // Map name to npcId.
		final String lowercaseName = name.toLowerCase();
		FakePlayerData.getInstance().addFakePlayerName(lowercaseName, name); // Map lowercase name to original name.
		if (_talkable)
		{
			FakePlayerData.getInstance().addTalkableFakePlayerName(lowercaseName);
		}
	}
	
	public PlayerClass getPlayerClass()
	{
		return _playerClass;
	}
	
	public int getHair()
	{
		return _hair;
	}
	
	public int getHairColor()
	{
		return _hairColor;
	}
	
	public int getFace()
	{
		return _face;
	}
	
	public int getNameColor()
	{
		return _nameColor;
	}
	
	public int getTitleColor()
	{
		return _titleColor;
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
	
	public int getAgathionId()
	{
		return _agathionId;
	}
	
	public int getWeaponEnchantLevel()
	{
		return _weaponEnchantLevel;
	}
	
	public int getArmorEnchantLevel()
	{
		return _armorEnchantLevel;
	}
	
	public boolean isFishing()
	{
		return _fishing;
	}
	
	public int getBaitLocationX()
	{
		return _baitLocationX;
	}
	
	public int getBaitLocationY()
	{
		return _baitLocationY;
	}
	
	public int getBaitLocationZ()
	{
		return _baitLocationZ;
	}
	
	public int getRecommends()
	{
		return _recommends;
	}
	
	public int getNobleLevel()
	{
		return _nobleLevel;
	}
	
	public boolean isHero()
	{
		return _hero;
	}
	
	public int getClanId()
	{
		return _clanId;
	}
	
	public int getPledgeStatus()
	{
		return _pledgeStatus;
	}
	
	public boolean isSitting()
	{
		return _isSitting;
	}
	
	public int getPrivateStoreType()
	{
		return _privateStoreType;
	}
	
	public String getPrivateStoreMessage()
	{
		return _privateStoreMessage;
	}
	
	public boolean isTalkable()
	{
		return _talkable;
	}
}
