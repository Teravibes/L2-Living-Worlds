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
package org.l2jmobius.gameserver.model.groups;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExCloseMPCC;
import org.l2jmobius.gameserver.network.serverpackets.ExOpenMPCC;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * This class serves as a container for command channels.
 * @author chris_00
 */
public class CommandChannel extends AbstractPlayerGroup
{
	private final Collection<Party> _parties = ConcurrentHashMap.newKeySet();
	private Player _commandLeader;
	private int _channelLvl;
	
	/**
	 * Create a new command channel and add the leader's party to it.
	 * @param leader the leader of this command channel
	 */
	public CommandChannel(Player leader)
	{
		_commandLeader = leader;
		final Party party = leader.getParty();
		_parties.add(party);
		_channelLvl = party.getLevel();
		party.setCommandChannel(this);
		party.broadcastMessage(SystemMessageId.THE_COMMAND_CHANNEL_HAS_BEEN_FORMED);
		party.broadcastPacket(ExOpenMPCC.STATIC_PACKET);
	}
	
	/**
	 * Add a party to this command channel.
	 * @param party the party to add
	 */
	public void addParty(Party party)
	{
		if (party == null)
		{
			return;
		}
		
		// Update the CCinfo for existing players
		// broadcastPacket(new ExMPCCPartyInfoUpdate(party, 1));
		_parties.add(party);
		if (party.getLevel() > _channelLvl)
		{
			_channelLvl = party.getLevel();
		}
		
		party.setCommandChannel(this);
		party.broadcastPacket(new SystemMessage(SystemMessageId.YOU_HAVE_JOINED_THE_COMMAND_CHANNEL));
		party.broadcastPacket(ExOpenMPCC.STATIC_PACKET);
	}
	
	/**
	 * Remove a party from this command channel.
	 * @param party the party to remove
	 */
	public void removeParty(Party party)
	{
		if (party == null)
		{
			return;
		}
		
		_parties.remove(party);
		_channelLvl = 0;
		for (Party pty : _parties)
		{
			if (pty.getLevel() > _channelLvl)
			{
				_channelLvl = pty.getLevel();
			}
		}
		
		party.setCommandChannel(null);
		party.broadcastPacket(new ExCloseMPCC());
		if (_parties.size() < 2)
		{
			broadcastPacket(new SystemMessage(SystemMessageId.THE_COMMAND_CHANNEL_HAS_BEEN_DISBANDED));
			disbandChannel();
		}
		// else
		// {
		// Update the CCinfo for existing players
		// broadcastPacket(new ExMPCCPartyInfoUpdate(party, 0));
		// }
	}
	
	/**
	 * Disband this command channel.
	 */
	public void disbandChannel()
	{
		if (_parties != null)
		{
			for (Party party : _parties)
			{
				if (party != null)
				{
					removeParty(party);
				}
			}
			
			_parties.clear();
		}
	}
	
	/**
	 * @return the total count of all members of this command channel
	 */
	@Override
	public int getMemberCount()
	{
		int count = 0;
		for (Party party : _parties)
		{
			if (party != null)
			{
				count += party.getMemberCount();
			}
		}
		
		return count;
	}
	
	/**
	 * @return a list of all parties in this command channel
	 */
	public Collection<Party> getParties()
	{
		return _parties;
	}
	
	/**
	 * @return a list of all members in this command channel
	 */
	@Override
	public List<Player> getMembers()
	{
		final List<Player> members = new LinkedList<>();
		for (Party party : _parties)
		{
			members.addAll(party.getMembers());
		}
		
		return members;
	}
	
	/**
	 * @return the level of this command channel (equals the level of the highest-leveled character in this command channel)
	 */
	@Override
	public int getLevel()
	{
		return _channelLvl;
	}
	
	@Override
	public void setLeader(Player leader)
	{
		_commandLeader = leader;
		if (leader.getLevel() > _channelLvl)
		{
			_channelLvl = leader.getLevel();
		}
	}
	
	/**
	 * @param obj
	 * @return true if proper condition for RaidWar
	 */
	public boolean meetRaidWarCondition(WorldObject obj)
	{
		if (!(obj.isCreature() && obj.asCreature().isRaid()))
		{
			return false;
		}
		
		return (getMemberCount() >= PlayerConfig.LOOT_RAIDS_PRIVILEGE_CC_SIZE);
	}
	
	/**
	 * @return the leader of this command channel
	 */
	@Override
	public Player getLeader()
	{
		return _commandLeader;
	}
	
	/**
	 * Check if a given player is in this command channel.
	 * @param player the player to check
	 * @return {@code true} if he does, {@code false} otherwise
	 */
	@Override
	public boolean containsPlayer(Player player)
	{
		if ((_parties != null) && !_parties.isEmpty())
		{
			for (Party party : _parties)
			{
				if (party.containsPlayer(player))
				{
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Iterates over all command channel members without the need to allocate a new list
	 * @see org.l2jmobius.gameserver.model.groups.AbstractPlayerGroup#forEachMember(Function)
	 */
	@Override
	public boolean forEachMember(Function<Player, Boolean> function)
	{
		if ((_parties != null) && !_parties.isEmpty())
		{
			for (Party party : _parties)
			{
				if (!party.forEachMember(function))
				{
					return false;
				}
			}
		}
		
		return true;
	}
	
	/**
	 * Check whether the leader of this command channel is the same as the leader of the specified command channel<br>
	 * (which essentially means they're the same group).
	 * @param cc the other command channel to check against
	 * @return {@code true} if this command channel equals the specified command channel, {@code false} otherwise
	 */
	public boolean equals(CommandChannel cc)
	{
		return (cc != null) && (getLeaderObjectId() == cc.getLeaderObjectId());
	}
}
