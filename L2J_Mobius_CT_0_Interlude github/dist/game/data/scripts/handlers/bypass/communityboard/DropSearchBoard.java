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
package handlers.bypass.communityboard;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.config.custom.PremiumSystemConfig;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.handler.IParseBoardHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.npc.DropType;
import org.l2jmobius.gameserver.model.actor.holders.npc.DropGroupHolder;
import org.l2jmobius.gameserver.model.actor.holders.npc.DropHolder;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.spawns.Spawn;
import org.l2jmobius.gameserver.network.serverpackets.RadarControl;
import org.l2jmobius.gameserver.network.serverpackets.ShowMiniMap;

/**
 * @author yksdtc
 */
public class DropSearchBoard implements IParseBoardHandler
{
	private static final String NAVIGATION_PATH = "data/html/CommunityBoard/Custom/navigation.html";
	private static final String[] COMMAND =
	{
		"_bbs_search_item",
		"_bbs_search_drop",
		"_bbs_npc_trace"
	};
	
	private class CBDropHolder
	{
		final int itemId;
		final int npcId;
		final byte npcLevel;
		final long min;
		final long max;
		final double chance;
		final boolean isSpoil;
		final boolean isRaid;
		
		public CBDropHolder(NpcTemplate npcTemplate, DropHolder dropHolder)
		{
			isSpoil = dropHolder.getDropType() == DropType.SPOIL;
			itemId = dropHolder.getItemId();
			npcId = npcTemplate.getId();
			npcLevel = npcTemplate.getLevel();
			min = dropHolder.getMin();
			max = dropHolder.getMax();
			chance = dropHolder.getChance();
			isRaid = npcTemplate.getType().equals("RaidBoss") || npcTemplate.getType().equals("GrandBoss");
		}
		
		/**
		 * only for debug
		 */
		@Override
		public String toString()
		{
			return "DropHolder [itemId=" + itemId + ", npcId=" + npcId + ", npcLevel=" + npcLevel + ", min=" + min + ", max=" + max + ", chance=" + chance + ", isSpoil=" + isSpoil + "]";
		}
	}
	
	private final Map<Integer, List<CBDropHolder>> DROP_INDEX_CACHE = new HashMap<>();
	
	// nonsupport items
	private static final Set<Integer> BLOCK_ID = new HashSet<>();
	static
	{
		BLOCK_ID.add(Inventory.ADENA_ID);
	}
	
	public DropSearchBoard()
	{
		buildDropIndex();
	}
	
	private void buildDropIndex()
	{
		NpcData.getInstance().getTemplates(npc -> npc.getDropGroups() != null).forEach(npcTemplate ->
		{
			for (DropGroupHolder dropGroup : npcTemplate.getDropGroups())
			{
				final double chance = dropGroup.getChance() / 100;
				for (DropHolder dropHolder : dropGroup.getDropList())
				{
					addToDropList(npcTemplate, new DropHolder(dropHolder.getDropType(), dropHolder.getItemId(), dropHolder.getMin(), dropHolder.getMax(), dropHolder.getChance() * chance));
				}
			}
		});
		NpcData.getInstance().getTemplates(npc -> npc.getDropList() != null).forEach(npcTemplate ->
		{
			for (DropHolder dropHolder : npcTemplate.getDropList())
			{
				addToDropList(npcTemplate, dropHolder);
			}
		});
		NpcData.getInstance().getTemplates(npc -> npc.getSpoilList() != null).forEach(npcTemplate ->
		{
			for (DropHolder dropHolder : npcTemplate.getSpoilList())
			{
				addToDropList(npcTemplate, dropHolder);
			}
		});
		
		DROP_INDEX_CACHE.values().forEach(l -> l.sort((d1, d2) -> Byte.valueOf(d1.npcLevel).compareTo(Byte.valueOf(d2.npcLevel))));
	}
	
	private void addToDropList(NpcTemplate npcTemplate, DropHolder dropHolder)
	{
		if (BLOCK_ID.contains(dropHolder.getItemId()))
		{
			return;
		}
		
		List<CBDropHolder> dropList = DROP_INDEX_CACHE.get(dropHolder.getItemId());
		if (dropList == null)
		{
			dropList = new ArrayList<>();
			DROP_INDEX_CACHE.put(dropHolder.getItemId(), dropList);
		}
		
		dropList.add(new CBDropHolder(npcTemplate, dropHolder));
	}
	
	@Override
	public boolean onCommand(String command, Player player)
	{
		final String navigation = HtmCache.getInstance().getHtm(player, NAVIGATION_PATH);
		final String[] params = command.split(" ");
		String html = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/Custom/dropsearch/main.html");
		switch (params[0])
		{
			case "_bbs_search_item":
			{
				final String itemName = buildItemName(params);
				final String result = buildItemSearchResult(itemName);
				html = html.replace("%searchResult%", result);
				break;
			}
			case "_bbs_search_drop":
			{
				final DecimalFormat chanceFormat = new DecimalFormat("0.00##");
				final int itemId = Integer.parseInt(params[1]);
				int page = Integer.parseInt(params[2]);
				final List<CBDropHolder> list = DROP_INDEX_CACHE.get(itemId);
				int pages = list.size() / 4;
				if (pages == 0)
				{
					pages++;
				}
				
				final int start = (page - 1) * 4;
				final int end = Math.min(list.size() - 1, start + 4);
				final StringBuilder builder = new StringBuilder();
				final double dropAmountAdenaEffectBonus = player.getStat().getBonusDropAdenaMultiplier();
				final double dropAmountEffectBonus = player.getStat().getBonusDropAmountMultiplier();
				final double dropRateEffectBonus = player.getStat().getBonusDropRateMultiplier();
				final double spoilRateEffectBonus = player.getStat().getBonusSpoilRateMultiplier();
				builder.append("<tr>");
				builder.append("<td width=30>Lvl</td>");
				builder.append("<td width=180>NPC Name</td>");
				builder.append("<td width=100 align=CENTER>Amount</td>");
				builder.append("<td width=80 align=CENTER>Chance</td>");
				builder.append("<td width=80 align=CENTER>Type</td>");
				builder.append("</tr>");
				
				html = html.replace("%header%", builder.toString());
				builder.setLength(0);
				
				for (int index = start; index <= end; index++)
				{
					final CBDropHolder cbDropHolder = list.get(index);
					
					// real time server rate calculations
					double rateChance = 1;
					double rateAmount = 1;
					if (cbDropHolder.isSpoil)
					{
						rateChance = RatesConfig.RATE_SPOIL_DROP_CHANCE_MULTIPLIER;
						rateAmount = RatesConfig.RATE_SPOIL_DROP_AMOUNT_MULTIPLIER;
						
						// also check premium rates if available
						if (PremiumSystemConfig.PREMIUM_SYSTEM_ENABLED && player.hasPremiumStatus())
						{
							rateChance *= PremiumSystemConfig.PREMIUM_RATE_SPOIL_CHANCE;
							rateAmount *= PremiumSystemConfig.PREMIUM_RATE_SPOIL_AMOUNT;
						}
						
						// bonus spoil rate effect
						rateChance *= spoilRateEffectBonus;
					}
					else
					{
						final ItemTemplate item = ItemData.getInstance().getTemplate(cbDropHolder.itemId);
						if (RatesConfig.RATE_DROP_CHANCE_BY_ID.get(cbDropHolder.itemId) != null)
						{
							rateChance *= RatesConfig.RATE_DROP_CHANCE_BY_ID.get(cbDropHolder.itemId);
							if ((cbDropHolder.itemId == Inventory.ADENA_ID) && (rateChance > 100))
							{
								rateChance = 100;
							}
						}
						else if (item.hasExImmediateEffect())
						{
							rateChance *= RatesConfig.RATE_HERB_DROP_CHANCE_MULTIPLIER;
						}
						else if (cbDropHolder.isRaid)
						{
							rateChance *= RatesConfig.RATE_RAID_DROP_CHANCE_MULTIPLIER;
						}
						else
						{
							rateChance *= RatesConfig.RATE_DEATH_DROP_CHANCE_MULTIPLIER;
						}
						
						if (RatesConfig.RATE_DROP_AMOUNT_BY_ID.get(cbDropHolder.itemId) != null)
						{
							rateAmount *= RatesConfig.RATE_DROP_AMOUNT_BY_ID.get(cbDropHolder.itemId);
						}
						else if (item.hasExImmediateEffect())
						{
							rateAmount *= RatesConfig.RATE_HERB_DROP_AMOUNT_MULTIPLIER;
						}
						else if (cbDropHolder.isRaid)
						{
							rateAmount *= RatesConfig.RATE_RAID_DROP_AMOUNT_MULTIPLIER;
						}
						else
						{
							rateAmount *= RatesConfig.RATE_DEATH_DROP_AMOUNT_MULTIPLIER;
						}
						
						// also check premium rates if available
						if (PremiumSystemConfig.PREMIUM_SYSTEM_ENABLED && player.hasPremiumStatus())
						{
							if (PremiumSystemConfig.PREMIUM_RATE_DROP_CHANCE_BY_ID.get(cbDropHolder.itemId) != null)
							{
								rateChance *= PremiumSystemConfig.PREMIUM_RATE_DROP_CHANCE_BY_ID.get(cbDropHolder.itemId);
							}
							else if (item.hasExImmediateEffect())
							{
								// TODO: Premium herb chance? :)
							}
							else if (cbDropHolder.isRaid)
							{
								// TODO: Premium raid chance? :)
							}
							else
							{
								rateChance *= PremiumSystemConfig.PREMIUM_RATE_DROP_CHANCE;
							}
							
							if (PremiumSystemConfig.PREMIUM_RATE_DROP_AMOUNT_BY_ID.get(cbDropHolder.itemId) != null)
							{
								rateAmount *= PremiumSystemConfig.PREMIUM_RATE_DROP_AMOUNT_BY_ID.get(cbDropHolder.itemId);
							}
							else if (item.hasExImmediateEffect())
							{
								// TODO: Premium herb amount? :)
							}
							else if (cbDropHolder.isRaid)
							{
								// TODO: Premium raid amount? :)
							}
							else
							{
								rateAmount *= PremiumSystemConfig.PREMIUM_RATE_DROP_AMOUNT;
							}
						}
						
						// bonus drop amount effect
						rateAmount *= dropAmountEffectBonus;
						if (item.getId() == Inventory.ADENA_ID)
						{
							rateAmount *= dropAmountAdenaEffectBonus;
						}
						
						// bonus drop rate effect
						rateChance *= dropRateEffectBonus;
					}
					
					builder.append("<tr>");
					builder.append("<td width=30>").append(cbDropHolder.npcLevel).append("</td>");
					builder.append("<td width=180>").append("<a action=\"bypass _bbs_npc_trace " + cbDropHolder.npcId + "\">").append("&@").append(cbDropHolder.npcId).append(";").append("</a>").append("</td>");
					builder.append("<td width=100 align=CENTER>").append(cbDropHolder.min * rateAmount).append("-").append(cbDropHolder.max * rateAmount).append("</td>");
					builder.append("<td width=80 align=CENTER>").append(chanceFormat.format(cbDropHolder.chance * rateChance)).append("%").append("</td>");
					builder.append("<td width=80 align=CENTER>").append(cbDropHolder.isSpoil ? "Spoil" : "Drop").append("</td>");
					builder.append("</tr>");
				}
				
				html = html.replace("%searchResult%", builder.toString());
				builder.setLength(0);
				
				builder.append("<tr>");
				int maxDisplayPages = 9;
				if (page > 1)
				{
					builder.append("<td><button action=\"bypass _bbs_search_drop ").append(itemId).append(" ").append(page - 1).append(" $order $level\" back=\"l2ui_ch3.prev1_down\" fore=\"l2ui_ch3.prev1\" width=16 height=16 ></td>");
				}
				else
				{
					builder.append("<td><button back=\"l2ui_ch3.prev1_down\" fore=\"l2ui_ch3.prev1\" width=16 height=16 ></td>");
				}
				
				int startPage = Math.max(1, page - (maxDisplayPages / 2));
				int endPage = Math.min(pages, (startPage + maxDisplayPages) - 1);
				if ((endPage - startPage) < (maxDisplayPages - 1))
				{
					startPage = Math.max(1, (endPage - maxDisplayPages) + 1);
				}
				
				for (int i = startPage; i <= endPage; i++)
				{
					builder.append("<td>");
					if (i == page)
					{
						builder.append("<font>").append(i).append("</font>");
					}
					else
					{
						builder.append("<a action=\"bypass _bbs_search_drop ").append(itemId).append(" ").append(i).append(" $order $level\">").append(i).append("</a>");
					}
					builder.append("</td>");
				}
				
				if (page < pages)
				{
					builder.append("<td><button action=\"bypass _bbs_search_drop ").append(itemId).append(" ").append(page + 1).append(" $order $level\" back=\"l2ui_ch3.next1_down\" fore=\"l2ui_ch3.next1\" width=16 height=16 ></td>");
				}
				else
				{
					builder.append("<td><button back=\"l2ui_ch3.next1_down\" fore=\"l2ui_ch3.next1\" width=16 height=16 ></td>");
				}
				
				builder.append("</tr>");
				html = html.replace("%pages%", builder.toString());
				break;
			}
			case "_bbs_npc_trace":
			{
				final StringBuilder builder = new StringBuilder();
				final int npcId = Integer.parseInt(params[1]);
				final Spawn spawn = SpawnTable.getInstance().getAnySpawn(npcId);
				if (spawn == null)
				{
					builder.append("<tr><td width=100 align=CENTER>Cannot find any spawn. Maybe dropped by a boss or instance monster.</td></tr>");
					html = html.replace("%searchResult%", builder.toString());
				}
				else
				{
					player.sendPacket(new ShowMiniMap(-1));
					ThreadPool.schedule(() ->
					{
						player.getRadar().addMarker(spawn.getX(), spawn.getY(), spawn.getZ());
						player.sendPacket(new RadarControl(0, 2, spawn.getX(), spawn.getY(), spawn.getZ()));
					}, 500);
				}
				break;
			}
		}
		
		if (html != null)
		{
			html = html.replace("%navigation%", navigation);
			CommunityBoardHandler.separateAndSend(html, player);
		}
		
		return false;
	}
	
	/**
	 * @param itemName
	 * @return
	 */
	private String buildItemSearchResult(String itemName)
	{
		int limit = 0;
		final Set<Integer> existInDropData = DROP_INDEX_CACHE.keySet();
		final List<ItemTemplate> items = new ArrayList<>();
		for (ItemTemplate item : ItemData.getInstance().getAllItems())
		{
			if (item == null)
			{
				continue;
			}
			
			if (!existInDropData.contains(item.getId()))
			{
				continue;
			}
			
			if (item.getName().toLowerCase().contains(itemName.toLowerCase()))
			{
				items.add(item);
				limit++;
			}
			
			if (limit == 16)
			{
				break;
			}
		}
		
		if (items.isEmpty())
		{
			return "<tr><td width=100 align=CENTER>No Match</td></tr>";
		}
		
		int line = 0;
		
		final StringBuilder builder = new StringBuilder(items.size() * 28);
		int i = 0;
		for (ItemTemplate item : items)
		{
			i++;
			if (i == 1)
			{
				line++;
				builder.append("<tr>");
			}
			
			String icon = item.getIcon();
			if (icon == null)
			{
				icon = "icon.etc_question_mark_i00";
			}
			
			builder.append("<td>");
			builder.append("<button value=\".\" action=\"bypass _bbs_search_drop " + item.getId() + " 1 $order $level\" width=32 height=32 back=\"" + icon + "\" fore=\"" + icon + "\">");
			builder.append("</td>");
			builder.append("<td width=200>");
			builder.append("&#").append(item.getId()).append(";");
			builder.append("</td>");
			
			if (i == 2)
			{
				builder.append("</tr>");
				i = 0;
			}
		}
		
		if ((i % 2) == 1)
		{
			builder.append("</tr>");
		}
		
		if (line < 6)
		{
			for (i = 0; i < (6 - line); i++)
			{
				builder.append("<tr><td height=36></td></tr>");
			}
		}
		
		return builder.toString();
	}
	
	/**
	 * @param params
	 * @return
	 */
	private String buildItemName(String[] params)
	{
		final StringJoiner joiner = new StringJoiner(" ");
		for (int i = 1; i < params.length; i++)
		{
			joiner.add(params[i]);
		}
		
		return joiner.toString();
	}
	
	@Override
	public String[] getCommandList()
	{
		return COMMAND;
	}
}
