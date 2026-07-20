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
package conquerablehalls.RainbowSpringsChateau;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.ConquerableHallSiegeConfig;
import org.l2jmobius.gameserver.data.sql.ClanHallTable;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.xml.MapRegionData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.siege.SiegeClan;
import org.l2jmobius.gameserver.model.siege.SiegeClanType;
import org.l2jmobius.gameserver.model.siege.clanhalls.ClanHallSiegeEngine;
import org.l2jmobius.gameserver.model.siege.clanhalls.SiegeStatus;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.ArrayUtil;
import org.l2jmobius.gameserver.util.Broadcast;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * @author LordWinter
 */
public class RainbowSpringsChateau extends ClanHallSiegeEngine
{
	private static final SimpleDateFormat SIMPLE_FORMAT = new SimpleDateFormat("HH:mm dd.MM.yyyy");
	
	private static final int WAR_DECREES = 8034;
	private static final int RAINBOW_NECTAR = 8030;
	private static final int RAINBOW_MWATER = 8031;
	private static final int RAINBOW_WATER = 8032;
	private static final int RAINBOW_SULFUR = 8033;
	
	private static final int MESSENGER = 35604;
	private static final int CARETAKER = 35603;
	private static final int CHEST = 35593;
	private static final int ENRAGED_YETI = 35592;
	
	protected static final Map<Integer, Integer> _warDecreesCount = new HashMap<>();
	protected static final List<Clan> _acceptedClans = new ArrayList<>();
	protected static ArrayList<Integer> _playersOnArena = new ArrayList<>();
	protected static final List<Npc> chests = new ArrayList<>();
	
	private static final int ITEM_A = 8035;
	private static final int ITEM_B = 8036;
	private static final int ITEM_C = 8037;
	private static final int ITEM_D = 8038;
	private static final int ITEM_E = 8039;
	private static final int ITEM_F = 8040;
	private static final int ITEM_G = 8041;
	private static final int ITEM_H = 8042;
	private static final int ITEM_I = 8043;
	private static final int ITEM_K = 8045;
	private static final int ITEM_L = 8046;
	private static final int ITEM_N = 8047;
	private static final int ITEM_O = 8048;
	private static final int ITEM_P = 8049;
	private static final int ITEM_R = 8050;
	private static final int ITEM_S = 8051;
	private static final int ITEM_T = 8052;
	private static final int ITEM_U = 8053;
	private static final int ITEM_W = 8054;
	private static final int ITEM_Y = 8055;
	
	protected static int _generated;
	protected Future<?> _task = null;
	protected Future<?> _chesttask = null;
	private Clan _winner;
	
	private static class Word
	{
		private final String _name;
		private final int[][] _items;
		
		public Word(String name, int[]... items)
		{
			_name = name;
			_items = items;
		}
		
		public String getName()
		{
			return _name;
		}
		
		public int[][] getItems()
		{
			return _items;
		}
	}
	
	private static final int[] GOURDS =
	{
		35588,
		35589,
		35590,
		35591
	};
	
	private static Npc[] _gourds = new Npc[4];
	private static Npc[] _yetis = new Npc[4];
	protected Npc _chest1;
	protected Npc _chest2;
	protected Npc _chest3;
	protected Npc _chest4;
	
	private static final Skill[] DEBUFFS =
	{
		SkillData.getInstance().getSkill(4991, 1)
	};
	
	private static final int[] YETIS =
	{
		35596,
		35597,
		35598,
		35599
	};
	
	// @formatter:off
	private static final int[][] ARENAS =
	{
		{151562, -127080, -2214},
		{153141, -125335, -2214},
		{153892, -127530, -2214},
		{155657, -125752, -2214},
	};
	
	private static final int[][] YETIS_SPAWN =
	{
		{151560, -127075, -2221},
		{153129, -125337, -2221},
		{153884, -127534, -2221},
		{156657, -125753, -2221},
	};
	
	protected static final int[][] CHESTS_SPAWN =
	{
		{151560, -127075, -2221},
		{153129, -125337, -2221},
		{153884, -127534, -2221},
		{155657, -125753, -2221},
		
	};
	
	protected final int[] arenaChestsCnt =
	{
		0, 0, 0, 0
	};
	
	protected static final Word[] WORLD_LIST =
	{
		new Word("BABYDUCK", new int[] {ITEM_B, 2}, new int[] {ITEM_A, 1}, new int[] {ITEM_Y, 1}, new int[] {ITEM_D, 1}, new int[] {ITEM_U, 1}, new int[] {ITEM_C, 1}, new int[] {ITEM_K, 1}),
		new Word("ALBATROS", new int[] {ITEM_A, 2}, new int[] {ITEM_L, 1}, new int[] {ITEM_B, 1}, new int[] {ITEM_T, 1}, new int[] {ITEM_R, 1}, new int[] {ITEM_O, 1}, new int[] {ITEM_S, 1}),
		new Word("PELICAN", new int[] {ITEM_P, 1}, new int[] {ITEM_E, 1}, new int[] {ITEM_L, 1}, new int[] {ITEM_I, 1}, new int[] {ITEM_C, 1}, new int[] {ITEM_A, 1}, new int[] {ITEM_N, 1}),
		new Word("KINGFISHER", new int[] {ITEM_K, 1}, new int[] {ITEM_I, 1}, new int[] {ITEM_N, 1}, new int[] {ITEM_G, 1}, new int[] {ITEM_F, 1}, new int[] {ITEM_I, 1}, new int[] {ITEM_S, 1}, new int[] {ITEM_H, 1}, new int[] {ITEM_E, 1}, new int[] {ITEM_R, 1}),
		new Word("CYGNUS", new int[] {ITEM_C, 1}, new int[] {ITEM_Y, 1}, new int[] {ITEM_G, 1}, new int[] {ITEM_N, 1}, new int[] {ITEM_U, 1}, new int[] {ITEM_S, 1}),
		new Word("TRITON", new int[] {ITEM_T, 2}, new int[] {ITEM_R, 1}, new int[] {ITEM_I, 1}, new int[] {ITEM_N, 1}),
		new Word("RAINBOW", new int[] {ITEM_R, 1}, new int[] {ITEM_A, 1}, new int[] {ITEM_I, 1}, new int[] {ITEM_N, 1}, new int[] {ITEM_B, 1}, new int[] {ITEM_O, 1}, new int[] {ITEM_W, 1}),
		new Word("SPRING", new int[] {ITEM_S, 1}, new int[] {ITEM_P, 1}, new int[] {ITEM_R, 1}, new int[] {ITEM_I, 1}, new int[] {ITEM_N, 1}, new int[] {ITEM_G, 1})
	};
	// @formatter:on
	
	public RainbowSpringsChateau()
	{
		super(RAINBOW_SPRINGS);
		
		addFirstTalkId(MESSENGER);
		addFirstTalkId(CARETAKER);
		addFirstTalkId(YETIS);
		addTalkId(MESSENGER);
		addTalkId(CARETAKER);
		addTalkId(YETIS);
		
		for (int squashes : GOURDS)
		{
			addSpawnId(squashes);
			addKillId(squashes);
		}
		
		addSpawnId(ENRAGED_YETI);
		
		addSkillSeeId(YETIS);
		
		addKillId(CHEST);
		
		_generated = -1;
		_winner = ClanTable.getInstance().getClan(_hall.getOwnerId());
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final Clan clan = player.getClan();
		
		switch (npc.getId())
		{
			case MESSENGER:
			{
				switch (event)
				{
					case "Register":
					{
						if (!player.isClanLeader())
						{
							htmltext = "35604-07.htm";
						}
						else if ((clan.getCastleId() > 0) || (clan.getHideoutId() > 0))
						{
							htmltext = "35604-09.htm";
						}
						else if (!_hall.isRegistering())
						{
							htmltext = "35604-11.htm";
						}
						else if (_warDecreesCount.containsKey(clan.getId()))
						{
							htmltext = "35604-10.htm";
						}
						else if (getAttackers().size() >= 4)
						{
							htmltext = "35604-18.htm";
						}
						else if ((clan.getLevel() < 3) || (clan.getMembersCount() < 5))
						{
							htmltext = "35604-08.htm";
						}
						else
						{
							final Item warDecrees = player.getInventory().getItemByItemId(WAR_DECREES);
							if (warDecrees == null)
							{
								htmltext = "35604-05.htm";
							}
							else
							{
								final int count = warDecrees.getCount();
								_warDecreesCount.put(clan.getId(), count);
								player.destroyItem(ItemProcessType.DESTROY, warDecrees, npc, true);
								registerClan(clan, count, true);
								htmltext = "35604-06.htm";
							}
						}
						break;
					}
					case "Cancel":
					{
						if (!player.isClanLeader())
						{
							htmltext = "35604-08.htm";
						}
						else if (!_warDecreesCount.containsKey(clan.getId()))
						{
							htmltext = "35604-12.htm";
						}
						else if (!_hall.isRegistering())
						{
							htmltext = "35604-13.htm";
						}
						else
						{
							registerClan(clan, 0, false);
							htmltext = "35604-17.htm";
						}
						break;
					}
					case "Unregister":
					{
						if (!player.isClanLeader())
						{
							htmltext = "35604-07.htm";
						}
						else if (_hall.isRegistering())
						{
							if (_warDecreesCount.containsKey(clan.getId()))
							{
								player.addItem(ItemProcessType.REFUND, WAR_DECREES, _warDecreesCount.get(clan.getId()) / 2, npc, true);
								_warDecreesCount.remove(clan.getId());
								htmltext = "35604-14.htm";
							}
							else
							{
								htmltext = "35604-16.htm";
							}
						}
						else if (_hall.isWaitingBattle())
						{
							_acceptedClans.remove(clan);
							htmltext = "35604-16.htm";
						}
						break;
					}
				}
				break;
			}
			case CARETAKER:
			{
				switch (event)
				{
					case "GoToArena":
					{
						final Party party = player.getParty();
						if (clan == null)
						{
							htmltext = "35603-07.htm";
						}
						else if (!player.isClanLeader())
						{
							htmltext = "35603-02.htm";
						}
						else if (!player.isInParty())
						{
							htmltext = "35603-03.htm";
						}
						else if (party.getLeaderObjectId() != player.getObjectId())
						{
							htmltext = "35603-04.htm";
						}
						else
						{
							final int clanId = player.getId();
							boolean nonClanMemberInParty = false;
							for (Player member : party.getMembers())
							{
								if (member.getId() != clanId)
								{
									nonClanMemberInParty = true;
									break;
								}
							}
							
							if (nonClanMemberInParty)
							{
								htmltext = "35603-05.htm";
							}
							else if (party.getMemberCount() < 5)
							{
								htmltext = "35603-06.htm";
							}
							
							if ((clan.getCastleId() > 0) || (clan.getHideoutId() > 0))
							{
								htmltext = "35603-08.htm";
							}
							else if (clan.getLevel() < ConquerableHallSiegeConfig.CHS_CLAN_MINLEVEL)
							{
								htmltext = "35603-09.htm";
							}
							else if (!_acceptedClans.contains(clan))
							{
								htmltext = "35603-10.htm";
							}
							else
							{
								portToArena(player, _acceptedClans.indexOf(clan));
							}
							
							return null;
						}
						break;
					}
				}
				break;
			}
		}
		
		if (event.startsWith("getItem"))
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
			boolean has = true;
			if (_generated == -1)
			{
				has = false;
			}
			else
			{
				final Word word = WORLD_LIST[_generated];
				switch (_generated)
				{
					case 0:
					{
						has = (player.getInventory().getInventoryItemCount(ITEM_B, -1) >= 2) && (player.getInventory().getInventoryItemCount(ITEM_A, -1) >= 1) && (player.getInventory().getInventoryItemCount(ITEM_Y, -1) >= 1) && (player.getInventory().getInventoryItemCount(ITEM_D, -1) >= 1) && (player.getInventory().getInventoryItemCount(ITEM_U, -1) >= 1) && (player.getInventory().getInventoryItemCount(ITEM_C, -1) >= 1) && (player.getInventory().getInventoryItemCount(ITEM_K, -1) >= 1);
						break;
					}
					case 1:
					{
						has = (player.getInventory().getInventoryItemCount(ITEM_A, -1) >= 2) && (player.getInventory().getInventoryItemCount(ITEM_L, -1) >= 1) && (player.getInventory().getInventoryItemCount(ITEM_B, -1) >= 1) && (player.getInventory().getInventoryItemCount(ITEM_T, -1) >= 1) && (player.getInventory().getInventoryItemCount(ITEM_R, -1) >= 1) && (player.getInventory().getInventoryItemCount(ITEM_O, -1) >= 1) && (player.getInventory().getInventoryItemCount(ITEM_S, -1) >= 1);
						break;
					}
					case 2:
					{
						has = (player.getInventory().getInventoryItemCount(ITEM_P, -1) >= 1) && (player.getInventory().getInventoryItemCount(ITEM_E, -1) >= 1) && (player.getInventory().getInventoryItemCount(ITEM_L, -1) >= 1) && (player.getInventory().getInventoryItemCount(ITEM_I, -1) >= 1) && (player.getInventory().getInventoryItemCount(ITEM_C, -1) >= 1) && (player.getInventory().getInventoryItemCount(ITEM_A, -1) >= 1) && (player.getInventory().getInventoryItemCount(ITEM_N, -1) >= 1);
						break;
					}
					case 3:
					{
						has = (player.getInventory().getInventoryItemCount(ITEM_K, -1) >= 1) && (player.getInventory().getInventoryItemCount(ITEM_I, -1) >= 2) && (player.getInventory().getInventoryItemCount(ITEM_N, -1) >= 1) && (player.getInventory().getInventoryItemCount(ITEM_G, -1) >= 1) && (player.getInventory().getInventoryItemCount(ITEM_F, -1) >= 1) && (player.getInventory().getInventoryItemCount(ITEM_S, -1) >= 1) && (player.getInventory().getInventoryItemCount(ITEM_H, -1) >= 1) && (player.getInventory().getInventoryItemCount(ITEM_E, -1) >= 1) && (player.getInventory().getInventoryItemCount(ITEM_R, -1) >= 1);
						break;
					}
					case 4:
					{
						has = (player.getInventory().getInventoryItemCount(ITEM_C, -1) >= 1) && (player.getInventory().getInventoryItemCount(ITEM_Y, -1) >= 1) && (player.getInventory().getInventoryItemCount(ITEM_G, -1) >= 1) && (player.getInventory().getInventoryItemCount(ITEM_N, -1) >= 1) && (player.getInventory().getInventoryItemCount(ITEM_U, -1) >= 1) && (player.getInventory().getInventoryItemCount(ITEM_S, -1) >= 1);
						break;
					}
					case 5:
					{
						has = (player.getInventory().getInventoryItemCount(ITEM_T, -1) >= 2) && (player.getInventory().getInventoryItemCount(ITEM_R, -1) >= 1) && (player.getInventory().getInventoryItemCount(ITEM_I, -1) >= 1) && (player.getInventory().getInventoryItemCount(ITEM_O, -1) >= 1) && (player.getInventory().getInventoryItemCount(ITEM_N, -1) >= 1);
						break;
					}
					case 6:
					{
						has = (player.getInventory().getInventoryItemCount(ITEM_R, -1) >= 1) && (player.getInventory().getInventoryItemCount(ITEM_A, -1) >= 1) && (player.getInventory().getInventoryItemCount(ITEM_I, -1) >= 1) && (player.getInventory().getInventoryItemCount(ITEM_N, -1) >= 1) && (player.getInventory().getInventoryItemCount(ITEM_B, -1) >= 1) && (player.getInventory().getInventoryItemCount(ITEM_O, -1) >= 1) && (player.getInventory().getInventoryItemCount(ITEM_W, -1) >= 1);
						break;
					}
					case 7:
					{
						has = (player.getInventory().getInventoryItemCount(ITEM_S, -1) >= 1) && (player.getInventory().getInventoryItemCount(ITEM_P, -1) >= 1) && (player.getInventory().getInventoryItemCount(ITEM_R, -1) >= 1) && (player.getInventory().getInventoryItemCount(ITEM_I, -1) >= 1) && (player.getInventory().getInventoryItemCount(ITEM_N, -1) >= 1) && (player.getInventory().getInventoryItemCount(ITEM_G, -1) >= 1);
						break;
					}
				}
				
				if (has)
				{
					for (int[] itemInfo : word.getItems())
					{
						player.destroyItemByItemId(ItemProcessType.DESTROY, itemInfo[0], itemInfo[1], player, true);
					}
					
					final int rnd = Rnd.get(100);
					if ((_generated >= 0) && (_generated <= 5))
					{
						if (rnd < 70)
						{
							giveItems(player, RAINBOW_NECTAR, 1);
						}
						else if (rnd < 80)
						{
							giveItems(player, RAINBOW_MWATER, 1);
						}
						else if (rnd < 90)
						{
							giveItems(player, RAINBOW_WATER, 1);
						}
						else
						{
							giveItems(player, RAINBOW_SULFUR, 1);
						}
					}
					else
					{
						if (rnd < 10)
						{
							giveItems(player, RAINBOW_NECTAR, 1);
						}
						else if (rnd < 40)
						{
							giveItems(player, RAINBOW_MWATER, 1);
						}
						else if (rnd < 70)
						{
							giveItems(player, RAINBOW_WATER, 1);
						}
						else
						{
							giveItems(player, RAINBOW_SULFUR, 1);
						}
					}
				}
				
				if (!has)
				{
					html.setFile(player, "data/scripts/conquerablehalls/RainbowSpringsChateau/35596-02.htm");
				}
				else
				{
					html.setFile(player, "data/scripts/conquerablehalls/RainbowSpringsChateau/35596-04.htm");
				}
				
				player.sendPacket(html);
			}
			
			return null;
		}
		else if (event.startsWith("seeItem"))
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
			html.setFile(player, "data/scripts/conquerablehalls/RainbowSpringsChateau/35596-05.htm");
			if (_generated == -1)
			{
				html.replace("%word%", "Undecided");
			}
			else
			{
				html.replace("%word%", WORLD_LIST[_generated].getName());
			}
			
			player.sendPacket(html);
			return null;
		}
		
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		final int npcId = npc.getId();
		
		if (npcId == MESSENGER)
		{
			final String main = (_hall.getOwnerId() > 0) ? "35604-01.htm" : "35604-00.htm";
			html.setFile(player, "data/scripts/conquerablehalls/RainbowSpringsChateau/" + main);
			html.replace("%nextSiege%", SIMPLE_FORMAT.format(_hall.getSiegeDate().getTime()));
			if (_hall.getOwnerId() > 0)
			{
				html.replace("%owner%", ClanTable.getInstance().getClan(_hall.getOwnerId()).getName());
			}
			
			player.sendPacket(html);
		}
		else if (npcId == CARETAKER)
		{
			final String main = (_hall.isInSiege() || !_hall.isWaitingBattle()) ? "35603-00.htm" : "35603-01.htm";
			html.setFile(player, "data/scripts/conquerablehalls/RainbowSpringsChateau/" + main);
			player.sendPacket(html);
		}
		else if (ArrayUtil.contains(YETIS, npcId))
		{
			final Clan clan = player.getClan();
			if (_acceptedClans.contains(clan))
			{
				final int index = _acceptedClans.indexOf(clan);
				if (npcId == YETIS[index])
				{
					if (!player.isClanLeader())
					{
						html.setFile(player, "data/scripts/conquerablehalls/RainbowSpringsChateau/35596-00.htm");
					}
					else
					{
						html.setFile(player, "data/scripts/conquerablehalls/RainbowSpringsChateau/35596-01.htm");
					}
				}
				else
				{
					html.setFile(player, "data/scripts/conquerablehalls/RainbowSpringsChateau/35596-06.htm");
				}
			}
			else
			{
				html.setFile(player, "data/scripts/conquerablehalls/RainbowSpringsChateau/35596-06.htm");
			}
			
			player.sendPacket(html);
		}
		
		player.setLastQuestNpcObject(npc.getObjectId());
		return "";
	}
	
	@Override
	public void onSkillSee(Npc npc, Player caster, Skill skill, List<WorldObject> targets, boolean isSummon)
	{
		if (targets.contains(npc))
		{
			final Clan clan = caster.getClan();
			if ((clan == null) || !_acceptedClans.contains(clan))
			{
				return;
			}
			
			final int index = _acceptedClans.indexOf(clan);
			int warIndex = Integer.MIN_VALUE;
			
			if (npc.isInsideRadius2D(caster, 60))
			{
				switch (skill.getId())
				{
					case 2240:
					{
						if (getRandom(100) < 10)
						{
							addSpawn(ENRAGED_YETI, caster.getX() + 10, caster.getY() + 10, caster.getZ(), 0, false, 0, false);
						}
						
						reduceGourdHp(index, caster);
						break;
					}
					case 2241:
					{
						warIndex = rndEx(_acceptedClans.size(), index);
						if (warIndex == Integer.MIN_VALUE)
						{
							return;
						}
						
						increaseGourdHp(warIndex);
						break;
					}
					case 2242:
					{
						warIndex = rndEx(_acceptedClans.size(), index);
						if (warIndex == Integer.MIN_VALUE)
						{
							return;
						}
						
						moveGourds(warIndex);
						break;
					}
					case 2243:
					{
						warIndex = rndEx(_acceptedClans.size(), index);
						if (warIndex == Integer.MIN_VALUE)
						{
							return;
						}
						
						castDebuffsOnEnemies(caster, warIndex);
						break;
					}
				}
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Clan clan = killer.getClan();
		final int index = _acceptedClans.indexOf(clan);
		if ((clan == null) || !_acceptedClans.contains(clan))
		{
			return;
		}
		
		if (npc.getId() == CHEST)
		{
			chestDie(npc);
			if (chests.contains(npc))
			{
				chests.remove(npc);
			}
			
			final int chance = Rnd.get(100);
			if (chance <= 5)
			{
				npc.dropItem(killer, ITEM_A, 1);
			}
			else if ((chance > 5) && (chance <= 10))
			{
				npc.dropItem(killer, ITEM_B, 1);
			}
			else if ((chance > 10) && (chance <= 15))
			{
				npc.dropItem(killer, ITEM_C, 1);
			}
			else if ((chance > 15) && (chance <= 20))
			{
				npc.dropItem(killer, ITEM_D, 1);
			}
			else if ((chance > 20) && (chance <= 25))
			{
				npc.dropItem(killer, ITEM_E, 1);
			}
			else if ((chance > 25) && (chance <= 30))
			{
				npc.dropItem(killer, ITEM_F, 1);
			}
			else if ((chance > 30) && (chance <= 35))
			{
				npc.dropItem(killer, ITEM_G, 1);
			}
			else if ((chance > 35) && (chance <= 40))
			{
				npc.dropItem(killer, ITEM_H, 1);
			}
			else if ((chance > 40) && (chance <= 45))
			{
				npc.dropItem(killer, ITEM_I, 1);
			}
			else if ((chance > 45) && (chance <= 50))
			{
				npc.dropItem(killer, ITEM_K, 1);
			}
			else if ((chance > 50) && (chance <= 55))
			{
				npc.dropItem(killer, ITEM_L, 1);
			}
			else if ((chance > 55) && (chance <= 60))
			{
				npc.dropItem(killer, ITEM_N, 1);
			}
			else if ((chance > 60) && (chance <= 65))
			{
				npc.dropItem(killer, ITEM_O, 1);
			}
			else if ((chance > 65) && (chance <= 70))
			{
				npc.dropItem(killer, ITEM_P, 1);
			}
			else if ((chance > 70) && (chance <= 75))
			{
				npc.dropItem(killer, ITEM_R, 1);
			}
			else if ((chance > 75) && (chance <= 80))
			{
				npc.dropItem(killer, ITEM_S, 1);
			}
			else if ((chance > 80) && (chance <= 85))
			{
				npc.dropItem(killer, ITEM_T, 1);
			}
			else if ((chance > 85) && (chance <= 90))
			{
				npc.dropItem(killer, ITEM_U, 1);
			}
			else if ((chance > 90) && (chance <= 95))
			{
				npc.dropItem(killer, ITEM_W, 1);
			}
			else if (chance > 95)
			{
				npc.dropItem(killer, ITEM_Y, 1);
			}
		}
		
		if (npc.getId() == GOURDS[index])
		{
			_missionAccomplished = true;
			_winner = ClanTable.getInstance().getClan(clan.getId());
			
			synchronized (this)
			{
				cancelSiegeTask();
				endSiege();
				
				ThreadPool.schedule(() ->
				{
					for (int id : _playersOnArena)
					{
						final Player pl = World.getInstance().getPlayer(id);
						if (pl != null)
						{
							pl.teleToLocation(TeleportWhereType.TOWN);
						}
					}
					
					_playersOnArena = new ArrayList<>();
				}, 120 * 1000);
			}
		}
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		if (npc.getId() == ENRAGED_YETI)
		{
			npc.broadcastSay(ChatType.NPC_SHOUT, "Oooh! Who poured nectar on my head while I was sleeping?");
		}
		
		if (ArrayUtil.contains(GOURDS, npc.getId()))
		{
			npc.disableCoreAI(true);
			npc.setImmobilized(true);
		}
	}
	
	@Override
	public void startSiege()
	{
		if ((_acceptedClans == null) || _acceptedClans.isEmpty() || (_acceptedClans.size() < 2))
		{
			onSiegeEnds();
			_acceptedClans.clear();
			_hall.updateNextSiege();
			final SystemMessage sm = new SystemMessage(SystemMessageId.THE_SIEGE_OF_S1_HAS_BEEN_CANCELED_DUE_TO_LACK_OF_INTEREST);
			sm.addString(ClanHallTable.getInstance().getClanHallById(_hall.getId()).getName());
			Broadcast.toAllOnlinePlayers(sm);
			return;
		}
		
		spawnGourds();
		spawnYetis();
	}
	
	@Override
	public void prepareOwner()
	{
		if (_hall.getOwnerId() > 0)
		{
			registerClan(ClanTable.getInstance().getClan(_hall.getOwnerId()), 10000, true);
		}
		
		_hall.banishForeigners();
		final SystemMessage msg = new SystemMessage(SystemMessageId.THE_REGISTRATION_TERM_FOR_S1_HAS_ENDED);
		msg.addString(ClanHallTable.getInstance().getClanHallById(_hall.getId()).getName());
		Broadcast.toAllOnlinePlayers(msg);
		_hall.updateSiegeStatus(SiegeStatus.WAITING_BATTLE);
		_siegeTask = ThreadPool.schedule(new SiegeStarts(), 3600000);
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
		unSpawnGourds();
		unSpawnYetis();
		unSpawnChests();
		clearTables();
	}
	
	protected void portToArena(Player leader, int arena)
	{
		if ((arena < 0) || (arena > 3))
		{
			LOGGER.warning(getClass().getSimpleName() + ": Wrong arena id passed: " + arena);
			return;
		}
		
		for (Player pc : leader.getParty().getMembers())
		{
			if (pc != null)
			{
				pc.stopAllEffects();
				if (pc.hasSummon())
				{
					pc.getSummon().unSummon(pc);
				}
				
				_playersOnArena.add(pc.getObjectId());
				pc.teleToLocation(ARENAS[arena][0], ARENAS[arena][1], ARENAS[arena][2], true);
			}
		}
	}
	
	protected void spawnYetis()
	{
		if ((_acceptedClans == null) || _acceptedClans.isEmpty())
		{
			return;
		}
		
		for (int i = 0; i < _acceptedClans.size(); i++)
		{
			if (_yetis[i] == null)
			{
				try
				{
					_yetis[i] = addSpawn(YETIS[i], YETIS_SPAWN[i][0], YETIS_SPAWN[i][1], YETIS_SPAWN[i][2], 0, false, 0, false);
					_yetis[i].setHeading(1);
					_task = ThreadPool.scheduleAtFixedRate(new GenerateTask(_yetis[i]), 10000, 300000);
				}
				catch (Exception e)
				{
					LOGGER.warning(getClass().getSimpleName() + ": Problem with spawnYetis: " + e.getMessage());
				}
			}
		}
	}
	
	protected void spawnGourds()
	{
		if ((_acceptedClans == null) || _acceptedClans.isEmpty() || (_acceptedClans.size() < 2))
		{
			return;
		}
		
		for (int i = 0; i < _acceptedClans.size(); i++)
		{
			
			try
			{
				_gourds[i] = addSpawn(GOURDS[i], ARENAS[i][0] + 150, ARENAS[i][1] + 150, ARENAS[i][2], 1);
			}
			catch (Exception e)
			{
				LOGGER.warning(getClass().getSimpleName() + ": Problem with spawnGourds: " + e.getMessage());
			}
		}
		
		_chesttask = ThreadPool.scheduleAtFixedRate(new ChestsSpawn(), 5000, 5000);
	}
	
	protected void unSpawnYetis()
	{
		if ((_acceptedClans == null) || _acceptedClans.isEmpty())
		{
			return;
		}
		
		for (int i = 0; i < _acceptedClans.size(); i++)
		{
			if (_yetis[i] != null)
			{
				_yetis[i].deleteMe();
			}
			
			if (_task != null)
			{
				_task.cancel(false);
				_task = null;
			}
		}
	}
	
	protected void unSpawnGourds()
	{
		if ((_acceptedClans == null) || _acceptedClans.isEmpty())
		{
			return;
		}
		
		for (int i = 0; i < _acceptedClans.size(); i++)
		{
			if ((_gourds[i] != null) && (_gourds[i].getSpawn().getLastSpawn() != null))
			{
				_gourds[i].getSpawn().getLastSpawn().deleteMe();
			}
		}
	}
	
	protected void unSpawnChests()
	{
		if (!chests.isEmpty())
		{
			for (Npc chest : chests)
			{
				if (chest != null)
				{
					chest.deleteMe();
					if (_chesttask != null)
					{
						_chesttask.cancel(false);
						_chesttask = null;
					}
				}
			}
		}
	}
	
	private static void moveGourds(int index)
	{
		final Npc[] tempArray = _gourds;
		for (int i = 0; i < index; i++)
		{
			final Npc oldSpawn = _gourds[(index - 1) - i];
			final Npc curSpawn = tempArray[i];
			_gourds[(index - 1) - i] = curSpawn;
			
			final int newX = oldSpawn.getX();
			final int newY = oldSpawn.getY();
			final int newZ = oldSpawn.getZ();
			curSpawn.getSpawn().getLastSpawn().teleToLocation(newX, newY, newZ, true);
		}
	}
	
	private static void reduceGourdHp(int index, Player player)
	{
		final Npc gourd = _gourds[index];
		if ((gourd != null) && (gourd.getSpawn().getLastSpawn() != null))
		{
			gourd.getSpawn().getLastSpawn().reduceCurrentHp(1000, player, null);
		}
	}
	
	private static void increaseGourdHp(int index)
	{
		final Npc gourd = _gourds[index];
		if (gourd != null)
		{
			final Npc gourdNpc = gourd.getSpawn().getLastSpawn();
			if (gourdNpc != null)
			{
				gourdNpc.setCurrentHp(gourdNpc.getCurrentHp() + 1000);
			}
		}
	}
	
	private void castDebuffsOnEnemies(Player player, int myArena)
	{
		final Clan clan = player.getClan();
		if (_acceptedClans.contains(clan))
		{
			final int index = _acceptedClans.indexOf(clan);
			if (_playersOnArena.contains(player.getObjectId()))
			{
				for (Player pl : player.getParty().getMembers())
				{
					if (index != myArena)
					{
						continue;
					}
					
					if (pl != null)
					{
						for (Skill sk : DEBUFFS)
						{
							sk.applyEffects(pl, pl);
						}
					}
				}
			}
		}
	}
	
	private void registerClan(Clan clan, int count, boolean register)
	{
		if (register)
		{
			final SiegeClan sc = new SiegeClan(clan.getId(), SiegeClanType.ATTACKER);
			getAttackers().put(clan.getId(), sc);
			
			final int spotLeft = 4;
			for (int i = 0; i < spotLeft; i++)
			{
				long counter = 0;
				Clan fightclan = null;
				for (int clanId : _warDecreesCount.keySet())
				{
					final Clan actingClan = ClanTable.getInstance().getClan(clanId);
					if ((actingClan == null) || (actingClan.getDissolvingExpiryTime() > 0))
					{
						_warDecreesCount.remove(clanId);
						continue;
					}
					
					final int counts = _warDecreesCount.get(clanId);
					if (counts > counter)
					{
						counter = counts;
						fightclan = actingClan;
					}
				}
				
				if ((fightclan != null) && (_acceptedClans.size() < 4))
				{
					_acceptedClans.add(clan);
				}
			}
			
			updateAttacker(clan.getId(), count, false);
		}
		else
		{
			updateAttacker(clan.getId(), 0, true);
		}
	}
	
	private void updateAttacker(int clanId, int count, boolean remove)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement;
			if (remove)
			{
				statement = con.prepareStatement("DELETE FROM rainbowsprings_attacker_list WHERE clanId = ?");
				statement.setInt(1, clanId);
			}
			else
			{
				statement = con.prepareStatement("INSERT INTO rainbowsprings_attacker_list VALUES (?,?)");
				statement.setInt(1, clanId);
				statement.setInt(2, count);
			}
			
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Problem with updateAttacker: " + e.getMessage());
		}
	}
	
	@Override
	public void loadAttackers()
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			final PreparedStatement statement = con.prepareStatement("SELECT * FROM rainbowsprings_attacker_list");
			final ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				final int clanId = rset.getInt("clanId");
				final int count = rset.getInt("war_decrees_count");
				_warDecreesCount.put(clanId, count);
				for (int clan : _warDecreesCount.keySet())
				{
					final Clan loadClan = ClanTable.getInstance().getClan(clan);
					_acceptedClans.add(loadClan);
				}
			}
			
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ".loadAttackers()->" + e.getMessage());
		}
	}
	
	private void clearTables()
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			final PreparedStatement stat1 = con.prepareStatement("DELETE FROM rainbowsprings_attacker_list");
			stat1.execute();
			stat1.close();
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ".clearTables()->" + e.getMessage());
		}
	}
	
	protected class ChestsSpawn implements Runnable
	{
		@Override
		public void run()
		{
			for (int i = 0; i < _acceptedClans.size(); i++)
			{
				if (arenaChestsCnt[i] < 4)
				{
					final Npc chest = addSpawn(CHEST, CHESTS_SPAWN[i][0] + getRandom(-400, 400), CHESTS_SPAWN[i][1] + getRandom(-400, 400), CHESTS_SPAWN[i][2], 0, false, 0, false);
					if (chest != null)
					{
						chests.add(chest);
					}
					
					arenaChestsCnt[i]++;
				}
				
				if (arenaChestsCnt[i] < 4)
				{
					final Npc chest = addSpawn(CHEST, CHESTS_SPAWN[i][0] + getRandom(-400, 400), CHESTS_SPAWN[i][1] + getRandom(-400, 400), CHESTS_SPAWN[i][2], 0, false, 0, false);
					if (chest != null)
					{
						chests.add(chest);
					}
					
					arenaChestsCnt[i]++;
				}
				
				if (arenaChestsCnt[i] < 4)
				{
					final Npc chest = addSpawn(CHEST, CHESTS_SPAWN[i][0] + getRandom(-400, 400), CHESTS_SPAWN[i][1] + getRandom(-400, 400), CHESTS_SPAWN[i][2], 0, false, 0, false);
					if (chest != null)
					{
						chests.add(chest);
					}
					
					arenaChestsCnt[i]++;
				}
				
				if (arenaChestsCnt[i] < 4)
				{
					final Npc chest = addSpawn(CHEST, CHESTS_SPAWN[i][0] + getRandom(-400, 400), CHESTS_SPAWN[i][1] + getRandom(-400, 400), CHESTS_SPAWN[i][2], 0, false, 0, false);
					if (chest != null)
					{
						chests.add(chest);
					}
					
					arenaChestsCnt[i]++;
				}
			}
		}
	}
	
	protected class GenerateTask implements Runnable
	{
		protected final Npc _npc;
		
		protected GenerateTask(Npc npc)
		{
			_npc = npc;
		}
		
		@Override
		public void run()
		{
			_generated = getRandom(WORLD_LIST.length);
			final Word word = WORLD_LIST[_generated];
			final ExShowScreenMessage msg = new ExShowScreenMessage(word.getName(), 5000);
			final int region = MapRegionData.getInstance().getMapRegionLocId(_npc.getX(), _npc.getY());
			for (Player player : World.getInstance().getPlayers())
			{
				if ((region == MapRegionData.getInstance().getMapRegionLocId(player.getX(), player.getY())) && LocationUtil.checkIfInRange(750, _npc, player, false))
				{
					player.sendPacket(msg);
				}
			}
		}
	}
	
	protected void chestDie(Npc npc)
	{
		for (int i = 0; i < _acceptedClans.size(); i++)
		{
			arenaChestsCnt[i]--;
		}
	}
	
	private int rndEx(int size, int ex)
	{
		int rnd = Integer.MIN_VALUE;
		for (int i = 0; i < Byte.MAX_VALUE; i++)
		{
			rnd = Rnd.get(size);
			if (rnd != ex)
			{
				break;
			}
		}
		
		return rnd;
	}
	
	@Override
	public Clan getWinner()
	{
		return _winner;
	}
	
	public static void main(String[] args)
	{
		new RainbowSpringsChateau();
	}
}
