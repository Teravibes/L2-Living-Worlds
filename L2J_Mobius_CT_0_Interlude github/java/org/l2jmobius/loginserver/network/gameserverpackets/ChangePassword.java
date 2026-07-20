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
package org.l2jmobius.loginserver.network.gameserverpackets;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.network.base.BaseReadablePacket;
import org.l2jmobius.loginserver.GameServerTable;
import org.l2jmobius.loginserver.GameServerTable.GameServerInfo;
import org.l2jmobius.loginserver.GameServerThread;

/**
 * Handles password change requests coming from game servers.<br>
 * Performs credential verification and persists the updated password hash in the login database.
 * <ul>
 * <li>Resolves the session owner GameServerThread for the requested account.</li>
 * <li>Validates the provided current password hash against the stored value.</li>
 * <li>Updates the stored hash and replies with the result.</li>
 * </ul>
 * @author BazookaRpm
 */
public class ChangePassword extends BaseReadablePacket
{
	// Logging.
	protected static final Logger LOGGER = Logger.getLogger(ChangePassword.class.getName());
	
	// Password Hashing.
	private static final String PASSWORD_DIGEST_ALGORITHM = "SHA";
	
	// Database Queries.
	private static final String SELECT_PASSWORD_SQL = "SELECT password FROM accounts WHERE login=?";
	private static final String UPDATE_PASSWORD_MATCHING_SQL = "UPDATE accounts SET password=? WHERE login=? AND password=?";
	
	// Response Messages.
	private static final String MSG_INVALID_PASSWORD_DATA = "Invalid password data! Try again.";
	private static final String MSG_PASSWORD_CHANGED = "You have successfully changed your password!";
	private static final String MSG_PASSWORD_CHANGE_FAILED = "The password change was unsuccessful!";
	private static final String MSG_CURRENT_PASSWORD_MISMATCH = "The typed current password doesn't match with your current one.";
	
	/**
	 * Reads the password change request and updates the account password when validation succeeds.<br>
	 * Sends the result back to the owning game server thread.
	 * @param decrypt
	 */
	public ChangePassword(byte[] decrypt)
	{
		super(decrypt);
		
		final int packetId = readByte();
		final String loginName = readString();
		final String playerName = readString();
		final String currentPassword = readString();
		final String newPassword = readString();
		
		GameServerThread ownerThread = null;
		int ownerServerId = 0;
		
		final Collection<GameServerInfo> registeredServers = GameServerTable.getInstance().getRegisteredGameServers().values();
		for (GameServerInfo serverInfo : registeredServers)
		{
			final GameServerThread serverThread = serverInfo.getGameServerThread();
			if ((serverThread != null) && serverThread.hasAccountOnGameServer(loginName))
			{
				ownerThread = serverThread;
				ownerServerId = serverInfo.getId();
				break;
			}
		}
		
		if (ownerThread == null)
		{
			if (LOGGER.isLoggable(Level.FINER))
			{
				LOGGER.finer("Ignored ChangePassword packetId=" + packetId + " for account '" + loginName + "' requested by player '" + playerName + "'.");
			}
			return;
		}
		
		if ((currentPassword == null) || (newPassword == null))
		{
			ownerThread.changePasswordResponse(playerName, MSG_INVALID_PASSWORD_DATA);
			return;
		}
		
		try
		{
			final MessageDigest digest = MessageDigest.getInstance(PASSWORD_DIGEST_ALGORITHM);
			final Encoder encoder = Base64.getEncoder();
			
			final String currentHash = encoder.encodeToString(digest.digest(currentPassword.getBytes(StandardCharsets.UTF_8)));
			final String newHash = encoder.encodeToString(digest.digest(newPassword.getBytes(StandardCharsets.UTF_8)));
			
			try (Connection con = DatabaseFactory.getConnection())
			{
				int updatedRows = 0;
				try (PreparedStatement updateStatement = con.prepareStatement(UPDATE_PASSWORD_MATCHING_SQL))
				{
					updateStatement.setString(1, newHash);
					updateStatement.setString(2, loginName);
					updateStatement.setString(3, currentHash);
					updatedRows = updateStatement.executeUpdate();
				}
				
				if (updatedRows > 0)
				{
					// LOGGER.info("Password changed for account '" + loginName + "' requested by player '" + playerName + "' on GS " + ownerServerId + ".");
					ownerThread.changePasswordResponse(playerName, MSG_PASSWORD_CHANGED);
					return;
				}
				
				String storedHash = null;
				try (PreparedStatement selectStatement = con.prepareStatement(SELECT_PASSWORD_SQL))
				{
					selectStatement.setString(1, loginName);
					try (ResultSet resultSet = selectStatement.executeQuery())
					{
						if (resultSet.next())
						{
							storedHash = resultSet.getString("password");
						}
					}
				}
				
				if (currentHash.equals(storedHash))
				{
					LOGGER.warning("Password change failed for account '" + loginName + "' requested by player '" + playerName + "' on GS " + ownerServerId + " (0 rows updated).");
					ownerThread.changePasswordResponse(playerName, MSG_PASSWORD_CHANGE_FAILED);
				}
				else
				{
					ownerThread.changePasswordResponse(playerName, MSG_CURRENT_PASSWORD_MISMATCH);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Error while changing password for account '" + loginName + "' requested by player '" + playerName + "' on GS " + ownerServerId + " (packetId=" + packetId + ").", e);
		}
	}
}
