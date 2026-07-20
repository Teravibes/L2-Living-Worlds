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
package org.l2jmobius.loginserver;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.l2jmobius.commons.crypt.NewCrypt;
import org.l2jmobius.commons.network.base.BaseWritablePacket;
import org.l2jmobius.commons.util.TraceUtil;
import org.l2jmobius.loginserver.GameServerTable.GameServerInfo;
import org.l2jmobius.loginserver.network.GameServerPacketHandler;
import org.l2jmobius.loginserver.network.GameServerPacketHandler.GameServerState;
import org.l2jmobius.loginserver.network.ScrambledKeyPair;
import org.l2jmobius.loginserver.network.loginserverpackets.ChangePasswordResponse;
import org.l2jmobius.loginserver.network.loginserverpackets.InitLS;
import org.l2jmobius.loginserver.network.loginserverpackets.KickPlayer;
import org.l2jmobius.loginserver.network.loginserverpackets.LoginServerFail;
import org.l2jmobius.loginserver.network.loginserverpackets.RequestCharacters;

/**
 * Handles connection and communication between login server and game server.<br>
 * Manages authentication, packet exchange and player account tracking for a single game server connection.
 * <ul>
 * <li>RSA key exchange for secure communication</li>
 * <li>Blowfish encryption setup for packet encryption</li>
 * <li>Game server authentication and registration</li>
 * <li>Continuous packet processing and forwarding</li>
 * <li>Player account management and tracking</li>
 * </ul>
 * @author -Wooden-, KenM, Mobius
 */
public class GameServerThread extends Thread
{
	protected static final Logger LOGGER = Logger.getLogger(GameServerThread.class.getName());
	
	// Constants.
	private static final int PACKET_HEADER_SIZE = 2;
	private static final int BLOWFISH_BLOCK_SIZE = 8;
	private static final int HIGH_BYTE_MULTIPLIER = 256;
	private static final String DEFAULT_BLOWFISH_KEY = "_;v.]05-31!|+-%xT!^[$\00";
	
	// Player Account Management.
	private final Set<String> _accountsOnGameServer = ConcurrentHashMap.newKeySet();
	
	// Network Communication.
	private final Socket _socket;
	private InputStream _inputStream;
	private OutputStream _outputStream;
	
	// Cryptography.
	private final RSAPublicKey _publicKey;
	private final RSAPrivateKey _privateKey;
	private NewCrypt _blowfishCipher;
	
	// Connection State Management.
	private final String _connectionIp;
	private String _connectionIpAddress;
	private GameServerInfo _gameServerInfo;
	private GameServerState _loginConnectionState = GameServerState.CONNECTED;
	
	/**
	 * Constructs a new GameServerThread for handling communication with a game server.<br>
	 * Initializes the network streams, RSA key pair and Blowfish encryption. The thread automatically starts upon construction.
	 * @param socket the TCP socket connection to the game server
	 */
	public GameServerThread(Socket socket)
	{
		_socket = socket;
		_connectionIp = socket.getInetAddress().getHostAddress();
		
		try
		{
			// Initialize network streams for communication.
			_inputStream = _socket.getInputStream();
			_outputStream = new BufferedOutputStream(_socket.getOutputStream());
		}
		catch (IOException e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Failed to initialize network streams - " + e.getMessage());
		}
		
		// Retrieve RSA key pair for secure initial communication.
		final ScrambledKeyPair keyPair = LoginController.getInstance().getScrambledRSAKeyPair();
		_privateKey = (RSAPrivateKey) keyPair.getPrivateKey();
		_publicKey = (RSAPublicKey) keyPair.getPublicKey();
		
		// Initialize Blowfish cipher with default key for packet encryption.
		_blowfishCipher = new NewCrypt(DEFAULT_BLOWFISH_KEY);
		
		// Set thread name for easier debugging and monitoring.
		// Java 18: setName(getClass().getSimpleName() + "-" + getId() + "@" + _connectionIp);
		// Java 19+: Using threadId() method.
		setName(getClass().getSimpleName() + "-" + threadId() + "@" + _connectionIp);
		
		// Start the thread to begin processing.
		start();
	}
	
	/**
	 * Main thread execution method that handles the game server connection lifecycle.<br>
	 * Processes incoming packets continuously until connection is terminated.
	 */
	@Override
	public void run()
	{
		// Resolve and store the connection IP address.
		_connectionIpAddress = _socket.getInetAddress().getHostAddress();
		
		// Check if the connecting IP is banned.
		if (isBannedGameserverIP(_connectionIpAddress))
		{
			LOGGER.info("GameServerRegistration: IP Address " + _connectionIpAddress + " is on the banned IP list. Connection rejected.");
			forceClose(LoginServerFail.REASON_IP_BANNED);
			return; // Terminate processing for banned IP.
		}
		
		// Send initial packet with RSA public key for secure key exchange.
		final InitLS initializationPacket = new InitLS(_publicKey.getModulus().toByteArray());
		try
		{
			sendPacket(initializationPacket);
			
			// Main packet processing loop.
			int lengthHighByte = 0;
			int lengthLowByte = 0;
			int packetLength = 0;
			boolean checksumValid = false;
			
			while (true)
			{
				// Read packet length header (2 bytes: low byte, high byte).
				lengthLowByte = _inputStream.read();
				lengthHighByte = _inputStream.read();
				packetLength = (lengthHighByte * HIGH_BYTE_MULTIPLIER) + lengthLowByte;
				
				// Check for connection termination.
				if ((lengthHighByte < 0) || _socket.isClosed())
				{
					LOGGER.finer("GameServerThread: Game server terminated the connection gracefully.");
					break;
				}
				
				// Allocate buffer for packet data (excluding header).
				final byte[] packetData = new byte[packetLength - PACKET_HEADER_SIZE];
				int totalBytesReceived = 0;
				int bytesRead = 0;
				int remainingBytes = packetLength - PACKET_HEADER_SIZE;
				
				// Read the complete packet data.
				while ((bytesRead != -1) && (totalBytesReceived < (packetLength - PACKET_HEADER_SIZE)))
				{
					bytesRead = _inputStream.read(packetData, totalBytesReceived, remainingBytes);
					totalBytesReceived += bytesRead;
					remainingBytes -= bytesRead;
				}
				
				// Validate that we received the complete packet.
				if (totalBytesReceived != (packetLength - PACKET_HEADER_SIZE))
				{
					LOGGER.warning("Incomplete packet received from game server. Expected " + (packetLength - PACKET_HEADER_SIZE) + " bytes, got " + totalBytesReceived + ". Closing connection.");
					break;
				}
				
				// Decrypt the packet data using Blowfish cipher.
				_blowfishCipher.decrypt(packetData, 0, packetData.length);
				
				// Verify packet integrity using checksum.
				checksumValid = NewCrypt.verifyChecksum(packetData);
				if (!checksumValid)
				{
					LOGGER.warning("Packet checksum verification failed. Possible data corruption or tampering detected. Closing connection.");
					return;
				}
				
				// Forward the validated packet to the packet handler.
				GameServerPacketHandler.handlePacket(packetData, this);
			}
		}
		catch (IOException e)
		{
			// Log connection loss with server identification.
			final String serverIdentification = getServerId() != -1 ? "[" + getServerId() + "] " + GameServerTable.getInstance().getServerNameById(getServerId()) : "(" + _connectionIpAddress + ")";
			final String connectionLostMessage = "GameServer " + serverIdentification + ": Connection lost - " + e.getMessage();
			LOGGER.info(connectionLostMessage);
		}
		finally
		{
			// Cleanup operations when thread terminates.
			if (isAuthed())
			{
				// Mark the game server as offline.
				if (_gameServerInfo != null)
				{
					_gameServerInfo.setDown();
				}
				
				LOGGER.info("Server [" + getServerId() + "] " + GameServerTable.getInstance().getServerNameById(getServerId()) + " is now marked as disconnected.");
			}
			
			// Remove this game server from active connections and cleanup flood protection.
			LoginServer.getInstance().getGameServerListener().removeGameServer(this);
			LoginServer.getInstance().getGameServerListener().removeFloodProtection(_connectionIp);
		}
	}
	
	/**
	 * Checks if a specific account is currently logged into this game server.
	 * @param accountName the account name to check
	 * @return true if the account is logged into this game server, false otherwise
	 */
	public boolean hasAccountOnGameServer(String accountName)
	{
		return _accountsOnGameServer.contains(accountName);
	}
	
	/**
	 * Returns the current number of players logged into this game server.
	 * @return the count of active player accounts on this game server
	 */
	public int getPlayerCount()
	{
		return _accountsOnGameServer.size();
	}
	
	/**
	 * Attaches GameServerInfo to this thread and configures the game server settings.<br>
	 * This method completes the game server registration process by:
	 * <ul>
	 * <li>Linking the GameServerInfo object to this thread</li>
	 * <li>Setting the server's network configuration (port and hosts)</li>
	 * <li>Configuring player capacity limits</li>
	 * <li>Marking the server as authenticated and ready to accept players</li>
	 * </ul>
	 * @param gameServerInfo the GameServerInfo object to attach
	 * @param port the port number the game server is listening on
	 * @param hosts array of host addresses (pairs of host and subnet)
	 * @param maxPlayers maximum number of players the server can accommodate
	 */
	public void attachGameServerInfo(GameServerInfo gameServerInfo, int port, String[] hosts, int maxPlayers)
	{
		// Establish bidirectional relationship between thread and server info.
		setGameServerInfo(gameServerInfo);
		gameServerInfo.setGameServerThread(this);
		
		// Configure server network settings.
		gameServerInfo.setPort(port);
		setGameHosts(hosts);
		
		// Set player capacity and mark as authenticated.
		gameServerInfo.setMaxPlayers(maxPlayers);
		gameServerInfo.setAuthed(true);
	}
	
	/**
	 * Forces the connection to close immediately and sends a failure reason to the game server.<br>
	 * This method is used when the game server needs to be disconnected due to violations such as IP banning, authentication failures, or other security issues.
	 * @param reasonCode the reason code for the disconnection (from LoginServerFail constants)
	 */
	public void forceClose(int reasonCode)
	{
		// Send failure notification to the game server.
		sendPacket(new LoginServerFail(reasonCode));
		
		try
		{
			// Close the socket connection immediately.
			_socket.close();
		}
		catch (IOException e)
		{
			LOGGER.finer("GameServerThread: Failed to close socket for banned server. Socket may already be closed - " + e.getMessage());
		}
	}
	
	/**
	 * Checks if the specified IP address is banned from connecting as a game server.<br>
	 * This method can be extended to implement IP-based access control.
	 * @param ipAddress the IP address to check
	 * @return true if the IP address is banned, false otherwise
	 */
	public static boolean isBannedGameserverIP(String ipAddress)
	{
		// TODO: Implement IP banning logic here.
		// This could check against a database table, configuration file, or in-memory cache.
		return false;
	}
	
	/**
	 * Sends a packet to the connected game server with proper encryption and checksums.<br>
	 * The packet is automatically encrypted using Blowfish cipher and padded to the required block size. A checksum is appended for integrity verification.
	 * @param packet the packet to send to the game server
	 */
	public void sendPacket(BaseWritablePacket packet)
	{
		// Verify connection is still valid.
		if ((_blowfishCipher == null) || (_socket == null) || _socket.isClosed())
		{
			return;
		}
		
		try
		{
			// Prepare packet data.
			packet.write(); // Write the packet content.
			packet.writeInt(0); // Reserve space for checksum.
			
			int dataSize = packet.getLength() - PACKET_HEADER_SIZE; // Size without header.
			final int paddingNeeded = dataSize % BLOWFISH_BLOCK_SIZE; // Calculate padding for 8-byte blocks.
			
			// Add padding bytes to align with Blowfish block size.
			if (paddingNeeded != 0)
			{
				for (int i = paddingNeeded; i < BLOWFISH_BLOCK_SIZE; i++)
				{
					packet.writeByte(0);
				}
			}
			
			// Get the complete packet data including header.
			final byte[] packetData = packet.getSendableBytes();
			dataSize = packetData.length - PACKET_HEADER_SIZE; // Recalculate size after padding.
			
			// Thread-safe packet transmission.
			synchronized (_outputStream)
			{
				// Append checksum for integrity verification.
				NewCrypt.appendChecksum(packetData, PACKET_HEADER_SIZE, dataSize);
				
				// Encrypt the data portion (excluding header).
				_blowfishCipher.crypt(packetData, PACKET_HEADER_SIZE, dataSize);
				
				// Send the complete packet.
				_outputStream.write(packetData);
				try
				{
					_outputStream.flush();
				}
				catch (IOException e)
				{
					// Game server might have terminated connection during flush.
					LOGGER.finer("GameServerThread: Failed to flush output stream. Game server may have disconnected - " + e.getMessage());
				}
			}
		}
		catch (IOException e)
		{
			LOGGER.severe("GameServerThread: IOException occurred while sending packet " + packet.getClass().getSimpleName() + " - " + e.getMessage());
			LOGGER.severe(TraceUtil.getStackTrace(e));
		}
	}
	
	/**
	 * Sends a kick command to the game server to disconnect a specific player.<br>
	 * This is typically used when a player violates login server policies or when administrative action is required.
	 * @param accountName the account name of the player to kick
	 */
	public void kickPlayer(String accountName)
	{
		sendPacket(new KickPlayer(accountName));
	}
	
	/**
	 * Requests character information for a specific account from the game server.<br>
	 * This is used during the login process to retrieve character list data.
	 * @param accountName the account name to request character data for
	 */
	public void requestCharacters(String accountName)
	{
		sendPacket(new RequestCharacters(accountName));
	}
	
	/**
	 * Sends a password change response to the game server for a specific character.<br>
	 * This notifies the game server about the result of a password change operation.
	 * @param characterName the name of the character whose password was changed
	 * @param responseMessage the message to send back to the client
	 */
	public void changePasswordResponse(String characterName, String responseMessage)
	{
		sendPacket(new ChangePasswordResponse(characterName, responseMessage));
	}
	
	/**
	 * Configures the game server's host addresses and subnets.<br>
	 * Updates the server's network configuration and logs the changes for monitoring. Host addresses are provided in pairs (host, subnet mask).
	 * @param hosts array of host addresses in pairs (host1, subnet1, host2, subnet2, ...)
	 */
	public void setGameHosts(String[] hosts)
	{
		LOGGER.info("Updated Gameserver [" + getServerId() + "] " + GameServerTable.getInstance().getServerNameById(getServerId()) + " IP's:");
		
		// Clear existing server addresses.
		_gameServerInfo.clearServerAddresses();
		
		// Process host pairs (host, subnet).
		for (int i = 0; i < hosts.length; i += 2)
		{
			try
			{
				_gameServerInfo.addServerAddress(hosts[i], hosts[i + 1]);
			}
			catch (Exception e)
			{
				LOGGER.warning("Failed to resolve hostname \"" + hosts[i] + "\" - " + e.getMessage());
			}
		}
		
		// Log all configured server addresses.
		for (String serverAddress : _gameServerInfo.getServerAddresses())
		{
			LOGGER.info(serverAddress);
		}
	}
	
	/**
	 * Checks if this game server thread is authenticated and ready to handle players.
	 * @return true if the game server is authenticated, false otherwise
	 */
	public boolean isAuthed()
	{
		if (_gameServerInfo == null)
		{
			return false;
		}
		
		return _gameServerInfo.isAuthed();
	}
	
	/**
	 * Sets the GameServerInfo object for this thread.<br>
	 * This establishes the link between the thread and server configuration.
	 * @param gameServerInfo the GameServerInfo object to associate with this thread
	 */
	public void setGameServerInfo(GameServerInfo gameServerInfo)
	{
		_gameServerInfo = gameServerInfo;
	}
	
	/**
	 * Returns the GameServerInfo object associated with this thread.
	 * @return the GameServerInfo object, or null if not set
	 */
	public GameServerInfo getGameServerInfo()
	{
		return _gameServerInfo;
	}
	
	/**
	 * Returns the IP address of the connected game server.
	 * @return the connection IP address as a string
	 */
	public String getConnectionIpAddress()
	{
		return _connectionIpAddress;
	}
	
	/**
	 * Returns the unique server ID of this game server.
	 * @return the server ID, or -1 if not assigned
	 */
	public int getServerId()
	{
		if (_gameServerInfo != null)
		{
			return _gameServerInfo.getId();
		}
		
		return -1;
	}
	
	/**
	 * Returns the RSA private key used for decrypting session keys from the game server.
	 * @return the RSA private key
	 */
	public RSAPrivateKey getPrivateKey()
	{
		return _privateKey;
	}
	
	/**
	 * Updates the Blowfish cipher used for packet encryption.<br>
	 * This is typically called after the initial key exchange to use the session key.
	 * @param blowfishCipher the new Blowfish cipher to use
	 */
	public void setBlowFish(NewCrypt blowfishCipher)
	{
		_blowfishCipher = blowfishCipher;
	}
	
	/**
	 * Adds an account to the set of players logged into this game server.<br>
	 * Also removes the account from the login server's authenticated client list to prevent duplicate logins.
	 * @param accountName the account name to add
	 */
	public void addAccountOnGameServer(String accountName)
	{
		_accountsOnGameServer.add(accountName);
		LoginController.getInstance().removeAuthedLoginClient(accountName);
	}
	
	/**
	 * Removes an account from the set of players logged into this game server.<br>
	 * Also cleans up the account from the login server's authenticated client list.
	 * @param accountName the account name to remove
	 */
	public void removeAccountOnGameServer(String accountName)
	{
		_accountsOnGameServer.remove(accountName);
		LoginController.getInstance().removeAuthedLoginClient(accountName);
	}
	
	/**
	 * Returns the current connection state of this game server.
	 * @return the current GameServerState
	 */
	public GameServerState getLoginConnectionState()
	{
		return _loginConnectionState;
	}
	
	/**
	 * Sets the connection state of this game server.<br>
	 * This is used to track the authentication and registration progress.
	 * @param state the new connection state
	 */
	public void setLoginConnectionState(GameServerState state)
	{
		_loginConnectionState = state;
	}
}
