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

import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldRegion;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.ExServerPrimitive;

/**
 * @author Mobius
 */
public class AdminRegions implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_region",
		"admin_regions",
		"admin_show_region",
		"admin_show_regions",
		"admin_showregion",
		"admin_showregions",
		"admin_region_clear",
		"admin_regions_clear",
		"admin_regionclear",
		"admin_regionsclear"
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if (command.contains("clear"))
		{
			final ExServerPrimitive exsp = new ExServerPrimitive("DebugRegion", activeChar.getX(), activeChar.getY(), -16000);
			exsp.addLine(Color.BLACK, activeChar.getX(), activeChar.getY(), -16000, activeChar.getX(), activeChar.getY(), -16000);
			activeChar.sendPacket(exsp);
		}
		else
		{
			final WorldRegion region = activeChar.getWorldRegion();
			if (region != null)
			{
				final ExServerPrimitive exsp = new ExServerPrimitive("DebugRegion", activeChar.getX(), activeChar.getY(), -16000);
				
				for (WorldRegion wr : region.getSurroundingRegions())
				{
					// Calculate region boundaries.
					final int regionX = wr.getRegionX();
					final int regionY = wr.getRegionY();
					final int regionZ = wr.getRegionZ();
					
					// Convert region coordinates to world coordinates.
					final int minX = ((regionX - World.OFFSET_X) << World.SHIFT_BY);
					final int maxX = minX + (1 << World.SHIFT_BY);
					final int minY = ((regionY - World.OFFSET_Y) << World.SHIFT_BY);
					final int maxY = minY + (1 << World.SHIFT_BY);
					
					// Calculate Z boundaries from region Z index.
					final int minZ = World.WORLD_Z_MIN + (regionZ * World.Z_REGION_SIZE);
					final int maxZ = minZ + World.Z_REGION_SIZE;
					
					// Determine color - highlight current region differently.
					final Color color = (wr == region) ? Color.ORANGE : Color.GREEN;
					
					// Draw bottom rectangle (minZ).
					exsp.addLine(color, minX, minY, minZ, maxX, minY, minZ);
					exsp.addLine(color, maxX, minY, minZ, maxX, maxY, minZ);
					exsp.addLine(color, maxX, maxY, minZ, minX, maxY, minZ);
					exsp.addLine(color, minX, maxY, minZ, minX, minY, minZ);
					
					// Draw top rectangle (maxZ).
					exsp.addLine(color, minX, minY, maxZ, maxX, minY, maxZ);
					exsp.addLine(color, maxX, minY, maxZ, maxX, maxY, maxZ);
					exsp.addLine(color, maxX, maxY, maxZ, minX, maxY, maxZ);
					exsp.addLine(color, minX, maxY, maxZ, minX, minY, maxZ);
					
					// Draw vertical edges connecting bottom and top.
					exsp.addLine(color, minX, minY, minZ, minX, minY, maxZ);
					exsp.addLine(color, maxX, minY, minZ, maxX, minY, maxZ);
					exsp.addLine(color, maxX, maxY, minZ, maxX, maxY, maxZ);
					exsp.addLine(color, minX, maxY, minZ, minX, maxY, maxZ);
				}
				
				activeChar.sendPacket(exsp);
			}
		}
		
		return true;
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
