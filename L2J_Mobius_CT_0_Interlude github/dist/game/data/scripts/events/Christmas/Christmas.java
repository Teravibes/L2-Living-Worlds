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
package events.Christmas;

import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.LongTimeEvent;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;

/**
 * @author Mobius
 */
public class Christmas extends LongTimeEvent
{
	// NPC
	private static final int SANTA = 31863;
	
	// Skills
	private static final SkillHolder[] FIGHTER_BUFFS =
	{
		new SkillHolder(4322, 1), // Wind Walk
		new SkillHolder(4323, 1), // Shield
		new SkillHolder(4396, 2), // Magic Barrier
		new SkillHolder(4324, 1), // Bless the Body
		new SkillHolder(4325, 1), // Vampiric Rage
		new SkillHolder(4326, 1), // Regeneration
		new SkillHolder(4327, 1), // Haste
	};
	private static final SkillHolder[] MAGE_BUFFS =
	{
		new SkillHolder(4322, 1), // Wind Walk
		new SkillHolder(4323, 1), // Shield
		new SkillHolder(4396, 2), // Magic Barrier
		new SkillHolder(4328, 1), // Bless the Soul
		new SkillHolder(4329, 1), // Acumen
		new SkillHolder(4330, 1), // Concentration
		new SkillHolder(4331, 1), // Empower
	};
	
	// Misc
	private static final boolean ALLOW_SANTA_BUFFS = true;
	private static final int SANTA_BUFF_RANGE = 500;
	
	private Christmas()
	{
		addStartNpc(SANTA);
		addFirstTalkId(SANTA);
		addTalkId(SANTA);
		addSpawnId(SANTA);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if ("SantaBlessings".equals(event))
		{
			if ((npc != null) && npc.isSpawned() && (npc.getId() == SANTA))
			{
				for (Player nearby : World.getInstance().getVisibleObjectsInRange(npc, Player.class, SANTA_BUFF_RANGE))
				{
					boolean buffed = false;
					npc.setTarget(nearby);
					
					if (nearby.isMageClass())
					{
						for (SkillHolder holder : MAGE_BUFFS)
						{
							if (!nearby.isAffectedBySkill(holder.getSkillId()))
							{
								npc.doCast(holder.getSkill());
								buffed = true;
							}
						}
					}
					else
					{
						for (SkillHolder holder : FIGHTER_BUFFS)
						{
							if (!nearby.isAffectedBySkill(holder.getSkillId()))
							{
								npc.doCast(holder.getSkill());
								buffed = true;
							}
						}
					}
					
					if (buffed)
					{
						nearby.sendMessage("Santa Trainee has gifted you buffs!");
					}
				}
				
				startQuestTimer("SantaBlessings", 15000, npc, null);
			}
		}
		
		return null;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return "31863.htm";
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		if (ALLOW_SANTA_BUFFS)
		{
			startQuestTimer("SantaBlessings", 15000, npc, null);
		}
	}
	
	public static void main(String[] args)
	{
		new Christmas();
	}
}
