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
package modules.helloworld;

import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.modules.GameModule;
import org.l2jmobius.gameserver.modules.ModuleContext;

/**
 * The Hello World module. It proves the module framework end to end: the platform discovers this directory, validates
 * its manifest and api version, loads its {@code config/module.ini}, compiles this script, instantiates this entry
 * point, and calls {@link #onEnable}. When the switch is on, it registers a single voiced command, {@code .hello}, that
 * replies with the configured greeting. When the switch is off, or the directory is removed, the server is stock.
 */
public class HelloWorldModule implements GameModule
{
	@Override
	public void onEnable(ModuleContext context)
	{
		if (!context.config().getBoolean("Enabled", false))
		{
			return; // Switch off: register nothing, behave as stock.
		}

		final String greeting = context.config().getString("Greeting", "Hello from the module framework!");
		context.handlers().registerVoicedCommand(new HelloVoicedCommand(greeting));
		context.logging().info("Hello World module enabled, registered voiced command .hello");
	}

	private static class HelloVoicedCommand implements IVoicedCommandHandler
	{
		private static final String[] COMMANDS =
		{
			"hello"
		};

		private final String _greeting;

		HelloVoicedCommand(String greeting)
		{
			_greeting = greeting;
		}

		@Override
		public boolean onCommand(String command, Player player, String params)
		{
			if (player == null)
			{
				return false;
			}

			player.sendMessage(_greeting);
			return true;
		}

		@Override
		public String[] getCommandList()
		{
			return COMMANDS;
		}
	}
}
