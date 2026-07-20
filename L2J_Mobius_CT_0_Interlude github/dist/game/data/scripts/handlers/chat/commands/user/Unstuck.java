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
package handlers.chat.commands.user;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.custom.FactionSystemConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.handler.IUserCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.SetupGauge;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;

/**
 * Unstuck user command.
 */
public class Unstuck implements IUserCommandHandler
{
	private static final int[] COMMAND_IDS =
	{
		52
	};
	
	@Override
	public boolean onCommand(int id, Player player)
	{
		if (player.isRegisteredOnEvent())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		else if (player.isJailed())
		{
			player.sendMessage("You cannot use this function while you are jailed.");
			return false;
		}
		
		if (FactionSystemConfig.FACTION_SYSTEM_ENABLED && !player.isGood() && !player.isEvil())
		{
			player.sendMessage("You cannot use this function while you are neutral.");
			return false;
		}
		
		final int unstuckTimer = (player.getAccessLevel().isGm() ? 1000 : PlayerConfig.UNSTUCK_INTERVAL * 1000);
		if (player.isInOlympiadMode())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_THAT_SKILL_IN_A_GRAND_OLYMPIAD_GAMES_MATCH);
			return false;
		}
		
		if (player.isCastingNow() || player.isMovementDisabled() || player.isMuted() || player.isAlikeDead() || player.inObserverMode())
		{
			return false;
		}
		
		player.forceIsCasting(GameTimeTaskManager.getInstance().getGameTicks() + (unstuckTimer / GameTimeTaskManager.MILLIS_IN_TICK));
		
		final Skill escape = SkillData.getInstance().getSkill(2099, 1); // 5 minutes escape
		final Skill gmEscape = SkillData.getInstance().getSkill(2100, 1); // 1 second escape
		if (player.getAccessLevel().isGm())
		{
			if (gmEscape != null)
			{
				player.doCast(gmEscape);
				return true;
			}
			
			player.sendMessage("You use Escape: 1 second.");
		}
		else if ((PlayerConfig.UNSTUCK_INTERVAL == 300) && (escape != null))
		{
			// If unstuck is default (5min), send retail system message.
			player.sendPacket(SystemMessageId.YOU_ARE_STUCK_YOU_WILL_BE_TRANSPORTED_TO_THE_NEAREST_VILLAGE_IN_FIVE_MINUTES);
			player.stopMove(null);
			player.abortCast();
			player.doCast(escape);
			return true;
		}
		else
		{
			if (PlayerConfig.UNSTUCK_INTERVAL > 100)
			{
				player.sendMessage("You use Escape: " + (unstuckTimer / 60000) + " minutes.");
			}
			else
			{
				player.sendMessage("You use Escape: " + (unstuckTimer / 1000) + " seconds.");
			}
		}
		
		player.getAI().setIntention(Intention.IDLE);
		
		// SoE Animation section
		player.setTarget(player);
		player.disableAllSkills();
		
		player.broadcastSkillPacket(new MagicSkillUse(player, 1050, 1, unstuckTimer, 0), player);
		player.sendPacket(new SetupGauge(player.getObjectId(), 0, unstuckTimer));
		// End SoE Animation section
		
		// continue execution later
		player.setSkillCast(ThreadPool.schedule(new EscapeFinalizer(player), unstuckTimer));
		return true;
	}
	
	private static class EscapeFinalizer implements Runnable
	{
		private final Player _player;
		
		protected EscapeFinalizer(Player player)
		{
			_player = player;
		}
		
		@Override
		public void run()
		{
			if (_player.isDead())
			{
				return;
			}
			
			_player.setIn7sDungeon(false);
			_player.enableAllSkills();
			_player.setCastingNow(false);
			_player.setInstanceId(0);
			_player.teleToLocation(TeleportWhereType.TOWN);
		}
	}
	
	@Override
	public int[] getCommandList()
	{
		return COMMAND_IDS;
	}
}
