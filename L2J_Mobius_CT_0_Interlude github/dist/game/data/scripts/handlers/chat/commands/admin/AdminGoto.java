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
package handlers.chat.commands.admin;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.handler.AdminCommandHandler;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;

/**
 * @author Mobius
 */
public class AdminGoto implements IAdminCommandHandler
{
	private static final Map<String, Location> LOCATIONS = new HashMap<>();
	static
	{
		// Town
		LOCATIONS.put("gludio", new Location(-14225, 123540, -3121));
		LOCATIONS.put("gludin", new Location(-83063, 150791, -3133));
		LOCATIONS.put("giran", new Location(82698, 148638, -3473));
		LOCATIONS.put("giranharbor", new Location(47938, 186864, -3420));
		LOCATIONS.put("dion", new Location(18748, 145437, -3132));
		LOCATIONS.put("hunter", new Location(116589, 76268, -2734));
		LOCATIONS.put("huntervillage", new Location(116589, 76268, -2734));
		LOCATIONS.put("hunters", new Location(116589, 76268, -2734));
		LOCATIONS.put("huntersvillage", new Location(116589, 76268, -2734));
		LOCATIONS.put("oren", new Location(82321, 55139, -1529));
		LOCATIONS.put("aden", new Location(147450, 27064, -2208));
		LOCATIONS.put("ti", new Location(-82687, 243157, -3734));
		LOCATIONS.put("talkingisland", new Location(-82687, 243157, -3734));
		LOCATIONS.put("dwarf", new Location(116551, -182493, -1525));
		LOCATIONS.put("dwarfvillage", new Location(116551, -182493, -1525));
		LOCATIONS.put("dwarven", new Location(116551, -182493, -1525));
		LOCATIONS.put("dwarvenvillage", new Location(116551, -182493, -1525));
		LOCATIONS.put("orc", new Location(-44211, -113521, -241));
		LOCATIONS.put("orcvillage", new Location(-44211, -113521, -241));
		LOCATIONS.put("de", new Location(12428, 16551, -4588));
		LOCATIONS.put("delf", new Location(12428, 16551, -4588));
		LOCATIONS.put("darkelf", new Location(12428, 16551, -4588));
		LOCATIONS.put("darkelfvillage", new Location(12428, 16551, -4588));
		LOCATIONS.put("darkelven", new Location(12428, 16551, -4588));
		LOCATIONS.put("darkelvenvillage", new Location(12428, 16551, -4588));
		LOCATIONS.put("elf", new Location(45873, 49288, -3064));
		LOCATIONS.put("elfvillage", new Location(45873, 49288, -3064));
		LOCATIONS.put("elven", new Location(45873, 49288, -3064));
		LOCATIONS.put("elvenvillage", new Location(45873, 49288, -3064));
		LOCATIONS.put("floran", new Location(17144, 170156, -3502));
		LOCATIONS.put("heine", new Location(111115, 219017, -3547));
		LOCATIONS.put("rune", new Location(44070, -50243, -796));
		LOCATIONS.put("runeharbor", new Location(36839, -38435, -3640));
		LOCATIONS.put("goddard", new Location(147725, -56517, -2780));
		LOCATIONS.put("sc", new Location(87358, -141982, -1341));
		LOCATIONS.put("schuttgart", new Location(87358, -141982, -1341));
		// World
		LOCATIONS.put("arena", new Location(12312, 182752, -3558));
		LOCATIONS.put("racetrack", new Location(12312, 182752, -3558));
		LOCATIONS.put("racetrackarena", new Location(12312, 182752, -3558));
		LOCATIONS.put("gludioarena", new Location(12312, 182752, -3558));
		LOCATIONS.put("gludinarena", new Location(-86979, 142402, -3643));
		LOCATIONS.put("giranarena", new Location(73890, 142656, -3778));
		LOCATIONS.put("coliseum", new Location(147451, 46728, -3410));
		LOCATIONS.put("coloseum", new Location(147451, 46728, -3410));
		LOCATIONS.put("jail", new Location(-114598, -249431, -2984));
		LOCATIONS.put("gm", new Location(-114542, -251005, -2992));
		LOCATIONS.put("gmroom", new Location(-114542, -251005, -2992));
		LOCATIONS.put("misc", new Location(-113360, -244676, -15536));
		LOCATIONS.put("jail", new Location(80151, -15445, -1805));
		LOCATIONS.put("fg", new Location(188611, 20588, -3696));
		LOCATIONS.put("forbiddengateway", new Location(188611, 20588, -3696));
		LOCATIONS.put("cemetary", new Location(167047, 20304, -3328));
		LOCATIONS.put("toi1", new Location(121685, 15749, -3852));
		LOCATIONS.put("toi2", new Location(114665, 12697, -3609));
		LOCATIONS.put("toi3", new Location(111249, 16031, -2127));
		LOCATIONS.put("toi4", new Location(114605, 19371, -645));
		LOCATIONS.put("toi5", new Location(117996, 16103, 843));
		LOCATIONS.put("toi6", new Location(114743, 19707, 1947));
		LOCATIONS.put("toi7", new Location(114552, 12354, 2957));
		LOCATIONS.put("toi8", new Location(110963, 16147, 3967));
		LOCATIONS.put("toi9", new Location(117356, 18462, 4977));
		LOCATIONS.put("toi10", new Location(118250, 15858, 5897));
		LOCATIONS.put("toi11", new Location(115824, 17242, 6760));
		LOCATIONS.put("toi12", new Location(113288, 14692, 7997));
		LOCATIONS.put("toi13", new Location(115322, 16756, 9007));
		LOCATIONS.put("valleyofsaints", new Location(65797, -71510, -3744));
		LOCATIONS.put("forestofthedead", new Location(52107, -54328, -3158));
		LOCATIONS.put("beastfarm", new Location(43805, -88010, -2780));
		LOCATIONS.put("hotsprings", new Location(144880, -113468, -2560));
		LOCATIONS.put("rainbowsprings", new Location(139997, -124860, -1896));
		LOCATIONS.put("ketra", new Location(146990, -67128, -3640));
		LOCATIONS.put("varka", new Location(125740, -40864, -3736));
		LOCATIONS.put("it", new Location(186699, -75915, -2826));
		LOCATIONS.put("imperialtomb", new Location(186699, -75915, -2826));
		LOCATIONS.put("pagan", new Location(36603, -51202, 712));
		LOCATIONS.put("pagantemple", new Location(36603, -51202, 712));
		LOCATIONS.put("denofevil", new Location(68693, -110438, -1946));
		LOCATIONS.put("mithrilmines", new Location(171946, -173352, 3440));
		LOCATIONS.put("pi", new Location(10468, -24569, -3650));
		LOCATIONS.put("primeval", new Location(10468, -24569, -3650));
		LOCATIONS.put("primevalisle", new Location(10468, -24569, -3650));
		LOCATIONS.put("primevalisland", new Location(10468, -24569, -3650));
		// Grandboss
		LOCATIONS.put("qa", new Location(-21610, 181594, -5734));
		LOCATIONS.put("queenant", new Location(-21610, 181594, -5734));
		LOCATIONS.put("antqueen", new Location(-21610, 181594, -5734));
		LOCATIONS.put("core", new Location(17723, 108915, -6493));
		LOCATIONS.put("orfen", new Location(55024, 17368, -5412));
		LOCATIONS.put("zaken", new Location(55312, 219168, -3223));
		LOCATIONS.put("baium", new Location(115213, 16623, 10080));
		LOCATIONS.put("antharas", new Location(183409, 114824, -8020));
		LOCATIONS.put("valakas", new Location(213896, -115436, -1644));
		LOCATIONS.put("teza", new Location(174238, -88011, -5115));
		LOCATIONS.put("tezza", new Location(174238, -88011, -5115));
		LOCATIONS.put("frintezza", new Location(174238, -88011, -5115));
	}
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_goto"
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		final String targetName = command.toLowerCase().replace("admin_goto ", "");
		final String noSpacesTargetName = targetName.replace(" ", "");
		if (noSpacesTargetName.isEmpty())
		{
			activeChar.sendMessage("Usage: //goto <area name, player name, npc name or npc id>");
			return true;
		}
		
		// Find if a predefined location exists.
		final Location targetLocation = LOCATIONS.get(noSpacesTargetName);
		if (targetLocation != null)
		{
			activeChar.teleToLocation(targetLocation);
			activeChar.sendSysMessage("You have been teleported.");
			return true;
		}
		
		// Find if a player with that name exists.
		final Player foundPlayer = World.getInstance().getPlayer(noSpacesTargetName);
		if ((foundPlayer != null) && (foundPlayer != activeChar))
		{
			activeChar.teleToLocation(foundPlayer);
			activeChar.sendSysMessage("You have been teleported.");
			return true;
		}
		
		// When no predefined location or player found, try to move to NPC.
		AdminCommandHandler.getInstance().onCommand(activeChar, "admin_list_positions " + targetName + " 1", false);
		return true;
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
