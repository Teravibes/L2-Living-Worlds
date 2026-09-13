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

import java.util.logging.Logger;

/**
 * The single stable object a module receives in {@link GameModule#onEnable}. It exposes the supported extension
 * operations; everyday game types such as {@code Player}, {@code Npc}, {@code Skill}, and {@code Item} are used directly
 * and are not wrapped.
 * <p>
 * It exposes configuration, handler registration, event-listener registration, and a module-scoped logger. The hooks
 * and resource surfaces described in the framework specification are added in later milestones, as the first module that
 * needs each one drives it.
 */
public interface ModuleContext
{
	/**
	 * @return generic typed access to this module's own {@code config/module.ini}
	 */
	ModuleConfig config();

	/**
	 * @return the handler registration surface for this module
	 */
	ModuleHandlers handlers();

	/**
	 * @return the event-listener registration surface for this module
	 */
	ModuleEvents events();

	/**
	 * @return a logger scoped to this module, so its output is attributable
	 */
	Logger logging();
}
