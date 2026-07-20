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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.conditions.Condition;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemSkillType;
import org.l2jmobius.gameserver.model.item.holders.ExtractableProduct;
import org.l2jmobius.gameserver.model.item.type.ArmorType;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.model.stats.functions.FuncEnchant;
import org.l2jmobius.gameserver.model.stats.functions.FuncTemplate;

/**
 * @author mkizub, JIV, Mobius
 */
public class DocumentItem extends DocumentBase implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(DocumentItem.class.getName());
	
	private DocumentItemDataHolder _currentItem = null;
	private final List<ItemTemplate> _itemsInFile = new ArrayList<>();
	
	private class DocumentItemDataHolder
	{
		public DocumentItemDataHolder()
		{
		}
		
		int id;
		String type;
		StatSet set;
		int currentLevel;
		ItemTemplate item;
	}
	
	public DocumentItem(File file)
	{
		super(file);
	}
	
	@Override
	protected StatSet getStatSet()
	{
		return _currentItem.set;
	}
	
	@Override
	protected String getTableValue(String name)
	{
		return _tables.get(name)[_currentItem.currentLevel];
	}
	
	@Override
	protected String getTableValue(String name, int idx)
	{
		return _tables.get(name)[idx - 1];
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
					if ("item".equalsIgnoreCase(d.getNodeName()))
					{
						try
						{
							_currentItem = new DocumentItemDataHolder();
							parseItem(d);
							_itemsInFile.add(_currentItem.item);
							resetTable();
						}
						catch (Exception e)
						{
							LOGGER.log(Level.WARNING, "Cannot create item " + _currentItem.id, e);
						}
					}
				}
			}
		}
	}
	
	private void parseItem(Node node) throws InvocationTargetException
	{
		Node n = node;
		final int itemId = Integer.parseInt(n.getAttributes().getNamedItem("id").getNodeValue());
		final String className = n.getAttributes().getNamedItem("type").getNodeValue();
		final String itemName = n.getAttributes().getNamedItem("name").getNodeValue();
		
		_currentItem.id = itemId;
		_currentItem.type = className;
		_currentItem.set = new StatSet();
		_currentItem.set.set("item_id", itemId);
		_currentItem.set.set("name", itemName);
		
		final Node first = n.getFirstChild();
		for (n = first; n != null; n = n.getNextSibling())
		{
			// Check if this is a direct element value node (like <icon>value</icon>).
			if ((n.getNodeType() == Node.ELEMENT_NODE) && (n.getFirstChild() != null) && (n.getAttributes().getLength() == 0))
			{
				// First check if this node has any child ELEMENT nodes (not just text/whitespace).
				boolean hasChildElements = false;
				for (Node child = n.getFirstChild(); child != null; child = child.getNextSibling())
				{
					if (child.getNodeType() == Node.ELEMENT_NODE)
					{
						hasChildElements = true;
						break;
					}
				}
				
				// Only parse as element value if it has NO child elements AND has text content.
				if (!hasChildElements && (n.getFirstChild().getNodeType() == Node.TEXT_NODE))
				{
					// Parse element value, regardless of whether the item is created or not.
					parseElementValue(n, _currentItem.set, 1);
					
					// If the item is already created, update it with the modified StatSet.
					if (_currentItem.item != null)
					{
						_currentItem.item.set(_currentItem.set);
					}
					continue;
				}
			}
			
			if ("table".equalsIgnoreCase(n.getNodeName()))
			{
				if (_currentItem.item != null)
				{
					throw new IllegalStateException("Item created but table node found! Item " + itemId);
				}
				
				parseTable(n);
			}
			else if ("set".equalsIgnoreCase(n.getNodeName()))
			{
				if (_currentItem.item != null)
				{
					throw new IllegalStateException("Item created but set node found! Item " + itemId);
				}
				
				parseBeanSet(n, _currentItem.set, 1);
			}
			else if ("stats".equalsIgnoreCase(n.getNodeName()))
			{
				makeItem();
				for (Node b = n.getFirstChild(); b != null; b = b.getNextSibling())
				{
					if ("stat".equals(b.getNodeName()))
					{
						final Stat type = Stat.valueOfXml(b.getAttributes().getNamedItem("type").getNodeValue());
						final double value = Double.parseDouble(b.getTextContent());
						
						switch (type)
						{
							case POWER_ATTACK:
							case POWER_ATTACK_SPEED:
							case POWER_ATTACK_RANGE:
							case CRITICAL_RATE:
							case MAGIC_ATTACK:
							case SHIELD_DEFENCE:
							case SHIELD_RATE:
							{
								_currentItem.item.attach(new FuncTemplate(null, null, "set", 0x00, type, value));
								break;
							}
							case ACCURACY_COMBAT:
							case EVASION_RATE:
							case RANDOM_DAMAGE:
							case POWER_DEFENCE:
							case MAGIC_DEFENCE:
							case MAX_MP:
							case FIRE_POWER:
							case WATER_POWER:
							case WIND_POWER:
							case EARTH_POWER:
							case HOLY_POWER:
							case DARK_POWER:
							case FIRE_RES:
							case WATER_RES:
							case WIND_RES:
							case EARTH_RES:
							case HOLY_RES:
							case DARK_RES:
							{
								_currentItem.item.attach(new FuncTemplate(null, null, "add", 0x00, type, value));
								break;
							}
							case MAGIC_SUCCESS_RES:
							{
								_currentItem.item.attach(new FuncTemplate(null, null, "mul", 0x00, type, value));
								break;
							}
							default:
							{
								LOGGER.warning("Unhandled stat type " + type);
								break;
							}
						}
					}
				}
				
				// Enable enchant functions.
				if (_currentItem.item.isArmor())
				{
					if (!_currentItem.item.hasFunction(FuncEnchant.class))
					{
						if (_currentItem.item.getItemType() == ArmorType.SHIELD)
						{
							_currentItem.item.attach(new FuncTemplate(null, null, "enchant", 0x00, Stat.SHIELD_DEFENCE, 0d));
						}
						else if ((_currentItem.item.getType1() == ItemTemplate.TYPE1_WEAPON_RING_EARRING_NECKLACE) && (_currentItem.item.getType2() == ItemTemplate.TYPE2_ACCESSORY))
						{
							_currentItem.item.attach(new FuncTemplate(null, null, "enchant", 0x00, Stat.MAGIC_DEFENCE, 0d));
						}
						else
						{
							_currentItem.item.attach(new FuncTemplate(null, null, "enchant", 0x00, Stat.POWER_DEFENCE, 0d));
						}
					}
				}
				else if (_currentItem.item.isWeapon())
				{
					if (!_currentItem.item.hasFunction(FuncEnchant.class))
					{
						_currentItem.item.attach(new FuncTemplate(null, null, "enchant", 0x00, Stat.POWER_ATTACK, 0d));
						_currentItem.item.attach(new FuncTemplate(null, null, "enchant", 0x00, Stat.MAGIC_ATTACK, 0d));
					}
				}
			}
			else if ("skills".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node b = n.getFirstChild(); b != null; b = b.getNextSibling())
				{
					if ("skill".equalsIgnoreCase(b.getNodeName()))
					{
						final int id = parseInteger(b.getAttributes(), "id");
						final int level = parseInteger(b.getAttributes(), "level");
						final int chance = parseInteger(b.getAttributes(), "type_chance", 100);
						final ItemSkillType type = parseEnum(b.getAttributes(), ItemSkillType.class, "type", ItemSkillType.NORMAL);
						switch (type)
						{
							case NORMAL:
							{
								if (_currentItem.set.getString("item_skill", null) == null)
								{
									_currentItem.set.set("item_skill", id + "-" + level);
								}
								else
								{
									_currentItem.set.set("item_skill", _currentItem.set.getString("item_skill") + ";" + id + "-" + level);
								}
								break;
							}
							case ON_ENCHANT_4:
							{
								_currentItem.set.set("enchant4_skill", id + "-" + level);
								break;
							}
							case ON_UNEQUIP:
							{
								_currentItem.set.set("unequip_skill", id + "-" + level);
								break;
							}
							case ON_CRITICAL_SKILL:
							{
								_currentItem.set.set("oncrit_skill", id + "-" + level);
								_currentItem.set.set("oncrit_chance", chance);
								break;
							}
							case ON_MAGIC_SKILL:
							{
								_currentItem.set.set("onmagic_skill", id + "-" + level);
								_currentItem.set.set("onmagic_chance", chance);
								break;
							}
						}
					}
				}
			}
			else if ("capsuled_items".equalsIgnoreCase(n.getNodeName()))
			{
				makeItem();
				for (Node b = n.getFirstChild(); b != null; b = b.getNextSibling())
				{
					if ("item".equals(b.getNodeName()))
					{
						final int id = parseInteger(b.getAttributes(), "id");
						final int min = parseInteger(b.getAttributes(), "min");
						final int max = parseInteger(b.getAttributes(), "max");
						final double chance = parseDouble(b.getAttributes(), "chance");
						final int minEnchant = parseInteger(b.getAttributes(), "minEnchant", 0);
						final int maxEnchant = parseInteger(b.getAttributes(), "maxEnchant", 0);
						_currentItem.item.addCapsuledItem(new ExtractableProduct(id, min, max, chance, minEnchant, maxEnchant));
					}
				}
			}
			else if ("conditions".equalsIgnoreCase(n.getNodeName()))
			{
				makeItem();
				final Condition condition = parseCondition(n.getFirstChild(), _currentItem.item);
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
				
				_currentItem.item.attach(condition);
			}
		}
		
		// bah! in this point item doesn't have to be still created
		makeItem();
	}
	
	private void makeItem() throws InvocationTargetException
	{
		// If item exists just reload the data.
		if (_currentItem.item != null)
		{
			_currentItem.item.set(_currentItem.set);
			return;
		}
		
		try
		{
			final Constructor<?> itemClass = Class.forName("org.l2jmobius.gameserver.model.item." + _currentItem.type).getConstructor(StatSet.class);
			_currentItem.item = (ItemTemplate) itemClass.newInstance(_currentItem.set);
		}
		catch (Exception e)
		{
			throw new InvocationTargetException(e);
		}
	}
	
	public List<ItemTemplate> getItemList()
	{
		return _itemsInFile;
	}
	
	@Override
	public void load()
	{
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
	}
}
