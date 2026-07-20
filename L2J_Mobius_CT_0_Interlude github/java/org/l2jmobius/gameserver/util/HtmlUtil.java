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
package org.l2jmobius.gameserver.util;

import java.util.Arrays;
import java.util.Locale;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.config.DevelopmentConfig;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.enums.HtmlActionScope;
import org.l2jmobius.gameserver.network.serverpackets.AbstractHtmlPacket;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.ShowBoard;

/**
 * A class containing useful methods for constructing HTML
 * @author NosBit, Mobius
 */
public class HtmlUtil
{
	private static final Logger LOGGER = Logger.getLogger(HtmlUtil.class.getName());
	
	/**
	 * Generates an HTML representation of the CP (Combat Points) gauge. The gauge visually represents the current CP value relative to its maximum, displayed as either a percentage or a fraction.
	 * @param width the width of the gauge, which determines its horizontal size
	 * @param current the current CP value to display on the gauge
	 * @param max the maximum CP value, used to calculate the fill ratio
	 * @param displayAsPercentage if {@code true}, the gauge's text will show the CP as a percentage; if {@code false}, it will display the text as "current / max"
	 * @return a string containing the HTML representation of the CP gauge, ready for rendering in the user interface
	 */
	public static String getCpGauge(int width, long current, long max, boolean displayAsPercentage)
	{
		return getGauge(width, current, max, displayAsPercentage, "L2UI_CH3.br_bar1_cp", "L2UI_CH3.br_bar1_cp", 16, -13);
	}
	
	/**
	 * Generates an HTML representation of the HP (Health Points) gauge. This gauge visually represents the current HP value in relation to its maximum capacity, with an option to display the value as a percentage or as a fraction.
	 * @param width the width of the gauge, controlling its horizontal size in the UI
	 * @param current the current HP value to be displayed on the gauge
	 * @param max the maximum HP value, used to calculate the fill level
	 * @param displayAsPercentage if {@code true}, displays the HP value as a percentage in the center of the gauge; if {@code false}, displays the HP as "current / max"
	 * @return a string containing the HTML representation of the HP gauge, formatted and ready for rendering within the user interface
	 */
	public static String getHpGauge(int width, long current, long max, boolean displayAsPercentage)
	{
		return getGauge(width, current, max, displayAsPercentage, "L2UI_CH3.br_bar1_hp", "L2UI_CH3.br_bar1_hp", 16, -13);
	}
	
	/**
	 * Generates an HTML representation of the MP (Mana Points) gauge. This gauge visually represents the current MP value relative to its maximum, with an option to display the value as either a percentage or a fraction.
	 * @param width the width of the gauge, defining its horizontal size in the UI
	 * @param current the current MP value to be displayed on the gauge
	 * @param max the maximum MP value, used to calculate the fill ratio
	 * @param displayAsPercentage if {@code true}, displays the MP value as a percentage in the center of the gauge; if {@code false}, displays the MP as "current / max"
	 * @return a string containing the HTML representation of the MP gauge, formatted and ready for rendering within the user interface
	 */
	public static String getMpGauge(int width, long current, long max, boolean displayAsPercentage)
	{
		return getGauge(width, current, max, displayAsPercentage, "L2UI_CH3.br_bar1_mp", "L2UI_CH3.br_bar1_mp", 16, -13);
	}
	
	/**
	 * Generates an HTML representation of a gauge with customizable images, size, and display format.<br>
	 * The gauge visually displays a current value relative to a maximum, either as a percentage or as "current / max."
	 * @param width the width of the gauge, defining its horizontal size in the UI
	 * @param currentValue the current value to display on the gauge
	 * @param max the maximum value, used to calculate the fill ratio
	 * @param displayAsPercentage if {@code true}, displays the value as a percentage in the center of the gauge; if {@code false}, displays the value as "current / max"
	 * @param backgroundImage the path of the background image for the gauge
	 * @param image the path of the foreground image for the gauge, representing the filled portion
	 * @param imageHeight the height of the gauge's image in pixels
	 * @param top vertical offset for the inner table to adjust alignment
	 * @return a string containing the HTML representation of the gauge, formatted and ready for rendering within the user interface
	 */
	private static String getGauge(int width, long currentValue, long max, boolean displayAsPercentage, String backgroundImage, String image, long imageHeight, long top)
	{
		final long current = Math.min(currentValue, max);
		final StringBuilder sb = new StringBuilder();
		sb.append("<table width=");
		sb.append(width);
		sb.append(" cellpadding=0 cellspacing=0>");
		sb.append("<tr>");
		sb.append("<td background=\"");
		sb.append(backgroundImage);
		sb.append("\">");
		sb.append("<img src=\"");
		sb.append(image);
		sb.append("\" width=");
		sb.append((long) (((double) current / max) * width));
		sb.append(" height=");
		sb.append(imageHeight);
		sb.append(">");
		sb.append("</td>");
		sb.append("</tr>");
		sb.append("<tr>");
		sb.append("<td align=center>");
		sb.append("<table cellpadding=0 cellspacing=");
		sb.append(top);
		sb.append(">");
		sb.append("<tr>");
		sb.append("<td>");
		if (displayAsPercentage)
		{
			sb.append("<table cellpadding=0 cellspacing=2>");
			sb.append("<tr><td>");
			sb.append(String.format("%.2f%%", ((double) current / max) * 100));
			sb.append("</td></tr>");
			sb.append("</table>");
		}
		else
		{
			final int tdWidth = (width - 10) / 2;
			sb.append("<table cellpadding=0 cellspacing=0>");
			sb.append("<tr>");
			sb.append("<td width=");
			sb.append(tdWidth);
			sb.append(" align=right>");
			sb.append(current);
			sb.append("</td>");
			sb.append("<td width=10 align=center>/</td>");
			sb.append("<td width=");
			sb.append(tdWidth);
			sb.append(">");
			sb.append(max);
			sb.append("</td>");
			sb.append("</tr>");
			sb.append("</table>");
		}
		sb.append("</td>");
		sb.append("</tr>");
		sb.append("</table>");
		sb.append("</td>");
		sb.append("</tr>");
		sb.append("</table>");
		return sb.toString();
	}
	
	/**
	 * Caches HTML bypass actions found in the provided HTML content, allowing specific player actions to be executed when the associated bypass links are clicked.
	 * @param player the player for whom the bypass actions are being cached
	 * @param scope the HTML action scope, which defines the lifetime and context of the cached actions
	 * @param html the HTML content to scan for bypass actions
	 */
	private static void buildHtmlBypassCache(Player player, HtmlActionScope scope, String html)
	{
		final String htmlLower = html.toLowerCase(Locale.ENGLISH);
		int bypassEnd = 0;
		int bypassStart = htmlLower.indexOf("=\"bypass ", bypassEnd);
		int bypassStartEnd;
		while (bypassStart != -1)
		{
			bypassStartEnd = bypassStart + 9;
			bypassEnd = htmlLower.indexOf("\"", bypassStartEnd);
			if (bypassEnd == -1)
			{
				break;
			}
			
			final int hParamPos = htmlLower.indexOf("-h ", bypassStartEnd);
			String bypass;
			if ((hParamPos != -1) && (hParamPos < bypassEnd))
			{
				bypass = html.substring(hParamPos + 3, bypassEnd).trim();
			}
			else
			{
				bypass = html.substring(bypassStartEnd, bypassEnd).trim();
			}
			
			final int firstParameterStart = bypass.indexOf(AbstractHtmlPacket.VAR_PARAM_START_CHAR);
			if (firstParameterStart != -1)
			{
				bypass = bypass.substring(0, firstParameterStart + 1);
			}
			
			if (DevelopmentConfig.HTML_ACTION_CACHE_DEBUG)
			{
				LOGGER.info("Cached html bypass(" + scope + "): '" + bypass + "'");
			}
			
			player.addHtmlAction(scope, bypass);
			bypassStart = htmlLower.indexOf("=\"bypass ", bypassEnd);
		}
	}
	
	/**
	 * Caches HTML link actions found in the provided HTML content, enabling specific player actions to be executed when the associated links are clicked. This method also validates the link paths to ensure security.
	 * @param player the player for whom the link actions are being cached
	 * @param scope the HTML action scope, which defines the lifetime and context of the cached actions
	 * @param html the HTML content to scan for link actions
	 */
	private static void buildHtmlLinkCache(Player player, HtmlActionScope scope, String html)
	{
		final String htmlLower = html.toLowerCase(Locale.ENGLISH);
		int linkEnd = 0;
		int linkStart = htmlLower.indexOf("=\"link ", linkEnd);
		int linkStartEnd;
		while (linkStart != -1)
		{
			linkStartEnd = linkStart + 7;
			linkEnd = htmlLower.indexOf("\"", linkStartEnd);
			if (linkEnd == -1)
			{
				break;
			}
			
			final String htmlLink = html.substring(linkStartEnd, linkEnd).trim();
			if (htmlLink.isEmpty())
			{
				LOGGER.warning("Html link path is empty!");
				continue;
			}
			
			if (htmlLink.contains(".."))
			{
				LOGGER.warning("Html link path is invalid: " + htmlLink);
				continue;
			}
			
			if (DevelopmentConfig.HTML_ACTION_CACHE_DEBUG)
			{
				LOGGER.info("Cached html link(" + scope + "): '" + htmlLink + "'");
			}
			
			// let's keep an action cache with "link " lowercase literal kept
			player.addHtmlAction(scope, "link " + htmlLink);
			linkStart = htmlLower.indexOf("=\"link ", linkEnd);
		}
	}
	
	/**
	 * Builds the HTML action cache for the specified scope. An {@code npcObjId} of 0 indicates that the cached actions can be activated without proximity to an NPC spawned in the world.
	 * @param player the player for whom the HTML action cache is being built
	 * @param scope the scope within which the HTML action cache is valid
	 * @param npcObjId the NPC object ID associated with the cached actions, where 0 indicates no specific NPC
	 * @param html the HTML content to parse for cacheable actions
	 */
	public static void buildHtmlActionCache(Player player, HtmlActionScope scope, int npcObjId, String html)
	{
		if ((player == null) || (scope == null) || (npcObjId < 0) || (html == null))
		{
			throw new IllegalArgumentException();
		}
		
		if (DevelopmentConfig.HTML_ACTION_CACHE_DEBUG)
		{
			LOGGER.info("Set html action npc(" + scope + "): " + npcObjId);
		}
		
		player.setHtmlActionOriginObjectId(scope, npcObjId);
		buildHtmlBypassCache(player, scope, html);
		buildHtmlLinkCache(player, scope, html);
	}
	
	/**
	 * Sends an NpcHtmlMessage to the specified player.<br>
	 * The HtmlActionCache is built with an NPC origin of 0, meaning that the HTML links are not bound to a specific NPC.
	 * @param player the player to whom the HTML content is sent
	 * @param html the HTML content to send
	 */
	public static void sendHtml(Player player, String html)
	{
		final NpcHtmlMessage message = new NpcHtmlMessage();
		message.setHtml(html);
		player.sendPacket(message);
	}
	
	/**
	 * Sends community board HTML content to the specified player.<br>
	 * The HtmlActionCache is built with an NPC origin of 0, meaning that the HTML links are not bound to a specific NPC.
	 * @param player the player to whom the HTML content is sent
	 * @param html the HTML content for the community board
	 */
	public static void sendCBHtml(Player player, String html)
	{
		sendCBHtml(player, html, 0);
	}
	
	/**
	 * Sends community board HTML content to the specified player.<br>
	 * If {@code npcObjId} is greater than -1, the HtmlActionCache is built with {@code npcObjId} as the origin.<br>
	 * An origin of 0 indicates that cached bypasses are not bound to a specific NPC.
	 * @param player the player to whom the HTML content is sent
	 * @param html the HTML content for the community board
	 * @param npcObjId the NPC object ID to associate with the bypass actions, where 0 indicates no specific NPC
	 */
	public static void sendCBHtml(Player player, String html, int npcObjId)
	{
		sendCBHtml(player, html, null, npcObjId);
	}
	
	/**
	 * Sends community board HTML content to the specified player and populates a multiedit field if {@code fillMultiEdit} is provided.<br>
	 * The HtmlActionCache is built with an NPC origin of 0, indicating that the HTML links are not bound to a specific NPC.
	 * @param player the player to whom the HTML content is sent
	 * @param html the HTML content for the community board
	 * @param fillMultiEdit the text to populate in the multiedit field; may be {@code null}
	 */
	public static void sendCBHtml(Player player, String html, String fillMultiEdit)
	{
		sendCBHtml(player, html, fillMultiEdit, 0);
	}
	
	/**
	 * Sends community board HTML content to the specified player and fills a multiedit field if {@code fillMultiEdit} is provided.<br>
	 * If {@code npcObjId} is greater than -1, the HtmlActionCache is built with {@code npcObjId} as the origin.<br>
	 * An origin of 0 indicates that cached bypasses are not bound to a specific NPC.
	 * @param player the player to whom the HTML content is sent
	 * @param html the HTML content for the community board
	 * @param fillMultiEdit the text to populate in the multiedit field; may be {@code null}
	 * @param npcObjId the NPC object ID to associate with the bypass actions, where 0 indicates no specific NPC
	 */
	public static void sendCBHtml(Player player, String html, String fillMultiEdit, int npcObjId)
	{
		if ((player == null) || (html == null))
		{
			return;
		}
		
		player.clearHtmlActions(HtmlActionScope.COMM_BOARD_HTML);
		
		if (npcObjId > -1)
		{
			buildHtmlActionCache(player, HtmlActionScope.COMM_BOARD_HTML, npcObjId, html);
		}
		
		if (fillMultiEdit != null)
		{
			player.sendPacket(new ShowBoard(html, "1001"));
			fillMultiEditContent(player, fillMultiEdit);
		}
		else if (html.length() < 4090)
		{
			player.sendPacket(new ShowBoard(html, "101"));
			player.sendPacket(new ShowBoard(null, "102"));
			player.sendPacket(new ShowBoard(null, "103"));
		}
		else if (html.length() < 8180)
		{
			player.sendPacket(new ShowBoard(html.substring(0, 4090), "101"));
			player.sendPacket(new ShowBoard(html.substring(4090), "102"));
			player.sendPacket(new ShowBoard(null, "103"));
		}
		else if (html.length() < 12270)
		{
			player.sendPacket(new ShowBoard(html.substring(0, 4090), "101"));
			player.sendPacket(new ShowBoard(html.substring(4090, 8180), "102"));
			player.sendPacket(new ShowBoard(html.substring(8180), "103"));
		}
		else
		{
			player.sendPacket(new ShowBoard("<html><body><br><center>Error: HTML was too long!</center></body></html>", "101"));
			player.sendPacket(new ShowBoard(null, "102"));
			player.sendPacket(new ShowBoard(null, "103"));
		}
	}
	
	/**
	 * Fills the community board's multiedit window with specified text.<br>
	 * This method should be called after {@code sendCBHtml} to ensure the multiedit field is populated.
	 * @param player the player to whom the multiedit content is sent
	 * @param text the text to populate in the multiedit field
	 */
	public static void fillMultiEditContent(Player player, String text)
	{
		player.sendPacket(new ShowBoard(Arrays.asList("0", "0", "0", "0", "0", "0", player.getName(), Integer.toString(player.getObjectId()), player.getAccountName(), "9", " ", " ", text.replace("<br>", System.lineSeparator()), "0", "0", "0", "0")));
	}
	
	/**
	 * Calculates the number of pages required to display all items with a specified page size.
	 * @param totalItems the total number of items
	 * @param itemsPerPage the number of items per page
	 * @return the number of pages required
	 * @throws IllegalArgumentException if {@code itemsPerPage} is zero or negative
	 */
	public static int countPageNumber(int totalItems, int itemsPerPage)
	{
		if (itemsPerPage <= 0)
		{
			return 0;
		}
		
		return ((totalItems + itemsPerPage) - 1) / itemsPerPage;
	}
}
