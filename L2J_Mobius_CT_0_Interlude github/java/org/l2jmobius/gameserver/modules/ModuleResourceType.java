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
package org.l2jmobius.gameserver.modules;

/**
 * The kinds of datapack resource a module can contribute through its manifest {@code resources} block. This is the
 * shared vocabulary between a manifest, the {@link ModuleResourceRegistry}, and the stock data loaders that read the
 * registry.
 * <p>
 * Declaring the full set here is only naming, not a promise that every loader is wired: the framework wires one loader
 * at a time as a real module needs it. Milestone 3 wires {@link #ITEMS}; the others are recognised in a manifest and
 * carried in the registry, but no loader consumes them yet, so declaring one has no effect until its loader is wired.
 */
public enum ModuleResourceType
{
	ITEMS("items"),
	SKILLS("skills"),
	NPCS("npcs"),
	SPAWNS("spawns"),
	HTML("html"),
	MULTISELL("multisell");

	private final String _key;

	ModuleResourceType(String key)
	{
		_key = key;
	}

	/**
	 * @return the lowercase key used for this type in a {@code module.json} {@code resources} block
	 */
	public String getKey()
	{
		return _key;
	}

	/**
	 * @param key a key from a manifest {@code resources} block
	 * @return the matching type, or {@code null} if no type uses that key
	 */
	public static ModuleResourceType fromKey(String key)
	{
		for (ModuleResourceType type : values())
		{
			if (type._key.equals(key))
			{
				return type;
			}
		}
		return null;
	}
}
