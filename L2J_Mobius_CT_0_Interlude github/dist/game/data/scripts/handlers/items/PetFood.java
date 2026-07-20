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
package handlers.items;

import java.util.Set;

import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.data.xml.PetDataTable;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.Pet;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * @author Kerberos, Zoey76
 */
public class PetFood implements IItemHandler
{
	@Override
	public boolean onItemUse(Playable playable, Item item, boolean forceUse)
	{
		if (playable.isPet() && !playable.asPet().canEatFoodId(item.getId()))
		{
			playable.sendPacket(SystemMessageId.THIS_PET_CANNOT_USE_THIS_ITEM);
			return false;
		}
		
		final SkillHolder[] skills = item.getTemplate().getSkills();
		if (skills != null)
		{
			for (SkillHolder sk : skills)
			{
				useFood(playable, sk.getSkillId(), sk.getSkillLevel(), item);
			}
		}
		
		return true;
	}
	
	private boolean useFood(Playable activeChar, int skillId, int skillLevel, Item item)
	{
		final Skill skill = SkillData.getInstance().getSkill(skillId, skillLevel);
		if (skill != null)
		{
			if (activeChar.isPet())
			{
				final Pet pet = activeChar.asPet();
				if (pet.destroyItem(ItemProcessType.NONE, item.getObjectId(), 1, null, false))
				{
					pet.broadcastSkillPacket(new MagicSkillUse(pet, pet, skillId, skillLevel, 0, 0), pet);
					pet.setCurrentFed(pet.getCurrentFed() + (skill.getFeed() * RatesConfig.PET_FOOD_RATE));
					pet.broadcastStatusUpdate();
					if (pet.isHungry())
					{
						pet.sendPacket(SystemMessageId.YOUR_PET_ATE_A_LITTLE_BUT_IS_STILL_HUNGRY);
					}
					
					return true;
				}
			}
			else if (activeChar.isPlayer())
			{
				final Player player = activeChar.asPlayer();
				if (player.isMounted())
				{
					final Set<Integer> foodIds = PetDataTable.getInstance().getPetData(player.getMountNpcId()).getFood();
					if (foodIds.contains(item.getId()) && player.destroyItem(ItemProcessType.NONE, item.getObjectId(), 1, null, false))
					{
						player.broadcastSkillPacket(new MagicSkillUse(player, player, skillId, skillLevel, 0, 0), player);
						player.setCurrentFeed(player.getCurrentFeed() + skill.getFeed());
						return true;
					}
				}
				
				final SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS);
				sm.addItemName(item);
				player.sendPacket(sm);
			}
		}
		
		return false;
	}
}
