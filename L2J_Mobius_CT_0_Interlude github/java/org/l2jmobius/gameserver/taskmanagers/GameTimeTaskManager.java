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

import java.util.Calendar;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.managers.DayNightSpawnManager;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.OnDayNightChange;
import org.l2jmobius.gameserver.model.skill.CommonSkill;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * GameTime task manager class.
 * @author Forsaiken, Mobius
 */
public class GameTimeTaskManager extends Thread
{
	public static final int TICKS_PER_SECOND = 10; // Not able to change this without checking through code.
	public static final int MILLIS_IN_TICK = 1000 / TICKS_PER_SECOND;
	public static final int IG_DAYS_PER_DAY = 6;
	public static final int MILLIS_PER_IG_DAY = (3600000 * 24) / IG_DAYS_PER_DAY;
	public static final int SECONDS_PER_IG_DAY = MILLIS_PER_IG_DAY / 1000;
	public static final int TICKS_PER_IG_DAY = SECONDS_PER_IG_DAY * TICKS_PER_SECOND;
	
	private final long _referenceTime;
	private boolean _isNight;
	private int _gameTicks;
	private int _gameTime;
	private int _gameHour;
	
	protected GameTimeTaskManager()
	{
		super("GameTimeTaskManager");
		super.setDaemon(true);
		super.setPriority(MAX_PRIORITY);
		
		final Calendar c = Calendar.getInstance();
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		_referenceTime = c.getTimeInMillis();
		
		super.start();
	}
	
	@Override
	public void run()
	{
		while (true)
		{
			_gameTicks = (int) ((System.currentTimeMillis() - _referenceTime) / MILLIS_IN_TICK);
			_gameTime = (_gameTicks % TICKS_PER_IG_DAY) / MILLIS_IN_TICK;
			_gameHour = _gameTime / 60;
			
			if ((_gameHour < 6) != _isNight)
			{
				_isNight = !_isNight;
				
				// Shadow Sense skill for Dark Elf players.
				for (Player player : World.getInstance().getPlayers())
				{
					if (player.getRace() == Race.DARK_ELF)
					{
						final Skill shadowSense = player.getKnownSkill(CommonSkill.SHADOW_SENSE.getId());
						if (shadowSense != null)
						{
							if (_isNight)
							{
								// It is night, apply the skill.
								player.sendPacket(new SystemMessage(SystemMessageId.IT_IS_NOW_MIDNIGHT_AND_THE_EFFECT_OF_S1_CAN_BE_FELT).addSkillName(shadowSense));
								player.addSkill(shadowSense, false);
							}
							else
							{
								// It is day, remove the skill.
								player.sendPacket(new SystemMessage(SystemMessageId.IT_IS_DAWN_AND_THE_EFFECT_OF_S1_WILL_NOW_DISAPPEAR).addSkillName(shadowSense));
								player.getEffectList().stopSkillEffects(SkillFinishType.NORMAL, shadowSense);
							}
						}
					}
				}
				
				ThreadPool.execute(() -> DayNightSpawnManager.getInstance().notifyChangeMode());
				
				if (EventDispatcher.getInstance().hasListener(EventType.ON_DAY_NIGHT_CHANGE))
				{
					EventDispatcher.getInstance().notifyEventAsync(new OnDayNightChange(_isNight));
				}
			}
			
			try
			{
				Thread.sleep(MILLIS_IN_TICK);
			}
			catch (InterruptedException e)
			{
				// Ignore.
			}
		}
	}
	
	public boolean isNight()
	{
		return _isNight;
	}
	
	/**
	 * @return The actual GameTime tick. Directly taken from current time.
	 */
	public int getGameTicks()
	{
		return _gameTicks;
	}
	
	public int getGameTime()
	{
		return _gameTime;
	}
	
	public int getGameHour()
	{
		return _gameHour;
	}
	
	public int getGameMinute()
	{
		return _gameTime % 60;
	}
	
	public static final GameTimeTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final GameTimeTaskManager INSTANCE = new GameTimeTaskManager();
	}
}
