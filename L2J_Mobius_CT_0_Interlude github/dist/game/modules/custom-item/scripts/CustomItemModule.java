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
package modules.customitem;

import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.modules.GameModule;
import org.l2jmobius.gameserver.modules.ModuleContext;

/**
 * The Custom Item module. It proves that a module can ship its own datapack content and have a stock loader pick it up:
 * the module declares a {@code data/items} resource root in its manifest, the platform registers that root during
 * discovery, and {@code ItemData} loads the Living World Token (id 60000) from it. On enable it registers a single
 * voiced command, {@code .token}, that grants the token so the loaded item can be seen in game. When the switch is off,
 * or the directory is removed, the item is never loaded and the server is stock.
 */
public class CustomItemModule implements GameModule
{
	/** The item id owned by this module. It sits in the reserved range 60000-60009 declared in the manifest. */
	private static final int TOKEN_ITEM_ID = 60000;

	@Override
	public void onEnable(ModuleContext context)
	{
		if (!context.config().getBoolean("Enabled", false))
		{
			return; // Switch off: register nothing, behave as stock.
		}

		final int grantCount = Math.max(1, context.config().getInt("GrantCount", 1));
		context.handlers().registerVoicedCommand(new TokenVoicedCommand(grantCount));
		context.logging().info("Custom Item module enabled, registered voiced command .token granting item " + TOKEN_ITEM_ID);
	}

	private static class TokenVoicedCommand implements IVoicedCommandHandler
	{
		private static final String[] COMMANDS =
		{
			"token"
		};

		private final int _grantCount;

		TokenVoicedCommand(int grantCount)
		{
			_grantCount = grantCount;
		}

		@Override
		public boolean onCommand(String command, Player player, String params)
		{
			if (player == null)
			{
				return false;
			}

			player.addItem(ItemProcessType.REWARD, TOKEN_ITEM_ID, _grantCount, player, true);
			return true;
		}

		@Override
		public String[] getCommandList()
		{
			return COMMANDS;
		}
	}
}
