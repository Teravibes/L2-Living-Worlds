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
package org.l2jmobius.gameserver.model.item;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;

/**
 * Class for the Henna object.
 * @author Zoey76
 */
public class Henna
{
	private final int _dyeId;
	private final int _dyeItemId;
	private final int _str;
	private final int _con;
	private final int _dex;
	private final int _int;
	private final int _men;
	private final int _wit;
	private final int _wearFee;
	private final int _wearCount;
	private final int _cancelFee;
	private final int _cancelCount;
	private final Set<PlayerClass> _wearClass;
	
	public Henna(StatSet set)
	{
		_dyeId = set.getInt("dyeId");
		_dyeItemId = set.getInt("dyeItemId");
		_str = set.getInt("str", 0);
		_con = set.getInt("con", 0);
		_dex = set.getInt("dex", 0);
		_int = set.getInt("int", 0);
		_men = set.getInt("men", 0);
		_wit = set.getInt("wit", 0);
		_wearFee = set.getInt("wear_fee");
		_wearCount = set.getInt("wear_count");
		_cancelFee = set.getInt("cancel_fee");
		_cancelCount = set.getInt("cancel_count");
		_wearClass = new HashSet<>();
	}
	
	/**
	 * @return the dye Id.
	 */
	public int getDyeId()
	{
		return _dyeId;
	}
	
	/**
	 * @return the item Id, required for this dye.
	 */
	public int getDyeItemId()
	{
		return _dyeItemId;
	}
	
	/**
	 * @return the STR stat.
	 */
	public int getStatSTR()
	{
		return _str;
	}
	
	/**
	 * @return the CON stat.
	 */
	public int getStatCON()
	{
		return _con;
	}
	
	/**
	 * @return the DEX stat.
	 */
	public int getStatDEX()
	{
		return _dex;
	}
	
	/**
	 * @return the INT stat.
	 */
	public int getStatINT()
	{
		return _int;
	}
	
	/**
	 * @return the MEN stat.
	 */
	public int getStatMEN()
	{
		return _men;
	}
	
	/**
	 * @return the WIT stat.
	 */
	public int getStatWIT()
	{
		return _wit;
	}
	
	/**
	 * @return the wear fee, cost for adding this dye to the player.
	 */
	public int getWearFee()
	{
		return _wearFee;
	}
	
	/**
	 * @return the wear count, the required count to add this dye to the player.
	 */
	public int getWearCount()
	{
		return _wearCount;
	}
	
	/**
	 * @return the cancel fee, cost for removing this dye from the player.
	 */
	public int getCancelFee()
	{
		return _cancelFee;
	}
	
	/**
	 * @return the cancel count, the retrieved amount of dye items after removing the dye.
	 */
	public int getCancelCount()
	{
		return _cancelCount;
	}
	
	/**
	 * @return the list with the allowed classes to wear this dye.
	 */
	public Set<PlayerClass> getAllowedWearClass()
	{
		return _wearClass;
	}
	
	/**
	 * @param playerClass the class trying to wear this dye.
	 * @return {@code true} if the player is allowed to wear this dye, {@code false} otherwise.
	 */
	public boolean isAllowedClass(PlayerClass playerClass)
	{
		return _wearClass.contains(playerClass);
	}
	
	/**
	 * @param wearClassIds the list of classes that can wear this dye.
	 */
	public void setWearClassIds(List<PlayerClass> wearClassIds)
	{
		_wearClass.addAll(wearClassIds);
	}
}
