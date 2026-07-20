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
package org.l2jmobius.gameserver.taskmanagers;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.custom.AutoPotionsConfig;
import org.l2jmobius.gameserver.handler.ItemHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.instance.Item;

/**
 * @author Mobius, Gigi
 */
public class AutoPotionTaskManager implements Runnable
{
	private static final Set<Player> PLAYERS = ConcurrentHashMap.newKeySet();
	private static boolean _working = false;
	
	protected AutoPotionTaskManager()
	{
		ThreadPool.schedulePriorityTaskAtFixedRate(this, 0, 1000);
	}
	
	@Override
	public void run()
	{
		if (_working)
		{
			return;
		}
		
		_working = true;
		
		if (!PLAYERS.isEmpty())
		{
			PLAYER: for (Player player : PLAYERS)
			{
				if ((player == null) || player.isAlikeDead() || (player.isOnlineInt() != 1) || (!AutoPotionsConfig.AUTO_POTIONS_IN_OLYMPIAD && player.isInOlympiadMode()))
				{
					remove(player);
					continue PLAYER;
				}
				
				boolean success = false;
				if (AutoPotionsConfig.AUTO_HP_ENABLED)
				{
					final boolean restoreHP = ((player.getStatus().getCurrentHp() / player.getMaxHp()) * 100) < AutoPotionsConfig.AUTO_HP_PERCENTAGE;
					HP: for (int itemId : AutoPotionsConfig.AUTO_HP_ITEM_IDS)
					{
						final Item hpPotion = player.getInventory().getItemByItemId(itemId);
						if ((hpPotion != null) && (hpPotion.getCount() > 0))
						{
							success = true;
							if (restoreHP)
							{
								ItemHandler.getInstance().getHandler(hpPotion.getEtcItem()).onItemUse(player, hpPotion, false);
								player.sendMessage("Auto potion: Restored HP.");
								break HP;
							}
						}
					}
				}
				
				if (AutoPotionsConfig.AUTO_CP_ENABLED)
				{
					final boolean restoreCP = ((player.getStatus().getCurrentCp() / player.getMaxCp()) * 100) < AutoPotionsConfig.AUTO_CP_PERCENTAGE;
					CP: for (int itemId : AutoPotionsConfig.AUTO_CP_ITEM_IDS)
					{
						final Item cpPotion = player.getInventory().getItemByItemId(itemId);
						if ((cpPotion != null) && (cpPotion.getCount() > 0))
						{
							success = true;
							if (restoreCP)
							{
								ItemHandler.getInstance().getHandler(cpPotion.getEtcItem()).onItemUse(player, cpPotion, false);
								player.sendMessage("Auto potion: Restored CP.");
								break CP;
							}
						}
					}
				}
				
				if (AutoPotionsConfig.AUTO_MP_ENABLED)
				{
					final boolean restoreMP = ((player.getStatus().getCurrentMp() / player.getMaxMp()) * 100) < AutoPotionsConfig.AUTO_MP_PERCENTAGE;
					MP: for (int itemId : AutoPotionsConfig.AUTO_MP_ITEM_IDS)
					{
						final Item mpPotion = player.getInventory().getItemByItemId(itemId);
						if ((mpPotion != null) && (mpPotion.getCount() > 0))
						{
							success = true;
							if (restoreMP)
							{
								ItemHandler.getInstance().getHandler(mpPotion.getEtcItem()).onItemUse(player, mpPotion, false);
								player.sendMessage("Auto potion: Restored MP.");
								break MP;
							}
						}
					}
				}
				
				if (!success)
				{
					player.sendMessage("Auto potion: You are out of potions!");
				}
			}
		}
		
		_working = false;
	}
	
	public void add(Player player)
	{
		if (!PLAYERS.contains(player))
		{
			PLAYERS.add(player);
		}
	}
	
	public void remove(Player player)
	{
		PLAYERS.remove(player);
	}
	
	public boolean hasPlayer(Player player)
	{
		return PLAYERS.contains(player);
	}
	
	public static AutoPotionTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final AutoPotionTaskManager INSTANCE = new AutoPotionTaskManager();
	}
}
