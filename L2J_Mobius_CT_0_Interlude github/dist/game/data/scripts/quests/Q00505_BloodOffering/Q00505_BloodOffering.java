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
package quests.Q00505_BloodOffering;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.model.sevensigns.SevenSigns;
import org.l2jmobius.gameserver.util.ArrayUtil;

import quests.Q00635_IntoTheDimensionalRift.Q00635_IntoTheDimensionalRift;

/**
 * @author Mobius
 * @note Based on python script
 */
public class Q00505_BloodOffering extends Quest
{
	// NPCs
	// @formatter:off
	private static final int[] TOWN_DAWN = {31078, 31079, 31080, 31081, 31083, 31084, 31082, 31692, 31694, 31997, 31168};
	private static final int[] TOWN_DUSK = {31085, 31086, 31087, 31088, 31090, 31091, 31089, 31693, 31695, 31998, 31169};
	private static final int[] DIM_GK = {31494, 31495, 31496, 31497, 31498, 31499, 31500, 31501, 31502, 31503, 31504, 31505, 31506, 31507};
	private static final int[] GK_ZIGGURAT = {31095, 31096, 31097, 31098, 31099, 31100, 31101, 31102, 31103, 31104, 31105, 31106, 31107, 31108, 31109, 31110, 31114, 31115, 31116, 31117, 31118, 31119, 31120, 31121, 31122, 31123, 31124, 31125};
	private static final int[] FESTIVALGUIDE = {31127, 31128, 31129, 31130, 31131, 31137, 31138, 31139, 31140, 31141};
	private static final int[] FESTIVALWITCH = {31132, 31133, 31134, 31135, 31136, 31142, 31143, 31144, 31145, 31146};
	private static final int[] RIFTPOST = {31488, 31489, 31490, 31491, 31492, 31493};
	// @formatter:on
	
	public Q00505_BloodOffering()
	{
		super(505, "Blood Offering");
		addStartNpc(TOWN_DAWN);
		addTalkId(TOWN_DAWN);
		addStartNpc(TOWN_DUSK);
		addTalkId(TOWN_DUSK);
		addStartNpc(DIM_GK);
		addTalkId(DIM_GK);
		addStartNpc(GK_ZIGGURAT);
		addTalkId(GK_ZIGGURAT);
		addStartNpc(FESTIVALGUIDE);
		addTalkId(FESTIVALGUIDE);
		addStartNpc(FESTIVALWITCH);
		addTalkId(FESTIVALWITCH);
		addStartNpc(RIFTPOST);
		addTalkId(RIFTPOST);
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final String htmltext = getNoQuestMsg(player);
		final QuestState qs = getQuestState(player, true);
		if (qs == null)
		{
			return htmltext;
		}
		
		final QuestState qs2 = player.getQuestState(Q00635_IntoTheDimensionalRift.class.getSimpleName());
		final int npcId = npc.getId();
		if (ArrayUtil.contains(FESTIVALGUIDE, npcId))
		{
			player.teleToLocation(-114796, -179334, -6752);
			if (qs2 != null)
			{
				qs2.setState(State.STARTED);
				qs2.setCond(1);
			}
			
			qs.exitQuest(true, true);
			return "guide.htm";
		}
		else if (npcId == 31132)
		{
			player.teleToLocation(-80204, 87056, -5154);
			return "witch.htm";
		}
		else if (npcId == 31133)
		{
			player.teleToLocation(-77198, 87678, -5182);
			return "witch.htm";
		}
		else if (npcId == 31134)
		{
			player.teleToLocation(-76183, 87135, -5179);
			return "witch.htm";
		}
		else if (npcId == 31135)
		{
			player.teleToLocation(-76945, 86602, 5153);
			return "witch.htm";
		}
		else if (npcId == 31136)
		{
			player.teleToLocation(-79970, 85997, -5154);
			return "witch.htm";
		}
		else if (npcId == 31142)
		{
			player.teleToLocation(-79182, 111893, -4898);
			return "witch.htm";
		}
		else if (npcId == 31143)
		{
			player.teleToLocation(-76176, 112505, -4899);
			return "witch.htm";
		}
		else if (npcId == 31144)
		{
			player.teleToLocation(-75198, 111969, -4898);
			return "witch.htm";
		}
		else if (npcId == 31145)
		{
			player.teleToLocation(-75920, 111435, -4900);
			return "witch.htm";
		}
		else if (npcId == 31146)
		{
			player.teleToLocation(-78928, 110825, -4926);
			return "witch.htm";
		}
		else if (ArrayUtil.contains(RIFTPOST, npcId))
		{
			if (!SevenSigns.getInstance().isSealValidationPeriod())
			{
				final int cabal = SevenSigns.getInstance().getPlayerCabal(player.getObjectId());
				if (cabal == SevenSigns.CABAL_DAWN)
				{
					qs.setState(State.STARTED);
					qs.setCond(1);
					qs.getPlayer().teleToLocation(-80157, 111344, -4901);
					if (qs2 != null)
					{
						qs2.unset("cond");
					}
					
					return "riftpost-1.htm";
				}
				else if (cabal == SevenSigns.CABAL_DUSK)
				{
					qs.setState(State.STARTED);
					qs.setCond(1);
					if (qs2 != null)
					{
						qs2.unset("cond");
					}
					
					qs.getPlayer().teleToLocation(-81261, 86531, -5157);
					return "riftpost-1.htm";
				}
				
				return "riftpost-2.htm";
			}
			
			return "riftpost-2.htm";
		}
		
		return htmltext;
	}
}
