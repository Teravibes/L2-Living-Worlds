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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.Inet6Address;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.ConfigReader;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.commons.util.StringUtil;

/**
 * This class loads all the server related configurations.
 * @author Mobius
 */
public class ServerConfig
{
	private static final Logger LOGGER = Logger.getLogger(ServerConfig.class.getName());
	
	// Files
	private static final String SERVER_CONFIG_FILE = "./config/Server.ini";
	private static final String IPCONFIG_FILE = "./config/ipconfig.xml";
	private static final String CHAT_FILTER_FILE = "./config/chatfilter.txt";
	private static final String HEXID_FILE = "./config/hexid.txt";
	
	// Constants
	public static String GAMESERVER_HOSTNAME;
	public static int PORT_GAME;
	public static int GAME_SERVER_LOGIN_PORT;
	public static String GAME_SERVER_LOGIN_HOST;
	public static boolean PACKET_ENCRYPTION;
	public static int REQUEST_ID;
	public static boolean ACCEPT_ALTERNATE_ID;
	public static File DATAPACK_ROOT;
	public static File SCRIPT_ROOT;
	public static Pattern CHARNAME_TEMPLATE_PATTERN;
	public static String PET_NAME_TEMPLATE;
	public static String CLAN_NAME_TEMPLATE;
	public static int MAX_CHARACTERS_NUMBER_PER_ACCOUNT;
	public static int MAXIMUM_ONLINE_USERS;
	public static boolean HARDWARE_INFO_ENABLED;
	public static boolean KICK_MISSING_HWID;
	public static int MAX_PLAYERS_PER_HWID;
	public static List<Integer> PROTOCOL_LIST;
	public static int SERVER_LIST_TYPE;
	public static int SERVER_LIST_AGE;
	public static boolean SERVER_LIST_BRACKET;
	public static boolean DEADLOCK_WATCHER;
	public static int DEADLOCK_CHECK_INTERVAL;
	public static boolean RESTART_ON_DEADLOCK;
	public static boolean SERVER_RESTART_SCHEDULE_ENABLED;
	public static boolean SERVER_RESTART_SCHEDULE_MESSAGE;
	public static int SERVER_RESTART_SCHEDULE_COUNTDOWN;
	public static String[] SERVER_RESTART_SCHEDULE;
	public static List<Integer> SERVER_RESTART_DAYS;
	public static boolean PRECAUTIONARY_RESTART_ENABLED;
	public static boolean PRECAUTIONARY_RESTART_CPU;
	public static boolean PRECAUTIONARY_RESTART_MEMORY;
	public static boolean PRECAUTIONARY_RESTART_CHECKS;
	public static int PRECAUTIONARY_RESTART_PERCENTAGE;
	public static int PRECAUTIONARY_RESTART_DELAY;
	public static List<String> GAME_SERVER_SUBNETS;
	public static List<String> GAME_SERVER_HOSTS;
	
	// Other
	public static boolean RESERVE_HOST_ON_LOGIN = false;
	public static List<String> FILTER_LIST;
	public static int SERVER_ID;
	public static byte[] HEX_ID;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(SERVER_CONFIG_FILE);
		GAMESERVER_HOSTNAME = config.getString("GameserverHostname", "0.0.0.0");
		PORT_GAME = config.getInt("GameserverPort", 7777);
		GAME_SERVER_LOGIN_PORT = config.getInt("LoginPort", 9014);
		GAME_SERVER_LOGIN_HOST = config.getString("LoginHost", "127.0.0.1");
		PACKET_ENCRYPTION = config.getBoolean("PacketEncryption", false);
		REQUEST_ID = config.getInt("RequestServerID", 0);
		ACCEPT_ALTERNATE_ID = config.getBoolean("AcceptAlternateID", true);
		
		try
		{
			DATAPACK_ROOT = new File(config.getString("DatapackRoot", ".").replaceAll("\\\\", "/")).getCanonicalFile();
		}
		catch (IOException e)
		{
			LOGGER.log(Level.WARNING, "Error setting datapack root!", e);
			DATAPACK_ROOT = new File(".");
		}
		
		try
		{
			SCRIPT_ROOT = new File(config.getString("ScriptRoot", "./data/scripts").replaceAll("\\\\", "/")).getCanonicalFile();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Error setting script root!", e);
			SCRIPT_ROOT = new File(".");
		}
		
		Pattern charNamePattern;
		try
		{
			charNamePattern = Pattern.compile(config.getString("CnameTemplate", ".*"));
		}
		catch (PatternSyntaxException e)
		{
			LOGGER.log(Level.WARNING, "Character name pattern is invalid!", e);
			charNamePattern = Pattern.compile(".*");
		}
		CHARNAME_TEMPLATE_PATTERN = charNamePattern;
		PET_NAME_TEMPLATE = config.getString("PetNameTemplate", ".*");
		CLAN_NAME_TEMPLATE = config.getString("ClanNameTemplate", ".*");
		MAX_CHARACTERS_NUMBER_PER_ACCOUNT = config.getInt("CharMaxNumber", 7);
		MAXIMUM_ONLINE_USERS = config.getInt("MaximumOnlineUsers", 100);
		HARDWARE_INFO_ENABLED = config.getBoolean("EnableHardwareInfo", false);
		KICK_MISSING_HWID = config.getBoolean("KickMissingHWID", false);
		MAX_PLAYERS_PER_HWID = config.getInt("MaxPlayersPerHWID", 0);
		if (MAX_PLAYERS_PER_HWID > 0)
		{
			KICK_MISSING_HWID = true;
		}
		
		final String[] protocols = config.getString("AllowedProtocolRevisions", "746").split(";");
		PROTOCOL_LIST = new ArrayList<>(protocols.length);
		for (String protocol : protocols)
		{
			try
			{
				PROTOCOL_LIST.add(Integer.parseInt(protocol.trim()));
			}
			catch (NumberFormatException e)
			{
				LOGGER.warning("Wrong config protocol version: " + protocol + ". Skipped.");
			}
		}
		
		SERVER_LIST_TYPE = getServerTypeId(config.getString("ServerListType", "Free").split(","));
		SERVER_LIST_AGE = config.getInt("ServerListAge", 0);
		SERVER_LIST_BRACKET = config.getBoolean("ServerListBrackets", false);
		DEADLOCK_WATCHER = config.getBoolean("DeadlockWatcher", true);
		DEADLOCK_CHECK_INTERVAL = config.getInt("DeadlockCheckInterval", 20);
		RESTART_ON_DEADLOCK = config.getBoolean("RestartOnDeadlock", false);
		SERVER_RESTART_SCHEDULE_ENABLED = config.getBoolean("ServerRestartScheduleEnabled", false);
		SERVER_RESTART_SCHEDULE_MESSAGE = config.getBoolean("ServerRestartScheduleMessage", false);
		SERVER_RESTART_SCHEDULE_COUNTDOWN = config.getInt("ServerRestartScheduleCountdown", 600);
		SERVER_RESTART_SCHEDULE = config.getString("ServerRestartSchedule", "08:00").split(",");
		
		SERVER_RESTART_DAYS = new ArrayList<>();
		for (String day : config.getString("ServerRestartDays", "").trim().split(","))
		{
			if (StringUtil.isNumeric(day))
			{
				SERVER_RESTART_DAYS.add(Integer.parseInt(day));
			}
		}
		
		PRECAUTIONARY_RESTART_ENABLED = config.getBoolean("PrecautionaryRestartEnabled", false);
		PRECAUTIONARY_RESTART_CPU = config.getBoolean("PrecautionaryRestartCpu", true);
		PRECAUTIONARY_RESTART_MEMORY = config.getBoolean("PrecautionaryRestartMemory", false);
		PRECAUTIONARY_RESTART_CHECKS = config.getBoolean("PrecautionaryRestartChecks", true);
		PRECAUTIONARY_RESTART_PERCENTAGE = config.getInt("PrecautionaryRestartPercentage", 95);
		PRECAUTIONARY_RESTART_DELAY = config.getInt("PrecautionaryRestartDelay", 60) * 1000;
		
		final IPConfigData ipConfigData = new IPConfigData();
		GAME_SERVER_SUBNETS = ipConfigData.getSubnets();
		GAME_SERVER_HOSTS = ipConfigData.getHosts();
		
		// Load chatfilter.txt file.
		loadChatFilter();
		
		// Load hexid.txt file.
		loadHexid();
	}
	
	/**
	 * Loads the chat filter words from the specified file.<br>
	 * This method reads lines from the {@code CHAT_FILTER_FILE}, trims whitespace and ignores empty lines or lines starting with a '#' character.<br>
	 * The filtered words are collected into the {@code FILTER_LIST}. If an error occurs during file reading, a warning message is logged.
	 */
	private static void loadChatFilter()
	{
		try
		{
			FILTER_LIST = Files.lines(Paths.get(CHAT_FILTER_FILE), StandardCharsets.UTF_8).map(String::trim).filter(line -> (!line.isEmpty() && (line.charAt(0) != '#'))).collect(Collectors.toList());
			LOGGER.info("Loaded " + FILTER_LIST.size() + " Filter Words.");
		}
		catch (IOException e)
		{
			LOGGER.log(Level.WARNING, "Error while loading chat filter words!", e);
		}
	}
	
	/**
	 * Loads the HexID configuration from a properties file.<br>
	 * This method reads the {@code HEXID_FILE} and attempts to load the server ID and hexadecimal ID if available.<br>
	 * If the file exists, it parses the properties to retrieve the {@code ServerID} and {@code HexID} values.<br>
	 * The {@code ServerID} is stored as an integer, while the {@code HexID} is converted from a hexadecimal string to a byte array.<br>
	 * If the file does not contain valid data or cannot be loaded, a warning is logged and the system attempts to retrieve the HexID from another source.
	 */
	private static void loadHexid()
	{
		final File hexIdFile = new File(HEXID_FILE);
		if (hexIdFile.exists())
		{
			final ConfigReader hexId = new ConfigReader(HEXID_FILE);
			if (hexId.containsKey("ServerID") && hexId.containsKey("HexID"))
			{
				SERVER_ID = hexId.getInt("ServerID", 1);
				try
				{
					HEX_ID = new BigInteger(hexId.getString("HexID", null), 16).toByteArray();
				}
				catch (Exception e)
				{
					LOGGER.warning("Could not load HexID file (" + HEXID_FILE + "). Hopefully login will give us one.");
				}
			}
		}
		
		if (HEX_ID == null)
		{
			LOGGER.warning("Could not load HexID file (" + HEXID_FILE + "). Hopefully login will give us one.");
		}
	}
	
	/**
	 * Save hexadecimal ID of the server in the config file.<br>
	 * Check {@link #HEXID_FILE}.
	 * @param serverId the ID of the server whose hexId to save
	 * @param hexId the hexadecimal ID to store
	 */
	public static void saveHexid(int serverId, String hexId)
	{
		saveHexid(serverId, hexId, HEXID_FILE);
	}
	
	/**
	 * Save hexadecimal ID of the server in the config file.
	 * @param serverId the ID of the server whose hexId to save
	 * @param hexId the hexadecimal ID to store
	 * @param fileName name of the config file
	 */
	private static void saveHexid(int serverId, String hexId, String fileName)
	{
		try
		{
			final Properties hexSetting = new Properties();
			final File file = new File(fileName);
			
			// Create a new empty file only if it doesn't exist.
			if (!file.exists())
			{
				try (OutputStream out = new FileOutputStream(file))
				{
					hexSetting.setProperty("ServerID", String.valueOf(serverId));
					hexSetting.setProperty("HexID", hexId);
					hexSetting.store(out, "The HexId to Auth into LoginServer");
					LOGGER.log(Level.INFO, "Gameserver: Generated new HexID file for server id " + serverId + ".");
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warning(StringUtil.concat("Failed to save hex id to ", fileName, " File."));
			LOGGER.warning("Config: " + e.getMessage());
		}
	}
	
	/**
	 * Calculates a bitwise ID representing the types of servers specified. Each server type is associated with a unique bit position, allowing multiple types to be combined in a single integer using bitwise OR.
	 * @param serverTypes An array of server type names as strings. Any unrecognized types are ignored.
	 * @return An integer representing the combined server types, where each bit corresponds to a specific server type. The result is 0 if no recognized types are provided.
	 */
	public static int getServerTypeId(String[] serverTypes)
	{
		int serverType = 0;
		for (String cType : serverTypes)
		{
			switch (cType.trim().toLowerCase())
			{
				case "normal":
				{
					serverType |= 0x01;
					break;
				}
				case "relax":
				{
					serverType |= 0x02;
					break;
				}
				case "test":
				{
					serverType |= 0x04;
					break;
				}
				case "nolabel":
				{
					serverType |= 0x08;
					break;
				}
				case "restricted":
				{
					serverType |= 0x10;
					break;
				}
				case "event":
				{
					serverType |= 0x20;
					break;
				}
				case "free":
				{
					serverType |= 0x40;
					break;
				}
				default:
				{
					break;
				}
			}
		}
		
		return serverType;
	}
	
	/**
	 * A configuration class for managing server IP and subnet settings. This class loads network configuration settings from an XML file or performs automatic configuration if the file is unavailable.<br>
	 * <p>
	 * If the configuration file exists, it parses the file to define subnets and hosts manually. If the file is missing, it attempts automatic configuration by retrieving the external IP address and identifying local network interfaces to configure internal IP addresses and subnets.
	 * </p>
	 */
	private static class IPConfigData implements IXmlReader
	{
		private static final List<String> _subnets = new ArrayList<>(5);
		private static final List<String> _hosts = new ArrayList<>(5);
		
		public IPConfigData()
		{
			load();
		}
		
		@Override
		public void load()
		{
			final File file = new File(IPCONFIG_FILE);
			if (file.exists())
			{
				LOGGER.info("Network Config: ipconfig.xml exists, using manual configuration...");
				parseFile(new File(IPCONFIG_FILE));
			}
			else // Auto configuration...
			{
				LOGGER.info("Network Config: ipconfig.xml does not exist, using automatic configuration...");
				autoIpConfig();
			}
		}
		
		@Override
		public void parseDocument(Document document, File file)
		{
			NamedNodeMap attrs;
			for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if ("gameserver".equalsIgnoreCase(n.getNodeName()))
				{
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if ("define".equalsIgnoreCase(d.getNodeName()))
						{
							attrs = d.getAttributes();
							_subnets.add(attrs.getNamedItem("subnet").getNodeValue());
							_hosts.add(attrs.getNamedItem("address").getNodeValue());
							
							if (_hosts.size() != _subnets.size())
							{
								LOGGER.warning("Failed to Load " + IPCONFIG_FILE + " File - subnets does not match server addresses.");
							}
						}
					}
					
					final Node att = n.getAttributes().getNamedItem("address");
					if (att == null)
					{
						LOGGER.warning("Failed to load " + IPCONFIG_FILE + " file - default server address is missing.");
						_hosts.add("127.0.0.1");
					}
					else
					{
						_hosts.add(att.getNodeValue());
					}
					
					_subnets.add("0.0.0.0/0");
				}
			}
		}
		
		protected void autoIpConfig()
		{
			String externalIp = "127.0.0.1";
			try
			{
				// Java 19
				// final URL autoIp = new URL("http://checkip.amazonaws.com");
				// Java 20
				final URL autoIp = URI.create("http://checkip.amazonaws.com").toURL();
				try (BufferedReader in = new BufferedReader(new InputStreamReader(autoIp.openStream())))
				{
					externalIp = in.readLine();
				}
			}
			catch (IOException e)
			{
				LOGGER.log(Level.INFO, "Failed to connect to checkip.amazonaws.com please check your internet connection using 127.0.0.1!");
				externalIp = "127.0.0.1";
			}
			
			try
			{
				final Enumeration<NetworkInterface> niList = NetworkInterface.getNetworkInterfaces();
				while (niList.hasMoreElements())
				{
					final NetworkInterface ni = niList.nextElement();
					if (!ni.isUp() || ni.isVirtual())
					{
						continue;
					}
					
					if (!ni.isLoopback() && ((ni.getHardwareAddress() == null) || (ni.getHardwareAddress().length != 6)))
					{
						continue;
					}
					
					for (InterfaceAddress ia : ni.getInterfaceAddresses())
					{
						if (ia.getAddress() instanceof Inet6Address)
						{
							continue;
						}
						
						final String hostAddress = ia.getAddress().getHostAddress();
						final int subnetPrefixLength = ia.getNetworkPrefixLength();
						final int subnetMaskInt = IntStream.rangeClosed(1, subnetPrefixLength).reduce((r, _) -> (r << 1) + 1).orElse(0) << (32 - subnetPrefixLength);
						final int hostAddressInt = Arrays.stream(hostAddress.split("\\.")).mapToInt(Integer::parseInt).reduce((r, e) -> (r << 8) + e).orElse(0);
						final int subnetAddressInt = hostAddressInt & subnetMaskInt;
						final String subnetAddress = ((subnetAddressInt >> 24) & 0xFF) + "." + ((subnetAddressInt >> 16) & 0xFF) + "." + ((subnetAddressInt >> 8) & 0xFF) + "." + (subnetAddressInt & 0xFF);
						final String subnet = subnetAddress + '/' + subnetPrefixLength;
						if (!_subnets.contains(subnet) && !subnet.equals("0.0.0.0/0"))
						{
							_subnets.add(subnet);
							_hosts.add(hostAddress);
							LOGGER.info("Network Config: Adding new subnet: " + subnet + " address: " + hostAddress);
						}
					}
				}
				
				// External host and subnet.
				_hosts.add(externalIp);
				_subnets.add("0.0.0.0/0");
				LOGGER.info("Network Config: Adding new subnet: 0.0.0.0/0 address: " + externalIp);
			}
			catch (SocketException e)
			{
				LOGGER.log(Level.INFO, "Network Config: Configuration failed please configure manually using ipconfig.xml", e);
				System.exit(0);
			}
		}
		
		protected List<String> getSubnets()
		{
			if (_subnets.isEmpty())
			{
				return Arrays.asList("0.0.0.0/0");
			}
			
			return _subnets;
		}
		
		protected List<String> getHosts()
		{
			if (_hosts.isEmpty())
			{
				return Arrays.asList("127.0.0.1");
			}
			
			return _hosts;
		}
	}
}
