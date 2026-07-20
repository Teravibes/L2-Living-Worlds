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
package org.l2jmobius.gameserver.managers;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.config.custom.SellBuffsConfig;
import org.l2jmobius.gameserver.data.holders.SellBuffHolder;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PrivateStoreType;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.olympiad.Olympiad;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.serverpackets.PrivateStoreMsgSell;
import org.l2jmobius.gameserver.util.FormatUtil;
import org.l2jmobius.gameserver.util.HtmlUtil;

/**
 * @author St3eT
 */
public class SellBuffsManager implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(SellBuffsManager.class.getName());
	
	private static final Set<Integer> ALLOWED_BUFFS = new HashSet<>();
	private static final String HTML_FOLDER = "data/html/mods/SellBuffs/";
	
	protected SellBuffsManager()
	{
		load();
	}
	
	@Override
	public void load()
	{
		if (SellBuffsConfig.SELLBUFF_ENABLED)
		{
			ALLOWED_BUFFS.clear();
			parseDatapackFile("data/SellBuffData.xml");
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + ALLOWED_BUFFS.size() + " allowed buffs.");
		}
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		final NodeList node = document.getDocumentElement().getElementsByTagName("skill");
		for (int i = 0; i < node.getLength(); ++i)
		{
			final Element elem = (Element) node.item(i);
			final int skillId = Integer.parseInt(elem.getAttribute("id"));
			ALLOWED_BUFFS.add(skillId);
		}
	}
	
	/**
	 * Sends the sell menu HTML to the specified player.
	 * <p>
	 * This displays either a menu for starting to sell buffs or a message indicating that the player is already selling buffs.
	 * </p>
	 * @param player the {@link Player} to display the menu to
	 */
	public void sendSellMenu(Player player)
	{
		final String html = HtmCache.getInstance().getHtm(player, HTML_FOLDER + (player.isSellingBuffs() ? "BuffMenu_already.html" : "BuffMenu.html"));
		CommunityBoardHandler.separateAndSend(html, player);
	}
	
	/**
	 * Sends the buff choice menu to the specified player.
	 * <p>
	 * The menu displays a list of available buffs that the player can choose to sell.
	 * </p>
	 * @param player the {@link Player} to display the menu to
	 * @param index the starting index for paginated buff options
	 */
	public void sendBuffChoiceMenu(Player player, int index)
	{
		String html = HtmCache.getInstance().getHtm(player, HTML_FOLDER + "BuffChoice.html");
		html = html.replace("%list%", buildSkillMenu(player, index));
		CommunityBoardHandler.separateAndSend(html, player);
	}
	
	/**
	 * Sends the buff edit menu to the specified player, allowing them to modify prices or remove buffs from their sell list.
	 * @param player the {@link Player} to display the edit menu to
	 */
	public void sendBuffEditMenu(Player player)
	{
		String html = HtmCache.getInstance().getHtm(player, HTML_FOLDER + "BuffChoice.html");
		html = html.replace("%list%", buildEditMenu(player));
		CommunityBoardHandler.separateAndSend(html, player);
	}
	
	/**
	 * Sends the buff purchase menu to a player, showing the buffs available for purchase from a seller.
	 * @param player the {@link Player} to display the menu to
	 * @param seller the {@link Player} selling the buffs
	 * @param index the starting index for paginated buff options
	 */
	public void sendBuffMenu(Player player, Player seller, int index)
	{
		if (!seller.isSellingBuffs() || seller.getSellingBuffs().isEmpty())
		{
			return;
		}
		
		String html = HtmCache.getInstance().getHtm(player, HTML_FOLDER + "BuffBuyMenu.html");
		html = html.replace("%list%", buildBuffMenu(seller, index));
		CommunityBoardHandler.separateAndSend(html, player);
	}
	
	/**
	 * Initiates the buff selling process for the specified player.
	 * <p>
	 * The player is set up to sell buffs, with a private store type and title, and a sell menu is displayed.
	 * </p>
	 * @param player the {@link Player} who wants to start selling buffs
	 * @param title the title of the buff store
	 */
	public void startSellBuffs(Player player, String title)
	{
		player.sitDown();
		player.setSellingBuffs(true);
		player.setPrivateStoreType(PrivateStoreType.PACKAGE_SELL);
		player.getSellList().setTitle(title);
		player.getSellList().setPackaged(true);
		player.broadcastUserInfo();
		
		player.broadcastPacket(new PrivateStoreMsgSell(player));
		player.sendPacket(new PrivateStoreMsgSell(player));
		
		sendSellMenu(player);
	}
	
	/**
	 * Stops the buff selling process for the specified player and resets the store type.
	 * @param player the {@link Player} who wants to stop selling buffs
	 */
	public void stopSellBuffs(Player player)
	{
		player.setSellingBuffs(false);
		player.setPrivateStoreType(PrivateStoreType.NONE);
		player.standUp();
		player.broadcastUserInfo();
		sendSellMenu(player);
	}
	
	/**
	 * Builds an HTML-formatted menu listing available buffs from the specified seller.
	 * @param seller the {@link Player} selling the buffs
	 * @param index the starting index for paginated buff options
	 * @return a string of HTML representing the buff menu
	 */
	private String buildBuffMenu(Player seller, int index)
	{
		final int ceiling = 9;
		int nextIndex = -1;
		int previousIndex = -1;
		int emptyFields = 0;
		final StringBuilder sb = new StringBuilder();
		final List<SellBuffHolder> sellList = new ArrayList<>();
		
		int count = 0;
		for (SellBuffHolder holder : seller.getSellingBuffs())
		{
			count++;
			if ((count > index) && (count <= (ceiling + index)))
			{
				sellList.add(holder);
			}
		}
		
		if ((count > 9) && (count > (index + 9)))
		{
			nextIndex = index + 9;
		}
		
		if (index >= 9)
		{
			previousIndex = index - 9;
		}
		
		emptyFields = ceiling - sellList.size();
		
		sb.append("<br>");
		sb.append(HtmlUtil.getMpGauge(250, (long) seller.getCurrentMp(), seller.getMaxMp(), false));
		sb.append("<br>");
		
		sb.append("<table>");
		sb.append("<tr><td><br></td></tr>");
		sb.append("<tr>");
		sb.append("<td fixwidth=\"20\"></td>");
		sb.append("<td> <button action=\"\" value=\"Icon\" width=70 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"> </td>"); // Icon
		sb.append("<td> <button action=\"\" value=\"Name\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"> </td>"); // Name
		sb.append("<td> <button action=\"\" value=\"Level\" width=70 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"> </td>"); // Leve
		sb.append("<td> <button action=\"\" value=\"MP Cost\" width=70 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"> </td>"); // Price
		sb.append("<td> <button action=\"\" value=\"Price\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"> </td>"); // Price
		sb.append("<td> <button action=\"\" value=\"Player\" width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"> </td>"); // Player
		sb.append("<td> <button action=\"\" value=\"Pet\" width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"> </td>"); // Pet
		sb.append("<td fixwidth=\"20\"></td>");
		sb.append("<tr><td><br></td></tr>");
		sb.append("</tr>");
		
		for (SellBuffHolder holder : sellList)
		{
			final Skill skill = seller.getKnownSkill(holder.getSkillId());
			if (skill == null)
			{
				emptyFields++;
				continue;
			}
			
			final ItemTemplate item = ItemData.getInstance().getTemplate(SellBuffsConfig.SELLBUFF_PAYMENT_ID);
			
			sb.append("<tr>");
			sb.append("<td fixwidth=\"20\"></td>");
			sb.append("<td align=center><img src=\"" + skill.getIcon() + "\" width=\"32\" height=\"32\"></td>");
			sb.append("<td align=center>" + skill.getName() + (skill.getLevel() > 100 ? "<font color=\"LEVEL\"> + " + (skill.getLevel() % 100) + "</font></td>" : "</td>"));
			sb.append("<td align=center>" + ((skill.getLevel() > 100) ? SkillData.getInstance().getMaxLevel(skill.getId()) : skill.getLevel()) + "</td>");
			sb.append("<td align=center> <font color=\"1E90FF\">" + (skill.getMpConsume() * SellBuffsConfig.SELLBUFF_MP_MULTIPLER) + "</font></td>");
			sb.append("<td align=center> " + FormatUtil.formatAdena(holder.getPrice()) + " <font color=\"LEVEL\"> " + (item != null ? item.getName() : "") + "</font> </td>");
			sb.append("<td align=right fixwidth=\"40\"><button value=\"Buy Buff\" action=\"bypass sellbuffbuyskill " + seller.getObjectId() + " " + skill.getId() + " " + index + "\" width=\"75\" height=\"21\" back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
			sb.append("<td align=right fixwidth=\"40\"><button value=\"Buy Buff\" action=\"bypass sellbuffbuyskillPet " + seller.getObjectId() + " " + skill.getId() + " " + index + "\" width=\"75\" height=\"21\" back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
			sb.append("</tr>");
			sb.append("<tr><td><br></td></tr>");
		}
		
		for (int i = 0; i < emptyFields; i++)
		{
			sb.append("<tr>");
			sb.append("<td fixwidth=\"20\" height=\"32\"></td>");
			sb.append("<td align=center></td>");
			sb.append("<td align=left></td>");
			sb.append("<td align=center></td>");
			sb.append("<td align=center></font></td>");
			sb.append("<td align=center></td>");
			sb.append("<td align=center fixwidth=\"50\"></td>");
			sb.append("</tr>");
			sb.append("<tr><td><br></td></tr>");
		}
		
		sb.append("</table>");
		
		sb.append("<table width=\"250\" border=\"0\">");
		sb.append("<tr>");
		
		if (previousIndex > -1)
		{
			sb.append("<td align=left><button value=\"Previous Page\" action=\"bypass sellbuffbuymenu " + seller.getObjectId() + " " + previousIndex + "\" width=\"95\" height=\"21\" back=\"bigbutton_over\" fore=\"bigbutton\"></td>");
		}
		
		if (nextIndex > -1)
		{
			sb.append("<td align=right><button value=\"Next Page\" action=\"bypass sellbuffbuymenu " + seller.getObjectId() + " " + nextIndex + "\" width=\"95\" height=\"21\" back=\"bigbutton_over\" fore=\"bigbutton\"></td>");
		}
		sb.append("</tr>");
		sb.append("</table>");
		return sb.toString();
	}
	
	/**
	 * Builds an HTML-formatted menu for editing buffs the player has listed for sale.
	 * @param player the {@link Player} editing their buff list
	 * @return a string of HTML representing the edit menu
	 */
	private String buildEditMenu(Player player)
	{
		final StringBuilder sb = new StringBuilder();
		
		sb.append("<table>");
		sb.append("<tr>");
		sb.append("<td> <button action=\"\" value=\"Icon\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"> </td>"); // Icon
		sb.append("<td> <button action=\"\" value=\"Name\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"> </td>"); // Name
		sb.append("<td> <button action=\"\" value=\"Level\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"> </td>"); // Level
		sb.append("<td> <button action=\"\" value=\"Old Price\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"> </td>"); // Old price
		sb.append("<td> <button action=\"\" value=\"New Price\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"> </td>"); // New price
		sb.append("<td> <button action=\"\" value=\"Action\" width=70 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"> </td>"); // Change Price
		sb.append("<td> <button action=\"\" value=\"Remove\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"> </td>"); // Remove Buff
		sb.append("</tr>");
		
		if (player.getSellingBuffs().isEmpty())
		{
			sb.append("</table>");
			sb.append("<br><br><br>");
			sb.append("You don't have added any buffs yet!");
		}
		else
		{
			for (SellBuffHolder holder : player.getSellingBuffs())
			{
				final Skill skill = player.getKnownSkill(holder.getSkillId());
				if (skill == null)
				{
					continue;
				}
				
				sb.append("<tr>");
				sb.append("<td align=center><img src=\"" + skill.getIcon() + "\" width=\"32\" height=\"32\"></td>"); // Icon
				sb.append("<td align=center>" + skill.getName() + (skill.getLevel() > 100 ? "<font color=\"LEVEL\"> + " + (skill.getLevel() % 100) + "</font></td>" : "</td>")); // Name + enchant
				sb.append("<td align=center>" + ((skill.getLevel() > 100) ? SkillData.getInstance().getMaxLevel(skill.getId()) : skill.getLevel()) + "</td>"); // Level
				sb.append("<td align=center> " + FormatUtil.formatAdena(holder.getPrice()) + " </td>"); // Price show
				sb.append("<td align=center><edit var=\"price_" + skill.getId() + "\" width=100 type=\"number\"></td>"); // Price edit
				sb.append("<td align=center><button value=\"Edit\" action=\"bypass sellbuffchangeprice " + skill.getId() + " $price_" + skill.getId() + "\" width=\"70\" height=\"20\" back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
				sb.append("<td align=center><button value=\" X \" action=\"bypass sellbuffremove " + skill.getId() + "\" width=\"40\" height=\"20\" back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
				sb.append("</tr>");
			}
			sb.append("</table>");
		}
		
		return sb.toString();
	}
	
	/**
	 * Builds an HTML-formatted menu listing available skills the player can add to their buff sell list.
	 * @param player the {@link Player} adding skills to their sell list
	 * @param index the starting index for paginated skill options
	 * @return a string of HTML representing the skill menu
	 */
	private String buildSkillMenu(Player player, int index)
	{
		final int ceiling = index + 9;
		int nextIndex = -1;
		int previousIndex = -1;
		final StringBuilder sb = new StringBuilder();
		final List<Skill> skillList = new ArrayList<>();
		
		int count = 0;
		for (Skill skill : player.getAllSkills())
		{
			if (ALLOWED_BUFFS.contains(skill.getId()) && !isInSellList(player, skill))
			{
				count++;
				
				if ((count > index) && (count <= ceiling))
				{
					skillList.add(skill);
				}
			}
		}
		
		if ((count > 9) && (count > (index + 9)))
		{
			nextIndex = index + 9;
		}
		
		if (index >= 9)
		{
			previousIndex = index - 9;
		}
		
		sb.append("<table>");
		sb.append("<tr><td><br></td></tr>");
		sb.append("<tr>");
		sb.append("<td fixwidth=\"20\"></td>");
		sb.append("<td> <button action=\"\" value=\"Icon\" width=80 height=23 back=\"sek.cbui94\" fore=\"sek.cbui92\"> </td>"); // Icon
		sb.append("<td> <button action=\"\" value=\"Name\" width=100 height=23 back=\"sek.cbui94\" fore=\"sek.cbui92\"> </td>"); // Name
		sb.append("<td> <button action=\"\" value=\"Level\" width=70 height=23 back=\"sek.cbui94\" fore=\"sek.cbui92\"> </td>"); // Leve
		sb.append("<td> <button action=\"\" value=\"Price\" width=100 height=23 back=\"sek.cbui94\" fore=\"sek.cbui92\"> </td>"); // Price
		sb.append("<td> <button action=\"\" value=\"Action\" width=110 height=23 back=\"sek.cbui94\" fore=\"sek.cbui92\"> </td>"); // Action
		sb.append("<td fixwidth=\"40\"></td>");
		sb.append("</tr>");
		
		if (skillList.isEmpty())
		{
			sb.append("</table>");
			sb.append("<br><br><br>");
			sb.append("At this moment you cannot add any buffs!");
		}
		else
		{
			for (Skill skill : skillList)
			{
				sb.append("<tr>");
				sb.append("<td fixwidth=\"20\"></td>");
				sb.append("<td align=center><img src=\"" + skill.getIcon() + "\" width=\"32\" height=\"32\"></td>");
				sb.append("<td align=center>" + skill.getName() + (skill.getLevel() > 100 ? "<font color=\"LEVEL\"> + " + (skill.getLevel() % 100) + "</font></td>" : "</td>"));
				sb.append("<td align=center>" + ((skill.getLevel() > 100) ? SkillData.getInstance().getMaxLevel(skill.getId()) : skill.getLevel()) + "</td>");
				sb.append("<td align=left><edit var=\"price_" + skill.getId() + "\" width=100 type=\"number\"></td>");
				sb.append("<td align=center><button value=\" Add Buff \" action=\"bypass sellbuffaddskill " + skill.getId() + " $price_" + skill.getId() + "\" width=\"110\" height=\"20\" back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
				sb.append("</tr>");
				sb.append("<tr><td><br></td></tr>");
			}
			sb.append("</table>");
		}
		
		sb.append("<table width=\"250\" border=\"0\">");
		sb.append("<tr>");
		
		if (previousIndex > -1)
		{
			sb.append("<td align=left><button value=\"Previous Page\" action=\"bypass sellbuffadd " + previousIndex + "\"  width=\"95\" height=\"21\" back=\"bigbutton_over\" fore=\"bigbutton\"></td>");
		}
		
		if (nextIndex > -1)
		{
			sb.append("<td align=right><button value=\"Next Page\" action=\"bypass sellbuffadd " + nextIndex + "\" width=\"95\" height=\"21\" back=\"bigbutton_over\" fore=\"bigbutton\"></td>");
		}
		sb.append("</tr>");
		sb.append("</table>");
		return sb.toString();
	}
	
	/**
	 * Checks if a specific skill is already in the player's buff sell list.
	 * @param player the {@link Player} to check
	 * @param skill the {@link Skill} to verify
	 * @return {@code true} if the skill is in the player's sell list, {@code false} otherwise
	 */
	public boolean isInSellList(Player player, Skill skill)
	{
		for (SellBuffHolder holder : player.getSellingBuffs())
		{
			if (holder.getSkillId() == skill.getId())
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Determines if the player can start selling buffs based on various conditions.
	 * <p>
	 * Checks for restrictions such as being in an event, in a chaotic state, mounted, or in a restricted zone.
	 * </p>
	 * @param player the {@link Player} attempting to sell buffs
	 * @return {@code true} if the player meets all conditions to start selling buffs, {@code false} otherwise
	 */
	public boolean canStartSellBuffs(Player player)
	{
		if (player.isAlikeDead())
		{
			player.sendMessage("You can't sell buffs in fake death!");
			return false;
		}
		else if (player.isInOlympiadMode() || Olympiad.getInstance().isRegistered(player))
		{
			player.sendMessage("You can't sell buffs with Olympiad status!");
			return false;
		}
		else if (player.isRegisteredOnEvent())
		{
			player.sendMessage("You can't sell buffs while registered in an event!");
			return false;
		}
		else if (player.isCursedWeaponEquipped() || (player.getKarma() > 0))
		{
			player.sendMessage("You can't sell buffs in Chaotic state!");
			return false;
		}
		else if (player.isInDuel())
		{
			player.sendMessage("You can't sell buffs in Duel state!");
			return false;
		}
		else if (player.isFishing())
		{
			player.sendMessage("You can't sell buffs while fishing.");
			return false;
		}
		else if (player.isMounted() || player.isFlying())
		{
			player.sendMessage("You can't sell buffs in Mount state!");
			return false;
		}
		else if (player.isInsideZone(ZoneId.NO_STORE) || !player.isInsideZone(ZoneId.PEACE) || player.isJailed())
		{
			player.sendMessage("You can't sell buffs here!");
			return false;
		}
		
		return true;
	}
	
	public static SellBuffsManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final SellBuffsManager INSTANCE = new SellBuffsManager();
	}
}
