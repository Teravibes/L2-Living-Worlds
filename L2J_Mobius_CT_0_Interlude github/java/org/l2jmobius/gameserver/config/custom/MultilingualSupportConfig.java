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
package org.l2jmobius.gameserver.config.custom;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.l2jmobius.commons.util.ConfigReader;
import org.l2jmobius.gameserver.config.GeneralConfig;

/**
 * This class loads all the custom multilingual support related configurations.
 * @author Mobius
 */
public class MultilingualSupportConfig
{
	private static final Logger LOGGER = Logger.getLogger(MultilingualSupportConfig.class.getName());
	
	// File
	private static final String MULTILANGUAL_SUPPORT_CONFIG_FILE = "./config/Custom/MultilingualSupport.ini";
	
	// Constants
	public static String MULTILANG_DEFAULT;
	public static boolean MULTILANG_ENABLE;
	public static List<String> MULTILANG_ALLOWED = new ArrayList<>();
	public static boolean MULTILANG_VOICED_ALLOW;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(MULTILANGUAL_SUPPORT_CONFIG_FILE);
		MULTILANG_DEFAULT = config.getString("MultiLangDefault", "en").toLowerCase();
		
		MULTILANG_ENABLE = config.getBoolean("MultiLangEnable", false);
		if (MULTILANG_ENABLE)
		{
			GeneralConfig.CHECK_HTML_ENCODING = false;
		}
		
		final String[] allowed = config.getString("MultiLangAllowed", MULTILANG_DEFAULT).split(";");
		MULTILANG_ALLOWED = new ArrayList<>(allowed.length);
		for (String lang : allowed)
		{
			MULTILANG_ALLOWED.add(lang.toLowerCase());
		}
		
		if (!MULTILANG_ALLOWED.contains(MULTILANG_DEFAULT))
		{
			LOGGER.warning("MultiLang[MultilingualSupportConfig.load()]: default language: " + MULTILANG_DEFAULT + " is not in allowed list!");
		}
		
		MULTILANG_VOICED_ALLOW = config.getBoolean("MultiLangVoiceCommand", true);
	}
}
