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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A per-module record of what the module registered through its {@link ModuleContext}.
 * <p>
 * V1 applies enable and disable on restart, so this record is not used to unregister anything yet. It exists for
 * diagnostics now (the platform can report exactly what each module contributed) and as the foundation a future
 * hot-unload path would build on.
 */
public class ModuleHandles
{
	private final String _moduleId;
	private final List<String> _registrations = new ArrayList<>();

	public ModuleHandles(String moduleId)
	{
		_moduleId = moduleId;
	}

	public String getModuleId()
	{
		return _moduleId;
	}

	/**
	 * Records one registration made by this module, described for diagnostics.
	 * @param description a short human-readable description, for example "voiced command .hello"
	 */
	public void record(String description)
	{
		_registrations.add(description);
	}

	public List<String> getRegistrations()
	{
		return Collections.unmodifiableList(_registrations);
	}

	public int size()
	{
		return _registrations.size();
	}
}
