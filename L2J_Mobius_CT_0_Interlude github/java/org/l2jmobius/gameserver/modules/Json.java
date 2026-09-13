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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A small, dependency-free JSON reader for module manifests.
 * <p>
 * It parses the standard JSON grammar into plain Java values: a JSON object becomes a {@code Map<String, Object>}
 * (insertion-ordered), an array becomes a {@code List<Object>}, a string becomes a {@link String}, an integral number
 * becomes a {@link Long}, a fractional number becomes a {@link Double}, and {@code true}/{@code false}/{@code null}
 * become {@link Boolean} or {@code null}. It is deliberately strict and reports the offset of the first problem, since
 * a manifest is small and authored by hand. It is not a general high-performance parser and is scoped to the module
 * framework's needs.
 */
public final class Json
{
	private final String _text;
	private int _pos;

	private Json(String text)
	{
		_text = text;
	}

	/**
	 * Parses a JSON document.
	 * @param text the JSON text
	 * @return the parsed value (typically a {@code Map<String, Object>} for a manifest)
	 * @throws JsonException if the text is not well-formed JSON
	 */
	public static Object parse(String text) throws JsonException
	{
		if (text == null)
		{
			throw new JsonException("No JSON text.");
		}

		final Json json = new Json(text);
		json.skipWhitespace();
		final Object value = json.readValue();
		json.skipWhitespace();
		if (json._pos < json._text.length())
		{
			throw json.error("Trailing content after the JSON value");
		}
		return value;
	}

	private Object readValue() throws JsonException
	{
		if (_pos >= _text.length())
		{
			throw error("Unexpected end of input");
		}

		final char c = _text.charAt(_pos);
		switch (c)
		{
			case '{':
				return readObject();
			case '[':
				return readArray();
			case '"':
				return readString();
			case 't':
			case 'f':
				return readBoolean();
			case 'n':
				return readNull();
			default:
				if ((c == '-') || ((c >= '0') && (c <= '9')))
				{
					return readNumber();
				}
				throw error("Unexpected character '" + c + "'");
		}
	}

	private Map<String, Object> readObject() throws JsonException
	{
		final Map<String, Object> object = new LinkedHashMap<>();
		_pos++; // consume '{'
		skipWhitespace();
		if (peek() == '}')
		{
			_pos++;
			return object;
		}

		while (true)
		{
			skipWhitespace();
			if (peek() != '"')
			{
				throw error("Expected a string key");
			}

			final String key = readString();
			skipWhitespace();
			if (peek() != ':')
			{
				throw error("Expected ':' after key '" + key + "'");
			}
			_pos++; // consume ':'
			skipWhitespace();
			object.put(key, readValue());
			skipWhitespace();

			final char next = peek();
			if (next == ',')
			{
				_pos++;
				continue;
			}
			if (next == '}')
			{
				_pos++;
				return object;
			}
			throw error("Expected ',' or '}' in object");
		}
	}

	private List<Object> readArray() throws JsonException
	{
		final List<Object> array = new ArrayList<>();
		_pos++; // consume '['
		skipWhitespace();
		if (peek() == ']')
		{
			_pos++;
			return array;
		}

		while (true)
		{
			skipWhitespace();
			array.add(readValue());
			skipWhitespace();

			final char next = peek();
			if (next == ',')
			{
				_pos++;
				continue;
			}
			if (next == ']')
			{
				_pos++;
				return array;
			}
			throw error("Expected ',' or ']' in array");
		}
	}

	private String readString() throws JsonException
	{
		final StringBuilder sb = new StringBuilder();
		_pos++; // consume opening quote
		while (_pos < _text.length())
		{
			final char c = _text.charAt(_pos++);
			if (c == '"')
			{
				return sb.toString();
			}
			if (c == '\\')
			{
				if (_pos >= _text.length())
				{
					throw error("Unterminated escape sequence");
				}
				final char escape = _text.charAt(_pos++);
				switch (escape)
				{
					case '"':
						sb.append('"');
						break;
					case '\\':
						sb.append('\\');
						break;
					case '/':
						sb.append('/');
						break;
					case 'b':
						sb.append('\b');
						break;
					case 'f':
						sb.append('\f');
						break;
					case 'n':
						sb.append('\n');
						break;
					case 'r':
						sb.append('\r');
						break;
					case 't':
						sb.append('\t');
						break;
					case 'u':
						if ((_pos + 4) > _text.length())
						{
							throw error("Incomplete unicode escape");
						}
						try
						{
							sb.append((char) Integer.parseInt(_text.substring(_pos, _pos + 4), 16));
						}
						catch (NumberFormatException e)
						{
							throw error("Invalid unicode escape");
						}
						_pos += 4;
						break;
					default:
						throw error("Invalid escape character '" + escape + "'");
				}
			}
			else if (c < 0x20)
			{
				throw error("Unescaped control character in string");
			}
			else
			{
				sb.append(c);
			}
		}
		throw error("Unterminated string");
	}

	private Object readNumber() throws JsonException
	{
		final int start = _pos;
		boolean fractional = false;
		if (peek() == '-')
		{
			_pos++;
		}
		while (_pos < _text.length())
		{
			final char c = _text.charAt(_pos);
			if ((c >= '0') && (c <= '9'))
			{
				_pos++;
			}
			else if ((c == '.') || (c == 'e') || (c == 'E') || (c == '+') || (c == '-'))
			{
				fractional = true;
				_pos++;
			}
			else
			{
				break;
			}
		}

		final String number = _text.substring(start, _pos);
		try
		{
			if (fractional)
			{
				return Double.valueOf(number);
			}
			return Long.valueOf(number);
		}
		catch (NumberFormatException e)
		{
			throw error("Invalid number '" + number + "'");
		}
	}

	private Boolean readBoolean() throws JsonException
	{
		if (_text.startsWith("true", _pos))
		{
			_pos += 4;
			return Boolean.TRUE;
		}
		if (_text.startsWith("false", _pos))
		{
			_pos += 5;
			return Boolean.FALSE;
		}
		throw error("Invalid literal, expected 'true' or 'false'");
	}

	private Object readNull() throws JsonException
	{
		if (_text.startsWith("null", _pos))
		{
			_pos += 4;
			return null;
		}
		throw error("Invalid literal, expected 'null'");
	}

	private char peek() throws JsonException
	{
		if (_pos >= _text.length())
		{
			throw error("Unexpected end of input");
		}
		return _text.charAt(_pos);
	}

	private void skipWhitespace()
	{
		while (_pos < _text.length())
		{
			final char c = _text.charAt(_pos);
			if ((c == ' ') || (c == '\t') || (c == '\n') || (c == '\r'))
			{
				_pos++;
			}
			else
			{
				break;
			}
		}
	}

	private JsonException error(String message)
	{
		return new JsonException(message + " at offset " + _pos + ".");
	}

	/**
	 * Thrown when JSON text cannot be parsed.
	 */
	public static final class JsonException extends Exception
	{
		private static final long serialVersionUID = 1L;

		JsonException(String message)
		{
			super(message);
		}
	}
}
