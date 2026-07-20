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

import java.text.DateFormat;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.managers.IdManager;
import org.l2jmobius.gameserver.managers.games.LotteryManager;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class Loto implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
		"Loto"
	};
	
	@Override
	public boolean onCommand(String command, Player player, Creature target)
	{
		if (!target.isNpc())
		{
			return false;
		}
		
		int val = 0;
		try
		{
			val = Integer.parseInt(command.substring(5));
		}
		catch (Exception e)
		{
			// Handled above.
		}
		
		if (val == 0)
		{
			// new loto ticket
			for (int i = 0; i < 5; i++)
			{
				player.setLoto(i, 0);
			}
		}
		
		showLotoWindow(player, target.asNpc(), val);
		
		return false;
	}
	
	/**
	 * Open a Loto window on client with the text of the Npc.<br>
	 * <br>
	 * <b><u>Actions</u>:</b><br>
	 * <li>Get the text of the selected HTML file in function of the npcId and of the page number</li>
	 * <li>Send a Server->Client NpcHtmlMessage containing the text of the Npc to the Player</li>
	 * <li>Send a Server->Client ActionFailed to the Player in order to avoid that the client wait another packet</li><br>
	 * @param player The Player that talk with the Npc
	 * @param npc Npc loto instance
	 * @param value The number of the page of the Npc to display
	 */
	// 0 - first buy lottery ticket window
	// 1-20 - buttons
	// 21 - second buy lottery ticket window
	// 22 - selected ticket with 5 numbers
	// 23 - current lottery jackpot
	// 24 - Previous winning numbers/Prize claim
	// >24 - check lottery ticket by item object id
	public static void showLotoWindow(Player player, Npc npc, int value)
	{
		final int npcId = npc.getTemplate().getId();
		String filename;
		SystemMessage sm;
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		
		if (value == 0) // 0 - first buy lottery ticket window
		{
			filename = (npc.getHtmlPath(npcId, 1));
			html.setFile(player, filename);
		}
		else if ((value >= 1) && (value <= 21)) // 1-20 - buttons, 21 - second buy lottery ticket window
		{
			if (!LotteryManager.getInstance().isStarted())
			{
				// tickets can't be sold
				player.sendPacket(SystemMessageId.LOTTERY_TICKETS_ARE_NOT_CURRENTLY_BEING_SOLD);
				return;
			}
			
			if (!LotteryManager.getInstance().isSellableTickets())
			{
				// tickets can't be sold
				player.sendPacket(SystemMessageId.TICKETS_FOR_THE_CURRENT_LOTTERY_ARE_NO_LONGER_AVAILABLE);
				return;
			}
			
			filename = (npc.getHtmlPath(npcId, 5));
			html.setFile(player, filename);
			
			int count = 0;
			int found = 0;
			
			// counting buttons and unsetting button if found
			for (int i = 0; i < 5; i++)
			{
				if (player.getLoto(i) == value)
				{
					// unsetting button
					player.setLoto(i, 0);
					found = 1;
				}
				else if (player.getLoto(i) > 0)
				{
					count++;
				}
			}
			
			// if not rearched limit 5 and not unseted value
			if ((count < 5) && (found == 0) && (value <= 20))
			{
				for (int i = 0; i < 5; i++)
				{
					if (player.getLoto(i) == 0)
					{
						player.setLoto(i, value);
						break;
					}
				}
			}
			
			// setting pusshed buttons
			count = 0;
			for (int i = 0; i < 5; i++)
			{
				if (player.getLoto(i) > 0)
				{
					count++;
					String button = String.valueOf(player.getLoto(i));
					if (player.getLoto(i) < 10)
					{
						button = "0" + button;
					}
					
					final String search = "fore=\"L2UI.lottoNum" + button + "\" back=\"L2UI.lottoNum" + button + "a_check\"";
					final String replace = "fore=\"L2UI.lottoNum" + button + "a_check\" back=\"L2UI.lottoNum" + button + "\"";
					html.replace(search, replace);
				}
			}
			
			if (count == 5)
			{
				final String search = "0\">Return";
				final String replace = "22\">Your lucky numbers have been selected above.";
				html.replace(search, replace);
			}
		}
		else if (value == 22) // 22 - selected ticket with 5 numbers
		{
			if (!LotteryManager.getInstance().isStarted())
			{
				// tickets can't be sold
				player.sendPacket(SystemMessageId.LOTTERY_TICKETS_ARE_NOT_CURRENTLY_BEING_SOLD);
				return;
			}
			
			if (!LotteryManager.getInstance().isSellableTickets())
			{
				// tickets can't be sold
				player.sendPacket(SystemMessageId.TICKETS_FOR_THE_CURRENT_LOTTERY_ARE_NO_LONGER_AVAILABLE);
				return;
			}
			
			final int price = GeneralConfig.ALT_LOTTERY_TICKET_PRICE;
			final int lotonumber = LotteryManager.getInstance().getId();
			int enchant = 0;
			int type2 = 0;
			
			for (int i = 0; i < 5; i++)
			{
				if (player.getLoto(i) == 0)
				{
					return;
				}
				
				if (player.getLoto(i) < 17)
				{
					enchant += Math.pow(2, player.getLoto(i) - 1);
				}
				else
				{
					type2 += Math.pow(2, player.getLoto(i) - 17);
				}
			}
			
			if (player.getAdena() < price)
			{
				sm = new SystemMessage(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
				player.sendPacket(sm);
				return;
			}
			
			if (!player.reduceAdena(ItemProcessType.FEE, price, npc, true))
			{
				return;
			}
			
			LotteryManager.getInstance().increasePrize(price);
			
			sm = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S1);
			sm.addItemName(4442);
			player.sendPacket(sm);
			
			final Item item = new Item(IdManager.getInstance().getNextId(), 4442);
			item.setCount(1);
			item.setCustomType1(lotonumber);
			item.setEnchantLevel(enchant);
			item.setCustomType2(type2);
			player.getInventory().addItem(ItemProcessType.QUEST, item, player, npc);
			
			final InventoryUpdate iu = new InventoryUpdate();
			iu.addItem(item);
			final Item adenaupdate = player.getInventory().getItemByItemId(57);
			if (adenaupdate != null)
			{
				iu.addModifiedItem(adenaupdate);
			}
			
			player.sendInventoryUpdate(iu);
			
			filename = (npc.getHtmlPath(npcId, 6));
			html.setFile(player, filename);
		}
		else if (value == 23) // 23 - current lottery jackpot
		{
			filename = (npc.getHtmlPath(npcId, 3));
			html.setFile(player, filename);
		}
		else if (value == 24) // 24 - Previous winning numbers/Prize claim
		{
			filename = (npc.getHtmlPath(npcId, 4));
			html.setFile(player, filename);
			
			final int lotonumber = LotteryManager.getInstance().getId();
			String message = "";
			for (Item item : player.getInventory().getItems())
			{
				if (item == null)
				{
					continue;
				}
				
				if ((item.getId() == 4442) && (item.getCustomType1() < lotonumber))
				{
					message = message + "<a action=\"bypass -h npc_%objectId%_Loto " + item.getObjectId() + "\">" + item.getCustomType1() + " Event Number ";
					final int[] numbers = LotteryManager.getInstance().decodeNumbers(item.getEnchantLevel(), item.getCustomType2());
					for (int i = 0; i < 5; i++)
					{
						message += numbers[i] + " ";
					}
					
					final int[] check = LotteryManager.getInstance().checkTicket(item);
					if (check[0] > 0)
					{
						switch (check[0])
						{
							case 1:
							{
								message += "- 1st Prize";
								break;
							}
							case 2:
							{
								message += "- 2nd Prize";
								break;
							}
							case 3:
							{
								message += "- 3th Prize";
								break;
							}
							case 4:
							{
								message += "- 4th Prize";
								break;
							}
						}
						
						message += " " + check[1] + "a.";
					}
					
					message += "</a><br>";
				}
			}
			
			if (message.isEmpty())
			{
				message += "There has been no winning lottery ticket.<br>";
			}
			
			html.replace("%result%", message);
		}
		else if (value == 25) // 25 - lottery instructions
		{
			filename = (npc.getHtmlPath(npcId, 2));
			html.setFile(player, filename);
		}
		else if (value > 25) // >25 - check lottery ticket by item object id
		{
			final int lotonumber = LotteryManager.getInstance().getId();
			final Item item = player.getInventory().getItemByObjectId(value);
			if ((item == null) || (item.getId() != 4442) || (item.getCustomType1() >= lotonumber))
			{
				return;
			}
			
			final int[] check = LotteryManager.getInstance().checkTicket(item);
			
			sm = new SystemMessage(SystemMessageId.S1_HAS_DISAPPEARED);
			sm.addItemName(4442);
			player.sendPacket(sm);
			
			final int adena = check[1];
			if (adena > 0)
			{
				player.addAdena(ItemProcessType.REWARD, adena, npc, true);
			}
			
			player.destroyItem(ItemProcessType.FEE, item, npc, false);
			return;
		}
		
		html.replace("%objectId%", String.valueOf(npc.getObjectId()));
		html.replace("%race%", Integer.toString(LotteryManager.getInstance().getId()));
		html.replace("%adena%", Long.toString(LotteryManager.getInstance().getPrize()));
		html.replace("%ticket_price%", Long.toString(GeneralConfig.ALT_LOTTERY_TICKET_PRICE));
		html.replace("%prize5%", Float.toString(GeneralConfig.ALT_LOTTERY_5_NUMBER_RATE * 100));
		html.replace("%prize4%", Float.toString(GeneralConfig.ALT_LOTTERY_4_NUMBER_RATE * 100));
		html.replace("%prize3%", Float.toString(GeneralConfig.ALT_LOTTERY_3_NUMBER_RATE * 100));
		html.replace("%prize2%", Long.toString(GeneralConfig.ALT_LOTTERY_2_AND_1_NUMBER_PRIZE));
		html.replace("%enddate%", "" + DateFormat.getDateInstance().format(LotteryManager.getInstance().getEndDate()));
		player.sendPacket(html);
		
		// Send a Server->Client ActionFailed to the Player in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
}
