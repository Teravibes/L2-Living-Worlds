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
package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author -Wooden-
 */
public class ExFishingHpRegen extends ServerPacket
{
	private final Creature _creature;
	private final int _time;
	private final int _fishHP;
	private final int _hpMode;
	private final int _anim;
	private final int _goodUse;
	private final int _penalty;
	private final int _hpBarColor;
	
	public ExFishingHpRegen(Creature creature, int time, int fishHP, int hpMode, int goodUse, int anim, int penalty, int hpBarColor)
	{
		_creature = creature;
		_time = time;
		_fishHP = fishHP;
		_hpMode = hpMode;
		_goodUse = goodUse;
		_anim = anim;
		_penalty = penalty;
		_hpBarColor = hpBarColor;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_FISHING_HP_REGEN.writeId(this, buffer);
		buffer.writeInt(_creature.getObjectId());
		buffer.writeInt(_time);
		buffer.writeInt(_fishHP);
		buffer.writeByte(_hpMode); // 0 = HP stop, 1 = HP raise
		buffer.writeByte(_goodUse); // 0 = none, 1 = success, 2 = failed
		buffer.writeByte(_anim); // Anim: 0 = none, 1 = reeling, 2 = pumping
		buffer.writeInt(_penalty); // Penalty
		buffer.writeByte(_hpBarColor); // 0 = normal hp bar, 1 = purple hp bar
	}
}
