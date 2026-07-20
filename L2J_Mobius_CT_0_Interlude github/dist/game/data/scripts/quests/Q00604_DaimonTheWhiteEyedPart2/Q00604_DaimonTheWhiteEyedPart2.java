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
package quests.Q00604_DaimonTheWhiteEyedPart2;

import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.managers.GlobalVariablesManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * Daimon the White-Eyed - Part 2 (604)
 * @author Adry_85
 */
public class Q00604_DaimonTheWhiteEyedPart2 extends Quest
{
	// NPCs
	private static final int DAIMONS_ALTAR = 31541;
	private static final int EYE_OF_ARGOS = 31683;
	
	// Raid Boss
	private static final int DAIMON_THE_WHITE_EYED = 25290;
	
	// Items
	private static final int UNFINISHED_SUMMON_CRYSTAL = 7192;
	private static final int SUMMON_CRYSTAL = 7193;
	private static final int ESSENCE_OF_DAIMON = 7194;
	
	// Rewards
	private static final int DYE_I2M2_C = 4595; // Greater Dye of INT <Int+2 Men-2>
	private static final int DYE_I2W2_C = 4596; // Greater Dye of INT <Int+2 Wit-2>
	private static final int DYE_M2I2_C = 4597; // Greater Dye of MEN <Men+2 Int-2>
	private static final int DYE_M2W2_C = 4598; // Greater Dye of MEN <Men+2 Wit-2>
	private static final int DYE_W2I2_C = 4599; // Greater Dye of WIT <Wit+2 Int-2>
	private static final int DYE_W2M2_C = 4600; // Greater Dye of WIT <Wit+2 Men-2>
	
	// Location
	private static final Location DAIMON_THE_WHITE_EYED_LOC = new Location(186320, -43904, -3175);
	
	// Misc
	private static final String DAIMON_THE_WHITE_EYED_RESPAWN_TIME = "DAIMON_THE_WHITE_EYED_RESPAWN_TIME";
	private static final int MIN_LEVEL = 73;
	
	public Q00604_DaimonTheWhiteEyedPart2()
	{
		super(604, "Daimon the White-Eyed - Part 2");
		addStartNpc(EYE_OF_ARGOS);
		addTalkId(EYE_OF_ARGOS, DAIMONS_ALTAR);
		addSpawnId(DAIMON_THE_WHITE_EYED);
		addKillId(DAIMON_THE_WHITE_EYED);
		registerQuestItems(SUMMON_CRYSTAL, ESSENCE_OF_DAIMON);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if ("DESPAWN".equals(event))
		{
			if (isDaimonSpawned())
			{
				npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getTemplate().getDisplayId(), "Can light exist without darkness?"));
				npc.deleteMe();
			}
			
			return super.onEvent(event, npc, player);
		}
		
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return null;
		}
		
		String htmltext = null;
		switch (event)
		{
			case "31683-04.htm":
			{
				takeItems(player, UNFINISHED_SUMMON_CRYSTAL, 1);
				qs.startQuest();
				qs.setMemoState(11);
				giveItems(player, SUMMON_CRYSTAL, 1);
				htmltext = event;
				break;
			}
			case "31683-07.html":
			{
				if (hasQuestItems(player, ESSENCE_OF_DAIMON))
				{
					final int reward;
					final int random = getRandom(1000);
					takeItems(player, ESSENCE_OF_DAIMON, 1);
					if (random < 167)
					{
						reward = DYE_I2M2_C;
					}
					else if (random < 334)
					{
						reward = DYE_I2W2_C;
					}
					else if (random < 501)
					{
						reward = DYE_M2I2_C;
					}
					else if (random < 668)
					{
						reward = DYE_M2W2_C;
					}
					else if (random < 835)
					{
						reward = DYE_W2I2_C;
					}
					else
					{
						reward = DYE_W2M2_C;
					}
					
					rewardItems(player, reward, 5);
					qs.exitQuest(true, true);
					htmltext = event;
				}
				else
				{
					htmltext = "31683-08.html";
				}
				break;
			}
			case "31541-02.html":
			{
				if (hasQuestItems(player, SUMMON_CRYSTAL))
				{
					if (!isDaimonSpawned())
					{
						takeItems(player, SUMMON_CRYSTAL, 1);
						htmltext = event;
						addSpawn(DAIMON_THE_WHITE_EYED, DAIMON_THE_WHITE_EYED_LOC);
						qs.setMemoState(21);
						qs.setCond(2, true);
					}
					else
					{
						htmltext = "31541-03.html";
					}
				}
				else
				{
					htmltext = "31541-04.html";
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		if (qs.isCreated())
		{
			if (player.getLevel() < MIN_LEVEL)
			{
				htmltext = "31683-01.htm";
			}
			else if (!hasQuestItems(player, UNFINISHED_SUMMON_CRYSTAL))
			{
				htmltext = "31683-02.htm";
			}
			else
			{
				htmltext = "31683-03.htm";
			}
		}
		else if (qs.isStarted())
		{
			if (npc.getId() == EYE_OF_ARGOS)
			{
				if (qs.isMemoState(11))
				{
					htmltext = "31683-05.html";
				}
				else if ((qs.getMemoState() >= 22) && hasQuestItems(player, ESSENCE_OF_DAIMON))
				{
					htmltext = "31683-06.html";
				}
				else
				{
					htmltext = "31683-09.html";
				}
			}
			else // DAIMONS_ALTAR
			{
				if (qs.isMemoState(11))
				{
					if (isDaimonSpawned())
					{
						htmltext = "31541-05.html";
					}
					else if (hasQuestItems(player, SUMMON_CRYSTAL))
					{
						htmltext = "31541-01.html";
					}
					else
					{
						htmltext = "31541-04.html";
					}
				}
				else if (qs.isMemoState(21))
				{
					if (!isDaimonSpawned())
					{
						if (hasQuestItems(player, SUMMON_CRYSTAL))
						{
							takeItems(player, SUMMON_CRYSTAL, 1);
							addSpawn(DAIMON_THE_WHITE_EYED, DAIMON_THE_WHITE_EYED_LOC);
							htmltext = "31541-02.html";
						}
						else
						{
							htmltext = "31541-04.html";
						}
					}
					else
					{
						htmltext = "31541-03.html";
					}
				}
				else if (qs.getMemoState() >= 22)
				{
					htmltext = "31541-05.html";
				}
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		startQuestTimer("DESPAWN", 1200000, npc, null);
		npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getTemplate().getDisplayId(), "Who is calling me?"));
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		executeForEachPlayer(killer, npc, isSummon, true, false);
		
		final int respawnMinDelay = (int) (43200000 * NpcConfig.RAID_MIN_RESPAWN_MULTIPLIER);
		final int respawnMaxDelay = (int) (129600000 * NpcConfig.RAID_MAX_RESPAWN_MULTIPLIER);
		final int respawnDelay = getRandom(respawnMinDelay, respawnMaxDelay);
		GlobalVariablesManager.getInstance().set(DAIMON_THE_WHITE_EYED_RESPAWN_TIME, System.currentTimeMillis() + respawnDelay);
	}
	
	@Override
	public void actionForEachPlayer(Player player, Npc npc, boolean isSummon)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && (qs.getMemoState() >= 11) && (qs.getMemoState() <= 21) && LocationUtil.checkIfInRange(PlayerConfig.ALT_PARTY_RANGE, npc, player, false))
		{
			// if (hasQuestItems(player, ESSENCE_OF_DAIMON))
			// {
			qs.setCond(3, true);
			qs.setMemoState(22);
			// }
			
			giveItems(player, ESSENCE_OF_DAIMON, 1);
			playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
	}
	
	private static boolean isDaimonSpawned()
	{
		if (System.currentTimeMillis() > GlobalVariablesManager.getInstance().getLong(DAIMON_THE_WHITE_EYED_RESPAWN_TIME, 0))
		{
			return World.getInstance().getVisibleObjects().stream().anyMatch(object -> object.getId() == DAIMON_THE_WHITE_EYED);
		}
		
		return true;
	}
}
