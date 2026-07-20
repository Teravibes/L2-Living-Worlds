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
package handlers.bypass.npc;

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.Teleporter;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class Link implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
		"Link"
	};
	
	private static final Set<String> VALID_LINKS = new HashSet<>();
	static
	{
		VALID_LINKS.add("adventurer_guildsman/AboutHighLevelGuilds.htm");
		VALID_LINKS.add("adventurer_guildsman/AboutNewLifeCrystals.htm");
		VALID_LINKS.add("clanHallDoorman/evolve.htm");
		VALID_LINKS.add("common/augmentation_01.htm");
		VALID_LINKS.add("common/augmentation_02.htm");
		VALID_LINKS.add("common/crafting_01.htm");
		VALID_LINKS.add("common/duals_01.htm");
		VALID_LINKS.add("common/duals_02.htm");
		VALID_LINKS.add("common/duals_03.htm");
		VALID_LINKS.add("common/g_cube_warehouse001.htm");
		VALID_LINKS.add("common/weapon_sa_01.htm");
		VALID_LINKS.add("common/welcomeback002.htm");
		VALID_LINKS.add("common/welcomeback003.htm");
		VALID_LINKS.add("default/BlessingOfProtection.htm");
		VALID_LINKS.add("default/SupportMagic.htm");
		VALID_LINKS.add("fisherman/fishing_championship.htm");
		VALID_LINKS.add("fortress/foreman.htm");
		VALID_LINKS.add("olympiad/hero_main2.htm");
		VALID_LINKS.add("seven_signs/blkmrkt_1.htm");
		VALID_LINKS.add("seven_signs/blkmrkt_2.htm");
		VALID_LINKS.add("seven_signs/mammblack_1a.htm");
		VALID_LINKS.add("seven_signs/mammblack_1b.htm");
		VALID_LINKS.add("seven_signs/mammblack_1c.htm");
		VALID_LINKS.add("seven_signs/mammblack_2a.htm");
		VALID_LINKS.add("seven_signs/mammblack_2b.htm");
		VALID_LINKS.add("seven_signs/mammmerch_1.htm");
		VALID_LINKS.add("seven_signs/mammmerch_1a.htm");
		VALID_LINKS.add("seven_signs/mammmerch_1b.htm");
		VALID_LINKS.add("warehouse/clanwh.htm");
		VALID_LINKS.add("warehouse/privatewh.htm");
	}
	
	@Override
	public boolean onCommand(String command, Player player, Creature target)
	{
		final String htmlPath = command.substring(4).trim();
		if (htmlPath.isEmpty())
		{
			LOGGER.warning(player + " sent empty link html!");
			return false;
		}
		
		if (htmlPath.contains(".."))
		{
			LOGGER.warning(player + " sent invalid link html: " + htmlPath);
			return false;
		}
		
		String content = VALID_LINKS.contains(htmlPath) ? HtmCache.getInstance().getHtm(player, "data/html/" + htmlPath) : null;
		
		// Precaution.
		if (htmlPath.startsWith("teleporter/") && !(player.getTarget() instanceof Teleporter))
		{
			content = null;
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(target != null ? target.getObjectId() : 0);
		if (content != null)
		{
			html.setHtml(content.replace("%objectId%", String.valueOf(target != null ? target.getObjectId() : 0)));
		}
		
		player.sendPacket(html);
		return true;
	}
	
	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
}
