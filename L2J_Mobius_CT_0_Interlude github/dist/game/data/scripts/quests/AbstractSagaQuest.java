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
package quests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;

public abstract class AbstractSagaQuest extends Quest
{
	protected int[] _npc = {};
	protected int[] _items = {};
	protected int[] _mob = {};
	protected int[] _classId = {};
	protected int[] _prevClass = {};
	protected int[] _x = {};
	protected int[] _y = {};
	protected int[] _z = {};
	protected String[] _text = {};
	private static final Map<Npc, Integer> SPAWN_LIST = new HashMap<>();
	// @formatter:off
	private static final int[][] QUEST_CLASSES =
	{
		{ 0x7f }, { 0x80, 0x81 }, { 0x82 }, { 0x05 }, { 0x14 }, { 0x15 },
		{ 0x02 }, { 0x03 }, { 0x2e }, { 0x30 }, { 0x33 }, { 0x34 }, { 0x08 },
		{ 0x17 }, { 0x24 }, { 0x09 }, { 0x18 }, { 0x25 }, { 0x10 }, { 0x11 },
		{ 0x1e }, { 0x0c }, { 0x1b }, { 0x28 }, { 0x0e }, { 0x1c }, { 0x29 },
		{ 0x0d }, { 0x06 }, { 0x22 }, { 0x21 }, { 0x2b }, { 0x37 }, { 0x39 }
	};
	// @formatter:on
	private static final int[] ARCHON_HALISHA_NORM =
	{
		18212,
		18214,
		18215,
		18216,
		18218
	};
	
	public AbstractSagaQuest(int questId, String description)
	{
		super(questId, description);
	}
	
	public void registerNPCs()
	{
		addStartNpc(_npc[0]);
		addAttackId(_mob[2]);
		addAttackId(_mob[1]);
		addSkillSeeId(_mob[1]);
		addFirstTalkId(_npc[4]);
		addTalkId(_npc);
		addKillId(_mob);
		final int[] questItemIds = _items.clone();
		questItemIds[0] = 0;
		questItemIds[2] = 0; // remove Ice Crystal and Divine Stone of Wisdom
		registerQuestItems(questItemIds);
		for (int archonMinion = 21646; archonMinion < 21652; archonMinion++)
		{
			addKillId(archonMinion);
		}
		
		addKillId(ARCHON_HALISHA_NORM);
		for (int guardianAngel = 27214; guardianAngel < 27217; guardianAngel++)
		{
			addKillId(guardianAngel);
		}
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState st = getQuestState(player, false);
		if (st == null)
		{
			return null;
		}
		
		String htmltext = null;
		switch (event)
		{
			case "0-011.htm":
			case "0-012.htm":
			case "0-013.htm":
			case "0-014.htm":
			case "0-015.htm":
			{
				htmltext = event;
				break;
			}
			case "accept":
			{
				st.startQuest();
				giveItems(player, _items[10], 1);
				htmltext = "0-03.htm";
				break;
			}
			case "0-1":
			{
				if (player.getLevel() < 76)
				{
					htmltext = "0-02.htm";
					if (st.isCreated())
					{
						st.exitQuest(true);
					}
				}
				else
				{
					htmltext = "0-05.htm";
				}
				break;
			}
			case "0-2":
			{
				if (player.getLevel() < 76)
				{
					takeItems(player, _items[10], -1);
					st.setCond(20, true);
					htmltext = "0-08.htm";
				}
				else
				{
					takeItems(player, _items[10], -1);
					addExpAndSp(player, 2299404, 0);
					giveAdena(player, 5000000, true);
					giveItems(player, 6622, 1);
					final int playerClass = getPlayerClass(player);
					final int prevClass = getPrevClass(player);
					player.setPlayerClass(playerClass);
					if (!player.isSubClassActive() && (player.getBaseClass() == prevClass))
					{
						player.setBaseClass(playerClass);
					}
					
					player.broadcastUserInfo();
					cast(npc, player, 4339, 1);
					st.exitQuest(false);
					htmltext = "0-07.htm";
				}
				break;
			}
			case "1-3":
			{
				st.setCond(3);
				htmltext = "1-05.htm";
				break;
			}
			case "1-4":
			{
				st.setCond(4);
				takeItems(player, _items[0], 1);
				if (_items[11] != 0)
				{
					takeItems(player, _items[11], 1);
				}
				
				giveItems(player, _items[1], 1);
				htmltext = "1-06.htm";
				break;
			}
			case "2-1":
			{
				st.setCond(2);
				htmltext = "2-05.htm";
				break;
			}
			case "2-2":
			{
				st.setCond(5);
				takeItems(player, _items[1], 1);
				giveItems(player, _items[4], 1);
				htmltext = "2-06.htm";
				break;
			}
			case "3-5":
			{
				htmltext = "3-07.htm";
				break;
			}
			case "3-6":
			{
				st.setCond(11);
				htmltext = "3-02.htm";
				break;
			}
			case "3-7":
			{
				st.setCond(12);
				htmltext = "3-03.htm";
				break;
			}
			case "3-8":
			{
				st.setCond(13);
				takeItems(player, _items[2], 1);
				giveItems(player, _items[7], 1);
				htmltext = "3-08.htm";
				break;
			}
			case "4-1":
			{
				htmltext = "4-010.htm";
				break;
			}
			case "4-2":
			{
				giveItems(player, _items[9], 1);
				st.setCond(18, true);
				htmltext = "4-011.htm";
				break;
			}
			case "4-3":
			{
				giveItems(player, _items[9], 1);
				st.setCond(18, true);
				autoChat(npc, _text[13].replace("PLAYERNAME", player.getName()));
				st.set("Quest0", "0");
				cancelQuestTimer("Mob_2 has despawned", npc, player);
				deleteSpawn(npc);
				return null;
			}
			case "5-1":
			{
				st.setCond(6, true);
				takeItems(player, _items[4], 1);
				cast(npc, player, 4546, 1);
				htmltext = "5-02.htm";
				break;
			}
			case "6-1":
			{
				st.setCond(8, true);
				st.set("spawned", "0");
				takeItems(player, _items[5], 1);
				cast(npc, player, 4546, 1);
				htmltext = "6-03.htm";
				break;
			}
			case "7-1":
			{
				if (st.getInt("spawned") == 1)
				{
					htmltext = "7-03.htm";
				}
				else if (st.getInt("spawned") == 0)
				{
					final Npc mob1 = addSpawn(_mob[0], _x[0], _y[0], _z[0], -1);
					st.set("spawned", "1");
					startQuestTimer("Mob_1 Timer 1", 500, mob1, player);
					startQuestTimer("Mob_1 has despawned", 300000, mob1, player);
					addSpawn(st, mob1);
					htmltext = "7-02.htm";
				}
				else
				{
					htmltext = "7-04.htm";
				}
				break;
			}
			case "7-2":
			{
				st.setCond(10, true);
				takeItems(player, _items[6], 1);
				cast(npc, player, 4546, 1);
				htmltext = "7-06.htm";
				break;
			}
			case "8-1":
			{
				st.setCond(14, true);
				takeItems(player, _items[7], 1);
				cast(npc, player, 4546, 1);
				htmltext = "8-02.htm";
				break;
			}
			case "9-1":
			{
				st.setCond(17, true);
				st.set("Quest0", "0");
				st.set("Tab", "0");
				takeItems(player, _items[8], 1);
				cast(npc, player, 4546, 1);
				htmltext = "9-03.htm";
				break;
			}
			case "10-1":
			{
				if (st.getInt("Quest0") == 0)
				{
					final Npc mob3 = addSpawn(_mob[2], _x[1], _y[1], _z[1], -1);
					final Npc mob2 = addSpawn(_npc[4], _x[2], _y[2], _z[2], -1);
					addSpawn(st, mob3);
					addSpawn(st, mob2);
					st.set("Mob_2", String.valueOf(mob2.getObjectId()));
					st.set("Quest0", "1");
					st.set("Quest1", "45");
					startQuestTimer("Mob_3 Timer 1", 500, mob3, player, true);
					startQuestTimer("Mob_3 has despawned", 59000, mob3, player);
					startQuestTimer("Mob_2 Timer 1", 500, mob2, player);
					startQuestTimer("Mob_2 has despawned", 60000, mob2, player);
					htmltext = "10-02.htm";
				}
				else if (st.getInt("Quest1") == 45)
				{
					htmltext = "10-03.htm";
				}
				else
				{
					htmltext = "10-04.htm";
				}
				break;
			}
			case "10-2":
			{
				st.setCond(19, true);
				takeItems(player, _items[9], 1);
				cast(npc, player, 4546, 1);
				htmltext = "10-06.htm";
				break;
			}
			case "11-9":
			{
				st.setCond(15);
				htmltext = "11-03.htm";
				break;
			}
			case "Mob_1 Timer 1":
			{
				autoChat(npc, _text[0].replace("PLAYERNAME", player.getName()));
				return null;
			}
			case "Mob_1 has despawned":
			{
				autoChat(npc, _text[1].replace("PLAYERNAME", player.getName()));
				st.set("spawned", "0");
				deleteSpawn(npc);
				return null;
			}
			case "Archon Hellisha has despawned":
			{
				autoChat(npc, _text[6].replace("PLAYERNAME", player.getName()));
				st.set("spawned", "0");
				deleteSpawn(npc);
				return null;
			}
			case "Mob_3 Timer 1":
			{
				final Npc mob2 = findSpawn(player, World.getInstance().findObject(st.getInt("Mob_2")).asNpc());
				if (World.getInstance().getVisibleObjects(npc, Npc.class).contains(mob2))
				{
					npc.asAttackable().addDamageHate(mob2, 0, 99999);
					npc.getAI().setIntention(Intention.ATTACK, mob2, null);
					mob2.getAI().setIntention(Intention.ATTACK, npc, null);
					autoChat(npc, _text[14].replace("PLAYERNAME", player.getName()));
					cancelQuestTimer("Mob_3 Timer 1", npc, player);
				}
				
				return null;
			}
			case "Mob_3 has despawned":
			{
				autoChat(npc, _text[15].replace("PLAYERNAME", player.getName()));
				st.set("Quest0", "2");
				deleteSpawn(npc);
				return null;
			}
			case "Mob_2 Timer 1":
			{
				autoChat(npc, _text[7].replace("PLAYERNAME", player.getName()));
				startQuestTimer("Mob_2 Timer 2", 1500, npc, player);
				if (st.getInt("Quest1") == 45)
				{
					st.set("Quest1", "0");
				}
				
				return null;
			}
			case "Mob_2 Timer 2":
			{
				autoChat(npc, _text[8].replace("PLAYERNAME", player.getName()));
				startQuestTimer("Mob_2 Timer 3", 10000, npc, player);
				return null;
			}
			case "Mob_2 Timer 3":
			{
				if (st.getInt("Quest0") == 0)
				{
					startQuestTimer("Mob_2 Timer 3", 13000, npc, player);
					if (getRandomBoolean())
					{
						autoChat(npc, _text[9].replace("PLAYERNAME", player.getName()));
					}
					else
					{
						autoChat(npc, _text[10].replace("PLAYERNAME", player.getName()));
					}
				}
				
				return null;
			}
			case "Mob_2 has despawned":
			{
				st.set("Quest1", String.valueOf(st.getInt("Quest1") + 1));
				if ((st.getInt("Quest0") == 1) || (st.getInt("Quest0") == 2) || (st.getInt("Quest1") > 3))
				{
					st.set("Quest0", "0");
					
					// TODO: This "if" will never be true.
					if (st.getInt("Quest0") == 1)
					{
						autoChat(npc, _text[11].replace("PLAYERNAME", player.getName()));
					}
					else
					{
						autoChat(npc, _text[12].replace("PLAYERNAME", player.getName()));
					}
					
					deleteSpawn(npc);
				}
				else
				{
					startQuestTimer("Mob_2 has despawned", 1000, npc, player);
				}
				
				return null;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onAttack(Npc npc, Player player, int damage, boolean isSummon)
	{
		final QuestState st2 = findRightState(npc);
		if (st2 != null)
		{
			final int cond = st2.getCond();
			final QuestState st = getQuestState(player, false);
			final int npcId = npc.getId();
			if ((npcId == _mob[2]) && (st == st2) && (cond == 17))
			{
				int quest0 = st.getInt("Quest0") + 1;
				if (quest0 == 1)
				{
					autoChat(npc, _text[16].replace("PLAYERNAME", player.getName()));
				}
				
				if (quest0 > 15)
				{
					quest0 = 1;
					autoChat(npc, _text[17].replace("PLAYERNAME", player.getName()));
					cancelQuestTimer("Mob_3 has despawned", npc, st2.getPlayer());
					st.set("Tab", "1");
					deleteSpawn(npc);
				}
				
				st.set("Quest0", Integer.toString(quest0));
			}
			else if ((npcId == _mob[1]) && (cond == 15) && ((st != st2) || player.isInParty()))
			{
				autoChat(npc, _text[5].replace("PLAYERNAME", player.getName()));
				cancelQuestTimer("Archon Hellisha has despawned", npc, st2.getPlayer());
				st2.set("spawned", "0");
				deleteSpawn(npc);
			}
		}
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		String htmltext = "";
		final QuestState st = getQuestState(player, false);
		final int npcId = npc.getId();
		if ((st != null) && (npcId == _npc[4]))
		{
			final int cond = st.getCond();
			if (cond == 17)
			{
				final QuestState st2 = findRightState(npc);
				if (st2 != null)
				{
					player.setLastQuestNpcObject(npc.getObjectId());
					final int tab = st.getInt("Tab");
					final int quest0 = st.getInt("Quest0");
					if (st == st2)
					{
						if (tab == 1)
						{
							if (quest0 == 0)
							{
								htmltext = "4-04.htm";
							}
							else if (quest0 == 1)
							{
								htmltext = "4-06.htm";
							}
						}
						else if (quest0 == 0)
						{
							htmltext = "4-01.htm";
						}
						else if (quest0 == 1)
						{
							htmltext = "4-03.htm";
						}
					}
					else if (tab == 1)
					{
						if (quest0 == 0)
						{
							htmltext = "4-05.htm";
						}
						else if (quest0 == 1)
						{
							htmltext = "4-07.htm";
						}
					}
					else if (quest0 == 0)
					{
						htmltext = "4-02.htm";
					}
				}
			}
			else if (cond == 18)
			{
				htmltext = "4-08.htm";
			}
		}
		
		if (htmltext.equals(""))
		{
			npc.showChatWindow(player);
		}
		
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		final int npcId = npc.getId();
		QuestState st = getQuestState(player, false);
		for (int archonMinion = 21646; archonMinion < 21652; archonMinion++)
		{
			if (npcId == archonMinion)
			{
				final Party party = player.getParty();
				if (party != null)
				{
					final List<QuestState> partyQuestMembers = new ArrayList<>();
					for (Player player1 : party.getMembers())
					{
						final QuestState st1 = findQuest(player1);
						if ((st1 != null) && st1.isCond(15) && player1.isInsideRadius2D(player, PlayerConfig.ALT_PARTY_RANGE))
						{
							partyQuestMembers.add(st1);
						}
					}
					
					if (!partyQuestMembers.isEmpty())
					{
						final QuestState st2 = partyQuestMembers.get(getRandom(partyQuestMembers.size()));
						giveHalishaMark(st2);
					}
				}
				else
				{
					final QuestState st1 = findQuest(player);
					if ((st1 != null) && st1.isCond(15))
					{
						giveHalishaMark(st1);
					}
				}
				return;
			}
		}
		
		for (int element : ARCHON_HALISHA_NORM)
		{
			if (npcId == element)
			{
				final QuestState st1 = findQuest(player);
				if ((st1 != null) && st1.isCond(15))
				{
					// This is just a guess....not really sure what it actually says, if anything
					autoChat(npc, _text[4].replace("PLAYERNAME", st1.getPlayer().getName()));
					giveItems(player, _items[8], 1);
					takeItems(player, _items[3], -1);
					st1.setCond(16, true);
				}
				return;
			}
		}
		
		for (int guardianAngel = 27214; guardianAngel < 27217; guardianAngel++)
		{
			if (npcId == guardianAngel)
			{
				final QuestState st1 = findQuest(player);
				if ((st1 != null) && st1.isCond(6))
				{
					final int kills = st1.getInt("kills");
					if (kills < 9)
					{
						st1.set("kills", Integer.toString(kills + 1));
					}
					else
					{
						giveItems(player, _items[5], 1);
						st.setCond(7, true);
					}
				}
				return;
			}
		}
		
		if ((st != null) && (npcId != _mob[2]))
		{
			final QuestState st2 = findRightState(npc);
			if (st2 != null)
			{
				final int cond = st.getCond();
				if ((npcId == _mob[0]) && (cond == 8))
				{
					if (!player.isInParty() && (st == st2))
					{
						autoChat(npc, _text[12].replace("PLAYERNAME", player.getName()));
						giveItems(player, _items[6], 1);
						st.setCond(9, true);
					}
					
					cancelQuestTimer("Mob_1 has despawned", npc, st2.getPlayer());
					st2.set("spawned", "0");
					deleteSpawn(npc);
				}
				else if ((npcId == _mob[1]) && (cond == 15))
				{
					if (!player.isInParty())
					{
						if (st == st2)
						{
							autoChat(npc, _text[4].replace("PLAYERNAME", player.getName()));
							giveItems(player, _items[8], 1);
							takeItems(player, _items[3], -1);
							st.setCond(16, true);
						}
						else
						{
							autoChat(npc, _text[5].replace("PLAYERNAME", player.getName()));
						}
					}
					
					cancelQuestTimer("Archon Hellisha has despawned", npc, st2.getPlayer());
					st2.set("spawned", "0");
					deleteSpawn(npc);
				}
			}
		}
		else if (npcId == _mob[0])
		{
			st = findRightState(npc);
			if (st != null)
			{
				cancelQuestTimer("Mob_1 has despawned", npc, st.getPlayer());
				st.set("spawned", "0");
				deleteSpawn(npc);
			}
		}
		else if (npcId == _mob[1])
		{
			st = findRightState(npc);
			if (st != null)
			{
				cancelQuestTimer("Archon Hellisha has despawned", npc, st.getPlayer());
				st.set("spawned", "0");
				deleteSpawn(npc);
			}
		}
		else if (npcId == _mob[2])
		{
			st = findRightState(npc);
			if (st != null)
			{
				st.setCond(18, true);
				autoChat(npc, _text[13].replace("PLAYERNAME", player.getName()));
				st.set("Quest0", "0");
				cancelQuestTimer("Mob_2 has despawned", npc, player);
				deleteSpawn(npc);
			}
		}
	}
	
	@Override
	public void onSkillSee(Npc npc, Player player, Skill skill, List<WorldObject> targets, boolean isSummon)
	{
		if (SPAWN_LIST.containsKey(npc) && (SPAWN_LIST.get(npc) != player.getObjectId()))
		{
			final Player questPlayer = World.getInstance().findObject(SPAWN_LIST.get(npc)).asPlayer();
			if (questPlayer == null)
			{
				return;
			}
			
			for (WorldObject obj : targets)
			{
				if ((obj == questPlayer) || (obj == npc))
				{
					final QuestState st2 = findRightState(npc);
					if (st2 == null)
					{
						return;
					}
					
					autoChat(npc, _text[5].replace("PLAYERNAME", player.getName()));
					cancelQuestTimer("Archon Hellisha has despawned", npc, st2.getPlayer());
					st2.set("spawned", "0");
					deleteSpawn(npc);
				}
			}
		}
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState st = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		final int npcId = npc.getId();
		if ((npcId == _npc[0]) && st.isCompleted())
		{
			htmltext = getAlreadyCompletedMsg(player);
		}
		else if (player.getPlayerClass().getId() == getPrevClass(player))
		{
			switch (st.getCond())
			{
				case 0:
				{
					if (npcId == _npc[0])
					{
						htmltext = "0-01.htm";
					}
					break;
				}
				case 1:
				{
					if (npcId == _npc[0])
					{
						htmltext = "0-04.htm";
					}
					else if (npcId == _npc[2])
					{
						htmltext = "2-01.htm";
					}
					break;
				}
				case 2:
				{
					if (npcId == _npc[2])
					{
						htmltext = "2-02.htm";
					}
					else if (npcId == _npc[1])
					{
						htmltext = "1-01.htm";
					}
					break;
				}
				case 3:
				{
					if ((npcId == _npc[1]) && hasQuestItems(player, _items[0]))
					{
						if ((_items[11] == 0) || hasQuestItems(player, _items[11]))
						{
							htmltext = "1-03.htm";
						}
						else
						{
							htmltext = "1-02.htm";
						}
					}
					break;
				}
				case 4:
				{
					if (npcId == _npc[1])
					{
						htmltext = "1-04.htm";
					}
					else if (npcId == _npc[2])
					{
						htmltext = "2-03.htm";
					}
					break;
				}
				case 5:
				{
					if (npcId == _npc[2])
					{
						htmltext = "2-04.htm";
					}
					else if (npcId == _npc[5])
					{
						htmltext = "5-01.htm";
					}
					break;
				}
				case 6:
				{
					if (npcId == _npc[5])
					{
						htmltext = "5-03.htm";
					}
					else if (npcId == _npc[6])
					{
						htmltext = "6-01.htm";
					}
					break;
				}
				case 7:
				{
					if (npcId == _npc[6])
					{
						htmltext = "6-02.htm";
					}
					break;
				}
				case 8:
				{
					if (npcId == _npc[6])
					{
						htmltext = "6-04.htm";
					}
					else if (npcId == _npc[7])
					{
						htmltext = "7-01.htm";
					}
					break;
				}
				case 9:
				{
					if (npcId == _npc[7])
					{
						htmltext = "7-05.htm";
					}
					break;
				}
				case 10:
				{
					if (npcId == _npc[7])
					{
						htmltext = "7-07.htm";
					}
					else if (npcId == _npc[3])
					{
						htmltext = "3-01.htm";
					}
					break;
				}
				case 11:
				case 12:
				{
					if (npcId == _npc[3])
					{
						if (hasQuestItems(player, _items[2]))
						{
							htmltext = "3-05.htm";
						}
						else
						{
							htmltext = "3-04.htm";
						}
					}
					break;
				}
				case 13:
				{
					if (npcId == _npc[3])
					{
						htmltext = "3-06.htm";
					}
					else if (npcId == _npc[8])
					{
						htmltext = "8-01.htm";
					}
					break;
				}
				case 14:
				{
					if (npcId == _npc[8])
					{
						htmltext = "8-03.htm";
					}
					else if (npcId == _npc[11])
					{
						htmltext = "11-01.htm";
					}
					break;
				}
				case 15:
				{
					if (npcId == _npc[11])
					{
						htmltext = "11-02.htm";
					}
					else if (npcId == _npc[9])
					{
						htmltext = "9-01.htm";
					}
					break;
				}
				case 16:
				{
					if (npcId == _npc[9])
					{
						htmltext = "9-02.htm";
					}
					break;
				}
				case 17:
				{
					if (npcId == _npc[9])
					{
						htmltext = "9-04.htm";
					}
					else if (npcId == _npc[10])
					{
						htmltext = "10-01.htm";
					}
					break;
				}
				case 18:
				{
					if (npcId == _npc[10])
					{
						htmltext = "10-05.htm";
					}
					break;
				}
				case 19:
				{
					if (npcId == _npc[10])
					{
						htmltext = "10-07.htm";
					}
					else if (npcId == _npc[0])
					{
						htmltext = "0-06.htm";
					}
					break;
				}
				case 20:
				{
					if (npcId == _npc[0])
					{
						if (player.getLevel() >= 76)
						{
							htmltext = "0-09.htm";
							
							st.exitQuest(false);
							addExpAndSp(player, 2299404, 0);
							giveAdena(player, 5000000, true);
							giveItems(player, 6622, 1); // XXX rewardItems?
							final int playerClass = getPlayerClass(player);
							final int prevClass = getPrevClass(player);
							player.setPlayerClass(playerClass);
							if (!player.isSubClassActive() && (player.getBaseClass() == prevClass))
							{
								player.setBaseClass(playerClass);
							}
							
							player.broadcastUserInfo();
							cast(npc, player, 4339, 1);
						}
						else
						{
							htmltext = "0-010.htm";
						}
					}
					break;
				}
			}
		}
		
		return htmltext;
	}
	
	private QuestState findQuest(Player player)
	{
		final QuestState st = getQuestState(player, false);
		if (st != null)
		{
			if (getId() == 68)
			{
				for (int q = 0; q < 2; q++)
				{
					if (player.getPlayerClass().getId() == QUEST_CLASSES[1][q])
					{
						return st;
					}
				}
			}
			else if (player.getPlayerClass().getId() == QUEST_CLASSES[getId() - 67][0])
			{
				return st;
			}
		}
		
		return null;
	}
	
	private void autoChat(Npc npc, String text)
	{
		npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.GENERAL, npc.getId(), text));
	}
	
	private void cast(Npc npc, Creature target, int skillId, int level)
	{
		target.broadcastSkillPacket(new MagicSkillUse(target, target, skillId, level, 6000, 1), target);
		target.broadcastSkillPacket(new MagicSkillUse(npc, npc, skillId, level, 6000, 1), npc);
	}
	
	private void addSpawn(QuestState st, Npc mob)
	{
		SPAWN_LIST.put(mob, st.getPlayer().getObjectId());
	}
	
	private static Npc findSpawn(Player player, Npc npc)
	{
		if (SPAWN_LIST.containsKey(npc) && (SPAWN_LIST.get(npc) == player.getObjectId()))
		{
			return npc;
		}
		
		return null;
	}
	
	private void deleteSpawn(Npc npc)
	{
		if (SPAWN_LIST.containsKey(npc))
		{
			SPAWN_LIST.remove(npc);
			npc.deleteMe();
		}
	}
	
	private QuestState findRightState(Npc npc)
	{
		Player player = null;
		QuestState st = null;
		if (SPAWN_LIST.containsKey(npc))
		{
			player = World.getInstance().getPlayer(SPAWN_LIST.get(npc));
			if (player != null)
			{
				st = player.getQuestState(getName());
			}
		}
		
		return st;
	}
	
	private int getPlayerClass(Player player)
	{
		if (player.getPlayerClass().getId() == 0x81)
		{
			return _classId[1];
		}
		
		return _classId[0];
	}
	
	private int getPrevClass(Player player)
	{
		if (player.getPlayerClass().getId() == 0x81)
		{
			if (_prevClass.length == 1)
			{
				return -1;
			}
			
			return _prevClass[1];
		}
		
		return _prevClass[0];
	}
	
	private void giveHalishaMark(QuestState st)
	{
		if (st.getInt("spawned") == 0)
		{
			final Player player = st.getPlayer();
			if (getQuestItemsCount(player, _items[3]) >= 700)
			{
				takeItems(player, _items[3], 20);
				final int xx = st.getPlayer().getX();
				final int yy = st.getPlayer().getY();
				final int zz = st.getPlayer().getZ();
				final Npc archon = addSpawn(_mob[1], xx, yy, zz, -1);
				addSpawn(st, archon);
				st.set("spawned", "1");
				startQuestTimer("Archon Hellisha has despawned", 600000, archon, player);
				autoChat(archon, _text[13].replace("PLAYERNAME", st.getPlayer().getName()));
				archon.asAttackable().addDamageHate(st.getPlayer(), 0, 99999);
				archon.getAI().setIntention(Intention.ATTACK, st.getPlayer(), null);
			}
			else
			{
				giveItems(player, _items[3], getRandom(1, 4));
			}
		}
	}
}
