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
package org.l2jmobius.gameserver.model.residences;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.sql.ClanHallTable;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.managers.ClanHallAuctionManager;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class AuctionableHall extends ClanHall
{
	private static final int CH_RATE = 604800000;
	
	protected long _paidUntil;
	private final int _grade;
	protected boolean _paid;
	final int _lease;
	
	public AuctionableHall(StatSet set)
	{
		super(set);
		_paidUntil = set.getLong("paidUntil");
		_grade = set.getInt("grade");
		_paid = set.getBoolean("paid");
		_lease = set.getInt("lease");
		if (getOwnerId() != 0)
		{
			_isFree = false;
			initialyzeTask(false);
			loadFunctions();
		}
	}
	
	/**
	 * @return if clanHall is paid or not
	 */
	public boolean getPaid()
	{
		return _paid;
	}
	
	/** Return lease */
	@Override
	public int getLease()
	{
		return _lease;
	}
	
	/** Return PaidUntil */
	@Override
	public long getPaidUntil()
	{
		return _paidUntil;
	}
	
	/** Return Grade */
	@Override
	public int getGrade()
	{
		return _grade;
	}
	
	@Override
	public void free()
	{
		super.free();
		_paidUntil = 0;
		_paid = false;
	}
	
	@Override
	public void setOwner(Clan clan)
	{
		super.setOwner(clan);
		_paidUntil = System.currentTimeMillis();
		initialyzeTask(true);
	}
	
	/**
	 * Initialize Fee Task
	 * @param forced
	 */
	private void initialyzeTask(boolean forced)
	{
		final long currentTime = System.currentTimeMillis();
		if (_paidUntil > currentTime)
		{
			ThreadPool.schedule(new FeeTask(), _paidUntil - currentTime);
		}
		else if (!_paid && !forced)
		{
			if ((System.currentTimeMillis() + (3600000 * 24)) <= (_paidUntil + CH_RATE))
			{
				ThreadPool.schedule(new FeeTask(), System.currentTimeMillis() + (3600000 * 24));
			}
			else
			{
				ThreadPool.schedule(new FeeTask(), (_paidUntil + CH_RATE) - System.currentTimeMillis());
			}
		}
		else
		{
			ThreadPool.schedule(new FeeTask(), 0);
		}
	}
	
	/** Fee Task */
	protected class FeeTask implements Runnable
	{
		private final Logger LOGGER = Logger.getLogger(FeeTask.class.getName());
		
		@Override
		public void run()
		{
			try
			{
				final long _time = System.currentTimeMillis();
				if (isFree())
				{
					return;
				}
				
				if (_paidUntil > _time)
				{
					ThreadPool.schedule(new FeeTask(), _paidUntil - _time);
					return;
				}
				
				final Clan clan = ClanTable.getInstance().getClan(getOwnerId());
				if (ClanTable.getInstance().getClan(getOwnerId()).getWarehouse().getAdena() >= getLease())
				{
					if (_paidUntil != 0)
					{
						while (_paidUntil <= _time)
						{
							_paidUntil += CH_RATE;
						}
					}
					else
					{
						_paidUntil = _time + CH_RATE;
					}
					
					ClanTable.getInstance().getClan(getOwnerId()).getWarehouse().destroyItemByItemId(ItemProcessType.FEE, Inventory.ADENA_ID, getLease(), null, null);
					ThreadPool.schedule(new FeeTask(), _paidUntil - _time);
					_paid = true;
					updateDb();
				}
				else
				{
					_paid = false;
					if (_time > (_paidUntil + CH_RATE))
					{
						if (ClanHallTable.getInstance().loaded())
						{
							ClanHallAuctionManager.getInstance().initNPC(getId());
							ClanHallTable.getInstance().setFree(getId());
							clan.broadcastToOnlineMembers(new SystemMessage(SystemMessageId.THE_CLAN_HALL_FEE_IS_ONE_WEEK_OVERDUE_THEREFORE_THE_CLAN_HALL_OWNERSHIP_HAS_BEEN_REVOKED));
						}
						else
						{
							ThreadPool.schedule(new FeeTask(), 3000);
						}
					}
					else
					{
						updateDb();
						final SystemMessage sm = new SystemMessage(SystemMessageId.PAYMENT_FOR_YOUR_CLAN_HALL_HAS_NOT_BEEN_MADE_PLEASE_MAKE_PAYMENT_TO_YOUR_CLAN_WAREHOUSE_BY_S1_TOMORROW);
						sm.addInt(_lease);
						clan.broadcastToOnlineMembers(sm);
						if ((_time + (3600000 * 24)) <= (_paidUntil + CH_RATE))
						{
							ThreadPool.schedule(new FeeTask(), _time + (3600000 * 24));
						}
						else
						{
							ThreadPool.schedule(new FeeTask(), (_paidUntil + CH_RATE) - _time);
						}
					}
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "", e);
			}
		}
	}
	
	@Override
	public void updateDb()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE clanhall SET ownerId=?, paidUntil=?, paid=? WHERE id=?"))
		{
			ps.setInt(1, getOwnerId());
			ps.setLong(2, _paidUntil);
			ps.setInt(3, _paid ? 1 : 0);
			ps.setInt(4, getId());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception: updateOwnerInDB(Pledge clan): " + e.getMessage(), e);
		}
	}
}
