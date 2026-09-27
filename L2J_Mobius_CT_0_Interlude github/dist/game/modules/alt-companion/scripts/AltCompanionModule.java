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
package modules.altcompanion;

import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.modules.GameModule;
import org.l2jmobius.gameserver.modules.ModuleCompanions;
import org.l2jmobius.gameserver.modules.ModuleContext;

/**
 * The Alt Companion module. The {@code .alt Name} command brings one of the player's own characters, from any account,
 * into their party as a clientless member run by the party AI. It plays its class, earns party experience, uses its own
 * soulshots and potions, and is saved when it leaves the party (removed, owner logged out, dead past the res window, or
 * its own account logged in). It never learns skills by itself and loses experience on death like any character.
 * Everything else is the platform's companion extension point.
 */
public class AltCompanionModule implements GameModule
{
	@Override
	public void onEnable(ModuleContext context)
	{
		if (!context.config().getBoolean("Enabled", false))
		{
			return; // Switch off: register nothing, behave as stock.
		}

		context.handlers().registerVoicedCommand(new AltVoicedCommand(context.companions()));
		context.logging().info("Alt Companion module enabled, registered voiced command .alt");
	}

	private static class AltVoicedCommand implements IVoicedCommandHandler
	{
		private static final String[] COMMANDS =
		{
			"alt"
		};

		private final ModuleCompanions _companions;

		AltVoicedCommand(ModuleCompanions companions)
		{
			_companions = companions;
		}

		@Override
		public boolean onCommand(String command, Player player, String params)
		{
			if (player == null)
			{
				return false;
			}

			final String name = (params == null) ? "" : params.trim();
			if (name.isEmpty())
			{
				player.sendMessage("Usage: .alt <character name>. Brings one of your characters into your party. Remove it from the party to send it home.");
				return true;
			}

			final int charId = CharInfoTable.getInstance().getIdByName(name);
			if (charId <= 0)
			{
				player.sendMessage("There is no character named " + name + ".");
				return true;
			}

			final String realName = CharInfoTable.getInstance().getNameById(charId);
			final int ownerId = player.getObjectId();
			switch (_companions.summon(player, charId, () -> onLeft(ownerId, realName)))
			{
				case JOINED:
				{
					player.sendMessage(realName + " joined your party.");
					break;
				}
				case NOT_FOUND:
				{
					player.sendMessage("There is no character named " + name + ".");
					break;
				}
				case BOT:
				{
					player.sendMessage(realName + " is a bot and cannot be summoned.");
					break;
				}
				case ALREADY_ONLINE:
				{
					player.sendMessage(realName + " is already in the world.");
					break;
				}
				case DEAD:
				{
					player.sendMessage(realName + " is dead. Log in to it and revive it first.");
					break;
				}
				case PARTY_CLOSED:
				{
					player.sendMessage("Your party is full, or you are not its leader.");
					break;
				}
				default:
				{
					player.sendMessage(realName + " could not join your party.");
					break;
				}
			}
			return true;
		}

		private static void onLeft(int ownerId, String name)
		{
			final Player owner = World.getInstance().getPlayer(ownerId);
			if ((owner != null) && owner.isOnline())
			{
				owner.sendMessage(name + " left your party and was saved.");
			}
		}

		@Override
		public String[] getCommandList()
		{
			return COMMANDS;
		}
	}
}
