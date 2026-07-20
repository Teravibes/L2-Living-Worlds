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
package org.l2jmobius.gameserver.network.serverpackets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.config.custom.MultilingualSupportConfig;
import org.l2jmobius.gameserver.config.custom.OfflinePlayConfig;
import org.l2jmobius.gameserver.config.custom.OfflineTradeConfig;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.network.Disconnection;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.holders.CharacterInfoHolder;

public class CharSelectionInfo extends ServerPacket
{
	private static final Logger LOGGER = Logger.getLogger(CharSelectionInfo.class.getName());
	
	private final String _loginName;
	private final int _sessionId;
	private int _activeId;
	private final List<CharacterInfoHolder> _characterPackages;
	
	/**
	 * Constructor for CharSelectionInfo.
	 * @param loginName
	 * @param sessionId
	 */
	public CharSelectionInfo(String loginName, int sessionId)
	{
		_sessionId = sessionId;
		_loginName = loginName;
		_characterPackages = loadCharacterSelectInfo(_loginName);
		_activeId = -1;
	}
	
	public CharSelectionInfo(String loginName, int sessionId, int activeId)
	{
		_sessionId = sessionId;
		_loginName = loginName;
		_characterPackages = loadCharacterSelectInfo(_loginName);
		_activeId = activeId;
	}
	
	public List<CharacterInfoHolder> getCharInfo()
	{
		return _characterPackages;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.CHAR_SELECT_INFO.writeId(this, buffer);
		final int size = _characterPackages.size();
		buffer.writeInt(size); // Created character count
		long lastAccess = 0;
		if (_activeId == -1)
		{
			for (int i = 0; i < size; i++)
			{
				if (lastAccess < _characterPackages.get(i).getLastAccess())
				{
					lastAccess = _characterPackages.get(i).getLastAccess();
					_activeId = i;
				}
			}
		}
		
		for (int i = 0; i < size; i++)
		{
			final CharacterInfoHolder charInfoPackage = _characterPackages.get(i);
			buffer.writeString(charInfoPackage.getName()); // Character name
			buffer.writeInt(charInfoPackage.getObjectId()); // Character ID
			buffer.writeString(_loginName); // Account name
			buffer.writeInt(_sessionId); // Account ID
			buffer.writeInt(charInfoPackage.getClanId()); // Clan ID
			buffer.writeInt(0); // Builder level
			buffer.writeInt(charInfoPackage.getSex()); // Sex
			buffer.writeInt(charInfoPackage.getRace()); // Race
			buffer.writeInt(charInfoPackage.getBaseClassId());
			buffer.writeInt(1); // GameServerName
			buffer.writeInt(0);
			buffer.writeInt(0);
			buffer.writeInt(0);
			buffer.writeDouble(charInfoPackage.getCurrentHp());
			buffer.writeDouble(charInfoPackage.getCurrentMp());
			buffer.writeInt((int) charInfoPackage.getSp());
			buffer.writeLong(charInfoPackage.getExp());
			buffer.writeInt(charInfoPackage.getLevel());
			buffer.writeInt(charInfoPackage.getKarma());
			buffer.writeInt(0);
			buffer.writeInt(0);
			buffer.writeInt(0);
			buffer.writeInt(0);
			buffer.writeInt(0);
			buffer.writeInt(0);
			buffer.writeInt(0);
			buffer.writeInt(0);
			buffer.writeInt(0);
			buffer.writeInt(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_UNDER));
			buffer.writeInt(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_REAR));
			buffer.writeInt(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_LEAR));
			buffer.writeInt(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_NECK));
			buffer.writeInt(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_RFINGER));
			buffer.writeInt(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_LFINGER));
			buffer.writeInt(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_HEAD));
			buffer.writeInt(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_RHAND));
			buffer.writeInt(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_LHAND));
			buffer.writeInt(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_GLOVES));
			buffer.writeInt(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_CHEST));
			buffer.writeInt(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_LEGS));
			buffer.writeInt(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_FEET));
			buffer.writeInt(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_CLOAK));
			buffer.writeInt(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_RHAND));
			buffer.writeInt(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_HAIR));
			buffer.writeInt(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_HAIR2));
			buffer.writeInt(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_UNDER));
			buffer.writeInt(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_REAR));
			buffer.writeInt(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_LEAR));
			buffer.writeInt(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_NECK));
			buffer.writeInt(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_RFINGER));
			buffer.writeInt(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_LFINGER));
			buffer.writeInt(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_HEAD));
			buffer.writeInt(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_RHAND));
			buffer.writeInt(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_LHAND));
			buffer.writeInt(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_GLOVES));
			buffer.writeInt(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_CHEST));
			buffer.writeInt(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_LEGS));
			buffer.writeInt(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_FEET));
			buffer.writeInt(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_CLOAK));
			buffer.writeInt(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_RHAND));
			buffer.writeInt(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_HAIR));
			buffer.writeInt(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_HAIR2));
			buffer.writeInt(charInfoPackage.getHairStyle());
			buffer.writeInt(charInfoPackage.getHairColor());
			buffer.writeInt(charInfoPackage.getFace());
			buffer.writeDouble(charInfoPackage.getMaxHp()); // Maximum HP
			buffer.writeDouble(charInfoPackage.getMaxMp()); // Maximum MP
			
			if (charInfoPackage.getAccessLevel() > -1)
			{
				// Deleted.
				if (charInfoPackage.getDeleteTimer() > 0)
				{
					buffer.writeInt((int) ((charInfoPackage.getDeleteTimer() - System.currentTimeMillis()) / 1000));
				}
				else // Normal.
				{
					buffer.writeInt(0);
				}
			}
			else // Banned.
			{
				buffer.writeInt(-1);
			}
			
			buffer.writeInt(charInfoPackage.getClassId());
			buffer.writeInt(i == _activeId);
			buffer.writeByte(Math.min(charInfoPackage.getEnchantEffect(), 127));
			buffer.writeInt(charInfoPackage.getAugmentationId());
		}
	}
	
	private static List<CharacterInfoHolder> loadCharacterSelectInfo(String loginName)
	{
		CharacterInfoHolder charInfopackage;
		final List<CharacterInfoHolder> characterList = new LinkedList<>();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT * FROM characters WHERE account_name=? ORDER BY createDate"))
		{
			statement.setString(1, loginName);
			try (ResultSet charList = statement.executeQuery())
			{
				while (charList.next()) // fills the package
				{
					charInfopackage = restoreChar(charList);
					if (charInfopackage != null)
					{
						characterList.add(charInfopackage);
						
						// Disconnect offline trader.
						if (OfflineTradeConfig.OFFLINE_DISCONNECT_SAME_ACCOUNT)
						{
							final Player player = World.getInstance().getPlayer(charInfopackage.getObjectId());
							if ((player != null) && player.isInStoreMode())
							{
								Disconnection.of(player).storeAndDelete();
								continue;
							}
						}
						
						// Disconnect offline play.
						if (OfflinePlayConfig.OFFLINE_PLAY_DISCONNECT_SAME_ACCOUNT)
						{
							final Player player = World.getInstance().getPlayer(charInfopackage.getObjectId());
							if ((player != null) && player.isOfflinePlay())
							{
								Disconnection.of(player).storeAndDelete();
							}
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not restore char info: " + e.getMessage(), e);
		}
		
		return characterList;
	}
	
	private static void loadCharacterSubclassInfo(CharacterInfoHolder charInfopackage, int objectId, int activeClassId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT exp, sp, level FROM character_subclasses WHERE charId=? AND class_id=? ORDER BY charId"))
		{
			statement.setInt(1, objectId);
			statement.setInt(2, activeClassId);
			try (ResultSet charList = statement.executeQuery())
			{
				if (charList.next())
				{
					charInfopackage.setExp(charList.getLong("exp"));
					charInfopackage.setSp(charList.getInt("sp"));
					charInfopackage.setLevel(charList.getInt("level"));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not restore char subclass info: " + e.getMessage(), e);
		}
	}
	
	private static CharacterInfoHolder restoreChar(ResultSet chardata) throws Exception
	{
		final int objectId = chardata.getInt("charId");
		final String name = chardata.getString("char_name");
		
		// See if the char must be deleted
		final long deletetime = chardata.getLong("deletetime");
		if ((deletetime > 0) && (System.currentTimeMillis() > deletetime))
		{
			final Clan clan = ClanTable.getInstance().getClan(chardata.getInt("clanid"));
			if (clan != null)
			{
				clan.removeClanMember(objectId, 0);
			}
			
			GameClient.deleteCharByObjId(objectId);
			return null;
		}
		
		final CharacterInfoHolder charInfopackage = new CharacterInfoHolder(objectId, name);
		charInfopackage.setAccessLevel(chardata.getInt("accesslevel"));
		charInfopackage.setLevel(chardata.getInt("level"));
		charInfopackage.setMaxHp(chardata.getInt("maxhp"));
		charInfopackage.setCurrentHp(chardata.getDouble("curhp"));
		charInfopackage.setMaxMp(chardata.getInt("maxmp"));
		charInfopackage.setCurrentMp(chardata.getDouble("curmp"));
		charInfopackage.setKarma(chardata.getInt("karma"));
		charInfopackage.setPkKills(chardata.getInt("pkkills"));
		charInfopackage.setPvPKills(chardata.getInt("pvpkills"));
		charInfopackage.setFace(chardata.getInt("face"));
		charInfopackage.setHairStyle(chardata.getInt("hairstyle"));
		charInfopackage.setHairColor(chardata.getInt("haircolor"));
		charInfopackage.setSex(chardata.getInt("sex"));
		charInfopackage.setExp(chardata.getLong("exp"));
		charInfopackage.setSp(chardata.getLong("sp"));
		charInfopackage.setVitalityPoints(chardata.getInt("vitality_points"));
		charInfopackage.setClanId(chardata.getInt("clanid"));
		charInfopackage.setRace(chardata.getInt("race"));
		final int baseClassId = chardata.getInt("base_class");
		final int activeClassId = chardata.getInt("classid");
		charInfopackage.setX(chardata.getInt("x"));
		charInfopackage.setY(chardata.getInt("y"));
		charInfopackage.setZ(chardata.getInt("z"));
		final int faction = chardata.getInt("faction");
		if (faction == 1)
		{
			charInfopackage.setGood();
		}
		
		if (faction == 2)
		{
			charInfopackage.setEvil();
		}
		
		if (MultilingualSupportConfig.MULTILANG_ENABLE)
		{
			String lang = chardata.getString("language");
			if (!MultilingualSupportConfig.MULTILANG_ALLOWED.contains(lang))
			{
				lang = MultilingualSupportConfig.MULTILANG_DEFAULT;
			}
			
			charInfopackage.setHtmlPrefix("data/lang/" + lang + "/");
		}
		
		// if is in subclass, load subclass exp, sp, level info
		if (baseClassId != activeClassId)
		{
			loadCharacterSubclassInfo(charInfopackage, objectId, activeClassId);
		}
		
		charInfopackage.setClassId(activeClassId);
		
		// Get the augmentation id for equipped weapon
		int weaponObjId = charInfopackage.getPaperdollObjectId(Inventory.PAPERDOLL_RHAND);
		if (weaponObjId < 1)
		{
			weaponObjId = charInfopackage.getPaperdollObjectId(Inventory.PAPERDOLL_RHAND);
		}
		
		if (weaponObjId > 0)
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement statement = con.prepareStatement("SELECT augAttributes FROM item_attributes WHERE itemId=?"))
			{
				statement.setInt(1, weaponObjId);
				try (ResultSet result = statement.executeQuery())
				{
					if (result.next())
					{
						final int augment = result.getInt("augAttributes");
						charInfopackage.setAugmentationId(augment == -1 ? 0 : augment);
					}
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "Could not restore augmentation info: " + e.getMessage(), e);
			}
		}
		
		// Check if the base class is set to zero and also doesn't match with the current active class, otherwise send the base class ID. This prevents chars created before base class was introduced from being displayed incorrectly.
		if ((baseClassId == 0) && (activeClassId > 0))
		{
			charInfopackage.setBaseClassId(activeClassId);
		}
		else
		{
			charInfopackage.setBaseClassId(baseClassId);
		}
		
		charInfopackage.setDeleteTimer(deletetime);
		charInfopackage.setLastAccess(chardata.getLong("lastAccess"));
		return charInfopackage;
	}
}
