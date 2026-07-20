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
package ai.bosses.Zaken;

import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.model.actor.instance.Door;
import org.l2jmobius.gameserver.model.events.Containers;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.OnDayNightChange;
import org.l2jmobius.gameserver.model.events.listeners.ConsumerEventListener;

/*
 * @author Skache
 */
public class ZakenDoor
{
	private static final Logger LOGGER = Logger.getLogger(ZakenDoor.class.getName());
	
	private static final int ZAKEN_DOOR_ID = 21240006;
	private static final long CLOSE_DELAY = 300000L; // 5 minutes in real time.
	
	private ZakenDoor()
	{
		Containers.Global().addListener(new ConsumerEventListener(Containers.Global(), EventType.ON_DAY_NIGHT_CHANGE, (OnDayNightChange event) -> onDayNightChange(event), this));
	}
	
	private void onDayNightChange(OnDayNightChange event)
	{
		if (event.isNight())
		{
			openZakenDoor();
		}
	}
	
	private void openZakenDoor()
	{
		final Door door = DoorData.getInstance().getDoor(ZAKEN_DOOR_ID);
		if (door == null)
		{
			LOGGER.warning("ZakenDoor: Door ID " + ZAKEN_DOOR_ID + " not found in DoorData!");
			return;
		}
		
		try
		{
			door.openMe();
			LOGGER.info("Zaken door ID: " + ZAKEN_DOOR_ID + " opened, game time 00:00.");
			ThreadPool.schedule(this::closeZakenDoor, CLOSE_DELAY);
		}
		catch (Exception e)
		{
			LOGGER.warning("Failed to open Zaken door ID " + ZAKEN_DOOR_ID + ": " + e);
		}
	}
	
	private void closeZakenDoor()
	{
		final Door door = DoorData.getInstance().getDoor(ZAKEN_DOOR_ID);
		if (door == null)
		{
			LOGGER.warning("ZakenDoor: Door ID " + ZAKEN_DOOR_ID + " not found in DoorData!");
			return;
		}
		
		try
		{
			door.closeMe();
			LOGGER.info("Zaken door ID: " + ZAKEN_DOOR_ID + " closed, game time 00:30.");
		}
		catch (Exception e)
		{
			LOGGER.warning("Failed to close Zaken door ID " + ZAKEN_DOOR_ID + ": " + e);
		}
	}
	
	public static void main(String[] args)
	{
		new ZakenDoor();
	}
}
