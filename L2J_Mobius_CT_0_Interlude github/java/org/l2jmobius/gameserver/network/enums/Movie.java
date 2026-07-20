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
package org.l2jmobius.gameserver.network.enums;

/**
 * This file contains all movies.
 * @author St3eT
 */
public enum Movie
{
	SC_LINDVIOR(1),
	SC_ECHMUS_OPENING(2),
	SC_ECHMUS_SUCCESS(3),
	SC_ECHMUS_FAIL(4),
	SC_BOSS_TIAT_OPENING(5),
	SC_BOSS_TIAT_ENDING_SUCCES(6),
	SC_BOSS_TIAT_ENDING_FAIL(7),
	SSQ_SUSPICIOUS_DEATHS(8),
	SSQ_DYING_MASSAGE(9),
	SSQ_CONTRACT_OF_MAMMON(10),
	SSQ_RITUAL_OF_PRIEST(11),
	SSQ_SEALING_EMPEROR_1ST(12),
	SSQ_SEALING_EMPEROR_2ND(13),
	SSQ_EMBRYO(14),
	SC_BOSS_FREYA_OPENING(15),
	SC_BOSS_FREYA_PHASECH_A(16),
	SC_BOSS_FREYA_PHASECH_B(17),
	SC_BOSS_KEGOR_INTRUSION(18),
	SC_BOSS_FREYA_ENDING_A(19),
	SC_BOSS_FREYA_ENDING_B(20),
	SC_BOSS_FREYA_FORCED_DEFEAT(21),
	SC_BOSS_FREYA_DEFEAT(22),
	SC_ICE_HEAVYKNIGHT_SPAWN(23),
	SSQ2_HOLY_BURIAL_GROUND_OPENING(24),
	SSQ2_HOLY_BURIAL_GROUND_CLOSING(25),
	SSQ2_SOLINA_TOMB_OPENING(26),
	SSQ2_SOLINA_TOMB_CLOSING(27),
	SSQ2_ELYSS_NARRATION(28),
	SSQ2_BOSS_OPENING(29),
	SSQ2_BOSS_CLOSING(30),
	LAND_KSERTH_A(1000),
	LAND_KSERTH_B(1001),
	LAND_UNDEAD_A(1002),
	LAND_DISTRUCTION_A(1003),
	LAND_ANNIHILATION_A(1004);
	
	private final int _clientId;
	
	private Movie(int clientId)
	{
		_clientId = clientId;
	}
	
	/**
	 * @return the client id.
	 */
	public int getClientId()
	{
		return _clientId;
	}
}
