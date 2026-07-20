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
package ai.areas.MonasteryOfSilence;

import java.util.LinkedList;
import java.util.List;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.Pet;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;
import org.l2jmobius.gameserver.util.ArrayUtil;
import org.l2jmobius.gameserver.util.LocationUtil;

public class MonasteryOfSilence extends Script
{
	static final int[] mobs1 =
	{
		22124,
		22125,
		22126,
		22127,
		22129
	};
	static final int[] mobs2 =
	{
		22134,
		22135
	};
	static final String[] text =
	{
		"You cannot carry a weapon without authorization!",
		"name, why would you choose the path of darkness?!",
		"name! How dare you defy the will of Einhasad!"
	};
	
	private MonasteryOfSilence()
	{
		addNpcHateId(mobs1);
		addNpcHateId(mobs2);
		addAggroRangeEnterId(mobs1);
		addAggroRangeEnterId(mobs2);
		addSkillSeeId(mobs1);
		addSkillSeeId(mobs2);
		addSpellFinishedId(mobs1);
		addSpellFinishedId(mobs2);
		addSpawnId(mobs1);
		addSpawnId(mobs2);
	}
	
	@Override
	public boolean onNpcHate(Attackable mob, Player player, boolean isSummon)
	{
		return player.getActiveWeaponInstance() != null;
	}
	
	@Override
	public void onAggroRangeEnter(Npc npc, Player player, boolean isSummon)
	{
		if (ArrayUtil.contains(mobs1, npc.getId()) && !npc.isInCombat() && (npc.getTarget() == null))
		{
			if (player.getActiveWeaponInstance() != null)
			{
				npc.setTarget(player);
				npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.GENERAL, npc.getId(), text[0]));
				switch (npc.getId())
				{
					case 22124:
					case 22126:
					{
						final Skill skill = SkillData.getInstance().getSkill(4589, 8);
						npc.doCast(skill);
						break;
					}
					default:
					{
						npc.setRunning();
						npc.asAttackable().addDamageHate(player, 0, 999);
						npc.getAI().setIntention(Intention.ATTACK, player);
						break;
					}
				}
			}
			// else if (npc.asAttackable().getMostHated() == null)
			// {
			// return;
			// }
		}
	}
	
	@Override
	public void onSkillSee(Npc npc, Player caster, Skill skill, List<WorldObject> targets, boolean isSummon)
	{
		if (ArrayUtil.contains(mobs2, npc.getId()))
		{
			if (skill.hasEffectType(EffectType.AGGRESSION) && !targets.isEmpty())
			{
				for (WorldObject obj : targets)
				{
					if (obj.equals(npc))
					{
						npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.GENERAL, npc.getId(), text[Rnd.get(2) + 1].replace("name", caster.getName())));
						npc.asAttackable().addDamageHate(caster, 0, 999);
						npc.getAI().setIntention(Intention.ATTACK, caster);
						break;
					}
				}
			}
		}
	}
	
	@Override
	public void onSpellFinished(Npc npc, Player player, Skill skill)
	{
		if (ArrayUtil.contains(mobs1, npc.getId()) && (skill.getId() == 4589))
		{
			npc.setRunning();
			npc.asAttackable().addDamageHate(player, 0, 999);
			npc.getAI().setIntention(Intention.ATTACK, player);
		}
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		if (ArrayUtil.contains(mobs1, npc.getId()))
		{
			final List<Playable> result = new LinkedList<>();
			for (WorldObject obj : World.getInstance().getVisibleObjects(npc, WorldObject.class))
			{
				if ((obj instanceof Player) || (obj instanceof Pet))
				{
					if (LocationUtil.checkIfInRange(npc.getAggroRange(), npc, obj, true) && !obj.asCreature().isDead())
					{
						result.add(obj.asPlayable());
					}
				}
			}
			
			if (!result.isEmpty())
			{
				for (Playable obj : result)
				{
					final Playable target = obj instanceof Player ? obj : obj.asSummon().getOwner();
					if ((target.getActiveWeaponInstance() != null) && !npc.isInCombat() && (npc.getTarget() == null))
					{
						npc.setTarget(target);
						npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.GENERAL, npc.getId(), text[0]));
						switch (npc.getId())
						{
							case 22124:
							case 22126:
							case 22127:
							{
								final Skill skill = SkillData.getInstance().getSkill(4589, 8);
								npc.doCast(skill);
								break;
							}
							default:
							{
								npc.setRunning();
								npc.asAttackable().addDamageHate(target, 0, 999);
								npc.getAI().setIntention(Intention.ATTACK, target);
								break;
							}
						}
					}
				}
			}
		}
	}
	
	public static void main(String[] args)
	{
		new MonasteryOfSilence();
	}
}
