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

import java.util.List;

/**
 * A module's {@code database} manifest block: the module-relative install script the platform runs when the module is
 * enabled, the opt-in cleanup script the platform never runs on its own, and the tables the module owns. An absent block
 * is represented by empty strings and an empty list, so a module with no storage needs no special case.
 *
 * @param install the module-relative path to the install script, or an empty string
 * @param remove the module-relative path to the opt-in cleanup script, or an empty string
 * @param tables the names of the tables the module owns
 */
public record ModuleDatabaseSpec(String install, String remove, List<String> tables)
{
	public boolean hasInstall()
	{
		return !install.isEmpty();
	}

	public boolean hasRemove()
	{
		return !remove.isEmpty();
	}
}
