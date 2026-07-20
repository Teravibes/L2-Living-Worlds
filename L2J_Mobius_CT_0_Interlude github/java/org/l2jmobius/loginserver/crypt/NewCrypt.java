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
package org.l2jmobius.loginserver.crypt;

import java.io.IOException;

import org.l2jmobius.commons.network.Buffer;

/**
 * Handles Blowfish cipher encryption with ECB processing for network communication.<br>
 * Provides packet integrity verification and XOR encryption for secure data exchange.
 * <ul>
 * <li>Checksum validation for packet integrity verification.</li>
 * <li>XOR encryption for initial handshake communication.</li>
 * <li>Blowfish encryption/decryption for secure packet transmission.</li>
 * </ul>
 */
public class NewCrypt
{
	// Constants.
	private static final int CHECKSUM_SIZE = 4;
	private static final int XOR_KEY_SIZE = 4;
	private static final int MINIMUM_PACKET_SIZE = 4;
	private static final int SIZE_ALIGNMENT_MASK = 3;
	private static final int XOR_OFFSET_ADJUSTMENT = 8;
	
	// Encryption engines.
	private final BlowfishEngine _encryptionEngine;
	private final BlowfishEngine _decryptionEngine;
	
	/**
	 * Creates a new crypt instance with byte array key.
	 * @param blowfishKey the encryption key as byte array
	 */
	public NewCrypt(byte[] blowfishKey)
	{
		_encryptionEngine = new BlowfishEngine();
		_encryptionEngine.init(true, blowfishKey);
		_decryptionEngine = new BlowfishEngine();
		_decryptionEngine.init(false, blowfishKey);
	}
	
	/**
	 * Creates a new crypt instance with string key.
	 * @param key the encryption key as string
	 */
	public NewCrypt(String key)
	{
		this(key.getBytes());
	}
	
	/**
	 * Verifies packet checksum integrity by comparing calculated XOR checksum with stored value.
	 * @param data the buffer containing packet data
	 * @param offset the starting position in buffer
	 * @param size the total size of data including checksum
	 * @return true if checksum is valid, false otherwise
	 */
	public static boolean verifyChecksum(Buffer data, int offset, int size)
	{
		// Check if size is multiple of 4 and if there is more than only the checksum.
		if (((size & SIZE_ALIGNMENT_MASK) != 0) || (size <= MINIMUM_PACKET_SIZE))
		{
			return false;
		}
		
		long checksum = 0;
		final int count = size - CHECKSUM_SIZE;
		int i;
		for (i = offset; i < count; i += CHECKSUM_SIZE)
		{
			checksum ^= data.readInt(i);
		}
		
		return data.readInt(i) == checksum;
	}
	
	/**
	 * Appends XOR checksum to packet data for integrity verification.
	 * @param data the buffer containing packet data
	 * @param offset the starting position in buffer
	 * @param size the total size including space for checksum
	 */
	public static void appendChecksum(Buffer data, int offset, int size)
	{
		int checksum = 0;
		final int count = size - CHECKSUM_SIZE;
		int i;
		for (i = offset; i < count; i += CHECKSUM_SIZE)
		{
			checksum ^= data.readInt(i);
		}
		
		data.writeInt(i, checksum);
	}
	
	/**
	 * Encrypts packet data using XOR encoding with progressive key modification.<br>
	 * The XOR key is written to the final 4 bytes of the encrypted data.
	 * @param rawData the buffer containing raw packet data
	 * @param offset the beginning of data to encrypt
	 * @param size the length of data to encrypt
	 * @param xorKey the initial XOR key value
	 */
	public static void encXORPass(Buffer rawData, int offset, int size, int xorKey)
	{
		final int stopPosition = size - XOR_OFFSET_ADJUSTMENT;
		int currentPosition = XOR_KEY_SIZE + offset;
		int dataValue;
		int progressiveKey = xorKey; // Initial XOR key.
		while (currentPosition < stopPosition)
		{
			dataValue = rawData.readInt(currentPosition);
			progressiveKey += dataValue;
			dataValue ^= progressiveKey;
			rawData.writeInt(currentPosition, dataValue);
			currentPosition += XOR_KEY_SIZE;
		}
		
		rawData.writeInt(currentPosition, progressiveKey);
	}
	
	/**
	 * Decrypts buffer data using Blowfish decryption in ECB mode.
	 * @param rawData the buffer containing encrypted data
	 * @param offset the starting position for decryption
	 * @param size the total size of data to decrypt
	 * @throws IOException if decryption process fails
	 */
	public synchronized void decrypt(Buffer rawData, int offset, int size) throws IOException
	{
		final int blockSize = _decryptionEngine.getBlockSize();
		final int blockCount = size / blockSize;
		for (int i = 0; i < blockCount; i++)
		{
			_decryptionEngine.processBlock(rawData, offset + (i * blockSize));
		}
	}
	
	/**
	 * Encrypts buffer data using Blowfish encryption in ECB mode.
	 * @param rawData the buffer containing plaintext data
	 * @param offset the starting position for encryption
	 * @param size the total size of data to encrypt
	 * @throws IOException if encryption process fails
	 */
	public synchronized void crypt(Buffer rawData, int offset, int size) throws IOException
	{
		int blockSize = _encryptionEngine.getBlockSize();
		int blockCount = size / blockSize;
		for (int i = 0; i < blockCount; i++)
		{
			_encryptionEngine.processBlock(rawData, offset + (i * blockSize));
		}
	}
}
