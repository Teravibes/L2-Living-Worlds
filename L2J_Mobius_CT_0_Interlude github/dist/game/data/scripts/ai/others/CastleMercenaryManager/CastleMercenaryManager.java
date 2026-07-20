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
package ai.others.CastleMercenaryManager;

import java.util.StringTokenizer;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.Merchant;
import org.l2jmobius.gameserver.model.clan.ClanAccess;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.sevensigns.SevenSigns;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Castle Mercenary Manager AI.
 * @author malyelfik
 */
public class CastleMercenaryManager extends Script
{
	// NPCs
	private static final int[] NPCS =
	{
		35102, // Greenspan
		35144, // Sanford
		35186, // Arvid
		35228, // Morrison
		35276, // Eldon
		35318, // Solinus
		35365, // Rowell
		35511, // Gompus
		35557, // Kendrew
	};
	
	private CastleMercenaryManager()
	{
		addStartNpc(NPCS);
		addTalkId(NPCS);
		addFirstTalkId(NPCS);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		final StringTokenizer st = new StringTokenizer(event, " ");
		switch (st.nextToken())
		{
			case "limit":
			{
				final Castle castle = npc.getCastle();
				final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
				if (castle.getName().equals("aden"))
				{
					html.setHtml(getHtm(player, "mercmanager-aden-limit.html"));
				}
				else if (castle.getName().equals("rune"))
				{
					html.setHtml(getHtm(player, "mercmanager-rune-limit.html"));
				}
				else
				{
					html.setHtml(getHtm(player, "mercmanager-limit.html"));
				}
				
				html.replace("%feud_name%", castle.getName());
				player.sendPacket(html);
				break;
			}
			case "buy":
			{
				if (SevenSigns.getInstance().isSealValidationPeriod())
				{
					final int listId = Integer.parseInt(npc.getId() + st.nextToken());
					((Merchant) npc).showBuyWindow(player, listId, false); // NOTE: Not affected by Castle Taxes, baseTax is 20% (done in merchant buylists)
				}
				else
				{
					htmltext = "mercmanager-ssq.html";
				}
				break;
			}
			case "main":
			{
				htmltext = onFirstTalk(npc, player);
				break;
			}
			case "mercmanager-01.html":
			{
				htmltext = event;
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final String htmltext;
		if (player.isGM() || ((player.getClanId() == npc.getCastle().getOwnerId()) && player.hasAccess(ClanAccess.CASTLE_MERCENARIES)))
		{
			if (npc.getCastle().getSiege().isInProgress())
			{
				htmltext = "mercmanager-siege.html";
			}
			else
			{
				switch (SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE))
				{
					case SevenSigns.CABAL_DUSK:
					{
						htmltext = "mercmanager-dusk.html";
						break;
					}
					case SevenSigns.CABAL_DAWN:
					{
						htmltext = "mercmanager-dawn.html";
						break;
					}
					default:
					{
						htmltext = "mercmanager.html";
					}
				}
			}
		}
		else
		{
			htmltext = "mercmanager-no.html";
		}
		
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new CastleMercenaryManager();
	}
}
