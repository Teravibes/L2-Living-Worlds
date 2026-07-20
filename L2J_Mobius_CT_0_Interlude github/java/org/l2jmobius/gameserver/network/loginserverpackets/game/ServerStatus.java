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
 * Announces Game Server availability and classification to the Login Server using a compact attribute list.<br>
 * Binary layout: <i>[id:byte][count:int][(attrId:int, attrValue:int)*count]</i>. Protocol compatible.
 * <ul>
 * <li>Flat <b>int[]</b> storage (id/value pairs) to minimize GC and improve cache locality.</li>
 * <li>There are no changes to the logic or API compared to the legacy implementation used in the original.</li>
 * </ul>
 * @author BazookaRpm
 */
public class ServerStatus extends BaseWritablePacket
{
	// Packet id.
	private static final int PACKET_ID = 0x06;
	
	// Generic toggles.
	public static final int ON = 0x01;
	public static final int OFF = 0x00;
	
	// Age gates.
	public static final int SERVER_AGE_ALL = 0x00;
	public static final int SERVER_AGE_15 = 0x0F;
	public static final int SERVER_AGE_18 = 0x12;
	
	// Server type bit flags.
	public static final int SERVER_NORMAL = 0x01;
	public static final int SERVER_RELAX = 0x02;
	public static final int SERVER_TEST = 0x04;
	public static final int SERVER_NOLABEL = 0x08;
	public static final int SERVER_CREATION_RESTRICTED = 0x10;
	public static final int SERVER_EVENT = 0x20;
	public static final int SERVER_FREE = 0x40;
	
	// Status codes.
	public static final int STATUS_AUTO = 0x00;
	public static final int STATUS_GOOD = 0x01;
	public static final int STATUS_NORMAL = 0x02;
	public static final int STATUS_FULL = 0x03;
	public static final int STATUS_DOWN = 0x04;
	public static final int STATUS_GM_ONLY = 0x05;
	
	// Attribute identifiers expected by the login list.
	public static final int SERVER_LIST_STATUS = 0x01;
	public static final int SERVER_TYPE = 0x02;
	public static final int SERVER_LIST_SQUARE_BRACKET = 0x03;
	public static final int MAX_PLAYERS = 0x04;
	public static final int SERVER_AGE = 0x05;
	
	// Optional human-readable labels (not part of the wire format).
	public static final String[] STATUS_STRING =
	{
		"Auto",
		"Good",
		"Normal",
		"Full",
		"Down",
		"Gm Only"
	};
	
	// Packed storage: [id0,val0,id1,val1,...].
	private static final int INITIAL_PAIRS = 16;
	private int[] _packedPairs = new int[INITIAL_PAIRS * 2];
	private int _pairCount = 0;
	
	/**
	 * Creates an empty status packet with preallocated capacity.
	 */
	public ServerStatus()
	{
		// No-op.
	}
	
	/**
	 * Adds an attribute pair to the payload, growing the internal array when needed.
	 * @param id attribute identifier
	 * @param value attribute value
	 */
	public void addAttribute(int id, int value)
	{
		int nextSize = (_pairCount + 1) * 2;
		if (nextSize > _packedPairs.length)
		{
			int newLen = _packedPairs.length << 1;
			while (newLen < nextSize)
			{
				newLen <<= 1;
			}
			
			int[] grown = new int[newLen];
			System.arraycopy(_packedPairs, 0, grown, 0, _packedPairs.length);
			_packedPairs = grown;
		}
		
		int base = _pairCount * 2;
		_packedPairs[base] = id;
		_packedPairs[base + 1] = value;
		_pairCount++;
	}
	
	@Override
	public void write()
	{
		writeByte(PACKET_ID);
		writeInt(_pairCount);
		for (int i = 0; i < _pairCount; i++)
		{
			final int base = i * 2;
			writeInt(_packedPairs[base]); // Attribute id.
			writeInt(_packedPairs[base + 1]); // Attribute value.
		}
	}
}
