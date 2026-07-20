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
package handlers.bypass.npc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.listeners.AbstractEventListener;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class ScriptLink implements IBypassHandler
{
	private static final int TO_LEAD_AND_BE_LED = 118;
	private static final int THE_LEADER_AND_THE_FOLLOWER = 123;
	private static final String[] COMMANDS =
	{
		"Script"
	};
	
	@Override
	public boolean onCommand(String command, Player player, Creature target)
	{
		String scriptName = "";
		
		try
		{
			scriptName = command.substring(6).trim();
		}
		catch (IndexOutOfBoundsException ioobe)
		{
			// Handled below.
		}
		
		if (scriptName.isEmpty())
		{
			showQuestWindow(player, target.asNpc());
		}
		else
		{
			final int scriptNameEnd = scriptName.indexOf(' ');
			if (scriptNameEnd == -1)
			{
				showQuestWindow(player, target.asNpc(), scriptName);
			}
			else
			{
				player.processScriptEvent(scriptName.substring(0, scriptNameEnd), scriptName.substring(scriptNameEnd).trim());
			}
		}
		
		return true;
	}
	
	/**
	 * Open a choose quest window on client with all quests available of the Npc.<br>
	 * <br>
	 * <b><u>Actions</u>:</b><br>
	 * <li>Send a Server->Client NpcHtmlMessage containing the text of the Npc to the Player</li><br>
	 * @param player The Player that talk with the Npc
	 * @param npc The table containing quests of the Npc
	 * @param quests
	 */
	private static void showQuestChooseWindow(Player player, Npc npc, Collection<Quest> quests)
	{
		final StringBuilder sb = new StringBuilder(128);
		sb.append("<html><body>");
		String state = "";
		int questId = -1;
		for (Quest quest : quests)
		{
			if (quest == null)
			{
				continue;
			}
			
			final QuestState qs = player.getQuestState(quest.getName());
			if ((qs == null) || qs.isCreated())
			{
				state = "";
			}
			else if (qs.isStarted())
			{
				state = " (In Progress)";
			}
			else if (qs.isCompleted())
			{
				state = " (Done)";
			}
			
			sb.append("<a action=\"bypass npc_" + npc.getObjectId() + "_Script " + quest.getName() + "\">");
			sb.append("[");
			
			if (quest.isCustomQuest())
			{
				sb.append(quest.getPath() + state);
			}
			else
			{
				sb.append(quest.getDescription());
				sb.append(state);
			}
			sb.append("]</a><br>");
			
			if ((player.getApprentice() > 0) && (World.getInstance().getPlayer(player.getApprentice()) != null))
			{
				questId = quest.getId();
				
				if (questId == TO_LEAD_AND_BE_LED)
				{
					sb.append("<a action=\"bypass Script Q00118_ToLeadAndBeLed sponsor\">[" + quest.getDescription() + state + " (Sponsor)]</a><br>");
				}
				else if (questId == THE_LEADER_AND_THE_FOLLOWER)
				{
					sb.append("<a action=\"bypass Script Q00123_TheLeaderAndTheFollower sponsor\">[" + quest.getDescription() + state + " (Sponsor)]</a><br>");
				}
			}
		}
		
		sb.append("</body></html>");
		
		// Send a Server->Client packet NpcHtmlMessage to the Player in order to display the message of the Npc
		npc.insertObjectIdAndShowChatWindow(player, sb.toString());
	}
	
	/**
	 * Open a quest window on client with the text of the Npc.<br>
	 * <br>
	 * <b><u>Actions</u>:</b><br>
	 * <ul>
	 * <li>Get the text of the quest state in the folder data/scripts/quests/questId/stateId.htm</li>
	 * <li>Send a Server->Client NpcHtmlMessage containing the text of the Npc to the Player</li>
	 * <li>Send a Server->Client ActionFailed to the Player in order to avoid that the client wait another packet</li>
	 * </ul>
	 * @param player the Player that talk with the {@code npc}
	 * @param npc the Npc that chats with the {@code player}
	 * @param questId the Id of the quest to display the message
	 */
	public static void showQuestWindow(Player player, Npc npc, String questId)
	{
		String content = null;
		
		final Quest q = ScriptManager.getInstance().getScript(questId);
		
		// Get the state of the selected quest
		final QuestState qs = player.getQuestState(questId);
		if (q != null)
		{
			if (((q.getId() >= 1) && (q.getId() < 20000)) && ((player.getWeightPenalty() >= 3) || !player.isInventoryUnder90(true)))
			{
				player.sendPacket(SystemMessageId.PROGRESS_IN_A_QUEST_IS_POSSIBLE_ONLY_WHEN_YOUR_INVENTORY_S_WEIGHT_AND_VOLUME_ARE_LESS_THAN_80_PERCENT_OF_CAPACITY);
				return;
			}
			
			if ((qs == null) && (q.getId() >= 1) && (q.getId() < 20000) && (player.getAllActiveQuests().size() > 25))
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
				html.setFile(player, "data/html/fullquest.html");
				player.sendPacket(html);
				return;
			}
			
			q.notifyTalk(npc, player);
		}
		else
		{
			content = Quest.getNoQuestMsg(player); // no quests found
		}
		
		// Send a Server->Client packet NpcHtmlMessage to the Player in order to display the message of the Npc
		if (content != null)
		{
			npc.insertObjectIdAndShowChatWindow(player, content);
		}
		
		// Send a Server->Client ActionFailed to the Player in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	/**
	 * @param player
	 * @param npcId The Identifier of the NPC
	 * @return a table containing all QuestState from the table _quests in which the Player must talk to the NPC.
	 */
	private static List<QuestState> getQuestsForTalk(Player player, int npcId)
	{
		// Create a QuestState table that will contain all QuestState to modify
		final List<QuestState> states = new ArrayList<>();
		final NpcTemplate template = NpcData.getInstance().getTemplate(npcId);
		if (template == null)
		{
			LOGGER.log(Level.WARNING, ScriptLink.class.getSimpleName() + ": " + player.getName() + " requested quests for talk on non existing npc " + npcId);
			return states;
		}
		
		// Go through the QuestState of the Player quests
		for (AbstractEventListener listener : template.getListeners(EventType.ON_NPC_TALK))
		{
			if (listener.getOwner() instanceof Quest)
			{
				final Quest quest = (Quest) listener.getOwner();
				
				// Copy the current Player QuestState in the QuestState table
				final QuestState qs = player.getQuestState(quest.getName());
				if (qs != null)
				{
					states.add(qs);
				}
			}
		}
		
		// Return a table containing all QuestState to modify
		return states;
	}
	
	/**
	 * Collect awaiting quests/start points and display a QuestChooseWindow (if several available) or QuestWindow.
	 * @param player the Player that talk with the {@code npc}.
	 * @param npc the Npc that chats with the {@code player}.
	 */
	public static void showQuestWindow(Player player, Npc npc)
	{
		boolean conditionMeet = false;
		final Set<Quest> options = new HashSet<>();
		for (QuestState state : getQuestsForTalk(player, npc.getId()))
		{
			final Quest quest = state.getQuest();
			if (quest == null)
			{
				LOGGER.log(Level.WARNING, player + " Requested incorrect quest state for non existing quest: " + state.getQuestName());
				continue;
			}
			
			if ((quest.getId() > 0) && (quest.getId() < 20000))
			{
				options.add(quest);
				if (quest.canStartQuest(player))
				{
					conditionMeet = true;
				}
			}
		}
		
		for (AbstractEventListener listener : npc.getListeners(EventType.ON_NPC_QUEST_START))
		{
			if (listener.getOwner() instanceof Quest)
			{
				final Quest quest = (Quest) listener.getOwner();
				if ((quest.getId() > 0) && (quest.getId() < 20000))
				{
					options.add(quest);
					if (quest.canStartQuest(player))
					{
						conditionMeet = true;
					}
				}
			}
		}
		
		if (!conditionMeet)
		{
			showQuestWindow(player, npc, "");
		}
		else if ((options.size() > 1) || ((player.getApprentice() > 0) && (World.getInstance().getPlayer(player.getApprentice()) != null) && options.stream().anyMatch(q -> q.getId() == TO_LEAD_AND_BE_LED)))
		{
			showQuestChooseWindow(player, npc, options);
		}
		else if (options.size() == 1)
		{
			showQuestWindow(player, npc, options.stream().findFirst().get().getName());
		}
		else
		{
			showQuestWindow(player, npc, "");
		}
	}
	
	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
}
