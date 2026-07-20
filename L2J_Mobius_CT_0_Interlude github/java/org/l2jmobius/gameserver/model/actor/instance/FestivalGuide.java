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
package org.l2jmobius.gameserver.model.actor.instance;

import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.sevensigns.SevenSigns;
import org.l2jmobius.gameserver.model.sevensigns.SevenSignsFestival;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Festival of Darkness Guide (Seven Signs)
 * @author Tempy
 */
public class FestivalGuide extends Npc
{
	private final int _festivalType;
	private final int _festivalOracle;
	private final int _blueStonesNeeded;
	private final int _greenStonesNeeded;
	private final int _redStonesNeeded;
	
	/**
	 * Creates a festival guide.
	 * @param template the festival guide NPC template
	 */
	public FestivalGuide(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.FestivalGuide);
		
		switch (getId())
		{
			case 31127:
			case 31132:
			{
				_festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_31;
				_festivalOracle = SevenSigns.CABAL_DAWN;
				_blueStonesNeeded = 900;
				_greenStonesNeeded = 540;
				_redStonesNeeded = 270;
				break;
			}
			case 31128:
			case 31133:
			{
				_festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_42;
				_festivalOracle = SevenSigns.CABAL_DAWN;
				_blueStonesNeeded = 1500;
				_greenStonesNeeded = 900;
				_redStonesNeeded = 450;
				break;
			}
			case 31129:
			case 31134:
			{
				_festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_53;
				_festivalOracle = SevenSigns.CABAL_DAWN;
				_blueStonesNeeded = 3000;
				_greenStonesNeeded = 1800;
				_redStonesNeeded = 900;
				break;
			}
			case 31130:
			case 31135:
			{
				_festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_64;
				_festivalOracle = SevenSigns.CABAL_DAWN;
				_blueStonesNeeded = 4500;
				_greenStonesNeeded = 2700;
				_redStonesNeeded = 1350;
				break;
			}
			case 31131:
			case 31136:
			{
				_festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_NONE;
				_festivalOracle = SevenSigns.CABAL_DAWN;
				_blueStonesNeeded = 6000;
				_greenStonesNeeded = 3600;
				_redStonesNeeded = 1800;
				break;
			}
			case 31137:
			case 31142:
			{
				_festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_31;
				_festivalOracle = SevenSigns.CABAL_DUSK;
				_blueStonesNeeded = 900;
				_greenStonesNeeded = 540;
				_redStonesNeeded = 270;
				break;
			}
			case 31138:
			case 31143:
			{
				_festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_42;
				_festivalOracle = SevenSigns.CABAL_DUSK;
				_blueStonesNeeded = 1500;
				_greenStonesNeeded = 900;
				_redStonesNeeded = 450;
				break;
			}
			case 31139:
			case 31144:
			{
				_festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_53;
				_festivalOracle = SevenSigns.CABAL_DUSK;
				_blueStonesNeeded = 3000;
				_greenStonesNeeded = 1800;
				_redStonesNeeded = 900;
				break;
			}
			case 31140:
			case 31145:
			{
				_festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_64;
				_festivalOracle = SevenSigns.CABAL_DUSK;
				_blueStonesNeeded = 4500;
				_greenStonesNeeded = 2700;
				_redStonesNeeded = 1350;
				break;
			}
			case 31141:
			case 31146:
			{
				_festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_NONE;
				_festivalOracle = SevenSigns.CABAL_DUSK;
				_blueStonesNeeded = 6000;
				_greenStonesNeeded = 3600;
				_redStonesNeeded = 1800;
				break;
			}
			default:
			{
				_festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_NONE;
				_festivalOracle = SevenSigns.CABAL_NULL;
				_blueStonesNeeded = 0;
				_greenStonesNeeded = 0;
				_redStonesNeeded = 0;
			}
		}
	}
	
	public int getFestivalType()
	{
		return _festivalType;
	}
	
	public int getFestivalOracle()
	{
		return _festivalOracle;
	}
	
	public int getStoneCount(int stoneType)
	{
		switch (stoneType)
		{
			case SevenSigns.SEAL_STONE_BLUE_ID:
			{
				return _blueStonesNeeded;
			}
			case SevenSigns.SEAL_STONE_GREEN_ID:
			{
				return _greenStonesNeeded;
			}
			case SevenSigns.SEAL_STONE_RED_ID:
			{
				return _redStonesNeeded;
			}
			default:
			{
				return -1;
			}
		}
	}
	
	public void showChatWindow(Player player, int value, String suffix, boolean isDescription)
	{
		String filename = SevenSigns.SEVEN_SIGNS_HTML_PATH + "festival/";
		filename += (isDescription) ? "desc_" : "festival_";
		filename += (suffix != null) ? value + suffix + ".htm" : value + ".htm";
		
		// Send a Server->Client NpcHtmlMessage containing the text of the Npc to the Player
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player, filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%festivalType%", SevenSignsFestival.getFestivalName(_festivalType));
		html.replace("%cycleMins%", String.valueOf(SevenSignsFestival.getInstance().getMinsToNextCycle()));
		if (!isDescription && "2b".equals(value + suffix))
		{
			html.replace("%minFestivalPartyMembers%", String.valueOf(FeatureConfig.ALT_FESTIVAL_MIN_PLAYER));
		}
		
		// If the stats or bonus table is required, construct them.
		if (value == 5)
		{
			html.replace("%statsTable%", getStatsTable());
		}
		
		if (value == 6)
		{
			html.replace("%bonusTable%", getBonusTable());
		}
		
		// festival's fee
		if (value == 1)
		{
			html.replace("%blueStoneNeeded%", String.valueOf(_blueStonesNeeded));
			html.replace("%greenStoneNeeded%", String.valueOf(_greenStonesNeeded));
			html.replace("%redStoneNeeded%", String.valueOf(_redStonesNeeded));
		}
		
		player.sendPacket(html);
		
		// Send a Server->Client ActionFailed to the Player in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	private static String getStatsTable()
	{
		final StringBuilder tableHtml = new StringBuilder(1000);
		
		// Get the scores for each of the festival level ranges (types).
		for (int i = 0; i < 5; i++)
		{
			final int dawnScore = SevenSignsFestival.getInstance().getHighestScore(SevenSigns.CABAL_DAWN, i);
			final int duskScore = SevenSignsFestival.getInstance().getHighestScore(SevenSigns.CABAL_DUSK, i);
			final String festivalName = SevenSignsFestival.getFestivalName(i);
			String winningCabal = "Children of Dusk";
			if (dawnScore > duskScore)
			{
				winningCabal = "Children of Dawn";
			}
			else if (dawnScore == duskScore)
			{
				winningCabal = "None";
			}
			
			tableHtml.append("<tr><td width=\"100\" align=\"center\">" + festivalName + "</td><td align=\"center\" width=\"35\">" + duskScore + "</td><td align=\"center\" width=\"35\">" + dawnScore + "</td><td align=\"center\" width=\"130\">" + winningCabal + "</td></tr>");
		}
		
		return tableHtml.toString();
	}
	
	private static String getBonusTable()
	{
		final StringBuilder tableHtml = new StringBuilder(500);
		
		// Get the accumulated scores for each of the festival level ranges (types).
		for (int i = 0; i < 5; i++)
		{
			final int accumScore = SevenSignsFestival.getInstance().getAccumulatedBonus(i);
			final String festivalName = SevenSignsFestival.getFestivalName(i);
			tableHtml.append("<tr><td align=\"center\" width=\"150\">" + festivalName + "</td><td align=\"center\" width=\"150\">" + accumScore + "</td></tr>");
		}
		
		return tableHtml.toString();
	}
}
