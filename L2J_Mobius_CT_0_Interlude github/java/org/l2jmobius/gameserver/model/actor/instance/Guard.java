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
package org.l2jmobius.gameserver.model.actor.instance;

import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.config.custom.FactionSystemConfig;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.npc.OnNpcFirstTalk;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;

/**
 * This class manages all Guards in the world. It inherits all methods from Attackable and adds some more such as tracking PK and aggressive Monster.
 */
public class Guard extends Attackable
{
	/**
	 * Creates a guard.
	 * @param template the guard NPC template
	 */
	public Guard(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.Guard);
	}
	
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		if (attacker.isMonster() && !attacker.isFakePlayer())
		{
			return true;
		}
		
		if (FactionSystemConfig.FACTION_SYSTEM_ENABLED && FactionSystemConfig.FACTION_GUARDS_ENABLED && attacker.isPlayable())
		{
			final Player player = attacker.asPlayer();
			if ((player.isGood() && getTemplate().isClan(FactionSystemConfig.FACTION_EVIL_TEAM_NAME)) || (player.isEvil() && getTemplate().isClan(FactionSystemConfig.FACTION_GOOD_TEAM_NAME)))
			{
				return true;
			}
		}
		
		return super.isAutoAttackable(attacker);
	}
	
	@Override
	public void addDamage(Creature attacker, int damage, Skill skill)
	{
		super.addDamage(attacker, damage, skill);
		getAI().startFollow(attacker);
		addDamageHate(attacker, 0, 10);
		World.getInstance().forEachVisibleObjectInRange(this, Guard.class, 500, guard ->
		{
			guard.getAI().startFollow(attacker);
			guard.addDamageHate(attacker, 0, 10);
		});
	}
	
	/**
	 * Set the home location of its GuardInstance.
	 */
	@Override
	public void onSpawn()
	{
		setRandomWalking(false);
		super.onSpawn();
		
		getAI().setIntention(Intention.ACTIVE);
		
		// check the region where this mob is, do not activate the AI if region is inactive.
		// final WorldRegion region = World.getInstance().getRegion(this);
		// if ((region != null) && (!region.isActive()))
		// {
		// getAI().stopAITask();
		// }
	}
	
	/**
	 * Return the pathfile of the selected HTML file in function of the GuardInstance Identifier and of the page number.<br>
	 * <br>
	 * <b><u>Format of the pathfile</u>:</b>
	 * <ul>
	 * <li>if page number = 0 : <b>data/html/guard/12006.htm</b> (npcId-page number)</li>
	 * <li>if page number > 0 : <b>data/html/guard/12006-1.htm</b> (npcId-page number)</li>
	 * </ul>
	 * @param npcId The Identifier of the Npc whose text must be display
	 * @param value The number of the page to display
	 */
	@Override
	public String getHtmlPath(int npcId, int value)
	{
		String pom = "";
		if (value == 0)
		{
			pom = Integer.toString(npcId);
		}
		else
		{
			pom = npcId + "-" + value;
		}
		
		return "data/html/guard/" + pom + ".htm";
	}
	
	/**
	 * Manage actions when a player click on the GuardInstance.<br>
	 * <br>
	 * <b><u>Actions on first click on the GuardInstance (Select it)</u>:</b>
	 * <ul>
	 * <li>Set the GuardInstance as target of the Player player (if necessary)</li>
	 * <li>Send a Server->Client packet MyTargetSelected to the Player player (display the select window)</li>
	 * <li>Set the Player Intention to IDLE</li>
	 * <li>Send a Server->Client packet ValidateLocation to correct the GuardInstance position and heading on the client</li>
	 * </ul>
	 * <br>
	 * <b><u>Actions on second click on the GuardInstance (Attack it/Interact with it)</u>:</b>
	 * <ul>
	 * <li>If Player is in the _aggroList of the GuardInstance, set the Player Intention to ATTACK</li>
	 * <li>If Player is NOT in the _aggroList of the GuardInstance, set the Player Intention to INTERACT (after a distance verification) and show message</li>
	 * </ul>
	 * <br>
	 * <b><u>Example of use</u>:</b>
	 * <ul>
	 * <li>Client packet : Action, AttackRequest</li>
	 * </ul>
	 * @param player The Player that start an action on the GuardInstance
	 */
	@Override
	public void onAction(Player player, boolean interactValue)
	{
		if (!canTarget(player))
		{
			return;
		}
		
		boolean interact = interactValue;
		if (FactionSystemConfig.FACTION_SYSTEM_ENABLED && FactionSystemConfig.FACTION_GUARDS_ENABLED && ((player.isGood() && getTemplate().isClan(FactionSystemConfig.FACTION_EVIL_TEAM_NAME)) || (player.isEvil() && getTemplate().isClan(FactionSystemConfig.FACTION_GOOD_TEAM_NAME))))
		{
			interact = false;
			
			// TODO: Fix normal targeting
			player.getAI().setIntention(Intention.ATTACK, this);
		}
		
		if (isFakePlayer() && isInCombat())
		{
			interact = false;
			
			// TODO: Fix normal targeting
			player.getAI().setIntention(Intention.ATTACK, this);
		}
		
		// Check if the Player already target the GuardInstance
		if (getObjectId() != player.getTargetId())
		{
			// Set the target of the Player player
			player.setTarget(this);
		}
		else if (interact)
		{
			// Check if the Player is in the _aggroList of the GuardInstance
			if (isInAggroList(player))
			{
				// Set the Player Intention to ATTACK
				player.getAI().setIntention(Intention.ATTACK, this);
			}
			else
			{
				// Calculate the distance between the Player and the Npc
				if (!canInteract(player))
				{
					// Set the Player Intention to INTERACT
					player.getAI().setIntention(Intention.INTERACT, this);
				}
				else
				{
					player.setLastFolkNPC(this);
					
					// Open a chat window on client with the text of the GuardInstance
					if (hasListener(EventType.ON_NPC_QUEST_START))
					{
						player.setLastQuestNpcObject(getObjectId());
					}
					
					if (hasListener(EventType.ON_NPC_FIRST_TALK))
					{
						if (EventDispatcher.getInstance().hasListener(EventType.ON_NPC_FIRST_TALK, this))
						{
							EventDispatcher.getInstance().notifyEventAsync(new OnNpcFirstTalk(this, player), this);
						}
					}
					else
					{
						showChatWindow(player, 0);
					}
				}
			}
		}
		
		// Send a Server->Client ActionFailed to the Player in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
}
