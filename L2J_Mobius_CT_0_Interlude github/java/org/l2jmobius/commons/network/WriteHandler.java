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
package org.l2jmobius.commons.network;

import java.nio.channels.CompletionHandler;

/**
 * Handles the completion of write operations for network clients.<br>
 * This class implements {@link CompletionHandler} to process the results of data writing to the client, ensuring proper handling of the write operation's conclusion.
 * @param <T> The type of Client associated with this write handler.
 * @author JoeAlisson, Mobius
 */
public class WriteHandler<T extends Client<Connection<T>>> implements CompletionHandler<Long, T>
{
	@Override
	public void completed(Long result, T client)
	{
		// If client is null, there's nothing to handle, possibly due to disconnection.
		if (client == null)
		{
			return;
		}
		
		// Negative result indicates failure to send data, possibly due to client disconnection.
		final int bytesWritten = result.intValue();
		if (bytesWritten < 0)
		{
			if (client.isConnected())
			{
				client.disconnect();
			}
			return;
		}
		
		// If there is still data remaining to send, resume sending with the remaining data.
		if ((bytesWritten > 0) && (bytesWritten < client.getDataSentSize()))
		{
			client.resumeSend(bytesWritten);
		}
		else // All data sent, finish the writing process.
		{
			client.finishWriting();
		}
	}
	
	@Override
	public void failed(Throwable e, T client)
	{
		// Handle failures, disconnecting the client if an error occurs.
		client.disconnect();
	}
}
