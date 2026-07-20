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
package org.l2jmobius.gameserver.managers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.enums.StatType;
import org.l2jmobius.gameserver.data.holders.RecipeHolder;
import org.l2jmobius.gameserver.data.holders.RecipeStatHolder;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.RecipeData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.item.recipe.ManufactureItem;
import org.l2jmobius.gameserver.model.item.recipe.RecipeItemInfo;
import org.l2jmobius.gameserver.model.item.recipe.RecipeList;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.skill.CommonSkill;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.RecipeBookItemList;
import org.l2jmobius.gameserver.network.serverpackets.RecipeItemMakeInfo;
import org.l2jmobius.gameserver.network.serverpackets.RecipeShopItemInfo;
import org.l2jmobius.gameserver.network.serverpackets.SetupGauge;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;

public class RecipeManager
{
	protected static final Map<Integer, RecipeItemMaker> _activeMakers = new ConcurrentHashMap<>();
	
	protected RecipeManager()
	{
		// Prevent external initialization.
	}
	
	public void requestBookOpen(Player player, boolean isDwarvenCraft)
	{
		// Check if player is trying to alter recipe book while engaged in manufacturing.
		if (!_activeMakers.containsKey(player.getObjectId()))
		{
			final RecipeBookItemList response = new RecipeBookItemList(isDwarvenCraft, player.getMaxMp());
			response.addRecipes(isDwarvenCraft ? player.getDwarvenRecipeBook() : player.getCommonRecipeBook());
			player.sendPacket(response);
			return;
		}
		
		player.sendPacket(SystemMessageId.YOU_MAY_NOT_ALTER_YOUR_RECIPE_BOOK_WHILE_ENGAGED_IN_MANUFACTURING);
	}
	
	public void requestMakeItemAbort(Player player)
	{
		_activeMakers.remove(player.getObjectId()); // TODO: anything else here?
	}
	
	public void requestManufactureItem(Player manufacturer, int recipeListId, Player player)
	{
		final RecipeList recipeList = RecipeData.getInstance().getValidRecipeList(player, recipeListId);
		if (recipeList == null)
		{
			return;
		}
		
		if (!manufacturer.getDwarvenRecipeBook().contains(recipeList) && !manufacturer.getCommonRecipeBook().contains(recipeList))
		{
			PunishmentManager.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " sent a false recipe id.", GeneralConfig.DEFAULT_PUNISH);
			return;
		}
		
		// Check if manufacturer is under manufacturing store or private store.
		if (PlayerConfig.ALT_GAME_CREATION && _activeMakers.containsKey(manufacturer.getObjectId()))
		{
			player.sendPacket(SystemMessageId.PLEASE_CLOSE_THE_THE_SETUP_WINDOW_FOR_YOUR_PRIVATE_MANUFACTURING_STORE_OR_PRIVATE_STORE_AND_TRY_AGAIN);
			return;
		}
		
		final RecipeItemMaker maker = new RecipeItemMaker(manufacturer, recipeList, player);
		if (maker._isValid)
		{
			if (PlayerConfig.ALT_GAME_CREATION)
			{
				_activeMakers.put(manufacturer.getObjectId(), maker);
				ThreadPool.schedule(maker, 100);
			}
			else
			{
				maker.run();
			}
		}
	}
	
	public void requestMakeItem(Player player, int recipeListId)
	{
		// Check if player is trying to operate a private store or private workshop while engaged in combat.
		if (player.isInCombat() || player.isInDuel())
		{
			player.sendPacket(SystemMessageId.WHILE_YOU_ARE_ENGAGED_IN_COMBAT_YOU_CANNOT_OPERATE_A_PRIVATE_STORE_OR_PRIVATE_WORKSHOP);
			return;
		}
		
		final RecipeList recipeList = RecipeData.getInstance().getValidRecipeList(player, recipeListId);
		if (recipeList == null)
		{
			return;
		}
		
		if (!player.getDwarvenRecipeBook().contains(recipeList) && !player.getCommonRecipeBook().contains(recipeList))
		{
			PunishmentManager.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " sent a false recipe id.", GeneralConfig.DEFAULT_PUNISH);
			return;
		}
		
		// Check if player is busy (possible if alt game creation is enabled)
		if (PlayerConfig.ALT_GAME_CREATION && _activeMakers.containsKey(player.getObjectId()))
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.S2_S1);
			sm.addItemName(recipeList.getItemId());
			sm.addString("You are busy creating.");
			player.sendPacket(sm);
			return;
		}
		
		final RecipeItemMaker maker = new RecipeItemMaker(player, recipeList, player);
		if (maker._isValid)
		{
			if (PlayerConfig.ALT_GAME_CREATION)
			{
				_activeMakers.put(player.getObjectId(), maker);
				ThreadPool.schedule(maker, 100);
			}
			else
			{
				maker.run();
			}
		}
	}
	
	private static class RecipeItemMaker implements Runnable
	{
		private static final Logger LOGGER = Logger.getLogger(RecipeItemMaker.class.getName());
		protected boolean _isValid;
		protected List<RecipeItemInfo> _items = null;
		protected final RecipeList _recipeList;
		protected final Player _player; // "crafter"
		protected final Player _target; // "customer"
		protected final Skill _skill;
		protected final int _skillId;
		protected final int _skillLevel;
		protected int _creationPasses = 1;
		protected int _itemGrab;
		protected int _exp = -1;
		protected int _sp = -1;
		protected int _price;
		protected int _totalItems;
		protected int _delay;
		
		public RecipeItemMaker(Player pPlayer, RecipeList pRecipeList, Player pTarget)
		{
			_player = pPlayer;
			_target = pTarget;
			_recipeList = pRecipeList;
			_isValid = false;
			_skillId = _recipeList.isDwarvenRecipe() ? CommonSkill.CREATE_DWARVEN.getId() : CommonSkill.CREATE_COMMON.getId();
			_skillLevel = _player.getSkillLevel(_skillId);
			_skill = _player.getKnownSkill(_skillId);
			_player.setCrafting(true);
			
			if (_player.isAlikeDead())
			{
				_player.sendPacket(ActionFailed.STATIC_PACKET);
				abort();
				return;
			}
			
			if (_target.isAlikeDead())
			{
				_target.sendPacket(ActionFailed.STATIC_PACKET);
				abort();
				return;
			}
			
			if (_target.isProcessingTransaction())
			{
				_target.sendPacket(ActionFailed.STATIC_PACKET);
				abort();
				return;
			}
			
			if (_player.isProcessingTransaction())
			{
				_player.sendPacket(ActionFailed.STATIC_PACKET);
				abort();
				return;
			}
			
			// validate recipe list
			if (_recipeList.getRecipes().length == 0)
			{
				_player.sendPacket(ActionFailed.STATIC_PACKET);
				abort();
				return;
			}
			
			// validate skill level
			if (_recipeList.getLevel() > _skillLevel)
			{
				_player.sendPacket(ActionFailed.STATIC_PACKET);
				abort();
				return;
			}
			
			// check that customer can afford to pay for creation services
			if (_player != _target)
			{
				final ManufactureItem item = _player.getManufactureItems().get(_recipeList.getId());
				if (item != null)
				{
					_price = item.getCost();
					if (_target.getAdena() < _price) // check price
					{
						_target.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
						abort();
						return;
					}
				}
			}
			
			// make temporary items
			_items = listItems(false);
			if (_items == null)
			{
				abort();
				return;
			}
			
			for (RecipeItemInfo i : _items)
			{
				_totalItems += i.getQuantity();
			}
			
			// initial statUse checks
			if (!calculateStatUse(false, false))
			{
				abort();
				return;
			}
			
			// initial AltStatChange checks
			if (PlayerConfig.ALT_GAME_CREATION)
			{
				calculateAltStatChange();
			}
			
			updateMakeInfo(true);
			updateCurMp();
			updateCurLoad();
			
			_player.setCrafting(false);
			_isValid = true;
		}
		
		@Override
		public void run()
		{
			if (!PlayerConfig.IS_CRAFTING_ENABLED)
			{
				_target.sendMessage("Item creation is currently disabled.");
				abort();
				return;
			}
			
			if ((_player == null) || (_target == null))
			{
				LOGGER.warning("player or target == null (disconnected?), aborting" + _target + _player);
				abort();
				return;
			}
			
			// if (!_player.isOnline() || !_target.isOnline())
			// {
			// LOGGER.warning("Player or target is not online, aborting " + _target + _player);
			// abort();
			// return;
			// }
			
			if (PlayerConfig.ALT_GAME_CREATION && !_activeMakers.containsKey(_player.getObjectId()))
			{
				if (_target != _player)
				{
					_target.sendMessage("Manufacture aborted");
					_player.sendMessage("Manufacture aborted");
				}
				else
				{
					_player.sendMessage("Item creation aborted");
				}
				
				abort();
				return;
			}
			
			if (PlayerConfig.ALT_GAME_CREATION && !_items.isEmpty())
			{
				if (!calculateStatUse(true, true))
				{
					return; // check stat use
				}
				
				updateCurMp(); // update craft window mp bar
				grabSomeItems(); // grab (equip) some more items with a nice msg to player
				
				// if still not empty, schedule another pass
				if (!_items.isEmpty())
				{
					_delay = (int) (PlayerConfig.ALT_GAME_CREATION_SPEED * _player.getMReuseRate(_skill) * GameTimeTaskManager.TICKS_PER_SECOND * GameTimeTaskManager.MILLIS_IN_TICK);
					
					// FIXME: please fix this packet to show crafting animation (somebody)
					_player.broadcastSkillPacket(new MagicSkillUse(_player, _skillId, _skillLevel, _delay, 0), _player);
					_player.sendPacket(new SetupGauge(_player.getObjectId(), 0, _delay));
					ThreadPool.schedule(this, 100 + _delay);
				}
				else
				{
					// for alt mode, sleep delay msec before finishing
					_player.sendPacket(new SetupGauge(_player.getObjectId(), 0, _delay));
					
					try
					{
						Thread.sleep(_delay);
					}
					catch (Exception e)
					{
						// Ignore.
					}
					finally
					{
						finishCrafting();
					}
				}
				
			} // for old craft mode just finish
			else
			{
				finishCrafting();
			}
		}
		
		private void finishCrafting()
		{
			if (!PlayerConfig.ALT_GAME_CREATION)
			{
				calculateStatUse(false, true);
			}
			
			// first take adena for manufacture
			if ((_target != _player) && (_price > 0) && (_target.transferItem(ItemProcessType.TRANSFER, _target.getInventory().getAdenaInstance().getObjectId(), _price, _player.getInventory(), _player) == null)) // customer must pay for services
			{
				_target.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
				abort();
				return;
			}
			
			_items = listItems(true); // this line actually takes materials from inventory
			if (_items != null)
			{
				if (Rnd.get(100) < _recipeList.getSuccessRate())
				{
					rewardPlayer();
					updateMakeInfo(true);
				}
				else
				{
					if (_target != _player)
					{
						SystemMessage msg = new SystemMessage(SystemMessageId.THE_ATTEMPT_TO_CREATE_S2_FOR_S1_AT_THE_PRICE_OF_S3_ADENA_HAS_FAILED);
						msg.addString(_target.getName());
						msg.addItemName(_recipeList.getItemId());
						msg.addInt(_price);
						_player.sendPacket(msg);
						msg = new SystemMessage(SystemMessageId.S1_HAS_FAILED_TO_CREATE_S2_AT_THE_PRICE_OF_S3_ADENA);
						msg.addString(_player.getName());
						msg.addItemName(_recipeList.getItemId());
						msg.addInt(_price);
						_target.sendPacket(msg);
					}
					else
					{
						_target.sendPacket(SystemMessageId.YOU_FAILED_AT_MIXING_THE_ITEM);
					}
					
					updateMakeInfo(false);
				}
			}
			
			// update load and mana bar of craft window
			updateCurMp();
			updateCurLoad();
			_activeMakers.remove(_player.getObjectId());
			_player.setCrafting(false);
			_target.sendItemList(false);
		}
		
		private void updateMakeInfo(boolean success)
		{
			if (_target == _player)
			{
				_target.sendPacket(new RecipeItemMakeInfo(_recipeList.getId(), _target, success));
			}
			else
			{
				_target.sendPacket(new RecipeShopItemInfo(_player, _recipeList.getId()));
			}
		}
		
		private void updateCurLoad()
		{
			final StatusUpdate su = new StatusUpdate(_target);
			su.addAttribute(StatusUpdate.CUR_LOAD, _target.getCurrentLoad());
			_target.sendPacket(su);
		}
		
		private void updateCurMp()
		{
			final StatusUpdate su = new StatusUpdate(_target);
			su.addAttribute(StatusUpdate.CUR_MP, (int) _target.getCurrentMp());
			_target.sendPacket(su);
		}
		
		private void grabSomeItems()
		{
			int grabItems = _itemGrab;
			while ((grabItems > 0) && !_items.isEmpty())
			{
				final RecipeItemInfo item = _items.get(0);
				final int count = item.getQuantity() >= grabItems ? grabItems : item.getQuantity();
				item.setQuantity(item.getQuantity() - count);
				if (item.getQuantity() <= 0)
				{
					_items.remove(0);
				}
				else
				{
					_items.set(0, item);
				}
				
				grabItems -= count;
				if (_target == _player)
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.EQUIPPED_S1_S2);
					sm.addInt(count);
					sm.addItemName(item.getItemId());
					_player.sendPacket(sm);
				}
				else
				{
					_target.sendMessage("Manufacturer " + _player.getName() + " used " + count + " " + item.getItemName());
				}
			}
		}
		
		// AltStatChange parameters make their effect here
		private void calculateAltStatChange()
		{
			_itemGrab = _skillLevel;
			for (RecipeStatHolder altStatChange : _recipeList.getAltStatChange())
			{
				if (altStatChange.getType() == StatType.XP)
				{
					_exp = altStatChange.getValue();
				}
				else if (altStatChange.getType() == StatType.SP)
				{
					_sp = altStatChange.getValue();
				}
				else if (altStatChange.getType() == StatType.GIM)
				{
					_itemGrab *= altStatChange.getValue();
				}
			}
			
			// determine number of creation passes needed
			_creationPasses = (_totalItems / _itemGrab) + ((_totalItems % _itemGrab) != 0 ? 1 : 0);
			if (_creationPasses < 1)
			{
				_creationPasses = 1;
			}
		}
		
		// StatUse
		private boolean calculateStatUse(boolean isWait, boolean isReduce)
		{
			boolean ret = true;
			for (RecipeStatHolder statUse : _recipeList.getStatUse())
			{
				final double modifiedValue = statUse.getValue() / _creationPasses;
				if (statUse.getType() == StatType.HP)
				{
					// we do not want to kill the player, so its CurrentHP must be greater than the reduce value
					if (_player.getCurrentHp() <= modifiedValue)
					{
						// rest (wait for HP)
						if (PlayerConfig.ALT_GAME_CREATION && isWait)
						{
							_player.sendPacket(new SetupGauge(_player.getObjectId(), 0, _delay));
							ThreadPool.schedule(this, 100 + _delay);
						}
						else
						{
							_target.sendPacket(SystemMessageId.NOT_ENOUGH_HP);
							abort();
						}
						
						ret = false;
					}
					else if (isReduce)
					{
						_player.reduceCurrentHp(modifiedValue, _player, _skill);
					}
				}
				else if (statUse.getType() == StatType.MP)
				{
					if (_player.getCurrentMp() < modifiedValue)
					{
						// rest (wait for MP)
						if (PlayerConfig.ALT_GAME_CREATION && isWait)
						{
							_player.sendPacket(new SetupGauge(_player.getObjectId(), 0, _delay));
							ThreadPool.schedule(this, 100 + _delay);
						}
						else
						{
							_target.sendPacket(SystemMessageId.NOT_ENOUGH_MP);
							abort();
						}
						
						ret = false;
					}
					else if (isReduce)
					{
						_player.reduceCurrentMp(modifiedValue);
					}
				}
				else
				{
					// there is an unknown StatUse value
					_target.sendMessage("Recipe error!!!, please tell this to your GM.");
					ret = false;
					abort();
				}
			}
			
			return ret;
		}
		
		private List<RecipeItemInfo> listItems(boolean remove)
		{
			final RecipeHolder[] recipes = _recipeList.getRecipes();
			final Inventory inv = _target.getInventory();
			final List<RecipeItemInfo> materials = new ArrayList<>();
			SystemMessage sm;
			for (RecipeHolder recipe : recipes)
			{
				if (recipe.getQuantity() > 0)
				{
					final Item item = inv.getItemByItemId(recipe.getItemId());
					final int itemQuantityAmount = item == null ? 0 : item.getCount();
					
					// check materials
					if (itemQuantityAmount < recipe.getQuantity())
					{
						sm = new SystemMessage(SystemMessageId.YOU_ARE_MISSING_S2_S1_REQUIRED_TO_CREATE_THAT);
						sm.addItemName(recipe.getItemId());
						sm.addInt(recipe.getQuantity() - itemQuantityAmount);
						_target.sendPacket(sm);
						
						abort();
						return null;
					}
					
					// make new temporary object, just for counting purposes
					materials.add(new RecipeItemInfo(item, recipe.getQuantity()));
				}
			}
			
			if (remove)
			{
				for (RecipeItemInfo tmp : materials)
				{
					inv.destroyItemByItemId(ItemProcessType.FEE, tmp.getItemId(), tmp.getQuantity(), _target, _player);
					if (tmp.getQuantity() > 1)
					{
						sm = new SystemMessage(SystemMessageId.S2_S1_HAS_DISAPPEARED);
						sm.addItemName(tmp.getItemId());
						sm.addInt(tmp.getQuantity());
					}
					else
					{
						sm = new SystemMessage(SystemMessageId.S1_HAS_DISAPPEARED);
						sm.addItemName(tmp.getItemId());
					}
					
					_target.sendPacket(sm);
				}
			}
			
			return materials;
		}
		
		private void abort()
		{
			updateMakeInfo(false);
			_player.setCrafting(false);
			_activeMakers.remove(_player.getObjectId());
		}
		
		private void rewardPlayer()
		{
			final int itemId = _recipeList.getItemId();
			final int itemCount = _recipeList.getCount();
			final ItemTemplate template = ItemData.getInstance().getTemplate(itemId);
			_target.getInventory().addItem(ItemProcessType.CRAFT, itemId, itemCount, _target, _player);
			
			// inform customer of earned item
			SystemMessage sm = null;
			if (_target != _player)
			{
				// inform manufacturer of earned profit
				if (itemCount == 1)
				{
					sm = new SystemMessage(SystemMessageId.S2_HAS_BEEN_CREATED_FOR_S1_AFTER_THE_PAYMENT_OF_S3_ADENA_IS_RECEIVED);
					sm.addString(_target.getName());
					sm.addItemName(itemId);
					sm.addInt(_price);
					_player.sendPacket(sm);
					
					sm = new SystemMessage(SystemMessageId.S1_CREATED_S2_AFTER_RECEIVING_S3_ADENA);
					sm.addString(_player.getName());
					sm.addItemName(itemId);
					sm.addInt(_price);
					_target.sendPacket(sm);
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S2_S3_HAVE_BEEN_CREATED_FOR_S1_AT_THE_PRICE_OF_S4_ADENA);
					sm.addString(_target.getName());
					sm.addInt(itemCount);
					sm.addItemName(itemId);
					sm.addInt(_price);
					_player.sendPacket(sm);
					
					sm = new SystemMessage(SystemMessageId.S1_CREATED_S2_S3_AT_THE_PRICE_OF_S4_ADENA);
					sm.addString(_player.getName());
					sm.addInt(itemCount);
					sm.addItemName(itemId);
					sm.addInt(_price);
					_target.sendPacket(sm);
				}
			}
			
			if (itemCount > 1)
			{
				sm = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S2_S1_S);
				sm.addItemName(itemId);
				sm.addInt(itemCount);
			}
			else
			{
				sm = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S1);
				sm.addItemName(itemId);
			}
			
			_target.sendPacket(sm);
			
			if (PlayerConfig.ALT_GAME_CREATION)
			{
				final int recipeLevel = _recipeList.getLevel();
				if (_exp < 0)
				{
					_exp = template.getReferencePrice() * itemCount;
					_exp /= recipeLevel;
				}
				
				if (_sp < 0)
				{
					_sp = _exp / 10;
				}
				
				if (_exp < 0)
				{
					_exp = 0;
				}
				
				if (_sp < 0)
				{
					_sp = 0;
				}
				
				for (int i = _skillLevel; i > recipeLevel; i--)
				{
					_exp /= 4;
					_sp /= 4;
				}
				
				// Added multiplication of Creation speed with XP/SP gain slower crafting -> more XP,
				// faster crafting -> less XP you can use ALT_GAME_CREATION_XP_RATE/SP to modify XP/SP gained (default = 1)
				_player.addExpAndSp((int) _player.calcStat(Stat.EXPSP_RATE, _exp * PlayerConfig.ALT_GAME_CREATION_XP_RATE * PlayerConfig.ALT_GAME_CREATION_SPEED, null, null), (int) _player.calcStat(Stat.EXPSP_RATE, _sp * PlayerConfig.ALT_GAME_CREATION_SP_RATE * PlayerConfig.ALT_GAME_CREATION_SPEED, null, null));
			}
			
			updateMakeInfo(true); // success
		}
	}
	
	public static RecipeManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final RecipeManager INSTANCE = new RecipeManager();
	}
}
