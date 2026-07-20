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
package custom.DelevelManager;

import org.l2jmobius.gameserver.config.custom.DelevelManagerConfig;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;

/**
 * @author Mobius
 */
public class DelevelManager extends Script
{
	private DelevelManager()
	{
		addStartNpc(DelevelManagerConfig.DELEVEL_MANAGER_NPCID);
		addTalkId(DelevelManagerConfig.DELEVEL_MANAGER_NPCID);
		addFirstTalkId(DelevelManagerConfig.DELEVEL_MANAGER_NPCID);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (!DelevelManagerConfig.DELEVEL_MANAGER_ENABLED)
		{
			return null;
		}
		
		switch (event)
		{
			case "delevel":
			{
				if (player.getLevel() <= DelevelManagerConfig.DELEVEL_MANAGER_MINIMUM_DELEVEL)
				{
					return "1002000-2.htm";
				}
				
				if (getQuestItemsCount(player, DelevelManagerConfig.DELEVEL_MANAGER_ITEMID) >= DelevelManagerConfig.DELEVEL_MANAGER_ITEMCOUNT)
				{
					takeItems(player, DelevelManagerConfig.DELEVEL_MANAGER_ITEMID, DelevelManagerConfig.DELEVEL_MANAGER_ITEMCOUNT);
					player.getStat().removeExpAndSp((player.getExp() - ExperienceData.getInstance().getExpForLevel(player.getLevel() - 1)), 0);
					player.broadcastUserInfo();
					return "1002000.htm";
				}
				
				return "1002000-1.htm";
			}
		}
		
		return null;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return "1002000.htm";
	}
	
	public static void main(String[] args)
	{
		new DelevelManager();
	}
}
