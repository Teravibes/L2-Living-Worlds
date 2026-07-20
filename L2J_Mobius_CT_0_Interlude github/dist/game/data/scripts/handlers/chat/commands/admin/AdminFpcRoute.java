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

import java.util.Collection;

import org.l2jmobius.gameserver.data.xml.RouteData;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.managers.RouteRecorder;
import org.l2jmobius.gameserver.model.actor.Player;

/**
 * GM commands for recording bot movement routes in-game.
 *
 * <ul>
 * <li>{@code //record_route <name>} — starts capturing your position every ~150 units walked.</li>
 * <li>{@code //stop_route} — stops recording and writes to {@code data/routes/<name>.xml}.</li>
 * <li>{@code //list_routes} — lists all saved route names.</li>
 * </ul>
 */
public class AdminFpcRoute implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_record_route",
		"admin_stop_route",
		"admin_list_routes"
	};

	@Override
	public boolean onCommand(String command, Player player)
	{
		if (command.startsWith("admin_record_route"))
		{
			final String[] parts = command.split("\\s+", 2);
			if (parts.length < 2 || parts[1].isBlank())
			{
				player.sendMessage("Usage: //record_route <name>  (e.g. //record_route gludio_market)");
				return false;
			}
			final String name = parts[1].trim();
			final String error = RouteRecorder.startRecording(player, name);
			if (error != null)
			{
				player.sendMessage(error);
				return false;
			}
			player.sendMessage("Recording route '" + name + "'. Walk the path, then type //stop_route.");
			player.sendMessage("A waypoint is saved every 150 units. First point: " + player.getX() + ", " + player.getY() + ", " + player.getZ());
			return true;
		}

		if (command.equals("admin_stop_route"))
		{
			final String result = RouteRecorder.stopRecording(player);
			player.sendMessage(result);
			return !result.startsWith("Not recording") && !result.contains("only");
		}

		if (command.equals("admin_list_routes"))
		{
			final Collection<String> names = RouteData.getInstance().getRouteNames();
			if (names.isEmpty())
			{
				player.sendMessage("No routes saved yet.");
			}
			else
			{
				player.sendMessage("Saved routes (" + names.size() + "): " + String.join(", ", names));
			}
			return true;
		}

		return false;
	}

	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
