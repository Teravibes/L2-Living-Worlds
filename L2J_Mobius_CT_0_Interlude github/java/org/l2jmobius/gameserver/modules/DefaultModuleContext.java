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
 * The platform's implementation of {@link ModuleContext}, built by {@link ModuleManager} for one module and handed to
 * its entry point.
 */
class DefaultModuleContext implements ModuleContext
{
	private final ModuleConfig _config;
	private final ModuleHandlers _handlers;
	private final ModuleEvents _events;
	private final Logger _logger;

	DefaultModuleContext(String moduleId, ModuleConfig config, ModuleHandlers handlers, ModuleEvents events)
	{
		_config = config;
		_handlers = handlers;
		_events = events;
		_logger = Logger.getLogger("module." + moduleId);
	}

	@Override
	public ModuleConfig config()
	{
		return _config;
	}

	@Override
	public ModuleHandlers handlers()
	{
		return _handlers;
	}

	@Override
	public ModuleEvents events()
	{
		return _events;
	}

	@Override
	public Logger logging()
	{
		return _logger;
	}
}
