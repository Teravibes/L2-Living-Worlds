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

import java.util.logging.Logger;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.xml.EnchantSkillTreeData;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.IllegalActionPunishmentType;
import org.l2jmobius.gameserver.model.actor.enums.player.ShortcutType;
import org.l2jmobius.gameserver.model.actor.holders.player.Shortcut;
import org.l2jmobius.gameserver.model.actor.instance.Folk;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.holders.EnchantSkillLearn;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ShortcutRegister;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Format chdd c: (id) 0xD0 h: (subid) 0x06 d: skill id d: skill level
 * @author -Wooden-
 */
public class RequestExEnchantSkill extends ClientPacket
{
	private static final Logger LOGGER_ENCHANT = Logger.getLogger("enchant.skills");
	
	private int _skillId;
	private int _skillLevel;
	
	@Override
	protected void readImpl()
	{
		_skillId = readInt();
		_skillLevel = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		if (!getClient().getFloodProtectors().canPerformPlayerAction())
		{
			return;
		}
		
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final Npc trainer = player.getLastFolkNPC();
		if (!(trainer instanceof Folk))
		{
			return;
		}
		
		if (!player.isInsideRadius2D(trainer, Npc.INTERACTION_DISTANCE) && !player.isGM())
		{
			return;
		}
		
		if (player.getSkillLevel(_skillId) >= _skillLevel)
		{
			return;
		}
		
		if (player.getPlayerClass().getId() < 88)
		{
			return;
		}
		
		if (player.getLevel() < 76)
		{
			return;
		}
		
		final Skill skill = SkillData.getInstance().getSkill(_skillId, _skillLevel);
		int counts = 0;
		int requiredSp = 10000000;
		int requiredExp = 100000;
		byte rate = 0;
		int baseLevel = 1;
		
		for (EnchantSkillLearn s : EnchantSkillTreeData.getInstance().getAvailableEnchantSkills(player))
		{
			final Skill sk = SkillData.getInstance().getSkill(s.getId(), s.getLevel());
			if ((sk == null) || (sk != skill) || !trainer.getTemplate().canTeach(player.getPlayerClass()))
			{
				continue;
			}
			
			counts++;
			requiredSp = s.getSpCost();
			requiredExp = s.getExp();
			rate = s.getRate(player);
			baseLevel = s.getBaseLevel();
		}
		
		if ((counts == 0) && !PlayerConfig.ALT_GAME_SKILL_LEARN)
		{
			player.sendMessage("You are trying to learn skill that you can't...");
			PunishmentManager.handleIllegalPlayerAction(player, player + " tried to learn skill that he can't!!!", IllegalActionPunishmentType.KICK);
			return;
		}
		
		Item spb = null;
		if (player.getSp() >= requiredSp)
		{
			// Like L2OFF you can't delevel during skill enchant
			final long expAfter = player.getExp() - requiredExp;
			if ((player.getExp() >= requiredExp) && (expAfter >= ExperienceData.getInstance().getExpForLevel(player.getLevel())))
			{
				if (PlayerConfig.ES_SP_BOOK_NEEDED && ((_skillLevel == 101) || (_skillLevel == 141))) // only first level requires book
				{
					spb = player.getInventory().getItemByItemId(6622);
					if (spb == null) // Does not have spellbook.
					{
						player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ALL_OF_THE_ITEMS_NEEDED_TO_ENCHANT_THAT_SKILL);
						return;
					}
					
					// ok
					player.destroyItem(ItemProcessType.FEE, spb, 1, trainer, true);
				}
			}
			else
			{
				player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_EXPERIENCE_EXP_TO_ENCHANT_THAT_SKILL);
				return;
			}
		}
		else
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_SP_TO_ENCHANT_THAT_SKILL);
			return;
		}
		
		if (Rnd.get(100) < rate)
		{
			if (GeneralConfig.LOG_SKILL_ENCHANTS)
			{
				final StringBuilder sb = new StringBuilder();
				LOGGER_ENCHANT.info(sb.append("Success, Character:").append(player.getName()).append(" [").append(player.getObjectId()).append("] Account:").append(player.getAccountName()).append(" IP:").append(player.getIPAddress()).append(", Skill:").append(skill).append(", SPB:").append(spb).append(", Rate:").append(rate).toString());
			}
			
			player.addSkill(skill, true);
			player.getStat().removeExpAndSp(requiredExp, requiredSp);
			
			final StatusUpdate su = new StatusUpdate(player.getObjectId());
			su.addAttribute(StatusUpdate.SP, (int) player.getSp());
			player.sendPacket(su);
			
			final SystemMessage sm = new SystemMessage(SystemMessageId.SKILL_ENCHANT_WAS_SUCCESSFUL_S1_HAS_BEEN_ENCHANTED);
			sm.addSkillName(_skillId);
			player.sendPacket(sm);
		}
		else
		{
			if (skill.getLevel() > 100)
			{
				_skillLevel = baseLevel;
				player.addSkill(SkillData.getInstance().getSkill(_skillId, _skillLevel), true);
				player.sendSkillList();
			}
			
			player.sendPacket(SystemMessageId.SKILL_ENCHANT_FAILED_THE_SKILL_WILL_BE_INITIALIZED);
			
			if (GeneralConfig.LOG_SKILL_ENCHANTS)
			{
				final StringBuilder sb = new StringBuilder();
				LOGGER_ENCHANT.info(sb.append("Failed, Character:").append(player.getName()).append(" [").append(player.getObjectId()).append("] Account:").append(player.getAccountName()).append(" IP:").append(player.getIPAddress()).append(", Skill:").append(skill).append(", SPB:").append(spb).append(", Rate:").append(rate).toString());
			}
		}
		
		((Folk) trainer).showEnchantSkillList(player);
		player.updateUserInfo();
		player.sendSkillList();
		
		// Update all the shortcuts to this skill.
		for (Shortcut sc : player.getAllShortcuts())
		{
			if ((sc.getId() == _skillId) && (sc.getType() == ShortcutType.SKILL))
			{
				final Shortcut newsc = new Shortcut(sc.getSlot(), sc.getPage(), sc.getType(), sc.getId(), _skillLevel);
				player.sendPacket(new ShortcutRegister(newsc));
				player.registerShortcut(newsc);
			}
		}
	}
}
