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
package handlers.bypass.npc;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;

/**
 * @author Mobius
 */
public class SupportMagic implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
		"supportmagic"
	};
	
	// Levels
	private static final int LOWEST_LEVEL = 8;
	private static final int HIGHEST_LEVEL = 25;
	
	@Override
	public boolean onCommand(String command, Player player, Creature target)
	{
		if (!target.isNpc() || player.isCursedWeaponEquipped())
		{
			return false;
		}
		
		if (command.equalsIgnoreCase(COMMANDS[0]))
		{
			final int level = player.getLevel();
			final Npc npc = target.asNpc();
			if (!PlayerConfig.ALT_GAME_NEW_CHAR_ALWAYS_IS_NEWBIE && !player.isNewbie())
			{
				npc.showChatWindow(player, "data/html/default/SupportMagicNovice.htm");
				return false;
			}
			else if (level > HIGHEST_LEVEL)
			{
				npc.showChatWindow(player, "data/html/default/SupportMagicHighLevel.htm");
				return false;
			}
			else if (level < LOWEST_LEVEL)
			{
				npc.showChatWindow(player, "data/html/default/SupportMagicLowLevel.htm");
				return false;
			}
			else if (player.getPlayerClass().level() == 3)
			{
				player.sendMessage("Only adventurers who have not completed their 3rd class transfer may receive these buffs."); // Custom message
				return false;
			}
			
			npc.setTarget(player);
			
			if ((player.getLevel() >= 8) && (player.getLevel() <= 24))
			{
				npc.doCast(SkillData.getInstance().getSkill(4322, 1)); // WindWalk
			}
			
			if ((player.getLevel() >= 11) && (player.getLevel() <= 23))
			{
				npc.doCast(SkillData.getInstance().getSkill(4323, 1)); // Shield
			}
			
			if (player.isInCategory(CategoryType.BEGINNER_MAGE))
			{
				if ((player.getLevel() >= 12) && (player.getLevel() <= 22))
				{
					npc.doCast(SkillData.getInstance().getSkill(4328, 1)); // Bless the Soul
				}
				
				if ((player.getLevel() >= 13) && (player.getLevel() <= 21))
				{
					npc.doCast(SkillData.getInstance().getSkill(4329, 1)); // Acumen
				}
				
				if ((player.getLevel() >= 14) && (player.getLevel() <= 20))
				{
					npc.doCast(SkillData.getInstance().getSkill(4330, 1)); // Concentration
				}
				
				if ((player.getLevel() >= 15) && (player.getLevel() <= 19))
				{
					npc.doCast(SkillData.getInstance().getSkill(4331, 1)); // Empower
				}
			}
			else
			{
				if ((player.getLevel() >= 12) && (player.getLevel() <= 22))
				{
					npc.doCast(SkillData.getInstance().getSkill(4324, 1)); // Bless the Body
				}
				
				if ((player.getLevel() >= 13) && (player.getLevel() <= 21))
				{
					npc.doCast(SkillData.getInstance().getSkill(4325, 1)); // Vampiric Rage
				}
				
				if ((player.getLevel() >= 14) && (player.getLevel() <= 20))
				{
					npc.doCast(SkillData.getInstance().getSkill(4326, 1)); // Regeneration
				}
				
				if ((player.getLevel() >= 15) && (player.getLevel() <= 19))
				{
					npc.doCast(SkillData.getInstance().getSkill(4327, 1)); // Haste
				}
			}
			
			if ((player.getLevel() >= 16) && (player.getLevel() <= 19))
			{
				player.doSimultaneousCast(SkillData.getInstance().getSkill(4338, 1)); // Life Cubic
			}
		}
		
		return true;
	}
	
	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
}
