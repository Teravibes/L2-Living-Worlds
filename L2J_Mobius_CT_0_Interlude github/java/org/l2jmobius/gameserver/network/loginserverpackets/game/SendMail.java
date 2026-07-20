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
package org.l2jmobius.gameserver.network.loginserverpackets.game;

import org.l2jmobius.commons.network.base.BaseWritablePacket;

/**
 * Requests the login server to send a templated mail to a given account.<br>
 * Includes a variable-length list of string arguments.
 * <ul>
 * <li>Opcode: 0x09.</li>
 * <li>Payload: account (String), templateId (String), argsCount (byte), args (String[]).</li>
 * </ul>
 * @author BazookaRpm
 */
public class SendMail extends BaseWritablePacket
{
	// Opcode.
	private static final int OPCODE = 0x09;
	
	// Data.
	private final String _accountName;
	private final String _mailId;
	private final String[] _args;
	
	/**
	 * @param accountName target account
	 * @param mailId template identifier
	 * @param args template parameters
	 */
	public SendMail(String accountName, String mailId, String... args)
	{
		_accountName = (accountName != null) ? accountName : "";
		_mailId = (mailId != null) ? mailId : "";
		_args = (args != null) ? args : new String[0];
	}
	
	/**
	 * Serializes opcode, account, template, count and all parameters.
	 */
	@Override
	public void write()
	{
		writeByte(OPCODE);
		writeString(_accountName);
		writeString(_mailId);
		writeByte(_args.length);
		for (String a : _args)
		{
			writeString((a != null) ? a : "");
		}
	}
}
