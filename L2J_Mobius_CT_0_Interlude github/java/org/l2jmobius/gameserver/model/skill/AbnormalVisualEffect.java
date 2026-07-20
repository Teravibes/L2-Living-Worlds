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
package org.l2jmobius.gameserver.model.skill;

/**
 * Abnormal Visual Effect enumerated.
 * @author DrHouse, Zoey76
 */
public enum AbnormalVisualEffect
{
	NONE(0x0000000, 0),
	DOT_BLEEDING(0x00000001, 0),
	DOT_POISON(0x00000002, 0),
	DOT_FIRE(0x00000004, 0),
	DOT_WATER(0x00000008, 0),
	DOT_WIND(0x00000010, 0),
	DOT_SOIL(0x00000020, 0),
	STUN(0x00000040, 0),
	SLEEP(0x00000080, 0),
	SILENCE(0x00000100, 0),
	ROOT(0x00000200, 0),
	PARALYZE(0x00000400, 0),
	FLESH_STONE(0x00000800, 0),
	DOT_MP(0x00001000, 0),
	BIG_HEAD(0x00002000, 0),
	DOT_FIRE_AREA(0x00004000, 0),
	CHANGE_TEXTURE(0x00008000, 0),
	BIG_BODY(0x00010000, 0),
	FLOATING_ROOT(0x00020000, 0),
	DANCE_ROOT(0x00040000, 0),
	GHOST_STUN(0x00080000, 0),
	STEALTH(0x00100000, 0),
	SEIZURE1(0x00200000, 0),
	SEIZURE2(0x00400000, 0),
	MAGIC_SQUARE(0x00800000, 0),
	FREEZING(0x01000000, 0),
	SHAKE(0x02000000, 0),
	BLIND(0x04000000, 0),
	ULTIMATE_DEFENCE(0x08000000, 0),
	VP_UP(0x10000000, 0),
	REAL_TARGET(0x20000000, 0),
	DEATH_MARK(0x40000000, 0),
	TURN_FLEE(0x80000000, 0),
	VP_KEEP(0x10000000, 0), // TODO: Find.
	// Special
	INVINCIBILITY(0x000001, 1),
	AIR_BATTLE_SLOW(0x000002, 1),
	AIR_BATTLE_ROOT(0x000004, 1),
	CHANGE_WP(0x000008, 1),
	CHANGE_HAIR_G(0x000010, 1),
	CHANGE_HAIR_P(0x000020, 1),
	CHANGE_HAIR_B(0x000040, 1),
	STIGMA_OF_SILEN(0x000100, 1),
	SPEED_DOWN(0x000200, 1),
	FROZEN_PILLAR(0x000400, 1),
	CHANGE_VES_S(0x000800, 1),
	CHANGE_VES_C(0x001000, 1),
	CHANGE_VES_D(0x002000, 1),
	TIME_BOMB(0x004000, 1), // High Five
	MP_SHIELD(0x008000, 1), // High Five
	NAVIT_ADVENT(0x080000, 1), // High Five
	
	// Event
	// TODO: Fix, currently not working.
	BR_NONE(0x000000, 2),
	BR_AFRO_NORMAL(0x000001, 2),
	BR_AFRO_PINK(0x000002, 2),
	BR_AFRO_GOLD(0x000004, 2),
	BR_POWER_OF_EVA(0x000008, 2), // High Five
	BR_HEADPHONE(0x000010, 2), // High Five
	BR_VESPER1(0x000020, 2),
	BR_VESPER2(0x000040, 2),
	BR_VESPER3(0x000080, 2),
	BR_SOUL_AVATAR(0x000100, 2); // High Five
	
	/** Int mask. */
	private final int _mask;
	/** Type: 0 Normal, 1 Special, 2 Event. */
	private final int _type;
	
	private AbnormalVisualEffect(int mask, int type)
	{
		_mask = mask;
		_type = type;
	}
	
	/**
	 * Gets the int bitmask for the abnormal visual effect.
	 * @return the int bitmask
	 */
	public int getMask()
	{
		return _mask;
	}
	
	/**
	 * Verify if it's a special abnormal visual effect.
	 * @return {@code true} it's a special abnormal visual effect, {@code false} otherwise
	 */
	public boolean isSpecial()
	{
		return _type == 1;
	}
	
	/**
	 * Verify if it's an event abnormal visual effect.
	 * @return {@code true} it's an event abnormal visual effect, {@code false} otherwise
	 */
	public boolean isEvent()
	{
		return _type == 2;
	}
}
