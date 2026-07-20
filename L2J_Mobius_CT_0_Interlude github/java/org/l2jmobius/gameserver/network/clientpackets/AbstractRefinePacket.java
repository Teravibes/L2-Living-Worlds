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
package org.l2jmobius.gameserver.network.clientpackets;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.Armor;
import org.l2jmobius.gameserver.model.item.Weapon;
import org.l2jmobius.gameserver.model.item.enums.BodyPart;
import org.l2jmobius.gameserver.model.item.enums.ItemLocation;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.item.type.CrystalType;
import org.l2jmobius.gameserver.network.SystemMessageId;

public abstract class AbstractRefinePacket extends ClientPacket
{
	public static final int GRADE_NONE = 0;
	public static final int GRADE_MID = 1;
	public static final int GRADE_HIGH = 2;
	public static final int GRADE_TOP = 3;
	
	protected static final int GEMSTONE_D = 2130;
	protected static final int GEMSTONE_C = 2131;
	
	private static final Map<Integer, LifeStone> _lifeStones = new HashMap<>();
	
	protected static class LifeStone
	{
		// Lifestone level to player level table.
		private static final int[] LEVELS =
		{
			46,
			49,
			52,
			55,
			58,
			61,
			64,
			67,
			70,
			76,
			80
		};
		private final int _grade;
		private final int _level;
		
		public LifeStone(int grade, int level)
		{
			_grade = grade;
			_level = level;
		}
		
		public int getLevel()
		{
			return _level;
		}
		
		public int getGrade()
		{
			return _grade;
		}
		
		public int getPlayerLevel()
		{
			return LEVELS[_level];
		}
	}
	
	static
	{
		// itemId, (LS grade, LS level)
		_lifeStones.put(8723, new LifeStone(GRADE_NONE, 0));
		_lifeStones.put(8724, new LifeStone(GRADE_NONE, 1));
		_lifeStones.put(8725, new LifeStone(GRADE_NONE, 2));
		_lifeStones.put(8726, new LifeStone(GRADE_NONE, 3));
		_lifeStones.put(8727, new LifeStone(GRADE_NONE, 4));
		_lifeStones.put(8728, new LifeStone(GRADE_NONE, 5));
		_lifeStones.put(8729, new LifeStone(GRADE_NONE, 6));
		_lifeStones.put(8730, new LifeStone(GRADE_NONE, 7));
		_lifeStones.put(8731, new LifeStone(GRADE_NONE, 8));
		_lifeStones.put(8732, new LifeStone(GRADE_NONE, 9));
		
		_lifeStones.put(8733, new LifeStone(GRADE_MID, 0));
		_lifeStones.put(8734, new LifeStone(GRADE_MID, 1));
		_lifeStones.put(8735, new LifeStone(GRADE_MID, 2));
		_lifeStones.put(8736, new LifeStone(GRADE_MID, 3));
		_lifeStones.put(8737, new LifeStone(GRADE_MID, 4));
		_lifeStones.put(8738, new LifeStone(GRADE_MID, 5));
		_lifeStones.put(8739, new LifeStone(GRADE_MID, 6));
		_lifeStones.put(8740, new LifeStone(GRADE_MID, 7));
		_lifeStones.put(8741, new LifeStone(GRADE_MID, 8));
		_lifeStones.put(8742, new LifeStone(GRADE_MID, 9));
		
		_lifeStones.put(8743, new LifeStone(GRADE_HIGH, 0));
		_lifeStones.put(8744, new LifeStone(GRADE_HIGH, 1));
		_lifeStones.put(8745, new LifeStone(GRADE_HIGH, 2));
		_lifeStones.put(8746, new LifeStone(GRADE_HIGH, 3));
		_lifeStones.put(8747, new LifeStone(GRADE_HIGH, 4));
		_lifeStones.put(8748, new LifeStone(GRADE_HIGH, 5));
		_lifeStones.put(8749, new LifeStone(GRADE_HIGH, 6));
		_lifeStones.put(8750, new LifeStone(GRADE_HIGH, 7));
		_lifeStones.put(8751, new LifeStone(GRADE_HIGH, 8));
		_lifeStones.put(8752, new LifeStone(GRADE_HIGH, 9));
		
		_lifeStones.put(8753, new LifeStone(GRADE_TOP, 0));
		_lifeStones.put(8754, new LifeStone(GRADE_TOP, 1));
		_lifeStones.put(8755, new LifeStone(GRADE_TOP, 2));
		_lifeStones.put(8756, new LifeStone(GRADE_TOP, 3));
		_lifeStones.put(8757, new LifeStone(GRADE_TOP, 4));
		_lifeStones.put(8758, new LifeStone(GRADE_TOP, 5));
		_lifeStones.put(8759, new LifeStone(GRADE_TOP, 6));
		_lifeStones.put(8760, new LifeStone(GRADE_TOP, 7));
		_lifeStones.put(8761, new LifeStone(GRADE_TOP, 8));
		_lifeStones.put(8762, new LifeStone(GRADE_TOP, 9));
	}
	
	protected static LifeStone getLifeStone(int itemId)
	{
		return _lifeStones.get(itemId);
	}
	
	/**
	 * Checks player, source item, lifestone and gemstone validity for augmentation process
	 * @param player
	 * @param item
	 * @param refinerItem
	 * @param gemStones
	 * @return
	 */
	protected static boolean isValid(Player player, Item item, Item refinerItem, Item gemStones)
	{
		if (!isValid(player, item, refinerItem))
		{
			return false;
		}
		
		// GemStones must belong to owner
		if (gemStones.getOwnerId() != player.getObjectId())
		{
			return false;
		}
		
		// .. and located in inventory
		if (gemStones.getItemLocation() != ItemLocation.INVENTORY)
		{
			return false;
		}
		
		final CrystalType grade = item.getTemplate().getCrystalType();
		final LifeStone ls = _lifeStones.get(refinerItem.getId());
		
		// Check for item id
		if (getGemStoneId(grade) != gemStones.getId())
		{
			return false;
		}
		
		// Count must be greater or equal of required number
		if (getGemStoneCount(grade, ls.getGrade()) > gemStones.getCount())
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * Checks player, source item and lifestone validity for augmentation process
	 * @param player
	 * @param item
	 * @param refinerItem
	 * @return
	 */
	protected static boolean isValid(Player player, Item item, Item refinerItem)
	{
		if (!isValid(player, item))
		{
			return false;
		}
		
		// Item must belong to owner
		if (refinerItem.getOwnerId() != player.getObjectId())
		{
			return false;
		}
		
		// Lifestone must be located in inventory
		if (refinerItem.getItemLocation() != ItemLocation.INVENTORY)
		{
			return false;
		}
		
		final LifeStone ls = _lifeStones.get(refinerItem.getId());
		if (ls == null)
		{
			return false;
		}
		
		// check for level of the lifestone
		if (player.getLevel() < ls.getPlayerLevel())
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * Check both player and source item conditions for augmentation process
	 * @param player
	 * @param item
	 * @return
	 */
	protected static boolean isValid(Player player, Item item)
	{
		if (!isValid(player))
		{
			return false;
		}
		
		// Item must belong to owner
		if (item.getOwnerId() != player.getObjectId())
		{
			return false;
		}
		
		if (item.isAugmented())
		{
			return false;
		}
		
		if (item.isHeroItem())
		{
			return false;
		}
		
		if (item.isShadowItem())
		{
			return false;
		}
		
		if (item.isCommonItem())
		{
			return false;
		}
		
		if (item.isEtcItem())
		{
			return false;
		}
		
		if (item.isTimeLimitedItem())
		{
			return false;
		}
		
		if (item.isPvp() && !PlayerConfig.ALT_ALLOW_AUGMENT_PVP_ITEMS)
		{
			return false;
		}
		
		if (item.getTemplate().getCrystalType().isLesser(CrystalType.C))
		{
			return false;
		}
		
		// Source item can be equipped or in inventory
		switch (item.getItemLocation())
		{
			case INVENTORY:
			case PAPERDOLL:
			{
				break;
			}
			default:
			{
				return false;
			}
		}
		
		if (item.getTemplate() instanceof Weapon)
		{
			switch (((Weapon) item.getTemplate()).getItemType())
			{
				case NONE:
				case FISHINGROD:
				{
					return false;
				}
				default:
				{
					break;
				}
			}
		}
		else if (item.getTemplate() instanceof Armor)
		{
			// Only accessories can be augmented.
			switch (item.getTemplate().getBodyPart())
			{
				case BodyPart.LR_FINGER:
				case BodyPart.LR_EAR:
				case BodyPart.NECK:
				{
					break;
				}
				default:
				{
					return false;
				}
			}
		}
		else
		{
			return false; // neither weapon nor armor ?
		}
		
		// blacklist check
		if (Arrays.binarySearch(PlayerConfig.AUGMENTATION_BLACKLIST, item.getId()) >= 0)
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * Check if player's conditions valid for augmentation process
	 * @param player
	 * @return
	 */
	protected static boolean isValid(Player player)
	{
		if (player.isInStoreMode())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_AUGMENT_ITEMS_WHILE_A_PRIVATE_STORE_OR_PRIVATE_WORKSHOP_IS_IN_OPERATION);
			return false;
		}
		
		if (player.getActiveTradeList() != null)
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_AUGMENT_ITEMS_WHILE_ENGAGED_IN_TRADE_ACTIVITIES);
			return false;
		}
		
		if (player.isDead())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_AUGMENT_ITEMS_WHILE_DEAD);
			return false;
		}
		
		if (player.isParalyzed())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_AUGMENT_ITEMS_WHILE_PARALYZED);
			return false;
		}
		
		if (player.isFishing())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_AUGMENT_ITEMS_WHILE_FISHING);
			return false;
		}
		
		if (player.isSitting())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_AUGMENT_ITEMS_WHILE_SITTING_DOWN);
			return false;
		}
		
		if (player.isCursedWeaponEquipped())
		{
			return false;
		}
		
		if (player.isEnchanting() || player.isProcessingTransaction())
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * @param itemGrade
	 * @return GemStone itemId based on item grade
	 */
	protected static int getGemStoneId(CrystalType itemGrade)
	{
		switch (itemGrade)
		{
			case C:
			case B:
			{
				return GEMSTONE_D;
			}
			case A:
			case S:
			{
				return GEMSTONE_C;
			}
			default:
			{
				return 0;
			}
		}
	}
	
	/**
	 * Different for weapon and accessory augmentation.
	 * @param itemGrade
	 * @param lifeStoneGrade
	 * @return GemStone count based on item grade and life stone grade
	 */
	protected static int getGemStoneCount(CrystalType itemGrade, int lifeStoneGrade)
	{
		switch (itemGrade)
		{
			case C:
			{
				return 20;
			}
			case B:
			{
				return 30;
			}
			case A:
			{
				return 20;
			}
			case S:
			{
				return 25;
			}
			default:
			{
				return 0;
			}
		}
	}
}
