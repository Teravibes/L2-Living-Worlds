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

import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.managers.PhantomManager;
import org.l2jmobius.gameserver.managers.PhantomPartyManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Player;

/**
 * Admin control for the real-Player phantom slice.<br>
 * Usage:
 * <ul>
 * <li>{@code //phantom spawn [count] [level]} - spawn N clientless phantom fighters at your position
 * (default 1) brought to the given level (default 1).</li>
 * <li>{@code //phantom reload} - re-read PhantomPopulations.xml live (populations + editor-authored
 * {@code <friend>} orders): everything despawns, zones redeploy on approach, friends rejoin in ~15s.</li>
 * <li>{@code //phantom clear} - despawn all phantoms.</li>
 * <li>{@code //phantom count} - report how many are active.</li>
 * <li>{@code //phantom debug [on|off]} - toggle the raid combat trace (logs to the gameserver console).</li>
 * </ul>
 */
public class AdminPhantom implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_phantom"
	};

	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		final String[] words = command.split(" ");
		if (words.length < 2)
		{
			activeChar.sendSysMessage("Usage: //phantom spawn [count] [level] | reload | clear | count | debug [on|off]");
			return false;
		}

		switch (words[1].toLowerCase())
		{
			case "spawn":
			{
				int count = 1;
				if (words.length > 2)
				{
					try
					{
						count = Math.max(1, Math.min(20, Integer.parseInt(words[2])));
					}
					catch (NumberFormatException e)
					{
						activeChar.sendSysMessage("Count must be a number (1-20).");
						return false;
					}
				}

				int level = 1;
				if (words.length > 3)
				{
					try
					{
						level = Math.max(1, Math.min(80, Integer.parseInt(words[3])));
					}
					catch (NumberFormatException e)
					{
						activeChar.sendSysMessage("Level must be a number (1-80).");
						return false;
					}
				}

				int spawned = 0;
				for (int i = 0; i < count; i++)
				{
					// Scatter slightly around the admin so they do not stack on one tile.
					final Location location = new Location(activeChar.getX() + ((i % 5) * 40), activeChar.getY() + ((i / 5) * 40), activeChar.getZ());
					if (PhantomManager.getInstance().spawnPhantom(location, level) != null)
					{
						spawned++;
					}
				}
				activeChar.sendSysMessage("Spawned " + spawned + "/" + count + " phantom(s) at level " + level + ". Active: " + PhantomManager.getInstance().getCount());
				break;
			}
			case "reload":
			{
				// Re-read PhantomPopulations.xml live: populations + editor-authored <friend> orders.
				activeChar.sendSysMessage(PhantomManager.getInstance().reloadPopulations());
				break;
			}
			case "clear":
			{
				final int removed = PhantomManager.getInstance().clear();
				activeChar.sendSysMessage("Despawned " + removed + " phantom(s).");
				break;
			}
			case "count":
			{
				activeChar.sendSysMessage("Active phantoms: " + PhantomManager.getInstance().getCount());
				break;
			}
			case "debug":
			{
				// Toggle the raid combat trace (//phantom debug on|off). Logs go to the gameserver log, raid-only.
				if (words.length > 2)
				{
					PhantomPartyManager.DEBUG = words[2].equalsIgnoreCase("on") || words[2].equalsIgnoreCase("true") || words[2].equals("1");
				}
				else
				{
					PhantomPartyManager.DEBUG = !PhantomPartyManager.DEBUG; // no arg = flip it
				}
				activeChar.sendSysMessage("Phantom raid debug trace: " + (PhantomPartyManager.DEBUG ? "ON" : "OFF") + " (logs to the gameserver console).");
				break;
			}
			default:
			{
				activeChar.sendSysMessage("Usage: //phantom spawn [count] [level] | reload | clear | count | debug [on|off]");
				return false;
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
