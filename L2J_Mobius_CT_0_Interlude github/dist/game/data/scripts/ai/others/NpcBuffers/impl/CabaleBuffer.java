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
package ai.others.NpcBuffers.impl;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.sevensigns.SevenSigns;
import org.l2jmobius.gameserver.model.skill.BuffInfo;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.enums.ChatType;

/**
 * Preacher of Doom and Orator of Revelations AI
 * @author UnAfraid, malyelfik
 */
public class CabaleBuffer extends Script
{
	private static final int DISTANCE_TO_WATCH_OBJECT = 900;
	
	// Messages
	protected static final String[] ORATOR_MSG =
	{
		"The day of judgment is near!",
		"The prophecy of darkness has been fulfilled!",
		"As foretold in the prophecy of darkness, the era of chaos has begun!",
		"The prophecy of darkness has come to pass!"
	};
	protected static final String[] PREACHER_MSG =
	{
		"This world will soon be annihilated!",
		"All is lost! Prepare to meet the goddess of death!",
		"All is lost! The prophecy of destruction has been fulfilled!",
		"The end of time has come! The prophecy of destruction has been fulfilled!"
	};
	
	// Skills
	private static final int ORATOR_FIGTER = 4364;
	private static final int ORATOR_MAGE = 4365;
	private static final int PREACHER_FIGTER = 4361;
	private static final int PREACHER_MAGE = 4362;
	
	private CabaleBuffer()
	{
		addFirstTalkId(SevenSigns.ORATOR_NPC_ID, SevenSigns.PREACHER_NPC_ID);
		addSpawnId(SevenSigns.ORATOR_NPC_ID, SevenSigns.PREACHER_NPC_ID);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return null;
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		ThreadPool.schedule(new CabaleAI(npc), 3000);
		ThreadPool.schedule(new Talk(npc), 60000);
	}
	
	protected class Talk implements Runnable
	{
		private final Npc _npc;
		
		protected Talk(Npc npc)
		{
			_npc = npc;
		}
		
		@Override
		public void run()
		{
			if ((_npc != null) && !_npc.isDecayed())
			{
				String[] messages = ORATOR_MSG;
				if (_npc.getId() == SevenSigns.PREACHER_NPC_ID)
				{
					messages = PREACHER_MSG;
				}
				
				broadcastSay(_npc, getRandomEntry(messages), -1);
				ThreadPool.schedule(this, 60000);
			}
		}
	}
	
	protected class CabaleAI implements Runnable
	{
		private final Npc _npc;
		
		protected CabaleAI(Npc npc)
		{
			_npc = npc;
		}
		
		@Override
		public void run()
		{
			if ((_npc == null) || !_npc.isSpawned())
			{
				return;
			}
			
			boolean isBuffAWinner = false;
			boolean isBuffALoser = false;
			
			final int winningCabal = SevenSigns.getInstance().getCabalHighestScore();
			int losingCabal = SevenSigns.CABAL_NULL;
			if (winningCabal == SevenSigns.CABAL_DAWN)
			{
				losingCabal = SevenSigns.CABAL_DUSK;
			}
			else if (winningCabal == SevenSigns.CABAL_DUSK)
			{
				losingCabal = SevenSigns.CABAL_DAWN;
			}
			
			for (Player player : World.getInstance().getVisibleObjects(_npc, Player.class))
			{
				if ((player == null) || player.isInvul())
				{
					continue;
				}
				
				final int playerCabal = SevenSigns.getInstance().getPlayerCabal(player.getObjectId());
				if ((playerCabal == winningCabal) && (playerCabal != SevenSigns.CABAL_NULL) && (_npc.getId() == SevenSigns.ORATOR_NPC_ID))
				{
					if (!player.isMageClass())
					{
						if (handleCast(player, ORATOR_FIGTER))
						{
							if (getAbnormalLevel(player, ORATOR_FIGTER) == 2)
							{
								broadcastSay(_npc, player.getName() + "! I give you the blessing of prophecy!", 500);
							}
							else
							{
								broadcastSay(_npc, "I bestow upon you a blessing!", 1);
							}
							
							isBuffAWinner = true;
							continue;
						}
					}
					else
					{
						if (handleCast(player, ORATOR_MAGE))
						{
							if (getAbnormalLevel(player, ORATOR_MAGE) == 2)
							{
								broadcastSay(_npc, player.getName() + "! I bestow upon you the authority of the abyss!", 500);
							}
							else
							{
								broadcastSay(_npc, "Herald of the new era, open your eyes!", 1);
							}
							
							isBuffAWinner = true;
							continue;
						}
					}
				}
				else if ((playerCabal == losingCabal) && (playerCabal != SevenSigns.CABAL_NULL) && (_npc.getId() == SevenSigns.PREACHER_NPC_ID))
				{
					if (!player.isMageClass())
					{
						if (handleCast(player, PREACHER_FIGTER))
						{
							if (getAbnormalLevel(player, PREACHER_FIGTER) == 2)
							{
								broadcastSay(_npc, "A curse upon you!", 500);
							}
							else
							{
								broadcastSay(_npc, "You don't have any hope! Your end has come!", 1);
							}
							
							isBuffALoser = true;
							continue;
						}
					}
					else
					{
						if (handleCast(player, PREACHER_MAGE))
						{
							if (getAbnormalLevel(player, PREACHER_MAGE) == 2)
							{
								broadcastSay(_npc, player.getName() + "! You might as well give up!", 500);
							}
							else
							{
								broadcastSay(_npc, player.getName() + "! You bring an ill wind!", 1);
							}
							
							isBuffALoser = true;
							continue;
						}
					}
				}
				
				if (isBuffAWinner && isBuffALoser)
				{
					break;
				}
			}
			
			ThreadPool.schedule(this, 3000);
		}
		
		/**
		 * For each known player in range, cast either the positive or negative buff.<br>
		 * The stats affected depend on the player type, either a fighter or a mystic.<br>
		 * <br>
		 * Curse of Destruction (Loser)<br>
		 * - Fighters: -25% Accuracy, -25% Effect Resistance<br>
		 * - Mystics: -25% Casting Speed, -25% Effect Resistance<br>
		 * <br>
		 * <br>
		 * Blessing of Prophecy (Winner)<br>
		 * - Fighters: +25% Max Load, +25% Effect Resistance<br>
		 * - Mystics: +25% Magic Cancel Resist, +25% Effect Resistance<br>
		 * @param player
		 * @param skillId
		 * @return
		 */
		private boolean handleCast(Player player, int skillId)
		{
			if (player.isDead() || !player.isSpawned() || !_npc.isInsideRadius3D(player, DISTANCE_TO_WATCH_OBJECT))
			{
				return false;
			}
			
			boolean doCast = false;
			int skillLevel = 1;
			
			final int level = getAbnormalLevel(player, skillId);
			if (level == 0)
			{
				doCast = true;
			}
			else if ((level == 1) && (getRandom(100) < 5))
			{
				doCast = true;
				skillLevel = 2;
			}
			
			if (doCast)
			{
				final Skill skill = SkillData.getInstance().getSkill(skillId, skillLevel);
				_npc.setTarget(player);
				_npc.doCast(skill);
				return true;
			}
			
			return false;
		}
	}
	
	public void broadcastSay(Npc npc, String message, int chance)
	{
		if (chance == -1)
		{
			npc.broadcastSay(ChatType.NPC_GENERAL, message);
		}
		else if (getRandom(10000) < chance)
		{
			npc.broadcastSay(ChatType.NPC_GENERAL, message);
		}
	}
	
	public int getAbnormalLevel(Player player, int skillId)
	{
		final BuffInfo info = player.getEffectList().getBuffInfoBySkillId(skillId);
		return (info != null) ? info.getSkill().getAbnormalLevel() : 0;
	}
	
	public static void main(String[] args)
	{
		new CabaleBuffer();
	}
}
