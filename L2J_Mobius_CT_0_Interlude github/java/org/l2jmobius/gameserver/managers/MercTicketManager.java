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
package org.l2jmobius.gameserver.managers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.Defender;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemLocation;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.siege.Castle;

/**
 * This class is similar to the SiegeGuardManager, except it handles the loading of the mercenary tickets that are dropped on castle floors by the castle lords.<br>
 * These tickets (a.k.a. badges) need to be read after each server reboot except when the server crashed in the middle of an ongoing siege.<br>
 * In addition, this class keeps track of the added tickets, in order to properly limit the number of mercenaries in each castle and the number of mercenaries from each mercenary type.<br>
 * Finally, we provide auxiliary functions to identify the castle in which each item (and its corresponding NPC) belong to, in order to help avoid mixing them up.
 * @author yellowperil, Fulminus
 */
public class MercTicketManager
{
	private static final Logger LOGGER = Logger.getLogger(MercTicketManager.class.getName());
	
	private static final Collection<Item> DROPPED_TICKETS = ConcurrentHashMap.newKeySet();
	
	// TODO: move all these values into siege.properties
	// max tickets per merc type = 10 + (castleid * 2)?
	// max ticker per castle = 40 + (castleid * 20)?
	// @formatter:off
	private static final int[] MAX_MERC_PER_TYPE =
	{
		10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, // Gludio
		15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, // Dion
		10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, // Giran
		10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, // Oren
		20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, // Aden
		20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, // Innadril
		20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, // Goddard
		20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, // Rune
		20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20 // Schuttgart
	};
	
	// TODO: not retail like: clan level 5 - 30 ticks max, level 7+ - 50 max
	protected static final int[] MERCS_MAX_PER_CASTLE =
	{
		100, // Gludio
		150, // Dion
		200, // Giran
		300, // Oren
		400, // Aden
		400, // Innadril
		400, // Goddard
		400, // Rune
		400 // Schuttgart
	};
	
	private static final int[] ITEM_IDS =
	{
		3960, 3961, 3962, 3963, 3964, 3965, 3966, 3967, 3968, 3969, 6115, 6116, 6117, 6118, 6119, 6120, 6121, 6122, 6123, 6124, 6038, 6039, 6040, 6041, 6042, 6043, 6044, 6045, 6046, 6047, 6175, 6176, 6177, 6178, 6179, 6180, 6181, 6182, 6183, 6184, 6235, 6236, 6237, 6238, 6239, 6240, 6241, 6242, 6243, 6244, 6295, 6296,	// Gludio
		3973, 3974, 3975, 3976, 3977, 3978, 3979, 3980, 3981, 3982, 6125, 6126, 6127, 6128, 6129, 6130, 6131, 6132, 6133, 6134, 6051, 6052, 6053, 6054, 6055, 6056, 6057, 6058, 6059, 6060, 6185, 6186, 6187, 6188, 6189, 6190, 6191, 6192, 6193, 6194, 6245, 6246, 6247, 6248, 6249, 6250, 6251, 6252, 6253, 6254, 6297, 6298,	// Dion
		3986, 3987, 3988, 3989, 3990, 3991, 3992, 3993, 3994, 3995, 6135, 6136, 6137, 6138, 6139, 6140, 6141, 6142, 6143, 6144, 6064, 6065, 6066, 6067, 6068, 6069, 6070, 6071, 6072, 6073, 6195, 6196, 6197, 6198, 6199, 6200, 6201, 6202, 6203, 6204, 6255, 6256, 6257, 6258, 6259, 6260, 6261, 6262, 6263, 6264, 6299, 6300,	// Giran
		3999, 4000, 4001, 4002, 4003, 4004, 4005, 4006, 4007, 4008, 6145, 6146, 6147, 6148, 6149, 6150, 6151, 6152, 6153, 6154, 6077, 6078, 6079, 6080, 6081, 6082, 6083, 6084, 6085, 6086, 6205, 6206, 6207, 6208, 6209, 6210, 6211, 6212, 6213, 6214, 6265, 6266, 6267, 6268, 6269, 6270, 6271, 6272, 6273, 6274, 6301, 6302,	// Oren
		4012, 4013, 4014, 4015, 4016, 4017, 4018, 4019, 4020, 4021, 6155, 6156, 6157, 6158, 6159, 6160, 6161, 6162, 6163, 6164, 6090, 6091, 6092, 6093, 6094, 6095, 6096, 6097, 6098, 6099, 6215, 6216, 6217, 6218, 6219, 6220, 6221, 6222, 6223, 6224, 6275, 6276, 6277, 6278, 6279, 6280, 6281, 6282, 6283, 6284, 6303, 6304,	// Aden
		5205, 5206, 5207, 5208, 5209, 5210, 5211, 5212, 5213, 5214, 6165, 6166, 6167, 6168, 6169, 6170, 6171, 6172, 6173, 6174, 6105, 6106, 6107, 6108, 6109, 6110, 6111, 6112, 6113, 6114, 6225, 6226, 6227, 6228, 6229, 6230, 6231, 6232, 6233, 6234, 6285, 6286, 6287, 6288, 6289, 6290, 6291, 6292, 6293, 6294, 6305, 6306,	// Innadril
		6779, 6780, 6781, 6782, 6783, 6784, 6785, 6786, 6787, 6788, 6802, 6803, 6804, 6805, 6806, 6807, 6808, 6809, 6810, 6811, 6792, 6793, 6794, 6795, 6796, 6797, 6798, 6799, 6800, 6801, 6812, 6813, 6814, 6815, 6816, 6817, 6818, 6819, 6820, 6821, 6822, 6823, 6824, 6825, 6826, 6827, 6828, 6829, 6830, 6831, 6832, 6833,	// Goddard
		7973, 7974, 7975, 7976, 7977, 7978, 7979, 7980, 7981, 7982, 7998, 7999, 8000, 8001, 8002, 8003, 8004, 8005, 8006, 8007, 7988, 7989, 7990, 7991, 7992, 7993, 7994, 7995, 7996, 7997, 8008, 8009, 8010, 8011, 8012, 8013, 8014, 8015, 8016, 8017, 8018, 8019, 8020, 8021, 8022, 8023, 8024, 8025, 8026, 8027, 8028, 8029,	// Rune
		7918, 7919, 7920, 7921, 7922, 7923, 7924, 7925, 7926, 7927, 7941, 7942, 7943, 7944, 7945, 7946, 7947, 7948, 7949, 7950, 7931, 7932, 7933, 7934, 7935, 7936, 7937, 7938, 7939, 7940, 7951, 7952, 7953, 7954, 7955, 7956, 7957, 7958, 7959, 7960, 7961, 7962, 7963, 7964, 7965, 7966, 7967, 7968, 7969, 7970, 7971, 7972	// Schuttgart
	};
	
	private static final int[] NPC_IDS =
	{
		35010, 35011, 35012, 35013, 35014, 35015, 35016, 35017, 35018, 35019, 35020, 35021, 35022, 35023, 35024, 35025, 35026, 35027, 35028, 35029, 35030,35031,35032,35033,35034,35035,35036,35037,35038,35039, 35040, 35041, 35042, 35043, 35044, 35045, 35046, 35047, 35048, 35049, 35050, 35051, 35052, 35053, 35054, 35055, 35056, 35057, 35058, 35059, 35060, 35061, // Gludio
		35010, 35011, 35012, 35013, 35014, 35015, 35016, 35017, 35018, 35019, 35020, 35021, 35022, 35023, 35024, 35025, 35026, 35027, 35028, 35029, 35030,35031,35032,35033,35034,35035,35036,35037,35038,35039, 35040, 35041, 35042, 35043, 35044, 35045, 35046, 35047, 35048, 35049, 35050, 35051, 35052, 35053, 35054, 35055, 35056, 35057, 35058, 35059, 35060, 35061, // Dion
		35010, 35011, 35012, 35013, 35014, 35015, 35016, 35017, 35018, 35019, 35020, 35021, 35022, 35023, 35024, 35025, 35026, 35027, 35028, 35029, 35030,35031,35032,35033,35034,35035,35036,35037,35038,35039, 35040, 35041, 35042, 35043, 35044, 35045, 35046, 35047, 35048, 35049, 35050, 35051, 35052, 35053, 35054, 35055, 35056, 35057, 35058, 35059, 35060, 35061, // Giran
		35010, 35011, 35012, 35013, 35014, 35015, 35016, 35017, 35018, 35019, 35020, 35021, 35022, 35023, 35024, 35025, 35026, 35027, 35028, 35029, 35030,35031,35032,35033,35034,35035,35036,35037,35038,35039, 35040, 35041, 35042, 35043, 35044, 35045, 35046, 35047, 35048, 35049, 35050, 35051, 35052, 35053, 35054, 35055, 35056, 35057, 35058, 35059, 35060, 35061, // Oren
		35010, 35011, 35012, 35013, 35014, 35015, 35016, 35017, 35018, 35019, 35020, 35021, 35022, 35023, 35024, 35025, 35026, 35027, 35028, 35029, 35030,35031,35032,35033,35034,35035,35036,35037,35038,35039, 35040, 35041, 35042, 35043, 35044, 35045, 35046, 35047, 35048, 35049, 35050, 35051, 35052, 35053, 35054, 35055, 35056, 35057, 35058, 35059, 35060, 35061, // Aden
		35010, 35011, 35012, 35013, 35014, 35015, 35016, 35017, 35018, 35019, 35020, 35021, 35022, 35023, 35024, 35025, 35026, 35027, 35028, 35029, 35030,35031,35032,35033,35034,35035,35036,35037,35038,35039, 35040, 35041, 35042, 35043, 35044, 35045, 35046, 35047, 35048, 35049, 35050, 35051, 35052, 35053, 35054, 35055, 35056, 35057, 35058, 35059, 35060, 35061, // Innadril
		35010, 35011, 35012, 35013, 35014, 35015, 35016, 35017, 35018, 35019, 35020, 35021, 35022, 35023, 35024, 35025, 35026, 35027, 35028, 35029, 35030,35031,35032,35033,35034,35035,35036,35037,35038,35039, 35040, 35041, 35042, 35043, 35044, 35045, 35046, 35047, 35048, 35049, 35050, 35051, 35052, 35053, 35054, 35055, 35056, 35057, 35058, 35059, 35060, 35061, // Goddard
		35010, 35011, 35012, 35013, 35014, 35015, 35016, 35017, 35018, 35019, 35020, 35021, 35022, 35023, 35024, 35025, 35026, 35027, 35028, 35029, 35030,35031,35032,35033,35034,35035,35036,35037,35038,35039, 35040, 35041, 35042, 35043, 35044, 35045, 35046, 35047, 35048, 35049, 35050, 35051, 35052, 35053, 35054, 35055, 35056, 35057, 35058, 35059, 35060, 35061, // Rune
		35010, 35011, 35012, 35013, 35014, 35015, 35016, 35017, 35018, 35019, 35020, 35021, 35022, 35023, 35024, 35025, 35026, 35027, 35028, 35029, 35030,35031,35032,35033,35034,35035,35036,35037,35038,35039, 35040, 35041, 35042, 35043, 35044, 35045, 35046, 35047, 35048, 35049, 35050, 35051, 35052, 35053, 35054, 35055, 35056, 35057, 35058, 35059, 35060, 35061, // Schuttgart
	};
	// @formatter:on
	
	private static final int GUARDIAN_TYPES_COUNT = 52;
	
	protected MercTicketManager()
	{
		load();
	}
	
	// returns the castleId for the passed ticket item id
	public int getTicketCastleId(int itemId)
	{
		for (int i = 0; i < 9; i++) // CastleID`s from 1 to 9 minus;
		{
			for (int i2 = 0; i2 < 50; i2 += 10)
			{
				// Simplified if statement;
				if ((itemId >= ITEM_IDS[i2 + (i * GUARDIAN_TYPES_COUNT)]) && (itemId <= ITEM_IDS[i2 + 9 + (i * GUARDIAN_TYPES_COUNT)]))
				{
					return i + 1;
				}
			}
			
			if ((itemId >= ITEM_IDS[50]) && (itemId <= ITEM_IDS[51]))
			{
				return i + 1;
			}
		}
		
		return -1;
	}
	
	public void reload()
	{
		DROPPED_TICKETS.clear();
		load();
	}
	
	/**
	 * Load merc tickets into the world.
	 */
	private void load()
	{
		try (Connection con = DatabaseFactory.getConnection();
			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("SELECT * FROM castle_siege_guards Where isHired = 1"))
		{
			int npcId;
			int itemId;
			int x;
			int y;
			int z;
			final int[] mercPlaced = new int[20];
			
			// start index to begin the search for the itemId corresponding to this NPC
			// this will help with:
			// a) skip unnecessary iterations in the search loop
			// b) avoid finding the wrong itemId whenever tickets of different spawn the same npc!
			int startindex = 0;
			
			while (rs.next())
			{
				npcId = rs.getInt("npcId");
				x = rs.getInt("x");
				y = rs.getInt("y");
				z = rs.getInt("z");
				final Castle castle = CastleManager.getInstance().getCastle(x, y, z);
				if (castle != null)
				{
					if (mercPlaced[castle.getResidenceId() - 1] >= MERCS_MAX_PER_CASTLE[castle.getResidenceId() - 1])
					{
						continue;
					}
					
					startindex = GUARDIAN_TYPES_COUNT * (castle.getResidenceId() - 1);
					mercPlaced[castle.getResidenceId() - 1] += 1;
				}
				
				// find the FIRST ticket itemId with spawns the saved NPC in the saved location
				for (int i = startindex; i < (startindex + GUARDIAN_TYPES_COUNT); i++)
				{
					if (NPC_IDS[i] == npcId) // Find the index of the item used
					{
						// only handle tickets if a siege is not ongoing in this npc's castle
						if ((castle != null) && !castle.getSiege().isInProgress())
						{
							itemId = ITEM_IDS[i];
							
							// create the ticket in the gameworld
							final Item dropticket = new Item(IdManager.getInstance().getNextId(), itemId);
							dropticket.setItemLocation(ItemLocation.VOID);
							dropticket.dropMe(null, x, y, z);
							dropticket.setDropTime(0); // avoids it from being removed by the auto item destroyer
							World.getInstance().addObject(dropticket);
							DROPPED_TICKETS.add(dropticket);
						}
						break;
					}
				}
			}
			
			rs.close();
			s.close();
			
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + DROPPED_TICKETS.size() + " Mercenary Tickets");
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception: loadMercenaryData(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Checks if the passed item has reached the limit of number of dropped tickets that this SPECIFIC item may have in its castle
	 * @param itemId
	 * @return
	 */
	public boolean isAtTypeLimit(int itemId)
	{
		int limit = -1;
		
		// find the max value for this item
		for (int i = 0; i < ITEM_IDS.length; i++)
		{
			if (ITEM_IDS[i] == itemId) // Find the index of the item used
			{
				limit = MAX_MERC_PER_TYPE[i];
				break;
			}
		}
		
		if (limit <= 0)
		{
			return true;
		}
		
		int count = 0;
		for (Item ticket : DROPPED_TICKETS)
		{
			if ((ticket != null) && (ticket.getId() == itemId))
			{
				count++;
			}
		}
		
		return count >= limit;
	}
	
	/**
	 * Checks if the passed item belongs to a castle which has reached its limit of number of dropped tickets.
	 * @param itemId
	 * @return
	 */
	public boolean isAtCasleLimit(int itemId)
	{
		final int castleId = getTicketCastleId(itemId);
		if (castleId <= 0)
		{
			return true;
		}
		
		final int limit = MERCS_MAX_PER_CASTLE[castleId - 1];
		if (limit <= 0)
		{
			return true;
		}
		
		int count = 0;
		for (Item ticket : DROPPED_TICKETS)
		{
			if ((ticket != null) && (getTicketCastleId(ticket.getId()) == castleId))
			{
				count++;
			}
		}
		
		return count >= limit;
	}
	
	public int getMaxAllowedMerc(int castleId)
	{
		return MERCS_MAX_PER_CASTLE[castleId - 1];
	}
	
	public boolean isTooCloseToAnotherTicket(int x, int y, int z)
	{
		for (Item item : DROPPED_TICKETS)
		{
			final double dx = x - item.getX();
			final double dy = y - item.getY();
			final double dz = z - item.getZ();
			if (((dx * dx) + (dy * dy) + (dz * dz)) < (25 * 25))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * addTicket actions 1) find the npc that needs to be saved in the mercenary spawns, given this item 2) Use the passed character's location info to add the spawn 3) create a copy of the item to drop in the world returns the id of the mercenary npc that was added to the spawn returns -1 if this
	 * fails.
	 * @param itemId
	 * @param player
	 * @return
	 */
	public int addTicket(int itemId, Player player)
	{
		final int x = player.getX();
		final int y = player.getY();
		final int z = player.getZ();
		final int heading = player.getHeading();
		final Castle castle = CastleManager.getInstance().getCastle(player);
		if (castle == null)
		{
			return -1;
		}
		
		for (int i = 0; i < ITEM_IDS.length; i++)
		{
			if (ITEM_IDS[i] == itemId) // Find the index of the item used
			{
				spawnMercenary(NPC_IDS[i], x, y, z, 3000);
				
				// Hire merc for this castle. NpcId is at the same index as the item used.
				castle.getSiege().getSiegeGuardManager().hireMerc(x, y, z, heading, NPC_IDS[i]);
				
				// create the ticket in the gameworld
				final Item dropticket = new Item(IdManager.getInstance().getNextId(), itemId);
				dropticket.setItemLocation(ItemLocation.VOID);
				dropticket.dropMe(null, x, y, z);
				dropticket.setDropTime(0); // avoids it from beeing removed by the auto item destroyer
				World.getInstance().addObject(dropticket); // add to the world
				
				// and keep track of this ticket in the list
				DROPPED_TICKETS.add(dropticket);
				
				return NPC_IDS[i];
			}
		}
		
		return -1;
	}
	
	private void spawnMercenary(int npcId, int x, int y, int z, int despawnDelay)
	{
		final NpcTemplate template = NpcData.getInstance().getTemplate(npcId);
		if (template == null)
		{
			return;
		}
		
		final Defender npc = new Defender(template);
		npc.setCurrentHpMp(npc.getMaxHp(), npc.getMaxMp());
		npc.setDecayed(false);
		npc.spawnMe(x, y, z + 20);
		if (despawnDelay > 0)
		{
			npc.scheduleDespawn(despawnDelay);
		}
	}
	
	/**
	 * Delete all tickets from a castle; remove the items from the world and remove references to them from this class
	 * @param castleId
	 */
	public void deleteTickets(int castleId)
	{
		for (Item item : DROPPED_TICKETS)
		{
			if ((item != null) && (getTicketCastleId(item.getId()) == castleId))
			{
				item.decayMe();
				World.getInstance().removeObject(item);
				DROPPED_TICKETS.remove(item);
			}
		}
	}
	
	/**
	 * remove a single ticket and its associated spawn from the world (used when the castle lord picks up a ticket, for example)
	 * @param item
	 */
	public void removeTicket(Item item)
	{
		final int itemId = item.getId();
		int npcId = -1;
		
		// find the FIRST ticket itemId with spawns the saved NPC in the saved location
		for (int i = 0; i < ITEM_IDS.length; i++)
		{
			if (ITEM_IDS[i] == itemId) // Find the index of the item used
			{
				npcId = NPC_IDS[i];
				break;
			}
		}
		
		// find the castle where this item is
		final Castle castle = CastleManager.getInstance().getCastleById(getTicketCastleId(itemId));
		if ((npcId > 0) && (castle != null))
		{
			(new SiegeGuardManager(castle)).removeMerc(npcId, item.getX(), item.getY(), item.getZ());
		}
		
		DROPPED_TICKETS.remove(item);
	}
	
	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
	
	public Collection<Item> getDroppedTickets()
	{
		return DROPPED_TICKETS;
	}
	
	/**
	 * Gets the single instance of {@code MercTicketManager}.
	 * @return single instance of {@code MercTicketManager}
	 */
	public static MercTicketManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final MercTicketManager INSTANCE = new MercTicketManager();
	}
}
