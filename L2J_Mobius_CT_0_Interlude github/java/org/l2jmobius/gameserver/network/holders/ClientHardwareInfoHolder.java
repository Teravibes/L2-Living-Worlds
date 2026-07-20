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
package org.l2jmobius.gameserver.network.holders;

import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.variables.AccountVariables;

/**
 * @author Mobius
 */
public class ClientHardwareInfoHolder
{
	private final String _macAddress;
	private final int _windowsPlatformId;
	private final int _windowsMajorVersion;
	private final int _windowsMinorVersion;
	private final int _windowsBuildNumber;
	private final int _directxVersion;
	private final int _directxRevision;
	private final String _cpuName;
	private final int _cpuSpeed;
	private final int _cpuCoreCount;
	private final int _vgaCount;
	private final int _vgaPcxSpeed;
	private final int _physMemorySlot1;
	private final int _physMemorySlot2;
	private final int _physMemorySlot3;
	private final int _videoMemory;
	private final int _vgaVersion;
	private final String _vgaName;
	private final String _vgaDriverVersion;
	
	public ClientHardwareInfoHolder(String macAddress, int windowsPlatformId, int windowsMajorVersion, int windowsMinorVersion, int windowsBuildNumber, int directxVersion, int directxRevision, String cpuName, int cpuSpeed, int cpuCoreCount, int vgaCount, int vgaPcxSpeed, int physMemorySlot1, int physMemorySlot2, int physMemorySlot3, int videoMemory, int vgaVersion, String vgaName, String vgaDriverVersion)
	{
		_macAddress = macAddress;
		_windowsPlatformId = windowsPlatformId;
		_windowsMajorVersion = windowsMajorVersion;
		_windowsMinorVersion = windowsMinorVersion;
		_windowsBuildNumber = windowsBuildNumber;
		_directxVersion = directxVersion;
		_directxRevision = directxRevision;
		_cpuName = cpuName;
		_cpuSpeed = cpuSpeed;
		_cpuCoreCount = cpuCoreCount;
		_vgaCount = vgaCount;
		_vgaPcxSpeed = vgaPcxSpeed;
		_physMemorySlot1 = physMemorySlot1;
		_physMemorySlot2 = physMemorySlot2;
		_physMemorySlot3 = physMemorySlot3;
		_videoMemory = videoMemory;
		_vgaVersion = vgaVersion;
		_vgaName = vgaName;
		_vgaDriverVersion = vgaDriverVersion;
	}
	
	public ClientHardwareInfoHolder(String info)
	{
		final String[] split = info.split(AccountVariables.HWIDSLIT_VAR);
		_macAddress = split[0];
		_windowsPlatformId = Integer.valueOf(split[1]);
		_windowsMajorVersion = Integer.valueOf(split[2]);
		_windowsMinorVersion = Integer.valueOf(split[3]);
		_windowsBuildNumber = Integer.valueOf(split[4]);
		_directxVersion = Integer.valueOf(split[5]);
		_directxRevision = Integer.valueOf(split[6]);
		_cpuName = split[7];
		_cpuSpeed = Integer.valueOf(split[8]);
		_cpuCoreCount = Integer.valueOf(split[9]);
		_vgaCount = Integer.valueOf(split[10]);
		_vgaPcxSpeed = Integer.valueOf(split[11]);
		_physMemorySlot1 = Integer.valueOf(split[12]);
		_physMemorySlot2 = Integer.valueOf(split[13]);
		_physMemorySlot3 = Integer.valueOf(split[14]);
		_videoMemory = Integer.valueOf(split[15]);
		_vgaVersion = Integer.valueOf(split[16]);
		_vgaName = split[17];
		_vgaDriverVersion = split[18];
	}
	
	/**
	 * Save hardware info to account variables for later use.
	 * @param player The Player related with this hardware info.
	 */
	public void store(Player player)
	{
		final StringBuilder sb = new StringBuilder();
		sb.append(_macAddress);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_windowsPlatformId);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_windowsMajorVersion);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_windowsMinorVersion);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_windowsBuildNumber);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_directxVersion);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_directxRevision);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_cpuName);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_cpuSpeed);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_cpuCoreCount);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_vgaCount);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_vgaPcxSpeed);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_physMemorySlot1);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_physMemorySlot2);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_physMemorySlot3);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_videoMemory);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_vgaVersion);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_vgaName);
		sb.append(AccountVariables.HWIDSLIT_VAR);
		sb.append(_vgaDriverVersion);
		player.getAccountVariables().set(AccountVariables.HWID, sb.toString());
	}
	
	/**
	 * @return the macAddress
	 */
	public String getMacAddress()
	{
		return _macAddress;
	}
	
	/**
	 * @return the windowsPlatformId
	 */
	public int getWindowsPlatformId()
	{
		return _windowsPlatformId;
	}
	
	/**
	 * @return the windowsMajorVersion
	 */
	public int getWindowsMajorVersion()
	{
		return _windowsMajorVersion;
	}
	
	/**
	 * @return the windowsMinorVersion
	 */
	public int getWindowsMinorVersion()
	{
		return _windowsMinorVersion;
	}
	
	/**
	 * @return the windowsBuildNumber
	 */
	public int getWindowsBuildNumber()
	{
		return _windowsBuildNumber;
	}
	
	/**
	 * @return the directxVersion
	 */
	public int getDirectxVersion()
	{
		return _directxVersion;
	}
	
	/**
	 * @return the directxRevision
	 */
	public int getDirectxRevision()
	{
		return _directxRevision;
	}
	
	/**
	 * @return the cpuName
	 */
	public String getCpuName()
	{
		return _cpuName;
	}
	
	/**
	 * @return the cpuSpeed
	 */
	public int getCpuSpeed()
	{
		return _cpuSpeed;
	}
	
	/**
	 * @return the cpuCoreCount
	 */
	public int getCpuCoreCount()
	{
		return _cpuCoreCount;
	}
	
	/**
	 * @return the vgaCount
	 */
	public int getVgaCount()
	{
		return _vgaCount;
	}
	
	/**
	 * @return the vgaPcxSpeed
	 */
	public int getVgaPcxSpeed()
	{
		return _vgaPcxSpeed;
	}
	
	/**
	 * @return the physMemorySlot1
	 */
	public int getPhysMemorySlot1()
	{
		return _physMemorySlot1;
	}
	
	/**
	 * @return the physMemorySlot2
	 */
	public int getPhysMemorySlot2()
	{
		return _physMemorySlot2;
	}
	
	/**
	 * @return the physMemorySlot3
	 */
	public int getPhysMemorySlot3()
	{
		return _physMemorySlot3;
	}
	
	/**
	 * @return the videoMemory
	 */
	public int getVideoMemory()
	{
		return _videoMemory;
	}
	
	/**
	 * @return the vgaVersion
	 */
	public int getVgaVersion()
	{
		return _vgaVersion;
	}
	
	/**
	 * @return the vgaName
	 */
	public String getVgaName()
	{
		return _vgaName;
	}
	
	/**
	 * @return the vgaDriverVersion
	 */
	public String getVgaDriverVersion()
	{
		return _vgaDriverVersion;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		return (obj instanceof ClientHardwareInfoHolder) && _macAddress.equals(((ClientHardwareInfoHolder) obj).getMacAddress());
	}
}
