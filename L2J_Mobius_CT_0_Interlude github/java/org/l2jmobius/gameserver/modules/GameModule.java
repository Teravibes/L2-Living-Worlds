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

/**
 * The entry-point contract every module implements.
 * <p>
 * The platform instantiates the class named by the manifest {@code entrypoint} and calls {@link #onEnable} once at
 * startup, handing it a {@link ModuleContext}. An entry point must check its own enable switch through
 * {@code context.config()} before registering anything, so a disabled module changes nothing.
 * <p>
 * V1 is restart-based: {@link #onDisable} is reserved for a future hot-unload path and is not called today.
 */
public interface GameModule
{
	/**
	 * Called once at server startup for an installed module.
	 * @param context the supported extension surface for this module
	 */
	void onEnable(ModuleContext context);

	/**
	 * Reserved for a future hot-unload path. Not called in V1, which applies enable and disable on restart.
	 * @param context the supported extension surface for this module
	 */
	default void onDisable(ModuleContext context)
	{
		// No-op in V1.
	}
}
