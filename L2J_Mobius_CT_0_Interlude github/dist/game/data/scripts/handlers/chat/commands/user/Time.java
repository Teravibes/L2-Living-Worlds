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
package handlers.chat.commands.user;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.l2jmobius.gameserver.config.custom.ServerTimeConfig;
import org.l2jmobius.gameserver.handler.IUserCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;

/**
 * Time user command.
 */
public class Time implements IUserCommandHandler
{
	private static final int[] COMMAND_IDS =
	{
		77
	};
	
	private static final SimpleDateFormat SDF = new SimpleDateFormat("H:mm.");
	
	@Override
	public boolean onCommand(int id, Player player)
	{
		if (COMMAND_IDS[0] != id)
		{
			return false;
		}
		
		final int t = GameTimeTaskManager.getInstance().getGameTime();
		final String h = Integer.toString(((t / 60) % 24));
		String m;
		if ((t % 60) < 10)
		{
			m = "0" + (t % 60);
		}
		else
		{
			m = Integer.toString((t % 60));
		}
		
		SystemMessage sm;
		if (GameTimeTaskManager.getInstance().isNight())
		{
			sm = new SystemMessage(SystemMessageId.THE_CURRENT_TIME_IS_S1_S2_IN_THE_NIGHT);
			sm.addString(h);
			sm.addString(m);
		}
		else
		{
			sm = new SystemMessage(SystemMessageId.THE_CURRENT_TIME_IS_S1_S2_IN_THE_DAY);
			sm.addString(h);
			sm.addString(m);
		}
		
		player.sendPacket(sm);
		if (ServerTimeConfig.DISPLAY_SERVER_TIME)
		{
			player.sendMessage("Server time is " + SDF.format(new Date(System.currentTimeMillis())));
		}
		
		return true;
	}
	
	@Override
	public int[] getCommandList()
	{
		return COMMAND_IDS;
	}
}
