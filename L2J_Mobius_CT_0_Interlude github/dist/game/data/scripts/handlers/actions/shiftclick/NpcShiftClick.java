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
package handlers.actions.shiftclick;

import java.util.Set;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.data.xml.SpawnData;
import org.l2jmobius.gameserver.handler.IActionShiftHandler;
import org.l2jmobius.gameserver.managers.WalkingManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

import handlers.bypass.npc.NpcViewMod;

public class NpcShiftClick implements IActionShiftHandler
{
	/**
	 * Manage and Display the GM console to modify the Npc (GM only).<br>
	 * <br>
	 * <b><u>Actions (If the Player is a GM only)</u>:</b><br>
	 * <li>Set the Npc as target of the Player player (if necessary)</li>
	 * <li>Send a Server->Client packet MyTargetSelected to the Player player (display the select window)</li>
	 * <li>If Npc is autoAttackable, send a Server->Client packet StatusUpdate to the Player in order to update Npc HP bar</li>
	 * <li>Send a Server->Client NpcHtmlMessage() containing the GM console about this Npc</li><br>
	 * <font color=#FF0000><b><u>Caution</u>: Each group of Server->Client packet must be terminated by a ActionFailed packet in order to avoid that client wait an other packet</b></font><br>
	 * <br>
	 * <b><u>Example of use</u>:</b><br>
	 * <li>Client packet : Action</li>
	 */
	@Override
	public boolean onAction(Player player, WorldObject target, boolean interact)
	{
		// Check if the Player is a GM
		if (player.isGM())
		{
			// Set the target of the Player player
			player.setTarget(target);
			
			final NpcHtmlMessage html = new NpcHtmlMessage();
			html.setFile(player, "data/html/admin/npcinfo.htm");
			
			html.replace("%objid%", String.valueOf(target.getObjectId()));
			html.replace("%class%", (target.isFakePlayer() ? "Fake Player - " : "") + target.getClass().getSimpleName());
			html.replace("%race%", target.asNpc().getTemplate().getRace().toString());
			html.replace("%id%", String.valueOf(target.asNpc().getTemplate().getId()));
			html.replace("%lvl%", String.valueOf(target.asNpc().getTemplate().getLevel()));
			html.replace("%name%", target.asNpc().getTemplate().getName());
			html.replace("%tmplid%", String.valueOf(target.asNpc().getTemplate().getId()));
			html.replace("%aggro%", String.valueOf(target.isAttackable() ? target.asAttackable().getAggroRange() : 0));
			html.replace("%hp%", String.valueOf((int) target.asCreature().getCurrentHp()));
			html.replace("%hpmax%", String.valueOf(target.asCreature().getMaxHp()));
			html.replace("%mp%", String.valueOf((int) target.asCreature().getCurrentMp()));
			html.replace("%mpmax%", String.valueOf(target.asCreature().getMaxMp()));
			
			html.replace("%patk%", String.valueOf((int) target.asCreature().getPAtk(null)));
			html.replace("%matk%", String.valueOf((int) target.asCreature().getMAtk(null, null)));
			html.replace("%pdef%", String.valueOf((int) target.asCreature().getPDef(null)));
			html.replace("%mdef%", String.valueOf((int) target.asCreature().getMDef(null, null)));
			html.replace("%accu%", String.valueOf(target.asCreature().getAccuracy()));
			html.replace("%evas%", String.valueOf(target.asCreature().getEvasionRate(null)));
			html.replace("%crit%", String.valueOf(target.asCreature().getCriticalHit(null, null)));
			html.replace("%rspd%", String.valueOf((int) target.asCreature().getRunSpeed()));
			html.replace("%aspd%", String.valueOf(target.asCreature().getPAtkSpd()));
			html.replace("%cspd%", String.valueOf(target.asCreature().getMAtkSpd()));
			html.replace("%atkType%", String.valueOf(target.asCreature().getTemplate().getBaseAttackType()));
			html.replace("%atkRng%", String.valueOf(target.asCreature().getTemplate().getBaseAttackRange()));
			html.replace("%str%", String.valueOf(target.asCreature().getSTR()));
			html.replace("%dex%", String.valueOf(target.asCreature().getDEX()));
			html.replace("%con%", String.valueOf(target.asCreature().getCON()));
			html.replace("%int%", String.valueOf(target.asCreature().getINT()));
			html.replace("%wit%", String.valueOf(target.asCreature().getWIT()));
			html.replace("%men%", String.valueOf(target.asCreature().getMEN()));
			html.replace("%loc%", target.getX() + " " + target.getY() + " " + target.getZ());
			html.replace("%heading%", String.valueOf(target.asCreature().getHeading()));
			html.replace("%collision_radius%", String.valueOf(target.asCreature().getTemplate().getFCollisionRadius()));
			html.replace("%collision_height%", String.valueOf(target.asCreature().getTemplate().getFCollisionHeight()));
			html.replace("%loc2d%", String.valueOf((int) player.calculateDistance2D(target)));
			html.replace("%loc3d%", String.valueOf((int) player.calculateDistance3D(target)));
			
			if (target.asNpc().getSpawn() != null)
			{
				html.replace("%territory%", target.asNpc().getSpawn().getSpawnTerritory() == null ? "None" : target.asNpc().getSpawn().getSpawnTerritory().getName());
				if (target.asNpc().getSpawn().isTerritoryBased())
				{
					html.replace("%spawntype%", "Random");
					final Location spawnLoc = target.asNpc().getSpawn();
					html.replace("%spawn%", spawnLoc.getX() + " " + spawnLoc.getY() + " " + spawnLoc.getZ());
				}
				else
				{
					html.replace("%spawntype%", "Fixed");
					final Location spawnLoc = target.asNpc().getSpawn().getSpawnLocation() != null ? target.asNpc().getSpawn().getSpawnLocation() : target.asNpc().getSpawn();
					html.replace("%spawn%", spawnLoc.getX() + " " + spawnLoc.getY() + " " + spawnLoc.getZ());
				}
				
				if (target.asNpc().getSpawn().getRespawnMinDelay() == 0)
				{
					html.replace("%resp%", "None");
				}
				else if (target.asNpc().getSpawn().hasRespawnRandom())
				{
					html.replace("%resp%", (target.asNpc().getSpawn().getRespawnMinDelay() / 1000) + "-" + (target.asNpc().getSpawn().getRespawnMaxDelay() / 1000) + " sec");
				}
				else
				{
					html.replace("%resp%", (target.asNpc().getSpawn().getRespawnMinDelay() / 1000) + " sec");
				}
				
				final String spawnFile = SpawnData.getInstance().getSpawnFile(target.asNpc().getSpawn().getNpcSpawnTemplateId());
				html.replace("%spawnfile%", spawnFile.substring(spawnFile.lastIndexOf('\\') + 1));
			}
			else
			{
				html.replace("%spawnfile%", "<font color=FF0000>--</font>");
				html.replace("%territory%", "<font color=FF0000>--</font>");
				html.replace("%spawntype%", "<font color=FF0000>--</font>");
				html.replace("%spawn%", "<font color=FF0000>null</font>");
				html.replace("%resp%", "<font color=FF0000>--</font>");
			}
			
			if (target.asNpc().hasAI())
			{
				final Set<Integer> clans = target.asNpc().getTemplate().getClans();
				final Set<Integer> ignoreClanNpcIds = target.asNpc().getTemplate().getIgnoreClanNpcIds();
				final String clansString = clans != null ? StringUtil.implode(clans.toArray(), ", ") : "";
				final String ignoreClanNpcIdsString = ignoreClanNpcIds != null ? StringUtil.implode(ignoreClanNpcIds.toArray(), ", ") : "";
				
				html.replace("%ai_intention%", "<tr><td><table width=270 border=0 bgcolor=000000><tr><td width=100><font color=FFAA00>Intention:</font></td><td align=right width=170>" + target.asNpc().getAI().getIntention().name() + "</td></tr></table></td></tr>");
				html.replace("%ai%", "<tr><td><table width=270 border=0><tr><td width=100><font color=FFAA00>AI</font></td><td align=right width=170>" + target.asNpc().getAI().getClass().getSimpleName() + "</td></tr></table></td></tr>");
				html.replace("%ai_type%", "<tr><td><table width=270 border=0 bgcolor=000000><tr><td width=100><font color=FFAA00>AIType</font></td><td align=right width=170>" + target.asNpc().getAiType() + "</td></tr></table></td></tr>");
				html.replace("%ai_clan%", "<tr><td><table width=270 border=0><tr><td width=100><font color=FFAA00>Clan & Range:</font></td><td align=right width=170>" + clansString + " " + target.asNpc().getTemplate().getClanHelpRange() + "</td></tr></table></td></tr>");
				html.replace("%ai_enemy_clan%", "<tr><td><table width=270 border=0 bgcolor=000000><tr><td width=100><font color=FFAA00>Ignore & Range:</font></td><td align=right width=170>" + ignoreClanNpcIdsString + " " + target.asNpc().getTemplate().getAggroRange() + "</td></tr></table></td></tr>");
			}
			else
			{
				html.replace("%ai_intention%", "");
				html.replace("%ai%", "");
				html.replace("%ai_type%", "");
				html.replace("%ai_clan%", "");
				html.replace("%ai_enemy_clan%", "");
			}
			
			final String routeName = WalkingManager.getInstance().getRouteName(target.asNpc());
			if (!routeName.isEmpty())
			{
				html.replace("%route%", "<tr><td><table width=270 border=0><tr><td width=100><font color=LEVEL>Route:</font></td><td align=right width=170>" + routeName + "</td></tr></table></td></tr>");
			}
			else
			{
				html.replace("%route%", "");
			}
			
			player.sendPacket(html);
		}
		else if (NpcConfig.ALT_GAME_VIEWNPC)
		{
			if (!target.isNpc() || target.isFakePlayer())
			{
				return false;
			}
			
			player.setTarget(target);
			
			// Only show view if NPC is alive.
			final Npc npc = target.asNpc();
			if ((npc != null) && !npc.isDead())
			{
				NpcViewMod.sendNpcView(player, npc);
			}
		}
		
		return true;
	}
	
	@Override
	public InstanceType getInstanceType()
	{
		return InstanceType.Npc;
	}
}
