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
package handlers.chat.commands.admin;

import java.util.StringTokenizer;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.xml.EnchantItemGroupsData;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.handler.ItemHandler;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.serverpackets.GMViewItemList;

/**
 * This class handles following admin commands: - itemcreate = show menu - create_item <id> [num] = creates num items with respective id, if num is not specified, assumes 1.
 * @version $Revision: 1.2.2.2.2.3 $ $Date: 2005/04/11 10:06:06 $
 */
public class AdminCreateItem implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_itemcreate",
		"admin_create_item",
		"admin_create_coin",
		"admin_give_item_target",
		"admin_give_item_to_all",
		"admin_delete_item",
		"admin_use_item"
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if (command.equals("admin_itemcreate"))
		{
			AdminHtml.showAdminHtml(activeChar, "itemcreation.htm");
		}
		else if (command.startsWith("admin_create_item"))
		{
			try
			{
				final String val = command.substring(17);
				final StringTokenizer st = new StringTokenizer(val);
				if (st.countTokens() >= 2)
				{
					final String id = st.nextToken();
					final int idval = Integer.parseInt(id);
					final String num = st.nextToken();
					final int numval = Integer.parseInt(num);
					if (st.hasMoreTokens())
					{
						final String enchant = st.nextToken();
						final int enchantval = Integer.parseInt(enchant);
						createItem(activeChar, activeChar, idval, numval, enchantval);
					}
					else
					{
						createItem(activeChar, activeChar, idval, numval);
					}
				}
				else if (st.countTokens() == 1)
				{
					final String id = st.nextToken();
					final int idval = Integer.parseInt(id);
					createItem(activeChar, activeChar, idval, 1);
				}
			}
			catch (StringIndexOutOfBoundsException e)
			{
				activeChar.sendSysMessage("Usage: //create_item <itemId> [amount]");
			}
			catch (NumberFormatException nfe)
			{
				activeChar.sendSysMessage("Specify a valid number.");
			}
			
			AdminHtml.showAdminHtml(activeChar, "itemcreation.htm");
		}
		else if (command.startsWith("admin_create_coin"))
		{
			try
			{
				final String val = command.substring(17);
				final StringTokenizer st = new StringTokenizer(val);
				if (st.countTokens() == 2)
				{
					final String name = st.nextToken();
					final int idval = getCoinId(name);
					if (idval > 0)
					{
						final String num = st.nextToken();
						final int numval = Integer.parseInt(num);
						createItem(activeChar, activeChar, idval, numval);
					}
				}
				else if (st.countTokens() == 1)
				{
					final String name = st.nextToken();
					final int idval = getCoinId(name);
					createItem(activeChar, activeChar, idval, 1);
				}
			}
			catch (StringIndexOutOfBoundsException e)
			{
				activeChar.sendSysMessage("Usage: //create_coin <name> [amount]");
			}
			catch (NumberFormatException nfe)
			{
				activeChar.sendSysMessage("Specify a valid number.");
			}
			
			AdminHtml.showAdminHtml(activeChar, "itemcreation.htm");
		}
		else if (command.startsWith("admin_give_item_target"))
		{
			try
			{
				final WorldObject target = activeChar.getTarget();
				if ((target == null) || !target.isPlayer())
				{
					activeChar.sendSysMessage("Invalid target.");
					return false;
				}
				
				final String val = command.substring(22);
				final StringTokenizer st = new StringTokenizer(val);
				if (st.countTokens() == 2)
				{
					final String id = st.nextToken();
					final int idval = Integer.parseInt(id);
					final String num = st.nextToken();
					final int numval = Integer.parseInt(num);
					createItem(activeChar, target.asPlayer(), idval, numval);
				}
				else if (st.countTokens() == 1)
				{
					final String id = st.nextToken();
					final int idval = Integer.parseInt(id);
					createItem(activeChar, target.asPlayer(), idval, 1);
				}
			}
			catch (StringIndexOutOfBoundsException e)
			{
				activeChar.sendSysMessage("Usage: //give_item_target <itemId> [amount]");
			}
			catch (NumberFormatException nfe)
			{
				activeChar.sendSysMessage("Specify a valid number.");
			}
			
			AdminHtml.showAdminHtml(activeChar, "itemcreation.htm");
		}
		else if (command.startsWith("admin_give_item_to_all"))
		{
			final String val = command.substring(22);
			final StringTokenizer st = new StringTokenizer(val);
			int idval = 0;
			int numval = 0;
			if (st.countTokens() == 2)
			{
				final String id = st.nextToken();
				idval = Integer.parseInt(id);
				final String num = st.nextToken();
				numval = Integer.parseInt(num);
			}
			else if (st.countTokens() == 1)
			{
				final String id = st.nextToken();
				idval = Integer.parseInt(id);
				numval = 1;
			}
			
			int counter = 0;
			final ItemTemplate template = ItemData.getInstance().getTemplate(idval);
			if (template == null)
			{
				activeChar.sendSysMessage("This item doesn't exist.");
				return false;
			}
			
			if ((numval > 10) && !template.isStackable())
			{
				activeChar.sendSysMessage("This item does not stack - Creation aborted.");
				return false;
			}
			
			for (Player onlinePlayer : World.getInstance().getPlayers())
			{
				if ((activeChar != onlinePlayer) && onlinePlayer.isOnline() && ((onlinePlayer.getClient() != null) && !onlinePlayer.getClient().isDetached()))
				{
					onlinePlayer.getInventory().addItem(ItemProcessType.REWARD, idval, numval, onlinePlayer, activeChar);
					onlinePlayer.sendMessage("Admin spawned " + numval + " " + template.getName() + " in your inventory.");
					counter++;
				}
			}
			
			activeChar.sendMessage(counter + " players rewarded with " + template.getName());
		}
		else if (command.startsWith("admin_delete_item"))
		{
			final String val = command.substring(18);
			final StringTokenizer st = new StringTokenizer(val);
			int idval = 0;
			int numval = 0;
			if (st.countTokens() == 2)
			{
				final String id = st.nextToken();
				idval = Integer.parseInt(id);
				final String num = st.nextToken();
				numval = Integer.parseInt(num);
			}
			else if (st.countTokens() == 1)
			{
				final String id = st.nextToken();
				idval = Integer.parseInt(id);
				numval = 1;
			}
			
			final Item item = (Item) World.getInstance().findObject(idval);
			final int ownerId = item.getOwnerId();
			if (ownerId > 0)
			{
				final Player player = World.getInstance().getPlayer(ownerId);
				if (player == null)
				{
					activeChar.sendSysMessage("Player is not online.");
					return false;
				}
				
				if (numval == 0)
				{
					numval = item.getCount();
				}
				
				player.getInventory().destroyItem(ItemProcessType.DESTROY, idval, numval, activeChar, null);
				activeChar.sendPacket(new GMViewItemList(player));
				activeChar.sendSysMessage("Item deleted.");
			}
			else
			{
				activeChar.sendSysMessage("Item doesn't have owner.");
				return false;
			}
		}
		else if (command.startsWith("admin_use_item"))
		{
			final String val = command.substring(15);
			final int idval = Integer.parseInt(val);
			final Item item = (Item) World.getInstance().findObject(idval);
			final int ownerId = item.getOwnerId();
			if (ownerId > 0)
			{
				final Player player = World.getInstance().getPlayer(ownerId);
				if (player == null)
				{
					activeChar.sendSysMessage("Player is not online.");
					return false;
				}
				
				// equip
				if (item.isEquipable())
				{
					player.useEquippableItem(item, false);
				}
				else
				{
					final IItemHandler handler = ItemHandler.getInstance().getHandler(item.getEtcItem());
					if (handler != null)
					{
						handler.onItemUse(player, item, false);
					}
				}
				
				activeChar.sendPacket(new GMViewItemList(player));
			}
			else
			{
				activeChar.sendSysMessage("Item doesn't have owner.");
				return false;
			}
		}
		
		return true;
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private void createItem(Player activeChar, Player target, int id, int num)
	{
		final ItemTemplate template = ItemData.getInstance().getTemplate(id);
		if (template == null)
		{
			activeChar.sendSysMessage("This item doesn't exist.");
			return;
		}
		
		if ((num > 10) && !template.isStackable())
		{
			activeChar.sendSysMessage("This item does not stack - Creation aborted.");
			return;
		}
		
		target.getInventory().addItem(ItemProcessType.REWARD, id, num, target, activeChar);
		if (activeChar != target)
		{
			target.sendMessage("Admin spawned " + num + " " + template.getName() + " in your inventory.");
		}
		
		target.sendItemList(false);
		
		activeChar.sendSysMessage("You have spawned " + num + " " + template.getName() + "(" + id + ") in " + target.getName() + " inventory.");
	}
	
	private void createItem(Player activeChar, Player target, int id, int num, int enchant)
	{
		final ItemTemplate template = ItemData.getInstance().getTemplate(id);
		if (template == null)
		{
			activeChar.sendSysMessage("This item doesn't exist.");
			return;
		}
		
		if ((num > 10) && !template.isStackable())
		{
			activeChar.sendSysMessage("This item does not stack - Creation aborted.");
			return;
		}
		
		final Item item = target.getInventory().addItem(ItemProcessType.REWARD, id, num, target, activeChar);
		if ((item != null) && item.isEnchantable() && (enchant > 0))
		{
			item.setEnchantLevel(PlayerConfig.OVER_ENCHANT_PROTECTION ? Math.min(enchant, getMaxEnchant(item)) : enchant);
		}
		else
		{
			activeChar.sendMessage("This item is not enchantable, no enchant applied.");
		}
		
		if (activeChar != target)
		{
			target.sendMessage("Admin spawned " + num + " " + template.getName() + " in your inventory.");
		}
		
		target.sendItemList(false);
		
		activeChar.sendSysMessage("You have spawned " + num + " " + template.getName() + "(" + id + ") in " + target.getName() + " inventory.");
	}
	
	public int getMaxEnchant(Item itemInstance)
	{
		if (itemInstance.isWeapon())
		{
			return EnchantItemGroupsData.getInstance().getMaxWeaponEnchant();
		}
		else if (itemInstance.getTemplate().getType2() == ItemTemplate.TYPE2_ACCESSORY)
		{
			return EnchantItemGroupsData.getInstance().getMaxAccessoryEnchant();
		}
		else
		{
			return EnchantItemGroupsData.getInstance().getMaxArmorEnchant();
		}
	}
	
	private int getCoinId(String name)
	{
		int id;
		if (name.equalsIgnoreCase("adena"))
		{
			id = 57;
		}
		else if (name.equalsIgnoreCase("ancientadena"))
		{
			id = 5575;
		}
		else if (name.equalsIgnoreCase("festivaladena"))
		{
			id = 6673;
		}
		else if (name.equalsIgnoreCase("blueeva"))
		{
			id = 4355;
		}
		else if (name.equalsIgnoreCase("goldeinhasad"))
		{
			id = 4356;
		}
		else if (name.equalsIgnoreCase("silvershilen"))
		{
			id = 4357;
		}
		else if (name.equalsIgnoreCase("bloodypaagrio"))
		{
			id = 4358;
		}
		else
		{
			id = 0;
		}
		
		return id;
	}
}
