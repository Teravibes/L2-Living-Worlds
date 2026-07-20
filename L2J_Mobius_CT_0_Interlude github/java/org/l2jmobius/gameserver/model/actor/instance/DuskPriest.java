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
package org.l2jmobius.gameserver.model.actor.instance;

import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.sevensigns.SevenSigns;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class DuskPriest extends SignsPriest
{
	/**
	 * Creates a dusk priest.
	 * @param template the dusk priest NPC template
	 */
	public DuskPriest(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.DuskPriest);
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (command.startsWith("Chat"))
		{
			showChatWindow(player);
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
	
	@Override
	public void showChatWindow(Player player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		
		String filename = SevenSigns.SEVEN_SIGNS_HTML_PATH;
		final int sealGnosisOwner = SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_GNOSIS);
		final int playerCabal = SevenSigns.getInstance().getPlayerCabal(player.getObjectId());
		final boolean isSealValidationPeriod = SevenSigns.getInstance().isSealValidationPeriod();
		final boolean isCompResultsPeriod = SevenSigns.getInstance().isCompResultsPeriod();
		final int recruitPeriod = SevenSigns.getInstance().getCurrentPeriod();
		final int compWinner = SevenSigns.getInstance().getCabalHighestScore();
		
		switch (playerCabal)
		{
			case SevenSigns.CABAL_DUSK:
			{
				if (isCompResultsPeriod)
				{
					filename += "dusk_priest_5.htm";
				}
				else if (recruitPeriod == 0)
				{
					filename += "dusk_priest_6.htm";
				}
				else if (isSealValidationPeriod)
				{
					if (compWinner == SevenSigns.CABAL_DUSK)
					{
						if (compWinner != sealGnosisOwner)
						{
							filename += "dusk_priest_2c.htm";
						}
						else
						{
							filename += "dusk_priest_2a.htm";
						}
					}
					else if (compWinner == SevenSigns.CABAL_NULL)
					{
						filename += "dusk_priest_2d.htm";
					}
					else
					{
						filename += "dusk_priest_2b.htm";
					}
				}
				else
				{
					filename += "dusk_priest_1b.htm";
				}
				break;
			}
			case SevenSigns.CABAL_DAWN:
			{
				if (isSealValidationPeriod)
				{
					filename += "dusk_priest_3a.htm";
				}
				else
				{
					filename += "dusk_priest_3b.htm";
				}
				break;
			}
			default:
			{
				if (isCompResultsPeriod)
				{
					filename += "dusk_priest_5.htm";
				}
				else if (recruitPeriod == 0)
				{
					filename += "dusk_priest_6.htm";
				}
				else if (isSealValidationPeriod)
				{
					if (compWinner == SevenSigns.CABAL_DUSK)
					{
						filename += "dusk_priest_4.htm";
					}
					else if (compWinner == SevenSigns.CABAL_NULL)
					{
						filename += "dusk_priest_2d.htm";
					}
					else
					{
						filename += "dusk_priest_2b.htm";
					}
				}
				else
				{
					filename += "dusk_priest_1a.htm";
				}
				break;
			}
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player, filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}
}
