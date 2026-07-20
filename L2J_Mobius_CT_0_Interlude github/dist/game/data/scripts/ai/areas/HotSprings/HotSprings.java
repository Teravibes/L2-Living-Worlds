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
package ai.areas.HotSprings;

import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.BuffInfo;
import org.l2jmobius.gameserver.model.skill.Skill;

/**
 * Hot Springs AI.
 * @author Mobius
 */
public class HotSprings extends Script
{
	// NPCs
	private static final int BANDERSNATCHLING = 21314;
	private static final int FLAVA = 21316;
	private static final int ATROXSPAWN = 21317;
	private static final int NEPENTHES = 21319;
	private static final int ATROX = 21321;
	private static final int BANDERSNATCH = 21322;
	
	// Skills
	private static final int RHEUMATISM = 4551;
	private static final int CHOLERA = 4552;
	private static final int FLU = 4553;
	private static final int MALARIA = 4554;
	
	// Misc
	private static final int DISEASE_CHANCE = 10;
	
	private HotSprings()
	{
		addAttackId(BANDERSNATCHLING, FLAVA, ATROXSPAWN, NEPENTHES, ATROX, BANDERSNATCH);
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (getRandom(100) < DISEASE_CHANCE)
		{
			tryToInfect(npc, attacker, MALARIA);
		}
		
		if (getRandom(100) < DISEASE_CHANCE)
		{
			switch (npc.getId())
			{
				case BANDERSNATCHLING:
				case ATROX:
				{
					tryToInfect(npc, attacker, RHEUMATISM);
					break;
				}
				case FLAVA:
				case NEPENTHES:
				{
					tryToInfect(npc, attacker, CHOLERA);
					break;
				}
				case ATROXSPAWN:
				case BANDERSNATCH:
				{
					tryToInfect(npc, attacker, FLU);
					break;
				}
			}
		}
	}
	
	private void tryToInfect(Npc npc, Creature creature, int diseaseId)
	{
		final BuffInfo info = creature.getEffectList().getBuffInfoBySkillId(diseaseId);
		final int skillLevel = (info == null) ? 1 : (info.getSkill().getLevel() < 10) ? info.getSkill().getLevel() + 1 : 10;
		final Skill skill = SkillData.getInstance().getSkill(diseaseId, skillLevel);
		if ((skill != null) && !npc.isCastingNow() && npc.checkDoCastConditions(skill))
		{
			npc.setTarget(creature);
			npc.doCast(skill);
		}
	}
	
	public static void main(String[] args)
	{
		new HotSprings();
	}
}
