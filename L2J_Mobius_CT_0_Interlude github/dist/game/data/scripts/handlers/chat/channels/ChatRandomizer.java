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
package handlers.chat.channels;

import org.l2jmobius.commons.util.Rnd;

/**
 * @author Mobius
 */
class ChatRandomizer
{
	static String randomize(String text)
	{
		final StringBuilder textOut = new StringBuilder();
		for (char c : text.toCharArray())
		{
			if ((c > 96) && (c < 123))
			{
				textOut.append(Character.toString((char) Rnd.get(96, 123)));
			}
			else if ((c > 64) && (c < 91))
			{
				textOut.append(Character.toString((char) Rnd.get(64, 91)));
			}
			else if ((c == 32) || (c == 44) || (c == 46))
			{
				textOut.append(c);
			}
			else
			{
				textOut.append(Character.toString((char) Rnd.get(47, 64)));
			}
		}
		
		return textOut.toString();
	}
}
