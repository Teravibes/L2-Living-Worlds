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

import org.l2jmobius.gameserver.data.xml.AdminData;
import org.l2jmobius.gameserver.data.xml.FakePlayerData;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.managers.FakePlayerBehaviorManager;
import org.l2jmobius.gameserver.managers.FakePlayerChatManager;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;

/**
 * @author Mobius
 */
public class AdminFakePlayers implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_fakechat",
		"admin_reloadfakeplayers"
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if (command.equals("admin_reloadfakeplayers"))
		{
			activeChar.sendSysMessage("Reloading fake players — despawning current bots...");
			final int removed = FakePlayerBehaviorManager.getInstance().reload();
			final String msg = activeChar.getName() + ": Reloaded fake players (" + removed + " bots despawned, redeploying from XML).";
			AdminData.getInstance().broadcastMessageToGMs(msg);
			activeChar.sendSysMessage("Done. " + removed + " bots removed — new bots deploy in ~15s.");
		}
		else if (command.startsWith("admin_fakechat"))
		{
			final String[] words = command.substring(15).split(" ");
			if (words.length < 3)
			{
				activeChar.sendSysMessage("Usage: //fakechat playername fpcname message");
				return false;
			}
			
			final Player player = World.getInstance().getPlayer(words[0]);
			if (player == null)
			{
				activeChar.sendSysMessage("Player not found.");
				return false;
			}
			
			final String fpcName = FakePlayerData.getInstance().getProperName(words[1]);
			if (fpcName == null)
			{
				activeChar.sendSysMessage("Fake player not found.");
				return false;
			}
			
			String message = "";
			for (int i = 0; i < words.length; i++)
			{
				if (i < 2)
				{
					continue;
				}
				
				message += (words[i] + " ");
			}
			
			FakePlayerChatManager.getInstance().sendChat(player, fpcName, message);
			activeChar.sendSysMessage("Your message has been sent.");
		}
		
		return true;
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
