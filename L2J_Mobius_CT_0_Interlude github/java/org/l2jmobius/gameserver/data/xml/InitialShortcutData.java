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
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.MacroType;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.actor.enums.player.ShortcutType;
import org.l2jmobius.gameserver.model.actor.holders.player.Macro;
import org.l2jmobius.gameserver.model.actor.holders.player.MacroCmd;
import org.l2jmobius.gameserver.model.actor.holders.player.Shortcut;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.serverpackets.ShortcutRegister;

/**
 * Manages initial shortcut and macro configurations for new player characters.<br>
 * Loads configuration data from XML files and provides shortcuts based on player class.
 * <ul>
 * <li>Class-specific shortcut configurations for different player classes.</li>
 * <li>Global shortcut settings available to all players.</li>
 * <li>Macro preset definitions with commands and parameters.</li>
 * <li>Automatic shortcut registration for skills, items, and macros.</li>
 * </ul>
 * @author Zoey76, Mobius
 */
public class InitialShortcutData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(InitialShortcutData.class.getName());
	
	// Shortcut Data Storage.
	private final Map<PlayerClass, List<Shortcut>> _initialShortcutData = new EnumMap<>(PlayerClass.class);
	private final List<Shortcut> _initialGlobalShortcutList = new ArrayList<>();
	
	// Macro Configuration.
	private final Map<Integer, Macro> _macroPresets = new HashMap<>();
	
	protected InitialShortcutData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_initialShortcutData.clear();
		_initialGlobalShortcutList.clear();
		
		parseDatapackFile("data/stats/players/initialShortcuts.xml");
		
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _initialGlobalShortcutList.size() + " initial global shortcut data.");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _initialShortcutData.size() + " initial shortcut data.");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _macroPresets.size() + " macro presets.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node node = document.getFirstChild(); node != null; node = node.getNextSibling())
		{
			if ("list".equals(node.getNodeName()))
			{
				for (Node dataNode = node.getFirstChild(); dataNode != null; dataNode = dataNode.getNextSibling())
				{
					switch (dataNode.getNodeName())
					{
						case "shortcuts":
						{
							NamedNodeMap attributes = dataNode.getAttributes();
							final Node classIdNode = attributes.getNamedItem("classId");
							final List<Shortcut> shortcutList = new ArrayList<>();
							
							for (Node childNode = dataNode.getFirstChild(); childNode != null; childNode = childNode.getNextSibling())
							{
								if ("page".equals(childNode.getNodeName()))
								{
									attributes = childNode.getAttributes();
									final int pageId = parseInteger(attributes, "pageId");
									for (Node slotNode = childNode.getFirstChild(); slotNode != null; slotNode = slotNode.getNextSibling())
									{
										if ("slot".equals(slotNode.getNodeName()))
										{
											final NamedNodeMap slotAttributes = slotNode.getAttributes();
											final int slotId = parseInteger(slotAttributes, "slotId");
											final ShortcutType shortcutType = parseEnum(slotAttributes, ShortcutType.class, "shortcutType");
											final int shortcutId = parseInteger(slotAttributes, "shortcutId");
											final int shortcutLevel = parseInteger(slotAttributes, "shortcutLevel", 0);
											shortcutList.add(new Shortcut(slotId, pageId, shortcutType, shortcutId, shortcutLevel));
										}
									}
								}
							}
							
							if (classIdNode != null)
							{
								_initialShortcutData.put(PlayerClass.getPlayerClass(Integer.parseInt(classIdNode.getNodeValue())), shortcutList);
							}
							else
							{
								_initialGlobalShortcutList.addAll(shortcutList);
							}
							break;
						}
						case "macros":
						{
							for (Node childNode = dataNode.getFirstChild(); childNode != null; childNode = childNode.getNextSibling())
							{
								if ("macro".equals(childNode.getNodeName()))
								{
									NamedNodeMap attributes = childNode.getAttributes();
									if (!parseBoolean(attributes, "enabled", true))
									{
										continue;
									}
									
									final int macroId = parseInteger(attributes, "macroId");
									final int icon = parseInteger(attributes, "icon");
									final String name = parseString(attributes, "name");
									final String description = parseString(attributes, "description");
									final String acronym = parseString(attributes, "acronym");
									final List<MacroCmd> commands = new ArrayList<>(1);
									int entryIndex = 0;
									
									for (Node commandNode = childNode.getFirstChild(); commandNode != null; commandNode = commandNode.getNextSibling())
									{
										if ("command".equals(commandNode.getNodeName()))
										{
											attributes = commandNode.getAttributes();
											final MacroType type = parseEnum(attributes, MacroType.class, "type");
											int parameterOne = 0;
											int parameterTwo = 0;
											final String commandText = commandNode.getTextContent();
											
											switch (type)
											{
												case SKILL:
												{
													parameterOne = parseInteger(attributes, "skillId"); // Skill ID.
													parameterTwo = parseInteger(attributes, "skillLevel", 0); // Skill level.
													break;
												}
												case ACTION:
												{
													parameterOne = parseInteger(attributes, "actionId"); // Not handled by client.
													break;
												}
												case TEXT:
												{
													// Text commands have no numeric parameters.
													break;
												}
												case SHORTCUT:
												{
													parameterOne = parseInteger(attributes, "page"); // Page.
													parameterTwo = parseInteger(attributes, "slot", 0); // Slot.
													break;
												}
												case ITEM:
												{
													parameterOne = parseInteger(attributes, "itemId"); // Not handled by client.
													break;
												}
												case DELAY:
												{
													parameterOne = parseInteger(attributes, "delay"); // Delay in seconds.
													break;
												}
											}
											
											commands.add(new MacroCmd(entryIndex++, type, parameterOne, parameterTwo, commandText));
										}
									}
									
									_macroPresets.put(macroId, new Macro(macroId, icon, name, description, acronym, commands));
								}
							}
							break;
						}
					}
				}
			}
		}
	}
	
	/**
	 * Registers all available shortcuts for the specified player including global and class-specific shortcuts.<br>
	 * Validates item availability, skill knowledge, and macro definitions before registration.
	 * @param player the {@link Player} for whom to register the shortcuts.
	 */
	public void registerAllShortcuts(Player player)
	{
		if (player == null)
		{
			return;
		}
		
		// Register global shortcuts.
		for (Shortcut shortcut : _initialGlobalShortcutList)
		{
			int shortcutId = shortcut.getId();
			switch (shortcut.getType())
			{
				case ITEM:
				{
					final Item item = player.getInventory().getItemByItemId(shortcutId);
					if (item == null)
					{
						continue;
					}
					
					shortcutId = item.getObjectId();
					break;
				}
				case SKILL:
				{
					if (!player.getSkills().containsKey(shortcutId))
					{
						continue;
					}
					break;
				}
				case MACRO:
				{
					final Macro macro = _macroPresets.get(shortcutId);
					if (macro == null)
					{
						continue;
					}
					
					player.registerMacro(macro);
					break;
				}
			}
			
			// Register shortcut.
			final Shortcut newShortcut = new Shortcut(shortcut.getSlot(), shortcut.getPage(), shortcut.getType(), shortcutId, shortcut.getLevel());
			player.sendPacket(new ShortcutRegister(newShortcut));
			player.registerShortcut(newShortcut);
		}
		
		// Register class specific shortcuts.
		if (_initialShortcutData.containsKey(player.getPlayerClass()))
		{
			for (Shortcut shortcut : _initialShortcutData.get(player.getPlayerClass()))
			{
				int shortcutId = shortcut.getId();
				switch (shortcut.getType())
				{
					case ITEM:
					{
						final Item item = player.getInventory().getItemByItemId(shortcutId);
						if (item == null)
						{
							continue;
						}
						
						shortcutId = item.getObjectId();
						break;
					}
					case SKILL:
					{
						if (!player.getSkills().containsKey(shortcut.getId()))
						{
							continue;
						}
						break;
					}
					case MACRO:
					{
						final Macro macro = _macroPresets.get(shortcutId);
						if (macro == null)
						{
							continue;
						}
						
						player.registerMacro(macro);
						break;
					}
				}
				
				// Register shortcut.
				final Shortcut newShortcut = new Shortcut(shortcut.getSlot(), shortcut.getPage(), shortcut.getType(), shortcutId, shortcut.getLevel());
				player.sendPacket(new ShortcutRegister(newShortcut));
				player.registerShortcut(newShortcut);
			}
		}
	}
	
	public static InitialShortcutData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final InitialShortcutData INSTANCE = new InitialShortcutData();
	}
}
