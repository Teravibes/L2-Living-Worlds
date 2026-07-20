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
package ai.others.Servitors;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.annotations.Id;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.annotations.RegisterType;
import org.l2jmobius.gameserver.model.events.holders.actor.creature.OnCreatureAttacked;
import org.l2jmobius.gameserver.model.events.holders.actor.creature.OnCreatureDeath;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;

/**
 * Sin Eater AI.
 * @author St3eT
 */
public class SinEater extends Script
{
	// NPCs
	private static final int SIN_EATER = 12564;
	
	private SinEater()
	{
		addSummonSpawnId(SIN_EATER);
		addSummonTalkId(SIN_EATER);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("TALK") && (player != null) && (player.getSummon() != null))
		{
			if (getRandom(100) < 30)
			{
				final int random = getRandom(100);
				final Summon summon = player.getSummon();
				if (random < 20)
				{
					broadcastSummonSay(summon, "Yawwwwn! It's so boring here. We should go and find some action!");
				}
				else if (random < 40)
				{
					broadcastSummonSay(summon, "Hey, if you continue to waste time you will never finish your penance!");
				}
				else if (random < 60)
				{
					broadcastSummonSay(summon, "I know you don't like me. The feeling is mutual!");
				}
				else if (random < 80)
				{
					broadcastSummonSay(summon, "I need a drink.");
				}
				else
				{
					broadcastSummonSay(summon, "Oh, this is dragging on too long... At this rate I won't make it home before the seven seals are broken.");
				}
			}
			
			startQuestTimer("TALK", 60000, null, player);
		}
		
		return super.onEvent(event, npc, player);
	}
	
	@RegisterEvent(EventType.ON_CREATURE_DEATH)
	@RegisterType(ListenerRegisterType.NPC)
	@Id(SIN_EATER)
	public void onCreatureKill(OnCreatureDeath event)
	{
		final int random = getRandom(100);
		final Summon summon = event.getTarget().asSummon();
		if (random < 30)
		{
			broadcastSummonSay(summon, "Oh, this is just great! What are you going to do now?");
		}
		else if (random < 70)
		{
			broadcastSummonSay(summon, "You inconsiderate moron! Can't you even take care of little old me?!");
		}
		else
		{
			broadcastSummonSay(summon, "Oh no! The man who eats one's sins has died! Penitence is further away~!");
		}
	}
	
	@RegisterEvent(EventType.ON_CREATURE_ATTACKED)
	@RegisterType(ListenerRegisterType.NPC)
	@Id(SIN_EATER)
	public void onCreatureAttacked(OnCreatureAttacked event)
	{
		if (getRandom(100) < 30)
		{
			final int random = getRandom(100);
			final Summon summon = event.getTarget().asSummon();
			if (random < 35)
			{
				broadcastSummonSay(summon, "Oh, that smarts!");
			}
			else if (random < 70)
			{
				broadcastSummonSay(summon, "Hey, master! Pay attention! I'm dying over here!");
			}
			else
			{
				broadcastSummonSay(summon, "What have I done to deserve this?");
			}
		}
	}
	
	@Override
	public void onSummonSpawn(Summon summon)
	{
		broadcastSummonSay(summon, getRandomBoolean() ? "Hey, it seems like you need my help, doesn't it?" : "Almost got it... Ouch! Stop! Damn these bloody manacles!");
		startQuestTimer("TALK", 60000, null, summon.getOwner());
	}
	
	@Override
	public void onSummonTalk(Summon summon)
	{
		if (getRandom(100) < 10)
		{
			final int random = getRandom(100);
			if (random < 25)
			{
				broadcastSummonSay(summon, "Using a special skill here could trigger a bloodbath!");
			}
			else if (random < 50)
			{
				broadcastSummonSay(summon, "Hey, what do you expect of me?");
			}
			else if (random < 75)
			{
				broadcastSummonSay(summon, "Ugggggh! Push! It's not coming out!");
			}
			else
			{
				broadcastSummonSay(summon, "Ah, I missed the mark!");
			}
		}
	}
	
	private void broadcastSummonSay(Summon summon, String string)
	{
		summon.broadcastPacket(new NpcSay(summon.getObjectId(), ChatType.NPC_GENERAL, summon.getId(), string));
	}
	
	public static void main(String[] args)
	{
		new SinEater();
	}
}
