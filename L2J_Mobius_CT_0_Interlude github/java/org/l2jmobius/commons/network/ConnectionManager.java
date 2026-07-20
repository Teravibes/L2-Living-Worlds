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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.l2jmobius.commons.network.internal.MMOThreadFactory;

/**
 * Manages network connections for clients.<br>
 * This class handles the creation and management of server socket channels, and processes incoming client connections.
 * @param <T> The type of Client associated with this connection manager.
 * @author Mobius
 */
public class ConnectionManager<T extends Client<Connection<T>>>
{
	private final AsynchronousChannelGroup _group;
	private final AsynchronousServerSocketChannel _socketChannel;
	private final ConnectionConfig _config;
	private final WriteHandler<T> _writeHandler;
	private final ReadHandler<T> _readHandler;
	private final Function<Connection<T>, T> _clientFactory;
	
	/**
	 * Initializes the connection manager with the specified address, client factory, and packet handler.
	 * @param address The address to bind the server socket.
	 * @param clientFactory Factory function to create clients.
	 * @param packetHandler The handler for processing packets.
	 * @throws IOException If an I/O error occurs when opening the socket channel.
	 */
	public ConnectionManager(InetSocketAddress address, Function<Connection<T>, T> clientFactory, PacketHandler<T> packetHandler) throws IOException
	{
		_config = new ConnectionConfig(address);
		_clientFactory = clientFactory;
		_readHandler = new ReadHandler<>(packetHandler, new PacketExecutor<>(_config));
		_writeHandler = new WriteHandler<>();
		
		// Initialize channel group with a custom thread pool.
		_group = AsynchronousChannelGroup.withCachedThreadPool(new ThreadPoolExecutor(_config.threadPoolSize, Integer.MAX_VALUE, 1, TimeUnit.MINUTES, new SynchronousQueue<>(), new MMOThreadFactory("Server", _config.threadPriority)), 0);
		
		// Configure and bind server socket.
		_socketChannel = _group.provider().openAsynchronousServerSocketChannel(_group);
		_socketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
		_socketChannel.bind(_config.address);
		
		// Start accepting connections.
		_socketChannel.accept(null, new AcceptConnectionHandler());
	}
	
	/**
	 * Shuts down the connection manager, including the socket channel and associated resources.
	 */
	public void shutdown()
	{
		try
		{
			_socketChannel.close();
			_group.shutdown();
			_group.awaitTermination(_config.shutdownWaitTime, TimeUnit.MILLISECONDS);
			_group.shutdownNow();
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt(); // Restore interrupted status.
		}
		catch (Exception e)
		{
			// Placeholder for exception handling/logging if needed.
		}
	}
	
	private class AcceptConnectionHandler implements CompletionHandler<AsynchronousSocketChannel, Void>
	{
		@Override
		public void completed(AsynchronousSocketChannel clientChannel, Void attachment)
		{
			// Accept the next connection.
			if (_socketChannel.isOpen())
			{
				_socketChannel.accept(null, this);
			}
			
			processNewConnection(clientChannel);
		}
		
		@Override
		public void failed(Throwable t, Void attachment)
		{
			// Re-accept connections on failure if the listener is open.
			if (_socketChannel.isOpen())
			{
				_socketChannel.accept(null, this);
			}
		}
		
		private void processNewConnection(AsynchronousSocketChannel channel)
		{
			if ((channel != null) && channel.isOpen())
			{
				try
				{
					// Set TCP_NODELAY based on Nagle's algorithm usage in the configuration.
					channel.setOption(StandardSocketOptions.TCP_NODELAY, !_config.useNagle);
					
					// Establish connection with the new client.
					final Connection<T> connection = new Connection<>(channel, _readHandler, _writeHandler, _config);
					final T client = _clientFactory.apply(connection);
					connection.setClient(client);
					
					// Notify the client of connection establishment and start reading.
					client.onConnected();
					client.read();
				}
				catch (ClosedChannelException e)
				{
					// Placeholder for handling/logging ClosedChannelException if needed.
				}
				catch (Exception e)
				{
					// Close the channel on exception during setup.
					try
					{
						channel.close();
					}
					catch (IOException ioe)
					{
						// Placeholder for handling/logging IOException if needed.
					}
				}
			}
		}
	}
}
