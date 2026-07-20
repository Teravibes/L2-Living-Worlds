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
package org.l2jmobius.gameserver.model.skill.targets;

/**
 * Affect scope enumerated.
 * @author Zoey76
 */
public enum AffectScope
{
	/** Affects Valakas. */
	VALAKAS_SCOPE,
	/** Affects dead clan mates. */
	DEAD_PLEDGE,
	/** Affects fan area. */
	FAN,
	/** Affects nothing. */
	NONE,
	/** Affects party members. */
	PARTY,
	/** Affects party and clan mates. */
	PARTY_PLEDGE,
	/** Affects clan mates. */
	PLEDGE,
	/** Affects point blank targets, using caster as point of origin. */
	POINT_BLANK,
	/** Affects ranged targets, using selected target as point of origin. */
	RANGE,
	/** Affects ranged targets, using selected target as point of origin. */
	RING_RANGE,
	/** Affects a single target. */
	SINGLE,
	/** Affects targets inside an square area, using selected target as point of origin. */
	SQUARE,
	/** Affects targets inside an square area, using caster as point of origin. */
	SQUARE_PB,
	/** Affects static object targets. */
	STATIC_OBJECT_SCOPE,
	/** Affects wyverns. */
	WYVERN_SCOPE
}
