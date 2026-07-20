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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.nio.file.Path;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.JavaFileObject;

/**
 * @author HorridoJoho, Mobius
 */
public class ScriptClassData implements JavaFileObject
{
	private final Path _sourcePath;
	private final String _javaName;
	private final String _javaSimpleName;
	private final ByteArrayOutputStream _out;
	
	public ScriptClassData(Path sourcePath, String javaName, String javaSimpleName)
	{
		_sourcePath = sourcePath;
		_javaName = javaName;
		_javaSimpleName = javaSimpleName;
		_out = new ByteArrayOutputStream();
	}
	
	public Path getSourcePath()
	{
		return _sourcePath;
	}
	
	public String getJavaName()
	{
		return _javaName;
	}
	
	public String getJavaSimpleName()
	{
		return _javaSimpleName;
	}
	
	public byte[] getJavaData()
	{
		return _out.toByteArray();
	}
	
	@Override
	public URI toUri()
	{
		return null;
	}
	
	@Override
	public String getName()
	{
		return null;
	}
	
	@Override
	public InputStream openInputStream()
	{
		return null;
	}
	
	@Override
	public OutputStream openOutputStream()
	{
		return _out;
	}
	
	@Override
	public Reader openReader(boolean ignoreEncodingErrors)
	{
		return null;
	}
	
	@Override
	public CharSequence getCharContent(boolean ignoreEncodingErrors)
	{
		return null;
	}
	
	@Override
	public Writer openWriter()
	{
		return null;
	}
	
	@Override
	public long getLastModified()
	{
		return 0;
	}
	
	@Override
	public boolean delete()
	{
		return false;
	}
	
	@Override
	public Kind getKind()
	{
		return Kind.CLASS;
	}
	
	@Override
	public boolean isNameCompatible(String simpleName, Kind kind)
	{
		return (kind == Kind.CLASS) && (_javaSimpleName.contentEquals(simpleName));
	}
	
	@Override
	public NestingKind getNestingKind()
	{
		return NestingKind.TOP_LEVEL;
	}
	
	@Override
	public Modifier getAccessLevel()
	{
		return null;
	}
}
