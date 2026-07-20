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
package ai.others;

import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.enums.ChatType;

/**
 * Turek Orcs AI - flee and return with assistance
 * @author GKR
 */

public class TurekOrcs extends Script
{
	// NPCs
	private static final int[] MOBS =
	{
		20494, // Turek War Hound
		20495, // Turek Orc Warlord
		20497, // Turek Orc Skirmisher
		20498, // Turek Orc Supplier
		20499, // Turek Orc Footman
		20500, // Turek Orc Sentinel
	};
	// Misc
	
	private static final String[] MESSAGES =
	{
		"We shall see about that!",
		"I will definitely repay this humiliation!",
		"Retreat!",
		"Tactical retreat!",
		"Mass fleeing!",
		"It's stronger than expected!",
		"I'll kill you next time!",
		"I'll definitely kill you next time!",
		"Oh! How strong!",
		"Invader!",
		"There is no reason for you to kill me! I have nothing you need!",
		"Someday you will pay!",
		"I won't just stand still while you hit me.",
		"Stop hitting!",
		"It hurts to the bone!",
		"Am I the neighborhood drum for beating!",
		"Follow me if you want!",
		"Surrender!",
		"Oh, I'm dead!",
		"I'll be back!",
		"I'll give you ten million arena if you let me live!"
	};
	
	private TurekOrcs()
	{
		addAttackId(MOBS);
		addEventReceivedId(MOBS);
		addMoveFinishedId(MOBS);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("checkState") && !npc.isDead() && (npc.getAI().getIntention() != Intention.ATTACK))
		{
			if ((npc.getCurrentHp() > (npc.getMaxHp() * 0.7)) && (npc.getVariables().getInt("state") == 2))
			{
				npc.getVariables().set("state", 3);
				npc.asAttackable().returnHome();
			}
			else
			{
				npc.getVariables().remove("state");
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (!npc.getVariables().hasVariable("isHit"))
		{
			npc.getVariables().set("isHit", 1);
		}
		else if ((npc.getCurrentHp() < (npc.getMaxHp() * 0.5)) && (npc.getCurrentHp() > (npc.getMaxHp() * 0.3)) && (attacker.getCurrentHp() > (attacker.getMaxHp() * 0.25)) && npc.hasAIValue("fleeX") && npc.hasAIValue("fleeY") && npc.hasAIValue("fleeZ") && (npc.getVariables().getInt("state") == 0) && (getRandom(100) < 10))
		{
			// Say and flee
			npc.broadcastSay(ChatType.GENERAL, getRandomEntry(MESSAGES));
			npc.disableCoreAI(true); // to avoid attacking behaviour, while flee
			npc.setRunning();
			npc.getAI().setIntention(Intention.MOVE_TO, new Location(npc.getAIValue("fleeX"), npc.getAIValue("fleeY"), npc.getAIValue("fleeZ")));
			npc.getVariables().set("state", 1);
			npc.getVariables().set("attacker", attacker.getObjectId());
		}
	}
	
	@Override
	public String onEventReceived(String eventName, Npc sender, Npc receiver, WorldObject reference)
	{
		if (eventName.equals("WARNING") && !receiver.isDead() && (receiver.getAI().getIntention() != Intention.ATTACK) && (reference != null))
		{
			final Player player = reference.asPlayer();
			if ((player != null) && !player.isDead())
			{
				receiver.getVariables().set("state", 3);
				receiver.setRunning();
				receiver.asAttackable().addDamageHate(player, 0, 99999);
				receiver.getAI().setIntention(Intention.ATTACK, player);
			}
		}
		
		return super.onEventReceived(eventName, sender, receiver, reference);
	}
	
	@Override
	public void onMoveFinished(Npc npc)
	{
		// NPC reaches flee point
		if (npc.getVariables().getInt("state") == 1)
		{
			if ((npc.getX() == npc.getAIValue("fleeX")) && (npc.getY() == npc.getAIValue("fleeY")))
			{
				npc.disableCoreAI(false);
				startQuestTimer("checkState", 15000, npc, null);
				npc.getVariables().set("state", 2);
				npc.broadcastEvent("WARNING", 400, World.getInstance().getPlayer(npc.getVariables().getInt("attacker")));
			}
			else
			{
				npc.getAI().setIntention(Intention.MOVE_TO, new Location(npc.getAIValue("fleeX"), npc.getAIValue("fleeY"), npc.getAIValue("fleeZ")));
			}
		}
		else if ((npc.getVariables().getInt("state") == 3) && npc.staysInSpawnLoc())
		{
			npc.disableCoreAI(false);
			npc.getVariables().remove("state");
		}
	}
	
	public static void main(String[] args)
	{
		new TurekOrcs();
	}
}
