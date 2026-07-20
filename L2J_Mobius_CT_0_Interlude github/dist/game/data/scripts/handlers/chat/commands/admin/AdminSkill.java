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
package handlers.chat.commands.admin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.data.xml.ClassListData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.html.PageBuilder;
import org.l2jmobius.gameserver.model.html.PageResult;
import org.l2jmobius.gameserver.model.html.styles.ButtonsStyle;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.holders.SkillLearn;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.PledgeSkillList;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * @author Mobius
 */
public class AdminSkill implements IAdminCommandHandler
{
	private static final Logger LOGGER = Logger.getLogger(AdminSkill.class.getName());
	
	private static final int PAGE_LIMIT = 7;
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_show_skills",
		"admin_remove_skills",
		"admin_skill_list",
		"admin_skill_race",
		"admin_skill_class",
		"admin_skill_add_list",
		"admin_skill_index",
		"admin_add_skill",
		"admin_remove_skill",
		"admin_get_skills",
		"admin_reset_skills",
		"admin_give_all_skills",
		"admin_give_all_skills_fs",
		"admin_give_clan_skills",
		"admin_give_all_clan_skills",
		"admin_remove_all_skills",
		"admin_add_clan_skill",
		"admin_setskill"
	};
	
	private static Skill[] adminSkills;
	
	@Override
	public boolean onCommand(String commandValue, Player activeChar)
	{
		String command = commandValue;
		if (command.equals("admin_show_skills"))
		{
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_remove_skills"))
		{
			try
			{
				final String val = command.substring(20);
				removeSkillsPage(activeChar, Integer.parseInt(val));
			}
			catch (StringIndexOutOfBoundsException e)
			{
				// Not important.
			}
		}
		else if (command.startsWith("admin_skill_list"))
		{
			adminAddSkills(activeChar);
		}
		else if (command.startsWith("admin_skill_race"))
		{
			final String[] split = command.split(" ");
			if (split.length == 2)
			{
				adminAddRaceSkills(activeChar, Enum.valueOf(Race.class, split[1]));
			}
			else
			{
				adminAddSkills(activeChar);
			}
		}
		else if (command.startsWith("admin_skill_class"))
		{
			final String[] split = command.split(" ");
			if (split.length > 1)
			{
				adminAddClassSkills(activeChar, Enum.valueOf(PlayerClass.class, split[1]), split.length > 2 ? Integer.parseInt(split[2]) : 0);
			}
			else
			{
				adminAddSkills(activeChar);
			}
		}
		else if (command.startsWith("admin_skill_add_list"))
		{
			final String[] split = command.split(" ");
			if (split.length > 1)
			{
				adminAddClassSkillList(activeChar, Enum.valueOf(PlayerClass.class, split[1]), split.length > 2 ? Integer.parseInt(split[2]) : 1, split.length > 3 ? Integer.parseInt(split[3]) : 0);
			}
			else
			{
				adminAddSkills(activeChar);
			}
		}
		else if (command.startsWith("admin_skill_index"))
		{
			try
			{
				final String val = command.substring(18);
				AdminHtml.showAdminHtml(activeChar, "skills/" + val + ".htm");
			}
			catch (StringIndexOutOfBoundsException e)
			{
				// Not important.
			}
		}
		else if (command.startsWith("admin_add_skill"))
		{
			try
			{
				final String val = command.substring(15);
				adminAddSkill(activeChar, val);
			}
			catch (Exception e)
			{
				activeChar.sendSysMessage("Usage: //add_skill <skill_id> <level>");
			}
		}
		else if (command.startsWith("admin_remove_skill"))
		{
			try
			{
				final String id = command.substring(19);
				final int idval = Integer.parseInt(id);
				adminRemoveSkill(activeChar, idval);
			}
			catch (Exception e)
			{
				activeChar.sendSysMessage("Usage: //remove_skill <skill_id>");
			}
		}
		else if (command.equals("admin_get_skills"))
		{
			adminGetSkills(activeChar);
		}
		else if (command.equals("admin_reset_skills"))
		{
			adminResetSkills(activeChar);
		}
		else if (command.equals("admin_give_all_skills"))
		{
			adminGiveAllSkills(activeChar, false, false);
		}
		else if (command.equals("admin_give_all_skills_fs"))
		{
			adminGiveAllSkills(activeChar, true, true);
		}
		else if (command.equals("admin_give_clan_skills"))
		{
			adminGiveClanSkills(activeChar, false);
		}
		else if (command.equals("admin_give_all_clan_skills"))
		{
			adminGiveClanSkills(activeChar, true);
		}
		else if (command.equals("admin_remove_all_skills"))
		{
			final WorldObject target = activeChar.getTarget();
			if ((target == null) || !target.isPlayer())
			{
				activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
				return false;
			}
			
			final Player player = target.asPlayer();
			for (Skill skill : player.getAllSkills())
			{
				player.removeSkill(skill);
			}
			
			activeChar.sendSysMessage("You have removed all skills from " + player.getName() + ".");
			player.sendMessage("Admin removed all skills from you.");
			player.sendSkillList();
			player.broadcastUserInfo();
		}
		else if (command.startsWith("admin_add_clan_skill"))
		{
			try
			{
				final String[] val = command.split(" ");
				adminAddClanSkill(activeChar, Integer.parseInt(val[1]), Integer.parseInt(val[2]));
			}
			catch (Exception e)
			{
				activeChar.sendSysMessage("Usage: //add_clan_skill <skill_id> <level>");
			}
		}
		else if (command.startsWith("admin_setskill"))
		{
			final String[] split = command.split(" ");
			final int id = Integer.parseInt(split[1]);
			final int level = Integer.parseInt(split[2]);
			final Skill skill = SkillData.getInstance().getSkill(id, level);
			activeChar.addSkill(skill);
			activeChar.sendSkillList();
			activeChar.sendSysMessage("You added yourself skill " + skill.getName() + "(" + id + ") level " + level);
		}
		
		return true;
	}
	
	/**
	 * This function will give all the skills that the target can learn at his/her level
	 * @param activeChar the player
	 * @param includeByFs if {@code true} Forgotten Scroll skills will be delivered.
	 * @param includeRequiredItems if {@code true} skills that have required items will be added
	 */
	private void adminGiveAllSkills(Player activeChar, boolean includeByFs, boolean includeRequiredItems)
	{
		final WorldObject target = activeChar.getTarget();
		if ((target == null) || !target.isPlayer())
		{
			activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
			return;
		}
		
		// Notify player and admin.
		final Player player = target.asPlayer();
		activeChar.sendSysMessage("You gave " + player.giveAvailableSkills(includeByFs, true, includeRequiredItems) + " skills to " + player.getName());
		player.sendSkillList();
	}
	
	/**
	 * This function will give all the skills that the target's clan can learn at it's level.<br>
	 * If the target is not the clan leader, a system message will be sent to the Game Master.
	 * @param activeChar the player, probably a Game Master.
	 * @param includeSquad if Squad skills is included
	 */
	private void adminGiveClanSkills(Player activeChar, boolean includeSquad)
	{
		final WorldObject target = activeChar.getTarget();
		if ((target == null) || !target.isPlayer())
		{
			activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
			return;
		}
		
		final Player player = target.asPlayer();
		final Clan clan = player.getClan();
		if (clan == null)
		{
			activeChar.sendPacket(SystemMessageId.THE_TARGET_MUST_BE_A_CLAN_MEMBER);
			return;
		}
		
		if (!player.isClanLeader())
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.S1_IS_NOT_A_CLAN_LEADER);
			sm.addString(player.getName());
			activeChar.sendPacket(sm);
		}
		
		final Map<Integer, SkillLearn> skills = SkillTreeData.getInstance().getMaxPledgeSkills(clan);
		for (SkillLearn s : skills.values())
		{
			clan.addNewSkill(SkillData.getInstance().getSkill(s.getSkillId(), s.getSkillLevel()));
		}
		
		// Notify target and active char.
		clan.broadcastToOnlineMembers(new PledgeSkillList(clan));
		for (Player member : clan.getOnlineMembers(0))
		{
			member.sendSkillList();
		}
		
		activeChar.sendSysMessage("You gave " + skills.size() + " skills to " + player.getName() + "'s clan " + clan.getName() + ".");
		player.sendMessage("Your clan received " + skills.size() + " skills.");
	}
	
	/**
	 * @param activeChar the active Game Master.
	 * @param pageValue
	 */
	private void removeSkillsPage(Player activeChar, int pageValue)
	{
		final WorldObject target = activeChar.getTarget();
		if ((target == null) || !target.isPlayer())
		{
			activeChar.sendPacket(SystemMessageId.THAT_IS_THE_INCORRECT_TARGET);
			return;
		}
		
		final Player player = target.asPlayer();
		final Skill[] skills = player.getAllSkills().toArray(new Skill[player.getAllSkills().size()]);
		final int maxSkillsPerPage = 10;
		int maxPages = skills.length / maxSkillsPerPage;
		if (skills.length > (maxSkillsPerPage * maxPages))
		{
			maxPages++;
		}
		
		int page = pageValue;
		if (page > maxPages)
		{
			page = maxPages;
		}
		
		final int skillsStart = maxSkillsPerPage * page;
		int skillsEnd = skills.length;
		if ((skillsEnd - skillsStart) > maxSkillsPerPage)
		{
			skillsEnd = skillsStart + maxSkillsPerPage;
		}
		
		final NpcHtmlMessage adminReply = new NpcHtmlMessage();
		final StringBuilder replyMSG = new StringBuilder(500 + (maxPages * 50) + (((skillsEnd - skillsStart) + 1) * 50));
		replyMSG.append("<html><body><table width=260><tr><td width=40><button value=\"Main\" action=\"bypass admin_admin\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td width=180><center>Character Selection Menu</center></td><td width=40><button value=\"Back\" action=\"bypass -h admin_show_skills\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr></table><br><br><center>Editing <font color=\"LEVEL\">" + player.getName() + "</font></center><br><table width=270><tr><td>Lv: " + player.getLevel() + " " + ClassListData.getInstance().getClass(player.getPlayerClass()).getClassName() + "</td></tr></table><br><table width=270><tr><td>Note: Do not forget that modifying players skills can</td></tr><tr><td>ruin the game...</td></tr></table><br><center>Click on the skill you wish to remove:</center><br><center><table width=270><tr>");
		for (int x = 0; x < maxPages; x++)
		{
			final int pagenr = x + 1;
			replyMSG.append("<td><a action=\"bypass -h admin_remove_skills " + x + "\">Page " + pagenr + "</a></td>");
		}
		
		replyMSG.append("</tr></table></center><br><table width=270><tr><td width=80>Name:</td><td width=60>Level:</td><td width=40>Id:</td></tr>");
		for (int i = skillsStart; i < skillsEnd; i++)
		{
			replyMSG.append("<tr><td width=80><a action=\"bypass -h admin_remove_skill " + skills[i].getId() + "\">" + skills[i].getName() + "</a></td><td width=60>" + skills[i].getLevel() + "</td><td width=40>" + skills[i].getId() + "</td></tr>");
		}
		
		replyMSG.append("</table><br><center><table>Remove skill by ID :<tr><td>Id: </td><td><edit var=\"id_to_remove\" width=110></td></tr></table></center><center><button value=\"Remove skill\" action=\"bypass -h admin_remove_skill $id_to_remove\" width=110 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center><br><center><button value=\"Back\" action=\"bypass -h admin_current_player\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center></body></html>");
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}
	
	private void adminAddSkills(Player player)
	{
		final List<Race> races = new ArrayList<>();
		for (PlayerClass playerClass : PlayerClass.values())
		{
			final Race race = playerClass.getRace();
			if ((race != null) && !races.contains(race) && !SkillTreeData.getInstance().getCompleteClassSkillTree(playerClass).isEmpty())
			{
				races.add(race);
			}
		}
		
		final StringBuilder sb = new StringBuilder();
		for (Race race : races)
		{
			sb.append("<tr><td><a action=\"bypass admin_skill_race " + race + "\">" + StringUtil.enumToString(race) + "</a></td></tr>");
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage();
		html.setFile(player, "data/html/admin/charskills_add.htm");
		html.replace("%racelist%", sb.toString());
		player.sendPacket(html);
	}
	
	private void adminAddRaceSkills(Player player, Race race)
	{
		final List<PlayerClass> classes = new ArrayList<>();
		for (PlayerClass playerClass : PlayerClass.values())
		{
			if ((playerClass.getRace() == race) && !SkillTreeData.getInstance().getCompleteClassSkillTree(playerClass).isEmpty())
			{
				classes.add(playerClass);
			}
		}
		
		final StringBuilder sb = new StringBuilder();
		for (PlayerClass playerClass : classes)
		{
			final String color;
			switch (playerClass.level())
			{
				case 1:
				{
					color = "999999";
					break;
				}
				case 2:
				{
					color = "F2B705";
					break;
				}
				case 3:
				{
					color = "F26005";
					break;
				}
				case 4:
				{
					color = "FF0022";
					break;
				}
				default:
				{
					color = "777777";
					break;
				}
			}
			
			sb.append("<tr><td><a action=\"bypass admin_skill_class " + playerClass + "\"><font color=\"" + color + "\">" + ClassListData.getInstance().getClass(playerClass).getClassName() + "</font></a></td></tr>");
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage();
		html.setFile(player, "data/html/admin/charskills_race.htm");
		html.replace("%racename%", StringUtil.enumToString(race));
		html.replace("%classlist%", sb.toString());
		player.sendPacket(html);
	}
	
	private void adminAddClassSkills(Player player, PlayerClass playerClass, int page)
	{
		final Set<Skill> skills = new HashSet<>();
		for (SkillLearn skillLearn : SkillTreeData.getInstance().getCompleteClassSkillTree(playerClass).values())
		{
			skills.add(SkillData.getInstance().getSkill(skillLearn.getSkillId(), 1));
		}
		
		int row = 0;
		final String pageLink = "bypass admin_skill_class " + playerClass;
		final PageResult result = PageBuilder.newBuilder(skills, PAGE_LIMIT, pageLink).currentPage(page).style(ButtonsStyle.INSTANCE).bodyHandler((pages, skill, sb) ->
		{
			sb.append((row % 2) == 0 ? "<table width=\"295\" bgcolor=\"000000\">" : "<table width=\"295\">");
			sb.append("<tr><td height=40 width=40><img src=\"");
			sb.append(skill.getIcon());
			sb.append("\" width=32 height=32></td><td width=190><font color=\"B09878\"><a action=\"bypass admin_skill_add_list ");
			sb.append(playerClass);
			sb.append(" ");
			sb.append(skill.getId());
			sb.append("\">");
			sb.append(skill.getName());
			sb.append(" (");
			sb.append(skill.getId());
			sb.append(")</a></font></td></tr></table><img src=\"L2UI.SquareGray\" width=295 height=1>");
		}).build();
		
		final NpcHtmlMessage html = new NpcHtmlMessage();
		html.setFile(player, "data/html/admin/charskills_class.htm");
		html.replace("%classname%", ClassListData.getInstance().getClass(playerClass).getClassName());
		html.replace("%skilllist%", result.getBodyTemplate().toString());
		if (result.getPages() > 1)
		{
			html.replace("%pages%", "<table width=280 cellspacing=0><tr>" + result.getPagerTemplate() + "</tr></table>");
		}
		else
		{
			html.replace("%pages%", "");
		}
		
		player.sendPacket(html);
	}
	
	private void adminAddClassSkillList(Player player, PlayerClass playerClass, int skillId, int page)
	{
		final Map<Integer, Skill> skills = new TreeMap<>();
		for (SkillLearn skillLearn : SkillTreeData.getInstance().getCompleteClassSkillTree(playerClass).values())
		{
			final int id = skillLearn.getSkillId();
			if (id == skillId)
			{
				final int level = skillLearn.getSkillLevel();
				skills.put(level, SkillData.getInstance().getSkill(id, level));
			}
		}
		
		int row = 0;
		final String pageLink = "bypass admin_skill_add_list " + playerClass + " " + skillId;
		final PageResult result = PageBuilder.newBuilder(skills.values(), PAGE_LIMIT, pageLink).currentPage(page).style(ButtonsStyle.INSTANCE).bodyHandler((pages, skill, sb) ->
		{
			sb.append((row % 2) == 0 ? "<table width=\"295\" bgcolor=\"000000\">" : "<table width=\"295\">");
			sb.append("<tr><td height=40 width=40><img src=\"");
			sb.append(skill.getIcon());
			sb.append("\" width=32 height=32></td><td width=190><font color=\"B09878\"><a action=\"bypass admin_add_skill ");
			sb.append(skill.getId());
			sb.append(" ");
			sb.append(skill.getLevel());
			sb.append("\">");
			sb.append(skill.getName());
			sb.append(" (");
			sb.append(skill.getLevel());
			sb.append(")</a></font></td></tr></table><img src=\"L2UI.SquareGray\" width=295 height=1>");
		}).build();
		
		final NpcHtmlMessage html = new NpcHtmlMessage();
		html.setFile(player, "data/html/admin/charskills_add_list.htm");
		html.replace("%skillid%", skillId);
		html.replace("%skilllist%", result.getBodyTemplate().toString());
		html.replace("%classId%", playerClass.toString());
		if (result.getPages() > 1)
		{
			html.replace("%pages%", "<table width=280 cellspacing=0><tr>" + result.getPagerTemplate() + "</tr></table>");
		}
		else
		{
			html.replace("%pages%", "");
		}
		
		player.sendPacket(html);
	}
	
	private void showMainPage(Player activeChar)
	{
		final WorldObject target = activeChar.getTarget();
		if ((target == null) || !target.isPlayer())
		{
			activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
			return;
		}
		
		final Player player = target.asPlayer();
		final NpcHtmlMessage adminReply = new NpcHtmlMessage();
		adminReply.setFile(activeChar, "data/html/admin/charskills.htm");
		adminReply.replace("%name%", player.getName());
		adminReply.replace("%level%", String.valueOf(player.getLevel()));
		adminReply.replace("%class%", ClassListData.getInstance().getClass(player.getPlayerClass()).getClassName());
		activeChar.sendPacket(adminReply);
	}
	
	private void adminGetSkills(Player activeChar)
	{
		final WorldObject target = activeChar.getTarget();
		if ((target == null) || !target.isPlayer())
		{
			activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
			return;
		}
		
		final Player player = target.asPlayer();
		if (player.getName().equals(activeChar.getName()))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_THIS_ON_YOURSELF);
		}
		else
		{
			final Skill[] skills = player.getAllSkills().toArray(new Skill[player.getAllSkills().size()]);
			adminSkills = activeChar.getAllSkills().toArray(new Skill[activeChar.getAllSkills().size()]);
			for (Skill skill : adminSkills)
			{
				activeChar.removeSkill(skill);
			}
			
			for (Skill skill : skills)
			{
				activeChar.addSkill(skill, true);
			}
			
			activeChar.sendSysMessage("You now have all the skills of " + player.getName() + ".");
			activeChar.sendSkillList();
		}
		
		showMainPage(activeChar);
	}
	
	private void adminResetSkills(Player activeChar)
	{
		final WorldObject target = activeChar.getTarget();
		if ((target == null) || !target.isPlayer())
		{
			activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
			return;
		}
		
		final Player player = target.asPlayer();
		if (adminSkills == null)
		{
			activeChar.sendSysMessage("You must get the skills of someone in order to do this.");
		}
		else
		{
			final Skill[] skills = player.getAllSkills().toArray(new Skill[player.getAllSkills().size()]);
			for (Skill skill : skills)
			{
				player.removeSkill(skill);
			}
			
			for (Skill skill : activeChar.getAllSkills())
			{
				player.addSkill(skill, true);
			}
			
			for (Skill skill : skills)
			{
				activeChar.removeSkill(skill);
			}
			
			for (Skill skill : adminSkills)
			{
				activeChar.addSkill(skill, true);
			}
			
			player.sendMessage("[GM]" + activeChar.getName() + " updated your skills.");
			activeChar.sendSysMessage("You now have all your skills back.");
			adminSkills = null;
			activeChar.sendSkillList();
			player.sendSkillList();
		}
		
		showMainPage(activeChar);
	}
	
	private void adminAddSkill(Player activeChar, String value)
	{
		final WorldObject target = activeChar.getTarget();
		if ((target == null) || !target.isPlayer())
		{
			activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
			showMainPage(activeChar);
			return;
		}
		
		final Player player = target.asPlayer();
		final StringTokenizer st = new StringTokenizer(value);
		if ((st.countTokens() != 1) && (st.countTokens() != 2))
		{
			showMainPage(activeChar);
		}
		else
		{
			Skill skill = null;
			try
			{
				final String id = st.nextToken();
				final String level = st.countTokens() == 1 ? st.nextToken() : null;
				final int idval = Integer.parseInt(id);
				final int levelval = level == null ? 1 : Integer.parseInt(level);
				skill = SkillData.getInstance().getSkill(idval, levelval);
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "", e);
			}
			
			if (skill != null)
			{
				final String name = skill.getName();
				
				// Player's info.
				player.sendMessage("Admin gave you the skill " + name + ".");
				player.addSkill(skill, true);
				player.sendSkillList();
				
				// Admin info.
				activeChar.sendSysMessage("You gave the skill " + name + " to " + player.getName() + ".");
				activeChar.sendSkillList();
			}
			else
			{
				activeChar.sendSysMessage("Error: there is no such skill.");
			}
			
			showMainPage(activeChar); // Back to start
		}
	}
	
	private void adminRemoveSkill(Player activeChar, int idval)
	{
		final WorldObject target = activeChar.getTarget();
		if ((target == null) || !target.isPlayer())
		{
			activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
			return;
		}
		
		final Player player = target.asPlayer();
		final Skill skill = SkillData.getInstance().getSkill(idval, player.getSkillLevel(idval));
		if (skill != null)
		{
			final String skillname = skill.getName();
			player.sendMessage("Admin removed the skill " + skillname + " from your skills list.");
			player.removeSkill(skill);
			
			// Admin information
			activeChar.sendSysMessage("You removed the skill " + skillname + " from " + player.getName() + ".");
			activeChar.sendSkillList();
		}
		else
		{
			activeChar.sendSysMessage("Error: there is no such skill.");
		}
		
		removeSkillsPage(activeChar, 0); // Back to previous page
	}
	
	private void adminAddClanSkill(Player activeChar, int id, int level)
	{
		final WorldObject target = activeChar.getTarget();
		if ((target == null) || !target.isPlayer())
		{
			activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
			showMainPage(activeChar);
			return;
		}
		
		final Player player = target.asPlayer();
		if (!player.isClanLeader())
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.S1_IS_NOT_A_CLAN_LEADER);
			sm.addString(player.getName());
			activeChar.sendPacket(sm);
			showMainPage(activeChar);
			return;
		}
		
		if ((id < 370) || (id > 391) || (level < 1) || (level > 3))
		{
			activeChar.sendSysMessage("Usage: //add_clan_skill <skill_id> <level>");
			showMainPage(activeChar);
			return;
		}
		
		final Skill skill = SkillData.getInstance().getSkill(id, level);
		if (skill == null)
		{
			activeChar.sendSysMessage("Error: there is no such skill.");
			return;
		}
		
		final String skillname = skill.getName();
		final SystemMessage sm = new SystemMessage(SystemMessageId.THE_CLAN_SKILL_S1_HAS_BEEN_ADDED);
		sm.addSkillName(skill);
		player.sendPacket(sm);
		final Clan clan = player.getClan();
		clan.broadcastToOnlineMembers(sm);
		clan.addNewSkill(skill);
		activeChar.sendSysMessage("You gave the Clan Skill: " + skillname + " to the clan " + clan.getName() + ".");
		clan.broadcastToOnlineMembers(new PledgeSkillList(clan));
		for (Player member : clan.getOnlineMembers(0))
		{
			member.sendSkillList();
		}
		
		showMainPage(activeChar);
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
