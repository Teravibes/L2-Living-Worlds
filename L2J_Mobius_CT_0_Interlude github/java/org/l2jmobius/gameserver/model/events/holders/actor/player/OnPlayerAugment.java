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
package org.l2jmobius.gameserver.model.events.holders.actor.player;

import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.IBaseEvent;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.options.Augmentation;

/**
 * @author UnAfraid
 */
public class OnPlayerAugment implements IBaseEvent
{
	private final Player _player;
	private final Item _item;
	private final Augmentation _augmentation;
	private final boolean _isAugment; // true = is being augmented // false = augment is being removed
	
	public OnPlayerAugment(Player player, Item item, Augmentation augment, boolean isAugment)
	{
		_player = player;
		_item = item;
		_augmentation = augment;
		_isAugment = isAugment;
	}
	
	public Player getPlayer()
	{
		return _player;
	}
	
	public Item getItem()
	{
		return _item;
	}
	
	public Augmentation getAugmentation()
	{
		return _augmentation;
	}
	
	public boolean isAugment()
	{
		return _isAugment;
	}
	
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_AUGMENT;
	}
}
