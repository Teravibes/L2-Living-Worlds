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
package handlers.chat.commands.admin;

import java.util.StringTokenizer;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.handler.AdminCommandHandler;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;

/**
 * A retail-like implementation of //gmspeed builder command.
 * @author lord_rex
 */
public class AdminGmSpeed implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_gmspeed",
	};
	
	@Override
	public boolean onCommand(String command, Player player)
	{
		final StringTokenizer st = new StringTokenizer(command);
		final String cmd = st.nextToken();
		if (cmd.equals("admin_gmspeed"))
		{
			if (!st.hasMoreTokens())
			{
				player.sendSysMessage("//gmspeed [0...10]");
				return false;
			}
			
			final String token = st.nextToken();
			
			// Rollback feature for old custom way, in order to make everyone happy.
			if (GeneralConfig.USE_SUPER_HASTE_AS_GM_SPEED)
			{
				AdminCommandHandler.getInstance().onCommand(player, AdminSuperHaste.ADMIN_COMMANDS[0] + " " + token, false);
				return true;
			}
			
			if (!StringUtil.isDouble(token))
			{
				player.sendSysMessage("//gmspeed [0...10]");
				return false;
			}
			
			final double runSpeedBoost = Double.parseDouble(token);
			if ((runSpeedBoost < 0) || (runSpeedBoost > 10))
			{
				// Custom limit according to SDW's request - real retail limit is unknown.
				player.sendSysMessage("//gmspeed [0...10]");
				return false;
			}
			
			final Creature targetCharacter;
			final WorldObject target = player.getTarget();
			if ((target != null) && target.isCreature())
			{
				targetCharacter = target.asCreature();
			}
			else
			{
				// If there is no target, let's use the command executer.
				targetCharacter = player;
			}
			
			targetCharacter.getStat().setGmSpeedMultiplier(runSpeedBoost > 0 ? runSpeedBoost : 1);
			if (targetCharacter.isPlayer())
			{
				targetCharacter.asPlayer().broadcastUserInfo();
			}
			else
			{
				targetCharacter.broadcastInfo();
			}
			
			player.sendSysMessage("[" + targetCharacter.getName() + "] speed is [" + (runSpeedBoost * 100) + "0]% fast.");
		}
		
		return true;
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
