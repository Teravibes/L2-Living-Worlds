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

/**
 * Flood protector settings
 * @author fordfrog, Mobius
 */
public class FloodProtectorSettings
{
	private final String _floodProtectorType;
	private int _protectionInterval;
	private boolean _logFlooding;
	private int _punishmentLimit;
	private String _punishmentType;
	private long _punishmentTime;
	
	public FloodProtectorSettings(String floodProtectorType)
	{
		_floodProtectorType = floodProtectorType;
	}
	
	public String getFloodProtectorType()
	{
		return _floodProtectorType;
	}
	
	public int getProtectionInterval()
	{
		return _protectionInterval;
	}
	
	public void setProtectionInterval(int protectionInterval)
	{
		_protectionInterval = protectionInterval;
	}
	
	public void setLogFlooding(boolean logFlooding)
	{
		_logFlooding = logFlooding;
	}
	
	public boolean isLogFlooding()
	{
		return _logFlooding;
	}
	
	public void setPunishmentLimit(int punishmentLimit)
	{
		_punishmentLimit = punishmentLimit;
	}
	
	public int getPunishmentLimit()
	{
		return _punishmentLimit;
	}
	
	public void setPunishmentType(String punishmentType)
	{
		_punishmentType = punishmentType;
	}
	
	public String getPunishmentType()
	{
		return _punishmentType;
	}
	
	public void setPunishmentTime(long punishmentTime)
	{
		_punishmentTime = punishmentTime;
	}
	
	public long getPunishmentTime()
	{
		return _punishmentTime;
	}
}
