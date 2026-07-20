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
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.model.buylist.BuyListHolder;
import org.l2jmobius.gameserver.model.buylist.Product;
import org.l2jmobius.gameserver.model.item.ItemTemplate;

/**
 * Loads buy lists for NPCs.
 * @author NosBit
 */
public class BuyListData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(BuyListData.class.getName());
	
	private final Map<Integer, BuyListHolder> _buyLists = new ConcurrentHashMap<>();
	
	protected BuyListData()
	{
		load();
	}
	
	@Override
	public synchronized void load()
	{
		_buyLists.clear();
		parseDatapackDirectory("data/buylists", false);
		if (GeneralConfig.CUSTOM_BUYLIST_LOAD)
		{
			parseDatapackDirectory("data/buylists/custom", false);
		}
		
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _buyLists.size() + " buyLists.");
		
		try (Connection con = DatabaseFactory.getConnection();
			Statement statement = con.createStatement();
			ResultSet rs = statement.executeQuery("SELECT * FROM `buylists`"))
		{
			while (rs.next())
			{
				final int buyListId = rs.getInt("buylist_id");
				final int itemId = rs.getInt("item_id");
				final int count = rs.getInt("count");
				final long nextRestockTime = rs.getLong("next_restock_time");
				final BuyListHolder buyList = getBuyList(buyListId);
				if (buyList == null)
				{
					LOGGER.warning("BuyList found in database but not loaded from xml! BuyListId: " + buyListId);
					continue;
				}
				
				final Product product = buyList.getProductByItemId(itemId);
				if (product == null)
				{
					LOGGER.warning("ItemId found in database but not loaded from xml! BuyListId: " + buyListId + " ItemId: " + itemId);
					continue;
				}
				
				if (count < product.getMaxCount())
				{
					product.setCount(count);
					product.restartRestockTask(nextRestockTime);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Failed to load buyList data from database.", e);
		}
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		try
		{
			final int buyListId = Integer.parseInt(file.getName().replaceAll(".xml", ""));
			for (Node node = document.getFirstChild(); node != null; node = node.getNextSibling())
			{
				if ("list".equalsIgnoreCase(node.getNodeName()))
				{
					final BuyListHolder buyList = new BuyListHolder(buyListId);
					for (Node list_node = node.getFirstChild(); list_node != null; list_node = list_node.getNextSibling())
					{
						if ("item".equalsIgnoreCase(list_node.getNodeName()))
						{
							int itemId = -1;
							int price = -1;
							long restockDelay = -1;
							int count = -1;
							final NamedNodeMap attrs = list_node.getAttributes();
							Node attr = attrs.getNamedItem("id");
							itemId = Integer.parseInt(attr.getNodeValue());
							attr = attrs.getNamedItem("price");
							if (attr != null)
							{
								price = Integer.parseInt(attr.getNodeValue());
							}
							
							attr = attrs.getNamedItem("restock_delay");
							if (attr != null)
							{
								restockDelay = Long.parseLong(attr.getNodeValue());
							}
							
							attr = attrs.getNamedItem("count");
							if (attr != null)
							{
								count = Integer.parseInt(attr.getNodeValue());
							}
							
							final ItemTemplate item = ItemData.getInstance().getTemplate(itemId);
							if (item != null)
							{
								final int sellPrice = item.getReferencePrice() / 2;
								if (GeneralConfig.CORRECT_PRICES && (price > -1) && (sellPrice > price) && (buyList.getNpcsAllowed() != null))
								{
									LOGGER.warning("Buy price " + price + " is less than sell price " + sellPrice + " for ItemID:" + itemId + " of buylist " + buyList.getListId() + ".");
									buyList.addProduct(new Product(buyList.getListId(), item, sellPrice, restockDelay, count));
								}
								else
								{
									buyList.addProduct(new Product(buyList.getListId(), item, price, restockDelay, count));
								}
							}
							else
							{
								LOGGER.warning("Item not found. BuyList:" + buyList.getListId() + " ItemID:" + itemId + " File:" + file.getName());
							}
						}
						else if ("npcs".equalsIgnoreCase(list_node.getNodeName()))
						{
							for (Node npcs_node = list_node.getFirstChild(); npcs_node != null; npcs_node = npcs_node.getNextSibling())
							{
								if ("npc".equalsIgnoreCase(npcs_node.getNodeName()))
								{
									buyList.addAllowedNpc(Integer.parseInt(npcs_node.getTextContent()));
								}
							}
						}
					}
					
					_buyLists.put(buyList.getListId(), buyList);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Failed to load buyList data from xml File:" + file.getName(), e);
		}
	}
	
	@Override
	public boolean isValidXmlFile(File file)
	{
		return (file != null) && file.isFile() && file.getName().toLowerCase().matches("\\d+\\.xml");
	}
	
	public BuyListHolder getBuyList(int listId)
	{
		return _buyLists.get(listId);
	}
	
	public static BuyListData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final BuyListData INSTANCE = new BuyListData();
	}
}
