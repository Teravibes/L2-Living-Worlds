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
package quests.Q00247_PossessorOfAPreciousSoul4;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;

public class Q00247_PossessorOfAPreciousSoul4 extends Quest
{
	// NPCs
	private static final int CARADINE = 31740;
	private static final int LADY_OF_THE_LAKE = 31745;
	
	// Items
	private static final int CARADINE_LETTER = 7679;
	private static final int NOBLESS_TIARA = 7694;
	
	public Q00247_PossessorOfAPreciousSoul4()
	{
		super(247, "Possessor of a Precious Soul - 4");
		addStartNpc(CARADINE);
		addTalkId(CARADINE, LADY_OF_THE_LAKE);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final QuestState st = getQuestState(player, false);
		
		if (st == null)
		{
			return htmltext;
		}
		
		switch (event)
		{
			case "31740-03.htm":
			{
				st.startQuest();
				takeItems(player, CARADINE_LETTER, 1);
				break;
			}
			case "TELEPORT":
			{
				st.setCond(2);
				player.teleToLocation(143209, 43968, -3038);
				break;
			}
			case "31745-05.htm":
			{
				player.setNoble(true);
				giveItems(player, NOBLESS_TIARA, 1);
				addExpAndSp(player, 93836, 0);
				player.broadcastPacket(new SocialAction(player.getObjectId(), 3));
				st.exitQuest(false, true);
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = getQuestState(player, true);
		
		switch (st.getState())
		{
			case State.CREATED:
			{
				if (hasQuestItems(player, CARADINE_LETTER))
				{
					htmltext = (!player.isSubClassActive() || (player.getLevel() < 75)) ? "31740-02.htm" : "31740-01.htm";
				}
				break;
			}
			case State.STARTED:
			{
				if (!player.isSubClassActive())
				{
					break;
				}
				
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case CARADINE:
					{
						if (cond == 1)
						{
							htmltext = "31740-04.htm";
						}
						else if (cond == 2)
						{
							htmltext = "31740-06.htm";
						}
						break;
					}
					case LADY_OF_THE_LAKE:
					{
						if (cond == 2)
						{
							htmltext = (player.getLevel() < 75) ? "31745-06.htm" : "31745-01.htm";
						}
						break;
					}
				}
				break;
			}
			case State.COMPLETED:
			{
				htmltext = getAlreadyCompletedMsg(player);
				break;
			}
		}
		
		return htmltext;
	}
}
