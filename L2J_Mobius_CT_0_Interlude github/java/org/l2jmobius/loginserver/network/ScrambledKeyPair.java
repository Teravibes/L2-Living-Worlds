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

import java.math.BigInteger;
import java.security.Key;
import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;

public class ScrambledKeyPair
{
	private final KeyPair _pair;
	private final byte[] _scrambledModulus;
	
	public ScrambledKeyPair(KeyPair pair)
	{
		_pair = pair;
		_scrambledModulus = scrambleModulus(((RSAPublicKey) _pair.getPublic()).getModulus());
	}
	
	private byte[] scrambleModulus(BigInteger modulus)
	{
		byte[] scrambledMod = modulus.toByteArray();
		if ((scrambledMod.length == 0x81) && (scrambledMod[0] == 0x00))
		{
			final byte[] temp = new byte[0x80];
			System.arraycopy(scrambledMod, 1, temp, 0, 0x80);
			scrambledMod = temp;
		}
		
		// Step 1 : 0x4d-0x50 <-> 0x00-0x04.
		for (int i = 0; i < 4; i++)
		{
			final byte temp = scrambledMod[0x00 + i];
			scrambledMod[0x00 + i] = scrambledMod[0x4d + i];
			scrambledMod[0x4d + i] = temp;
		}
		
		// Step 2 : xor first 0x40 bytes with last 0x40 bytes.
		for (int i = 0; i < 0x40; i++)
		{
			scrambledMod[i] = (byte) (scrambledMod[i] ^ scrambledMod[0x40 + i]);
		}
		
		// Step 3 : xor bytes 0x0d-0x10 with bytes 0x34-0x38.
		for (int i = 0; i < 4; i++)
		{
			scrambledMod[0x0d + i] = (byte) (scrambledMod[0x0d + i] ^ scrambledMod[0x34 + i]);
		}
		
		// Step 4 : xor last 0x40 bytes with first 0x40 bytes.
		for (int i = 0; i < 0x40; i++)
		{
			scrambledMod[0x40 + i] = (byte) (scrambledMod[0x40 + i] ^ scrambledMod[i]);
		}
		
		return scrambledMod;
	}
	
	public byte[] getScrambledModulus()
	{
		return _scrambledModulus;
	}
	
	public Key getPrivateKey()
	{
		return _pair.getPrivate();
	}
	
	public Key getPublicKey()
	{
		return _pair.getPublic();
	}
}
