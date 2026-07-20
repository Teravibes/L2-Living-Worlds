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
package handlers.chat.commands.user;

import org.l2jmobius.gameserver.handler.IUserCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Party Info user command.
 * @author Tempy
 */
public class PartyInfo implements IUserCommandHandler
{
	private static final int[] COMMAND_IDS =
	{
		81
	};
	
	@Override
	public boolean onCommand(int id, Player player)
	{
		if (id != COMMAND_IDS[0])
		{
			return false;
		}
		
		player.sendPacket(SystemMessageId.PARTY_INFORMATION);
		if (player.isInParty())
		{
			final Party party = player.getParty();
			switch (party.getDistributionType())
			{
				case FINDERS_KEEPERS:
				{
					player.sendPacket(SystemMessageId.LOOTING_METHOD_FINDERS_KEEPERS);
					break;
				}
				case RANDOM:
				{
					player.sendPacket(SystemMessageId.LOOTING_METHOD_RANDOM);
					break;
				}
				case RANDOM_INCLUDING_SPOIL:
				{
					player.sendPacket(SystemMessageId.LOOTING_METHOD_RANDOM_INCLUDING_SPOIL);
					break;
				}
				case BY_TURN:
				{
					player.sendPacket(SystemMessageId.LOOTING_METHOD_BY_TURN);
					break;
				}
				case BY_TURN_INCLUDING_SPOIL:
				{
					player.sendPacket(SystemMessageId.LOOTING_METHOD_BY_TURN_INCLUDING_SPOIL);
					break;
				}
			}
			
			if (!party.isLeader(player))
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.PARTY_LEADER_S1);
				sm.addPcName(party.getLeader());
				player.sendPacket(sm);
			}
			
			player.sendMessage("Members: " + party.getMemberCount() + "/9"); // TODO: Custom?
		}
		
		player.sendPacket(SystemMessageId.EMPTY_3);
		return true;
	}
	
	@Override
	public int[] getCommandList()
	{
		return COMMAND_IDS;
	}
}
