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
package org.l2jmobius.gameserver.data.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.custom.AutoPlayConfig;
import org.l2jmobius.gameserver.config.custom.AutoPotionsConfig;
import org.l2jmobius.gameserver.config.custom.OfflinePlayConfig;
import org.l2jmobius.gameserver.config.custom.OfflineTradeConfig;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.player.AutoUseSettingsHolder;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.groups.PartyDistributionType;
import org.l2jmobius.gameserver.network.Disconnection;
import org.l2jmobius.gameserver.network.serverpackets.LeaveWorld;
import org.l2jmobius.gameserver.taskmanagers.AutoPlayTaskManager;
import org.l2jmobius.gameserver.taskmanagers.AutoPotionTaskManager;
import org.l2jmobius.gameserver.taskmanagers.AutoUseTaskManager;

/**
 * @author Mobius
 */
public class OfflinePlayTable
{
	private static final Logger LOGGER = Logger.getLogger(OfflinePlayTable.class.getName());
	
	private static final String LOAD_PLAYER_IDS = "SELECT DISTINCT charId FROM character_offline_play";
	private static final String LOAD_PLAYER = "SELECT * FROM character_offline_play WHERE charId=?";
	private static final String SAVE_PLAYER = "INSERT INTO character_offline_play (charId, type, id) VALUES (?, ?, ?)";
	private static final String REMOVE_PLAYER = "DELETE FROM character_offline_play WHERE charId=?";
	private static final String LOAD_GROUP_LEADER_IDS = "SELECT DISTINCT leaderId FROM character_offline_play_group";
	private static final String LOAD_GROUP_MEMBERS = "SELECT charId, type FROM character_offline_play_group WHERE leaderId=?";
	private static final String SAVE_GROUP_MEMBER = "INSERT INTO character_offline_play_group (leaderId, charId, type) VALUES (?, ?, ?)";
	private static final String REMOVE_GROUP_MEMBERS = "TRUNCATE TABLE character_offline_play_group";
	
	private static final int TYPE_ACTIVE_SOULSHOT = 0;
	private static final int TYPE_AUTO_SUPPLY_ITEM = 1;
	private static final int TYPE_AUTO_BUFF = 2;
	private static final int TYPE_AUTO_SKILL = 3;
	private static final int TYPE_AUTO_ACTION = 4;
	private static final int TYPE_AUTO_POTION_ITEM = 5;
	private static final int TYPE_CUSTOM_AUTO_POTION_SYSTEM = 100; // config\Custom\AutoPotions.ini
	
	protected OfflinePlayTable()
	{
	}
	
	/**
	 * Restores all offline play players on server start.
	 */
	public void restoreOfflinePlayers()
	{
		LOGGER.info(getClass().getSimpleName() + ": Loading offline auto players...");
		
		try (Connection con = DatabaseFactory.getConnection();
			Statement statement = con.createStatement();
			ResultSet result = statement.executeQuery(LOAD_PLAYER_IDS))
		{
			int nPlayers = 0;
			
			while (result.next())
			{
				Player player = null;
				
				try
				{
					player = Player.load(result.getInt("charId"));
					player.setOnlineStatus(true, false);
					player.spawnMe(player.getX(), player.getY(), player.getZ());
					
					try (PreparedStatement stmItems = con.prepareStatement(LOAD_PLAYER))
					{
						stmItems.setInt(1, player.getObjectId());
						
						try (ResultSet items = stmItems.executeQuery())
						{
							final AutoUseSettingsHolder settings = player.getAutoUseSettings();
							
							while (items.next())
							{
								final int type = items.getInt("type");
								final int id = items.getInt("id");
								
								switch (type)
								{
									case TYPE_ACTIVE_SOULSHOT:
									{
										player.addAutoSoulShot(id);
										break;
									}
									case TYPE_AUTO_SUPPLY_ITEM:
									{
										settings.getAutoSupplyItems().add(id);
										break;
									}
									case TYPE_AUTO_BUFF:
									{
										settings.getAutoBuffs().add(id);
										break;
									}
									case TYPE_AUTO_SKILL:
									{
										settings.getAutoSkills().add(id);
										break;
									}
									case TYPE_AUTO_ACTION:
									{
										settings.getAutoActions().add(id);
										break;
									}
									case TYPE_AUTO_POTION_ITEM:
									{
										settings.setAutoPotionItem(id);
										break;
									}
									case TYPE_CUSTOM_AUTO_POTION_SYSTEM:
									{
										if (AutoPotionsConfig.AUTO_POTIONS_ENABLED)
										{
											AutoPotionTaskManager.getInstance().add(player);
										}
										break;
									}
								}
							}
						}
					}
					
					player.setOfflinePlay(true);
					player.setOnlineStatus(true, true);
					player.restoreEffects();
					player.setRunning();
					
					if (OfflineTradeConfig.OFFLINE_SET_NAME_COLOR)
					{
						player.getAppearance().setNameColor(OfflinePlayConfig.OFFLINE_PLAY_NAME_COLOR);
					}
					
					if (!OfflinePlayConfig.OFFLINE_PLAY_ABNORMAL_EFFECTS.isEmpty())
					{
						player.startAbnormalVisualEffect(true, OfflinePlayConfig.OFFLINE_PLAY_ABNORMAL_EFFECTS.get(Rnd.get(OfflinePlayConfig.OFFLINE_PLAY_ABNORMAL_EFFECTS.size())));
					}
					
					// player.broadcastUserInfo();
					
					// Start auto play and auto use.
					AutoPlayTaskManager.getInstance().startAutoPlay(player);
					AutoUseTaskManager.getInstance().startAutoUseTask(player);
					
					nPlayers++;
				}
				catch (Exception e)
				{
					LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Error loading auto player " + player, e);
					if (player != null)
					{
						Disconnection.of(player).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
					}
				}
			}
			
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + nPlayers + " offline auto players.");
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Error while loading offline auto players. " + e.getMessage(), e);
		}
		
		// Restore parties.
		if (AutoPlayConfig.ENABLE_AUTO_ASSIST)
		{
			try (Connection con = DatabaseFactory.getConnection();
				Statement statement = con.createStatement();
				ResultSet result = statement.executeQuery(LOAD_GROUP_LEADER_IDS))
			{
				int nParties = 0;
				
				while (result.next())
				{
					final int leaderId = result.getInt("leaderId");
					final Player leader = World.getInstance().getPlayer(leaderId);
					if (leader != null)
					{
						nParties++;
						
						try (PreparedStatement stmtMembers = con.prepareStatement(LOAD_GROUP_MEMBERS))
						{
							stmtMembers.setInt(1, leaderId);
							
							try (ResultSet members = stmtMembers.executeQuery())
							{
								Party party = null;
								
								while (members.next())
								{
									final int charId = members.getInt("charId");
									final Player member = World.getInstance().getPlayer(charId);
									if (member != null)
									{
										if (party == null)
										{
											party = new Party(leader, PartyDistributionType.findById(members.getInt("type")));
										}
										
										member.joinParty(party);
									}
								}
							}
						}
					}
				}
				
				LOGGER.info(getClass().getSimpleName() + ": Loaded " + nParties + " offline groups.");
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Error while restoring offline groups. " + e.getMessage(), e);
			}
		}
		
		// Clear all party data in one operation.
		try (Connection con = DatabaseFactory.getConnection();
			Statement statement = con.createStatement())
		{
			statement.executeUpdate(REMOVE_GROUP_MEMBERS);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Error clearing offline group data. " + e.getMessage(), e);
		}
	}
	
	/**
	 * Stores the player entering offline play mode.
	 * @param player the player
	 */
	public void storeOfflinePlay(Player player)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement1 = con.prepareStatement(REMOVE_PLAYER);
			PreparedStatement statement2 = con.prepareStatement(SAVE_PLAYER))
		{
			// First remove existing entries.
			final int playerObjectId = player.getObjectId();
			statement1.setInt(1, playerObjectId);
			statement1.execute();
			
			try
			{
				// Store active soulshot items.
				for (int shotId : player.getAutoSoulShot())
				{
					statement2.setInt(1, playerObjectId);
					statement2.setInt(2, TYPE_ACTIVE_SOULSHOT);
					statement2.setInt(3, shotId);
					statement2.addBatch();
				}
				
				// Store auto supply items.
				for (Integer itemId : player.getAutoUseSettings().getAutoSupplyItems())
				{
					statement2.setInt(1, playerObjectId);
					statement2.setInt(2, TYPE_AUTO_SUPPLY_ITEM);
					statement2.setInt(3, itemId);
					statement2.addBatch();
				}
				
				// Store auto buffs.
				for (Integer buffId : player.getAutoUseSettings().getAutoBuffs())
				{
					statement2.setInt(1, playerObjectId);
					statement2.setInt(2, TYPE_AUTO_BUFF);
					statement2.setInt(3, buffId);
					statement2.addBatch();
				}
				
				// Store auto skills.
				for (Integer skillId : player.getAutoUseSettings().getAutoSkills())
				{
					statement2.setInt(1, playerObjectId);
					statement2.setInt(2, TYPE_AUTO_SKILL);
					statement2.setInt(3, skillId);
					statement2.addBatch();
				}
				
				// Store auto actions.
				for (Integer actionId : player.getAutoUseSettings().getAutoActions())
				{
					statement2.setInt(1, playerObjectId);
					statement2.setInt(2, TYPE_AUTO_ACTION);
					statement2.setInt(3, actionId);
					statement2.addBatch();
				}
				
				// Store auto potion item.
				final int autoPotionItem = player.getAutoUseSettings().getAutoPotionItem();
				if (autoPotionItem > 0)
				{
					statement2.setInt(1, playerObjectId);
					statement2.setInt(2, TYPE_AUTO_POTION_ITEM);
					statement2.setInt(3, autoPotionItem);
					statement2.addBatch();
				}
				
				// Custom auto potion system.
				if (AutoPotionsConfig.AUTO_POTIONS_ENABLED && AutoPotionTaskManager.getInstance().hasPlayer(player))
				{
					statement2.setInt(1, playerObjectId);
					statement2.setInt(2, TYPE_CUSTOM_AUTO_POTION_SYSTEM);
					statement2.setInt(3, 0);
					statement2.addBatch();
				}
				
				// Execute all batched statements.
				statement2.executeBatch();
				// No need to call con.commit() as HikariCP autocommit is true.
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Error while saving offline auto player " + player, e);
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Error while saving offline auto players. " + e.getMessage(), e);
		}
	}
	
	public void storeOfflineGroups()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(SAVE_GROUP_MEMBER))
		{
			for (Player player : World.getInstance().getPlayers())
			{
				if (player.isAutoPlaying())
				{
					final Party party = player.getParty();
					if ((party != null) && !party.isLeader(player))
					{
						final Player leader = party.getLeader();
						if (leader.isAutoPlaying())
						{
							statement.setInt(1, leader.getObjectId());
							statement.setInt(2, player.getObjectId());
							statement.setInt(3, party.getDistributionType().getId());
							statement.addBatch();
						}
					}
				}
			}
			
			// Execute all batched statements.
			statement.executeBatch();
			// No need to call con.commit() as HikariCP autocommit is true.
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Error while storing offline parties. " + e.getMessage(), e);
		}
	}
	
	/**
	 * Handles player removal of offline play mode.
	 * @param player the player
	 */
	public void removeOfflinePlay(Player player)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(REMOVE_PLAYER))
		{
			statement.setInt(1, player.getObjectId());
			statement.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Error while removing offline auto players. " + e.getMessage(), e);
		}
	}
	
	public static OfflinePlayTable getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final OfflinePlayTable INSTANCE = new OfflinePlayTable();
	}
}
