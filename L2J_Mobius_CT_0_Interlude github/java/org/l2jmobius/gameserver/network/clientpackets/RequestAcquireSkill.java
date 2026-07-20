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

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.IllegalActionPunishmentType;
import org.l2jmobius.gameserver.model.actor.instance.Fisherman;
import org.l2jmobius.gameserver.model.actor.instance.Folk;
import org.l2jmobius.gameserver.model.actor.instance.VillageMaster;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerSkillLearn;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.skill.CommonSkill;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.enums.AcquireSkillType;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.model.skill.holders.SkillLearn;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExStorageMaxCount;
import org.l2jmobius.gameserver.network.serverpackets.PledgeSkillList;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Request Acquire Skill client packet implementation.
 * @author Zoey76
 */
public class RequestAcquireSkill extends ClientPacket
{
	private int _id;
	private int _level;
	private AcquireSkillType _skillType;
	
	@Override
	protected void readImpl()
	{
		_id = readInt();
		_level = readInt();
		_skillType = AcquireSkillType.getAcquireSkillType(readInt());
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if ((_level < 1) || (_level > 1000) || (_id < 1))
		{
			PunishmentManager.handleIllegalPlayerAction(player, "Wrong Packet Data in Aquired Skill", GeneralConfig.DEFAULT_PUNISH);
			PacketLogger.warning("Recived Wrong Packet Data in Aquired Skill - id: " + _id + " level: " + _level + " for " + player);
			return;
		}
		
		final Npc trainer = player.getLastFolkNPC();
		if ((trainer == null) || !trainer.isNpc() || (!trainer.canInteract(player) && !player.isGM()))
		{
			return;
		}
		
		final Skill skill = SkillData.getInstance().getSkill(_id, _level);
		if (skill == null)
		{
			PacketLogger.warning(RequestAcquireSkill.class.getSimpleName() + ": " + player + " is trying to learn a null skill Id: " + _id + " level: " + _level + "!");
			return;
		}
		
		final SkillLearn s = SkillTreeData.getInstance().getSkillLearn(_skillType, _id, _level, player);
		if (s == null)
		{
			return;
		}
		
		switch (_skillType)
		{
			case CLASS:
			{
				if (checkPlayerSkill(player, trainer, s))
				{
					giveSkill(player, trainer, skill);
				}
				break;
			}
			case FISHING:
			{
				if (checkPlayerSkill(player, trainer, s))
				{
					giveSkill(player, trainer, skill);
				}
				break;
			}
			case PLEDGE:
			{
				if (!player.isClanLeader())
				{
					return;
				}
				
				final Clan clan = player.getClan();
				final int repCost = s.getLevelUpSp();
				if (clan.getReputationScore() >= repCost)
				{
					if (PlayerConfig.LIFE_CRYSTAL_NEEDED)
					{
						for (ItemHolder item : s.getRequiredItems())
						{
							if (!player.destroyItemByItemId(null, item.getId(), item.getCount(), trainer, false))
							{
								// Doesn't have required item.
								player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_THE_NECESSARY_MATERIALS_OR_PREREQUISITES_TO_LEARN_THIS_SKILL);
								VillageMaster.showPledgeSkillList(player);
								return;
							}
							
							final SystemMessage sm = new SystemMessage(SystemMessageId.S2_S1_HAS_DISAPPEARED);
							sm.addItemName(item.getId());
							sm.addInt(item.getCount());
							player.sendPacket(sm);
						}
					}
					
					clan.takeReputationScore(repCost);
					
					final SystemMessage cr = new SystemMessage(SystemMessageId.S1_POINTS_HAVE_BEEN_DEDUCTED_FROM_THE_CLAN_S_REPUTATION_SCORE);
					cr.addInt(repCost);
					player.sendPacket(cr);
					
					clan.addNewSkill(skill);
					
					clan.broadcastToOnlineMembers(new PledgeSkillList(clan));
					
					// player.sendPacket(new AcquireSkillDone());
					VillageMaster.showPledgeSkillList(player);
				}
				else
				{
					player.sendPacket(SystemMessageId.THE_ATTEMPT_TO_ACQUIRE_THE_SKILL_HAS_FAILED_BECAUSE_OF_AN_INSUFFICIENT_CLAN_REPUTATION_SCORE);
					VillageMaster.showPledgeSkillList(player);
				}
				break;
			}
			default:
			{
				PacketLogger.warning("Recived Wrong Packet Data in Aquired Skill, unknown skill type:" + _skillType);
				break;
			}
		}
	}
	
	/**
	 * Perform a simple check for current player and skill.<br>
	 * Takes the needed SP if the skill require it and all requirements are meet.<br>
	 * Consume required items if the skill require it and all requirements are meet.
	 * @param player the skill learning player.
	 * @param trainer the skills teaching Npc.
	 * @param skillLearn the skill to be learn.
	 * @return {@code true} if all requirements are meet, {@code false} otherwise.
	 */
	private boolean checkPlayerSkill(Player player, Npc trainer, SkillLearn skillLearn)
	{
		if ((skillLearn != null) && (skillLearn.getSkillId() == _id) && (skillLearn.getSkillLevel() == _level))
		{
			// Hack check.
			if (skillLearn.getGetLevel() > player.getLevel())
			{
				player.sendMessage("You do not meet the skill level requirements.");
				PunishmentManager.handleIllegalPlayerAction(player, player + ", level " + player.getLevel() + " is requesting skill Id: " + _id + " level " + _level + " without having minimum required level, " + skillLearn.getGetLevel() + "!", IllegalActionPunishmentType.NONE);
				return false;
			}
			
			// First it checks that the skill require SP and the player has enough SP to learn it.
			final int levelUpSp = skillLearn.getCalculatedLevelUpSp(player.getPlayerClass(), player.getLearningClass());
			if ((levelUpSp > 0) && (levelUpSp > player.getSp()))
			{
				player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_SP_TO_LEARN_THIS_SKILL);
				showSkillList(trainer, player);
				return false;
			}
			
			if (!PlayerConfig.DIVINE_SP_BOOK_NEEDED && (_id == CommonSkill.DIVINE_INSPIRATION.getId()))
			{
				return true;
			}
			
			// Check for required skills.
			if (!skillLearn.getPreReqSkills().isEmpty())
			{
				for (SkillHolder skill : skillLearn.getPreReqSkills())
				{
					if (player.getSkillLevel(skill.getSkillId()) < skill.getSkillLevel())
					{
						player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_THE_NECESSARY_MATERIALS_OR_PREREQUISITES_TO_LEARN_THIS_SKILL);
						return false;
					}
				}
			}
			
			// Check for required items.
			if (!skillLearn.getRequiredItems().isEmpty())
			{
				// Then checks that the player has all the items
				long reqItemCount = 0;
				for (ItemHolder item : skillLearn.getRequiredItems())
				{
					reqItemCount = player.getInventory().getInventoryItemCount(item.getId(), -1);
					if (reqItemCount < item.getCount())
					{
						// Player doesn't have required item.
						player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_THE_NECESSARY_MATERIALS_OR_PREREQUISITES_TO_LEARN_THIS_SKILL);
						showSkillList(trainer, player);
						return false;
					}
				}
				
				// If the player has all required items, they are consumed.
				for (ItemHolder itemIdCount : skillLearn.getRequiredItems())
				{
					if (!player.destroyItemByItemId(ItemProcessType.FEE, itemIdCount.getId(), itemIdCount.getCount(), trainer, true))
					{
						PunishmentManager.handleIllegalPlayerAction(player, "Somehow " + player + ", level " + player.getLevel() + " lose required item Id: " + itemIdCount.getId() + " to learn skill while learning skill Id: " + _id + " level " + _level + "!", IllegalActionPunishmentType.NONE);
					}
				}
			}
			
			// If the player has SP and all required items then consume SP.
			if (levelUpSp > 0)
			{
				player.setSp(player.getSp() - levelUpSp);
				player.updateUserInfo();
			}
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Add the skill to the player and makes proper updates.
	 * @param player the player acquiring a skill.
	 * @param trainer the Npc teaching a skill.
	 * @param skill the skill to be learn.
	 */
	private void giveSkill(Player player, Npc trainer, Skill skill)
	{
		// Send message.
		final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S1_2);
		sm.addSkillName(skill);
		player.sendPacket(sm);
		
		// player.sendPacket(new AcquireSkillDone());
		player.addSkill(skill, true);
		player.sendSkillList();
		
		player.updateShortcuts(_id, _level);
		showSkillList(trainer, player);
		
		// If skill is expand type then sends packet:
		if ((_id >= 1368) && (_id <= 1372))
		{
			player.sendPacket(new ExStorageMaxCount(player));
		}
		
		// Notify scripts of the skill learn.
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_SKILL_LEARN, trainer))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerSkillLearn(trainer, player, skill, _skillType), trainer);
		}
	}
	
	/**
	 * Wrapper for returning the skill list to the player after it's done with current skill.
	 * @param trainer the Npc which the {@code player} is interacting
	 * @param player the active character
	 */
	private void showSkillList(Npc trainer, Player player)
	{
		if (trainer instanceof Fisherman)
		{
			Fisherman.showFishSkillList(player);
		}
		else
		{
			Folk.showSkillList(player, trainer, player.getLearningClass());
		}
	}
}
