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

import org.l2jmobius.commons.util.ConfigReader;

/**
 * Generic typed access to a module's own configuration, loaded from the fixed file {@code config/module.ini} inside the
 * module directory. A module reads every tunable through this rather than editing any central config class. When the
 * file is absent, every read returns the supplied default, so a module with no settings still works.
 */
public class ModuleConfig
{
	private final ConfigReader _reader;

	public ModuleConfig(ConfigReader reader)
	{
		_reader = reader;
	}

	public boolean getBoolean(String key, boolean defaultValue)
	{
		return _reader.getBoolean(key, defaultValue);
	}

	public int getInt(String key, int defaultValue)
	{
		return _reader.getInt(key, defaultValue);
	}

	public long getLong(String key, long defaultValue)
	{
		return _reader.getLong(key, defaultValue);
	}

	public double getDouble(String key, double defaultValue)
	{
		return _reader.getDouble(key, defaultValue);
	}

	public String getString(String key, String defaultValue)
	{
		return _reader.getString(key, defaultValue);
	}
}
