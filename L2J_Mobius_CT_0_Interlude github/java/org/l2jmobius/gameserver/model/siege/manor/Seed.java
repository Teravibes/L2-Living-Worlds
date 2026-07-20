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
package org.l2jmobius.gameserver.model.siege.manor;

import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.item.ItemTemplate;

public class Seed
{
	private final int _seedId;
	private final int _cropId; // crop type
	private final int _level; // seed level
	private final int _matureId; // mature crop type
	private final int _reward1;
	private final int _reward2;
	private final int _castleId; // id of manor (castle id) where seed can be farmed
	private final boolean _isAlternative;
	private final int _limitSeeds;
	private final int _limitCrops;
	private final int _seedReferencePrice;
	private final int _cropReferencePrice;
	
	public Seed(StatSet set)
	{
		_cropId = set.getInt("id");
		_seedId = set.getInt("seedId");
		_level = set.getInt("level");
		_matureId = set.getInt("mature_Id");
		_reward1 = set.getInt("reward1");
		_reward2 = set.getInt("reward2");
		_castleId = set.getInt("castleId");
		_isAlternative = set.getBoolean("alternative");
		_limitCrops = set.getInt("limit_crops");
		_limitSeeds = set.getInt("limit_seed");
		
		// Set prices
		ItemTemplate item = ItemData.getInstance().getTemplate(_cropId);
		_cropReferencePrice = (item != null) ? item.getReferencePrice() : 1;
		item = ItemData.getInstance().getTemplate(_seedId);
		_seedReferencePrice = (item != null) ? item.getReferencePrice() : 1;
	}
	
	public int getCastleId()
	{
		return _castleId;
	}
	
	public int getSeedId()
	{
		return _seedId;
	}
	
	public int getCropId()
	{
		return _cropId;
	}
	
	public int getMatureId()
	{
		return _matureId;
	}
	
	public int getReward(int type)
	{
		return (type == 1) ? _reward1 : _reward2;
	}
	
	public int getLevel()
	{
		return _level;
	}
	
	public boolean isAlternative()
	{
		return _isAlternative;
	}
	
	public int getSeedLimit()
	{
		return _limitSeeds * RatesConfig.RATE_DROP_MANOR;
	}
	
	public int getCropLimit()
	{
		return _limitCrops * RatesConfig.RATE_DROP_MANOR;
	}
	
	public int getSeedReferencePrice()
	{
		return _seedReferencePrice;
	}
	
	public int getSeedMaxPrice()
	{
		return _seedReferencePrice * 10;
	}
	
	public int getSeedMinPrice()
	{
		return (int) (_seedReferencePrice * 0.6);
	}
	
	public int getCropReferencePrice()
	{
		return _cropReferencePrice;
	}
	
	public int getCropMaxPrice()
	{
		return _cropReferencePrice * 10;
	}
	
	public int getCropMinPrice()
	{
		return (int) (_cropReferencePrice * 0.6);
	}
	
	@Override
	public String toString()
	{
		return "SeedData [_id=" + _seedId + ", _level=" + _level + ", _crop=" + _cropId + ", _mature=" + _matureId + ", _type1=" + _reward1 + ", _type2=" + _reward2 + ", _manorId=" + _castleId + ", _isAlternative=" + _isAlternative + ", _limitSeeds=" + _limitSeeds + ", _limitCrops=" + _limitCrops + "]";
	}
}
