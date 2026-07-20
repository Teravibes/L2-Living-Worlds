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
package custom.FactionSystem;

import org.l2jmobius.gameserver.config.custom.FactionSystemConfig;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.appearance.PlayerAppearance;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author Mobius
 */
public class FactionSystem extends Script
{
	// NPCs
	private static final int MANAGER = 500;
	
	// Other
	private static final String[] TEXTS =
	{
		FactionSystemConfig.FACTION_GOOD_TEAM_NAME + " or " + FactionSystemConfig.FACTION_EVIL_TEAM_NAME + "?",
		"Select your faction!",
		"The choice is yours!"
	};
	
	private FactionSystem()
	{
		addSpawnId(MANAGER);
		addStartNpc(MANAGER);
		addTalkId(MANAGER);
		addFirstTalkId(MANAGER);
		
		if (FactionSystemConfig.FACTION_SYSTEM_ENABLED)
		{
			addSpawn(MANAGER, FactionSystemConfig.FACTION_MANAGER_LOCATION, false, 0);
		}
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "selectGoodFaction":
			{
				if (FactionSystemConfig.FACTION_BALANCE_ONLINE_PLAYERS && (World.getInstance().getAllGoodPlayers().size() >= (World.getInstance().getAllEvilPlayers().size() + FactionSystemConfig.FACTION_BALANCE_PLAYER_EXCEED_LIMIT)))
				{
					final String htmltext = null;
					final NpcHtmlMessage packet = new NpcHtmlMessage(npc.getObjectId());
					packet.setHtml(getHtm(player, "onlinelimit.html"));
					packet.replace("%name%", player.getName());
					packet.replace("%more%", FactionSystemConfig.FACTION_GOOD_TEAM_NAME);
					packet.replace("%less%", FactionSystemConfig.FACTION_EVIL_TEAM_NAME);
					player.sendPacket(packet);
					return htmltext;
				}
				
				if (FactionSystemConfig.FACTION_AUTO_NOBLESS)
				{
					player.setNoble(true);
				}
				
				player.setGood();
				final PlayerAppearance appearance = player.getAppearance();
				appearance.setNameColor(FactionSystemConfig.FACTION_GOOD_NAME_COLOR);
				appearance.setTitleColor(FactionSystemConfig.FACTION_GOOD_NAME_COLOR);
				player.setTitle(FactionSystemConfig.FACTION_GOOD_TEAM_NAME);
				player.sendMessage("You are now fighting for the " + FactionSystemConfig.FACTION_GOOD_TEAM_NAME + " faction.");
				player.teleToLocation(FactionSystemConfig.FACTION_GOOD_BASE_LOCATION);
				broadcastMessageToFaction(FactionSystemConfig.FACTION_GOOD_TEAM_NAME, FactionSystemConfig.FACTION_GOOD_TEAM_NAME + " faction grows stronger with the arrival of " + player.getName() + ".");
				World.addFactionPlayerToWorld(player);
				break;
			}
			case "selectEvilFaction":
			{
				if (FactionSystemConfig.FACTION_BALANCE_ONLINE_PLAYERS && (World.getInstance().getAllEvilPlayers().size() >= (World.getInstance().getAllGoodPlayers().size() + FactionSystemConfig.FACTION_BALANCE_PLAYER_EXCEED_LIMIT)))
				{
					final String htmltext = null;
					final NpcHtmlMessage packet = new NpcHtmlMessage(npc.getObjectId());
					packet.setHtml(getHtm(player, "onlinelimit.html"));
					packet.replace("%name%", player.getName());
					packet.replace("%more%", FactionSystemConfig.FACTION_EVIL_TEAM_NAME);
					packet.replace("%less%", FactionSystemConfig.FACTION_GOOD_TEAM_NAME);
					player.sendPacket(packet);
					return htmltext;
				}
				
				if (FactionSystemConfig.FACTION_AUTO_NOBLESS)
				{
					player.setNoble(true);
				}
				
				player.setEvil();
				final PlayerAppearance appearance = player.getAppearance();
				appearance.setNameColor(FactionSystemConfig.FACTION_EVIL_NAME_COLOR);
				appearance.setTitleColor(FactionSystemConfig.FACTION_EVIL_NAME_COLOR);
				player.setTitle(FactionSystemConfig.FACTION_EVIL_TEAM_NAME);
				player.sendMessage("You are now fighting for the " + FactionSystemConfig.FACTION_EVIL_TEAM_NAME + " faction.");
				player.teleToLocation(FactionSystemConfig.FACTION_EVIL_BASE_LOCATION);
				broadcastMessageToFaction(FactionSystemConfig.FACTION_EVIL_TEAM_NAME, FactionSystemConfig.FACTION_EVIL_TEAM_NAME + " faction grows stronger with the arrival of " + player.getName() + ".");
				World.addFactionPlayerToWorld(player);
				break;
			}
			case "SPEAK":
			{
				if (npc != null)
				{
					npc.broadcastSay(ChatType.NPC_GENERAL, getRandomEntry(TEXTS), 1500);
				}
				break;
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final String htmltext = null;
		final NpcHtmlMessage packet = new NpcHtmlMessage(npc.getObjectId());
		packet.setHtml(getHtm(player, "manager.html"));
		packet.replace("%name%", player.getName());
		packet.replace("%good%", FactionSystemConfig.FACTION_GOOD_TEAM_NAME);
		packet.replace("%evil%", FactionSystemConfig.FACTION_EVIL_TEAM_NAME);
		player.sendPacket(packet);
		return htmltext;
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		if (npc.getId() == MANAGER)
		{
			startQuestTimer("SPEAK", 10000, npc, null, true);
		}
	}
	
	private void broadcastMessageToFaction(String factionName, String message)
	{
		if (factionName.equals(FactionSystemConfig.FACTION_GOOD_TEAM_NAME))
		{
			for (Player player : World.getInstance().getAllGoodPlayers())
			{
				player.sendMessage(message);
			}
		}
		else
		{
			for (Player player : World.getInstance().getAllEvilPlayers())
			{
				player.sendMessage(message);
			}
		}
	}
	
	public static void main(String[] args)
	{
		new FactionSystem();
	}
}
