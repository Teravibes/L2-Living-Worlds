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
package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExAskJoinMPCC;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Format: (ch) S<br>
 * D0 0D 00 5A 00 77 00 65 00 72 00 67 00 00 00
 * @author chris_00
 */
public class RequestExAskJoinMPCC extends ClientPacket
{
	private String _name;
	
	@Override
	protected void readImpl()
	{
		_name = readString();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final Player target = World.getInstance().getPlayer(_name);
		if (target == null)
		{
			return;
		}
		
		// invite yourself? ;)
		if (player.isInParty() && target.isInParty() && player.getParty().equals(target.getParty()))
		{
			return;
		}
		
		SystemMessage sm;
		
		// activeChar is in a Party?
		if (player.isInParty())
		{
			final Party activeParty = player.getParty();
			
			// activeChar is PartyLeader? && activeChars Party is already in a CommandChannel?
			if (activeParty.getLeader().equals(player))
			{
				// if activeChars Party is in CC, is activeChar CCLeader?
				if (activeParty.isInCommandChannel() && activeParty.getCommandChannel().getLeader().equals(player))
				{
					// in CC and the CCLeader
					// target in a party?
					if (target.isInParty())
					{
						// targets party already in a CChannel?
						if (target.getParty().isInCommandChannel())
						{
							sm = new SystemMessage(SystemMessageId.S1_S_PARTY_IS_ALREADY_A_MEMBER_OF_THE_COMMAND_CHANNEL);
							sm.addString(target.getName());
							player.sendPacket(sm);
						}
						else
						{
							// ready to open a new CC
							// send request to targets Party's PartyLeader
							askJoinMPCC(player, target);
						}
					}
					else
					{
						player.sendMessage(target.getName() + " doesn't have party and cannot be invited to Command Channel.");
					}
				}
				else if (activeParty.isInCommandChannel() && !activeParty.getCommandChannel().getLeader().equals(player))
				{
					// in CC, but not the CCLeader
					sm = new SystemMessage(SystemMessageId.YOU_DO_NOT_HAVE_AUTHORITY_TO_INVITE_SOMEONE_TO_THE_COMMAND_CHANNEL);
					player.sendPacket(sm);
				}
				else
				{
					// target in a party?
					if (target.isInParty())
					{
						// targets party already in a CChannel?
						if (target.getParty().isInCommandChannel())
						{
							sm = new SystemMessage(SystemMessageId.S1_S_PARTY_IS_ALREADY_A_MEMBER_OF_THE_COMMAND_CHANNEL);
							sm.addString(target.getName());
							player.sendPacket(sm);
						}
						else
						{
							// ready to open a new CC
							// send request to targets Party's PartyLeader
							askJoinMPCC(player, target);
						}
					}
					else
					{
						player.sendMessage(target.getName() + " doesn't have party and cannot be invited to Command Channel.");
					}
				}
			}
			else
			{
				player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_AUTHORITY_TO_INVITE_SOMEONE_TO_THE_COMMAND_CHANNEL);
			}
		}
	}
	
	private void askJoinMPCC(Player requestor, Player target)
	{
		boolean hasRight = false;
		if (requestor.isClanLeader() && (requestor.getClan().getLevel() >= 5))
		{
			// Clan leader of level5 Clan or higher.
			hasRight = true;
		}
		else if (requestor.getInventory().getItemByItemId(8871) != null)
		{
			// 8871 Strategy Guide.
			// TODO: Should destroyed after successful invite?
			hasRight = true;
		}
		else if ((requestor.getPledgeClass() >= 5) && (requestor.getKnownSkill(391) != null))
		{
			// At least Baron or higher and the skill Clan Imperium
			hasRight = true;
		}
		
		if (!hasRight)
		{
			requestor.sendPacket(SystemMessageId.COMMAND_CHANNELS_CAN_ONLY_BE_FORMED_BY_A_PARTY_LEADER_WHO_IS_ALSO_THE_LEADER_OF_A_LEVEL_5_CLAN);
			return;
		}
		
		// Get the target's party leader, and do whole actions on him.
		final Player targetLeader = target.getParty().getLeader();
		SystemMessage sm;
		if (!targetLeader.isProcessingRequest())
		{
			requestor.onTransactionRequest(targetLeader);
			sm = new SystemMessage(SystemMessageId.S1_HAS_INVITED_YOU_TO_JOIN_A_COMMAND_CHANNEL_DO_YOU_WISH_TO_ACCEPT);
			sm.addString(requestor.getName());
			targetLeader.sendPacket(sm);
			targetLeader.sendPacket(new ExAskJoinMPCC(requestor.getName()));
			requestor.sendMessage("You invited " + targetLeader.getName() + " to your Command Channel.");
		}
		else
		{
			sm = new SystemMessage(SystemMessageId.S1_IS_BUSY_PLEASE_TRY_AGAIN_LATER);
			sm.addString(targetLeader.getName());
			requestor.sendPacket(sm);
		}
	}
}
