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
package org.l2jmobius.gameserver.managers.games;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.Broadcast;

public class LotteryManager
{
	public static final long SECOND = 1000;
	public static final long MINUTE = 60000;
	
	protected static final Logger LOGGER = Logger.getLogger(LotteryManager.class.getName());
	
	private static final String INSERT_LOTTERY = "INSERT INTO lottery(id, idnr, enddate, prize, newprize) VALUES (?, ?, ?, ?, ?)";
	private static final String UPDATE_PRICE = "UPDATE lottery SET prize=?, newprize=? WHERE id = 1 AND idnr = ?";
	private static final String UPDATE_LOTTERY = "UPDATE lottery SET finished=1, prize=?, newprize=?, number1=?, number2=?, prize1=?, prize2=?, prize3=? WHERE id=1 AND idnr=?";
	private static final String SELECT_LAST_LOTTERY = "SELECT idnr, prize, newprize, enddate, finished FROM lottery WHERE id = 1 ORDER BY idnr DESC LIMIT 1";
	private static final String SELECT_LOTTERY_ITEM = "SELECT enchant_level, custom_type2 FROM items WHERE item_id = 4442 AND custom_type1 = ?";
	private static final String SELECT_LOTTERY_TICKET = "SELECT number1, number2, prize1, prize2, prize3 FROM lottery WHERE id = 1 and idnr = ?";
	
	protected int _number;
	protected int _prize;
	protected boolean _isSellingTickets;
	protected boolean _isStarted;
	protected long _enddate;
	
	protected LotteryManager()
	{
		_number = 1;
		_prize = GeneralConfig.ALT_LOTTERY_PRIZE;
		_isSellingTickets = false;
		_isStarted = false;
		_enddate = System.currentTimeMillis();
		if (GeneralConfig.ALLOW_LOTTERY)
		{
			(new startLottery()).run();
		}
	}
	
	public int getId()
	{
		return _number;
	}
	
	public long getPrize()
	{
		return _prize;
	}
	
	public long getEndDate()
	{
		return _enddate;
	}
	
	public void increasePrize(long count)
	{
		_prize += count;
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(UPDATE_PRICE))
		{
			ps.setInt(1, _prize);
			ps.setInt(2, _prize);
			ps.setInt(3, _number);
			ps.execute();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "Lottery: Could not increase current lottery prize: " + e.getMessage(), e);
		}
	}
	
	public boolean isSellableTickets()
	{
		return _isSellingTickets;
	}
	
	public boolean isStarted()
	{
		return _isStarted;
	}
	
	private class startLottery implements Runnable
	{
		protected startLottery()
		{
			// Do nothing
		}
		
		@Override
		public void run()
		{
			try (Connection con = DatabaseFactory.getConnection();
				Statement statement = con.createStatement();
				ResultSet rset = statement.executeQuery(SELECT_LAST_LOTTERY))
			{
				if (rset.next())
				{
					_number = rset.getInt("idnr");
					if (rset.getInt("finished") == 1)
					{
						_number++;
						_prize = rset.getInt("newprize");
					}
					else
					{
						_prize = rset.getInt("prize");
						_enddate = rset.getLong("enddate");
						if (_enddate <= (System.currentTimeMillis() + (2 * MINUTE)))
						{
							(new finishLottery()).run();
							return;
						}
						
						if (_enddate > System.currentTimeMillis())
						{
							_isStarted = true;
							ThreadPool.schedule(new finishLottery(), _enddate - System.currentTimeMillis());
							if (_enddate > (System.currentTimeMillis() + (12 * MINUTE)))
							{
								_isSellingTickets = true;
								ThreadPool.schedule(new stopSellingTickets(), _enddate - System.currentTimeMillis() - (10 * MINUTE));
							}
							return;
						}
					}
				}
			}
			catch (SQLException e)
			{
				LOGGER.log(Level.WARNING, "Lottery: Could not restore lottery data: " + e.getMessage(), e);
			}
			
			_isSellingTickets = true;
			_isStarted = true;
			Broadcast.toAllOnlinePlayers("Lottery tickets are now available for Lucky Lottery #" + _number + ".");
			final Calendar finishtime = Calendar.getInstance();
			finishtime.setTimeInMillis(_enddate);
			finishtime.set(Calendar.MINUTE, 0);
			finishtime.set(Calendar.SECOND, 0);
			if (finishtime.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
			{
				finishtime.set(Calendar.HOUR_OF_DAY, 19);
				_enddate = finishtime.getTimeInMillis();
				_enddate += 604800000;
			}
			else
			{
				finishtime.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
				finishtime.set(Calendar.HOUR_OF_DAY, 19);
				_enddate = finishtime.getTimeInMillis();
			}
			
			final long endDate = _enddate - System.currentTimeMillis();
			ThreadPool.schedule(new stopSellingTickets(), Math.max(endDate - (10 * MINUTE), 0));
			ThreadPool.schedule(new finishLottery(), Math.max(endDate, 0));
			
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement(INSERT_LOTTERY))
			{
				ps.setInt(1, 1);
				ps.setInt(2, _number);
				ps.setLong(3, _enddate);
				ps.setInt(4, _prize);
				ps.setInt(5, _prize);
				ps.execute();
			}
			catch (SQLException e)
			{
				LOGGER.log(Level.WARNING, "Lottery: Could not store new lottery data: " + e.getMessage(), e);
			}
		}
	}
	
	private class stopSellingTickets implements Runnable
	{
		protected stopSellingTickets()
		{
			// Do nothing
		}
		
		@Override
		public void run()
		{
			_isSellingTickets = false;
			Broadcast.toAllOnlinePlayers(new SystemMessage(SystemMessageId.LOTTERY_TICKET_SALES_HAVE_BEEN_TEMPORARILY_SUSPENDED));
		}
	}
	
	private class finishLottery implements Runnable
	{
		protected finishLottery()
		{
			// Do nothing
		}
		
		@Override
		public void run()
		{
			final int[] luckynums = new int[5];
			int luckynum = 0;
			for (int i = 0; i < 5; i++)
			{
				boolean found = true;
				
				while (found)
				{
					luckynum = Rnd.get(20) + 1;
					found = false;
					for (int j = 0; j < i; j++)
					{
						if (luckynums[j] == luckynum)
						{
							found = true;
						}
					}
				}
				
				luckynums[i] = luckynum;
			}
			
			int enchant = 0;
			int type2 = 0;
			for (int i = 0; i < 5; i++)
			{
				if (luckynums[i] < 17)
				{
					enchant += Math.pow(2, luckynums[i] - 1);
				}
				else
				{
					type2 += Math.pow(2, luckynums[i] - 17);
				}
			}
			
			int count1 = 0;
			int count2 = 0;
			int count3 = 0;
			int count4 = 0;
			
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement(SELECT_LOTTERY_ITEM))
			{
				ps.setInt(1, _number);
				try (ResultSet rset = ps.executeQuery())
				{
					while (rset.next())
					{
						int curenchant = rset.getInt("enchant_level") & enchant;
						int curtype2 = rset.getInt("custom_type2") & type2;
						if ((curenchant == 0) && (curtype2 == 0))
						{
							continue;
						}
						
						int count = 0;
						for (int i = 1; i <= 16; i++)
						{
							final int val = curenchant / 2;
							if (val != Math.round((double) curenchant / 2))
							{
								count++;
							}
							
							final int val2 = curtype2 / 2;
							if (val2 != ((double) curtype2 / 2))
							{
								count++;
							}
							
							curenchant = val;
							curtype2 = val2;
						}
						
						if (count == 5)
						{
							count1++;
						}
						else if (count == 4)
						{
							count2++;
						}
						else if (count == 3)
						{
							count3++;
						}
						else if (count > 0)
						{
							count4++;
						}
					}
				}
			}
			catch (SQLException e)
			{
				LOGGER.log(Level.WARNING, "Lottery: Could restore lottery data: " + e.getMessage(), e);
			}
			
			final int prize4 = count4 * GeneralConfig.ALT_LOTTERY_2_AND_1_NUMBER_PRIZE;
			int prize1 = 0;
			int prize2 = 0;
			int prize3 = 0;
			if (count1 > 0)
			{
				prize1 = (int) (((_prize - prize4) * GeneralConfig.ALT_LOTTERY_5_NUMBER_RATE) / count1);
			}
			
			if (count2 > 0)
			{
				prize2 = (int) (((_prize - prize4) * GeneralConfig.ALT_LOTTERY_4_NUMBER_RATE) / count2);
			}
			
			if (count3 > 0)
			{
				prize3 = (int) (((_prize - prize4) * GeneralConfig.ALT_LOTTERY_3_NUMBER_RATE) / count3);
			}
			
			final int newprize = _prize - (prize1 + prize2 + prize3 + prize4);
			SystemMessage sm;
			if (count1 > 0)
			{
				// There are winners.
				sm = new SystemMessage(SystemMessageId.THE_PRIZE_AMOUNT_FOR_THE_WINNER_OF_LOTTERY_S1_IS_S2_ADENA_WE_HAVE_S3_FIRST_PRIZE_WINNERS);
				sm.addInt(_number);
				sm.addInt(_prize);
				sm.addInt(count1);
			}
			else
			{
				// There are no winners.
				sm = new SystemMessage(SystemMessageId.THE_PRIZE_AMOUNT_FOR_LUCKY_LOTTERY_S1_IS_S2_ADENA_THERE_WAS_NO_FIRST_PRIZE_WINNER_IN_THIS_DRAWING_THEREFORE_THE_JACKPOT_WILL_BE_ADDED_TO_THE_NEXT_DRAWING);
				sm.addInt(_number);
				sm.addInt(_prize);
			}
			
			Broadcast.toAllOnlinePlayers(sm);
			
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement(UPDATE_LOTTERY))
			{
				ps.setInt(1, _prize);
				ps.setInt(2, newprize);
				ps.setInt(3, enchant);
				ps.setInt(4, type2);
				ps.setInt(5, prize1);
				ps.setInt(6, prize2);
				ps.setInt(7, prize3);
				ps.setInt(8, _number);
				ps.execute();
			}
			catch (SQLException e)
			{
				LOGGER.log(Level.WARNING, "Lottery: Could not store finished lottery data: " + e.getMessage(), e);
			}
			
			ThreadPool.schedule(new startLottery(), MINUTE);
			_number++;
			
			_isStarted = false;
		}
	}
	
	public int[] decodeNumbers(int enchantValue, int type2Value)
	{
		final int[] res = new int[5];
		int id = 0;
		int nr = 1;
		
		int enchant = enchantValue;
		while (enchant > 0)
		{
			final int val = enchant / 2;
			if (val != ((double) enchant / 2))
			{
				res[id] = nr;
				id++;
			}
			
			enchant /= 2;
			nr++;
		}
		
		nr = 17;
		
		int type2 = type2Value;
		while (type2 > 0)
		{
			final int val = type2 / 2;
			if (val != ((double) type2 / 2))
			{
				res[id] = nr;
				id++;
			}
			
			type2 /= 2;
			nr++;
		}
		
		return res;
	}
	
	public int[] checkTicket(Item item)
	{
		return checkTicket(item.getCustomType1(), item.getEnchantLevel(), item.getCustomType2());
	}
	
	public int[] checkTicket(int id, int enchant, int type2)
	{
		final int[] res =
		{
			0,
			0
		};
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(SELECT_LOTTERY_TICKET))
		{
			ps.setInt(1, id);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					int curenchant = rs.getInt("number1") & enchant;
					int curtype2 = rs.getInt("number2") & type2;
					if ((curenchant == 0) && (curtype2 == 0))
					{
						return res;
					}
					
					int count = 0;
					for (int i = 1; i <= 16; i++)
					{
						final int val = curenchant / 2;
						if (val != Math.round((double) curenchant / 2))
						{
							count++;
						}
						
						final int val2 = curtype2 / 2;
						if (val2 != ((double) curtype2 / 2))
						{
							count++;
						}
						
						curenchant = val;
						curtype2 = val2;
					}
					
					switch (count)
					{
						case 0:
						{
							break;
						}
						case 5:
						{
							res[0] = 1;
							res[1] = rs.getInt("prize1");
							break;
						}
						case 4:
						{
							res[0] = 2;
							res[1] = rs.getInt("prize2");
							break;
						}
						case 3:
						{
							res[0] = 3;
							res[1] = rs.getInt("prize3");
							break;
						}
						default:
						{
							res[0] = 4;
							res[1] = GeneralConfig.ALT_LOTTERY_2_AND_1_NUMBER_PRIZE;
						}
					}
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "Lottery: Could not check lottery ticket #" + id + ": " + e.getMessage(), e);
		}
		
		return res;
	}
	
	public static LotteryManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final LotteryManager INSTANCE = new LotteryManager();
	}
}
