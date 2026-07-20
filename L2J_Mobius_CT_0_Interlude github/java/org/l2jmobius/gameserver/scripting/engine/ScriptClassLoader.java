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
package org.l2jmobius.gameserver.scripting.engine;

import java.util.concurrent.ConcurrentHashMap;

/**
 * A script class loader that accumulates all compiled classes.
 * @author Mobius
 */
public class ScriptClassLoader extends ClassLoader
{
	private final ConcurrentHashMap<String, ScriptClassData> _compiledClasses = new ConcurrentHashMap<>();
	
	public ScriptClassLoader(ClassLoader parent)
	{
		super(parent);
	}
	
	/**
	 * Adds compiled classes to this class loader.
	 * @param compiledClasses the compiled classes to add
	 */
	public void addCompiledClasses(Iterable<ScriptClassData> compiledClasses)
	{
		for (ScriptClassData compiledClass : compiledClasses)
		{
			_compiledClasses.put(compiledClass.getJavaName(), compiledClass);
		}
	}
	
	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException
	{
		final ScriptClassData compiledClass = _compiledClasses.get(name);
		if (compiledClass != null)
		{
			final byte[] classBytes = compiledClass.getJavaData();
			return defineClass(name, classBytes, 0, classBytes.length);
		}
		
		return super.findClass(name);
	}
}
