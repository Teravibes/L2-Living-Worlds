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
package events.HeavyMedal;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.LongTimeEvent;
import org.l2jmobius.gameserver.model.script.QuestSound;

/**
 * Heavy Medals event AI.
 * @author Gnacik
 */
public class HeavyMedal extends LongTimeEvent
{
	private static final int CAT_ROY = 31228;
	private static final int CAT_WINNIE = 31229;
	private static final int GLITTERING_MEDAL = 6393;
	
	private static final int WIN_CHANCE = 50;
	
	private static final int[] MEDALS =
	{
		5,
		10,
		20,
		40
	};
	private static final int[] BADGES =
	{
		6399,
		6400,
		6401,
		6402
	};
	
	private HeavyMedal()
	{
		addStartNpc(CAT_ROY);
		addStartNpc(CAT_WINNIE);
		addTalkId(CAT_ROY);
		addTalkId(CAT_WINNIE);
		addFirstTalkId(CAT_ROY);
		addFirstTalkId(CAT_WINNIE);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		int level = checkLevel(player);
		if (event.equalsIgnoreCase("game"))
		{
			htmltext = getQuestItemsCount(player, GLITTERING_MEDAL) < MEDALS[level] ? "31229-no.htm" : "31229-game.htm";
		}
		else if (event.equalsIgnoreCase("heads") || event.equalsIgnoreCase("tails"))
		{
			if (getQuestItemsCount(player, GLITTERING_MEDAL) < MEDALS[level])
			{
				htmltext = "31229-" + event.toLowerCase() + "-10.htm";
			}
			else
			{
				takeItems(player, GLITTERING_MEDAL, MEDALS[level]);
				if (getRandom(100) > WIN_CHANCE)
				{
					level = 0;
				}
				else
				{
					if (level > 0)
					{
						takeItems(player, BADGES[level - 1], -1);
					}
					
					giveItems(player, BADGES[level], 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					level++;
				}
				
				htmltext = "31229-" + event.toLowerCase() + "-" + level + ".htm";
			}
		}
		else if (event.equalsIgnoreCase("talk"))
		{
			htmltext = npc.getId() + "-lvl-" + level + ".htm";
		}
		
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (player.getQuestState(getName()) == null)
		{
			newQuestState(player);
		}
		
		return npc.getId() + ".htm";
	}
	
	public int checkLevel(Player player)
	{
		int level = 0;
		if (hasQuestItems(player, 6402))
		{
			level = 4;
		}
		else if (hasQuestItems(player, 6401))
		{
			level = 3;
		}
		else if (hasQuestItems(player, 6400))
		{
			level = 2;
		}
		else if (hasQuestItems(player, 6399))
		{
			level = 1;
		}
		
		return level;
	}
	
	public static void main(String[] args)
	{
		new HeavyMedal();
	}
}
