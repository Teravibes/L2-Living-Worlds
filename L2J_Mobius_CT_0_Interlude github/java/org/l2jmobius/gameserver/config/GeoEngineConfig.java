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
package org.l2jmobius.gameserver.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.l2jmobius.commons.util.ConfigReader;

/**
 * This class loads all the geo engine related configurations.
 * @author Mobius
 */
public class GeoEngineConfig
{
	// File
	private static final String GEOENGINE_CONFIG_FILE = "./config/GeoEngine.ini";
	
	// Constants
	public static Path GEODATA_PATH;
	public static Path PATHNODE_PATH;
	public static Path GEOEDIT_PATH;
	public static int PATHFINDING;
	public static String PATHFIND_BUFFERS;
	public static float LOW_WEIGHT;
	public static float MEDIUM_WEIGHT;
	public static float HIGH_WEIGHT;
	public static boolean ADVANCED_DIAGONAL_STRATEGY;
	public static boolean AVOID_OBSTRUCTED_PATH_NODES;
	public static float DIAGONAL_WEIGHT;
	public static int MAX_POSTFILTER_PASSES;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(GEOENGINE_CONFIG_FILE);
		GEODATA_PATH = Paths.get(ServerConfig.DATAPACK_ROOT.getPath() + "/" + config.getString("GeoDataPath", "geodata"));
		PATHNODE_PATH = Paths.get(ServerConfig.DATAPACK_ROOT.getPath() + "/" + config.getString("PathnodePath", "pathnode"));
		GEOEDIT_PATH = Paths.get(ServerConfig.DATAPACK_ROOT.getPath() + "/" + config.getString("GeoEditPath", "saves"));
		PATHFINDING = config.getInt("PathFinding", 0);
		PATHFIND_BUFFERS = config.getString("PathFindBuffers", "100x6;128x6;192x6;256x4;320x4;384x4;500x2");
		LOW_WEIGHT = config.getFloat("LowWeight", 0.5f);
		MEDIUM_WEIGHT = config.getFloat("MediumWeight", 2);
		HIGH_WEIGHT = config.getFloat("HighWeight", 3);
		ADVANCED_DIAGONAL_STRATEGY = config.getBoolean("AdvancedDiagonalStrategy", true);
		AVOID_OBSTRUCTED_PATH_NODES = config.getBoolean("AvoidObstructedPathNodes", true);
		DIAGONAL_WEIGHT = config.getFloat("DiagonalWeight", 0.707f);
		MAX_POSTFILTER_PASSES = config.getInt("MaxPostfilterPasses", 3);
	}
}
