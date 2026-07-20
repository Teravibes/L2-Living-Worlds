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
package org.l2jmobius.gameserver.util;

import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Locale;

/**
 * The Locator is a utility class which is used to find certain items in the environment.
 * @since Ant 1.6
 */
public class Locator
{
	/**
	 * Not instantiable
	 */
	private Locator()
	{
	}
	
	/**
	 * Find the directory or jar file the class has been loaded from.
	 * @param c the class whose location is required.
	 * @return the file or jar with the class or null if we cannot determine the location.
	 * @since Ant 1.6
	 */
	public static File getClassSource(Class<?> c)
	{
		final String classResource = c.getName().replace('.', '/') + ".class";
		return getResourceSource(c.getClassLoader(), classResource);
	}
	
	/**
	 * Find the directory or jar a given resource has been loaded from.
	 * @param classLoader the classloader to be consulted for the source.
	 * @param resource the resource whose location is required.
	 * @return the file with the resource source or null if we cannot determine the location.
	 * @since Ant 1.6
	 */
	public static File getResourceSource(ClassLoader classLoader, String resource)
	{
		ClassLoader c = classLoader;
		if (c == null)
		{
			c = Locator.class.getClassLoader();
		}
		
		URL url = null;
		if (c == null)
		{
			url = ClassLoader.getSystemResource(resource);
		}
		else
		{
			url = c.getResource(resource);
		}
		
		if (url != null)
		{
			final String u = url.toString();
			if (u.startsWith("jar:file:"))
			{
				final int pling = u.indexOf('!');
				final String jarName = u.substring(4, pling);
				return new File(fromURI(jarName));
			}
			else if (u.startsWith("file:"))
			{
				final int tail = u.indexOf(resource);
				final String dirName = u.substring(0, tail);
				return new File(fromURI(dirName));
			}
		}
		
		return null;
	}
	
	/**
	 * Constructs a file path from a <code>file:</code> URI.
	 * <p>
	 * Will be an absolute path if the given URI is absolute.
	 * </p>
	 * <p>
	 * Swallows '%' that are not followed by two characters, doesn't deal with non-ASCII characters.
	 * </p>
	 * @param uriValue the URI designating a file in the local filesystem.
	 * @return the local file system path for the file.
	 * @since Ant 1.6
	 */
	public static String fromURI(String uriValue)
	{
		String uri = uriValue;
		URL url = null;
		try
		{
			// Java 19
			// url = new URL(uri);
			// Java 20
			url = URI.create(uri).toURL();
		}
		catch (MalformedURLException emYouEarlEx)
		{
			// Ignore malformed exception
		}
		
		if ((url == null) || !("file".equals(url.getProtocol())))
		{
			throw new IllegalArgumentException("Can only handle valid file: URIs");
		}
		
		final StringBuilder buf = new StringBuilder(url.getHost());
		if (buf.length() > 0)
		{
			buf.insert(0, File.separatorChar).insert(0, File.separatorChar);
		}
		
		final String file = url.getFile();
		final int queryPos = file.indexOf('?');
		buf.append((queryPos < 0) ? file : file.substring(0, queryPos));
		uri = buf.toString().replace('/', File.separatorChar);
		if ((File.pathSeparatorChar == ';') && uri.startsWith("\\") && (uri.length() > 2) && Character.isLetter(uri.charAt(1)) && (uri.lastIndexOf(':') > -1))
		{
			uri = uri.substring(1);
		}
		
		return decodeUri(uri);
	}
	
	/**
	 * Decodes an Uri with % characters.
	 * @param uri String with the uri possibly containing % characters.
	 * @return The decoded Uri
	 */
	private static String decodeUri(String uri)
	{
		if (uri.indexOf('%') == -1)
		{
			return uri;
		}
		
		final StringBuilder sb = new StringBuilder();
		final CharacterIterator iter = new StringCharacterIterator(uri);
		for (char c = iter.first(); c != CharacterIterator.DONE; c = iter.next())
		{
			if (c == '%')
			{
				final char c1 = iter.next();
				if (c1 != CharacterIterator.DONE)
				{
					final int i1 = Character.digit(c1, 16);
					final char c2 = iter.next();
					if (c2 != CharacterIterator.DONE)
					{
						final int i2 = Character.digit(c2, 16);
						sb.append((char) ((i1 << 4) + i2));
					}
				}
			}
			else
			{
				sb.append(c);
			}
		}
		
		return sb.toString();
	}
	
	/**
	 * Get the File necessary to load the Sun compiler tools. If the classes are available to this class, then no additional URL is required and null is returned. This may be because the classes are explicitly in the class path or provided by the JVM directly.
	 * @return the tools jar as a File if required, null otherwise.
	 */
	public static File getToolsJar()
	{
		// firstly check if the tools jar is already in the classpath
		boolean toolsJarAvailable = false;
		try
		{
			// just check whether this throws an exception
			Class.forName("com.sun.tools.javac.Main");
			toolsJarAvailable = true;
		}
		catch (Exception e)
		{
			try
			{
				Class.forName("sun.tools.javac.Main");
				toolsJarAvailable = true;
			}
			catch (Exception e2)
			{
				// ignore
			}
		}
		
		if (toolsJarAvailable)
		{
			return null;
		}
		
		// couldn't find compiler - try to find tools.jar
		// based on java.home setting
		String javaHome = System.getProperty("java.home");
		if (javaHome.toLowerCase(Locale.US).endsWith("jre"))
		{
			javaHome = javaHome.substring(0, javaHome.length() - 4);
		}
		
		final File toolsJar = new File(javaHome + "/lib/tools.jar");
		if (!toolsJar.exists())
		{
			System.out.println("Unable to locate tools.jar. " + "Expected to find it in " + toolsJar.getPath());
			return null;
		}
		
		return toolsJar;
	}
	
	/**
	 * Get an array of URLs representing all of the jar files in the given location. If the location is a file, it is returned as the only element of the array. If the location is a directory, it is scanned for jar files.
	 * @param location the location to scan for Jars.
	 * @return an array of URLs for all jars in the given location.
	 * @exception MalformedURLException if the URLs for the jars cannot be formed.
	 */
	public static URL[] getLocationURLs(File location) throws MalformedURLException
	{
		return getLocationURLs(location, new String[]
		{
			".jar"
		});
	}
	
	/**
	 * Get an array of URLs representing all of the files of a given set of extensions in the given location. If the location is a file, it is returned as the only element of the array. If the location is a directory, it is scanned for matching files.
	 * @param location the location to scan for files.
	 * @param extensions an array of extension that are to match in the directory search.
	 * @return an array of URLs of matching files.
	 * @exception MalformedURLException if the URLs for the files cannot be formed.
	 */
	public static URL[] getLocationURLs(File location, String[] extensions) throws MalformedURLException
	{
		URL[] urls = new URL[0];
		
		if (!location.exists())
		{
			return urls;
		}
		
		if (!location.isDirectory())
		{
			urls = new URL[1];
			final String path = location.getPath();
			for (String extension : extensions)
			{
				if (path.toLowerCase().endsWith(extension))
				{
					urls[0] = location.toURI().toURL();
					break;
				}
			}
			
			return urls;
		}
		
		final File[] matches = location.listFiles((FilenameFilter) (_, name) ->
		{
			for (String extension : extensions)
			{
				if (name.toLowerCase().endsWith(extension))
				{
					return true;
				}
			}
			
			return false;
		});
		urls = new URL[matches.length];
		for (int i = 0; i < matches.length; ++i)
		{
			urls[i] = matches[i].toURI().toURL();
		}
		
		return urls;
	}
}
