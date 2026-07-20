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
package conquerablehalls.WildBeastReserve;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.ai.SpecialSiegeGuardAI;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.data.sql.ClanHallTable;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.clan.ClanMember;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.siege.SiegeClan;
import org.l2jmobius.gameserver.model.siege.SiegeClanType;
import org.l2jmobius.gameserver.model.siege.clanhalls.ClanHallSiegeEngine;
import org.l2jmobius.gameserver.model.siege.clanhalls.SiegeStatus;
import org.l2jmobius.gameserver.model.zone.type.ResidenceHallTeleportZone;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * @author LordWinter
 */
public class WildBeastReserve extends ClanHallSiegeEngine
{
	private static final String SQL_LOAD_ATTACKERS = "SELECT * FROM siegable_hall_flagwar_attackers WHERE hall_id = ?";
	private static final String SQL_SAVE_ATTACKER = "INSERT INTO siegable_hall_flagwar_attackers_members VALUES (?,?,?)";
	private static final String SQL_LOAD_MEMEBERS = "SELECT object_id FROM siegable_hall_flagwar_attackers_members WHERE clan_id = ?";
	private static final String SQL_SAVE_CLAN = "INSERT INTO siegable_hall_flagwar_attackers VALUES(?,?,?,?)";
	private static final String SQL_SAVE_NPC = "UPDATE siegable_hall_flagwar_attackers SET npc = ? WHERE clan_id = ?";
	private static final String SQL_CLEAR_CLAN = "DELETE FROM siegable_hall_flagwar_attackers WHERE hall_id = ?";
	private static final String SQL_CLEAR_CLAN_ATTACKERS = "DELETE FROM siegable_hall_flagwar_attackers_members WHERE hall_id = ?";
	
	private static final int ROYAL_FLAG = 35606;
	private static final int FLAG_RED = 35607;
	
	private static final int ALLY_1 = 35618;
	private static final int ALLY_2 = 35619;
	private static final int ALLY_3 = 35620;
	private static final int ALLY_4 = 35621;
	private static final int ALLY_5 = 35622;
	
	private static final int TELEPORT_1 = 35612;
	
	private static final int MESSENGER = 35627;
	
	protected static final int[] OUTTER_DOORS_TO_OPEN =
	{
		21150003,
		21150004
	};
	protected static final int[] INNER_DOORS_TO_OPEN =
	{
		21150001,
		21150002
	};
	private static final Location[] FLAG_COORDS =
	{
		new Location(56963, -92211, -1303, 60611),
		new Location(58090, -91641, -1303, 47274),
		new Location(58908, -92556, -1303, 34450),
		new Location(58336, -93600, -1303, 21100),
		new Location(57152, -93360, -1303, 8400),
		new Location(59116, -93251, -1302, 31000),
		new Location(56432, -92864, -1303, 64000)
	};
	
	private static final ResidenceHallTeleportZone[] TELE_ZONES = new ResidenceHallTeleportZone[6];
	static
	{
		final Collection<ResidenceHallTeleportZone> zoneList = ZoneManager.getInstance().getAllZones(ResidenceHallTeleportZone.class);
		for (ResidenceHallTeleportZone teleZone : zoneList)
		{
			if (teleZone.getResidenceId() != BEAST_FARM)
			{
				continue;
			}
			
			final int id = teleZone.getResidenceZoneId();
			if ((id < 0) || (id >= 6))
			{
				continue;
			}
			
			TELE_ZONES[id] = teleZone;
		}
	}
	
	private static final int QUEST_REWARD = 8293;
	private static final int STONE = 8084;
	
	private static final Location CENTER = new Location(57762, -92696, -1359, 0);
	
	protected static final Map<Integer, ClanData> _data = new HashMap<>();
	private Clan _winner;
	private boolean _firstPhase;
	
	public WildBeastReserve()
	{
		super(BEAST_FARM);
		
		addStartNpc(MESSENGER);
		addFirstTalkId(MESSENGER);
		addTalkId(MESSENGER);
		
		for (int i = 0; i < 6; i++)
		{
			addFirstTalkId(TELEPORT_1 + i);
		}
		
		addKillId(ALLY_1);
		addKillId(ALLY_2);
		addKillId(ALLY_3);
		addKillId(ALLY_4);
		addKillId(ALLY_5);
		
		addSpawnId(ALLY_1);
		addSpawnId(ALLY_2);
		addSpawnId(ALLY_3);
		addSpawnId(ALLY_4);
		addSpawnId(ALLY_5);
		
		_winner = ClanTable.getInstance().getClan(_hall.getOwnerId());
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final Clan clan = player.getClan();
		
		if (event.startsWith("Register"))
		{
			if (!_hall.isRegistering())
			{
				if (_hall.isInSiege())
				{
					htmltext = "35627-02.htm";
				}
				else
				{
					sendRegistrationPageDate(player);
					return null;
				}
			}
			else if ((clan == null) || !player.isClanLeader())
			{
				htmltext = "35627-03.htm";
			}
			else if (getAttackers().size() >= 5)
			{
				htmltext = "35627-04.htm";
			}
			else if (checkIsAttacker(clan))
			{
				htmltext = "35627-05.htm";
			}
			else if (_hall.getOwnerId() == clan.getId())
			{
				htmltext = "35627-06.htm";
			}
			else
			{
				final String[] arg = event.split(" ");
				if (arg.length >= 2)
				{
					if (arg[1].equals("wQuest"))
					{
						if (player.destroyItemByItemId(ItemProcessType.QUEST, QUEST_REWARD, 1, player, true))
						{
							registerClan(clan);
							htmltext = getFlagHtml(_data.get(clan.getId()).flag);
						}
						else
						{
							htmltext = "35627-07.htm";
						}
					}
					else if (arg[1].equals("wFee") && canPayRegistration())
					{
						if (player.reduceAdena(ItemProcessType.FEE, 200000, player, true))
						{
							registerClan(clan);
							htmltext = getFlagHtml(_data.get(clan.getId()).flag);
						}
						else
						{
							htmltext = "35627-08.htm";
						}
					}
				}
			}
		}
		else if (event.startsWith("Select_NPC"))
		{
			if (!player.isClanLeader())
			{
				htmltext = "35627-09.htm";
			}
			else if (!_data.containsKey(clan.getId()))
			{
				htmltext = "35627-10.htm";
			}
			else
			{
				final String[] var = event.split(" ");
				if (var.length >= 2)
				{
					int id = 0;
					try
					{
						id = Integer.parseInt(var[1]);
					}
					catch (Exception e)
					{
						LOGGER.warning(getClass().getSimpleName() + "->select_clan_npc->Wrong mahum warrior id: " + var[1]);
					}
					
					if ((id > 0) && ((htmltext = getAllyHtml(id)) != null))
					{
						_data.get(clan.getId()).npc = id;
						saveNpc(id, clan.getId());
					}
				}
				else
				{
					LOGGER.warning(getClass().getSimpleName() + " Siege: Not enough parameters to save clan npc for clan: " + clan.getName());
				}
			}
		}
		else if (event.startsWith("View"))
		{
			ClanData cd = null;
			if (clan == null)
			{
				htmltext = "35627-10.htm";
			}
			else if ((cd = _data.get(clan.getId())) == null)
			{
				htmltext = "35627-03.htm";
			}
			else if (cd.npc == 0)
			{
				htmltext = "35627-11.htm";
			}
			else
			{
				htmltext = getAllyHtml(cd.npc);
			}
		}
		else if (event.startsWith("RegisterMember"))
		{
			if (clan == null)
			{
				htmltext = "35627-10.htm";
			}
			else if (!_hall.isRegistering())
			{
				htmltext = "35627-02.htm";
			}
			else if (!_data.containsKey(clan.getId()))
			{
				htmltext = "35627-03.htm";
			}
			else if (_data.get(clan.getId()).players.size() >= 18)
			{
				htmltext = "35627-12.htm";
			}
			else
			{
				final ClanData data = _data.get(clan.getId());
				data.players.add(player.getObjectId());
				saveMember(clan.getId(), player.getObjectId());
				if (data.npc == 0)
				{
					htmltext = "35627-11.htm";
				}
				else
				{
					htmltext = "35627-13.htm";
				}
			}
		}
		else if (event.startsWith("Attackers"))
		{
			if (_hall.isRegistering())
			{
				sendRegistrationPageDate(player);
				return null;
			}
			
			htmltext = HtmCache.getInstance().getHtm(player, "data/scripts/conquerablehalls/WildBeastReserve/35627-14.htm");
			int i = 1;
			for (Entry<Integer, ClanData> clanData : _data.entrySet())
			{
				final Clan attacker = ClanTable.getInstance().getClan(clanData.getKey());
				if (attacker == null)
				{
					continue;
				}
				
				htmltext = htmltext.replaceAll("%clan" + i + "%", clan.getName());
				htmltext = htmltext.replaceAll("%clanMem" + i + "%", String.valueOf(clanData.getValue().players.size()));
				i++;
			}
			
			if (_data.size() < 5)
			{
				for (int c = i; c <= 5; c++)
				{
					htmltext = htmltext.replaceAll("%clan" + c + "%", "Empty pos. ");
					htmltext = htmltext.replaceAll("%clanMem" + c + "%", String.valueOf(0));
				}
			}
		}
		else if (event.startsWith("CheckQuest"))
		{
			if ((clan == null) || (clan.getLevel() < 4))
			{
				htmltext = "35627-23.htm";
			}
			else if (!player.isClanLeader())
			{
				htmltext = "35627-24.htm";
			}
			else if ((clan.getHideoutId() > 0) || (clan.getCastleId() > 0))
			{
				htmltext = "35627-25.htm";
			}
			else if (!_hall.isWaitingBattle())
			{
				sendRegistrationPageDate(player);
				return null;
			}
			else if (player.getInventory().getItemByItemId(QUEST_REWARD) != null)
			{
				htmltext = "35627-26.htm";
			}
			else
			{
				if (player.getInventory().getInventoryItemCount(STONE, -1) >= 10)
				{
					htmltext = "35627-22a.htm";
				}
				else
				{
					htmltext = "35627-22.htm";
				}
			}
		}
		
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (npc.getId() == MESSENGER)
		{
			if (!checkIsAttacker(player.getClan()))
			{
				final Clan clan = ClanTable.getInstance().getClan(_hall.getOwnerId());
				final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
				html.setFile(player, "data/scripts/conquerablehalls/WildBeastReserve/35627-00.htm");
				html.replace("%clanName%", clan == null ? "no owner" : clan.getName());
				player.sendPacket(html);
			}
			else
			{
				return "35627-01.htm";
			}
		}
		else
		{
			final int index = npc.getId() - TELEPORT_1;
			if ((index == 0) && _firstPhase)
			{
				return "35612-00.htm";
			}
			
			TELE_ZONES[index].checkTeleportTask();
			return "35612-01.htm";
		}
		
		return "";
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (_hall.isInSiege())
		{
			final int npcId = npc.getId();
			for (int keys : _data.keySet())
			{
				if (_data.get(keys).npc == npcId)
				{
					removeParticipant(keys, true);
				}
			}
			
			synchronized (this)
			{
				final List<Integer> clanIds = new ArrayList<>(_data.keySet());
				if (_firstPhase)
				{
					if (((clanIds.size() == 1) && (_hall.getOwnerId() <= 0)) || (_data.get(clanIds.get(0)).npc == 0))
					{
						_missionAccomplished = true;
						cancelSiegeTask();
						endSiege();
					}
					else if ((_data.size() == 2) && (_hall.getOwnerId() > 0))
					{
						cancelSiegeTask();
						_firstPhase = false;
						_hall.getSiegeZone().setActive(false);
						for (int doorId : INNER_DOORS_TO_OPEN)
						{
							_hall.openCloseDoor(doorId, true);
						}
						
						for (ClanData data : _data.values())
						{
							doUnSpawns(data);
						}
						
						ThreadPool.schedule(() ->
						{
							for (int doorId : INNER_DOORS_TO_OPEN)
							{
								_hall.openCloseDoor(doorId, false);
							}
							
							for (Entry<Integer, ClanData> e : _data.entrySet())
							{
								doSpawns(e.getKey(), e.getValue());
							}
							
							_hall.getSiegeZone().setActive(true);
						}, 300000);
					}
				}
				else
				{
					_missionAccomplished = true;
					_winner = ClanTable.getInstance().getClan(clanIds.get(0));
					removeParticipant(clanIds.get(0), false);
					endSiege();
				}
			}
		}
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		npc.getAI().setIntention(Intention.MOVE_TO, CENTER, 0);
	}
	
	@Override
	public Clan getWinner()
	{
		return _winner;
	}
	
	@Override
	public void prepareOwner()
	{
		if (_hall.getOwnerId() > 0)
		{
			registerClan(ClanTable.getInstance().getClan(_hall.getOwnerId()));
		}
		
		_hall.banishForeigners();
		final SystemMessage msg = new SystemMessage(SystemMessageId.THE_REGISTRATION_TERM_FOR_S1_HAS_ENDED);
		msg.addString(ClanHallTable.getInstance().getClanHallById(_hall.getId()).getName());
		Broadcast.toAllOnlinePlayers(msg);
		_hall.updateSiegeStatus(SiegeStatus.WAITING_BATTLE);
		
		_siegeTask = ThreadPool.schedule(new SiegeStarts(), 3600000);
	}
	
	@Override
	public void startSiege()
	{
		if (getAttackers().size() < 2)
		{
			onSiegeEnds();
			getAttackers().clear();
			_hall.updateNextSiege();
			final SystemMessage sm = new SystemMessage(SystemMessageId.THE_SIEGE_OF_S1_HAS_BEEN_CANCELED_DUE_TO_LACK_OF_INTEREST);
			sm.addString(ClanHallTable.getInstance().getClanHallById(_hall.getId()).getName());
			Broadcast.toAllOnlinePlayers(sm);
			return;
		}
		
		for (int door : OUTTER_DOORS_TO_OPEN)
		{
			_hall.openCloseDoor(door, true);
		}
		
		if (_hall.getOwnerId() > 0)
		{
			final Clan owner = ClanTable.getInstance().getClan(_hall.getOwnerId());
			final Location loc = _hall.getZone().getSpawns().get(0);
			for (ClanMember member : owner.getMembers())
			{
				if (member != null)
				{
					final Player player = member.getPlayer();
					if ((player != null) && player.isOnline())
					{
						player.teleToLocation(loc, false);
					}
				}
			}
		}
		
		ThreadPool.schedule(() ->
		{
			for (int door : OUTTER_DOORS_TO_OPEN)
			{
				_hall.openCloseDoor(door, false);
			}
			
			_hall.getZone().banishNonSiegeParticipants();
			
			startSiege();
		}, 300000);
	}
	
	@Override
	public void onSiegeStarts()
	{
		for (Entry<Integer, ClanData> clan : _data.entrySet())
		{
			try
			{
				final ClanData data = clan.getValue();
				doSpawns(clan.getKey(), data);
				fillPlayerList(data);
			}
			catch (Exception e)
			{
				endSiege();
				LOGGER.warning(getClass().getSimpleName() + ": Problems in siege initialization! " + e.getMessage());
			}
		}
	}
	
	@Override
	public void endSiege()
	{
		if (_hall.getOwnerId() > 0)
		{
			final Clan clan = ClanTable.getInstance().getClan(_hall.getOwnerId());
			clan.setHideoutId(0);
			_hall.free();
		}
		
		super.endSiege();
	}
	
	@Override
	public void onSiegeEnds()
	{
		if (_data.size() > 0)
		{
			for (int clanId : _data.keySet())
			{
				if (_hall.getOwnerId() == clanId)
				{
					removeParticipant(clanId, false);
				}
				else
				{
					removeParticipant(clanId, true);
				}
			}
		}
		
		clearTables();
	}
	
	@Override
	public final Location getInnerSpawnLoc(Player player)
	{
		Location loc = null;
		if (player.getId() == _hall.getOwnerId())
		{
			loc = _hall.getZone().getSpawns().get(0);
		}
		else
		{
			final ClanData cd = _data.get(player.getId());
			if (cd != null)
			{
				final int index = cd.flag - FLAG_RED;
				if ((index >= 0) && (index <= 4))
				{
					loc = _hall.getZone().getChallengerSpawns().get(index);
				}
				else
				{
					throw new ArrayIndexOutOfBoundsException();
				}
			}
		}
		
		return loc;
	}
	
	@Override
	public final boolean canPlantFlag()
	{
		return false;
	}
	
	@Override
	public final boolean doorIsAutoAttackable()
	{
		return false;
	}
	
	void doSpawns(int clanId, ClanData data)
	{
		try
		{
			final NpcTemplate mahumTemplate = NpcData.getInstance().getTemplate(data.npc);
			final NpcTemplate flagTemplate = NpcData.getInstance().getTemplate(data.flag);
			if (flagTemplate == null)
			{
				LOGGER.warning(getClass().getSimpleName() + ": Flag NpcTemplate[" + data.flag + "] does not exist!");
				throw new NullPointerException();
			}
			else if (mahumTemplate == null)
			{
				LOGGER.warning(getClass().getSimpleName() + ": Ally NpcTemplate[" + data.npc + "] does not exist!");
				throw new NullPointerException();
			}
			
			int index = 0;
			if (_firstPhase)
			{
				index = data.flag - FLAG_RED;
			}
			else
			{
				index = clanId == _hall.getOwnerId() ? 5 : 6;
			}
			
			final Location loc = FLAG_COORDS[index];
			
			data.flagInstance = addSpawn(flagTemplate.getId(), loc);
			data.flagInstance.getSpawn().setRespawnDelay(10000);
			data.flagInstance.getSpawn().startRespawn();
			
			data.warrior = addSpawn(mahumTemplate.getId(), loc);
			data.warrior.getSpawn().setRespawnDelay(10000);
			data.warrior.getSpawn().startRespawn();
			
			((SpecialSiegeGuardAI) data.warrior.getSpawn().getLastSpawn().getAI()).getAlly().addAll(data.players);
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Could not make clan spawns: " + e.getMessage());
		}
	}
	
	private void fillPlayerList(ClanData data)
	{
		for (int objId : data.players)
		{
			final Player player = World.getInstance().getPlayer(objId);
			if (player != null)
			{
				data.playersInstance.add(player);
			}
		}
	}
	
	private void registerClan(Clan clan)
	{
		final int clanId = clan.getId();
		final SiegeClan sc = new SiegeClan(clanId, SiegeClanType.ATTACKER);
		getAttackers().put(clanId, sc);
		
		final ClanData data = new ClanData();
		data.flag = ROYAL_FLAG + _data.size();
		data.players.add(clan.getLeaderId());
		_data.put(clanId, data);
		
		saveClan(clanId, data.flag);
		saveMember(clanId, clan.getLeaderId());
	}
	
	private void doUnSpawns(ClanData data)
	{
		if (data.flagInstance != null)
		{
			data.flagInstance.getSpawn().stopRespawn();
			data.flagInstance.getSpawn().getLastSpawn().deleteMe();
		}
		
		if (data.warrior != null)
		{
			data.warrior.getSpawn().stopRespawn();
			data.warrior.getSpawn().getLastSpawn().deleteMe();
		}
	}
	
	private void removeParticipant(int clanId, boolean teleport)
	{
		final ClanData dat = _data.remove(clanId);
		if (dat != null)
		{
			if (dat.flagInstance != null)
			{
				dat.flagInstance.getSpawn().stopRespawn();
				if (dat.flagInstance.getSpawn().getLastSpawn() != null)
				{
					dat.flagInstance.getSpawn().getLastSpawn().deleteMe();
				}
			}
			
			if (dat.warrior != null)
			{
				dat.warrior.getSpawn().stopRespawn();
				if (dat.warrior.getSpawn().getLastSpawn() != null)
				{
					dat.warrior.getSpawn().getLastSpawn().deleteMe();
				}
			}
			
			dat.players.clear();
			
			if (teleport)
			{
				for (Player player : dat.playersInstance)
				{
					if (player != null)
					{
						player.teleToLocation(TeleportWhereType.TOWN);
					}
				}
			}
			
			dat.playersInstance.clear();
		}
	}
	
	public boolean canPayRegistration()
	{
		return true;
	}
	
	private void sendRegistrationPageDate(Player player)
	{
		final NpcHtmlMessage msg = new NpcHtmlMessage(5);
		msg.setFile(player, "data/scripts/conquerablehalls/WildBeastReserve/35627-15.htm");
		msg.replace("%nextSiege%", _hall.getSiegeDate().getTime().toString());
		player.sendPacket(msg);
	}
	
	public String getFlagHtml(int flag)
	{
		String result = "35627-15a.htm";
		switch (flag)
		{
			case 35607:
			{
				result = "35627-16.htm";
				break;
			}
			case 35608:
			{
				result = "35627-17.htm";
				break;
			}
			case 35609:
			{
				result = "35627-18.htm";
				break;
			}
			case 35610:
			{
				result = "35627-19.htm";
				break;
			}
			case 35611:
			{
				result = "35627-20.htm";
				break;
			}
		}
		
		return result;
	}
	
	public String getAllyHtml(int ally)
	{
		String result = null;
		switch (ally)
		{
			case 35618:
			{
				result = "35627-16a.htm";
				break;
			}
			case 35619:
			{
				result = "35627-17a.htm";
				break;
			}
			case 35620:
			{
				result = "35627-18a.htm";
				break;
			}
			case 35621:
			{
				result = "35627-19a.htm";
				break;
			}
			case 35622:
			{
				result = "35627-20a.htm";
				break;
			}
		}
		
		return result;
	}
	
	@Override
	public void loadAttackers()
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			final PreparedStatement statement = con.prepareStatement(SQL_LOAD_ATTACKERS);
			statement.setInt(1, _hall.getId());
			final ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				final int clanId = rset.getInt("clan_id");
				if (ClanTable.getInstance().getClan(clanId) == null)
				{
					LOGGER.warning(getClass().getSimpleName() + ": Loaded an unexistent clan as attacker! Clan Id: " + clanId);
					continue;
				}
				
				final int flag = rset.getInt("flag");
				final int npc = rset.getInt("npc");
				final SiegeClan sc = new SiegeClan(clanId, SiegeClanType.ATTACKER);
				getAttackers().put(clanId, sc);
				
				final ClanData data = new ClanData();
				data.flag = flag;
				data.npc = npc;
				_data.put(clanId, data);
				
				loadAttackerMembers(clanId);
			}
			
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ".loadAttackers()->" + e.getMessage());
		}
	}
	
	private void loadAttackerMembers(int clanId)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			final ArrayList<Integer> listInstance = _data.get(clanId).players;
			if (listInstance == null)
			{
				LOGGER.warning(getClass().getSimpleName() + ": Tried to load unregistered clan: " + clanId + "[clan Id]");
				return;
			}
			
			final PreparedStatement statement = con.prepareStatement(SQL_LOAD_MEMEBERS);
			statement.setInt(1, clanId);
			final ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				listInstance.add(rset.getInt("object_id"));
			}
			
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ".loadAttackerMembers()->" + e.getMessage());
		}
	}
	
	private void saveClan(int clanId, int flag)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			final PreparedStatement statement = con.prepareStatement(SQL_SAVE_CLAN);
			statement.setInt(1, _hall.getId());
			statement.setInt(2, flag);
			statement.setInt(3, 0);
			statement.setInt(4, clanId);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ".saveClan()->" + e.getMessage());
		}
	}
	
	private void saveNpc(int npc, int clanId)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			final PreparedStatement statement = con.prepareStatement(SQL_SAVE_NPC);
			statement.setInt(1, npc);
			statement.setInt(2, clanId);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ".saveNpc()->" + e.getMessage());
		}
	}
	
	private void saveMember(int clanId, int objectId)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			final PreparedStatement statement = con.prepareStatement(SQL_SAVE_ATTACKER);
			statement.setInt(1, _hall.getId());
			statement.setInt(2, clanId);
			statement.setInt(3, objectId);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ".saveMember()->" + e.getMessage());
		}
	}
	
	private void clearTables()
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			final PreparedStatement stat1 = con.prepareStatement(SQL_CLEAR_CLAN);
			stat1.setInt(1, _hall.getId());
			stat1.execute();
			stat1.close();
			
			final PreparedStatement stat2 = con.prepareStatement(SQL_CLEAR_CLAN_ATTACKERS);
			stat2.setInt(1, _hall.getId());
			stat2.execute();
			stat2.close();
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ".clearTables()->" + e.getMessage());
		}
	}
	
	protected class ClanData
	{
		int flag = 0;
		int npc = 0;
		ArrayList<Integer> players = new ArrayList<>(18);
		ArrayList<Player> playersInstance = new ArrayList<>(18);
		Npc warrior = null;
		Npc flagInstance = null;
	}
	
	public static void main(String[] args)
	{
		new WildBeastReserve();
	}
}
