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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.html.PageBuilder;
import org.l2jmobius.gameserver.model.html.PageResult;
import org.l2jmobius.gameserver.model.html.styles.ButtonsStyle;
import org.l2jmobius.gameserver.model.skill.AbnormalType;
import org.l2jmobius.gameserver.model.skill.BuffInfo;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SkillCoolTime;
import org.l2jmobius.gameserver.util.GMAudit;

/**
 * @author Mobius
 */
public class AdminBuffs implements IAdminCommandHandler
{
	private static final int PAGE_LIMIT = 7;
	private static final String FONT_RED1 = "<font color=\"FF0000\">";
	private static final String FONT_RED2 = "</font>";
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_buff",
		"admin_getbuffs",
		"admin_getbuffs_ps",
		"admin_stopbuff",
		"admin_stopallbuffs",
		"admin_viewblockedeffects",
		"admin_viewskilleffects",
		"admin_viewskilleffects_ps",
		"admin_areacancel",
		"admin_removereuse",
		"admin_switch_gm_buffs"
	};
	
	@Override
	public boolean onCommand(String commandValue, Player activeChar)
	{
		String command = commandValue;
		if (command.startsWith("admin_buff"))
		{
			if ((activeChar.getTarget() == null) || !activeChar.getTarget().isCreature())
			{
				activeChar.sendPacket(SystemMessageId.THAT_IS_THE_INCORRECT_TARGET);
				return false;
			}
			
			final StringTokenizer st = new StringTokenizer(command, " ");
			command = st.nextToken();
			if (!st.hasMoreTokens())
			{
				activeChar.sendSysMessage("Skill Id and level are not specified.");
				activeChar.sendSysMessage("Usage: //buff <skillId> <skillLevel>");
				return false;
			}
			
			try
			{
				final int skillId = Integer.parseInt(st.nextToken());
				final int skillLevel = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : SkillData.getInstance().getMaxLevel(skillId);
				final Creature target = activeChar.getTarget().asCreature();
				final Skill skill = SkillData.getInstance().getSkill(skillId, skillLevel);
				if (skill == null)
				{
					activeChar.sendSysMessage("Skill with id: " + skillId + ", level: " + skillLevel + " not found.");
					return false;
				}
				
				activeChar.sendSysMessage("Admin buffing " + skill.getName() + " (" + skillId + "," + skillLevel + ")");
				skill.applyEffects(activeChar, target);
				return true;
			}
			catch (Exception e)
			{
				activeChar.sendSysMessage("Failed buffing: " + e.getMessage());
				activeChar.sendSysMessage("Usage: //buff <skillId> <skillLevel>");
				return false;
			}
		}
		else if (command.startsWith("admin_getbuffs"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			command = st.nextToken();
			if (st.hasMoreTokens())
			{
				final String playername = st.nextToken();
				final Player player = World.getInstance().getPlayer(playername);
				if (player != null)
				{
					int page = 0;
					if (st.hasMoreTokens())
					{
						page = Integer.parseInt(st.nextToken());
					}
					
					showBuffs(activeChar, player, page, command.endsWith("_ps"));
					return true;
				}
				
				activeChar.sendSysMessage("The player " + playername + " is not online.");
				return false;
			}
			else if ((activeChar.getTarget() != null) && activeChar.getTarget().isCreature())
			{
				showBuffs(activeChar, activeChar.getTarget().asCreature(), 0, command.endsWith("_ps"));
				return true;
			}
			else
			{
				activeChar.sendPacket(SystemMessageId.THAT_IS_THE_INCORRECT_TARGET);
				return false;
			}
		}
		else if (command.startsWith("admin_stopbuff"))
		{
			try
			{
				final StringTokenizer st = new StringTokenizer(command, " ");
				st.nextToken();
				final int objectId = Integer.parseInt(st.nextToken());
				final int skillId = Integer.parseInt(st.nextToken());
				removeBuff(activeChar, objectId, skillId);
				return true;
			}
			catch (Exception e)
			{
				activeChar.sendSysMessage("Failed removing effect: " + e.getMessage());
				activeChar.sendSysMessage("Usage: //stopbuff <objectId> <skillId>");
				return false;
			}
		}
		else if (command.startsWith("admin_stopallbuffs"))
		{
			try
			{
				final StringTokenizer st = new StringTokenizer(command, " ");
				st.nextToken();
				final int objectId = Integer.parseInt(st.nextToken());
				removeAllBuffs(activeChar, objectId);
				return true;
			}
			catch (Exception e)
			{
				activeChar.sendSysMessage("Failed removing all effects: " + e.getMessage());
				activeChar.sendSysMessage("Usage: //stopallbuffs <objectId>");
				return false;
			}
		}
		else if (command.startsWith("admin_viewblockedeffects"))
		{
			try
			{
				final StringTokenizer st = new StringTokenizer(command, " ");
				st.nextToken();
				final int objectId = Integer.parseInt(st.nextToken());
				viewBlockedEffects(activeChar, objectId);
				return true;
			}
			catch (Exception e)
			{
				activeChar.sendSysMessage("Failed viewing blocked effects: " + e.getMessage());
				activeChar.sendSysMessage("Usage: //viewblockedeffects <objectId>");
				return false;
			}
		}
		else if (command.startsWith("admin_viewskilleffects"))
		{
			try
			{
				final StringTokenizer st = new StringTokenizer(command, " ");
				st.nextToken();
				final int objectId = Integer.parseInt(st.nextToken());
				final int skillId = Integer.parseInt(st.nextToken());
				final int skillLevel = Integer.parseInt(st.nextToken());
				final int page = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 0;
				viewSkillEffects(activeChar, objectId, skillId, skillLevel, page, command.contains("_ps"));
				return true;
			}
			catch (Exception e)
			{
				activeChar.sendSysMessage("Failed viewing skill effects: " + e.getMessage());
				activeChar.sendSysMessage("Usage: //viewskilleffects <objectId skillId skillLevel>");
				return false;
			}
		}
		else if (command.startsWith("admin_areacancel"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			if (!st.hasMoreTokens())
			{
				activeChar.sendSysMessage("Usage: //areacancel <radius>");
				return false;
			}
			
			final String val = st.nextToken();
			try
			{
				final int radius = Integer.parseInt(val);
				final int[] affected =
				{
					0
				};
				World.getInstance().forEachVisibleObjectInRange(activeChar, Player.class, radius, player ->
				{
					player.stopAllEffects();
					affected[0]++;
				});
				
				activeChar.sendSysMessage("Effects canceled for " + affected[0] + " player(s) within radius " + radius);
				return true;
			}
			catch (NumberFormatException e)
			{
				activeChar.sendSysMessage("Invalid radius value.");
				return false;
			}
		}
		else if (command.startsWith("admin_removereuse"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			
			Player player = null;
			if (st.hasMoreTokens())
			{
				final String playername = st.nextToken();
				
				try
				{
					player = World.getInstance().getPlayer(playername);
				}
				catch (Exception e)
				{
				}
				
				if (player == null)
				{
					activeChar.sendSysMessage("The player " + playername + " is not online.");
					return false;
				}
			}
			else if ((activeChar.getTarget() != null) && activeChar.getTarget().isPlayer())
			{
				player = activeChar.getTarget().asPlayer();
			}
			else
			{
				activeChar.sendPacket(SystemMessageId.THAT_IS_THE_INCORRECT_TARGET);
				return false;
			}
			
			try
			{
				player.resetTimeStamps();
				player.resetDisabledSkills();
				player.sendPacket(new SkillCoolTime(player));
				activeChar.sendSysMessage("Skill reuse was removed from " + player.getName() + ".");
				return true;
			}
			catch (NullPointerException e)
			{
				return false;
			}
		}
		else if (command.startsWith("admin_switch_gm_buffs"))
		{
			if (GeneralConfig.GM_GIVE_SPECIAL_SKILLS != GeneralConfig.GM_GIVE_SPECIAL_AURA_SKILLS)
			{
				final boolean toAuraSkills = activeChar.getKnownSkill(7041) != null;
				switchSkills(activeChar, toAuraSkills);
				activeChar.sendSkillList();
				activeChar.sendSysMessage("You have succefully changed to target " + (toAuraSkills ? "aura" : "one") + " special skills.");
				return true;
			}
			
			activeChar.sendSysMessage("There is nothing to switch.");
			return false;
		}
		
		return true;
	}
	
	/**
	 * @param gmchar the player to switch the Game Master skills.
	 * @param toAuraSkills if {@code true} it will remove "GM Aura" skills and add "GM regular" skills, vice versa if {@code false}.
	 */
	public static void switchSkills(Player gmchar, boolean toAuraSkills)
	{
		final Collection<Skill> skills = toAuraSkills ? SkillTreeData.getInstance().getGMSkillTree().values() : SkillTreeData.getInstance().getGMAuraSkillTree().values();
		for (Skill skill : skills)
		{
			gmchar.removeSkill(skill, false); // Don't Save GM skills to database
		}
		
		SkillTreeData.getInstance().addSkills(gmchar, toAuraSkills);
	}
	
	private void showBuffs(Player activeChar, Creature target, int page, boolean passive)
	{
		final List<BuffInfo> effects = new ArrayList<>();
		if (passive)
		{
			effects.addAll(target.getEffectList().getPassives());
		}
		else
		{
			effects.addAll(target.getEffectList().getEffects());
		}
		
		final Map<Skill, Integer> skills = new HashMap<>();
		for (BuffInfo info : effects)
		{
			final Skill skill = info.getSkill();
			if (skill != null)
			{
				if (skills.containsKey(skill))
				{
					skills.put(skill, Math.max(info.getAbnormalTime(), skills.get(skill)));
				}
				else
				{
					skills.put(skill, info.getTime());
				}
			}
		}
		
		int row = 0;
		final String pageLink = "bypass admin_getbuffs" + (passive ? "_ps " : " ") + target.getName();
		final PageResult result = PageBuilder.newBuilder(skills.keySet(), PAGE_LIMIT, pageLink).currentPage(page).style(ButtonsStyle.INSTANCE).bodyHandler((pages, skill, sb) ->
		{
			sb.append((row % 2) == 0 ? "<table width=\"295\" bgcolor=\"000000\">" : "<table width=\"295\">");
			sb.append("<tr><td height=40 width=40><img src=\"");
			sb.append(skill.getIcon());
			sb.append("\" width=32 height=32></td><td width=");
			sb.append(skill.isPassive() ? "850" : "650");
			sb.append("><font color=\"B09878\">");
			sb.append(passive ? "<a action=\"bypass admin_viewskilleffects_ps " : "<a action=\"bypass admin_viewskilleffects ");
			sb.append(target.getObjectId());
			sb.append(" ");
			sb.append(skill.getId());
			sb.append(" ");
			sb.append(skill.getLevel());
			sb.append("\">");
			sb.append(skill.getName());
			if (!skill.getName().contains("Lv") && !skill.getName().contains("Level"))
			{
				sb.append(" Lv. ");
				sb.append(skill.getLevel());
			}
			sb.append(" (");
			sb.append(skill.getId());
			sb.append(")</a></font></td><td fixwidth=");
			sb.append(skill.isPassive() ? "1" : "200");
			sb.append(" height=32 valign=\"center\" align=\"center\">");
			sb.append(skill.isPassive() ? "" : skill.isToggle() ? "T" : (skills.get(skill)) + "s");
			sb.append("</td><td><table><tr><td></td></tr><tr><td fixwidth=360><button value=\"X\" action=\"bypass admin_stopbuff ");
			sb.append(target.getObjectId());
			sb.append(" ");
			sb.append(skill.getId());
			sb.append("\" width=30 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr></table></td></tr></table><img src=\"L2UI.SquareGray\" width=295 height=1>");
		}).build();
		
		final NpcHtmlMessage html = new NpcHtmlMessage();
		html.setFile(activeChar, "data/html/admin/getbuff_skills.htm");
		if (result.getPages() > 1)
		{
			html.replace("%pages%", "<table width=280 cellspacing=0><tr>" + result.getPagerTemplate() + "</tr></table>");
		}
		else
		{
			html.replace("%pages%", "");
		}
		
		html.replace("%buffsText%", passive ? "Hide Passives" : "Show Passives");
		html.replace("%passives%", passive ? "" : "_ps");
		
		html.replace("%targetName%", target.getName());
		html.replace("%targetObjId%", target.getObjectId());
		html.replace("%buffs%", result.getBodyTemplate().toString());
		html.replace("%skillSize%", skills.size());
		activeChar.sendPacket(html);
		
		if (GeneralConfig.GMAUDIT)
		{
			GMAudit.logAction(activeChar.getName() + " [" + activeChar.getObjectId() + "]", "getbuffs", target.getName() + " (" + target.getObjectId() + ")", "");
		}
	}
	
	private void viewSkillEffects(Player activeChar, int objectId, int skillId, int skillLevel, int page, boolean passive)
	{
		Creature target = null;
		try
		{
			target = World.getInstance().findObject(objectId).asCreature();
		}
		catch (Exception e)
		{
			activeChar.sendSysMessage("Target with object id " + objectId + " not found.");
			return;
		}
		
		int count = 0;
		final List<BuffInfo> effects = new ArrayList<>();
		if (passive)
		{
			for (BuffInfo info : target.getEffectList().getPassives())
			{
				final Skill skill = info.getSkill();
				if ((skill != null) && (skill.getId() == skillId) && (skill.getLevel() == skillLevel))
				{
					effects.add(info);
					count += info.getEffects().size();
				}
			}
		}
		else
		{
			for (BuffInfo info : target.getEffectList().getEffects())
			{
				final Skill skill = info.getSkill();
				if ((skill != null) && (skill.getId() == skillId) && (skill.getLevel() == skillLevel))
				{
					effects.add(info);
					count += info.getEffects().size();
				}
			}
		}
		
		int row = 0;
		final String pageLink = "bypass admin_viewskilleffects" + (passive ? "_ps " : " ") + objectId + " " + skillId + " " + skillLevel;
		final PageResult result = PageBuilder.newBuilder(effects, PAGE_LIMIT, pageLink).currentPage(page).style(ButtonsStyle.INSTANCE).bodyHandler((pages, info, sb) ->
		{
			for (AbstractEffect effect : info.getEffects())
			{
				sb.append((row % 2) == 0 ? "<table width=\"295\" bgcolor=\"000000\">" : "<table width=\"295\">");
				sb.append("<tr><td width=750>");
				sb.append(!info.isInUse() ? FONT_RED1 : "");
				sb.append(info.getSkill().getName());
				sb.append(" Lv. ");
				sb.append(info.getSkill().getLevel());
				sb.append(" (");
				sb.append(effect.getClass().getSimpleName());
				sb.append(")");
				sb.append(!info.isInUse() ? FONT_RED2 : "");
				sb.append("</td><td width=180><center>");
				sb.append(info.getSkill().isToggle() ? "T" : info.getSkill().isPassive() ? "P" : info.getTime() + "s");
				sb.append("</center></td><td width=200><button value=\"X\" action=\"bypass admin_stopbuff ");
				sb.append(objectId);
				sb.append(" ");
				sb.append(info.getSkill().getId());
				sb.append("\" width=30 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr></table>");
				sb.append("<img src=\"L2UI.SquareGray\" width=295 height=1>");
			}
			
		}).build();
		
		final NpcHtmlMessage html = new NpcHtmlMessage();
		html.setFile(activeChar, "data/html/admin/getbuff_effects.htm");
		if (result.getPages() > 1)
		{
			html.replace("%pages%", "<table width=280 cellspacing=0><tr>" + result.getPagerTemplate() + "</tr></table>");
		}
		else
		{
			html.replace("%pages%", "");
		}
		
		html.replace("%buffsText%", passive ? "Hide Passives" : "Show Passives");
		html.replace("%passives%", passive ? "" : "_ps");
		
		html.replace("%targetName%", target.getName());
		html.replace("%targetObjId%", target.getObjectId());
		html.replace("%buffs%", result.getBodyTemplate().toString());
		html.replace("%effectSize%", count);
		activeChar.sendPacket(html);
	}
	
	private void removeBuff(Player activeChar, int objId, int skillId)
	{
		Creature target = null;
		try
		{
			target = World.getInstance().findObject(objId).asCreature();
		}
		catch (Exception e)
		{
			// Checked bellow.
		}
		
		if ((target != null) && (skillId > 0))
		{
			if (target.isAffectedBySkill(skillId))
			{
				target.stopSkillEffects(SkillFinishType.REMOVED, skillId);
				activeChar.sendSysMessage("Removed skill ID: " + skillId + " effects from " + target.getName() + " (" + objId + ").");
			}
			
			showBuffs(activeChar, target, 0, false);
			if (GeneralConfig.GMAUDIT)
			{
				GMAudit.logAction(activeChar.getName() + " [" + activeChar.getObjectId() + "]", "stopbuff", target.getName() + " (" + objId + ")", Integer.toString(skillId));
			}
		}
	}
	
	private void removeAllBuffs(Player activeChar, int objId)
	{
		Creature target = null;
		try
		{
			target = World.getInstance().findObject(objId).asCreature();
		}
		catch (Exception e)
		{
			// Checked bellow.
		}
		
		if (target != null)
		{
			target.stopAllEffects();
			activeChar.sendSysMessage("Removed all effects from " + target.getName() + " (" + objId + ")");
			showBuffs(activeChar, target, 0, false);
			if (GeneralConfig.GMAUDIT)
			{
				GMAudit.logAction(activeChar.getName() + " [" + activeChar.getObjectId() + "]", "stopallbuffs", target.getName() + " (" + objId + ")", "");
			}
		}
	}
	
	private void viewBlockedEffects(Player activeChar, int objId)
	{
		Creature target = null;
		try
		{
			target = World.getInstance().findObject(objId).asCreature();
		}
		catch (Exception e)
		{
			activeChar.sendSysMessage("Target with object id " + objId + " not found.");
			return;
		}
		
		if (target != null)
		{
			final Set<AbnormalType> blockedAbnormals = target.getEffectList().getBlockedAbnormalTypes();
			final int blockedAbnormalsSize = blockedAbnormals != null ? blockedAbnormals.size() : 0;
			final StringBuilder html = new StringBuilder(500 + (blockedAbnormalsSize * 50));
			html.append("<html><table width=\"100%\"><tr><td width=45><button value=\"Main\" action=\"bypass admin_admin\" width=45 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td width=180><center><font color=\"LEVEL\">Blocked effects of ");
			html.append(target.getName());
			html.append("</font></td><td width=45><button value=\"Back\" action=\"bypass admin_getbuffs" + (target.isPlayer() ? (" " + target.getName()) : "") + "\" width=45 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr></table><br>");
			if ((blockedAbnormals != null) && !blockedAbnormals.isEmpty())
			{
				html.append("<br>Blocked buff slots: ");
				for (AbnormalType slot : blockedAbnormals)
				{
					html.append("<br>").append(slot.toString());
				}
			}
			
			html.append("</html>");
			
			// Send the packet
			activeChar.sendPacket(new NpcHtmlMessage(html.toString()));
			if (GeneralConfig.GMAUDIT)
			{
				GMAudit.logAction(activeChar.getName() + " [" + activeChar.getObjectId() + "]", "viewblockedeffects", target.getName() + " (" + Integer.toString(target.getObjectId()) + ")", "");
			}
		}
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
