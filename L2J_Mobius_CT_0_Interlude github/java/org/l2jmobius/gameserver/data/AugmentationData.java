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
package org.l2jmobius.gameserver.data;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.model.item.enums.BodyPart;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.options.Augmentation;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.clientpackets.AbstractRefinePacket;

/**
 * Loads augmentation bonuses and skills.
 * @author durgus, Gigiikun
 */
public class AugmentationData
{
	private static final Logger LOGGER = Logger.getLogger(AugmentationData.class.getName());
	
	// stats
	private static final int STAT_BLOCKSIZE = 3640;
	private static final int STAT_SUBBLOCKSIZE = 91;
	public static final int MIN_SKILL_ID = STAT_BLOCKSIZE * 4;
	
	// skills
	private static final int BLUE_START = 14561;
	private static final int SKILLS_BLOCKSIZE = 178;
	
	// basestats
	private static final int BASESTAT_STR = 16341;
	private static final int BASESTAT_MEN = 16344;
	
	private final List<List<Integer>> _blueSkills = new ArrayList<>();
	private final List<List<Integer>> _purpleSkills = new ArrayList<>();
	private final List<List<Integer>> _redSkills = new ArrayList<>();
	
	private final List<AugmentationChance> _augmentationChances = new ArrayList<>();
	
	private final Map<Integer, SkillHolder> _allSkills = new HashMap<>();
	
	protected AugmentationData()
	{
		for (int i = 0; i < 10; i++)
		{
			_blueSkills.add(new ArrayList<>());
			_purpleSkills.add(new ArrayList<>());
			_redSkills.add(new ArrayList<>());
		}
		
		load();
		if (!PlayerConfig.RETAIL_LIKE_AUGMENTATION)
		{
			for (int i = 0; i < 10; i++)
			{
				LOGGER.info(getClass().getSimpleName() + ": Loaded " + _blueSkills.get(i).size() + " blue, " + _purpleSkills.get(i).size() + " purple and " + _redSkills.get(i).size() + " red skills for lifeStoneLevel " + i);
			}
		}
		else
		{
			LOGGER.log(Level.INFO, getClass().getSimpleName() + ": Loaded " + _augmentationChances.size() + " augmentations.");
		}
	}
	
	public class AugmentationChance
	{
		private final String _weaponType;
		private final int _stoneId;
		private final int _variationId;
		private final int _categoryChance;
		private final int _augmentId;
		private final float _augmentChance;
		
		public AugmentationChance(String weaponType, int stoneId, int variationId, int categoryChance, int augmentId, float augmentChance)
		{
			_weaponType = weaponType;
			_stoneId = stoneId;
			_variationId = variationId;
			_categoryChance = categoryChance;
			_augmentId = augmentId;
			_augmentChance = augmentChance;
		}
		
		public String getWeaponType()
		{
			return _weaponType;
		}
		
		public int getStoneId()
		{
			return _stoneId;
		}
		
		public int getVariationId()
		{
			return _variationId;
		}
		
		public int getCategoryChance()
		{
			return _categoryChance;
		}
		
		public int getAugmentId()
		{
			return _augmentId;
		}
		
		public float getAugmentChance()
		{
			return _augmentChance;
		}
	}
	
	public class augmentationChanceAcc
	{
		private final String _weaponType;
		private final int _stoneId;
		private final int _variationId;
		private final int _categoryChance;
		private final int _augmentId;
		private final float _augmentChance;
		
		public augmentationChanceAcc(String weaponType, int stoneId, int variationId, int categoryChance, int augmentId, float augmentChance)
		{
			_weaponType = weaponType;
			_stoneId = stoneId;
			_variationId = variationId;
			_categoryChance = categoryChance;
			_augmentId = augmentId;
			_augmentChance = augmentChance;
		}
		
		public String getWeaponType()
		{
			return _weaponType;
		}
		
		public int getStoneId()
		{
			return _stoneId;
		}
		
		public int getVariationId()
		{
			return _variationId;
		}
		
		public int getCategoryChance()
		{
			return _categoryChance;
		}
		
		public int getAugmentId()
		{
			return _augmentId;
		}
		
		public float getAugmentChance()
		{
			return _augmentChance;
		}
	}
	
	private void load()
	{
		// Load the skillmap
		// Note: the skillmap data is only used when generating new augmentations the client expects a different id in order to display the skill in the items description...
		if (!PlayerConfig.RETAIL_LIKE_AUGMENTATION)
		{
			try
			{
				int badAugmantData = 0;
				final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				factory.setValidating(false);
				factory.setIgnoringComments(true);
				
				final File file = new File(ServerConfig.DATAPACK_ROOT + "/data/stats/augmentation/augmentation_skillmap.xml");
				if (!file.exists())
				{
					LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": ERROR The augmentation skillmap file is missing.");
					return;
				}
				
				final Document document = factory.newDocumentBuilder().parse(file);
				for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
				{
					if ("list".equalsIgnoreCase(n.getNodeName()))
					{
						for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
						{
							if ("augmentation".equalsIgnoreCase(d.getNodeName()))
							{
								NamedNodeMap attrs = d.getAttributes();
								int skillId = 0;
								final int augmentationId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
								int skillLevel = 0;
								String type = "blue";
								for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
								{
									if ("skillId".equalsIgnoreCase(cd.getNodeName()))
									{
										attrs = cd.getAttributes();
										skillId = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
									}
									else if ("skillLevel".equalsIgnoreCase(cd.getNodeName()))
									{
										attrs = cd.getAttributes();
										skillLevel = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
									}
									else if ("type".equalsIgnoreCase(cd.getNodeName()))
									{
										attrs = cd.getAttributes();
										type = attrs.getNamedItem("val").getNodeValue();
									}
								}
								
								if (skillId == 0)
								{
									badAugmantData++;
									continue;
								}
								else if (skillLevel == 0)
								{
									badAugmantData++;
									continue;
								}
								
								final int k = (augmentationId - BLUE_START) / SKILLS_BLOCKSIZE;
								if (type.equalsIgnoreCase("blue"))
								{
									_blueSkills.get(k).add(augmentationId);
								}
								else if (type.equalsIgnoreCase("purple"))
								{
									_purpleSkills.get(k).add(augmentationId);
								}
								else
								{
									_redSkills.get(k).add(augmentationId);
								}
								
								_allSkills.put(augmentationId, new SkillHolder(skillId, skillLevel));
							}
						}
					}
				}
				
				if (badAugmantData != 0)
				{
					LOGGER.info(getClass().getSimpleName() + ": " + badAugmantData + " negative effect skill(s) were skipped.");
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": ERROR parsing augmentation_skillmap.xml.", e);
				return;
			}
		}
		else
		{
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			
			final File file = new File(ServerConfig.DATAPACK_ROOT + "/data/stats/augmentation/retailchances.xml");
			if (file.exists())
			{
				Document document = null;
				try
				{
					document = factory.newDocumentBuilder().parse(file);
				}
				catch (Exception e)
				{
					LOGGER.warning("Problem with AugmentationData: " + e.getMessage());
					return;
				}
				
				String weaponType = null;
				int stoneId = 0;
				int variationId = 0;
				int categoryChance = 0;
				int augmentId = 0;
				float augmentChance = 0;
				for (Node node = document.getFirstChild(); node != null; node = node.getNextSibling())
				{
					if (node.getNodeName().equals("list"))
					{
						NamedNodeMap nodeAttributes = null;
						
						// System.out.println("We're going through the list now.");
						for (Node n = node.getFirstChild(); n != null; n = n.getNextSibling())
						{
							if (n.getNodeName().equals("weapon"))
							{
								nodeAttributes = n.getAttributes();
								weaponType = nodeAttributes.getNamedItem("type").getNodeValue();
								
								// System.out.println("Now showing Augmentations for " + aWeaponType + " Weapons.");
								for (Node c = n.getFirstChild(); c != null; c = c.getNextSibling())
								{
									if (c.getNodeName().equals("stone"))
									{
										nodeAttributes = c.getAttributes();
										stoneId = Integer.parseInt(nodeAttributes.getNamedItem("id").getNodeValue());
										for (Node v = c.getFirstChild(); v != null; v = v.getNextSibling())
										{
											if (v.getNodeName().equals("variation"))
											{
												nodeAttributes = v.getAttributes();
												variationId = Integer.parseInt(nodeAttributes.getNamedItem("id").getNodeValue());
												for (Node j = v.getFirstChild(); j != null; j = j.getNextSibling())
												{
													if (j.getNodeName().equals("category"))
													{
														nodeAttributes = j.getAttributes();
														categoryChance = Integer.parseInt(nodeAttributes.getNamedItem("probability").getNodeValue());
														
														// System.out.println("Stone Id: " + aStoneId + ", Variation Id: " + aVariationId + ", Category Chances: " + aCategoryChance);
														for (Node e = j.getFirstChild(); e != null; e = e.getNextSibling())
														{
															if (e.getNodeName().equals("augment"))
															{
																nodeAttributes = e.getAttributes();
																augmentId = Integer.parseInt(nodeAttributes.getNamedItem("id").getNodeValue());
																augmentChance = Float.parseFloat(nodeAttributes.getNamedItem("chance").getNodeValue());
																_augmentationChances.add(new AugmentationChance(weaponType, stoneId, variationId, categoryChance, augmentId, augmentChance));
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
			else
			{
				LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": ERROR The retailchances.xml data file is missing.");
				return;
			}
		}
	}
	
	/**
	 * Generate a new random augmentation
	 * @param level
	 * @param lifeStoneGrade
	 * @param bodyPart
	 * @param lifeStoneId
	 * @param item
	 * @return
	 */
	public Augmentation generateRandomAugmentation(int level, int lifeStoneGrade, BodyPart bodyPart, int lifeStoneId, Item item)
	{
		int stat12 = 0;
		int stat34 = 0;
		if (PlayerConfig.RETAIL_LIKE_AUGMENTATION)
		{
			if (item.getTemplate().isMagicWeapon())
			{
				final List<AugmentationChance> selectedChances12 = new ArrayList<>();
				final List<AugmentationChance> selectedChances34 = new ArrayList<>();
				for (AugmentationChance ac : _augmentationChances)
				{
					if (ac.getWeaponType().equals("mage") && (ac.getStoneId() == lifeStoneId))
					{
						if (ac.getVariationId() == 1)
						{
							selectedChances12.add(ac);
						}
						else
						{
							selectedChances34.add(ac);
						}
					}
				}
				
				int r = Rnd.get(10000);
				float s = 10000;
				for (AugmentationChance ac : selectedChances12)
				{
					if (s > r)
					{
						s -= (ac.getAugmentChance() * 100);
						stat12 = ac.getAugmentId();
					}
				}
				
				int[] gradeChance = null;
				switch (lifeStoneGrade)
				{
					case AbstractRefinePacket.GRADE_NONE:
					{
						gradeChance = PlayerConfig.RETAIL_LIKE_AUGMENTATION_NG_CHANCE;
						break;
					}
					case AbstractRefinePacket.GRADE_MID:
					{
						gradeChance = PlayerConfig.RETAIL_LIKE_AUGMENTATION_MID_CHANCE;
						break;
					}
					case AbstractRefinePacket.GRADE_HIGH:
					{
						gradeChance = PlayerConfig.RETAIL_LIKE_AUGMENTATION_HIGH_CHANCE;
						break;
					}
					case AbstractRefinePacket.GRADE_TOP:
					{
						gradeChance = PlayerConfig.RETAIL_LIKE_AUGMENTATION_TOP_CHANCE;
						break;
					}
					default:
					{
						gradeChance = PlayerConfig.RETAIL_LIKE_AUGMENTATION_NG_CHANCE;
					}
				}
				
				int c = Rnd.get(100);
				if (c < gradeChance[0])
				{
					c = 55;
				}
				else if (c < (gradeChance[0] + gradeChance[1]))
				{
					c = 35;
				}
				else if (c < (gradeChance[0] + gradeChance[1] + gradeChance[2]))
				{
					c = 7;
				}
				else
				{
					c = 3;
				}
				
				final List<AugmentationChance> selectedChances34final = new ArrayList<>();
				for (AugmentationChance ac : selectedChances34)
				{
					if (ac.getCategoryChance() == c)
					{
						selectedChances34final.add(ac);
					}
				}
				
				r = Rnd.get(10000);
				s = 10000;
				for (AugmentationChance ac : selectedChances34final)
				{
					if (s > r)
					{
						s -= (ac.getAugmentChance() * 100);
						stat34 = ac.getAugmentId();
					}
				}
			}
			else
			{
				final List<AugmentationChance> selectedChances12 = new ArrayList<>();
				final List<AugmentationChance> selectedChances34 = new ArrayList<>();
				for (AugmentationChance ac : _augmentationChances)
				{
					if (ac.getWeaponType().equals("warrior") && (ac.getStoneId() == lifeStoneId))
					{
						if (ac.getVariationId() == 1)
						{
							selectedChances12.add(ac);
						}
						else
						{
							selectedChances34.add(ac);
						}
					}
				}
				
				int r = Rnd.get(10000);
				float s = 10000;
				for (AugmentationChance ac : selectedChances12)
				{
					if (s > r)
					{
						s -= (ac.getAugmentChance() * 100);
						stat12 = ac.getAugmentId();
					}
				}
				
				int[] gradeChance = null;
				switch (lifeStoneGrade)
				{
					case AbstractRefinePacket.GRADE_NONE:
					{
						gradeChance = PlayerConfig.RETAIL_LIKE_AUGMENTATION_NG_CHANCE;
						break;
					}
					case AbstractRefinePacket.GRADE_MID:
					{
						gradeChance = PlayerConfig.RETAIL_LIKE_AUGMENTATION_MID_CHANCE;
						break;
					}
					case AbstractRefinePacket.GRADE_HIGH:
					{
						gradeChance = PlayerConfig.RETAIL_LIKE_AUGMENTATION_HIGH_CHANCE;
						break;
					}
					case AbstractRefinePacket.GRADE_TOP:
					{
						gradeChance = PlayerConfig.RETAIL_LIKE_AUGMENTATION_TOP_CHANCE;
						break;
					}
					default:
					{
						gradeChance = PlayerConfig.RETAIL_LIKE_AUGMENTATION_NG_CHANCE;
					}
				}
				
				int c = Rnd.get(100);
				if (c < gradeChance[0])
				{
					c = 55;
				}
				else if (c < (gradeChance[0] + gradeChance[1]))
				{
					c = 35;
				}
				else if (c < (gradeChance[0] + gradeChance[1] + gradeChance[2]))
				{
					c = 7;
				}
				else
				{
					c = 3;
				}
				
				final List<AugmentationChance> selectedChances34final = new ArrayList<>();
				for (AugmentationChance ac : selectedChances34)
				{
					if (ac.getCategoryChance() == c)
					{
						selectedChances34final.add(ac);
					}
				}
				
				r = Rnd.get(10000);
				s = 10000;
				for (AugmentationChance ac : selectedChances34final)
				{
					if (s > r)
					{
						s -= (ac.getAugmentChance() * 100);
						stat34 = ac.getAugmentId();
					}
				}
			}
			
			return new Augmentation(((stat34 << 16) + stat12));
		}
		
		boolean generateSkill = false;
		boolean generateGlow = false;
		
		// life stone level is used for stat Id and skill level, but here the max level is 9
		int lifeStoneLevel = Math.min(level, 9);
		
		switch (lifeStoneGrade)
		{
			case AbstractRefinePacket.GRADE_NONE:
			{
				if (Rnd.get(1, 100) <= PlayerConfig.AUGMENTATION_NG_SKILL_CHANCE)
				{
					generateSkill = true;
				}
				
				if (Rnd.get(1, 100) <= PlayerConfig.AUGMENTATION_NG_GLOW_CHANCE)
				{
					generateGlow = true;
				}
				break;
			}
			case AbstractRefinePacket.GRADE_MID:
			{
				if (Rnd.get(1, 100) <= PlayerConfig.AUGMENTATION_MID_SKILL_CHANCE)
				{
					generateSkill = true;
				}
				
				if (Rnd.get(1, 100) <= PlayerConfig.AUGMENTATION_MID_GLOW_CHANCE)
				{
					generateGlow = true;
				}
				break;
			}
			case AbstractRefinePacket.GRADE_HIGH:
			{
				if (Rnd.get(1, 100) <= PlayerConfig.AUGMENTATION_HIGH_SKILL_CHANCE)
				{
					generateSkill = true;
				}
				
				if (Rnd.get(1, 100) <= PlayerConfig.AUGMENTATION_HIGH_GLOW_CHANCE)
				{
					generateGlow = true;
				}
				break;
			}
			case AbstractRefinePacket.GRADE_TOP:
			{
				if (Rnd.get(1, 100) <= PlayerConfig.AUGMENTATION_TOP_SKILL_CHANCE)
				{
					generateSkill = true;
				}
				
				if (Rnd.get(1, 100) <= PlayerConfig.AUGMENTATION_TOP_GLOW_CHANCE)
				{
					generateGlow = true;
				}
				break;
			}
		}
		
		if (!generateSkill && (Rnd.get(1, 100) <= PlayerConfig.AUGMENTATION_BASESTAT_CHANCE))
		{
			stat34 = Rnd.get(BASESTAT_STR, BASESTAT_MEN);
		}
		
		// Second: decide which grade the augmentation result is going to have:
		// 0:yellow, 1:blue, 2:purple, 3:red
		// The chances used here are most likely custom,
		// what's known is: you can't have yellow with skill(or baseStatModifier)
		// noGrade stone can not have glow, mid only with skill, high has a chance(custom), top allways glow
		int resultColor = Rnd.get(0, 100);
		if ((stat34 == 0) && !generateSkill)
		{
			if (resultColor <= ((15 * lifeStoneGrade) + 40))
			{
				resultColor = 1;
			}
			else
			{
				resultColor = 0;
			}
		}
		else
		{
			if ((resultColor <= ((10 * lifeStoneGrade) + 5)) || (stat34 != 0))
			{
				resultColor = 3;
			}
			else if (resultColor <= ((10 * lifeStoneGrade) + 10))
			{
				resultColor = 1;
			}
			else
			{
				resultColor = 2;
			}
		}
		
		// generate a skill if necessary
		if (generateSkill)
		{
			switch (resultColor)
			{
				case 1: // blue skill
				{
					stat34 = _blueSkills.get(lifeStoneLevel).get(Rnd.get(0, _blueSkills.get(lifeStoneLevel).size() - 1));
					break;
				}
				case 2: // purple skill
				{
					stat34 = _purpleSkills.get(lifeStoneLevel).get(Rnd.get(0, _purpleSkills.get(lifeStoneLevel).size() - 1));
					break;
				}
				case 3: // red skill
				{
					stat34 = _redSkills.get(lifeStoneLevel).get(Rnd.get(0, _redSkills.get(lifeStoneLevel).size() - 1));
					break;
				}
			}
		}
		
		// Third: Calculate the subblock offset for the chosen color,
		// and the level of the lifeStone
		// from large number of retail augmentations:
		// no skill part
		// Id for stat12:
		// A:1-910 B:911-1820 C:1821-2730 D:2731-3640 E:3641-4550 F:4551-5460 G:5461-6370 H:6371-7280
		// Id for stat34(this defines the color):
		// I:7281-8190(yellow) K:8191-9100(blue) L:10921-11830(yellow) M:11831-12740(blue)
		// you can combine I-K with A-D and L-M with E-H
		// using C-D or G-H Id you will get a glow effect
		// there seems no correlation in which grade use which Id except for the glowing restriction
		// skill part
		// Id for stat12:
		// same for no skill part
		// A same as E, B same as F, C same as G, D same as H
		// A - no glow, no grade LS
		// B - weak glow, mid grade LS?
		// C - glow, high grade LS?
		// D - strong glow, top grade LS?
		
		// is neither a skill nor basestat used for stat34? then generate a normal stat
		int offset;
		if (stat34 == 0)
		{
			final int temp = Rnd.get(2, 3);
			final int colorOffset = (resultColor * (10 * STAT_SUBBLOCKSIZE)) + (temp * STAT_BLOCKSIZE) + 1;
			offset = (lifeStoneLevel * STAT_SUBBLOCKSIZE) + colorOffset;
			stat34 = Rnd.get(offset, (offset + STAT_SUBBLOCKSIZE) - 1);
			if (generateGlow && (lifeStoneGrade >= 2))
			{
				offset = (lifeStoneLevel * STAT_SUBBLOCKSIZE) + ((temp - 2) * STAT_BLOCKSIZE) + (lifeStoneGrade * (10 * STAT_SUBBLOCKSIZE)) + 1;
			}
			else
			{
				offset = (lifeStoneLevel * STAT_SUBBLOCKSIZE) + ((temp - 2) * STAT_BLOCKSIZE) + (Rnd.get(0, 1) * (10 * STAT_SUBBLOCKSIZE)) + 1;
			}
		}
		else
		{
			if (!generateGlow)
			{
				offset = (lifeStoneLevel * STAT_SUBBLOCKSIZE) + (Rnd.get(0, 1) * STAT_BLOCKSIZE) + 1;
			}
			else
			{
				offset = (lifeStoneLevel * STAT_SUBBLOCKSIZE) + (Rnd.get(0, 1) * STAT_BLOCKSIZE) + (((lifeStoneGrade + resultColor) / 2) * (10 * STAT_SUBBLOCKSIZE)) + 1;
			}
		}
		
		stat12 = Rnd.get(offset, (offset + STAT_SUBBLOCKSIZE) - 1);
		return new Augmentation(((stat34 << 16) + stat12));
	}
	
	public static AugmentationData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final AugmentationData INSTANCE = new AugmentationData();
	}
}
