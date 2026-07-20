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

import java.util.List;
import java.util.Locale;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.managers.IdManager;
import org.l2jmobius.gameserver.managers.games.MonsterRaceManager;
import org.l2jmobius.gameserver.managers.games.MonsterRaceManager.HistoryInfo;
import org.l2jmobius.gameserver.managers.games.MonsterRaceManager.RaceState;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class RaceManager extends Npc
{
	protected static final int[] TICKET_PRICES =
	{
		100,
		500,
		1000,
		5000,
		10000,
		20000,
		50000,
		100000
	};
	
	public RaceManager(NpcTemplate template)
	{
		super(template);
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (command.startsWith("BuyTicket"))
		{
			if (!GeneralConfig.ALLOW_RACE || (MonsterRaceManager.getInstance().getCurrentRaceState() != RaceState.ACCEPTING_BETS))
			{
				player.sendPacket(SystemMessageId.MONSTER_RACE_TICKETS_ARE_NO_LONGER_AVAILABLE);
				super.onBypassFeedback(player, "Chat 0");
				return;
			}
			
			int val = Integer.parseInt(command.substring(10));
			if (val == 0)
			{
				player.setRaceTicket(0, 0);
				player.setRaceTicket(1, 0);
			}
			
			if (((val == 10) && (player.getRaceTicket(0) == 0)) || ((val == 20) && (player.getRaceTicket(0) == 0) && (player.getRaceTicket(1) == 0)))
			{
				val = 0;
			}
			
			String search;
			String replace;
			
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			if (val < 10)
			{
				html.setFile(player, getHtmlPath(getId(), 2));
				for (int i = 0; i < 8; i++)
				{
					int n = i + 1;
					search = "Mob" + n;
					html.replace(search, MonsterRaceManager.getInstance().getMonsters()[i].getTemplate().getName());
				}
				
				search = "No1";
				if (val == 0)
				{
					html.replace(search, "");
				}
				else
				{
					html.replace(search, val);
					player.setRaceTicket(0, val);
				}
			}
			else if (val < 20)
			{
				if (player.getRaceTicket(0) == 0)
				{
					return;
				}
				
				html.setFile(player, getHtmlPath(getId(), 3));
				html.replace("0place", player.getRaceTicket(0));
				search = "Mob1";
				replace = MonsterRaceManager.getInstance().getMonsters()[player.getRaceTicket(0) - 1].getTemplate().getName();
				html.replace(search, replace);
				search = "0adena";
				if (val == 10)
				{
					html.replace(search, "");
				}
				else
				{
					html.replace(search, TICKET_PRICES[val - 11]);
					player.setRaceTicket(1, val - 10);
				}
			}
			else if (val == 20)
			{
				if ((player.getRaceTicket(0) == 0) || (player.getRaceTicket(1) == 0))
				{
					return;
				}
				
				html.setFile(player, getHtmlPath(getId(), 4));
				html.replace("0place", player.getRaceTicket(0));
				search = "Mob1";
				replace = MonsterRaceManager.getInstance().getMonsters()[player.getRaceTicket(0) - 1].getTemplate().getName();
				html.replace(search, replace);
				search = "0adena";
				int price = TICKET_PRICES[player.getRaceTicket(1) - 1];
				html.replace(search, price);
				search = "0tax";
				int tax = 0;
				html.replace(search, tax);
				search = "0total";
				int total = price + tax;
				html.replace(search, total);
			}
			else
			{
				if ((player.getRaceTicket(0) == 0) || (player.getRaceTicket(1) == 0))
				{
					return;
				}
				
				int ticket = player.getRaceTicket(0);
				int priceId = player.getRaceTicket(1);
				if (!player.reduceAdena(ItemProcessType.FEE, TICKET_PRICES[priceId - 1], this, true))
				{
					return;
				}
				
				player.setRaceTicket(0, 0);
				player.setRaceTicket(1, 0);
				Item item = new Item(IdManager.getInstance().getNextId(), 4443);
				item.setCount(1);
				item.setEnchantLevel(MonsterRaceManager.getInstance().getRaceNumber());
				item.setCustomType1(ticket);
				item.setCustomType2(TICKET_PRICES[priceId - 1] / 100);
				player.addItem(ItemProcessType.QUEST, item, player, false);
				final SystemMessage msg = new SystemMessage(SystemMessageId.ACQUIRED_S1_S2);
				msg.addInt(MonsterRaceManager.getInstance().getRaceNumber());
				msg.addItemName(4443);
				player.sendPacket(msg);
				
				// Refresh lane bet.
				MonsterRaceManager.getInstance().setBetOnLane(ticket, TICKET_PRICES[priceId - 1], true);
				super.onBypassFeedback(player, "Chat 0");
				return;
			}
			
			html.replace("1race", MonsterRaceManager.getInstance().getRaceNumber());
			html.replace("%objectId%", getObjectId());
			player.sendPacket(html);
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
		else if (command.equals("ShowOdds"))
		{
			if (!GeneralConfig.ALLOW_RACE || (MonsterRaceManager.getInstance().getCurrentRaceState() == RaceState.ACCEPTING_BETS))
			{
				player.sendPacket(SystemMessageId.MONSTER_RACE_PAYOUT_INFORMATION_IS_NOT_AVAILABLE_WHILE_TICKETS_ARE_BEING_SOLD);
				super.onBypassFeedback(player, "Chat 0");
				return;
			}
			
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile(player, getHtmlPath(getId(), 5));
			for (int i = 0; i < 8; i++)
			{
				final int n = i + 1;
				html.replace("Mob" + n, MonsterRaceManager.getInstance().getMonsters()[i].getTemplate().getName());
				
				// Odd
				final double odd = MonsterRaceManager.getInstance().getOdds().get(i);
				html.replace("Odd" + n, (odd > 0D) ? String.format(Locale.ENGLISH, "%.1f", odd) : "&$804;");
			}
			
			html.replace("1race", MonsterRaceManager.getInstance().getRaceNumber());
			html.replace("%objectId%", getObjectId());
			player.sendPacket(html);
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
		else if (command.equals("ShowInfo"))
		{
			if (!GeneralConfig.ALLOW_RACE)
			{
				return;
			}
			
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile(player, getHtmlPath(getId(), 6));
			for (int i = 0; i < 8; i++)
			{
				int n = i + 1;
				String search = "Mob" + n;
				html.replace(search, MonsterRaceManager.getInstance().getMonsters()[i].getTemplate().getName());
			}
			
			html.replace("%objectId%", getObjectId());
			player.sendPacket(html);
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
		else if (command.equals("ShowTickets"))
		{
			if (!GeneralConfig.ALLOW_RACE)
			{
				super.onBypassFeedback(player, "Chat 0");
				return;
			}
			
			// Generate data.
			final StringBuilder sb = new StringBuilder();
			
			// Retrieve player's tickets.
			for (Item ticket : player.getInventory().getAllItemsByItemId(4443))
			{
				// Don't list current race tickets.
				if (ticket.getEnchantLevel() == MonsterRaceManager.getInstance().getRaceNumber())
				{
					continue;
				}
				
				StringUtil.append(sb, "<tr><td><a action=\"bypass -h npc_%objectId%_ShowTicket ", "" + ticket.getObjectId(), "\">", "" + ticket.getEnchantLevel(), " Race Number</a></td><td align=right><font color=\"LEVEL\">", "" + ticket.getCustomType1(), "</font> Number</td><td align=right><font color=\"LEVEL\">", "" + (ticket.getCustomType2() * 100), "</font> Adena</td></tr>");
			}
			
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile(player, getHtmlPath(getId(), 7));
			html.replace("%tickets%", sb.toString());
			html.replace("%objectId%", getObjectId());
			player.sendPacket(html);
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
		else if (command.startsWith("ShowTicket"))
		{
			// Retrieve ticket objectId.
			final int val = Integer.parseInt(command.substring(11));
			if (!GeneralConfig.ALLOW_RACE || (val == 0))
			{
				super.onBypassFeedback(player, "Chat 0");
				return;
			}
			
			// Retrieve ticket on player's inventory.
			final Item ticket = player.getInventory().getItemByObjectId(val);
			if (ticket == null)
			{
				super.onBypassFeedback(player, "Chat 0");
				return;
			}
			
			final int raceId = ticket.getEnchantLevel();
			final int lane = ticket.getCustomType1();
			final int bet = ticket.getCustomType2() * 100;
			
			// Retrieve HistoryInfo for that race.
			final HistoryInfo info = MonsterRaceManager.getInstance().getHistory().get(raceId - 1);
			if (info == null)
			{
				super.onBypassFeedback(player, "Chat 0");
				return;
			}
			
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile(player, getHtmlPath(getId(), 8));
			html.replace("%raceId%", raceId);
			html.replace("%lane%", lane);
			html.replace("%bet%", bet);
			html.replace("%firstLane%", info.getFirst() + 1);
			html.replace("%odd%", (lane == (info.getFirst() + 1)) ? String.format(Locale.ENGLISH, "%.2f", info.getOddRate()) : "0.01");
			html.replace("%objectId%", getObjectId());
			html.replace("%ticketObjectId%", val);
			player.sendPacket(html);
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
		else if (command.startsWith("CalculateWin"))
		{
			// Retrieve ticket objectId.
			final int val = Integer.parseInt(command.substring(13));
			if (!GeneralConfig.ALLOW_RACE || (val == 0))
			{
				super.onBypassFeedback(player, "Chat 0");
				return;
			}
			
			// Delete ticket on player's inventory.
			final Item ticket = player.getInventory().getItemByObjectId(val);
			if (ticket == null)
			{
				super.onBypassFeedback(player, "Chat 0");
				return;
			}
			
			final int raceId = ticket.getEnchantLevel();
			final int lane = ticket.getCustomType1();
			final int bet = ticket.getCustomType2() * 100;
			
			// Retrieve HistoryInfo for that race.
			final HistoryInfo info = MonsterRaceManager.getInstance().getHistory().get(raceId - 1);
			if (info == null)
			{
				super.onBypassFeedback(player, "Chat 0");
				return;
			}
			
			// Destroy the ticket.
			if (player.destroyItem(ItemProcessType.FEE, ticket, this, true))
			{
				player.addAdena(ItemProcessType.REWARD, (int) (bet * ((lane == (info.getFirst() + 1)) ? info.getOddRate() : 0.01)), this, true);
			}
			
			super.onBypassFeedback(player, "Chat 0");
			return;
		}
		else if (command.equals("ViewHistory"))
		{
			if (!GeneralConfig.ALLOW_RACE)
			{
				super.onBypassFeedback(player, "Chat 0");
				return;
			}
			
			// Generate data.
			final StringBuilder sb = new StringBuilder();
			
			// Use whole history, pickup from 'last element' and stop at 'latest element - 7'.
			final List<HistoryInfo> history = MonsterRaceManager.getInstance().getHistory();
			for (int i = history.size() - 1; i >= Math.max(0, history.size() - 7); i--)
			{
				final HistoryInfo info = history.get(i);
				StringUtil.append(sb, "<tr><td><font color=\"LEVEL\">", "" + info.getRaceId(), "</font> th</td><td><font color=\"LEVEL\">", "" + (info.getFirst() + 1), "</font> Lane </td><td><font color=\"LEVEL\">", "" + (info.getSecond() + 1), "</font> Lane</td><td align=right><font color=00ffff>", String.format(Locale.ENGLISH, "%.2f", info.getOddRate()), "</font> Times</td></tr>");
			}
			
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile(player, getHtmlPath(getId(), 9));
			html.replace("%infos%", sb.toString());
			html.replace("%objectId%", getObjectId());
			player.sendPacket(html);
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
}
