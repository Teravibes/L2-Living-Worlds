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
package handlers.bypass.npc;

import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;

import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.FestivalGuide;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.groups.PartyMessageType;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.sevensigns.SevenSigns;
import org.l2jmobius.gameserver.model.sevensigns.SevenSignsFestival;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class Festival implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
		"festival",
		"festivaldesc"
	};
	
	@Override
	public boolean onCommand(String command, Player player, Creature target)
	{
		if (!(target instanceof FestivalGuide))
		{
			return false;
		}
		
		final FestivalGuide npc = (FestivalGuide) target;
		try
		{
			final int val;
			if (command.toLowerCase().startsWith(COMMANDS[1]))
			{
				val = Integer.parseInt(command.substring(13));
				npc.showChatWindow(player, val, null, true);
				return true;
			}
			
			final Party party;
			val = Integer.parseInt(command.substring(9, 10));
			switch (val)
			{
				case 1: // Become a Participant
				{
					// Check if the festival period is active, if not then don't allow registration.
					if (SevenSigns.getInstance().isSealValidationPeriod())
					{
						npc.showChatWindow(player, 2, "a", false);
						return true;
					}
					
					// Check if a festival is in progress, then don't allow registration yet.
					if (SevenSignsFestival.getInstance().isFestivalInitialized())
					{
						player.sendMessage("You cannot sign up while a festival is in progress.");
						return true;
					}
					
					// Check if the player is in a formed party already.
					if (!player.isInParty())
					{
						npc.showChatWindow(player, 2, "b", false);
						return true;
					}
					
					party = player.getParty();
					
					// Check if the player is the party leader.
					if (!party.isLeader(player))
					{
						npc.showChatWindow(player, 2, "c", false);
						return true;
					}
					
					// Check to see if the party has at least 5 members.
					if (party.getMemberCount() < FeatureConfig.ALT_FESTIVAL_MIN_PLAYER)
					{
						npc.showChatWindow(player, 2, "b", false);
						return true;
					}
					
					// Check if all the party members are in the required level range.
					if (party.getLevel() > SevenSignsFestival.getMaxLevelForFestival(npc.getFestivalType()))
					{
						npc.showChatWindow(player, 2, "d", false);
						return true;
					}
					
					// Check to see if the player has already signed up
					if (player.isFestivalParticipant())
					{
						SevenSignsFestival.getInstance().setParticipants(npc.getFestivalOracle(), npc.getFestivalType(), party);
						npc.showChatWindow(player, 2, "f", false);
						return true;
					}
					
					npc.showChatWindow(player, 1, null, false);
					break;
				}
				case 2: // Seal Stones
				{
					final int stoneType = Integer.parseInt(command.substring(11));
					final int stoneCount = npc.getStoneCount(stoneType);
					if (stoneCount <= 0)
					{
						return false;
					}
					
					if (!player.destroyItemByItemId(ItemProcessType.FEE, stoneType, stoneCount, npc, true))
					{
						return false;
					}
					
					SevenSignsFestival.getInstance().setParticipants(npc.getFestivalOracle(), npc.getFestivalType(), player.getParty());
					SevenSignsFestival.getInstance().addAccumulatedBonus(npc.getFestivalType(), stoneType, stoneCount);
					npc.showChatWindow(player, 2, "e", false);
					break;
				}
				case 3: // Score Registration
				{
					// Check if the festival period is active, if not then don't register the score.
					if (SevenSigns.getInstance().isSealValidationPeriod())
					{
						npc.showChatWindow(player, 3, "a", false);
						return true;
					}
					
					// Check if a festival is in progress, if it is don't register the score.
					if (SevenSignsFestival.getInstance().isFestivalInProgress())
					{
						player.sendMessage("You cannot register a score while a festival is in progress.");
						return true;
					}
					
					// Check if the player is in a party.
					if (!player.isInParty())
					{
						npc.showChatWindow(player, 3, "b", false);
						return true;
					}
					
					final List<Integer> prevParticipants = SevenSignsFestival.getInstance().getPreviousParticipants(npc.getFestivalOracle(), npc.getFestivalType());
					
					// Check if there are any past participants.
					if ((prevParticipants == null) || prevParticipants.isEmpty() || !prevParticipants.contains(player.getObjectId()))
					{
						npc.showChatWindow(player, 3, "b", false);
						return true;
					}
					
					// Check if this player was the party leader in the festival.
					if (player.getObjectId() != prevParticipants.get(0))
					{
						npc.showChatWindow(player, 3, "b", false);
						return true;
					}
					
					final Item bloodOfferings = player.getInventory().getItemByItemId(SevenSignsFestival.FESTIVAL_OFFERING_ID);
					
					// Check if the player collected any blood offerings during the festival.
					if (bloodOfferings == null)
					{
						player.sendMessage("You do not have any blood offerings to contribute.");
						return true;
					}
					
					final int offeringScore = bloodOfferings.getCount() * SevenSignsFestival.FESTIVAL_OFFERING_VALUE;
					if (!player.destroyItem(ItemProcessType.FEE, bloodOfferings, npc, false))
					{
						return true;
					}
					
					final boolean isHighestScore = SevenSignsFestival.getInstance().setFinalScore(player, npc.getFestivalOracle(), npc.getFestivalType(), offeringScore);
					final SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_CONTRIBUTION_SCORE_IS_INCREASED_BY_S1);
					sm.addInt(offeringScore);
					player.sendPacket(sm);
					if (isHighestScore)
					{
						npc.showChatWindow(player, 3, "c", false);
					}
					else
					{
						npc.showChatWindow(player, 3, "d", false);
					}
					break;
				}
				case 4: // Current High Scores
				{
					final StringBuilder strBuffer = new StringBuilder(500);
					strBuffer.append("<html><body>Festival Guide:<br>These are the top scores of the week, for the ");
					final StatSet dawnData = SevenSignsFestival.getInstance().getHighestScoreData(SevenSigns.CABAL_DAWN, npc.getFestivalType());
					final StatSet duskData = SevenSignsFestival.getInstance().getHighestScoreData(SevenSigns.CABAL_DUSK, npc.getFestivalType());
					final StatSet overallData = SevenSignsFestival.getInstance().getOverallHighestScoreData(npc.getFestivalType());
					final int dawnScore = dawnData.getInt("score");
					final int duskScore = duskData.getInt("score");
					int overallScore = 0;
					
					// If no data is returned, assume there is no record, or all scores are 0.
					if (overallData != null)
					{
						overallScore = overallData.getInt("score");
					}
					
					strBuffer.append(SevenSignsFestival.getFestivalName(npc.getFestivalType()) + " festival.<br>");
					if (dawnScore > 0)
					{
						strBuffer.append("Dawn: " + calculateDate(dawnData.getString("date")) + ". Score " + dawnScore + "<br>" + dawnData.getString("members") + "<br>");
					}
					else
					{
						strBuffer.append("Dawn: No record exists. Score 0<br>");
					}
					
					if (duskScore > 0)
					{
						strBuffer.append("Dusk: " + calculateDate(duskData.getString("date")) + ". Score " + duskScore + "<br>" + duskData.getString("members") + "<br>");
					}
					else
					{
						strBuffer.append("Dusk: No record exists. Score 0<br>");
					}
					
					if ((overallScore > 0) && (overallData != null))
					{
						final String cabalStr;
						if (overallData.getString("cabal").equals("dawn"))
						{
							cabalStr = "Children of Dawn";
						}
						else
						{
							cabalStr = "Children of Dusk";
						}
						
						strBuffer.append("Consecutive top scores: " + calculateDate(overallData.getString("date")) + ". Score " + overallScore + "<br>Affilated side: " + cabalStr + "<br>" + overallData.getString("members") + "<br>");
					}
					else
					{
						strBuffer.append("Consecutive top scores: No record exists. Score 0<br>");
					}
					strBuffer.append("<a action=\"bypass -h npc_" + npc.getObjectId() + "_Chat 0\">Go back.</a></body></html>");
					final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
					html.setHtml(strBuffer.toString());
					player.sendPacket(html);
					break;
				}
				case 8: // Increase the Festival Challenge
				{
					if (!player.isInParty())
					{
						return true;
					}
					
					if (!SevenSignsFestival.getInstance().isFestivalInProgress())
					{
						return true;
					}
					
					party = player.getParty();
					if (!party.isLeader(player))
					{
						npc.showChatWindow(player, 8, "a", false);
						return true;
					}
					
					if (SevenSignsFestival.getInstance().increaseChallenge(npc.getFestivalOracle(), npc.getFestivalType()))
					{
						npc.showChatWindow(player, 8, "b", false);
					}
					else
					{
						npc.showChatWindow(player, 8, "c", false);
					}
					break;
				}
				case 9: // Leave the Festival
				{
					if (!player.isInParty())
					{
						return true;
					}
					
					party = player.getParty();
					if (party.isLeader(player))
					{
						SevenSignsFestival.getInstance().updateParticipants(player, null);
					}
					else
					{
						if (party.getMemberCount() > FeatureConfig.ALT_FESTIVAL_MIN_PLAYER)
						{
							party.removePartyMember(player, PartyMessageType.EXPELLED);
						}
						else
						{
							player.sendMessage("Only the party leader can leave a festival when a party has minimum number of members.");
						}
					}
					break;
				}
				case 0: // Distribute Accumulated Bonus
				{
					if (!SevenSigns.getInstance().isSealValidationPeriod())
					{
						player.sendMessage("Bonuses cannot be paid during the competition period.");
						return true;
					}
					
					if (SevenSignsFestival.getInstance().distribAccumulatedBonus(player) > 0)
					{
						npc.showChatWindow(player, 0, "a", false);
					}
					else
					{
						npc.showChatWindow(player, 0, "b", false);
					}
					break;
				}
				default:
				{
					npc.showChatWindow(player, val, null, false);
				}
			}
			
			return true;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception in " + getClass().getSimpleName(), e);
		}
		
		return false;
	}
	
	private final String calculateDate(String milliFromEpoch)
	{
		final long numMillis = Long.parseLong(milliFromEpoch);
		final Calendar calCalc = Calendar.getInstance();
		calCalc.setTimeInMillis(numMillis);
		return calCalc.get(Calendar.YEAR) + "/" + calCalc.get(Calendar.MONTH) + "/" + calCalc.get(Calendar.DAY_OF_MONTH);
	}
	
	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
}
