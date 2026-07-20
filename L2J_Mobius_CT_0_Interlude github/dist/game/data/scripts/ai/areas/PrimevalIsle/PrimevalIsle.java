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
package ai.areas.PrimevalIsle;

import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.util.ArrayUtil;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * Primeval Isle AI.
 * @author St3eT
 */
public class PrimevalIsle extends Script
{
	// NPC
	private static final int EGG = 18344; // Ancient Egg
	private static final int ORNIT = 22202; // Ornithomimus 22742
	private static final int DEINO = 22205; // Deinonychus 22743
	private static final int[] SPRIGNANT =
	{
		18345, // Sprigant (Anesthesia)
		18346, // Sprigant (Deadly Poison)
	};
	private static final int[] MONSTERS =
	{
		22196, // Velociraptor
		22198, // Velociraptor
		22200, // Ornithomimus
		22202, // Ornithomimus
		22203, // Deinonychus
		22205, // Deinonychus
		22208, // Pachycephalosaurus
		22210, // Pachycephalosaurus
		22211, // Wild Strider
		22213, // Wild Strider
		22223, // Velociraptor
		22224, // Ornithomimus
		22225, // Deinonychus
		22226, // Pachycephalosaurus
		22227, // Wild Strider
		// 22742, // Ornithomimus
		// 22743, // Deinonychus
	};
	private static final int[] TREX =
	{
		22215, // Tyrannosaurus
		22216, // Tyrannosaurus
		22217, // Tyrannosaurus
	};
	private static final int[] VEGETABLE =
	{
		22200, // Ornithomimus
		22201, // Ornithomimus
		22202, // Ornithomimus
		22203, // Deinonychus
		22204, // Deinonychus
		22205, // Deinonychus
		22224, // Ornithomimus
		22225, // Deinonychus
	};
	
	// Skill
	private static final SkillHolder ANESTHESIA = new SkillHolder(5085, 1); // Anesthesia
	private static final SkillHolder DEADLY_POISON = new SkillHolder(5086, 1); // Deadly Poison
	private static final SkillHolder SELFBUFF1 = new SkillHolder(5087, 1); // Berserk
	private static final SkillHolder SELFBUFF2 = new SkillHolder(5087, 2); // Berserk
	private static final SkillHolder LONGRANGEDMAGIC1 = new SkillHolder(5120, 1); // Stun
	private static final SkillHolder PHYSICALSPECIAL1 = new SkillHolder(5083, 4); // Stun
	private static final SkillHolder PHYSICALSPECIAL2 = new SkillHolder(5081, 4); // Silence
	private static final SkillHolder PHYSICALSPECIAL3 = new SkillHolder(5082, 4); // NPC Spinning, Slashing Trick
	
	private PrimevalIsle()
	{
		addSpawnId(TREX);
		addSpawnId(SPRIGNANT);
		addSpawnId(MONSTERS);
		addAggroRangeEnterId(TREX);
		addSpellFinishedId(TREX);
		addAttackId(EGG);
		addAttackId(TREX);
		addAttackId(MONSTERS);
		addKillId(EGG, DEINO, ORNIT);
		addCreatureSeeId(TREX);
		addCreatureSeeId(MONSTERS);
	}
	
	@Override
	public void onSpellFinished(Npc npc, Player player, Skill skill)
	{
		if (npc.isInCombat())
		{
			final Attackable mob = npc.asAttackable();
			final Creature target = mob.getMostHated();
			if (((npc.getCurrentHp() / npc.getMaxHp()) * 100) < 60)
			{
				if (skill.getId() == SELFBUFF1.getSkillId())
				{
					npc.setScriptValue(3);
					if ((target != null))
					{
						npc.setTarget(target);
						mob.setRunning();
						mob.addDamageHate(target, 0, 555);
						mob.getAI().setIntention(Intention.ATTACK, target);
					}
				}
			}
			else if (((npc.getCurrentHp() / npc.getMaxHp()) * 100) < 30)
			{
				if (skill.getId() == SELFBUFF1.getSkillId())
				{
					npc.setScriptValue(1);
					if ((target != null))
					{
						npc.setTarget(target);
						mob.setRunning();
						mob.addDamageHate(target, 0, 555);
						mob.getAI().setIntention(Intention.ATTACK, target);
					}
				}
				else if (skill.getId() == SELFBUFF2.getSkillId())
				{
					npc.setScriptValue(5);
					if ((target != null))
					{
						npc.setTarget(target);
						mob.setRunning();
						mob.addDamageHate(target, 0, 555);
						mob.getAI().setIntention(Intention.ATTACK, target);
					}
				}
			}
		}
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "USE_SKILL":
			{
				if ((npc != null) && !npc.isDead())
				{
					npc.doCast((npc.getId() == SPRIGNANT[0] ? ANESTHESIA.getSkill() : DEADLY_POISON.getSkill()));
					startQuestTimer("USE_SKILL", 15000, npc, null);
				}
				break;
			}
			case "GHOST_DESPAWN":
			{
				if ((npc != null) && !npc.isDead())
				{
					if (!npc.isInCombat())
					{
						npc.deleteMe();
					}
					else
					{
						startQuestTimer("GHOST_DESPAWN", 1800000, npc, null);
					}
				}
				break;
			}
			case "TREX_ATTACK":
			{
				if ((npc != null) && (player != null))
				{
					npc.setScriptValue(0);
					if (player.isInsideRadius3D(npc, 800))
					{
						npc.setTarget(player);
						npc.doCast(LONGRANGEDMAGIC1.getSkill());
						addAttackDesire(npc, player);
					}
				}
				break;
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public void onCreatureSee(Npc npc, Creature creature)
	{
		if (ArrayUtil.contains(MONSTERS, npc.getId()))
		{
			if (creature.isPlayer())
			{
				final Attackable mob = npc.asAttackable();
				final int ag_type = npc.getTemplate().getParameters().getInt("ag_type", 0);
				final int probPhysicalSpecial1 = npc.getTemplate().getParameters().getInt("ProbPhysicalSpecial1", 0);
				final int probPhysicalSpecial2 = npc.getTemplate().getParameters().getInt("ProbPhysicalSpecial2", 0);
				final SkillHolder physicalSpecial1 = npc.getTemplate().getParameters().getObject("PhysicalSpecial1", SkillHolder.class);
				final SkillHolder physicalSpecial2 = npc.getTemplate().getParameters().getObject("PhysicalSpecial2", SkillHolder.class);
				
				if (((getRandom(100) < 30) && (npc.getId() == DEINO)) || ((npc.getId() == ORNIT) && npc.isScriptValue(0)))
				{
					mob.clearAggroList();
					npc.setScriptValue(1);
					npc.setRunning();
					
					final int distance = 3000;
					final int heading = LocationUtil.calculateHeadingFrom(creature, npc);
					final double angle = LocationUtil.convertHeadingToDegree(heading);
					final double radian = Math.toRadians(angle);
					final double sin = Math.sin(radian);
					final double cos = Math.cos(radian);
					final int newX = (int) (npc.getX() + (cos * distance));
					final int newY = (int) (npc.getY() + (sin * distance));
					final Location loc = GeoEngine.getInstance().getValidLocation(npc.getX(), npc.getY(), npc.getZ(), newX, newY, npc.getZ(), npc.getInstanceId());
					npc.getAI().setIntention(Intention.MOVE_TO, loc, 0);
				}
				else if (ag_type == 1)
				{
					if (getRandom(100) <= (probPhysicalSpecial1 * npc.getVariables().getInt("SKILL_MULTIPLER")))
					{
						if (!npc.isSkillDisabled(physicalSpecial1.getSkill()))
						{
							npc.setTarget(creature);
							npc.doCast(physicalSpecial1.getSkill());
						}
					}
					else if ((getRandom(100) <= (probPhysicalSpecial2 * npc.getVariables().getInt("SKILL_MULTIPLER"))) && !npc.isSkillDisabled(physicalSpecial2.getSkill()))
					{
						npc.setTarget(creature);
						npc.doCast(physicalSpecial2.getSkill());
					}
				}
			}
		}
		else if (ArrayUtil.contains(VEGETABLE, creature.getId()))
		{
			npc.setTarget(creature);
			npc.setRunning();
			npc.getAI().setIntention(Intention.ATTACK, creature);
		}
	}
	
	@Override
	public void onAggroRangeEnter(Npc npc, Player player, boolean isSummon)
	{
		if (npc.isScriptValue(0))
		{
			npc.setScriptValue(1);
			npc.broadcastSay(ChatType.NPC_GENERAL, "?");
			npc.asAttackable().clearAggroList();
			startQuestTimer("TREX_ATTACK", 6000, npc, player);
		}
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (npc.getId() == EGG)
		{
			if ((getRandom(100) <= 80) && npc.isScriptValue(0))
			{
				npc.setScriptValue(1);
				final Playable playable = isSummon ? attacker.getSummon() : attacker;
				World.getInstance().forEachVisibleObjectInRange(npc, Attackable.class, 500, monster ->
				{
					if ((getRandomBoolean()))
					{
						addAttackDesire(monster, playable);
					}
				});
			}
		}
		else if (ArrayUtil.contains(TREX, npc.getId()))
		{
			final Attackable mob = npc.asAttackable();
			final Creature target = mob.getMostHated();
			
			if (((npc.getCurrentHp() / npc.getMaxHp()) * 100) <= 30)
			{
				if (npc.isScriptValue(3))
				{
					if (!npc.isSkillDisabled(SELFBUFF1.getSkill()))
					{
						npc.doCast(SELFBUFF1.getSkill());
					}
				}
				else if (npc.isScriptValue(1) && !npc.isSkillDisabled(SELFBUFF2.getSkill()))
				{
					npc.doCast(SELFBUFF2.getSkill());
				}
			}
			else if ((((npc.getCurrentHp() / npc.getMaxHp()) * 100) <= 60) && (npc.isScriptValue(3)) && !npc.isSkillDisabled(SELFBUFF1.getSkill()))
			{
				npc.doCast(SELFBUFF1.getSkill());
			}
			
			if (LocationUtil.calculateDistance(npc, attacker, true, false) > 100)
			{
				if (!npc.isSkillDisabled(LONGRANGEDMAGIC1.getSkill()) && (getRandom(100) <= (10 * npc.getScriptValue())))
				{
					npc.setTarget(attacker);
					npc.doCast(LONGRANGEDMAGIC1.getSkill());
				}
			}
			else
			{
				if (!npc.isSkillDisabled(LONGRANGEDMAGIC1.getSkill()) && (getRandom(100) <= (10 * npc.getScriptValue())))
				{
					npc.setTarget(target);
					npc.doCast(LONGRANGEDMAGIC1.getSkill());
				}
				
				if (!npc.isSkillDisabled(PHYSICALSPECIAL1.getSkill()) && (getRandom(100) <= (5 * npc.getScriptValue())))
				{
					npc.setTarget(target);
					npc.doCast(PHYSICALSPECIAL1.getSkill());
				}
				
				if (!npc.isSkillDisabled(PHYSICALSPECIAL2.getSkill()) && (getRandom(100) <= (3 * npc.getScriptValue())))
				{
					npc.setTarget(target);
					npc.doCast(PHYSICALSPECIAL2.getSkill());
				}
				
				if (!npc.isSkillDisabled(PHYSICALSPECIAL3.getSkill()) && (getRandom(100) <= (5 * npc.getScriptValue())))
				{
					npc.setTarget(target);
					npc.doCast(PHYSICALSPECIAL3.getSkill());
				}
			}
		}
		else
		{
			Creature target = null;
			final int probPhysicalSpecial1 = npc.getTemplate().getParameters().getInt("ProbPhysicalSpecial1", 0);
			final int probPhysicalSpecial2 = npc.getTemplate().getParameters().getInt("ProbPhysicalSpecial2", 0);
			final SkillHolder selfRangeBuff1 = npc.getTemplate().getParameters().getObject("SelfRangeBuff1", SkillHolder.class);
			final SkillHolder physicalSpecial1 = npc.getTemplate().getParameters().getObject("PhysicalSpecial1", SkillHolder.class);
			final SkillHolder physicalSpecial2 = npc.getTemplate().getParameters().getObject("PhysicalSpecial2", SkillHolder.class);
			
			if (((npc.getCurrentHp() / npc.getMaxHp()) * 100) <= 50)
			{
				npc.getVariables().set("SKILL_MULTIPLER", 2);
			}
			else
			{
				npc.getVariables().set("SKILL_MULTIPLER", 1);
			}
			
			if ((((npc.getCurrentHp() / npc.getMaxHp()) * 100) <= 30) && (npc.getVariables().getInt("SELFBUFF_USED") == 0))
			{
				final Attackable mob = npc.asAttackable();
				target = mob.getMostHated();
				mob.clearAggroList();
				if (!npc.isSkillDisabled(selfRangeBuff1.getSkill()))
				{
					npc.getVariables().set("SELFBUFF_USED", 1);
					npc.doCast(selfRangeBuff1.getSkill());
					npc.setRunning();
					npc.getAI().setIntention(Intention.ATTACK, target);
				}
			}
			
			if (target != null)
			{
				if ((getRandom(100) <= (probPhysicalSpecial1 * npc.getVariables().getInt("SKILL_MULTIPLER"))) && !npc.isSkillDisabled(physicalSpecial1.getSkill()))
				{
					npc.setTarget(target);
					npc.doCast(physicalSpecial1.getSkill());
				}
				
				if ((getRandom(100) <= (probPhysicalSpecial2 * npc.getVariables().getInt("SKILL_MULTIPLER"))) && !npc.isSkillDisabled(physicalSpecial2.getSkill()))
				{
					npc.setTarget(target);
					npc.doCast(physicalSpecial2.getSkill());
				}
			}
		}
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		if (ArrayUtil.contains(SPRIGNANT, npc.getId()))
		{
			cancelQuestTimer("USE_SKILL", npc, null);
			startQuestTimer("USE_SKILL", 15000, npc, null);
		}
		else if (ArrayUtil.contains(TREX, npc.getId()))
		{
			final int collectGhost = npc.getTemplate().getParameters().getInt("CollectGhost", 0);
			final int collectDespawn = npc.getTemplate().getParameters().getInt("CollectGhostDespawnTime", 30);
			
			if (collectGhost == 1)
			{
				cancelQuestTimer("GHOST_DESPAWN", npc, null);
				startQuestTimer("GHOST_DESPAWN", collectDespawn * 60000, npc, null);
			}
		}
		else
		{
			npc.getVariables().set("SELFBUFF_USED", 0);
			npc.getVariables().set("SKILL_MULTIPLER", 1);
		}
	}
	
	public static void main(String[] args)
	{
		new PrimevalIsle();
	}
}
