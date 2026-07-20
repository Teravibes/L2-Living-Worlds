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

import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.Cipher;

import org.l2jmobius.commons.crypt.NewCrypt;
import org.l2jmobius.commons.network.base.BaseReadablePacket;
import org.l2jmobius.loginserver.GameServerThread;
import org.l2jmobius.loginserver.network.GameServerPacketHandler.GameServerState;

/**
 * Receives the Blowfish session key from the game server, encrypted with RSA.<br>
 * The decrypted RSA block may contain leading zero bytes that must be stripped before use.
 * @author BazookaRpm
 */
public class BlowFishKey extends BaseReadablePacket
{
	private static final Logger LOGGER = Logger.getLogger(BlowFishKey.class.getName());
	
	/**
	 * Constructor that processes the Blowfish key packet received from the game server.<br>
	 * Decrypts the RSA key and configures Blowfish encryption for the connection.
	 * @param decrypt Byte array containing the received encrypted packet
	 * @param server Game server thread instance that will process the key
	 */
	public BlowFishKey(byte[] decrypt, GameServerThread server)
	{
		super(decrypt);
		
		readByte(); // Packet id, it is already processed.
		
		// Reads the size of the RSA encrypted block.
		final int encryptedBlockSize = readInt();
		if (encryptedBlockSize <= 0)
		{
			LOGGER.warning("Invalid RSA block size for Blowfish key: " + encryptedBlockSize);
			return;
		}
		
		// Reads the encrypted data block.
		final byte[] encryptedBlock = readBytes(encryptedBlockSize);
		
		try
		{
			// Decrypts the RSA block.
			final byte[] decryptedBlock = decryptRsaBlock(encryptedBlock, server);
			
			// Extracts the Blowfish key from the decrypted block.
			final byte[] blowfishKey = extractKeyMaterial(decryptedBlock);
			
			if (blowfishKey.length == 0)
			{
				LOGGER.warning("Decrypted Blowfish key is empty.");
				return;
			}
			
			// Configures Blowfish encryption and updates connection state.
			server.setBlowFish(new NewCrypt(blowfishKey));
			server.setLoginConnectionState(GameServerState.BF_CONNECTED);
		}
		catch (GeneralSecurityException e)
		{
			LOGGER.log(Level.SEVERE, "Error while decrypting Blowfish key (RSA).", e);
		}
	}
	
	/**
	 * Decrypts a data block encrypted with RSA using the server's private key.<br>
	 * Uses the RSA/ECB/NoPadding algorithm for decryption.
	 * @param encryptedBlock Byte array containing the RSA encrypted block
	 * @param server Server instance that provides the RSA private key
	 * @return Byte array with the decrypted block
	 * @throws GeneralSecurityException If an error occurs during the decryption process
	 */
	private static byte[] decryptRsaBlock(byte[] encryptedBlock, GameServerThread server) throws GeneralSecurityException
	{
		final Cipher rsaCipher = Cipher.getInstance("RSA/ECB/NoPadding");
		rsaCipher.init(Cipher.DECRYPT_MODE, server.getPrivateKey());
		return rsaCipher.doFinal(encryptedBlock);
	}
	
	/**
	 * Extracts the useful key material from a decrypted block by removing leading zeros.<br>
	 * RSA encryption with NoPadding may add zeros at the beginning of the block that must be removed.
	 * @param decryptedBlock Byte array containing the decrypted block
	 * @return Byte array with the Blowfish key without leading zeros, or an empty array if no valid material exists
	 */
	private static byte[] extractKeyMaterial(byte[] decryptedBlock)
	{
		// Finds the first non-zero byte.
		int offset = 0;
		while ((offset < decryptedBlock.length) && (decryptedBlock[offset] == 0))
		{
			offset++;
		}
		
		// Calculates the useful key length.
		final int keyLength = decryptedBlock.length - offset;
		if (keyLength <= 0)
		{
			return new byte[0];
		}
		
		// Copies the useful key material to a new array.
		final byte[] key = new byte[keyLength];
		System.arraycopy(decryptedBlock, offset, key, 0, keyLength);
		return key;
	}
}
