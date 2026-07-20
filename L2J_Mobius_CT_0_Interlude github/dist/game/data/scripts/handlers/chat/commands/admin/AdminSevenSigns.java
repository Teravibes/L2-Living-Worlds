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
package handlers.chat.commands.admin;

import java.util.StringTokenizer;

import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.sevensigns.SevenSigns;

/**
 * Admin command to drive the Seven Signs cycle by hand when the automatic weekly schedule is
 * disabled (see the SevenSignsNoSchedule option in Feature.ini). Lets a solo player close out the
 * competition to validate seals (opening the Anakim/Lilith rooms) and start a fresh cycle on demand.
 */
public class AdminSevenSigns implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_signs"
	};

	private static final String USAGE = "Usage: //signs <status|validate|reset>";

	@Override
	public boolean onCommand(String command, Player player)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		st.nextToken(); // admin_signs
		final String action = st.hasMoreTokens() ? st.nextToken().toLowerCase() : "status";

		final SevenSigns signs = SevenSigns.getInstance();
		switch (action)
		{
			case "status":
			{
				player.sendMessage(signs.getSummaryText());
				break;
			}
			case "validate":
			{
				if (!FeatureConfig.ALT_SEVENSIGNS_NO_SCHEDULE)
				{
					player.sendMessage("Manual Seven Signs control is disabled. Set SevenSignsNoSchedule = True in Feature.ini to use it.");
					break;
				}
				if (signs.isSealValidationPeriod())
				{
					player.sendMessage("The Seal Validation period is already active. Use //signs reset to start a new cycle first.");
					break;
				}
				if (signs.getCabalHighestScore() == SevenSigns.CABAL_NULL)
				{
					player.sendMessage("No cabal has contributed yet, so no seal would be captured. Register with a Priest of Dawn / Priestess of Dusk and contribute seal stones first.");
					break;
				}
				signs.forceSealValidation();
				player.sendMessage("Seven Signs: competition closed. " + signs.getSummaryText());
				break;
			}
			case "reset":
			{
				if (!FeatureConfig.ALT_SEVENSIGNS_NO_SCHEDULE)
				{
					player.sendMessage("Manual Seven Signs control is disabled. Set SevenSignsNoSchedule = True in Feature.ini to use it.");
					break;
				}
				signs.forceNewCycle();
				player.sendMessage("Seven Signs: a new cycle has begun - register again to take part. " + signs.getSummaryText());
				break;
			}
			default:
			{
				player.sendMessage(USAGE);
				break;
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
