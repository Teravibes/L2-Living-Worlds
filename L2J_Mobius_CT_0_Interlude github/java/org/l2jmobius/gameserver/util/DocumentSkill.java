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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.conditions.Condition;
import org.l2jmobius.gameserver.model.skill.EffectScope;
import org.l2jmobius.gameserver.model.skill.Skill;

/**
 * @author mkizub, Mobius
 */
public class DocumentSkill extends DocumentBase
{
	private DocumentSkillDataHolder _currentSkill;
	private final List<Skill> _skillsInFile = new ArrayList<>();
	
	private class DocumentSkillDataHolder
	{
		public DocumentSkillDataHolder()
		{
		}
		
		public int id;
		public String name;
		public StatSet[] sets;
		public StatSet[] enchsets1;
		public StatSet[] enchsets2;
		public StatSet[] enchsets3;
		public StatSet[] enchsets4;
		public StatSet[] enchsets5;
		public StatSet[] enchsets6;
		public StatSet[] enchsets7;
		public StatSet[] enchsets8;
		public int currentLevel;
		public List<Skill> skills = new ArrayList<>();
		public List<Skill> currentSkills = new ArrayList<>();
	}
	
	public DocumentSkill(File file)
	{
		super(file);
	}
	
	private void setCurrentSkill(DocumentSkillDataHolder skill)
	{
		_currentSkill = skill;
	}
	
	@Override
	protected StatSet getStatSet()
	{
		return _currentSkill.sets[_currentSkill.currentLevel];
	}
	
	@Override
	protected String getTableValue(String name)
	{
		try
		{
			return _tables.get(name)[_currentSkill.currentLevel];
		}
		catch (RuntimeException e)
		{
			LOGGER.log(Level.SEVERE, "Error in table: " + name + " of Skill Id " + _currentSkill.id, e);
			return "";
		}
	}
	
	@Override
	protected String getTableValue(String name, int idx)
	{
		try
		{
			return _tables.get(name)[idx - 1];
		}
		catch (RuntimeException e)
		{
			LOGGER.log(Level.SEVERE, "Wrong level count in skill Id " + _currentSkill.id + " name: " + name + " index : " + idx, e);
			return "";
		}
	}
	
	@Override
	protected void parseDocument(Document document)
	{
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("skill".equalsIgnoreCase(d.getNodeName()))
					{
						setCurrentSkill(new DocumentSkillDataHolder());
						parseSkill(d);
						_skillsInFile.addAll(_currentSkill.skills);
						resetTable();
					}
				}
			}
			else if ("skill".equalsIgnoreCase(n.getNodeName()))
			{
				setCurrentSkill(new DocumentSkillDataHolder());
				parseSkill(n);
				_skillsInFile.addAll(_currentSkill.skills);
			}
		}
	}
	
	/**
	 * CT2.4 compatibility.
	 * @param group id identifier.
	 * @return amount of levels expected.
	 */
	private int getEnchantGroupSize(int group)
	{
		// 1, 2 = 30
		// 5, 6 = 15
		return group < 3 ? 30 : 15;
	}
	
	private void parseSkill(Node node)
	{
		Node n = node;
		final NamedNodeMap attrs = n.getAttributes();
		int enchantLevels1 = 0;
		int enchantLevels2 = 0;
		int enchantLevels3 = 0;
		int enchantLevels4 = 0;
		int enchantLevels5 = 0;
		int enchantLevels6 = 0;
		int enchantLevels7 = 0;
		int enchantLevels8 = 0;
		final int skillId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
		final String skillName = attrs.getNamedItem("name").getNodeValue();
		final String levels = attrs.getNamedItem("levels").getNodeValue();
		final int lastLvl = Integer.parseInt(levels);
		if (attrs.getNamedItem("enchantGroup1") != null)
		{
			enchantLevels1 = getEnchantGroupSize(Integer.parseInt(attrs.getNamedItem("enchantGroup1").getNodeValue()));
		}
		
		if (attrs.getNamedItem("enchantGroup2") != null)
		{
			enchantLevels2 = getEnchantGroupSize(Integer.parseInt(attrs.getNamedItem("enchantGroup2").getNodeValue()));
		}
		
		if (attrs.getNamedItem("enchantGroup3") != null)
		{
			enchantLevels3 = getEnchantGroupSize(Integer.parseInt(attrs.getNamedItem("enchantGroup3").getNodeValue()));
		}
		
		if (attrs.getNamedItem("enchantGroup4") != null)
		{
			enchantLevels4 = getEnchantGroupSize(Integer.parseInt(attrs.getNamedItem("enchantGroup4").getNodeValue()));
		}
		
		if (attrs.getNamedItem("enchantGroup5") != null)
		{
			enchantLevels5 = getEnchantGroupSize(Integer.parseInt(attrs.getNamedItem("enchantGroup5").getNodeValue()));
		}
		
		if (attrs.getNamedItem("enchantGroup6") != null)
		{
			enchantLevels6 = getEnchantGroupSize(Integer.parseInt(attrs.getNamedItem("enchantGroup6").getNodeValue()));
		}
		
		if (attrs.getNamedItem("enchantGroup7") != null)
		{
			enchantLevels7 = getEnchantGroupSize(Integer.parseInt(attrs.getNamedItem("enchantGroup7").getNodeValue()));
		}
		
		if (attrs.getNamedItem("enchantGroup8") != null)
		{
			enchantLevels8 = getEnchantGroupSize(Integer.parseInt(attrs.getNamedItem("enchantGroup8").getNodeValue()));
		}
		
		_currentSkill.id = skillId;
		_currentSkill.name = skillName;
		_currentSkill.sets = new StatSet[lastLvl];
		_currentSkill.enchsets1 = new StatSet[enchantLevels1];
		_currentSkill.enchsets2 = new StatSet[enchantLevels2];
		_currentSkill.enchsets3 = new StatSet[enchantLevels3];
		_currentSkill.enchsets4 = new StatSet[enchantLevels4];
		_currentSkill.enchsets5 = new StatSet[enchantLevels5];
		_currentSkill.enchsets6 = new StatSet[enchantLevels6];
		_currentSkill.enchsets7 = new StatSet[enchantLevels7];
		_currentSkill.enchsets8 = new StatSet[enchantLevels8];
		
		for (int i = 0; i < lastLvl; i++)
		{
			_currentSkill.sets[i] = new StatSet();
			_currentSkill.sets[i].set("skill_id", _currentSkill.id);
			_currentSkill.sets[i].set("level", i + 1);
			_currentSkill.sets[i].set("name", _currentSkill.name);
		}
		
		if (_currentSkill.sets.length != lastLvl)
		{
			throw new RuntimeException("Skill id=" + skillId + " number of levels missmatch, " + lastLvl + " levels expected");
		}
		
		final Node first = n.getFirstChild();
		for (n = first; n != null; n = n.getNextSibling())
		{
			if ("table".equalsIgnoreCase(n.getNodeName()))
			{
				parseTable(n);
			}
		}
		
		for (int i = 1; i <= lastLvl; i++)
		{
			for (n = first; n != null; n = n.getNextSibling())
			{
				if (n.getNodeType() != Node.ELEMENT_NODE)
				{
					continue;
				}
				
				final String nodeName = n.getNodeName();
				if (nodeName.equalsIgnoreCase("table") || nodeName.toLowerCase().startsWith("enchant") || nodeName.toLowerCase().endsWith("effects") || nodeName.equalsIgnoreCase("conditions"))
				{
					continue;
				}
				
				if ("set".equalsIgnoreCase(nodeName))
				{
					parseBeanSet(n, _currentSkill.sets[i - 1], i);
				}
				else
				{
					// Check if this is a direct element value node (like <reuseDelay>3000</reuseDelay>).
					if (n.getAttributes().getNamedItem("levelValues") != null)
					{
						parseInlineLevelTable(n, _currentSkill.sets[i - 1], i);
					}
					else
					{
						parseElementValue(n, _currentSkill.sets[i - 1], i);
					}
				}
			}
		}
		
		// --- Enchant Route 1 ---
		for (int i = 0; i < enchantLevels1; i++)
		{
			_currentSkill.enchsets1[i] = new StatSet();
			_currentSkill.enchsets1[i].set("skill_id", _currentSkill.id);
			_currentSkill.enchsets1[i].set("level", i + 101); // C6 adaptation.
			_currentSkill.enchsets1[i].set("name", _currentSkill.name);
			
			for (n = first; n != null; n = n.getNextSibling())
			{
				if (n.getNodeType() != Node.ELEMENT_NODE)
				{
					continue;
				}
				
				final String nodeName = n.getNodeName();
				if (nodeName.equalsIgnoreCase("table") || nodeName.equalsIgnoreCase("conditions") || (nodeName.toLowerCase().startsWith("enchant") && !nodeName.equalsIgnoreCase("enchant1")))
				{
					continue;
				}
				
				if ("set".equalsIgnoreCase(nodeName))
				{
					parseBeanSet(n, _currentSkill.enchsets1[i], _currentSkill.sets.length);
				}
				else if ("enchant1".equalsIgnoreCase(nodeName))
				{
					parseBeanSet(n, _currentSkill.enchsets1[i], i + 1);
				}
				else if (!nodeName.toLowerCase().endsWith("effects"))
				{
					if (n.getAttributes().getNamedItem("subLevel1Values") != null)
					{
						parseInlineLevelTable(n, _currentSkill.enchsets1[i], i + 1, 1);
					}
					else
					{
						parseElementValue(n, _currentSkill.enchsets1[i], _currentSkill.sets.length);
					}
				}
			}
		}
		
		// --- Enchant Route 2 ---
		for (int i = 0; i < enchantLevels2; i++)
		{
			_currentSkill.enchsets2[i] = new StatSet();
			_currentSkill.enchsets2[i].set("skill_id", _currentSkill.id);
			_currentSkill.enchsets2[i].set("level", i + 141); // C6 adaptation.
			_currentSkill.enchsets2[i].set("name", _currentSkill.name);
			
			for (n = first; n != null; n = n.getNextSibling())
			{
				if (n.getNodeType() != Node.ELEMENT_NODE)
				{
					continue;
				}
				
				final String nodeName = n.getNodeName();
				if (nodeName.equalsIgnoreCase("table") || nodeName.equalsIgnoreCase("conditions") || (nodeName.toLowerCase().startsWith("enchant") && !nodeName.equalsIgnoreCase("enchant2")))
				{
					continue;
				}
				
				if ("set".equalsIgnoreCase(nodeName))
				{
					parseBeanSet(n, _currentSkill.enchsets2[i], _currentSkill.sets.length);
				}
				else if ("enchant2".equalsIgnoreCase(nodeName))
				{
					parseBeanSet(n, _currentSkill.enchsets2[i], i + 1);
				}
				else if (!nodeName.toLowerCase().endsWith("effects"))
				{
					if (n.getAttributes().getNamedItem("subLevel2Values") != null)
					{
						parseInlineLevelTable(n, _currentSkill.enchsets2[i], i + 1, 2);
					}
					else
					{
						parseElementValue(n, _currentSkill.enchsets2[i], _currentSkill.sets.length);
					}
				}
			}
		}
		
		// --- Enchant Route 3 ---
		for (int i = 0; i < enchantLevels3; i++)
		{
			_currentSkill.enchsets3[i] = new StatSet();
			_currentSkill.enchsets3[i].set("skill_id", _currentSkill.id);
			_currentSkill.enchsets3[i].set("level", i + 181); // C6 adaptation.
			_currentSkill.enchsets3[i].set("name", _currentSkill.name);
			
			for (n = first; n != null; n = n.getNextSibling())
			{
				if (n.getNodeType() != Node.ELEMENT_NODE)
				{
					continue;
				}
				
				final String nodeName = n.getNodeName();
				if (nodeName.equalsIgnoreCase("table") || nodeName.equalsIgnoreCase("conditions") || (nodeName.toLowerCase().startsWith("enchant") && !nodeName.equalsIgnoreCase("enchant3")))
				{
					continue;
				}
				
				if ("set".equalsIgnoreCase(nodeName))
				{
					parseBeanSet(n, _currentSkill.enchsets3[i], _currentSkill.sets.length);
				}
				else if ("enchant3".equalsIgnoreCase(nodeName))
				{
					parseBeanSet(n, _currentSkill.enchsets3[i], i + 1);
				}
				else if (!nodeName.toLowerCase().endsWith("effects"))
				{
					if (n.getAttributes().getNamedItem("subLevel3Values") != null)
					{
						parseInlineLevelTable(n, _currentSkill.enchsets3[i], i + 1, 3);
					}
					else
					{
						parseElementValue(n, _currentSkill.enchsets3[i], _currentSkill.sets.length);
					}
				}
			}
		}
		
		// --- Enchant Route 4 ---
		for (int i = 0; i < enchantLevels4; i++)
		{
			_currentSkill.enchsets4[i] = new StatSet();
			_currentSkill.enchsets4[i].set("skill_id", _currentSkill.id);
			_currentSkill.enchsets4[i].set("level", i + 221); // C6 adaptation.
			_currentSkill.enchsets4[i].set("name", _currentSkill.name);
			
			for (n = first; n != null; n = n.getNextSibling())
			{
				if (n.getNodeType() != Node.ELEMENT_NODE)
				{
					continue;
				}
				
				final String nodeName = n.getNodeName();
				if (nodeName.equalsIgnoreCase("table") || nodeName.equalsIgnoreCase("conditions") || (nodeName.toLowerCase().startsWith("enchant") && !nodeName.equalsIgnoreCase("enchant4")))
				{
					continue;
				}
				
				if ("set".equalsIgnoreCase(nodeName))
				{
					parseBeanSet(n, _currentSkill.enchsets4[i], _currentSkill.sets.length);
				}
				else if ("enchant4".equalsIgnoreCase(nodeName))
				{
					parseBeanSet(n, _currentSkill.enchsets4[i], i + 1);
				}
				else if (!nodeName.toLowerCase().endsWith("effects"))
				{
					if (n.getAttributes().getNamedItem("subLevel4Values") != null)
					{
						parseInlineLevelTable(n, _currentSkill.enchsets4[i], i + 1, 4);
					}
					else
					{
						parseElementValue(n, _currentSkill.enchsets4[i], _currentSkill.sets.length);
					}
				}
			}
		}
		
		// --- Enchant Route 5 ---
		for (int i = 0; i < enchantLevels5; i++)
		{
			_currentSkill.enchsets5[i] = new StatSet();
			_currentSkill.enchsets5[i].set("skill_id", _currentSkill.id);
			_currentSkill.enchsets5[i].set("level", i + 261); // C6 adaptation.
			_currentSkill.enchsets5[i].set("name", _currentSkill.name);
			
			for (n = first; n != null; n = n.getNextSibling())
			{
				if (n.getNodeType() != Node.ELEMENT_NODE)
				{
					continue;
				}
				
				final String nodeName = n.getNodeName();
				if (nodeName.equalsIgnoreCase("table") || nodeName.equalsIgnoreCase("conditions") || (nodeName.toLowerCase().startsWith("enchant") && !nodeName.equalsIgnoreCase("enchant5")))
				{
					continue;
				}
				
				if ("set".equalsIgnoreCase(nodeName))
				{
					parseBeanSet(n, _currentSkill.enchsets5[i], _currentSkill.sets.length);
				}
				else if ("enchant5".equalsIgnoreCase(nodeName))
				{
					parseBeanSet(n, _currentSkill.enchsets5[i], i + 1);
				}
				else if (!nodeName.toLowerCase().endsWith("effects"))
				{
					if (n.getAttributes().getNamedItem("subLevel5Values") != null)
					{
						parseInlineLevelTable(n, _currentSkill.enchsets5[i], i + 1, 5);
					}
					else
					{
						parseElementValue(n, _currentSkill.enchsets5[i], _currentSkill.sets.length);
					}
				}
			}
		}
		
		// --- Enchant Route 6 ---
		for (int i = 0; i < enchantLevels6; i++)
		{
			_currentSkill.enchsets6[i] = new StatSet();
			_currentSkill.enchsets6[i].set("skill_id", _currentSkill.id);
			_currentSkill.enchsets6[i].set("level", i + 301); // C6 adaptation.
			_currentSkill.enchsets6[i].set("name", _currentSkill.name);
			
			for (n = first; n != null; n = n.getNextSibling())
			{
				if (n.getNodeType() != Node.ELEMENT_NODE)
				{
					continue;
				}
				
				final String nodeName = n.getNodeName();
				if (nodeName.equalsIgnoreCase("table") || nodeName.equalsIgnoreCase("conditions") || (nodeName.toLowerCase().startsWith("enchant") && !nodeName.equalsIgnoreCase("enchant6")))
				{
					continue;
				}
				
				if ("set".equalsIgnoreCase(nodeName))
				{
					parseBeanSet(n, _currentSkill.enchsets6[i], _currentSkill.sets.length);
				}
				else if ("enchant6".equalsIgnoreCase(nodeName))
				{
					parseBeanSet(n, _currentSkill.enchsets6[i], i + 1);
				}
				else if (!nodeName.toLowerCase().endsWith("effects"))
				{
					if (n.getAttributes().getNamedItem("subLevel6Values") != null)
					{
						parseInlineLevelTable(n, _currentSkill.enchsets6[i], i + 1, 6);
					}
					else
					{
						parseElementValue(n, _currentSkill.enchsets6[i], _currentSkill.sets.length);
					}
				}
			}
		}
		
		// --- Enchant Route 7 ---
		for (int i = 0; i < enchantLevels7; i++)
		{
			_currentSkill.enchsets7[i] = new StatSet();
			_currentSkill.enchsets7[i].set("skill_id", _currentSkill.id);
			_currentSkill.enchsets7[i].set("level", i + 341); // C6 adaptation.
			_currentSkill.enchsets7[i].set("name", _currentSkill.name);
			
			for (n = first; n != null; n = n.getNextSibling())
			{
				if (n.getNodeType() != Node.ELEMENT_NODE)
				{
					continue;
				}
				
				final String nodeName = n.getNodeName();
				if (nodeName.equalsIgnoreCase("table") || nodeName.equalsIgnoreCase("conditions") || (nodeName.toLowerCase().startsWith("enchant") && !nodeName.equalsIgnoreCase("enchant7")))
				{
					continue;
				}
				
				if ("set".equalsIgnoreCase(nodeName))
				{
					parseBeanSet(n, _currentSkill.enchsets7[i], _currentSkill.sets.length);
				}
				else if ("enchant7".equalsIgnoreCase(nodeName))
				{
					parseBeanSet(n, _currentSkill.enchsets7[i], i + 1);
				}
				else if (!nodeName.toLowerCase().endsWith("effects"))
				{
					if (n.getAttributes().getNamedItem("subLevel7Values") != null)
					{
						parseInlineLevelTable(n, _currentSkill.enchsets7[i], i + 1, 7);
					}
					else
					{
						parseElementValue(n, _currentSkill.enchsets7[i], _currentSkill.sets.length);
					}
				}
			}
		}
		
		// --- Enchant Route 8 ---
		for (int i = 0; i < enchantLevels8; i++)
		{
			_currentSkill.enchsets8[i] = new StatSet();
			_currentSkill.enchsets8[i].set("skill_id", _currentSkill.id);
			_currentSkill.enchsets8[i].set("level", i + 381); // C6 adaptation.
			_currentSkill.enchsets8[i].set("name", _currentSkill.name);
			
			for (n = first; n != null; n = n.getNextSibling())
			{
				if (n.getNodeType() != Node.ELEMENT_NODE)
				{
					continue;
				}
				
				final String nodeName = n.getNodeName();
				if (nodeName.equalsIgnoreCase("table") || nodeName.equalsIgnoreCase("conditions") || (nodeName.toLowerCase().startsWith("enchant") && !nodeName.equalsIgnoreCase("enchant8")))
				{
					continue;
				}
				
				if ("set".equalsIgnoreCase(nodeName))
				{
					parseBeanSet(n, _currentSkill.enchsets8[i], _currentSkill.sets.length);
				}
				else if ("enchant8".equalsIgnoreCase(nodeName))
				{
					parseBeanSet(n, _currentSkill.enchsets8[i], i + 1);
				}
				else if (!nodeName.toLowerCase().endsWith("effects"))
				{
					if (n.getAttributes().getNamedItem("subLevel8Values") != null)
					{
						parseInlineLevelTable(n, _currentSkill.enchsets8[i], i + 1, 8);
					}
					else
					{
						parseElementValue(n, _currentSkill.enchsets8[i], _currentSkill.sets.length);
					}
				}
			}
		}
		
		if (_currentSkill.enchsets8.length != enchantLevels8)
		{
			throw new RuntimeException("Skill id=" + skillId + " number of levels missmatch, " + enchantLevels8 + " levels expected");
		}
		
		makeSkills();
		for (int i = 0; i < lastLvl; i++)
		{
			_currentSkill.currentLevel = i;
			for (n = first; n != null; n = n.getNextSibling())
			{
				if ("conditions".equalsIgnoreCase(n.getNodeName()))
				{
					final Condition condition = parseCondition(n.getFirstChild(), _currentSkill.currentSkills.get(i));
					final Node msg = n.getAttributes().getNamedItem("msg");
					final Node msgId = n.getAttributes().getNamedItem("msgId");
					if ((condition != null) && (msg != null))
					{
						condition.setMessage(msg.getNodeValue());
					}
					else if ((condition != null) && (msgId != null))
					{
						condition.setMessageId(Integer.decode(getValue(msgId.getNodeValue(), null)));
						final Node addName = n.getAttributes().getNamedItem("addName");
						if ((addName != null) && (Integer.decode(getValue(msgId.getNodeValue(), null)) > 0))
						{
							condition.addName();
						}
					}
					
					_currentSkill.currentSkills.get(i).attach(condition, false);
				}
				else if ("effects".equalsIgnoreCase(n.getNodeName()))
				{
					parseTemplate(n, _currentSkill.currentSkills.get(i));
				}
				else if ("startEffects".equalsIgnoreCase(n.getNodeName()))
				{
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.START);
				}
				else if ("channelingEffects".equalsIgnoreCase(n.getNodeName()))
				{
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.CHANNELING);
				}
				else if ("pveEffects".equalsIgnoreCase(n.getNodeName()))
				{
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.PVE);
				}
				else if ("pvpEffects".equalsIgnoreCase(n.getNodeName()))
				{
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.PVP);
				}
				else if ("endEffects".equalsIgnoreCase(n.getNodeName()))
				{
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.END);
				}
				else if ("selfEffects".equalsIgnoreCase(n.getNodeName()))
				{
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.SELF);
				}
			}
		}
		
		for (int i = lastLvl; i < (lastLvl + enchantLevels1); i++)
		{
			_currentSkill.currentLevel = i - lastLvl;
			boolean foundConditions = false;
			boolean foundEffects = false;
			boolean foundChannelingEffects = false;
			boolean foundStartEffects = false;
			boolean foundPveEffects = false;
			boolean foundPvpEffects = false;
			boolean foundEndEffects = false;
			boolean foundSelfEffects = false;
			for (n = first; n != null; n = n.getNextSibling())
			{
				if ("enchant1conditions".equalsIgnoreCase(n.getNodeName()))
				{
					foundConditions = true;
					final Condition condition = parseCondition(n.getFirstChild(), _currentSkill.currentSkills.get(i));
					final Node msg = n.getAttributes().getNamedItem("msg");
					final Node msgId = n.getAttributes().getNamedItem("msgId");
					if ((condition != null) && (msg != null))
					{
						condition.setMessage(msg.getNodeValue());
					}
					else if ((condition != null) && (msgId != null))
					{
						condition.setMessageId(Integer.decode(getValue(msgId.getNodeValue(), null)));
						final Node addName = n.getAttributes().getNamedItem("addName");
						if ((addName != null) && (Integer.decode(getValue(msgId.getNodeValue(), null)) > 0))
						{
							condition.addName();
						}
					}
					
					_currentSkill.currentSkills.get(i).attach(condition, false);
				}
				else if ("enchant1effects".equalsIgnoreCase(n.getNodeName()))
				{
					foundEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i));
				}
				else if ("enchant1startEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundStartEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.START);
				}
				else if ("enchant1channelingEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundChannelingEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.CHANNELING);
				}
				else if ("enchant1pveEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundPveEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.PVE);
				}
				else if ("enchant1pvpEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundPvpEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.PVP);
				}
				else if ("enchant1endEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundEndEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.END);
				}
				else if ("enchant1selfEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundSelfEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.SELF);
				}
			}
			
			// If none found, the enchanted skill will take effects from maxLvL of norm skill
			if (!foundConditions || !foundEffects || !foundChannelingEffects || !foundStartEffects || !foundPveEffects || !foundPvpEffects || !foundEndEffects || !foundSelfEffects)
			{
				_currentSkill.currentLevel = lastLvl - 1;
				for (n = first; n != null; n = n.getNextSibling())
				{
					if (!foundConditions && "conditions".equalsIgnoreCase(n.getNodeName()))
					{
						final Condition condition = parseCondition(n.getFirstChild(), _currentSkill.currentSkills.get(i));
						final Node msg = n.getAttributes().getNamedItem("msg");
						final Node msgId = n.getAttributes().getNamedItem("msgId");
						if ((condition != null) && (msg != null))
						{
							condition.setMessage(msg.getNodeValue());
						}
						else if ((condition != null) && (msgId != null))
						{
							condition.setMessageId(Integer.decode(getValue(msgId.getNodeValue(), null)));
							final Node addName = n.getAttributes().getNamedItem("addName");
							if ((addName != null) && (Integer.decode(getValue(msgId.getNodeValue(), null)) > 0))
							{
								condition.addName();
							}
						}
						
						_currentSkill.currentSkills.get(i).attach(condition, false);
					}
					else if (!foundEffects && "effects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i));
					}
					else if (!foundStartEffects && "startEffects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.START);
					}
					else if (!foundChannelingEffects && "channelingEffects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.CHANNELING);
					}
					else if (!foundPveEffects && "pveEffects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.PVE);
					}
					else if (!foundPvpEffects && "pvpEffects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.PVP);
					}
					else if (!foundEndEffects && "endEffects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.END);
					}
					else if (!foundSelfEffects && "selfEffects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.SELF);
					}
				}
			}
		}
		
		for (int i = lastLvl + enchantLevels1; i < (lastLvl + enchantLevels1 + enchantLevels2); i++)
		{
			boolean foundConditions = false;
			boolean foundEffects = false;
			boolean foundChannelingEffects = false;
			boolean foundStartEffects = false;
			boolean foundPveEffects = false;
			boolean foundPvpEffects = false;
			boolean foundEndEffects = false;
			boolean foundSelfEffects = false;
			_currentSkill.currentLevel = i - lastLvl - enchantLevels1;
			for (n = first; n != null; n = n.getNextSibling())
			{
				if ("enchant2conditions".equalsIgnoreCase(n.getNodeName()))
				{
					foundConditions = true;
					final Condition condition = parseCondition(n.getFirstChild(), _currentSkill.currentSkills.get(i));
					final Node msg = n.getAttributes().getNamedItem("msg");
					final Node msgId = n.getAttributes().getNamedItem("msgId");
					if ((condition != null) && (msg != null))
					{
						condition.setMessage(msg.getNodeValue());
					}
					else if ((condition != null) && (msgId != null))
					{
						condition.setMessageId(Integer.decode(getValue(msgId.getNodeValue(), null)));
						final Node addName = n.getAttributes().getNamedItem("addName");
						if ((addName != null) && (Integer.decode(getValue(msgId.getNodeValue(), null)) > 0))
						{
							condition.addName();
						}
					}
					
					_currentSkill.currentSkills.get(i).attach(condition, false);
				}
				else if ("enchant2effects".equalsIgnoreCase(n.getNodeName()))
				{
					foundEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i));
				}
				else if ("enchant2startEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundStartEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.START);
				}
				else if ("enchant2channelingEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundChannelingEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.CHANNELING);
				}
				else if ("enchant2pveEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundPveEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.PVE);
				}
				else if ("enchant2pvpEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundPvpEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.PVP);
				}
				else if ("enchant2endEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundEndEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.END);
				}
				else if ("enchant2selfEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundSelfEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.SELF);
				}
			}
			
			// If none found, the enchanted skill will take effects from maxLvL of norm skill
			if (!foundConditions || !foundEffects || !foundChannelingEffects || !foundStartEffects || !foundPveEffects || !foundPvpEffects || !foundEndEffects || !foundSelfEffects)
			{
				_currentSkill.currentLevel = lastLvl - 1;
				for (n = first; n != null; n = n.getNextSibling())
				{
					if (!foundConditions && "conditions".equalsIgnoreCase(n.getNodeName()))
					{
						final Condition condition = parseCondition(n.getFirstChild(), _currentSkill.currentSkills.get(i));
						final Node msg = n.getAttributes().getNamedItem("msg");
						final Node msgId = n.getAttributes().getNamedItem("msgId");
						if ((condition != null) && (msg != null))
						{
							condition.setMessage(msg.getNodeValue());
						}
						else if ((condition != null) && (msgId != null))
						{
							condition.setMessageId(Integer.decode(getValue(msgId.getNodeValue(), null)));
							final Node addName = n.getAttributes().getNamedItem("addName");
							if ((addName != null) && (Integer.decode(getValue(msgId.getNodeValue(), null)) > 0))
							{
								condition.addName();
							}
						}
						
						_currentSkill.currentSkills.get(i).attach(condition, false);
					}
					else if (!foundEffects && "effects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i));
					}
					else if (!foundChannelingEffects && "channelingEffects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.CHANNELING);
					}
					else if (!foundPveEffects && "pveEffects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.PVE);
					}
					else if (!foundPvpEffects && "pvpEffects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.PVP);
					}
					else if (!foundEndEffects && "endEffects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.END);
					}
					else if (!foundSelfEffects && "selfEffects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.SELF);
					}
				}
			}
		}
		
		for (int i = lastLvl + enchantLevels1 + enchantLevels2; i < (lastLvl + enchantLevels1 + enchantLevels2 + enchantLevels3); i++)
		{
			boolean foundConditions = false;
			boolean foundEffects = false;
			boolean foundChannelingEffects = false;
			boolean foundStartEffects = false;
			boolean foundPveEffects = false;
			boolean foundPvpEffects = false;
			boolean foundEndEffects = false;
			boolean foundSelfEffects = false;
			_currentSkill.currentLevel = i - lastLvl - enchantLevels1 - enchantLevels2;
			for (n = first; n != null; n = n.getNextSibling())
			{
				if ("enchant3conditions".equalsIgnoreCase(n.getNodeName()))
				{
					foundConditions = true;
					final Condition condition = parseCondition(n.getFirstChild(), _currentSkill.currentSkills.get(i));
					final Node msg = n.getAttributes().getNamedItem("msg");
					final Node msgId = n.getAttributes().getNamedItem("msgId");
					if ((condition != null) && (msg != null))
					{
						condition.setMessage(msg.getNodeValue());
					}
					else if ((condition != null) && (msgId != null))
					{
						condition.setMessageId(Integer.decode(getValue(msgId.getNodeValue(), null)));
						final Node addName = n.getAttributes().getNamedItem("addName");
						if ((addName != null) && (Integer.decode(getValue(msgId.getNodeValue(), null)) > 0))
						{
							condition.addName();
						}
					}
					
					_currentSkill.currentSkills.get(i).attach(condition, false);
				}
				else if ("enchant3effects".equalsIgnoreCase(n.getNodeName()))
				{
					foundEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i));
				}
				else if ("enchant3startEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundStartEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.START);
				}
				else if ("enchant3channelingEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundChannelingEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.CHANNELING);
				}
				else if ("enchant3pveEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundPveEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.PVE);
				}
				else if ("enchant3pvpEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundPvpEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.PVP);
				}
				else if ("enchant3endEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundEndEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.END);
				}
				else if ("enchant3selfEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundSelfEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.SELF);
				}
			}
			
			// If none found, the enchanted skill will take effects from maxLvL of norm skill
			if (!foundConditions || !foundEffects || !foundChannelingEffects || !foundStartEffects || !foundPveEffects || !foundPvpEffects || !foundEndEffects || !foundSelfEffects)
			{
				_currentSkill.currentLevel = lastLvl - 1;
				for (n = first; n != null; n = n.getNextSibling())
				{
					if (!foundConditions && "conditions".equalsIgnoreCase(n.getNodeName()))
					{
						final Condition condition = parseCondition(n.getFirstChild(), _currentSkill.currentSkills.get(i));
						final Node msg = n.getAttributes().getNamedItem("msg");
						final Node msgId = n.getAttributes().getNamedItem("msgId");
						if ((condition != null) && (msg != null))
						{
							condition.setMessage(msg.getNodeValue());
						}
						else if ((condition != null) && (msgId != null))
						{
							condition.setMessageId(Integer.decode(getValue(msgId.getNodeValue(), null)));
							final Node addName = n.getAttributes().getNamedItem("addName");
							if ((addName != null) && (Integer.decode(getValue(msgId.getNodeValue(), null)) > 0))
							{
								condition.addName();
							}
						}
						
						_currentSkill.currentSkills.get(i).attach(condition, false);
					}
					else if (!foundEffects && "effects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i));
					}
					else if (!foundStartEffects && "startEffects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.START);
					}
					else if (!foundChannelingEffects && "channelingEffects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.CHANNELING);
					}
					else if (!foundPveEffects && "pveEffects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.PVE);
					}
					else if (!foundPvpEffects && "pvpEffects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.PVP);
					}
					else if (!foundEndEffects && "endEffects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.END);
					}
					else if (!foundSelfEffects && "selfEffects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.SELF);
					}
				}
			}
		}
		
		for (int i = lastLvl + enchantLevels1 + enchantLevels2 + enchantLevels3; i < (lastLvl + enchantLevels1 + enchantLevels2 + enchantLevels3 + enchantLevels4); i++)
		{
			boolean foundConditions = false;
			boolean foundEffects = false;
			boolean foundChannelingEffects = false;
			boolean foundStartEffects = false;
			boolean foundPveEffects = false;
			boolean foundPvpEffects = false;
			boolean foundEndEffects = false;
			boolean foundSelfEffects = false;
			_currentSkill.currentLevel = i - lastLvl - enchantLevels1 - enchantLevels2 - enchantLevels3;
			for (n = first; n != null; n = n.getNextSibling())
			{
				if ("enchant4conditions".equalsIgnoreCase(n.getNodeName()))
				{
					foundConditions = true;
					final Condition condition = parseCondition(n.getFirstChild(), _currentSkill.currentSkills.get(i));
					final Node msg = n.getAttributes().getNamedItem("msg");
					final Node msgId = n.getAttributes().getNamedItem("msgId");
					if ((condition != null) && (msg != null))
					{
						condition.setMessage(msg.getNodeValue());
					}
					else if ((condition != null) && (msgId != null))
					{
						condition.setMessageId(Integer.decode(getValue(msgId.getNodeValue(), null)));
						final Node addName = n.getAttributes().getNamedItem("addName");
						if ((addName != null) && (Integer.decode(getValue(msgId.getNodeValue(), null)) > 0))
						{
							condition.addName();
						}
					}
					
					_currentSkill.currentSkills.get(i).attach(condition, false);
				}
				else if ("enchant4effects".equalsIgnoreCase(n.getNodeName()))
				{
					foundEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i));
				}
				else if ("enchant4startEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundStartEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.START);
				}
				else if ("enchant4channelingEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundChannelingEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.CHANNELING);
				}
				else if ("enchant4pveEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundPveEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.PVE);
				}
				else if ("enchant4pvpEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundPvpEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.PVP);
				}
				else if ("enchant4endEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundEndEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.END);
				}
				else if ("enchant4selfEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundSelfEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.SELF);
				}
			}
			
			// If none found, the enchanted skill will take effects from maxLvL of norm skill
			if (!foundConditions || !foundEffects || !foundChannelingEffects || !foundStartEffects || !foundPveEffects || !foundPvpEffects || !foundEndEffects || !foundSelfEffects)
			{
				_currentSkill.currentLevel = lastLvl - 1;
				for (n = first; n != null; n = n.getNextSibling())
				{
					if (!foundConditions && "conditions".equalsIgnoreCase(n.getNodeName()))
					{
						final Condition condition = parseCondition(n.getFirstChild(), _currentSkill.currentSkills.get(i));
						final Node msg = n.getAttributes().getNamedItem("msg");
						final Node msgId = n.getAttributes().getNamedItem("msgId");
						if ((condition != null) && (msg != null))
						{
							condition.setMessage(msg.getNodeValue());
						}
						else if ((condition != null) && (msgId != null))
						{
							condition.setMessageId(Integer.decode(getValue(msgId.getNodeValue(), null)));
							final Node addName = n.getAttributes().getNamedItem("addName");
							if ((addName != null) && (Integer.decode(getValue(msgId.getNodeValue(), null)) > 0))
							{
								condition.addName();
							}
						}
						
						_currentSkill.currentSkills.get(i).attach(condition, false);
					}
					else if (!foundEffects && "effects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i));
					}
					else if (!foundStartEffects && "startEffects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.START);
					}
					else if (!foundChannelingEffects && "channelingEffects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.CHANNELING);
					}
					else if (!foundPveEffects && "pveEffects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.PVE);
					}
					else if (!foundPvpEffects && "pvpEffects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.PVP);
					}
					else if (!foundEndEffects && "endEffects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.END);
					}
					else if (!foundSelfEffects && "selfEffects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.SELF);
					}
				}
			}
		}
		
		for (int i = lastLvl + enchantLevels1 + enchantLevels2 + enchantLevels3 + enchantLevels4; i < (lastLvl + enchantLevels1 + enchantLevels2 + enchantLevels3 + enchantLevels4 + enchantLevels5); i++)
		{
			boolean foundConditions = false;
			boolean foundEffects = false;
			boolean foundChannelingEffects = false;
			boolean foundStartEffects = false;
			boolean foundPveEffects = false;
			boolean foundPvpEffects = false;
			boolean foundEndEffects = false;
			boolean foundSelfEffects = false;
			_currentSkill.currentLevel = i - lastLvl - enchantLevels1 - enchantLevels2 - enchantLevels3 - enchantLevels4;
			for (n = first; n != null; n = n.getNextSibling())
			{
				if ("enchant5conditions".equalsIgnoreCase(n.getNodeName()))
				{
					foundConditions = true;
					final Condition condition = parseCondition(n.getFirstChild(), _currentSkill.currentSkills.get(i));
					final Node msg = n.getAttributes().getNamedItem("msg");
					final Node msgId = n.getAttributes().getNamedItem("msgId");
					if ((condition != null) && (msg != null))
					{
						condition.setMessage(msg.getNodeValue());
					}
					else if ((condition != null) && (msgId != null))
					{
						condition.setMessageId(Integer.decode(getValue(msgId.getNodeValue(), null)));
						final Node addName = n.getAttributes().getNamedItem("addName");
						if ((addName != null) && (Integer.decode(getValue(msgId.getNodeValue(), null)) > 0))
						{
							condition.addName();
						}
					}
					
					_currentSkill.currentSkills.get(i).attach(condition, false);
				}
				else if ("enchant5effects".equalsIgnoreCase(n.getNodeName()))
				{
					foundEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i));
				}
				else if ("enchant5startEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundStartEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.START);
				}
				else if ("enchant5channelingEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundChannelingEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.CHANNELING);
				}
				else if ("enchant5pveEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundPveEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.PVE);
				}
				else if ("enchant5pvpEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundPvpEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.PVP);
				}
				else if ("enchant5endEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundEndEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.END);
				}
				else if ("enchant5selfEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundSelfEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.SELF);
				}
			}
			
			// If none found, the enchanted skill will take effects from maxLvL of norm skill
			if (!foundConditions || !foundEffects || !foundChannelingEffects || !foundStartEffects || !foundPveEffects || !foundPvpEffects || !foundEndEffects || !foundSelfEffects)
			{
				_currentSkill.currentLevel = lastLvl - 1;
				for (n = first; n != null; n = n.getNextSibling())
				{
					if (!foundConditions && "conditions".equalsIgnoreCase(n.getNodeName()))
					{
						final Condition condition = parseCondition(n.getFirstChild(), _currentSkill.currentSkills.get(i));
						final Node msg = n.getAttributes().getNamedItem("msg");
						final Node msgId = n.getAttributes().getNamedItem("msgId");
						if ((condition != null) && (msg != null))
						{
							condition.setMessage(msg.getNodeValue());
						}
						else if ((condition != null) && (msgId != null))
						{
							condition.setMessageId(Integer.decode(getValue(msgId.getNodeValue(), null)));
							final Node addName = n.getAttributes().getNamedItem("addName");
							if ((addName != null) && (Integer.decode(getValue(msgId.getNodeValue(), null)) > 0))
							{
								condition.addName();
							}
						}
						
						_currentSkill.currentSkills.get(i).attach(condition, false);
					}
					else if (!foundEffects && "effects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i));
					}
					else if (!foundStartEffects && "startEffects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.START);
					}
					else if (!foundChannelingEffects && "channelingEffects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.CHANNELING);
					}
					else if (!foundPveEffects && "pveEffects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.PVE);
					}
					else if (!foundPvpEffects && "pvpEffects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.PVP);
					}
					else if (!foundEndEffects && "endEffects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.END);
					}
					else if (!foundSelfEffects && "selfEffects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.SELF);
					}
				}
			}
		}
		
		for (int i = lastLvl + enchantLevels1 + enchantLevels2 + enchantLevels3 + enchantLevels4 + enchantLevels5; i < (lastLvl + enchantLevels1 + enchantLevels2 + enchantLevels3 + enchantLevels4 + enchantLevels5 + enchantLevels6); i++)
		{
			boolean foundConditions = false;
			boolean foundEffects = false;
			boolean foundChannelingEffects = false;
			boolean foundStartEffects = false;
			boolean foundPveEffects = false;
			boolean foundPvpEffects = false;
			boolean foundEndEffects = false;
			boolean foundSelfEffects = false;
			_currentSkill.currentLevel = i - lastLvl - enchantLevels1 - enchantLevels2 - enchantLevels3 - enchantLevels4 - enchantLevels5;
			for (n = first; n != null; n = n.getNextSibling())
			{
				if ("enchant6conditions".equalsIgnoreCase(n.getNodeName()))
				{
					foundConditions = true;
					final Condition condition = parseCondition(n.getFirstChild(), _currentSkill.currentSkills.get(i));
					final Node msg = n.getAttributes().getNamedItem("msg");
					final Node msgId = n.getAttributes().getNamedItem("msgId");
					if ((condition != null) && (msg != null))
					{
						condition.setMessage(msg.getNodeValue());
					}
					else if ((condition != null) && (msgId != null))
					{
						condition.setMessageId(Integer.decode(getValue(msgId.getNodeValue(), null)));
						final Node addName = n.getAttributes().getNamedItem("addName");
						if ((addName != null) && (Integer.decode(getValue(msgId.getNodeValue(), null)) > 0))
						{
							condition.addName();
						}
					}
					
					_currentSkill.currentSkills.get(i).attach(condition, false);
				}
				else if ("enchant6effects".equalsIgnoreCase(n.getNodeName()))
				{
					foundEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i));
				}
				else if ("enchant6startEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundStartEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.START);
				}
				else if ("enchant6channelingEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundChannelingEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.CHANNELING);
				}
				else if ("enchant6pveEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundPveEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.PVE);
				}
				else if ("enchant6pvpEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundPvpEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.PVP);
				}
				else if ("enchant6endEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundEndEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.END);
				}
				else if ("enchant6selfEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundSelfEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.SELF);
				}
			}
			
			// If none found, the enchanted skill will take effects from maxLvL of norm skill
			if (!foundConditions || !foundEffects || !foundChannelingEffects || !foundStartEffects || !foundPveEffects || !foundPvpEffects || !foundEndEffects || !foundSelfEffects)
			{
				_currentSkill.currentLevel = lastLvl - 1;
				for (n = first; n != null; n = n.getNextSibling())
				{
					if (!foundConditions && "conditions".equalsIgnoreCase(n.getNodeName()))
					{
						final Condition condition = parseCondition(n.getFirstChild(), _currentSkill.currentSkills.get(i));
						final Node msg = n.getAttributes().getNamedItem("msg");
						final Node msgId = n.getAttributes().getNamedItem("msgId");
						if ((condition != null) && (msg != null))
						{
							condition.setMessage(msg.getNodeValue());
						}
						else if ((condition != null) && (msgId != null))
						{
							condition.setMessageId(Integer.decode(getValue(msgId.getNodeValue(), null)));
							final Node addName = n.getAttributes().getNamedItem("addName");
							if ((addName != null) && (Integer.decode(getValue(msgId.getNodeValue(), null)) > 0))
							{
								condition.addName();
							}
						}
						
						_currentSkill.currentSkills.get(i).attach(condition, false);
					}
					else if (!foundEffects && "effects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i));
					}
					else if (!foundStartEffects && "startEffects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.START);
					}
					else if (!foundChannelingEffects && "channelingEffects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.CHANNELING);
					}
					else if (!foundPveEffects && "pveEffects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.PVE);
					}
					else if (!foundPvpEffects && "pvpEffects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.PVP);
					}
					else if (!foundEndEffects && "endEffects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.END);
					}
					else if (!foundSelfEffects && "selfEffects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.SELF);
					}
				}
			}
		}
		
		for (int i = lastLvl + enchantLevels1 + enchantLevels2 + enchantLevels3 + enchantLevels4 + enchantLevels5 + enchantLevels6; i < (lastLvl + enchantLevels1 + enchantLevels2 + enchantLevels3 + enchantLevels4 + enchantLevels5 + enchantLevels6 + enchantLevels7); i++)
		{
			boolean foundConditions = false;
			boolean foundEffects = false;
			boolean foundChannelingEffects = false;
			boolean foundStartEffects = false;
			boolean foundPveEffects = false;
			boolean foundPvpEffects = false;
			boolean foundEndEffects = false;
			boolean foundSelfEffects = false;
			_currentSkill.currentLevel = i - lastLvl - enchantLevels1 - enchantLevels2 - enchantLevels3 - enchantLevels4 - enchantLevels5 - enchantLevels6;
			for (n = first; n != null; n = n.getNextSibling())
			{
				if ("enchant7conditions".equalsIgnoreCase(n.getNodeName()))
				{
					foundConditions = true;
					final Condition condition = parseCondition(n.getFirstChild(), _currentSkill.currentSkills.get(i));
					final Node msg = n.getAttributes().getNamedItem("msg");
					final Node msgId = n.getAttributes().getNamedItem("msgId");
					if ((condition != null) && (msg != null))
					{
						condition.setMessage(msg.getNodeValue());
					}
					else if ((condition != null) && (msgId != null))
					{
						condition.setMessageId(Integer.decode(getValue(msgId.getNodeValue(), null)));
						final Node addName = n.getAttributes().getNamedItem("addName");
						if ((addName != null) && (Integer.decode(getValue(msgId.getNodeValue(), null)) > 0))
						{
							condition.addName();
						}
					}
					
					_currentSkill.currentSkills.get(i).attach(condition, false);
				}
				else if ("enchant7effects".equalsIgnoreCase(n.getNodeName()))
				{
					foundEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i));
				}
				else if ("enchant7startEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundStartEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.START);
				}
				else if ("enchant7channelingEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundChannelingEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.CHANNELING);
				}
				else if ("enchant7pveEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundPveEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.PVE);
				}
				else if ("enchant7pvpEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundPvpEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.PVP);
				}
				else if ("enchant7endEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundEndEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.END);
				}
				else if ("enchant7selfEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundSelfEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.SELF);
				}
			}
			
			// If none found, the enchanted skill will take effects from maxLvL of norm skill
			if (!foundConditions || !foundEffects || !foundChannelingEffects || !foundStartEffects || !foundPveEffects || !foundPvpEffects || !foundEndEffects || !foundSelfEffects)
			{
				_currentSkill.currentLevel = lastLvl - 1;
				for (n = first; n != null; n = n.getNextSibling())
				{
					if (!foundConditions && "conditions".equalsIgnoreCase(n.getNodeName()))
					{
						final Condition condition = parseCondition(n.getFirstChild(), _currentSkill.currentSkills.get(i));
						final Node msg = n.getAttributes().getNamedItem("msg");
						final Node msgId = n.getAttributes().getNamedItem("msgId");
						if ((condition != null) && (msg != null))
						{
							condition.setMessage(msg.getNodeValue());
						}
						else if ((condition != null) && (msgId != null))
						{
							condition.setMessageId(Integer.decode(getValue(msgId.getNodeValue(), null)));
							final Node addName = n.getAttributes().getNamedItem("addName");
							if ((addName != null) && (Integer.decode(getValue(msgId.getNodeValue(), null)) > 0))
							{
								condition.addName();
							}
						}
						
						_currentSkill.currentSkills.get(i).attach(condition, false);
					}
					else if (!foundEffects && "effects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i));
					}
					else if (!foundChannelingEffects && "startEffects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.START);
					}
					else if (!foundChannelingEffects && "channelingEffects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.CHANNELING);
					}
					else if (!foundPveEffects && "pveEffects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.PVE);
					}
					else if (!foundPvpEffects && "pvpEffects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.PVP);
					}
					else if (!foundEndEffects && "endEffects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.END);
					}
					else if (!foundSelfEffects && "selfEffects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.SELF);
					}
				}
			}
		}
		
		for (int i = lastLvl + enchantLevels1 + enchantLevels2 + enchantLevels3 + enchantLevels4 + enchantLevels5 + enchantLevels6 + enchantLevels7; i < (lastLvl + enchantLevels1 + enchantLevels2 + enchantLevels3 + enchantLevels4 + enchantLevels5 + enchantLevels6 + enchantLevels7 + enchantLevels8); i++)
		{
			boolean foundConditions = false;
			boolean foundEffects = false;
			boolean foundChannelingEffects = false;
			boolean foundStartEffects = false;
			boolean foundPveEffects = false;
			boolean foundPvpEffects = false;
			boolean foundEndEffects = false;
			boolean foundSelfEffects = false;
			_currentSkill.currentLevel = i - lastLvl - enchantLevels1 - enchantLevels2 - enchantLevels3 - enchantLevels4 - enchantLevels5 - enchantLevels6 - enchantLevels7;
			for (n = first; n != null; n = n.getNextSibling())
			{
				if ("enchant8conditions".equalsIgnoreCase(n.getNodeName()))
				{
					foundConditions = true;
					final Condition condition = parseCondition(n.getFirstChild(), _currentSkill.currentSkills.get(i));
					final Node msg = n.getAttributes().getNamedItem("msg");
					final Node msgId = n.getAttributes().getNamedItem("msgId");
					if ((condition != null) && (msg != null))
					{
						condition.setMessage(msg.getNodeValue());
					}
					else if ((condition != null) && (msgId != null))
					{
						condition.setMessageId(Integer.decode(getValue(msgId.getNodeValue(), null)));
						final Node addName = n.getAttributes().getNamedItem("addName");
						if ((addName != null) && (Integer.decode(getValue(msgId.getNodeValue(), null)) > 0))
						{
							condition.addName();
						}
					}
					
					_currentSkill.currentSkills.get(i).attach(condition, false);
				}
				else if ("enchant8effects".equalsIgnoreCase(n.getNodeName()))
				{
					foundEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i));
				}
				else if ("enchant8startEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundStartEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.START);
				}
				else if ("enchant8channelingEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundChannelingEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.CHANNELING);
				}
				else if ("enchant8pveEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundPveEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.PVE);
				}
				else if ("enchant8pvpEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundPvpEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.PVP);
				}
				else if ("enchant8endEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundEndEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.END);
				}
				else if ("enchant8selfEffects".equalsIgnoreCase(n.getNodeName()))
				{
					foundSelfEffects = true;
					parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.SELF);
				}
			}
			
			// If none found, the enchanted skill will take effects from maxLvL of norm skill
			if (!foundConditions || !foundEffects || !foundChannelingEffects || !foundStartEffects || !foundPveEffects || !foundPvpEffects || !foundEndEffects || !foundSelfEffects)
			{
				_currentSkill.currentLevel = lastLvl - 1;
				for (n = first; n != null; n = n.getNextSibling())
				{
					if (!foundConditions && "conditions".equalsIgnoreCase(n.getNodeName()))
					{
						final Condition condition = parseCondition(n.getFirstChild(), _currentSkill.currentSkills.get(i));
						final Node msg = n.getAttributes().getNamedItem("msg");
						final Node msgId = n.getAttributes().getNamedItem("msgId");
						if ((condition != null) && (msg != null))
						{
							condition.setMessage(msg.getNodeValue());
						}
						else if ((condition != null) && (msgId != null))
						{
							condition.setMessageId(Integer.decode(getValue(msgId.getNodeValue(), null)));
							final Node addName = n.getAttributes().getNamedItem("addName");
							if ((addName != null) && (Integer.decode(getValue(msgId.getNodeValue(), null)) > 0))
							{
								condition.addName();
							}
						}
						
						_currentSkill.currentSkills.get(i).attach(condition, false);
					}
					else if (!foundEffects && "effects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i));
					}
					else if (!foundStartEffects && "startEffects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.START);
					}
					else if (!foundChannelingEffects && "channelingEffects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.CHANNELING);
					}
					else if (!foundPveEffects && "pveEffects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.PVE);
					}
					else if (!foundPvpEffects && "pvpEffects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.PVP);
					}
					else if (!foundEndEffects && "endEffects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.END);
					}
					else if (!foundSelfEffects && "selfEffects".equalsIgnoreCase(n.getNodeName()))
					{
						parseTemplate(n, _currentSkill.currentSkills.get(i), EffectScope.SELF);
					}
				}
			}
		}
		
		_currentSkill.skills.addAll(_currentSkill.currentSkills);
	}
	
	/**
	 * Parse an element with inline level table attributes.<br>
	 * Example: <mpConsume levelTable="40 45 50 55 60" /> for normal levels Example: <mpConsume subLevelTable1="140 145 150 155 160" /> for enchant route 1
	 * @param n the XML node to parse
	 * @param set the StatSet to store the data into
	 * @param level the current level (1-based)
	 * @param enchantRoute the enchant route (0 for normal levels, 1-8 for enchant routes)
	 */
	protected void parseInlineLevelTable(Node n, StatSet set, int level, int enchantRoute)
	{
		final String nodeName = n.getNodeName().trim();
		final NamedNodeMap attrs = n.getAttributes();
		
		// Check main level table attribute.
		if (enchantRoute == 0)
		{
			final Node levelTableNode = attrs.getNamedItem("levelValues");
			if (levelTableNode != null)
			{
				final String tableValue = levelTableNode.getNodeValue().trim();
				final String[] values = tableValue.split("\\s+");
				if ((values.length > 0) && (level > 0) && (level <= values.length))
				{
					set.set(nodeName, StringUtil.parseValue(values[level - 1]));
					return;
				}
			}
		}
		
		// Check enchant level table attributes.
		if (enchantRoute > 0)
		{
			final Node subLevelNode = attrs.getNamedItem("subLevel" + enchantRoute + "Values");
			if (subLevelNode != null)
			{
				final String tableValue = subLevelNode.getNodeValue().trim();
				final String[] values = tableValue.split("\\s+");
				if ((values.length > 0) && (level > 0) && (level <= values.length))
				{
					set.set(nodeName, StringUtil.parseValue(values[level - 1]));
					return;
				}
			}
		}
	}
	
	/**
	 * Parse an element with inline level table attributes for normal levels.
	 * @param n the XML node to parse
	 * @param set the StatSet to store the data into
	 * @param level the current level (1-based)
	 */
	protected void parseInlineLevelTable(Node n, StatSet set, int level)
	{
		parseInlineLevelTable(n, set, level, 0);
	}
	
	private void makeSkills()
	{
		int count = 0;
		_currentSkill.currentSkills = new ArrayList<>(_currentSkill.sets.length + _currentSkill.enchsets1.length + _currentSkill.enchsets2.length + _currentSkill.enchsets3.length + _currentSkill.enchsets4.length + _currentSkill.enchsets5.length + _currentSkill.enchsets6.length + _currentSkill.enchsets7.length + _currentSkill.enchsets8.length);
		StatSet set;
		for (int i = 0; i < _currentSkill.sets.length; i++)
		{
			set = _currentSkill.sets[i];
			try
			{
				_currentSkill.currentSkills.add(i, new Skill(set));
				count++;
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "Skill id=" + set.getInt("skill_id") + "level" + set.getInt("level"), e);
			}
		}
		
		int count2 = count;
		for (int i = 0; i < _currentSkill.enchsets1.length; i++)
		{
			set = _currentSkill.enchsets1[i];
			try
			{
				_currentSkill.currentSkills.add(count2 + i, new Skill(set));
				count++;
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "Skill id=" + set.getInt("skill_id") + "level" + set.getInt("level"), e);
			}
		}
		
		count2 = count;
		for (int i = 0; i < _currentSkill.enchsets2.length; i++)
		{
			set = _currentSkill.enchsets2[i];
			try
			{
				_currentSkill.currentSkills.add(count2 + i, new Skill(set));
				count++;
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "Skill id=" + set.getInt("skill_id") + "level" + set.getInt("level"), e);
			}
		}
		
		count2 = count;
		for (int i = 0; i < _currentSkill.enchsets3.length; i++)
		{
			set = _currentSkill.enchsets3[i];
			try
			{
				_currentSkill.currentSkills.add(count2 + i, new Skill(set));
				count++;
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "Skill id=" + set.getInt("skill_id") + "level" + set.getInt("level"), e);
			}
		}
		
		count2 = count;
		for (int i = 0; i < _currentSkill.enchsets4.length; i++)
		{
			set = _currentSkill.enchsets4[i];
			try
			{
				_currentSkill.currentSkills.add(count2 + i, new Skill(set));
				count++;
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "Skill id=" + set.getInt("skill_id") + "level" + set.getInt("level"), e);
			}
		}
		
		count2 = count;
		for (int i = 0; i < _currentSkill.enchsets5.length; i++)
		{
			set = _currentSkill.enchsets5[i];
			try
			{
				_currentSkill.currentSkills.add(count2 + i, new Skill(set));
				count++;
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "Skill id=" + set.getInt("skill_id") + "level" + set.getInt("level"), e);
			}
		}
		
		count2 = count;
		for (int i = 0; i < _currentSkill.enchsets6.length; i++)
		{
			set = _currentSkill.enchsets6[i];
			try
			{
				_currentSkill.currentSkills.add(count2 + i, new Skill(set));
				count++;
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "Skill id=" + set.getInt("skill_id") + "level" + set.getInt("level"), e);
			}
		}
		
		count2 = count;
		for (int i = 0; i < _currentSkill.enchsets7.length; i++)
		{
			set = _currentSkill.enchsets7[i];
			try
			{
				_currentSkill.currentSkills.add(count2 + i, new Skill(set));
				count++;
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "Skill id=" + set.getInt("skill_id") + "level" + set.getInt("level"), e);
			}
		}
		
		count2 = count;
		for (int i = 0; i < _currentSkill.enchsets8.length; i++)
		{
			set = _currentSkill.enchsets8[i];
			try
			{
				_currentSkill.currentSkills.add(count2 + i, new Skill(set));
				count++;
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "Skill id=" + set.getInt("skill_id") + "level" + set.getInt("level"), e);
			}
		}
	}
	
	public List<Skill> getSkills()
	{
		return _skillsInFile;
	}
}
