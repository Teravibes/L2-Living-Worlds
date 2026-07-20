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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.Folk;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * @author n0nam3
 * @date 08/08/2010 15:11
 */
public class FishingChampionshipManager
{
	protected static final Logger LOGGER = Logger.getLogger(FishingChampionshipManager.class.getName());
	
	private static final String INSERT = "INSERT INTO fishing_championship(player_name,fish_length,rewarded) VALUES (?,?,?)";
	private static final String DELETE = "DELETE FROM fishing_championship";
	private static final String SELECT = "SELECT `player_name`, `fish_length`, `rewarded` FROM fishing_championship";
	
	private static final FishingChampionshipManager INSTANCE = new FishingChampionshipManager();
	
	public static FishingChampionshipManager getInstance()
	{
		return INSTANCE;
	}
	
	protected long _enddate = 0;
	protected final List<String> _playersName = new ArrayList<>();
	protected final List<String> _fishLength = new ArrayList<>();
	protected final List<String> _winPlayersName = new ArrayList<>();
	protected final List<String> _winFishLength = new ArrayList<>();
	protected final List<Fisher> _tmpPlayers = new ArrayList<>();
	protected final List<Fisher> _winPlayers = new ArrayList<>();
	protected double _minFishLength = 0;
	protected boolean _needRefresh = true;
	
	private FishingChampionshipManager()
	{
		restoreData();
		refreshWinResult();
		recalculateMinLength();
		
		if (_enddate <= System.currentTimeMillis())
		{
			_enddate = System.currentTimeMillis();
			new finishChamp().run();
		}
		else
		{
			ThreadPool.schedule(new finishChamp(), _enddate - System.currentTimeMillis());
		}
	}
	
	protected void setEndOfChamp()
	{
		final Calendar finishtime = Calendar.getInstance();
		finishtime.setTimeInMillis(_enddate);
		finishtime.set(Calendar.MINUTE, 0);
		finishtime.set(Calendar.SECOND, 0);
		finishtime.add(Calendar.DAY_OF_MONTH, 6);
		finishtime.set(Calendar.DAY_OF_WEEK, 3);
		finishtime.set(Calendar.HOUR_OF_DAY, 19);
		_enddate = finishtime.getTimeInMillis();
	}
	
	private void restoreData()
	{
		_enddate = GlobalVariablesManager.getInstance().getLong("fishChampionshipEnd", 0);
		
		try (Connection con = DatabaseFactory.getConnection())
		{
			final PreparedStatement statement = con.prepareStatement(SELECT);
			final ResultSet rs = statement.executeQuery();
			while (rs.next())
			{
				final int rewarded = rs.getInt("rewarded");
				if (rewarded == 0)
				{
					_tmpPlayers.add(new Fisher(rs.getString("player_name"), rs.getDouble("fish_length"), 0));
				}
				else if (rewarded > 0)
				{
					_winPlayers.add(new Fisher(rs.getString("player_name"), rs.getDouble("fish_length"), rewarded));
				}
			}
			
			rs.close();
			statement.close();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "FishingChampionshipManager: can't restore fishing championship info: " + e.getMessage(), e);
		}
	}
	
	public synchronized void newFish(Player pl, int lureId)
	{
		if (!GeneralConfig.ALT_FISH_CHAMPIONSHIP_ENABLED)
		{
			return;
		}
		
		double len = Rnd.get(60, 89) + (Rnd.get(0, 1000) / 1000.);
		if ((lureId >= 8484) && (lureId <= 8486))
		{
			len += Rnd.get(0, 3000) / 1000.;
		}
		
		pl.sendPacket(new SystemMessage(SystemMessageId.YOU_CAUGHT_A_FISH_S1_IN_LENGTH).addString(String.valueOf(len)));
		
		if (_tmpPlayers.size() < 5)
		{
			for (Fisher fisher : _tmpPlayers)
			{
				if (fisher.getName().equalsIgnoreCase(pl.getName()))
				{
					if (fisher.getLength() < len)
					{
						fisher.setLength(len);
						pl.sendPacket(SystemMessageId.BECAUSE_OF_THE_SIZE_OF_FISH_CAUGHT_YOU_WILL_BE_REGISTERED_IN_THE_RANKING);
						recalculateMinLength();
					}
					return;
				}
			}
			
			_tmpPlayers.add(new Fisher(pl.getName(), len, 0));
			pl.sendPacket(SystemMessageId.BECAUSE_OF_THE_SIZE_OF_FISH_CAUGHT_YOU_WILL_BE_REGISTERED_IN_THE_RANKING);
			recalculateMinLength();
		}
		else if (_minFishLength < len)
		{
			for (Fisher fisher : _tmpPlayers)
			{
				if (fisher.getName().equalsIgnoreCase(pl.getName()))
				{
					if (fisher.getLength() < len)
					{
						fisher.setLength(len);
						pl.sendPacket(SystemMessageId.BECAUSE_OF_THE_SIZE_OF_FISH_CAUGHT_YOU_WILL_BE_REGISTERED_IN_THE_RANKING);
						recalculateMinLength();
					}
					return;
				}
			}
			
			Fisher minFisher = null;
			double minLen = 99999.;
			for (Fisher fisher : _tmpPlayers)
			{
				if (fisher.getLength() < minLen)
				{
					minFisher = fisher;
					minLen = minFisher.getLength();
				}
			}
			
			_tmpPlayers.remove(minFisher);
			_tmpPlayers.add(new Fisher(pl.getName(), len, 0));
			pl.sendPacket(SystemMessageId.BECAUSE_OF_THE_SIZE_OF_FISH_CAUGHT_YOU_WILL_BE_REGISTERED_IN_THE_RANKING);
			recalculateMinLength();
		}
	}
	
	private void recalculateMinLength()
	{
		double minLen = 99999.;
		for (Fisher fisher : _tmpPlayers)
		{
			if (fisher.getLength() < minLen)
			{
				minLen = fisher.getLength();
			}
		}
		
		_minFishLength = minLen;
	}
	
	public long getTimeRemaining()
	{
		return (_enddate - System.currentTimeMillis()) / 60000;
	}
	
	public String getWinnerName(int par)
	{
		return _winPlayersName.size() >= par ? _winPlayersName.get(par - 1) : "None";
	}
	
	public String getCurrentName(int par)
	{
		return _playersName.size() >= par ? _playersName.get(par - 1) : "None";
	}
	
	public String getFishLength(int par)
	{
		return _winFishLength.size() >= par ? _winFishLength.get(par - 1) : "0";
	}
	
	public String getCurrentFishLength(int par)
	{
		return _fishLength.size() >= par ? _fishLength.get(par - 1) : "0";
	}
	
	public boolean isWinner(String playerName)
	{
		for (String name : _winPlayersName)
		{
			if (name.equals(playerName))
			{
				return true;
			}
		}
		
		return false;
	}
	
	public void getReward(Player pl)
	{
		for (Fisher fisher : _winPlayers)
		{
			if (fisher.getName().equalsIgnoreCase(pl.getName()) && (fisher.getRewardType() != 2))
			{
				int rewardCnt = 0;
				for (int x = 0; x < _winPlayersName.size(); x++)
				{
					if (_winPlayersName.get(x).equalsIgnoreCase(pl.getName()))
					{
						switch (x)
						{
							case 0:
							{
								rewardCnt = GeneralConfig.ALT_FISH_CHAMPIONSHIP_REWARD_1;
								break;
							}
							case 1:
							{
								rewardCnt = GeneralConfig.ALT_FISH_CHAMPIONSHIP_REWARD_2;
								break;
							}
							case 2:
							{
								rewardCnt = GeneralConfig.ALT_FISH_CHAMPIONSHIP_REWARD_3;
								break;
							}
							case 3:
							{
								rewardCnt = GeneralConfig.ALT_FISH_CHAMPIONSHIP_REWARD_4;
								break;
							}
							case 4:
							{
								rewardCnt = GeneralConfig.ALT_FISH_CHAMPIONSHIP_REWARD_5;
								break;
							}
						}
					}
				}
				
				fisher.setRewardType(2);
				if (rewardCnt > 0)
				{
					pl.addItem(ItemProcessType.REWARD, GeneralConfig.ALT_FISH_CHAMPIONSHIP_REWARD_ITEM, rewardCnt, null, true);
					
					final NpcHtmlMessage html = new NpcHtmlMessage();
					html.setFile(pl, "data/html/fisherman/championship/fish_event_reward001.htm");
					pl.sendPacket(html);
				}
			}
		}
	}
	
	public void showMidResult(Player pl)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage();
		
		if (_needRefresh)
		{
			html.setFile(pl, "data/html/fisherman/championship/fish_event003.htm");
			pl.sendPacket(html);
			
			refreshResult();
			ThreadPool.schedule(new needRefresh(), 60000);
			return;
		}
		
		html.setFile(pl, "data/html/fisherman/championship/fish_event002.htm");
		
		String str = null;
		for (int x = 1; x <= 5; x++)
		{
			str += "<tr><td width=70 align=center>" + x + "</td>";
			str += "<td width=110 align=center>" + getCurrentName(x) + "</td>";
			str += "<td width=80 align=center>" + getCurrentFishLength(x) + "</td></tr>";
		}
		
		html.replace("%TABLE%", str);
		html.replace("%prizeItem%", ItemData.getInstance().getTemplate(GeneralConfig.ALT_FISH_CHAMPIONSHIP_REWARD_ITEM).getName());
		html.replace("%prizeFirst%", String.valueOf(GeneralConfig.ALT_FISH_CHAMPIONSHIP_REWARD_1));
		html.replace("%prizeTwo%", String.valueOf(GeneralConfig.ALT_FISH_CHAMPIONSHIP_REWARD_2));
		html.replace("%prizeThree%", String.valueOf(GeneralConfig.ALT_FISH_CHAMPIONSHIP_REWARD_3));
		html.replace("%prizeFour%", String.valueOf(GeneralConfig.ALT_FISH_CHAMPIONSHIP_REWARD_4));
		html.replace("%prizeFive%", String.valueOf(GeneralConfig.ALT_FISH_CHAMPIONSHIP_REWARD_5));
		pl.sendPacket(html);
	}
	
	public void showChampScreen(Player pl, Folk npc)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage();
		html.setFile(pl, "data/html/fisherman/championship/fish_event001.htm");
		
		String str = null;
		for (int x = 1; x <= 5; x++)
		{
			str += "<tr><td width=70 align=center>" + x + "</td>";
			str += "<td width=110 align=center>" + getWinnerName(x) + "</td>";
			str += "<td width=80 align=center>" + getFishLength(x) + "</td></tr>";
		}
		
		html.replace("%TABLE%", str);
		html.replace("%prizeItem%", ItemData.getInstance().getTemplate(GeneralConfig.ALT_FISH_CHAMPIONSHIP_REWARD_ITEM).getName());
		html.replace("%prizeFirst%", String.valueOf(GeneralConfig.ALT_FISH_CHAMPIONSHIP_REWARD_1));
		html.replace("%prizeTwo%", String.valueOf(GeneralConfig.ALT_FISH_CHAMPIONSHIP_REWARD_2));
		html.replace("%prizeThree%", String.valueOf(GeneralConfig.ALT_FISH_CHAMPIONSHIP_REWARD_3));
		html.replace("%prizeFour%", String.valueOf(GeneralConfig.ALT_FISH_CHAMPIONSHIP_REWARD_4));
		html.replace("%prizeFive%", String.valueOf(GeneralConfig.ALT_FISH_CHAMPIONSHIP_REWARD_5));
		html.replace("%refresh%", String.valueOf(getTimeRemaining()));
		html.replace("%objectId%", String.valueOf(npc.getObjectId()));
		pl.sendPacket(html);
	}
	
	public void shutdown()
	{
		GlobalVariablesManager.getInstance().set("fishChampionshipEnd", _enddate);
		
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement(DELETE);
			statement.execute();
			statement.close();
			
			for (Fisher fisher : _winPlayers)
			{
				statement = con.prepareStatement(INSERT);
				statement.setString(1, fisher.getName());
				statement.setDouble(2, fisher.getLength());
				statement.setInt(3, fisher.getRewardType());
				statement.execute();
				statement.close();
			}
			
			for (Fisher fisher : _tmpPlayers)
			{
				statement = con.prepareStatement(INSERT);
				statement.setString(1, fisher.getName());
				statement.setDouble(2, fisher.getLength());
				statement.setInt(3, 0);
				statement.execute();
				statement.close();
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "FishingChampionshipManager: can't update infos: " + e.getMessage(), e);
		}
	}
	
	private synchronized void refreshResult()
	{
		_needRefresh = false;
		
		_playersName.clear();
		_fishLength.clear();
		
		Fisher fisher1;
		Fisher fisher2;
		
		for (int x = 0; x <= (_tmpPlayers.size() - 1); x++)
		{
			for (int y = 0; y <= (_tmpPlayers.size() - 2); y++)
			{
				fisher1 = _tmpPlayers.get(y);
				fisher2 = _tmpPlayers.get(y + 1);
				if (fisher1.getLength() < fisher2.getLength())
				{
					_tmpPlayers.set(y, fisher2);
					_tmpPlayers.set(y + 1, fisher1);
				}
			}
		}
		
		for (int x = 0; x <= (_tmpPlayers.size() - 1); x++)
		{
			_playersName.add(_tmpPlayers.get(x).getName());
			_fishLength.add(String.valueOf(_tmpPlayers.get(x).getLength()));
		}
	}
	
	protected void refreshWinResult()
	{
		_winPlayersName.clear();
		_winFishLength.clear();
		
		Fisher fisher1;
		Fisher fisher2;
		
		for (int x = 0; x <= (_winPlayers.size() - 1); x++)
		{
			for (int y = 0; y <= (_winPlayers.size() - 2); y++)
			{
				fisher1 = _winPlayers.get(y);
				fisher2 = _winPlayers.get(y + 1);
				if (fisher1.getLength() < fisher2.getLength())
				{
					_winPlayers.set(y, fisher2);
					_winPlayers.set(y + 1, fisher1);
				}
			}
		}
		
		for (int x = 0; x <= (_winPlayers.size() - 1); x++)
		{
			_winPlayersName.add(_winPlayers.get(x).getName());
			_winFishLength.add(String.valueOf(_winPlayers.get(x).getLength()));
		}
	}
	
	private class finishChamp implements Runnable
	{
		protected finishChamp()
		{
			// Do nothing
		}
		
		@Override
		public void run()
		{
			_winPlayers.clear();
			for (Fisher fisher : _tmpPlayers)
			{
				fisher.setRewardType(1);
				_winPlayers.add(fisher);
			}
			
			_tmpPlayers.clear();
			
			refreshWinResult();
			setEndOfChamp();
			shutdown();
			
			LOGGER.info("FishingChampionshipManager : new event period start.");
			ThreadPool.schedule(new finishChamp(), _enddate - System.currentTimeMillis());
		}
	}
	
	private class needRefresh implements Runnable
	{
		protected needRefresh()
		{
			// Do nothing
		}
		
		@Override
		public void run()
		{
			_needRefresh = true;
		}
	}
	
	private class Fisher
	{
		private double _length;
		private final String _name;
		private int _reward;
		
		public Fisher(String name, double length, int rewardType)
		{
			_name = name;
			_length = length;
			_reward = rewardType;
		}
		
		public void setLength(double value)
		{
			_length = value;
		}
		
		public void setRewardType(int value)
		{
			_reward = value;
		}
		
		public String getName()
		{
			return _name;
		}
		
		public int getRewardType()
		{
			return _reward;
		}
		
		public double getLength()
		{
			return _length;
		}
	}
}
