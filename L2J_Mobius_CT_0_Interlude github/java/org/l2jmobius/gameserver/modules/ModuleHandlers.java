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
package org.l2jmobius.gameserver.modules;

import java.util.Arrays;

import org.l2jmobius.gameserver.handler.AdminCommandHandler;
import org.l2jmobius.gameserver.handler.BypassHandler;
import org.l2jmobius.gameserver.handler.EffectHandler;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.handler.ITargetTypeHandler;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.handler.ItemHandler;
import org.l2jmobius.gameserver.handler.TargetHandler;
import org.l2jmobius.gameserver.handler.VoicedCommandHandler;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;

/**
 * The handler registration surface handed to a module through {@link ModuleContext#handlers()}.
 * <p>
 * A module registers through this rather than editing {@code MasterHandler}. Every registration goes into the stock
 * handler singleton and is also recorded against the module (see {@link ModuleHandles}) so the platform can report what
 * each module contributed. The registrations available in Milestone 1 are the command and item handlers that need no
 * new platform capability; more are added as later milestones need them.
 */
public class ModuleHandlers
{
	private final ModuleHandles _handles;

	ModuleHandlers(ModuleHandles handles)
	{
		_handles = handles;
	}

	/**
	 * Registers a voiced command handler (a dot command in chat).
	 * @param handler the handler to register
	 */
	public void registerVoicedCommand(IVoicedCommandHandler handler)
	{
		VoicedCommandHandler.getInstance().registerHandler(handler);
		_handles.record("voiced command " + Arrays.toString(handler.getCommandList()));
	}

	/**
	 * Registers an admin command handler.
	 * @param handler the handler to register
	 */
	public void registerAdminCommand(IAdminCommandHandler handler)
	{
		AdminCommandHandler.getInstance().registerHandler(handler);
		_handles.record("admin command " + Arrays.toString(handler.getCommandList()));
	}

	/**
	 * Registers a bypass handler (a link target from an html window).
	 * @param handler the handler to register
	 */
	public void registerBypass(IBypassHandler handler)
	{
		BypassHandler.getInstance().registerHandler(handler);
		_handles.record("bypass " + Arrays.toString(handler.getCommandList()));
	}

	/**
	 * Registers an item handler (a custom item being used).
	 * @param handler the handler to register
	 */
	public void registerItem(IItemHandler handler)
	{
		ItemHandler.getInstance().registerHandler(handler);
		_handles.record("item handler " + handler.getClass().getSimpleName());
	}

	/**
	 * Registers a custom effect by its class, so a skill can name it in {@code <effect name="...">}. The class simple
	 * name is the name skills reference, the same contract the stock effect master handler uses.
	 * @param effect the effect class to register
	 */
	public void registerEffect(Class<? extends AbstractEffect> effect)
	{
		EffectHandler.getInstance().registerHandler(effect);
		_handles.record("effect handler " + effect.getSimpleName());
	}

	/**
	 * Registers a custom target-type handler, so a skill can use a target type this module defines.
	 * @param handler the target-type handler to register
	 */
	public void registerTarget(ITargetTypeHandler handler)
	{
		TargetHandler.getInstance().registerHandler(handler);
		_handles.record("target handler " + handler.getClass().getSimpleName());
	}
}
