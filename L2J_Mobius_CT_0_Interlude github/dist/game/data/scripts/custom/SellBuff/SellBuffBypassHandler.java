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
package custom.SellBuff;

import java.util.Collections;
import java.util.StringTokenizer;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.config.custom.SellBuffsConfig;
import org.l2jmobius.gameserver.data.holders.SellBuffHolder;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.handler.BypassHandler;
import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.managers.SellBuffsManager;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * Sell Buffs bypass commands.
 * @author St3eT
 */
public class SellBuffBypassHandler implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
		"sellbuffadd",
		"sellbuffaddskill",
		"sellbuffedit",
		"sellbuffchangeprice",
		"sellbuffremove",
		"sellbuffbuymenu",
		"sellbuffbuyskill",
		"sellbuffbuyskillPet",
		"sellbuffstart",
		"sellbuffstop",
	};
	
	private SellBuffBypassHandler()
	{
		if (SellBuffsConfig.SELLBUFF_ENABLED)
		{
			BypassHandler.getInstance().registerHandler(this);
		}
	}
	
	@Override
	public boolean onCommand(String command, Player player, Creature target)
	{
		String cmd = "";
		final StringBuilder params = new StringBuilder();
		final StringTokenizer st = new StringTokenizer(command, " ");
		
		if (st.hasMoreTokens())
		{
			cmd = st.nextToken();
		}
		
		while (st.hasMoreTokens())
		{
			params.append(st.nextToken() + (st.hasMoreTokens() ? " " : ""));
		}
		
		if (cmd.isEmpty())
		{
			return false;
		}
		
		return onCommand(cmd, player, params.toString());
	}
	
	private boolean onCommand(String command, Player player, String params)
	{
		if (!SellBuffsConfig.SELLBUFF_ENABLED)
		{
			return false;
		}
		
		switch (command)
		{
			case "sellbuffstart":
			{
				if (player.isSellingBuffs() || (params == null) || params.isEmpty())
				{
					return false;
				}
				else if (player.getSellingBuffs().isEmpty())
				{
					player.sendMessage("Your list of buffs is empty, please add some buffs first!");
					return false;
				}
				else
				{
					final StringBuilder title = new StringBuilder();
					title.append("BUFF SELL: ");
					final StringTokenizer st = new StringTokenizer(params, " ");
					while (st.hasMoreTokens())
					{
						title.append(st.nextToken() + " ");
					}
					
					if (title.length() > 40)
					{
						player.sendMessage("Your title cannot exceed 29 characters in length. Please try again.");
						return false;
					}
					
					SellBuffsManager.getInstance().startSellBuffs(player, title.toString());
				}
				break;
			}
			case "sellbuffstop":
			{
				if (player.isSellingBuffs())
				{
					SellBuffsManager.getInstance().stopSellBuffs(player);
				}
				break;
			}
			case "sellbuffadd":
			{
				if (!player.isSellingBuffs())
				{
					int index = 0;
					if ((params != null) && !params.isEmpty() && StringUtil.isNumeric(params))
					{
						index = Integer.parseInt(params);
					}
					
					SellBuffsManager.getInstance().sendBuffChoiceMenu(player, index);
				}
				break;
			}
			case "sellbuffedit":
			{
				if (!player.isSellingBuffs())
				{
					SellBuffsManager.getInstance().sendBuffEditMenu(player);
				}
				break;
			}
			case "sellbuffchangeprice":
			{
				if (!player.isSellingBuffs() && (params != null) && !params.isEmpty())
				{
					final StringTokenizer st = new StringTokenizer(params, " ");
					
					int skillId = -1;
					int price = -1;
					
					if (st.hasMoreTokens())
					{
						skillId = Integer.parseInt(st.nextToken());
					}
					
					if (st.hasMoreTokens())
					{
						try
						{
							price = Integer.parseInt(st.nextToken());
						}
						catch (NumberFormatException e)
						{
							player.sendMessage("Too big price! Maximum price is " + SellBuffsConfig.SELLBUFF_MAX_PRICE);
							SellBuffsManager.getInstance().sendBuffEditMenu(player);
						}
					}
					
					if ((skillId == -1) || (price == -1))
					{
						return false;
					}
					
					final Skill skillToChange = player.getKnownSkill(skillId);
					if (skillToChange == null)
					{
						return false;
					}
					
					final SellBuffHolder holder = player.getSellingBuffs().stream().filter(h -> (h.getSkillId() == skillToChange.getId())).findFirst().orElse(null);
					if ((holder != null))
					{
						player.sendMessage("Price of " + player.getKnownSkill(holder.getSkillId()).getName() + " has been changed to " + price + "!");
						holder.setPrice(price);
						SellBuffsManager.getInstance().sendBuffEditMenu(player);
					}
				}
				break;
			}
			case "sellbuffremove":
			{
				if (!player.isSellingBuffs() && (params != null) && !params.isEmpty())
				{
					final StringTokenizer st = new StringTokenizer(params, " ");
					
					int skillId = -1;
					
					if (st.hasMoreTokens())
					{
						skillId = Integer.parseInt(st.nextToken());
					}
					
					if ((skillId == -1))
					{
						return false;
					}
					
					final Skill skillToRemove = player.getKnownSkill(skillId);
					if (skillToRemove == null)
					{
						return false;
					}
					
					final SellBuffHolder holder = player.getSellingBuffs().stream().filter(h -> (h.getSkillId() == skillToRemove.getId())).findFirst().orElse(null);
					if ((holder != null) && player.getSellingBuffs().remove(holder))
					{
						player.sendMessage("Skill " + player.getKnownSkill(holder.getSkillId()).getName() + " has been removed!");
						SellBuffsManager.getInstance().sendBuffEditMenu(player);
					}
				}
				break;
			}
			case "sellbuffaddskill":
			{
				if (!player.isSellingBuffs() && (params != null) && !params.isEmpty())
				{
					final StringTokenizer st = new StringTokenizer(params, " ");
					
					int skillId = -1;
					int price = -1;
					
					if (st.hasMoreTokens())
					{
						skillId = Integer.parseInt(st.nextToken());
					}
					
					if (st.hasMoreTokens())
					{
						try
						{
							price = Integer.parseInt(st.nextToken());
						}
						catch (NumberFormatException e)
						{
							player.sendMessage("Too big price! Maximum price is " + SellBuffsConfig.SELLBUFF_MIN_PRICE);
							SellBuffsManager.getInstance().sendBuffEditMenu(player);
						}
					}
					
					if ((skillId == -1) || (price == -1))
					{
						return false;
					}
					
					final Skill skillToAdd = player.getKnownSkill(skillId);
					if (skillToAdd == null)
					{
						return false;
					}
					else if (price < SellBuffsConfig.SELLBUFF_MIN_PRICE)
					{
						player.sendMessage("Too small price! Minimum price is " + SellBuffsConfig.SELLBUFF_MIN_PRICE);
						return false;
					}
					else if (price > SellBuffsConfig.SELLBUFF_MAX_PRICE)
					{
						player.sendMessage("Too big price! Maximum price is " + SellBuffsConfig.SELLBUFF_MAX_PRICE);
						return false;
					}
					else if (player.getSellingBuffs().size() >= SellBuffsConfig.SELLBUFF_MAX_BUFFS)
					{
						player.sendMessage("You already reached max count of buffs! Max buffs is: " + SellBuffsConfig.SELLBUFF_MAX_BUFFS);
						return false;
					}
					else if (!SellBuffsManager.getInstance().isInSellList(player, skillToAdd))
					{
						player.getSellingBuffs().add(new SellBuffHolder(skillToAdd.getId(), price));
						player.sendMessage(skillToAdd.getName() + " has been added!");
						SellBuffsManager.getInstance().sendBuffChoiceMenu(player, 0);
					}
				}
				break;
			}
			case "sellbuffbuymenu":
			{
				if ((params != null) && !params.isEmpty())
				{
					final StringTokenizer st = new StringTokenizer(params, " ");
					
					int objId = -1;
					int index = 0;
					if (st.hasMoreTokens())
					{
						objId = Integer.parseInt(st.nextToken());
					}
					
					if (st.hasMoreTokens())
					{
						index = Integer.parseInt(st.nextToken());
					}
					
					final Player seller = World.getInstance().getPlayer(objId);
					if (seller != null)
					{
						if (!seller.isSellingBuffs() || !player.isInsideRadius3D(seller, Npc.INTERACTION_DISTANCE))
						{
							return false;
						}
						
						SellBuffsManager.getInstance().sendBuffMenu(player, seller, index);
					}
				}
				break;
			}
			case "sellbuffbuyskill":
			{
				if ((params != null) && !params.isEmpty())
				{
					final StringTokenizer st = new StringTokenizer(params, " ");
					int objId = -1;
					int skillId = -1;
					int index = 0;
					
					if (st.hasMoreTokens())
					{
						objId = Integer.parseInt(st.nextToken());
					}
					
					if (st.hasMoreTokens())
					{
						skillId = Integer.parseInt(st.nextToken());
					}
					
					if (st.hasMoreTokens())
					{
						index = Integer.parseInt(st.nextToken());
					}
					
					if ((skillId == -1) || (objId == -1))
					{
						return false;
					}
					
					final Player seller = World.getInstance().getPlayer(objId);
					if (seller == null)
					{
						return false;
					}
					
					final Skill skillToBuy = seller.getKnownSkill(skillId);
					if (!seller.isSellingBuffs() || !LocationUtil.checkIfInRange(Npc.INTERACTION_DISTANCE, player, seller, true) || (skillToBuy == null))
					{
						return false;
					}
					
					if (seller.getCurrentMp() < (skillToBuy.getMpConsume() * SellBuffsConfig.SELLBUFF_MP_MULTIPLER))
					{
						player.sendMessage(seller.getName() + " has not enough mana for " + skillToBuy.getName() + "!");
						SellBuffsManager.getInstance().sendBuffMenu(player, seller, index);
						return false;
					}
					
					// Check if buff requires item to cast and seller has enough.
					final int itemConsumeId = skillToBuy.getItemConsumeId();
					final int itemConsumeCount = skillToBuy.getItemConsumeCount();
					if ((itemConsumeId > 0) && (itemConsumeCount > 0))
					{
						final long available = seller.getInventory().getInventoryItemCount(itemConsumeId, -1);
						if (available < itemConsumeCount)
						{
							final ItemTemplate requiredItem = ItemData.getInstance().getTemplate(itemConsumeId);
							final String itemName = (requiredItem != null) ? requiredItem.getName() : "required item";
							
							// Check if the seller is online before sending them the message.
							if (seller.isOnline())
							{
								seller.sendMessage(player.getName() + " tried to buy " + skillToBuy.getName() + " but you do not have enough " + itemName + "!");
							}
							
							player.sendMessage(seller.getName() + " doesn't have enough " + itemName + " to cast " + skillToBuy.getName() + "!");
							SellBuffsManager.getInstance().sendBuffMenu(player, seller, index);
							return false;
						}
					}
					
					final SellBuffHolder holder = seller.getSellingBuffs().stream().filter(h -> (h.getSkillId() == skillToBuy.getId())).findFirst().orElse(null);
					if (holder != null)
					{
						if (Quest.getQuestItemsCount(player, SellBuffsConfig.SELLBUFF_PAYMENT_ID) >= holder.getPrice())
						{
							Quest.takeItems(player, SellBuffsConfig.SELLBUFF_PAYMENT_ID, holder.getPrice());
							Quest.giveItems(seller, SellBuffsConfig.SELLBUFF_PAYMENT_ID, holder.getPrice());
							seller.reduceCurrentMp(skillToBuy.getMpConsume() * SellBuffsConfig.SELLBUFF_MP_MULTIPLER);
							
							// Consume item(s) required by the buff.
							if ((itemConsumeId > 0) && (itemConsumeCount > 0))
							{
								seller.destroyItemByItemId(ItemProcessType.FEE, itemConsumeId, itemConsumeCount, player, true);
							}
							
							// Cast buff.
							skillToBuy.activateSkill(seller, Collections.singletonList(player));
						}
						else
						{
							final ItemTemplate item = ItemData.getInstance().getTemplate(SellBuffsConfig.SELLBUFF_PAYMENT_ID);
							if (item != null)
							{
								player.sendMessage("Not enough " + item.getName() + "!");
							}
							else
							{
								player.sendMessage("Not enough items!");
							}
						}
					}
					
					SellBuffsManager.getInstance().sendBuffMenu(player, seller, index);
				}
				break;
			}
			case "sellbuffbuyskillPet":
			{
				if ((params != null) && !params.isEmpty())
				{
					final StringTokenizer st = new StringTokenizer(params, " ");
					int objId = -1;
					int skillId = -1;
					int index = 0;
					
					if (st.hasMoreTokens())
					{
						objId = Integer.parseInt(st.nextToken());
					}
					
					if (st.hasMoreTokens())
					{
						skillId = Integer.parseInt(st.nextToken());
					}
					
					if (st.hasMoreTokens())
					{
						index = Integer.parseInt(st.nextToken());
					}
					
					if ((skillId == -1) || (objId == -1))
					{
						return false;
					}
					
					final Player seller = World.getInstance().getPlayer(objId);
					if (seller == null)
					{
						return false;
					}
					
					final Skill skillToBuy = seller.getKnownSkill(skillId);
					if (!seller.isSellingBuffs() || !LocationUtil.checkIfInRange(Npc.INTERACTION_DISTANCE, player, seller, true) || (skillToBuy == null))
					{
						return false;
					}
					
					if (seller.getCurrentMp() < (skillToBuy.getMpConsume() * SellBuffsConfig.SELLBUFF_MP_MULTIPLER))
					{
						player.sendMessage(seller.getName() + " has not enough mana for " + skillToBuy.getName() + "!");
						SellBuffsManager.getInstance().sendBuffMenu(player, seller, index);
						return false;
					}
					
					// Check if buff requires item to cast and seller has enough.
					final int itemConsumeId = skillToBuy.getItemConsumeId();
					final int itemConsumeCount = skillToBuy.getItemConsumeCount();
					if ((itemConsumeId > 0) && (itemConsumeCount > 0))
					{
						final long available = seller.getInventory().getInventoryItemCount(itemConsumeId, -1);
						if (available < itemConsumeCount)
						{
							final ItemTemplate requiredItem = ItemData.getInstance().getTemplate(itemConsumeId);
							final String itemName = (requiredItem != null) ? requiredItem.getName() : "required item";
							
							// Check if the seller is online before sending them the message.
							if (seller.isOnline())
							{
								seller.sendMessage(player.getName() + " tried to buy " + skillToBuy.getName() + " but you do not have enough " + itemName + "!");
							}
							
							player.sendMessage(seller.getName() + " doesn't have enough " + itemName + " to cast " + skillToBuy.getName() + "!");
							SellBuffsManager.getInstance().sendBuffMenu(player, seller, index);
							return false;
						}
					}
					
					final SellBuffHolder holder = seller.getSellingBuffs().stream().filter(h -> (h.getSkillId() == skillToBuy.getId())).findFirst().orElse(null);
					if (holder != null)
					{
						if (Quest.getQuestItemsCount(player, SellBuffsConfig.SELLBUFF_PAYMENT_ID) >= holder.getPrice())
						{
							if ((player.getSummon() == null) || player.getSummon().isDead())
							{
								player.sendMessage("Your pet must be summoned and alive to receive buffs.");
							}
							else
							{
								Quest.takeItems(player, SellBuffsConfig.SELLBUFF_PAYMENT_ID, holder.getPrice());
								Quest.giveItems(seller, SellBuffsConfig.SELLBUFF_PAYMENT_ID, holder.getPrice());
								seller.reduceCurrentMp(skillToBuy.getMpConsume() * SellBuffsConfig.SELLBUFF_MP_MULTIPLER);
								
								// Consume item(s) required by the buff.
								if ((itemConsumeId > 0) && (itemConsumeCount > 0))
								{
									seller.destroyItemByItemId(ItemProcessType.FEE, itemConsumeId, itemConsumeCount, player, true);
								}
								
								// Cast buff.
								skillToBuy.activateSkill(seller, Collections.singletonList(player.getSummon()));
							}
						}
						else
						{
							final ItemTemplate item = ItemData.getInstance().getTemplate(SellBuffsConfig.SELLBUFF_PAYMENT_ID);
							if (item != null)
							{
								player.sendMessage("Not enough " + item.getName() + "!");
							}
							else
							{
								player.sendMessage("Not enough items!");
							}
						}
					}
					
					SellBuffsManager.getInstance().sendBuffMenu(player, seller, index);
				}
				break;
			}
		}
		
		return true;
	}
	
	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
	
	public static void main(String[] args)
	{
		new SellBuffBypassHandler();
	}
}
