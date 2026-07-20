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
package handlers.chat.commands.voiced;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.config.custom.AutoPlayConfig;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.OptionData;
import org.l2jmobius.gameserver.data.xml.PetSkillData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.events.Containers;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogout;
import org.l2jmobius.gameserver.model.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.options.OptionSkillHolder;
import org.l2jmobius.gameserver.model.options.Options;
import org.l2jmobius.gameserver.model.skill.AbnormalType;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.taskmanagers.AutoPlayTaskManager;
import org.l2jmobius.gameserver.taskmanagers.AutoUseTaskManager;
import org.l2jmobius.gameserver.util.HtmlUtil;

/**
 * @author Mobius
 */
public class AutoPlay implements IVoicedCommandHandler
{
	private static final int PAGE_LIMIT = 7;
	private static final Integer AUTO_ATTACK_ACTION = 2;
	
	private static final String[] VOICED_COMMANDS =
	{
		"play",
		"playskills",
		"playitems",
		"playpotion"
	};
	
	private static final Consumer<OnPlayerLogin> ON_PLAYER_LOGIN = event ->
	{
		if (!AutoPlayConfig.ENABLE_AUTO_PLAY)
		{
			return;
		}
		
		final Player player = event.getPlayer();
		if (!AutoPlayConfig.AUTO_PLAY_LOGIN_MESSAGE.isEmpty())
		{
			player.sendPacket(new CreatureSay(null, ChatType.ANNOUNCEMENT, "AutoPlay", AutoPlayConfig.AUTO_PLAY_LOGIN_MESSAGE));
		}
		
		player.getVariables().getIntegerList(PlayerVariables.AUTO_USE_ACTIONS).forEach(id -> player.getAutoUseSettings().getAutoActions().add(id));
		player.getVariables().getIntegerList(PlayerVariables.AUTO_USE_BUFFS).forEach(id -> player.getAutoUseSettings().getAutoBuffs().add(id));
		player.getVariables().getIntegerList(PlayerVariables.AUTO_USE_SKILLS).forEach(id -> player.getAutoUseSettings().getAutoSkills().add(id));
		player.getVariables().getIntegerList(PlayerVariables.AUTO_USE_ITEMS).forEach(id -> player.getAutoUseSettings().getAutoSupplyItems().add(id));
		player.getAutoUseSettings().setAutoPotionItem(player.getVariables().getInt(PlayerVariables.AUTO_USE_POTION, 0));
		
		final List<Integer> settings = player.getVariables().getIntegerList(PlayerVariables.AUTO_USE_SETTINGS);
		if (settings.isEmpty())
		{
			return;
		}
		
		final int options = settings.get(0);
		final boolean active = AutoPlayConfig.RESUME_AUTO_PLAY && (settings.get(1) == 1);
		final boolean pickUp = settings.get(2) == 1;
		final int nextTargetMode = settings.get(3);
		final boolean shortRange = settings.get(4) == 1;
		final int potionPercent = settings.get(5);
		final boolean respectfulHunting = settings.get(6) == 1;
		
		player.getAutoPlaySettings().setAutoPotionPercent(potionPercent);
		player.getAutoPlaySettings().setOptions(options);
		player.getAutoPlaySettings().setPickup(pickUp);
		player.getAutoPlaySettings().setNextTargetMode(nextTargetMode);
		player.getAutoPlaySettings().setShortRange(shortRange);
		player.getAutoPlaySettings().setRespectfulHunting(respectfulHunting);
		
		if (active)
		{
			AutoPlayTaskManager.getInstance().startAutoPlay(player);
		}
	};
	
	private static final Consumer<OnPlayerLogout> ON_PLAYER_LOGOUT = event ->
	{
		if (!AutoPlayConfig.ENABLE_AUTO_PLAY)
		{
			return;
		}
		
		final Player player = event.getPlayer();
		player.getVariables().setIntegerList(PlayerVariables.AUTO_USE_ACTIONS, new ArrayList<>(player.getAutoUseSettings().getAutoActions()));
		player.getVariables().setIntegerList(PlayerVariables.AUTO_USE_BUFFS, new ArrayList<>(player.getAutoUseSettings().getAutoBuffs()));
		player.getVariables().setIntegerList(PlayerVariables.AUTO_USE_SKILLS, new ArrayList<>(player.getAutoUseSettings().getAutoSkills()));
		player.getVariables().setIntegerList(PlayerVariables.AUTO_USE_ITEMS, new ArrayList<>(player.getAutoUseSettings().getAutoSupplyItems()));
		
		final int potionId = player.getVariables().getInt(PlayerVariables.AUTO_USE_POTION, 0);
		if (potionId < 1)
		{
			player.getVariables().remove(PlayerVariables.AUTO_USE_POTION);
		}
		else
		{
			player.getVariables().set(PlayerVariables.AUTO_USE_POTION, potionId);
		}
		
		final List<Integer> settings = new ArrayList<>(7);
		settings.add(0, player.getAutoPlaySettings().getOptions());
		settings.add(1, player.isAutoPlaying() ? 1 : 0);
		settings.add(2, player.getAutoPlaySettings().doPickup() ? 1 : 0);
		settings.add(3, player.getAutoPlaySettings().getNextTargetMode());
		settings.add(4, player.getAutoPlaySettings().isShortRange() ? 1 : 0);
		settings.add(5, player.getAutoPlaySettings().getAutoPotionPercent());
		settings.add(6, player.getAutoPlaySettings().isRespectfulHunting() ? 1 : 0);
		player.getVariables().setIntegerList(PlayerVariables.AUTO_USE_SETTINGS, settings);
	};
	
	public AutoPlay()
	{
		Containers.Players().addListener(new ConsumerEventListener(Containers.Players(), EventType.ON_PLAYER_LOGIN, ON_PLAYER_LOGIN, this));
		Containers.Players().addListener(new ConsumerEventListener(Containers.Players(), EventType.ON_PLAYER_LOGOUT, ON_PLAYER_LOGOUT, this));
	}
	
	@Override
	public boolean onCommand(String command, Player player, String params)
	{
		if (!AutoPlayConfig.ENABLE_AUTO_PLAY || (player == null))
		{
			return false;
		}
		
		if (AutoPlayConfig.AUTO_PLAY_PREMIUM && !player.hasPremiumStatus())
		{
			player.sendPacket(new ExShowScreenMessage("This command is only available to premium players.", 5000));
			player.sendMessage("This command is only available to premium players.");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		switch (command)
		{
			case "play":
			{
				if (params != null)
				{
					final String[] paramArray = params.toLowerCase().split(" ");
					COMMAND: switch (paramArray[0])
					{
						case "attack":
						{
							if (player.getAutoUseSettings().getAutoActions().contains(AUTO_ATTACK_ACTION))
							{
								player.getAutoUseSettings().getAutoActions().remove(AUTO_ATTACK_ACTION);
							}
							else
							{
								player.getAutoUseSettings().getAutoActions().add(AUTO_ATTACK_ACTION);
							}
							break COMMAND;
						}
						case "loot":
						{
							player.getAutoPlaySettings().setPickup(!player.getAutoPlaySettings().doPickup());
							break COMMAND;
						}
						case "respect":
						{
							player.getAutoPlaySettings().setRespectfulHunting(!player.getAutoPlaySettings().isRespectfulHunting());
							break COMMAND;
						}
						case "range":
						{
							player.getAutoPlaySettings().setShortRange(!player.getAutoPlaySettings().isShortRange());
							break COMMAND;
						}
						case "mode0":
						{
							player.getAutoPlaySettings().setNextTargetMode(0);
							break COMMAND;
						}
						case "mode1":
						{
							player.getAutoPlaySettings().setNextTargetMode(1);
							break COMMAND;
						}
						case "mode2":
						{
							player.getAutoPlaySettings().setNextTargetMode(2);
							break COMMAND;
						}
						case "mode3":
						{
							player.getAutoPlaySettings().setNextTargetMode(3);
							break COMMAND;
						}
						case "percent":
						{
							if ((paramArray.length > 1) && StringUtil.isNumeric(paramArray[1]))
							{
								player.getAutoPlaySettings().setAutoPotionPercent(Math.max(0, Math.min(100, Integer.parseInt(paramArray[1]))));
							}
							break COMMAND;
						}
						case "start":
						{
							AutoPlayTaskManager.getInstance().startAutoPlay(player);
							AutoUseTaskManager.getInstance().startAutoUseTask(player);
							break COMMAND;
						}
						case "stop":
						{
							AutoPlayTaskManager.getInstance().stopAutoPlay(player);
							AutoUseTaskManager.getInstance().stopAutoUseTask(player);
							break COMMAND;
						}
					}
				}
				
				final NpcHtmlMessage html = new NpcHtmlMessage();
				String content = HtmCache.getInstance().getHtm(player, "data/html/mods/AutoPlay/Main.htm");
				
				content = content.replace("%attack%", player.getAutoUseSettings().getAutoActions().contains(AUTO_ATTACK_ACTION) ? "L2UI.CheckBox_checked" : "L2UI.CheckBox");
				content = content.replace("%loot%", player.getAutoPlaySettings().doPickup() ? "L2UI.CheckBox_checked" : "L2UI.CheckBox");
				content = content.replace("%respect%", player.getAutoPlaySettings().isRespectfulHunting() ? "L2UI.CheckBox_checked" : "L2UI.CheckBox");
				content = content.replace("%range%", !player.getAutoPlaySettings().isShortRange() ? "L2UI.CheckBox_checked" : "L2UI.CheckBox");
				
				content = content.replace("%mode0%", player.getAutoPlaySettings().getNextTargetMode() == 0 ? "L2UI_CH3.radiobutton2" : "L2UI_CH3.radiobutton1");
				content = content.replace("%mode1%", player.getAutoPlaySettings().getNextTargetMode() == 1 ? "L2UI_CH3.radiobutton2" : "L2UI_CH3.radiobutton1");
				content = content.replace("%mode2%", player.getAutoPlaySettings().getNextTargetMode() == 2 ? "L2UI_CH3.radiobutton2" : "L2UI_CH3.radiobutton1");
				content = content.replace("%mode3%", player.getAutoPlaySettings().getNextTargetMode() == 3 ? "L2UI_CH3.radiobutton2" : "L2UI_CH3.radiobutton1");
				
				content = content.replace("%skill_button%", AutoPlayConfig.ENABLE_AUTO_SKILL ? "<br><table width=295><tr><td height=31><center><button action=\"bypass voice .playskills\" value=\"Skills\" width=95 height=21 back=\"bigbutton_over\" fore=\"bigbutton\"></center></td></tr></table>" : "");
				content = content.replace("%item_button%", AutoPlayConfig.ENABLE_AUTO_ITEM ? "<br><table width=295><tr><td height=31><center><button action=\"bypass voice .playitems\" value=\"Supply Items\" width=95 height=21 back=\"bigbutton_over\" fore=\"bigbutton\"></center></td></tr></table>" : "");
				content = content.replace("%potion_button%", AutoPlayConfig.ENABLE_AUTO_POTION ? "<br><table><tr><td height=31><center><button action=\"bypass voice .playpotion\" value=\"Healing Potion\" width=95 height=21 back=\"bigbutton_over\" fore=\"bigbutton\"></center></td></tr></table><table width=295><tr><td height=31><center><table width=150><tr><td width=120><font color=\"CDB67F\">HP Percent (%percent%)</font></td><td><edit var=\"percentbox\" width=30 height=13></td><td><button value=\"Apply\" action=\"bypass voice .play percent $percentbox\" width=65 height=21 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\"></td></tr></table></center></td></tr></table>" : "");
				content = content.replace("%percent%", String.valueOf(player.getAutoPlaySettings().getAutoPotionPercent()));
				
				if (player.isAutoPlaying())
				{
					content = content.replace("%status_button%", "<table width=295><tr><td height=31><center><button action=\"bypass voice .play stop\" value=\"Stop\" width=95 height=21 back=\"bigbutton_over\" fore=\"bigbutton\"></center></td></tr></table>");
				}
				else
				{
					content = content.replace("%status_button%", "<table width=295><tr><td height=31><center><button action=\"bypass voice .play start\" value=\"Start\" width=95 height=21 back=\"bigbutton_over\" fore=\"bigbutton\"></center></td></tr></table>");
				}
				
				html.setHtml(content);
				player.sendPacket(html);
				break;
			}
			case "playskills":
			{
				final NpcHtmlMessage html = new NpcHtmlMessage();
				final String content = HtmCache.getInstance().getHtm(player, "data/html/mods/AutoPlay/Skills.htm");
				
				// Generate the skill list. Filter our some skills.
				List<Skill> skills = new ArrayList<>();
				final Set<Skill> siegeSkills = new HashSet<>();
				final Skill[] siegeSkillArray = SkillData.getInstance().getSiegeSkills(true, true);
				if (siegeSkillArray != null)
				{
					for (Skill s : siegeSkillArray)
					{
						if (s != null)
						{
							siegeSkills.add(s);
						}
					}
				}
				
				for (Skill skill : player.getAllSkills())
				{
					if (!siegeSkills.contains(skill) && !skill.isPassive() && !skill.isToggle() && !skills.contains(skill) && !AutoPlayConfig.DISABLED_AUTO_SKILLS.contains(skill.getId()))
					{
						skills.add(skill);
					}
				}
				
				if (player.hasServitor() || player.hasPet())
				{
					final Summon summon = player.getSummon();
					for (Skill skill : summon.getAllSkills())
					{
						if (!skill.isPassive() && !skill.isToggle() && !skills.contains(skill) && !AutoPlayConfig.DISABLED_AUTO_SKILLS.contains(skill.getId()))
						{
							skills.add(skill);
						}
					}
					
					for (Skill skill : PetSkillData.getInstance().getKnownSkills(summon))
					{
						if (!skill.isPassive() && !skill.isToggle() && !skills.contains(skill) && !AutoPlayConfig.DISABLED_AUTO_SKILLS.contains(skill.getId()))
						{
							skills.add(skill);
						}
					}
				}
				
				// Remove item skills.
				for (Item item : player.getInventory().getPaperdollItems())
				{
					final ItemTemplate template = item.getTemplate();
					if (template.hasSkills())
					{
						for (SkillHolder holder : template.getSkills())
						{
							final Skill skill = holder.getSkill();
							if (skill != null)
							{
								skills.remove(skill);
							}
						}
					}
					
					if (item.isAugmented())
					{
						final Options options = OptionData.getInstance().getOptions(item.getAugmentation().getAugmentationId());
						if ((options != null) && options.hasActivationSkills())
						{
							for (OptionSkillHolder holder : options.getActivationSkills())
							{
								final Skill skill = holder.getSkill();
								if (skill != null)
								{
									skills.remove(skill);
								}
							}
						}
					}
				}
				
				// Manage skill activation.
				final String[] paramArray = params == null ? new String[0] : params.split(" ");
				if (paramArray.length > 1)
				{
					final Integer skillId = Integer.parseInt(paramArray[1]);
					Skill knownSkill = player.getKnownSkill(skillId);
					if ((knownSkill == null) && (player.hasServitor() || player.hasPet()))
					{
						final Summon summon = player.getSummon();
						knownSkill = summon.getKnownSkill(skillId);
						if (knownSkill == null)
						{
							knownSkill = PetSkillData.getInstance().getKnownSkill(summon, skillId);
						}
						
						if (knownSkill != null)
						{
							break;
						}
					}
					
					if (AutoPlayConfig.ENABLE_AUTO_SKILL && (knownSkill != null) && skills.contains(knownSkill))
					{
						if (knownSkill.hasNegativeEffect())
						{
							if (player.getAutoUseSettings().getAutoSkills().contains(skillId))
							{
								player.getAutoUseSettings().getAutoSkills().remove(skillId);
							}
							else
							{
								player.getAutoUseSettings().getAutoSkills().add(skillId);
							}
						}
						else
						{
							if (player.getAutoUseSettings().getAutoBuffs().contains(skillId))
							{
								player.getAutoUseSettings().getAutoBuffs().remove(skillId);
							}
							else
							{
								player.getAutoUseSettings().getAutoBuffs().add(skillId);
							}
						}
					}
				}
				
				// Calculate page number.
				final int max = HtmlUtil.countPageNumber(skills.size(), PAGE_LIMIT);
				int page = params == null ? 1 : Integer.parseInt(paramArray[0]);
				if (page > max)
				{
					page = max;
				}
				
				// Cut skills list up to page number.
				final StringBuilder sb = new StringBuilder();
				skills = skills.subList(Math.max(0, (page - 1) * PAGE_LIMIT), Math.min(page * PAGE_LIMIT, skills.size()));
				if (skills.isEmpty())
				{
					sb.append("<center><br>No skills found.<br></center>");
				}
				else
				{
					// Generate skill table.
					int row = 0;
					for (Skill skill : skills)
					{
						sb.append(((row % 2) == 0 ? "<table width=\"295\" bgcolor=\"000000\"><tr>" : "<table width=\"295\"><tr>"));
						if (player.getAutoUseSettings().getAutoBuffs().contains(skill.getId()) || player.getAutoUseSettings().getAutoSkills().contains(skill.getId()))
						{
							sb.append("<td height=40 width=40><img src=\"" + skill.getIcon() + "\" width=32 height=32></td><td width=190>" + skill.getName() + "</td><td><button value=\" \" action=\"bypass voice .playskills " + page + " " + skill.getId() + "\" width=32 height=32 back=\"L2UI_CH3.mapbutton_zoomout2\" fore=\"L2UI_CH3.mapbutton_zoomout1\"></td>");
						}
						else
						{
							sb.append("<td height=40 width=40><img src=\"" + skill.getIcon() + "\" width=32 height=32></td><td width=190><font color=\"B09878\">" + skill.getName() + "</font></td><td><button value=\" \" action=\"bypass voice .playskills " + page + " " + skill.getId() + "\" width=32 height=32 back=\"L2UI_CH3.mapbutton_zoomin2\" fore=\"L2UI_CH3.mapbutton_zoomin1\"></td>");
						}
						sb.append("</tr></table><img src=\"L2UI.SquareGray\" width=295 height=1>");
					}
					
					// Generate page footer.
					sb.append("<br><img src=\"L2UI.SquareGray\" width=295 height=1><table width=\"100%\" bgcolor=000000><tr>");
					if (page > 1)
					{
						sb.append("<td align=left width=70><a action=\"bypass voice .playskills " + (page - 1) + "\"><font color=\"CDB67F\">Previous</font></a></td>");
					}
					else
					{
						sb.append("<td align=left width=70><font color=\"B09878\">Previous</font></td>");
					}
					sb.append("<td align=center width=100>Page " + page + " of " + max + "</td>");
					if (page < max)
					{
						sb.append("<td align=right width=70><a action=\"bypass voice .playskills " + (page + 1) + "\"><font color=\"CDB67F\">Next</font></a></td>");
					}
					else
					{
						sb.append("<td align=right width=70><font color=\"B09878\">Next</font></td>");
					}
					sb.append("</tr></table><img src=\"L2UI.SquareGray\" width=295 height=1>");
				}
				
				// Replace and send the html.
				html.setHtml(content.replace("%skills%", sb.toString()));
				player.sendPacket(html);
				break;
			}
			case "playitems":
			{
				final NpcHtmlMessage html = new NpcHtmlMessage();
				final String content = HtmCache.getInstance().getHtm(player, "data/html/mods/AutoPlay/Items.htm");
				
				// Generate the item list. Filter our some items.
				List<ItemTemplate> items = new ArrayList<>();
				ITEM_SEARCH: for (Item item : player.getInventory().getItems())
				{
					final ItemTemplate template = item.getTemplate();
					if (item.isEtcItem() && template.hasSkills() && !AutoPlayConfig.DISABLED_AUTO_ITEMS.contains(item.getId()))
					{
						for (SkillHolder holder : template.getSkills())
						{
							final Skill skill = holder.getSkill();
							if ((skill != null) && skill.isContinuous() && (skill.getAbnormalType() != AbnormalType.HP_RECOVER) && !items.contains(template))
							{
								items.add(template);
								continue ITEM_SEARCH;
							}
						}
					}
				}
				
				// Manage item activation.
				final String[] paramArray = params == null ? new String[0] : params.split(" ");
				if (paramArray.length > 1)
				{
					final int itemId = Integer.parseInt(paramArray[1]);
					if (AutoPlayConfig.ENABLE_AUTO_ITEM && items.contains(ItemData.getInstance().getTemplate(itemId)))
					{
						if (player.getAutoUseSettings().getAutoSupplyItems().contains(itemId))
						{
							player.getAutoUseSettings().getAutoSupplyItems().remove(itemId);
						}
						else
						{
							player.getAutoUseSettings().getAutoSupplyItems().add(itemId);
						}
					}
				}
				
				// Calculate page number.
				final int max = HtmlUtil.countPageNumber(items.size(), PAGE_LIMIT);
				int page = params == null ? 1 : Integer.parseInt(paramArray[0]);
				if (page > max)
				{
					page = max;
				}
				
				// Cut items list up to page number.
				final StringBuilder sb = new StringBuilder();
				items = items.subList(Math.max(0, (page - 1) * PAGE_LIMIT), Math.min(page * PAGE_LIMIT, items.size()));
				if (items.isEmpty())
				{
					sb.append("<center><br>No items found.<br></center>");
				}
				else
				{
					// Generate item table.
					int row = 0;
					for (ItemTemplate template : items)
					{
						sb.append(((row % 2) == 0 ? "<table width=\"295\" bgcolor=\"000000\"><tr>" : "<table width=\"295\"><tr>"));
						if (player.getAutoUseSettings().getAutoSupplyItems().contains(template.getId()))
						{
							sb.append("<td height=40 width=40><img src=\"" + template.getIcon() + "\" width=32 height=32></td><td width=190>" + template.getName() + "</td><td><button value=\" \" action=\"bypass voice .playitems " + page + " " + template.getId() + "\" width=32 height=32 back=\"L2UI_CH3.mapbutton_zoomout2\" fore=\"L2UI_CH3.mapbutton_zoomout1\"></td>");
						}
						else
						{
							sb.append("<td height=40 width=40><img src=\"" + template.getIcon() + "\" width=32 height=32></td><td width=190><font color=\"B09878\">" + template.getName() + "</font></td><td><button value=\" \" action=\"bypass voice .playitems " + page + " " + template.getId() + "\" width=32 height=32 back=\"L2UI_CH3.mapbutton_zoomin2\" fore=\"L2UI_CH3.mapbutton_zoomin1\"></td>");
						}
						sb.append("</tr></table><img src=\"L2UI.SquareGray\" width=295 height=1>");
					}
					
					// Generate page footer.
					sb.append("<br><img src=\"L2UI.SquareGray\" width=295 height=1><table width=\"100%\" bgcolor=000000><tr>");
					if (page > 1)
					{
						sb.append("<td align=left width=70><a action=\"bypass voice .playitems " + (page - 1) + "\"><font color=\"CDB67F\">Previous</font></a></td>");
					}
					else
					{
						sb.append("<td align=left width=70><font color=\"B09878\">Previous</font></td>");
					}
					sb.append("<td align=center width=100>Page " + page + " of " + max + "</td>");
					if (page < max)
					{
						sb.append("<td align=right width=70><a action=\"bypass voice .playitems " + (page + 1) + "\"><font color=\"CDB67F\">Next</font></a></td>");
					}
					else
					{
						sb.append("<td align=right width=70><font color=\"B09878\">Next</font></td>");
					}
					sb.append("</tr></table><img src=\"L2UI.SquareGray\" width=295 height=1>");
				}
				
				// Replace and send the html.
				html.setHtml(content.replace("%items%", sb.toString()));
				player.sendPacket(html);
				break;
			}
			case "playpotion":
			{
				final NpcHtmlMessage html = new NpcHtmlMessage();
				final String content = HtmCache.getInstance().getHtm(player, "data/html/mods/AutoPlay/Potion.htm");
				
				// Generate the item list. Filter our some items.
				List<ItemTemplate> items = new ArrayList<>();
				POTION_SEARCH: for (Item item : player.getInventory().getItems())
				{
					final ItemTemplate template = item.getTemplate();
					if (item.isEtcItem() && template.hasSkills() && !AutoPlayConfig.DISABLED_AUTO_ITEMS.contains(item.getId()))
					{
						for (SkillHolder holder : template.getSkills())
						{
							final Skill skill = holder.getSkill();
							if ((skill != null) && (skill.getAbnormalType() == AbnormalType.HP_RECOVER) && !items.contains(template))
							{
								items.add(template);
								continue POTION_SEARCH;
							}
						}
					}
				}
				
				// Manage item activation.
				final String[] paramArray = params == null ? new String[0] : params.split(" ");
				if (paramArray.length > 1)
				{
					final int itemId = Integer.parseInt(paramArray[1]);
					if (AutoPlayConfig.ENABLE_AUTO_POTION && items.contains(ItemData.getInstance().getTemplate(itemId)))
					{
						if (player.getAutoUseSettings().getAutoPotionItem() == itemId)
						{
							player.getAutoUseSettings().setAutoPotionItem(0);
						}
						else
						{
							player.getAutoUseSettings().setAutoPotionItem(itemId);
						}
					}
				}
				
				// Calculate page number.
				final int max = HtmlUtil.countPageNumber(items.size(), PAGE_LIMIT);
				int page = params == null ? 1 : Integer.parseInt(paramArray[0]);
				if (page > max)
				{
					page = max;
				}
				
				// Cut items list up to page number.
				final StringBuilder sb = new StringBuilder();
				items = items.subList(Math.max(0, (page - 1) * PAGE_LIMIT), Math.min(page * PAGE_LIMIT, items.size()));
				if (items.isEmpty())
				{
					sb.append("<center><br>No potions found.<br></center>");
				}
				else
				{
					// Generate item table.
					int row = 0;
					for (ItemTemplate template : items)
					{
						sb.append(((row % 2) == 0 ? "<table width=\"295\" bgcolor=\"000000\"><tr>" : "<table width=\"295\"><tr>"));
						if (player.getAutoUseSettings().getAutoPotionItem() == template.getId())
						{
							sb.append("<td height=40 width=40><img src=\"" + template.getIcon() + "\" width=32 height=32></td><td width=190>" + template.getName() + "</td><td><button value=\" \" action=\"bypass voice .playpotion " + page + " " + template.getId() + "\" width=32 height=32 back=\"L2UI_CH3.mapbutton_zoomout2\" fore=\"L2UI_CH3.mapbutton_zoomout1\"></td>");
						}
						else
						{
							sb.append("<td height=40 width=40><img src=\"" + template.getIcon() + "\" width=32 height=32></td><td width=190><font color=\"B09878\">" + template.getName() + "</font></td><td><button value=\" \" action=\"bypass voice .playpotion " + page + " " + template.getId() + "\" width=32 height=32 back=\"L2UI_CH3.mapbutton_zoomin2\" fore=\"L2UI_CH3.mapbutton_zoomin1\"></td>");
						}
						sb.append("</tr></table><img src=\"L2UI.SquareGray\" width=295 height=1>");
					}
					
					// Generate page footer.
					sb.append("<br><img src=\"L2UI.SquareGray\" width=295 height=1><table width=\"100%\" bgcolor=000000><tr>");
					if (page > 1)
					{
						sb.append("<td align=left width=70><a action=\"bypass voice .playpotion " + (page - 1) + "\"><font color=\"CDB67F\">Previous</font></a></td>");
					}
					else
					{
						sb.append("<td align=left width=70><font color=\"B09878\">Previous</font></td>");
					}
					sb.append("<td align=center width=100>Page " + page + " of " + max + "</td>");
					if (page < max)
					{
						sb.append("<td align=right width=70><a action=\"bypass voice .playpotion " + (page + 1) + "\"><font color=\"CDB67F\">Next</font></a></td>");
					}
					else
					{
						sb.append("<td align=right width=70><font color=\"B09878\">Next</font></td>");
					}
					sb.append("</tr></table><img src=\"L2UI.SquareGray\" width=295 height=1>");
				}
				
				// Replace and send the html.
				html.setHtml(content.replace("%items%", sb.toString()));
				player.sendPacket(html);
				break;
			}
		}
		
		return true;
	}
	
	@Override
	public String[] getCommandList()
	{
		return VOICED_COMMANDS;
	}
}
