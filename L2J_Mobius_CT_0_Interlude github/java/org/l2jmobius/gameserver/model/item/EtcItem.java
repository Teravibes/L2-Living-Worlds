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
package org.l2jmobius.gameserver.model.item;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.item.holders.ExtractableProduct;
import org.l2jmobius.gameserver.model.item.type.ActionType;
import org.l2jmobius.gameserver.model.item.type.EtcItemType;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;

/**
 * This class is dedicated to the management of EtcItem.
 */
public class EtcItem extends ItemTemplate
{
	private String _handler;
	private EtcItemType _type;
	private List<ExtractableProduct> _extractableItems;
	private int _extractableCountMin;
	private int _extractableCountMax;
	private boolean _isBlessed;
	
	/**
	 * Constructor for EtcItem.
	 * @param set StatSet designating the set of couples (key,value) for description of the Etc
	 */
	public EtcItem(StatSet set)
	{
		super(set);
	}
	
	@Override
	public void set(StatSet set)
	{
		super.set(set);
		_type = set.getEnum("etcitem_type", EtcItemType.class, EtcItemType.NONE);
		
		// l2j custom - EtcItemType.SHOT
		switch (getDefaultAction())
		{
			case SOULSHOT:
			case SUMMON_SOULSHOT:
			case SUMMON_SPIRITSHOT:
			case SPIRITSHOT:
			{
				_type = EtcItemType.SHOT;
				break;
			}
		}
		
		_type1 = ItemTemplate.TYPE1_ITEM_QUESTITEM_ADENA;
		_type2 = ItemTemplate.TYPE2_OTHER; // default is other
		
		if (isQuestItem())
		{
			_type2 = ItemTemplate.TYPE2_QUEST;
		}
		else if ((getId() == Inventory.ADENA_ID) || (getId() == Inventory.ANCIENT_ADENA_ID))
		{
			_type2 = ItemTemplate.TYPE2_MONEY;
		}
		
		_handler = set.getString("handler", null); // ! null !
		
		_extractableCountMin = set.getInt("extractableCountMin", 0);
		_extractableCountMax = set.getInt("extractableCountMax", 0);
		if (_extractableCountMin > _extractableCountMax)
		{
			LOGGER.warning("Item " + this + " extractableCountMin is bigger than extractableCountMax!");
		}
		
		_isBlessed = set.getBoolean("blessed", false) || (((getDefaultAction() == ActionType.SPIRITSHOT) || (getDefaultAction() == ActionType.SOULSHOT)) && (getName() != null) && getName().contains("Blessed"));
	}
	
	/**
	 * @return the type of Etc Item.
	 */
	@Override
	public EtcItemType getItemType()
	{
		return _type;
	}
	
	/**
	 * @return the ID of the Etc item after applying the mask.
	 */
	@Override
	public int getItemMask()
	{
		return _type.mask();
	}
	
	/**
	 * @return {@code true} if the item is an etc item, {@code false} otherwise.
	 */
	@Override
	public boolean isEtcItem()
	{
		return true;
	}
	
	/**
	 * @return the handler name, null if no handler for item.
	 */
	public String getHandlerName()
	{
		return _handler;
	}
	
	/**
	 * @return the extractable items list.
	 */
	public List<ExtractableProduct> getExtractableItems()
	{
		return _extractableItems;
	}
	
	/**
	 * @return the minimum count of extractable items
	 */
	public int getExtractableCountMin()
	{
		return _extractableCountMin;
	}
	
	/**
	 * @return the maximum count of extractable items
	 */
	public int getExtractableCountMax()
	{
		return _extractableCountMax;
	}
	
	/**
	 * @param extractableProduct
	 */
	@Override
	public void addCapsuledItem(ExtractableProduct extractableProduct)
	{
		if (_extractableItems == null)
		{
			_extractableItems = new ArrayList<>();
		}
		
		_extractableItems.add(extractableProduct);
	}
	
	/**
	 * @return {@code true} if the item is blessed, {@code false} otherwise.
	 */
	public boolean isBlessed()
	{
		return _isBlessed;
	}
}
