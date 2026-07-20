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
package custom.Transmog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.config.custom.TransmogConfig;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.Containers;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogout;
import org.l2jmobius.gameserver.model.events.holders.actor.player.inventory.OnPlayerItemAdd;
import org.l2jmobius.gameserver.model.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.model.item.EtcItem;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.BodyPart;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.util.HtmlUtil;

/**
 * @author Mobius
 */
public class Transmog extends Script
{
	private static final int NPC = 900009;
	private static final Map<Integer, Map<Integer, Set<Integer>>> PLAYER_TRANSMOGS = new ConcurrentHashMap<>();
	private static final String LOAD_SQL = "SELECT itemId FROM character_transmogs WHERE owner=? ORDER BY itemId ASC;";
	private static final String SAVE_SQL = "REPLACE INTO character_transmogs (owner,itemId) VALUE (?,?)";
	
	private Transmog()
	{
		addStartNpc(NPC);
		addTalkId(NPC);
		addFirstTalkId(NPC);
		
		if (TransmogConfig.ENABLE_TRANSMOG)
		{
			Containers.Players().addListener(new ConsumerEventListener(Containers.Players(), EventType.ON_PLAYER_ITEM_ADD, (OnPlayerItemAdd event) -> onPlayerItemAdd(event), this));
			Containers.Players().addListener(new ConsumerEventListener(Containers.Players(), EventType.ON_PLAYER_LOGIN, (OnPlayerLogin event) -> onPlayerLogin(event), this));
			Containers.Players().addListener(new ConsumerEventListener(Containers.Players(), EventType.ON_PLAYER_LOGOUT, (OnPlayerLogout event) -> onPlayerLogout(event), this));
		}
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("menu"))
		{
			return "900009-01.html";
		}
		else if (event.startsWith("rem"))
		{
			final String[] split = event.split("!");
			final String slot = split[1];
			final String page = split[2];
			if (isValidSlot(slot))
			{
				final Item item = player.getInventory().unEquipItemInBodySlot(getBodypart(slot));
				if (item != null)
				{
					final int transmogId = item.getTransmogId();
					if (transmogId > 0)
					{
						if (TransmogConfig.TRANSMOG_REMOVE_COST > 0)
						{
							if (player.getAdena() < TransmogConfig.TRANSMOG_REMOVE_COST)
							{
								player.getInventory().equipItem(item);
								player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
								onEvent(slot + "!" + page, npc, player);
								return null;
							}
							
							player.reduceAdena(ItemProcessType.FEE, TransmogConfig.TRANSMOG_REMOVE_COST, player, true);
						}
						
						item.removeTransmog();
						player.sendMessage("You have removed your " + ItemData.getInstance().getTemplate(transmogId).getName() + " transmog.");
					}
					
					player.getInventory().equipItem(item);
					player.broadcastInfo();
				}
				else
				{
					player.sendMessage("No item equipped in this slot.");
				}
			}
			
			onEvent(slot + "!" + page, npc, player);
		}
		else if (event.startsWith("set"))
		{
			final String[] split = event.split("!");
			final String slot = split[1];
			final String page = split[2];
			final int transmogId = Integer.parseInt(split[3]);
			if (isValidSlot(slot))
			{
				final BodyPart bodypart = getBodypart(slot);
				final Item item = player.getInventory().unEquipItemInBodySlot(bodypart);
				if (item != null)
				{
					final ItemTemplate itemTemplate = ItemData.getInstance().getTemplate(transmogId);
					if (itemTemplate != null)
					{
						final Map<Integer, Set<Integer>> playerTransmogs = PLAYER_TRANSMOGS.getOrDefault(player.getObjectId(), Collections.emptyMap());
						final Set<Integer> itemIds = playerTransmogs.getOrDefault(bodypart.getMask(), Collections.emptySet());
						if (itemIds.contains(transmogId))
						{
							if (TransmogConfig.TRANSMOG_APPLY_COST > 0)
							{
								if (player.getAdena() < TransmogConfig.TRANSMOG_APPLY_COST)
								{
									player.getInventory().equipItem(item);
									player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
									onEvent(slot + "!" + page, npc, player);
									return null;
								}
								
								player.reduceAdena(ItemProcessType.FEE, TransmogConfig.TRANSMOG_APPLY_COST, player, true);
							}
							
							item.setTransmogId(transmogId);
							player.sendMessage("You have set your item transmog to " + itemTemplate.getName() + ".");
						}
					}
					
					player.getInventory().equipItem(item);
					player.broadcastInfo();
				}
				else
				{
					player.sendMessage("No item equipped in this slot.");
				}
			}
			
			onEvent(slot + "!" + page, npc, player);
		}
		else
		{
			final String[] split = event.split("!");
			final String slot = split[0];
			final int page = split.length > 1 ? Integer.parseInt(split[1]) : 1;
			
			final Map<Integer, Set<Integer>> playerTransmogs = PLAYER_TRANSMOGS.getOrDefault(player.getObjectId(), Collections.emptyMap());
			final Set<Integer> itemIds = playerTransmogs.getOrDefault(getBodypart(slot).getMask(), Collections.emptySet());
			if (itemIds.isEmpty())
			{
				return "900009-02.html";
			}
			
			int total = 0;
			int counter = 0;
			final int pages = (int) Math.ceil((double) itemIds.size() / 8);
			final int maxItem = page * 8;
			final int minItem = maxItem - 8;
			String content = HtmCache.getInstance().getHtm(player, "data/scripts/custom/Transmog/900009-03.html");
			for (Integer itemId : itemIds)
			{
				final ItemTemplate itemTemplate = ItemData.getInstance().getTemplate(itemId);
				if (itemTemplate != null)
				{
					total++;
					if ((total < minItem) || (total > maxItem))
					{
						continue;
					}
					
					counter++;
					final String itemName = itemTemplate.getName();
					content = content.replace("%" + counter + "%", "<center><img src=\"" + itemTemplate.getIcon() + "\" width=32 height=32><br><a action=\"bypass Script Transmog set!" + slot + "!" + page + "!" + itemTemplate.getDisplayId() + "\">" + itemName + "</a></center>");
				}
			}
			
			for (int i = 1; i < 9; i++)
			{
				content = content.replace("%" + i + "%", "");
			}
			
			content = content.replace("%title%", "Transmog (" + page + " of " + pages + ")");
			content = content.replace("%previous%", "<a action=\"bypass Script Transmog " + slot + "!" + Math.max(1, (page - 1)) + "\">Previous</a>");
			content = content.replace("%next%", "<a action=\"bypass Script Transmog " + slot + "!" + Math.min(pages, (page + 1)) + "\">Next</a>");
			content = content.replace("%remove%", "<a action=\"bypass Script Transmog rem!" + slot + "!" + page + "\">Remove Transmog</a>");
			
			HtmlUtil.sendCBHtml(player, content);
		}
		
		return super.onEvent(event, npc, player);
	}
	
	public void onPlayerItemAdd(OnPlayerItemAdd event)
	{
		final ItemTemplate itemTemplate = event.getItem().getTemplate();
		if ((itemTemplate instanceof EtcItem) || itemTemplate.isForNpc())
		{
			return;
		}
		
		final Integer bodypart = itemTemplate.getBodyPart().getMask();
		if ((bodypart < BodyPart.R_HAND.getMask()) || (bodypart > BodyPart.HAIRALL.getMask()))
		{
			return;
		}
		
		if (TransmogConfig.TRANSMOG_BANNED_ITEM_IDS.contains(itemTemplate.getId()))
		{
			return;
		}
		
		final Player player = event.getPlayer();
		final Integer playerObjectId = player.getObjectId();
		final Map<Integer, Set<Integer>> playerTransmogs = PLAYER_TRANSMOGS.getOrDefault(playerObjectId, new HashMap<>());
		final Set<Integer> itemIds = playerTransmogs.getOrDefault(bodypart, new HashSet<>());
		if (itemIds.add(itemTemplate.getDisplayId()))
		{
			playerTransmogs.putIfAbsent(bodypart, itemIds);
			PLAYER_TRANSMOGS.putIfAbsent(playerObjectId, playerTransmogs);
			player.sendPacket(new CreatureSay(null, ChatType.WHISPER, "[Transmog]", itemTemplate.getName() + " has been added to your appearance collection."));
		}
	}
	
	private void onPlayerLogin(OnPlayerLogin event)
	{
		final Player player = event.getPlayer();
		final String owner = TransmogConfig.TRANSMOG_SHARE_ACCOUNT ? player.getAccountName() : String.valueOf(player.getObjectId());
		final Map<Integer, Set<Integer>> playerTransmogs = new HashMap<>();
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(LOAD_SQL))
		{
			statement.setString(1, owner);
			try (ResultSet result = statement.executeQuery())
			{
				while (result.next())
				{
					final int itemId = result.getInt(1);
					final ItemTemplate itemTemplate = ItemData.getInstance().getTemplate(itemId);
					if (itemTemplate != null)
					{
						final Integer bodypart = itemTemplate.getBodyPart().getMask();
						final Set<Integer> itemIds = playerTransmogs.getOrDefault(bodypart, new HashSet<>());
						itemIds.add(itemId);
						playerTransmogs.putIfAbsent(bodypart, itemIds);
					}
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.warning("Problem with Transmog: " + e.getMessage());
		}
		
		if (!playerTransmogs.isEmpty())
		{
			PLAYER_TRANSMOGS.put(player.getObjectId(), playerTransmogs);
		}
	}
	
	private void onPlayerLogout(OnPlayerLogout event)
	{
		final Player player = event.getPlayer();
		final Integer playerObjectId = player.getObjectId();
		final String owner = TransmogConfig.TRANSMOG_SHARE_ACCOUNT ? player.getAccountName() : String.valueOf(player.getObjectId());
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(SAVE_SQL))
		{
			for (Set<Integer> itemIds : PLAYER_TRANSMOGS.getOrDefault(playerObjectId, Collections.emptyMap()).values())
			{
				for (int itemId : itemIds)
				{
					statement.setString(1, owner);
					statement.setInt(2, itemId);
					statement.addBatch();
				}
			}
			
			statement.executeBatch();
		}
		catch (SQLException e)
		{
			LOGGER.warning("Problem with Transmog: " + e.getMessage());
		}
		
		PLAYER_TRANSMOGS.remove(playerObjectId);
	}
	
	private BodyPart getBodypart(String event)
	{
		switch (event)
		{
			case "R_HAND":
			{
				return BodyPart.R_HAND;
			}
			case "L_HAND":
			{
				return BodyPart.L_HAND;
			}
			case "GLOVES":
			{
				return BodyPart.GLOVES;
			}
			case "CHEST":
			{
				return BodyPart.CHEST;
			}
			case "LEGS":
			{
				return BodyPart.LEGS;
			}
			case "FEET":
			{
				return BodyPart.FEET;
			}
			case "BACK":
			{
				return BodyPart.BACK;
			}
			case "LR_HAND":
			{
				return BodyPart.LR_HAND;
			}
			case "FULL_ARMOR":
			{
				return BodyPart.FULL_ARMOR;
			}
			case "HAIRALL":
			{
				return BodyPart.HAIRALL;
			}
			case "HAIR2":
			{
				return BodyPart.HAIR2;
			}
			case "HAIR":
			{
				return BodyPart.HAIR;
			}
			default:
			{
				return BodyPart.NONE;
			}
		}
	}
	
	private boolean isValidSlot(String slot)
	{
		switch (slot)
		{
			case "R_HAND":
			case "L_HAND":
			case "GLOVES":
			case "CHEST":
			case "LEGS":
			case "FEET":
			case "BACK":
			case "LR_HAND":
			case "FULL_ARMOR":
			case "HAIRALL":
			case "HAIR2":
			case "HAIR":
			{
				return true;
			}
			default:
			{
				return false;
			}
		}
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".html";
	}
	
	public static void main(String[] args)
	{
		new Transmog();
	}
}
