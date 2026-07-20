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

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.custom.SchemeBufferConfig;
import org.l2jmobius.gameserver.data.SchemeBufferTable;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.actor.holders.npc.BuffSkillHolder;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.HtmlUtil;

/**
 * Scheme Buffer NPC handler supporting scheme management and manual buff casting.<br>
 * Provides extra pages (Buffs/Dances/Songs/etc.) and auto-buff presets without exposing internal groups in the edit UI.
 * <ul>
 * <li>Creates, edits, deletes and casts player schemes.</li>
 * <li>Manually casts skills by category with pagination and target selection.</li>
 * <li>Applies class-based auto-buffs using MAGE_GROUP / FIGHTER_GROUP.</li>
 * </ul>
 * @author Mobius, BazookaRpm
 */
public class SchemeBuffer extends Npc
{
	// Constants.
	private static final int PAGE_LIMIT = 6;
	private static final int SCHEME_NAME_MAX_LENGTH = 14;
	private static final int TYPES_PER_ROW = 4;
	private static final String AUTO_BUFF_MAGE_GROUP = "MAGE_GROUP";
	private static final String AUTO_BUFF_FIGHTER_GROUP = "FIGHTER_GROUP";
	
	// HTML layout.
	private static final int HTML_WIDTH = 280;
	private static final int HTML_HALF_WIDTH = 140;
	private static final int HTML_NAV_CELL_WIDTH = 70;
	private static final int HTML_PAGE_CELL_WIDTH = 100;
	private static final int HTML_ICON_CELL_SIZE = 40;
	private static final int HTML_ICON_SIZE = 32;
	private static final int HTML_NAME_CELL_WIDTH = 190;
	
	/**
	 * Creates a SchemeBuffer instance.
	 * @param template
	 */
	public SchemeBuffer(NpcTemplate template)
	{
		super(template);
	}
	
	/**
	 * Handles Scheme Buffer bypass commands.
	 * @param player
	 * @param commandValue
	 */
	@Override
	public void onBypassFeedback(Player player, String commandValue)
	{
		if ((player == null) || (commandValue == null) || commandValue.isEmpty())
		{
			return;
		}
		
		final SchemeBufferTable schemeBufferTable = SchemeBufferTable.getInstance();
		final SkillData skillData = SkillData.getInstance();
		
		final StringTokenizer tokenizer = new StringTokenizer(commandValue.replace("createscheme ", "createscheme;"), ";");
		if (!tokenizer.hasMoreTokens())
		{
			return;
		}
		
		final String currentCommand = tokenizer.nextToken();
		
		if (currentCommand.startsWith("menu"))
		{
			showMainMenu(player);
			return;
		}
		else if (currentCommand.startsWith("cleanup"))
		{
			player.stopAllEffects();
			
			final Summon summon = player.getSummon();
			if (summon != null)
			{
				summon.stopAllEffects();
			}
			
			showMainMenu(player);
			return;
		}
		else if (currentCommand.startsWith("heal"))
		{
			player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp());
			player.setCurrentCp(player.getMaxCp());
			
			final Summon summon = player.getSummon();
			if (summon != null)
			{
				summon.setCurrentHpMp(summon.getMaxHp(), summon.getMaxMp());
			}
			
			showMainMenu(player);
			return;
		}
		else if (currentCommand.startsWith("support"))
		{
			showGiveBuffsWindow(player);
			return;
		}
		else if (currentCommand.startsWith("givebuffs"))
		{
			if (tokenizer.countTokens() < 2)
			{
				return;
			}
			
			final String schemeName = tokenizer.nextToken();
			
			// NOTE: Cost from bypass is client-controlled. Always recalculate server-side.
			// Keep consuming the token to preserve bypass format.
			tokenizer.nextToken();
			
			final List<Integer> scheme = schemeBufferTable.getScheme(player.getObjectId(), schemeName);
			final int cost = getFee(scheme);
			
			Creature target = player;
			if (tokenizer.hasMoreTokens() && "pet".equalsIgnoreCase(tokenizer.nextToken()))
			{
				target = player.getSummon();
			}
			
			if (target == null)
			{
				player.sendMessage("You don't have a pet.");
				return;
			}
			
			if ((cost == 0) || ((SchemeBufferConfig.BUFFER_ITEM_ID == 57) && player.reduceAdena(ItemProcessType.FEE, cost, this, true)) || ((SchemeBufferConfig.BUFFER_ITEM_ID != 57) && player.destroyItemByItemId(ItemProcessType.FEE, SchemeBufferConfig.BUFFER_ITEM_ID, cost, player, true)))
			{
				for (int skillId : scheme)
				{
					final BuffSkillHolder holder = schemeBufferTable.getAvailableBuff(skillId);
					if (holder == null)
					{
						continue;
					}
					
					final Skill skill = skillData.getSkill(skillId, holder.getLevel());
					if (skill != null)
					{
						skill.applyEffects(this, target);
					}
				}
			}
			return;
		}
		else if (currentCommand.startsWith("editschemes"))
		{
			if (tokenizer.countTokens() < 3)
			{
				return;
			}
			
			final String groupType = tokenizer.nextToken();
			final String schemeName = tokenizer.nextToken();
			
			final int page;
			try
			{
				page = Integer.parseInt(tokenizer.nextToken());
			}
			catch (NumberFormatException e)
			{
				return;
			}
			
			showEditSchemeWindow(player, groupType, schemeName, page);
			return;
		}
		else if (currentCommand.startsWith("skill"))
		{
			if (tokenizer.countTokens() < 4)
			{
				return;
			}
			
			final String groupType = tokenizer.nextToken();
			final String schemeName = tokenizer.nextToken();
			
			final int skillId;
			final int page;
			try
			{
				skillId = Integer.parseInt(tokenizer.nextToken());
				page = Integer.parseInt(tokenizer.nextToken());
			}
			catch (NumberFormatException e)
			{
				return;
			}
			
			final Map<String, List<Integer>> playerSchemes = schemeBufferTable.getPlayerSchemes(player.getObjectId());
			if ((playerSchemes == null) || !playerSchemes.containsKey(schemeName))
			{
				player.sendMessage("Invalid scheme name: " + schemeName + ".");
				showGiveBuffsWindow(player);
				return;
			}
			
			final List<Integer> schemeSkills = schemeBufferTable.getScheme(player.getObjectId(), schemeName);
			
			if (currentCommand.startsWith("skillselect") && !schemeName.equalsIgnoreCase("none"))
			{
				final Skill skill = skillData.getSkill(skillId, 1);
				if (skill == null)
				{
					showEditSchemeWindow(player, groupType, schemeName, page);
					return;
				}
				
				final int totalBuffs = schemeSkills.size();
				final int currentDanceSongCount = getCountOf(schemeSkills, true);
				final boolean isDanceOrSong = skill.isDance();
				final int maxCount = player.getStat().getMaxBuffCount();
				
				if (totalBuffs >= maxCount)
				{
					player.sendMessage("This scheme has reached the maximum amount of buffs.");
				}
				else if (isDanceOrSong && (currentDanceSongCount >= PlayerConfig.DANCES_MAX_AMOUNT))
				{
					player.sendMessage("You cannot add more than " + PlayerConfig.DANCES_MAX_AMOUNT + " songs/dances to this scheme.");
				}
				else if (!schemeSkills.contains(skillId))
				{
					schemeBufferTable.addSkillToScheme(player.getObjectId(), schemeName, skillId);
				}
			}
			else if (currentCommand.startsWith("skillunselect"))
			{
				schemeBufferTable.removeSkillFromScheme(player.getObjectId(), schemeName, skillId);
			}
			
			showEditSchemeWindow(player, groupType, schemeName, page);
			return;
		}
		else if (currentCommand.startsWith("createscheme"))
		{
			if (!tokenizer.hasMoreTokens())
			{
				player.sendMessage("Scheme's name must contain up to " + SCHEME_NAME_MAX_LENGTH + " chars.");
				return;
			}
			
			final String schemeName = tokenizer.nextToken().trim();
			if (schemeName.isEmpty() || (schemeName.length() > SCHEME_NAME_MAX_LENGTH))
			{
				player.sendMessage("Scheme's name must contain up to " + SCHEME_NAME_MAX_LENGTH + " chars.");
				return;
			}
			
			if (!StringUtil.isAlphaNumeric(schemeName.replace(" ", "").replace(".", "").replace(",", "").replace("-", "").replace("+", "").replace("!", "").replace("?", "")))
			{
				player.sendMessage("Please use plain alphanumeric characters.");
				return;
			}
			
			final Map<String, List<Integer>> playerSchemes = schemeBufferTable.getPlayerSchemes(player.getObjectId());
			if (playerSchemes != null)
			{
				if (playerSchemes.size() >= SchemeBufferConfig.BUFFER_MAX_SCHEMES)
				{
					player.sendMessage("Maximum schemes amount is already reached.");
					return;
				}
				if (playerSchemes.containsKey(schemeName))
				{
					player.sendMessage("The scheme name already exists: " + schemeName + ".");
					return;
				}
			}
			
			schemeBufferTable.setScheme(player.getObjectId(), schemeName, new ArrayList<>());
			showGiveBuffsWindow(player);
			return;
		}
		else if (currentCommand.startsWith("deletescheme"))
		{
			if (!tokenizer.hasMoreTokens())
			{
				player.sendMessage("This scheme name is invalid.");
				showGiveBuffsWindow(player);
				return;
			}
			
			final String schemeName = tokenizer.nextToken();
			if (schemeName.isEmpty())
			{
				player.sendMessage("This scheme name is invalid.");
				showGiveBuffsWindow(player);
				return;
			}
			
			schemeBufferTable.deleteScheme(player.getObjectId(), schemeName);
			
			showGiveBuffsWindow(player);
			return;
		}
		else if (currentCommand.startsWith("manual"))
		{
			final String category = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : "Buffs";
			
			final int page;
			try
			{
				page = tokenizer.hasMoreTokens() ? Integer.parseInt(tokenizer.nextToken()) : 1;
			}
			catch (NumberFormatException e)
			{
				showMainMenu(player);
				return;
			}
			
			final String targetType = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : "me";
			showManualWindow(player, category, page, targetType);
			return;
		}
		else if (currentCommand.startsWith("castbuff"))
		{
			if (tokenizer.countTokens() < 4)
			{
				return;
			}
			
			try
			{
				final int skillId = Integer.parseInt(tokenizer.nextToken());
				final String category = tokenizer.nextToken();
				final int page = Integer.parseInt(tokenizer.nextToken());
				final String targetType = tokenizer.nextToken();
				
				final Creature target = "pet".equalsIgnoreCase(targetType) ? player.getSummon() : player;
				if (target == null)
				{
					player.sendMessage("You don't have a pet.");
					showManualWindow(player, category, page, targetType);
					return;
				}
				
				final BuffSkillHolder holder = schemeBufferTable.getAvailableBuff(category, skillId);
				if (holder != null)
				{
					final Skill skill = skillData.getSkill(skillId, holder.getLevel());
					if (skill != null)
					{
						skill.applyEffects(this, target);
					}
				}
				
				showManualWindow(player, category, page, targetType);
			}
			catch (NumberFormatException e)
			{
				return;
			}
			return;
		}
		else if (currentCommand.startsWith("autobuff"))
		{
			final String targetType = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : "me";
			applyAutoBuff(player, targetType);
			return;
		}
		
		super.onBypassFeedback(player, commandValue);
	}
	
	/**
	 * Returns the HTML path used by this NPC.
	 * @param npcId
	 * @param value
	 * @return the HTML file path.
	 */
	@Override
	public String getHtmlPath(int npcId, int value)
	{
		return "data/html/mods/SchemeBuffer/" + ((value == 0) ? Integer.toString(npcId) : (npcId + "-" + value)) + ".htm";
	}
	
	/**
	 * Shows the main menu window.
	 * @param player
	 */
	private void showMainMenu(Player player)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player, getHtmlPath(getId(), 0));
		html.replace("%objectId%", getObjectId());
		player.sendPacket(html);
	}
	
	/**
	 * Shows the scheme list and actions window.
	 * @param player
	 */
	private void showGiveBuffsWindow(Player player)
	{
		final StringBuilder htmlBuilder = new StringBuilder(200);
		final Map<String, List<Integer>> playerSchemes = SchemeBufferTable.getInstance().getPlayerSchemes(player.getObjectId());
		
		if ((playerSchemes == null) || playerSchemes.isEmpty())
		{
			htmlBuilder.append("<font color=\"LEVEL\">You haven't defined any scheme.</font>");
		}
		else
		{
			for (Entry<String, List<Integer>> scheme : playerSchemes.entrySet())
			{
				final int cost = getFee(scheme.getValue());
				htmlBuilder.append("<font color=\"LEVEL\">").append(scheme.getKey()).append(" [").append(scheme.getValue().size()).append(" skill(s)]").append((cost > 0) ? (" - cost: " + NumberFormat.getInstance(Locale.ENGLISH).format(cost)) : "").append("</font><br1>");
				
				htmlBuilder.append("<a action=\"bypass -h npc_%objectId%_givebuffs;").append(scheme.getKey()).append(";").append(cost).append("\">Use on Me</a>&nbsp;|&nbsp;");
				htmlBuilder.append("<a action=\"bypass -h npc_%objectId%_givebuffs;").append(scheme.getKey()).append(";").append(cost).append(";pet\">Use on Pet</a>&nbsp;|&nbsp;");
				htmlBuilder.append("<a action=\"bypass -h npc_%objectId%_editschemes;Buffs;").append(scheme.getKey()).append(";1\">Edit</a>&nbsp;|&nbsp;");
				htmlBuilder.append("<a action=\"bypass -h npc_%objectId%_deletescheme;").append(scheme.getKey()).append("\">Delete</a><br>");
			}
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player, getHtmlPath(getId(), 1));
		html.replace("%schemes%", htmlBuilder.toString());
		html.replace("%max_schemes%", SchemeBufferConfig.BUFFER_MAX_SCHEMES);
		html.replace("%objectId%", getObjectId());
		player.sendPacket(html);
	}
	
	/**
	 * Shows the scheme edit window for adding and removing skills.
	 * @param player
	 * @param groupType
	 * @param schemeName
	 * @param page
	 */
	private void showEditSchemeWindow(Player player, String groupType, String schemeName, int page)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		final List<Integer> schemeSkills = SchemeBufferTable.getInstance().getScheme(player.getObjectId(), schemeName);
		
		html.setFile(player, getHtmlPath(getId(), 2));
		html.replace("%schemename%", schemeName);
		html.replace("%count%", (getCountOf(schemeSkills, false) + getCountOf(schemeSkills, true)) + " / " + player.getStat().getMaxBuffCount() + " buffs");
		html.replace("%typesframe%", getTypesFrame(groupType, schemeName));
		html.replace("%skilllistframe%", getGroupSkillList(player, groupType, schemeName, page));
		html.replace("%objectId%", getObjectId());
		player.sendPacket(html);
	}
	
	/**
	 * Shows manual casting window by category with pagination and target selection.
	 * @param player
	 * @param category
	 * @param pageValue
	 * @param targetType
	 */
	private void showManualWindow(Player player, String category, int pageValue, String targetType)
	{
		final SchemeBufferTable schemeBufferTable = SchemeBufferTable.getInstance();
		final SkillData skillData = SkillData.getInstance();
		
		List<Integer> skillIds = schemeBufferTable.getSkillsIdsByType(category);
		if (skillIds.isEmpty())
		{
			player.sendMessage("That category doesn't contain any skills.");
			return;
		}
		
		final int maxPage = HtmlUtil.countPageNumber(skillIds.size(), PAGE_LIMIT);
		int page = pageValue;
		if (page > maxPage)
		{
			page = maxPage;
		}
		if (page < 1)
		{
			page = 1;
		}
		
		skillIds = skillIds.subList((page - 1) * PAGE_LIMIT, Math.min(page * PAGE_LIMIT, skillIds.size()));
		
		final StringBuilder htmlBuilder = new StringBuilder(skillIds.size() * 200);
		
		htmlBuilder.append("<table width=\"").append(HTML_WIDTH).append("\"><tr>");
		
		htmlBuilder.append("<td width=\"").append(HTML_HALF_WIDTH).append("\" align=\"center\">");
		if ("me".equalsIgnoreCase(targetType))
		{
			htmlBuilder.append("<font color=\"LEVEL\">Me</font>");
		}
		else
		{
			htmlBuilder.append("<a action=\"bypass -h npc_").append(getObjectId()).append("_manual;").append(category).append(";").append(page).append(";me\">Me</a>");
		}
		htmlBuilder.append("</td>");
		
		htmlBuilder.append("<td width=\"").append(HTML_HALF_WIDTH).append("\" align=\"center\">");
		if ("pet".equalsIgnoreCase(targetType))
		{
			htmlBuilder.append("<font color=\"LEVEL\">Pet</font>");
		}
		else
		{
			htmlBuilder.append("<a action=\"bypass -h npc_").append(getObjectId()).append("_manual;").append(category).append(";").append(page).append(";pet\">Pet</a>");
		}
		htmlBuilder.append("</td>");
		
		htmlBuilder.append("</tr></table><br1>");
		
		int row = 0;
		for (int skillId : skillIds)
		{
			final BuffSkillHolder holder = schemeBufferTable.getAvailableBuff(category, skillId);
			if (holder == null)
			{
				continue;
			}
			
			final Skill skill = skillData.getSkill(skillId, holder.getLevel());
			if (skill == null)
			{
				continue;
			}
			
			htmlBuilder.append(((row % 2) == 0) ? "<table width=\"" + HTML_WIDTH + "\" bgcolor=\"000000\"><tr>" : "<table width=\"" + HTML_WIDTH + "\"><tr>");
			htmlBuilder.append("<td height=").append(HTML_ICON_CELL_SIZE).append(" width=").append(HTML_ICON_CELL_SIZE).append("><img src=\"").append(skill.getIcon()).append("\" width=").append(HTML_ICON_SIZE).append(" height=").append(HTML_ICON_SIZE).append("></td>");
			htmlBuilder.append("<td width=").append(HTML_NAME_CELL_WIDTH).append(">").append(skill.getName()).append("<br1>");
			htmlBuilder.append("<font color=\"B09878\">").append(holder.getDescription()).append("</font></td>");
			htmlBuilder.append("<td><button action=\"bypass -h npc_").append(getObjectId()).append("_castbuff;").append(skillId).append(";").append(category).append(";").append(page).append(";").append(targetType).append("\" ").append("width=").append(HTML_ICON_SIZE).append(" height=").append(HTML_ICON_SIZE).append(" back=\"L2UI_CH3.mapbutton_zoomin2\" fore=\"L2UI_CH3.mapbutton_zoomin1\"></td>");
			htmlBuilder.append("</tr></table>");
			htmlBuilder.append("<img src=\"L2UI.SquareGray\" width=").append(HTML_WIDTH).append(" height=1>");
			row++;
		}
		
		htmlBuilder.append("<br1><img src=\"L2UI.SquareGray\" width=").append(HTML_WIDTH).append(" height=1><table width=\"100%\" bgcolor=000000><tr>");
		
		if (page > 1)
		{
			htmlBuilder.append("<td align=left width=").append(HTML_NAV_CELL_WIDTH).append("><a action=\"bypass -h npc_").append(getObjectId()).append("_manual;").append(category).append(";").append(page - 1).append(";").append(targetType).append("\">Previous</a></td>");
		}
		else
		{
			htmlBuilder.append("<td align=left width=").append(HTML_NAV_CELL_WIDTH).append(">Previous</td>");
		}
		
		htmlBuilder.append("<td align=center width=").append(HTML_PAGE_CELL_WIDTH).append(">Page ").append(page).append("</td>");
		
		if (page < maxPage)
		{
			htmlBuilder.append("<td align=right width=").append(HTML_NAV_CELL_WIDTH).append("><a action=\"bypass -h npc_").append(getObjectId()).append("_manual;").append(category).append(";").append(page + 1).append(";").append(targetType).append("\">Next</a></td>");
		}
		else
		{
			htmlBuilder.append("<td align=right width=").append(HTML_NAV_CELL_WIDTH).append(">Next</td>");
		}
		
		htmlBuilder.append("</tr></table><img src=\"L2UI.SquareGray\" width=").append(HTML_WIDTH).append(" height=1>");
		htmlBuilder.append("<br1><center><a action=\"bypass -h npc_").append(getObjectId()).append("_menu\">Back</a></center>");
		
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player, getHtmlPath(getId(), 3));
		html.replace("%category%", category);
		html.replace("%skills%", htmlBuilder.toString());
		html.replace("%objectId%", getObjectId());
		player.sendPacket(html);
	}
	
	/**
	 * Applies auto-buff preset based on player class to player or pet.
	 * @param player
	 * @param targetType
	 */
	private void applyAutoBuff(Player player, String targetType)
	{
		final SchemeBufferTable schemeBufferTable = SchemeBufferTable.getInstance();
		final SkillData skillData = SkillData.getInstance();
		
		final Creature target = "pet".equalsIgnoreCase(targetType) ? player.getSummon() : player;
		if (target == null)
		{
			player.sendMessage("You don't have a pet.");
			showMainMenu(player);
			return;
		}
		
		final String category = player.isMageClass() ? AUTO_BUFF_MAGE_GROUP : AUTO_BUFF_FIGHTER_GROUP;
		final List<Integer> skillIds = schemeBufferTable.getSkillsIdsByType(category);
		
		if (skillIds.isEmpty())
		{
			player.sendMessage("Auto buff configuration is not available.");
			showMainMenu(player);
			return;
		}
		
		for (int skillId : skillIds)
		{
			final BuffSkillHolder holder = schemeBufferTable.getAvailableBuff(category, skillId);
			if (holder == null)
			{
				continue;
			}
			
			final Skill skill = skillData.getSkill(skillId, holder.getLevel());
			if (skill != null)
			{
				skill.applyEffects(this, target);
			}
		}
		
		player.sendMessage("Auto buff applied successfully!");
		showMainMenu(player);
	}
	
	/**
	 * Builds paginated skill list for scheme edit window.
	 * @param player
	 * @param groupType
	 * @param schemeName
	 * @param pageValue
	 * @return the HTML fragment.
	 */
	private String getGroupSkillList(Player player, String groupType, String schemeName, int pageValue)
	{
		final SchemeBufferTable schemeBufferTable = SchemeBufferTable.getInstance();
		final SkillData skillData = SkillData.getInstance();
		
		List<Integer> skillIds = schemeBufferTable.getSkillsIdsByType(groupType);
		if (skillIds.isEmpty())
		{
			return "That group doesn't contain any skills.";
		}
		
		final int maxPage = HtmlUtil.countPageNumber(skillIds.size(), PAGE_LIMIT);
		int page = pageValue;
		if (page > maxPage)
		{
			page = maxPage;
		}
		if (page < 1)
		{
			page = 1;
		}
		
		skillIds = skillIds.subList((page - 1) * PAGE_LIMIT, Math.min(page * PAGE_LIMIT, skillIds.size()));
		
		final List<Integer> schemeSkills = schemeBufferTable.getScheme(player.getObjectId(), schemeName);
		final StringBuilder htmlBuilder = new StringBuilder(skillIds.size() * 150);
		
		int row = 0;
		for (int skillId : skillIds)
		{
			final BuffSkillHolder holder = schemeBufferTable.getAvailableBuff(groupType, skillId);
			if (holder == null)
			{
				continue;
			}
			
			final Skill skill = skillData.getSkill(skillId, 1);
			if (skill == null)
			{
				continue;
			}
			
			htmlBuilder.append(((row % 2) == 0) ? "<table width=\"" + HTML_WIDTH + "\" bgcolor=\"000000\"><tr>" : "<table width=\"" + HTML_WIDTH + "\"><tr>");
			htmlBuilder.append("<td height=").append(HTML_ICON_CELL_SIZE).append(" width=").append(HTML_ICON_CELL_SIZE).append("><img src=\"").append(skill.getIcon()).append("\" width=").append(HTML_ICON_SIZE).append(" height=").append(HTML_ICON_SIZE).append("></td>");
			htmlBuilder.append("<td width=").append(HTML_NAME_CELL_WIDTH).append(">").append(skill.getName()).append("<br1><font color=\"B09878\">").append(holder.getDescription()).append("</font></td>");
			
			if (schemeSkills.contains(skillId))
			{
				htmlBuilder.append("<td><button action=\"bypass -h npc_%objectId%_skillunselect;").append(groupType).append(";").append(schemeName).append(";").append(skillId).append(";").append(page).append("\" ").append("width=").append(HTML_ICON_SIZE).append(" height=").append(HTML_ICON_SIZE).append(" back=\"L2UI_CH3.mapbutton_zoomout2\" fore=\"L2UI_CH3.mapbutton_zoomout1\"></td>");
			}
			else
			{
				htmlBuilder.append("<td><button action=\"bypass -h npc_%objectId%_skillselect;").append(groupType).append(";").append(schemeName).append(";").append(skillId).append(";").append(page).append("\" ").append("width=").append(HTML_ICON_SIZE).append(" height=").append(HTML_ICON_SIZE).append(" back=\"L2UI_CH3.mapbutton_zoomin2\" fore=\"L2UI_CH3.mapbutton_zoomin1\"></td>");
			}
			
			htmlBuilder.append("</tr></table><img src=\"L2UI.SquareGray\" width=").append(HTML_WIDTH).append(" height=1>");
			row++;
		}
		
		htmlBuilder.append("<br1><img src=\"L2UI.SquareGray\" width=").append(HTML_WIDTH).append(" height=1><table width=\"100%\" bgcolor=000000><tr>");
		
		if (page > 1)
		{
			htmlBuilder.append("<td align=left width=").append(HTML_NAV_CELL_WIDTH).append("><a action=\"bypass -h npc_").append(getObjectId()).append("_editschemes;").append(groupType).append(";").append(schemeName).append(";").append(page - 1).append("\">Previous</a></td>");
		}
		else
		{
			htmlBuilder.append("<td align=left width=").append(HTML_NAV_CELL_WIDTH).append(">Previous</td>");
		}
		
		htmlBuilder.append("<td align=center width=").append(HTML_PAGE_CELL_WIDTH).append(">Page ").append(page).append("</td>");
		
		if (page < maxPage)
		{
			htmlBuilder.append("<td align=right width=").append(HTML_NAV_CELL_WIDTH).append("><a action=\"bypass -h npc_").append(getObjectId()).append("_editschemes;").append(groupType).append(";").append(schemeName).append(";").append(page + 1).append("\">Next</a></td>");
		}
		else
		{
			htmlBuilder.append("<td align=right width=").append(HTML_NAV_CELL_WIDTH).append(">Next</td>");
		}
		
		htmlBuilder.append("</tr></table><img src=\"L2UI.SquareGray\" width=").append(HTML_WIDTH).append(" height=1>");
		return htmlBuilder.toString();
	}
	
	/**
	 * Builds the category selector frame for scheme editing.
	 * @param groupType
	 * @param schemeName
	 * @return the HTML fragment.
	 */
	private static String getTypesFrame(String groupType, String schemeName)
	{
		final StringBuilder htmlBuilder = new StringBuilder(500);
		htmlBuilder.append("<table>");
		
		int count = 0;
		for (String type : SchemeBufferTable.getInstance().getSkillTypes())
		{
			if (count == 0)
			{
				htmlBuilder.append("<tr>");
			}
			
			if (groupType.equalsIgnoreCase(type))
			{
				htmlBuilder.append("<td width=65>").append(type).append("</td>");
			}
			else
			{
				htmlBuilder.append("<td width=65><a action=\"bypass -h npc_%objectId%_editschemes;").append(type).append(";").append(schemeName).append(";1\">").append(type).append("</a></td>");
			}
			
			count++;
			if (count == TYPES_PER_ROW)
			{
				htmlBuilder.append("</tr>");
				count = 0;
			}
		}
		
		if (count != 0)
		{
			htmlBuilder.append("</tr>");
		}
		
		htmlBuilder.append("</table>");
		return htmlBuilder.toString();
	}
	
	/**
	 * Computes the total fee for a skill list.
	 * @param list
	 * @return the fee.
	 */
	private static int getFee(List<Integer> list)
	{
		if (SchemeBufferConfig.BUFFER_STATIC_BUFF_COST > 0)
		{
			return list.size() * SchemeBufferConfig.BUFFER_STATIC_BUFF_COST;
		}
		
		int fee = 0;
		for (int skillId : list)
		{
			final BuffSkillHolder holder = SchemeBufferTable.getInstance().getAvailableBuff(skillId);
			if (holder != null)
			{
				fee += holder.getPrice();
			}
		}
		return fee;
	}
	
	/**
	 * Counts skills in a list as dances/songs or non-dances.
	 * @param skills
	 * @param dances
	 * @return the count.
	 */
	private static int getCountOf(List<Integer> skills, boolean dances)
	{
		final SkillData skillData = SkillData.getInstance();
		
		int count = 0;
		for (int skillId : skills)
		{
			final Skill skill = skillData.getSkill(skillId, 1);
			if ((skill != null) && (skill.isDance() == dances))
			{
				count++;
			}
		}
		return count;
	}
}
