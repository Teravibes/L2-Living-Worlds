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
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.ConfigLoader;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.multisell.Entry;
import org.l2jmobius.gameserver.model.multisell.Ingredient;
import org.l2jmobius.gameserver.model.multisell.ListContainer;
import org.l2jmobius.gameserver.model.multisell.PreparedListContainer;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExPCCafePointInfo;
import org.l2jmobius.gameserver.network.serverpackets.MultiSellList;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class MultisellData implements IXmlReader
{
	private final Map<Integer, ListContainer> _entries = new ConcurrentHashMap<>();
	
	public static final int PAGE_SIZE = 40;
	
	// Special IDs.
	public static final int PC_CAFE_POINTS = -100;
	public static final int CLAN_REPUTATION = -200;
	public static final int FAME = -300;
	
	protected MultisellData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_entries.clear();
		parseDatapackDirectory("data/multisell", false);
		if (GeneralConfig.CUSTOM_MULTISELL_LOAD)
		{
			parseDatapackDirectory("data/multisell/custom", false);
		}
		
		verify();
		LOGGER.log(Level.INFO, getClass().getSimpleName() + ": Loaded " + _entries.size() + " multisell lists.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		try
		{
			final int id = Integer.parseInt(file.getName().replaceAll(".xml", ""));
			int entryId = 1;
			Node att;
			final ListContainer list = new ListContainer(id);
			for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if ("list".equalsIgnoreCase(n.getNodeName()))
				{
					att = n.getAttributes().getNamedItem("applyTaxes");
					list.setApplyTaxes((att != null) && Boolean.parseBoolean(att.getNodeValue()));
					att = n.getAttributes().getNamedItem("useRate");
					if (att != null)
					{
						try
						{
							list.setUseRate(Double.parseDouble(att.getNodeValue()));
							if (list.getUseRate() <= 1e-6)
							{
								throw new NumberFormatException("The value cannot be 0"); // threat 0 as invalid value
							}
						}
						catch (NumberFormatException e)
						{
							try
							{
								list.setUseRate(ConfigLoader.class.getField(att.getNodeValue()).getDouble(ConfigLoader.class));
							}
							catch (Exception e1)
							{
								LOGGER.warning(e1.getMessage() + document.getLocalName());
								list.setUseRate(1.0);
							}
						}
						catch (DOMException e)
						{
							LOGGER.warning(e.getMessage() + document.getLocalName());
						}
					}
					
					att = n.getAttributes().getNamedItem("maintainEnchantment");
					list.setMaintainEnchantment((att != null) && Boolean.parseBoolean(att.getNodeValue()));
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if ("item".equalsIgnoreCase(d.getNodeName()))
						{
							final Entry e = parseEntry(d, entryId++, list);
							list.getEntries().add(e);
						}
						else if ("npcs".equalsIgnoreCase(d.getNodeName()))
						{
							for (Node b = d.getFirstChild(); b != null; b = b.getNextSibling())
							{
								if ("npc".equalsIgnoreCase(b.getNodeName()))
								{
									list.allowNpc(Integer.parseInt(b.getTextContent()));
								}
							}
						}
					}
				}
			}
			
			_entries.put(id, list);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Error in file " + file, e);
		}
	}
	
	@Override
	public boolean isValidXmlFile(File file)
	{
		return (file != null) && file.isFile() && file.getName().toLowerCase().matches("\\d+\\.xml");
	}
	
	private final Entry parseEntry(Node node, int entryId, ListContainer list)
	{
		Node n = node;
		final Node first = n.getFirstChild();
		final Entry entry = new Entry(entryId);
		NamedNodeMap attrs;
		Node att;
		StatSet set;
		for (n = first; n != null; n = n.getNextSibling())
		{
			if ("ingredient".equalsIgnoreCase(n.getNodeName()))
			{
				attrs = n.getAttributes();
				set = new StatSet();
				for (int i = 0; i < attrs.getLength(); i++)
				{
					att = attrs.item(i);
					set.set(att.getNodeName(), att.getNodeValue());
				}
				
				entry.addIngredient(new Ingredient(set));
			}
			else if ("production".equalsIgnoreCase(n.getNodeName()))
			{
				attrs = n.getAttributes();
				set = new StatSet();
				for (int i = 0; i < attrs.getLength(); i++)
				{
					att = attrs.item(i);
					set.set(att.getNodeName(), att.getNodeValue());
				}
				
				entry.addProduct(new Ingredient(set));
			}
		}
		
		// Check if buy price is lower than sell price.
		// Only applies when there is only one ingredient and it is adena.
		if (GeneralConfig.CORRECT_PRICES && (entry.getIngredients().size() == 1) && (entry.getProducts().size() == 1))
		{
			final Ingredient ingredient = entry.getIngredients().get(0);
			if (ingredient.getItemId() == 57)
			{
				int totalPrice = 0;
				for (Ingredient product : entry.getProducts())
				{
					final ItemTemplate template = ItemData.getInstance().getTemplate(product.getItemId());
					if (template == null)
					{
						continue;
					}
					
					totalPrice += (product.getItemCount() * (template.getReferencePrice() / 2));
				}
				
				final int adenaCount = ingredient.getItemCount();
				if (totalPrice > adenaCount)
				{
					ingredient.setItemCount(totalPrice);
					LOGGER.warning("Buy price " + adenaCount + " is less than sell price " + totalPrice + " at entry " + entryId + " of multisell " + list.getListId() + ".");
				}
			}
		}
		
		return entry;
	}
	
	/**
	 * This will generate the multisell list for the items.<br>
	 * There exist various parameters in multisells that affect the way they will appear:
	 * <ol>
	 * <li>Inventory only:
	 * <ul>
	 * <li>If true, only show items of the multisell for which the "primary" ingredients are already in the player's inventory. By "primary" ingredients we mean weapon and armor.</li>
	 * <li>If false, show the entire list.</li>
	 * </ul>
	 * </li>
	 * <li>Maintain enchantment: presumably, only lists with "inventory only" set to true should sometimes have this as true. This makes no sense otherwise...
	 * <ul>
	 * <li>If true, then the product will match the enchantment level of the ingredient.<br>
	 * If the player has multiple items that match the ingredient list but the enchantment levels differ, then the entries need to be duplicated to show the products and ingredients for each enchantment level.<br>
	 * For example: If the player has a crystal staff +1 and a crystal staff +3 and goes to exchange it at the mammon, the list should have all exchange possibilities for the +1 staff, followed by all possibilities for the +3 staff.</li>
	 * <li>If false, then any level ingredient will be considered equal and product will always be at +0</li>
	 * </ul>
	 * </li>
	 * <li>Apply taxes: Uses the "taxIngredient" entry in order to add a certain amount of adena to the ingredients.
	 * <li>
	 * <li>Additional product and ingredient multipliers.</li>
	 * </ol>
	 * @param listId
	 * @param player
	 * @param npc
	 * @param inventoryOnly
	 * @param productMultiplier
	 * @param ingredientMultiplier
	 */
	public void separateAndSend(int listId, Player player, Npc npc, boolean inventoryOnly, double productMultiplier, double ingredientMultiplier)
	{
		final ListContainer template = _entries.get(listId);
		if (template == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": can't find list id: " + listId + " requested by player: " + player.getName() + ", npcId:" + (npc != null ? npc.getId() : 0));
			return;
		}
		
		if (!template.isNpcAllowed(-1))
		{
			if ((npc == null) || !template.isNpcAllowed(npc.getId()))
			{
				if (player.isGM())
				{
					player.sendMessage("Multisell " + listId + " is restricted. Under current conditions cannot be used. Only GMs are allowed to use it.");
				}
				else
				{
					LOGGER.warning(getClass().getSimpleName() + ": " + player + " attempted to open multisell " + listId + " from npc " + npc + " which is not allowed!");
					return;
				}
			}
		}
		
		final PreparedListContainer list = new PreparedListContainer(template, inventoryOnly, player, npc);
		
		// Pass through this only when multipliers are different from 1
		if ((productMultiplier != 1) || (ingredientMultiplier != 1))
		{
			list.getEntries().forEach(entry ->
			{
				// Math.max used here to avoid dropping count to 0
				entry.getProducts().forEach(product -> product.setItemCount((int) Math.max(product.getItemCount() * productMultiplier, 1)));
				
				// Math.max used here to avoid dropping count to 0
				entry.getIngredients().forEach(ingredient -> ingredient.setItemCount((int) Math.max(ingredient.getItemCount() * ingredientMultiplier, 1)));
			});
		}
		
		int index = 0;
		do
		{
			// send list at least once even if size = 0
			player.sendPacket(new MultiSellList(list, index));
			index += PAGE_SIZE;
		}
		while (index < list.getEntries().size());
		player.setMultiSell(list);
	}
	
	public void separateAndSend(int listId, Player player, Npc npc, boolean inventoryOnly)
	{
		separateAndSend(listId, player, npc, inventoryOnly, 1, 1);
	}
	
	public static boolean hasSpecialIngredient(int id, long amount, Player player)
	{
		switch (id)
		{
			case PC_CAFE_POINTS:
			{
				if (player.getPcCafePoints() < amount)
				{
					player.sendPacket(SystemMessageId.YOU_ARE_SHORT_OF_ACCUMULATED_POINTS);
					break;
				}
				
				return true;
			}
			case CLAN_REPUTATION:
			{
				final Clan clan = player.getClan();
				if (clan == null)
				{
					player.sendPacket(SystemMessageId.YOU_ARE_NOT_A_CLAN_MEMBER_AND_CANNOT_PERFORM_THIS_ACTION);
					break;
				}
				
				if (!player.isClanLeader())
				{
					player.sendPacket(SystemMessageId.ONLY_THE_CLAN_LEADER_IS_ENABLED);
					break;
				}
				
				if (clan.getReputationScore() < amount)
				{
					player.sendPacket(SystemMessageId.THE_CLAN_REPUTATION_SCORE_IS_TOO_LOW);
					break;
				}
				
				return true;
			}
			case FAME:
			{
				if (player.getFame() < amount)
				{
					player.sendMessage("You don't have enough reputation to do that.");
					break;
				}
				
				return true;
			}
		}
		
		return false;
	}
	
	public static boolean takeSpecialIngredient(int id, int amount, Player player)
	{
		switch (id)
		{
			case PC_CAFE_POINTS:
			{
				player.setPcCafePoints(player.getPcCafePoints() - amount);
				player.sendPacket(new ExPCCafePointInfo(player.getPcCafePoints(), -amount, 0));
				return true;
			}
			case CLAN_REPUTATION:
			{
				player.getClan().takeReputationScore(amount);
				final SystemMessage smsg = new SystemMessage(SystemMessageId.S1_POINTS_HAVE_BEEN_DEDUCTED_FROM_THE_CLAN_S_REPUTATION_SCORE);
				smsg.addInt(amount);
				player.sendPacket(smsg);
				return true;
			}
			case FAME:
			{
				player.setFame(player.getFame() - amount);
				player.updateUserInfo();
				return true;
			}
		}
		
		return false;
	}
	
	public static void giveSpecialProduct(int id, long amount, Player player)
	{
		switch (id)
		{
			case PC_CAFE_POINTS:
			{
				player.setPcCafePoints((int) (player.getPcCafePoints() + amount));
				player.sendPacket(new ExPCCafePointInfo(player.getPcCafePoints(), (int) amount, 0));
				break;
			}
			case CLAN_REPUTATION:
			{
				player.getClan().addReputationScore((int) amount);
				break;
			}
			case FAME:
			{
				player.setFame((int) (player.getFame() + amount));
				player.updateUserInfo();
				break;
			}
		}
	}
	
	private void verify()
	{
		ListContainer list;
		final Iterator<ListContainer> iter = _entries.values().iterator();
		while (iter.hasNext())
		{
			list = iter.next();
			for (Entry ent : list.getEntries())
			{
				for (Ingredient ing : ent.getIngredients())
				{
					if (!verifyIngredient(ing))
					{
						LOGGER.warning(getClass().getSimpleName() + ": can't find ingredient with itemId: " + ing.getItemId() + " in list: " + list.getListId());
					}
				}
				
				for (Ingredient ing : ent.getProducts())
				{
					if (!verifyIngredient(ing))
					{
						LOGGER.warning(getClass().getSimpleName() + ": can't find product with itemId: " + ing.getItemId() + " in list: " + list.getListId());
					}
				}
			}
		}
	}
	
	private boolean verifyIngredient(Ingredient ing)
	{
		switch (ing.getItemId())
		{
			case PC_CAFE_POINTS:
			case CLAN_REPUTATION:
			case FAME:
			{
				return true;
			}
			default:
			{
				return ing.getTemplate() != null;
			}
		}
	}
	
	public static MultisellData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final MultisellData INSTANCE = new MultisellData();
	}
}
