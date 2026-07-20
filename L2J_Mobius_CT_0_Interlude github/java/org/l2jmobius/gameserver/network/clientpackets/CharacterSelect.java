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
package org.l2jmobius.gameserver.network.clientpackets;

import java.util.logging.Logger;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.custom.DualboxCheckConfig;
import org.l2jmobius.gameserver.config.custom.FactionSystemConfig;
import org.l2jmobius.gameserver.config.custom.OfflinePlayConfig;
import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.data.sql.OfflinePlayTable;
import org.l2jmobius.gameserver.data.xml.AdminData;
import org.l2jmobius.gameserver.managers.AntiFeedManager;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.Containers;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerSelect;
import org.l2jmobius.gameserver.model.events.returns.TerminateReturn;
import org.l2jmobius.gameserver.model.punishment.PunishmentAffect;
import org.l2jmobius.gameserver.model.punishment.PunishmentType;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.ConnectionState;
import org.l2jmobius.gameserver.network.Disconnection;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.holders.CharacterInfoHolder;
import org.l2jmobius.gameserver.network.serverpackets.CharSelected;
import org.l2jmobius.gameserver.network.serverpackets.LeaveWorld;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.ServerClose;

/**
 * @version $Revision: 1.5.2.1.2.5 $ $Date: 2005/03/27 15:29:30 $
 */
public class CharacterSelect extends ClientPacket
{
	protected static final Logger LOGGER_ACCOUNTING = Logger.getLogger("accounting");
	
	// cd
	private int _charSlot;
	
	@SuppressWarnings("unused")
	private int _unk1; // new in C4
	@SuppressWarnings("unused")
	private int _unk2; // new in C4
	@SuppressWarnings("unused")
	private int _unk3; // new in C4
	@SuppressWarnings("unused")
	private int _unk4; // new in C4
	
	@Override
	protected void readImpl()
	{
		_charSlot = readInt();
		_unk1 = readShort();
		_unk2 = readInt();
		_unk3 = readInt();
		_unk4 = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final GameClient client = getClient();
		if (!client.getFloodProtectors().canSelectCharacter())
		{
			return;
		}
		
		// We should always be able to acquire the lock
		// But if we can't lock then nothing should be done (i.e. repeated packet)
		if (client.getPlayerLock().tryLock())
		{
			try
			{
				// should always be null
				// but if not then this is repeated packet and nothing should be done here
				if (client.getPlayer() == null)
				{
					final CharacterInfoHolder info = client.getCharSelection(_charSlot);
					if (info == null)
					{
						return;
					}
					
					// Disconnect offline trader.
					final Player player = World.getInstance().getPlayer(info.getObjectId());
					if (player != null)
					{
						if (OfflinePlayConfig.RESTORE_AUTO_PLAY_OFFLINERS && player.isAutoPlaying())
						{
							OfflinePlayTable.getInstance().removeOfflinePlay(player);
						}
						
						Disconnection.of(player).storeAndDelete();
					}
					
					// Banned?
					if (PunishmentManager.getInstance().hasPunishment(info.getObjectId(), PunishmentAffect.CHARACTER, PunishmentType.BAN) //
						|| PunishmentManager.getInstance().hasPunishment(client.getAccountName(), PunishmentAffect.ACCOUNT, PunishmentType.BAN) //
						|| PunishmentManager.getInstance().hasPunishment(client.getIp(), PunishmentAffect.IP, PunishmentType.BAN))
					{
						client.close(ServerClose.STATIC_PACKET);
						return;
					}
					
					// Selected character is banned (compatibility with previous versions).
					if (info.getAccessLevel() < 0)
					{
						client.close(ServerClose.STATIC_PACKET);
						return;
					}
					
					if (client.getCharSelection(_charSlot).getAccessLevel() < 100)
					{
						if ((player != null) && player.hasPremiumStatus() && (DualboxCheckConfig.DUALBOX_CHECK_MAX_OFFLINEPLAY_PREMIUM_PER_IP > 0) && !AntiFeedManager.getInstance().tryAddPlayer(AntiFeedManager.OFFLINE_PLAY, player, DualboxCheckConfig.DUALBOX_CHECK_MAX_OFFLINEPLAY_PREMIUM_PER_IP))
						{
							final NpcHtmlMessage msg = new NpcHtmlMessage();
							msg.setFile(null, "data/html/mods/IPRestriction.htm");
							msg.replace("%max%", String.valueOf(AntiFeedManager.getInstance().getLimit(client, DualboxCheckConfig.DUALBOX_CHECK_MAX_OFFLINEPLAY_PREMIUM_PER_IP)));
							client.sendPacket(msg);
							return;
						}
						else if ((DualboxCheckConfig.DUALBOX_CHECK_MAX_PLAYERS_PER_IP > 0) && !AntiFeedManager.getInstance().tryAddClient(AntiFeedManager.GAME_ID, client, DualboxCheckConfig.DUALBOX_CHECK_MAX_PLAYERS_PER_IP))
						{
							final NpcHtmlMessage msg = new NpcHtmlMessage();
							msg.setFile(null, "data/html/mods/IPRestriction.htm");
							msg.replace("%max%", String.valueOf(AntiFeedManager.getInstance().getLimit(client, DualboxCheckConfig.DUALBOX_CHECK_MAX_PLAYERS_PER_IP)));
							client.sendPacket(msg);
							return;
						}
					}
					
					if (FactionSystemConfig.FACTION_SYSTEM_ENABLED && FactionSystemConfig.FACTION_BALANCE_ONLINE_PLAYERS)
					{
						if (info.isGood() && (World.getInstance().getAllGoodPlayers().size() >= (World.getInstance().getAllEvilPlayers().size() + FactionSystemConfig.FACTION_BALANCE_PLAYER_EXCEED_LIMIT)))
						{
							final NpcHtmlMessage msg = new NpcHtmlMessage();
							msg.setFile(null, "data/html/mods/Faction/ExceededOnlineLimit.htm");
							msg.replace("%more%", FactionSystemConfig.FACTION_GOOD_TEAM_NAME);
							msg.replace("%less%", FactionSystemConfig.FACTION_EVIL_TEAM_NAME);
							client.sendPacket(msg);
							return;
						}
						
						if (info.isEvil() && (World.getInstance().getAllEvilPlayers().size() >= (World.getInstance().getAllGoodPlayers().size() + FactionSystemConfig.FACTION_BALANCE_PLAYER_EXCEED_LIMIT)))
						{
							final NpcHtmlMessage msg = new NpcHtmlMessage();
							msg.setFile(null, "data/html/mods/Faction/ExceededOnlineLimit.htm");
							msg.replace("%more%", FactionSystemConfig.FACTION_EVIL_TEAM_NAME);
							msg.replace("%less%", FactionSystemConfig.FACTION_GOOD_TEAM_NAME);
							client.sendPacket(msg);
							return;
						}
					}
					
					// load up character from disk
					final Player cha = client.load(_charSlot);
					if (cha == null)
					{
						return; // handled in GameClient
					}
					
					CharInfoTable.getInstance().addName(cha);
					
					// Prevent instant disappear of invisible GMs on login.
					if (cha.isGM() && GeneralConfig.GM_STARTUP_INVISIBLE && AdminData.getInstance().hasAccess("admin_invisible", cha.getAccessLevel()))
					{
						cha.setInvisible(true);
					}
					
					// Restore player location.
					final PlayerVariables vars = cha.getVariables();
					final String restore = vars.getString(PlayerVariables.RESTORE_LOCATION, "");
					if (!restore.isEmpty())
					{
						final String[] split = restore.split(";");
						cha.setXYZ(Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2]));
						vars.remove(PlayerVariables.RESTORE_LOCATION);
					}
					
					cha.setClient(client);
					client.setPlayer(cha);
					cha.setOnlineStatus(true, true);
					
					if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_SELECT, Containers.Players()))
					{
						final TerminateReturn terminate = EventDispatcher.getInstance().notifyEvent(new OnPlayerSelect(cha, cha.getObjectId(), cha.getName(), client), Containers.Players(), TerminateReturn.class);
						if ((terminate != null) && terminate.terminate())
						{
							Disconnection.of(cha).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
							return;
						}
					}
					
					// client.sendPacket(new SSQInfo());
					client.setConnectionState(ConnectionState.ENTERING);
					client.sendPacket(new CharSelected(cha, client.getSessionId().playOkID1));
				}
			}
			finally
			{
				client.getPlayerLock().unlock();
			}
			
			LOGGER_ACCOUNTING.info("Logged in, " + client);
		}
	}
}
