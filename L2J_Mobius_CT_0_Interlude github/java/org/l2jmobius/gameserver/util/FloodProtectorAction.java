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
package org.l2jmobius.gameserver.util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.punishment.PunishmentAffect;
import org.l2jmobius.gameserver.model.punishment.PunishmentTask;
import org.l2jmobius.gameserver.model.punishment.PunishmentType;
import org.l2jmobius.gameserver.network.ConnectionState;
import org.l2jmobius.gameserver.network.Disconnection;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.serverpackets.LeaveWorld;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;

/**
 * Flood protector implementation for preventing client request spam and abuse.<br>
 * Provides configurable protection intervals and punishment systems for violating clients.
 * <ul>
 * <li>Configurable protection intervals and punishment limits.</li>
 * <li>Multiple punishment types: kick, ban, and jail.</li>
 * <li>Automatic logging and monitoring of flood attempts.</li>
 * <li>GM immunity and per-client tracking.</li>
 * </ul>
 * @author fordfrog, Mobius
 */
public class FloodProtectorAction
{
	private static final Logger LOGGER = Logger.getLogger(FloodProtectorAction.class.getName());
	
	// Constants.
	private static final boolean LOG_WARNING_ENABLED = LOGGER.isLoggable(Level.WARNING);
	private static final int LOG_BUILDER_INITIAL_CAPACITY = 128;
	private static final int MINUTES_TO_MILLISECONDS = 60000;
	private static final String PUNISHMENT_KICK = "kick";
	private static final String PUNISHMENT_BAN = "ban";
	private static final String PUNISHMENT_JAIL = "jail";
	private static final String DURATION_FOREVER = "forever";
	
	// Client Connection.
	private final GameClient _client;
	private final FloodProtectorSettings _settings;
	
	// Protection State.
	private final AtomicInteger _requestCount = new AtomicInteger(0);
	private volatile int _nextGameTick = GameTimeTaskManager.getInstance().getGameTicks();
	private volatile boolean _punishmentInProgress;
	private volatile boolean _logged;
	
	/**
	 * Creates a new flood protector action for the specified client and settings.
	 * @param client
	 * @param settings
	 */
	public FloodProtectorAction(GameClient client, FloodProtectorSettings settings)
	{
		_client = client;
		_settings = settings;
	}
	
	/**
	 * Checks whether the request is flood protected or not.<br>
	 * Applies punishment if violation limits are exceeded.
	 * @return true if action is allowed, otherwise false.
	 */
	public synchronized boolean canPerformAction()
	{
		final Player player = _client.getPlayer();
		if ((player != null) && player.isGM())
		{
			return true;
		}
		
		final int currentTick = GameTimeTaskManager.getInstance().getGameTicks();
		if ((currentTick < _nextGameTick) || _punishmentInProgress)
		{
			// Log flooding if enabled and not already logged.
			if (LOG_WARNING_ENABLED && _settings.isLogFlooding() && !_logged)
			{
				final int timeUntilNext = (_settings.getProtectionInterval() - (_nextGameTick - currentTick)) * GameTimeTaskManager.MILLIS_IN_TICK;
				logFlooding(timeUntilNext);
				_logged = true;
			}
			
			// Check if punishment should be applied.
			final int currentCount = _requestCount.incrementAndGet();
			if (!_punishmentInProgress)
			{
				final int punishmentLimit = _settings.getPunishmentLimit();
				if ((punishmentLimit > 0) && (currentCount >= punishmentLimit))
				{
					final String punishmentType = _settings.getPunishmentType();
					if (punishmentType != null)
					{
						_punishmentInProgress = true;
						
						try
						{
							switch (punishmentType)
							{
								case PUNISHMENT_KICK:
								{
									Disconnection.of(_client).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
									
									if (LOG_WARNING_ENABLED)
									{
										logPunishment("kicked for flooding");
									}
									break;
								}
								case PUNISHMENT_BAN:
								{
									final long punishmentTime = _settings.getPunishmentTime();
									
									PunishmentManager.getInstance().startPunishment(new PunishmentTask(_client.getAccountName(), PunishmentAffect.ACCOUNT, PunishmentType.BAN, System.currentTimeMillis() + punishmentTime, "", getClass().getSimpleName()));
									
									if (LOG_WARNING_ENABLED)
									{
										final String duration = (punishmentTime <= 0) ? DURATION_FOREVER : StringUtil.concat("for ", String.valueOf(punishmentTime / MINUTES_TO_MILLISECONDS), " mins.");
										logPunishment(StringUtil.concat("banned for flooding ", duration));
									}
									break;
								}
								case PUNISHMENT_JAIL:
								{
									final long punishmentTime = _settings.getPunishmentTime();
									
									if (player != null)
									{
										final int characterId = player.getObjectId();
										if (characterId > 0)
										{
											PunishmentManager.getInstance().startPunishment(new PunishmentTask(characterId, PunishmentAffect.CHARACTER, PunishmentType.JAIL, System.currentTimeMillis() + punishmentTime, "", getClass().getSimpleName()));
										}
									}
									
									if (LOG_WARNING_ENABLED)
									{
										final String duration = (punishmentTime <= 0) ? DURATION_FOREVER : StringUtil.concat("for ", String.valueOf(punishmentTime / MINUTES_TO_MILLISECONDS), " mins.");
										logPunishment(StringUtil.concat("jailed for flooding ", duration));
									}
									break;
								}
								default:
								{
									if (LOG_WARNING_ENABLED)
									{
										LOGGER.warning(StringUtil.concat("FloodProtector: Unknown punishment type configured: ", punishmentType));
									}
									break;
								}
							}
						}
						finally
						{
							_punishmentInProgress = false;
						}
					}
				}
			}
			
			return false;
		}
		
		// Reset state for next interval.
		if (LOG_WARNING_ENABLED && _settings.isLogFlooding() && (_requestCount.get() > 0))
		{
			final StringBuilder sb = buildLogPrefix();
			StringUtil.append(sb, " issued ", String.valueOf(_requestCount.get()), " extra requests within ~", String.valueOf(_settings.getProtectionInterval() * GameTimeTaskManager.MILLIS_IN_TICK), " ms.");
			LOGGER.warning(sb.toString());
		}
		
		_nextGameTick = currentTick + _settings.getProtectionInterval();
		_logged = false;
		_requestCount.set(0);
		return true;
	}
	
	/**
	 * Logs flood protection violation with timing information.
	 * @param timeUntilNext
	 */
	private void logFlooding(int timeUntilNext)
	{
		final StringBuilder sb = buildLogPrefix();
		StringUtil.append(sb, " called command ", _settings.getFloodProtectorType(), " ~", String.valueOf(timeUntilNext), " ms after previous command.");
		LOGGER.warning(sb.toString());
	}
	
	/**
	 * Logs punishment action applied to violating client.
	 * @param message
	 */
	private void logPunishment(String message)
	{
		final StringBuilder sb = buildLogPrefix();
		StringUtil.append(sb, " ", message);
		LOGGER.warning(sb.toString());
	}
	
	/**
	 * Builds log message prefix with client identification information.
	 * @return StringBuilder containing client identification details
	 */
	private StringBuilder buildLogPrefix()
	{
		final StringBuilder sb = new StringBuilder(LOG_BUILDER_INITIAL_CAPACITY);
		StringUtil.append(sb, _settings.getFloodProtectorType(), ": ");
		
		final ConnectionState connectionState = _client.getConnectionState();
		switch (connectionState)
		{
			case ENTERING:
			case IN_GAME:
			{
				final Player player = _client.getPlayer();
				if (player != null)
				{
					StringUtil.append(sb, player.getName(), "(", String.valueOf(player.getObjectId()), ") ");
				}
				break;
			}
			case AUTHENTICATED:
			{
				final String accountName = _client.getAccountName();
				if (accountName != null)
				{
					StringUtil.append(sb, accountName, " ");
				}
				break;
			}
			case CONNECTED:
			{
				try
				{
					if (!_client.isDetached())
					{
						final String clientAddress = _client.getIp();
						if (clientAddress != null)
						{
							StringUtil.append(sb, clientAddress);
						}
					}
				}
				catch (Exception e)
				{
					// Ignore - no IP information available.
				}
				break;
			}
			default:
			{
				throw new IllegalStateException(StringUtil.concat("FloodProtector: Missing connection state in switch: ", connectionState.toString()));
			}
		}
		
		return sb;
	}
}
