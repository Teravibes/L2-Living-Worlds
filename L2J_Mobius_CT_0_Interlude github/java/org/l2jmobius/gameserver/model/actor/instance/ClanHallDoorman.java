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
package org.l2jmobius.gameserver.model.actor.instance;

import java.util.Arrays;
import java.util.StringTokenizer;

import org.l2jmobius.gameserver.data.sql.ClanHallTable;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.residences.ClanHall;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class ClanHallDoorman extends Doorman
{
	private volatile boolean _init = false;
	private ClanHall _clanHall = null;
	private boolean _hasEvolve = false;
	
	// list of clan halls with evolve function, should be sorted
	private static final int[] CH_WITH_EVOLVE =
	{
		36,
		37,
		38,
		39,
		40,
		41,
		51,
		52,
		53,
		54,
		55,
		56,
		57
	};
	
	/**
	 * Creates a clan hall doorman.
	 * @param template the doorman NPC template
	 */
	public ClanHallDoorman(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.ClanHallDoorman);
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (_hasEvolve && command.startsWith("evolve") && isOwnerClan(player))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			if (st.countTokens() < 2)
			{
				return;
			}
			
			st.nextToken();
			boolean ok = false;
			switch (Integer.parseInt(st.nextToken()))
			{
				case 1:
				{
					ok = PetManager.doEvolve(player, this, 9882, 10307, 55);
					break;
				}
				case 2:
				{
					ok = PetManager.doEvolve(player, this, 4422, 10308, 55);
					break;
				}
				case 3:
				{
					ok = PetManager.doEvolve(player, this, 4423, 10309, 55);
					break;
				}
				case 4:
				{
					ok = PetManager.doEvolve(player, this, 4424, 10310, 55);
					break;
				}
				case 5:
				{
					ok = PetManager.doEvolve(player, this, 10426, 10611, 70);
					break;
				}
			}
			
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			if (ok)
			{
				html.setFile(player, "data/html/clanHallDoorman/evolve-ok.htm");
			}
			else
			{
				html.setFile(player, "data/html/clanHallDoorman/evolve-no.htm");
			}
			
			player.sendPacket(html);
			return;
		}
		
		super.onBypassFeedback(player, command);
	}
	
	@Override
	public void showChatWindow(Player player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		if (getClanHall() != null)
		{
			final Clan owner = ClanTable.getInstance().getClan(getClanHall().getOwnerId());
			if (isOwnerClan(player))
			{
				if (_hasEvolve)
				{
					html.setFile(player, "data/html/clanHallDoorman/doorman2.htm");
					html.replace("%clanname%", owner.getName());
				}
				else
				{
					html.setFile(player, "data/html/clanHallDoorman/doorman1.htm");
					html.replace("%clanname%", owner.getName());
				}
			}
			else
			{
				if ((owner != null) && (owner.getLeader() != null))
				{
					html.setFile(player, "data/html/clanHallDoorman/doorman-no.htm");
					html.replace("%leadername%", owner.getLeaderName());
					html.replace("%clanname%", owner.getName());
				}
				else
				{
					html.setFile(player, "data/html/clanHallDoorman/emptyowner.htm");
					html.replace("%hallname%", getClanHall().getName());
				}
			}
		}
		else
		{
			return;
		}
		
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}
	
	@Override
	protected void openDoors(Player player, String command)
	{
		getClanHall().openCloseDoors(true);
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player, "data/html/clanHallDoorman/doorman-opened.htm");
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}
	
	@Override
	protected void closeDoors(Player player, String command)
	{
		getClanHall().openCloseDoors(false);
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player, "data/html/clanHallDoorman/doorman-closed.htm");
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}
	
	private final ClanHall getClanHall()
	{
		if (!_init)
		{
			synchronized (this)
			{
				if (!_init)
				{
					_clanHall = ClanHallTable.getInstance().getNearbyClanHall(getX(), getY(), 500);
					if (_clanHall != null)
					{
						_hasEvolve = Arrays.binarySearch(CH_WITH_EVOLVE, _clanHall.getId()) >= 0;
					}
					
					_init = true;
				}
			}
		}
		
		return _clanHall;
	}
	
	@Override
	protected final boolean isOwnerClan(Player player)
	{
		return (player.getClan() != null) && (getClanHall() != null) && (player.getClanId() == getClanHall().getOwnerId());
	}
}
