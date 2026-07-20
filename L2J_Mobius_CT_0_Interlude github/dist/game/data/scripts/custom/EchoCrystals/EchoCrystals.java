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
package custom.EchoCrystals;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.Script;

/**
 * Echo Crystals AI.
 * @author Plim
 */
public class EchoCrystals extends Script
{
	private static final int[] NPCs =
	{
		31042,
		31043
	};
	
	private static final int ADENA = 57;
	private static final int COST = 200;
	
	private static final Map<Integer, ScoreData> SCORES = new HashMap<>();
	
	private class ScoreData
	{
		private final int crystalId;
		private final String okMsg;
		private final String noAdenaMsg;
		private final String noScoreMsg;
		
		public ScoreData(int crystalId, String okMsg, String noAdenaMsg, String noScoreMsg)
		{
			super();
			this.crystalId = crystalId;
			this.okMsg = okMsg;
			this.noAdenaMsg = noAdenaMsg;
			this.noScoreMsg = noScoreMsg;
		}
		
		public int getCrystalId()
		{
			return crystalId;
		}
		
		public String getOkMsg()
		{
			return okMsg;
		}
		
		public String getNoAdenaMsg()
		{
			return noAdenaMsg;
		}
		
		public String getNoScoreMsg()
		{
			return noScoreMsg;
		}
	}
	
	private EchoCrystals()
	{
		// Initialize Map
		SCORES.put(4410, new ScoreData(4411, "01", "02", "03"));
		SCORES.put(4409, new ScoreData(4412, "04", "05", "06"));
		SCORES.put(4408, new ScoreData(4413, "07", "08", "09"));
		SCORES.put(4420, new ScoreData(4414, "10", "11", "12"));
		SCORES.put(4421, new ScoreData(4415, "13", "14", "15"));
		SCORES.put(4419, new ScoreData(4417, "16", "05", "06"));
		SCORES.put(4418, new ScoreData(4416, "17", "05", "06"));
		
		for (int npc : NPCs)
		{
			addStartNpc(npc);
			addTalkId(npc);
		}
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = "";
		final QuestState qs = player.getQuestState(EchoCrystals.class.getSimpleName());
		if ((qs != null) && StringUtil.isNumeric(event))
		{
			final int score = Integer.parseInt(event);
			if (SCORES.containsKey(score))
			{
				final int crystal = SCORES.get(score).getCrystalId();
				final String ok = SCORES.get(score).getOkMsg();
				final String noadena = SCORES.get(score).getNoAdenaMsg();
				final String noscore = SCORES.get(score).getNoScoreMsg();
				if (!hasQuestItems(player, score))
				{
					htmltext = npc.getId() + "-" + noscore + ".htm";
				}
				else if (getQuestItemsCount(player, ADENA) < COST)
				{
					htmltext = npc.getId() + "-" + noadena + ".htm";
				}
				else
				{
					takeItems(player, ADENA, COST);
					giveItems(player, crystal, 1);
					htmltext = npc.getId() + "-" + ok + ".htm";
				}
			}
		}
		
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		return "1.htm";
	}
	
	public static void main(String[] args)
	{
		new EchoCrystals();
	}
}
