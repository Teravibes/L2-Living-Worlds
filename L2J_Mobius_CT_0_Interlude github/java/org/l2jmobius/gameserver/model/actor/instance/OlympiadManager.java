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
package org.l2jmobius.gameserver.model.actor.instance;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.config.OlympiadConfig;
import org.l2jmobius.gameserver.data.xml.MultisellData;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.olympiad.Olympiad;
import org.l2jmobius.gameserver.network.serverpackets.ExHeroList;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Olympiad NPCs Instance
 * @author godson
 */
public class OlympiadManager extends Npc
{
	private static Logger _logOlymp = Logger.getLogger(OlympiadManager.class.getName());
	
	private static final int GATE_PASS = OlympiadConfig.OLYMPIAD_COMP_RITEM;
	private static final String FEWER_THAN = "Fewer than " + String.valueOf(OlympiadConfig.OLYMPIAD_REG_DISPLAY);
	private static final String MORE_THAN = "More than " + String.valueOf(OlympiadConfig.OLYMPIAD_REG_DISPLAY);
	
	/**
	 * Creates an olympiad manager.
	 * @param template the olympiad manager NPC template
	 */
	public OlympiadManager(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.OlympiadManager);
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (command.startsWith("OlympiadDesc"))
		{
			final int val = Integer.parseInt(command.substring(13, 14));
			final String suffix = command.substring(14);
			showChatWindow(player, val, suffix);
		}
		else if (command.startsWith("OlympiadNoble"))
		{
			if (player.getClassIndex() != 0)
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player, Olympiad.OLYMPIAD_HTML_PATH + "noble_desc5.htm");
				html.replace("%objectId%", String.valueOf(getObjectId()));
				player.sendPacket(html);
				return;
			}
			
			if (!player.isNoble() || (player.getPlayerClass().level() < 3))
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player, Olympiad.OLYMPIAD_HTML_PATH + "noble_desc6.htm");
				html.replace("%objectId%", String.valueOf(getObjectId()));
				player.sendPacket(html);
				return;
			}
			
			final int val = Integer.parseInt(command.substring(14));
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			
			switch (val)
			{
				case 1:
				{
					Olympiad.getInstance().unRegisterNoble(player);
					break;
				}
				case 2:
				{
					int classed = 0;
					int nonClassed = 0;
					final int[] array = Olympiad.getInstance().getWaitingList();
					if (array != null)
					{
						classed = array[0];
						nonClassed = array[1];
					}
					
					html.setFile(player, Olympiad.OLYMPIAD_HTML_PATH + "noble_registered.htm");
					if (OlympiadConfig.OLYMPIAD_REG_DISPLAY > 0)
					{
						html.replace("%listClassed%", classed < OlympiadConfig.OLYMPIAD_REG_DISPLAY ? FEWER_THAN : MORE_THAN);
						html.replace("%listNonClassedTeam%", FEWER_THAN);
						html.replace("%listNonClassed%", nonClassed < OlympiadConfig.OLYMPIAD_REG_DISPLAY ? FEWER_THAN : MORE_THAN);
					}
					else
					{
						html.replace("%listClassed%", String.valueOf(classed));
						html.replace("%listNonClassedTeam%", "0");
						html.replace("%listNonClassed%", String.valueOf(nonClassed));
					}
					
					html.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(html);
					break;
				}
				case 3:
				{
					final int points = Olympiad.getInstance().getNoblePoints(player.getObjectId());
					html.setFile(player, Olympiad.OLYMPIAD_HTML_PATH + "noble_points1.htm");
					html.replace("%points%", String.valueOf(points));
					html.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(html);
					break;
				}
				case 4:
				{
					Olympiad.getInstance().registerNoble(player, false);
					break;
				}
				case 5:
				{
					Olympiad.getInstance().registerNoble(player, true);
					break;
				}
				case 6:
				{
					if (player.getVariables().getInt(Olympiad.UNCLAIMED_OLYMPIAD_PASSES_VAR, 0) > 0)
					{
						html.setFile(player, Olympiad.OLYMPIAD_HTML_PATH + "noble_settle.htm");
						html.replace("%objectId%", String.valueOf(getObjectId()));
						player.sendPacket(html);
					}
					else
					{
						html.setFile(player, Olympiad.OLYMPIAD_HTML_PATH + "noble_nopoints.htm");
						html.replace("%objectId%", String.valueOf(getObjectId()));
						player.sendPacket(html);
					}
					break;
				}
				case 7:
				{
					MultisellData.getInstance().separateAndSend(102, player, this, false);
					break;
				}
				case 8:
				{
					MultisellData.getInstance().separateAndSend(103, player, this, false);
					break;
				}
				case 9:
				{
					final int point = Olympiad.getInstance().getLastNobleOlympiadPoints(player.getObjectId());
					html.setFile(player, Olympiad.OLYMPIAD_HTML_PATH + "noble_points2.htm");
					html.replace("%points%", String.valueOf(point));
					html.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(html);
					break;
				}
				case 10:
				{
					final int passes = player.getVariables().getInt(Olympiad.UNCLAIMED_OLYMPIAD_PASSES_VAR, 0);
					if (passes > 0)
					{
						player.getVariables().remove(Olympiad.UNCLAIMED_OLYMPIAD_PASSES_VAR);
						player.addItem(ItemProcessType.REWARD, GATE_PASS, passes * OlympiadConfig.OLYMPIAD_GP_PER_POINT, player, true);
					}
					break;
				}
				default:
				{
					_logOlymp.warning("Olympiad System: Could not send packet for request " + val);
					break;
				}
			}
		}
		else if (command.startsWith("Olympiad"))
		{
			final int val = Integer.parseInt(command.substring(9, 10));
			final NpcHtmlMessage reply = new NpcHtmlMessage(getObjectId());
			
			switch (val)
			{
				case 1:
				{
					final Map<Integer, String> matches = Olympiad.getInstance().getMatchList();
					reply.setFile(player, Olympiad.OLYMPIAD_HTML_PATH + "olympiad_observe1.htm");
					for (int i = 0; i < Olympiad.getStadiumCount(); i++)
					{
						final int arenaID = i + 1;
						
						// &$906; -> \\&\\$906;
						reply.replace("%title" + arenaID + "%", matches.containsKey(i) ? matches.get(i) : "\\&$906;");
					}
					
					reply.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(reply);
					break;
				}
				case 2:
				{
					// for example >> Olympiad 1_88
					final int classId = Integer.parseInt(command.substring(11));
					if (((classId >= 88) && (classId <= 118)) || ((classId >= 131) && (classId <= 134)) || (classId == 136))
					{
						final List<String> names = Olympiad.getInstance().getClassLeaderBoard(classId);
						reply.setFile(player, Olympiad.OLYMPIAD_HTML_PATH + "olympiad_ranking.htm");
						int index = 1;
						for (String name : names)
						{
							reply.replace("%place" + index + "%", String.valueOf(index));
							reply.replace("%rank" + index + "%", name);
							index++;
							if (index > 10)
							{
								break;
							}
						}
						
						for (; index <= 10; index++)
						{
							reply.replace("%place" + index + "%", "");
							reply.replace("%rank" + index + "%", "");
						}
						
						reply.replace("%objectId%", String.valueOf(getObjectId()));
						player.sendPacket(reply);
					}
					break;
				}
				case 3:
				{
					final int id = Integer.parseInt(command.substring(11));
					Olympiad.addSpectator(id, player, true);
					break;
				}
				case 4:
				{
					player.sendPacket(new ExHeroList());
					break;
				}
				default:
				{
					_logOlymp.warning("Olympiad System: Could not send packet for request " + val);
					break;
				}
			}
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
	
	public void showChatWindow(Player player, int value, String suffix)
	{
		String filename = Olympiad.OLYMPIAD_HTML_PATH;
		filename += "noble_desc" + value;
		filename += (suffix != null) ? suffix + ".htm" : ".htm";
		if (filename.equals(Olympiad.OLYMPIAD_HTML_PATH + "noble_desc0.htm"))
		{
			filename = Olympiad.OLYMPIAD_HTML_PATH + "noble_main.htm";
		}
		
		showChatWindow(player, filename);
	}
}
