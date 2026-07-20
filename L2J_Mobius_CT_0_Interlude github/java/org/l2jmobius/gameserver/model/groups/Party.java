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

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.config.custom.ClassBalanceConfig;
import org.l2jmobius.gameserver.config.custom.PremiumSystemConfig;
import org.l2jmobius.gameserver.managers.DuelManager;
import org.l2jmobius.gameserver.managers.ItemManager;
import org.l2jmobius.gameserver.managers.PcCafePointsManager;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.groups.matching.PartyMatchRoomList;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.sevensigns.DimensionalRift;
import org.l2jmobius.gameserver.model.sevensigns.SevenSignsFestival;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExCloseMPCC;
import org.l2jmobius.gameserver.network.serverpackets.ExOpenMPCC;
import org.l2jmobius.gameserver.network.serverpackets.PartyMemberPosition;
import org.l2jmobius.gameserver.network.serverpackets.PartySmallWindowAdd;
import org.l2jmobius.gameserver.network.serverpackets.PartySmallWindowAll;
import org.l2jmobius.gameserver.network.serverpackets.PartySmallWindowDelete;
import org.l2jmobius.gameserver.network.serverpackets.PartySmallWindowDeleteAll;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * This class serves as a container for player parties.
 * @author nuocnam
 */
public class Party extends AbstractPlayerGroup
{
	private static final Logger LOGGER = Logger.getLogger(Party.class.getName());
	
	// @formatter:off
	private static final double[] BONUS_EXP_SP =
	{
		1.0, 1.10, 1.20, 1.30, 1.40, 1.50, 2.0, 2.10, 2.20
	};
	// @formatter:on
	
	private static final Duration PARTY_POSITION_BROADCAST_INTERVAL = Duration.ofSeconds(12);
	
	private final List<Player> _members = new CopyOnWriteArrayList<>();
	private boolean _pendingInvitation = false;
	private long _pendingInviteTimeout;
	private int _partyLvl = 0;
	private PartyDistributionType _distributionType = PartyDistributionType.FINDERS_KEEPERS;
	private int _itemLastLoot = 0;
	private CommandChannel _commandChannel = null;
	private DimensionalRift _dr;
	private Future<?> _positionBroadcastTask = null;
	protected PartyMemberPosition _positionPacket;
	private boolean _disbanding = false;
	
	/**
	 * Construct a new Party object with a single member - the leader.
	 * @param leader the leader of this party
	 * @param partyDistributionType the item distribution rule of this party
	 */
	public Party(Player leader, PartyDistributionType partyDistributionType)
	{
		_members.add(leader);
		_partyLvl = leader.getLevel();
		_distributionType = partyDistributionType;
	}
	
	/**
	 * Check if another player can start invitation process.
	 * @return {@code true} if this party waits for a response on an invitation, {@code false} otherwise
	 */
	public boolean getPendingInvitation()
	{
		return _pendingInvitation;
	}
	
	/**
	 * Set invitation process flag and store time for expiration.<br>
	 * Happens when a player joins party or declines to join.
	 * @param value the pending invitation state to set
	 */
	public void setPendingInvitation(boolean value)
	{
		_pendingInvitation = value;
		_pendingInviteTimeout = GameTimeTaskManager.getInstance().getGameTicks() + (Player.REQUEST_TIMEOUT * GameTimeTaskManager.TICKS_PER_SECOND);
	}
	
	/**
	 * Check if a player invitation request is expired.
	 * @return {@code true} if time is expired, {@code false} otherwise
	 * @see org.l2jmobius.gameserver.model.actor.Player#isRequestExpired()
	 */
	public boolean isInvitationRequestExpired()
	{
		return _pendingInviteTimeout <= GameTimeTaskManager.getInstance().getGameTicks();
	}
	
	/**
	 * Get a random member from this party.
	 * @param itemId the ID of the item for which the member must have inventory space
	 * @param target the object of which the member must be within a certain range (must not be null)
	 * @return a random member from this party or {@code null} if none of the members have inventory space for the specified item
	 */
	private Player getCheckedRandomMember(int itemId, Creature target)
	{
		final List<Player> availableMembers = new ArrayList<>();
		for (Player member : _members)
		{
			if (member.getInventory().validateCapacityByItemId(itemId) && LocationUtil.checkIfInRange(PlayerConfig.ALT_PARTY_RANGE, target, member, true))
			{
				availableMembers.add(member);
			}
		}
		
		return !availableMembers.isEmpty() ? availableMembers.get(Rnd.get(availableMembers.size())) : null;
	}
	
	/**
	 * get next item looter
	 * @param itemId
	 * @param target
	 * @return
	 */
	private Player getCheckedNextLooter(int itemId, Creature target)
	{
		for (int i = 0; i < getMemberCount(); i++)
		{
			if (++_itemLastLoot >= getMemberCount())
			{
				_itemLastLoot = 0;
			}
			
			Player member;
			try
			{
				member = _members.get(_itemLastLoot);
				if (member.getInventory().validateCapacityByItemId(itemId) && LocationUtil.checkIfInRange(PlayerConfig.ALT_PARTY_RANGE, target, member, true))
				{
					return member;
				}
			}
			catch (Exception e)
			{
				// continue, take another member if this just logged off
			}
		}
		
		return null;
	}
	
	/**
	 * get next item looter
	 * @param player
	 * @param itemId
	 * @param spoil
	 * @param target
	 * @return
	 */
	private Player getActualLooter(Player player, int itemId, boolean spoil, Creature target)
	{
		Player looter = null;
		
		switch (_distributionType)
		{
			case RANDOM:
			{
				if (!spoil)
				{
					looter = getCheckedRandomMember(itemId, target);
				}
				break;
			}
			case RANDOM_INCLUDING_SPOIL:
			{
				looter = getCheckedRandomMember(itemId, target);
				break;
			}
			case BY_TURN:
			{
				if (!spoil)
				{
					looter = getCheckedNextLooter(itemId, target);
				}
				break;
			}
			case BY_TURN_INCLUDING_SPOIL:
			{
				looter = getCheckedNextLooter(itemId, target);
				break;
			}
		}
		
		return looter != null ? looter : player;
	}
	
	/**
	 * Broadcasts UI update and User Info for new party leader.
	 */
	public void broadcastToPartyMembersNewLeader()
	{
		for (Player member : _members)
		{
			if (member != null)
			{
				member.sendPacket(PartySmallWindowDeleteAll.STATIC_PACKET);
				member.sendPacket(new PartySmallWindowAll(member, this));
				member.broadcastUserInfo();
			}
		}
	}
	
	/**
	 * Send a Server->Client packet to all other Player of the Party.
	 * @param player
	 * @param packet
	 */
	public void broadcastToPartyMembers(Player player, ServerPacket packet)
	{
		for (Player member : _members)
		{
			if ((member != null) && (member.getObjectId() != player.getObjectId()))
			{
				member.sendPacket(packet);
			}
		}
	}
	
	/**
	 * adds new member to party
	 * @param player
	 */
	public void addPartyMember(Player player)
	{
		if (_members.contains(player))
		{
			return;
		}
		
		// add player to party
		_members.add(player);
		
		// sends new member party window for all members
		// we do all actions before adding member to a list, this speeds things up a little
		player.sendPacket(new PartySmallWindowAll(player, this));
		
		// sends pets/summons of party members
		// for (Player pMember : _members)
		// {
		// if ((pMember != null) && pMember.hasSummon())
		// {
		// player.sendPacket(new ExPartyPetWindowAdd(pMember.getSummon()));
		// }
		// }
		
		SystemMessage msg = new SystemMessage(SystemMessageId.YOU_HAVE_JOINED_S1_S_PARTY);
		msg.addString(getLeader().getName());
		player.sendPacket(msg);
		
		msg = new SystemMessage(SystemMessageId.S1_HAS_JOINED_THE_PARTY);
		msg.addString(player.getName());
		broadcastToPartyMembers(player, msg);
		
		for (Player member : _members)
		{
			if (member != player)
			{
				member.sendPacket(new PartySmallWindowAdd(player, this));
			}
		}
		
		// send the position of all party members to the new party member
		// player.sendPacket(new PartyMemberPosition(this));
		// send the position of the new party member to all party members (except the new one - he knows his own position)
		// broadcastToPartyMembers(player, new PartyMemberPosition(this));
		
		// if member has pet/summon add it to other as well
		// if (player.hasSummon())
		// {
		// broadcastPacket(new ExPartyPetWindowAdd(player.getSummon()));
		// }
		
		// adjust party level
		if (player.getLevel() > _partyLvl)
		{
			_partyLvl = player.getLevel();
		}
		
		// update partySpelled
		Summon summon;
		for (Player member : _members)
		{
			if (member != null)
			{
				member.updateEffectIcons(true); // update party icons only
				summon = member.getSummon();
				member.broadcastUserInfo();
				if (summon != null)
				{
					summon.updateEffectIcons();
				}
			}
		}
		
		if (isInDimensionalRift())
		{
			_dr.partyMemberInvited();
		}
		
		// open the CCInformationwindow
		if (isInCommandChannel())
		{
			player.sendPacket(ExOpenMPCC.STATIC_PACKET);
		}
		
		if (_positionBroadcastTask == null)
		{
			_positionBroadcastTask = ThreadPool.scheduleAtFixedRate(() ->
			{
				if (_positionPacket == null)
				{
					_positionPacket = new PartyMemberPosition(this);
				}
				else
				{
					_positionPacket.reuse(this);
				}
				
				broadcastPacket(_positionPacket);
			}, PARTY_POSITION_BROADCAST_INTERVAL.toMillis() / 2, PARTY_POSITION_BROADCAST_INTERVAL.toMillis());
		}
	}
	
	/**
	 * Removes a party member using its name.
	 * @param name player the player to be removed from the party.
	 * @param type the message type {@link PartyMessageType}.
	 */
	public void removePartyMember(String name, PartyMessageType type)
	{
		removePartyMember(getPlayerByName(name), type);
	}
	
	/**
	 * Removes a party member instance.
	 * @param player the player to be removed from the party.
	 * @param type the message type {@link PartyMessageType}.
	 */
	public void removePartyMember(Player player, PartyMessageType type)
	{
		if (_members.contains(player))
		{
			final boolean isLeader = isLeader(player);
			if (!_disbanding && ((_members.size() == 2) || (isLeader && !PlayerConfig.ALT_LEAVE_PARTY_LEADER && (type != PartyMessageType.DISCONNECTED))))
			{
				disbandParty();
				return;
			}
			
			_members.remove(player);
			recalculatePartyLevel();
			
			if (player.isFestivalParticipant())
			{
				SevenSignsFestival.getInstance().updateParticipants(player, this);
			}
			
			if (player.isInDuel())
			{
				DuelManager.getInstance().onRemoveFromParty(player);
			}
			
			try
			{
				// Channeling a player!
				if (player.isChanneling() && (player.getSkillChannelizer().hasChannelized()))
				{
					player.abortCast();
				}
				else if (player.isChannelized())
				{
					player.getSkillChannelized().abortChannelization();
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "", e);
			}
			
			SystemMessage msg;
			if (type == PartyMessageType.EXPELLED)
			{
				player.sendPacket(SystemMessageId.YOU_HAVE_BEEN_EXPELLED_FROM_THE_PARTY);
				msg = new SystemMessage(SystemMessageId.S1_WAS_EXPELLED_FROM_THE_PARTY);
				msg.addString(player.getName());
				broadcastPacket(msg);
			}
			else if ((type == PartyMessageType.LEFT) || (type == PartyMessageType.DISCONNECTED))
			{
				player.sendPacket(SystemMessageId.YOU_HAVE_WITHDRAWN_FROM_THE_PARTY);
				msg = new SystemMessage(SystemMessageId.S1_HAS_LEFT_THE_PARTY);
				msg.addString(player.getName());
				broadcastPacket(msg);
			}
			
			// UI update.
			player.sendPacket(PartySmallWindowDeleteAll.STATIC_PACKET);
			player.setParty(null);
			broadcastPacket(new PartySmallWindowDelete(player));
			
			// if (player.hasSummon())
			// {
			// broadcastPacket(new ExPartyPetWindowDelete(player.getSummon()));
			// }
			
			if (isInDimensionalRift())
			{
				_dr.partyMemberExited(player);
			}
			
			// Close the CCInfoWindow
			if (isInCommandChannel())
			{
				player.sendPacket(new ExCloseMPCC());
			}
			
			if (isLeader && (_members.size() > 1) && (PlayerConfig.ALT_LEAVE_PARTY_LEADER || (type == PartyMessageType.DISCONNECTED)))
			{
				msg = new SystemMessage(SystemMessageId.S1_HAS_BECOME_THE_PARTY_LEADER);
				msg.addString(getLeader().getName());
				broadcastPacket(msg);
				broadcastToPartyMembersNewLeader();
			}
			else if (_members.size() == 1)
			{
				if (isInCommandChannel())
				{
					// delete the whole command channel when the party who opened the channel is disbanded
					if (_commandChannel.getLeader().getObjectId() == getLeader().getObjectId())
					{
						_commandChannel.disbandChannel();
					}
					else
					{
						_commandChannel.removeParty(this);
					}
				}
				
				final Player leader = getLeader();
				if (leader != null)
				{
					leader.setParty(null);
					if (leader.isInDuel())
					{
						DuelManager.getInstance().onRemoveFromParty(leader);
					}
				}
				
				if (_positionBroadcastTask != null)
				{
					_positionBroadcastTask.cancel(false);
					_positionBroadcastTask = null;
				}
				
				_members.clear();
			}
		}
	}
	
	/**
	 * Disperse a party and send a message to all its members.
	 */
	public void disbandParty()
	{
		_disbanding = true;
		broadcastPacket(new SystemMessage(SystemMessageId.THE_PARTY_HAS_DISPERSED));
		for (Player member : _members)
		{
			if (member != null)
			{
				removePartyMember(member, PartyMessageType.NONE);
			}
		}
	}
	
	/**
	 * Change party leader (used for string arguments)
	 * @param name the name of the player to set as the new party leader
	 */
	public void changePartyLeader(String name)
	{
		setLeader(getPlayerByName(name));
	}
	
	@Override
	public void setLeader(Player player)
	{
		if ((player != null) && !player.isInDuel())
		{
			if (_members.contains(player))
			{
				if (isLeader(player))
				{
					player.sendPacket(SystemMessageId.SLOW_DOWN_YOU_ARE_ALREADY_THE_PARTY_LEADER);
				}
				else
				{
					// Swap party members
					final Player temp = getLeader();
					final int p1 = _members.indexOf(player);
					_members.set(0, player);
					_members.set(p1, temp);
					SystemMessage msg = new SystemMessage(SystemMessageId.S1_HAS_BECOME_THE_PARTY_LEADER);
					msg.addString(getLeader().getName());
					broadcastPacket(msg);
					broadcastToPartyMembersNewLeader();
					if (isInCommandChannel() && _commandChannel.isLeader(temp))
					{
						_commandChannel.setLeader(getLeader());
						msg = new SystemMessage(SystemMessageId.COMMAND_CHANNEL_AUTHORITY_HAS_BEEN_TRANSFERRED_TO_S1);
						msg.addString(_commandChannel.getLeader().getName());
						_commandChannel.broadcastPacket(msg);
					}
					
					if (player.isInPartyMatchRoom())
					{
						PartyMatchRoomList.getInstance().getPlayerRoom(player).changeLeader(player);
					}
				}
			}
			else
			{
				player.sendPacket(SystemMessageId.YOU_MAY_ONLY_TRANSFER_PARTY_LEADERSHIP_TO_ANOTHER_MEMBER_OF_THE_PARTY);
			}
		}
	}
	
	/**
	 * Finds a player in the party by their name.
	 * @param name The name of the player to find.
	 * @return The player with the matching name, or {@code null} if no player with that name is in the party.
	 */
	public Player getPlayerByName(String name)
	{
		for (Player member : _members)
		{
			if (member.getName().equalsIgnoreCase(name))
			{
				return member;
			}
		}
		
		return null;
	}
	
	/**
	 * Distributes an item to party members.
	 * @param player The player initiating the distribution.
	 * @param item The item to be distributed.
	 */
	public void distributeItem(Player player, Item item)
	{
		if (item.getId() == Inventory.ADENA_ID)
		{
			distributeAdena(player, item.getCount(), player);
			ItemManager.destroyItem(ItemProcessType.LOOT, item, player, null);
			return;
		}
		
		final Player target = getActualLooter(player, item.getId(), false, player);
		target.addItem(ItemProcessType.LOOT, item, player, true);
		
		// Send messages to other party members about reward
		if (item.getCount() > 1)
		{
			final SystemMessage msg = new SystemMessage(SystemMessageId.S1_HAS_OBTAINED_S3_S2);
			msg.addString(target.getName());
			msg.addItemName(item);
			msg.addInt(item.getCount());
			broadcastToPartyMembers(target, msg);
		}
		else
		{
			final SystemMessage msg = new SystemMessage(SystemMessageId.S1_HAS_OBTAINED_S2);
			msg.addString(target.getName());
			msg.addItemName(item);
			broadcastToPartyMembers(target, msg);
		}
	}
	
	/**
	 * Distributes item loot between party members.
	 * @param player the reference player
	 * @param itemId the item ID
	 * @param itemCount the item count
	 * @param spoil {@code true} if it's spoil loot
	 * @param target the NPC target
	 */
	public void distributeItem(Player player, int itemId, int itemCount, boolean spoil, Attackable target)
	{
		if (itemId == Inventory.ADENA_ID)
		{
			distributeAdena(player, itemCount, target);
			return;
		}
		
		final Player looter = getActualLooter(player, itemId, spoil, target);
		looter.addItem(spoil ? ItemProcessType.SWEEP : ItemProcessType.LOOT, itemId, itemCount, target, true);
		
		// Send messages to other party members about reward
		if (itemCount > 1)
		{
			final SystemMessage msg = spoil ? new SystemMessage(SystemMessageId.S1_HAS_OBTAINED_S3_S2_BY_USING_SWEEPER) : new SystemMessage(SystemMessageId.S1_HAS_OBTAINED_S3_S2);
			msg.addString(looter.getName());
			msg.addItemName(itemId);
			msg.addInt(itemCount);
			broadcastToPartyMembers(looter, msg);
		}
		else
		{
			final SystemMessage msg = spoil ? new SystemMessage(SystemMessageId.S1_HAS_OBTAINED_S2_BY_USING_SWEEPER) : new SystemMessage(SystemMessageId.S1_HAS_OBTAINED_S2);
			msg.addString(looter.getName());
			msg.addItemName(itemId);
			broadcastToPartyMembers(looter, msg);
		}
	}
	
	/**
	 * Method overload for {@link Party#distributeItem(Player, int, int, boolean, Attackable)}
	 * @param player the reference player
	 * @param item the item holder
	 * @param spoil {@code true} if it's spoil loot
	 * @param target the NPC target
	 */
	public void distributeItem(Player player, ItemHolder item, boolean spoil, Attackable target)
	{
		distributeItem(player, item.getId(), item.getCount(), spoil, target);
	}
	
	/**
	 * Distribute adena to party members who are in range of the target.
	 * @param player The player responsible for the kill or loot (used as reference).
	 * @param adena Total amount of adena to distribute.
	 * @param target The target creature (used for range checking).
	 */
	public void distributeAdena(Player player, int adena, Creature target)
	{
		// List to collect party members eligible to receive adena.
		final List<Player> toReward = new LinkedList<>();
		
		// Iterate over all party members to find those within the allowed party range of the target.
		for (Player member : _members)
		{
			if (LocationUtil.checkIfInRange(PlayerConfig.ALT_PARTY_RANGE, target, member, true))
			{
				toReward.add(member); // Add eligible member to the reward list.
			}
		}
		
		// If no members are eligible or there is no adena to distribute, exit early.
		if (toReward.isEmpty() || (adena <= 0))
		{
			return;
		}
		
		final int baseShare = adena / toReward.size(); // Calculate the base share of adena each eligible member will receive.
		final int remainder = adena % toReward.size(); // Calculate the leftover adena that can't be evenly split (remainder).
		final Map<Player, Integer> adenaDistribution = new HashMap<>(); // Map to track how much adena each member will receive.
		
		// Assign the base share to every eligible member initially.
		for (Player member : toReward)
		{
			adenaDistribution.put(member, baseShare);
		}
		
		// Distribute the leftover adena (1 adena each) to some members starting from a random index.
		if (remainder > 0)
		{
			final int randomIndex = Rnd.get(toReward.size());
			for (int i = 0; i < remainder; i++)
			{
				final Player member = toReward.get((randomIndex + i) % toReward.size());
				adenaDistribution.put(member, adenaDistribution.get(member) + 1); // Increase the member's share by 1 to include remainder.
			}
		}
		
		// Finally, give each member their total calculated adena amount.
		for (Entry<Player, Integer> entry : adenaDistribution.entrySet())
		{
			final int amount = entry.getValue();
			if (amount > 0)
			{
				// Add adena to the player's inventory and send message.
				entry.getKey().addAdena(ItemProcessType.LOOT, amount, player, true);
			}
		}
	}
	
	/**
	 * Distribute Experience and SP rewards to Player Party members in the known area of the last attacker.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <li>Get the Player owner of the Servitor (if necessary)</li>
	 * <li>Calculate the Experience and SP reward distribution rate</li>
	 * <li>Add Experience and SP to the Player</li><br>
	 * @param xpRewardValue The Experience reward to distribute
	 * @param spRewardValue The SP reward to distribute
	 * @param rewardedMembers The list of Player to reward
	 * @param topLvl
	 * @param partyDmg
	 * @param target
	 */
	public void distributeXpAndSp(double xpRewardValue, double spRewardValue, List<Player> rewardedMembers, int topLvl, long partyDmg, Attackable target)
	{
		final List<Player> validMembers = getValidMembers(rewardedMembers, topLvl, target);
		double xpReward = xpRewardValue * getExpBonus(validMembers.size());
		double spReward = spRewardValue * getSpBonus(validMembers.size());
		int sqLevelSum = 0;
		int averagePartyMemberLevel = 0;
		for (Player member : validMembers)
		{
			sqLevelSum += member.getLevel() * member.getLevel();
			averagePartyMemberLevel += member.getLevel();
		}
		
		averagePartyMemberLevel /= Math.max(1, validMembers.size());
		
		final float vitalityPoints = (target.getVitalityPoints(averagePartyMemberLevel, partyDmg) * RatesConfig.RATE_PARTY_XP) / validMembers.size();
		final boolean useVitalityRate = target.useVitalityRate();
		for (Player member : rewardedMembers)
		{
			if (member.isDead())
			{
				continue;
			}
			
			// Calculate and add the EXP and SP reward to the member
			if (validMembers.contains(member))
			{
				// The servitor penalty
				final float penalty = member.hasServitor() ? member.getSummon().asServitor().getExpMultiplier() : 1;
				final double sqLevel = member.getLevel() * member.getLevel();
				final double preCalculation = (sqLevel / sqLevelSum) * penalty;
				
				// Add the XP/SP points to the requested party member
				double addexp = Math.round(member.calcStat(Stat.EXPSP_RATE, xpReward * preCalculation, null, null));
				final double addsp = member.calcStat(Stat.EXPSP_RATE, spReward * preCalculation, null, null);
				addexp = calculateExpSpPartyCutoff(member.asPlayer(), topLvl, addexp, addsp, useVitalityRate);
				if (addexp > 0)
				{
					member.updateVitalityPoints(vitalityPoints, true, false);
					PcCafePointsManager.getInstance().givePcCafePoint(member, addexp);
				}
			}
			else
			{
				member.addExpAndSp(0, 0);
			}
		}
	}
	
	private double calculateExpSpPartyCutoff(Player player, int topLvl, double addExpValue, double addSpValue, boolean vit)
	{
		double addExp = addExpValue * ClassBalanceConfig.EXP_AMOUNT_MULTIPLIERS[player.getPlayerClass().getId()];
		double addSp = addSpValue * ClassBalanceConfig.SP_AMOUNT_MULTIPLIERS[player.getPlayerClass().getId()];
		
		// Premium rates
		if (player.hasPremiumStatus())
		{
			addExp *= PremiumSystemConfig.PREMIUM_RATE_XP;
			addSp *= PremiumSystemConfig.PREMIUM_RATE_SP;
		}
		
		double xp = addExp;
		double sp = addSp;
		if (PlayerConfig.PARTY_XP_CUTOFF_METHOD == PartyExpType.HIGHFIVE)
		{
			int i = 0;
			final int levelDiff = topLvl - player.getLevel();
			for (int[] gap : PlayerConfig.PARTY_XP_CUTOFF_GAPS)
			{
				if ((levelDiff >= gap[0]) && (levelDiff <= gap[1]))
				{
					xp = (addExp * PlayerConfig.PARTY_XP_CUTOFF_GAP_PERCENTS[i]) / 100;
					sp = (addSp * PlayerConfig.PARTY_XP_CUTOFF_GAP_PERCENTS[i]) / 100;
					player.addExpAndSp(xp, sp, vit);
					break;
				}
				
				i++;
			}
		}
		else
		{
			player.addExpAndSp(addExp, addSp, vit);
		}
		
		return xp;
	}
	
	/**
	 * refresh party level
	 */
	public void recalculatePartyLevel()
	{
		int newLevel = 0;
		for (Player member : _members)
		{
			if (member == null)
			{
				_members.remove(member);
				continue;
			}
			
			if (member.getLevel() > newLevel)
			{
				newLevel = member.getLevel();
			}
		}
		
		_partyLvl = newLevel;
	}
	
	private List<Player> getValidMembers(List<Player> members, int topLvl, Attackable target)
	{
		// Phantom party members (buddies / recruited fake players) are clientless and must not take a share of
		// XP/SP - otherwise they'd both steal a cut and shrink the real player's by inflating the level-sum split.
		// Exclude them whenever at least one real (client-connected) player remains to be rewarded.
		boolean hasRealMember = false;
		for (Player member : members)
		{
			if (member.getClient() != null)
			{
				hasRealMember = true;
				break;
			}
		}
		if (hasRealMember)
		{
			final List<Player> realMembers = new ArrayList<>(members.size());
			for (Player member : members)
			{
				if (member.getClient() != null)
				{
					realMembers.add(member);
				}
			}
			members = realMembers;
		}

		final List<Player> validMembers = new ArrayList<>();
		switch (PlayerConfig.PARTY_XP_CUTOFF_METHOD)
		{
			case LEVEL:
			{
				for (Player member : members)
				{
					if ((target.getInstanceId() == member.getInstanceId()) && ((topLvl - member.getLevel()) <= PlayerConfig.PARTY_XP_CUTOFF_LEVEL))
					{
						validMembers.add(member);
					}
				}
				break;
			}
			case PERCENTAGE:
			{
				int sqLevelSum = 0;
				for (Player member : members)
				{
					if (target.getInstanceId() == member.getInstanceId())
					{
						sqLevelSum += (member.getLevel() * member.getLevel());
					}
				}
				
				for (Player member : members)
				{
					final int sqLevel = member.getLevel() * member.getLevel();
					if ((target.getInstanceId() == member.getInstanceId()) && ((sqLevel * 100) >= (sqLevelSum * PlayerConfig.PARTY_XP_CUTOFF_PERCENT)))
					{
						validMembers.add(member);
					}
				}
				break;
			}
			case AUTO:
			{
				int sqLevelSum = 0;
				for (Player member : members)
				{
					sqLevelSum += (member.getLevel() * member.getLevel());
				}
				
				int i = members.size() - 1;
				if (i < 1)
				{
					return members;
				}
				
				if (i >= BONUS_EXP_SP.length)
				{
					i = BONUS_EXP_SP.length - 1;
				}
				
				for (Player member : members)
				{
					final int sqLevel = member.getLevel() * member.getLevel();
					if ((target.getInstanceId() == member.getInstanceId()) && (sqLevel >= (sqLevelSum / (members.size() * members.size()))))
					{
						validMembers.add(member);
					}
				}
				break;
			}
			case HIGHFIVE:
			{
				for (Player member : members)
				{
					if (target.getInstanceId() == member.getInstanceId())
					{
						validMembers.add(member);
					}
				}
				break;
			}
			case NONE:
			{
				for (Player member : members)
				{
					if (target.getInstanceId() == member.getInstanceId())
					{
						validMembers.add(member);
					}
				}
				break;
			}
		}
		
		return validMembers;
	}
	
	private double getBaseExpSpBonus(int membersCount)
	{
		int i = membersCount - 1;
		if (i < 1)
		{
			return 1;
		}
		
		if (i >= BONUS_EXP_SP.length)
		{
			i = BONUS_EXP_SP.length - 1;
		}
		
		return BONUS_EXP_SP[i];
	}
	
	private double getExpBonus(int membersCount)
	{
		return (membersCount < 2) ? getBaseExpSpBonus(membersCount) : (getBaseExpSpBonus(membersCount) * RatesConfig.RATE_PARTY_XP);
	}
	
	private double getSpBonus(int membersCount)
	{
		return (membersCount < 2) ? getBaseExpSpBonus(membersCount) : (getBaseExpSpBonus(membersCount) * RatesConfig.RATE_PARTY_SP);
	}
	
	@Override
	public int getLevel()
	{
		return _partyLvl;
	}
	
	public PartyDistributionType getDistributionType()
	{
		return _distributionType;
	}
	
	public boolean isInCommandChannel()
	{
		return _commandChannel != null;
	}
	
	public CommandChannel getCommandChannel()
	{
		return _commandChannel;
	}
	
	public void setCommandChannel(CommandChannel channel)
	{
		_commandChannel = channel;
	}
	
	public boolean isInDimensionalRift()
	{
		return _dr != null;
	}
	
	public void setDimensionalRift(DimensionalRift dr)
	{
		_dr = dr;
	}
	
	public DimensionalRift getDimensionalRift()
	{
		return _dr;
	}
	
	/**
	 * @return the leader of this party
	 */
	@Override
	public Player getLeader()
	{
		if (_members.isEmpty())
		{
			return null;
		}
		
		final Player leader;
		try
		{
			leader = _members.get(0);
		}
		catch (Exception e)
		{
			return null;
		}
		
		return leader;
	}
	
	/**
	 * @return a list of all members of this party
	 */
	@Override
	public List<Player> getMembers()
	{
		return _members;
	}
	
	/**
	 * Check whether the leader of this party is the same as the leader of the specified party (which essentially means they're the same group).
	 * @param party the other party to check against
	 * @return {@code true} if this party equals the specified party, {@code false} otherwise
	 */
	public boolean equals(Party party)
	{
		return (party != null) && (getLeaderObjectId() == party.getLeaderObjectId());
	}
}
