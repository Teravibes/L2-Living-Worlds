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
package ai.others.ValakasTeleporters;

import org.l2jmobius.gameserver.config.GrandBossConfig;
import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.managers.GrandBossManager;
import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.GrandBoss;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.zone.type.BossZone;

import ai.bosses.Valakas.Valakas;

/**
 * @author Plim, Skache
 */
public class ValakasTeleporters extends Script
{
	// NPCs
	private static final int[] NPCs =
	{
		31384, // Gatekeeper of Fire Dragon: Opening some doors.
		31385, // Heart of Volcano: Teleport into Lair of Valakas.
		31540, // Watcher of Valakas Klein: Teleport into Hall of Flames.
		31686, // Gatekeeper of Fire Dragon: Opens doors to Heart of Volcano.
		31687, // Gatekeeper of Fire Dragon: Opens doors to Heart of Volcano.
		31759, // Teleportation Cubic: Teleport out of Lair of Valakas.
	};
	
	// Items
	private static final int VACUALITE_FLOATING_STONE = 7267;
	private static final Location ENTER_HALL_OF_FLAMES = new Location(183813, -115157, -3303);
	private static final Location TELEPORT_INTO_VALAKAS_LAIR = new Location(204328, -111874, 70);
	private static final Location TELEPORT_OUT_OF_VALAKAS_LAIR = new Location(150037, -57720, -2976);
	
	private static int playerCount = 0;
	
	private ValakasTeleporters()
	{
		addStartNpc(NPCs);
		addTalkId(NPCs);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = "";
		
		switch (event)
		{
			case "ENTER":
			{
				if (hasQuestItems(player, VACUALITE_FLOATING_STONE))
				{
					takeItems(player, VACUALITE_FLOATING_STONE, 1);
					player.teleToLocation(ENTER_HALL_OF_FLAMES);
					qs.set("allowEnter", "1");
				}
				else
				{
					htmltext = "31540-06.htm";
				}
				break;
			}
			
			case "ENTER_LAIR":
			{
				if (valakasAI() != null)
				{
					int status = GrandBossManager.getInstance().getStatus(29028);
					if (((status == 0) || (status == 1)) && (qs.getInt("allowEnter") == 1))
					{
						if (playerCount >= 200)
						{
							htmltext = "31385-03.htm";
						}
						else
						{
							qs.unset("allowEnter");
							BossZone zone = GrandBossManager.getInstance().getZone(212852, -114842, -1632);
							if (zone != null)
							{
								zone.allowPlayerEntry(player, 30);
							}
							
							player.teleToLocation(TELEPORT_INTO_VALAKAS_LAIR.getX() + getRandom(600), TELEPORT_INTO_VALAKAS_LAIR.getY() + getRandom(600), TELEPORT_INTO_VALAKAS_LAIR.getZ());
							playerCount++;
							
							if (status == 0)
							{
								final GrandBoss valakas = GrandBossManager.getInstance().getBoss(29028);
								valakasAI().startQuestTimer("VALAKAS_START_ENCOUNTER", GrandBossConfig.VALAKAS_WAIT_TIME * 60000, valakas, null);
								GrandBossManager.getInstance().setStatus(29028, 1);
							}
						}
					}
					else
					{
						htmltext = (status == 2) ? "31385-02.htm" : "31385-04.htm";
					}
				}
				else
				{
					htmltext = "31385-01.htm";
				}
				break;
			}
			
			case "OPEN_DOOR_1":
			{
				DoorData.getInstance().getDoor(24210004).openMe();
				break;
			}
			
			case "OPEN_DOOR_2":
			{
				DoorData.getInstance().getDoor(24210005).openMe();
				break;
			}
			
			case "OPEN_DOOR_3":
			{
				DoorData.getInstance().getDoor(24210006).openMe();
				break;
			}
			
			case "EXIT":
			{
				player.teleToLocation(TELEPORT_OUT_OF_VALAKAS_LAIR.getX() + getRandom(500), TELEPORT_OUT_OF_VALAKAS_LAIR.getY() + getRandom(500), TELEPORT_OUT_OF_VALAKAS_LAIR.getZ());
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		switch (npc.getId())
		{
			case 31540: // Watcher of Valakas Klein
			{
				if (playerCount < 50)
				{
					return "31540-01.htm";
				}
				else if (playerCount < 100)
				{
					return "31540-02.htm";
				}
				else if (playerCount < 150)
				{
					return "31540-03.htm";
				}
				else if (playerCount < 200)
				{
					return "31540-04.htm";
				}
				else
				{
					return "31540-05.htm";
				}
			}
			default:
			{
				return npc.getId() + ".htm";
			}
		}
	}
	
	private Quest valakasAI()
	{
		return ScriptManager.getInstance().getScript(Valakas.class.getSimpleName());
	}
	
	public static void main(String[] args)
	{
		new ValakasTeleporters();
	}
}
