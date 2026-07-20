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
package quests.Q00421_LittleWingsBigAdventure;

import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.actor.instance.Monster;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.enums.ChatType;

/**
 * iCond is an internal variable, used because cond isn't developped on that quest (only 3 states) :
 * <ul>
 * <li>1-3 leads initial mimyu behavior ;</li>
 * <li>used for leaves support as mask : 4, 8, 16 or 32 = 60 overall ;</li>
 * <li>63 becomes the "marker" to get back to mimyu (60 + 3), meaning you hitted the 4 trees ;</li>
 * <li>setted to 100 if mimyu check is ok.</li>
 * </ul>
 */
public class Q00421_LittleWingsBigAdventure extends Quest
{
	// NPCs
	private static final int CRONOS = 30610;
	private static final int MIMYU = 30747;
	
	// Item
	private static final int FAIRY_LEAF = 4325;
	
	public Q00421_LittleWingsBigAdventure()
	{
		super(421, "Little Wing's Big Adventure");
		registerQuestItems(FAIRY_LEAF);
		addStartNpc(CRONOS);
		addTalkId(CRONOS, MIMYU);
		addAttackId(27185, 27186, 27187, 27188);
		addKillId(27185, 27186, 27187, 27188);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final QuestState st = getQuestState(player, false);
		if (st == null)
		{
			return htmltext;
		}
		
		switch (event)
		{
			case "30610-06.htm":
			{
				if ((getQuestItemsCount(player, 3500) + getQuestItemsCount(player, 3501) + getQuestItemsCount(player, 3502)) == 1)
				{
					// Find the level of the flute.
					for (int i = 3500; i < 3503; i++)
					{
						final Item item = player.getInventory().getItemByItemId(i);
						if ((item != null) && (item.getEnchantLevel() >= 55))
						{
							st.startQuest();
							st.set("iCond", "1");
							st.set("summonOid", String.valueOf(item.getObjectId()));
							return "30610-05.htm";
						}
					}
				}
				
				// Exit quest if you got more than one flute, or the flute level doesn't meat requirements.
				st.exitQuest(true);
				break;
			}
			case "30747-02.htm":
			{
				final Summon summon = player.getSummon();
				if (summon != null)
				{
					htmltext = (summon.getControlObjectId() == st.getInt("summonOid")) ? "30747-04.htm" : "30747-03.htm";
				}
				break;
			}
			case "30747-05.htm":
			{
				final Summon summon = player.getSummon();
				if ((summon == null) || (summon.getControlObjectId() != st.getInt("summonOid")))
				{
					htmltext = "30747-06.htm";
				}
				else
				{
					st.setCond(2, true);
					st.set("iCond", "3");
					giveItems(player, FAIRY_LEAF, 4);
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = getQuestState(player, true);
		
		switch (st.getState())
		{
			case State.CREATED:
			{
				// Wrong level.
				if (player.getLevel() < 45)
				{
					htmltext = "30610-01.htm";
				}
				else if ((getQuestItemsCount(player, 3500) + getQuestItemsCount(player, 3501) + getQuestItemsCount(player, 3502)) != 1)
				{
					htmltext = "30610-02.htm";
				}
				else
				{
					// Find the level of the hatchling.
					for (int i = 3500; i < 3503; i++)
					{
						final Item item = player.getInventory().getItemByItemId(i);
						if ((item != null) && (item.getEnchantLevel() >= 55))
						{
							return "30610-04.htm";
						}
					}
					
					// Invalid level.
					htmltext = "30610-03.htm";
				}
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case CRONOS:
					{
						htmltext = "30610-07.htm";
						break;
					}
					case MIMYU:
					{
						final int id = st.getInt("iCond");
						if (id == 1)
						{
							htmltext = "30747-01.htm";
							st.set("iCond", "2");
						}
						else if (id == 2)
						{
							final Summon summon = player.getSummon();
							htmltext = (summon != null) ? ((summon.getControlObjectId() == st.getInt("summonOid")) ? "30747-04.htm" : "30747-03.htm") : "30747-02.htm";
						}
						else if (id == 3)
						{
							htmltext = "30747-07.htm";
						}
						else if ((id > 3) && (id < 63))
						{
							htmltext = "30747-11.htm";
						}
						else if (id == 63) // Did all trees, no more leaves.
						{
							final Summon summon = player.getSummon();
							if (summon == null)
							{
								return "30747-12.htm";
							}
							
							if (summon.getControlObjectId() != st.getInt("summonOid"))
							{
								return "30747-14.htm";
							}
							
							htmltext = "30747-13.htm";
							st.set("iCond", "100");
						}
						else if (id == 100) // Spoke with the Fairy.
						{
							final Summon summon = player.getSummon();
							if ((summon != null) && (summon.getControlObjectId() == st.getInt("summonOid")))
							{
								return "30747-15.htm";
							}
							
							if ((getQuestItemsCount(player, 3500) + getQuestItemsCount(player, 3501) + getQuestItemsCount(player, 3502)) > 1)
							{
								return "30747-17.htm";
							}
							
							for (int i = 3500; i < 3503; i++)
							{
								final Item item = player.getInventory().getItemByItemId(i);
								if ((item != null) && (item.getObjectId() == st.getInt("summonOid")))
								{
									takeItems(player, i, 1);
									giveItems(player, i + 922, 1, item.getEnchantLevel()); // TODO: rebuild entirely pet system in order enchant is given a fuck. Supposed to give an item level XX for a flute level XX.
									st.exitQuest(true, true);
									return "30747-16.htm";
								}
							}
							
							// Curse if the registered objectId is the wrong one (switch flutes).
							htmltext = "30747-18.htm";
							
							final Skill skill = SkillData.getInstance().getSkill(4167, 1);
							if ((skill != null) && player.isAffectedBySkill(skill.getId()))
							{
								skill.applyEffects(npc, player);
							}
						}
						break;
					}
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isPet)
	{
		// Minions scream no matter current quest state.
		if (npc.asMonster().hasMinions())
		{
			for (Monster ghost : npc.asMonster().getMinionList().getSpawnedMinions())
			{
				if (!ghost.isDead() && (getRandom(100) < 1))
				{
					ghost.broadcastSay(ChatType.GENERAL, "We must protect the fairy tree!");
				}
			}
		}
		
		// Condition required : 2.
		final QuestState st = getQuestState(attacker, false);
		if ((st == null) || !st.isCond(2))
		{
			return;
		}
		
		// A pet was the attacker, and the objectId is the good one.
		if (isPet && (attacker.getSummon().getControlObjectId() == st.getInt("summonOid")))
		{
			// Random luck is reached and you still have some leaves ; go further.
			if ((getRandom(100) < 1) && hasQuestItems(st.getPlayer(), FAIRY_LEAF))
			{
				final int idMask = (int) Math.pow(2, (npc.getId() - 27182) - 1);
				final int iCond = st.getInt("iCond");
				if ((iCond | idMask) != iCond)
				{
					st.set("iCond", String.valueOf(iCond | idMask));
					npc.broadcastSay(ChatType.GENERAL, "Give me a Fairy Leaf...!");
					takeItems(st.getPlayer(), FAIRY_LEAF, 1);
					npc.broadcastSay(ChatType.GENERAL, "Leave now, before you incur the wrath of the guardian ghost...");
					
					// Four leafs have been used ; update quest state.
					if (st.getInt("iCond") == 63)
					{
						st.setCond(3, true);
					}
					else
					{
						playSound(st.getPlayer(), QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isPet)
	{
		final Creature originalKiller = isPet ? killer.getSummon() : killer;
		
		// Tree curses the killer.
		if ((getRandom(100) < 30) && (originalKiller != null))
		{
			final Skill skill = SkillData.getInstance().getSkill(4243, 1);
			if ((skill != null) && originalKiller.isAffectedBySkill(skill.getId()))
			{
				skill.applyEffects(npc, originalKiller);
			}
		}
		
		// Spawn 20 ghosts, attacking the killer.
		for (int i = 0; i < 20; i++)
		{
			final Attackable newNpc = addSpawn(27189, npc.getX(), npc.getY(), npc.getZ(), getRandom(65536), true, 300000).asAttackable();
			newNpc.setRunning();
			newNpc.addDamageHate(originalKiller, 0, 999);
			newNpc.getAI().setIntention(Intention.ATTACK, originalKiller);
		}
	}
}
