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
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.holders.PetData;
import org.l2jmobius.gameserver.data.holders.PetLevelData;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.enums.player.MountType;

/**
 * @author Zoey76
 */
public class PetDataTable implements IXmlReader
{
	private final Map<Integer, PetData> _pets = new ConcurrentHashMap<>();
	
	protected PetDataTable()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_pets.clear();
		parseDatapackDirectory("data/stats/pets", false);
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _pets.size() + " pets.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		NamedNodeMap attrs;
		for (Node d = document.getFirstChild().getFirstChild(); d != null; d = d.getNextSibling())
		{
			if (d.getNodeName().equals("pet"))
			{
				final int npcId = parseInteger(d.getAttributes(), "id");
				final int itemId = parseInteger(d.getAttributes(), "itemId");
				
				// index ignored for now
				final PetData data = new PetData(npcId, itemId);
				for (Node p = d.getFirstChild(); p != null; p = p.getNextSibling())
				{
					if (p.getNodeName().equals("set"))
					{
						attrs = p.getAttributes();
						final String type = attrs.getNamedItem("name").getNodeValue();
						if ("food".equals(type))
						{
							for (String foodId : attrs.getNamedItem("val").getNodeValue().split(";"))
							{
								data.addFood(Integer.parseInt(foodId));
							}
						}
						else if ("load".equals(type))
						{
							data.setLoad(parseInteger(attrs, "val"));
						}
						else if ("hungry_limit".equals(type))
						{
							data.setHungryLimit(parseInteger(attrs, "val"));
						}
						else if ("sync_level".equals(type))
						{
							data.setSyncLevel(parseInteger(attrs, "val") == 1);
						}
						
						// evolve ignored
					}
					else if (p.getNodeName().equals("skills"))
					{
						for (Node s = p.getFirstChild(); s != null; s = s.getNextSibling())
						{
							if (s.getNodeName().equals("skill"))
							{
								attrs = s.getAttributes();
								data.addNewSkill(parseInteger(attrs, "skillId"), parseInteger(attrs, "skillLevel"), parseInteger(attrs, "minLevel"));
							}
						}
					}
					else if (p.getNodeName().equals("stats"))
					{
						for (Node s = p.getFirstChild(); s != null; s = s.getNextSibling())
						{
							if (s.getNodeName().equals("stat"))
							{
								final int level = Integer.parseInt(s.getAttributes().getNamedItem("level").getNodeValue());
								final StatSet set = new StatSet();
								for (Node bean = s.getFirstChild(); bean != null; bean = bean.getNextSibling())
								{
									if (bean.getNodeName().equals("set"))
									{
										attrs = bean.getAttributes();
										if (attrs.getNamedItem("name").getNodeValue().equals("speed_on_ride"))
										{
											set.set("walkSpeedOnRide", attrs.getNamedItem("walk").getNodeValue());
											set.set("runSpeedOnRide", attrs.getNamedItem("run").getNodeValue());
											set.set("slowSwimSpeedOnRide", attrs.getNamedItem("slowSwim").getNodeValue());
											set.set("fastSwimSpeedOnRide", attrs.getNamedItem("fastSwim").getNodeValue());
											if (attrs.getNamedItem("slowFly") != null)
											{
												set.set("slowFlySpeedOnRide", attrs.getNamedItem("slowFly").getNodeValue());
											}
											
											if (attrs.getNamedItem("fastFly") != null)
											{
												set.set("fastFlySpeedOnRide", attrs.getNamedItem("fastFly").getNodeValue());
											}
										}
										else
										{
											set.set(attrs.getNamedItem("name").getNodeValue(), attrs.getNamedItem("val").getNodeValue());
										}
									}
								}
								
								data.addNewStat(level, new PetLevelData(set));
							}
						}
					}
				}
				
				_pets.put(npcId, data);
			}
		}
	}
	
	/**
	 * Retrieves pet data based on the item ID used to summon it.
	 * @param itemId the item ID associated with the pet.
	 * @return the {@link PetData} associated with the given item ID, or {@code null} if no matching data is found.
	 */
	public PetData getPetDataByItemId(int itemId)
	{
		for (PetData data : _pets.values())
		{
			if (data.getItemId() == itemId)
			{
				return data;
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves pet level data for a specified pet ID and level.
	 * @param petId the unique identifier of the pet.
	 * @param petLevel the level of the pet.
	 * @return the {@link PetLevelData} containing the pet's parameters for the given ID and level, or the maximum level data if the requested level exceeds it.
	 */
	public PetLevelData getPetLevelData(int petId, int petLevel)
	{
		final PetData pd = getPetData(petId);
		if (pd != null)
		{
			if (petLevel > pd.getMaxLevel())
			{
				return pd.getPetLevelData(pd.getMaxLevel());
			}
			
			return pd.getPetLevelData(petLevel);
		}
		
		return null;
	}
	
	/**
	 * Retrieves the pet data for a specified pet ID.
	 * @param petId the unique identifier of the pet.
	 * @return the {@link PetData} associated with the given ID, or {@code null} if no data is available.
	 */
	public PetData getPetData(int petId)
	{
		if (!_pets.containsKey(petId))
		{
			LOGGER.info(getClass().getSimpleName() + ": Missing pet data for npcid: " + petId);
		}
		
		return _pets.get(petId);
	}
	
	/**
	 * Retrieves the minimum level for a specified pet ID.
	 * @param petId the unique identifier of the pet.
	 * @return the minimum level of the pet with the given ID.
	 */
	public int getPetMinLevel(int petId)
	{
		return _pets.get(petId).getMinLevel();
	}
	
	/**
	 * Retrieves the summoning item ID for a pet based on its NPC ID.
	 * @param npcId the NPC ID associated with the pet.
	 * @return the item ID used to summon the pet associated with the given NPC ID.
	 */
	public int getPetItemsByNpc(int npcId)
	{
		return _pets.get(npcId).getItemId();
	}
	
	/**
	 * Checks if a pet with the given NPC ID is mountable.
	 * @param npcId the NPC ID to verify.
	 * @return {@code true} if the pet with the specified NPC ID is mountable, {@code false} otherwise.
	 */
	public static boolean isMountable(int npcId)
	{
		return MountType.findByNpcId(npcId) != MountType.NONE;
	}
	
	public static PetDataTable getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final PetDataTable INSTANCE = new PetDataTable();
	}
}
