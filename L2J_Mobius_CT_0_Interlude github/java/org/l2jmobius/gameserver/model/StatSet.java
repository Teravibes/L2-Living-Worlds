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
package org.l2jmobius.gameserver.model;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.l2jmobius.commons.time.TimeUtil;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.model.actor.holders.npc.MinionHolder;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;

/**
 * This class is meant to hold a set of (key,value) pairs.<br>
 * They are stored as object but can be retrieved in any type wanted. As long as cast is available.<br>
 * @author mkizub, Mobius
 */
public class StatSet
{
	private static final Logger LOGGER = Logger.getLogger(StatSet.class.getName());
	
	/** Static empty immutable map, used to avoid multiple null checks over the source. */
	public static final StatSet EMPTY_STATSET = new StatSet(Collections.emptyMap());
	
	private final Map<String, Object> _set;
	
	public StatSet()
	{
		this(HashMap::new);
	}
	
	public StatSet(Supplier<Map<String, Object>> mapFactory)
	{
		this(mapFactory.get());
	}
	
	public StatSet(Map<String, Object> map)
	{
		_set = map;
	}
	
	/**
	 * Returns the set of values
	 * @return HashMap
	 */
	public Map<String, Object> getSet()
	{
		return _set;
	}
	
	/**
	 * Add a set of couple values in the current set
	 * @param newSet : StatSet pointing out the list of couples to add in the current set
	 */
	public void merge(StatSet newSet)
	{
		_set.putAll(newSet.getSet());
	}
	
	/**
	 * Verifies if the stat set is empty.
	 * @return {@code true} if the stat set is empty, {@code false} otherwise
	 */
	public boolean isEmpty()
	{
		return _set.isEmpty();
	}
	
	/**
	 * Return the boolean value associated with key.
	 * @param key : String designating the key in the set
	 * @return boolean : value associated to the key
	 * @throws IllegalArgumentException : If value is not set or value is not boolean
	 */
	public boolean getBoolean(String key)
	{
		final Object val = _set.get(key);
		if (val == null)
		{
			throw new IllegalArgumentException("Boolean value required, but not specified");
		}
		
		if (val instanceof Boolean)
		{
			return ((Boolean) val).booleanValue();
		}
		
		try
		{
			return Boolean.parseBoolean((String) val);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Boolean value required, but found: " + val);
		}
	}
	
	/**
	 * Return the boolean value associated with key.<br>
	 * If no value is associated with key, or type of value is wrong, returns defaultValue.
	 * @param key : String designating the key in the entry set
	 * @param defaultValue
	 * @return boolean : value associated to the key
	 */
	public boolean getBoolean(String key, boolean defaultValue)
	{
		final Object val = _set.get(key);
		if (val == null)
		{
			return defaultValue;
		}
		
		if (val instanceof Boolean)
		{
			return ((Boolean) val).booleanValue();
		}
		
		try
		{
			return Boolean.parseBoolean((String) val);
		}
		catch (Exception e)
		{
			return defaultValue;
		}
	}
	
	public byte getByte(String key)
	{
		final Object val = _set.get(key);
		if (val == null)
		{
			throw new IllegalArgumentException("Byte value required, but not specified");
		}
		
		if (val instanceof Number)
		{
			return ((Number) val).byteValue();
		}
		
		try
		{
			return Byte.parseByte((String) val);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Byte value required, but found: " + val);
		}
	}
	
	public byte getByte(String key, byte defaultValue)
	{
		final Object val = _set.get(key);
		if (val == null)
		{
			return defaultValue;
		}
		
		if (val instanceof Number)
		{
			return ((Number) val).byteValue();
		}
		
		try
		{
			return Byte.parseByte((String) val);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Byte value required, but found: " + val);
		}
	}
	
	public short increaseByte(String key, byte increaseWith)
	{
		final byte newValue = (byte) (getByte(key) + increaseWith);
		set(key, newValue);
		return newValue;
	}
	
	public short increaseByte(String key, byte defaultValue, byte increaseWith)
	{
		final byte newValue = (byte) (getByte(key, defaultValue) + increaseWith);
		set(key, newValue);
		return newValue;
	}
	
	public byte[] getByteArray(String key, String splitOn)
	{
		final Object val = _set.get(key);
		if (val == null)
		{
			throw new IllegalArgumentException("Byte value required, but not specified");
		}
		
		if (val instanceof Number)
		{
			return new byte[]
			{
				((Number) val).byteValue()
			};
		}
		
		int c = 0;
		final String[] vals = ((String) val).split(splitOn);
		final byte[] result = new byte[vals.length];
		for (String v : vals)
		{
			try
			{
				result[c++] = Byte.parseByte(v);
			}
			catch (Exception e)
			{
				throw new IllegalArgumentException("Byte value required, but found: " + val);
			}
		}
		
		return result;
	}
	
	public List<Byte> getByteList(String key, String splitOn)
	{
		final List<Byte> result = new LinkedList<>();
		for (Byte i : getByteArray(key, splitOn))
		{
			result.add(i);
		}
		
		return result;
	}
	
	public short getShort(String key)
	{
		final Object val = _set.get(key);
		if (val == null)
		{
			throw new IllegalArgumentException("Short value required, but not specified");
		}
		
		if (val instanceof Number)
		{
			return ((Number) val).shortValue();
		}
		
		try
		{
			return Short.parseShort((String) val);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Short value required, but found: " + val);
		}
	}
	
	public short getShort(String key, short defaultValue)
	{
		final Object val = _set.get(key);
		if (val == null)
		{
			return defaultValue;
		}
		
		if (val instanceof Number)
		{
			return ((Number) val).shortValue();
		}
		
		try
		{
			return Short.parseShort((String) val);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Short value required, but found: " + val);
		}
	}
	
	public short increaseShort(String key, short increaseWith)
	{
		final short newValue = (short) (getShort(key) + increaseWith);
		set(key, newValue);
		return newValue;
	}
	
	public short increaseShort(String key, short defaultValue, short increaseWith)
	{
		final short newValue = (short) (getShort(key, defaultValue) + increaseWith);
		set(key, newValue);
		return newValue;
	}
	
	public int getInt(String key)
	{
		final Object val = _set.get(key);
		if (val == null)
		{
			throw new IllegalArgumentException("Integer value required, but not specified: " + key + "!");
		}
		
		if (val instanceof Number)
		{
			return ((Number) val).intValue();
		}
		
		try
		{
			return Integer.parseInt((String) val);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Integer value required, but found: " + val + "!");
		}
	}
	
	public int getInt(String key, int defaultValue)
	{
		final Object val = _set.get(key);
		if (val == null)
		{
			return defaultValue;
		}
		
		if (val instanceof Number)
		{
			return ((Number) val).intValue();
		}
		
		try
		{
			return Integer.parseInt((String) val);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Integer value required, but found: " + val);
		}
	}
	
	public int increaseInt(String key, int increaseWith)
	{
		final int newValue = getInt(key) + increaseWith;
		set(key, newValue);
		return newValue;
	}
	
	public int increaseInt(String key, int defaultValue, int increaseWith)
	{
		final int newValue = getInt(key, defaultValue) + increaseWith;
		set(key, newValue);
		return newValue;
	}
	
	public int[] getIntArray(String key, String splitOn)
	{
		final Object val = _set.get(key);
		if (val == null)
		{
			throw new IllegalArgumentException("Integer value required, but not specified");
		}
		
		if (val instanceof Number)
		{
			return new int[]
			{
				((Number) val).intValue()
			};
		}
		
		int c = 0;
		final String[] vals = ((String) val).split(splitOn);
		final int[] result = new int[vals.length];
		for (String v : vals)
		{
			try
			{
				result[c++] = Integer.parseInt(v);
			}
			catch (Exception e)
			{
				throw new IllegalArgumentException("Integer value required, but found: " + val);
			}
		}
		
		return result;
	}
	
	public List<Integer> getIntegerList(String key)
	{
		final String val = getString(key, null);
		final List<Integer> result;
		if (val != null)
		{
			final String[] splitVal = val.split(",");
			result = new ArrayList<>(splitVal.length + 1);
			for (String split : splitVal)
			{
				result.add(Integer.parseInt(split));
			}
		}
		else
		{
			result = new ArrayList<>(1);
		}
		
		return result;
	}
	
	public void setIntegerList(String key, List<Integer> list)
	{
		if (key == null)
		{
			return;
		}
		
		if ((list == null) || list.isEmpty())
		{
			remove(key);
			return;
		}
		
		final StringBuilder sb = new StringBuilder();
		for (int element : list)
		{
			sb.append(element);
			sb.append(",");
		}
		
		sb.deleteCharAt(sb.length() - 1); // Prettify value.
		
		set(key, sb.toString());
	}
	
	public Map<Integer, Integer> getIntegerMap(String key)
	{
		final String val = getString(key, null);
		final Map<Integer, Integer> result;
		if (val != null)
		{
			final String[] splitVal = val.split(",");
			result = new HashMap<>(splitVal.length + 1);
			for (String split : splitVal)
			{
				final String[] entry = split.split("-");
				result.put(Integer.parseInt(entry[0]), Integer.parseInt(entry[1]));
			}
		}
		else
		{
			result = new HashMap<>(1);
		}
		
		return result;
	}
	
	public void setIntegerMap(String key, Map<Integer, Integer> map)
	{
		if (key == null)
		{
			return;
		}
		
		if ((map == null) || map.isEmpty())
		{
			remove(key);
			return;
		}
		
		final StringBuilder sb = new StringBuilder();
		for (Entry<Integer, Integer> entry : map.entrySet())
		{
			sb.append(entry.getKey());
			sb.append("-");
			sb.append(entry.getValue());
			sb.append(",");
		}
		
		sb.deleteCharAt(sb.length() - 1); // Prettify value.
		
		set(key, sb.toString());
	}
	
	public long getLong(String key)
	{
		final Object val = _set.get(key);
		if (val == null)
		{
			throw new IllegalArgumentException("Long value required, but not specified");
		}
		
		if (val instanceof Number)
		{
			return ((Number) val).longValue();
		}
		
		try
		{
			return Long.parseLong((String) val);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Long value required, but found: " + val);
		}
	}
	
	public long getLong(String key, long defaultValue)
	{
		final Object val = _set.get(key);
		if (val == null)
		{
			return defaultValue;
		}
		
		if (val instanceof Number)
		{
			return ((Number) val).longValue();
		}
		
		try
		{
			return Long.parseLong((String) val);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Long value required, but found: " + val);
		}
	}
	
	public long increaseLong(String key, long increaseWith)
	{
		final long newValue = getLong(key) + increaseWith;
		set(key, newValue);
		return newValue;
	}
	
	public long increaseLong(String key, long defaultValue, long increaseWith)
	{
		final long newValue = getLong(key, defaultValue) + increaseWith;
		set(key, newValue);
		return newValue;
	}
	
	public float getFloat(String key)
	{
		final Object val = _set.get(key);
		if (val == null)
		{
			throw new IllegalArgumentException("Float value required, but not specified");
		}
		
		if (val instanceof Number)
		{
			return ((Number) val).floatValue();
		}
		
		try
		{
			return Float.parseFloat((String) val);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Float value required, but found: " + val);
		}
	}
	
	public float getFloat(String key, float defaultValue)
	{
		final Object val = _set.get(key);
		if (val == null)
		{
			return defaultValue;
		}
		
		if (val instanceof Number)
		{
			return ((Number) val).floatValue();
		}
		
		try
		{
			return Float.parseFloat((String) val);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Float value required, but found: " + val);
		}
	}
	
	public float increaseFloat(String key, float increaseWith)
	{
		final float newValue = getFloat(key) + increaseWith;
		set(key, newValue);
		return newValue;
	}
	
	public float increaseFloat(String key, float defaultValue, float increaseWith)
	{
		final float newValue = getFloat(key, defaultValue) + increaseWith;
		set(key, newValue);
		return newValue;
	}
	
	public double getDouble(String key)
	{
		final Object val = _set.get(key);
		if (val == null)
		{
			throw new IllegalArgumentException("Double value required, but not specified");
		}
		
		if (val instanceof Number)
		{
			return ((Number) val).doubleValue();
		}
		
		try
		{
			return Double.parseDouble((String) val);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Double value required, but found: " + val);
		}
	}
	
	public double getDouble(String key, double defaultValue)
	{
		final Object val = _set.get(key);
		if (val == null)
		{
			return defaultValue;
		}
		
		if (val instanceof Number)
		{
			return ((Number) val).doubleValue();
		}
		
		try
		{
			return Double.parseDouble((String) val);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Double value required, but found: " + val);
		}
	}
	
	public double increaseDouble(String key, double increaseWith)
	{
		final double newValue = getDouble(key) + increaseWith;
		set(key, newValue);
		return newValue;
	}
	
	public double increaseDouble(String key, double defaultValue, double increaseWith)
	{
		final double newValue = getDouble(key, defaultValue) + increaseWith;
		set(key, newValue);
		return newValue;
	}
	
	public String getString(String key)
	{
		final Object val = _set.get(key);
		if (val == null)
		{
			throw new IllegalArgumentException("String value required, but not specified");
		}
		
		return String.valueOf(val);
	}
	
	public String getString(String key, String defaultValue)
	{
		final Object val = _set.get(key);
		if (val == null)
		{
			return defaultValue;
		}
		
		return String.valueOf(val);
	}
	
	public Duration getDuration(String key)
	{
		final Object val = _set.get(key);
		if (val == null)
		{
			throw new IllegalArgumentException("String value required, but not specified");
		}
		
		return TimeUtil.parseDuration(String.valueOf(val));
	}
	
	public Duration getDuration(String key, Duration defaultValue)
	{
		final Object val = _set.get(key);
		if (val == null)
		{
			return defaultValue;
		}
		
		return TimeUtil.parseDuration(String.valueOf(val));
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Enum<T>> T getEnum(String key, Class<T> enumClass)
	{
		final Object val = _set.get(key);
		if (val == null)
		{
			throw new IllegalArgumentException("Enum value of type " + enumClass.getName() + " required, but not specified");
		}
		
		if (enumClass.isInstance(val))
		{
			return (T) val;
		}
		
		try
		{
			return Enum.valueOf(enumClass, String.valueOf(val));
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Enum value of type " + enumClass.getName() + " required, but found: " + val);
		}
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Enum<T>> T getEnum(String key, Class<T> enumClass, T defaultValue)
	{
		final Object val = _set.get(key);
		if (val == null)
		{
			return defaultValue;
		}
		
		if (enumClass.isInstance(val))
		{
			return (T) val;
		}
		
		try
		{
			return Enum.valueOf(enumClass, String.valueOf(val));
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Enum value of type " + enumClass.getName() + " required, but found: " + val);
		}
	}
	
	@SuppressWarnings("unchecked")
	public <A> A getObject(String name, Class<A> type)
	{
		final Object obj = _set.get(name);
		if ((obj == null) || !type.isAssignableFrom(obj.getClass()))
		{
			return null;
		}
		
		return (A) obj;
	}
	
	@SuppressWarnings("unchecked")
	public <A> A getObject(String name, Class<A> type, A defaultValue)
	{
		final Object obj = _set.get(name);
		if ((obj == null) || !type.isAssignableFrom(obj.getClass()))
		{
			return defaultValue;
		}
		
		return (A) obj;
	}
	
	public SkillHolder getSkillHolder(String key)
	{
		final Object obj = _set.get(key);
		if (!(obj instanceof SkillHolder))
		{
			return null;
		}
		
		return (SkillHolder) obj;
	}
	
	public Location getLocation(String key)
	{
		final Object obj = _set.get(key);
		if (!(obj instanceof Location))
		{
			return null;
		}
		
		return (Location) obj;
	}
	
	@SuppressWarnings("unchecked")
	public List<MinionHolder> getMinionList(String key)
	{
		final Object obj = _set.get(key);
		if (!(obj instanceof List<?>))
		{
			return Collections.emptyList();
		}
		
		return (List<MinionHolder>) obj;
	}
	
	@SuppressWarnings("unchecked")
	public <T> List<T> getList(String key, Class<T> clazz)
	{
		final Object obj = _set.get(key);
		if (!(obj instanceof List<?>))
		{
			return null;
		}
		
		final List<Object> originalList = (List<Object>) obj;
		if (!originalList.isEmpty() && !originalList.stream().allMatch(clazz::isInstance))
		{
			if (clazz.getSuperclass() == Enum.class)
			{
				throw new IllegalAccessError("Please use getEnumList if you want to get list of Enums!");
			}
			
			// Attempt to convert the list
			final List<T> convertedList = convertList(originalList, clazz);
			if (convertedList == null)
			{
				LOGGER.log(Level.WARNING, "getList(\"" + key + "\", " + clazz.getSimpleName() + ") requested with wrong generic type: " + obj.getClass().getGenericInterfaces()[0] + "!", new ClassNotFoundException());
				return null;
			}
			
			// Overwrite the existing list with proper generic type
			_set.put(key, convertedList);
			return convertedList;
		}
		
		return (List<T>) obj;
	}
	
	public <T> List<T> getList(String key, Class<T> clazz, List<T> defaultValue)
	{
		final List<T> list = getList(key, clazz);
		return list == null ? defaultValue : list;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Enum<T>> List<T> getEnumList(String key, Class<T> clazz)
	{
		final Object obj = _set.get(key);
		if (!(obj instanceof List<?>))
		{
			return null;
		}
		
		final List<Object> originalList = (List<Object>) obj;
		if (!originalList.isEmpty() && (obj.getClass().getGenericInterfaces()[0] != clazz) && originalList.stream().allMatch(name -> StringUtil.isEnum(name.toString(), clazz)))
		{
			final List<T> convertedList = originalList.stream().map(Object::toString).map(name -> Enum.valueOf(clazz, name)).map(clazz::cast).collect(Collectors.toList());
			
			// Overwrite the existing list with proper generic type
			_set.put(key, convertedList);
			return convertedList;
		}
		
		return (List<T>) obj;
	}
	
	/**
	 * @param <T>
	 * @param originalList
	 * @param clazz
	 * @return
	 */
	private <T> List<T> convertList(List<Object> originalList, Class<T> clazz)
	{
		if (clazz == Integer.class)
		{
			if (originalList.stream().map(Object::toString).allMatch(StringUtil::isInteger))
			{
				return originalList.stream().map(Object::toString).map(Integer::valueOf).map(clazz::cast).collect(Collectors.toList());
			}
		}
		else if (clazz == Float.class)
		{
			if (originalList.stream().map(Object::toString).allMatch(StringUtil::isFloat))
			{
				return originalList.stream().map(Object::toString).map(Float::valueOf).map(clazz::cast).collect(Collectors.toList());
			}
		}
		else if (clazz == Double.class)
		{
			if (originalList.stream().map(Object::toString).allMatch(StringUtil::isDouble))
			{
				return originalList.stream().map(Object::toString).map(Double::valueOf).map(clazz::cast).collect(Collectors.toList());
			}
		}
		
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public <K, V> Map<K, V> getMap(String key, Class<K> keyClass, Class<V> valueClass)
	{
		final Object obj = _set.get(key);
		if (!(obj instanceof Map<?, ?>))
		{
			return null;
		}
		
		final Map<?, ?> originalList = (Map<?, ?>) obj;
		if (!originalList.isEmpty() && ((!originalList.keySet().stream().allMatch(keyClass::isInstance)) || (!originalList.values().stream().allMatch(valueClass::isInstance))))
		{
			LOGGER.log(Level.WARNING, "getMap(\"" + key + "\", " + keyClass.getSimpleName() + ", " + valueClass.getSimpleName() + ") requested with wrong generic type: " + obj.getClass().getGenericInterfaces()[0] + "!", new ClassNotFoundException());
		}
		
		return (Map<K, V>) obj;
	}
	
	public void set(String name, Object value)
	{
		if (value == null)
		{
			return;
		}
		
		_set.put(name, value);
	}
	
	public void set(String name, boolean value)
	{
		_set.put(name, value ? Boolean.TRUE : Boolean.FALSE);
	}
	
	public void set(String key, byte value)
	{
		_set.put(key, value);
	}
	
	public void set(String key, short value)
	{
		_set.put(key, value);
	}
	
	public void set(String key, int value)
	{
		_set.put(key, value);
	}
	
	public void set(String key, long value)
	{
		_set.put(key, value);
	}
	
	public void set(String key, float value)
	{
		_set.put(key, value);
	}
	
	public void set(String key, double value)
	{
		_set.put(key, value);
	}
	
	public void set(String key, String value)
	{
		if (value == null)
		{
			return;
		}
		
		_set.put(key, value);
	}
	
	public void set(String key, Enum<?> value)
	{
		if (value == null)
		{
			return;
		}
		
		_set.put(key, value);
	}
	
	public void remove(String key)
	{
		_set.remove(key);
	}
	
	public boolean contains(String name)
	{
		return _set.containsKey(name);
	}
	
	@Override
	public String toString()
	{
		return "StatSet{_set=" + _set + '}';
	}
}
