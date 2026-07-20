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
package org.l2jmobius.commons.crypt;

/**
 * Blowfish cipher implementation with ECB processing for L2J packet encryption.<br>
 * Provides checksum validation and XOR encryption for secure server communication.
 * <ul>
 * <li>Packet checksum verification and generation for integrity validation.</li>
 * <li>XOR pass encryption for initial login server to game client handshake.</li>
 * <li>Blowfish ECB encryption/decryption for ongoing packet security.</li>
 * </ul>
 */
public class NewCrypt
{
	// Constants.
	private static final int BYTES_PER_BLOCK = 4;
	private static final int CHECKSUM_SIZE = 4;
	private static final int BLOWFISH_BLOCK_SIZE = 8;
	private static final int XOR_KEY_OFFSET = 4;
	private static final int XOR_FINAL_OFFSET = 8;
	private static final int BYTE_MASK = 0xFF;
	private static final int SHIFT_8_BITS = 8;
	private static final int SHIFT_16_BITS = 16;
	private static final int SHIFT_24_BITS = 24;
	private static final long MASK_16_BITS = 0xFF00;
	private static final long MASK_24_BITS = 0xFF0000;
	private static final long MASK_32_BITS = 0xFF000000;
	
	// Encryption Engine.
	private final BlowfishEngine _blowfishCipher;
	
	/**
	 * Creates new crypt instance with blowfish key bytes.
	 * @param blowfishKey
	 */
	public NewCrypt(byte[] blowfishKey)
	{
		_blowfishCipher = new BlowfishEngine();
		_blowfishCipher.init(blowfishKey);
	}
	
	/**
	 * Creates new crypt instance with string key converted to bytes.
	 * @param key
	 */
	public NewCrypt(String key)
	{
		this(key.getBytes());
	}
	
	/**
	 * Verifies packet checksum for entire data array.
	 * @param rawData data array to be verified
	 * @return true when the checksum of the data is valid, false otherwise
	 */
	public static boolean verifyChecksum(byte[] rawData)
	{
		return verifyChecksum(rawData, 0, rawData.length);
	}
	
	/**
	 * Verifies packet checksum for data integrity validation.<br>
	 * Used for login server to game client and game server communication.
	 * @param rawData data array to be verified
	 * @param offset at which offset to start verifying
	 * @param size number of bytes to verify
	 * @return true if the checksum of the data is valid, false otherwise
	 */
	public static boolean verifyChecksum(byte[] rawData, int offset, int size)
	{
		// Check if size is multiple of 4 and if there is more than only the checksum.
		if (((size & 3) != 0) || (size <= CHECKSUM_SIZE))
		{
			return false;
		}
		
		long calculatedChecksum = 0;
		final int dataLength = size - CHECKSUM_SIZE;
		long currentBlock = -1;
		int position;
		
		for (position = offset; position < dataLength; position += BYTES_PER_BLOCK)
		{
			currentBlock = rawData[position] & BYTE_MASK;
			currentBlock |= (rawData[position + 1] << SHIFT_8_BITS) & MASK_16_BITS;
			currentBlock |= (rawData[position + 2] << SHIFT_16_BITS) & MASK_24_BITS;
			currentBlock |= (rawData[position + 3] << SHIFT_24_BITS) & MASK_32_BITS;
			
			calculatedChecksum ^= currentBlock;
		}
		
		currentBlock = rawData[position] & BYTE_MASK;
		currentBlock |= (rawData[position + 1] << SHIFT_8_BITS) & MASK_16_BITS;
		currentBlock |= (rawData[position + 2] << SHIFT_16_BITS) & MASK_24_BITS;
		currentBlock |= (rawData[position + 3] << SHIFT_24_BITS) & MASK_32_BITS;
		
		return currentBlock == calculatedChecksum;
	}
	
	/**
	 * Appends packet checksum to entire data array.
	 * @param rawData data array to compute the checksum from
	 */
	public static void appendChecksum(byte[] rawData)
	{
		appendChecksum(rawData, 0, rawData.length);
	}
	
	/**
	 * Computes and appends packet checksum at the end of the packet.
	 * @param rawData data array to compute the checksum from
	 * @param offset offset where to start in the data array
	 * @param size number of bytes to compute the checksum from
	 */
	public static void appendChecksum(byte[] rawData, int offset, int size)
	{
		long calculatedChecksum = 0;
		final int dataLength = size - CHECKSUM_SIZE;
		long currentBlock;
		int position;
		
		for (position = offset; position < dataLength; position += BYTES_PER_BLOCK)
		{
			currentBlock = rawData[position] & BYTE_MASK;
			currentBlock |= (rawData[position + 1] << SHIFT_8_BITS) & MASK_16_BITS;
			currentBlock |= (rawData[position + 2] << SHIFT_16_BITS) & MASK_24_BITS;
			currentBlock |= (rawData[position + 3] << SHIFT_24_BITS) & MASK_32_BITS;
			
			calculatedChecksum ^= currentBlock;
		}
		
		currentBlock = rawData[position] & BYTE_MASK;
		currentBlock |= (rawData[position + 1] << SHIFT_8_BITS) & MASK_16_BITS;
		currentBlock |= (rawData[position + 2] << SHIFT_16_BITS) & MASK_24_BITS;
		currentBlock |= (rawData[position + 3] << SHIFT_24_BITS) & MASK_32_BITS;
		
		rawData[position] = (byte) (calculatedChecksum & BYTE_MASK);
		rawData[position + 1] = (byte) ((calculatedChecksum >> SHIFT_8_BITS) & BYTE_MASK);
		rawData[position + 2] = (byte) ((calculatedChecksum >> SHIFT_16_BITS) & BYTE_MASK);
		rawData[position + 3] = (byte) ((calculatedChecksum >> SHIFT_24_BITS) & BYTE_MASK);
	}
	
	/**
	 * Encrypts packet with XOR encoding and appends the XOR key to entire data array.<br>
	 * Assumes sufficient room exists for the key without overwriting data.
	 * @param rawData The raw bytes to be encrypted
	 * @param xorKey The 4 bytes (int) XOR key
	 */
	public static void encXORPass(byte[] rawData, int xorKey)
	{
		encXORPass(rawData, 0, rawData.length, xorKey);
	}
	
	/**
	 * Encrypts packet with XOR encoding and appends the XOR key to the data.<br>
	 * Assumes sufficient room exists for the key without overwriting data.
	 * @param rawData The raw bytes to be encrypted
	 * @param offset The beginning of the data to be encrypted
	 * @param size Length of the data to be encrypted
	 * @param xorKey The 4 bytes (int) XOR key
	 */
	public static void encXORPass(byte[] rawData, int offset, int size, int xorKey)
	{
		final int endPosition = size - XOR_FINAL_OFFSET;
		int currentPosition = XOR_KEY_OFFSET + offset;
		int dataBlock;
		int encryptionKey = xorKey; // Initial xor key.
		
		while (currentPosition < endPosition)
		{
			dataBlock = rawData[currentPosition] & BYTE_MASK;
			dataBlock |= (rawData[currentPosition + 1] & BYTE_MASK) << SHIFT_8_BITS;
			dataBlock |= (rawData[currentPosition + 2] & BYTE_MASK) << SHIFT_16_BITS;
			dataBlock |= (rawData[currentPosition + 3] & BYTE_MASK) << SHIFT_24_BITS;
			
			encryptionKey += dataBlock;
			
			dataBlock ^= encryptionKey;
			
			rawData[currentPosition++] = (byte) (dataBlock & BYTE_MASK);
			rawData[currentPosition++] = (byte) ((dataBlock >> SHIFT_8_BITS) & BYTE_MASK);
			rawData[currentPosition++] = (byte) ((dataBlock >> SHIFT_16_BITS) & BYTE_MASK);
			rawData[currentPosition++] = (byte) ((dataBlock >> SHIFT_24_BITS) & BYTE_MASK);
		}
		
		rawData[currentPosition++] = (byte) (encryptionKey & BYTE_MASK);
		rawData[currentPosition++] = (byte) ((encryptionKey >> SHIFT_8_BITS) & BYTE_MASK);
		rawData[currentPosition++] = (byte) ((encryptionKey >> SHIFT_16_BITS) & BYTE_MASK);
		rawData[currentPosition++] = (byte) ((encryptionKey >> SHIFT_24_BITS) & BYTE_MASK);
	}
	
	/**
	 * Decrypts data using Blowfish cipher in ECB mode.<br>
	 * Results are placed directly inside the raw array without error checking.
	 * @param rawData the data array to be decrypted
	 * @param offset the offset at which to start decrypting
	 * @param size the number of bytes to be decrypted
	 */
	public void decrypt(byte[] rawData, int offset, int size)
	{
		for (int i = offset; i < (offset + size); i += BLOWFISH_BLOCK_SIZE)
		{
			_blowfishCipher.decryptBlock(rawData, i);
		}
	}
	
	/**
	 * Encrypts data using Blowfish cipher in ECB mode.<br>
	 * Results are placed directly inside the raw array without error checking.
	 * @param rawData the data array to be encrypted
	 * @param offset the offset at which to start encrypting
	 * @param size the number of bytes to be encrypted
	 */
	public void crypt(byte[] rawData, int offset, int size)
	{
		for (int i = offset; i < (offset + size); i += BLOWFISH_BLOCK_SIZE)
		{
			_blowfishCipher.encryptBlock(rawData, i);
		}
	}
}
