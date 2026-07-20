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

import org.l2jmobius.commons.network.Buffer;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.loginserver.crypt.NewCrypt;

/**
 * Handles login protocol encryption for a single connection.<br>
 * Manages the transition from the static bootstrap key to the session-specific key and provides helpers for computing and applying packet-level encryption.
 * <ul>
 * <li>Uses a shared static Blowfish key for the initial handshake.</li>
 * <li>Switches to a per-session Blowfish key after the first encrypted packet.</li>
 * <li>Handles checksum or XOR header according to protocol phase.</li>
 * </ul>
 * @author BazookaRpm
 */
public class LoginEncryption
{
	// Static Blowfish key used for the initial handshake.
	private static final byte[] STATIC_BLOWFISH_KEY =
	{
		(byte) 0x6b,
		(byte) 0x60,
		(byte) 0xcb,
		(byte) 0x5b,
		(byte) 0x82,
		(byte) 0xce,
		(byte) 0x90,
		(byte) 0xb1,
		(byte) 0xcc,
		(byte) 0x2b,
		(byte) 0x6c,
		(byte) 0x55,
		(byte) 0x6c,
		(byte) 0x6c,
		(byte) 0x6c,
		(byte) 0x6c
	};
	
	// Static crypt instance used while the protocol is in the bootstrap phase.
	private static final NewCrypt STATIC_CRYPT = new NewCrypt(STATIC_BLOWFISH_KEY);
	
	// Blowfish block size and protocol overhead constants.
	private static final int BLOWFISH_BLOCK_SIZE = 8;
	private static final int STATIC_HEADER_SIZE = 8;
	private static final int DYNAMIC_HEADER_SIZE = 4;
	private static final int CHECKSUM_SIZE = 8;
	
	// Session-specific crypt instance, set once the client key is negotiated.
	private NewCrypt _sessionCrypt;
	
	// Tracks whether the connection is still using the static key for encryption.
	private boolean _usingStaticKey = true;
	
	/**
	 * Sets the session-specific Blowfish key used after the static phase.
	 * @param key the negotiated session Blowfish key
	 */
	public void setKey(byte[] key)
	{
		_sessionCrypt = new NewCrypt(key);
	}
	
	/**
	 * Decrypts a packet using the session-specific Blowfish key and verifies its checksum.
	 * @param data the backing buffer
	 * @param offset the start offset of the packet
	 * @param size the packet size in bytes
	 * @return {@code true} if the checksum is valid, {@code false} otherwise
	 * @throws IOException if the underlying buffer operations fail
	 */
	public boolean decrypt(Buffer data, int offset, int size) throws IOException
	{
		_sessionCrypt.decrypt(data, offset, size);
		return NewCrypt.verifyChecksum(data, offset, size);
	}
	
	/**
	 * Computes the encrypted packet size for a given plaintext payload size, based on the current protocol phase (static vs. session key).
	 * @param dataSize the plaintext payload size
	 * @return the encrypted packet size including headers, padding and checksum/XOR data
	 */
	public int encryptedSize(int dataSize)
	{
		final int headerSize = _usingStaticKey ? STATIC_HEADER_SIZE : DYNAMIC_HEADER_SIZE;
		int sizeWithHeader = dataSize + headerSize;
		
		// Align to Blowfish block size (always pad at least one block).
		final int remainder = sizeWithHeader % BLOWFISH_BLOCK_SIZE;
		sizeWithHeader += (BLOWFISH_BLOCK_SIZE - remainder);
		
		// Append checksum/XOR header size.
		return sizeWithHeader + CHECKSUM_SIZE;
	}
	
	/**
	 * Encrypts the packet currently stored in the buffer using either the static bootstrap key or the session-specific key depending on the internal state.
	 * @param data the backing buffer
	 * @param offset the start offset of the plaintext payload
	 * @param size the plaintext payload size
	 * @return {@code true} once encryption is completed
	 * @throws IOException if the underlying buffer operations fail
	 */
	public boolean encrypt(Buffer data, int offset, int size) throws IOException
	{
		final int packetSize = encryptedSize(size);
		final int packetEndOffset = offset + packetSize;
		
		data.limit(packetEndOffset);
		
		if (_usingStaticKey)
		{
			encryptWithStaticKey(data, offset, packetEndOffset);
			_usingStaticKey = false;
		}
		else
		{
			encryptWithSessionKey(data, offset, packetEndOffset);
		}
		
		return true;
	}
	
	/**
	 * Encrypts the packet using the static Blowfish key and XOR header.
	 * @param data the backing buffer
	 * @param offset the start offset of the packet
	 * @param packetEndOffset the offset immediately after the last byte of the packet
	 * @throws IOException if the underlying buffer operations fail
	 */
	private void encryptWithStaticKey(Buffer data, int offset, int packetEndOffset) throws IOException
	{
		NewCrypt.encXORPass(data, offset, packetEndOffset, Rnd.nextInt());
		STATIC_CRYPT.crypt(data, offset, packetEndOffset);
	}
	
	/**
	 * Encrypts the packet using the session-specific Blowfish key and checksum.
	 * @param data the backing buffer
	 * @param offset the start offset of the packet
	 * @param packetEndOffset the offset immediately after the last byte of the packet
	 * @throws IOException if the underlying buffer operations fail
	 */
	private void encryptWithSessionKey(Buffer data, int offset, int packetEndOffset) throws IOException
	{
		NewCrypt.appendChecksum(data, offset, packetEndOffset);
		_sessionCrypt.crypt(data, offset, packetEndOffset);
	}
}
