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
package org.l2jmobius.gameserver.model.actor.instance;

import java.util.StringTokenizer;
import java.util.logging.Level;

import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.sevensigns.SevenSigns;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Dawn/Dusk Seven Signs Priest Instance
 * @author Tempy
 */
public class SignsPriest extends Npc
{
	public SignsPriest(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.SignsPriest);
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if ((player.getLastFolkNPC() == null) || (player.getLastFolkNPC().getObjectId() != getObjectId()))
		{
			return;
		}
		
		if (command.startsWith("SevenSignsDesc"))
		{
			final int val = Integer.parseInt(command.substring(15));
			showChatWindow(player, val, null, true);
		}
		else if (command.startsWith("SevenSignsSolo"))
		{
			handleSoloControl(player, command);
		}
		else if (command.startsWith("SevenSigns"))
		{
			SystemMessage sm;
			String path;
			int cabal = SevenSigns.CABAL_NULL;
			int stoneType = 0;
			
			final long ancientAdenaAmount = player.getAncientAdena();
			int val = Integer.parseInt(command.substring(11, 12).trim());
			if (command.length() > 12)
			{
				val = Integer.parseInt(command.substring(11, 13).trim());
			}
			
			if (command.length() > 13)
			{
				try
				{
					cabal = Integer.parseInt(command.substring(14, 15).trim());
				}
				catch (Exception e)
				{
					try
					{
						cabal = Integer.parseInt(command.substring(13, 14).trim());
					}
					catch (Exception e2)
					{
						try
						{
							final StringTokenizer st = new StringTokenizer(command.trim());
							st.nextToken();
							cabal = Integer.parseInt(st.nextToken());
						}
						catch (Exception e3)
						{
							LOGGER.warning("Failed to retrieve cabal from bypass command. NpcId: " + getId() + "; Command: " + command);
						}
					}
				}
			}
			
			switch (val)
			{
				case 2: // Purchase Record of the Seven Signs
				{
					if (!player.getInventory().validateCapacity(1))
					{
						player.sendPacket(SystemMessageId.YOUR_INVENTORY_IS_FULL);
						break;
					}
					
					if (!player.reduceAdena(ItemProcessType.FEE, SevenSigns.RECORD_SEVEN_SIGNS_COST, this, true))
					{
						player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
						break;
					}
					
					player.getInventory().addItem(ItemProcessType.REWARD, SevenSigns.RECORD_SEVEN_SIGNS_ID, 1, player, this);
					sm = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S1);
					sm.addItemName(SevenSigns.RECORD_SEVEN_SIGNS_ID);
					player.sendPacket(sm);
					
					if (this instanceof DawnPriest)
					{
						showChatWindow(player, val, "dawn", false);
					}
					else
					{
						showChatWindow(player, val, "dusk", false);
					}
					break;
				}
				case 33: // "I want to participate" request
				{
					final int oldCabal = SevenSigns.getInstance().getPlayerCabal(player.getObjectId());
					if (oldCabal != SevenSigns.CABAL_NULL)
					{
						if (this instanceof DawnPriest)
						{
							showChatWindow(player, val, "dawn_member", false);
						}
						else
						{
							showChatWindow(player, val, "dusk_member", false);
						}
						return;
					}
					else if (player.getPlayerClass().level() == 0)
					{
						if (this instanceof DawnPriest)
						{
							showChatWindow(player, val, "dawn_firstclass", false);
						}
						else
						{
							showChatWindow(player, val, "dusk_firstclass", false);
						}
						return;
					}
					else if ((cabal == SevenSigns.CABAL_DUSK) && FeatureConfig.ALT_GAME_CASTLE_DUSK) // dusk
					{
						// castle owners cannot participate with dusk side
						final Clan clan = player.getClan();
						if ((clan != null) && (clan.getCastleId() > 0))
						{
							showChatWindow(player, SevenSigns.SEVEN_SIGNS_HTML_PATH + "signs_33_dusk_no.htm");
							break;
						}
					}
					else if ((cabal == SevenSigns.CABAL_DAWN) && FeatureConfig.ALT_GAME_CASTLE_DAWN) // dawn
					{
						// clans without castle need to pay participation fee
						final Clan clan = player.getClan();
						if ((clan == null) || (clan.getCastleId() == 0))
						{
							showChatWindow(player, SevenSigns.SEVEN_SIGNS_HTML_PATH + "signs_33_dawn_fee.htm");
							break;
						}
					}
					
					if (this instanceof DawnPriest)
					{
						showChatWindow(player, val, "dawn", false);
					}
					else
					{
						showChatWindow(player, val, "dusk", false);
					}
					break;
				}
				case 34: // Pay the participation fee request
				{
					if ((player.getPlayerClass().level() > 0) && ((player.getAdena() >= FeatureConfig.SSQ_JOIN_DAWN_ADENA_FEE) || (player.getInventory().getInventoryItemCount(FeatureConfig.SSQ_MANORS_AGREEMENT_ID, -1) > 0)))
					{
						showChatWindow(player, SevenSigns.SEVEN_SIGNS_HTML_PATH + "signs_33_dawn.htm");
					}
					else
					{
						showChatWindow(player, SevenSigns.SEVEN_SIGNS_HTML_PATH + "signs_33_dawn_no.htm");
					}
					break;
				}
				case 3: // Join Cabal Intro 1
				case 8: // Festival of Darkness Intro - SevenSigns x [0]1
				{
					showChatWindow(player, val, SevenSigns.getCabalShortName(cabal), false);
					break;
				}
				case 4: // Join a Cabal - SevenSigns 4 [0]1 x
				{
					final int newSeal = Integer.parseInt(command.substring(15));
					if (player.getPlayerClass().level() >= 1)
					{
						// even if in htmls is said that ally can have castle too, but it is not
						final Clan clan = player.getClan();
						if ((cabal == SevenSigns.CABAL_DUSK) && FeatureConfig.ALT_GAME_CASTLE_DUSK && (clan != null) && (clan.getCastleId() > 0))
						{
							showChatWindow(player, SevenSigns.SEVEN_SIGNS_HTML_PATH + "signs_33_dusk_no.htm");
							return;
						}
						
						// If the player is trying to join the Lords of Dawn, check if they are carrying a Lord's certificate. If not then try to take the required amount of adena instead.
						if (FeatureConfig.ALT_GAME_CASTLE_DAWN && (cabal == SevenSigns.CABAL_DAWN))
						{
							boolean allowJoinDawn = false;
							if ((clan != null) && (clan.getCastleId() > 0))
							{
								allowJoinDawn = true;
							}
							else if (player.destroyItemByItemId(ItemProcessType.FEE, FeatureConfig.SSQ_MANORS_AGREEMENT_ID, 1, this, true))
							{
								allowJoinDawn = true;
							}
							else if (player.reduceAdena(ItemProcessType.FEE, FeatureConfig.SSQ_JOIN_DAWN_ADENA_FEE, this, true))
							{
								allowJoinDawn = true;
							}
							
							if (!allowJoinDawn)
							{
								showChatWindow(player, SevenSigns.SEVEN_SIGNS_HTML_PATH + "signs_33_dawn_fee.htm");
								return;
							}
						}
					}
					
					SevenSigns.getInstance().setPlayerInfo(player.getObjectId(), cabal, newSeal);
					if (cabal == SevenSigns.CABAL_DAWN)
					{
						player.sendPacket(SystemMessageId.YOU_WILL_PARTICIPATE_IN_THE_SEVEN_SIGNS_AS_A_MEMBER_OF_THE_LORDS_OF_DAWN); // Joined Dawn
					}
					else
					{
						player.sendPacket(SystemMessageId.YOU_WILL_PARTICIPATE_IN_THE_SEVEN_SIGNS_AS_A_MEMBER_OF_THE_REVOLUTIONARIES_OF_DUSK); // Joined Dusk
					}
					
					// Show a confirmation message to the user, indicating which seal they chose.
					switch (newSeal)
					{
						case SevenSigns.SEAL_AVARICE:
						{
							player.sendPacket(SystemMessageId.YOU_VE_CHOSEN_TO_FIGHT_FOR_THE_SEAL_OF_AVARICE_DURING_THIS_QUEST_EVENT_PERIOD);
							break;
						}
						case SevenSigns.SEAL_GNOSIS:
						{
							player.sendPacket(SystemMessageId.YOU_VE_CHOSEN_TO_FIGHT_FOR_THE_SEAL_OF_GNOSIS_DURING_THIS_QUEST_EVENT_PERIOD);
							break;
						}
						case SevenSigns.SEAL_STRIFE:
						{
							player.sendPacket(SystemMessageId.YOU_VE_CHOSEN_TO_FIGHT_FOR_THE_SEAL_OF_STRIFE_DURING_THIS_QUEST_EVENT_PERIOD);
							break;
						}
					}
					
					showChatWindow(player, 4, SevenSigns.getCabalShortName(cabal), false);
					break;
				}
				case 5:
				{
					if (this instanceof DawnPriest)
					{
						if (SevenSigns.getInstance().getPlayerCabal(player.getObjectId()) == SevenSigns.CABAL_NULL)
						{
							showChatWindow(player, val, "dawn_no", false);
						}
						else
						{
							showChatWindow(player, val, "dawn", false);
						}
					}
					else
					{
						if (SevenSigns.getInstance().getPlayerCabal(player.getObjectId()) == SevenSigns.CABAL_NULL)
						{
							showChatWindow(player, val, "dusk_no", false);
						}
						else
						{
							showChatWindow(player, val, "dusk", false);
						}
					}
					break;
				}
				case 21:
				{
					final int contribStoneId = Integer.parseInt(command.substring(14, 18));
					final Item contribBlueStones = player.getInventory().getItemByItemId(SevenSigns.SEAL_STONE_BLUE_ID);
					final Item contribGreenStones = player.getInventory().getItemByItemId(SevenSigns.SEAL_STONE_GREEN_ID);
					final Item contribRedStones = player.getInventory().getItemByItemId(SevenSigns.SEAL_STONE_RED_ID);
					final int contribBlueStoneCount = contribBlueStones == null ? 0 : contribBlueStones.getCount();
					final int contribGreenStoneCount = contribGreenStones == null ? 0 : contribGreenStones.getCount();
					final int contribRedStoneCount = contribRedStones == null ? 0 : contribRedStones.getCount();
					int score = SevenSigns.getInstance().getPlayerContribScore(player.getObjectId());
					int contributionCount = 0;
					boolean contribStonesFound = false;
					int redContrib = 0;
					int greenContrib = 0;
					int blueContrib = 0;
					
					try
					{
						contributionCount = Integer.parseInt(command.substring(19).trim());
					}
					catch (Exception nfe)
					{
						if (this instanceof DawnPriest)
						{
							showChatWindow(player, 6, "dawn_failure", false);
						}
						else
						{
							showChatWindow(player, 6, "dusk_failure", false);
						}
						break;
					}
					
					switch (contribStoneId)
					{
						case SevenSigns.SEAL_STONE_BLUE_ID:
						{
							blueContrib = (FeatureConfig.ALT_MAXIMUM_PLAYER_CONTRIB - score) / SevenSigns.BLUE_CONTRIB_POINTS;
							if (blueContrib > contribBlueStoneCount)
							{
								blueContrib = contributionCount;
							}
							break;
						}
						case SevenSigns.SEAL_STONE_GREEN_ID:
						{
							greenContrib = (FeatureConfig.ALT_MAXIMUM_PLAYER_CONTRIB - score) / SevenSigns.GREEN_CONTRIB_POINTS;
							if (greenContrib > contribGreenStoneCount)
							{
								greenContrib = contributionCount;
							}
							break;
						}
						case SevenSigns.SEAL_STONE_RED_ID:
						{
							redContrib = (FeatureConfig.ALT_MAXIMUM_PLAYER_CONTRIB - score) / SevenSigns.RED_CONTRIB_POINTS;
							if (redContrib > contribRedStoneCount)
							{
								redContrib = contributionCount;
							}
							break;
						}
					}
					
					if ((redContrib > 0) && player.destroyItemByItemId(ItemProcessType.FEE, SevenSigns.SEAL_STONE_RED_ID, redContrib, this, false))
					{
						contribStonesFound = true;
						final SystemMessage msg = new SystemMessage(SystemMessageId.S2_S1_HAS_DISAPPEARED);
						msg.addItemName(SevenSigns.SEAL_STONE_RED_ID);
						msg.addInt(redContrib);
						player.sendPacket(msg);
					}
					
					if ((greenContrib > 0) && player.destroyItemByItemId(ItemProcessType.FEE, SevenSigns.SEAL_STONE_GREEN_ID, greenContrib, this, false))
					{
						contribStonesFound = true;
						final SystemMessage msg = new SystemMessage(SystemMessageId.S2_S1_HAS_DISAPPEARED);
						msg.addItemName(SevenSigns.SEAL_STONE_GREEN_ID);
						msg.addInt(greenContrib);
						player.sendPacket(msg);
					}
					
					if ((blueContrib > 0) && player.destroyItemByItemId(ItemProcessType.FEE, SevenSigns.SEAL_STONE_BLUE_ID, blueContrib, this, false))
					{
						contribStonesFound = true;
						final SystemMessage msg = new SystemMessage(SystemMessageId.S2_S1_HAS_DISAPPEARED);
						msg.addItemName(SevenSigns.SEAL_STONE_BLUE_ID);
						msg.addInt(blueContrib);
						player.sendPacket(msg);
					}
					
					if (!contribStonesFound)
					{
						if (this instanceof DawnPriest)
						{
							showChatWindow(player, 6, "dawn_low_stones", false);
						}
						else
						{
							showChatWindow(player, 6, "dusk_low_stones", false);
						}
					}
					else
					{
						score = SevenSigns.getInstance().addPlayerStoneContrib(player.getObjectId(), blueContrib, greenContrib, redContrib);
						sm = new SystemMessage(SystemMessageId.YOUR_CONTRIBUTION_SCORE_IS_INCREASED_BY_S1);
						sm.addInt(score);
						player.sendPacket(sm);
						
						if (this instanceof DawnPriest)
						{
							showChatWindow(player, 6, "dawn", false);
						}
						else
						{
							showChatWindow(player, 6, "dusk", false);
						}
					}
					break;
				}
				case 6: // Contribute Seal Stones - SevenSigns 6 x
				{
					stoneType = Integer.parseInt(command.substring(13));
					
					final Item blueStones = player.getInventory().getItemByItemId(SevenSigns.SEAL_STONE_BLUE_ID);
					final Item greenStones = player.getInventory().getItemByItemId(SevenSigns.SEAL_STONE_GREEN_ID);
					final Item redStones = player.getInventory().getItemByItemId(SevenSigns.SEAL_STONE_RED_ID);
					final int blueStoneCount = blueStones == null ? 0 : blueStones.getCount();
					final int greenStoneCount = greenStones == null ? 0 : greenStones.getCount();
					final int redStoneCount = redStones == null ? 0 : redStones.getCount();
					int contribScore = SevenSigns.getInstance().getPlayerContribScore(player.getObjectId());
					boolean stonesFound = false;
					if (contribScore == FeatureConfig.ALT_MAXIMUM_PLAYER_CONTRIB)
					{
						player.sendPacket(SystemMessageId.CONTRIBUTION_LEVEL_HAS_EXCEEDED_THE_LIMIT_YOU_MAY_NOT_CONTINUE);
					}
					else
					{
						int redContribCount = 0;
						int greenContribCount = 0;
						int blueContribCount = 0;
						String contribStoneColor = null;
						String stoneColorContr = null;
						int stoneCountContr = 0;
						int stoneIdContr = 0;
						
						switch (stoneType)
						{
							case 1:
							{
								contribStoneColor = "Blue";
								stoneColorContr = "blue";
								stoneIdContr = SevenSigns.SEAL_STONE_BLUE_ID;
								stoneCountContr = blueStoneCount;
								break;
							}
							case 2:
							{
								contribStoneColor = "Green";
								stoneColorContr = "green";
								stoneIdContr = SevenSigns.SEAL_STONE_GREEN_ID;
								stoneCountContr = greenStoneCount;
								break;
							}
							case 3:
							{
								contribStoneColor = "Red";
								stoneColorContr = "red";
								stoneIdContr = SevenSigns.SEAL_STONE_RED_ID;
								stoneCountContr = redStoneCount;
								break;
							}
							case 4:
							{
								int tempContribScore = contribScore;
								redContribCount = (FeatureConfig.ALT_MAXIMUM_PLAYER_CONTRIB - tempContribScore) / SevenSigns.RED_CONTRIB_POINTS;
								if (redContribCount > redStoneCount)
								{
									redContribCount = redStoneCount;
								}
								
								tempContribScore += redContribCount * SevenSigns.RED_CONTRIB_POINTS;
								greenContribCount = (FeatureConfig.ALT_MAXIMUM_PLAYER_CONTRIB - tempContribScore) / SevenSigns.GREEN_CONTRIB_POINTS;
								if (greenContribCount > greenStoneCount)
								{
									greenContribCount = greenStoneCount;
								}
								
								tempContribScore += greenContribCount * SevenSigns.GREEN_CONTRIB_POINTS;
								blueContribCount = (FeatureConfig.ALT_MAXIMUM_PLAYER_CONTRIB - tempContribScore) / SevenSigns.BLUE_CONTRIB_POINTS;
								if (blueContribCount > blueStoneCount)
								{
									blueContribCount = blueStoneCount;
								}
								
								if ((redContribCount > 0) && player.destroyItemByItemId(ItemProcessType.FEE, SevenSigns.SEAL_STONE_RED_ID, redContribCount, this, false))
								{
									stonesFound = true;
									final SystemMessage msg = new SystemMessage(SystemMessageId.S2_S1_HAS_DISAPPEARED);
									msg.addItemName(SevenSigns.SEAL_STONE_RED_ID);
									msg.addInt(redContribCount);
									player.sendPacket(msg);
								}
								
								if ((greenContribCount > 0) && player.destroyItemByItemId(ItemProcessType.FEE, SevenSigns.SEAL_STONE_GREEN_ID, greenContribCount, this, false))
								{
									stonesFound = true;
									final SystemMessage msg = new SystemMessage(SystemMessageId.S2_S1_HAS_DISAPPEARED);
									msg.addItemName(SevenSigns.SEAL_STONE_GREEN_ID);
									msg.addInt(greenContribCount);
									player.sendPacket(msg);
								}
								
								if ((blueContribCount > 0) && player.destroyItemByItemId(ItemProcessType.FEE, SevenSigns.SEAL_STONE_BLUE_ID, blueContribCount, this, false))
								{
									stonesFound = true;
									final SystemMessage msg = new SystemMessage(SystemMessageId.S2_S1_HAS_DISAPPEARED);
									msg.addItemName(SevenSigns.SEAL_STONE_BLUE_ID);
									msg.addInt(blueContribCount);
									player.sendPacket(msg);
								}
								
								if (!stonesFound)
								{
									if (this instanceof DawnPriest)
									{
										showChatWindow(player, val, "dawn_no_stones", false);
									}
									else
									{
										showChatWindow(player, val, "dusk_no_stones", false);
									}
								}
								else
								{
									contribScore = SevenSigns.getInstance().addPlayerStoneContrib(player.getObjectId(), blueContribCount, greenContribCount, redContribCount);
									sm = new SystemMessage(SystemMessageId.YOUR_CONTRIBUTION_SCORE_IS_INCREASED_BY_S1);
									sm.addInt(contribScore);
									player.sendPacket(sm);
									
									if (this instanceof DawnPriest)
									{
										showChatWindow(player, 6, "dawn", false);
									}
									else
									{
										showChatWindow(player, 6, "dusk", false);
									}
								}
								return;
							}
						}
						
						if (this instanceof DawnPriest)
						{
							path = SevenSigns.SEVEN_SIGNS_HTML_PATH + "signs_6_dawn_contribute.htm";
						}
						else
						{
							path = SevenSigns.SEVEN_SIGNS_HTML_PATH + "signs_6_dusk_contribute.htm";
						}
						
						String contentContr = HtmCache.getInstance().getHtm(player, path);
						if (contentContr != null)
						{
							contentContr = contentContr.replace("%contribStoneColor%", contribStoneColor);
							contentContr = contentContr.replace("%stoneColor%", stoneColorContr);
							contentContr = contentContr.replace("%stoneCount%", String.valueOf(stoneCountContr));
							contentContr = contentContr.replace("%stoneItemId%", String.valueOf(stoneIdContr));
							contentContr = contentContr.replace("%objectId%", String.valueOf(getObjectId()));
							
							final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
							html.setHtml(contentContr);
							player.sendPacket(html);
						}
						else
						{
							LOGGER.warning("Problem with HTML text " + path);
						}
					}
					break;
				}
				case 7: // Exchange Ancient Adena for Adena - SevenSigns 7 xxxxxxx
				{
					int ancientAdenaConvert = 0;
					
					try
					{
						ancientAdenaConvert = Integer.parseInt(command.substring(13).trim());
					}
					catch (NumberFormatException e)
					{
						showChatWindow(player, SevenSigns.SEVEN_SIGNS_HTML_PATH + "blkmrkt_3.htm");
						break;
					}
					catch (StringIndexOutOfBoundsException e)
					{
						showChatWindow(player, SevenSigns.SEVEN_SIGNS_HTML_PATH + "blkmrkt_3.htm");
						break;
					}
					
					if (ancientAdenaConvert < 1)
					{
						showChatWindow(player, SevenSigns.SEVEN_SIGNS_HTML_PATH + "blkmrkt_3.htm");
						break;
					}
					
					if (ancientAdenaAmount < ancientAdenaConvert)
					{
						showChatWindow(player, SevenSigns.SEVEN_SIGNS_HTML_PATH + "blkmrkt_4.htm");
						break;
					}
					
					player.reduceAncientAdena(ItemProcessType.SELL, ancientAdenaConvert, this, true);
					player.addAdena(ItemProcessType.BUY, ancientAdenaConvert, this, true);
					showChatWindow(player, SevenSigns.SEVEN_SIGNS_HTML_PATH + "blkmrkt_5.htm");
					break;
				}
				case 9: // Receive Contribution Rewards
				{
					final int playerCabal = SevenSigns.getInstance().getPlayerCabal(player.getObjectId());
					final int winningCabal = SevenSigns.getInstance().getCabalHighestScore();
					if (SevenSigns.getInstance().isSealValidationPeriod() && (playerCabal == winningCabal))
					{
						final int ancientAdenaReward = SevenSigns.getInstance().getAncientAdenaReward(player.getObjectId(), true);
						if (ancientAdenaReward < 3)
						{
							if (this instanceof DawnPriest)
							{
								showChatWindow(player, 9, "dawn_b", false);
							}
							else
							{
								showChatWindow(player, 9, "dusk_b", false);
							}
							break;
						}
						
						player.addAncientAdena(ItemProcessType.REWARD, ancientAdenaReward, this, true);
						if (this instanceof DawnPriest)
						{
							showChatWindow(player, 9, "dawn_a", false);
						}
						else
						{
							showChatWindow(player, 9, "dusk_a", false);
						}
					}
					break;
				}
				case 11: // Teleport to Hunting Grounds
				{
					try
					{
						final String portInfo = command.substring(14).trim();
						final StringTokenizer st = new StringTokenizer(portInfo);
						final int x = Integer.parseInt(st.nextToken());
						final int y = Integer.parseInt(st.nextToken());
						final int z = Integer.parseInt(st.nextToken());
						final int ancientAdenaCost = Integer.parseInt(st.nextToken());
						if ((ancientAdenaCost > 0) && !player.reduceAncientAdena(ItemProcessType.FEE, ancientAdenaCost, this, true))
						{
							break;
						}
						
						player.teleToLocation(x, y, z);
					}
					catch (Exception e)
					{
						LOGGER.log(Level.WARNING, "SevenSigns: Error occurred while teleporting player: " + e.getMessage(), e);
					}
					break;
				}
				case 16:
				{
					if (this instanceof DawnPriest)
					{
						showChatWindow(player, val, "dawn", false);
					}
					else
					{
						showChatWindow(player, val, "dusk", false);
					}
					break;
				}
				case 17: // Exchange Seal Stones for Ancient Adena (Type Choice) - SevenSigns 17 x
				{
					stoneType = Integer.parseInt(command.substring(14));
					int stoneId = 0;
					long stoneCount = 0;
					int stoneValue = 0;
					String stoneColor = null;
					
					switch (stoneType)
					{
						case 1:
						{
							stoneColor = "blue";
							stoneId = SevenSigns.SEAL_STONE_BLUE_ID;
							stoneValue = SevenSigns.SEAL_STONE_BLUE_VALUE;
							break;
						}
						case 2:
						{
							stoneColor = "green";
							stoneId = SevenSigns.SEAL_STONE_GREEN_ID;
							stoneValue = SevenSigns.SEAL_STONE_GREEN_VALUE;
							break;
						}
						case 3:
						{
							stoneColor = "red";
							stoneId = SevenSigns.SEAL_STONE_RED_ID;
							stoneValue = SevenSigns.SEAL_STONE_RED_VALUE;
							break;
						}
						case 4:
						{
							final Item blueStonesAll = player.getInventory().getItemByItemId(SevenSigns.SEAL_STONE_BLUE_ID);
							final Item greenStonesAll = player.getInventory().getItemByItemId(SevenSigns.SEAL_STONE_GREEN_ID);
							final Item redStonesAll = player.getInventory().getItemByItemId(SevenSigns.SEAL_STONE_RED_ID);
							final int blueStoneCountAll = blueStonesAll == null ? 0 : blueStonesAll.getCount();
							final int greenStoneCountAll = greenStonesAll == null ? 0 : greenStonesAll.getCount();
							final int redStoneCountAll = redStonesAll == null ? 0 : redStonesAll.getCount();
							int ancientAdenaRewardAll = 0;
							ancientAdenaRewardAll = SevenSigns.calcAncientAdenaReward(blueStoneCountAll, greenStoneCountAll, redStoneCountAll);
							if (ancientAdenaRewardAll == 0)
							{
								if (this instanceof DawnPriest)
								{
									showChatWindow(player, 18, "dawn_no_stones", false);
								}
								else
								{
									showChatWindow(player, 18, "dusk_no_stones", false);
								}
								return;
							}
							
							if (blueStoneCountAll > 0)
							{
								player.destroyItemByItemId(ItemProcessType.FEE, SevenSigns.SEAL_STONE_BLUE_ID, blueStoneCountAll, this, true);
							}
							
							if (greenStoneCountAll > 0)
							{
								player.destroyItemByItemId(ItemProcessType.FEE, SevenSigns.SEAL_STONE_GREEN_ID, greenStoneCountAll, this, true);
							}
							
							if (redStoneCountAll > 0)
							{
								player.destroyItemByItemId(ItemProcessType.FEE, SevenSigns.SEAL_STONE_RED_ID, redStoneCountAll, this, true);
							}
							
							player.addAncientAdena(ItemProcessType.REWARD, ancientAdenaRewardAll, this, true);
							if (this instanceof DawnPriest)
							{
								showChatWindow(player, 18, "dawn", false);
							}
							else
							{
								showChatWindow(player, 18, "dusk", false);
							}
							return;
						}
					}
					
					final Item stoneInstance = player.getInventory().getItemByItemId(stoneId);
					if (stoneInstance != null)
					{
						stoneCount = stoneInstance.getCount();
					}
					
					if (this instanceof DawnPriest)
					{
						path = SevenSigns.SEVEN_SIGNS_HTML_PATH + "signs_17_dawn.htm";
					}
					else
					{
						path = SevenSigns.SEVEN_SIGNS_HTML_PATH + "signs_17_dusk.htm";
					}
					
					String content = HtmCache.getInstance().getHtm(player, path);
					if (content != null)
					{
						content = content.replace("%stoneColor%", stoneColor);
						content = content.replace("%stoneValue%", String.valueOf(stoneValue));
						content = content.replace("%stoneCount%", String.valueOf(stoneCount));
						content = content.replace("%stoneItemId%", String.valueOf(stoneId));
						content = content.replace("%objectId%", String.valueOf(getObjectId()));
						
						final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
						html.setHtml(content);
						player.sendPacket(html);
					}
					else
					{
						LOGGER.warning("Problem with HTML text " + SevenSigns.SEVEN_SIGNS_HTML_PATH + "signs_17.htm: " + path);
					}
					break;
				}
				case 18: // Exchange Seal Stones for Ancient Adena - SevenSigns 18 xxxx xxxxxx
				{
					final int convertStoneId = Integer.parseInt(command.substring(14, 18));
					int convertCount = 0;
					
					try
					{
						convertCount = Integer.parseInt(command.substring(19).trim());
					}
					catch (Exception nfe)
					{
						if (this instanceof DawnPriest)
						{
							showChatWindow(player, 18, "dawn_failed", false);
						}
						else
						{
							showChatWindow(player, 18, "dusk_failed", false);
						}
						break;
					}
					
					final Item convertItem = player.getInventory().getItemByItemId(convertStoneId);
					if (convertItem != null)
					{
						int ancientAdenaReward = 0;
						final int totalCount = convertItem.getCount();
						if ((convertCount <= totalCount) && (convertCount > 0))
						{
							switch (convertStoneId)
							{
								case SevenSigns.SEAL_STONE_BLUE_ID:
								{
									ancientAdenaReward = SevenSigns.calcAncientAdenaReward(convertCount, 0, 0);
									break;
								}
								case SevenSigns.SEAL_STONE_GREEN_ID:
								{
									ancientAdenaReward = SevenSigns.calcAncientAdenaReward(0, convertCount, 0);
									break;
								}
								case SevenSigns.SEAL_STONE_RED_ID:
								{
									ancientAdenaReward = SevenSigns.calcAncientAdenaReward(0, 0, convertCount);
									break;
								}
							}
							
							if (player.destroyItemByItemId(ItemProcessType.FEE, convertStoneId, convertCount, this, true))
							{
								player.addAncientAdena(ItemProcessType.REWARD, ancientAdenaReward, this, true);
								if (this instanceof DawnPriest)
								{
									showChatWindow(player, 18, "dawn", false);
								}
								else
								{
									showChatWindow(player, 18, "dusk", false);
								}
							}
						}
						else
						{
							if (this instanceof DawnPriest)
							{
								showChatWindow(player, 18, "dawn_low_stones", false);
							}
							else
							{
								showChatWindow(player, 18, "dusk_low_stones", false);
							}
							break;
						}
					}
					else
					{
						if (this instanceof DawnPriest)
						{
							showChatWindow(player, 18, "dawn_no_stones", false);
						}
						else
						{
							showChatWindow(player, 18, "dusk_no_stones", false);
						}
						break;
					}
					break;
				}
				case 19: // Seal Information (for when joining a cabal)
				{
					final int chosenSeal = Integer.parseInt(command.substring(16));
					final String fileSuffix = SevenSigns.getSealName(chosenSeal, true) + "_" + SevenSigns.getCabalShortName(cabal);
					showChatWindow(player, val, fileSuffix, false);
					break;
				}
				case 20: // Seal Status (for when joining a cabal)
				{
					final StringBuilder contentBuffer = new StringBuilder();
					if (this instanceof DawnPriest)
					{
						contentBuffer.append("<html><body>Priest of Dawn:<br><font color=\"LEVEL\">[ Seal Status ]</font><br>");
					}
					else
					{
						contentBuffer.append("<html><body>Dusk Priestess:<br><font color=\"LEVEL\">[ Status of the Seals ]</font><br>");
					}
					
					for (int i = 1; i < 4; i++)
					{
						final int sealOwner = SevenSigns.getInstance().getSealOwner(i);
						if (sealOwner != SevenSigns.CABAL_NULL)
						{
							contentBuffer.append("[" + SevenSigns.getSealName(i, false) + ": " + SevenSigns.getCabalName(sealOwner) + "]<br>");
						}
						else
						{
							contentBuffer.append("[" + SevenSigns.getSealName(i, false) + ": Nothingness]<br>");
						}
					}
					
					contentBuffer.append("<a action=\"bypass -h npc_" + getObjectId() + "_Chat 0\">Go back.</a></body></html>");
					
					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setHtml(contentBuffer.toString());
					player.sendPacket(html);
					break;
				}
				default:
				{
					showChatWindow(player, val, null, false);
					break;
				}
			}
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
	
	private void showChatWindow(Player player, int value, String suffix, boolean isDescription)
	{
		String filename = SevenSigns.SEVEN_SIGNS_HTML_PATH;
		filename += (isDescription) ? "desc_" + value : "signs_" + value;
		filename += (suffix != null) ? "_" + suffix + ".htm" : ".htm";
		showChatWindow(player, filename);
	}

	/**
	 * Handles the solo "no schedule" control options: closing the competition on demand (which
	 * validates the winning cabal's seals) and starting a fresh cycle. Command format:
	 * "SevenSignsSolo &lt;action&gt;" where action is 1 = open panel, 2 = validate, 3 = new cycle.
	 * @param player the player using the priest
	 * @param command the raw bypass command
	 */
	private void handleSoloControl(Player player, String command)
	{
		final SevenSigns signs = SevenSigns.getInstance();
		int action = 1;
		try
		{
			action = Integer.parseInt(command.substring(15).trim());
		}
		catch (Exception e)
		{
			action = 1;
		}

		if (!FeatureConfig.ALT_SEVENSIGNS_NO_SCHEDULE)
		{
			showSoloPanel(player, signs, "Manual Seven Signs control is disabled on this server.");
			return;
		}

		String message = null;
		switch (action)
		{
			case 2: // Close the competition and validate seals.
			{
				if (signs.isSealValidationPeriod())
				{
					message = "The Seal Validation period is already active. Begin a new cycle before closing again.";
				}
				else if (signs.getCabalHighestScore() == SevenSigns.CABAL_NULL)
				{
					message = "No cabal has contributed yet, so no Seal would be captured. Register and contribute Seal Stones first.";
				}
				else
				{
					signs.forceSealValidation();
					message = "The competition is closed and the Seals have been validated.";
				}
				break;
			}
			case 3: // Start a fresh cycle.
			{
				signs.forceNewCycle();
				message = "A new cycle has begun. Register again to take part.";
				break;
			}
			default: // Just (re)open the panel.
			{
				break;
			}
		}

		showSoloPanel(player, signs, message);
	}

	/**
	 * Sends the solo Seven Signs control panel, showing the current state and (optionally) a
	 * feedback message for the last action.
	 * @param player the player to send the panel to
	 * @param signs the Seven Signs engine
	 * @param message an optional feedback line, or {@code null} for none
	 */
	private void showSoloPanel(Player player, SevenSigns signs, String message)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player, SevenSigns.SEVEN_SIGNS_HTML_PATH + "solo_panel.htm");
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%status%", signs.getSummaryText());
		html.replace("%message%", (message != null) ? "<font color=\"FF0000\">" + message + "</font><br>" : "");
		player.sendPacket(html);
	}
}
