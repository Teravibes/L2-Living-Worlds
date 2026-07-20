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

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.xml.CategoryData;
import org.l2jmobius.gameserver.data.xml.ClassListData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.SiegeManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.actor.holders.player.ClassType;
import org.l2jmobius.gameserver.model.actor.holders.player.SubClassHolder;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.clan.Clan.SubPledge;
import org.l2jmobius.gameserver.model.clan.ClanMember;
import org.l2jmobius.gameserver.model.olympiad.Olympiad;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.model.skill.enums.AcquireSkillType;
import org.l2jmobius.gameserver.model.skill.holders.SkillLearn;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.AcquireSkillList;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillLaunched;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * @version $Revision: 1.4.2.3.2.8 $ $Date: 2005/03/29 23:15:15 $
 */
public class VillageMaster extends Folk
{
	private static final Logger LOGGER = Logger.getLogger(VillageMaster.class.getName());
	
	private static final Set<PlayerClass> mainSubclassSet;
	private static final Set<PlayerClass> neverSubclassed = EnumSet.of(PlayerClass.OVERLORD, PlayerClass.WARSMITH);
	private static final Set<PlayerClass> subclasseSet1 = EnumSet.of(PlayerClass.DARK_AVENGER, PlayerClass.PALADIN, PlayerClass.TEMPLE_KNIGHT, PlayerClass.SHILLIEN_KNIGHT);
	private static final Set<PlayerClass> subclasseSet2 = EnumSet.of(PlayerClass.TREASURE_HUNTER, PlayerClass.ABYSS_WALKER, PlayerClass.PLAINS_WALKER);
	private static final Set<PlayerClass> subclasseSet3 = EnumSet.of(PlayerClass.HAWKEYE, PlayerClass.SILVER_RANGER, PlayerClass.PHANTOM_RANGER);
	private static final Set<PlayerClass> subclasseSet4 = EnumSet.of(PlayerClass.WARLOCK, PlayerClass.ELEMENTAL_SUMMONER, PlayerClass.PHANTOM_SUMMONER);
	private static final Set<PlayerClass> subclasseSet5 = EnumSet.of(PlayerClass.SORCERER, PlayerClass.SPELLSINGER, PlayerClass.SPELLHOWLER);
	private static final EnumMap<PlayerClass, Set<PlayerClass>> subclassSetMap = new EnumMap<>(PlayerClass.class);
	static
	{
		final Set<PlayerClass> subclasses = CategoryData.getInstance().getCategoryByType(CategoryType.THIRD_CLASS_GROUP).stream().map(PlayerClass::getPlayerClass).collect(Collectors.toSet());
		subclasses.removeAll(neverSubclassed);
		mainSubclassSet = subclasses;
		subclassSetMap.put(PlayerClass.DARK_AVENGER, subclasseSet1);
		subclassSetMap.put(PlayerClass.PALADIN, subclasseSet1);
		subclassSetMap.put(PlayerClass.TEMPLE_KNIGHT, subclasseSet1);
		subclassSetMap.put(PlayerClass.SHILLIEN_KNIGHT, subclasseSet1);
		subclassSetMap.put(PlayerClass.TREASURE_HUNTER, subclasseSet2);
		subclassSetMap.put(PlayerClass.ABYSS_WALKER, subclasseSet2);
		subclassSetMap.put(PlayerClass.PLAINS_WALKER, subclasseSet2);
		subclassSetMap.put(PlayerClass.HAWKEYE, subclasseSet3);
		subclassSetMap.put(PlayerClass.SILVER_RANGER, subclasseSet3);
		subclassSetMap.put(PlayerClass.PHANTOM_RANGER, subclasseSet3);
		subclassSetMap.put(PlayerClass.WARLOCK, subclasseSet4);
		subclassSetMap.put(PlayerClass.ELEMENTAL_SUMMONER, subclasseSet4);
		subclassSetMap.put(PlayerClass.PHANTOM_SUMMONER, subclasseSet4);
		subclassSetMap.put(PlayerClass.SORCERER, subclasseSet5);
		subclassSetMap.put(PlayerClass.SPELLSINGER, subclasseSet5);
		subclassSetMap.put(PlayerClass.SPELLHOWLER, subclasseSet5);
	}
	
	/**
	 * Creates a village master.
	 * @param template the village master NPC template
	 */
	public VillageMaster(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.VillageMaster);
	}
	
	@Override
	public String getHtmlPath(int npcId, int value)
	{
		String pom = "";
		if (value == 0)
		{
			pom = Integer.toString(npcId);
		}
		else
		{
			pom = npcId + "-" + value;
		}
		
		return "data/html/villagemaster/" + pom + ".htm";
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		final String[] commandStr = command.split(" ");
		final String actualCommand = commandStr[0]; // Get actual command
		String cmdParams = "";
		String cmdParams2 = "";
		if (commandStr.length >= 2)
		{
			cmdParams = commandStr[1];
		}
		
		if (commandStr.length >= 3)
		{
			cmdParams2 = commandStr[2];
		}
		
		if (actualCommand.equalsIgnoreCase("create_clan"))
		{
			if (cmdParams.isEmpty())
			{
				player.sendPacket(SystemMessageId.PLEASE_CREATE_YOUR_CLAN_NAME);
				return;
			}
			
			if (!cmdParams2.isEmpty() || !isValidName(cmdParams))
			{
				player.sendPacket(SystemMessageId.CLAN_NAME_IS_INVALID);
				return;
			}
			
			ClanTable.getInstance().createClan(player, cmdParams);
		}
		else if (actualCommand.equalsIgnoreCase("create_academy"))
		{
			if (cmdParams.isEmpty())
			{
				player.sendPacket(SystemMessageId.PLEASE_ENTER_THE_NAME_OF_THE_CLAN_ACADEMY);
				return;
			}
			
			createSubPledge(player, cmdParams, null, Clan.SUBUNIT_ACADEMY, 5);
		}
		else if (actualCommand.equalsIgnoreCase("rename_pledge"))
		{
			if (cmdParams.isEmpty() || cmdParams2.isEmpty())
			{
				return;
			}
			
			renameSubPledge(player, Integer.parseInt(cmdParams), cmdParams2);
		}
		else if (actualCommand.equalsIgnoreCase("create_royal"))
		{
			if (cmdParams.isEmpty())
			{
				return;
			}
			
			createSubPledge(player, cmdParams, cmdParams2, Clan.SUBUNIT_ROYAL1, 6);
		}
		else if (actualCommand.equalsIgnoreCase("create_knight"))
		{
			if (cmdParams.isEmpty())
			{
				return;
			}
			
			createSubPledge(player, cmdParams, cmdParams2, Clan.SUBUNIT_KNIGHT1, 7);
		}
		else if (actualCommand.equalsIgnoreCase("assign_subpl_leader"))
		{
			if (cmdParams.isEmpty())
			{
				return;
			}
			
			assignSubPledgeLeader(player, cmdParams, cmdParams2);
		}
		else if (actualCommand.equalsIgnoreCase("create_ally"))
		{
			if (cmdParams.isEmpty())
			{
				return;
			}
			
			if (player.getClan() == null)
			{
				player.sendPacket(SystemMessageId.ONLY_CLAN_LEADERS_MAY_CREATE_ALLIANCES);
			}
			else
			{
				player.getClan().createAlly(player, cmdParams);
			}
		}
		else if (actualCommand.equalsIgnoreCase("dissolve_ally"))
		{
			player.getClan().dissolveAlly(player);
		}
		else if (actualCommand.equalsIgnoreCase("dissolve_clan"))
		{
			dissolveClan(player, player.getClanId());
		}
		else if (actualCommand.equalsIgnoreCase("change_clan_leader"))
		{
			if (cmdParams.isEmpty())
			{
				return;
			}
			
			if (!player.isClanLeader())
			{
				player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
				return;
			}
			
			if (player.getName().equalsIgnoreCase(cmdParams))
			{
				return;
			}
			
			final Clan clan = player.getClan();
			final ClanMember member = clan.getClanMember(cmdParams);
			if (member == null)
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.S1_DOES_NOT_EXIST);
				sm.addString(cmdParams);
				player.sendPacket(sm);
				return;
			}
			
			if (!member.isOnline())
			{
				player.sendPacket(SystemMessageId.THAT_PLAYER_IS_NOT_CURRENTLY_ONLINE);
				return;
			}
			
			// To avoid clans with null clan leader, academy members shouldn't be eligible for clan leader.
			if (member.getPlayer().isAcademyMember())
			{
				player.sendPacket(SystemMessageId.THAT_PRIVILEGE_CANNOT_BE_GRANTED_TO_A_CLAN_ACADEMY_MEMBER);
				return;
			}
			
			if (PlayerConfig.ALT_CLAN_LEADER_INSTANT_ACTIVATION)
			{
				clan.setNewLeader(member);
			}
			else
			{
				final NpcHtmlMessage msg = new NpcHtmlMessage(getObjectId());
				if (clan.getNewLeaderId() == 0)
				{
					clan.setNewLeaderId(member.getObjectId(), true);
					msg.setFile(player, "data/scripts/village_master/ClanMaster/9000-07-success.htm");
				}
				else
				{
					msg.setFile(player, "data/scripts/village_master/ClanMaster/9000-07-in-progress.htm");
				}
				
				player.sendPacket(msg);
			}
		}
		else if (actualCommand.equalsIgnoreCase("cancel_clan_leader_change"))
		{
			if (!player.isClanLeader())
			{
				player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
				return;
			}
			
			final Clan clan = player.getClan();
			final NpcHtmlMessage msg = new NpcHtmlMessage(getObjectId());
			if (clan.getNewLeaderId() != 0)
			{
				clan.setNewLeaderId(0, true);
				msg.setFile(player, "data/scripts/village_master/ClanMaster/9000-07-canceled.htm");
			}
			else
			{
				msg.setHtml("<html><body>You don't have clan leader delegation applications submitted yet!</body></html>");
			}
			
			player.sendPacket(msg);
		}
		else if (actualCommand.equalsIgnoreCase("recover_clan"))
		{
			recoverClan(player, player.getClanId());
		}
		else if (actualCommand.equalsIgnoreCase("increase_clan_level"))
		{
			if (player.getClan().levelUpClan(player))
			{
				player.broadcastSkillPacket(new MagicSkillUse(player, 5103, 1, 0, 0), player);
				player.broadcastSkillPacket(new MagicSkillLaunched(player, 5103, 1), player);
			}
		}
		else if (actualCommand.equalsIgnoreCase("learn_clan_skills"))
		{
			showPledgeSkillList(player);
		}
		else if (command.startsWith("Subclass"))
		{
			// Subclasses may not be changed while a skill is in use.
			if (player.isCastingNow() || player.isAllSkillsDisabled())
			{
				player.sendPacket(SystemMessageId.SUB_CLASSES_MAY_NOT_BE_CREATED_OR_CHANGED_WHILE_A_SKILL_IS_IN_USE);
				return;
			}
			
			if (Olympiad.getInstance().isRegisteredInComp(player))
			{
				Olympiad.getInstance().unRegisterNoble(player);
			}
			
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			
			// Subclasses may not be changed while a summon is active.
			if (player.hasSummon())
			{
				html.setFile(player, "data/html/villagemaster/SubClass_NoSummon.htm");
				player.sendPacket(html);
				return;
			}
			
			// Subclasses may not be changed while you have exceeded your inventory limit.
			if (!player.isInventoryUnder90(true))
			{
				player.sendPacket(SystemMessageId.A_SUB_CLASS_CANNOT_BE_CREATED_OR_CHANGED_BECAUSE_YOU_HAVE_EXCEEDED_YOUR_INVENTORY_LIMIT);
				return;
			}
			
			// Subclasses may not be changed while a you are over your weight limit.
			if (player.getWeightPenalty() >= 2)
			{
				player.sendPacket(SystemMessageId.A_SUB_CLASS_CANNOT_BE_CREATED_OR_CHANGED_WHILE_YOU_ARE_OVER_YOUR_WEIGHT_LIMIT);
				return;
			}
			
			int cmdChoice = 0;
			int paramOne = 0;
			int paramTwo = 0;
			try
			{
				cmdChoice = Integer.parseInt(command.substring(9, 10).trim());
				int endIndex = command.indexOf(' ', 11);
				if (endIndex == -1)
				{
					endIndex = command.length();
				}
				
				if (command.length() > 11)
				{
					paramOne = Integer.parseInt(command.substring(11, endIndex).trim());
					if (command.length() > endIndex)
					{
						paramTwo = Integer.parseInt(command.substring(endIndex).trim());
					}
				}
			}
			catch (Exception nfe)
			{
				LOGGER.warning(VillageMaster.class.getName() + ": Wrong numeric values for command " + command);
			}
			
			Set<PlayerClass> subsAvailable = null;
			switch (cmdChoice)
			{
				case 0: // Subclass change menu
				{
					html.setFile(player, getSubClassMenu(player.getRace()));
					break;
				}
				case 1: // Add Subclass - Initial
				{
					// Avoid giving player an option to add a new sub class, if they have max sub-classes already.
					if (player.getTotalSubClasses() >= PlayerConfig.MAX_SUBCLASS)
					{
						html.setFile(player, getSubClassFail());
						break;
					}
					
					subsAvailable = getAvailableSubClasses(player);
					if ((subsAvailable != null) && !subsAvailable.isEmpty())
					{
						html.setFile(player, "data/html/villagemaster/SubClass_Add.htm");
						final StringBuilder content1 = new StringBuilder(200);
						for (PlayerClass subClass : subsAvailable)
						{
							content1.append("<a action=\"bypass npc_%objectId%_Subclass 4 " + subClass.getId() + "\" msg=\"1268;" + ClassListData.getInstance().getClass(subClass.getId()).getClassName() + "\">" + ClassListData.getInstance().getClass(subClass.getId()).getClassName() + "</a><br>");
						}
						
						html.replace("%list%", content1.toString());
					}
					else
					{
						player.sendMessage("There are no sub classes available at this time.");
						return;
					}
					break;
				}
				case 2: // Change Class - Initial
				{
					if (player.getSubClasses().isEmpty())
					{
						html.setFile(player, "data/html/villagemaster/SubClass_ChangeNo.htm");
					}
					else
					{
						final StringBuilder content2 = new StringBuilder(200);
						content2.append("<a action=\"bypass -h npc_%objectId%_Subclass 5 0\">" + ClassListData.getInstance().getClass(player.getBaseClass()).getClassName() + "</a><br>");
						
						for (Iterator<SubClassHolder> subList = iterSubClasses(player); subList.hasNext();)
						{
							final SubClassHolder subClass = subList.next();
							content2.append("<a action=\"bypass -h npc_%objectId%_Subclass 5 " + subClass.getClassIndex() + "\">" + ClassListData.getInstance().getClass(subClass.getId()).getClassName() + "</a><br>");
						}
						
						if (content2.length() > 0)
						{
							html.setFile(player, "data/html/villagemaster/SubClass_Change.htm");
							html.replace("%list%", content2.toString());
						}
						else
						{
							html.setFile(player, "data/html/villagemaster/SubClass_ChangeNotFound.htm");
						}
					}
					break;
				}
				case 3: // Change/Cancel Subclass - Initial
				{
					if ((player.getSubClasses() == null) || player.getSubClasses().isEmpty())
					{
						html.setFile(player, "data/html/villagemaster/SubClass_ModifyEmpty.htm");
						break;
					}
					
					// custom value
					if (player.getTotalSubClasses() > 3)
					{
						html.setFile(player, "data/html/villagemaster/SubClass_ModifyCustom.htm");
						final StringBuilder content3 = new StringBuilder(200);
						int classIndex = 1;
						for (Iterator<SubClassHolder> subList = iterSubClasses(player); subList.hasNext();)
						{
							final SubClassHolder subClass = subList.next();
							content3.append("Sub-class " + classIndex++ + "<br><a action=\"bypass -h npc_%objectId%_Subclass 6 " + subClass.getClassIndex() + "\">" + ClassListData.getInstance().getClass(subClass.getId()).getClassName() + "</a><br>");
						}
						
						html.replace("%list%", content3.toString());
					}
					else
					{
						// retail html contain only 3 subclasses
						html.setFile(player, "data/html/villagemaster/SubClass_Modify.htm");
						if (player.getSubClasses().containsKey(1))
						{
							html.replace("%sub1%", ClassListData.getInstance().getClass(player.getSubClasses().get(1).getId()).getClassName());
						}
						else
						{
							html.replace("<a action=\"bypass npc_%objectId%_Subclass 6 1\">%sub1%</a><br>", "");
						}
						
						if (player.getSubClasses().containsKey(2))
						{
							html.replace("%sub2%", ClassListData.getInstance().getClass(player.getSubClasses().get(2).getId()).getClassName());
						}
						else
						{
							html.replace("<a action=\"bypass npc_%objectId%_Subclass 6 2\">%sub2%</a><br>", "");
						}
						
						if (player.getSubClasses().containsKey(3))
						{
							html.replace("%sub3%", ClassListData.getInstance().getClass(player.getSubClasses().get(3).getId()).getClassName());
						}
						else
						{
							html.replace("<a action=\"bypass npc_%objectId%_Subclass 6 3\">%sub3%</a><br>", "");
						}
					}
					break;
				}
				case 4: // Add Subclass - Action (Subclass 4 x[x])
				{
					/**
					 * If the character is less than level 75 on any of their previously chosen classes then disallow them to change to their most recently added sub-class choice.
					 */
					if (!player.getClient().getFloodProtectors().canChangeSubclass())
					{
						return;
					}
					
					boolean allowAddition = true;
					if (player.getTotalSubClasses() >= PlayerConfig.MAX_SUBCLASS)
					{
						allowAddition = false;
					}
					
					if (player.getLevel() < 75)
					{
						allowAddition = false;
					}
					
					if (allowAddition && !player.getSubClasses().isEmpty())
					{
						for (Iterator<SubClassHolder> subList = iterSubClasses(player); subList.hasNext();)
						{
							final SubClassHolder subClass = subList.next();
							if (subClass.getLevel() < 75)
							{
								allowAddition = false;
								break;
							}
						}
					}
					
					/**
					 * If quest checking is enabled, verify if the character has completed the Mimir's Elixir (Path to Subclass) and Fate's Whisper (A Grade Weapon) quests by checking for instances of their unique reward items. If they both exist, remove both unique items and continue with adding
					 * the sub-class.
					 */
					if (allowAddition && !PlayerConfig.ALT_GAME_SUBCLASS_WITHOUT_QUESTS)
					{
						allowAddition = checkQuests(player);
					}
					
					if (allowAddition && isValidNewSubClass(player, paramOne))
					{
						if (!player.addSubClass(paramOne, player.getTotalSubClasses() + 1))
						{
							return;
						}
						
						player.setActiveClass(player.getTotalSubClasses());
						
						html.setFile(player, "data/html/villagemaster/SubClass_AddOk.htm");
						player.sendPacket(SystemMessageId.THE_NEW_SUB_CLASS_HAS_BEEN_ADDED); // Subclass added.
					}
					else
					{
						html.setFile(player, getSubClassFail());
					}
					break;
				}
				case 5: // Change Class - Action
				{
					/**
					 * If the character is less than level 75 on any of their previously chosen classes then disallow them to change to their most recently added sub-class choice. Note: paramOne = classIndex
					 */
					if (!player.getClient().getFloodProtectors().canChangeSubclass())
					{
						return;
					}
					
					if (player.getClassIndex() == paramOne)
					{
						html.setFile(player, "data/html/villagemaster/SubClass_Current.htm");
						break;
					}
					
					player.setActiveClass(paramOne);
					player.sendPacket(SystemMessageId.THE_TRANSFER_OF_SUB_CLASS_HAS_BEEN_COMPLETED); // Transfer completed.
					player.broadcastPacket(new MagicSkillUse(player, 5103, 1, 0, 0));
					return;
				}
				case 6: // Change/Cancel Subclass - Choice
				{
					// validity check
					if ((paramOne < 1) || (paramOne > PlayerConfig.MAX_SUBCLASS))
					{
						return;
					}
					
					subsAvailable = getAvailableSubClasses(player);
					
					// another validity check
					if ((subsAvailable == null) || subsAvailable.isEmpty())
					{
						// TODO: Retail message
						player.sendMessage("There are no sub classes available at this time.");
						return;
					}
					
					final StringBuilder content6 = new StringBuilder(200);
					for (PlayerClass subClass : subsAvailable)
					{
						content6.append("<a action=\"bypass npc_%objectId%_Subclass 7 " + paramOne + " " + subClass.getId() + "\" msg=\"1445;\">" + ClassListData.getInstance().getClass(subClass.getId()).getClassName() + "</a><br>");
					}
					
					switch (paramOne)
					{
						case 1:
						{
							html.setFile(player, "data/html/villagemaster/SubClass_ModifyChoice1.htm");
							break;
						}
						case 2:
						{
							html.setFile(player, "data/html/villagemaster/SubClass_ModifyChoice2.htm");
							break;
						}
						case 3:
						{
							html.setFile(player, "data/html/villagemaster/SubClass_ModifyChoice3.htm");
							break;
						}
						default:
						{
							html.setFile(player, "data/html/villagemaster/SubClass_ModifyChoice.htm");
						}
					}
					
					html.replace("%list%", content6.toString());
					break;
				}
				case 7: // Change Subclass - Action
				{
					/**
					 * Warning: the information about this subclass will be removed from the subclass list even if false!
					 */
					if (!player.getClient().getFloodProtectors().canChangeSubclass())
					{
						return;
					}
					
					if (!isValidNewSubClass(player, paramTwo))
					{
						return;
					}
					
					if (player.modifySubClass(paramOne, paramTwo))
					{
						player.abortCast();
						player.stopAllEffectsExceptThoseThatLastThroughDeath(); // all effects from old subclass stopped!
						player.stopAllEffectsNotStayOnSubclassChange();
						player.stopCubics();
						player.setActiveClass(paramOne);
						
						html.setFile(player, "data/html/villagemaster/SubClass_ModifyOk.htm");
						html.replace("%name%", ClassListData.getInstance().getClass(paramTwo).getClassName());
						player.sendPacket(SystemMessageId.THE_NEW_SUB_CLASS_HAS_BEEN_ADDED); // Subclass added.
						player.broadcastPacket(new MagicSkillUse(player, 5103, 1, 0, 0));
					}
					else
					{
						/**
						 * This isn't good! modifySubClass() removed subclass from memory we must update _classIndex! Else IndexOutOfBoundsException can turn up some place down the line along with other seemingly unrelated problems.
						 */
						player.setActiveClass(0); // Also updates _classIndex plus switching _classid to baseclass.
						player.sendMessage("The sub class could not be added, you have been reverted to your base class.");
						return;
					}
					break;
				}
			}
			
			html.replace("%objectId%", String.valueOf(getObjectId()));
			player.sendPacket(html);
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
	
	protected String getSubClassMenu(Race race)
	{
		return "data/html/villagemaster/SubClass.htm";
	}
	
	protected String getSubClassFail()
	{
		return "data/html/villagemaster/SubClass_Fail.htm";
	}
	
	protected boolean checkQuests(Player player)
	{
		// Noble players can add Sub-Classes without quests
		if (player.isNoble())
		{
			return true;
		}
		
		QuestState qs = player.getQuestState("Q00234_FatesWhisper");
		if ((qs == null) || !qs.isCompleted())
		{
			return false;
		}
		
		qs = player.getQuestState("Q00235_MimirsElixir");
		if ((qs == null) || !qs.isCompleted())
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * @return the village master race
	 */
	private final Race getVillageMasterRace()
	{
		return getTemplate().getRace();
	}
	
	private ClassType getVillageMasterTeachType()
	{
		if (this instanceof VillageMasterPriest)
		{
			return ClassType.PRIEST;
		}
		
		if (this instanceof VillageMasterMystic)
		{
			return ClassType.MYSTIC;
		}
		
		return ClassType.FIGTHER;
	}
	
	private Set<PlayerClass> getAvailableSubClasses(Player player)
	{
		final Set<PlayerClass> availSubs = getSubclasses(player, player.getBaseClass());
		final Race npcRace = getVillageMasterRace();
		final ClassType npcTeachType = getVillageMasterTeachType();
		boolean everywhereEnabled = PlayerConfig.ALT_GAME_SUBCLASS_EVERYWHERE;
		
		if (availSubs != null)
		{
			// Create a copy of the set to avoid ConcurrentModificationException.
			final Set<PlayerClass> copySubs = new HashSet<>(availSubs);
			for (PlayerClass availSub : copySubs)
			{
				// Remove subclasses already chosen by the player.
				for (SubClassHolder subClass : player.getSubClasses().values())
				{
					if (subClass.getId() == availSub.ordinal())
					{
						availSubs.remove(availSub);
						break;
					}
				}
				
				// Remove subclasses unavailable due to previous choices or base class.
				final Iterator<SubClassHolder> subListIterator = iterSubClasses(player);
				while (subListIterator.hasNext())
				{
					final SubClassHolder prevSubClass = subListIterator.next();
					int subClassId = prevSubClass.getId();
					if (subClassId >= 88)
					{
						subClassId = PlayerClass.getPlayerClass(subClassId).getParent().getId();
					}
					
					if ((availSub.ordinal() == subClassId) || (availSub.ordinal() == player.getBaseClass()))
					{
						availSubs.remove(availSub);
						break;
					}
				}
				
				// Check if subclass restrictions based on race should be applied everywhere.
				if (!everywhereEnabled)
				{
					// Apply race restrictions based on the village master's race.
					if (((npcRace == Race.HUMAN) || (npcRace == Race.ELF)))
					{
						// If the master is human or light elf, ensure that fighter-type masters only teach fighter classes, and priest-type masters only teach priest classes etc.
						if (!availSub.isOfType(npcTeachType) || (!availSub.isOfRace(Race.HUMAN) && !availSub.isOfRace(Race.ELF)))
						{
							availSubs.remove(availSub);
						}
					}
					else if ((npcRace != Race.HUMAN) && (npcRace != Race.ELF) && !availSub.isOfRace(npcRace))
					{
						// If the master is not human and not light elf, then remove any classes not of the same race as the master.
						availSubs.remove(availSub);
					}
				}
			}
		}
		
		return availSubs;
	}
	
	public final Set<PlayerClass> getSubclasses(Player player, int classId)
	{
		Set<PlayerClass> subclasses = null;
		final PlayerClass pClass = PlayerClass.getPlayerClass(classId);
		if (CategoryData.getInstance().isInCategory(CategoryType.THIRD_CLASS_GROUP, classId) || (CategoryData.getInstance().isInCategory(CategoryType.FOURTH_CLASS_GROUP, classId)))
		{
			subclasses = EnumSet.copyOf(mainSubclassSet);
			subclasses.remove(pClass);
			
			if (player.getRace() == Race.ELF)
			{
				for (PlayerClass cid : PlayerClass.values())
				{
					if (cid.getRace() == Race.DARK_ELF)
					{
						subclasses.remove(cid);
					}
				}
			}
			else if (player.getRace() == Race.DARK_ELF)
			{
				for (PlayerClass cid : PlayerClass.values())
				{
					if (cid.getRace() == Race.ELF)
					{
						subclasses.remove(cid);
					}
				}
			}
			
			final Set<PlayerClass> unavailableClasses = subclassSetMap.get(pClass);
			if (unavailableClasses != null)
			{
				subclasses.removeAll(unavailableClasses);
			}
		}
		
		if (subclasses != null)
		{
			final PlayerClass currClassId = player.getPlayerClass();
			for (PlayerClass tempClass : subclasses)
			{
				if (currClassId.equalsOrChildOf(tempClass))
				{
					subclasses.remove(tempClass);
				}
			}
		}
		
		return subclasses;
	}
	
	/**
	 * Check new subclass classId for validity (villagemaster race/type, is not contains in previous subclasses, is contains in allowed subclasses) Base class not added into allowed subclasses.
	 * @param player
	 * @param classId
	 * @return
	 */
	private final boolean isValidNewSubClass(Player player, int classId)
	{
		final PlayerClass cid = PlayerClass.getPlayerClass(classId);
		SubClassHolder sub;
		PlayerClass subClassId;
		for (Iterator<SubClassHolder> subList = iterSubClasses(player); subList.hasNext();)
		{
			sub = subList.next();
			subClassId = PlayerClass.getPlayerClass(sub.getId());
			if (subClassId.equalsOrChildOf(cid))
			{
				return false;
			}
		}
		
		// get player base class
		final int currentBaseId = player.getBaseClass();
		final PlayerClass baseCID = PlayerClass.getPlayerClass(currentBaseId);
		
		// we need 2nd occupation ID
		final int baseClassId;
		if (baseCID.level() > 2)
		{
			baseClassId = baseCID.getParent().getId();
		}
		else
		{
			baseClassId = currentBaseId;
		}
		
		final Set<PlayerClass> availSubs = getSubclasses(player, baseClassId);
		if ((availSubs == null) || availSubs.isEmpty())
		{
			return false;
		}
		
		boolean found = false;
		for (PlayerClass pclass : availSubs)
		{
			if (pclass.getId() == classId)
			{
				found = true;
				break;
			}
		}
		
		return found;
	}
	
	private static Iterator<SubClassHolder> iterSubClasses(Player player)
	{
		return player.getSubClasses().values().iterator();
	}
	
	private void dissolveClan(Player player, int clanId)
	{
		if (!player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		
		final Clan clan = player.getClan();
		if (clan.getAllyId() != 0)
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_DISPERSE_THE_CLANS_IN_YOUR_ALLIANCE);
			return;
		}
		
		if (clan.isAtWar())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_DISSOLVE_A_CLAN_WHILE_ENGAGED_IN_A_WAR);
			return;
		}
		
		if ((clan.getCastleId() != 0) || (clan.getHideoutId() != 0))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_DISSOLVE_A_CLAN_WHILE_OWNING_A_CLAN_HALL_OR_CASTLE);
			return;
		}
		
		for (Castle castle : CastleManager.getInstance().getCastles())
		{
			if (SiegeManager.getInstance().checkIsRegistered(clan, castle.getResidenceId()))
			{
				player.sendPacket(SystemMessageId.YOU_CANNOT_DISSOLVE_A_CLAN_DURING_A_SIEGE_OR_WHILE_PROTECTING_A_CASTLE);
				return;
			}
		}
		
		if (player.isInsideZone(ZoneId.SIEGE))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_DISSOLVE_A_CLAN_DURING_A_SIEGE_OR_WHILE_PROTECTING_A_CASTLE);
			return;
		}
		
		if (clan.getDissolvingExpiryTime() > System.currentTimeMillis())
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_ALREADY_REQUESTED_THE_DISSOLUTION_OF_YOUR_CLAN);
			return;
		}
		
		clan.setDissolvingExpiryTime(System.currentTimeMillis() + (PlayerConfig.ALT_CLAN_DISSOLVE_DAYS * 86400000)); // 24*60*60*1000 = 86400000
		clan.updateClanInDB();
		
		// The clan leader should take the XP penalty of a full death.
		player.calculateDeathExpPenalty(null, false);
		ClanTable.getInstance().scheduleRemoveClan(clan.getId());
	}
	
	private void recoverClan(Player player, int clanId)
	{
		if (!player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		
		final Clan clan = player.getClan();
		clan.setDissolvingExpiryTime(0);
		clan.updateClanInDB();
	}
	
	private void createSubPledge(Player player, String clanName, String leaderName, int pledgeType, int minClanLvl)
	{
		if (!player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		
		final Clan clan = player.getClan();
		if (clan.getLevel() < minClanLvl)
		{
			if (pledgeType == Clan.SUBUNIT_ACADEMY)
			{
				player.sendPacket(SystemMessageId.TO_ESTABLISH_A_CLAN_ACADEMY_YOUR_CLAN_MUST_BE_LEVEL_5_OR_HIGHER);
			}
			else
			{
				player.sendPacket(SystemMessageId.THE_CONDITIONS_NECESSARY_TO_CREATE_A_MILITARY_UNIT_HAVE_NOT_BEEN_MET);
			}
			
			return;
		}
		
		if (!StringUtil.isAlphaNumeric(clanName) || !isValidName(clanName) || (2 > clanName.length()))
		{
			player.sendPacket(SystemMessageId.CLAN_NAME_IS_INVALID);
			return;
		}
		
		if (clanName.length() > 16)
		{
			player.sendPacket(SystemMessageId.CLAN_NAME_S_LENGTH_IS_INCORRECT);
			return;
		}
		
		for (Clan tempClan : ClanTable.getInstance().getClans())
		{
			if (tempClan.getSubPledge(clanName) != null)
			{
				if (pledgeType == Clan.SUBUNIT_ACADEMY)
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.S1_ALREADY_EXISTS);
					sm.addString(clanName);
					player.sendPacket(sm);
				}
				else
				{
					player.sendPacket(SystemMessageId.ANOTHER_MILITARY_UNIT_IS_ALREADY_USING_THAT_NAME_PLEASE_ENTER_A_DIFFERENT_NAME);
				}
				
				return;
			}
		}
		
		if (pledgeType != Clan.SUBUNIT_ACADEMY)
		{
			final ClanMember member = clan.getClanMember(leaderName);
			if ((member == null) || (member.getPledgeType() != 0) || (clan.getLeaderSubPledge(member.getObjectId()) > 0))
			{
				if (pledgeType >= Clan.SUBUNIT_KNIGHT1)
				{
					player.sendPacket(SystemMessageId.THE_CAPTAIN_OF_THE_ORDER_OF_KNIGHTS_CANNOT_BE_APPOINTED);
				}
				else if (pledgeType >= Clan.SUBUNIT_ROYAL1)
				{
					player.sendPacket(SystemMessageId.THE_CAPTAIN_OF_THE_ROYAL_GUARD_CANNOT_BE_APPOINTED);
				}
				
				return;
			}
		}
		
		final int leaderId = pledgeType != Clan.SUBUNIT_ACADEMY ? clan.getClanMember(leaderName).getObjectId() : 0;
		if (clan.createSubPledge(player, pledgeType, leaderId, clanName) == null)
		{
			return;
		}
		
		SystemMessage sm;
		if (pledgeType == Clan.SUBUNIT_ACADEMY)
		{
			sm = new SystemMessage(SystemMessageId.CONGRATULATIONS_THE_S1_S_CLAN_ACADEMY_HAS_BEEN_CREATED);
			sm.addString(player.getClan().getName());
		}
		else if (pledgeType >= Clan.SUBUNIT_KNIGHT1)
		{
			sm = new SystemMessage(SystemMessageId.THE_KNIGHTS_OF_S1_HAVE_BEEN_CREATED);
			sm.addString(player.getClan().getName());
		}
		else if (pledgeType >= Clan.SUBUNIT_ROYAL1)
		{
			sm = new SystemMessage(SystemMessageId.THE_ROYAL_GUARD_OF_S1_HAVE_BEEN_CREATED);
			sm.addString(player.getClan().getName());
		}
		else
		{
			sm = new SystemMessage(SystemMessageId.YOUR_CLAN_HAS_BEEN_CREATED);
		}
		
		player.sendPacket(sm);
		
		if (pledgeType != Clan.SUBUNIT_ACADEMY)
		{
			final ClanMember leaderSubPledge = clan.getClanMember(leaderName);
			final Player leaderPlayer = leaderSubPledge.getPlayer();
			if (leaderPlayer != null)
			{
				leaderPlayer.setPledgeClass(ClanMember.calculatePledgeClass(leaderPlayer));
				leaderPlayer.updateUserInfo();
			}
		}
	}
	
	private void renameSubPledge(Player player, int pledgeType, String pledgeName)
	{
		if (!player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		
		final Clan clan = player.getClan();
		final SubPledge subPledge = player.getClan().getSubPledge(pledgeType);
		if (subPledge == null)
		{
			player.sendMessage("Pledge don't exists.");
			return;
		}
		
		if (!StringUtil.isAlphaNumeric(pledgeName) || !isValidName(pledgeName) || (2 > pledgeName.length()))
		{
			player.sendPacket(SystemMessageId.CLAN_NAME_IS_INVALID);
			return;
		}
		
		if (pledgeName.length() > 16)
		{
			player.sendPacket(SystemMessageId.CLAN_NAME_S_LENGTH_IS_INCORRECT);
			return;
		}
		
		subPledge.setName(pledgeName);
		clan.updateSubPledgeInDB(subPledge.getId());
		clan.broadcastClanStatus();
		player.sendMessage("Pledge name changed.");
	}
	
	private void assignSubPledgeLeader(Player player, String clanName, String leaderName)
	{
		if (!player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		
		if (leaderName.length() > 16)
		{
			player.sendPacket(SystemMessageId.YOUR_TITLE_CANNOT_EXCEED_16_CHARACTERS_IN_LENGTH_PLEASE_TRY_AGAIN);
			return;
		}
		
		if (player.getName().equals(leaderName))
		{
			player.sendPacket(SystemMessageId.THE_CAPTAIN_OF_THE_ROYAL_GUARD_CANNOT_BE_APPOINTED);
			return;
		}
		
		final Clan clan = player.getClan();
		final SubPledge subPledge = player.getClan().getSubPledge(clanName);
		if ((null == subPledge) || (subPledge.getId() == Clan.SUBUNIT_ACADEMY))
		{
			player.sendPacket(SystemMessageId.CLAN_NAME_IS_INVALID);
			return;
		}
		
		final ClanMember member = clan.getClanMember(leaderName);
		if ((member == null) || (member.getPledgeType() != 0) || (clan.getLeaderSubPledge(member.getObjectId()) > 0))
		{
			if (subPledge.getId() >= Clan.SUBUNIT_KNIGHT1)
			{
				player.sendPacket(SystemMessageId.THE_CAPTAIN_OF_THE_ORDER_OF_KNIGHTS_CANNOT_BE_APPOINTED);
			}
			else if (subPledge.getId() >= Clan.SUBUNIT_ROYAL1)
			{
				player.sendPacket(SystemMessageId.THE_CAPTAIN_OF_THE_ROYAL_GUARD_CANNOT_BE_APPOINTED);
			}
			
			return;
		}
		
		subPledge.setLeaderId(member.getObjectId());
		clan.updateSubPledgeInDB(subPledge.getId());
		
		final Player leaderPlayer = member.getPlayer();
		if (leaderPlayer != null)
		{
			leaderPlayer.setPledgeClass(ClanMember.calculatePledgeClass(leaderPlayer));
			leaderPlayer.updateUserInfo();
		}
		
		clan.broadcastClanStatus();
		final SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_BEEN_SELECTED_AS_THE_CAPTAIN_OF_S2);
		sm.addString(leaderName);
		sm.addString(clanName);
		clan.broadcastToOnlineMembers(sm);
	}
	
	/**
	 * this displays PledgeSkillList to the player.
	 * @param player
	 */
	public static void showPledgeSkillList(Player player)
	{
		if (!player.isClanLeader())
		{
			final NpcHtmlMessage html = new NpcHtmlMessage();
			html.setFile(player, "data/html/villagemaster/NotClanLeader.htm");
			player.sendPacket(html);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final Clan clan = player.getClan();
		final List<SkillLearn> skills = SkillTreeData.getInstance().getAvailablePledgeSkills(clan);
		final AcquireSkillList asl = new AcquireSkillList(AcquireSkillType.PLEDGE);
		int counts = 0;
		for (SkillLearn s : skills)
		{
			asl.addSkill(s.getSkillId(), s.getSkillLevel(), s.getSkillLevel(), s.getLevelUpSp(), s.getSocialClass().ordinal());
			counts++;
		}
		
		if (counts == 0)
		{
			if (clan.getLevel() < 8)
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_DO_NOT_HAVE_ANY_FURTHER_SKILLS_TO_LEARN_COME_BACK_WHEN_YOU_HAVE_REACHED_LEVEL_S1);
				if (clan.getLevel() < 5)
				{
					sm.addInt(5);
				}
				else
				{
					sm.addInt(clan.getLevel() + 1);
				}
				
				player.sendPacket(sm);
			}
			else
			{
				final NpcHtmlMessage html = new NpcHtmlMessage();
				html.setFile(player, "data/html/villagemaster/NoMoreSkills.htm");
				player.sendPacket(html);
			}
		}
		else
		{
			player.sendPacket(asl);
		}
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	private static boolean isValidName(String name)
	{
		Pattern pattern;
		try
		{
			pattern = Pattern.compile(ServerConfig.CLAN_NAME_TEMPLATE);
		}
		catch (PatternSyntaxException e)
		{
			LOGGER.warning("ERROR: Wrong pattern for clan name!");
			pattern = Pattern.compile(".*");
		}
		
		return pattern.matcher(name).matches();
	}
}
