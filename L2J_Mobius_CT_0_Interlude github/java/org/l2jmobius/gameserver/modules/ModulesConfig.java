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

import java.io.File;

import org.l2jmobius.commons.util.ConfigReader;
import org.l2jmobius.gameserver.config.ServerConfig;

/**
 * Platform-level configuration for the module framework itself, read from {@code config/Modules.ini}. This is separate
 * from any individual module's {@code config/module.ini}: it sets where modules live and a single global master switch
 * for the whole framework. When the file is absent the defaults apply, so a server with no modules behaves exactly as
 * stock.
 */
public class ModulesConfig
{
	private static final String CONFIG_FILE = "./config/Modules.ini";

	private static boolean ENABLED;
	private static File MODULES_ROOT;

	public static void load()
	{
		final ConfigReader config = new ConfigReader(CONFIG_FILE);
		ENABLED = config.getBoolean("EnableModules", true);

		final String root = config.getString("ModulesRoot", "").trim();
		if (root.isEmpty())
		{
			MODULES_ROOT = new File(ServerConfig.DATAPACK_ROOT, "modules");
		}
		else
		{
			MODULES_ROOT = new File(root.replace('\\', '/'));
		}
	}

	/**
	 * @return {@code true} when the framework is allowed to discover and enable modules at all
	 */
	public static boolean isEnabled()
	{
		return ENABLED;
	}

	/**
	 * @return the directory scanned for installed modules
	 */
	public static File getModulesRoot()
	{
		return MODULES_ROOT;
	}
}
