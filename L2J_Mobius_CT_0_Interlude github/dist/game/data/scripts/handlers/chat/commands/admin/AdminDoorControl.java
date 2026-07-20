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
package handlers.chat.commands.admin;

import java.awt.Color;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.Door;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.network.serverpackets.ExServerPrimitive;

/**
 * This class handles following admin commands: - open1 = open coloseum door 24190001 - open2 = open coloseum door 24190002 - open3 = open coloseum door 24190003 - open4 = open coloseum door 24190004 - openall = open all coloseum door - close1 = close coloseum door 24190001 - close2 = close coloseum
 * door 24190002 - close3 = close coloseum door 24190003 - close4 = close coloseum door 24190004 - closeall = close all coloseum door - open = open selected door - close = close selected door
 * @version $Revision: 1.2.4.5 $ $Date: 2005/04/11 10:06:06 $
 */
public class AdminDoorControl implements IAdminCommandHandler
{
	private static final Logger LOGGER = Logger.getLogger(AdminDoorControl.class.getName());
	
	private static final Map<Player, Set<Integer>> PLAYER_SHOWN_DOORS = new ConcurrentHashMap<>();
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_open",
		"admin_close",
		"admin_openall",
		"admin_closeall",
		"admin_showdoors",
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		try
		{
			if (command.startsWith("admin_open "))
			{
				final int doorId = Integer.parseInt(command.substring(11));
				final Door door = DoorData.getInstance().getDoor(doorId);
				if (door != null)
				{
					door.openMe();
				}
				else
				{
					for (Castle castle : CastleManager.getInstance().getCastles())
					{
						if (castle.getDoor(doorId) != null)
						{
							castle.getDoor(doorId).openMe();
						}
					}
				}
			}
			else if (command.startsWith("admin_close "))
			{
				final int doorId = Integer.parseInt(command.substring(12));
				final Door door = DoorData.getInstance().getDoor(doorId);
				if (door != null)
				{
					door.closeMe();
				}
				else
				{
					for (Castle castle : CastleManager.getInstance().getCastles())
					{
						if (castle.getDoor(doorId) != null)
						{
							castle.getDoor(doorId).closeMe();
						}
					}
				}
			}
			else if (command.equals("admin_closeall"))
			{
				for (Door door : DoorData.getInstance().getDoors())
				{
					door.closeMe();
				}
				
				for (Castle castle : CastleManager.getInstance().getCastles())
				{
					for (Door door : castle.getDoors())
					{
						door.closeMe();
					}
				}
			}
			else if (command.equals("admin_openall"))
			{
				for (Door door : DoorData.getInstance().getDoors())
				{
					door.openMe();
				}
				
				for (Castle castle : CastleManager.getInstance().getCastles())
				{
					for (Door door : castle.getDoors())
					{
						door.openMe();
					}
				}
			}
			else if (command.equals("admin_open"))
			{
				final WorldObject target = activeChar.getTarget();
				if ((target != null) && target.isDoor())
				{
					target.asDoor().openMe();
				}
				else
				{
					activeChar.sendSysMessage("Incorrect target.");
				}
			}
			else if (command.equals("admin_close"))
			{
				final WorldObject target = activeChar.getTarget();
				if ((target != null) && target.isDoor())
				{
					target.asDoor().closeMe();
				}
				else
				{
					activeChar.sendSysMessage("Incorrect target.");
				}
			}
			else if (command.contains("admin_showdoors"))
			{
				if (command.contains("off"))
				{
					final Set<Integer> doorIds = PLAYER_SHOWN_DOORS.get(activeChar);
					if (doorIds == null)
					{
						return true;
					}
					
					for (int doorId : doorIds)
					{
						final ExServerPrimitive exsp = new ExServerPrimitive("Door" + doorId, activeChar.getX(), activeChar.getY(), -16000);
						exsp.addLine(Color.BLACK, activeChar.getX(), activeChar.getY(), -16000, activeChar.getX(), activeChar.getY(), -16000);
						activeChar.sendPacket(exsp);
					}
					
					doorIds.clear();
					PLAYER_SHOWN_DOORS.remove(activeChar);
				}
				else
				{
					final Set<Integer> doorIds;
					if (PLAYER_SHOWN_DOORS.containsKey(activeChar))
					{
						doorIds = PLAYER_SHOWN_DOORS.get(activeChar);
					}
					else
					{
						doorIds = new HashSet<>();
						PLAYER_SHOWN_DOORS.put(activeChar, doorIds);
					}
					
					World.getInstance().forEachVisibleObject(activeChar, Door.class, door ->
					{
						if (doorIds.contains(door.getId()))
						{
							return;
						}
						
						doorIds.add(door.getId());
						
						final ExServerPrimitive packet = new ExServerPrimitive("Door" + door.getId(), activeChar.getX(), activeChar.getY(), -16000);
						final Color color = door.isOpen() ? Color.GREEN : Color.RED;
						
						// box 1
						packet.addLine(color, door.getX(0), door.getY(0), door.getZMin(), door.getX(1), door.getY(1), door.getZMin());
						packet.addLine(color, door.getX(1), door.getY(1), door.getZMin(), door.getX(2), door.getY(2), door.getZMax());
						packet.addLine(color, door.getX(2), door.getY(2), door.getZMax(), door.getX(3), door.getY(3), door.getZMax());
						packet.addLine(color, door.getX(3), door.getY(3), door.getZMax(), door.getX(0), door.getY(0), door.getZMin());
						
						// box 2
						packet.addLine(color, door.getX(0), door.getY(0), door.getZMax(), door.getX(1), door.getY(1), door.getZMax());
						packet.addLine(color, door.getX(1), door.getY(1), door.getZMax(), door.getX(2), door.getY(2), door.getZMin());
						packet.addLine(color, door.getX(2), door.getY(2), door.getZMin(), door.getX(3), door.getY(3), door.getZMin());
						packet.addLine(color, door.getX(3), door.getY(3), door.getZMin(), door.getX(0), door.getY(0), door.getZMax());
						
						// diagonals
						packet.addLine(color, door.getX(0), door.getY(0), door.getZMin(), door.getX(1), door.getY(1), door.getZMax());
						packet.addLine(color, door.getX(2), door.getY(2), door.getZMin(), door.getX(3), door.getY(3), door.getZMax());
						packet.addLine(color, door.getX(0), door.getY(0), door.getZMax(), door.getX(1), door.getY(1), door.getZMin());
						packet.addLine(color, door.getX(2), door.getY(2), door.getZMax(), door.getX(3), door.getY(3), door.getZMin());
						activeChar.sendPacket(packet);
						
						// send message
						activeChar.sendSysMessage("Found door " + door.getId());
					});
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("Problem with AdminDoorControl: " + e.getMessage());
		}
		
		return true;
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
