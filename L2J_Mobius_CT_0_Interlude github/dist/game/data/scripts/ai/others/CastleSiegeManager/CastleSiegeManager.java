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
package ai.others.CastleSiegeManager;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;

/**
 * Castle Siege Manager AI.
 * @author St3eT
 */
public class CastleSiegeManager extends Script
{
	// NPCs
	private static final int[] SIEGE_MANAGER =
	{
		35104, // Gludio Castle
		35146, // Dion Castle
		35188, // Giran Castle
		35232, // Oren Castle
		35278, // Aden Castle
		35320, // Innadril Castle
		35367, // Goddard Castle
		35513, // Rune Castle
		35559, // Schuttgart Castle
		35639, // Fortress of the Dead
		35420, // Devastated Castle
	};
	
	private CastleSiegeManager()
	{
		addFirstTalkId(SIEGE_MANAGER);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		String htmltext = null;
		if (player.isClanLeader() && (player.getClanId() == npc.getCastle().getOwnerId()))
		{
			if (isInSiege(npc))
			{
				htmltext = "CastleSiegeManager.html";
			}
			else
			{
				htmltext = "CastleSiegeManager-01.html";
			}
		}
		else if (isInSiege(npc))
		{
			htmltext = "CastleSiegeManager-02.html";
		}
		else if (npc.getConquerableHall() != null)
		{
			npc.getConquerableHall().showSiegeInfo(player);
		}
		else
		{
			npc.getCastle().getSiege().listRegisterClan(player);
		}
		
		return htmltext;
	}
	
	private boolean isInSiege(Npc npc)
	{
		if ((npc.getConquerableHall() != null) && npc.getConquerableHall().isInSiege())
		{
			return true;
		}
		else if (npc.getCastle().getSiege().isInProgress())
		{
			return true;
		}
		
		return false;
	}
	
	public static void main(String[] args)
	{
		new CastleSiegeManager();
	}
}
