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

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardJavaFileManager;

/**
 * @author HorridoJoho, Mobius
 */
public class ScriptFileManager implements StandardJavaFileManager
{
	private final StandardJavaFileManager _wrapped;
	private final List<ScriptClassData> _classOutputs = new LinkedList<>();
	
	public ScriptFileManager(StandardJavaFileManager wrapped)
	{
		_wrapped = wrapped;
	}
	
	Iterable<ScriptClassData> getCompiledClasses()
	{
		return Collections.unmodifiableCollection(_classOutputs);
	}
	
	@Override
	public int isSupportedOption(String option)
	{
		return _wrapped.isSupportedOption(option);
	}
	
	@Override
	public ClassLoader getClassLoader(Location location)
	{
		return _wrapped.getClassLoader(location);
	}
	
	@Override
	public Iterable<JavaFileObject> list(Location location, String packageName, Set<Kind> kinds, boolean recurse) throws IOException
	{
		return _wrapped.list(location, packageName, kinds, recurse);
	}
	
	@Override
	public String inferBinaryName(Location location, JavaFileObject file)
	{
		return _wrapped.inferBinaryName(location, file);
	}
	
	@Override
	public boolean isSameFile(FileObject a, FileObject b)
	{
		return _wrapped.isSameFile(a, b);
	}
	
	@Override
	public boolean handleOption(String current, Iterator<String> remaining)
	{
		return _wrapped.handleOption(current, remaining);
	}
	
	@Override
	public boolean hasLocation(Location location)
	{
		return _wrapped.hasLocation(location);
	}
	
	@Override
	public JavaFileObject getJavaFileForInput(Location location, String className, Kind kind) throws IOException
	{
		return _wrapped.getJavaFileForInput(location, className, kind);
	}
	
	@Override
	public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind, FileObject sibling) throws IOException
	{
		if (kind != Kind.CLASS)
		{
			return _wrapped.getJavaFileForOutput(location, className, kind, sibling);
		}
		
		String javaName = className;
		if (javaName.contains("/"))
		{
			javaName = javaName.replace('/', '.');
		}
		
		ScriptClassData fileObject;
		if (sibling != null)
		{
			fileObject = new ScriptClassData(Paths.get(sibling.getName()), javaName, javaName.substring(javaName.lastIndexOf('.') + 1));
		}
		else
		{
			fileObject = new ScriptClassData(null, javaName, javaName.substring(javaName.lastIndexOf('.') + 1));
		}
		
		_classOutputs.add(fileObject);
		return fileObject;
	}
	
	@Override
	public FileObject getFileForInput(Location location, String packageName, String relativeName) throws IOException
	{
		return _wrapped.getFileForInput(location, packageName, relativeName);
	}
	
	@Override
	public FileObject getFileForOutput(Location location, String packageName, String relativeName, FileObject sibling) throws IOException
	{
		return _wrapped.getFileForOutput(location, packageName, relativeName, sibling);
	}
	
	@Override
	public void flush() throws IOException
	{
		_wrapped.flush();
	}
	
	@Override
	public void close() throws IOException
	{
		_wrapped.close();
	}
	
	@Override
	public Iterable<? extends JavaFileObject> getJavaFileObjectsFromFiles(Iterable<? extends File> files)
	{
		return _wrapped.getJavaFileObjectsFromFiles(files);
	}
	
	@Override
	public Iterable<? extends JavaFileObject> getJavaFileObjects(File... files)
	{
		return _wrapped.getJavaFileObjects(files);
	}
	
	@Override
	public Iterable<? extends JavaFileObject> getJavaFileObjectsFromStrings(Iterable<String> names)
	{
		return _wrapped.getJavaFileObjectsFromStrings(names);
	}
	
	@Override
	public Iterable<? extends JavaFileObject> getJavaFileObjects(String... names)
	{
		return _wrapped.getJavaFileObjects(names);
	}
	
	@Override
	public void setLocation(Location location, Iterable<? extends File> path) throws IOException
	{
		_wrapped.setLocation(location, path);
	}
	
	@Override
	public Iterable<? extends File> getLocation(Location location)
	{
		return _wrapped.getLocation(location);
	}
}
