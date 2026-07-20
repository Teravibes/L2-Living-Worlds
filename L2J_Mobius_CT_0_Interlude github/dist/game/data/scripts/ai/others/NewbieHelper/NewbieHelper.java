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
package ai.others.NewbieHelper;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.script.State;

import quests.Q00255_Tutorial.Q00255_Tutorial;

/**
 * @author Mobius
 */
public class NewbieHelper extends Script
{
	private static final int SOULSHOT_NOVICE = 5789;
	private static final int SPIRITSHOT_NOVICE = 5790;
	private static final int TOKEN = 8542;
	private static final int SCROLL = 8594;
	private static final int SCROLL_REWARD_CHANCE = 100; // 0 to disable.
	
	public NewbieHelper()
	{
		addStartNpc(30598, 30599, 30600, 30601, 30602);
		addTalkId(30598, 30599, 30600, 30601, 30602);
		addFirstTalkId(30598, 30599, 30600, 30601, 30602);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (!PlayerConfig.DISABLE_TUTORIAL)
		{
			final QuestState qs1 = getQuestState(player, true);
			if (!qs1.isCompleted() && (player.getLevel() < 18))
			{
				final QuestState qs2 = player.getQuestState(Q00255_Tutorial.class.getSimpleName());
				if (((qs2 != null) && (qs2.getInt("Ex") == 4)))
				{
					final boolean isMage = player.isMageClass();
					final boolean isOrcMage = player.getPlayerClass().getId() == 49;
					qs1.playTutorialVoice(isMage && !isOrcMage ? "tutorial_voice_027" : "tutorial_voice_026");
					giveItems(player, isMage && !isOrcMage ? SPIRITSHOT_NOVICE : SOULSHOT_NOVICE, isMage && !isOrcMage ? 100 : 200);
					giveItems(player, TOKEN, 12);
					if (getRandom(100) < SCROLL_REWARD_CHANCE) // Old C6 had this at 50%.
					{
						giveItems(player, SCROLL, 2);
					}
					
					qs1.setState(State.COMPLETED);
				}
			}
		}
		
		npc.showChatWindow(player);
		return null;
	}
	
	public static void main(String[] args)
	{
		new NewbieHelper();
	}
}
