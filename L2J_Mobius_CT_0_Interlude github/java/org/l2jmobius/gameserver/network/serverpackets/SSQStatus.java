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
package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.sevensigns.SevenSigns;
import org.l2jmobius.gameserver.model.sevensigns.SevenSignsFestival;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * Seven Signs Record Update
 * @author Tempy
 * @editor shansoft
 */
public class SSQStatus extends ServerPacket
{
	private final int _objectId;
	private final int _page;
	
	public SSQStatus(int objectId, int recordPage)
	{
		_objectId = objectId;
		_page = recordPage;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		final int winningCabal = SevenSigns.getInstance().getCabalHighestScore();
		final int totalDawnMembers = SevenSigns.getInstance().getTotalMembers(SevenSigns.CABAL_DAWN);
		final int totalDuskMembers = SevenSigns.getInstance().getTotalMembers(SevenSigns.CABAL_DUSK);
		ServerPackets.SSQ_STATUS.writeId(this, buffer);
		buffer.writeByte(_page);
		buffer.writeByte(SevenSigns.getInstance().getCurrentPeriod()); // current period?
		int dawnPercent = 0;
		int duskPercent = 0;
		switch (_page)
		{
			case 1:
			{
				// [ddd cc dd ddd c ddd c]
				buffer.writeInt(SevenSigns.getInstance().getCurrentCycle());
				final int currentPeriod = SevenSigns.getInstance().getCurrentPeriod();
				switch (currentPeriod)
				{
					case SevenSigns.PERIOD_COMP_RECRUITING:
					{
						buffer.writeInt(SystemMessageId.THIS_IS_THE_INITIAL_PERIOD.getId());
						break;
					}
					case SevenSigns.PERIOD_COMPETITION:
					{
						buffer.writeInt(SystemMessageId.THIS_IS_A_QUEST_EVENT_PERIOD.getId());
						break;
					}
					case SevenSigns.PERIOD_COMP_RESULTS:
					{
						buffer.writeInt(SystemMessageId.THIS_IS_A_PERIOD_OF_CALCULATING_STATISTICS_IN_THE_SERVER.getId());
						break;
					}
					case SevenSigns.PERIOD_SEAL_VALIDATION:
					{
						buffer.writeInt(SystemMessageId.THIS_IS_THE_SEAL_VALIDATION_PERIOD.getId());
						break;
					}
				}
				
				switch (currentPeriod)
				{
					case SevenSigns.PERIOD_COMP_RECRUITING:
					case SevenSigns.PERIOD_COMP_RESULTS:
					{
						buffer.writeInt(SystemMessageId.UNTIL_TODAY_AT_6_00_P_M.getId());
						break;
					}
					case SevenSigns.PERIOD_COMPETITION:
					case SevenSigns.PERIOD_SEAL_VALIDATION:
					{
						buffer.writeInt(SystemMessageId.UNTIL_NEXT_MONDAY_AT_6_00_P_M.getId());
						break;
					}
				}
				
				buffer.writeByte(SevenSigns.getInstance().getPlayerCabal(_objectId));
				buffer.writeByte(SevenSigns.getInstance().getPlayerSeal(_objectId));
				buffer.writeInt(SevenSigns.getInstance().getPlayerStoneContrib(_objectId)); // Seal Stones Turned-In
				buffer.writeInt(SevenSigns.getInstance().getPlayerAdenaCollect(_objectId)); // Ancient Adena to Collect
				final double dawnStoneScore = SevenSigns.getInstance().getCurrentStoneScore(SevenSigns.CABAL_DAWN);
				final int dawnFestivalScore = SevenSigns.getInstance().getCurrentFestivalScore(SevenSigns.CABAL_DAWN);
				final double duskStoneScore = SevenSigns.getInstance().getCurrentStoneScore(SevenSigns.CABAL_DUSK);
				final int duskFestivalScore = SevenSigns.getInstance().getCurrentFestivalScore(SevenSigns.CABAL_DUSK);
				final double totalStoneScore = duskStoneScore + dawnStoneScore;
				/*
				 * Scoring seems to be proportionate to a set base value, so base this on the maximum obtainable score from festivals, which is 500.
				 */
				int duskStoneScoreProp = 0;
				int dawnStoneScoreProp = 0;
				if (totalStoneScore != 0)
				{
					duskStoneScoreProp = Math.round(((float) duskStoneScore / (float) totalStoneScore) * 500);
					dawnStoneScoreProp = Math.round(((float) dawnStoneScore / (float) totalStoneScore) * 500);
				}
				
				final int duskTotalScore = SevenSigns.getInstance().getCurrentScore(SevenSigns.CABAL_DUSK);
				final int dawnTotalScore = SevenSigns.getInstance().getCurrentScore(SevenSigns.CABAL_DAWN);
				final int totalOverallScore = duskTotalScore + dawnTotalScore;
				if (totalOverallScore != 0)
				{
					dawnPercent = Math.round(((float) dawnTotalScore / totalOverallScore) * 100);
					duskPercent = Math.round(((float) duskTotalScore / totalOverallScore) * 100);
				}
				/* DUSK */
				buffer.writeInt(duskStoneScoreProp); // Seal Stone Score
				buffer.writeInt(duskFestivalScore); // Festival Score
				buffer.writeInt(duskTotalScore); // Total Score
				buffer.writeByte(duskPercent); // Dusk %
				/* DAWN */
				buffer.writeInt(dawnStoneScoreProp); // Seal Stone Score
				buffer.writeInt(dawnFestivalScore); // Festival Score
				buffer.writeInt(dawnTotalScore); // Total Score
				buffer.writeByte(dawnPercent); // Dawn %
				break;
			}
			case 2:
			{
				// c cc hc [cd (dc (S))]
				buffer.writeShort(1);
				buffer.writeByte(5); // Total number of festivals
				for (int i = 0; i < 5; i++)
				{
					buffer.writeByte(i + 1); // Current client-side festival ID
					buffer.writeInt(SevenSignsFestival.FESTIVAL_LEVEL_SCORES[i]);
					final int duskScore = SevenSignsFestival.getInstance().getHighestScore(SevenSigns.CABAL_DUSK, i);
					final int dawnScore = SevenSignsFestival.getInstance().getHighestScore(SevenSigns.CABAL_DAWN, i);
					
					// Dusk Score \\
					buffer.writeInt(duskScore);
					StatSet highScoreData = SevenSignsFestival.getInstance().getHighestScoreData(SevenSigns.CABAL_DUSK, i);
					String[] partyMembers = highScoreData.getString("members").split(",");
					if (partyMembers != null)
					{
						buffer.writeByte(partyMembers.length);
						for (String partyMember : partyMembers)
						{
							buffer.writeString(partyMember);
						}
					}
					else
					{
						buffer.writeByte(0);
					}
					
					// Dawn Score \\
					buffer.writeInt(dawnScore);
					highScoreData = SevenSignsFestival.getInstance().getHighestScoreData(SevenSigns.CABAL_DAWN, i);
					partyMembers = highScoreData.getString("members").split(",");
					if (partyMembers != null)
					{
						buffer.writeByte(partyMembers.length);
						for (String partyMember : partyMembers)
						{
							buffer.writeString(partyMember);
						}
					}
					else
					{
						buffer.writeByte(0);
					}
				}
				break;
			}
			case 3:
			{
				// c cc [ccc (cccc)]
				buffer.writeByte(10); // Minimum limit for winning cabal to retain their seal
				buffer.writeByte(35); // Minimum limit for winning cabal to claim a seal
				buffer.writeByte(3); // Total number of seals
				for (int i = 1; i < 4; i++)
				{
					final int dawnProportion = SevenSigns.getInstance().getSealProportion(i, SevenSigns.CABAL_DAWN);
					final int duskProportion = SevenSigns.getInstance().getSealProportion(i, SevenSigns.CABAL_DUSK);
					buffer.writeByte(i);
					buffer.writeByte(SevenSigns.getInstance().getSealOwner(i));
					if (totalDuskMembers == 0)
					{
						if (totalDawnMembers == 0)
						{
							buffer.writeByte(0);
							buffer.writeByte(0);
						}
						else
						{
							buffer.writeByte(0);
							buffer.writeByte(Math.round(((float) dawnProportion / totalDawnMembers) * 100));
						}
					}
					else
					{
						if (totalDawnMembers == 0)
						{
							buffer.writeByte(Math.round(((float) duskProportion / totalDuskMembers) * 100));
							buffer.writeByte(0);
						}
						else
						{
							buffer.writeByte(Math.round(((float) duskProportion / totalDuskMembers) * 100));
							buffer.writeByte(Math.round(((float) dawnProportion / totalDawnMembers) * 100));
						}
					}
				}
				break;
			}
			case 4:
			{
				// c cc [cc (cchh)]
				buffer.writeByte(winningCabal); // Overall predicted winner
				buffer.writeByte(3); // Total number of seals
				for (int i = 1; i < 4; i++)
				{
					final int dawnProportion = SevenSigns.getInstance().getSealProportion(i, SevenSigns.CABAL_DAWN);
					final int duskProportion = SevenSigns.getInstance().getSealProportion(i, SevenSigns.CABAL_DUSK);
					dawnPercent = Math.round((dawnProportion / (totalDawnMembers == 0 ? 1 : (float) totalDawnMembers)) * 100);
					duskPercent = Math.round((duskProportion / (totalDuskMembers == 0 ? 1 : (float) totalDuskMembers)) * 100);
					final int sealOwner = SevenSigns.getInstance().getSealOwner(i);
					buffer.writeByte(i);
					switch (sealOwner)
					{
						case SevenSigns.CABAL_NULL:
						{
							switch (winningCabal)
							{
								case SevenSigns.CABAL_NULL:
								{
									buffer.writeByte(SevenSigns.CABAL_NULL);
									buffer.writeShort(SystemMessageId.THE_COMPETITION_HAS_ENDED_IN_A_TIE_THEREFORE_NOBODY_HAS_BEEN_AWARDED_THE_SEAL.getId());
									break;
								}
								case SevenSigns.CABAL_DAWN:
								{
									if (dawnPercent >= 35)
									{
										buffer.writeByte(SevenSigns.CABAL_DAWN);
										buffer.writeShort(SystemMessageId.ALTHOUGH_THE_SEAL_WAS_NOT_OWNED_SINCE_35_PERCENT_OR_MORE_PEOPLE_HAVE_VOTED.getId());
									}
									else
									{
										buffer.writeByte(SevenSigns.CABAL_NULL);
										buffer.writeShort(SystemMessageId.SINCE_THE_SEAL_WAS_NOT_OWNED_DURING_THE_PREVIOUS_PERIOD_AND_SINCE_LESS_THAN_35_PERCENT_OF_PEOPLE_HAVE_VOTED.getId());
									}
									break;
								}
								case SevenSigns.CABAL_DUSK:
								{
									if (duskPercent >= 35)
									{
										buffer.writeByte(SevenSigns.CABAL_DUSK);
										buffer.writeShort(SystemMessageId.ALTHOUGH_THE_SEAL_WAS_NOT_OWNED_SINCE_35_PERCENT_OR_MORE_PEOPLE_HAVE_VOTED.getId());
									}
									else
									{
										buffer.writeByte(SevenSigns.CABAL_NULL);
										buffer.writeShort(SystemMessageId.SINCE_THE_SEAL_WAS_NOT_OWNED_DURING_THE_PREVIOUS_PERIOD_AND_SINCE_LESS_THAN_35_PERCENT_OF_PEOPLE_HAVE_VOTED.getId());
									}
									break;
								}
							}
							break;
						}
						case SevenSigns.CABAL_DAWN:
						{
							switch (winningCabal)
							{
								case SevenSigns.CABAL_NULL:
								{
									if (dawnPercent >= 10)
									{
										buffer.writeByte(SevenSigns.CABAL_DAWN);
										buffer.writeShort(SystemMessageId.SINCE_THE_SEAL_WAS_OWNED_DURING_THE_PREVIOUS_PERIOD_AND_10_PERCENT_OR_MORE_PEOPLE_HAVE_VOTED.getId());
									}
									else
									{
										buffer.writeByte(SevenSigns.CABAL_NULL);
										buffer.writeShort(SystemMessageId.THE_COMPETITION_HAS_ENDED_IN_A_TIE_THEREFORE_NOBODY_HAS_BEEN_AWARDED_THE_SEAL.getId());
									}
									break;
								}
								case SevenSigns.CABAL_DAWN:
								{
									if (dawnPercent >= 10)
									{
										buffer.writeByte(sealOwner);
										buffer.writeShort(SystemMessageId.SINCE_THE_SEAL_WAS_OWNED_DURING_THE_PREVIOUS_PERIOD_AND_10_PERCENT_OR_MORE_PEOPLE_HAVE_VOTED.getId());
									}
									else
									{
										buffer.writeByte(SevenSigns.CABAL_NULL);
										buffer.writeShort(SystemMessageId.ALTHOUGH_THE_SEAL_WAS_OWNED_DURING_THE_PREVIOUS_PERIOD_BECAUSE_LESS_THAN_10_PERCENT_OF_PEOPLE_HAVE_VOTED.getId());
									}
									break;
								}
								case SevenSigns.CABAL_DUSK:
								{
									if (duskPercent >= 35)
									{
										buffer.writeByte(SevenSigns.CABAL_DUSK);
										buffer.writeShort(SystemMessageId.ALTHOUGH_THE_SEAL_WAS_NOT_OWNED_SINCE_35_PERCENT_OR_MORE_PEOPLE_HAVE_VOTED.getId());
									}
									else if (dawnPercent >= 10)
									{
										buffer.writeByte(SevenSigns.CABAL_DAWN);
										buffer.writeShort(SystemMessageId.SINCE_THE_SEAL_WAS_OWNED_DURING_THE_PREVIOUS_PERIOD_AND_10_PERCENT_OR_MORE_PEOPLE_HAVE_VOTED.getId());
									}
									else
									{
										buffer.writeByte(SevenSigns.CABAL_NULL);
										buffer.writeShort(SystemMessageId.ALTHOUGH_THE_SEAL_WAS_OWNED_DURING_THE_PREVIOUS_PERIOD_BECAUSE_LESS_THAN_10_PERCENT_OF_PEOPLE_HAVE_VOTED.getId());
									}
									break;
								}
							}
							break;
						}
						case SevenSigns.CABAL_DUSK:
						{
							switch (winningCabal)
							{
								case SevenSigns.CABAL_NULL:
								{
									if (duskPercent >= 10)
									{
										buffer.writeByte(SevenSigns.CABAL_DUSK);
										buffer.writeShort(SystemMessageId.SINCE_THE_SEAL_WAS_OWNED_DURING_THE_PREVIOUS_PERIOD_AND_10_PERCENT_OR_MORE_PEOPLE_HAVE_VOTED.getId());
									}
									else
									{
										buffer.writeByte(SevenSigns.CABAL_NULL);
										buffer.writeShort(SystemMessageId.THE_COMPETITION_HAS_ENDED_IN_A_TIE_THEREFORE_NOBODY_HAS_BEEN_AWARDED_THE_SEAL.getId());
									}
									break;
								}
								case SevenSigns.CABAL_DAWN:
								{
									if (dawnPercent >= 35)
									{
										buffer.writeByte(SevenSigns.CABAL_DAWN);
										buffer.writeShort(SystemMessageId.ALTHOUGH_THE_SEAL_WAS_NOT_OWNED_SINCE_35_PERCENT_OR_MORE_PEOPLE_HAVE_VOTED.getId());
									}
									else if (duskPercent >= 10)
									{
										buffer.writeByte(sealOwner);
										buffer.writeShort(SystemMessageId.SINCE_THE_SEAL_WAS_OWNED_DURING_THE_PREVIOUS_PERIOD_AND_10_PERCENT_OR_MORE_PEOPLE_HAVE_VOTED.getId());
									}
									else
									{
										buffer.writeByte(SevenSigns.CABAL_NULL);
										buffer.writeShort(SystemMessageId.ALTHOUGH_THE_SEAL_WAS_OWNED_DURING_THE_PREVIOUS_PERIOD_BECAUSE_LESS_THAN_10_PERCENT_OF_PEOPLE_HAVE_VOTED.getId());
									}
									break;
								}
								case SevenSigns.CABAL_DUSK:
								{
									if (duskPercent >= 10)
									{
										buffer.writeByte(sealOwner);
										buffer.writeShort(SystemMessageId.SINCE_THE_SEAL_WAS_OWNED_DURING_THE_PREVIOUS_PERIOD_AND_10_PERCENT_OR_MORE_PEOPLE_HAVE_VOTED.getId());
									}
									else
									{
										buffer.writeByte(SevenSigns.CABAL_NULL);
										buffer.writeShort(SystemMessageId.ALTHOUGH_THE_SEAL_WAS_OWNED_DURING_THE_PREVIOUS_PERIOD_BECAUSE_LESS_THAN_10_PERCENT_OF_PEOPLE_HAVE_VOTED.getId());
									}
									break;
								}
							}
							break;
						}
					}
				}
				break;
			}
		}
	}
}
