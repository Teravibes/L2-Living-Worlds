/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.model.olympiad;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.config.OlympiadConfig;
import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.xml.ClassListData;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.data.xml.SendMessageLocalisationData;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Hero entity.
 * @author godson
 */
public class Hero
{
	private static final Logger LOGGER = Logger.getLogger(Hero.class.getName());
	
	private static final String GET_HEROES = "SELECT heroes.charId, characters.char_name, heroes.class_id, heroes.count, heroes.played, heroes.claimed FROM heroes, characters WHERE characters.charId = heroes.charId AND heroes.played = 1";
	private static final String GET_ALL_HEROES = "SELECT heroes.charId, characters.char_name, heroes.class_id, heroes.count, heroes.played, heroes.claimed FROM heroes, characters WHERE characters.charId = heroes.charId";
	private static final String UPDATE_ALL = "UPDATE heroes SET played = 0";
	private static final String INSERT_HERO = "INSERT INTO heroes (charId, class_id, count, played, claimed) VALUES (?,?,?,?,?)";
	private static final String UPDATE_HERO = "UPDATE heroes SET class_id = ?, count = ?, played = ?, claimed = ? WHERE charId = ?";
	private static final String GET_CLAN_ALLY = "SELECT characters.clanid AS clanid, coalesce(clan_data.ally_Id, 0) AS allyId FROM characters LEFT JOIN clan_data ON clan_data.clan_id = characters.clanid WHERE characters.charId = ?";
	private static final String DELETE_ITEMS = "DELETE FROM items WHERE item_id IN (6842, 6611, 6612, 6613, 6614, 6615, 6616, 6617, 6618, 6619, 6620, 6621) AND owner_id NOT IN (SELECT charId FROM characters WHERE accesslevel > 0)";
	
	private static final Map<Integer, StatSet> HEROES = new ConcurrentHashMap<>();
	private static final Map<Integer, StatSet> COMPLETE_HEROS = new ConcurrentHashMap<>();
	private static final Map<Integer, StatSet> HERO_COUNTS = new ConcurrentHashMap<>();
	private static final Map<Integer, List<StatSet>> HERO_FIGHTS = new ConcurrentHashMap<>();
	private static final Map<Integer, List<StatSet>> HERO_DIARY = new ConcurrentHashMap<>();
	private static final Map<Integer, String> HERO_MESSAGE = new ConcurrentHashMap<>();
	
	public static final String COUNT = "count";
	public static final String PLAYED = "played";
	public static final String CLAIMED = "claimed";
	public static final String CLAN_NAME = "clan_name";
	public static final String CLAN_CREST = "clan_crest";
	public static final String ALLY_NAME = "ally_name";
	public static final String ALLY_CREST = "ally_crest";
	
	public static final int ACTION_RAID_KILLED = 1;
	public static final int ACTION_HERO_GAINED = 2;
	public static final int ACTION_CASTLE_TAKEN = 3;
	
	protected Hero()
	{
		if (OlympiadConfig.OLYMPIAD_ENABLED)
		{
			init();
		}
	}
	
	private void init()
	{
		HEROES.clear();
		COMPLETE_HEROS.clear();
		HERO_COUNTS.clear();
		HERO_FIGHTS.clear();
		HERO_DIARY.clear();
		HERO_MESSAGE.clear();
		
		try (Connection con = DatabaseFactory.getConnection();
			Statement s1 = con.createStatement();
			ResultSet rset = s1.executeQuery(GET_HEROES);
			PreparedStatement ps = con.prepareStatement(GET_CLAN_ALLY);
			Statement s2 = con.createStatement();
			ResultSet rset2 = s2.executeQuery(GET_ALL_HEROES))
		{
			while (rset.next())
			{
				final StatSet hero = new StatSet();
				final int charId = rset.getInt(Olympiad.CHAR_ID);
				hero.set(Olympiad.CHAR_NAME, rset.getString(Olympiad.CHAR_NAME));
				hero.set(Olympiad.CLASS_ID, rset.getInt(Olympiad.CLASS_ID));
				hero.set(COUNT, rset.getInt(COUNT));
				hero.set(PLAYED, rset.getInt(PLAYED));
				hero.set(CLAIMED, Boolean.parseBoolean(rset.getString(CLAIMED)));
				loadFights(charId);
				loadDiary(charId);
				loadMessage(charId);
				processHeros(ps, charId, hero);
				HEROES.put(charId, hero);
			}
			
			while (rset2.next())
			{
				final StatSet hero = new StatSet();
				final int charId = rset2.getInt(Olympiad.CHAR_ID);
				hero.set(Olympiad.CHAR_NAME, rset2.getString(Olympiad.CHAR_NAME));
				hero.set(Olympiad.CLASS_ID, rset2.getInt(Olympiad.CLASS_ID));
				hero.set(COUNT, rset2.getInt(COUNT));
				hero.set(PLAYED, rset2.getInt(PLAYED));
				hero.set(CLAIMED, Boolean.parseBoolean(rset2.getString(CLAIMED)));
				processHeros(ps, charId, hero);
				COMPLETE_HEROS.put(charId, hero);
			}
		}
		catch (SQLException e)
		{
			LOGGER.warning("Hero System: Could not load Heroes: " + e.getMessage());
		}
		
		LOGGER.info("Hero System: Loaded " + HEROES.size() + " Heroes.");
		LOGGER.info("Hero System: Loaded " + COMPLETE_HEROS.size() + " all time Heroes.");
	}
	
	private void processHeros(PreparedStatement ps, int charId, StatSet hero) throws SQLException
	{
		ps.setInt(1, charId);
		try (ResultSet rs = ps.executeQuery())
		{
			if (rs.next())
			{
				final int clanId = rs.getInt("clanid");
				final int allyId = rs.getInt("allyId");
				String clanName = "";
				String allyName = "";
				int clanCrest = 0;
				int allyCrest = 0;
				if (clanId > 0)
				{
					clanName = ClanTable.getInstance().getClan(clanId).getName();
					clanCrest = ClanTable.getInstance().getClan(clanId).getCrestId();
					if (allyId > 0)
					{
						allyName = ClanTable.getInstance().getClan(clanId).getAllyName();
						allyCrest = ClanTable.getInstance().getClan(clanId).getAllyCrestId();
					}
				}
				
				hero.set(CLAN_CREST, clanCrest);
				hero.set(CLAN_NAME, clanName);
				hero.set(ALLY_CREST, allyCrest);
				hero.set(ALLY_NAME, allyName);
			}
			
			ps.clearParameters();
		}
	}
	
	private String calcFightTime(long fightTimeValue)
	{
		final String format = String.format("%%0%dd", 2);
		final long fightTime = fightTimeValue / 1000;
		return String.format(format, (fightTime % 3600) / 60) + ":" + String.format(format, fightTime % 60);
	}
	
	/**
	 * Restore hero message from Db.
	 * @param charId
	 */
	public void loadMessage(int charId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT message FROM heroes WHERE charId=?"))
		{
			ps.setInt(1, charId);
			try (ResultSet rset = ps.executeQuery())
			{
				if (rset.next())
				{
					HERO_MESSAGE.put(charId, rset.getString("message"));
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.warning("Hero System: Could not load Hero Message for CharId: " + charId + ": " + e.getMessage());
		}
	}
	
	public void loadDiary(int charId)
	{
		final List<StatSet> diary = new ArrayList<>();
		int diaryentries = 0;
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM  heroes_diary WHERE charId=? ORDER BY time ASC"))
		{
			ps.setInt(1, charId);
			try (ResultSet rset = ps.executeQuery())
			{
				while (rset.next())
				{
					final StatSet diaryEntry = new StatSet();
					final long time = rset.getLong("time");
					final int action = rset.getInt("action");
					final int param = rset.getInt("param");
					final String date = (new SimpleDateFormat("yyyy-MM-dd HH")).format(new Date(time));
					diaryEntry.set("date", date);
					if (action == ACTION_RAID_KILLED)
					{
						final NpcTemplate template = NpcData.getInstance().getTemplate(param);
						if (template != null)
						{
							diaryEntry.set("action", template.getName() + " was defeated");
						}
					}
					else if (action == ACTION_HERO_GAINED)
					{
						diaryEntry.set("action", "Gained Hero status");
					}
					else if (action == ACTION_CASTLE_TAKEN)
					{
						final Castle castle = CastleManager.getInstance().getCastleById(param);
						if (castle != null)
						{
							diaryEntry.set("action", castle.getName() + " Castle was successfuly taken");
						}
					}
					
					diary.add(diaryEntry);
					diaryentries++;
				}
			}
			
			HERO_DIARY.put(charId, diary);
			
			LOGGER.info("Hero System: Loaded " + diaryentries + " diary entries for Hero: " + CharInfoTable.getInstance().getNameById(charId));
		}
		catch (SQLException e)
		{
			LOGGER.warning("Hero System: Could not load Hero Diary for CharId: " + charId + ": " + e.getMessage());
		}
	}
	
	public void loadFights(int charId)
	{
		final List<StatSet> fights = new ArrayList<>();
		final StatSet heroCountData = new StatSet();
		final Calendar data = Calendar.getInstance();
		data.set(Calendar.DAY_OF_MONTH, 1);
		data.set(Calendar.HOUR_OF_DAY, 0);
		data.set(Calendar.MINUTE, 0);
		data.set(Calendar.MILLISECOND, 0);
		
		final long from = data.getTimeInMillis();
		int numberOfFights = 0;
		int victories = 0;
		int losses = 0;
		int draws = 0;
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM olympiad_fights WHERE (charOneId=? OR charTwoId=?) AND start<? ORDER BY start ASC"))
		{
			ps.setInt(1, charId);
			ps.setInt(2, charId);
			ps.setLong(3, from);
			try (ResultSet rset = ps.executeQuery())
			{
				int charOneId;
				int charOneClass;
				int charTwoId;
				int charTwoClass;
				int winner;
				long start;
				long time;
				int classed;
				while (rset.next())
				{
					charOneId = rset.getInt("charOneId");
					charOneClass = rset.getInt("charOneClass");
					charTwoId = rset.getInt("charTwoId");
					charTwoClass = rset.getInt("charTwoClass");
					winner = rset.getInt("winner");
					start = rset.getLong("start");
					time = rset.getLong("time");
					classed = rset.getInt("classed");
					if (charId == charOneId)
					{
						final String name = CharInfoTable.getInstance().getNameById(charTwoId);
						final String cls = ClassListData.getInstance().getClass(charTwoClass).getClassName();
						if ((name != null) && (cls != null))
						{
							final StatSet fight = new StatSet();
							fight.set("oponent", name);
							fight.set("oponentclass", cls);
							fight.set("time", calcFightTime(time));
							final String date = (new SimpleDateFormat("yyyy-MM-dd HH:mm")).format(new Date(start));
							fight.set("start", date);
							fight.set("classed", classed);
							if (winner == 1)
							{
								fight.set("result", "<font color=\"00ff00\">victory</font>");
								victories++;
							}
							else if (winner == 2)
							{
								fight.set("result", "<font color=\"ff0000\">loss</font>");
								losses++;
							}
							else if (winner == 0)
							{
								fight.set("result", "<font color=\"ffff00\">draw</font>");
								draws++;
							}
							
							fights.add(fight);
							numberOfFights++;
						}
					}
					else if (charId == charTwoId)
					{
						final String name = CharInfoTable.getInstance().getNameById(charOneId);
						final String cls = ClassListData.getInstance().getClass(charOneClass).getClassName();
						if ((name != null) && (cls != null))
						{
							final StatSet fight = new StatSet();
							fight.set("oponent", name);
							fight.set("oponentclass", cls);
							fight.set("time", calcFightTime(time));
							final String date = (new SimpleDateFormat("yyyy-MM-dd HH:mm")).format(new Date(start));
							fight.set("start", date);
							fight.set("classed", classed);
							if (winner == 1)
							{
								fight.set("result", "<font color=\"ff0000\">loss</font>");
								losses++;
							}
							else if (winner == 2)
							{
								fight.set("result", "<font color=\"00ff00\">victory</font>");
								victories++;
							}
							else if (winner == 0)
							{
								fight.set("result", "<font color=\"ffff00\">draw</font>");
								draws++;
							}
							
							fights.add(fight);
							numberOfFights++;
						}
					}
				}
			}
			
			heroCountData.set("victory", victories);
			heroCountData.set("draw", draws);
			heroCountData.set("loss", losses);
			HERO_COUNTS.put(charId, heroCountData);
			HERO_FIGHTS.put(charId, fights);
			
			LOGGER.info("Hero System: Loaded " + numberOfFights + " fights for Hero: " + CharInfoTable.getInstance().getNameById(charId));
		}
		catch (SQLException e)
		{
			LOGGER.warning("Hero System: Could not load Hero fights history for CharId: " + charId + ": " + e);
		}
	}
	
	public Map<Integer, StatSet> getHeroes()
	{
		return HEROES;
	}
	
	public int getHeroByClass(int classid)
	{
		for (Entry<Integer, StatSet> e : HEROES.entrySet())
		{
			if (e.getValue().getInt(Olympiad.CLASS_ID) == classid)
			{
				return e.getKey();
			}
		}
		
		return 0;
	}
	
	public void resetData()
	{
		HERO_DIARY.clear();
		HERO_FIGHTS.clear();
		HERO_COUNTS.clear();
		HERO_MESSAGE.clear();
	}
	
	public void showHeroDiary(Player player, int heroclass, int charid, int page)
	{
		final int perpage = 10;
		final List<StatSet> mainList = HERO_DIARY.get(charid);
		if (mainList != null)
		{
			final NpcHtmlMessage diaryReply = new NpcHtmlMessage();
			final String htmContent = HtmCache.getInstance().getHtm(player, "data/html/olympiad/herodiary.htm");
			final String heroMessage = HERO_MESSAGE.get(charid);
			if ((htmContent != null) && (heroMessage != null))
			{
				diaryReply.setHtml(htmContent);
				diaryReply.replace("%heroname%", CharInfoTable.getInstance().getNameById(charid));
				diaryReply.replace("%message%", heroMessage);
				diaryReply.disableValidation();
				
				if (!mainList.isEmpty())
				{
					final List<StatSet> list = new ArrayList<>(mainList);
					Collections.reverse(list);
					
					boolean color = true;
					final StringBuilder fList = new StringBuilder(500);
					int counter = 0;
					int breakat = 0;
					for (int i = (page - 1) * perpage; i < list.size(); i++)
					{
						breakat = i;
						final StatSet diaryEntry = list.get(i);
						fList.append("<tr><td>");
						if (color)
						{
							fList.append("<table width=270 bgcolor=\"131210\">");
						}
						else
						{
							fList.append("<table width=270>");
						}
						fList.append("<tr><td width=270><font color=\"LEVEL\">" + diaryEntry.getString("date") + ":xx</font></td></tr>");
						fList.append("<tr><td width=270>" + diaryEntry.getString("action", "") + "</td></tr>");
						fList.append("<tr><td>&nbsp;</td></tr></table>");
						fList.append("</td></tr>");
						color = !color;
						counter++;
						if (counter >= perpage)
						{
							break;
						}
					}
					
					if (breakat < (list.size() - 1))
					{
						diaryReply.replace("%buttprev%", "<button value=\"Prev\" action=\"bypass _diary?class=" + heroclass + "&page=" + (page + 1) + "\" width=65 height=21 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\">");
					}
					else
					{
						diaryReply.replace("%buttprev%", "");
					}
					
					if (page > 1)
					{
						diaryReply.replace("%buttnext%", "<button value=\"Next\" action=\"bypass _diary?class=" + heroclass + "&page=" + (page - 1) + "\" width=65 height=21 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\">");
					}
					else
					{
						diaryReply.replace("%buttnext%", "");
					}
					
					diaryReply.replace("%list%", fList.toString());
				}
				else
				{
					diaryReply.replace("%list%", "");
					diaryReply.replace("%buttprev%", "");
					diaryReply.replace("%buttnext%", "");
				}
				
				player.sendPacket(diaryReply);
			}
		}
	}
	
	public void showHeroFights(Player player, int heroclass, int charid, int page)
	{
		final int perpage = 20;
		int win = 0;
		int loss = 0;
		int draw = 0;
		
		final List<StatSet> heroFights = HERO_FIGHTS.get(charid);
		if (heroFights != null)
		{
			final NpcHtmlMessage fightReply = new NpcHtmlMessage();
			final String htmContent = HtmCache.getInstance().getHtm(player, "data/html/olympiad/herohistory.htm");
			if (htmContent != null)
			{
				fightReply.setHtml(htmContent);
				fightReply.replace("%heroname%", CharInfoTable.getInstance().getNameById(charid));
				if (!heroFights.isEmpty())
				{
					final StatSet heroCount = HERO_COUNTS.get(charid);
					if (heroCount != null)
					{
						win = heroCount.getInt("victory");
						loss = heroCount.getInt("loss");
						draw = heroCount.getInt("draw");
					}
					
					boolean color = true;
					final StringBuilder fList = new StringBuilder(500);
					int counter = 0;
					int breakat = 0;
					for (int i = (page - 1) * perpage; i < heroFights.size(); i++)
					{
						breakat = i;
						final StatSet fight = heroFights.get(i);
						fList.append("<tr><td>");
						if (color)
						{
							fList.append("<table width=270 bgcolor=\"131210\">");
						}
						else
						{
							fList.append("<table width=270>");
						}
						fList.append("<tr><td width=220><font color=\"LEVEL\">" + fight.getString("start") + "</font>&nbsp;&nbsp;" + fight.getString("result") + "</td><td width=50 align=right>" + (fight.getInt("classed") > 0 ? "<font color=\"FFFF99\">cls</font>" : "<font color=\"999999\">non-cls<font>") + "</td></tr>");
						fList.append("<tr><td width=220>vs " + fight.getString("oponent") + " (" + fight.getString("oponentclass") + ")</td><td width=50 align=right>(" + fight.getString("time") + ")</td></tr>");
						fList.append("<tr><td colspan=2>&nbsp;</td></tr></table>");
						fList.append("</td></tr>");
						color = !color;
						counter++;
						if (counter >= perpage)
						{
							break;
						}
					}
					
					if (breakat < (heroFights.size() - 1))
					{
						fightReply.replace("%buttprev%", "<button value=\"Prev\" action=\"bypass _match?class=" + heroclass + "&page=" + (page + 1) + "\" width=65 height=21 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\">");
					}
					else
					{
						fightReply.replace("%buttprev%", "");
					}
					
					if (page > 1)
					{
						fightReply.replace("%buttnext%", "<button value=\"Next\" action=\"bypass _match?class=" + heroclass + "&page=" + (page - 1) + "\" width=65 height=21 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\">");
					}
					else
					{
						fightReply.replace("%buttnext%", "");
					}
					
					fightReply.replace("%list%", fList.toString());
				}
				else
				{
					fightReply.replace("%list%", "");
					fightReply.replace("%buttprev%", "");
					fightReply.replace("%buttnext%", "");
				}
				
				fightReply.replace("%win%", String.valueOf(win));
				fightReply.replace("%draw%", String.valueOf(draw));
				fightReply.replace("%loos%", String.valueOf(loss));
				player.sendPacket(fightReply);
			}
		}
	}
	
	public synchronized void computeNewHeroes(List<StatSet> newHeroes)
	{
		updateHeroes(true);
		
		for (Integer objectId : HEROES.keySet())
		{
			final Player player = World.getInstance().getPlayer(objectId);
			if (player == null)
			{
				continue;
			}
			
			player.setHero(false);
			
			for (int i = 0; i < Inventory.PAPERDOLL_TOTALSLOTS; i++)
			{
				final Item equippedItem = player.getInventory().getPaperdollItem(i);
				if ((equippedItem != null) && equippedItem.isHeroItem())
				{
					player.getInventory().unEquipItemInSlot(i);
				}
			}
			
			final InventoryUpdate iu = new InventoryUpdate();
			for (Item item : player.getInventory().getAvailableItems(false, false, false))
			{
				if ((item != null) && item.isHeroItem())
				{
					player.destroyItem(ItemProcessType.DESTROY, item, null, true);
					iu.addRemovedItem(item);
				}
			}
			
			if (!iu.getItems().isEmpty())
			{
				player.sendInventoryUpdate(iu);
			}
			
			player.broadcastUserInfo();
		}
		
		deleteItemsInDb();
		HEROES.clear();
		
		if (newHeroes.isEmpty())
		{
			return;
		}
		
		for (StatSet hero : newHeroes)
		{
			final int charId = hero.getInt(Olympiad.CHAR_ID);
			if (COMPLETE_HEROS.containsKey(charId))
			{
				final StatSet oldHero = COMPLETE_HEROS.get(charId);
				final int count = oldHero.getInt(COUNT);
				oldHero.set(COUNT, count + 1);
				oldHero.set(PLAYED, 1);
				oldHero.set(CLAIMED, false);
				HEROES.put(charId, oldHero);
			}
			else
			{
				final StatSet newHero = new StatSet();
				newHero.set(Olympiad.CHAR_NAME, hero.getString(Olympiad.CHAR_NAME));
				newHero.set(Olympiad.CLASS_ID, hero.getInt(Olympiad.CLASS_ID));
				newHero.set(COUNT, 1);
				newHero.set(PLAYED, 1);
				newHero.set(CLAIMED, false);
				HEROES.put(charId, newHero);
			}
		}
		
		updateHeroes(false);
	}
	
	public void updateHeroes(boolean setDefault)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			if (setDefault)
			{
				try (Statement s = con.createStatement())
				{
					s.executeUpdate(UPDATE_ALL);
				}
			}
			else
			{
				StatSet hero;
				int heroId;
				for (Entry<Integer, StatSet> entry : HEROES.entrySet())
				{
					hero = entry.getValue();
					heroId = entry.getKey();
					if (!COMPLETE_HEROS.containsKey(heroId))
					{
						try (PreparedStatement insert = con.prepareStatement(INSERT_HERO))
						{
							insert.setInt(1, heroId);
							insert.setInt(2, hero.getInt(Olympiad.CLASS_ID));
							insert.setInt(3, hero.getInt(COUNT, 0));
							insert.setInt(4, hero.getInt(PLAYED, 0));
							insert.setString(5, String.valueOf(hero.getBoolean(CLAIMED, false)));
							insert.execute();
							insert.close();
						}
						
						try (PreparedStatement statement = con.prepareStatement(GET_CLAN_ALLY))
						{
							statement.setInt(1, heroId);
							try (ResultSet rset = statement.executeQuery())
							{
								if (rset.next())
								{
									final int clanId = rset.getInt("clanid");
									final int allyId = rset.getInt("allyId");
									String clanName = "";
									String allyName = "";
									int clanCrest = 0;
									int allyCrest = 0;
									if (clanId > 0)
									{
										clanName = ClanTable.getInstance().getClan(clanId).getName();
										clanCrest = ClanTable.getInstance().getClan(clanId).getCrestId();
										if (allyId > 0)
										{
											allyName = ClanTable.getInstance().getClan(clanId).getAllyName();
											allyCrest = ClanTable.getInstance().getClan(clanId).getAllyCrestId();
										}
									}
									
									hero.set(CLAN_CREST, clanCrest);
									hero.set(CLAN_NAME, clanName);
									hero.set(ALLY_CREST, allyCrest);
									hero.set(ALLY_NAME, allyName);
								}
							}
						}
						
						HEROES.put(heroId, hero);
						COMPLETE_HEROS.put(heroId, hero);
					}
					else
					{
						try (PreparedStatement statement = con.prepareStatement(UPDATE_HERO))
						{
							statement.setInt(1, hero.getInt(Olympiad.CLASS_ID));
							statement.setInt(2, hero.getInt(COUNT, 0));
							statement.setInt(3, hero.getInt(PLAYED, 0));
							statement.setString(4, String.valueOf(hero.getBoolean(CLAIMED, false)));
							statement.setInt(5, heroId);
							statement.execute();
						}
					}
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.warning("Hero System: Could not update Heroes: " + e.getMessage());
		}
	}
	
	public void setHeroGained(int charId)
	{
		setDiaryData(charId, ACTION_HERO_GAINED, 0);
	}
	
	public void setRBkilled(int charId, int npcId)
	{
		setDiaryData(charId, ACTION_RAID_KILLED, npcId);
		
		final NpcTemplate template = NpcData.getInstance().getTemplate(npcId);
		final List<StatSet> list = HERO_DIARY.get(charId);
		if ((list == null) || (template == null))
		{
			return;
		}
		
		// Prepare new data
		final StatSet diaryEntry = new StatSet();
		final String date = (new SimpleDateFormat("yyyy-MM-dd HH")).format(new Date(System.currentTimeMillis()));
		diaryEntry.set("date", date);
		diaryEntry.set("action", template.getName() + " was defeated");
		
		// Add to old list
		list.add(diaryEntry);
	}
	
	public void setCastleTaken(int charId, int castleId)
	{
		setDiaryData(charId, ACTION_CASTLE_TAKEN, castleId);
		
		final Castle castle = CastleManager.getInstance().getCastleById(castleId);
		final List<StatSet> list = HERO_DIARY.get(charId);
		if ((list == null) || (castle == null))
		{
			return;
		}
		
		// Prepare new data
		final StatSet diaryEntry = new StatSet();
		final String date = (new SimpleDateFormat("yyyy-MM-dd HH")).format(new Date(System.currentTimeMillis()));
		diaryEntry.set("date", date);
		diaryEntry.set("action", castle.getName() + " Castle was successfuly taken");
		
		// Add to old list
		list.add(diaryEntry);
	}
	
	public void setDiaryData(int charId, int action, int param)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("INSERT INTO heroes_diary (charId, time, action, param) values(?,?,?,?)"))
		{
			ps.setInt(1, charId);
			ps.setLong(2, System.currentTimeMillis());
			ps.setInt(3, action);
			ps.setInt(4, param);
			ps.execute();
		}
		catch (SQLException e)
		{
			LOGGER.severe("SQL exception while saving DiaryData: " + e.getMessage());
		}
	}
	
	/**
	 * Set new hero message for hero
	 * @param player the player instance
	 * @param message String to set
	 */
	public void setHeroMessage(Player player, String message)
	{
		HERO_MESSAGE.put(player.getObjectId(), message);
	}
	
	/**
	 * Update hero message in database
	 * @param charId character objid
	 */
	public void saveHeroMessage(int charId)
	{
		if (!HERO_MESSAGE.containsKey(charId))
		{
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE heroes SET message=? WHERE charId=?;"))
		{
			ps.setString(1, HERO_MESSAGE.get(charId));
			ps.setInt(2, charId);
			ps.execute();
		}
		catch (SQLException e)
		{
			LOGGER.severe("SQL exception while saving HeroMessage:" + e.getMessage());
		}
	}
	
	private void deleteItemsInDb()
	{
		try (Connection con = DatabaseFactory.getConnection();
			Statement s = con.createStatement())
		{
			s.executeUpdate(DELETE_ITEMS);
		}
		catch (SQLException e)
		{
			LOGGER.warning("Heroes: " + e.getMessage());
		}
	}
	
	/**
	 * Saving task for {@link Hero}<br>
	 * Save all hero messages to DB.
	 */
	public void shutdown()
	{
		HERO_MESSAGE.keySet().forEach(this::saveHeroMessage);
	}
	
	/**
	 * Verifies if the given object ID belongs to a claimed hero.
	 * @param objectId the player's object ID to verify
	 * @return {@code true} if there are heros and the player is in the list, {@code false} otherwise
	 */
	public boolean isHero(int objectId)
	{
		return HEROES.containsKey(objectId) && HEROES.get(objectId).getBoolean(CLAIMED);
	}
	
	/**
	 * Verifies if the given object ID belongs to an unclaimed hero.
	 * @param objectId the player's object ID to verify
	 * @return {@code true} if player is unclaimed hero
	 */
	public boolean isUnclaimedHero(int objectId)
	{
		return HEROES.containsKey(objectId) && !HEROES.get(objectId).getBoolean(CLAIMED);
	}
	
	/**
	 * Claims the hero status for the given player.
	 * @param player the player to become hero
	 */
	public void claimHero(Player player)
	{
		StatSet hero = HEROES.get(player.getObjectId());
		if (hero == null)
		{
			hero = new StatSet();
			HEROES.put(player.getObjectId(), hero);
		}
		
		hero.set(CLAIMED, true);
		
		final Clan clan = player.getClan();
		if ((clan != null) && (clan.getLevel() >= 5))
		{
			clan.addReputationScore(FeatureConfig.HERO_POINTS);
			
			// Retail system message is bugged?
			// final SystemMessage sm = new SystemMessage(SystemMessageId.CLAN_MEMBER_S1_WAS_NAMED_A_HERO_2S_POINTS_HAVE_BEEN_ADDED_TO_YOUR_CLAN_S_REPUTATION_SCORE);
			// sm.addString(CharInfoTable.getInstance().getNameById(player.getObjectId()));
			// sm.addInt(Config.HERO_POINTS);
			final SystemMessage sm = new SystemMessage(SendMessageLocalisationData.getLocalisation(player, "Clan member " + CharInfoTable.getInstance().getNameById(player.getObjectId()) + " was named a hero. " + FeatureConfig.HERO_POINTS + " points have been added to your clan's reputation score."));
			clan.broadcastToOnlineMembers(sm);
		}
		
		player.setHero(true);
		player.broadcastPacket(new SocialAction(player.getObjectId(), 20016)); // Hero Animation
		player.broadcastUserInfo();
		
		// Set Gained hero and reload data
		setHeroGained(player.getObjectId());
		loadFights(player.getObjectId());
		loadDiary(player.getObjectId());
		HERO_MESSAGE.put(player.getObjectId(), "");
		
		updateHeroes(false);
	}
	
	public static Hero getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final Hero INSTANCE = new Hero();
	}
}
