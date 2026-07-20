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
package ai.others.WyvernManager;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.MountType;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.sevensigns.SevenSigns;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.model.siege.clanhalls.SiegableHall;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.util.ArrayUtil;

/**
 * Wyvern Manager
 * @author xban1x
 */
public class WyvernManager extends Script
{
	private enum ManagerType
	{
		CASTLE,
		CLAN_HALL,
	}
	
	// Misc
	private static final int CRYSTAL_B_GRADE = 1460;
	private static final int WYVERN = 12621;
	private static final int WYVERN_FEE = 25;
	private static final int STRIDER_LEVEL = 55;
	private static final int[] STRIDERS =
	{
		12526,
		12527,
		12528,
		16038,
		16039,
		16040,
		16068,
		13197
	};
	
	// NPCS
	private static final Map<Integer, ManagerType> MANAGERS = new HashMap<>();
	static
	{
		MANAGERS.put(35101, ManagerType.CASTLE);
		MANAGERS.put(35143, ManagerType.CASTLE);
		MANAGERS.put(35185, ManagerType.CASTLE);
		MANAGERS.put(35227, ManagerType.CASTLE);
		MANAGERS.put(35275, ManagerType.CASTLE);
		MANAGERS.put(35317, ManagerType.CASTLE);
		MANAGERS.put(35364, ManagerType.CASTLE);
		MANAGERS.put(35510, ManagerType.CASTLE);
		MANAGERS.put(35536, ManagerType.CASTLE);
		MANAGERS.put(35556, ManagerType.CASTLE);
		MANAGERS.put(35419, ManagerType.CLAN_HALL);
		MANAGERS.put(35638, ManagerType.CLAN_HALL);
	}
	
	private WyvernManager()
	{
		addStartNpc(MANAGERS.keySet());
		addTalkId(MANAGERS.keySet());
		addFirstTalkId(MANAGERS.keySet());
	}
	
	private String mountWyvern(Npc npc, Player player)
	{
		if (player.isMounted() && (player.getMountLevel() >= STRIDER_LEVEL) && ArrayUtil.contains(STRIDERS, player.getMountNpcId()))
		{
			if (isOwnerClan(npc, player) && (getQuestItemsCount(player, CRYSTAL_B_GRADE) >= WYVERN_FEE))
			{
				takeItems(player, CRYSTAL_B_GRADE, WYVERN_FEE);
				player.dismount();
				player.mount(WYVERN, 0, true);
				
				return "wyvernmanager-04.html";
			}
			
			return replacePart(player, "wyvernmanager-06.html");
		}
		
		if (player.getMountType() != MountType.STRIDER)
		{
			player.sendPacket(SystemMessageId.YOU_MAY_ONLY_RIDE_A_WYVERN_WHILE_YOU_RE_RIDING_A_STRIDER);
		}
		
		return replacePart(player, "wyvernmanager-05.html");
	}
	
	private boolean isOwnerClan(Npc npc, Player player)
	{
		if (!player.isClanLeader())
		{
			return false;
		}
		
		final ManagerType type = MANAGERS.get(npc.getId());
		switch (type)
		{
			case CASTLE:
			{
				if (npc.getCastle() != null)
				{
					return player.getClanId() == npc.getCastle().getOwnerId();
				}
				
				return false;
			}
			case CLAN_HALL:
			{
				if (npc.getConquerableHall() != null)
				{
					return player.getClanId() == npc.getConquerableHall().getOwnerId();
				}
				
				return false;
			}
			default:
			{
				return false;
			}
		}
	}
	
	private boolean isInSiege(Npc npc)
	{
		final ManagerType type = MANAGERS.get(npc.getId());
		switch (type)
		{
			case CASTLE:
			{
				return npc.getCastle().getZone().isActive();
			}
			case CLAN_HALL:
			{
				final SiegableHall hall = npc.getConquerableHall();
				return (hall != null) ? hall.isInSiege() : npc.getCastle().getSiege().isInProgress();
			}
			default:
			{
				return false;
			}
		}
	}
	
	private String getResidenceName(Npc npc)
	{
		final ManagerType type = MANAGERS.get(npc.getId());
		switch (type)
		{
			case CASTLE:
			{
				return npc.getCastle().getName();
			}
			case CLAN_HALL:
			{
				return npc.getConquerableHall().getName();
			}
			default:
			{
				return null;
			}
		}
	}
	
	private String replaceAll(Npc npc, Player player)
	{
		return replacePart(player, "wyvernmanager-01.html").replace("%residence_name%", getResidenceName(npc));
	}
	
	private String replacePart(Player player, String htmlFile)
	{
		return getHtm(player, htmlFile).replace("%wyvern_fee%", String.valueOf(WYVERN_FEE)).replace("%strider_level%", String.valueOf(STRIDER_LEVEL));
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "Return":
			{
				if (!isOwnerClan(npc, player))
				{
					htmltext = "wyvernmanager-02.html";
				}
				else if (FeatureConfig.ALLOW_WYVERN_ALWAYS)
				{
					htmltext = replaceAll(npc, player);
				}
				else
				{
					final ManagerType type = MANAGERS.get(npc.getId());
					if ((type == ManagerType.CASTLE) && SevenSigns.getInstance().isSealValidationPeriod() && (SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DUSK))
					{
						htmltext = "wyvernmanager-dusk.html";
					}
					else
					{
						htmltext = replaceAll(npc, player);
					}
				}
				break;
			}
			case "Help":
			{
				final ManagerType type = MANAGERS.get(npc.getId());
				htmltext = type == ManagerType.CASTLE ? replacePart(player, "wyvernmanager-03.html") : replacePart(player, "wyvernmanager-03b.html");
				break;
			}
			case "RideWyvern":
			{
				if (!FeatureConfig.ALLOW_WYVERN_ALWAYS)
				{
					// If in siege and config blocks wyvern riding, allow only castle lord.
					if (!FeatureConfig.ALLOW_WYVERN_DURING_SIEGE && player.isClanLeader() && (player.isInSiege() || isInSiege(npc)))
					{
						// Determine if the player is the castle lord: must be clan leader and own the castle.
						final Castle castle = CastleManager.getInstance().getCastle(npc);
						final boolean isCastleLord = (castle != null) && (player.getClanId() == castle.getOwnerId());
						if (!isCastleLord)
						{
							player.sendMessage("You cannot summon a wyvern during a siege.");
							return null;
						}
					}
					
					// Check Seven Signs restriction.
					final ManagerType type = MANAGERS.get(npc.getId());
					if ((type == ManagerType.CASTLE) && SevenSigns.getInstance().isSealValidationPeriod() && ((SevenSigns.getInstance()).getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DUSK))
					{
						htmltext = "wyvernmanager-dusk.html";
					}
					else
					{
						htmltext = mountWyvern(npc, player);
					}
				}
				else
				{
					htmltext = mountWyvern(npc, player);
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		String htmltext = null;
		if (!isOwnerClan(npc, player))
		{
			htmltext = "wyvernmanager-02.html";
		}
		else
		{
			if (FeatureConfig.ALLOW_WYVERN_ALWAYS)
			{
				htmltext = replaceAll(npc, player);
			}
			else
			{
				final ManagerType type = MANAGERS.get(npc.getId());
				if ((type == ManagerType.CASTLE) && SevenSigns.getInstance().isSealValidationPeriod() && (SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DUSK))
				{
					htmltext = "wyvernmanager-dusk.html";
				}
				else
				{
					htmltext = replaceAll(npc, player);
				}
			}
		}
		
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new WyvernManager();
	}
}
