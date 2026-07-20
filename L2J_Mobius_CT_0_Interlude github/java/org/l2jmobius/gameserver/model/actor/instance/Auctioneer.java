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

import static org.l2jmobius.gameserver.model.itemcontainer.Inventory.MAX_ADENA;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.data.sql.ClanHallTable;
import org.l2jmobius.gameserver.data.xml.MapRegionData;
import org.l2jmobius.gameserver.managers.ClanHallAuctionManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.clan.ClanAccess;
import org.l2jmobius.gameserver.model.residences.ClanHallAuction;
import org.l2jmobius.gameserver.model.residences.ClanHallAuction.Bidder;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class Auctioneer extends Npc
{
	private static final int COND_ALL_FALSE = 0;
	private static final int COND_BUSY_BECAUSE_OF_SIEGE = 1;
	private static final int COND_REGULAR = 3;
	
	private final Map<Integer, ClanHallAuction> _pendingAuctions = new ConcurrentHashMap<>();
	
	public Auctioneer(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.Auctioneer);
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		final int condition = validateCondition();
		if (condition <= COND_ALL_FALSE)
		{
			player.sendMessage("Wrong conditions.");
			return;
		}
		else if (condition == COND_BUSY_BECAUSE_OF_SIEGE)
		{
			final String filename = "data/html/auction/auction-busy.htm";
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile(player, filename);
			html.replace("%objectId%", String.valueOf(getObjectId()));
			player.sendPacket(html);
			return;
		}
		else if (condition == COND_REGULAR)
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			final String actualCommand = st.nextToken(); // Get actual command
			
			String val = "";
			if (st.countTokens() >= 1)
			{
				val = st.nextToken();
			}
			
			if (actualCommand.equalsIgnoreCase("auction"))
			{
				if (val.isEmpty())
				{
					return;
				}
				
				try
				{
					final int days = Integer.parseInt(val);
					try
					{
						final SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
						int bid = 0;
						if (st.countTokens() >= 1)
						{
							bid = Math.min(Integer.parseInt(st.nextToken()), MAX_ADENA);
						}
						
						final ClanHallAuction a = new ClanHallAuction(player.getClan().getHideoutId(), player.getClan(), days * 86400000, bid, ClanHallTable.getInstance().getClanHallByOwner(player.getClan()).getName());
						if (_pendingAuctions.get(a.getId()) != null)
						{
							_pendingAuctions.remove(a.getId());
						}
						
						_pendingAuctions.put(a.getId(), a);
						
						final String filename = "data/html/auction/AgitSale3.htm";
						final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
						html.setFile(player, filename);
						html.replace("%x%", val);
						html.replace("%AGIT_AUCTION_END%", format.format(a.getEndDate()));
						html.replace("%AGIT_AUCTION_MINBID%", String.valueOf(a.getStartingBid()));
						html.replace("%AGIT_AUCTION_MIN%", String.valueOf(a.getStartingBid()));
						html.replace("%AGIT_AUCTION_DESC%", ClanHallTable.getInstance().getClanHallByOwner(player.getClan()).getDesc());
						html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_sale2");
						html.replace("%objectId%", String.valueOf((getObjectId())));
						player.sendPacket(html);
					}
					catch (Exception e)
					{
						player.sendMessage("Invalid bid!");
					}
				}
				catch (Exception e)
				{
					player.sendMessage("Invalid auction duration!");
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("confirmAuction"))
			{
				try
				{
					final ClanHallAuction a = _pendingAuctions.get(player.getClan().getHideoutId());
					a.confirmAuction();
					_pendingAuctions.remove(player.getClan().getHideoutId());
				}
				catch (Exception e)
				{
					player.sendMessage("Invalid auction");
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("bidding"))
			{
				if (val.isEmpty())
				{
					return;
				}
				
				try
				{
					final SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
					final int auctionId = Integer.parseInt(val);
					
					final String filename = "data/html/auction/AgitAuctionInfo.htm";
					final ClanHallAuction a = ClanHallAuctionManager.getInstance().getAuction(auctionId);
					
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(player, filename);
					if (a != null)
					{
						html.replace("%AGIT_NAME%", a.getItemName());
						html.replace("%OWNER_PLEDGE_NAME%", a.getSellerClanName());
						html.replace("%OWNER_PLEDGE_MASTER%", a.getSellerName());
						html.replace("%AGIT_SIZE%", String.valueOf(ClanHallTable.getInstance().getAuctionableHallById(a.getItemId()).getGrade() * 10));
						html.replace("%AGIT_LEASE%", String.valueOf(ClanHallTable.getInstance().getAuctionableHallById(a.getItemId()).getLease()));
						html.replace("%AGIT_LOCATION%", ClanHallTable.getInstance().getAuctionableHallById(a.getItemId()).getLocation());
						html.replace("%AGIT_AUCTION_END%", format.format(a.getEndDate()));
						html.replace("%AGIT_AUCTION_REMAIN%", ((a.getEndDate() - System.currentTimeMillis()) / 3600000) + " hours " + (((a.getEndDate() - System.currentTimeMillis()) / 60000) % 60) + " minutes");
						html.replace("%AGIT_AUCTION_MINBID%", String.valueOf(a.getStartingBid()));
						html.replace("%AGIT_AUCTION_COUNT%", String.valueOf(a.getBidders().size()));
						html.replace("%AGIT_AUCTION_DESC%", ClanHallTable.getInstance().getAuctionableHallById(a.getItemId()).getDesc());
						html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_list");
						html.replace("%AGIT_LINK_BIDLIST%", "bypass -h npc_" + getObjectId() + "_bidlist " + a.getId());
						html.replace("%AGIT_LINK_RE%", "bypass -h npc_" + getObjectId() + "_bid1 " + a.getId());
					}
					else
					{
						LOGGER.warning("Auctioneer Auction null for AuctionId : " + auctionId);
					}
					
					player.sendPacket(html);
				}
				catch (Exception e)
				{
					player.sendMessage("Invalid auction!");
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("bid"))
			{
				if (val.isEmpty())
				{
					return;
				}
				
				try
				{
					final int auctionId = Integer.parseInt(val);
					try
					{
						int bid = 0;
						if (st.countTokens() >= 1)
						{
							bid = Math.min(Integer.parseInt(st.nextToken()), MAX_ADENA);
						}
						
						ClanHallAuctionManager.getInstance().getAuction(auctionId).setBid(player, bid);
					}
					catch (Exception e)
					{
						player.sendMessage("Invalid bid!");
					}
				}
				catch (Exception e)
				{
					player.sendMessage("Invalid auction!");
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("bid1"))
			{
				if ((player.getClan() == null) || (player.getClan().getLevel() < 2))
				{
					player.sendPacket(SystemMessageId.ONLY_A_CLAN_LEADER_WHOSE_CLAN_IS_OF_LEVEL_2_OR_HIGHER_IS_ALLOWED_TO_PARTICIPATE_IN_A_CLAN_HALL_AUCTION);
					return;
				}
				
				if (val.isEmpty())
				{
					return;
				}
				
				if (((player.getClan().getAuctionBiddedAt() > 0) && (player.getClan().getAuctionBiddedAt() != Integer.parseInt(val))) || (player.getClan().getHideoutId() > 0))
				{
					player.sendPacket(SystemMessageId.SINCE_YOU_HAVE_ALREADY_SUBMITTED_A_BID_YOU_ARE_NOT_ALLOWED_TO_PARTICIPATE_IN_ANOTHER_AUCTION_AT_THIS_TIME);
					return;
				}
				
				try
				{
					final String filename = "data/html/auction/AgitBid1.htm";
					
					long minimumBid = ClanHallAuctionManager.getInstance().getAuction(Integer.parseInt(val)).getHighestBidderMaxBid();
					if (minimumBid == 0)
					{
						minimumBid = ClanHallAuctionManager.getInstance().getAuction(Integer.parseInt(val)).getStartingBid();
					}
					
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(player, filename);
					html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_bidding " + val);
					html.replace("%PLEDGE_ADENA%", String.valueOf(player.getClan().getWarehouse().getAdena()));
					html.replace("%AGIT_AUCTION_MINBID%", String.valueOf(minimumBid));
					html.replace("npc_%objectId%_bid", "npc_" + getObjectId() + "_bid " + val);
					player.sendPacket(html);
				}
				catch (Exception e)
				{
					player.sendMessage("Invalid auction!");
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("list"))
			{
				final List<ClanHallAuction> auctions = ClanHallAuctionManager.getInstance().getAuctions();
				final SimpleDateFormat format = new SimpleDateFormat("yy/MM/dd");
				/** Limit for make new page, prevent client crash **/
				int limit = 15;
				int start;
				int i = 1;
				final double npage = Math.ceil((float) auctions.size() / limit);
				
				if (val.isEmpty())
				{
					start = 1;
				}
				else
				{
					start = (limit * (Integer.parseInt(val) - 1)) + 1;
					limit *= Integer.parseInt(val);
				}
				
				final StringBuilder items = new StringBuilder();
				items.append("<table width=280 border=0><tr>");
				for (int j = 1; j <= npage; j++)
				{
					items.append("<td><center><a action=\"bypass -h npc_");
					items.append(getObjectId());
					items.append("_list ");
					items.append(j);
					items.append("\"> Page ");
					items.append(j);
					items.append(" </a></center></td>");
				}
				
				items.append("</tr></table>");
				items.append("<table width=280 border=0>");
				
				for (ClanHallAuction a : auctions)
				{
					if (a == null)
					{
						continue;
					}
					
					if (i > limit)
					{
						break;
					}
					else if (i < start)
					{
						i++;
						continue;
					}
					else
					{
						i++;
					}
					
					items.append("<tr>");
					items.append("<td>");
					items.append(ClanHallTable.getInstance().getAuctionableHallById(a.getItemId()).getLocation());
					items.append("</td>");
					items.append("<td><a action=\"bypass -h npc_");
					items.append(getObjectId());
					items.append("_bidding ");
					items.append(a.getId());
					items.append("\">");
					items.append(a.getItemName());
					items.append("</a></td>");
					items.append("<td>" + format.format(a.getEndDate()));
					items.append("</td>");
					items.append("<td>");
					items.append(a.getStartingBid());
					items.append("</td>");
					items.append("</tr>");
				}
				
				items.append("</table>");
				final String filename = "data/html/auction/AgitAuctionList.htm";
				
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player, filename);
				html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_start");
				html.replace("%itemsField%", items.toString());
				player.sendPacket(html);
				return;
			}
			else if (actualCommand.equalsIgnoreCase("bidlist"))
			{
				int auctionId = 0;
				if (val.isEmpty())
				{
					if (player.getClan().getAuctionBiddedAt() <= 0)
					{
						return;
					}
					
					auctionId = player.getClan().getAuctionBiddedAt();
				}
				else
				{
					auctionId = Integer.parseInt(val);
				}
				
				String biders = "";
				final Map<Integer, Bidder> bidders = ClanHallAuctionManager.getInstance().getAuction(auctionId).getBidders();
				for (Bidder b : bidders.values())
				{
					biders += "<tr><td>" + b.getClanName() + "</td><td>" + b.getName() + "</td><td>" + b.getTimeBid().get(Calendar.YEAR) + "/" + (b.getTimeBid().get(Calendar.MONTH) + 1) + "/" + b.getTimeBid().get(Calendar.DATE) + "</td><td>" + b.getBid() + "</td></tr>";
				}
				
				final String filename = "data/html/auction/AgitBidderList.htm";
				
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player, filename);
				html.replace("%AGIT_LIST%", biders);
				html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_selectedItems");
				html.replace("%x%", val);
				html.replace("%objectId%", String.valueOf(getObjectId()));
				player.sendPacket(html);
				return;
			}
			else if (actualCommand.equalsIgnoreCase("selectedItems"))
			{
				if ((player.getClan() != null) && (player.getClan().getHideoutId() == 0) && (player.getClan().getAuctionBiddedAt() > 0))
				{
					final SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
					final String filename = "data/html/auction/AgitBidInfo.htm";
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(player, filename);
					final ClanHallAuction a = ClanHallAuctionManager.getInstance().getAuction(player.getClan().getAuctionBiddedAt());
					if (a != null)
					{
						html.replace("%AGIT_NAME%", a.getItemName());
						html.replace("%OWNER_PLEDGE_NAME%", a.getSellerClanName());
						html.replace("%OWNER_PLEDGE_MASTER%", a.getSellerName());
						html.replace("%AGIT_SIZE%", String.valueOf(ClanHallTable.getInstance().getAuctionableHallById(a.getItemId()).getGrade() * 10));
						html.replace("%AGIT_LEASE%", String.valueOf(ClanHallTable.getInstance().getAuctionableHallById(a.getItemId()).getLease()));
						html.replace("%AGIT_LOCATION%", ClanHallTable.getInstance().getAuctionableHallById(a.getItemId()).getLocation());
						html.replace("%AGIT_AUCTION_END%", format.format(a.getEndDate()));
						html.replace("%AGIT_AUCTION_REMAIN%", ((a.getEndDate() - System.currentTimeMillis()) / 3600000) + " hours " + (((a.getEndDate() - System.currentTimeMillis()) / 60000) % 60) + " minutes");
						html.replace("%AGIT_AUCTION_MINBID%", String.valueOf(a.getStartingBid()));
						html.replace("%AGIT_AUCTION_MYBID%", String.valueOf(a.getBidders().get(player.getClanId()).getBid()));
						html.replace("%AGIT_AUCTION_DESC%", ClanHallTable.getInstance().getAuctionableHallById(a.getItemId()).getDesc());
						html.replace("%objectId%", String.valueOf(getObjectId()));
						html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_start");
					}
					else
					{
						LOGGER.warning("Auctioneer Auction null for AuctionBiddedAt : " + player.getClan().getAuctionBiddedAt());
					}
					
					player.sendPacket(html);
					return;
				}
				else if ((player.getClan() != null) && (ClanHallAuctionManager.getInstance().getAuction(player.getClan().getHideoutId()) != null))
				{
					final SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
					final String filename = "data/html/auction/AgitSaleInfo.htm";
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(player, filename);
					final ClanHallAuction a = ClanHallAuctionManager.getInstance().getAuction(player.getClan().getHideoutId());
					if (a != null)
					{
						html.replace("%AGIT_NAME%", a.getItemName());
						html.replace("%AGIT_OWNER_PLEDGE_NAME%", a.getSellerClanName());
						html.replace("%OWNER_PLEDGE_MASTER%", a.getSellerName());
						html.replace("%AGIT_SIZE%", String.valueOf(ClanHallTable.getInstance().getAuctionableHallById(a.getItemId()).getGrade() * 10));
						html.replace("%AGIT_LEASE%", String.valueOf(ClanHallTable.getInstance().getAuctionableHallById(a.getItemId()).getLease()));
						html.replace("%AGIT_LOCATION%", ClanHallTable.getInstance().getAuctionableHallById(a.getItemId()).getLocation());
						html.replace("%AGIT_AUCTION_END%", format.format(a.getEndDate()));
						html.replace("%AGIT_AUCTION_REMAIN%", ((a.getEndDate() - System.currentTimeMillis()) / 3600000) + " hours " + (((a.getEndDate() - System.currentTimeMillis()) / 60000) % 60) + " minutes");
						html.replace("%AGIT_AUCTION_MINBID%", String.valueOf(a.getStartingBid()));
						html.replace("%AGIT_AUCTION_BIDCOUNT%", String.valueOf(a.getBidders().size()));
						html.replace("%AGIT_AUCTION_DESC%", ClanHallTable.getInstance().getAuctionableHallById(a.getItemId()).getDesc());
						html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_start");
						html.replace("%id%", String.valueOf(a.getId()));
						html.replace("%objectId%", String.valueOf(getObjectId()));
					}
					else
					{
						LOGGER.warning("Auctioneer Auction null for getHideoutId : " + player.getClan().getHideoutId());
					}
					
					player.sendPacket(html);
					return;
				}
				else if ((player.getClan() != null) && (player.getClan().getHideoutId() != 0))
				{
					final int ItemId = player.getClan().getHideoutId();
					final String filename = "data/html/auction/AgitInfo.htm";
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(player, filename);
					if (ClanHallTable.getInstance().getAuctionableHallById(ItemId) != null)
					{
						html.replace("%AGIT_NAME%", ClanHallTable.getInstance().getAuctionableHallById(ItemId).getName());
						html.replace("%AGIT_OWNER_PLEDGE_NAME%", player.getClan().getName());
						html.replace("%OWNER_PLEDGE_MASTER%", player.getClan().getLeaderName());
						html.replace("%AGIT_SIZE%", String.valueOf(ClanHallTable.getInstance().getAuctionableHallById(ItemId).getGrade() * 10));
						html.replace("%AGIT_LEASE%", String.valueOf(ClanHallTable.getInstance().getAuctionableHallById(ItemId).getLease()));
						html.replace("%AGIT_LOCATION%", ClanHallTable.getInstance().getAuctionableHallById(ItemId).getLocation());
						html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_start");
						html.replace("%objectId%", String.valueOf(getObjectId()));
					}
					else
					{
						LOGGER.warning("Clan Hall ID NULL : " + ItemId + " Can be caused by concurent write in ClanHallManager");
					}
					
					player.sendPacket(html);
					return;
				}
				else if ((player.getClan() != null) && (player.getClan().getHideoutId() == 0))
				{
					player.sendPacket(SystemMessageId.THERE_ARE_NO_OFFERINGS_I_OWN_OR_I_MADE_A_BID_FOR);
					return;
				}
				else if (player.getClan() == null)
				{
					player.sendPacket(SystemMessageId.YOU_CANNOT_PARTICIPATE_IN_AN_AUCTION);
					return;
				}
			}
			else if (actualCommand.equalsIgnoreCase("cancelBid"))
			{
				final long bid = ClanHallAuctionManager.getInstance().getAuction(player.getClan().getAuctionBiddedAt()).getBidders().get(player.getClanId()).getBid();
				final String filename = "data/html/auction/AgitBidCancel.htm";
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player, filename);
				html.replace("%AGIT_BID%", String.valueOf(bid));
				html.replace("%AGIT_BID_REMAIN%", String.valueOf((long) (bid * 0.9)));
				html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_selectedItems");
				html.replace("%objectId%", String.valueOf(getObjectId()));
				player.sendPacket(html);
				return;
			}
			else if (actualCommand.equalsIgnoreCase("doCancelBid"))
			{
				if (ClanHallAuctionManager.getInstance().getAuction(player.getClan().getAuctionBiddedAt()) != null)
				{
					ClanHallAuctionManager.getInstance().getAuction(player.getClan().getAuctionBiddedAt()).cancelBid(player.getClanId());
					player.sendPacket(SystemMessageId.YOU_HAVE_CANCELED_YOUR_BID);
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("cancelAuction"))
			{
				if (!player.hasAccess(ClanAccess.HALL_AUCTION))
				{
					final String filename = "data/html/auction/not_authorized.htm";
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(player, filename);
					html.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(html);
					return;
				}
				
				final String filename = "data/html/auction/AgitSaleCancel.htm";
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player, filename);
				html.replace("%AGIT_DEPOSIT%", String.valueOf(ClanHallTable.getInstance().getClanHallByOwner(player.getClan()).getLease()));
				html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_selectedItems");
				html.replace("%objectId%", String.valueOf(getObjectId()));
				player.sendPacket(html);
				return;
			}
			else if (actualCommand.equalsIgnoreCase("doCancelAuction"))
			{
				if (ClanHallAuctionManager.getInstance().getAuction(player.getClan().getHideoutId()) != null)
				{
					ClanHallAuctionManager.getInstance().getAuction(player.getClan().getHideoutId()).cancelAuction();
					player.sendMessage("Your auction has been canceled");
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("sale2"))
			{
				final String filename = "data/html/auction/AgitSale2.htm";
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player, filename);
				html.replace("%AGIT_LAST_PRICE%", String.valueOf(ClanHallTable.getInstance().getClanHallByOwner(player.getClan()).getLease()));
				html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_sale");
				html.replace("%objectId%", String.valueOf(getObjectId()));
				player.sendPacket(html);
				return;
			}
			else if (actualCommand.equalsIgnoreCase("sale"))
			{
				if (!player.hasAccess(ClanAccess.HALL_AUCTION))
				{
					final String filename = "data/html/auction/not_authorized.htm";
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(player, filename);
					html.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(html);
					return;
				}
				
				final String filename = "data/html/auction/AgitSale1.htm";
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player, filename);
				html.replace("%AGIT_DEPOSIT%", String.valueOf(ClanHallTable.getInstance().getClanHallByOwner(player.getClan()).getLease()));
				html.replace("%AGIT_PLEDGE_ADENA%", String.valueOf(player.getClan().getWarehouse().getAdena()));
				html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_selectedItems");
				html.replace("%objectId%", String.valueOf(getObjectId()));
				player.sendPacket(html);
				return;
			}
			else if (actualCommand.equalsIgnoreCase("rebid"))
			{
				final SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
				if (!player.hasAccess(ClanAccess.HALL_AUCTION))
				{
					final String filename = "data/html/auction/not_authorized.htm";
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(player, filename);
					html.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(html);
					return;
				}
				
				try
				{
					final String filename = "data/html/auction/AgitBid2.htm";
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(player, filename);
					final ClanHallAuction a = ClanHallAuctionManager.getInstance().getAuction(player.getClan().getAuctionBiddedAt());
					if (a != null)
					{
						html.replace("%AGIT_AUCTION_BID%", String.valueOf(a.getBidders().get(player.getClanId()).getBid()));
						html.replace("%AGIT_AUCTION_MINBID%", String.valueOf(a.getStartingBid()));
						html.replace("%AGIT_AUCTION_END%", format.format(a.getEndDate()));
						html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_selectedItems");
						html.replace("npc_%objectId%_bid1", "npc_" + getObjectId() + "_bid1 " + a.getId());
					}
					else
					{
						LOGGER.warning("Auctioneer Auction null for AuctionBiddedAt : " + player.getClan().getAuctionBiddedAt());
					}
					
					player.sendPacket(html);
				}
				catch (Exception e)
				{
					player.sendMessage("Invalid auction!");
				}
				return;
			}
			else if (actualCommand.equalsIgnoreCase("location"))
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player, "data/html/auction/location.htm");
				html.replace("%location%", MapRegionData.getInstance().getClosestTownName(player));
				html.replace("%LOCATION%", getPictureName(player));
				html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_start");
				player.sendPacket(html);
				return;
			}
			else if (actualCommand.equalsIgnoreCase("start"))
			{
				showChatWindow(player);
				return;
			}
		}
		
		super.onBypassFeedback(player, command);
	}
	
	@Override
	public void showChatWindow(Player player)
	{
		String filename; // = "data/html/auction/auction-no.htm";
		
		final int condition = validateCondition();
		if (condition == COND_BUSY_BECAUSE_OF_SIEGE)
		{
			filename = "data/html/auction/auction-busy.htm"; // Busy because of siege
		}
		else
		{
			filename = "data/html/auction/auction.htm";
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player, filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcId%", String.valueOf(getId()));
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}
	
	private int validateCondition()
	{
		if ((getCastle() != null) && (getCastle().getResidenceId() > 0))
		{
			if (getCastle().getSiege().isInProgress())
			{
				return COND_BUSY_BECAUSE_OF_SIEGE; // Busy because of siege
			}
			
			return COND_REGULAR;
		}
		
		return COND_ALL_FALSE;
	}
	
	private String getPictureName(Player plyr)
	{
		final int nearestTownId = MapRegionData.getInstance().getMapRegionLocId(plyr);
		String nearestTown;
		
		switch (nearestTownId)
		{
			case 911:
			{
				nearestTown = "GLUDIN";
				break;
			}
			case 912:
			{
				nearestTown = "GLUDIO";
				break;
			}
			case 916:
			{
				nearestTown = "DION";
				break;
			}
			case 918:
			{
				nearestTown = "GIRAN";
				break;
			}
			case 1537:
			{
				nearestTown = "RUNE";
				break;
			}
			case 1538:
			{
				nearestTown = "GODARD";
				break;
			}
			case 1714:
			{
				nearestTown = "SCHUTTGART";
				break;
			}
			default:
			{
				nearestTown = "ADEN";
				break;
			}
		}
		
		return nearestTown;
	}
}
