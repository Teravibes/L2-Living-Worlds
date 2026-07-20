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
package org.l2jmobius.loginserver.network;

import java.io.IOException;
import java.security.interfaces.RSAPrivateKey;
import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.commons.network.Buffer;
import org.l2jmobius.commons.network.Client;
import org.l2jmobius.commons.network.Connection;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.loginserver.LoginController;
import org.l2jmobius.loginserver.SessionKey;
import org.l2jmobius.loginserver.enums.AccountKickedReason;
import org.l2jmobius.loginserver.enums.LoginFailReason;
import org.l2jmobius.loginserver.enums.PlayFailReason;
import org.l2jmobius.loginserver.network.serverpackets.AccountKicked;
import org.l2jmobius.loginserver.network.serverpackets.Init;
import org.l2jmobius.loginserver.network.serverpackets.LoginFail;
import org.l2jmobius.loginserver.network.serverpackets.LoginServerPacket;
import org.l2jmobius.loginserver.network.serverpackets.PlayFail;

/**
 * Represents a client connected to the LoginServer.<br>
 * Holds session state, cryptographic context, account metadata and server lists.
 * <ul>
 * <li>RSA/Blowfish handshake and per-client encryption.</li>
 * <li>Session id generation and access level tracking.</li>
 * <li>Lifecycle hooks for connection/disconnection.</li>
 * </ul>
 * @author KenM, BazookaRpm
 */
public class LoginClient extends Client<Connection<LoginClient>>
{
	private static final String DEFAULT_IP = "N/A";
	private static final int DISCONNECT_GRACE_MS = 1000;
	
	private final LoginEncryption _encryption;
	private final ScrambledKeyPair _scrambledPair;
	private final byte[] _blowfishKey;
	
	private String _ip = DEFAULT_IP;
	private final int _sessionId;
	private final long _connectionStartTime;
	
	private String _account;
	private int _accessLevel;
	private int _lastServer;
	private SessionKey _sessionKey;
	private boolean _joinedGS;
	private ConnectionState _connectionState = ConnectionState.CONNECTED;
	
	private Map<Integer, Integer> _charsOnServers;
	private Map<Integer, long[]> _charsToDelete;
	
	public LoginClient(Connection<LoginClient> connection)
	{
		super(connection);
		
		_scrambledPair = LoginController.getInstance().getScrambledRSAKeyPair();
		_blowfishKey = LoginController.getInstance().getBlowfishKey();
		_ip = connection.getRemoteAddress();
		_sessionId = Rnd.nextInt();
		_connectionStartTime = System.currentTimeMillis();
		
		_encryption = new LoginEncryption();
		_encryption.setKey(_blowfishKey);
		
		if (LoginController.getInstance().isBannedAddress(_ip))
		{
			close(LoginFailReason.REASON_NOT_AUTHED);
		}
	}
	
	@Override
	public boolean encrypt(Buffer data, int offset, int size)
	{
		try
		{
			return _encryption.encrypt(data, offset, size);
		}
		catch (IOException e)
		{
			return false;
		}
	}
	
	@Override
	public boolean decrypt(Buffer data, int offset, int size)
	{
		boolean decrypted;
		try
		{
			decrypted = _encryption.decrypt(data, offset, size);
		}
		catch (IOException e)
		{
			close();
			return false;
		}
		
		if (!decrypted)
		{
			close();
		}
		
		return decrypted;
	}
	
	@Override
	public void onConnected()
	{
		sendPacket(new Init(this));
	}
	
	@Override
	public void onDisconnection()
	{
		if (!_joinedGS)
		{
			LoginController.getInstance().removeAuthedLoginClient(_account);
			try
			{
				Thread.sleep(DISCONNECT_GRACE_MS);
			}
			catch (InterruptedException e)
			{
			}
		}
	}
	
	public byte[] getBlowfishKey()
	{
		return _blowfishKey;
	}
	
	public byte[] getScrambledModulus()
	{
		return _scrambledPair.getScrambledModulus();
	}
	
	public RSAPrivateKey getRSAPrivateKey()
	{
		return (RSAPrivateKey) _scrambledPair.getPrivateKey();
	}
	
	public String getIp()
	{
		return _ip;
	}
	
	public int getSessionId()
	{
		return _sessionId;
	}
	
	public long getConnectionStartTime()
	{
		return _connectionStartTime;
	}
	
	public String getAccount()
	{
		return _account;
	}
	
	public void setAccount(String account)
	{
		_account = account;
	}
	
	public void setAccessLevel(int accessLevel)
	{
		_accessLevel = accessLevel;
	}
	
	public int getAccessLevel()
	{
		return _accessLevel;
	}
	
	public void setLastServer(int lastServer)
	{
		_lastServer = lastServer;
	}
	
	public int getLastServer()
	{
		return _lastServer;
	}
	
	public boolean hasJoinedGS()
	{
		return _joinedGS;
	}
	
	public void setJoinedGS(boolean value)
	{
		_joinedGS = value;
	}
	
	public void setSessionKey(SessionKey sessionKey)
	{
		_sessionKey = sessionKey;
	}
	
	public SessionKey getSessionKey()
	{
		return _sessionKey;
	}
	
	public void setCharsOnServ(int servId, int chars)
	{
		if (_charsOnServers == null)
		{
			_charsOnServers = new HashMap<>();
		}
		_charsOnServers.put(servId, chars);
	}
	
	public Map<Integer, Integer> getCharsOnServ()
	{
		return _charsOnServers;
	}
	
	public void serCharsWaitingDelOnServ(int servId, long[] charsToDel)
	{
		if (_charsToDelete == null)
		{
			_charsToDelete = new HashMap<>();
		}
		_charsToDelete.put(servId, charsToDel);
	}
	
	public Map<Integer, long[]> getCharsWaitingDelOnServ()
	{
		return _charsToDelete;
	}
	
	public ConnectionState getConnectionState()
	{
		return _connectionState;
	}
	
	public void setConnectionState(ConnectionState connectionState)
	{
		_connectionState = connectionState;
	}
	
	public void sendPacket(LoginServerPacket packet)
	{
		writePacket(packet);
	}
	
	public void close(LoginFailReason reason)
	{
		sendPacket(new LoginFail(reason));
	}
	
	public void close(PlayFailReason reason)
	{
		close(new PlayFail(reason));
	}
	
	public void close(AccountKickedReason reason)
	{
		close(new AccountKicked(reason));
	}
	
	@Override
	public String toString()
	{
		final String ip = getHostAddress();
		final StringBuilder sb = new StringBuilder();
		sb.append(getClass().getSimpleName());
		sb.append(" [");
		if (_account != null)
		{
			sb.append("Account: ");
			sb.append(_account);
		}
		if (ip != null)
		{
			if (_account != null)
			{
				sb.append(" - ");
			}
			sb.append("IP: ");
			sb.append(ip.isEmpty() ? "disconnected" : ip);
		}
		sb.append("]");
		return sb.toString();
	}
}
