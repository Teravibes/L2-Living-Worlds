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
package ai.others.Asamah;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.Script;

import quests.Q00111_ElrokianHuntersProof.Q00111_ElrokianHuntersProof;

/**
 * Asamah AI.
 * @author Adry_85
 * @since 2.6.0.0
 */
public class Asamah extends Script
{
	// NPC
	private static final int ASAMAH = 32115;
	
	public Asamah()
	{
		addFirstTalkId(ASAMAH);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		if (event.equals("32115-03.htm") || event.equals("32115-04.htm"))
		{
			htmltext = event;
		}
		
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final QuestState qs = player.getQuestState(Q00111_ElrokianHuntersProof.class.getSimpleName());
		return ((qs != null) && (qs.isCompleted())) ? "32115-01.htm" : "32115-02.htm";
	}
	
	public static void main(String[] args)
	{
		new Asamah();
	}
}
