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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.cache.RelationCache;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CharInfo;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.RelationChanged;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @version $Revision: 1.2 $ $Date: 2004/06/27 08:12:59 $
 */
public class Broadcast
{
	private static final Logger LOGGER = Logger.getLogger(Broadcast.class.getName());
	
	/**
	 * Send a packet to all Player in the _KnownPlayers of the Creature that have the Character targeted.<br>
	 * <br>
	 * <b><u>Concept</u>:</b><br>
	 * <br>
	 * Player in the detection area of the Creature are identified in <b>_knownPlayers</b>.<br>
	 * In order to inform other players of state modification on the Creature, server just need to go through _knownPlayers to send Server->Client Packet<br>
	 * <font color=#FF0000><b><u>Caution</u>: This method DOESN'T SEND Server->Client packet to this Creature (to do this use method toSelfAndKnownPlayers)</b></font>
	 * @param creature
	 * @param packet
	 */
	public static void toPlayersTargettingMyself(Creature creature, ServerPacket packet)
	{
		packet.sendInBroadcast();
		World.getInstance().forEachVisibleObject(creature, Player.class, player ->
		{
			if (player.getTarget() == creature)
			{
				player.sendPacket(packet);
			}
		});
	}
	
	/**
	 * Send a packet to all Player in the _KnownPlayers of the Creature.<br>
	 * <br>
	 * <b><u>Concept</u>:</b><br>
	 * <br>
	 * Player in the detection area of the Creature are identified in <b>_knownPlayers</b>.<br>
	 * In order to inform other players of state modification on the Creature, server just need to go through _knownPlayers to send Server->Client Packet<br>
	 * <font color=#FF0000><b><u>Caution</u>: This method DOESN'T SEND Server->Client packet to this Creature (to do this use method toSelfAndKnownPlayers)</b></font>
	 * @param creature
	 * @param packet
	 */
	public static void toKnownPlayers(Creature creature, ServerPacket packet)
	{
		packet.sendInBroadcast();
		World.getInstance().forEachVisibleObject(creature, Player.class, player ->
		{
			try
			{
				player.sendPacket(packet);
				if ((packet instanceof CharInfo) && creature.isPlayer())
				{
					final Player broadcaster = creature.asPlayer();
					final int relation = broadcaster.getRelation(player);
					final boolean isAutoAttackable = broadcaster.isAutoAttackable(player);
					final RelationCache cache = broadcaster.getKnownRelations().get(player.getObjectId());
					if ((cache == null) || (cache.getRelation() != relation) || (cache.isAutoAttackable() != isAutoAttackable))
					{
						player.sendPacket(new RelationChanged(broadcaster, relation, isAutoAttackable));
						if (broadcaster.hasSummon())
						{
							player.sendPacket(new RelationChanged(broadcaster.getSummon(), relation, isAutoAttackable));
						}
						
						broadcaster.getKnownRelations().put(player.getObjectId(), new RelationCache(relation, isAutoAttackable));
					}
				}
			}
			catch (NullPointerException e)
			{
				LOGGER.log(Level.WARNING, e.getMessage(), e);
			}
		});
	}
	
	/**
	 * Send a packet to all Player in the _KnownPlayers (in the specified radius) of the Creature.<br>
	 * <br>
	 * <b><u>Concept</u>:</b><br>
	 * <br>
	 * Player in the detection area of the Creature are identified in <b>_knownPlayers</b>.<br>
	 * In order to inform other players of state modification on the Creature, server just needs to go through _knownPlayers to send Server->Client Packet and check the distance between the targets.<br>
	 * <font color=#FF0000><b><u>Caution</u>: This method DOESN'T SEND Server->Client packet to this Creature (to do this use method toSelfAndKnownPlayers)</b></font>
	 * @param creature
	 * @param packet
	 * @param radiusValue
	 */
	public static void toKnownPlayersInRadius(Creature creature, ServerPacket packet, int radiusValue)
	{
		int radius = radiusValue;
		if (radius < 0)
		{
			radius = 1500;
		}
		
		packet.sendInBroadcast();
		World.getInstance().forEachVisibleObjectInRange(creature, Player.class, radius, player -> player.sendPacket(packet));
	}
	
	/**
	 * Send a packet to all Player in the _KnownPlayers of the Creature and to the specified character.<br>
	 * <br>
	 * <b><u>Concept</u>:</b><br>
	 * <br>
	 * Player in the detection area of the Creature are identified in <b>_knownPlayers</b>.<br>
	 * In order to inform other players of state modification on the Creature, server just need to go through _knownPlayers to send Server->Client Packet
	 * @param creature
	 * @param packet
	 */
	public static void toSelfAndKnownPlayers(Creature creature, ServerPacket packet)
	{
		packet.sendInBroadcast();
		if (creature.isPlayer())
		{
			creature.sendPacket(packet);
		}
		
		toKnownPlayers(creature, packet);
	}
	
	// To improve performance we are comparing values of radius^2 instead of calculating sqrt all the time
	public static void toSelfAndKnownPlayersInRadius(Creature creature, ServerPacket packet, int radiusValue)
	{
		int radius = radiusValue;
		if (radius < 0)
		{
			radius = 600;
		}
		
		packet.sendInBroadcast();
		if (creature.isPlayer())
		{
			creature.sendPacket(packet);
		}
		
		World.getInstance().forEachVisibleObjectInRange(creature, Player.class, radius, player -> player.sendPacket(packet));
	}
	
	/**
	 * Send a packet to all Player present in the world.<br>
	 * <br>
	 * <b><u>Concept</u>:</b><br>
	 * <br>
	 * In order to inform other players of state modification on the Creature, server just need to go through _allPlayers to send Server->Client Packet<br>
	 * <font color=#FF0000><b><u>Caution</u>: This method DOESN'T SEND Server->Client packet to this Creature (to do this use method toSelfAndKnownPlayers)</b></font>
	 * @param packet
	 */
	public static void toAllOnlinePlayers(ServerPacket packet)
	{
		packet.sendInBroadcast();
		for (Player player : World.getInstance().getPlayers())
		{
			if (player.isOnline())
			{
				player.sendPacket(packet);
			}
		}
	}
	
	public static void toAllOnlinePlayers(String text)
	{
		toAllOnlinePlayers(text, false);
	}
	
	public static void toAllOnlinePlayers(String text, boolean isCritical)
	{
		toAllOnlinePlayers(new CreatureSay(null, isCritical ? ChatType.CRITICAL_ANNOUNCE : ChatType.ANNOUNCEMENT, "", text));
	}
	
	public static void toPlayersInInstance(ServerPacket packet, int instanceId)
	{
		for (Player player : World.getInstance().getPlayers())
		{
			if (player.isOnline() && (player.getInstanceId() == instanceId))
			{
				player.sendPacket(packet);
			}
		}
	}
	
	public static void toPlayersInInstance(String text, int instanceId)
	{
		for (Player player : World.getInstance().getPlayers())
		{
			if (player.isOnline() && (player.getInstanceId() == instanceId))
			{
				player.sendMessage(text);
			}
		}
	}
	
	public static void toAllOnlinePlayersOnScreen(String text)
	{
		toAllOnlinePlayers(new ExShowScreenMessage(text, 10000));
	}
	
	/**
	 * Send a packet to all players in a specific zone type.
	 * @param <T> ZoneType.
	 * @param zoneType : The zone type to send packets.
	 * @param packets : The packets to send.
	 */
	public static <T extends ZoneType> void toAllPlayersInZoneType(Class<T> zoneType, ServerPacket... packets)
	{
		for (ServerPacket packet : packets)
		{
			packet.sendInBroadcast();
		}
		
		for (ZoneType zone : ZoneManager.getInstance().getAllZones(zoneType))
		{
			for (Creature creature : zone.getCharactersInside())
			{
				if (creature == null)
				{
					continue;
				}
				
				for (ServerPacket packet : packets)
				{
					creature.sendPacket(packet);
				}
			}
		}
	}
}
