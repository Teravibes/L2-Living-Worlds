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
package org.l2jmobius.gameserver.model.actor.holders.player;

import org.l2jmobius.gameserver.model.actor.Player;

/**
 * @author Gnacik
 */
public class RecoBonus
{
	private static final int[][] _recoBonus =
	{
		{
			25,
			50,
			50,
			50,
			50,
			50,
			50,
			50,
			50,
			50
		},
		{
			16,
			33,
			50,
			50,
			50,
			50,
			50,
			50,
			50,
			50
		},
		{
			12,
			25,
			37,
			50,
			50,
			50,
			50,
			50,
			50,
			50
		},
		{
			10,
			20,
			30,
			40,
			50,
			50,
			50,
			50,
			50,
			50
		},
		{
			8,
			16,
			25,
			33,
			41,
			50,
			50,
			50,
			50,
			50
		},
		{
			7,
			14,
			21,
			28,
			35,
			42,
			50,
			50,
			50,
			50
		},
		{
			6,
			12,
			18,
			25,
			31,
			37,
			43,
			50,
			50,
			50
		},
		{
			5,
			11,
			16,
			22,
			27,
			33,
			38,
			44,
			50,
			50
		},
		{
			5,
			10,
			15,
			20,
			25,
			30,
			35,
			40,
			45,
			50
		}
	};
	
	public static int getRecoBonus(Player player)
	{
		if ((player != null) && player.isOnline() && (player.getRecomHave() != 0))
		{
			final int level = player.getLevel() / 10;
			final int exp = (Math.min(100, player.getRecomHave()) - 1) / 10;
			return _recoBonus[level][exp];
		}
		
		return 0;
	}
	
	public static double getRecoMultiplier(Player player)
	{
		double multiplier = 1.0;
		final double bonus = getRecoBonus(player);
		if (bonus > 0)
		{
			multiplier += (bonus / 100);
		}
		
		return multiplier;
	}
}
