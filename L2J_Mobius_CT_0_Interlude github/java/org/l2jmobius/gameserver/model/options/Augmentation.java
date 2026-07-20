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
package org.l2jmobius.gameserver.model.options;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.data.xml.OptionData;
import org.l2jmobius.gameserver.model.actor.Player;

/**
 * Used to store an augmentation and its bonuses.
 * @author durgus
 */
public class Augmentation
{
	private int _effectsId = 0;
	private AugmentationStatBonus _bonus = null;
	
	public Augmentation(int effects)
	{
		_effectsId = effects;
		_bonus = new AugmentationStatBonus(_effectsId);
	}
	
	public static class AugmentationStatBonus
	{
		private static final Logger LOGGER = Logger.getLogger(AugmentationStatBonus.class.getName());
		private final List<Options> _options = new ArrayList<>();
		private boolean _active;
		
		public AugmentationStatBonus(int augmentationId)
		{
			_active = false;
			final int[] stats = new int[2];
			stats[0] = 0x0000FFFF & augmentationId;
			stats[1] = (augmentationId >> 16);
			for (int stat : stats)
			{
				final Options op = OptionData.getInstance().getOptions(stat);
				if (op != null)
				{
					_options.add(op);
				}
				else
				{
					LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Couldn't find option: " + stat);
				}
			}
		}
		
		public void applyBonus(Player player)
		{
			// make sure the bonuses are not applied twice..
			if (_active)
			{
				return;
			}
			
			for (Options op : _options)
			{
				op.apply(player);
			}
			
			_active = true;
		}
		
		public void removeBonus(Player player)
		{
			// make sure the bonuses are not removed twice
			if (!_active)
			{
				return;
			}
			
			for (Options op : _options)
			{
				op.remove(player);
			}
			
			_active = false;
		}
	}
	
	/**
	 * Get the augmentation "id" used in serverpackets.
	 * @return augmentationId
	 */
	public int getAugmentationId()
	{
		return _effectsId;
	}
	
	/**
	 * Applies the bonuses to the player.
	 * @param player
	 */
	public void applyBonus(Player player)
	{
		_bonus.applyBonus(player);
	}
	
	/**
	 * Removes the augmentation bonuses from the player.
	 * @param player
	 */
	public void removeBonus(Player player)
	{
		_bonus.removeBonus(player);
	}
}
