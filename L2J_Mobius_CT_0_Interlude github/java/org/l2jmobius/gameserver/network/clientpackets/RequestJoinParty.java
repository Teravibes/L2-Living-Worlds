/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.managers.PhantomBuddyManager;
import org.l2jmobius.gameserver.managers.PhantomManager;
import org.l2jmobius.gameserver.managers.PhantomPartyManager;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.player.BlockList;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.groups.PartyDistributionType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.AskJoinParty;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * sample 29 42 00 00 10 01 00 00 00 format cdd
 * @version $Revision: 1.7.4.4 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestJoinParty extends ClientPacket
{
	private String _name;
	private int _partyDistributionTypeId;
	
	@Override
	protected void readImpl()
	{
		_name = readString();
		_partyDistributionTypeId = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player requestor = getPlayer();
		if (requestor == null)
		{
			return;
		}
		
		final Player target = World.getInstance().getPlayer(_name);
		if (target == null)
		{
			requestor.sendPacket(SystemMessageId.YOU_MUST_FIRST_SELECT_A_USER_TO_INVITE_TO_YOUR_PARTY);
			return;
		}

		// Support buddy: a clientless phantom cannot answer the normal invite dialog, so accept it server-side
		// and bind it to the inviting player. (Must be checked before the offline-mode guard below, since a
		// buddy has no client either.)
		if (PhantomManager.getInstance().isBuddy(target))
		{
			final SystemMessage invited = new SystemMessage(SystemMessageId.YOU_HAVE_INVITED_S1_TO_YOUR_PARTY);
			invited.addString(target.getName());
			requestor.sendPacket(invited);
			if (!PhantomBuddyManager.getInstance().onInvited(requestor, target))
			{
				requestor.sendMessage(target.getName() + " could not join your party right now.");
			}
			return;
		}

		// Recruited combat party member (answered an LFM/LFP shout and walked over): same server-side accept.
		if (PhantomPartyManager.getInstance().isRecruit(target))
		{
			final SystemMessage invited = new SystemMessage(SystemMessageId.YOU_HAVE_INVITED_S1_TO_YOUR_PARTY);
			invited.addString(target.getName());
			requestor.sendPacket(invited);
			if (!PhantomPartyManager.getInstance().onInvited(requestor, target))
			{
				requestor.sendMessage(target.getName() + " could not join your party right now.");
			}
			return;
		}

		// Befriended regular phantom (friend tier): invite it like a person - it accepts server-side and is
		// adopted with the full recruited-member party AI, returning to idle friend life when the party ends.
		// Gated on the requester's friend list, so a stranger's phantom stays "offline" to everyone else.
		if (PhantomManager.getInstance().isPhantom(target) && requestor.getFriendList().contains(target.getObjectId()))
		{
			final SystemMessage invited = new SystemMessage(SystemMessageId.YOU_HAVE_INVITED_S1_TO_YOUR_PARTY);
			invited.addString(target.getName());
			requestor.sendPacket(invited);
			if (!PhantomPartyManager.getInstance().onInvitedFriend(requestor, target))
			{
				requestor.sendMessage(target.getName() + " could not join your party right now.");
			}
			return;
		}

		if ((target.getClient() == null) || target.getClient().isDetached())
		{
			requestor.sendMessage("Player is in offline mode.");
			return;
		}
		
		if (requestor.isPartyBanned())
		{
			requestor.sendMessage("You have been reported as an illegal program user, so participating in a party is not allowed.");
			requestor.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (target.isPartyBanned())
		{
			requestor.sendMessage(target.getName() + " has been reported as an illegal program user and cannot join a party.");
			return;
		}
		
		if (!target.isVisibleFor(requestor))
		{
			requestor.sendPacket(SystemMessageId.THAT_IS_THE_INCORRECT_TARGET);
			return;
		}
		
		if (requestor.isRegisteredOnEvent() || target.isRegisteredOnEvent())
		{
			if (GeneralConfig.ALLOW_PARTY_IN_SAME_EVENT)
			{
				if (!((requestor.getInstanceId() == target.getInstanceId()) && requestor.isRegisteredOnEvent() && target.isRegisteredOnEvent()))
				{
					requestor.sendMessage("Event paticipants cannot be invited to parties.");
					return;
				}
				
				if (!requestor.getTeam().equals(target.getTeam()))
				{
					requestor.sendMessage("You cannot be invited to a party of another team.");
					return;
				}
			}
			else
			{
				requestor.sendMessage("Event paticipants cannot be invited to parties.");
				return;
			}
		}
		
		SystemMessage sm;
		if (target.isInParty())
		{
			sm = new SystemMessage(SystemMessageId.S1_IS_A_MEMBER_OF_ANOTHER_PARTY_AND_CANNOT_BE_INVITED);
			sm.addString(target.getName());
			requestor.sendPacket(sm);
			return;
		}
		
		if (BlockList.isBlocked(target, requestor))
		{
			sm = new SystemMessage(SystemMessageId.S1_HAS_PLACED_YOU_ON_HIS_HER_IGNORE_LIST);
			sm.addString(target.getName());
			requestor.sendPacket(sm);
			return;
		}
		
		if (target == requestor)
		{
			requestor.sendPacket(SystemMessageId.YOU_HAVE_INVITED_THE_WRONG_TARGET);
			return;
		}
		
		if (target.isCursedWeaponEquipped() || requestor.isCursedWeaponEquipped())
		{
			requestor.sendPacket(SystemMessageId.INVALID_TARGET);
			return;
		}
		
		if (target.isJailed() || requestor.isJailed())
		{
			requestor.sendMessage("You cannot invite a player while is in Jail.");
			return;
		}
		
		if (target.isInOlympiadMode() || requestor.isInOlympiadMode())
		{
			if ((target.isInOlympiadMode() != requestor.isInOlympiadMode()) || (target.getOlympiadGameId() != requestor.getOlympiadGameId()) || (target.getOlympiadSide() != requestor.getOlympiadSide()))
			{
				requestor.sendMessage("A user currently participating in the Olympiad cannot send party and friend invitations.");
				return;
			}
		}
		
		if (requestor.isProcessingRequest())
		{
			requestor.sendPacket(SystemMessageId.WAITING_FOR_ANOTHER_REPLY);
			return;
		}
		
		if (target.isProcessingRequest())
		{
			sm = new SystemMessage(SystemMessageId.S1_IS_BUSY_PLEASE_TRY_AGAIN_LATER);
			sm.addString(target.getName());
			requestor.sendPacket(sm);
			return;
		}
		
		final Party party = requestor.getParty();
		if ((party != null) && !party.isLeader(requestor))
		{
			requestor.sendPacket(SystemMessageId.ONLY_THE_LEADER_CAN_GIVE_OUT_INVITATIONS);
			return;
		}
		
		sm = new SystemMessage(SystemMessageId.YOU_HAVE_INVITED_S1_TO_YOUR_PARTY);
		sm.addString(target.getName());
		requestor.sendPacket(sm);
		
		if (!requestor.isInParty())
		{
			createNewParty(target, requestor);
		}
		else
		{
			if (requestor.getParty().isInDimensionalRift())
			{
				requestor.sendMessage("You cannot invite a player when you are in the Dimensional Rift.");
			}
			else
			{
				addTargetToParty(target, requestor);
			}
		}
	}
	
	/**
	 * @param target
	 * @param requestor
	 */
	private void addTargetToParty(Player target, Player requestor)
	{
		final Party party = requestor.getParty();
		
		// summary of ppl already in party and ppl that get invitation
		if (!party.isLeader(requestor))
		{
			requestor.sendPacket(SystemMessageId.ONLY_THE_LEADER_CAN_GIVE_OUT_INVITATIONS);
			return;
		}
		
		if (party.getMemberCount() >= 9)
		{
			requestor.sendPacket(SystemMessageId.THE_PARTY_IS_FULL);
			return;
		}
		
		if (party.getPendingInvitation() && !party.isInvitationRequestExpired())
		{
			requestor.sendPacket(SystemMessageId.WAITING_FOR_ANOTHER_REPLY);
			return;
		}
		
		if (!target.isProcessingRequest())
		{
			requestor.onTransactionRequest(target);
			
			// in case a leader change has happened, use party's mode
			target.sendPacket(new AskJoinParty(requestor.getName(), party.getDistributionType()));
			party.setPendingInvitation(true);
		}
		else
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.S1_IS_BUSY_PLEASE_TRY_AGAIN_LATER);
			sm.addString(target.getName());
			requestor.sendPacket(sm);
		}
	}
	
	/**
	 * @param target
	 * @param requestor
	 */
	private void createNewParty(Player target, Player requestor)
	{
		final PartyDistributionType partyDistributionType = PartyDistributionType.findById(_partyDistributionTypeId);
		if (partyDistributionType == null)
		{
			return;
		}
		
		if (!target.isProcessingRequest())
		{
			target.sendPacket(new AskJoinParty(requestor.getName(), partyDistributionType));
			target.setActiveRequester(requestor);
			requestor.onTransactionRequest(target);
			requestor.setPartyDistributionType(partyDistributionType);
		}
		else
		{
			requestor.sendPacket(SystemMessageId.WAITING_FOR_ANOTHER_REPLY);
		}
	}
}
