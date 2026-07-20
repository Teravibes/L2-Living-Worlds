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
package handlers.chat.commands.voiced;

import java.util.StringTokenizer;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.custom.MultilingualSupportConfig;
import org.l2jmobius.gameserver.data.xml.NpcNameLocalisationData;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.AbstractNpcInfo.NpcInfo;
import org.l2jmobius.gameserver.network.serverpackets.DeleteObject;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class Lang implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"lang"
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar, String params)
	{
		if (!MultilingualSupportConfig.MULTILANG_ENABLE || !MultilingualSupportConfig.MULTILANG_VOICED_ALLOW)
		{
			return false;
		}
		
		final NpcHtmlMessage msg = new NpcHtmlMessage();
		if (params == null)
		{
			final StringBuilder html = new StringBuilder(100);
			for (String lang : MultilingualSupportConfig.MULTILANG_ALLOWED)
			{
				html.append("<button value=\"" + lang.toUpperCase() + "\" action=\"bypass -h voice .lang " + lang + "\" width=65 height=21 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\"><br>");
			}
			
			msg.setFile(activeChar, "data/html/mods/Lang/LanguageSelect.htm");
			msg.replace("%list%", html.toString());
			activeChar.sendPacket(msg);
			return true;
		}
		
		final StringTokenizer st = new StringTokenizer(params);
		if (st.hasMoreTokens())
		{
			final String lang = st.nextToken().trim();
			if (activeChar.setLang(lang))
			{
				msg.setFile(activeChar, "data/html/mods/Lang/Ok.htm");
				activeChar.sendPacket(msg);
				for (WorldObject obj : World.getInstance().getVisibleObjects())
				{
					if (obj.isNpc() && NpcNameLocalisationData.getInstance().hasLocalisation(obj.getId()))
					{
						activeChar.sendPacket(new DeleteObject(obj));
						ThreadPool.schedule(() ->
						{
							activeChar.sendPacket(new NpcInfo(obj.asNpc(), activeChar));
						}, 1000);
					}
				}
				
				activeChar.setTarget(null);
				return true;
			}
			
			msg.setFile(activeChar, "data/html/mods/Lang/Error.htm");
			activeChar.sendPacket(msg);
			return true;
		}
		
		return false;
	}
	
	@Override
	public String[] getCommandList()
	{
		return VOICED_COMMANDS;
	}
}
